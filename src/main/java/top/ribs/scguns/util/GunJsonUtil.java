package top.ribs.scguns.util;

import com.google.gson.JsonObject;

public class GunJsonUtil {
   public GunJsonUtil() {
      super();
   }

   public static void addObjectIfNotEmpty(JsonObject parent, String key, JsonObject child) {
      if (child.size() > 0) {
         parent.add(key, child);
      }
   }
}
