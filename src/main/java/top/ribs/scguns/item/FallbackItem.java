package top.ribs.scguns.item;


import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class FallbackItem extends Item {
   public FallbackItem(Properties pProperties) {
      super(pProperties);
   }

   @Override
   public void appendHoverText(ItemStack stack, Item.TooltipContext worldIn, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("info.scguns.fallback").withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD));
      tooltip.add(Component.translatable("info.scguns.fallback_help").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("info.scguns.fallback_info").withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD));
   }
}
