package top.ribs.scguns.entity.throwable;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

public abstract class ThrowableItemEntity extends ThrowableProjectile implements IEntityWithComplexSpawn {
   private ItemStack item = ItemStack.EMPTY;
   boolean shouldBounce;
   private float gravityVelocity = 0.03F;
   private int maxLife = 200;

   public ThrowableItemEntity(EntityType<? extends ThrowableItemEntity> entityType, Level worldIn) {
      super(entityType, worldIn);
   }

   public ThrowableItemEntity(EntityType<? extends ThrowableItemEntity> entityType, Level world, LivingEntity player) {
      super(entityType, player, world);
   }

   public ThrowableItemEntity(EntityType<? extends ThrowableItemEntity> entityType, Level world, double x, double y, double z) {
      super(entityType, x, y, z, world);
   }

   public void setItem(ItemStack item) {
      this.item = item;
   }

   public ItemStack getItem() {
      return this.item;
   }

   public void setShouldBounce(boolean shouldBounce) {
      this.shouldBounce = shouldBounce;
   }

   protected void setGravityVelocity(float gravity) {
      this.gravityVelocity = gravity;
   }

   /**
    * 1.21 makes {@code Entity#getGravity()} final and moves the per-entity value into
    * {@code getDefaultGravity()}, so the 0.5.5 override has to move with it. {@link #isNoGravity()}
    * still returns false, so {@code getGravity()} keeps reporting {@code gravityVelocity}.
    */
   protected double getDefaultGravity() {
      return (double)this.gravityVelocity;
   }

   public void setMaxLife(int maxLife) {
      this.maxLife = maxLife;
   }

   public void tick() {
      super.tick();
      if (this.shouldBounce && this.tickCount >= this.maxLife) {
         this.remove(RemovalReason.KILLED);
         this.onDeath();
      }
   }

   public void onDeath() {
   }

   protected void onHit(HitResult result) {
      switch (result.getType()) {
         case BLOCK:
            BlockHitResult blockResult = (BlockHitResult)result;
            if (this.shouldBounce) {
               BlockPos resultPos = blockResult.getBlockPos();
               BlockState state = this.level().getBlockState(resultPos);
               SoundEvent event = state.getBlock().getSoundType(state, this.level(), resultPos, this).getStepSound();
               double speed = this.getDeltaMovement().length();
               if (speed > 0.1) {
                  this.level()
                     .playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z, event, SoundSource.AMBIENT, 1.0F, 1.0F);
               }

               this.bounce(blockResult.getDirection());
            } else {
               this.remove(RemovalReason.KILLED);
               this.onDeath();
            }
            break;
         case ENTITY:
            EntityHitResult entityResult = (EntityHitResult)result;
            Entity entity = entityResult.getEntity();
            if (this.shouldBounce) {
               double speed = this.getDeltaMovement().length();
               if (speed > 0.1) {
                  entity.hurt(entity.damageSources().thrown(this, this.getOwner()), 1.0F);
               }

               this.bounce(Direction.getNearest(this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z()).getOpposite());
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.25, 1.0, 0.25));
            } else {
               this.remove(RemovalReason.KILLED);
               this.onDeath();
            }
      }
   }

   void bounce(Direction direction) {
      switch (direction.getAxis()) {
         case X:
            this.setDeltaMovement(this.getDeltaMovement().multiply(-0.5, 0.75, 0.75));
            break;
         case Y:
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.75, -0.25, 0.75));
            if (this.getDeltaMovement().y() < (double)this.getGravity()) {
               this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.0, 1.0));
            }
            break;
         case Z:
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.75, 0.75, -0.5));
      }
   }

   public boolean isNoGravity() {
      return false;
   }

   public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
      buffer.writeBoolean(this.shouldBounce);
      buffer.writeFloat(this.gravityVelocity);
      ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, this.item);
   }

   public void readSpawnData(RegistryFriendlyByteBuf buffer) {
      this.shouldBounce = buffer.readBoolean();
      this.gravityVelocity = buffer.readFloat();
      this.item = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
   }


}
