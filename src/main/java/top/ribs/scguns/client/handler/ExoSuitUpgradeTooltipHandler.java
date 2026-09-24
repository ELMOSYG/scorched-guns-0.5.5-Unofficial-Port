package top.ribs.scguns.client.handler;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import org.lwjgl.glfw.GLFW;
import top.ribs.scguns.common.exosuit.ExoSuitUpgrade;
import top.ribs.scguns.common.exosuit.ExoSuitUpgradeManager;
import top.ribs.scguns.item.animated.ExoSuitItem;
import top.ribs.scguns.item.exosuit.EnergyUpgradeItem;
import top.ribs.scguns.item.exosuit.GasMaskModuleItem;
import top.ribs.scguns.item.exosuit.RebreatherModuleItem;
import top.ribs.scguns.item.exosuit.TargetTrackerModuleItem;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME,
   value = {Dist.CLIENT}
)
public class ExoSuitUpgradeTooltipHandler {
   public ExoSuitUpgradeTooltipHandler() {
      super();
   }

   @SubscribeEvent
   public static void onItemTooltip(ItemTooltipEvent event) {
      ItemStack stack = event.getItemStack();
      ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(stack);
      if (upgrade != null) {
         List<Component> tooltip = event.getToolTip();
         boolean showDetailed = isShiftPressed();
         addUpgradeTooltip(tooltip, upgrade, stack, showDetailed);
      }
   }

   private static boolean isShiftPressed() {
      return GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), 340) == 1 || GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), 344) == 1;
   }

   private static void addUpgradeTooltip(List<Component> tooltip, ExoSuitUpgrade upgrade, ItemStack stack, boolean showDetailed) {
      tooltip.add(Component.literal(""));
      String upgradeType = upgrade.getType();
      String formattedType = formatUpgradeType(upgradeType);
      tooltip.add(Component.translatable("tooltip.scguns.exosuit.upgrade.type").append(formattedType).withStyle(ChatFormatting.GRAY));
      addSlotCompatibility(tooltip, upgradeType);
      if (!showDetailed) {
         tooltip.add(Component.literal(""));
         tooltip.add(Component.translatable("tooltip.scguns.upgrade.hold_shift").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}));
      } else {
         addSpecialFunctionality(tooltip, stack, upgrade);
         addEffectsTooltip(tooltip, upgrade.getEffects());
         if (stack.getItem() instanceof EnergyUpgradeItem energyUpgrade) {
            addEnergyTooltip(tooltip, energyUpgrade);
         }

         tooltip.add(Component.literal(""));
         tooltip.add(
            Component.translatable("tooltip.scguns.upgrade.install_hint").withStyle(new ChatFormatting[]{ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC})
         );
      }
   }

   private static void addEffectsTooltip(List<Component> tooltip, ExoSuitUpgrade.Effects effects) {
      if (effects.getArmorBonus() > 0.0F || effects.getArmorToughness() > 0.0F || effects.getKnockbackResistance() > 0.0F) {
         tooltip.add(Component.literal(""));
         tooltip.add(Component.translatable("tooltip.scguns.upgrade.effects.defensive").withStyle(ChatFormatting.GRAY));
         if (effects.getArmorBonus() > 0.0F) {
            tooltip.add(
               Component.literal(" • ")
                  .append(Component.translatable("tooltip.scguns.exosuit.upgrade.stat.armor", new Object[]{effects.getArmorBonus()}))
                  .withStyle(ChatFormatting.GRAY)
            );
         }

         if (effects.getArmorToughness() > 0.0F) {
            tooltip.add(
               Component.literal(" • ")
                  .append(Component.translatable("tooltip.scguns.exosuit.upgrade.stat.armor_toughness", new Object[]{effects.getArmorToughness()}))
                  .withStyle(ChatFormatting.GRAY)
            );
         }

         if (effects.getKnockbackResistance() > 0.0F) {
            tooltip.add(
               Component.literal(" • ")
                  .append(
                     Component.translatable(
                        "tooltip.scguns.exosuit.upgrade.stat.knockback_resistance", new Object[]{(int)(effects.getKnockbackResistance() * 100.0F)}
                     )
                  )
                  .withStyle(ChatFormatting.GRAY)
            );
         }
      }

      if (effects.getSpeedModifier() != 0.0F || effects.getJumpBoost() > 0.0F || effects.getFallDamageReduction() > 0.0F) {
         tooltip.add(Component.literal(""));
         tooltip.add(Component.translatable("tooltip.scguns.upgrade.effects.mobility").withStyle(ChatFormatting.GRAY));
         if (effects.getSpeedModifier() != 0.0F) {
            String speedKey = effects.getSpeedModifier() > 0.0F
               ? "tooltip.scguns.exosuit.upgrade.stat.speed_positive"
               : "tooltip.scguns.exosuit.upgrade.stat.speed_negative";
            ChatFormatting color = effects.getSpeedModifier() > 0.0F ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY;
            tooltip.add(
               Component.literal(" • ")
                  .append(Component.translatable(speedKey, new Object[]{Math.abs((int)(effects.getSpeedModifier() * 100.0F))}))
                  .withStyle(color)
            );
         }

         if (effects.getJumpBoost() > 0.0F) {
            tooltip.add(
               Component.literal(" • ")
                  .append(Component.translatable("tooltip.scguns.exosuit.upgrade.stat.jump", new Object[]{(int)(effects.getJumpBoost() * 100.0F)}))
                  .withStyle(ChatFormatting.GRAY)
            );
         }

         if (effects.getFallDamageReduction() > 0.0F) {
            tooltip.add(
               Component.literal(" • ")
                  .append(
                     Component.translatable(
                        "tooltip.scguns.exosuit.upgrade.stat.fall_damage_reduction", new Object[]{(int)(effects.getFallDamageReduction() * 100.0F)}
                     )
                  )
                  .withStyle(ChatFormatting.GRAY)
            );
         }
      }

      if (effects.getRecoilAngleReduction() > 0.0F || effects.getRecoilKickReduction() > 0.0F || effects.getSpreadReduction() > 0.0F) {
         tooltip.add(Component.literal(""));
         tooltip.add(Component.translatable("tooltip.scguns.upgrade.effects.combat").withStyle(ChatFormatting.GRAY));
         if (effects.getRecoilAngleReduction() > 0.0F) {
            tooltip.add(
               Component.literal(" • ")
                  .append(
                     Component.translatable("tooltip.scguns.exosuit.upgrade.stat.recoil_angle", new Object[]{(int)(effects.getRecoilAngleReduction() * 100.0F)})
                  )
                  .withStyle(ChatFormatting.GRAY)
            );
         }

         if (effects.getRecoilKickReduction() > 0.0F) {
            tooltip.add(
               Component.literal(" • ")
                  .append(
                     Component.translatable("tooltip.scguns.exosuit.upgrade.stat.recoil_kick", new Object[]{(int)(effects.getRecoilKickReduction() * 100.0F)})
                  )
                  .withStyle(ChatFormatting.GRAY)
            );
         }

         if (effects.getSpreadReduction() > 0.0F) {
            tooltip.add(
               Component.literal(" • ")
                  .append(
                     Component.translatable("tooltip.scguns.exosuit.upgrade.stat.spread_reduction", new Object[]{(int)(effects.getSpreadReduction() * 100.0F)})
                  )
                  .withStyle(ChatFormatting.GRAY)
            );
         }
      }

      if (effects.hasNightVision()) {
         tooltip.add(Component.literal(""));
         tooltip.add(Component.translatable("tooltip.scguns.upgrade.effects.special").withStyle(ChatFormatting.GRAY));
         tooltip.add(Component.literal(" • ").append(Component.translatable("tooltip.scguns.exosuit.upgrade.stat.night_vision")).withStyle(ChatFormatting.GRAY));
      }
   }

   private static void addSlotCompatibility(List<Component> tooltip, String upgradeType) {
      String slotKey = switch (upgradeType) {
         case "pouches" -> "tooltip.scguns.exosuit.slots.chest_only";
         case "utility" -> "tooltip.scguns.exosuit.slots.chest_legs_utility";
         case "knee_guard" -> "tooltip.scguns.exosuit.slots.legs_only";
         case "mobility" -> "tooltip.scguns.exosuit.slots.boots_only";
         case "hud", "breathing" -> "tooltip.scguns.exosuit.slots.helmet_only";
         case "pauldron" -> "tooltip.scguns.exosuit.slots.chest_shoulders";
         case "plating" -> "tooltip.scguns.exosuit.slots.helmet_chest_legs";
         case "power_core" -> "tooltip.scguns.exosuit.slots.chest_power";
         default -> null;
      };
      if (slotKey != null) {
         tooltip.add(
            Component.translatable("tooltip.scguns.exosuit.compatible_slots")
               .withStyle(ChatFormatting.DARK_GRAY)
               .append(Component.translatable(slotKey).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}))
         );
      }
   }

   private static void addSpecialFunctionality(List<Component> tooltip, ItemStack stack, ExoSuitUpgrade upgrade) {
      if (stack.getItem() instanceof RebreatherModuleItem) {
         tooltip.add(Component.literal(""));
         tooltip.add(Component.translatable("tooltip.scguns.exosuit.functionality.header").withStyle(ChatFormatting.GRAY));
         tooltip.add(
            Component.literal(" • ")
               .append(Component.translatable("tooltip.scguns.exosuit.functionality.rebreather.water_breathing"))
               .withStyle(ChatFormatting.GRAY)
         );
         tooltip.add(
            Component.literal(" • ")
               .append(Component.translatable("tooltip.scguns.exosuit.functionality.rebreather.unlimited_exploration"))
               .withStyle(ChatFormatting.GRAY)
         );
      } else if (stack.getItem() instanceof TargetTrackerModuleItem) {
         tooltip.add(Component.literal(""));
         tooltip.add(Component.translatable("tooltip.scguns.exosuit.functionality.header").withStyle(ChatFormatting.GRAY));
         tooltip.add(
            Component.literal(" • ")
               .append(Component.translatable("tooltip.scguns.exosuit.functionality.target_tracker.highlights"))
               .withStyle(ChatFormatting.GRAY)
         );
         tooltip.add(
            Component.literal(" • ")
               .append(Component.translatable("tooltip.scguns.exosuit.functionality.target_tracker.accuracy"))
               .withStyle(ChatFormatting.GRAY)
         );
      } else if (stack.getItem() instanceof GasMaskModuleItem) {
         tooltip.add(Component.literal(""));
         tooltip.add(Component.translatable("tooltip.scguns.exosuit.functionality.header").withStyle(ChatFormatting.GRAY));
         tooltip.add(
            Component.literal(" • ")
               .append(Component.translatable("tooltip.scguns.exosuit.functionality.gas_mask.toxic_protection"))
               .withStyle(ChatFormatting.GRAY)
         );
         tooltip.add(
            Component.literal(" • ")
               .append(Component.translatable("tooltip.scguns.exosuit.functionality.gas_mask.environmental_filter"))
               .withStyle(ChatFormatting.GRAY)
         );
      } else if (upgrade.getType().equals("pouches")) {
         tooltip.add(Component.literal(""));
         tooltip.add(Component.translatable("tooltip.scguns.exosuit.functionality.header").withStyle(ChatFormatting.GRAY));
         int storageSize = upgrade.getDisplay().getStorageSize();
         tooltip.add(
            Component.literal(" • ")
               .append(Component.translatable("tooltip.scguns.exosuit.functionality.pouches.storage", new Object[]{storageSize}))
               .withStyle(ChatFormatting.GRAY)
         );
         addPouchContentStatus(tooltip, stack);
      }
   }

   private static void addPouchContentStatus(List<Component> tooltip, ItemStack pouchStack) {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         for (ItemStack armorStack : player.getArmorSlots()) {
            if (armorStack.getItem() instanceof ExoSuitItem exosuit && exosuit.getType() == Type.CHESTPLATE) {
               break;
            }
         }
      }
   }

   private static void addEnergyTooltip(List<Component> tooltip, EnergyUpgradeItem energyUpgrade) {
      tooltip.add(Component.literal(""));
      tooltip.add(Component.translatable("tooltip.scguns.exosuit.energy.header").withStyle(ChatFormatting.GRAY));

      String consumptionKey = switch (energyUpgrade.getConsumptionType()) {
         case PER_TICK -> "tooltip.scguns.exosuit.energy.consumption.per_tick";
         case PER_USE -> "tooltip.scguns.exosuit.energy.consumption.per_use";
         case PER_SECOND -> "tooltip.scguns.exosuit.energy.consumption.per_second";
         case ACTIVATION -> "tooltip.scguns.exosuit.energy.consumption.activation";
      };
      tooltip.add(
         Component.literal(" • ")
            .append(Component.translatable("tooltip.scguns.exosuit.energy.consumption.label"))
            .withStyle(ChatFormatting.GRAY)
            .append(Component.translatable(consumptionKey, new Object[]{energyUpgrade.getEnergyConsumption()}).withStyle(ChatFormatting.GRAY))
      );
      if (!energyUpgrade.canFunctionWithoutPower()) {
         tooltip.add(
            Component.literal(" • ")
               .append(Component.translatable("tooltip.scguns.exosuit.energy.requires_power"))
               .withStyle(new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.ITALIC})
         );
      }
   }

   private static String formatUpgradeType(String type) {
      return switch (type) {
         case "plating" -> Component.translatable("tooltip.scguns.exosuit.upgrade.plating").getString();
         case "hud" -> Component.translatable("tooltip.scguns.exosuit.upgrade.hud").getString();
         case "breathing" -> Component.translatable("tooltip.scguns.exosuit.upgrade.breathing").getString();
         case "pauldron" -> Component.translatable("tooltip.scguns.exosuit.upgrade.pauldron").getString();
         case "power_core" -> Component.translatable("tooltip.scguns.exosuit.upgrade.power_core").getString();
         case "utility" -> Component.translatable("tooltip.scguns.exosuit.upgrade.utility").getString();
         case "pouches" -> Component.translatable("tooltip.scguns.exosuit.upgrade.pouches").getString();
         case "knee_guard" -> Component.translatable("tooltip.scguns.exosuit.upgrade.knee_guard").getString();
         case "mobility" -> Component.translatable("tooltip.scguns.exosuit.upgrade.mobility").getString();
         default -> type.substring(0, 1).toUpperCase() + type.substring(1).replace("_", " ");
      };
   }
}
