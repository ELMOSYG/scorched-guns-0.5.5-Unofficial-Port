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
         // elapsed). Each arrival used to build a fresh tracker, so the magazine was filled twice and the animation
         // played twice. If the gun is already full there is nothing left to do.
         ItemStack handStack = player.getMainHandItem();
         if (handStack.getItem() instanceof top.ribs.scguns.item.GunItem handGunItem) {
            int current = top.ribs.scguns.common.Gun.getAmmoCount(handStack);
            int maxAmmo = top.ribs.scguns.util.GunModifierHelper.getModifiedAmmoCapacity(
               handStack, handGunItem.getModifiedGun(handStack));
            if (current >= maxAmmo && !Boolean.TRUE.equals(
               top.ribs.scguns.init.ModSyncedDataKeys.RELOADING.getValue(player))) {
               return;
            }
         }

         ReloadTracker tracker = new ReloadTracker(player);
                  if (gun.getReloads().getReloadType() == ReloadType.MAG_FED) {
                     tracker.increaseMagAmmo(player);
                     top.ribs.scguns.common.ReloadState.finish(player, player.getMainHandItem());
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
                     top.ribs.scguns.common.ReloadState.finish(player, player.getMainHandItem());
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
                           top.ribs.scguns.common.ReloadState.finish(player, player.getMainHandItem());
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
