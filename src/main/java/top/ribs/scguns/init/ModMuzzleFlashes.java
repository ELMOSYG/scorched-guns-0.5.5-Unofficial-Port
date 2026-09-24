package top.ribs.scguns.init;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public class ModMuzzleFlashes {
   private static final Map<String, ResourceLocation> muzzleFlashes = new HashMap<>();

   public ModMuzzleFlashes() {
      super();
   }

   public static void registerMuzzleFlash(String flashType, ResourceLocation texture) {
      muzzleFlashes.put(flashType, texture);
   }

   public static ResourceLocation getMuzzleFlashTexture(String flashType) {
      return muzzleFlashes.getOrDefault(flashType, ResourceLocation.fromNamespaceAndPath("scguns", "textures/effect/muzzle_flash_1.png"));
   }

   public static void init() {
      registerMuzzleFlash("flash_type_1", ResourceLocation.fromNamespaceAndPath("scguns", "textures/effect/muzzle_flash_1.png"));
      registerMuzzleFlash("flash_type_2", ResourceLocation.fromNamespaceAndPath("scguns", "textures/effect/muzzle_flash_2.png"));
      registerMuzzleFlash("flash_type_3", ResourceLocation.fromNamespaceAndPath("scguns", "textures/effect/muzzle_flash_3.png"));
      registerMuzzleFlash("flash_type_4", ResourceLocation.fromNamespaceAndPath("scguns", "textures/effect/muzzle_flash_4.png"));
      registerMuzzleFlash("flash_type_5", ResourceLocation.fromNamespaceAndPath("scguns", "textures/effect/muzzle_flash_5.png"));
      registerMuzzleFlash("flash_type_6", ResourceLocation.fromNamespaceAndPath("scguns", "textures/effect/muzzle_flash_6.png"));
      registerMuzzleFlash("flash_type_7", ResourceLocation.fromNamespaceAndPath("scguns", "textures/effect/muzzle_flash_7.png"));
   }
}
