package top.ribs.scguns.common;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.phys.AABB;
import top.ribs.scguns.Config;
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.util.GunModifierHelper;

public class GunEffectsHandler {
   private static final Map<UUID, Long> lastEffectTime = new ConcurrentHashMap<>();
   private static final long EFFECT_COOLDOWN_MS = 250L;
   private static final int MAX_ENTITIES_PER_SHOT = 25;
   private static final Map<UUID, Integer> shotCounter = new ConcurrentHashMap<>();
   private static final Predicate<LivingEntity> FLEEING_ENTITIES = entity -> {
      if (entity instanceof Animal) {
         return true;
      } else {
         return entity.getType().is(ModTags.Entities.FLEEING_FROM_GUNS)
            ? true
            : ((List)Config.COMMON.fleeingMobs.fleeingEntities.get()).contains(EntityType.getKey(entity.getType()).toString());
      }
   };
   private static final Predicate<LivingEntity> HOSTILE_ENTITIES = entity -> {
      if (entity.getType().is(ModTags.Entities.AGGRO_FROM_GUNS)) {
         return true;
      } else if (entity.getSoundSource() == SoundSource.HOSTILE) {
         return true;
      } else {
         return entity.getType() != EntityType.PIGLIN
               && entity.getType() != EntityType.PIGLIN_BRUTE
               && entity.getType() != ModEntities.HORNLIN.get()
               && entity.getType() != ModEntities.ZOMBIFIED_HORNLIN.get()
               && entity.getType() != EntityType.ZOMBIFIED_PIGLIN
               && entity.getType() != EntityType.ENDERMAN
            ? !((List)Config.COMMON.aggroMobs.exemptEntities.get()).contains(EntityType.getKey(entity.getType()).toString())
            : true;
      }
   };

   public GunEffectsHandler() {
      super();
   }

   public static void handleGunEffects(ServerPlayer player, ItemStack heldItem, Gun modifiedGun) {
      if (!player.isCreative() && ((Boolean)Config.COMMON.aggroMobs.enabled.get() || (Boolean)Config.COMMON.fleeingMobs.enabled.get())) {
         UUID playerId = player.getUUID();
         long currentTime = System.currentTimeMillis();
         Long lastTime = lastEffectTime.get(playerId);
         if (lastTime == null || currentTime - lastTime >= 250L) {
            int fireRate = modifiedGun.getGeneral().getRate();
            if (fireRate < 6) {
               int shots = shotCounter.merge(playerId, 1, Integer::sum);
               if (shots % 2 != 0) {
                  return;
               }
            }

            lastEffectTime.put(playerId, currentTime);
            Level world = player.level();
            boolean isSilenced = GunModifierHelper.isSilencedFire(heldItem);
            if (!isSilenced && world instanceof ServerLevel serverLevel) {
               triggerSculkSensor(serverLevel, player);
            }

            double effectRadius = getEffectRadius(isSilenced);

            for (LivingEntity entity : getOptimizedNearbyEntities(world, player, effectRadius)) {
               if (entity != player) {
                  handleEntityReaction(entity, player, isSilenced);
               }
            }

            if (player.tickCount % 1200 == 0) {
               cleanupOldEntries(currentTime);
            }
         }
      }
   }

   private static void triggerSculkSensor(ServerLevel level, ServerPlayer player) {
      BlockPos playerPos = player.blockPosition();
      level.gameEvent(GameEvent.PROJECTILE_SHOOT, playerPos, Context.of(player));
   }

   private static double getEffectRadius(boolean isSilenced) {
      return isSilenced
         ? (Double)Config.COMMON.fleeingMobs.silencedRange.get()
         : Math.max((Double)Config.COMMON.aggroMobs.unsilencedRange.get(), (Double)Config.COMMON.fleeingMobs.unsilencedRange.get());
   }

   private static List<LivingEntity> getOptimizedNearbyEntities(Level world, ServerPlayer player, double radius) {
      AABB searchArea = new AABB(
         player.getX() - radius,
         player.getY() - radius,
         player.getZ() - radius,
         player.getX() + radius,
         player.getY() + radius,
         player.getZ() + radius
      );
      List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, searchArea);
      return entities.size() > 25
         ? entities.stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(player))).limit(25L).collect(Collectors.toList())
         : entities;
   }

   private static void handleEntityReaction(LivingEntity entity, ServerPlayer player, boolean isSilenced) {
      if (!isSilenced) {
         if (shouldEntityFlee(entity)) {
            applyKnockbackEffect(entity, player);
         }

         if (shouldEntityAggro(entity)) {
            handleAggroBehavior(entity, player);
         }
      }
   }

   private static boolean shouldEntityFlee(LivingEntity entity) {
      return (Boolean)Config.COMMON.fleeingMobs.enabled.get() && FLEEING_ENTITIES.test(entity) && !isTamedMob(entity) && entity.getPassengers().isEmpty();
   }

   private static boolean shouldEntityAggro(LivingEntity entity) {
      return (Boolean)Config.COMMON.aggroMobs.enabled.get() && HOSTILE_ENTITIES.test(entity);
   }

   private static boolean isTamedMob(LivingEntity entity) {
      if (entity instanceof TamableAnimal tamableAnimal) {
         return tamableAnimal.isTame();
      } else {
         return entity instanceof AbstractHorse horse ? horse.isTamed() : false;
      }
   }

   private static void applyKnockbackEffect(LivingEntity entity, ServerPlayer player) {
      double deltaX = entity.getX() - player.getX();
      double deltaZ = entity.getZ() - player.getZ();
      double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
      if (distance > 0.0) {
         double knockbackStrength = getKnockbackStrength(entity);
         double normalizedX = deltaX / distance;
         double normalizedZ = deltaZ / distance;
         entity.knockback(knockbackStrength, -normalizedX, -normalizedZ);
      }
   }

   private static double getKnockbackStrength(LivingEntity entity) {
      if (entity.getType().is(ModTags.Entities.HEAVY)) {
         return 0.4;
      } else {
         return entity.getType().is(ModTags.Entities.VERY_HEAVY) ? 0.2 : 0.8;
      }
   }

   private static void handleAggroBehavior(LivingEntity entity, ServerPlayer player) {
      if (entity instanceof Monster monster) {
         float aggroChance = ((Double)Config.COMMON.aggroMobs.aggroChance.get()).floatValue();
         if (player.level().random.nextFloat() < aggroChance) {
            monster.setTarget(player);
            alertNearbyMobs(monster, player);
         }
      }
   }

   private static void alertNearbyMobs(Monster alertedMob, ServerPlayer player) {
      double chainRadius = (Double)Config.COMMON.aggroMobs.chainAggroRadius.get();
      float chainChance = ((Double)Config.COMMON.aggroMobs.chainAggroChance.get()).floatValue();
      EntityType<?> mobType = alertedMob.getType();

      for (LivingEntity entity : getOptimizedNearbyEntities(player.level(), player, chainRadius)) {
         if (entity.getType() == mobType && entity instanceof Monster) {
            Monster nearbyMonster = (Monster)entity;
            if (entity != alertedMob && player.level().random.nextFloat() < chainChance) {
               nearbyMonster.setTarget(player);
            }
         }
      }
   }

   private static void cleanupOldEntries(long currentTime) {
      long expireTime = currentTime - 5000L;
      lastEffectTime.entrySet().removeIf(entry -> entry.getValue() < expireTime);
      shotCounter.clear();
   }
}
