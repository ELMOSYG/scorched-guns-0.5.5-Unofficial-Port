Changes in 1.1.3

Follow up to 1.1.2. Mostly fixes, plus one dispenser feature.

New

- Dispensers can throw this mod's grenades: grenade, stun grenade, gas grenade, choke bomb, swarm bomb, nail bomb,
  hellfire bomb, molotov cocktail, beacon grenade and shotball.
- Ammunition is not fired by dispensers; it still drops as an item, as in vanilla.

Fixed

- Semi automatic beam guns (laser musket, minksy, waltz conversion) lost view bobbing while held.
- Only the true continuing beam is rendered as a mining-style beam again; semi automatic beams no longer linger.
- Bullet holes and break particles drew the missing texture on copycat blocks (Copycats+).
- Mobs in this mod kept firing or swinging during their death animation.
- Melee attacks from this mod's mobs reached through walls.
- Mobs kept attacking targets that were already dead.
- Mobs attacked players in creative and spectator mode.
- Mobs fought each other: friendly fire from same-side rounds started retaliation chains.
- Blood effects no longer apply to Touhou Little Maid companions.

Maid compatibility

- A maid hit by this mod's guns no longer produces hurt particles (config hide_gun_hit_particles, on by default).

Technical

- Target: Minecraft 1.21.1, NeoForge 21.1.x, Java 21.
- Requires GeckoLib, Framework and Curios.
- Temporary diagnostic logging was removed.
