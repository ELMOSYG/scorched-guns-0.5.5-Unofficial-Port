package top.ribs.scguns.client;


import top.ribs.scguns.util.NbtHelper;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.level.ItemLike;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.commons.lang3.Validate;
import top.ribs.scguns.common.CustomGun;
import top.ribs.scguns.common.CustomGunLoader;
import top.ribs.scguns.init.ModItems;
import top.ribs.scguns.network.message.S2CMessageUpdateGuns;

@EventBusSubscriber(
   modid = "scguns",
   value = {Dist.CLIENT}
)
public class CustomGunManager {
   private static Map<ResourceLocation, CustomGun> customGunMap;

   public CustomGunManager() {
      super();
   }

   public static boolean updateCustomGuns(S2CMessageUpdateGuns message) {
      return updateCustomGuns(message.getCustomGuns());
   }

   private static boolean updateCustomGuns(Map<ResourceLocation, CustomGun> customGunMap) {
      CustomGunManager.customGunMap = customGunMap;
      return true;
   }

   public static void fill(Output output) {
      if (customGunMap != null) {
         customGunMap.forEach((id, gun) -> {
            ItemStack stack = new ItemStack((ItemLike)ModItems.M3_CARABINE.get());
            // 1.21 replaced ItemStack#setHoverName with the CUSTOM_NAME data component.
            stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item." + id.getNamespace() + "." + id.getPath() + ".name"));
            CompoundTag tag = NbtHelper.getOrCreateTag(stack);
            tag.put("Model", top.ribs.scguns.util.NbtHelper.tagFromItem(gun.getModel()));
            tag.put("Gun", gun.getGun().serializeNBT());
            tag.putBoolean("Custom", true);
            tag.putInt("AmmoCount", gun.getGun().getReloads().getMaxAmmo());
            output.accept(stack);
         });
      }
   }

   @SubscribeEvent
   public static void onClientDisconnect(LoggingOut event) {
      customGunMap = null;
   }

}
