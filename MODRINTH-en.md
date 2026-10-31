# Scorched Guns 0.5.5 · Unofficial 1.21.1 / NeoForge Port

**Port version 1.0.8** | file name `scguns-1.0.8.jar`

An **unofficial** port of **Scorched Guns 0.5.5** (originally Minecraft 1.20.1 / Forge) to
**Minecraft 1.21.1 / NeoForge**. Community-maintained; not affiliated with the original author's releases.

The port aims to stay close to the original: every gun was checked against the upstream 0.5.5 data, and the
advancements, raids, loot and production chain are kept as they were.

---

## Contents

| Item | Count |
|---|---|
| Guns | **Over 140**, all animated |
| Weapon types | 13 (pistol, SMG, rifle, shotgun, sniper, machine gun, heavy, rail, laser, plasma, shock, flamethrower, special) |
| Advancements | **261** |
| Raids | 11, each with a boss and henchmen |
| Production chain | From **power generation** (lightning battery, polar generator) through **ammunition and moulds** (stamping press, macerator) to the guns themselves |

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
Create: New Age, Create Ore Excavation, Farmer's Delight, Touhou Little Maid (maids can use the guns),
Guard Villagers (guards carry guns).

---

## What this port does

- **A compatibility layer for addons built against other ports.** Many gun addons are compiled against a
  different 1.21.1 port. Dropped into this one, their attachments would not render, recoil and spread did
  nothing, critical hits and damage falloff were ignored, and enchanting failed. All of that is fixed here.
- **Three problems that made the game or a save unplayable were fixed**: wiring the lightning battery to
  another mod's energy network crashed the server and left the save unloadable; bullet holes never appeared
  on a dedicated server; and without Polymorph installed the mod failed to load and the game would not start.
- **The ammo-counter magazines no longer twitch during a reload**, and no longer flash back to the empty
  position when the reload finishes.
- **Two things upstream defined but never registered** now work: automatic and burst beam fire modes.
  Mob-type detection now asks the entity first and falls back to entity type tags, so a training dummy
  wearing a zombie head counts as undead.

---

## Notes

- Gun damage, ammunition and attachment data are all datapack-editable; nothing is hard-locked.
- Running alongside another gun mod can make gun bench recipes collide; installing Polymorph lets you pick
  in the interface.
- When reporting a problem, include `latest.log` and the steps to reproduce. A screenshot helps.

---

## Credits

- **ribs**, the original author: the guns, art, data and design are all Scorched Guns.
- The 0.5.5 contributors and translators upstream.
- This port is community-maintained and unofficial.
