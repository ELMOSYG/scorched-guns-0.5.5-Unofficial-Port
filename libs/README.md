# libs/ — dependency jars (not distributed in this repository)

The build compiles against the other mods' jars. They belong to their own authors, so they are **not**
committed here — put them into this folder yourself, with these exact file names:

## Required (the mod cannot run without them)

| File name | Mod | Notes |
|---|---|---|
| `framework-neoforge-1.21.1-0.13.11.jar` | MrCrayfish's Framework | 0.13.11 or newer for 1.21.1 |
| `geckolib-neoforge-1.21.1-4.9.3.jar` | GeckoLib | 4.9.3 or newer for 1.21.1 |
| `curios-neoforge-9.5.1+1.21.1.jar` | Curios API | 9.5.1 or newer for 1.21.1 |

These three are the ones declared as real dependencies in `META-INF/neoforge.mods.toml`. Their exact
file names do not have to match the table — `build.gradle` picks them up by glob — but the **versions
do**, and GeckoLib 4.9.3 is what makes NeoForge 21.1.150 the lowest supported loader version
(it requires `neoforge [21.1.150,)` itself).

## Optional integrations

Everything else you drop in here is compiled against (`compileOnly`) and loaded in dev runs
(`localRuntime`) only when present. None of it is required to build or to run:

```
jei-1.21.1-neoforge-*.jar                 create-1.21.1-*.jar
create-new-age-*.jar                      createoreexcavation-*.jar
create-aeronautics-bundled-*.jar          ImmersiveEngineering-*.jar
Mekanism-*.jar                            sable-neoforge-*.jar
prometheus-*.jar                          soul-fire-d-*.jar
cobweb-neoforge-*.jar                     FarmersDelight-*.jar
guardvillagers-1.21.1-*.jar
```

`guardvillagers` is the one that is also integrated (HANDOFF section 82): villagers' guards can carry and
fire the mod's guns, and a guard's shots pass through villagers, iron golems and other guards. The
integration is inert without the mod - `tools/audit_guard_compat.py` keeps it that way - and the jar is only
compiled against here, never redistributed.

Two of them are used in the dev environment only and demand a newer NeoForge than the floor, which is
why `gradlew runServer` has a switch to leave them out:

```powershell
cmd /c "gradlew.bat runServer -PnoIntegrationRuntime=true --console=plain"
```

## Sable's companion library

`sable-neoforge-*.jar` ships its API library as a jar-in-jar, which Gradle cannot see. Extract it once:

```powershell
python tools/extract_jarjar.py libs/sable-neoforge-1.21.1-2.0.5.jar libs-compile
```

See `libs-compile/README.md`.
