package top.ribs.scguns.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.compat.SablePhysicsBridge;

/**
 * Frame conversion for blocks that physics mods have moved into a structure.
 *
 * <p>Sable (and Valkyrien Skies before it) does not add blocks to the main level: it moves
 * them into a "sub-level" and renders/physics them through a pose. A block entity inside one
 * therefore reports <b>plot coordinates</b> from {@code getBlockPos()}/{@code worldPosition},
 * while entities still report <b>world coordinates</b>. Any code that subtracts one from the
 * other is mixing two frames; for the turrets that produced a nearly constant aim, which is
 * why a turret mounted on a contraption kept firing in one direction.</p>
 *
 * <p>Both helpers return {@code null} when the position is not inside a structure (or no such
 * mod is installed), so callers can simply fall back to using the coordinates as they are.</p>
 *
 * <p><b>This class must not name a Sable type, not even in a local variable or a return
 * type.</b> It is called unconditionally - a turret ticks in any save that has one - and the
 * JVM verifies a whole class, not one method, when the class is linked. The first version of
 * this file held the Sable calls itself behind {@code ScorchedGuns.physicsStructuresLoaded}
 * checks, and that was not enough: verification resolves the types the bytecode checks
 * assignability between, so the very first call to any method here died with
 * {@code NoClassDefFoundError: dev/ryanhcode/sable/companion/math/Pose3dc} on a machine
 * without Sable, before any guard could run. That is what made entering a save crash.
 * The calls now live in {@link SablePhysicsBridge}, reached only through {@link Sable} below,
 * which is a separate class and is therefore verified separately - and only once the guard has
 * already said yes.</p>
 */
public final class PhysicsStructureHelper {
    private PhysicsStructureHelper() {
    }

    /**
     * Converts a point given in the frame of the block at {@code localPos} into world space,
     * or returns null when that block is not inside a physics structure.
     */
    @Nullable
    public static Vec3 toWorld(Level level, BlockPos localPos, Vec3 localPoint) {
        if (!ScorchedGuns.physicsStructuresLoaded || level == null) {
            return null;
        }

        return Sable.BRIDGE.toWorld(level, localPos, localPoint);
    }

    /**
     * The inverse of {@link #toWorld}: converts a world point into the frame of the block at
     * {@code localPos}, or returns null when that block is not inside a physics structure.
     */
    @Nullable
    public static Vec3 toLocal(Level level, BlockPos localPos, Vec3 worldPoint) {
        if (!ScorchedGuns.physicsStructuresLoaded || level == null) {
            return null;
        }

        return Sable.BRIDGE.toLocal(level, localPos, worldPoint);
    }

    /**
     * Converts a direction (a normal, so translation must not apply) from the frame of the block
     * at {@code localPos} into world space, or null when that block is not inside a structure.
     */
    @Nullable
    public static Vec3 normalToWorld(Level level, BlockPos localPos, Vec3 localDirection) {
        if (!ScorchedGuns.physicsStructuresLoaded || level == null) {
            return null;
        }

        return Sable.BRIDGE.normalToWorld(level, localPos, localDirection);
    }

    /**
     * Pushes whatever physics structure a shot hit, using the same force Sable gives a player's
     * punch. See {@link SablePhysicsBridge#applyShotImpulse} for the force model, where the
     * force is applied, and the three safety limits.
     *
     * @param hitPoint impact point in the projectile's frame
     * @param forcePoint where the force is applied, in world space - the shooter's position; when
     *                   null (no shooter, e.g. a turret or a dispenser) the impact point is used
     * @param direction flight direction in the projectile's frame
     * @param punchScale how many player punches this shot is worth (0 or less does nothing)
     * @return true when a structure was found and moved
     */
    public static boolean applyShotImpulse(Level level, Vec3 hitPoint, @Nullable Vec3 forcePoint,
                                           Vec3 direction, double punchScale) {
        if (!ScorchedGuns.physicsStructuresLoaded || level == null || !(punchScale > 0.0)) {
            return false;
        }

        return Sable.BRIDGE.applyShotImpulse(level, hitPoint, forcePoint, direction, punchScale);
    }

    /**
     * Applies {@code punchScale} player-punches worth of impulse at {@code worldForcePoint}, in
     * world space, to the structure found at {@code worldHitPoint}. See
     * {@link SablePhysicsBridge#applyPunchAt}.
     */
    public static boolean applyPunchAt(Level level, Vec3 worldHitPoint, Vec3 worldForcePoint,
                                       Vec3 worldDirection, double punchScale) {
        if (!ScorchedGuns.physicsStructuresLoaded || level == null || !(punchScale > 0.0)) {
            return false;
        }

        return Sable.BRIDGE.applyPunchAt(level, worldHitPoint, worldForcePoint, worldDirection, punchScale);
    }

    /**
     * The one door onto the Sable API.
     *
     * <p>A nested class is a class of its own, so it is verified on its own schedule: touching
     * {@code PhysicsStructureHelper} does not link it, and the only thing that does is the field
     * read here - which sits behind the guard in every method above. Confirmed with a standalone
     * JVM reproduction of both shapes, because this is the kind of thing that is very easy to
     * "simplify" back into a direct call and very hard to notice afterwards.</p>
     */
    private static final class Sable {
        static final SablePhysicsBridge BRIDGE = SablePhysicsBridge.INSTANCE;

        private Sable() {
        }
    }
}
