package top.ribs.scguns.event;


import net.neoforged.bus.api.ICancellableEvent;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.Event;
import top.ribs.scguns.entity.projectile.ProjectileEntity;

public class GunProjectileHitEvent extends Event implements ICancellableEvent {
   private final HitResult result;
   private final ProjectileEntity projectile;

   public GunProjectileHitEvent(HitResult result, ProjectileEntity projectile) {
      super();
      this.result = result;
      this.projectile = projectile;
   }

   public HitResult getRayTrace() {
      return this.result;
   }

   public ProjectileEntity getProjectile() {
      return this.projectile;
   }
}
