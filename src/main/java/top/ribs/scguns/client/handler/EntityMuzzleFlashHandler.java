package top.ribs.scguns.client.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLivingEvent.Post;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Matrix4f;
import top.ribs.scguns.client.GunRenderType;
import top.ribs.scguns.client.util.PropertyHelper;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.util.GunModifierHelper;

public class EntityMuzzleFlashHandler {
   public EntityMuzzleFlashHandler() {
      super();
   }

   @SubscribeEvent
   public void onRenderLivingPost(Post<?, ?> event) {
      LivingEntity entity = event.getEntity();
      if (entity instanceof Mob) {
         ItemStack heldItem = entity.getMainHandItem();
         if (heldItem.getItem() instanceof GunItem gunItem) {
            if (GunRenderingHandler.entityIdForMuzzleFlash.contains(entity.getId())) {
               Gun modifiedGun = gunItem.getModifiedGun(heldItem);
               Gun.Display.Flash flash = modifiedGun.getDisplay().getFlash();
               if (flash != null) {
                  PoseStack poseStack = event.getPoseStack();
                  MultiBufferSource buffer = event.getMultiBufferSource();
                  poseStack.pushPose();
                  float randomValue = GunRenderingHandler.entityIdToRandomValue.getOrDefault(entity.getId(), 0.0F);
                  ResourceLocation flashTexture = ResourceLocation.fromNamespaceAndPath("scguns", "textures/effect/" + flash.getTextureLocation() + ".png");
                  this.renderEntityMuzzleFlash(heldItem, modifiedGun, randomValue, poseStack, buffer, event.getPackedLight(), flashTexture, entity);
                  poseStack.popPose();
               }
            }
         }
      }
   }

   private void renderEntityMuzzleFlash(
      ItemStack weapon,
      Gun modifiedGun,
      float random,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      ResourceLocation flashTexture,
      LivingEntity entity
   ) {
      if (PropertyHelper.hasMuzzleFlash(weapon, modifiedGun)) {
         Gun.Display.Flash flash = modifiedGun.getDisplay().getFlash();
         if (flash != null) {
            poseStack.pushPose();
            double eyeHeight = (double)entity.getEyeHeight();
            float entityScale = entity.getBbHeight() / 1.8F;
            poseStack.translate(0.0, eyeHeight - 0.2, 0.0);
            float bodyYaw = entity.yBodyRot;
            float headPitch = entity.getXRot();
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw + 180.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-headPitch));
            float baseForwardOffset = -1.0F * entityScale;
            float baseSideOffset = 0.8F * entityScale;
            float baseVerticalOffset = -0.1F * entityScale;
            poseStack.translate(baseSideOffset, baseVerticalOffset, baseForwardOffset);
            Vec3 weaponOrigin = PropertyHelper.getModelOrigin(weapon, PropertyHelper.GUN_DEFAULT_ORIGIN);
            Vec3 flashPosition = PropertyHelper.getMuzzleFlashPosition(weapon, modifiedGun).subtract(weaponOrigin);
            poseStack.translate(flashPosition.x * 0.0625, flashPosition.y * 0.0625, flashPosition.z * 0.0625);
            poseStack.translate(-0.5, -0.5, 0.0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(360.0F * random));
            Vec3 flashScale = PropertyHelper.getMuzzleFlashScale(weapon, modifiedGun);
            float scaleX = (float)flashScale.x;
            float scaleY = (float)flashScale.y;
            poseStack.scale(scaleX, scaleY, 1.0F);
            float scaleModifier = (float)GunModifierHelper.getMuzzleFlashScale(weapon, 1.0);
            poseStack.scale(scaleModifier, scaleModifier, 1.0F);
            poseStack.translate(-0.5, -0.5, 0.0);
            float minU = weapon.isEnchanted() ? 0.5F : 0.0F;
            float maxU = weapon.isEnchanted() ? 1.0F : 0.5F;
            Matrix4f matrix = poseStack.last().pose();
            VertexConsumer builder = buffer.getBuffer(GunRenderType.getMuzzleFlash(flashTexture));
            builder.addVertex(matrix, 0.0F, 0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(maxU, 1.0F).setLight(15728880);
            builder.addVertex(matrix, 1.0F, 0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(minU, 1.0F).setLight(15728880);
            builder.addVertex(matrix, 1.0F, 1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(minU, 0.0F).setLight(15728880);
            builder.addVertex(matrix, 0.0F, 1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(maxU, 0.0F).setLight(15728880);
            poseStack.popPose();
         }
      }
   }
}
