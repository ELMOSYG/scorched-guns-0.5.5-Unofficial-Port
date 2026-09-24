# Scorched Guns 0.5.5 — Unofficial 1.21.1 / NeoForge port

An **unofficial** port of [Scorched Guns](https://www.curseforge.com/minecraft/mc-mods/scorched-guns) **0.5.5** (Minecraft 1.20.1 / Forge 47.x) to **Minecraft 1.21.1 / NeoForge**.

The port was built from the **0.5.5 release jar** (decompiled and remapped), not from any GitHub source tree, so the gameplay content is the one you know from 0.5.5 — all 141 guns, attachments, grenades, turrets, exo-suits, raids, blueprints, the gun bench, machines and the mob factions.

> Not affiliated with the original author. See [Credits and license](#credits-and-license).

## Requirements

| | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | **21.1.150 or newer** |
| [Framework](https://www.curseforge.com/minecraft/mc-mods/framework) (MrCrayfish) | 0.13.11+ |
| [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) | 4.9.3+ |
| [Curios API](https://www.curseforge.com/minecraft/mc-mods/curios) | 9.5.1+ |

Framework, GeckoLib and Curios are **required** — the mod will not load without them.

Optional integrations (each of them only activates when the other mod is present): JEI, Create (+ Create New Age, Create Ore Excavation, Create Aeronautics), Immersive Engineering, Mekanism, Sable, Prometheus / Soul Fire'd, Cobweb, Farmer's Delight, Touhou Little Maid (via the bundled maid-compat).

## Installing

1. Install NeoForge 21.1.150+ for Minecraft 1.21.1.
2. Drop `scguns-0.5.5.jar` into your `mods/` folder.
3. Drop Framework, GeckoLib and Curios into `mods/` as well.

The jar also ships **Scorched Guns: Maid Compat** as a nested mod (its own mod id, mixin config and class loader). It only does anything when Touhou Little Maid is installed, and it is completely inert otherwise.

## Building from source

The build needs the dependency jars in `libs/` — they are **not** distributed here (they belong to their own authors). See [`libs/README.md`](libs/README.md) for the exact file names and where to get them, and [`maid-compat/libs/README.md`](maid-compat/libs/README.md) + [`libs-compile/README.md`](libs-compile/README.md) for the two extra cases.

```powershell
$env:JAVA_HOME='<path to a JDK 21>'          # required - a Java 8 on PATH will not work
cmd /c "gradlew.bat build --console=plain"   # -> build/libs/scguns-0.5.5.jar
```

Useful switches and gates:

```powershell
# Dev run without the optional integration mods (they demand a newer NeoForge than the floor)
cmd /c "gradlew.bat runServer -PnoIntegrationRuntime=true --console=plain"

# Can this source tree really run on the NeoForge floor it advertises?
python tools/check_neoforge_api_floor.py --run
```

## Porting documentation

This repository ships the complete working notebook of the port, including every dead end:

* [`HANDOFF.md`](HANDOFF.md) — **the handover document** (Chinese, ~5500 lines, 70 sections): what the port does, why, every trap, and the evidence for each claim.
* [`PORTING_STATUS.md`](PORTING_STATUS.md) — the status board (short form).
* `tools/` — 23 static audits plus the rewrite/generator scripts, each of them able to fail on the build it was written for.

The **Static checks** workflow runs the subset of those audits that needs nothing but this repository
(sources and resources). The rest — the audits that read compiled classes, the NeoForge sources or the
dependency jars — are documented in `HANDOFF.md` §6.5 and §70.3 and are meant to be run locally.

Highlights of the mechanical part of the port (1.21 / NeoForge changed all of it):

* NBT → `DataComponents.CUSTOM_DATA` (`NbtHelper`), keeping every original NBT key name.
* MrCrayfish Framework 0.13 `StreamCodec` networking behind a reflection bridge (49 message classes kept as they were).
* Item capabilities (`RegisterCapabilitiesEvent`) instead of `ICapabilityProvider`/`LazyOptional`.
* Data-driven enchantments, armor materials, jukebox songs and recipe conditions.
* A long list of **"the old signature is gone, so nothing is overridden any more"** bugs (mobs spawned without guns, grenades exploding in your hand, translucent layers rendering opaque, workstations not reacting to right click …), each now protected by `@Override` and an audit.

## Known limitations

* Only the **dedicated server** has been verified on the lowest supported NeoForge (21.1.150); the client has been tested on 21.1.250.
* Redirecting the loot-injection, enchantment-tag and recipe-book features relies on datapack behaviour that is verified on the server side; a few purely visual confirmations are still listed as "needs a player" in `HANDOFF.md`.
* Two recipes (`mech_press/depleted_diamond_steel` and its powered variant) reference a Create item without a mod-loaded condition — this is inherited from 0.5.5, and only shows up as a log error when Create is not installed.

## Credits and license

* **Scorched Guns** — original mod by **ribs** (credits: MrCrayfish, Ribs), licensed **GPL-3.0**. This port inherits that license (see [`LICENSE`](LICENSE)) and redistributes its assets and data.
* **Sounds** — CC0, as stated in the original jar (`assets/scguns/sounds/SOUND-LICENSE.txt`).
* **Simplified Chinese translation** (`zh_cn.json`) — provided by a community translation pack; see [`NOTICE`](NOTICE).
* **Maid compatibility** included as a nested mod — written for this port.
* Framework, GeckoLib, Curios and every optional integration belong to their own authors and are **not** redistributed here.

Full details, including the "this is unofficial" disclaimer, are in [`NOTICE`](NOTICE).

---

## 中文说明（简版）

这是 **Scorched Guns 0.5.5（1.20.1 / Forge）到 1.21.1 / NeoForge 的非官方移植**，基于 0.5.5 的发布 jar 反编译完成，内容与 0.5.5 一致。

* 需要：Minecraft 1.21.1、**NeoForge 21.1.150 或更高**、Framework 0.13.11+、GeckoLib 4.9.3+、Curios 9.5.1+。
* 移植过程、每一处坑与证据都写在 [`HANDOFF.md`](HANDOFF.md)（中文，70 节）与 [`PORTING_STATUS.md`](PORTING_STATUS.md) 里。
* 授权：GPL-3.0（沿用上游），原作者 ribs；本移植为非官方作品，与原作者无关。
