package top.ribs.scguns.item;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FuelAmmoItem extends Item implements IAmmo {
   private final List<MobEffectInstance> potionEffects;
   private final Supplier<Item> containerItem;

   public FuelAmmoItem(Properties properties, Supplier<Item> containerItem, MobEffectInstance... potionEffects) {
      super(properties);
      this.containerItem = containerItem;
      this.potionEffects = Arrays.stream(potionEffects).filter(Objects::nonNull).toList();
   }

   public boolean hasCraftingRemainingItem(ItemStack stack) {
      return true;
   }

   public ItemStack getCraftingRemainingItem(ItemStack itemStack) {
      return new ItemStack((ItemLike)this.containerItem.get());
   }

   public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
      return ItemUtils.startUsingInstantly(world, player, hand);
   }

   @NotNull
   public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entityLiving) {
      if (entityLiving instanceof Player player && !world.isClientSide) {
         for (MobEffectInstance effect : this.potionEffects) {
            if (effect != null) {
               player.addEffect(new MobEffectInstance(effect));
            }
         }

         if (!player.getAbilities().instabuild) {
            ItemStack containerStack = new ItemStack((ItemLike)this.containerItem.get());
            if (stack.getCount() > 1) {
               stack.shrink(1);
               if (!player.getInventory().add(containerStack)) {
                  player.drop(containerStack, false);
               }

               return stack;
            }

            return containerStack;
         }
      }

      return stack;
   }

   @Override
   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 32;
   }

   @NotNull
   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.DRINK;
   }

   @Override
   public void appendHoverText(@NotNull ItemStack stack, Item.TooltipContext world, List<Component> tooltip, TooltipFlag flag) {
      if (!this.potionEffects.isEmpty()) {
         tooltip.add(Component.translatable("item.tooltip.fuel_effects").withStyle(ChatFormatting.GRAY));

         for (MobEffectInstance effect : this.potionEffects) {
            if (effect != null) {
               effect.getEffect();
               Component effectName = Component.translatable(effect.getEffect().value().getDescriptionId()).withStyle(ChatFormatting.BLUE);
               int durationInSeconds = effect.getDuration() / 20;
               int minutes = durationInSeconds / 60;
               int seconds = durationInSeconds % 60;
               String formattedDuration = String.format(" (%02d:%02d)", minutes, seconds);
               Component effectDuration = Component.literal(formattedDuration).withStyle(ChatFormatting.BLUE);
               tooltip.add(Component.empty().append(effectName).append(effectDuration));
            }
         }
      }

      super.appendHoverText(stack, world, tooltip, flag);
   }
}
