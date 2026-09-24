package top.ribs.scguns.client.handler;


import top.ribs.scguns.util.NbtHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import top.ribs.scguns.Config;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.client.KeyBinds;
import top.ribs.scguns.common.ChargeHandler;
import top.ribs.scguns.common.FireMode;
import top.ribs.scguns.common.GripType;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.common.ReloadType;
import top.ribs.scguns.compat.PlayerReviveHelper;
import top.ribs.scguns.event.GunFireEvent;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.item.BayonetItem;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.animated.AnimatedDualWieldGunItem;
import top.ribs.scguns.item.animated.AnimatedGunItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.C2SMessageOffhandMelee;
import top.ribs.scguns.network.message.C2SMessagePreFireSound;
import top.ribs.scguns.network.message.C2SMessageShoot;
import top.ribs.scguns.network.message.C2SMessageShooting;
import top.ribs.scguns.network.message.C2SMessageStopBeam;
import top.ribs.scguns.util.GunCompositeStatHelper;

public class ShootingHandler {
   private static ShootingHandler instance;
   private int fireTimer;
   private int burstCooldownTimer;
   private boolean wasRightClickPressed = false;
   private boolean wasHoldingFireWhenEmpty = false;
   private boolean hasReleasedFireSinceEmpty = false;
   private boolean hasBufferedShot = false;
   private int bufferTimer = 0;
   private boolean lastFireKeyState = false;
   private static final int BUFFER_DURATION = 4;
   private int switchCooldown = 0;
   private ItemStack lastHeldItem = ItemStack.EMPTY;
   private boolean shooting;
   private boolean doEmptyClick;
   private int slot = -1;
   private int burstCounter = 0;

   public static ShootingHandler get() {
      if (instance == null) {
         instance = new ShootingHandler();
      }

      return instance;
   }

   private ShootingHandler() {
      super();
      this.fireTimer = 0;
   }

   private boolean isInGame() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.getOverlay() != null) {
         return true;
      } else if (mc.screen != null) {
         return true;
      } else {
         return !mc.mouseHandler.isMouseGrabbed() ? true : !mc.isWindowActive();
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public void onMouseClick(InteractionKeyMappingTriggered event) {
      if (!event.isCanceled()) {
         Minecraft mc = Minecraft.getInstance();
         Player player = mc.player;
         if (player != null) {
            if (!PlayerReviveHelper.isBleeding(player)) {
               if (event.isAttack()) {
                  ItemStack heldItem = player.getMainHandItem();
                  if (heldItem.getItem() instanceof GunItem) {
                     event.setSwingHand(false);
                     event.setCanceled(true);
                  }
               } else if (event.isUseItem()) {
                  ItemStack heldItem = player.getMainHandItem();
                  if (heldItem.getItem() instanceof GunItem gunItem) {
                     if (event.getHand() == InteractionHand.MAIN_HAND) {
                        Gun modifiedGun = gunItem.getModifiedGun(heldItem);
                        GripType gripType = modifiedGun.getGeneral().getGripType(heldItem);
                        if (gripType == GripType.ONE_HANDED && !player.getOffhandItem().isEmpty()) {
                           ItemStack offhandItem = player.getOffhandItem();
                           if (offhandItem.getItem() instanceof SwordItem || offhandItem.getItem() instanceof BayonetItem) {
                              boolean currentRightClick = mc.options.keyUse.isDown();
                              if (currentRightClick && !this.wasRightClickPressed) {
                                 event.setCanceled(true);
                                 event.setSwingHand(false);
                                 if (mc.hitResult != null && mc.hitResult.getType() == Type.ENTITY) {
                                    EntityHitResult entityHit = (EntityHitResult)mc.hitResult;
                                    Entity target = entityHit.getEntity();
                                    PacketHandler.getPlayChannel()
                                       .sendToServer(
                                          new C2SMessageOffhandMelee(
                                             target.getId(),
                                             (float)entityHit.getLocation().x,
                                             (float)entityHit.getLocation().y,
                                             (float)entityHit.getLocation().z
                                          )
                                       );
                                 } else {
                                    PacketHandler.getPlayChannel().sendToServer(new C2SMessageOffhandMelee(-1, 0.0F, 0.0F, 0.0F));
                                 }
                              }

                              this.wasRightClickPressed = currentRightClick;
                              return;
                           }
                        }
                     }

                     if (event.getHand() == InteractionHand.OFF_HAND) {
                        Gun modifiedGun = gunItem.getModifiedGun(heldItem);
                        GripType gripType = modifiedGun.getGeneral().getGripType(heldItem);
                        if (gripType == GripType.ONE_HANDED) {
                           return;
                        }

                        if (player.getOffhandItem().getItem() == Items.SHIELD && player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND) {
                           return;
                        }

                        event.setCanceled(true);
                        event.setSwingHand(false);
                        return;
                     }

                     if (AimingHandler.get().isZooming() && AimingHandler.get().isLookingAtInteractableBlock()) {
                        event.setCanceled(true);
                        event.setSwingHand(false);
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public void onHandleShooting(ClientTickEvent.Pre event) {
      {
         if (!this.isInGame()) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player != null) {
               ItemStack heldItem = player.getMainHandItem();
               if (heldItem.getItem() instanceof GunItem gunItem && !PlayerReviveHelper.isBleeding(player)) {
                  Gun modifiedGun = gunItem.getModifiedGun(heldItem);
                  boolean isCurrentlyPressingFire = KeyBinds.getShootMapping().isDown();
                  if (ScorchedGuns.controllableLoaded) {
                     isCurrentlyPressingFire |= ControllerHandler.isShooting();
                  }

                  if (modifiedGun.getGeneral().getFireMode() == FireMode.SEMI_AUTO) {
                     this.handleSemiAutoWithBuffer(player, heldItem, isCurrentlyPressingFire);
                  } else {
                     this.handleOtherFireModes(player, heldItem, modifiedGun, isCurrentlyPressingFire);
                  }

                  this.lastFireKeyState = isCurrentlyPressingFire;
                  return;
               }

               if (this.shooting) {
                  this.shooting = false;
                  PacketHandler.getPlayChannel().sendToServer(new C2SMessageShooting(false));
                  this.lastFireKeyState = false;
                  this.hasBufferedShot = false;
                  this.bufferTimer = 0;
               }
            } else {
               this.shooting = false;
               this.lastFireKeyState = false;
               this.hasBufferedShot = false;
               this.bufferTimer = 0;
            }
         }
      }
   }

   private void handleSemiAutoWithBuffer(Player player, ItemStack heldItem, boolean isCurrentlyPressingFire) {
      ItemCooldowns tracker = player.getCooldowns();
      boolean isOnCooldown = tracker.isOnCooldown(heldItem.getItem());
      boolean newFireInput = isCurrentlyPressingFire && !this.lastFireKeyState;
      if (newFireInput) {
         if (!isOnCooldown) {
            if (!this.shooting) {
               this.shooting = true;
               PacketHandler.getPlayChannel().sendToServer(new C2SMessageShooting(true));
            }
         } else {
            this.hasBufferedShot = true;
            this.bufferTimer = 4;
         }
      }

      if (this.hasBufferedShot && !isOnCooldown) {
         if (!this.shooting) {
            this.shooting = true;
            PacketHandler.getPlayChannel().sendToServer(new C2SMessageShooting(true));
         }

         this.hasBufferedShot = false;
         this.bufferTimer = 0;
      }

      if (this.hasBufferedShot) {
         this.bufferTimer--;
         if (this.bufferTimer <= 0) {
            this.hasBufferedShot = false;
         }
      }

      if (this.shooting && !isCurrentlyPressingFire && !this.hasBufferedShot) {
         this.shooting = false;
         PacketHandler.getPlayChannel().sendToServer(new C2SMessageShooting(false));
      }
   }

   private void handleOtherFireModes(Player player, ItemStack heldItem, Gun modifiedGun, boolean shouldShoot) {
      this.hasBufferedShot = false;
      this.bufferTimer = 0;
      if (modifiedGun.getGeneral().getFireMode() != FireMode.BEAM && modifiedGun.getGeneral().getFireMode() != FireMode.SEMI_BEAM) {
         if (shouldShoot && this.burstCooldownTimer <= 0) {
            if (!this.shooting) {
               this.shooting = true;
               PacketHandler.getPlayChannel().sendToServer(new C2SMessageShooting(true));
            }
         } else if (this.shooting) {
            this.shooting = false;
            PacketHandler.getPlayChannel().sendToServer(new C2SMessageShooting(false));
         }
      } else if (shouldShoot && this.burstCooldownTimer <= 0) {
         if (!this.shooting) {
            this.shooting = true;
            PacketHandler.getPlayChannel().sendToServer(new C2SMessageShooting(true));
         }
      } else if (this.shooting) {
         this.shooting = false;
         PacketHandler.getPlayChannel().sendToServer(new C2SMessageShooting(false));
         PacketHandler.getPlayChannel().sendToServer(new C2SMessageStopBeam());
      }
   }

   private boolean isEmpty(Player player, ItemStack heldItem) {
      if (!(heldItem.getItem() instanceof GunItem)) {
         return false;
      } else {
         return player.isSpectator() ? false : (!Gun.hasAmmo(heldItem) || !Gun.canShoot(heldItem)) && !player.isCreative();
      }
   }

   public void fire(Player player, ItemStack heldItem) {
      if (heldItem.getItem() instanceof GunItem gunItem) {
         if (heldItem.isDamageableItem() && heldItem.getDamageValue() >= heldItem.getMaxDamage() - 1) {
            player.displayClientMessage(Component.translatable("message.scguns.gun_broken").withStyle(ChatFormatting.RED), true);
         } else if (!heldItem.isDamageableItem() || heldItem.getDamageValue() < heldItem.getMaxDamage() - 1) {
            Gun gun = gunItem.getModifiedGun(heldItem);
            boolean isPulse = gun.getGeneral().getFireMode() == FireMode.PULSE;
            if (!this.isEmpty(player, heldItem)) {
               this.wasHoldingFireWhenEmpty = false;
               this.hasReleasedFireSinceEmpty = false;
               if (player.isSprinting()) {
                  player.setSprinting(false);
               }

               if (this.canFire(player, heldItem)) {
                  ItemCooldowns tracker = player.getCooldowns();
                  if (!tracker.isOnCooldown(heldItem.getItem())) {
                     if (NeoForge.EVENT_BUS.post(new GunFireEvent.Pre(player, heldItem)).isCanceled()) {
                        return;
                     }

                     if (gunItem instanceof AnimatedGunItem animatedGunItem) {
                        long id = GeoItem.getId(heldItem);
                        AnimationController<GeoAnimatable> controller = (AnimationController<GeoAnimatable>)animatedGunItem.getAnimatableInstanceCache()
                           .getManagerForId(id)
                           .getAnimationControllers()
                           .get("controller");
                        controller.forceAnimationReset();
                        if (gunItem instanceof AnimatedDualWieldGunItem) {
                           boolean useAlternate = DualWieldShotTracker.get().shouldUseAlternateAnimation(player.getId());
                           if ((Boolean)ModSyncedDataKeys.AIMING.getValue(player)) {
                              controller.tryTriggerAnimation(useAlternate ? "aim_shoot1" : "aim_shoot");
                           } else {
                              controller.tryTriggerAnimation(useAlternate ? "shoot1" : "shoot");
                           }
                        }
                     }

                     int rate = GunCompositeStatHelper.getCompositeRate(heldItem, gun, player);
                     tracker.addCooldown(heldItem.getItem(), rate);
                     if (Gun.hasBurstFire(heldItem)) {
                        if (this.burstCounter == 0) {
                           this.burstCounter = Gun.getBurstCount(heldItem);
                        }

                        this.burstCounter--;
                        if (this.burstCounter == 0) {
                           this.burstCooldownTimer = Gun.getBurstCooldown(heldItem);
                        }
                     }

                     PacketHandler.getPlayChannel().sendToServer(new C2SMessageShoot(player));
                     NeoForge.EVENT_BUS.post(new GunFireEvent.Post(player, heldItem)).isCanceled();
                  }
               }
            } else {
               ItemCooldowns tracker = player.getCooldowns();
               if (!tracker.isOnCooldown(heldItem.getItem())
                  && this.doEmptyClick
                  && heldItem.getItem() instanceof GunItem
                  && this.canUseTrigger(player, heldItem)) {
                  this.doEmptyClick = false;
                  boolean isCurrentlyHoldingFire = KeyBinds.getShootMapping().isDown();
                  if (isPulse) {
                     ChargeHandler.updateChargeTime(player, heldItem, false);
                     this.fireTimer = 0;
                  }

                  if ((Boolean)Config.COMMON.gameplay.enableAutoReload.get()) {
                     boolean isAutomaticWeapon = gun.getGeneral().getFireMode() == FireMode.AUTOMATIC;
                     if (isAutomaticWeapon) {
                        if (isCurrentlyHoldingFire && !this.wasHoldingFireWhenEmpty) {
                           this.wasHoldingFireWhenEmpty = true;
                           return;
                        }

                        if (this.wasHoldingFireWhenEmpty && !this.hasReleasedFireSinceEmpty) {
                           return;
                        }
                     }

                     boolean hasAmmoAvailable;
                     if (gun.getReloads().getReloadType() == ReloadType.SINGLE_ITEM) {
                        hasAmmoAvailable = !Gun.findAmmo(player, gun.getReloads().getReloadItem()).stack().isEmpty();
                     } else {
                        hasAmmoAvailable = !Gun.findAmmo(player, gun.getProjectile().getItem()).stack().isEmpty();
                     }

                     boolean isReloading = (Boolean)ModSyncedDataKeys.RELOADING.getValue(player);
                     int currentAmmo = Gun.getAmmoCount(heldItem);
                     // Modified capacity, not the value in the gun's data: the server fills up to the
                     // modified capacity (ReloadTracker.reloadItem) and decides "weapon full" the same
                     // way (ReloadTracker.isWeaponFull), as does every other check in the mod - HUD,
                     // tooltip, reload key, animated reload state machine. With the data value here the
                     // two sides could disagree, and an auto-reload that never sees a full magazine
                     // reloads forever (HANDOFF section 65).
                     int maxAmmo = top.ribs.scguns.util.GunModifierHelper.getModifiedAmmoCapacity(heldItem, gun);
                     if (hasAmmoAvailable && !isReloading && currentAmmo < maxAmmo) {
                        if (heldItem.getItem() instanceof AnimatedGunItem animatedGun) {
                           CompoundTag tag = NbtHelper.getOrCreateTag(heldItem);
                           String reloadState = tag.getString("scguns:ReloadState");
                           boolean isPlayingReloadStop = tag.getBoolean("scguns:IsPlayingReloadStop");
                           if (reloadState.equals("STOPPING") || isPlayingReloadStop) {
                              tag.remove("scguns:ReloadState");
                              tag.remove("scguns:IsPlayingReloadStop");
                              tag.remove("scguns:IsReloading");
                              tag.remove("IsReloading");
                              long id = GeoItem.getId(heldItem);
                              AnimationController<GeoAnimatable> controller = (AnimationController<GeoAnimatable>)animatedGun.getAnimatableInstanceCache()
                                 .getManagerForId(id)
                                 .getAnimationControllers()
                                 .get("controller");
                              if (controller != null) {
                                 controller.forceAnimationReset();
                                 if (animatedGun.isInCarbineMode(heldItem)) {
                                    controller.tryTriggerAnimation("carbine_idle");
                                 } else {
                                    controller.tryTriggerAnimation("idle");
                                 }
                              }
                           }
                        }

                        boolean canAutoReload = isCanAutoReload(heldItem);
                        if (canAutoReload) {
                           ReloadHandler.get().setReloading(true);
                           this.wasHoldingFireWhenEmpty = false;
                           this.hasReleasedFireSinceEmpty = false;
                        }
                     }
                  }
               }

               this.burstCounter = 0;
               this.hasBufferedShot = false;
               this.bufferTimer = 0;
            }
         }
      }
   }

   @SubscribeEvent
   public void onPostClientTick(ClientTickEvent.Post event) {
      {
         if (!this.isInGame()) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player != null) {
               boolean currentRightClick = mc.options.keyUse.isDown();
               if (!currentRightClick) {
                  this.wasRightClickPressed = false;
               }

               if (PlayerReviveHelper.isBleeding(player)) {
                  return;
               }

               boolean weaponChanged = !this.isSameWeapon(player);
               this.slot = player.getInventory().selected;
               if (weaponChanged) {
                  ModSyncedDataKeys.BURSTCOUNT.setValue(player, 0);
                  if (player.getMainHandItem().getItem() instanceof GunItem) {
                     this.burstCounter = 0;
                     this.burstCooldownTimer = 0;
                     this.fireTimer = 0;
                     this.wasHoldingFireWhenEmpty = false;
                     this.hasReleasedFireSinceEmpty = false;
                     ChargeHandler.resetCharge(player.getUUID());
                     this.switchCooldown = 5;
                  }

                  this.lastHeldItem = player.getMainHandItem().copy();
               }

               if (this.switchCooldown > 0) {
                  this.switchCooldown--;
                  return;
               }

               if ((Boolean)ModSyncedDataKeys.RELOADING.getValue(player)) {
                  this.burstCounter = 0;
                  this.burstCooldownTimer = 0;
               }

               ItemStack heldItem = player.getMainHandItem();
               if (heldItem.getItem() instanceof GunItem) {
                  Gun gun = ((GunItem)heldItem.getItem()).getModifiedGun(heldItem);
                  int maxChargeTime = gun.getGeneral().getFireTimer();
                  if (this.burstCooldownTimer > 0) {
                     this.burstCooldownTimer--;
                  }

                  boolean isHoldingFire = KeyBinds.getShootMapping().isDown();
                  if (!isHoldingFire && this.wasHoldingFireWhenEmpty) {
                     this.hasReleasedFireSinceEmpty = true;
                  }

                  if (gun.getGeneral().getFireMode() == FireMode.PULSE) {
                     if (isHoldingFire) {
                        if (!Gun.hasAmmo(heldItem) && !player.isCreative()) {
                           this.doEmptyClick = true;
                           this.fire(player, heldItem);
                           this.fireTimer = 0;
                           ChargeHandler.updateChargeTime(player, heldItem, false);
                        } else {
                           int preSoundThreshold = maxChargeTime / 3;
                           if (this.fireTimer == preSoundThreshold) {
                              PacketHandler.getPlayChannel().sendToServer(new C2SMessagePreFireSound(player));
                           }

                           this.fireTimer = Math.min(this.fireTimer + 1, maxChargeTime);
                           ChargeHandler.updateChargeTime(player, heldItem, true);
                        }
                     } else if (this.fireTimer > 0) {
                        this.fire(player, heldItem);
                        this.fireTimer = 0;
                        ChargeHandler.updateChargeTime(player, heldItem, false);
                     }
                  } else {
                     if (!isHoldingFire && maxChargeTime != 0) {
                        this.fireTimer = maxChargeTime;
                        if (this.wasHoldingFireWhenEmpty) {
                           this.wasHoldingFireWhenEmpty = false;
                        }

                        ChargeHandler.updateChargeTime(player, heldItem, false);
                     }

                     if ((isHoldingFire || this.burstCounter > 0) && this.burstCooldownTimer <= 0) {
                        if (maxChargeTime != 0) {
                           ItemCooldowns tracker = player.getCooldowns();
                           if (!tracker.isOnCooldown(heldItem.getItem())) {
                              if (this.fireTimer == maxChargeTime - 2) {
                                 PacketHandler.getPlayChannel().sendToServer(new C2SMessagePreFireSound(player));
                              }

                              this.fireTimer--;
                           } else {
                              this.fire(player, heldItem);
                              if (gun.getGeneral().getFireMode() == FireMode.SEMI_AUTO || gun.getGeneral().getFireMode() == FireMode.SEMI_BEAM) {
                                 mc.options.keyAttack.setDown(false);
                                 this.fireTimer = maxChargeTime;
                                 ChargeHandler.updateChargeTime(player, heldItem, false);
                              }
                           }
                        } else {
                           this.fire(player, heldItem);
                           if (gun.getGeneral().getFireMode() == FireMode.SEMI_AUTO || gun.getGeneral().getFireMode() == FireMode.SEMI_BEAM) {
                              mc.options.keyAttack.setDown(false);
                           }
                        }

                        ChargeHandler.updateChargeTime(player, heldItem, true);
                     } else {
                        ChargeHandler.updateChargeTime(player, heldItem, false);
                        this.doEmptyClick = true;
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean isCanAutoReload(ItemStack heldItem) {
      if (!(heldItem.getItem() instanceof AnimatedGunItem)) {
         return true;
      } else {
         CompoundTag tag = NbtHelper.getOrCreateTag(heldItem);
         Player player = Minecraft.getInstance().player;
         if (player != null) {
            boolean syncedReloading = (Boolean)ModSyncedDataKeys.RELOADING.getValue(player);
            if (syncedReloading) {
               return false;
            }
         }

         String reloadState = tag.getString("scguns:ReloadState");
         if (reloadState.isEmpty() || !reloadState.equals("LOADING") && !reloadState.equals("STOPPING")) {
            if (tag.getBoolean("scguns:IsPlayingReloadStop")) {
               return false;
            } else {
               if (heldItem.getItem() instanceof AnimatedGunItem animatedGun) {
                  long id = GeoItem.getId(heldItem);
                  AnimationController<GeoAnimatable> controller = (AnimationController<GeoAnimatable>)animatedGun.getAnimatableInstanceCache()
                     .getManagerForId(id)
                     .getAnimationControllers()
                     .get("controller");
                  if (controller != null) {
                     boolean playingReload = animatedGun.isAnimationPlaying(controller, "reload");
                     boolean playingCarbineReload = animatedGun.isAnimationPlaying(controller, "carbine_reload");
                     boolean playingReloadStart = animatedGun.isAnimationPlaying(controller, "reload_start");
                     boolean playingCarbineReloadStart = animatedGun.isAnimationPlaying(controller, "carbine_reload_start");
                     boolean playingReloadLoop = animatedGun.isAnimationPlaying(controller, "reload_loop");
                     boolean playingCarbineReloadLoop = animatedGun.isAnimationPlaying(controller, "carbine_reload_loop");
                     boolean playingReloadStop = animatedGun.isAnimationPlaying(controller, "reload_stop");
                     boolean playingCarbineReloadStop = animatedGun.isAnimationPlaying(controller, "carbine_reload_stop");
                     return !playingReload
                        && !playingCarbineReload
                        && !playingReloadStart
                        && !playingCarbineReloadStart
                        && !playingReloadLoop
                        && !playingCarbineReloadLoop
                        && !playingReloadStop
                        && !playingCarbineReloadStop;
                  }
               }

               return true;
            }
         } else {
            return false;
         }
      }
   }

   private boolean canFire(Player player, ItemStack heldItem) {
      if (player.isSpectator()) {
         return false;
      } else if (player.isCreative()) {
         return true;
      } else if (!Gun.hasAmmo(heldItem)) {
         return false;
      } else {
         Gun gun = ((GunItem)heldItem.getItem()).getModifiedGun(heldItem);
         if (gun.getGeneral().getFireMode() == FireMode.PULSE) {
            float chargeProgress = ChargeHandler.getChargeProgress(player, heldItem);
            return chargeProgress > 0.0F;
         } else {
            return Gun.canShoot(heldItem);
         }
      }
   }

   private boolean canUseTrigger(Player player, ItemStack heldItem) {
      if (player.isSpectator()) {
         return false;
      } else {
         return player.isCreative() ? true : Gun.canShoot(heldItem) || !Gun.hasAmmo(heldItem);
      }
   }

   private boolean isSameWeapon(Player player) {
      return this.slot == -1 ? true : player.getInventory().selected == this.slot;
   }

   public boolean isShooting() {
      return this.shooting;
   }
}
