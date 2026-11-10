# 1.1.0

Released 2026-11-10.

Two things in this one: a hitbox bug, and Entity Model Features compatibility. The maid compat moves to 1.2.5.

## 1. `projectile.size` did nothing

The field was in the gun JSON, the code read it and built an `EntityDimensions` out of it - but the **hitbox never
changed**.

It was an ordering problem. `Entity` computes its bounding box in its own constructor from `getDimensions()`, and our
`entitySize` is assigned after that, with no `refreshDimensions()` anywhere in the class. So whether the JSON said 0.5
or 100, every projectile kept the default box.

`refreshDimensions()` is now called in both places: in the constructor, and when the client rebuilds the projectile
from the spawn packet.

> `size` is in blocks, not pixels. 3.0 means three blocks. 0.5 to 2.0 is the useful range.

(GitHub issue #5.)

## 2. Entity Model Features / Fresh Animations

**To be clear: this compatibility is partial and cannot be fully fixed.** EMF replaces the player model wholesale, so
anything that poses the vanilla model stops working underneath it. Four things were done within that limit:

1. **The `is_scguns_gun_held` animation variable is registered**, so a resource pack can switch animation on it.
2. **The held pose is reapplied** at the tail of EMF's `EMFModelPartRoot#animate`, onto `right_arm`, `left_arm` and
   `head`, with the sleeves and hat copied along.
3. **The vanilla hand pass is cancelled** while a gun is held. EMF draws the first-person arm through
   `ItemInHandRenderer#renderHandsWithItems`; that is the method that has to be skipped, not
   `PlayerRenderer#renderHand` (which is never reached on this path - that injection was dead code).
4. **Arm and sleeve now come from one baked model.** They used to come from two different models: the arm followed
   the gun and the sleeve stayed behind.

Every EMF access goes through cached reflection: class, method and field resolved once, a failed probe leaves one
debug line instead of throwing per frame. Both mixins are `@Pseudo` with `require = 0` and a `Throwable` guard, so
nothing changes when EMF is absent.

**If an old model sticks around after swapping a model pack**, press F3+T or restart - that is EMF's own cache.

## 3. Maid compat 1.2.5

### 3.1 Bow and crossbow maids were driven by the gun strafing logic

`MaidAttackStrafingTaskMixin` cancelled `tick` unconditionally and substituted the gun behaviour. But
`MaidAttackStrafingTask` is shared by bows, crossbows and guns, so archers stopped keeping their distance and walked
into melee range before drawing.

The mixin now checks the main hand first: if it is not one of this mod's guns it returns without cancelling, and
Touhou Little Maid's own strafing keeps running.

### 3.2 The air condition line

A new "air canister has enough air (air guns only)" line. Testing "has air" alone marked ordinary guns as true,
because "no air" does not apply to a gun that does not use air. The line now asks whether it is an air gun first.

### 3.3 Air is charged per shot

Checked: 1.21.1 already charges once per shot, not per projectile (`performGunAttack` charges once, and the projectile
hook only asks). A diagnostic line was added to make the numbers readable:

```
scguns-maid air: gun=<item> cost=<cost of this shot> stored=<stored before> canister=<canister>
```

It appears once per charged shot in `latest.log`.
