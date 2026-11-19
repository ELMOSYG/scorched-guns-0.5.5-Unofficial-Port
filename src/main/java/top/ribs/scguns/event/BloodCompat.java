package top.ribs.scguns.event;


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

}
