package top.ribs.scguns.network;


import top.ribs.scguns.util.NbtHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.util.GunModifierHelper;
import top.ribs.scguns.util.GunAmmo;

@OnlyIn(Dist.CLIENT)
public class ClientMessageHandler {
   public ClientMessageHandler() {
      super();
   }

   public static boolean handleUpdateAmmo(int ammoCount) {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         ItemStack heldItem = player.getMainHandItem();
         if (heldItem.getItem() instanceof GunItem) {
            GunAmmo.setOrCreate(heldItem, ammoCount);
            // Fetched after the write (it re-installed the component) and as a private copy
            // (getTagForWrite, not getTag): the reload-stop writes below are in-place, and only a copy
            // that belongs to this stack keeps ItemStack.matches honest.
            CompoundTag tag = NbtHelper.getTagForWrite(heldItem);
            int maxAmmo = GunModifierHelper.getModifiedAmmoCapacity(heldItem, ((GunItem)heldItem.getItem()).getModifiedGun(heldItem));
            if (ammoCount >= maxAmmo && tag != null && tag.getBoolean("scguns:IsReloading")) {
               tag.putString("scguns:ReloadState", "STOPPING");
               tag.putBoolean("scguns:IsPlayingReloadStop", true);
               ModSyncedDataKeys.RELOADING.setValue(player, false);
               tag.remove("scguns:IsReloading");
            }
         }
      }

      return true;
   }
}
