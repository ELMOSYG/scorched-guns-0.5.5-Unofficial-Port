package top.ribs.scguns.compat;

import com.illusivesoulworks.polymorph.api.client.PolymorphWidgets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import top.ribs.scguns.client.screen.GunBenchScreen;

/**
 * Client half of the Polymorph integration: teach Polymorph which screen the gun bench uses.
 *
 * <p>Kept in its own class and behind a client-only event so a dedicated server never loads any Polymorph client
 * type. The common half ({@link GunBenchPolymorph}) registers the block entity and the menu; this half is what
 * actually makes the selector appear.</p>
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

      event.enqueueWork(() -> PolymorphWidgets.getInstance().registerWidget(
         screen -> screen instanceof GunBenchScreen bench ? new GunBenchRecipesWidget(bench) : null));
   }
}
