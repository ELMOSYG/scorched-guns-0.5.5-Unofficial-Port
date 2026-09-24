package top.ribs.scguns.entity.projectile;



import top.ribs.scguns.util.ScEnchants;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import top.ribs.scguns.Config;
import top.ribs.scguns.block.AutoTurretBlock;
import top.ribs.scguns.block.BasicTurretBlock;
import top.ribs.scguns.block.EnemyTurretBlock;
import top.ribs.scguns.block.ShotgunTurretBlock;
import top.ribs.scguns.block.SniperTurretBlock;
import top.ribs.scguns.blockentity.AutoTurretBlockEntity;
import top.ribs.scguns.blockentity.BasicTurretBlockEntity;
import top.ribs.scguns.blockentity.EnemyTurretBlockEntity;
import top.ribs.scguns.blockentity.ShotgunTurretBlockEntity;
import top.ribs.scguns.blockentity.SniperTurretBlockEntity;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModDamageTypes;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageBlood;
import top.ribs.scguns.util.GunEnchantmentHelper;
import top.ribs.scguns.util.math.ExtendedEntityRayTraceResult;

public class LightningProjectileEntity extends ProjectileEntity {
   private static final int MAX_BOUNCES = 3;
   private static final double BOUNCE_RANGE = 10.0;
   private static final float HEADSHOT_EFFECT_DURATION_MULTIPLIER = 1.5F;
   private static final float BOUNCE_EFFECT_REDUCTION = 0.55F;
   private int bouncesLeft;
   private float currentDamage;
   private final Set<Integer> hitEntities = new HashSet<>();

   public LightningProjectileEntity(EntityType<? extends Entity> entityType, Level worldIn) {
      super(entityType, worldIn);
      this.bouncesLeft = 3;
      this.currentDamage = this.getDamage();
   }

   public LightningProjectileEntity(
      EntityType<? extends Entity> entityType, Level worldIn, LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun
   ) {
      super(entityType, worldIn, shooter, weapon, item, modifiedGun);
      this.bouncesLeft = 3;
      this.currentDamage = this.getDamage();
      this.hitEntities.add(shooter.getId());
   }

   @Override
   protected void onHitEntity(Entity entity, Vec3 hitVec, Vec3 startVec, Vec3 endVec, boolean headshot) {
      if (entity instanceof LivingEntity livingEntity) {
         this.hitEntities.add(entity.getId());
         this.currentDamage = this.applyProjectileProtection(livingEntity, this.currentDamage);
         if (entity instanceof Creeper creeper && this.random.nextFloat() < 0.15F) {
            try {
               if (!creeper.isPowered()) {
                  CompoundTag nbt = new CompoundTag();
                  creeper.addAdditionalSaveData(nbt);
                  nbt.putBoolean("powered", true);
                  creeper.readAdditionalSaveData(nbt);
                  this.spawnLightningParticles(new Vec3(entity.getX(), entity.getY() + (double)entity.getEyeHeight(), entity.getZ()));
               }
            } catch (Exception var15) {
               this.spawnLightningParticles(new Vec3(entity.getX(), entity.getY() + (double)entity.getEyeHeight(), entity.getZ()));
            }
         }

         livingEntity.hurt(ModDamageTypes.Sources.projectile(this.level().registryAccess(), this, (LivingEntity)this.getOwner()), this.currentDamage);
         Vec3 entityPosition = new Vec3(entity.getX(), entity.getY() + (double)entity.getEyeHeight() * 0.5, entity.getZ());
         this.spawnLightningArc(this.position(), entityPosition);
         if (entity instanceof LivingEntity) {
            ResourceLocation effectLocation = this.getProjectile().getImpactEffect();
            if (effectLocation != null) {
               float effectChance = this.getProjectile().getImpactEffectChance();
               if (headshot) {
                  effectChance = Math.min(1.0F, effectChance * 1.25F);
               }

               float bounceChanceMultiplier = (float)Math.pow(0.55F, (double)(3 - this.bouncesLeft));
               effectChance *= bounceChanceMultiplier;
               if (this.random.nextFloat() < effectChance) {
                  MobEffect effect = (MobEffect)BuiltInRegistries.MOB_EFFECT.get(effectLocation);
                  if (effect != null) {
                     int duration = this.getProjectile().getImpactEffectDuration();
                     if (headshot) {
                        duration = (int)((float)duration * 1.5F);
                     }

                     float bounceMultiplier = (float)Math.pow(0.55F, (double)(3 - this.bouncesLeft));
                     duration = (int)((float)duration * bounceMultiplier);
                     int amplifier = Math.max(0, this.getProjectile().getImpactEffectAmplifier() - (3 - this.bouncesLeft));
                     livingEntity.addEffect(new MobEffectInstance(top.ribs.scguns.util.ScEffects.holder(effect), duration, amplifier));
                  }
               }
            }

            GunEnchantmentHelper.applyElementalPopEffect(this.getWeapon(), livingEntity);
         }

         if (this.bouncesLeft > 0) {
            this.bouncesLeft--;
            this.currentDamage *= 0.75F;
            LivingEntity nextTarget = this.findNextTarget(entity);
            if (nextTarget != null) {
               this.scheduleBounce(nextTarget, entityPosition);
            } else {
               this.discard();
            }
         } else {
            this.discard();
         }

         PacketHandler.getPlayChannel().sendToTrackingEntity(() -> entity, new S2CMessageBlood(hitVec.x, hitVec.y, hitVec.z, entity.getType()));
      }
   }

   private void scheduleBounce(LivingEntity nextTarget, Vec3 previousPosition) {
      if (this.level() instanceof ServerLevel serverLevel) {
         serverLevel.getServer().execute(() -> this.bounceToNextTarget(nextTarget, previousPosition));
      }
   }

   @Override
   public float applyProjectileProtection(LivingEntity target, float damage) {
      int protectionLevel = ScEnchants.level(target, Enchantments.PROJECTILE_PROTECTION);
      if (protectionLevel > 0) {
         float reduction = (float)protectionLevel * 0.1F;
         reduction = Math.min(reduction, 0.8F);
         damage *= 1.0F - reduction;
      }

      return damage;
   }

   private void bounceToNextTarget(LivingEntity nextTarget, Vec3 previousPosition) {
      if (nextTarget.getId() != this.getShooterId() && nextTarget != this.getShooter() && nextTarget != this.getOwner()) {
         Vec3 direction = nextTarget.position().subtract(this.position()).normalize();
         this.setDeltaMovement(direction.scale(1.5));
         this.setPos(nextTarget.getX(), nextTarget.getY() + (double)nextTarget.getEyeHeight() * 0.5, nextTarget.getZ());
         Vec3 nextTargetPosition = new Vec3(nextTarget.getX(), nextTarget.getY() + (double)nextTarget.getEyeHeight() * 0.5, nextTarget.getZ());
         this.spawnLightningArc(previousPosition, nextTargetPosition);
         this.onHitEntity(nextTarget, nextTargetPosition, this.position(), nextTargetPosition, false);
      } else {
         this.discard();
      }
   }

   @Override
   protected void onHitBlock(BlockState state, BlockPos pos, Direction face, double x, double y, double z) {
      if (state.getBlock() instanceof AutoTurretBlock && this.level().getBlockEntity(pos) instanceof AutoTurretBlockEntity turret) {
         turret.onHitByLightningProjectile();
      }

      if (state.getBlock() instanceof SniperTurretBlock && this.level().getBlockEntity(pos) instanceof SniperTurretBlockEntity turret) {
         turret.onHitByLightningProjectile();
      }

      if (state.getBlock() instanceof BasicTurretBlock && this.level().getBlockEntity(pos) instanceof BasicTurretBlockEntity turret) {
         turret.onHitByLightningProjectile();
      }

      if (state.getBlock() instanceof ShotgunTurretBlock && this.level().getBlockEntity(pos) instanceof ShotgunTurretBlockEntity turret) {
         turret.onHitByLightningProjectile();
      }

      if (state.getBlock() instanceof EnemyTurretBlock && this.level().getBlockEntity(pos) instanceof EnemyTurretBlockEntity turret) {
         turret.onHitByLightningProjectile();
      }

      this.spawnLightningParticles(new Vec3(x, y + 0.1, z));
      this.discard();
   }

   @Override
   public void onExpired() {
      this.spawnLightningParticles(new Vec3(this.getX(), this.getY() + 0.1, this.getZ()));
   }

   private void spawnLightningArc(Vec3 start, Vec3 end) {
      if (!this.level().isClientSide) {
         if (this.getShooter() != null) {
            Vec3 shooterPos = new Vec3(
               this.getShooter().getX(), this.getShooter().getY() + (double)this.getShooter().getEyeHeight() * 0.5, this.getShooter().getZ()
            );
            double distanceToShooterStart = start.distanceTo(shooterPos);
            double distanceToShooterEnd = end.distanceTo(shooterPos);
            if (distanceToShooterStart < 1.0 || distanceToShooterEnd < 1.0) {
               return;
            }
         }

         ServerLevel serverLevel = (ServerLevel)this.level();
         Vec3 direction = end.subtract(start);
         double distance = direction.length();
         direction = direction.normalize();
         double stepSize = 0.1;

         for (double d = 0.0; d < distance; d += stepSize) {
            Vec3 particlePos = start.add(direction.scale(d));
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, particlePos.x, particlePos.y, particlePos.z, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   private void spawnLightningParticles(Vec3 position) {
      if (!this.level().isClientSide) {
         ServerLevel serverLevel = (ServerLevel)this.level();
         int particleCount = 20;
         serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, position.x, position.y, position.z, particleCount, 0.0, 0.0, 0.0, 0.1);
      }
   }

   @Nullable
   @Override
   protected List<ProjectileEntity.EntityResult> findEntitiesOnPath(Vec3 startVec, Vec3 endVec) {
      List<ProjectileEntity.EntityResult> hitEntities = new ArrayList<>();

      for (Entity entity : this.level().getEntities(this, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0), PROJECTILE_TARGETS)) {
         if (entity.getId() != this.getShooterId()
            && entity != this.getShooter()
            && entity != this.getOwner()
            && !entity.equals(this.shooter)
            && !this.hitEntities.contains(entity.getId())) {
            ProjectileEntity.EntityResult result = this.getHitResult(entity, startVec, endVec);
            if (result != null) {
               hitEntities.add(result);
            }
         }
      }

      return hitEntities;
   }

   @Nullable
   @Override
   protected ProjectileEntity.EntityResult findEntityOnPath(Vec3 startVec, Vec3 endVec) {
      Vec3 hitVec = null;
      Entity hitEntity = null;
      boolean headshot = false;
      List<Entity> entities = this.level().getEntities(this, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0), PROJECTILE_TARGETS);
      double closestDistance = Double.MAX_VALUE;

      for (Entity entity : entities) {
         if (entity.getId() != this.getShooterId()
            && entity != this.getShooter()
            && entity != this.getOwner()
            && !entity.equals(this.shooter)
            && !this.hitEntities.contains(entity.getId())) {
            ProjectileEntity.EntityResult result = this.getHitResult(entity, startVec, endVec);
            if (result != null) {
               Vec3 hitPos = result.getHitPos();
               double distanceToHit = startVec.distanceTo(hitPos);
               if (distanceToHit < closestDistance) {
                  hitVec = hitPos;
                  hitEntity = entity;
                  closestDistance = distanceToHit;
                  headshot = result.isHeadshot();
               }
            }
         }
      }

      return hitEntity != null ? new ProjectileEntity.EntityResult(hitEntity, hitVec, headshot) : null;
   }

   private LivingEntity findNextTarget(Entity currentTarget) {
      List<LivingEntity> nearbyEntities = this.level().getEntitiesOfClass(LivingEntity.class, currentTarget.getBoundingBox().inflate(10.0));
      nearbyEntities.removeIf(
         entity -> this.hitEntities.contains(entity.getId())
               || entity == this.getOwner()
               || entity == currentTarget
               || entity == this.getShooter()
               || entity.getId() == this.getShooterId()
      );
      return !nearbyEntities.isEmpty() ? nearbyEntities.get(0) : null;
   }

   @Override
   public void tick() {
      if (!this.level().isClientSide && !this.isRemoved()) {
         this.updateHeading();
         this.onProjectileTick();
         if (!this.level().isClientSide()) {
            Vec3 startVec = this.position();
            Vec3 endVec = startVec.add(this.getDeltaMovement());
            ClipContext fluidContext = new ClipContext(startVec, endVec, Block.COLLIDER, Fluid.ANY, this);
            BlockHitResult fluidResult = this.level().clip(fluidContext);
            if (fluidResult.getType() == Type.BLOCK) {
               BlockPos blockPos = fluidResult.getBlockPos();
               BlockState blockState = this.level().getBlockState(blockPos);
               FluidState fluidState = blockState.getFluidState();
               if (fluidState.is(FluidTags.WATER)) {
                  if ((Boolean)Config.CLIENT.particle.enableWaterImpactParticles.get()) {
                     this.onWaterImpact(fluidResult.getLocation());
                  } else {
                     this.level()
                        .playSound(
                           null,
                           fluidResult.getLocation().x,
                           fluidResult.getLocation().y,
                           fluidResult.getLocation().z,
                           SoundEvents.PLAYER_SPLASH,
                           SoundSource.NEUTRAL,
                           1.2F,
                           1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F
                        );
                  }
               } else if (fluidState.is(FluidTags.LAVA)) {
                  this.onLavaImpact(fluidResult.getLocation());
               }
            }

            HitResult blockResult = rayTraceBlocks(this.level(), new ClipContext(startVec, endVec, Block.COLLIDER, Fluid.NONE, this), IGNORE_LEAVES);
            if (blockResult.getType() != Type.MISS) {
               endVec = blockResult.getLocation();
            }

            List<ProjectileEntity.EntityResult> hitEntitiesList = this.findCustomEntitiesOnPath(startVec, endVec);
            if (!hitEntitiesList.isEmpty()) {
               for (ProjectileEntity.EntityResult entityResult : hitEntitiesList) {
                  EntityHitResult result = new ExtendedEntityRayTraceResult(entityResult);
                  if (!(result.getEntity() instanceof Player player) || !(this.shooter instanceof Player) || ((Player)this.shooter).canHarmPlayer(player)) {
                     this.onHit(result, startVec, endVec);
                  }
               }
            } else if (blockResult.getType() != Type.MISS) {
               this.onHit(blockResult, startVec, endVec);
            }
         }

         double nextPosX = this.getX() + this.getDeltaMovement().x();
         double nextPosY = this.getY() + this.getDeltaMovement().y();
         double nextPosZ = this.getZ() + this.getDeltaMovement().z();
         this.setPos(nextPosX, nextPosY, nextPosZ);
         if (this.projectile.isGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, this.modifiedGravity, 0.0));
         }

         if (this.tickCount >= this.life) {
            if (this.isAlive()) {
               this.onExpired();
            }

            this.remove(RemovalReason.KILLED);
         }
      }
   }

   private List<ProjectileEntity.EntityResult> findCustomEntitiesOnPath(Vec3 startVec, Vec3 endVec) {
      List<ProjectileEntity.EntityResult> hitEntitiesList = new ArrayList<>();

      for (Entity entity : this.level().getEntities(this, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0), PROJECTILE_TARGETS)) {
         if (entity.getId() != this.getShooterId()
            && entity != this.getShooter()
            && entity != this.getOwner()
            && !entity.equals(this.shooter)
            && !this.hitEntities.contains(entity.getId())) {
            ProjectileEntity.EntityResult result = this.getHitResult(entity, startVec, endVec);
            if (result != null) {
               hitEntitiesList.add(result);
            }
         }
      }

      return hitEntitiesList;
   }
}
