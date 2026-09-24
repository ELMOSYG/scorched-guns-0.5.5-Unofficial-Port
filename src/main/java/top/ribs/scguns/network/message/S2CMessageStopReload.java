package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.network.FriendlyByteBuf;
import top.ribs.scguns.client.network.ClientPlayHandler;

public class S2CMessageStopReload {
   public S2CMessageStopReload() {
      super();
   }

   public void encode(S2CMessageStopReload message, FriendlyByteBuf buffer) {
   }

   public S2CMessageStopReload decode(FriendlyByteBuf buffer) {
      return new S2CMessageStopReload();
   }

   public void handle(S2CMessageStopReload message, MessageContext context) {
      context.execute(() -> ClientPlayHandler.handleStopReload(message));
      context.setHandled(true);
   }
}
