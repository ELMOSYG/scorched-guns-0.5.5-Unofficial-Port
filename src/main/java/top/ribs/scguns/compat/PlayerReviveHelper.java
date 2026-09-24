package top.ribs.scguns.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.world.entity.player.Player;
import top.ribs.scguns.ScorchedGuns;

public class PlayerReviveHelper {
   private static boolean disable = false;
   private static Method getBleeding;
   private static Method isBleeding;

   public PlayerReviveHelper() {
      super();
   }

   public static boolean isBleeding(Player player) {
      if (ScorchedGuns.playerReviveLoaded && !disable) {
         try {
            init();
            Object object = getBleeding.invoke(null, player);
            return (Boolean)isBleeding.invoke(object);
         } catch (IllegalAccessException | InvocationTargetException var2) {
            disable = true;
            return false;
         }
      } else {
         return false;
      }
   }

   private static void init() {
      if (getBleeding == null) {
         try {
            Class<?> playerReviveServer = Class.forName("team.creative.playerrevive.server.PlayerReviveServer");
            getBleeding = playerReviveServer.getDeclaredMethod("getBleeding", Player.class);
            Class<?> bleeding = Class.forName("team.creative.playerrevive.cap.Bleeding");
            isBleeding = bleeding.getDeclaredMethod("isBleeding");
         } catch (NoSuchMethodException | ClassNotFoundException var2) {
            disable = true;
         }
      }
   }
}
