package com.scg2tlm.elmomod.client;

import com.scg2tlm.elmomod.ExampleMod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The maid compat's config screen hooks, kept out of {@link ExampleMod} on purpose.
 *
 * <p>The registration lambda becomes a synthetic method of whichever class contains it, and that method's
 * descriptor mentions {@code net.minecraft.client.gui.screens.Screen}. With the lambda in the mod entry
 * class, the dedicated server resolved Screen while verifying {@code ExampleMod} and refused to start:</p>
 *
 * <pre>Attempted to load class net/minecraft/client/gui/screens/Screen for invalid dist DEDICATED_SERVER</pre>
 *
 * <p>Here the descriptors stay in a client-only class, and the entry point only makes an ordinary call to
 * {@link #register(ModContainer)} from behind its {@code FMLEnvironment.dist.isClient()} check.</p>
 */
public final class ClientConfigScreen {
   private static final Logger LOGGER = LogManager.getLogger("scg2_maid_compat");

   public static void register(ModContainer modContainer) {
      if (ModList.get().isLoaded(ExampleMod.CLOTH_CONFIG_MODID)) {
         NeoForge.EVENT_BUS.addListener(SCG2TLMClothConfigListener::onAddClothConfig);
         modContainer.registerExtensionPoint(IConfigScreenFactory.class,
            (container, parent) -> SCG2TLMClothConfig.createScreen(parent));
      } else {
         LOGGER.info("Cloth Config is not installed - the maid compat's options stay in "
            + "config/scg2_maid_compat-common.toml instead of a config screen.");
      }
   }

   private ClientConfigScreen() {
   }
}
