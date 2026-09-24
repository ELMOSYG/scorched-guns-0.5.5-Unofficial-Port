package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.common.exosuit.ExoSuitData;
import top.ribs.scguns.common.exosuit.ExoSuitPouchHandler;
import top.ribs.scguns.common.exosuit.ExoSuitPowerManager;
import top.ribs.scguns.common.exosuit.ExoSuitUpgrade;
import top.ribs.scguns.common.exosuit.ExoSuitUpgradeManager;
import top.ribs.scguns.item.animated.ExoSuitItem;

public class C2SMessageUtilityAction {
   public C2SMessageUtilityAction() {
      super();
   }

   public void encode(C2SMessageUtilityAction message, FriendlyByteBuf buffer) {
   }

   public C2SMessageUtilityAction decode(FriendlyByteBuf buffer) {
      return new C2SMessageUtilityAction();
   }

   public void handle(C2SMessageUtilityAction message, MessageContext context) {
      context.execute(
         () -> {
            ServerPlayer player = context.getPlayer().map(p -> (ServerPlayer) p).orElse(null);
            if (player != null) {
               ItemStack chestplate = player.getInventory().getArmor(2);
               if (chestplate.getItem() instanceof ExoSuitItem) {
                  ItemStack utilityUpgrade = ExoSuitData.getUpgradeInSlot(chestplate, 3);
                  if (!utilityUpgrade.isEmpty()) {
                     ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(utilityUpgrade);
                     if (upgrade != null) {
                        if (upgrade.getEffects().hasFlight()) {
                           this.handleJetpackToggle(player);
                        } else if (upgrade.getType().equals("pouches")) {
                           ExoSuitPouchHandler.openPouchInventory(player);
                        } else if (upgrade.getType().equals("utility") && upgrade.getDisplay().getStorageSize() > 0) {
                           ExoSuitPouchHandler.openPouchInventory(player);
                        } else {
                           Component feedbackMessage = Component.translatable("exosuit.message.prefix")
                              .withStyle(ChatFormatting.GOLD)
                              .append(Component.translatable("exosuit.message.no_action").withStyle(ChatFormatting.GRAY));
                           player.sendSystemMessage(feedbackMessage, true);
                        }
                     }
                  }
               }
            }
         }
      );
      context.setHandled(true);
   }

   private void handleJetpackToggle(ServerPlayer player) {
      if (!ExoSuitPowerManager.canUpgradeFunction(player, "utility")) {
         Component feedbackMessage = Component.translatable("exosuit.message.prefix")
            .withStyle(ChatFormatting.GOLD)
            .append(
               Component.translatable("exosuit.message.not_available", new Object[]{Component.translatable("exosuit.upgrade.jetpack")}).withStyle(ChatFormatting.RED)
            );
         player.sendSystemMessage(feedbackMessage, true);
         player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.5F, 0.8F);
      } else {
         boolean newState = ExoSuitPowerManager.togglePower(player, "utility");
         String statusKey = newState ? "exosuit.message.enabled" : "exosuit.message.disabled";
         ChatFormatting statusColor = newState ? ChatFormatting.GREEN : ChatFormatting.RED;
         Component feedbackMessage = Component.translatable("exosuit.message.prefix")
            .withStyle(ChatFormatting.GOLD)
            .append(Component.translatable(statusKey, new Object[]{Component.translatable("exosuit.upgrade.jetpack")}).withStyle(statusColor));
         player.sendSystemMessage(feedbackMessage, true);
         if (newState) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3F, 1.2F);
         } else {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3F, 0.8F);
         }
      }
   }
}
