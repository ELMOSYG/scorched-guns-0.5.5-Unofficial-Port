package top.ribs.scguns.entity.projectile;



import top.ribs.scguns.util.ScEnchants;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.List;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModDamageTypes;
import top.ribs.scguns.init.ModParticleTypes;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageProjectileHitEntity;
import top.ribs.scguns.util.GunEnchantmentHelper;

public class PlasmaProjectileEntity extends ProjectileEntity {
   private static final float SHIELD_DISABLE_CHANCE = 0.7F;
   private static final float SHIELD_DAMAGE_PENETRATION = 0.25F;
   private static final float HEADSHOT_EFFECT_DURATION_MULTIPLIER = 1.5F;
   private static final float SPLASH_DAMAGE_RADIUS = 2.0F;
   private static final float SPLASH_DAMAGE_FALLOFF = 0.7F;
   private static final float SPLASH_EFFECT_CHANCE_MULTIPLIER = 0.4F;
   private static final float FIRE_CHANCE = 0.2F;

   public PlasmaProjectileEntity(EntityType<? extends Entity> entityType, Level worldIn) {
      super(entityType, worldIn);
   }

   public PlasmaProjectileEntity(EntityType<? extends Entity> entityType, Level worldIn, LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun) {
      super(entityType, worldIn, shooter, weapon, item, modifiedGun);
   }

   @Override
   protected void onProjectileTick() {
      if (this.level().isClientSide && this.tickCount > 1 && this.tickCount < this.life) {
         if (this.tickCount % 2 == 0) {
            double offsetX = (this.random.nextDouble() - 0.5) * 0.5;
            double offsetY = (this.random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (this.random.nextDouble() - 0.5) * 0.5;
            this.level()
               .addParticle(
                  (ParticleOptions)ModParticleTypes.GREEN_FLAME.get(),
                  true,
                  this.getX() + offsetX,
                  this.getY() + offsetY,
                  this.getZ() + offsetZ,
                  0.0,
                  0.0,
                  0.0
               );
         }

         if (this.tickCount % 5 == 0) {
            this.level().addParticle((ParticleOptions)ModParticleTypes.PLASMA_RING.get(), true, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         }
      }
   }

   @Override
   protected void onHitEntity(Entity entity, Vec3 hitVec, Vec3 startVec, Vec3 endVec, boolean headshot) {
      float directDamage = this.getDamage();
      float newDamage = this.getCriticalDamage(this.getWeapon(), this.random, directDamage);
      boolean critical = directDamage != newDamage;
      directDamage = newDamage * this.advantageMultiplier(entity);
      boolean wasAlive = entity instanceof LivingEntity && entity.isAlive();
      if (headshot) {
         directDamage = (float)((double)directDamage * (Double)Config.COMMON.gameplay.headShotDamageMultiplier.get());
      }

      if (entity instanceof LivingEntity livingTarget) {
         directDamage = this.applyProjectileProtection(livingTarget, directDamage);
      }

      DamageSource source = ModDamageTypes.Sources.projectile(this.level().registryAccess(), this, (LivingEntity)this.getOwner());
      boolean blocked = ProjectileEntity.ProjectileHelper.handleShieldHit(entity, this, directDamage, 0.7F);
      if (blocked) {
         float penetratingDamage = directDamage * 0.25F;
         entity.hurt(source, penetratingDamage);
         if (entity instanceof LivingEntity livingEntity) {
            this.applyEffect(livingEntity, 0.25F, headshot);
         }
      } else {
         entity.hurt(source, directDamage);
         if (entity instanceof LivingEntity livingEntity) {
            this.applyEffect(livingEntity, 1.0F, headshot);
         }
      }

      if (entity instanceof LivingEntity && this.random.nextFloat() < 0.2F) {
         entity.igniteForSeconds(2);
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

      this.applySplashDamage(hitVec, directDamage * 0.6F);
      this.spawnPlasmaParticles(hitVec);
      if (wasAlive && entity instanceof LivingEntity livingEntity && !livingEntity.isAlive()) {
         this.checkForDiamondSteelBonus(livingEntity, hitVec);
      }
   }

   private void applySplashDamage(Vec3 center, float baseSplashDamage) {
      if (!this.level().isClientSide()) {
         List<LivingEntity> nearbyEntities = this.level()
            .getEntitiesOfClass(
               LivingEntity.class,
               new AABB(
                  center.x - 2.0, center.y - 2.0, center.z - 2.0, center.x + 2.0, center.y + 2.0, center.z + 2.0
               )
            );
         DamageSource splashSource = ModDamageTypes.Sources.projectile(this.level().registryAccess(), this, this.getShooter());

         for (LivingEntity target : nearbyEntities) {
            double distance = target.position().distanceTo(center);
            if (!(distance > 2.0)) {
               float distanceRatio = (float)(distance / 2.0);
               float damageMultiplier = 1.0F - distanceRatio * 0.3F;
               float splashDamage = baseSplashDamage * damageMultiplier;
               if (target == this.getShooter()) {
                  splashDamage *= 0.4F;
               }

               splashDamage = this.applyProjectileProtection(target, splashDamage);
               if (splashDamage > 0.5F) {
                  target.hurt(splashSource, splashDamage);
                  this.applyEffect(target, damageMultiplier * 0.4F, false);
                  float splashFireChance = 0.2F * damageMultiplier * 0.5F;
                  if (this.random.nextFloat() < splashFireChance) {
                     target.igniteForSeconds(2);
                  }
               }
            }
         }
      }
   }

   @Override
   public float applyProjectileProtection(LivingEntity target, float damage) {
      int protectionLevel = ScEnchants.level(target, Enchantments.PROJECTILE_PROTECTION);
      if (protectionLevel > 0) {
         float reduction = (float)protectionLevel * 0.07F;
         reduction = Math.min(reduction, 0.56F);
         damage *= 1.0F - reduction;
      }

      return damage;
   }

   private void applyEffect(LivingEntity target, float powerMultiplier, boolean headshot) {
      ResourceLocation effectLocation = this.getProjectile().getImpactEffect();
      if (effectLocation != null) {
         float effectChance = this.getProjectile().getImpactEffectChance() * powerMultiplier;
         if (headshot) {
            effectChance = Math.min(1.0F, effectChance * 1.25F);
         }

         if (this.random.nextFloat() < effectChance) {
            MobEffect effect = (MobEffect)BuiltInRegistries.MOB_EFFECT.get(effectLocation);
            if (effect != null) {
               int duration = this.getProjectile().getImpactEffectDuration();
               if (headshot) {
                  duration = (int)((float)duration * 1.5F);
               }

               duration = (int)((float)duration * powerMultiplier);
               target.addEffect(new MobEffectInstance(top.ribs.scguns.util.ScEffects.holder(effect), duration, this.getProjectile().getImpactEffectAmplifier()));
            }
         }
      }
   }

   @Override
   protected void onHitBlock(BlockState state, BlockPos pos, Direction face, double x, double y, double z) {
      Vec3 hitPos = new Vec3(x, y, z);
      this.applySplashDamage(hitPos, this.getDamage() * 0.4F);
      this.spawnPlasmaParticles(hitPos);
   }

   @Override
   public void onExpired() {
      Vec3 pos = this.position();
      this.applySplashDamage(pos, this.getDamage() * 0.3F);
      this.spawnPlasmaParticles(pos);
   }

   private void spawnPlasmaParticles(Vec3 position) {
      if (!this.level().isClientSide) {
         ServerLevel serverLevel = (ServerLevel)this.level();
         serverLevel.sendParticles(
            (SimpleParticleType)ModParticleTypes.PLASMA_EXPLOSION.get(), position.x, position.y, position.z, 1, 0.0, 0.0, 0.0, 0.1
         );

         for (int i = 0; i < 12; i++) {
            double angle = (double)i / 12.0 * 2.0 * Math.PI;
            double radius = 0.3 + this.random.nextDouble() * 0.8;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double offsetY = (this.random.nextDouble() - 0.5) * 0.3;
            double speedX = offsetX * 0.08;
            double speedY = (this.random.nextDouble() - 0.3) * 0.15;
            double speedZ = offsetZ * 0.08;
            serverLevel.sendParticles(
               (SimpleParticleType)ModParticleTypes.GREEN_FLAME.get(),
               position.x + offsetX,
               position.y + offsetY,
               position.z + offsetZ,
               1,
               speedX,
               speedY,
               speedZ,
               0.05
            );
         }

         for (int i = 0; i < 6; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 1.5;
            double offsetY = (this.random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (this.random.nextDouble() - 0.5) * 1.5;
            double speedX = (this.random.nextDouble() - 0.5) * 0.2;
            double speedY = (this.random.nextDouble() - 0.5) * 0.2;
            double speedZ = (this.random.nextDouble() - 0.5) * 0.2;
            serverLevel.sendParticles(
               ParticleTypes.SOUL_FIRE_FLAME, position.x + offsetX, position.y + offsetY, position.z + offsetZ, 1, speedX, speedY, speedZ, 0.1
            );
         }
      }
   }
}
