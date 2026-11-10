# 女仆兼容：1.20.1 → 1.21.1 同步清单

> 给负责 1.21.1 / NeoForge 侧（`scguns-0.5.5` 移植版内置的 `scg2_maid_compat`）的 agent。
> 本文来自 1.20.1 分支（`E:\mod\SCG2_TLM\forge-1.20.1-47.4.10-mdk`）的实际改动与实测结论。
> 目标：1.21.1 内置版本（当前 **1.2.3**）与 1.20.1 行为一致，并修掉两个已确认的 bug。

---

## 0. 版本与状态

| 项 | 值 |
|---|---|
| 1.20.1 分支产物 | `scg2_maid_compat-1.2.1.jar`，**174464 B / SHA-256 前 16 位 `40FB61F69B8DD370` / 2026-11-10 17:30:51** |
| 1.21.1 内置版本 | `scg2_maid_compat-neoforge-1.21.1-1.2.3`（Jar-in-Jar，位于移植版 `META-INF/jarjar/`）|
| 1.21.1 已发布更新日志 | 1.2.2（气枪空气、配件槽、马格南归手枪、射程 48/32/20、键名 `rifle_search_radius`）|
| 建议下一个版本号 | **1.2.4**（本文 1、3 节改完后）|

> 注意：1.20.1 的配置键 `rifle_magnum_search_radius` 已改名为 `rifle_search_radius`，
> 与 1.21.1 一致；旧 TOML 里的旧键值会被忽略（重新生成默认值 32）。

---

## 1. 必须改（有 bug / 两边行为不一致）

### 1.1 弓 / 弩女仆被我们的走位逻辑污染（已确认，1.20.1 已修）

**现象**：弓兵、弩兵会一路冲到敌人脸上再拉弓，不再保持距离。

**根因**：`MaidAttackStrafingTaskMixin`（mixin `MaidAttackStrafingTask`）在 `tick` 的 HEAD 处
**无条件 `ci.cancel()`**，用我们自己的枪械走位替代——而 `MaidAttackStrafingTask` 是 **TLM 弓/弩攻击共用的行为**。
我们的走位在 `dist > searchRadius()*0.5` 时朝前推进（约 2.5 格），于是中距离一路往上顶。

**修法**（1.20.1 已落地，1.21.1 请照抄）：

```java
@Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
private void scg2tlm$onTick(ServerLevel level, EntityMaid maid, long gameTime, CallbackInfo ci) {
    // 只在手持 SC2 枪械时接管；弓/弩直接 return（不 cancel），让 TLM 原 tick 继续跑
    if (!SC2GunCompat.isSC2Gun(maid.getMainHandItem())) {
        return;
    }
    ...
}
```

**验证**：弓兵/弩兵在 10~20 格保持距离并绕行射击；枪械女仆走位不变。

### 1.2 坐下射击出现「双发扳机」（**用户决定暂不修**，仅保留诊断与修法备查）

> **状态（2026-11-10）**：影响不大 —— 只在有敌人、且射击途中坐下时出现，目标丢失即恢复，故**暂不修**。
> 原版狼也有同类毛病（坐下只是一个姿态/移动标志，不会关掉攻击目标），可作参照。
> 以后若要修，按本节两步走即可；在此之前**不要**为它单独改开火流程。

**现象**：女仆在射击途中坐下，会出现一次扳机打两发。

**诊断**：`MaidSC2GunShootTask` 是四个战斗行为里**唯一没有 `isMaidInSittingPose()` 检查**的
（`MaidSC2GunWalkToTarget` / `SC2GunStrafingTask` / `MaidSC2GunIdleReloadTask` / `MedicHealTask` 都有）。
TLM 坐下会切换 brain activity ⇒ 普通攻击行为与坐下状态下的攻击行为（同一行为类的两个实例）可能同时 RUNNING ⇒ 同 tick 各开一枪。
另外，共享的「按女仆最小开火间隔」(`SCG2TLM$LAST_FIRE`, `MIN_FIRE_GAP = 1`) 检查位置太靠后：
被抑制的实例在此之前可能已经改过枪的 NBT（`scg2tlm:burst_shot` 连发计数、`AmmoCount`、音效/抛壳），
所以即使子弹被挡掉，连发状态也已经被搅乱。

**补充（用户现场观察，方向已确认）**：这个 bug **只在有敌人时出现，敌人丢失后自动恢复正常**。
因为两个实例都靠 `ATTACK_TARGET` 驱动：目标存在 ⇒ 坐姿射击逻辑与站姿射击逻辑（同一行为类的两份实例）
同时活跃 ⇒ 双发窗口存在；目标消失 ⇒ 其中一份停 ⇒ 恢复单实例。
这进一步说明它不是「连发计数写错」，而是**两个行为实例并存**。

**改代码之前先做这一步确认**（1.21.1 侧建议）：在 `MaidSC2GunShootTask` 的开火处临时打一行日志，
打印 `maid.getBrain().getActiveActivities()`、`gameTime`、`System.identityHashCode(this)`：
- 同一个 `gameTime` 出现两次开火、`identityHashCode` 不同 ⇒ 实锤是双实例；
- 两次的 activity 分别是什么（`WORK` / `RIDE_WORK` / 坐姿那个活动）就能定位是哪一处注册了第二份。

确认后再按下面两步修最稳（不确认也可以直接改，但改动效果不好归因）。

**修法（两步都要）**：

1. `MaidSC2GunShootTask.checkExtraStartConditions` 与 `canStillUse` 各加一行：
   ```java
   if (maid.isMaidInSittingPose() || maid.isPassenger()) return false;
   ```
2. 把共享的「最小开火间隔」检查**提到开火流程最前面**——在任何 `AmmoCount` / `burst_shot` / 音效 / 抛壳
   之前判定并 `return`，被抑制的实例一个字节都不许碰枪的 NBT。

**验证**：射击途中坐下/起身反复若干次，不应出现单次扳机两发；连发枪的 burst 计数不应因切状态而重置。

### 1.3 气枪扣费口径：确认是「每发」而不是「每弹丸」

1.2.2 的更新日志已声称「charged once per shot rather than once per projectile」，但代码里
`MaidProjectileHandler.onEntityJoinLevel`（弹丸进入世界时扣费）看起来仍是按**弹丸**扣。
请核对当前源码：

- 若扣费仍在 `EntityJoinLevelEvent` 里 ⇒ 霰弹枪一枪多弹丸会被扣多次；
  正确做法是把扣费移到**开火前**（`performGunAttack` 里，`AmmoCount` 判定之后、生成弹丸之前），
  弹丸侧只保留只读兜底（拿不到空气就取消弹丸，但不要再扣）。
- 若已经改成按发扣 ⇒ 保持，并在注释里写明「per shot」的原因。

1.20.1 的参考实现（可直接对照）：

```java
// SC2GunCompat.performGunAttack：开火前扣一次
if (mob instanceof EntityMaid airMaid && !maidAir(airMaid, gun, true)) {
    return rate;
}
```

---

## 2. 对齐项（1.20.1 已与 1.21.1 一致，请核对不要回退）

| 项 | 1.20.1 现状 | 备注 |
|---|---|---|
| 马格南归类 | `case pistol, magnum -> PISTOL_SEARCH_RADIUS`（**16 格**）| 不再属于步枪类 |
| 默认射程 | sniper **48** / rifle **32** / smg **20** / pistol 16 / shotgun 16 / flamethrower 10 | 键名见下 |
| 配置键 | `sniper_search_radius`、**`rifle_search_radius`**、`smg_shotgun_pistol_search_radius` | 旧名 `rifle_magnum_search_radius` 已废弃 |
| 配件槽（Curios）扫描 | 气瓶、弹药盒、创造盒都算；**可选机制**：`try/catch (Throwable)`，方法签名里不出现 Curios 类型 | 未装 Curios 时返回空表、不崩 |
| 气枪无空气 | `cannotFight()` 把它当「没弹药」⇒ 不选目标、不开火、不耗弹；有刺刀仍可近战 | 「不换弹」仅战斗路径覆盖，空闲补弹仍会执行（不耗空气，无害）|
| 条件行 | 新增 `has_air`（**显示用**，非枪械恒 ✓）| 语言键 `task.scg2_maid_compat.gun_attack.condition.has_air` |

1.20.1 参考实现位置（文件：`com/scg2tlm/elmomod/compat/SC2GunCompat.java`）：

- `maidAir(maid, gun, consume)` —— 装备栏 + 背包 + Curios；只认 `AirCanisterItem` 的 Forge 能量
- `airCostOf(gun)` —— 读 `getModifiedGun(gun).getProjectile().getEnergyUse()`
- `tryConsumeAir(stack, cost, consume)` —— `IEnergyStorage.extractEnergy(cost, consume)`
- `hasAirForGun(maid, gun)` —— 只问不扣
- `curioItems(maid)` —— Curios 配件槽（可选机制，`catch (Throwable)` 降级）

---

## 3. 1.21.1 已有、1.20.1 没有（保持 1.21.1 的现状即可）

1. **Create 背罐**：1.21.1 支持（`BacktankUtil`），1.20.1 未做（没有 Create 依赖）。
   请**保持支持**，并确认两点：① `getAir` 与 `consumeAir` 操作同一份存储；② 扣费口径与 1.3 一致（每发一次）。
2. **配置界面文案移入语言文件**：1.21.1 已完成，1.20.1 仍是内联字符串。
   如果要在 1.20.1 侧也对齐，需要把 `SCG2TLMClothConfig` 里的 `Component.literal(...)` 换成语言键。

---

## 4. 不要破坏的兼容面

移植版 scguns 的类/数据面见同目录 `PORTING_TOOLCHAIN.md` §4（类型、getter、NBT 键、枚举、工具方法、tag）。
本兼容层直接依赖的关键点（改动会导致编译失败或行为错乱）：

- `SC2GunCompat.isSC2Gun` → `stack.getItem() instanceof GunItem`
- `Gun.Reloads` 的 `reloadTimer` / `maxAmmo` / `reloadType`；`ReloadType.{MANUAL, MAG_FED, SINGLE_ITEM}`
- NBT：`AmmoCount` / `PrevAmmoCount` / `HeatLevel` / `RechargeCounter` / `IsShooting` / `scg2tlm:burst_shot`
- `Gun.Projectile.getEnergyUse()`、`top.ribs.scguns.item.AirCanisterItem`、`top.ribs.scguns.interfaces.IAirGun`
- `AIGunEvent.performGunAttack`、`GunEnchantmentHelper.getRate/getQuickHands`、`GunModifierHelper.getModifiedReloadSpeed/getModifiedAmmoCapacity`

---

## 5. 构建与验证

**构建**（本机经验，1.20.1 侧；1.21.1 侧按其 ModDevGradle 环境为准）：

```powershell
cd <工程根>
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-17.0.10.7-hotspot'   # 1.21.1 用 JDK 21
.\gradlew.bat build --offline --no-daemon
```

- **必须加 `--no-daemon`**；本环境偶发卡死在 `single-use Daemon process will be forked` 之后（与代码无关）。
  卡住时：`gradlew --stop` → 删工程 `.gradle\*.lock` → 重跑。
- 依赖别用 `curse.maven:`：CurseForge 的 deobf 缓存会被清空，`--offline` 下必然失败。
  1.20.1 已改为 `libs/` 本地 jar（`flatDir`）：`fg.deobf('local:ScorchedGuns:0.5.5-1.20.1')` 等。

**进游戏验证清单**：

1. 弓兵/弩兵在 10~20 格保持距离绕行射击（1.1）
2. 射击途中坐下/起身，不出现单次扳机两发（1.2）
3. 气枪：每发只扣一次空气（霰弹枪多弹丸不叠加扣费）（1.3）
4. 气瓶/弹药盒/创造盒挂在 **Curios 配件槽**也能被识别
5. 拿掉所有气瓶 ⇒ 女仆不追、不开火、不空耗弹药；放回后恢复
6. 马格南的索敌距离与手枪一致（16 格）；狙击/步枪/SMG 分别 48/32/20

---

## 6. 已知未验证 / 风险

- 1.2 的坐下双发修复**尚未实测**（1.20.1 侧也未实现，仅诊断）。
- `has_air` 条件行是**显示用**（TLM `isEnable` 默认 true），不参与「能不能接这个工作模式」。
- 1.20.1 没有 Create 背罐支持；两边若同时维护，注意扣费口径不要分叉。
- 1.2.2 的更新日志与代码是否完全一致（尤其 1.3 的扣费点）需要现场核对。
