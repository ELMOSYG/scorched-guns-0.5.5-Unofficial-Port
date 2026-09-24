package top.ribs.scguns.client.util;

public class GunRotationHandler {
   private static final float ROTATION_LERP_SPEED = 0.6F;
   private float currentCylinderRotation = 0.0F;
   private float currentMagazineRotation = 0.0F;
   private float targetCylinderRotation = 0.0F;
   private float targetMagazineRotation = 0.0F;

   public GunRotationHandler() {
      super();
   }

   public void updateRotations(float partialTick) {
      this.currentCylinderRotation = this.lerpRotation(this.currentCylinderRotation, this.targetCylinderRotation, 0.6F);
      this.currentMagazineRotation = this.lerpRotation(this.currentMagazineRotation, this.targetMagazineRotation, 0.6F);
      this.currentCylinderRotation = this.normalizeRotation(this.currentCylinderRotation);
      this.currentMagazineRotation = this.normalizeRotation(this.currentMagazineRotation);
   }

   public void incrementCylinderRotation(float amount) {
      this.targetCylinderRotation = this.normalizeRotation(this.targetCylinderRotation + amount);
   }

   public void incrementMagazineRotation(float amount) {
      this.targetMagazineRotation = this.normalizeRotation(this.targetMagazineRotation + amount);
   }

   public float getCurrentCylinderRotation() {
      return this.currentCylinderRotation;
   }

   public float getCurrentMagazineRotation() {
      return this.currentMagazineRotation;
   }

   private float lerpRotation(float current, float target, float speed) {
      float shortestDistance = ((target - current) % 360.0F + 540.0F) % 360.0F - 180.0F;
      return current + shortestDistance * speed;
   }

   private float normalizeRotation(float rotation) {
      rotation %= 360.0F;
      if (rotation < 0.0F) {
         rotation += 360.0F;
      }

      return rotation;
   }
}
