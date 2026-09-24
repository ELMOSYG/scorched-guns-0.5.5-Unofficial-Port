package top.ribs.scguns.init;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.item.AirCanisterItem;
import top.ribs.scguns.item.CreativeAirCanisterItem;
import top.ribs.scguns.item.EnergyGunItem;
import top.ribs.scguns.item.animated.AnimatedEnergyGunItem;
import top.ribs.scguns.item.exosuit.ExoSuitCoreItem;

/**
 * NeoForge capability registration.
 *
 * <p>Forge 1.20.1 exposed capabilities by having every provider implement
 * {@code ICapabilityProvider} and hand back a {@code LazyOptional}. NeoForge 1.21
 * removed both and registers capabilities centrally instead. Each block entity
 * therefore keeps its original side logic in a plain nullable
 * {@code getCapability(cap, side)} method, and this class binds that method to
 * the capability types the block entity actually supports. Items get a storage
 * factory bound for every item of the relevant class.</p>
 *
 * <p>{@code bus = Bus.MOD} is required, not cosmetic: {@code RegisterCapabilitiesEvent} is an
 * {@code IModBusEvent}, and FML 4.0.38 (NeoForge 21.1.150) registers the annotated class on the bus
 * the annotation names - the default is the GAME bus, which rejects mod-bus events with
 * "IModBusEvent events are not allowed on the common NeoForge bus!" and aborts mod loading. Newer FML
 * (4.0.44, NeoForge 21.1.249) routes each listener by its event type instead, which is why this only
 * ever failed on older NeoForge (HANDOFF section 70).</p>
 */
@EventBusSubscriber(modid = ScorchedGuns.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModCapabilities {
    private ModCapabilities() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // ---- item energy storage -------------------------------------------------
        registerItemEnergy(event, EnergyGunItem.class,
                (item, stack) -> ((EnergyGunItem) item).createEnergyStorage(stack));
        registerItemEnergy(event, AnimatedEnergyGunItem.class,
                (item, stack) -> ((AnimatedEnergyGunItem) item).createEnergyStorage(stack));
        registerItemEnergy(event, AirCanisterItem.class,
                (item, stack) -> ((AirCanisterItem) item).createEnergyStorage(stack));
        registerItemEnergy(event, CreativeAirCanisterItem.class,
                (item, stack) -> ((CreativeAirCanisterItem) item).createEnergyStorage(stack));
        registerItemEnergy(event, ExoSuitCoreItem.class,
                (item, stack) -> ((ExoSuitCoreItem) item).createEnergyStorage(stack));

        // ---- block entities -----------------------------------------------------
        itemHandler(event, ModBlockEntities.CRYONITER.get(),
                (blockEntity, side) -> blockEntity.getCapability(Capabilities.ItemHandler.BLOCK, side));
        itemHandler(event, ModBlockEntities.THERMOLITH.get(),
                (blockEntity, side) -> blockEntity.getCapability(Capabilities.ItemHandler.BLOCK, side));
        itemHandler(event, ModBlockEntities.BASIC_TURRET.get(),
                (blockEntity, side) -> blockEntity.getCapability(Capabilities.ItemHandler.BLOCK, side));
        itemHandler(event, ModBlockEntities.AUTO_TURRET.get(),
                (blockEntity, side) -> blockEntity.getCapability(Capabilities.ItemHandler.BLOCK, side));
        itemHandler(event, ModBlockEntities.SNIPER_TURRET.get(),
                (blockEntity, side) -> blockEntity.getCapability(Capabilities.ItemHandler.BLOCK, side));
        itemHandler(event, ModBlockEntities.SHOTGUN_TURRET.get(),
                (blockEntity, side) -> blockEntity.getCapability(Capabilities.ItemHandler.BLOCK, side));
        itemHandler(event, ModBlockEntities.VENT_COLLECTOR.get(),
                (blockEntity, side) -> blockEntity.getCapability(Capabilities.ItemHandler.BLOCK, side));
        itemHandler(event, ModBlockEntities.MECHANICAL_PRESS.get(),
                (blockEntity, side) -> blockEntity.getCapability(Capabilities.ItemHandler.BLOCK, side));
        itemHandler(event, ModBlockEntities.POWERED_MECHANICAL_PRESS.get(),
                (blockEntity, side) -> blockEntity.getCapability(Capabilities.ItemHandler.BLOCK, side));
        itemHandler(event, ModBlockEntities.MACERATOR.get(),
                (blockEntity, side) -> blockEntity.getCapability(Capabilities.ItemHandler.BLOCK, side));
        itemHandler(event, ModBlockEntities.POWERED_MACERATOR.get(),
                (blockEntity, side) -> blockEntity.getCapability(Capabilities.ItemHandler.BLOCK, side));
        itemHandler(event, ModBlockEntities.LIGHTNING_BATTERY.get(),
                (blockEntity, side) -> blockEntity.getCapability(Capabilities.ItemHandler.BLOCK, side));
        itemHandler(event, ModBlockEntities.POLAR_GENERATOR.get(),
                (blockEntity, side) -> blockEntity.getCapability(Capabilities.ItemHandler.BLOCK, side));

        energy(event, ModBlockEntities.LIGHTNING_BATTERY.get(),
                (blockEntity, side) -> blockEntity.getCapability(Capabilities.EnergyStorage.BLOCK, side));
        energy(event, ModBlockEntities.POLAR_GENERATOR.get(),
                (blockEntity, side) -> blockEntity.getCapability(Capabilities.EnergyStorage.BLOCK, side));
        energy(event, ModBlockEntities.SHOCK_COIL.get(),
                (blockEntity, side) -> blockEntity.getCapability(Capabilities.EnergyStorage.BLOCK, side));
        // 1.20.1 exposed these two through ICapabilityProvider as well.
        energy(event, ModBlockEntities.POWERED_MECHANICAL_PRESS.get(),
                (blockEntity, side) -> blockEntity.getCapability(Capabilities.EnergyStorage.BLOCK, side));
        energy(event, ModBlockEntities.POWERED_MACERATOR.get(),
                (blockEntity, side) -> blockEntity.getCapability(Capabilities.EnergyStorage.BLOCK, side));
    }

    private interface ItemStorageFactory {
        IEnergyStorage create(Item item, ItemStack stack);
    }

    /**
     * Reads one block entity's item handler through its own nullable
     * {@code getCapability(cap, side)} method. The type parameter keeps the
     * concrete block entity class visible at each call site.
     */
    private interface ItemHandlerGetter<BE extends BlockEntity> {
        IItemHandler get(BE blockEntity, Direction side);
    }

    /**
     * Reads one block entity's energy storage through its own nullable
     * {@code getCapability(cap, side)} method.
     */
    private interface EnergyStorageGetter<BE extends BlockEntity> {
        IEnergyStorage get(BE blockEntity, Direction side);
    }

    private static <T extends Item> void registerItemEnergy(RegisterCapabilitiesEvent event,
                                                            Class<T> type,
                                                            ItemStorageFactory factory) {
        Item[] items = BuiltInRegistries.ITEM.stream().filter(type::isInstance).toArray(Item[]::new);
        if (items.length == 0) {
            return;
        }
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (stack, context) -> factory.create(stack.getItem(), stack),
                items);
    }

    private static <BE extends BlockEntity> void itemHandler(RegisterCapabilitiesEvent event,
                                                             BlockEntityType<BE> type,
                                                             ItemHandlerGetter<BE> getter) {
        ICapabilityProvider<BE, Direction, IItemHandler> provider =
                (blockEntity, side) -> getter.get(blockEntity, side);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, provider);
    }

    private static <BE extends BlockEntity> void energy(RegisterCapabilitiesEvent event,
                                                        BlockEntityType<BE> type,
                                                        EnergyStorageGetter<BE> getter) {
        ICapabilityProvider<BE, Direction, IEnergyStorage> provider =
                (blockEntity, side) -> getter.get(blockEntity, side);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, provider);
    }
}
