package top.ribs.scguns.common;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.C2SMessageChargeSync;

public class ChargeHandler {
   private static final Map<UUID, Integer> playerChargeTime = new HashMap<>();
   private static final Map<UUID, Integer> playerMaxChargeTime = new HashMap<>();
   private static final Map<UUID, Float> lastChargeProgress = new HashMap<>();

   public ChargeHandler() {
      super();
   }

   public static int getChargeTime(UUID playerId) {
      return playerChargeTime.getOrDefault(playerId, 0);
   }

   public static void updateChargeTime(Player player, ItemStack weapon, boolean isCharging) {
      if (weapon.getItem() instanceof GunItem gunItem) {
         UUID var10 = player.getUUID();
         Gun modifiedGun = gunItem.getModifiedGun(weapon);
         int maxChargeTime = modifiedGun.getGeneral().getFireTimer();
         playerMaxChargeTime.put(var10, maxChargeTime);
         if (isCharging) {
            boolean hasAmmo = Gun.hasAmmo(weapon) || player.isCreative();
            if (!hasAmmo) {
               playerChargeTime.remove(var10);
               lastChargeProgress.remove(var10);
               if (player.level().isClientSide()) {
                  PacketHandler.getPlayChannel().sendToServer(new C2SMessageChargeSync(0.0F));
               }
            } else {
               int currentCharge = playerChargeTime.getOrDefault(var10, 0);
               if (++currentCharge > maxChargeTime) {
                  currentCharge = maxChargeTime;
               }

               playerChargeTime.put(var10, currentCharge);
               float progress = maxChargeTime > 0 ? Math.min(1.0F, (float)currentCharge / (float)maxChargeTime) : 0.0F;
               lastChargeProgress.put(var10, progress);
               if (player.level().isClientSide()) {
                  PacketHandler.getPlayChannel().sendToServer(new C2SMessageChargeSync(progress));
               }
            }
         } else {
            playerChargeTime.remove(var10);
            lastChargeProgress.remove(var10);
            if (player.level().isClientSide()) {
               PacketHandler.getPlayChannel().sendToServer(new C2SMessageChargeSync(0.0F));
            }
         }
      }
   }

   public static void resetCharge(UUID playerId) {
      playerChargeTime.remove(playerId);
      playerMaxChargeTime.remove(playerId);
      lastChargeProgress.remove(playerId);
   }

   public static float getChargeProgress(@Nullable Player player, ItemStack weapon) {
      if (player != null && weapon.getItem() instanceof GunItem) {
         UUID playerId = player.getUUID();
         return lastChargeProgress.getOrDefault(playerId, 0.0F);
      } else {
         return 0.0F;
      }
   }

   public static void clearLastChargeProgress(UUID playerId) {
      lastChargeProgress.remove(playerId);
   }
}
