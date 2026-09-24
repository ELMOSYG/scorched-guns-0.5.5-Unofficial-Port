package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import top.ribs.scguns.entity.projectile.TraumaHookEntity;

public class TraumaHookRenderer extends EntityRenderer<TraumaHookEntity> {
   private static final ResourceLocation TEXTURE = ResourceLocation.parse("textures/entity/fishing_hook.png");

   public TraumaHookRenderer(Context context) {
      super(context);
   }

   public void render(TraumaHookEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      Entity owner = entity.getOwner();
      if (owner != null) {
         this.renderFishingLine(entity, owner, partialTicks, poseStack, buffer, packedLight);
      }

      this.renderHook(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   private void renderHook(TraumaHookEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      poseStack.scale(0.5F, 0.5F, 0.5F);
      poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      Pose pose = poseStack.last();
      VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
      vertex(vertexConsumer, pose, packedLight, 0.0F, 0, 0, 1);
      vertex(vertexConsumer, pose, packedLight, 1.0F, 0, 1, 1);
      vertex(vertexConsumer, pose, packedLight, 1.0F, 1, 1, 0);
      vertex(vertexConsumer, pose, packedLight, 0.0F, 1, 0, 0);
      poseStack.popPose();
   }

   private void renderFishingLine(TraumaHookEntity hook, Entity owner, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      double ownerX = Mth.lerp((double)partialTicks, owner.xo, owner.getX());
      double ownerY = Mth.lerp((double)partialTicks, owner.yo, owner.getY()) + (double)owner.getEyeHeight() * 0.8;
      double ownerZ = Mth.lerp((double)partialTicks, owner.zo, owner.getZ());
      double hookX = Mth.lerp((double)partialTicks, hook.xo, hook.getX());
      double hookY = Mth.lerp((double)partialTicks, hook.yo, hook.getY()) + 0.25;
      double hookZ = Mth.lerp((double)partialTicks, hook.zo, hook.getZ());
      float deltaX = (float)(ownerX - hookX);
      float deltaY = (float)(ownerY - hookY);
      float deltaZ = (float)(ownerZ - hookZ);
      VertexConsumer lineConsumer = buffer.getBuffer(RenderType.lineStrip());
      Pose pose = poseStack.last();
      int segments = 16;

      for (int i = 0; i <= segments; i++) {
         renderLineSegment(deltaX, deltaY, deltaZ, lineConsumer, pose, fraction(i, segments), fraction(i + 1, segments));
      }

      poseStack.popPose();
   }

   private static float fraction(int numerator, int denominator) {
      return (float)numerator / (float)denominator;
   }

   private static void renderLineSegment(float deltaX, float deltaY, float deltaZ, VertexConsumer consumer, Pose pose, float start, float end) {
      float x = deltaX * start;
      float y = deltaY * (start * start + start) * 0.5F + 0.25F;
      float z = deltaZ * start;
      float nextX = deltaX * end - x;
      float nextY = deltaY * (end * end + end) * 0.5F + 0.25F - y;
      float nextZ = deltaZ * end - z;
      float length = Mth.sqrt(nextX * nextX + nextY * nextY + nextZ * nextZ);
      nextX /= length;
      nextY /= length;
      nextZ /= length;
      consumer.addVertex(pose.pose(), x, y, z).setColor(32, 32, 32, 255).setNormal(pose, nextX, nextY, nextZ);
   }

   private static void vertex(VertexConsumer consumer, Pose pose, int light, float x, int y, int u, int v) {
      consumer.addVertex(pose.pose(), x - 0.5F, (float)y - 0.5F, 0.0F)
         .setColor(255, 255, 255, 255)
         .setUv((float)u, (float)v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(pose, 0.0F, 1.0F, 0.0F)
         ;
   }

   public ResourceLocation getTextureLocation(TraumaHookEntity entity) {
      return TEXTURE;
   }
}
