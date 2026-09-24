package top.ribs.scguns.common.headshot;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public class NoChildRotatedHeadshotBox<T extends LivingEntity> extends RotatedHeadshotBox<T> {
   public NoChildRotatedHeadshotBox(double headSize, double headYOffset, double headZOffset, boolean rotatePitch, boolean rotateYaw) {
      super(headSize, headYOffset, headZOffset, rotatePitch, rotateYaw);
   }

   public NoChildRotatedHeadshotBox(double headWidth, double headHeight, double headYOffset, double headZOffset, boolean rotatePitch, boolean rotateYaw) {
      super(headWidth, headHeight, headYOffset, headZOffset, rotatePitch, rotateYaw);
   }

   @Nullable
   @Override
   public AABB getHeadshotBox(T entity) {
      return entity.isBaby() ? null : super.getHeadshotBox(entity);
   }
}
