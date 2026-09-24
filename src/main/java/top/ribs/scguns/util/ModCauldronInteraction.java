package top.ribs.scguns.util;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import top.ribs.scguns.init.ModBlocks;
import top.ribs.scguns.init.ModItems;

public class ModCauldronInteraction {
   public ModCauldronInteraction() {
      super();
   }

   public static void register() {
      CauldronInteraction.EMPTY.map().put((Item)ModItems.VICIOUS_ACID_BUCKET.get(), (state, level, pos, player, hand, stack) -> {
         if (!level.isClientSide) {
            player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            level.setBlockAndUpdate(pos, ((Block)ModBlocks.VICIOUS_ACID_CAULDRON.get()).defaultBlockState());
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
         }

         return ItemInteractionResult.sidedSuccess(level.isClientSide);
      });
      CauldronInteraction.EMPTY.map().put(Items.BUCKET, (state, level, pos, player, hand, stack) -> {
         if (level.getBlockState(pos).getBlock() == ModBlocks.VICIOUS_ACID_CAULDRON.get()) {
            if (!level.isClientSide) {
               player.setItemInHand(hand, new ItemStack((ItemLike)ModItems.VICIOUS_ACID_BUCKET.get()));
               level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
               level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
               level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
         } else {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         }
      });
   }
}
