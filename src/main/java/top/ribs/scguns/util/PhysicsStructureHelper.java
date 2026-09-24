package top.ribs.scguns.util;

import dev.ryanhcode.sable.SableConfig;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.network.packets.tcp.ServerboundPunchSubLevelPacket;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;import top.ribs.scguns.Config;
import top.ribs.scguns.ScorchedGuns;

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
 */
public final class PhysicsStructureHelper {
    /**
     * Upper bound on how much speed one hit may add to a structure. The punch curve never comes
     * close to this; it is here so that no combination of multipliers, pellets or bouncing rounds
     * can fling a contraption out of the world.
     */
    private static final double MAX_SPEED_GAIN_PER_HIT = 2.0;

    /**
     * Upper bound on how much spin one hit may add, in radians per second.
     *
     * <p>Sable's punch applies its force at the <b>player's</b> position, and this mod now does the
     * same for the shooter, so the lever arm is the distance from the structure to whoever fired -
     * which for a long shot is a very long lever. Sable never had to think about that because a
     * punch only reaches about five blocks. The angular velocity this impulse would produce is
     * computed from Sable's own inertia tensor ({@code MassData.getInverseInertiaTensor}) and the
     * impulse is scaled down when it would exceed this, so a distant shot stays a shove.</p>
     */
    private static final double MAX_SPIN_GAIN_PER_HIT = 1.5;

    private PhysicsStructureHelper() {
    }

    /** The pose of the structure covering this block position, or null when there is none. */
    @Nullable
    private static Pose3dc poseAt(Level level, BlockPos pos) {
        if (!ScorchedGuns.physicsStructuresLoaded || level == null) {
            return null;
        }

        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null || !container.inBounds(pos)) {
            return null;
        }

        LevelPlot plot = container.getPlot(new ChunkPos(pos));
        if (plot == null) {
            return null;
        }

        SubLevel subLevel = plot.getSubLevel();
        return subLevel == null ? null : subLevel.logicalPose();
    }

    /**
     * Converts a point given in the frame of the block at {@code localPos} into world space,
     * or returns null when that block is not inside a physics structure.
     */
    @Nullable
    public static Vec3 toWorld(Level level, BlockPos localPos, Vec3 localPoint) {
        Pose3dc pose = poseAt(level, localPos);
        return pose == null ? null : pose.transformPosition(localPoint);
    }

    /**
     * The inverse of {@link #toWorld}: converts a world point into the frame of the block at
     * {@code localPos}, or returns null when that block is not inside a physics structure.
     */
    @Nullable
    public static Vec3 toLocal(Level level, BlockPos localPos, Vec3 worldPoint) {
        Pose3dc pose = poseAt(level, localPos);
        return pose == null ? null : pose.transformPositionInverse(worldPoint);
    }

    /**
     * Converts a direction (a normal, so translation must not apply) from the frame of the block
     * at {@code localPos} into world space, or null when that block is not inside a structure.
     */
    @Nullable
    public static Vec3 normalToWorld(Level level, BlockPos localPos, Vec3 localDirection) {
        Pose3dc pose = poseAt(level, localPos);
        return pose == null ? null : pose.transformNormal(localDirection);
    }

    /**
     * Pushes whatever physics structure a shot hit, using the same force Sable gives a player's
     * punch.
     *
     * <p><b>Why the punch force.</b> Sable's punch impulse is
     * {@code direction * punchCurve(normalMass) * multiplier} - a curve of the structure's mass
     * along the hit normal, which is what makes a punch feel right on a dinghy and on a galleon
     * alike. The first revision of this feature instead handed the raw damage-scaled momentum to
     * the body; on a real contraption that launched it hard enough to disappear. So the impulse
     * here is expressed in <b>punches</b>: {@code punchScale} is how many player punches the shot
     * is worth, and Sable's own {@code punchCurve} supplies the mass response.</p>
     *
     * <p><b>Where the force is applied.</b> Sable's punch does not push the block it hit: it asks
     * for the mass at the <b>player's own position</b> and applies the impulse <b>there</b>
     * ({@code localPosition = pose.transformPositionInverse(player.position())}, verified with
     * javap on {@code ServerboundPunchSubLevelPacket.handle}). This mod follows the same rule and
     * uses the shooter's position, so a shot pushes the structure the way a punch from where the
     * player stands would - which is the behaviour that was asked for. The hit point is still used
     * to find the structure.</p>
     *
     * <p>Three safety limits apply on top: one hit never adds more than
     * {@link #MAX_SPEED_GAIN_PER_HIT} m/s and more than {@link #MAX_SPIN_GAIN_PER_HIT} rad/s of
     * spin, and a structure is never pushed past the configured speed limit, so a burst of
     * automatic fire cannot accelerate anything out of the world. The spin limit matters here
     * because the shooter can be far away, and Sable's punch never had to deal with a lever arm
     * longer than a punch's reach. Structures with no mass (Sable's static levels) are left
     * alone.</p>
     *
     * <p>Sable moves a projectile that has entered a structure into that structure's own frame, so
     * both the impact point and the flight direction arrive here in the projectile's frame; they
     * are converted to world space here, and {@link #applyPunchAt} converts them back into the
     * structure's frame for Sable. When the shot did not involve a structure the values are already
     * world space and are used as they are.</p>
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
        if (!ScorchedGuns.physicsStructuresLoaded || level == null || !(punchScale > 0.0)
            || !isFinite(punchScale)) {
            return false;
        }

        Vec3 unit = flightDirection(hitPoint, forcePoint, direction);
        if (unit == null) {
            ScorchedGuns.LOGGER.debug("Physics structure impulse: no usable flight direction");
            return false;
        }
        BlockPos framePos = BlockPos.containing(hitPoint);

        Vec3 worldPoint = toWorld(level, framePos, hitPoint);
        Vec3 worldDirection = worldPoint == null ? null : normalToWorld(level, framePos, unit);
        if (worldPoint != null && worldDirection != null) {
            return applyPunchAt(level, worldPoint,
                forcePoint == null || !isFinite(forcePoint) ? worldPoint : forcePoint,
                worldDirection, punchScale);
        }

        return applyPunchAt(level, hitPoint, forcePoint == null ? hitPoint : forcePoint, unit, punchScale);
    }

    /**
     * Applies {@code punchScale} player-punches worth of impulse at {@code worldForcePoint}, in
     * world space, to the structure found at {@code worldHitPoint}. See
     * {@link #applyShotImpulse} for the reasoning and the limits.
     *
     * <p><b>Both points handed to Sable must be structure-local.</b> Its punch handler passes its
     * own {@code localPosition}/{@code localDirection} (built with
     * {@code transformPositionInverse}/{@code transformNormalInverse}) straight to
     * {@code PhysicsPipeline.applyImpulse}, so that is the frame the pipeline reads. Passing a
     * world point instead keeps the linear push small but makes the lever arm as long as the
     * distance from the structure's origin, and the spin that produces is limited only by the
     * body's own (small) inertia - a distant contraption was thrown thousands of blocks into the
     * sky. Local coordinates keep the lever arm meaningful, and the spin it induces is bounded by
     * {@link #MAX_SPIN_GAIN_PER_HIT}.</p>
     */
    public static boolean applyPunchAt(Level level, Vec3 worldHitPoint, Vec3 worldForcePoint,
                                       Vec3 worldDirection, double punchScale) {
        if (!ScorchedGuns.physicsStructuresLoaded || level == null || !(punchScale > 0.0)
            || !isFinite(worldHitPoint) || !isFinite(worldForcePoint) || !isFinite(worldDirection)) {
            return false;
        }

        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return false;
        }

        // Expand a point into a hair-thin world box so the container query can match it. The
        // structure is located from the HIT point: the shooter is usually outside it.
        BoundingBox3d probe = new BoundingBox3d(worldHitPoint, worldHitPoint).expand(0.05);

        for (SubLevel subLevel : container.queryIntersecting(probe)) {
            BoundingBox3dc bounds = subLevel.boundingBox();
            if (bounds != null && !bounds.contains(worldHitPoint.x, worldHitPoint.y, worldHitPoint.z)) {
                continue;
            }

            if (!(subLevel instanceof ServerSubLevel serverSubLevel)) {
                continue;
            }

            RigidBodyHandle handle = RigidBodyHandle.of(serverSubLevel);
            if (handle == null || !handle.isValid()) {
                continue;
            }

            MassData massData = serverSubLevel.getMassTracker();
            double mass = massData == null ? 0.0 : massData.getMass();
            if (!(mass > 0.0) || !isFinite(mass)) {
                continue;
            }

            Pose3dc pose = serverSubLevel.logicalPose();
            if (pose == null) {
                continue;
            }

            // The force point and the normal in the structure's own frame - the same two values
            // Sable's punch handler calls localPosition/localDirection. Sable applies the punch at
            // the player's own position, so this is where the shooter stands.
            Vector3d localPoint = pose.transformPositionInverse(
                new Vector3d(worldForcePoint.x, worldForcePoint.y, worldForcePoint.z));
            Vector3d localNormal = pose.transformNormalInverse(
                new Vector3d(worldDirection.x, worldDirection.y, worldDirection.z));

            // Sable's own mass response for a punch (its public punchCurve), with the same
            // punch-strength multiplier Sable applies, scaled to how many punches this shot is
            // worth. With punchScale = 1.0 this is literally one player punch.
            //
            // The mass must be asked for in the structure's frame too: Sable's
            // computeStrengthScalar is called with exactly these local values, and a world
            // point makes getInverseNormalMass return nonsense (measured: 3.4e11 instead of
            // 0.014) so the shot does nothing at all.
            double inverseNormalMass = massData.getInverseNormalMass(localPoint, localNormal);
            double normalMass = inverseNormalMass > 0.0 ? 1.0 / inverseNormalMass : 0.0;

            double punchStrength;
            try {
                punchStrength = SableConfig.SUB_LEVEL_PUNCH_STRENGTH_MULTIPLIER.getAsDouble();
            } catch (RuntimeException e) {
                // Sable's config can be read before it is loaded; fall back to its own default.
                punchStrength = 1.0;
            }

            // The punch handler also multiplies by the player's PUNCH_STRENGTH attribute, whose
            // registered default is 1.0, so one punch is exactly punchCurve(normalMass) here.
            double strength = normalMass > 0.0
                ? ServerboundPunchSubLevelPacket.punchCurve(normalMass) * punchStrength * punchScale
                : 0.0;
            if (!(strength > 0.0) || !isFinite(strength)) {
                continue;
            }

            double magnitude = Math.min(strength, mass * MAX_SPEED_GAIN_PER_HIT);

            Vector3dc velocity = handle.getLinearVelocity();
            double currentSpeed = velocity == null ? 0.0 : velocity.length();
            double maxSpeed = (Double)Config.COMMON.gameplay.physicsStructureMaxSpeed.get();
            magnitude = Math.min(magnitude, mass * Math.max(0.0, maxSpeed - currentSpeed));

            // Spin limit. The force is applied where the shooter stands, so the lever arm can be
            // far longer than a punch's reach; without this a long shot would spin a light
            // structure violently even though its linear push is capped. The induced angular
            // velocity is computed exactly, from Sable's inertia tensor, and the impulse is scaled
            // down until it fits - so in normal (punch-range) cases this changes nothing at all.
            Vector3d lever = new Vector3d(localPoint).sub(massData.getCenterOfMass());
            Vector3d unitImpulse = new Vector3d(localNormal).normalize();
            double maxSpin = (Double)Config.COMMON.gameplay.physicsStructureMaxSpin.get();
            double allowedSpin = Math.max(0.0, Math.min(MAX_SPIN_GAIN_PER_HIT, maxSpin - currentSpin(handle)));
            double spinPerImpulse = spinPerImpulse(massData, lever, unitImpulse);
            if (spinPerImpulse > 0.0) {
                magnitude = Math.min(magnitude, allowedSpin / spinPerImpulse);
            }

            if (!(magnitude > 0.0)) {
                return false;
            }

            // The force is applied at the shooter's position, in the structure's own frame, with
            // the direction converted the same way - exactly as Sable's punch handler does. The
            // spin this can produce is bounded by the check above.
            handle.applyImpulseAtPoint(localPoint, unitImpulse.mul(magnitude));
            return true;
        }

        return false;
    }

    /** The structure's current spin, in radians per second. */
    private static double currentSpin(RigidBodyHandle handle) {
        Vector3dc angular = handle.getAngularVelocity();
        return angular == null ? 0.0 : angular.length();
    }

    /**
     * The direction the shot was travelling, or null when nothing usable can be worked out.
     *
     * <p>The projectile's own motion is the first choice, but it cannot be trusted on its own:
     * Sable moves a projectile that enters a structure into that structure's frame, and when it
     * does the motion can come out as (almost) zero. A shot that lost its motion was then rejected
     * outright - which is what "shooting the underside does nothing" was, because the motion is
     * spent exactly where the projectile crossed the boundary.</p>
     *
     * <p>So when the motion is degenerate the direction is taken from the shooter instead: the hit
     * point minus the force point is the straight line the shot travelled along, and it is also
     * the direction a punch from that position would push. That keeps the force model consistent
     * (Sable's punch pushes along the player's own look direction, not along a surface normal).</p>
     */
    @Nullable
    private static Vec3 flightDirection(Vec3 hitPoint, @Nullable Vec3 forcePoint, Vec3 direction) {
        if (isFinite(direction) && direction.lengthSqr() > 1.0E-8) {
            return direction.normalize();
        }

        if (forcePoint != null && isFinite(forcePoint)) {
            Vec3 fromShooter = hitPoint.subtract(forcePoint);
            if (isFinite(fromShooter) && fromShooter.lengthSqr() > 1.0E-8) {
                return fromShooter.normalize();
            }
        }

        return null;
    }

    /**
     * How much angular velocity one unit of impulse would add, from Sable's own inertia tensor:
     * {@code |I^-1 (r x p)|}. Zero when Sable cannot tell us the inertia, in which case no spin
     * limit is applied rather than a guessed one.
     */
    private static double spinPerImpulse(MassData massData, Vector3d lever, Vector3d unitImpulse) {
        Matrix3dc inverseInertia = massData.getInverseInertiaTensor();
        if (inverseInertia == null) {
            return 0.0;
        }

        Vector3d angularImpulse = lever.cross(unitImpulse, new Vector3d());
        Vector3d angularVelocity = inverseInertia.transform(angularImpulse, new Vector3d());
        double spin = angularVelocity.length();
        return isFinite(spin) ? spin : 0.0;
    }

    private static boolean isFinite(Vec3 vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    private static boolean isFinite(double value) {
        return Double.isFinite(value);
    }
}
