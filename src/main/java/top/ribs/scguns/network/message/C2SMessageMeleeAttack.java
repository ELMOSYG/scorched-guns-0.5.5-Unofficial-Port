package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.client.handler.MeleeAttackHandler;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.network.PacketHandler;

public class C2SMessageMeleeAttack {
   private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
   private static final int BANZAI_CHECK_INTERVAL_MS = 50;

   public C2SMessageMeleeAttack() {
      super();
   }

   public void encode(C2SMessageMeleeAttack message, FriendlyByteBuf buffer) {
   }

   public C2SMessageMeleeAttack decode(FriendlyByteBuf buffer) {
      return new C2SMessageMeleeAttack();
   }

   public void handle(C2SMessageMeleeAttack message, MessageContext context) {
      context.execute(() -> {
         ServerPlayer player = context.getPlayer().map(p -> (ServerPlayer) p).orElse(null);
         if (player != null && !player.isSpectator()) {
            if (MeleeAttackHandler.isBanzaiActive()) {
               MeleeAttackHandler.stopBanzai();
            } else if (player.isSprinting()) {
               ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
               if (!(heldItem.getItem() instanceof GunItem gunItem)) {
                  return;
               }

               if (gunItem.hasBayonet(heldItem)) {
                  MeleeAttackHandler.startBanzai(player);
                  scheduler.scheduleAtFixedRate(() -> {
                     if (MeleeAttackHandler.isBanzaiActive()) {
                        if (player.isRemoved() || !player.isAlive()) {
                           MeleeAttackHandler.stopBanzai();
                        } else if (!player.isSprinting()) {
                           MeleeAttackHandler.stopBanzai();
                        } else {
                           MeleeAttackHandler.handleBanzaiMode(player);
                        }
                     }
                  }, 0L, 50L, TimeUnit.MILLISECONDS);
               } else {
                  MeleeAttackHandler.performNormalMeleeAttack(player);
               }
            } else {
               this.handleNormalMeleeAttack(player);
            }
         }
      });
      context.setHandled(true);
   }

   private void handleNormalMeleeAttack(ServerPlayer player) {
      if (!player.getCooldowns().isOnCooldown(player.getMainHandItem().getItem())) {
         MeleeAttackHandler.performMeleeAttack(player);
         PacketHandler.getPlayChannel().sendToPlayer(() -> player, new S2CMessageMeleeAttack(player.getItemInHand(InteractionHand.MAIN_HAND)));
      }
   }
}
