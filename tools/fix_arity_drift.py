#!/usr/bin/env python3
"""Repair the arity drift family: methods that stopped overriding because a
Forge-only trailing parameter was dropped in NeoForge.

`tools/audit_override_drift.py` finds these (bucket ARITY-MISMATCH).  Each one
compiles as a plain overload, is never called, and fails silently, so the fix
has to restore the 1.21.1 signature AND `@Override`, which then makes javac the
proof.

Four families exist in this port:

1. ``finalizeSpawn`` -- 1.20.1 Forge appended ``@Nullable CompoundTag pDataTag``;
   1.21.1 is
   ``finalizeSpawn(ServerLevelAccessor, DifficultyInstance, MobSpawnType, SpawnGroupData)``.
   The port kept the 5-argument form, so it never ran, and the
   ``EntityEquipmentConfig.equipEntity(...)`` call inside it never happened --
   which is why mobs spawned with no weapons.  ``pDataTag`` is unused, so the
   parameter is simply dropped.

2. ``checkAndPerformAttack`` -- 1.20.1 took ``(LivingEntity, double distanceSqr)``;
   1.21.1 is ``checkAndPerformAttack(LivingEntity)``.  The body uses the distance,
   so it is recomputed locally with ``this.mob.distanceToSqr(...)``, which is the
   same value the old parameter carried.

3. ``getEyeHeight(Pose, EntityDimensions)`` -- that hook does not exist anywhere
   in 1.21.1 (``Entity.getEyeHeight(Pose)`` and ``getEyeHeight()`` are both
   ``final``).  Eye height is now a property of ``EntityDimensions``, so the
   override moves to ``getDimensions(Pose)`` and uses ``withEyeHeight``.

4. ``renderToBuffer`` -- 1.20.1 took trailing RGBA floats; 1.21.1 is
   ``renderToBuffer(PoseStack, VertexConsumer, int, int, int color)``.

Usage:
    python tools/fix_arity_drift.py --check
    python tools/fix_arity_drift.py
    python tools/fix_arity_drift.py --selftest
"""

import argparse
import pathlib
import re
import sys

SRC = pathlib.Path("src/main/java/top/ribs/scguns")

# 1. finalizeSpawn: drop the trailing Forge-only tag parameter.
FINALIZE_DECL = re.compile(r"^(\s*)public\s+SpawnGroupData\s+finalizeSpawn\s*\(\s*$")
# `..., @Nullable CompoundTag pDataTag` at the end of a shared parameter line.
FINALIZE_TAIL = re.compile(r",\s*(?:@\w+(?:\([^)]*\))?\s+)*CompoundTag\s+\w+\s*$")
# ...or a whole line holding just that one parameter (parameter-per-line style).
FINALIZE_OWN_LINE = re.compile(r"^\s*(?:@\w+(?:\([^)]*\))?\s+)*CompoundTag\s+\w+\s*,?\s*$")

# 2. checkAndPerformAttack: drop the double and recompute it from the target.
ATTACK_DECL = re.compile(
    r"^(\s*)protected\s+void\s+checkAndPerformAttack\s*\(\s*LivingEntity\s+(\w+)\s*,\s*double\s+(\w+)\s*\)\s*\{\s*$"
)

# 3. getEyeHeight -> getDimensions(Pose).withEyeHeight(...)
EYE_DECL = re.compile(
    r"^(\s*)protected\s+float\s+getEyeHeight\s*\(\s*Pose\s+\w+\s*,\s*EntityDimensions\s+\w+\s*\)\s*\{\s*$"
)
EYE_RETURN = re.compile(r"^(\s*)return\s+(.+?);\s*$")

# 4. renderToBuffer: RGBA floats -> a single packed colour.  The declaration is
#    written both as one long line and wrapped onto a second line.
RENDER_DECL = re.compile(r"^(\s*)public\s+void\s+renderToBuffer\s*\(")
RENDER_FLOAT_TAIL = re.compile(
    r",\s*float\s+\w+\s*,\s*float\s+\w+\s*,\s*float\s+\w+\s*,\s*float\s+\w+"
)


def already_overridden(lines, index):
    for probe in range(index - 1, max(-1, index - 4), -1):
        stripped = lines[probe].strip()
        if stripped == "@Override":
            return True
        if stripped and not stripped.startswith("@"):
            return False
    return False


def indent_of(line):
    return re.match(r"(\s*)", line).group(1)


def fix_text(text):
    """Return (new_text, applied_counts)."""
    applied = {
        "finalize_spawn": 0,
        "attack_distance": 0,
        "eye_height": 0,
        "render_to_buffer": 0,
    }
    lines = text.split("\n")
    out = []
    index = 0
    while index < len(lines):
        line = lines[index]

        # --- 1. finalizeSpawn ------------------------------------------------
        # The parameter list may be one shared line or one parameter per line, so
        # collect the block up to the line carrying the closing paren and then
        # trim the Forge-only tag parameter off the end of it.
        match = FINALIZE_DECL.match(line)
        if match:
            block = []
            probe = index + 1
            while probe < len(lines) and ")" not in lines[probe]:
                block.append(probe)
                probe += 1
            if block:
                last = block[-1]
                if FINALIZE_TAIL.search(lines[last]):
                    lines[last] = FINALIZE_TAIL.sub("", lines[last])
                    removed = True
                elif FINALIZE_OWN_LINE.match(lines[last]) and len(block) > 1:
                    del lines[last]
                    block.pop()
                    if block:
                        lines[block[-1]] = re.sub(r",\s*$", "", lines[block[-1]])
                    removed = True
                else:
                    removed = False
                if removed:
                    if not already_overridden(lines, index):
                        out.append(indent_of(line) + "@Override")
                    out.append(line)
                    for position in block:
                        out.append(lines[position])
                    applied["finalize_spawn"] += 1
                    index = (block[-1] if block else index) + 1
                    continue

        # --- 2. checkAndPerformAttack ---------------------------------------
        match = ATTACK_DECL.match(line)
        if match:
            indent, enemy, distance = match.group(1), match.group(2), match.group(3)
            replacement = (
                "%sprotected void checkAndPerformAttack(LivingEntity %s) {" % (indent, enemy)
            )
            if not already_overridden(lines, index):
                out.append(indent + "@Override")
            out.append(replacement)
            # The old parameter carried the squared distance; recompute the same
            # quantity locally so the body keeps working unchanged.
            out.append("%s   double %s = this.mob.distanceToSqr(%s);" % (indent, distance, enemy))
            applied["attack_distance"] += 1
            index += 1
            continue

        # --- 3. getEyeHeight -------------------------------------------------
        match = EYE_DECL.match(line)
        if match and index + 1 < len(lines):
            indent = match.group(1)
            inner = EYE_RETURN.match(lines[index + 1])
            if inner:
                if not already_overridden(lines, index):
                    out.append(indent + "@Override")
                out.append("%spublic EntityDimensions getDimensions(Pose pose) {" % indent)
                out.append(
                    "%s   return super.getDimensions(pose).withEyeHeight(%s);"
                    % (indent, inner.group(2))
                )
                out.append("%s}" % indent)
                applied["eye_height"] += 1
                index += 2  # skip the old declaration and its single return
                # skip the old closing brace
                if index < len(lines) and lines[index].strip() == "}":
                    index += 1
                continue

        # --- 4. renderToBuffer ----------------------------------------------
        # Two layouts: the whole declaration on one line, or the parameters
        # wrapped onto the following line.
        match = RENDER_DECL.match(line)
        if match:
            if RENDER_FLOAT_TAIL.search(line):
                lines[index] = RENDER_FLOAT_TAIL.sub(", int color", line)
                if not already_overridden(lines, index):
                    out.append(indent_of(line) + "@Override")
                out.append(lines[index])
                applied["render_to_buffer"] += 1
                index += 1
                continue
            if index + 1 < len(lines) and RENDER_FLOAT_TAIL.search(lines[index + 1]):
                lines[index + 1] = RENDER_FLOAT_TAIL.sub(", int color", lines[index + 1])
                if not already_overridden(lines, index):
                    out.append(indent_of(line) + "@Override")
                out.append(line)
                out.append(lines[index + 1])
                applied["render_to_buffer"] += 1
                index += 2
                continue

        out.append(line)
        index += 1

    return "\n".join(out), applied


def run(check_only):
    if not SRC.exists():
        print("ERROR: %s not found; run from the repo root" % SRC)
        return 2

    totals = {"finalize_spawn": 0, "attack_distance": 0, "eye_height": 0, "render_to_buffer": 0}
    touched = []

    for path in sorted(SRC.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        new_text, applied = fix_text(text)
        if not any(applied.values()):
            continue
        touched.append((str(path), {k: v for k, v in applied.items() if v}))
        for key in totals:
            totals[key] += applied[key]
        if not check_only:
            path.write_text(new_text, encoding="utf-8")

    print("finalizeSpawn     (drop CompoundTag)      : %d" % totals["finalize_spawn"])
    print("checkAndPerformAttack (drop double)      : %d" % totals["attack_distance"])
    print("getEyeHeight      (-> getDimensions)     : %d" % totals["eye_height"])
    print("renderToBuffer    (RGBA -> int color)    : %d" % totals["render_to_buffer"])
    print("files %s: %d" % ("needing changes" if check_only else "rewritten", len(touched)))
    for name, applied in touched[:12]:
        print("    %-74s %s" % (name, applied))
    if len(touched) > 12:
        print("    ... and %d more" % (len(touched) - 12))
    return 0


def selftest():
    failures = []

    def check(label, got, want):
        if got != want:
            failures.append("%s: got %r want %r" % (label, got, want))

    # --- 1. finalizeSpawn -----------------------------------------------------
    src = (
        "public class A extends Monster {\n"
        "   public SpawnGroupData finalizeSpawn(\n"
        "      ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, "
        "@Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag\n"
        "   ) {\n"
        "      EntityEquipmentConfig.equipEntity(this, \"scguns:a\");\n"
        "      return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);\n"
        "   }\n"
        "}\n"
    )
    out, applied = fix_text(src)
    check("finalize counted", applied["finalize_spawn"], 1)
    check("CompoundTag param removed", "CompoundTag" not in out, True)
    check("4-parameter form kept", "@Nullable SpawnGroupData pSpawnData\n   ) {" in out, True)
    check("finalize @Override", "@Override" in out, True)
    check("equipEntity call preserved", 'equipEntity(this, "scguns:a");' in out, True)

    # --- 2. checkAndPerformAttack --------------------------------------------
    src = (
        "public class A2 extends MeleeAttackGoal {\n"
        "      protected void checkAndPerformAttack(LivingEntity pEnemy, double pDistToEnemySqr) {\n"
        "         if (pDistToEnemySqr <= 4.0 && this.getTicksUntilNextAttack() <= 0) {\n"
    )
    out, applied = fix_text(src)
    check("attack counted", applied["attack_distance"], 1)
    check("single-parameter form",
          "protected void checkAndPerformAttack(LivingEntity pEnemy) {" in out, True)
    check("distance recomputed",
          "double pDistToEnemySqr = this.mob.distanceToSqr(pEnemy);" in out, True)
    # The body must be byte-for-byte preserved.
    check("body preserved",
          "if (pDistToEnemySqr <= 4.0 && this.getTicksUntilNextAttack() <= 0) {" in out, True)
    check("attack @Override", "@Override" in out, True)
    check("indentation preserved", "\n      @Override\n" in out, True)

    # --- 3. getEyeHeight -> getDimensions ------------------------------------
    src = (
        "public class B extends Entity {\n"
        "   protected float getEyeHeight(Pose pose, EntityDimensions dimensions) {\n"
        "      return 0.15F;\n"
        "   }\n"
        "}\n"
    )
    out, applied = fix_text(src)
    check("eye counted", applied["eye_height"], 1)
    check("getDimensions override", "public EntityDimensions getDimensions(Pose pose) {" in out, True)
    check("withEyeHeight used",
          "return super.getDimensions(pose).withEyeHeight(0.15F);" in out, True)
    check("old signature gone", "getEyeHeight(Pose pose" not in out, True)
    # Exactly one closing brace must remain for the method: old body had 3 lines,
    # new has 3 lines, so the class still closes correctly.
    check("brace balance preserved", out.count("{"), src.count("{"))
    check("brace close balance", out.count("}"), src.count("}"))
    check("eye @Override", "@Override" in out, True)

    # --- 4. renderToBuffer ---------------------------------------------------
    src = (
        "public class C extends HierarchicalModel<X> {\n"
        "   public void renderToBuffer(\n"
        "      @NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, "
        "int packedLight, int packedOverlay, float red, float green, float blue, float alpha\n"
        "   ) {\n"
        "      this.head.render(poseStack, vertexConsumer, packedLight, packedOverlay);\n"
        "   }\n"
        "}\n"
    )
    out, applied = fix_text(src)
    check("render counted", applied["render_to_buffer"], 1)
    check("int color form", "int packedOverlay, int color" in out, True)
    check("floats gone", "float alpha" not in out, True)
    check("render body preserved",
          "this.head.render(poseStack, vertexConsumer, packedLight, packedOverlay);" in out, True)
    check("render @Override", "@Override" in out, True)

    # --- @Override must not be duplicated ------------------------------------
    src = (
        "   @Override\n"
        "   protected void checkAndPerformAttack(LivingEntity e, double d) {\n"
    )
    out, _applied = fix_text(src)
    check("no duplicate @Override", out.count("@Override"), 1)

    # --- idempotence ---------------------------------------------------------
    once, _a = fix_text(
        "   public SpawnGroupData finalizeSpawn(\n"
        "      ServerLevelAccessor l, DifficultyInstance d, MobSpawnType r, SpawnGroupData s, CompoundTag t\n"
        "   ) {\n"
    )
    twice, applied = fix_text(once)
    check("idempotent", twice, once)
    check("idempotent no-op", sum(applied.values()), 0)

    if failures:
        print("SELFTEST FAILED (%d)" % len(failures))
        for failure in failures:
            print("  - %s" % failure)
        return 1

    print("SELFTEST OK (finalizeSpawn, checkAndPerformAttack, getEyeHeight, renderToBuffer)")
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--selftest", action="store_true")
    args = parser.parse_args()

    if args.selftest:
        return selftest()
    return run(args.check)


if __name__ == "__main__":
    sys.exit(main())
