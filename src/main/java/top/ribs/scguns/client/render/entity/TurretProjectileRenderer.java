package top.ribs.scguns.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import top.ribs.scguns.entity.projectile.turret.TurretProjectileEntity;
import top.ribs.scguns.init.ModItems;

public class TurretProjectileRenderer extends EntityRenderer<TurretProjectileEntity> {
   public TurretProjectileRenderer(Context context) {
      super(context);
   }

   public ResourceLocation getTextureLocation(TurretProjectileEntity entity) {
      return null;
   }

   public void render(TurretProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      poseStack.pushPose();
      poseStack.scale(0.6F, 0.6F, 0.6F);
      poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
      ItemStack projectileItem = new ItemStack((ItemLike)ModItems.SHOTGUN_SHELL.get());
      Minecraft.getInstance()
         .getItemRenderer()
         .renderStatic(projectileItem, ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, entity.level(), 0);
      poseStack.popPose();
   }
}
