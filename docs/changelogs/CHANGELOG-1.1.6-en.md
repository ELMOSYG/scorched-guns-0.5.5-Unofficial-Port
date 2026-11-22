Changes in 1.1.6

This release carries a set of magazine-data stability fixes and a rework of the mob trap.

Magazines and ammo

- The creative-mode automatic magazine fill no longer writes a count while a gun definition has not resolved, which
  used to put the empty definition's default 30 rounds into guns that hold one.
- The reload paths do the same check and refuse to write a magazine count before the definition has resolved.
- Overflow correction now tells two cases apart. When a gun's definition answers with an air projectile (the "air
  magazine" state) the surplus is corrected silently instead of being turned into ammo items; when the definition is
  real - an extended magazine just removed, for example - the surplus is refunded as real ammo.
- Reading a gun's definition no longer clamps the magazine count, so rounds that are genuinely still loaded are not
  swallowed.

Mob trap

- Reworked to behave like a trial spawner: a survival or adventure player within 14 blocks and in line of sight
  starts a 40 tick charge, then the stored mobs are released one at a time, two at most at once (one more per extra
  player), and the block disappears 40 ticks after the last one dies.
- Once spawning has started the trap stops checking player position or sight and simply finishes what it was loaded
  with.
- Spawn positions are searched within three blocks for a spot the mob fits into (air at feet and head, a floor below,
  no collision), so mobs no longer appear inside walls.
- Trial spawner sounds: player detection, each spawn, the closing shutter and the break.
- Trial spawner particles: detection particles when a player is noticed, rising sparks while running, smoke when it
  clears.
- Fixed the trap ignoring spawn egg right clicks (1.21 reads the entity type from the item stack).
- New config `mobTrapSpawnsInWaves` (on by default). Setting it to `false` restores the original behaviour: the whole
  list spawns at once and the block vanishes immediately.

Notes

- Target: Minecraft 1.21.1, NeoForge 21.1.x, Java 21.
- Requires GeckoLib, Framework and Curios.
