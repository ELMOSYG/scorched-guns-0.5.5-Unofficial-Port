# WW1 附属：固定炮塔在 Sable 物理结构上的坐标系问题

给 AsYasya's WW1 Weaponry（`scgunsww1`）作者的补丁说明。等源码仓库建好后可直接改成 PR。

## 现象

玩家手操的固定机枪放在 Sable 子层上时：

- 炮塔**模型**相对方块偏移（结构一旦旋转就明显）
- 开火时**弹着点**固定朝某个方向飞，不跟随准星

在普通地面上一切正常。

## 原因

`net.roka.scgunsww1.block.turret.TurretBlockEntity#fire(ServerPlayer, BlockPos)` 里，
枪口位置由方块自身的 `getBlockPos()` 加上 `muzzleHeight` / `muzzleReach` 算出，
而瞄准方向来自座位上的玩家（`getEyePosition()` / `getLookAngle()`），两者相减得到方向：

```
muzzle  = blockPos + (0, muzzleHeight, 0) + look * muzzleReach    ← 子层 plot 坐标（千万级）
aim     = player.getEyePosition() + look * something              ← 世界坐标
方向     = (aim - muzzle).normalize()                              ← 两个空间相减，结果无意义
```

子层里的方块坐标是 plot 坐标，玩家是世界坐标，相减得到的是一个**常量向量**，所以子弹固定朝一个方向。

`TurretBlockRenderer` 用同样的坐标画模型，所以模型也偏。

## 修法

在把枪口位置交给后续计算**之前**换到世界空间。scguns 公开了换算方法，且未安装 Sable 时是空操作：

```java
PhysicsStructureHelper.toWorld(Level, BlockPos, Vec3 localPoint)
PhysicsStructureHelper.toLocal(Level, BlockPos, Vec3 worldPoint)
PhysicsStructureHelper.normalToWorld(Level, BlockPos, Vec3 localDir)
```

`fire()` 里大致是这样改：

```java
// 之前
Vec3 muzzle = new Vec3(
    this.worldPosition.getX() + 0.5,
    this.worldPosition.getY() + definition.muzzleHeight(),
    this.worldPosition.getZ() + 0.5
).add(look.scale(definition.muzzleReach()));

// 之后
Vec3 muzzleLocal = new Vec3(
    this.worldPosition.getX() + 0.5,
    this.worldPosition.getY() + definition.muzzleHeight(),
    this.worldPosition.getZ() + 0.5
).add(look.scale(definition.muzzleReach()));
Vec3 muzzle = PhysicsStructureHelper.toWorld(this.level, this.worldPosition, muzzleLocal);
```

两点注意：

1. `look` 已经是世界方向，**不要**再送进 `normalToWorld` 转一次。正确做法是先把它加进 `muzzleLocal`，
   再整体 `toWorld`。
2. `clientTurretAim()` 也要同样处理，否则子弹对了、准星还是偏。
3. `TurretBlockRenderer` 的模型变换同样要换算，否则模型仍然偏离方块。

## 参照

scguns 自己的炮塔（`TurretBlockEntity` / `EnemyTurretBlockEntity`）就是这么做的：
枪口用 `toWorld(...)`，预判目标用 `toLocal(...)`，渲染器也一样，因此在子层上位置与瞄准都正确。

## 我们这边做过什么

试过在 scguns 侧用 mixin `@Redirect` 拦截 `fire()` 里的 `Level.addFreshEntity`，
在子弹生成后修正位置与速度 —— **无效**，因为方向在更早的减法里就已经错了。
所以这个问题只能由附属自己修，或者由附属暴露一个可注入的点。

## 环境

- Minecraft 1.21.1 / NeoForge 21.1.250
- scguns 1.0.9.1（本移植版）
- Sable 2.0.6
- scgunsww1 1.1.1-r
