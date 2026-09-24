package top.ribs.scguns.item;


import net.minecraft.world.entity.LivingEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.blockentity.MineUnitBlockEntity;
import top.ribs.scguns.init.ModTags;

public class MetalDetectorItem extends Item {
   private static final int DETECTION_RADIUS = 16;
   private static final int COOLDOWN_TICKS = 30;
   private static final double BASE_PULL_STRENGTH = 0.5;
   private static final double MAX_PULL_STRENGTH = 1.3;

   public MetalDetectorItem(Properties properties) {
      super(properties);
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (player.getCooldowns().isOnCooldown(this)) {
         return InteractionResultHolder.pass(stack);
      } else {
         BlockPos nearestMetal = this.findNearestMetal(level, player.blockPosition());
         if (nearestMetal != null) {
            if (!level.isClientSide) {
               double distance = Math.sqrt(player.blockPosition().distSqr(nearestMetal));
               this.pullPlayerToward(player, nearestMetal, distance);
               this.spawnTargetHint((ServerLevel)level, nearestMetal);
               level.playSound(null, player.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 0.5F, 1.0F + (float)(distance / 16.0) * 0.5F);
               stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(player.getUsedItemHand()));
            }

            player.getCooldowns().addCooldown(this, 30);
            return InteractionResultHolder.consume(stack);
         } else {
            if (level.isClientSide) {
               level.playSound(player, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.3F, 0.8F);
            }

            player.getCooldowns().addCooldown(this, 15);
            return InteractionResultHolder.fail(stack);
         }
      }
   }

   private void spawnTargetHint(ServerLevel level, BlockPos targetPos) {
      BlockState state = level.getBlockState(targetPos);
      boolean isMine = level.getBlockEntity(targetPos) instanceof MineUnitBlockEntity;
      double baseY = isMine ? (double)targetPos.getY() + 0.5 : (double)targetPos.getY() + 1.0;
      int particleCount = isMine ? 3 : 2;

      for (int i = 0; i < particleCount; i++) {
         double offsetX = (double)targetPos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.3;
         double offsetY = baseY + level.random.nextDouble() * 0.1;
         double offsetZ = (double)targetPos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.3;
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, offsetX, offsetY, offsetZ, 1, 0.0, 0.0, 0.0, 0.01);
      }
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.NONE;
   }

   @Override
   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 0;
   }

   @Nullable
   private BlockPos findNearestMetal(Level level, BlockPos playerPos) {
      BlockPos nearestPos = null;
      double nearestDistSq = Double.MAX_VALUE;

      for (int x = -16; x <= 16; x++) {
         for (int y = -16; y <= 16; y++) {
            for (int z = -16; z <= 16; z++) {
               BlockPos checkPos = playerPos.offset(x, y, z);
               BlockState state = level.getBlockState(checkPos);
               if (state.is(ModTags.Blocks.METAL_DETECTABLE)) {
                  double distSq = playerPos.distSqr(checkPos);
                  if (distSq < nearestDistSq) {
                     nearestDistSq = distSq;
                     nearestPos = checkPos;
                  }
               }
            }
         }
      }

      return nearestPos;
   }

   private void pullPlayerToward(Player player, BlockPos targetPos, double distance) {
      Vec3 playerPos = player.position();
      Vec3 targetVec = Vec3.atCenterOf(targetPos);
      Vec3 direction = targetVec.subtract(playerPos).normalize();
      double pullStrength = 0.5 + 0.8 * (1.0 - Math.min(distance / 16.0, 1.0));
      Vec3 velocity = direction.scale(pullStrength);
      player.setDeltaMovement(player.getDeltaMovement().add(velocity));
      player.hurtMarked = true;
   }
}
