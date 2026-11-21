Changes in 1.1.5-coldfix

A follow up fix on top of 1.1.5. Please read the note first.

Important note

This issue is not guaranteed to be fully fixed. What is confirmed: the two paths that could write an oversized
magazine into a gun, and the one path that turned such a surplus into ammo items, are all closed (see the list
below). Whether those paths alone explain every report still needs testing. If
ammo still appears when you pick up a gun, please include:

- port version, modpack name and mod list
- whether the player was in creative
- which gun, how many rounds, and which ammo item
- latest.log from the reproduction

Fixed

- The server no longer converts a magazine count above the gun's own capacity into ammo items when it clamps
  that count down. An older build wrote the default 30 round magazine into every gun, so a flintlock with a
  capacity of 1 produced 29 phantom rounds - one left in the gun, twenty nine into the inventory. That stale
  surplus is now clamped silently.
- Capacity changes caused by attachments (removing an extended magazine, for example) still refund the rounds
  that were genuinely loaded.
- The creative mode automatic magazine fill now waits until the gun definition has actually resolved. Before that,
  a moment without a synced definition fell back to the empty default, whose magazine is 30 rounds, so guns that
  hold one were written with thirty.
- Reading a gun's definition now clamps an oversized magazine count down to that gun's legal maximum, silently and
  without turning the surplus into ammo items. However the count was corrupted and however the stack changes hands,
  the HUD, the reload tracker and the leftover-ammo handler all see the same legal value.

Notes

- Only magazine data changed - the write path, the read-time clamp and the overflow handler. Ammo economy,
  recipes and loot are untouched.
