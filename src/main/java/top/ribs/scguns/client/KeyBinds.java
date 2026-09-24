package top.ribs.scguns.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import top.ribs.scguns.Config;

public class KeyBinds {
   public static final KeyMapping KEY_RELOAD = new KeyMapping("key.scguns.reload", 82, "key.categories.scguns");
   public static final KeyMapping KEY_UNLOAD = new KeyMapping("key.scguns.unload", 85, "key.categories.scguns");
   public static final KeyMapping KEY_ATTACHMENTS = new KeyMapping("key.scguns.attachments", 90, "key.categories.scguns");
   public static final KeyMapping KEY_MELEE = new KeyMapping("key.scguns.melee", 86, "key.categories.scguns");
   public static final KeyMapping KEY_INSPECT = new KeyMapping("key.scguns.inspect", 88, "key.categories.scguns");
   public static final KeyMapping KEY_ENABLE_EXO_HELMET = new KeyMapping("key.scguns.enable_exo_helmet", 66, "key.categories.scguns");
   public static final KeyMapping KEY_ENABLE_EXO_BOOTS = new KeyMapping("key.scguns.enable_exo_boots", 78, "key.categories.scguns");
   public static final KeyMapping KEY_ENABLE_EXO_CHESTPLATE = new KeyMapping("key.scguns.enable_exo_chestplate", 77, "key.categories.scguns");
   public static final KeyMapping KEY_SWAP_AMMO = new KeyMapping("key.scguns.swap_ammo", 72, "key.categories.scguns");

   public KeyBinds() {
      super();
   }

   public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
      event.register(KEY_RELOAD);
      event.register(KEY_UNLOAD);
      event.register(KEY_ATTACHMENTS);
      event.register(KEY_MELEE);
      event.register(KEY_INSPECT);
      event.register(KEY_ENABLE_EXO_HELMET);
      event.register(KEY_ENABLE_EXO_CHESTPLATE);
      event.register(KEY_ENABLE_EXO_BOOTS);
      event.register(KEY_SWAP_AMMO);
   }

   public static KeyMapping getAimMapping() {
      Minecraft mc = Minecraft.getInstance();
      return Config.CLIENT.controls.flipControls.get() ? mc.options.keyAttack : mc.options.keyUse;
   }

   public static KeyMapping getShootMapping() {
      Minecraft mc = Minecraft.getInstance();
      return Config.CLIENT.controls.flipControls.get() ? mc.options.keyUse : mc.options.keyAttack;
   }
}
