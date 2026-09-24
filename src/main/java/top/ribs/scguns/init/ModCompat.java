package top.ribs.scguns.init;

import net.neoforged.fml.ModList;

public class ModCompat {
   public ModCompat() {
      super();
   }

   public static boolean isCreateLoaded() {
      return ModList.get().isLoaded("create");
   }

   public static boolean isFarmersDelightLoaded() {
      return ModList.get().isLoaded("farmersdelight");
   }

   public static boolean isIELoaded() {
      return ModList.get().isLoaded("immersiveengineering");
   }

   public static boolean isMekanismLoaded() {
      return ModList.get().isLoaded("mekanism");
   }

   public static boolean isCreateIronworksLoaded() {
      return ModList.get().isLoaded("create_ironworks");
   }
}
