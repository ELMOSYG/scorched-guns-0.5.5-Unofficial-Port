# 1.0.9.2 hotfix — emergency hotfix

Released 2026-11-08.

## Fixed: attacking a mob while reloading kicked the player off the server

**Symptom.** Left-clicking a mob during a reload disconnected the client immediately, and the server log showed

```
Caused by: java.lang.RuntimeException: Failed encoding custom payload
   scguns:play/s2c_melee_attack: java.util.ConcurrentModificationException
io.netty.handler.codec.EncoderException: Failed to encode packet 'clientbound/minecraft:custom_payload'
```

**Cause.** The melee-animation packet `s2c_melee_attack` stored the player's held `ItemStack` by reference
instead of copying it. The packet is not sent at construction time; it is encoded later on the network thread,
while the server keeps ticking. Reloading mutates that same stack's tag (`scguns:IsReloading`,
`ReloadComplete`, `MeleeCooldown`), so the encoder ended up iterating the item's component/NBT collections while
the server thread was writing to them. That threw `ConcurrentModificationException`, the payload failed to
encode, and the connection was dropped.

**Fix.** `S2CMessageMeleeAttack` now copies the held stack in its constructor and encodes that snapshot, with a
guard for an empty stack. Attacking during a reload, including repeated clicks, no longer disconnects.

**Scope.** This was the only packet holding a live reference. Every other custom payload
(`S2CMessageBanzaiImpact`, `S2CMessageHotBarrelSync`, …) carries primitives or an immediate copy, and each one
was checked.

**Verified by.** 55 static audits, the 291/291 installed-jar check, and the same jar installed on both client
instances and the dedicated server.
