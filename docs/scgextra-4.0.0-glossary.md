# scg-extra 4.0.0 汉化术语对照表

`scgextra-forge-4.0.0.jar` 的 `assets/scgextra/lang/` 里**只有 `en_us.json`，没有任何 `zh_cn.json`**，共 **556** 条
（`scgextra.*` 282、`item.*` 148、`entity.*` 73、`armorset.*` 20、`attribute.*` 18、`block.*` 8、`effect.*` 5、
`itemGroup.*` 1、`biome.*` 1）。所以不是“漏翻了几条”，是整份都要补。

下面先定命名规则，再给逐条对照。

## 一、命名规则（先统一，避免同类不同名）

| 规则 | 示例 |
|---|---|
| 派系前缀保留缩写，不翻译 | `COG` / `FAC` / `RRC` / `Asghar` 原样保留 |
| 刷怪蛋统一 `…刷怪蛋` | `Ammo Goblin Spawn Egg` → 弹药小鬼刷怪蛋 |
| 盔甲四件套统一 `头盔 / 胸甲 / 护腿 / 靴子` | `Wrecker Chestplate` → 破坏者胸甲 |
| `Super Flare` 统一 `超级照明弹` | `Iron Super Flare` → 铁质超级照明弹 |
| `Medal of …` 统一 `…勋章` | `Medal of Cruelty` → 残虐勋章 |
| `… Boots/Chestplate/Helmet/Leggings` 归入对应套装名 | 见第三节套装表 |
| `Halves / Full set bonus` 统一 `散件效果 / 全套效果` | 原文用 `Half Set Bonus` / `Full Set Bonus` |

## 二、实体（73 条）

| 英文 | 中文建议 | 备注 |
|---|---|---|
| Fish Folk | 鱼人 | |
| Turtleman | 龟人 | |
| Salmon Rider | 鲑鱼骑士 | key 是 `salmonsaur` |
| Lion Fish | 狮鱼 | |
| Pufficus | 河豚兽 | |
| Tentacliator | 触手撕裂者 | |
| Glowing Tentacliator | 发光触手撕裂者 | |
| Armored Whale | 装甲鲸 | |
| Anomaly | 异常体 | |
| Guardian Statue | 守卫雕像 | |
| Ammo Goblin | 弹药小鬼 | |
| Big Lump | 大肉块 | |
| Mutant Bat | 变异蝙蝠 | |
| Nitro Beetle | 硝化甲虫 | |
| Head Hunter | 猎头者 | |
| Netherite Eater | 下界合金吞噬者 | |
| Failed One | 弃造者 | |
| Candle Fiend | 烛焰魔 | |
| Soul Ripper | 灵魂撕裂者 | |
| Pigdread | 惧猪 | |
| Pigkan | 猪坎 | 音译 |
| Piglin Nob | 猪灵贵族 | |
| Piglin Warboss | 猪灵战酋 | |
| Piglin Hog Rider | 猪灵疣猪骑士 | |
| Piglin Strider Rider | 猪灵炽足兽骑士 | |
| Piglin Blaze Kopter | 猪灵烈焰旋翼机 | |
| Piglin Kiln Hog | 猪灵熔炉疣猪 | |
| Piglin Kiln Hog Magma Ball | 猪灵熔炉疣猪岩浆球 | |
| Inflicted Boar | 感染野猪 | |
| Inflicted Wolf | 感染狼 | |
| End Pod | 末地荚 | |
| End Dweller | 末地潜伏者 | |
| End Stone Crab | 末地石蟹 | |
| End Scorpion | 末地蝎 | |
| Turret Seat | 炮塔座位 | 载具实体 |
| COG Vulture | 齿轮 秃鹫 | |
| COG Devastator | 齿轮 毁灭者 | |
| COG Bombardier | 齿轮 轰炸机 | |
| COG Gigantes | 齿轮 巨像 | |
| COG Venator | 齿轮 猎手 | |
| COG Centipede | 齿轮 蜈蚣 | |
| COG Juggernaut | 齿轮 重装兵 | |
| COG Trenchbleeder | 齿轮 战壕屠夫 | |
| FAC Trencher | FAC 战壕兵 | |
| FAC Bluecoat | FAC 蓝衣军 | |
| FAC Trench Goblin | FAC 战壕哥布林 | |
| FAC Trench Sniper | FAC 战壕狙击手 | |
| FAC Shovel Knight | FAC 铲骑士 | |
| FAC Tank Buster | FAC 反坦克手 | |
| FAC Lion | FAC 狮 | |
| FAC Commissar | FAC 政委 | |
| FAC Walker | FAC 步行机 | |
| FAC Tank | FAC 坦克 | |
| RRC Drone | RRC 无人机 | |
| RRC Scout | RRC 侦察兵 | |
| RRC Spring Junkie | RRC 弹簧疯子 | |
| RRC Arc Psycho | RRC 电弧疯子 | |
| RRC Tallman | RRC 高个子 | |
| RRC Oppressor | RRC 压迫者 | |
| RRC Flaming Head | RRC 燃烧头颅 | |
| RRC Copper Knight | RRC 铜骑士 | |
| RRC Scrap Guard | RRC 废料卫士 | |
| RRC Gargoyle | RRC 石像鬼 | |
| Asghar Worker Unit | 阿斯加工兵 | |
| Asghar Flamer | 阿斯加喷火兵 | |
| Asghar Elite Surgeon | 阿斯加精英外科医生 | |
| Wrecker Red | 破坏者·红 | |
| Wrecker Blue | 破坏者·蓝 | |
| Wrecker Green | 破坏者·绿 | |
| Wrecker Jumbo | 破坏者·巨型 | |
| Wrecker Helicube | 破坏者·直升方块 | |
| Wrecker Dozer | 破坏者·推土机 | |
| Wrecker Biker | 破坏者·摩托手 | |
| Wrecker Turret | 破坏者炮塔 | |

## 三、盔甲套装（20 条效果 + 40 件装备）

| 套装 | 中文 | 散件效果（`half_set`） | 全套效果 |
|---|---|---|---|
| Wrecker | 破坏者 | 攻击或受击时使敌人撕裂 | 弹丸伤害 +20%，受到弹丸伤害 +20% |
| Oppressor | 压迫者 | 100% 抗击退 | 对亡灵伤害 +10% |
| Juggernaut | 重装兵 | 免疫撕裂 | 后坐力 -10%，移速 -10% |
| Commissar | 政委 | 弹丸伤害减免 10% | 换弹速度 +20% |
| Enlightened | 开悟者 | 获得生命恢复，且不被监守者侦测 | 受击时获得 3 秒冷却的治疗 I |
| Golden Idol | 黄金神像 | 为 5 格内友方与中立生物回血 | 使 10 格内敌对生物虚弱 |
| Leviathan | 利维坦 | 每 10 秒净化一次减益 | 水下呼吸 + 海豚的恩惠 |
| Pioneer | 先驱 | 免疫漂浮，并授予鞘翅飞行 | 暴击率 +10%，弹丸暴击伤害 +10% |
| Ritual | 仪式 | 受击时获得 4 秒冷却的生命恢复 I | 死亡时复活（冷却 20 分钟） |

> 四件套的件名：`<套装中文>头盔 / 胸甲 / 护腿 / 靴子`，例如 `Wrecker Helmet` → 破坏者头盔。

## 四、物品（148 条，除刷怪蛋外的重点）

| 英文 | 中文建议 |
|---|---|
| Bandage | 绷带 |
| Medipack | 医疗包（增强普通绷带效果） |
| Medkit | 急救箱 |
| Cavalry Saber | 骑兵军刀 |
| Atlantic Mace | 大西洋重锤 |
| Spear Shovel | 矛铲 |
| Wrecking Tool | 破拆工具 |
| Wrecker Sledge Hammer | 破坏者大锤 |
| Wrecker RPG | 破坏者火箭筒 |
| Netherite Chain Axe | 下界合金链锯斧 |
| End Shell | 末地甲壳 |
| Medal of Conqueror | 征服者勋章（暴击率 +20%，弹丸重力 -20%） |
| Medal of Cruelty | 残虐勋章（弹丸伤害 +10%） |
| Medal of Defiance | 抗命勋章（换弹与移速 +20%） |
| Medal of Enlightenment | 开悟勋章（最大生命 +8） |
| Medal of Fiery Rage | 炽怒勋章（力量 II、生命恢复 I） |
| Medal of Iron Will | 钢铁意志勋章（弹丸伤害减免 20%） |
| Medal of Obedience | 服从勋章（护甲韧性 +3，后坐力 -30%） |
| Medal of Survivor | 幸存者勋章（护甲 +5） |
| Medal of Wonder | 奇迹勋章（抗性 I，免疫中毒与硫磺中毒） |
| `<材质> Super Flare` | `<材质>超级照明弹`：Antique 古董 / Copper 铜 / Diamond Steel 钻石钢 / Frontier 拓荒 / Gold 金 / Iron 铁 / Ocean 海洋 / Sculk 幽匿 / Treated Brass 处理黄铜 / Wrecker 破坏者 |
| `Single Use Only` | 一次性物品 |
| `SCG Extra`（物品栏） | SCG 扩展 |

> 148 条里 **73 条是刷怪蛋**，按第一节规则批量生成即可。

## 五、方块（8 条）

| 英文 | 中文建议 |
|---|---|
| Barbed Wires | 带刺铁丝网 |
| Electrical Wires | 电网 |
| Leviathan Mobs Spawner | 利维坦刷怪笼 |
| Stationary Turret | 固定炮塔 |
| `Ammo: %s` | 弹药：%s |
| `Interact to mount` | 右键操作 |
| `No Ammo` | 没有弹药 |
| `Someone else is already manning this turret.` | 已经有其他人在操作这座炮塔。 |

## 六、属性（18 条）

| 英文 | 中文建议 |
|---|---|
| Additional Crit Chance | 额外暴击率 |
| Additional Crit Damage Multiplier | 额外暴击伤害倍率 |
| Bullet Damage Taken Multiplier | 受到弹丸伤害倍率 |
| Bullet Gravity Multiplier | 弹丸重力倍率 |
| General Damage Multiplier | 通用伤害倍率 |
| Magic Damage Taken | 受到魔法伤害 |
| Recoil Multiplier | 后坐力倍率 |
| Undead Damage Multiplier | 对亡灵伤害倍率 |
| Debuff Zone | 减益领域 |
| Commissar / Enlightened / Golden Idol / Juggernaut / Leviathan / Oppressor / Pioneer / Ritual / Wrecker | 同套装名（政委 / 开悟者 / 黄金神像 / 重装兵 / 利维坦 / 压迫者 / 先驱 / 仪式 / 破坏者） |

## 七、效果与其它（7 条）

| 英文 | 中文建议 |
|---|---|
| Enlightenment | 开悟 |
| Glowing Ink Effect | 发光墨汁 |
| Ink Effect | 墨汁 |
| Mortal | 凡躯 |
| Leviathan Regeneration | 利维坦再生 |
| Warzone（生物群系） | 战区 |
| SCG Extra（创造模式标签） | SCG 扩展 |

## 八、还有一类：`scgextra.*` 的 282 条

这些是**界面文案、提示与消息**（不是专有名词），按语意直译即可；如果要做完整 `zh_cn.json`，
建议先按本表把 73 个实体、40 件盔甲、148 个物品统一，再逐条处理这 282 条，避免同一句话出现两种叫法。
