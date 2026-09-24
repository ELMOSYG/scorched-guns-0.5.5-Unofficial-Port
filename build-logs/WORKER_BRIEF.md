# Worker brief — Scorched Guns 0.5.5 → MC 1.21.1 / NeoForge 21.1.249 port

You are fixing a slice of a large mechanical port. Read this whole file first.

## Where things are

| What | Path |
|---|---|
| Project root (your working dir) | `E:\mod\scgun-0.5.5-1.21.1-neoforge` |
| Sources to fix | `src\main\java\top\ribs\scguns\` |
| **Your error list** | `build-logs\errors-110.txt` (435 errors; grep it for your directory) |
| **1.21.1 API ground truth** | `.refs\nf-src\` — the extracted NeoForge 21.1.249 + vanilla sources jar (7106 .java). Read the real class. |
| Compiled classes (javap) | `build\moddev\artifacts\neoforge-21.1.249-merged.jar` |
| **Original 1.20.1 code** | `E:\mod\SG2-1.21\.sg055_deobf\` — decompiled 0.5.5, SRG names remapped to official names (994 .java). Use for intended behaviour. |
| Dependency jars (GeckoLib, Framework, Curios, JEI…) | `libs\` |

`grep` the error file with the tool, e.g. pattern `^top/ribs/scguns/client/screen/`.

## Rules

1. **Own only your assigned files.** Do not edit anything outside your scope — not even to add a helper class. If a correct fix needs a change outside your scope, write it in your final report instead of making it.
2. **Never run gradle.** The parent agent compiles centrally; concurrent gradle runs corrupt each other. Do not run `compileJava`, `build`, or any gradle task.
3. **Never edit `build.gradle`** or `gradle.properties`.
4. **Look up the real signature** in `.refs\nf-src` before writing it. Do not guess from memory — this port has already been burned by remembered signatures.
5. **Preserve 1.20.1 behaviour.** Check `.sg055_deobf` for what the code was doing. Prefer the smallest change that keeps the same semantics and the same public API surface of the mod's own classes.
6. Do not introduce new errors in your own files (watch imports, generics, and nullability annotations).
7. If a whole error category is fixable by a general rule across your files, apply the rule consistently.

## Established design decisions — do NOT undo these

- **Item NBT** lives in `DataComponents.CUSTOM_DATA` behind `top.ribs.scguns.util.NbtHelper`. `getTag` / `getOrCreateTag` return the **live** tag (mutate-in-place must keep working) and the original NBT key names (`AmmoCount`, `HeatLevel`, `IsShooting`, …) are preserved because downstream mods read them.
- **Capabilities**: `util\Caps.java` + `init\ModCapabilities.java` replace Forge's `ICapabilityProvider`/`LazyOptional`. Block entities keep a nullable `getCapability(Object cap, Direction side)`.
- **Enchantments are datapack entries**: `init\ModEnchantments.java` holds 15 `ResourceKey<Enchantment>` constants; behaviour lives in `util\ScEnchants.java`; the JSON is generated. `enchantment\CorrodedEnchantment.java` is a plain static behaviour class.
- **Networking**: the 49 message classes in `network\message\` are unchanged 1.20.1 classes; `network\FrameworkMessageBridge.java` adapts them (by reflection) to Framework 0.13 `StreamCodec`, and `network\PacketHandler.java` is **generated**. `MessageContext.getPlayer()` returns `Optional<Player>`. Do not restructure this.
- **Recipes**: `common\recipe\LegacyRecipeCodec.java` (feeds the original `fromJson` a `JsonObject`), `common\recipe\ScRecipeSerializer.java`, `common\recipe\ContainerRecipeInput.java` (wraps a `Container` as a 1.21 `RecipeInput`).
- Several files (`util\NbtHelper`, `util\Caps`, `util\ScEnchants`, `network\*`, `init\ModCapabilities`, …) are **hand-authored compat layers**: preserve their javadoc and intent; fix only what the compiler actually complains about.

## Known-good translations already applied elsewhere (reuse these patterns)

| 1.20.1 / Forge | 1.21.1 / NeoForge |
|---|---|
| `stack.getTag()/getOrCreateTag()/setTag(t)` | `NbtHelper.getTag(stack)` / `getOrCreateTag(stack)` / `setTag(stack, t)` |
| `stack.save(new CompoundTag())` | `NbtHelper.tagFromItem(stack)` |
| `ItemStack.hurtAndBreak(n, entity, slot)` | third arg is an `EquipmentSlot`; from a hand use `LivingEntity.getSlotForHand(entity.getUsedItemHand())`, from an armor stack use `entity.getEquipmentSlotForItem(stack)` |
| `appendHoverText(ItemStack, Level, List<Component>, TooltipFlag)` | `appendHoverText(ItemStack, Item.TooltipContext, List<Component>, TooltipFlag)` — `Item.TooltipContext.of(level)` when you only have a `Level`; needs `import net.minecraft.world.item.Item;` |
| `isValidBonemealTarget(level, pos, state, isClient)` | `isValidBonemealTarget(LevelReader, BlockPos, BlockState)` |
| `ignoreExplosion()` | `ignoreExplosion(Explosion)` — inside an `Explosion` subclass pass `this`; in a hand-written explosion loop with no `Explosion` use `top.ribs.scguns.util.ExplosionHelper.ignoresExplosion(entity)` |
| `TickEvent.RenderTickEvent` + `Phase.START/END` | `net.neoforged.neoforge.client.event.RenderFrameEvent.Pre` / `.Post` |
| `RenderGuiOverlayEvent` | `net.neoforged.neoforge.client.event.RenderGuiLayerEvent` |
| `Minecraft#getDeltaFrameTime()` | `mc.getTimer().getGameTimeDeltaPartialTick(false)` |
| `EntityDimensions.height/.width` (fields) | `height()` / `width()` |
| `new ResourceLocation(a, b)` | `ResourceLocation.fromNamespaceAndPath(a, b)`; `new ResourceLocation(s)` → `ResourceLocation.parse(s)` |
| tick events | `net.neoforged.neoforge.event.tick.{Level,Player,Server}TickEvent.Pre/.Post`, `net.neoforged.neoforge.client.event.ClientTickEvent.Pre/.Post` |

## Report format

When done, report briefly: files changed, error count you believe you fixed, and **anything you could not fix** with the exact reason (missing API, needs a change outside your scope, needs a design decision).
