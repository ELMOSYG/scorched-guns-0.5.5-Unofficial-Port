package top.ribs.scguns.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import top.ribs.scguns.entity.monster.SignalBeaconEntity;

public class SignalBeaconModel<T extends Entity> extends HierarchicalModel<T> {
   private final ModelPart body;
   private final PartPose bodyDefault;

   public SignalBeaconModel(ModelPart root) {
      super();
      this.body = root.getChild("body");
      this.bodyDefault = this.body.storePose();
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition body = partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 7)
            .addBox(-2.5F, 5.5F, -2.5F, 5.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
            .texOffs(0, 0)
            .addBox(-3.0F, -0.5F, -3.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(0, 13)
            .addBox(-2.0F, 0.5F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(16, 13)
            .addBox(-2.0F, -1.5F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 17.5F, 0.0F)
      );
      PartDefinition cube_r1 = body.addOrReplaceChild(
         "cube_r1",
         CubeListBuilder.create().texOffs(4, 22).addBox(0.0F, -1.0F, -1.0F, 0.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, -5.5F, 0.0F, 0.0F, -2.3562F, 0.0F)
      );
      PartDefinition cube_r2 = body.addOrReplaceChild(
         "cube_r2",
         CubeListBuilder.create().texOffs(0, 22).addBox(0.0F, -1.0F, -1.0F, 0.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, -5.5F, 0.0F, 0.0F, -0.7854F, 0.0F)
      );
      PartDefinition head = body.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(16, 18).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -7.5F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 32, 32);
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      this.body.loadPose(this.bodyDefault);
      if (entity instanceof SignalBeaconEntity beacon) {
         int remainingLife = beacon.getRemainingLifespan();
         float breathScale = Mth.sin(ageInTicks * 0.1F) * 0.025F + 1.0F;
         this.body.xScale = breathScale;
         this.body.yScale = breathScale;
         this.body.zScale = breathScale;
         if (remainingLife <= 10 && remainingLife > 0) {
            float deathProgress = 1.0F - (float)remainingLife / 10.0F;
            float bounceIntensity = deathProgress * 0.5F;
            float bounce = Mth.sin(ageInTicks * 0.8F) * bounceIntensity;
            this.body.y += bounce;
            float wobble = Mth.sin(ageInTicks * 0.6F) * deathProgress * 0.1F;
            this.body.zRot = wobble;
         }
      }
   }

   @Override
   public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
      this.body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
   }

   public ModelPart root() {
      return this.body;
   }
}
