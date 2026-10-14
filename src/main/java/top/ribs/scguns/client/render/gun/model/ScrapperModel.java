package top.ribs.scguns.client.render.gun.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.client.SpecialModels;
import top.ribs.scguns.client.render.gun.IOverrideModel;
import top.ribs.scguns.client.util.RenderUtil;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModItems;
import top.ribs.scguns.item.attachment.IAttachment;

public class ScrapperModel implements IOverrideModel {
   public ScrapperModel() {
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
      RenderUtil.renderModel(SpecialModels.SCRAPPER_MAIN.getModel(), stack, matrixStack, buffer, light, overlay);
      this.renderBarrelAttachments(matrixStack, buffer, stack, light, overlay);
      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.STOCK)) {
         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.WEIGHTED_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.SCRAPPER_STOCK_HEAVY.getModel(), stack, matrixStack, buffer, light, overlay);
         }

         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.LIGHT_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.SCRAPPER_STOCK_LIGHT.getModel(), stack, matrixStack, buffer, light, overlay);
         }

         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.WOODEN_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.SCRAPPER_STOCK_WOODEN.getModel(), stack, matrixStack, buffer, light, overlay);
         }

         if (Gun.getAttachment(IAttachment.Type.STOCK, stack).getItem() == ModItems.BUMP_STOCK.get()) {
            RenderUtil.renderModel(SpecialModels.SCRAPPER_STOCK_WOODEN.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      }

      if (entity.equals(Minecraft.getInstance().player)) {
         matrixStack.pushPose();
         matrixStack.translate(0.0, -0.3625, 0.0);
         ItemCooldowns tracker = Minecraft.getInstance().player.getCooldowns();
         float cooldown = tracker.getCooldownPercent(stack.getItem(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
         cooldown = (float)this.ease((double)cooldown);
         matrixStack.translate(0.0F, 0.0F, cooldown / 10.0F);
         matrixStack.translate(0.0, 0.3625, 0.0);
         RenderUtil.renderModel(SpecialModels.SCRAPPER_BOLT.getModel(), stack, matrixStack, buffer, light, overlay);
         matrixStack.popPose();
      }

      float magazinePosition = this.calculateMagazinePosition(stack);
      float translationMultiplier = 0.25F;
      matrixStack.pushPose();
      matrixStack.translate(this.clampMagazinePosition(magazinePosition * translationMultiplier), 0.0F, 0.0F);
      RenderUtil.renderModel(SpecialModels.SCRAPPER_MAGAZINE.getModel(), stack, matrixStack, buffer, light, overlay);
      matrixStack.popPose();
   }

   private void renderBarrelAttachments(PoseStack matrixStack, MultiBufferSource buffer, ItemStack stack, int light, int overlay) {
      boolean hasExtendedBarrel = false;
      if (Gun.hasAttachmentEquipped(stack, IAttachment.Type.BARREL)) {
         if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.EXTENDED_BARREL.get()) {
            RenderUtil.renderModel(SpecialModels.SCRAPPER_EXT_BARREL.getModel(), stack, matrixStack, buffer, light, overlay);
            hasExtendedBarrel = true;
         } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.SILENCER.get()) {
            RenderUtil.renderModel(SpecialModels.SCRAPPER_SILENCER.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.MUZZLE_BRAKE.get()) {
            RenderUtil.renderModel(SpecialModels.SCRAPPER_MUZZLE_BRAKE.getModel(), stack, matrixStack, buffer, light, overlay);
         } else if (Gun.getAttachment(IAttachment.Type.BARREL, stack).getItem() == ModItems.ADVANCED_SILENCER.get()) {
            RenderUtil.renderModel(SpecialModels.SCRAPPER_ADVANCED_SILENCER.getModel(), stack, matrixStack, buffer, light, overlay);
         }
      }

      if (!hasExtendedBarrel) {
         RenderUtil.renderModel(SpecialModels.SCRAPPER_STAN_BARREL.getModel(), stack, matrixStack, buffer, light, overlay);
      }
   }

   private double ease(double x) {
      return 1.0 - Math.pow(1.0 - 2.0 * x, 4.0);
   }

   private float calculateMagazinePosition(ItemStack stack) {
      int maxAmmo = Gun.getMaxAmmo(stack);
      int currentAmmo = Gun.getAmmoCount(stack);
      return Math.min((float)(maxAmmo - currentAmmo) / (float)maxAmmo, 1.0F);
   }

   private float clampMagazinePosition(float position) {
      return Math.max(0.0F, Math.min(position, 1.0F));
   }
}
