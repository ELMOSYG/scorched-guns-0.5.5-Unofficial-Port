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
| **Port version** | **0.5.5.1** (release/file version — the content is upstream 0.5.5) |
| License | **GNU General Public License v3.0** (the upstream mod is GPL-3.0, so this port must be too) |
| Source | https://github.com/ELMOSYG/scorched-guns-0.5.5-Unofficial-Port |
| Summary | An unofficial port of Scorched Guns 0.5.5 (Minecraft 1.20.1 / Forge 47.x) to Minecraft 1.21.1 / NeoForge. |

Upload `build/libs/scguns-0.5.5.1.jar` (≈18.5 MB) as the file, with **0.5.5.1** as its display name.

## State of the live page (checked after it was created)

| Item | State |
|---|---|
| Title / summary / description | ✅ set — the description on the page is the **HTML block below**, character for character |
| Categories | ✅ Armor/Tools and Weapons, Ores and Resources, Mobs, Technology, Redstone |
| Relations (required deps) | ❓ not visible through the public API — set Framework / GeckoLib / Curios as **Required** |
| License | ❓ not visible through the public API — it must be **GPL-3.0**, the upstream licence |
| First file | ⚠️ **`scguns-0.5.5.jar` (19,393,354 bytes)** — that build predates the fixes for `scguns:niami` (HANDOFF §72) and the 0.5.5.1 version bump (§73) |

**Replace that file**: upload `build/libs/scguns-0.5.5.1.jar` (**19,392,915 bytes**, version `0.5.5.1`) as a
new release file, then retire/delete the old one. CurseForge files are immutable, so a re-upload of the same
name is not possible anyway — and the byte size is the quickest way to tell them apart.

## Description — Markdown (paste into the editor's Markdown mode)

```markdown
# Scorched Guns 0.5.5: NeoForge Port

**Unofficial Scorched Guns 0.5.5: NeoForge Port** brings the original [Scorched Guns made by ribs](https://www.curseforge.com/minecraft/mc-mods/scorched-guns) experience to **Minecraft 1.21.1** on **NeoForge**.

This port was built from the **0.5.5 release** for Minecraft 1.20.1, and focuses on preserving the original mod's gritty firearm combat, custom ammunition systems, attachments, hostile mobs, structures, loot, machines, turrets and ExoSuit gameplay on a newer Minecraft version.

## Features

* A large arsenal of firearms and custom weapons — all **141 guns**, fully animated
* Multiple ammunition types, magazines and reload styles (mag-fed, single-item and manual reloads)
* Weapon attachments and gun customization, with attachment durability and Mending support
* Hostile mobs from the original mod, including gunner factions that spawn already carrying guns
* Raids and the gun-tier progression system, with progress messages
* World structures with loot — including the 0.5.5 loot injection into dungeons and mineshafts
* Turrets and combat machines, with targeting, ammo modules and turret platforms
* GunBench and crafting-related systems, including the **gun bench recipe book** with its own tabs (guns / turrets / ExoSuit), JEI categories and blueprint ghost placement
* Blueprints, machines (mech press, macerator, polar generator, ...) and their upgrades
* ExoSuit equipment and upgrade mechanics, including flight and gas-mask handling
* Custom particles, projectiles, sounds and effects, plus the mod's own enchantments (gun rust is a real curse: red name, not removable at the grindstone)
* Singleplayer, multiplayer and dedicated server support
* **Optional compatibility with Create** — Create 6 recipes, sequenced assembly ammo lines and mechanical crafting for guns
* **Optional compatibility with Create Aeronautics / Sable** — bullets and turrets interact with physical structures; shots push structures with an impulse scaled to Sable's own punch strength, and turrets built on a moving structure aim in world space
* **Optional compatibility with Immersive Engineering, Mekanism, Create: New Age, Create Ore Excavation and Farmer's Delight** — their materials appear in the mod's recipes when installed
* **Optional compatibility with Touhou Little Maid** — maids can use the mod's firearms, grenades and medkits (shipped as a nested mod; completely inactive if TLM is not installed)

## Requirements

This port requires:

* NeoForge for Minecraft 1.21.1 — **21.1.150 or newer**
* Framework (MrCrayfish) 0.13.11+
* GeckoLib 4.9.3+
* Curios API 9.5.1+

Make sure all required dependencies are installed before launching the game. The optional compatibility mods are not required — each of them only activates when it is present.

## Notes

This is a NeoForge 1.21.1 port of **Scorched Guns 0.5.5**, not a new original mod. Nothing was rebalanced: the goal is to keep the original gameplay experience working on a newer Minecraft version while preserving the feel and systems of the source mod as closely as possible.

The port was verified on a dedicated server on the minimum supported NeoForge (21.1.150) and in the client on 21.1.250. A few purely visual details are still listed as "needs a player to confirm" in the repository's porting notes, and two Create-based recipes log a parse error when Create is not installed (inherited from 0.5.5). The complete porting notebook — every trap, every fix and the evidence for each claim — is in the source repository (`HANDOFF.md`, 71 sections, plus 23 static audits).

## Credits

Original Scorched Guns concept, assets, data and gameplay belong to **ribs** (credits: MrCrayfish, Ribs) and the original mod's contributors. Sounds are CC0.

This project is an unofficial port to NeoForge 1.21.1; the maid compatibility is written for this port, and the Simplified Chinese translation comes from a community translation pack.

All credit for the mod itself goes to the original author — please do not report this port's bugs to them.
```

## Description — HTML (only if the editor is in HTML mode)

Same text as HTML. Use whichever form the editor is in; do not paste both.

```html
<h1>Scorched Guns 0.5.5: NeoForge Port</h1>
<p><strong>Unofficial Scorched Guns 0.5.5: NeoForge Port</strong> brings the original <a href="https://www.curseforge.com/minecraft/mc-mods/scorched-guns" target="_blank" rel="nofollow">Scorched Guns made by ribs</a> experience to <strong>Minecraft 1.21.1</strong> on <strong>NeoForge</strong>.</p>
<p>This port was built from the <strong>0.5.5 release</strong> for Minecraft 1.20.1, and focuses on preserving the original mod's gritty firearm combat, custom ammunition systems, attachments, hostile mobs, structures, loot, machines, turrets and ExoSuit gameplay on a newer Minecraft version.</p>
<h2>Features</h2>
<ul>
<li>A large arsenal of firearms and custom weapons - all <strong>141 guns</strong>, fully animated</li>
<li>Multiple ammunition types, magazines and reload styles (mag-fed, single-item and manual reloads)</li>
<li>Weapon attachments and gun customization, with attachment durability and Mending support</li>
<li>Hostile mobs from the original mod, including gunner factions that spawn already carrying guns</li>
<li>Raids and the gun-tier progression system, with progress messages</li>
<li>World structures with loot - including the 0.5.5 loot injection into dungeons and mineshafts</li>
<li>Turrets and combat machines, with targeting, ammo modules and turret platforms</li>
<li>GunBench and crafting-related systems, including the <strong>gun bench recipe book</strong> with its own tabs (guns / turrets / ExoSuit), JEI categories and blueprint ghost placement</li>
<li>Blueprints, machines (mech press, macerator, polar generator, ...) and their upgrades</li>
<li>ExoSuit equipment and upgrade mechanics, including flight and gas-mask handling</li>
<li>Custom particles, projectiles, sounds and effects, plus the mod's own enchantments (gun rust is a real curse)</li>
<li>Singleplayer, multiplayer and dedicated server support</li>
<li><strong>Optional compatibility with Create</strong> - Create 6 recipes, sequenced assembly ammo lines and mechanical crafting for guns</li>
<li><strong>Optional compatibility with Create Aeronautics / Sable</strong> - bullets and turrets interact with physical structures, and turrets built on a moving structure aim in world space</li>
<li><strong>Optional compatibility with Immersive Engineering, Mekanism, Create: New Age, Create Ore Excavation and Farmer's Delight</strong> - their materials appear in the mod's recipes when installed</li>
<li><strong>Optional compatibility with Touhou Little Maid</strong> - maids can use the mod's firearms, grenades and medkits (shipped as a nested mod, inactive without TLM)</li>
</ul>
<h2>Requirements</h2>
<p>This port requires:</p>
<ul>
<li>NeoForge for Minecraft 1.21.1 - <strong>21.1.150 or newer</strong></li>
<li>Framework (MrCrayfish) 0.13.11+</li>
<li>GeckoLib 4.9.3+</li>
<li>Curios API 9.5.1+</li>
</ul>
<p>Make sure all required dependencies are installed before launching the game. The optional compatibility mods are not required - each of them only activates when it is present.</p>
<h2>Notes</h2>
<p>This is a NeoForge 1.21.1 port of <strong>Scorched Guns 0.5.5</strong>, not a new original mod. Nothing was rebalanced: the goal is to keep the original gameplay experience working on a newer Minecraft version while preserving the feel and systems of the source mod as closely as possible.</p>
<p>The port was verified on a dedicated server on the minimum supported NeoForge (21.1.150) and in the client on 21.1.250. A few purely visual details are still listed as "needs a player to confirm" in the repository's porting notes, and two Create-based recipes log a parse error when Create is not installed (inherited from 0.5.5). The complete porting notebook is in the source repository (HANDOFF.md, 71 sections, plus 23 static audits).</p>
<h2>Credits</h2>
<p>Original Scorched Guns concept, assets, data and gameplay belong to <strong>ribs</strong> (credits: MrCrayfish, Ribs) and the original mod's contributors. Sounds are CC0.</p>
<p>This project is an unofficial port to NeoForge 1.21.1; the maid compatibility is written for this port, and the Simplified Chinese translation comes from a community translation pack.</p>
<p>All credit for the mod itself goes to the original author - please do not report this port's bugs to them.</p>
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
First public release of the unofficial 1.21.1 / NeoForge port of Scorched Guns 0.5.5 (port version 0.5.5.1).

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

1. `gradlew build` passes and `build/libs/scguns-0.5.5.1.jar` is the file you want (≈18.5 MB).
2. The GitHub repository is pushed — CurseForge asks for the source URL, and GPL-3.0 requires the
   source to be available for the binary you publish.
3. Do **not** upload the dependency jars from `libs/` — they belong to their own authors; list them as
   required relations instead.
4. The Chinese translation credit in `NOTICE` is a placeholder: replace it with the translator's name or
   a link to the resource pack before publishing (or say the word and it stays generic).
5. If you link the other 1.21.1 port on your page, note that this one is based on **0.5.5** while
   `scorched-guns-neoforged` is based on the later SC2 versions — different content sets, so players
   should not mix their configs/saves' expectations.
