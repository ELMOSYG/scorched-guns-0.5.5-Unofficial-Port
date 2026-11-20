Changes in 1.1.5

Fixes a bug that could one-shot players.

Fixed

- Shatter round shrapnel no longer hits the target the round itself hit. On 1.20.1 that target's invulnerability
  window absorbed most of the shrapnel; 1.21.1 no longer absorbs it, so a point blank or leg hit dealt the round's
  own damage plus roughly twenty fragments - fifty damage or more in a single hit. Mobs armed with shatter rounds
  (scg-extra's FAC trench troops, for example) were killing players outright.
- Behaviour now matches 1.20.1: the entity that was hit takes the round's damage only, while nearby entities still
  take shrapnel damage, so area suppression is unchanged.

Notes

- This is a deliberate port-side change that aligns the port with upstream 1.20.1; only shrapnel target selection
  is affected.
