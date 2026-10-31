# Scorched Guns (unofficial 1.21.1 / NeoForge port)

Scorched Guns is a gun mod by ribs. This is the 0.5.5 version brought to Minecraft 1.21.1 on NeoForge.
Community-maintained, not official.

The original targets 1.20.1 Forge. This is not a recompile: 1.21 rewrote item data, enchantments, armour,
loot and networking, and a lot of code that still compiles no longer does what it did. Beyond getting it to
build, a long list of runtime problems had to be fixed. What follows describes the current state and the
known gaps.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.150 or newer |
| Required | Framework, GeckoLib 4.9.3+, Curios 9.5.1+ |

Controllable, Backpacked and Guard Villagers are optional compatibilities; the mod runs without them.

## What's in it

- Over a hundred guns, flintlocks to energy weapons, with attachments (scopes, barrels, stocks, magazines, bayonets)
- An ammunition chain: casings, projectiles, powder, and machines that reclaim spent casings
- Gun bench, ammunition bench, mechanical press, turrets
- A set of gun-specific enchantments that use the vanilla enchantment system, so the table, anvil and loot all work
- Armed hostiles: pillagers, piglins, and the mod's own machines
- Gas masks, poison gas, and an exosuit

## On balance

Early guns are cheap in materials: across the copper, iron, frontier and antique blueprints a gun resolves to
5-14 base items. The real gate is the blueprint (loot, gunsmith trades), not the ingredients. Recipe data lives
in `data/scguns/recipe/guns/` and `data/scguns/recipe/` if you want to change that.

## With other mods

- Guard Villagers: guards carry guns with their own AI (keeps distance, reloads, avoids friendly fire)
- Sable / Create: Aeronautics: muzzle and hit points are converted for sub-levels
- Touhou Little Maid: a separate submodule lets maids use guns (shooting, reloading, repositioning, grenades)
- Combat mods that animate mob attacks: the mod cancels the melee swing for mobs holding a gun

## Known limits

- Other players' gun parts **are** visible, but they do not move with that player's own shots. That state is not
  available client-side; a sync attempt did not work and was reverted.
- The mod's cog minion and viventrum draw inner and outer armour layers only; mobs with custom models (maids,
  and similar) do not show armour at all.
- Against `Enchanting Infuser`-style tables the mod's enchantments are expensive (21 for several), so a 30-point
  budget fits very little.

## Reporting problems

Include `latest.log` and the steps to reproduce. A screenshot helps.
