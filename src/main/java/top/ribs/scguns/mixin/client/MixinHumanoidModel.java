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

/**
 * Poses a mob's arms around the gun it is holding.
 *
 * <p>Vanilla does exactly this for the crossbow: {@code HumanoidModel.ArmPose.CROSSBOW_HOLD} sets the arms near
 * -90 degrees and then adds the head rotation on top, which is what makes a crossbow follow a target up and down
 * instead of always pointing level. This mixin is the same idea for guns, with per-family rest angles because
 * skeletons, piglins and witches do not hold their arms the same way.</p>
 *
 * <p>Two problems in the previous version are fixed here. The angles were fixed, so a gun only ever pointed
 * straight ahead no matter where the target was. And while crouching it added {@code +0.4} radians to both arms:
 * that constant comes from vanilla's own crouch handling, where it stops <i>hanging</i> arms from sinking into the
 * body, but on an arm already raised to -90 degrees it pressed the barrel into the floor. That sign is inverted
 * here, and the crouch pose is in any case only used while reloading now (see {@code GuardGunAttackGoal}), where a
 * lowered muzzle is what the pose should look like.</p>
 */
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
            // Zombies and illagers are drawn by their own models, which this mixin does not touch.
            if (!(entity instanceof Zombie) && !(entity instanceof AbstractIllager)) {
               this.applyGunHoldingPose(mob);
            }
         }
      }
   }

   private void applyGunHoldingPose(Mob mob) {
      boolean rightHanded = mob.getMainArm() == HumanoidArm.RIGHT;

      float mainPitch;
      float mainYaw;
      float offPitch;
      float offYaw;
      float offRoll;

      if (mob instanceof AbstractSkeleton) {
         mainPitch = -90.0F;
         mainYaw = -8.0F;
         offPitch = -85.0F;
         offYaw = 35.0F;
         offRoll = -5.0F;
      } else if (mob instanceof AbstractPiglin) {
         mainPitch = -90.0F;
         mainYaw = -5.0F;
         offPitch = -87.0F;
         offYaw = 28.0F;
         offRoll = 0.0F;
      } else if (mob instanceof Witch) {
         mainPitch = -90.0F;
         mainYaw = -7.0F;
         offPitch = -85.0F;
         offYaw = 30.0F;
         offRoll = 0.0F;
      } else {
         mainPitch = -90.0F;
         mainYaw = -5.0F;
         offPitch = -85.0F;
         offYaw = 30.0F;
         offRoll = 0.0F;
      }

      // The whole point: the arms follow the head, so the gun tracks a target above or below the mob the way a
      // crossbow does. The head rotation is already relative to a body that faces the target.
      float headPitch = this.head.xRot;
      float headYaw = this.head.yRot;

      // A crouched body is tipped forward by vanilla's crouch pose; subtract rather than add so the barrel stays
      // level instead of being pushed towards the floor.
      float crouchCompensation = this.crouching ? -0.4F : 0.0F;

      this.rightArm.visible = true;
      this.leftArm.visible = true;

      if (rightHanded) {
         this.rightArm.xRot = (float)Math.toRadians(mainPitch) + headPitch + crouchCompensation;
         this.rightArm.yRot = (float)Math.toRadians(mainYaw) + headYaw;
         this.rightArm.zRot = 0.0F;
         this.leftArm.xRot = (float)Math.toRadians(offPitch) + headPitch + crouchCompensation;
         this.leftArm.yRot = (float)Math.toRadians(offYaw) + headYaw;
         this.leftArm.zRot = (float)Math.toRadians(offRoll);
      } else {
         this.leftArm.xRot = (float)Math.toRadians(mainPitch) + headPitch + crouchCompensation;
         this.leftArm.yRot = (float)Math.toRadians(-mainYaw) + headYaw;
         this.leftArm.zRot = 0.0F;
         this.rightArm.xRot = (float)Math.toRadians(offPitch) + headPitch + crouchCompensation;
         this.rightArm.yRot = (float)Math.toRadians(-offYaw) + headYaw;
         this.rightArm.zRot = (float)Math.toRadians(-offRoll);
      }
   }
}
