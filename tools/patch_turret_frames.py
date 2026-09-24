#!/usr/bin/env python3
"""Patch both turret block entities so their aiming works inside Sable sub-levels.

Inside a physics structure a block entity's position is in that structure's frame while
entities report world coordinates. The turrets compared the two directly, so the yaw was
computed from a target position offset by the whole structure displacement - the aim
barely changed, i.e. a turret on a contraption only ever fired one way.

Two changes per class:
  * store the smoothed aim point in the turret's own frame (one place, so every later
    yaw/pitch/distance calculation stays in a single frame);
  * convert the muzzle from the turret's frame into world space before spawning the
    projectile / playing the shot sound.

Usage:
    python tools/patch_turret_frames.py [--apply]
"""
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TURRET = os.path.join(ROOT, 'src', 'main', 'java', 'top', 'ribs', 'scguns', 'blockentity', 'TurretBlockEntity.java')
ENEMY = os.path.join(ROOT, 'src', 'main', 'java', 'top', 'ribs', 'scguns', 'blockentity', 'EnemyTurretBlockEntity.java')

IMPORT_ANCHOR = 'import top.ribs.scguns.util.NbtHelper;\n'
IMPORT_LINE = 'import top.ribs.scguns.util.PhysicsStructureHelper;\n'

# ---- TurretBlockEntity -------------------------------------------------------
T_IMPORT = (IMPORT_ANCHOR, IMPORT_LINE + IMPORT_ANCHOR)

T_TURRET_POS = (
    """            Vec3 turretPos = new Vec3((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 1.0, (double)this.worldPosition.getZ() + 0.5);
""",
    """            Vec3 turretPos = new Vec3((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 1.0, (double)this.worldPosition.getZ() + 0.5);
            // Line-of-sight raycasts happen in world space, while the distance comparisons
            // below stay in this turret's own frame (the search box is built from blockPos).
            Vec3 convertedTurretPos = PhysicsStructureHelper.toWorld(level, this.worldPosition, turretPos);
            final Vec3 turretPosWorld = convertedTurretPos != null ? convertedTurretPos : turretPos;
""",
)

T_LOS = (
    """                     .filter(entity -> this.hasLineOfSight(level, turretPos, entity))""",
    """                     .filter(entity -> this.hasLineOfSight(level, turretPosWorld, entity))""",
)

T_SAVE_AIM = (
    """                  double predictedZ = this.target.getZ() + this.target.getDeltaMovement().z * (double)predMult;
                  float smoothing = this.config.getTargeting().getPositionSmoothing();""",
    """                  double predictedZ = this.target.getZ() + this.target.getDeltaMovement().z * (double)predMult;
                  // Entities report world coordinates; inside a physics structure this turret's
                  // block position is in that structure's own frame. Store the aim point in the
                  // turret's frame so every later yaw/pitch/distance calculation stays in one
                  // frame - mixing the two is what pinned a contraption-mounted turret's aim.
                  Vec3 predictedLocal = PhysicsStructureHelper.toLocal(level, this.worldPosition, new Vec3(predictedX, predictedY, predictedZ));
                  if (predictedLocal != null) {
                     predictedX = predictedLocal.x;
                     predictedY = predictedLocal.y;
                     predictedZ = predictedLocal.z;
                  }

                  float smoothing = this.config.getTargeting().getPositionSmoothing();""",
)

T_MUZZLE = (
    """         Vec3 muzzlePos = this.getMuzzlePosition(yaw, pitch);
""",
    """         // getMuzzlePosition works in this turret's frame; the projectile, its flash and its
         // sound all live in the world, so convert once here.
         Vec3 muzzleLocal = this.getMuzzlePosition(yaw, pitch);
         Vec3 convertedMuzzle = PhysicsStructureHelper.toWorld(this.level, this.worldPosition, muzzleLocal);
         Vec3 muzzlePos = convertedMuzzle != null ? convertedMuzzle : muzzleLocal;
""",
)

T_SOUND = (
    """            this.level.playSound(null, this.worldPosition, fireSound, SoundSource.BLOCKS, 0.7F, 0.7F);""",
    """            this.level.playSound(null, muzzlePos.x, muzzlePos.y, muzzlePos.z, fireSound, SoundSource.BLOCKS, 0.7F, 0.7F);""",
)

# ---- EnemyTurretBlockEntity --------------------------------------------------
E_IMPORT = (
    'import top.ribs.scguns.init.ModBlockEntities;\n',
    'import top.ribs.scguns.init.ModBlockEntities;\n'
    'import top.ribs.scguns.util.PhysicsStructureHelper;\n',
)

E_TURRET_POS = (
    """      Vec3 turretPos = new Vec3((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 1.0, (double)this.worldPosition.getZ() + 0.5);
""",
    """      Vec3 turretPos = new Vec3((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 1.0, (double)this.worldPosition.getZ() + 0.5);
      // Line-of-sight raycasts happen in world space, while the distance comparisons below
      // stay in this turret's own frame.
      Vec3 convertedTurretPos = PhysicsStructureHelper.toWorld(level, this.worldPosition, turretPos);
      final Vec3 turretPosWorld = convertedTurretPos != null ? convertedTurretPos : turretPos;
""",
)

E_LOS = (
    """               && this.hasLineOfSight(level, turretPos, player)""",
    """               && this.hasLineOfSight(level, turretPosWorld, player)""",
)

E_SAVE_AIM = (
    """         double predictedZ = this.target.getZ() + this.target.getDeltaMovement().z * 7.0;
         this.smoothedTargetX = lerp(this.smoothedTargetX, predictedX, 0.2F);""",
    """         double predictedZ = this.target.getZ() + this.target.getDeltaMovement().z * 7.0;
         // Keep the aim point in this turret's own frame (see TurretBlockEntity).
         Vec3 predictedLocal = PhysicsStructureHelper.toLocal(this.level, this.worldPosition, new Vec3(predictedX, predictedY, predictedZ));
         if (predictedLocal != null) {
            predictedX = predictedLocal.x;
            predictedY = predictedLocal.y;
            predictedZ = predictedLocal.z;
         }

         this.smoothedTargetX = lerp(this.smoothedTargetX, predictedX, 0.2F);""",
)

E_MUZZLE = (
    """         Vec3 muzzlePos = this.getMuzzlePosition(this.yaw, this.pitch);""",
    """         Vec3 muzzleLocal = this.getMuzzlePosition(this.yaw, this.pitch);
         Vec3 convertedMuzzle = PhysicsStructureHelper.toWorld(this.level, this.worldPosition, muzzleLocal);
         Vec3 muzzlePos = convertedMuzzle != null ? convertedMuzzle : muzzleLocal;""",
)

E_SOUND = (
    """         this.level.playSound(null, this.worldPosition, (SoundEvent)ModSounds.IRON_RIFLE_FIRE.get(), SoundSource.BLOCKS, 0.7F, 0.7F);""",
    """         this.level.playSound(null, muzzlePos.x, muzzlePos.y, muzzlePos.z, (SoundEvent)ModSounds.IRON_RIFLE_FIRE.get(), SoundSource.BLOCKS, 0.7F, 0.7F);""",
)

# Disable-effect particles/sounds are emitted from the server, so they need world
# coordinates too (otherwise they appear wherever the plot happens to sit).
T_EFFECT = (
    """      if (this.level instanceof ServerLevel serverLevel) {
         double x = (double)this.worldPosition.getX() + 0.5;
         double y = (double)this.worldPosition.getY() + 1.0;
         double z = (double)this.worldPosition.getZ() + 0.5;

         for (int i = 0; i < 20; i++) {""",
    """      if (this.level instanceof ServerLevel serverLevel) {
         Vec3 centre = PhysicsStructureHelper.toWorld(this.level, this.worldPosition,
               new Vec3((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 1.0, (double)this.worldPosition.getZ() + 0.5));
         if (centre == null) {
            centre = new Vec3((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 1.0, (double)this.worldPosition.getZ() + 0.5);
         }

         double x = centre.x;
         double y = centre.y;
         double z = centre.z;

         for (int i = 0; i < 20; i++) {""",
)

T_EFFECT_SOUND = (
    """         serverLevel.playSound(null, this.worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);""",
    """         serverLevel.playSound(null, x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);""",
)

E_EFFECT = (
    """      if (this.level instanceof ServerLevel serverLevel) {
         double x = (double)this.worldPosition.getX() + 0.5;
         double y = (double)this.worldPosition.getY() + 1.0;
         double z = (double)this.worldPosition.getZ() + 0.5;
         int particleCount = 20;""",
    """      if (this.level instanceof ServerLevel serverLevel) {
         Vec3 centre = PhysicsStructureHelper.toWorld(this.level, this.worldPosition,
               new Vec3((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 1.0, (double)this.worldPosition.getZ() + 0.5));
         if (centre == null) {
            centre = new Vec3((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 1.0, (double)this.worldPosition.getZ() + 0.5);
         }

         double x = centre.x;
         double y = centre.y;
         double z = centre.z;
         int particleCount = 20;""",
)

E_EFFECT_SOUND = (
    """         serverLevel.playSound(null, this.worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);""",
    """         serverLevel.playSound(null, x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);""",
)

# The muzzle flash must be tracked from the chunk the shot actually happens in; a turret
# inside a structure sits in a plot chunk, which no player tracks.
T_TRACKING = (
    """            PacketHandler.getPlayChannel().sendToTrackingChunk(() -> this.level.getChunkAt(this.worldPosition), new S2CMessageMuzzleFlash(muzzlePos, yaw, pitch));""",
    """            PacketHandler.getPlayChannel()
               .sendToTrackingChunk(() -> this.level.getChunkAt(BlockPos.containing(muzzlePos)), new S2CMessageMuzzleFlash(muzzlePos, yaw, pitch));""",
)

E_TRACKING = (
    """            .sendToTrackingChunk(() -> this.level.getChunkAt(this.worldPosition), new S2CMessageMuzzleFlash(muzzlePos, this.yaw, this.pitch));""",
    """            .sendToTrackingChunk(() -> this.level.getChunkAt(BlockPos.containing(muzzlePos)), new S2CMessageMuzzleFlash(muzzlePos, this.yaw, this.pitch));""",
)

PATCHES = [
    (TURRET, 'import', T_IMPORT),
    (TURRET, 'turretPos (world copy for LOS)', T_TURRET_POS),
    (TURRET, 'LOS uses the world position', T_LOS),
    (TURRET, 'aim point stored in the local frame', T_SAVE_AIM),
    (TURRET, 'muzzle converted to world', T_MUZZLE),
    (TURRET, 'shot sound at the world muzzle', T_SOUND),
    (TURRET, 'disable particles in world space', T_EFFECT),
    (TURRET, 'disable sound at the world centre', T_EFFECT_SOUND),
    (TURRET, 'muzzle flash tracked from the world chunk', T_TRACKING),
    (ENEMY, 'import', E_IMPORT),
    (ENEMY, 'turretPos (world copy for LOS)', E_TURRET_POS),
    (ENEMY, 'LOS uses the world position', E_LOS),
    (ENEMY, 'aim point stored in the local frame', E_SAVE_AIM),
    (ENEMY, 'muzzle converted to world', E_MUZZLE),
    (ENEMY, 'shot sound at the world muzzle', E_SOUND),
    (ENEMY, 'disable particles in world space', E_EFFECT),
    (ENEMY, 'disable sound at the world centre', E_EFFECT_SOUND),
    (ENEMY, 'muzzle flash tracked from the world chunk', E_TRACKING),
]


def main():
    apply = '--apply' in sys.argv
    contents = {}
    done = 0
    for path, label, (old, new) in PATCHES:
        if path not in contents:
            with open(path, 'r', encoding='utf-8') as fh:
                contents[path] = fh.read()
        text = contents[path]
        rel = os.path.relpath(path, ROOT).replace('\\', '/')
        if new in text:
            print('skip %-58s %-40s already applied' % (rel, label))
            continue
        count = text.count(old)
        if count != 1:
            print('FAIL %-58s %-40s anchor found %d time(s)' % (rel, label, count))
            return 2
        contents[path] = text.replace(old, new, 1)
        done += 1
        print('ok   %-58s %s' % (rel, label))

    if apply:
        for path, text in contents.items():
            with open(path, 'w', encoding='utf-8', newline='') as fh:
                fh.write(text)
        print('applied (%d change(s))' % done)
    else:
        print('dry run (use --apply)')
    return 0


if __name__ == '__main__':
    sys.exit(main())
