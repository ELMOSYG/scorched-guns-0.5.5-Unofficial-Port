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

public class TraumaHookEntity extends FishingHook implements net.neoforged.neoforge.entity.IEntityWithComplexSpawn {
   private boolean isRetracting = false;

   /** Tick the hook caught something on, so the pull can be released after a moment. */
   private int hookedAt = -1;

   /**
    * The entity that threw this hook.
    *
    * <p>FishingHook resolves its owner through {@link #getPlayerOwner()}, and this hook has a mob owner rather than a
    * player, so that override returns null - which made {@link #getOwner()} null too. Every check written as
    * {@code entity != this.getOwner()} then let the thrower itself be hooked, and since the hook is launched half a
    * block in front of the mob it usually was. The owner is therefore tracked here instead of being asked for.</p>
    */
   @Nullable
   private Entity hookOwner;

   @Nullable
   private java.util.UUID hookOwnerId;
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
      this.hookOwner = owner;
      this.hookOwnerId = owner.getUUID();
      this.noCulling = true;
      this.moveTo(owner.getX(), owner.getEyeY() - 0.1, owner.getZ(), owner.getYRot(), owner.getXRot());
   }

   /**
    * Builds this hook on the client from the spawn packet.
    *
    * <p>Vanilla {@code FishingHook} refuses any owner that is not a player, logs "is not a valid owner" and drops the
    * entity - which left the client without a hook at all, so there was no bobber and nothing for the line to reach.
    * A mob throws this one, so the packet is handled here instead: the position, velocity and owner are applied
    * without the player check.</p>
    */
   /**
    * Sends the thrower alongside the spawn packet.
    *
    * <p>Vanilla {@code FishingHook} only puts a <em>player</em> owner into that packet, and this hook's owner is a mob,
    * so the client received id 0 and had no way to know who threw it - the line had nothing to attach to. The 1.20.1
    * original solved this with {@code NetworkHooks.getEntitySpawningPacket(this)}; on NeoForge the equivalent is this
    * interface, which appends the id to the spawn data.</p>
    */
   @Override
   public void writeSpawnData(net.minecraft.network.RegistryFriendlyByteBuf buffer) {
      Entity owner = this.hookOwner();
      buffer.writeVarInt(owner == null ? 0 : owner.getId());
   }

   @Override
   public void readSpawnData(net.minecraft.network.RegistryFriendlyByteBuf buffer) {
      int ownerId = buffer.readVarInt();
      if (ownerId > 0) {
         Entity owner = this.level().getEntity(ownerId);
         if (owner != null) {
            this.setOwner(owner);
         }
      }
   }

   @Override
   public void recreateFromPacket(net.minecraft.network.protocol.game.ClientboundAddEntityPacket packet) {
      this.setPosRaw(packet.getX(), packet.getY(), packet.getZ());
      this.setDeltaMovement(packet.getXa(), packet.getYa(), packet.getZa());
      this.setYRot((float)packet.getYRot());
      this.setXRot((float)packet.getXRot());
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();

      Entity thrower = this.level().getEntity(packet.getData());
      if (thrower != null) {
         this.setOwner(thrower);
      }
   }

   public void setOwner(@Nullable Entity owner) {
      super.setOwner(owner);
      if (owner != null) {
         this.hookOwner = owner;
         this.hookOwnerId = owner.getUUID();
      }
   }

   /** The thrower, preferring the tracked reference over {@code getOwner()}, which can be null here. */
   @Nullable
   private Entity hookOwner() {
      if (this.hookOwner != null && !this.hookOwner.isRemoved()) {
         return this.hookOwner;
      }

      return this.getOwner();
   }

   /** The entity that threw this hook. Never null while the thrower is loaded, unlike {@code getOwner()}. */
   @Nullable
   public Entity getThrower() {
      return this.hookOwner();
   }

   private boolean isThrower(Entity entity) {
      if (this.hookOwnerId != null && entity.getUUID().equals(this.hookOwnerId)) {
         return true;
      }

      return entity == this.hookOwner();
   }

   /**
    * A hit pulls the target towards the thrower and sends the hook home.
    *
    * <p>Vanilla {@code FishingHook.onHitEntity} stores the target in {@code hookedIn} and lets {@code FishingHook.tick}
    * do the pulling - but this class replaces that tick, so nothing ever released the hook again and it sat on the
    * creature until the lifetime ran out. The pull is done here instead and the hook starts retracting at once, so no
    * hooked state is left behind.</p>
    */
   protected void onHitEntity(EntityHitResult result) {
      Entity entity = result.getEntity();
      if (entity instanceof LivingEntity && !this.isThrower(entity) && !this.isRetracting) {
         // Vanilla's own handling: it stores the target and pulls it with the strength a fishing rod has. The
         // release is handled in tick(), because this class replaces FishingHook.tick() entirely.
         super.onHitEntity(result);
         this.setDeltaMovement(Vec3.ZERO);
         this.hookedAt = this.tickCount;
      }
   }

   protected boolean canHitEntity(Entity entity) {
      return !this.isRetracting && entity instanceof LivingEntity && !this.isThrower(entity);
   }

   public void tick() {
      this.baseTick();
      if (this.level().isClientSide()) {
         // No local prediction on purpose. The server stops the hook on the creature it caught and later pulls it
         // home; a client that integrates its own gravity never sees either, so the drawn line slid straight through
         // the creature and the hook hung in the world until it was unloaded. The client now only follows the
         // positions the server sends.
         this.xRotO = this.getXRot();
         this.yRotO = this.getYRot();
      } else {
         Entity owner = this.hookOwner();
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

            Entity caught = this.getHookedIn();
            if (caught != null && !this.isRetracting) {
               // Pull the target toward the thrower, then let go: about a second of dragging is enough.
               Vec3 anchor = owner.position().add(0.0, (double)owner.getEyeHeight() * 0.6, 0.0);
               Vec3 toAnchor = anchor.subtract(caught.position());

               if (toAnchor.lengthSqr() > 1.0E-4) {
                  Vec3 pull = toAnchor.normalize().scale(0.35);
                  Vec3 moved = caught.getDeltaMovement().add(pull.x, 0.06, pull.z);
                  caught.setDeltaMovement(moved);
                  caught.hurtMarked = true;
               }

               this.setPos(caught.getX(), caught.getY() + caught.getBbHeight() * 0.5, caught.getZ());

               if (this.hookedAt < 0) {
                  this.hookedAt = this.tickCount;
               } else if (this.tickCount - this.hookedAt > 20) {
                  this.startRetraction();
               }
            }

            if (this.isRetracting) {
               this.handleRetraction();
            } else if (caught == null) {
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
      Entity owner = this.hookOwner();
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

            // setPos rather than move(): the hook is often sitting inside the creature it just hit, and a collision
            // resolved move is refused there, so the hook could never travel home and never despawned.
            Vec3 next = hookPos.add(retractVelocity);
            this.setPos(next.x, next.y, next.z);
            this.setDeltaMovement(retractVelocity);
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
