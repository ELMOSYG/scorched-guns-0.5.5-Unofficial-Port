package top.ribs.scguns.common;




import top.ribs.scguns.util.ScEnchants;
import top.ribs.scguns.util.NbtHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import com.mrcrayfish.framework.api.network.LevelLocation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import top.ribs.scguns.Config;
import top.ribs.scguns.client.handler.BeamHandler;
import top.ribs.scguns.init.ModDamageTypes;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageBeamImpact;
import top.ribs.scguns.network.message.S2CMessageBeamPenetration;
import top.ribs.scguns.network.message.S2CMessageBeamUpdate;
import top.ribs.scguns.network.message.S2CMessageProjectileHitEntity;
import top.ribs.scguns.network.message.S2CMessageStopBeam;
import top.ribs.scguns.util.GunEnchantmentHelper;
import top.ribs.scguns.util.GunModifierHelper;
import top.ribs.scguns.util.math.ExtendedEntityRayTraceResult;

public class BeamWeaponHandler {
   private static final Map<UUID, BeamHandler.BeamInfo> activeBeams = new HashMap<>();

   public BeamWeaponHandler() {
      super();
   }

   public static void handleBeamWeapon(ServerPlayer player, ItemStack heldItem, Gun modifiedGun) {
      UUID playerId = player.getUUID();
      Level world = player.level();
      Vec3 beamOriginOffset = new Vec3(0.0, (double)player.getEyeHeight(), 0.0);
      Vec3 beamOrigin = player.position().add(beamOriginOffset);
      Vec3 lookVec = player.getLookAngle();
      double maxDistance = modifiedGun.getGeneral().getBeamMaxDistance();
      Vec3 endVec = beamOrigin.add(lookVec.scale(maxDistance));
      HitResult finalHitResult = BeamHandlerCommon.BeamMiningManager.getBeamHitResult(world, beamOrigin, endVec, player, maxDistance);
      Vec3 hitPos = finalHitResult.getLocation();
      List<BlockHitResult> glassPenetrations = new ArrayList<>();
      double damageMultiplier = 1.0;
      if (finalHitResult instanceof BeamHandlerCommon.BeamMiningManager.ExtendedBlockHitResult extendedBlock) {
         glassPenetrations = extendedBlock.getGlassPenetrations();
         damageMultiplier = extendedBlock.getDamageMultiplier();
      } else if (finalHitResult instanceof BeamHandlerCommon.BeamMiningManager.ExtendedEntityHitResult extendedEntity) {
         damageMultiplier = extendedEntity.getDamageMultiplier();
      }

      long currentTime = System.currentTimeMillis();
      boolean isBeamFireMode = modifiedGun.getGeneral().getFireMode() == FireMode.BEAM;
      BeamHandler.BeamInfo beamInfo = activeBeams.computeIfAbsent(playerId, k -> new BeamHandler.BeamInfo(beamOrigin, hitPos, currentTime, isBeamFireMode));
      beamInfo.startPos = beamOrigin;
      beamInfo.endPos = hitPos;
      sendBeamUpdate(player, beamOrigin, hitPos);
      if (!glassPenetrations.isEmpty()) {
         sendBeamPenetrationEffects(player, playerId, glassPenetrations, beamOrigin);
      }

      handleBeamMining(world, finalHitResult, glassPenetrations, player, modifiedGun);
      int damageDelayMs = modifiedGun.getGeneral().getBeamDamageDelay();
      if (currentTime - beamInfo.lastDamageTime >= (long)damageDelayMs) {
         handleBeamDamage(player, finalHitResult, modifiedGun, damageMultiplier);
         beamInfo.lastDamageTime = currentTime;
      }

      FireMode fireMode = modifiedGun.getGeneral().getFireMode();
      if (fireMode == FireMode.BEAM || fireMode == FireMode.SEMI_BEAM) {
         if (fireMode == FireMode.BEAM) {
            if (currentTime - beamInfo.startTime >= (long)modifiedGun.getGeneral().getBeamAmmoConsumptionDelay()) {
               consumeBeamAmmo(player, heldItem);
               beamInfo.startTime = currentTime;
            }
         } else if (beamInfo.startTime == currentTime) {
            consumeBeamAmmo(player, heldItem);
         }
      }
   }

   private static void sendBeamUpdate(ServerPlayer player, Vec3 beamOrigin, Vec3 hitPos) {
      double radius = 64.0;
      S2CMessageBeamUpdate beamUpdate = new S2CMessageBeamUpdate(player.getUUID(), beamOrigin, hitPos);
      PacketHandler.getPlayChannel()
         .sendToNearbyPlayers(() -> LevelLocation.create((ServerLevel) player.level(), beamOrigin.x, beamOrigin.y, beamOrigin.z, radius), beamUpdate);
   }

   private static void sendBeamPenetrationEffects(ServerPlayer player, UUID playerId, List<BlockHitResult> glassPenetrations, Vec3 beamOrigin) {
      double radius = 64.0;
      S2CMessageBeamPenetration penetrationMessage = new S2CMessageBeamPenetration(playerId, glassPenetrations);
      PacketHandler.getPlayChannel()
         .sendToNearbyPlayers(
            () -> LevelLocation.create((ServerLevel) player.level(), beamOrigin.x, beamOrigin.y, beamOrigin.z, radius), penetrationMessage
         );
   }

   private static void handleBeamMining(Level world, HitResult finalHitResult, List<BlockHitResult> glassPenetrations, ServerPlayer player, Gun modifiedGun) {
      if (finalHitResult.getType() == Type.BLOCK) {
         BlockHitResult blockHit = (BlockHitResult)finalHitResult;
         BlockPos pos = blockHit.getBlockPos();
         if (!glassPenetrations.contains(blockHit)) {
            BeamHandlerCommon.BeamMiningManager.updateBlockMining(world, pos, player, modifiedGun);
         }
      }
   }

   private static void handleBeamDamage(ServerPlayer player, HitResult hitResult, Gun modifiedGun, double damageMultiplier) {
      if (hitResult.getType() == Type.ENTITY) {
         handleEntityDamage(player, (EntityHitResult)hitResult, modifiedGun, damageMultiplier);
      } else if (hitResult.getType() == Type.BLOCK) {
         sendBeamImpactEffect(player, hitResult.getLocation());
      }
   }

   private static void handleEntityDamage(ServerPlayer player, EntityHitResult entityHitResult, Gun modifiedGun, double damageMultiplier) {
      Entity hitEntity = entityHitResult.getEntity();
      if (hitEntity.isAttackable()) {
         if (hitEntity instanceof Player hitPlayer && !player.canHarmPlayer(hitPlayer)) {
            return;
         }

         ItemStack weapon = player.getMainHandItem();
         if (hitEntity instanceof LivingEntity livingEntity) {
            applyBeamImpactEffects(livingEntity, modifiedGun, player);
         }

         float damage = calculateBeamDamage(weapon, modifiedGun, player, damageMultiplier, entityHitResult);
         DamageSource damageSource = ModDamageTypes.Sources.projectile(player.server.registryAccess(), null, player);
         boolean damaged = hitEntity.hurt(damageSource, damage);
         if (damaged) {
            hitEntity.invulnerableTime = 0;
            applyPostDamageEffects(player, weapon, hitEntity, damageSource);
            boolean critical = isBeamCritical(weapon, modifiedGun, player.level().random, modifiedGun.getProjectile().getDamage());
            if (critical) {
               PacketHandler.getPlayChannel()
                  .sendToPlayer(
                     () -> player,
                     new S2CMessageProjectileHitEntity(
                        entityHitResult.getLocation().x,
                        entityHitResult.getLocation().y,
                        entityHitResult.getLocation().z,
                        2,
                        hitEntity instanceof Player
                     )
                  );
            }
         }

         sendBeamImpactEffect(player, entityHitResult.getLocation());
      }
   }

   private static void applyBeamImpactEffects(LivingEntity livingEntity, Gun modifiedGun, ServerPlayer player) {
      Gun.Projectile projectile = modifiedGun.getProjectile();
      ResourceLocation effectLocation = projectile.getImpactEffect();
      if (effectLocation != null) {
         float effectChance = projectile.getImpactEffectChance();
         if (player.level().random.nextFloat() < effectChance) {
            MobEffect effect = (MobEffect)BuiltInRegistries.MOB_EFFECT.get(effectLocation);
            if (effect != null) {
               livingEntity.addEffect(new MobEffectInstance(top.ribs.scguns.util.ScEffects.holder(effect), projectile.getImpactEffectDuration(), projectile.getImpactEffectAmplifier()));
            }
         }
      }
   }

   private static float calculateBeamDamage(ItemStack weapon, Gun modifiedGun, ServerPlayer player, double damageMultiplier, EntityHitResult hitResult) {
      float damage = modifiedGun.getProjectile().getDamage();
      damage = GunModifierHelper.getModifiedDamage(weapon, modifiedGun, damage);
      damage = GunEnchantmentHelper.getAcceleratorDamage(weapon, damage);
      damage = GunEnchantmentHelper.getHeavyShotDamage(weapon, damage);
      damage = GunEnchantmentHelper.getHotBarrelDamage(player, weapon, damage);
      damage = getBeamCriticalDamage(weapon, modifiedGun, player.level().random, damage);
      damage *= (float)damageMultiplier;
      damage *= ((Double)Config.COMMON.gameplay.globalDamageMultiplier.get()).floatValue();
      if (hitResult instanceof ExtendedEntityRayTraceResult extendedResult && extendedResult.isHeadshot()) {
         damage = (float)((double)damage * (Double)Config.COMMON.gameplay.headShotDamageMultiplier.get());
      }

      if (hitResult.getEntity() instanceof LivingEntity livingEntity) {
         damage += ScEnchants.getDamageBonus(weapon, top.ribs.scguns.util.MobType.of(livingEntity));
         damage = GunEnchantmentHelper.getPuncturingDamageReduction(weapon, livingEntity, damage);
         damage = applyProjectileProtection(livingEntity, damage);
         damage = calculateBeamArmorBypassDamage(weapon, modifiedGun, livingEntity, damage);
      }

      return damage;
   }

   private static void applyPostDamageEffects(ServerPlayer player, ItemStack weapon, Entity hitEntity, DamageSource damageSource) {
      if (hitEntity instanceof LivingEntity livingEntity) {
         GunEnchantmentHelper.applyElementalPopEffect(weapon, livingEntity);
         // 1.21 merged the 1.20.1 doPostHurtEffects (victim equipment) and doPostDamageEffects
         // (attacker weapon) into a single pass; the item source keeps the attacker-enchantment half
         // that vanilla's Player#attack gets from getWeaponItem().
         EnchantmentHelper.doPostAttackEffectsWithItemSource(player.serverLevel(), hitEntity, damageSource, weapon);
         if (player.level().random.nextFloat() < 0.05F) {
            hitEntity.igniteForSeconds(3);
         }

         if (GunEnchantmentHelper.shouldSetOnFire(player, weapon)) {
            hitEntity.igniteForSeconds(5);
         }
      }
   }

   private static void sendBeamImpactEffect(ServerPlayer player, Vec3 location) {
      PacketHandler.getPlayChannel().sendToPlayer(() -> player, new S2CMessageBeamImpact(location, player.getUUID()));
   }

   private static float getBeamCriticalDamage(ItemStack weapon, Gun modifiedGun, RandomSource rand, float damage) {
      float chance = modifiedGun.getProjectile().getCriticalChance();
      if (rand.nextFloat() < chance) {
         float critMultiplier = modifiedGun.getProjectile().getCritDamageMultiplier();
         return damage * critMultiplier;
      } else {
         return damage;
      }
   }

   private static boolean isBeamCritical(ItemStack weapon, Gun modifiedGun, RandomSource rand, float baseDamage) {
      float chance = modifiedGun.getProjectile().getCriticalChance();
      return rand.nextFloat() < chance;
   }

   private static float calculateBeamArmorBypassDamage(ItemStack weapon, Gun modifiedGun, LivingEntity target, float damage) {
      int armorValue = target.getArmorValue();
      float baseReduction = Math.min(0.75F, (float)armorValue * 0.004F);
      float baseArmorBypass = modifiedGun.getProjectile().getArmorPen();
      float puncturingBypass = GunEnchantmentHelper.getPuncturingArmorBypass(weapon);
      float totalArmorBypass = baseArmorBypass + puncturingBypass;
      if (totalArmorBypass <= 0.0F) {
         return damage * (1.0F - baseReduction);
      } else {
         float bypassPercent = totalArmorBypass / 10.0F;
         float effectiveArmor = (float)armorValue * (1.0F - bypassPercent);
         float finalReduction = Math.min(0.75F, effectiveArmor * 0.004F);
         return damage * (1.0F - finalReduction);
      }
   }

   private static float applyProjectileProtection(LivingEntity target, float damage) {
      int protectionLevel = ScEnchants.level(target, Enchantments.PROJECTILE_PROTECTION);
      if (protectionLevel > 0) {
         float reduction = (float)protectionLevel * 0.1F;
         reduction = Math.min(reduction, 0.8F);
         damage *= 1.0F - reduction;
      }

      return damage;
   }

   private static void consumeBeamAmmo(ServerPlayer player, ItemStack heldItem) {
      if (!player.isCreative()) {
         CompoundTag tag = NbtHelper.getOrCreateTag(heldItem);
         if (!tag.getBoolean("IgnoreAmmo")) {
            int currentAmmo = tag.getInt("AmmoCount");
            if (currentAmmo > 0) {
               tag.putInt("AmmoCount", currentAmmo - 1);
            }
         }
      }
   }

   public static void stopBeam(ServerPlayer player) {
      UUID playerId = player.getUUID();
      activeBeams.remove(playerId);
      double radius = 64.0;
      S2CMessageStopBeam stopBeamMessage = new S2CMessageStopBeam(playerId);
      PacketHandler.getPlayChannel()
         .sendToNearbyPlayers(() -> LevelLocation.create((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(), radius), stopBeamMessage);
   }
}
