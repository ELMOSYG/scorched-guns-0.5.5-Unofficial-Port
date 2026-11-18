package top.ribs.scguns.event;

import top.ribs.scguns.Config;

/** One place that answers "may this hit send blood particles" for the whole mod. */
public final class BloodCompat {

   private BloodCompat() {
   }

   /** Touhou Little Maid's mod id, used to recognise its entities without depending on the mod. */
   private static final String MAID_NAMESPACE = "touhou_little_maid";

   /**
    * Whether this entity type belongs to Touhou Little Maid.
    *
    * <p>Maids are deliberately kept out of the blood effects: they are companions, and players react strongly to
    * seeing them bleed. Checked by registry namespace so this mod keeps working with or without the maid mod present.
    * </p>
    */
   public static boolean isMaid(net.minecraft.world.entity.EntityType<?> type) {
      if (type == null) {
         return false;
      }
      net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
      return id != null && MAID_NAMESPACE.equals(id.getNamespace());
   }

   /**
    * Whether blood particles may be sent to clients.
    *
    * <p>The player facing option lives in the client config, which a server cannot control. This adds a server side
    * switch so an owner can turn blood off for everyone without asking each player to change their settings.</p>
    */
   public static boolean allowed() {
      return Config.SERVER == null || !Config.SERVER.disableBloodParticles.get();
   }
}
