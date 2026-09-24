# CurseForge upload sheet

Everything needed to publish this port on CurseForge. Copy the fields into the project page.

## Project

| Field | Value |
|---|---|
| Project name | **Scorched Guns 0.5.5 — Unofficial 1.21.1 / NeoForge Port** |
| Slug (suggested) | `scorched-guns-1-21-1-unofficial-port` |
| Game / Category | Minecraft → Mods |
| License | **GNU General Public License v3.0** (same as the upstream mod — it is GPL-3.0, so this port must be too) |
| Source | https://github.com/ELMOSYG/scorched-guns-0.5.5-Unofficial-Port |
| Summary (short field) | Unofficial port of Scorched Guns 0.5.5 to Minecraft 1.21.1 / NeoForge. All 141 guns, attachments, grenades, turrets, exo-suits and raids, ported from the 0.5.5 release with the full porting notes included. |

Upload the file `build/libs/scguns-0.5.5.jar` (≈19 MB). Game version **1.21.1**, mod loader **NeoForge**.

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

## Description (Markdown, ready to paste)

```markdown
# Scorched Guns 0.5.5 — Unofficial 1.21.1 / NeoForge Port

An **unofficial** port of **Scorched Guns 0.5.5** (Minecraft 1.20.1 / Forge) to **Minecraft 1.21.1 / NeoForge**.

The port was built from the **0.5.5 release jar**, so the content is the 0.5.5 you know: all 141 guns,
attachments, grenades, turrets, exo-suits, raids, blueprints, the gun bench, the machines and the mob
factions — no gameplay rebalancing, no missing content.

> I am not the original author. All credit for the mod goes to **ribs** (credits: MrCrayfish, Ribs).
> This port is unofficial and unsupported by them; please do not report its bugs to them.

## Requirements

* Minecraft 1.21.1 + NeoForge **21.1.150 or newer**
* **Required**: Framework 0.13.11+, GeckoLib 4.9.3+, Curios API 9.5.1+
* Optional integrations activate only when the other mod is installed: JEI, Create (+ New Age, Ore
  Excavation, Aeronautics), Immersive Engineering, Mekanism, Sable, Prometheus / Soul Fire'd, Cobweb,
  Farmer's Delight, Touhou Little Maid

## What the port covers

* NBT gun state moved onto 1.21 data components while keeping every original NBT key name, so existing
  tools and addons keep working.
* Enchantments, armor materials, jukebox songs, recipe conditions and the loot injection converted to
  the data-driven 1.21 form (the mod's enchantments are discoverable at the enchanting table and
  tradeable by librarians again, gun rust is a real curse, loot injection into dungeons/mineshafts works).
* The gun bench recipe book (its own recipe book type and tabs: guns, turrets, exo-suits, plus a search
  page), JEI categories, and ghost/one-click placement including the blueprint slot.
* Sable physics structures: bullets hit them, they take impulse scaled to Sable's own punch strength,
  and turrets placed on a moving structure aim in world space.
* A maid compatibility (Touhou Little Maid) is bundled as a nested mod; it stays completely inactive
  when TLM is not installed.

## Known limitations

* Verified on NeoForge 21.1.150 (dedicated server) and 21.1.250 (client + server).
* A few purely visual details are still listed as "needs a player to confirm" in the repository's
  porting notes.
* Two recipes that reference a Create item have no mod-loaded condition (inherited from 0.5.5); without
  Create installed they log a parse error and are skipped.

## Porting notes

The complete working notebook is in the repository (`HANDOFF.md`, 70 sections, Chinese) — every trap,
every fix and the evidence behind each claim, plus 24 static audits under `tools/`.
```

## Changelog for the first release (paste into "Changelog")

```markdown
First public release of the unofficial 1.21.1 / NeoForge port of Scorched Guns 0.5.5.

* Ported from the 0.5.5 1.20.1 release: 141 animated guns, attachments, grenades, turrets, exo-suits,
  raids, blueprints, gun bench, machines, mob factions.
* 1.21 data components, Framework 0.13 networking, NeoForge capabilities, data-driven enchantments /
  armor materials / jukebox songs / recipe conditions.
* Gun bench recipe book with guns / turrets / exo-suit tabs, JEI categories, blueprint ghost placement.
* Sable physics-structure support (bullet impulse, turret aiming in world space).
* Bundled maid compatibility for Touhou Little Maid (inactive without it).
* Requires Minecraft 1.21.1, NeoForge 21.1.150+, Framework 0.13.11+, GeckoLib 4.9.3+, Curios 9.5.1+.
```

## Before you press upload

1. `gradlew build` passes and `build/libs/scguns-0.5.5.jar` is the file you want (≈19 MB).
2. The GitHub repository is pushed — CurseForge asks for the source URL, and GPL-3.0 requires the
   source to be available for the binary you publish.
3. Do **not** upload the dependency jars from `libs/` — they belong to their own authors; list them as
   required relations instead.
4. The Chinese translation credit in `NOTICE` is a placeholder: replace it with the translator's name or
   a link to the resource pack before publishing (or say the word and it stays generic).
