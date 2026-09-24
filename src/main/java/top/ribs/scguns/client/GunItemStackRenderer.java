package top.ribs.scguns.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.client.handler.GunRenderingHandler;

public class GunItemStackRenderer extends BlockEntityWithoutLevelRenderer {
   public GunItemStackRenderer() {
      super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
   }

   public void renderByItem(ItemStack stack, ItemDisplayContext display, PoseStack poseStack, MultiBufferSource source, int light, int overlay) {
      poseStack.popPose();
      poseStack.pushPose();
      Minecraft mc = Minecraft.getInstance();
      if (display == ItemDisplayContext.GROUND) {
         GunRenderingHandler.get().applyWeaponScale(stack, poseStack);
      }

      // 0.5.5 passed Minecraft#getDeltaFrameTime() here -- the elapsed ticks this
      // frame. 1.21's equivalent is DeltaTracker#getGameTimeDeltaTicks(); the
      // partial tick (getGameTimeDeltaPartialTick) is a different quantity.
      GunRenderingHandler.get().renderWeapon(mc.player, stack, display, poseStack, source, light, mc.getTimer().getGameTimeDeltaTicks());
      poseStack.popPose();
      poseStack.pushPose();
   }
}
