package top.ribs.scguns.util;


import top.ribs.scguns.util.DistHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.function.Supplier;

/**
 * Replacement for Forge's {@code DistExecutor}, which NeoForge removed.
 *
 * <p>The 1.20.1 code wraps client-only work in
 * {@code DistHelper.runWhenOn(Dist.CLIENT, () -> ...)} so that the
 * lambda body is only class-loaded on the physical client. These helpers keep
 * the same shape and the same safety property: the action {@link Supplier} is
 * only evaluated when the physical side matches.</p>
 */
public final class DistHelper {
    private DistHelper() {
    }

    public static void runWhenOn(Dist dist, Runnable action) {
        if (FMLEnvironment.dist == dist) {
            action.run();
        }
    }

    public static void runOnClient(Runnable action) {
        runWhenOn(Dist.CLIENT, action);
    }

    public static <T> T callWhenOn(Dist dist, Supplier<T> supplier) {
        if (FMLEnvironment.dist == dist) {
            return supplier.get();
        }
        return null;
    }

    public static <T> T callOnClient(Supplier<T> supplier) {
        return callWhenOn(Dist.CLIENT, supplier);
    }

    public static boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }
}
