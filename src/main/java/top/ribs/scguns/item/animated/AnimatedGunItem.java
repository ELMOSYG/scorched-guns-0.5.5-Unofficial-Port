package top.ribs.scguns.item.animated;


import top.ribs.scguns.util.NbtHelper;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController.State;
import software.bernie.geckolib.animation.keyframe.event.ParticleKeyframeEvent;
import software.bernie.geckolib.animation.keyframe.event.SoundKeyframeEvent;
import software.bernie.geckolib.animation.PlayState;
import top.ribs.scguns.Config;
import top.ribs.scguns.animations.GunAnimations;
import top.ribs.scguns.attributes.SCAttributes;
import top.ribs.scguns.client.KeyBinds;
import top.ribs.scguns.client.handler.AimingHandler;
import top.ribs.scguns.client.handler.MeleeAttackHandler;
import top.ribs.scguns.client.render.gun.animated.AnimatedGunRenderer;
import top.ribs.scguns.client.util.GunRotationHandler;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.common.ReloadType;
import top.ribs.scguns.event.GunEventBus;
import top.ribs.scguns.init.ModSounds;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.C2SMessageEjectCasing;
import top.ribs.scguns.network.message.C2SMessageGunLoaded;
import top.ribs.scguns.network.message.C2SMessageReload;
import top.ribs.scguns.util.GunEnchantmentHelper;
import top.ribs.scguns.util.GunModifierHelper;

public class AnimatedGunItem extends GunItem implements GeoAnimatable, GeoItem {
   private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
   private final String gunID;
   private final SoundEvent reloadSoundMagOut;
   private final SoundEvent reloadSoundMagIn;
   private final SoundEvent reloadSoundEnd;
   private final SoundEvent boltPullSound;
   private final SoundEvent boltReleaseSound;
   private int drawTick = 0;
   private static final String RELOAD_START_TIME = "reloadStartTime";
   public static final String RELOAD_STATE = "scguns:ReloadState";
   private final GunRotationHandler rotationHandler = new GunRotationHandler();

   public GunRotationHandler getRotationHandler() {
      return this.rotationHandler;
   }

   public AnimatedGunItem(
      Properties properties,
      String path,
      SoundEvent reloadSoundMagOut,
      SoundEvent reloadSoundMagIn,
      SoundEvent reloadSoundEnd,
      SoundEvent boltPullSound,
      SoundEvent boltReleaseSound
   ) {
      super(properties);
      this.gunID = path;
      this.reloadSoundMagOut = reloadSoundMagOut;
      this.reloadSoundMagIn = reloadSoundMagIn;
      this.reloadSoundEnd = reloadSoundEnd;
      this.boltPullSound = boltPullSound;
      this.boltReleaseSound = boltReleaseSound;
   }

   public boolean isInCarbineMode(ItemStack stack) {
      return !(stack.getItem() instanceof GunItem gunItem)
         ? false
         : gunItem.isOneHandedCarbineCandidate(stack) && (Gun.hasExtendedBarrel(stack) || Gun.hasStock(stack));
   }

   @OnlyIn(Dist.CLIENT)
   private void updateCarbineState(ItemStack stack, AnimationController<GeoAnimatable> controller) {
      boolean isCarbine = this.isInCarbineMode(stack);
      String currentAnim = controller.getCurrentAnimation() != null ? controller.getCurrentAnimation().animation().name() : "";
      boolean isCurrentCarbine = currentAnim.startsWith("carbine_");
      if (isCarbine != isCurrentCarbine) {
         controller.forceAnimationReset();
         controller.tryTriggerAnimation(isCarbine ? "carbine_idle" : "idle");
      }
   }

   public void inventoryTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Entity entity, int slot, boolean selected) {
      if (entity instanceof ItemEntity) {
         CompoundTag tag = NbtHelper.getOrCreateTag(stack);
         this.cleanupReloadState(tag);
         tag.remove("IsShooting");
         tag.remove("IsInspecting");
         tag.remove("IsAiming");
         tag.remove("IsRunning");
         tag.remove("IsDrawn");
         tag.remove("IsDrawing");
         tag.remove("DrawnTick");
         tag.remove("loaded");
         tag.remove("shouldStopOnLoopEnd");
         tag.remove("shouldTransitionToStop");
         tag.remove("MagazinePosition");
         tag.remove("MagazineOverride");
         tag.remove("scguns:MagazineTracking");
         tag.remove("_InitializedThisSession");
         tag.putBoolean("IsDroppedItem", true);
         if (world.isClientSide()) {
            tag.getBoolean("WasReloadingLastTick");
            boolean isReloading = tag.getBoolean("scguns:IsReloading");
            tag.putBoolean("WasReloadingLastTick", isReloading);
            // A dropped gun is never in anybody's hands.
            this.clientInventoryTick(stack, entity, slot, selected, false, false);
         }
      } else {
         CompoundTag tag = NbtHelper.getOrCreateTag(stack);
         tag.remove("IsDroppedItem");
         // Whether this stack is in the player's hands is decided by the slot it was ticked from,
         // never by GeoItem.getId(): a gun without an id component reports the same fallback id as
         // every other gun without one, so the id test could not tell a held gun from a stored one
         // (HANDOFF sections 68 and 69).
         boolean inHands = false;
         boolean justLeftHands = false;
         if (entity instanceof Player player) {
            inHands = selected || player.getMainHandItem() == stack || player.getOffhandItem() == stack;
            boolean wasHeld = tag.getBoolean("WasHeldLastTick");
            justLeftHands = wasHeld && !inHands;
            if (inHands && !wasHeld) {
               tag.remove("_InitializedThisSession");
               tag.remove("IsDrawn");
               tag.remove("DrawnTick");
            }

            tag.putBoolean("WasHeldLastTick", inHands);
            if (!inHands && !world.isClientSide) {
               clearStaleReloadState(stack);
            }
         }

         if (world.isClientSide()) {
            this.clientInventoryTick(stack, entity, slot, selected, inHands, justLeftHands);
         }

         // GeckoLib 4.6 moved the per-stack animatable id from the stack's NBT to a data component,
         // and GeoItem.getId() now falls back to Long.MAX_VALUE when the component is absent. The
         // "GeckoLibID" NBT key this used to write is not read by GeckoLib 4.9.3 at all (0 references
         // in the jar), so every gun in the game resolved to the same id: one shared
         // AnimationController per gun type, and every `GeoItem.getId(...) != id` identity test was
         // false. That is what let AnimatedGunItem treat every gun in the inventory as the held one
         // and play the reload animation on all of them (HANDOFF section 69).
         if (world instanceof ServerLevel serverLevel) {
            GeoItem.getOrAssignId(stack, serverLevel);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void clientInventoryTick(ItemStack stack, Entity entity, int slot, boolean selected, boolean inHands, boolean justLeftHands) {
      CompoundTag nbtCompound = NbtHelper.getOrCreateTag(stack);
      this.handleReloadStateSynchronization(stack, entity, nbtCompound, inHands);
      this.handleAnimationControllerUpdates(stack, entity, nbtCompound);
      if (entity instanceof Player player) {
         this.handlePlayerSpecificLogic(stack, player, nbtCompound, inHands, justLeftHands);
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void handleReloadStateSynchronization(ItemStack stack, Entity entity, CompoundTag nbtCompound, boolean inHands) {
      // The player level RELOADING flag only ever describes the gun in the player's hands. A gun
      // sitting in the inventory used to mirror it, which wrote "scguns:IsReloading" into every gun
      // there and fed the animation logic below - that is how reloading one gun made all the others
      // reload as well (HANDOFF section 69). Clear whatever such a gun is still carrying and leave
      // its state to the not-held clean-up.
      if (!inHands) {
         if (hasReloadState(nbtCompound)) {
            this.cleanupReloadState(nbtCompound);
            nbtCompound.remove("IsReloading");
            nbtCompound.remove("scguns:IsReloading");
            nbtCompound.remove("InCriticalReloadPhase");
            nbtCompound.remove("ReloadStartTime");
         }

         return;
      }

      boolean currentReloading = nbtCompound.getBoolean("scguns:IsReloading");
      boolean serverReloading = entity instanceof Player ? (Boolean)ModSyncedDataKeys.RELOADING.getValue((Player)entity) : false;
      String currentReloadState = nbtCompound.getString("scguns:ReloadState");
      boolean inCriticalPhase = nbtCompound.getBoolean("InCriticalReloadPhase");
      Gun modifiedGun = ((GunItem)stack.getItem()).getModifiedGun(stack);
      boolean isManualReload = modifiedGun.getReloads().getReloadType() == ReloadType.MANUAL;
      long currentTime = System.currentTimeMillis();
      if (currentReloading && inCriticalPhase && !isManualReload) {
         long reloadStartTime = nbtCompound.getLong("ReloadStartTime");
         if (reloadStartTime == 0L) {
            nbtCompound.putLong("ReloadStartTime", currentTime);
            reloadStartTime = currentTime;
         }

         if (currentTime - reloadStartTime > 10000L) {
            this.cleanupReloadState(nbtCompound);
            nbtCompound.remove("InCriticalReloadPhase");
            nbtCompound.remove("ReloadStartTime");
            nbtCompound.remove("ReloadAnimationStarted");
            nbtCompound.remove("ReloadAnimationRestarted");
            nbtCompound.remove("ReloadCompleted");
            if (entity instanceof Player) {
               ModSyncedDataKeys.RELOADING.setValue((Player)entity, false);
            }

            return;
         }
      } else {
         nbtCompound.remove("ReloadStartTime");
      }

      if (!serverReloading && inCriticalPhase && !isManualReload) {
         this.cleanupReloadState(nbtCompound);
         nbtCompound.remove("InCriticalReloadPhase");
         nbtCompound.remove("ReloadStartTime");
         nbtCompound.remove("ReloadAnimationStarted");
         nbtCompound.remove("ReloadAnimationRestarted");
         nbtCompound.remove("ReloadCompleted");
      } else if (!isManualReload) {
         if (currentReloadState.equals("STOPPING") || currentReloadState.equals("LOADING") || currentReloadState.equals("STARTING")) {
            nbtCompound.remove("scguns:ReloadState");
         }

         if (serverReloading && !currentReloading) {
            nbtCompound.putBoolean("scguns:IsReloading", true);
            nbtCompound.remove("scguns:ReloadState");
            nbtCompound.putLong("ReloadStartTime", currentTime);
         } else if (!serverReloading && currentReloading) {
            nbtCompound.remove("scguns:IsReloading");
            this.cleanupReloadState(nbtCompound);
            nbtCompound.remove("InCriticalReloadPhase");
            nbtCompound.remove("ReloadStartTime");
            nbtCompound.remove("ReloadAnimationStarted");
            nbtCompound.remove("ReloadAnimationRestarted");
            nbtCompound.remove("ReloadCompleted");
         }
      } else {
         boolean isInTransitionState = currentReloadState.equals("STOPPING") || nbtCompound.getBoolean("scguns:IsPlayingReloadStop");
         boolean playerTryingToAim = false;
         if (entity instanceof Player) {
            playerTryingToAim = KeyBinds.getAimMapping().isDown();
         }

         if (serverReloading && !currentReloading && !isInTransitionState) {
            nbtCompound.putBoolean("scguns:IsReloading", true);
            if (!nbtCompound.contains("scguns:ReloadState")) {
               nbtCompound.putString("scguns:ReloadState", AnimatedGunItem.ReloadState.NONE.name());
            }
         } else if (!serverReloading && currentReloading && !isInTransitionState) {
            nbtCompound.putString("scguns:ReloadState", AnimatedGunItem.ReloadState.STOPPING.name());
            nbtCompound.putBoolean("scguns:IsPlayingReloadStop", true);
         } else if (!serverReloading && isInTransitionState && !playerTryingToAim) {
            long lastCleanup = nbtCompound.getLong("LastCleanupTime");
            if (currentTime - lastCleanup > 50L) {
               nbtCompound.putLong("LastCleanupTime", currentTime);
               this.finishReloadTransition(nbtCompound);
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void handleAnimationControllerUpdates(ItemStack stack, Entity entity, CompoundTag nbtCompound) {
      long id = GeoItem.getId(stack);
      AnimationController<GeoAnimatable> animationController = (AnimationController<GeoAnimatable>)this.getAnimatableInstanceCache()
         .getManagerForId(id)
         .getAnimationControllers()
         .get("controller");
      this.rotationHandler.updateRotations(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
      if (nbtCompound.getBoolean("AttachmentChanged")) {
         if (animationController != null) {
            this.updateCarbineState(stack, animationController);
         }

         nbtCompound.remove("AttachmentChanged");
      }

      boolean inCriticalPhase = nbtCompound.getBoolean("InCriticalReloadPhase");
      boolean isReloading = nbtCompound.getBoolean("scguns:IsReloading");
      if (animationController != null
         && !isReloading
         && !inCriticalPhase
         && !this.isAnimationPlaying(animationController, "draw")
         && !this.isAnimationPlaying(animationController, "carbine_draw")
         && !this.isAnimationPlaying(animationController, "shoot")
         && !this.isAnimationPlaying(animationController, "shoot1")
         && !this.isAnimationPlaying(animationController, "carbine_shoot")
         && !this.isAnimationPlaying(animationController, "inspect")
         && !this.isAnimationPlaying(animationController, "carbine_inspect")
         && !this.isAnimationPlaying(animationController, "reload")
         && !this.isAnimationPlaying(animationController, "carbine_reload")
         && !this.isAnimationPlaying(animationController, "reload_start")
         && !this.isAnimationPlaying(animationController, "carbine_reload_start")
         && !this.isAnimationPlaying(animationController, "reload_loop")
         && !this.isAnimationPlaying(animationController, "carbine_reload_loop")
         && !this.isAnimationPlaying(animationController, "reload_stop")
         && !this.isAnimationPlaying(animationController, "carbine_reload_stop")) {
         this.updateCarbineState(stack, animationController);
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void handlePlayerSpecificLogic(ItemStack stack, Player player, CompoundTag nbtCompound, boolean inHands, boolean justLeftHands) {
      long id = GeoItem.getId(stack);
      AnimationController<GeoAnimatable> animationController = (AnimationController<GeoAnimatable>)this.getAnimatableInstanceCache()
         .getManagerForId(id)
         .getAnimationControllers()
         .get("controller");
      // GeoItem ids cannot answer "is this the gun in my hand?": a stack without an id component
      // reports the same fallback id as any other such stack, so this test used to be false for
      // every single gun and the held-gun logic ran for the whole inventory. The slot the stack was
      // ticked from is the reliable answer (HANDOFF section 69).
      if (!inHands) {
         this.handleItemNotHeld(nbtCompound, animationController, stack, player, justLeftHands);
      } else {
         this.handleDrawingState(nbtCompound);
         this.handleActionStates(nbtCompound, animationController, stack, player);
         this.handlePlayerStateUpdates(nbtCompound, player, stack);
         this.handleInitializationAndAnimations(nbtCompound, animationController, stack, player);
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void handleItemNotHeld(CompoundTag nbtCompound, AnimationController<GeoAnimatable> animationController, ItemStack stack, Player player, boolean justLeftHands) {
      if (nbtCompound.getBoolean("IsDrawn")) {
         nbtCompound.remove("IsDrawn");
         nbtCompound.remove("DrawnTick");
         this.drawTick = 0;
         this.cleanupAllReloadTags(nbtCompound);
         ModSyncedDataKeys.RELOADING.setValue(player, false);
         nbtCompound.remove("IsShooting");
         nbtCompound.remove("IsInspecting");
         nbtCompound.remove("IsAiming");
         nbtCompound.remove("IsRunning");
         nbtCompound.remove("loaded");
         nbtCompound.remove("DrawnTick");
      }

      // Only touch the controller on the tick the gun actually leaves the hands. 0.5.5 reset it on
      // every tick, which was harmless while every stack had its own id, but a stack that has no id
      // yet shares its AnimationController with the gun of the same type still in hand - resetting it
      // for a stored gun would then cancel the animation the held gun is playing.
      if (!justLeftHands) {
         return;
      }

      animationController.setAnimationSpeed(1.0);
      animationController.forceAnimationReset();
      if (this.isInCarbineMode(stack)) {
         animationController.tryTriggerAnimation("carbine_idle");
      } else {
         animationController.tryTriggerAnimation("idle");
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void handleDrawingState(CompoundTag nbtCompound) {
      this.updateBooleanTag(nbtCompound, "IsDrawing", nbtCompound.getBoolean("IsDrawn"));
      if (nbtCompound.getBoolean("IsDrawing") && nbtCompound.getInt("DrawnTick") < 15) {
         this.drawTick++;
         nbtCompound.putInt("DrawnTick", this.drawTick);
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void handleActionStates(CompoundTag nbtCompound, AnimationController<GeoAnimatable> animationController, ItemStack stack, Player player) {
      boolean isMeleeActive = (Boolean)ModSyncedDataKeys.MELEE.getValue(player);
      if (isMeleeActive) {
         Gun modifiedGun = ((GunItem)stack.getItem()).getModifiedGun(stack);
         if (modifiedGun.getGeneral().usesCustomMeleeAnimation()) {
            this.handleMeleeState(nbtCompound, animationController, stack, player);
            return;
         }
      }

      if (nbtCompound.getBoolean("IsShooting")) {
         this.handleShootState(nbtCompound, animationController, stack);
      }

      if (nbtCompound.getBoolean("IsInspecting")) {
         this.handleInspectState(animationController, stack);
      }

      if (MeleeAttackHandler.isBanzaiActive()
         && (this.isAnimationPlaying(animationController, "inspect") || this.isAnimationPlaying(animationController, "carbine_inspect"))) {
         animationController.setAnimationSpeed(1.0);
         if (this.isInCarbineMode(stack)) {
            animationController.tryTriggerAnimation("carbine_idle");
         } else {
            animationController.tryTriggerAnimation("idle");
         }
      }

      if (player.isSprinting() && (this.isAnimationPlaying(animationController, "inspect") || this.isAnimationPlaying(animationController, "carbine_inspect"))) {
         animationController.setAnimationSpeed(1.0);
         if (this.isInCarbineMode(stack)) {
            animationController.tryTriggerAnimation("carbine_idle");
         } else {
            animationController.tryTriggerAnimation("idle");
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void handlePlayerStateUpdates(CompoundTag nbtCompound, Player player, ItemStack stack) {
      if (!nbtCompound.getBoolean("IsDrawn")) {
         nbtCompound.putBoolean("IsDrawn", true);
      }

      boolean isSprinting = player.isSprinting();
      boolean isAiming = (Boolean)ModSyncedDataKeys.AIMING.getValue(player);
      boolean serverReloading = (Boolean)ModSyncedDataKeys.RELOADING.getValue(player);
      boolean clientReloading = nbtCompound.getBoolean("scguns:IsReloading");
      if (!serverReloading && !clientReloading) {
         if (isAiming && nbtCompound.getBoolean("IsDrawing") && nbtCompound.getInt("DrawnTick") < 15) {
            ModSyncedDataKeys.AIMING.setValue(player, false);
            isAiming = false;
         }

         this.updateBooleanTag(nbtCompound, "IsAiming", isAiming);
         this.updateBooleanTag(nbtCompound, "IsRunning", isSprinting);
      } else {
         if (isAiming) {
            ModSyncedDataKeys.AIMING.setValue(player, false);
         }

         this.updateBooleanTag(nbtCompound, "IsAiming", false);
         this.updateBooleanTag(nbtCompound, "IsRunning", isSprinting);
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void handleInitializationAndAnimations(
      CompoundTag nbtCompound, AnimationController<GeoAnimatable> animationController, ItemStack stack, Player player
   ) {
      boolean wasDrawn = nbtCompound.getBoolean("IsDrawn");
      boolean isFirstTick = !wasDrawn && !nbtCompound.getBoolean("_InitializedThisSession");
      if (isFirstTick) {
         this.handleFirstTickInitialization(nbtCompound, animationController, stack, player);
      } else {
         boolean isMeleeActive = (Boolean)ModSyncedDataKeys.MELEE.getValue(player);
         if (isMeleeActive) {
            Gun modifiedGun = ((GunItem)stack.getItem()).getModifiedGun(stack);
            if (modifiedGun.getGeneral().usesCustomMeleeAnimation()) {
               return;
            }
         }

         this.handleAnimationStateFixes(nbtCompound, animationController, stack, player);
         this.handleMainAnimationLogic(nbtCompound, animationController, stack, player);
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void handleMeleeState(CompoundTag nbt, AnimationController<GeoAnimatable> animationController, ItemStack stack, Player player) {
      Gun modifiedGun = ((GunItem)stack.getItem()).getModifiedGun(stack);
      boolean isCarbine = this.isInCarbineMode(stack);
      boolean hasBayonet = ((GunItem)stack.getItem()).hasBayonet(stack);
      String meleeAnimToPlay;
      if (hasBayonet) {
         meleeAnimToPlay = isCarbine ? "carbine_bayonet" : "bayonet";
      } else {
         meleeAnimToPlay = isCarbine ? "carbine_melee" : "melee";
      }

      if (!this.isAnimationPlaying(animationController, meleeAnimToPlay)) {
         animationController.setAnimationSpeed(1.0);
         animationController.forceAnimationReset();
         animationController.tryTriggerAnimation(meleeAnimToPlay);
         nbt.putLong("CustomMeleeStartTime", System.currentTimeMillis());
      }

      long startTime = nbt.getLong("CustomMeleeStartTime");
      if (startTime > 0L && (float)(System.currentTimeMillis() - startTime) >= 400.0F) {
         nbt.remove("CustomMeleeStartTime");
         ModSyncedDataKeys.MELEE.setValue(player, false);
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void handleFirstTickInitialization(CompoundTag nbtCompound, AnimationController<GeoAnimatable> animationController, ItemStack stack, Player player) {
      ModSyncedDataKeys.RELOADING.getValue(player);
      nbtCompound.putBoolean("_InitializedThisSession", true);
      if (!nbtCompound.getBoolean("IsDrawn")) {
         nbtCompound.putBoolean("IsDrawn", true);
         nbtCompound.putInt("DrawnTick", 0);
         this.drawTick = 0;
      }

      if (animationController != null) {
         boolean hasNoAnimation = animationController.getCurrentAnimation() == null;
         boolean isStopped = animationController.getAnimationState() == State.STOPPED;
         boolean isCurrentlyReloading = nbtCompound.getBoolean("scguns:IsReloading") || (Boolean)ModSyncedDataKeys.RELOADING.getValue(player);
         if ((hasNoAnimation || isStopped) && !isCurrentlyReloading && nbtCompound.getInt("DrawnTick") >= 15) {
            animationController.setAnimationSpeed(1.0);
            this.updateCarbineState(stack, animationController);
            if (this.isInCarbineMode(stack)) {
               animationController.tryTriggerAnimation("carbine_idle");
            } else {
               animationController.tryTriggerAnimation("idle");
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void handleAnimationStateFixes(CompoundTag nbtCompound, AnimationController<GeoAnimatable> animationController, ItemStack stack, Player player) {
      if (animationController != null) {
         boolean hasNoAnimation = animationController.getCurrentAnimation() == null;
         boolean isStopped = animationController.getAnimationState() == State.STOPPED;
         boolean isMeleeActive = (Boolean)ModSyncedDataKeys.MELEE.getValue(player);
         if (isMeleeActive) {
            Gun modifiedGun = ((GunItem)stack.getItem()).getModifiedGun(stack);
            if (modifiedGun.getGeneral().usesCustomMeleeAnimation()) {
               return;
            }
         }

         if ((hasNoAnimation || isStopped) && !nbtCompound.getBoolean("scguns:IsReloading") && nbtCompound.getInt("DrawnTick") >= 15) {
            animationController.setAnimationSpeed(1.0);
            this.updateCarbineState(stack, animationController);
            if (this.isInCarbineMode(stack)) {
               animationController.tryTriggerAnimation("carbine_idle");
            } else {
               animationController.tryTriggerAnimation("idle");
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void handleMainAnimationLogic(CompoundTag nbtCompound, AnimationController<GeoAnimatable> animationController, ItemStack stack, Player player) {
      if (nbtCompound.getBoolean("IsDrawing") && nbtCompound.getInt("DrawnTick") < 15 && (Boolean)Config.COMMON.gameplay.drawAnimation.get()) {
         this.handleDrawingState(nbtCompound, animationController, stack);
      } else {
         if (nbtCompound.getInt("DrawnTick") >= 15) {
            assert animationController != null;

            boolean isReloading = nbtCompound.getBoolean("scguns:IsReloading") || (Boolean)ModSyncedDataKeys.RELOADING.getValue(player);
            boolean inCriticalPhase = nbtCompound.getBoolean("InCriticalReloadPhase");
            if (isReloading) {
               this.handleReloadingState(nbtCompound, animationController, stack);
               return;
            }

            if (inCriticalPhase) {
               return;
            }

            if (this.isPlayingCriticalAnimations(animationController)) {
               if (nbtCompound.contains("scguns:ReloadState")
                  && nbtCompound.getString("scguns:ReloadState").equals(AnimatedGunItem.ReloadState.STOPPING.name())) {
                  this.handleReloadingState(nbtCompound, animationController, stack);
               } else if (nbtCompound.getBoolean("IsAiming")) {
                  this.handleAimingState(nbtCompound, animationController);
               } else if (nbtCompound.getBoolean("IsRunning") && this.isPlayingInspectOrReloadAnimations(animationController)) {
                  this.handleRunningState(animationController);
               } else if (this.isPlayingInspectOrReloadAnimations(animationController)) {
                  animationController.setAnimationSpeed(1.0);
                  if (this.isInCarbineMode(stack)) {
                     animationController.tryTriggerAnimation("carbine_idle");
                  } else {
                     animationController.tryTriggerAnimation("idle");
                  }
               }
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   private boolean isPlayingCriticalAnimations(AnimationController<GeoAnimatable> animationController) {
      return !this.isAnimationPlaying(animationController, "draw")
         && !this.isAnimationPlaying(animationController, "carbine_draw")
         && !this.isAnimationPlaying(animationController, "jam")
         && !this.isAnimationPlaying(animationController, "melee")
         && !this.isAnimationPlaying(animationController, "carbine_melee")
         && !this.isAnimationPlaying(animationController, "bayonet")
         && !this.isAnimationPlaying(animationController, "carbine_bayonet")
         && !this.isAnimationPlaying(animationController, "shoot")
         && !this.isAnimationPlaying(animationController, "shoot1")
         && !this.isAnimationPlaying(animationController, "carbine_shoot")
         && !this.isAnimationPlaying(animationController, "aim_shoot")
         && !this.isAnimationPlaying(animationController, "aim_shoot1")
         && !this.isAnimationPlaying(animationController, "carbine_aim_shoot")
         && !this.isAnimationPlaying(animationController, "inspect")
         && !this.isAnimationPlaying(animationController, "carbine_inspect")
         && !this.isAnimationPlaying(animationController, "reload_stop")
         && !this.isAnimationPlaying(animationController, "carbine_reload_stop");
   }

   @OnlyIn(Dist.CLIENT)
   private boolean isPlayingInspectOrReloadAnimations(AnimationController<GeoAnimatable> animationController) {
      return !this.isAnimationPlaying(animationController, "inspect")
         && !this.isAnimationPlaying(animationController, "carbine_inspect")
         && !this.isAnimationPlaying(animationController, "reload")
         && !this.isAnimationPlaying(animationController, "carbine_reload")
         && !this.isAnimationPlaying(animationController, "reload_alt")
         && !this.isAnimationPlaying(animationController, "carbine_reload_loop")
         && !this.isAnimationPlaying(animationController, "reload_loop");
   }

   @OnlyIn(Dist.CLIENT)
   private void finishReloadTransition(CompoundTag nbt) {
      nbt.remove("scguns:IsReloading");
      nbt.remove("scguns:IsPlayingReloadStop");
      nbt.remove("scguns:ReloadComplete");
      nbt.remove("IsManualReload");
      nbt.remove("InReloadLoop");
      nbt.remove("PendingStopTransition");
      nbt.remove("PendingStopTime");
      nbt.remove("LastReloadStateChange");
      nbt.remove("ManualReloadInitialized");
      nbt.remove("InCriticalReloadPhase");
      String currentState = nbt.getString("scguns:ReloadState");
      if (currentState.equals("STOPPING")) {
         this.cleanupReloadState(nbt);
      }
   }

   private void cleanupAllReloadTags(CompoundTag nbt) {
      this.cleanupReloadState(nbt);
      nbt.remove("IsMagReload");
      nbt.remove("IsManualReload");
      nbt.remove("shouldStopOnLoopEnd");
      nbt.remove("shouldTransitionToStop");
   }

   public boolean isAnimationPlaying(AnimationController<GeoAnimatable> animationController, String animationName) {
      return animationController.getCurrentAnimation() != null && animationController.getCurrentAnimation().animation().name().equals(animationName);
   }

   private void updateBooleanTag(CompoundTag nbt, String key, boolean value) {
      if (value) {
         nbt.putBoolean(key, true);
      } else {
         nbt.remove(key);
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void handleDrawingState(CompoundTag nbt, AnimationController<GeoAnimatable> animationController, ItemStack stack) {
      double drawSpeedMultiplier = 1.0;
      int quickHandsLevel = GunEnchantmentHelper.getQuickHands(stack);
      if (quickHandsLevel > 0) {
         drawSpeedMultiplier += 0.12 * (double)quickHandsLevel;
      }

      int lightweightLevel = GunEnchantmentHelper.getLightweight(stack);
      if (lightweightLevel > 0) {
         drawSpeedMultiplier += 0.05 * (double)lightweightLevel;
      }

      drawSpeedMultiplier = GunModifierHelper.getModifiedDrawSpeed(stack, drawSpeedMultiplier);
      animationController.setAnimationSpeed(drawSpeedMultiplier);
      if (nbt.getInt("DrawnTick") < 15 && !nbt.getBoolean("scguns:IsReloading") && this.isPlayingCriticalAnimations(animationController)) {
         if (this.isInCarbineMode(stack)) {
            animationController.tryTriggerAnimation("carbine_draw");
         } else {
            animationController.tryTriggerAnimation("draw");
         }
      }

      nbt.remove("IsShooting");
      nbt.remove("IsInspecting");
   }

   @OnlyIn(Dist.CLIENT)
   private void handleNormalReload(CompoundTag nbt, AnimationController<GeoAnimatable> animationController, ItemStack stack) {
      Gun modifiedGun = ((GunItem)stack.getItem()).getModifiedGun(stack);
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         boolean serverReloading = (Boolean)ModSyncedDataKeys.RELOADING.getValue(player);
         if (animationController.getCurrentAnimation() != null) {
            animationController.getCurrentAnimation().animation().name();
         } else {
            String var10000 = "none";
         }

         State animState = animationController.getAnimationState();
         if (!serverReloading) {
            animationController.setAnimationSpeed(1.0);
            this.cleanupReloadState(nbt);
            nbt.remove("InCriticalReloadPhase");
            nbt.remove("ReloadAnimationStarted");
            nbt.remove("ReloadAnimationRestarted");
            nbt.remove("ReloadCompleted");
         } else {
            boolean isCarbine = this.isInCarbineMode(stack);
            String reloadAnim = isCarbine ? "carbine_reload" : "reload";
            if (nbt.getBoolean("ReloadCompleted")) {
               animationController.setAnimationSpeed(1.0);
            } else {
               double reloadSpeedMultiplier = 1.0;
               AttributeInstance reloadSpeedAttribute = player.getAttribute(SCAttributes.RELOAD_SPEED);
               if (reloadSpeedAttribute != null) {
                  reloadSpeedMultiplier = reloadSpeedAttribute.getValue();
               }

               int actualReloadTime = (int)Math.ceil((double)GunEnchantmentHelper.getRealReloadSpeed(stack) / reloadSpeedMultiplier);
               float speedMultiplier = (float)modifiedGun.getReloads().getReloadTimer() / (float)actualReloadTime;
               boolean hasStartedReload = nbt.getBoolean("ReloadAnimationStarted");
               if (!this.isAnimationPlaying(animationController, reloadAnim)
                  && !hasStartedReload
                  && !this.isAnimationPlaying(animationController, "draw")
                  && !this.isAnimationPlaying(animationController, "carbine_draw")) {
                  animationController.setAnimationSpeed((double)speedMultiplier);
                  animationController.forceAnimationReset();
                  animationController.tryTriggerAnimation(reloadAnim);
                  nbt.putBoolean("ReloadAnimationStarted", true);
               } else {
                  if (this.isAnimationPlaying(animationController, reloadAnim)) {
                     animationController.setAnimationSpeed((double)speedMultiplier);
                  }

                  if (!this.isAnimationPlaying(animationController, reloadAnim)
                     && hasStartedReload
                     && animState == State.STOPPED
                     && !nbt.getBoolean("ReloadAnimationRestarted")) {
                     animationController.setAnimationSpeed((double)speedMultiplier);
                     animationController.forceAnimationReset();
                     animationController.tryTriggerAnimation(reloadAnim);
                     nbt.putBoolean("ReloadAnimationRestarted", true);
                  } else {
                     if (animationController.getAnimationState() == State.STOPPED
                        && hasStartedReload
                        && !this.isAnimationPlaying(animationController, reloadAnim)
                        && !this.isAnimationPlaying(animationController, "draw")
                        && !this.isAnimationPlaying(animationController, "carbine_draw")) {
                        animationController.setAnimationSpeed(1.0);
                        if (modifiedGun.getReloads().getReloadType() == ReloadType.MAG_FED) {
                           PacketHandler.getPlayChannel().sendToServer(new C2SMessageGunLoaded());
                        } else if (modifiedGun.getReloads().getReloadType() == ReloadType.SINGLE_ITEM) {
                           PacketHandler.getPlayChannel().sendToServer(new C2SMessageReload(false));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void handleReloadingState(CompoundTag nbt, AnimationController<GeoAnimatable> animationController, ItemStack stack) {
      Gun modifiedGun = ((GunItem)stack.getItem()).getModifiedGun(stack);
      String currentState = nbt.getString("scguns:ReloadState");
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         if ((Boolean)ModSyncedDataKeys.AIMING.getValue(player)) {
            ModSyncedDataKeys.AIMING.setValue(player, false);
            AimingHandler.get().aiming = false;
         }

         boolean serverReloading = (Boolean)ModSyncedDataKeys.RELOADING.getValue(player);
         boolean clientReloading = nbt.getBoolean("scguns:IsReloading");
         if (modifiedGun.getReloads().getReloadType() != ReloadType.MANUAL) {
            this.handleNormalReload(nbt, animationController, stack);
         } else if (!serverReloading && clientReloading && !currentState.equals("STOPPING")) {
            nbt.putString("scguns:ReloadState", AnimatedGunItem.ReloadState.STOPPING.name());
            nbt.putBoolean("scguns:IsPlayingReloadStop", true);
            nbt.remove("scguns:IsReloading");
            animationController.stop();
            animationController.setAnimationSpeed(1.0);
            animationController.tryTriggerAnimation(this.isInCarbineMode(stack) ? "carbine_reload_stop" : "reload_stop");
         } else {
            long currentTime = System.currentTimeMillis();
            long lastStateChange = nbt.getLong("LastReloadStateChange");
            if (currentTime - lastStateChange >= 0L) {
               double reloadSpeedMultiplier = 1.0;
               if (Minecraft.getInstance().player != null) {
                  AttributeInstance reloadSpeedAttribute = Minecraft.getInstance().player.getAttribute(SCAttributes.RELOAD_SPEED);
                  if (reloadSpeedAttribute != null) {
                     reloadSpeedMultiplier = reloadSpeedAttribute.getValue();
                  }
               }

               int actualReloadTime = (int)Math.ceil((double)GunEnchantmentHelper.getRealReloadSpeed(stack) / reloadSpeedMultiplier);
               float speedMultiplier = (float)modifiedGun.getReloads().getReloadTimer() / (float)actualReloadTime;
               CompoundTag tag = NbtHelper.getOrCreateTag(stack);
               int currentAmmo = tag.getInt("AmmoCount");
               int maxAmmo = GunModifierHelper.getModifiedAmmoCapacity(stack, modifiedGun);
               boolean ammoFull = currentAmmo >= maxAmmo;
               boolean hasNoAmmo = Gun.findAmmo(player, modifiedGun.getProjectile().getItem()).stack().isEmpty();
               if (currentState.isEmpty() || currentState.equals("NONE")) {
                  currentState = AnimatedGunItem.ReloadState.NONE.name();
                  nbt.putString("scguns:ReloadState", currentState);
               }

               AnimatedGunItem.ReloadState state;
               try {
                  state = AnimatedGunItem.ReloadState.valueOf(currentState);
               } catch (IllegalArgumentException var28) {
                  state = AnimatedGunItem.ReloadState.NONE;
                  nbt.putString("scguns:ReloadState", AnimatedGunItem.ReloadState.NONE.name());
               }

               switch (state) {
                  case NONE:
                     nbt.putString("scguns:ReloadState", AnimatedGunItem.ReloadState.STARTING.name());
                     nbt.putLong("LastReloadStateChange", currentTime);
                     nbt.putBoolean("ManualReloadInitialized", true);
                     animationController.setAnimationSpeed((double)speedMultiplier);
                     animationController.tryTriggerAnimation(this.isInCarbineMode(stack) ? "carbine_reload_start" : "reload_start");
                     break;
                  case STARTING:
                     boolean isStartAnimPlaying = this.isAnimationPlaying(animationController, "reload_start")
                        || this.isAnimationPlaying(animationController, "carbine_reload_start");
                     if (!isStartAnimPlaying && animationController.getAnimationState() == State.STOPPED) {
                        if (!ammoFull && !hasNoAmmo && (Boolean)ModSyncedDataKeys.RELOADING.getValue(player)) {
                           nbt.putString("scguns:ReloadState", AnimatedGunItem.ReloadState.LOADING.name());
                           nbt.putLong("LastReloadStateChange", currentTime);
                           nbt.putBoolean("InReloadLoop", true);
                           animationController.setAnimationSpeed((double)speedMultiplier);
                           animationController.tryTriggerAnimation(this.isInCarbineMode(stack) ? "carbine_reload_loop" : "reload_loop");
                        } else {
                           nbt.putString("scguns:ReloadState", AnimatedGunItem.ReloadState.STOPPING.name());
                           nbt.putBoolean("scguns:IsPlayingReloadStop", true);
                           animationController.setAnimationSpeed(1.0);
                           animationController.tryTriggerAnimation(this.isInCarbineMode(stack) ? "carbine_reload_stop" : "reload_stop");
                        }
                     }
                     break;
                  case LOADING:
                     boolean isLoopPlaying = this.isAnimationPlaying(animationController, "reload_loop")
                        || this.isAnimationPlaying(animationController, "carbine_reload_loop");
                     boolean shouldStopAfterLoop = ammoFull || hasNoAmmo || !(Boolean)ModSyncedDataKeys.RELOADING.getValue(player);
                     if (shouldStopAfterLoop) {
                        if (!nbt.getBoolean("PendingStopTransition")) {
                           nbt.putBoolean("PendingStopTransition", true);
                           nbt.putLong("PendingStopTime", System.currentTimeMillis());
                        }

                        if (!isLoopPlaying && animationController.getAnimationState() == State.STOPPED) {
                           long pendingTime = System.currentTimeMillis() - nbt.getLong("PendingStopTime");
                           if (pendingTime > 100L) {
                              nbt.putString("scguns:ReloadState", AnimatedGunItem.ReloadState.STOPPING.name());
                              nbt.putBoolean("scguns:IsPlayingReloadStop", true);
                              nbt.remove("InReloadLoop");
                              nbt.remove("PendingStopTransition");
                              nbt.remove("PendingStopTime");
                              animationController.setAnimationSpeed(1.0);
                              animationController.tryTriggerAnimation(this.isInCarbineMode(stack) ? "carbine_reload_stop" : "reload_stop");
                           }
                        }
                     } else {
                        nbt.remove("PendingStopTransition");
                        nbt.remove("PendingStopTime");
                        if (!isLoopPlaying && animationController.getAnimationState() == State.STOPPED) {
                           animationController.setAnimationSpeed((double)speedMultiplier);
                           animationController.tryTriggerAnimation(this.isInCarbineMode(stack) ? "carbine_reload_loop" : "reload_loop");
                        }
                     }
                     break;
                  case STOPPING:
                     if (!this.isAnimationPlaying(animationController, "reload_stop")
                        && !this.isAnimationPlaying(animationController, "carbine_reload_stop")
                        && animationController.getAnimationState() == State.STOPPED) {
                        animationController.setAnimationSpeed(1.0);
                        this.cleanupReloadState(nbt);
                        nbt.remove("LastReloadStateChange");
                        nbt.remove("ManualReloadInitialized");
                        nbt.remove("InReloadLoop");
                        nbt.remove("PendingStopTransition");
                        if (this.isInCarbineMode(stack)) {
                           animationController.tryTriggerAnimation("carbine_idle");
                        } else {
                           animationController.tryTriggerAnimation("idle");
                        }
                     }
               }
            }
         }
      }
   }

   /**
    * Clears reload state left on a gun that is not in the selected slot (HANDOFF section 68).
    *
    * <p>Kept in its own method on purpose: fetching the tag, mutating it and writing it back must not
    * happen in the middle of a method that keeps using an earlier tag local - writing the component
    * back invalidates that local, and the later writes to it would go nowhere
    * ({@code audit_nbt_write_alias.py} flags exactly that).</p>
    */
   private void clearStaleReloadState(ItemStack stack) {
      CompoundTag tag = NbtHelper.getTagForWrite(stack);
      if (tag == null || !hasReloadState(tag)) {
         return;
      }

      this.cleanupReloadState(tag);
      tag.remove("IsReloading");
      tag.remove("scguns:IsReloading");
      tag.remove("InCriticalReloadPhase");
      tag.remove("scguns:ShouldStopAfterLoop");
      tag.remove("scguns:StopAfterLoopTime");
      tag.remove("scguns:IsPlayingReloadStop");
      tag.remove("InReloadLoop");
      NbtHelper.setTag(stack, tag);
   }

   /** Whether the tag carries any reload state at all - used by the not-held clean-up. */
   private static boolean hasReloadState(CompoundTag nbt) {
      return nbt.getBoolean("IsReloading")
         || nbt.getBoolean("scguns:IsReloading")
         || nbt.getBoolean("InCriticalReloadPhase")
         || nbt.getBoolean("scguns:ShouldStopAfterLoop")
         || nbt.getBoolean("scguns:IsPlayingReloadStop")
         || nbt.getBoolean("InReloadLoop")
         || nbt.getLong("scguns:StopAfterLoopTime") != 0L
         || !nbt.getString("scguns:ReloadState").isEmpty();
   }

   public void cleanupReloadState(CompoundTag nbt) {
      nbt.remove("scguns:ReloadState");
      nbt.remove("reloadStartTime");
      nbt.remove("scguns:ReloadComplete");
      nbt.remove("scguns:IsPlayingReloadStop");
      nbt.remove("scguns:IsMagReload");
      nbt.remove("scguns:IsManualReload");
      nbt.remove("loaded");
      nbt.remove("IsReloading");
      nbt.remove("scguns:IsReloading");
      nbt.remove("scguns:PausedDuringReload");
      nbt.remove("LastReloadStateChange");
      nbt.remove("InCriticalReloadPhase");
      nbt.remove("ReloadAnimStartTime");
      nbt.remove("IsMagReload");
      nbt.remove("IsManualReload");
      nbt.remove("ReloadAnimationStarted");
      nbt.remove("ReloadAnimationRestarted");
      nbt.remove("ReloadCompleted");
   }

   @OnlyIn(Dist.CLIENT)
   private void handleAimingState(CompoundTag nbt, AnimationController<GeoAnimatable> animationController) {
      if (!nbt.getBoolean("IsDrawing") || nbt.getInt("DrawnTick") >= 15) {
         animationController.setAnimationSpeed(1.0);
         nbt.remove("IsInspecting");

         assert Minecraft.getInstance().player != null;

         ItemStack stack = Minecraft.getInstance().player.getMainHandItem();
         this.isInCarbineMode(stack);
         if (stack.getItem() instanceof AnimatedGunItem && this.isInCarbineMode(stack)) {
            animationController.tryTriggerAnimation("carbine_idle");
         } else {
            animationController.tryTriggerAnimation("idle");
         }
      }
   }

   private void handleRunningState(AnimationController<GeoAnimatable> animationController) {
      animationController.setAnimationSpeed(1.0);

      assert Minecraft.getInstance().player != null;

      ItemStack stack = Minecraft.getInstance().player.getMainHandItem();
      boolean isCarbine = stack.getItem() instanceof AnimatedGunItem && this.isInCarbineMode(stack);
      if (this.isAnimationPlaying(animationController, isCarbine ? "carbine_inspect" : "inspect")) {
         animationController.tryTriggerAnimation(isCarbine ? "carbine_idle" : "idle");
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void handleInspectState(AnimationController<GeoAnimatable> animationController, ItemStack stack) {
      boolean isCarbine = this.isInCarbineMode(stack);
      String animToPlay = isCarbine ? "carbine_inspect" : "inspect";
      if (!this.isAnimationPlaying(animationController, animToPlay)) {
         animationController.setAnimationSpeed(1.0);
         animationController.forceAnimationReset();
         animationController.tryTriggerAnimation(animToPlay);
      }
   }

   private void handleShootState(CompoundTag nbt, AnimationController<GeoAnimatable> animationController, ItemStack stack) {
      boolean isCarbine = this.isInCarbineMode(stack);
      animationController.setAnimationSpeed(1.0);
      if (!(stack.getItem() instanceof AnimatedDualWieldGunItem)) {
         if (nbt.getBoolean("IsAiming")) {
            animationController.tryTriggerAnimation(isCarbine ? "carbine_aim_shoot" : "aim_shoot");
         } else {
            animationController.tryTriggerAnimation(isCarbine ? "carbine_shoot" : "shoot");
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void soundListener(SoundKeyframeEvent<AnimatedGunItem> gunItemSoundKeyframeEvent) {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         ItemStack mainHand = player.getMainHandItem();
         ItemStack offHand = player.getOffhandItem();
         boolean isMainHand = mainHand.getItem() == this && !NbtHelper.getOrCreateTag(mainHand).getBoolean("IsDroppedItem");
         boolean isOffHand = offHand.getItem() == this && !NbtHelper.getOrCreateTag(offHand).getBoolean("IsDroppedItem");
         if (isMainHand || isOffHand) {
            ItemStack heldStack = isMainHand ? mainHand : offHand;
            CompoundTag heldTag = NbtHelper.getOrCreateTag(heldStack);
            if (heldTag.getBoolean("IsDrawn")) {
               String var9 = gunItemSoundKeyframeEvent.getKeyframeData().getSound();
               switch (var9) {
                  case "gun_rustle":
                     player.playSound((SoundEvent)ModSounds.GUN_RUSTLE.get(), 1.0F, 1.0F);
                     break;
                  case "metal":
                     player.playSound((SoundEvent)ModSounds.METAL.get(), 1.0F, 1.0F);
                     break;
                  case "pump":
                     player.playSound((SoundEvent)ModSounds.PUMP.get(), 1.0F, 1.0F);
                     break;
                  case "pump_half":
                     player.playSound((SoundEvent)ModSounds.PUMP_HALF.get(), 1.0F, 1.0F);
                     break;
                  case "insert":
                     player.playSound((SoundEvent)ModSounds.INSERT.get(), 1.0F, 1.0F);
                     break;
                  case "jam":
                     player.playSound((SoundEvent)ModSounds.COPPER_GUN_JAM.get(), 0.2F, 1.0F);
                     break;
                  case "lever":
                     player.playSound((SoundEvent)ModSounds.LEVER.get(), 1.0F, 1.0F);
                     break;
                  case "slap":
                     player.playSound((SoundEvent)ModSounds.SLAP.get(), 1.0F, 1.0F);
                     break;
                  case "rack":
                     player.playSound((SoundEvent)ModSounds.RACK.get(), 1.0F, 1.0F);
                     break;
                  case "reload_mag_out":
                     player.playSound(this.reloadSoundMagOut, 1.0F, 1.0F);
                     break;
                  case "reload_mag_in":
                     player.playSound(this.reloadSoundMagIn, 1.0F, 1.0F);
                     break;
                  case "reload_end":
                     player.playSound(this.reloadSoundEnd, 1.0F, 1.0F);
                     break;
                  case "bolt_pull":
                     player.playSound(this.boltPullSound, 1.0F, 1.0F);
                     break;
                  case "bolt_release":
                     player.playSound(this.boltReleaseSound, 1.0F, 1.0F);
                     break;
                  case "bolt":
                     player.playSound((SoundEvent)ModSounds.BOLT.get(), 1.0F, 1.0F);
               }
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void particleListener(ParticleKeyframeEvent<AnimatedGunItem> gunItemParticleKeyframeEvent) {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         ItemStack currentItem = player.getMainHandItem();
         ItemStack mainHand = player.getMainHandItem();
         ItemStack offHand = player.getOffhandItem();
         boolean isMainHand = mainHand.getItem() == this && !NbtHelper.getOrCreateTag(mainHand).getBoolean("IsDroppedItem");
         boolean isOffHand = offHand.getItem() == this && !NbtHelper.getOrCreateTag(offHand).getBoolean("IsDroppedItem");
         if (isMainHand || isOffHand) {
            ItemStack heldStack = isMainHand ? mainHand : offHand;
            CompoundTag heldTag = NbtHelper.getOrCreateTag(heldStack);
            if (heldTag.getBoolean("IsDrawn")) {
               GunItem gunItem = (GunItem)heldStack.getItem();
               Gun gun = gunItem.getModifiedGun(heldStack);
               CompoundTag tag = NbtHelper.getOrCreateTag(heldStack);
               String effect = gunItemParticleKeyframeEvent.getKeyframeData().getEffect();
               switch (effect) {
                  case "loaded":
                     // 1.21: the discarded getOrCreateTag() call that sat here re-detached the component and left the tag above stale.
                     ModSyncedDataKeys.RELOADING.getValue(player);
                     tag.putBoolean("loaded", true);
                     PacketHandler.getPlayChannel().sendToServer(new C2SMessageGunLoaded());
                     break;
                  case "eject_casing":
                     if (gun.getProjectile().ejectsCasing() && gun.getProjectile().ejectDuringReload() && (Boolean)Config.COMMON.gameplay.spawnCasings.get()) {
                        Level level = player.level();
                        GunEventBus.ejectCasing(level, player, false);
                        if (gun.getProjectile().casingType != null && !player.getAbilities().instabuild) {
                           PacketHandler.getPlayChannel().sendToServer(new C2SMessageEjectCasing());
                        }
                     }
                     break;
                  case "rotate_cylinder":
                     if (heldStack.getItem() instanceof AnimatedGunItem animatedGun) {
                        animatedGun.getRotationHandler().incrementCylinderRotation(90.0F);
                        tag.putBoolean("UseManualCylinderRotation", true);
                     }
               }
            }
         }
      }
   }

   public boolean isPerspectiveAware() {
      return true;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(new IClientItemExtensions() {
         private AnimatedGunRenderer renderer;

         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
               this.renderer = new AnimatedGunRenderer(ResourceLocation.fromNamespaceAndPath("scguns", AnimatedGunItem.this.gunID));
            }

            return this.renderer;
         }
      });
   }

   @OnlyIn(Dist.CLIENT)
   private PlayState predicate(AnimationState<AnimatedGunItem> event) {
      Player player = Minecraft.getInstance().player;
      if (player == null) {
         return PlayState.STOP;
      } else {
         ItemStack mainHand = player.getMainHandItem();
         ItemStack offHand = player.getOffhandItem();
         boolean isMainHand = mainHand.getItem() == this && !NbtHelper.getOrCreateTag(mainHand).getBoolean("IsDroppedItem");
         boolean isOffHand = offHand.getItem() == this && !NbtHelper.getOrCreateTag(offHand).getBoolean("IsDroppedItem");
         if (!isMainHand && !isOffHand) {
            return PlayState.STOP;
         } else {
            ItemStack heldStack = isMainHand ? mainHand : offHand;
            CompoundTag heldTag = NbtHelper.getOrCreateTag(heldStack);
            return !heldTag.getBoolean("IsDrawn") ? PlayState.STOP : PlayState.CONTINUE;
         }
      }
   }

   public void registerControllers(ControllerRegistrar controllers) {
      if (FMLEnvironment.dist == Dist.CLIENT) {
         this.registerClientControllers(controllers);
      }
   }

   @OnlyIn(Dist.CLIENT)
   private void registerClientControllers(ControllerRegistrar controllers) {
      AnimationController<AnimatedGunItem> controller = new AnimationController(this, "controller", 0, this::predicate)
         .setSoundKeyframeHandler(this::soundListener)
         .setParticleKeyframeHandler(this::particleListener)
         .triggerableAnim("idle", GunAnimations.IDLE)
         .triggerableAnim("carbine_idle", GunAnimations.CARBINE_IDLE)
         .triggerableAnim("shoot", GunAnimations.SHOOT)
         .triggerableAnim("shoot1", GunAnimations.SHOOT1)
         .triggerableAnim("carbine_shoot", GunAnimations.CARBINE_SHOOT)
         .triggerableAnim("aim_shoot", GunAnimations.AIM_SHOOT)
         .triggerableAnim("aim_shoot1", GunAnimations.AIM_SHOOT1)
         .triggerableAnim("carbine_aim_shoot", GunAnimations.CARBINE_AIM_SHOOT)
         .triggerableAnim("reload", GunAnimations.RELOAD)
         .triggerableAnim("carbine_reload", GunAnimations.CARBINE_RELOAD)
         .triggerableAnim("reload_alt", GunAnimations.RELOAD_ALT)
         .triggerableAnim("reload_start", GunAnimations.RELOAD_START)
         .triggerableAnim("carbine_reload_start", GunAnimations.CARBINE_RELOAD_START)
         .triggerableAnim("reload_loop", GunAnimations.RELOAD_LOOP)
         .triggerableAnim("carbine_reload_loop", GunAnimations.CARBINE_RELOAD_LOOP)
         .triggerableAnim("reload_stop", GunAnimations.RELOAD_STOP)
         .triggerableAnim("carbine_reload_stop", GunAnimations.CARBINE_RELOAD_STOP)
         .triggerableAnim("draw", GunAnimations.DRAW)
         .triggerableAnim("carbine_draw", GunAnimations.CARBINE_DRAW)
         .triggerableAnim("inspect", GunAnimations.INSPECT)
         .triggerableAnim("carbine_inspect", GunAnimations.CARBINE_INSPECT)
         .triggerableAnim("jam", GunAnimations.JAM)
         .triggerableAnim("melee", GunAnimations.MELEE)
         .triggerableAnim("carbine_melee", GunAnimations.CARBINE_MELEE)
         .triggerableAnim("bayonet", GunAnimations.BAYONET)
         .triggerableAnim("carbine_bayonet", GunAnimations.CARBINE_BAYONET);
      controllers.add(new AnimationController[]{controller});
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   private static enum ReloadState {
      NONE,
      STARTING,
      LOADING,
      STOPPING;

      private ReloadState() {
      }
   }
}
