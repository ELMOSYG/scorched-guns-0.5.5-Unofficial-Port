# Scorched Guns 1.0.9.2

**Minecraft 1.21.1 · NeoForge 21.1.150+**

A behaviour pass: one recoil case, one mob's AI, three collision boxes and one charge step.

## Jumping no longer cancels the recoil kick

1.0.9 added a 0.08 lift cap so that firing while standing on a physics structure would not slowly push the player off
the deck. That cap was meant for the grounded case, but it also got applied to the airborne one - so firing downwards
mid-jump, which should give a 1.25x upward kick, was flattened to 0.08 and felt like no recoil at all.

| Situation | Vertical recoil |
|---|---|
| Standing on the ground | at most 0.08, so sustained fire cannot climb |
| Airborne, aiming down | the full 1.25x |

## The trauma unit: hook and AI

Four problems were stacked on top of each other here.

- **The hook caught its own thrower.** Vanilla `FishingHook` resolves its owner through `getPlayerOwner()`, which is
  null for a mob, so `entity != getOwner()` was always true. The thrower is now tracked by the hook itself and
  compared by UUID.
- **The client never received the hook.** The spawn packet only carries a player owner, and the client discarded
  anything else. The hook is now a plain `Projectile`, which carries its owner in the packet by itself.
- **The line passed through creatures and never went away.** The client was integrating its own gravity while the
  server had already stopped the hook on a target. The client now only follows the server.
- **Two lines at once.** The AI looked for its own hook with `getOwner()` - also null - so it threw another one every
  cooldown. It now asks the hook for its thrower.

The hook also has a hard 100 tick lifetime, so a stray one always disappears.

## A charging cog knight can step over a one block rise

It used to be stopped by a single block step, so it could not reach a target standing on one. While charging it now
gets +0.5 step height (1.1 instead of the vanilla 0.6), applied as an attribute modifier that is removed when the
charge ends.

## Collision boxes for a few mobs

| Mob | Before | After | Why |
|---|---|---|---|
| Dissident | 1.4 x 1.7 | 1.4 x **2.0** | the model is taller than the box |
| Hornlin | **1.4** x 1.7 | **0.8** x 1.7 | too wide for a slender model |
| Zombified hornlin | **1.4** x 1.7 | **0.8** x 1.7 | same |

## Verification

- Build passes, 55 static audits pass, install verification 291/291
- Client and server share the same jar
