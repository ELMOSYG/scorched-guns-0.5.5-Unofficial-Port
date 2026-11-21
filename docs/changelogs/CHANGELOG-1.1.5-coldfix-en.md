Changes in 1.1.5-coldfix

A follow up fix on top of 1.1.5. Please read the note first.

Important note

This issue is not guaranteed to be fully fixed. We can confirm that one path - stale magazine data being
refunded as ammo items - is closed, but whether that path alone explains every report still needs testing. If
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

Notes

- Only the magazine-repair path changed; ammo economy and loot are untouched.
