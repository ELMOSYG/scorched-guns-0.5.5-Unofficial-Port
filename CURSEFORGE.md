# CurseForge upload sheet

Ready-to-paste fields for the project page, modelled on the layout of the existing NeoForge port
(<https://www.curseforge.com/minecraft/mc-mods/scorched-guns-neoforged>) — intro, **Features**,
**Requirements**, **Notes**, **Credits** — with this port's own facts.

> That page's text was read through the public cfwidget API (`api.cfwidget.com/minecraft/mc-mods/scorched-guns-neoforged`),
> because curseforge.com itself answers 403 to a scripted request. Its claims about *other* optional
> mods (Punchy, Fresh Animations, Guard Villagers) are deliberately **not** copied — this port does not
> have those integrations, and it does not mention that port's feature set.

## Project fields

| Field | Value |
|---|---|
| Project name | **Scorched Guns 2 0.5.5 Unofficial port** ← created, live |
| Project URL | <https://www.curseforge.com/minecraft/mc-mods/scorched-guns-2-0-5-5-unofficial-port> |
| Slug | `scorched-guns-2-0-5-5-unofficial-port` |
| Project id | 1709719 |
| Game / Category | Minecraft → Mods |
| Version / Loader | 1.21.1, NeoForge |
| **Port version** | **1.0.4** (release/file version — the content is upstream 0.5.5) |
| License | **GNU General Public License v3.0** (the upstream mod is GPL-3.0, so this port must be too) |
| Source | https://github.com/ELMOSYG/scorched-guns-0.5.5-Unofficial-Port |
| Summary | An unofficial port of Scorched Guns 0.5.5 (Minecraft 1.20.1 / Forge 47.x) to Minecraft 1.21.1 / NeoForge. |

Upload `build/libs/scguns-1.0.4.jar` (≈18.5 MB) as the file, with **1.0.4** as its display name.

## State of the live page (checked after it was created)

| Item | State |
|---|---|
| Title / summary / description | ✅ set — the description on the page is the **HTML block below**, character for character |
| Categories | ✅ Armor/Tools and Weapons, Ores and Resources, Mobs, Technology, Redstone |
| Relations (required deps) | ❓ not visible through the public API — set Framework / GeckoLib / Curios as **Required** |
| License | ❓ not visible through the public API — it must be **GPL-3.0**, the upstream licence |
| First file | ⚠️ **`scguns-0.5.5.jar` (19,393,354 bytes)** — that build predates the fixes for `scguns:niami` (HANDOFF §72) and the 1.0.4 version bump (§73) |

**Replace that file**: upload `build/libs/scguns-1.0.4.jar` (**19,519,472 bytes**, version `1.0.4`) as a
new release file, then retire/delete the old one. CurseForge files are immutable, so a re-upload of the same
name is not possible anyway — and the byte size is the quickest way to tell them apart.

## Description — Markdown (paste into the editor's Markdown mode)

```markdown
# Scorched Guns 0.5.5 · Unofficial 1.21.1 / NeoForge Port

**Port version 1.0.4** | file name `scguns-1.0.4.jar`

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

- Original mod: Scorched Guns by ribs
- Maid compatibility: provided alongside this port
- Addon authors: AsYasya's WW1 Weaponry and others

**Please do not report problems with this port to the original author.**
```

## Description — HTML (only if the editor is in HTML mode)

Same text as HTML. Use whichever form the editor is in; do not paste both.

```html
<h1>Scorched Guns 0.5.5 · Unofficial 1.21.1 / NeoForge Port</h1>
<p>**Port version 1.0.4** | file name `scguns-1.0.4.jar`</p>
<p>An **unofficial** port of **Scorched Guns 0.5.5** (originally Minecraft 1.20.1 / Forge) to</p>
<p>**Minecraft 1.21.1 / NeoForge**, built to stay close to the original: all 141 guns were checked</p>
<p>against the upstream 0.5.5 data, and the advancements, raids, loot and production chain are kept as</p>
<p>they were.</p>
<p>---</p>
<h2>What makes this port different</h2>
<p>**1. A compatibility layer for addons built against other ports.**</p>
<p>Many gun addons are compiled against a different 1.21.1 port. Dropped into this one, their attachments</p>
<p>would not render, recoil and spread did nothing, critical hits and damage falloff were ignored, and</p>
<p>enchanting failed. All of that is fixed here.</p>
<p>**2. Three problems that made the game or a save unplayable were fixed.**</p>
<ul>
<li>**Wiring the lightning battery to another mod&#x27;s energy network crashed the server** and then left the</li>
</ul>
<p>  **save unloadable**. The battery pushed energy into its neighbours from inside its own tick, while the</p>
<p>  other side was iterating its own member table. Fixed in three places, without breaking other mods that</p>
<p>  hook the same code.</p>
<ul>
<li>**Bullet holes never appeared on a dedicated server** (they worked in single player). The hit message</li>
</ul>
<p>  can reach the client before the chunk does, and the old code read that as &quot;this is air&quot; and deleted the</p>
<p>  decal.</p>
<ul>
<li>**Without Polymorph installed, the mod failed to load and the game would not start.** Now a missing or</li>
</ul>
<p>  mismatched Polymorph only costs the crafting recipe selector; the game launches normally.</p>
<p>**3. The ammo-counter magazines no longer twitch during a reload, and no longer flash back to the empty</p>
<p>position when the reload finishes** (Scrapper, Birdfeeder, Repeating Musket, Triquetra).</p>
<p>---</p>
<h2>New in this port</h2>
<ul>
<li>**Automatic beam** and **burst beam** fire modes. Both were defined upstream but never registered, so</li>
</ul>
<p>  writing them into data silently fell back to semi-automatic beams. They now work: holding the trigger</p>
<p>  emits pulses of laser instead of one unbroken line.</p>
<p>  **No gun data was changed** — whether to use them, and on which gun, is up to a datapack or an addon.</p>
<ul>
<li>**More accurate mob-type detection**: the game now asks the entity what it is first, then falls back to</li>
</ul>
<p>  entity type tags. Any entity that changes its reported type at runtime is therefore recognised — for</p>
<p>  example a training dummy wearing a zombie head can now be used to test undead bonuses.</p>
<ul>
<li>**The projectile advantage system** (for example the ramrod round&#x27;s 1.75x damage and 2 second burn</li>
</ul>
<p>  against undead) uses the same detection, so it works against those entities too.</p>
<p>---</p>
<h2>Contents</h2>
<p>Also included: turrets and combat machines, ExoSuit equipment, custom enchantments, blueprints and a gun</p>
<p>bench recipe book.</p>
<p>---</p>
<h2>Requirements</h2>
<p>Optional compatibility: Create, Create Aeronautics / Sable, Immersive Engineering, Mekanism,</p>
<p>Create: New Age, Create Ore Excavation, Farmer&#x27;s Delight, Touhou Little Maid (maids can use the guns).</p>
<p>---</p>
<h2>What we kept, and what we did not do</h2>
<p>So you know what to expect, here is what still follows the original:</p>
<ul>
<li>**Fire rate is still measured in whole ticks**: the ceiling is 1200 RPM and there is nothing between</li>
</ul>
<p>  600 and 1200, so values like 800 or 900 RPM cannot be expressed exactly. This is how the original works.</p>
<ul>
<li>**The &quot;armour penetration&quot; stat uses the original maths**: its effect on ordinary bullets is very</li>
</ul>
<p>  limited, and the number on the tooltip does not match what you get.</p>
<ul>
<li>**Mob fire cadence is unchanged**: gunners still strafe, aim and fire in bursts. That difficulty is</li>
</ul>
<p>  deliberate.</p>
<ul>
<li>**Content the original never enabled is left in place** (for example the End raid text and several</li>
</ul>
<p>  unused advancement texts) rather than being deleted.</p>
<p>---</p>
<h2>Credits</h2>
<p>**The name, numbers, art and design of Scorched Guns all belong to the original author, ribs.** This port</p>
<p>does not rename anything and does not redesign anything; it only ports and fixes.</p>
<ul>
<li>Original mod: Scorched Guns by ribs</li>
<li>Maid compatibility: provided alongside this port</li>
<li>Addon authors: AsYasya&#x27;s WW1 Weaponry and others</li>
</ul>
<p>**Please do not report problems with this port to the original author.**</p>
```

## Relations (dependencies)

Set these on the project page — players get a "missing dependency" prompt without them:

| Mod | Relation | Version |
|---|---|---|
| MrCrayfish's Framework | **Required** | 0.13.11+ |
| GeckoLib | **Required** | 4.9.3+ |
| Curios API | **Required** | 9.5.1+ |
| Just Enough Items | Optional | 19.27+ |
| Create | Optional | 6.0.10+ |
| Create: New Age | Optional | 1.2.0+ |
| Create Ore Excavation | Optional | 1.6.8+ |
| Create Aeronautics | Optional | 1.3.2+ |
| Immersive Engineering | Optional | 12.4.2+ |
| Mekanism | Optional | 10.7.19+ |
| Sable | Optional | 2.0.5+ |
| Prometheus / Soul Fire'd | Optional | 1.2.5+ / 6.1.0+ |
| Cobweb | Optional | 1.4.0+ |
| Farmer's Delight | Optional | 1.3.4+ |
| Touhou Little Maid | Optional | 1.5.3+ (the bundled maid compat activates only with it) |

Minimum **NeoForge 21.1.150** (GeckoLib 4.9.3 itself requires that).

## Changelog for the first release (paste into "Changelog")

```markdown
First public release of the unofficial 1.21.1 / NeoForge port of Scorched Guns 0.5.5 (port version 1.0.4).

* Ported from the 0.5.5 1.20.1 release: 141 animated guns, attachments, grenades, turrets, exo-suits,
  raids, blueprints, gun bench, machines, mob factions.
* 1.21 data components, Framework 0.13 networking, NeoForge capabilities, data-driven enchantments /
  armor materials / jukebox songs / recipe conditions.
* Gun bench recipe book with guns / turrets / exo-suit tabs, JEI categories, blueprint ghost placement.
* Enchantments behave like they did in 0.5.5: discoverable at the enchanting table, sold by librarians,
  gun rust is a real curse, loot injection into dungeons and mineshafts works again.
* Sable physics-structure support (bullet impulse scaled to Sable's punch strength, turret aiming in
  world space).
* Bundled maid compatibility for Touhou Little Maid (inactive without it).
* Requires Minecraft 1.21.1, NeoForge 21.1.150+, Framework 0.13.11+, GeckoLib 4.9.3+, Curios 9.5.1+.
```

## Before you press upload

1. `gradlew build` passes and `build/libs/scguns-1.0.4.jar` is the file you want (≈18.5 MB).
2. The GitHub repository is pushed — CurseForge asks for the source URL, and GPL-3.0 requires the
   source to be available for the binary you publish.
3. Do **not** upload the dependency jars from `libs/` — they belong to their own authors; list them as
   required relations instead.
4. The Chinese translation credit in `NOTICE` is a placeholder: replace it with the translator's name or
   a link to the resource pack before publishing (or say the word and it stays generic).
5. If you link the other 1.21.1 port on your page, note that this one is based on **0.5.5** while
   `scorched-guns-neoforged` is based on the later SC2 versions — different content sets, so players
   should not mix their configs/saves' expectations.
