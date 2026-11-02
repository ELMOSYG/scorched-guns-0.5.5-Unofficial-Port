package top.ribs.scguns.entity.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The trauma unit's grapnel: a projectile it throws at whatever it is fighting, which pulls the target towards the
 * unit for a moment and then comes home.
 *
 * <p>This used to extend {@code FishingHook}. That class assumes its owner is a player and enforces it in three
 * separate places - the spawn packet only carries a player owner, {@code recreateFromPacket} rejects anything else on
 * the client, and {@code tick} is written around a player reeling the line in. Every one of those had to be worked
 * around separately, and the workarounds fought each other: the hook could catch its own thrower, the client dropped
 * the entity outright, and a caught target was never released. A plain {@link Projectile} carries its owner through
 * the spawn packet by itself and lets the whole flight, pull and return be written once, here.</p>
 */
public class TraumaHookEntity extends Projectile {
   private static final double GRAVITY = 0.03;
   private static final double AIR_DRAG = 0.98;
   private static final double GROUND_DRAG = 0.8;
   private static final int MAX_LIFETIME = 100;
   private static final int PULL_TICKS = 20;
   private static final double PULL_ACCEL = 0.45;
   private static final double MAX_RANGE = 40.0;

   private boolean retracting;
   private int life;
   @Nullable
   private Entity caught;
   private int caughtAt = -1;

   public TraumaHookEntity(EntityType<? extends TraumaHookEntity> entityType, Level level) {
      super(entityType, level);
      this.noCulling = true;
   }

   public TraumaHookEntity(EntityType<? extends TraumaHookEntity> entityType, LivingEntity owner, Level level) {
      this(entityType, level);
      this.setOwner(owner);
      this.moveTo(owner.getX(), owner.getEyeY() - 0.1, owner.getZ(), owner.getYRot(), owner.getXRot());
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      this.life = tag.getInt("Life");
      this.retracting = tag.getBoolean("Retracting");
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putInt("Life", this.life);
      tag.putBoolean("Retracting", this.retracting);
   }

   /** The entity that threw this hook, on either side. */
   @Nullable
   public Entity getThrower() {
      return this.getOwner();
   }

   /** What the hook is currently dragging, if anything. */
   @Nullable
   public Entity getHookedIn() {
      return this.caught;
   }

   public boolean isRetracting() {
      return this.retracting;
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double distance) {
      return distance < 4096.0;
   }

   @Override
   public void tick() {
      this.baseTick();

      // Counted on both sides: a client copy that never expires would keep drawing its line forever.
      ++this.life;
      if (this.life > MAX_LIFETIME) {
         this.discard();
         return;
      }

      if (this.level().isClientSide()) {
         // No local prediction. The server stops the hook on what it catches and later reels it in; a client that
         // integrated its own gravity never saw either, so the line slid through the target and hung in the world.
         return;
      }

      Entity owner = this.getOwner();
      if (owner == null || owner.isRemoved() || !owner.isAlive()) {
         this.discard();
         return;
      }

      if (this.caught != null) {
         this.dragCaught(owner);
      }

      if (this.retracting) {
         this.flyHome(owner);
      } else if (this.caught == null) {
         this.fly();
      }

      this.updateRotation();
   }

   private void fly() {
      HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
      if (hitResult.getType() != HitResult.Type.MISS) {
         this.onHit(hitResult);
         return;
      }

      Vec3 motion = this.getDeltaMovement();
      if (!this.onGround()) {
         motion = motion.add(0.0, -GRAVITY, 0.0).scale(AIR_DRAG);
      } else {
         motion = motion.multiply(GROUND_DRAG, 0.0, GROUND_DRAG);
      }

      this.setDeltaMovement(motion);
      this.move(MoverType.SELF, motion);

      Entity owner = this.getOwner();
      if (owner != null && this.distanceTo(owner) > MAX_RANGE) {
         this.startRetraction();
      } else if (this.onGround() && this.tickCount > 20) {
         this.startRetraction();
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      Entity target = result.getEntity();
      if (!(target instanceof LivingEntity) || target == this.getOwner() || this.retracting) {
         return;
      }

      this.caught = target;
      this.caughtAt = this.tickCount;
      this.setDeltaMovement(Vec3.ZERO);
   }

   @Override
   protected void onHitBlock(BlockHitResult result) {
      this.setDeltaMovement(Vec3.ZERO);
      Vec3 location = result.getLocation();
      this.setPos(location.x, location.y, location.z);
   }

   private void dragCaught(Entity owner) {
      if (!(this.caught instanceof LivingEntity living) || !living.isAlive() || living.isRemoved()) {
         this.caught = null;
         this.startRetraction();
         return;
      }

      Vec3 anchor = owner.position().add(0.0, (double)owner.getEyeHeight() * 0.6, 0.0);
      Vec3 delta = anchor.subtract(living.position());
      if (delta.lengthSqr() > 1.0E-4) {
         Vec3 pull = delta.normalize().scale(PULL_ACCEL);
         living.setDeltaMovement(living.getDeltaMovement().add(pull.x, 0.06, pull.z));
         living.hurtMarked = true;
      }

      this.setPos(living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ());

      if (this.caughtAt < 0) {
         this.caughtAt = this.tickCount;
      } else if (this.tickCount - this.caughtAt > PULL_TICKS) {
         this.caught = null;
         this.startRetraction();
      }
   }

   private void startRetraction() {
      this.retracting = true;
      this.caught = null;
   }

   private void flyHome(Entity owner) {
      Vec3 ownerPos = new Vec3(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
      Vec3 hookPos = this.position();
      double distance = hookPos.distanceTo(ownerPos);
      if (distance < 1.0) {
         this.discard();
         return;
      }

      Vec3 direction = ownerPos.subtract(hookPos).normalize();
      double speed = Math.min(0.4 + distance * 0.05, 1.5);
      Vec3 velocity = direction.scale(speed);
      Vec3 next = hookPos.add(velocity);

      // setPos rather than move(): the hook usually starts its trip home from inside the creature it just grabbed,
      // where a collision-resolved move is refused and the hook could never leave.
      this.setPos(next.x, next.y, next.z);
      this.setDeltaMovement(velocity);
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      return !this.retracting && entity instanceof LivingEntity && entity != this.getOwner();
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

   @Override
   public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
      Vec3 direction = new Vec3(x, y, z).normalize();
      direction = direction.add(
         this.random.triangle(0.0, 0.0172275 * (double)inaccuracy),
         this.random.triangle(0.0, 0.0172275 * (double)inaccuracy),
         this.random.triangle(0.0, 0.0172275 * (double)inaccuracy)
      ).normalize().scale((double)velocity);

      this.setDeltaMovement(direction);
      this.setYRot((float)(Math.atan2(direction.x, direction.z) * 180.0 / Math.PI));
      this.setXRot((float)(Math.atan2(direction.y, direction.horizontalDistance()) * 180.0 / Math.PI));
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
   }
}
