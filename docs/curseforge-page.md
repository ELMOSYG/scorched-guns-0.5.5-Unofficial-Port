# Scorched Guns 0.5.5 — 非官方 1.21.1 / NeoForge 移植

这是 [Scorched Guns](https://www.curseforge.com/minecraft/mc-mods/scorched-guns)（原作者 ribs，1.20.1 / Forge）
移植到 Minecraft 1.21.1 / NeoForge 的版本，内容取自 0.5.5 发布版。

原模组：https://www.curseforge.com/minecraft/mc-mods/scorched-guns

## 包含内容

0.5.5 的枪械、配件、手雷、炮塔、外骨骼、袭击、蓝图、枪械工作台和机器都在，一共 141 把枪。
大部分数值沿用原版，但**有一批东西是改过的**，下面单独列出来。

## 与原版的差异

修改了原 mod 里一些不合理的内容，比如刺刀系列物品的数值。机制上大改的只有 COG 骑士：它的冲锋改成了
直线冲刺，期间不再持续追踪玩家；冲刺时还会获得额外的抬腿高度，能直接冲上台阶，原来的设定里它会在台阶前停住。

其余枪械的伤害、射速、弹道沿用 0.5.5。具体改了什么、为什么改，记在 `docs/changelogs/` 里。

## 前置

| 模组 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.150 或更高 |
| Framework（MrCrayfish） | 0.13.11+ |
| GeckoLib | 4.9.3+ |
| Curios API | 9.5.1+ |

Framework、GeckoLib、Curios 是必需的，缺一个就启动不了。

可选的联动模组（装了才会生效）：JEI、Create 及其附属、Immersive Engineering、Mekanism、Sable、
Prometheus / Soul Fire'd、Cobweb、Farmer's Delight、东方小女仆（jar 里自带兼容模块）。

## 安装

1. 装好 Minecraft 1.21.1 和 NeoForge 21.1.150+。
2. 把本模组、Framework、GeckoLib、Curios 一起放进 `mods` 文件夹。

## 已知问题

**光影下第一人称的瞄具会“透出世界”。** 光影把半透明模型放在单独的渲染阶段，这个阶段不写深度，
瞄准镜的实心部分也能看到后面的方块。可以在 `config/scguns-client.toml` 里把 `sightsUseCutout` 改成
`true`，代价是镜片从半透明变成不透明。不装光影没有这个现象。

**EMF / Fresh Animations 的兼容性只能做到一部分，没法真正修好。** EMF 会整体替换玩家模型，所有依赖原版模型
姿态的做法在它面前都会失效。这个版本做了两件事：持枪姿势在 EMF 动画之后重新套用，第一人称关掉原版那层手。
但两者仍然可能冲突，出现多余手臂或姿势不对时只能关掉 EMF 或换资源包。另外换过模型类资源包后会出现旧模型
残留，按 F3+T 重载资源或重启，那是 EMF 自己的缓存。

**换资源包、换光影之后出现异常，先重载资源再判断是不是模组的问题。**

## 几点说明

这个移植是从 0.5.5 的发布 jar 反编译做的，不是从源码编译，所以内容与原版一致，没有新增物品。
移植过程中踩过的坑、为什么某些行为改成了现在这样，都记在仓库的 `docs/HANDOFF.md` 里，有兴趣可以翻。

中文翻译最初由 mcmod.cn 的用户 ZR4U 制作并提交给了原作者，上游后来更新变慢，我在他的基础上删除了
一些不适宜内容，补了新增的条目。英文文本沿用原模组。

## 授权

本模组沿用原模组的 GPL-3.0，资源与数据来自原模组。原作者 ribs，移植部分由 ELMO_SYG 完成。
和原作者没有隶属关系。

---

# Scorched Guns 0.5.5 — unofficial 1.21.1 / NeoForge port

A port of [Scorched Guns](https://www.curseforge.com/minecraft/mc-mods/scorched-guns) by ribs (1.20.1 / Forge)
to Minecraft 1.21.1 / NeoForge. The content is the 0.5.5 release.

Original mod: https://www.curseforge.com/minecraft/mc-mods/scorched-guns

## What is included

Everything from 0.5.5: the guns, attachments, grenades, turrets, exo-suits, raids, blueprints, the gun bench and the
machines. 141 guns in total. Most of the numbers are the original ones, but a few things were changed on purpose -
they are listed below.

## What differs from the original

Some of the original mod's unreasonable content was changed, for example the numbers on the bayonet line. One mob was
mechanically reworked: the COG knight's charge is now a straight-line dash that does not keep tracking the player, and
it steps up while charging, so it runs up a block instead of stopping dead at every step.

Gun damage, fire rate and ballistics otherwise keep their 0.5.5 values. What was changed and why is recorded in
`docs/changelogs/`.

## Requirements

Minecraft 1.21.1, NeoForge 21.1.150+, Framework (MrCrayfish) 0.13.11+, GeckoLib 4.9.3+ and Curios API 9.5.1+.
The last three are required; the game will not start without them.

Optional and only active when installed: JEI, Create (and its addons), Immersive Engineering, Mekanism, Sable,
Prometheus / Soul Fire'd, Cobweb, Farmer's Delight, Touhou Little Maid (compat is bundled in the jar).

## Installing

Install NeoForge 21.1.150+ for 1.21.1, then drop this mod together with Framework, GeckoLib and Curios into `mods`.

## Known issues

**Sights can show the world through them when shaders are on.** Shader packs draw translucent models in a separate
pass that does not write depth, so the solid part of a scope stops occluding. Set `sightsUseCutout = true` in
`config/scguns-client.toml` to force alpha-cutout rendering; the lens then reads as solid instead of translucent.
Without shaders this does not happen.

**EMF / Fresh Animations compatibility is partial and cannot be fully fixed.** EMF replaces the player model
wholesale, and anything that poses the vanilla model stops working underneath it. This build does two things: the held
pose is reapplied after EMF animates the player, and the vanilla first-person hand is suppressed while a gun is held.
The two can still conflict, and when an extra arm or a wrong pose shows up the only fix is to drop EMF or the resource
pack. Separately, an old model can stick around after swapping a model pack - press F3+T (or restart), that is EMF
caching the models it loads.

**If something looks broken right after changing resource packs or shaders, reload resources before blaming the mod.**

## Notes

The port was made from the 0.5.5 release jar (decompiled and remapped), not from a source tree. Every trap hit along
the way, and why a given behaviour works the way it does, is written down in `docs/HANDOFF.md` in the repository.

The Chinese translation was originally made by ZR4U on mcmod.cn and submitted upstream; upstream slowed down, so
this port removes some unsuitable content from it and fills in the entries added since. English
strings are unchanged from the original mod.

## License

GPL-3.0, inherited from the original mod, whose assets and data are redistributed here. Original mod by ribs; the
port is by ELMO_SYG. Not affiliated with the original author.
