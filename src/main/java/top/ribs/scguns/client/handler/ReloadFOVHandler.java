package top.ribs.scguns.client.handler;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.item.GunItem;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME,
   value = {Dist.CLIENT}
)
public class ReloadFOVHandler {
   private static float preReloadFOV = 1.0F;
   private static boolean wasReloading = false;
   private static int reloadEndCooldown = 0;
   private static final int COOLDOWN_DURATION = 3;

   public ReloadFOVHandler() {
      super();
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post event) {
      {
         if (reloadEndCooldown > 0) {
            reloadEndCooldown--;
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onComputeFOV(ComputeFovModifierEvent event) {
      Player player = event.getPlayer();
      ItemStack mainHand = player.getMainHandItem();
      ItemStack offHand = player.getOffhandItem();
      boolean holdingGun = mainHand.getItem() instanceof GunItem || offHand.getItem() instanceof GunItem;
      boolean isReloading = (Boolean)ModSyncedDataKeys.RELOADING.getValue(player);
      if (wasReloading && !holdingGun) {
         wasReloading = false;
         reloadEndCooldown = 0;
      } else {
         if (holdingGun) {
            if (isReloading) {
               if (!wasReloading) {
                  preReloadFOV = event.getFovModifier();
                  wasReloading = true;
                  reloadEndCooldown = 0;
               }

               event.setNewFovModifier(preReloadFOV);
            } else if (wasReloading) {
               wasReloading = false;
               reloadEndCooldown = 3;
               event.setNewFovModifier(preReloadFOV);
            } else if (reloadEndCooldown > 0) {
               event.setNewFovModifier(preReloadFOV);
            }
         } else {
            reloadEndCooldown = 0;
         }
      }
   }
}
