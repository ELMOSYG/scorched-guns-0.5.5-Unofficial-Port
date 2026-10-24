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

---

## Notes

- Client and server both need this build.
- 1.0.7.1 is a rename of the 1.0.7 jar; nothing else changed in the manifest.
