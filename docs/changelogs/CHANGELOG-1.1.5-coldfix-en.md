Changes in 1.1.5-coldfix

A follow up fix on top of 1.1.5. Please read the note first.

Important note

This is not confirmed to be fully fixed, because the problem normally only shows up on a dedicated server: in
single player the gun definitions are always resolved and item handovers have no sync delay, so it is very hard to
reproduce locally. What we did is close the write paths that could store an oversized magazine while a definition
had not resolved yet, and make the "air magazine" kind of stale data recognisable.

If ammo still appears when you pick up a gun, please include:

- port version, modpack name and mod list
- whether any of these happened first: dropping and re-picking the gun, changing hands, reconnecting, changing dimension
- which gun, how many rounds, and which ammo item
- latest.log from both server and client at the moment of the reproduction

Fixed

- The creative mode automatic magazine fill now waits until the gun definition has actually resolved. Without a
  synced definition the item falls back to the empty default, whose magazine is 30 rounds, so guns that hold one
  were written with thirty.
- The reload paths do the same check and refuse to write a magazine count while the definition has not resolved.
- The surplus correction now tells two cases apart precisely. When the gun's definition answers with an air
  projectile (the "air magazine" state), the surplus is corrected silently and never converted into ammo items. When
  the definition is real - an extended magazine just removed, for example - the surplus is refunded as real ammo.
- Reading a gun's definition no longer clamps the magazine count, so rounds that are genuinely still loaded are not
  swallowed.

Notes

- The change is limited to the definition check before magazine data is written and to the surplus handling; ammo
  economy, recipes and loot are untouched.
- Stale magazine data left by an older build is still refunded as real ammo when its definition is otherwise valid;
  only a stack whose definition really is the air default is corrected silently.
