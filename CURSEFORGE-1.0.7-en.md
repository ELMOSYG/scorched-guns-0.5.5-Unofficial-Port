# Scorched Guns 1.0.7

**Minecraft 1.21.1 · NeoForge 21.1.150+**

Replaces the unreleased 1.0.6. Most of this is the Guard Villagers compat; one fix here matters beyond
guards: loot was handing out enchantments that do not belong on guns, and the anvil accepted any enchantment
book.

---

## Fixes

**Loot no longer puts the wrong enchantments on gear.**
`enchant_randomly` with no pool draws from the whole vanilla registry without checking the item, so guns could
arrive with Sharpness or Fortune. Gun entries now use the mod's own pool. The follow-up audit also caught a
regression from the previous attempt at this fix, where the gun pool had been applied to armour as well.

**Any enchantment book could be applied to a gun at an anvil.**
`Item#isBookEnchantable` answers true for everything, so books are now checked one enchantment at a time: guns take
the mod's enchantments plus Unbreaking and Mending, mining guns also take Efficiency, Silk Touch and Fortune, and
everything else — Density included — is refused.

**Guards no longer swap weapons the moment they spawn.**
Two systems were arming the same hand. Natural guards now get their gun from Guard Villagers' own equipment table
(5 early guns, ~38%), and a weapon a player handed a villager is never taken away.

**Guard gun AI.**
They hold fire until the target is inside the gun's range instead of shooting at anything they can see; the
friendly-fire check now walks the whole shot instead of a six-block box (and retries a blocked shot after a pause
rather than shuffling in place); reloads use the gun's real reload time, actually take time, finish for
single-shot guns, and survive the goal being rebuilt.

**Guards hold a gun with their arms tracking their head,** the way the vanilla crossbow pose does, so the barrel
follows a target above or below them. They crouch while reloading.

**Also:** the attachment screen zooms with the wheel again, containers no longer empty themselves on reload (1.21
changed what `ItemStack#save` returns), raid gunners keep their gun AI across a chunk reload, and turrets turn again
for players who join a server directly.

---

## Notes

- Client and server both need this build.
- Guard Villagers is optional: its compat data is inert when the mod is absent.
