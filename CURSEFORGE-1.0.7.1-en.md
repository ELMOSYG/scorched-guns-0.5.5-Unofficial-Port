# Scorched Guns 1.0.7.1

**Minecraft 1.21.1 · NeoForge 21.1.150+**

A small follow-up to 1.0.7. The mod's own version stays 1.0.7; only the file name changes.

Two of these were reported after 1.0.7 and were serious: **guards never actually received their gun AI**, and
**firing a gun turned the shooter's own summons against them**.

**Guards now use the guard gun AI at all.** Every guard fix before this build - reloading, firing range, the
friendly-fire test, the aiming pose - lived in `GuardGunAttackGoal`, but the code that arms a gunner attached the
raider goal instead. Once guards started getting their guns from Guard Villagers' equipment table, that class was never
added, so none of it ran.

**Guards reload.** An empty gun was not treated as an empty magazine (a loot-table gun starts with no ammo at all),
so a guard would neither shoot nor reload. Reload progress now persists across the goal being rebuilt, and
single-shot weapons size their magazine correctly.

**Guards choose where to shoot from.** When an ally blocks the line, a guard samples positions around itself and picks
one that is in range, unobstructed, and clear of allies, preferring the gun's own ideal distance.

**A shield guard closes in instead of standing there**, lowering the shield while it advances and letting Guard
Villagers raise it again once inside firing range. Crouching while aiming is now the default.

**A gunshot no longer angers your own summons or pets.** Nearby monsters are still alerted - that is the point of the
setting - but anything owned by, allied to, or summoned by the shooter is exempt, including Iron's Spells summons,
which use their own owner interface.

**Gunners no longer seem to drop a stack of guns.** They were being re-armed, and each re-arm dropped the previous gun
on the ground. A mob that was re-armed a few times left several guns behind. The main hand is now only written when it
is empty.

**The monster guidebook has pages and opens again.** The book a supply scamp or viventrum package hands out had
no text and refused to open: its contents were still written as the 1.20 tags (`title`, `author`, `pages`) while
1.21 reads a `WRITTEN_BOOK_CONTENT` component - and a book that is not marked resolved is treated as still being edited
and will not open at all. Both package items are fixed, since they share the same helper.

**The netherite respirator and the ridgetop hat no longer stack to 64.** Both were registered without a stack
limit, unlike every other armour piece in the mod, so the default of 64 applied and armour could pile up in a slot.

**Armour on mobs shows its real model.** The cog minion and the viventrum draw armour through their own render layer,
and that layer had two problems: it only drew the inner helmet model, where vanilla helmets are an inner shell plus an
inflated outer one, and it always used the vanilla head proxy as the model - so even GeckoLib armour came out as a cube
with an armour texture on it. Both are fixed; the layer now asks the item for its client extension and falls back to the
vanilla model only when there is no such extension.

**Armed guards move faster by default.** `guard_gun_move_speed` now defaults to 1.5 instead of 1.0. The multiplier
feeds into the guard's base pace (0.6 when closing in, 0.65 when backing off, matching Guard Villagers' own rhythm);
at 1.0 a gunner took noticeably longer than a melee guard to reach an enemy. The range is unchanged, so anyone who
preferred the old pace can set it back to 1.0.

**Armed guards fight back when something hits them.** Guard Villagers registers its own retaliation goal, but it
never fired on armed guards: the goal selector keeps a single goal per flag, and ours already held the target flag. The
first attempt made it a separate goal, which is why the guard only ever retaliated once. The check now lives inside the
gun goal's tick, so it runs every tick the guard is fighting. The same hit only re-targets once (tracked by timestamp),
and friendly fire, creative players and spectators are ignored.

**Guards no longer walk into a pack while keeping their distance.** Picking a firing position checked range, terrain
and allies in the line of fire - but not whether the spot itself was surrounded. A spot could pass every check and still
sit in the middle of a group. Positions with three or more monsters within four blocks are now rejected, and the rest
are scored with a per-monster penalty so open ground wins. If no sample is clear, the guard keeps shooting from where it
is instead of standing still.

---

## Notes

- Client and server both need this build.
- 1.0.7.1 is a rename of the 1.0.7 jar; nothing else changed in the manifest.
