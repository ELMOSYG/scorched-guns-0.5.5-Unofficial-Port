Changes in 1.1.4

Hotfix for a crash introduced in 1.1.3.

Fixed

- With Touhou Little Maid not installed, any living entity taking a bullet from this mod crashed the game: the maid
  particle handler referenced the maid entity class directly. It now matches on the entity namespace instead, and is
  registered only when the maid mod is present.
- The maid particle suppression mixin was registered under the scgextra configuration; it now lives in the main maid
  configuration so it works whenever the maid mod is installed.

Notes

- Without Touhou Little Maid, the maid compat registers no listeners and no mixins at all.
