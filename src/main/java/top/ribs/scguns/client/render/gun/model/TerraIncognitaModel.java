package top.ribs.scguns.client.render.gun.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.client.SpecialModels;
import top.ribs.scguns.client.render.gun.IOverrideModel;
import top.ribs.scguns.client.util.RenderUtil;
import top.ribs.scguns.common.Gun;

public class TerraIncognitaModel implements IOverrideModel {
   public TerraIncognitaModel() {
      super();
   }

   @Override
   public void render(
      float partialTicks,
      ItemDisplayContext transformType,
      ItemStack stack,
      ItemStack parent,
      LivingEntity entity,
      PoseStack matrixStack,
      MultiBufferSource buffer,
      int light,
      int overlay
   ) {
      RenderUtil.renderModel(SpecialModels.TERRA_INCOGNITA_MAIN.getModel(), stack, matrixStack, buffer, light, overlay);
      if (Gun.getScope(stack) == null) {
         RenderUtil.renderModel(SpecialModels.TERRA_INCOGNITA_SIGHTS.getModel(), stack, matrixStack, buffer, light, overlay);
      } else {
         RenderUtil.renderModel(SpecialModels.TERRA_INCOGNITA_NO_SIGHTS.getModel(), stack, matrixStack, buffer, light, overlay);
      }
   }
}
