package top.ribs.scguns.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.config.EnemyProjectileConfig;
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.init.ModSounds;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageTurretBulletTrail;

public class EnemyProjectileEntity extends AbstractArrow {
   private boolean trailSpawned = false;

   public EnemyProjectileEntity(EntityType<? extends AbstractArrow> type, Level world) {
      super(type, world);
   }

   public EnemyProjectileEntity(Level world, LivingEntity shooter) {
      this((EntityType<EnemyProjectileEntity>)ModEntities.ENEMY_PROJECTILE.get(), world, shooter);
   }

   public EnemyProjectileEntity(EntityType<EnemyProjectileEntity> type, Level world, LivingEntity shooter) {
      super(type, world);
      // 1.21 removed AbstractArrow's (EntityType, LivingEntity, Level) constructor; its
      // replacement also takes the pickup stack and the firing weapon, so the 1.20.1 spawn
      // placement and owner are reproduced directly instead of fabricating those arguments.
      this.setPos(shooter.getX(), shooter.getEyeY() - 0.1F, shooter.getZ());
      this.setOwner(shooter);
      this.setBaseDamage(EnemyProjectileConfig.getDamageForEntity(shooter.getType()));
   }

   /**
    * 1.21 replaces the abstract {@code getPickupItem()} with {@code getDefaultPickupItem()};
    * 0.5.5 never let these rounds be picked up.
    */
   @Override
   protected ItemStack getDefaultPickupItem() {
      return ItemStack.EMPTY;
   }

   protected ItemStack getPickupItem() {
      return ItemStack.EMPTY;
   }

   protected void onHitEntity(EntityHitResult result) {
      if (result.getEntity() instanceof LivingEntity livingEntity) {
         int damage = Mth.ceil(this.getBaseDamage());
         if (livingEntity.hurt(this.damageSources().arrow(this, this.getOwner()), (float)damage) && livingEntity.isAlive()) {
            this.doPostHurtEffects(livingEntity);
         }

         livingEntity.setArrowCount(livingEntity.getArrowCount() - 1);
      }

      this.discard();
   }

   protected void onHitBlock(BlockHitResult result) {
      super.onHitBlock(result);
      this.discard();
   }

   public void tick() {
      if (this.level().isClientSide || !this.inGround && this.tickCount <= 300) {
         if (!this.level().isClientSide && !this.trailSpawned && this.tickCount == 1) {
            this.spawnBulletTrail();
            this.trailSpawned = true;
         }

         super.tick();
         if (!this.inGround && !this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.01, 0.0));
         }
      } else {
         this.discard();
      }
   }

   private void spawnBulletTrail() {
      Vec3 position = this.position();
      Vec3 motion = this.getDeltaMovement();
      int trailColor = 16755200;
      double trailLength = 1.0;
      int maxAge = 300;
      double trailThickness = 0.7;
      S2CMessageTurretBulletTrail message = new S2CMessageTurretBulletTrail(this.getId(), position, motion, trailColor, trailLength, maxAge, trailThickness);
      PacketHandler.getPlayChannel().sendToTrackingEntity(() -> this, message);
   }

   protected void onHit(HitResult hitResult) {
      super.onHit(hitResult);
      this.discard();
   }

   @NotNull
   protected SoundEvent getDefaultHitGroundSoundEvent() {
      return (SoundEvent)ModSounds.BULLET_FLYBY.get();
   }

   public void playSound(SoundEvent soundEvent, float volume, float pitch) {
   }

   public void addAdditionalSaveData(CompoundTag compound) {
      super.addAdditionalSaveData(compound);
      compound.putDouble("damage", this.getBaseDamage());
      compound.putBoolean("TrailSpawned", this.trailSpawned);
   }

   public void readAdditionalSaveData(CompoundTag compound) {
      super.readAdditionalSaveData(compound);
      if (compound.contains("damage")) {
         this.setBaseDamage(compound.getDouble("damage"));
      }

      this.trailSpawned = compound.getBoolean("TrailSpawned");
   }



   public void handleInsidePortal(BlockPos pos) {
      this.discard();
   }

   public boolean isCritArrow() {
      return false;
   }

   public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
      super.shoot(x, y, z, velocity, inaccuracy);
   }

   public void setEnchantmentEffectsFromEntity(LivingEntity pShooter, float pVelocity) {
   }
}
