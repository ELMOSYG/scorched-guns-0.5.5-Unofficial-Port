package top.ribs.scguns.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface IHeldAnimation {
   @OnlyIn(Dist.CLIENT)
   default void applyPlayerModelRotation(Player player, ModelPart rightArm, ModelPart leftArm, ModelPart head, InteractionHand hand, float aimProgress) {
   }

   @OnlyIn(Dist.CLIENT)
   default void applyPlayerPreRender(Player player, InteractionHand hand, float aimProgress, PoseStack poseStack, MultiBufferSource buffer) {
   }

   @OnlyIn(Dist.CLIENT)
   default void applyHeldItemTransforms(Player player, InteractionHand hand, float aimProgress, PoseStack poseStack, MultiBufferSource buffer) {
   }

   default void renderFirstPersonArms(
      Player player, HumanoidArm hand, ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int light, float partialTicks
   ) {
   }

   default boolean applyOffhandTransforms(Player player, PlayerModel model, ItemStack stack, PoseStack poseStack, float partialTicks) {
      return false;
   }

   default boolean canApplySprintingAnimation() {
      return true;
   }

   default boolean canRenderOffhandItem() {
      return false;
   }

   @OnlyIn(Dist.CLIENT)
   static void copyModelAngles(ModelPart source, ModelPart dest) {
      dest.xRot = source.xRot;
      dest.yRot = source.yRot;
      dest.zRot = source.zRot;
   }

   default double getFallSwayZOffset() {
      return 0.35;
   }
}
