package top.ribs.scguns.client.handler;


import top.ribs.scguns.util.NbtHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeFov;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import org.joml.Matrix4f;
import top.ribs.scguns.Config;
import top.ribs.scguns.client.GunModel;
import top.ribs.scguns.client.GunRenderType;
import top.ribs.scguns.client.SwayType;
import top.ribs.scguns.client.render.gun.IOverrideModel;
import top.ribs.scguns.client.render.gun.ModelOverrides;
import top.ribs.scguns.client.util.PropertyHelper;
import top.ribs.scguns.client.util.RenderUtil;
import top.ribs.scguns.common.GripType;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.common.ReloadType;
import top.ribs.scguns.common.properties.SightAnimation;
import top.ribs.scguns.event.GunFireEvent;
import top.ribs.scguns.init.ModItems;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.animated.AnimatedDualWieldGunItem;
import top.ribs.scguns.item.animated.AnimatedGunItem;
import top.ribs.scguns.item.attachment.IAttachment;
import top.ribs.scguns.item.attachment.impl.Scope;
import top.ribs.scguns.util.GunEnchantmentHelper;
import top.ribs.scguns.util.GunModifierHelper;

public class GunRenderingHandler {
   private final Map<Integer, Integer> entityShotCount = new HashMap<>();
   public static final Map<Integer, Vec3> entityIdToFlashPosition = new HashMap<>();
   public static final Map<Integer, Boolean> entityIdToUseEnchantedTexture = new HashMap<>();
   private static GunRenderingHandler instance;
   private final Random random = new Random();
   public static final Set<Integer> entityIdForMuzzleFlash = new HashSet<>();
   private final Set<Integer> entityIdForDrawnMuzzleFlash = new HashSet<>();
   public static final Map<Integer, Float> entityIdToRandomValue = new HashMap<>();
   private int sprintTransition;
   private int prevSprintTransition;
   private int sprintCooldown;
   private float sprintIntensity;
   private float banzaiProgress;
   private float banzaiImpactProgress;
   private float prevBanzaiImpactProgress;
   private float sprintToBanzaiProgress;
   private float prevSprintToBanzaiProgress;
   private float offhandTranslate;
   private float prevOffhandTranslate;
   private Field equippedProgressMainHandField;
   private Field prevEquippedProgressMainHandField;
   private float immersiveRoll;
   private float prevImmersiveRoll;
   private float fallSway;
   private float prevFallSway;
   private float meleeProgress;
   private float prevMeleeProgress;
   private boolean isMeleeAttacking;
   private long meleeStartTime;
   public static final float MELEE_DURATION = 400.0F;
   public float thirdPersonMeleeProgress;
   public float prevThirdPersonMeleeProgress;
   private boolean isThirdPersonMeleeAttacking;
   private long thirdPersonMeleeStartTime;
   public static final float THIRD_PERSON_MELEE_DURATION = 400.0F;
   private static final long PARTICLE_COOLDOWN_MS = 100L;
   private long lastParticleSpawnTime = 0L;
   private float thirdPersonMeleeStartTick = -1.0F;
   private static final float THIRD_PERSON_MELEE_TICKS = 8.0F;
   @Nullable
   private ItemStack renderingWeapon;

   public static GunRenderingHandler get() {
      if (instance == null) {
         instance = new GunRenderingHandler();
      }

      return instance;
   }

   private GunRenderingHandler() {
      super();
   }

   @Nullable
   public ItemStack getRenderingWeapon() {
      return this.renderingWeapon;
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent.Post event) {
      {
         this.updateState();
         this.handleClientTick();
      }
   }

   public void updateDualWieldShotCount(int entityId, int shotCount) {
      this.entityShotCount.put(entityId, shotCount);
   }

   private void updateState() {
      this.updateSprinting();
      this.updateMuzzleFlash();
      this.updateOffhandTranslate();
      this.updateImmersiveCamera();
   }

   private void handleClientTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.isWindowActive()) {
         Player player = mc.player;
         if (player != null) {
            ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!heldItem.isEmpty()) {
               this.updateMelee();
            }
         }
      }
   }


   private void updateSprinting() {
      this.prevSprintTransition = this.sprintTransition;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null
         && mc.player.isSprinting()
         && !(Boolean)ModSyncedDataKeys.SHOOTING.getValue(mc.player)
         && !(Boolean)ModSyncedDataKeys.RELOADING.getValue(mc.player)
         && !AimingHandler.get().isAiming()
         && this.sprintCooldown == 0) {
         if (this.sprintTransition < 5) {
            this.sprintTransition++;
         }
      } else if (this.sprintTransition > 0) {
         this.sprintTransition--;
      }

      if (this.sprintCooldown > 0) {
         this.sprintCooldown--;
      }
   }

   public void updateMuzzleFlash() {
      entityIdForMuzzleFlash.removeAll(this.entityIdForDrawnMuzzleFlash);
      entityIdToRandomValue.keySet().removeAll(this.entityIdForDrawnMuzzleFlash);
      entityIdToFlashPosition.keySet().removeAll(this.entityIdForDrawnMuzzleFlash);
      entityIdToUseEnchantedTexture.keySet().removeAll(this.entityIdForDrawnMuzzleFlash);
      this.entityIdForDrawnMuzzleFlash.clear();
      this.entityIdForDrawnMuzzleFlash.addAll(entityIdForMuzzleFlash);
   }

   private void updateOffhandTranslate() {
      this.prevOffhandTranslate = this.offhandTranslate;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         boolean down = false;
         ItemStack heldItem = mc.player.getMainHandItem();
         if (heldItem.getItem() instanceof GunItem) {
            Gun modifiedGun = ((GunItem)heldItem.getItem()).getModifiedGun(heldItem);
            GripType gripType = modifiedGun.getGeneral().getGripType(heldItem);
            if (gripType == GripType.ONE_HANDED) {
               down = (Boolean)ModSyncedDataKeys.RELOADING.getValue(mc.player);
            } else {
               down = !gripType.heldAnimation().canRenderOffhandItem() || (Boolean)ModSyncedDataKeys.RELOADING.getValue(mc.player);
            }
         }

         float direction = down ? -0.6F : 0.6F;
         this.offhandTranslate = Mth.clamp(this.offhandTranslate + direction, -1.0F, 1.0F);
      }
   }

   private void updateImmersiveCamera() {
      this.prevImmersiveRoll = this.immersiveRoll;
      this.prevFallSway = this.fallSway;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         ItemStack heldItem = mc.player.getMainHandItem();
         boolean isGun = heldItem.getItem() instanceof GunItem;
         if ((Boolean)Config.CLIENT.display.restrictCameraRollToWeapons.get() && !isGun) {
            this.immersiveRoll = 0.0F;
         } else {
            float targetAngle = mc.player.input.leftImpulse;
            float speed = mc.player.input.leftImpulse != 0.0F ? 0.1F : 0.15F;
            this.immersiveRoll = Mth.lerp(speed, this.immersiveRoll, targetAngle);
            float deltaY = (float)Mth.clamp(mc.player.yo - mc.player.getY(), -1.0, 1.0);
            deltaY *= (float)(1.0 - AimingHandler.get().getNormalisedAdsProgress());
            deltaY *= (float)(1.0 - (double)(Mth.abs(mc.player.getXRot()) / 90.0F));
            this.fallSway = Mth.approach(this.fallSway, deltaY * 60.0F * ((Double)Config.CLIENT.display.swaySensitivity.get()).floatValue(), 10.0F);
            float intensity = mc.player.isSprinting() ? 0.75F : 1.0F;
            this.sprintIntensity = Mth.approach(this.sprintIntensity, intensity, 0.1F);
         }
      }
   }

   private void updateMelee() {
      this.updateCustomMeleeAnimation();
      this.prevSprintToBanzaiProgress = this.sprintToBanzaiProgress;
      this.prevBanzaiImpactProgress = this.banzaiImpactProgress;
      if (MeleeAttackHandler.isBanzaiActive()) {
         this.banzaiProgress = Mth.clamp(this.banzaiProgress + 0.3F, 0.0F, 1.0F);
         this.sprintToBanzaiProgress = Mth.clamp(this.sprintToBanzaiProgress + 0.2F, 0.0F, 1.0F);
      } else {
         this.banzaiProgress = Mth.clamp(this.banzaiProgress - 0.3F, 0.0F, 1.0F);
         this.sprintToBanzaiProgress = Mth.clamp(this.sprintToBanzaiProgress - 0.2F, 0.0F, 1.0F);
      }

      long currentTime = System.currentTimeMillis();
      if (this.isMeleeAttacking) {
         long elapsed = currentTime - this.meleeStartTime;
         this.prevMeleeProgress = this.meleeProgress;
         this.meleeProgress = Math.min((float)elapsed / 400.0F, 1.0F);
         if (this.meleeProgress >= 1.0F) {
            this.isMeleeAttacking = false;
            this.meleeProgress = 0.0F;
            this.prevMeleeProgress = 0.0F;
            ModSyncedDataKeys.MELEE.setValue(Minecraft.getInstance().player, false);
         }
      } else {
         this.meleeProgress = 0.0F;
         this.prevMeleeProgress = 0.0F;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         float currentTick = (float)mc.player.tickCount + mc.getTimer().getGameTimeDeltaPartialTick(false);
         if (this.isThirdPersonMeleeAttacking) {
            if (this.thirdPersonMeleeStartTick < 0.0F || currentTick < this.thirdPersonMeleeStartTick) {
               this.thirdPersonMeleeStartTick = currentTick;
            }

            float elapsedTicks = currentTick - this.thirdPersonMeleeStartTick;
            this.prevThirdPersonMeleeProgress = this.thirdPersonMeleeProgress;
            this.thirdPersonMeleeProgress = Math.min(elapsedTicks / 8.0F, 1.0F);
            if (this.thirdPersonMeleeProgress >= 1.0F) {
               this.isThirdPersonMeleeAttacking = false;
               this.thirdPersonMeleeProgress = 0.0F;
               this.prevThirdPersonMeleeProgress = 0.0F;
               this.thirdPersonMeleeStartTick = -1.0F;
               ModSyncedDataKeys.MELEE.setValue(mc.player, false);
            }
         } else {
            this.thirdPersonMeleeProgress = 0.0F;
            this.prevThirdPersonMeleeProgress = 0.0F;
            this.thirdPersonMeleeStartTick = -1.0F;
         }
      }

      if (this.banzaiImpactProgress > 0.0F) {
         this.banzaiImpactProgress = Mth.clamp(this.banzaiImpactProgress - 0.1F, 0.0F, 1.0F);
      }
   }

   private void applySprintingTransforms(Gun modifiedGun, ItemStack stack, HumanoidArm hand, PoseStack poseStack, float partialTicks) {
      GripType gripType = modifiedGun.determineGripType(stack);
      float leftHanded = hand == HumanoidArm.LEFT ? -1.0F : 1.0F;
      float transition = ((float)this.prevSprintTransition + (float)(this.sprintTransition - this.prevSprintTransition) * partialTicks) / 5.0F;
      transition = (float)Math.sin((double)transition * Math.PI / 2.0);
      float sprintToBanzai = Mth.lerp(partialTicks, this.prevSprintToBanzaiProgress, this.sprintToBanzaiProgress);
      transition *= 1.0F - sprintToBanzai;
      double adsProgress = AimingHandler.get().getNormalisedAdsProgress();
      transition *= (float)(1.0 - adsProgress);
      if ((Boolean)Config.CLIENT.display.sprintAnimation.get() && gripType.heldAnimation().canApplySprintingAnimation() && transition > 0.001F) {
         if (!(stack.getItem() instanceof AnimatedGunItem)) {
            poseStack.translate(-0.25 * (double)leftHanded * (double)transition, -0.1 * (double)transition, 0.0);
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F * leftHanded * transition));
            poseStack.mulPose(Axis.XP.rotationDegrees(-25.0F * transition));
         } else {
            poseStack.translate(-0.25 * (double)leftHanded * (double)transition, -0.1 * (double)transition, 0.0);
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F * leftHanded * transition));
            poseStack.mulPose(Axis.XP.rotationDegrees(-25.0F * transition));
         }
      }
   }

   private void applyBanzaiTransforms(PoseStack poseStack, float partialTicks) {
      float sprintToBanzai = Mth.lerp(partialTicks, this.prevSprintToBanzaiProgress, this.sprintToBanzaiProgress);
      if (sprintToBanzai > 0.0F) {
         poseStack.translate(0.0, -0.1 * (double)sprintToBanzai, 0.2 * (double)sprintToBanzai);
         poseStack.mulPose(Axis.XP.rotationDegrees(10.0F * sprintToBanzai));
      }

      float impactProgress = Mth.lerp(partialTicks, this.prevBanzaiImpactProgress, this.banzaiImpactProgress);
      if (impactProgress > 0.0F) {
         poseStack.translate(0.0, 0.0, 0.1 * (double)impactProgress);
      }
   }

   public void triggerBanzaiImpact() {
      this.banzaiImpactProgress = 1.0F;
   }

   public void startBayonetStabAnimation() {
      this.isMeleeAttacking = true;
      this.meleeStartTime = System.currentTimeMillis();
      this.meleeProgress = 0.0F;
      this.prevMeleeProgress = 0.0F;
      ModSyncedDataKeys.MELEE.setValue(Minecraft.getInstance().player, true);
   }

   public void startThirdPersonMeleeAnimation() {
      this.isThirdPersonMeleeAttacking = true;
      this.thirdPersonMeleeStartTick = -1.0F;
      this.thirdPersonMeleeProgress = 0.0F;
      this.prevThirdPersonMeleeProgress = 0.0F;
      ModSyncedDataKeys.MELEE.setValue(Minecraft.getInstance().player, true);
   }

   public boolean isThirdPersonMeleeAttacking() {
      return this.isThirdPersonMeleeAttacking;
   }

   public void startMeleeAnimation(ItemStack heldItem) {
      long currentTime = System.currentTimeMillis();
      LocalPlayer player = Minecraft.getInstance().player;
      if (heldItem.getItem() instanceof GunItem gunItem) {
         assert player != null;

         if (MeleeAttackHandler.isMeleeOnCooldown(player, heldItem)) {
            return;
         }

         MeleeAttackHandler.setMeleeCooldown(player, heldItem, gunItem);
         Gun modifiedGun = gunItem.getModifiedGun(heldItem);
         if (modifiedGun.getGeneral().usesCustomMeleeAnimation() && heldItem.getItem() instanceof AnimatedGunItem) {
            CompoundTag tag = NbtHelper.getOrCreateTag(heldItem);
            tag.putBoolean("scguns:IsMelee", true);
            tag.putLong("MeleeStartTime", currentTime);
            ModSyncedDataKeys.MELEE.setValue(player, true);
            return;
         }
      }

      if ((float)(currentTime - this.meleeStartTime) >= 400.0F) {
         this.isMeleeAttacking = true;
         this.meleeStartTime = currentTime;
         this.meleeProgress = 0.0F;
         this.prevMeleeProgress = 0.0F;
         ModSyncedDataKeys.MELEE.setValue(player, true);
      }
   }

   private void updateCustomMeleeAnimation() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         ItemStack heldItem = player.getMainHandItem();
         if (heldItem.getItem() instanceof GunItem gunItem) {
            Gun gun = gunItem.getModifiedGun(heldItem);
            if (gun.getGeneral().usesCustomMeleeAnimation()) {
               if (heldItem.getItem() instanceof AnimatedGunItem) {
                  CompoundTag tag = NbtHelper.getOrCreateTag(heldItem);
                  if (tag.getBoolean("scguns:IsMelee")) {
                     long currentTime = System.currentTimeMillis();
                     long meleeStartTime = tag.getLong("MeleeStartTime");
                     if ((float)(currentTime - meleeStartTime) >= 400.0F) {
                        tag.remove("scguns:IsMelee");
                        tag.remove("MeleeStartTime");
                        ModSyncedDataKeys.MELEE.setValue(player, false);
                     }
                  }
               }
            }
         }
      }
   }

   private void applyMeleeTransforms(PoseStack poseStack, float partialTicks) {
      if (this.isMeleeAttacking) {
         float progress = Mth.lerp(partialTicks, this.prevMeleeProgress, this.meleeProgress);

         assert Minecraft.getInstance().player != null;

         ItemStack heldItem = Minecraft.getInstance().player.getMainHandItem();
         if (heldItem.getItem() instanceof GunItem gunItem) {
            Gun gun = gunItem.getModifiedGun(heldItem);
            if (gun.getGeneral().usesCustomMeleeAnimation() && heldItem.getItem() instanceof AnimatedGunItem) {
               return;
            }

            if ((Boolean)Config.CLIENT.display.cinematicGunEffects.get() && gun.getGeneral().hasCameraShake()) {
               this.addCameraShake(gun, 0.5F * (1.0F - progress), 0);
            }

            boolean isBayonetEquipped = gunItem.hasBayonet(heldItem);
            if (isBayonetEquipped) {
               if (progress < 0.3F) {
                  float stabProgress = progress / 0.3F;
                  poseStack.translate(0.0, 0.0, -0.35 * (double)stabProgress);
                  poseStack.mulPose(Axis.XP.rotationDegrees(10.0F * stabProgress));
                  if ((Boolean)Config.CLIENT.display.cinematicGunEffects.get() && gun.getGeneral().hasCameraShake()) {
                     this.addCameraShake(gun, 0.3F * stabProgress, 3);
                  }
               } else {
                  float returnProgress = (progress - 0.3F) / 0.7F;
                  poseStack.translate(0.0, 0.0, 0.35 * (double)(returnProgress - 1.0F));
                  poseStack.mulPose(Axis.XP.rotationDegrees(10.0F * (1.0F - returnProgress)));
                  if ((Boolean)Config.CLIENT.display.cinematicGunEffects.get() && gun.getGeneral().hasCameraShake()) {
                     this.addCameraShake(gun, 0.1F * (1.0F - returnProgress), 2);
                  }
               }
            } else if (progress < 0.33F) {
               float raiseProgress = progress / 0.33F;
               poseStack.translate(0.0, 0.35 * (double)raiseProgress, 0.0);
               poseStack.translate(0.0, 0.0, 0.1 * (double)raiseProgress);
               poseStack.mulPose(Axis.XP.rotationDegrees(35.0F * raiseProgress));
            } else if (progress < 0.66F) {
               float swingProgress = (progress - 0.33F) / 0.33F;
               poseStack.translate(0.0, 0.35 - 0.7 * (double)swingProgress, 0.0);
               poseStack.translate(0.0, 0.1 - 0.2 * (double)swingProgress, 0.0);
               poseStack.mulPose(Axis.XP.rotationDegrees(35.0F - 70.0F * swingProgress));
            } else {
               float returnProgress = (progress - 0.66F) / 0.34F;
               poseStack.translate(0.0, -0.35 * (double)(1.0F - returnProgress), 0.0);
               poseStack.translate(0.0, 0.0, -0.1 * (double)(1.0F - returnProgress));
               poseStack.mulPose(Axis.XP.rotationDegrees(-35.0F * (1.0F - returnProgress)));
            }
         }
      }
   }

   @SubscribeEvent
   public void onRenderOverlay(RenderHandEvent event) {
      PoseStack poseStack = event.getPoseStack();
      boolean right = Minecraft.getInstance().options.mainHand().get() == HumanoidArm.RIGHT
         ? event.getHand() == InteractionHand.MAIN_HAND
         : event.getHand() == InteractionHand.OFF_HAND;
      HumanoidArm hand = right ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
      ItemStack heldItem = event.getItemStack();
      if (event.getHand() == InteractionHand.OFF_HAND) {
         if (heldItem.getItem() instanceof GunItem) {
            // 0.5.5 cancelled the vanilla hand render here. Without it the vanilla
            // first-person pass still draws the offhand item on top of ours.
            event.setCanceled(true);
            return;
         }

         float offhand = 1.0F - Mth.lerp(event.getPartialTick(), this.prevOffhandTranslate, this.offhandTranslate);
         poseStack.translate(0.0F, offhand * -0.6F, 0.0F);
         Player player = Minecraft.getInstance().player;
         if (player != null && player.getMainHandItem().getItem() instanceof GunItem) {
            Gun modifiedGun = ((GunItem)player.getMainHandItem().getItem()).getModifiedGun(player.getMainHandItem());
            GripType gripType = modifiedGun.getGeneral().getGripType(player.getMainHandItem());
            if (gripType != GripType.ONE_HANDED && !gripType.heldAnimation().canRenderOffhandItem()) {
               return;
            }
         }

         poseStack.translate(0.0, -1.0 * AimingHandler.get().getNormalisedAdsProgress(), 0.0);
      }

      if (heldItem.getItem() instanceof GunItem gunItem) {
         // 0.5.5 cancelled here as well: this handler draws the gun (and its own arms)
         // itself, so letting the vanilla pass continue would draw the vanilla arm and
         // the held item a second time - "the gun renders twice, with an extra arm".
         event.setCanceled(true);
         ItemStack var32 = ItemStack.EMPTY;
         if (NbtHelper.getTag(heldItem) != null && NbtHelper.getTag(heldItem).contains("Model", 10)) {
            var32 = top.ribs.scguns.util.NbtHelper.itemFromTag(NbtHelper.getTag(heldItem).getCompound("Model"));
         }

         LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
         BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(var32.isEmpty() ? heldItem : var32, player.level(), player, 0);
         float scaleX = model.getTransforms().firstPersonRightHand.scale.x();
         float scaleY = model.getTransforms().firstPersonRightHand.scale.y();
         float scaleZ = model.getTransforms().firstPersonRightHand.scale.z();
         float translateX = model.getTransforms().firstPersonRightHand.translation.x();
         float translateY = model.getTransforms().firstPersonRightHand.translation.y();
         float translateZ = model.getTransforms().firstPersonRightHand.translation.z();
         poseStack.pushPose();
         Gun modifiedGun = gunItem.getModifiedGun(heldItem);
         if (AimingHandler.get().getNormalisedAdsProgress() > 0.0 && modifiedGun.canAimDownSight() && event.getHand() == InteractionHand.MAIN_HAND) {
            double xOffset = (double)translateX;
            double yOffset = (double)translateY;
            double zOffset = (double)translateZ;
            xOffset -= 0.5 * (double)scaleX;
            yOffset -= 0.5 * (double)scaleY;
            zOffset -= 0.5 * (double)scaleZ;
            Vec3 gunOrigin = PropertyHelper.getModelOrigin(heldItem, PropertyHelper.GUN_DEFAULT_ORIGIN);
            xOffset += gunOrigin.x * 0.0625 * (double)scaleX;
            yOffset += gunOrigin.y * 0.0625 * (double)scaleY;
            zOffset += gunOrigin.z * 0.0625 * (double)scaleZ;
            Scope scope = Gun.getScope(heldItem);
            if (modifiedGun.canAttachType(IAttachment.Type.SCOPE) && scope != null) {
               Vec3 scopePosition = PropertyHelper.getAttachmentPosition(heldItem, modifiedGun, IAttachment.Type.SCOPE).subtract(gunOrigin);
               xOffset += scopePosition.x * 0.0625 * (double)scaleX;
               yOffset += scopePosition.y * 0.0625 * (double)scaleY;
               zOffset += scopePosition.z * 0.0625 * (double)scaleZ;
               ItemStack scopeStack = Gun.getScopeStack(heldItem);
               Vec3 scopeOrigin = PropertyHelper.getModelOrigin(scopeStack, PropertyHelper.ATTACHMENT_DEFAULT_ORIGIN);
               Vec3 scopeCamera = PropertyHelper.getScopeCamera(scopeStack).subtract(scopeOrigin);
               Vec3 scopeScale = PropertyHelper.getAttachmentScale(heldItem, modifiedGun, IAttachment.Type.SCOPE);
               xOffset += scopeCamera.x * 0.0625 * (double)scaleX * scopeScale.x;
               yOffset += scopeCamera.y * 0.0625 * (double)scaleY * scopeScale.y;
               zOffset += scopeCamera.z * 0.0625 * (double)scaleZ * scopeScale.z;
            } else {
               Vec3 ironSightCamera = PropertyHelper.getIronSightCamera(heldItem, modifiedGun, gunOrigin).subtract(gunOrigin);
               xOffset += ironSightCamera.x * 0.0625 * (double)scaleX;
               yOffset += ironSightCamera.y * 0.0625 * (double)scaleY;
               zOffset += ironSightCamera.z * 0.0625 * (double)scaleZ;
               if (PropertyHelper.isLegacyIronSight(heldItem)) {
                  zOffset += 0.72;
               }
            }

            float side = right ? 1.0F : -1.0F;
            double time = AimingHandler.get().getNormalisedAdsProgress();
            double transition = PropertyHelper.getSightAnimations(heldItem, modifiedGun).getSightCurve().apply(time);
            poseStack.translate(-0.56 * (double)side * transition, 0.52 * transition, 0.72 * transition);
            poseStack.translate(-xOffset * (double)side * transition, -yOffset * transition, -zOffset * transition);
         }

         this.applyBobbingTransforms(poseStack, event.getPartialTick());
         this.applyBanzaiTransforms(poseStack, event.getPartialTick());
         float equipProgress = this.getEquipProgress(event.getPartialTick());
         poseStack.mulPose(Axis.XP.rotationDegrees(equipProgress * -50.0F));
         this.renderReloadArm(poseStack, event.getMultiBufferSource(), event.getPackedLight(), modifiedGun, heldItem, hand, translateX);
         int offset = right ? 1 : -1;
         poseStack.translate(0.56 * (double)offset, -0.52, -0.72);
         this.applyAimingTransforms(poseStack, heldItem, modifiedGun, translateX, translateY, translateZ, offset);
         this.applySwayTransforms(poseStack, modifiedGun, heldItem, player, translateX, translateY, translateZ, event.getPartialTick());
         this.applySprintingTransforms(modifiedGun, heldItem, hand, poseStack, event.getPartialTick());
         this.applyRecoilTransforms(poseStack, heldItem, modifiedGun);
         this.applyReloadTransforms(poseStack, event.getPartialTick());
         this.applyShieldTransforms(poseStack, player, modifiedGun, heldItem, event.getPartialTick());
         this.applyMeleeTransforms(poseStack, event.getPartialTick());
         int blockLight = player.isOnFire() ? 15 : player.level().getBrightness(LightLayer.BLOCK, BlockPos.containing(player.getEyePosition(event.getPartialTick())));
         blockLight += entityIdForMuzzleFlash.contains(player.getId()) ? 3 : 0;
         blockLight = Math.min(blockLight, 15);
         int packedLight = LightTexture.pack(
            blockLight, player.level().getBrightness(LightLayer.SKY, BlockPos.containing(player.getEyePosition(event.getPartialTick())))
         );
         poseStack.pushPose();
         modifiedGun.getGeneral()
            .getGripType(heldItem)
            .heldAnimation()
            .renderFirstPersonArms(Minecraft.getInstance().player, hand, heldItem, poseStack, event.getMultiBufferSource(), packedLight, event.getPartialTick());
         poseStack.popPose();
         ItemDisplayContext display = right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
         this.renderWeapon(
            Minecraft.getInstance().player, heldItem, display, event.getPoseStack(), event.getMultiBufferSource(), packedLight, event.getPartialTick()
         );
         poseStack.popPose();
      }
   }

   @SubscribeEvent
   public void onGunFire(GunFireEvent.Post event) {
      if (event.isClient()) {
         this.sprintTransition = 0;
         this.sprintCooldown = 20;
         ItemStack heldItem = event.getStack();
         GunItem gunItem = (GunItem)heldItem.getItem();
         Gun modifiedGun = gunItem.getModifiedGun(heldItem);
         if ((Boolean)Config.CLIENT.display.cinematicGunEffects.get()) {
            this.addCameraShake(modifiedGun, 0.5F, 10);
         }

         if (event.getShooter() instanceof Player && modifiedGun.getDisplay().getFlash() != null) {
            int entityId = event.getShooter().getId();
            this.showMuzzleFlashForPlayer(entityId);
            if (gunItem instanceof AnimatedDualWieldGunItem) {
               DualWieldShotTracker.get().incrementShotCount(entityId);
            }

            this.entityShotCount.put(entityId, DualWieldShotTracker.get().getShotCount(entityId));
         }
      }
   }

   private void addCameraShake(Gun gun, float baseIntensity, int durationTicks) {
      if (gun.getGeneral().hasCameraShake()) {
         float randomIntensity = baseIntensity + (this.random.nextFloat() - 0.5F) * 0.2F;
         int randomDuration = durationTicks + this.random.nextInt(5) - 2;
         this.immersiveRoll = randomIntensity;
         this.sprintCooldown = Math.max(randomDuration, 1);
      }
   }

   public void showMuzzleFlashForPlayer(int entityId) {
      entityIdForMuzzleFlash.add(entityId);
      entityIdToRandomValue.put(entityId, this.random.nextFloat());
   }

   @SubscribeEvent
   public void onComputeFov(ComputeFov event) {
      if (!event.usedConfiguredFov()) {
         LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
         ItemStack heldItem = player.getMainHandItem();
         if (heldItem.getItem() instanceof GunItem gunItem) {
            Gun modifiedGun = gunItem.getModifiedGun(heldItem);
            if (modifiedGun.canAimDownSight()) {
               if (!(AimingHandler.get().getNormalisedAdsProgress() <= 0.0)) {
                  double time = AimingHandler.get().getNormalisedAdsProgress();
                  SightAnimation sightAnimation = PropertyHelper.getSightAnimations(heldItem, modifiedGun);
                  time = sightAnimation.getViewportCurve().apply(time);
                  double viewportFov = PropertyHelper.getViewportFov(heldItem, modifiedGun);
                  double newFov = viewportFov > 0.0 ? viewportFov : event.getFOV();
                  event.setFOV(Mth.lerp(time, event.getFOV(), newFov));
               }
            }
         }
      }
   }

   private void applyBobbingTransforms(PoseStack poseStack, float partialTicks) {
      Minecraft mc = Minecraft.getInstance();
      if ((Boolean)mc.options.bobView().get() && mc.getCameraEntity() instanceof Player player) {
         float deltaDistanceWalked = player.walkDist - player.walkDistO;
         float distanceWalked = -(player.walkDist + deltaDistanceWalked * partialTicks);
         float bobbing = Mth.lerp(partialTicks, player.oBob, player.bob);
         poseStack.mulPose(Axis.XP.rotationDegrees(-(Math.abs(Mth.cos(distanceWalked * (float) Math.PI - 0.2F) * bobbing) * 5.0F)));
         poseStack.mulPose(Axis.ZP.rotationDegrees(-(Mth.sin(distanceWalked * (float) Math.PI) * bobbing * 3.0F)));
         poseStack.translate(
            (double)(-(Mth.sin(distanceWalked * (float) Math.PI) * bobbing * 0.5F)),
            (double)(-(-Math.abs(Mth.cos(distanceWalked * (float) Math.PI) * bobbing))),
            0.0
         );
         bobbing *= (float)(player.isSprinting() ? 8.0 : 4.0);
         bobbing = (float)((double)bobbing * (Double)Config.CLIENT.display.bobbingIntensity.get());
         double invertZoomProgress = 1.0 - AimingHandler.get().getNormalisedAdsProgress() * (double)this.sprintIntensity;
         poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(distanceWalked * (float) Math.PI) * bobbing * 3.0F * (float)invertZoomProgress));
         poseStack.mulPose(
            Axis.XP.rotationDegrees(Math.abs(Mth.cos(distanceWalked * (float) Math.PI - 0.2F) * bobbing) * 5.0F * (float)invertZoomProgress)
         );
      }
   }

   private void applyAimingTransforms(PoseStack poseStack, ItemStack heldItem, Gun modifiedGun, float x, float y, float z, int offset) {
      if (!(Boolean)Config.CLIENT.display.oldAnimations.get()) {
         poseStack.translate(x * (float)offset, y, z);
         poseStack.translate(0.0, -0.25, 0.25);
         float aiming = (float)Math.sin(Math.toRadians(AimingHandler.get().getNormalisedAdsProgress() * 180.0));
         aiming = PropertyHelper.getSightAnimations(heldItem, modifiedGun).getAimTransformCurve().apply(aiming);
         poseStack.mulPose(Axis.ZP.rotationDegrees(aiming * 10.0F * (float)offset));
         poseStack.mulPose(Axis.XP.rotationDegrees(aiming * 5.0F));
         poseStack.mulPose(Axis.YP.rotationDegrees(aiming * 5.0F * (float)offset));
         poseStack.translate(0.0, 0.25, -0.25);
         poseStack.translate(-x * (float)offset, -y, -z);
      }
   }

   private void applySwayTransforms(PoseStack poseStack, Gun modifiedGun, ItemStack stack, LocalPlayer player, float x, float y, float z, float partialTicks) {
      if ((Boolean)Config.CLIENT.display.weaponSway.get() && player != null) {
         poseStack.translate(x, y, z);
         double zOffset = modifiedGun.determineGripType(stack).heldAnimation().getFallSwayZOffset();
         poseStack.translate(0.0, -0.25, zOffset);
         poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, this.prevFallSway, this.fallSway)));
         poseStack.translate(0.0, 0.25, -zOffset);
         float bobPitch = Mth.rotLerp(partialTicks, player.xBobO, player.xBob);
         float headPitch = Mth.rotLerp(partialTicks, player.xRotO, player.getXRot());
         float swayPitch = headPitch - bobPitch;
         swayPitch *= (float)(1.0 - 0.5 * AimingHandler.get().getNormalisedAdsProgress());
         poseStack.mulPose(
            ((SwayType)Config.CLIENT.display.swayType.get())
               .getPitchRotation()
               .rotationDegrees(swayPitch * ((Double)Config.CLIENT.display.swaySensitivity.get()).floatValue())
         );
         float bobYaw = Mth.rotLerp(partialTicks, player.yBobO, player.yBob);
         float headYaw = Mth.rotLerp(partialTicks, player.yHeadRotO, player.yHeadRot);
         float swayYaw = headYaw - bobYaw;
         swayYaw *= (float)(1.0 - 0.5 * AimingHandler.get().getNormalisedAdsProgress());
         poseStack.mulPose(
            ((SwayType)Config.CLIENT.display.swayType.get())
               .getYawRotation()
               .rotationDegrees(swayYaw * ((Double)Config.CLIENT.display.swaySensitivity.get()).floatValue())
         );
         poseStack.translate(-x, -y, -z);
      }
   }

   private void applyReloadTransforms(PoseStack poseStack, float partialTicks) {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         ItemStack stack = player.getMainHandItem();
         if (stack.getItem() instanceof GunItem) {
            if (!(stack.getItem() instanceof AnimatedGunItem)) {
               float reloadProgress = ReloadHandler.get().getReloadProgress(partialTicks);
               if (((GunItem)stack.getItem()).getGun().getReloads().getReloadType() == ReloadType.MANUAL) {
                  if (reloadProgress > 0.0F) {
                     poseStack.translate(0.0, -0.2 * (double)reloadProgress, 0.0);
                     poseStack.translate(0.0, 0.0, -0.1 * (double)reloadProgress);
                     poseStack.mulPose(Axis.XP.rotationDegrees(-35.0F * reloadProgress));
                  }
               } else {
                  poseStack.translate(0.0, -0.35 * (double)reloadProgress, 0.0);
                  poseStack.translate(0.0, 0.0, -0.1 * (double)reloadProgress);
                  poseStack.mulPose(Axis.XP.rotationDegrees(-35.0F * reloadProgress));
               }
            }
         }
      }
   }

   private void applyRecoilTransforms(PoseStack poseStack, ItemStack item, Gun gun) {
      double recoilNormal = RecoilHandler.get().getGunRecoilNormal();
      if (Gun.hasAttachmentEquipped(item, gun, IAttachment.Type.SCOPE)) {
         recoilNormal -= recoilNormal * 0.5 * AimingHandler.get().getNormalisedAdsProgress();
      }

      Minecraft mc = Minecraft.getInstance();
      float kickReduction = mc.player != null
         ? 1.0F - GunModifierHelper.getKickReduction(mc.player, item)
         : 1.0F - GunModifierHelper.getKickReduction(item);
      float recoilReduction = mc.player != null
         ? 1.0F - GunModifierHelper.getRecoilModifier(mc.player, item)
         : 1.0F - GunModifierHelper.getRecoilModifier(item);
      double kick = (double)gun.getProjectile().getRecoilKick() * 0.0625 * recoilNormal * RecoilHandler.get().getAdsRecoilReduction(gun);
      float recoilLift = (float)((double)gun.getProjectile().getRecoilAngle() * recoilNormal) * (float)RecoilHandler.get().getAdsRecoilReduction(gun);
      float recoilSwayAmount = (float)(2.0 + 1.0 * (1.0 - AimingHandler.get().getNormalisedAdsProgress()));
      float recoilSway = (float)((double)(RecoilHandler.get().getGunRecoilRandom() * recoilSwayAmount - recoilSwayAmount / 2.0F) * recoilNormal);
      poseStack.translate(0.0, 0.0, kick * (double)kickReduction);
      poseStack.translate(0.0, 0.0, 0.15);
      poseStack.mulPose(Axis.YP.rotationDegrees(recoilSway * recoilReduction));
      poseStack.mulPose(Axis.ZP.rotationDegrees(recoilSway * recoilReduction));
      poseStack.mulPose(Axis.XP.rotationDegrees(recoilLift * recoilReduction));
      poseStack.translate(0.0, 0.0, -0.15);
   }

   private void applyShieldTransforms(PoseStack poseStack, LocalPlayer player, Gun modifiedGun, ItemStack stack, float partialTick) {
      GripType gripType = modifiedGun.determineGripType(stack);
      if (player.isUsingItem() && player.getOffhandItem().getItem() == Items.SHIELD && (gripType == GripType.ONE_HANDED || gripType == GripType.ONE_HANDED_2)) {
         double time = Mth.clamp((double)((float)player.getTicksUsingItem() + partialTick), 0.0, 4.0) / 4.0;
         if (gripType == GripType.ONE_HANDED) {
            poseStack.translate(0.45 * time, -0.05 * time, 0.15 * time);
            poseStack.mulPose(Axis.YP.rotationDegrees(30.0F * (float)time));
            poseStack.mulPose(Axis.XP.rotationDegrees(10.0F * (float)time));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-8.0F * (float)time));
         }
      }
   }

   public void applyWeaponScale(ItemStack heldItem, PoseStack stack) {
      if (NbtHelper.getTag(heldItem) != null) {
         CompoundTag compound = NbtHelper.getTag(heldItem);
         if (compound.contains("Scale", 5)) {
            float scale = compound.getFloat("Scale");
            stack.scale(scale, scale, scale);
         }
      }
   }

   private void renderGun(
      @Nullable LivingEntity entity,
      ItemDisplayContext display,
      ItemStack stack,
      PoseStack poseStack,
      MultiBufferSource renderTypeBuffer,
      int light,
      float partialTicks
   ) {
      if (ModelOverrides.hasModel(stack)) {
         IOverrideModel model = ModelOverrides.getModel(stack);
         if (model != null
            && (
               display == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                  || display == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                  || display == ItemDisplayContext.FIXED
                  || display == ItemDisplayContext.GUI
            )) {
            model.render(partialTicks, display, stack, ItemStack.EMPTY, entity, poseStack, renderTypeBuffer, light, OverlayTexture.NO_OVERLAY);
            if (display == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || display == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
               return;
            }
         }
      }

      if (!(stack.getItem() instanceof AnimatedGunItem)
         || display != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND && display != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
         Level level = entity != null ? entity.level() : null;
         BakedModel bakedModel = Minecraft.getInstance().getItemRenderer().getModel(stack, level, entity, 0);
         Minecraft.getInstance()
            .getItemRenderer()
            .render(stack, ItemDisplayContext.NONE, false, poseStack, renderTypeBuffer, light, OverlayTexture.NO_OVERLAY, bakedModel);
      }
   }

   private void renderAttachments(
      @Nullable LivingEntity entity,
      ItemDisplayContext display,
      ItemStack stack,
      PoseStack poseStack,
      MultiBufferSource renderTypeBuffer,
      int light,
      float partialTicks
   ) {
      if (!(stack.getItem() instanceof AnimatedGunItem)
         || display != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
            && display != ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            && display != ItemDisplayContext.GUI
            && display != ItemDisplayContext.GROUND) {
         if (stack.getItem() instanceof GunItem) {
            Gun modifiedGun = ((GunItem)stack.getItem()).getModifiedGun(stack);
            CompoundTag gunTag = NbtHelper.getOrCreateTag(stack);
            CompoundTag attachments = gunTag.getCompound("Attachments");
            Set<Item> customAttachments = Set.of(
               (Item)ModItems.SILENCER.get(),
               (Item)ModItems.ADVANCED_SILENCER.get(),
               (Item)ModItems.MUZZLE_BRAKE.get(),
               (Item)ModItems.VERTICAL_GRIP.get(),
               (Item)ModItems.LIGHT_GRIP.get(),
               (Item)ModItems.IRON_BAYONET.get(),
               (Item)ModItems.ANTHRALITE_BAYONET.get(),
               (Item)ModItems.DIAMOND_BAYONET.get(),
               (Item)ModItems.NETHERITE_BAYONET.get(),
               (Item)ModItems.EXTENDED_BARREL.get()
            );

            for (String tagKey : attachments.getAllKeys()) {
               IAttachment.Type type = IAttachment.Type.byTagKey(tagKey);
               if (type != null && modifiedGun.canAttachType(type)) {
                  ItemStack attachmentStack = Gun.getAttachment(type, stack);
                  if (!customAttachments.contains(attachmentStack.getItem()) && !attachmentStack.isEmpty()) {
                     poseStack.pushPose();
                     Vec3 origin = PropertyHelper.getModelOrigin(attachmentStack, PropertyHelper.ATTACHMENT_DEFAULT_ORIGIN);
                     poseStack.translate(-origin.x * 0.0625, -origin.y * 0.0625, -origin.z * 0.0625);
                     Vec3 gunOrigin = PropertyHelper.getModelOrigin(stack, PropertyHelper.GUN_DEFAULT_ORIGIN);
                     poseStack.translate(gunOrigin.x * 0.0625, gunOrigin.y * 0.0625, gunOrigin.z * 0.0625);
                     Vec3 translation = PropertyHelper.getAttachmentPosition(stack, modifiedGun, type).subtract(gunOrigin);
                     poseStack.translate(translation.x * 0.0625, translation.y * 0.0625, translation.z * 0.0625);
                     Vec3 scale = PropertyHelper.getAttachmentScale(stack, modifiedGun, type);
                     Vec3 center = origin.subtract(8.0, 8.0, 8.0).scale(0.0625);
                     poseStack.translate(center.x, center.y, center.z);
                     poseStack.scale((float)scale.x, (float)scale.y, (float)scale.z);
                     poseStack.translate(-center.x, -center.y, -center.z);
                     IOverrideModel model = ModelOverrides.getModel(attachmentStack);
                     if (model != null) {
                        model.render(partialTicks, display, attachmentStack, stack, entity, poseStack, renderTypeBuffer, light, OverlayTexture.NO_OVERLAY);
                     } else {
                        Level level = entity != null ? entity.level() : null;
                        BakedModel bakedModel = Minecraft.getInstance().getItemRenderer().getModel(attachmentStack, level, entity, 0);
                        Minecraft.getInstance()
                           .getItemRenderer()
                           .render(
                              attachmentStack,
                              ItemDisplayContext.NONE,
                              false,
                              poseStack,
                              renderTypeBuffer,
                              light,
                              OverlayTexture.NO_OVERLAY,
                              GunModel.wrap(bakedModel)
                           );
                     }

                     poseStack.popPose();
                  }
               }
            }
         }
      }
   }

   public void renderWeapon(
      @Nullable LivingEntity entity,
      ItemStack stack,
      ItemDisplayContext display,
      PoseStack poseStack,
      MultiBufferSource renderTypeBuffer,
      int light,
      float partialTicks
   ) {
      if (stack.getItem() instanceof GunItem) {
         poseStack.pushPose();
         RenderUtil.applyTransformType(stack, poseStack, display, entity);
         this.renderGun(entity, display, stack, poseStack, renderTypeBuffer, light, partialTicks);
         this.renderAttachments(entity, display, stack, poseStack, renderTypeBuffer, light, partialTicks);
         this.renderMuzzleFlash(entity, poseStack, renderTypeBuffer, stack, display, partialTicks);
         poseStack.popPose();
      }
   }

   private void renderMuzzleFlash(
      @Nullable LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer, ItemStack weapon, ItemDisplayContext display, float partialTicks
   ) {
      Gun modifiedGun = ((GunItem)weapon.getItem()).getModifiedGun(weapon);
      Gun.Display.Flash flash = modifiedGun.getDisplay().getFlash();
      if (flash != null) {
         if (entity != null) {
            if (display == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
               || display == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
               || display == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
               || display == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
               boolean isBeamActive = BeamHandler.activeBeams.containsKey(entity.getUUID());
               if (isBeamActive || entityIdForMuzzleFlash.contains(entity.getId())) {
                  float randomValue = entityIdToRandomValue.getOrDefault(entity.getId(), 0.0F);
                  ResourceLocation flashTexture = ResourceLocation.fromNamespaceAndPath("scguns", "textures/effect/" + flash.getTextureLocation() + ".png");
                  boolean mirror = this.entityShotCount.getOrDefault(entity.getId(), 0) % 2 == 1 && flash.hasAlternateMuzzleFlash();
                  this.drawMuzzleFlash(weapon, modifiedGun, randomValue, mirror, poseStack, buffer, partialTicks, flashTexture, entity);
                  if (flash.shouldSpawnParticles() && flash.getParticleType() != null) {
                     this.spawnParticles(flash, entity);
                  }
               }
            }
         }
      }
   }

   private void drawMuzzleFlash(
      ItemStack weapon,
      Gun modifiedGun,
      float random,
      boolean mirror,
      PoseStack poseStack,
      MultiBufferSource buffer,
      float partialTicks,
      ResourceLocation flashTexture,
      LivingEntity entity
   ) {
      if (PropertyHelper.hasMuzzleFlash(weapon, modifiedGun)) {
         Gun.Display.Flash flash = modifiedGun.getDisplay().getFlash();
         if (flash != null) {
            int shotCount = DualWieldShotTracker.get().getShotCount(entity.getId());
            Vec3 muzzlePosition = shotCount % 2 == 0 ? Vec3.ZERO : flash.getAlternatePosition();
            this.drawSingleMuzzleFlash(weapon, modifiedGun, random, mirror, poseStack, buffer, partialTicks, flashTexture, muzzlePosition, entity);
         }
      }
   }

   private void drawSingleMuzzleFlash(
      ItemStack weapon,
      Gun modifiedGun,
      float random,
      boolean mirror,
      PoseStack poseStack,
      MultiBufferSource buffer,
      float partialTicks,
      ResourceLocation flashTexture,
      Vec3 offset,
      LivingEntity entity
   ) {
      Gun.Display.Flash flash = modifiedGun.getDisplay().getFlash();
      if (flash != null) {
         poseStack.pushPose();
         Vec3 weaponOrigin = PropertyHelper.getModelOrigin(weapon, PropertyHelper.GUN_DEFAULT_ORIGIN);
         Vec3 flashPosition;
         if (entity != null && entityIdToFlashPosition.containsKey(entity.getId())) {
            flashPosition = entityIdToFlashPosition.get(entity.getId());
         } else {
            flashPosition = PropertyHelper.getMuzzleFlashPosition(weapon, modifiedGun).subtract(weaponOrigin);
         }

         flashPosition = flashPosition.add(offset);
         poseStack.translate(weaponOrigin.x * 0.0625, weaponOrigin.y * 0.0625, weaponOrigin.z * 0.0625);
         poseStack.translate(flashPosition.x * 0.0625, flashPosition.y * 0.0625, flashPosition.z * 0.0625);
         poseStack.translate(-0.5, -0.5, -0.5);
         poseStack.mulPose(Axis.ZP.rotationDegrees(360.0F * random));
         poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
         Vec3 flashScale = PropertyHelper.getMuzzleFlashScale(weapon, modifiedGun);
         float scaleX = (float)flashScale.x / 2.0F - (float)flashScale.x / 2.0F * (1.0F - partialTicks);
         float scaleY = (float)flashScale.y / 2.0F - (float)flashScale.y / 2.0F * (1.0F - partialTicks);
         poseStack.scale(scaleX, scaleY, 1.0F);
         float scaleModifier = (float)GunModifierHelper.getMuzzleFlashScale(weapon, 1.0);
         poseStack.scale(scaleModifier, scaleModifier, 1.0F);
         poseStack.translate(-0.5, -0.5, 0.0);
         boolean shouldUseEnchanted = weapon.isEnchanted();
         if (entity != null && entityIdToUseEnchantedTexture.containsKey(entity.getId())) {
            shouldUseEnchanted = entityIdToUseEnchantedTexture.get(entity.getId());
         }

         float minU = shouldUseEnchanted ? 0.5F : 0.0F;
         float maxU = shouldUseEnchanted ? 1.0F : 0.5F;
         Matrix4f matrix = poseStack.last().pose();
         VertexConsumer builder = buffer.getBuffer(GunRenderType.getMuzzleFlash(flashTexture));
         builder.addVertex(matrix, 0.0F, 0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(maxU, 1.0F).setLight(15728880);
         builder.addVertex(matrix, 1.0F, 0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(minU, 1.0F).setLight(15728880);
         builder.addVertex(matrix, 1.0F, 1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(minU, 0.0F).setLight(15728880);
         builder.addVertex(matrix, 0.0F, 1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(maxU, 0.0F).setLight(15728880);
         poseStack.popPose();
         poseStack.pushPose();
         poseStack.translate(weaponOrigin.x * 0.0625, weaponOrigin.y * 0.0625, weaponOrigin.z * 0.0625);
         poseStack.translate(flashPosition.x * 0.0625, flashPosition.y * 0.0625, flashPosition.z * 0.0625);
         poseStack.translate(-0.5, -0.5, -0.5);
         poseStack.mulPose(Axis.ZP.rotationDegrees(360.0F * random));
         poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
         poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
         poseStack.scale(scaleX, scaleY, 1.0F);
         poseStack.scale(scaleModifier, scaleModifier, 1.0F);
         poseStack.translate(-0.5, -0.5, 0.0);
         matrix = poseStack.last().pose();
         builder = buffer.getBuffer(GunRenderType.getMuzzleFlash(flashTexture));
         builder.addVertex(matrix, 0.0F, 0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(maxU, 1.0F).setLight(15728880);
         builder.addVertex(matrix, 1.0F, 0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(minU, 1.0F).setLight(15728880);
         builder.addVertex(matrix, 1.0F, 1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(minU, 0.0F).setLight(15728880);
         builder.addVertex(matrix, 0.0F, 1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(maxU, 0.0F).setLight(15728880);
         poseStack.popPose();
      }
   }

   private void spawnParticles(Gun.Display.Flash flash, LivingEntity entity) {
      if (entity != null && entity.level().isClientSide()) {
         ClientLevel world = (ClientLevel)entity.level();
         RandomSource random = entity.getRandom();
         double posX = entity.getX();
         double posY = entity.getY() + (double)entity.getEyeHeight();
         double posZ = entity.getZ();
         SimpleParticleType particleType = flash.getParticleType();
         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastParticleSpawnTime >= 100L) {
            this.lastParticleSpawnTime = currentTime;
            double radius = flash.getParticleRingRadius();
            if (radius > 0.0) {
               for (int i = 0; i < flash.getParticleCount(); i++) {
                  double angle = random.nextDouble() * 2.0 * Math.PI;
                  double randomizedRadius = radius * (0.8 + random.nextDouble() * 0.4);
                  double offsetX = Math.cos(angle) * randomizedRadius;
                  double offsetZ = Math.sin(angle) * randomizedRadius;
                  double spread = flash.getParticleSpread();
                  double offsetY = (random.nextDouble() - 0.5) * spread;
                  offsetX += (random.nextDouble() - 0.5) * spread * 0.5;
                  offsetZ += (random.nextDouble() - 0.5) * spread * 0.5;
                  double motionStrength = 0.03 + random.nextDouble() * 0.02;
                  double motionX = offsetX * motionStrength;
                  double motionY = offsetY * motionStrength + 0.01;
                  double motionZ = offsetZ * motionStrength;
                  world.addParticle(particleType, posX + offsetX, posY + offsetY, posZ + offsetZ, motionX, motionY, motionZ);
               }
            } else {
               for (int i = 0; i < flash.getParticleCount(); i++) {
                  double spread = flash.getParticleSpread();
                  double offsetX = (random.nextDouble() - 0.5) * spread;
                  double offsetY = (random.nextDouble() - 0.5) * spread;
                  double offsetZ = (random.nextDouble() - 0.5) * spread;
                  world.addParticle(particleType, posX + offsetX, posY + offsetY, posZ + offsetZ, offsetX * 0.1, offsetY * 0.1, offsetZ * 0.1);
               }
            }
         }
      }
   }

   private void renderReloadArm(PoseStack poseStack, MultiBufferSource buffer, int light, Gun modifiedGun, ItemStack stack, HumanoidArm hand, float translateX) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.player.tickCount >= ReloadHandler.get().getStartReloadTick() && ReloadHandler.get().getReloadTimer() == 5) {
         poseStack.pushPose();
         int side = hand.getOpposite() == HumanoidArm.RIGHT ? 1 : -1;
         poseStack.translate(translateX * (float)side, 0.0F, 0.0F);
         float interval = (float)GunEnchantmentHelper.getRealReloadSpeed(stack);
         float reload = ((float)(mc.player.tickCount - ReloadHandler.get().getStartReloadTick()) + mc.getTimer().getGameTimeDeltaPartialTick(false)) % interval / interval;
         float percent = 1.0F - reload;
         if (percent >= 0.5F) {
            percent = 1.0F - percent;
         }

         percent *= 2.0F;
         percent = (double)percent < 0.5 ? 2.0F * percent * percent : -1.0F + (4.0F - 2.0F * percent) * percent;
         poseStack.translate(0.0, 0.35 * (1.0 - (double)percent), 0.0);
         poseStack.mulPose(Axis.XP.rotationDegrees(30.0F * percent));
         poseStack.popPose();
      }
   }

   private float getEquipProgress(float partialTicks) {
      if (this.equippedProgressMainHandField == null) {
         this.equippedProgressMainHandField = ObfuscationReflectionHelper.findField(ItemInHandRenderer.class, "mainHandHeight");
         this.equippedProgressMainHandField.setAccessible(true);
      }

      if (this.prevEquippedProgressMainHandField == null) {
         this.prevEquippedProgressMainHandField = ObfuscationReflectionHelper.findField(ItemInHandRenderer.class, "oMainHandHeight");
         this.prevEquippedProgressMainHandField.setAccessible(true);
      }

      ItemInHandRenderer firstPersonRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer();

      try {
         float equippedProgressMainHand = (Float)this.equippedProgressMainHandField.get(firstPersonRenderer);
         float prevEquippedProgressMainHand = (Float)this.prevEquippedProgressMainHandField.get(firstPersonRenderer);
         return 1.0F - Mth.lerp(partialTicks, prevEquippedProgressMainHand, equippedProgressMainHand);
      } catch (IllegalAccessException var5) {
         var5.printStackTrace();
         return 0.0F;
      }
   }

   @SubscribeEvent
   public void onCameraSetup(ComputeCameraAngles event) {
      if ((Boolean)Config.CLIENT.display.cameraRollEffect.get()) {
         // ViewportEvent#getPartialTick() already is the render partial tick (a double).
         float roll = (float)Mth.lerp(event.getPartialTick(), (double)this.prevImmersiveRoll, (double)this.immersiveRoll);
         roll = (float)Math.sin((double)roll * Math.PI / 2.0);
         if ((Boolean)Config.CLIENT.display.cinematicGunEffects.get()) {
            roll *= ((Double)Config.CLIENT.display.cameraRollAngle.get()).floatValue() + this.immersiveRoll;
         }

         event.setRoll(-roll);
      }
   }

   public float getThirdPersonMeleeProgress() {
      return this.thirdPersonMeleeProgress;
   }

   public float getSprintTransition(float frameTime) {
      return Mth.lerp(frameTime, (float)this.prevSprintTransition, (float)this.sprintTransition);
   }
}
