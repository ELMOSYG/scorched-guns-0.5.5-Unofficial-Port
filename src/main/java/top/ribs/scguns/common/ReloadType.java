package top.ribs.scguns.common;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public record ReloadType(ResourceLocation id) {
   public static final ReloadType MANUAL = new ReloadType(ResourceLocation.fromNamespaceAndPath("scguns", "manual"));
   public static final ReloadType MAG_FED = new ReloadType(ResourceLocation.fromNamespaceAndPath("scguns", "mag_fed"));
   public static final ReloadType SINGLE_ITEM = new ReloadType(ResourceLocation.fromNamespaceAndPath("scguns", "single_item"));
   private static final Map<ResourceLocation, ReloadType> reloadTypeMap = new HashMap<>();

   public ReloadType(ResourceLocation id) {
      this.id = id;
   }

   public static void registerType(ReloadType mode) {
      reloadTypeMap.putIfAbsent(mode.id(), mode);
   }

   public static ReloadType getType(ResourceLocation id) {
      return reloadTypeMap.getOrDefault(id, MANUAL);
   }

   static {
      registerType(MANUAL);
      registerType(MAG_FED);
      registerType(SINGLE_ITEM);
   }
}
