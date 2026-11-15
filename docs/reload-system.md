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

先做第 5 节的阶段 1（弹药读写 helper），否则只会在别处制造新的怪现象。
