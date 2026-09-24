package top.ribs.scguns.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import top.ribs.scguns.init.ModItems;

public class EmptyBlasphemyItem extends Item {
   public EmptyBlasphemyItem(Properties properties) {
      super(properties);
   }

   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      Player player = context.getPlayer();
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity instanceof SpawnerBlockEntity) {
         BaseSpawner spawner = ((SpawnerBlockEntity)blockEntity).getSpawner();
         if (this.isBlazeSpawner(spawner, level, pos)) {
            return this.captureBlaze(context, player);
         }
      }

      return super.useOn(context);
   }

   private boolean isBlazeSpawner(BaseSpawner spawner, Level level, BlockPos pos) {
      // 1.21 dropped the RandomSource parameter from getOrCreateDisplayEntity.
      return spawner.getOrCreateDisplayEntity(level, pos) != null && spawner.getOrCreateDisplayEntity(level, pos).getType() == EntityType.BLAZE;
   }

   private InteractionResult captureBlaze(Level level, BlockPos pos, Player player, ItemStack stack, InteractionHand hand) {
      if (!level.isClientSide) {
         level.playSound(null, pos, SoundEvents.BLAZE_HURT, SoundSource.HOSTILE, 1.0F, 1.0F);
         level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.0F, 1.0F);
         ItemStack blasphemyItem = new ItemStack((ItemLike)ModItems.BLASPHEMY.get());
         if (!player.isCreative()) {
            stack.shrink(1);
         }

         if (stack.isEmpty()) {
            player.setItemInHand(hand, blasphemyItem);
         } else if (!player.getInventory().add(blasphemyItem)) {
            player.drop(blasphemyItem, false);
         }
      }

      return InteractionResult.SUCCESS;
   }

   private InteractionResult captureBlaze(UseOnContext context, Player player) {
      return this.captureBlaze(context.getLevel(), context.getClickedPos(), player, context.getItemInHand(), context.getHand());
   }
}
