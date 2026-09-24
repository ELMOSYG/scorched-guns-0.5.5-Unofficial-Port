package top.ribs.scguns.entity.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.init.ModTags;

public class ScampRocketEntity extends Projectile {
   private static final EntityDataAccessor<Boolean> HAS_EXPLODED = SynchedEntityData.defineId(ScampRocketEntity.class, EntityDataSerializers.BOOLEAN);
   private int life = 0;
   private double damage = 8.0;
   private float explosionRadius = 3.0F;

   public ScampRocketEntity(EntityType<? extends ScampRocketEntity> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public ScampRocketEntity(EntityType<? extends ScampRocketEntity> pEntityType, Level pLevel, LivingEntity pShooter) {
      this(pEntityType, pLevel);
      this.setOwner(pShooter);
      this.setPos(pShooter.getX(), pShooter.getEyeY() - 0.1, pShooter.getZ());
   }

   public void setDamage(double pDamage) {
      this.damage = pDamage;
   }

   public double getDamage() {
      return this.damage;
   }

   public void setExplosionRadius(float radius) {
      this.explosionRadius = radius;
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(HAS_EXPLODED, false);
   }

   public void tick() {
      super.tick();
      if (!(Boolean)this.entityData.get(HAS_EXPLODED)) {
         Vec3 vec3 = this.getDeltaMovement();
         HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
         if (hitresult.getType() != Type.MISS) {
            this.onHit(hitresult);
         }

         double d0 = this.getX() + vec3.x;
         double d1 = this.getY() + vec3.y;
         double d2 = this.getZ() + vec3.z;
         this.setPos(d0, d1, d2);
         if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            if (this.random.nextInt(2) == 0) {
               this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }

            if (this.random.nextInt(2) == 0) {
               this.level().addParticle(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
         }

         if (++this.life >= 200) {
            this.explode();
         }
      }
   }

   protected void onHit(@NotNull HitResult pResult) {
      super.onHit(pResult);
      if (!this.level().isClientSide && !(Boolean)this.entityData.get(HAS_EXPLODED)) {
         this.explode();
      }
   }

   protected void onHitEntity(@NotNull EntityHitResult pResult) {
      super.onHitEntity(pResult);
      Entity entity = pResult.getEntity();
      if (!entity.equals(this.getOwner()) || this.life >= 5) {
         if (entity instanceof LivingEntity) {
            entity.hurt(this.damageSources().explosion(this, this.getOwner()), (float)this.damage);
         }
      }
   }

   private void explode() {
      if (!(Boolean)this.entityData.get(HAS_EXPLODED)) {
         this.entityData.set(HAS_EXPLODED, true);
         if (!this.level().isClientSide) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), this.explosionRadius, ExplosionInteraction.NONE);
            this.destroyBreakableBlocks();
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, this.getSoundSource(), 1.0F, 1.0F);
            this.discard();
         } else {
            for (int i = 0; i < 20; i++) {
               double offsetX = this.random.nextGaussian() * 0.3;
               double offsetY = this.random.nextGaussian() * 0.3;
               double offsetZ = this.random.nextGaussian() * 0.3;
               this.level().addParticle(ParticleTypes.EXPLOSION, this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ, 0.0, 0.0, 0.0);
            }
         }
      }
   }

   private void destroyBreakableBlocks() {
      if (!this.level().isClientSide) {
         double breakRadius = (double)this.explosionRadius * 1.5;
         BlockPos center = this.blockPosition();
         int range = (int)Math.ceil(breakRadius);

         for (BlockPos pos : BlockPos.betweenClosed(center.offset(-range, -range, -range), center.offset(range, range, range))) {
            double distance = Math.sqrt(pos.distToCenterSqr(this.position()));
            if (!(distance > breakRadius)) {
               BlockState state = this.level().getBlockState(pos);
               if (!state.isAir() && state.is(ModTags.Blocks.TANK_BREAKABLE)) {
                  float breakChance = 1.0F - (float)(distance / breakRadius);
                  breakChance *= 0.8F;
                  if (this.random.nextFloat() < breakChance) {
                     Level var12 = this.level();
                     if (var12 instanceof ServerLevel) {
                        ServerLevel serverLevel = (ServerLevel)var12;
                        if (serverLevel.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) && this.random.nextFloat() < 0.15F) {
                           Block.dropResources(state, serverLevel, pos, null, null, ItemStack.EMPTY);
                        }

                        this.level().destroyBlock(pos, false);
                        serverLevel.sendParticles(
                           ParticleTypes.CLOUD,
                           (double)pos.getX() + 0.5,
                           (double)pos.getY() + 0.5,
                           (double)pos.getZ() + 0.5,
                           3,
                           0.25,
                           0.25,
                           0.25,
                           0.05
                        );
                     }
                  }
               }
            }
         }
      }
   }

   public boolean isPickable() {
      return !(Boolean)this.entityData.get(HAS_EXPLODED);
   }

   public float getPickRadius() {
      return 1.0F;
   }

   protected boolean canHitEntity(@NotNull Entity pTarget) {
      return super.canHitEntity(pTarget) && !pTarget.noPhysics;
   }

   public boolean isOnFire() {
      return false;
   }

   public boolean shouldRenderAtSqrDistance(double pDistance) {
      return pDistance < 16384.0;
   }

   protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      pCompound.putInt("life", this.life);
      pCompound.putDouble("damage", this.damage);
      pCompound.putFloat("explosionRadius", this.explosionRadius);
      pCompound.putBoolean("hasExploded", (Boolean)this.entityData.get(HAS_EXPLODED));
   }

   protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      this.life = pCompound.getInt("life");
      this.damage = pCompound.getDouble("damage");
      this.explosionRadius = pCompound.getFloat("explosionRadius");
      this.entityData.set(HAS_EXPLODED, pCompound.getBoolean("hasExploded"));
   }

   @NotNull


   public void shoot(double pX, double pY, double pZ, float pVelocity, float pInaccuracy) {
      Vec3 vec3 = new Vec3(pX, pY, pZ)
         .normalize()
         .add(
            this.random.triangle(0.0, 0.0172275 * (double)pInaccuracy),
            this.random.triangle(0.0, 0.0172275 * (double)pInaccuracy),
            this.random.triangle(0.0, 0.0172275 * (double)pInaccuracy)
         )
         .scale((double)pVelocity);
      this.setDeltaMovement(vec3);
      double d0 = vec3.horizontalDistance();
      this.setYRot((float)(Mth.atan2(vec3.x, vec3.z) * (180.0 / Math.PI)));
      this.setXRot((float)(Mth.atan2(vec3.y, d0) * (180.0 / Math.PI)));
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
   }
}
