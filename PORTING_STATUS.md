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
| **Create 联动配方"全是 0 tick"** | ✅ **已修（真正 0 tick 的只有 14 条；加错方向会丢 45 条配方）**：Create 6 用 `Codec.INT.optionalFieldOf("processing_time", 0)` 读该字段（`javap` 实测）且**不夹紧** ⇒ 没写就是 0 tick；但 `ProcessingRecipe#validate` 会**主动拒绝**类型不认的字段（`Durations have no impact on this type of recipe`）⇒ `canSpecifyDuration()` 只在三处被覆写（`AbstractCrushingRecipe`/`BasinRecipe`/`CuttingRecipe`）⇒ **crushing/milling/mixing/compacting/cutting 接受，pressing/filling/splashing/deploying 拒绝**。逐类型点数：**crushing 150、milling 7、cutting 2 原本都有值**；**唯一真的 0 tick 是 14 条 `create:mixing`**（火药/硝化火药/铜与钢 blend/铁 blend/soul_soil/peal，即所有搅拌工序）——0.5.5 的 jar 里同样一个都没有 ⇒ **不是移植回归**，玩家要求修才修。**`create:mechanical_crafting`（138 条枪械配方）根本没有这个字段**：Create 6 已删除，`MechanicalCraftingRecipe` 只是 `ShapedRecipe` 薄壳，工作台时长运行期由格数算出。改动 = **14 文件 +14 行**（mixing 100 tick）。`tools/fix_create_processing_time.py`（幂等 + selftest，按表**加/删**）+ 新增 `tools/audit_create_processing_time.py`（第 41 个，**自己解析 Create jar 的 class 头沿继承链判定**，不信硬编码表）。**教训**：第一版按"看起来该有时长"给 218 个文件全补，`runServer` 当场炸出 45 条被丢（16 deploying + 26 seq_assembly + 3 splashing；另有 1 条 filling 因 `create_enchantment_industry` 条件门禁**只在玩家装了 Create AEI 时才消失**）。**实测**：`Done` ✓、**`Parsing error loading recipe` = 0** ✓、tag 0 ✓、ERROR/FATAL 0 ✓。**未实测**：搅拌 100 tick 的手感（改表里一处即可）。见 HANDOFF §83.1 |
| **缺失 Sable 时进存档就崩** | ✅ **已修（方法里的守卫挡不住链接期校验，用最小 JVM 实验证明过）**：`PhysicsStructureHelper.poseAt` 的**返回类型**是 Sable 的 `Pose3dc`、值却由 `Pose3d` 算出 ⇒ 类型检查校验器为判可赋值性**必须加载这两个类** ⇒ 该类**被链接**（炮塔第一次 tick，因为 `toWorld` 无条件调用）时就抛 `NoClassDefFoundError: dev/ryanhcode/sable/companion/math/Pose3dc`，**守卫根本没机会执行**。最小实验：同形状的代码直接崩、经**嵌套 holder** 且守卫先 return 则不崩 ⇒ **规律：可选模组的类型不能出现在"会被无条件链接的类"的签名里**（`instanceof` 里安全、调用的中间值也安全）。改法：Sable 调用整体搬进新类 `compat/SablePhysicsBridge.java`，`PhysicsStructureHelper` 只留 vanilla 类型，经嵌套 holder 取桥。新增 `tools/audit_optional_api_refs.py`（第 42 个，源码签名 + **产物 jar 的描述符**双层检查，`L` 前缀区分描述符与方法体引用）。**反向验证**：对改之前的旧 jar 跑，它准确报出 `PhysicsStructureHelper.class has dev/ryanhcode in a field or method descriptor`；重建后 0（jar 里只剩桥那 1 个 class）。顺带订正 `GuardFriendlyRules` 里那句**导致本 bug 的错误注释**。**未实测**：需玩家在没装 Sable 的实例里进一个**有炮塔**的存档。见 HANDOFF §83.2 |
| **刷新的敌人枪械等级错误地跟着玩家同步** | ✅ **已修（0.5.5 本来就是"敌人慢玩家一级"）**：§82.29 修文案时让 `getAvailableMobTiers()` **把本级也加进去**，而 `GunnerMobSpawner` **读的是同一个列表** ⇒ 敌人立刻拿到玩家刚解锁的那一级（§82.29.6 当时已预警"若你觉得敌人不该立刻跟上，说一声"）。对着 0.5.5 反编译产物逐行核对：该方法**只返回 `previousTierIds`**，而 `ANTIQUE` 的 `previousTierIds` 为空 ⇒ **玩家只有古典阶段时列表为空 ⇒ `hasValidTiers` 为假 ⇒ 一只带枪的敌人都不刷**，与玩家举的例子完全一致。改法：**拆成两个方法** —— `getAvailableMobTiers()` 恢复 0.5.5 语义（生成器 + 两条 `/progression` 指令用，指令标签是"会出现什么敌人"，必须与实际一致），新增 `getUnlockedTiersNewestFirst()`（含本级 + `level>0` 守卫 + 降序，**只有解锁提示那一行**用）。审计 `audit_progression_messages.py` 规则**双向重写**并新增调用点规则；**反向验证 3/3** 全被抓；`show_progression_messages` 的模拟输出与 §82.29.4 **逐字一致** ⇒ 文案无回归。**按玩家决定保持 0.5.5 原样、不动的两条带枪路径**：`gunner_mobs.json`（掠夺者固定拿边疆枪、猪灵拿猪灵枪）与 `entity/equipment/*.json`（6 个本模组生物 1.0 概率钻石钢枪）—— 所以"古典阶段仍可能有掠夺者拿边疆枪"是**预期行为**。**未实测**：古典阶段不再刷带枪手、边疆阶段只出古典枪、提示仍无"袭击"二字。见 HANDOFF §83.3 |
| **验收（本轮）** | **42 个审计全 0**（新增 2 个）、**12 个改写脚本 selftest 全 0**、`javac` 0 错误（1011 文件）、`gradlew build` ✅、**专用服务器 `Done` + `Parsing error loading recipe` 0 + `Couldn't load tag` 0 + ERROR/FATAL 0**、`verify_installed_jar` **259/259**。另记一条环境坑：git-bash 里 `cmd /c` 被 MSYS 改写 ⇒ `:clean` 没跑、产物时间戳不更新，**却仍打印 `BUILD SUCCESSFUL`**；正确写法是 `cmd //c`，且 `JAVA_HOME` 用正斜杠。见 HANDOFF §83.5 |

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

---

## §74 袭击系统"不抗卸载"：玩家一死血条消失、战利品拿不到（**上游 0.5.5 就有的缺陷，按玩家要求修掉**）

- **根因**：`ActiveRaid` 用 `level.getEntity(uuid)` 找 boss —— 它对**区块未加载**的实体返回 **null**，
  与"已死"同值。而 `tick()` 写的是 `else { this.endRaid(this.bossConfirmed); }`，`bossConfirmed` 一旦确认
  就永久为 true ⇒ **"看不见 boss" 被当成 "boss 已被击败"**：血条对所有人隐藏、袭击被从 `activeRaids`
  与存档移除，而专属战利品只在 `onEntityDeath` 里、且袭击仍被跟踪时才掉落 ⇒ **玩家回来杀掉 boss 只剩普通掉落**。
  触发条件正是"卸载"：玩家死亡后重生到远处 / 被传送 / 走开 ⇒ boss 区块卸载 ⇒ 立刻误判。
- **顺带两处**：① `validateBoss()` 对"解析不到"只等 30 秒就判失败（恢复存档的袭击尤其容易中招）；
  ② 那条等待路径 `setChunkForced` 之后**从不解除** ⇒ 区块永久加载。
- **修法**：把三种状态分开 —— 已加载存活（正常）/ 已加载已死（`endRaid(true)`）/ **解析不到（不是失败：
  计数 + 强制加载 boss 上次所在区块 + 袭击继续，含血条与战利品表）**，只有连续 5 分钟仍找不到才
  `endRaid(false)` 并提示新语言键 `raid.scguns.boss_lost`；`endRaid()` 一定释放强制区块。
- **实测**（临时探针，已删）：模拟卸载用 `setRemoved(UNLOADED_TO_CHUNK)`（= 引擎在区块卸载时所做的），
  200 tick 后 **`active=true tracked=true barVisible=true`（SURVIVED THE UNLOAD）**；另一组让 boss
  `kill()` ⇒ `active=false` 且**专属战利品照掉**（grapeshot ×28 / powder_and_ball ×30 / antique_flare ×1）。
- **防复发**：新增 `tools/audit_raid_unload_safety.py`（`--selftest` 对修复前源码实测命中 **8 条**；已入 CI）；
  语言文件 EN/ZH 各加 1 键（1807/1807 对齐）。
- **门禁**：`javac` 0 / `build` ✓ / **25 个审计 0** / `verify_installed_jar` **161/161** / 探针已删 / 已安装
  （备份 `.bak-201801`）。

---

## §75 袭击"有时候刷在洞穴层"（**上游 0.5.5 的落点逻辑缺陷，已修**）

- **根因（一个地方两个错）**：`findRaidSpawnLocation` 用 `playerY < 50` 猜"玩家在地下"，
  而洞穴搜索 `for (int yOffset = -5; yOffset <= 5; yOffset++)` **从玩家脚下 5 格开始** ⇒
  站在**露天低处**（峡谷底 / 深谷 / 下潜海里）的玩家被判成"地下" ✓，且 y<50 地层洞穴密布 ✓
  ⇒ **袭击被优先塞进玩家脚下的洞里**（玩家报告的症状）；反方向：**浅层洞穴（y>50）**里的玩家会被判成
  "地表" ⇒ 袭击刷到头顶地表。
- **修法**：不再猜 —— 先按**玩家自己的 Y** 找可站立点（0 偏移优先 ✓，然后 ±1..±8 ✓），
  最后才回退到该列的**地面高度**，且要求 `|groundY - playerY| <= SPAWN_Y_WINDOW(8)` ⇒
  崖顶/山顶这类"头顶的地面"永远不会被选中；15 次候选都不合格就不开袭击（同 0.5.5）。
- **实测**（临时探针，已删）：落点 `y=73` vs 玩家 `y=74`（delta −1 ✓ ON THE PLAYER'S LEVEL），
  脚下 `grass_block`、落点空气；探针同时打印"0.5.5 会先试 y=69"作为机制证据。
- **防复发**：新增 `tools/audit_raid_spawn_level.py`（`--selftest` 对修复前源码实测命中 **6 条**；已入 CI）。
  第一版审计又因 **`playerY < 50` 出现在我自己的注释里**而误报 ⇒ 加 `strip_comments()`（只读代码不读注释）。
- **门禁**：`javac` 0 / `build` ✓ / **26 个审计 0** / `verify_installed_jar` **161/161** / 探针已删 / 已安装
  （备份 `.bak-202642`）。
- **待玩家定**：是否要"袭击永不上地下"（哪怕玩家在挖矿）—— 这是玩法选择，可在 `Config.COMMON.raids`
  加开关，默认保持"跟着玩家"。

---

## §76 袭击**只在地表刷**（玩家拍板：洞穴层 = 刷在玩家找不到的地方）

- **规则**：`boolean surfaceOnly = !level.dimensionType().hasCeiling();` ⇒ 主世界/末地一律取该列
  **自己的地面高度**（`getHeightmapPos(MOTION_BLOCKING_NO_LEAVES)`）并要求 `isStandableSpawn && canSeeSky`
  ⇒ **玩家在洞里挖矿时，袭击仍落在他正上方的地表**（他上去就能找到），树冠/悬垂/屋顶遮挡的候选点一律拒绝；
  **下界例外**（那里 `hasCeiling()` 为真，"地表"是基岩顶 ⇒ 改按玩家自己那层 = 下界地面）。
- **为什么高度图不会指进洞穴**：洞穴天花板本身就是阻挡移动的方块 ⇒ 该列"最高阻挡方块"在洞穴之上
  ⇒ 高度图必然在地表之上（旧代码是**人为**往下找才进洞的）。
- **实测**（探针已删）：玩家 y=71 ⇒ 落点 y=70（canSeeSky=true、脚下草方块）；玩家 **y=30 地下** ⇒
  落点**仍是 y=70 地表**（ON THE SURFACE）✓。
- **顺带修掉门禁隐患**：`--selftest` 若拿 `HEAD` 当"修复前源码"，**修复一提交自测就自动空转**
  （`audit_server_fire_paths --selftest` 已实测 FAIL）。4 个 selftest 改为**固定提交**：
  `cdab0ec` / `a70bcb3` / `9ce181e` / `e9d8e03`，复跑全部命中。
- **门禁**：`javac` 0 / `build` ✓ / **26 个审计 0** / `verify_installed_jar` **161/161** / 探针已删 / 已安装
  （备份 `.bak-203422`）。

---

## §77 玩家在地下时，**自然**袭击不再生成（手动不受影响）

- **改法**：午夜起事那一刻（`checkForNightlyRaidSpawn` 的 18000 分支）先判
  `if (isUnderground(level, player.position())) { saveData.removeScheduledRaid(dimension); return; }`
  ⇒ 今晚不来（排期丢弃），明晚重新掷骰。**在 18000 判而不是 13000**：黄昏到午夜玩家可能已经下矿。
  **信号弹与指令仍然照常**（那是玩家主动要的）。
- **判据**：`isUnderground = position.y < heightmapY - 8`（`UNDERGROUND_MARGIN = 8`）。
  ⚠️ 第一版写的是"低于地面 **且** `!canSeeSky`"，**那是错的**：站在地表房子里的玩家因屋顶抬高高度图
  会被误判成地下 ⇒ 整晚在屋里就永远等不到自然袭击（树下同理）。改成只比高度 + 8 格余量，语义与 §76
  落点搜索的窗口一致。
- **实测**（探针已删）：地表 y=71 ⇒ false；**地表屋内（头顶 3 格屋顶）⇒ false**；地下 5 格 ⇒ false；
  地下 40 格 ⇒ **true**（自然袭击不生成）。
- **门禁**：`audit_raid_spawn_level.py` 扩展（午夜分支必须调 `isUnderground`、判据必须带 8 格余量），
  `--selftest`（固定提交 `e9d8e03`）实测报 **5 条**、当前源码 0。`javac` 0 / `build` ✓ /
  **26 个审计 0** / `verify_installed_jar` **161/161** / 探针已删 / 已安装（备份 `.bak-203859`）。
- **未验证**：真实午夜链路需要真人玩家等一个游戏夜（专用服务器上排期需要 `level.players()` 里有玩家）。
  快速自测：`nightlyRaidChance` 调 1.0，在地下等到午夜应当不刷，上来在地表过一夜应当刷。

---

## §78 最终形态：落点**只在地表**、自然刷新**只在海平面以上**（与幻翼刻意不同）；地下机制清零

- **玩家三轮定死的规则**：① 地下刷新机制删掉；② 低于海平面不自然刷新（像幻翼）；③ **头顶有方块也要能刷**
  ⇒ 与幻翼做差异化。§77 的自造判据（"低于该列地面 8 格"）**已被取代**（它会把站在地表房子里的玩家
  判成地下）。
- **最终规则**：
  - **落点**：`findSurfaceSpawn` = 该列地面高度 + 可站立 + `canSeeSky`，**无任何维度例外**；
  - **自然刷新**：`canGetNaturalRaid = playerY >= level.getSeaLevel()` —— **只看高度**，头顶有无方块不影响；
  - **手动**（信号弹/指令）照常；**落点找不到**（如下界：地面=基岩顶、`LightLayer.SKY` 恒 0）⇒ 不开袭击
    并给一行提示（新键 `raid.scguns.no_surface`）。
- **删掉的残留**：`findSpawnAtPlayerLevel`、`SPAWN_Y_WINDOW`、`hasCeiling()` 维度例外 ⇒ 全类只剩一条落点路径
  （`findRaidSpawnLocation → findSurfaceSpawn`，小怪 `findHenchmanSpawnPos` 也改用它）。
- **顺带查清**：`canSeeSky` = **skylight==15**（`BlockAndTintGetter:22`），不是"头顶有没有方块"
  ⇒ 落点用它合适（满天空光=露天），门禁不能用它（会把屋里/树下也否掉）。
- **实测**（探针已删，海平面 63）：地表 y=71 ⇒ true；**地表屋内 y=71 ⇒ true**（差异化的那一点）；y=31 ⇒ false；
  y=62 ⇒ false；y=63 ⇒ true。
- **门禁**：`audit_raid_spawn_level.py` 重写为最终形态（禁 `playerY<50`/洞穴搜索/`yOffset=-5`/
  `findSpawnAtPlayerLevel`/`SPAWN_Y_WINDOW`/落点里的 `hasCeiling`；要求午夜分支调 `canGetNaturalRaid`，
  且该判据必须用 `getSeaLevel` **且不得含 `canSeeSky`**），`--selftest`（`e9d8e03`）命中 **5 条**；
  语言文件 EN/ZH 各加 1 键（1808/1808）；`javac` 0 / `build` ✓ / **26 个审计 0** /
  `verify_installed_jar` **161/161** / 探针已删 / 已安装（备份 `.bak-204827`）。

## §79 自然袭击"当场可测"：新增诊断命令 `/scguns raid check`

- **玩家反馈**："这个不好测试，因为自然刷新的袭击在提示后要等很久才会来"。原因：黄昏 13000 抽签 +
  发警告，**18000** 才开刷（5000 刻 ≈ 4 分 10 秒）；而且六种失败全是静默的（抽签没过 / 目标是创造旁观 /
  raidLevel 0 / 海平面门不过 / 没有露天落点 / 已有袭击）——外面看都是"今晚没刷"。
- **做法**：`/scguns raid check`（不需权限，只能查自己）**报告调度器同一批判断**：
  `canGetNaturalRaid`（海平面门）、`findRaidSpawnLocation`（真落点搜索，含坐标）、
  `RaidSaveData.getScheduledRaid`（今晚排没排上、排给谁）、`PlayerGunProgression.getCurrentRaidLevel`
  + `RaidConfig.getRaidsForLevel`（等级与可用袭击），再加一行创造/旁观提示（最常见的白等一晚原因）。
  **刻意不做成触发器**（`raid start`/`startnext` 已是触发器，且触发器答不出"卡在哪一步"）。
- **改动**：`RaidManager.findRaidSpawnLocation` 由 private 改 public（补 `@Nullable`，逻辑未动）；
  `ModCommands` 新增 `raid check` 子命令 + `executeRaidCheck`；语言文件各加 **15** 键（1823/1823）。
- **顺带查清（只记录，未改行为）**：`minDaysBetweenRaids` 从未被任何代码读取 ——
  `canScheduleRaid/setLastRaidDay/getLastRaidDay` 是死代码（`setLastRaidDay` 无调用点），
  "最少间隔 N 天"目前完全无效；已在报告里写明，是否接上交给玩家决定。
- **实测**（专用服务器 + `FakePlayerFactory` 假玩家探针，四场景，探针已删）：15 条键全部正常渲染，
  无原始 key、无 `TranslatableFormatException`。落点每次不同且**可低于海平面**（门管玩家高度、落点管那列地面，
  互不干涉）；`raidLevel=1` 打出翻译后的 `Antique Raid, Frontier Raid`；假玩家不在玩家列表 ⇒ 排期行按设计回退 UUID。
- **快速测试流程**（已收录进游戏内提示与 HANDOFF §79.4）：生存模式 + `raidLevel != 0` 前提下
  `/time set 12000` → `/time set 13000`（抽签+警告，未通过就重来）→ `/scguns raid check` → `/time set 17995`（5 刻后开刷）。
- **门禁**：新增 `audit_raid_check_command.py`（要求 raid 节点下确有 `check`、必须调真方法、**不得**调
  `startRaid/scheduleRaid/endRaid/surrenderRaid`、15 键 EN/ZH 齐且占位符与实参数一致），`--selftest`（`1bd9f60`）
  命中 **3 条**；`verify_installed_jar` 新增 5 条（含"探针不得随包发出"）⇒ **166/166**；
  `javac` 0（999 文件）/ `build` ✓ / **27 个审计 0** / 已安装（19397734 字节，备份 `.bak-211833`）。

## §80 补上 `minDaysBetweenRaids`（玩家发现"配置根本没被调用"）

- **事实**：`minDaysBetweenRaids` 自 0.5.5 起只在 `Config.java` 出现，`RaidSaveData.canScheduleRaid/
  setLastRaidDay/getLastRaidDay` **无任何调用点** ⇒ 选项对行为零影响（同组另外三个选项都有人读）。
- **语义按配置自己的注释实现**：`0 = 每晚`、`1 = 隔一晚`、`2+ = 更稀` ⇒ 严格 `>`，
  而非死代码里的 `>=`（那会让 0 与 1 完全一样，与注释矛盾——正是它从没被跑过的证据）。
  第 7 天刷过之后：0 ⇒ 下次 8，1 ⇒ 9，2 ⇒ 10。
- **接线两处**：① 黄昏抽签**之前**判冷却（冷却中连抽签都不发生）；② 自然袭击**真正开始**时才记录当天
  （必须 `hasActiveRaid()` 为真，故"门不过/没落点/播种失败"的那晚不算数）。**手动（信号弹/指令）不受影响**。
- **实测**（专用服务器探针，一次性实例，不动存档，探针已删）：真值表逐格吻合（0/1/2 ⇒ 8/9/10，
  `never` 哨兵始终允许）；**NBT 往返** `saved=42 reloaded=42`、day43/44 false、day45 true ⇒ 冷却能挺过重启。
  连测两晚需把该值设为 0（已写进游戏内提示）。
- **门禁**：新增 `audit_raid_cooldown.py`（黄昏必须调 `canScheduleRaid` 且由该选项驱动；必须"真正开始才记当天"
  且有 `hasActiveRaid()` 守卫；算术必须严格 `>`；手动路径不得碰冷却；诊断必须用同一个 helper；
  另加通用防呆：`Config.Raids` 每个选项都必须被真正作用于它的代码读到，`Config.java`/`ModCommands.java` 不算），
  `--selftest`（`c6519dc`）命中 **6 条**；`/scguns raid check` 换成三条真实冷却状态；
  `verify_installed_jar` 新增 2 条 ⇒ **168/168**；语言键 **1825/1825**；
  `javac` 0 / `build` ✓ / **28 个审计 0** / 已安装（19398550 字节，备份 `.bak-213047`）。

## §81 枪手 AI"不换弹"：两个真凶（服务端读客户端配置崩服 + 野生枪手的枪从没装弹）

- **真凶 A（已实测复现）**：`AIGunEvent.performGunAttack:122` 读 **`Config.CLIENT.display.fireLights`**。
  专用服务器不加载 `scguns-client.toml`，NeoForge 直接抛
  `IllegalStateException: Cannot get config value before config is loaded`（Forge 1.20.1 是**返回默认值**，
  所以这是迁移引入的）。而它在 `GunAttackGoal.consumeAmmo` **之前** ⇒ **弹匣永不减少** ⇒
  `getAmmoCount > 0` 恒成立 ⇒ **AI 永远不换弹**（与玩家描述逐字吻合），且每次开枪都
  `ReportedException: Ticking entity` **崩服**。探针（修复前）：`ammo=3` 不动、t≈80 崩服。
- **真凶 B（上游 0.5.5 就有的死守卫，§13 漏改一个文件）**：`GunnerMobSpawner.createModifiedGun` 的
  守卫 `NbtHelper.getTagForWrite(gunStack) != null` 对刚 new 的 ItemStack **恒假** ⇒ `AmmoCount` 预填
  **从未执行** ⇒ 野生枪手（掠夺者/卫道士/猪灵…）的枪**连 custom_data 都没有** ⇒ 读作 0 ⇒ 首次接敌
  **先换弹**而不是开火。§13 修的 `RaidManager` / `EntityEquipmentConfig` 是同模式的两处兄弟路径。
- **修复**：`Config.clientOr(ConfigValue)`（已加载用真值，未加载用默认值 = Forge 行为），服务端可达的
  `Config.CLIENT` 读取全部改走它（`AIGunEvent`/`TemporaryLightManager`/`GunProgressionEventHandler`/
  `ProjectileEntity`×2/`LightningProjectileEntity`/`SulfurGasCloud`×3），`client/` 包内保持原样；
  同时拆掉三处 `catch (IllegalStateException)` 局部补丁（正是它们掩盖了其余六处）；
  `GunnerMobSpawner` 改用 `getOrCreateTag`，与另两处同形。
- **实测（修复后，同一探针）**：`ammo 3→2→1→0` ⇒ `RELOAD STARTED`（15 刻）⇒ `RELOAD ENDED ammo=12` ⇒
  继续射击，整条链路闭合；野生枪手 `customData={AmmoCount:2}`（修复前无 custom_data）。
  `GunAttackGoal` 与 0.5.5 仅剩 NBT 访问差异 ⇒ **换弹逻辑本身一直是对的**，坏在前面两处。
  141 把枪 `maxAmmo` 全 ≥1 ⇒ 不存在补 0 死循环；`reloadTimer` 6–125 刻（多数 20–60）。
- **门禁**：新增 `audit_client_config_side.py`（`client/` 外读取必须走 `clientOr`；禁止
  `catch (IllegalStateException` 兜底；扫描前剥注释），`--selftest`（`d950b34`）命中 **13 条**；
  `verify_installed_jar` 新增 4 条（枪手枪必须装弹、恒假守卫不得回归、`clientOr` 必须存在且被 `AIGunEvent` 使用）
  ⇒ **172/172**；`javac` 0 / `build` ✓ / **29 个审计 0** / 已安装（19398807 字节，备份 `.bak-214429`）。

## §82 警卫村民（Guard Villagers）兼容：警卫持枪开火，且不打自己人（可选前置，不装也照跑）

- **参考**：玩家 1.20.1 的独立兼容 mod `guard_guns`（战利品表塞枪 + mixin 进 SCG 自己的
  `hasGunAttackGoal` + 友伤两层）与新版上游 1.21.1（`gunner_mobs.json` 加 `guardvillagers:guard` +
  按实体 id 识别 + 一堆 mixin）。本轮**采纳数据驱动与警卫专用 AI**，但**零 mixin**：持枪/开火/友伤
  三件事全部用主机代码 + 实体 id 判断完成（实测证明"优先级"可完全替代上游的 `GuardMeleeGoalMixin`）。
- **实现**：`compat/guardvillagers/` 四个类（`GuardVillagersCompat` 只用实体 id、**不引用 Guard 类**；
  `GuardFriendlyRules` 是全仓**唯一**提到 GV 类的文件，只在确认是警卫后才被调用；`GuardGunAttackGoal`
  搬 1.20.1 那份 AI 且不依赖 GV；`GuardVillagersEvents` 用 `LivingIncomingDamageEvent` +
  `LivingKnockBackEvent`）；三个钩子（join / tick<2 / 装备变更，抽签只记一次）；`gunner_mobs.json`
  新增 `guardvillagers:guard`（spawn_chance 1.0、8 把枪、armor 空）；配置 `common.compat.guard_gun_accuracy`
  （默认 3.5，取自玩家 1.20.1 那份配置）；`mods.toml` 声明 optional。
- **实测（四轮，过程即证据）**：① 只挂 join 钩子 ⇒ 枪 20 刻后被 GV 自己的铁剑覆盖；② 三处重试 ⇒ 枪保住了
  但 `shots=0`；③ 打印目标列表 ⇒ `GuardMeleeGoal p=3 running=true` 压死同优先级的枪手目标；
  ④ 改优先级 2 ⇒ `GuardGunAttackGoal p=2 running=true`、`shots=15 refills=7`（弹匣 2 发的枪打空→换弹→补满）、
  村民全程 20 血（且就在连线上）。**不装 GV** 用 `-PnoIntegrationRuntime=true` 实跑：`loaded=false`、
  无 `NoClassDefFoundError`、普通枪手照常。
- **顺带修掉一条死配置**：`gunner_mobs.json` 的 `weapon_drop_chance` 一直被解析却从未使用（主题枪手掉枪率
  等于原版默认），现已在 `equipThematicGun` 接上（精英仍 0.0 不掉）。
- **门禁**：新增 `audit_guard_compat.py`（只有 `GuardFriendlyRules` 可提及 GV 类、常量与 JSON key 必须一致、
  三个钩子必须在、`hasGunAttackGoal` 必须算上警卫目标、**必须加在优先级 2**、友伤两层在、精度可配置、
  `mods.toml` 声明 optional），`--selftest`（`8863bb4`）命中 **13 条**；`verify_installed_jar` 新增 9 条
  ⇒ **181/181**；`javac` 0（1003 文件）/ `build` ✓ / **30 个审计 0**。

**友伤机制的更正（玩家指出原兼容 mod 那套其实没用）**：原来的 `Projectile.canHitEntity`、
基类 `onHitEntity`、`findEntityOnPath/findEntitiesOnPath` 三层在**本 mod 的子弹上全部无效**
（`ProjectileEntity` 不是原版 `Projectile`；二十多个子类覆盖 `onHitEntity` 不调 super；
`LightningProjectileEntity`/`ShotballProjectileEntity` 走自己的搜索）。改成女仆兼容那套：
注入 **`ProjectileEntity.getHitResult`**（全仓 6 处调用点、无任何子类覆盖 ⇒ 唯一漏斗），
返回 `null` 让子弹**穿过**友军（伤害/破盾/impactEffect/元素爆裂一并跳过）。
**A/B 实测**：同一条射线、同一个村民，只换弹体主人 ⇒ 警卫弹对村民 `null`、对目标命中、
非警卫弹对村民照常命中 ⇒ 拦得住、不误拦、且是警卫专属。事件层（`LivingIncomingDamageEvent` +
`LivingKnockBackEvent`）保留作兜底。

## §82.6 玩家验收的两条：持枪率 100% + 射速不正常（都已修 + 实测）

- **持枪率**：`spawn_chance` 当时照抄新版上游的 `1.0` ⇒ 人手一把。改成 **0.25**（= 玩家 1.20.1 那份兼容的值），
  每只警卫只抽一次签。**6 次开服、240 只警卫实测：57 只持枪 = 23.8%**（与 25% 相差不到 1 个标准差）。
- **射速**：那 8 把枪的 `fireMode` **全是 `semi_automatic`**，而旧实现把 `rate` 直接当"两发间隔" ⇒
  `callwell`（rate=2）变成**每秒 10 发**；并且**绕过 mob 自己的节奏**：不看 `mobFireRateMultiplier`、
  没有点射/间歇（`GunAttackGoal` 是打 1–2 发、歇 40–80 刻）、换弹被夹到 10–40 刻。
  现在整套照 `GunAttackGoal`：间隔 `rate × mobFireRateMultiplier`、点射 `1+rand(2+ai_difficulty/2)`、
  间歇 `40+rand(40)` 刻 × `mobBurstDelayMultiplier`、换弹用该枪自己的 `reloadTimer`、
  `ai_difficulty` 从 `gunner_mobs.json` 传入。
  **实测**（1 警卫 + 4000 血 NoAI 掠夺者，1200 刻）：`pax`(rate=9) 射击刻 97/108/193/205/269
  ⇒ 点射内 **11–12** 刻、点射间 **64–85** 刻；`winnie_millend`(rate=14) 间隔 **15** 刻。
  探针踩的三个坑也记进 HANDOFF §82.6.2：无敌目标不会被认作敌人（`canBeSeenAsEnemy()`）、
  1.21.1 属性要用 `minecraft:generic.max_health`、`String.formatted` 被 `+` 抢先绑定导致命令带 `%d` 发出。

## §82.7 / §82.8 玩家后续三条（AI 被接替 / 开火逻辑改走女仆兼容那套）

- **§82.7 "scgun 的枪手 ai 接替了警卫的 ai"（真的）**：① 枪手目标 `setFlags(MOVE|LOOK)` ⇒ 只要警卫"有枪+有目标"，
  Guard Villagers 自己的近战/巡逻回检查点/回村/跟随英雄/开门/闲逛/看玩家**全部无法启动**；
  ② `extendFollowRange()` 把警卫跟随范围从 ~20 拉到**64** ⇒ 持枪警卫追出村子。
  修法：**不占任何旗标**、**不碰任何导航/移动**（"保持射程/后退/让位"这些侵略者走位全删）、
  装备时改 `resetFollowRange`。实测：`GuardMeleeGoal` 与 `GuardGunAttackGoal` **同时 RUNNING**（修复前不可能）、
  follow range 19/20/22（不再 64）、仍能开枪。顺带修掉"目标短暂切换 ⇒ 目标重启 ⇒ `start()` 重置倒计时 ⇒ 一发不发"。
- **§82.8 开火逻辑改成女仆兼容那套**：新增主机共用管线 `entity/ai/MobGunFire`（源自 `SC2GunCompat.performGunAttack`：
  射速链 `GunEnchantmentHelper.getRate`（附魔→配件）、玩家扣弹规则（`IgnoreAmmo` + 幽灵弹 `RECLAIMED`）、
  消音/附魔开火音效、抛壳、挥手、威胁、含弹匣配件的弹匣容量）。警卫目标只负责"何时开火"，开火全交给管线。
  **A/B 实测**：`callwell_conversion` 附魔 Trigger Finger 2 后 `enchantedRate` 15→11、`fireInterval` 15→11
  （基础 rate 仍 15）；`IgnoreAmmo` 打开时开枪而弹药不减。
  `GunAttackGoal`（袭击怪）**未改**，维持 0.5.5 行为。
- **门禁**：审计新增"警卫必须走 `MobGunFire`、不得自己 `performGunAttack`/扣弹"与"管线必须保留六条规则"，
  `verify_installed_jar` 新增 4 条 ⇒ **186/186**；`javac` 0 / `build` ✓ / **30 个审计 0** /
  已安装（19413980 字节，备份 `.bak-235937`）。

## §82.9 警卫改用**本体枪手 AI**（玩家："自己的枪手 ai 从来没有被调用，警卫只会在近战时随机开枪"）

两条都成立，而且第二条正是 §82.7"把移动还给警卫"的直接后果：警卫自己的 AI 只会往近战冲
（`GuardMeleeGoal` 就是 `MeleeAttackGoal`），于是持枪警卫贴脸开火、路上偶尔放两枪。

- **改法**：`GuardGunAttackGoal` 现在只有 15 行 —— `extends GunAttackGoal<PathfinderMob>`，
  只选性格（`AIType.TACTICAL`）并把准度设成 `common.compat.guard_gun_accuracy`（`accuracyModifier` 是 protected）。
  本体的枪手 AI **不占任何旗标** ⇒ 警卫的巡逻/回村/开门/闲逛照常；它还会**按枪的 idealRange 逼近、太近后退、
  点射/间歇、TACTICAL 找掩体** —— 这些正是自己那套写不出来的。射速链/扣弹/音效走 §82.8 的 `MobGunFire`。
- **补上"持枪不近战"**：新增唯一一处改动 GV 行为的 mixin `GuardMeleeGoalMixin`（`@Pseudo` 打进
  `Guard$GuardMeleeGoal.canUse`，只在手上是 `GunItem` 时返回 false；枪一丢近战立刻回来），并在 `MixinPlugin`
  里按前缀门禁（没装 GV 就跳过；**其它 mixin 一律 true** —— §8.7 的教训）。
- **踩坑**：第一版把门禁探测放在 `acceptTargets` ⇒ Mixin 读门禁时它还没跑 ⇒ 守卫 mixin **静默未应用**
  （探针里 `GuardMeleeGoal` 照样 RUNNING 就是证据）；已改为 `onLoad` 探测 + `shouldApplyMixin` 兜底重探。
  `audit_mixin_plugin_gate.py` 也升级为能识别"按前缀收窄的门禁"，并要求该前缀下确有 mixin 引用被门禁的前置。
- **门禁**：`verify_installed_jar` 新增 3 条 ⇒ **189/189**；`javac` 0（1006 文件）/ `build` ✓ / **30 个审计 0** /
  已安装（19411182 字节，备份 `.bak-002750`）。
- **未验证**：dev 环境撞上两个第三方坑（`libs/prometheus` 让所有实体创建崩、Curios mixin 偶发失败，
  已记进 HANDOFF §9.0）⇒ "近战被抑制"这一条没跑完，需玩家进游戏确认（持枪警卫应在枪的射程上开火、不贴脸）。

## §82.10 玩家报"geckolib 注入失败？"——真凶是 mixin **准备阶段就加载了 `LivingEntity`**

崩溃报告里的"肇事 mod"（先是 GeckoLib，后是 Curios）都是**下一个受害者**：
`MixinTargetAlreadyLoadedException: target net.minecraft.world.entity.LivingEntity was loaded too early`
之后，**任何**往 `LivingEntity` 注入的 mixin 都会失败。两个真凶：

1. `GuardMeleeGoalMixin` 里的 `@Shadow protected PathfinderMob mob` —— **`@Shadow` 字段/方法签名的类型是在
   mixin 准备阶段解析的**，于是 `PathfinderMob → LivingEntity` 被提前拉起来（**方法体里**的类型引用是惰性的，安全）。
2. `MixinPlugin` 用 `Class.forName` 探测 GV 的 `Guard`（`extends PathfinderMob`）——准备阶段同样把 `LivingEntity` 拉起来。

- **改法**：近战 mixin 改**纯反射**（沿父类链找 `mob` 字段、调 `getMainHandItem`），不留任何实体类型的 `@Shadow`；
  `MixinPlugin` 改查 `LoadingModList.isModLoaded("guardvillagers")`（`onLoad` 一次 + `shouldApplyMixin` 懒重探，
  **失败即放行**），仍只 gate `...guardvillagers.` 前缀。`audit_mixins.py` 新增"`@Shadow` 字段的类型不得是实体类"
  （**去注释**后匹配 —— 上一轮就是被注释里的示例坑出假阳性）。
- **实测**：无 FATAL、两个警卫 mixin 都应用、服务器 `Done`。玩家另一个崩溃 `crash-...-client.txt` 是 Iris/Sodium，与本 mod 无关。

## §82.11 霰弹枪卡顿第一刀：**逐颗弹丸的内联 `tick()`**（已修，但"卡顿已解决"这句不能说）

- **定位**：玩家路径 `ServerPlayHandler.fireProjectiles` 与 mob 路径 `AIGunEvent.performGunAttack`
  都在循环里对每颗弹丸调 `tick()` ⇒ 一发 `boomstick`（26 颗）= 26 次完整弹丸 tick（射线/碰撞/命中效果）
  挤在封包处理里 ⇒ 只影响多弹丸武器，和"只有霰弹枪卡"完全对得上。**改法**：两处内联 `tick()` 都删掉，
  弹丸改由下一个游戏刻正常 tick。
- **冷/热测量的分量要说清楚**：**冷**测量 41 ms → 33 ms，但分解只合计 9.7 ms（差额全在首调用类加载）；
  而"热"那次**写错了**（清的是 `player.getCooldowns()` 而不是 `ShootTracker`，8 发里 7 发被冷却拦掉，
  `avg 0.01 ms` 无意义）⇒ 本节只能声称"逐颗内联 tick 已删除"，**不能**声称"卡顿已解决"。
- **顺手排除**（读代码）：每颗弹丸的生成包**不含**枪械物品（`ProjectileEntity.defineSynchedData` 为空，
  `S2CMessageBulletTrail` 每次开火只写**一次** `ItemStack`）⇒ 网络侧不是按弹丸数线性膨胀，不是主因。

## §82.12 卡顿真凶之一：弹道拖尾**每帧画三遍**、每颗弹丸**各刷一次批次**（已修，门禁全绿，实测交给玩家）

玩家给的 spark 链接**已失效**（`?raw=1` → `err: 404 - Not Found`；之前那个 `200` 的 HTML 只是 SPA 外壳，
随便编个 id 也是同样 5713 字节）⇒ 本轮不靠 profile，改为读代码 + 补审计，结论全是可复算的结构事实。

三处缺陷叠加，**都按"每颗弹丸"放大**（所以只有霰弹枪卡）：

1. **重复注册**：`ScorchedGuns` 与 `ClientHandler` 各注册一次 `BulletTrailRenderingHandler.get()`
   ⇒ 每个 `@SubscribeEvent` 跑两遍 ⇒ `onClientTick` 每刻两遍 ⇒ 每条拖尾 `age`/`position` 走两遍 ⇒
   **拖尾寿命只有配置值的一半**。
2. **两个渲染 hook**：同一处理器**还**订阅 `RenderLevelStageEvent.AFTER_PARTICLES`，而 `LevelRendererMixin`
   也调 `render()` ⇒ 每帧 **2 遍**；叠加第 1 条 ⇒ **3 遍**。
   （已查证两者**不在**不同坐标空间：`.refs/nf-src` 里 `LevelRenderer` 的关卡 PoseStack 就是 `new PoseStack()`，
   相机位移逐实体减掉、全程无 `mulPose`，事件各阶段传的是同一个栈 ⇒ 事件那两遍纯属重复，删掉不改观感。）
3. **逐条重建 + 逐条刷批**：循环里每条拖尾都新建 `RenderType.energySwirl(...)`（每次都 new 一整套
   `CompositeState`，无缓存）、`String.format`+`ResourceLocation.parse` 拼纹理、并**在循环内** `endBatch()`
   ⇒ 每帧 26 次绘制调用 + 26 个新建 RenderType/BufferBuilder；乘上前两条 ⇒ 每帧 **78 次**。
   隔壁 `TurretBulletTrailRenderingHandler` 从不逐条刷批（整帧一次）—— 炮塔那条路本来就是对的。

- **改法**：只留 `LevelRendererMixin` 一个 hook、删掉重复注册、按弹丸类型**缓存纹理与 RenderType**
  （26 条拖尾共用一个 RenderType ⇒ 同一个 buffer）、`endBatch()` 移到循环外、炮塔纹理提升为 `static final RenderType`。
  ⇒ 3 遍 → **1 遍**、26 次刷批 → **1 次**、26 个 RenderType → **1 个**、拖尾寿命回到配置值。
- **防复发（审计缺口才是这次漏掉的真正原因）**：`audit_bus_registrations.py` 的重复注册以前只是**打印提示**、
  退出码 0 ⇒ 改成**阻断**；`audit_duplicate_registrations.py` 只认 `new X()`/`X.class`、**不认识 `X.get()`**
  （本 mod 到处都是的单例写法）⇒ 补上 + 重复即失败 + `--selftest`；**新增** `audit_trail_render.py`
  （只能有一个 hook / 不得重复注册 / `endBatch()` 不得在"逐条拖尾循环**或其所调方法**里" / `render*` 里不得新建
  RenderType / `getTexture` 必须缓存；`--selftest` 对 `5b53ff4` 报 6 条）。写这条审计时自己踩了个坑：
  用 `/\*.*?\*/` 去注释会把 `ScorchedGuns` 注释里的 glob `.../*.json` 当成块注释起始、**吞掉一百行**
  （包括要检查的那行注册）⇒ 改成一次扫描同时处理行注释/块注释/字符串字面量。
- **门禁**：**31 个审计全 0** / `verify_installed_jar` **189/189** / `javac` 0 / `build` ✓ /
  已安装（19412068 字节，备份 `.bak-201538`）。
- **未实测**：帧时间数字**没拿到** —— 临时探针（在"旧写法=逐条新建+逐条刷批"与"新写法"之间自动交替统计每帧毫秒）
  需要客户端真的进世界挨一发霰弹，两次尝试都在客户端刚进服后 5 秒内被外部关掉（客户端 `Stopping!`、
  服务器 `BUILD SUCCESSFUL`，两个 JVM 同时退出）。探针**已删除、未进发布 jar**，`build.gradle` 里为验证加的
  两行 `programArgument` 也已还原（`git diff build.gradle` 为空）。
  **玩家决定这类实测自己上手** ⇒ 交给玩家验收两点：① 霰弹枪开火还卡不卡；② 弹道拖尾仍正常显示
  （保留的 hook 本来就是之前就在画的那一个，几何/矩阵/纹理一字未改，低风险；顺带会看到拖尾活得比原来久）。
  若仍卡，请给**新的** spark 链接或本地 `.sparkprofile`。
- **待办**：炮塔在不装 Sable 时崩溃（隔离 `PhysicsStructureHelper` 的 11 处 `dev.ryanhcode.sable.*` 引用）、
  警卫 AI 套用 1.21.1 移植版。

## §82.13 玩家实测反馈：拖尾"很挡视野，之前是好的"（已修，旋钮交给玩家）

旧的"好看"是 §82.12 那个 bug 的副产物：重复注册让拖尾**每客户端刻走两格**，于是它同时
**只活一半寿命**（`age` 每刻 +2）而且**跑到弹丸前方 2 倍距离**（常常已经埋进墙里/飞出视野）。
§82.12 把它改回"每刻一格"，拖尾于是**贴着弹丸**走（设计意图，位置修正是对的）并活满 `life`：
`boomstick` 是 26 颗 / `life 25` / `thickness 0.5` / `speed 5` ⇒ 每颗约 **1.8 格**光束，存活
12.5 tick → **25 tick**，同一时刻屏幕上的光束数量接近翻倍 —— 这就是"挡视野"。

- **修法（不恢复错位，改为可调 + 默认回到玩家习惯的时长）**：新增两个**客户端**配置
  `display.bulletTrailLifeMultiplier`（默认 **0.5**，`round(25*0.5)=13` ≈ 旧版 12.5）与
  `display.bulletTrailLengthMultiplier`（默认 1.0）；前者在 `ClientPlayHandler` 收到拖尾时折算 `life`，
  后者在渲染器里缩放光束长度。两处都在纯客户端路径、经 `Config.clientOr` 读取（`audit_client_config_side` 保持 0）。
- **顺带查清**：每把枪 JSON 的 `trailLengthMultiplier`（`boomstick` 写 2.0）**从没被渲染器读过**，是死配置；
  本轮没接上（接上会让光束变 2 倍长，与诉求相反），可用旋钮就是新加的客户端倍率。
- **玩家怎么调**：实例 `config/scguns-client.toml` 的 `[display]`：嫌挡视野 → `bulletTrailLifeMultiplier=0.25`；
  嫌光束长 → `bulletTrailLengthMultiplier=0.5`；想看完整拖尾 → 都设 1.0。重启生效。
- **门禁**：**31 个审计全 0** / `verify_installed_jar` **194/194**（新增 5 条：渲染器仍用 `energySwirl`、
  渲染器不再订阅 `RenderLevelStageEvent`、配置项存在、接收端折算 life、渲染端缩放长度）/ `javac` 0 /
  已安装（19412769 字节，备份 `.bak-202613`）。
- **仍未做**：若还嫌多，可选"只给多弹丸枪的前几颗弹丸画拖尾"（26 条 → 3~5 条，单发枪不受影响）。

### §82.13.6 玩家给出真正的规则：**开火后 10 tick 不渲染拖尾**（已实现）

玩家的第二次反馈把病根说清楚了："拖尾直接在玩家背后出现，直接从玩家摄像头穿过去" —— 渲染几何上
光束是从拖尾位置沿弹丸方向**向后**延伸，弹丸还在枪口时这条光束正好落在玩家自己的摄像头上，
所以横穿屏幕。以前看不到是因为 §82.12 之前的重复注册让拖尾每刻走两格，开火第一帧光束就被甩到 10 格外。

- **修法**：新增客户端配置 `display.bulletTrailRenderDelay`（范围 0–40 tick），
  `renderBulletTrail` 在 `age < 延迟` 时直接不画（`age` = 收到开火封包后的客户端刻数）。
  **默认值由玩家实测选定**：首版取 10，玩家试后给出"拖尾延迟渲染 1 tick 最合适" ⇒ **默认 1**
  （只盖住开火那一帧，即光束压在自己摄像头上的那一下）。
- **边界**：两把枪的拖尾寿命短于 10（`inquisitor` life 8、`spitfire` life 10），硬套会把拖尾整个抹掉 ⇒
  `renderDelay() = min(配置值, max(0, maxAge - 4))`，保证至少 4 个可见刻（为此给 `BulletTrail` 加 `getMaxAge()`）。
- **炮塔拖尾不加延迟**：摄像头不在炮塔枪口上，不存在穿摄像头问题。延迟值每帧只读一次。
- **验收**：**31 个审计全 0** / `verify_installed_jar` **197/197**（本节新增 4 条）/ `javac` 0 / `build` ✓ /
  已安装（19413256 字节，备份 `.bak-204155`）。**玩家实测确认延迟 1 tick 最合适**（已设为默认，改动即重新决策）。


## §82.14 警卫 AI：套用"之前的 1.21.1 移植版"（玩家指定；客户端持枪动画不做）

参照版 `E:\mod\SG2-1.21\ScorchedGunsNeoforge-main` 的警卫兼容共 7 个文件，逐条对齐后**只搬行为、不换 AI 类**：

- **移植**：① 持枪时取消 `Guard.performRangedAttack`（否则 GV 的弩射击逻辑对着手里的枪跑）；
  ② `Mob.canReplaceCurrentItem` 允许枪替换非枪（**收窄到警卫**，参照版改的是全游戏所有 mob）；
  ③ 持枪也能踹（**按 GV 原逻辑重写**，见下）。
- **不换 AI 类**：它那套独立 `GuardGunAttackGoal` 里 `setFlags(MOVE,LOOK)`、每颗弹丸 `projectile.tick()`、
  射速 `rate/50` —— 正好是 §82.7 / §82.11 / §82.6 刚修掉的三条。AI 仍用本体 `GunAttackGoal` + `MobGunFire`。
- **不照搬 `registerGoals` 注入**：我们的目标构造时要拿枪的物品栈（取这把枪的 idealRange），照搬会在
  警卫还没枪时构造 ⇒ 射程钉死默认值且 `hasGunAttackGoal` 之后拒绝装正确的那个。改为在
  `reassessWeaponGoal` 里给警卫分流：用真枪构造 `GuardGunAttackGoal`，并用 `resetFollowRange`
  而非 `extendFollowRange`（否则被命令发枪的警卫会拿 64 格追击范围跑出村）。
- **踹击的证据与修正**：反编译 `Guard$KickGoal.canUse` 的实际条件是
  `target != null && distanceTo <= 2.5 && Item.useOnRelease == true && !isBlocking && kickCoolDown == 0`；
  其中 `useOnRelease` 只对**弩/三叉戟**为 true ⇒ 持枪警卫永远踹不了（所以参照版那个 mixin 必要），
  但参照版把整个判断替换成 `!isBlocking()` ⇒ 丢掉 `kickCoolDown` ⇒ 每刻都能踹。我们只放宽"手里是弩"这一条，
  其余四条逐条保留；冷却字段读不到时不动，把决定权还给 GV。
- **客户端持枪/后坐动画不做**（玩家："那个动画是有问题的"）。
- **顺手补掉审计缺口**：`audit_mixins.py` 取方法名用 `(\w+)`，而本项目注入方法名是 `scguns$Foo`（`\w` 不匹配 `$`）
  ⇒ "回调参数匹配"检查**从未真正生效**；已改 `[\w$]+`，并新增"外部目标 mixin 的注入签名不得出现实体类型"规则
  （§82.10 教训的推广），带反向验证（种入 `LivingEntity` 参数 ⇒ 报 BROKEN 且退出 1）。
- **坑**：反向验证用 PowerShell `WriteAllText(..., UTF8)` 还原文件会**写 BOM** ⇒ 编译 `illegal character: '\ufeff'`，
  且 install 在 build 失败后仍执行 ⇒ verify 拿旧 jar 报 FAIL，看起来像功能没生效。已剥离 BOM 并全仓确认无残留。
- **门禁**：**31 个审计全 0** / `verify_installed_jar` **204/204**（新增 7 条）/ `javac` 0 / `build` ✓ /
  已安装（19417719 字节，备份 `.bak-205020`）。
- **未实测（交给玩家）**：① 持枪警卫不再有弩的射击/装填动作；② 贴脸 ≤2.5 格会踹且有节奏；
  ③ 枪不会被 GV 装备换走；④ 命令发枪时用枪的射程打、不跑出村。

## §82.15 玩家推翻 §82.9：**独立警卫 AI 要移植**（"scgun 自带的枪手 ai 智商很低"）

"智商低"不是代码笨，而是**它不许动**：§82.9 把警卫切到本体 `GunAttackGoal` 并按 §82.7 去掉了 `MOVE|LOOK`
旗标，而那套 AI 的接近/后撤全靠 `getNavigation()` —— 不占旗标时 GV 自己的巡逻/回村/闲逛目标与它**共用**
navigation 并抢走移动，于是警卫该退不退、该压不进，站着随便开枪。要自己走位的战斗 AI 必须占住移动
（香草 `MeleeAttackGoal` 亦然，GV 的近战目标就是 extends 它），所以本轮**有意恢复 `MOVE|LOOK`**，
但只在"有目标且手里是枪"时占用，目标一没立刻交还 GV 的目标。

- **移植的决策逻辑**：卡**这把枪自己的** `idealAttackRange`（参照版写死 15 格，我们改成按枪）、
  6 格内 `LandRandomPos` 后撤、`seeTime >= 5` 才开火、**用我们自己的 `isFriendlyShot`** 判断射线上有友军
  （参照版硬编码村民+铁傀儡，会与投射物闸门不一致）⇒ 不开火 + 侧移 + 压 10 刻、难度决定出枪门限
  `clamp(9-difficulty,5,8)`、弹匣空按 `reloadTimer` 上弹（有上弹/上膛音；不留就等于"打空一梭子站着"= §81）。
- **仍然坚决不搬**：`projectile.tick()` 逐颗内联（§82.11 卡顿）、射速 `rate/50`（§82.6）、自己扣弹/放音效（§82.8）
  —— 全走 `MobGunFire.fire`/`fireInterval`，审计明令禁止这两条。
- **审计随决策改**：原先三条规则（不得 setFlags、不得 navigation、必须 extends `GunAttackGoal`）编码的是
  §82.7/82.9 的结论 ⇒ 改为新不变量（必须占 MOVE 且自己 navigation、必须是独立 Goal 且不得再 extends 枪手 AI、
  必须走共用管线/读枪射程/会换弹/查友军、禁止内联 tick 与 `/50`）；`verify_installed_jar` 那条"子类"检查换成 5 条行为检查。
  既有约束保持：警卫不得 `extendFollowRange`、持枪时近战仍被压制。
- **门禁**：**31 个审计全 0** / `verify_installed_jar` **208/208** / `javac` 0 / `build` ✓ /
  已安装（19421712 字节，备份 `.bak-205454`）。
- **未实测（交给玩家）**：① 会走位（压射程、被贴脸会退）；② 打空会上弹；③ 村民挡线不开火、绕开再打；
  ④ 拿枪的警卫不追出村；⑤ 没目标时正常巡逻/回村。

## §82.16 玩家报"拿走警卫的枪会再刷一把"（已修）

根因是 §82 那条补枪钩子开得太宽：`onLivingEquipmentChange` 里只要主手变成**非枪**就补枪——它本意是应付
"GV 在警卫 join 之后才把自己的剑/弩装上去"（探针：join 时是枪、20 tick 后变铁剑），但"玩家把枪拿走"在事件
层面完全一样（只是变成了**空**）⇒ 等于无限发枪机。

- **修法两道闸门**：① 事件里只在**替换物非空**时才补（`event.getTo().isEmpty()` 跳过）——空手=枪被拿走；
  ② 兼容类里加 `GUARD_REARM_WINDOW_TICKS = 100`（约 5 秒，依据是实测 GV 装备约 20 tick 落地），
  只在生成窗口内、且 `isAlive()` 时才补。补枪路径于是只剩生成期的三条（join / `tickCount < 2` / 装备变化）。
- **语义**：枪是"生成时自带"的东西；拿走就是拿走；被 GV 自己覆盖时仍会补回来；生成很久后被动装备则保持玩家留下的状态。
- **防复发**：`audit_guard_compat.py` 新增三条（装备钩子必须检查替换物为空；`rearmReplacedGuardGun` 必须含 `tickCount` 窗口；
  join 钩子必须含 `loadedFromDisk()`）；`verify_installed_jar` 新增 3 条 ⇒ **211/211**；**31 个审计全 0**；
  已安装（19421950 字节，备份 `.bak-210304`）。
- **同源的第三个洞（一起修）**：join 事件在**区块重载**时也会触发（新实例 ⇒ 掷骰记录丢失）⇒ 拿走枪、走远、回来
  可能又凭空多一把。`onEntityJoinWorld` 加了 `event.loadedFromDisk()` 判断：只有真正新生成才发枪；重载只保留必要的一件事——手里是枪就 `reassessWeaponGoal`（AI 目标不存盘，重载必须重装）。

## §82.17 警卫持枪动画：试过、已按玩家决定回退

先按玩家提议做了一版：把玩家姿势入口 `IHeldAnimation.applyPlayerModelRotation` 从 `Player` 放宽到 `LivingEntity`、
给 GV 的 `GuardModel.setupAnim(Guard, ...)` 加客户端 mixin、并把身体侧转抽成共享方法。玩家进游戏实测后判定
**观感不理想** ⇒ **全部回退**：姿势入口恢复 `Player`、mixin 删除、`MixinPlugin` 的客户端前缀与
`scguns.mixins.json` 的 client 条目撤销、共享侧转方法与相关审计/校验规则一并撤掉。
只保留与动画**无关**的审计改进（`audit_mixin_plugin_gate` 逐个前缀检查 + 把 `isModLoaded(...)` 的 mod id 纳入判据）。

**留下的结论（要再做就从这里开始）**：

- 参照版那套姿势为什么怪（玩家的诊断，已用代码证实）：玩家的侧身来自姿势数据自己的 `renderYawOffset`
  （双手 25°、火箭筒 35°、机枪 45°），由 `WeaponPose.applyPlayerPreRender` 写进 `yBodyRot/yBodyRotO`；
  参照版的手写姿势只摆手臂、**从不碰 `yBodyRot`** ⇒ 躯干朝走路方向、手臂按"已侧身"的躯干摆。
  那 9 个姿势类里各有一份硬编码同值写法，属于 `Config.CLIENT.display.oldAnimations` 的遗留分支
  （默认路径走 `super` 的数据驱动值）——**改动画前先看那个开关**。
- 通用 mob 姿势对警卫从来不起作用：`MixinHumanoidModel` 注入 `HumanoidModel.setupAnim(LivingEntity, ...)`，
  而 GV 的 `GuardModel` **自己覆写了 `setupAnim(Guard, ...)`** ⇒ 覆写优先 ⇒ 任何方案都必须直接注入 GV 的 `GuardModel`。
- 枪在手里的**物品摆放**对 mob 保持原版是**有意**的（`ItemInHandLayerMixin` 记着上一版手工摆枪
  "看起来不像在持枪"、已删）。

**门禁**：31 个审计全 0 / `verify_installed_jar` 211/211 / `javac` 0 / `build` ✓。
警卫侧保留的是 §82.14 起那套：独立警卫 AI、装备规则、踹击、防友伤闸门 —— 即
"**AI 用移植版那套，客户端持枪动画不做**"。

## §82.19 玩家报"持枪警卫的移动速度过快"（已修）

**根因**：§82.15 移植的独立警卫 AI 自己负责走位，而它沿用了参照版的速度常数 `1.0`（接近）/`1.2`（后撤）——
`moveTo(target, speed)` 里那是**移动速度属性的倍率**，1.0 就是满速。而 GV 自己走路用的是 **0.6 / 0.65**
（反编译 `Guard$GuardMeleeGoal` 的常量）⇒ 持枪警卫比拿剑的警卫快约 1.7 倍。这也解释了为什么是 §82.15 引入的：
在那之前枪手目标不占旗标，实际移动由 GV 的目标驱动。顺带排除：本 mod 从没给警卫加过移速属性修正，
所以不是残留的永久修饰符。

- **修法**：`APPROACH_SPEED = 0.6`、`RETREAT_SPEED = 0.65`（都用 GV 自己的数字），并新增
  `common.compat.guard_gun_move_speed`（默认 1.0，范围 0.25–2.0）作为倍率，玩家可自行微调。
- **审计**：新增两条（接近速度必须是 0.6、移动必须受该配置缩放）。**踩到子串陷阱**：第一版写成 `"0.6" not in goal`，
  被 `0.65` 命中 ⇒ 反向验证（放回 1.0）没触发；改成精确匹配 `APPROACH_SPEED\s*=\s*0\.6` 后双向生效。
  教训：数值规则别用"包含"判断。
- **门禁**：31 个审计全 0 / `verify_installed_jar` **213/213** / `javac` 0 / `build` ✓ /
  已安装（19422432 字节，备份 `.bak-213840`）。
- **未实测（交给玩家）**：① 持枪警卫步速应与拿剑的警卫一致、不再像冲刺；② 嫌慢/嫌快调 `guard_gun_move_speed`。
## §82.20 玩家报"区块重载时警卫物品仍被换成枪"（§82.16 没堵干净，已修）

**根因**：`Entity.tickCount` **不写进 NBT**（vanilla 只拿它算水晶音效，已核对源码）⇒ 区块重载后归零
⇒ `onLivingUpdate` 里那条"生成后最初两刻"的补枪路径（`tickCount < 2`）其实等于"**每次区块加载**"：
空手就塞一把、手里是剑就换掉。§82.16 的补枪窗口同样用 `tickCount` 量，所以也随重载重开。
**会存盘的是 `getPersistentData()`**（NeoForge 写进实体 NBT 的 `NeoForgeData`）。

- **修法**：掷骰结果改存 `getPersistentData()`（`ScgunsGunRolled` / `ScgunsGunArmed` / `ScgunsGunRolledAt`），
  删掉静态 `WeakHashMap`（换存档就丢，正是漏洞）；`equipGuardGun` 对已掷骰的警卫直接 no-op（枪=生成期属性）；
  `rearmReplacedGuardGun` 的窗口改与**存盘的掷骰时刻**比较；
  **删掉**每 tick 的补枪路径（tick 钩子无法区分"刚生成"和"刚加载"）。
- **审计**：新增三条并做反向验证（把 tick 路径放回去 ⇒ 报错退出 1）：掷骰必须在 `getPersistentData`、
  rearm 必须用 `getGameTime` 且不得出现 `tickCount`、每 tick 钩子不得调用 `equipGuardGun`。
- **门禁**：31 个审计全 0 / `verify_installed_jar` **215/215** / `javac` 0 / `build` ✓ /
  已安装（19422496 字节，备份 `.bak-132807`）。
- **顺带发现**：mods 里还有一个多余的 `scguns-0.5.5.2.jar`（同 mod id 两份不会加载），安装脚本已移走备份。
- **未实测（交给玩家）**：① 拿走枪后重载区块依旧没有枪；② 拿剑/空手的警卫重载后不会被换成枪；
  ③ 新警卫仍按 25% 持枪、生成后 5 秒内被 GV 覆盖会补回；④ 旧存档警卫下次加载只掷一次骰。

## §82.21 Create 联动配方：硫磺碎块在石磨磨不出硫磺粉（已修）

**澄清**：`item.scguns.sulfur_chunk` 的 zh_cn 名就是"硫磺块"（与方块 `sulfur_block` 同名），玩家说的"硫磺碎块"= 该物品；
配方 `create/sulfur_chunk_from_milling.json` 存在、随包、且**无解析错误** ⇒ 问题是**参数被静默忽略**。

**根因**：Create 6.0 用蛇形 `processing_time`/`heat_requirement`，移植版留的是驼峰 `processingTime`/`heatRequirement`，
未知键被忽略且不报错。`processing_time` 缺失 ⇒ 时长默认 0 ⇒ 石磨 `tick` 里 `if (timer <= 0) 跳过递减分支`，
而产出（`MillingRecipe.rollResults`）就在该分支内 ⇒ **永远不产出**（字节码级证据）。同理"超热"要求失效。
Create Addition 1.7.1 的 charging schema 也变了（`ingredients`/`results`/`max_charge_rate`），我们那 6 条直接解析失败。

**修法**：167 个 create 系配方改名（只动 `type` 为 `create:*` 的文件）；6 条 charging 按 1.7.1 schema 重写；
两条被 `create` 条件误卡的原版烧炼配方去掉条件。**新增审计 `tools/audit_create_recipes.py`**（第 32 个）：
非原版类型必须门禁在拥有该类型的 mod 上、禁止驼峰键、**键若在该 mod 自己的配方里从未出现即阻断**（静默忽略探测器）、
同类型差异仅作汇总提示（已核对 mixing 的 `results.chance` 合法，`BasinRecipe` 会 roll）。反向验证：种回 `processingTime` ⇒ 退出 1。

**门禁**：32 个审计全 0 / `verify_installed_jar` 217/217 / `javac` 0 / `build` ✓。
**顺带发现（未改，等决定）**：`block.scguns.sulfur` 缺 lang 键（显示原始键）；`sulfur_chunk` 与 `sulfur_block` 中文同名易混。

## §82.22 刺刀冲锋动画不播放：客户端读了只有服务端会写的静态字段（已修）

**现象**：服务器上刺刀冲锋"动画没播放"（位移本身是服务端执行的，所以人在冲，只有客户端那一半不动）。

**根因**：冲锋状态由服务端决定（`MeleeAttackHandler.startBanzai`，入口 `C2SMessageMeleeAttack`），写进 **`static` 的 `isBanzai`**；
客户端的 `GunRenderingHandler.updateMelee()` 与 `AnimatedGunItem`（约 409 行）**直接读这个静态**。静态字段不是同步过的状态：
单人时两边同一 JVM ⇒ 正常；服务器上客户端那份恒为 false ⇒ `banzaiProgress` 恒 0 ⇒ 冲锋动画一次都不播。
排除法：0.5.5 原始资源里根本没有 melee/bayonet 的 GeckoLib 动画（普通枪走姿势变换路径），且
`ClientMeleeAttackHandler`/`MeleeAttackHandler` 相对 0.5.5 没有丢调用 ⇒ 不是移植删掉的，是一直如此、只在服务器上暴露。

**修法**：`ModSyncedDataKeys.BANZAI`（`scguns:banzai`，Boolean/PLAYER/`resetOnDeath`）+ 在 `ScorchedGuns` 注册；
`MeleeAttackHandler.isBanzaiCharging(Player)` 读同步键，`startBanzai`/`stopBanzai` 写它；两个客户端读点改用它。
保留读 `isBanzaiActive()` 的三处服务端调用方（melee 包处理器、`GunEventBus`、`MeleeAttackHandler` 自身）。
动画本身未动（§82.17/§82.18 的持枪动画保持回退）。

**新增审计 `tools/audit_synced_state.py`**（第 33 个）：① 除服务端白名单外不得调用 `MeleeAttackHandler.isBanzaiActive()`；
② 客户端两个读点必须用 `isBanzaiCharging(`；③ `BANZAI` 必须定义**且必须注册**（漏注册 ⇒ 值永不发送 ⇒ 静默退回默认值）。
注释剥离改用状态机（第一版只剥 `//`，被 `ModSyncedDataKeys` 的 javadoc 里提到的 `isBanzaiActive()` 误报）。
反向验证三条各一次（改回静态读⇒2 条、改键名⇒1 条、注释掉注册⇒1 条），还原后 0。

**门禁**：33 个审计全 0 / `verify_installed_jar` **222/222**（新增 5 条）/ `javac` 0（1009 文件）/ `build` ✓ / 已安装 ✓。
**未实测（交给玩家）**：在**服务器**上用带刺刀的枪冲锋，应能看到冲锋动画（单人生效不算验证）。

**生命周期核对（`javap -c` 证据）**：`saveToFile()` 才落盘，构造默认 `save = false` 且我们没调用 ⇒ 重连不会卡姿势；
`resetOnDeath()` 的实现就是 `persistent = false`，而 `SyncedEntityData.onPlayerClone` 只在
`wasDeath == false || key.persistent()` 时搬运旧值 ⇒ 死亡**和切维度**都回默认 false；默认 `syncMode = ALL`
⇒ 变化发给自己 + 追踪者（别人也能看到你冲锋）。全局单槽 `isBanzai`/`banzaiPlayer` 使"同时只能一人冲锋"是原始设计限制，本次未改。

## §82.23 刺刀冲锋被"疾跑丢失"打断：维持条件自相矛盾（已修）

**报告**：玩家"刺刀冲锋可能被疾跑打断，但是刺刀冲锋就是依赖疾跑的"。**不是移植引入**：0.5.5 原文相同。

**根因①**：维持条件是 `!player.isSprinting() → stopBanzai()`。而原版 `LocalPlayer.aiStep` 会在
**撞到方块**（`horizontalCollision && !minorHorizontalCollision`）、**水里**、**饥饿 ≤ 6**（`hasEnoughFoodToStartSprinting`）
时丢掉疾跑标志；双点 W 疾跑的玩家还要 `sprintTriggerTime = 7` tick 后重新双点才能恢复。冲锋本来就靠撞东西打伤害 ⇒ 自己的玩法杀掉自己。

**根因②（决定性）**：0.5.5 的撞击分支本来有 `KnockbackGracePeriod`（撞墙被弹开后 5 tick 不检查疾跑），
但 50 ms 调度任务**先**做裸疾跑检查、**再**调 `handleBanzaiMode` ⇒ 宽限期**永远执行不到**（死代码）⇒ 撞到任何东西冲锋立刻结束。
同一路径还有任务泄漏：每次起冲新开一个 50 ms 任务且从不取消 ⇒ 冲 N 次就有 N 个任务一起跑 `handleBanzaiMode`。

**修法**：调度任务不再检查疾跑（只处理死亡/移除 + 调 `handleBanzaiMode`，**起手仍要求疾跑**）；
`handleBanzaiMode` 顺序改为**先撞击判定、再维持判定**；宽限期 5 → **10 tick**（> 击退滞空 ~6 + 原版 7 tick 重取疾跑）；
维持规则改为"**还在往前跑**"：疾跑中 ⇒ 继续；疾跑丢失但向前速度 > 0.1 ⇒ 宽限 **20 tick**；向前速度归零 ⇒ 立刻结束；
20 tick 内疾跑没回来（饥饿见底/水里）⇒ 结束（饥饿仍是天然上限）。速度按水平视线方向投影、水平分量先归一化（低头冲锋不会误判）。
顺带恢复 0.5.5 的常量写法（移植版在 `handleBanzaiMode` 里退化成字面量）与 `WALL_CHECK_DISTANCE`；任务改为起冲前 cancel 上一个 + 结束自我取消。

**新增审计 `tools/audit_banzai_charge.py`**（第 34 个）六条规则：调度任务不得再出现 `isSprinting`（起手条件必须在）、
`handleBanzaiMode` 必须用 `keepCharging(` 且不得有裸 `!isSprinting()`、**撞击判定必须在维持判定之前**、
`KNOCKBACK_GRACE_PERIOD_TICKS > 7`、tag 字面量只允许出现在常量声明处、任务必须被 cancel。反向验证 6/6 被抓。

**门禁**：34 个审计全 0 / `verify_installed_jar` **225/225**（新增 3 条）/ `javac` 0（1009 文件）/ `build` ✓ / 已安装 ✓。
**未实测（交给玩家）**：① 冲锋撞墙不该立刻断（应弹开继续）；② 饥饿耗到 6 以下约 1 秒后应断；③ 松开 W 立刻断；④ 连冲多次伤害节奏不变。

## §82.24 物品栏/JEI/EMI 枪械渲染：先核对"是不是被移植修好了"，再砍掉真存在的每帧开销

**玩家观察**："1.21.1 移植版里 EMI/JEI/物品栏的枪械渲染严重掉帧可能被修复了"。

**核对（结论：没有证据指向某处代码改动）**：GUI 路径 = `ItemRenderer.render` → 因模型 `builtin/entity`（`BuiltInModel.isCustomRenderer()` 恒真）
直接交给 BEWLR = GeckoLib `AnimatedGunRenderer`，不经过 `renderGun`（§10.9）。GeckoLib 的 GUI 快路径 `renderInGui` **4.8.4（1.20.1）与 4.9.3（1.21.1）逐条字节码结构一致**
（`javap -c` 对照：setupLightingForGuiRender → BufferSource → getTextureLocation → getFoilBufferDirect → pushPose → defaultRender(partialTick=0) → endBatch → enableDepthTest → popPose）
⇒ 不是 GeckoLib 的差异。0.5.5 与移植版在该路径的每帧工作也基本相同（`getModifiedGun` 的 WeakHashMap 缓存 0.5.5 就有；GUI 不画手臂的守卫 0.5.5 也有）
⇒ **不能声称"移植修好了它"**；最可能是环境差异（EMI 会动画渲染列表物品，而这套整合按记录只有 JEI）。要真下结论必须实测（探针提议见 HANDOFF 82.24.6）。

**顺手查出的真开销（本轮修掉）**：`AnimatedGunRenderer.renderByItem` 在 GUI 分支之前做了四件 GUI 不用的事（每可见枪械每帧）：
`ItemRenderer.getModel`（= ItemModelShaper + `ItemOverrides.resolve`）、玩家眼睛位置的世界光照查询（下一行就被 `pack(12,12)` 覆盖）、gun NBT 读取、`Objects.requireNonNull(client.player)`。
修法：设好三个字段后立即从 GUI 快路径 `renderGuiItem` 返回（只做 `super.renderByItem(..., LightTexture.pack(12,12), ...)`），第一人称用的 model/transform/localPlayer 挪进第一人称分支；行为不变。

**顺带修**：`AnimatedGunRenderer.renderByItem`、`GunItemStackRenderer.renderByItem`、`ExoSuitRenderer.renderRecursively` 三个真覆写缺 `@Override` ⇒ 补上（javac 通过即证明），
避免 §10.9.2 那类"签名一漂移就静默变死代码"。注意 `audit_override_drift.py` 看不到这一类（它只报签名漂移，完全匹配但缺注解会被跳过）。

**新增审计 `tools/audit_gui_render_cost.py`**（第 35 个）：① `renderByItem` 必须在碰 getModel/calculateBlockLight/getModifiedGun/NbtHelper.getTag/requireNonNull **之前**从 GUI 返回（按方法体内位置判定，注释状态机剥离）；
② GUI 分支必须仍画枪（`renderGuiItem` + `super.renderByItem` + `pack(12,12)`，是快路径不是跳过）；③ GeckoLib 渲染钩子必须带 `@Override`（mixin 豁免）。反向验证 4/4 被抓。

**门禁**：35 个审计全 0 / `verify_installed_jar` **226/226**（新增：class 里必须有 `renderGuiItem`）/ `javac` 0 / `build` ✓ / 已安装 ✓。
**未实测**：手感画面应完全不变，唯一可能被察觉的是帧率；如需数字，可加临时探针统计"每帧枪械 GUI 渲染次数/耗时"（等玩家发话）。

**更正**：最初推测"这套整合只有 JEI、1.20.1 那边才有 EMI"——**错了**：`mods/` 里 EMI 1.1.24 与 JEI 19.57 **都装着**。所以"环境差异"这个解释不成立，本节结论仍然是"没有证据指向某处代码改动"；82.24.3 的每帧浪费是客观存在、已修。

## §82.25 装 Better Combat / 铁魔法后：攻击完再切枪，手臂渲染错乱（已修）

**报告**（玩家指出是 scgun 本体老 bug）：装 BC 用其攻击模块攻击后再切枪 ⇒ 手臂渲染错乱；铁魔法可能同样。
现状：本实例**没装 BC**，但装了**铁魔法 3.16.3** 与其共用的动画库 **player-animation-lib 2.0.4** ⇒ 用铁魔法可立即验证。

**机制（`javap -c` 证据）**：Player Animator 的 `AnimationApplier.updatePart(ModelPart)` 写四类状态：
位置 `x/y/z`、旋转 `xRot/yRot/zRot`、**缩放 `xScale/yScale/zScale`**、经 bendylib 的**逐 cuboid bend**（`IBendHelper.bend(part, side, amount)`）。
我们的枪械手臂（`AnimatedGunRenderer.renderRightArm/renderLeftArm`）直接对**同一个共享 `PlayerModel`** 调 `ModelPart.render`，却只重置了位置+旋转
⇒ **缩放与 bend 泄漏**到枪的手臂上（拉伸/弯折 = 错乱）。第一人称下本地玩家模型不会重跑 `setupAnim` ⇒ 上次攻击动画的姿势无人清除，一直留到我们画手臂时。

**修法**：新增 `resetArmPose(part, bone)` = `part.resetPose()`（恢复烘焙姿势，含 scale=1）+ `setPos(pivot)` + `PlayerAnimatorCompat.resetBend(part)`；右臂/右袖/左臂/左袖四个部件全部走这条路。
`bend(part,0,0)` 即"清除"（`BendHelper.bend` 里 `|amount| < 1.0E-4` 走还原分支，库里没有单独 reset）。兼容隔离：新 `client/compat/PlayerAnimatorCompat`
先查 mod id `playeranimator`、反射只解析一次并缓存 `INSTANCE`/`Method`（每帧每臂都调用，不能每帧 `Class.forName`）、失败只记一次并停用；渲染器内不出现任何 `dev.kosmx` 类型（§82.10 教训）。第三人称未动（那里动画库的姿势本就该生效）。

**新增审计 `tools/audit_arm_render_reset.py`**（第 36 个）：四个部件必须 reset 后才 render；`resetArmPose` 必须含 `resetPose()`+`setPos(`+`resetBend(`；兼容类必须查 mod id、缓存反射 Method、渲染器不得出现 `dev.kosmx`。
反向验证 6/6 被抓——其中"bend 不做 mod 检查"**第一次漏报**（审计规则当时允许用 `unusable` 替代 `isLoaded()`），收紧后通过：**反向验证也在验审计规则本身**。

**门禁**：36 个审计全 0 / `verify_installed_jar` **228/228**（新增 2 条）/ `javac` 0（1010 文件）/ `build` ✓ / 已安装 ✓。
**未实测（交给玩家）**：① 铁魔法施法后切枪手臂应正常（现在就能测）；② 装 BC 后攻击再切枪；③ 无动画模组时手臂应与以前完全一致。
**若仍不对的下一步抓手**：Player Animator 对第一人称还有独立通路（`FirstPersonMode` + `firstPerson.ItemInHandRendererMixin` 会取消原版手部渲染并/或用第三人称模型画整个人），那是"动画播放期间枪不见/手臂重叠"的另一条通路，需 API 级 `setFirstPersonMode(DISABLED)` 处理，等确认现象再动。

## §82.26 第三次同类报告（资源包）：改为自己烘焙手臂模型（取代 §82.25 的做法）

**玩家补充 + 更正**：Fresh Move 这类是**资源包**（不是 mod）；资源包改实体模型/动画要靠 EMF/OptiFine 那套，而它改的模型/动画**同样绑在渲染器持有的那个 `PlayerModel` 实例**上 ⇒ 与 BC/铁魔法撞同一个实例。

**为什么 §82.25 的补丁原理上不可能完整**：它是"逐个动画系统把它写脏的状态擦干净"（位置/旋转/缩放 + 反射清 bend），但①每个动画系统有私有状态（资源包/EMF 的根本不知道）②只要它从 `ModelPart.render` 钩子写，改字段就擦不到 ③每加一个模组/资源包都要再查一遍实现——不是工程做法。
真正的问题是**借用**：`renderPlayerArms` 取的是 `getEntityRenderDispatcher().getRenderer(player).getModel()`，正是全游戏所有玩家动画都在动画化的那个实例；加上第一人称不重跑 `setupAnim` ⇒ 上次的姿势原封不动留在手臂上。

**修法**：新增 `armModel(AbstractClientPlayer)`，按皮肤选层并**与原版 `PlayerRenderer` 逐字一致**（`javap -c` 核对：`new PlayerModel<>(bakeLayer(slim ? PLAYER_SLIM : PLAYER), slim)`，变体读 `getSkin().model() == PlayerSkin.Model.SLIM`），两种变体各缓存一份懒烘焙；只有我们持有 ⇒ 外部无引用 ⇒ 谁也动画不到它（整类 bug 消失，而非修好某一个模组）。
资源包改的**几何**照旧生效（用 `bakeLayer`），只有**动画**到不了我们这里——正是想要的。**瘦皮肤必须仍变窄**，请务必实测。据此**删除** `PlayerAnimatorCompat` 与反射调用（§82.25 定向补丁被取代），`resetArmPose` 保留作防御。第三人称未动。

**审计更新**（仍是第 36 个 `tools/audit_arm_render_reset.py`，改为 4 条规则）：四部件必须 reset 后画；reset = 烘焙姿势 + 轴心；**手臂模型必须自己烘焙**（`bakeLayer(`/两个层/皮肤变体/`armModel(`，且 **不得**出现 `getEntityRenderDispatcher(` 与 `.getModel()`）；渲染器不得出现动画库类型。反向验证 **7/7** 被抓（含"退回借用渲染器模型""去掉 slim 变体/层"）。

**门禁**：36 个审计全 0 / `verify_installed_jar` **229/229**（新增 2 条，其中一条是**反向检查**：class 里不得有 `getEntityRenderDispatcher`）/ `javac` 0（1009 文件）/ `build` ✓ / 已安装 ✓。
**未实测（交给玩家）**：① 装该资源包（+EMF）后切枪手臂应正常；② BC 攻击后切枪；③ 铁魔法施法后切枪；④ **瘦皮肤手臂应仍细**；⑤ 无动画模组/资源包时与以前完全一致。
**仍未动的另一条通路**：动画库/资源包可能在动画**播放期间**自己画一套第一人称模型（`FirstPersonMode` 等）⇒ 可能出现两套手臂/枪不见，这是独立问题，需 API 级处理，等确认现象再动。

## §82.27 命中/爆头音效改动：试过、已按玩家决定全部回退

**做过**：给普通命中生物补声音（0.5.5 原本 `return null`=完全无声）、爆头改"闷响 + 确认音"两层、每类独立音效 id 与音量共 8 个新 config 键、每 tick 每类去重（霰弹枪一枪 26 个命中包）、并接入玩家提供的 `Hitmarker.ogg`（新资源 + sounds.json + `ModSounds.HITMARKER`）。
**回退原因**：玩家实测判定"没什么用"⇒ 全部撤销：`src/` 与 `src/main/resources/` 与 `e8d1306` **逐字节一致**；`tools/audit_hit_feedback.py` 删除、`verify_installed_jar` 那 7 条检查撤销、`Config` 8 个新键撤销、`sounds.json`/`ModSounds`/`.ogg` 撤销（`Hitmarker.ogg` 原文件仍在 `E:\音效素材\`，仓库里那份已删）。

**留下的事实（要再做时的起点）**：
1. 0.5.5 普通命中**本来就完全无声**（`getHitSound` 对非暴击非爆头 `return null`）；爆头音是 `entity.player.attack.knockback`（挥空声）；音量全写死 1.0。
2. **霰弹枪每颗弹丸一个命中包** ⇒ 任何"每次命中一个音/一次标记"的改动**必须按 tick 去重**，否则一枪 26 个 UI 音糊成噪声（通用结论，与音效无关）。
3. 命中包**只发给射手**（`sendToPlayer(() -> shooter, ...)`）⇒ 射手侧本地反馈安全。
4. **`scguns:item.ping.ping` 不能用**（玩家指出那是亿宏的换弹音效，借来的素材且听感是"换弹"）。
5. **`Hitmarker.ogg` 可用性已核实**（解析容器/识别头，非看扩展名）：Ogg Vorbis、单声道、48 kHz、0.106 s ⇒ 以后要用可直接复制进 `assets/scguns/sounds/` 并登记 `sounds.json`（写法见回退前的 `c66e5c1`）。
6. **审计规则自己也会写错**：本次 9 条反向对照中 2 条是规则写太松（只查音效名⇒开关改 false 也蒙混；只查 config 字段名⇒`define` 键改名发现不了）、1 条误报（被禁素材 id 写在我的注释字符串里）⇒ 反向验证同时在验规则。
7. "默认音效 id 必须真的存在"这条检查随回退撤掉了；以后再往 config 加音效 id 值得重新加回来（能挡住"拿别人素材当默认值"）。

## §82.28 命中音效真正的问题：被枪声掩蔽（玩家找到的原因）——按该原因重做

**玩家给的原因**："实际上爆头的音效被枪声盖过去了，所以听起来没有反馈"。⇒ §82.27 失败的原因清楚了：它只加声音/加音量键/加去重，**没有处理同刻响的枪声**——声音一直在播，只是**听不见**；瓶颈不在"有没有音效"，而在**可听性**。

**量化（两处测量）**：解析 45 个 `item/*/fire.ogg` 的 Ogg 页 granule ⇒ 枪声**中位 1.000 s**、平均 1.258 s、**卡宾 3.767 s**、火箭筒 4.920 s；
`GunModifierHelper.getFireSoundVolume` **从 1.0 起**（只有配件能改），射手那发是 `Attenuation.NONE` 本地播放 ⇒ **枪声就是 1.0**，旧命中音也是 1.0 ⇒ 响度不吃亏，吃的是**时长+频谱**：短点击被 1~4 秒宽带枪声**时间+频率掩蔽**。
⇒ "延迟"救不了（枪声太长），**主要手段是抬高音调**（把能量搬到枪声频段之上）+ **提高音量**（不能只是"相等"），延迟是次要（躲开起爆瞬态）。

**修法（只针对可听性，不重做反馈系统）**：① `handleProjectileHitEntity` 改为 `scheduleHitSound(...)`（命中包与开火同一 tick 就是问题本身）；
② 新增每 client tick 排空的队列（`pendingHitSounds` + `tickHitSounds()`，由既有 `ClientHandler.onClientTick` 驱动；记录 `dueTick`；**上限 8**；无 level 清空）；
③ 三个 config 键：`hitSoundDelayTicks` 默认 **3**、`hitSoundVolume` 默认 **2.0**（0–8）、`hitSoundPitch` 默认 **1.8**（0.5–2.0）+ 0.1 随机抖动；
④ 玩家给的 `scguns:hit.hitmarker`（0.106 s 短点击）作为 `headshotSound` 默认值，配置写错仍回落到原版音。
**没有**重新引入 §82.27 的"给普通命中补声音/每类独立音量/叠层"；普通命中保持 0.5.5 的无声。

**新增审计 `tools/audit_hit_masking.py`**（第 37 个）：命中音必须经 `scheduleHitSound` 排队且收包处不得直接 play；队列必须每 tick 排空（含 dueTick 比较、无 level 清空）且上限必须是小的具体数字；三个默认值必须越过失衡点（延迟≥1、音量≥1.5、音调≥1.5）；配置的命中音必须真的存在。
反向验证 **11/11** 被抓，过程两次踩坑：① 第一版对照脚本用 LF 而这三个文件在磁盘上是 **CRLF** ⇒ 4 条对照根本没生效（好在那版会打印 SETUP FAILED 才没默默放过）⇒ 改成不含换行的模式；② "队列上限"规则只查字段名 ⇒ 值改成 `Integer.MAX_VALUE` 也能过 ⇒ 改为解析数值并要求 ≤ 64。**规则比代码更容易写松。**

**门禁**：37 个审计全 0 / `verify_installed_jar` **236/236**（新增 7 条）/ `javac` 0（1009 文件）/ `build` ✓ / 已安装 ✓。
**未实测（交给玩家）**：① 爆头点击应能听出来；② 仍偏弱 ⇒ 先加 `hitSoundPitch`（对付频率掩蔽最有效）；③ 延迟明显 ⇒ 降 `hitSoundDelayTicks` 到 1–2（0=立即=回到被掩蔽）；④ 更响 ⇒ `hitSoundVolume`（0–8）；⑤ `headshotSound` 可换回原版音。

## §82.29 枪械等级解锁提示：去掉"袭击"字样 + 等级列表漏掉本级（玩家报的文案 bug）

**玩家报告**：解锁【边疆】时第二行写"现在可能会出现【古典】的敌人和袭击！"；随后明确要求"**袭击字眼应该去掉**"（`你获得了【铁】的枪械！现在可能会出现【铜、边疆、古典】的敌人和袭击！`）。

**根因（一行文案 + 一个玩法 bug，同一原因）**：`GunTier.getAvailableMobTiers()` 只返回 `previousTierIds`、**不含自己** ⇒ 解�锁【边疆】（previous=[antique]）时列表=【古典】；
更严重的是 `GunnerMobSpawner.equipProgressionGun` **用的就是这个列表** ⇒ **边疆枪手永远不可能装备边疆枪械**（最新一级永久不可达）。
旁证：该方法的权重是"前段常见/尾段罕见"，设计上最新一级本应是稀有尾段。
"袭击"错在突袭并非按枪械等级绑定（`raid_level` 绑定、且有自己那行），"【铜】的袭击"不存在。
第三行还会重复：`getRaidsForLevel` 是"≤该等级全部"，而边疆与古典 `raidLevel` 都是 1 ⇒ 把古典那次已公布的两条又列一遍。

**修法**：① `getAvailableMobTiers()` 补上自己（`level>0` 守卫，`none` 不入列），**放最后**以保持"最新=稀有"的权重；② 新增 `getAvailableMobTiersNewestFirst()`（按等级降序）**专供文案**，不依赖 GunTiers 书写顺序；
③ 键 `enemies_and_raids_can_spawn` 改名 `enemies_can_spawn`（名字不再承诺突袭），EN `[%s] enemies may now appear!` / ZH `现在可能会出现【%s】的敌人！`（**去掉"和袭击"**），两语言文件逐行替换同步（§40.3 教训：不 re-serialize）；
④ `sendRaidUnlockedMessage` 只报本级新增（下界＝上一级们的最高 `raidLevel`），无新增则整行不出现。

**修复后输出（脚本按真实表+语言文件模拟）**：边疆 → `你获得了【边疆】的枪械！/ 现在可能会出现【边疆、古典】的敌人！/（无新突袭，该行不出现）`；联邦 → `【联邦、锈岭、边疆、古典】的敌人！/ 联邦突袭、海洋突袭、扫荡者突袭`；锈岭 → `【锈岭、边疆、古典】的敌人！/ 锈岭突袭`。
（模拟脚本已提升为常驻工具 `tools/show_progression_messages.py`，改文案时不用进游戏即可核对。）

**等级名/突袭名对齐阵营（玩家后续要求）**：`gun_tier.scguns.copper` 铜 → **锈岭**、`iron` 铁 → **联邦**；并把 `raid.scguns.copper` 铜突袭 → **锈岭突袭**、`raid.scguns.iron` 铁突袭 → **联邦突袭**（否则同一段文字里"【锈岭】的敌人 … 铜突袭"两名并存）。
依据是原本就存在的矛盾：`raid.scguns.copper.name` = 召唤**锈岭**集体、`raid.scguns.iron.name` = 召唤**联邦**军备联合会 ⇒ 阵营名一直是锈岭/联邦，只有等级名与突袭标题没跟上。只改 zh_cn、逐行替换（不 re-serialize），键仍 1825/1825 对齐；这几个键同时被解锁消息与 `/scguns … progression` 指令使用 ⇒ 两处一起改名。**en_us 未动**（仍是 Copper/Iron/Copper Raid，英文那边有同样的不一致，需要英文用词再改）。
（一次自造的错误：第一版改名脚本键名拼接写错导致 JSON 损坏 ⇒ `git checkout` 还原后改成整对 `"键": "值"` 替换 + 唯一性断言。）

**新增审计 `tools/audit_progression_messages.py`**（第 38 个）：列表必须含本级且有 `level>0` 守卫；文案必须用 newest-first 且该方法按等级降序；**敌人句子的键名与文本都不许出现 raid/袭击**；突袭行必须用上一级最高 raidLevel 做下界且无新增时跳过。
反向验证 **8/8** 被抓；一次是规则自己写松（只查 `previousRaidLevel` 名字出现 ⇒ 赋常量 `-999` 也能过，那等于"全部突袭都算新"）⇒ 改为必须匹配 `Math.max(..., getRaidLevel())`。**同类教训第三次。**

**门禁**：38 个审计全 0 / `verify_installed_jar` **239/239**（新增 3 条）/ `javac` 0（1009 文件）/ `build` ✓ / 已安装 ✓。
**未实测（交给玩家）**：① 再解锁一级看第二行（新等级在最前、无"袭击"）；② **行为改动**：枪手现在真会拿最新等级枪械（以前永远不会）——若希望"敌人不要立刻跟上"，一句话即可改回只到上一级；③ 突袭行只在真有新增时出现。

## §82.30 下界硫磺矿石"破坏掉本体"：掉落表根本没加载（1.20.1 字段名 + 缺 type）

**根因**：`loot_table/blocks/nether_sulfur_ore.json` 存在且是合法 JSON，但附魔谓词写的是 1.20.1 的 **`"enchantment"`（单数）**，1.21 正确字段是 **`"enchantments"`**（对照原版 `diamond_ore.json`）。MC 用严格 codec 解析 ⇒ 一个未知字段使**整张表解析失败** ⇒ 表等于不存在 ⇒ 方块退回"掉自己"。
同一错误在 **28 文件 / 29 处谓词**（三种硫磺矿、晶碳矿、硝石玻璃、富磷矿、补给箱、喷口…）；**10 张 Boss 表**用了 1.20.1 的 `"treasure": true`（1.21 为 `"options": "#minecraft:on_random_loot"`）。
**反向提醒**：`"enchantment"`（单数）在 `apply_bonus`/`enchanted_count_increase` 里是**正确的**（原版数据 + 函数 codec 字节码 `fieldOf("enchantment")` 双证）⇒ 整词批量替换会改坏 25 处 fortune、18 处 looting ⇒ 必须结构化判断。
**顺带查出（玩家还没遇到）**：**11 张箱子掉落表没有顶层 `type`**（原版箱子表都是 `"type": "minecraft:chest"`）⇒ 也从未加载 ⇒ 被 `loot_modifiers/add_loot_*.json` 注入原版箱子的枪械战利品一直是空的。**0.5.5 原版同样缺** ⇒ 本体 bug，非移植引入。

**修法**（逐行替换、保留格式、改完全量重解析）：29 处谓词 → `enchantments`（按文件断言"文本次数 == 结构化次数"）；10 处 `treasure` → `options: "#minecraft:on_random_loot"`；11 张箱子表补 `"type": "minecraft:chest"`。**`bonusMultiplier` 保持原样**——原版 `deepslate_redstone_ore.json` 证明它在 1.21.1 仍正确（我先怀疑错了，靠原版数据挡住）。

**新增审计 `tools/audit_loot_tables.py`**（第 39 个）：合法 JSON；附魔谓词不得用单数（结构化）；**mod 用到的每个键必须出现在原版 94 键词表里**（不需记字段名，改名/删除都会被抓住，`treasure` 正是这样被抓的）；引用的 `scguns:` 物品必须已注册；必须有顶层 `type` 且取值原版用过。
反向验证 **7/7**（含"把谓词改回单数"= 玩家报的 bug、真弄坏 JSON）。过程中两次自己写偏：`register("id")` 漏了 `registerBurnable(`（3 个误报）、`type` 用首个正则匹配抓到池内 `minecraft:item`（10 个误报）⇒ **规则必须用解析后的结构**。

**门禁**：39 个审计全 0 / `verify_installed_jar` **243/243**（新增 4 条，其中两条是**全树反向检查**：随包掉落表不得再有单数谓词、不得再有 `treasure`）/ `javac` 0（1009 文件）/ `build` ✓ / 已安装 ✓。
**未实测（交给玩家）**：① 下界硫磺矿石应掉 2–3 硫磺碎块（精准采集才掉本体）；② 其余 28 张表同样恢复；③ **地牢/要塞/远古城市/埋藏宝藏/末地城/矿井等箱子应开始出现枪械战利品**（以前一直为空）；④ 突袭 Boss 掉落附魔装备不再整表失效。

## §82.31 枪手生物的枪"满耐久"（按玩家纠正缩小范围）+ 修掉审计工具的静默漏报隐患

**玩家两次报告**：先"枪手生物手持的枪械都是满耐久的"，随后纠正：**"刷怪蛋放出来的**枪手生物枪械是满耐久的，**其他的貌似是有的"** ⇒ 我原本的"给所有生物枪加磨损"是过度推广（会覆盖已正确的路径），按纠正缩小。

**两条装配路径（解释差异）**：**JSON 路径**（`data/scguns/entity/equipment/*.json`）给 mod 自己的生物（adjudicator/blunderer/cog_knight…）配枪并写了 `min_durability 0.2`/`max_durability 0.6`，`EntityEquipmentConfig` 会按比例扣耐久 ⇒ 这些生物的枪是旧的（玩家说"其他的有的"）。**代码路径**（主题枪手/进度枪手/警卫三条都走 `GunnerMobSpawner.createModifiedGun`）只填 `AmmoCount`、从不设耐久 ⇒ 刷怪蛋放出来的枪手是这条。另一证据：**生物开火不磨损枪械**（`MobGunFire`/`GunAttackGoal` 里没有 `hurtAndBreak`）⇒ 代码路径的枪永远不会变旧。

**修法**（只动代码路径）：新增 `MobGunDurability.roll`，按 `Config.COMMON.gunnerMobs.mobGunMinDurability`/`mobGunMaxDurability`（默认 **0.2/0.6**，与 `entity/equipment/*.json` 同一套数值）随机剩余耐久比例，`Mth.clamp` 上限、不可损坏物品跳过、min/max 写反也容错。调用点只有一处：`GunnerMobSpawner.createModifiedGun`（正是三条路径共用的那份）。**JSON 路径故意不调用**（那些文件自带范围，再滚一次会覆盖）。

**顺带修掉审计工具的静默漏报**：新审计初版报"Config 里没有该配置项"，而它就在文件里——因为我这次写的配置注释含 `data/scguns/entity/equipment/*.json`，这个 `/*` 在**字符串字面量**里，而剥离器不认识字符串 ⇒ 当成块注释开头 ⇒ 删掉其后代码 ⇒ 假阴性。全仓排查后：6 个审计的状态机剥离器 + 3 个用正则剥离器的（`audit_client_config_side`/`audit_guard_compat`/`audit_raid_cooldown`，危险形态是"字符串里的 `/*` + 后面某处的 `*/`"）全部替换为认识字符串的版本；替换后 40 个审计仍全 0（说明当前恰好没被抓到，但隐患消除）。**假阴性比报错危险得多。**

**新增审计 `tools/audit_mob_gun_durability.py`**（第 40 个）：两个耐久比例必须是 config 且默认值必须 < 1.0（默认 1.0 = 复现玩家报的 bug）；`roll` 必须跳过不可损坏物品、必须**读两个键**、必须 clamp；代码路径必须调用它；JSON 路径不得调用。反向验证 **6/6**（含一条规则自己写松后的收紧：只查 `Config.COMMON` 出现过 ⇒ 一端写死也能过 ⇒ 改成两键都必须在 roll 里）。**同类教训第四次。**

**门禁**：40 个审计全 0 / `verify_installed_jar` **247/247**（新增 4 条）/ `javac` 0（1010 文件）/ `build` ✓ / 已安装 ✓。
**未实测（交给玩家）**：① 刷怪蛋放出的枪手枪应是旧的（20%–60% 剩余耐久）；② mod 自己的生物保持原样（JSON 的 0.2–0.6，无叠加）；③ 调 `mobGunMinDurability`/`mobGunMaxDurability`（1.0/1.0 = 回到满耐久）。
**没动、等发话**：突袭 Boss/随从武器走 `RaidManager.createModifiedGun`（同样无耐久）；本轮按住报告只改了枪手路径，若要 Boss 的枪也是旧的，一处调用即可。

## §82.32 刺刀冲锋有了自己的配置区（玩家要求：机制改动放进这个配置项）

把它所有写死的参数搬成 `Config.COMMON.bayonetCharge` 下的选项并在代码里改为读它（否则旋钮是摆设）：`enabled`（关掉后近战键=普通捅击）、`requireSprintToStart`、`damageRadius`/`hitRadius`、`damageIntervalTicks`、`damageScalingLevel1/2/3`（默认由 3.0/5.5/7.0 改为 **0.0**，见 §82.33）、`knockbackGraceTicks`、`sprintLossToleranceTicks`、`minimumForwardSpeed`、`wallImpactEnabled`/`wallImpactCooldownTicks`、`wallCheckDistance`/`wallCheckSpreadDegrees`（30 度复现原来的 0/±10/±20/±30 射线）、`execute*`、`knockPlayerBackOnHit`、`endChargeOnHit`。只保留 3 个存档 tag 名为常量（不是可调项）。
顺带修好两条**过时**门禁检查：`verify_installed_jar` 原本在 `MeleeAttackHandler.class` 里找两个已搬家的常量，改指向 `Config$BayonetCharge`（**门禁的字段名检查必须跟着搬家**）。

## §82.33 刺刀冲锋机制重做：单体 + 枪械当前近战伤害 + 命中弹开玩家 + 残血敌对生物处决（玩家指定）

**玩家指定**：改为单体伤害；刺到敌人造成枪械当前近战伤害并**把玩家弹开**；刺到的若是**敌对**生物且血量 < 15 则**直接处决**。参考 1.21.11+ 长矛；玩家把 `spear-backport-neoforge-1.8.0` 放进实例后，用它作**权威参照**反查了原版机制（`KineticWeapon` 的 `hitboxMargin`/`contactCooldownTicks`/`delayTicks`/`forwardMovement`/`damageMultiplier`、`PiercingWeapon.stab`、`AttackRange`、`SpearUser` 的穿刺冷却与计数）。

**实现**：① 单体——`handleBanzaiMode` 伤害分支改为 `findChargeTarget`（正前方、`damageRadius` 内、`hitRadius` 触及内最近一个）＋ `stabWithBayonet`，删除 `performMeleeAttackOnTarget` 的 AoE 分支；② 伤害＝`meleeDamageOf(...)`，与普通捅击**同一算法**（攻击力+配件附加+刺刀附魔+枪械近战伤害），速度倍率默认 0.0；③ 命中用 `knockPlayerBack` 把玩家弹开（`knockPlayerBackOnHit` 默认开）；④ 处决＝`executeEnabled && instanceof Enemy && health < 15`，用 `hurt(playerAttack, Float.MAX_VALUE)` 而**非 `kill()`**（保住战利品/经验/击杀归属）；⑤ `endChargeOnHit`（默认结束）⇒ 一次冲锋一次突刺。

**审计**（`audit_banzai_charge.py` 扩到"机制+旋钮"两层）：保留原规则（ticker 不得查疾跑、撞墙判定在维持判定之前、tag 只能常量、`knockbackGraceTicks>7`、`0<minimumForwardSpeed<0.2`）；新增"必须 stabWithBayonet 且不得再出现旧区域攻击/遍历目标""19 个选项每个都必须被读到""速度倍率默认必须 0.0""处决必须查 `instanceof Enemy` 且走两个配置""处决必须是压倒性伤害""弹开与结束冲锋必须在开关后面"。规则作用域修过一次（初版把处决检查查在 `stabWithBayonet` 里，而逻辑在 `isExecutionTarget`）⇒ **规则要查逻辑真正所在的方法**。反向验证 **9/9**。

**门禁**：40 个审计全 0 / `verify_installed_jar` **256/256**（新增 4 条 + 修好 2 条）/ `javac` 0（1010 文件）/ `build` ✓ / 已安装 ✓。
**未实测（交给玩家）**：① 撞到敌人只伤那一个；② 伤害＝同枪普通捅击；③ 命中被弹开；④ 敌对血量<15 一刀处决（非敌对不会被处决，掉落/经验/击杀归属仍是玩家的）；⑤ 一次冲锋只刺一次（要连刺关 `endChargeOnHit`）。
**还没做（等你点名）**：长矛的蓄力/前冲位移（`KineticWeapon.forwardMovement`）、穿刺冷却（`SpearUser.isInPiercingCooldown`）、下马、触及 min/max、以及"按住蓄力"的输入模型（现在是按键切换）——都能直接加进 `bayonet_charge`。

## §82.34 两套机制并存：原区域冲锋是默认，单体突刺是选项（玩家要求）

**玩家要求**："把这个做成一个可选项，默认关闭，原先的刺刀冲锋机制留着"⇒ 从"替换"改为"**并存 + 开关**"：
- 默认 `singleTargetStab = false` = **原机制**：每次判定对 `hitRadius` 内所有目标造成伤害，伤害按**玩家速度**缩放（`damageScalingLevel1/2/3` 本轮已从 0.0 **恢复为 3.0/5.5/7.0**，因为那是原机制的一部分）；无弹开、无处决、命中不结束冲锋。
- 开启 `singleTargetStab = true` = §82.33 的单体突刺：单体、枪械当前近战伤害（**不乘速度**）、命中弹开玩家、敌对血量<15 处决、命中结束冲锋。

**实现**：`handleBanzaiMode` 伤害分支按 `isSingleTargetStab()` 分岔 ⇒ `stabWithBayonet(...)` 或 **`areaSweep(...)`**（原逻辑原样搬进：区域扫描＋速度倍率＋每目标粒子/击退/附魔，不含弹开与处决）；突刺那条不再调用 `getBanzaiDamageMultiplier`。配置注释写明两种模式各做什么，且 `execute*`/`knockPlayerBackOnHit`/`endChargeOnHit` 都标注"只在 `singleTargetStab` 模式下生效"。

**审计同步**（`audit_banzai_charge.py`）：`singleTargetStab` 默认必须是 false；`handleBanzaiMode` 必须同时引用开关、`stabWithBayonet`、`areaSweep`（两条路都要在）；`areaSweep` 必须保留速度倍率；`stabWithBayonet` 不得有速度倍率；`damageScalingLevel*` 默认必须 >0（把上一节"必须 0.0"的规则**反向**过来——规则跟着机制走）；`handleBanzaiMode` 自身不得遍历目标（循环只能待在 `areaSweep`）。反向验证 **7/7**。

**门禁**：40 个审计全 0 / `verify_installed_jar` **259/259**（新增 3 条）/ `javac` 0（1010 文件）/ `build` ✓ / 已安装 ✓。
**未实测（交给玩家）**：① 默认状态下手感应与改之前完全一致（区域伤害＋速度倍率；只保留 §82.23 的疾跑丢失容错，那是修 bug 不是机制）；② 打开 `singleTargetStab` ⇒ 单体突刺；③ 打开后 `execute*`/`knockPlayerBackOnHit`/`endChargeOnHit` 才生效。

## §82.35 血液"直接出现在地上"：喷溅从来就不存在（玩家报告）

**玩家报告**："血液粒子没有喷溅效果，直接出现在地上"。

**先定性：不是移植改坏的** —— 0.5.5 与移植逐字相同（构造器 `super(world,x,y,z, 0.1, 0.1, 0.1)` + `gravity = 1.5`，`lifetime = 12 / (0.1..1.0)`，两个调用点都是"同一个坐标刷 10 个"✓）⇒ "没喷溅"从 1.20.1 时代就是这样，只是一直没人报 ✓（和 §48 同一个教训：先确认"0.5.5 里它是工作的吗" —— 这里答案是**不工作的** ✓）。

**三条根因**（逐行读源码核对，不是猜）：
1. **同一个坐标刷 10 滴**：`ClientPlayHandler.handleMessageBlood` 循环 10 次、坐标一字不改（`message.getX/Y/Z` ✓）；`BeamHandler.spawnBeamImpactParticles` 虽然算了 `offsetX/Y/Z`，但幅度只有 **±0.1**（`(rand-0.5)*0.2` ✓）⇒ 视觉上仍是一团 ✓。
2. **液滴的运动是粒子自己写死的 0.1**，而**调用方传的三个"速度"参数根本不是速度** ✓：`BloodParticle.Factory.createParticle` 把它们交给 `setColor((float)xSpeed, (float)ySpeed, (float)zSpeed)` ✓ —— 投射物路径传 `0.5, 0.0, 0.5`（= 颜色 ✓），光束路径传**武器光束颜色** ✓（behaviour 必须留着，删了光束的血会变色 ✓）。所以 `0.1,0.1,0.1` 各向同性 + `gravity 1.5` ⇒ 一 tick 贴地 ✓。
3. **寿命没有上界**：`lifetime = 12 / random(0.1..1.0)` ⇒ **最长 120 tick（6 秒）** ✓ ⇒ 少数长寿液滴落地后还挂着，"整团慢慢落到地上"读起来就是"血直接出现在地上" ✓。

**改动**（`Config.CLIENT.particle` 新增 3 个选项；两条生成路径共用）：
- `bloodParticleCount`（默认 **12**，1..64）：一次命中喷几滴。
- `bloodParticleSpread`（默认 **0.15** 格，0..1）：出生点散布半径 —— 两条路径都按 ±spread 在**三个轴**上撒开 ✓。
- `bloodParticleSpeed`（默认 **1.0**，0..4）：喷射力度乘数（**0 = 垂直落下 = 改动前的观感** ✓，方便对照）。
- 粒子本体：在 `super` **之后**设速度 ✓（基类的 `random` 那时才存在，且 `super` 的速度参数已被基类吃掉 ✓）—— 随机水平方向 `cos/sin(angle)`、速率 `(0.10 + rand*0.45) * speed`、**向上偏置** `(0.15 + rand*0.30) * speed` ⇒ 有弧线 ✓；`gravity` **1.5 → 1.2F**（弧线看得见 ✓，落地仍快 ✓）；寿命改成 **`10 + rand(14)`** = 10–23 tick ✓。

**审计**（新增 `tools/audit_blood_spray.py`）：构造器仍设三轴速度、喷射必须随机、`yd` 必须为正、`gravity` 不得超上限、寿命必须是**短的有界区间**、`setColor((float)xSpeed, ...)` 这条"颜色走参数"的行为必须保留（光束染色依赖它 ✓）、两条生成路径的 count/spread 必须被读、三个选项必须存在、粒子必须读 speed。
**规则本身修过一次** ✓：初版"整个文件里 `random.nextDouble()` 少于 3 个就报错"**抓不住**"算了偏移量却传裸坐标"✗（偏移算了不用，计数照样够 ✓）⇒ 改成**在生成调用内部**数轴 ✓（内联写法，以及本方法内由 `nextDouble()` 赋值的局部变量，都算 ✓），并把作用域从整文件收到**方法体** ✓。反向验证 **8/8** ✓，其中 2 条专测新规则，并**逐条确认失败原因就是新规则本身** ✓（"BROKEN ... passes fewer than three offset axes to createParticle/addParticle" ✓）。
`verify_installed_jar` 新增 **11 项**：3 个选项在包里 ✓、粒子读 speed ✓、两条路径各读 count/spread ✓，以及 **`1.2F` 的 IEEE-754 常量池字节检查** ✓（打包后的**数字**只能这么查 ✓）；该条自身也做了反向验证 —— 同法查 `1.5` / `2.5` 都**查不到** ✓。

**门禁**：**44 个审计全 0** ✓ / `verify_installed_jar` **283/283**（新增 11 条 ✓）/ `javac` 0 错误（1013 文件 ✓）/ `build` ✓（`build/libs/scguns-0.5.5.1.jar` 19,444,072 B ✓，并逐类核对过新选项名确实进了常量池 ✓）/ 已安装 ✓（上一版备份 `.bak-194453` ✓；注释调整后重编的 jar **字节数完全一致** ✓ = 改动只动了注释 ✓）。
**未实测（交给玩家）**：① 打中生物时血是**先喷出去再落地**、不再是原地出现；② `12 / 0.15 / 1.0` 的手感 —— 嫌少嫌淡直接改配置（`bloodParticleCount` / `bloodParticleSpread` / `bloodParticleSpeed`，改完要重开会话 ✓）。

## §82.36 血液"没有掉落过程"的真因是**形状**不是大小（玩家要求对照其他 1.21.1 移植版）

**玩家反馈**：上一轮改完仍是"血粒子直接出现在地上，没有掉落的过程"，并要求"查看其他 1.21.1 移植版是怎么做的"。

**一、先按要求把别的移植版翻了个遍 —— 结论：全都一样，没有可抄的** ✓
- **官方 1.21.1 `ScorchedGuns-1.5.2.jar`**（玩家 GFL2MC 在用的那份）`javap -c` 反汇编：`BloodParticle` 构造器与 0.5.5 **逐字节同形** ✓（`super(..., 0.1, 0.1, 0.1)` / `gravity 1.5F` / `quadSize 0.1F` / `lifetime = 12/(rand*0.9+0.1)` ✓），`ClientPlayHandler.handleMessageBlood` 同样是"**10 个粒子、同一坐标、`0.5, 0, 0.5`**"✓，`ProjectileEntity` 传的同样是 `hitVec` ✓。
- 本机另两份 1.21.1 移植源码（`E:\mod\SG2-1.21\ScorchedGunsNeoforge-main`、`ScorchedGuns-NeoForge-New`）：`BloodParticle.java` 与 `handleMessageBlood` 与 0.5.5 **一字不差** ✓。
- 资源也一样：`assets/scguns/particles/blood.json`、`textures/particle/blood.png`、`blood.png.mcmeta` 在 0.5.5 / 1.5 / 1.5.2 / 本移植里的 **SHA1 完全相同** ✓。
⇒ **没有任何移植版把血做得更好** ✓；这不是移植引入的，而是上游从来如此 ✓（和 §48 同一条教训 ✓）。

**二、真因：这次是量出来的，不是猜的** —— 新增 `tools/audit_blood_trajectory.py`，**照 1.21.1 的 tick 逐步重放**（`Particle` 构造器给每轴 ±0.4 的随机、`yd -= 0.04*gravity`、`friction 0.98`、落地后 `BloodParticle` 把 `xd/zd` 归零并缩小 —— 全部取自**合并 jar 的字节码** ✓，不是记忆 ✓）：

| 版本 | 上升（格） | 滞空（tick） | 水平（格） |
|---|---|---|---|
| 上游（0.5.5 / 官方 1.5.2 / 所有移植） | **0.17** | **5.8**（0.29 秒） | 2.27 |
| 本轮 | **1.40** | **17.5**（0.88 秒） | 2.59 |

上游是"**平着喷出去**"：几乎不上升、四分之一秒就落地 ⇒ 玩家看到的正是"直接出现在地上，没有掉落的过程" ✓✓。

**三、改动（只改形状，开关照旧）**：`BloodParticle` 改成**以向上为主** —— 水平 `0.06 + rand*0.20`、**向上 `0.25 + rand*0.25`**、`gravity 1.5 → 1.0`、寿命 `10+rand(14) → 16+rand(14)`（16–29 tick，够落下来 ✓）。12 滴里 **9 滴在还活着的时候落地** ✓（= 玩家看得见"掉下来" ✓）。

**四、顺带发现：玩家自己的客户端配置把效果调小了一半** —— `config/scguns-client.toml` 里 `bloodParticleSpread = 0.01`、`bloodParticleSpeed = 0.5`（20:03 写入 ⇒ 玩家在游戏里调过 ✓）。速度 **0.5** 时本轮数值只剩"上升 0.33 / 滞空 12 tick" ⇒ 自然还是觉得"没变化" ✓。**建议把 `bloodParticleSpeed` 调回 1.0** ✓（1.5–2.0 更夸张；0 = 旧观感 ✓）。

**五、审计**：`tools/audit_blood_trajectory.py`（第 **45** 个）从源码读 gravity / 寿命 / 两个速度表达式并**模拟**，要求：上升 ≥0.6 格 ✓、滞空 ≥14 tick ✓、水平 0.5–5 格 ✓、必须有液滴在活着时落地 ✓，并且**要求上游数值必须不达标** ✓（否则这条规则就没在量东西 ✓）。反向验证 **8/8** ✓（其中 5 条专测本审计：退回上游重力 ✓ / 退回上游寿命 ✓ / 喷得太平 ✓ / 喷得太远 ✓ / 数值读不出来 ✓，且逐条确认失败原因正确 ✓）。
`audit_blood_spray.py` 的 `MAX_GRAVITY` 由 1.5 收到 **1.2** ⇒ 上游的 1.5 现在会**失败** ✓。`verify_installed_jar` 把"gravity=1.2F 在常量池里"那条（**本身没区分度**，1.0F 在 `setColor` 的白血分支里就有 ✓）换成 **"向上偏置 0.25（double）在"+ "上游的 1.5F（float）不在"** ✓ —— 后者是有区分度的那一半：对官方 1.5.2 与 0.5.5 的 jar 实测**两条都 FAIL** ✓，对本轮 jar 通过 ✓（净 +1 条 ⇒ **284/284** ✓）。

**门禁**：**45 个审计全 0** ✓ / `verify_installed_jar` **284/284** ✓ / `javac` 0 错误（1013 文件 ✓）/ `build` ✓ / 已安装 ✓（上一版备份 `.bak-201354` ✓）。
**未实测（交给玩家）**：把 `bloodParticleSpeed` 调回 **1.0** 后打中生物，应能看到血**向上喷出、划弧再落下**（不再是一出现就躺在地上）；嫌夸张在 0.7–1.2 之间调 ✓。

## §82.37 与 1.20.1 **逐值对齐**：运动改成 0.5.5 原样，配置只做倍率（玩家要求）

**玩家澄清**："1.20.1 的血液粒子喷溅效果是正常生效的，我们的 1.21.1 移植缺失了这个效果（粒子可能没有正常渲染），直接出现在地上" ⇒ 目标从"我觉得更好看"改成**与 1.20.1 一致** ✓。

**一、先把"移植丢了代码"这个可能彻底排掉（全是实证）**：
- 玩家 1.20.1 实例（`D:\MCJAVA\.minecraft\versions\1.20.1-Forge_47.4.21\mods`）里就是 **`ScorchedGuns-0.5.5-1.20.1.jar`** ✓ —— **和我们移植的基准是同一个 jar** ✓；两个实例的日志显示**今天 19:22 玩 1.20.1、20:00 玩 1.21.1** ✓ ⇒ 这是真正的同版本、背靠背对比 ✓。
- **mod 代码**：`BloodParticle`（构造器/`tick`/`render`/Factory）、`handleMessageBlood`、`ProjectileEntity` 传的 `hitVec`、`S2CMessageBlood` 的编解码 —— 与 0.5.5 反编译源码**逐行一致** ✓；本机另外两份 1.21.1 移植源码也一样 ✓。
- **引擎代码**：把 1.20.1 的**反混淆合并 jar**（ForgeGradle 缓存里的 `forge-1.20.1-47.4.10_mapped_official_1.20.1.jar`）与 1.21.1 合并 jar 逐方法反汇编对比 —— **`Particle` / `SingleQuadParticle` / `TextureSheetParticle` 的指令完全相同** ✓（只有常量池索引不同；`SingleQuadParticle` 只是多了 `getFacingCameraMode`/`renderRotatedQuad`/`renderVertex`/`getRenderBoundingBox` 这几个新方法 ✓），连 `±0.4` 随机、`yd -= 0.04*gravity`、落地 `onGround` 判定、`stoppedByCollision` 都一样 ✓。
- **资源**：`blood.json` / `blood.png` / `blood.png.mcmeta` 在 0.5.5、1.5、1.5.2、本移植里 **SHA1 全同** ✓；注册方式（`registerSpriteSet(BLOOD, BloodParticle.Factory::new)`）也一样 ✓。
⇒ **不存在"移植漏了这段代码"** ✓，唯一还能不同的就是**运动数值本身** ✓。

**二、改动：把 0.5.5 的运动原样写出来，配置只做倍率** —— 0.5.5 的运动不是它自己那三个参数，而是"`super(..., 0.1, 0.1, 0.1)` + 原版构造器给每轴加的 `±0.4`"✓。现在写成：
```java
this.xd = (0.1 + (this.random.nextDouble() * 2.0 - 1.0) * 0.4) * speedMultiplier;   // yd、zd 同
this.gravity = 1.5F;
this.lifetime = (int)(12.0F / (this.random.nextFloat() * 0.9F + 0.1F));
```
⇒ **倍率 = 1.0 时与 0.5.5 逐值相同** ✓（不再是我前两轮"改形状"的版本 ✓）。倍率必须乘在**最终速度**上（原版加的 `±0.4` 不受种子影响，只乘种子等于没改）✓。

**三、审计改成"与 0.5.5 对齐"而不是"我觉得好看"**：
- `tools/audit_blood_trajectory.py` 重写为**对拍**：从源码读出 gravity / 寿命公式 / 三个轴的速度式，**照 1.21.1 的真实 tick 重放**，要求各项与 0.5.5 相差 **≤12%**，并要求倍率确实能放大 ✓。实测：0.5.5 `上升 0.24 / 滞空 6.5 tick / 水平 2.44`、本轮 `0.21 / 6.4 / 2.35` ✓（倍率 0.5 ⇒ 0.04/5.9/1.17，2.0 ⇒ 0.87/8.5/5.14 ✓）。
- `tools/audit_blood_spray.py`：把上一轮"重力必须 ≤1.2、寿命必须是短区间、yd 必须为正"这些**我自己的口味规则**换成**对齐规则**（重力必须 1.5、寿命必须 `12/(rand*0.9+0.1)`、三个轴必须都是 `(0.1+(rand*2-1)*0.4)*倍率`）✓。
- `verify_installed_jar`：把"向下偏置 0.25 在 / 1.5F 不在"换成 **"`±0.4`（double）在" + "重力 `1.5F` 在"** ✓；反向验证：本 jar 两条都过 ✓、**0.5.5 原 jar 是 `0.4` 不在**（它靠引擎加 ✓）、上一版 §82.36 是**两条都不在** ✓。
- 反向验证 **8/8** ✓（重力偏离 ✓ / 散布不是 0.4 ✓ / 寿命公式变了 ✓ / 某个轴不再乘倍率 ✓ / 重力变温和 ✓ / 寿命变成区间 ✓ / 某个轴丢了原版散布 ✓ / 倍率选项没了 ✓）。

**四、⚑ 仍待玩家实测（诊断版已装）**：这一轮还发现**玩家自己的客户端配置是 `bloodParticleSpeed = 0.5` + `bloodParticleSpread = 0.01`**（20:03 写入 ✓）—— 速度减半、12 滴从**同一个点**出来，正是"一团血直接落到地上"的观感 ✓。所以：请把 `bloodParticleSpeed` 调回 **1.0**（`spread` 建议 0.15）✓。为拿到真凭实据，**当前安装的 jar 带一个临时探针**（`SCGUNS-BLOOD` 日志：数据包坐标/坐标下方的方块/每滴初速度/每 4 tick 的位置与是否落地/渲染时的 sprite·UV·alpha·光值 ✓），玩家打一枪后我直接读 `latest.log` 即可判定是"生成位置、运动、还是渲染"哪一环 ✓。**下一轮拿到日志后立刻删除探针** ✓（本轮为了"已装 jar 与提交源码一致"暂时保留并在代码里标注 TEMPORARY ✓）。

**门禁**：45 个审计全 0 ✓ / `verify_installed_jar` **284/284** ✓ / `javac` 0 错误（1013 文件 ✓）/ `build` ✓ / 已安装 ✓（备份 `.bak-202420` ✓）。

## §82.39 **找到真凶：手写四边形绕序反了 ⇒ 空中的血粒子一直被背面剔除**（玩家判断正确）

**玩家线索（决定性的两条）**："命中点产生的血液粒子从来没有出现过，只有在地上的血液粒子" ✓ 与"我认为是渲染错误" + 截图 ✓。

**一、先用玩家自己的客户端日志把"是不是没生成/没运动"排掉**（§82.37 装的探针正好在截图那段时间生效 ✓）：
- 16 次命中 / 192 滴 / **2112 次 `render` 调用** ✓ —— 渲染函数**被调用了** ✓；`sprite=scguns:blood`、`u/v` 落在 4×4 帧内 ✓、`alpha=1.0` ✓、`rCol=0.541` ✓、`light=15728640`（天光 15 ✓）✓、`renderType=PARTICLE_SHEET_TRANSLUCENT` ✓。
- 生成点 `y≈64.3–64.8`（地面 y=63）✓ = 生物胸口 ✓；初速度 ±0.25 ✓、每 4 tick 的位置在正常变化 ✓、落地点 `y=63.000` ✓。
- **`onGround=true` 的采样占 80%** ✓；在空中：age 4 → 192/192、age 8 → 77/192、age 12 → 2/192 ✓（飞行只有 0.3–0.5 秒 ✓）。
- **决定性一条**：把日志时间戳与截图时间对齐 —— **20:29:53 那张截图是在命中后 0.41 秒拍的，当时 12 滴里有 11 滴仍在空中** ✓，可是画面里**只有地上的血** ✗ ⇒ **空中的粒子确实没画出来** ✓（玩家的判断正确 ✓，我前两轮的"大小/形状"方向都错了 ✓）。

**二、真因：四边形绕序（winding）反了 ⇒ 背面剔除**
- 1.21.1 原版 `SingleQuadParticle.renderRotatedQuad` 的四个角是 **(1,-1)→(1,1)→(-1,1)→(-1,-1)**，即**逆时针**（正面朝 +Z，经 `camera.rotation()` 后正对镜头 ✓）。
- 而 0.5.5（以及本移植照抄的）是 **(-1,-1)→(-1,1)→(1,1)→(1,-1)**，即**顺时针**（正面朝 −Z ✓）。
- 用**游戏自己的类**实测（`javap` 取 `Direction.getRotation()`，JOML 算旋转后的法线 ✓）：
  - 落地态（`Direction.NORTH.getRotation()`）：−Z → **(0, +1, 0) 朝上** ✓ ⇒ **看得见** ✓（这正是"只有地上的血"的原因 ✓）；
  - 空中态（`camera.rotation()`）：−Z → **背对镜头** ✗ ⇒ **被剔除 ⇒ 不可见** ✗；
  - 反过来用逆时针：空中态 → **正对镜头** ✓。
- 1.20.1 没事是因为当年这条路径没有剔除它画的东西 ✓；1.21.1 会 ✓ —— **所以"1.20.1 正常、移植缺失"完全吻合** ✓（也与"所有移植版都一样错"吻合 ✓）。

**三、修法**：`BloodParticle` 增加 `cameraFacing` 标志 —— **朝向镜头的那一支按逆时针发出四角（3,2,1,0），落地那一支保持原绕序** ✓（两态都正面朝观察者 ✓，贴图与 UV 逐点不变 ✓）；顺带修 **`BulletHoleParticle`**（同一处绕序问题：原来正面朝**方块内部**，弹孔从你站的那一侧被剔除 ✓ —— 实测 up→(0,−1,0)、north→(0,0,1) ✓）。
**新增审计 `tools/audit_particle_winding.py`（第 46 个）**：从源码读四角坐标与发出顺序，用**鞋带公式**算有向面积，要求"朝向镜头的那支必须逆时针、落地那支必须顺时针、弹孔必须逆时针"，并把上面测得的方向写进理由 ✓。反向验证 **4/4** ✓（空中支退回顺时针 ✓ / 落地支翻转 ✓ / 不再标记朝向镜头分支 ✓ / 弹孔绕序反转 ✓，且失败原因都是绕序规则本身 ✓）。

**四、探针转为常驻开关**：新增 `bloodDebugLog`（**默认 false** ✓），需要时打开即可拿到 §82.37 那套日志 ✓。审计新增一条规则：**`SCGUNS-BLOOD` 日志必须在 `bloodDebugLog` 之后** ✓（否则会往每个玩家日志里刷几千行 ✓），反向验证 **3/3** ✓；`verify_installed_jar` 新增 2 项（选项存在 ✓ + 粒子确实先查它 ✓）⇒ **286/286** ✓。

**门禁**：**46 个审计全 0** ✓ / `verify_installed_jar` **286/286** ✓ / `javac` 0（1013 文件 ✓）/ `build` ✓ / 已安装 ✓（备份 `.bak-204614` ✓）。
**未实测（交给玩家）**：现在开枪时**命中点应该立刻能看到血喷出来并飞出去** ✓，落地后仍是地上的血 ✓；`bloodParticleSpeed` 建议 1.0（玩家配置当时是 0.5 ✓）。

## §82.40 **撤回弹孔那处改动**（玩家："弹孔不需要修改，改回之前的做法，这次给改坏了"）

**玩家报告**：弹孔（`BulletHoleParticle`）本来正常，§82.39 顺手"修"了它的绕序 ⇒ **改坏了** ✓ ⇒ 已**逐行还原成 0.5.5 的原样** ✓（四角按 `points[0..3]` 顺序、UV `f8f6 / f8f5 / f7f5 / f7f6` ✓）。

**为什么会坏（也解释了为什么血要改、弹孔不能改）**：两者**渲染类型不同** ✓ ——
- 血用 `ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT` ✓ ⇒ **会剔除背面** ✓ ⇒ 正面必须朝观察者 ✓（§82.39 的修法 ✓）；
- 弹孔用 `ParticleRenderType.TERRAIN_SHEET` ✓ ⇒ **不按同一套剔除** ✓ ⇒ 它原本"正面朝方块内部"照样正常显示 ✓；把它反过来反而让正面朝向不对 ⇒ 弹孔消失 ✓✓。
⇒ **教训**：绕序的结论**不能跨渲染类型照搬** ✓（我把它当成通用问题了 ✗）。

**审计同步**：`tools/audit_particle_winding.py` 里弹孔那条从"必须逆时针"改成 **"必须保持 0.5.5 的原始发出顺序 [0,1,2,3]"** ✓，理由写进规则文本 ✓。反向验证：把它再反过来 ⇒ 报 `emits its corners as [3, 1, 2, 3]; it must stay in 0.5.5's original order` ✓；干净树 0 ✓。

**门禁**：46 个审计全 0 ✓ / `verify_installed_jar` **286/286** ✓ / `javac` 0（1013 文件 ✓）/ `build` ✓ / 已安装 ✓（备份 `.bak-210027` ✓）。
**待办**：采矿枪（`scguns:shard_culler` / `scguns:cr4k_mining_laser`）"没有挖掘方块的纹理"——需要玩家确认指的是**挖掘裂纹（破坏阶段贴图）**还是**破坏时的方块碎屑粒子**（两者在代码里是两条不同路径 ✓）；已核对的部分：两者都与 0.5.5 逐字相同 ✓、`enableBeamMining` 在玩家配置里是 true ✓、1.21.1 客户端确认**会渲染**裂纹且**不按 id 过滤** ✓、服务端 `destroyBlockProgress` 会**跳过 breakerId 等于自己 id 的玩家**（所以假 id 反而让挖掘者能收到 ✓）。

## §82.41 顺带查实：光束代码里两处 `forge:` 标签在 1.21.1 **根本不存在**（已修）

追查上一条（采矿枪的"裂纹"）时，把整条**光束采矿/破坏**路径逐方法对比了 1.20.1 与 1.21.1 的引擎实现 —— `ServerLevel.destroyBlockProgress`、`ClientPacketListener.handleBlockDestruction`、`ClientLevel.destroyBlockProgress`、`LevelRenderer.destroyBlockProgress` **四处的指令流完全相同** ✓（连"跳过 id 等于 breakerId 的玩家"这条过滤都一样 ✓），mod 自己的 `BeamHandlerCommon` / `BeamWeaponHandler` / 枪数据 / `fragile` 标签也与 0.5.5 等价 ✓ ⇒ **裂纹这条链路上没有找到移植差异** ✓（详见上一条待办 ✓）。

**但顺手查出两处真问题**：NeoForge 21.1 把跨模组通用标签从 `forge:` 改名成 `c:` ✓，本移植的数据文件早就迁移了（自己的 `scguns:fragile` 里写的是 `#c:glass_blocks` / `#c:glass_panes` ✓），**但 Java 里还有两处按老名字查标签** ✗ —— 而"没人定义的 `TagKey`"不会报错，它只会**对一切方块恒为 false** ✓：
- `BeamHandlerCommon.isGlassBlock` 里的 `#forge:glass` ✗ ⇒ **带标签的玻璃（染色玻璃、其它模组的玻璃）不再算玻璃** ⇒ 光束不会穿过它 ✓（0.5.5 里会 ✓）；改用 `Tags.Blocks.GLASS_BLOCKS`（= `c:glass_blocks` ✓，NeoForge 21.1 的同一个标签 ✓）。
- `#forge:ores` ✗ ⇒ 已改 `Tags.Blocks.ORES` ✓；这一处**本来也不影响行为**（0.5.5 的 if/else 两个分支**写得一模一样** ✓），如实注明 ✓。

**新增审计 `tools/audit_tag_namespaces.py`（第 47 个）**：扫描所有 `BlockTags/ItemTags/TagKey.create(...)` 里的命名空间，要求它属于 `minecraft` / `c` / `neoforge` / `scguns` / **本移植自己发布了 `data/<ns>/tags/` 的命名空间** ✓；另禁止源码里再出现 `"forge:..."` 字面量 ✓。反向验证 **3/3** ✓（`#forge:glass` ✓ / `#forge:ores` ✓ / 一个本移植没有数据的命名空间 ✓，且都报在该行 ✓）；干净树 **7 处查找全过** ✓。

**门禁**：**47 个审计全 0** ✓ / `verify_installed_jar` **286/286** ✓ / `javac` 0（1013 文件 ✓）/ `build` ✓ / 已安装 ✓（备份 `.bak-210709` ✓）。

## §82.42 **采矿枪不能附时运/精准采集：0.5.5 写在 Java 里的规则在 1.21.1 被删了**（玩家报告，已修）

**玩家报告**："这类可以挖掘方块的武器不能正常附魔时运和精准采集" ✓。

**根因（0.5.5 有、移植丢）**：0.5.5 的 `GunItem` 重写了 **`canApplyAtEnchantingTable`**（反编译源码 `:278-287` ✓）：
```java
return !stack.is(ModTags.Items.MINING_GUN) || enchantment != Enchantments.BLOCK_FORTUNE && enchantment != Enchantments.SILK_TOUCH
   ? super.canApplyAtEnchantingTable(stack, enchantment) : true;      // ← 采矿枪恒可附时运/精准
```
而 **1.21.1 直接删掉了这个方法** ✗ —— "某附魔能否附到某物品"改由**附魔自身 JSON 的 `supported_items` 标签**决定 ✓。从玩家客户端的原版 jar 里实测（`data/minecraft/enchantment/fortune.json` / `silk_touch.json` ✓）：
```json
"supported_items": "#minecraft:enchantable/mining_loot"
```
而该标签默认只有 `#axes / #pickaxes / #shovels / #hoes` ✓ ⇒ 我们的枪**不在里面** ⇒ 附魔台**静默不再提供**时运/精准 ✓✓（`isBookEnchantable` 里那条"采矿枪收任何书"的规则还在 ✓，所以铁砧那条路是通的 ✓ —— 也解释了为什么表现为"不能**正常**附魔" ✓）。

**修法（把 Java 规则改写成数据）**：新增 `data/minecraft/tags/item/enchantable/mining_loot.json` = `{"replace": false, "values": ["#scguns:mining_gun"]}` ✓（`#scguns:mining_gun` 里正是 `cr4k_mining_laser` + `shard_culler` ✓，与 0.5.5 的 `enableMining` 集合一致 ✓）。
**刻意不加 `#minecraft:enchantable/mining`（效率）** ✓：0.5.5 对效率走的是 `super` ⇒ 1.20.1 的 `EnchantmentCategory.DIGGER.canEnchant` = `item instanceof DiggerItem` ⇒ 枪**从来就不能附效率** ✓ ⇒ 保持原样 ✓（写在审计的理由里 ✓）。

**新增审计 `tools/audit_enchant_applicability.py`（第 48 个）**：要求该标签存在、含 `#scguns:mining_gun`、且**不是 `replace: true`**（否则会把原版工具全废掉 ✓）；并要求 `mining_gun` 标签非空、`GunItem.isBookEnchantable` 那条特例仍在 ✓。反向验证 **4/4** ✓（标签文件缺失 ✓ / 条目被删 ✓ / 改成 replace ✓ / 铁砧特例被改 ✓）。`verify_installed_jar` 新增 1 项（打包后的标签里确实有 `scguns:mining_gun` ✓）⇒ **287/287** ✓。

**门禁**：**48 个审计全 0** ✓ / `verify_installed_jar` **287/287** ✓ / `javac` 0（1013 文件 ✓）/ `build` ✓ / 已安装 ✓（备份 `.bak-211227` ✓）。
**待实测（交给玩家）**：① 附魔台现在应该能对 `shard_culler` / `cr4k_mining_laser` 提供时运/精准（需要重进世界让标签重载 ✓）；② 铁砧用书依旧可以 ✓。
**仍未解决（本轮装了探针）**：光束采矿**看不到裂纹**（原版挖掘正常 ✓ ⇒ 客户端渲染没问题 ⇒ 差异在光束路径 ✓）；已把服务端每 tick 的 `pos/方块/硬度/枪/速度/进度/阶段/breakerId` 打进日志 ✓，请用采矿枪挖一格方块后告诉我，我直接读 `latest.log` 定位 ✓。
**另记（本轮未动）**：0.5.5 的同一个重写里还有"**半自动类附魔不得附到全自动枪上**"这一条（`enchantment.category == SEMI_AUTO_GUN` 且 `fireMode != AUTOMATIC` ✓），1.21.1 里同样只能靠标签表达 ✓，本移植的附魔 `supported_items` 是按"枪/非枪"生成的 ⇒ 这条**目前没有体现** ✓；是否补上属于行为对齐，等玩家拍板 ✓。

## §82.43 **采矿枪看不到裂纹的真因：假 breakerId 撞上玩家实体 id，包被服务端自己过滤掉了**（玩家判断正确）

**玩家提示**："原版的挖掘和破坏方块是正常的" ✓ + "查看日志，我觉得也是渲染bug" ✓。

**一、日志先证明"服务端一切正常"**（182 行 `SCGUNS-MINE` ✓，玩家用的是 `cr4k_mining_laser`（speed 14.0）与 `shard_culler`（speed 1.0）✓）：
- 快枪（14.0）挖草方块：**第 1 tick 进度就到 2.33** ⇒ `newStage = min(23, 9) = 9` ✓，第 2 tick 破坏 ⇒ **裂纹最多存在 1 tick（50 ms）** ✓ ⇒ 看不见是**公式本身**决定的 ✓（0.5.5 亦然 ✓）。
- **慢枪（1.0）挖泥土：阶段序列是 `1 → 3 → 5 → 6 → 8 → 9`，跨约 7 tick** ✓✓ —— **服务端确实在一个阶段一个阶段地发包** ✓。
⇒ **服务端没问题 ⇒ 玩家的判断对：问题在客户端一侧** ✓（准确说是"客户端根本没被告知" ✓）。

**二、真因：`ServerLevel.destroyBlockProgress` 会跳过"实体 id == breakerId"的那个玩家**
1.21.1 反汇编实测（合并 jar ✓）：
```java
if (player != null && player.level() == this && player.getId() != breakerId
    && player.blockPosition().distSqr(pos) < 1024.0D) player.connection.send(new ClientboundBlockDestructionPacket(...));
```
**原版挖矿不需要把裂纹发回给挖掘者本人** ✓（客户端用 `MultiPlayerGameMode` 自己在本地记 ✓）；而**光束挖矿不是客户端自己的挖掘** ⇒ 完全依赖这个包 ✓。
而本移植的 `nextBreakerId` **从 1 开始自增** ✗ ⇒ **第一个用采矿枪的玩家拿到 id = 1** ✓ ⇒ **只要该玩家的实体 id 也是 1，服务端就把他自己过滤掉** ⇒ 永远收不到裂纹包 ✓✓✓（实体 id 只会 ≥ 0 ✓，而每次开游戏计数器都会重置 ⇒ 单机里玩家 id 很小的概率很高 ✓）。

**三、修法**：把 breaker id 改成**只可能为负**的计数器（`nextBreakerId = -1` 且 `nextBreakerId--` ✓）⇒ **永远不可能等于任何实体 id** ✓✓。**刻意不用玩家真实 id**：客户端自己的挖掘状态是**按 id 存**的 ✓，用玩家 id 会让"手动挖任何方块"顺手清掉光束的裂纹 ✗ ⇒ 保持假 id、但保证不撞 ✓。

**新增审计 `tools/audit_beam_mining_crack.py`（第 49 个）**：breaker id 计数器必须从负数开始并递减 ✓、阶段包必须按存下来的 `breakerId` 发送 ✓、阶段变化必须发包 ✓（理由里带上了上面那段原版字节码 ✓）。反向验证 **2/2** ✓（退回 `= 1` + 自增 ✓ / 只把初值改成正数 ✓）。

**门禁**：**49 个审计全 0** ✓ / `verify_installed_jar` **287/287** ✓ / `javac` 0（1013 文件 ✓）/ `build` ✓ / 已安装 ✓（备份 `.bak-214227` ✓）。
**待实测（交给玩家）**：用 **`shard_culler`**（慢枪，1.0）挖一格石头/泥土 ⇒ **应该能看到一格一格加深的裂纹** ✓；快枪（14.0）因为一 tick 就破坏到位 ⇒ 裂纹仍然只有一帧 ✓（如需放慢属平衡改动 ✓，`cr4k` 的 `miningSpeed` 就在枪的 JSON 里 ✓）。日志里新加了 `playerId=` 与 `skipped=` 两列 ✓，正好可以核对这次是否命中 ✓。
**探针仍在**（`SCGUNS-MINE` ✓，下一轮删 ✓）。

## §82.44 裂纹**已实测正常** ✓；补上光束挖掘的**敲击音效**（玩家要求）

**玩家实测**："正常了" ✓ ✓ ⇒ §82.43 的假 breakerId 就是根因 ✓（探针已删除 ✓）。

**一、先查清"破坏音效到底有没有"**：0.5.5 的 `breakBlockWithEnchantments` 在两条分支里都调 `world.levelEvent(2001, pos, Block.getId(blockState))` ✓，本移植**逐字相同** ✓。逐层核对 1.21.1：
- `LevelAccessor.levelEvent(int, BlockPos, int)`（default）→ `levelEvent(null, type, pos, data)` ✓；
- `ServerLevel.levelEvent(Player, int, BlockPos, int)` → 向 64 格内玩家广播 `ClientboundLevelEventPacket` ✓；
- 客户端 `LevelRenderer.levelEvent` 的 `case 2001` → 非空气方块 → `getSoundType(...).getBreakSound()` + `playLocalSound` ✓ **并且** `addDestroyBlockEffect`（碎屑粒子）✓。
⇒ **"破坏"那一下的粒子与音效本来就是通的** ✓，所以缺的不是它 ✓。

**二、真正缺的是"敲击音效"（挖的过程中那声"咚、咚"）**：原版这颗音由**客户端**播放 —— `MultiPlayerGameMode.continueDestroyBlock`（反汇编实测 ✓）：每 **4 tick** 一次、`state.getSoundType(level,pos,entity).getHitSound()`、音量 `(volume + 1) / 8`、音高 `pitch * 0.5`，并用 `level.playLocalSound` 只放给挖掘者自己 ✓。**光束是服务端驱动的 ⇒ 客户端这段代码永远不会跑 ⇒ 挖方块全程无声** ✓✓。

**修法（照抄原版数值 ✓）**：在 `handleBeamMining` 里
```java
if (world.getGameTime() % 4L == 0L) {
   SoundType soundType = state.getSoundType(world, pos, player);
   player.playNotifySound(soundType.getHitSound(), SoundSource.BLOCKS,
      (soundType.getVolume() + 1.0F) / 8.0F, soundType.getPitch() * 0.5F);
}
```
✓ 用 `playNotifySound` **只发给挖掘者** ✓（与原版"只有自己听得见"一致 ✓）；**不额外播放破坏音** ✓（2001 已经在放 ✓，再放一次会变成双响 ✗）。

**审计同步**（`tools/audit_beam_mining_crack.py` 扩成"光束挖掘反馈" ✓）：新增 5 条 —— 必须播放 `getHitSound` ✓、间隔必须是 4 tick ✓、音量必须是 `(volume+1)/8` ✓、音高必须是 `pitch*0.5` ✓、必须只发给挖掘者（`playNotifySound`）✓，并要求破坏仍走 `levelEvent(2001)` ✓。反向验证 **5/5** ✓（换成别的音效 ✓ / 间隔改成 20 tick ✓ / 音量改成 /2 ✓ / 去掉音高 0.5 ✓ / 改成 `world.playSound` 放给所有人 ✓）。

**门禁**：**49 个审计全 0** ✓ / `verify_installed_jar` **287/287** ✓ / `javac` 0（1013 文件 ✓）/ `build` ✓ / 已安装 ✓（备份 `.bak-215223` ✓）；探针已清空（`SCGUNS-MINE` / `TEMPORARY` 命中数 **0** ✓）。
**待实测（交给玩家）**：用采矿枪挖方块时应该能听到**持续的四拍敲击声**（与原版手感同频同音），破坏那一下仍是方块自身的破坏音 ✓。
