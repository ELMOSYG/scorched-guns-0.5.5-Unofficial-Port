package top.ribs.scguns.entity.projectile;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModDamageTypes;
import top.ribs.scguns.init.ModParticleTypes;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageBlood;
import top.ribs.scguns.network.message.S2CMessageProjectileHitEntity;
import top.ribs.scguns.util.GunEnchantmentHelper;

public class BeowulfProjectileEntity extends ProjectileEntity {
   private static final float SHIELD_DISABLE_CHANCE = 0.5F;
   private static final float SHIELD_DAMAGE_PENETRATION = 0.3F;
   private static final float BEOWULF_XP_MULTIPLIER = 0.75F;
   private static final int BEOWULF_LOOTING_LEVEL = 3;

   public BeowulfProjectileEntity(EntityType<? extends Entity> entityType, Level worldIn) {
      super(entityType, worldIn);
   }

   public BeowulfProjectileEntity(EntityType<? extends Entity> entityType, Level worldIn, LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun) {
      super(entityType, worldIn, shooter, weapon, item, modifiedGun);
   }

   // 0.5.5 registered this class on the game bus to add a flat looting bonus via
   // Forge's LootingLevelEvent. NeoForge 21.1 has no such event and its
   // LivingDropsEvent carries no modifiable looting level, so the handler could not
   // be ported; registering a class with no @SubscribeEvent methods aborts mod
   // loading ("class ... has no @SubscribeEvent methods, but register was called
   // anyway"), so the registration is gone. See HANDOFF.md "known deviations".



   @Override
   protected void onProjectileTick() {
      if (this.level().isClientSide && this.tickCount > 1 && this.tickCount < this.life && this.tickCount % 2 == 0) {
         double offsetX = (this.random.nextDouble() - 0.5) * 0.5;
         double offsetY = (this.random.nextDouble() - 0.5) * 0.5;
         double offsetZ = (this.random.nextDouble() - 0.5) * 0.5;
         double velocityX = (this.random.nextDouble() - 0.5) * 0.1;
         double velocityY = (this.random.nextDouble() - 0.5) * 0.1;
         double velocityZ = (this.random.nextDouble() - 0.5) * 0.1;
         this.level()
            .addParticle(
               ParticleTypes.SMALL_FLAME, true, this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ, velocityX, velocityY, velocityZ
            );
      }

      if (this.level().isClientSide && this.tickCount > 1 && this.tickCount < this.life && this.tickCount % 5 == 0) {
         this.level().addParticle((ParticleOptions)ModParticleTypes.BEOWULF_IMPACT.get(), true, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
      }
   }

   @Override
   protected void onHitEntity(Entity entity, Vec3 hitVec, Vec3 startVec, Vec3 endVec, boolean headshot) {
      float damage = this.getDamage();
      float newDamage = this.getCriticalDamage(this.getWeapon(), this.random, damage);
      boolean critical = damage != newDamage;
      ResourceLocation advantage = this.getProjectile().getAdvantage();
      damage = newDamage * this.advantageMultiplier(entity);
      boolean wasAlive = entity instanceof LivingEntity && entity.isAlive();
      if (headshot) {
         damage = (float)((double)damage * (Double)Config.COMMON.gameplay.headShotDamageMultiplier.get());
      }

      if (entity instanceof LivingEntity livingTarget) {
         damage = GunEnchantmentHelper.getPuncturingDamageReduction(this.getWeapon(), livingTarget, damage);
         damage = this.applyProjectileProtection(livingTarget, damage);
         damage = this.calculateArmorBypassDamage(livingTarget, damage);
      }

      DamageSource source = ModDamageTypes.Sources.projectile(this.level().registryAccess(), this, (LivingEntity)this.getOwner());
      boolean blocked = ProjectileEntity.ProjectileHelper.handleShieldHit(entity, this, damage, 0.5F);
      if (blocked) {
         float penetratingDamage = damage * 0.3F;
         entity.hurt(source, penetratingDamage);
         if (entity instanceof LivingEntity livingEntity) {
            ResourceLocation effectLocation = this.getProjectile().getImpactEffect();
            if (effectLocation != null) {
               float effectChance = this.getProjectile().getImpactEffectChance() * 0.3F;
               if (this.random.nextFloat() < effectChance) {
                  MobEffect effect = (MobEffect)BuiltInRegistries.MOB_EFFECT.get(effectLocation);
                  if (effect != null) {
                     int reducedDuration = (int)((float)this.getProjectile().getImpactEffectDuration() * 0.3F);
                     livingEntity.addEffect(new MobEffectInstance(top.ribs.scguns.util.ScEffects.holder(effect), reducedDuration, this.getProjectile().getImpactEffectAmplifier()));
                  }
               }
            }
         }
      } else if (!entity.getType().is(ModTags.Entities.GHOST) || advantage.equals(ModTags.Entities.UNDEAD.location())) {
         entity.hurt(source, damage);
         if (entity instanceof LivingEntity livingEntityx) {
            ResourceLocation effectLocation = this.getProjectile().getImpactEffect();
            if (effectLocation != null) {
               float effectChance = this.getProjectile().getImpactEffectChance();
               if (headshot) {
                  effectChance = Math.min(1.0F, effectChance * 1.25F);
               }

               if (this.random.nextFloat() < effectChance) {
                  MobEffect effect = (MobEffect)BuiltInRegistries.MOB_EFFECT.get(effectLocation);
                  if (effect != null) {
                     int duration = this.getProjectile().getImpactEffectDuration();
                     if (headshot) {
                        duration = (int)((float)duration * 1.25F);
                     }

                     livingEntityx.addEffect(new MobEffectInstance(top.ribs.scguns.util.ScEffects.holder(effect), duration, this.getProjectile().getImpactEffectAmplifier()));
                  }
               }
            }
         }
      }

      if (entity instanceof LivingEntity) {
         GunEnchantmentHelper.applyElementalPopEffect(this.getWeapon(), (LivingEntity)entity);
      }

      if (this.shooter instanceof Player) {
         int hitType = critical ? 2 : (headshot ? 1 : 0);
         PacketHandler.getPlayChannel()
            .sendToPlayer(
               () -> (ServerPlayer)this.shooter,
               new S2CMessageProjectileHitEntity(hitVec.x, hitVec.y, hitVec.z, hitType, entity instanceof Player)
            );
      }

      if (wasAlive && entity instanceof LivingEntity livingEntityxx && !livingEntityxx.isAlive()) {
         this.checkForDiamondSteelBonus(livingEntityxx, hitVec);
         this.checkForBeowulfXPBonus(livingEntityxx, hitVec);
      }

      PacketHandler.getPlayChannel().sendToTrackingEntity(() -> entity, new S2CMessageBlood(hitVec.x, hitVec.y, hitVec.z, entity.getType()));
      this.spawnExplosionParticles(hitVec);
   }

   private void checkForBeowulfXPBonus(LivingEntity killedEntity, Vec3 position) {
      if (!this.level().isClientSide && this.getShooter() instanceof Player) {
         int baseXP = killedEntity.getExperienceReward((net.minecraft.server.level.ServerLevel) this.level(), null);
         int beowulfXP = Math.round((float)baseXP * 0.75F);
         if (beowulfXP > 0) {
            ExperienceOrb xpOrb = new ExperienceOrb(this.level(), position.x, position.y, position.z, beowulfXP);
            this.level().addFreshEntity(xpOrb);
         }
      }
   }

   @Override
   protected void onHitBlock(BlockState state, BlockPos pos, Direction face, double x, double y, double z) {
      this.spawnExplosionParticles(new Vec3(x, y + 0.1, z));
   }

   @Override
   public void onExpired() {
      this.spawnExplosionParticles(new Vec3(this.getX(), this.getY() + 0.1, this.getZ()));
   }

   private void spawnExplosionParticles(Vec3 position) {
      if (!this.level().isClientSide) {
         ServerLevel serverLevel = (ServerLevel)this.level();
         int particleCount = 5;
         serverLevel.sendParticles(
            (SimpleParticleType)ModParticleTypes.BEOWULF_IMPACT.get(),
            position.x,
            position.y,
            position.z,
            particleCount,
            0.0,
            0.0,
            0.0,
            0.1
         );

         for (int i = 0; i < particleCount; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 0.2;
            double offsetY = (this.random.nextDouble() - 0.5) * 0.2;
            double offsetZ = (this.random.nextDouble() - 0.5) * 0.2;
            double speedX = (this.random.nextDouble() - 0.5) * 0.5;
            double speedY = (this.random.nextDouble() - 0.5) * 0.5;
            double speedZ = (this.random.nextDouble() - 0.5) * 0.5;
            serverLevel.sendParticles(
               ParticleTypes.FLAME, position.x + offsetX, position.y + offsetY, position.z + offsetZ, 1, speedX, speedY, speedZ, 0.1
            );
         }
      }
   }
}
