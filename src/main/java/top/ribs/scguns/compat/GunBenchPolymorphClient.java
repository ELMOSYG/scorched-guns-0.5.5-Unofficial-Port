package top.ribs.scguns.compat;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import top.ribs.scguns.ScorchedGuns;

/**
 * Client half of the Polymorph integration: teach Polymorph which screen the gun bench uses.
 *
 * <p>This class is annotated, so FML resolves it - and everything its methods mention - while it is still scanning
 * mods for event subscribers. A direct reference to a Polymorph type here is therefore resolved long before the
 * {@code POLYMORPH_LOADED} check below can run, and if that type is missing the game dies during startup rather
 * than losing one feature. The Polymorph-using code therefore lives in {@link GunBenchPolymorphWidgets}, which is
 * reached only through the reflective lookup in {@link #install()}.</p>
 */
@EventBusSubscriber(modid = "scguns", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GunBenchPolymorphClient {
   private GunBenchPolymorphClient() {
   }

   @SubscribeEvent
   public static void onClientSetup(FMLClientSetupEvent event) {
      if (!CompatManager.POLYMORPH_LOADED) {
         return;
      }

      event.enqueueWork(GunBenchPolymorphClient::install);
   }

   /**
    * Loads the Polymorph-touching half. A missing or incompatible Polymorph API is not a crash: the gun bench simply
    * keeps working without the recipe selector.
    */
   private static void install() {
      try {
         Class.forName("top.ribs.scguns.compat.GunBenchPolymorphWidgets").getMethod("install").invoke(null);
      } catch (Throwable throwable) {
         ScorchedGuns.LOGGER.warn(
            "Gun bench recipe selector disabled: the installed Polymorph API is missing or incompatible", throwable);
      }
   }
}
