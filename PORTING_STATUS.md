# SC2 0.5.5 → MC 1.21.1 / NeoForge 移植：进度速览

> **完整交接文档见 [`HANDOFF.md`](HANDOFF.md)。这份只是状态牌。**

| 项 | 值 |
|---|---|
| 代码基准 | **0.5.5 的 1.20.1 release jar** 反编译（`E:\mod\SG2-1.21\.sg055_deobf`，994 个外类，与 jar 1:1 一致）。**不使用** GitHub 上的 0.4.7 源码，也**不使用**现存的 0.4.7 底子 NeoForge 移植工程 |
| 目标 | MC 1.21.1 + NeoForge 21.1.249（用户实测实例：NeoForge 21.1.250） |
| **编译错误** | **0**（起始基线 3487 → 496 → 0），989 个源文件全部通过 |
| **`gradlew build`** | ✅ **BUILD SUCCESSFUL**，产出 `build/libs/scguns-0.5.5.1.jar`（18.5 MB） |
| **专用服务器加载** | ✅ **成功启动**（`Done (4.838s)!`），无 mod 相关 ERROR/FATAL |
| **客户端实测** | ✅ **上一轮反馈的功能全部实测正常**（用户确认）。累计已修：崩溃 ×6 类、枪械渲染、物品 tooltip、枪手 AI/生物装备、界面背景模糊、配件界面枪械模型、后坐力速度、持枪姿势、**进度标签页、蓝图枪械描述、配件实时同步、外骨骼界面崩溃** |
| **持枪姿势** | ✅ **根因已找到并修复**：`MixinPlugin.shouldApplyMixin` 探测的是 1.20.1 Forge 的类名 `FrameworkForge`（NeoForge 是 `FrameworkNeoForge`），于是**全部 15 个 mixin 从来没生效过** —— 生物的摆臂 mixin、玩家的 `ItemInHandLayerMixin`/`PlayerModelMixin` 全是死的。现在开关恒为 `true`（HANDOFF §15） |
| **mixin 首次真正生效** | ✅ 顺带查修 4 个"一开启就崩客户端"的注入点（`EndPortalBlockMixin`/`GameRendererMixin`/`LevelRendererMixin`/`MinecraftMixin`/`MouseHandlerMixin`），`runServer` 现在能看到 `Mixing ... from scguns.mixins.json` 且 0 注入失败 |
| JEI | ✅ 8 个界面类**已并入编译**（`build.gradle` 的 exclude 已删除），全部通过 |
| 可选前置 | ✅ `libs/` 现有 **15** 个 jar（新增 `soul-fire-d` 6.1.0 / `FarmersDelight` 1.3.4；`fileTree('libs')` 会自动收为 `compileOnly` + `localRuntime`）。接进来后**立刻查出并修掉**"装了农夫乐事就启动崩"的 `KnifeItem` 构造器漂移；灵魂火兼容改用 Prometheus 的**同签名** API；并**实测确认没装这些模组时不会 NoClassDefFoundError**（HANDOFF §19） |
| 数据包体检 | ✅ **`Parsing error loading recipe` = 0，`Couldn't load tag` = 0**，6922 个配方 + 3644 个进度全部加载；服务端整轮 **0 条 ERROR/FATAL** |
| 「本该覆写却签名漂移」体检 | ✅ **0**（起始 17 处签名漂移 + **39 处参数个数漂移**）。其中 `finalizeSpawn` 多一个参数导致**所有生物从来不带武器**，`appendHoverText` 参数顺序错导致**所有 tooltip 不显示** |
| 生物装备（**实机验证**） | ✅ RCON 实测：**6/6** 生物召唤时带主手物品（修前 0/6），**3/6** 带**已装弹的枪**（`AmmoCount` 存在）。`tools/rcon_mob_equipment.py` |
| 战利品函数体检 | ✅ 8 种 function id 全部为 1.21.1 现存（`tools/fix_loot_functions.py`） |
| **Sable 物理结构** | ✅ **已兼容**：0.5.5 对物理模组只有一处兼容（Valkyrien Skies 的"带船射线"），本移植把那一处降级成了死代码；而 Sable 是**替换 `BlockGetter#clip`**（不是往主世界加方块），我们那套 `world.getBlockState` 逐格遍历**只看主世界** ⇒ 子弹穿过物理结构。现在 `ProjectileEntity` 在 `sableLoaded \|\| valkyrienSkiesLoaded` 时改为调用 `world.clip(context)`（HANDOFF §20）。实机：空中射箭能停在 sub-level 上（`inGround=true` 且该处主世界为空气） |
| **物理结构上的炮塔只能朝一个方向打** | ✅ **已修（不是不能修）**：炮塔的 `worldPosition` 是**结构自身坐标**，而目标的 `getX/Y/Z` 是**世界坐标**，两者相减 ⇒ 角度几乎不随目标变化。现在用 Sable 官方 API（`SubLevelContainer`→`LevelPlot`→`SubLevel.logicalPose()`→`Pose3dc.transformPosition(Inverse)`）把瞄准点/枪口/音效/粒子/发包 chunk 统一到同一坐标系（HANDOFF §21）。**待玩家客户端确认** |
| **手榴弹/炸弹右键直接爆炸、丢不出去** | ✅ **已修（1.21.1 改了 `getUseDuration` 参数）**：1.20.1 是 `getUseDuration(ItemStack)`，1.21.1 变成 `(ItemStack, LivingEntity)` ⇒ 旧签名**不再重写任何东西**，引擎拿到默认 **0 tick**，右键下一 tick 就 `finishUsingItem` ⇒ `grenade.onDeath()` **在手里炸**，且永远走不到 `releaseUsing`（投掷）。全树 **16 处**同一问题（10 种手榴弹/炸弹/燃烧瓶 + 绷带/冰袋/储气罐/燃料弹/金属探测器/测距仪 + 枪）；物品内部调用解析到自己的旧方法，所以"看起来自洽"才一直没暴露。已全部改成两参 + **`@Override`**（签名再漂就编译失败）并修 18 处内部调用（HANDOFF §24）。新增通用检查 `tools/audit_stale_overrides.py`（同名不同签名 + 基线 277 条合法项），对改动前的 class 实测报出 16 条 NEW ✓ |
| **模型的半透明/染色层不渲染**（Sulfurhead 凝胶层） | ✅ **已修（1.21.1 迁移漏洞，影响全部模型）**：1.21.1 把顶点颜色塞进 `renderToBuffer(..., int color)` 并新增五参 `ModelPart.render(...)`，移植时**只改了签名、方法体仍调四参版** ⇒ 颜色（含 alpha）被静默丢弃，按"纯白+不透明"渲染。Sulfurhead 的凝胶层请求 alpha 0.4，结果外壳变成不透明白色（玩家报"半透明材质无法渲染"）。影响 **21 个模型类、57 处调用**，已全部修好；新增审计 `tools/audit_model_vertex_color.py`（对补丁前的源码实测报 57 个问题）+ `verify_installed_jar` 4 项检查（用旧 jar 自测 54/57）。见 HANDOFF §23 |
| **Sulfurhead 只受玩家伤害** | ⚠️ **按玩家要求已去掉**（**有意偏离 0.5.5**）：0.5.5 用 `hurt` 重写拒绝一切非玩家来源（子弹的 `DamageSource` 把射手作为 causing entity，所以玩家开枪照常有效），移植原本照搬了这条。玩家要求去掉后已删除该重写 ⇒ 与普通怪物一致，`HurtByTargetGoal` 也会反击任何攻击者。实测（RCON + 僵尸对照）：`sulfurhead 30.0→21.0 hp (-9.0)`、`zombie 20.0→11.1 hp (-8.9)`，两者一致 ✓。回滚代码见 HANDOFF §23.5（源码注释里也留了一份） |
| **Anthralite 系列工具没有属性** | ✅ **已修（1.21 把工具属性挪进了物品组件）**：1.20.1 是 `new PickaxeItem(Tier, int, float, Properties)`，1.21 改成把这两个数字放进 `Properties.attributes(PickaxeItem.createAttributes(...))`；移植只删了数字、没补 `.attributes(...)` ⇒ 5 件工具（pickaxe/sword/axe/shovel/hoe）**一条属性修饰符都没有**，攻击力退回裸手水平（挖掘等级来自 `Tier`，所以挖矿看着正常，很难发现）。模组自己的工具类（Waraxe/CogMace/AnthraliteHammer）早就改对了。数值沿用 0.5.5，并用游戏内实测核对：剑 `5.50`/`-2.40`、斧 `7.50`/`-3.00`、镐 `3.50`/`-2.80`、铲 `4.00`/`-3.00`、锄 `-0.50`/`-3.00`（= ANTHRALITE 的 tier 加成 2.5 + 0.5.5 的原参数）。新增审计 `tools/audit_item_attributes.py`（对补丁前的源码实测报 5 条）。顺带确认 `AnthralitePaxelItem`/`BayonetItem` 走的 `getDefaultAttributeModifiers(ItemStack)` 重写**是生效的**（NeoForge 接口默认方法；实测 `iron_bayonet` 打出 `1.50` ✓），且 paxel/knife 属于**条件注册**（需 Create: Iron Works / Farmer's Delight）。见 HANDOFF §25 |
| **mod 的工作方块无法交互** | ✅ **已修（1.21.1 把 `use` 拆成 `useItemOn`/`useWithoutItem`）**：移植里 **14 个方块**照 0.5.5 重写了 `use(BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)`，而该签名在 1.21.1（原版与 NeoForge 合并 jar 都 `javap` 核对过）**已不存在** ⇒ 没有 `@Override` 保护 ⇒ 编译通过但**没人调用** ⇒ 右键无反应。受影响：枪械工作台 / 研磨机（含通电）/ 机械压床（含通电）/ 极光发电机 / 采矿单元 / 纪念碑 / 充能紫水晶继电器 / 高级堆肥桶 / 酸性坩埚 / 鸟粪蜡烛 / 沙袋。已按"是否读取手持物品"分别改到两个新钩子（9 个 `useWithoutItem` ✓ + 5 个 `useItemOn` ✓），并**全部补 `@Override`**（签名再漂即编译失败 ✓）。`verify_installed_jar` 新增 15 项：旧 jar **65/80**（必须 FAIL ✓）、新 jar **80/80** ✓。见 HANDOFF §30.1 |
| **en_us 缺的 10 条字幕** | ✅ **已补（+ 发现整个字幕前缀拼错）**：10 条 `subtitle.scguns.*` 已按玩家要求插入 `en_us.json`（只 +10 行 ✓，英文措辞见 HANDOFF §34.1 ✓），补完后 **en_us 与 zh_cn 键集完全一致**（都是 1794 ✓，`audit_lang_keys.py` 实测 missing/extra 全 0 ✓）。**但更深的发现**：1.21.1 的 `SoundEvent` 已不含字幕组件（`javap` 只有 `createVariableRangeEvent`/`getRange` 等 ✓），原版字幕键拼法是 **`subtitles.<ns>.<声音路径>`（有 s）** ✓，而本模组（0.5.5 与移植版都）用 `subtitle.scguns.*`（无 s）✗ 且注册时用单参构造 ✓ ⇒ **没有任何代码会查这些键** ✗。审计新增实测：**103 个声音事件需要 103 条 `subtitles.scguns.*`，现存 0 条** ✗，46 条 legacy `subtitle.*` 永远不会被查 ✓ ⇒ 这是"字幕缺失"的真因（0.5.5 亦然，非移植引入）。修法需玩家拍板（涉及 103 条英文文案，其中 46 条可由现有键机械映射）。见 HANDOFF §34.2 |
| **敌怪枪械"不带 Gun Rust 附魔"** | ✅ **已查清：附魔在，移植没漏，无需改代码**（三层实测）。① **附魔确实被施加**：服务器上反复召唤枪手读 `HandItems` NBT，**15 把枪里 12 把带 `minecraft:enchantments: {levels: {"scguns:gun_rust": 1}}`** ✓（就是 85% 的掷骰 ✓，且走**原版**附魔组件 ✓）。容易误判的原因是**怪物同时会掷近战/盔甲** ✗（adjudicator 6 条里 2 条近战、cog_knight 6 条里 5 条近战、blunderer 固定 `war_axe` 永远不带咒 ✓，它不是 `GunItem` ✓）—— 判枪的可靠标志是 NBT 里的 `custom_data: {AmmoCount}` ✓。② **施加代码与 0.5.5 逐行一致**：`applyCurseIfRoll` 在两棵树里都是同样三处调用点（`:140/:142`、`:163/:167`、`:259/:263` ✓），`RaidManager` 发枪不掷咒也是 0.5.5 原样 ✓；所有枪类都 `extends GunItem` ✓。③ **真正的原因：这条咒对怪物本来就没有任何作用** —— 唯一生效处是 `GunEventBus` 里的卡壳判定，而它的事件是 `GunFireEvent extends PlayerEvent` ✗（0.5.5 亦然）⇒ 怪物 AI 的 `GunAttackGoal` 根本不经过它 ⇒ **带咒/不带咒的怪物枪行为完全一样** ✓；`isCursed()`/`removeCurse()` 两棵树都是死代码 ✓。要"怪物也卡壳"属于**新功能**（有意偏离 0.5.5），待玩家拍板 ✓。附两条环境事实：`/kill` **永远不会掉装备**（原版 `Mob.dropCustomDeathLoot` 要求 `hitByPlayer`，`/kill` 不满足 ⇒ 实测 30 只主手 0 掉落是正常的 ✓）、带咒的枪**没有附魔光效**（`GunItem.isFoil` 返回 false，0.5.5 逐字相同 ✓，该重写经 `javap` 确认生效 ✓）。工具 `tools/rcon_mob_gun_curse_check.py` 已重写（旧版把近战当枪 ✗ 报过误导性的 3/4 ✓，现按 `AmmoCount` 判定、失败才返回 1 ✓，实跑 5/5 ✓）。见 HANDOFF §35 |
| **枪锈诅咒其实是诅咒类附魔** | ✅ **已修（1.21 把"是不是诅咒"改成了标签，我们一个都没带）**：0.5.5 的 `GunRustEnchantment` 重写了 `isCurse() → true` 与 `isTreasureOnly() → true` ✓（扫描整个 enchantment 包实测：它是 15 个附魔里**唯一**带这两个标记的 ✓）。而 1.21 **删掉了这两个方法** ✗（`javap` 核对 ✓）—— 现在由**标签**决定：`Enchantment.getFullname` 字节码里是 `holder.is(EnchantmentTags.CURSE)` ⇒ 在 `#minecraft:curse` 里名字才显示**红色** ✓；`GrindstoneMenu.removeNonCursesFrom` 也只保留 `#curse` 成员 ⇒ 只有诅咒**不会被砂轮剥掉** ✓。我们上一轮只带了 `non_treasure.json`（15 个全列 ✓，修的是附魔台/图书管理员 ✓）⇒ 顺带两个错：`gun_rust` **不是诅咒** ✗，而且还被列进 non_treasure ✗（0.5.5 是宝藏专属、本不该进附魔台/村民交易 ✓）。修法：新增 `curse.json` / `treasure.json` / `on_random_loot.json`（各含 `scguns:gun_rust` ✓；后者的依据是原版把两个诅咒也列在 `#on_random_loot` 里 ✓，而移出 non_treasure 会让它掉出该标签 ✗），并从 `non_treasure.json` 移除它 ✓。**运行期实测**（临时 `ServerStartedEvent` 诊断，验证后已删 ✓）：`gun_rust curse=true treasure=true non_treasure=false table=false tradeable=false loot=true`，其余 14 个附魔归属**完全没变** ✓（上一轮修好的"附魔台能附魔枪械 + 图书管理员卖书"未被破坏 ✓），`vanilla curse tag=3` = 绑定诅咒 + 消失诅咒 + gun_rust ✓。怪物枪上的 85% 掷咒走 `GunCurseUtil` 自己掷骰、与标签无关 ⇒ **完全不受影响** ✓。`verify_installed_jar` 新增 4 项（含一条反向检查 ✓）：修复前 jar **84/88（新增 4 项全 FAIL）** ✓、新 jar **88/88** ✓。见 HANDOFF §37 |
| **女仆兼容（TLM）内置** | ✅ **已内置（玩家请求，嵌套 jar 方式）**：把玩家 1.20.1 的女仆兼容（43 文件 / 7,756 行）对着本移植重编后，作为**独立 mod jar 嵌套**进主 jar（`META-INF/jarjar/...` + `metadata.json` ✓，已实测在 `build/libs/scguns-0.5.5.jar` 与**已安装的 jar** 里都存在 ✓）⇒ 单文件分发，但兼容仍是独立 modid / 独立 mixin 配置 / 独立类加载器 ✓。**原 1.21 版用不了的根因（实测）**：它对着官方 SC2 1.2.5/1.5 编、`mods.toml` 写 `scguns >= [1.2.0,)` ✗，而本移植自报 0.5.5 ⇒ 依赖不满足、NeoForge 根本不加载 ✓（玩家 1.20.1 的成品 jar 写的是 `[0.5.0,)` ✓）。移植量实测 **259 → 110 → 52 → 0** 个编译错误（机械改写 192 处用 `tools/port_rewrite_maid.py`，其余真漂移全部改用本移植已有的 `NbtHelper`/`ScEnchants`/`Caps`/`ScEffects`/`MobType` ✓）。**"没装 TLM 就完全不加载"三层守卫**（依赖 optional + 入口 `ModList.isLoaded` 提前 return + `MaidMixinPlugin` 整份门控 mixin，并把 6 个 `@EventBusSubscriber` 改成显式注册 ✓）：同一个 jar 实测 **有 TLM ⇒ `Done` + 注入 9 个 mixin + 0 ERROR；无 TLM ⇒ `Done` + 注入 0 个 + 0 ERROR** ✓，另有 `tools/audit_tlm_isolation.py` 断言宿主零 TLM 引用（0/0 ✓）。顺带修掉 **3 个 1.20.1 时代就存在的 bug**：`ProjectileEntityMixin` 的 `@Redirect` 签名错（尾参数应为目标方法参数 `Vec3,Vec3` ⇒ 该重定向从来没生效过 ✓）、`EntityMaidShieldMixin` 还在用 SRG 名 `m_6469_`/`m_8119_` ✓、**5 个进度的触发 id 写成 `maid/tamed_maid`（TLM 实际注册的是 `maid_event`）⇒ 从来没弹出过** ✓；另修 NeoForge 的 `@EventBusSubscriber` 更严导致崩启动、mixin 插件用 `Class.forName` 探测导致 geckolib `LivingEntity` 过早加载、以及 TLM 相关数据在无 TLM 时的 6 条 ERROR（进度加 `neoforge:conditions`、标签条目改 `required:false` ✓）。**取舍**：Cloth Config 配置屏删除（实例里没装该前置，配置项仍生效 ✓）、`SulfurheadEntityMixin` 删除（宿主已按玩家要求去掉"只受玩家伤害"，该 `@Overwrite` 已无目标 ✓）。**游戏内表现未验证**（需玩家看客户端 ✓）；已知风险：能量枪回充依赖插件 mod 注册 `Capabilities.EnergyStorage.ITEM`，无法离线验证 ✓。见 HANDOFF §36 |
| **枪械工作台支持原版配方书** | ✅ **已实现（玩家想法，范围＝只做 GunBench）**：1.21 的 `RecipeBookMenu` 是**抽象类**（8 个抽象方法 ✓），而"配方书类型/分类"在 NeoForge 里是**可扩展枚举**（`IExtensibleEnum` ✓）⇒ 用 `META-INF/enumextensions.json` 加了 `SCGUNS_GUN_BENCH` 与两个分类 ✓（常量只能 `valueOf` 取 ✓，客户端分类的图标由 `EnumProxy` 提供 ✓）。自定义配方靠 `RegisterRecipeBookCategoriesEvent#registerRecipeCategoryFinder` 进分类 ✓（原版 `ClientRecipeBook` 只认 crafting/cooking ✗）。网格设计：`GunBenchRecipe` 按下标匹配原料 ✓、`ServerPlaceRecipe` 按 `getSlot(i)` 寻址 ✓ ⇒ 声明 1×10 网格，蓝图槽由自定义 `handlePlacement` 在 `super` 之后放入 ✓（并抑制这一轮工作台自身的 auto-craft ✓，否则会按蓝图里的旧配方清空重填 ✗）。顺带修掉 **0.5.5 的真 bug**：蓝图槽被 `addSlot` 两次 ✓，且 `quickMoveStack` 的范围 `11..12` 实际指向输出槽 ✗。解锁数据：原本**一条 `advancement/recipes/**` 都没有** ✗（配方书只会是空的 ✓）⇒ 脚本生成 **146 条**，触发条件是"玩家拥有该配方对应的蓝图" ✓（贴合蓝图分级的进度设计 ✓）。**验证**：build ✓、20 审计 0 失败 ✓、专用服务器 **146 条进度零解析错误 + 配方零错误 + 无 ERROR/FATAL** ✓；**界面本身需玩家在游戏里看** ✗（开关、页签、ghost recipe 位置、点击一键填充 ✓）。⚠️ **首版让玩家客户端崩过**：`No enum constant ... SCGUNS_GUN_BENCH_SEARCH` ✗（枚举扩展没生效、而事件里抛异常＝mod 加载失败 ✓）。**根因已找到并修好**：**`neoforge.mods.toml` 少了一行 `enumExtensions="META-INF/enumextensions.json"`** ✗ —— NeoForge **不会自动扫描**这个文件（对照 Farmer's Delight 的 `mods.toml` 才发现的 ✓，这也解释了"故意把 JSON 写坏却毫无报错" ✓）。补上后**服务端实测生效**：`recipeBookTypes=[..., FARMERSDELIGHT_COOKING, SCGUNS_GUN_BENCH]` ✓。同时加了**兜底**（取不到常量只记 WARN 并跳过 ✓、屏幕在不可用时**干脆不显示配方书** ✓，绝不回退到"书里显示原版工作台配方" ✗）⇒ **不会再崩、也不会误导** ✓。客户端分类那半仍需一次进游戏确认 ✓。**玩家实测反馈后修的两处**（§39.6）：① **书里只有 2 条配方**（应为 11 条铜级 ✓）—— 我用的 `inventory_changed` 判据写法是对的 ✓（与原版 `salvage_sherd.json` 同形 ✓），但它**只在背包变化时触发** ✗，而玩家早就拿着铜蓝图 ⇒ 永不触发 ✓（显示的是 4 条"无蓝图、tick 兜底"里的 2 条 ✓）；改为**登录时扫背包**，把已持有的蓝图对应配方直接 `awardRecipes` 解锁 ✓，新蓝图仍走拾取即解锁 ✓。② **ghost 投影错位一格** —— 原版把结果画在 `slots.get(0)`、`placeRecipe` 槽位从 0 起算并在经过输出槽时跳一格 ✓ ⇒ 它假定**输出槽＝菜单第 0 槽** ✓，而我们是"模块 0..9/蓝图 10/输出 11" ✗；已把槽序改成 **0=输出、1..10=模块网格、11=蓝图** ✓ 与之一致 ✓（连带同步 `MENU_SLOT_*`、`getResultSlotIndex`、`shouldMoveToInventory`、shift 左键范围 ✓），因此**不需要**自定义 `RecipeBookComponent` ✓。build ✓、20 审计 0 失败 ✓、88/88 ✓、已装实例 ✓，效果待玩家确认 ✓。 |
| **【崩溃】硫磺中毒 + 火焰伤害 ⇒ ClassCastException** | ✅ **已修并实测**（玩家日志/崩溃报告里发现）：`SulfurPoisoningEffect.getFireDamageMultiplier` 照 0.5.5 写了 `(SulfurPoisoningEffect) effect.getEffect()` ✗，而 **1.21.1 的 `MobEffectInstance#getEffect()` 返回 `Holder<MobEffect>`** ✗ ⇒ 强转 Holder ⇒ 异常在**受伤事件**里抛出 ⇒ 被当作 `Ticking entity` ⇒ **整个游戏崩** ✗（触发条件很普通：身上有硫磺中毒的实体吃火焰伤害 ✓）。改为 `effect.getEffect().value() instanceof ...` ✓（取真值 + 兜底 ✓）。**实测**：dev 服务器上给僵尸 `scguns:sulfur_poisoning` 再 `/damage ... minecraft:in_fire` ✓（先核对过处理器只对 IN_FIRE/ON_FIRE/LAVA/HOT_FLOOR 生效 ✓，保证真的走到那一行 ✓）⇒ 无异常、服务器存活 ✓。全仓库同类写法只此一处（grep 实测 ✓，另两处早就是 `.value()` ✓）。见 HANDOFF §45.1 |
| **【存档丢失】枪械工作台里的东西从未被保存** | ✅ **已修并实测**：`GunBenchBlockEntity.saveAdditional` 对全部 12 个槽位**无条件** `ItemStack.save` ✗，而 1.21 的 `save` 对**空栈**抛 `Cannot encode empty ItemStack` ✗ ⇒ 工作台必有空槽 ⇒ **每次区块保存都抛异常**、实体被丢弃（日志原话 "It will not persist" ✓）⇒ **里面的东西存不下来** ✗。改为跳过空槽 ✓（读取侧本来就把缺失 tag 当空 ✓）。**旁证**：模组自己的 `SupplyScampEntity`（:735）早就有这道空栈判断 ✓ ⇒ 是移植工作台时漏掉的守卫 ✓。**实测**：`setblock` 放工作台 + `data merge block {Item0:{id:"minecraft:stone",count:1}}` + `save-all flush` ⇒ 无报错 ✓。见 HANDOFF §45.2 |
| **带附魔的配件装上就消失** | ✅ **已修（根因实测 + 修复已实测，玩家："直接修"）**：玩家给的线索精准 ✓ —— **两边都没了**（枪上没有、背包也没回来 ✓ = 栈被销毁 ✓）、**只有带附魔的** ✓、**放进去立即** ✓。临时探针实测根因 ✓：`ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack)` 对**带附魔**的栈**直接失败** ✗（`DataResult.Error['Can't access registry minecraft:root / minecraft:enchantment']` ✓ —— 1.21 的附魔按**注册表引用**编码，`NbtOps` 没有注册表访问权 ✓），而 `NbtHelper.tagFromItem` 把失败**静默**变成空标签 ✗ ⇒ `AttachmentContainer.slotsChanged` 把 `Attachments.<类型>` 写成 `{}` ⇒ 读回来空栈 ⇒ **配件被销毁** ✓✓（三条症状一次全解释 ✓）。**这不是新 bug**：该 helper 一直用 `NbtOps` 而非 `RegistryOps` ✗，只是 §57 之前配件不可能带附魔 ⇒ 从未被触发 ✓（潜伏的静默数据丢失 ✓）。**修法**：① `NbtHelper` 加 `volatile HolderLookup.Provider` + `RegistryOps.create(NbtOps.INSTANCE, provider)` ✓（解码同样需要 ✓），失败时**打 ERROR 日志** ✓ 而不是静默兜底 ✓；② 新增 `event/RegistryAccessListener`（`LevelEvent.Load` + `ServerStartedEvent` ✓ 覆盖单人/服务器/连服 ✓）负责写入 provider ✓；③ `AttachmentContainer.slotsChanged` 改为"**任一非空配件编码失败就整块放弃**" ✓（保留枪上原有 `Attachments` ✓，绝不写入空标签 ✓）。**复测**：同一探针 ⇒ 修复前 `helperTagSize=0 / roundTripEmpty=TRUE` ✗ → 修复后 `providerSet=TRUE / helperTagSize=3 / roundTripEmpty=false / roundTripEnchanted=TRUE` ✓✓。打包核对：探针不在 jar ✓、监听器在 ✓、`NbtHelper.class` 含 `RegistryOps` ✓；`verify_installed_jar` **140/140** ✓、已安装（备份 `.bak-141412`）✓。见 HANDOFF §58 |
| **枪械/配件的原版附魔 + 配件吃经验修补** | ✅ **已实现（玩家定规则：只有配件自己带经验修补才修）**：玩家先问"枪现在能吃耐久/经验修补吗"、"装上的配件吃不到经验修补" ✓，并更正了我上一轮的顾虑 ✓（经验修补是**宝藏附魔** ✓、附魔台拿不到 ✓ ⇒ 不存在刷出经验修补的问题 ✓；耐久可以 ✓）。**实测**：原版标签 `treasure` 含 mending ✓、`non_treasure`/`in_enchanting_table` 不含 ✓；1.20.1 的 `EnchantmentCategory.BREAKABLE.canEnchant` **只调 `Item.canBeDepleted()`** ✓（javap ✓）⇒ 当年**任何有耐久的物品**都能附耐久/经验修补 ✓ ⇒ 移植弄丢了 ✗（与 §56 同根因 ✓）。**配件吃不到经验修补的机制**：原版在 `ExperienceOrb.repairPlayerItems` 里用 `getRandomItemWith(REPAIR_WITH_XP, player, …)` 只找**背包/装备** ✓，而配件存在**枪自己的标签**里（`Attachments.<类型>` ✓）✗ ⇒ 结构上看不见 ✓。**改动**：(a) 生成器收集注册里写了 `durability(...)` 的物品（实测 **218 件** ✓：枪/配件/工具模具 ✓）写进 `#enchantable/durability` + `vanishing`（共 220 条 ✓）；(b) 新增 `AttachmentMendingHandler` ✓ 挂 `PlayerXpEvent.PickupXp`（该事件在 `playerTouch` 顶部、原版修理之前 ✓），**只修自带 `minecraft:mending` 的配件** ✓，用**原版同一公式** ✓ 修完写回枪标签 ✓，且**不扣经验球经验** ✓（不干扰枪自身的经验修补 ✓；要改成共用经验池需接管拾取流程 ✓，玩家要就说 ✓）。**实测**：枪 `Applied enchantment Unbreaking I` ✓（修复前拒绝 ✓）、配件 `Applied enchantment Mending` + `Unbreaking I` ✓。门禁新增 4 项：上一版 **128/140** ✓、新 jar **140/140** ✓。**未验证** ✗：真正"捡经验球 → 配件耐久回升"（需真玩家 ✓，已给测试步骤 ✓）。见 HANDOFF §57 |
| **战利品里的盔甲没有附魔** | ✅ **已修（玩家报告，已实测验证）**：战利品表用了 `enchant_with_levels`（10 处）+ `enchant_randomly`（26 处）✓，而 1.21 里"某附魔能否附到某物品"**完全由原版物品标签** `#minecraft:enchantable/*` 决定 ✓（函数内部是 `EnchantmentHelper.enchantItem(…, options)` ✓，`only_compatible` 默认 true ✓）—— **移植一个 enchantable 标签都没带** ✗（实测 `data/minecraft/tags/item/` 下只有 swords/axes/... 没有 enchantable 目录 ✓）⇒ 找不到任何兼容附魔 ⇒ **掉出来是白板** ✓。0.5.5（1.20.1）用 `EnchantmentCategory`，**任何 `ArmorItem` 都算 `ARMOR`** ✓ ⇒ 当年模组护甲能附魔 ✓。修法：新增生成器 `tools/gen_enchantable_tags.py` ✓ 产出 8 个标签（armor 37 ✓、head 12 ✓、chest 9 ✓、legs 8 ✓、feet 8 ✓、durability/equippable/vanishing 各 37 ✓，全部 `replace:false` 合并 ✓）。**武器那条经实测不成立** ✓：移植带了 `data/minecraft/tags/item/swords.json` = `scguns:anthralite_sword` ✓，而 1.21 的 `#enchantable/sword` 就是 `#minecraft:swords` ✓ ⇒ 模组近战武器本来就可附魔 ✓。**运行期实证**：`loot spawn … loot scguns:raids/copper_boss` ⇒ `Scrap Helmet` 带 `minecraft:enchantments:{minecraft:unbreaking:3}` ✓（修复前白板 ✓）。门禁新增 8 项：上一个 jar **126/136** ✓、新 jar **136/136** ✓。**未验证** ✗：附魔台界面（纯客户端 ✓）会因此开始给模组护甲提供附魔 ✓（= 0.5.5 行为 ✓）。见 HANDOFF §56 |
| **supply_crate 破坏不出弹药（+28 张表同因）** | ✅ **已修（玩家报告，已实测复现与验证）**：复现 `loot spawn … mine` ⇒ `Dropped 1 [Supply Crate]` ✗（掉的是箱子本身 ✓）。**根因**：1.20.5 把物品谓词里的附魔挪进了组件式谓词 ✓ —— 旧写法 `predicate.enchantments` 在 1.21 **不报错但被静默忽略** ✗（Mojang codec 忽略未知字段 ✓）⇒ 解析成**空谓词** ⇒ **匹配一切** ✗ ⇒ "精准采集"分支永远成立 ✓。改成原版 `blocks/stone.json` 的写法 `predicate.predicates["minecraft:enchantments"]` ✓。**影响 28 张表** ✗：`supply_crate`（永远掉自己 ✓）+ 矿石 ✓ + 20 种硝化玻璃 ✓ —— 矿石**空手就掉矿石方块**、玻璃空手也掉自己 ✓（= 白送精准采集 ✓）。**实测对照**：修复后箱子掉 `Copper Flare ×2 / Advanced Round ×4 / Microjet ×4` ✓、空手挖矿掉 `Raw Anthralite` ✓、精准采集镐掉 `Anthralite Ore` ✓。**顺带修**：`shulker_core_from_shulker` 用了 1.21 **已删除**的条件类型 `minecraft:random_chance_with_looting` ✗（开机日志一直在报 WARN ✓）⇒ 那条"潜影贝掉 shulker_core（5%+每级抢夺 2%）"**从来没生效过** ✓，改为 `minecraft:random_chance_with_enchanted_bonus`（字段名从源码核实 ✓）✓，`/reload` 后警告消失 ✓。改动：`tools/fix_loot_item_predicates.py`（28 表，逐表只改谓词几行 ✓，`git diff` 28 文件 +287/−229、无整文件重排 ✓）+ 手工改那个修饰器 ✓。门禁新增 2 项（包里不得再有旧谓词写法、不得再用已删除条件类型）：修好前 **126/128（两项 FAIL）** ✓、新 jar **128/128** ✓。**未验证** ✗：20 种玻璃未逐个开箱（与矿石同模式 ✓）。见 HANDOFF §55 |
| **神枪手（puncturing）描述与效果不符** | ✅ **已修文案（玩家点名；实测是上游就有的问题）**：玩家报"实际不降低伤害，但描述写了" ✓ —— **正确** ✓。实测两棵树并排读：`getPuncturingDamageReduction(...) { return damage; }` **在 0.5.5 与移植里逐字相同** ✓ ⇒ "but decreases base damage" 这一条**从来没实现过** ✗（**不是**移植改坏的 ✓）。描述的另外两条**确实在** ✓：暴击几率（`getPuncturingChance` → `GunModifierHelper:293` ✓）、穿甲 `5.0F×等级` ✓（4 处调用 ✓）。按"行为对齐 0.5.5"的原则**只改文案** ✓：EN 去掉 ", improves armor penetration but decreases base damage" 里的假半句 → `Increases chance of critical hits and improves armor penetration` ✓；ZH `增加暴击几率，提高穿甲但降低基础伤害` → `增加暴击几率，提高穿甲` ✓（两文件同步 ✓，`audit_lang_keys` 仍 **1806/1806** ✓、`audit_lang_values` 格式占位符 **0 处不一致** ✓）。**顺带发现描述漏了两条真实效果** ✗（0.5.5 亦然）：rate 修正 `1 + 0.06×等级`（与 `trigger_finger` 同一乘数 ✓）、后坐力 `+0.1×等级` ✓ —— 要不要补进描述、或反过来**实现**那条降伤害（= 玩法改动、偏离 0.5.5 ✗），等玩家指示 ✓。build ✓、126/126 ✓、已装（上一版备份 `.bak-123758` ✓）。见 HANDOFF §54 |
| **0.5.5 战利品注入完全没生效（矿井/地牢开不出古典武器）** | ✅ **已修（玩家报告，纯路径问题）**：注入文件一直存在 ✓，只是**放错了命名空间** ✗ —— NeoForge 21.1 的 `LootModifierManager.prepare` **只读一个路径** ✓（`javap -c` 实测：`fromNamespaceAndPath("neoforge", "loot_modifiers/global_loot_modifiers.json")` ✓，并用 `getResourceStack` 合并各数据包的同名文件 ✓），而移植放在 `data/scguns/loot_modifiers/global_loot_modifiers.json` ✗ ⇒ **整份列表无人读取** ⇒ `add_loot_dungeon`/`add_loot_mineshaft` 等 **14 条修饰器一条都没生效** ✗（连带 `pebbles_from_gravel`、`shulker_core_from_shulker` 也是死的 ✓）。修法：列表移到 `data/neoforge/loot_modifiers/`（内容一字未改 ✓）并删除旧路径死文件 ✓；其余（`scguns:add_loot_table` 类型 ✓、`neoforge:loot_table_id` 条件 ✓、`scguns:chests/scguns_dungeon` 自定义表 ✓）本来就对 ✓。**只放一份** ✓：参考移植同时留了 `forge/` 与 `neoforge/`，但实测 21.1 只读 `neoforge/` ✓，两份都放若被读到会双倍注入 ✗。**运行期实证**（新增 `tools/rcon_verify_loot_injection.py` ✓：把地牢箱子表在世界里开 **30 次**再精确断言 ✓）：出现 **`scguns:flintlock_pistol`、`scguns:longarm`**（古典系列 ✓）与 `powder_and_ball`、`grapeshot` ✓ ⇒ **注入已生效** ✓。`verify_installed_jar` 新增 2 项：上一个 jar **123/126（两项 FAIL）** ✓、新 jar **126/126** ✓。**未验证** ✗：其它结构（要塞/古城/堡垒等，同一机制、只是 `loot_table_id` 不同 ✓，未逐个开箱 ✓）。见 HANDOFF §51 |
| **枪械附魔互不冲突 + 能附出剑的附魔（腐蚀）** | ✅ **两条都已修（玩家报告）**：1.21 把"能附到哪类物品"和"附魔间互斥"都挪进了**数据** ✓，而移植两样都没带 ✗ ⇒ **15 个附魔的 `exclusive_set` 全缺失** ✗（所有枪械附魔随便叠 ✓）、`corroded.json` 的 `supported_items` 被写成 `#scguns:enchantable/guns` ✗ ⇒ **枪能附腐蚀** ✓（0.5.5 里它用的是**原版 `EnchantmentCategory.WEAPON`** = 剑/斧近战 ✓，并与原版伤害附魔互斥 ✓）。修法：从 0.5.5 源码恢复两套规则并数据化 ✓ —— ① 互斥：`GunEnchantment.checkCompatibility` 规定**同 `Type` 即互斥** ✓，四个组 = `WEAPON`(banzai/elemental_pop/gun_rust/lightweight)、`AMMO`(reclaimed/shell_catcher)、`PROJECTILE`(accelerator/collateral/heavy_shot/hot_barrel/puncturing/waterproof)、`RELOAD`(quick_hands/trigger_finger) ⇒ 生成 4 个 `tags/enchantment/exclusive_set/*` ✓（每组成员**含自己** ✓，与原版 damage 组写法一致 ⇒ 互斥双向 ✓），每个附魔引用自己那组 ✓；② 类别：`corroded` → `#minecraft:enchantable/sharp_weapon` ✓（不用 `weapon`，后者含重锤而 0.5.5 时代没有 ✓）；③ **另外三条 0.5.5 里带否定条件**的类别（`trigger_finger` 排除 `#single_shot` ✓、`shell_catcher` 排除 `#does_not_eject_casings` ✓、`collateral` 排除 `#non_collateral` ✓）被移植一律放宽成"所有枪" ✗ ⇒ 生成物化标签（实测 **132/126/135** 成员 = 141 把枪减去对应标签 ✓）。生成器 `gen_enchantments.py` 同步改造（源指向 0.5.5 参考树 ✓、解析 `Type` ✓、生成两类标签 ✓），并修掉它自己的一个坑 ✗：`ModEnchantments` 改用 `ResourceKey` 后旧正则匹配不到 ⇒ 它会自编 id 写出 `water_proof.json` 与真正的 `waterproof.json` 并存 ✓（已删多余文件 ✓，改读 `ResourceKey<Enchantment> NAME = key("id")` ✓）。**运行期实证**（`tools/rcon_verify_enchant_conflicts.py` ✓：僵尸持枪 + `/enchant`）：**7/7 全过** ✓ —— 枪拒绝腐蚀 ✓、枪接受 heavy_shot ✓、同组 accelerator 被拒 ✓、异组 quick_hands 通过 ✓、火枪拒绝 trigger_finger ✓、普通枪接受 ✓、**剑接受腐蚀** ✓（注：原版"拒绝"提示原文就是 `incompatible` 的 "%s cannot support that enchantment" ✓，第 3/4 条同枪对照 ⇒ 只可能是互斥组生效 ✓）。`verify_installed_jar` 新增 **9 项**：上一个 jar **115/124（9 项 FAIL）** ✓、新 jar **124/124** ✓。**未验证** ✗：附魔台/铁砧/村民界面里的候选显示（纯客户端 ✓）。见 HANDOFF §50 |
| **撤回：冷却指示器不该显示（我改错了）** | ✅ **已撤回（玩家指出 0.5.5 里根本不显示）**：§45.3 里我把 `renderCooldownIndicator` 的两个毛病（用了 1.21.1 已删除的 `textures/gui/icons.png` ✗、自建 `GuiGraphics` 从不 flush ✗）当成"移植漏修" ✗，改成 GUI 图层 + 原版 sprite ⇒ **让一个 0.5.5 从未显示过的功能显示了出来** ✗ = 引入偏离 ✗。**实测证据三条**：① 玩家实机记忆（0.5.5 不显示 ✓）；② 反汇编**真 0.5.5 jar**（`ScorchedGuns-0.5.5-1.20.1.jar` ✓）：`GunRenderingHandler` 整个类**零处 `flush()`** ✓ ⇒ 它的野 `GuiGraphics` 写进 BufferSource 后无人提交 ⇒ **画了等于没画** ✓（与 §28.5 闪光弹遮罩同一个坑，只是这次 0.5.5 就已经是坏的 ✓）；③ 贴图在 1.20.1 **存在** ✓ ⇒ 当年不显示只因没提交 ✓。（附注：参考移植 SC2 **1.5** 里它是能显示的 ✓，但基准是 0.5.5 ✓。）**改动**：删除 §45.3 新建的 `GunCooldownOverlay` ✓、删除 `handleRenderTick` 与其 `RenderFrameEvent.Post` 监听（唯一职责就是它 ✓）与随之无用的 3 个 import ✓；`Config.cooldownIndicator` 配置键**保留**（0.5.5 就有 ✓，删掉会破坏既有配置 ✓）但注释如实写明"0.5.5 里也是无效的、本移植同样不绘制" ✓。`verify_installed_jar` 把 §45.3 那两项换成"**指示器不再随包发布**"+"**不再引用已删除的 icons 贴图**" ✓：带 overlay 的 jar **114/115（新检查 FAIL）** ✓、新 jar **115/115** ✓。**教训**：坏掉的功能不一定是 bug ✓ —— 动手前先确认"0.5.5 里它是工作的吗" ✓。见 HANDOFF §48 |
| **女仆兼容的 Cloth Config 配置界面补回** | ✅ **已补（玩家要求）**：§36.6 里删掉配置屏是**有意取舍** ✗（当时 `me.shedaniel.clothconfig2` 既不在依赖、也不在实例里 ⇒ 只能编辑 `config/scg2_maid_compat-common.toml` ✓）；玩家现在装了 `cloth-config-15.0.140-neoforge.jar` ✓ ⇒ 界面补回 ✓。做法比上游更好：**用 TLM 自己的 `AddClothConfigEvent`**（把条目加进 TLM 的设置界面 ✓），而不是上游那个 Forge 专有类 `ConfigScreenHandler` ✗（NeoForge 上不存在 ✓，也正是移植脚本删掉它的原因 ✓）。**实测确认**（`javap`）：TLM 1.5.3 仍有 `AddClothConfigEvent#getRoot/#getEntryBuilder` ✓，且它是在 `compat.cloth.MenuIntegration` 里用 **`NeoForge.EVENT_BUS`** 发的 ✓（`javap -c` 看到 `getstatic NeoForge.EVENT_BUS` + `IEventBus.post` ✓）⇒ 用既有的 `addListener` 风格注册即可 ✓。屏幕文件从 1.20.1 上游**原样取回**（270 行 ✓），只改 2 处 Forge 引用（`ForgeRegistries.SOUND_EVENTS` → `BuiltInRegistries.SOUND_EVENT` ✓）。**顺带补齐**上游屏缺的两个选项 ✓（`FOLLOW_LEASH_RADIUS` ✓、今天新增的 `IDLE_RELOAD_DELAY` ✓），并新增工具 `tools/check_maid_config_screen.py` 盯覆盖（实测 **39/39、0 遗漏** ✓）。**三道守卫**：TLM 守卫（既有 ✓）、Cloth Config 守卫（没装就不注册、两个类都不加载 ✓，否则方法签名里的 Cloth 类型会 NoClassDefFoundError ✓）、依赖声明 **optional + side=CLIENT** ✓（写 required 会让没装的人开不了游戏 ✗）；TLM 自身既不 shade 也不依赖 Cloth Config（实测 ✓）⇒ 这道守卫必须自己做 ✓。验收：兼容单独编译 **43 文件 0 error** ✓、mixin 检查 0 问题 ✓、宿主零 TLM 引用 ✓、`gradlew build` ✓、`verify_installed_jar` 新增 2 项（读嵌套 jar 内部 ✓）⇒ 上一个 jar **113/115（两项 FAIL）** ✓、新 jar **115/115** ✓。**未验证** ✗：界面本身（纯客户端 ✓）。见 HANDOFF §47 |
| **女仆兼容：空闲补弹立刻换弹（上游 bug）** | ✅ **已修（玩家指出是上游 1.20.1 附属原有问题）**：`MaidSC2GunIdleReloadTask` 的内存要求是 `ATTACK_TARGET = VALUE_ABSENT` ⇒ **该行为一运行就代表"当前没有敌人"** ✓，而 `tick()` 里条件成立就**直接换弹** ✗ ⇒ 目标一消失（人还没倒下）就开始拉栓 ✗。改为**无敌人满 `idle_reload_delay_ticks`（默认 100 刻 = 5 秒）** 才开始补弹 ✓：计数用**本行为自己运行的刻数**（它只在无目标时运行 ⇒ 等价于"敌人离开多久" ✓，无需额外时间戳 ✓）；敌人一出现 ⇒ 战斗行为抢占 ⇒ `stop()` 清零 ⇒ **重新计时** ✓（"连续无敌人 5 秒" ✓）；"正在换弹就继续"这条仍优先于等待 ✓（不会卡住进行中的换弹 ✓）。配置项在 `reload` 段 ✓：`idle_reload_delay_ticks`（0..1200，**0 = 恢复上游行为** ✓）。验收：兼容单独编译 **41 文件 0 error** ✓、mixin 检查 0 问题 ✓、宿主零 TLM 引用 ✓、主工程 build ✓、`verify_installed_jar` 新增 2 项（**读嵌套 jar 内部** ✓）⇒ 上一个 jar **111/113（两项 FAIL）** ✓、新 jar **113/113** ✓。**未验证** ✗：实际计时（需要"女仆拿枪 + 打完最后一只敌人"，专用服务器上没有可指挥的女仆 ✓）。**
| **【画不出+刷屏】枪械冷却条** | ✅ **已修**：`renderCooldownIndicator` 用了 **1.21.1 已不存在的 `textures/gui/icons.png`** ✗（1.20.2 起 GUI 图标改成图谱 sprite ⇒ 每 tick/每帧 `FileNotFoundException` 刷屏 ✓、冷却条永远不显示 ✗），并且**自建 `GuiGraphics` 从不 flush** ✗（与 §28.5 闪光弹遮罩同一坑 ✓），还被 `handleClientTick` 与 `handleRenderTick` **各调一次** ✗。改为 **GUI 图层** `GunCooldownOverlay`（`RegisterGuiLayersEvent.registerAboveAll` ✓，同一套做法见 §28.5.3 ✓），贴图改用原版现成的 `hud/crosshair_attack_indicator_background/_progress` ✓（九参 `blitSprite` 按进度裁宽 ✓，位置与缩放与 0.5.5 一致 ✓），并删掉死常量与 4 个无用 import ✓。`verify_installed_jar` 新增 2 项：上一个 jar **109/111（两项 FAIL）** ✓、新 jar **111/111** ✓；画面仍需玩家确认 ✗。**顺带修了我自己的一条误导日志**：页签统计把聚合页（搜索页）先数掉 ⇒ 打印成 `SEARCH=146 MISC=0 …` ✗，让人误以为页签没做对；其实 `SEARCH=146` 正说明三个分组页合起来有 146 条 ✓，页签是正常的 ✓。日志已改为**跳过聚合页** ✓。见 HANDOFF §45.3/§45.4 |
| **炮塔 / 外骨骼单独页签** | ✅ **已实现（玩家要求）**：配方书原来只有"搜索页 + 一个通用页（146 条全塞进去）"⇒ 找炮塔/外骨骼要翻页 ✗。现在拆出**炮塔**与**外骨骼**两个独立页签 ✓（其余 138 条仍在通用页 ✓）。分类规则**按产出物**而不是配方 id 或文件目录 ✓，并放在 **common** 包（`GunBenchBookTabs` ✓）：外骨骼用类型判断 ✓（四条护甲都是 `ExoSuitItem` ✓）；炮塔**没有共同基类** ✗（四个类都直接继承 `BaseEntityBlock` ✓，实测 ✓）⇒ 只能按方块身份列四家 ✓（注释写明 + 打包检查兜住漂移 ✓）。客户端侧按 §39.5 的教训**两处同改** ✓：`enumextensions.json` 新增 `SCGUNS_GUN_BENCH_TURRET`（图标＝炮塔方块 ✓）/`SCGUNS_GUN_BENCH_EXO_SUIT`（图标＝外骨骼胸甲 ✓）+ `ScgunsRecipeBookCategories` 按名字解析 ✓ + 注册 `[搜索,通用,炮塔,外骨骼]` ✓（搜索页聚合后三个 ✓，分类器按 `GunBenchBookTabs.of` 分流 ✓；两个新页签取不到就退回通用页、**不会**整本书消失 ✓）。启动日志会打印四个分类是否解析成功（`tabs=[search=ok misc=ok turret=ok exo_suit=ok]` ✓）。**实测**：临时 `ServerStartedEvent` 探针（跑完已删 ✓ 且由打包检查确保不再进 jar ✓）在专用服务器上用**真分类代码**量出 **GUNS 138 / TURRETS 4 / EXO_SUIT 4 = 146** ✓✓；`/scguns recipebook` 新增"页签"一行（同一份 common 代码，服务端可见 ✓）。`verify_installed_jar` 新增 **9 项**（两个新分类已声明 ✓、客户端确实解析 ✓、分页行语言键 ✓、两页各 4 条 ✓、探针不打包 ✓），自测上一个 jar **105/109（4 项 FAIL）** ✓、新 jar **109/109** ✓；build ✓、审计 0 失败 ✓、语言文件各 1806 条对齐 ✓。**未验证** ✗：页签画出来的样子（纯客户端 ✓，需玩家看 ✓）。见 HANDOFF §44 |
| **四座炮塔的配方书解锁方式** | ✅ **已改（玩家要求：用 `scguns:turret_platform` 解锁）**：146 条枪械工作台配方里**只有这 4 条没有蓝图** ✓（`auto/basic/shotgun/sniper_turret_from_gun_bench` ✓），而它们当时挂的是 **`minecraft:tick` 兜底** ✗ ⇒ **一进世界就白送四条配方** ✗、平台在解锁上毫无作用 ✗。读配方数据发现：**平台本来就是这四条的必需材料** ✓（都写在 `gun_grip` 位 ✓）⇒ 它就是"炮塔的蓝图" ✓，玩家要求正确 ✓。改法：把"钥匙物品"的定义**收成一处**（新增 `common/recipe/GunBenchUnlockKeys` ✓：有蓝图用蓝图、没有用炮塔平台 ✓），三处共用 ✓ —— ① 数据：4 条的解锁条件由 `tick` 改成 `inventory_changed` + `items:[scguns:turret_platform]` ✓（与其余 142 条形状一致 ✓，生成器同步更新并会打印按钥匙分组的条数 ✓，`git diff` 实测**只动这 4 个文件** ✓）；② 登录扫描：不再跳过无蓝图配方 ✓（带平台的老玩家登录即补上 ✓）；③ `/scguns recipebook`：分组键改成钥匙物品 ✓ ⇒ 这 4 条显示在"炮塔平台"一行 ✓（旧的"无需蓝图"语言键已无用，两个语言文件同步删除 ✓，提示语也补上"四座炮塔由炮塔平台解锁" ✓）。验证：新增 **2 项打包检查**（`advancement/recipes/**` 里**没有任何**进度带 `minecraft:tick` ✓、四个炮塔进度都存在且引用平台 ✓），自测上一个 jar **98/100（两项全 FAIL）** ✓、新 jar **100/100** ✓；build ✓、`runServer` 到 `Done` + 0 ERROR/FATAL + 进度/配方零解析错误 ✓。**未验证** ✗：游戏内"拿到平台才解锁"这条链路（解锁存在存档里，现有存档那 4 条仍是已解锁 ✓，要复现需新世界或先 `/recipe take` ✓）。见 HANDOFF §43 |
| **配方书 ghost 不投影蓝图槽** | ✅ **已修（玩家实测反馈：蓝图槽的空位也要投影）**：原版 ghost 与**服务端自动放置**完全由 `recipe.getIngredients()` 驱动 ✓（`RecipeBookComponent.setupGhostRecipe` 把 `getIngredients().iterator()` 交给 `PlaceRecipe.placeRecipe(width,height,resultSlot,...)` ✓，而 ghost 画在 `menu.slots.get(p_slot)` ✓、`p_slot` 是"网格下标 → 菜单槽位（从 0 数、跳过输出槽）"✓），而我们的蓝图是 `GunBenchRecipe` 的**独立字段、不在原料列表里** ✗，网格又只声明 `1×10` ⇒ 迭代器到不了第 11 槽 ⇒ **蓝图槽永远没有 ghost、也不会被自动放置** ✗（顺带解释：`ServerPlaceRecipe` 遍历 `gridWidth*gridHeight+1 = 11` ⇒ 只管到槽 0..10 ✓）。修法：把蓝图当作**配方的第 11 个输入** ✓（`getIngredients()` = 10 模块 + 蓝图 ✓，新增 `getModuleIngredients()` 保留 10 模块 ✓；网格 `1×11`、`getSize()=11` ✓）⇒ index 10 → 菜单槽 **11 = 蓝图槽** ✓，且 `11+1=12` 正好等于工作站自己的槽数 ✓。上机网络格式**保持不变**（仍写 10 + 蓝图 ✓）。连带修掉三处"原料下标 ≠ 容器/菜单下标"：`GunBenchMenu.consumeIngredients` 会扣到**输出槽** ✗、**JEI `GunBenchTransferInfo` 从移植起就有 off-by-one**（用原料下标当菜单槽下标 ⇒ 转移目标错一格且漏最后一个模块槽 ✗）、`GunBenchCategory` 会把蓝图加两次 ✗。验证：新增 `tools/check_gun_bench_layout.py` **照抄原版 `PlaceRecipe` 算法 + 读真实常量**做算术断言 ✓（实测 `ingredient index -> menu slot [1..11]` ✓、蓝图 → 槽 11 ✓，**但这是算术验证、不是客户端实录** ✗，ghost 画面仍需玩家确认 ✓）；`verify_installed_jar` 新增 1 项（上一个 jar **97/98** ✓、新 jar **98/98** ✓）；build ✓、相关审计 0 失败 ✓。见 HANDOFF §42.8 |
| **配方书里显示"原版工作台图标" + 只显示 9 把枪** | ✅ **图标＝移植漏了一个方法（已修）；条数＝解锁数量，已给出可测量的诊断**：① 图标根因是 `Recipe#getToastSymbol()` 的**原版默认实现返回工作台**（`return new ItemStack(Blocks.CRAFTING_TABLE)` ✓，实测原版源码 ✓），而 `GunBenchRecipe` 没有重写 ⇒ **配方解锁提示（`RecipeToast`）里画的站点图标一直是原版工作台** ✗（全仓库只有 `RecipeToast:48` 用这个图标 ✓）——玩家说的"枪械配方图标旁边有个原版工作台"和上一轮说的"右上角解锁的配方都是原版工作台的图标"是同一件事 ✓。已重写为**枪械工作台方块** ✓。② 顺带修同类混淆：配方书**搜索页图标**原用**枪械工作台方块**（其贴图本身就是台面形，容易被读成原版工作台 ✗）⇒ 改成原版同款的**指南针** ✓（原版分类图标实测只有 `COMPASS/BRICKS/REDSTONE/IRON_AXE/LAVA_BUCKET`，**没有任何工作台** ✓ ⇒ 玩家看到的那个图标只能来自模组侧 ✓）。③ "只有 9 条" **也是移植漏的方法（已修，见下）**：先把"书里画什么"读清（`getRecipeBookCategories` 是默认方法 ⇒ 取我们注册的 `[SEARCH,MISC]` ✓；分类靠 `registerRecipeCategoryFinder` 只映射我们的类型 ⇒ 原版合成配方进不来 ✓；显示前 `RecipeCollection#updateKnownRecipes` + `removeIf(!hasKnownRecipes)` ⇒ **只画已解锁的** ✓），据此我一度推断"9 = 该玩家当前解锁数" ✗ —— **被玩家实测否掉** ✗（`/scguns recipebook` 显示 **146/146 全解锁**，书里仍只有 9 条 ✓）。真因是 **`Recipe#isIncomplete()` 的默认实现"任意一个原料槽为空即判为配方不完整"**，而 `ClientRecipeBook.categorizeAndGroupRecipes` 的**第一道门**就是 `if (!isSpecial() && !isIncomplete())` ⇒ 枪只用得到 10 个模块槽里的几个、其余是 `Ingredient.EMPTY` ⇒ **137 条整条被丢弃** ✗，活下来的正好是"十个槽全填满"的 **9 条** ✓✓（数据侧复算：146 条里全填满的**恰好 9 条** = `big_bore / callwell / echoes_2 / gattaler / scratches / shellurker / terra_incognita / thunderhead / weevil` ✓ —— 玩家**第一次**报的"只有 big_bore 和 callwell"就是这 9 条的前两条 ✓，即这个 bug 从第一版就在 ✓）。修法照抄原版 `ShapedRecipe.java:88` 的语义：忽略"故意留空"的槽位、只看非空槽位是否真无物品 ⇒ `isIncomplete()` 重写 ✓（空槽是版式的一部分 ✓）。客户端日志（新诊断行，玩家 `latest.log` 实测）与之一致：`tabs=[..._SEARCH=9 ..._MISC=9]` ✓（SEARCH 是 MISC 的聚合 ⇒ 同一批数了两遍 ⇒ 真实 9 ✓，日志已改成按集合身份去重 ✓）。另新增只读工具 `tools/inspect_player_recipebook.py`（直接读存档 `playerdata` 的 `recipeBook` ✓，实测三份存档解锁为 0/146 —— 那是**上一轮**的状态；登录扫描只扫主背包 36 格、漏副手，已补 ✓）。④ 新增诊断：`/scguns recipebook`（查自己**无需权限** ✓，按蓝图分组打印 `已解锁/总数` ✓，实现放在独立类里用**第二次 `dispatcher.register`**，Brigadier 会合并同名根节点 ✓）+ 打开工作台时打一行 `SCGUNS-RECIPEBOOK display ... unlocked=N`（**客户端**真值，与服务端命令对照即可定位是同步问题还是解锁数量问题 ✓）。`verify_installed_jar` 新增 **9 项**（含 3 条反向检查 ✓）：修复前 jar **88/97** ✓、新 jar **97/97** ✓；build ✓、10 项相关审计 0 失败 ✓、语言键 en_us/zh_cn 各 1806 条**全对齐** ✓。见 HANDOFF §42（§42.7 = 真正的根因） |
| **枪械等级提升没有任何提示** | ✅ **已修（两个原因）**：① **开关默认 `false`** ✗（`Config.CLIENT.display.showProgressionMessages` ✓，**0.5.5 也是 false** ✓，不是移植改坏的 ✓）⇒ 默认状态**一条都不发** ✓，所以玩家"没在聊天栏看到" ✓；改成默认 **true** ✓（**有意偏离 0.5.5**，注释写明 ✓）。② **措辞只说解锁不说后果** ✗：`"Gun Tier Unlocked: %s"` + `"Enemies can now spawn with:"`（悬空冒号 ✓，列表中分隔符还是代码硬编码的 `", "` ✗）。现在按玩家给的句式合并成一句：EN `You obtained a [%s]-tier gun!` / `[%s] enemies and raids may now appear!`，ZH `你获得了【%s】的枪械！` / `现在可能会出现【%s】的敌人和袭击！` ✓，分隔符抽成 `list_separator` 键（ZH 顿号 ✓）✓，并追加 `PLAYER_LEVELUP` 提示音 ✓。③ 顺带补触发面：新增 **`PlayerContainerEvent.Close`** ⇒ 从**箱子/战利品袋/村民交易**拿到更高等级枪也会提示 ✓（0.5.5 只盯拾取与**原版**合成 ✗）。**验证**：build ✓、`audit_lang_keys` 两语言 **1795/1795 全对齐** ✓；实际聊天栏显示与音效需玩家在游戏内确认 ✗。另记一条自己的失误：第一版用 `json.dumps` 重写语言文件导致 **3459 行 diff** ✗，已还原并改成逐行替换（最终 en_us/zh_cn 各 7 行 + Config 3 行 ✓）。见 HANDOFF §40 |
| **等级进度可以用指令查询** | ✅ **已实现（玩家请求）**：原来**已有** `/scguns progression check <player>` ✓，但三个问题让它形同不存在：**要权限 2（OP）** ✗、**必须带玩家参数**（连查自己都得写全 ✓）、**输出是硬编码英文** ✗（中文客户端也显示英文 ✓）。现在：`check`（**无参数＝查自己，无权限门槛** ✓，控制台执行提示 `requires_player` ✓）、`check <player>`（仍要权限 2 ✓）、**新增 `info <tier>`**（某等级会带来哪些敌人等级与袭击 ✓，Tab 可补全 ✓，控制台可用 ✓）；输出全部改用可翻译组件 ✓，袭击显示**名字**而非配置 id ✓，分隔符复用 §40 的 `list_separator`（中文顿号 ✓）。新增 7 键、删 2 个已无用的旧键；语言文件仍**逐行**修改（en_us/zh_cn 各 9 行 diff ✓）。**RCON 实测**：`info frontier` → `Antique` 敌人 + `Antique/Frontier Raid` ✓、`info copper` → 2 个敌人等级 + 3 个袭击 ✓、`info diamond_steel` → 4 等级 + 10 袭击 ✓、`info bogus_tier` → `Invalid tier` ✓、控制台 `check` → `Requires Player` ✓；"查自己"需真玩家 ⇒ 需游戏内确认 ✗。顺带修正 `verify_installed_jar` 一条**过宽**的检查（原要求所有进度都有 `display.icon` ✗，但原版 1277 条配方解锁进度**都没有 display** ✓）：改为跳过无 display 者、有 display 者仍校验 `id` 形式 ✓ ⇒ **88/88** ✓。见 HANDOFF §41 |
| **汉化内置** | ✅ **已内置（玩家请求）**：把玩家的资源包 `Scorched Guns_v0.5.5-汉化v1.3.zip` 里的 `assets/scguns/lang/zh_cn.json` **逐字节**装进 mod（118,567 字节、1794 条）⇒ **不需要启用资源包**就能显示中文 ✓。实测覆盖 **en_us 全部 1784 条键的 100%**（缺 0 条 ✓），另有 10 条译者提前补的 `subtitle.scguns.*`（0.5.5 的 en_us 也没有、代码无字面引用，但 `sounds.json` 里确有 `flyby`/`fire_2`/`silenced_fire` 等声音 ⇒ 留着无害 ✓）✓。顺带核对：**移植的 en_us 与 0.5.5 的 en_us 键集完全一致**（1784/1784 ✓）。新增报告型审计 `tools/audit_lang_keys.py`（zh_cn 100% ✓ 高于 uk 74.8% / ko 54.3% / vi 49.7% / ru 26.2% ✓）+ `verify_installed_jar` 2 项打包检查（旧 jar 82/84 FAIL ✓、新 jar **84/84** ✓，其中一项查**中文字节**而非键名，避免"文件在就行"的假通过 ✓）。见 HANDOFF §33 |
| **蓝图"设置配方"全部变成高斯步枪** | ✅ **已修（所有配方共用一个占位 id）**：1.21 把配方 id 从 `Recipe` 挪到 `RecipeHolder`，移植的新基类 `ScRecipeSerializer` 用 `UNKNOWN_ID = scguns:unknown` 兜底 ⇒ **146 个枪械工作台配方的 `getId()` 全是同一个占位符**（实测日志：`holderId=scguns:guns/gauss_rifle_from_gun_bench recipeValueId=scguns:unknown`、`146/146` 都是占位符 ✓）。而蓝图界面**保存/发送**与**显示激活配方**都按 `recipe.getId()` 做事 ⇒ 设置时存的是 `scguns:unknown`、显示时 `filter(getId().equals(...)).findFirst()` 永远命中**列表第一条**，而第一条正是 **Gauss Rifle** ⇒ 每张蓝图都显示高斯步枪 ✓✓。修法：id 一律取自 `RecipeHolder`（`DisplayEntry` 存 holder id ✓、不再 `map(RecipeHolder::value)` ✓、`byKey(recipeId)` 精确取 ✓、`saveActiveRecipe` 收 `ResourceLocation` ✓）。根因已实测、编译与 82/82 已验；**界面本身需玩家确认**（纯客户端，无法 headless 验证）。见 HANDOFF §32 |
| **附魔台"附魔功能受限" + 图书管理员不卖 scguns 附魔书** | ✅ **同一个根因，已修（缺的是标签）**：1.21 用标签决定"哪些附魔能出现在附魔台/村民交易"——附魔台只给 `#minecraft:in_enchanting_table` 里的附魔，图书管理员只从 `#minecraft:tradeable` 抽取（本机 `/datapack list` 实测 `trade_rebalance` 数据包**未启用** ⇒ 走读 `#tradeable` 的代码路径），而这两个标签都由 **`#minecraft:non_treasure`** 构成 ✓。移植只带了 15 个 `data/scguns/enchantment/*.json`，**一个 `data/minecraft/tags/enchantment/` 都没有** ⇒ 模组附魔不在任何标签里 ⇒ 附魔台不提供 + 村民永不进货 ✓✓。0.5.5 没这问题是因为 1.20.1 没有这些标签、Forge 的 `isDiscoverable/isTradeable` 默认 true（模组附魔只设了 `Rarity`）✓。修法：新增 `data/minecraft/tags/enchantment/non_treasure.json`（15 个 id、`replace:false` 合并）⇒ 一处恢复"可发现+可交易"（`in_enchanting_table`/`tradeable`/`on_random_loot`/`on_traded_equipment`/`on_mob_spawn_equipment` 都引用它）✓。验证：`verify_installed_jar` 新增 2 项打包检查（旧 jar 80/82 FAIL ✓、新 jar **82/82** ✓）、`/enchant scguns:accelerator` 对枪械成功 ✓、服务端无标签加载报错 ✓；**附魔台界面本身需玩家在游戏里确认**（无客户端建不起菜单）。另记：命令生成的村民不会正常生成交易 ⇒ 村民交易无法在专用服务器验证。见 HANDOFF §31 |
| **村民交易附魔书** | ⏳ **已排除一条，待玩家确认对象**：模组枪匠交易是**追加**的（未清空原版 ✓）⇒ 不该破坏原版图书管理员 ✓；且模组交易里**没有任何附魔书**（0.5.5 与移植版一致，两棵树都搜不到相关 API ✓）。已知偏差：`MerchantTradeConfig.parseItemStack` 只支持 `{item,count}` ⇒ 交易 JSON 写附魔物品会变**白板**（0.5.5 的 NBT 路径当年能带 NBT）——若玩家指的是模组商人 NPC，这就是根因。见 HANDOFF §30.3 |
| **闪光弹对创造模式玩家完全无效** | ✅ **已修（移植写错了 `ignoreExplosion`）**：0.5.5 调用 `Entity#ignoreExplosion()`，而 **1.20.1 的真实运行时 jar** 里该方法（SRG `m_6128_`）就是 `return false` ✓，全 jar 只有 `Entity` 声明、`ArmorStand` 重写（`isMarker()`）、`Explosion` 调用，**`Player` 没有重写** ⇒ 1.20.1 里**没有任何玩家会被跳过**（创造/旁观一样吃闪光弹 ✓，与玩家记忆一致 ✓）。移植把它改写成 `ExplosionHelper.ignoresExplosion` 时**凭想象**加了"无敌/旁观返回 true" ✗ ⇒ 创造玩家被整段跳过 ⇒ 玩家报"完全没有被施加" ✓。已把 helper 改成忠实实现（只跳过 marker 盔甲架 ✓），调用点保持 0.5.5 原样 ⇒ 6 处调用点语义同时恢复 ✓。**真玩家实测**：修前 生存 `deafened+blinded` / 创造 **空** ✗；修后 生存 `deafened(204)+blinded(135)` / 创造 `deafened(205)+blinded(136)` ✓✓。见 HANDOFF §29（含"注释不是证据"的教训 ✓） |
| **闪光弹遮罩（屏幕白光）不渲染** | ✅ **已修（改用 GUI 图层）**：① 服务端：召唤僵尸引爆闪光弹 ⇒ 背对时拿到 `scguns:deafened`(185t) ✓，**朝向时 `scguns:blinded`(88t)+`deafened`(165t) 都有** ✓ ⇒ `onDeath` 判据、`ScEffects.holder`、`addEffect` 全对 ✓。② 客户端：0.5.5 的 `GameRendererMixin` 自建了一个 `GuiGraphics` 却**从不 `flush()`** ✗（1.21.1 的 `fill` 只写进 BufferSource ⇒ 没人提交 ⇒ 画了等于没画 ✗）⇒ 玩家"扔出去屏幕完全没变化" ✓。已删除该 mixin（含配置项）+ 新增 `client/handler/BlindnessOverlay`（`RegisterGuiLayersEvent.registerAboveAll` ⇒ 一定画在世界/HUD 之上，用当帧原版 GuiGraphics ✓），淡出公式与两个配置项与 0.5.5 一致 ✓。③ **实机截图验证**：dev 客户端连进 dev 服务器 + `/effect give Dev scguns:blinded` ⇒ 整个游戏窗口被白光罩住 ✓（修复前无变化 ✓）。见 HANDOFF §28.5 |
| **打物理结构底部没有反应** | ✅ **已修（命中瞬间速度被清零）**：`applyShotImpulse` 原有一条"飞行方向长度 ≤1e-4 就丢弃"的输入检查，而 Sable 把飞进子级的弹丸搬进结构坐标系时会**连速度一起清掉** ⇒ 从下方穿到底面边界时 `getDeltaMovement()` 只剩 `5.0e-5` ⇒ **整发被丢弃**。实测日志一句话见底：`rejected: direction 5.000013228676376E-5 too short`。现在方向改走**回退链**：`getDeltaMovement()` → 命中面**反向法向**（底面命中时即"向上"，正是飞行方向）→ 射手位置→命中点 → 都不成立才放弃；两个调用点都按此取方向。修后同一发不再被丢弃、正常进入施力流程 ✓ 见 HANDOFF §27 |
| **射弹推动物理结构不计算玩家位置** | ✅ **已按玩家要求改为与出拳同构**：核对字节码后确认 Sable 出拳把力施加在**玩家自己所站的位置**（`localPosition = transformPositionInverse(player.position())`，质量查询也用该点），而我们的射弹用的是命中点 ⇒ 与玩家站位无关。现在 `applyShotImpulse(level, 命中点, 施力点, 方向, 拳数)`：命中点只用来**找结构**，施力点（= `shooter.position()`，炮塔弹丸取 owner、无 owner 则回退命中点）用来**查质量+施力**。因为远距离力臂会比出拳长得多，新增自旋上限（`physicsStructureMaxSpin` 默认 3.0 rad/s、单次 1.5 rad/s），用 Sable 的**惯量张量**精确计算诱发角速度再按比例削冲量。实测：力臂 5.07 / 199.82 格证明施力点已生效 ✓；出拳距离内 `beforeSpin == afterSpin == 61.94`（上限不介入 ✓）；200 格也只有 0.0092 rad/s ⇒ 上限只作兜底 ✓。见 HANDOFF §26 |
| **子弹推动/打滚物理结构**（新功能） | ✅ 已实现并按玩家的校准意见（"空手左键的力最好"）重做，两轮实测各自定位到一个**坐标系**错误：① 法向质量用世界坐标查 ⇒ Sable 回 `3.4e11`（应为 `0.014`），等效冲量 `~1e-11` ⇒ 玩家报"**没有效果**"；② **施力点**仍用世界坐标，而 Sable 的管线要的是结构局部坐标 ⇒ 力臂从 4.94 格变成 **2.897e7 格**（结构的 600 万倍），线性冲量虽小、角冲量爆表 ⇒ 玩家报"**结构被扔到 y=14022**"。两点现在都用 `transformPositionInverse/transformNormalInverse` 转换（与 Sable 出拳处理器 `localPosition/localDirection` 逐参数一致，`javap -l` 核对）。冲量 = Sable 自己的出拳力（`punchCurve(法向质量) × 拳力系数 × 相当于几拳`，单位就是"拳"，1.0 = 每 10 伤害一拳），实测 `magnitude == strength == 32.45`，三层上限都不削减；单次 ≤2 m/s、总速度 ≤ `physicsStructureMaxSpeed`（默认 8→**4** 格/秒）。**注意：速度上限只管线性、管不了自旋，所以力臂必须正确**（HANDOFF §22.6、§22.7） |

## 第三轮实测反馈（HANDOFF §16）

| 症状 | 根因 | 状态 |
|---|---|---|
| **进度界面里没有 scguns 标签页** | 115 份进度的 `icon` 还是 1.20.1 的 `{"item": ...}`；1.21 的 `DisplayInfo` 用 `ItemStack.STRICT_CODEC`（字段是 `id`）⇒ `display` 解析失败。**注意：进度不会被丢弃，而是静默变成"没有 display"** ⇒ 界面里看不见（服务端日志计数完全不变，实测过） | ✅ 已修（`tools/fix_advancement_icons.py`，校验 `problems: 0`） |
| **蓝图界面的枪械描述缺失** | `Item.toString()` 换语义：1.20.1 返回注册名 path（`musket`），1.21 返回 `scguns:musket` ⇒ 描述 key 变成 `scguns.desc.scguns:musket` ⇒ 回落"未发现" | ✅ 已修（改用 `BuiltInRegistries.ITEM.getKey(...).getPath()`；`AttachmentRenderer` 里同类的正则用法一并修） |
| **配件装上后不同步，要丢出去再捡回来** | 1.21 的 `ItemStack.copy()` **浅拷贝组件**（`PatchedDataComponentMap.copy()` 共享 patch map）⇒ 容器影子和服务端共享同一个 `CompoundTag` ⇒ `ItemStack.matches` 恒等 ⇒ **永远不发更新包**。1.20.1 的 `copy()` 是深拷贝所以没这问题 | ✅ 已修（`NbtHelper`：写入必须走 `getOrCreateTag`/新的 `getTagForWrite`，它们先"脱钩"再交付；14 处 `getTag` 原地写 + 2 处陈旧引用一起改；新审计 `audit_nbt_write_alias.py` 自测过 14→0） |

| **打开外骨骼界面就崩溃/掉线** | 1.21 的 `ItemStack.STREAM_CODEC` **拒绝空栈**（1.20.1 的 `writeItem` 允许），而 `C2SMessageSaveExoSuitUpgrades` 把 4 个升级槽整组发送（空格也发）⇒ 在 netty 编码器里抛异常 = **连接断掉**，不是普通报错 | ✅ 已修（全模组 22 处 `ItemStack.STREAM_CODEC` → `OPTIONAL_STREAM_CODEC`；新审计 `audit_item_stream_codec.py` 自测 22→0） |

## 数据层（`data/**/*.json`）：**曾经一整层是坏的**（HANDOFF §11）

编译通过、服务端 `Done`、六项静态审计全 0 —— 但数据文件在加载时被静默丢弃。
入口只有两个：服务端日志里的 `Parsing error loading recipe <id>` 和
`Couldn't load tag <tag> …missing following references`。**每轮都要数这两行。**

**配方被拒 206 → 0；坏标签 6 → 0。**

| 缺陷 | 规模 | 后果 | 状态 |
|---|---|---|---|
| 条件键写成 `"conditions"`，1.21 只认 `neoforge:conditions` | 432 处 | **所有跨模组门禁空转**：模组不在时配方照样解析并报错 | ✅ 已修 |
| 标签目录是 1.20.1 的 `tags/items`+`tags/blocks` | 整个 `c:` 树 | 自建约定标签**一个都没加载** | ✅ 已修 |
| 配方引用 `neoforge:` 标签（1.21 已改为 `c:`） | 105 处 | 配方能加载但**永远做不出来**（材料为空），日志无声 | ✅ 已修 |
| Create 6 字段改名 `acceptMirrored`/`transitionalItem` | 164 文件 | **全部枪械配方 + 弹药生产线**被丢弃 | ✅ 已修 |
| Mekanism / CreateOreExcavation / IE 各自改了自家 schema | 30 文件 | 跨模组加工链全部失效 | ✅ 已修 |
| 条件注册的物品在标签里没写 `required: false` | 6 个坏标签 | 连带打坏 IE 的 `toolbox/tools` | ✅ 已修 |
| `minecraft:grass`（1.21 已改名 `short_grass`）、`#c:glass` 方块标签不存在 | 2 处 | 标签整个不加载 | ✅ 已修 |
| `category` 用了 `ammo`/`throwable`/`dirt` | 24 处 | **无影响**——`EnumCodec` 是宽容查找，未知值回落默认 | ❌ **不要改**（假阳性） |

## 「本该覆写却签名漂移」：编译器和六个审计都查不出来（HANDOFF §12 / §13）

`javac` **不会**报错——签名不匹配的方法只是一个**普通重载**：合法、能编译、**永远不被调用**。
唯一的编译期证明是 `@Override`。本轮一共查出 **56 处**：

| 家族 | 处数 | 症状 |
|---|---|---|
| `renderRecursively`（GeckoLib 4.4→4.6） | 1 | 枪模型自带手臂、幻影配件、非皮肤手臂 |
| `appendHoverText`（参数顺序） | 19 文件 | **所有物品 tooltip 不显示** |
| `getCloneItemStack`（`BlockGetter`→`LevelReader`） | 3 | 挖掉时掉原版物品 |
| **`finalizeSpawn`（多一个 Forge 参数）** | 10 | **所有 gunner 生物从来不带武器** |
| `checkAndPerformAttack`（`double` 参数） | 10 | 自定义近战攻击从不触发 |
| `renderToBuffer`（RGBA→打包色） | 17 | 实体模型显式绘制顺序失效 |
| `getEyeHeight(Pose, EntityDimensions)`（1.21 已移除） | 2 | 引爆桶眼高失效 |

**判据（新增的 ARITY-MISMATCH 规则）**：父类参数表是**我们参数表的真前缀** →
就是"Forge 回调参数在 NeoForge 被删掉"的指纹。`tools/audit_override_drift.py`。

## 快速上手（校验当前状态）

```powershell
cd E:\mod\scgun-0.5.5-1.21.1-neoforge
python tools\javac_check.py                # 秒级全量编译（gradlew printCompileClasspath 先跑一次）
python tools\audit_subscribers.py          # @EventBusSubscriber 类必须至少有一个 @SubscribeEvent
python tools\audit_bus_registrations.py    # 事件总线注册（0 可解析残留 = 干净）
python tools\audit_dist_safety.py          # Dist 泄漏（0 = 干净）
python tools\audit_abstract_events.py      # 抽象事件监听（0 = 干净）
python tools\audit_unsafe_casts.py         # 事件里把 Entity 强转 LivingEntity 之类（0 = 干净）
python tools\audit_mixins.py               # mixin 目标/签名体检
python tools\audit_override_drift.py       # ★ 签名漂移 + 参数个数漂移（都必须是 0）
python tools\audit_nbt_write_alias.py      # ★ 组件别名写（getTag 原地写 / 局部变量陈旧）必须是 0
python tools\audit_item_stream_codec.py    # ★ ItemStack 流 codec 必须是 OPTIONAL_（普通版拒绝空栈）
python tools\analyze_advancements.py       # ★ 进度 JSON 体检（problems 必须是 0）
python tools\rcon_tick_probe.py            # ★ 实体行为测试前先跑：没玩家时要 forceload 才会 tick 实体
python tools\rcon_item_exists.py scguns:anthralite_knife   # 条件注册的物品是否存在（实机）
$env:JAVA_HOME='D:\jdk-21.0.3'
cmd /c "gradlew.bat build --console=plain > build-logs\build-NN.txt 2>&1"
# 服务端（会一直挂着；先确认上一轮 java 进程已退出）。run/ 已被 gitignore，
# 其中 server.properties 已开 RCON（密码 scgunsverify），可直接做实机验证：
cmd /c "gradlew.bat runServer --console=plain > build-logs\server-NN.txt 2>&1"
python tools\list_recipe_errors.py build-logs\server-NN.txt     # ★ 必须是 0
python tools\list_tag_failures.py  build-logs\server-NN.txt     # ★ 必须是 0
python tools\rcon_cmd.py "reload"                               # 服务端实机验证入口（跑起来之后）
python tools\rcon_mob_equipment.py                             # ★ 生物装备实机验证（应 6/6）
```

## 现在最该做的三件事

1. **继续客户端实测**：`build/libs/scguns-0.5.5.1.jar` 已复制到用户实例 mods 目录，上一轮反馈的项目
   已全部实测正常（含持枪姿势，临时的 `PoseDiagnostics` 已按约定删除）。继续玩，遇到异常先看
   `logs/latest.log`——**编解码器里的异常表现为"掉线/崩溃"而不是报错**（HANDOFF §17.4）。
2. 把 276 处 "erasure-only"（真覆写但缺 `@Override`）逐批补上 `@Override`，
   让编译器替我们守住这一类漂移（HANDOFF §12.3）——已经证明不补的代价是 56 处静默失效。
3. 处理 HANDOFF.md「已知偏差」里剩下的**非阻塞但影响玩法**项
   （3 种弹药的抢夺加成丢失、容器方块实体缺 `ItemHandler.BLOCK`）。
4. 实机验证工具都已就绪（`tools/`）：`rcon_cmd.py`（通用命令）、
   **`rcon_tick_probe.py`（测任何实体行为前必须先确认区块在 tick 实体——没玩家时要 `forceload`）**、
   `rcon_item_exists.py`（条件注册的物品到底在不在）、`rcon_soul_fire_check.py`、
   `rcon_mob_equipment.py`、`summarize_server_log.py`。

## 最新一轮（HANDOFF §59）：装带附魔的配件导致**掉线** —— §58 修法的回归，已修

- **现象**：玩家 `Internal Exception: Failed to encode packet 'clientbound/minecraft:container_set_content'`，
  服务端 `Can't find id for 'Reference{… minecraft:mending …}'` ⇒ 断线（§59.1）。
- **根因**：§58 让**任何** `LevelEvent.Load`（含**客户端** level）写 `NbtHelper` 的静态注册表 ✗ ⇒ 单人下
  客户端那份附魔注册表实例覆盖了服务端的 ✗ ⇒ 服务端解码出来的附魔 holder 不属于自己那份实例 ⇒ 发包时
  `IdMap.getIdOrThrow` 失败（§59.2，判定依据：日志里同时有 `Server thread` 与 `Render thread` ⇒ 单人集成服）。
- **修法**：`RegistryAccessListener` 改为「服务端 level/服务端启动 ⇒ 恒写服务端那份；客户端 level ⇒ 仅当本
  JVM 内**没有**服务端时才写自己那份；`ServerStoppedEvent` ⇒ 释放归属」（§59.3）。
- **实测**：枪架（走 `NbtHelper` 编解码）里放 `long_scope` + `minecraft:mending:1` ⇒ `save-all` → 干净退出 →
  重启 ⇒ 附魔**仍在** ✓（§59.4）；对照组＝§58 探针在**无** provider 时 `roundTripEmpty=true` ✓。
  门禁：`build` ✓ / 21 审计全过 ✓ / `verify_installed_jar` **140/140** ✓ / 服务端日志 0 ERROR·FATAL ✓。
- **待玩家复测**：「带附魔的配件 → 装到枪上」**不再掉线**（§59.6；这条真机链路无法无头复现）。
- 工具补充：`tools/rcon_cmd.py` 新增 `--file <路径>`（PowerShell 5.1 会吃掉传给原生 exe 的双引号，SNBT 没法内联传参）。

## 最新一轮（HANDOFF §60）：配件开火**不掉耐久** —— 0.5.5 的缺陷，已修

- **现象**：玩家报告"配件在枪械开火时没有正常消耗耐久"。
- **根因**：`Gun.getAttachment` 是**从枪 tag 解码出的副本** ⇒ 0.5.5（以及我们逐字移植的版本）里
  `damageAttachments` 的 `hurtAndBreak` 打在副本上，用完即丢 ⇒ 配件永不磨损；那个"到 maxDamage-1
  就摘除 + 播 `ITEM_BREAK`"的分支因此是**死代码**（`grep` 实测 0.5.5 全树没有写回 `Attachments` 的路径）。
  参考 1.21.1 移植版**已修同一处**（新增 `Gun.setAttachment(...)`），本轮照它补齐。
- **改动**：新增 `Gun.setAttachment(gun, type, attachment, provider)`（`getOrCreateTag` 保证可同步、
  `tagFromItem(..., provider)` 显式注册表、编码失败不覆盖）；`GunEventBus` 磨损改为每类型 `damageAttachment(...)`
  并写回、按 `type.getTagKey()` 摘除；`Gun.removeAttachment` 改用 `getTagForWrite`（否则"断了看不见断"）；
  `AttachmentMendingHandler` 复用 `setAttachment`；`NbtHelper` 增加显式注册表重载。
- **实测**：临时探针 + `FakePlayer` 驱动磨损 ⇒ `damage=1/2/3` 逐发递增 ✓（读回重新解码 ⇒ 真的写进了 NBT ✓），
  1599/1600 再挨一发 ⇒ `equipped=false` ✓。门禁：`build` ✓ / 21 审计 ✓ / `verify_installed_jar` **145/145** ✓
  （新增 §60 五项）/ 探针不在 jar 里 ✓ / 已安装（`.bak-143716`）。
- **待玩家复测**：实弹打几百发看配件耐久是否下降（长瞄具 1600 点 ≈ 1600 发）；近战砸人路径同样受益但未实测。

## 最新一轮（HANDOFF §61）：经验修补 —— 实测矩阵 + sculk 枪的 XP 修复已修

- **实测矩阵**（FakePlayer + 真 `ExperienceOrb`）：A 普通枪自身有 Mending ⇒ 100→80 ✓；B 配件自身有 Mending ⇒
  200→180 ✓；C **sculk 枪**（0.5.5 自带 XP 修复）⇒ 修前 100→100 ✗、修后 100→**90**（球值 10→0）✓；
  D 创造 ⇒ 100→80 ✓（创造不阻止修补）；E `/xp add`（没球）⇒ 不修 = **原版行为** ✓。
- **根因（C）**：0.5.5 的 `GunXpHandler` 声明 `value = {Dist.CLIENT}`（参考 1.21.1 移植版同样是 CLIENT ✗ ⇒ 上游缺陷），
  只在客户端改副本、改客户端的 `orb.value` ⇒ sculk 枪的"拾球修枪"从未生效；同类的配件循环也因此从未生效（即玩家说的老痛点）。
- **改动**：`GunXpHandler` 改为两侧注册 + 服务端守卫（**不能**用 `Dist.DEDICATED_SERVER`，那会排除单人），
  删掉失效的配件循环（由服务端 `AttachmentMendingHandler` 负责），保留 0.5.5 的数值与"只对 sculk 枪"语义。
- **门禁**：`build` ✓ / 21 审计 ✓ / `verify_installed_jar` **147/147** ✓（新增 §61 两项）/ 探针不在 jar 里 ✓ / 已安装（`.bak-145847`）。
- **待玩家定**：`/xp add` 这类"没有经验球的加经验"要不要也修（偏离原版）；配件是否扩到背包里没拿在手上的枪。

## 最新一轮（HANDOFF §62）：经验修补"不生效" —— 141 把枪逐把实测**全过**，等玩家现场数据

- 玩家用的是 **iron 蓝图**的枪（该蓝图对应 18 把枪）✓、枪与配件都附了 Mending ✓、拾取经验不生效 ✗；
  其 `config/scguns-common.toml` 里 `enableGunDamage`/`enableAttachmentDamage` **都是 true** ✓ ⇒ 配置不是原因 ✓。
- 逐把 `GunItem` 实测（临时探针 + FakePlayer + 真经验球，已删除）：`guns=141 wear=141 mend=141 attachmentMend=141/141` ✓
  ⇒ 代码侧四条路径（开火磨损 / 原版 Mending 修枪 / 配件 Mending / sculk 自修）**逐把成立** ✓。
- 时间线核对：玩家那次会话 14:40–14:43 ✓，实例 jar 是 14:37:16 那版 ✓（已含 §59/§60/§57）✓；§61 的 14:58 版本与此例无关 ✓。
- **下一步**：请玩家开几枪打出耐久后执行 `/data get entity @s SelectedItem`，确认 `damage > 0`、
  `minecraft:mending` 是否真的在物品上、附件 `damage > 0` ✓（`damage` 为 0 ⇒ "没有东西需要修" ✓，不是修补失效 ✓）。
- 玩家已确认：`/xp add`（无经验球）保持**原版不修** ✓ —— 不改。

## 最新一轮（HANDOFF §63）：换弹后**再也开不了镜** —— 卡住的 `InCriticalReloadPhase`，已修

- **现象**：玩家"换弹后会一直取消开镜状态，导致不能正常瞄准"。
- **根因**：客户端 `AimingHandler` 有四个一票否决（`RELOADING` ✓、`ReloadState` 非 NONE ✓、`InCriticalReloadPhase`
  每 tick 强制关镜 ✓），且它的清理**只在** `!isReloading && !inCriticalPhase` 时跑 ⇒ **自锁** ✓。而服务端
  `ReloadTracker` 的换弹结束分支清了 `IsReloading`、写了 `ReloadState=STOPPING`，却**没清**
  `InCriticalReloadPhase` ✗；else 分支也只清 `IsReloading` ✗。⇒ 换弹不经 `C2SMessageGunLoaded` 结束时（被打断/切枪/
  动画没走到装填点）该标记永久残留 ⇒ 永远无法开镜。**`grep` 对比：这两处与 0.5.5 逐字相同 ⇒ 上游缺陷**，非移植引入。
- **改动**（服务端 = 权威侧）：① 换弹结束分支补清 phase；② else 分支无条件清 phase（无任何路径会给 MANUAL 枪设它 ⇒
  必然是残留，同时自愈存档里的坏状态）；③ 新增陈旧停止态看门狗（`ReloadState` 非 NONE + stop 标记仍在 + 无换弹运行
  ⇒ 100 tick 后清理并打 WARN，便于后续定位真正源头）。
- **实测**：真 `PlayerTickEvent.Pre` 驱动 `ReloadTracker.onPlayerTick`，三把枪（`mk43_rifle`/`flintlock_pistol`/
  `venturi`（MANUAL））⇒ 卡住的 phase **3 tick 内自愈** ✓；陈旧 `STOPPING` 在 50 tick 时仍在 ✓（不误杀动画）、
  100 tick 被看门狗清掉并记 WARN ✓。门禁：`build` ✓ / 21 审计 ✓ / `verify_installed_jar` **149/149** ✓ /
  探针不在 jar 里 ✓ / 已安装（`.bak-153517`）。
- **待玩家复测**：换弹后能否立刻正常开镜；若仍复现，日志里会有 `[scguns-reload] Clearing a reload state that
  outlived its reload: …`，把它发我即可定位剩余源头。

## 最新一轮（HANDOFF §64）：开镜不改鼠标灵敏度 —— 注入点退化，已改成参数注入

- **现象**：玩家"开镜后不会修改鼠标灵敏度"。
- **根因**：1.21.1 的 `MouseHandler.turnPlayer(double)` **带参数**（1.20.1 是无参），灵敏度最终用在
  `MouseHandler.java:329` 的 `player.turn(d0, d1 * i)` 上；移植时只把 `method` 名字改成 `turnPlayer`，却把 0.5.5 的
  `@ModifyVariable(..., ordinal = 2)` 原样留着 ✗ —— 那个 ordinal 只保证"存在"（注释自己都这么写），不保证改中的是
  会被用掉的量 ⇒ 注入生效、客户端不崩，但**灵敏度毫无变化**。
- **改动**：改为参考 1.21.1 移植版的两个 `@ModifyArg`，打在 `LocalPlayer.turn(DD)V` 调用点的 index 0/1 上
  （数值仍照 0.5.5：`(1-(1-ads)*progress) * clamp(modifier^0.25, 0.5, 1)`）。
- **验证**：`audit_mixins.py` 会把 `@At` 目标解析到 `.refs/nf-src` 的真实 1.21.1 源码 ⇒ 改后仍
  **0 target drift / 0 missing injection / 0 unresolved** ✓；`build` ✓ / 21 审计 ✓ / `verify_installed_jar`
  **149/149** ✓ / 已安装（`.bak-154333`）。
- **待玩家复测**：把 `config/scguns-client.toml` 的 `aimDownSightSensitivity` 调到 **0.2~0.3**（默认 0.75 只降 25%，
  本来很轻微）后开镜对比；纯客户端 mixin，无法无头验证手感。

## 最新一轮（HANDOFF §65）：充能枪不能开火 + 单发装填"装满还装" —— 测清服务端一半，修掉一处弹容不一致

- **两条报告**：① `FireMode.PULSE` 枪无法正常开火；② `ReloadType.single_item` 武器装满后仍一直装弹。
- **实测（FakePlayer 探针）**：PULSE 共 7 把（`gale`/`gauss_rifle`/`hullbreaker`/`nervepinch`/
  `pyroclastic_flow`/`teslock_rifle`/`venturi`）数据正常 ✓（`fireMode=pulse` + `fireTimer` 20–25）；单发装填共 11 把，
  服务端 `reloadItem` 一次装满 ✓ 且**装满时什么都不做**（不涨弹、不消耗装填物）✓ ⇒ "一直装"不是服务端行为。
- **修掉的不一致**：`client/handler/ShootingHandler.java:375` 的自动换弹判断用原始弹容 `getReloads().getMaxAmmo()`，
  而服务端（`isWeaponFull`/`reloadItem`）、HUD、提示、换弹键、换弹状态机等 20+ 处都用
  `GunModifierHelper.getModifiedAmmoCapacity(...)` ⇒ 有改弹容的模块/附件时两边判断会不一致，自动换弹可能永远认为没满。
  已统一为 modified 弹容（0.5.5 同一行也是原值 ⇒ 上游不一致）。
- **门禁**：`build` ✓ / 21 审计 ✓ / `verify_installed_jar` **149/149** ✓ / 探针不在 jar 里 ✓ / 已安装（`.bak-155850`）。
- **未修（缺关键现象，主体在客户端，无法无头测）**：充能枪是"充能条不涨"还是"松手没反应"；单发装填是"HUD 满但动画一直循环"
  还是"装填物被消耗"（后者服务端实测不可能），以及是否装了改弹容的模块。已向玩家提问。

## 最新一轮（HANDOFF §66）：温妮装满不收尾 / 所有枪都在"装弹" —— 两处根因实测已修

- 玩家诊断日志（上一轮临时加的 `scguns-gunprobe`）显示温妮 `reloadType=scguns:manual` ⇒ 走手动装填收尾 + MANUAL 状态机；
  客户端动画看**玩家级** `RELOADING` ⇒ 它卡住就"所有枪都在装弹"。
- **根因一（实测）**：`ReloadTracker` 手动收尾的 100ms 宽限期存在物品 tag（`scguns:StopAfterLoopTime`），但写进去**下一 tick
  读回来永远是 0** ⇒ `stopTime == 0L` 永远成立 ⇒ 收尾代码不可达 ⇒ `RELOADING` 永远 true。改为 tracker **内存**
  `pendingManualStopSince`。
  （测量教训：首次"没生效"其实是探针在几毫秒内连发 20 次 tick，100ms 宽限期无法过去；改成每 tick `sleep(15ms)` 后收尾立即出现。）
- **根因二（实测）**：装填中切枪/收枪时，"武器变了就换 tracker"被 `!isActivelyReloading` 挡住 ⇒ tracker 用旧枪的 stack 却被
  拿**新**手持物判断满/断弹 ⇒ 永远收不了尾；手上不是枪时整个分支被跳过 ⇒ 更卡。新增 `endReload(...)`：`RELOADING=false`、
  给旧枪清标记并置 STOPPING，武器变化（含空手）时调用。
- **实测**：A 装满 ⇒ `syncedReloading=false` ✓；B 换枪 ⇒ false ✓；C 空手 ⇒ false ✓。
- **门禁**：`build` ✓ / 21 审计 ✓ / `verify_installed_jar` **151/151**（新增 2 项）✓ / 探针与诊断日志全部删除（grep 0 命中）✓ /
  已安装（`.bak-163526`）。
- **待玩家复测**：真机里温妮装满后是否正常接上膛动画；其它枪是否还会显示装弹动画；顺带验证 §65 的弹容一致性。

## 最新一轮（HANDOFF §67）：对照其它移植版 —— 收尾根因是"tag 写回没生效"，已修

- 按玩家建议对照了 `E:\mod\SG2-1.21\` 下三个移植版（`ScorchedGunsNeoforge-main` 1.21.1 / `ScorchedGuns-NeoForge-New` 1.21.1 /
  `Scorched-Guns-1.20.1-master`）：**新版移植（549 行）没有 0.5.5 的 100ms 收尾舞蹈，且对每次 tag 改动都用
  `setCustomData` 显式写回**；它在"切枪/收枪"时结束装填的做法与 §66 的 `endReload` 意图一致。
- **由此定位真正根因**：`ReloadTracker.onPlayerTick` 改完 tag **没写回**，实测写入到不了物品 ⇒ 收尾写的
  `ReloadState=STOPPING` + `IsPlayingReloadStop=true` 客户端根本看不到 ⇒ "装弹动画不被上膛动画接替"。
  探针实测：修前 20 tick 后 `reloadState='' isPlayingStop=false syncedReloading=true`；修后
  `reloadState='STOPPING' isPlayingStop=true syncedReloading=false` ✓。
  修法：两处写入都补 `NbtHelper.setTag(heldItem, tag)`（等价上游 `setCustomData`）。
- **有意保留的差异**：§65 客户端自动换弹用**改装后**弹容（三个移植版都用原始弹容 ✗，而 `PLUS_P_MAG` 把弹容 ×0.5 ⇒
  它们会无限自动换弹）；§66 收尾宽限期改为内存计时（上游新版直接删除该舞蹈）。
- **门禁**：`build` ✓ / 21 审计 ✓ / `verify_installed_jar` **152/152**（新增"会写回 tag"）✓ / 探针全部删除 ✓ / 已安装（`.bak-164613`）。
- **待玩家复测**：温妮装满后的上膛动画、其它枪的装弹动画是否恢复正常。

## 最新一轮（HANDOFF §68）：背包里其它枪也进装填动画 —— 收尾把 STOPPING 留在了被收起的枪上，已修

- **根因**：§66 的 `endReload` 照抄"正常收尾"，给**被收起**的枪设了 `ReloadState=STOPPING` +
  `IsPlayingReloadStop=true`；枪不在手上时它的状态机不跑 ⇒ 标记一直挂着 ⇒ 之后拿出/看到这把枪就播装弹动画。
  上游新版在同样情形走 `clearReloadData(tracker.stack)`（清掉），方向被我们搞反了。
- **改动**：① `endReload` 改为**清空**九项装填标记 + 显式写回；② `AnimatedGunItem.inventoryTick` 增加**自愈**
  `clearStaleReloadState(stack)`：枪不在**选中槽**且非客户端时清残留（同时修好旧存档）。
  - 判"在不在手上"必须用槽位：原先用 `GeoItem.getId(...) == GeoItem.getId(...)`，两把未分配 id 的枪 id 都是 0 ⇒ 背包枪被误判成在手 ⇒ 自愈从不触发（实测）。
  - 自愈独立成方法：取 tag→改→写回不能跨着别的 tag 局部变量，否则后续写入丢失 —— `audit_nbt_write_alias.py`
    当场抓到第一版的这个真问题。
- **实测**：A 装填中移到别格 ⇒ 清空 ✓；B 旧存档遗留 ⇒ 清空 ✓；C 手握装填中 ⇒ **保持不变** ✓（不误伤）。
- **门禁**：`build` ✓ / 21 审计 ✓ / `verify_installed_jar` **153/153** ✓ / 探针 0 个 ✓ / 已安装（`.bak-170244`）。
- **待玩家复测**：背包/其它枪是否还会显示装弹动画；从箱子直接拿到选中槽这一条未实测。

---

## §69 【真凶】换弹时背包里的其他枪**真的**在换弹 —— GeckoLib 4.6 换了 id 的存放位置

> 玩家第四轮反馈 **"背包里的其他枪械是真的在换弹"**（提示），§66/§67/§68 三轮修的是**服务端状态**，
> 与这条**无关**；这一条从头到尾是**客户端"哪把枪在手上"判断错了**，而且是移植自己引入的（0.5.5 无此问题）。

- **根因（字节码实证）**：`GeoItem.getId(ItemStack)` 在 GeckoLib 4.6 读的是**数据组件**
  `geckolib:stack_animatable_id`，**组件缺失时回落到 `Long.MAX_VALUE`**；而移植沿用 0.5.5 写了
  `nbtCompound.putLong("GeckoLibID", ...)` —— 该字符串在 GeckoLib 4.9.3 的 jar 里**出现 0 次**（实测）
  ⇒ **全游戏每把枪的 id 都是 `Long.MAX_VALUE`**。
  1. `handlePlayerSpecificLogic` 用 `GeoItem.getId(主手) != id` 判断手持 ⇒ `MAX != MAX` 恒 false
     ⇒ **背包里每一把枪都走"手持"分支**；
  2. 手持分支读的是**玩家级** `ModSyncedDataKeys.RELOADING` ⇒ 玩家换弹时**每把枪都在跑换弹状态机**，
     `tryTriggerAnimation("reload")` 落到它们各自的 controller 上；
  3. 而 **triggered 动画会绕过 `predicate`**（`javap -c AnimationController.handleAnimationState` 实证）
     ⇒ 这些枪**画到哪演到哪** ⇒ 背包 GUI 里真的在演换弹动画。
  4. 同 id 还让**同型号所有枪共用一个 controller**，手持那把的状态会串给背包那把。
- **实测（专用服务器 + FakePlayer 探针，已删）**：
  `fresh A=B=C=9223372036854775807` → `ticked A=9 B=10 C=11`，
  `patch={custom_data={WasHeldLastTick:1b}, geckolib:stack_animatable_id=>9}`。
  组件是 `.persistent(LONG).networkSynchronized(VAR_LONG)` ⇒ **会同步到客户端**（这也是 GeckoLib 用两个
  mixin 让 `ItemStack` 比较忽略该 id 的原因）。
- **改动（`item/animated/AnimatedGunItem`）**：① 改用 **`GeoItem.getOrAssignId(stack, serverLevel)`**；
  ② `inventoryTick` 按**槽位**算手持（`selected || 主手 == stack || 副手 == stack`）；
  ③ `handlePlayerSpecificLogic` 按该标志分流；④ `handleReloadStateSynchronization` 只服务手持那把
  （不再往背包枪写 `scguns:IsReloading`）；⑤ `handleItemNotHeld` 的动画重置改为**只在刚离手那一 tick**执行
  （避免在"还没 id"的窗口里打断手上同型号枪的动画）。顺带修掉 `drawTick` 被背包每把枪各加一次的问题。
- **防复发**：新增 `tools/audit_gun_animation_identity.py`（`--selftest` 对修复前源码实测命中 **7 条**）；
  `verify_installed_jar` 新增 **4 项**（对修复前 jar 实测 **4 项全 FAIL / 153-157**，新 jar **157/157**）；
  新增只读工具 `tools/probe_geckolib_anim_id.py`、`tools/scan_jar_refs.py`。
- **环境事实**：`FakePlayer.tick()` 是**空实现**（javap 实证）⇒ 探针要直接调 `stack.inventoryTick(...)`。
- **状态**：`javac` 0 错误 ✓ / `build` ✓ / 21 审计 0 ✓ / `verify_installed_jar` **157/157** ✓ /
  探针已删除 ✓ / 已安装（备份 `.bak-173518`）。
- **待玩家复测（纯客户端）**：换弹时**只有手上那把**播动画，背包里同型号与不同型号的枪都应静止；
  顺带看抽枪动画速度（以前同型号枪越多越快）与**掉在地上的枪**（以前也会走手持分支）。

---

## §70 NeoForge 前置版本降到 **21.1.150**（并抓出一个"老版本必崩"的真 bug）

> 玩家："neoforge 的前置版本可以放低点，因为很多人没有去更新 neoforge 版本"。

- **下限是 21.1.150，不是我们说了算**：NeoForge 逐个检查**每个 mod** 自己的范围，**必需依赖 GeckoLib 4.9.3**
  自己就声明 `neoforge [21.1.150,)`（`type="required"`）⇒ 再低 NeoForge 会拒绝加载 GeckoLib，本 mod 根本跑不起来。
  framework `[21.1,)`、curios `[21.1.60,)`。工具 `tools/check_neoforge_floors.py`（读各 jar 元数据）。
  （联动模组门槛更高、但那是它们自己的要求：Create `21.1.219`、Sable/Aeronautics `21.1.228`、
  FarmersDelight `21.1.219`、Create New Age `21.1.209`、Mekanism `21.1.194`、IE `21.1.164`。）
- **改动一行**：`gradle.properties` 的 `neo_version_range` → `[21.1.150,)`（进 jar 的 `neoforge.mods.toml`）；
  `neo_version`（dev 目标）保持 21.1.249，因为 dev 要加载那些联动模组；另加 `-PnoIntegrationRuntime` 开关让它们不进 dev 运行。
- **降版本当场抓出真 bug（老 NeoForge 必崩）**：`ModCapabilities` 用默认 `Bus.GAME` 注解，却监听
  `RegisterCapabilitiesEvent`（两个版本里都是 `IModBusEvent`，`javap` 实证）。
  - **FML 4.0.38（21.1.150）**：按注解上的 bus 把**整个类**注册到一个总线 ⇒ game 总线拒绝 IModBusEvent
    ⇒ `IModBusEvent events are not allowed on the common NeoForge bus!` ⇒ **mod 加载失败**；
  - **FML 4.0.44（21.1.249）**：改成**按监听器事件类型逐个路由** ⇒ 同一个类完全正常 ⇒ 这个错误一直没被发现。
  - 修法：`bus = EventBusSubscriber.Bus.MOD`；防复发：新增 `tools/audit_subscriber_bus.py`
    （解析每个订阅类与它自己的监听器，把事件类型在合并 jar 里沿继承链判成是否 IModBusEvent；
    实测 84 个订阅类里**只报出这一条**，修复后 0）。
- **验证（真跑）**：新工具 `tools/check_neoforge_api_floor.py` —— ① 整套源码对着 21.1.150 编译 ✓
  `BUILD SUCCESSFUL`；② `--run` 在 21.1.150 上启专用服务器 ⇒ **`Done (0.958s)!`** ✓，3 个 common mixin 注入 ✓，
  0 条 mod 相关 ERROR/FATAL ✓。
- **环境坑**：dev 运行加载的是 **`run/mods/scg2_maid_compat-...jar`**（女仆兼容不是源集 mod，`gradlew build`
  不刷新它）⇒ 它一直带旧范围，导致 floor 运行连续失败 3 次；`--run` 现在会先刷新它。另外 `runServer` 不会自己退出，
  工具检测到 `Done (` 后会按 `devlaunch` 精确停服（绝不按进程名杀，见 §38）。
- **门禁**：`javac` 0 错误 / `build` ✓ / **22 个审计 0**（新增 `audit_subscriber_bus`）/ `verify_installed_jar`
  **160/160**（新增 3 项：host floor、**嵌套 jar** floor、`ModCapabilities` 命名 MOD 总线；对上一版 jar 实测
  **3 项全 FAIL / 157-160**）；已安装。
- **未验证**：低版本**客户端**（21.1.150）没实机跑过（本次只验证了专用服务器）。
  floor 运行里的两类 ERROR 都不是本 mod 的：① 该 dev 世界是在装了联动模组时存的（旧区块缺内容）；
  ② `mech_press/depleted_diamond_steel` 引用 `create:experience_nugget` 却没有条件 —— **0.5.5 原版就是如此**，
  只有未装 Create 的玩家会看到两行 ERROR，未擅自改（要改需同时写进 `convert_resources.py` 之后的修复脚本）。

---

## §71 公开发布：GitHub 已推送 + CurseForge 文案已备

- **公开仓库**：<https://github.com/ELMOSYG/scorched-guns-0.5.5-Unofficial-Port>（`origin/main` = `dbf4beb`，
  单一初始提交，7827 个文件 / 41.2 MB）。本地 `master` **保留全部历史**，以后开发走 `main`。
- **公开前剔除**（`tools/prepublish_audit.py` 新工具实测）：**121.5 MB 第三方 jar 被跟踪**
  （`libs/` 97 MB、`maid-compat/libs/` 24 MB、`libs-compile/`）+ **上游 0.5.5 jar** ⇒ 全部排除；
  `build-logs/` 559 个 / 115 MB ⇒ 只留 6 个（1.8 MB）；`tools/rcon_*.py` 13 处硬编码 RCON 密码 ⇒
  改为环境变量（`tools/harden_rcon_password.py`，`--selftest` 实测命中且幂等）。
- **授权**：上游是 **GPL-3.0**（0.5.5 jar 元数据实测）⇒ 本移植沿用 GPL-3.0，`LICENSE` 放全文（gnu.org 下载）；
  `NOTICE` 写明上游、音效 CC0、汉化包来源、女仆兼容作者、第三方依赖不随仓库分发；README 重写（英/中）。
- **CurseForge**：`CURSEFORGE.md` 备好可直接粘贴的描述、依赖关系表、更新日志与上传清单；
  项目已由玩家创建：**Scorched Guns 2 0.5.5 Unofficial port**（id 1709719，
  <https://www.curseforge.com/minecraft/mc-mods/scorched-guns-2-0-5-5-unofficial-port>），
  标题/简介/描述已填好（描述 = 我给的 HTML 块，经公共 API 复核逐字符一致）。
  ⚠️ 页面上现有文件 `scguns-0.5.5.jar`（19,393,354 字节）**早于 §72/§73 的修复**，需用
  `build/libs/scguns-0.5.5.1.jar`（19,392,915 字节）作为新文件替换；另需确认许可 = GPL-3.0、
  Framework/GeckoLib/Curios 设为 Required。
- **待玩家**：`NOTICE` 里汉化包署名仍是占位文字。

---

## §72 `scguns:niami` 无法射击 —— 1.21 的箭要求"发射它的武器"，移植传了空物品

> 玩家："scguns:niami 无法正常射击"。

- **数据无问题**：`data/scguns/guns/niami.json` 与 0.5.5 **逐字一致**（41 字段全同）；它是那把**射箭的枪**
  （`firesArrows: true`、projectile/弹药都是 `minecraft:arrow`、`weaponType: special`、`semi_automatic`）。
- **复现**（FakePlayer + `ServerPlayHandler.handleShoot`，专用服务器）：
  `THREW java.lang.IllegalArgumentException: Invalid weapon firing an arrow`。
- **根因**：1.21 的 `AbstractArrow` 在**服务端**且 weapon 非 null 时，**空栈直接抛异常**
  （`.refs/nf-src/.../AbstractArrow.java:97-100`）。0.5.5 调的是 1.20.1 的 `new Arrow(world, player)`
  （没有 weapon），移植改成传 `ItemStack.EMPTY` ⇒ 每次都在 `getArrow` 里抛 ⇒ **整发子弹被丢弃**
  （无箭、不扣弹药、无枪声）。单人同样中招（集成服 level 也是 ServerLevel）。
- **修法**：传 **`null`**（参数本就是 `@Nullable`，与 0.5.5"没有 weapon"语义一致）。
  ⚠️ 我第一版传**枪本身**（已实测通过），但查源码发现 `AbstractArrow` 会把 weapon **存进箭的存档**
  ⇒ 每支箭都会带一份整枪拷贝 ⇒ 改成 null 并重新实跑验证。
- **顺带修掉 3 处服务端 NPE**：`AnimatedGunItem.registerControllers` 只在客户端跑 ⇒ 服务端 controller 恒 null，
  而 `GunFireEvent$Post`（**构造函数**里，事件还没投递 ⇒ Post 的所有监听器都不执行 ⇒ 服务端每枪都少掉
  击退/热管/枪灯/抛壳/卡壳音效）、`GunEventBus.postShoot`、`ReloadTracker` 装填完成分支（服务端 tick）
  都直接调用它。三处补 `!= null` 守卫。
- **防复发**：新增 `tools/audit_server_fire_paths.py`（`--selftest` 对修复前源码实测命中 **4 处**：
  1 个空 weapon + 3 个未守卫 controller；已加入 CI 静态检查）。第一版审计因"守卫窗口 300 字符被注释挤爆"
  **误报了刚修好的三处**，改成剥注释 + 按变量名精确匹配后归零。
- **实测**：修复后 `niami -> arrows spawned=1，ammo 6→5`；对照枪开枪成功且**无异常**。
- **门禁**：`javac` 0 / `build` ✓ / **24 个审计 0** / `verify_installed_jar` **160/160** / 探针已删 / 已安装
  （备份 `.bak-213733`）。环境坑：上一轮 dev 服务器没停时下一次 `runServer` 会在启动阶段失败（§9 老坑）。
- **待玩家确认**：客户端表现（拉弓音效、持枪动画、箭命中表现）。与 §65「充能枪不能开火」无关（那是 PULSE 枪）。

---

## §73 版本号改为 **0.5.5.1**（移植自己的发布号）

- `gradle.properties` 的 `mod_version` 是唯一真相 ⇒ 决定 jar 名（`build/libs/scguns-0.5.5.1.jar`，
  18.49 MB）与 `neoforge.mods.toml` 里的 `version`。**内容仍是上游 0.5.5**，`0.5.5.1` 是本移植的发布号。
- 4 个工具原先硬编码旧文件名 ⇒ 改为**按 glob 发现最新产物**（`install_jar` / `verify_installed_jar` /
  `audit_tlm_isolation` / `probe_tag_ids`），以后升版本不必再改工具；`install_jar` 还会校验
  **包内声明的版本 == `mod_version`** 才安装。
- **必须处理的坑**：版本号进文件名 ⇒ 新 jar 不覆盖旧 jar ⇒ `mods/` 里两份同 mod id = 加载报错。
  `install_jar.py` 现在会备份并**删除所有旧的 `scguns-*.jar`** 再安装（实测：删除 `scguns-0.5.5.jar`，
  备份 `.bak-214132`）⇒ 实例里只剩一份。
- 验收：`clean build` ⇒ 产物只有 `scguns-0.5.5.1.jar`；`verify_installed_jar` **161/161**
  （新增"包内版本 == mod_version"）；文档已同步（README / CURSEFORGE / HANDOFF §2+§9）。
  ⚠️ §69 之前历史段落里的 `scguns-0.5.5.jar` 是当时的真实文件名，不要照它找文件。


