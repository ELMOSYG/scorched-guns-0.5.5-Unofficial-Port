package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import top.ribs.scguns.common.network.ServerPlayHandler;

public class C2SMessagePreFireSound {
   public C2SMessagePreFireSound() {
      super();
   }

   public C2SMessagePreFireSound(Player player) {
      super();
   }

   public void encode(C2SMessagePreFireSound message, FriendlyByteBuf buffer) {
   }

   public C2SMessagePreFireSound decode(FriendlyByteBuf buffer) {
      return new C2SMessagePreFireSound();
   }

   public void handle(C2SMessagePreFireSound message, MessageContext context) {
      context.execute(() -> {
         ServerPlayer player = context.getPlayer().map(p -> (ServerPlayer) p).orElse(null);
         if (player != null) {
            ServerPlayHandler.handlePreFireSound(player);
         }
      });
      context.setHandled(true);
   }
}
