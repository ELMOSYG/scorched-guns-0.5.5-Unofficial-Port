package top.ribs.scguns.entity.block;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity.MovementEmission;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import top.ribs.scguns.init.ModEntities;

public class PrimedPowderKeg extends Entity {
   private static final EntityDataAccessor<Integer> DATA_FUSE_ID = SynchedEntityData.defineId(PrimedPowderKeg.class, EntityDataSerializers.INT);
   private static final int DEFAULT_FUSE_TIME = 40;
   @Nullable
   private LivingEntity owner;

   public PrimedPowderKeg(EntityType<? extends PrimedPowderKeg> entityType, Level level) {
      super(entityType, level);
      this.blocksBuilding = true;
   }

   public PrimedPowderKeg(Level level, double x, double y, double z, @Nullable LivingEntity owner) {
      this((EntityType<? extends PrimedPowderKeg>)ModEntities.PRIMED_POWDER_KEG.get(), level);
      this.setPos(x, y, z);
      double d0 = level.random.nextDouble() * (float) (Math.PI * 2);
      this.setDeltaMovement(-Math.sin(d0) * 0.02, 0.2, -Math.cos(d0) * 0.02);
      this.setFuse(40);
      this.xo = x;
      this.yo = y;
      this.zo = z;
      this.owner = owner;
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(DATA_FUSE_ID, 40);
   }

   protected MovementEmission getMovementEmission() {
      return MovementEmission.NONE;
   }

   public boolean isPickable() {
      return !this.isRemoved();
   }

   public void tick() {
      if (!this.isNoGravity()) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
      }

      this.move(MoverType.SELF, this.getDeltaMovement());
      this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
      if (this.onGround()) {
         this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
      }

      int i = this.getFuse() - 1;
      this.setFuse(i);
      if (i <= 0) {
         this.discard();
         if (!this.level().isClientSide) {
            this.explode();
         }
      } else {
         this.updateInWaterStateAndDoFluidPushing();
         if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 0.0, 0.0, 0.0);
         }
      }
   }

   protected void explode() {
      float explosionPower = 7.0F;
      this.level().explode(this, this.getX(), this.getY(0.0625), this.getZ(), explosionPower, ExplosionInteraction.TNT);
   }

   protected void addAdditionalSaveData(CompoundTag compound) {
      compound.putShort("Fuse", (short)this.getFuse());
   }

   protected void readAdditionalSaveData(CompoundTag compound) {
      this.setFuse(compound.getShort("Fuse"));
   }

   @Nullable
   public LivingEntity getOwner() {
      return this.owner;
   }

   @Override
   public EntityDimensions getDimensions(Pose pose) {
      return super.getDimensions(pose).withEyeHeight(0.15F);
   }

   public void setFuse(int fuse) {
      this.entityData.set(DATA_FUSE_ID, fuse);
   }

   public int getFuse() {
      return (Integer)this.entityData.get(DATA_FUSE_ID);
   }
}
