package top.ribs.scguns.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.item.GunItem;

@Mixin({HumanoidModel.class})
public abstract class MixinHumanoidModel {
   @Shadow
   public ModelPart rightArm;
   @Shadow
   public ModelPart leftArm;
   @Shadow
   public ModelPart head;
   @Shadow
   public boolean crouching;

   public MixinHumanoidModel() {
      super();
   }

   @Inject(
      method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
      at = {@At("TAIL")}
   )
   private void overrideGunPose(
      LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci
   ) {
      if (entity instanceof Mob mob) {
         ItemStack mainHand = mob.getMainHandItem();
         if (mainHand.getItem() instanceof GunItem) {
            if (!(entity instanceof Zombie) && !(entity instanceof AbstractIllager)) {
               this.applyGunHoldingPose(mob);
            }
         }
      }
   }

   private void applyGunHoldingPose(Mob mob) {
      HumanoidArm mainArm = mob.getMainArm();
      boolean rightHanded = mainArm == HumanoidArm.RIGHT;
      this.rightArm.xRot = 0.0F;
      this.rightArm.yRot = 0.0F;
      this.rightArm.zRot = 0.0F;
      this.leftArm.xRot = 0.0F;
      this.leftArm.yRot = 0.0F;
      this.leftArm.zRot = 0.0F;
      this.rightArm.visible = true;
      this.leftArm.visible = true;
      if (mob instanceof AbstractSkeleton) {
         this.applySkeletonGunPose(rightHanded);
      } else if (mob instanceof AbstractPiglin) {
         this.applyPiglinGunPose(rightHanded);
      } else if (mob instanceof Witch) {
         this.applyWitchGunPose(rightHanded);
      } else {
         this.applyDefaultGunPose(rightHanded);
      }

      if (this.crouching) {
         this.rightArm.xRot += 0.4F;
         this.leftArm.xRot += 0.4F;
      }
   }

   private void applyDefaultGunPose(boolean rightHanded) {
      if (rightHanded) {
         this.rightArm.xRot = (float)Math.toRadians(-90.0);
         this.rightArm.yRot = (float)Math.toRadians(-5.0);
         this.rightArm.zRot = 0.0F;
         this.leftArm.xRot = (float)Math.toRadians(-85.0);
         this.leftArm.yRot = (float)Math.toRadians(30.0);
         this.leftArm.zRot = 0.0F;
      } else {
         this.leftArm.xRot = (float)Math.toRadians(-90.0);
         this.leftArm.yRot = (float)Math.toRadians(5.0);
         this.leftArm.zRot = 0.0F;
         this.rightArm.xRot = (float)Math.toRadians(-85.0);
         this.rightArm.yRot = (float)Math.toRadians(-30.0);
         this.rightArm.zRot = 0.0F;
      }
   }

   private void applySkeletonGunPose(boolean rightHanded) {
      if (rightHanded) {
         this.rightArm.xRot = (float)Math.toRadians(-90.0);
         this.rightArm.yRot = (float)Math.toRadians(-8.0);
         this.rightArm.zRot = 0.0F;
         this.leftArm.xRot = (float)Math.toRadians(-85.0);
         this.leftArm.yRot = (float)Math.toRadians(35.0);
         this.leftArm.zRot = (float)Math.toRadians(-5.0);
      } else {
         this.leftArm.xRot = (float)Math.toRadians(-90.0);
         this.leftArm.yRot = (float)Math.toRadians(8.0);
         this.leftArm.zRot = 0.0F;
         this.rightArm.xRot = (float)Math.toRadians(-85.0);
         this.rightArm.yRot = (float)Math.toRadians(-35.0);
         this.rightArm.zRot = (float)Math.toRadians(5.0);
      }
   }

   private void applyPiglinGunPose(boolean rightHanded) {
      if (rightHanded) {
         this.rightArm.xRot = (float)Math.toRadians(-90.0);
         this.rightArm.yRot = (float)Math.toRadians(-5.0);
         this.rightArm.zRot = 0.0F;
         this.leftArm.xRot = (float)Math.toRadians(-87.0);
         this.leftArm.yRot = (float)Math.toRadians(28.0);
         this.leftArm.zRot = 0.0F;
      } else {
         this.leftArm.xRot = (float)Math.toRadians(-90.0);
         this.leftArm.yRot = (float)Math.toRadians(5.0);
         this.leftArm.zRot = 0.0F;
         this.rightArm.xRot = (float)Math.toRadians(-87.0);
         this.rightArm.yRot = (float)Math.toRadians(-28.0);
         this.rightArm.zRot = 0.0F;
      }
   }

   private void applyWitchGunPose(boolean rightHanded) {
      if (rightHanded) {
         this.rightArm.xRot = (float)Math.toRadians(-90.0);
         this.rightArm.yRot = (float)Math.toRadians(-7.0);
         this.rightArm.zRot = 0.0F;
         this.leftArm.xRot = (float)Math.toRadians(-85.0);
         this.leftArm.yRot = (float)Math.toRadians(30.0);
         this.leftArm.zRot = 0.0F;
      } else {
         this.leftArm.xRot = (float)Math.toRadians(-90.0);
         this.leftArm.yRot = (float)Math.toRadians(7.0);
         this.leftArm.zRot = 0.0F;
         this.rightArm.xRot = (float)Math.toRadians(-85.0);
         this.rightArm.yRot = (float)Math.toRadians(-30.0);
         this.rightArm.zRot = 0.0F;
      }
   }
}
