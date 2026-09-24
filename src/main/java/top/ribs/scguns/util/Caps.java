package top.ribs.scguns.util;


import top.ribs.scguns.util.Caps;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Null-safe accessors for the NeoForge capability system.
 *
 * <p>Forge returned a LazyOptional, so 1.20.1 code reads like
 * {@code stack.getCapability(Capabilities.ENERGY).map(IEnergyStorage::getEnergyStored).orElse(0)}
 * or {@code be.getCapability(cap, side).ifPresent(handler -> ...)}. NeoForge hands
 * back a plain nullable value and resolves block capabilities through the level,
 * so those call sites collapse into the helpers here.</p>
 */
public final class Caps {
    private Caps() {
    }

    public static int energyStored(ItemStack stack, int fallback) {
        IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return storage == null ? fallback : storage.getEnergyStored();
    }

    public static int maxEnergyStored(ItemStack stack, int fallback) {
        IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return storage == null ? fallback : storage.getMaxEnergyStored();
    }

    /** Replaces Forge's {@code LazyOptional#ifPresent}. */
    public static <T> void ifPresent(T value, Consumer<T> action) {
        if (value != null) {
            action.accept(value);
        }
    }

    /** Replaces Forge's {@code LazyOptional#map(...).orElse(fallback)}. */
    public static <T, R> R map(T value, Function<T, R> mapper, R fallback) {
        return value == null ? fallback : mapper.apply(value);
    }

    /**
     * Block entity capability lookup. Forge let a block entity expose a
     * capability directly; NeoForge resolves it through the level.
     */
    public static <T> T of(BlockEntity blockEntity, BlockCapability<T, Direction> capability, Direction side) {
        if (blockEntity == null) {
            return null;
        }
        Level level = blockEntity.getLevel();
        if (level == null) {
            return null;
        }
        return level.getCapability(capability, blockEntity.getBlockPos(), side);
    }

    /**
     * Item handler of a neighbouring block entity, with 1.20.1 semantics.
     *
     * <p>Forge patched every {@code Container} block entity to expose
     * {@code ForgeCapabilities.ITEM_HANDLER} through {@code ICapabilityProvider}, so 1.20.1 code
     * could query any neighbouring inventory, including plain ones such as this mod's ammo box.
     * NeoForge only serves capabilities that were registered for the block, so this looks the
     * registered capability up first and falls back to wrapping the {@link Container}, which
     * keeps pipe-style transfer into plain containers working exactly as before.</p>
     *
     * @return the handler, or {@code null} when the block entity is neither registered for the
     * item handler capability nor a container
     */
    @Nullable
    public static IItemHandler itemHandler(BlockEntity blockEntity, @Nullable Direction side) {
        IItemHandler handler = of(blockEntity, Capabilities.ItemHandler.BLOCK, side);
        if (handler != null) {
            return handler;
        }
        return blockEntity instanceof Container container ? new InvWrapper(container) : null;
    }
}
