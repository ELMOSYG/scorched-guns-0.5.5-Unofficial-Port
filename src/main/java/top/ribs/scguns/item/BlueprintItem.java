package top.ribs.scguns.item;


import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.client.screen.BlueprintScreen;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.C2SMessageClearBlueprintRecipe;

public class BlueprintItem extends Item {
   public BlueprintItem(Properties properties) {
      this(properties, "");
   }

   public BlueprintItem(Properties properties, String blueprintType) {
      super(properties);
   }

   public boolean hasCraftingRemainingItem(ItemStack stack) {
      return true;
   }

   public ItemStack getCraftingRemainingItem(ItemStack stack) {
      return stack.copy();
   }

   @NotNull
   public InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
      ItemStack itemstack = player.getItemInHand(hand);
      if (level.isClientSide) {
         if (player.isShiftKeyDown()) {
            this.clearBlueprintRecipe(hand);
         } else {
            this.openBlueprintScreen(itemstack, player, hand);
         }
      }

      return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
   }

   @OnlyIn(Dist.CLIENT)
   private void openBlueprintScreen(ItemStack blueprintStack, Player player, InteractionHand hand) {
      Minecraft.getInstance().setScreen(new BlueprintScreen(blueprintStack, player, hand));
   }

   @OnlyIn(Dist.CLIENT)
   private void clearBlueprintRecipe(InteractionHand hand) {
      PacketHandler.getPlayChannel().sendToServer(new C2SMessageClearBlueprintRecipe(hand));
   }

   @Override
   public void appendHoverText(ItemStack pStack, Item.TooltipContext pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
      super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
      pTooltipComponents.add(
         Component.translatable("item.scguns.blueprint.tooltip.right_click").withStyle(new ChatFormatting[]{ChatFormatting.BLUE, ChatFormatting.ITALIC})
      );
      if (pLevel.level() != null && pLevel.level().isClientSide) {
         String activeRecipeName = BlueprintScreen.getActiveRecipeName(pStack);
         if (activeRecipeName != null) {
            pTooltipComponents.add(
               Component.translatable("item.scguns.blueprint.tooltip.active_recipe", new Object[]{activeRecipeName})
                  .withStyle(new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.ITALIC})
            );
            pTooltipComponents.add(
               Component.translatable("item.scguns.blueprint.tooltip.shift_right_click_clear")
                  .withStyle(new ChatFormatting[]{ChatFormatting.YELLOW, ChatFormatting.ITALIC})
            );
         }
      }
   }
}
