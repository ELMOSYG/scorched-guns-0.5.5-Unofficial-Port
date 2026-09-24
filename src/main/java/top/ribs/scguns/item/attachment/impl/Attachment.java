package top.ribs.scguns.item.attachment.impl;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.ribs.scguns.common.GunModifiers;
import top.ribs.scguns.interfaces.IGunModifier;
import top.ribs.scguns.item.attachment.IAttachment;

@EventBusSubscriber(
   modid = "scguns",
   value = {Dist.CLIENT}
)
public abstract class Attachment {
   protected IGunModifier[] modifiers;
   private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("0.#");
   private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##");

   Attachment(IGunModifier... modifiers) {
      super();
      this.modifiers = modifiers;
   }

   public IGunModifier[] getModifiers() {
      return this.modifiers;
   }

   @OnlyIn(Dist.CLIENT)
   @SubscribeEvent
   public static void addInformationEvent(ItemTooltipEvent event) {
      ItemStack stack = event.getItemStack();
      if (stack.getItem() instanceof IAttachment) {
         IAttachment<?> attachment = (IAttachment<?>)stack.getItem();
         List<Component> enhancedTooltips = generateEnhancedTooltips(attachment);
         if (!enhancedTooltips.isEmpty()) {
            event.getToolTip()
               .add(Component.translatable("tooltip.scguns.attachment.stats").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.BOLD}));
            event.getToolTip().addAll(enhancedTooltips);
         }
      }
   }

   private static List<Component> generateEnhancedTooltips(IAttachment<?> attachment) {
      List<Component> tooltips = new ArrayList<>();
      IGunModifier[] modifiers = attachment.getProperties().getModifiers();
      Attachment.AttachmentStats stats = analyzeAttachmentStats(modifiers);
      addDamageTooltips(tooltips, stats);
      addAccuracyTooltips(tooltips, stats);
      addHandlingTooltips(tooltips, stats);
      addSpecialTooltips(tooltips, stats);
      return tooltips;
   }

   private static Attachment.AttachmentStats analyzeAttachmentStats(IGunModifier[] modifiers) {
      Attachment.AttachmentStats stats = new Attachment.AttachmentStats();
      float baseDamage = 10.0F;
      float baseSpread = 2.0F;
      double baseSpeed = 20.0;
      double baseAdsSpeed = 1.0;
      float baseRecoil = 1.0F;
      float baseKick = 1.0F;
      int baseRate = 10;
      double baseReloadSpeed = 1.0;
      int baseAmmoCapacity = 30;
      float baseFalloffRange = 20.0F;

      for (IGunModifier modifier : modifiers) {
         stats.additionalDamage = stats.additionalDamage + modifier.additionalDamage();
         baseDamage = modifier.modifyProjectileDamage(baseDamage);
         stats.criticalChance = stats.criticalChance + modifier.criticalChance();
         baseSpread = modifier.modifyProjectileSpread(baseSpread);
         baseSpeed = modifier.modifyProjectileSpeed(baseSpeed);
         baseAdsSpeed = modifier.modifyAimDownSightSpeed(baseAdsSpeed);
         baseRate = modifier.modifyFireRate(baseRate);
         baseReloadSpeed = modifier.modifyReloadSpeed(baseReloadSpeed);
         stats.ammoCapacity = modifier.modifyAmmoCapacity(baseAmmoCapacity);
         baseFalloffRange = modifier.modifyDamageFalloffStart(baseFalloffRange);
         if (modifier == GunModifiers.EXTENDED_BARREL_MODIFIER) {
            baseRecoil *= 1.15F;
            baseKick *= 1.2F;
         } else {
            baseRecoil *= modifier.recoilModifier();
            baseKick *= modifier.kickModifier();
         }

         if (modifier == GunModifiers.BUMP_STOCK_MODIFIER) {
            stats.hasDurabilityPenalty = true;
            stats.isFireRateWeaponDependent = true;
         }

         if (modifier.silencedFire()) {
            stats.silenced = true;
         }
      }

      stats.damageMultiplier = (baseDamage - 10.0F) / 10.0F;
      stats.spreadReduction = (2.0F - baseSpread) / 2.0F;
      stats.speedMultiplier = (baseSpeed - 20.0) / 20.0;
      stats.adsSpeedMultiplier = baseAdsSpeed - 1.0;
      stats.recoilReduction = 1.0F - baseRecoil;
      stats.kickReduction = 1.0F - baseKick;
      if (baseRate != 10) {
         float baseRPM = 120.0F;
         float modifiedRPM = 1200.0F / (float)baseRate;
         stats.fireRateChange = (modifiedRPM - baseRPM) / baseRPM;
      }

      stats.reloadSpeedChange = 1.0 - baseReloadSpeed;
      stats.capacityMultiplier = (float)(stats.ammoCapacity - baseAmmoCapacity) / (float)baseAmmoCapacity;
      stats.rangeMultiplier = (baseFalloffRange - 20.0F) / 20.0F;
      return stats;
   }

   private static void addDamageTooltips(List<Component> tooltips, Attachment.AttachmentStats stats) {
      if (stats.additionalDamage != 0.0F) {
         String damageText = (stats.additionalDamage > 0.0F ? "+" : "") + DECIMAL_FORMAT.format((double)stats.additionalDamage / 2.0);
         Component tooltip = Component.translatable("tooltip.scguns.attachment.damage.additional", new Object[]{damageText})
            .withStyle(stats.additionalDamage > 0.0F ? ChatFormatting.GREEN : ChatFormatting.RED);
         tooltips.add(tooltip);
      }

      if (Math.abs(stats.damageMultiplier) > 0.001F) {
         String percentText = (stats.damageMultiplier > 0.0F ? "+" : "") + PERCENTAGE_FORMAT.format((double)(stats.damageMultiplier * 100.0F)) + "%";
         Component tooltip = Component.translatable("tooltip.scguns.attachment.damage.multiplier", new Object[]{percentText})
            .withStyle(stats.damageMultiplier > 0.0F ? ChatFormatting.GREEN : ChatFormatting.RED);
         tooltips.add(tooltip);
      }

      if (stats.criticalChance > 0.0F) {
         String critText = "+" + PERCENTAGE_FORMAT.format((double)(stats.criticalChance * 100.0F)) + "%";
         Component tooltip = Component.translatable("tooltip.scguns.attachment.critical_chance", new Object[]{critText}).withStyle(ChatFormatting.YELLOW);
         tooltips.add(tooltip);
      }
   }

   private static void addAccuracyTooltips(List<Component> tooltips, Attachment.AttachmentStats stats) {
      if (Math.abs(stats.spreadReduction) > 0.001F) {
         String spreadText = (stats.spreadReduction > 0.0F ? "-" : "+") + PERCENTAGE_FORMAT.format((double)Math.abs(stats.spreadReduction * 100.0F)) + "%";
         Component tooltip = Component.translatable("tooltip.scguns.attachment.spread", new Object[]{spreadText})
            .withStyle(stats.spreadReduction > 0.0F ? ChatFormatting.GREEN : ChatFormatting.RED);
         tooltips.add(tooltip);
      }

      if (Math.abs(stats.speedMultiplier) > 0.001) {
         String speedText = (stats.speedMultiplier > 0.0 ? "+" : "") + PERCENTAGE_FORMAT.format(stats.speedMultiplier * 100.0) + "%";
         Component tooltip = Component.translatable("tooltip.scguns.attachment.projectile_speed", new Object[]{speedText})
            .withStyle(stats.speedMultiplier > 0.0 ? ChatFormatting.GREEN : ChatFormatting.RED);
         tooltips.add(tooltip);
      }

      if (Math.abs(stats.rangeMultiplier) > 0.001F) {
         String rangeText = "+" + PERCENTAGE_FORMAT.format((double)(stats.rangeMultiplier * 100.0F)) + "%";
         Component tooltip = Component.translatable("tooltip.scguns.attachment.effective_range", new Object[]{rangeText}).withStyle(ChatFormatting.GREEN);
         tooltips.add(tooltip);
      }
   }

   private static void addHandlingTooltips(List<Component> tooltips, Attachment.AttachmentStats stats) {
      if (Math.abs(stats.adsSpeedMultiplier) > 0.001) {
         String adsText = (stats.adsSpeedMultiplier > 0.0 ? "+" : "") + PERCENTAGE_FORMAT.format(stats.adsSpeedMultiplier * 100.0) + "%";
         Component tooltip = Component.translatable("tooltip.scguns.attachment.ads_speed", new Object[]{adsText})
            .withStyle(stats.adsSpeedMultiplier > 0.0 ? ChatFormatting.GREEN : ChatFormatting.RED);
         tooltips.add(tooltip);
      }

      if (Math.abs(stats.recoilReduction) > 0.001F) {
         String recoilText = (stats.recoilReduction > 0.0F ? "-" : "+") + PERCENTAGE_FORMAT.format((double)Math.abs(stats.recoilReduction * 100.0F)) + "%";
         Component tooltip = Component.translatable("tooltip.scguns.attachment.recoil", new Object[]{recoilText})
            .withStyle(stats.recoilReduction > 0.0F ? ChatFormatting.GREEN : ChatFormatting.RED);
         tooltips.add(tooltip);
      }

      if (Math.abs(stats.kickReduction) > 0.001F) {
         String kickText = (stats.kickReduction > 0.0F ? "-" : "+") + PERCENTAGE_FORMAT.format((double)Math.abs(stats.kickReduction * 100.0F)) + "%";
         Component tooltip = Component.translatable("tooltip.scguns.attachment.kick", new Object[]{kickText})
            .withStyle(stats.kickReduction > 0.0F ? ChatFormatting.GREEN : ChatFormatting.RED);
         tooltips.add(tooltip);
      }

      if (Math.abs(stats.fireRateChange) > 0.001F) {
         if (stats.isFireRateWeaponDependent) {
            Component tooltip = Component.translatable("tooltip.scguns.attachment.fire_rate.increased").withStyle(ChatFormatting.GREEN);
            tooltips.add(tooltip);
         } else {
            String rateText = (stats.fireRateChange > 0.0F ? "+" : "") + PERCENTAGE_FORMAT.format((double)(stats.fireRateChange * 100.0F)) + "%";
            Component tooltip = Component.translatable("tooltip.scguns.attachment.fire_rate", new Object[]{rateText})
               .withStyle(stats.fireRateChange > 0.0F ? ChatFormatting.GREEN : ChatFormatting.RED);
            tooltips.add(tooltip);
         }
      }

      if (Math.abs(stats.reloadSpeedChange) > 0.001) {
         String reloadText = (stats.reloadSpeedChange > 0.0 ? "+" : "") + PERCENTAGE_FORMAT.format(stats.reloadSpeedChange * 100.0) + "%";
         Component tooltip = Component.translatable("tooltip.scguns.attachment.reload_speed", new Object[]{reloadText})
            .withStyle(stats.reloadSpeedChange > 0.0 ? ChatFormatting.GREEN : ChatFormatting.RED);
         tooltips.add(tooltip);
      }

      if ((double)Math.abs(stats.capacityMultiplier) > 0.001) {
         String capacityText = (stats.capacityMultiplier > 0.0F ? "+" : "") + PERCENTAGE_FORMAT.format((double)(stats.capacityMultiplier * 100.0F)) + "%";
         Component tooltip = Component.translatable("tooltip.scguns.attachment.ammo_capacity", new Object[]{capacityText})
            .withStyle(stats.capacityMultiplier > 0.0F ? ChatFormatting.GREEN : ChatFormatting.RED);
         tooltips.add(tooltip);
      }
   }

   private static void addSpecialTooltips(List<Component> tooltips, Attachment.AttachmentStats stats) {
      if (stats.silenced) {
         Component tooltip = Component.translatable("tooltip.scguns.attachment.silenced").withStyle(ChatFormatting.AQUA);
         tooltips.add(tooltip);
      }

      if (stats.hasDurabilityPenalty) {
         Component tooltip = Component.translatable("tooltip.scguns.attachment.durability_penalty").withStyle(ChatFormatting.GOLD);
         tooltips.add(tooltip);
      }
   }

   private static class AttachmentStats {
      float additionalDamage = 0.0F;
      float damageMultiplier = 0.0F;
      float criticalChance = 0.0F;
      float spreadReduction = 0.0F;
      double speedMultiplier = 0.0;
      double adsSpeedMultiplier = 0.0;
      float recoilReduction = 0.0F;
      float kickReduction = 0.0F;
      float fireRateChange = 0.0F;
      double reloadSpeedChange = 0.0;
      int ammoCapacity = 30;
      float capacityMultiplier = 0.0F;
      boolean silenced = false;
      float rangeMultiplier = 0.0F;
      boolean hasDurabilityPenalty = false;
      boolean isFireRateWeaponDependent = false;

      private AttachmentStats() {
         super();
      }
   }
}
