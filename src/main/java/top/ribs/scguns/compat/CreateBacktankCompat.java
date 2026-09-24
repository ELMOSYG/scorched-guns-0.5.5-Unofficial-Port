package top.ribs.scguns.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Create backtank air compatibility.
 *
 * <p>0.5.5 read and drained Create's backtank air through
 * {@code BacktankUtil}. No 1.21.1 Create build is in this project's dependency
 * set, so the entry points are kept with neutral answers: no backtank is found
 * and no air is available, which makes the air source fall back to its own
 * canisters. Re-enabling means depending on Create again and delegating these
 * calls to {@code com.simibubi.create.content.equipment.armor.BacktankUtil}.</p>
 */
public final class CreateBacktankCompat {
    private CreateBacktankCompat() {
    }

    public static List<ItemStack> getAllWithAir(Player player) {
        return List.of();
    }

    public static boolean hasAirRemaining(ItemStack stack) {
        return false;
    }

    public static int getAir(ItemStack stack) {
        return 0;
    }

    public static void consumeAir(Player player, ItemStack stack, int amount) {
        // no backtank support
    }

    public static int maxAir(ItemStack stack) {
        return 0;
    }

    public static int getBarColor(ItemStack stack, int width) {
        return 0xFFFFFF;
    }
}
