# 1.0.3 — Addon Compatibility Overhaul

**Minecraft 1.21.1 · NeoForge** ｜ `scguns-1.0.3.jar`

This release focuses on gun addons built against the other 1.21.1 port, such as AsYasya's WW1 Weaponry.

## Addon Compatibility

- **Attachments show up again** — fixed addon gun attachments failing to render and crashing the client
- **Recoil and spread work** — fixed recoil angle, recoil kick, spread and always-spread having no effect on addon guns
- **Shotguns fire their full spread** — fixed addon shotguns firing a single pellet
- **Crit chance, energy cost and shooter knockback work** on addon guns
- **Damage falloff works** — fixed the falloff range having no effect on addon guns
- **Addon guns can be enchanted**
- **No more crashes** — a failing addon model is now skipped and logged instead of taking the client down
- **Inventory preview no longer twitches** — the inner model is no longer drawn on top of an override model

## Gun Bench / Polymorph

- The gun bench now offers a recipe choice when several gun addons ship duplicate recipes
- Only recipes matching the bench contents are offered, closing a free-craft exploit
- Fixed the Polymorph recipe selector not appearing at all
- The selector's position can be nudged from the client config

## HUD and Client

- The ammo HUD position is adjustable, so it can be moved clear of the subtitles
- Reloading no longer changes the field of view

## Gameplay and Visuals

- Lacerated and sulfur poisoning have their own death messages, instead of showing as "killed by magic"
- Both damage types now ignore armour, armour toughness, Resistance and Protection
- Fixed both effects shoving entities in a random direction
- Fixed beam weapons throwing targets around — knockback now follows the shooter's position, at the same strength
- Fixed a duplicated entity renderer registration for the shatter round

## Fixes

- Corrupted gun definitions in an existing save are repaired automatically (falls back to the built-in definition and clamps the ammo count to the magazine)
- Fixed every gun failing to load — it showed as ammo reading "Air", a fixed 30/30 magazine and no firing
- Fixed the direction data for damage that has no projectile behind it (beams and effect damage), so entities are no longer pushed in a random direction
