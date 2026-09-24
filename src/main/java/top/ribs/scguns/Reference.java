package top.ribs.scguns;

import net.minecraft.resources.ResourceLocation;

public class Reference {
   public static final String MOD_ID = "scguns";

   public Reference() {
      super();
   }

   public static ResourceLocation id(String string) {
      return ResourceLocation.fromNamespaceAndPath("scguns", string);
   }
}
