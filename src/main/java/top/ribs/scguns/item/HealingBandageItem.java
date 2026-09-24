package top.ribs.scguns.item;


import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.init.ModEffects;

public class HealingBandageItem extends Item {
   int healingAmount;
   List<MobEffectInstance> potionEffects;

   public HealingBandageItem(Properties properties, int healingAmount, MobEffectInstance... potionEffects) {
      super(properties);
      this.potionEffects = Arrays.stream(potionEffects).filter(Objects::nonNull).toList();
      this.healingAmount = healingAmount;
   }

   public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
      return ItemUtils.startUsingInstantly(world, player, hand);
   }

   @NotNull
   public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entityLiving) {
      if (entityLiving instanceof Player player && !world.isClientSide) {
         player.heal((float)this.healingAmount);
         if (player.hasEffect(ModEffects.LACERATED)) {
            player.removeEffect(ModEffects.LACERATED);
         }

         for (MobEffectInstance effect : this.potionEffects) {
            if (effect != null) {
               player.addEffect(new MobEffectInstance(effect));
            }
         }

         if (!player.getAbilities().instabuild) {
            stack.shrink(1);
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
      return UseAnim.BRUSH;
   }

   @Override
   public void appendHoverText(ItemStack stack, Item.TooltipContext world, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.scguns.healing_bandage.heal", new Object[]{this.healingAmount}).withStyle(ChatFormatting.GREEN));
      if (!this.potionEffects.isEmpty()) {
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
