package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.entity.monster.ZombifiedHornlinEntity;

public class ZombifiedHornlinRenderer extends HumanoidMobRenderer<ZombifiedHornlinEntity, ZombifiedHornlinModel> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("scguns", "textures/entity/zombified_hornlin.png");

   public ZombifiedHornlinRenderer(Context context) {
      super(context, new ZombifiedHornlinModel(context.bakeLayer(ModModelLayers.ZOMBIFIED_HORNLIN_LAYER)), 0.4F);
      this.addLayer(new ZombifiedHornlinRenderer.ZombifiedHornlinFoodItemLayer(this, context.getItemInHandRenderer()));
      this.addLayer(
         new ZombifiedHornlinRenderer.ScaledArmorLayer(
            this, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()
         )
      );
   }

   public ResourceLocation getTextureLocation(ZombifiedHornlinEntity entity) {
      return TEXTURE;
   }

   private static class ScaledArmorLayer<T extends ZombifiedHornlinEntity, M extends HumanoidModel<T>> extends HumanoidArmorLayer<T, M, HumanoidModel<T>> {
      private final HumanoidModel<T> innerModel;
      private final HumanoidModel<T> outerModel;

      public ScaledArmorLayer(HumanoidMobRenderer<T, M> renderer, HumanoidModel<T> innerModel, HumanoidModel<T> outerModel, ModelManager modelManager) {
         super(renderer, innerModel, outerModel, modelManager);
         this.innerModel = innerModel;
         this.outerModel = outerModel;
      }

      public void render(
         PoseStack poseStack,
         MultiBufferSource buffer,
         int packedLight,
         T entity,
         float limbSwing,
         float limbSwingAmount,
         float partialTicks,
         float ageInTicks,
         float netHeadYaw,
         float headPitch
      ) {
         boolean hasHelmet = !entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
         boolean hasBodyArmor = !entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
            || !entity.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
            || !entity.getItemBySlot(EquipmentSlot.FEET).isEmpty();
         if (hasHelmet || hasBodyArmor) {
            ItemStack helmetItem = entity.getItemBySlot(EquipmentSlot.HEAD);
            if (hasHelmet) {
               this.innerModel.body.visible = false;
               this.innerModel.rightArm.visible = false;
               this.innerModel.leftArm.visible = false;
               this.innerModel.rightLeg.visible = false;
               this.innerModel.leftLeg.visible = false;
               this.outerModel.body.visible = false;
               this.outerModel.rightArm.visible = false;
               this.outerModel.leftArm.visible = false;
               this.outerModel.rightLeg.visible = false;
               this.outerModel.leftLeg.visible = false;
               poseStack.pushPose();
               poseStack.scale(1.21F, 1.0F, 1.05F);
               super.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
               poseStack.popPose();
               this.innerModel.body.visible = true;
               this.innerModel.rightArm.visible = true;
               this.innerModel.leftArm.visible = true;
               this.innerModel.rightLeg.visible = true;
               this.innerModel.leftLeg.visible = true;
               this.outerModel.body.visible = true;
               this.outerModel.rightArm.visible = true;
               this.outerModel.leftArm.visible = true;
               this.outerModel.rightLeg.visible = true;
               this.outerModel.leftLeg.visible = true;
            }

            if (hasBodyArmor) {
               entity.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
               poseStack.pushPose();
               poseStack.scale(1.2F, 1.05F, 1.35F);
               super.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
               poseStack.popPose();
               entity.setItemSlot(EquipmentSlot.HEAD, helmetItem);
            }
         }
      }
   }

   private static class ZombifiedHornlinFoodItemLayer extends RenderLayer<ZombifiedHornlinEntity, ZombifiedHornlinModel> {
      private final ItemInHandRenderer itemInHandRenderer;

      public ZombifiedHornlinFoodItemLayer(HumanoidMobRenderer<ZombifiedHornlinEntity, ZombifiedHornlinModel> renderer, ItemInHandRenderer itemInHandRenderer) {
         super(renderer);
         this.itemInHandRenderer = itemInHandRenderer;
      }

      public void render(
         PoseStack poseStack,
         MultiBufferSource buffer,
         int packedLight,
         ZombifiedHornlinEntity entity,
         float limbSwing,
         float limbSwingAmount,
         float partialTicks,
         float ageInTicks,
         float netHeadYaw,
         float headPitch
      ) {
         if (entity.isEatingGold() || entity.isPreparingToEat()) {
            ItemStack heldFood = entity.getHeldFoodItem();
            if (!heldFood.isEmpty()) {
               poseStack.pushPose();
               ZombifiedHornlinModel model = (ZombifiedHornlinModel)this.getParentModel();
               model.leftArm.translateAndRotate(poseStack);
               poseStack.translate(0.125, 0.625, 0.0);
               poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
               poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
               poseStack.scale(0.75F, 0.75F, 0.75F);
               this.itemInHandRenderer.renderItem(entity, heldFood, ItemDisplayContext.GROUND, false, poseStack, buffer, packedLight);
               poseStack.popPose();
            }
         }
      }
   }
}
