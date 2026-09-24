#!/usr/bin/env python3
"""Make the structure-impulse path defensive and pull the punch strength safely.

Two edits:
  * read Sable's punch-strength config defensively (it can be touched before it is loaded);
  * wrap the callers in a try/catch so a physics-mod reaction can never take gunfire down.

Usage:
    python tools/patch_impulse_guard.py [--apply]
"""
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HELPER = os.path.join(ROOT, 'src', 'main', 'java', 'top', 'ribs', 'scguns', 'util', 'PhysicsStructureHelper.java')
PROJECTILE = os.path.join(ROOT, 'src', 'main', 'java', 'top', 'ribs', 'scguns', 'entity', 'projectile', 'ProjectileEntity.java')
TURRET = os.path.join(ROOT, 'src', 'main', 'java', 'top', 'ribs', 'scguns', 'entity', 'projectile', 'turret', 'TurretProjectileEntity.java')

HELPER_OLD = """            double punchStrength = SableConfig.SUB_LEVEL_PUNCH_STRENGTH_MULTIPLIER.getAsDouble();"""
HELPER_NEW = """            double punchStrength;
            try {
                punchStrength = SableConfig.SUB_LEVEL_PUNCH_STRENGTH_MULTIPLIER.getAsDouble();
            } catch (RuntimeException e) {
                // Sable's config can be read before it is loaded; fall back to its own default.
                punchStrength = 1.0;
            }"""

PROJECTILE_OLD = """      PhysicsStructureHelper.applyShotImpulse(this.level(), hitVec, this.getDeltaMovement(),
         punchesPerTenDamage * (double)this.getDamage() / 10.0);"""
PROJECTILE_NEW = """      try {
         PhysicsStructureHelper.applyShotImpulse(this.level(), hitVec, this.getDeltaMovement(),
            punchesPerTenDamage * (double)this.getDamage() / 10.0);
      } catch (RuntimeException e) {
         // A physics-mod reaction must never take gunfire down with it.
         ScorchedGuns.LOGGER.debug("Physics structure impulse failed", e);
      }"""

TURRET_OLD = """      PhysicsStructureHelper.applyShotImpulse(this.level(), hitVec, this.getDeltaMovement(),
         punchesPerTenDamage * this.getBaseDamage() / 10.0);"""
TURRET_NEW = """      try {
         PhysicsStructureHelper.applyShotImpulse(this.level(), hitVec, this.getDeltaMovement(),
            punchesPerTenDamage * this.getBaseDamage() / 10.0);
      } catch (RuntimeException e) {
         // A physics-mod reaction must never take gunfire down with it.
         ScorchedGuns.LOGGER.debug("Physics structure impulse failed", e);
      }"""

EDITS = [
    (HELPER, 'punch strength read defensively', HELPER_OLD, HELPER_NEW),
    (PROJECTILE, 'guard the projectile hook', PROJECTILE_OLD, PROJECTILE_NEW),
    (TURRET, 'guard the turret shell hook', TURRET_OLD, TURRET_NEW),
]


def main():
    apply = '--apply' in sys.argv
    contents = {}
    done = 0
    for path, label, old, new in EDITS:
        if path not in contents:
            with open(path, 'r', encoding='utf-8') as fh:
                contents[path] = fh.read()
        text = contents[path]
        rel = os.path.relpath(path, ROOT).replace('\\', '/')
        if new in text:
            print('skip %-62s %s' % (rel, label))
            continue
        if text.count(old) != 1:
            print('FAIL %-62s %s (anchor %d)' % (rel, label, text.count(old)))
            return 2
        contents[path] = text.replace(old, new, 1)
        done += 1
        print('ok   %-62s %s' % (rel, label))

    if apply:
        for path, text in contents.items():
            with open(path, 'w', encoding='utf-8', newline='') as fh:
                fh.write(text)
        print('applied (%d change(s))' % done)
    else:
        print('dry run (use --apply)')
    return 0


if __name__ == '__main__':
    sys.exit(main())
