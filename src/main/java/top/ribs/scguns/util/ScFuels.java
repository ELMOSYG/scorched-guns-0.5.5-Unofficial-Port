package top.ribs.scguns.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Map;

/**
 * Fuel burn times for the 1.21.1 port.
 *
 * <p>Forge let mods query the vanilla fuel map with
 * {@code ForgeHooks.getBurnTime(stack, recipeType)}. NeoForge 1.21.1 has no
 * queryable fuel map - {@code FuelValues} only arrived in 1.21.2 - and the item
 * side hook {@code getBurnTime(ItemStack, RecipeType)} only covers items that
 * opt in. So the NeoForge hook is consulted first and the vanilla fuels Scorched
 * Guns machines accept are listed here as the fallback.</p>
 */
public final class ScFuels {
    private static final Map<Item, Integer> VANILLA_FUELS = Map.ofEntries(
            Map.entry(Items.COAL, 1600),
            Map.entry(Items.CHARCOAL, 1600),
            Map.entry(Items.COAL_BLOCK, 16000),
            Map.entry(Items.BLAZE_ROD, 2400),
            Map.entry(Items.LAVA_BUCKET, 20000),
            Map.entry(Items.DRIED_KELP_BLOCK, 4001),
            Map.entry(Items.STICK, 100),
            Map.entry(Items.BAMBOO, 50),
            Map.entry(Items.DEAD_BUSH, 100),
            Map.entry(Items.CACTUS, 100),
            Map.entry(Items.SCAFFOLDING, 400),
            Map.entry(Items.BOWL, 100),
            Map.entry(Items.CRAFTING_TABLE, 300),
            Map.entry(Items.CHEST, 300),
            Map.entry(Items.BOOKSHELF, 300),
            Map.entry(Items.LADDER, 300),
            Map.entry(Items.BEEHIVE, 300),
            Map.entry(Items.BEE_NEST, 300),
            Map.entry(Items.NOTE_BLOCK, 300),
            Map.entry(Items.JUKEBOX, 300),
            Map.entry(Items.CARTOGRAPHY_TABLE, 300),
            Map.entry(Items.FLETCHING_TABLE, 300),
            Map.entry(Items.SMITHING_TABLE, 300),
            Map.entry(Items.LOOM, 300),
            Map.entry(Items.COMPOSTER, 300),
            Map.entry(Items.BARREL, 300),
            Map.entry(Items.LECTERN, 300),
            Map.entry(Items.CAULDRON, 2000),
            Map.entry(Items.MAGMA_BLOCK, 30000),
            Map.entry(Items.BLAZE_POWDER, 1200),
            Map.entry(Items.DRIED_KELP, 200)
    );

    private ScFuels() {
    }

    /** Burn time in ticks, 0 when the stack is not a fuel. */
    public static int burnTime(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        int itemHook = stack.getItem().getBurnTime(stack, RecipeType.SMELTING);
        if (itemHook > 0) {
            return itemHook;
        }
        Integer vanilla = VANILLA_FUELS.get(stack.getItem());
        if (vanilla != null) {
            return vanilla;
        }
        // planks, logs, saplings, wool and wooden items share a tag based time
        if (stack.is(net.minecraft.tags.ItemTags.PLANKS) || stack.is(net.minecraft.tags.ItemTags.LOGS)
                || stack.is(net.minecraft.tags.ItemTags.SAPLINGS) || stack.is(net.minecraft.tags.ItemTags.WOOL)
                || stack.is(net.minecraft.tags.ItemTags.WOODEN_SLABS)
                || stack.is(net.minecraft.tags.ItemTags.WOODEN_STAIRS)
                || stack.is(net.minecraft.tags.ItemTags.WOODEN_FENCES)
                || stack.is(net.minecraft.tags.ItemTags.WOODEN_PRESSURE_PLATES)
                || stack.is(net.minecraft.tags.ItemTags.WOODEN_BUTTONS)
                || stack.is(net.minecraft.tags.ItemTags.WOODEN_DOORS)
                || stack.is(net.minecraft.tags.ItemTags.WOODEN_TRAPDOORS)) {
            return 300;
        }
        if (stack.is(net.minecraft.tags.ItemTags.COALS)) {
            return 1600;
        }
        if (stack.is(net.minecraft.tags.ItemTags.LOGS_THAT_BURN)) {
            return 300;
        }
        return 0;
    }

    public static boolean isFuel(ItemStack stack) {
        return burnTime(stack) > 0;
    }
}
