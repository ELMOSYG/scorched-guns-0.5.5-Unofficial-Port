package top.ribs.scguns.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import top.ribs.scguns.init.ModBlocks;

public class PhosphorItem extends Item {
   public PhosphorItem(Properties pProperties) {
      super(pProperties);
   }

   public InteractionResult useOn(UseOnContext context) {
      Level world = context.getLevel();
      BlockPos blockpos = context.getClickedPos();
      Player player = context.getPlayer();
      ItemStack itemstack = context.getItemInHand();
      BlockState blockstate = world.getBlockState(blockpos);
      if (blockstate.getBlock() instanceof BonemealableBlock && applyPhosphorFertilizer(itemstack, world, blockpos, player)) {
         if (!world.isClientSide) {
            world.levelEvent(1505, blockpos, 0);
         }

         return InteractionResult.sidedSuccess(world.isClientSide);
      } else {
         if (blockstate.is((Block)ModBlocks.PHOSPHOR_LAYER.get())) {
            int i = (Integer)blockstate.getValue(SnowLayerBlock.LAYERS);
            if (i < 8) {
               world.setBlock(blockpos, (BlockState)blockstate.setValue(SnowLayerBlock.LAYERS, i + 1), 2);
               if (player == null || !player.getAbilities().instabuild) {
                  itemstack.shrink(1);
               }

               return InteractionResult.SUCCESS;
            }
         }

         BlockPos blockposAbove = blockpos.above();
         BlockState stateAbove = world.getBlockState(blockposAbove);
         if (stateAbove.isAir() && blockstate.isFaceSturdy(world, blockpos, Direction.UP)) {
            BlockState newState = ((Block)ModBlocks.PHOSPHOR_LAYER.get()).defaultBlockState();
            world.setBlock(blockposAbove, newState, 2);
            if (player == null || !player.getAbilities().instabuild) {
               itemstack.shrink(1);
            }

            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.FAIL;
         }
      }
   }

   public static boolean applyPhosphorFertilizer(ItemStack stack, Level world, BlockPos pos, Player player) {
      BlockState blockstate = world.getBlockState(pos);
      if (blockstate.getBlock() instanceof BonemealableBlock bonemealableblock && bonemealableblock.isValidBonemealTarget(world, pos, blockstate)) {
         if (world instanceof ServerLevel serverLevel) {
            RandomSource random = world.getRandom();

            for (int i = 0; i < 3; i++) {
               BlockState currentState = world.getBlockState(pos);
               if (currentState.getBlock() instanceof BonemealableBlock currentBonemeal) {
                  if (!currentBonemeal.isValidBonemealTarget(world, pos, currentState) || !(random.nextFloat() < 0.6F)) {
                     break;
                  }

                  if (currentBonemeal.isBonemealSuccess(world, random, pos, currentState)) {
                     currentBonemeal.performBonemeal(serverLevel, random, pos, currentState);
                     if (i > 0) {
                        world.levelEvent(1505, pos, 0);
                     }
                  }
               }
            }

            stack.shrink(1);
         }

         return true;
      }

      return false;
   }
}
