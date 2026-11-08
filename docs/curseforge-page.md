# Scorched Guns 0.5.5 — 非官方 1.21.1 / NeoForge 移植

这是 [Scorched Guns](https://www.curseforge.com/minecraft/mc-mods/scorched-guns)（原作者 ribs，1.20.1 / Forge）
移植到 Minecraft 1.21.1 / NeoForge 的版本，内容取自 0.5.5 发布版，不添加改动平衡性的东西。

原模组：https://www.curseforge.com/minecraft/mc-mods/scorched-guns

## 包含内容

0.5.5 的枪械、配件、手雷、炮塔、外骨骼、袭击、蓝图、枪械工作台和机器都在，一共 141 把枪。
如果你玩过 1.20.1 版本，数值和手感是同一个版本，没有重新做平衡。

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

**装了 EMF / Fresh Animations 时，第三人称持枪动作可能不对。** 从 1.0.9.2 hotfix 起已经做了适配：
持枪姿势会在 EMF 动画之后重新套用，第一人称也会关掉原版那层手。如果换过模型类资源包之后出现旧模型残留，
按 F3+T 重载资源（或者重启），EMF 会缓存资源包里的模型。

**换资源包、换光影之后出现异常，先重载资源再判断是不是模组的问题。**

## 几点说明

这个移植是从 0.5.5 的发布 jar 反编译做的，不是从源码编译，所以内容与原版一致，没有新增物品。
移植过程中踩过的坑、为什么某些行为改成了现在这样，都记在仓库的 `docs/HANDOFF.md` 里，有兴趣可以翻。

中文翻译最初由 mcmod.cn 的用户 ZR4U 制作并提交给了原作者，上游后来更新变慢，我在他的基础上删掉了一部分
梗和脏话，补了新增的条目。英文文本沿用原模组。

## 授权

本模组沿用原模组的 GPL-3.0，资源与数据来自原模组。原作者 ribs，移植部分由 ELMO_SYG 完成。
和原作者没有隶属关系。

---

# Scorched Guns 0.5.5 — unofficial 1.21.1 / NeoForge port

A port of [Scorched Guns](https://www.curseforge.com/minecraft/mc-mods/scorched-guns) by ribs (1.20.1 / Forge)
to Minecraft 1.21.1 / NeoForge. The content is the 0.5.5 release; nothing was rebalanced for this port.

Original mod: https://www.curseforge.com/minecraft/mc-mods/scorched-guns

## What is included

Everything from 0.5.5: the guns, attachments, grenades, turrets, exo-suits, raids, blueprints, the gun bench and the
machines. 141 guns in total, with the numbers you remember from the 1.20.1 version.

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

**With EMF / Fresh Animations installed, the third-person gun pose can look wrong.** Since 1.0.9.2 hotfix the mod
reapplies the held pose after EMF animates the player, and suppresses the vanilla first-person hand while a gun is
held. If an old model sticks around after swapping a model pack, press F3+T (or restart): EMF caches the models it
loads from resource packs.

**If something looks broken right after changing resource packs or shaders, reload resources before blaming the mod.**

## Notes

The port was made from the 0.5.5 release jar (decompiled and remapped), not from a source tree. Every trap hit along
the way, and why a given behaviour works the way it does, is written down in `docs/HANDOFF.md` in the repository.

The Chinese translation was originally made by ZR4U on mcmod.cn and submitted upstream; upstream slowed down, so
this port removes some of the meme lines and strong language from it and fills in the entries added since. English
strings are unchanged from the original mod.

## License

GPL-3.0, inherited from the original mod, whose assets and data are redistributed here. Original mod by ribs; the
port is by ELMO_SYG. Not affiliated with the original author.
