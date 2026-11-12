# 1.1.0 hotfix

Released 2026-11-12.

## The raygun made the game stutter when fired

**Symptom.** Firing the raygun stuttered; every other gun was fine.

**Cause.** `raygun.json` had `projectile.size` set to **100**. That field used to do nothing - the previous build fixed the hitbox refresh, so it finally took effect, and every shot carried a 100x100 block bounding box. Collision checks collapsed under it. `hullbreaker.json` had the same value.

**Fix.** The two data files now say 0.6 and 1.0, and `ProjectileEntity` clamps `size` to **0.05 - 4.0**, so a bad value can no longer cause this.

## Not a bug: laser weapons appear to disable view bobbing

Laser weapons (Laser Musket, the mining laser) are beam weapons: firing them means staying aimed, which is why the bobbing looks gone. Their data also sets `hasCameraShake: false`, which is the original author disabling recoil camera shake on purpose. The raygun is an ordinary automatic pistol and bobs normally.

## All gun projectile hitboxes are now 0.25

With the hitbox refresh fixed, the `size` values in the gun data started taking effect for the first time - and they were all over the place (56 guns at 0.05, 40 at 1.0, a few at 2.0 and 6.0). Fixing the refresh alone would have changed the hitbox of more than a hundred guns at once.

All 141 guns now say **0.25**, which is exactly what every projectile actually used before the fix. So the fix corrects the mechanism without changing how any gun feels, and a value like 100 can no longer do damage. The 0.05 - 4.0 clamp stays in the code as a second guard.
