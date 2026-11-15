package top.ribs.scguns.network.message;


import top.ribs.scguns.util.NbtHelper;
import com.mrcrayfish.framework.api.network.LevelLocation;
import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.common.ReloadTracker;
import top.ribs.scguns.common.ReloadType;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.animated.AnimatedGunItem;
import top.ribs.scguns.network.PacketHandler;

public class C2SMessageGunLoaded {
   public C2SMessageGunLoaded() {
      super();
   }

   public void encode(C2SMessageGunLoaded message, FriendlyByteBuf buffer) {
   }

   public C2SMessageGunLoaded decode(FriendlyByteBuf buffer) {
      return new C2SMessageGunLoaded();
   }

   public void handle(C2SMessageGunLoaded message, MessageContext context) {
      context.execute(
         () -> {
            ServerPlayer player = context.getPlayer().map(p -> (ServerPlayer) p).orElse(null);
            if (player != null && !player.isSpectator()) {
               ItemStack heldItem = player.getMainHandItem();
               if (heldItem.getItem() instanceof GunItem) {
                  if (!heldItem.getItem().getClass().getPackageName().startsWith("top.ribs.scguns")) {
                     return;
                  }

                  Gun gun = ((GunItem)heldItem.getItem()).getModifiedGun(heldItem);
                  CompoundTag tag = NbtHelper.getOrCreateTag(heldItem);
                  // A loaded packet can arrive twice inside one reload window (the client retries while its start tick has not
         // elapsed). Each arrival used to build a fresh tracker, so a magazine gun filled twice and played the reload
         // animation twice - the "reload happens twice on multiplayer" report. When the magazine is already full and
         // nothing is in flight, this arrival is a duplicate: drop it. Nothing about animation timing changes here.
         ItemStack hand = player.getMainHandItem();
         if (hand.getItem() instanceof top.ribs.scguns.item.GunItem handGun
            && !Boolean.TRUE.equals(top.ribs.scguns.init.ModSyncedDataKeys.RELOADING.getValue(player))) {
            int current = top.ribs.scguns.common.Gun.getAmmoCount(hand);
            int capacity = top.ribs.scguns.util.GunModifierHelper.getModifiedAmmoCapacity(
               hand, handGun.getModifiedGun(hand));
            if (current >= capacity) {
               return;
            }
         }

         ReloadTracker tracker = new ReloadTracker(player);
                  if (gun.getReloads().getReloadType() == ReloadType.MAG_FED) {
                     tracker.increaseMagAmmo(player);
                     ModSyncedDataKeys.RELOADING.setValue(player, false);
                     tag.remove("IsReloading");
                     tag.remove("scguns:IsReloading");
                     tag.remove("InCriticalReloadPhase");
                     PacketHandler.getPlayChannel()
                        .sendToNearbyPlayers(
                           () -> LevelLocation.create((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(), 64.0),
                           new S2CMessageUpdateAmmo(tag.getInt("AmmoCount"))
                        );
                     PacketHandler.getPlayChannel()
                        .sendToNearbyPlayers(
                           () -> LevelLocation.create((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(), 64.0),
                           new S2CMessageReload(false)
                        );
                  } else if (gun.getReloads().getReloadType() == ReloadType.SINGLE_ITEM) {
                     tracker.reloadItem(player);
                     ModSyncedDataKeys.RELOADING.setValue(player, false);
                     tag.remove("IsReloading");
                     tag.remove("scguns:IsReloading");
                     tag.remove("InCriticalReloadPhase");
                     PacketHandler.getPlayChannel()
                        .sendToNearbyPlayers(
                           () -> LevelLocation.create((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(), 64.0),
                           new S2CMessageUpdateAmmo(tag.getInt("AmmoCount"))
                        );
                     PacketHandler.getPlayChannel()
                        .sendToNearbyPlayers(
                           () -> LevelLocation.create((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(), 64.0),
                           new S2CMessageReload(false)
                        );
                  } else if (gun.getReloads().getReloadType() == ReloadType.MANUAL) {
                     tracker.increaseAmmo(player);
                     PacketHandler.getPlayChannel()
                        .sendToNearbyPlayers(
                           () -> LevelLocation.create((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(), 64.0),
                           new S2CMessageUpdateAmmo(tag.getInt("AmmoCount"))
                        );
                     boolean weaponFull = tracker.isWeaponFull(player);
                     boolean hasNoAmmo = tracker.hasNoAmmo(player);
                     if (weaponFull || hasNoAmmo) {
                        if (player.getMainHandItem().getItem() instanceof AnimatedGunItem) {
                           tag.putBoolean("scguns:ShouldStopAfterLoop", true);
                        } else {
                           ModSyncedDataKeys.RELOADING.setValue(player, false);
                           tag.remove("IsReloading");
                           tag.remove("scguns:IsReloading");
                           PacketHandler.getPlayChannel()
                              .sendToNearbyPlayers(
                                 () -> LevelLocation.create((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(), 64.0),
                                 new S2CMessageReload(false)
                              );
                        }
                     }
                  }
               }
            }
         }
      );
      context.setHandled(true);
   }
}
