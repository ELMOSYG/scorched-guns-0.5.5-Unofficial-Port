# Scorched Guns 1.0.8

**Minecraft 1.21.1 · NeoForge 21.1.150+**

**Other players can see your gun's bolt and barrel again.** On your own screen a gun looked complete, but in
multiplayer other players saw a gun missing a whole section: on several weapons the `bolt` bone is not just the
charging handle but part of the barrel and handguard, so their view showed a barrel-less gun. Every one of the 82 gun
models only drew that bone for the local player, so it was never rendered for anyone else. It is now drawn for every
entity; the recoil offset still reads only the local player's cooldown, so nothing shifts out of place.

**The cooldown lookup is null-safe.** Rendering happens with no local player present - item frames, dropped items, the
main menu - and opening the render path to everyone made that read a real hazard. All 87 reads now fall back to zero
when there is no local player. Note that another player's bolt still does not move with their own shots: that state
cannot be derived client-side, and the version that tried to sync it did not work and was reverted.

**The log is quiet again.** Three `LOGGER.info` calls in the blood particle ran every frame, and nine `System.out`
printers (exosuit ammo, upgrade sync, ammo box, exosuit renderer) bypassed log levels entirely. Those are now
`DEBUG`, alongside nine leftover `SCGUNS-*` probes, and there is no `System.out` left in the mod. Set the `scguns`
logger to `DEBUG` if you want them back.

**Piglin soldiers no longer start a civil war.** The mod's two hornlin soldiers are piglins in theme but not in class,
and vanilla's only piglin classes do not retaliate for friendly fire - so one stray round made every nearby piglin
hostile. Piglins now ignore damage from the soldiers, and the soldiers no longer retaliate against piglins.

**Mobs stop swinging guns like clubs.** Combat mods that animate mob attacks classify ranged weapons by vanilla types,
and guns are none of them, so a gunner was given a melee swing. The swing is now cancelled client-side for any mob
holding a gun; server logic is untouched. With `Better Mob Combat - Reimagined`, setting `enableFallbackMeleeAnimations`
to false removes it at the source.

---

## Notes

- Client and server both need this build.
