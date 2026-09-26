# Scorched Guns 0.5.5 → MC 1.21.1 / NeoForge 移植：交接文档

> 写给接手这个工作的下一个人（或 AI）。先读这一份，再看 `PORTING_STATUS.md`（简版状态）与 `build-logs/`（编译历史）。
> 基线：`git log -1` 见本文件所在的提交（`docs:` 开头那个）；文中数字都用 §9 末尾的命令核对过。

---

## 1. 任务与硬约束

| 项 | 值 |
|---|---|
| 目标 | 把 **Scorched Guns 0.5.5（MC 1.20.1 / Forge 47.x）** 移植到 **MC 1.21.1 + NeoForge 21.1.249** |
| 代码基准 | **只有** 0.5.5 的 1.20.1 release jar（`需要移植的mod/ScorchedGuns-0.5.5-1.20.1.jar`，19,173,074 B） |
| 反编译产物 | `E:\mod\SG2-1.21\.sg055_deobf`（994 个 .java，已用 TSRG 把 SRG 名还原为官方名，可读） |
| 已核验 | jar 内 **994 个外类** 与反编译产物 **双向差集为 0**，确认基准就是 0.5.5 |
| **禁止** | ① GitHub 上的 `Scorched-Guns-1.20.1-master` 实为 **0.4.7**，不可当基准；② `E:\mod\SG2-1.21\ScorchedGunsNeoforge-main` 是**现存 0.4.7 底子的 NeoForge 移植**，只能当 API 对照读物，不得搬代码 |
| Java | `D:\jdk-21.0.3`（必须显式设 `JAVA_HOME`，PATH 上是 Java 8） |
| 完成标准 | `gradlew build` 通过，产出可加载的 Scorched Guns 1.21.1 NeoForge jar |

## 2. 当前进度（最重要的一行）

**编译错误 0 个**（初始结构性基线 3487 → 接手时 496 → 0），989 个源文件全部通过。
**`gradlew clean build` 通过**，产出 `build/libs/scguns-0.5.5.1.jar`（18.5 MB；版本号见 §73）。
**专用服务器实测加载成功**：`Done (3.947s)! For help, type "help"`，无 mod 相关 ERROR/FATAL，
战利品表 0 解析失败。证据日志：`build-logs/server-final.txt`、`build-logs/build-final.txt`。

JEI 的 8 个界面类**已并入编译**（`build.gradle` 里那 8 行 `exclude` 已删除）。它们只需要两处
改动：`getAllRecipesFor` 现返回 `List<RecipeHolder<T>>`、接口默认方法必须写成
`IRecipeCategory.super.draw(...)`（见 `tools/port_rewrite44.py`；`build-logs/JEI19_NOTES.md`
有完整的 JEI 19 API 核对记录）。

**客户端已由用户实测多轮**（NeoForge 21.1.250）。实测暴露了 4 类 `runServer` 测不到的问题，
已逐个修复（详见 §10.1、§10.8）：

| 轮到 | 症状 | 根因 |
|---|---|---|
| 1 | 启动崩 | `RangeFinderItem` / `LaserSightItem`：`@EventBusSubscriber` 同时挂在**外层类**和**内部 handler 类**上，外层那个没有 handler → 新增 `tools/audit_subscribers.py` |
| 2 | 启动崩 | `ModelOverrides.register(model)` 注册了没有 handler 的 model 实例 → 旧版 `audit_bus_registrations.py` **静默跳过变量实参**，已加固 |
| 3 | 读护甲崩 | `ArmorMaterial` 在 1.21.1 是 built-in registry，移植按 1.21.2 写成了数据包 → §10.1 |
| 4 | 掉东西就崩 / 视觉 | `EntityTickEvent` 对所有实体触发却强转成 LivingEntity（§已修）；**20 处 `setCanceled(true)` 被丢**（§10.8），其中第一人称手部渲染未取消正好表现为"枪画两遍 + 多一条手臂" |

客户端侧的机械性加载失败现在由 **7 项静态审计**覆盖（见 §6.5），全部为 0。
**遗留的非阻塞偏差见 §10。**

## 3. 环境与构建（踩过的坑都在这里）

```powershell
cd E:\mod\scgun-0.5.5-1.21.1-neoforge
$env:JAVA_HOME='D:\jdk-21.0.3'
cmd /c "gradlew.bat compileJava --console=plain > build-logs\compile-NN.txt 2>&1"
```

- **必须用 `cmd /c` 重定向**。用 PowerShell 管道（`| Tee-Object`）会把日志按控制台宽度**折行并加前缀**，导致解析器全废。
- **Gradle**：wrapper 指向 `https://mirrors.tencent.com/gradle/gradle-8.8-bin.zip`。官方 `services.gradle.org` 在本机 JDK 的 `HttpsURLConnection` 下 **TLS 证书校验失败**（下载卡在 0 字节）。用户级 `~/.gradle` 里已有 gradle-8.8 与 NeoForge 21.1.249 缓存，可直接离线复用。
- **网络**：`maven.neoforged.net` 的目录列表会被拒，但**具体 artifact 可下载**（HTTP 200）；`repo1.maven.org`、`services.gradle.org` 正常。
- **javac 输出英文**：`gradle.properties` 里 `-Duser.language=en`，否则错误信息是乱码，脚本无法分类。
- **判断"这一轮是否真有进展"**：只看错误数不够，**必须看 `error kinds`**。若出现 `')' expected` / `illegal character` / `reached end of file` 等**语法类**错误，说明有编排事故，错误数会**虚假暴跌**（本会话发生过 4 次，见 §7 前四行）。

## 4. 工程结构

```
src/main/java/top/ribs/scguns/     当前 988 个 .java（反编译导入 994：删了附魔类等 16 个、
                                   新增兼容层 10+ 个；另 8 个 JEI 界面类在源里但不参与编译）
src/main/resources/                6404 个文件（`convert_resources.py` 转换 6374 + 生成器产出约 30）
libs/                              13 个 jar（3 必需 + 10 联动，见下）
tools/                             59 个脚本（见 §6）
build-logs/                        108 份逐轮编译日志（最新 = compile-109.txt，可回溯每一轮）
PORTING_STATUS.md                  简版状态
HANDOFF.md                         本文件
```

**依赖声明**（`build.gradle`）：
- 必需：`framework-neoforge-1.21.1-0.13.11`、`geckolib-neoforge-1.21.1-4.9.3`、`curios-neoforge-9.5.1+1.21.1`（`implementation files(...)`）
- 联动 10 个：JEI 19.27.0.340 / Create 6.0.10 / Create New Age 1.2.0 / Create Aeronautics 1.3.2 / Create Ore Excavation 1.6.8 / Immersive Engineering 12.4.2-194 / Mekanism 10.7.19.85 / Sable 2.0.5 / Prometheus 1.2.5 / Cobweb 1.4.0 —— 以 **`compileOnly` + `localRuntime`** 接入（可选依赖，不进发布依赖表）
- **还没接进去但已经有 jar**（在 `准备的前置/` 里，可以从容补）：`soul-fire-d-neoforge-1.21-6.1.0.jar`、`[农夫乐事] FarmersDelight-1.21.1-1.3.4.jar`。接入方式与上面 10 个一样（记得去掉中文文件名前缀，Gradle 的 fileTree 处理不了）。

## 5. 关键设计决定（**不要轻易推翻**）

### 5.1 NBT → DataComponents（`util/NbtHelper.java`）
1.21 删除了 `ItemStack.getOrCreateTag/getTag/setTag`。移植用 **`DataComponents.CUSTOM_DATA`** 承载原 NBT，**完整保留 NBT 键名**（`AmmoCount`/`PrevAmmoCount`/`HeatLevel`/`RechargeCounter`/`IsShooting`…——下游 mod 依赖这些名字）。
`getTag/getOrCreateTag` 返回**实时 tag（`CustomData#getUnsafe()`）**，以维持 1.20.1 的"就地修改"语义。**全局替换规则**把调用点改成了 `NbtHelper.*`（当前 `NbtHelper.` 共 **188 处引用**，见 §9 的统计命令）。

### 5.2 Framework 网络（`network/FrameworkMessageBridge.java` + `PacketHandler.java`）
0.5.5 的网络基于 MrCrayfish Framework。Framework 0.13 用 `StreamCodec` 取代了 `PlayMessage` 基类：
- **49 个消息类原样保留**（`network/message/` 下 22 个 C2S + 27 个 S2C），桥接层用反射把它们的 `decode(FriendlyByteBuf)/encode(X, FriendlyByteBuf)/handle(X, MessageContext)` 适配成 `(StreamCodec, BiConsumer)` 注册三元组。
- `PacketHandler` 由 `tools/gen_packet_handler.py` **生成**（49 条，方向按类名 C2S/S2C 推断）。改消息类后重跑该脚本。
- Framework 0.13 移除了 `registerLoginData` → 登录时改发 `S2CMessageUpdateGuns`（见 `NetworkGunManager.onPlayerLogin`）。
- Framework 的 `MessageContext.getPlayer()` 返回 **`Optional<Player>`**。

### 5.3 能力系统（`init/ModCapabilities.java` + `util/Caps.java`）
Forge 的 `ICapabilityProvider`/`LazyOptional` 全部消失。做法：
- 方块实体保留可空的 `getCapability(Object cap, Direction side)`（参数用 `Object` 是为了让原来的 `cap == Capabilities.X` 比较继续编译），由 `ModCapabilities` 按能力类型注册。
- 物品的匿名 `initCapabilities` → `createEnergyStorage(ItemStack)` 工厂 + `registerItem`。
- 调用点折叠成 `Caps.energyStored/ifPresent/of(...)`。

### 5.4 附魔改为数据驱动
1.21 附魔是**数据包注册项**。产物：
- `init/ModEnchantments.java` 现在是 15 个 **`ResourceKey<Enchantment>`** 常量（原来的 `DeferredRegister` 已删）。
- `data/scguns/enchantment/*.json` 由 `tools/gen_enchantments.py` 从原附魔类提取（rarity/槽位/最高等级/费用公式/互斥）；`#scguns:enchantable/guns`(141 项) 与 `bayonets` 标签同时生成。
- 原 `enchantment/` 包下 17 个类里 **16 个已删除**（附魔变成 JSON）；仅 `CorrodedEnchantment` 保留为**纯静态行为类**（含它的 `LivingIncomingDamageEvent` 钩子）。
- `util/ScEnchants.java`：1.21 的 `Enchantments.X` 是 `ResourceKey`，而 Forge 的 `getDamageBonus`/`getKnockbackBonus`/`getFireAspect`/`getSweepingDamageRatio`/`hasBindingCurse` 全被删除 → 按 **1.20.1 原公式**重建，等级直接从 `ENCHANTMENTS` 组件读（无需 registry access）。

### 5.5 其它已落地的结构性迁移
- **护甲材质** → `data/scguns/armor_material/*.json`（7 个）+ `ModArmorMaterials` 改为 `DeferredHolder<ArmorMaterial, ArmorMaterial>`；生成器 `tools/gen_armor_materials.py`。
- **唱片** → `data/scguns/jukebox_song/*.json` + `Item.Properties.jukeboxPlayable(...)`（`RecordItem` 已删）。
- **配方序列化器** → `common/recipe/LegacyRecipeCodec`（把 `MapLike` 转成 `JsonObject` 喂给**原有 `fromJson`**）+ `ScRecipeSerializer`，6 个序列化器改成 `extends ScRecipeSerializer<X>`。
- **配方输入** → `common/recipe/ContainerRecipeInput`（把机器的 `Container` 包成 1.21 的 `RecipeInput`）。
- **配方条件** → 4 个 `scguns:*_mod_loaded` 自定义条件（其类已在移植中删除）统一改写为 NeoForge 内建的 `neoforge:mod_loaded` + `modid`；数据包条件现已**全部可解析**（`tools/fix_conditions.py` + `tools/survey_conditions.py`）。
- **tick 事件** → `TickEvent.{Client,Player,Level,Server}TickEvent` + `Phase` 判断被折叠成 `.Pre/.Post`（`ClientTickEvent` 在 `client.event` 包）。
- **MobType / DistExecutor / ToolAction / Rarity.create / RenderUtils 等被删的 API** → `util/MobType`、`util/DistHelper`、`CombatHelper`、`util/Constants`（自定义稀有度映射到原版四档，仅有 tooltip 颜色差异）等兼容实现。

### 5.6 主动降级/放弃的兼容（**恢复入口都在 `compat/`**）

| 项 | 现状 | 恢复方式 |
|---|---|---|
| **JEI 界面** | 8 个类已从 git 恢复但**被 `build.gradle` 的 `exclude` 排除出编译**（本体优先） | 移除那 8 行 exclude → 按 JEI 19 迁移（`IRecipeCategory`/`IDrawable`/`RecipeIngredientRole` 包与注册方式都变了，`getAllRecipesFor` 返回 `RecipeHolder`） |
| `soul_fire_d`（灵魂火） | `setSoulFireOnEntity` 与 `FakeSoulFireBlock` 降级为普通点燃；`SoulFiredModCondition` 已删 | **jar 其实已经有了**：`准备的前置/soul-fire-d-neoforge-1.21-6.1.0.jar`（还没放进 `libs/`）。接进去后恢复 `FireManager` 分支 |
| `controllable`（手柄） | `ControllerHandler` 已改为桩（`init/isAiming/isShooting` 恒 false），`GunButtonBindings` 已删 | 加依赖 + 按 1.21 API 重写绑定 |
| `shouldersurfing` | 改成**全反射**（连 `Perspective` 枚举都 `Class.forName` 解析），**无需 jar** | 已可用，不必恢复 |
| `sculkhorde` | `SculkHordeEvents` 已删、注册调用已去 | 加依赖后恢复 |
| `valkyrienskies` | `ProjectileEntity` 的 `RaycastUtilsKt.clipIncludeShips` 分支桩化为原版 `world.clip` | 同上 |
| `Create` 背包罐 | **已接真实 `BacktankUtil`**（有 jar） | — |
| `farmersdelight`（农夫乐事） | 只剩配方 JSON 的 `neoforge:mod_loaded` 条件；`FarmersDelightModCondition` 已删 | **jar 其实已经有了**：`准备的前置/[农夫乐事] FarmersDelight-1.21.1-1.3.4.jar`（还没放进 `libs/`）。这个 mod 没有专门的 helper 类，接上去即可 |
| **4 个自定义配方条件** | `compat/{Create,FarmersDelight,IE,SoulFired}ModCondition` **已删**，原本都是 `ModList.get().isLoaded("<modid>")` | **已用 `tools/fix_conditions.py` 把全部改成 `neoforge:mod_loaded` + `modid`（语义等价、无损）**；重跑 `convert_resources.py` 后必须再跑一次它 |
| `backpacked`、`playerrevive`、`guardvillagers` | 仍缺 jar（`BackpackHelper`/`PlayerReviveHelper` 等已桩化） | 拿到 1.21.1 jar 丢进 `准备的前置/`，用同样的 `compileOnly + localRuntime` 接入 |

> `tools/survey_conditions.py` 可以随时核对数据包条件是否全部可解析（当前：`neoforge:mod_loaded` 428 + `neoforge:not` 6，**没有需要自定义序列化器的了**）。

### 5.7 已记录的语义偏差（可接受，写在这里以免被误当 bug）
- **配方 id**：1.21 的 `Recipe` 不再持有 id（由 `RecipeHolder` 承载），而数据包路径不再把 id 交给序列化器 → 经 codec 构造的配方拿到**占位 id**（`scguns:unknown`）。蓝图保存那块若要用真实 id，需改读 `RecipeHolder.id()`。
- **护甲耐久**：1.21 的 `ArmorMaterial` 只有单一 `durability` 基数（分部位倍率固定为原版 13/15/16/11），与 0.5.5 的 `BASE_DURABILITY{8,12,12,9}` 略有差异。
- **自定义稀有度**：11 个自定义稀有度映射到原版四档（`Constants.java` 有注释对照表），仅 tooltip 颜色不同。
- **paxel**：`ToolAction` 已删，当前的斧/铲交互行为需补 `DataComponents.TOOL`（`Tool.Rule`）才会生效。
- **燃料**：1.21.1 没有可查询的燃料表（`FuelValues` 是 1.21.2 才加的）→ `util/ScFuels` 先问 NeoForge 的 item 钩子，再回退到内置的原版燃料表 + 标签判定。
- **唱片比较器**：原代码给第三个唱片传了 20（超出 0–15），已按 15 夹紧。

## 6. 工具链（`tools/`，59 个文件）

### 6.1 一次性/生成器（改数据后重跑）
| 脚本 | 用途 |
|---|---|
| `convert_resources.py` | 从 **0.5.5 jar** 重放资源：`recipes→recipe`、`tags/items→tags/item`、`structures→structure`、`loot_tables→loot_table`、`forge→neoforge`、`data/forge/tags→data/c/tags`，配方 `result.item→result.id`，`forge:→neoforge:` 条件名。**⚠️ 跑完它之后必须再跑 `fix_conditions.py`**（它会把已删的自定义条件名重新写回来） |
| `fix_conditions.py` | 把 `scguns:{create,farmersdelight,ie,soul_fired}_mod_loaded` 换成 `neoforge:mod_loaded` + `modid`（4 个 `*ModCondition` 类已在移植中删除，留着这些条件会导致配方**加载失败**）。幂等 |
| `survey_conditions.py` | 扫描全部数据包，列出**需要自定义序列化器的条件类型**（期望输出 `(none - good)`）。只统计带 `type` 的加载条件，不会把战利品表的 `condition` 谓词算进来 |
| `gen_packet_handler.py` | 由 `network/message/` 生成 `PacketHandler` |
| `gen_enchantments.py` | 生成 15 个附魔 JSON + 附魔物品标签 |
| `gen_armor_materials.py` | 生成 7 个护甲材质 JSON 并重写 `ModArmorMaterials` |
| `fix_thirdparty_imports.py` | 按依赖 jar 的**真实类表**重映射 GeckoLib/Framework/Curios 导入（GeckoLib 4.6 去掉了 `core` 层级） |

### 6.2 日志分析器（每轮都用）
```powershell
python tools\analyze_errors.py build-logs\compile-109.txt --top 10        # 文件/类别排行
python tools\analyze_errors.py build-logs\compile-109.txt --symbols 20    # 缺失 API 排行
python tools\show_errors.py  build-logs\compile-109.txt "<正则>" 5 --file <文件名片段>
```
两个脚本都会**自动识别 UTF-16 日志**；`show_errors` 的匹配范围包含错误行**后面的上下文行**（`symbol:`/`location:` 都在那里）。

### 6.3 `port_rewrite*.py`（41 个，**幂等**，可重复运行）
这是本移植的主力：每轮"写规则 → 跑 → 编译 → 看日志 → 补规则/回滚"。
⚠️ 它们**都有 AUTHORED 白名单**（跳过 `util/NbtHelper`、`Caps`、`ScEnchants`、`network/*`、`init/ModCapabilities` 等我手写的文件），否则会把兼容层自己的 javadoc 也重写掉——**加新脚本时务必照抄这个白名单**。
⚠️ 新增规则后请**至少跑两次**验证幂等（第二次应为 0 改动）。

### 6.4 定点修复脚本
`repair_tooltips.py`、`repair_level_location.py`、`repair_recipe_holder.py` —— 都是"某次替换模板写错后的一次性回滚器"，可作模板参考。

### 6.5 加载期静态审计（编译通过之后才用得上，**都是 0 才算干净**）
编译清零并不意味着能加载：NeoForge 21.1 的加载期检查比 Forge 严格得多，本移植有 10 类问题
**只在实跑时才炸**。这七个脚本把这些检查变成静态可跑，避免"一次崩溃只暴露一个"的循环：

| 脚本 | 查什么 | 对应崩溃信息 |
|---|---|---|
| `audit_subscribers.py` | `@EventBusSubscriber` 注解所在的那个类**自己**有没有 handler、是否 static；并标出只跑客户端的 | `class X has no @SubscribeEvent methods, but register was called anyway`（`RangeFinderItem` 就是这类：注解同时挂在**外层类**和**内部 handler 类**上，按文件 grep 查不出来） |
| `audit_bus_registrations.py` | 显式 `EVENT_BUS.register(...)`：无 handler / static-instance 不符 / 重复注册；**实参是变量时反解其声明类型，解不出来一律报错而不是跳过** | 同上；`Expected @SubscribeEvent method ... to NOT be static`（`ModelOverrides.register(model)` 就是被旧版脚本静默跳过的） |
| `audit_dist_safety.py` | 客户端类型（`net.minecraft.client.*`）出现在会被专用服务器扫描的方法签名里 | `Attempted to load class net/minecraft/client/Minecraft for invalid dist DEDICATED_SERVER` |
| `audit_abstract_events.py` | 监听**抽象**事件类（须改成 `.Pre`/`.Post`） | `Cannot register listeners for abstract class ...` |
| `audit_unsafe_casts.py` | 对 `event.getX()` 的强制转换没有 `instanceof` 守卫 | `ClassCastException: ItemEntity cannot be cast to LivingEntity`（`EntityTickEvent` 对**所有**实体触发，不能当成 Player/LivingEntity） |
| `audit_mixins.py` | 15 个 mixin 的 `@Mixin` 目标与 0.5.5 是否一致、`method=` 注入点在 1.21.1 源码里是否还存在 | 客户端启动期 mixin 注入失败 |
| `fix_loot_functions.py` | 战利品函数 id 是否仍存在于 1.21.1（`--write` 可修） | 数据包解析失败 → 该实体**完全不掉落** |

另外几个本轮新增且常用的：
- `javac_check.py` —— 用 JDK 21 javac 直接编译全部源文件，**秒级**（gradle 一轮要几十秒到几分钟）。
- `round.py` / `dump_errors.py` / `group_errors.py` / `root_causes.py` —— 编译轮的日志汇总、按目录分组、按符号找跨包根因。
- `verify_message_shapes.py` —— 静态验证 48 个网络消息类都满足 `FrameworkMessageBridge` 的反射契约
  （它每类只解析一次，不满足就在启动时抛异常）。

> **专用服务器通过 ≠ 客户端通过**。客户端独有的崩溃源（`value = {Dist.CLIENT}` 的订阅者、
> mixin、渲染/模型注册）在 `runServer` 里根本不会执行——`RangeFinderItem` 就是这么漏过去的。
> 改动客户端相关代码后，**至少要重跑 `audit_subscribers.py` 与 `audit_mixins.py`**。

## 7. 血泪教训（**强烈建议先读这一节再动手**）

这个会话里**每一次错误数异常暴跌都对应一次编排事故**：

| 事故 | 症状 | 根因 | 教训 |
|---|---|---|---|
| 误删 16 个界面的 `render` | 错误数 576 → 42 | 规则按**方法签名**匹配，而 `AbstractSelectionList.render` 与 `AbstractContainerScreen.render` 签名相同 | **按签名删除方法前，先确认目标类的继承链**；删除类成员永远优先用"平衡括号 + 类上下文"，不要用裸签名 |
| 替换串里写了 `\1` 却没用 `re.sub` | 16 个文件被写入字面量 `\1`，编译中断（错误数 168） | 手工拼接替换串时误用正则反向引用 | 自写的替换函数用 **`@N`/`@A` 这类自定义占位符**，不要用 `\1` |
| `LevelLocation.create(...)` 模板多一个 `)` | 16 处调用提前闭合 | 模板把参数列表也闭合了 | 改调用形态时，模板要么保留原参数列表，要么用平衡括号显式重建 |
| 又一个 `\1` 字面量（recipe holder） | 14 个文件 | 同上 | 同上 |
| 签名凭记忆写错（`appendHoverText` 的 `Consumer` vs `List`） | 19 处返工 | 1.21.1 实际仍是 `List<Component>`，只把 `Level` 换成 `Item.TooltipContext` | **改签名前先 `javap`**：`javap -cp build/moddev/artifacts/neoforge-21.1.249-merged.jar <类>`，这是本机的 API 真值来源 |
| 幂等性缺失 | 第二次运行规则时自伤（如 `Caps.ifPresent(Caps, ...)`、`RegistryRegistryFriendlyByteBuf`） | 规则匹配到了自己刚生成的文本 | 每加一条规则，**跑两次**确认第二次 0 改动 |

其它实战要点：
- **PowerShell 里的内联 python 极容易因引号/转义出错**（本会话踩了 5 次以上）。**写脚本文件再执行**，不要用 here-string 传正则。
- 反编译产物里有**反编译痕迹**需要清理：record 里的 `super();`、多余的 `@NotNull`、`Mode.QUAADS` 拼写、裸 `List` 强转（导致 lambda 目标类型退化成 `Object`）。
- **级联错误**很常见：先找"基类/接口改名"这类根因（例：`AbstractGlassBlock→TransparentBlock` 一处改动清掉 20 个错误），不要逐个修调用点。
- 有些 API 是**混合**的：`SoundEvents` 只有唱片相关常量是 `Holder<SoundEvent>`，`Attributes`/`MobEffects` 则全部是 Holder。**别一刀切加 `.value()`**（本会话因此把 940 涨到 962，回滚后按编译日志逐点加才对）。

## 8. 下一步（建议顺序）

编译与加载这两关已经过了（见 §2）。按价值排序：

1. **客户端启动冒烟测试**（唯一没做过的验证）：`build/libs/scguns-0.5.5.1.jar` 连同
   `libs/` 里的三个必需依赖（`framework-neoforge-1.21.1-0.13.11`、`geckolib-neoforge-1.21.1-4.9.3`、
   `curios-neoforge-9.5.1+1.21.1`）放进客户端 `mods/`。重点看：mixin 是否注入、GeckoLib 模型是否加载、
   JEI 的 6 个分类是否出现、HUD 是否正常。**注意**：客户端与专用服务器的注册路径不同，
   有三处重复事件注册（§10.4）在客户端会更明显。
2. 接上 `准备的前置/` 里现成的两个 jar：`soul-fire-d-neoforge-1.21-6.1.0.jar`、
   `FarmersDelight-1.21.1-1.3.4.jar`（去掉中文前缀放进 `libs/` 即自动生效，`build.gradle` 的
   fileTree 会接上），然后恢复 `FireManager` 的灵魂火分支（§5.6）。
3. 收 §10 的偏差项，尤其 **§10.1 护甲贴图**（8 种护甲现在都会显示紫黑格）与
   **§10.2 抢夺加成丢失**（3 种弹药的玩法缺失）。
4. 玩法回归：开火/装填/热度、蓝图、护甲、附魔、机器配方、炮塔、外骨骼（注意 §5.7 的语义偏差）。

### 8.1 这一轮"从 496 到 0"是怎么做的（可复用的方法论）

- **别逐个修调用点，先找根因**。496 个错误里最高频的符号只占 11 个，所以用了 11 个并行
  子代理按**目录切分**（互不重叠）分头清，每个都拿到 API 真值路径与设计约束。
- **关键字面 API 真值**：`.refs/nf-src` 是解出来的 NeoForge 21.1.249 **源码**（7106 个 .java），
  比 `javap` 猜签名可靠得多。提取方式见 §9。
- **"能不能编译"之外还有"能不能加载"**。编译清零后，专用服务器实测又暴露了 7 类
  **编译期看不出来**的 1.21 严格化问题（§10.5 列出了全部）。这类问题必须靠实跑或静态审计发现，
  不要以为编译通过就结束了。
- **写规则脚本要带自测**。本轮 6 个 `port_rewrite4x.py` 里有两个的自测当场抓出了规则自身的 bug
  （捕获组用错、注释里出现 `@SubscribeEvent` 导致 import 删不掉），避免了静默改坏上百个文件。

## 9. 每轮的验证清单

**秒级全量编译**（比 gradle 快一个数量级，本轮迭代的主力）：

```powershell
python tools\javac_check.py           # 用 JDK 21 javac 直接编译全部 989 个文件
# 依赖 build-logs\compile-classpath.txt，由下面这条一次性生成：
$env:JAVA_HOME='D:\jdk-21.0.3'
cmd /c "gradlew.bat printCompileClasspath --console=plain"
```

**四项静态审计**（都应为 0；它们覆盖的是编译通过但*加载*会崩的问题）：

```powershell
python tools\audit_bus_registrations.py   # 事件总线注册：无 handler / static-instance 不符 / 重复注册
python tools\audit_dist_safety.py         # 客户端类型泄漏进服务端会被扫描的方法签名
python tools\audit_abstract_events.py     # 监听抽象事件类（须改 Pre/Post）
python tools\fix_loot_functions.py        # 战利品函数 id 体检（1.21 删掉了 looting_enchant / set_nbt）
```

**出 jar 与实测加载**：

```powershell
cmd /c "gradlew.bat clean build --console=plain > build-logs\build-NN.txt 2>&1"
cmd /c "gradlew.bat runServer --console=plain > build-logs\server-NN.txt 2>&1"   # 需 run\eula.txt
# 判定：日志出现 "Done (…)! For help, type" = 通过；
#       并确认 "Couldn't parse element" 计数为 0、无 scguns 相关 ERROR/FATAL。
# ⚠️ runServer 会一直挂着：先确认上一轮的服务端进程已退出（否则 run\logs\latest.log 被占用，
#    下一次启动会在启动阶段报 IOException）。
```

**规则脚本一律要求幂等**（再跑一次为 0 改动），且**新增脚本必须带 `--selftest`**。

文档里所有数字都可自证，交接时若对不上就用这组命令刷新：

```powershell
python tools\javac_check.py | Select-Object -First 5              # 编译错误数 + 文件数
python tools\analyze_errors.py build-logs\compile-109.txt --top 0  # 接手时的基线（496）
(Get-ChildItem src\main\java -Recurse -File -Filter *.java).Count  # 源码数（989）
(Get-ChildItem src\main\resources -Recurse -File).Count            # 资源数（6405）
(Get-ChildItem tools -File).Count                                  # 工具数
```

**API 真值优先用源码**（比 javap 更完整，解包命令一次性）：

```powershell
Copy-Item build\moddev\artifacts\neoforge-21.1.249-sources.jar .refs\nf-src.zip -Force
Expand-Archive -Path .refs\nf-src.zip -DestinationPath .refs\nf-src -Force
# 之后直接读 .refs\nf-src\net\minecraft\...\X.java；.refs/ 已加入 .gitignore
javap -cp build\moddev\artifacts\neoforge-21.1.249-merged.jar net.minecraft.world.item.Item
# 全量类名表缓存在 build-logs\jarclasses.txt
```

## 10. 已知偏差与遗留问题（编译/加载都过了，但这些还没做）

按影响排序。**每一条都注明了是"0.5.5 本来就有的"还是"移植造成的"**，别误当成新 bug。

### 10.1 护甲材质原本**根本没注册**（已修）— 移植造成，判断错了版本
**1.21.1 的 `ArmorMaterial` 仍是 built-in registry**（`BuiltInRegistries.ARMOR_MATERIAL =
registerSimple(Registries.ARMOR_MATERIAL, ArmorMaterials::bootstrap)`），**数据驱动是 1.21.2 才有**。
移植按 1.21.2 的做法写成 `DeferredHolder.create(Registries.ARMOR_MATERIAL, id)`（**永不绑定**）
＋ `data/scguns/armor_material/*.json`（1.21.1 **从不读**），于是任何读取都抛
`NullPointerException: Trying to access unbound value: ... scguns:treated_brass`。

已按 1.21.1 正确做法重写 `init/ModArmorMaterials.java`：真 `DeferredRegister` + 在
`ScorchedGuns` 里注册；数值取自 0.5.5 枚举，**按槽位显式传参**（顺序是
`HELMET, CHESTPLATE, LEGGINGS, BOOTS`，即 `ArmorItem.Type` 的 ordinal 序；这是本轮
用"胸甲 ≥ 护腿"这一原版恒定规律 + 原版 `BASE_DURABILITY` 比值两条证据定的，
参照工程用的是转置映射，会把 3 套的护腿画得比胸甲厚）。失效的 8 个 JSON 已删除。

顺带修了耐久：**1.21.1 的 `ArmorItem` 不再从材质取耐久**，必须
`Properties.durability(type.getDurability(factor))`（原版写法），移植用 `new Properties()`
导致护甲耐久为 0。`tools/port_rewrite49.py` 给 35 处护甲物品补上了。

> **护甲贴图不用动**：1.21.1 的 `ArmorMaterial.Layer.resolveTexture()` 生成的是
> `textures/models/armor/<name>_layer_1.png` / `_layer_2.png`（`textures/entity/equipment/...`
> 是 1.21.2 的路径）。而本 mod 的护甲由自己的 GeckoLib 渲染器绘制，贴图放在
> `assets/scguns/textures/armor/*.png` —— **与参照工程 1.21.1 移植完全一致**，保持原样。

### 10.2 三种弹药的抢夺加成丢失（玩法缺失）— API 移除，移植无法保留
0.5.5 里 `BeowulfProjectileEntity`（+3）、`GibbsRoundProjectileEntity`（+4）、
`TurretProjectileEntity`（+4，仅 gibbs 弹）各有一个 `@SubscribeEvent onLootingLevel(LootingLevelEvent)`。
**NeoForge 21.1.249 没有 `LootingLevelEvent`**，且它的 `LivingDropsEvent` 已不再携带可修改的
looting level（javadoc 里还写着 `#lootingLevel`，但字段已删）。
处理：`tools/port_rewrite47.py` 删掉了悬空的注册（保留注册会让加载崩），
**加成本身没有恢复**。要恢复需改用全局战利品修饰器（global loot modifier）。

### 10.3 画作 `the_collective` 的贴图是 0 字节 — **0.5.5 本来就有**
`assets/scguns/textures/painting/the_collective.png` 是**空文件**（0 字节），
已核对 0.5.5 原 jar：**原版就是这个样子**。本轮只修了尺寸单位问题：
1.21 的 `PaintingVariant` width/height 是**方块数**且限制 1..16，而 0.5.5 的 JSON 写的是
`64/32`（像素），会让整个 registry 加载失败（本轮实测崩在
`Value must be within range [1;16]: 32`）。已改为 `4/2`（贴图本就该是 64×32 px）。

### 10.4 三处重复的事件注册（处理器会跑多次）— **0.5.5 本来就有**
`audit_bus_registrations.py` 会列出：
- `PlayerModelHandler` 在 `ClientHandler` 里注册了**两次**（0.5.5 原码同样两行）；
- `BulletTrailRenderingHandler` / `HUDRenderHandler` 在 `ScorchedGuns` 与 `ClientHandler` 各注册一次
  （且两者还带 `@EventBusSubscriber(value = Dist.CLIENT)` 自动注册）。
NeoForge 不报错，且**与 0.5.5 行为一致**，所以本轮刻意保留未改。若客户端实测发现 HUD/模型
重复绘制，就从这里入手。

### 10.5 容器类方块实体没有注册 `ItemHandler.BLOCK` — 移植造成（福雷行为差异）
`init/ModCapabilities` 没有给 `AMMO_MODULE` / `AMMO_BOX` / `SHELL_CATCHER_MODULE`
（都是 `RandomizableContainerBlockEntity`）注册物品能力。Forge 会自动为任何 `Container`
方块实体暴露 `ITEM_HANDLER`，NeoForge 只认显式注册，**所以漏斗/管道拉不走弹药箱里的东西**。
代码内路径已被 `Caps.itemHandler(...)` 的 `Container` 回退覆盖，但外部（漏斗、其他 mod）不行。
补法（这三个类**没有** `getCapability(Object, Direction)`，不能用那个泛型助手）：
```java
event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
        ModBlockEntities.AMMO_BOX.get(), (container, side) -> new InvWrapper(container));
```

### 10.6 客户端未做启动冒烟测试 — 验证空白
无显示环境。已用 §6.5 的三项审计覆盖客户端侧的机械性失败，但 mixin 注入、GeckoLib 渲染、
JEI 分类实际注册**只能在客户端实测确认**。

### 10.7 其它已知语义偏差
见 §5.7（配方 id、稀有度、paxel、燃料、唱片比较器）与
`world/ViventrumEntity`、`TheMerchantEntity` 的存档格式变化（其 worker 报告里有细节：
旧版 `Offers` NBT 与 `buy` 为 `ItemStack` 的老存档不再可读，会回退到重新初始化交易）。
护甲耐久已在 §10.1 修复（1.21.1 改为由物品自身 `Properties.durability` 提供）。

**另有 1 处"按玩家要求有意偏离 0.5.5"**：Sulfurhead 原本只受玩家伤害，玩家要求去掉
（§23.5），现与普通怪物一致 —— 这不是移植漏洞，回滚方法写在同节。

### 10.8 20 处 `event.setCanceled(true)` 被丢（已修）— 移植造成，**影响面很大**
0.5.5 全树有 **24** 处 `setCanceled(true)`，移植版一度只剩 **2** 处（`GunMobFriendlyFire` 的），
即 **20 处事件取消被某个重写脚本删掉了**。取消丢失意味着原本被抑制的行为全部恢复：

- `GunEventBus`(10)：枪快损坏 / `reload_stop` 动画中 / 正在装填 / 举盾且单手持枪 /
  `DrawnTick < 15`（抽枪未完成）/ 能量枪没电 / 空气枪没气 / 水下枪型不符 /
  枪损到 `maxDamage-1` / 卡壳判定 —— **这些本该阻止开火的判定全部失效**。
- `ShootingHandler`(4)：持枪攻击/单手+副手剑/副手分支/对着可交互方块开镜时的输入取消。
- `CrosshairHandler`(2)：ADS 时与自定义准心时抑制原版准心。
- `ExoSuitFlightHandler`(2)：落地保护窗口内、以及喷气背包下降时的摔落伤害取消。
- `GunRenderingHandler`(2)：**第一人称手部渲染未取消** —— 用户实测表现为
  "枪渲染两次 + 多出一条模型手臂"（原版会额外画出玩家手臂与手持物品）。

处理：以 0.5.5 逐处恢复（含原有的 `return;`），核验过每个事件在 1.21.1 中确实
`implements ICancellableEvent`。现在计数 22 = 18 恢复 + 2 本就在 + 2 无关。
**两处不改**：`DishesEventHandler` / `RockPickupEventHandler` 的 `ItemEntityPickupEvent.Pre`
在 1.21.1 **不是**可取消事件，只有 `setCanPickup(TriState.FALSE)`，移植写法正确。

### 10.9 `ItemRenderer#render` 的分派规则，以及那个 `NONE` 回退渲染

> **修订记录**：本节最初把"GUI 里多出手臂 / 枪画两遍"归因于下面这个 `NONE` 回退渲染。
> 后续实测与源码核对证明**归因错误**，已改正。真正的两处原因见 §10.9.2。保留本节是因为
> 其中关于 `ItemRenderer` 分派规则的事实是验证过的，而且 §10.9.3 的隐患仍然存在。

#### 10.9.1 已验证的机制

`GunRenderingHandler.renderGun` 结尾（与 0.5.5 逐字一致）会调

```java
Minecraft.getInstance().getItemRenderer().render(stack, ItemDisplayContext.NONE, false, poseStack, ...);
```

**1.21.1 的 `ItemRenderer#render` 只在 `!model.isCustomRenderer()` 时才自己画模型；否则直接把
绘制交给该物品的 BEWLR，并且原样传下去同一个 display 上下文**：

- `.refs/nf-src/.../entity/ItemRenderer.java` 第 125 行 `if (!p_model.isCustomRenderer() && ...)`，
  第 155-157 行 `else { IClientItemExtensions.of(itemStack).getCustomRenderer().renderByItem(itemStack, displayContext, ...); }`
- 枪械物品模型是 `"parent": "builtin/entity"`，而 `BuiltInModel.isCustomRenderer()` **恒为 true**
  （`.refs/nf-src/.../model/BuiltInModel.java` 第 51 行）。

所以"BEWLR 会被带着 `NONE` 调用"这一句本身是事实。

#### 10.9.2 但它不是那两个症状的原因（原结论已推翻）

物品栏（GUI）里那条路径**根本不经过 `renderGun`**：动画枪的 BEWLR 是 GeckoLib 的
`AnimatedGunRenderer`（`item/animated/AnimatedGunItem.java` 第 1047-1053 行
`getCustomRenderer()`），不是 `GunItemStackRenderer`。而 `AnimatedGunRenderer.renderByItem`
**从不调用 `renderGun`/`renderWeapon`/`ItemRenderer.render`**（全文只用到
`getItemRenderer().getModel(...)`），因此 GUI 只是
`render(GUI) → renderByItem(GUI)` 一次，没有二次进入。`GunItemStackRenderer` 只服务于
非动画的 `GunItem`。

用户实测的三个症状真正的原因是：

1. **枪渲染两次**：`GunRenderingHandler.onRenderOverlay` 丢了两处 `event.setCanceled(true)`
   —— 见 §10.8。原版那条手部渲染没被取消，于是原版手臂 + 手持物品又画了一遍。
2. **物品栏里多出一条手臂 / 手臂不是玩家皮肤 / 未安装的配件照样渲染**：
   `AnimatedGunRenderer.renderRecursively` 的形参仍是 0.5.5 时代的
   `float red, float green, float blue, float alpha`，而 GeckoLib 4.6+ 把这个钩子改成了
   单个 `int renderColor`。**签名不匹配 → 这个 `@Override` 静默失效 → 整个方法成了死代码**，
   GeckoLib 的默认实现接管：枪模型自带的 `left_arm`/`right_arm` 骨骼在**所有** display 上下文
   都被画出来（GUI 里那条多余手臂），骨骼可见性判断（含配件骨骼与 arm 骨骼的隐藏）全部失效，
   而唯一会画玩家皮肤手臂的 `renderPlayerArms` 也从不执行。

`javac` 无法发现第 2 类问题（签名漂移不是编译错误），所以**凡是"本该覆写父类/接口"的方法
都必须带 `@Override`**，让编译器来保证。这是本轮最有价值的教训。

#### 10.9.3 保留 `NONE` 回退渲染的理由与遗留隐患

**保留**：对动画枪来说 `renderGun` 里第二个 `if`
（`!(stack.getItem() instanceof AnimatedGunItem) || display != 第三人称…`）在第一人称为真，
于是 `render(NONE)` → `renderByItem(NONE)` → `AnimatedGunRenderer` 把 `NONE` + 第一人称相机
映射成 `FIRST_PERSON_RIGHT_HAND` → 画出枪。**这是第一人称唯一画出枪的路径**
（同一个 `onRenderOverlay` 已经把原版那遍取消了），且参照工程
`E:\mod\SG2-1.21\ScorchedGunsNeoforge-main` 在同一位置的写法完全一致。删掉它第一人称就没枪了。

**隐患（当前不可达）**：若某个附属模组注册一个**非动画**的 `new GunItem(...)`，它的 BEWLR 是
`GunItemStackRenderer`，于是会
`renderByItem(NONE) → renderWeapon(NONE) → renderGun(NONE) → render(NONE) → renderByItem(NONE) → …`
**无限递归 → StackOverflowError**。本仓库当前**不可达**：全源码树 `new GunItem(` 匹配数为 0
（0.5.5 与本移植注册的枪全是 `AnimatedGunItem`），`GunItem` 虽不是抽象类但无人直接实例化。
真要修就在 `renderGun` 里加一个重入标志（进入时置位、绘制完清位，标志为真时直接 return）。

---

# 11. 数据层（`data/**/*.json`）—— 编译和服务端启动都查不出来的一整层

这一节是**新发现的一整类缺陷**：`gradlew build` 通过、`runServer` 打出 `Done`、
所有静态审计为 0，但**数据文件在加载时被静默丢弃**或**条件判断整个失效**。
入口是服务端日志里的 `Parsing error loading recipe <id>` 行——

> **每轮都要数这一行。** 它不是警告，是一条数据条目被丢掉。
> `python tools\group_recipe_errors.py build-logs\server-NN.txt` 分组，`tools\list_recipe_errors.py` 逐条列出。

本轮把 **206 条被拒 → 30 条**（剩下 30 条全是可选跨模组配方，另见 §11.5）。

## 11.1 `"conditions"` 这个键名在 1.21.1 **根本不生效**（影响最大，已修）

**432 处**条件判断全部是空转的。NeoForge 读取条件用的字面键是 `neoforge:conditions`：

- `ConditionalOps.DEFAULT_CONDITIONS_KEY = "neoforge:conditions"`
  （`.refs/nf-src/.../common/conditions/ConditionalOps.java:54`），
  `ConditionalDecoder` 就是拿这个键去 `inputMap.get(...)`（同文件 205 行）。
- `RecipeManager` **先**解 `Recipe.CONDITIONAL_CODEC`、**再**解配方本体
  （`.refs/nf-src/.../world/item/crafting/RecipeManager.java:60`），条件不满足就
  `Skipping loading recipe ... as its conditions were not met`（67 行）静默跳过。

本移植**全部**写成 `"conditions"`（1925 个数据文件里 708 个含该键），于是
"仅当加载了 create / mekanism / createoreexcavation / immersiveengineering 时才注册"
这类门禁**一条都没生效**：模组不在时配方照样解析、引用不存在的物品、然后报错。

**反例证据**：`data/scguns/recipe/create/charging/charged_amethyst_shard.json` 明明写了
`conditions: [{mod_loaded: createaddition}]`，日志里仍然报
`Unknown registry key ... createaddition:charging`——说明该条件没有被读取。

**判据（关键，别搞反）**：只把**数组元素全都带 `"type"` 键**的 `conditions`改名。
原版战利品条件用的是 `"condition"`（如 `{"condition": "neoforge:loot_table_id"}`），
**必须保持 `"conditions"` 不变**。全树 432 个是前者、198 个是后者，**0 个混用**。

脚本：`tools/fix_data_1211.py`（含 `--selftest`）。参照工程用 `neoforge:conditions` 419 处，可交叉验证。

## 11.2 标签目录是 1.20.1 的 `tags/items` + `tags/blocks`（已修）

1.21 的物品/方块标签目录是**单数** `tags/item`、`tags/block`：

- `Registries.elementsDirPath()` 返回 `"tags/" + prefixNamespace(key.location())`
  （`.refs/nf-src/.../core/registries/Registries.java:256`），物品注册表 key 是 `minecraft:item`。

本移植 `data/c/tags/items/**` 与 `data/c/tags/blocks/**` 是 **1.20.1 Forge 的布局**，
于是**自己定义的整套 `c:` 约定标签一个都没加载**。已 `git mv` 到单数目录。
（注意 `tags/worldgen/biome` 本来就是对的；`scguns/tags/item`、`minecraft/tags/item` 等也本来就是单数。）

## 11.3 `neoforge:` 标签命名空间在 1.21 已经不存在（已修，105 处）

NeoForge 21.1.249 提供 **243 个 `data/c/tags/item/...`**，`data/neoforge/tags/item/` 只有 **2 个**；
Mekanism 提供 164 个 `c:`、**0 个 `neoforge:`**。即约定命名空间已从 `neoforge:`/`forge:` 变成 `c:`。

本移植配方里 105 处写 `neoforge:dusts/...`、`neoforge:ingots/...` 等，**指向没人定义的标签**：
这类配方**能加载、但永远做不出来**（材料为空），服务端日志**一声不响**。

脚本 `tools/fix_tag_refs.py`（含 `--selftest`），表是**逐条核对过的**，不是无脑换前缀——
有两条连路径也改了：`neoforge:cobblestone/normal` → `c:cobblestones/normal`、
`.../deepslate` → `c:cobblestones/deepslate`。脚本会把**任何未映射的遗留引用**报出来并非零退出，
所以不会漏。`minecraft:` 标签是原版标签，**不要动**。

## 11.4 Create 6 改了两个字段名（已修，164 个文件）

| 旧 (Create 5 / 1.20.1) | 新 (Create 6 / 1.21.1) |
|---|---|
| `acceptMirrored` | `accept_mirrored` |
| `transitionalItem` | `transitional_item` |
| `result`/`results`/`transitional_item` 里的 `{"item": …}` | `{"id": …}` |

`key`/`ingredient`/`ingredients` 里的 `{"item": …}` 是**材料编解码器**，**保持 `item`**。

**这 138 个 `create:mechanical_crafting` 就是全部枪械的制作配方**，26 个
`create:sequenced_assembly` 是弹药生产线——本轮修好的正是这条主线。

另：`create:filling` 的流体材料要写成
`{"type": "neoforge:single", "amount": n, "fluid": "…"}`（原来少了 `type`）。

## 11.5 还没修完的 30 条：全是**可选跨模组**配方

都是 1.21 的模组改了自家 JSON 字段名，需要按各模组 jar 反推 schema，与本移植无关：

| 类别 | 条数 | 症状 |
|---|---|---|
| Mekanism 10.7 | 15 | 要 `chemical_input` / `item_input` / `per_tick_usage` / `extra_input`，物品栈要 `id` |
| CreateOreExcavation 1.6.8 | 8 | `ore_vein_type` 要 `amountMultiplierMax`/`Min` + `finite`；`drilling` 要 `veinId`（现在是 `vein_id`） |
| ImmersiveEngineering 12.4.2 | 7 | `result.base_ingredient` → 要 `basePredicate` |

（8 条 CreateOreExcavation 在 `data/createoreexcavation/recipe/...`，不在 `scguns` 命名空间下。）

这些配方**只在这些模组装了的时候才有意义**；修好 §11.1 之后，模组不在时它们会被正确跳过，
不再刷错误日志。**注意**：这 3 个模组在用户的测试实例里都装了，所以这 30 条会真实出现。

## 11.6 一条真实的内容 bug：`jr_wristbreaker` 定义了没用到的键

Create 6 把"`key` 里定义了但 `pattern` 里没用到"从警告升级成**报错**：
`Key defines symbols that aren't used in pattern: {C}`。该配方于是整条被丢弃。
处理：删掉没用到的 `C`（`minecraft:planks`）——**不改动 pattern，即不改变配方语义**。
（**参照工程有同样的 bug**，说明这处不能拿它当依据。`category` 则相反：见下。）

## 11.7 `category` 的值**不用改**——别被 `misc` 误导

参照工程把 `category` 都写成 `misc`，但 1.21.1 的 `CraftingBookCategory` 只有
`building/redstone/equipment/misc`（`.refs/nf-src/.../CraftingBookCategory.java:12-15`），
而本移植有 `ammo`(13) / `throwable`(9) / `dirt`(2) 这些"非法"值，**却没有任何报错**。
原因：`CODEC = StringRepresentable.fromEnum(...)`，`EnumCodec` 有
`byName(name, defaultValue)` 这种**宽容查找**（`StringRepresentable.java:84`），未知值回落到默认。

**结论**：这是查出来的**假阳性**，不要去"修"。只有顺路改的那两个
`organic_compost_guano*.json`（它们的条件本来也写错了模组，见 §11.8）顺手改成 `misc`。

## 11.8 条件写错模组、以及 `neoforge:single`

- `organic_compost_guano.json` / `_2.json`：材料与产物都来自 **farmersdelight**，
  条件却写成 `modid: create`。已改为 `farmersdelight`（参照工程一致）。
- `create/diamond_steel_filling.json`：条件只门禁 `create`，却用
  `create_enchantment_industry:experience` 流体。已**追加第二个条件**
  （条件数组是"全部满足"，可以并列），并把流体改成 `neoforge:single` 写法。

## 11.9 这一节新增的工具（都在 `tools/`，可重复运行）

| 工具 | 作用 |
|---|---|
| `group_recipe_errors.py` | 把服务端日志的拒收条目**按原因分组** |
| `list_recipe_errors.py` | 逐条打印 `<id> <错误摘要>` |
| `fix_data_1211.py` | §11.1 + §11.4 的迁移（幂等，`--selftest`） |
| `fix_tag_refs.py` | §11.3 的标签重映射（幂等，`--selftest`，未映射即非零退出） |
| `scan_data_latent.py` | 扫 `category` 与遗留标签命名空间（找**日志看不见**的） |
| `scan_tag_namespaces.py` | 标签"引用了但没人定义"的全量比对 |
| `c_tags.py` | 列出各 jar 实际提供的 `c:` 标签，用来查现代名称 |
| `_probe_roundtrip.py` | 一次性探针：确认 JSON 往返是否保格式（370/387 保真，可放心做结构化改写） |

## 11.10 标签加载失败（已修，6 个坏标签 / 4 个根因）

服务端日志里 `Couldn't load tag <tag> as it is missing following references:` 意味着
**整个标签没加载**——依赖它的配方材料/附魔表/modd 兼容项都会静默失效。
`tools/list_tag_failures.py` 按根因分组（一条缺失引用常常连带打坏好几个标签）。

| 根因 | 后果 | 修法 |
|---|---|---|
| `scguns:anthralite_knife` | 打坏 `c:tools`、`c:tools/knives`、`farmersdelight:tools/knives`，再连带打坏 IE 的 `immersiveengineering:toolbox/tools` | 该物品**只在装了 FarmersDelight 时才注册**（`ModItems.registerItems()` 里 `if (ScorchedGuns.farmersDelightLoaded)` + 反射构造），所以引用要写成 `{"id": …, "required": false}` |
| `#c:glass`（在**方块**标签里） | 打坏 `scguns:fragile` | 1.21 约定里没有 `c:glass` 方块标签；方块侧是 **`c:glass_blocks`**（`c:glass_panes` 本来就有） |
| `minecraft:grass` | 打坏 `scguns:weak_compost` | 1.20.5 起物品已改名，1.21 是 **`minecraft:short_grass`**（`javap Items` 可见，`GRASS` 常量已不存在） |

**通用教训**：凡是**条件注册**的物品（`farmersDelightLoaded` / `createIronWorksLoaded` 分支里的
`ANTHRALITE_KNIFE` / `ANTHRALITE_HAMMER` / `ANTHRALITE_PAXEL`），在标签/配方里引用都必须
`required: false`，否则模组缺失时会打坏整个标签。

---

# 12. 「本该覆写但签名漂移」——编译器和六个审计都查不出来的第二类 bug

§10.9 那个 `renderRecursively` 不是孤例：**同一类 bug 还有 17 处**，而且其中 14 处
让**所有物品的 tooltip 都不显示**。

## 12.1 判据

`javac` **不会**报错，因为签名不匹配的方法只是一个**普通重载**——合法、能编译、永远不被调用。
唯一的编译期证明是 `@Override`。

新增审计 **`tools/audit_override_drift.py`**（含 `--selftest`）：
它**解析 `.class` 字节码**（自带常量池解析器，不依赖源码），沿父类链 + 接口闭包找
**同名同参数个数但描述符不同**的方法；`@Override` 是 `SOURCE` 保留、字节码里没有，
所以从源码文本读。

## 12.2 查出来的 17 处（已修）

| 家族 | 数量 | 我们的签名 | 1.21.1 的签名 | 后果 |
|---|---|---|---|---|
| `appendHoverText` | **19 个文件**（审计只报 14 个"根"，见下） | `(ItemStack, TooltipContext, TooltipFlag, List<Component>)` | `(ItemStack, TooltipContext, List<Component>, TooltipFlag)` | **所有物品 tooltip 完全不显示** |
| `getCloneItemStack` | 3 | `(BlockGetter, BlockPos, BlockState)` | `(LevelReader, BlockPos, BlockState)` | `LevelReader extends BlockGetter`，方向反了无法覆写 → 掉的是原版 Clone 物品而不是硫磺粉 |

两个签名都用 `javap` 对着 `build/moddev/artifacts/neoforge-21.1.249-merged.jar` 核对过。

**为什么审计只报 14 个而实际要改 19 个**：子类（`EnergyGunItem extends GunItem`、
`AnimatedEnergyGunItem`…）声明的是**和父类一模一样的错签名**，所以它**确实覆写**了父类那个
错方法（`@Override` 合法），看上去干净。**只有漂移家族的"根"会被报出来。**
所以修的时候必须**顺着继承链把整个家族一起改**，否则改完父类、子类就变成新的孤儿。

脚本：**`tools/fix_override_drift.py`**（幂等，`--selftest`）。它同时给这些方法补上
`@Override`——**这是本轮最有价值的产出：以后同类漂移会变成编译错误，而不是静默失效。**
改完 `javac_check` 仍为 0 错误，这本身就证明了新加的全部 `@Override` 都是真覆写。

## 12.3 剩下 276 处 "erasure-only"

这些是**真的在覆写**、只是没写 `@Override` 的方法（泛型父类擦除，javac 会自动生成 bridge，
已用 `javap` 确认）。审计单独分桶、不影响退出码。**它们是同一类风险的温床**：
哪天父类签名变了，这 276 个方法会和 `renderRecursively` 一样静默失效。
建议后续逐批补 `@Override`（编译器会替你验证每一次）。

## 12.4 写这类重写脚本的教训（本轮踩过）

第一版 `fix_override_drift.py` 把整行**用正则分组拼回去**，结果**吃掉了行尾的 ` {` 和行首缩进**，
一次改坏 19 个文件、`javac` 报 214 个错。正确做法是**只替换匹配区间**：
`line[:m.start()] + 改写 + line[m.end():]`。

同一版还做了两处过度动作，都已收敛：
- 把**全仓 226 个未被引用的 import** 一起删了（能编译过，但把一次定点修复变成了不可审阅的大 diff）
  → 改成**只允许删本工具自己弄失效的那几个**（`DROPPABLE_IMPORTS`）。
- 把 `BlockGetter` 的 import **改名**成 `LevelReader`，但文件别处还在用 `BlockGetter` → 改坏。
  → 改成 `ensure_import()`（缺就补）+ `normalize_imports()`（只删确实失效的、并去重）。

**selftest 一开始没抓住这些**，因为断言太弱（只断言顺序对不对，没断言行尾 `{`、缩进、
以及"行数没变"）。补上这三条断言后才真正兜住。**改写脚本的 selftest 必须断言"没被改动的地方"。**

---

# 13. 枪手 AI 崩溃 / 生物不带武器 / 没有持枪动画

用户实测报告的三件事，根因是三个**互相独立**的问题，其中两个是**继承自 0.5.5 的老 bug**。

## 13.1 崩溃：`GunAttackGoal` 读了不存在的 NBT

崩溃报告（`crash-2026-09-22_16.58.35-server.txt`，"Ticking entity"，实体是**原版掠夺者**）：

```
NullPointerException: Cannot invoke "CompoundTag.getInt(String)" because the return value of
"top.ribs.scguns.util.NbtHelper.getTag(ItemStack)" is null
    at top.ribs.scguns.entity.ai.GunAttackGoal.tick(GunAttackGoal.java:173)
```

**链路**：

1. `RaidManager.createModifiedGun()` 给袭击者发枪，里面有这么一行（**0.5.5 逐字相同**）：
   ```java
   if (gun instanceof GunItem gunItem && NbtHelper.getTag(gunStack) != null) { ... putInt("AmmoCount", ...) }
   ```
   而 `gunStack` 是**刚 new 出来的** `ItemStack`，**永远没有** custom data → 判断恒为假 →
   `AmmoCount` **从来没被写入过**。（0.5.5 是 `gunStack.getTag() != null`，同一个错误。）
2. 掠夺者拿着这把"没有 NBT 的枪"，`GunAttackGoal.tick()` 直接 `NbtHelper.getTag(heldItem).getInt("AmmoCount")`
   → `getTag` 返回 null → **NPE → 服务端崩**。

**修法（两处，纵深防御）**：

- `RaidManager.createModifiedGun`：改用 `NbtHelper.getOrCreateTag(gunStack)`，不再用 `!= null` 把唯一该做的初始化挡掉。
- `GunAttackGoal`：新增 `getAmmoCount(ItemStack)`，**没有 NBT 就当作 0**（于是走到换弹分支，正是想要的行为）；
  3 处读（tick 的两处 + 条件判断）和 1 处写（`consumeAmmo`）都改用它 / `getOrCreateTag`。
- `EntityEquipmentConfig.EquipmentEntry.createItemStack`：**装备配置发出来的枪也补上 `AmmoCount`**（满弹匣）。
  0.5.5 只有**创造模式物品栏**会初始化 `AmmoCount`，装备配置这条路完全没管。

## 13.2 生物不带武器：`finalizeSpawn` 的签名多了一个参数，**整个方法从没被执行过**

这才是"scgun 的生物不会自带武器"的根因，而且它是 §12 那类 bug 的**第二个家族**。

1.20.1 Forge 的签名是 5 个参数（末尾多了个 `@Nullable CompoundTag pDataTag`）：

```java
public SpawnGroupData finalizeSpawn(ServerLevelAccessor, DifficultyInstance, MobSpawnType,
                                    @Nullable SpawnGroupData, @Nullable CompoundTag)   // ours (5)
public SpawnGroupData finalizeSpawn(ServerLevelAccessor, DifficultyInstance, MobSpawnType,
                                    SpawnGroupData)                                     // 1.21.1 (4)
```

参数个数不同 → **不是覆写**，只是一个普通重载 → 原版从不调用它 →
里面的 `EntityEquipmentConfig.equipEntity(this, "scguns:xxx")` **一次都没跑过** →
**所有 gunner 类生物都不带武器**。参数个数不同，所以 §12 原来的审计规则（同参数个数）
**看不到它**——这就是给审计加 ARITY-MISMATCH 桶的原因（见 §12，规则：**父类参数表是我们参数表的真前缀**，
即"Forge 回调参数被删掉"的指纹）。

**受影响 10 个实体**（`Adjudicator / Blunderer / CogKnight / CogMinion / Dissident / Finforcer /
Hornlin / Subjugator / SupplyScamp / ZombifiedHornlin`）。`testTag` 参数在方法体里**从未被使用**，
所以直接删掉即可。脚本 `tools/fix_arity_drift.py`（幂等，`--selftest`）。

**注意**：`HornlinEntity` / `ZombifiedHornlinEntity` 传的 id 是 `"hornlin"` / `"zombified_hornlin"`（无命名空间），
而 `data/scguns/entity/equipment/` 里**只有 6 个配置**（adjudicator/blunderer/cog_knight/cog_minion/finforcer/subjugator）。
查过 0.5.5 原始 jar：**它也正好只有这 6 个**，两个 id 也一样。所以这两只**在 0.5.5 就没有装备配置**，
`equipEntity` 是个空操作 —— **属于继承行为，不是移植回归，故意不擅自补内容**。

## 13.3 持枪动画：**不要抄参照工程**（第一版抄错了，已回退）

> **修订记录**：本节最初按参照工程的做法给 mixin 加了"非玩家分支"，用户实测后指出
> **那正是参照工程的写法，也就是没有动画**，要求用 0.5.5 的做法。已回退。教训记在下面。

### 13.3.1 生物：0.5.5 的持枪动画 = **原版物品层 + 模型自身的举枪姿势**

0.5.5 的 `ItemInHandLayerMixin` 整段逻辑**只对玩家生效**（`entity.getType() == EntityType.PLAYER`）。
所以生物走的是**原版** `ItemInHandLayer.renderArmWithItem`：

- 枪由它自己的渲染器（GeckoLib `AnimatedGunRenderer`）按**物品模型的第三人称变换**画出来 —— 这就是那份
  "通用"的持枪表现；
- 手臂姿势由**生物模型自己的 `setupAnim`** 摆（`AdjudicatorModel` 等 9 个模型里都有
  `entity.getMainHandItem().getItem() instanceof GunItem` 分支；原版人形还有三套 mixin，见 13.3.3）。

参照工程（0.4.7 底子）在非玩家分支里改成：取消原版 → `translateToHand` + 硬编码旋转
（`-90° X`、`180° Y`、`translate(±1/16, 0.125, -0.625)`）→ `renderWeapon`。
实测**看起来就是没有动画**。这是两个不同版本的设计差异，**不要互相抄**。

**结论：这条路径保持 0.5.5 原样（player-only）。** 上一轮"生物没动画"的真正原因是**它们身上根本没有枪**
（`finalizeSpawn` 从不执行，见 13.2），而不是缺姿势代码。

### 13.3.2 玩家第三人称：0.5.5 有个"站着不动就不摆姿势"的提前返回

`PlayerModelMixin`（与 0.5.5 **逐字一致**，只有 1.21 的机械改名）里有：

```java
if (player.isLocalPlayer() && animationPos == 0.0F) { 把两只手臂清零; return; }
```

`setupAnim` 的**第二个参数就是 `limbSwing`**（已核对 1.21.1 字节码：`LivingEntityRenderer` 把
`WalkAnimationState.position(partialTick)` 传进去），玩家站住不动时它**恰好是 0.0F** →
手臂被清零并 `return` → 下面的 `heldAnimation().applyPlayerModelRotation(...)` 根本不执行 →
**第三人称看起来就是"普通地拿着物品"**。而按 F5 站着看，正是最常见的观察方式。

**修法**：把该提前返回**限定在第一人称**（第一人称下屏幕上不是玩家模型的手臂，这个清零才有意义），
第三人称无论如何都套用持枪姿势。这是**有意偏离 0.5.5 的一处**，理由如上。

### 13.3.3 手臂姿势 mixin 的注入目标（已验证存在）

| mixin | 注入目标（1.21.1 实际描述符） |
|---|---|
| `MixinHumanoidModel` | `setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V` ✅ |
| `MixinIllagerModel` | `setupAnim(Lnet/minecraft/world/entity/monster/AbstractIllager;FFFFF)V` ✅ |
| `MixinAbstractZombieModel` | `setupAnim(Lnet/minecraft/world/entity/monster/Monster;FFFFF)V` ✅ |

用 `tools/probe_setupanim.py` 打印真实描述符核对过（`javap` 打的是泛型形式，会看错）。
`@Shadow` 的字段（`rightArm/leftArm/head/crouching`、`arms`）在 1.21.1 里也都还在。
注意 `PlayerItemInHandLayer` **覆写**了 `renderArmWithItem`，但它内部 `invokespecial` 调了
`super`（字节码偏移 54），所以注入在父类 HEAD 的 mixin **对玩家仍然生效**。

### 13.3.4 顺带确认：枪械在第三人称一定能画出来

`renderGun` 对**动画枪**在第三人称会**直接 return**（我们与参照工程在这里等价，德摩根展开后相同），
所以必须有 `ModelOverrides` 兜住。`tools/audit_gun_render_paths.py` 的结论：

- **141 把枪全部是 `Animated*GunItem`**（`AnimatedAirGunItem` / `AnimatedDiamondSteelGunItem` /
  `AnimatedUnderWaterGunItem` / `AnimatedSculkGunItem` / `AnimatedDualWieldGunItem` /
  `AnimatedScorchedGunItem` …），**并且 141 把全部注册了 `ModelOverrides`**；因此第三人称一定画得出来。
- **因此也不存在**"非动画 `GunItem` 无限递归"的风险：那需要 BEWLR 是 `GunItemStackRenderer`，
  而它只挂在 `GunItem`（非 `Animated*`）上，本仓库一把都没有。

## 13.4 顺带修好的另外两个 arity 家族

同一个脚本一起处理（全部由 `@Override` + javac 证明）：

- **`checkAndPerformAttack`（10 个近战 goal）**：1.20.1 是 `(LivingEntity, double 距离平方)`，
  1.21.1 只有 `(LivingEntity)`。所以**这些自定义近战攻击从来没触发过**。
  修法：删掉 `double`，在方法体开头补 `double pDistToEnemySqr = this.mob.distanceToSqr(pEnemy);`
  （与原参数同值），其余代码一字未动。
- **`renderToBuffer`（17 个实体模型）**：1.20.1 末尾是 4 个 RGBA float，1.21.1 是
  `renderToBuffer(PoseStack, VertexConsumer, int, int, int color)`。改成 5 参数后
  有 **3 个调用点**（`SulfurheadGelLayer`、`SulfurheadRenderer`、`TraumaUnitRenderer` 的"引爆闪白"层）
  需要把 RGBA 转成打包色：`FastColor.ARGB32.colorFromFloat(alpha, red, green, blue)`，颜色值原样保留。
  （这 3 个调用点证明那 17 个方法并非全部死代码——`HierarchicalModel` 自己也会实现 5 参数版，
  所以另外 14 个此前确实没有生效。）
- **`getEyeHeight(Pose, EntityDimensions)`（2 个引爆桶实体）**：1.21.1 **根本没有这个钩子**
  （`Entity.getEyeHeight(Pose)` 和 `getEyeHeight()` 都是 `final`）。眼睛高度现在属于
  `EntityDimensions`，所以改成覆写 `getDimensions(Pose)`：
  `return super.getDimensions(pose).withEyeHeight(0.15F);`（只改眼高，**不动碰撞箱**）。

## 13.5 新增/更新的工具

| 工具 | 作用 |
|---|---|
| `audit_override_drift.py`（扩展） | 新增 **ARITY-MISMATCH** 桶：父类参数表是我们参数表的真前缀 → 判定为"Forge 回调参数被删"（§13.2/§13.4 共 39 处） |
| `drift_table.py` | 把该审计的输出按方法名分组，打印 ours/super 与全部文件行号 |
| `fix_arity_drift.py` | 修上述 4 个家族（幂等，`--selftest`） |
| `probe_setupanim.py` | 打印模型类里 `setupAnim` 的**真实描述符**，用来验证 mixin 注入目标是否还存在 |
| `audit_gun_render_paths.py` | 查"哪些枪在第三人称画不出来 / 哪些会无限递归"（靠 `extends` 链判定动画枪，**不能用子串匹配**：`AnimatedAirGunItem` 里并没有 "AnimatedGunItem" 这个子串） |
| `diff_vs_055.py` | 把 0.5.5 反编译与移植版**逐行对比**（归一化 1.21 的机械改名），用来找被静默删掉的行——例如"少了 `heldItem.setTag(tag)`"这类 |
| `compare_trees.py` | 逐包对比文件集合，找**被丢掉的类** |
| `rcon_mob_equipment.py` | 起真实专用服务器，RCON 召唤生物并读回 `HandItems`，**实机验证生物装备与 `AmmoCount`** |

---

# 14. 客户端界面与时间：三类"1.21 改了契约但能编译"的 bug

## 14.1 界面背景被渲染两次 → **模糊效果盖住内容**（已修，21 个界面）

1.20.1 的 `Screen.render` **不**调用 `renderBackground`，界面必须自己调；多调一次只是把背景重画一遍，**看不出来**。
1.21.1 的 `Screen.render` **自己会调**（`.refs/nf-src/.../gui/screens/Screen.java:132-138`），而且
`renderBackground` 现在还多跑一个**模糊后处理**（同文件 382 行 → `GameRenderer.processBlurEffect`）。

于是 `this.renderBackground(...)` + `super.render(...)` 的写法在 1.21 里会把**两者之间画的东西全部模糊掉**：
面板、物品、文字都糊了，而之后画的按钮是清晰的 —— 用户截图里"蓝图页面被模糊盖住、
底部两个按钮却很清楚"就是这个。

**修法分两种**（vanilla 自己的实现就是答案）：

- **普通 `Screen`**（只有 `BlueprintScreen`）：`super.render` 就是 `Screen.render`。
  vanilla 的 `AbstractContainerScreen.render` 注释写着 "replicate the super method's implementation
  to insert the event between background and widgets" —— 照做：自己调一次 `renderBackground`，
  然后**直接遍历 `this.renderables`**，不要调 `super.render`。
- **`AbstractContainerScreen`**（其余 20 个）：`super.render` 是 `AbstractContainerScreen.render`，
  它**已经**调了 `renderBackground`（并通过自己的覆写调用 `renderBg`）；这些界面的自定义内容都画在
  `super.render` **之后**，所以那行显式调用是多余的，**删掉**即可。

脚本：`tools/fix_double_background.py`（幂等，`--selftest`，按基类分流）。
审计：`tools/audit_double_background.py`（必须 0）。

## 14.2 配件界面没有枪械模型（已修）

`AttachmentScreen` 里 0.5.5 是这么写的：

```java
PoseStack modelStack = RenderSystem.getModelViewStack();   // 1.20.1 有
modelStack.pushPose();
modelStack.mulPoseMatrix(pGuiGraphics.pose().last().pose());
RenderSystem.applyModelViewMatrix();
... renderWeapon(..., new PoseStack(), ...)                 // 注意是**新建的空** PoseStack
```

1.20.1 的着色器从 RenderSystem 的 model-view 矩阵栈取值，所以"把 GUI 的位姿压进去 +
传一个空 `PoseStack`"是能工作的。**1.21 删除了 `getModelViewStack()`**（移植时这几行被删），
而且 1.21 的矩阵是**跟着每次 draw 传入的 `PoseStack` 走**的 —— 传空栈就等于**没有任何变换**，
枪被画到别处（被 scissor 裁掉），界面里自然看不到枪。

**修法**：把 `pGuiGraphics.pose()` 直接传给 `renderWeapon`（这条链上的变换本来就已经压在它上面），
顺带删掉两处已经无意义的 `applyModelViewMatrix()`。这类"1.20.1 靠全局矩阵、1.21 靠参数"的差异
**能编译、能运行、只是画不出来**，属于最难查的一类。

## 14.3 后坐力：1.20.1 有**两个**不同的时间量，移植时合并成了一个（已修）

| 1.20.1 | 含义 | 1.21.1 对应 |
|---|---|---|
| `Minecraft#getFrameTime()` | 插值系数，0..1 | `getTimer().getGameTimeDeltaPartialTick(false)` ✅ |
| `Minecraft#getDeltaFrameTime()` | **本帧流逝的游戏刻数**（20fps 时约 1.0） | **`getTimer().getGameTimeDeltaTicks()`** |

移植时把**两者都**映射成了 partial tick。0.5.5 里只有 4 处用后者：

- `RecoilHandler` / `GunRecoilHandler` 的**镜头后坐力回复**：
  `recoilAmount = cameraRecoil * delta * 0.15F` —— 用 partial tick（与真实帧时间无关、
  且随帧率乱跳）会让回复速度完全错掉，于是"上抬"和"回复"互相打架。
- `PlayerModelMixin` 里战斧持握动画的 `delta`。
- `GunItemStackRenderer` 传给 `renderWeapon` 的最后一个参数。

4 处已改为 `getGameTimeDeltaTicks()`（= 1.20.1 `deltaTickTime`，语义一致）。
审计：`tools/audit_time_delta.py`（必须 0；它会**先去掉注释**再数，免得注释里提到方法名就算通过）。

## 14.4 重复的事件注册（已修 2 处）

同一处理器注册两次 → 每个 `@SubscribeEvent` 每事件跑两遍。0.5.5 就有，Forge 容忍：

- `HUDRenderHandler`：`ClientHandler.registerClientHandlers` **和** `ScorchedGuns` 各注册一次
  → HUD 画两遍。
- `PlayerModelHandler`：`ClientHandler` 里注册两次 → `applyPlayerPreRender` 与近战手臂变换各跑两遍。

审计：`tools/audit_duplicate_registrations.py`（必须 0）。

## 14.5 玩家第三人称持枪姿势：**仍在查**（已加临时诊断）

已确认的事实：

- `ItemInHandLayerMixin` 的玩家分支**确实在跑**（上一轮加的非玩家分支生效过，证明注入有效；
  且 `PlayerItemInHandLayer` 虽覆写了 `renderArmWithItem`，字节码里 `invokespecial` 调了 `super`）。
- 姿势数据完整（`GripType` 12 种都指向真实 Pose 类；`TwoHandedPose.forwardPose` 的角度值都在）。
- 代码与 0.5.5 **逐字一致**（只有 1.21 机械改名），用户实测所用的 jar（17:30 安装、17:33 启动）
  **包含**把"站立不动就清零手臂"限定到第一人称的改动。

也就是说静态可查的地方全都对，但实测仍不对 —— 因此加了**临时诊断**：
`client/render/PoseDiagnostics.java`（每 key 只打一次、整局最多 40 行，前缀 `[SCGUNS-POSE]`），
在 `ItemInHandLayerMixin` 与 `PlayerModelMixin` 各打一行，报告本分支是否执行、
相机类型、`limbSwing`、`aimProgress`、gripType 与当前 `rightArm.xRot`。
**定位完请一并删除这个类与两处调用。**

---

# 15. 【已定位的真凶】`MixinPlugin` 把**全部 15 个 mixin** 关掉了

上一节那些"静态全对但实测不对"的怪事，根因在这里，而且**一个原因解释了全部**：
**这个 mod 的 mixin 从来没有生效过。**

## 15.1 机制

`scguns.mixins.json` 里写着 `"plugin": "top.ribs.scguns.mixin.MixinPlugin"`。
0.5.5 的插件是这么写的（移植时逐字保留）：

```java
public void onLoad(String mixinPackage) {
   try {
      Class.forName("com.mrcrayfish.framework.FrameworkForge", false, this.getClass().getClassLoader());
      this.isFrameworkInstalled = true;
   } catch (Exception e) {
      this.isFrameworkInstalled = false;
   }
}
public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
   return this.isFrameworkInstalled;          // ← 全部 mixin 的生死开关
}
```

**`com.mrcrayfish.framework.FrameworkForge` 是 1.20.1 Forge 版 Framework 的类名。**
NeoForge 版换成了 **`FrameworkNeoForge`**（已在实例的
`framework-neoforge-1.21.1-0.13.11.jar` 里核对：`FrameworkForge` **不存在**）。
于是 `Class.forName` 必然抛异常 → `isFrameworkInstalled = false` →
**`shouldApplyMixin` 对每一个 mixin 都返回 false → 15 个 mixin 全部被跳过。**

而**被跳过的 mixin 在普通日志级别下什么都不打**，所以完全无声。

## 15.2 一个原因解释了此前所有症状

| 症状 | 失效的 mixin |
|---|---|
| 生物没有持枪动画（手臂不摆） | `MixinHumanoidModel` / `MixinIllagerModel` / `MixinAbstractZombieModel` |
| 玩家第三人称持枪=普通持物姿势 | `ItemInHandLayerMixin`（枪不再走枪械渲染器）+ `PlayerModelMixin`（手臂不摆） |
| 别的 | `VehiclePoseMixin`、`GameRendererMixin`、`MinecraftMixin`、`MouseHandlerMixin`、`LevelRendererMixin`、`MixinVindicatorRenderer`，以及 **4 个 common mixin**（`EndPortalBlockMixin`/`ItemRarityMixin`/`LivingEntityMixin`/`SittingPhaseMixin`）也全部没生效 |

**顺带一个方法论教训**：上一轮我加了"非玩家分支"后，**从用户的描述里推断"它生效了"**，
并把这个推断当成"注入有效"的证据写进了文档。那一步是错的 —— 用户并没有对比过前后差异。
**没有实测证据就不要把"看起来变了"当成验证。** 这一轮 `PlayerModelMixin` 那条同理：
静态核了一整圈（注入目标存在、姿势数据完整、与 0.5.5 逐字一致、jar 里确实有新代码），
**唯独没查"这个 mixin 有没有被加载"**，而它就在 `MixinPlugin` 里。

## 15.3 修法与审计

`MixinPlugin` 现在：

- 探测候选数组 `{FrameworkNeoForge, FrameworkForge}`（旧名保留作回退，两个平台都能用）；
- **`shouldApplyMixin` 恒返回 `true`**。理由：**本包内没有任何 mixin 引用 Framework**
  （全包搜索 "mrcrayfish"/"framework" 只命中探测那一行），所以这个开关唯一的作用就是
  "在探测失败时把全部功能关掉"，没有任何收益。探测结果只用来**打一条 WARN**，
  把这类失败从"无声"变成"看得见"。

新增审计 **`tools/audit_mixin_plugin_gate.py`**（已用"临时回退到 0.5.5 写法"验证过它**确实会报错**）：

1. 插件里探测的类名（含候选数组里的字面量）**必须至少有一个能在依赖 jar 里找到** ——
   全部找不到 ⇒ 这个门永远为假 ⇒ 整份 config 都会被跳过；
2. `shouldApplyMixin` **不允许**返回一个门字段，除非真有 mixin 用到那个依赖。

## 15.4 复现验证

```powershell
python tools\audit_mixin_plugin_gate.py
# 修好后：0 problem(s)
# 把 MixinPlugin 临时改回 0.5.5 写法：2 problem(s)，exit 1
```

打包后的字节码也核过：`shouldApplyMixin` 就是 `iconst_1; ireturn`。

## 15.5 关掉开关后**立刻暴露**的第二个 bug：`EndPortalBlockMixin` 注入点已不存在（已修）

mixin 一生效，`runServer` 立刻**启动失败**（`> Task :runServer FAILED`，没有 `Done`）：

```
InjectionError: Critical injection failure:
Callback method beforeChangeDimension(BlockState, Level, BlockPos, Entity, ...)
```

原因：0.5.5 的注入点在 `EndPortalBlock#entityInside` 里 `Entity#changeDimension(ServerLevel)`
这条指令上。**1.21 把 `entityInside` 里的这条调用删掉了**（现在只做
`entity.setInsidePortal(this, pos)`，真正的穿越移到
`EndPortalBlock#getPortalDestination(ServerLevel, Entity, BlockPos)`，由 `Entity#handlePortal` 调用）。
注入点不存在 + `defaultRequire = 1` ⇒ **启动期硬失败**，而不是静默失效。

**注意**：`Entity#changeDimension` 在 1.21 **仍然存在**，只是参数从 `ServerLevel` 换成了
`DimensionTransition` —— 所以**只比对方法名的检查查不出来**，必须比对**参数类型**。

修法：把注入改到 `getPortalDestination` 的 HEAD，回调签名
`(ServerLevel level, Entity entityIn, BlockPos pos, CallbackInfoReturnable<DimensionTransition> cir)`；
`level.dimension() == Level.END` 与原 `worldIn.dimension() == Level.END` 语义相同。

## 15.6 `audit_mixins.py` 补上"注入点目标"检查（并已证明能抓住上面这个 bug）

原审计只检查 `method = "xxx"` 里的方法名，**完全不看 `@At(target = "L...;name(desc)ret")`**，
所以它当时报"0 missing injection point(s)"，而实际有一个注入点是死的。现在它：

1. 解析每个 `@At` 的 `target`，取出 owner / 方法名 / **参数类型序列**；
2. **沿继承链**找该方法（`this.addLayer(...)` 这类调用点不算声明，继承的方法也算数）；
3. 参数类型对不上 ⇒ 报 `BROKEN`。

两个实现要点（都踩过）：

- **类声明的 `extends` 不能用正则抓**：`class MobRenderer<T extends Mob, M extends EntityModel<T>> extends LivingEntityRenderer`
  里有嵌套泛型和类型参数自带的 `extends`。改成扫描类头直到 `{`，取其中**最后一个** `extends`。
- **参数类型取"第一个标识符"**，不是倒数第二个：`RenderLayer<T, M> layer` 取倒数第二个会得到 `M`。

**证明**：把 `EndPortalBlockMixin` 临时改回 0.5.5 的注入点，审计立刻报

```
BROKEN .../EndPortalBlockMixin.java  @At target Lnet/minecraft/world/entity/Entity;changeDimension(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/entity/Entity;
        no method changeDimension with those parameter types
1 missing injection point(s)   exit 1
```

改回修复版后 0 problem(s)。

## 15.7 这一轮的教训汇总

1. **`MixinPlugin.shouldApplyMixin` 是全局开关**：一个探测类名写错，就有 15 个 mixin 静默失效。
   现在它恒为 `true`，探测只用来打 WARN。
2. **"静态审计 0" 不等于"功能生效"**：审计要检查**注入点目标**、**回调签名**、**插件门禁**，
   不能只检查 `method =` 的名字。
3. **不要把用户的描述当验证**：上一轮我从用户措辞推断"非玩家分支生效了"，
   并据此写进文档当作证据 —— 那是错的。要实机证据（这次是 `runServer` 的
   `Mixing ... from scguns.mixins.json into ...` 日志行）。

## 15.8 开关一开，**连查出 4 个会崩客户端的 mixin bug**（全部已修）

因为 15 个 mixin 之前从来没生效，它们里的错误也从来没被执行过 —— 关掉 `MixinPlugin` 的门之后
`runServer` 立刻炸，随后靠新审计又找出 3 个**只会在客户端炸**的。四个都是
"注入点/回调签名与 1.20.1 不同 → `defaultRequire = 1` ⇒ 启动即硬失败"：

| mixin | 问题 | 修法 |
|---|---|---|
| `common.EndPortalBlockMixin` | 注入点 `Entity#changeDimension(ServerLevel)` 在 1.21 的 `entityInside` 里已不存在（穿越移到 `getPortalDestination`） | 改注入 `getPortalDestination` HEAD，回调改 `(ServerLevel, Entity, BlockPos, CallbackInfoReturnable<DimensionTransition>)` |
| `client.GameRendererMixin` | 回调声明的是 1.20.1 的 `render(float, long, boolean)`；1.21 是 `render(DeltaTracker, boolean)` | 回调改 `(DeltaTracker, boolean, CallbackInfo)` |
| `client.LevelRendererMixin` | 同上：`renderLevel` 少了 `PoseStack`/`long`，多了 `Matrix4f` | 回调改 `(DeltaTracker, boolean, Camera, GameRenderer, LightTexture, Matrix4f, Matrix4f, CallbackInfo)`；注入点 `checkPoseStack` ordinal 0 **仍存在**，且那里 level 的 `PoseStack` 恰好是单位矩阵（`checkPoseStack` 刚断言过），所以传 `new PoseStack()` 等价 |
| `client.MinecraftMixin` | 用 `locals = LocalCapture.CAPTURE_FAILHARD` 捕获一个 **1.21 已不存在的 `InteractionHand[]` 局部变量** | 改成 `@Redirect` 掉 `ItemInHandRenderer#itemUsed` 调用 —— 回调签名来自**被重定向的方法**（receiver + 参数），完全不依赖局部变量表 |
| `client.MouseHandlerMixin` | `method = {"turnPlayer()V"}` 这个描述符在 1.21 不存在（现在是 `turnPlayer(double)`） | 改成只写方法名 `"turnPlayer"`（`opcode=57` 的 DSTORE 有 7 个，`ordinal=2` 仍有效） |

**关于 `MinecraftMixin` 的语义差异**：原做法 `ci.cancel()` 会跳过 `startUseItem` 里那条调用**之后**的
所有代码；`@Redirect` 只跳过 `itemUsed` 本身，之后可能的挥臂仍会发生。若实测发现枪有异常挥臂，
再回来处理。

## 15.9 `audit_mixins.py` 现在还检查这些（都验证过能抓住对应 bug）

| 检查 | 抓的是 |
|---|---|
| `@Mixin(X.class)` 目标漂移 | 目标被改名 |
| `method = "name"` 名字存在 | EndPortalBlock 那类 |
| **`method = "name(desc)ret"` 里的描述符必须匹配某个真实重载** | `turnPlayer()V` 那类（1.21 变成 `turnPlayer(double)`） |
| **`@At` 的 `target = "L...;name(desc)ret"` 必须能在目标类（含继承链）里找到同参方法** | `changeDimension(ServerLevel)` 那类 |
| **`@Inject` 回调参数必须等于目标方法参数 + 末尾的 `CallbackInfo`** | `GameRendererMixin`/`LevelRendererMixin` 那类 |
| `@Redirect`/`@ModifyArg`/`@ModifyVariable` 与 `locals = LocalCapture` 的回调**不按上面的规则判**（它们的签名来自被重定向成员或额外声明捕获的局部变量），**因此不在自动检查范围内 —— 手工复核** | 本地捕获漂移 |

最后一行是**已知盲区**：`MinecraftMixin` 那个 bug 正是落在这里，最后是靠**去读真实字节码的
`LocalVariableTable`**（`tools/probe_startuseitem_locals.py`）才确认的。**遇到 `locals =` 或
`@ModifyVariable`/`@ModifyArg` 一定要手工核对，别指望审计。**

# 16. 第三轮实测反馈：进度全缺 / 蓝图描述缺失 / 配件不同步

三个症状，两个根因，都属于**"1.21 组件化之后语义变了，但代码照样编译、照样跑"**那一类。

## 16.1 进度界面里完全没有 scguns 标签页：`DisplayInfo.icon` 用的是 `ItemStack.STRICT_CODEC`

* 1.20.1 的进度写 `"icon": {"item": "scguns:m3_carabine"}`。
* 1.21.1 的 `DisplayInfo` 用 **`ItemStack.STRICT_CODEC`**（`javap -c net.minecraft.advancements.DisplayInfo`
  的 `lambda$static$0` 里能看到 `Field net/minecraft/world/item/ItemStack.STRICT_CODEC` + `Codec.fieldOf("icon")`），
  字段是 **`id`**（`count`/`components` 可选），**根本没有 `item`**；
  `ItemStack` 自己的 codec 里也只有 `id`/`count`/`components`（javap 可查），所以 `item` 必然让 `DisplayInfo` 解析失败。
* 规模：`data/scguns/advancement/` 下 **115 份全部**用了 `item`。

### ⚠️ 这个失败的**症状很反直觉**：进度不会消失，而是**静默失去 display**

用 `tools/probe_advancement_icon.py`（写 `build/resources/main` 里的进度 → RCON `/reload` → 数日志）
实测出来的（**四种点都试了**）：

| 改法 | `Loaded N advancements` | 是否报错 |
|---|---|---|
| `icon: {"item": ...}`（0.5.5 写法） | 3644（**没变**） | 无 |
| `icon: {"id": "scguns:不存在的物品"}` | 3644 | 无 |
| `icon: {}` / `icon: 5` / `"display": 5` / 删掉 `title` | 3644 | 无 |
| 删掉 `criteria` / `criteria: {}` / `parent` 指向不存在的进度 | **3529** | `Couldn't load advancements: [...]` |
| 把整个 `advancement` 目录移走 | **3529** | — |

（3644 − 3529 = **115**，正好就是我们的文件 ⇒ 这个日志计数确实包含它们。）

结论：**`display` 解析失败不会让进度被丢弃**，1.21 会把这个进度装进树里，只是 `display` 变成了空
⇒ 在进度界面里**完全看不见**，root 没有 display 就**连标签页都不出现**。这正好是玩家看到的现象。
`criteria`/`parent` 这类"硬字段"失败才会真正丢弃。

**所以：只用"被丢弃数"来体检进度是查不出问题的**（HANDOFF 全局规则："审计 0 ≠ 功能生效" 的又一例）。
只有客户端能最终确认；服务端侧能拿到的最强证据就是下面这套探针。

* 修法：`tools/fix_advancement_icons.py --apply`（对每份文件的那个 `icon` 对象做**单行手术式替换**，
  其余字节不变 —— 这些文件不是 `sort_keys` 转储：有 `parent` 的会把 `parent` 放最前）。
* 校验：`tools/analyze_advancements.py` → `problems: 0`。它现在按**会静默失败的字段**来查：

  | 检查 | 为什么 |
  |---|---|
  | icon 必须有 `id`、不能有 `item`、键只能是 `id`/`count`/`components` | 直接决定 `DisplayInfo` 能不能解析 |
  | display 必须有 `title` 和 `description` | 少一个就静默无 display |
  | display 键只能是 `icon`/`title`/`description`/`background`/`frame`/`show_toast`/`announce_to_chat`/`hidden` | 防止 1.20.1 的残留键 |
  | `frame` ∈ `task`/`goal`/`challenge` | 同上 |
  | 114 个被引用的物品 id 全部能在 `models/item` / `lang` / `data/scguns/guns` / `blockstates` 里找到 | `Holder` 允许**未绑定的**名字（写错物品 id 也不报错），只能自己查 |

同一层顺带核对过、**确认没问题**的（别再去改）：

| 检查 | 结果 |
|---|---|
| `Advancement` codec 字段 | `parent`/`display`/`rewards`/`criteria`/`requirements`/`sends_telemetry_event`：我们全对得上 |
| `InventoryChangeTrigger.TriggerInstance` 的 `items` | 是 `optionalFieldOf("items", List.of())` ⇒ `root.json` 里的空 `"conditions": {}` **合法** |
| `ItemPredicate` 字段 | 只有 `items`/`count`/`components`/`predicates` —— 1.20.1 的 `tag`/`nbt` 已不存在；扫过全部 115 份，**一处都没用** |
| 目录名 | 1.21 是**单数** `advancement/`（原版 jar 里就是 `data/minecraft/advancement/...`），我们用对了 |
| 触发器等 | 115 个 criterion 全是 `minecraft:inventory_changed`（1.21.1 存在） |

## 16.2 蓝图界面枪械描述变成"未发现"：`Item.toString()` 换了语义

| | `Item.toString()` 返回 |
|---|---|
| 1.20.1 | `BuiltInRegistries.ITEM.getKey(this).getPath()` → `musket` |
| 1.21.1 | `BuiltInRegistries.ITEM.wrapAsHolder(this).getRegisteredName()` → `scguns:musket` |

`BlueprintScreen.renderGunInfo` 里写的是
`resultItem.getItem().toString().replace("item.scguns.", "")` —— 那句 `replace` 在 1.20.1 本来就是空转
（返回值已经是 path），所以 key = `scguns.desc.musket`；1.21 之后 key 变成 `scguns.desc.scguns:musket`，
查不到 ⇒ 回落 `scguns.desc.unknown`（"Knowledge of this weapon is not yet discovered"）。
**修法**：直接 `BuiltInRegistries.ITEM.getKey(item).getPath()`。

同一家族的另一处：`AttachmentRenderer.renderForBone` 把 `toString()` 当**正则**用
（`bone.getName().matches(itemStack.getItem().toString())`），1.21 的 `scguns:` 前缀让它永不匹配 → 同样改用 path。

**规则**：全项目**不要再依赖 `Item#toString()`/`Block#toString()`**（`tools/` 里 grep `getItem().toString()`）。
剩下 3 处 `.contains("cogloader")` 之类的**恰好仍然成立**（`scguns:cogloader` 含 `cogloader`），
以及 4 处 `getPouchId()` 返回 `toString()` 当 NBT 键 —— 读写两端用的是同一个函数，
自洽，所以**没改**（改了反而会让老存档的 pouch 数据读不到）。

## 16.3 配件装上后不同步（要丢出去再捡回来）：`ItemStack.copy()` 在 1.21 是**浅拷贝组件**

这是本轮最重要的一条：**它不是一个单点 bug，而是一个通用陷阱**。证据链全部来自官方 jar 字节码：

1. `ItemStack.copy()` → `this.components.copy()` → **`PatchedDataComponentMap.copy()`**：新实例
   **共享同一个 patch map**（只是把 `copyOnWrite` 置 true），所以 `Optional<CustomData>` 和里面的
   `CompoundTag` **是同一个对象**。
2. `AbstractContainerMenu.synchronizeSlotToRemote`：只有 `ItemStack.matches(remoteSlots.get(i), stack)`
   为 **false** 才发包。
3. `ItemStack.matches` → `isSameItemSameComponents` → `Objects.equals(a.components, b.components)`，
   而 `CustomData.equals` 是**按 tag 内容**比较。

⇒ **原地修改共享 tag**：服务端和它的 `remoteSlots` 影子看到的是同一份数据，比较恒等 ⇒ **永远不发包**。
服务端有、客户端没有；把物品丢出再捡回来会走整栈下发，所以"捡回来就同步了"。
**1.20.1 不会这样**：那时 `ItemStack.copy()` 做的是 `tag.copy()` 深拷贝。

同一个机制还影响 `ServerEntity` 的"上次发过的装备"（别的玩家看到你手里的枪），一起修好了。

**修法（新的项目规则，写进 `NbtHelper` 的类注释）**：

* `NbtHelper.getTag(stack)` —— **只读**（返回活 tag，可为 null，零拷贝，渲染/tick 循环里随便用）。
* `NbtHelper.getOrCreateTag(stack)` —— **先脱钩再交付**：`stack.set(CUSTOM_DATA, CustomData.of(tag))`
  会复制 tag 并把 patch 从旧副本上拆下来，所以之后对返回值的原地写**能被 `ItemStack.matches` 看见**。
  代价是每次调用一次 tag 拷贝 ⇒ **只读路径别用它**。
* `NbtHelper.getTagForWrite(stack)` —— 语义与 `getTag` 完全一致（没有 tag 时返回 null，
  所以 0.5.5 那些 `if (tag != null)` 判空分支的含义不变），但交付的是私有副本。

已改的写点：

| 位置 | 问题 |
|---|---|
| `MeleeAttackHandler`、`ReloadTracker` ×5、`ServerPlayHandler` ×2、`GunEventBus`、`AmmoBoxItem`、`GunnerMobSpawner`、`C2SMessageClearBlueprintRecipe` | 共 14 处"透过 `getTag` 原地写" ⇒ 改成 `getTagForWrite` |
| `GunEventBus.postShoot` | `getOrCreateTag` 的局部变量跨过了 `Gun.getAmmoCount()`（它内部会再次脱钩）⇒ 拆成各自重新取 |
| `AnimatedGunItem` 粒子回调 | 那行"取了结果又丢掉"的 `getOrCreateTag(heldStack);` 会把上面的局部变量弄成陈旧引用 ⇒ 删掉（它本来就是死语句） |

新审计 **`tools/audit_nbt_write_alias.py`**（退出码非 0 即有隐患），并且**自测过**：
`git stash push -- src/main/java` 之后跑 → **14 条**；改完再跑 → **0 条**。
它按**花括号作用域**判断局部变量的存活区间（所以同名变量在 `if`/`else` 两个分支里不会互相误报）。

## 16.4 这一轮新增的工具

| 工具 | 用途 |
|---|---|
| `tools/fix_advancement_icons.py` | 进度 `icon.item` → `icon.id`（默认 dry-run，`--apply` 才写） |
| `tools/analyze_advancements.py` | 进度 JSON 体检：icon 形状、display 必需字段、legacy 键、父链、翻译键、item id 存在性 |
| `tools/probe_advancement_icon.py` | 改 `build/resources/main` 里的进度 → RCON `/reload` → 数日志，用来实测"什么样的进度会被丢弃" |
| `tools/rcon_cmd.py` | 通用 RCON 命令执行器（`python tools/rcon_cmd.py "reload"`） |
| `tools/audit_nbt_write_alias.py` | ★ 组件别名写审计（必须 0） |
| `tools/scan_nbt_aliasing.py`、`tools/scan_nbt_reentrancy.py` | 写审计之前的人工排查脚本（保留备查） |

# 17. 第四轮实测反馈：外骨骼界面一打开就"崩溃"

## 17.1 症状与日志

玩家的日志里直接给了答案（`logs/latest.log`）：

```
io.netty.handler.codec.EncoderException: Failed to encode packet 'serverbound/minecraft:custom_payload'
Caused by: io.netty.handler.codec.EncoderException: Empty ItemStack not allowed
    at net.minecraft.world.item.ItemStack$2.encode(ItemStack.java:167)
    at top.ribs.scguns.network.message.C2SMessageSaveExoSuitUpgrades.encode(...:39)
```

## 17.2 根因：1.20.1 的 `writeItem` 允许空栈，1.21 的 `STREAM_CODEC` 不允许

1.20.1 用 `FriendlyByteBuf#writeItem(ItemStack)`，写 `ItemStack.EMPTY` 完全正常（就是 count=0）。
1.21 换成了 `ItemStack.STREAM_CODEC`，它**明确拒绝空栈**——
`ItemStack.OPTIONAL_STREAM_CODEC` 才是"允许空栈"的那个（两者线格式一致，只是空栈时写一个 0 标记）。

外骨骼界面正好踩中：`ExoSuitMenu.saveUpgradesToServer()` 把 4 个升级槽**整组**发出去（空的也发），
只要有一个空格就 100% 必炸。而且这个异常发生在 **netty 编码器里**，不是普通异常：
玩家看到的不是"报错弹窗"，而是**连接直接断掉 = 崩溃**。

触发点：`onContentsChanged`（槽位变化）、`onTake`、`removed`（关界面）都会发；打开界面时
`loadUpgradesFromArmor()` 把已装的升级塞进槽位 → 触发 `onContentsChanged` → 发包 → 炸。

## 17.3 修法（把这一整类都堵掉）

`tools/fix_item_stream_codec.py --apply`：把本模组里 **11 个文件 22 处** `ItemStack.STREAM_CODEC` /
`LIST_STREAM_CODEC` 全部换成 `OPTIONAL_`（encode/decode 成对替换，两端都是我们自己的代码，
线格式一致，所以安全）：

| 文件 | 为什么可能为空 |
|---|---|
| `C2SMessageSaveExoSuitUpgrades` | ★ 4 个升级槽整组发送，空的也发（**本次崩溃**） |
| `S2CMessageBulletTrail` | `ProjectileEntity.item` 字段初值就是 `ItemStack.EMPTY`，没被赋值的弹射物就是空栈 |
| `ThrowableItemEntity`（spawn data） | `/summon` 出来的投掷物没有 item 数据 |
| `S2CMessageMeleeAttack` | 手上前一刻/后一刻可能已经不是枪 |
| `S2CShowTotemAnimationMessage` | 目前调用点必定非空，但没必要留这个雷 |
| 6 个自定义配方（`GunBenchRecipe` 等）的 `streamCodec` | 配方 result 来自 JSON，第三方数据包可以写 `minecraft:air` |

新审计 **`tools/audit_item_stream_codec.py`**：任何 `ItemStack.STREAM_CODEC` /
`LIST_STREAM_CODEC` 直接判失败（本模组一律用 OPTIONAL）。**自测过**：
把改动 stash 掉 → 22 条；改完 → 0 条。

**为什么这个替换一定安全（字节码证据）**：`ItemStack$2`（即 `STREAM_CODEC`）的反编译结果是

```java
decode(buf)          -> ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)      // 直接委托
encode(buf, stack)   -> { if (stack.isEmpty()) throw new EncoderException("Empty ItemStack not allowed");
                          ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack); }
```

也就是说**严格版 = OPTIONAL 版 + 一句空栈检查**，线格式完全一样，连 decode 都是同一个实现。
所以两边都换、或只换一边，都不会改变协议。

## 17.4 教训（写进全局规则）

* **1.20.1 的缓冲区读取器几乎全是"宽容"的，1.21 换成 codec 后很多变成"严格"的**。
  已知的一对：`writeItem`/`readItem` → `ItemStack.STREAM_CODEC`（严格）vs `OPTIONAL_STREAM_CODEC`（宽容）。
  同类还要留意：`writeNbt`→`readNbt`（还在）、`writeItemStack` 系列已不存在。
* **编解码器里抛异常 = 玩家掉线，而不是一条错误日志**。看到"打开某个界面就崩溃/掉线"，
  第一反应应该去 `logs/latest.log` 找 `Failed to encode packet`，而不是去查界面渲染代码。

# 18. 收尾：临时诊断已按约定删除

第四轮实测确认"持枪姿势"正常后，`client/render/PoseDiagnostics.java`（前缀 `[SCGUNS-POSE]`）
和它的两处调用（`ItemInHandLayerMixin`、`PlayerModelMixin`）已删除，两个文件现在与 0.5.5
的结构逐行一致（只保留 1.21 的机械改写）。删除脚本 `tools/remove_pose_diagnostics.py`
按**内容**定位待删行并逐行校验，避免行号漂移误删；`tools/verify_installed_jar.py`
现在反过来断言**这个 class 不在产物里**。

`ItemInHandLayerMixin` 的注释里仍保留着"为什么不做生物分支"的说明（§15.2 / §13），
那是设计决策，不是诊断代码，别一起删掉。

# 19. 把 Farmer's Delight 与 Soul Fire'd 两个前置接进 `libs/`

`libs/` 里的 jar 会被 `build.gradle` 的 `fileTree('libs')` **自动**收进
`compileOnly` + `localRuntime`（三个必需依赖 framework/geckolib/curios 单独 `implementation`），
所以"接入"只需把 `准备的前置/` 里那两个 jar 复制过来（注意 PowerShell：文件名带 `[]` 时
必须用 `-LiteralPath`，否则 `[农夫乐事]` 会被当成通配符，复制静默失败）：

```
准备的前置/soul-fire-d-neoforge-1.21-6.1.0.jar              -> libs/soul-fire-d-neoforge-1.21-6.1.0.jar
准备的前置/[农夫乐事] FarmersDelight-1.21.1-1.3.4.jar        -> libs/FarmersDelight-1.21.1-1.3.4.jar
```

接进来**立刻暴露了一个会崩游戏的真 bug**——这正是之前没接进来时查不到的那类问题。

## 19.1 `ANTHRALITE_KNIFE` 在装了农夫乐事时必崩：`KnifeItem` 构造器改签名了

0.5.5 用反射建这把刀，写死了 1.20.1 的构造器：

```java
knifeItemClass.getConstructor(Tier.class, float.class, float.class, Properties.class);
... newInstance(ModTiers.ANTHRALITE, 0.5F, -2.0F, new Properties());
```

而 Farmer's Delight 1.21.1-1.3.4 的 `KnifeItem` **只有** `(Tier, Properties)`（javap 可查），
并且攻击力/攻速改从 properties 走——他们自己的注册就是这么做的：

```java
// FD ModItems.knifeItem(Tier tier):
new Item.Properties().attributes(KnifeItem.createAttributes(tier, 0.5F, -2.0F))
// KnifeItem.createAttributes 是继承自原版 DiggerItem 的 public static
```

于是老代码在**物品注册阶段**（也就是启动时）抛 `NoSuchMethodException` →
`RuntimeException("Failed to create ANTHRALITE_KNIFE")` → **装了农夫乐事就启动崩**。
修法：改用 `(Tier, Properties)`，攻击力/攻速用原版
`DiggerItem.createAttributes(ModTiers.ANTHRALITE, 0.5F, -2.0F)` 塞进 properties
（就是 0.5.5 当年传的那两个数），仍保留反射（农夫乐事是可选依赖，没有它时
`KnifeItem` 绝不能被解析）。

**实机验证**（`tools/rcon_item_exists.py`，靠"召唤带该物品的掉落物再读回"判断注册与否——
未知 id 不会让数据包加载报错，日志里看不出来）：

```
scguns:anthralite_knife     REGISTERED   (Anthralite Knife)      <- 显示名正确，说明 lang/model 也对上了
farmersdelight:iron_knife   REGISTERED   （对照组：FD 确实加载了）
minecraft:iron_ingot        REGISTERED   （对照组：探针本身有效）
scguns:definitely_not_an_item  MISSING   （对照组：假 id 会被认出来）
```

## 19.2 灵魂火兼容：API 搬到了 Prometheus

0.5.5 调的是 `it.crystalnest.soul_fire_d.api.FireManager.setOnFire(entity, seconds, SOUL_FIRE_TYPE)`。
Soul Fire'd 的 **1.21 重写版把这个 API 整个搬到了 Prometheus**：

* `soul_fire_d` 6.1.0 的 jar 里**没有** `api.FireManager`（只有 13 个类）；
* 它的 `it.crystalnest.soul_fire_d.fire.FireRegistry` 静态初始化里
  `SOUL_FIRE_TYPE = it.crystalnest.prometheus.api.FireManager.SOUL_FIRE_TYPE`，
  并用 `FireManager.fireBuilder(...)` 注册（灵魂火焰粒子、亮度 10、伤害 2.0）；
* `it.crystalnest.prometheus.api.FireManager` 有 **同名同签名** 的
  `setOnFire(Entity, float, ResourceLocation)`；
* Prometheus 是 soul_fire_d 的**必需前置**，所以"先判断 soul_fire_d 再调 Prometheus"是安全的。

所以 `ScorchedGuns.setSoulFireOnEntity` 与 `FakeSoulFireBlock.entityInside` 现在恢复成
0.5.5 的结构（`if (soulFiredLoaded) { try { FireManager.setOnFire(...) } catch { 普通火 } }`），
只把包名换成 Prometheus。**实机验证**（`tools/rcon_soul_fire_check.py`）：把铁傀儡丢进
`scguns:fake_soul_fire`，3 秒后 Health 100 → 88、Fire 160（8 秒）、
实体上带 Prometheus 的火类型 `FireType = "minecraft:soul"`。
（保留说明：Prometheus 自己也会给火方块"打类型"，所以严格说不能 100% 归因于我们这次显式调用；
但玩法结果与 0.5.5 一致。）

## 19.3 "可选依赖在运行时不解析"这条**实测过**

新代码直接 `import it.crystalnest.prometheus.api.FireManager`（0.5.5 也是直接引用），
所以必须确认：**没装这些模组时不会 `NoClassDefFoundError`**。做法是临时把
`build.gradle` 的 `localRuntime` 过滤掉 prometheus / soul-fire-d / FarmersDelight（`compileOnly` 不动，
否则编译都过不去），跑一次 `runServer`：

```
Done (3.932s)!   ERROR/FATAL = 0   三个模组均未加载（"Found valid mod file" 0 条）
```

只有一条无害的 `ClassNotFoundException: org.jetbrains.annotations.ApiStatus$ScheduledForRemoval`
（其他模组的 mixin 探针，一直存在）。验证完 `git checkout -- build.gradle` 还原。
**结论：反射 + 标志位守卫的写法，对没装这些模组的玩家是安全的。**

## 19.4 顺手记住的两个 RCON 实机测试环境事实

1. **没有玩家在线时，普通区块"加载但不 tick 实体"**：召唤出的实体 Age 恒为 0、不下落，
   于是 `entityInside`、火伤、AI 全都观察不到。先 `/forceload add <cx> <cz>` 才有实体 tick
   （`tools/rcon_tick_probe.py` 就是用来确认这件事的）。
2. **`BlockState#entityInside` 只在实体"移动"时调用**（1.21 把它挂在 `Entity#move` →
   `tryCheckInsideBlocks` 上），所以静止不动的实体站在火里不会触发；测试要让实体**穿过**火方块。

## 19.5 顺带核实、**不需要修**的一处

0.5.5 还注册过一个自定义配方条件 `scguns:soul_fired_mod_loaded`
（`compat/SoulFiredModCondition`）。我把 0.5.5 原始 jar 里所有 JSON 扫了一遍：
**没有任何配方用它**（0 个文件），是死代码，所以本移植没有实现它，数据层也没有引用。

# 20. Sable 物理结构（sub-level）不兼容：射线检测绕过了 `BlockGetter#clip`

## 20.1 症状与根因

玩家反馈"当前不兼容 sable 物理结构"。根因是**射线检测的实现方式**：

* 0.5.5 对物理结构只有**一处**兼容代码：`ProjectileEntity.rayTraceBlocks` 里
  `if (valkyrienSkiesLoaded) return RaycastUtilsKt.clipIncludeShips(world, context);`
  —— 用 VS 的"带船的射线"替换自己那套逐格遍历。
* 本移植把它降级成了死代码：Valkyrien Skies 没有 1.21.1 版本，于是保留逐格遍历
  （`performRayTrace`），注释写着 "plain vanilla clip"。
* 但 **Sable 不往主世界加方块**：它把方块搬进 sub-level，并且**替换 `BlockGetter#clip`**
  （`dev.ryanhcode.sable.mixin.clip_overwrite.BlockGetterMixin`，原版遍历留作 `originalClip`）。
  javap 已确认 **`net.minecraft.world.level.Level` 没有覆写 `clip`**，所以 `Level`/`ServerLevel`
  继承的就是被 Sable 换掉的那个接口默认实现。
* 我们的逐格遍历直接 `world.getBlockState(blockPos)` —— **只看主世界** ⇒ 子弹穿过物理结构。

## 20.2 修法：把方块射线交回 `Level#clip`（与 0.5.5 对 VS 的做法同构）

```java
// ScorchedGuns
public static boolean sableLoaded;               // ModList.get().isLoaded("sable")
public static boolean physicsStructuresLoaded;   // sableLoaded || valkyrienSkiesLoaded

// ProjectileEntity.rayTraceBlocks
if (ScorchedGuns.physicsStructuresLoaded) {
   return world.clip(context);   // Sable 的实现先打 sub-level，再回落到原版遍历
}
return performRayTrace(...);     // 否则保持 0.5.5 的逐格遍历
```

与 0.5.5 的 VS 分支一样，这个模式下 `ignorePredicate`（忽略树叶）**不生效**，保持原样。

**这条也是给未来的通则**：物理模组只会替换 `clip`，不会往主世界加方块；所以任何
"自己做逐格遍历"的代码天然不兼容。全项目目前只有 `ProjectileEntity` 这一处是逐格遍历
（其余 20+ 处 `level.clip(...)` 调用点自动受益）。

## 20.3 验证

静态证据（javap）：Sable `BlockGetterMixin` 提供 `clip(ClipContext)` 默认实现 + `originalClip`；
`Level` 不覆写 `clip`。`tools/verify_installed_jar.py` 新增 3 条（`ScorchedGuns` 探测 `sable`、
暴露 `physicsStructuresLoaded`、`ProjectileEntity` 引用它）→ 39/39。

实机探测 `tools/rcon_sable_clip_check.py`（**部分成功**）：

* 对照组：空中水平射出的原版箭**继续飞**（`(15,125,0)` → `(-9.3, 90.5, 0)`，`inGround=false`）✓
  —— 探针有效；
* 生成 Sable 平台（`/sable spawn platform 5`）后，读到一支箭 `inGround=true` 停在
  **(7, 129, 7)**，而 `/execute if block 7 129 7 minecraft:air` → **Test passed**，
  即那里主世界是空气 ⇒ 箭只能停在被 Sable 搬走的方块上 ⇒
  **服务端 `Level#clip` 确实能打到 sub-level** ✓（正是我们现在委托的调用）。

**没能做到**：把平台挪到受控位置做"有/无结构"的对照射击——Sable 的 sub-level 参数语法没试出来
（`/sable spawn platform <size>` 可用，但 `/sable sub_level get @e` 一直 `Incorrect argument`；
`SubLevelSelectorType` 的字符是 e/i/n/r/v/l/t，解析器却要别的形式）。
**最终确认请玩家在客户端朝 Sable 结构打一发。**

## 20.4 顺带记录的环境事实

* **RCON 的命令源在 (0,0,0)**（不是世界出生点地面）。按命令源定位的指令（如
  `/sable spawn platform 5`）必须写成 `/execute positioned <x> <y> <z> run ...`。
* Sable sub-level 里的实体**不在主世界实体列表**里，`@e` 选择器看不到，
  用 `data get entity @e[...]` 判断"结构里的实体"不可靠。

## 20.5 已知限制（**没有**动，避免臆造）

* 本模组弹射物继承 `Entity` 而**不是原版 `Projectile`**（0.5.5 的设计），所以 Sable 针对
  `Projectile` 的 `ProjectileMixin`（开火前清零射手速度等）对它们不生效。若要让弹射物
  完全跟随物理结构运动，需另做——Sable 提供 `api.SubLevelHelper`
  （`pushEntityLocal`/`popEntityLocal`/`getVelocityRelativeToAir`）与
  `api.entity.EntitySubLevelUtil`（`kickEntity` 等）作为入口。
* `PenetratorBlockEntity` 这类"按主世界方块位置工作"的机器同样只看主世界，
  放在物理结构上不会作用于结构内方块。同类问题，与本轮反馈无关，未动。

# 21. 物理结构上的炮塔"只能朝一个方向打"：坐标系混用（**可修，已修**）

## 21.1 根因

玩家反馈：装在物理结构上的炮塔**能识别到敌人**，但**只能朝固定方向射击**。根因是**两套坐标系混用**：

* Sable 把方块搬进 sub-level（"plot"），所以**方块实体**的 `worldPosition` / `getBlockPos()`
  是**结构自身坐标系**（plot 坐标）；
* 而**实体**（玩家/怪）在 `level` 里报的是**世界坐标**；
* 炮塔的瞄准数学是 `dx = smoothedTargetX - (worldPosition.getX() + 0.5)`（两个类各 4 处），
  等于拿"世界坐标 − 结构坐标"⇒ 差值被整条结构的位移主导，**目标怎么动，算出来的角度几乎不变**
  ⇒ 表现就是"锁定敌人但只朝一个方向打"。
* 同一处还把 plot 坐标当世界坐标用：散布/枪口位置、开火音效、击中电弧粒子、
  muzzle flash 发包用的 chunk，全都会跑到结构在 plot 里的那个"影子位置"。

## 21.2 修法：加一个坐标系转换工具，把射线/发射/特效都搬回世界坐标

新增 `top.ribs.scguns.util.PhysicsStructureHelper`（**只有入口暴露 vanilla 类型**，
调用方不需要碰 Sable 的类型）：

```java
Vec3 toWorld(Level, BlockPos localPos, Vec3 localPoint);   // 结构坐标 -> 世界，不在结构里则 null
Vec3 toLocal(Level, BlockPos localPos, Vec3 worldPoint);   // 世界 -> 结构坐标，同上
```

实现用的是 Sable 的**官方 API**：`SubLevelContainer.getContainer(level)` →（`inBounds` 守卫）
→ `getPlot(new ChunkPos(pos))` → `plot.getSubLevel()` → `subLevel.logicalPose()`，
再调 `Pose3dc.transformPosition` / `transformPositionInverse`（companion 库，见 21.3）。

改的点（`tools/patch_turret_frames.py`，两个炮塔类各 9 处，脚本**幂等**、逐条断言锚点唯一）：

| 位置 | 改法 |
|---|---|
| `findTarget` 里保存 `smoothedTarget*` 的地方（**只此一处**） | 先把预测点转成炮塔自身坐标再存 ⇒ 后面所有 yaw/pitch/距离计算自动同框 |
| `hasLineOfSight` 的起点 | 用转换后的**世界**坐标（射线本来就在世界里） |
| `fire()` 的 `muzzlePos` | 局部枪口 → **世界**（弹药、弹道、muzzle flash、音效都用它） |
| 开火音效 | 从 `worldPosition`（plot）改为世界枪口位置 |
| 被电击时的电弧粒子 + 音效 | 同样转成世界坐标（否则特效出现在结构在 plot 里的影子处） |
| muzzle flash 的 `sendToTrackingChunk` | 按**世界**位置的 chunk 发（plot chunk 没有任何玩家在跟踪） |

## 21.3 编译期细节：Sable 的 API 库是 jar-in-jar

`dev.ryanhcode.sable.companion.*`（`Pose3d`、`SubLevelAccess`…）**不在 sable 的 jar 里**，
它在 `META-INF/jarjar/sable-companion-common-1.21.1-1.6.0.jar`（NeoForge 运行时会加载，
但 Gradle 不会自动放进编译类路径）。做法：

```
python tools/extract_jarjar.py libs/sable-neoforge-1.21.1-2.0.5.jar libs-compile
# -> libs-compile/sable-companion-common-1.21.1-1.6.0.jar（纳入版本库，35 KB）
```

`build.gradle` 里只加 **`compileOnly`**：

```groovy
compileOnly files('libs-compile/sable-companion-common-1.21.1-1.6.0.jar')
```

刻意**不加 `localRuntime`**：开发运行时由 Sable 自己的 jarjar 提供这些类，避免同一个类被加载两次。

（另一个坑：`tools/javap_class.py` / `javap_methods.py` 是为此写的——Windows shell 会把
`$` 和长 `-cp` 弄坏，导致 `javap 'a.b.Outer$Inner'` 报 "class not found"。）

## 21.4 验证

* 静态：`javap` 确认 `SubLevel.logicalPose()` 返回 `Pose3d`，`Pose3dc` 提供
  `transformPosition`/`transformPositionInverse`/`transformNormal`；`SubLevelContainer` 提供
  `getContainer`/`inBounds`/`getPlot`；`LevelPlot.getSubLevel()`。
* 产物：`tools/verify_installed_jar.py` 新增 3 条（helper 类在包里、两个炮塔类引用它）→ **42/42**。
* 代码：`javac` 0 错误、`gradlew build` 成功、14 个审计全 0。
* **实机部分需要玩家确认**：把炮塔装在物理结构上，看它是否随目标转动、弹道是否从炮口射出。
  （服务端 RCON 无法在一次会话里可靠地"组装一个含炮塔的 sub-level + 放一个目标"，
  这一条留给客户端。）

## 21.5 同一类问题还没动的地方（都属于"效果落在 plot 影子位置"）

`AmmoModuleBlockEntity` / `ShellCatcherModuleBlockEntity` 的粒子锚点、
`PolarGeneratorBlockEntity` 等方块实体的音效、`PenetratorBlockEntity` 的掘进，
以及各种 `level.playSound(null, this.worldPosition, ...)`。它们**不影响玩法判定**，
按"不做臆造"的原则没有一起改；将来若要收拾，套 `PhysicsStructureHelper.toWorld` 即可。

# 22. 子弹打中物理结构会把它推动/打滚（新功能）

## 22.1 需求与最终做法：**以"拳头的力"为单位**

玩家提出"射弹命中物理结构也许可以让这个结构滚动"，随后又给了**最好的校准参照**：
"**玩家空手左键物理结构产生的力是最好的**"。

于是去读 Sable 自己的出拳代码（`net.minecraft...ServerboundPunchSubLevelPacket.handle`），
它的冲量是：

```java
// Sable 原版逻辑（反编译）
double scale     = player.getAttribute(SableAttributes.PUNCH_STRENGTH).getValue();   // 默认 1.0
double strength  = punchCurve(normalMass) * SableConfig.SUB_LEVEL_PUNCH_STRENGTH_MULTIPLIER.getAsDouble();
double normalMass = 1.0 / massData.getInverseNormalMass(point, normal);
impulse = direction * (scale * strength);
```

* `punchCurve(double mass)` 是 **public static**（在 `ServerboundPunchSubLevelPacket` 里）——
  一条**随质量次线性增长**的曲线，这正是"打小船和打大船手感都合理"的原因；
* `SableAttributes.PUNCH_STRENGTH` 默认 **1.0**；
* 于是本模组的实现变成：**冲量 = punchCurve(法向质量) × Sable 的拳力系数 × 本次射击相当于几拳**，
  配置项 `gameplay.physicsStructureImpulse` 的单位就是**拳**（1.0 = 每 10 点伤害一拳）：

```java
// ProjectileEntity / TurretProjectileEntity
double punches = config * damage / 10.0;      // 10 伤害的步枪 = 正好一拳
PhysicsStructureHelper.applyShotImpulse(level, hitVec, deltaMovement, punches);
```

**数值感（把 punchCurve 复原后算的，拳力系数 k≈2.1 由 §22.6 实测反推）**：

| 结构质量 | 一拳的冲量 | 一拳带来的速度变化 |
|---|---|---|
| 5 kg | 6.76 | 1.35 m/s |
| 25 kg | 18.1 | 0.72 m/s |
| 242 kg（测试平台） | 62.3 | 0.26 m/s |
| 5000 kg（大船） | 293.8 | 0.059 m/s |

## 22.2 上一版为什么会让结构"直接消失"（教训）

上一版把 **`damage × 系数` 直接当冲量**丢给刚体——这个数**与刚体的质量模型毫无关系**，
也完全没参考 Sable 的手感曲线；同时**没有任何上界**。玩家实测：打上去结构**直接不见了**。

现在有三层保护，全部写成"速度"再换算回冲量（`PhysicsStructureHelper`）：

1. **质量曲线**：用 Sable 自己的 `punchCurve`，重结构本来就不动；
2. **单次上限**：一次命中最多给结构 **2 m/s**（常量 `MAX_SPEED_GAIN_PER_HIT`），
   所以霰弹一次 8 颗、跳弹连续命中都不会把结构"弹飞"；
3. **总速度上限**：`gameplay.physicsStructureMaxSpeed`（默认 **4** 格/秒，见 §22.6 的实测依据），
   超过就不再加速——**连射也不可能把结构推出世界**。

另外：`mass <= 0`（静态子级）直接跳过；所有输入做有限性检查（NaN 会让物理引擎彻底崩坏）；
命中点必须落在结构自身包围盒内（于是力臂、也就是旋转量，被结构尺寸天然限制住）；
调用点再包一层 `try/catch`，**物理兼容出问题绝不能让开枪本身崩掉**。

## 22.3 关键坑：**进入结构的弹丸会被 Sable 搬进结构的坐标系**

第一次实测时诊断日志打出的命中点是 `(20481037, 128.9, 20485128)` —— plot 坐标：

* 弹丸**飞进结构**的那一刻被 Sable 收进该结构的坐标系，于是
  `position()` / `HitResult.getLocation()` / `getDeltaMovement()` 全变成 **plot 空间**；
* 而 `RigidBodyHandle.applyImpulseAtPoint` 要的是**世界**坐标 ⇒ 直接用会打在几十万格之外。
* `PhysicsStructureHelper.applyShotImpulse` 因此先做帧转换：点用 `transformPosition`、
  **方向用 `transformNormal`**（方向不能带平移），不在结构里时按世界坐标原样使用。

> 与 §21 的炮塔问题是同一主题的两面：结构内方块实体是 plot 坐标、结构外实体是世界坐标、
> **进入**结构的弹丸又是 plot 坐标。写结构相关代码前先问"我这个坐标是哪一帧的"。
>
> **而且有两个坐标要各自检查**（§22.7 的教训）：Sable 的管线同时要"质量查询用的点"
> 和"施力用的点"，两个都得是**结构局部**坐标；只改对其中一个，症状分别是"完全没效果"
> （质量查询错 ⇒ 冲量 ≈1e-11）和"结构被扔上天"（施力点错 ⇒ 力臂 2.9e7 格）。


## 22.4 验证到什么程度

`tools/rcon_sable_impulse_check.py`（全服务端，不需要玩家）：用
`/sable storage find_all_sub_levels` 读所有子级位置、`/sable info <uuid>` 读质量/线速度角速度、
`/summon scguns:basic_turret`（实体 id 是 `basic_turret`，不是 `turret_projectile`）发射炮弹，
并用原版箭做对照。**炮弹用 `TurretDamage 10`** —— 配置单位就是"每 10 伤害一拳"，
所以这才是"一拳"的参照；早先测试用的 500 伤害等于 50 拳，只能证明"离谱的冲量确实能把东西打飞"。

* ✅ **命中确实到达刚体**：诊断日志 `applying strength=32.335 magnitude=32.335 punches=1.0`
  —— `magnitude == strength`，说明**三层上限一个都没削减**，送进物理管线的就是
  Sable 出拳时那个数（同一次命中的 `normalMass=70.82`，位置靠边，所以比正面命中小）；
* ✅ **量级等于 Sable 自己的出拳冲量**（见 §22.1 表 + §22.6 的反推）；
* ✅ **调用路径与出拳完全相同**（`javap -c` 核对）：`RigidBodyHandle.applyImpulseAtPoint`
  → `PhysicsPipeline.applyImpulse(body, point, impulse)`，和
  `ServerboundPunchSubLevelPacket.handle` 走的是同一个接口方法；
* ⚠️ **仍未能观察到结构真的位移**：专用服务器上这些命令生成的平台**根本不参与模拟**
  （对照组的 6 支原版箭同样让它"位移 0.000 格"）。所以"打上去会不会滚"依旧请玩家在
  **有玩家在旁的真实机械**上确认——玩家在场时 Sable 才会真正推进该刚体。

## 22.5 顺带记下的 Sable 命令/API 用法（省得再摸）

```
/sable storage find_all_sub_levels     # 所有子级：uuid、世界位置、世界包围盒
/sable info <uuid>                    # 位置、朝向、质量、线速度、角速度
/sable forceload add <uuid>           # 加载票据（要求结构已加载）
/sable spawn platform 5               # 生成 5x5 测试平台（落在世界出生点，不是命令源）
/sable physics impulse <sub_level> linear <x> <y> <z>
```

只用到的公开 API：`SubLevelContainer.getContainer/inBounds/getPlot`、`LevelPlot.getSubLevel`、
`SubLevel.logicalPose`、`Pose3d.transformPosition/transformPositionInverse/transformNormal`、
`ServerSubLevel.getMassTracker`、`MassData.getMass/getInverseNormalMass`、
`RigidBodyHandle.of(ServerSubLevel).applyImpulseAtPoint(point, force)`（**参数顺序是"点、力"**，
用 `javap -l` 的 LocalVariableTable 核对过）、`ServerboundPunchSubLevelPacket.punchCurve`、
`SableConfig.SUB_LEVEL_PUNCH_STRENGTH_MULTIPLIER`。

## 22.6 玩家反馈"没有效果"的**真正**根因：法向质量算在了错误的坐标系（已实测）

上一版（`.bak-punchframe`）虽然已经改成"以拳为单位"，但 `getInverseNormalMass` 收到的是
**世界坐标**的点与法向，而 Sable 要的是**结构自身坐标系**（它的出拳处理器先调
`transformPositionInverse` / `transformNormalInverse` 再查质量）。结果不是报错，而是**静默失效**：

同一次命中，两个坐标系各算一遍（临时诊断，已删除）：

```
localInv=0.014120  localNormalMass=70.82   worldInv=3.411e11  mass=242.0
applying strength=32.335 magnitude=32.335 punches=1.0
```

| 传入的坐标系 | `getInverseNormalMass` | 法向质量 | `punchCurve` 后的冲量 |
|---|---|---|---|
| 结构局部（**现在**） | 0.01412 | 70.82 | **32.335**（= 一拳，正是玩家认可的力度） |
| 世界（**上一版**） | 3.41e11 | 2.9e-12 | ≈ **1e-11**，等于零 |

也就是说上一版打上去的冲量是 `1e-11` 量级——玩家看到的"完全没效果"就是这个；
上一轮我把"结构消失"归因于冲量过大，其实那一版真正的毛病是**太小**（`punchScale` 之外
还有一个坐标系错误），这点必须写下来，免得再犯同样的归因错误。

反推 Sable 的拳力系数（两处独立数据互相印证）：

* 质量 242、正面命中：`punchCurve(241.94) × k = 62.255`（50 拳 → 3112.75）；
* 质量 242、偏心命中：`punchCurve(70.82) × k = 32.335`（1 拳）；
* 我复原的曲线在这两点给出 29.65 / 15.40 ⇒ **k ≈ 2.1**（即
  `SableConfig.SUB_LEVEL_PUNCH_STRENGTH_MULTIPLIER` 的当前取值）。

于是**一拳带来的速度变化**（配置默认 `physicsStructureImpulse = 1.0`）：

| 结构质量 | 一拳冲量 | 速度变化 |
|---|---|---|
| 5 | 6.76 | 1.35 m/s |
| 70（偏心命中） | 32.3 | 0.46 m/s |
| 242（测试平台） | 62.3 | 0.26 m/s |
| 5000（大船） | 293.8 | 0.059 m/s |

连射时的总上限因此从 8 格/秒下调为 **4 格/秒**：30 发弹匣全中最多让 242 质量的平台
累积到 4 格/秒（0.26 × 30 = 7.7，被上限截住），既看得出来，也不会把它推出世界。

**验收这一步怎么自测的**：`tools/verify_installed_jar.py` 新增 4 项检查（§22.6 的坐标系、
`punchCurve`、拳力系数、速度上限），并用 `SCGUNS_JAR=` 指向修复前的旧 jar 跑了一遍——
旧 jar 在"impulse normal is converted into the structure frame"上**必须** FAIL
（实测 49/50），现在装着的 jar 50/50 通过。

## 22.7 玩家的第二次实测：结构被抛到 y=14022 —— **施力点也必须用结构局部坐标**

玩家截图（`/sable storage find_all_sub_levels`）里有两个结构停在
`269.9 / 14022.7 / -7238.6` 和 `410.1 / 1719.4 / 788.8`，也就是说刚被"修正过冲量"的版本
**仍然把结构扔上了天**。这次的原因和 §22.6 是同一个主题、**另一个参数**：

Sable 的出拳处理器把 **`localPosition` / `localDirection`**（`transformPositionInverse` /
`transformNormalInverse` 的结果）直接交给 `PhysicsPipeline.applyImpulse(body, point, impulse)`
——用 `javap -l` 的 LocalVariableTable 核对过这两个变量名。也就是说**管线读的是结构自身坐标**
（它就是 plot 坐标，数量级 2e7），不是世界坐标。§22.6 我只把**质量查询**改成了局部坐标，
**施力点却还在传世界坐标** ⇒ 线性冲量是对的，但**力臂错成了 2e7 格**：

```
leverLocal=4.94  leverWorld=2.897e7  size=15.10  strength=32.45 magnitude=32.45 mass=242 punches=1
```

| 传给管线的点 | 力臂 | 后果 |
|---|---|---|
| 结构局部（**现在**） | 4.94（结构尺寸 15.1 之内） | 扭矩被几何天然限制，ω 约 0.03 rad/s |
| 世界坐标（**上一版**） | 2.897e7（结构的 600 万倍） | 角冲量 ≈ r×p，而惯量只由结构本身决定 ⇒ ω 爆表、解算器发散 ⇒ 抛到 y=14022 |

这也解释了**最早那次"结构直接消失"**：只要力臂是错的，线性冲量再小也压不住自旋。
三个"速度上限"（单次 2 m/s、总 4 m/s）**只管线速度、管不了角速度**，所以挡不住这种爆炸——
这一点必须在文档里留着：**给刚体施力时，"力臂属于哪一帧"和"力有多大"一样重要。**

修法：`applyPunchAt` 现在把命中的世界点/法向用
`pose.transformPositionInverse` / `transformNormalInverse` 转成局部坐标再交给
`handle.applyImpulseAtPoint(localPoint, localNormal.normalize().mul(magnitude))`，
与出拳处理器逐参数一致（含方向向量本身也用局部系）。

**玩家世界里的善后**：那两个结构还在存储里，Sable 自带命令可以处理（`javap` 看到字面量）：

```
/sable sub_level remove <uuid>          # 删掉被扔飞的结构（截图里 y=14022、y=1719 那两个）
/sable sub_level teleport <targets> <x> <y> <z> [angle]   # 或者拖回来继续用
/sable sub_level get name <sub_level> / set name / clear name
```

**这一步的验收自测**：新增源码审计 `tools/audit_physics_impulse_frame.py`——
一旦 `applyImpulseAtPoint` 的第一个参数不是 `local*`、或第二个参数不是局部值、
或 `getInverseNormalMass` 没吃到局部点，就报错。用 `git show 86a2d1f:…PhysicsStructureHelper.java`
取出修复前的源码跑它，**必须**报 3 个问题（实测：`impulse point is 'worldPoint'…`），
当前源码 0 问题。临时诊断（`leverLocal/leverWorld`）已删除，jar 里 `SCGUNS-PHYS` 命中数为 0。

# 23. Sulfurhead：半透明"凝胶层"不显示（**全模型共有的 1.21.1 迁移漏洞**）

## 23.1 症状与结论

玩家报："sulfurhead 设定里只能受到玩家伤害，半透明材质无法渲染。"

两句要分开看，结论也不一样：

* **"只能受到玩家伤害"——本来就实现了，而且是实测过的**（见 23.3）；
* **"半透明材质无法渲染"——真 bug，而且不止 sulfurhead**：**全部 21 个模型类、57 处调用**
  都把顶点颜色（连同 alpha）丢掉了（见 23.2）。

## 23.2 根因：1.21.1 改了模型渲染入口，移植只改了签名没改方法体

1.20.1 的入口是 `renderToBuffer(..., float red, float green, float blue, float alpha)`，
1.21.1 改成把颜色打包成一个 `int`：

```java
// 1.21.1
public void renderToBuffer(PoseStack, VertexConsumer, int packedLight, int packedOverlay, int color)
public void ModelPart.render(PoseStack, VertexConsumer, int packedLight, int packedOverlay, int color)  // 新增五参版
```

移植时签名跟着改了 ✓，方法体却是照抄旧的**四参** `render(...)`：
`part.render(poseStack, vertexConsumer, packedLight, packedOverlay)` —— **颜色参数直接丢掉**。
丢掉的后果不是报错，而是"按纯白 + 完全不透明"渲染：

* `SulfurheadGelLayer` 传的是 `colorFromFloat(0.4F, 1F, 1F, 0.3F)`（alpha 0.4 的半透明外壳），
  模型把它丢掉 ⇒ 外壳变成**不透明的白色**，玩家看到的就是"半透明材质没渲染"；
* `SulfurheadPrimeOverlayLayer`（引爆闪烁）同样丢色 ⇒ 闪烁颜色不对；
* 影响面是**所有**有分层/染色的模型（21 个类、57 处），只是硫磺头最明显。

修法：`tools/patch_model_vertex_color.py` 逐个找到带 `int color` 的 `renderToBuffer` 方法体，
把里面每处四参 `render(...)` 改成五参并补上 `color`（57 处 / 21 个文件）。

**先排除掉的两个"看起来更可疑"的假设**（都做了证据，省得以后再查一遍）：

* **纹理本身没坏**：`tools/compare_textures.py` 把原版 0.5.5 jar 里的 1080 张 PNG 与移植资源
  逐张比对（大小 + sha256）⇒ **1079 张完全一致**，唯一差异是早已记录的 0 字节
  `painting/the_collective.png`；`sulfurhead.png`(5786B) 与 `sulfurhead_gel.png`(292B) 都是**逐字节相同** ✓；
* **渲染代码抄得没错**：`tools/diff_against_055.py` 逐行比对 0.5.5 与移植的
  `SulfurheadRenderer/GelLayer/Model/Entity` ⇒ 差异只有 `new ResourceLocation(...)`→
  `fromNamespaceAndPath(...)` 与颜色 API 的换算，**唯一实质差异就是这个丢色的方法体** ✓
  （参考移植 `ScorchedGunsNeoforge-main` 的凝胶层写法也与我们的修法一致 ✓）。

## 23.3 "只能受到玩家伤害"：0.5.5 的做法本来就在，而且实测有效

0.5.5 与参考移植的做法一致：

```java
public boolean hurt(DamageSource source, float amount) {
   if (!this.level().isClientSide && !(source.getEntity() instanceof Player)) return false;
   return super.hurt(source, amount);
}
```

**为什么这条能挡住枪以外的伤害**：子弹的 `DamageSource` 是
`new DamageSource(holder, directEntity = 弹丸, causingEntity = 射手)`（`ModDamageTypes.Sources.projectile`），
所以 `getEntity()` 返回的是**射手**——玩家开枪时就是 `Player` ✓，而爆炸、火焰、怪物攻击、
掉出世界等都拿不到 Player ⇒ 一律 `false` ✓（连它自己引爆的爆炸也伤不到它 ✓）。

**实测**（`tools/rcon_sulfurhead_damage_check.py`，专用服务器 + RCON，用原版僵尸做对照）：

```
start: sulfurhead 30.0 hp, zombie 20.0 hp
after 3 non-player damage sources: sulfurhead 30.0 hp (-0.0), zombie 11.1 hp (-8.9)
RESULT: the sulfurhead ignores non-player damage, the control zombie does not
```

对照组证明"这个测试确实能测到伤害"，30.0 → 30.0 才是有效结论。
唯一没法在专用服务器上验证的是**正面情形**（玩家本人/玩家的子弹打上去应当掉血）——
不能 `/summon` 玩家；但这条走的就是 `super.hurt`，且 `getEntity()` 是玩家 ✓。

## 23.4 这一步的验收自测

* 新增 `tools/audit_model_vertex_color.py`：只要某个 `renderToBuffer(..., int color)` 的方法体里
  还留着四参 `part.render(...)` 就报错。**自测**：用 `git show HEAD:` 取出打补丁前的 21 个模型
  跑它 ⇒ 实测 **57 个问题**；打补丁后 ⇒ **0 问题**（23 个模型、57 处调用全部通过）；
* `tools/verify_installed_jar.py` 新增 4 项（五参 part render 存在、四参 descriptor **不再被引用**、
  凝胶层仍是 `entityTranslucent`），并**用旧 jar 自测**：旧的装好的 jar 实测 **54/57**，
  三项"passes the vertex colour to its parts"必须 FAIL ✓，新 jar **57/57** ✓。
  > 注意踩过的坑：一开始用"某 descriptor 出现次数 ≥2"来判定，结果**新旧 jar 都通过**——
  > class 文件的常量池会去重，同一 UTF-8 项只存一份，出现 20 次也只算 1 次。
  > 改成"四参 descriptor **不再出现**"才真正能区分 ✓（"不能失败的检查等于没有检查"）。

## 23.5 按玩家要求去掉"只受玩家伤害"（**有意偏离 0.5.5**）

玩家明确要求："我想把 sulfurhead 只受玩家伤害的特性去掉"。删掉的就是这一段（0.5.5 原文）：

```java
public boolean hurt(DamageSource pSource, float pAmount) {
   if (!this.level().isClientSide && pSource.getEntity() instanceof Player) {
      return super.hurt(pSource, pAmount);
   } else {
      return !this.level().isClientSide && !(pSource.getEntity() instanceof Player) ? false : super.hurt(pSource, pAmount);
   }
}
```

删掉后走 `Mob.hurt`，与普通怪物一致。**回滚方法**：把上面的方法原样放回
`SulfurheadEntity`（`performRangedAttack` 之后、`registerGoals` 之前）即可；
同样的代码块也留在源码注释里（§23.5 位置）以免以后翻文档。

**副作用检查（确认没有连带伤害）**：这个怪并不会用 `level.explode` 自爆，它的"喷发"是
`spawnGasCloudAndDie()` —— 生成毒气云后 `this.discard()`，所以**不存在"被自己的爆炸打死"**的问题；
唯一的行为变化是 `HurtByTargetGoal` 现在会反击任何伤害它的来源（而不是只反击玩家），这正是
"与普通怪物一致"的含义。

**实测（`tools/rcon_sulfurhead_damage_check.py`，脚本语义已随本次改动反转）**：同一组
非玩家伤害源打硫磺头和对照僵尸，两者掉血应当接近：

```
after 3 non-player damage sources: sulfurhead 30.0 hp (-x.x), zombie 20.0 hp (-x.x)
RESULT: the sulfurhead now takes non-player damage like any other monster
```

（脚本内置"对照僵尸必须掉血"的断言：对照组不掉血就判定测试无效，而不是当成通过。）

**验收自测**：`verify_installed_jar.py` 新增 1 项 —— `SulfurheadEntity.class` 里
**不应再出现** `(Lnet/minecraft/world/damagesource/DamageSource;F)Z` 这个描述符
（该描述符只在声明/调用该重写时进常量池）。用**改动前**的 jar 跑它必须 FAIL（实测如此），
改动后通过 ✓。

# 24. 手榴弹右键直接爆炸 —— **1.21.1 改了 `getUseDuration` 的参数**（同类 bug 第 3 次）

## 24.1 症状与根因

玩家报："手榴弹类武器无法正常丢出，右键直接爆炸。"

1.21.1 把 `Item` 的这个方法**加了一个参数**：

```java
// 1.20.1
public int getUseDuration(ItemStack stack)
// 1.21.1（用 javap 核对过，1.21.1 只有这一个）
public int getUseDuration(ItemStack stack, LivingEntity entity)
```

移植沿用了 1.20.1 的一参签名 ⇒ **它不再重写任何东西**，变成一个没人调用的私有辅助方法，
而引擎拿到的是父类默认值 **0 tick**。于是 `startUsingItem` 下一 tick 就"用完"了，
直接进 `finishUsingItem` —— 那里面是 `grenade.onDeath()`，也就是**在手里炸** ✓✓，
而且**根本走不到 `releaseUsing`（投掷）那一步**，所以"丢不出去" ✓✓。

**为什么之前一直没被发现**：物品内部调用的是 `this.getUseDuration(stack)` ✓，
它解析到**本类自己的那个一参方法**（返回 `maxCookTime` ✓），所以引信计算、动画时长
看起来全都自洽 ✓，只有**引擎**看到的是 0 ✗ —— 典型的"编译通过、逻辑自成一体、就是不管用"。

## 24.2 受影响的不止手榴弹

全树 **16 处**同一个签名问题 ⇒ 所有"需要按住一会才生效"的物品都变成**瞬间完成**：

| 物品 | 原本的行为 | 之前实际 |
|---|---|---|
| `GrenadeItem` / `StunGrenade` / `GasGrenade` / `BeaconGrenade` / `ChokeBomb` / `NailBomb` / `SwarmBomb` / `HellfireBomb` / `MolotovCocktail` / `ThrowableShotball` | 右键**开始拉弦**，松手投掷，弦烧完在手里炸 | **右键立刻在手里炸**（玩家报的 bug） |
| `HealingBandageItem` / `ColdPackItem` / `AirCanisterItem` / `FuelAmmoItem` | 按住一段时间完成 | 瞬间完成 |
| `MetalDetectorItem` / `RangeFinderItem` | 按住扫描 | 瞬间扫描 |
| `GunItem` | `getGeneral().getRate() * 4`（枪的近战/使用动作时长） | 瞬间完成 |

**修法**（`tools/patch_get_use_duration.py`）：16 处改成两参签名 + **补上 `@Override`**，
并把 18 处内部调用改成传实体（`onUseTick` 传 `player`、`releaseUsing` 传 `entityLiving`）。
**补 `@Override` 本身就是验收**：签名只要再漂一次，javac 直接编译失败，
不会再出现"静默失效"（这次编译报的 18 个错就是这么发现的）。

## 24.3 防复发：`tools/audit_stale_overrides.py`

这是本移植**最贵的一类 bug**（已发生 3 次：`finalizeSpawn` 多一个 Forge 参数、
`getUseDuration` 多一个实体参数、`renderToBuffer` 颜色参数被丢），共同形态是
**"方法签名过期 ⇒ 编译通过 ⇒ 什么都没重写"**。所以写了一个通用检查：

1. 自己解析 class 文件（常量池 + 方法表 + 父类/接口 ✓，纯 Python，不需要 javap）✓；
2. 对每个模组类，沿**父类 + 接口**的祖先图（传递闭包 ✓）查找**同名但描述符不同**的方法
   ⇒ 命中即报 ✓；
3. 祖先信息**合并所有声明该类的 jar**（NeoForge 通过 `IBlockExtension` 之类的接口给原版类
   加了很多重载，只看单个 jar 会把合法重写全部误报 ✓）；
4. 判定规则（**先写了一版错的，这里记录教训**）：第一版用"同名但签名不同"就报，
   再用一个"基线文件"把 277 条现存命中记下来当作"已复核" ✓ —— 结果那个基线把**两个**
   `getDefaultAttributeModifiers(ItemStack)` 命中一起吸收了，而它们其实是
   **NeoForge `IItemExtension` 的默认方法**（1.21.1 确实存在 ✓，实测 `iron_bayonet` 属性正常 ✓）
   ⇒ 不是 bug ✓。**教训：不要用"把现状记成基线"当门禁**，那是把未知当成已知 ✓。
   现在的规则是"**任何祖先都没有这个精确签名**才算可疑"（同名+精确签名都查 ✓），
   并额外提供 `--prefix-only`：只列"参数表是另一个的前缀"的命中（也就是"API 多/少了一个参数"
   这种真正的过期形态 ✓，`getUseDuration(ItemStack)` vs `(ItemStack, LivingEntity)` ✓）。
   ⚠️ 它仍是**诊断工具**而不是门禁：实测 6 条可疑项逐条读代码后全部排除
   （`onDataPacket(2参)` 是委托给 3 参版的辅助重载 ✓、`getData()`/`getDirection(...)`/`getDamage()`
   是同名的模组方法 ✓、paxel/bayonet 那条是 NeoForge 接口默认方法 ✓）。
   真正的门禁是**编译期**：修好的方法一律加 `@Override`，签名再漂就直接编译失败 ✓。


**自测（关键）**：把改动前那个 jar 的 class 解出来再跑同一个检查
（解包命令：`python -c "import zipfile,pathlib; z=zipfile.ZipFile(r'<旧jar>'); out=pathlib.Path('build-logs/classes-before-use'); [ (out/n).parent.mkdir(parents=True, exist_ok=True) or (out/n).write_bytes(z.read(n)) for n in z.namelist() if n.startswith('top/ribs/scguns/') and n.endswith('.class') ]"`，
然后 `SCGUNS_CLASSES=build-logs/classes-before-use python tools/audit_stale_overrides.py` ✓）
⇒ 实测报出 **16 条 NEW**，而且每条都精确指向
`item/GrenadeItem#getUseDuration(Lnet/minecraft/world/item/ItemStack;)I vs
net/minecraft/world/item/Item(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)I`
✓✓ —— 也就是说这个工具**本来就能提前抓到这个 bug**；当前代码跑 ⇒ **0 new** ✓。
检查当前 6290 个方法与 995 个无法解析的祖先链（模组自身类在 classpath 上找不到，属正常 ✓）。

**验收**：`gradlew build` 成功（`@Override` 通过编译 = 签名正确 ✓）、18 项审计全清、
`verify_installed_jar` 新增 6 项（`GrenadeItem`/`MolotovCocktailItem`/`SwarmBombItem`/
`NailBombItem`/`GunItem`/`HealingBandageItem` 含两参描述符 ✓），用改动前的 jar 跑必须 FAIL
（实测 58/64）✓，装上后 64/64 ✓，`runServer` 到 `Done` 且之后 0 ERROR/FATAL ✓。

**未验证的部分**：真正的"右键投掷"需要玩家客户端（专用服务器不能 `/summon` 玩家）——
但机制上现在**引擎能看到 60 tick 的拉弦时间**（`GRENADE` 的 `maxCookTime` ✓），
投掷路径 `releaseUsing` 也才会被调用 ✓；`@Override` 让签名由编译器把关 ✓。

# 25. Anthralite 系列工具"没有属性" —— 1.21 把工具属性挪进了物品组件

## 25.1 症状与根因

玩家报："anthralite 系列工具没有属性。"

1.20.1 把攻击力/攻速**作为构造参数**交给工具类：

```java
new PickaxeItem(ModTiers.ANTHRALITE, 1, -2.8F, new Properties())   // 0.5.5
```

1.21 把这两个数字挪进了物品的 **`ATTRIBUTE_MODIFIERS` 组件**，工具类构造函数只收 `(Tier, Properties)`：

```java
new PickaxeItem(ModTiers.ANTHRALITE,
    new Properties().attributes(PickaxeItem.createAttributes(ModTiers.ANTHRALITE, 1, -2.8F)))   // 1.21.1
```

移植时**只删掉了两个数字、没有补 `.attributes(...)`** ⇒ 这 5 件工具**一条属性修饰符都没有**：
攻击力和攻速退回裸手水平（"没有属性" ✓），但挖掘等级/挖掘速度来自 `Tier` ⇒ 挖矿看起来还是正常的，
所以很容易被忽略。

**受影响的 5 件**（`ModItems` 里直接 `new` 原版工具类的那几件）：
`anthralite_pickaxe` / `anthralite_sword` / `anthralite_axe` / `anthralite_shovel` / `anthralite_hoe`。
模组自己的工具类（`WaraxeItem`、`CogMaceItem`、`AnthraliteHammerItem`）**早就改成属性组件了** ✓ ——
只有这几件"直接用原版类"的漏了 ✓。

## 25.2 数值（沿用 0.5.5，未改平衡）

| 物品 | 0.5.5 构造参数 | 现在的属性修饰符（实测） |
|---|---|---|
| `anthralite_pickaxe` | `(1, -2.8F)` | `attack_damage=3.50` `attack_speed=-2.80` |
| `anthralite_sword` | `(3, -2.4F)` | `attack_damage=5.50` `attack_speed=-2.40` |
| `anthralite_axe` | `(5.0F, -3.0F)` | `attack_damage=7.50` `attack_speed=-3.00` |
| `anthralite_shovel` | `(1.5F, -3.0F)` | `attack_damage=4.00` `attack_speed=-3.00` |
| `anthralite_hoe` | `(-3, -3.0F)` | `attack_damage=-0.50` `attack_speed=-3.00` |

（表里是**修饰符本身**；实际伤害还要加上玩家的 1 点基础，例如剑 = 1 + 5.5 = 6.5，
与 0.5.5 的 `1 + 3 + 2.5(ANTHRALITE 的 tier 加成)` 完全一致 ✓）

## 25.3 怎么"量"出来的（不靠肉眼）

临时加了一个 **GAME 总线**上的 `ServerStartedEvent` 诊断，用战斗同一套查询
（`ItemStack#forEachModifier(EquipmentSlotGroup.MAINHAND, …)`）把每件工具的有效属性打出来：

```
[SCGUNS-ATTR] scguns:anthralite_pickaxe -> attack_damage=3.50 ADD_VALUE  attack_speed=-2.80 ADD_VALUE
[SCGUNS-ATTR] scguns:anthralite_sword   -> attack_damage=5.50 ADD_VALUE  attack_speed=-2.40 ADD_VALUE
...（5 件全部有值；修之前这里会是 `<NO ATTRIBUTES>`）
```

> 踩坑记录：第一次把诊断放进 `ModCommonEventBus`（`@EventBusSubscriber(bus = Bus.MOD)`），
> 结果**一行日志都没有**——`ServerStartedEvent` 是 **GAME 总线**事件，MOD 总线的订阅者根本不会收到 ✓。
> 第二次终于跑起来，却在 `ANTHRALITE_PAXEL` 上**空指针崩服**——因为
> `anthralite_paxel` / `anthralite_knife` 是**条件注册**的（分别要求 Create: Iron Works 与 Farmer's Delight），
> 字段在没装那个模组时是 **null** ✓。诊断最终只覆盖必然存在的物品 ✓，临时类已删除
> （jar 里 `SCGUNS-ATTR` 命中数为 0 ✓）。

**顺带确认了两件本来担心的事**：

* `AnthralitePaxelItem` / `BayonetItem` 用的是 `getDefaultAttributeModifiers(ItemStack)` 重写
  （NeoForge 的 `IItemExtension` 默认方法 ✓，1.21.1 确实存在 ✓）——实测 `scguns:iron_bayonet`
  打出了 `attack_damage=1.50` ✓ ⇒ **这条路径是生效的** ✓，两者都没问题（它们的数字也与 0.5.5 一致 ✓）；
* `anthralite_paxel` 只在装了 Create: Iron Works 时才注册 ✓ ——玩家若没装这个模组，
  "paxel 没属性"根本不是 bug（它不存在 ✓）。

## 25.4 防复发 + 验收

* 新增 `tools/audit_item_attributes.py`：扫描 `ModItems.java` 的每个 `REGISTER.register(...)`，
  凡是 `new PickaxeItem/AxeItem/ShovelItem/HoeItem/SwordItem(...)` 而整段里没有 `.attributes(`
  的一律报错 ✓。**自测**：用 `git show HEAD:…ModItems.java`（补丁前）跑 ⇒ 实测报 **5 条**
  （pickaxe/sword/axe/shovel/hoe ✓），当前源码 ⇒ **0 条** ✓；
* `verify_installed_jar.py` 新增 1 项：`ModItems.class` 里必须出现
  `(Lnet/minecraft/world/item/Tier;IF)Lnet/minecraft/world/item/component/ItemAttributeModifiers;`
  （即 `(Tier, int, float)` 形式的 `createAttributes` ✓）。**自测**：改动前的 jar 该项 **FAIL**
  （实测 65/66）✓，新 jar 66/66 ✓。
  > 同一位置本来还想加一个 `(Tier;FF)` 的检查，实测**新旧 jar 都通过** ⇒ 因为刀（knife）早就用了
  > 那个 helper ⇒ 这种"不可能失败的检查"直接删掉，不留在门禁里充数 ✓。

# 26. 施力点改为**射手（玩家）所在位置**——与 Sable 出拳完全同构

## 26.1 玩家指出的问题

玩家报："射弹推动物理结构不会计算玩家的位置。"

核对 Sable 出拳的字节码（`javap -c -l ServerboundPunchSubLevelPacket.handle`）后确认玩家说得对：

```
320: pose.transformPositionInverse(player.position())   →  局部变量 17  = localPosition
329: pose.transformNormalInverse(direction)             →  局部变量 18  = localDirection
342: computeStrengthScalar(subLevel, localPosition, localDirection)      ← 质量也在玩家位置查
358: aload 17 (localPosition)  ← 施力点
377: PhysicsPipeline.applyImpulse(body, localPosition, localDirection * 强度)
```

**出拳并不是"打在哪个方块就在哪里施力"** —— 它是把力施加在**玩家自己所站的位置**（并沿玩家视线方向），
质量查询用的也是这个点 ✓。而我们的射弹用的是**命中点** ✗，所以结果与玩家站位无关 ✓。

玩家选择的口径：**完全照出拳**（一律用玩家位置，含质量查询）✓ ——本节即按此实现。

## 26.2 改法

* `PhysicsStructureHelper.applyShotImpulse(level, hitPoint, forcePoint, direction, punches)`：
  `hitPoint` 只用来**找到结构**（射手通常在结构外，不能拿它当判定点 ✓），
  `forcePoint` 用来**查质量 + 施力** ✓（世界坐标，内部统一转成结构局部坐标 ✓，与出拳逐参数一致 ✓）；
* `ProjectileEntity` 传 `this.shooter.position()` ✓（射手是玩家时就是玩家位置 ✓；射手为空
  ——发射器、炮塔、丢失射手的弹丸——则由 helper 回退到命中点 ✓）；
  `TurretProjectileEntity` 传 `getOwner()` 的位置 ✓（炮塔弹丸现在没有 owner ⇒ 回退命中点 ✓）；
* **新增自旋上限** `gameplay.physicsStructureMaxSpin`（默认 3.0 rad/s）+ 单次上限
  `MAX_SPIN_GAIN_PER_HIT = 1.5 rad/s` ✓：因为"施力点在射手处"意味着**远距离射击的力臂可以很长**，
  这是出拳从不遇到的问题（拳头只够 5 格 ✓）。诱发角速度用 Sable 自己的**惯量张量**精确算出
  （`MassData.getInverseInertiaTensor` ✓），超限就把冲量按比例缩小 ✓ —— 也就是说
  **正常出拳距离内这个上限根本不介入**（实测见 26.3 ✓）。

## 26.3 实测（专用服务器，无玩家 ⇒ 用临时命令替代"玩家站位"）

发射器/炮塔弹丸没有 owner、命令生成的弹丸不保存 shooter ✓，所以专门加了一个临时命令
`/scgunsphysdebug <命中点> <施力点>`（GAME 总线注册 ✓，用完已删除、jar 里无残留 ✓），
分别用"出拳距离 5 格"和"200 格外"两种施力点各打一发：

```
lever=5.07    spinPerImpulse=1.247e-4  allowedSpin=1.5  strength=61.94  beforeSpin=61.94  afterSpin=61.94  mass=242
lever=199.82  spinPerImpulse=1.484e-4  allowedSpin=1.5  strength=61.77  beforeSpin=61.77  afterSpin=61.77  mass=242
```

* ✅ **施力点确实换成了传入的点**：力臂 5.07 / 199.82 正是两个施力点到结构的距离 ✓
  （改之前永远是命中点 ⇒ 力臂会是个位数 ✗）；
* ✅ **出拳距离内与出拳完全一致**：`beforeSpin == afterSpin == 61.94` ⇒ 自旋上限一点没削 ✓；
* ✅ **强度对施力点不敏感**：5 格 61.94 / 200 格 61.77 ✓（Sable 的法向质量基本只看法向 ✓）；
* ⚠️ **实测自旋比预想小得多**：200 格力臂算下来也只有 `61.77 × 1.484e-4 ≈ 0.0092 rad/s`
  （5 格时 0.0077 rad/s ✓）⇒ Sable 报出来的惯量比"按结构尺寸估算"大得多，
  所以**这个自旋上限在常规情况下永远不会介入** ✓。它留着只作为兜底（极端小的结构才可能触发 ✓），
  文档如实记录，不吹成"保护了你" ✓。
* 反过来这也解释了 §22.7 的事故量级：当年力臂是 **2.9e7** ✗ ⇒
  ω ≈ 2.9e7 × 32 × 1.2e-4 ≈ **1.1e5 rad/s** ⇒ 解算器直接炸 ✓✓ 与"飞上天"完全吻合 ✓。

## 26.4 验收

* `tools/audit_physics_impulse_frame.py` 已同步（施力点/法向仍必须是**结构局部**值 ✓，
  并接受由 `localNormal` 归一化而来的 `unitImpulse` ✓）⇒ **0 problem** ✓
  （该审计拿修复前的源码跑会报 3 条 ✓，见 §22.7 ✓）；
* `gradlew build` 成功、jar 里 `SCGUNS-PHYS` 与 `PhysicsDebugCommand` 命中数均为 0 ✓、
  `verify_installed_jar` 66/66 ✓、`runServer` 到 `Done` 且之后 0 ERROR/FATAL ✓；
* **未验证**：真正的"玩家开枪"路径（专用服务器没有玩家，弹丸也不持久化 shooter ✓）——
  但该路径只多了一句 `this.shooter.position()` ✓，其余数学与本次实测完全相同 ✓。

# 27. 打物理结构**底部**没反应 —— 命中瞬间 `getDeltaMovement()` 已被清零

## 27.1 症状与实测定位

玩家报："射击物理结构的底部没有反应。"

先按"复现优先"做：写了个脚本把**所有**遗留测试平台列出来、清洗世界、只留一个新平台 ✓，
然后分别打**侧面**（对照）、**底部慢速**、**底部快速**，并在 `applyPunchAt` 里临时打印
"候选结构数 / 命中点 / 包围盒 / 被哪一步拒绝" ✓。结果一句话就见底了：

```
[SCGUNS-PHYS] rejected: direction 5.000013228676376E-5 too short
```

`applyShotImpulse` 里有一条输入检查：飞行方向长度 ≤ 1e-4 就直接返回 ✗。而这次命中的
`getDeltaMovement()` 只有 **5e-5** —— 几乎为零 ✓ ⇒ **整发子弹被直接丢弃** ✓✓ ⇒ 底部没反应 ✓。

**为什么会是零**：Sable 会把飞进子级的弹丸**搬进该结构自己的坐标系** ✓（§22.3 已记录），
而这次转移**把速度也一起清掉了** ✗ —— 弹丸穿过结构边界的那个瞬间正是它的速度被抹掉的时刻 ✓，
所以从下方打（要穿过底面边界 ✓）尤其容易撞上这个 ✗。

## 27.2 修法：给方向加一条回退链

`applyShotImpulse` 现在用 `flightDirection(...)` 依次尝试：

1. **弹丸自身的 `getDeltaMovement()`** ✓（长度平方 > 1e-8 才算有效 ✓）—— 常规情况 ✓；
2. **命中面的反向法向** ✓（`-blockHitResult.getDirection().getNormal()` ✓，由调用方传入 ✓）——
   子级底面被击中时面法向朝下 ⇒ 反向即"向上"，正是这发的飞行方向 ✓✓；
3. **射手位置 → 命中点** ✓（helper 内部的最后兜底 ✓，与 §26 的"力在射手处"模型一致 ✓）；
4. 三者都不成立才放弃（此时也确实无从判断方向 ✓）。

两个调用点（`ProjectileEntity.onHit` ✓ / `TurretProjectileEntity.onHitBlock` ✓）都按这条链取方向 ✓。

## 27.3 验证

修好后再跑同一个脚本：**`rejected ... too short` 那行消失了** ✓，两次底部命中的
`[SCGUNS-PHYS] lookup` 都正常打出候选结构并进入施力流程 ✓（对比修复前：同一发直接被丢弃 ✓）。
（临时诊断已全部删除 ✓，jar 里 `SCGUNS-PHYS` 命中数为 0 ✓，`audit_physics_impulse_frame` 0 problem ✓。）

## 27.4 顺带纠正一条我给过的错误建议

§26 末尾我让玩家用 `/sable sub_level remove <uuid>` 清理被扔飞的结构 —— **实测这条不成立** ✗：
`remove` 收的是**选择器**（`SubLevelSelectorType`：ALL/NEAREST/RANDOM/VIEWED/LATEST/TRACKING/INSIDE ✓），
直接给 uuid 会得到 `Incorrect argument for command` ✓；而 `/sable info <uuid>` ✓ 是能收 uuid 的 ✓
（两者参数类型不同 ✓）。我试了 `@all` / `@ALL` / `@a` / `#all` / `all` / `nearest` / `*` 等写法都不被接受 ✓，
**确切的选择器写法请在客户端里用 Tab 补全确认** ✓（语言文件只给了显示名，没给语法 ✓）。
本节的测试脚本因此改为"列出所有子级并逐个尝试删除" ✓，并在删不掉时明确打印仍然存在的 uuid ✓，
不会静默假装清理成功 ✓。

# 28. 闪光弹"没有给予玩家 debuff" —— **服务端实测是给了**，问题在别处

## 28.1 实测结论（先摆证据）

写了一个可在专用服务器上跑的检查（`tools/rcon_stun_grenade_check.py` ✓）：召唤一只僵尸，
在它上方引爆一颗闪光弹，读僵尸的 `active_effects` ✓：

```
start: zombie effects before: [(), []]
Summoned new Thrown Stun Grenade
   grenade still present: False
   zombie effects after:  [('scguns:deafened',), ['185']]
RESULT: the stun grenade does apply its effect server-side
```

⇒ **`onDeath()` 里的判据逻辑、`ScEffects.holder(...)` 包装、`addEffect(...)` 全都是通的** ✓
（僵尸拿到 `scguns:deafened`，185 tick ✓ 与配置的 280 相符，差值是引爆到我读数据之间的时间 ✓）。

> 顺带发现：这些投掷物在 `registerBasic` 里带 **`.noSummon()`** ✓（0.5.5 同样如此 ✓），
> 所以 `/summon scguns:throwable_stun_grenade` 会回 `Can't summon entity of type ...` ✗ ——
> 我一开始把这个当成"实体没注册"✗，其实是我探针错了 ✓。上面的脚本就是**临时**把
> `.noSummon()` 注释掉、跑完再还原得到的 ✓（已还原 ✓，脚本 docstring 里记了这一步 ✓）。

## 28.2 玩家会得到什么（以及为什么"看起来没有"）

| 效果 | 判据 | 玩家能感知到的部分 | 在哪实现 |
|---|---|---|---|
| `scguns:deafened` | **360°**（配置 `angleEffect=360` ✓）、半径 15 ✓ | 声音被压低 + 耳鸣声 | 客户端 `SoundHandler` ✓ / `StunRingingSound` ✓ |
| `scguns:blinded` | **必须朝向闪光**（锥角 170°⇒半角 85° ✓）+ 视线不被挡住（`raytraceOpaqueBlocks=true` ✓） | 屏幕白色/闪光遮罩 | 客户端 `GameRendererMixin` ✓ |

**两个效果都用 `new MobEffectInstance(..., ambient=false, visible=false)`** ✓ —— 即**没有 HUD 图标** ✓
（0.5.5 与移植版完全一致 ✓，不是移植引入的差异 ✓）。所以"给了 debuff 但玩家感觉不到"是**设计如此**：
玩家能感知到的是**遮罩和声音** ✓，而不是状态栏图标 ✓。

其余都核对过、与 0.5.5 逐行一致 ✓：`ThrowableStunGrenadeEntity.onDeath()` ✓、
`GameRendererMixin`（注入目标是 1.21.1 的 `GameRenderer.render(DeltaTracker, boolean)` ✓，
`audit_mixins` 报 0 drift ✓）、`SoundHandler` / `StunRingingSound`（`getEffect(Holder)` ✓ 1.21.1 正确写法 ✓）、
判据默认值（半径 15 / 角度 170 / 360 ✓）与 0.5.5 的 `Config` **无差异** ✓。

## 28.3 最可能被忽略的两个前提

1. **闪光弹之前根本扔不出去** ✗ —— §24 的 `getUseDuration` 签名过期会让物品"右键即用完"✓
   （`finishUsingItem` ⇒ `onDeath()` ⇒ 在手里炸 ✗）。该问题在 `26f1074` 已修 ✓，
   **如果玩家测的是那之前的 jar，症状就完全对得上** ✓；
2. **`blinded` 要求朝向闪光** ✓ —— 背对或扭头时只会有 `deafened`（声音变化很轻 ✓），
   容易被当成"没给 debuff" ✓。

## 28.4 我没有验证到的部分（说清楚）

* **客户端可见/可听的两处**（遮罩 ✓、压低音量/耳鸣 ✓）**无法在专用服务器上验证** ✗ ——
  需要玩家在有客户端的实机确认 ✓；
* 因此这一轮**没有改任何代码** ✓：现有证据不支持"移植漏了什么" ✗，
  改动反而可能掩盖真实原因 ✗。若玩家希望状态栏**显示**这两个效果，
  把 `MobEffectInstance` 的 `visible` 改成 `true` 即可（一行 ✓，但这是**有意偏离 0.5.5** ✓，
  要做请先说 ✓ —— 两个效果的 `getColor()` 都是 0（黑）✓，图标底色也会需要一起调 ✓）。

## 28.5 后续：玩家确认是**遮罩没出现** —— 已改用 GUI 图层（真正修好了）

§28.4 里我请玩家确认症状，回答是"**扔出去后屏幕完全没变化（没有白光/遮罩）**" ✓。
于是把客户端那条路走通并**实机验证**了：

### 28.5.1 先把服务端排除干净

把 `noSummon()` 临时去掉后做了两次对照（`tools/rcon_stun_grenade_check.py` ✓）：

```
（背对闪光）zombie effects: [('scguns:deafened',), ['185']]                    ← 只有 deafened
（朝向闪光）zombie effects: [('scguns:blinded','scguns:deafened'), ['88','165']] ← 两个都有 ✓
```

⇒ `blinded` 的判据（85° 锥角 + 视线）在服务端**完全正常** ✓，缺的是玩家*看到*的东西 ✓。

### 28.5.2 旧做法为什么看不见

0.5.5 的遮罩在 `mixin/client/GameRendererMixin` 里：注入 `GameRenderer.render` 的
第一个 `ProfilerFiller.popPush` 之后 ✓，然后

```java
GuiGraphics pGuiGraphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
pGuiGraphics.fill(0, 0, window.getScreenWidth(), window.getScreenHeight(), argb);
```

**注意：它自建了一个 `GuiGraphics` 却从不 `flush()`** ✗ ——
1.21.1 的 `GuiGraphics.fill` 只是把顶点写进 `BufferSource` ✓，必须 `flush()` 才会提交 ✓
（原版每帧由 GUI 流程替它 flush ✓，而这个"野"对象没有任何人替它提交 ✓）⇒ 画了等于没画 ✓。

我把这条**注入点**也查了字节码 ✓：1.21.1 的 `GameRenderer.render(DeltaTracker, boolean)` 里
`renderLevel` 在偏移 **244** ✓，第一个 `popPush` 在 **455**（即之后）✓
—— 所以"被世界盖住"这个猜想**不成立** ✗（我一开始猜错了 ✓，如实记录 ✓），
真正的问题就是**没有提交** ✓。

### 28.5.3 改法：1.21.1 官方做法——GUI 图层

删掉 `GameRendererMixin`（连同 `scguns.mixins.json` 里的条目 ✓），新增：

`client/handler/BlindnessOverlay.java` ✓

```java
@EventBusSubscriber(modid = "scguns", bus = Bus.MOD, value = Dist.CLIENT)
public final class BlindnessOverlay {
    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, BlindnessOverlay::render);   // 一定画在世界和 HUD 之上
    }
    // render(GuiGraphics, DeltaTracker)：效果存在时用 0.5.5 同一套淡出公式 fill 整个画面
}
```

优点：用**当帧原版的 `GuiGraphics`** ✓（由原版负责 flush ✓，不存在"没人提交"的问题 ✓），
并且 `registerAboveAll` 保证在最上层 ✓，不再依赖"注入到某个原版方法的第几个 popPush"这种脆弱假设 ✓。
淡出公式与两个配置项（`alphaFadeThreshold` / `alphaOverlay`）与 0.5.5 **完全一致** ✓。

### 28.5.4 实机验证（有截图）

专用服务器上跑不起来客户端遮罩 ✓，所以真的开了 dev 客户端并让它直接连进 dev 服务器 ✓
（临时在 `build.gradle` 的 `runs.client` 里加 `programArgument '--quickPlayMultiplayer' 127.0.0.1:25565` ✓，
验证完已还原 ✓；`tools/run_client_quickplay.cmd` 与 `tools/screenshot_window.ps1` 留作复用 ✓）：

* 客户端日志：`Setting user: Dev` ✓ → 服务器日志：`Dev joined the game` ✓（真的进服了 ✓）；
* RCON：`effect give Dev scguns:blinded 600` ⇒ `Dev has the following entity data: [{duration: 12000, ..., id: "scguns:blinded"}]` ✓；
* 截图对照（`build-logs/shot-before.png` / `shot-after.png` ✓）：**效果生效后整个游戏窗口被白光罩住** ✓✓
  —— 修复前同样的操作屏幕上**没有任何变化** ✗（玩家报的症状 ✓）。

**这一节是本次会话里唯一"看到画面"的验证** ✓：客户端可见的东西只有真的开客户端才能确认 ✓。
（顺带发现：`/effect give` 给出的效果带 `show_icon: 1b` ✓，而模组自己施加时是 `visible=false` ✓
—— 这是两条不同的路径 ✓，模组那条**不显示图标**是 0.5.5 原有设计 ✓。）

# 29. 玩家："闪光弹爆炸后 debuff 完全没有被施加" —— **移植自己写错了 `ignoreExplosion`**（已确认并修正）

> **先纠正我在本节前一版里的错误结论**：我曾写"0.5.5 也是跳过创造玩家的，这是上游问题，属有意偏离" ✗。
> 玩家当即指出"我在 1.20.1 测的时候创造模式是有效的" ✓ —— **玩家是对的** ✓，我错了 ✓。
> 我当时唯一的依据是移植版 `ExplosionHelper` 里的一句**注释** ✓（声称复刻了 1.20.1 行为 ✓），
> 没有去核对真正的 1.20.1 ✓ —— 这正是本项目反复踩的坑：**把注释/推测当证据** ✗。
> 下面的 29.1 是事后用**真 1.20.1 运行时 jar** 逐条核对出来的结论 ✓。

## 29.1 真 1.20.1 到底做了什么（javap 实证）

机器上正好有玩家的 1.20.1 Forge 实例 ✓，直接查它的运行时 jar
（`libraries/net/minecraftforge/forge/1.20.1-47.2.21/forge-1.20.1-47.2.21-client.jar` ✓）：

1. 0.5.5 的**字节码**里，`ThrowableStunGrenadeEntity` 的循环调用的是
   `net/minecraft/world/entity/LivingEntity.m_6128_:()Z` ✓（在 0.5.5 jar 上用 `javap -c` 看到 ✓）；
2. `m_6128_` 在 1.20.1 里**声明于 `Entity`**，方法体是**字面上的 `return false`** ✓：
   ```
   public boolean m_6128_();
     Code:
      0: iconst_0
      1: ireturn
   ```
3. 整个 jar 里只有 **3 个类**提到 `m_6128_`：`Entity`（声明）、`ArmorStand`（重写，体为 `isMarker()`）、
   `Explosion`（调用）✓ —— **`Player` 没有重写它** ✗✗。

⇒ 1.20.1 里 `if (!entity.ignoreExplosion())` 对**任何玩家**都等价于 `if (true)` ✓✓ ——
**创造模式与旁观玩家一样会被闪光弹影响** ✓，与玩家的记忆完全一致 ✓。

**结论**：这不是"0.5.5 上游问题" ✓，而是**移植自己引入的 bug** ✗：
移植把 `Entity#ignoreExplosion()` 改写成 `ExplosionHelper.ignoresExplosion(...)` 时，
凭想象写成了"无敌/旁观玩家返回 true" ✗（1.20.1 根本没有这个语义 ✓）⇒
**创造模式玩家被整段跳过** ✗ ⇒ 玩家"完全没有被施加" ✓。

## 29.2 实测（真玩家，创造 vs 生存）

`tools/rcon_stun_player_check.py` ✓：dev 客户端连进 dev 服务器 ✓，切换游戏模式 ✓，
在玩家旁 1.5 格引爆闪光弹 ✓，读 `active_effects` ✓：

```
（修复前）
survival after: [('scguns:deafened','205'), ('scguns:blinded','137')]
creative after: []                                                        ← 空 ✗

（把 helper 改成忠实实现后，同一个脚本、同一客户端、最终构建）
survival after: [('scguns:deafened','204'), ('scguns:blinded','135')]
creative after: [('scguns:deafened','205'), ('scguns:blinded','136')]     ← 正常 ✓✓
```

## 29.3 修正后的改法（**不是**偏离 0.5.5，而是**恢复** 0.5.5）

不再改调用点 ✓（上一版临时加的 `isSpectator()` 判断已撤销 ✓ ✓），而是把 helper 本身改成 1.20.1 的忠实实现 ✓：

```java
public static boolean ignoresExplosion(Entity entity) {
    return entity instanceof ArmorStand armorStand && armorStand.isMarker();
}
```

即：**只有 marker 盔甲架被跳过** ✓（这正是 1.20.1 的唯一重写 ✓），玩家一律照常 ✓。
这一处是**共享** helper ✓ ⇒ 6 个调用点（CustomExplosion / RocketExplosion / BouncyGrenadeRound /
HeGrenadeRound / ProjectileExplosion / 闪光弹 ✓）同时恢复成 0.5.5 的语义 ✓✓
—— 爆炸**伤害**那几处本来也不会有可见差异 ✓（无敌玩家的 `hurt` 依旧返回 false ✓），
但语义是与 0.5.5 一致的 ✓。

## 29.4 教训（写在这里，避免再犯）

* **注释不是证据** ✓：`ExplosionHelper` 的旧注释把"1.20.1 会让无敌玩家免疫"说得像事实 ✓，
  我据此下了"上游问题"的结论 ✗ —— 正确的做法是**直接查 1.20.1 的字节码** ✓（这次做了 ✓，5 分钟 ✓）；
* **玩家的实测记忆优先级高于我的推断** ✓：玩家说 1.20.1 有效 ✓ ⇒ 应该先去验证 ✓，而不是先解释 ✓；
* 附带好处：以后凡遇到"1.20.1 应该是这样"的假设 ✓，机器上有现成的 1.20.1 Forge 运行时 jar ✓，
  `javap` 一下就能定论 ✓（本次就是这么做掉的 ✓）。


**未验证**：真正"玩家自己右键扔出去"的完整链路（需要手动操作 ✓）——但这轮已经用*真玩家*验证了
"实体在玩家旁边爆炸 ⇒ 玩家拿到 debuff ⇒ 屏幕变白"这条主链路 ✓✓。

# 30. 玩家报的三个问题：工作方块无法交互（**已修**）/ 附魔台无法附魔枪械 / 村民附魔书交易

玩家一次报了三个 ✓。这一节记录**每一个的证据与当前状态** ✓ —— 第 1 个已修复并上闸门 ✓，
另外两个我**已经查到什么、还差什么**都写清楚 ✓，不拿"想当然"当结论 ✓。

## 30.1 工作方块无法交互 —— **已修**（1.21.1 把 `use` 拆成了两个钩子）

**根因** ✓：1.21.1 把 `BlockBehaviour` 的交互入口拆成两个（对**原版 1.21.1 jar 与 NeoForge 合并 jar
都做过 `javap`** ✓）：

```java
protected ItemInteractionResult useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)
protected InteractionResult    useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult)
```

**1.20.1 的 `use(BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)` 已不存在** ✗。
移植里有 **14 个方块**照 0.5.5 原样重写它 ✓，而它**没有 `@Override`** ✓ ⇒ 编译通过 ✓，
却只是一个**谁都不会调用的新方法** ✗ ⇒ **右键毫无反应** ✓✓ —— 正是"mod 的工作方块无法交互" ✓。
受影响：**枪械工作台 / 研磨机（含通电）/ 机械压床（含通电）/ 极光发电机 / 采矿单元 / 纪念碑 /
充能紫水晶继电器 / 高级堆肥桶 / 酸性坩埚 / 鸟粪蜡烛 / 沙袋** ✓。

**修法**（`tools/patch_block_use.py` ✓，14 处）：

* 方法体**不看手持物品**的 9 个 ⇒ 改成 `useWithoutItem(...)` ✓（去掉 `InteractionHand` 参数 ✓）；
* **需要看手持物品**的 5 个（堆肥桶 / 鸟粪蜡烛 / 采矿单元 / 沙袋 / 酸性坩埚 ✓）
  ⇒ 改成 `useItemOn(ItemStack stack, ...)` ✓（`player.getItemInHand(hand)` 换成新的 `stack` 参数 ✓，
  返回值按表映射成 `ItemInteractionResult.*` ✓；堆肥桶的私有辅助 `extractProduce` 也一并改返回类型 ✓）；
* **两者都补 `@Override`** ✓✓ —— 签名再漂就**编译失败** ✓，不会再"编译通过但没人调用" ✓
  （本次就靠它验收 ✓：14 个 `@Override` 全部通过编译 ⇒ 签名确实对上了 ✓）。

**验收** ✓：`verify_installed_jar.py` 新增 **15 项**（8 空手钩子 + 4 物品钩子 + 3 条"旧 `use` 描述符
**不再出现**" ✓），**用修复前的 jar 自测**：旧 jar **65/80**（那 15 项必须 FAIL ✓），新 jar **80/80** ✓。
`gradlew build` 成功 ✓、12 项审计全清 ✓、`runServer` 到 `Done` 且之后 0 ERROR/FATAL ✓。

> 这是本项目第四次栽在同一坑上 ✓（`finalizeSpawn` ✓、`getUseDuration` ✓、`renderToBuffer` 颜色 ✓、现在 `use` ✓）
> —— 共同形态都是"**签名过期 ⇒ 编译通过 ⇒ 什么都没重写**" ✓。
> 现有 `audit_stale_overrides.py` 抓不到这一类 ✗（它只比对**同名不同签名** ✓，而 `use` 是**方法名整个消失** ✗）⇒
> 待办见 §30.4 ✓。

## 30.2 附魔台无法附魔枪械 —— 数据侧实测是**通的**，症状还需确认

已查清（**实测**，非推测 ✓）：

* 附魔数据链完整 ✓：`data/scguns/tags/item/enchantable/guns.json` 存在且列了 ~150 把枪 ✓，
  模组附魔 JSON 用 `"supported_items": "#scguns:enchantable/guns"` ✓，服务端无
  `Parsing error` / `Couldn't load tag` 报错 ✓；
* **`/enchant` 直接验证** ✓（僵尸手持 `scguns:gale`）：
  ```
  /enchant @e[type=zombie,limit=1] scguns:accelerator
     → Applied enchantment Hyper Velocity I to Zombie's item      ✓ 成功
  /enchant @e[type=zombie,limit=1] minecraft:unbreaking
     → Gale cannot support that enchantment                        （设计如此：枪只吃模组附魔）
  ```
  物品随后确实是 `{minecraft:enchantments:{levels:{"scguns:accelerator":1}}}` ✓；
* 即 `isEnchantable`（`getMaxStackSize(stack)==1` ✓，枪 `stacksTo(1)` ⇒ true ✓）、
  `getEnchantmentValue()=13` ✓、附魔与物品的匹配 ✓ **都没问题** ✓。

**未查清** ✗：附魔台**界面里显示了什么** ✓ —— 附魔台三选项由**客户端驱动** ✓，专用服务器测不出来 ✓。
需要玩家补一句 ✓：**三个槽位全空（连等级数字都没有）** ✗ 还是**有选项但没有模组附魔/点了没反应** ✗？
（工具现成：`tools/run_client_quickplay.cmd` + `tools/screenshot_window.ps1` ✓，可以像 §29 那样开真客户端截图 ✓。）

## 30.3 村民不能正常交易附魔书 —— 已排除一条，还差关键信息

已排除（静态 + 实测 ✓）：

* 模组枪匠交易是**追加**的 ✓（`ModEvents.addCustomTrades` 只 `add` ✓，**没有清空/替换**原版交易 ✓）
  ⇒ 原版图书管理员的附魔书交易**不该**被模组破坏 ✓；
* 模组交易里**根本没有附魔书** ✗：枪匠 1~5 级全是零件/枪械/材料/照明弹 ✓（0.5.5 与移植一致 ✓，
  两棵树都搜不到 `EnchantedBookItem` / `createForEnchantment` / `ENCHANTED_BOOK` ✓）；
  `data/scguns/entity/merchant_trades.json`（模组商人 NPC）里也没有 `enchanted_book` ✓。

⇒ 需要玩家确认具体是哪一个 ✓：(甲) 原版**图书管理员**坏了 ✗；(乙) 模组**商人 NPC** 某条交易 ✗；
(丙) 其实想说"枪匠**应该**卖枪械附魔书"（新功能 ✗，0.5.5 也没有 ✓）。

> 顺带记录一条**已知移植偏差** ✓（§10.7 提过 ✓）：`MerchantTradeConfig.parseItemStack` 只支持
> `{item, count}` ✓（`new ItemStack(item, count)` ✓），**无法表达 1.21 的组件** ✗ ⇒ 交易 JSON 里写附魔物品
> 会出来**白板物品** ✗。0.5.5 是 NBT 时代 ✓，那条路径当年能带 NBT ✓。若玩家确认是 (乙) ✓，
> 这就是根因 ✓，修法是用 `ItemStack.CODEC` 解析整份物品 JSON ✓。

## 30.4 待办（下一轮）

1. 给 `audit_stale_overrides.py` 加"**方法名在祖先链里完全不存在**"的检查 ✓（本次 14 处本该被它抓到 ✓）；
2. 按玩家答复处理 30.2 / 30.3 ✓。

# 32. 蓝图"设置配方"全部变成高斯步枪 —— **移植给所有配方塞了同一个占位 id**

玩家报："蓝图界面的设置配方选项有bug，**所有枪械的激活配方全部变为了高斯步枪**" ✓。

## 32.1 根因（有实测证据）

移植的新基类 `common/recipe/ScRecipeSerializer.java` 里有这么一段 ✓：

```java
/** Stand-in for the id the codec based path can no longer provide. */
public static final ResourceLocation UNKNOWN_ID = ResourceLocation.fromNamespaceAndPath("scguns", "unknown");
...
json -> fromJson(UNKNOWN_ID, json)             // 1.21 的 codec 路径拿不到 id，于是全部用占位符
buffer -> fromNetwork(UNKNOWN_ID, buffer)
```

1.21 把"配方 id"从 `Recipe` 挪到了 `RecipeHolder` ✓，移植为了少改代码就统一塞了一个占位 id ✗ ⇒
**146 个枪械工作台配方的 `recipe.getId()` 全是 `scguns:unknown`** ✗（实测 ✓）：

```
[SCGUNS-RECIPEID] gun bench recipes: 146
[SCGUNS-RECIPEID] holderId=scguns:guns/gauss_rifle_from_gun_bench recipeValueId=scguns:unknown result=Gauss Rifle
[SCGUNS-RECIPEID] holderId=scguns:guns/blunderbuss_from_gun_bench recipeValueId=scguns:unknown result=Blunderbuss
[SCGUNS-RECIPEID] recipes whose own id is the placeholder: 146/146
```

而蓝图界面两处**都按 `recipe.getId()` 做事** ✗：

* 点"设置"时发送/保存的 id = `scguns:unknown` ✗；
* 显示"当前激活配方"时用 `filter(r -> r.getId().equals(recipeId)).findFirst()` ✗ ——
  所有配方 id 相同 ⇒ **永远命中列表里的第一条** ✗，而第一条正好是
  **`scguns:guns/gauss_rifle_from_gun_bench`（Gauss Rifle）** ✓✓（上面日志第一行就是它 ✓）。

⇒ **每张蓝图、每个枪械的"激活配方"都显示成高斯步枪** ✓✓ —— 与玩家描述完全一致 ✓。

## 32.2 修法：id 一律取自 `RecipeHolder`

1.21 的正确做法就是用 holder ✓ —— 改动都在 `client/screen/BlueprintScreen.java` ✓：

* `DisplayEntry` 增加 `final ResourceLocation id` 字段（由 `RecipeHolder.id()` 提供 ✓，
  lore-only 的展示条目为 null ✓）；
* `loadAvailableEntries()` **不再 `map(RecipeHolder::value)`** ✓（这一步正是把 id 丢掉的原因 ✓），
  直接保留 holder 列表 ✓（1.21 的 `getAllRecipesFor` 本来返回 `List<RecipeHolder<T>>` ✓，反而更简单 ✓）；
* 判断"哪一条是当前激活"改用 `entry.id.equals(activeRecipeId)` ✓；
* "设置"时发送/保存 `entry.id` ✓（`saveActiveRecipe` 形参也从 `GunBenchRecipe` 改成 `ResourceLocation` ✓）；
* `getActiveRecipeName()` 改成 `level.getRecipeManager().byKey(recipeId)` ✓（按 id 精确取 ✓，
  不再"遍历 + 第一条" ✓）。

## 32.3 验证与遗留

* ✅ **根因已实测**（上面那段日志 ✓，用临时 `ServerStartedEvent` 诊断打出 holder id 与 recipe value id ✓，
  诊断已删除、jar 内无残留 ✓）；
* ✅ 修复后**编译通过** ✓（`@Override`/类型层面都由 javac 保证 ✓）、`verify_installed_jar` **82/82** ✓、
  `runServer` 到 `Done` 且之后 0 ERROR/FATAL ✓；
* ⚠️ **界面本身需要玩家确认** ✗：蓝图界面是纯客户端 ✓，无客户端时连屏幕都开不起来 ✓ ——
  请玩家再进一次蓝图界面点"设置配方" ✓，激活配方应当显示成**那把枪自己** ✓；
* 📌 **写进待办的一般性教训** ✓：`ScRecipeSerializer.UNKNOWN_ID` 是个**地雷** ✓ ——
  凡是拿 `recipe.getId()` 当键的代码在这个移植里**都是错的** ✗（本次已把仅有的两处改掉 ✓，
  其余 `getRecipeFor(...).map(RecipeHolder::value)` 之类只用 value 的调用点不受影响 ✓）。

# 33. 把玩家的汉化资源包内置进 mod（`zh_cn.json`）

玩家说："我在资源包界面放了一个汉化资源包，我认为可以把这个资源包的 lang 文件内置进去" ✓ ——
于是把那份包里的语言文件**原样**装进 mod ✓，这样**不需要启用资源包**就能显示中文 ✓。

## 33.1 做法与核对

* 来源 ✓：玩家的资源包 `…\versions\1.21.1-NeoForge_21.1.250\resourcepacks\Scorched Guns_v0.5.5-汉化v1.3.zip`
  里的 `assets/scguns/lang/zh_cn.json` ✓；
* 落地 ✓：**逐字节**写入 `src/main/resources/assets/scguns/lang/zh_cn.json` ✓
  （118,567 字节 ✓、1794 条 ✓）—— 只取 lang ✓，包里的 `pack.png`(639 KB) 不需要 ✓；
* 覆盖率实测 ✓：**en_us 的 1784 条键 100% 都有中文** ✓（缺 0 条 ✓），另外多出 10 条
  `subtitle.scguns.*`（`jam`/`flyby`/`jetpack`/`loop`/`rack`/`slap`/`silenced_fire`/`fire_2`/
  `distant_fire`/`fire.silenced` ✓）。这 10 条**0.5.5 的 en_us 里也没有** ✓，
  代码里也没有字面量引用 ✗，但 `sounds.json` 里确实存在 `bullet.flyby1`、`item.airgun.fire_2`、
  `item.*.silenced_fire` 这类声音 ✓ ⇒ 属于"译者提前补的条目" ✓，留着无害 ✓
  （若想让英文端也有对应文本，补 en_us 一行即可 ✓，本次**没动 en_us** ✓ 以保持与 0.5.5 一致 ✓）；
* 顺带核对 ✓：**移植的 `en_us.json` 与 0.5.5 的 en_us 键集完全一致** ✓（1784 / 1784 ✓，
  既没丢也没多 ✓）—— 这条以前没验过 ✓，现在有了 ✓。

## 33.2 新增审计 `/ 验收`

* 新增 `tools/audit_lang_keys.py` ✓（**报告型** ✓，不算门禁：翻译不全本来就是合法状态 ✓）——
  逐文件对比 en_us ✓。实测结果 ✓：

  ```
  file             keys  missing      extra coverage
  en_us.json       1784        0          0   100.0%
  zh_cn.json       1794        0         10   100.0%     ← 内置的汉化
  uk_ua.json       1335      449          0    74.8%
  ko_kr.json        971      815          2    54.3%
  vi_vn.json        891      898          5    49.7%
  ru_ru.json        471     1317          4    26.2%
  ```
  ⇒ 内置的 zh_cn 现在是**最完整**的语言文件 ✓；
* `verify_installed_jar.py` 新增 2 项打包检查 ✓：jar 里必须有
  `assets/scguns/lang/zh_cn.json` 且含 `"item.scguns.musket"` ✓、并且**含中文字符字节** ✓
  （第二项故意不查键名 ✓，避免"文件在就行"的假通过 ✓）。**自测** ✓：旧 jar 实测 **82/84**
  （这两项必须 FAIL ✓），新 jar **84/84** ✓；
* `gradlew build` 成功 ✓、`runServer` 到 `Done` 且之后无真实报错 ✓。

## 33.3 玩家侧说明

资源包现在**可以关掉** ✓（同样的字符串已经在 mod 里 ✓）；留着也无害 ✓
（资源包只是覆盖同名键 ✓，内容一致 ✓）。

# 34. 补上 en_us 缺的 10 条字幕 —— **顺带发现整个字幕前缀都是错的**

玩家："en_us 缺失的十条字幕也补上" ✓ —— 已补 ✓，但核对时发现**更深的问题** ✓，一并记录 ✓。

## 34.1 按玩家要求补的 10 条（已做）

在 `en_us.json` 里、现有 `subtitle.scguns.*` 块之后**逐行插入** ✓（只 +10 行 ✓，不动其它任何字符 ✓）：

| key | 英文（本次所写） | 汉化包里的中文 |
|---|---|---|
| `subtitle.scguns.distant_fire` | Distant Gunfire | 远处枪声 |
| `subtitle.scguns.fire.silenced` | Silenced Gunshot | 消音枪声 |
| `subtitle.scguns.fire_2` | Gunshot | 枪声 |
| `subtitle.scguns.flyby` | Bullet Flyby | 子弹飞掠 |
| `subtitle.scguns.jam` | Gun Jam | 卡壳 |
| `subtitle.scguns.jetpack` | Jetpack | 喷气背包 |
| `subtitle.scguns.loop` | Looping Sound | 循环音 |
| `subtitle.scguns.rack` | Gun Racked | 上膛 |
| `subtitle.scguns.silenced_fire` | Silenced Gunshot | 消音枪声 |
| `subtitle.scguns.slap` | Magazine Slap | 拍击声 |

补完后 **en_us 与 zh_cn 的键集完全一致** ✓（都是 1794 条 ✓，`audit_lang_keys.py` 实测两边
missing/extra 都为 0 ✓）。

## 34.2 但真正的问题：**这些键客户端根本不会查**

1.21.1 的 `SoundEvent` **已经不再携带字幕组件** ✗（`javap net.minecraft.sounds.SoundEvent`：
只有 `createVariableRangeEvent` / `createFixedRangeEvent` / `getLocation` / `getRange` ✓，
**没有 `getSubtitle`** ✓）。原版字幕键的拼法是：

```
subtitles.<命名空间>.<声音路径>          ← 注意有 "s"
```

（对照原版 `en_us.json`：`subtitles.entity.zombie_horse.ambient` ✓ 等 ✓）
而本模组（0.5.5 与移植版都一样 ✓）用的是 **`subtitle.scguns.*`（没有 s）** ✗，
且 `ModSounds.register(...)` 用的是单参 `createVariableRangeEvent` ✓ ⇒ **没有任何地方会去查
`subtitle.scguns.*`** ✗。

`tools/audit_lang_keys.py` 因此新增一段实测报告 ✓（从 `sounds.json` 反推客户端真正要查的键 ✓）：

```
subtitles the client asks for (103 sound events):
  present: 0   missing: 103
   missing: subtitles.scguns.entity.praetor.hurt
   missing: subtitles.scguns.entity.praetor.die
   ...
  legacy 'subtitle.*' keys in the reference (46) - never looked up by the client
```

⇒ **103 个声音事件、103 条字幕键，一条都不存在** ✗✓✓ —— 也就是说**这个模组的字幕从来没显示过**
（玩家看到的会是原始键名，或干脆没有字幕 ✓）。这不是移植引入的 ✓（0.5.5 同样如此 ✓），
但它是玩家"字幕缺失"的真正原因 ✓。

**修法（需要玩家拍板 ✓，因为它涉及 103 条英文文案）** ✓：
按原版拼法补 `subtitles.scguns.<声音路径>` ✓ —— 其中 46 条可以从现有 `subtitle.scguns.*`
**机械对应**过来（例如 `subtitle.scguns.praetor.hurt` → `subtitles.scguns.entity.praetor.hurt` ✓），
剩下的按声音名生成 ✓（例：`subtitles.scguns.bullet.flyby1` → "Bullet Flyby" ✓、
`subtitles.scguns.item.mag_in.mag_in` → "Magazine In" ✓）。中文侧同理可用汉化包里的 46 条映射 ✓，
其余留英文 ✓。**本次没有擅自加这 103 条** ✗ —— 等玩家确认要不要做、以及英文措辞的口径 ✓。




# 31. 玩家补充信息后定位：附魔台"受限" + 图书管理员不卖 scguns 附魔书 —— **同一个根因，缺的是标签**

玩家补充 ✓："附魔台**显示附魔功能受限**" ✓、"**图书管理员**疑似无法交易 scgun 的附魔书" ✓ ——
这两句把 §30.2 / §30.3 一次性解释清楚了 ✓。

## 31.1 根因：1.21 用**标签**决定"哪些附魔能出现在附魔台/村民交易里"，移植一个标签都没带

从原版 1.21.1 jar 里直接读出这些**数据文件** ✓（不是猜 ✓）：

```
data/minecraft/tags/enchantment/in_enchanting_table.json  →  { "values": ["#minecraft:non_treasure"] }
data/minecraft/tags/enchantment/tradeable.json            →  { "values": ["#minecraft:non_treasure", ...] }
```

* **附魔台**只会提供 `#minecraft:in_enchanting_table` 里的附魔 ✓；
* **图书管理员**只从 `#minecraft:tradeable` 里抽附魔做附魔书 ✓（且本机 `/datapack list` 实测
  **`trade_rebalance` 数据包是关闭的** ✓ ⇒ 走的是代码路径 `VillagerTrades.EnchantBookForEmeralds` ✓，
  读的就是 `#tradeable` ✓✓）；
* 而这两个标签都建立在 **`#minecraft:non_treasure`** 之上 ✓。

**移植只带了 `data/scguns/enchantment/*.json`（15 个附魔定义 ✓），一个 `data/minecraft/tags/enchantment/` 都没有** ✗
（本次专门查过 ✓）⇒ 模组的 15 个附魔**不在任何标签里** ✗ ⇒
**附魔台不提供它们** ✗（玩家看到的"附魔功能受限" ✓）**且图书管理员永不进它们的货** ✗✓✓ ——
两个症状同一个原因 ✓✓。

**0.5.5 为什么没这问题** ✓：1.20.1 没有这些标签 ✓，Forge 的 `Enchantment#isDiscoverable()` /
`isTradeable()` **默认就是 true**（只有 treasure / curse 才排除 ✓），而模组的附魔类只设了
`Rarity`（COMMON~VERY_RARE ✓，已核对 ✓）⇒ 当年**既能在附魔台出现、也能被村民交易** ✓✓。
1.21 把这份语义**搬进了标签** ✓ ⇒ 移植必须自己声明 ✓。

## 31.2 修法（一个新文件，尽量小）

`src/main/resources/data/minecraft/tags/enchantment/non_treasure.json` ✓：

```json
{ "replace": false, "values": [ "scguns:accelerator", ... 共 15 个 ... ] }
```

只加这一个文件 ✓ —— 因为 `in_enchanting_table` ✓ / `tradeable` ✓ / `on_random_loot` ✓ /
`on_traded_equipment` ✓ / `on_mob_spawn_equipment` ✓ 在原版里**都引用 `#non_treasure`** ✓，
所以一处即可把这四项一起恢复成 0.5.5 的语义 ✓✓（"可发现 + 可交易" ✓）。
`replace: false` ⇒ **与原生标签合并**（不是覆盖 ✓），原版附魔不受影响 ✓。

## 31.3 验证到什么程度

* ✅ **打包检查** ✓：`verify_installed_jar.py` 新增 2 项（`data/minecraft/tags/enchantment/non_treasure.json`
  存在且包含 `scguns:accelerator` 与 `scguns:waterproof` ✓）—— 旧 jar 实测 **80/82**（这两项 FAIL ✓），
  新 jar **82/82** ✓；
* ✅ **数据包层面** ✓：`/datapack list` 确认 `trade_rebalance` **未启用** ✓（所以图书管理员确实读 `#tradeable` ✓）；
  服务端启动无任何 `Parsing error` / `Couldn't load tag` ✓（标签加载正常 ✓）；
* ✅ 附魔**本身**可作用于枪械 ✓：`/enchant … scguns:accelerator` → `Applied enchantment Hyper Velocity I` ✓（§30.2 ✓）；
* ⚠️ **没能直接验证"附魔台界面上出现模组附魔"** ✗：附魔台要玩家把枪放进槽位 ✗，
  无客户端时连菜单都建不起来 ✓（试过 servr 侧读 `EnchantmentMenu` ✗ 不可行 ✓）。
  **请玩家在游戏里放一把枪 + 书架试一下** ✓（对玩家来说是两秒钟的事 ✓）；
* ⚠️ 顺带记录一条**测试环境事实** ✓：命令生成的村民**不会**正常生成全部交易 ✗
  （`level:5` 只给"大师级"那一条 ✓、`level:1` 干脆为空 ✓、放到讲台旁也不会自动就职 ✓）
  ⇒ **村民交易无法在专用服务器上验证** ✓，只能靠玩家 ✓（这条写在这里，免得下次又白试一遍 ✓）。




# 35. 玩家报"敌怪身上的枪械不会携带 Gun Rust 附魔" —— **实测：附魔在，移植也没错；但这条附魔对怪物本来就没有任何作用**

玩家原话：**"敌怪身上的枪械貌似不会携带 Curse of Gun Rust 附魔"**。查完的结论分三层，
每层都有实测 ✓ —— **本次没有改任何代码** ✗（因为没有可修的东西 ✓）。

## 35.1 第一层：附魔**确实**被施加了（实测，不是读代码）

用 `tools/rcon_mob_gun_curse_check.py` 在专用服务器上反复召唤枪手（每次都是全新的一次 85% 掷骰 ✓），
读它们**手里那件物品**的 `HandItems` NBT ✓：

```
scguns:krauser      [{components: {"minecraft:enchantments": {levels: {"scguns:gun_rust": 1}},
                                   "minecraft:damage": 468,
                                   "minecraft:custom_data": {AmmoCount: 13}}, ...}]
scguns:soul_drummer 同样带 scguns:gun_rust: 1
```

而且附魔走的是**原版组件** `minecraft:enchantments` ✓（不是自定义组件 ✓ —— 也就是说原版附魔系统
认得它 ✓）。汇总：**15 把枪里 12 把带咒 ✓**（配置的掷骰率就是 85% ✓，二项分布完全吻合 ✓）。

**注意一个坑（我一开始就被它骗了 ✓）**：这些怪物**同时也会掷出近战武器和盔甲** ✗ ——
adjudicator 的 6 条 mainhand 配置里 4 条是枪、2 条是近战 ✓，cog_knight 更是 6 条里 5 条近战 ✓，
blunderer 拿的是 `war_axe`（永远不带咒 ✓，因为它不是 `GunItem` ✓）。
**所以"看到怪拿着枪没附魔"最常见的原因，是那件东西根本不是枪** ✓。
判定是不是枪有个可靠标志：枪的 NBT 里有 `minecraft:custom_data: {AmmoCount: N}` ✓，
近战武器没有 ✓（`tools/rcon_mob_gun_curse_check.py` 现在就按这个判 ✓）。

## 35.2 第二层：施加附魔的代码与 0.5.5 **逐行一致**（三处调用点全对上了）

`GunCurseUtil.applyCurseIfRoll` 在两棵树里的调用点是**完全相同的三处** ✓：

| 调用点 | 0.5.5 | 移植 | 说明 |
|---|---|---|---|
| `EntityEquipmentConfig`（JSON 装备表） | :140 | :142 | 只对 `EquipmentSlot.MAINHAND` 且 `instanceof GunItem` 掷骰 ✓ |
| `GunnerMobSpawner`（主题枪手） | :163 | :167 | 语句顺序、`setItemSlot` 位置都相同 ✓ |
| `GunnerMobSpawner`（进度枪手） | :259 | :263 | 同上 ✓ |

顺带排掉一条**看着像漏了**的路径 ✓：`RaidManager` 给首领/喽啰发枪时（`:498` / `:673`）
**不**掷咒 ✗ —— 但 0.5.5 也一样不掷 ✗（两棵树这段代码逐行相同 ✓）⇒ **不是移植引入的** ✓。
另外确认所有枪类都 `extends GunItem` ✓（`AnimatedGunItem extends GunItem` ✓、
`EnergyGunItem extends GunItem` ✓、`UnderwaterGunItem`/`NonUnderwaterGunItem` 同理 ✓）
⇒ `instanceof GunItem` 这道门槛不会漏掉某一类枪 ✓。

## 35.3 第三层（真正的答案）：**这条咒对怪物不产生任何效果 —— 0.5.5 也是**

能查到的"咒生效"的地方**全模组只有一处** ✓：`GunEventBus` 里玩家开火时算 `jamChance`
（`0.1 + level*0.05`，耐久 >80% 再 +0.025 ✓），命中就卡壳 + 冷却 ✓。而它所在的类事件是：

```java
public class GunFireEvent extends PlayerEvent      // ← 0.5.5 就是 PlayerEvent
```

⇒ 这个事件只由**玩家的开火路径**投递 ✓（`ShootingHandler:301` 客户端、
`ServerPlayHandler:78` 服务端 ✓），怪物 AI 走的是 `GunAttackGoal` ✓，**根本不经过这里** ✗。
`isCursed()` / `removeCurse()` 两棵树里都是**死代码** ✓（全仓库没有任何调用点 ✓）。

⇒ **结论：带咒的怪物枪和不带咒的怪物枪，行为完全一样** ✓；这不是移植缺陷 ✓，
是 0.5.5 的原始设计 ✓（`GunFireEvent extends PlayerEvent` 这一条就是铁证 ✓）。
如果玩家想要"怪物也会卡壳"，那是一个**新功能**（有意偏离 0.5.5），需要玩家明确要求 ✓。

## 35.4 两条会误导排查的测试环境事实（已实测，记下来）

1. **`/kill` 永远不会让怪物掉装备** ✓ —— 我一开始用 `/kill` 打了 30 只 adjudicator，
   结果**主手 0/30 掉落** ✓，差点当成 bug ✓。查 1.21.1 原版字节码才明白 ✗：
   装备掉落不在 `LivingEntity.dropEquipment()`（它是空方法 ✓）里，而在
   **`Mob.dropCustomDeathLoot(level, source, hitByPlayer)`** 里 ✓，并且有这样一道门 ✓：

   ```
   147: iload_3            // hitByPlayer
   148: ifne  156           // 是玩家杀的 → 继续
   151: iload 10           // 否则只有 chance > 1.0（必掉）才继续
   153: ifeq  242
   ```

   ⇒ **不是玩家杀的（`/kill`、摔死、岩浆、别的怪打死）就不掉装备** ✓，
   而 `hitByPlayer` 来自 `lastHurtByPlayerTime` ✓（只有玩家造成伤害才会设 ✓）。
   所以**装备掉落必须在有玩家的情况下测** ✓ —— 专用服务器上没有玩家 ⇒ 这条只能靠玩家验证 ✓。
   （战利品表掉落不受影响 ✓：`advanced_round` / `gunpowder` / `brass_mask` 照掉 ✓，
   这也是为什么"看着有掉落、就是没有枪"这个假象很逼真 ✓。）
2. **怪手里的带咒枪不会有附魔光效** ✓：`GunItem.isFoil(ItemStack)` 直接 `return false` ✗
   （0.5.5 :305 与移植版逐字相同 ✓，只是移植版**缺 `@Override`** ✓ ——
   1.21.1 的 `Item.isFoil(ItemStack)` 仍在 ✓、`ItemStack.hasFoil()` 也确实会调它 ✓
   （`javap` 核过 ✓），所以**这个重写是生效的** ✓，不是签名漂移 ✓）。
   ⇒ 靠"看有没有光效"判断怪物枪有没有咒，是**判断不出来**的 ✓。

## 35.5 工具改进与遗留

- `tools/rcon_mob_gun_curse_check.py` **重写** ✓：旧版把"名字里带 `scguns:`"当枪 ✗，
  于是把近战武器也算进分母 ✓，输出过误导性的 "3/4 带咒" ✓。现在按 `AmmoCount` 判定 ✓、
  打印每个召唤物拿着什么 ✓，并且**只在真正失败时返回 1** ✓（没有枪可查也算 FAIL ✓、
  非枪物品带咒也算 FAIL ✓）。实跑：`18 summons, 5 guns, 5/5 cursed` ✓ exit 0 ✓。
- **待玩家确认**：本次是按"附魔确实在"结案的 ✓，所以没动代码 ✓。
  如果玩家看到的其实是**行为**（怪物开枪从不卡壳 ✓）⇒ 需要玩家拍板要不要加"怪物卡壳"这个新功能 ✓。




# 36. 内置女仆兼容（Touhou Little Maid）—— **以嵌套 jar 形式随主 jar 一起分发**

玩家要求：把 1.20.1 的 Scorched Guns 女仆兼容（`E:\mod\SCG2_TLM\forge-1.20.1-47.4.10-mdk`，
玩家自制，1.20.1 那版比现存的 1.21 移植版功能更全）内置进这个移植。玩家选择的做法是
**B：作为嵌套 jar（jar-in-jar）内置** —— 单文件分发，但兼容仍是**独立的 mod**（自己的 modid、
自己的 mixin 配置、自己的类加载器），对我们核心零风险 ✓。

## 36.1 先回答"为什么现存的 1.21 版用不了"（读元数据实测，不是猜）

`E:\mod\1.21_SCG2_TLM\MDK-1.21.1-ModDevGradle-main` 是对着**官方 SC2 1.2.5 / 1.5** 编的
（`libs/ScorchedGuns-1.2.5.jar.new`、`ScorchedGuns-1.5.jar` ✓），它的 `neoforge.mods.toml` 写死：

```
modId="scguns"  versionRange="[1.2.0,")
```

而本移植自报 **0.5.5**（`gradle.properties` 的 `mod_version`）⇒ **依赖不满足，NeoForge 直接不加载它**
✗ —— 这才是"用不了"的真因 ✓，与功能无关。对照玩家 1.20.1 的成品 jar：它写的是
`scguns >= [0.5.0,)` ✓ 正好匹配本移植 ⇒ **以 1.20.1 源码为基准、对着本移植重编**是正路 ✓。

## 36.2 移植量：**259 → 0**（每一档都有实测）

把 1.20.1 的 43 个源文件（7,756 行）直接对着本移植的 classpath 编译，得到基线：

| 阶段 | 错误数 | 手段 |
|---|---|---|
| 原始 1.20.1 源码 | **259** | `javac`（`tools/check_maid_compat.py` 的前身，见 `build-logs/maid-spike/`） |
| 机械改写 Forge→NeoForge（导入表 + 注册表 + 标识符，192 处） | 110 | `tools/port_rewrite_maid.py` |
| 补第二轮机械规则（`Registry#getValue`→`get`、`getOrCreateTag`→`NbtHelper`、`isSameItemSameTags`→`isSameItemSameComponents`、`@EventBusSubscriber`、`ToolActions`→`ItemAbilities`） | 52 | 同上 |
| Cloth Config 相关文件删除 + 入口重写 + 真 API 漂移 | **0** | 人工（并发交给两个子代理处理 6 个文件，都是 javap 核对后改的） |

机械改写里**每一条规则都在注释里写明理由**，`port_rewrite_maid.py` 顶部还列了 5 个"不是纯改名、
故意不自动改"的 API（`FMLJavaModLoadingContext` / `ToolActions` / `LivingHurtEvent` /
`TickEvent.PlayerTickEvent` / `LazyOptional`·`NonNullConsumer`）⇒ 留给编译器报出来再逐个看 ✓。

真漂移全部用本移植**已有的辅助类**解决（这是"移植同源"的红利 ✓）：
`NbtHelper.itemFromTag/tagFromItem`、`ScEnchants.level/getEnchantments/is/getDamageBonus`、
`Caps.ifPresent/map`、`ScEffects.holder`、`MobType.of`（1.21 删了 `LivingEntity#getMobType`）。

## 36.3 构建接线（两个工程，一个产物）

- `maid-compat/` 是**子工程**（`settings.gradle` 里 `include 'maid-compat'`），用 ModDevGradle，
  `neoForge { mods { scg2_maid_compat { sourceSet(...) } } }`，**不配 runs**（它只在主工程的
  runServer/runClient 里跑 ✓）。
- 编译依赖：`compileOnly files(rootProject.layout.buildDirectory.dir('classes/java/main'))`
  —— 故意指向**宿主编译产物目录而不是宿主 jar** ✓，因为宿主 jar 里会包含它（否则循环依赖 ✗）。
- 主工程 `build.gradle`：`jarJar(project(':maid-compat'))` ⇒ 产出
  `META-INF/jarjar/com.scg2tlm.scg2_maid_compat-neoforge-1.21.1-1.0.8.jar` + `metadata.json` ✓
  （已实测在 `build/libs/scguns-0.5.5.jar` 与**已安装的 jar** 里都存在 ✓）。
- 依赖声明：`scguns >= [0.5.0,)`（required ✓）、**`touhou_little_maid` 为 optional** ✓、
  `scgextra` / `bettercombat` 也是 optional ✓。

## 36.4 "没装车万女仆就完全不要加载" —— 三层守卫，两层实测

玩家明确要求这一点。做法（缺一不可 ✓）：

1. **依赖 optional**：写成 required 的话，没装 TLM 的人会直接卡在"缺少依赖"错误屏 ✗。
2. **入口守卫**：`ExampleMod` 构造函数第一行判 `ModList.get().isLoaded("touhou_little_maid")`，
   不在就 log 一行然后 `return` ✓；并且入口类本身**不出现任何 TLM 类型**（方法体里的引用不会被解析，
   因为 return 之前不会执行到 ✓）。
3. **整份 mixin 配置门控**：`MaidMixinPlugin implements IMixinConfigPlugin`，
   在 `onLoad` 里探测 TLM 是否**可加载**，`shouldApplyMixin` 直接返回该结果 ✓
   ⇒ 没装 TLM 时**一个 mixin 都不注入** ✓。
   注意：兼容的 17 个 mixin 里有 7 个直接注入 TLM 类，**注入我们自己类的那几个也在代码里引用
   `EntityMaid`**，所以必须整份关掉，不能只关 TLM 目标那部分 ✓。

另外把 6 个原先靠 `@EventBusSubscriber` 自动注册的类**改成启动时显式注册**（`NeoForge.EVENT_BUS.addListener(...)`）
✓ —— 注解扫描会**加载**被注解的类，没装 TLM 时那就是 NoClassDefFoundError ✗。

**实测（同一个 jar，只切换 `run/mods` 里有没有 TLM）：**

| | 有 TLM（`server-96-TLM.txt`） | 无 TLM（`server-95-noTLM.txt`） |
|---|---|---|
| 服务器 | `Done (1.143s)` ✓ | `Done (1.085s)` ✓ |
| 兼容日志 | `Touhou Little Maid detected - enabling ...` ✓ | `... not detected - skipping every maid compatibility mixin` + `... the compat is disabled` ✓ |
| 注入的 mixin | **9 个** ✓ | **0 个** ✓ |
| ERROR/FATAL | 0 ✓ | 0 ✓ |

外加一条长期保险：`tools/audit_tlm_isolation.py` 断言**宿主（`top.ribs.scguns` 源码 + 宿主 jar 里除嵌套 jar 外的全部条目）
零 TLM 引用** ✓（实测 0/0 ✓）—— 宿主一旦沾上 TLM，没装 TLM 的玩家就会 NoClassDefFoundError ✓。

## 36.5 移植时发现并修掉的问题（其中 3 个是 1.20.1 时代就存在的）

1. **`@EventBusSubscriber` 在 NeoForge 上更严**：`SCG2TLMConfig` 只是个配置 spec，却被注解了
   `@Mod.EventBusSubscriber` ⇒ NeoForge 的 `AutomaticEventSubscriber` 直接
   `IllegalArgumentException: class ... has no @SubscribeEvent methods, but register was called anyway`
   ⇒ **崩启动** ✓（Forge 1.20.1 会静默忽略 ✗）。去掉注解即可 ✓。这条只有真跑服务器才看得见 ✓。
2. **mixin 插件的探测方式本身会把游戏弄崩**（我自己第一版就踩了 ✓）：`Class.forName("...EntityMaid")`
   会连带解析父类链 → 触发加载 `LivingEntity` → geckolib 自己的 `LivingEntityMixin` 报
   `MixinTargetAlreadyLoadedException: ... target LivingEntity was loaded too early` ✗。
   改成**用 `ClassLoader.getResource("...EntityMaid.class")` 探测**（只看资源存在性、不加载类 ✓）
   ⇒ 同一场景通过 ✓。
3. **`ProjectileEntityMixin` 的 `@Redirect` 签名是错的**（1.20.1 源码里就错 ✓，而且**从来没生效过** ✓
   —— 玩家成品 1.0.8 的 jar 里这个类只有 `onHitEntity` 一个方法，重定向是后来才加的 ✓）。
   Mixin 报 `Found unexpected argument type ... ProjectileEntity at index 0, expected ...Level`，
   并把**正确签名**打了出来：`(Level, Entity, AABB, Predicate, Vec3, Vec3)` ✓ —— 尾参数必须是
   **目标方法自身的参数**（`findEntityOnPath(Vec3, Vec3)` ✓），不是弹体实例 ✓。照报错改成
   「被调用方实例 + 实参 + 目标方法参数」后通过 ✓。
4. **`EntityMaidShieldMixin` 还在用 SRG 名**：`method = "m_6469_"`（hurt）/ `"m_8119_"`（tick）
   ✗ —— 1.21.1 的 NeoForge 运行期是官方名，SRG 名已不存在 ✓（当时靠 `remap=false` 匹配得上 ✓）。
   改成 `hurt` / `tick`（javap 核对 TLM 的 `EntityMaid` 确实有这两个 ✓）。
5. **5 个进度的触发条件 id 是错的**：它们写 `touhou_little_maid:maid/tamed_maid` ✗，
   而 TLM 1.5.3 只注册了 **`touhou_little_maid:maid_event`** ✓（TLM 自己的进度就是这么写的 ✓，
   `InitTrigger` 字节码里就是 `"touhou_little_maid"` + `"maid_event"` ✓）⇒ 这 5 个进度
   **从来没弹出过** ✓（1.20.1 也一样 ✗）。改对之后实测 `advancement errors: 0` ✓（改之前是 5 条 ERROR ✓）。
6. **TLM 相关数据在没装 TLM 时会报错**（mixin 和入口都关了，但 data 照常加载 ✓）：
   - 5 个进度引用 TLM 的判据 ⇒ `Parsing error loading custom advancement ... Unknown registry key`；
   - `data/scgextra/tags/entity_type/factions/player.json` 里的 `touhou_little_maid:maid`
     ⇒ `Couldn't load tag ... as it is missing following references`。
   两条都**实测**修好：进度加 `neoforge:conditions: [{type: neoforge:mod_loaded, modid: touhou_little_maid}]` ✓
   （NeoForge 对进度**确实支持** conditions ✓ —— 这条是实测，我一开始因为测试用了**过期的 jar**
   误判成"不支持" ✗，见 §36.7）；标签那条把 `touhou_little_maid:maid` 写成
   `{"id": ..., "required": false}` ✓ ⇒ 无 TLM 时 0 条 ERROR ✓。

## 36.6 有意的取舍（两处，都需要玩家知情）

1. **Cloth Config 配置屏被删掉了** ✓：`SCG2TLMClothConfig` / `AddClothConfigEventListener`
   依赖 `me.shedaniel.clothconfig2` ✗ —— 玩家的实例里**没有装 Cloth Config**，TLM 的 jar 里
   也没有内嵌 ✓。所以配置屏去掉，配置项**照常工作**（`SCG2TLMConfig` 走 NeoForge 的
   `ModConfigSpec` ✓，落在 `config/scg2_maid_compat-common.toml` ✓），只是不能在游戏里点着改 ✓。
2. **`SulfurheadEntityMixin` 删掉了** ✓：它 `@Overwrite` 了 `SulfurheadEntity#hurt`，
   目的是"让非玩家来源（女仆的子弹）也能打硫磺头" ✓ —— 但玩家此前已要求**把这个限制整个去掉** ✓
   （HANDOFF §23.5）⇒ 宿主已经没有这个重写了 ⇒ mixin 的 `@Overwrite` 找不到目标 ⇒ **崩启动** ✓。
   删掉即正确（宿主行为已经覆盖了它的目的 ✓）。这条也说明：**宿主改过的行为，兼容侧要同步检查** ✓。

## 36.7 工具（本次新增，都可重复运行）

| 工具 | 作用 |
|---|---|
| `tools/port_rewrite_maid.py` | 机械改写（幂等，可反复跑）；顶部列出"故意不自动改"的 API ✓ |
| `tools/check_maid_compat.py [--tag X]` | 用 javac 直接编 maid-compat（秒级，不必过 Gradle）；`--tag` 让多人/多代理并行跑不互相踩 ✓ |
| `tools/check_maid_mixins.py` | 静态检查每个 mixin 的注入点/成员在目标类里是否存在（实测抓到 4 个问题 ✓） |
| `tools/audit_tlm_isolation.py` | 断言宿主零 TLM 引用（源码 + jar 字节 ✓） |

**踩过的坑（写下来免得下次再花时间）**：① `javac` 的 `@argfile` 会把 `\` 当转义字符 ⇒
类路径要写成正斜杠；② TLM 的 jar 文件名带空格（`[车万女仆] ...`）⇒ 直接用会解析失败，先复制成无空格名 ✓；
③ PowerShell 里 `[...]` 是**通配符** ⇒ 引用这个文件名必须用 `-LiteralPath` ✓
（我第一次 `Copy-Item` 静默失败，结果**测的是过期的 jar**，白跑了三轮 ✗）；④ 被 kill 的 job 里
`cmd`/`java` 可能还活着并**占着 Gradle 锁** ⇒ 下一次 `gradlew` 会一直不起来 ✓，要显式
`Stop-Process java,cmd` ✓。

## 36.8 未验证 / 待玩家确认

- **游戏内表现完全没验证** ✗（女仆拿枪、动画、医疗、盾牌、聊天气泡、阵营友伤都只能在客户端看 ✓）。
  已安装到玩家实例（`scguns-0.5.5.jar`，备份 `.bak-maid` ✓），mod 列表里应出现
  **Scorched Guns: Maid Compat 1.0.8** ✓。
- **能量枪回充的运行时风险**（子代理发现并如实上报 ✓）：`SC2GunCompat` 里给能量枪回充走的是
  `Capabilities.EnergyStorage.ITEM`，而 NeoForge 的物品能力**必须在 `RegisterCapabilitiesEvent` 里注册** ✗
  —— 供能的枪来自插件 mod（`top.ribs.scguns.common.item.gun.RechargeableEnergyGunItem`），
  那个 jar 不在 `libs/` 里，**无法验证它到底注册了没有** ✓。若没注册 ⇒ `stored` 恒为 0 ⇒
  女仆的能量枪不会回充 ✓（功能缺失，不崩 ✓）。需要玩家在游戏里试，或把插件 jar 给我看一下 ✓。
- 顺带确认：`data/scguns/guns/turnpike.json`（兼容往本模组命名空间里加的一把枪 ✓）与
  `assets/scg2_maid_compat/animation/melee.animation.json` 都随嵌套 jar 正常加载 ✓，没有冲突 ✓。




# 37. 枪锈诅咒（gun_rust）不是"诅咒类附魔" —— **1.21 把它变成了标签，我们一个都没带**

玩家原话：**"枪锈诅咒实际上是诅咒类附魔，当前是普通附魔"**。属实 ✓。

## 37.1 0.5.5 是怎么写的（实测）

`GunRustEnchantment` 重写了两个方法 ✓：

```java
public boolean isTreasureOnly() { return true; }
public boolean isCurse()        { return true; }
```

而且扫描整个 `top/ribs/scguns/enchantment` 包实测：**它是 15 个附魔里唯一带这两个标记的** ✓
（其余 14 个既不是诅咒也不是宝藏 ✓）。

## 37.2 1.21 没有这两个方法了 ⇒ 改成**标签**（字节码实测）

- `javap` 看 `net.minecraft.world.item.enchantment.Enchantment`：**没有 `isCurse()`、也没有
  `isTreasureOnly()`** ✗（两个方法都被删掉了）。
- 那"是不是诅咒"在 1.21 由什么决定？看 `Enchantment.getFullname` 的字节码 ✓：

  ```
  19: getstatic  net/minecraft/tags/EnchantmentTags.CURSE
  22: invokeinterface net/minecraft/core/Holder.is(TagKey)
  34: getstatic  net/minecraft/ChatFormatting.RED
  ```

  ⇒ **在 `#minecraft:curse` 里就是诅咒** ✓（名字显示为红色 ✓）。
- 砂轮也一样 ✓：`GrindstoneMenu.removeNonCursesFrom` 里就一句 `holder.is(EnchantmentTags.CURSE)` ✓
  ⇒ **只有诅咒不会被砂轮剥掉** ✓。

原版两个诅咒的写法印证了这点 ✓：`vanishing_curse.json` 带 `minecraft:prevent_equipment_drop`、
`binding_curse.json` 带 `minecraft:prevent_armor_change` ✓（那是**效果** ✓），而"是不是诅咒"这一层
由 `data/minecraft/tags/enchantment/curse.json` 列 `minecraft:binding_curse` / `minecraft:vanishing_curse` 决定 ✓。

## 37.3 我们之前只带了 `non_treasure.json` ⇒ 两个错

上一轮为了修"附魔台不提供 + 图书管理员不进货"，我们带了
`data/minecraft/tags/enchantment/non_treasure.json`（15 个全列 ✓）—— 这解决了那两个问题 ✓，
但顺带造成：

1. `gun_rust` **不是诅咒** ✗（名字不会红 ✓、砂轮可以把它剥掉 ✗）；
2. `gun_rust` 被列进了 **non_treasure** ✗ —— 而 0.5.5 明确 `isTreasureOnly() → true` ✓
   ⇒ 它本不该出现在**附魔台**里 ✓，也不该被**图书管理员**卖 ✓（这两处读的正是
   `#in_enchanting_table` / `#tradeable`，二者都由 `#non_treasure` 构成 ✓）。

## 37.4 修法（4 个标签文件，一处移动）

| 文件 | 内容 | 依据 |
|---|---|---|
| `data/minecraft/tags/enchantment/curse.json` | `scguns:gun_rust` | 0.5.5 `isCurse()=true` ✓；决定红色 + 砂轮不剥 ✓ |
| `data/minecraft/tags/enchantment/treasure.json` | `scguns:gun_rust` | 0.5.5 `isTreasureOnly()=true` ✓ |
| `data/minecraft/tags/enchantment/non_treasure.json` | **移除** `scguns:gun_rust` | 宝藏附魔不能同时算非宝藏 ✓（否则自相矛盾 ✓） |
| `data/minecraft/tags/enchantment/on_random_loot.json` | `scguns:gun_rust` | 对齐原版：诅咒**可以**出现在随机战利品里 ✓（`#on_random_loot` 里原版就列了两个诅咒 ✓），而移出 non_treasure 会让它掉出这个标签 ✗ |

全部 `"replace": false` ✓（与其它数据包合并、不覆盖原版 ✓）。

## 37.5 实测（运行期，不是读代码）

临时加了一个 `ServerStartedEvent` 诊断（打印每个 scguns 附魔在各标签里的归属 ✓，验证完**已删除** ✓），
服务器实测输出 ✓：

```
accelerator ... non_treasure=true  table=true  tradeable=true  loot=true      ← 其余 14 个都是这样 ✓
gun_rust       curse=true  treasure=true  non_treasure=false  table=false  tradeable=false  loot=true
total=15   vanilla curse tag=3        ← 绑定诅咒 + 消失诅咒 + 我们的 gun_rust ✓
```

⇒ `gun_rust` 现在**确实是诅咒** ✓、**不再进附魔台/图书管理员** ✓（对上 0.5.5 的宝藏标记 ✓）、
**仍会出现在随机战利品** ✓；其余 14 个附魔的归属**完全没有变化** ✓（玩家上一轮修好的"附魔台能附魔枪械 +
图书管理员卖书"没有被破坏 ✓）。

对玩家的可见变化：枪锈的名字**变红** ✓、**砂轮剥不掉它** ✓（这两条正是"诅咒"的语义 ✓）、
附魔台不再提供它 ✓、图书管理员不再卖它 ✓；怪物枪上的 85% 掷咒**完全不受影响** ✓（那条走的是
`GunCurseUtil` 自己的掷骰 ✓，与标签无关 ✓）。

## 37.6 新增打包检查 + 一次自己的失误（记下来）

`tools/verify_installed_jar.py` 新增 4 项（第 3 项是**反向**检查 ✓，因为"没重复列"才是对的 ✓）：

```
gun rust is in the vanilla curse tag             OK
gun rust is treasure-only, as in 0.5.5           OK
gun rust is not also in non_treasure             OK
gun rust can still appear on random loot         OK
```

**自测**：拿修复前的 jar（`scguns-0.5.5.jar.bak-gunrustcurse`）跑同一套检查 ⇒ **4 项全 FAIL、84/88** ✓，
换新 jar ⇒ **88/88** ✓ —— 新检查确实能失败，不是摆设 ✓。

**我这次的失误（值得写下来）**：做运行期验证时，我把临时诊断类放在
`src/main/java/top/ribs/scguns/debug/` ✗ —— 这个包**本来就有东西** ✗（`Debug.java`、`IDebugWidget.java`、
`client/screen/EditorScreen.java` 等 8 个文件 ✓），而验证完我用 `Remove-Item -Recurse` 删目录 ⇒
**把原有 8 个文件一起删了** ✗，下一次编译立刻报
`package top.ribs.scguns.debug does not exist`（`ClientHandler` / `Gun` 都在用 ✓）。
`git checkout -- src` 已完整恢复 ✓。教训两条：① 临时文件**不要**放进已存在的包 ✓；
② 删目录前先看里面有什么 ✓。顺带第二个坑：`git checkout -- src` 会连**本次未提交的改动**一起还原 ✗
（`non_treasure.json` 那处编辑就是这么丢的 ✓，重新改了一次 ✓）—— 恢复范围要看着用 ✓。




# 38. 环境坑（**最贵的一条**）：DSH 后台是被 `cmd.exe` 拉起来的 —— 绝对不要 mass-kill `cmd`

玩家报告："dsh 后台报错，报错后就动不了了"。查清了 ✓，**是我自己的命令把 DSH 干掉的** ✗。

## 38.1 症状与现场还原（全部来自会话记录与会话日志，不是猜）

会话记录（`~/.dsh/sessions/<proj>/<sid>/session.jsonl.zstd`，这个格式是**一串独立 zstd 帧** ✓，
用 Node 24 的 `zlib.zstdDecompressSync` 逐帧解就能读 ✓）里，两次事件长得完全一样：

```
16:02:20  tool/call    pwsh  ← 命令开头是  Get-Process java,cmd | Stop-Process -Force ...
16:02:20  tool/result  id="interrupted-tool-result-…"   ← 同一毫秒就被取消（不是超时）
16:02:20  turn/end     reason={"kind":"interrupted"}
   …  约 8 分钟：记录里**什么都没有**（这就是玩家感觉到的"动不了"）…
16:20:46  session/end-seed        ← 会话被**从磁盘重新加载**（Session 构造时追加的种子标记 ✓）
16:20:55  agent/inbox/spliced     玩家："继续"
16:20:57  turn/start   turn=50
```

时间戳能对上：**DSH 后端进程（`node.exe`）的启动时间是 16:20:06** ✓ —— 也就是它死于 16:12:15、
在 16:20 才被重新拉起来 ✓。（另一次 16:02:20 同样 ✓。）

两个名字要认清楚 ✓：
- `turn/end {"kind":"interrupted"}` 与 `interrupted-tool-result-…` 是**会话修复**写出来的 ✓
  （`packages/core/session/src/repair.ts`：日志里有一个没闭合的 turn 时，补一个 interrupted 收尾 ✓）
  ⇒ 看到它就意味着 **DSH 是在工具执行途中被终止的** ✗，而不是工具自己失败 ✓。
- `session/end-seed` 表示**历史被重新加载** ✓（`packages/core/session/src/index.ts`：构造 Session 时
  从 seed 恢复就追加这个标记 ✓）⇒ 它出现＝DSH 重启过 ✓。

`{kind:"aborted", reason:{kind:"user"}}`（15:45 那次）是另一条路 ✓：那是玩家按了停止 ✓。
14:37 那次 `assistant/chunk finish={"kind":"error", …maximum context length…}`（1,048,576 上限、
请求了 1,049,479 ✓）**DSH 自己处理得很好** ✓：立刻 `compaction/prune` → 生成摘要 → 注入 checkpoint →
`compaction/end` → 继续跑 ✓（就是本次对话开头那段"compacted summary" ✓）。

## 38.2 根因（进程链实测）

`Get-CimInstance Win32_Process` 拉出来的父子链 ✓：

```
explorer.exe#8976
 └─ cmd.exe#11820        ← "cmd /c E:\deepseek\start-dsh.bat"   （玩家双击的启动脚本）
     └─ node.exe#10556   ← pnpm dsh web
         └─ cmd.exe#15620
             └─ node.exe#17268
                 └─ cmd.exe#4412    ← ★ DSH 后端的**直接父进程**
                     └─ node.exe#8140   ← ★ DSH 后端
                         └─ powershell.exe  ← 我执行命令用的 shell
```

⇒ DSH 是被一串 `cmd.exe` 夹着拉起来的 ✓，而我的命令 `Get-Process java,cmd | Stop-Process -Force`
**把这些 cmd 一起杀了** ⇒ 后端及其父 cmd 被终止 ⇒ DSH 死 ✓✓✓。
我当时以为"我只杀 java 和 cmd，我的 shell 是 powershell，所以安全" ✗ —— 我的直系里确实没有 cmd ✓，
但**DSH 上游有** ✗。

后果（当时看像玄学，现在全解释得通）：工具调用没有持久化结果（前端显示 "interrupted"）✓、
后台 job 全丢 ✓、排队的 `gradlew runServer` 永远起不来（Gradle 锁还在）✓、
两次"工具调用被中断、结果未知" ✓。

## 38.3 以后的规矩（写死在这里）

1. **绝不** `Stop-Process cmd` / `taskkill /im cmd.exe` / 任何按名字杀全体的写法 ✓。
2. 要杀开发服务器，**按命令行精确匹配** ✓（这条已经在用 ✓）：
   ```powershell
   Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
     Where-Object { $_.CommandLine -match 'devlaunch' } |
     ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
   ```
   顺带：`GradleDaemon` 那种 java **不用杀** ✓（杀了只会让下次构建变慢 ✓）。
3. 需要"清场重来"时，只杀自己起的东西（job id / 明确 pid）✓，并先 `Get-CimInstance`
   看清楚它的父子链再动手 ✓。
4. 另一条（同一类）：被 kill 的 job 里 `cmd`/`java` 可能还活着并占着 Gradle 锁 ✓
   ⇒ 下一次 `gradlew` 会一直不起来 ✓（见 §36.7 ④）✓。




# 39. 枪械工作台支持原版配方书

玩家想法："让工作方块支持 Minecraft 的配方书机制"，确认范围后**先只做枪械工作台（GunBench）** ✓。

## 39.1 1.21 的配方书是怎么回事（全部实测）

| 事实 | 依据 |
|---|---|
| `RecipeBookMenu` 是**抽象类**（不是接口），要 8 个抽象方法 | `javap` ✓：`fillCraftSlotsStackedContents`/`clearCraftingContent`/`recipeMatches`/`getResultSlotIndex`/`getGridWidth`/`getGridHeight`/`getSize`/`getRecipeBookType`/`shouldMoveToInventory` ✓ |
| 默认 `handlePlacement` 走 `ServerPlaceRecipe` | 字节码里 `ServerPlaceRecipe.recipeClicked(...)` ✓，它用 `getSlot(i)`/`getSize()`/`getGridWidth()` 寻址 ✓ |
| **`RecipeBookType` 与 `RecipeBookCategories` 都是可扩展枚举** | 都是 `IExtensibleEnum` ✓，NeoForge 通过 `META-INF/enumextensions.json` 加常量 ✓ |
| 扩展常量的获取方式 | 代码里**不能直接引用**（编译期不存在 ✗）⇒ 用 `RecipeBookType.valueOf("…")` ✓（Farmer's Delight 就是这么写的，字节码实测 ✓） |
| `RecipeBookCategories` 的构造是 `(Supplier<List<ItemStack>>)` | `javap` ✓ ⇒ 扩展条目要用 `EnumProxy<RecipeBookCategories>` 提供图标 ✓（`EnumProxy(Class, Object...)` ✓） |
| 自定义配方怎么进分类 | NeoForge 的 `RegisterRecipeBookCategoriesEvent#registerRecipeCategoryFinder(RecipeType, fn)` ✓ —— 原版 `ClientRecipeBook` 只认 `CraftingRecipe`/`AbstractCookingRecipe` ✓，没有这个钩子我们的配方会被归到"未分类"✗ |
| 客户端模板 | `.refs/nf-src` 里有**真正的 1.21.1 源码** ✓，`CraftingScreen` 就是模板（`implements RecipeUpdateListener` + `RecipeBookComponent` + 位置偏移 + 输入转发 ✓） |

## 39.2 做法

| 文件 | 作用 |
|---|---|
| `src/main/resources/META-INF/enumextensions.json` | 加 `RecipeBookType.SCGUNS_GUN_BENCH`、`RecipeBookCategories.SCGUNS_GUN_BENCH_{SEARCH,MISC}` ✓（后者用 `EnumProxy` 提供图标 ✓） |
| `client/recipebook/ScgunsRecipeBookCategories` | 两个 `EnumProxy` 字段 + `init()` 里 `valueOf` 取常量 ✓（**客户端**，因为 `RecipeBookCategories` 只存在于客户端 ✓） |
| `common/recipe/ScgunsRecipeBookTypes` | **通用**（服务端也要 ✓）：懒加载 `RecipeBookType.valueOf("SCGUNS_GUN_BENCH")` ✓（懒，避免类初始化早于枚举扩展 ✗） |
| `client/recipebook/GunBenchRecipeBookCategories` | 事件处理器：`registerBookCategories` + `registerAggregateCategory`（搜索页聚合 ✓）+ `registerRecipeCategoryFinder` ✓，由 `ClientHandler.registerClientHandlers` 在 dist 判断下注册 ✓ |
| `GunBenchMenu` | 改成 `extends RecipeBookMenu<ContainerRecipeInput, GunBenchRecipe>` ✓，实现 8 个方法 + 自定义 `handlePlacement` ✓ |
| `GunBenchScreen` | 照 `CraftingScreen` 接上 `RecipeBookComponent`（开关按钮、书打开时面板右移、ghost recipe、输入转发、`RecipeUpdateListener` ✓） |
| `tools/gen_gun_bench_unlocks.py` | 生成 146 条解锁进度 ✓ |

**网格设计**：工作台有 10 个"带类型限制"的模块槽 + 蓝图槽 + 输出槽 ✓。因为 `GunBenchRecipe.matches` 是**按下标**比较 `container.getItem(i)` 与 `recipeItems.get(i)` ✓，而 `ServerPlaceRecipe` 是 `getSlot(i)` 寻址 ✓ ⇒ 声明 `getGridWidth()=1`、`getGridHeight()=10`、`getSize()=10` 就正好把"配方第 i 个原料 ↔ 菜单第 i 个槽"对上 ✓。蓝图不在网格里 ⇒ 由我们自己的 `handlePlacement` 在 `super` 之后放入 ✓（并在这一轮**抑制工作台自身的 auto-craft** ✓，否则它会按蓝图里存的*旧*配方把刚填好的槽清掉重填 ✗）。

**顺带修掉 0.5.5 的一个真 bug**：`GunBenchMenu` 把蓝图槽 **addSlot 了两次** ✓（两个菜单槽共享容器 11、同一坐标 ✓）⇒ 连带 `quickMoveStack` 里的范围 `11..12` 实际指向**输出槽** ✗（shift+左键把蓝图往输出槽塞 ✓ 失败后落进模块槽 ✓）。现在只加一次，并改用 `MENU_SLOT_BLUEPRINT` 常量 ✓。

## 39.3 解锁数据（否则面板是空的）

配方书**只显示已解锁的配方** ✗，而这个模组原本**没有任何 `data/scguns/advancement/recipes/**`** ✗ ⇒ 就算界面接好也是空的 ✓。
`tools/gen_gun_bench_unlocks.py` 为 146 个 gun_bench 配方各生成一条进度 ✓，触发条件是**玩家拥有该配方对应的蓝图**（`minecraft:inventory_changed` ✓）—— 正好贴合模组"蓝图分级"的进度设计 ✓（拿到铜蓝图 ⇒ 解锁全部铜级配方 ✓）；蓝图不是具体物品的 4 条用 `minecraft:tick` 兜底 ✓。

## 39.4 验证到什么程度

- ✅ `gradlew build` 通过 ✓；20 个审计 0 失败 ✓（含 dist 安全检查：新代码没有把客户端类型漏进通用签名 ✓）。
- ✅ 专用服务器实测：`Done`、**146 条解锁进度零解析错误** ✓、配方零解析错误 ✓、无 ERROR/FATAL ✓。
- ❌ **界面本身没法 headless 验证** ✗：书的开关、页签、ghost recipe 位置、点击后一键填充 —— 都要玩家在游戏里看 ✓（和蓝图界面同一条限制 ✓）。已知需要盯的点：`GunBenchScreen` 的书打开时面板右移是否与自定义贴图对齐 ✓；ghost recipe 是否落在 10 个模块槽上 ✓。

## 39.5 首版把玩家的游戏**搞崩了** —— 原因、兜底、当前进度（**功能仍不可用**）

**症状**：装上新 jar 后客户端启动即崩 ✓：

```
Scorched Guns (scguns) encountered an error while dispatching RegisterRecipeBookCategoriesEvent
java.lang.IllegalArgumentException: No enum constant net.minecraft.client.RecipeBookCategories.SCGUNS_GUN_BENCH_SEARCH
```

`RegisterRecipeBookCategoriesEvent` 里抛异常会被 NeoForge 当成"mod 加载失败" ⇒ 直接崩 ✗。**枚举扩展没生效** ✓。

### 已确证的事实（逐条有实测）

| 结论 | 证据 |
|---|---|
| 扩展**文件本身没问题** | 与 Farmer's Delight 的 `META-INF/enumextensions.json` **结构逐字段一致** ✓（都能解析 ✓、无 BOM ✓、路径都正确 ✓） |
| 我们的扩展**根本没被读** | 把文件**故意改成非法 JSON** 再跑 ⇒ **毫无报错** ✓ ⇒ 不是"条目不合法"，是"文件没被读" ✓ |
| **dev 环境测不出来** | dev 运行里我们的 mod 是**目录式** mod（日志 `Found valid mod file main with {scguns}` ✓），FD 是 **jar** ✓ 且它的扩展**在 dev 里生效** ✓（探针实测 `constants=[..., FARMERSDELIGHT_COOKING]` ✓）⇒ 枚举扩展只从 **jar** 里读 ✓ |
| 玩家客户端（jar）里**分类条目**没生效 | 崩在 `valueOf("SCGUNS_GUN_BENCH_SEARCH")` ✓（就是带 `EnumProxy` 参数的两条 ✓）；**无参数**的 `RecipeBookType` 那半是否生效**尚未测到** ✗（崩在它之后 ✓） |
| 移动代理类不管用 | 先把 `EnumProxy` 从 `client` 包挪到 `common` 包（照 FD 的做法 ✓）⇒ **仍然不生效** ✗ ⇒ 不是包的位置问题 ✓ |
| **真正的原因：`mods.toml` 少一行声明** | FD 的 `neoforge.mods.toml` 里有 **`enumExtensions="META-INF/enumextensions.json"`** ✓，我们没有 ✗ ⇒ **NeoForge 不会自动扫描这个文件** ✓ ⇒ 整个文件被静默忽略 ✓（完美解释"写坏 JSON 也毫无报错" ✓） |

### 修法（一行）

`src/main/templates/META-INF/neoforge.mods.toml` 的 `[[mods]]` 段里加 ✓：

```toml
enumExtensions="META-INF/enumextensions.json"
```

**实测生效** ✓（临时服务端探针 ✓，跑完已删 ✓）：

```
SCGUNS-ENUMPROBE recipeBookTypes=[CRAFTING, FURNACE, BLAST_FURNACE, SMOKER, FARMERSDELIGHT_COOKING, SCGUNS_GUN_BENCH]
                                                                                          ↑ 我们的类型出现了 ✓
```

⇒ 顺带**推翻我前一条推断** ✗：不是"目录式 mod 读不到"（dev 运行下同样生效 ✓），就是少了声明 ✓。
**我在这里绕了很久**（试过改 JSON 内容、故意写坏、挪包、比对字节 ✗），教训是：**先去看能工作的同类 mod 的 `mods.toml`，而不是先怀疑打包** ✓。

### 已做的兜底（**这类崩溃不会再发生**）

1. `ScgunsRecipeBookCategories.init()` 与 `ScgunsRecipeBookTypes.gunBench()` 全部 **try/catch** ✓：
   取不到常量就记 WARN 并跳过注册 ✓（类型回退 `RecipeBookType.CRAFTING` ✓）—— **绝不在事件里抛异常** ✓。
2. 客户端启动打一条 **INFO 诊断** `SCGUNS-RECIPEBOOK type=... book=enabled|disabled(...)` ✓
   —— 这是唯一能在真实（jar）客户端里判定扩展是否生效的手段 ✓。
3. 已重新安装到玩家实例 ✓ 且 `verify_installed_jar` **88/88** ✓（游戏可正常启动 ✓）。

### 下一步：需要一次客户端启动（带诊断的 jar 已装好）

看 `latest.log`（`D:\MCJAVA\.minecraft\versions\1.21.1-NeoForge_21.1.250\logs\latest.log`）里的 `SCGUNS-RECIPEBOOK` 行 ✓：
- `type=SCGUNS_GUN_BENCH book=enabled` ⇒ 生效 ✓ ⇒ 接着验界面（开关、页签、ghost recipe、一键填充 ✓）；
- `type=CRAFTING book=disabled(...)` ⇒ 客户端侧的分类仍是没生效 ✗ ⇒ 那时才需要再查客户端那一半 ✓。

**状态**：`mods.toml` 的声明已补齐并**在服务端实测生效** ✓（`SCGUNS_GUN_BENCH` 出现在枚举里 ✓）；
客户端分类那半的最终确认仍需一次客户端启动 ✓（但我已经把它做成"缺了就退化成没有配方书" ✓，
**不会再出现"配方书里显示原版工作台配方"这种误导** ✗ ✓）。

## 39.6 玩家实测后报的两个问题（都已修）

玩家截图确认 **配方书能开、页签是我们自己的、点击后 ghost 投影也出现了** ✓，同时报了两个问题 ✓：

### ① 书里只有 2 条配方（应为 11 条铜级的）

**根因**：我生成的解锁进度用 `minecraft:inventory_changed` ✓（写法本身正确 ✓，与原版
`adventure/salvage_sherd.json` 的形状一致 ✓）—— 但该判据**只在背包"发生变化"时触发** ✗
⇒ **玩家早在这些数据存在之前就拿着铜蓝图了** ✗ ⇒ 永远不会触发 ✓。
显示的 2 条恰好是**没有蓝图、用 `minecraft:tick` 兜底**的那几条 ✓（146 条里有 4 条无蓝图 ✓）✓。

**修法**：登录时扫一遍背包 ✓，把"玩家已持有的蓝图所对应的 gun bench 配方"直接
`player.awardRecipes(...)` 解锁 ✓（`GunProgressionEventHandler.awardBenchRecipesForHeldBlueprints` ✓）。
新拿到的蓝图仍走进度那条路 ✓（拾取 ⇒ `inventory_changed` ⇒ 立即解锁 ✓）。

### ② ghost 投影错位一格

**根因**：原版 `RecipeBookComponent.setupGhostRecipe` 把**结果**画在 `slots.get(0)` ✓，而
`PlaceRecipe.placeRecipe` 的槽位**从 0 开始计数**、并在经过 `outputSlot` 时跳一格 ✓
⇒ 原版假定 **输出槽就是菜单第 0 槽** ✓（`CraftingMenu` 正是如此 ✓）。
我们的菜单当时是"模块 0..9 / 蓝图 10 / 输出 11" ✗ ⇒ **整个投影错一格** ✓（结果落在第一个模块槽上 ✓）。

**修法**：把菜单槽序改成 **0 = 输出**、**1..10 = 十个模块槽（＝网格，按配方原料顺序）**、
**11 = 蓝图** ✓，与 `placeRecipe` 的计数方式完全对齐 ✓（并同步 `MENU_SLOT_*`、`getResultSlotIndex()`、
`shouldMoveToInventory()`、shift 左键的范围 ✓）。这样**不需要**自定义 `RecipeBookComponent` 子类 ✓，
用原版逻辑就能落准 ✓。

**验证**：build ✓、20 审计 0 失败 ✓、`verify_installed_jar` **88/88** ✓、已装入玩家实例 ✓；
效果仍需玩家在游戏内确认 ✓（书里配方条数、投影是否严丝合缝、点击是否一键填充 ✓）。




# 40. 枪械等级提示：默认不提示 + 措辞没说明后果

玩家两轮反馈：① "枪械等级进度不够明显，不会提示玩家当前可能会出现的敌人装备和袭击"；② 补充 **"不够明显的原因是他不在聊天栏提示"** ✓。

## 40.1 根因（实测，两件事）

1. **开关默认是 `false`** ✗：`Config.CLIENT.display.showProgressionMessages` 定义时 `.define("showProgressionMessages", false)` ✓
   —— 而且**0.5.5 也是 false** ✓（比对反编译源码 ✓）⇒ 不是移植改坏的，是原设计里"要玩家自己去开" ✓。
   发送逻辑整个被这个开关包着 ✓（`if ((Boolean)Config.CLIENT.display.showProgressionMessages.get())` ✓）⇒ 默认状态下**一条都不发** ✓，玩家自然"没在聊天栏看到" ✓✓。
2. **措辞只说解锁、不说后果** ✗：三条消息是
   `"Gun Tier Unlocked: %s"` / `"Enemies can now spawn with:"` / `"These Raids will now target you:"` ✓
   —— 第二、三条是"悬空冒号 + 后面拼接列表" ✓，而且分隔符在代码里硬编码 `", "` ✗（中文里该用顿号 ✓）。

## 40.2 改动

| 项 | 前 | 后 |
|---|---|---|
| 默认值 | `false` ✗ | **`true`** ✓（**有意偏离 0.5.5**，源码注释里写明原因 ✓） |
| 解锁提示 | `Gun Tier Unlocked: %s` | EN `You obtained a [%s]-tier gun!` / ZH `你获得了【%s】的枪械！` ✓ |
| 后果提示 | `Enemies can now spawn with:` + 列表（两条消息） | **合并成一句**：EN `[%s] enemies and raids may now appear!` / ZH `现在可能会出现【%s】的敌人和袭击！` ✓（就是玩家给的句式 ✓） |
| 袭击细节 | `These Raids will now target you:` | EN `Raids that may now target you: %s` / ZH `现在可能针对你的袭击：%s` ✓（袭击名作为参数放进句子里 ✓） |
| 分隔符 | 代码硬编码 `", "` ✗ | 新键 `progression.scguns.list_separator`（EN `, ` / ZH `、`）✓ |
| 醒目度 | 只有聊天文字 | 追加 `SoundEvents.PLAYER_LEVELUP` 提示音 ✓ |
| 触发面 | 拾取 / 原版合成 / 登录 / 工作台取出 | **新增"容器关闭"** ✓（`PlayerContainerEvent.Close` ✓）⇒ 从**箱子/战利品袋/村民交易**拿到更高等级的枪也会触发 ✓（0.5.5 只盯拾取和原版合成 ✓，所以"在结构里捡到好枪"是静默的 ✗） |

`progression.scguns.enemies_can_spawn` 这个键已无人使用 ⇒ 两个语言文件里都删掉 ✓。

## 40.3 一次自己造成的问题（记下来）

我第一版改文案时用 `json.load` + `json.dumps(sort_keys=True)` 重写了语言文件 ✗ ⇒ **en_us.json 整个重排，diff 变成 3459 行** ✗（还破坏了"en_us 与 0.5.5 键集一致、zh_cn 逐字节来自玩家汉化包"这两个性质 ✓）。
`git checkout -- src/main/resources/assets/scguns/lang/` 还原后改成**逐行替换**（每处断言只匹配一行 + 写回后 `json.loads` 复查 ✓）⇒ 现在 diff 只有 en_us/zh_cn **各 7 行**、`Config.java` **3 行** ✓。
**教训**：语言文件只做行级替换，永远不要 re-serialize ✓。

## 40.4 验证边界

- ✅ 构建通过 ✓、`audit_lang_keys`：两个语言文件 **1795/1795 键完全对齐、missing/extra 全 0** ✓（新增两个键、删掉一个旧键都同步到位 ✓）。
- ❌ 提示的实际显示（聊天栏那一行、金色粗体、音效）**只能在客户端看** ✗ —— 需要玩家进游戏确认 ✓。




# 41. 等级进度可以用指令查询（普通玩家查自己 + 管理员的 info）

玩家："如果这个进度可以通过指令查询就好了"。**其实已经有** `/scguns progression check <player>` ✓ ——
但它有三个问题，所以玩家根本用不上 ✓：

1. **要求权限 2（OP）** ✗ ⇒ 普通玩家查不了 ✓；
2. **必须带玩家参数** ✗ ⇒ 连"查自己"都得输入完整命令 ✓；
3. 输出是**硬编码英文** ✗（`Component.literal("Gun Tier: ")` ✓）⇒ 中文客户端看到的还是英文 ✓。

## 41.1 改动

| 命令 | 权限 | 说明 |
|---|---|---|
| `/scguns progression check` | **无门槛** ✓ | 查**自己** ✓（控制台执行会提示 `commands.scguns.requires_player` ✓） |
| `/scguns progression check <player>` | 2 ✓ | 查别人（保持原样 ✓） |
| `/scguns progression info <tier>` | **无门槛** ✓ | **新增** ✓：某等级**会带来什么**（`Tab` 可补全等级 id ✓），控制台也能用 ✓ |
| `/scguns progression set / clear` | 2 ✓ | 不动 ✓ |

输出全部改成**可翻译组件** ✓，给的就是玩家想看的信息（敌人等级 + 可能针对你的袭击 ✓）；袭击显示**名字**而不是配置 id ✓；
分隔符复用 §40 新增的 `progression.scguns.list_separator`（中文顿号 ✓）。
新增 7 个键、删掉 2 个已无人用的旧键（`commands.scguns.progression.check` / `check_none` ✓）；
语言文件仍是**逐行**改的 ✓（en_us / zh_cn 各 9 行 diff ✓ —— §40.3 的教训照做 ✓）。

## 41.2 实测（RCON，控制台身份）

```
/scguns progression info frontier
    Tier Frontier (level 2) brings:
    Enemies that may now appear: Antique
    Raids that may now target you: Antique Raid, Frontier Raid
/scguns progression info copper
    Tier Copper (level 3) brings:
    Enemies that may now appear: Frontier, Antique
    Raids that may now target you: Antique Raid, Frontier Raid, Copper Raid
/scguns progression info diamond_steel     → 4 个敌人等级 + 10 个袭击 ✓
/scguns progression info bogus_tier        → Invalid tier: bogus_tier ✓
/scguns progression check                  → Requires Player ✓（控制台没有玩家 ✓）
```

`check` 的"查自己"分支需要真玩家 ⇒ **只能在游戏内确认** ✗（`check <player>` 与 `info` 都已实测 ✓）。

## 41.3 顺带：`verify_installed_jar` 有一项检查过宽，已修正

第 9 项（`advancement icons use "id"`）原本要求**所有** scguns 进度都有可解析的 `display.icon` ✗ ——
但**没有 display 段的进度是合法的** ✓：原版 `data/minecraft/advancement/recipes/**` 的 **1277 条**全是
"只有 criteria/requirements/rewards" ✓（§39 之前本模组没有这类文件，所以这个假设一直没被暴露 ✓）。
改成：**没有 display 的跳过** ✓；有 display 的仍然要求 `icon` 存在、且必须是 `id` 形式（不能是 1.20.1 的 `item` ✓）。
现在输出 `advancement icons use "id" (261 files, 146 recipe unlocks have no display) OK` ✓、**88/88** ✓；
同一项在旧 jar 上是 `115 files, 0 recipe unlocks have no display` ✓ ⇒ 有 display 的那些仍在受检 ✓。

## 41.4 又一次自己踩的坑（同一类，务必记住）

改 `ModCommands.java` 时我用 `Set-Content -Encoding UTF8` 走 shell 写文件 ✗ ⇒
① Windows PowerShell 5.1 的 `-Encoding UTF8` **会写 BOM** ✗ ⇒ javac 直接报
`error: illegal character: '\ufeff'` ✓；② 文件的**行尾被整体改成 CRLF** ✓。
（这次侥幸**没有**损坏内容 —— 该文件恰好是**纯 ASCII** ✓（实测非 ASCII 字符数 = 0 ✓），
所以没出现 §40.3 那种中文乱码 ✓。）
处理：`git checkout --` 还原后**用 edit 工具**重做三处修改 ✓。
**规矩**：改仓库里的源码/数据文件一律用 `edit`/`write` 工具 ✓；shell 只在读写临时文件时用 ✓。




# 42. 玩家："悬停的枪械配方左上角有一个原版工作台的图标" + "枪械工作台只有 9 把枪的配方"

两句话，两个不同的根因 ✓ —— 一个是**移植真的漏了一个方法** ✓（已修 ✓），
另一个**还不能靠推断结案** ✓（已给出可测量的诊断手段 ✓，理由见 42.3）。

## 42.1 "原版工作台图标" —— 根因：`Recipe#getToastSymbol()` 的默认返回值就是原版工作台

**实测（读 1.21.1 源码）** ✓：

```java
// net/minecraft/world/item/crafting/Recipe.java:62
default ItemStack getToastSymbol() {
    return new ItemStack(Blocks.CRAFTING_TABLE);      // ← 原版默认 = 工作台
}
```

全仓库里**只有两处**会用这个图标 ✓（`grep getToastSymbol` 实测 ✓）：
`RecipeToast.java:48` ✓、以及各原版配方类自己的重写 ✓（`SmeltingRecipe` / `BlastingRecipe` /
`SmokingRecipe` / `CampfireCookingRecipe` / `StonecutterRecipe` / `SmithingRecipe` ✓）——

```
SmeltingRecipe     → 熔炉
BlastingRecipe     → 高炉
SmokingRecipe      → 烟熏炉
StonecutterRecipe  → 切石机
⋮
GunBenchRecipe     → （没有重写）⇒ 原版工作台 ✗✗
```

⇒ **玩家看到的"原版工作台图标"是配方解锁提示（`RecipeToast`）里画的那一格** ✓✓ ——
提示框的布局正是"**左边本站台图标 + 右边成品**" ✓ ⇒ 玩家描述成"枪械配方图标**左上角**有一个
原版工作台的图标"完全对得上 ✓（也解释了他上一轮说的"**右上角**解锁的配方都是原版工作台的图标" ✓
—— 那两次说的是同一个东西 ✓，我当时归因到"进度图标"上是错的 ✗，见 42.4）。

**修法**（`client/screen/GunBenchRecipe.java` 新增 4 行 ✓）：

```java
@Override
public ItemStack getToastSymbol() {
   return new ItemStack(ModBlocks.GUN_BENCH.get());
}
```

**顺带修的另一处同类混淆** ✓：分类标签页的图标 ✓。原来搜索页图标用的是**枪械工作台方块**
（`ModBlocks.GUN_BENCH` ✓）——工作台方块的贴图本身就是一张工作台台面 ✓，玩家自然会读成
"原版工作台" ✗。原版自己的搜索页用的是**指南针** ✓（`CRAFTING_SEARCH(new ItemStack(Items.COMPASS))` ✓，
实测 ✓）⇒ 改成指南针 ✓，与"这一页只是搜索"的语义一致 ✓，且不可能被误认成任何工作台 ✓。

> 附带确认：**原版的分类图标里根本没有工作台** ✓（实测：`COMPASS` / `BRICKS` / `REDSTONE` /
> `IRON_AXE` / `LAVA_BUCKET` ✓）⇒ 玩家看到的那个工作台图标**只能来自模组侧** ✓，
> 这条排除了"书里显示的是原版配方"的可能 ✓（见 42.3）。

## 42.2 顺手核实：客户端书里画的就是**已解锁**的枪械工作台配方（三条链，全是读源码实测）

"只有 N 条"这类问题必须先确定"书里到底按什么过滤" ✓，逐条查清 ✓：

1. **书用哪些分类** ✓：`RecipeBookMenu#getRecipeBookCategories()` 是**默认方法** ✓
   （`RecipeBookMenu.java:46` ✓），返回 `RecipeBookCategories.getCategories(getRecipeBookType())` ✓；
   我们的类型不是原版那四个 ⇒ 走 `default → RecipeBookManager.getCustomCategoriesOrEmpty(type)` ✓
   = `GunBenchRecipeBookCategories` 里注册的 `[SEARCH, MISC]` ✓。
2. **哪些配方进这些分类** ✓：`registerRecipeCategoryFinder(GunBenchRecipe.Type.INSTANCE, holder -> misc)` ✓
   ⇒ **只有 `scguns:gun_bench` 类型的配方** ✓ —— 原版合成配方进不来 ✓✓
   （这条正是"书里会不会显示原版工作台配方"的答案：**不会** ✓）。
3. **显示时按什么过滤** ✓：`ClientRecipeBook.setupCollections(recipeManager.getOrderedRecipes(), …)` ✓
   先装入**全部**配方 ✓，然后 `RecipeCollection#updateKnownRecipes(book)` ✓、
   `RecipeBookComponent.updateCollections` 里 `removeIf(c -> !c.hasKnownRecipes())` ✓
   ⇒ **只画玩家已解锁的** ✓✓。

⇒ **`9` 这个数字 = 该玩家当前已解锁的枪械工作台配方条数** ✓，不是"模组只有 9 条配方" ✓
（模组共 **146** 条 ✓，实测 ✓）。

## 42.3 但"9"到底对不对 —— 离线读存档实测，**结论是"玩家那台机器的 jar 里根本没有解锁动作生效"**

新增工具 `tools/inspect_player_recipebook.py` ✓（只读 gzip NBT ✓，纯标准库 ✓）：
直接读 `saves/<world>/playerdata/<uuid>.dat` 里的 `recipeBook.recipes` ✓，
再和 `data/scguns/recipe/**` 的 146 条比对 ✓，按蓝图分组打印 ✓。实测三份存档：

| 存档 | 已知配方总数 | 其中 scguns | 枪械工作台已解锁 | 身上带的蓝图 |
|---|---|---|---|---|
| `新的世界`（9/22 20:08） | 3113 | **0** | **0 / 146** | 无 |
| `新的世界 (1)`（9/23 22:20） | 0 | 0 | **0 / 146** | `antique_blueprint` ×1 |
| `新的世界 (2)`（9/23 22:23） | 0 | 0 | **0 / 146** | `copper_blueprint` ×1 |

⇒ 玩家 22:16 那次启动用的是 **22:05 构建的 jar** ✓（文件时间实测 ✓），
而 22:20 / 22:23 两次保存时**一条 scguns 配方都没解锁** ✗ —— 即使当时身上就带着蓝图 ✓。
两条解锁路径当时都应该已经生效 ✓：

* 数据驱动的解锁进度（146 条 `inventory_changed` ✓，见 §39 ✓）；
* 登录时扫描背包发放（`awardBenchRecipesForHeldBlueprints`，§39.6 ✓）。

两条都没落地 ⇒ **那台机器上跑的很可能是不含这两处改动的构建** ✗（存档时间与 jar 构建时间的对应
关系我无法从磁盘进一步确认 ✓ —— 22:05 的 jar 里有没有这两处，需要装新 jar 后再测一次才能定论 ✓）。
**这一条我没有下结论** ✗：我不把"肯定是旧 jar"当成事实写 ✓（这正是本项目反复强调的：**证据到哪说到哪** ✓）。

**顺带**：既然存档里明明白白写着"玩家手里有铜蓝图、却 0 条解锁" ✓，`awardBenchRecipesForHeldBlueprints`
不该只扫 `inventory.items` ✓（主背包 36 格 ✓）而漏掉副手 ✓ —— 已顺手补上 `player.getOffhandItem()` ✓
（一行，零风险 ✓）。

## 42.4 我上一轮的错误（记下来）

§41 那一轮我把玩家说的"右上角解锁的配方都是原版工作台的图标"理解成"**进度图标**不对" ✗，
于是去改 `advancement` 的 `display.icon` ✓ —— 方向错了 ✗。玩家说的其实是
**`RecipeToast`（配方解锁提示）里画的那个站点图标** ✓（见 42.1 ✓）。
正确做法本该是**先问清楚"右上角那个图标出现在什么框里"** ✗，或者直接 `grep getToastSymbol` ✓
（一条 grep 就能定位 ✓）。教训与 §35.4 / §29.4 同一条：**玩家的观察是对的，我的归因要先找证据** ✓。

## 42.5 本轮新增的诊断（都是为了让下一轮不用猜）

1. **`/scguns recipebook`**（无权限要求 ✓，查自己 ✓；`/scguns recipebook <player>` 需要 OP ✓）
   —— 服务端真值 ✓：已解锁 / 总数 ✓、按蓝图分组的 `已解锁/总数` ✓、以及"带蓝图即解锁"的提示 ✓。
   实现放在**独立类** `init/ModRecipeBookCommand.java` ✓（第二次 `dispatcher.register` ✓ ——
   Brigadier 会把两个 `scguns` 根节点**合并** ✓，这样就不必去动 `ModCommands` 里那串深嵌套的 builder ✓，
   上一次正是在那里因为缩进/括号对不上而 edit 失败 ✓）。
2. **打开枪械工作台时打一行日志** ✓（`scguns-recipebook` 标签 ✓）：

   ```
   SCGUNS-RECIPEBOOK display type=SCGUNS_GUN_BENCH tabs=[SCGUNS_GUN_BENCH_SEARCH=11 SCGUNS_GUN_BENCH_MISC=11] unlocked=11
   ```

   —— 这是**客户端**真值 ✓，与 `/scguns recipebook` 的**服务端**真值对照 ✓，
   两边不一致就说明是同步/解锁链路的问题 ✓，一致则说明"少"是解锁数量本身 ✓。

## 42.6 验收状态

* `gradlew build` 成功 ✓（含新增 4 处：toast 图标 ✓ / 搜索页图标 ✓ / 副手扫描 ✓ / 诊断命令与日志 ✓）；
* 语言文件：新增 6 个键，**en_us 与 zh_cn 逐行各加一份** ✓（`audit_lang_keys.py` 的键集对齐是门禁 ✓）；
* **未验证** ✗：`getToastSymbol` 的实际画面 ✓（纯客户端 ✓，只能由玩家看 ✓）、
  `/scguns recipebook` 的聊天输出 ✓（需要真玩家 ✓ —— 命令本身由 `runServer` 启动即可确认无解析错误 ✓）。

## 42.7 玩家实测后**真正的根因**：`isIncomplete()` 把 137 条配方整条丢掉了（已修）

玩家给了两张截图 ✓：`/scguns recipebook` 显示 **146/146 全解锁（全绿）** ✓，
而配方书里**仍然只有 9 把枪** ✗ ⇒ 上一节"9 = 解锁数量"的解释**被实测否掉了** ✗
（这正是我上一节不肯下结论的那部分 ✓，好在留了诊断 ✓）。

### 证据链（三份独立数据，全部吻合到个位数）

1. **客户端日志**（新加的诊断行 ✓，玩家那一轮的 `latest.log` 实测 ✓）：

   ```
   22:38:53  SCGUNS-RECIPEBOOK display ... tabs=[..._SEARCH=5 ..._MISC=5] unlocked=10
   22:39:06  SCGUNS-RECIPEBOOK display ... tabs=[..._SEARCH=9 ..._MISC=9] unlocked=18
   ```
   ⇒ **客户端书里只有 9 条** ✓（SEARCH 是 MISC 的聚合页 ⇒ 同一批被数了两遍 ⇒ 真实是 9 ✓，
   已顺手把日志改成按**集合身份**去重 ✓）。
2. **服务端**：`/scguns recipebook` = **146/146** ✓ ⇒ 解锁链路完全正常 ✓（上一轮的登录扫描/进度都生效 ✓）。
3. **读原版源码**：`ClientRecipeBook.categorizeAndGroupRecipes` 的第一道门就是

   ```java
   if (!recipe.isSpecial() && !recipe.isIncomplete()) { ... }     // 不满足 ⇒ 连分类都不进
   ```
   而 `Recipe#isIncomplete()` 的**默认实现**是：
   ```java
   return nonnulllist.isEmpty() || nonnulllist.stream().anyMatch(Ingredient::hasNoItems);
   //                                   ^^^ 只要有【任意一个】空原料槽 ⇒ 判定为"配方不完整"
   ```
4. **数据侧复算**（`data/scguns/recipe/**` 直接统计 ✓）：146 条枪械工作台配方里，
   **十个槽位全部填满的正好 9 条** ✓✓ —— 名单是
   `big_bore / callwell / echoes_2 / gattaler / scratches / shellurker / terra_incognita / thunderhead / weevil` ✓，
   其余 **137 条**都留有空槽（枪只用得到其中几个模块 ✓）。

⇒ **玩家第一次报的"配方书界面只有 `scguns:big_bore` 和 `scguns:callwell` 的配方"就是这 9 条的前两条** ✓✓✓
—— 这个现象从第一版起就存在 ✓，一直被"解锁"这条线盖住了 ✓（两个 bug 叠在一起 ✓）。

### 修法：像原版 `ShapedRecipe` 那样忽略"故意留空"的槽位

`ShapedRecipe.java:88` 早就为重载图案里的空格做过同样的事 ✓：

```java
public boolean isIncomplete() {
    return nonnulllist.isEmpty()
        || nonnulllist.stream().filter(i -> !i.isEmpty()).anyMatch(Ingredient::hasNoItems);
}   //                                     ^^^ 只看【非空】的槽位
```

`GunBenchRecipe` 现在照抄这一条语义 ✓：空槽是**版式的一部分** ✓，不是数据缺失 ✓；
而"某个非空槽位的原料解析成了 0 物品"（例如标签写空 ✓）**仍然**算不完整 ✓ —— 与原版一致 ✓。

### 顺带

* `verify_installed_jar` 新增 **2 项**（`isIncomplete` 与 `hasNoItems` 出现在打包类里 ✓）；
* 这一条也说明：**配方书的"少"不一定是解锁问题** ✓ —— 先看 `isIncomplete/isSpecial` 这两道**在分类之前**的门 ✓，
  再谈解锁 ✓（顺序反了就会像我上一轮那样得出错误结论 ✓）。

## 42.8 玩家："工作台的蓝图的空位应该也要投影" —— 蓝图不是"配方原料"，所以 ghost 与自动放置都够不着它（已修）

### 根因：ghost 与自动放置**只**由 `getIngredients()` 驱动

原版那块代码一共三处，全部只看原料列表 ✓（都读过源码 ✓）：

```java
// RecipeBookComponent.java:545
public void setupGhostRecipe(RecipeHolder<?> recipe, List<Slot> slots) {
   this.ghostRecipe.addIngredient(Ingredient.of(resultItem), slots.get(0).x, slots.get(0).y);   // 输出
   this.placeRecipe(this.menu.getGridWidth(), this.menu.getGridHeight(),
                    this.menu.getResultSlotIndex(), recipe, recipe.value().getIngredients().iterator(), 0);
}

// RecipeBookComponent.java:554  ← ghost 画在哪
public void addItemToSlot(Ingredient item, int p_slot, int maxAmount, int x, int y) {
   if (!item.isEmpty()) { Slot slot = this.menu.slots.get(p_slot); this.ghostRecipe.addIngredient(item, slot.x, slot.y); }
}

// PlaceRecipe.java:9  ← 网格下标 → 菜单槽位
int k1 = 0;
for (int k = 0; k < height; k++) {
   if (k1 == outputSlot) k1++;                       // 跳过输出槽
   ... this.addItemToSlot(ingredients.next(), k1, ...); k1++;
}
```

而我们的蓝图是 `GunBenchRecipe` 的**独立字段**（`blueprint` ✓），**不在** `getIngredients()` 里 ✗，
再加上网格声明的是 `1 x 10` ⇒ 迭代器只有 10 个原料 ✓、`k1` 最多走到 **10** ✓
⇒ **第 11 槽（蓝图槽）永远拿不到 ghost，也永远不被自动放置** ✗✓ —— 正是玩家看到的现象 ✓。

（顺带解释了一个旧现象 ✓：`ServerPlaceRecipe` 用 `gridWidth*gridHeight + 1` 遍历槽位 ✓，
即 `10 + 1 = 11` ⇒ 它只管到 **菜单槽 0..10**（输出 + 十个模块）✓，蓝图槽同样漏在外面 ✓。）

### 修法：把蓝图当作**配方的第 11 个输入**

这是最贴合原版模型的做法 ✓ —— 蓝图本来就是工作台的一个真实输入 ✓：

| 改动 | 位置 |
|---|---|
| `getIngredients()` 返回**10 个模块 + 蓝图**（蓝图 index=10 ✓） | `GunBenchRecipe`（构造时拼好 `inputs` ✓，新增 `getModuleIngredients()` 保留 10 个模块 ✓） |
| 网格声明 `1 x 11`、`getSize() = 11` | `GunBenchMenu`（`GRID_SIZE + 1` ✓） |
| 上机网络格式**不变**（仍写 10 + 蓝图 ✓，改用 `getModuleIngredients()` ✓） | `GunBenchRecipe.Serializer#toNetwork` |

于是：index 10 → `k1 = 11` → **菜单槽 11 = 蓝图槽** ✓✓（`PlaceRecipe` 跳过输出槽的那一步是关键 ✓），
`ServerPlaceRecipe` 遍历 `11 + 1 = 12` 槽 ✓ = 工作站自己的槽数 ✓，**客户端 ghost 与服务端自动放置同时覆盖蓝图** ✓✓。

### 连带修掉的三处（都是"原料下标 ≠ 容器/菜单下标"）

1. **`GunBenchMenu.consumeIngredients`** ✗：它用 `container.getItem(i)` 配 `getIngredients().get(i)` ✓ ——
   容器里 **index 10 是输出槽、11 是蓝图** ✓，所以第 11 个原料会去扣**输出槽**的东西 ✗ ⇒
   改用 `getModuleIngredients()` ✓（`attemptAutoCrafting` 同理 ✓）；
2. **`GunBenchTransferInfo.getRecipeSlots`（JEI "+" 转移）有一处**移植以前就存在**的 off-by-one** ✗：
   它用**原料下标**当**菜单槽下标**（`container.getSlot(i)` ✓ 而 i=0 指的是**输出槽** ✗）
   ⇒ 转移目标整体错一格、且漏掉最后一个模块槽 ✗ ⇒ 改成 `MENU_SLOT_GRID_START + i` ✓ + 蓝图用 `MENU_SLOT_BLUEPRINT` ✓；
3. **`GunBenchCategory`（JEI 展示）**本来就手写了蓝图那一格 ✓（`slotX[10]` ✓），
   若继续用 `getIngredients()` 会把蓝图**加两次到同一格** ✗ ⇒ 改用 `getModuleIngredients()` ✓。

### 验证

* 新增 `tools/check_gun_bench_layout.py` ✓：**照抄** `PlaceRecipe.placeRecipe` 的算法（我读过的那 58 行 ✓），
  用 `GunBenchMenu`/`GunBenchRecipe` 里**真实的常量**跑一遍 ✓，并断言三条不变量 ✓（蓝图 index → 蓝图槽 ✓、
  第一个模块 → `MENU_SLOT_GRID_START` ✓、`gridHeight + 1 == 蓝图槽 + 1` ✓）。实跑输出 ✓：

  ```
  bench: 10 attachment slots, output at menu slot 0, blueprint at menu slot 11
  grid:  1 x 11 inputs, so ServerPlaceRecipe walks 11 + 1 = 12 slots
  ghost: ingredient index -> menu slot [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
  OK: a book click projects and fills every attachment slot and the blueprint slot
  ```

  ⚠️ **这条是"照着原版算法做的算术验证"** ✓，**不是**客户端实录 ✗ —— ghost 究竟画在哪，仍然只能由玩家看 ✓（如实说明 ✓）；
* `verify_installed_jar` 新增 **1 项**（`getModuleIngredients` 出现在打包类里 ⇒ 输入列表确实被拆成"网格 + 蓝图" ✓）：
  上一个 jar **97/98** ✓、新 jar **98/98** ✓；
* build ✓、`audit_dist_safety` / `audit_unsafe_casts` / `audit_override_drift` / `audit_subscribers` 0 失败 ✓。

# 43. 玩家："四种炮塔可以通过 `scguns:turret_platform` 解锁配方书"

玩家指出四座炮塔（`auto_turret` / `basic_turret` / `shotgun_turret` / `sniper_turret` ✓）在配方书里
应当由 **`scguns:turret_platform`** 解锁 ✓。查完发现：**这四条是 146 条里唯一"没有蓝图"的** ✓，
而它们当时挂的是 **`minecraft:tick` 兜底** ✗（§39 生成器写的 ✓）—— 也就是说
**玩家一进世界就白得四条炮塔配方** ✗，平台本身在解锁上毫无作用 ✗（与玩家直觉冲突 ✓）。

## 43.1 关键事实：平台本来就是这四条的"钥匙物品"

读配方数据（`data/scguns/recipe/guns/*_turret_from_gun_bench.json` ✓）：
四条配方**都把 `scguns:turret_platform` 放在 `gun_grip` 这个原料位** ✓（作者当年的写法 ✓），
即**平台是它们的必需材料** ✓，语义上就等于"炮塔的蓝图" ✓✓ —— 所以玩家要求的是对的 ✓：

```
auto_turret_from_gun_bench:   gun_grip = scguns:turret_platform
basic_turret_from_gun_bench:  gun_grip = scguns:turret_platform
shotgun_turret_from_gun_bench:gun_grip = scguns:turret_platform
sniper_turret_from_gun_bench: gun_grip = scguns:turret_platform
```

（顺带核实：`turret_platform` 在 Java 侧**只出现在创造模式物品栏** ✓，没有任何代码引用 ✓ ⇒
它纯粹是数据里的钥匙物品 ✓。）

## 43.2 改动：把"钥匙物品"的定义收成一处，三处共用

原来"什么解锁这条配方"这件事在**三个地方各写一遍**（生成器 ✓、登录扫描 ✓、诊断命令 ✓），
而且互相不一致 ✗：登录扫描**直接跳过没有蓝图的配方** ✗ ⇒ 那四条只能靠 tick 兜底 ✓。
现在新增 `common/recipe/GunBenchUnlockKeys` ✓：

```java
/** 钥匙物品：有蓝图就用蓝图，没有蓝图的（＝四座炮塔）用炮塔平台。 */
public static ItemStack keyFor(GunBenchRecipe recipe) { ... }
public static boolean matches(GunBenchRecipe recipe, ItemStack carried) { ... }
```

* **数据**（`tools/gen_gun_bench_unlocks.py` 重新生成 ✓）：四条炮塔的解锁条件由
  `minecraft:tick` 改成 `minecraft:inventory_changed` + `items: [scguns:turret_platform]` ✓
  —— 与其余 142 条"持有蓝图即解锁"的形状**完全一致** ✓。脚本现在还会打印**按钥匙分组的条数** ✓
  （实测：`treated_brass 21 / diamond_steel 21 / iron 18 / antique 12 / wrecker 12 / copper 11 /
  frontier 10 / scorched 10 / ocean 7 / end 6 / piglin 6 / deep_dark 4 / exo_suit 4 /
  **turret_platform 4**` ✓，合计 146 ✓）。`git diff` 实测**只动了这 4 个文件** ✓；
* **登录扫描**（`GunProgressionEventHandler`）：不再跳过无蓝图配方 ✓，改用 `GunBenchUnlockKeys.matches` ✓
  ⇒ 已经带着平台的老玩家**一登录就补上** ✓（与蓝图的行为一致 ✓）；
* **诊断命令**（`/scguns recipebook`）：分组键也改成"钥匙物品" ✓ ⇒ 那四条现在显示在
  **炮塔平台**这一行 ✓，而不是以前含混的"无需蓝图" ✓（该语言键已无用，两个语言文件里一并删掉 ✓）。

## 43.3 验收

* 新增 2 项打包检查 ✓（**不依赖截图** ✓）：
  1. `data/scguns/advancement/recipes/**` 里**没有任何**解锁进度带 `minecraft:tick` ✓
     （实测 146 条 ✓）—— 也就是"没有任何配方会自己解锁" ✓；
  2. 四个炮塔进度**都存在且都引用 `scguns:turret_platform`** ✓。
  自测 ✓：上一个 jar **98/100（两项全 FAIL ✓）**，新 jar **100/100** ✓；
* build ✓、`runServer` 到 `Done` 且 0 ERROR/FATAL ✓、进度/配方零解析错误 ✓；
* **未验证** ✗：游戏内"拿到平台才解锁"这条链路 ✓ —— 需要玩家在**没有**这四条解锁的状态下试 ✓。
  注意：**解锁是存在玩家存档里的** ✓，所以你现有存档里那四条仍然是已解锁状态 ✓，
  想看新逻辑要么开新世界 ✓，要么先 `/recipe take @s scguns:guns/auto_turret_from_gun_bench`
  （四条各来一次 ✓）再把平台丢出背包测试 ✓。

# 44. 玩家："四个炮塔单出个按钮，外骨骼蓝图的四种护甲也单独出个按钮"

配方书原来只有两个页签 ✓（搜索页 + 一个把 146 条全装进去的通用页 ✓）—— 找炮塔或外骨骼得翻页 ✓。
现在按玩家要求拆出两个独立页签 ✓：**炮塔** ✓、**外骨骼** ✓，其余 138 条仍在通用页 ✓。

## 44.1 分类规则：按"产出什么"分，而不是按配方 id 或文件目录

新增 `common/recipe/GunBenchBookTabs` ✓（**common** 包 ✓、不含任何客户端类型 ✓）：

```java
public enum Tab { GUNS, TURRETS, EXO_SUIT }

public static Tab of(GunBenchRecipe recipe) {
   Item item = recipe.getResultItem(RegistryAccess.EMPTY).getItem();
   if (item instanceof BlockItem b && isTurretBlock(b)) return Tab.TURRETS;
   if (item instanceof ExoSuitItem)               return Tab.EXO_SUIT;
   return Tab.GUNS;
}
```

* **外骨骼**用类型判断 ✓（`top.ribs.scguns.item.animated.ExoSuitItem` ✓，四条护甲都是它 ✓）；
* **炮塔**无法用类型判断 ✗ —— 四个炮塔方块类**都直接继承 `BaseEntityBlock`** ✓（实测：
  `AutoTurretBlock` / `BasicTurretBlock` / `ShotgunTurretBlock` / `SniperTurretBlock` ✓，
  **没有共同基类** ✓）⇒ 只能按**方块身份**列四家 ✓；这一点在注释里写明 ✓，
  并由打包检查兜住"炮塔落错页"这种漂移 ✓；
* 放在 **common** 是关键 ✓：客户端用它生成 `RecipeBookCategories` ✓，而
  `/scguns recipebook` 用**同一份代码**在服务端汇报分组 ✓ ⇒ 两边不可能各说各话 ✓。

## 44.2 客户端接线（第 2、3 个枚举扩展分类）

按 §39.5 的教训 ✓，新增分类必须同时改**两处** ✓：

1. `META-INF/enumextensions.json` 增加
   `SCGUNS_GUN_BENCH_TURRET`（图标＝`ModBlocks.BASIC_TURRET` ✓）与
   `SCGUNS_GUN_BENCH_EXO_SUIT`（图标＝`ModItems.EXO_SUIT_CHESTPLATE` ✓，与原版"装备页用装备代表"一致 ✓），
   参数类仍是 `ScgunsRecipeBookEnumParameters#PROXY_TURRET` / `#PROXY_EXO_SUIT` ✓；
2. `ScgunsRecipeBookCategories` 用 `valueOf` 取新常量 ✓（取不到时**整体降级** ✓，绝不半残 ✓）；
3. `GunBenchRecipeBookCategories` 注册 `[搜索, 通用, 炮塔, 外骨骼]` ✓（搜索页聚合后三个 ✓），
   分类器 `registerRecipeCategoryFinder` 改成按 `GunBenchBookTabs.of(...)` 分流 ✓
   （两个新页签若取不到 ⇒ 退回通用页 ✓，**不会**因为多一个页签就整本书不显示 ✓）；
4. 启动日志现在打印四个分类各自是否解析成功 ✓：
   `SCGUNS-RECIPEBOOK type=SCGUNS_GUN_BENCH tabs=[search=ok misc=ok turret=ok exo_suit=ok] book=enabled` ✓。

## 44.3 实测（临时探针，已删除）

页签是纯客户端 ✓，但**分类本身是 common 代码** ✓ ⇒ 用专用服务器就能量出"客户端页签里会有什么" ✓。
临时加了一个 `ServerStartedEvent` 探针（跑完已删除 ✓，并由打包检查确保不再进 jar ✓），实测 ✓：

```
[SCGUNS-TABS] GUNS     = 138 -> guns/gauss_rifle… guns/bruiser… （其余全部）
[SCGUNS-TABS] TURRETS  =   4 -> shotgun_turret / sniper_turret / basic_turret / auto_turret
[SCGUNS-TABS] EXO_SUIT =   4 -> exo_suit_helmet / leggings / boots / chestplate
```

⇒ 138 + 4 + 4 = **146** ✓，三个页签的分组**完全正确** ✓（这是用真的分类代码跑出来的 ✓，不是复算数据 ✓）。

## 44.4 验收

* `verify_installed_jar` 新增 **9 项** ✓：两个新分类在 `enumextensions.json` 里 ✓、
  客户端确实按名字解析它们 ✓、`/scguns recipebook` 的分页行语言键在包里 ✓、
  炮塔页 4 条 / 外骨骼页 4 条（从打包的配方数据复算 ✓）、**临时探针不在 jar 里** ✓
  以及原有的两项 ✓。自测 ✓：上一个 jar **105/109（4 项 FAIL ✓）**、新 jar **109/109** ✓；
* build ✓（`--rerun-tasks` 强制重编 ✓，`installed jar is byte-identical to build/libs/...` ✓）、
  10 项相关审计 0 失败 ✓、语言文件 en_us / zh_cn 各 **1806** 条全对齐 ✓；
* **未验证** ✗：页签**画出来的样子**（图标、顺序、点击切换 ✓）—— 纯客户端 ✓，需要玩家看 ✓。
  不过有一个日志层证据可用 ✓：打开工作台时若四个分类都解析成功 ✓，会打出上面那行 `tabs=[...ok...]` ✓；
  若新分类没生效 ✓，日志会直接显示 `turret=missing` ✓（而不是静默变成"少两个页签"✓）。

# 45. 玩家实测日志里的三个真 bug（一个直接崩游戏）+ 我自己那条误导性日志

玩家贴了 `latest.log` ✓，里面除了页签那行 ✓，还藏着三条互不相关的错误 ✓ —— **全部是移植引入的** ✓，
全部已修 **并在专用服务器上实测复现/验证** ✓（脚本：`tools/rcon_verify_section45.py` ✓）。

## 45.1 【崩溃】硫磺中毒 + 火焰伤害 ⇒ `ClassCastException`（已修，已实测）

玩家的崩溃报告 ✓：

```
java.lang.ClassCastException: class DeferredHolder cannot be cast to class
    top.ribs.scguns.effect.SulfurPoisoningEffect
  at SulfurPoisoningEffect.getFireDamageMultiplier(SulfurPoisoningEffect.java:122)
  at SulfurPoisoningEventHandler.onLivingHurt(SulfurPoisoningEventHandler.java:28)
  ...
Caused by: Ticking entity / zombie  →  MinecraftServer 崩溃退出
```

**根因**：1.21.1 的 `MobEffectInstance#getEffect()` 返回的是 **`Holder<MobEffect>`** ✓（1.20.1 返回 `MobEffect` ✗），
而移植照 0.5.5 原样写了 `(SulfurPoisoningEffect) effect.getEffect()` ✗ ⇒ 强转的是 **Holder** ⇒ 抛异常 ✓。
这个异常发生在 **受伤事件**里 ✓ ⇒ 被 `MinecraftServer` 当成"Ticking entity"⇒ **整个游戏崩掉** ✓
（触发条件很普通：**身上有硫磺中毒的实体被火焰伤害打到** ✓）。

**修法**（`SulfurPoisoningEffect` ✓）：`effect.getEffect().value() instanceof SulfurPoisoningEffect` ✓
—— 用 `value()` 取真实效果 ✓，再加 `instanceof` 兜底 ✓（万一 holder 里是别的效果也只是返回 1.0 而不是崩 ✓）。

**实测** ✓（`tools/rcon_verify_section45.py` ✓）：先确认了处理器的触发条件 ✓
（`hasFireVulnerability` ✓ + 伤害类型必须是 `IN_FIRE`/`ON_FIRE`/`LAVA`/`HOT_FLOOR` ✓ —— 所以测试用 `in_fire` ✓
正好走进第 122 行 ✓）：

```
> effect give @e[type=minecraft:zombie,limit=1] scguns:sulfur_poisoning 200 0
Applied effect Sulfur Poisoning to Zombie
> damage @e[type=minecraft:zombie,limit=1] 3 minecraft:in_fire
Applied 3.0 damage to Zombie
sulfur poisoning damage: no ClassCastException        ← 修好后
```

> 同类写法全仓库只此一处 ✓（grep `getEffect()` 实测 ✓；`FuelAmmoItem`/`HealingBandageItem` 早就是
> `.value()` ✓ —— 也就是说这是**移植时漏改的孤例** ✓）。

## 45.2 【存档丢东西】枪械工作台每次保存都抛异常 ⇒ 里面的东西**从未被保存**（已修，已实测）

```
ERROR [minecraft/LevelChunk]: A BlockEntity type ...GunBenchBlockEntity has thrown an exception
    trying to write state. It will not persist
java.lang.IllegalStateException: Cannot encode empty ItemStack
  at ItemStack.save(ItemStack.java:400)
  at GunBenchBlockEntity.saveAdditional(GunBenchBlockEntity.java:59)
```

**根因**：`GunBenchBlockEntity.saveAdditional` 对**全部 12 个槽位无条件**调用 `ItemStack.save` ✗，
而 1.21 的 `save` 对**空栈**会抛 `Cannot encode empty ItemStack` ✗ —— 工作台**必然有空槽** ✓
⇒ **每一次区块保存都抛异常** ✗ ⇒ 实体被丢弃 ⇒ **工作台里的东西存不下来** ✓（而且只在日志里静默丢 ✓）。

**修法**：空槽直接 `continue` ✓（读的那侧本来就把"没有这个 tag"当空 ✓，所以跳过即可 ✓）。
**证据**：模组自己的 `SupplyScampEntity.addAdditionalSaveData`（:735 ✓）**早就有这个空栈判断** ✓
—— 说明这是移植工作台时**漏掉的一道守卫** ✓，不是 0.5.5 的行为 ✓。

**实测** ✓：`setblock` 放一个工作台 → `data merge block ... {Item0:{id:"minecraft:stone",count:1}}`
→ `save-all flush` ✓：

```
bench save: no empty-stack error        ← 修好后（修前就是上面那段 ERROR）
```

**顺带**：这解释了玩家之前可能遇到的"工作台里的东西消失" ✓ —— 若曾发生 ✓，根因就是这条 ✓。

## 45.3 【画不出来 + 刷屏】枪械冷却条用了 1.21.1 已删除的贴图，而且从不提交（已修）

```
WARN [TextureManager]: Failed to load texture: minecraft:textures/gui/icons.png
java.io.FileNotFoundException: minecraft:textures/gui/icons.png
  at GunRenderingHandler.renderCooldownIndicator(GunRenderingHandler.java:221)
```

两个问题叠在一起 ✓（都是本项目踩过的老坑 ✓）：

1. **`textures/gui/icons.png` 在 1.21.1 里不存在** ✗（1.20.2 起 GUI 图标改成图谱 sprite ✓）
   ⇒ 每帧/每 tick 抛 `FileNotFoundException` 刷日志 ✓，冷却条**永远画不出来** ✗；
2. 它还**自建了一个 `GuiGraphics` 却从不 `flush()`** ✗ —— 与 §28.5 的闪光弹遮罩**同一个坑** ✓
   （`fill`/`blit` 只是写进 BufferSource ✓，没人提交就等于没画 ✓）；
3. 而且它被 **`handleClientTick`（每 tick ✓）和 `handleRenderTick`（每帧 ✓）各调一次** ✗
   ⇒ 刷屏更严重 ✓。

**修法**：删掉该方法与两处调用 ✓，改成 **GUI 图层** `client/handler/GunCooldownOverlay` ✓
（`RegisterGuiLayersEvent.registerAboveAll` ✓ —— 与 §28.5.3 修闪光弹遮罩的同一套做法 ✓），
贴图改用**原版现成的 `hud/crosshair_attack_indicator_background` / `_progress`** ✓
（原版画的就是同样的 16×4 条 ✓，用九参 `blitSprite` 支持按进度裁宽 ✓，位置与缩放沿用 0.5.5 ✓）。
顺带删掉已死的 `GUI_ICONS_LOCATION` 常量与 4 个不再使用的 import ✓（`git diff` 实测 4 增 41 删 ✓）。

**验证** ✓：新增 2 项打包检查 —— `GunRenderingHandler` **不再引用** `textures/gui/icons.png` ✓
且 `GunCooldownOverlay` 引用了原版 HUD sprite ✓：上一个 jar **109/111（两项 FAIL ✓）**、新 jar **111/111** ✓。
⚠️ **画面本身仍需玩家确认** ✗（纯客户端 ✓）。

## 45.4 我自己的错：那条页签日志**误导了玩家**（已修）

玩家日志里：

```
tabs=[SCGUNS_GUN_BENCH_SEARCH=146 SCGUNS_GUN_BENCH_MISC=0 SCGUNS_GUN_BENCH_TURRET=0 SCGUNS_GUN_BENCH_EXO_SUIT=0]
```

看起来"全都跑到搜索页、另外三页是空的" ✗ —— **但这是我这行日志写错了** ✗，不是页签坏了 ✓。
原因：`ClientRecipeBook` 用**聚合页的成员**去填聚合页 ✓ ⇒ `SEARCH` 里装的就是 MISC+炮塔+外骨骼的**全部集合** ✓；
我按 `all()` 的顺序（搜索页在前 ✓）逐个累加并**跨页去重** ✗ ⇒ 搜索页先把 146 个集合全数掉 ✓，
后面三个页签就都变成 0 ✗。

**反过来的证据**：`SEARCH=146` 恰恰说明三个分组页**一共装着 146 条** ✓（聚合页就是按成员 flatMap 出来的 ✓）
⇒ **页签是正常的** ✓。

**修法**：日志**跳过聚合页** ✓（`GunBenchRecipeBookCategories.isAggregate(...)` ✓），只报三个真实分组 ✓。
下一次打开工作台应当看到 `MISC=138 TURRET=4 EXO_SUIT=4` ✓。

**教训**（写进本节）：诊断输出本身也要有"它测的到底是什么"的断言 ✓ —— 我这次是拿一个**会自我抵消**的统计
去汇报 ✓，差点让玩家以为页签没做对 ✓。同类错误本节之前的 §42.7 也犯过一次（"9 = 解锁数量"✗）✓。

# 46. 女仆兼容：空闲补弹"没有敌人就立刻换弹" → 改成**无敌人 5 秒后**（玩家指出这是上游原有问题）

玩家原话：**"空闲换弹在没有敌人后立即触发，应该在没有敌人的五秒后触发换弹，这个是 mod 原先就有的问题"** ✓
—— 也就是**上游（玩家自己 1.20.1 的附属）就有的 bug** ✓，不是这次移植改坏的 ✓（本节照做修复 ✓）。

## 46.1 为什么原来是"立刻"

`MaidSC2GunIdleReloadTask` 的内存要求是 `ATTACK_TARGET = VALUE_ABSENT` ✓
—— 也就是说**这个行为只要在运行，就代表"当前没有敌人"** ✓；而它的 `tick()` 里一旦
`shouldIdleReload(...)` 成立就**直接 `beginReload`** ✗ ⇒ **目标一消失就开始换弹** ✓
（打死后女仆转身就拉栓 ✓，看起来像"人还没倒下她就在装弹" ✓）。

## 46.2 修法：数"本行为自己运行了多少刻"，就等于"敌人离开多久"

```java
private int idleTicks = 0;                       // 本行为本轮运行的刻数 = 无敌人时长

// tick(): 未在换弹时
idleTicks++;
if (!shouldIdleReload(maid, gun)) return;
if (idleTicks < SCG2TLMConfig.IDLE_RELOAD_DELAY.get()) return;   // ← 新增的等待
if (checkCooldown > 0) { ... }

// stop(): 敌人出现 / 坐下 / 换枪等都会走到这里
idleTicks = 0;                                   // ← 重新计时
```

三点值得说明 ✓：

* **为什么用"运行刻数"而不是时间戳** ✓：本行为只在没有目标时运行 ✓ ⇒ 它运行了多少刻**就是**敌人离开了多久 ✓，
  不需要另外维护"最后见到敌人的时刻"✓，也不会出现"时间戳被别的路径改写"的漂移 ✓；
* **敌人回来怎么办** ✓：任何战斗行为优先级都高于它 ✓ ⇒ 抢占 ⇒ 立刻 `stop()` ⇒ `idleTicks = 0` ✓
  ⇒ **等待重新开始** ✓（"无敌人**连续** 5 秒"✓，不是累计 ✓）；
* **换弹中途被打断** ✓：`canStillUse` 里"正在换弹就继续"这条**仍然优先于等待** ✓
  ⇒ 已在进行的换弹不会被这个等待卡住 ✓（换弹进度与 §36 的共享状态机不变 ✓）。

配置项新增在 `reload` 段 ✓（`SCG2TLMConfig` ✓）：

```toml
# How long a maid must have NO enemy before the idle reload above starts, in ticks (20 = 1 second).
idle_reload_delay_ticks = 100      # 0..1200，默认 100 = 5 秒；0 = 恢复上游"立刻换弹"的行为
```

## 46.3 验收

* 女仆兼容单独编译 ✓（`tools/check_maid_compat.py`：**41 个源文件、0 error** ✓）、
  mixin 引用检查 ✓（34 处、0 问题 ✓）、宿主零 TLM 引用 ✓；
* 主工程 `gradlew build` ✓（兼容以嵌套 jar 打包 ✓）；
* `verify_installed_jar` 新增 **2 项**（**读嵌套 jar 内部** ✓：配置键 `idle_reload_delay_ticks` ✓
  与行为类里对 `IDLE_RELOAD_DELAY` 的引用 ✓）—— 上一个 jar **111/113（两项 FAIL ✓）**、新 jar **113/113** ✓；
* **未验证** ✗：**实际计时**（需要"女仆拿枪 + 打完最后一只敌人"这种局面 ✓，专用服务器上没有可指挥的女仆 ✓）。
  请玩家看两点 ✓：① 打完最后一只敌人后应当**约 5 秒**才开始换弹 ✓（而不是立刻 ✓）；
  ② 这 5 秒内若又冒出敌人 ✓，等待应当**重新计时** ✓（不会一有空档就换弹 ✓）。
  想改时长就调 `config/scg2_maid_compat-common.toml` 的 `idle_reload_delay_ticks` ✓。

> 说明：这条是**上游 bug** ✓，本移植只是把它一起修了 ✓。若要把修复同步回 1.20.1 的源码 ✓，
> 改动与这里完全一致 ✓（`MaidSC2GunIdleReloadTask` 加一个计数 + 一个配置项 ✓）。

# 47. 女仆兼容的 Cloth Config 配置界面**补回来了**（玩家要求）

§36.6 里记了一处**有意的取舍** ✓：移植时把 `SCG2TLMClothConfig` / `AddClothConfigEventListener`
**整个删掉** ✗ —— 理由是当时 `me.shedaniel.clothconfig2` 既不在依赖里、也不在玩家实例里 ✗
（配置项本身照常生效 ✓，只是不能在游戏里点着改 ✓，只能编辑 `config/scg2_maid_compat-common.toml` ✓）。

现在玩家的 mod 列表里**已经有 Cloth Config 了** ✓（`cloth-config-15.0.140-neoforge.jar` ✓，
就在他贴的崩溃报告 mod list 里 ✓）⇒ 玩家要求补回来 ✓，已完成 ✓。

## 47.1 用 TLM 自己的钩子，比当年那个 Forge 专有类更好

上游 1.20.1 是用 Forge 的 `ModLoadingContext.registerExtensionPoint(ConfigScreenHandler...)` 注册**独立配置屏** ✗
—— 那个类在 NeoForge 上**不存在** ✓，也正是移植脚本把它删掉的原因（`port_rewrite_maid.py:56` 记着
`net.minecraftforge.client.ConfigScreenHandler → None, Cloth Config screen, dropped` ✓）。

但上游其实**还有第二条路** ✓：TLM 自己的事件 `AddClothConfigEvent` ✓ ——
TLM 会把**它的**配置屏的 `ConfigBuilder` / `ConfigEntryBuilder` 交给订阅者 ✓，
让附属把条目**加进 TLM 的设置界面**里 ✓。这比单独注册一个屏更好 ✓（玩家的入口不变 ✓）。

**实测确认这条路还在** ✓（`javap` ✓）：

```
public class com.github.tartaricacid.touhoulittlemaid.api.event.client.AddClothConfigEvent
        extends net.neoforged.bus.api.Event {
    public me.shedaniel.clothconfig2.api.ConfigBuilder getRoot();
    public me.shedaniel.clothconfig2.api.ConfigEntryBuilder getEntryBuilder();
}
```

以及它**发在哪个总线** ✓（`javap -c` 反汇编 TLM 的 `compat.cloth.MenuIntegration` ✓）：

```
65: getstatic  Field net/neoforged/neoforge/common/NeoForge.EVENT_BUS:Lnet/neoforged/bus/api/IEventBus;
77: invokeinterface IEventBus.post:(Lnet/neoforged/bus/api/Event;)...
```

⇒ 用 `NeoForge.EVENT_BUS.addListener(...)` 即可 ✓ —— 正是本兼容**既有的注册风格** ✓
（§36.4 定下的规矩：不用 `@EventBusSubscriber` ✓，避免注解扫描在不该加载时把类加载起来 ✓）。

顺带核实 ✓：TLM **没有** shade Cloth Config（jar 里 0 个 `clothconfig2` 条目 ✓），
它的 `neoforge.mods.toml` 也**没有**声明 `cloth_config` 依赖 ✓ ⇒ "装了 TLM 就一定有 Cloth Config"**不成立** ✗
⇒ 这道守卫必须由我们自己做 ✓（详见 47.3 ✓）。

## 47.2 改动

| 文件 | 内容 |
|---|---|
| `client/SCG2TLMClothConfig.java` | 从 1.20.1 上游**原样**取回 ✓（270 行、覆盖 112 处配置项 ✓），只改了 **2 处** Forge 引用 ✓：`ForgeRegistries.SOUND_EVENTS.containsKey` → `BuiltInRegistries.SOUND_EVENT.containsKey` ✓（音效 id 校验 ✓） |
| `client/SCG2TLMClothConfigListener.java` | 新增 ✓：把事件转给上面的屏 ✓；**没有任何注解** ✓，只在 TLM+Cloth 都在时才被引用到 ✓ |
| `ExampleMod.java` | 客户端分支里加守卫注册 ✓：`if (ModList.get().isLoaded("cloth_config")) NeoForge.EVENT_BUS.addListener(SCG2TLMClothConfigListener::onAddClothConfig);` ✓，否则打一行 INFO 说明"配置仍在 toml 里" ✓；新增常量 `CLOTH_CONFIG_MODID` ✓ |
| `neoforge.mods.toml`（兼容） | 新增 **optional / side=CLIENT** 的 `cloth_config` 依赖 ✓（**不能**写成 required ✗ —— 没装 Cloth Config 的玩家会直接卡在缺依赖界面 ✓） |
| `maid-compat/libs/` | 放入 `cloth-config-15.0.140-neoforge.jar` ✓ 供编译（`compileOnly fileTree('libs')` 本来就覆盖整个 libs ✓，无需改 build.gradle ✓；与既有的 TLM jar 同样处理 ✓，两者都被 git 跟踪 ✓） |

**顺带补齐两个漏掉的配置项** ✓：上游那份屏写于 1.20.1，而本移植后来加了两个选项 ✗ ——
`FOLLOW_LEASH_RADIUS`（跟随牵引半径 ✓）与今天刚加的 `IDLE_RELOAD_DELAY`（空闲补弹延迟 ✓）
⇒ 已补上条目 ✓（位置分别在"目标与跟丢"与"换弹"分类里 ✓）。
新增工具 `tools/check_maid_config_screen.py` ✓ 专门盯这件事 ✓（实测：**39 个配置项 / 39 个都被界面覆盖 / 0 遗漏** ✓）。

## 47.3 三道守卫（缺一不可）

1. **TLM 守卫**（既有 ✓）：没装 TLM 时 `ExampleMod` 直接 return ✓ ⇒ 连 `AddClothConfigEvent` 这个类型都不会被解析 ✓；
2. **Cloth Config 守卫**（本次新增 ✓）：没装 Cloth Config 时**不注册监听器** ✓，
   `SCG2TLMClothConfig` / `...Listener` 两个类**都不会被加载** ✓（它们的方法签名直接引用 Cloth Config 类型 ✓
   ⇒ 一旦加载就是 `NoClassDefFoundError` ✓）；
3. **依赖声明 optional** ✓：写成 required 会让没装 Cloth Config 的玩家**开不了游戏** ✗。

## 47.4 验收

* 女仆兼容单独编译 ✓（`tools/check_maid_compat.py`：**43 个源文件、0 error** ✓ ——
  也就是说取回的 270 行屏**直接**就能对着 Cloth Config 15 与我们的配置编译 ✓，无需改动逻辑 ✓）；
* 配置项覆盖 ✓：`tools/check_maid_config_screen.py` ⇒ **0 遗漏** ✓、且没有引用已不存在的选项 ✓；
* mixin 检查 ✓（34 处、0 问题 ✓）、宿主零 TLM 引用 ✓、本兼容 0 阻塞注册 ✓；
* 主工程 `gradlew build` ✓（兼容仍以嵌套 jar 打包 ✓）；
* `verify_installed_jar` 新增 **2 项**（**读嵌套 jar 内部** ✓：两个 Cloth 类都在 ✓、
  且嵌套 jar 的 `neoforge.mods.toml` 里 `cloth_config` 是 optional ✓）—— 上一个 jar **113/115（两项 FAIL ✓）**、新 jar **115/115** ✓；
* **未验证** ✗：**界面本身**（纯客户端 ✓，而且需要 TLM + Cloth Config 同时存在 ✓）。
  请玩家看一处即可 ✓：打开 TLM 的设置界面（女仆的配置屏 ✓）⇒ 应当多出一个 **"赤焦枪械：女仆兼容"** 分类 ✓，
  里面是战斗 / 换弹 / 索敌范围 / 目标与跟丢 / 联动 / 盾牌 等子分类 ✓；改完点保存即写入 toml ✓。

# 48. **撤回 §45.3**：冷却指示器在 0.5.5 里根本不会显示 —— 我"修好"了一个本来就该是死的功能

玩家原话：**"枪械开火多出来一个冷却指示器？这个可以去掉吗？"** ✓ 以及
**"实际上在 0.5.5 原版内不显示这个东西"** ✓ —— 玩家是对的 ✓，我 §45.3 做错了 ✗。

## 48.1 我错在哪

§45.3 里我看到 `renderCooldownIndicator` 有两个毛病（用了 1.21.1 已删除的 `textures/gui/icons.png` ✗、
自建 `GuiGraphics` 从不 flush ✗），就把它当成"移植漏修的 bug" ✗，**改成 GUI 图层 + 原版 sprite** ✓
—— 结果让一个**在 0.5.5 里从未显示过**的功能**显示了出来** ✗✗。这不是修 bug ✓，而是**引入了偏离** ✗。

## 48.2 实测证据（三条，独立）

1. **玩家的实机记忆** ✓：0.5.5 里不显示 ✓（本项目一贯的规矩：玩家的观察优先于我的推断 ✓）；
2. **直接反汇编真正的 0.5.5 jar** ✓（`D:\MCJAVA\.minecraft\versions\1.20.1-Forge_47.4.21\mods\ScorchedGuns-0.5.5-1.20.1.jar` ✓）：

   ```
   javap -p -c ... top.ribs.scguns.client.handler.GunRenderingHandler | grep flush
   → 0 处
   ```

   整个类里**没有任何 `flush()` 调用** ✓ —— 而它画冷却条用的是
   `new GuiGraphics(mc, mc.renderBuffers().bufferSource())` ✓：
   `blit` 只把顶点写进 BufferSource ✓，**必须有人提交才会显示** ✓ ——
   这一帧里没有任何人提交这个"野" `GuiGraphics` ✓ ⇒ **画了等于没画** ✓✓
   （与 §28.5 闪光弹遮罩**同一个坑** ✓ —— 那次是真的坏在大版本升级上 ✓，这次 0.5.5 就已经是坏的 ✓）；
3. **贴图不是 1.20.1 的原因** ✓：`textures/gui/icons.png` 在 1.20.1 **存在** ✓（1.20.2 才改成图谱 sprite ✓）
   ⇒ 当年不显示**只**因为没提交 ✓，不是因为贴图 ✗。

> 附注：参考移植（SC2 **1.5**，1.21）里这个指示器是**能显示的** ✓（它改用了 `blitSprite` ✓）
> —— 但我们的基准是 **0.5.5** ✓，所以这边保持**不显示** ✓。

## 48.3 改动（删掉，而不是"修好"）

* 删除 `client/handler/GunCooldownOverlay`（§45.3 新建的那个 ✓）；
* 删除 `GunRenderingHandler` 里的 `handleRenderTick` 与其 `RenderFrameEvent.Post` 监听 ✓
  —— 它的**唯一职责**就是这个指示器 ✓（其余守卫条件也随之成为死代码 ✓），
  连带清掉 `CameraType` / `RenderFrameEvent` / `GrenadeItem` 三个已无引用的 import ✓；
* `Config.cooldownIndicator` 的配置键**保留** ✓（0.5.5 就有这个键 ✓，删掉会破坏既有配置文件 ✓），
  但注释改成如实说明：**这个键在 0.5.5 里也是无效的** ✓（那边同样画不出来 ✓），本移植同样不绘制 ✓；
* 玩家实例的 `config/scguns-client.toml` 里我已经把它设成 `false` ✓（现在其实无所谓了 ✓）。

## 48.4 验收与教训

* `verify_installed_jar`：把 §45.3 那两项**换掉** ✓ —— 现在是
  **"冷却指示器不再随包发布"** ✓（`GunCooldownOverlay.class` 必须不存在 ✓）与
  **"不再引用已删除的 icons 贴图"** ✓（防止那条刷屏路径回来 ✓）；
  自测 ✓：带着 overlay 的那个 jar **114/115（新检查 FAIL ✓）**、新 jar **115/115** ✓；
* `gradlew build` ✓；其余门禁不变 ✓；
* **教训（写进本节）**：**"坏掉的功能"不一定是 bug** ✓ —— 上游可能本来就坏着 ✓，
  而"修好它"会**改变玩家看到的行为** ✗。以后遇到"这段代码显然不工作"的情况 ✓，
  先问一句"**0.5.5 里它是工作的吗**" ✓（这次一条 `javap | grep flush` 就能回答 ✓），
  再决定是修还是保持原样 ✓。这与 §42.7 / §45.4 是同一类错误：**没先验证基准就动手** ✗。

# 49. 【流程】游戏运行中替换 mod jar 会让客户端崩溃 —— **是我干的**，已加守卫

玩家报"游戏又崩溃了" ✓，崩溃报告是：

```
java.lang.NoClassDefFoundError: top/ribs/scguns/event/ArmorRenderHandler$1
  at ArmorRenderHandler.hideSecondLayerForSlot(ArmorRenderHandler.java:92)   ← switch (EquipmentSlot) 的合成类
Caused by: java.lang.ClassNotFoundException: top.ribs.scguns.event.ArmorRenderHandler$1
```

**不是 mod 的问题** ✗ —— 那个类**确实在已安装的 jar 里** ✓（实测 `ArmorRenderHandler$1.class` 存在于
实例的 `scguns-0.5.5.jar` ✓）。时间线说明一切 ✓：

| 时间 | 事件 |
|---|---|
| 23:31:36 | 客户端启动（ModLauncher 开始加载，jar 被打开） |
| **23:37:39** | **我在游戏运行中把 `scguns-0.5.5.jar` 覆盖成新构建** ✗ |
| 23:38:04 | 崩溃（相距 25 秒） |

原因：JVM 打开 zip 时缓存的是**旧文件的条目偏移** ✓，文件被就地替换后，按旧偏移去读新文件 ⇒
拿到的是别的数据/找不到条目 ⇒ `NoClassDefFoundError` ✓（这次是随机命中 `$1` 那个合成类 ✓，
换一次可能命中别的类 ✓）。**重启即可恢复** ✓，不会复现 ✓。

**规矩（已写进工具）** ✓：新增 `tools/install_jar.py` ✓ —— 安装前先查有没有 java 进程占着实例目录或 dev 环境 ✓，
**有就拒绝安装**并打印是哪个进程 ✓（`--check` 只检查 ✓、`--force` 才强行装 ✓），
安装时自动把旧 jar 备份成带时间戳的 `.bak-HHMMSS` ✓。以后一律走这个脚本 ✓，
不再手写 `Copy-Item` ✓（§36.7 记过一次"测到过期 jar、白跑三轮" ✓，这次是更严重的版本 ✓）。

> 顺带：玩家说的"**硫磺雾气的 bug 有点多**"与这次崩溃**无关** ✓（崩溃栈里没有任何雾气代码 ✓）。
> 我已按 §35/§45 的老办法把这条链逐个与 0.5.5 对比 ✓：
> `SulfurGasCloudEntity.tick()` **逐行一致** ✓（数值、顺序全同 ✓）、`GasExplosion` / 两个粒子类**完全相同** ✓，
> 其余差异（`SulfurGasCloud` +6/-4 ✓、`ExoSuitGasMaskHandler` +8/-9 ✓、`GasGrenadeItem` +4/-3 ✓）
> **全部是正确的 1.21 改写** ✓（`Blocks.GRASS→SHORT_GRASS` ✓、`EnchantmentHelper→ScEnchants` ✓、
> `hurtAndBreak` 新签名 ✓、Forge→NeoForge 事件 ✓、`getUseDuration(LivingEntity)` ✓）⇒ **没有发现逻辑漂移** ✓。
> 具体症状需要玩家点名 ✓（下一步）。

# 50. 枪械附魔：**互不冲突** + **能把剑的附魔（腐蚀）附到枪上** —— 两条都修好了（"能附什么""互相冲不冲突"在 1.21 是数据）

玩家报了两件事 ✓：**① 枪械附魔之间不会冲突** ✓；**② 枪械能附出剑的附魔（腐蚀）** ✓。
两条都能在 0.5.5 源码里查到确切规则 ✓，而**移植把它们全丢了** ✗ —— 因为 1.21 把这两件事从 Java 挪进了数据 ✗。

## 50.1 0.5.5 的真实规则（逐条读出来的）

```java
// GunEnchantment：同 type 即互斥
protected boolean checkCompatibility(Enchantment other) {
   return other instanceof GunEnchantment ? ((GunEnchantment)other).type != this.type
                                          : super.checkCompatibility(other);
}
public static enum Type { WEAPON, AMMO, PROJECTILE, RELOAD }
```

| 冲突组 | 成员（从每个类的 `super(...)` 里读出来） |
|---|---|
| `WEAPON` | banzai, elemental_pop, gun_rust, lightweight |
| `AMMO` | reclaimed, shell_catcher |
| `PROJECTILE` | accelerator, collateral, heavy_shot, hot_barrel, puncturing, waterproof |
| `RELOAD` | quick_hands, trigger_finger |

"能附到哪类物品"由 `EnchantmentTypes` 的 7 个类别决定 ✓，其中三条是**带否定条件**的 ✓：

```java
GUN                      = item instanceof GunItem
BAYONET                  = item instanceof BayonetItem
TRIGGER_FINGER_COMPATIBLE= GunItem 且 不在 #scguns:single_shot
SHELL_CATCHER_COMPATIBLE = GunItem 且 不在 #scguns:does_not_eject_casings
COLLATERAL_COMPATIBLE    = GunItem 且 不在 #scguns:non_collateral
WATER_PROOF_COMPATIBLE   = GunItem
SEMI_AUTO_GUN            = （0.5.5 里没有任何附魔用它 ⇒ 死代码）
```

而 `CorrodedEnchantment`（腐蚀）用的是**原版 `EnchantmentCategory.WEAPON`** ✓（剑/斧近战 ✓，
并且 `!(other instanceof DamageEnchantment)` ⇒ 与锋利/亡灵杀手等互斥 ✓）—— **不是**枪 ✓。

## 50.2 移植丢掉了什么（实测）

* 15 个附魔 JSON 里 **`exclusive_set` 全部缺失** ✗（只有 corroded 有原版的伤害互斥 ✓）⇒ 所有枪械附魔随便叠 ✓；
* `corroded.json` 的 `supported_items` 被写成了 **`#scguns:enchantable/guns`** ✗ ⇒ **枪能附腐蚀** ✓（玩家的报告 ✓）；
* 三个带否定条件的类别被一律写成了"所有枪" ✗ ⇒ `trigger_finger` 能附到单发枪（火枪等 9 把）✗、
  `shell_catcher` 能附到不抛壳的 15 把 ✗、`collateral` 能附到 `#non_collateral` 的 6 把 ✗。

## 50.3 修法：把 0.5.5 的两套规则都变成数据

1. **互斥**：生成 `data/scguns/tags/enchantment/exclusive_set/{weapon,ammo,projectile,reload}.json` ✓
   （每组成员**含自己** ✓ —— 与原版 `minecraft:exclusive_set/damage` 的写法一致 ✓，这样互斥才是双向的 ✓），
   每个枪械附魔的 JSON 写上 `"exclusive_set": "#scguns:exclusive_set/<type>"` ✓；
2. **类别**：`corroded` → `#minecraft:enchantable/sharp_weapon` ✓（**不用** `weapon` ✓，
   因为后者还含重锤 ✓，而 0.5.5 的时代没有重锤 ✓）；三条否定类别在数据里**无法表达"除了"** ✗
   ⇒ 生成物化标签 `scguns:enchantable/{trigger_finger,shell_catcher,collateral}` ✓
   （实测 **132 / 126 / 135** 个成员 ✓，= 141 把枪减去对应标签 ✓）；
3. `tools/gen_enchantments.py` 同步改造 ✓：源目录指向 **0.5.5 参考树** ✓（本仓库只留了
   `CorrodedEnchantment` ✓，其余定义已变成数据 ✓），解析 `GunEnchantment.Type.X` ✓、补全类别映射 ✓、
   生成上面两类标签 ✓。**顺带修掉生成器自己的一个坑** ✗：`ModEnchantments` 改成 `ResourceKey` 注册后 ✓
   旧的 `register("x", C::new)` 正则再也匹配不到 ✓ ⇒ 它会**自己编 id** ✓，于是写出了
   `water_proof.json` ✓ 和真正的 `waterproof.json` **并存** ✗（实测发现 ✓，已删除多余文件 ✓，
   现在改成读 `ResourceKey<Enchantment> NAME = key("id")` ✓）。

## 50.4 验收（**运行期实证**，不是读 JSON）

新增 `tools/rcon_verify_enchant_conflicts.py` ✓：给僵尸手里塞一把枪 ✓ 再用 `/enchant` 试 ✓（实测输出 ✓）：

```
gun refuses corroded (melee only)                  OK   Gale cannot support that enchantment
gun accepts heavy_shot (PROJECTILE)                OK   Applied enchantment Heavy Shot I to Zombie's item
accelerator refused with heavy_shot (same group)   OK   Gale cannot support that enchantment
quick_hands accepted with heavy_shot (other group) OK   Applied enchantment Swift Reloader I to Zombie's item
musket refuses trigger_finger (#single_shot)       OK   Musket cannot support that enchantment
a normal gun accepts trigger_finger                OK   Applied enchantment Lead Finger I to Zombie's item
sword accepts corroded                             OK   Applied enchantment Corroded I to Zombie's item
7/7 checks passed
```

> 注意原版那条"拒绝"提示的原文是 `commands.enchant.failed.incompatible` = **"%s cannot support that
> enchantment"** ✓（在 1.21.1 的 en_us.json 里核对过 ✓）—— 也就是说"冲突"和"不该支持"共用同一句话 ✓。
> 第 3 条（accelerator）与第 4 条（quick_hands）是**同一把枪、同一次测试**里两种结果 ✓
> ⇒ 触发它的只能是**互斥组** ✓，不可能是别的 ✓。

`verify_installed_jar` 新增 **9 项**（corroded 不再支持枪 ✓、四个互斥组标签在包里 ✓、
每个枪械附魔都声明了互斥组 ✓、三个收窄标签在包里 ✓）：上一个 jar **115/124（9 项全 FAIL ✓）**、新 jar **124/124** ✓。

**未验证** ✗：**附魔台/铁砧/村民**界面里的实际候选（纯客户端 ✓ —— 但底层判据就是上面这些数据 ✓，
而它们已被运行期验证过 ✓）。

# 51. 0.5.5 的战利品注入**完全没生效**（废弃矿井/地牢开不出古典系列武器）—— 文件放错了命名空间

玩家报：**0.5.5 的战利品没有被注入到原版战利品表里**（例：废弃矿井、地牢应当出现古典系列武器）✓。
查完发现注入文件**一直都在** ✓，只是**放错了目录** ✗ ⇒ NeoForge **从来不读** ✓。

## 51.1 根因：全局战利品修饰器**只有一个合法路径**

NeoForge 21.1 的 `LootModifierManager.prepare` 里写死了一个位置 ✓（`javap -c` 实测 ✓）：

```
16: ldc  String neoforge
18: ldc  String loot_modifiers/global_loot_modifiers.json
20: invokestatic ResourceLocation.fromNamespaceAndPath:(...)   ← 即 data/neoforge/loot_modifiers/...
28: invokeinterface ResourceManager.getResourceStack:(...)     ← 所有数据包的同名文件会【合并】
```

而移植把它放在 **`data/scguns/loot_modifiers/global_loot_modifiers.json`** ✗ —— 命名空间是模组自己 ✓，
不是 `neoforge` ✓ ⇒ **整个列表无人读取** ✓ ⇒ `add_loot_dungeon` / `add_loot_mineshaft` 等 14 条修饰器
**一条都没生效** ✓✓（受伤的不止结构箱子 ✓：`pebbles_from_gravel` ✓、`shulker_core_from_shulker` ✓ 同样是死的 ✓）。

> 顺带核实：参考移植（SC2 1.5，1.21）里 `neoforge/` 与 `forge/` **两份都有** ✓；
> 但 NeoForge 21.1 只读 `neoforge/` ✓（上面那条实测）⇒ **我们只放一份** ✓ ——
> 放两份若都被读到就会**双倍注入** ✗，所以这一点必须只信实测 ✓。

## 51.2 改动

* 新增 `data/neoforge/loot_modifiers/global_loot_modifiers.json` ✓（内容与原文件完全一致：14 条 ✓）；
* 删除 `data/scguns/loot_modifiers/global_loot_modifiers.json` ✓（死文件，留着只会误导 ✓）；
* 其余什么都没动 ✓ —— 14 个修饰器定义本身（`scguns:add_loot_table` 类型、`neoforge:loot_table_id` 条件 ✓）
  以及 `scguns:chests/scguns_dungeon` / `scguns_mineshaft` 这些**自定义战利品表**本来就是对的 ✓。

## 51.3 验收（**运行期实证**）

新增 `tools/rcon_verify_loot_injection.py` ✓：把原版地牢箱子表**在世界里开 30 次** ✓，
再问游戏"地上有没有这几样东西" ✓（单次读回全部掉落物会撑爆一条 RCON 响应 ✗，所以改用精确断言 ✓）：

```
30 chests rolled into the world, 395 stacks dropped
  injected items present: ['scguns:flintlock_pistol', 'scguns:longarm',
                           'scguns:powder_and_ball', 'scguns:grapeshot']
RESULT: loot injection is live
```

⇒ **古典系列（燧发手枪、长管）确实从地牢里掉出来了** ✓✓ —— 正是玩家说缺失的那批 ✓。
（更早一次单开箱的原始回显里也直接看到过 `scguns:grapeshot` / `scguns:powder_and_ball` ✓。）

`verify_installed_jar` 新增 **2 项**（列表必须位于 `neoforge` 命名空间 ✓、且**不得**留在模组自己的命名空间 ✓）：
上一个 jar **123/126（两项 FAIL ✓）**、新 jar **126/126** ✓。

**未验证** ✗：其它结构（要塞/古城/堡垒/埋藏的宝藏等）—— 它们用的是同一套修饰器机制 ✓
（只差 `loot_table_id` 条件不同 ✓），地牢这条通了就说明机制通了 ✓，但各表的**具体掉落**没有逐个人工开箱 ✓。

# 52. 汉化是否"和英文对不上"：做了一次**值层面**的比对（结论：没发现问题，但范围要说清）

玩家要求检查汉化的描述是否与英文原文对不上 ✓。§33 当初只验过**键**（1806 条全对齐 ✓），
**从没验过值** ✗ —— 所以这次新增 `tools/audit_lang_values.py` ✓，专查"翻译贴错 key"这类问题 ✓。

## 52.1 能机械判定的四类，全部干净 ✓

| 检查 | 结果 |
|---|---|
| **格式占位符**（`%s` / `%d` / `%1$s`）逐一比对 | **0 处不一致** ✓✓ —— 这是"整段贴错位置"最直接的证据 ✓ |
| 数字 | 仅 3 处不同 ✓，逐条看过**都是中文写成"两/四"** ✓（`It won 2 world wars` → `两次世界大战的赢家` ✓、`4 barrel` → `四管` ✓） |
| 命名空间 id（`scguns:item.gauss.reload` 这类必须原样保留） | **0 条丢失** ✓ |
| 长度异常 | 1 条 ✓（`Collects empty bullet casings for recycling` → `回收空弹壳` ✓ 忠实简译 ✓） |

另外统计 ✓：**14 条与英文完全相同** ✓、**18 条不含任何汉字** ✓ —— 逐条看过 ✓，
**全是专有名词 / 格式串 / 玩笑** ✓（枪名 `Auvtomag` / `MAK MKII` ✓、音乐唱片作者 `KryoX - …` ✓、
`%s` / `°` / `+ %s` ✓、以及 `ammo_pouch.fullness` 的 `yerp` → `:)` ✓）。
⇒ **没有"漏翻译"的条目** ✓，也**没有整段错位** ✓。

## 52.2 第二条检查：关键词 —— 结论是"这条不可靠"，写清楚免得下次又指望它

"语义是否对得上"没法机械判定 ✓，我试了最接近的办法：英文提到 `tick`/伤害/半径/弹药/换弹… 时 ✓，
中文里应当出现对应词 ✓。它给出 **39 条候选** ✓，我**逐条读过** ✓ —— **全部是误报** ✓，例如：

* `Stun Grenade` → `闪光弹` ✓（命名选择 ✓，物品本身就是闪光弹 ✓）；
* `Craft Blank Mold` → `空白模具` ✓（成就标题省掉动词 ✓，中文习惯如此 ✓）；
* `Range Finder` → `测距仪` ✓；`Speed Mag` → `速装弹匣` ✓；
* `Increases damage, But lowers fire rate and shot speed.` → `增加伤害，但降低射速和弹速` ✓（完全忠实 ✓）。

这条启发式一开始还更糟 ✗：`range` 命中了 `**O**range`（没加词边界 ✓）、`Block` 没算上"块" ✓
（已修 ✓，但即便修完噪音仍然大 ✓）⇒ 工具里明确标注为 **review aid，不是门禁** ✓。

## 52.3 边界（如实说明）

* **能确定的**：不存在整段错位 ✓、不存在漏翻译 ✓、占位符/数字/id 这些"机械可判定"的部分全部正确 ✓；
* **不能确定的**：1806 条中文的**措辞与语义**是否逐条忠实 ✓ —— 这需要人读 ✓，
  启发式做不到（本轮 39 条候选全误报就是证明 ✗）。
  ⇒ 玩家若在游戏里看到**具体某一条**不对劲 ✓，给出那条的**位置或键名**即可 ✓，
  我会逐条核对并**在中英两个文件同步修正** ✓（两边都改是为了保持 §33 定下的键集对齐 ✓）。

# 53. 附魔描述 vs 实际效果：做了一遍逐条比对（结论：**没有发现不符**，但把方法与盲区写清楚）

玩家怀疑"附魔描述与当前效果不符" ✓。新增 `tools/check_enchantment_effects.py` ✓：
把 15 条描述（中英）列出来 ✓，再统计它在 **0.5.5 参考树**与**本移植**里**各自被哪里读取** ✓
—— 描述承诺了、但没有任何代码去读的效果 ✗，就会在这里暴露 ✓。

## 53.1 第一次跑出来 3 条"移植从不读取" —— **是我搜索的盲区，不是真问题**

`gun_rust` / `reclaimed` / `trigger_finger` 第一次都报"port uses it in 0 place(s)" ✗。原因 ✓：
我只按**类名**搜 ✓，而移植的 `ModEnchantments` 改用 `ResourceKey` 之后 ✓ **不再 import 那些类** ✗，
效果是从**常量**取的 ✓（`ScEnchants.level(weapon, ModEnchantments.RECLAIMED)` ✓）。
改成同时搜 **类名 / 常量名 / 注册 id** 三种写法后 ✓ ⇒ **0 个附魔"移植没人读"** ✓✓。

> 这条本身值得记下来 ✓：**"搜不到"往往是搜索方式的问题** ✓，不是代码的问题 ✓ ——
> 今天第二次栽在同类事情上（§52 的 `range` 命中 `Orange` 也是 ✓）。

## 53.2 逐条核对结果

引用数普遍比 0.5.5 少 1–2 处 ✓，逐条看过**全部是"少了 import 与 `register(..., X::new)` 那一行"** ✓
（移植不再有这些类 ✓），**没有一个调用点丢失** ✓。抽查两条最可疑的 ✓：

| 附魔 | 0.5.5 调用点 | 移植调用点 | 结论 |
|---|---|---|---|
| `trigger_finger` | `GunEnchantmentHelper:113` | `:118` | 同一处 ✓ |
| `collateral` | `BearPackShell:63` / `OsborneSlug:68` / `ProjectileEntity:454,655` | `:66` / `:71` / `:463,714` | **四处全在** ✓ |

并逐条核对了"描述承诺的具体效果"在代码里的实现 ✓：

* `trigger_finger`「增加开火速度」✓ → `1.0F - 0.12F × 等级` 作为**射速修正** ✓（`GunEnchantmentHelper:163`）✓；
* `waterproof`「减少水中开火的武器损耗 **且** 增加水下伤害」✓ → **两个都在** ✓：
  水下伤害 `×1.15`（`:241`）✓、水下开火损耗减免（`GunEventBus:442-449`，25% 判定）✓；
* `heavy_shot`「增加伤害，但降低射速与弹速」✓ → 伤害 helper ✓ + 同一处 rate 修正里**降低射速** ✓
  （`heavyShotModifier` 与 trigger_finger 相乘 ✓，方向相反 ✓ 与描述一致 ✓）；
* `collateral`「子弹穿透多个实体」✓ → `remainingPenetrations = 3 + 等级` / `2 + 等级` ✓；
* `puncturing`「暴击/穿甲↑、基础伤害↓」✓、`accelerator`「伤害与弹速↑、耐久损耗↑」✓、
  `hot_barrel`「进度条叠伤害与射速、大幅增加后坐与散布」✓、`corroded`「对自动机加伤、对非自动机中毒」✓
  —— 调用点均在位 ✓。

## 53.3 唯一"看起来不符"的地方，其实**0.5.5 也是**

`gun_rust`「枪械将频繁卡壳」✓ —— §35 已经查实 ✓：卡壳判定只走
`GunFireEvent extends PlayerEvent` ✓ ⇒ **怪物拿的带咒枪不会卡壳** ✗。
但这是 **0.5.5 的原设计** ✓（两棵树逐行相同 ✓），不是移植改的 ✓，
所以描述**不需要改** ✓（要改就是"给怪物也加卡壳"这种**新功能** ✓，需玩家拍板 ✓）。

## 53.4 边界

* **已核实**：15 个附魔的效果**都仍被读取** ✓、调用点与 0.5.5 一一对应 ✓、
  上面 8 条描述的具体数值/方向与代码一致 ✓；
* **未逐条核实**：剩下 7 条（banzai / elemental_pop / lightweight / quick_hands / reclaimed /
  shell_catcher / gun_rust 的数值细节 ✓）—— 它们的**调用点都在** ✓，但我没有逐个读公式 ✓。
  玩家若指某一条"感觉和描述不一致" ✓，点名即可 ✓，我会把那条的公式与文案一起核对 ✓。

# 54. 玩家点名 `puncturing`（神枪手）：描述说"降低基础伤害"，代码里**这一条从来没实现过**（已修文案）

玩家报：**神枪手附魔实际上不会降低伤害，但描述里写了** ✓ —— 玩家是对的 ✓，而且**0.5.5 就是这样** ✓。

## 54.1 实测（两棵树并排读）

```java
// 0.5.5 GunEnchantmentHelper:230 与 本移植 :235 —— 逐字相同
public static float getPuncturingDamageReduction(ItemStack weapon, LivingEntity target, float damage) {
   return damage;      // ← 原样返回，什么都没做
}
```

`return damage;` ✓ —— 所以"**but decreases base damage**"这条**在 0.5.5 里就是假的** ✗✓，
移植只是忠实照搬 ✓（**不是**移植改坏的 ✓）。

它描述的**另外两条都真的在** ✓：

| 描述里的承诺 | 实现 |
|---|---|
| Increases chance of critical hits ✓ | `getPuncturingChance` → `GunModifierHelper:293 chance += ...` ✓ |
| improves armor penetration ✓ | `getPuncturingArmorBypass` = `5.0F × 等级` ✓（4 处调用 ✓） |

**描述还漏了两条真实效果** ✗（0.5.5 与移植都如此 ✓）：
同一个 rate 修正里的 `1 + 0.06 × 等级` ✓（与 `trigger_finger` 的 `1 - 0.12 × 等级` 同一个乘数 ✓），
以及后坐力 `+0.1 × 等级` ✓。

## 54.2 修法：改**文案**，不动代码

按本项目一贯原则（**行为对齐 0.5.5** ✓），把假的那半句删掉 ✓：

* EN：`Increases chance of critical hits, improves armor penetration but decreases base damage`
  → `Increases chance of critical hits and improves armor penetration` ✓
* ZH：`增加暴击几率，提高穿甲但降低基础伤害` → `增加暴击几率，提高穿甲` ✓

两个文件**同步改** ✓（保持 §33 的键集对齐 ✓），`audit_lang_keys` 仍是 **1806/1806、缺 0 多 0** ✓；
`audit_lang_values` 复跑：**格式占位符 0 处不一致** ✓。

## 54.3 留给玩家的两个选择（都不做，等指示）

1. **让代码去实现那条描述** ✓（即真的降低基础伤害 ✓）—— 这是**玩法改动** ✗、
   偏离 0.5.5 ✓，要做请明确说 ✓（做法就是在 `getPuncturingDamageReduction` 里按等级返回 `damage * (1 - k×level)` ✓）；
2. **把漏掉的两条效果写进描述** ✓（如"并略微降低射速、增加后坐力" ✓）—— 也是文案改动 ✓，说一声即可 ✓。

**验收**：`gradlew build` ✓、`verify_installed_jar` **126/126** ✓、已用带守卫的安装脚本装好 ✓
（本版之前的 jar 备份为 `.bak-123758` ✓）。

## 54.4 追查版本史：玩家猜"这机制曾经存在、后来被删" —— 实测是**反过来**的

玩家说：神枪手的降伤害机制"**曾经可能存在过，后来被删除了**" ✓ —— 这个可以直接查证 ✓：
机器上有 **0.5.5 / 1.2.5 / 1.5 / 1.5.2** 四个版本 ✓，新增 `tools/trace_puncturing_versions.py` ✓
逐个反汇编那个方法 ✓ 并从**同一个 jar** 里读出它的描述文案 ✓：

| 版本 | `getPuncturingDamageReduction` 的实现 | 描述文案 |
|---|---|---|
| **0.5.5**（本移植基准 ✓） | `return damage;` ⇒ **空实现（2 条字节码）** ✗ | 已写"but decreases base damage" ✓ |
| **1.2.5** | **在计算（25 条字节码）** ✓ | 同样文案 ✓ |
| **1.5 / 1.5.2** | 同 1.2.5 ✓ | 同样文案 ✓ |

⇒ **机制不是被删除，而是 0.5.5 时还没实现、1.2.5 之后才补上** ✓✓ ——
也就是说 0.5.5 的描述属于"**提前写好**"✗（写在了实现前面 ✓）。

**1.5 的官方公式**（读参考移植源码 ✓）：

```java
public static float getPuncturingDamageReduction(ItemStack weapon, LivingEntity target, float damage) {
   int puncturingLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.PUNCTURING, weapon);
   if (puncturingLevel > 0) {
      if (target == null || target.getArmorValue() < 10) {      // 只对护甲 < 10 的目标生效
         float reductionPercent = 0.05f * puncturingLevel;      // 每级 -5%
         return damage * (1.0f - reductionPercent);
      }
   }
   return damage;
}
```

1.5 还多一个 `getPuncturingDamageReductionForTooltip` ✓ —— 他们特意让**提示框也显示降后的伤害** ✓。

**这就成了一个明确的选择题**（本轮**没有**擅自实现 ✓，因为那是玩法改动、偏离 0.5.5 基准 ✓）：

* **(A) 保持现状** ✓：行为 = 0.5.5（不降伤害）✓ + 文案已改对 ✓；
* **(B) 按 1.5 补上机制** ✓：把上面的公式搬进本移植 ✓（并按 1.5 一起补 tooltip 那一份 ✓），
  文案再改回"并降低基础伤害" ✓ —— 这属于**向更新版对齐**而非偏离游戏本体 ✓，玩家的原意多半是这个 ✓。

（玩家点头就做 (B) ✓；实现工作量很小 ✓，且 1.5 的公式就是现成的 ✓。）

## 54.5 更正 54.4：**1.2.5 / 1.5 / 1.5.2 都是 1.21.1 的移植版**，不是更新的原版（玩家指出 ✓）

玩家指出："1.5 版本实际上是 1.21.1 的移植版" ✓ —— **按元数据实测，玩家是对的** ✓，我又一次按文件名推断 ✗：

| 文件 | 读出来的真实身份 |
|---|---|
| `ScorchedGuns-0.5.5-1.20.1.jar` | 只有 `META-INF/mods.toml`（legacy Forge 格式 ✓）、**没有** `neoforge.mods.toml` ⇒ **1.20.1 原版** ✓ |
| `ScorchedGuns-1.2.5.jar` | `neoforge=[21.1.228,)`、`minecraft=[1.21.1,1.22)`、有 `neoforge.mods.toml` ⇒ **1.21.1 NeoForge 移植** ✗ |
| `ScorchedGuns-1.5.jar`（SG2-1.21） | 同上 ⇒ **1.21.1 移植** ✗ |
| `ScorchedGuns-1.5.2.jar` | 同上 ⇒ **1.21.1 移植** ✗ |

⇒ 54.4 的**事实**仍然成立 ✓（0.5.5 是空实现 ✓、这几个 1.21.1 版本实现了 ✓），
但**结论的措辞要改** ✗：不能再说"机制在更新的官方版本里被补上" ✗ ——
准确说法是"**1.21.1 的移植版（1.2.5 / 1.5 / 1.5.2）实现了它**" ✓。

**这对选择 (A)/(B) 的意义** ✓：

* 本项目的移植基准是 **0.5.5（1.20.1）** ✓ ⇒ 现状 (A) 是"忠实 0.5.5" ✓；
* 而 **(B) 现在是"向 1.21.1 参考移植对齐"** ✓ —— 那个参考移植正是本项目一路上参照的对象 ✓
  （§36 的女仆兼容、§42 的配方书排布等都参考过它 ✓），所以 (B) 并非"引入新玩法" ✗，
  而是"与同一目标版本的另一份移植保持一致" ✓。

**教训（又一次）**：**文件名不是证据** ✗ —— 版本号相同（1.5）的 jar 可能是**另一个 MC 版本的移植** ✓。
凡是判断"某个行为属于原版还是移植" ✓，都要**读 jar 里的元数据** ✓（这次一条 `neoforge.mods.toml` 的存在与否就定了 ✓）。

# 55. `scguns:supply_crate` 破坏不出弹药 —— **1.21 物品谓词改了写法，旧写法被静默忽略**（影响 28 张表）

玩家报"supply_crate 不能正常破坏出弹药" ✓。**复现且定位** ✓：

```
> loot spawn 0 99 0 mine 0 100 0            ← 先用原版 /loot 直接评估这张表（空手）
Dropped 1 [Supply Crate] from loot table scguns:blocks/supply_crate     ← 掉的是箱子本身 ✗
```

## 55.1 根因：1.20.5 把物品谓词里的附魔挪进了组件式谓词

0.5.5（1.20.1）的写法 ✓：

```json
"condition": "minecraft:match_tool",
"predicate": { "enchantments": [ { "enchantment": "minecraft:silk_touch", "levels": {"min": 1} } ] }
```

1.21.1 的写法 ✓（从原版 `data/minecraft/loot_table/blocks/stone.json` 里抄的 ✓）：

```json
"condition": "minecraft:match_tool",
"predicate": { "predicates": { "minecraft:enchantments": [ ... ] } }
```

**关键**：Mojang 的 codec **忽略未知字段** ✗ ⇒ 旧写法**不报错** ✓、
但解析成**空谓词** ✓ —— 而**空谓词匹配一切** ✓✓ ⇒ 那些"精准采集"分支**永远成立** ✓：

* `supply_crate`：永远掉**箱子本身** ✗ ⇒ 玩家说的"破坏不出弹药" ✓；
* **另外 27 张表**（矿石 + 20 种硝化玻璃）✓：矿石**空手就掉矿石方块** ✗（等于白送精准采集 ✓）、
  玻璃**空手也掉自己** ✗ —— 全是同一个根因 ✓✓。

实测对照（`/loot ... mine` ✓，同一张表两种工具 ✓）：

| 工具 | 修复前 | 修复后 |
|---|---|---|
| 空手 | `Dropped 1 [Supply Crate]` ✗ | `Copper Flare ×2 / Advanced Round ×4 / Microjet ×4` ✓ |
| 空手挖矿 | 掉矿石方块 ✗ | `Raw Anthralite` ✓ |
| 精准采集镐 | — | `Anthralite Ore` ✓ |

## 55.2 顺带修掉：一个用了 1.21 已删除条件类型的战利品修饰器

开机日志里一直有这条 ✓（**它不是新问题，之前没人查** ✗）：

```
WARN LootModifierManager: Could not decode GlobalLootModifier with json id
     scguns:shulker_core_from_shulker - error: Unknown registry key ... minecraft:random_chance_with_looting
```

`random_chance_with_looting` 在 1.21 已被移除 ✗ ⇒ 那条"潜影贝掉 shulker_core"（5% + 每级抢夺 +2%）
**从来没生效过** ✓。改成 1.21 的替代品 ✓（字段名从源码核实 ✓）：

```json
{ "condition": "minecraft:random_chance_with_enchanted_bonus",
  "unenchanted_chance": 0.05,
  "enchanted_chance": { "type": "minecraft:linear", "base": 0.05, "per_level_above_first": 0.02 },
  "enchantment": "minecraft:looting" }
```

`/reload` 后该警告消失 ✓（日志里只剩修复前那次启动的那条 ✓）。

## 55.3 改动与验收

* `tools/fix_loot_item_predicates.py` ✓（**28 张表**，逐表只改谓词那几行 ✓ —— `git diff` 实测 28 文件、+287/−229 ✓，
  没有整文件重排 ✓）；`shulker_core_from_shulker.json` 手工改 ✓；
* **实测**（dev 服务器 + `/loot ... mine`，见 55.1 的对照表 ✓）：箱子掉弹药 ✓、矿石按工具区分 ✓；
* `verify_installed_jar` 新增 **2 项**（包里**不得**再有旧谓词写法 ✓、**不得**再用已删除的条件类型 ✓）：
  修好前 **126/128（两项 FAIL ✓）**、新 jar **128/128** ✓；
* ⚠️ **安装脚本第一次拒绝了** ✓ —— 因为**我自己的 dev 服务器**还在跑 ✓（正是 §49 的守卫在起作用 ✓，
  不是玩家客户端 ✓）。停掉 dev 服务器后安装成功 ✓。

**未验证** ✗：20 种硝化玻璃的表只做了同一模式的静态修复 ✓，没有逐个开箱 ✓（模式与矿石完全一致 ✓）。

# 56. 战利品里的盔甲**没有附魔** —— 1.21 的"可附魔"改由原版物品标签决定，我们一个标签都没带

玩家报"战利品表内出现的盔甲没有附魔，武器可能也存在这个问题" ✓。**护甲这条成立 ✓，武器那条不成立 ✓**
（下面都有实测 ✓）。

## 56.1 根因

战利品表里用了 `minecraft:enchant_with_levels`（**10 处** ✓）与 `minecraft:enchant_randomly`（**26 处** ✓）。
1.21 的这个函数是 `EnchantmentHelper.enchantItem(randomsource, stack, levels, registryAccess, options)` ✓，
`options` 为空时从**与该物品兼容**的附魔里挑 ✓（`only_compatible` 默认 true ✓）。
而"兼容"在 1.21 = **物品在原版 `#minecraft:enchantable/*` 标签里** ✓。

* 0.5.5（1.20.1）用的是 `EnchantmentCategory`：**任何 `ArmorItem` 都算 `ARMOR`** ✓ ⇒ 模组护甲**能附魔** ✓；
* 移植**一个 `data/minecraft/tags/item/enchantable/*` 都没有** ✗（实测：`data/minecraft/tags/item/` 下
  只有 swords/axes/hoes/pickaxes/shovels/music_discs/… ✓，**没有 enchantable 目录** ✗）
  ⇒ 函数找不到任何兼容附魔 ⇒ **掉出来的是白板** ✓✓ —— 正是玩家看到的 ✓。

**武器那条不成立** ✓（实测）：移植带了 `data/minecraft/tags/item/swords.json` = `scguns:anthralite_sword` ✓，
而 1.21 的 `#minecraft:enchantable/sword` 就是 `#minecraft:swords` ✓ ⇒ 模组唯一的近战武器**本来就可附魔** ✓。

## 56.2 改动：补 8 个标签（生成器产出，便于物品变动后重跑）

`tools/gen_enchantable_tags.py` ✓ 从 `ModItems` 里按类名识别护甲 ✓、按后缀分槽位 ✓，写出：

| 标签 | 件数 |
|---|---|
| `enchantable/armor` | 37 ✓ |
| `enchantable/head_armor` | 12 ✓（头盔/面具/呼吸器 ✓） |
| `enchantable/chest_armor` | 9 ✓ |
| `enchantable/leg_armor` | 8 ✓ |
| `enchantable/foot_armor` | 8 ✓ |
| `enchantable/durability` / `equippable` / `vanishing` | 各 37 ✓（耐久/绑定诅咒/消失诅咒 ✓） |

全部 `"replace": false` ✓ ⇒ 与原版标签**合并**而不是覆盖 ✓。

> 注意**没有**给枪加这些标签 ✓ —— 枪在 0.5.5 里也不吃原版附魔 ✓（§30.2 实测 `/enchant … unbreaking` 被拒 ✓），
> 枪只走模组自己的附魔 ✓。这次改的**只是护甲** ✓，即"恢复 1.20.1 的护甲可附魔" ✓。

## 56.3 验收（运行期实证）

```
> loot spawn 0 99 0 loot scguns:raids/copper_boss
Dropped 4 items
Scrap Helmet has the following entity data: {components:
    {"minecraft:enchantments": {levels: {"minecraft:unbreaking": 3}}}, count: 1, id: "scguns:scrap_helmet"}
```

⇒ 修复后**掉出来的护甲带上了附魔** ✓✓（修复前同一张表掉的是白板 ✓）。

`verify_installed_jar` 新增 **8 项**（每个标签里的 `scguns:*` 条目数下限 ✓）：
上一个 jar **126/136** ✓（8 项新检查 + §55 的 2 项一起 FAIL ✓）、新 jar **136/136** ✓。

**顺带说明一个可见后果** ✗：护甲既然回到了原版可附魔集合 ✓，**附魔台**现在也会给模组护甲提供这些附魔 ✓
（这正是 0.5.5 的行为 ✓）。附魔台界面本身是纯客户端 ✓ ⇒ 需要玩家在游戏里确认一眼 ✓。

# 57. 枪械/配件的原版附魔 + 配件吃经验修补（玩家定规则：**只有配件自己带经验修补才修**）

玩家先问了两件事 ✓：**枪械现在能吃耐久/经验修补吗** ✓、**装在枪上的配件吃不到经验修补** ✓；
并对上一轮我的顾虑做了更正 ✓：**经验修补是宝藏附魔、附魔台拿不到** ✓（所以不存在"附魔台刷出经验修补"✓）、
**耐久可以** ✓。两条我都实测过 ✓：

* 原版标签实测：`enchantment/treasure` 含 mending ✓、`non_treasure` 不含 ✓、`in_enchanting_table` 不含 ✓；
* 1.20.1 的 `EnchantmentCategory.BREAKABLE.canEnchant` **只调 `Item.canBeDepleted()`** ✓（javap 实测 ✓，没有类别限制 ✓）
  ⇒ 当年**任何有耐久的物品**（枪 ✓、配件 ✓）都能附耐久/经验修补 ✓ ⇒ **移植把它弄丢了** ✗（与 §56 同一个根因 ✓）。

## 57.1 配件为什么吃不到经验修补（机制，非猜测）

原版修 XP 的位置是 `ExperienceOrb.repairPlayerItems` ✓：

```java
Optional<EnchantedItemInUse> optional = EnchantmentHelper.getRandomItemWith(
        EnchantmentEffectComponents.REPAIR_WITH_XP, player, ItemStack::isDamaged);   // ← 只找【背包/装备】
int i = EnchantmentHelper.modifyDurabilityToRepairFromXp(level, itemstack, (int)(value * itemstack.getXpRepairRatio()));
```

而配件存在**枪自己的标签里** ✓（`Attachments.<类型>` ✓，`Gun.getAttachment` 用 `NbtHelper.itemFromTag` 读回 ✓）
—— 它**根本不是一件背包物品** ✗ ⇒ 原版**结构上永远看不到它** ✓✓ —— 这就是那个痛点的真因 ✓。

## 57.2 改动

**(a) 恢复"有耐久的物品可附魔"** ✓：生成器现在还会收集**注册时写了 `durability(...)` 的物品** ✓
（枪 ✓ / 配件 ✓ / 工具模具 ✓，实测 **218 件** ✓），写进 `#minecraft:enchantable/durability` 与 `vanishing` ✓
（共 220 条 ✓）。这样配件**才可能带上经验修补** ✓，也是 (b) 的前提 ✓。

**(b) 新增 `AttachmentMendingHandler`** ✓：挂在 **`PlayerXpEvent.PickupXp`** 上 ✓
（该事件在 `ExperienceOrb.playerTouch` 顶部、**原版修理之前**发出 ✓），对**手持枪的配件**逐个检查 ✓：

* **只有配件自己带 `minecraft:mending` 才修** ✓ —— 玩家选的规则 3 ✓（枪上的经验修补**不**惠及配件 ✓）；
* 修理量用**原版同一个公式** ✓（`modifyDurabilityToRepairFromXp(xp * getXpRepairRatio())` ✓），
  减去耐久上限后写回枪的标签 ✓（`NbtHelper.tagFromItem` + `Attachments.<类型>` ✓）；
* **不扣经验球的经验** ✓：完全不碰原版流程 ⇒ 枪自己的经验修补照旧 ✓。
  （改成"共用一个经验池"需要接管经验球拾取 ✗，那是更大的行为改动 ✓ —— 玩家要就说 ✓。）

## 57.3 实测

```
> item replace … weapon.mainhand with scguns:unbreaking … (枪)
Applied enchantment Unbreaking I to Zombie's item        ← 修复前是 "cannot support that enchantment" ✗

> item replace … weapon.mainhand with scguns:long_scope … (配件)
Applied enchantment Mending to Zombie's item             ← 配件现在能带经验修补 ✓（规则 3 的前提 ✓）
Applied enchantment Unbreaking I to Zombie's item        ✓
```

`verify_installed_jar` 新增 **4 项**（durability 标签条数 ✓、枪在 ✓、配件在 ✓、处理器在 ✓）：
上一版 **128/140** ✓、新 jar **140/140** ✓。

**未验证** ✗：**真正"捡经验球 → 配件耐久被修"** 那一步 —— 需要真玩家捡球 ✓（专用服务器没有玩家 ✓，
我在这一轮里也没能构造出来 ✓）。请玩家验：给枪装上**带经验修补**的配件 ✓、把配件耐久打掉一些 ✓、
再捡几颗经验球 ✓ ⇒ 配件耐久应当回升 ✓（同时枪的耐久照旧由原版处理 ✓）。

# 58. **带附魔的配件一装上就消失** —— `ItemStack.CODEC` 没有注册表访问权，编码失败被静默吞掉（根因实测）

玩家报：**带附魔的配件安装到枪上后直接消失** ✓，并给了三条关键信息 ✓：
**① 两边都没了**（枪上没有、背包里也没回来 ✓ = 那个栈被销毁了 ✓）；
**② 只有带附魔的才会** ✓；**③ 放进去立即消失** ✓。

## 58.1 根因（临时探针实测，非推断）

`NbtHelper.tagFromItem` 是安装路径用的转换器 ✓：

```java
public static CompoundTag tagFromItem(ItemStack stack) {
   return (CompoundTag) ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack)
           .result()                       // ← 编码失败时是 empty
           .filter(t -> t instanceof CompoundTag)
           .orElseGet(CompoundTag::new);   // ← 于是【静默】返回空标签
}
```

临时探针（`ServerStartedEvent` ✓，读完后已删除 ✓）直接把两种情况都编码了一遍 ✓：

```
[SCGUNS-PROBE] Long Scope enchanted=false  encodeOk=true   helperTagSize=2  roundTripEmpty=false
[SCGUNS-PROBE] Long Scope enchanted=true   encodeOk=false  helperTagSize=0  roundTripEmpty=true
     encodeError = DataResult.Error['Can't access registry ResourceKey[minecraft:root / minecraft:enchantment]']
```

⇒ **带附魔的栈编码直接失败** ✗，原因是 **`NbtOps.INSTANCE` 没有注册表访问权** ✗ ——
1.21 的附魔是按**注册表引用**编码的 ✓，没有 `RegistryOps` 就拿不到 ✓。
⇒ `tagFromItem` 把失败**静默**变成 `{}` ✗ ⇒ `AttachmentContainer.slotsChanged` 把 `Attachments.<类型>` 写成空标签 ✗
⇒ 读回来是空栈 ⇒ **枪上没有、背包里也没有** ✓✓ —— 三条症状一次全解释 ✓。

**这不是"带附魔才有的新 bug"** ✓：这段代码用 **`NbtOps` 而非 `RegistryOps`** ✗ 从一开始就是错的 ✗；
只是 §57 之前配件**不可能**带附魔 ✓，所以它从来没有被触发过 ✓（本质是**潜伏已久的静默数据丢失** ✓）。

## 58.2 修法（下一步，尚未实现）

1. **转换器要带注册表** ✓：`tagFromItem` / `itemFromTag` 改成用
   `RegistryOps.create(NbtOps.INSTANCE, provider)` ✓（编码与解码**都需要** ✓ —— 解码带附魔的配件同理会失败 ✗）；
2. **provider 从哪来** ✓：这两个 helper 的调用点很多（`Gun.getAttachment` ✓、方块实体 ✓、菜单 ✓、服务端包处理 ✓），
   短期用一个**静态持有者**（服务端在 `ServerStartedEvent` 时写入 ✓、客户端在登录/进世界时写入 ✓）并在 helper 内部取用 ✓，
   同时保留旧签名（未初始化时退回 `NbtOps` ✓）；长期应把 provider 参数显式传到调用点 ✓；
3. **绝不能再静默** ✗：编码失败时必须**打错误日志**（现在是一声不响地丢东西 ✗），
   并且**不覆盖**已有的 `Attachments` 标签 ✓（失败时保持原样，宁可不动也不要销毁 ✓）。

**验收计划**：修好后用同一个探针复测（`enchanted=true` 时 `encodeOk=true`、`roundTripEmpty=false` ✓），
再用 RCON 走一遍"把带附魔的配件装进枪"的往返 ✓（若需要玩家端操作 ✓ 则给出步骤 ✓），
并加门禁检查（helper 里必须出现 `RegistryOps` ✓、且不得再有 `.orElseGet(CompoundTag::new)` 这种静默兜底 ✓）。

## 58.3 已按 58.2 实现并实测（玩家："直接修"）

| 改动 | 内容 |
|---|---|
| `util/NbtHelper` | 新增 **`volatile HolderLookup.Provider registryAccess`** ✓ + `setRegistryAccess/hasRegistryAccess` ✓ + `ops()` ✓：有 provider 时用 **`RegistryOps.create(NbtOps.INSTANCE, provider)`** ✓，没有则退回 `NbtOps`（与旧行为一致 ✓）；`itemFromTag` 解码失败会 **ERROR 日志** ✓；`tagFromItem` 编码失败同样 ERROR 并注明"调用方不得用空标签覆盖既有数据" ✓ |
| `event/RegistryAccessListener`（新） | `LevelEvent.Load` ✓（客户端/服务端都会触发 ✓，客户端的 level 带着连接的注册表 ✓）+ `ServerStartedEvent` ✓ ⇒ 覆盖单人/专用服务器/客户端连服三种情形 ✓；**故意不在卸载时清空** ✓（注册表在本会话内是静态的 ✓） |
| `client/screen/AttachmentContainer.slotsChanged` | 安装路径改为：**某个非空配件编码失败就整块放弃** ✓（保持枪上的 `Attachments` 原样 ✓ + 日志 ✓），不再写入空标签把配件抹掉 ✓ |

**复测（同一个探针，读完已删除 ✓）**：

```
修复前  Long Scope enchanted=true  helperTagSize=0  roundTripEmpty=TRUE   ← 配件被销毁 ✗
修复后  Long Scope enchanted=true  providerSet=TRUE  helperTagSize=3  roundTripEmpty=false  roundTripEnchanted=TRUE ✓✓
```

⇒ **带附魔的配件现在能完整往返** ✓，且 `providerSet=true` 说明**监听器确实在服务端把注册表访问权写进去了** ✓
（不是探针自己塞的 ✓）。打包核对 ✓：探针不在 jar 里 ✓、`RegistryAccessListener` 在 ✓、
`NbtHelper.class` 里出现 `RegistryOps` ✓；`verify_installed_jar` **140/140** ✓、已安装 ✓
（上一版备份 `.bak-141412` ✓）。

> 备注：这条修的是**通用隐患**（`NbtHelper` 的编解码）✓ —— 凡是用它做物品↔标签转换的地方（枪 ✓、
> 配件 ✓、方块实体 ✓、服务端包 ✓）此前都在**没有注册表**的情况下工作 ✗，
> 只是"没附魔/没引用注册表的组件"才碰巧成功 ✓。现在这些路径都拿到注册表了 ✓。

# 59. 装带附魔的配件 → 玩家**掉线**（`container_set_content` 编码失败）—— §58 的修法把注册表访问权交给了错误的一方（实测根因）

## 59.1 现象（玩家原话 + 日志现场）

玩家报：`Internal Exception: io.netty.handler.codec.EncoderException: Failed to encode packet
'clientbound/minecraft:container_set_content'`。

日志现场（玩家实例 `logs/latest.log` 第 2026–2103 行，14:17:27）：

```
[14:17:27.643] [Server thread/ERROR] [scguns-nbt/]: Could not encode 0 minecraft:air (with registry access); …
[14:17:27.645] [Netty Server IO #2/ERROR] [net.minecraft.network.Connection/]: Exception caught in connection
io.netty.handler.codec.EncoderException: Failed to encode packet 'clientbound/minecraft:container_set_content'
	at …IdDispatchCodec.encode(IdDispatchCodec.java:55)
	at …ClientboundContainerSetContentPacket.write(ClientboundContainerSetContentPacket.java:41)
Caused by: java.lang.IllegalArgumentException: Can't find id for
   'Reference{ResourceKey[minecraft:enchantment / minecraft:mending]=Enchantment 经验修补}' in map net.minecraft.core.Registry$1@…
	at …core.IdMap.getIdOrThrow(IdMap.java:27)
	at …network.codec.ByteBufCodecs$25.encode(ByteBufCodecs.java:479)
	at …core.component.DataComponentPatch$1.encodeComponent(DataComponentPatch.java:127)
	at …world.item.ItemStack$1.encode(ItemStack.java:151)
[14:17:27.718] [Server thread/INFO] [minecraft/MinecraftServer/]: Stopping server
[14:17:28.027] [Render thread/WARN] [ClientCommonPacketListenerImpl/]: Client disconnected with reason: Internal Exception: …
```

同一个日志里既有 `Server thread` 也有 `Render thread` ✓ ⇒ 玩家当时在**单人**（集成服）✓。

## 59.2 根因（为什么 `minecraft:mending` 找不到 id）

`IdMap.getIdOrThrow` 查的是**网络 id 表**，它只认识**用来发包的那个注册表实例**里的对象 ✓。抛错说明物品栈里那个 `minecraft:mending` 的
holder **不属于发包方那份附魔注册表实例** ✓。

单人下客户端与集成服在同一个 JVM ✓，但**附魔注册表是两份实例**（客户端那份是从登录包重建出来的）✓（**推定**：错误信息只说明"holder 不在该
实例的 id 表里" ✓，结合代码，能造成这一点的只有客户端那一份 ✓）。

§58 我写的是「**任何** `LevelEvent.Load` 都 `setRegistryAccess(level.registryAccess())`」✗ ⇒ 单人的加载顺序是"服务端 level → 客户端
level" ✓ ⇒ **最后写入的成了客户端那份** ✗ ⇒ 服务端在服务端线程上用 `NbtHelper.itemFromTag` 解码物品时，拿到的是**客户端注册表的 holder** ✗
⇒ 这个 holder 被放进枪的 NBT（`Attachments.<type>` ✓）⇒ 服务端把容器内容发给客户端时
`IdMap.getIdOrThrow` 失败 ✗ ⇒ **掉线** ✓。

⇒ 与"配件带附魔"本身无关 ✓：是**我把注册表访问权交给了错误的一方** ✗ —— 上一轮的修法引入了更严重的回归 ✓（玩家看到的是掉线，不是丢物品 ✓）。

## 59.3 修法（按"谁在场"决定归属）

`RegistryAccessListener` 三条规则（`src/main/java/top/ribs/scguns/event/RegistryAccessListener.java`）：

1. `ServerLevel` 加载 / `ServerStartedEvent` ⇒ `serverOwnsRegistryAccess = true` 且**无条件**写服务端注册表 ✓ —— 存储与发包的一方永远赢 ✓；
2. 非服务端 level（客户端）加载 ⇒ **只有**在 `!serverOwnsRegistryAccess` 时才写自己那份 ✓ —— 这正是"专用服 + 外部客户端"：那个 JVM 里没有服务端，客户端那份就是唯一的一份 ✓；
3. `ServerStoppedEvent` ⇒ `serverOwnsRegistryAccess = false` ✓（此事件触发时存档已写完 ✓）—— 同一个 JVM 里"先单人、后连专用服"时，客户端会刷新成自己那份 ✓，不会留着已死服务端的 holder ✗→✓。

`grep` 实测：`NbtHelper.setRegistryAccess` 的调用点**只有 3 处** ✓，全在上面这个类里 ✓。

## 59.4 实测（无头端到端，走的正是出错的那条编解码路径）

枪架方块实体（`GunShelfBlockEntity`）存展示物品**正好**走 `NbtHelper.tagFromItem`/`itemFromTag` ✓（`grep` 实测 ✓），直接拿它做往返：

```
setblock 0 100 0 scguns:gun_shelf
data merge block 0 100 0 {DisplayedItem:{id:"scguns:long_scope",count:1,
     components:{"minecraft:enchantments":{levels:{"minecraft:mending":1}}}}}
  → Modified block data of 0, 100, 0
data get block 0 100 0 DisplayedItem
  → {components: {"minecraft:enchantments": {levels: {"minecraft:mending": 1}}}, count: 1, id: "scguns:long_scope"}
save-all → （干净退出，区块落盘并卸载）→ 重新 runServer
data get block 0 100 0
  → {x:0,y:100,z:0,id:"scguns:gun_shelf",DisplayedItem:{components:{"minecraft:enchantments":
     {levels:{"minecraft:mending":1}}},count:1,id:"scguns:long_scope"}}
```

⇒ **存盘 → 区块卸载 → 重新加载**之后附魔仍在 ✓（`load` 用服务端 provider 解码 ✓、`saveAdditional` 再编码 ✓）。
对照组是 §58 的探针实测：**没有** provider 时 `roundTripEmpty=true`（附魔全丢，变成 `{}`）✓。

> 命令行备注：Windows PowerShell 5.1 会把**传给原生 exe 的参数里的双引号吃掉** ✗，带 `"minecraft:mending"` 的 SNBT 没法内联传参 ✓ ⇒
> `tools/rcon_cmd.py` 新增 `--file <路径>` ✓（从文件逐行读命令，绕过 shell ✓）。

打包与门禁：`gradlew build` ✓、21 个审计全过 ✓、`verify_installed_jar` **140/140** ✓、已安装到玩家实例 ✓（上一版备份
`.bak-142126`、`.bak-142853` ✓ —— **注意**：`142126` 那次装的还是"只过滤客户端 level"的中间版本 ✗，
与本节代码一致的最终版是 `142853` ✓）；本次服务端 `run/logs/latest.log` 过滤第三方噪声后 **0 条 ERROR/FATAL** ✓。

## 59.5 顺手修掉的日志噪声（§58 自己引入的）

`tagFromItem(ItemStack.EMPTY)` 之前会打 ERROR ✗ —— 但空栈在 `ItemStack.CODEC` 里**本来就不可编码**（它拒绝
`minecraft:air` 和 `[1;99]` 之外的 count ✓），这是正常输入 ✓。现在空栈直接返回空 tag 且**不记日志** ✓；非空栈编码失败仍然 ERROR ✓
（这条日志本身没丢 ✓：`AttachmentContainer` 的"编码失败就别覆盖旧数据"守卫还在 ✓）。

## 59.6 边界（未验证）

- **真机客户端链路我无法无头复现** ✗：触发条件就是"客户端那份注册表覆盖了服务端那份" ✗ ⇒ **请再进一次游戏复测**：
  「带附魔的配件 → 装到枪上」应**不再掉线** ✓。这条是本次修复的最终验收 ✓。
- 「单人下客户端与服务端是两份附魔注册表实例」是**推定** ✓（依据见 59.2），没有直接打印两个实例来对照 ✗。
- 规则 2（专用服 + 外部客户端各用自己那份）**没实测过外部客户端** ✗，只做了源码级核对 ✓。
- 枪架往返只覆盖 `NbtHelper` 的编解码 ✓，不覆盖 `AttachmentContainer` 的菜单逻辑 ✓。

# 60. 配件开火**不掉耐久** —— 0.5.5 打在了"副本"上（源码对比 + 探针实测），已修

## 60.1 玩家报告

"配件在枪械开火时没有正常消耗耐久"。

## 60.2 根因（实测）

`GunEventBus.damageAttachments`（0.5.5 原文，我们此前逐字移植 ✓）：

```java
ItemStack scopeStack = Gun.getAttachment(IAttachment.Type.SCOPE, stack);
…
scopeStack.hurtAndBreak(1, player, null);
```

而 `Gun.getAttachment` 是**从枪的 tag 里解码出一个新栈** ✓（0.5.5：`ItemStack.of(attachment.getCompound(type.getTagKey()))` ✓；
我们 1.21.1：`NbtHelper.itemFromTag(attachment.getCompound(type.getTagKey()))` ✓）⇒ `hurtAndBreak` 打在**副本**上 ✗，副本用完即丢 ✗
⇒ 枪 NBT 里的配件**永远是 0 耐久消耗** ✓。

连带后果 ✓：那个"到 `maxDamage-1` 就 `removeAttachment` + 播放 `ITEM_BREAK`"的分支**永远进不去** ✗（`currentDamage` 恒为 0 ✓）
⇒ 配件既不磨损、也不会断掉 ✓ —— 即 0.5.5 里这段是**死代码** ✓。

证据（`grep` 实测）：0.5.5 全树里写 `Attachments` 的地方**只有两处** ✓ —— `AttachmentContainer`（安装界面 ✓）与
`Gun.Modules.Attachments.serializeNBT`（那是枪数据，不是物品 ✓）⇒ **没有任何写回路径** ✓。
旁证 ✓：参考 1.21.1 移植版已经修了同一处 ✓ —— `GunEventBus.java` 第 507–526 行新增
`Gun.setAttachment(gunStack, type, attachmentStack, player.registryAccess())` ✓ 并在磨损分支调用 ✓（它同时把 `==` 放宽为 `>=` ✓）。

⇒ 这属于"**0.5.5 本身就坏了**、上游已经修过"的情形 ✓（§48 那条教训的反面：坏掉的功能确实是 bug，但要先证明原版也坏 ✗→✓ 后才动手 ✓）。

## 60.3 改动

1. `Gun.setAttachment(ItemStack gun, IAttachment.Type type, ItemStack attachment, HolderLookup.Provider provider)` ✓
   - tag 走 `NbtHelper.getOrCreateTag(gun)` ✓ —— 必须是**独属于这个栈**的 tag ✓，因为 `ItemStack.matches` 是按值比较组件 ✓，
     直接改 live tag 客户端看不见 ✗（NbtHelper 类注释里那条规则 ✓）；
   - 编码走 `NbtHelper.tagFromItem(attachment, provider)` ✓ —— 显式传 `player.registryAccess()` ✓：tag 里存的是**引用** ✓，
     谁解码谁解析 ✓，两侧都正确 ✓，也少依赖 §59 那个静态 provider ✓；
   - **编码失败就不写** ✓（保留原配件 ✓，§58/§59 的守卫 ✓）。
2. `GunEventBus.damageAttachments` 改为按类型调用 `damageAttachment(...)` ✓（结构同参考版 ✓），保留 0.5.5 的 `ITEM_BREAK` 音效 ✓；
   `hurtAndBreak` 只在**不会跨过 maxDamage** 时调用 ✓（"断掉"由我们播声 + 摘除 ✓，不让 `hurtAndBreak` 自己决定销毁 ✓）；
   摘除用 `type.getTagKey()` ✓（0.5.5 是硬编码 `"Scope"`/`"Under_Barrel"` ✓）。
3. `Gun.removeAttachment` 改用 `NbtHelper.getTagForWrite` ✓ —— 0.5.5 直接改 live tag ✗ ⇒ 摘除不一定被同步 ✗
   （"配件要丢掉再捡起来才消失"那一类 bug ✓）。**摘除与磨损必须在同一版里修** ✓，否则修好磨损会换来一个"断了但看不见断"的新问题 ✗。
4. `AttachmentMendingHandler` 的写回改为复用 `Gun.setAttachment` ✓（顺带获得同一个编码失败守卫 ✓）。
5. `NbtHelper` 新增**显式注册表访问权**重载 ✓：`tagFromItem(ItemStack, HolderLookup.Provider)` /
   `itemFromTag(CompoundTag, HolderLookup.Provider)` ✓（原无参版本保留 ✓，语义不变 ✓）。

## 60.4 实测（临时探针 + FakePlayer，已删除）

开火触发需要玩家 ✗，所以用 `FakePlayerFactory.getMinecraft(level)` 直接驱动
`GunEventBus.damageAttachments` ✓；每次读回都重新执行 `Gun.getAttachment`（**从枪的 NBT 解码** ✓）—— 能读回就等于真的写进去了 ✓：

```
PROBE gun=scguns:flintlock_pistol instabuild=false
PROBE candidate scguns:long_scope maxDamage=1600 damageable=true
PROBE installed scguns:long_scope as Scope -> equipped=true damage=0
PROBE after shot 1 damage=1 equipped=true
PROBE after shot 2 damage=2 equipped=true
PROBE after shot 3 damage=3 equipped=true
PROBE before break shot damage=1599 maxDamage=1600
PROBE after break shot equipped=false attachment=0 minecraft:air
```

⇒ 每发 **-1** ✓、耐久**持久化** ✓、到 `maxDamage-1` 后再挨一发就**断掉并摘下** ✓。
（同批测得候选配件耐久 ✓：`long_scope` 1600 ✓ / `extended_barrel` 700 ✓ / `wooden_stock` 1550 ✓ /
`extended_mag` 1700 ✓ / `iron_bayonet` 256 ✓ —— 也就是说长瞄具要约 **1600 发**才会断 ✓，掉耐久很慢是设计如此 ✓。）

门禁：`build` ✓、21 个审计全过 ✓、`verify_installed_jar` **145/145** ✓（新增五项：`Gun.setAttachment` 存在 ✓、
磨损路径调用它 ✓、按 `getTagKey` 摘除 ✓、编解码有显式注册表重载 ✓、写回用 `getOrCreateTag`（可同步）✓）、
探针类**不在 jar 里**（实测 0 个 `TempAttachment*` ✓）、已安装 ✓（备份 `.bak-143716` ✓）。

## 60.5 边界（未验证）

- **真机开火链路没实测** ✗（`ServerPlayHandler.handleWeaponDamage` → 服务端 ✓）：探针直接调的正是同一个方法 ✓，
  但没走"扣扳机 → 客户端发包 → 服务端开火"那条路 ✓。**请实弹打几百发看配件耐久是否下降** ✓（长瞄具 1600 点 ≈ 1600 发 ✓）。
- **近战路径**（`MeleeAttackHandler.damageGunAndAttachments`，枪托砸人 ✓）同样受益 ✓，未实测 ✗。
- **客户端显示同步**未验证 ✗：写入特意走 `getOrCreateTag` ✓ 就是为了能被 `ItemStack.matches` 看到 ✓，
  但"枪拿在手上时配件耐久是否实时刷新"仍需真机 ✓。
- `grep` 只找到这一处配件磨损代码 ✓；**不能排除** 0.5.5 还有别的途径让配件掉耐久 ✗（本轮未找到 ✓）。

# 61. "经验修补不修枪/配件" —— 实测矩阵：三条路径是好的 ✓、sculk 枪那条**真的坏了**（已修 ✓）、`/xp add` 那条是原版行为 ✓

## 61.1 玩家报告

"在附魔经验修补后，拾取经验并不能正常修补枪械和配件"。

## 61.2 实测矩阵（FakePlayer + 真 `ExperienceOrb`，走完整 `playerTouch → PickupXp` 链路）

| | 场景 | 结果 |
|---|---|---|
| A | 普通枪（`flintlock_pistol`）**自身有 Mending** + 经验球 | 100 → **80** ✓（走**原版** `ExperienceOrb.repairPlayerItems` ✓）|
| B | 枪无 Mending、**配件自身有 Mending** + 经验球 | 200 → **180** ✓（走 `AttachmentMendingHandler` ✓，正好等于原版公式 `modifyDurabilityToRepairFromXp(10 × ratio 1.0)` = 20 ✓）|
| C | **sculk 枪**（`scguns:sculk_resonator`，无附魔，0.5.5 自带 XP 修复）+ 经验球 | 修前 100 → **100** ✗ ⇒ **真 bug** ✓；修复后 100 → **90** ✓（球值 10 → **0** ✓，即确实扣掉 10 点经验 ✓）|
| D | 创造模式 + 经验球 | 100 → **80** ✓（创造**不**阻止原版修补 ✓）|
| E | `/xp add` 式（直接 `giveExperiencePoints`，**没有球**） | 100 → **100** ✗ = **原版行为** ✓（原版 Mending 只在**经验球被拾取**时触发 ✓）|

⇒ 也就是说：**A/B/D 本来就是好的** ✓，"枪+配件都不修"的现场只可能来自 C（枪是 sculk 系 ✓）、E（经验不是用球给的 ✓），
或者**测试用的 jar/步骤不对** ✓（比如 §60 之前配件根本不掉耐久、或 §59 之前带附魔配件装不上去 ✓）。

## 61.3 根因（C）

0.5.5 的 `event/GunXpHandler.java` 声明的是 `@EventBusSubscriber(modid = "scguns", value = {Dist.CLIENT})` ✗ ——
参考 1.21.1 移植版**也是** `value = Dist.CLIENT` ✓ ⇒ 这是**上游一直存在的缺陷** ✓。后果 ✓：

- 这个类只在**客户端**执行 ✗ ⇒ 它改的是客户端那份枪的副本 ✗（下一次同步就被覆盖 ✓）；
- 它改的 `orb.value` 在客户端毫无意义 ✗（经验值是服务端数据 ✓）。

⇒ sculk 枪的"拾球修枪"从来没真正生效过 ✓（实测 C ✓）。同一个类里那段配件修复循环（0.5.5 第 43–56 行 ✓）
也是同一个原因失效 ✓ —— 这正是玩家此前描述的"原先就有的痛点" ✓，那段现在由服务端的 `AttachmentMendingHandler` 负责 ✓（实测 B ✓）。

## 61.4 改动（`src/main/java/top/ribs/scguns/event/GunXpHandler.java`）

1. 去掉 `value = {Dist.CLIENT}` ✗ ⇒ **两侧注册** ✓，并在方法开头 `if (player.level().isClientSide) return;` ✓
   ⇒ 修改发生在**物品所属的那一侧** ✓（单人里集成服就是服务端 ✓，所以单人同样生效 ✓；这一点很关键：不能用
   `Dist.DEDICATED_SERVER` ✗，那会把单人排除掉 ✓）。
2. 删掉那段**失效的**配件循环 ✗（改由 `AttachmentMendingHandler` 负责 ✓ —— 那里有配件必需的写回 ✓，
   以及玩家选的"配件需自带 Mending"规则 ✓）。
3. 保留 0.5.5 的数值与语义 ✓：`repairAmount = min((int)(xp × 2 × 0.5), damage)` ✓、扣掉的经验写回 `orb.value` ✓、
   **只对 sculk 系枪生效** ✓（普通枪交给原版 Mending ✓）。

> 记录但不擅自改的平衡项 ✓：0.5.5 里 sculk 枪按"1 点经验 = 1 点耐久"结算 ✓，而配件与**原版**是"1 点经验 = 2 点耐久" ✓
> （0.5.5 配件那段写的是 `remainingXp -= repairAmount / 2` ✓）。数值保持 0.5.5 原样 ✓。

## 61.5 门禁

`build` ✓、21 个审计全过 ✓、`verify_installed_jar` **147/147** ✓（新增两项：`GunXpHandler` 不再引用 `Dist` ✓、
改为按 `isClientSide` 守卫 ✓）、探针类不在 jar 里 ✓、已安装 ✓（备份 `.bak-145847` ✓）。

## 61.6 边界（未验证 / 待玩家定）

- **真机（真人）拾球**未实测 ✗：探针走的是同一条 `ExperienceOrb.playerTouch → PickupXp` ✓，但拾取者是 FakePlayer ✓。
- **`/xp add` 不会修**（E ✓）是**原版行为** ✓（原版 Mending 只响应经验球 ✓）。要不要让本模组的枪/配件在"直接加经验"
  时也修 —— 这是**偏离原版**的改动 ✗，**未做** ✓，等玩家表态 ✓。
- **配件只认主手/副手那把手上的枪** ✓（0.5.5 也只认主手 ✓）：枪放在背包但没拿在手上时不会修 ✓（是否扩到整个背包未定 ✓）。
- 配件的 Mending 必须**在配件自己身上** ✓（玩家早先选的规则 3 ✓）；只给枪附 Mending 不会修配件 ✓。
- 客户端显示同步（耐久与经验条刷新）仍未在真机验证 ✗。

# 62. "经验修补不生效"第二轮排查：**141 把枪逐把实测全过** —— 缺的是玩家侧现场数据

## 62.1 玩家补充信息

- 用的是 **iron 蓝图（Federal Blueprint）**出的枪 ✓（该蓝图对应的枪共 18 把：`brawler` `crusader` `defender_pistol`
  `combat_shotgun` `iron_spear` `iron_javeline` `gyrojet_pistol` `greaser_smg` `m3_marksman` `m3_carabine`
  `mk43_rifle` `rocket_rifle` `pulsar` `trenchur` `triquetra` `ultra_knight_hawk` `venturi` `auvtomag` ✓）；
- **枪和配件都附了经验修补** ✓，但"拾取经验不生效" ✗；
- 玩家决定：`/xp add` 这类**没有经验球**的加经验**保持原版不修** ✓⇒ 本轮**不动** ✓；
- 玩家实例配置已核对 ✓：`config/scguns-common.toml` 中 `enableGunDamage = true` ✓、`enableAttachmentDamage = true` ✓
  （文件 mtime 2026-09-23 ✓）⇒ 配置不是原因 ✓。

## 62.2 逐把枪实测（临时探针，已删除；FakePlayer + 真经验球）

对**每一个注册的 `GunItem`** 依次做三件事 ✓：① `GunEventBus.damageGun`（开火磨损 ✓）；
② 附 Mending 后拾球（原版修补 ✓）；③ 装一个带 Mending 的 `long_scope` 后拾球（本模组修补 ✓）。

```
SUMMARY guns=141 wear=141 mend=141 attachmentMend=141/141
GUN scguns:mk43_rifle    max=800  wear 100->101  mend 101->81  attachment 100->80
GUN scguns:crusader      max=256  wear 100->101  mend 101->81  attachment 100->80
GUN scguns:ultra_knight_hawk max=21 wear 19->20  mend 20->0    attachment 100->80
GUN scguns:sculk_resonator max=1100 wear 100->101 mend 101->91  attachment 100->80   ← 走自己的经验修复，设计如此
```

⇒ **141 把全部通过** ✓✓✓：开火磨损 ✓、原版 Mending 修枪 ✓、配件 Mending 修配件 ✓、sculk 自修 ✓，在开发环境里逐把成立 ✓。
⇒ 玩家看到的"不生效"**不可能**由这四条路径本身解释 ✗。

## 62.3 时间线核对（玩家实例日志 + jar 时间戳）

- 玩家那次会话：**14:40:10 → 14:43:27** ✓（`logs/latest.log` ✓）；
- 当时实例里的 jar 是 **14:37:16** 安装的那版 ✓ ⇒ 已含 §59（注册表访问权 ✓）、§60（配件磨损 + 写回 ✓）、
  以及 §57 的配件修补 handler ✓；
- **14:58:18** 安装的才是 §61（sculk 修复 ✓）—— 他那把不是 sculk 枪 ✓，所以 §61 与本例无关 ✓。

## 62.4 下一步需要玩家侧现场数据（我无法无头复现 ✗）

请拿枪在手、**先开几枪把耐久打下去**，然后执行：

```
/data get entity @s SelectedItem
```

逐项确认 ✓：
- `components."minecraft:enchantments".levels` 里有没有 `"minecraft:mending"` ✓；
- `damage` 是不是 **> 0** ✓（若为 0 ⇒ 没有东西需要修 ✓，不是修补失效 ✓ —— 这是目前最可能的一种 ✓）；
- `components."minecraft:custom_data".Attachments.Scope.components...` 里的 `damage` 是不是 **> 0** ✓。

## 62.5 边界（未验证）

- **真人拾球链路**仍未实测 ✗（探针用 FakePlayer ✓，但走的是同一条 `ExperienceOrb.playerTouch → PickupXp` ✓）。
- **客户端耐久显示是否实时刷新**未验证 ✗ —— 如果服务端修了而界面没动 ✓，看起来同样像"不生效" ✓。
- 未排查玩家整合包内其它模组是否改写了 Mending 行为 ✗（该实例还有 IE / Create / Mekanism / 高级 AE / TLM 等 ✓）。
- 配件的 Mending 必须在**配件自己身上** ✓（规则 3 ✓）；`/xp add` 不修是**原版行为** ✓（玩家已确认保持 ✓）。

# 63. 换弹后**再也开不了镜** —— 卡住的 `InCriticalReloadPhase` / `ReloadState`（读码 + 探针实测，与 0.5.5 同源）

## 63.1 玩家报告

"换弹后会一直取消开镜状态，导致不能正常瞄准"。

## 63.2 根因：客户端有四个"一票否决"，而且它自己解不开

`client/handler/AimingHandler.java`：

- `isAiming()` 第 216 行：`ModSyncedDataKeys.RELOADING` 为 true ⇒ 返回 false ✓；
- `isAiming()` 第 233 行：物品 NBT `scguns:ReloadState` **非空且不是 `"NONE"`** ⇒ 返回 false ✓；
- `onClientTick()` 第 131 行：`InCriticalReloadPhase` 为 true 且换弹类型非 MANUAL ⇒ **每 tick 把 aiming 强制置 false** ✓（这就是"一直取消"的观感来源 ✓）；
- 而 `onClientTick()` 第 108 行：清理逻辑**只在** `!isReloading && !inCriticalPhase` 时执行 ✓ ⇒ 一旦卡住，它永远清理不了自己 ✓（**自锁** ✓）。

两条把状态留下的路径（`common/ReloadTracker.java`）：

1. **换弹结束分支**（弹匣满 / 没子弹，第 436–444 行）：清了 `IsReloading` ✓、写下 `ReloadState="STOPPING"` ✓，却**没有**清 `InCriticalReloadPhase` ✗；
2. **else 分支**（当前没在换弹，第 447–453 行）：只清 `IsReloading` ✗，完全不管 phase ✗。

⇒ 只要换弹**不是**经由 `C2SMessageGunLoaded` 结束的（那条路会清 phase ✓，见其第 51/67 行 ✓）—— 例如动画没走到装填点、被打断、切枪、
弹匣因其它原因变满 —— `InCriticalReloadPhase` 就**永久**留下 ✓ ⇒ 客户端每 tick 取消开镜 ✓ ⇒ 玩家再也开不了镜 ✓✓。

**`grep` 对比 0.5.5**：`ReloadTracker` 这两处与 0.5.5 **逐字相同** ✓（含第 442 行 `STOPPING` 那段 ✓）⇒ 属**上游一直存在的缺陷** ✓，
不是本次移植引入的 ✓。

## 63.3 修法（三处，全部在服务端 = 权威侧）

1. `ReloadTracker` 换弹结束分支：补 `tag.remove("InCriticalReloadPhase")` ✓。
2. `ReloadTracker` else 分支：**无条件**清 `InCriticalReloadPhase` ✓。依据：**没有任何代码路径会给 MANUAL 枪设这个标记** ✓
   （`C2SMessageReload` 第 60 行 ✓ 与 `ReloadHandler` 第 284 行 ✓ 都带 `!= MANUAL` 守卫 ✓）⇒ 没换弹时它必然是残留 ✓。
   这一条同时**自愈**已经写进存档的坏状态 ✓。
3. 新增**陈旧停止态看门狗** ✓：`scguns:ReloadState` 非 NONE ✓ 且 `IsPlayingReloadStop`/`ShouldStopAfterLoop` 仍在 ✓、
   同时没有任何换弹在跑 ✓ ⇒ 连续 **100 tick（5 秒）** 后清掉 `ReloadState`/`IsPlayingReloadStop`/`ShouldStopAfterLoop`/
   `StopAfterLoopTime`/`IsReloading`/`InCriticalReloadPhase` ✓，并打一条 **WARN** ✓（便于日后定位剩余源头 ✓）。
   5 秒远大于停止动画（约 1 秒 ✓）⇒ 不会误杀正常动画 ✓。

> 为什么三处都必须在**服务端**：客户端读的 `RELOADING` 是服务端同步过来的 ✓，物品 NBT 也以服务端为准 ✓
> ⇒ 只有服务端清掉才会同步到客户端 ✓（§59 的教训：客户端自己的修改会被同步覆盖 ✓）。

## 63.4 实测（临时探针 + 真 `PlayerTickEvent.Pre`，驱动模组自己的 `ReloadTracker.onPlayerTick`；探针已删除）

```
mk43_rifle      stuck-before: critical=true  isReloading=true
mk43_rifle      after-3-ticks: critical=false isReloading=false          ← 3 tick 内自愈 ✓
mk43_rifle      stop@50:  reloadState='STOPPING' playingStop=true        ← 50 tick 时仍在 ✓（不误杀动画 ✓）
WARN [scguns-reload]: Clearing a reload state that outlived its reload: item=scguns:mk43_rifle state=STOPPING after 100 ticks
mk43_rifle      stop@110: reloadState='' playingStop=false               ← 看门狗清掉 ✓
```

同一探针在 `flintlock_pistol` ✓ 与 `venturi`（**MANUAL** 换弹 ✓）上结论完全相同 ✓。
第一轮实测还发现 `venturi` 最初**仍被卡住** ✗ ⇒ 去掉 `!= MANUAL` 守卫后三把全过 ✓（这条修正本身就是被实测逼出来的 ✓）。

门禁：`build` ✓、21 个审计全过 ✓、`verify_installed_jar` **149/149** ✓（新增两项：`ReloadTracker` 会清 phase ✓、
陈旧停止态会被清理并记日志 ✓）、探针不在 jar 里 ✓、已安装 ✓（备份 `.bak-153517` ✓）。

## 63.5 边界（未验证 / 需要玩家）

- **真人客户端链路没实测** ✗：修的是"阻塞开镜的那两个标记不再残留" ✓（客户端读的正是它们 ✓），
  但"换弹后立刻再开镜的手感"必须真机确认 ✓。
- 若**仍然**出现 ✓：日志里会出现 `[scguns-reload] Clearing a reload state that outlived its reload: …` ✓
  （说明是看门狗兜住的 ✓）—— 把它发我 ✓，按 `state=`/`item=` 就能定位剩下的写入者 ✓。
- 未验证"换弹过程中**故意**不能开镜"是否仍然保持 ✓（那是设计行为 ✓；本次只清**没有换弹在跑**时的残留 ✓）。
- 未排查其它模组是否也写同一批自定义 NBT 键 ✗（概率极低 ✓）。

# 64. 开镜不改变鼠标灵敏度 —— 移植时注入点从"参数"退化成了"猜一个局部变量"

## 64.1 玩家报告

"开镜后不会修改鼠标灵敏度"。

## 64.2 事实（直接读 1.21.1 原版源码 `.refs/nf-src/net/minecraft/client/MouseHandler.java`）

```java
298: private void turnPlayer(double movementTime) {
299:    var event = ClientHooks.getTurnPlayerValues(options.sensitivity().get(), options.smoothCamera);
300:    double d2 = event.getMouseSensitivity() * 0.6F + 0.2F;
301:    double d3 = d2 * d2 * d2;
302:    double d4 = d3 * 8.0;
...
329:    this.minecraft.player.turn(d0, d1 * (double)i);   // ← 灵敏度最终就是在这行被用掉的
```

⇒ 1.21.1 的 `turnPlayer` **带一个 double 参数** ✓（1.20.1 是无参 ✓），而真正决定"转多少"的是第 329 行
`player.turn(...)` 的**两个实参** ✓。

## 64.3 根因

- 0.5.5：`@ModifyVariable(method = "turnPlayer()V", at = @At(value = "STORE", opcode = 57), ordinal = 2)` ✓；
- 我们的移植版：把 method 名字改成了 `turnPlayer` ✓（这一步是对的 ✓），但 **`ordinal = 2` 原样留着** ✗ ——
  这个 ordinal 是按"第 N 个 double 局部变量/存储"数的 ✓，而我们文件里的注释自己也只写着"有 7 个 DSTORE，**所以 ordinal 2 存在**" ✓
  —— 只是"存在"✓，**从没验证过它是不是那个会被用掉的量** ✗。1.21.1 这个方法里有 `d2/d3/d4/d5/d6/d0/d1` 一堆 double ✓
  （源码 300–319 行 ✓），改中哪个都不保证作用到第 329 行 ✓ ⇒ 注入确实生效 ✓、客户端也照常启动 ✓，
  但**灵敏度看起来毫无变化** ✓ —— 正是玩家看到的现象 ✓。
- 参考 1.21.1 移植版（同一个 MC / NeoForge 版本 ✓）用的是 `@ModifyArg` 打在第 329 行**调用点的两个实参**上 ✓ ——
  也就是最终真被用掉的那两个值 ✓。

## 64.4 改动（`src/main/java/top/ribs/scguns/mixin/client/MouseHandlerMixin.java`）

- 两个 `@ModifyArg` ✓：`method = "turnPlayer(D)V"` +
  `at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")` ✓，
  `index = 0`（yaw ✓）与 `index = 1`（pitch ✓，注意它带 invertY 的 `* i` ✓）；
- 乘数抽成 `scguns$getAimingSensitivityMultiplier()` ✓，**数值照 0.5.5 原样** ✓：
  `(1 - (1 - aimDownSightSensitivity) * progress) * clamp(modifier^0.25, 0.5, 1)` ✓（有 zoom 模块时 ✓）。

## 64.5 验证（能无头测的都测了）

- `tools/audit_mixins.py` 会把 `@At` 目标**解析到 `.refs/nf-src` 里真正的 1.21.1 源码** ✓（`check_at_target` ✓，
  解析不到就报 `BROKEN`/`UNRES` ✓）⇒ 改完后仍是 **0 target drift / 0 missing injection point / 0 unresolved target** ✓✓
  —— 也就是说"`turnPlayer(D)V` 里有一个 `LocalPlayer.turn(DD)V` 调用"这件事被 1.21.1 源码证实了 ✓（64.2 引的就是它 ✓）。
- `build` ✓、21 个审计全过 ✓、`verify_installed_jar` **149/149** ✓、已安装 ✓（备份 `.bak-154333` ✓）。

## 64.6 边界与"怎么一眼看出有没有生效"

- **真机手感无法无头验证** ✗：这是纯客户端 mixin ✓，只能由玩家确认 ✓。
- 配置默认 `aimDownSightSensitivity = 0.75` ✓（玩家实例里也是 0.75 ✓，见 `config/scguns-client.toml` ✓）
  ⇒ 满开镜时灵敏度只降 **25%** ✓，本来就**很轻微** ✗。要一眼看出效果 ✓：把它调到 **0.2~0.3** ✓ 再对比开镜前后 ✓。
- 配置注释给出的位置是 `Options > Controls > Mouse Settings > ADS Sensitivity` ✓（本模组自己的设置界面 ✓）。
- 未验证：装了瞄具（有 zoom 模块 ✓）时 `additionalAdsSensitivity` 的 `clamp(…, 0.5, 1)` 区间是否体感合理 ✓
  （数值照 0.5.5 ✓，未改 ✓）。

# 65. 充能枪"不能开火" + 单发装填"装满还一直装" —— 实测到什么、查出什么、还缺什么

## 65.1 玩家报告（两条）

1. "充能射击模式的枪械无法正常开火"；
2. "单发装填类武器在换弹时会一直装弹，即使弹容已经装满了"。

## 65.2 先把"数据/服务端"这一半测干净（临时探针 + FakePlayer，已删除）

充能枪（`FireMode.PULSE` ✓）共 **7 把** ✓：`gale` `gauss_rifle` `hullbreaker` `nervepinch` `pyroclastic_flow`
`teslock_rifle` `venturi` ✓。单发装填（`ReloadType.single_item` ✓）共 **11 把** ✓：`basker` `blasphemy`
`cr4k_mining_laser` `flayed_god` `inquisitor` `laser_musket` `minksy` `pyroclastic_flow` `shard_culler`
`spitfire` `waltz_conversion` ✓。

```
PULSE scguns:gale          fireMode=true fireTimer=23 hasAmmo=false canShoot=true projectile=scguns:hardened_bullet
PULSE scguns:gauss_rifle   fireMode=true fireTimer=25 hasAmmo=false canShoot=true projectile=scguns:energy_cell
PULSE scguns:venturi       fireMode=true fireTimer=20 hasAmmo=false canShoot=true projectile=scguns:buckshot
PULSE scguns:teslock_rifle fireMode=true fireTimer=20 hasAmmo=false canShoot=true projectile=scguns:shock_cell

SINGLE scguns:basker   reloadItem=scguns:blaze_fuel   capacity=15 ammo=-1 full=false
SINGLE scguns:basker   load#1 -> ammo=15 full=true   ← 一次装填就装满 ✓（"单发装填"= 每次消耗 **1 个**装填物 ✓
SINGLE scguns:spitfire reloadItem=scguns:blaze_fuel   capacity=32 … 一次到 32 ✓  而不是 1 发 ✓）
```

⇒ 结论（服务端这一半是干净的 ✓）：
- **充能枪的数据没问题** ✓：`fireMode` ✓ 与 `fireTimer`（充能时长 20–25 tick ✓）都在 ✓（`venturi.json` 里就是
  `fireMode: "scguns:pulse"` + `fireTimer: 20` ✓）；客户端 `canFire()` 的充能门槛是 `ChargeProgress > 0` ✓
  （`ShootingHandler.canFire` ✓ / `ChargeHandler` ✓，且**创造模式直接放行** ✓）。
- **单发装填在服务端不会"装满还装"** ✓：`ReloadTracker.reloadItem` 是
  `if (currentAmmo < maxAmmo) { AmmoCount = maxAmmo; 消耗 1 个装填物; }` ✓ ⇒ **装满时什么都不做** ✓
  （既不涨弹也不消耗物品 ✓）✓。⇒ 玩家看到的"一直装"不是服务端在装 ✗，而是**客户端**那边的循环/动画状态没收住 ✓。

## 65.3 查出的一个真不一致（已修）

`ReloadTracker.isWeaponFull` ✓、`reloadItem`/`increaseMagAmmo`/`increaseAmmo` ✓、服务端开火 ✓、
`AnimatedGunItem` 的换弹状态机 ✓、HUD ✓、物品提示 ✓、按住换弹键的判断 ✓ —— **全部**用
`GunModifierHelper.getModifiedAmmoCapacity(...)`（"被改装后的"弹容 ✓）；**只有**客户端自动换弹那处用的是数据里的原始值 ✗：

```java
// client/handler/ShootingHandler.java:375（修前）
int maxAmmo = gun.getReloads().getMaxAmmo();      // ← 原始弹容，与其它 20+ 处不一致
```

⇒ 一旦两者不相等（有模块/附件改了弹容 ✓），客户端与服务端对"满了没有"的判断就会不一致 ✓，而
**自动换弹循环永远不会认为弹匣已满 ⇒ 一直装** ✓（`if (hasAmmoAvailable && !isReloading && currentAmmo < maxAmmo)` ✓）。
已改成 `GunModifierHelper.getModifiedAmmoCapacity(heldItem, gun)` ✓，与服务端及其它所有判断统一 ✓。
（`grep` 对比：0.5.5 同一行也是 `getReloads().getMaxAmmo()` ✓ ⇒ 属**上游**的不一致 ✓，不是移植引入 ✓。）

门禁：`build` ✓、21 个审计全过 ✓、`verify_installed_jar` **149/149** ✓、探针不在 jar 里 ✓、已安装 ✓（备份 `.bak-155850` ✓）。

## 65.4 还缺什么（**未修**，需要玩家给一个关键现象）

两条症状的主体都在**客户端** ✗（我无法无头运行客户端 ✓；FakePlayer 只能覆盖服务端那一半 ✓）⇒ 不猜着改 ✗
（§48 的教训：没测到就动手，容易"修"掉一个本来正常的东西 ✗）。需要玩家回答：

1. **充能枪**：按住开火键时，HUD 上那条**充能条**会不会涨 ✓（`HUDRenderHandler.renderChargeBarHUD` ✓）？
   - 完全不涨 ⇒ 卡在充能阶段 ✓（`ShootingHandler` 476–496 行的 PULSE 分支 ✓ / `ChargeHandler.updateChargeTime` ✓）；
   - 会涨、但松手没反应 ⇒ 卡在 `fire()` 的充能门槛 ✓（`canFire` ✓）；
   - 会涨、松手也开火、只是没伤害 ⇒ 那是 `ChargeProgress` 的服务端同步 ✓（`C2SMessageChargeSync` ✓）。
2. **单发装填**：装满之后到底是哪种"一直装" ✓？HUD 弹数一直显示满 ✓ / 装填物被持续消耗 ✗（服务端实测不会 ✓）/
   只是循环动画一直在播 ✓。另外：**是不是装了改弹容的模块或附件**（那正好命中 65.3 那条 ✓）✓。

## 65.5 边界

- 服务端结论来自 FakePlayer 探针 ✓（走的是模组自己的 `ReloadTracker.reloadItem` ✓ / `isWeaponFull` ✓）；
  **客户端链路未实测** ✗。
- 65.3 的修复是"把不一致改一致" ✓，**没有**实测复现"装满还装" ✗ ⇒ 可能不是玩家这一例的根因 ✓（等 65.4 的回答 ✓）。
- 未排查"充能枪在创造模式下"的表现 ✓（`canFire` 在创造直接放行 ✓）。

# 66. 手动装填（温妮）装满了不收尾 → 上膛动画不播、所有枪都在"装弹"（两处根因，实测已修）

## 66.1 玩家第二轮报告（已锁定具体枪）

"我使用**温妮步枪**在装填满弹药后，装弹动画没有正常被上膛动画接替（其他物品栏内的枪械也进入了单发装填的动画）"。

上一轮我加的临时客户端诊断（`scguns-gunprobe`，已删除）正好记下了现场 ✓：
```
RELOAD-START path=KEY ammo=0 capacity=8 reloadType=ReloadType[id=scguns:manual]
```
⇒ **温妮的装填类型是 MANUAL** ✓（不是 `single_item` ✓）⇒ 走 `ReloadTracker` 的手动装填收尾 + `AnimatedGunItem` 的 MANUAL 状态机 ✓；
而客户端动画看的是**玩家级**的 `RELOADING` ✓ ⇒ 它卡住就"所有枪都在装弹" ✓✓。

## 66.2 根因一：收尾的 100ms 宽限期写进物品 tag 后**读不回来**（实测）

```java
if (!tag.getBoolean("scguns:ShouldStopAfterLoop")) { tag.putBoolean(…, true); return; }
long stopTime = tag.getLong("scguns:StopAfterLoopTime");
if (stopTime == 0L) { tag.putLong("scguns:StopAfterLoopTime", System.currentTimeMillis()); return; }
if (System.currentTimeMillis() - stopTime < 100L) { return; }
```

FakePlayer 探针（先把弹匣装满并置 `ShouldStopAfterLoop=true`）逐 tick 打印：

```
TICK full=true noAmmo=false animated=true stopAfterLoop=true stopTime=0 …
```

⇒ `StopAfterLoopTime` **每一 tick 都是 0** ✓ —— 刚写进去、下一 tick 读回来就没了 ⇒ `stopTime == 0L` 永远成立 ⇒
**收尾代码不可达** ✓ ⇒ `RELOADING` 永远 true ✓ ⇒ 手动装填循环不停、上膛（stop）动画不播 ✓、其它枪也一起"在装弹" ✓。

**修法**：把这个宽限期从物品 tag 挪进 tracker **内存**（`pendingManualStopSince` ✓），不再依赖会丢的 tag 键 ✓。

> 测量教训（记一笔）✓：第一次改完"看起来没生效" ✓ —— 实际是我的探针在**几毫秒内**连发 20 次 `PlayerTickEvent.Pre` ✗，
> 100ms 的宽限期不可能过去 ✓。改成每 tick `Thread.sleep(15)` 后 ✓，收尾立刻出现（`TICK completing reload, clearing RELOADING` ✓）。

## 66.3 根因二：装填中把枪换掉/收起来 ⇒ `RELOADING` 永久卡住（实测）

"武器变了就换 tracker"的判断被 `!isActivelyReloading` 挡住 ✗：

```java
if (!isActivelyReloading && (tracker.slot != currentSlot || currentWeapon.isEmpty() || …)) { … }
```

⇒ 装填进行中切枪时**不换 tracker** ✗ ⇒ tracker 拿着**旧枪**的 `stack`/`gun` ✓，却用 `player.getMainHandItem()`（**新**物品 ✗）
算 `isWeaponFull`/`hasNoAmmo` ✓ ⇒ 既不满也没断弹 ⇒ 永远收不了尾 ✓；而"手上完全不是枪"时更彻底 ✗：整个
`if (heldItem.getItem() instanceof GunItem …)` 块被跳过 ✗ ⇒ 连分支都进不去 ✓。实测（修前）：

```
B after switching held=scguns:flintlock_pistol syncedReloading=true   ← 卡住
C empty hand held=minecraft:air syncedReloading=true                  ← 卡住
```

**修法**：① 武器变了（槽位/物品类别不同 ✓ 或手上为空 ✓）⇒ 调 `endReload(player, tracker)`：`RELOADING=false` ✓、
给**旧枪**清 `IsReloading`/`InCriticalReloadPhase`/`ShouldStopAfterLoop`/`StopAfterLoopTime` 并置
`ReloadState=STOPPING`+`IsPlayingReloadStop`（交给它的状态机正常收尾 ✓）、清掉内存宽限期 ✓；
② 手上不是枪时走同一个 `endReload` ✓（新增 `else` ✓）。

## 66.4 实测（临时探针 + FakePlayer + 真实 `PlayerTickEvent.Pre`，共三次；探针已全部删除）

```
A 装满弹匣   → TICK completing reload, clearing RELOADING ; 20 tick 后 syncedReloading=false ✓
B 换成另一把枪 → 40 tick 后 syncedReloading=false ✓
C 手上空     → 20 tick 后 syncedReloading=false ✓
```

门禁：`build` ✓、21 个审计全过 ✓、`verify_installed_jar` **151/151** ✓（新增两项：内存宽限期 ✓、`endReload` 存在 ✓）、
探针与诊断日志**全部删除**（`grep` 0 命中 ✓）、jar 内无 `Temp*Probe` ✓、已安装 ✓（备份 `.bak-163526` ✓）。

## 66.5 边界（未验证）

- **真机动画没实测** ✗：客户端"装弹动画 → 上膛动画"的接替必须由玩家确认 ✓（服务端侧的 `RELOADING` 与各项标记已实测会正常收尾 ✓）。
- "装填中途切枪"我选择**直接把这次装填结束掉** ✓ —— 合理，但属行为决定 ✓，未与 0.5.5 的意图逐条对齐 ✗。
- 温妮之外的其它手动/单发枪未逐把过一遍 ✗（逻辑共用 ✓，与 §65 的 11 把 `single_item` 清单可一起复测 ✓）。
- §65 的弹容一致性修复仍在 ✓ 未回退 ✓；两条报告可能重叠 ✓，请一并复测 ✓。

# 67. 对照其它移植版：装填收尾的真正根因是 **tag 写回没生效**（上游用 `setCustomData` 显式写回）

## 67.1 玩家建议

"看看其他移植版是怎么做的，可以查看相关代码" ✓ —— 于是把磁盘上现成的移植版逐个对照 ✓（都在 `E:\mod\SG2-1.21\` ✓）：

| 树 | MC / 加载器 | `ReloadTracker` 关键差异 |
|---|---|---|
| `ScorchedGunsNeoforge-main` | 1.21.1 / NeoForge 21.1.228 | 549 行 ✓ **没有** `isActivelyReloading` ✓、**没有** `ShouldStopAfterLoop`/`StopAfterLoopTime` ✓ |
| `ScorchedGuns-NeoForge-New` | 1.21.1 / NeoForge 21.1.228 | 535 行 ✓ 与 0.5.5 相同：有 `isActivelyReloading` 守卫 ✓ |
| `Scorched-Guns-1.20.1-master` | 1.20.1 / Forge | 535 行 ✓ 同上（0.5.5 原样 ✓） |
| 我们的移植 | 1.21.1 / NeoForge 21.1.250 | 现在 642 行 ✓ |

## 67.2 对照得到的三条结论

1. **"切枪/收枪就结束这次装填"** ✓：`ScorchedGunsNeoforge-main` 第 400–417 行在槽位或物品变了时直接
   `RELOADING=false` + `clearReloadData(tracker.stack)` + 清标记 + `STOPPING` + `S2CMessageStopReload` + `return` ✓
   —— 与 §66 的 `endReload(...)` **意图完全一致** ✓（我们的还多覆盖"手上完全不是枪"✓）。
2. **它把 0.5.5 的 100ms 收尾等待整个去掉了** ✓（第 431–442 行直接收尾 ✓）⇒ 那段
   `ShouldStopAfterLoop`/`StopAfterLoopTime` 舞蹈在新版里不存在 ✓ —— 与 §66 实测到的"**那个 tag 键写进物品后读不回来**"吻合 ✓。
3. **它对每次 tag 改动都显式写回** ✓：`getOrCreateCustomData(...)` 取 ✓ → 改 ✓ → `setCustomData(stack, tag)` 写回 ✓
   （见其第 228 / 245 / 267 / 289 行 ✓）—— **这正是我们缺的一环** ✓。

## 67.3 由此定位的**真正根因**（已修 + 实测）

`ReloadTracker.onPlayerTick` 改完 tag **没有写回** ✗（依赖 `NbtHelper.getOrCreateTag` 返回"就是该组件实例" ✓，
但实测那些写入**到不了物品** ✗）。临时探针（温妮装满 → 20 次真实 tick，每 tick `sleep(15ms)`）：

```
修前  before:      syncedReloading=true  reloadState=''          isPlayingStop=false isReloading=true
      after 20:    syncedReloading=true  reloadState=''          isPlayingStop=false isReloading=true  ← 收尾状态根本没进物品
修后  after 20:    syncedReloading=false reloadState='STOPPING'  isPlayingStop=true  isReloading=false ✓✓
```

⇒ 玩家看到的"**装弹动画不被上膛动画接替**"就是这条 ✓：收尾时写的 `ReloadState=STOPPING` + `IsPlayingReloadStop=true`
从来没到物品上 ✗ ⇒ 客户端的手动状态机看不到"该上膛了" ✓。现在两处写入都补了显式写回 ✓
（`NbtHelper.setTag(holder, tag)` ✓，等价上游的 `setCustomData` ✓）：`ShouldStopAfterLoop` 那处 ✓ 与收尾那处 ✓。

## 67.4 我们与其它移植版的差异（有意保留）

1. **§65 弹容一致** ✓：三个移植版在客户端自动换弹里都用**原始**弹容 ✗ —— 这是真 bug ✓：`GunModifiers.PLUS_P_MAG` 把弹容
   **×0.5** ✓（`GunModifiers.java` 第 224 行 ✓），而服务端按**改装后**弹容灌满 ✓ ⇒ 客户端"永远没满" ✗ ⇒ 自动换弹无限循环 ✓
   （正是 §65 描述的现象 ✓）。保留我们的修法 ✓。
2. **§66 收尾宽限期** ✓：上游新版直接删掉了 ✓；我们保留但改成**内存**计时 ✓（不依赖会丢的 tag 键 ✓，语义仍贴 0.5.5 ✓）。
3. **§66 "手上不是枪也收尾"** ✓：我们用 `else` 覆盖 ✓（上游靠另一条早退路径 ✓）。

## 67.5 门禁

`build` ✓、21 个审计全过 ✓、`verify_installed_jar` **152/152** ✓（新增：`ReloadTracker` 会写回 tag ✓）、
临时探针**已删除** ✓（`Temp*.java` 计数 0 ✓）、已安装 ✓（备份 `.bak-164613` ✓）。

## 67.6 边界与教训

- **真机动画仍需玩家确认** ✓：服务端已实测把 `STOPPING`/`IsPlayingReloadStop` 写进物品 ✓ 且 `RELOADING` 归零 ✓。
- `NbtHelper.getOrCreateTag` 为什么"写了不生效"**没查到机制层面** ✗ —— 只确定现象（实测 ✓）与上游的对策（显式写回 ✓）。
  记一条规矩 ✓：**这类 tag 改动一律显式写回** ✓（`NbtHelper.setTag(stack, tag)` ✓），别指望"拿到的一定是组件里那个实例" ✓。

# 68. "背包里的其它枪也进装填动画" —— 收尾时把 STOPPING **留在被收起来的枪上**（实测已修）

## 68.1 玩家第三轮报告

"背包内的其他枪械也会进入装填动画的bug还是没有修复"。

## 68.2 根因：§66 的 `endReload` 写错了方向

§66 我给"枪离开手"写的 `endReload` 是照抄**正常收尾**的写法 ✓：给那把**被收起来的**枪设
`scguns:ReloadState=STOPPING` + `scguns:IsPlayingReloadStop=true` ✗。但枪既然不在手上 ✓，
它的状态机（`AnimatedGunItem` 的手动装填机 ✓）**根本不会运行** ✗ ⇒ 这两个标记就一直挂着 ✓
⇒ 之后玩家再拿出/看到这把枪时 ✓，它就照着 `ReloadState` 播装弹/收尾动画 ✓✓ —— 正是"背包里的其它枪也进装填动画" ✓✓。

对照上游 ✓：`ScorchedGunsNeoforge-main` 在同样情形下走的是 `clearReloadData(tracker.stack)` ✓ —— **清掉**旧枪的装填数据 ✓，
而不是给它留状态 ✓。方向搞反了 ✓。

## 68.3 改动

1. `ReloadTracker.endReload(...)` ✓：把 `putString(ReloadState, "STOPPING")` + `putBoolean(IsPlayingReloadStop, true)`
   换成**清空**（`IsReloading` / `scguns:IsReloading` / `InCriticalReloadPhase` / `scguns:ShouldStopAfterLoop` /
   `scguns:StopAfterLoopTime` / `scguns:ReloadState` / `scguns:IsPlayingReloadStop` / `InReloadLoop` /
   `LastReloadStateChange`）✓，并显式写回 ✓。
2. **自愈**（顺带修好已经被写坏的存档 ✓）：`AnimatedGunItem.inventoryTick` 里，当这把枪**不在选中槽**且客户端不成立时 ✓
   清掉它的装填残留 ✓（新方法 `clearStaleReloadState(stack)` ✓）。
   - 判"在不在手上"必须用**槽位**（`slot == player.getInventory().selected` ✓）：原先那句用
     `GeoItem.getId(mainHand) == GeoItem.getId(stack)` ✗ —— 两把**都还没分配 GeckoLibID** 的枪 id 都是 0 ✓
     ⇒ 会把背包里的枪误判成"正握在手里" ✗ ⇒ 自愈从来不触发 ✓（实测发现 ✓）。
   - 该方法**单独成一个方法** ✓：取 tag → 改 → 写回必须不跨着别的局部 tag 变量 ✓，
     否则写回会把那个局部变量作废、它后面的写入全部丢失 ✓ —— `audit_nbt_write_alias.py` 正是抓这个 ✓
     （它当场抓到了我第一版 ✓：`AnimatedGunItem.java:136 局部 tag 在 157 行 setTag 之后仍被使用` ✓，
     **这条是审计替我发现的真问题** ✓，谢谢它 ✓）。

## 68.4 实测（临时探针 + FakePlayer，已删除）

```
A 装填中把枪移到另一格、选另一把枪 → 该枪（非选中槽）inventoryTick 后：
     isReloading=false  reloadState=''  isPlayingStop=false  （syncedReloading=false ✓）
B 旧存档遗留（IsReloading + STOPPING + IsPlayingReloadStop + InCriticalReloadPhase）→ 全部清空 ✓
C 对照：把枪**握在手里**装填中（ReloadState=LOADING）→ **保持不动** ✓（自愈不会误伤正常装填 ✓）
```

门禁：`build` ✓、21 个审计全过 ✓（含 `audit_nbt_write_alias` ✓）、`verify_installed_jar` **153/153**
（新增"非选中槽的枪会清理装填状态" ✓）、临时探针 0 个 ✓、已安装 ✓（备份 `.bak-170244` ✓）。

## 68.5 边界（未验证 / 需玩家）

- **真机动画仍要玩家确认** ✓（服务端侧已实测：离开手的枪不再带装填状态 ✓，握着装填的枪不受影响 ✓）。
- 躺在**箱子**里的枪不会跑 `inventoryTick` ✗（那不是玩家背包 ✓）⇒ 旧存档里箱子中的枪仍带旧标记 ✓；
  取到背包（非选中槽 ✓）或握在手里后 ✓ 会由自愈/客户端状态机清掉 ✓，但"从箱子直接拿到**选中槽**"这一条
  我没实测 ✗ —— 若真机上仍见到某把枪一拿出来就播装弹动画 ✓，把那把枪先放进背包另一格再拿 ✓ 即会自愈 ✓，也请告诉我 ✓。
- `endReload` 里的清空写入是否真的生效 ✗：实测显示"切枪后立刻"那把枪**还带着** `IsReloading` ✓（写回又没落地 ✓），
  真正把它清掉的是**自愈** ✓ ⇒ 结论不变（结果正确 ✓），但 §67 那条"写回不落地"的现象依然存在 ✓，未查到机制 ✗。

# 69. 【换弹动画的真凶】背包里的枪**真的**在换弹 —— GeckoLib 4.6 换了 id 的存放位置，移植写了个没人读的键

玩家第四轮反馈：**"换弹时背包里其他的枪械也会进行换弹"** + 追加提示 **"背包里的其他枪械是真的在换弹"** ✓
—— §66/§67/§68 三轮都在治"装填状态残留"（服务端 ✓），但**这一条与那些无关** ✗：
它从头到尾是**客户端**的"哪把枪在手上"判断错了 ✓，而且**移植自己引入** ✗（0.5.5 不会 ✓）。

## 69.1 根因：`GeoItem.getId()` 在 GeckoLib 4.6 里读的是**数据组件**，我们写的是**NBT 键**

移植沿用 0.5.5 的写法给枪分配 GeckoLib 的 animatable id ✓（`AnimatedGunItem.inventoryTick`）：

```java
if (!nbtCompound.contains("GeckoLibID", 99) && world instanceof ServerLevel) {
   nbtCompound.putLong("GeckoLibID", AnimatableIdCache.getFreeId((ServerLevel)world));   // 0.5.5 的写法 ✗
}
```

而 GeckoLib 4.6 已经把它搬到了一个**数据组件**里 ✓。`javap -c software.bernie.geckolib.animatable.GeoItem`：

```
public static long getId(ItemStack);          // 读 STACK_ANIMATABLE_ID_COMPONENT（组件）
    ... orElse(Long.valueOf(9223372036854775807L))   // ← 组件缺失时回落到 Long.MAX_VALUE
public static long getOrAssignId(ItemStack, ServerLevel);   // 4.6 指定的分配入口
```

并且 **`GeckoLibID` 这个字符串在 GeckoLib 4.9.3 的 jar 里出现 0 次** ✓
（`tools/probe_geckolib_anim_id.py` 实测 ✓）⇒ **我们写的那一行从来没有任何东西读过** ✗
⇒ 全游戏**每一把枪**的 `GeoItem.getId(...)` 都是 `Long.MAX_VALUE` ✓✓。

后果（两步，一步比一步致命）：

1. **`handlePlayerSpecificLogic` 用 id 判断"这把枪是不是在手上"** ✗：
   ```java
   if (GeoItem.getId(player.getMainHandItem()) != id) { handleItemNotHeld(...); } else { /* 手持逻辑 */ }
   ```
   `MAX_VALUE != MAX_VALUE` 恒为 **false** ⇒ **背包里每一把枪都走"手持"分支** ✗✓
   ⇒ 而手持分支读的是**玩家级**的 `ModSyncedDataKeys.RELOADING` ✓（§63 的教训：这个键描述的是"玩家在换弹" ✗）
   ⇒ **玩家换弹时，背包里每一把枪都在跑换弹状态机** ✓✓。
2. **每把枪都有一个"controller"** ✓：`handleReloadingState` → `animationController.tryTriggerAnimation("reload"/"carbine_reload")` ✓；
   而 **triggered 动画会绕过 `predicate`** ✓（`javap -c AnimationController.handleAnimationState`：`triggeredAnimation != null`
   就直接 `setAnimation` 并返回 `CONTINUE` ✓，根本不问谓词 ✓）
   ⇒ 这些枪**被画到哪就播到哪** ✓ —— 背包 GUI 里的枪就是真的在演换弹动画 ✓✓。
3. 同一个 id 还意味着**同型号的所有枪共用一个 `AnimatableManager`/controller** ✓
   （`ContextBasedAnimatableInstanceCache.getManagerForId(long)` 是 `Long2ObjectMap` 按 id 取 ✓，
   我们用的 `SingletonAnimatableInstanceCache` 同理 ✓）⇒ 手持那把的动画状态会**串**给背包里同型号的那把 ✓。

## 69.2 实测（专用服务器 + FakePlayer，探针已删除）

临时探针（`ServerStartedEvent` ✓，读完已删 ✓）在装有枪的 FakePlayer 背包上直接驱动 `inventoryTick`：

```
[SCGUNS-ANIMID] MAX_VALUE=9223372036854775807
[SCGUNS-ANIMID] fresh   A=9223372036854775807 B=9223372036854775807 C=9223372036854775807   ← 修复前的真实取值
[SCGUNS-ANIMID] ticked  A=9 B=10 C=11                                                        ← 修复后：每把枪不同 ✓
[SCGUNS-ANIMID] direct getOrAssignId=11 now=11
[SCGUNS-ANIMID] patch={minecraft:custom_data=>{WasHeldLastTick:1b}, geckolib:stack_animatable_id=>11}
[SCGUNS-ANIMID] distinct=true noneAreMax=true
```

⇒ **id 真的被分配了、而且每把枪不同** ✓（组件名实测为 `geckolib:stack_animatable_id` ✓）；
组件本身在 GeckoLib 里是 `.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG)` ✓
（`javap -c GeckoLibConstants.lambda$static$0` ✓）⇒ **会同步到客户端** ✓，
这也解释了 GeckoLib 为什么要用两个 mixin 让 `ItemStack` 比较**忽略**这个 id ✓
（`ItemStackMixin.geckolib$skipGeckolibIdOnCompare` / `LivingEntityMixin.geckolib$allowLazyStackIdParity` ✓：
id 值一模一样地在两端存在 ✓，不忽略就会把自己搞成"永远不同步" ✓——正是 §16 那个坑 ✓）。

> 环境事实（记下来，省得再摸一次）：**`FakePlayer.tick()` 是空实现** ✗
> （`javap -p net.neoforged.neoforge.common.util.FakePlayer` → 它覆写了 `tick()` ✓）。
> 第一次探针用 `player.tick()` 驱动，结果什么都没发生、差点被误读成"API 不生效" ✗；
> 改成**直接调 `stack.inventoryTick(level, player, slot, selected)`** ✓ 才拿到读数 ✓（§60.4 也是这么做的 ✓）。

## 69.3 修法（四处，全部在 `item/animated/AnimatedGunItem`）

| # | 改动 | 理由 |
|---|---|---|
| 1 | 用 **`GeoItem.getOrAssignId(stack, serverLevel)`** 取代那行死 NBT 写入 ✓ | 这是 GeckoLib 4.6 指定的入口 ✓；它自己检查组件是否存在、按需分配 ✓（实测见 69.2 ✓） |
| 2 | `inventoryTick` 里按**格子**算"在不在手上" ✓：`selected \|\| mainHand == stack \|\| offHand == stack` ✓，并把 `inHands` / `justLeftHands` 传下去 ✓ | id 不能回答这个问题 ✗（组件缺失时所有枪同号 ✓；客户端也可能还没拿到 ✓） |
| 3 | `handlePlayerSpecificLogic` 改成 **`if (!inHands)`** 分流 ✓ | 背包里的枪回到 `handleItemNotHeld` ✓，不再跑手持状态机 ✓ |
| 4 | `handleReloadStateSynchronization` 里 **`if (!inHands)` 先清掉残留再 return** ✓ | 玩家级 `RELOADING` 只描述手上那把枪 ✓；以前它把 `scguns:IsReloading` 写进**每一把**枪 ✓（那些幻影标记是背包枪"看起来在装填"的燃料 ✓） |
| 5 | `handleItemNotHeld` 的**动画重置**改成只在"这一 tick 刚离手"时执行 ✓（`justLeftHands` ✓） | 0.5.5 每 tick 无条件 `forceAnimationReset()` ✓；在"还没有 id"的窗口里 ✓ 那会**打断手上那把同型号枪**的动画 ✗（自己给自己造的新 bug ✓，靠这条堵掉 ✓） |

顺带的好处 ✓：`drawTick`（**实例字段** ✓，同型号所有枪共享 ✓）以前被背包里每一把枪各加一次 ✓（13 把枪 = 13 倍速抽枪动画 ✓），
现在只有手上那把会加 ✓。

## 69.4 防复发

* 新增审计 **`tools/audit_gun_animation_identity.py`** ✓（含 `--selftest` ✓）：要求 ① 出现 `GeoItem.getOrAssignId(` ✓、
  ② 不出现 `"GeckoLibID"` / `AnimatableIdCache.getFreeId` ✓、③ `inventoryTick` 用槽位算手持 ✓、
  ④ `handlePlayerSpecificLogic` 按 `inHands` 分流且**不再**用 `GeoItem.getId(...) !=` ✓、
  ⑤ `handleReloadStateSynchronization` 有 `!inHands` 守卫 ✓、⑥ `handleItemNotHeld` 有 `justLeftHands` 门 ✓，
  并扫全树不得再出现那个死键 ✓。
  **自测** ✓：对 `git show HEAD:` 的修复前源码跑 ⇒ **7 条问题全部命中** ✓（`selftest OK` ✓）；当前源码 **0** ✓。
* `verify_installed_jar` 新增 **4 项** ✓（`getOrAssignId` 在包里 ✓、死键不在包里 ✓、
  `handleItemNotHeld` 的**新描述符**（多一个 `Z` ✓）在包里 ✓、全树无其它地方写死键 ✓）。
  **自测** ✓：对修复前的 jar（`.bak-173518` ✓）跑 ⇒ **那 4 项全 FAIL（153/157）** ✓，新 jar **157/157** ✓。
  （第一版我写的第 3 项是"类里出现 `WasHeldLastTick`" ✗ —— §68 早就引入过这个字符串 ⇒ **新旧 jar 都通过** ✗，
  按 §23.4/§25.4 的规矩**换成能失败的那一项**（新方法描述符 ✓）✓。）
* 新增两个只读探针工具（复用价值）：`tools/probe_geckolib_anim_id.py`（列出 GeckoLib 里 id 的读写者 ✓）、
  `tools/scan_jar_refs.py <jar> <字符串...>`（扫任意 jar 的常量池 ✓）。

## 69.5 验收与边界

* `javac` 0 错误 ✓、`gradlew build` 成功 ✓、**21 个审计全 0** ✓、
  `verify_installed_jar` **157/157** ✓、临时探针**已删除**（`tmpprobe` 包已整个移除 ✓，`src` 下 0 个 `*Probe` ✓）、
  已安装 ✓（上一版备份 `.bak-173518` ✓）。
* **已实测** ✓：id 分配（69.2 的读数 ✓）、每把枪不同 ✓、组件进物品 ✓、全部门禁自测 ✓、
  服务端启动 `Done (1.101s)` ✓。
* **未实测（需玩家客户端）** ✗：**换弹时背包里的枪不再跟着演动画** ✓ —— 这是纯客户端的表现 ✓，
  必须真机确认 ✓。判据很明确 ✓：**只换弹手上那把枪** ✓，背包里同型号与不同型号的枪都应保持静止 ✓；
  另外顺带可看两处曾经被这个 bug 影响的：**抽枪动画速度**（以前同型号枪越多越快 ✓）与
  **扔在地上的枪**（以前也会走手持分支 ✓，因为 id 全都相同 ✓）。
* 与前三轮的关系 ✓：§66（手动装填收尾）、§67（tag 写回）、§68（离开选中槽清状态）修的是**服务端状态** ✓，
  本条修的是**客户端"谁在手上"** ✓，两者互相独立 ✓ —— 也就是说**这一条才是玩家看到的那个动画** ✓；
  三轮的服务端修复仍然有效、必须保留 ✓。

# 70. 把 NeoForge 前置版本从 21.1.249 降到 **21.1.150**（并因此修掉一个"老版本必崩"的真 bug）

玩家要求："neoforge 的前置版本可以放低点，因为很多人没有去更新 neoforge 版本" ✓。

## 70.1 最低能低到哪：**21.1.150 是硬极限**（不是我们说了算）

我们声明的 `neoforge` 范围只是**一半** ✓ —— NeoForge 会**逐个检查每个 mod** 自己的范围 ✓，
任何一个必需依赖不满足，游戏在加载阶段就拒绝 ✗。`tools/check_neoforge_floors.py`（新工具 ✓）读每个 jar 的元数据实测：

| 必需依赖 | 自己声明的 neoforge 范围 | 类型 |
|---|---|---|
| **geckolib 4.9.3** | **`[21.1.150,)`** | required |
| framework 0.13.11 | `[21.1,)` | required |
| curios 9.5.1 | `[21.1.60,)` | required |

⇒ **21.1.150 就是下限** ✓：再低 NeoForge 会拒绝加载 GeckoLib ✗，本 mod 无论声明什么都跑不起来 ✓。
（装了才生效的联动模组门槛更高 ✓，但那是**它们自己**的要求 ✓：Create 6.0.10 `21.1.219` ✓、
Sable / Create Aeronautics `21.1.228` ✓、FarmersDelight `21.1.219` ✓、Create New Age `21.1.209` ✓、
Mekanism `21.1.194` ✓、IE `21.1.164` ✓。）

**改动只有一行** ✓：`gradle.properties` 的 `neo_version_range` `[21.1.249,)` → **`[21.1.150,)`** ✓
（这一行会原样进 jar 的 `neoforge.mods.toml` ✓）。`neo_version`（dev/编译目标）**保持 21.1.249** ✓ ——
dev 运行要加载那些联动模组，而它们不接受更老的 NeoForge ✓（另加了 `-PnoIntegrationRuntime` 开关 ✓，见 70.4 ✓）。

## 70.2 降版本**当场抓出一个真 bug**：`ModCapabilities` 挂错了总线（老 NeoForge 必崩）

专用服务器第一次在 21.1.150 上起就炸在加载阶段 ✓：

```
FATAL AutomaticEventSubscriber: Failed to register class Ltop/ribs/scguns/init/ModCapabilities;
Caused by: java.lang.IllegalArgumentException: IModBusEvent events are not allowed on the common
           NeoForge bus! Use a mod bus instead.        (neoforge@21.1.150)
```

根因是**新旧 FML 对"谁负责选总线"的分工不同** ✓（两边字节码都核过 ✓）：

| FML | `AutomaticEventSubscriber` 的做法 |
|---|---|
| **4.0.38**（NeoForge 21.1.150） | 读注解上的 `bus`（默认 **GAME** ✓），把**整个类**注册到那一个总线 ✓ ⇒ 类里只要有 `IModBusEvent` 监听器就**硬失败** ✗ |
| **4.0.44**（NeoForge 21.1.249） | 按**每个监听器的事件类型**分别路由 ✓（`Subscribing ... to the mod event bus of mod {}` ✓、甚至 `Found mix of game bus and mod bus listeners in @EventBusSubscriber class {}, registering them separately` ✓） |

而 `RegisterCapabilitiesEvent` 在**两个版本里都是** `implements IModBusEvent` ✓（`javap` 核对 ✓），
`ModCapabilities` 却写的是 `@EventBusSubscriber(modid = "scguns")`（默认 GAME ✗）⇒
**新版本自动兜住、老版本直接崩** ✓✓ —— 这类问题只有真的在低版本上跑才会暴露 ✓，这正是本次降版本的价值 ✓。

**修法**：`bus = EventBusSubscriber.Bus.MOD` ✓（一行 + 注释说明为什么不能省 ✓）。

**防复发**：新增审计 **`tools/audit_subscriber_bus.py`** ✓ —— 解析每个 `@EventBusSubscriber`
（含默认 bus ✓、`value = Dist...` ✓）与它**自己类里**的 `@SubscribeEvent` 方法 ✓，
把事件类型在 NeoForge 合并 jar 里**沿继承链**解析成"是不是 `IModBusEvent`" ✓（自带最小 class 解析器 ✓），
两边不一致就报错 ✓。实测：**84 个订阅类里只报出 `ModCapabilities` 一条** ✓（修复后 0 ✓）。

> 写它的两个坑（都踩了 ✓）：① 索引 jar 时用 lambda 延迟读字节 → zip 已关闭 ⇒ **所有 NeoForge 事件类型都变成 None** ✗
> ⇒ 改成**立即读取** ✓；② 嵌套事件（`ModelEvent.ModifyBakingResult` ✓、`PlayerXpEvent.PickupXp` ✓）
> 在 class 文件里是 `Outer$Inner` ⇒ 必须同时按**内层简单名**建索引 ✓；
> 另外生命周期事件在 FancyModLoader 的 jar 里 ✓ ⇒ 从 `build-logs/compile-classpath.txt` 把 jar 补进索引 ✓。

## 70.3 验证（**编译与启动都在 21.1.150 上真跑过**）

新工具 **`tools/check_neoforge_api_floor.py`** ✓ 把三件事串起来，可重复运行：

1. 读 `neo_version_range` 的**下限** ✓，并断言它**不低于**必需依赖自己的下限 ✓
   （否则就是给了一个做不到的承诺 ✓）；
2. `gradlew compileJava -Pneo_version=<floor>` ✓ ⇒ **整套源码（含女仆兼容）对着 21.1.150 编译成功** ✓
   （`BUILD SUCCESSFUL` ✓，产物 `build/moddev/artifacts/neoforge-21.1.150-merged.jar` ✓）；
3. `--run`：在该版本上启专用服务器 ✓ ⇒ 实测 **`Done (0.958s)! For help, type "help"`** ✓、
   `scguns.mixins.json` 的 3 个 common mixin **注入成功** ✓、能力注册通过 ✓、无 mod 相关 ERROR/FATAL ✓。

⇒ **这份源码确实能在 NeoForge 21.1.150 上加载并运行** ✓（不是只把数字改小 ✓）。

## 70.4 两个环境坑（都让 floor 运行白跑过）

1. **dev 运行加载的是 `run/mods/scg2_maid_compat-neoforge-1.21.1-1.0.8.jar`** ✓ —— 女仆兼容不是源集 mod ✓，
   那个**拷贝**才是 dev 里被加载的东西 ✓，而 `gradlew build` **不会刷新它** ✗。
   它一直写着 `[21.1.249,)` ✓ ⇒ floor 运行**连续 3 次**都报
   `Mod scg2_maid_compat requires neoforge 21.1.249 or above` ✗（我一度误判成"源集资源陈旧" ✗，
   删 `maid-compat/build/{resources,generated}` 无用 ✓ —— 那是另一个副本 ✓）。
   现在 `--run` 会**先刷新那个拷贝** ✓（并打印刷新前后的范围 ✓）。
2. **新增可复用开关 `-PnoIntegrationRuntime`** ✓：让联动模组**不进 dev 运行** ✓ ——
   它们自己的 NeoForge 门槛比 floor 高 ✓，在 21.1.150 上加载不了 ✓
   （这正是"只装必需前置的玩家"的真实情形 ✓）。

## 70.5 验收与边界

* `javac` 0 错误 ✓、`gradlew build` ✓、**22 个审计全 0** ✓（新增 `audit_subscriber_bus` ✓）、
  `verify_installed_jar` **160/160** ✓（新增 3 项：host 声明的 floor ✓、**嵌套女仆 jar** 的 floor ✓、
  `ModCapabilities` 命名了 MOD 总线 ✓）。**自测** ✓：对上一版 jar（`.bak-182549` ✓）跑 ⇒
  **这 3 项全 FAIL（157/160）** ✓。已安装到实例 ✓。
* **floor 运行里的两类 ERROR 都不是本 mod 的** ✗（如实记录，免得被当成回归 ✓）：
  1. `ChunkSerializer ... Recoverable errors` 与 `immersiveengineering:wire_network` attachment ✓ ——
     这个 dev 世界是在**装了联动模组**时存的 ✓，本次故意不带联动 ⇒ 读旧区块自然缺内容 ✓；
  2. `Parsing error loading recipe scguns:mech_press/depleted_diamond_steel`（及 `powered_...` ✓）——
     它们引用 `create:experience_nugget` 却**没有 `neoforge:conditions`** ✓。**已核对 0.5.5 原始 jar：原版就是如此** ✓
     ⇒ 只有"没装 Create"的玩家会看到这两行 ✓（配方被丢掉 ✓，而它本来就是 Create 专属配方 ✓）。
     **没有擅自改** ✗：补法就是加 `neoforge:conditions: [{"type":"neoforge:mod_loaded","modid":"create"}]` ✓，
     但 `convert_resources.py` 重放资源时会覆盖这类手工数据修正 ✓（§6.1 的既有注意事项 ✓），
     要做就得同时把它写进修复脚本 ✓ —— 等玩家拍板 ✓。
* **未验证** ✗：**低版本客户端**（21.1.150 客户端）没实机跑过 ✓ —— 本次只验证了专用服务器的加载与启动 ✓；
  客户端侧（渲染、客户端 mixin、JEI）建议玩家拿一个 21.1.150 的实例试一次 ✓。
* 顺带把范围写清楚 ✓：如果玩家的 NeoForge **低于 21.1.150** ✓，游戏会在加载 GeckoLib 时就停下 ✓，
  这不是我们能绕过的 ✗（除非改用更老的 GeckoLib ✓，而那会丢掉本移植依赖的 4.6+ API ✓）。

# 71. 公开发布：GitHub（已推送）+ CurseForge（文案已备好）

目标仓库 `https://github.com/ELMOSYG/scorched-guns-0.5.5-Unofficial-Port`（玩家已创建、初始为空）✓。

## 71.1 公开前查出来的三件事（新工具 `tools/prepublish_audit.py`）

| 问题 | 实测 | 处理 |
|---|---|---|
| **第三方 jar 被 git 跟踪** ✗ | `libs/` **97.1 MB**（Create Aeronautics 31.6 / Create 18.2 / IE 13.6 / Sable 12.4 / Mekanism 11.4 / JEI …）、`maid-compat/libs/` **24.4 MB**（TLM 23.3 / Cloth Config）、`libs-compile/` 35 KB，**另加上游的 `ScorchedGuns-0.5.5-1.20.1.jar`** | 全部排除 ✓（它们是别人的作品、多数 ARR，公开分发会踩线 ✗） |
| `build-logs/` **559 个文件 / 115 MB** | 其中绝大多数是逐轮编译日志 | 只公开 **6 个**（`WORKER_BRIEF.md`、`JEI19_NOTES.md`、`build-final.txt`、`server-final.txt`、`floor-compile.txt`、`floor-server.txt`，共 **1.8 MB** ✓） |
| `tools/rcon_*.py` **13 处硬编码 RCON 密码** | 那只是本地 dev 世界的密码（`run/server.properties` 不入库） | 改成 `os.environ.get("SCGUNS_RCON_PASSWORD", …)` ✓（`tools/harden_rcon_password.py` ✓，`--selftest` 对 `git show HEAD:` 实测命中、且**幂等** ✓） |

公开树审计复跑 ⇒ **只剩 gradle-wrapper 一个 jar、无凭据、无 >5 MB 文件** ✓。

## 71.2 授权与署名（`LICENSE` + `NOTICE`）

* **上游是 GPL-3.0** ✓（0.5.5 jar 的 `mods.toml` 里 `license="GNU GPLv3"` ✓，authors=`ribs` ✓，
  credits=`MrCrayfish, Ribs` ✓）⇒ 本移植**沿用同一许可** ✓：`LICENSE` 放 GPLv3 全文（35,149 字节，
  取自 gnu.org ✓）。
* `NOTICE` 逐条写明 ✓：上游内容与其许可 ✓；**音效 CC0**（上游 jar 内
  `assets/scguns/sounds/SOUND-LICENSE.txt` ✓）；`zh_cn.json` 来自社区汉化包（**署名待玩家填** ✗）；
  女仆兼容（`scg2_maid_compat`）作者 ✓；第三方依赖**不随仓库分发** ✓ 及其各自的许可与获取方式 ✓；
  Mojang mappings 的许可 ✓。
* `README.md` 重写 ✓（英文为主 + 中文简版）：这是什么 ✓ / 要求（MC 1.21.1、**NeoForge ≥21.1.150**、
  Framework 0.13.11+、GeckoLib 4.9.3+、Curios 9.5.1+ ✓）/ 安装 ✓ / 从源码构建 ✓ /
  移植文档指引 ✓ / 已知限制 ✓ / 授权与"非官方、与原作者无关"免责 ✓。

## 71.3 分支与推送

* 玩家选择**单一初始提交** ✓：`git checkout --orphan main` → 按新 `.gitignore` 重新 `add` ✓
  （排除 `libs/*.jar`、`maid-compat/libs/*.jar`、`libs-compile/*.jar`、`准备的前置/`、`需要移植的mod/` ✓，
  `build-logs` 用白名单只留 6 个 ✓）⇒ **7827 个文件 / 41.2 MB** ✓（原 8406 个 / 275.9 MB ✓）。
* 已推送 ✓ `origin/main` = **`dbf4beb`** ✓（`git push -u origin main` ✓，`credential.helper=manager` ✓）。
* **本地 `master` 保留全部历史** ✓ —— HANDOFF 里引用的提交哈希（如 §42.7 的 `86a2d1f` ✓）仍然可在本地查 ✓；
  公开的 `main` 是单提交 ✓。**以后开发走 `main`** ✓（jar 已被 ignore ✓，构建不受影响 ✓）。
* 三个"要自己放哪些 jar"的说明 ✓：`libs/README.md`、`maid-compat/libs/README.md`、`libs-compile/README.md` ✓。
* **MDK 残留清理** ✓（推送后在公开树里发现的 ✓）：删掉 `MDK-1.21.1-ModDevGradle-main.zip`（模板压缩包 ✓）；
  `TEMPLATE_LICENSE.txt` **保留** ✓ 并写进 `NOTICE` 第 6 节 —— 它讲的是 MDK 骨架的 **MIT 许可**
  （Copyright (c) 2023 NeoForged project ✓，只覆盖模板文件本身 ✓）；
  **`build.yml` 换掉** ✗ —— MDK 自带的 CI 是 `./gradlew build` ✓，而公开仓库里**没有依赖 jar** ✗
  ⇒ 每次推送必然是红叉 ✗。新增 `.github/workflows/checks.yml` ✓ 只跑**不需要 jar/编译产物/`.refs`** 的那 19 个工具 ✓
  （实测本地全过 ✓）：15 个审计 + `survey_conditions` + `fix_loot_functions` + `analyze_advancements` +
  `prepublish_audit` ✓。需要 `build/classes`、`.refs/` 或依赖 jar 的审计（`audit_stale_overrides`、
  `audit_subscriber_bus`、`audit_mixins`、`audit_override_drift`、`audit_mixin_plugin_gate`、
  `audit_tlm_isolation`、`audit_abstract_events`、`audit_double_background`）**只在本地跑** ✓ ——
  这一点在 workflow 文件头和 README 里都写明了 ✓。

## 71.4 CurseForge

**项目已由玩家创建** ✓：**Scorched Guns 2 0.5.5 Unofficial port** ✓
（<https://www.curseforge.com/minecraft/mc-mods/scorched-guns-2-0-5-5-unofficial-port> ✓，slug
`scorched-guns-2-0-5-5-unofficial-port` ✓，项目 id **1709719** ✓，建于 2026-09-24 ✓）。
页面上的标题 / 简介 / 描述**都已填好** ✓ —— 描述就是 `CURSEFORGE.md` 里那份 **HTML 块**（逐字符一致 ✓，
用公共 API `api.cfwidget.com` 复核过 ✓）。
`CURSEFORGE.md` ✓ 里保留着可再次粘贴的：字段表 ✓ / **必需依赖关系表**（Framework、GeckoLib、Curios 设为
required ✓，其余可选 ✓）/ 描述全文（Markdown 与 HTML 两版 ✓）/ 首版更新日志 ✓ / 上传前检查清单 ✓，
以及一节**"页面现状"** ✓（记录已做与待做 ✓）。

⚠️ **待玩家补两件事** ✓：
1. **换掉那个文件** ✗ —— 页面上现在是 `scguns-0.5.5.jar`（**19,393,354 字节** ✓），那是**在 §72（niami 射击）
   与 §73（版本号 0.5.5.1）之前**构建的 ✓ ⇒ 要用 `build/libs/scguns-0.5.5.1.jar`（**19,392,915 字节** ✓，
   版本 `0.5.5.1` ✓）作为**新文件**上传 ✓（CurseForge 的文件不可覆盖 ✓，字节数是最快的区分办法 ✓），
   再把旧文件退下/删除 ✓。
2. **确认许可设成 GPL-3.0** ✓（公共 API 看不到该字段 ✓，上游是 GPL-3.0 ✓ ⇒ 必须一致 ✓），
   并把 Framework / GeckoLib / Curios 设为 **Required** 关系 ✓（同样无法从 API 侧确认 ✓）。


## 71.5 待玩家确认的一处

`NOTICE` 里汉化包的署名目前是占位文字 ✓ —— 给一个**译者名字或资源包链接**即可替换 ✓
（或明确说"就写社区汉化"也行 ✓）。

# 72. `scguns:niami` 打不出去 —— 1.21 的箭要求"发射它的那把武器"，而移植传了**空物品**

玩家报告："**scguns:niami 无法正常射击**" ✓。

## 72.1 复现（FakePlayer + `ServerPlayHandler.handleShoot`，专用服务器）

先核对数据 ✓：`data/scguns/guns/niami.json` 与 0.5.5 **逐字一致** ✓（41 个字段全同 ✓）；
它是那把**射箭的枪** ✓（`"firesArrows": true`、`projectile.item = minecraft:arrow`、弹药也是箭 ✓、
`weaponType: special`、`fireMode: semi_automatic` ✓）⇒ 问题在代码 ✓。

于是按项目的"复现优先"写探针 ✓（临时类 + `FakePlayer` ✓，`NbtHelper` 塞 `AmmoCount=6` ✓，
直接调 `ServerPlayHandler.handleShoot(new C2SMessageShoot(player), player)` ✓）：

```
[SCGUNS-NIAMI] scguns:niami fireMode=semi_automatic reloadType=mag_fed firesArrows=true ammo=6 hasAmmo=true
[SCGUNS-NIAMI] scguns:niami THREW java.lang.IllegalArgumentException: Invalid weapon firing an arrow
```

## 72.2 根因：`AbstractArrow` 的 weapon 参数不能是空栈

1.21 的 `AbstractArrow`（源码实测 `.refs/nf-src/.../AbstractArrow.java:97-100` ✓）：

```java
if (firedFromWeapon != null && level instanceof ServerLevel serverlevel) {
   if (firedFromWeapon.isEmpty()) {
      throw new IllegalArgumentException("Invalid weapon firing an arrow");
   }
```

0.5.5 调的是 **`new Arrow(world, player)`** ✓（1.20.1 那个"没有 weapon"的构造器 ✓），
移植为适配 1.21 改成了 `new Arrow(world, player, new ItemStack(Items.ARROW), ItemStack.EMPTY)` ✗
—— 第 4 个参数正是 weapon ✓，**空栈在服务端直接抛异常** ✗。而它抛在 `getArrow` 里 ✓
⇒ `handleShoot` 从 `for (i < count)` 循环那里就断了 ✗ ⇒ **这一发什么都不会发生** ✓
（没有箭、不扣弹药、没有枪声 ✓）⇒ 玩家看到的正是"无法正常射击" ✓✓。
（**单人同样中招** ✓：集成服的 level 也是 `ServerLevel` ✓。）

## 72.3 修法：传 **`null`**（这才是 0.5.5 的语义）

该参数是 **`@Nullable ItemStack firedFromWeapon`** ✓（`Arrow` 的两个构造器都标了 ✓），
传 null 时整段校验与附魔钩子都被跳过 ✓ —— 与 0.5.5"没有 weapon"完全一致 ✓。

```java
Arrow arrow = new Arrow(world, player, new ItemStack(Items.ARROW), null);
```

> **改了又改的一次（记下来）** ✓：我第一版传的是**枪本身**（"发射它的就是这把枪"，听起来更对 ✓），
> 并且已经实测通过 ✓。但顺手核源码时发现 **`AbstractArrow` 会把 weapon 存进箭的存档** ✓
> （`compound.put("weapon", this.firedFromWeapon.save(...))` ✓）⇒ 每支箭都会**带一份整枪（含配件）的拷贝** ✗
> ⇒ 改成 `null` ✓ 并**重新实跑验证** ✓。教训：**"参数能不能传"和"传了会存下来什么"是两件事** ✓。

## 72.4 同一探针顺带暴露：服务端**根本没有 animation controller**（3 处 NPE）

`AnimatedGunItem.registerControllers` 只在客户端执行 ✓（`FMLEnvironment.dist == Dist.CLIENT` ✓）
⇒ 服务端 `getManagerForId(id).getAnimationControllers().get("controller")` **恒为 null** ✗，
而三处**服务端可达**的代码直接调用它 ✓（对照用的 `mk43_rifle` 就是这么暴露出来的 ✓）：

| 位置 | 后果 |
|---|---|
| **`GunFireEvent$Post` 的构造函数** | 它在 `handleShoot` 末尾 `new GunFireEvent.Post(...)` ✓ ⇒ **事件还没投递就抛** ✗ ⇒ `Post` 的**所有监听器都不执行** ✗ ⇒ 服务端每开一枪都少掉击退 / 热管 / 枪灯 / 抛壳 / 卡壳音效 ✓，异常还会从 `handleShoot` 逃出去 ✓ |
| `GunEventBus.postShoot` | 同样没有 dist 守卫 ✗（它后半段的玩法逻辑因此永远跑不到 ✓） |
| `ReloadTracker` 装填完成分支 | 它是 `!isClientSide` 的**服务端** tick 处理器 ✓ ⇒ 专用服务器上"装填完成"会 NPE ✓ |

三处都补了 `!= null` 守卫 ✓（客户端行为一字不变 ✓）：动画只在有 controller 时触发 ✓，
玩法部分照常执行 ✓。

## 72.5 防复发 + 验证

* 新增审计 **`tools/audit_server_fire_paths.py`** ✓（含 `--selftest` ✓）：
  ① `new Arrow(...)` 参数不足 4 个、或第 4 个是 `ItemStack.EMPTY` ⇒ 报错 ✓；
  ② 除 `client/**` 与 `@OnlyIn(Dist.CLIENT)` 方法外 ✓，任何取 controller 的地方必须在后面的代码里
  **守卫同名变量** ✓（要求 `if (x != null)` / `if (x == null) return` ✓）。
  **自测** ✓：对 `git show HEAD:` 的修复前源码跑 ⇒ **4 处全部命中**（1 个空 weapon + 3 个未守卫 ✓）；
  当前源码 **0** ✓。已加入 CI 的静态检查列表 ✓。
  > 这条审计的第一版**误报了刚修好的三处** ✗：我把"守卫窗口"设成 300 字符，而守卫前有一段注释 ✓
  > 正好把它挤出去 ✓（差 5 个字符 ✓）⇒ 改成**先剥注释 + 按变量名精确匹配** ✓。
  > 教训：**启发式审计也要先在自己刚改过的代码上跑一遍** ✓。
* 实测（专用服务器 + FakePlayer，探针读完即删 ✓）：

```
修复前  scguns:niami THREW IllegalArgumentException: Invalid weapon firing an arrow
        scguns:mk43_rifle(对照) THREW NullPointerException: animationController is null
修复后  scguns:niami            -> arrows spawned=1   ammo 6 -> 5   （Arrow@x,y,z ✓）
        scguns:mk43_rifle(对照)  -> 开枪成功、弹药 6 -> 5、**无异常** ✓
```

* 门禁：`javac` 0 错误 ✓、`gradlew build` ✓、**24 个审计全 0** ✓、`verify_installed_jar` **160/160** ✓、
  探针已删除 ✓（`tmpprobe` 包整个移除 ✓）、已安装 ✓（上一版备份 `.bak-213733` ✓）。
  - 又踩一次 §9 的坑 ✓：**上一轮 dev 服务器还在跑时，下一次 `runServer` 会在启动阶段失败**
    （`Unable to delete file run\logs\latest.log` ✓）⇒ 跑完必须按 `devlaunch` 精确杀 java 进程 ✓。
* **边界**：**客户端表现仍要玩家确认** ✓ —— "能射出箭"是服务端实测 ✓；
  拉弓音效 / 持枪动画 / 箭的命中表现在客户端 ✓。另外这个 bug 与 §65 的"充能枪不能开火"**无关** ✗
  （那是 PULSE 枪 ✓，niami 是 `semi_automatic` ✓），§65.4 的问题依然待玩家给现象 ✓。

# 73. 版本号从 `0.5.5` 改为 **`0.5.5.1`**（移植自己的版本号）

玩家要求：产出的 jar 版本号改成 **0.5.5.1** ✓。

## 73.1 改法与影响面

`gradle.properties` 的 `mod_version` 是**一处真相** ✓：它同时决定
① jar 文件名（`build.gradle` 的 `archivesName = mod_id` + `version` ⇒ `build/libs/scguns-<v>.jar` ✓）、
② `neoforge.mods.toml` 里的 `version="..."` ✓（模组列表里显示的版本 ✓）。
⇒ 只改一行 ✓，clean build 后产物为 **`build/libs/scguns-0.5.5.1.jar`**（18.49 MB ✓）。

> 语义说明 ✓：**内容版本仍是上游的 0.5.5** ✓，`0.5.5.1` 是**本移植自己的发布号** ✓
> （`gradle.properties` 里已写明这一点 ✓）。女仆兼容声明的 `scguns >= [0.5.0,)` 仍然满足 ✓，无需改动 ✓。

## 73.2 工具里的硬编码文件名（这次一并改成"按版本发现"）

之前有 4 个工具写死了 `build/libs/scguns-0.5.5.jar` ✗ —— 版本一动就全失效 ✓，所以这次不只是改名，
而是**改成按 glob 找最新产物** ✓（以后升版本不用再改工具 ✓）：

| 工具 | 现在的做法 |
|---|---|
| `tools/install_jar.py` | 取 `build/libs/scguns-*.jar` 里最新的那个 ✓，并**校验它声明的版本等于 `mod_version`** ✓ |
| `tools/verify_installed_jar.py` | 按 `mods/scguns-*.jar`（排除 `.bak-*`）找已安装的 ✓、按 `build/libs/scguns-*.jar` 找产物 ✓；新增一项检查 **"包内版本 == mod_version"** ✓ |
| `tools/audit_tlm_isolation.py` | 同上按 glob 取产物 ✓ |
| `tools/probe_tag_ids.py` | 同上 ✓ |

## 73.3 一个必须处理的坑：实例里不能同时有两份

版本号进了文件名 ⇒ 新 jar 不会覆盖旧 jar ✗ ⇒ `mods/` 里会同时躺着 `scguns-0.5.5.jar` 与
`scguns-0.5.5.1.jar` ✓ ⇒ **同一个 mod id 出现两次 = 加载报错** ✗。
所以 `install_jar.py` 现在会：**把所有旧的 `scguns-*.jar` 备份成 `.bak-HHMMSS` 后删除** ✓，再放新的 ✓。实测输出：

```
installing scguns-0.5.5.1.jar (declared version 0.5.5.1)
removed the old copy scguns-0.5.5.jar (kept as scguns-0.5.5.jar.bak-214132) - two mod jars with one mod id would not load
installed scguns-0.5.5.1.jar (19392915 bytes)
```

## 73.4 验收

* `gradlew clean build` ✓ ⇒ `build/libs/` 里**只有** `scguns-0.5.5.1.jar` ✓。
* `install_jar.py` ✓ 装入实例 ✓，旧 jar 已备份并移除 ✓（实例里现在只有一份 ✓）。
* `verify_installed_jar.py` **161/161** ✓（新增的第 33 项：包内版本 == `mod_version` ✓）。
* **文档同步** ✓：README（安装与构建命令 ✓）、`CURSEFORGE.md`（上传文件与清单 ✓）、`PORTING_STATUS.md` ✓、
  §2 与 §9 的可执行命令 ✓。
  ⚠️ **§69 之前的历史段落里仍会看到 `scguns-0.5.5.jar`** ✓ —— 那是**当时的真实文件名** ✓（例如 §49 那次
  "游戏运行中覆盖 jar"的事故 ✓），**不要按它去找文件** ✓。

# 74. 袭击系统"不抗卸载"：玩家一死，血条消失、战利品也拿不到

玩家报告：**"scgun 自带的袭击系统不抗卸载，只要玩家死了袭击 boss 的血条自动消失，也拿不到战利品"** ✓。

## 74.1 根因：**"看不见 boss" 被当成了 "boss 已死"**

`ActiveRaid` 找 boss 用的是 `level.getEntity(uuid)` ✓ —— 它对**区块未加载**的实体返回 **null** ✓，
与"实体已死"**是同一个返回值** ✗。而 0.5.5（以及本移植到这一轮之前 ✓）在 `tick()` 里写的是：

```java
LivingEntity boss = this.getBoss();
if (boss != null && boss.isAlive()) { ...更新血条 / 刷小怪... }
else { this.endRaid(this.bossConfirmed); }     // ← bossConfirmed 此时通常已经是 true
```

`validateBoss()` 一旦确认过 boss，`bossConfirmed` 就**永久为 true** ✓ ⇒ 这个 `else` 实际走的是
**`endRaid(true)` = "袭击已被击败"** ✗。触发条件正好就是"卸载" ✓：玩家死亡后重生到远处 / 被传送 /
单纯走开 ⇒ boss 所在区块卸载 ⇒ `getBoss()` 返回 null ⇒ **下一 tick 袭击被判定为胜利结束** ✗✓。

一条链解释两个症状 ✓：

1. `endRaid(true)` 第一件事就是 `bossBar.setVisible(false)` + `removeAllPlayers()` ✓ ⇒ **血条消失** ✓；
2. 它只对**附近 64 格**广播 "Raid Defeated!" ✓ ⇒ 玩家在远处 ⇒ **什么都没看到** ✓；
3. `RaidManager.tickActiveRaids` 会把 `isActive()==false` 的袭击从 `activeRaids` **和存档**里移除 ✓；
4. 而**袭击专属战利品**只在 `RaidManager.onEntityDeath` 里、**且该袭击仍在 `activeRaids` 中**时才掉落 ✓
   （`bossData.specialLootTable()` ✓）⇒ 玩家回来杀掉 boss **只剩普通掉落** ⇒ **拿不到战利品** ✓✓。

即：**boss 还活着、还在世上**（`setPersistenceRequired()` ✓，`endRaid(true)` 也不会 discard 它 ✓），
但袭击已经被"提前结算" ✗ —— 这就是玩家说的"不抗卸载" ✓。

## 74.2 顺带发现的第二处同类问题 + 一个泄漏

* `validateBoss()`（**确认 boss 之前**的阶段 ✓）用 `ticksSinceLoad >= 600`（30 秒 ✓）⇒ 袭击若是
  **从存档恢复**的（`restore()` 把 `bossConfirmed` 置回 false ✓）而玩家当时在远处 ✓，boss 同样解析不到
  ⇒ **30 秒后袭击被判失败** ✗ —— 同一个"卸载即失败"的错误 ✓。
* 那条等待路径**强制加载了区块却从不解除** ✗（`setChunkForced(x, z, true)` 之后没人 unforce ✓）
  ⇒ 袭击结束后该区块**永久保持加载** ✓。

## 74.3 修法（把三种状态分开）

| 状态 | 判定 | 处理 |
|---|---|---|
| 已加载且存活 ✓ | `boss != null && boss.isAlive()` | 正常：更新血条 / 刷小怪 ✓；记录 `lastKnownBossPos` ✓、**解除强制区块** ✓、清零"看不见"计数 ✓ |
| 已加载但已死 ✓ | `boss != null`（`!isAlive`） | **真的**被击败（漏了死亡事件的极端情况 ✓）⇒ `endRaid(true)` ✓ |
| **解析不到** ✓ | `boss == null` | **不是失败** ✗ ⇒ 计数 +1、**把 boss 上次所在区块强制加载**回来 ✓、袭击（含血条与战利品表）**继续存在** ✓；只有连续 `BOSS_LOST_GRACE_TICKS = 6000`（**5 分钟** ✓）仍找不到才 `endRaid(false)` 并广播新键 `raid.scguns.boss_lost` ✓ |

配套 ✓：`endRaid()` 现在**一定** `releaseForcedChunk()` ✓（消灭永久强制加载的泄漏 ✓）；
`validateBoss()` 的等待改用同一个 5 分钟上限 ✓（恢复存档的袭击不再 30 秒被丢 ✓）；
`restore()` 显式清空 `lastKnownBossPos` / `forcedChunkPos`（运行期状态，不入存档 ✓）。

> 依据 ✓：这是**上游 0.5.5 就有的设计缺陷** ✓（0.5.5 的 `ActiveRaid.java:162` 逐字相同 ✓，
> 已用反编译源核对 ✓），不是移植引入 ✓；但它直接把玩法毁掉（打完拿不到东西 ✓），
> 所以按玩家报告修掉 ✓，并记为**有意偏离 0.5.5** ✓。

## 74.4 实测（调度型探针：起袭击 → 模拟卸载 → 再让 boss 真死）

临时探针 ✓（`ServerTickEvent.Post` 驱动的状态机 ✓，跑完已删 ✓）在专用服务器上做了两件事：

```
[SCGUNS-RAID] A started: active=true confirmed=true boss=Sheriff Tibias barVisible=true
[SCGUNS-RAID] A: boss made unresolvable (simulated unload) -> unresolvable
[SCGUNS-RAID] A after 200 ticks unloaded: active=true tracked=true barVisible=true
                 -> SURVIVED THE UNLOAD          ← 修复后 ✓（修复前这里会是 active=false、tracked=false）
[SCGUNS-RAID] B started: active=true boss=Colonel Jil
[SCGUNS-RAID] B: items near boss before the kill = 0
[SCGUNS-RAID] B after the boss died: active=false tracked=false itemsNear=3
                 -> RAID ENDED AS DEFEATED       ← 真死仍然正常结算 ✓
[SCGUNS-RAID]    dropped: 28x scguns:grapeshot
[SCGUNS-RAID]    dropped: 30x scguns:powder_and_ball
[SCGUNS-RAID]    dropped: 1x scguns:antique_flare   ← 专属战利品表仍然掉 ✓
```

**"卸载"是怎么模拟的** ✓：`boss.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK)` ✓ ——
这正是**区块卸载时引擎对实体做的事** ✓（不是 kill ✓、不是 discard ✓），所以 `level.getEntity` 立刻返回
null ✓ 而 boss 并没有死 ✓，与玩家遇到的场景等价 ✓。

## 74.5 防复发 + 验收

* 新增审计 **`tools/audit_raid_unload_safety.py`** ✓（含 `--selftest` ✓）：禁止
  `endRaid(this.bossConfirmed)` ✓；要求"解析不到"分支里有 `keepBossChunkLoaded` ✓ 与
  `BOSS_LOST_GRACE_TICKS` ✓；要求 `tick()` 能区分"已加载但已死" ✓；要求 `endRaid()` 释放强制区块 ✓、
  `onBossResolved()` 清理丢失状态 ✓。
  **自测** ✓：对 `git show HEAD:` 的修复前源码跑 ⇒ **8 条全部命中** ✓；当前源码 **0** ✓；已加入 CI ✓。
* 语言文件 ✓：新增 `raid.scguns.boss_lost`（EN/ZH 两边都加 ✓，`audit_lang_keys` 实测
  **1807/1807、missing/extra 全 0** ✓）。
* 门禁 ✓：`javac` 0 错误 ✓、`gradlew build` ✓、**25 个审计全 0** ✓、`verify_installed_jar` **161/161** ✓、
  探针已删除 ✓、已安装 ✓（上一版备份 `.bak-201801` ✓）。
* **边界**：这是**服务端**行为，已实测 ✓；血条在**客户端**的显示时机 ✓ 建议玩家实机再确认一次 ✓
  （现在应当是：玩家死了血条**留在屏幕上** ✓，回来继续打，或 5 分钟后才提示找不到首领 ✓）。
  另注 ✓：**B 组**用 `boss.kill()` 验证的是"真死仍结算战利品" ✓，而玩家那次"拿不到战利品"是 A 种情况
  （袭击被提前结束 ✓）—— 两条路径现在都正确 ✓。

# 75. 袭击"有时候刷在洞穴层"：0.5.5 的洞穴搜索**从 -5 开始**（优先放到玩家脚下的洞里）

玩家报告：**"袭击有时候会刷在地下（也就是洞穴层）"** ✓。

## 75.1 根因：两个错在同一个地方 —— 用 `playerY < 50` 猜"玩家在地下"

0.5.5（本移植逐字相同 ✓，`ActiveRaid`/`RaidManager` 都核对过 ✓）的 `findRaidSpawnLocation`：

```java
int playerY = (int)center.y;
boolean isUnderground = playerY < 50;          // ← 用 Y 猜"是否在地下"
...
if (isUnderground) groundPos = findNearestValidCaveSpawn(level, pos, playerY);
else               groundPos = level.getHeightmapPos(MOTION_BLOCKING_NO_LEAVES, pos);
```

而那个洞穴搜索是：

```java
for (int yOffset = -5; yOffset <= 5; yOffset++) { ... }   // ← 从"玩家脚下 5 格"开始往上试
```

两个错叠在一起 ✓：

1. **`playerY < 50` 不是"在地下"** ✗ —— 站在**露天**低处（峡谷底、深谷、下潜到海里 ✓）的玩家也会被判成"地下" ✓；
2. **洞穴搜索的起点是 -5** ✗ —— 它**优先**选玩家**脚下 5 格**的位置 ✓，而 y&lt;50 的地层里到处是洞穴 ✓
   ⇒ 玩家明明站在地面上 ✓ 袭击却被塞进脚下的洞里 ✓✓ —— 这正是玩家看到的"有时候刷在洞穴层" ✓。

反方向的错同样存在 ✓：**浅层洞穴**（y&gt;50）里的玩家会被判成"地表" ✓ ⇒ 袭击刷在**头顶的地表** ✓。

## 75.2 修法：不猜了 —— **先按玩家自己的 Y 找，再退回到该列的地面**

```java
private Vec3 findRaidSpawnLocation(ServerLevel level, Vec3 center) { ... findSpawnAtPlayerLevel(level, column, playerY) ... }

private BlockPos findSpawnAtPlayerLevel(ServerLevel level, BlockPos column, int playerY) {
   // 1) 玩家自己那一层（0 偏移先试）
   // 2) 依次 ±1..±SPAWN_Y_WINDOW（8 格）
   // 3) 最后才用该列的地面高度，且要求 |groundY - playerY| <= SPAWN_Y_WINDOW
}
```

* 站在**地表**的玩家 ✓：他自己的 Y 就是地面 ✓ ⇒ 落点与玩家同层 ✓（不再"往脚下 5 格" ✗）；
* 站在**峡谷底/露天低处**的玩家 ✓：他自己的 Y 就是峡谷底 ✓ ⇒ **不再被放进洞穴** ✓✓；
* 在**洞穴里**的玩家 ✓：他自己的 Y 就是洞穴层 ✓ ⇒ 袭击照旧在他身边 ✓（0.5.5 的本意 ✓）；
* 在**浅层洞穴**的玩家 ✓：落点跟着他 ✓（不再跑回地表 ✓）；
* 站在**山体/峡谷壁**旁边的玩家 ✓：地面高度会被 8 格窗口**排除** ✓ ⇒ 不会把袭击放到头顶的崖顶/山顶 ✓；
* 全部候选都不合格时（15 次尝试 ✓）仍然返回 null ✓ = 不开袭击 ✓（与 0.5.5 相同 ✓）。

## 75.3 实测（专用服务器 + FakePlayer，探针读完即删）

```
[SCGUNS-SPAWN] standing spot 0, 74, -4   heightmap here = 71   canSeeSky=true
[SCGUNS-SPAWN] player feet y=74.0
[SCGUNS-SPAWN] raid spawn centre y=73.0   player y=74   delta=-1.0
                 -> ON THE PLAYER'S LEVEL ✓
                 (0.5.5 would have picked y=69 first: its cave search started at -5)
[SCGUNS-SPAWN] spawn block below=grass_block   at spawn=air
```

⇒ 落点就在玩家那一层 ✓、脚下是草方块 ✓、落点是空气 ✓。
（探针同时打印了"0.5.5 会先试 y=69" ✓ —— 这就是老代码把袭击塞进洞穴的机制 ✓。）

## 75.4 防复发 + 验收

* 新增审计 **`tools/audit_raid_spawn_level.py`** ✓（含 `--selftest` ✓）：禁止 `playerY < 50` 这种
  "用 Y 猜地下"的判据 ✓、禁止 `findNearestValidCaveSpawn` / `yOffset = -5` 回归 ✓、
  要求存在 `findSpawnAtPlayerLevel` ✓（先试玩家自己那层 ✓）、要求 `SPAWN_Y_WINDOW` 有界 ✓、
  要求地面高度回退被窗口夹住 ✓。
  **自测** ✓：对 `git show HEAD:`（本轮之前的源码 ✓）跑 ⇒ **6 条全部命中** ✓；当前源码 **0** ✓。
  > 第一版又踩了同一个坑 ✗：`playerY < 50` 出现在**我自己的注释**里 ✓，于是审计把**当前源码**也报成问题 ✗
  > ⇒ 加 `strip_comments()` ✓（审计**只允许读代码，不读注释** ✓）。已加入 CI ✓。
* 门禁 ✓：`javac` 0 错误 ✓、`gradlew build` ✓、**26 个审计全 0** ✓、`verify_installed_jar` **161/161** ✓、
  探针已删除 ✓、已安装 ✓（上一版备份 `.bak-202642` ✓）。
* **边界**：这是**服务端落点**行为 ✓（已实测 ✓）。若玩家希望"袭击永远不上地下"（哪怕自己正在挖矿 ✓），
  那是一次**玩法选择** ✓：可以在 `Config.COMMON.raids` 加一个开关 ✓（例如 `raidsForceSurfaceSpawn` ✓），
  默认仍保持"跟着玩家" ✓ —— 等玩家拍板 ✓。

# 76. 袭击**只在地表刷**（玩家拍板：洞穴层 = 刷在玩家找不到的地方，必须去掉）

玩家补充说明 ✓：**"scgun 的袭击机制问题就是他会到处乱刷，洞穴层的问题就是他会刷在玩家找不到的地方，
所以应该只在地表层刷新"** ✓。

## 76.1 规则（§75 的"跟着玩家"改成"只在地表"）

| 情形 | 现在的落点 |
|---|---|
| 玩家在地表 ✓ | 附近**地表** ✓ |
| 玩家在峡谷底 / 露天低处 ✓ | 该列自己的地面（= 峡谷底）✓ |
| **玩家在洞穴里挖矿** ✓ | **仍然刷在地表** ✓（就在他上方 25–40 格处 ✓，他上去就能找到 ✓✓）|
| 玩家在浅层洞穴（y&gt;50）✓ | **地表** ✓（不再跟着他进洞 ✓）|
| **下界**（有天花板 ✓）| 例外：那里 `hasCeiling()` 为真 ✓ ⇒ 该维度的"地表"是**基岩顶** ✗ ⇒ 仍然按**玩家自己那层**（= 下界地面）✓ |
| 候选点被**树冠/悬垂/屋顶**遮住 ✓ | **拒绝** ✓（`canSeeSky` ✓ —— 玩家看不见的地方不刷 ✓）|

实现要点 ✓（`findRaidSpawnLocation`）：

```java
boolean surfaceOnly = !level.dimensionType().hasCeiling();      // 下界例外
BlockPos candidate = surfaceOnly ? this.findSurfaceSpawn(level, column)
                                : this.findSpawnAtPlayerLevel(level, column, playerY);

private BlockPos findSurfaceSpawn(ServerLevel level, BlockPos column) {
   BlockPos ground = level.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, column);
   return this.isStandableSpawn(level, ground) && level.canSeeSky(ground) ? ground : null;
}
```

**为什么高度图不会指进洞穴** ✓：洞穴的**天花板本身就是阻挡移动的方块** ✓ ⇒ 该列的"最高阻挡方块"在洞穴之上 ✓
⇒ 高度图给出的位置一定在**地表之上** ✓ —— 这正是"只在地表"能一行代码做到的原因 ✓
（旧的洞穴搜索是在**人为地**往下找 ✓，删掉它即可 ✓）。

## 76.2 实测（专用服务器 + FakePlayer，探针读完即删）

```
[SCGUNS-SURFACE] reference surface spot 0,71,0 (heightmap=71)
case 1: player on the surface (y=71):  spawn y=70.0  heightmap=70  canSeeSky=true  below=grass_block  -> ON THE SURFACE ✓
case 2: player deep underground (y=30): spawn y=70.0  heightmap=70  canSeeSky=true  below=grass_block  -> ON THE SURFACE ✓
```

⇒ **玩家在 y=30 挖矿时，袭击落在 y=70 的地表** ✓（他那一列的高度图是 71 ✓）—— 不再是洞穴里的某个角落 ✓✓。

## 76.3 顺带修掉门禁自身的一个隐患：`--selftest` 用 `HEAD` 作对照会"随提交失效"

这一轮跑 `audit_server_fire_paths --selftest` 时发现它**报 FAIL** ✗ —— 因为它拿 `HEAD` 当"修复前源码" ✓，
而 §72 的修复**已经提交** ✓ ⇒ 它在新源码里当然找不到旧 bug ✓ ⇒ 自测自动变成空转 ✗
（更糟的是：它此前**通过**过 ✓，所以没人会注意到它坏了 ✓）。
**规矩（已写进各个 selftest 的注释 ✓）**：**`--selftest` 必须对着"曾经有该 bug 的那个固定提交"跑** ✓，
不能用 `HEAD` ✓。4 个 selftest 已按此钉死 ✓：

| 审计 | 对照提交 |
|---|---|
| `audit_gun_animation_identity` | `cdab0ec`（§68） |
| `audit_server_fire_paths` | `a70bcb3`（§72 之前） |
| `audit_raid_unload_safety` | `9ce181e`（§74 之前） |
| `audit_raid_spawn_level` | `e9d8e03`（§75，仍按玩家那层） |

四个复跑 ✓：`--selftest` 全部命中 ✓、正式检查全部 0 ✓。

## 76.4 验收

* `javac` 0 错误 ✓、`gradlew build` ✓、**26 个审计全 0** ✓、`verify_installed_jar` **161/161** ✓、
  探针已删除 ✓、已安装 ✓（上一版备份 `.bak-203422` ✓）。
* `audit_raid_spawn_level.py` 已改为检查"**只在地表**"（要求 `findSurfaceSpawn` + `canSeeSky` +
  `dimensionType().hasCeiling()` 例外 ✓；仍禁止 `playerY < 50` / `findNearestValidCaveSpawn` / `yOffset = -5` ✓）。
* **边界** ✓：玩家在**很深的地下**时，袭击会出现在他正上方的地表 ✓ —— 他需要爬上去打 ✓
  （这正是玩家要的"能找到" ✓）；`raidTimeoutMinutes` 仍然兜底 ✓（打不完会超时结束 ✓）。
  若还想更贴脸，可以把候选半径从 25–40 格调小 ✓，说一声即可 ✓。

# 77. **玩家在地下时，自然袭击不再生成**（手动不受影响）

玩家要求：**"我想让玩家在地下时不自然刷新袭击"** ✓。

## 77.1 改了什么

「自然刷新」= 夜里那套自动袭击 ✓（`checkForNightlyRaidSpawn`：黄昏 13000 掷骰排期 ✓、午夜 18000 起事 ✓）。
现在在**起事那一刻**先看目标玩家是否在地下 ✓：

```java
if (isUnderground(level, player.position())) {
   saveData.removeScheduledRaid(dimension);   // 今晚不来了；明晚重新掷骰
   return;
}
```

* **为什么在 18000 判、而不在 13000 判** ✓：从黄昏到午夜玩家可能已经下矿 ✓，要看**起事时**他在哪 ✓。
* **手动途径不受影响** ✓：**袭击信号弹**（`RaidFlareEntity` ✓）与**指令** ✓ 照旧 ✓ —— 那是玩家主动要的 ✓。
* **排期被丢弃** ✓（不是"留着等他上来" ✓）⇒ 他今晚在地下就真的不来 ✓；明晚重新掷骰 ✓。

## 77.2 判据：`isUnderground`（低于该列地面 8 格以上）

```java
public static boolean isUnderground(ServerLevel level, Vec3 position) {
   int surfaceY = level.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(position)).getY();
   return position.y < (double)(surfaceY - UNDERGROUND_MARGIN);   // UNDERGROUND_MARGIN = 8
}
```

> **我第一版写错了，记录一下** ✗：最初写的是"低于该列地面 **且** 看不见天空（`!canSeeSky`）" ✓ ——
> 听起来更严谨 ✓，但**站在地表房子里**的玩家会因为屋顶把高度图抬高 ⇒ 被误判成"地下" ✗
> ⇒ 那样他**整晚待在屋里就永远等不到自然袭击** ✗（树下的玩家同理 ✓）。
> 改成**只比高度、留 8 格余量** ✓：地表 / 屋内 / 树下 / 海面游泳 / 浅坑（≤8 格）都不算地下 ✓，
> 真正下矿（几十格深）才算 ✓ —— 与 §76 落点搜索用的是同一个 8 格窗口 ✓，两边语义一致 ✓。

## 77.3 实测（专用服务器探针，读完即删）

```
[SCGUNS-UG] surface spot 0,71,0   heightmap=71
on the surface                     y= 71.0  underground=false  ✓
indoors on the surface (roof 3 up) y= 71.0  underground=false  ✓   ← 就是上面那个修正点
5 blocks below the ground          y= 66.0  underground=false  ✓
40 blocks below the ground         y= 31.0  underground=true   ✓   ← 自然袭击不会生成
```

## 77.4 防复发 + 验收

* 审计 **`tools/audit_raid_spawn_level.py`** 扩展 ✓：要求午夜分支**必须**先调 `isUnderground(` ✓；
  要求 `isUnderground` 用 `getHeightmapPos` ✓ **且带 `UNDERGROUND_MARGIN`** ✓
  （防止退回"屋里也算地下"那个错误 ✓）。
  **自测** ✓：对固定提交 `e9d8e03`（§76）跑 ⇒ 现在报 **5 条** ✓（含新增的两条 ✓）；当前源码 **0** ✓。
* 门禁 ✓：`javac` 0 错误 ✓、`gradlew build` ✓、**26 个审计全 0** ✓、`verify_installed_jar` **161/161** ✓、
  探针已删除 ✓、已安装 ✓（上一版备份 `.bak-203859` ✓）。
* **未验证** ✓：**真实"午夜自动袭击"链路**需要在有真人玩家的服务器上等一个游戏夜 ✓
  （专用服务器上 `scheduleRaidForTonight` 需要 `level.players()` 里有玩家 ✓，FakePlayer 不算 ✓）
  ⇒ 判据本身已实测 ✓、接线由审计把关 ✓，最终请玩家在游戏里过一夜确认 ✓。
  想快速自测：把 `nightlyRaidChance` 调到 `1.0` ✓，在地下挖矿等到午夜 ⇒ 应当**不刷** ✓；
  上来在地表过一夜 ⇒ 应当刷 ✓。

# 78. 归纳：袭击落点**只在地表**、自然刷新**只在海平面以上**（与幻翼**刻意不同**）

玩家连续三轮把规则定死了 ✓：**"地下袭击刷新机制也许应该删除"** ✓ +
**"玩家低于海平面不自然刷新袭击，和幻翼一致"** ✓ + **"玩家头顶有方块也可以自然刷新，需要和幻翼的机制做差异化"** ✓。
本节是这三条要求的最终形态 ✓，并**取代 §77 的自造判据** ✗。

## 78.1 最终规则

| 项 | 规则 | 依据 |
|---|---|---|
| **落点** | **只在地表**：该列自己的地面高度（`MOTION_BLOCKING_NO_LEAVES` ✓），必须可站立 **且** `canSeeSky` ✓。**没有任何维度例外** ✓ | §76 + 本轮"删除地下机制" ✓ |
| **自然刷新** | **只按高度判**：`playerY >= level.getSeaLevel()` ✓ —— 低于海平面（挖矿/洞穴/潜水）不刷 ✓；海平面及以上**照常刷，哪怕头顶有方块** ✓ | 幻翼规则的"海平面那一半" ✓ |
| **与幻翼的差异** | 幻翼还要求 `canSeeSky` ✓（`PlayerSpawnPhantomsEvent:78` ✓），**我们不要这一条** ✓ | 玩家明确要求 ✓ |
| **手动** | 信号弹 / 指令照常 ✓；若**落点找不到**（例如下界——那里"地面"是基岩顶且没有天空光 ✓）⇒ 不开袭击，但**给玩家一行提示** ✓（新语言键 `raid.scguns.no_surface` ✓） | 本轮 ✓ |

## 78.2 这一轮删掉的"地下机制"（残留清零）

| 删除项 | 原因 |
|---|---|
| `findSpawnAtPlayerLevel`（按玩家那一层找落点 ✓）| 它就是"地下刷新机制"的最后一块 ✓ —— 只剩它还能把袭击放到非地表 ✓ |
| `SPAWN_Y_WINDOW = 8`（配合上一条的竖向窗口 ✓）| 随之无用 ✓ |
| `hasCeiling()` 维度例外 ✓ | 下界"地面"是基岩顶 ✓，放上去等于**玩家找不到** ✗ ⇒ 改为**拒绝**并提示 ✓ |

现在全类**只剩一条落点路径** ✓：`findRaidSpawnLocation → findSurfaceSpawn` ✓（`findHenchmanSpawnPos` 也改用它 ✓，
小怪同样落在地表露天处 ✓）。`grep` 可证：`playerY <`、`findNearestValidCaveSpawn`、`yOffset = -5`、
`findSpawnAtPlayerLevel`、`SPAWN_Y_WINDOW`、`hasCeiling` **全部为 0** ✓。

> **§77 的判据已被取代** ✗：那一版是"低于该列地面 8 格以上算地下" ✓ —— 自造、且会把**站在地表房子里**的
> 玩家（屋顶抬高高度图 ✓）判成地下 ✗。现在这一版**不看高度图、只看海平面** ✓，没有这个副作用 ✓
> （实测确认：地表屋内 `naturalRaid=true` ✓）。

## 78.3 `canSeeSky` 到底是什么（本轮顺带查清，避免以后再误会）

`BlockAndTintGetter:22` ✓：

```java
default boolean canSeeSky(BlockPos pos) { return this.getBrightness(LightLayer.SKY, pos) >= this.getMaxLightLevel(); }
```

⇒ 它是 **skylight == 15** 的判定 ✓，**不是**"头顶有没有方块" ✗。所以：

* **落点**用它 ✓ 是恰当的：要求**满天空光**= 露天 ✓（洞穴/屋里/下界都不满足 ✓，下界 `LightLayer.SKY` 恒 0 ✓）；
* **门禁**不能用它 ✗ —— 那会把"屋里/树下"也一起否掉 ✓（这正是玩家要求差异化的那一点 ✓）。

## 78.4 实测（专用服务器探针，读完即删）

```
[SCGUNS-SEA] sea level=63   surface spot 0,71,0
on the surface          y=71  naturalRaid=true   ✓
indoors on the surface  y=71  naturalRaid=true   ✓   ← 头顶有方块也照常刷（与幻翼的差异）
deep underground (y=31) y=31  naturalRaid=false  ✓
just under sea level    y=62  naturalRaid=false  ✓
exactly at sea level    y=63  naturalRaid=true   ✓   （与原版一致，用 >=）
```

## 78.5 防复发 + 验收

* 审计 **`tools/audit_raid_spawn_level.py`** 已重写为最终形态 ✓：禁止 `playerY < 50` / 洞穴搜索 / `yOffset = -5` ✓；
  要求落点只用 `findSurfaceSpawn`（含 `canSeeSky` + `isStandableSpawn` ✓）且**落点里不得出现 `hasCeiling`** ✓；
  禁止 `findSpawnAtPlayerLevel` / `SPAWN_Y_WINDOW` 回归 ✓；要求午夜分支必须调 `canGetNaturalRaid` ✓、
  而 `canGetNaturalRaid` 必须用 `getSeaLevel` ✓ 且**不得出现 `canSeeSky`** ✓（防止退回幻翼式 ✓）。
  **自测** ✓：对固定提交 `e9d8e03`（§75）跑 ⇒ 命中 **5 条** ✓；当前源码 **0** ✓；已在 CI 里 ✓。
* 语言文件 ✓：新增 `raid.scguns.no_surface`（EN/ZH ✓，`audit_lang_keys` **1808/1808 对齐** ✓）。
* 门禁 ✓：`javac` 0 错误 ✓、`gradlew build` ✓、**26 个审计全 0** ✓、`verify_installed_jar` **161/161** ✓、
  探针已删除 ✓、已安装 ✓（上一版备份 `.bak-204827` ✓）。
* **未验证** ✓：真实午夜链路仍需真人玩家过一夜 ✓（同 §77 ✓）。快速自测：`nightlyRaidChance = 1.0`，
  在海平面以上过夜（**屋里也行** ✓）应当刷 ✓；下到 y&lt;63 过夜应当不刷 ✓。

# 79. 自然袭击"当场可测"：`/scguns raid check`（**诊断**，不是触发器）

玩家反馈 ✓：**"这个不好测试，因为自然刷新的袭击在提示后要等很久才会来"** ✓。

## 79.1 原来为什么没法测

自然袭击是一条**又长又静默**的链路 ✓：黄昏 `13000` 抽签（`nightlyRaidChance` ✓）⇒ 提示
`raid.scguns.warning` ✓ ⇒ 到 `18000` 才真正开刷 —— 中间 **5000 刻 ≈ 4 分 10 秒**真实时间 ✓。
而**六种失败全是静默的** ✓：抽签没过 ✓、目标玩家是创造/旁观（`scheduleRaidForTonight` 只挑非创造非旁观 ✓）、
`raidLevel == 0` ✓、海平面门不过 ✓、地面找不到露天落点 ✓、本维度已有袭击 ✓。
从外部看**一模一样** ✗ —— 今晚没刷 ✗，而且过完夜也不知道卡在哪一步 ✓。

## 79.2 做法：报告调度器**同一批**判断，而不是再写一套

`/scguns raid check`（**不需要权限** ✓，但只能查自己 ✓）把调度器用的**同一批方法**的返回值直接打出来 ✓：

| 报告行 | 调用的**真**方法 |
|---|---|
| 海平面门 `PASS/FAIL` ✓ | `RaidManager.canGetNaturalRaid(level, pos)` ✓ |
| 露天落点坐标 ✓ | `RaidManager.findRaidSpawnLocation(level, pos)` ✓（真搜索 ⇒ §78 那种"下界门过了但没地方刷"也能当场看见 ✓）|
| 今晚排没排上、排在谁头上 ✓ | `RaidSaveData.getScheduledRaid(dimension)` ✓ |
| 突袭等级 / 该等级可用的袭击 ✓ | `PlayerGunProgression.get(...).getCurrentRaidLevel()` + `RaidConfig.getRaidsForLevel(...)` ✓ |
| 创造/旁观提示 ✓ | `scheduleRaidForTonight` 的筛选条件 ✓（**最容易白等一晚的坑** ✓）|

刻意**不做成"一键触发"** ✓：`/scguns raid start`、`startnext` 已经是触发器 ✓，再添一个只会变成
**第二条会走偏的路径** ✗；而且触发器回答不了"是**哪一步**把我挡了" ✗。
所以新增的审计里有一条硬规定：报告函数**不得调用** `startRaid/scheduleRaid/endRaid/surrenderRaid` ✓。

## 79.3 改动

* `RaidManager.findRaidSpawnLocation`：`private` → `public` ✓（并补 `@Nullable` ✓，它本来就会返回 `null` ✓）；
  **判定逻辑一字未改** ✓ —— 只多了一段"这是给诊断命令用的"注释 ✓。
* `ModCommands`：新增 `raid check` 子命令 + `executeRaidCheck` ✓（含注释约 90 行 ✓）。
* 语言：**15 条** `commands.scguns.raid.check.*` ✓（EN/ZH 各 **1823** key，**100% 对齐** ✓）。
* **顺带查出一条"陈旧配置"** ✓（**本轮只记录、不改行为** ✓）：`minDaysBetweenRaids`（默认 2 ✓）
  **从来没被任何代码读过** ✗ —— `RaidSaveData.canScheduleRaid/setLastRaidDay/getLastRaidDay`
  全是死代码 ✓（`setLastRaidDay` 无任何调用点 ✓），所以"最少间隔 N 天"目前**完全无效** ✓，
  实际只有概率抽取 + 海平面门在起作用 ✓。（上游 0.5.5 即如此 ✓，不是移植引入的 ✗。）
  已直接写进报告 ✓，免得玩家以为是自己配置写错 ✓。**是否要按玩家意愿把它接上，留给玩家决定** ✓。

## 79.4 快速测试流程（正式收录 ✓）

前提：**生存模式** ✓（创造/旁观**永远不会**被抽中 ✗）、`raidLevel != 0` ✓（`/scguns progression check` 可查 ✓）。

```
/time set 12000      # 先退到黄昏之前，确保一定会穿过 13000 那个 20 刻窗口
/time set 13000      # 抽签 + 发警告（默认只有 20% 概率通过；没抽中就看 raid check 的提示重来）
/scguns raid check   # 当场看到：门过没过、落点在哪、今晚排没排上、排给谁
/time set 17995      # 5 刻后即 18000 ⇒ 袭击当场开始（原来是再等 4 分多钟）
```

* 验证"地下不刷" ✓：站到 **y &lt; 63**（海平面 ✓）后重复 `13000 → 17995` ✓ ⇒ `raid check` 门 **FAIL** ✓、
  且**不刷** ✓（当晚排期被丢弃 ✓，明晚重新抽签 ✓）。
* 验证"屋里有方块照样刷" ✓：在室内（y 仍 ≥ 63 ✓）走同一流程 ⇒ 照常刷 ✓（与幻翼的差异点 ✓，§78 ✓）。
* 验证"落点只在地表" ✓：`raid check` 给出的坐标**就是**袭击真正会用的落点 ✓（同一条 `findSurfaceSpawn` ✓），
  不必先开一次袭击再跑过去看 ✓。

## 79.5 实测（专用服务器 + 假玩家探针，读完即删 ✓）

专用服务器上**没有玩家** ⇒ 用 NeoForge 的 `FakePlayerFactory.getMinecraft(level)` 造一个假玩家当命令源 ✓，
在 `ServerStartedEvent` 里把 `/scguns raid check` 跑四遍（地表 / y=31 / 创造 / 已排期 ✓），
让输出以服务器的 `[minecraft/MinecraftServer]:` 反馈落进日志 ✓ —— **15 条键全部正常渲染 ✓，
没有原始 key ✗，没有 `TranslatableFormatException` ✗** ✓：

```
=== Raid check: minecraft:overworld ===
Nightly raids enabled: true (chance 20% per night)
Raid already active in this dimension: false
Day 7, time 1171
Note: the nightly scheduler never enforces minDaysBetweenRaids (2); only the chance roll and the sea-level gate decide
Raid level for [Minecraft]: 1, raids at that level: Antique Raid, Frontier Raid
Natural raid gate for [Minecraft]: PASS (y 71, sea level 63)
Surface spawn within 40 blocks: 35 63 -1
Nothing scheduled for tonight: the dusk roll failed (chance 20%) or has not happened yet
Fast test: /time set 12000, then /time set 13000 for the roll and the warning, then /time set 17995 - the raid starts within 5 ticks

--- underground (y=31) ---
Natural raid gate for [Minecraft]: FAIL - below sea level (y 31 < 63), tonight's raid is dropped. A roof overhead does not matter, only the sea level does
Surface spawn within 40 blocks: 20 56 -33

--- creative mode ---
Natural raid gate for [Minecraft]: PASS (y 71, sea level 63)
[Minecraft] is in creative mode, and the scheduler never picks such a player - switch to survival to test a natural raid

--- scheduled for tonight (antique) ---
Natural raid gate for [Minecraft]: PASS (y 71, sea level 63)
Surface spawn within 40 blocks: -20 78 -15
Scheduled for tonight: antique, target 41c82c87-7afb-4024-ba57-13d2c99cae77, day 7
```

值得记下的三点 ✓：① 落点每次都不一样 ✓（真的是随机搜索 ✓），且**可以低于海平面** ✓
（例如 `20 56 -33`：那一列的地表本来就在 56 ✓ —— 门管的是**玩家**高度 ✓，落点管的是**那一列的地面** ✓，
两者互不干涉 ✓）；② `raidLevel=1` 的袭击名是**翻译过的** ✓（`Antique Raid, Frontier Raid` ✓ —— 走的是
`raid.scguns.<id>` ✓）；③ 假玩家不在玩家列表里 ✓ ⇒ 排期那行按设计**回退打印 UUID** ✓（真人玩家则显示名字 ✓）。

## 79.6 防复发 + 验收

* 审计 **`tools/audit_raid_check_command.py`** ✓（新增）：要求 raid 节点下确有 `check` 子命令 ✓
  （**不能**被 `/scguns progression check` 顶替 ✓ —— 审计第一版正是被它骗过的 ✗）、
  `executeRaidCheck` 必须调用上面那批真方法 ✓、**不得调用** `startRaid/scheduleRaid/endRaid/surrenderRaid` ✓、
  15 条语言键 **EN/ZH 都在且 `%s` 数量一致** ✓、**每个调用点的实参个数 == 该键的占位符个数** ✓
  （少参会抛 `TranslatableFormatException` ✓，而多参只是静默忽略 ✓）。
  **自测** ✓：对固定提交 `1bd9f60`（§78）跑 ⇒ 命中 **3 条** ✓（无 `check` 子命令 ✓ / 无 `executeRaidCheck` ✓ /
  `findRaidSpawnLocation` 仍非 public ✓）；当前源码 **0** ✓；已加入 CI ✓。
* 审计**自己**踩的两个坑（记下来 ✓）：① `%s%%` 里的**字面百分号**被第一版当成占位符 ✓ ⇒ 改为逐字符扫描 ✓；
  ② 用"向后找 `(`"定位实参 ⇒ 抓到的是 `dimension.toString()` 的括号 ✓，实参被算成 0 个 ✓ ⇒ 改为
  **向前找**（key 本身是第一个实参 ✓）。两处都修好后才归零 ✓。
* `verify_installed_jar` ✓：新增 4 条 ✓（命令随包发出 ✓、两种语言都有 ✓、快速测试文案在 ✓）+
  1 条"探针**不得**随包发出" ✓ ⇒ **166/166** ✓。
* 门禁 ✓：`javac` 0 错误 ✓（999 文件 ✓，探针已删 ✓）、`gradlew build` ✓、**27 个审计全 0** ✓、
  语言键 **1823/1823** ✓、已安装 ✓（`19397734` 字节 ✓，上一版备份 `.bak-211833` ✓，与 `build/libs` 逐字节一致 ✓）。
* **未验证** ✓：真人在客户端里敲 `/scguns raid check` 看到的样子（颜色/排版 ✓）—— 文本内容已由上面的
  实测逐行确认 ✓，剩下的只是玩家自己的观感 ✓。




















