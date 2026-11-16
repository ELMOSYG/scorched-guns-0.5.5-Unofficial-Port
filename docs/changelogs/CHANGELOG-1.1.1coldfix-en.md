# Scorched Guns 1.1.1coldfix (1.21.1 / NeoForge)

## Main fix: projectile accumulation

**Symptom**: projectiles pile up in the world - `/tp @e` can pull out a hundred at once, usually parked at the
edge of the simulation distance.

**Cause**: when the simulation distance is lower than the render distance, the band between them is loaded but not
entity-ticking. A round that enters it stops being ticked, so its lifetime never advances and it stays there
forever, still visible to entity radars. Those entities were also written into chunk saves, so they accumulated
across reloads.

**Fix, three layers**:
- A 125 block flight limit (eight chunks of simulation distance is exactly 128, so three blocks of margin). The
  round is gone once it flies that far and nothing depends on chunk state.
- A round that enters a region that will not tick is discarded immediately instead of stopping at the edge.
- The server sweeps this mod's projectiles beyond the simulation distance every ten seconds, clearing ones that
  were already parked.
- Projectiles are no longer written to chunk saves (`shouldBeSaved() = false`).

## Also fixed

- **Colourless trails on special ammo**: 139 of 141 gun files have no `trail_color`, and deserialisation read the
  missing value as 0 (transparent black), overriding the default. A missing key now keeps the default colour.

## Known issue (upstream behaviour, unchanged)

- Pressing unload during a per-round reload makes the gun reload forever without spending ammo, and Cowell-style
  revolvers can generate casings out of nothing. Workaround: do not press unload while the reload animation plays.
  Details and the reason our patches were reverted are in `docs/reload-system.md`.

## Notes

- Version `1.5.0+1.1.1coldfix`, file `scguns-1.1.1coldfix.jar`.
- Requires Minecraft 1.21.1 and NeoForge 21.1.x.
