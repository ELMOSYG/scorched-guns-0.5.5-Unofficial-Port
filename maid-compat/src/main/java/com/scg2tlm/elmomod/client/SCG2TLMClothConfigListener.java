package com.scg2tlm.elmomod.client;

import com.github.tartaricacid.touhoulittlemaid.api.event.client.AddClothConfigEvent;
import com.scg2tlm.elmomod.SCG2TLMConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;

/**
 * Adds the compat's options to Touhou Little Maid's own Cloth Config screen.
 *
 * <p>TLM does not only have its own config - it lets addons contribute entries to the screen it
 * builds, by posting {@code AddClothConfigEvent} on {@code NeoForge.EVENT_BUS} from
 * {@code compat.cloth.MenuIntegration} (verified with {@code javap -c}: the call site there is
 * {@code getstatic NeoForge.EVENT_BUS} followed by {@code IEventBus.post}).</p>
 *
 * <h2>Why the injected options used to forget themselves</h2>
 * <p>Adding entries is only half of it. Cloth calls a {@code setSaveConsumer} when the screen saves,
 * and that consumer does nothing but {@code SCG2TLMConfig.X.set(value)} - and {@code ConfigValue#set}
 * only writes the in-memory NightConfig value, never the file (see
 * {@code ModConfigSpec$ConfigValue#set}: {@code loadedConfig.config().set(path, value)} and a cache
 * update, then return). The file is written by {@code ModConfigSpec#save} alone, which the standalone
 * screen wires up through {@code setSavingRunnable(SPEC::save)} - but the injected copy of the entries
 * never arranged that, and TLM's own saving runnable only saves TLM's spec. So options changed in
 * TLM's screen took effect for the session and were replaced by the old file contents on restart,
 * which is what the player saw.</p>
 *
 * <p>The fix chains rather than replaces: {@code setSavingRunnable} overwrites, so TLM's runnable is
 * read back with {@code getSavingRunnable} and called first, then ours. Replacing it would have fixed
 * this screen by breaking TLM's own config.</p>
 *
 * <h2>Guards</h2>
 * <p>Registered from {@link com.scg2tlm.elmomod.ExampleMod} only when Touhou Little Maid is loaded
 * (the compat returns early otherwise) and only when Cloth Config is. Nothing in this class may be
 * touched when either is missing: the method's parameter type comes from TLM and the entry builders
 * come from Cloth Config, so loading it without them would be a {@code NoClassDefFoundError}.</p>
 */
public final class SCG2TLMClothConfigListener {
    private SCG2TLMClothConfigListener() {
    }

    public static void onAddClothConfig(AddClothConfigEvent event) {
        ConfigBuilder root = event.getRoot();
        SCG2TLMClothConfig.addEntries(root, event.getEntryBuilder());

        Runnable tlmSave = root.getSavingRunnable();
        root.setSavingRunnable(() -> {
            if (tlmSave != null) {
                tlmSave.run();
            }
            SCG2TLMConfig.SPEC.save();
        });
    }
}
