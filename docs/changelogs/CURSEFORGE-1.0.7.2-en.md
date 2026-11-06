# Scorched Guns 1.0.7.2

**Minecraft 1.21.1 · NeoForge 21.1.150+**

A small follow-up. The mod's own version stays 1.0.7; only the file name changes.

**The cog knight's charge is now readable and survivable.** On hard difficulty a charge from a cog knight carrying the
gear mace took roughly 19 of a player's 20 health through iron armour - close to a one-shot - and it could not be
avoided: the knight re-aimed every tick and set its velocity straight at the player for the whole 20 ticks, with no
wind-up.

Four things were fixed:

- A charge could land twice (a 20-tick charge against a 10-tick hurt cooldown, with the hit check running every tick).
  It now lands once per charge.
- It could not be dodged. It is now **6 ticks of wind-up + 6 ticks of dash**: the knight stands still and keeps tracking
  during the wind-up, then commits to that direction and does not turn.
- The damage was too high. Charge damage is capped at **18 after the difficulty scale**.
- It dragged on. Dash speed went from 0.8 to **1.15** and the whole move from 20 to **12 ticks** (about 10 blocks).

The wind-up also leads the player's movement by 5 ticks, so simply walking sideways is no longer enough - but a
deliberate turn or a stop inside the 0.3-second window clears the charge, and the 4-second cooldown afterwards is a real
opening. Before the player has a gun, kiting used to be the only option; now melee is viable.

---

## Notes

- Client and server both need this build.
- 1.0.7.2 is a rename of the 1.0.7 jar; nothing else changed in the manifest.
