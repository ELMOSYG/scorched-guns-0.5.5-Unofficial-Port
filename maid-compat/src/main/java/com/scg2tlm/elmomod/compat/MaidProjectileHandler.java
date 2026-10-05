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
        // Air is charged here rather than before the shot because this is the one place both fire
        // paths pass. The task checks maidAir first, so a maid without air never fires at all.
        if (!SC2GunCompat.maidAir(maid, projectile.getWeapon(), true)) {
            // Cancel the join instead of discarding: discard() has no effect while the entity is still
            // being added, so the projectile entered the level anyway and the client drew a bullet that
            // dealt no damage. Cancelling stops it from ever existing, whichever AI path fired it.
            // Diagnostic: tells us which AI path fired and what it fired, so a report of "she still
            // shoots" can be traced to a specific route instead of guessed at.
            // Stack trace: the shot bypasses every gate we added, so the only way to pin down which AI
            // path fires it is to print the frames. Only the first few are interesting; the rest is
            // vanilla plumbing.
            StringBuilder stack = new StringBuilder();
            StackTraceElement[] frames = new Throwable().getStackTrace();
            for (int f = 0; f < Math.min(frames.length, 14); f++) {
                String cls = frames[f].getClassName();
                if (cls.startsWith("java.") || cls.startsWith("org.slf4j")) continue;
                stack.append("\n      ").append(cls).append('.').append(frames[f].getMethodName())
                     .append(':').append(frames[f].getLineNumber());
            }
            org.slf4j.LoggerFactory.getLogger("scg2_maid_compat").info(
                "[scg2_maid_compat] airless shot: projectile={} weapon={} stack:{}",
                projectile.getClass().getSimpleName(),
                projectile.getWeapon().isEmpty() ? "empty" : projectile.getWeapon().getItem(), stack);
            event.setCanceled(true);
            // Stop the behaviour itself. The shot bypasses both gated paths (no consume=false scan ever
            // appeared in the log), so this hook - which provably runs - is where the attack has to be
            // ended: drop her target and halt her movement, or she keeps firing every 200 ms.
            maid.getNavigation().stop();
            maid.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET);
            maid.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);
            return;
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
