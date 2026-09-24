"""Forge 1.20.1 -> NeoForge 1.21.1 rewrite for the maid compat sources.

The import/registry tables are the same ones tools/port_rewrite.py used for the main port; the
extra entries here are the renames that only the compat hits, each verified against
neoforge-21.1.249-merged.jar with javap (see HANDOFF section 36):

  FMLJavaModLoadingContext   - gone; the @Mod class must take injected constructor parameters
  ToolActions                - renamed to ItemAbilities
  LivingHurtEvent            - replaced by LivingIncomingDamageEvent / LivingDamageEvent
  TickEvent.PlayerTickEvent  - replaced by PlayerTickEvent.Pre / PlayerTickEvent.Post
  LazyOptional/NonNullConsumer - NeoForge capabilities return a nullable value directly

Anything that is not a straight rename is deliberately NOT rewritten here: it is left for the
compiler to report, so every remaining case is inspected instead of guessed.

Usage:
    python tools/port_rewrite_maid.py [--dry-run]
"""
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    'maid-compat', 'src', 'main', 'java')

# Longest first: 'net.minecraftforge.fml.common.Mod.EventBusSubscriber' must win over '...Mod'.
IMPORT_RENAMES = [
    ("net.minecraftforge.fml.common.Mod.EventBusSubscriber", "net.neoforged.fml.common.EventBusSubscriber"),
    ("net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext", None),  # handled by hand
    ("net.minecraftforge.event.entity.living.LivingHurtEvent", "net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent"),
    ("net.minecraftforge.eventbus.api.SubscribeEvent", "net.neoforged.bus.api.SubscribeEvent"),
    ("net.minecraftforge.eventbus.api.Event", "net.neoforged.bus.api.Event"),
    ("net.minecraftforge.api.distmarker.Dist", "net.neoforged.api.distmarker.Dist"),
    ("net.minecraftforge.common.MinecraftForge", "net.neoforged.neoforge.common.NeoForge"),
    ("net.minecraftforge.common.ForgeConfigSpec", "net.neoforged.neoforge.common.ModConfigSpec"),
    ("net.minecraftforge.common.ToolActions", "net.neoforged.neoforge.common.ItemAbilities"),
    ("net.minecraftforge.common.capabilities.ForgeCapabilities", "net.neoforged.neoforge.capabilities.Capabilities"),
    ("net.minecraftforge.common.capabilities.", "net.neoforged.neoforge.capabilities."),
    ("net.minecraftforge.common.util.FakePlayer", "net.neoforged.neoforge.common.util.FakePlayer"),
    ("net.minecraftforge.common.util.", "net.neoforged.neoforge.common.util."),
    ("net.minecraftforge.common.", "net.neoforged.neoforge.common."),
    ("net.minecraftforge.registries.ForgeRegistries", None),  # handled by REGISTRY_MAP below
    ("net.minecraftforge.registries.", "net.neoforged.neoforge.registries."),
    ("net.minecraftforge.fml.common.Mod", "net.neoforged.fml.common.Mod"),
    ("net.minecraftforge.fml.ModList", "net.neoforged.fml.ModList"),
    ("net.minecraftforge.fml.ModLoadingContext", None),  # handled by hand
    ("net.minecraftforge.fml.config.ModConfig", "net.neoforged.fml.config.ModConfig"),
    ("net.minecraftforge.fml.loading.FMLEnvironment", "net.neoforged.fml.loading.FMLEnvironment"),
    ("net.minecraftforge.fml.", "net.neoforged.fml."),
    ("net.minecraftforge.items.", "net.neoforged.neoforge.items."),
    ("net.minecraftforge.energy.", "net.neoforged.neoforge.energy."),
    ("net.minecraftforge.event.entity.player.PlayerInteractEvent", "net.neoforged.neoforge.event.entity.player.PlayerInteractEvent"),
    ("net.minecraftforge.event.entity.living.", "net.neoforged.neoforge.event.entity.living."),
    ("net.minecraftforge.event.entity.", "net.neoforged.neoforge.event.entity."),
    ("net.minecraftforge.event.", "net.neoforged.neoforge.event."),
    ("net.minecraftforge.client.ConfigScreenHandler", None),  # Cloth Config screen, dropped
    ("net.minecraftforge.client.", "net.neoforged.neoforge.client."),
]

REGISTRY_MAP = {
    "ForgeRegistries.ITEMS": "BuiltInRegistries.ITEM",
    "ForgeRegistries.BLOCKS": "BuiltInRegistries.BLOCK",
    "ForgeRegistries.ENTITY_TYPES": "BuiltInRegistries.ENTITY_TYPE",
    "ForgeRegistries.SOUND_EVENTS": "BuiltInRegistries.SOUND_EVENT",
    "ForgeRegistries.PARTICLE_TYPES": "BuiltInRegistries.PARTICLE_TYPE",
    "ForgeRegistries.MOB_EFFECTS": "BuiltInRegistries.MOB_EFFECT",
    "ForgeRegistries.POTIONS": "BuiltInRegistries.POTION",
    "ForgeRegistries.ATTRIBUTES": "BuiltInRegistries.ATTRIBUTE",
    "ForgeRegistries.ENCHANTMENTS": "BuiltInRegistries.ENCHANTMENT",
    "ForgeRegistries.DATA_COMPONENT_TYPES": "BuiltInRegistries.DATA_COMPONENT_TYPE",
}

IDENT_RENAMES = [
    ("MinecraftForge", "NeoForge"),
    ("ForgeConfigSpec", "ModConfigSpec"),
    ("ForgeCapabilities", "Capabilities"),
    ("LivingHurtEvent", "LivingIncomingDamageEvent"),
    ("ToolActions", "ItemAbilities"),
]

# 1.21 API moves that are still pure renames. Each was confirmed by the compiler: the first pass
# reported exactly these symbols, and the replacement is the 1.21.1 name for the same thing.
REGEX_RENAMES = [
    # Registry#getValue(ResourceLocation) is Registry#get in 1.21.
    (re.compile(r'(BuiltInRegistries\.\w+)\.getValue\('), r'\1.get('),
    # A fully qualified ForgeRegistries use (not an import) has to lose its package first so the
    # REGISTRY_MAP pass below can see it.
    (re.compile(r'net\.neoforged\.neoforge\.registries\.ForgeRegistries\.'), 'ForgeRegistries.'),
    (re.compile(r'net\.neoforged\.neoforge\.registries\.BuiltInRegistries\.'), 'BuiltInRegistries.'),
    # Enchantment tooltip/compare helpers: tags became components.
    (re.compile(r'\bisSameItemSameTags\('), 'isSameItemSameComponents('),
    # Forge's NBT convenience accessors live in the port's NbtHelper.
    (re.compile(r'([A-Za-z_][\w.]*(?:\([^()]*\))?)\.getOrCreateTag\(\)'), r'NbtHelper.getOrCreateTag(\1)'),
    (re.compile(r'([A-Za-z_][\w.]*(?:\([^()]*\))?)\.getTag\(\)'), r'NbtHelper.getTag(\1)'),
]

# Imports that must disappear rather than move: the type is gone (or the class now lives in a
# vanilla package that the file already imports).
DROP_IMPORTS = [
    "net.neoforged.neoforge.registries.ForgeRegistries",
    "net.neoforged.neoforge.registries.BuiltInRegistries",
]

ALWAYS_IMPORTS = [
    ("NbtHelper.", "top.ribs.scguns.util.NbtHelper"),
    ("@EventBusSubscriber", "net.neoforged.fml.common.EventBusSubscriber"),
]


def ensure_import(text, fqcn):
    line = "import %s;" % fqcn
    if line in text:
        return text
    m = re.search(r"^package\s+[\w.]+;\s*$", text, re.M)
    if not m:
        return text
    return text[:m.end()] + "\n\n" + line + text[m.end():]


def process(path, text):
    stats = {}
    for old, new in IMPORT_RENAMES:
        if new and old in text:
            stats['import:' + old] = text.count(old)
            text = text.replace(old, new)
    for old, new in REGISTRY_MAP.items():
        if old in text:
            stats['reg:' + old] = text.count(old)
            text = text.replace(old, new)
    for old, new in IDENT_RENAMES:
        n = len(re.findall(r'\b' + old + r'\b', text))
        if n:
            stats['ident:' + old] = n
            text = re.sub(r'\b' + old + r'\b', new, text)
    for pattern, replacement in REGEX_RENAMES:
        text, n = pattern.subn(replacement, text)
        if n:
            stats['regex:' + pattern.pattern[:40]] = n

    # @Mod.EventBusSubscriber -> @EventBusSubscriber (NeoForge moved it out of Mod)
    if '@Mod.EventBusSubscriber' in text:
        stats['subscriber'] = text.count('@Mod.EventBusSubscriber')
        text = text.replace('@Mod.EventBusSubscriber', '@EventBusSubscriber')
    if 'EventBusSubscriber.Bus.FORGE' in text:
        stats['bus:FORGE'] = text.count('EventBusSubscriber.Bus.FORGE')
        text = text.replace('EventBusSubscriber.Bus.FORGE', 'EventBusSubscriber.Bus.GAME')
    text = re.sub(r'\bMod\.EventBusSubscriber\.Bus\.', 'EventBusSubscriber.Bus.', text)

    for fqcn in DROP_IMPORTS:
        line = "import %s;\n" % fqcn
        if line in text:
            stats['drop:' + fqcn] = 1
            text = text.replace(line, "")

    if "BuiltInRegistries." in text:
        text = ensure_import(text, "net.minecraft.core.registries.BuiltInRegistries")
    for marker, fqcn in ALWAYS_IMPORTS:
        if marker in text:
            text = ensure_import(text, fqcn)
    if "ForgeRegistries" in text:
        stats['leftover:ForgeRegistries'] = text.count('ForgeRegistries')
    return text, stats


def main():
    dry = '--dry-run' in sys.argv
    totals = {}
    touched = 0
    for root, _dirs, names in os.walk(ROOT):
        for name in names:
            if not name.endswith('.java'):
                continue
            path = os.path.join(root, name)
            text = open(path, encoding='utf-8').read()
            new, stats = process(path, text)
            if new != text:
                touched += 1
                if not dry:
                    open(path, 'w', encoding='utf-8', newline='').write(new)
                for key, count in stats.items():
                    totals[key] = totals.get(key, 0) + count
    print('%s %d file(s)' % ('would rewrite' if dry else 'rewrote', touched))
    for key in sorted(totals):
        print('  %4d  %s' % (totals[key], key))
    return 0


if __name__ == '__main__':
    sys.exit(main())
