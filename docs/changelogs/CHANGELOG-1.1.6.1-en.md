Changes in 1.1.6.1

Two enchantment fixes and one cross-mod compatibility fix.

Fixed

- The corroded enchantment applies poison again. On 1.20.1 it hooked the vanilla post-attack call by overriding
  the enchantment itself; after the move to data-driven enchantments that logic became a static method nothing
  called, so swords and axes never poisoned anything. It now runs after damage lands: 30% chance, 60 + 20 per level
  ticks of poison.
- Corroded no longer rolls on axes at the enchanting table. Its primary_items now matches Sharpness and Smite (swords
  only), so axes can still get it from a book or an anvil.
- The hot barrel description no longer promises bonus fire rate. The enchantment grants damage, recoil, kick, spread
  and ignition; a fire-rate bonus was never implemented.
- A bayonet attached to a gun now triggers other mods' data-driven on-hit enchantments. The bayonet is an attachment
  rather than the held item, so the game never ran its post-attack effects and enchantments such as Soul Fire'd's
  soul fire aspect did nothing.
- Off-hand melee swings trigger those post-attack enchantments as well.

Notes

- Target: Minecraft 1.21.1, NeoForge 21.1.x, Java 21.
- Requires GeckoLib, Framework and Curios.
