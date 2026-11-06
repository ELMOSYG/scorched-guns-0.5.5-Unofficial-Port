# Scorched Guns 1.0.7.3

**Minecraft 1.21.1 · NeoForge 21.1.150+**

A small follow-up. The mod's own version stays 1.0.7; only the file name changes.

**Some settings moved from the server config to the client.** Aim-to-toggle, the blindness overlay, the deafened audio
levels, the tinnitus ring and camera recoil only affect what the player sees and hears, yet they lived in the server
config - so a server decided them for everyone, and changing servers meant readjusting. They now live in
`config/scguns-client.toml`, one set per player. The rule was simply "if only client code reads it, the server should not
own it"; settings that affect gameplay or must match on both sides stayed where they were. The old keys in the server
config are no longer read, and the defaults are unchanged.

**Killing a mob with Gibbs rounds no longer duplicates its gun and shield.** Upstream raised the Looting level by four
through Forge's `LootingLevelEvent`, and the loot tables' own looting functions decided what that meant - guns and
shields never multiplied because no table attaches those functions to them. That event does not exist on 1.21.1, and the
port replaced it with "grow every dropped stack", which pushed unstackable items past their limit: a count of three on a
gun is invalid, and the game split it into three separate items on pickup. The fallback now follows the enchantment's
own rules - unstackable drops are never grown, and stackable ones never exceed their stack limit. A pack that installs the
enchantment addon still gets the original "raise the level" path.

**Gunfire can knock a shield out of a mob's hands.** Vanilla only has one way to break a shield: a player struck by an
axe-wielding mob runs `Player#disableShield()`, which puts the shield on a 100-tick cooldown. Nothing does the equivalent
for a mob holding a shield - `disableShield()` lives on `Player`, and non-players do not even have item cooldowns - so a
skeleton or a guard with a raised shield blocked everything forever.

The mod's shield handling had the same gap: it bailed out for any non-player. It now works for any living entity. If the
disable roll succeeds, a player's shield goes on cooldown as before and a mob's shield is knocked to the ground (mobs have
no cooldowns, and the player can pick it up). If it fails, the hit is still blocked and the shield takes 12 durability,
exactly as before.

**This is off by default.** Mobs that raise shields - Guard Villagers' guards, skeletons given shields by other mods -
usually drive that shield with their own logic, so removing it can disturb their AI. The switch is
`guns_break_mob_shields`, default `false`; packs that need it can turn it on. Even with the option off, blocking still
wears the shield down, so those shields are not invulnerable.

---

## Notes

- Client and server both need this build.
- 1.0.7.3 is a rename of the 1.0.7 jar; nothing else changed in the manifest.
