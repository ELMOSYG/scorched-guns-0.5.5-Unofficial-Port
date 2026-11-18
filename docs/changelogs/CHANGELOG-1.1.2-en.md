Changes in 1.1.2

Follow up to 1.1.1coldfix. Fixes only, no new gameplay.

Fixed

- Bullets no longer ignore their own size. A projectile's size used to affect only the visible hitbox and block collisions: entity hits were tested against the line its centre travels along, so a round of size 2 grazed a target and passed through. Hit detection now grows the target box by the round's radius before testing.
- Temporary diagnostic logging was removed from the build.

Changed

- Chinese enchantment names follow what the enchantments actually do: Hyper Velocity, Perforator, Heavy Shot, Swift Reloader and Lead Finger were renamed; a name for an enchantment that does not exist in game was removed.

Known issues (unchanged from upstream)

- Pressing the unload key (U) during a reload can still leave an endless reload that consumes no ammo, and revolver type weapons can mint casings. Workaround: do not press U while the reload animation is playing.
- Armour penetration keeps the 1.20.1 upstream percentage model.

Technical

- Target: Minecraft 1.21.1, NeoForge 21.1.x, Java 21.
- Requires GeckoLib, Framework and Curios.
