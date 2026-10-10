package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
   /**
    * The repeating task that drives the charge. It is replaced (never duplicated) by a new charge and
    * cancels itself once the charge is over: the original scheduled one task per charge start and never
    * cancelled it, so a player who charged N times had N tasks calling {@code handleBanzaiMode} - each of
    * them knocking the player back and scanning for targets every 50 ms (HANDOFF section 82.23).
    */
   private static final java.util.Map<java.util.UUID, ScheduledFuture<?>> banzaiTasks =
      new java.util.concurrent.ConcurrentHashMap<>();

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
            if (MeleeAttackHandler.isBanzaiCharging(player)) {
               MeleeAttackHandler.stopBanzai(player);
            } else if (!MeleeAttackHandler.isBanzaiEnabled()) {
               // The charge is switched off (HANDOFF section 82.32). A press with a bayonet fitted is then an
               // ordinary bayonet stab, not a dead key, and any charge still running is stopped above.
               this.handleNormalMeleeAttack(player);
            } else if (player.isSprinting() || !MeleeAttackHandler.isSprintRequiredToStart()) {
               ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
               if (!(heldItem.getItem() instanceof GunItem gunItem)) {
                  return;
               }

               if (gunItem.hasBayonet(heldItem)) {
                  MeleeAttackHandler.startBanzai(player);
                  startBanzaiTicker(player);
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

   /**
    * Drives one charge. Sprinting is required to <em>start</em> a charge (above), but it is deliberately not
    * re-checked here: the sprint flag drops on every block collision, in water and when the food bar empties,
    * so a bare check here cancelled charges on the first wall the player ran into - before
    * {@code handleBanzaiMode}, which owns that decision together with its knockback grace period, was ever
    * reached. A charge ends there, not here (HANDOFF section 82.23).
    */
   private static void startBanzaiTicker(ServerPlayer player) {
      // One task per player. A single shared task meant that whoever charged last cancelled everyone else's
      // ticker, so those charges were never driven to their end and their animation never stopped.
      ScheduledFuture<?> previous = banzaiTasks.remove(player.getUUID());
      if (previous != null) {
         previous.cancel(false);
      }

      final ScheduledFuture<?>[] self = new ScheduledFuture<?>[1];
      self[0] = scheduler.scheduleAtFixedRate(() -> {
         // Ask whether THIS player is still charging; the static flag is global and cannot answer that.
         if (!MeleeAttackHandler.isBanzaiCharging(player)) {
            banzaiTasks.remove(player.getUUID(), self[0]);
            self[0].cancel(false);
            return;
         }

         if (player.isRemoved() || !player.isAlive()) {
            MeleeAttackHandler.stopBanzai(player);
            banzaiTasks.remove(player.getUUID(), self[0]);
            self[0].cancel(false);
            return;
         }

         MeleeAttackHandler.handleBanzaiMode(player);
      }, 0L, BANZAI_CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
      banzaiTasks.put(player.getUUID(), self[0]);
   }

   private void handleNormalMeleeAttack(ServerPlayer player) {
      if (!player.getCooldowns().isOnCooldown(player.getMainHandItem().getItem())) {
         MeleeAttackHandler.performMeleeAttack(player);
         PacketHandler.getPlayChannel().sendToPlayer(() -> player, new S2CMessageMeleeAttack(player.getItemInHand(InteractionHand.MAIN_HAND)));
      }
   }
}
