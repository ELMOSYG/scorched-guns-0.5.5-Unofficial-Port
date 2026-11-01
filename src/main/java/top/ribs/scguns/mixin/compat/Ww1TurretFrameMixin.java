package top.ribs.scguns.mixin.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.util.PhysicsStructureHelper;

/**
 * Puts a player-manned WW1 turret's bullets into world space when the turret stands on a Sable physics structure.
 *
 * <p>That turret builds its muzzle from its own {@code getBlockPos()}, and its aim from the seated player's eye and
 * look vector. On a sub-level the first of those is a plot coordinate - in the tens of millions - while the second is
 * an ordinary world coordinate, so the direction it derives is meaningless and the gun shoots several blocks off the
 * crosshair. The turret's own maths is fine on normal ground; only the frame is wrong.</p>
 *
 * <p>The fix is applied where the bullet is added to the level: its position and motion are converted once, out of
 * the structure's frame. Nothing else is touched, and without Sable installed the call is passed straight through.</p>
 *
 * <p>The target is another mod's class and its internals may change between versions, so the mixin is registered as
 * optional - if it cannot apply, the turret keeps its old behaviour instead of taking the game down.</p>
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
   private boolean scguns$spawnBulletInWorldSpace(Level level, Entity bullet, ServerPlayer gunner, BlockPos turretPos) {
      if (ScorchedGuns.physicsStructuresLoaded && bullet != null) {
         try {
            Vec3 position = PhysicsStructureHelper.toWorld(level, turretPos, bullet.position());
            if (position != null) {
               bullet.setPos(position.x, position.y, position.z);
            }

            Vec3 motion = PhysicsStructureHelper.normalToWorld(level, turretPos, bullet.getDeltaMovement());
            if (motion != null) {
               bullet.setDeltaMovement(motion);
            }
         } catch (RuntimeException e) {
            // A physics-structure quirk must never stop a turret from firing.
            ScorchedGuns.LOGGER.debug("WW1 turret frame conversion failed", e);
         }
      }

      return level.addFreshEntity(bullet);
   }
}
