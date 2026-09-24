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

public class TooltipItem extends Item {
   private final String tooltipKey;
   private final String secondaryTooltipKey;

   public TooltipItem(Properties properties, String tooltipKey) {
      this(properties, tooltipKey, null);
   }

   public TooltipItem(Properties properties, String tooltipKey, @Nullable String secondaryTooltipKey) {
      super(properties);
      this.tooltipKey = tooltipKey;
      this.secondaryTooltipKey = secondaryTooltipKey;
   }

   @Override
   public void appendHoverText(ItemStack stack, Item.TooltipContext level, List<Component> tooltip, TooltipFlag flag) {
      if (this.tooltipKey != null && !this.tooltipKey.isEmpty()) {
         tooltip.add(Component.translatable(this.tooltipKey).withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
      }

      if (this.secondaryTooltipKey != null && !this.secondaryTooltipKey.isEmpty()) {
         tooltip.add(Component.translatable(this.secondaryTooltipKey).withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC));
      }

      super.appendHoverText(stack, level, tooltip, flag);
   }
}
