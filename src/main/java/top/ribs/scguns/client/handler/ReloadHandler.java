package top.ribs.scguns.client.handler;



import top.ribs.scguns.util.NbtHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.InputEvent.Key;
import net.neoforged.neoforge.client.event.InputEvent.MouseButton;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import top.ribs.scguns.Config;
import top.ribs.scguns.client.KeyBinds;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.common.ReloadType;
import top.ribs.scguns.event.GunReloadEvent;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.animated.AnimatedGunItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.C2SMessageAim;
import top.ribs.scguns.network.message.C2SMessageGunLoaded;
import top.ribs.scguns.network.message.C2SMessageLeftOverAmmo;
import top.ribs.scguns.network.message.C2SMessageReload;
import top.ribs.scguns.network.message.C2SMessageUnload;
import top.ribs.scguns.util.GunModifierHelper;

public class ReloadHandler {
   private static ReloadHandler instance;
   private int startReloadTick;
   private int reloadTimer;
   private int prevReloadTimer;
   private int reloadingSlot;

   public static ReloadHandler get() {
      if (instance == null) {
         instance = new ReloadHandler();
      }

      return instance;
   }

   private ReloadHandler() {
      super();
   }

   public static void loaded(Player player) {
      Item item = player.getMainHandItem().getItem();
      if (item instanceof GunItem gunItem) {
         ItemStack stack = player.getMainHandItem();
         CompoundTag tag = NbtHelper.getOrCreateTag(stack);
         Gun gun = gunItem.getModifiedGun(stack);
         if (gun.getReloads().getReloadType() == ReloadType.MANUAL) {
            if (tag.getBoolean("scguns:ReloadComplete") && !tag.getBoolean("scguns:IsPlayingReloadStop")) {
               tag.putBoolean("scguns:IsPlayingReloadStop", true);
               tag.remove("InCriticalReloadPhase");
               ModSyncedDataKeys.RELOADING.setValue(player, false);
               if (item instanceof AnimatedGunItem animatedGun) {
                  long id = GeoItem.getId(stack);
                  AnimationController<GeoAnimatable> animationController = (AnimationController<GeoAnimatable>)animatedGun.getAnimatableInstanceCache()
                     .getManagerForId(id)
                     .getAnimationControllers()
                     .get("controller");
                  if (animationController != null) {
                     if (animatedGun.isInCarbineMode(stack)) {
                        animationController.tryTriggerAnimation("carbine_reload_stop");
                     } else {
                        animationController.tryTriggerAnimation("reload_stop");
                     }
                  }
               }
            }
         } else if (tag.getBoolean("scguns:ReloadComplete")) {
            tag.putBoolean("scguns:IsPlayingReloadStop", true);
            tag.remove("InCriticalReloadPhase");
            ModSyncedDataKeys.RELOADING.setValue(player, false);
            if (item instanceof AnimatedGunItem) {
               long id = GeoItem.getId(stack);
               AnimationController<GeoAnimatable> animationController = (AnimationController<GeoAnimatable>)((AnimatedGunItem)item)
                  .getAnimatableInstanceCache()
                  .getManagerForId(id)
                  .getAnimationControllers()
                  .get("controller");
               if (animationController != null) {
                  animationController.tryTriggerAnimation("reload_stop");
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onClientTick(ClientTickEvent.Post event) {
      {
         Player player = Minecraft.getInstance().player;
         if (player != null) {
            if ((Boolean)ModSyncedDataKeys.RELOADING.getValue(player) && (Boolean)ModSyncedDataKeys.AIMING.getValue(player)) {
               ModSyncedDataKeys.AIMING.setValue(player, false);
               PacketHandler.getPlayChannel().sendToServer(new C2SMessageAim(false));
            }

            this.prevReloadTimer = this.reloadTimer;
            if ((Boolean)ModSyncedDataKeys.RELOADING.getValue(player)) {
               ItemStack stack = player.getMainHandItem();
               if (Minecraft.getInstance().isPaused() && stack.getItem() instanceof GunItem) {
                  Gun gun = ((GunItem)stack.getItem()).getModifiedGun(stack);
                  CompoundTag tag = NbtHelper.getOrCreateTag(stack);
                  if (gun.getReloads().getReloadType() == ReloadType.MANUAL) {
                     tag.putString("scguns:ReloadState", "STOPPING");
                     tag.putBoolean("scguns:IsPlayingReloadStop", true);
                     tag.remove("InReloadLoop");
                     tag.remove("scguns:IsReloading");
                     if (stack.getItem() instanceof AnimatedGunItem animatedGun) {
                        long id = GeoItem.getId(stack);
                        AnimationController<GeoAnimatable> controller = (AnimationController<GeoAnimatable>)animatedGun.getAnimatableInstanceCache()
                           .getManagerForId(id)
                           .getAnimationControllers()
                           .get("controller");
                        if (controller != null) {
                           controller.stop();
                           controller.setAnimationSpeed(1.0);
                           controller.tryTriggerAnimation(animatedGun.isInCarbineMode(stack) ? "carbine_reload_stop" : "reload_stop");
                        }
                     }
                  }

                  this.setReloading(false);
                  return;
               }
            }

            PacketHandler.getPlayChannel().sendToServer(new C2SMessageLeftOverAmmo());
            if ((Boolean)ModSyncedDataKeys.RELOADING.getValue(player) && this.reloadingSlot != player.getInventory().selected) {
               this.setReloading(false);
            }

            this.updateReloadTimer(player);
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof GunItem && !(stack.getItem() instanceof AnimatedGunItem)) {
               Gun gun = ((GunItem)stack.getItem()).getModifiedGun(stack);
               if (gun.getReloads().getReloadType() == ReloadType.MAG_FED && this.reloadTimer <= 0 && (Boolean)ModSyncedDataKeys.RELOADING.getValue(player)) {
                  PacketHandler.getPlayChannel().sendToServer(new C2SMessageGunLoaded());
                  this.setReloading(false);
               }
            }

            HUDRenderHandler.updateReserveAmmo(player);
         }
      }
   }

   @SubscribeEvent
   public void onKeyPressed(Key event) {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         ItemStack stack = player.getMainHandItem();
         if (stack.getItem() instanceof GunItem) {
            CompoundTag tag = NbtHelper.getOrCreateTag(stack);
            ((GunItem)stack.getItem()).getModifiedGun(stack);
            tag.getBoolean("InCriticalReloadPhase");
            if (KeyBinds.KEY_RELOAD.isDown() && event.getAction() == 1) {
               boolean currentlyReloading = (Boolean)ModSyncedDataKeys.RELOADING.getValue(player);
               if (currentlyReloading) {
                  return;
               }

               this.setReloading(true);
               HUDRenderHandler.updateReserveAmmo(player);
            }

            if (KeyBinds.KEY_UNLOAD.consumeClick() && event.getAction() == 1) {
               this.setReloading(false);
               PacketHandler.getPlayChannel().sendToServer(new C2SMessageUnload());
               HUDRenderHandler.stageReserveAmmoUpdate();
            }
         }
      }
   }

   // `InputEvent.MouseButton` is abstract in NeoForge (Pre/Post subclasses) and the bus
   // refuses listeners for abstract events; Pre is the input-handling phase.
   @SubscribeEvent
   public void onMouseInput(MouseButton.Pre event) {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         ItemStack stack = player.getMainHandItem();
         if (stack.getItem() instanceof GunItem) {
            CompoundTag tag = NbtHelper.getOrCreateTag(stack);
            Gun gun = ((GunItem)stack.getItem()).getModifiedGun(stack);
            boolean inCriticalPhase = tag.getBoolean("InCriticalReloadPhase");
            boolean isReloading = (Boolean)ModSyncedDataKeys.RELOADING.getValue(player);
            if (!isReloading && inCriticalPhase) {
               tag.remove("InCriticalReloadPhase");
               inCriticalPhase = false;
            }

            if (KeyBinds.getAimMapping().isDown() && event.getAction() == 1 && inCriticalPhase && gun.getReloads().getReloadType() != ReloadType.MANUAL) {
               return;
            }

            KeyBinds.getAimMapping().isDown();
         }
      }
   }

   public void setReloading(boolean reloading) {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         ItemStack stack = player.getMainHandItem();
         CompoundTag tag = NbtHelper.getOrCreateTag(stack);
         boolean inCriticalPhase = tag.getBoolean("InCriticalReloadPhase");
         if (reloading) {
            if (stack.getItem() instanceof GunItem gunItem) {
               if (inCriticalPhase) {
                  return;
               }

               if (!tag.contains("IgnoreAmmo", 1)) {
                  if ((Boolean)ModSyncedDataKeys.RELOADING.getValue(player)) {
                     return;
                  }

                  if (stack.getItem() instanceof AnimatedGunItem animatedGun) {
                     gunItem.getModifiedGun(stack);
                     if ((Boolean)ModSyncedDataKeys.AIMING.getValue(player)) {
                        ModSyncedDataKeys.AIMING.setValue(player, false);
                        PacketHandler.getPlayChannel().sendToServer(new C2SMessageAim(false));
                        AimingHandler.get().aiming = false;
                     }

                     if (!tag.getBoolean("IsDrawn")) {
                        tag.putBoolean("IsDrawn", true);
                        tag.putInt("DrawnTick", 15);
                     }

                     boolean hasReloadTags = tag.getBoolean("IsReloading") || tag.getBoolean("scguns:IsReloading") || tag.contains("scguns:ReloadState");
                     if (hasReloadTags && !(Boolean)ModSyncedDataKeys.RELOADING.getValue(player)) {
                        animatedGun.cleanupReloadState(tag);
                     }

                     if (tag.getBoolean("IsReloading") && (Boolean)ModSyncedDataKeys.RELOADING.getValue(player)) {
                        return;
                     }
                  }

                  Gun gun = gunItem.getModifiedGun(stack);
                  if (tag.getInt("AmmoCount") >= GunModifierHelper.getModifiedAmmoCapacity(stack, gun)) {
                     return;
                  }

                  if (Gun.findAmmo(player, gun.getProjectile().getItem()).stack().isEmpty()) {
                     return;
                  }

                  ResourceLocation preReloadSound = gun.getSounds().getPreReload();
                  if (preReloadSound != null) {
                     Config.SERVER.reloadMaxDistance.get();
                     player.playSound(Objects.requireNonNull((SoundEvent)BuiltInRegistries.SOUND_EVENT.get(preReloadSound)), 0.7F, 1.0F);
                  }

                  if (NeoForge.EVENT_BUS.post(new GunReloadEvent.Pre(player, stack)).isCanceled()) {
                     return;
                  }

                  ModSyncedDataKeys.RELOADING.setValue(player, true);
                  PacketHandler.getPlayChannel().sendToServer(new C2SMessageReload(true));
                  this.reloadingSlot = player.getInventory().selected;
                  if (stack.getItem() instanceof AnimatedGunItem animatedGun) {
                     tag.putBoolean("IsReloading", true);
                     tag.putBoolean("scguns:IsReloading", true);
                     tag.remove("ReloadComplete");
                     tag.remove("scguns:ReloadComplete");
                     if (gun.getReloads().getReloadType() != ReloadType.MANUAL) {
                        tag.putBoolean("InCriticalReloadPhase", true);
                     }

                     long id = GeoItem.getId(stack);
                     AnimationController<GeoAnimatable> controller = (AnimationController<GeoAnimatable>)animatedGun.getAnimatableInstanceCache()
                        .getManagerForId(id)
                        .getAnimationControllers()
                        .get("controller");
                     if (controller != null) {
                        ReloadType reloadType = gun.getReloads().getReloadType();
                        if (reloadType == ReloadType.MAG_FED || reloadType == ReloadType.SINGLE_ITEM) {
                           tag.putBoolean("IsMagReload", true);
                           if (animatedGun.isInCarbineMode(stack)) {
                              controller.tryTriggerAnimation("carbine_reload");
                           } else {
                              controller.tryTriggerAnimation("reload");
                           }
                        }

                        if (gun.getReloads().getReloadType() == ReloadType.MANUAL) {
                           tag.putBoolean("IsManualReload", true);
                           tag.putString("scguns:ReloadState", "NONE");
                           if (animatedGun.isInCarbineMode(stack)) {
                              controller.tryTriggerAnimation("carbine_reload_start");
                           } else {
                              controller.tryTriggerAnimation("reload_start");
                           }
                        }
                     }
                  }

                  NeoForge.EVENT_BUS.post(new GunReloadEvent.Post(player, stack)).isCanceled();
               }
            }
         } else {
            if (inCriticalPhase) {
               Gun gunx = ((GunItem)stack.getItem()).getModifiedGun(stack);
               if (gunx.getReloads().getReloadType() != ReloadType.MANUAL) {
                  return;
               }
            }

            if (stack.getItem() instanceof AnimatedGunItem animatedGun) {
               Gun gunx = ((GunItem)stack.getItem()).getModifiedGun(stack);
               if (gunx.getReloads().getReloadType() == ReloadType.MANUAL) {
                  if (tag.getBoolean("scguns:IsReloading") && !tag.getBoolean("scguns:IsPlayingReloadStop")) {
                     tag.putString("scguns:ReloadState", "STOPPING");
                     tag.putBoolean("scguns:IsPlayingReloadStop", true);
                     PacketHandler.getPlayChannel().sendToServer(new C2SMessageReload(false));
                  } else {
                     animatedGun.cleanupReloadState(tag);
                     ModSyncedDataKeys.RELOADING.setValue(player, false);
                     tag.remove("InCriticalReloadPhase");
                     tag.remove("IsManualReload");
                     tag.remove("InReloadLoop");
                     tag.remove("PendingStopTransition");
                     tag.remove("PendingStopTime");
                     tag.remove("LastReloadStateChange");
                     tag.remove("ManualReloadInitialized");
                     PacketHandler.getPlayChannel().sendToServer(new C2SMessageReload(false));
                  }
               } else if (!inCriticalPhase) {
                  animatedGun.cleanupReloadState(tag);
                  ModSyncedDataKeys.RELOADING.setValue(player, false);
                  tag.remove("InCriticalReloadPhase");
                  PacketHandler.getPlayChannel().sendToServer(new C2SMessageReload(false));
               }
            } else {
               ModSyncedDataKeys.RELOADING.setValue(player, false);
               tag.remove("InCriticalReloadPhase");
               PacketHandler.getPlayChannel().sendToServer(new C2SMessageReload(false));
               this.reloadingSlot = -1;
            }
         }

         if (stack.getItem() instanceof GunItem) {
            HUDRenderHandler.updateReserveAmmo(player);
         }
      }
   }

   private void updateReloadTimer(Player player) {
      ItemStack stack = player.getMainHandItem();
      if ((Boolean)ModSyncedDataKeys.RELOADING.getValue(player)) {
         if (stack.getItem() instanceof AnimatedGunItem) {
            return;
         }

         if (this.startReloadTick == -1) {
            this.startReloadTick = player.tickCount + 5;
         }

         if (this.reloadTimer < 5) {
            this.reloadTimer++;
         }
      } else {
         if (this.startReloadTick != -1) {
            this.startReloadTick = -1;
         }

         if (this.reloadTimer > 0) {
            this.reloadTimer--;
         }
      }
   }

   public int getStartReloadTick() {
      return this.startReloadTick;
   }

   public int getReloadTimer() {
      return this.reloadTimer;
   }

   public float getReloadProgress(float partialTicks) {
      return ((float)this.prevReloadTimer + (float)(this.reloadTimer - this.prevReloadTimer) * partialTicks) / 5.0F;
   }
}
