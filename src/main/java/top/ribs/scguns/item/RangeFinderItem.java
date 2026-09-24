package top.ribs.scguns.item;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.init.ModParticleTypes;

// 0.5.5 carried @EventBusSubscriber on this outer class *and* on the nested
// ClientEventHandler that actually holds the @SubscribeEvent method. Forge ignored a
// subscriber with nothing to subscribe; NeoForge aborts mod loading with "class
// RangeFinderItem has no @SubscribeEvent methods, but register was called anyway",
// so the annotation stays only on ClientEventHandler.
public class RangeFinderItem extends Item {
   public RangeFinderItem(Properties properties) {
      super(properties);
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack itemstack = player.getItemInHand(hand);
      if (level.isClientSide) {
         RangeFinderItem.ClientEventHandler.calculateAndDisplayRange(player);
      }

      return InteractionResultHolder.consume(itemstack);
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.NONE;
   }

   @Override
   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 0;
   }

   @EventBusSubscriber(
      modid = "scguns",
      bus = Bus.GAME,
      value = {Dist.CLIENT}
   )
   public static class ClientEventHandler {
      private static int tickCounter = 0;
      private static final int TICK_DELAY = 2;
      private static final double OFFSET = 0.1;
      private static final int MAX_RANGE = 200;

      public ClientEventHandler() {
         super();
      }

      @SubscribeEvent
      public static void onClientTick(ClientTickEvent.Post event) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            tickCounter++;
            if (tickCounter >= 2) {
               tickCounter = 0;
               ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
               if (heldItem.getItem() instanceof RangeFinderItem) {
                  showLaserParticles(player);
               }
            }
         }
      }

      private static void showLaserParticles(LocalPlayer player) {
         Level world = player.level();
         Vec3 start = player.getEyePosition(1.0F);
         Vec3 direction = player.getLookAngle();
         Vec3 end = start.add(direction.scale(200.0));
         HitResult hitResult = player.level().clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, player));
         Vec3 finalEnd = hitResult.getType() != Type.MISS ? hitResult.getLocation() : end;
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
            (ParticleOptions)ModParticleTypes.LASER.get(), adjustedPosition.x, adjustedPosition.y, adjustedPosition.z, 2.0, 0.0, 0.0
         );
      }

      private static void calculateAndDisplayRange(Player player) {
         Vec3 start = player.getEyePosition(1.0F);
         Vec3 direction = player.getLookAngle();
         Vec3 end = start.add(direction.scale(200.0));
         BlockHitResult hitResult = player.level().clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, player));
         AABB aabb = new AABB(start, end).inflate(0.5);
         List<Entity> entities = player.level().getEntities(player, aabb, e -> !e.isSpectator() && e.isPickable());
         EntityHitResult entityHitResult = null;
         double closestDistance = Double.MAX_VALUE;

         for (Entity entity : entities) {
            AABB entityAABB = entity.getBoundingBox().inflate(0.3);
            Optional<Vec3> optionalHit = entityAABB.clip(start, end);
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
            double distance = start.distanceTo(entityHitResult.getLocation());
            String entityName = entityHitResult.getEntity().getName().getString();
            player.displayClientMessage(Component.literal("Target: " + entityName + " - Distance: " + String.format("%.1f", distance) + " blocks"), true);
         } else if (hitResult.getType() != Type.MISS) {
            double distance = start.distanceTo(hitResult.getLocation());
            BlockPos blockPos = hitResult instanceof BlockHitResult ? hitResult.getBlockPos() : null;
            String blockName = player.level().getBlockState(blockPos).getBlock().getName().getString();
            player.displayClientMessage(Component.literal("Target: " + blockName + " - Distance: " + String.format("%.1f", distance) + " blocks"), true);
         } else {
            player.displayClientMessage(Component.literal("No target in range"), true);
         }
      }
   }
}
