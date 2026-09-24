# SC2 0.5.5 → MC 1.21.1 / NeoForge 移植：工具与环境清单

> 生成时间：2026-09-19。本文件只描述**工具与版本事实**，不含移植实现。
> 所有"本机状态"都是在本机实测得到的（命令见每节说明）。

---

## 1. 目标版本（= 要用的工具链）

| 项 | 值 | 依据 |
|---|---|---|
| Minecraft | **1.21.1** | MDK `gradle.properties`：`minecraft_version=1.21.1`、`minecraft_version_range=[1.21.1]` |
| NeoForge | **21.1.249** | MDK `gradle.properties`：`neo_version=21.1.249` |
| Loader | FML 2（`loader_version_range=[1,)`）| MDK `gradle.properties` |
| 映射 | **Parchment** `1.21.1` / `2024.11.17` | MDK `gradle.properties` |
| 构建系统 | **ModDevGradle 2.0.146**（不是 ForgeGradle）| MDK `build.gradle`：`id 'net.neoforged.moddev' version '2.0.146'` |
| Gradle | **9.2.1**（wrapper）| MDK `gradle/wrapper/gradle-wrapper.properties`：`distributionUrl=...gradle-9.2.1-bin.zip` |
| Java（编译工具链）| **21** | MDK `build.gradle`：`java.toolchain.languageVersion = JavaLanguageVersion.of(21)` |
| 工具链自动解析 | `org.gradle.toolchains.foojay-resolver-convention` 1.0.0 | MDK `settings.gradle` |

⚠️ 与我们现有的 1.20.1 工程**完全不同**：那边是 ForgeGradle + Gradle 8.8 + Java 17 + `--offline`；
这边是 ModDevGradle + Gradle 9.2.1 + Java 21，**首次构建必须联网**（要拉 moddev 插件、NeoForge 21.1.249、Parchment）。

---

## 2. 必需工具清单

| 工具 | 需要的版本 | 用途 | 本机状态（实测）| 路径 / 获取方式 |
|---|---|---|---|---|
| **JDK 21** | 21.x（如 Temurin/Microsoft 21）| 编译与运行（toolchain=21）| ❌ **本机没有**：只装了两个 → `C:\Program Files\Java\jre-1.8`（JRE，无 javac）与 `C:\Program Files\Microsoft\jdk-17.0.10.7-hotspot`（JDK 17）| ① 依赖 MDK 自带的 **foojay-resolver** 自动下载工具链（联网即可）；② 或手动装 JDK 21 并把 `JAVA_HOME` 指向它 |
| **NeoForge MDK（1.21.1 ModDevGradle）** | `MDK-1.21.1-ModDevGradle-main` | 脚手架 | ✅ 已下载，**尚未解压** | `E:\mod\scgun-0.5.5-1.21.1-neoforge\MDK-1.21.1-ModDevGradle-main.zip`（62,449 B）|
| **Gradle 9.2.1** | 随 MDK 的 wrapper | 构建 | ✅ 随 MDK（首次运行下载发行包）| 解压后用 `gradlew.bat` |
| **ModDevGradle 插件 2.0.146** | 2.0.146 | 构建插件 | ⬜ 首次构建时联网拉取 | 由 `build.gradle` 声明 |
| **Parchment 2024.11.17** | 1.21.1 版 | 参数名映射（可读性）| ⬜ 首次构建时下载 | 由 `gradle.properties` 声明 |
| **NeoForge 21.1.249** | 21.1.249 | 依赖/运行 | ⬜ 首次构建时下载 | 由 moddev 插件解析 |
| **vineflower** | 1.10.1 | 反编译 0.5.5 的 jar 做对照（移植基准）| ✅ 已有 | `E:\mod\SG2-1.21\.gradle-home\caches\modules-2\files-2.1\org.vineflower\vineflower\1.10.1\4f48c5947b21f9ebc743e7c80215ee839d3dc668\vineflower-1.10.1.jar` |
| **javap**（JDK 自带）| 17 或 21 | 看类签名/字节码（快速确认 API 是否还在）| ✅ JDK 17 里就有 | `C:\Program Files\Microsoft\jdk-17.0.10.7-hotspot\bin\javap.exe` |
| **git** | 2.54.0 | 版本管理（强烈建议一上来就 `git init`）| ✅ 已装 | PATH 上的 `git` |
| **1.21.1 NeoForge 游戏实例** | 21.1.249 或 21.1.250 | 进游戏实测 | ✅ 已有**两个** | `D:\MCJAVA\.minecraft\versions\1.21.1-NeoForge_21.1.249`、`…_21.1.250` |
| 解压工具 | — | 解压 MDK | ✅ PowerShell 自带 | `Expand-Archive` |

### 环境陷阱（会直接导致构建失败）

1. **PATH 上的 `java` 是 Java 8**（实测 `java -version` → `1.8.0_411`），且 `JAVA_HOME` 为空。
   → 不要依赖 PATH；要么让 toolchain/foojay 解析 JDK 21，要么显式设置 `JAVA_HOME` 指向 JDK 21。
2. **不要沿用 1.20.1 工程的 `--offline` 习惯**：这里第一次构建必须联网。
3. 1.20.1 那边"打包前先确认游戏没在跑"的纪律同样适用（jar 被占用会导致失败），但 NeoForge 的路径是
   `runs/` 与 `build/`，与 Forge 略不同，别混淆。

---

## 3. 输入材料（移植的原料）

| 材料 | 状态 | 说明 |
|---|---|---|
| **scguns 0.5.5 的 1.20.1 jar** | ✅ 有 | `D:\MCJAVA\.minecraft\versions\1.20.1-Forge_47.4.21\mods\ScorchedGuns-0.5.5-1.20.1.jar`（**19,173,074 B**）——这是**移植基准** |
| scguns 源码 | ⚠️ **版本不符** | `E:\mod\SCG2_TLM\temp_extract\scguns_src\Scorched-Guns-1.20.1-master` 的 `META-INF/mods.toml` 写着 **`version="0.4.7"`**（Forge 47.0.19、mappings 2023.06.26）。**直接拿它移植会得到 0.4.7，不是 0.5.5** ✗ |
| 0.5.5 源码（正版获取）| ❌ 待获取 | 优先找 0.5.5 的官方源码/仓库；拿不到就以 0.5.5 jar + vineflower 反编译为基准 |
| 生态依赖的 1.21.1 版本 | ❓ 待确认 | 至少需要：`scguns_cnc`（FE 能量枪与过热机制的来源）、`scgunsww1`、`scgextra`、`touhoulittlemaid`（1.21.1 的 TLM API 与 1.20.1 差异需重新核对）|

---

## 4. 必须保持不变的兼容契约（否则下游 mod 会一起碎）

以下都是 1.20.1 侧实测确认过、被下游依赖的面。移植时**改包名/改字段名/删字段**都会破坏它们：

- **物品类型**：`top.ribs.scguns.common.item.gun.RechargeableEnergyGunItem`（**由 scguns_cnc 提供**，extends `AnimatedGunItem`）；
  必须保留公共 getter：`getEnergyRequired()`、`getRefillCooldown()`、`getUsesOverheat()`、`getReloadRechargeTimeMult()`
- **换弹数据**：`Gun.Reloads` 的 `reloadTimer` / `emptyMagTimer`（即使没人读也别删）/ `maxAmmo` / `reloadType`；
  枚举 `ReloadType.{MANUAL, MAG_FED, SINGLE_ITEM}`（语义：MANUAL = 一发一发装、MAG_FED = 整匣、SINGLE_ITEM = 一个物品装满）
- **NBT 键**：`AmmoCount`、`PrevAmmoCount`、`HeatLevel`、`RechargeCounter`、`IsShooting`
- **类型链**：`GunItem` / `AnimatedGunItem`（前者是全部枪的基类，后者决定"换弹音是否由动画关键帧负责"）
- **工具方法**：`GunEnchantmentHelper.getRate/getQuickHands`、`GunModifierHelper.getModifiedReloadSpeed/getModifiedAmmoCapacity`、
  `Gun.getAmmoCount/hasAmmo/getReloads/getSounds`
- **行为/弹道**：`AIGunEvent.performGunAttack`、`ProjectileEntity.{getHitResult, onHit, findEntityOnPath, findEntitiesOnPath}`、
  `GunAttackGoal`（枪手怪 AI）、`ModEffects.LACERATED`（撕裂伤，药品会清除它）
- **数据/tag**：`#scguns:ammo`（创造弹药盒判定）、`data/scguns/guns/*.json` 的 `projectile.item` / `reloadType` / `maxAmmo` /
  `sounds.reload`（占位音，可保留）

---

## 5. 建议的第一步（不含实现）

1. 解压 MDK：`Expand-Archive .\MDK-1.21.1-ModDevGradle-main.zip -DestinationPath .\port`
2. 改 `gradle.properties`（mod_id/mod_version/group）与 `src/main/templates/META-INF/neoforge.mods.toml`
3. `gradlew.bat build`（**联网**；让 foojay 去解析 JDK 21）
4. 先确认空工程能构建 + `gradlew.bat runClient` 能起 1.21.1 实例，再开始搬 0.5.5 的代码
5. 全程 `git init` + 小步提交；每搬完一个包就构建一次，别攒到最后
