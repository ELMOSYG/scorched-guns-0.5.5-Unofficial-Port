package top.ribs.scguns.item;



import net.minecraft.world.item.Item;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class TooltipAmmo extends AmmoItem {
   private final int armorBypassAmount;
   private final String descriptionKey;

   public TooltipAmmo(Properties properties, int armorBypassAmount) {
      super(properties);
      this.armorBypassAmount = armorBypassAmount;
      this.descriptionKey = null;
   }

   public TooltipAmmo(Properties properties, String descriptionKey) {
      super(properties);
      this.armorBypassAmount = -1;
      this.descriptionKey = descriptionKey;
   }

   @Override
   public void appendHoverText(ItemStack stack, Item.TooltipContext level, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, level, tooltip, flag);
      if (this.armorBypassAmount >= 0) {
         tooltip.add(this.getArmorBypassTooltip());
      }

      if (this.descriptionKey != null) {
         tooltip.add(this.getDescriptionTooltip());
      }
   }

   private Component getArmorBypassTooltip() {
      return Component.translatable("tooltip.scguns.armor_bypass", new Object[]{this.armorBypassAmount})
         .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC});
   }

   private Component getDescriptionTooltip() {
      assert this.descriptionKey != null;

      return Component.translatable(this.descriptionKey).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC});
   }
}
