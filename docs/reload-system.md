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

---

## 9. 阶段 3 之一：卸载打断换弹 → 无限弹壳（2026-11-15，玩家实测报告）

> "callwell 在装填时按卸载弹药键清空弹药后继续装填，装满子弹后仍然继续装填，然后因为枪械是
> 换弹时弹出弹壳的，导致可以无限刷弹壳。"

### 9.1 服务端复现（真协议、真处理器、FakePlayer，21.1.264）

探针按客户端实际发送顺序调用 `C2SMessageReload` / `C2SMessageUnload` / `C2SMessageGunLoaded` /
`C2SMessageEjectCasing`，并驱动真的 `ReloadTracker.onPlayerTick`。**修复前**：

```
after unload:        ammo=0 reloading=false isReloading=false shells=8 casings=0
loop 1:              ammo=1 ... shells=7        ← 已经没有换弹了，还在上膛
loop 2:              ammo=2 ... shells=6
loop 10:             ammo=2（满）... casings=3  ← 满弹后仍每轮产弹壳
```

### 9.2 三个独立的信任漏洞（都在服务端）

| # | 位置 | 漏洞 |
|---|---|---|
| 1 | `C2SMessageGunLoaded` | 这是**动画关键帧**"loaded"，却**不看有没有换弹在跑**就上膛。卸载后换弹已结束，客户端循环继续发它 ⇒ **免费上膛**（弹匣从 0 被填到 2） |
| 2 | `C2SMessageEjectCasing` | 同样是关键帧"eject_casing"，**不看换弹、也不看是否真开过枪**就发弹壳（40% + 弹壳收集器）。装满后不再上膛，但这条**永远有效** ⇒ 无限弹壳 |
| 3 | `AnimatedGunItem.handleMainAnimationLogic` | 卸载后服务端把换弹状态**清掉**（而不是置 STOPPING），客户端没有任何路径去停那段 `reload_loop`：`isReloading` 已假，`ReloadState` 不是 STOPPING，`isPlayingInspectOrReloadAnimations()` 又因为"正在播 reload_loop"返回假 ⇒ **所有分支都不命中，循环动画永远播下去**，每轮重新触发那两个关键帧 |

⚠️ 注意第 1 条是**旧版就有的漏洞**，第 2 条是它的放大器：客户端只要还在循环，服务端就一直配合。

### 9.3 修法

**（a）上膛必须有一次运行中的换弹**（`C2SMessageGunLoaded`）：

```java
if (!Boolean.TRUE.equals(ModSyncedDataKeys.RELOADING.getValue(player))) {
   return;
}
```

**（b）弹壳必须有"欠账"**：新增 `util/SpentRounds`（键 `ScgunsSpentRounds`）——
开枪时 **+1**（`ServerPlayHandler.consumeAmmo`），换弹弹出时 **−1**（`C2SMessageEjectCasing`），
且必须同时满足"正在换弹 + 该枪 `ejectDuringReload`"。40% 的掷骰只决定**捡不捡得到**，
所以是每次**尝试**都扣 1，而不是成功才扣。
⇒ 弹壳数永远 ≤ 实际开火数，**装填/卸载同一批子弹不再产生任何弹壳**。

> 为什么不用持久标记（上一轮 `ScgunsUnloadedMag` 失败的原因）：标记要"记得清"，清不掉就会永久
> 静音之后的正常换弹（玩家看到 NBT 里留着 `ScgunsUnloadedMag:1b`）。**计数器自限**：即使残留，
> 也只能换出"开过的那些枪"对应的壳，不可能无限。

**（c）客户端释放过期动画**（`handleMainAnimationLogic` 的最后一个 `else`）：没有换弹在跑、
但某段换弹动画还在播 ⇒ `cleanupAllReloadTags` + `animationController.stop()` + 回 `idle`。
正常换弹走不到这里（`isReloading` 为真时前面就 return 了），`reload_stop` 阶段也被
`isPlayingCriticalAnimations` 排除在外。

### 9.4 修复后同一套探针

```
1 unload mid-reload:    ammo=0 spent=0 reloading=false
1 after 10 stale loops: ammo=0 spent=0 casings=0     ← 修前 ammo 1→2、casings 3
2 after 2 shots:        ammo=0 spent=2               ← 开两枪，欠两个壳
2 after reload loops:   ammo=2 spent=0 casings=1     ← 合法换弹：上满、掉 1 个壳（40%×2）
3 repeat after unload:  ammo=2 spent=0 casings=1     ← 再玩一次"卸载+装填"：**0 个新壳**
```

### 9.5 门禁

| 工具 | 查什么 |
|---|---|
| `tools/audit_reload_trust.py`（`--selftest`） | ① 上膛前必须有"只否换弹标志"的早退（**不是**旧版那种 `current >= maxAmmo && !RELOADING`）；② 弹壳包必须查换弹 + `SpentRounds.take`；③ 开火必须 `SpentRounds.add`；④ 客户端必须释放过期换弹动画 |
| `verify_installed_jar` 5 项 | `SpentRounds.class` 在产物里；`C2SMessageGunLoaded` 读同步标志（旧版只写）；`C2SMessageEjectCasing` 引用 `SpentRounds` + `ModSyncedDataKeys`；`ServerPlayHandler` 引用 `SpentRounds` |

反向验证：上一版 jar **296/302**（5 项新检查 + GV 表全 FAIL），新 jar **302/302**；
`audit_reload_trust.py --selftest` 在 `backup-reloadstate-20261115` 上报出 **5 条**（含上面第 1 条）。
`javac` 0（1038 文件）、61 个审计（2 个既有无关失败）、已装入实例（备份 `.bak-132638`）。

### 9.6 未验 / 边界

- **客户端 (c) 没跑过**（无显示环境）：需要玩家实测"卸载后动画立刻回到 idle、不再循环"。
  服务端的 (a)(b) 已在真服上用探针验证。
- 弹壳仍会在**开火数**范围内产生（这正是设计意图：每发一颗壳），所以"开 N 枪、换弹"仍能得到
  ≤N 颗壳 —— 不再是无限，但如果你想要"换弹完全不产壳"，那是内容改动，说一声。
- `MobGunFire.consumeAmmo`（生物开火）**没有**加计数：生物的枪不会走客户端的换弹关键帧，
  没有可消费的地方，加了只是空涨。
- 卸载仍然**不会**服务端强制结束换弹：客户端按 G 时会先发 `C2SMessageReload(false)`（探针里
  `reloading=false` 就是这么来的）。若哪天有路径漏发那条包，换弹会继续跑，但因为 (b)，
  它**拿不到任何弹壳**，也不再是漏洞。

---

## 10. 阶段 3 之二：MANUAL 换弹结束后没有平滑衔接到 `reload_stop`（2026-11-15）

> 玩家："单发装填枪械在换弹完成后没有平滑衔接到 reload_stop 动画。"
> （澄清：这里指 **MANUAL**（callwell 那种逐发压弹）；`SINGLE_ITEM` 是激光/喷火那种一次灌满的。）

### 10.1 先排除服务端：它是对的

探针（真处理器 + FakePlayer + **真实等待**，因为手动收尾有 100 ms 宽限）实测：

```
shell 1 loaded:      ammo=1 reloading=true  state=-        playingStop=false
shell 2 loaded:      ammo=2 reloading=true  state=-        playingStop=false shouldStop=true
after full tick 1:   ammo=2 reloading=true  state=-        playingStop=false shouldStop=true
after full tick 2:   ammo=2 reloading=false state=STOPPING playingStop=true  shouldStop=false
```

⇒ 服务端在满弹后**按时**写了 `STOPPING` + `IsPlayingReloadStop`，并发出 `S2CMessageStopReload`。
问题在客户端这一段。

### 10.2 两个事实合起来解释了它

1. **动画数据**：`callwell.reload_loop` 的 `"loop": true`（0.6667 s 一循环）。所以服务端在中途发来
   stop 时，循环**没有"自然结束"的时刻**可等；`handleStopReload` 原来直接
   `controller.stop()` + `tryTriggerAnimation("reload_stop")` ⇒ 在动作中途硬切，看起来就是"跳"。
2. **客户端状态机其实是死的**：`clientInventoryTick` 在开头取了一次 tag，而
   `handleReloadingState` 里又 `getOrCreateTag(stack)` 取了一次 —— 探针实测证明这会**让前一个
   tag 脱钩**（`first == second: false`，写入不落地）：

   ```
   write through the first tag reached the stack: false
   write through getTagForWrite reached the stack without setTag: true
   ```

   于是那张状态机里所有 `nbt.putString("scguns:ReloadState", …)`（NONE/STARTING/LOADING/STOPPING）
   **全部写进了孤儿**。它自带的"等当前循环播完再转 STOPPING"的平滑逻辑（`PendingStopTransition`）
   因此从来不会生效 —— 这就是"没有平滑衔接"的根因。

### 10.3 修法：把切换点挪到循环周期的边界

不去复活那张状态机（它一旦有写权限就会与服务端争同一批键，见 §8.3 的取舍），而是在**已有的、
每循环触发一次的关键帧**上做交接。

> **第一版错了，这是玩家实测的第二版**：`reload_stop` 的待处理标记一开始写在**物品 NBT**
> （`scguns:ReloadStopAtCycleEnd`）里，玩家实测的结果是"直接衔接到 idle"。原因很清楚：**服务端才是
> 物品 tag 的权威方**，它随后任何一次槽位同步都会把客户端写进去的键整段覆盖掉；标记一丢，
> §10.2 的兜底分支就把循环收成 idle。
> **教训：客户端的临时状态不要写进服务端拥有的物品 tag。**

第二版把待处理标记放在**客户端静态内存**里（`pendingReloadStopStack` / `pendingReloadStopAt`），
并用一个 `ready` 标志把"关键帧"和"切动画"拆成两步：

| 位置 | 改动 |
|---|---|
| `ClientPlayHandler.handleStopReload` | 若 `reload_loop`/`carbine_reload_loop` 正在播 ⇒ **不切**，调 `AnimatedGunItem.requestReloadStopAtCycleEnd(heldItem)`（**纯客户端内存**）并清掉刚同步来的 STOPPING；没有循环在播时维持原行为 |
| `particleListener` 的 `"loaded"` 关键帧 | 只置 `pendingReloadStopReady = true`。**不在关键帧回调里切动画**：那是动画控制器的 tick 内部，`stop()`/`tryTriggerAnimation` 会造成重入 |
| `handleMainAnimationLogic` 的兜底分支 | 下一个 inventory tick 看到 `ready`（或 1.5 s 超时）才调 `beginReloadStop(...)`；没有待处理请求时保持原行为（卸载那种"无主循环"直接释放） |
| `handleItemNotHeld` | 枪离手时丢掉待处理请求 |
| `beginReloadStop(...)` | 写 STOPPING / IsPlayingReloadStop 并 `tryTriggerAnimation("reload_stop")` |

**为什么这样是"平滑"的**：`loaded` 关键帧就落在每个压弹循环的末尾，所以切换发生在**动作完成的那一刻**
（最多晚一个 inventory tick，≈ 50 ms），而不是服务端说停就停的那一刻。

### 10.4 未验 / 边界

- **视觉效果只能玩家看**：本轮没有显示环境，客户端动画时序无法无头验证。请确认：
  逐发压弹到满 → 当前这一发压完后**接着**播 `reload_stop`（不再半途跳过去、也不再直接回 idle）
  → 回到 idle。
- 1.5 s 兜底只在"循环动画里没有 `loaded` 关键帧"时触发；40 把 MANUAL 枪的 `reload_loop` 都有
  `loaded`（压弹靠它发包），所以正常不会用到。
- **客户端 MANUAL 状态机仍然是死的**（10.2 第 2 条）。本轮只是绕开它实现平滑收尾；若要修
  它的 `reload_start → reload_loop` 过渡，就得把 `getOrCreateTag` 换成只读并处理"两个写入者"，
  那是 §8.3 说的高风险项，**没有玩家报这个问题之前不要动**。
- 门禁：`verify_installed_jar` 新增 2 项（`pendingReloadStopStack` 在客户端类里、`ClientPlayHandler`
  调用 `requestReloadStopAtCycleEnd`）⇒ 上一版（tag 标记那版）**302/304**（2 项 FAIL），新 jar
  **304/304**；`javac` 0（1038 文件）、61 审计（2 个既有无关失败）、已安装（备份 `.bak-141526`）。
