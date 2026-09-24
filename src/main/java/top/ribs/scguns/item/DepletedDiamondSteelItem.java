package top.ribs.scguns.item;


import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.ribs.scguns.init.ModItems;

public class DepletedDiamondSteelItem extends Item {
   private static final int XP_COST_PER_INGOT = 3;

   public DepletedDiamondSteelItem(Properties properties) {
      super(properties);
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      return level.isClientSide ? InteractionResultHolder.sidedSuccess(itemStack, true) : this.convertToRegularSteel(level, player, itemStack);
   }

   private InteractionResultHolder<ItemStack> convertToRegularSteel(Level level, Player player, ItemStack depletedStack) {
      int stackSize = depletedStack.getCount();
      int playerXP = this.getTotalExperience(player);
      int maxConvertible = playerXP / 3;
      int actualConvertible = Math.min(stackSize, maxConvertible);
      if (actualConvertible <= 0) {
         return InteractionResultHolder.fail(depletedStack);
      } else {
         ItemStack diamondSteelStack = new ItemStack((ItemLike)ModItems.DIAMOND_STEEL_INGOT.get(), actualConvertible);
         boolean addedToInventory = player.getInventory().add(diamondSteelStack);
         if (!addedToInventory) {
            player.drop(diamondSteelStack, false);
         }

         int xpToRemove = actualConvertible * 3;
         this.removeExperience(player, xpToRemove);
         depletedStack.shrink(actualConvertible);
         level.playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.EXPERIENCE_ORB_PICKUP,
            SoundSource.PLAYERS,
            0.5F,
            1.0F + (level.random.nextFloat() - 0.5F) * 0.2F
         );
         return InteractionResultHolder.sidedSuccess(depletedStack, false);
      }
   }

   private int getTotalExperience(Player player) {
      int experience = 0;
      int level = player.experienceLevel;
      if (level <= 16) {
         experience = level * level + 6 * level;
      } else if (level <= 31) {
         experience = (int)(2.5 * (double)level * (double)level - 40.5 * (double)level + 360.0);
      } else {
         experience = (int)(4.5 * (double)level * (double)level - 162.5 * (double)level + 2220.0);
      }

      return experience + Math.round(player.experienceProgress * (float)player.getXpNeededForNextLevel());
   }

   private void removeExperience(Player player, int xpToRemove) {
      int currentXP = this.getTotalExperience(player);
      int newXP = Math.max(0, currentXP - xpToRemove);
      player.experienceLevel = 0;
      player.experienceProgress = 0.0F;
      player.totalExperience = 0;
      player.giveExperiencePoints(newXP);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void appendHoverText(ItemStack stack, Item.TooltipContext level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.scguns.depleted_diamond_steel_ingot.tooltip.usage"));
      tooltip.add(Component.translatable("item.scguns.depleted_diamond_steel_ingot.tooltip.cost", new Object[]{3}));
      super.appendHoverText(stack, level, tooltip, flag);
   }
}
