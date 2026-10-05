package com.scg2tlm.elmomod.compat;



import top.ribs.scguns.ScorchedGuns;
import com.scg2tlm.elmomod.SCG2TLMConfig;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.interfaces.IAirGun;
import top.ribs.scguns.common.Gun;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import top.ribs.scguns.entity.monster.ZombifiedHornlinEntity;
import top.ribs.scguns.entity.projectile.ProjectileEntity;

// Registered explicitly from ExampleMod, which is gated on Touhou Little Maid being installed.
// It is deliberately NOT annotated with @EventBusSubscriber: annotation scanning loads the
// annotated class, which would resolve TLM (EntityMaid) even when TLM is absent.
public class MaidProjectileHandler {
    private static final String LASER_MUSKET_COUNT_KEY = "scg2tlm:laser_musket_illager_kills";
    private static final int LASER_MUSKET_GOAL = 100;
    private static final String TOTAL_KILL_COUNT_KEY = "scg2tlm:total_kills";
    private static final int TOTAL_KILL_GOAL = 1000;
    private static Item laserMusketItem;

    private static Item getLaserMusketItem() {
        if (laserMusketItem == null) {
            laserMusketItem = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("scguns", "laser_musket"));
        }
        return laserMusketItem;
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        if (!(event.getEntity() instanceof ProjectileEntity projectile)) return;
        if (!(projectile.getOwner() instanceof EntityMaid maid)) return;
        // Air-powered guns: SC2 charges the canister in GunEventBus, but its helper and its
        // GunFireEvent are both Player-only, so a maid fired them for free. Charge her here, the one
        // place both fire paths (our task and SC2's native gunner AI) pass through.
        if (!SCG2TLMConfig.RELOAD_FREE_AMMO.get() && ScorchedGuns.createLoaded) {
            ItemStack airWeapon = projectile.getWeapon();
            if (airWeapon != null && airWeapon.getItem() instanceof IAirGun
                    && airWeapon.getItem() instanceof GunItem airGunItem) {
                Gun airGun = airGunItem.getModifiedGun(airWeapon);
                float cost = airGun != null && airGun.getProjectile() != null
                    ? (float) airGun.getProjectile().getEnergyUse() : 0.0F;
                if (cost > 0.0F) {
                    boolean paid = false;
                    // Create's getAllWithAir is its own player-oriented lookup and finds nothing on a maid,
                    // which made every shot look airless. Scan her slots instead: getAir(stack) needs no
                    // entity, so the same test works for her armour (the chest slot is where Create's own
                    // backtank lives), her inventory and her accessory slots.
                    java.util.List<ItemStack> canisters = new java.util.ArrayList<>();
                    for (net.minecraft.world.entity.EquipmentSlot slot
                            : net.minecraft.world.entity.EquipmentSlot.values()) {
                        canisters.add(maid.getItemBySlot(slot));
                    }
                    net.neoforged.neoforge.items.IItemHandler maidInv = maid.getAvailableInv(true);
                    for (int i = 0; i < maidInv.getSlots(); i++) {
                        canisters.add(maidInv.getStackInSlot(i));
                    }
                    var maidCurios = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(maid);
                    if (maidCurios.isPresent()) {
                        var curioHandler = maidCurios.get().getEquippedCurios();
                        for (int i = 0; i < curioHandler.getSlots(); i++) {
                            canisters.add(curioHandler.getStackInSlot(i));
                        }
                    }
                    for (ItemStack canister : canisters) {
                        if (canister.isEmpty()) continue;
                        // scguns' own air canisters (AirCanisterItem) keep their air as Forge energy, not as
                        // Create backtank air - AirSourceHelper.consumeAir has a separate AIR_CANISTER branch
                        // for exactly that. Testing only BacktankUtil.getAir read 0 on those items and threw
                        // every shot away, which is why air guns dealt no damage.
                        net.neoforged.neoforge.energy.IEnergyStorage energy =
                            canister.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
                        if (energy != null && energy.getEnergyStored() >= (int) cost) {
                            if (energy.extractEnergy((int) cost, false) >= (int) cost) {
                                paid = true;
                                break;
                            }
                            continue;
                        }
                        if (BacktankUtil.getAir(canister) >= (int) cost) {
                            BacktankUtil.consumeAir(maid, canister, (int) cost);
                            paid = true;
                            break;
                        }
                    }
                    if (!paid) {
                        // Same outcome as the player path, where a shot without air is cancelled.
                        projectile.discard();
                        return;
                    }
                }
            }
        }

        if (!projectile.getPersistentData().contains("AIDamageScale")) return;
        projectile.getPersistentData().putFloat("AIDamageScale", 1.0f);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof ProjectileEntity)) return;

        Entity shooter = event.getSource().getEntity();

        if (shooter instanceof Player player && event.getEntity() instanceof EntityMaid maid) {
            if (player.getUUID().equals(maid.getOwnerUUID())) {
                event.setCanceled(true);
                return;
            }
        }

        if (!(shooter instanceof EntityMaid maid)) return;
        if (event.getEntity() instanceof Player) return;

        event.getEntity().invulnerableTime = 0;

        if (event.getEntity() instanceof Mob mob) {
            mob.setLastHurtByMob(maid);
            mob.setTarget(maid);

            var brain = mob.getBrain();
            if (brain != null) {
                brain.setMemory(MemoryModuleType.ATTACK_TARGET, maid);
                brain.setMemory(MemoryModuleType.HURT_BY_ENTITY, maid);
                brain.setMemory(MemoryModuleType.HURT_BY, event.getSource());
            }

            if (mob instanceof ZombifiedPiglin && !mob.level().isClientSide) {
                Vec3 pos = mob.position();
                AABB searchBox = AABB.ofSize(pos, 52.0, 20.0, 52.0);
                mob.level().getEntitiesOfClass(ZombifiedHornlinEntity.class, searchBox, EntitySelector.NO_SPECTATORS)
                    .stream()
                    .filter(h -> h.getTarget() == null)
                    .forEach(h -> {
                        h.setTarget(maid);
                        h.stopBeingAngry();
                        h.setLastHurtByMob(maid);
                        h.setPersistentAngerTarget(maid.getUUID());
                    });
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof ProjectileEntity)) return;
        if (!(event.getSource().getEntity() instanceof EntityMaid maid)) return;
        if (event.getEntity() instanceof Player) return;

        SC2AdvancementTriggers.triggerForMaidOwner(maid, SC2AdvancementTriggers.KILL_MOB);

        if (!maid.level().isClientSide) {
            CompoundTag data = maid.getPersistentData();
            int total = data.getInt(TOTAL_KILL_COUNT_KEY) + 1;
            data.putInt(TOTAL_KILL_COUNT_KEY, total);
            if (total >= TOTAL_KILL_GOAL) {
                SC2AdvancementTriggers.triggerForMaidOwner(maid, SC2AdvancementTriggers.KILL_1000);
            }
        }

        if (event.getEntity() instanceof AbstractIllager && !maid.level().isClientSide) {
            ItemStack held = maid.getMainHandItem();
            if (held.getItem() == getLaserMusketItem()) {
                CompoundTag data = maid.getPersistentData();
                int count = data.getInt(LASER_MUSKET_COUNT_KEY) + 1;
                data.putInt(LASER_MUSKET_COUNT_KEY, count);
                if (count >= LASER_MUSKET_GOAL) {
                    SC2AdvancementTriggers.triggerForMaidOwner(maid, SC2AdvancementTriggers.LASER_MUSKET_100_ILLAGER);
                }
            }
        }
    }
}
