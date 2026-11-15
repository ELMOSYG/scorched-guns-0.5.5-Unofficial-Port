# 换弹系统（1.21.1 移植版）逻辑梳理

> 结论先说：这不是“一个状态机”，而是**三个状态机 + 四个写入者 + 一个会脱钩的 NBT 层**。
> 任何局部修补都会在别处产生反效果，动手前请先读第 5 节的修复顺序。

## 1. 涉及的文件

| 层 | 文件 | 职责 |
|---|---|---|
| 网络 C→S | `C2SMessageReload` | 开始换弹，写入两套标记 |
| | `C2SMessageGunLoaded` | “装填结果”，会 `new ReloadTracker(player)` |
| | `C2SMessageUnload` | 卸载弹药 |
| | `C2SMessageSwapAmmo` | 切换弹药类型，同时置换弹标记 |
| | `C2SMessageEjectCasing` | 弹壳产出，**不检查任何换弹状态** |
| 网络 S→C | `S2CMessageReload(reloading)` | 客户端切换显示状态 |
| | `S2CMessageStopReload` | 强制停止（换枪时） |
| | `S2CMessageUpdateAmmo` | 同步弹数 |
| 服务端状态机 | `ReloadTracker`（666 行） | 唯一真正执行装填的流程 |
| | `ServerPlayHandler.handleUnload` | 卸载 |
| 客户端 | `ReloadHandler`（约 400 行） | 计时、触发、状态回收 |
| | `ClientPlayHandler.handleReloadState` | 收 S2C 后改本地状态 |
| | `AnimatedGunItem`（12 处相关） | 动画控制器读 NBT |
| 其它写入者 | `ShootingHandler`、`MeleeAttackHandler`、`WeaponMovementEventHandler`、`GunEventBus` | 各自会清换弹状态 |

## 2. 三套状态

| 状态 | 写入 | 清除 | 读取 |
|---|---|---|---|
| `ModSyncedDataKeys.RELOADING`（同步键） | `C2SMessageReload`、`ReloadTracker` | `ReloadTracker`（8 处）、`ShootingHandler`、`MeleeAttackHandler`、换枪 | `handleUnload`、射击门控、移动禁射 |
| NBT `IsReloading` | 同上 | 同上 + `ReloadTracker.loaded()` | `ReloadHandler`（计时）、`GunReloadState` |
| NBT `scguns:IsReloading` | 同上 | `AnimatedGunItem`（动画结束） | `AnimatedGunItem`（动画） |

**关键**：第三个键不只是镜像，它控制动画是否继续播放。提前清理会让动画重播，
因此换弹时长会随机快慢（2026-11-15 的 `ReloadState` 统一尝试就是因为这一点被回退）。

## 3. 一次换弹的时间线

```
[客户端] 按 R
   → ReloadHandler 判定（有弹药、枪不空…）
   → 本地 startReloadTick = tickCount + 5（延迟窗口）
   → 发 C2SMessageReload
[服务端] C2SMessageReload.handle
   → RELOADING = true；IsReloading / scguns:IsReloading = true
   → 回发 S2CMessageReload(true)
[客户端] 收到 → handleReloadState(true) → 动画开始
[服务端] 每 tick ReloadTracker.onPlayerTick
   → 校验：换枪？弹药耗尽？已满？
   → 未满 → increaseMagAmmo / increaseAmmo / reloadItem（按 ReloadType）
   → 满或无弹 → 结束：清状态 + S2CMessageUpdateAmmo
   → 停顿类换弹由 InReloadLoop / ReloadState / ShouldStopAfterLoop 控制
[客户端] ReloadHandler 轮询 NBT
   → 看到 scguns:IsReloading 消失 → 播收尾动画 → 允许射击
```

## 4. 三处结构性缺陷

1. **三套状态没有权威**：一处被提前清、另一处还在，装填仍在跑而界面认为已结束。
   直接后果：换弹中卸载会吞光背包弹药、无限装弹；换弹时长漂移。
2. **弹容 `AmmoCount` 的写入者不唯一**：`ReloadTracker`、`handleUnload`、`BeamWeaponHandler`、
   创造弹药箱都会写，互相覆盖。“卸载不扣弹却给了弹”就是两个写入者错位。
3. **`NbtHelper.getTagForWrite` 返回的 tag 可能脱钩**：任何“先取 tag → 中间动别的数据 → 回来写”
   的代码都会写进副本。`ReloadState.cancel` 重写 CustomData 之后，`handleUnload` 里那个旧 tag
   就是这种情况，导致弹匣没清零而子弹已发出。

## 5. 修复顺序（必须按此顺序）

| 阶段 | 内容 | 风险 |
|---|---|---|
| 1 | 把 `AmmoCount` 读写全部收进一个 helper（`GunAmmo.get / set`），内部每次 `getTagForWrite` + `setTag` 写回 | 低（纯机械） |
| 2 | `handleUnload` 改成“先扣弹匣再发物品”，顺序反了就会出现“弹匣没扣、物品已发” | 低 |
| 3 | 换弹状态只保留 `RELOADING` 做判定；两个 NBT 键只允许 `AnimatedGunItem` 写，且**不改它们的清除时机** | 中 |

## 6. 已回退的尝试（不要重复）

| 提交 | 做法 | 失败原因 |
|---|---|---|
| `26414f45` … `75085608` | 在退弹包/卸载包加多层拦截、`ScgunsUnloadedMag` 标记 | 三套状态不稳，拦截反而让“打空后换弹不产壳” |
| `d19cabd5` … `e062664f` | `ReloadState` 统一三个写入点 | 提前移除 `scguns:IsReloading`，动画重播，换弹时长随机 |

备份：标签 `backup-reloadstate-20261115`（= `e062664f`），文件副本在 `E:\mod\backup-reloadstate-20261115`。

## 7. 给接手者的一句话

~~先做第 5 节的阶段 1（弹药读写 helper），否则只会在别处制造新的怪现象。~~
阶段 1 已完成（见第 8 节），下一步是阶段 3 的"谁有权写状态"。

---

## 8. 阶段 1 已完成（2026-11-15）：`AmmoCount` 收进 `GunAmmo`

### 8.1 做了什么

`src/main/java/top/ribs/scguns/util/GunAmmo.java` 现在是这个键的唯一持有者：

| 方法 | 语义 |
|---|---|
| `get(ItemStack)` / `get(CompoundTag)` | 只读；没有 tag 读作 0；**不创建、不复制组件** |
| `has(...)` | 键真的存在（= 0.5.5 的 `tag.contains("AmmoCount", 3)`） |
| `hasCustomData(ItemStack)` | 0.5.5 每个写点外面的 `tag != null` 守卫 |
| `set(stack, n)` | 复制 → 改 → 写回；**没有 tag 时不写**，返回 false |
| `setOrCreate(stack, n)` | 同上，但允许创建（创造页 / 生物装备 / 附属 mod） |

21 个文件改成调用它，`"AmmoCount"` 字面量只剩 `GunAmmo.java` 一处。三个读访问器
（`Gun.getAmmoCount`、`GunAttackGoal.getAmmoCount`、`ReloadTracker.isWeaponFull`）不再用
`getOrCreateTag`：那是每 tick / 每帧一次的组件复制，而且会**让调用方手里的 tag 脱钩**。

**为什么"没有 tag 就不写"必须保留**：`ReloadTracker.increaseMagAmmo` 这类写点，缩弹药池的调用就在
同一个守卫里。把守卫去掉会让一把没有 tag 的枪**既不装弹也不扣弹**变成"扣了弹却没装"——比现在更糟。
所以 `set` 返回布尔值，调用方据此决定是否吞弹药。

### 8.2 顺手暴露并修掉的两处（都是第 4 节第 3 类缺陷）

1. **`C2SMessageGunLoaded`**：`tag` 在 `increaseMagAmmo / reloadItem / increaseAmmo` **之前**取，
   写完弹匣后那个 tag 已脱钩 ⇒ ① 发给客户端的 `S2CMessageUpdateAmmo` 用的是**换弹前**的弹数；
   ② 三条 `remove("IsReloading"/"scguns:IsReloading"/"InCriticalReloadPhase")` 全写进孤儿。
   现在："写完弹匣 → 重新取 tag → 清标记；发报时读新值"（`clearReloadFlags` 每次自己取 tag）。
2. **`ReloadTracker.loaded`**：同一形状（`increaseAmmo` 之后 `tag.remove("IsReloading")`）。
   抽成 `clearIsReloading(stack)`，每次重新取 tag 并 `setTag`。实际影响很小：`RELOADING` 已置
   false，下一 tick 的 else 分支本来也会清掉它。

### 8.3 **故意没动**的地方（阶段 3 的议题，别顺手改）

- `AnimatedGunItem.handleReloadingState`（约 810 行）的那个 `getOrCreateTag` **保留**。它今天的
  副作用（把调用方的 tag 脱钩）恰好让这个**客户端 MANUAL 状态机**写不进物品，于是不会与服务端争
  同一个 `scguns:ReloadState`。改成只读会让两套状态机同时写 —— 正是第 4 节的缺陷 1。源码里留了注释。
- 清除时机、`ReloadState` / `IsPlayingReloadStop` 的语义、`C2SMessageReload` 的 MANUAL 分支：**未改**。
  顺带发现 `C2SMessageReload` 第 73 行先 `remove("scguns:IsReloading")`、第 77 行又读它 ⇒
  那个 `IsPlayingReloadStop` / `STOPPING` 分支**恒不成立**（死分支）。这是状态机顺序问题，属阶段 3。

### 8.4 防复发

| 工具 | 查什么 |
|---|---|
| `tools/audit_ammo_writer.py`（`--selftest`） | ① 键字面量只能在 `GunAmmo.java`；② 两个读访问器必须委托 `GunAmmo.get`；③ `set` 必须保留 `hasCustomData` 守卫、并经 `NbtHelper.setInt` 写回 |
| `verify_installed_jar`（5 项新/改） | 字节码层：`AmmoCount` 的常量池条目**只在 `GunAmmo.class`**；`Gun` / `ReloadTracker` / `ServerPlayHandler` / `C2SMessageGunLoaded` / `EntityEquipmentConfig$EquipmentEntry` / `GunnerMobSpawner` 都引用它 |

**反向验证**：上一版 jar（未含 helper）在当时的 296 项里实测 **289/296**（7 项 FAIL，全是这批新/改的
检查）；加完 §83.12 的战利品表条件检查后总数 297，新 jar **297/297**（另一版核对：296/297 → 297/297）。
`audit_ammo_writer.py --selftest` 在标签 `backup-reloadstate-20261115` 上实测报出 13 处字面量 +
2 个不委托的读访问器。

> 检查的**写法**本身很重要：字节码里 `getAmmoCount` 含 `AmmoCount` 这个子串，所以裸搜文本会把
> 所有调用读访问器的类都算成"写了键"。用的是常量池条目本身（`0x01`、长度 `0x09`、再是字节）。

### 8.5 验收，以及**没验到的部分**

- `javac` 0 错误（1037 文件）、`gradlew build` ✓、60 个审计（2 个失败与本轮无关，见下）、
  10 个 `--selftest` 全过、`verify_installed_jar` **297/297**、已装入实例（备份 `.bak-130634`）。
- 与本轮**无关**的 2 个失败：`audit_scguns_tags.py`（`scguns:ghost` 标签从未发货，上游 0.5.5 也没有）、
  `audit_menu_spectator_guard.py`（`GunBenchBlockEntity.createMenu` 缺旁观者守卫；本轮没改这个文件）。
- **运行时验证已补做（2026-11-15，NeoForge 21.1.264 专用服务端）**。dev 服起不来
  （`:prepareServerRun` 需要 `neoforge-21.1.249-moddev-config.json`，`maven.neoforged.net:443` 超时），
  改用玩家给的 `neoforge-21.1.264-installer-fat.jar`（**内嵌 MC server.jar + 全部 libraries**，
  可完全离线安装）：
  1. `java -jar neoforge-21.1.264-installer-fat.jar --installServer build/server-21.1.264`
  2. `mods/` = 本 mod + framework 0.13.11 + geckolib 4.9.3 + curios 9.5.1
  3. 干净启动 ⇒ `Done (4.049s)!`、**ERROR/FATAL 0**、`Couldn't parse element` 0、配方 0、标签 0、
     mixin 失败 0
  4. 同一实例换成本轮探针版 jar ⇒ `[SCGUNS-AMMO]` **17/17 全 ok**，含最关键的
     `ItemStack.matches sees the write`（副本看不到新值、原栈看到）⇒ 原子写的核心保证在真实物品上成立
  5. 探针已删除、再构建后核对：产物里无探针类，`[SCGUNS-AMMO]` 不再出现
- 静态依据（与实测一致）：`GunAmmo.set` 走 `NbtHelper.setInt`（`getTag → copy → put → setTag`），
  `setTag` 用 `CustomData.of`（**复制**）安装，而 `PatchedDataComponentMap.set` 是拿**原型**
  （此处为 null）比较后必然装入新实例 ⇒ 写必然脱钩、`ItemStack.matches` 必然看见
  （`tools/audit_nbt_write_alias.py` 本轮为 0）。
- **仍未做**：客户端手感验证（开枪数字 −1 / R 装填 HUD 立变 / G 卸载弹药回包）——需要真人进游戏；
  以及装 Guard Villagers 时那张战利品表**仍然加载**（本机没有 GV，只验证了"没有 GV 时不报错"）。

### 8.6 下一步

第 5 节的阶段 2（卸载顺序）现在由原子写**结构性满足**：`handleUnload` 先 `GunAmmo.set(stack, 0)`
再 `spawnAmmo`，旧代码那种"写进孤儿 ⇒ 弹匣没扣、物品已发"已经不可能。
剩下的是阶段 3：**谁有权清状态**（`C2SMessageReload` 的死分支、客户端 MANUAL 状态机、
两个 NBT 键的写入者收敛），风险中，别在没读 8.3 的情况下动手。
