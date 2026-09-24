package top.ribs.scguns.entity.projectile;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;

public class TraumaHookEntity extends FishingHook {
   private boolean isRetracting = false;
   private static final double GRAVITY = 0.03;
   private static final double AIR_RESISTANCE = 0.98;
   private static final double GROUND_FRICTION = 0.8;
   private static final int MAX_LIFETIME = 200;
   private static final double RETRACT_SPEED_BASE = 0.4;

   public TraumaHookEntity(EntityType<? extends TraumaHookEntity> entityType, Level level) {
      super(entityType, level);
      this.noCulling = true;
   }

   public TraumaHookEntity(EntityType<? extends TraumaHookEntity> entityType, LivingEntity owner, Level level) {
      super(entityType, level);
      this.setOwner(owner);
      this.noCulling = true;
      this.moveTo(owner.getX(), owner.getEyeY() - 0.1, owner.getZ(), owner.getYRot(), owner.getXRot());
   }

   protected void onHitEntity(EntityHitResult result) {
      Entity entity = result.getEntity();
      if (entity instanceof LivingEntity && entity != this.getOwner() && !this.isRetracting) {
         super.onHitEntity(result);
         this.setDeltaMovement(Vec3.ZERO);
      }
   }

   protected boolean canHitEntity(Entity entity) {
      return !this.isRetracting && entity instanceof LivingEntity && entity != this.getOwner();
   }

   public void tick() {
      this.baseTick();
      if (this.level().isClientSide()) {
         if (!this.isRetracting && !this.onGround()) {
            Vec3 motion = this.getDeltaMovement();
            motion = motion.add(0.0, -0.03, 0.0);
            motion = motion.scale(0.98);
            this.setDeltaMovement(motion);
            this.move(MoverType.SELF, motion);
         }
      } else {
         Entity owner = this.getOwner();
         if (owner != null && !owner.isRemoved()) {
            if (!this.isRetracting) {
               boolean shouldRetract = false;
               if (this.tickCount > 200) {
                  shouldRetract = true;
               } else if (this.onGround() && this.getHookedIn() == null && this.tickCount > 20) {
                  shouldRetract = true;
               } else if ((double)this.distanceTo(owner) > 32.0) {
                  shouldRetract = true;
               } else if (!owner.isAlive()) {
                  shouldRetract = true;
               }

               if (shouldRetract) {
                  this.startRetraction();
               }
            }

            if (this.isRetracting) {
               this.handleRetraction();
            } else {
               this.handleNormalFlight();
            }

            this.updateRotation();
         } else {
            this.discard();
         }
      }
   }

   private void handleNormalFlight() {
      HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
      if (hitResult.getType() != Type.MISS) {
         this.onHit(hitResult);
      } else {
         Vec3 motion = this.getDeltaMovement();
         if (!this.onGround()) {
            motion = motion.add(0.0, -0.03, 0.0);
            motion = motion.scale(0.98);
            this.setDeltaMovement(motion);
            this.move(MoverType.SELF, motion);
         } else {
            motion = motion.multiply(0.8, 0.0, 0.8);
            this.setDeltaMovement(motion);
         }
      }
   }

   private void startRetraction() {
      this.isRetracting = true;
      if (this.getHookedIn() != null) {
      }
   }

   private void handleRetraction() {
      Entity owner = this.getOwner();
      if (owner == null) {
         this.discard();
      } else {
         Vec3 ownerPos = new Vec3(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
         Vec3 hookPos = this.position();
         double distanceToOwner = hookPos.distanceTo(ownerPos);
         if (distanceToOwner < 1.0) {
            this.discard();
         } else {
            Vec3 direction = ownerPos.subtract(hookPos).normalize();
            double retractSpeed = 0.4 + distanceToOwner * 0.05;
            retractSpeed = Math.min(retractSpeed, 1.5);
            Vec3 retractVelocity = direction.scale(retractSpeed);
            this.setDeltaMovement(retractVelocity);
            this.move(MoverType.SELF, retractVelocity);
         }
      }
   }

   public void updateRotation() {
      Vec3 motion = this.getDeltaMovement();
      if (motion.horizontalDistanceSqr() > 1.0E-7) {
         this.setYRot((float)(Math.atan2(motion.x, motion.z) * 180.0 / Math.PI));
         this.setXRot((float)(Math.atan2(motion.y, motion.horizontalDistance()) * 180.0 / Math.PI));
         this.yRotO = this.getYRot();
         this.xRotO = this.getXRot();
      }
   }

   public boolean isRetracting() {
      return this.isRetracting;
   }

   public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
      Vec3 direction = new Vec3(x, y, z);
      direction = direction.normalize();
      direction = direction.add(
         this.random.triangle(0.0, 0.0172275 * (double)inaccuracy),
         this.random.triangle(0.0, 0.0172275 * (double)inaccuracy),
         this.random.triangle(0.0, 0.0172275 * (double)inaccuracy)
      );
      direction = direction.normalize().scale((double)velocity);
      this.setDeltaMovement(direction);
      this.setYRot((float)(Math.atan2(direction.x, direction.z) * 180.0 / Math.PI));
      this.setXRot((float)(Math.atan2(direction.y, direction.horizontalDistance()) * 180.0 / Math.PI));
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
   }

   public boolean shouldRenderAtSqrDistance(double distance) {
      return distance < 4096.0;
   }



   @Nullable
   public Player getPlayerOwner() {
      return null;
   }
}
