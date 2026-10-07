package top.ribs.scguns.client.handler;


import top.ribs.scguns.util.NbtHelper;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeFov;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import top.ribs.scguns.Config;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.client.KeyBinds;
import top.ribs.scguns.client.util.PropertyHelper;
import top.ribs.scguns.common.GripType;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.common.ReloadType;
import top.ribs.scguns.compat.PlayerReviveHelper;
import top.ribs.scguns.debug.Debug;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.animated.AnimatedGunItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.C2SMessageAim;
import top.ribs.scguns.util.GunEnchantmentHelper;
import top.ribs.scguns.util.GunModifierHelper;

public class AimingHandler {
   private static AimingHandler instance;
   private static final double MAX_AIM_PROGRESS = 5.0;
   private final AimingHandler.AimTracker localTracker = new AimingHandler.AimTracker();
   private final Map<Player, AimingHandler.AimTracker> aimingMap = new WeakHashMap<>();
   private double normalisedAdsProgress;
   public boolean aiming = false;
   private boolean wasKeyPressed = false;

   public static AimingHandler get() {
      if (instance == null) {
         instance = new AimingHandler();
      }

      return instance;
   }

   private AimingHandler() {
      super();
   }

   @SubscribeEvent
   public void onPlayerTick(PlayerTickEvent.Pre event) {
      {
         Player player = event.getEntity();
         AimingHandler.AimTracker tracker = this.getAimTracker(player);
         if (tracker != null) {
            tracker.handleAiming(player, player.getItemInHand(InteractionHand.MAIN_HAND));
            if (!tracker.isAiming()) {
               this.aimingMap.remove(player);
            }
         }
      }
   }

   @Nullable
   private AimingHandler.AimTracker getAimTracker(Player player) {
      if ((Boolean)ModSyncedDataKeys.AIMING.getValue(player) && !this.aimingMap.containsKey(player)) {
         this.aimingMap.put(player, new AimingHandler.AimTracker());
      }

      return this.aimingMap.get(player);
   }

   public float getAimProgress(Player player, float partialTicks) {
      if (player.isLocalPlayer()) {
         return (float)this.localTracker.getNormalProgress(partialTicks);
      } else {
         AimingHandler.AimTracker tracker = this.getAimTracker(player);
         return tracker != null ? (float)tracker.getNormalProgress(partialTicks) : 0.0F;
      }
   }

   @SubscribeEvent
   public void onClientTick(ClientTickEvent.Pre event) {
      {
         Player player = Minecraft.getInstance().player;
         if (player != null) {
            ItemStack heldItem = player.getMainHandItem();
            boolean isReloading = (Boolean)ModSyncedDataKeys.RELOADING.getValue(player);
            boolean inCriticalPhase = false;
            if (heldItem.getItem() instanceof GunItem) {
               CompoundTag tag = NbtHelper.getOrCreateTag(heldItem);
               inCriticalPhase = tag.getBoolean("InCriticalReloadPhase");
               Gun gun = ((GunItem)heldItem.getItem()).getModifiedGun(heldItem);
               if (!isReloading && !inCriticalPhase && heldItem.getItem() instanceof AnimatedGunItem animatedGun) {
                  String reloadState = tag.getString("scguns:ReloadState");
                  if (reloadState.equals("STOPPING") && !tag.getBoolean("scguns:IsPlayingReloadStop")) {
                     animatedGun.cleanupReloadState(tag);
                  }

                  if (gun.getReloads().getReloadType() == ReloadType.MANUAL
                     && (
                        tag.getBoolean("IsManualReload")
                           || tag.getBoolean("InReloadLoop")
                           || tag.contains("LastReloadStateChange")
                           || tag.contains("ManualReloadInitialized")
                     )) {
                     tag.remove("IsManualReload");
                     tag.remove("InReloadLoop");
                     tag.remove("PendingStopTransition");
                     tag.remove("PendingStopTime");
                     tag.remove("LastReloadStateChange");
                     tag.remove("ManualReloadInitialized");
                     tag.remove("scguns:ReloadState");
                  }
               }

               if (inCriticalPhase && gun.getReloads().getReloadType() != ReloadType.MANUAL) {
                  this.aiming = false;
                  this.wasKeyPressed = KeyBinds.getAimMapping().isDown();
                  if ((Boolean)ModSyncedDataKeys.AIMING.getValue(player)) {
                     ModSyncedDataKeys.AIMING.setValue(player, false);
                     PacketHandler.getPlayChannel().sendToServer(new C2SMessageAim(false));
                  }

                  this.localTracker.handleAiming(player, player.getItemInHand(InteractionHand.MAIN_HAND));
                  return;
               }
            }

            if (!isReloading && !inCriticalPhase) {
               boolean currentKeyPressed = KeyBinds.getAimMapping().isDown();
               boolean toggleAdsEnabled = (Boolean)Config.COMMON.gameplay.toggleADS.get();
               if (toggleAdsEnabled) {
                  if (currentKeyPressed && !this.wasKeyPressed) {
                     this.aiming = !this.aiming;
                  }
               } else {
                  this.aiming = currentKeyPressed;
               }

               this.wasKeyPressed = currentKeyPressed;
               if (ScorchedGuns.controllableLoaded) {
                  boolean controllerAiming = ControllerHandler.isAiming();
                  if (toggleAdsEnabled) {
                     if (controllerAiming && !this.wasKeyPressed) {
                        this.aiming = !this.aiming;
                     }
                  } else {
                     this.aiming |= controllerAiming;
                  }
               }

               boolean shouldBeAiming = this.aiming;
               if (shouldBeAiming) {
                  if (heldItem.getItem() instanceof GunItem) {
                     Gun gunx = ((GunItem)heldItem.getItem()).getModifiedGun(heldItem);
                     GripType gripType = gunx.getGeneral().getGripType(heldItem);
                     if (!gunx.canAimDownSight()) {
                        shouldBeAiming = false;
                        this.aiming = false;
                     } else if (gripType == GripType.ONE_HANDED && !player.getOffhandItem().isEmpty()) {
                        shouldBeAiming = false;
                        this.aiming = false;
                     }
                  } else {
                     shouldBeAiming = false;
                     this.aiming = false;
                  }
               }

               if (shouldBeAiming) {
                  if (!(Boolean)ModSyncedDataKeys.AIMING.getValue(player)) {
                     ModSyncedDataKeys.AIMING.setValue(player, true);
                     PacketHandler.getPlayChannel().sendToServer(new C2SMessageAim(true));
                  }
               } else if ((Boolean)ModSyncedDataKeys.AIMING.getValue(player)) {
                  ModSyncedDataKeys.AIMING.setValue(player, false);
                  PacketHandler.getPlayChannel().sendToServer(new C2SMessageAim(false));
               }

               this.localTracker.handleAiming(player, player.getItemInHand(InteractionHand.MAIN_HAND));
            } else {
               this.aiming = false;
               this.wasKeyPressed = KeyBinds.getAimMapping().isDown();
               if ((Boolean)ModSyncedDataKeys.AIMING.getValue(player)) {
                  ModSyncedDataKeys.AIMING.setValue(player, false);
                  PacketHandler.getPlayChannel().sendToServer(new C2SMessageAim(false));
               }

               this.localTracker.handleAiming(player, player.getItemInHand(InteractionHand.MAIN_HAND));
            }
         }
      }
   }

   public boolean isAiming() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return false;
      } else if (mc.player.isSpectator()) {
         return false;
      } else if ((Boolean)ModSyncedDataKeys.RELOADING.getValue(mc.player)) {
         return false;
      } else {
         ItemStack heldItem = mc.player.getMainHandItem();
         if (heldItem.getItem() instanceof GunItem) {
            CompoundTag tag = NbtHelper.getOrCreateTag(heldItem);
            if (tag.contains("OldScopeType")) {
               tag.remove("OldScopeType");
            }

            if (tag.contains("ScopeCacheTime")) {
               long cacheTime = tag.getLong("ScopeCacheTime");
               if (System.currentTimeMillis() - cacheTime > 5000L) {
                  tag.remove("ScopeCacheTime");
               }
            }

            String reloadState = tag.getString("scguns:ReloadState");
            if (!reloadState.isEmpty() && !reloadState.equals("NONE")) {
               return false;
            }
         }

         if (Debug.isForceAim()) {
            return true;
         } else if (mc.screen != null) {
            this.aiming = false;
            return false;
         } else if (PlayerReviveHelper.isBleeding(mc.player)) {
            this.aiming = false;
            return false;
         } else if (!(heldItem.getItem() instanceof GunItem)) {
            this.aiming = false;
            return false;
         } else {
            Gun gun = ((GunItem)heldItem.getItem()).getModifiedGun(heldItem);
            if (!gun.canAimDownSight()) {
               this.aiming = false;
               return false;
            } else {
               if (heldItem.getItem() instanceof AnimatedGunItem) {
                  CompoundTag tagx = NbtHelper.getOrCreateTag(heldItem);
                  if (tagx.getBoolean("IsDrawing") && tagx.getInt("DrawnTick") < 15) {
                     this.aiming = false;
                     return false;
                  }
               }

               GripType gripType = gun.getGeneral().getGripType(heldItem);
               if (gripType == GripType.ONE_HANDED && !mc.player.getOffhandItem().isEmpty()) {
                  this.aiming = false;
                  return false;
               } else if (!this.localTracker.isAiming() && this.isLookingAtInteractableBlock()) {
                  this.aiming = false;
                  return false;
               } else {
                  return this.aiming;
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onFovUpdate(ComputeFov event) {
      if (event.usedConfiguredFov()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && !mc.player.getMainHandItem().isEmpty() && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
            ItemStack heldItem = mc.player.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem gunItem) {
               if (get().getNormalisedAdsProgress() != 0.0) {
                  if (!(Boolean)ModSyncedDataKeys.RELOADING.getValue(mc.player)) {
                     Gun modifiedGun = gunItem.getModifiedGun(heldItem);
                     if (modifiedGun.getModules().getZoom() != null) {
                        double time = PropertyHelper.getSightAnimations(heldItem, modifiedGun).getFovCurve().apply(this.normalisedAdsProgress);
                        float modifier = Gun.getFovModifier(heldItem, modifiedGun);
                        modifier = (1.0F - modifier) * (float)time;
                        event.setFOV(event.getFOV() - event.getFOV() * (double)modifier);
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onClientTick(LoggingOut event) {
      this.aimingMap.clear();
   }

   // `RenderGuiLayerEvent` is abstract in NeoForge (Pre/Post subclasses) and the bus
   // refuses listeners for abstract events; Post reads the finished ADS progress.
   @SubscribeEvent(
      receiveCanceled = true
   )
   public void onRenderOverlay(RenderGuiLayerEvent.Post event) {
      this.normalisedAdsProgress = this.localTracker.getNormalProgress(event.getPartialTick().getGameTimeDeltaPartialTick(false));
   }

   public boolean isZooming() {
      return this.aiming;
   }

   public boolean isLookingAtInteractableBlock() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.hitResult != null && mc.level != null) {
         if (mc.hitResult instanceof BlockHitResult result) {
            BlockState state = mc.level.getBlockState(result.getBlockPos());
            Block var5 = state.getBlock();
         } else if (mc.hitResult instanceof EntityHitResult result) {
            return result.getEntity() instanceof ItemFrame;
         }
      }

      return false;
   }

   public double getNormalisedAdsProgress() {
      return this.normalisedAdsProgress;
   }

   public class AimTracker {
      private double currentAim;
      private double previousAim;

      public AimTracker() {
         super();
      }

      private void handleAiming(Player player, ItemStack heldItem) {
         this.previousAim = this.currentAim;
         if ((Boolean)ModSyncedDataKeys.AIMING.getValue(player) || player.isLocalPlayer() && AimingHandler.this.isAiming()) {
            if (this.currentAim < 5.0) {
               double speed = GunEnchantmentHelper.getAimDownSightSpeed(heldItem);
               speed = GunModifierHelper.getModifiedAimDownSightSpeed(heldItem, speed);
               this.currentAim += speed;
               if (this.currentAim > 5.0) {
                  this.currentAim = 5.0;
               }
            }
         } else if (this.currentAim > 0.0) {
            double speed = GunEnchantmentHelper.getAimDownSightSpeed(heldItem);
            speed = GunModifierHelper.getModifiedAimDownSightSpeed(heldItem, speed);
            this.currentAim -= speed;
            if (this.currentAim < 0.0) {
               this.currentAim = 0.0;
            }
         }
      }

      public boolean isAiming() {
         return this.currentAim != 0.0 || this.previousAim != 0.0;
      }

      public double getNormalProgress(float partialTicks) {
         return Mth.clamp((this.previousAim + (this.currentAim - this.previousAim) * (double)partialTicks) / 5.0, 0.0, 1.0);
      }
   }
}
