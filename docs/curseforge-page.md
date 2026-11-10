# Scorched Guns 0.5.5 — unofficial 1.21.1 / NeoForge port

A port of [Scorched Guns](https://www.curseforge.com/minecraft/mc-mods/scorched-guns) by ribs (1.20.1 / Forge) to Minecraft 1.21.1 / NeoForge. The content is the 0.5.5 release. Original mod: https://www.curseforge.com/minecraft/mc-mods/scorched-guns

## What is included
Everything from 0.5.5: guns, attachments, grenades, turrets, exo-suits, raids, blueprints, the gun bench and the machines. 141 guns. Most numbers are the original ones, a few things were changed on purpose - see below.

## What differs from the original
Some of the original mod's unreasonable content was changed, for example the numbers on the bayonet line. One mob was mechanically reworked: the COG knight's charge is now a straight-line dash that does not keep tracking the player, and it steps up while charging so it runs up a block instead of stopping dead at every step. Gun damage, fire rate and ballistics otherwise keep their 0.5.5 values.

## Requirements
Minecraft 1.21.1, NeoForge 21.1.150+, Framework (MrCrayfish) 0.13.11+, GeckoLib 4.9.3+, Curios API 9.5.1+. The last three are required; the game will not start without them.

Optional and only active when installed: JEI, Create and its addons, Immersive Engineering, Mekanism, Sable, Prometheus, Cobweb, Farmer's Delight, Touhou Little Maid (compat is bundled in the jar).

## Installing
Install NeoForge 21.1.150+ for 1.21.1, then drop this mod together with Framework, GeckoLib and Curios into `mods`.

## Known issues
**Sights can show the world through them with shaders on.** Shader packs draw translucent models in a pass that does not write depth. Set `sightsUseCutout = true` in `config/scguns-client.toml`; the lens then reads as solid instead of translucent. Without shaders this does not happen.

**EMF / Fresh Animations compatibility is partial and cannot be fully fixed.** EMF replaces the player model wholesale, so anything that poses the vanilla model stops working underneath it. This build reapplies the held pose after EMF animates the player and suppresses the vanilla first-person hand while a gun is held. The two can still conflict; when an extra arm or a wrong pose shows up, the fix is to drop EMF or the resource pack. An old model can also stick around after swapping a model pack - press F3+T (or restart), that is EMF's cache.

**If something looks broken right after changing resource packs or shaders, reload resources before blaming the mod.**

## Notes
The port was made from the 0.5.5 release jar (decompiled and remapped), not from a source tree. Every trap hit along the way is written down in `docs/HANDOFF.md` in the repository.

The Chinese translation was originally made by ZR4U on mcmod.cn and submitted upstream; this port removes some unsuitable content from it and fills in the entries added since. English strings are unchanged from the original mod.

## License
GPL-3.0, inherited from the original mod, whose assets and data are redistributed here. Original mod by ribs; the port is by ELMO_SYG. Not affiliated with the original author.

## 中文说明
这是 Scorched Guns（原作者 ribs，1.20.1 / Forge）移植到 1.21.1 / NeoForge 的版本，内容取自 0.5.5 发布版。原模组：https://www.curseforge.com/minecraft/mc-mods/scorched-guns

包含 0.5.5 的全部内容：枪械、配件、手雷、炮塔、外骨骼、袭击、蓝图、枪械工作台与机器，共 141 把枪。大部分数值沿用原版，少数改过的见下。

**与原版的差异**：修改了原 mod 里一些不合理的内容，比如刺刀系列物品的数值。机制上大改的只有 COG 骑士：它的冲锋改成了直线冲刺，期间不再持续追踪玩家；冲刺时还会获得额外的抬腿高度，能直接冲上台阶，原来的设定里它会在台阶前停住。其余枪械的伤害、射速、弹道沿用 0.5.5。

**前置**：Minecraft 1.21.1、NeoForge 21.1.150+、Framework 0.13.11+、GeckoLib 4.9.3+、Curios API 9.5.1+。后三个是必需的，缺一个就启动不了。可选联动（装了才生效）：JEI、Create 及其附属、Immersive Engineering、Mekanism、Sable、Prometheus、Cobweb、Farmer's Delight、东方小女仆（兼容模块已内置）。

**安装**：装好 NeoForge 21.1.150+，把本模组与 Framework、GeckoLib、Curios 一起放进 `mods`。

**已知问题**：光影下瞄具会透出后面的世界 —— 光影把半透明模型放在不写深度的单独阶段。在 `config/scguns-client.toml` 里把 `sightsUseCutout` 改成 `true`，代价是镜片从不透明变实心。不装光影没有这个现象。EMF / Fresh Animations 的兼容只能做到一部分、没法真正修好：持枪姿势会在 EMF 动画之后重新套用，第一人称会关掉原版那层手，但两者仍可能冲突，出现多余手臂时只能关掉 EMF 或资源包。换过模型类资源包后出现旧模型残留，按 F3+T 重载资源或重启即可，那是 EMF 自己的缓存。

**几点说明**：本移植从 0.5.5 发布 jar 反编译完成，不是从源码编译。戳过的坑都记在仓库 `docs/HANDOFF.md`。中文翻译最初由 mcmod.cn 用户 ZR4U 制作并提交给原作者，本移植在其基础上删除了一些不适宜内容并补了新增条目。

**授权**：GPL-3.0，沿用原模组，资源与数据来自原模组。原作者 ribs，移植 ELMO_SYG，与原作者没有隶属关系。
