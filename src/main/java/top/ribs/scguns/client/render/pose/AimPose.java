package top.ribs.scguns.client.render.pose;

import org.joml.Vector3f;

public class AimPose {
   private final AimPose.Instance idle = new AimPose.Instance();
   private final AimPose.Instance aiming = new AimPose.Instance();

   public AimPose() {
      super();
   }

   public AimPose.Instance getIdle() {
      return this.idle;
   }

   public AimPose.Instance getAiming() {
      return this.aiming;
   }

   public static class Instance {
      private LimbPose leftArm = new LimbPose();
      private LimbPose rightArm = new LimbPose();
      private float renderYawOffset = 0.0F;
      private Vector3f itemTranslate = new Vector3f();
      private Vector3f itemRotation = new Vector3f();

      public Instance() {
         super();
      }

      public LimbPose getLeftArm() {
         return this.leftArm;
      }

      public AimPose.Instance setLeftArm(LimbPose leftArm) {
         this.leftArm = leftArm;
         return this;
      }

      public LimbPose getRightArm() {
         return this.rightArm;
      }

      public AimPose.Instance setRightArm(LimbPose rightArm) {
         this.rightArm = rightArm;
         return this;
      }

      public float getRenderYawOffset() {
         return this.renderYawOffset;
      }

      public AimPose.Instance setRenderYawOffset(float renderYawOffset) {
         this.renderYawOffset = renderYawOffset;
         return this;
      }

      public Vector3f getItemTranslate() {
         return this.itemTranslate;
      }

      public AimPose.Instance setItemTranslate(Vector3f itemTranslate) {
         this.itemTranslate = itemTranslate;
         return this;
      }

      public Vector3f getItemRotation() {
         return this.itemRotation;
      }

      public AimPose.Instance setItemRotation(Vector3f itemRotation) {
         this.itemRotation = itemRotation;
         return this;
      }
   }
}
