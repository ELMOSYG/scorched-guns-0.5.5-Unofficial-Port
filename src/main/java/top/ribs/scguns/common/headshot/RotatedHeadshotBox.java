package top.ribs.scguns.common.headshot;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RotatedHeadshotBox<T extends LivingEntity> extends BasicHeadshotBox<T> {
   protected double headZOffset;
   protected boolean rotatePitch;
   protected boolean rotateYaw;

   public RotatedHeadshotBox(double headSize, double headYOffset, double headZOffset, boolean rotatePitch, boolean rotateYaw) {
      super(headSize, headSize, headYOffset);
      this.headZOffset = headZOffset;
      this.rotatePitch = rotatePitch;
      this.rotateYaw = rotateYaw;
   }

   public RotatedHeadshotBox(double headWidth, double headHeight, double headYOffset, double headZOffset, boolean rotatePitch, boolean rotateYaw) {
      super(headWidth, headHeight, headYOffset);
      this.headZOffset = headZOffset;
      this.rotatePitch = rotatePitch;
      this.rotateYaw = rotateYaw;
   }

   @Nullable
   @Override
   public AABB getHeadshotBox(T entity) {
      AABB headBox = super.getHeadshotBox(entity);
      return headBox != null
         ? headBox.move(
            Vec3.directionFromRotation(this.rotatePitch ? entity.getXRot() : 0.0F, this.rotateYaw ? entity.yBodyRot : 0.0F).normalize().scale(this.headZOffset * 0.0625)
         )
         : null;
   }
}
