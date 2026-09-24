package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import top.ribs.scguns.common.exosuit.ExoSuitData;
import top.ribs.scguns.common.exosuit.ExoSuitFlightHandler;
import top.ribs.scguns.common.exosuit.ExoSuitPowerManager;
import top.ribs.scguns.common.exosuit.ExoSuitUpgrade;
import top.ribs.scguns.common.exosuit.ExoSuitUpgradeManager;
import top.ribs.scguns.item.animated.ExoSuitItem;
import top.ribs.scguns.item.exosuit.GasMaskModuleItem;
import top.ribs.scguns.item.exosuit.NightVisionModuleItem;
import top.ribs.scguns.item.exosuit.RebreatherModuleItem;
import top.ribs.scguns.item.exosuit.TargetTrackerModuleItem;

public class C2SMessageToggleExoSuitPower {
   private C2SMessageToggleExoSuitPower.PowerType powerType;

   public C2SMessageToggleExoSuitPower() {
      super();
   }

   public C2SMessageToggleExoSuitPower(C2SMessageToggleExoSuitPower.PowerType powerType) {
      super();
      this.powerType = powerType;
   }

   public void encode(C2SMessageToggleExoSuitPower message, FriendlyByteBuf buffer) {
      buffer.writeEnum(message.powerType);
   }

   public C2SMessageToggleExoSuitPower decode(FriendlyByteBuf buffer) {
      C2SMessageToggleExoSuitPower message = new C2SMessageToggleExoSuitPower();
      message.powerType = (C2SMessageToggleExoSuitPower.PowerType)buffer.readEnum(C2SMessageToggleExoSuitPower.PowerType.class);
      return message;
   }

   public void handle(C2SMessageToggleExoSuitPower message, MessageContext context) {
      context.execute(
         () -> {
            ServerPlayer player = context.getPlayer().map(p -> (ServerPlayer) p).orElse(null);
            if (player != null) {
               if ("jetpack".equals(message.powerType.getUpgradeType())) {
                  boolean currentState = ExoSuitPowerManager.isPowerEnabled(player, "utility");
                  boolean newState = !currentState;
                  ExoSuitPowerManager.setPowerEnabled(player, "utility", newState);
                  ExoSuitFlightHandler.setJetpackActive(player, newState);
                  String statusKey = newState ? "exosuit.message.enabled" : "exosuit.message.disabled";
                  ChatFormatting statusColor = newState ? ChatFormatting.GREEN : ChatFormatting.RED;
                  Component feedbackMessage = Component.translatable("exosuit.message.prefix")
                     .withStyle(ChatFormatting.GOLD)
                     .append(Component.translatable(statusKey, new Object[]{Component.translatable("exosuit.upgrade.jetpack")}).withStyle(statusColor));
                  player.sendSystemMessage(feedbackMessage, true);
                  if (newState) {
                     player.level()
                        .playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3F, 1.2F);
                  } else {
                     player.level()
                        .playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3F, 0.8F);
                  }
               } else if (!ExoSuitPowerManager.canUpgradeFunction(player, message.powerType.getUpgradeType())) {
                  String moduleTranslationKey = getSpecificModuleTranslationKey(player, message.powerType.getUpgradeType());
                  Component feedbackMessage = Component.translatable("exosuit.message.prefix")
                     .withStyle(ChatFormatting.GOLD)
                     .append(
                        Component.translatable("exosuit.message.not_available", new Object[]{Component.translatable(moduleTranslationKey)})
                           .withStyle(ChatFormatting.RED)
                     );
                  player.sendSystemMessage(feedbackMessage, true);
                  player.level()
                     .playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.5F, 0.8F);
               } else {
                  boolean newState = ExoSuitPowerManager.togglePower(player, message.powerType.getUpgradeType());
                  String moduleTranslationKey = getSpecificModuleTranslationKey(player, message.powerType.getUpgradeType());
                  String statusKey = newState ? "exosuit.message.enabled" : "exosuit.message.disabled";
                  ChatFormatting statusColor = newState ? ChatFormatting.GREEN : ChatFormatting.RED;
                  Component feedbackMessage = Component.translatable("exosuit.message.prefix")
                     .withStyle(ChatFormatting.GOLD)
                     .append(Component.translatable(statusKey, new Object[]{Component.translatable(moduleTranslationKey)}).withStyle(statusColor));
                  player.sendSystemMessage(feedbackMessage, true);
                  if (newState) {
                     player.level()
                        .playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3F, 1.2F);
                  } else {
                     player.level()
                        .playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3F, 0.8F);
                  }
               }
            }
         }
      );
      context.setHandled(true);
   }

   private static String getSpecificModuleTranslationKey(ServerPlayer player, String upgradeType) {
      if ("hud".equals(upgradeType)) {
         ItemStack hudModule = findHudModule(player);
         if (!hudModule.isEmpty()) {
            if (hudModule.getItem() instanceof NightVisionModuleItem) {
               return "exosuit.upgrade.night_vision";
            }

            if (hudModule.getItem() instanceof TargetTrackerModuleItem) {
               return "exosuit.upgrade.target_tracker";
            }

            if (hudModule.getItem() instanceof GasMaskModuleItem) {
               return "exosuit.upgrade.gas_mask";
            }

            if (hudModule.getItem() instanceof RebreatherModuleItem) {
               return "exosuit.upgrade.rebreather";
            }
         }

         return "exosuit.upgrade.hud";
      } else {
         return "exosuit.upgrade." + upgradeType;
      }
   }

   private static ItemStack findHudModule(ServerPlayer player) {
      for (ItemStack armorStack : player.getArmorSlots()) {
         if (armorStack.getItem() instanceof ExoSuitItem exosuit && exosuit.getType() == Type.HELMET) {
            for (int slot = 0; slot < 4; slot++) {
               ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
               if (!upgradeItem.isEmpty()) {
                  ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
                  if (upgrade != null && upgrade.getType().equals("hud")) {
                     return upgradeItem;
                  }
               }
            }
            break;
         }
      }

      return ItemStack.EMPTY;
   }

   public static enum PowerType {
      HELMET_HUD("hud", "HUD Module"),
      BOOTS_MOBILITY("mobility", "Mobility Enhancement"),
      JETPACK("jetpack", "Jetpack");

      private final String upgradeType;
      private final String displayName;

      private PowerType(String upgradeType, String displayName) {
         this.upgradeType = upgradeType;
         this.displayName = displayName;
      }

      public String getUpgradeType() {
         return this.upgradeType;
      }

      public String getDisplayName() {
         return this.displayName;
      }
   }
}
