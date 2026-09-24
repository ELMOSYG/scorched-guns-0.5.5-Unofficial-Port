package top.ribs.scguns.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;

/**
 * 1.21 changed villager trade costs from ItemStack to {@link ItemCost}. Scorched
 * Guns builds its trades from item stacks, so this converts one to the other.
 */
public final class ScTrades {
    private ScTrades() {
    }

    public static ItemCost cost(ItemStack stack) {
        return new ItemCost(stack.getItem(), stack.getCount());
    }

    public static ItemCost cost(ItemCost cost) {
        return cost;
    }
}
