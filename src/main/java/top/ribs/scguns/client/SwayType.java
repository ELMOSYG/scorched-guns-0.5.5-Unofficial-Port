package top.ribs.scguns.client;

import com.mojang.math.Axis;

public enum SwayType {
   DIRECTIONAL(Axis.XN, Axis.YN),
   DRAG(Axis.XP, Axis.YP);

   final Axis pitchRotation;
   final Axis yawRotation;

   private SwayType(Axis pitchRotation, Axis yawRotation) {
      this.pitchRotation = pitchRotation;
      this.yawRotation = yawRotation;
   }

   public Axis getPitchRotation() {
      return this.pitchRotation;
   }

   public Axis getYawRotation() {
      return this.yawRotation;
   }
}
