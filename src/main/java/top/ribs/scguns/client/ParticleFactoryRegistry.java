package top.ribs.scguns.client;

import net.minecraft.core.particles.ParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.client.particle.AcidBubbleParticle;
import top.ribs.scguns.client.particle.BeowulfImpactParticle;
import top.ribs.scguns.client.particle.BloodParticle;
import top.ribs.scguns.client.particle.BulletHoleParticle;
import top.ribs.scguns.client.particle.CasingParticle;
import top.ribs.scguns.client.particle.FireBallParticle;
import top.ribs.scguns.client.particle.FireGrenadeExplosionParticle;
import top.ribs.scguns.client.particle.GreenFlameParticle;
import top.ribs.scguns.client.particle.GrenadeExplosionParticle;
import top.ribs.scguns.client.particle.LaserParticle;
import top.ribs.scguns.client.particle.PlasmaExplosionParticle;
import top.ribs.scguns.client.particle.PlasmaRingParticle;
import top.ribs.scguns.client.particle.RamrodImpactParticle;
import top.ribs.scguns.client.particle.RocketExplosionParticle;
import top.ribs.scguns.client.particle.RocketTrailParticle;
import top.ribs.scguns.client.particle.SmallLaserParticle;
import top.ribs.scguns.client.particle.SonicBlastParticle;
import top.ribs.scguns.client.particle.SoulFireBallParticle;
import top.ribs.scguns.client.particle.SulfurDustParticle;
import top.ribs.scguns.client.particle.SulfurSmokeParticle;
import top.ribs.scguns.client.particle.TrailParticle;
import top.ribs.scguns.client.particle.TurretMuzzleFlashParticle;
import top.ribs.scguns.init.ModParticleTypes;

@EventBusSubscriber(
   modid = "scguns",
   value = {Dist.CLIENT},
   bus = Bus.MOD
)
public class ParticleFactoryRegistry {
   public ParticleFactoryRegistry() {
      super();
   }

   @SubscribeEvent
   public static void onRegisterParticleFactory(RegisterParticleProvidersEvent event) {
      // No raw cast here: DeferredHolder#get() already returns ParticleType<BulletHoleData>,
      // so the provider lambda keeps its BulletHoleData parameter type.
      event.registerSpecial(
         ModParticleTypes.BULLET_HOLE.get(),
         (typeIn, worldIn, x, y, z, xSpeed, ySpeed, zSpeed) -> new BulletHoleParticle(worldIn, x, y, z, typeIn.getDirection(), typeIn.getPos())
      );
      event.registerSpriteSet((ParticleType)ModParticleTypes.BLOOD.get(), BloodParticle.Factory::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.TRAIL.get(), TrailParticle.Factory::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.ROCKET_TRAIL.get(), RocketTrailParticle.Factory::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.SONIC_BLAST.get(), SonicBlastParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.COPPER_CASING_PARTICLE.get(), CasingParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.BRASS_CASING_PARTICLE.get(), CasingParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.IRON_CASING_PARTICLE.get(), CasingParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.DIAMOND_STEEL_CASING_PARTICLE.get(), CasingParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.SHULK_CASING_PARTICLE.get(), CasingParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.SHELL_PARTICLE.get(), CasingParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.BEARPACK_PARTICLE.get(), CasingParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.GREEN_FLAME.get(), GreenFlameParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.PLASMA_RING.get(), PlasmaRingParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.SULFUR_SMOKE.get(), SulfurSmokeParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.SULFUR_DUST.get(), SulfurDustParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.RAMROD_IMPACT.get(), RamrodImpactParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.BEOWULF_IMPACT.get(), BeowulfImpactParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.TURRET_MUZZLE_FLASH.get(), TurretMuzzleFlashParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.LASER.get(), LaserParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.SMALL_LASER.get(), SmallLaserParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.ACID_BUBBLE.get(), AcidBubbleParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.PLASMA_EXPLOSION.get(), PlasmaExplosionParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.ROCKET_EXPLOSION.get(), RocketExplosionParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.GRENADE_EXPLOSION.get(), GrenadeExplosionParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.FIRE_GRENADE_EXPLOSION.get(), FireGrenadeExplosionParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.FIREBALL.get(), FireBallParticle.Provider::new);
      event.registerSpriteSet((ParticleType)ModParticleTypes.SOUL_FIREBALL.get(), SoulFireBallParticle.Provider::new);
   }
}
