package top.ribs.scguns.client;

import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.client.render.entity.GrenadeRenderer;
import top.ribs.scguns.client.render.entity.GrenadeRoundRenderer;
import top.ribs.scguns.client.render.entity.MicroJetRenderer;
import top.ribs.scguns.client.render.entity.ProjectileRenderer;
import top.ribs.scguns.client.render.entity.RocketRenderer;
import top.ribs.scguns.client.render.entity.ShotballRenderer;
import top.ribs.scguns.client.render.entity.ShulkshotRenderer;
import top.ribs.scguns.client.render.entity.ThrowableGrenadeRenderer;
import top.ribs.scguns.client.render.entity.ThrowableItemRenderer;
import top.ribs.scguns.client.render.entity.ThrowableShotballRenderer;
import top.ribs.scguns.entity.client.RaidFlareRenderer;
import top.ribs.scguns.init.ModEntities;

@EventBusSubscriber(
   modid = "scguns",
   value = {Dist.CLIENT},
   bus = Bus.MOD
)
public class GunEntityRenderers {
   public GunEntityRenderers() {
      super();
   }

   @SubscribeEvent
   public static void registerEntityRenders(RegisterRenderers event) {
      event.registerEntityRenderer((EntityType)ModEntities.PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.BEARPACK_SHELL_PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.PLASMA_PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.RAMROD_PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.HOG_ROUND_PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.BEOWULF_PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.BLAZE_ROD_PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.BASIC_BULLET_PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.NEEDLE_PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.FLECHETTE_PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.HARDENED_BULLET_PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.BUCKSHOT_PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.FIRE_ROUND_PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.OSBORNE_SLUG_PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.GRENADE.get(), GrenadeRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.HE_GRENADE_PROJECTILE.get(), GrenadeRoundRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.FIRE_GRENADE_PROJECTILE.get(), GrenadeRoundRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.BOUNCY_GRENADE_PROJECTILE.get(), GrenadeRoundRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.GAS_GRENADE_PROJECTILE.get(), GrenadeRoundRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.ROCKET.get(), RocketRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.MICROJET.get(), MicroJetRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.SCULK_CELL.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.FROG_DART_PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.SHATTER_ROUND_PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.SYRINGE_PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.SHULKSHOT.get(), ShulkshotRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.KRAHG_ROUND_PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.ADVANCED_ROUND_PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.SHATTER_ROUND_PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.GIBBS_ROUND_PROJECTILE.get(), ProjectileRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.SHOTBALL_PROJECTILE.get(), ShotballRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.THROWABLE_GRENADE.get(), ThrowableGrenadeRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.THROWABLE_STUN_GRENADE.get(), ThrowableGrenadeRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.THROWABLE_MOLOTOV_COCKTAIL.get(), ThrowableItemRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.THROWABLE_HELLFIRE_BOMB.get(), ThrowableItemRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.THROWABLE_BEACON_GRENADE.get(), ThrowableItemRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.THROWABLE_GAS_GRENADE.get(), ThrowableItemRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.THROWABLE_CHOKE_BOMB.get(), ThrowableGrenadeRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.THROWABLE_SWARM_BOMB.get(), ThrowableItemRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.THROWABLE_SHOTBALL.get(), ThrowableShotballRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.THROWABLE_NAIL_BOMB.get(), ThrowableItemRenderer::new);
      event.registerEntityRenderer((EntityType)ModEntities.RAID_FLARE.get(), RaidFlareRenderer::new);
   }
}
