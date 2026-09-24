# libs-compile/ — compile-only classes extracted from another mod's jar

Not distributed in this repository: this folder holds code that belongs to Sable's authors.

`sable-neoforge-*.jar` bundles its API library (`dev.ryanhcode.sable.companion.*`) as a jar-in-jar.
NeoForge loads it at runtime, but Gradle never puts it on the compile classpath, so the few files that
reference those types need a local copy. Regenerate it with the extractor:

```powershell
python tools/extract_jarjar.py libs/sable-neoforge-1.21.1-2.0.5.jar libs-compile
# -> libs-compile/sable-companion-common-1.21.1-1.6.0.jar
```

`build.gradle` adds it as `compileOnly` on purpose and **not** as `localRuntime`, so a dev run uses
Sable's own copy instead of loading those classes twice.
