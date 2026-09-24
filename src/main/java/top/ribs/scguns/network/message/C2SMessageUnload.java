package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import top.ribs.scguns.common.network.ServerPlayHandler;

public class C2SMessageUnload {
   public C2SMessageUnload() {
      super();
   }

   public void encode(C2SMessageUnload message, FriendlyByteBuf buffer) {
   }

   public C2SMessageUnload decode(FriendlyByteBuf buffer) {
      return new C2SMessageUnload();
   }

   public void handle(C2SMessageUnload message, MessageContext context) {
      context.execute(() -> {
         ServerPlayer player = context.getPlayer().map(p -> (ServerPlayer) p).orElse(null);
         if (player != null && !player.isSpectator()) {
            ServerPlayHandler.handleUnload(player);
         }
      });
      context.setHandled(true);
   }
}
