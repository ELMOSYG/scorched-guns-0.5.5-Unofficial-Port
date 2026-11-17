# 1.1.2

Follow-up to `1.1.1coldfix`. Fixes only, no new gameplay.

## Fixed

### Bullets no longer ignore their own size
A projectile's `size` used to affect only the visible hitbox and block collisions: entity hits were tested against the
line its centre travels along, so a `size: 2` round grazed a target and passed through. Hit detection now grows the
target box by the round's radius before testing, so a fat round finally behaves like one (very visible on guns with a
custom `size`, such as `forlorn_hope`).

### Reload fix carried over
Stale reload action groups on the server are still cleared after a timeout, as introduced in `1.1.1coldfix`.

### Projectile accumulation
The headline fix of the previous release is kept: rounds are no longer saved to disk, rounds beyond 125 blocks are
removed by the periodic sweep, and the sweep no longer prints a line every time it runs.

## Changed

- Chinese enchantment names follow what the enchantments actually do: 音速弹 (Hyper Velocity), 穿甲弹 (Sharpshooter),
  贯穿弹 (Perforator), 重型弹 (Heavy Shot), 灵巧快手 (Swift Reloader), 金手指 (Lead Finger). A name for an
  enchantment that does not exist in game (Flame Seeder) was removed.
- Temporary diagnostic logging was removed from the build.

## Known issues (upstream design, unchanged)

- Pressing the unload key (U) during a reload can still leave an endless reload that consumes no ammo, and revolver
  type weapons can mint casings. Workaround: do not press U while the reload animation is playing.
- Armour penetration keeps the 1.20.1 upstream percentage model.

## Technical

- Target: Minecraft 1.21.1 / NeoForge 21.1.x, Java 21.
- Requires: GeckoLib, Framework, Curios.
