package top.ribs.scguns.compat;

import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;

public class CompatManager {
   public static final boolean SCULK_HORDE_LOADED = modLoaded("sculkhorde");
   public static final boolean POLYMORPH_LOADED = modLoaded("polymorph");

   public CompatManager() {
      super();
   }

   public static boolean modLoaded(String modID) {
      ModFileInfo mod = FMLLoader.getLoadingModList().getModFileById(modID);
      return mod != null;
   }
}
