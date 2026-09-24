package com.scg2tlm.elmomod.client;

import com.github.tartaricacid.touhoulittlemaid.api.event.client.AddClothConfigEvent;

/**
 * Adds the compat's options to Touhou Little Maid's own Cloth Config screen.
 *
 * <p>TLM does not only have its own config - it lets addons contribute entries to the screen it
 * builds, by posting {@code AddClothConfigEvent} on {@code NeoForge.EVENT_BUS} from
 * {@code compat.cloth.MenuIntegration} (verified with {@code javap -c}: the call site there is
 * {@code getstatic NeoForge.EVENT_BUS} followed by {@code IEventBus.post}). That is a better hook than
 * registering a config screen of our own: the compat's options appear next to TLM's, inside the screen
 * the player already knows.</p>
 *
 * <h2>Why this was missing, and why that is now fixed</h2>
 * <p>The 1.20.1 original registerd the screen through Forge's
 * {@code ModLoadingContext.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory, ...)}. That
 * class does not exist on NeoForge, and at the time Cloth Config was not installed in the test instance
 * either, so the port dropped the whole screen and left the options to
 * {@code config/scg2_maid_compat-common.toml} (HANDOFF section 36.6). Cloth Config is in the instance
 * now, so the screen is back - and it needs no Forge-only class at all, because TLM's hook does the
 * work.</p>
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
        SCG2TLMClothConfig.addEntries(event.getRoot(), event.getEntryBuilder());
    }
}
