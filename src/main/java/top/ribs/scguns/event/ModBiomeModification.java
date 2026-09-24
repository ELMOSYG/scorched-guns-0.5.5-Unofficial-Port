package top.ribs.scguns.event;

// 0.5.5 shipped this as an empty @EventBusSubscriber class. Forge tolerated a subscriber
// with nothing to subscribe; NeoForge aborts mod loading with "class ... has no
// @SubscribeEvent methods, but register was called anyway", so the annotation is gone.
// The class is kept for parity with 0.5.5 and is referenced by nothing.
public class ModBiomeModification {
   public ModBiomeModification() {
      super();
   }
}
