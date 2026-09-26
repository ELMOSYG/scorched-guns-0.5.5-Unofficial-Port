package top.ribs.scguns.mixin.common.compat.guardvillagers;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.ribs.scguns.compat.guardvillagers.GuardVillagersCompat;
import top.ribs.scguns.entity.projectile.ProjectileEntity;

/**
 * A guard's shots pass through the village: villagers, iron golems and other guards take no impact, no
 * damage, no knockback and no impact effect from them (HANDOFF section 82).
 *
 * <h2>Why this is the hook, and why the obvious ones are not</h2>
 *
 * <p>This is the mechanism the maid compat uses, after the one the standalone 1.20.1 guard compat used
 * turned out to do nothing. Two of its three layers cannot work on Scorched Guns projectiles at all:</p>
 *
 * <ul>
 *   <li>vanilla {@code Projectile.canHitEntity} - SCG's {@code ProjectileEntity} is not a vanilla
 *       {@code Projectile} and picks its targets with its own {@code PROJECTILE_TARGETS} predicate, so a
 *       mixin there never sees one of its bullets;</li>
 *   <li>the base class's {@code onHitEntity} - about twenty-five subclasses override it without calling
 *       {@code super}, so intercepting the base method misses every advanced round, rocket, plasma,
 *       fire round, shotgun ball and the lightning projectile;</li>
 *   <li>the two entity searches ({@code findEntityOnPath} / {@code findEntitiesOnPath}) - most projectiles
 *       use the base versions, but {@code LightningProjectileEntity} and {@code ShotballProjectileEntity}
 *       run their own and would still hit an ally.</li>
 * </ul>
 *
 * <p>{@code getHitResult(Entity, Vec3, Vec3)} is the one funnel every one of those paths goes through, and
 * no projectile class overrides it. Returning {@code null} means "this shot does not hit this entity":
 * the bullet flies on, and the damage, the shield break, the impact effect and the elemental burst are
 * skipped together.</p>
 *
 * <p>The mixin targets the mod's own class and only asks {@link GuardVillagersCompat}, which answers false
 * for every non-guard - so it needs no mod gate and is harmless on a server without Guard Villagers, whose
 * classes it never names.</p>
 */
@Mixin(ProjectileEntity.class)
public abstract class GuardProjectileHitMixin {
   @Inject(method = "getHitResult", at = @At("HEAD"), cancellable = true, remap = false)
   private void scguns$skipGuardFriendlyFire(Entity candidate, Vec3 startVec, Vec3 endVec,
                                             CallbackInfoReturnable<ProjectileEntity.EntityResult> cir) {
      ProjectileEntity self = (ProjectileEntity)(Object)this;
      if (GuardVillagersCompat.isFriendlyShot(self.getOwner(), candidate)) {
         cir.setReturnValue(null);
      }
   }
}
