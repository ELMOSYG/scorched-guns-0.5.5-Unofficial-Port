package top.ribs.scguns.util.math;

import net.minecraft.world.phys.EntityHitResult;
import top.ribs.scguns.entity.projectile.ProjectileEntity;

public class ExtendedEntityRayTraceResult extends EntityHitResult {
   private final boolean headshot;

   public ExtendedEntityRayTraceResult(ProjectileEntity.EntityResult result) {
      super(result.getEntity(), result.getHitPos());
      this.headshot = result.isHeadshot();
   }

   public boolean isHeadshot() {
      return this.headshot;
   }
}
