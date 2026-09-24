package top.ribs.scguns.client.render.gun.animated;


import top.ribs.scguns.util.NbtHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
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
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Matrix4f;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.RenderUtil;
import top.ribs.scguns.Config;
import top.ribs.scguns.client.GunRenderType;
import top.ribs.scguns.client.SwayType;
import top.ribs.scguns.client.handler.AimingHandler;
import top.ribs.scguns.client.handler.BeamHandler;
import top.ribs.scguns.client.handler.DualWieldShotTracker;
import top.ribs.scguns.client.handler.GunRecoilHandler;
import top.ribs.scguns.client.handler.GunRenderingHandler;
import top.ribs.scguns.client.handler.ShootingHandler;
import top.ribs.scguns.client.render.gun.IOverrideModel;
import top.ribs.scguns.client.render.gun.ModelOverrides;
import top.ribs.scguns.client.screen.AttachmentScreen;
import top.ribs.scguns.client.util.PropertyHelper;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.event.GunFireEvent;
import top.ribs.scguns.init.ModItems;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.animated.AnimatedDualWieldGunItem;
import top.ribs.scguns.item.animated.AnimatedGunItem;
import top.ribs.scguns.item.attachment.IAttachment;
import top.ribs.scguns.item.attachment.IBarrel;
import top.ribs.scguns.item.attachment.impl.Scope;
import top.ribs.scguns.util.GunModifierHelper;

public class AnimatedGunRenderer extends GeoItemRenderer<AnimatedGunItem> implements GeoRenderer<AnimatedGunItem> {
   private static final ResourceLocation custom_path = null;
   private static AnimatedGunRenderer instance;
   private final AttachmentRenderer attachmentRenderer = new AttachmentRenderer(this);
   private MultiBufferSource bufferSource;
   private ItemDisplayContext currentDisplayContext;
   private ItemStack currentRenderStack;
   private float sprintIntensity;
   private float immersiveRoll;
   private float fallSway;
   private float prevFallSway;
   private static final long PARTICLE_COOLDOWN_MS = 100L;
   private long lastParticleSpawnTime = 0L;
   private static final Set<String> ARM_BONE_NAMES = Set.of("left_arm", "right_arm", "fake_left_arm", "fake_right_arm");
   private static final Set<String> MAGAZINE_BONE_NAMES = Set.of("_mag", "magazine");
   private static final Set<Item> CUSTOM_ATTACHMENTS = Set.of(
      (Item)ModItems.SILENCER.get(),
      (Item)ModItems.ADVANCED_SILENCER.get(),
      (Item)ModItems.MUZZLE_BRAKE.get(),
      (Item)ModItems.VERTICAL_GRIP.get(),
      (Item)ModItems.LIGHT_GRIP.get(),
      (Item)ModItems.IRON_BAYONET.get(),
      (Item)ModItems.ANTHRALITE_BAYONET.get(),
      (Item)ModItems.DIAMOND_BAYONET.get(),
      (Item)ModItems.NETHERITE_BAYONET.get(),
      (Item)ModItems.EXTENDED_BARREL.get(),
      (Item)ModItems.WOODEN_STOCK.get(),
      (Item)ModItems.LIGHT_STOCK.get(),
      (Item)ModItems.WEIGHTED_STOCK.get(),
      (Item)ModItems.BUMP_STOCK.get(),
      (Item)ModItems.EXTENDED_MAG.get(),
      (Item)ModItems.SPEED_MAG.get()
   );
   // Only the (name keyed, immutable) bone type lookup is cached. Bone visibility is
   // deliberately recomputed on every render: the working 1.21.1 port does the same,
   // and a cache keyed on the stack goes stale because the gun NBT is mutated in place.
   private static final Map<String, AnimatedGunRenderer.BoneType> BONE_TYPE_CACHE = new HashMap<>();

   public AnimatedGunRenderer(ResourceLocation path) {
      super(new AnimatedGunModel(path));
      instance = this;
   }

   public static AnimatedGunRenderer get() {
      if (instance == null) {
         instance = new AnimatedGunRenderer(custom_path);
      }

      return instance;
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent.Post event) {
      // 0.5.5 also swept the bone visibility cache here; it is gone (see BONE_TYPE_CACHE).
      this.updateImmersiveCamera();
   }

   private void updateImmersiveCamera() {
      this.prevFallSway = this.fallSway;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         ItemStack heldItem = mc.player.getMainHandItem();
         float targetAngle = !(heldItem.getItem() instanceof GunItem) && Config.CLIENT.display.restrictCameraRollToWeapons.get()
            ? 0.0F
            : mc.player.input.leftImpulse;
         float speed = mc.player.input.leftImpulse != 0.0F ? 0.1F : 0.15F;
         this.immersiveRoll = Mth.lerp(speed, this.immersiveRoll, targetAngle);
         float deltaY = (float)Mth.clamp(mc.player.yo - mc.player.getY(), -1.0, 1.0);
         deltaY = (float)((double)deltaY * (1.0 - AimingHandler.get().getNormalisedAdsProgress()));
         deltaY = (float)((double)deltaY * (1.0 - (double)(Mth.abs(mc.player.getXRot()) / 90.0F)));
         this.fallSway = Mth.approach(this.fallSway, deltaY * 60.0F * ((Double)Config.CLIENT.display.swaySensitivity.get()).floatValue(), 10.0F);
         float intensity = mc.player.isSprinting() ? 0.75F : 1.0F;
         this.sprintIntensity = Mth.approach(this.sprintIntensity, intensity, 0.1F);
      }
   }

   @SubscribeEvent
   public void onGunFire(GunFireEvent.Post event) {
      if (event.isClient() && event.getShooter() instanceof Player player) {
         ItemStack heldItem = event.getStack();
         if (heldItem.getItem() instanceof AnimatedDualWieldGunItem) {
            DualWieldShotTracker.get().incrementShotCount(player.getId());
         }
      }
   }

   public void renderByItem(
      ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay
   ) {
      Minecraft client = Minecraft.getInstance();
      // The working 1.21.1 port resolves the effective context from the configured main
      // hand, so a left-handed player gets FIRST_PERSON_LEFT_HAND; 0.5.5 hardcoded the
      // right hand and then mirrored the left hand wrongly.
      this.currentDisplayContext = getEffectiveDisplayContext(stack, transformType, client);
      this.currentRenderStack = stack;
      this.bufferSource = bufferSource;

      Player player = client.player;
      boolean actualFirstPersonHand = isFirstPersonHand(transformType);
      boolean right = isFirstPersonHand(this.currentDisplayContext)
         ? isRightHandDisplay(this.currentDisplayContext)
         : player == null || (client.options.mainHand().get() == HumanoidArm.RIGHT
            ? player.getUsedItemHand() == InteractionHand.MAIN_HAND
            : player.getUsedItemHand() == InteractionHand.OFF_HAND);
      ItemStack overrideModel = ItemStack.EMPTY;
      if (NbtHelper.getTag(stack) != null && NbtHelper.getTag(stack).contains("Model", 10)) {
         overrideModel = top.ribs.scguns.util.NbtHelper.itemFromTag(NbtHelper.getTag(stack).getCompound("Model"));
      }

      LocalPlayer localPlayer = Objects.requireNonNull(client.player);
      BakedModel model = client.getItemRenderer().getModel(overrideModel.isEmpty() ? stack : overrideModel, player.level(), player, 0);
      ItemTransform firstPersonTransform = model.getTransforms().firstPersonRightHand;
      float scaleX = firstPersonTransform.scale.x();
      float scaleY = firstPersonTransform.scale.y();
      float scaleZ = firstPersonTransform.scale.z();
      float translateX = firstPersonTransform.translation.x();
      float translateY = firstPersonTransform.translation.y();
      float translateZ = firstPersonTransform.translation.z();
      if (stack.getItem() instanceof AnimatedGunItem && actualFirstPersonHand) {
         Gun modifiedGun = ((GunItem)stack.getItem()).getModifiedGun(stack);
         if (AimingHandler.get().getNormalisedAdsProgress() > 0.0 && modifiedGun.canAimDownSight()) {
            this.applyAdsTransforms(poseStack, stack, modifiedGun, scaleX, scaleY, scaleZ, translateX, translateY, translateZ, right);
         }

         this.applyBobbingTransforms(poseStack, client.getTimer().getGameTimeDeltaPartialTick(false));
         int offset = right ? 1 : -1;
         this.applyRecoilTransforms(poseStack, stack, modifiedGun);
         this.applyAimingTransforms(poseStack, stack, modifiedGun, translateX, translateY, translateZ, offset);
         this.applySwayTransforms(poseStack, modifiedGun, stack, localPlayer, translateX, translateY, translateZ, client.getTimer().getGameTimeDeltaPartialTick(false));
         if (ShootingHandler.get().isShooting() && !GunModifierHelper.isSilencedFire(stack)) {
            // Same as the working port: the effective context, not a hardcoded right hand.
            this.renderMuzzleFlash(client.player, poseStack, bufferSource, stack, this.currentDisplayContext, client.getTimer().getGameTimeDeltaPartialTick(false));
         }
      }

      int blockLight = this.calculateBlockLight(player, stack);
      if (transformType == ItemDisplayContext.GUI) {
         packedLight = LightTexture.pack(12, 12);
      } else {
         packedLight = LightTexture.pack(
            blockLight, player.level().getBrightness(LightLayer.SKY, BlockPos.containing(player.getEyePosition(client.getTimer().getGameTimeDeltaPartialTick(false))))
         );
      }

      super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
   }

   /**
    * 0.5.5 only remapped {@link ItemDisplayContext#NONE} to a hardcoded right hand. The
    * working 1.21.1 port (and {@code GunRenderingHandler}, which sends the first person
    * render through {@code NONE}) resolve it from the configured main hand instead, so a
    * left-handed player is treated as a left hand.
    */
   private static ItemDisplayContext getEffectiveDisplayContext(ItemStack stack, ItemDisplayContext transformType, Minecraft client) {
      if (stack.getItem() instanceof AnimatedGunItem
         && transformType == ItemDisplayContext.NONE
         && client.options.getCameraType() == CameraType.FIRST_PERSON) {
         return client.options.mainHand().get() == HumanoidArm.LEFT
            ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
      }

      return transformType;
   }

   private static boolean isFirstPersonHand(ItemDisplayContext displayContext) {
      return displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
   }

   private static boolean isRightHandDisplay(ItemDisplayContext displayContext) {
      return displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
   }

   private void applyAdsTransforms(
      PoseStack poseStack,
      ItemStack stack,
      Gun modifiedGun,
      float scaleX,
      float scaleY,
      float scaleZ,
      float translateX,
      float translateY,
      float translateZ,
      boolean right
   ) {
      double xOffset = (double)translateX - 0.5 * (double)scaleX;
      double yOffset = (double)translateY - 0.5 * (double)scaleY;
      double zOffset = (double)translateZ - 0.5 * (double)scaleZ;
      Vec3 gunOrigin = PropertyHelper.getModelOrigin(stack, PropertyHelper.GUN_DEFAULT_ORIGIN);
      xOffset += gunOrigin.x * 0.0625 * (double)scaleX;
      yOffset += gunOrigin.y * 0.0625 * (double)scaleY;
      zOffset += gunOrigin.z * 0.0625 * (double)scaleZ;
      Scope scope = Gun.getScope(stack);
      if (modifiedGun.canAttachType(IAttachment.Type.SCOPE) && scope != null) {
         Vec3 ironSightCamera = PropertyHelper.getAttachmentPosition(stack, modifiedGun, IAttachment.Type.SCOPE).subtract(gunOrigin);
         xOffset += ironSightCamera.x * 0.0625 * (double)scaleX;
         yOffset += ironSightCamera.y * 0.0625 * (double)scaleY;
         zOffset += ironSightCamera.z * 0.0625 * (double)scaleZ;
         ItemStack scopeStack = Gun.getScopeStack(stack);
         Vec3 scopeOrigin = PropertyHelper.getModelOrigin(scopeStack, PropertyHelper.ATTACHMENT_DEFAULT_ORIGIN);
         Vec3 scopeCamera = PropertyHelper.getScopeCamera(scopeStack).subtract(scopeOrigin);
         Vec3 scopeScale = PropertyHelper.getAttachmentScale(stack, modifiedGun, IAttachment.Type.SCOPE);
         xOffset += scopeCamera.x * 0.0625 * (double)scaleX * scopeScale.x;
         yOffset += (scopeCamera.y * 0.0625 * (double)scaleY + 0.54) * scopeScale.y;
         zOffset += (scopeCamera.z * 0.0625 * (double)scaleZ - 0.16) * scopeScale.z;
      } else {
         Vec3 ironSightCamera = PropertyHelper.getIronSightCamera(stack, modifiedGun, gunOrigin).subtract(gunOrigin);
         xOffset += ironSightCamera.x * 0.0625 * (double)scaleX;
         yOffset += ironSightCamera.y * 0.0625 * (double)scaleY + 0.6059;
         zOffset += ironSightCamera.z * 0.0625 * (double)scaleZ - 0.16;
         if (PropertyHelper.isLegacyIronSight(stack)) {
            zOffset += 0.72;
         }
      }

      float side = right ? 1.0F : -1.0F;
      double time = AimingHandler.get().getNormalisedAdsProgress();
      double transition = PropertyHelper.getSightAnimations(stack, modifiedGun).getSightCurve().apply(time);
      poseStack.translate(-0.56 * (double)side * transition, 0.52 * transition, 0.72 * transition);
      poseStack.translate(-xOffset * (double)side * transition, -yOffset * transition, -zOffset * transition);
   }

   private int calculateBlockLight(Player player, ItemStack stack) {
      int blockLight = player.isOnFire()
         ? 15
         : player.level().getBrightness(LightLayer.BLOCK, BlockPos.containing(player.getEyePosition(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false))));
      if (ShootingHandler.get().isShooting() && !GunModifierHelper.isSilencedFire(stack)) {
         blockLight += GunRenderingHandler.entityIdForMuzzleFlash.contains(player.getId()) ? 3 : 0;
      }

      return Math.min(blockLight, 15);
   }

   private boolean shouldRenderArms() {
      if (!(Boolean)Config.CLIENT.display.renderArms.get()) {
         return false;
      } else if (this.currentRenderStack == null) {
         return false;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null) {
            return false;
         } else if (mc.screen instanceof AttachmentScreen) {
            return false;
         } else if (this.currentDisplayContext != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
            && this.currentDisplayContext != ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            return false;
         } else {
            return mc.options.getCameraType() != CameraType.FIRST_PERSON ? false : !mc.player.isSpectator() && mc.player.isAlive();
         }
      }
   }

   private AnimatedGunRenderer.BoneType getBoneType(String boneName) {
      return BONE_TYPE_CACHE.computeIfAbsent(boneName, name -> {
         if (ARM_BONE_NAMES.contains(name)) {
            return AnimatedGunRenderer.BoneType.ARM;
         } else {
            switch (name) {
               case "cogloader_magazine":
                  return AnimatedGunRenderer.BoneType.COGLOADER_MAGAZINE;
               case "cylinder_magazine":
                  return AnimatedGunRenderer.BoneType.CYLINDER_MAGAZINE;
               case "crank_magazine":
                  return AnimatedGunRenderer.BoneType.CRANK_MAGAZINE;
               case "sliding_magazine":
                  return AnimatedGunRenderer.BoneType.SLIDING_MAGAZINE;
               case "laser_origin":
                  return AnimatedGunRenderer.BoneType.LASER_ORIGIN;
               case "seal":
                  return AnimatedGunRenderer.BoneType.SEAL;
               case "attachment_bone":
                  return AnimatedGunRenderer.BoneType.ATTACHMENT;
               case "glow":
               case "glow_2":
               case "glow_3":
               case "glow_4":
               case "glow_5":
               case "glow_6":
                  return AnimatedGunRenderer.BoneType.GLOW;
               default:
                  return MAGAZINE_BONE_NAMES.stream().anyMatch(name::contains) ? AnimatedGunRenderer.BoneType.MAGAZINE : AnimatedGunRenderer.BoneType.REGULAR;
            }
         }
      });
   }

   /**
    * GeckoLib 4.6+ hands this hook a single packed ARGB colour and no longer four floats.
    * Keeping 0.5.5's {@code float red, float green, float blue, float alpha} parameter list
    * silently stopped overriding {@code GeoRenderer#renderRecursively}, so none of the bone
    * logic in this class ran: the gun model drew its own baked-in arm and attachment meshes
    * in every display context (the extra, non skinned arm in the inventory/hand and the
    * attachments that were visible although they were not installed) and the player's own
    * skinned arm was never drawn. The working 1.21.1 port takes the packed colour; so do we.
    */
   @Override
   public void renderRecursively(
      PoseStack poseStack,
      AnimatedGunItem animatable,
      GeoBone bone,
      RenderType renderType,
      MultiBufferSource bufferSource,
      VertexConsumer buffer,
      boolean isReRender,
      float partialTick,
      int packedLight,
      int packedOverlay,
      int renderColor
   ) {
      AnimatedGunRenderer.BoneType boneType = this.getBoneType(bone.getName());
      Minecraft client = Minecraft.getInstance();
      boolean shouldRenderCustomArms = false;
      poseStack.pushPose();
      switch (boneType) {
         case COGLOADER_MAGAZINE:
            this.handleCogloaderMagazine(poseStack, bone, animatable);
            break;
         case CYLINDER_MAGAZINE:
            this.handleCylinderMagazine(poseStack, bone, animatable);
            break;
         case CRANK_MAGAZINE:
            this.handleCrankMagazine(poseStack, bone, animatable);
            break;
         case SLIDING_MAGAZINE:
            this.handleSlidingMagazine(poseStack, bone);
      }

      if (boneType == AnimatedGunRenderer.BoneType.ARM) {
         if (this.currentDisplayContext != ItemDisplayContext.GUI) {
            boolean shouldRender = this.shouldRenderArms();
            bone.setHidden(true);
            bone.setChildrenHidden(true);
            if (shouldRender) {
               shouldRenderCustomArms = true;
            }
         } else {
            bone.setHidden(true);
            bone.setChildrenHidden(true);
         }
      } else if (boneType == AnimatedGunRenderer.BoneType.SEAL) {
         boolean hidden = !(Boolean)Config.CLIENT.display.puritySeals.get();
         bone.setHidden(hidden);
         bone.setChildrenHidden(hidden);
      } else {
         this.handleBoneVisibility(bone);
      }

      if (shouldRenderCustomArms && client.player != null) {
         this.renderPlayerArms(client, poseStack, bone, animatable, packedLight, packedOverlay);
      }

      if (boneType == AnimatedGunRenderer.BoneType.ATTACHMENT) {
         this.renderAttachments(bone, this.currentRenderStack, poseStack, renderType, buffer, packedLight, client.getTimer().getGameTimeDeltaPartialTick(false), packedOverlay);
      }

      if (boneType == AnimatedGunRenderer.BoneType.GLOW) {
         packedLight = 15728880;
      }

      super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, this.bufferSource.getBuffer(renderType), isReRender, partialTick, packedLight, packedOverlay, renderColor);
      poseStack.popPose();
   }

   private void handleCogloaderMagazine(PoseStack poseStack, GeoBone bone, AnimatedGunItem animatable) {
      float pivotX = bone.getPivotX() * 0.0625F;
      float pivotY = bone.getPivotY() * 0.0625F;
      float pivotZ = bone.getPivotZ() * 0.0625F;
      poseStack.translate(pivotX, pivotY, pivotZ);
      poseStack.mulPose(Axis.YP.rotationDegrees(animatable.getRotationHandler().getCurrentMagazineRotation()));
      poseStack.translate(-pivotX, -pivotY, -pivotZ);
   }

   private void handleCylinderMagazine(PoseStack poseStack, GeoBone bone, AnimatedGunItem animatable) {
      float pivotX = bone.getPivotX() * 0.0625F;
      float pivotY = bone.getPivotY() * 0.0625F;
      float pivotZ = bone.getPivotZ() * 0.0625F;
      poseStack.translate(pivotX, pivotY, pivotZ);
      poseStack.mulPose(Axis.ZP.rotationDegrees(animatable.getRotationHandler().getCurrentCylinderRotation()));
      poseStack.translate(-pivotX, -pivotY, -pivotZ);
   }

   private void handleCrankMagazine(PoseStack poseStack, GeoBone bone, AnimatedGunItem animatable) {
      float pivotX = bone.getPivotX() * 0.0625F;
      float pivotY = bone.getPivotY() * 0.0625F;
      float pivotZ = bone.getPivotZ() * 0.0625F;
      poseStack.translate(pivotX, pivotY, pivotZ);
      poseStack.mulPose(Axis.XP.rotationDegrees(animatable.getRotationHandler().getCurrentCylinderRotation()));
      poseStack.translate(-pivotX, -pivotY, -pivotZ);
   }

   private void handleSlidingMagazine(PoseStack poseStack, GeoBone bone) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || !(Boolean)ModSyncedDataKeys.RELOADING.getValue(mc.player)) {
         float maxAmmo = (float)Gun.getMaxAmmo(this.currentRenderStack);
         float currentAmmo = (float)Gun.getAmmoCount(this.currentRenderStack);
         if (maxAmmo > 0.0F) {
            float slidePosition = Math.min((maxAmmo - currentAmmo) / maxAmmo, 1.0F);
            poseStack.translate(slidePosition * 0.25F, 0.0F, 0.0F);
         }
      }
   }

   private void handleBoneVisibility(GeoBone bone) {
      // The working 1.21.1 port recomputes this on every render. 0.5.5 cached it per
      // bone name + stack hash, and since the stack hash is the identity hash while the
      // gun NBT is mutated in place, a removed attachment's mesh (and every other
      // attachment bone) kept the visibility it had when the cache entry was written.
      bone.setHidden(this.calculateBoneVisibility(bone.getName()));
   }

   private boolean calculateBoneVisibility(String boneName) {
      return switch (boneName) {
         case "sights" -> Gun.getScope(this.currentItemStack) != null;
         case "no_sights" -> Gun.getScope(this.currentItemStack) == null;
         case "wooden_stock" -> Gun.getAttachment(IAttachment.Type.STOCK, this.currentItemStack).getItem() != ModItems.WOODEN_STOCK.get();
         case "light_stock" -> Gun.getAttachment(IAttachment.Type.STOCK, this.currentItemStack).getItem() != ModItems.LIGHT_STOCK.get();
         case "weighted_stock" -> {
            ItemStack stockAttachment = Gun.getAttachment(IAttachment.Type.STOCK, this.currentItemStack);
            yield stockAttachment.getItem() != ModItems.WEIGHTED_STOCK.get() && stockAttachment.getItem() != ModItems.BUMP_STOCK.get();
         }
         case "standard_stock", "standard_grip" -> !Gun.getAttachment(IAttachment.Type.STOCK, this.currentItemStack).isEmpty();
         case "standard_barrel", "standard_barrel1", "extended_barrel", "extended_barrel1", "silencer", "silencer1", "advanced_silencer", "advanced_silencer1", "muzzle_brake", "muzzle_brake1" -> this.handleBarrelVisibility(
         boneName
      );
         default -> !boneName.contains("grip") && !boneName.contains("bayonet")
         ? (boneName.contains("mag") ? this.handleMagazineVisibility(boneName) : false)
         : this.handleUnderBarrelVisibility(boneName);
      };
   }

   private boolean handleBarrelVisibility(String boneName) {
      ItemStack barrelAttachment = Gun.getAttachment(IAttachment.Type.BARREL, this.currentItemStack);

      return switch (boneName) {
         case "standard_barrel", "standard_barrel1" -> barrelAttachment.getItem() == ModItems.EXTENDED_BARREL.get();
         case "extended_barrel", "extended_barrel1" -> barrelAttachment.getItem() != ModItems.EXTENDED_BARREL.get();
         case "silencer", "silencer1" -> barrelAttachment.getItem() != ModItems.SILENCER.get();
         case "advanced_silencer", "advanced_silencer1" -> barrelAttachment.getItem() != ModItems.ADVANCED_SILENCER.get();
         case "muzzle_brake", "muzzle_brake1" -> barrelAttachment.getItem() != ModItems.MUZZLE_BRAKE.get();
         default -> false;
      };
   }

   private boolean handleUnderBarrelVisibility(String boneName) {
      ItemStack underBarrelAttachment = Gun.getAttachment(IAttachment.Type.UNDER_BARREL, this.currentItemStack);
      Item attachmentItem = underBarrelAttachment.getItem();

      return switch (boneName) {
         case "light_grip" -> attachmentItem != ModItems.LIGHT_GRIP.get();
         case "vertical_grip" -> attachmentItem != ModItems.VERTICAL_GRIP.get();
         case "iron_bayonet" -> attachmentItem != ModItems.IRON_BAYONET.get();
         case "anthralite_bayonet" -> attachmentItem != ModItems.ANTHRALITE_BAYONET.get();
         case "diamond_bayonet" -> attachmentItem != ModItems.DIAMOND_BAYONET.get();
         case "netherite_bayonet" -> attachmentItem != ModItems.NETHERITE_BAYONET.get();
         default -> false;
      };
   }

   private boolean handleMagazineVisibility(String boneName) {
      ItemStack magazineAttachment = Gun.getAttachment(IAttachment.Type.MAGAZINE, this.currentItemStack);

      return switch (boneName) {
         case "standard_mag", "default_mag", "standard_mag_2", "default_mag_2" -> !magazineAttachment.isEmpty();
         case "extended_mag", "extended_mag_2" -> magazineAttachment.getItem() != ModItems.EXTENDED_MAG.get();
         case "speed_mag", "speed_mag_2" -> magazineAttachment.getItem() != ModItems.SPEED_MAG.get();
         default -> false;
      };
   }

   private void renderPlayerArms(Minecraft client, PoseStack poseStack, GeoBone bone, AnimatedGunItem animatable, int packedLight, int packedOverlay) {
      if (client.player != null) {
         PlayerRenderer playerEntityRenderer = (PlayerRenderer)client.getEntityRenderDispatcher().getRenderer(client.player);
         PlayerModel<AbstractClientPlayer> playerEntityModel = (PlayerModel<AbstractClientPlayer>)playerEntityRenderer.getModel();
         this.setupArmTransforms(poseStack, bone);
         // Same as the working 1.21.1 port: hand the skin down and let each arm resolve
         // its own RenderType from it, instead of pre-building buffers up here.
         ResourceLocation playerSkin = client.player.getSkin().texture();
         if (this.isRightArm(bone)) {
            this.renderRightArm(poseStack, playerEntityModel, playerSkin, bone, packedLight, packedOverlay);
         } else {
            this.renderLeftArm(poseStack, playerEntityModel, playerSkin, bone, packedLight, packedOverlay, client.player, animatable);
         }
      }
   }

   private void setupArmTransforms(PoseStack poseStack, GeoBone bone) {
      RenderUtil.translateMatrixToBone(poseStack, bone);
      RenderUtil.translateToPivotPoint(poseStack, bone);
      RenderUtil.rotateMatrixAroundBone(poseStack, bone);
      RenderUtil.scaleMatrixForBone(poseStack, bone);
      RenderUtil.translateAwayFromPivotPoint(poseStack, bone);
   }

   private boolean isRightArm(GeoBone bone) {
      String boneName = bone.getName();
      return !boneName.equals("fake_left_arm") && (boneName.equals("right_arm") || boneName.equals("fake_right_arm"));
   }

   private void renderRightArm(
      PoseStack poseStack,
      PlayerModel<AbstractClientPlayer> playerEntityModel,
      ResourceLocation playerSkin,
      GeoBone bone,
      int packedLight,
      int packedOverlay
   ) {
      poseStack.scale(0.66F, 0.78F, 0.66F);
      poseStack.translate(0.25, -0.1, 0.1625);
      playerEntityModel.rightArm.setPos(bone.getPivotX(), bone.getPivotY(), bone.getPivotZ());
      playerEntityModel.rightArm.setRotation(0.0F, 0.0F, 0.0F);
      playerEntityModel.rightArm.render(poseStack, this.bufferSource.getBuffer(RenderType.entitySolid(playerSkin)), packedLight, packedOverlay);
      playerEntityModel.rightSleeve.setPos(bone.getPivotX(), bone.getPivotY(), bone.getPivotZ());
      playerEntityModel.rightSleeve.setRotation(0.0F, 0.0F, 0.0F);
      playerEntityModel.rightSleeve.render(poseStack, this.bufferSource.getBuffer(RenderType.entityTranslucent(playerSkin)), packedLight, packedOverlay);
   }

   private void renderLeftArm(
      PoseStack poseStack,
      PlayerModel<AbstractClientPlayer> playerEntityModel,
      ResourceLocation playerSkin,
      GeoBone bone,
      int packedLight,
      int packedOverlay,
      Player player,
      AnimatedGunItem animatable
   ) {
      Gun modifiedGun = ((GunItem)this.currentItemStack.getItem()).getModifiedGun(this.currentItemStack);
      modifiedGun.determineGripType(this.currentItemStack);
      long id = GeoItem.getId(player.getMainHandItem());
      AnimationController<GeoAnimatable> animationController = (AnimationController<GeoAnimatable>)animatable.getAnimatableInstanceCache()
         .getManagerForId(id)
         .getAnimationControllers()
         .get("controller");
      this.checkAndHandleAnimations(animationController);
      poseStack.scale(0.66F, 0.79F, 0.66F);
      poseStack.translate(-0.25, -0.1, 0.1625);
      playerEntityModel.leftArm.setPos(bone.getPivotX(), bone.getPivotY(), bone.getPivotZ());
      playerEntityModel.leftArm.setRotation(0.0F, 0.0F, 0.0F);
      playerEntityModel.leftArm.render(poseStack, this.bufferSource.getBuffer(RenderType.entitySolid(playerSkin)), packedLight, packedOverlay);
      playerEntityModel.leftSleeve.setPos(bone.getPivotX(), bone.getPivotY(), bone.getPivotZ());
      playerEntityModel.leftSleeve.setRotation(0.0F, 0.0F, 0.0F);
      playerEntityModel.leftSleeve.render(poseStack, this.bufferSource.getBuffer(RenderType.entityTranslucent(playerSkin)), packedLight, packedOverlay);
   }

   private void checkAndHandleAnimations(AnimationController<GeoAnimatable> animationController) {
      if (animationController != null && animationController.getCurrentAnimation() != null) {
         String animName = animationController.getCurrentAnimation().animation().name();
         if (!animName.equals("draw") && !animName.equals("reload") && !animName.equals("reload_start") && !animName.equals("reload_loop")) {
            animationController.getCurrentAnimation();
         }
      }
   }

   private void applyRecoilTransforms(PoseStack poseStack, ItemStack item, Gun gun) {
      double recoilNormal = GunRecoilHandler.get().getGunRecoilNormal();
      if (Gun.hasAttachmentEquipped(item, gun, IAttachment.Type.SCOPE)) {
         recoilNormal -= recoilNormal * 0.5 * AimingHandler.get().getNormalisedAdsProgress();
      }

      Minecraft mc = Minecraft.getInstance();
      float kickReduction;
      float recoilReduction;
      if (mc.player != null) {
         kickReduction = 1.0F - GunModifierHelper.getKickReduction(mc.player, item);
         recoilReduction = 1.0F - GunModifierHelper.getRecoilModifier(mc.player, item);
      } else {
         kickReduction = 1.0F - GunModifierHelper.getKickReduction(item);
         recoilReduction = 1.0F - GunModifierHelper.getRecoilModifier(item);
      }

      double kick = (double)gun.getProjectile().getRecoilKick() * 0.0625 * recoilNormal * GunRecoilHandler.get().getAdsRecoilReduction(gun);
      float recoilLift = (float)((double)gun.getProjectile().getRecoilAngle() * recoilNormal) * (float)GunRecoilHandler.get().getAdsRecoilReduction(gun);
      float recoilSwayAmount = (float)(2.0 + (1.0 - AimingHandler.get().getNormalisedAdsProgress()));
      float recoilSway = (float)((double)(GunRecoilHandler.get().getGunRecoilRandom() * recoilSwayAmount - recoilSwayAmount / 2.0F) * recoilNormal);
      poseStack.translate(0.0, 0.0, kick * (double)kickReduction);
      poseStack.translate(0.0, 0.0, 0.15);
      poseStack.mulPose(Axis.YP.rotationDegrees(recoilSway * recoilReduction / 5.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(recoilSway * recoilReduction / 5.0F));
      poseStack.mulPose(Axis.XP.rotationDegrees(recoilLift * recoilReduction / 5.0F));
      poseStack.translate(0.0, 0.0, -0.15);
   }

   private void applyBobbingTransforms(PoseStack poseStack, float partialTicks) {
      Minecraft mc = Minecraft.getInstance();
      if ((Boolean)mc.options.bobView().get()) {
         if (mc.getCameraEntity() instanceof Player player) {
            float deltaDistanceWalked = player.walkDist - player.walkDistO;
            float distanceWalked = -(player.walkDist + deltaDistanceWalked * partialTicks);
            float bobbing = Mth.lerp(partialTicks, player.oBob, player.bob);
            poseStack.mulPose(Axis.XP.rotationDegrees(-(Math.abs(Mth.cos(distanceWalked * (float) Math.PI - 0.2F) * bobbing) * 5.0F)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-(Mth.sin(distanceWalked * (float) Math.PI) * bobbing * 3.0F)));
            poseStack.translate(
               (double)(-(Mth.sin(distanceWalked * (float) Math.PI) * bobbing * 0.5F)),
               (double)Math.abs(Mth.cos(distanceWalked * (float) Math.PI) * bobbing),
               0.0
            );
            bobbing *= (float)(player.isSprinting() ? 8.0 : 4.0);
            bobbing *= ((Double)Config.CLIENT.display.bobbingIntensity.get()).floatValue();
            double invertZoomProgress = 1.0 - AimingHandler.get().getNormalisedAdsProgress() * (double)this.sprintIntensity;
            if (!AimingHandler.get().isAiming()) {
               poseStack.mulPose(
                  Axis.XP.rotationDegrees(Math.abs(Mth.cos(distanceWalked * (float) Math.PI - 0.2F) * bobbing) * -2.0F * (float)invertZoomProgress)
               );
               poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(distanceWalked * (float) Math.PI) * bobbing * 3.0F * (float)invertZoomProgress));
            }
         }
      }
   }

   private void applyAimingTransforms(PoseStack poseStack, ItemStack stack, Gun modifiedGun, float x, float y, float z, int offset) {
      poseStack.translate(x * (float)offset, y, z);
      poseStack.translate(0.0, -0.25, 0.25);
      float aiming = (float)Math.sin(Math.toRadians(AimingHandler.get().getNormalisedAdsProgress() * 180.0));
      aiming = PropertyHelper.getSightAnimations(stack, modifiedGun).getAimTransformCurve().apply(aiming / 2.0F);
      poseStack.mulPose(Axis.ZP.rotationDegrees(aiming * 10.0F * (float)offset));
      poseStack.mulPose(Axis.XP.rotationDegrees(aiming * 8.0F));
      poseStack.mulPose(Axis.YP.rotationDegrees(aiming * 8.0F * (float)offset));
      poseStack.translate(0.0, 0.25, -0.25);
      poseStack.translate(-x * (float)offset, -y, -z);
   }

   private void applySwayTransforms(PoseStack poseStack, Gun modifiedGun, ItemStack stack, LocalPlayer player, float x, float y, float z, float partialTicks) {
      if ((Boolean)Config.CLIENT.display.weaponSway.get() && player != null) {
         poseStack.pushPose();
         poseStack.translate(x, y, z);
         double zOffset = modifiedGun.determineGripType(stack).heldAnimation().getFallSwayZOffset();
         poseStack.translate(0.0, -0.25, zOffset);
         poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, this.prevFallSway, this.fallSway)));
         poseStack.translate(0.0, 0.25, -zOffset);
         float bobPitch = Mth.rotLerp(partialTicks, player.xBobO, player.xBob);
         float headPitch = Mth.rotLerp(partialTicks, player.xRotO, player.getXRot());
         float swayPitch = headPitch - bobPitch;
         float adsProgress = (float)AimingHandler.get().getNormalisedAdsProgress();
         swayPitch *= 1.0F - 0.5F * adsProgress;
         float pitchSensitivity = ((Double)Config.CLIENT.display.swaySensitivity.get()).floatValue();
         poseStack.mulPose(((SwayType)Config.CLIENT.display.swayType.get()).getPitchRotation().rotationDegrees(swayPitch * pitchSensitivity));
         float bobYaw = Mth.rotLerp(partialTicks, player.yBobO, player.yBob);
         float headYaw = Mth.rotLerp(partialTicks, player.yHeadRotO, player.yHeadRot);
         float swayYaw = headYaw - bobYaw;
         swayYaw *= 1.0F - 0.5F * adsProgress;
         float yawSensitivity = ((Double)Config.CLIENT.display.swaySensitivity.get()).floatValue();
         poseStack.mulPose(((SwayType)Config.CLIENT.display.swayType.get()).getYawRotation().rotationDegrees(swayYaw * yawSensitivity));
         poseStack.translate(-x, -y, -z);
         poseStack.popPose();
      }
   }

   private void renderAttachments(
      GeoBone bone,
      ItemStack stack,
      PoseStack poseStack,
      RenderType renderType,
      VertexConsumer renderTypeBuffer,
      int light,
      float partialTicks,
      int packedOverlay
   ) {
      if (stack.getItem() instanceof GunItem) {
         Gun modifiedGun = ((GunItem)stack.getItem()).getModifiedGun(stack);
         CompoundTag gunTag = NbtHelper.getOrCreateTag(stack);
         CompoundTag attachments = gunTag.getCompound("Attachments");

         for (String tagKey : attachments.getAllKeys()) {
            IAttachment.Type type = IAttachment.Type.byTagKey(tagKey);
            if (type != null && modifiedGun.canAttachType(type)) {
               ItemStack attachmentStack = Gun.getAttachment(type, stack);
               if (!CUSTOM_ATTACHMENTS.contains(attachmentStack.getItem()) && !attachmentStack.isEmpty()) {
                  this.renderSingleAttachment(
                     bone, stack, attachmentStack, type, modifiedGun, poseStack, renderType, renderTypeBuffer, light, partialTicks, packedOverlay
                  );
               }
            }
         }
      }
   }

   private void renderSingleAttachment(
      GeoBone bone,
      ItemStack stack,
      ItemStack attachmentStack,
      IAttachment.Type type,
      Gun modifiedGun,
      PoseStack poseStack,
      RenderType renderType,
      VertexConsumer renderTypeBuffer,
      int light,
      float partialTicks,
      int packedOverlay
   ) {
      poseStack.pushPose();
      Vec3 origin = PropertyHelper.getModelOrigin(attachmentStack, PropertyHelper.ATTACHMENT_DEFAULT_ORIGIN);
      Vec3 gunOrigin = PropertyHelper.getModelOrigin(stack, PropertyHelper.GUN_DEFAULT_ORIGIN);
      Vec3 translation = PropertyHelper.getAttachmentPosition(stack, modifiedGun, type).subtract(gunOrigin);
      Vec3 scale = PropertyHelper.getAttachmentScale(stack, modifiedGun, type);
      Vec3 center = origin.subtract(8.0, 8.0, 8.0).scale(0.0625);
      poseStack.translate(-origin.x * 0.0625, -origin.y * 0.0625, -origin.z * 0.0625);
      poseStack.translate(gunOrigin.x * 0.0625, gunOrigin.y * 0.0625, gunOrigin.z * 0.0625);
      poseStack.translate(translation.x * 0.0625, translation.y * 0.0625, translation.z * 0.0625);
      poseStack.translate(center.x, center.y, center.z);
      poseStack.scale((float)scale.x, (float)scale.y, (float)scale.z);
      poseStack.translate(-center.x, -center.y, -center.z);
      IOverrideModel overrideModel = ModelOverrides.getModel(attachmentStack);
      if (overrideModel != null) {
         overrideModel.render(
            partialTicks,
            ItemDisplayContext.NONE,
            attachmentStack,
            stack,
            Minecraft.getInstance().player,
            poseStack,
            this.bufferSource,
            light,
            OverlayTexture.NO_OVERLAY
         );
      } else {
         this.attachmentRenderer.updateAttachment(attachmentStack);
         this.attachmentRenderer
            .renderForBone(
               poseStack, (AnimatedGunItem)this.animatable, bone, renderType, this.bufferSource, renderTypeBuffer, partialTicks, light, packedOverlay
            );
      }

      poseStack.popPose();
   }

   private void renderMuzzleFlash(
      @Nullable LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer, ItemStack weapon, ItemDisplayContext display, float partialTicks
   ) {
      Gun modifiedGun = ((GunItem)weapon.getItem()).getModifiedGun(weapon);
      Gun.Display.Flash flash = modifiedGun.getDisplay().getFlash();
      if (flash != null) {
         if (display == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
            || display == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
            || display == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            || display == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            if (entity != null) {
               boolean isBeamActive = BeamHandler.activeBeams.containsKey(entity.getUUID());
               if (isBeamActive || GunRenderingHandler.entityIdForMuzzleFlash.contains(entity.getId())) {
                  float randomValue = GunRenderingHandler.entityIdToRandomValue.getOrDefault(entity.getId(), 0.0F);
                  boolean mirror = DualWieldShotTracker.get().shouldUseAlternateAnimation(entity.getId()) && flash.hasAlternateMuzzleFlash();
                  ResourceLocation flashTexture = ResourceLocation.fromNamespaceAndPath("scguns", "textures/effect/" + flash.getTextureLocation() + ".png");
                  this.drawMuzzleFlash(weapon, modifiedGun, randomValue, mirror, poseStack, buffer, partialTicks, flashTexture, entity);
                  if (flash.shouldSpawnParticles() && flash.getParticleType() != null) {
                     this.spawnParticles(flash, entity);
                  }
               }
            }
         }
      }
   }

   private void spawnParticles(Gun.Display.Flash flash, LivingEntity entity) {
      if (entity != null && entity.level().isClientSide()) {
         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastParticleSpawnTime >= 100L) {
            this.lastParticleSpawnTime = currentTime;
            ClientLevel world = (ClientLevel)entity.level();
            RandomSource random = entity.getRandom();
            double posX = entity.getX();
            double posY = entity.getY() + (double)entity.getEyeHeight();
            double posZ = entity.getZ();
            SimpleParticleType particleType = flash.getParticleType();
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
            poseStack.pushPose();
            int shotCount = DualWieldShotTracker.get().getShotCount(entity.getId());
            Vec3 weaponOrigin = PropertyHelper.getModelOrigin(weapon, PropertyHelper.GUN_DEFAULT_ORIGIN);
            Vec3 flashPosition = PropertyHelper.getMuzzleFlashPosition(weapon, modifiedGun).subtract(weaponOrigin);
            if (shotCount % 2 == 1 && flash.hasAlternateMuzzleFlash()) {
               Vec3 alternatePos = flash.getAlternatePosition();
               flashPosition = flashPosition.add(alternatePos.x * 0.0625, alternatePos.y * 0.0625, alternatePos.z * 0.0625);
            }

            poseStack.translate(weaponOrigin.x * 0.0625, weaponOrigin.y * 0.0625, weaponOrigin.z * 0.0625);
            poseStack.translate(flashPosition.x * 0.0625 + 0.575, flashPosition.y * 0.0625 + 1.08, flashPosition.z * 0.0625);
            if (AimingHandler.get().isAiming()) {
               poseStack.translate(-0.075, 0.0, 0.0);
            }

            poseStack.translate(-0.5, -0.5, -0.5);
            ItemStack barrelStack = Gun.getAttachment(IAttachment.Type.BARREL, weapon);
            if (!barrelStack.isEmpty() && barrelStack.getItem() instanceof IBarrel barrel && !PropertyHelper.isUsingBarrelMuzzleFlash(barrelStack)) {
               Vec3 scale = PropertyHelper.getAttachmentScale(weapon, modifiedGun, IAttachment.Type.BARREL);
               double length = (double)barrel.getProperties().getLength();
               poseStack.translate(0.0, 0.0, -length * 0.0625 * scale.z);
            }

            poseStack.mulPose(Axis.ZP.rotationDegrees(360.0F * random));
            if (mirror) {
               poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            }

            Vec3 flashScale = PropertyHelper.getMuzzleFlashScale(weapon, modifiedGun);
            float scaleX = (float)flashScale.x / 2.0F - (float)flashScale.x / 2.0F * (1.0F - partialTicks);
            float scaleY = (float)flashScale.y / 2.0F - (float)flashScale.y / 2.0F * (1.0F - partialTicks);
            poseStack.scale(scaleX * 2.0F, scaleY * 2.0F, 1.0F);
            float scaleModifier = (float)GunModifierHelper.getMuzzleFlashScale(weapon, 1.0);
            poseStack.scale(scaleModifier, scaleModifier, 1.0F);
            poseStack.translate(-0.5, -0.5, 0.0);
            this.renderFlashQuad(poseStack, buffer, weapon, flashTexture);
            poseStack.popPose();
         }
      }
   }

   private void renderFlashQuad(PoseStack poseStack, MultiBufferSource buffer, ItemStack weapon, ResourceLocation flashTexture) {
      float minU = weapon.isEnchanted() ? 0.5F : 0.0F;
      float maxU = weapon.isEnchanted() ? 1.0F : 0.5F;
      Matrix4f matrix = poseStack.last().pose();
      VertexConsumer builder = buffer.getBuffer(GunRenderType.getMuzzleFlash(flashTexture));
      builder.addVertex(matrix, 0.0F, 0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(maxU, 1.0F).setLight(15728880);
      builder.addVertex(matrix, 1.0F, 0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(minU, 1.0F).setLight(15728880);
      builder.addVertex(matrix, 1.0F, 1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(minU, 0.0F).setLight(15728880);
      builder.addVertex(matrix, 0.0F, 1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(maxU, 0.0F).setLight(15728880);
   }

   static enum BoneType {
      ARM,
      MAGAZINE,
      ATTACHMENT,
      COGLOADER_MAGAZINE,
      CYLINDER_MAGAZINE,
      CRANK_MAGAZINE,
      SLIDING_MAGAZINE,
      LASER_ORIGIN,
      SEAL,
      GLOW,
      REGULAR;

      private BoneType() {
      }
   }
}
