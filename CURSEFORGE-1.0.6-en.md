# Scorched Guns 1.0.6

**Minecraft 1.21.1 · NeoForge 21.1.150+**

Four fixes. Nothing was rebalanced — no gun stats, recipes or art changed.

---

## Fixes

**Gun benches and supply scamp packs no longer empty themselves.**
Put something in, save and reload, and it came back empty. The 1.21 `ItemStack#save` method returns the encoded data
instead of filling the tag passed to it, and the port had carried the 1.20.1 call over unchanged — so every slot was
written as `{}`. Both containers now save through vanilla's own `ContainerHelper`. Saves that were already emptied
cannot be recovered (the items were never written); a backup from before the loss will load fine.

**Raid gunners keep their AI after a chunk unloads.**
Gun-carrying raid mobs punched with their fists or mimed a bow after a reload, because a mob's gun AI lives in memory
only. Naturally spawned gunners get it re-attached on join; raid members were explicitly skipped. Fixed, and their
configured firing style is restored from the tag the raid wrote.

**Turrets turn their barrel on servers.**
Joining a server directly left turrets aiming but never moving — their definitions were loaded server-side and never
sent to clients. They now sync on join.

**The attachment screen zooms with the mouse wheel again.**
1.21 added a parameter to `mouseScrolled`, so the port's version was a harmless-looking overload that never ran.

---

## Notes

- Client and server both need this build.
- Container saves use vanilla's format now: this build reads old saves, old builds will not read new ones.
