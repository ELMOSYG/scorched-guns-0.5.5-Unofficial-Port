# Scorched Guns — 1.0.6

**Minecraft 1.21.1 · NeoForge 21.1.150+**

Three fixes. All of them are problems the mod has had since well before 1.0.5 — two of them since the very first
port, and one that an earlier release only half-fixed.

---

## Containers no longer empty themselves

**Put anything in a gun bench, or in a supply scamp's pack, save and reload — it came back empty.**

This is the release's headline fix, and it is two bugs stacked on top of each other.

The first one was known: the bench saved each slot with `ItemStack#save`, which throws
`Cannot encode empty ItemStack` for an empty stack. A bench always has empty slots, so every chunk save threw, the
block entity was dropped, and nothing was written at all. 1.0.4 added a skip for empty slots, and that half was
genuinely fixed.

The second one was never found. Loading used `NbtHelper.itemFromTag(tag)` — the overload without registries — which
reads a **static** registry access populated when a level loads. A chunk that loaded before that point decoded every
slot as an empty stack. The items were being written correctly; they just could not be read back. So after 1.0.4 the
bug stopped being "always lost" and became a race: sometimes fine, sometimes empty. It was not a regression — it was
a second defect that the first one had been hiding.

Both the bench and the scamp now decode with the registries their level already holds.

**Can an affected save be rescued?** Possibly. The item NBT was written normally, only the read failed — so a chunk
that has not been saved again since it came back empty will read its contents back after this update. If the chunk
was saved again in the meantime, the empty container overwrote it. Try a backup from before the loss.

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
