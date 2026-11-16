# Scorched Guns 1.1.1 (1.21.1 / NeoForge)

## Severe bug warning - please read first

**Pressing the unload key (default: U) while a per-round reload is playing makes the gun reload forever and
consumes no ammo at all.** Unloading empties the magazine, the reload keeps running, the magazine never fills,
and the reload never finishes. Nothing is deducted from the player's ammo, so the result is unlimited rounds.

**Cowell and other revolver / per-round reload guns can also produce casings out of nothing.** Interleaving
unload with a reload triggers the casing drop repeatedly with no matching consumption, so casings can be
generated indefinitely.

**Workaround: do not press unload while the reload animation is playing.** On a server, consider monitoring the
action through logs or permissions.

The defect comes from the reload state machine in upstream 0.5.5: the "is this gun reloading" answer is kept in
three parallel places (one synced flag and two NBT keys), and `AmmoCount` has several writers, so unloading and
reloading each overwrite it. Every patch we tried introduced a different problem elsewhere (reload duration
becoming random, no casing after an emptied magazine, stale state after switching guns), so 1.1.1 **keeps the
upstream behaviour** and documents the issue here instead of shipping another unstable change.

## Fixes

### Explosions and block damage
- Rocket blast radius reduced from 7.0 to 4.0, matching TNT. A single rocket used to destroy roughly five times
  as many blocks as TNT.
- The rocket explosion now fills its `getToBlow()` set again, fixing rockets that did not break blocks.
- High-explosive grenade rounds now create their explosion object, restoring block damage.

### Projectiles and hit detection
- Projectile entities refresh their dimensions after `projectile.size` is applied, so a changed size is actually
  used for hit detection.
- Projectile size data corrected: the odd `1e-05` and `100.0` values across 141 guns are clamped into 0.05-4.0.
- The melee hit packet no longer holds a live `ItemStack`, fixing an occasional concurrent-modification crash
  while reloading.

### Compatibility
- No duplicate arm is drawn when Entity Model Features is installed. Two new options: `suppressVanillaArmWithGun`
  and `emfSkipPlayerAnimationWithGun`.
- A recoil multiplier of zero now falls back to 1.0, so a third-party mod cannot silently remove all recoil.

### World and structures
- Structures are no longer flooded by 1.21's default `APPLY_WATERLOGGING` (six structure definitions plus
  `CogChambers`). Ship and submarine interiors in ocean biomes stay dry.

### Projectile accumulation
- Projectiles now have a 128 block flight limit. When the simulation distance is lower than the render distance,
  entities in the band between them are never ticked, so the lifetime check never advanced and plasma and sculk
  rounds sat there, kept visible by entity radars. Nothing can be hit past 128 blocks anyway.
- A projectile is discarded the moment it enters a region that will not tick, instead of stopping at the edge.
- The server sweeps this mod's projectiles beyond the simulation distance every ten seconds, clearing ones that
  are already parked.
- Projectiles are no longer written to chunk saves, so unloading a chunk cannot leave orphans behind.

### Performance
- The shock coil's lightning arc is emitted in world space, so clients no longer load far-away chunks because of
  internal coordinates.
- Arc particle step raised from 0.15 to 0.6 with a 48 point cap; `sendParticles` cost drops to about a quarter.
- The shock coil skips its target search entirely when no player is nearby, avoiding chunk loads for empty areas.

### Gameplay
- Guns crafted at a bench now start with an empty magazine. Previously a missing NBT value was read as a full one.
- The "gun loaded" packet is now idempotent, fixing magazine guns that resolved a reload twice on multiplayer.

## Notes

- Version `1.5.0+1.1.1`, file `scguns-1.1.1.jar`.
- Requires Minecraft 1.21.1 and NeoForge 21.1.x.
