package top.ribs.scguns.client.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.ribs.scguns.blockentity.BasicTurretBlockEntity;
import top.ribs.scguns.client.SpecialModels;
import top.ribs.scguns.client.util.RenderUtil;

@OnlyIn(Dist.CLIENT)
public class BasicTurretRenderer implements BlockEntityRenderer<BasicTurretBlockEntity> {
   public BasicTurretRenderer(Context context) {
      super();
   }

   public void render(BasicTurretBlockEntity turret, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
      double previousYaw = (double)turret.getPreviousYaw();
      double yaw = (double)turret.getYaw();
      float interpolatedYaw = (float)(previousYaw + Mth.wrapDegrees(yaw - previousYaw) * (double)partialTicks);
      double previousPitch = (double)turret.getPreviousPitch();
      double pitch = (double)turret.getPitch();
      float interpolatedPitch = (float)(previousPitch + (pitch - previousPitch) * (double)partialTicks);
      this.renderTurretTop(
         turret, matrixStack, buffer, light, overlay, SpecialModels.BASIC_TURRET_TOP.getModel(), 0.5, 1.0, 0.5, interpolatedYaw, interpolatedPitch
      );
   }

   private void renderTurretTop(
      BasicTurretBlockEntity turret,
      PoseStack matrixStack,
      MultiBufferSource buffer,
      int light,
      int overlay,
      BakedModel model,
      double x,
      double y,
      double z,
      float yaw,
      float pitch
   ) {
      if (model != null) {
         matrixStack.pushPose();
         matrixStack.translate(x, y, z);
         matrixStack.mulPose(Axis.YP.rotationDegrees(yaw));
         float recoilPitch = pitch + turret.getRecoilPitchOffset();
         matrixStack.mulPose(Axis.XP.rotationDegrees(recoilPitch));
         matrixStack.translate(-x, -y, -z);
         RenderUtil.renderMaceratorWheel(model, matrixStack, buffer, light, overlay);
         matrixStack.popPose();
      }
   }

   public boolean shouldRenderOffScreen(BasicTurretBlockEntity p_112304_) {
      return true;
   }
}
