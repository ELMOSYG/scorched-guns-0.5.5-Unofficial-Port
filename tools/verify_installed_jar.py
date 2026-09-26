"""Verify the jar installed in the user's instance actually contains every fix.

Checks the packaged artifact rather than the source tree, because the jar is
what the game loads.

Usage:
    python tools/verify_installed_jar.py
    SCGUNS_JAR=<path to an older jar> python tools/verify_installed_jar.py
        (used to prove a new check actually fails on the build before the fix)
"""

import json
import os
import pathlib
import re
import zipfile

MODS = pathlib.Path(r"D:\MCJAVA\.minecraft\versions\1.21.1-NeoForge_21.1.250\mods")


def _installed_jar():
    """The instance's scguns jar, newest first; SCGUNS_JAR overrides it for older-build self-tests."""
    override = os.environ.get("SCGUNS_JAR")
    if override:
        return pathlib.Path(override)
    candidates = [p for p in MODS.glob("scguns-*.jar") if ".bak-" not in p.name]
    return max(candidates, key=lambda p: p.stat().st_mtime) if candidates else MODS / "scguns-none.jar"


def _built_jar():
    """The jar build/libs currently holds (the name follows mod_version)."""
    candidates = [p for p in pathlib.Path("build/libs").glob("scguns-*.jar")
                  if not p.name.endswith(("-sources.jar", "-javadoc.jar"))]
    return max(candidates, key=lambda p: p.stat().st_mtime) if candidates else \
        pathlib.Path("build/libs/scguns-none.jar")


JAR = _installed_jar()
BUILT = _built_jar()
DATA = "data/scguns/recipe/"


def _check_nested_mods_toml(zf, needle, label):
    """Look for *needle* inside the maid compat that ships nested in the host jar.

    The nested jar goes stale quietly: `gradlew runServer` loads it from run/mods, and the host jar only
    picks up a rebuilt copy when the maid-compat jar task actually reruns. Checking the packaged copy is
    therefore the only way to know what players get (HANDOFF section 70).
    """
    for name in zf.namelist():
        if not (name.startswith("META-INF/jarjar/") and name.endswith(".jar")):
            continue
        import io

        with zipfile.ZipFile(io.BytesIO(zf.read(name))) as nested:
            for inner in nested.namelist():
                if inner.endswith("neoforge.mods.toml"):
                    if needle in nested.read(inner):
                        return (label, True)
    return (label + " [not found in the nested jar]", False)


def _check_neoforge_floor(zf, floor, label):
    """The shipped metadata must ask for the floor we claim (and nothing higher)."""
    for entry in ("META-INF/neoforge.mods.toml",):
        if entry not in zf.namelist():
            return (label + " [%s missing]" % entry, False)
        text = zf.read(entry).decode("utf-8", "replace")
        for block in text.split("[["):
            if 'modId="neoforge"' not in block and 'modId = "neoforge"' not in block:
                continue
            found = re.search(r'versionRange\s*=\s*"([^"]+)"', block)
            if found:
                return (label + " [%s]" % found.group(1), found.group(1) == floor)
    return (label + " [neoforge dependency missing]", False)


def _check_class_contains(zf, entry, needle, label):
    """True when the packaged class's constant pool contains *needle*."""
    try:
        data = zf.read(entry)
    except KeyError:
        return (label + " [class missing]", False)
    return (label, needle in data)


def _check_class_lacks(zf, entry, needle, label):
    """True when the class does NOT mention *needle* at all.

    This is how the model colour fix is checked: a fixed model renders its parts through the
    five argument ModelPart.render, so the four argument descriptor is no longer referenced
    anywhere in that class. Counting occurrences cannot be used instead - a class file's
    constant pool deduplicates identical UTF-8 entries, so a descriptor used twenty times
    still appears once.
    """
    try:
        data = zf.read(entry)
    except KeyError:
        return (label + " [class missing]", False)
    return (label, needle not in data)


def _check_packaged_tree_lacks(zf, needle, label):
    """True when no class we ship (outside the nested maid-compat jar) mentions *needle*.

    For strings that identify a dead API - the legacy GeckoLib id key, for instance: it only has
    to be gone from the class that used to write it, but a second writer would reintroduce the bug,
    so the whole tree is checked.
    """
    for name in zf.namelist():
        if not (name.startswith("top/ribs/scguns/") and name.endswith(".class")):
            continue
        if needle in zf.read(name):
            return (label + " [%s]" % name, False)
    return (label, True)


def _check_resource_contains(zf, entry, needle, label):
    """True when a packaged data/resource file contains *needle*.

    Used for things that are data rather than code - the enchantment tag that decides whether the
    enchanting table and the librarian may offer the mod's enchantments, for instance.
    """
    try:
        data = zf.read(entry)
    except KeyError:
        return (label + " [%s missing]" % entry, False)
    return (label, needle in data)


def _check_declared_version(zf):
    """The packaged version is the one gradle.properties asks for (the jar name follows it too)."""
    expected = re.search(r"^mod_version\s*=\s*(\S+)",
                         pathlib.Path("gradle.properties").read_text(encoding="utf-8"), re.M)
    if not expected:
        return ("the packaged version matches mod_version [gradle.properties unreadable]", False)
    wanted = expected.group(1)
    text = zf.read("META-INF/neoforge.mods.toml").decode("utf-8", "replace")
    found = re.search(r'^\s*version\s*=\s*"([^"]+)"', text, re.M)
    return ("the packaged version is mod_version [%s]" % (found.group(1) if found else "?"),
            bool(found) and found.group(1) == wanted)


FINALIZE_4 = (
    b"(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;"
    b"Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/world/entity/SpawnGroupData;)"
    b"Lnet/minecraft/world/entity/SpawnGroupData;"
)
FINALIZE_5 = (
    b"(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;"
    b"Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/world/entity/SpawnGroupData;"
    b"Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/entity/SpawnGroupData;"
)


def _check_finalize_spawn(zf):
    """The mobs with equipment configs must declare the 4-argument override.

    The five-argument Forge form is a plain overload: it compiles, never runs,
    and that is what left every gunner unarmed.
    """
    stragglers = []
    checked = 0
    for name in (
        "AdjudicatorEntity", "BlundererEntity", "CogKnightEntity", "CogMinionEntity",
        "DissidentEntity", "FinforcerEntity", "HornlinEntity", "SubjugatorEntity",
        "SupplyScampEntity", "ZombifiedHornlinEntity",
    ):
        entry = "top/ribs/scguns/entity/monster/%s.class" % name
        try:
            data = zf.read(entry)
        except KeyError:
            continue
        checked += 1
        if FINALIZE_5 in data:
            stragglers.append(name + " still has the 5-arg form")

    if checked == 0:
        return ("finalizeSpawn overrides are 4-argument [no classes found]", False)
    if stragglers:
        return ("finalizeSpawn overrides are 4-argument [%s]" % "; ".join(stragglers), False)
    return ("finalizeSpawn overrides are 4-argument (%d classes)" % checked, True)


def main():
    if not JAR.exists():
        print("MISSING: %s" % JAR)
        return 2

    if BUILT.exists() and BUILT.read_bytes() != JAR.read_bytes():
        print("WARNING: installed jar differs from %s" % BUILT)
    else:
        print("installed jar is byte-identical to %s" % BUILT)

    checks = []

    with zipfile.ZipFile(JAR) as zf:
        names = set(zf.namelist())

        def read(name):
            try:
                return zf.read(name).decode("utf-8")
            except KeyError:
                return None

        # 1. conditions key migration
        basker = read("data/scguns/recipe/create/mechanical_crafting/basker.json")
        checks.append(("Create recipe uses neoforge:conditions", basker and "neoforge:conditions" in basker))
        checks.append(("Create recipe uses accept_mirrored", basker and "accept_mirrored" in basker))
        checks.append(("Create recipe has no stale conditions key", basker and '\n  "conditions"' not in basker))

        # 2. tag directory layout
        checks.append(("c: tag moved to singular tags/item", "data/c/tags/item/dusts/saltpeter.json" in names))
        checks.append(("legacy tags/items path gone", "data/c/tags/items/dusts/saltpeter.json" not in names))
        checks.append(("legacy tags/blocks path gone", not any(n.startswith("data/c/tags/blocks/") for n in names)))

        # 3. tag namespace
        brutal = read("data/scguns/recipe/immersiveengineering/crusher/ore_anthralite_1.json")
        checks.append(("IE crusher uses basePredicate", brutal and "basePredicate" in brutal))
        checks.append(("IE secondary uses own conditions key", brutal and '"conditions"' in brutal))

        # 4. no legacy neoforge: tag references in recipes
        legacy_refs = 0
        for name in names:
            if name.startswith(DATA) and name.endswith(".json"):
                text = read(name) or ""
                if '"tag": "neoforge:' in text:
                    legacy_refs += 1
        checks.append(("no recipe references a neoforge: tag", legacy_refs == 0))

        # 5. stale vanilla ids
        weak = read("data/scguns/tags/item/weak_compost.json")
        checks.append(("weak_compost uses minecraft:short_grass", weak and "minecraft:short_grass" in weak))
        checks.append(("weak_compost has no minecraft:grass", weak and '"minecraft:grass"' not in weak))

        fragile = read("data/scguns/tags/block/fragile.json")
        checks.append(("fragile uses c:glass_blocks", fragile and "#c:glass_blocks" in fragile))

        tools_tag = read("data/c/tags/item/tools.json")
        checks.append(("c:tools marks the knife optional", tools_tag and "required" in tools_tag))

        # 6. cross-mod schemas
        mek = read("data/scguns/recipe/mekanism/clump/from_raw_block.json")
        checks.append(("Mekanism uses chemical_input", mek and "chemical_input" in mek))
        checks.append(("Mekanism uses per_tick_usage", mek and "per_tick_usage" in mek))

        vein = read("data/createoreexcavation/recipe/ore_vein_type/anthralite.json")
        checks.append(("COE vein uses amountMultiplierMax", vein and "amountMultiplierMax" in vein))
        checks.append(("COE vein has finite", vein and '"finite"' in vein))

        drill = read("data/createoreexcavation/recipe/drilling/anthralite.json")
        checks.append(("COE drilling uses veinId", drill and "veinId" in drill))

        # 7. code-level fixes, checked in the packaged bytecode.
        # finalizeSpawn must carry the 1.21.1 four-parameter descriptor; the
        # five-parameter one means it does not override and never runs.
        checks.append(_check_finalize_spawn(zf))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/entity/ai/GunAttackGoal.class", b"getAmmoCount",
            "GunAttackGoal reads ammo through the null-safe helper"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/config/EntityEquipmentConfig$EquipmentEntry.class", b"AmmoCount",
            "EquipmentEntry initialises AmmoCount for guns"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/util/NbtHelper.class", b"getUnsafe",
            "NbtHelper re-reads the stored tag (CustomData.of copies)"))

        # 8. the 1.21 time-delta and diagnostic changes.
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/client/handler/RecoilHandler.class", b"getGameTimeDeltaTicks",
            "RecoilHandler uses the frame delta, not the partial tick"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/client/handler/GunRecoilHandler.class", b"getGameTimeDeltaTicks",
            "GunRecoilHandler uses the frame delta"))
        # The pose diagnostic was temporary: the pose is confirmed working in
        # game, so the class must be gone from the shipped jar.
        checks.append((
            "temporary pose diagnostic removed from the build",
            "top/ribs/scguns/client/render/PoseDiagnostics.class" not in names,
        ))

        # 9. advancement display + component-write fixes (HANDOFF §16).
        # A 1.20.1 "item" icon makes DisplayInfo fail, and a failed display does
        # not drop the advancement -- it loads invisibly, so only the file
        # content can be checked here.
        #
        # Advancements without any display section are legal and are exactly what
        # vanilla ships for recipe unlocks: all 1277 of data/minecraft/advancement/
        # recipes/** have only criteria/requirements/rewards (HANDOFF §39 added the
        # mod's first such files). Such a file has no icon to validate.
        bad_icons = []
        display_less = 0
        for name in names:
            if not name.startswith("data/scguns/advancement/") or not name.endswith(".json"):
                continue
            try:
                display = json.loads(read(name)).get("display")
            except Exception:
                bad_icons.append(name + " (unreadable json)")
                continue
            if display is None:
                display_less += 1
                continue
            icon = display.get("icon")
            if icon is None:
                bad_icons.append(name + " (display without icon)")
            elif "item" in icon or "id" not in icon:
                bad_icons.append(name)
        if bad_icons:
            checks.append(("advancement icons use \"id\" [%s]" % "; ".join(sorted(bad_icons)[:3]), False))
        else:
            checks.append(("advancement icons use \"id\" (%d files, %d recipe unlocks have no display)" % (
                sum(1 for n in names
                    if n.startswith("data/scguns/advancement/") and n.endswith(".json")),
                display_less), True))

        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/util/NbtHelper.class", b"getTagForWrite",
            "NbtHelper exposes a detached write tag"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/client/screen/AttachmentContainer.class", b"getOrCreateTag",
            "attachment menu writes through the detaching accessor"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/client/screen/BlueprintScreen.class", b"getKey",
            "blueprint description resolves the item path from the registry"))

        # 10. packet codecs must tolerate empty stacks (1.21's strict
        # ItemStack.STREAM_CODEC throws inside the netty encoder, which drops the
        # player instead of logging a normal error).
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/network/message/C2SMessageSaveExoSuitUpgrades.class",
            b"OPTIONAL_STREAM_CODEC",
            "exosuit upgrade packet uses the optional item codec"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/entity/throwable/ThrowableItemEntity.class",
            b"OPTIONAL_STREAM_CODEC",
            "throwable spawn data uses the optional item codec"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/network/message/S2CMessageBulletTrail.class",
            b"OPTIONAL_STREAM_CODEC",
            "bullet trail packet uses the optional item codec"))

        # 11. optional-mod compat (HANDOFF section 19). The knife is built through
        # reflection, so the class only carries the target name plus the vanilla
        # attribute helper; the soul fire calls must reference the Prometheus API the
        # 1.21 rewrite moved to (soul_fire_d's own FireManager is gone).
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/init/ModItems.class",
            b"vectorwing.farmersdelight.common.item.KnifeItem",
            "Farmer's Delight knife resolves KnifeItem reflectively"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/init/ModItems.class", b"createAttributes",
            "knife uses DiggerItem.createAttributes (1.21.1 KnifeItem signature)"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/ScorchedGuns.class",
            b"it/crystalnest/prometheus/api/FireManager",
            "soul fire uses the Prometheus fire API"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/block/FakeSoulFireBlock.class",
            b"it/crystalnest/prometheus/api/FireManager",
            "fake soul fire block applies the soul fire type"))

        # 12. physics-structure compatibility (HANDOFF section 20). Projectiles must
        # check the flag and then let Level#clip do the work, because physics mods
        # (Sable) replace BlockGetter#clip rather than adding blocks.
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/ScorchedGuns.class", b"sable",
            "ScorchedGuns probes for the Sable physics mod"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/ScorchedGuns.class", b"physicsStructuresLoaded",
            "ScorchedGuns exposes the physics-structure flag"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/entity/projectile/ProjectileEntity.class",
            b"physicsStructuresLoaded",
            "projectiles delegate their block ray when structures are present"))

        # 13. turret aiming inside a physics structure (HANDOFF section 21). A structure's
        # block entities report plot coordinates while entities report world ones, so the
        # aim/muzzle/effects have to be converted between the two frames.
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/util/PhysicsStructureHelper.class", b"transformPositionInverse",
            "physics structure frame helper is packaged"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/blockentity/TurretBlockEntity.class", b"PhysicsStructureHelper",
            "turret aims and fires in the world frame"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/blockentity/EnemyTurretBlockEntity.class", b"PhysicsStructureHelper",
            "enemy turret aims and fires in the world frame"))

        # 14. shots push physics structures (HANDOFF section 22).
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/util/PhysicsStructureHelper.class", b"applyImpulseAtPoint",
            "physics structures can be pushed by shots"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/entity/projectile/ProjectileEntity.class", b"applyShotImpulse",
            "gun projectiles push structures they hit"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/entity/projectile/turret/TurretProjectileEntity.class", b"applyShotImpulse",
            "turret shells push structures they hit"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/Config$Gameplay.class", b"physicsStructureImpulse",
            "impulse strength is configurable"))

        # 15. the impulse is computed in the STRUCTURE's frame (HANDOFF section 22.6). Sable
        # asks for the normal mass at a point in its own coordinates; feeding it world
        # coordinates made the shot do nothing at all (verified: 3.4e11 instead of 0.014,
        # i.e. an impulse of ~1e-11). So the helper must invert both the point and the
        # normal, and must use Sable's own punch curve and multiplier for the strength.
        helper = "top/ribs/scguns/util/PhysicsStructureHelper.class"
        checks.append(_check_class_contains(
            zf, helper, b"transformNormalInverse",
            "impulse normal is converted into the structure frame"))
        checks.append(_check_class_contains(
            zf, helper, b"punchCurve",
            "impulse strength comes from Sable's own punch curve"))
        checks.append(_check_class_contains(
            zf, helper, b"SUB_LEVEL_PUNCH_STRENGTH_MULTIPLIER",
            "impulse uses Sable's punch strength multiplier"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/Config$Gameplay.class", b"physicsStructureMaxSpeed",
            "structure speed limit is configurable"))

        # 16. the vertex colour reaches the model parts (HANDOFF section 23). 1.21.1 moved the
        # colour into renderToBuffer(..., int color) and added a five argument ModelPart.render.
        # A model that accepts the colour and then calls the four argument version renders the
        # mob but silently drops the tint - which is what made the sulfurhead's translucent gel
        # shell come out opaque. A fixed model contains the five argument descriptor and no
        # longer references the four argument one.
        MODEL_PART_RENDER_5 = (
            b"(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            b"III)V"
        )
        MODEL_PART_RENDER_4 = (
            b"(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            b"II)V"
        )
        for model in ("SulfurheadModel", "CogMinionModel", "TheMerchantModel"):
            checks.append(_check_class_contains(
                zf, "top/ribs/scguns/entity/client/%s.class" % model, MODEL_PART_RENDER_5,
                "%s has the five argument part render" % model))
            checks.append(_check_class_lacks(
                zf, "top/ribs/scguns/entity/client/%s.class" % model, MODEL_PART_RENDER_4,
                "%s passes the vertex colour to its parts" % model))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/entity/client/SulfurheadGelLayer.class", b"entityTranslucent",
            "sulfurhead gel layer renders translucent"))

        # 17. the sulfurhead no longer restricts damage to players (HANDOFF section 23.5). 0.5.5
        # overrode hurt(DamageSource, float) to refuse every non-player source; the user asked
        # for that behaviour to be removed, so the mob uses Mob.hurt like any other monster and
        # the descriptor below must no longer appear in the class. Verified to discriminate: the
        # jar built before the change contains it.
        checks.append(_check_class_lacks(
            zf, "top/ribs/scguns/entity/monster/SulfurheadEntity.class",
            b"(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            "sulfurhead takes damage from every source"))

        # 18. the items declare the 1.21.1 getUseDuration (HANDOFF section 24). 1.21.1 added the
        # LivingEntity parameter; the 1.20.1 one-argument override compiled but overrode nothing,
        # so the engine saw a use duration of 0 and every chargeable item finished instantly -
        # a grenade cooked off in the player's hand on right click instead of being thrown.
        USE_DURATION_1211 = (
            b"(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)I"
        )
        for item in ("GrenadeItem", "MolotovCocktailItem", "SwarmBombItem", "NailBombItem",
                     "GunItem", "HealingBandageItem"):
            checks.append(_check_class_contains(
                zf, "top/ribs/scguns/item/%s.class" % item, USE_DURATION_1211,
                "%s declares the 1.21.1 use duration" % item))

        # 19. the anthralite tools carry attribute modifiers (HANDOFF section 25). 1.20.1 passed
        # them to the tool constructor; 1.21 moved them into the item properties, so a tool built
        # with a bare `new Properties()` has no attack damage or speed at all. The descriptor
        # below is the (Tier, int, float) helper the pickaxe and sword use; it is new in this fix
        # and was verified to be absent from the previous build. The (Tier, float, float) form is
        # NOT checked - the knife already used it before, so such a check could never fail.
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/init/ModItems.class",
            b"(Lnet/minecraft/world/item/Tier;IF)Lnet/minecraft/world/item/component/ItemAttributeModifiers;",
            "ModItems builds tool attribute modifiers"))

        # 20. the block interaction hooks exist (HANDOFF section 30). 1.21.1 replaced
        # BlockBehaviour#use(...) with useItemOn/useWithoutItem, so the port's 14 overrides of the
        # old method became brand new methods nobody calls and those blocks stopped responding to
        # right click - the gun bench, the macerator, the mechanical press, the polar generator and
        # others. A fixed class declares the 1.21.1 hook and no longer declares the removed one;
        # both halves are checked, because a descriptor that is merely present proves little.
        USE_WITHOUT_ITEM = (
            b"(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;"
            b"Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;"
            b"Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"
        )
        USE_ITEM_ON = (
            b"(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;"
            b"Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;"
            b"Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;"
            b"Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/ItemInteractionResult;"
        )
        REMOVED_USE = (
            b"(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;"
            b"Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;"
            b"Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)"
            b"Lnet/minecraft/world/InteractionResult;"
        )
        for block in ("GunBenchBlock", "MaceratorBlock", "PoweredMaceratorBlock",
                      "MechanicalPressBlock", "PolarGeneratorBlock",
                      "MemorialBlock", "ChargedAmethystRelayBlock"):
            checks.append(_check_class_contains(
                zf, "top/ribs/scguns/block/%s.class" % block, USE_WITHOUT_ITEM,
                "%s uses the 1.21.1 empty-hand hook" % block))
        for block in ("GunBenchBlock", "MaceratorBlock", "PolarGeneratorBlock"):
            checks.append(_check_class_lacks(
                zf, "top/ribs/scguns/block/%s.class" % block, REMOVED_USE,
                "%s no longer declares the removed use hook" % block))
        for block in ("AdvancedComposterBlock", "GuanoCandleBlock", "SandbagBlock",
                      "ViciousAcidCauldronBlock", "MineUnitBlock"):
            checks.append(_check_class_contains(
                zf, "top/ribs/scguns/block/%s.class" % block, USE_ITEM_ON,
                "%s uses the 1.21.1 item hook" % block))

        # 21. the mod's enchantments are in vanilla's tag chain (HANDOFF section 31). 1.21 gates the
        # enchanting table by #minecraft:in_enchanting_table and the librarian by
        # #minecraft:tradeable, both built from #minecraft:non_treasure. Without a tag file the mod's
        # enchantments were in none of them, so the table offered nothing and librarians never
        # stocked their books.
        checks.append(_check_resource_contains(
            zf, "data/minecraft/tags/enchantment/non_treasure.json",
            b"scguns:accelerator", "enchantments are in the vanilla tag chain"))
        checks.append(_check_resource_contains(
            zf, "data/minecraft/tags/enchantment/non_treasure.json",
            b"scguns:waterproof", "all mod enchantments are tagged, not just one"))

        # 23. gun_rust is a curse (HANDOFF section 37). 0.5.5 overrode isCurse() and
        # isTreasureOnly() to return true; 1.21 has neither method - a curse is an enchantment in
        # #minecraft:curse, which is what colours its tooltip red and what makes the grindstone
        # refuse to strip it. The third check is negative on purpose: a treasure enchantment must
        # not also sit in non_treasure, because that is the tag the enchanting table and the
        # librarian read.
        checks.append(_check_resource_contains(
            zf, "data/minecraft/tags/enchantment/curse.json", b"scguns:gun_rust",
            "gun rust is in the vanilla curse tag"))
        checks.append(_check_resource_contains(
            zf, "data/minecraft/tags/enchantment/treasure.json", b"scguns:gun_rust",
            "gun rust is treasure-only, as in 0.5.5"))
        checks.append(("gun rust is not also in non_treasure",
                       not _check_resource_contains(
                           zf, "data/minecraft/tags/enchantment/non_treasure.json",
                           b"scguns:gun_rust", "")[1]))
        checks.append(_check_resource_contains(
            zf, "data/minecraft/tags/enchantment/on_random_loot.json", b"scguns:gun_rust",
            "gun rust can still appear on random loot"))

        # 22. the Chinese translation is embedded (HANDOFF section 33). The user had a translation
        # pack enabled for it; the pack covered every English key, so it is shipped inside the mod
        # instead and no longer has to be enabled. The second check looks for Chinese characters
        # rather than another key, so a file that merely exists is not enough to pass.
        checks.append(_check_resource_contains(
            zf, "assets/scguns/lang/zh_cn.json", b'"item.scguns.musket"',
            "embedded Chinese translation ships"))
        checks.append(_check_resource_contains(
            zf, "assets/scguns/lang/zh_cn.json", "\u67aa".encode("utf-8"),
            "embedded Chinese translation really is Chinese"))

        # 23. the gun bench recipe book names the right station (HANDOFF section 42). Vanilla's
        # Recipe#getToastSymbol default is a *crafting table*, so every gun bench recipe announced
        # itself beside a vanilla workbench icon - which the player reported verbatim. Checking the
        # packaged class is the only way to catch this without a screenshot.
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/client/screen/GunBenchRecipe.class", b"getToastSymbol",
            "gun bench recipes name their own station icon"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/client/screen/GunBenchRecipe.class",
            b"top/ribs/scguns/init/ModBlocks",
            "the station icon is the gun bench block"))
        # 25. empty attachment slots must not mark the recipe incomplete (HANDOFF section 42.7). The
        # recipe book drops isIncomplete() recipes before it filters by category, so without this
        # override 137 of the 146 recipes never reach the book at all - only the 9 whose ten slots
        # are all filled survived, which is exactly what the player counted.
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/client/screen/GunBenchRecipe.class", b"isIncomplete",
            "empty attachment slots do not hide the recipe"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/client/screen/GunBenchRecipe.class", b"hasNoItems",
            "the completeness rule mirrors ShapedRecipe"))
        # 26. the blueprint has to be part of the recipe's input list, otherwise the recipe book can
        # neither ghost it nor place it - PlaceRecipe only ever walks getIngredients() (HANDOFF 42.8).
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/client/screen/GunBenchRecipe.class", b"getModuleIngredients",
            "the inputs are split into grid + blueprint"))

        # 27. no gun bench recipe may unlock itself (HANDOFF section 43). The four turret recipes used
        # to carry a minecraft:tick criterion, so they appeared in the recipe book the moment a player
        # logged in - with no blueprint and no turret platform earned. They now key on the turret
        # platform, which they already require as their gun_grip ingredient, so every unlock in the
        # book is something the player actually obtained.
        unlocked_files = [n for n in zf.namelist()
                          if n.startswith("data/scguns/advancement/recipes/") and n.endswith(".json")]
        freeloaders = []
        turrets = []
        for name in unlocked_files:
            body = zf.read(name)
            if b"minecraft:tick" in body:
                freeloaders.append(name)
            stem = name.rsplit("/", 1)[-1]
            if "turret" in stem:
                turrets.append((stem, b"scguns:turret_platform" in body))
        checks.append(("%d gun bench unlocks, none unlock themselves" % len(unlocked_files),
                       bool(unlocked_files) and not freeloaders))
        missing_platform = [stem for stem, has in turrets if not has]
        checks.append(("the 4 turret recipes unlock from the turret platform",
                       len(turrets) == 4 and not missing_platform))

        # 28. the turret and exo suit tabs must be declared in the enum extension (HANDOFF section 44).
        # NeoForge does not scan that file - it is named by enumExtensions= in neoforge.mods.toml - so a
        # category missing from it throws "No enum constant" while the client starts, which is exactly
        # how the first version crashed. Checking the packaged file is the only offline way to see it.
        for category in (b"SCGUNS_GUN_BENCH_SEARCH", b"SCGUNS_GUN_BENCH_MISC",
                         b"SCGUNS_GUN_BENCH_TURRET", b"SCGUNS_GUN_BENCH_EXO_SUIT"):
            checks.append(_check_resource_contains(
                zf, "META-INF/enumextensions.json", category,
                "enum extension declares %s" % category.decode("ascii")))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/client/recipebook/ScgunsRecipeBookCategories.class",
            b"SCGUNS_GUN_BENCH_TURRET", "the client resolves the new tabs at runtime"))
        checks.append(_check_resource_contains(
            zf, "assets/scguns/lang/en_us.json", b'"commands.scguns.recipebook.tabs"',
            "the tab split is reported by /scguns recipebook"))

        # 29. the tab split must match the recipes that actually exist, and the temporary probe that was
        # used to measure it must not ship. Recipes are classified by what they produce: the turret tab
        # by a turret block result, the exo suit tab by an armour piece result.
        turret_ids = {b"scguns:auto_turret", b"scguns:basic_turret",
                      b"scguns:shotgun_turret", b"scguns:sniper_turret"}
        exo_ids = {b"scguns:exo_suit_helmet", b"scguns:exo_suit_chestplate",
                   b"scguns:exo_suit_leggings", b"scguns:exo_suit_boots"}
        turret_recipes = exo_recipes = 0
        for name in names:
            if not name.startswith(DATA) or not name.endswith(".json"):
                continue
            body = zf.read(name)
            if b'"scguns:gun_bench"' not in body:
                continue
            result = re.search(rb'"result"\s*:\s*\{[^}]*"id"\s*:\s*"([^"]+)"', body)
            if not result:
                continue
            if result.group(1) in turret_ids:
                turret_recipes += 1
            elif result.group(1) in exo_ids:
                exo_recipes += 1
        checks.append(("the turret tab holds the 4 turrets (%d)" % turret_recipes, turret_recipes == 4))
        checks.append(("the exo suit tab holds the 4 armour pieces (%d)" % exo_recipes, exo_recipes == 4))

        # 33. the gun enchantment rules (HANDOFF section 50). 0.5.5 expressed both in Java:
        # EnchantmentCategory decided what an enchantment may go on, and
        # GunEnchantment.checkCompatibility made enchantments of the same Type mutually exclusive.
        # 1.21 needs both as data, and neither was there: every exclusive_set was absent (so all
        # enchantments stacked) and corroded - a vanilla WEAPON enchantment - was pointed at the gun
        # tag (so guns could roll it).
        ench_dir = "data/scguns/enchantment/"
        corroded = zf.read(ench_dir + "corroded.json") if (ench_dir + "corroded.json") in names else b""
        checks.append(("corroded is a melee enchantment, not a gun one",
                       b"enchantable/sharp_weapon" in corroded and b"enchantable/guns" not in corroded))
        for kind in (b"weapon", b"ammo", b"projectile", b"reload"):
            entry = "data/scguns/tags/enchantment/exclusive_set/%s.json" % kind.decode("ascii")
            checks.append(_check_resource_contains(zf, entry, b"scguns:",
                                                   "the %s conflict group ships" % kind.decode("ascii")))
        missing_exclusive = []
        for name in names:
            if not name.startswith(ench_dir) or not name.endswith(".json"):
                continue
            body = zf.read(name)
            if b"exclusive_set" not in body:
                missing_exclusive.append(name.rsplit("/", 1)[-1])
        checks.append(("every gun enchantment declares a conflict group [%s]"
                       % ", ".join(missing_exclusive[:3]) if missing_exclusive
                       else "every gun enchantment declares a conflict group",
                       not missing_exclusive))
        # the narrowed categories: 0.5.5's "every gun except <tag>" needs its own tag in 1.21
        for narrowed in (b"trigger_finger", b"shell_catcher", b"collateral"):
            entry = "data/scguns/tags/item/enchantable/%s.json" % narrowed.decode("ascii")
            checks.append(_check_resource_contains(zf, entry, b"scguns:",
                                                   "the narrowed %s tag ships" % narrowed.decode("ascii")))
        checks.append(("the temporary tab probe does not ship",
                       "top/ribs/scguns/debug/RecipeBookTabProbe.class" not in names))

        # 34. loot injection (HANDOFF section 51). NeoForge reads exactly one file for the global loot
        # modifier list - data/neoforge/loot_modifiers/global_loot_modifiers.json, verified with javap
        # on LootModifierManager.prepare - and the port had it under its own namespace instead, so
        # nothing was injected and no modded loot ever appeared in mineshafts or dungeons.
        checks.append(_check_resource_contains(
            zf, "data/neoforge/loot_modifiers/global_loot_modifiers.json", b"scguns:add_loot_dungeon",
            "the global loot modifier list is in the neoforge namespace"))
        checks.append(("the list is not left in the mod's own namespace",
                       "data/scguns/loot_modifiers/global_loot_modifiers.json" not in names))

        # 35. loot tables must use the 1.21 item predicate shape (HANDOFF section 55). 1.20 wrote the
        # silk-touch test as predicate.enchantments; 1.20.5 moved it under predicate.predicates, and
        # Mojang codecs ignore unknown fields, so the old shape parsed into an EMPTY predicate - which
        # matches every tool. Every silk-touch branch fired unconditionally: the supply crate dropped
        # itself instead of ammo and 28 ore/glass tables ignored whether the tool was enchanted.
        legacy_predicates = []
        for name in names:
            if not name.startswith("data/scguns/loot_table/") or not name.endswith(".json"):
                continue
            body = zf.read(name)
            if b'"predicate"' in body and b'"enchantments"' in body and b'"predicates"' not in body:
                legacy_predicates.append(name)
        checks.append(("loot tables use the 1.21 item predicate shape [%d legacy]"
                       % len(legacy_predicates), not legacy_predicates))
        # and the condition type that 1.21 removed
        dead_conditions = [n for n in names
                           if n.startswith("data/scguns/loot_modifiers/") and n.endswith(".json")
                           and b"random_chance_with_looting" in zf.read(n)]
        checks.append(("loot modifiers avoid the condition 1.21 removed [%d]" % len(dead_conditions),
                       not dead_conditions))

        # 36. modded armour must be in vanilla's enchantable item tags (HANDOFF section 56). 1.20.1
        # decided enchantability by EnchantmentCategory, where any ArmorItem matched ARMOR; 1.21 uses
        # these tags, and with none shipped every enchantment found the rolled armour incompatible -
        # so loot tables using enchant_with_levels or enchant_randomly produced bare gear.
        for tag, minimum in (("armor", 30), ("head_armor", 8), ("chest_armor", 6),
                             ("leg_armor", 6), ("foot_armor", 6),
                             ("durability", 30), ("equippable", 30), ("vanishing", 30)):
            entry = "data/minecraft/tags/item/enchantable/%s.json" % tag
            body = zf.read(entry) if entry in names else b""
            checks.append(("%-11s tag carries the mod's armour (%d)"
                           % (tag, body.count(b"scguns:")), body.count(b"scguns:") >= minimum))

        # 37. the durability tag must also carry the guns and attachments, not just armour (HANDOFF
        # section 57). 1.20.1's EnchantmentCategory.BREAKABLE matched every item with durability, so
        # an attachment could hold Mending and be repaired; 1.21 asks this tag instead, and with only
        # armour in it an attachment could not carry the enchantment that repairs it.
        durability = zf.read("data/minecraft/tags/item/enchantable/durability.json") if (
            "data/minecraft/tags/item/enchantable/durability.json" in names) else b""
        checks.append(("durability tag carries guns and attachments (%d entries)"
                       % durability.count(b"scguns:"), durability.count(b"scguns:") >= 150))
        checks.append(("a gun is in the durability tag",
                       b'"scguns:gale"' in durability))
        checks.append(("an attachment is in the durability tag",
                       b'"scguns:long_scope"' in durability or b'"scguns:extended_barrel"' in durability))
        checks.append(("the attachment mending handler ships",
                       "top/ribs/scguns/event/AttachmentMendingHandler.class" in names))

        # 30. the cooldown indicator must stay gone (HANDOFF section 48). 0.5.5's copy built its own
        # GuiGraphics, wrote into the frame's BufferSource and never flushed it, so the bar was never
        # submitted and never appeared in game; a first attempt here "fixed" it into visibility, which
        # was a deviation. The dead texture reference must not come back either - 1.21.1 has no
        # textures/gui/icons.png, so that path only produced FileNotFoundException spam.
        checks.append(_check_class_lacks(
            zf, "top/ribs/scguns/client/handler/GunRenderingHandler.class",
            b"textures/gui/icons.png",
            "no reference to the removed icons sheet"))
        checks.append(("the cooldown indicator does not ship",
                       "top/ribs/scguns/client/handler/GunCooldownOverlay.class" not in names))

        # 31. the maid compat's idle reload must wait before it starts (HANDOFF section 46). The
        # compat is a nested jar, so this reads inside it: the config key and the behavior that
        # consults it both have to be there.
        nested = [n for n in names if n.startswith("META-INF/jarjar/") and "maid_compat" in n]
        if len(nested) != 1:
            checks.append(("the maid compat is nested exactly once (%d)" % len(nested), False))
        else:
            import io

            with zipfile.ZipFile(io.BytesIO(zf.read(nested[0]))) as compat:
                compat_names = set(compat.namelist())
                config = compat.read("com/scg2tlm/elmomod/SCG2TLMConfig.class") if (
                    "com/scg2tlm/elmomod/SCG2TLMConfig.class" in compat_names) else b""
                task = compat.read(
                    "com/scg2tlm/elmomod/compat/task/MaidSC2GunIdleReloadTask.class") if (
                    "com/scg2tlm/elmomod/compat/task/MaidSC2GunIdleReloadTask.class" in compat_names) else b""
                checks.append(("the idle reload delay is configurable",
                               b"idle_reload_delay_ticks" in config))
                checks.append(("the idle reload behavior waits for it",
                               b"IDLE_RELOAD_DELAY" in task))

                # 32. the Cloth Config screen is back (HANDOFF section 47). It was dropped during the
                # port because Cloth Config was in neither the mod nor the instance; it listens to
                # TLM's own AddClothConfigEvent, so the two classes below must be inside the nested jar
                # and the compat must declare cloth_config optional rather than required.
                checks.append(("the compat ships its Cloth Config screen",
                               "com/scg2tlm/elmomod/client/SCG2TLMClothConfig.class" in compat_names
                               and "com/scg2tlm/elmomod/client/SCG2TLMClothConfigListener.class"
                               in compat_names))
                meta = compat.read("META-INF/neoforge.mods.toml") if (
                    "META-INF/neoforge.mods.toml" in compat_names) else b""
                checks.append(("Cloth Config stays an optional dependency",
                               b'cloth_config' in meta and b'type="optional"' in meta))
        checks.append(_check_class_lacks(
            zf, "top/ribs/scguns/common/recipe/ScgunsRecipeBookEnumParameters.class",
            b"GUN_BENCH",
            "the search tab avoids the workbench-looking bench block"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/common/recipe/ScgunsRecipeBookEnumParameters.class",
            b"net/minecraft/world/item/Items",
            "the search tab uses the vanilla search icon instead"))

        # 24. the recipe book diagnostic ships (HANDOFF section 42.5): without it, "the book shows
        # too few recipes" cannot be settled with a number, only with guesswork.
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/init/ModRecipeBookCommand.class", b"recipebook",
            "/scguns recipebook ships"))
        checks.append(_check_resource_contains(
            zf, "assets/scguns/lang/en_us.json", b'"commands.scguns.recipebook.total"',
            "the diagnostic is translated (en_us)"))
        checks.append(_check_resource_contains(
            zf, "assets/scguns/lang/zh_cn.json", b'"commands.scguns.recipebook.total"',
            "the diagnostic is translated (zh_cn)"))

        # 25. attachment wear (HANDOFF section 60). 0.5.5 damaged the stack Gun.getAttachment
        # returns, which is a decode of the gun's tag - the damage went to a throwaway copy, so
        # attachments never wore out. The write-back is what makes it real, and it must survive
        # packaging: the firing path has to call it, and Gun must own it.
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/common/Gun.class", b"setAttachment",
            "Gun can write an attachment back"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/event/GunEventBus.class", b"setAttachment",
            "the wear path writes the damaged attachment back"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/event/GunEventBus.class", b"getTagKey",
            "a broken attachment is removed by its type's tag key"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/util/NbtHelper.class",
            b"(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/HolderLookup$Provider;)",
            "the item codec takes an explicit registry access"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/common/Gun.class", b"getOrCreateTag",
            "the attachment write owns its tag (so it syncs)"))

        # 26. sculk gun XP repair (HANDOFF section 61). 0.5.5 registered the handler for Dist.CLIENT
        # only, so it never ran where the item lives - a sculk gun at 100 damage kept 100 damage after
        # an orb. The class must no longer mention the client dist marker, and it must guard on the
        # client side instead.
        checks.append(_check_class_lacks(
            zf, "top/ribs/scguns/event/GunXpHandler.class", b"net/neoforged/api/distmarker/Dist",
            "the sculk XP repair is no longer client-dist only"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/event/GunXpHandler.class", b"isClientSide",
            "the sculk XP repair runs on the server and guards the client"))

        # 27. aiming after a reload (HANDOFF section 63). The client refuses to aim while
        # InCriticalReloadPhase or a non-NONE ReloadState is set, and it reads both from the server's
        # copy of the item - so the clean-up has to ship in ReloadTracker.
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/common/ReloadTracker.class", b"InCriticalReloadPhase",
            "the reload tracker clears the critical reload phase"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/common/ReloadTracker.class",
            b"Clearing a reload state that outlived its reload",
            "a stale stop state is cleaned up and logged"))

        # 28. a manual (winnie-style) reload must actually finish (HANDOFF section 66). The stop
        # grace period used to live in the gun's tag; the value read back next tick was always 0, so
        # the reload never completed and the player-level RELOADING flag stayed set - every gun then
        # animated as if it were reloading.
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/common/ReloadTracker.class", b"pendingManualStopSince",
            "the manual reload's stop grace period lives in memory"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/common/ReloadTracker.class", b"endReload",
            "a reload ends when its gun leaves the hand"))

        # 29. the reload completion must actually reach the item (HANDOFF section 67). The upstream
        # 1.21.1 port writes the tag back explicitly after mutating it; without that the removals and
        # the STOPPING state were read back as absent, so the client never played the chambering
        # animation and RELOADING stayed set.
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/common/ReloadTracker.class", b"util/NbtHelper",
            "the reload tracker writes its tag changes back"))

        # 30. a gun that is not in the selected slot must not keep reload state (HANDOFF section 68):
        # that state made stored guns play the reload/stop animation, and it is also how guns damaged by
        # older builds get repaired.
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/item/animated/AnimatedGunItem.class", b"hasReloadState",
            "a gun outside the selected slot cleans its reload state"))

        # 31. "which gun is in my hands?" (HANDOFF section 69). GeckoLib 4.6 keeps the per-stack
        # animatable id in a data component and GeoItem.getId() falls back to Long.MAX_VALUE without
        # it, so the port's legacy "GeckoLibID" NBT key left every gun with the same id: the held-gun
        # check was false for all of them, the whole inventory ran the held-gun state machine, and the
        # reload animation played on every gun in the backpack. The id must be assigned through
        # GeckoLib, and the branch must be chosen from the slot the stack was ticked from - hence both
        # the presence of getOrAssignId and the absence of the dead key.
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/item/animated/AnimatedGunItem.class", b"getOrAssignId",
            "guns get a GeckoLib 4.6 animatable id"))
        checks.append(_check_class_lacks(
            zf, "top/ribs/scguns/item/animated/AnimatedGunItem.class", b"GeckoLibID",
            "the dead GeckoLibID NBT key is gone"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/item/animated/AnimatedGunItem.class",
            b"(Lnet/minecraft/nbt/CompoundTag;Lsoftware/bernie/geckolib/animation/AnimationController;"
            b"Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Z)V",
            "the not-held path takes the leaves-the-hand flag"))
        checks.append(_check_packaged_tree_lacks(
            zf, b"GeckoLibID", "no other class writes the dead GeckoLib id key"))

        # 32. the NeoForge version players are asked for (HANDOFF section 70). The range is expanded
        # from gradle.properties into the shipped metadata, so a typo there is shipped silently - and
        # the nested maid compat carries its own copy, which went stale once already.
        floor = re.search(r"^neo_version_range\s*=\s*\[?([0-9][^,)\]]*)",
                          pathlib.Path("gradle.properties").read_text(encoding="utf-8"), re.M)
        floor = floor.group(1) if floor else None
        checks.append(_check_neoforge_floor(
            zf, "[%s,)" % floor, "the host asks for the NeoForge floor we claim"))
        checks.append(_check_nested_mods_toml(
            zf, ('versionRange="[%s,)"' % floor).encode(),
            "the nested maid compat asks for the same floor"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/init/ModCapabilities.class",
            b"Lnet/neoforged/fml/common/EventBusSubscriber$Bus;",
            "capability registration names the mod bus"))

        # 33. the packaged version, which is also the jar name (HANDOFF section 73).
        checks.append(_check_declared_version(zf))

        # 34. the raid-check diagnostic (HANDOFF section 79). A natural raid rolls at dusk and only
        # starts at 18000, and every way it can fail is silent - a failed roll, a creative target, raid
        # level 0, the sea-level gate, no open surface column, a raid already running - so "no raid
        # tonight" was untestable without waiting out the whole night per attempt. `/scguns raid check`
        # reports those decisions on demand; it ships with both language files, because a missing key
        # prints a raw id instead of an answer.
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/init/ModCommands.class", b"commands.scguns.raid.check",
            "/scguns raid check ships"))
        for locale in ("en_us", "zh_cn"):
            checks.append(_check_resource_contains(
                zf, "assets/scguns/lang/%s.json" % locale,
                b'"commands.scguns.raid.check.gate_fail"',
                "the raid check is translated (%s)" % locale))
        checks.append(_check_resource_contains(
            zf, "assets/scguns/lang/en_us.json", b'"commands.scguns.raid.check.howto"',
            "the raid check prints the fast test recipe"))
        # The probe that exercised the report on a dedicated server ran as a fake player, printed its
        # four scenarios, and was deleted again - like the pose diagnostic, it must not ship.
        checks.append((
            "the temporary raid-check probe removed from the build",
            "top/ribs/scguns/entity/raid/RaidCheckProbe.class" not in names,
        ))

        # 35. the raid cooldown actually runs (HANDOFF section 80). minDaysBetweenRaids shipped from
        # 0.5.5 as a config option that nothing read - RaidSaveData.canScheduleRaid and setLastRaidDay
        # had no caller at all - so a raid came every night the roll succeeded. Both halves have to be in
        # the packaged bytecode: the nightly scheduler consulting the cooldown, and the day being
        # recorded when a raid really starts.
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/entity/raid/RaidManager.class", b"canScheduleRaid",
            "the nightly raid consults the minDaysBetweenRaids cooldown"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/entity/raid/RaidManager.class", b"setLastRaidDay",
            "a natural raid records the day it started"))

        # 36. the two halves of the gunner-reload fix (HANDOFF section 81).
        # a) The gun a wild gunner is given must carry AmmoCount: the pre-fill sat behind a guard that
        #    is always false for a freshly created stack, so pillagers and friends held a gun with no
        #    data at all and reloaded before they ever fired. getTagForWrite must be gone from that call.
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/config/GunnerMobSpawner.class", b"getOrCreateTag",
            "a wild gunner's gun is given AmmoCount"))
        checks.append(_check_class_lacks(
            zf, "top/ribs/scguns/config/GunnerMobSpawner.class", b"getTagForWrite",
            "the always-false guard is gone from the gunner gun"))
        # b) Reading a client-only option on the server throws on NeoForge, and in the mob's firing path
        #    that throw landed before the ammo was spent - so the magazine never emptied and the AI never
        #    reloaded, on top of killing the server. Every server-side read goes through Config.clientOr.
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/Config.class", b"clientOr",
            "Config.clientOr exists for server-side reads of client options"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/entity/ai/AIGunEvent.class", b"clientOr",
            "the mob firing path reads fireLights through it"))

        # 37. the Guard Villagers integration (HANDOFF section 82). Optional mod, single jar: guards carry
        # and fire the mod's guns, and the four classes must ship - the data entry alone would arm nobody,
        # and the compat class alone would have no weapon list. GuardFriendlyRules is the only class that
        # may name a Guard Villagers type, and it must be there for the friendly-fire check to work.
        #
        # The friendly-fire gate itself lives on the projectile funnel: the layers the reference
        # implementations tried first - vanilla Projectile.canHitEntity, the base onHitEntity, the two
        # entity searches - all miss projectiles this mod actually fires, which is why the standalone
        # 1.20.1 compat's protection did nothing. getHitResult is the funnel every path goes through, and
        # the mixin that hooks it has to be both in the jar and listed in the packaged mixin config.
        for entry in ("compat/guardvillagers/GuardVillagersCompat.class",
                      "compat/guardvillagers/GuardFriendlyRules.class",
                      "compat/guardvillagers/GuardGunAttackGoal.class",
                      "compat/guardvillagers/GuardVillagersEvents.class",
                      "mixin/common/compat/guardvillagers/GuardProjectileHitMixin.class"):
            checks.append(("the guard compat ships (%s)" % entry.rsplit("/", 1)[-1],
                           "top/ribs/scguns/" + entry in names))
        checks.append(_check_resource_contains(
            zf, "data/scguns/entity/gunner_mobs.json", b'"guardvillagers:guard"',
            "guards are configured as gunners"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/config/GunnerMobSpawner.class", b"equipGuardGun",
            "the guard equip hook ships"))
        checks.append(_check_class_contains(
            zf, "top/ribs/scguns/mixin/common/compat/guardvillagers/GuardProjectileHitMixin.class",
            b"isFriendlyShot", "the guard friendly-fire gate calls the shared ally test"))
        checks.append((
            "the mixin is listed in the packaged mixin config",
            "common.compat.guardvillagers.GuardProjectileHitMixin" in (read("scguns.mixins.json") or "")))

    failures = [label for label, ok in checks if not ok]
    for label, ok in checks:
        print("  %-48s %s" % (label, "OK" if ok else "FAIL"))
    print("")
    print("%d/%d checks passed" % (len(checks) - len(failures), len(checks)))
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
