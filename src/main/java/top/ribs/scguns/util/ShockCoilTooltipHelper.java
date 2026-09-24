package top.ribs.scguns.util;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.ribs.scguns.config.ShockCoilConfig;

public class ShockCoilTooltipHelper {
   public ShockCoilTooltipHelper() {
      super();
   }

   public static void addShockCoilTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(
         Component.translatable("info.scguns.shock_coil.damage")
            .append(": ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.1f", ShockCoilConfig.getBaseDamage())).withStyle(ChatFormatting.WHITE))
      );
      double zapsPerSecond = 20.0 / (double)ShockCoilConfig.getZapCooldown();
      tooltip.add(
         Component.translatable("info.scguns.shock_coil.fire_rate")
            .append(": ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.2f", zapsPerSecond)).withStyle(ChatFormatting.WHITE))
            .append(Component.translatable("info.scguns.shock_coil.zaps_per_second").withStyle(ChatFormatting.WHITE))
      );
      if (Screen.hasShiftDown()) {
         if (!ShockCoilConfig.getStatusEffects().isEmpty()) {
            tooltip.add(Component.literal(""));
            tooltip.add(
               Component.translatable("info.scguns.shock_coil.range")
                  .append(": ")
                  .withStyle(ChatFormatting.GRAY)
                  .append(Component.literal(String.format("%.1f", ShockCoilConfig.getZapRange())).withStyle(ChatFormatting.WHITE))
            );
            tooltip.add(
               Component.translatable("info.scguns.shock_coil.energy_per_zap")
                  .append(": ")
                  .withStyle(ChatFormatting.GRAY)
                  .append(Component.literal(String.valueOf(ShockCoilConfig.getEnergyPerZap())).withStyle(ChatFormatting.AQUA))
                  .append(Component.literal(" FE").withStyle(ChatFormatting.AQUA))
            );
            tooltip.add(
               Component.translatable("info.scguns.shock_coil.max_energy")
                  .append(": ")
                  .withStyle(ChatFormatting.GRAY)
                  .append(Component.literal(String.valueOf(ShockCoilConfig.getMaxEnergy())).withStyle(ChatFormatting.AQUA))
                  .append(Component.literal(" FE").withStyle(ChatFormatting.AQUA))
            );
            tooltip.add(
               Component.translatable("info.scguns.shock_coil.max_targets")
                  .append(": ")
                  .withStyle(ChatFormatting.GRAY)
                  .append(Component.literal(String.valueOf(ShockCoilConfig.getMaxTargetsPerZap())).withStyle(ChatFormatting.WHITE))
            );
            tooltip.add(Component.translatable("info.scguns.shock_coil.status_effects").withStyle(ChatFormatting.GRAY));

            for (ShockCoilConfig.StatusEffect statusEffect : ShockCoilConfig.getStatusEffects()) {
               float chancePercent = statusEffect.getChance() * 100.0F;
               int durationSeconds = statusEffect.getDuration() / 20;
               tooltip.add(
                  Component.literal("  ")
                     .append(Component.translatable(statusEffect.getEffect().getDescriptionId()).withStyle(ChatFormatting.WHITE))
                     .append(Component.literal(" " + (statusEffect.getAmplifier() + 1)).withStyle(ChatFormatting.WHITE))
                     .append(Component.literal(" (" + durationSeconds + "s").withStyle(ChatFormatting.GRAY))
                     .append(Component.literal(" - " + String.format("%.0f", chancePercent) + "%)").withStyle(ChatFormatting.GRAY))
               );
            }
         }

         tooltip.add(Component.literal(""));
         tooltip.add(Component.translatable("info.scguns.shock_coil.targeting_info").withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC));
         tooltip.add(Component.translatable("info.scguns.shock_coil.redstone_info").withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC));
      } else {
         tooltip.add(Component.translatable("info.scguns.shock_coil.shift_details").withStyle(ChatFormatting.GRAY));
      }
   }
}
