# 1.0.5 性能优化总结

这一版只做性能与健壮性，不改枪械数值、不改配方、不改美术。

## 结果

| 场景 | 优化前 | 优化后 |
|---|---|---|
| 穿满配件外骨骼 | 90 FPS | 320 FPS |
| 空外骨骼 | 300 FPS | 与不穿时接近 |
| 不穿外骨骼 | 500 FPS | 600 FPS |
| 枪械 HUD（逐行 → 批量） | 240 FPS | ~400 FPS |
| 外骨骼模型闪烁 | 有（1~2 帧消失 / 多人串位） | 无 |
| 枪械数据加载失败 | 整个存档进不去 | 跳过坏文件，其余照常 |

外骨骼与 HUD 的采样里，本模组自身的代码从「渲染热点前列」降到约 0.06%；
剩下的开销是 GeckoLib 的 3D 骨骼提交与着色器/顶点上传，属于渲染管线的固有成本。

## 一、外骨骼渲染

问题：`ExoSuitRenderer` 每根骨骼都会走一遍可见性判断，而每个判断都重新读升级槽的 NBT
（`ExoSuitData.getUpgradeInSlot`）并在注册表里线性查找升级定义。三十来根骨骼、每帧一次，
于是每帧上百次 NBT 解析。玩家反馈"穿着外骨骼就掉性能、大整合包更明显"即由此而来。

| 改动 | 说明 |
|---|---|
| `ExoSuitStateCache` | 每客户端 tick 解析一次四件护甲 + 各自的四个升级槽，其余地方只读缓存 |
| 渲染器改读缓存 | `ensureUpgrades` 从缓存填充，渲染路径不再碰 NBT |
| `cachedUpgrade` | 注册表查找也先查缓存，未命中才回退 |
| `setHiddenIfChanged` | 36 处 `bone.setHidden(...)` 改为值变了才写（GeckoLib 会因此标脏重建） |
| 缓存只服务本地玩家 | 之前是全局单例，渲染其他玩家时读到本地的配置，导致外观错乱与闪烁 |
| `invalidate()` 立即重建 | 之前是清空，渲染时用空护甲重建，导致升级模型消失 1~2 帧 |
| `ExoSuitData` 槽读取缓存 | 50ms 时间窗 + 引用校验，HUD 与能量管理器的重复读取一并受益 |

## 二、枪械数据与附件

| 改动 | 说明 |
|---|---|
| `modifiedGunCache` 换键 | 原本是 `WeakHashMap<整个物品 NBT, Gun>`：NBT 里含弹药数，打一发就失效；
又因每次 `getTag` 都返回新对象，弱引用立刻被回收，缓存几乎从不命中，于是每次查询都深拷贝并
反序列化整个 Gun。现在按 `Gun` 段的结构哈希做键、`HashMap` + 上限，弹药变化不再重建 |
| `GunAttachmentCache` | 附件查询是渲染路径上最频繁的本模组调用，改为 50ms 窗口缓存，一次填满六种类型 |

## 三、HUD

问题：`HUDRenderHandler` 每两帧重算一次状态文字，而 `GuiGraphics.drawString` 在
overlay 事件里每次都会重新校验缓冲源，于是每行文字提交一次批次。采样显示渲染线程约 15%
耗在这条 flush 路径上，而字形本身只有约 2%。

| 改动 | 说明 |
|---|---|
| 改为 `Font.drawInBatch` | 同一 HUD 的多行写进同一个缓冲，方法末尾 flush 一次 |
| 每方法各自 flush | flush 必须在该 HUD 的 pose 作用域内；之前集中到最后一处，弹药顶点被拖到
后面的坐标系提交，于是画到准星左上角，并且与正常那份重复 |
| 补 alpha | `drawString` 会补不透明 alpha，`drawInBatch` 不会，颜色来自 `ChatFormatting` 时
alpha 为 0，文字会被画成全透明 |
| 补阴影与基线 | `drawString` 收的是行顶、`drawInBatch` 收的是基线，且默认不画阴影 |

## 四、顺带的两项

- **模型骨骼定位机瞄相机**：`camera_aim` / `aim_camera` / `iron_sight_camera` / `camera`
  任一骨骼可决定 ADS 相机位置，缺失时回退到 `modules.zoom`，141 把原版枪行为不变。
- **相机骨骼缓存 1 秒过期**：重新导出模型后 F3+T 即可生效，不必重启。

## 五、健壮性

- **逐文件容错**：`NetworkGunManager` 对每个枪械定义单独 try/catch，坏文件跳过并在日志里点名，
  不再让一个坏 JSON 导致整个存档无法进入。
- **Gson 自检**：一次性对 `Gun` 走一遍反序列化，失败时把完整堆栈写进日志。
- **事故记录**：本版曾把附件缓存作为静态字段放进 `Gun`。`NetworkGunManager` 的
  `excludeFieldsWithModifiers(new int[]{128})` 只排除 `TRANSIENT`，覆盖了 Gson 默认对
  `STATIC` 的排除，于是那个含 `ItemStack` 的静态字段把整套数据组件类型图拖进反射，碰到
  `java.util.regex.Pattern` 的私有字段后 JDK 21 拒绝访问，所有枪械定义解析失败。
  解决方式是把缓存挪进独立的 `GunAttachmentCache`，让 `Gun` 的形态保持原样。

## 六、复现与验证方式

- 采样：客户端 `/spark profiler start`，穿满配件站 20 秒后 `stop`，看 Flat 视图。
- 判定标准：本模组的类不应再出现在热点前列；`MSPT` 与 TPS 不受影响（服务端一直健康）。
- 计数核对：`tools/audit_*.py` 全绿、`tools/verify_installed_jar.py` 289/289。
