package top.ribs.scguns.entity.projectile;



import top.ribs.scguns.util.ScEnchants;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.effect.CustomExplosion;
import top.ribs.scguns.init.ModDamageTypes;
import top.ribs.scguns.init.ModParticleTypes;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.util.GunEnchantmentHelper;

public class MicroJetEntity extends ProjectileEntity {
   public static final float EXPLOSION_DAMAGE_MULTIPLIER = 2.0F;
   private static final float MAX_DAMAGE_MULTIPLIER = 1.5F;
   private static final int TICKS_TO_MAX_SPEED = 20;
   private static final float SHIELD_DISABLE_CHANCE = 0.75F;
   private static final float SHIELD_DAMAGE_PENETRATION = 0.2F;
   private static final float HEADSHOT_EFFECT_DURATION_MULTIPLIER = 1.5F;
   private static final float AREA_EFFECT_DURATION_MULTIPLIER = 0.75F;
   private int ticksInFlight = 0;

   public MicroJetEntity(EntityType<? extends ProjectileEntity> entityType, Level worldIn) {
      super(entityType, worldIn);
   }

   public MicroJetEntity(
      EntityType<? extends ProjectileEntity> entityType, Level worldIn, LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun
   ) {
      super(entityType, worldIn, shooter, weapon, item, modifiedGun);
   }

   @Override
   protected void onProjectileTick() {
      this.ticksInFlight++;
      if (this.level().isClientSide) {
         for (int i = 2; i > 0; i--) {
            this.level()
               .addParticle(
                  (ParticleOptions)ModParticleTypes.ROCKET_TRAIL.get(),
                  true,
                  this.getX() - this.getDeltaMovement().x() / (double)i,
                  this.getY() - this.getDeltaMovement().y() / (double)i,
                  this.getZ() - this.getDeltaMovement().z() / (double)i,
                  0.0,
                  0.0,
                  0.0
               );
         }

         if (this.level().random.nextInt(4) == 0) {
            this.level().addParticle(ParticleTypes.SMALL_FLAME, true, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            this.level().addParticle(ParticleTypes.SMOKE, true, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         }
      }
   }

   @Override
   public float getDamage() {
      float baseDamage = super.getDamage();
      float speedMultiplier = this.calculateSpeedMultiplier();
      return baseDamage * speedMultiplier;
   }

   private float calculateSpeedMultiplier() {
      if (this.ticksInFlight >= 20) {
         return 1.5F;
      } else {
         float progress = (float)this.ticksInFlight / 20.0F;
         return 1.0F + progress * 0.5F;
      }
   }

   @Override
   protected void onHitEntity(Entity entity, Vec3 hitVec, Vec3 startVec, Vec3 endVec, boolean headshot) {
      float damage = this.getDamage();
      if (headshot) {
         damage = (float)((double)damage * (Double)Config.COMMON.gameplay.headShotDamageMultiplier.get());
      }

      if (entity instanceof LivingEntity livingTarget) {
         damage = this.applyBlastProtection(livingTarget, damage);
      }

      boolean wasAlive = entity instanceof LivingEntity && entity.isAlive();
      DamageSource source = ModDamageTypes.Sources.projectile(this.level().registryAccess(), this, (LivingEntity)this.getOwner());
      boolean blocked = ProjectileEntity.ProjectileHelper.handleShieldHit(entity, this, damage, 0.75F);
      if (blocked) {
         float penetratingDamage = damage * 0.2F;
         entity.hurt(source, penetratingDamage);
         if (entity instanceof LivingEntity livingEntity) {
            this.applyEffect(livingEntity, 0.2F, headshot);
         }
      } else {
         entity.hurt(source, damage);
         if (entity instanceof LivingEntity livingEntity) {
            this.applyEffect(livingEntity, 1.0F, headshot);
         }
      }

      if (entity instanceof LivingEntity) {
         GunEnchantmentHelper.applyElementalPopEffect(this.getWeapon(), (LivingEntity)entity);
      }

      this.applyAreaEffects(hitVec);
      if (wasAlive && entity instanceof LivingEntity livingEntity && !livingEntity.isAlive()) {
         this.checkForDiamondSteelBonus(livingEntity, hitVec);
      }

      createMiniExplosion(this, 1.0F);
   }

   private float applyBlastProtection(LivingEntity target, float damage) {
      int protectionLevel = ScEnchants.level(target, Enchantments.BLAST_PROTECTION);
      if (protectionLevel > 0) {
         float reduction = (float)protectionLevel * 0.08F;
         reduction = Math.min(reduction, 0.8F);
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

   private void applyAreaEffects(Vec3 center) {
      if (!this.level().isClientSide()) {
         ResourceLocation effectLocation = this.getProjectile().getImpactEffect();
         if (effectLocation != null) {
            MobEffect effect = (MobEffect)BuiltInRegistries.MOB_EFFECT.get(effectLocation);
            if (effect != null) {
               List<LivingEntity> nearbyEntities = this.level()
                  .getEntitiesOfClass(
                     LivingEntity.class,
                     new AABB(
                        center.x - 1.0,
                        center.y - 1.0,
                        center.z - 1.0,
                        center.x + 1.0,
                        center.y + 1.0,
                        center.z + 1.0
                     )
                  );
               float areaEffectChance = this.getProjectile().getImpactEffectChance() * 0.4F;

               for (LivingEntity entity : nearbyEntities) {
                  if (entity != this.getShooter()) {
                     this.applyEffect(entity, 0.3F, false);
                     double distance = entity.position().distanceTo(center);
                     float distanceMultiplier = (float)(1.0 - distance);
                     float finalChance = areaEffectChance * Math.max(0.0F, distanceMultiplier);
                     if (this.random.nextFloat() < finalChance) {
                        this.applyEffect(entity, 0.75F, false);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   protected void onHitBlock(BlockState state, BlockPos pos, Direction face, double x, double y, double z) {
      Vec3 hitPos = new Vec3(x, y, z);
      this.applyAreaEffects(hitPos);
      createMiniExplosion(this, 1.0F);
   }

   @Override
   public void onExpired() {
      Vec3 pos = this.position();
      this.applyAreaEffects(pos);
      createMiniExplosion(this, 1.0F);
   }

   public static void createMiniExplosion(Entity entity, float radius) {
      Level world = entity.level();
      if (!world.isClientSide) {
         CustomExplosion explosion = new CustomExplosion(
            world, entity, entity.getX(), entity.getY(), entity.getZ(), radius, false, CustomExplosion.CustomBlockInteraction.NONE
         ) {
         };
         if (!EventHooks.onExplosionStart(world, explosion)) {
            explosion.explode();
            explosion.finalizeExplosion(true);

            for (ServerPlayer player : ((ServerLevel)world).players()) {
               if (player.distanceToSqr(entity.getX(), entity.getY(), entity.getZ()) < 4096.0) {
                  player.connection
                     .send(
                        new ClientboundExplodePacket(
                           entity.getX(),
                           entity.getY(),
                           entity.getZ(),
                           radius,
                           explosion.getToBlow(),
                           (Vec3)explosion.getHitPlayers().get(player),
                           explosion.getBlockInteraction(),
                           explosion.getSmallExplosionParticles(),
                           explosion.getLargeExplosionParticles(),
                           explosion.getExplosionSound()
                        )
                     );
               }
            }
         }
      }
   }
}
