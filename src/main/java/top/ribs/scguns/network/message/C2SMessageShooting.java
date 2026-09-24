package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import top.ribs.scguns.init.ModSyncedDataKeys;

public class C2SMessageShooting {
   private boolean shooting;

   public C2SMessageShooting() {
      super();
   }

   public C2SMessageShooting(boolean shooting) {
      super();
      this.shooting = shooting;
   }

   public void encode(C2SMessageShooting message, FriendlyByteBuf buffer) {
      buffer.writeBoolean(message.shooting);
   }

   public C2SMessageShooting decode(FriendlyByteBuf buffer) {
      return new C2SMessageShooting(buffer.readBoolean());
   }

   public void handle(C2SMessageShooting message, MessageContext context) {
      context.execute(() -> {
         ServerPlayer player = context.getPlayer().map(p -> (ServerPlayer) p).orElse(null);
         if (player != null) {
            ModSyncedDataKeys.SHOOTING.setValue(player, message.shooting);
         }
      });
      context.setHandled(true);
   }
}
