package top.ribs.scguns.item;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModParticleTypes;
import top.ribs.scguns.item.attachment.impl.Scope;

// 0.5.5 carried @EventBusSubscriber on this outer class *and* on the nested handler
// class that actually holds the @SubscribeEvent method. Forge ignored a subscriber
// with nothing to subscribe; NeoForge aborts mod loading with "class LaserSightItem
// has no @SubscribeEvent methods, but register was called anyway", so the annotation
// stays only on the nested class.
public class LaserSightItem extends ScopeItem {
   public LaserSightItem(Scope scope, Properties properties) {
      super(scope, properties, true);
   }



   @EventBusSubscriber(
      modid = "scguns",
      bus = Bus.GAME,
      value = {Dist.CLIENT}
   )
   public static class ClientTickHandler {
      private static int tickCounter = 0;
      private static final int TICK_DELAY = 2;
      private static final double OFFSET = 0.1;

      public ClientTickHandler() {
         super();
      }

      @SubscribeEvent
      public static void onClientTick(ClientTickEvent.Post event) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            tickCounter++;
            if (tickCounter >= 2) {
               tickCounter = 0;
               Level world = player.level();
               ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
               if (heldItem.getItem() instanceof LaserSightItem || heldItem.getItem() instanceof GunItem && Gun.hasLaserSight(heldItem)) {
                  Vec3 start = player.getEyePosition(1.0F);
                  Vec3 direction = player.getLookAngle();
                  Vec3 end = start.add(direction.scale(50.0));
                  HitResult hitResult = player.level().clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, player));
                  Vec3 finalEnd = hitResult.getType() != net.minecraft.world.phys.HitResult.Type.MISS ? hitResult.getLocation() : end;
                  AABB aabb = new AABB(start, finalEnd).inflate(0.5);
                  List<Entity> entities = world.getEntities(player, aabb, e -> !e.isSpectator() && e.isPickable());
                  EntityHitResult entityHitResult = null;
                  double closestDistance = Double.MAX_VALUE;

                  for (Entity entity : entities) {
                     AABB entityAABB = entity.getBoundingBox().inflate(0.3);
                     Optional<Vec3> optionalHit = entityAABB.clip(start, finalEnd);
                     if (optionalHit.isPresent()) {
                        Vec3 hitVec = optionalHit.get();
                        double distance = start.distanceTo(hitVec);
                        if (distance < closestDistance) {
                           closestDistance = distance;
                           entityHitResult = new EntityHitResult(entity, hitVec);
                        }
                     }
                  }

                  if (entityHitResult != null) {
                     finalEnd = entityHitResult.getLocation();
                  }

                  Vec3 adjustedPosition = finalEnd.subtract(direction.scale(0.1));
                  world.addParticle(
                     (ParticleOptions)ModParticleTypes.LASER.get(),
                     adjustedPosition.x,
                     adjustedPosition.y,
                     adjustedPosition.z,
                     0.0,
                     0.0,
                     0.0
                  );
               }
            }
         }
      }
   }


}
