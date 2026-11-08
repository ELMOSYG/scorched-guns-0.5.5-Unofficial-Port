# Scorched Guns 0.5.5 — Unofficial 1.21.1 / NeoForge port

**Port version: 1.0.9.2 hotfix** — the content is the upstream 0.5.5 release; the number is this port's own release, and it is what the mod list shows and what the jar is named.

**Download:** [CurseForge](https://www.curseforge.com/minecraft/mc-mods/scorched-guns-2-0-5-5-unofficial-port) · [Modrinth](https://modrinth.com/mod/scorched-guns-neoforged) · [GitHub releases](https://github.com/ELMOSYG/scorched-guns-0.5.5-Unofficial-Port/releases)

An **unofficial** port of [Scorched Guns](https://www.curseforge.com/minecraft/mc-mods/scorched-guns) **0.5.5** (1.20.1 / Forge) to **1.21.1 / NeoForge**, built from the 0.5.5 release jar (decompiled and remapped) rather than any source tree. All 141 guns, attachments, grenades, turrets, exo-suits, raids, blueprints, the gun bench, machines and mob factions are here.

> Not affiliated with the original author. See [Credits and license](#credits-and-license).

## Requirements

| | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | **21.1.150 or newer** |
| [Framework](https://www.curseforge.com/minecraft/mc-mods/framework) (MrCrayfish) | 0.13.11+ |
| [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) | 4.9.3+ |
| [Curios API](https://www.curseforge.com/minecraft/mc-mods/curios) | 9.5.1+ |

Framework, GeckoLib and Curios are **required**.

Optional integrations activate only when the other mod is present: JEI, Create (+ New Age, Ore Excavation, Aeronautics), Immersive Engineering, Mekanism, Sable, Prometheus / Soul Fire'd, Cobweb, Farmer's Delight, Touhou Little Maid (bundled maid-compat).

## Installing

1. Install NeoForge 21.1.150+ for Minecraft 1.21.1.
2. Put `scguns-1.0.9.2hotfix.jar`, Framework, GeckoLib and Curios into `mods/`.

The jar also ships **Scorched Guns: Maid Compat** as a nested mod; it is inert unless Touhou Little Maid is installed.

## Building from source

The dependency jars belong to their own authors and are not distributed here — see [`libs/README.md`](libs/README.md), [`maid-compat/libs/README.md`](maid-compat/libs/README.md) and [`libs-compile/README.md`](libs-compile/README.md).

```powershell
$env:JAVA_HOME='<JDK 21>'                     # a Java 8 on PATH will not work
cmd /c "gradlew.bat build --console=plain"    # -> build/libs/scguns-1.0.9.2hotfix.jar
```

## Documentation

* [`docs/HANDOFF.md`](docs/HANDOFF.md) — the full handover notebook (Chinese, ~70 sections): what the port does, why, and the evidence for every claim.
* [`docs/PORTING_STATUS.md`](docs/PORTING_STATUS.md) — short status board.
* [`docs/PORTING_TOOLCHAIN.md`](docs/PORTING_TOOLCHAIN.md) — build and tooling notes.
* [`docs/changelogs/`](docs/changelogs) — every release note, Chinese and English.
* `tools/` — the static audits and generator scripts.

## Known limitations

* Only the **dedicated server** is verified on the lowest NeoForge (21.1.150); the client has been tested on 21.1.250.
* Two recipes (`mech_press/depleted_diamond_steel` and its powered variant) reference a Create item without a mod-loaded condition — inherited from 0.5.5, and only a log error when Create is absent.
* 3D sights render through `entityTranslucent`, which shader packs draw in a pass that does not write depth, so a sight can show the world through its solid parts. Set `sightsUseCutout = true` in `config/scguns-client.toml` if that happens.

## Credits and license

* **Scorched Guns** — by **ribs** (credits: MrCrayfish, Ribs), **GPL-3.0**. This port inherits that license (see [`LICENSE`](LICENSE)) and redistributes its assets and data.
* **Sounds** — CC0, as stated in the original jar.
* **Simplified Chinese translation** — originally by the Mod Wiki (mcmod.cn) user **ZR4U**, who submitted
  it upstream; continued and edited for this port (some unsuitable content removed, newer keys
  filled in). See [`NOTICE`](NOTICE).
* **Port author** — the 1.21.1 / NeoForge port, its fixes and new content were written by **ELMO_SYG**. AI tools were used for the porting work and for the code of new content; the original mod's assets and design are untouched.
* Framework, GeckoLib, Curios and every optional integration belong to their own authors and are **not** redistributed here.

Full details, including the "this is unofficial" disclaimer, are in [`NOTICE`](NOTICE).

---

## 中文说明（简版）

这是 **Scorched Guns 0.5.5（1.20.1 / Forge）到 1.21.1 / NeoForge 的非官方移植**，基于 0.5.5 发布 jar 反编译完成，内容与 0.5.5 一致。

* 需要：Minecraft 1.21.1、**NeoForge 21.1.150+**、Framework 0.13.11+、GeckoLib 4.9.3+、Curios 9.5.1+。
* 文档：移植记录在 [`docs/HANDOFF.md`](docs/HANDOFF.md) 与 [`docs/PORTING_STATUS.md`](docs/PORTING_STATUS.md)，发布日志在 [`docs/changelogs/`](docs/changelogs)。
* 光影下瞄具会“透出世界”：把 `config/scguns-client.toml` 的 `sightsUseCutout` 设为 `true`。
* 授权：GPL-3.0（沿用上游），原作者 ribs；本移植为非官方作品，与原作者无关。
* 移植：**ELMO_SYG**；移植过程与新增内容的代码使用了 AI 辅助。
