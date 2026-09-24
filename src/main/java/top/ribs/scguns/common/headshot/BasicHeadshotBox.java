package top.ribs.scguns.common.headshot;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import top.ribs.scguns.interfaces.IHeadshotBox;

public class BasicHeadshotBox<T extends LivingEntity> implements IHeadshotBox<T> {
   protected double headWidth;
   protected double headHeight;
   protected double headYOffset;

   public BasicHeadshotBox(double headSize, double headYOffset) {
      super();
      this.headWidth = headSize;
      this.headHeight = headSize;
      this.headYOffset = headYOffset;
   }

   public BasicHeadshotBox(double headWidth, double headHeight, double headYOffset) {
      super();
      this.headWidth = headWidth;
      this.headHeight = headHeight;
      this.headYOffset = headYOffset;
   }

   @Nullable
   public AABB getHeadshotBox(T entity) {
      double halfWidth = this.headWidth / 2.0;
      AABB headBox = new AABB(-halfWidth * 0.0625, 0.0, -halfWidth * 0.0625, halfWidth * 0.0625, this.headHeight * 0.0625, halfWidth * 0.0625);
      return headBox.move(0.0, this.headYOffset * 0.0625, 0.0);
   }
}
