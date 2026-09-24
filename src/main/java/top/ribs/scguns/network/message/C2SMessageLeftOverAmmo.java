package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import top.ribs.scguns.common.network.ServerPlayHandler;

public class C2SMessageLeftOverAmmo {
   public C2SMessageLeftOverAmmo() {
      super();
   }

   public void encode(C2SMessageLeftOverAmmo message, FriendlyByteBuf buffer) {
   }

   public C2SMessageLeftOverAmmo decode(FriendlyByteBuf buffer) {
      return new C2SMessageLeftOverAmmo();
   }

   public void handle(C2SMessageLeftOverAmmo message, MessageContext context) {
      context.execute(() -> {
         ServerPlayer player = context.getPlayer().map(p -> (ServerPlayer) p).orElse(null);
         if (player != null && !player.isSpectator()) {
            ServerPlayHandler.handleExtraAmmo(player);
         }
      });
      context.setHandled(true);
   }
}
