package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Locale;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import top.ribs.scguns.entity.monster.CogMinionEntity;

public class CogMinionRenderer extends MobRenderer<CogMinionEntity, CogMinionModel<CogMinionEntity>> {
   public CogMinionRenderer(Context pContext) {
      super(pContext, new CogMinionModel(pContext.bakeLayer(ModModelLayers.COG_MINION_LAYER)), 0.5F);
      this.addLayer(new ItemInHandLayer(this, pContext.getItemInHandRenderer()));
      this.addLayer(new CogMinionRenderer.CogMinionHelmetLayer(this, pContext.getModelSet(), pContext.getItemInHandRenderer()));
   }

   public ResourceLocation getTextureLocation(CogMinionEntity pEntity) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/entity/cog_minion.png");
   }

   public void render(CogMinionEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
      pMatrixStack.pushPose();
      pMatrixStack.scale(1.0F, 1.0F, 1.0F);
      super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
      pMatrixStack.popPose();
   }

   private static class CogMinionHelmetLayer extends RenderLayer<CogMinionEntity, CogMinionModel<CogMinionEntity>> {
      private final ItemInHandRenderer itemInHandRenderer;
      private final HumanoidModel<CogMinionEntity> helmetModel;
      private final HumanoidModel<CogMinionEntity> geoArmorProxy;

      public CogMinionHelmetLayer(
         MobRenderer<CogMinionEntity, CogMinionModel<CogMinionEntity>> renderer, EntityModelSet modelSet, ItemInHandRenderer itemInHandRenderer
      ) {
         super(renderer);
         this.itemInHandRenderer = itemInHandRenderer;
         this.helmetModel = new HumanoidModel(modelSet.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
         this.geoArmorProxy = new HumanoidModel(modelSet.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
      }

      public void render(
         PoseStack poseStack,
         MultiBufferSource buffer,
         int packedLight,
         CogMinionEntity entity,
         float limbSwing,
         float limbSwingAmount,
         float partialTicks,
         float ageInTicks,
         float netHeadYaw,
         float headPitch
      ) {
         ItemStack helmetStack = entity.getItemBySlot(EquipmentSlot.HEAD);
         if (!helmetStack.isEmpty()) {
            Item item = helmetStack.getItem();
            if (item instanceof ArmorItem armorItem) {
               if (item instanceof GeoItem) {
                  this.renderGeoHelmet(poseStack, buffer, packedLight, entity, helmetStack, armorItem, partialTicks);
               } else {
                  poseStack.pushPose();
                  ((CogMinionModel)this.getParentModel()).head.translateAndRotate(poseStack);
                  this.renderVanillaHelmet(poseStack, buffer, packedLight, entity, helmetStack, armorItem);
                  poseStack.popPose();
               }
            } else if (item instanceof BlockItem) {
               poseStack.pushPose();
               ((CogMinionModel)this.getParentModel()).head.translateAndRotate(poseStack);
               poseStack.scale(0.625F, -0.625F, -0.625F);
               poseStack.translate(0.0, -0.5, 0.0);
               this.itemInHandRenderer.renderItem(entity, helmetStack, ItemDisplayContext.HEAD, false, poseStack, buffer, packedLight);
               poseStack.popPose();
            }
         }
      }

      private void renderGeoHelmet(
         PoseStack poseStack, MultiBufferSource buffer, int packedLight, CogMinionEntity entity, ItemStack helmetStack, ArmorItem armorItem, float partialTicks
      ) {
         this.copyHeadTransform(((CogMinionModel)this.getParentModel()).head, this.geoArmorProxy.head);
         this.geoArmorProxy.head.y -= 17.0F;
         this.geoArmorProxy.head.visible = true;
         this.geoArmorProxy.body.visible = false;
         this.geoArmorProxy.rightArm.visible = false;
         this.geoArmorProxy.leftArm.visible = false;
         this.geoArmorProxy.rightLeg.visible = false;
         this.geoArmorProxy.leftLeg.visible = false;
         this.geoArmorProxy.hat.visible = false;
         HumanoidModel<?> armorModel = this.getArmorModel(entity, helmetStack, EquipmentSlot.HEAD, this.geoArmorProxy);
         if (armorModel != null) {
            poseStack.pushPose();
            float scale = 1.4F;
            poseStack.scale(scale, scale, scale);
            armorModel.renderToBuffer(poseStack, buffer.getBuffer(RenderType.armorCutoutNoCull(this.getArmorTexture(helmetStack))), packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
         }
      }

      private void copyHeadTransform(ModelPart source, ModelPart target) {
         target.x = source.x;
         target.y = source.y;
         target.z = source.z;
         target.xRot = source.xRot;
         target.yRot = source.yRot;
         target.zRot = source.zRot;
         target.xScale = source.xScale;
         target.yScale = source.yScale;
         target.zScale = source.zScale;
      }

      private HumanoidModel<?> getArmorModel(CogMinionEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> defaultModel) {
         return (HumanoidModel<?>)defaultModel;
      }

      private void renderVanillaHelmet(
         PoseStack poseStack, MultiBufferSource buffer, int packedLight, CogMinionEntity entity, ItemStack helmetStack, ArmorItem armorItem
      ) {
         poseStack.scale(1.1F, 1.0F, 1.1F);
         poseStack.translate(0.0, -0.09, 0.0);
         this.helmetModel.head.xRot = 0.0F;
         this.helmetModel.head.yRot = 0.0F;
         this.helmetModel.head.zRot = 0.0F;
         this.helmetModel.body.visible = false;
         this.helmetModel.rightArm.visible = false;
         this.helmetModel.leftArm.visible = false;
         this.helmetModel.rightLeg.visible = false;
         this.helmetModel.leftLeg.visible = false;
         this.helmetModel.hat.visible = false;
         this.helmetModel.head.visible = true;
         ResourceLocation texture = this.getArmorTexture(helmetStack);
         RenderType renderType = RenderType.armorCutoutNoCull(texture);
         VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
         this.helmetModel.head.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
         this.helmetModel.body.visible = true;
         this.helmetModel.rightArm.visible = true;
         this.helmetModel.leftArm.visible = true;
         this.helmetModel.rightLeg.visible = true;
         this.helmetModel.leftLeg.visible = true;
      }

      private ResourceLocation getArmorTexture(ItemStack stack) {
         ArmorItem item = (ArmorItem)stack.getItem();
         // 1.21 drops ArmorMaterial#getName(); the 1.20.1 name was the material's registry id
         // ("minecraft:diamond"), which is what the armor layer path below is assembled from.
         ResourceLocation materialId = BuiltInRegistries.ARMOR_MATERIAL.getKey(item.getMaterial().value());
         String texture = materialId != null ? materialId.toString() : "minecraft:missingno";
         String domain = "minecraft";
         int idx = texture.indexOf(58);
         if (idx != -1) {
            domain = texture.substring(0, idx);
            texture = texture.substring(idx + 1);
         }

         String s1 = String.format(Locale.ROOT, "%s:textures/models/armor/%s_layer_%d.png", domain, texture, 1);
         return ResourceLocation.parse(s1);
      }
   }
}
