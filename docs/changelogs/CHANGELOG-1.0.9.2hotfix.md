# 1.0.9.2 hotfix — 紧急热修复

**发布日期**：2026-11-08

## 修复：换弹期间攻击生物会导致玩家被踢出服务器

**现象**：装填过程中左键攻击生物，客户端立刻掉线，服务端日志出现

```
Caused by: java.lang.RuntimeException: Failed encoding custom payload
   scguns:play/s2c_melee_attack: java.util.ConcurrentModificationException
io.netty.handler.codec.EncoderException: Failed to encode packet 'clientbound/minecraft:custom_payload'
```

**原因**：近战动画用的 `s2c_melee_attack` 包直接保存了手持物品 `ItemStack` 的引用。这个包不是在构造时
立即发送，而是在网络线程稍后编码；而同一 tick 里换弹逻辑会修改这把手枪的 NBT（写入 / 移除
`scguns:IsReloading`、`ReloadComplete`、`MeleeCooldown`）。于是编码线程遍历该物品的组件 / NBT 集合时，
服务端线程正在往同一个集合里写，抛出 `ConcurrentModificationException`，包编码失败，连接被断开。

**修复**：`S2CMessageMeleeAttack` 构造时对手持物品执行 `copy()`，编码时只读这份快照；同时对空物品做了
保护。修复后换弹中攻击、连点攻击都不再断线。

**影响范围**：只有这一个包持有了活引用。其余自定义载荷（`S2CMessageBanzaiImpact`、
`S2CMessageHotBarrelSync` 等）都只带基本类型或即时拷贝的数据，已逐一确认。

**验证**：55 项静态审计通过；安装校验 291/291 通过；单机与专用服务器均已安装同一 jar。
