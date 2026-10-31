# 已知问题与已评估方案

这份文件记录**已知但尚未修**的问题，以及每个问题评估过的方案与取舍。
修掉一条就删一条，别让它变成第二个 HANDOFF。

---

## 1. 枪械在铁砧修复会越修越贵，而砂轮虽然能重置却会洗掉附魔

**影响**：高。这是"装备会一步步失去维护能力"的问题，而且原版那个解法有代价。

### 机制

原版铁砧每次操作（修复 / 合并 / 改名）都会给物品的 `REPAIR_COST` 组件 +2，
花费按 2^n 量级上涨，超过 39 级就显示 "Too Expensive!"。枪械只有铁砧这一条修复路径
（`GunItem.isValidRepairItem`），所以会稳定累积到修不了。

### 砂轮能重置惩罚，但会清掉枪械附魔（已实测确认）

原版 `GrindstoneMenu` 生成输出时会把 `REPAIR_COST` 清掉，所以"过一遍砂轮"确实能重置修理成本。

**但同一趟操作也会剥掉附魔，而本 mod 的附魔就存在原版 `ENCHANTMENTS` 组件里**：

```java
// ScEnchants
ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
```

所以砂轮**不是**免费解法：重置换来的是"把这把枪的附魔全部洗掉"，而枪械附魔本身很贵
（附魔台、附魔书、经验）。这一条修正了早期文档里"砂轮安全"的错误说法。

### 现在的候选方案

| 方案 | 做法 | 代价 | 评价 |
|---|---|---|---|
| **① 专属维修途径**（推荐） | 走 mod 自己的机器（枪械台 / 维修套件配方）修复，不动 `REPAIR_COST` | 低～中，需加一条配方 | **不牺牲附魔，也不改原版逻辑 ✓** |
| **② 周期性清 `REPAIR_COST`** | 每 40 tick 扫背包清理 | 每 tick 约 40 ns（0.0001%） | 有效，但等于取消铁砧惩罚这一步玩法 |
| **③ tooltip 提示** | 提醒"修太贵可以过砂轮，但会洗掉附魔" | 低 | 只能当应急说明，不算解决 |
| **④ 改砂轮行为** | 让砂轮只清惩罚不清 mod 附魔 | 中，要 mixin 原版菜单 | 改动原版逻辑，风险高于收益 |

### 结论

优先 **①（专属维修途径）**：既保住附魔，又不抹掉原版铁砧的经济约束。
② 保留为配置可选项。③ 作为兜底说明。**不要再用砂轮当解法**——代价是附魔。

---

## 2. 机瞄相机骨骼会有固定偏移

**影响**：中（只影响用了相机骨骼的枪；原版 141 把不受影响）。

1.20.1 的 `GUN_DEFAULT_ORIGIN` 与移植版不同（字节码里能看到 `8.0` 常量），
而 `getCameraBone` 用的是写死的常量，加上骨骼分支绕过了原路径的补偿
（`+0.6059` 等），叠加起来表现为"相机比机瞄低约 2 格"。

**方案**：把骨骼分支**降级为兜底**（有 `ironSight.camera` 数据时优先用数据），
这样已发布行为完全不变；自定义枪改用数据键定位。

---

## 3. Create 动力合成与枪械工作台的材料不一致

**影响**：中（有 Create 的整合包里能绕过蓝图与部分材料）。

实测：137 把枪两套配方都有，其中 **90 把不一致**——22 把缺蓝图（古典/边疆可接受），
87 把材料不同，其中 78 把是"动力合成少料"（缺枪管、框架、木板、等级金属）。

**状态**：**暂不修**。用户判断"AI 生成配方布局精确度不够"，应由人工改。
需要时用只读脚本输出"哪把枪少了什么"的清单即可。

---

## 4. 1.20.1 优化 mod 未收尾

`E:\mod\scguns-optimizer`，5 个 mixin 全部生效（实测约 10% 提升），
但还留着反射打印日志，没有 README。


## Bullet holes on a rotated physics structure face the wrong way

A hole fired at a **Sable** sub-level that is tilted or rotated lands on the wrong plane. On an ordinary block it is
correct, which is why this only shows up with physics mods installed.

The decal's orientation is stored as a `Direction`, i.e. one of six axes:

```java
public class BulletHoleData implements ParticleOptions {
   private final Direction direction;   // east / west / north / south / up / down, nothing else
   private final BlockPos pos;
}
```

A rotated structure's hit face is not axis-aligned, so the hole is snapped to the nearest axis and ends up on a
different surface than the one that was shot. The impact **impulse** had the same kind of fault and is already fixed:
the direction was being pushed through `normalToWorld` even though it was already in world space, so a rotated
structure was pushed in the wrong direction (`6255e199`).

### What it would take

Five steps, in this order, building after each one:

1. `BulletHoleData`: add `nx`/`ny`/`nz` next to the existing fields and keep the old two-argument constructor, so
   nothing else has to change yet.
2. `ModParticleTypes`: stop letting `BULLET_HOLE_CODEC` list its own fields. **There are two copies of this codec** -
   one here and one in `BulletHoleData.CODEC` - and they drift apart the moment only one is updated. Reuse the one in
   `BulletHoleData` instead of restating the fields.
3. `S2CMessageProjectileHitBlock`: carry the flight direction as an optional `Vec3`, keep the five-argument
   constructor so the seven existing call sites keep working, and pass the direction from
   `ProjectileEntity` where it is available.
4. `ClientPlayHandler`: build the normal as the inverted flight direction (falling back to the hit face), offset the
   decal along that normal instead of along a face step, and hand it to the particle.
5. `BulletHoleParticle#render`: replace `direction.getRotation()` with
   `new Quaternionf().rotationTo(0, 0, 1 -> normal)`.

The reason this was not done in the 1.0.8 cycle: the four edits touch a network message, a particle codec and a
constructor ordering (`super()` must be the first statement, so the normal has to be assigned after it). Getting it
wrong does not fail loudly at runtime - it silently draws holes on the wrong plane - so it needs a client to look at.
