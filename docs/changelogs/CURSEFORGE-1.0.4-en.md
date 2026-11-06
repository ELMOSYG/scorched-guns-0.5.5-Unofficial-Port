# Scorched Guns 0.5.5 · Unofficial 1.21.1 / NeoForge Port

**Port version 1.0.5** | file name `scguns-1.0.5.jar`

An **unofficial** port of **Scorched Guns 0.5.5** (originally Minecraft 1.20.1 / Forge) to
**Minecraft 1.21.1 / NeoForge**, built to stay close to the original: all 141 guns were checked
against the upstream 0.5.5 data, and the advancements, raids, loot and production chain are kept as
they were.

---

## What makes this port different

**1. A compatibility layer for addons built against other ports.**
Many gun addons are compiled against a different 1.21.1 port. Dropped into this one, their attachments
would not render, recoil and spread did nothing, critical hits and damage falloff were ignored, and
enchanting failed. All of that is fixed here.

**2. Three problems that made the game or a save unplayable were fixed.**

- **Wiring the lightning battery to another mod's energy network crashed the server** and then left the
  **save unloadable**. The battery pushed energy into its neighbours from inside its own tick, while the
  other side was iterating its own member table. Fixed in three places, without breaking other mods that
  hook the same code.
- **Bullet holes never appeared on a dedicated server** (they worked in single player). The hit message
  can reach the client before the chunk does, and the old code read that as "this is air" and deleted the
  decal.
- **Without Polymorph installed, the mod failed to load and the game would not start.** Now a missing or
  mismatched Polymorph only costs the crafting recipe selector; the game launches normally.

**3. The ammo-counter magazines no longer twitch during a reload, and no longer flash back to the empty
position when the reload finishes** (Scrapper, Birdfeeder, Repeating Musket, Triquetra).

---

## New in this port

- **Automatic beam** and **burst beam** fire modes. Both were defined upstream but never registered, so
  writing them into data silently fell back to semi-automatic beams. They now work: holding the trigger
  emits pulses of laser instead of one unbroken line.
  **No gun data was changed** — whether to use them, and on which gun, is up to a datapack or an addon.
- **More accurate mob-type detection**: the game now asks the entity what it is first, then falls back to
  entity type tags. Any entity that changes its reported type at runtime is therefore recognised — for
  example a training dummy wearing a zombie head can now be used to test undead bonuses.
- **The projectile advantage system** (for example the ramrod round's 1.75x damage and 2 second burn
  against undead) uses the same detection, so it works against those entities too.

---

## Contents

| Item | Count |
|---|---|
| Guns | **141**, all animated |
| Weapon types | 13 (pistol, SMG, rifle, shotgun, sniper, machine gun, heavy, rail, laser, plasma, shock, flamethrower, special) |
| Advancements | **261** |
| Raids | 11, each with a boss and henchmen |
| Production chain | From **power generation** (lightning battery, polar generator) through **ammunition and molds** (stamping press, macerator) to the guns themselves |

Also included: turrets and combat machines, ExoSuit equipment, custom enchantments, blueprints and a gun
bench recipe book.

---

## Requirements

| Item | Requirement |
|---|---|
| Minecraft | **1.21.1** |
| NeoForge | **21.1.150 or newer** |
| Required mods | **Framework (MrCrayfish) 0.13.11+**, **GeckoLib 4.9.3+**, **Curios API 9.5.1+** |
| Where to install | **Both client and server** (a server without it will refuse to start) |

Optional compatibility: Create, Create Aeronautics / Sable, Immersive Engineering, Mekanism,
Create: New Age, Create Ore Excavation, Farmer's Delight, Touhou Little Maid (maids can use the guns).

---

## What we kept, and what we did not do

So you know what to expect, here is what still follows the original:

- **Fire rate is still measured in whole ticks**: the ceiling is 1200 RPM and there is nothing between
  600 and 1200, so values like 800 or 900 RPM cannot be expressed exactly. This is how the original works.
- **The "armour penetration" stat uses the original maths**: its effect on ordinary bullets is very
  limited, and the number on the tooltip does not match what you get.
- **Mob fire cadence is unchanged**: gunners still strafe, aim and fire in bursts. That difficulty is
  deliberate.
- **Content the original never enabled is left in place** (for example the End raid text and several
  unused advancement texts) rather than being deleted.

---

## Credits

**The name, numbers, art and design of Scorched Guns all belong to the original author, ribs.** This port
does not rename anything and does not redesign anything; it only ports and fixes.

- **Original mod**: Scorched Guns by **ribs** (GPL-3.0)
- **1.21.1 / NeoForge port**: **ELMO_SYG**
- **Maid compatibility**: provided alongside this port
- **Addon authors**: AsYasya's WW1 Weaponry and others
- **Sounds**: CC0, as stated in the original jar (`SOUND-LICENSE.txt`)

### On AI use

AI assistance was used for **the porting process and for the code of new content** (including code
migration, debugging and documentation). The original mod's assets and design are untouched, and the
port author is responsible for what is committed.

**Please do not report problems with this port to the original author.**
