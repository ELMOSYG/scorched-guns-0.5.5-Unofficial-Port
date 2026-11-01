# Scorched Guns 1.0.9.1

**Minecraft 1.21.1 · NeoForge 21.1.150+**

A correction release. Two enchantment fixes, two recipe-viewer fixes, and one impulse formula reverted.

## Enchantments respect their tags again, anvil included

`GunItem` used to answer "yes" to every one of the mod's own enchantments, which let anvils and books bypass each
enchantment's `supported_items` tag. The Madsen and the MG 08/15 are deliberately left out of
`#scguns:enchantable/trigger_finger`, yet could still be given Trigger Finger and double their fire rate.

They now go through the tag like vanilla enchantments do, with a second check on the anvil so a bad combination
produces no output instead of silently succeeding. Enchantments already applied in old saves are not removed.

## The enchanted muzzle flash toggle finally works

`ignoreEnchantedMuzzleFlash` only guarded the value arriving in a packet, not the starting value, so an enchanted gun
always showed the enchanted flash. The setting now decides the starting value.

## JEI's anvil page covers guns and attachments

JEI 19 no longer scans anvils on its own; mods have to register the combinations. Each gun and each attachment is now
registered for both "same item + same item" (merge repair) and "damaged + repair kit". The list comes from the item
registry, so guns and attachments added by addons are included automatically.

## `slug_splitter` has a model and a name

The item was registered but its model, texture and translations were never ported. It now has a model (a barrel
texture as a placeholder) and a name. A sweep of all 469 registered items found it was the only one missing a model.

## Physics structures: the impulse no longer scales with mass

1.0.9 used Sable's punch curve, which derives its magnitude from the structure's inverse normal mass. That goes wrong
when a structure is flipped or upside down, and scaling by mass meant every structure received the same speed change
regardless of its size - large vehicles reacted as if they weighed nothing.

It now works the way Sable's own arrow impacts do:

```
impulse  = punchScale * 3.0              fixed, independent of mass
applied  = at the impact point           not at the shooter
capped   = mass * remaining speed budget
           spin budget / spin per impulse
```

Light structures react visibly, heavy vehicles barely move, and flipped structures no longer spin wildly. Sustained
shotgun fire is still limited by the speed budget.

## The 600 RPM floor is gone

1.0.9 briefly clamped every gun's rate to at least 2 ticks (600 RPM). That is reverted - the enchantment tags already
handle it, and the code does not need a second gate.

## Known issues

- **JEI bookmark recipe lookup**: a gun bookmarked from the gun bench page shows no recipe preview on hover, and `R`
  finds nothing. Forcing the recipe output to a raw stack, resolving it through a real registry access, and
  registering a typed recipe manager plugin all failed. `R` works inside the page itself, so the recipes are indexed;
  the break is on the bookmark side. Shelved for now.
- Explosion shockwaves still do not work.

## Verification

- Build passes, 55 static audits pass, install verification 291/291
- Client and server share the same jar
