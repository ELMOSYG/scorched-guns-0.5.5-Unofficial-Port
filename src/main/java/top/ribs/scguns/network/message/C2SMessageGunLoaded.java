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
import top.ribs.scguns.util.GunAmmo;

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
                  // The packet is the reload animation's "loaded" keyframe, so it may only seat a round
                  // while a reload is actually running. It used to seat one unconditionally: after an
                  // unload mid-reload the client's loop kept sending it and the magazine refilled for
                  // free (reproduced on 21.1.264 - ammo 0 -> 1 -> 2 with reloading already false).
                  if (!Boolean.TRUE.equals(ModSyncedDataKeys.RELOADING.getValue(player))) {
                     return;
                  }

                  // A freshly built gun may have no custom data at all, and the tracker only writes a
                  // magazine into a tag that already exists (GunAmmo.set keeps 0.5.5's guard).
                  NbtHelper.ensureTag(heldItem);
                  ReloadTracker tracker = new ReloadTracker(player);
                  if (gun.getReloads().getReloadType() == ReloadType.MAG_FED) {
                     tracker.increaseMagAmmo(player);
                     ModSyncedDataKeys.RELOADING.setValue(player, false);
                     clearReloadFlags(heldItem);
                     PacketHandler.getPlayChannel()
                        .sendToNearbyPlayers(
                           () -> LevelLocation.create((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(), 64.0),
                           new S2CMessageUpdateAmmo(GunAmmo.get(heldItem))
                        );
                     PacketHandler.getPlayChannel()
                        .sendToNearbyPlayers(
                           () -> LevelLocation.create((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(), 64.0),
                           new S2CMessageReload(false)
                        );
                  } else if (gun.getReloads().getReloadType() == ReloadType.SINGLE_ITEM) {
                     tracker.reloadItem(player);
                     ModSyncedDataKeys.RELOADING.setValue(player, false);
                     clearReloadFlags(heldItem);
                     PacketHandler.getPlayChannel()
                        .sendToNearbyPlayers(
                           () -> LevelLocation.create((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(), 64.0),
                           new S2CMessageUpdateAmmo(GunAmmo.get(heldItem))
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
                           new S2CMessageUpdateAmmo(GunAmmo.get(heldItem))
                        );
                     boolean weaponFull = tracker.isWeaponFull(player);
                     boolean hasNoAmmo = tracker.hasNoAmmo(player);
                     if (weaponFull || hasNoAmmo) {
                        if (player.getMainHandItem().getItem() instanceof AnimatedGunItem) {
                           NbtHelper.setBoolean(heldItem, "scguns:ShouldStopAfterLoop", true);
                        } else {
                           ModSyncedDataKeys.RELOADING.setValue(player, false);
                           clearReloadFlags(heldItem);
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

   /**
    * Clears the reload flags a finished reload leaves behind.
    *
    * <p>Fetches the tag itself: the reload writes the magazine through {@link GunAmmo}, which
    * re-installs the component, so a tag bound before that write is an orphan and these removals
    * never reached the item - the same shape as the write-back bug fixed in HANDOFF section 67.</p>
    */
   private static void clearReloadFlags(ItemStack stack) {
      CompoundTag tag = NbtHelper.getTagForWrite(stack);
      if (tag == null) {
         return;
      }

      tag.remove("IsReloading");
      tag.remove("scguns:IsReloading");
      tag.remove("InCriticalReloadPhase");
      NbtHelper.setTag(stack, tag);
   }
}
