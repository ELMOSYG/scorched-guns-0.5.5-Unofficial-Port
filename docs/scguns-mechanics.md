# Scorched Guns 1.21.1 移植版 —— 机制参考

本文记录这个移植版里**已经核实过**的机制，供做附属、写数据包、或将来调整时参考。
每一条都标了出处（代码位置或数据文件），未经核实的写「待核实」。

---

## 1. 开火模式（FireMode）

`common/FireMode.java` 定义了 8 个模式，但**只有注册过的才能用** —— 未注册的值会被
`FireMode.getType(...)` 静默退回 `SEMI_AUTO`（半自动）。写数据时务必核对：

| id | 是否注册 | 说明 |
|---|---|---|
| `scguns:semi_automatic` | 是 | 半自动 |
| `scguns:automatic` | 是 | 全自动（按住持续射击） |
| `scguns:burst` | 是 | 连发 |
| `scguns:pulse` | 是 | 蓄力 |
| `scguns:beam` | 是 | 持续光束（按 `beamAmmoConsumptionDelay` 持续扣弹） |
| `scguns:semi_beam` | 是 | 半自动光束（每段只扣 1 发） |
| `scguns:automatic_beam` | 是（本移植版补注册） | 全自动光束：按射速反复「开始一段光束」，即 `semi_beam` 的连发版 |
| `scguns:beam_burst` | 是（本移植版补注册） | 连发光束 |

> **历史问题**：`automatic_beam` 与 `beam_burst` 的枚举值一直存在，但从未 `registerType`，
> 所以写进数据也会变成半自动 —— 看上去像「模组只支持半自动光束」，实际是注册遗漏。

### 全自动光束的行为

- 判定上属于**光束家族**（`FireMode.isBeam(...)` = `BEAM` / `SEMI_BEAM` / `AUTOMATIC_BEAM`）。
- 弹药：走 `semi_beam` 那条分支，**每一段只扣 1 发**（不是持续烧弹）。
- 观感：客户端 `BeamHandler.AUTOMATIC_BEAM_GAP_MS`（60 ms）会在两次开火之间把光束判定为
  「本段结束」，于是按住扳机时呈现**脉动**，而不是一条不断的光。
  该值必须**小于射击间隔**（`rate 3` = 150 ms 时可以）；把 `rate` 调到 1（50 ms）就会连成一条。

### 光束枪需要的数据字段（`general` 段）

```
beamColor, secondaryBeamColor, enchantedBeamColor, enchantedSecondaryBeamColor
beamMaxDistance, beamDamageDelay, beamAmmoConsumptionDelay
```

---

## 2. 装填类型（ReloadType）

| id | 真实语义 | 数量 |
|---|---|---|
| `scguns:mag_fed` | 弹匣供弹：一次装填装满 | 90 把 |
| `scguns:manual` | 手动：一次装填装满 | 40 把 |
| `scguns:single_item` | **用「单个物品」装满整个弹匣** | 11 把 |

> **命名陷阱**：`single_item` 的 “single” 指**物品**（一个能量核心），不是发数。
> 实测：消耗 1 个 `scguns:energy_core`，**一次装满整个弹容**（`laser_musket` 7 发、`cr4k` 150 发）。
> 不要理解成「装一次加一发」。

用 `single_item` 的 11 把枪（消耗品一致，是刻意设计，不宜单独改动）：

```
basker, blasphemy, inquisitor, pyroclastic_flow, spitfire      -> scguns:blaze_fuel
cr4k_mining_laser, flayed_god, laser_musket, minksy,
shard_culler, waltz_conversion                                 -> scguns:energy_core
```

---

## 3. 能量核心类武器的平衡模型

这是一套**闭环经济**，不是「弹药免费」：

```
造核心（造价贵 + 需要电力基建）
   -> 装填：消耗 1 个核心，装满一整匣
   -> 打空：抛出「耗尽的核心」（reloadByproduct + byproductChance，laser_musket = 0.6）
   -> 回收再加工 -> 回到第一步
```

- 好处：单发伤害较高 + 核心可回收。
- 代价：核心造价 + 电力基建（本模组自带 `lightning_battery` / `polar_generator` /
  `powered_macerator` 等，构成完整的弹药产业链）。
- 因此 `cr4k_mining_laser` 能有 150 发：它是采集工具，靠 `enableMining` / `miningSpeed`。

---

## 4. 弹丸「优势」（advantage）系统

数据文件：`data/scguns/entity/advantages.json`（内置默认在
`config/ProjectileAdvantageConfig.java#loadDefaults`，文件存在时以文件为准）。

每个优势包含：

```json
{ "multiplier": 1.75, "causes_fire": true, "fire_duration": 2,
  "target_tags": ["minecraft:sensitive_to_smite"] }
```

判定在 `entity/projectile/ProjectileEntity#advantageMultiplier`：

1. 若优势名为 `undead` / `illager` / `water`，**先问实体自报的族群**（见第 5 节）；
2. 再遍历 `target_tags`，按**实体类型标签**匹配；
3. 命中则应用 `multiplier`，并在 `causes_fire` 时点燃 `fire_duration` 秒。

> 第 1 步是本移植版补的：原先只看实体类型标签，于是「戴上僵尸头的试验假人」永远吃不到
> `scguns:undead` 的加成。

---

## 5. 生物族群判定

`util/MobType.java#of(Entity)` 的顺序：

1. `Player` 返回 `UNDEFINED`；
2. **反射调用实体自己的 `getMobType()`**（按类缓存方法，无编译期依赖），非 `UNDEFINED` 就用它；
3. 退回 `EntityTypeTags`（`SENSITIVE_TO_SMITE` / `SENSITIVE_TO_BANE_OF_ARTHROPODS` /
   `SENSITIVE_TO_IMPALING`）与 `AbstractIllager`。

这样任何**在运行时改变自己族群**的实体都能被识别：例如试验假人（`dummmmmmy`）戴上僵尸头后
自报为亡灵，本模组的近战 / 光束 / 附魔加成都会生效。

`getPuncturingDamageReduction` 目前是空实现（`return damage;`），属未启用内容。

---

## 6. 数据包覆盖

数据包放在世界的 `datapacks/<名称>/`，结构照模组内的路径：

```
datapacks/<名称>/pack.mcmeta                     {"pack":{"pack_format":48,...}}
datapacks/<名称>/data/scguns/guns/<枪>.json       整份覆盖（不是合并）
```

仓库 `datapacks/` 下的示例：

- `greaser-smg-900rpm` —— 用 `general.rpm` 覆盖射速（需本移植版的连续射速改造，尚未落地）；
- `waltz-automatic-beam` —— `fireMode: scguns:automatic_beam` + `rate: 3`。

> **提醒**：覆盖是**整份替换**，所以要复制原文件再改，只改需要的字段。

---

## 7. 本会话核实过的「定义了但未启用」清单

（供将来决定是否启用或清理，切勿当作 bug 直接「修」掉）

| 项 | 状态 |
|---|---|
| `scguns:automatic_beam` / `scguns:beam_burst` | 已由本移植版补注册，现在可用 |
| `Gun.Reloads.infiniteAmmo` / `Gun.General.infiniteAmmo` | 字段存在但无人消费（唯一生效的是 NBT `IgnoreAmmo`）；要做「免耗材」枪需要接线 |
| 血月袭击 `blood_moon_raid.json` | 缺少 `raid_level`，按等级索引的 `RaidConfig` 永远不会选中它，因此不会被触发 |
| 末地袭击语言键 | `raid.scguns.end` 等三条只有文本，上游与我们都**没有**对应的袭击数据 |
| 7 组进度文本 | `flint` / `copper` / `iron` / `ancient_brass` / `mk43_rifle` / `nervepinch` / `flayed_god`：语言文件有、没有对应进度 JSON |
| 附魔 `fire_starter` | 语言文件有、`data/scguns/enchantment/` 里没有 |
| `entity.scguns.redcoat` | 死键（另一个移植版有 `RedcoatEntity`，我们没有） |
| 堡垒战利品只挂 `bastion_treasure` + `bastion_bridge` | **有意为之**：让 `super_shotgun` 等顶级武器保持稀有，勿补 `bastion_other` |
| 穿甲（`PUNCTURING`） | 每级 +5% 暴击率 + 5 点穿甲，但「点数穿甲」几乎不生效；射速 -6%/级、后坐力 +10%/级 |
| 高能子弹（`ramrod_round`） | 优势 `scguns:undead`，数据文件里是 1.75 倍 + 2 秒点燃 |
| 枪手 AI（`GunAttackGoal`） | 刻意不接 `baseRate * mobFireRateMultiplier`：有走位、瞄准延迟与点射节奏 |
| 女仆兼容定位 | 视为「可直接用枪的可移动炮塔」；炮塔弹丸的穿甲是**真生效**的，若复用会让女仆强于玩家 |
