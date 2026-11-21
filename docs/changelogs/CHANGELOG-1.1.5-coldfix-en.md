Changes in 1.1.5-coldfix

A follow up fix on top of 1.1.5. Please read the note first.

Important note

This issue is not guaranteed to be fully fixed. What we did: closed the write paths that could store an oversized
magazine while a gun definition had not resolved yet, while keeping the existing behaviour of refunding rounds when
an attachment makes the magazine smaller. If ammo still appears when you pick up a gun, please include:

- port version, modpack name and mod list
- whether the player was in creative
- which gun, how many rounds, and which ammo item
- latest.log from the reproduction

Fixed

- The creative mode automatic magazine fill now waits until the gun definition has actually resolved. Without a
  synced definition the item falls back to the empty default, whose magazine is 30 rounds, so guns that hold one
  were written with thirty.
- The reload paths do the same check: they refuse to write a magazine count while the definition has not resolved,
  which is what let the default 30 reach guns with a smaller magazine.
- Capacity changes caused by attachments (removing an extended magazine, for example) still refund the surplus,
  because those rounds were genuinely loaded.

Notes

- The change is limited to the definition check before magazine data is written; ammo economy, recipes and loot are
  untouched.
- Magazine data already corrupted by an older build is still corrected when the stack changes hands; the surplus in
  that case is refunded as real ammo.
