package top.ribs.scguns.client.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.block.GunShelfBlock;
import top.ribs.scguns.blockentity.GunShelfBlockEntity;

public class GunShelfRenderer implements BlockEntityRenderer<GunShelfBlockEntity> {
   private final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

   public GunShelfRenderer(Context context) {
      super();
   }

   public void render(
      GunShelfBlockEntity tile, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn
   ) {
      ItemStack displayedItem = tile.getDisplayedItem();
      if (displayedItem != null && !displayedItem.isEmpty()) {
         matrixStackIn.pushPose();

         try {
            Direction facing = (Direction)tile.getBlockState().getValue(GunShelfBlock.FACING);
            switch (facing) {
               case NORTH:
                  matrixStackIn.translate(0.5, 0.4, 0.8F);
                  break;
               case SOUTH:
                  matrixStackIn.translate(0.5, 0.4, 0.2);
                  break;
               case EAST:
                  matrixStackIn.translate(0.2, 0.4, 0.5);
                  break;
               case WEST:
                  matrixStackIn.translate(0.8F, 0.4, 0.5);
            }

            matrixStackIn.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
            matrixStackIn.scale(0.55F, 0.55F, 0.55F);
            BakedModel model = this.itemRenderer.getModel(displayedItem, tile.getLevel(), null, 0);
            this.itemRenderer.render(displayedItem, ItemDisplayContext.FIXED, false, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn, model);
         } finally {
            matrixStackIn.popPose();
         }
      }
   }
}
