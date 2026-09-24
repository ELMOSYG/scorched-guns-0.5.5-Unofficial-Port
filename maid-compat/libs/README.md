# maid-compat/libs/ — compile-time jars for the nested maid compat

Not distributed in this repository (they belong to their own authors). Put these here to compile
`maid-compat/`:

| File name | Mod | Notes |
|---|---|---|
| `touhoulittlemaid-1.5.3-neoforge-mc1.21.1.jar` | Touhou Little Maid | the mod this compat is written for; **optional at runtime** |
| `cloth-config-15.0.140-neoforge.jar` | Cloth Config | used for the config screen TLM lets addons add; **optional at runtime** |

Notes:

* The Touhou Little Maid jar name may contain a Minecraft version suffix or brackets when downloaded —
  rename it so that the file name has no `[`, `]` or spaces, otherwise Gradle's `fileTree` will not pick
  it up (the tooling notes in `HANDOFF.md` section 36.7 describe the same trap).
* Neither jar is a hard dependency of the shipped compat: the mod's entry point returns immediately
  when TLM is absent, and `MaidMixinPlugin` switches the whole mixin configuration off. Cloth Config is
  guarded the same way, which is why both are declared `optional` in
  `maid-compat/src/main/templates/META-INF/neoforge.mods.toml`.
