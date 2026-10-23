# Scorched Guns — 1.0.6

**Minecraft 1.21.1 · NeoForge 21.1.150+**

Three fixes. All of them are problems the mod has had since well before 1.0.5 — two of them since the very first
port, and one that an earlier release only half-fixed.

---

## Containers no longer empty themselves

**Put anything in a gun bench, or in a supply scamp's pack, save and reload — it came back empty.**

Ask the block for its data and you saw the signature: `Item11: {}`. The key was there, the contents were not.

**The cause is a 1.21 API change that the port carried over from 1.20.1 unchanged.** Upstream 0.5.5 writes:

```java
CompoundTag itemTag = new CompoundTag();
itemstack.save(itemTag);          // 1.20.1: fills in and returns that same tag
tag.put("Item" + i, itemTag);
```

In 1.21, `ItemStack#save(...)` **returns** the encoded tag instead of filling the one you hand it. Ported as-is, the
tag stayed empty `{}` for every slot — no id, no count, no components. Loading skips an empty tag, and the next save
writes nothing at all.

So the bug had three layers, each hiding the next:

| Layer | Status |
|---|---|
| `save` throws `Cannot encode empty ItemStack` for an empty slot, and the block entity gets dropped | fixed in 1.0.4 (skip empty slots) |
| A non-empty stack cannot be written either, because the return value carries the data | **fixed here** |
| Loading decoded through a static registry that may not be initialised yet | also fixed here |

This is why 0.5.5.3's fix appeared to work and then came back: the port re-introduced the second layer by treating the
1.21 method as if it were the 1.20.1 one.

Both containers now save through vanilla's `ContainerHelper.saveAllItems` / `loadAllItems`, the same path the mod's
ammo boxes, composter and ammo module already use. Old saves that still carry the previous `Item<n>` keys or the
`Slot`-tagged list are read as a fallback, so changing the format does not throw anything away.

After the fix the block data reads:

```json
Items: [{count: 1b, Slot: 11b, id: "scguns:iron_blueprint"}]
```

**Old saves cannot be rescued by this build** — the items were never written, only empty tags were. A backup from
before the loss is fine: it holds real NBT and will load on this version.

---

## Raid gunners keep their AI across chunk reloads

**Gun-carrying mobs from a raid fight with their fists (zombies) or mime an archery animation (skeletons,
pillagers) instead of shooting. Worse after a reload.**

The gun AI is a goal, and goals live in memory — they are not part of an entity's saved data. Unload a chunk and the
entity is rebuilt from scratch with only its vanilla goals: a zombie's melee, a skeleton's bow.

Naturally spawned gunners get repaired when they join the world, because `GunnerMobSpawner.reassessWeaponGoal` runs
on join. That method had an early return for anything tagged `RaidMember_`, and raid members are exactly the mobs
that carry that tag — so raids were the one group of gunners nothing ever repaired, and the first chunk reload
downgraded them permanently.

The early return is gone. Raid members record their firing profile in an `AI_<TYPE>` tag, which *is* saved, so the
repaired goal keeps the style their raid config asked for rather than a random one. The goal also moved to priority
0: a vanilla mob registers its own attack goal at 2 or later, equal priorities run in registration order, and the
vanilla goal is always registered first — a tie at 2 is a loss.

---

## Turrets turn their barrel on servers

**Join a server directly and turrets aim and fire but never move. Visiting a single-player world once "fixes" it.**

Turret definitions were loaded by a server-side data listener and never sent to clients. A turret's `tick()` returns
early while its definition is missing, so on the client it did nothing at all and the barrel never turned.

The single-player detour worked only because the client and server share a JVM there: the static definition map the
server filled was visible to the client's renderer. Nothing was actually repaired — shared memory was covering for a
missing packet.

Definitions now ride to the client on join as a normal play message, the same way the addon upgrade registry already
did.

---

## Verification

- All 53 gate scripts pass. One of them, `audit_override_drift.py`, rejected an earlier attempt at the container fix:
  it had added a two-argument `readAdditionalSaveData` overload to an entity, and 1.21.1's `Entity` only declares the
  one-argument form — the overload would never have run. The gate caught a fix that would have looked correct and
  done nothing.
- Install verification 291/291.
- All three fixes are server-side behaviour, so client and server both need this build.
