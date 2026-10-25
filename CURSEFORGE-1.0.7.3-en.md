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

---

## Notes

- Client and server both need this build.
- 1.0.7.3 is a rename of the 1.0.7 jar; nothing else changed in the manifest.
