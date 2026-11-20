Changes in 1.1.4

Hotfix for a crash introduced in 1.1.3.

About the maid blood particles

That change was not a random addition. It came from concerns raised in the Touhou Little Maid community: many players
do not want to see companions bleeding when they are hit. The intent was narrow - keep this mod's blood effects off
maids, and change nothing else.

The 1.1.3 crash was an unintended side effect of how that was implemented, not of the intent itself: the handler
referenced the maid mod's entity class directly and was registered unconditionally, so without Touhou Little Maid
installed the first bullet hit crashed the game.

Fixed

- With Touhou Little Maid not installed, any living entity taking a bullet from this mod crashed the game: the maid
  particle handler referenced the maid entity class directly. It now matches on the entity namespace instead, and is
  registered only when the maid mod is present.
- The maid particle suppression mixin was registered under the scgextra configuration; it now lives in the main maid
  configuration so it works whenever the maid mod is installed.

Notes

- Without Touhou Little Maid, the maid compat registers no listeners and no mixins at all.
