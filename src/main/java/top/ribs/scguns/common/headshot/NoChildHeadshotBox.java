package top.ribs.scguns.common.headshot;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public class NoChildHeadshotBox<T extends LivingEntity> extends BasicHeadshotBox<T> {
   public NoChildHeadshotBox(double headSize, double headYOffset) {
      super(headSize, headYOffset);
   }

   public NoChildHeadshotBox(double headWidth, double headHeight, double headYOffset) {
      super(headWidth, headHeight, headYOffset);
   }

   @Nullable
   @Override
   public AABB getHeadshotBox(T entity) {
      return entity.isBaby() ? null : super.getHeadshotBox(entity);
   }
}
