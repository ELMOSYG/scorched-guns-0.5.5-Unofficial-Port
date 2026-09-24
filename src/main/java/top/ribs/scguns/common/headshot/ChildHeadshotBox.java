package top.ribs.scguns.common.headshot;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public class ChildHeadshotBox<T extends LivingEntity> extends BasicHeadshotBox<T> {
   private final double childHeadScale;
   private final double headYOffsetScale;

   public ChildHeadshotBox(double headSize, double headYOffset, double childHeadScale, double headYOffsetScale) {
      super(headSize, headYOffset);
      this.childHeadScale = childHeadScale;
      this.headYOffsetScale = headYOffsetScale;
   }

   public ChildHeadshotBox(double headWidth, double headHeight, double headYOffset, double childHeadScale, double headYOffsetScale) {
      super(headWidth, headHeight, headYOffset);
      this.childHeadScale = childHeadScale;
      this.headYOffsetScale = headYOffsetScale;
   }

   @Nullable
   @Override
   public AABB getHeadshotBox(T entity) {
      AABB headBox = super.getHeadshotBox(entity);
      return headBox != null && entity.isBaby()
         ? new AABB(
            headBox.minX * this.childHeadScale,
            headBox.minY * this.headYOffsetScale,
            headBox.minZ * this.childHeadScale,
            headBox.maxX * this.childHeadScale,
            headBox.maxY * (this.headYOffsetScale + 0.065),
            headBox.maxZ * this.childHeadScale
         )
         : headBox;
   }
}
