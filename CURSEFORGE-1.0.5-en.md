# Scorched Guns — 1.0.5

**Minecraft 1.21.1 · NeoForge 21.1.250+**

This release is about **performance** and **addon compatibility**. No gun stats, recipes or art were changed.

---

## Highlights

- **Wearing a fully kitted exosuit no longer tanks your framerate** — measured **90 → 320 FPS**.
- **The gun HUD is roughly 1.7× cheaper** — **240 → ~400 FPS** while holding a gun.
- **One broken gun file can no longer make a save unloadable** — bad definitions are skipped and named in the log.
- **Addons can now place their own aiming camera in the model**, with a bone, instead of hand-tuning numbers.

---

## Performance

### Exosuit rendering

The renderer used to re-read every upgrade slot from NBT for **every bone**, then walk the upgrade registry
for each lookup. On a thirty-bone suit that came to well over a hundred NBT reads every frame — and the more
upgrades you fitted, the more bones were touched, so a fully kitted suit dropped to **90 FPS** while an empty
shell ran at 300 and no suit at all ran at 500.

Upgrade slots are now resolved once per client tick and cached; the render path only reads the cache. Registry
lookups hit that cache too, and bone visibility is only written when it actually changes (GeckoLib marks bones
dirty on every write).


### Gun HUD

`GuiGraphics.drawString` re-validates its buffer source on every call, so inside a GUI overlay each **line of text
was submitted as its own batch**. A profile showed about fifteen percent of the render thread inside that submit
path, while the glyphs themselves cost about two percent.

Lines are now written into one buffer and submitted once per HUD pass. Ammo and exosuit readouts submit
separately so neither can drag the other into the wrong coordinate space.

### Gun data and attachments

The modified gun definition was cached under the **whole item tag**, which contains the ammo count — every shot
invalidated it. Worse, `getTag()` returns a fresh object each call, so the `WeakHashMap` entries were collectable
immediately and the cache almost never hit; each lookup deep-copied and re-deserialised the entire definition.

It is now keyed on the `Gun` compound alone, with a bounded map, so firing no longer rebuilds anything.
Attachment lookups — the most frequent mod call on the render path — are cached the same way.

### Aiming camera

Gun models can now decide where the aiming camera sits by placing a bone. `camera_aim`, `aim_camera`,
`iron_sight_camera` and `camera` are recognised in that priority order. When no such bone exists the original
`modules.zoom` offsets are used, so every existing gun behaves exactly as before.

The bone is cached with a one-second expiry, so re-exporting a model and pressing F3+T is enough — no restart.

---

## Addon compatibility

- **Per-file isolation.** Each gun definition is loaded inside its own error boundary. Previously a single bad
  JSON aborted the whole data load and made the save unloadable; now only that gun is missing, and the log names
  the file.
- **Addons may use their own namespace.** An item registered as `yourmod:rifle` is matched with
  `data/yourmod/guns/rifle.json`. Models, textures and animations still live under `assets/scguns/` because they
  are asset references, not registry ids. If a definition exists in both `scguns` and another namespace, the
  `scguns` one wins.
- **Bone-driven iron sights.** Put a camera bone in your `.geo.json` and the sight lines up automatically.
- **Better diagnostics.** The aiming camera logs the bone it chose (or that it fell back to zoom offsets), and gun
  load failures print a full stack trace.

---

## Fixes

| Issue | Fix |
|---|---|
| Wearing an exosuit cost framerate, and more upgrades meant more cost | Upgrade slots are resolved once per tick and the render path reads that cache |
| The gun HUD submitted one batch per line | All lines of a HUD pass share a single batch |
| A single malformed gun file made the save unloadable | Each definition loads inside its own error boundary |

---

## Known limitations

- **How much you gain depends on the Minecraft version.** The same work gives a 3.5× improvement on 1.21.1 but
  only about 10% on 1.20.1. In 1.20.1 `ItemStack.getTag()` returns NBT directly, while 1.21.1 goes through the
  data-component system and is far more expensive per call; the exosuit and HUD defects exist on both, but the
  1.20.1 baseline was already fast.
- **Cache window is 50 ms.** In the worst case a freshly modified gun can read a stale value for up to one tick.
- **No gameplay data changed.** Damage, fire rate, magazine size, recipes and loot are untouched.

---

## Verification

- All 53 gate scripts pass; install verification 289/289.
- Client-side Spark profiles put this mod's own code at about **0.02%** of the render thread, down from the top of
  the list. What remains is Minecraft's render pipeline (shader switches, vertex uploads) and GeckoLib's bone
  submission — inherent cost rather than anything this mod contributes.

---

## Credits

- **Port author:** ELMO_SYG
- **Original mod:** Ribs (Scorched Guns)
- AI assistance was used for parts of this port, including the performance work described above.
