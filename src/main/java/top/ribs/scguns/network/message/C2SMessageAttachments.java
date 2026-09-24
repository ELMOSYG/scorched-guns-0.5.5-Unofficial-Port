package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import top.ribs.scguns.common.network.ServerPlayHandler;

public class C2SMessageAttachments {
   public C2SMessageAttachments() {
      super();
   }

   public void encode(C2SMessageAttachments message, FriendlyByteBuf buffer) {
   }

   public C2SMessageAttachments decode(FriendlyByteBuf buffer) {
      return new C2SMessageAttachments();
   }

   public void handle(C2SMessageAttachments message, MessageContext context) {
      context.execute(() -> {
         ServerPlayer player = context.getPlayer().map(p -> (ServerPlayer) p).orElse(null);
         if (player != null) {
            ServerPlayHandler.handleAttachments(player);
         }
      });
      context.setHandled(true);
   }
}
