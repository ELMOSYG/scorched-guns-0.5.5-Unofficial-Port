# Changelog

Versions carry both halves: `0.5.5` is the upstream Scorched Guns release this port is built from, and
the last number is this port's own release counter. So `0.5.5.2` is the third published build of the
0.5.5 port.

## 0.5.5.2

### Fixed

* **Crash when Sable is not installed.** The mod reached into Sable's API without checking that the mod
  was actually there, so the game crashed for players who do not have it.

* **Blood particles not displaying properly.** A hit did spawn its droplets at the wound, but they were
  invisible while they were in the air: their quads faced away from the camera and the particle pass
  culled them. The only blood you could ever see was what had already landed, which is why it looked
  like blood appearing out of the ground. The droplets now spray out of the hit point, arc and fall,
  matching 1.20.1.

* **Mining guns (`scguns:shard_culler`, `scguns:cr4k_mining_laser`) showed no block-breaking texture.**
  The server was computing the crack and sending it, but the packets were addressed with an id that
  could match your own entity id, and vanilla deliberately skips that player - so nothing reached your
  client and the block never cracked. Blocks now crack as the beam cuts into them.

* **Mining guns could not be enchanted with Fortune or Silk Touch** at the enchanting table. 1.21.1
  moved that rule out of the item class and into a data tag, and the guns were not in the tag.

* **Create recipes.** Recipes that finished instantly now take the time they should - Create was reading
  a missing processing time as zero - and the two depleted diamond steel recipes no longer fail to load
  for players who do not have Create installed.

* **Beam knockback.** Mobs hit by a beam were shoved in a random direction instead of being pushed along
  the beam.

* **Enemy gun tiers.** Gunners no longer spawn a tier ahead of your progression; they lag one tier
  behind you, as 0.5.5 does.

* **Progression unlock notice.** The message no longer announces an enemy tier whose mobs never spawn.

### Also in this build

These landed in the builds leading up to 0.5.5.2 as well.

* **Ores dropped themselves when mined without Silk Touch.** Their loot tables used a 1.20.1 field name
  that 1.21.1 ignores, which made the Silk Touch branch match everything. 28 tables were affected, along
  with the supply crate and the 20 kinds of nitrated glass.

* **Enemy guns are worn when they spawn**, so they are not always pristine; this matches the equipment
  files the mod already shipped.

* **The bayonet charge has its own config section** (`bayonetCharge`), and the single-target thrust the
  player asked for sits behind the `singleTargetStab` option, off by default, with 0.5.5's original area
  charge kept as the shipped behaviour.

* **First-person gun and arm posing** no longer borrows the animated player model, so the arms no longer
  keep a pose left behind by another mod.

* **The headshot confirmation sound is no longer buried under the gunshot.**

* **Progression text**: the copper and iron tiers are named after their factions (Rustridge and
  Federation) in both languages.
