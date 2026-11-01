package top.ribs.scguns.mixin.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.util.PhysicsStructureHelper;

/**
 * Puts a player-manned WW1 turret's bullets into world space when the turret stands on a Sable physics structure.
 *
 * <p>That turret builds its muzzle from its own getBlockPos(), and its aim from the seated player's eye and look
 * vector. On a sub-level the first of those is a plot coordinate - in the tens of millions - while the second is an
 * ordinary world coordinate, so the direction it derives is meaningless and the gun shoots several blocks off the
 * crosshair. The turret's own maths is fine on normal ground; only the frame is wrong.</p>
 *
 * <p>The fix is applied where the bullet is added to the level: its position and motion are converted once, out of the
 * structure's frame. Nothing else is touched, and without Sable installed the call is passed straight through.</p>
 *
 * <p>The handler names no types from the other mod - the entity arrives as Object and is narrowed here, and the
 * turret's position is read through the vanilla BlockEntity interface. The injection is optional as well, so a future
 * version of that turret simply keeps its old behaviour instead of crashing.</p>
 */
@Mixin(targets = "net.roka.scgunsww1.block.turret.TurretBlockEntity")
public abstract class Ww1TurretFrameMixin {

   @Redirect(
      method = "fire",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"),
      require = 0
   )
   private boolean scguns$spawnBulletInWorldSpace(Level level, Entity bullet) {
      BlockEntity self = (BlockEntity)(Object)this;

      if (bullet != null && ScorchedGuns.physicsStructuresLoaded) {
         try {
            Vec3 position = PhysicsStructureHelper.toWorld(level, self.getBlockPos(), bullet.position());
            if (position != null) {
               bullet.setPos(position.x, position.y, position.z);
            }

            Vec3 motion = PhysicsStructureHelper.normalToWorld(level, self.getBlockPos(), bullet.getDeltaMovement());
            if (motion != null) {
               bullet.setDeltaMovement(motion);
            }
         } catch (RuntimeException e) {
            ScorchedGuns.LOGGER.debug("WW1 turret frame conversion failed", e);
         }
      }

      return level.addFreshEntity(bullet);
   }
}
