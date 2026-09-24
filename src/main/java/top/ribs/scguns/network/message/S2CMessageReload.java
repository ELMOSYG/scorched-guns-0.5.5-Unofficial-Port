package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.network.FriendlyByteBuf;
import top.ribs.scguns.client.network.ClientPlayHandler;

public class S2CMessageReload {
   private boolean reloading;

   public S2CMessageReload() {
      super();
   }

   public S2CMessageReload(boolean reloading) {
      super();
      this.reloading = reloading;
   }

   public void encode(S2CMessageReload message, FriendlyByteBuf buffer) {
      buffer.writeBoolean(message.reloading);
   }

   public S2CMessageReload decode(FriendlyByteBuf buffer) {
      return new S2CMessageReload(buffer.readBoolean());
   }

   public void handle(S2CMessageReload message, MessageContext context) {
      context.execute(() -> ClientPlayHandler.handleReloadState(message.reloading));
      context.setHandled(true);
   }

   public boolean isReloading() {
      return this.reloading;
   }
}
