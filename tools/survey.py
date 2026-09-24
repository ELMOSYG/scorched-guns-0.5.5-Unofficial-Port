"""Survey the decompiled 0.5.5 sources: which APIs are used, how often.

Guides the port: every external package that changed between 1.20.1/Forge and
1.21.1/NeoForge shows up here with a call-site count.
"""
from __future__ import annotations

import collections
import os
import re
import sys

SRC = r"E:\mod\scgun-0.5.5-1.21.1-neoforge\src\main\java"

IMPORT_RE = re.compile(r"^import\s+(static\s+)?([\w.$]+);", re.M)

files = []
for dirpath, _, filenames in os.walk(SRC):
    for fn in filenames:
        if fn.endswith(".java"):
            files.append(os.path.join(dirpath, fn))
print(f"{len(files)} java files\n")

pkg = collections.Counter()
file_for_pkg = collections.defaultdict(set)
texts = {}
for f in files:
    text = open(f, encoding="utf-8", errors="replace").read()
    texts[f] = text
    for _static, imp in IMPORT_RE.findall(text):
        parts = imp.split(".")
        # only external roots matter
        if parts[0] in ("java", "javax"):
            continue
        key = ".".join(parts[:4]) if parts[0] == "net" else ".".join(parts[:3])
        pkg[key] += 1
        file_for_pkg[key].add(f)

print("==== import prefixes (count=import statements, files=distinct files) ====")
for k, v in sorted(pkg.items(), key=lambda kv: -kv[1]):
    if v < 2:
        continue
    print(f"{v:5d}  {len(file_for_pkg[k]):4d}f  {k}")

PATTERNS = {
    "forge getOrCreateTag": r"\.getOrCreateTag\(\)",
    "forge getOrCreateTagElement": r"\.getOrCreateTagElement\(",
    "ItemStack.getTag()": r"\.getTag\(\)",
    "ItemStack.setTag(": r"\.setTag\(",
    "hurtAndBreak": r"\.hurtAndBreak\(",
    "ResourceLocation ctor": r"new ResourceLocation\(",
    "RegistryObject": r"\bRegistryObject\b",
    "DeferredRegister": r"\bDeferredRegister\b",
    "ForgeRegistries": r"\bForgeRegistries\b",
    "SimpleChannel": r"\bSimpleChannel\b",
    "PacketDistributor": r"\bPacketDistributor\b",
    "NetworkEvent": r"\bNetworkEvent\b",
    "Capability": r"\bCapabilit(y|ies)\b",
    "IItemHandler": r"\bIItemHandler\b",
    "ForgeConfigSpec": r"\bForgeConfigSpec\b",
    "MinecraftForge.EVENT_BUS": r"MinecraftForge\.EVENT_BUS",
    "DistExecutor": r"\bDistExecutor\b",
    "@Mod.EventBusSubscriber": r"@Mod\.EventBusSubscriber",
    "defineSynchedData()": r"defineSynchedData\(\s*\)",
    "entityData.define": r"entityData\.define\(",
    "AttributeModifier": r"\bAttributeModifier\b",
    "geckolib": r"software\.bernie\.geckolib",
    "framework": r"com\.mrcrayfish\.framework",
    "curios": r"top\.theillusivec4\.curios",
    "jei": r"\bmezz\.jei\b",
    "create": r"\bcom\.simibubi\.create\b",
    "mekanism": r"\bmekanism\b",
    "immersiveengineering": r"\bblusunrize\.immersiveengineering\b",
    "farmersdelight": r"\bvectorwing\.farmersdelight\b",
    "controllable": r"\bcom\.mrcrayfish\.controllable\b",
    "soul_fire_d": r"\bsoul_fire_d\b",
    "backpacked": r"\bcom\.mrcrayfish\.backpacked\b",
    "createoreexcavation": r"\bcreateoreexcavation\b",
    "guardvillagers": r"\bguardvillagers\b",
    "mixin": r"org\.spongepowered\.asm\.mixin",
    "RenderType/RenderState": r"\bRenderType\b",
    "GuiGraphics": r"\bGuiGraphics\b",
    "blit(": r"\.blit\(",
    "LootModifier": r"\bLootModifier\b",
    "GlobalLootModifier": r"\bGlobalLootModifier\b",
    "data gen": r"\bDataGenerator\b|\bGatherDataEvent\b",
    "ForgeConfigSpec.Builder": r"\bForgeConfigSpec\.Builder\b",
    "ParticleType": r"\bParticleType\b",
    "ParticleOptions": r"\bParticleOptions\b",
    "RecipeSerializer": r"\bRecipeSerializer\b",
    "RecipeType": r"\bRecipeType\b",
    "MenuType": r"\bMenuType\b",
    "Attribute fields": r"\bAttributes\.",
    "SoundEvent": r"\bSoundEvent\b",
    "DamageSource": r"\bDamageSource\b",
    "Holder<": r"Holder<",
    "TagKey": r"\bTagKey\b",
    "creative tab": r"\bCreativeModeTab\b",
    "structure": r"\bStructure\b|\bStructurePiece\b|\bStructureType\b",
    "registerGoals": r"registerGoals\(",
    "EntityDataAccessor": r"\bEntityDataAccessor\b",
    "IForgeItem": r"\bIForgeItem\b",
    "IForgeItemStack": r"\bIForgeItemStack\b",
    "ForgeEventFactory": r"\bForgeEventFactory\b",
    "networking payload": r"\bCustomPacketPayload\b",
}

print("\n==== API pattern hits ====")
for label, pat in PATTERNS.items():
    rx = re.compile(pat)
    nf = 0
    nh = 0
    for f, text in texts.items():
        hits = rx.findall(text)
        if hits:
            nf += 1
            nh += len(hits)
    if nf:
        print(f"{nh:6d} hits {nf:4d}f  {label}")

# package sizes inside the mod
print("\n==== mod package sizes ====")
modpkg = collections.Counter()
for f in files:
    rel = os.path.relpath(f, SRC).replace("\\", "/")
    p = rel.split("/")
    key = "/".join(p[:4]) if len(p) > 4 else "/".join(p[:-1])
    modpkg[key] += 1
for k, v in sorted(modpkg.items(), key=lambda kv: -kv[1])[:40]:
    print(f"{v:5d}  {k}")
