#!/usr/bin/env python3
"""Stop every screen from rendering its background (and blur) twice.

1.20.1's `Screen.render` did NOT call `renderBackground`; a screen had to call it
itself, and calling it again was harmless because it only redrew the backdrop.
1.21.1's `Screen.render` (`.refs/nf-src/.../Screen.java:132-138`) calls
`renderBackground` for you, and that method now also runs the menu **blur**
post effect (`Screen.java:382` -> `GameRenderer.processBlurEffect`).  So a
screen that calls `renderBackground` itself and then calls `super.render` blurs
everything it drew in between -- its panel, its items and its text all come out
blurry while the widgets drawn afterwards stay crisp.  That is exactly the
reported blueprint-screen defect.

The fix differs by base class, and the vanilla 1.21.1 sources show why:

* plain `Screen` (`BlueprintScreen`): `super.render` is `Screen.render`, i.e.
  background + widgets.  Vanilla's `AbstractContainerScreen.render` "replicates
  the super method's implementation to insert the event between background and
  widgets" -- so replicate it here: call `renderBackground` once, then render
  `this.renderables` directly instead of calling `super.render`.

* `AbstractContainerScreen` (the other 20): `super.render` is
  `AbstractContainerScreen.render`, which already calls `renderBackground`
  (and through its override of it, `renderBg`).  Their custom drawing happens
  *after* `super.render`, so the explicit call is simply redundant -- drop it.

Usage:
    python tools/fix_double_background.py --check
    python tools/fix_double_background.py
    python tools/fix_double_background.py --selftest
"""

import argparse
import pathlib
import re
import sys

SCREENS = pathlib.Path("src/main/java/top/ribs/scguns/client/screen")

CLASS_DECL = re.compile(r"\bclass\s+\w+Screen\s+extends\s+(\w+)")
RENDER_BG = re.compile(r"^(\s*)this\.renderBackground\(([^;]*)\);\s*$")
SUPER_RENDER = re.compile(r"^(\s*)super\.render\(([^;]*)\);\s*$")
RENDERABLE_IMPORT = "import net.minecraft.client.gui.components.Renderable;"


def fix_text(text):
    """Return (new_text, applied_counts)."""
    applied = {"container_dropped_background": 0, "screen_replicated_super": 0, "import_added": 0}

    declaration = CLASS_DECL.search(text)
    if declaration is None:
        return text, applied
    base = declaration.group(1)

    lines = text.split("\n")
    out = []
    for line in lines:
        match = RENDER_BG.match(line)
        if match and base == "AbstractContainerScreen":
            # super.render already renders the background (and blur) exactly once.
            applied["container_dropped_background"] += 1
            continue

        match = SUPER_RENDER.match(line)
        if match and base == "Screen":
            indent, args = match.group(1), match.group(2)
            out.append("%s// Screen.render renders the background (and its blur) itself, so calling" % indent)
            out.append("%s// super.render here would run the blur a second time and smear everything" % indent)
            out.append("%s// drawn above. Replicate the super implementation instead, exactly as" % indent)
            out.append("%s// vanilla's AbstractContainerScreen.render does." % indent)
            out.append("%sfor (Renderable renderable : this.renderables) {" % indent)
            out.append("%s   renderable.render(%s);" % (indent, args))
            out.append("%s}" % indent)
            applied["screen_replicated_super"] += 1
            continue

        out.append(line)

    text = "\n".join(out)

    if applied["screen_replicated_super"] and RENDERABLE_IMPORT not in text:
        lines = text.split("\n")
        last_import = max(
            (i for i, l in enumerate(lines) if l.startswith("import ")),
            default=-1,
        )
        lines.insert(last_import + 1, RENDERABLE_IMPORT)
        text = "\n".join(lines)
        applied["import_added"] += 1

    return text, applied


def run(check_only):
    if not SCREENS.is_dir():
        print("ERROR: %s not found; run from the repo root" % SCREENS)
        return 2

    totals = {"container_dropped_background": 0, "screen_replicated_super": 0, "import_added": 0}
    touched = []

    for path in sorted(SCREENS.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        new_text, applied = fix_text(text)
        if not any(applied.values()):
            continue
        touched.append((path.name, {k: v for k, v in applied.items() if v}))
        for key in totals:
            totals[key] += applied[key]
        if not check_only:
            path.write_text(new_text, encoding="utf-8")

    print("container screens: redundant renderBackground removed : %d" % totals["container_dropped_background"])
    print("plain screens: super.render replicated (background once): %d" % totals["screen_replicated_super"])
    print("imports added                                          : %d" % totals["import_added"])
    print("files %s: %d" % ("needing changes" if check_only else "rewritten", len(touched)))
    for name, applied in touched[:25]:
        print("    %-38s %s" % (name, applied))
    return 0


def selftest():
    failures = []

    def check(label, got, want):
        if got != want:
            failures.append("%s: got %r want %r" % (label, got, want))

    container = (
        "package p;\n"
        "import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;\n"
        "public class FooScreen extends AbstractContainerScreen<FooMenu> {\n"
        "   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {\n"
        "      this.renderBackground(guiGraphics, mouseX, mouseY, delta);\n"
        "      super.render(guiGraphics, mouseX, mouseY, delta);\n"
        "      this.renderTooltip(guiGraphics, mouseX, mouseY);\n"
        "   }\n"
        "}\n"
    )
    out, applied = fix_text(container)
    check("container background dropped", applied["container_dropped_background"], 1)
    check("background line gone", "this.renderBackground(" not in out, True)
    check("super.render kept", "super.render(guiGraphics, mouseX, mouseY, delta);" in out, True)
    check("tooltip kept", "this.renderTooltip(guiGraphics, mouseX, mouseY);" in out, True)

    plain = (
        "package p;\n"
        "import net.minecraft.client.gui.screens.Screen;\n"
        "public class BarScreen extends Screen {\n"
        "   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {\n"
        "      this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);\n"
        "      guiGraphics.blit(TEX, 0, 0, 0, 0, 192, 192);\n"
        "      super.render(guiGraphics, mouseX, mouseY, partialTick);\n"
        "      this.renderTooltips(guiGraphics, mouseX, mouseY);\n"
        "   }\n"
        "}\n"
    )
    out, applied = fix_text(plain)
    check("plain keeps explicit background", applied["container_dropped_background"], 0)
    check("plain replicated super", applied["screen_replicated_super"], 1)
    check("plain still calls renderBackground", "this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);" in out, True)
    check("plain no longer calls super.render", "super.render(" not in out, True)
    check("widget loop emitted",
          "for (Renderable renderable : this.renderables) {" in out, True)
    check("widget render args match super's args",
          "renderable.render(guiGraphics, mouseX, mouseY, partialTick);" in out, True)
    # The panel blit must still happen between the background and the widgets.
    check("panel blit preserved", "guiGraphics.blit(TEX, 0, 0, 0, 0, 192, 192);" in out, True)
    check("content order preserved",
          out.index("this.renderBackground(") < out.index("guiGraphics.blit(") < out.index("for (Renderable"), True)
    check("import added", RENDERABLE_IMPORT in out, True)
    check("indentation preserved", "\n      for (Renderable renderable" in out, True)

    # An existing import must not be duplicated.
    with_import = plain.replace(
        "import net.minecraft.client.gui.screens.Screen;",
        "import net.minecraft.client.gui.screens.Screen;\n" + RENDERABLE_IMPORT,
    )
    out, applied = fix_text(with_import)
    check("no duplicate import", out.count(RENDERABLE_IMPORT), 1)
    check("import not recounted", applied["import_added"], 0)

    # Idempotence.
    once, _a = fix_text(plain)
    twice, applied = fix_text(once)
    check("idempotent", twice, once)
    check("idempotent no-op", sum(applied.values()), 0)

    once, _a = fix_text(container)
    twice, applied = fix_text(once)
    check("container idempotent", twice, once)

    if failures:
        print("SELFTEST FAILED (%d)" % len(failures))
        for failure in failures:
            print("  - %s" % failure)
        return 1

    print("SELFTEST OK (container drop, plain replicate, import handling)")
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
