package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import top.ribs.scguns.common.exosuit.ExoSuitFlightHandler;

public class C2SMessageJetpackThrust {
   private boolean isThrusting;

   public C2SMessageJetpackThrust() {
      super();
   }

   public C2SMessageJetpackThrust(boolean isThrusting) {
      super();
      this.isThrusting = isThrusting;
   }

   public void encode(C2SMessageJetpackThrust message, FriendlyByteBuf buffer) {
      buffer.writeBoolean(message.isThrusting);
   }

   public C2SMessageJetpackThrust decode(FriendlyByteBuf buffer) {
      return new C2SMessageJetpackThrust(buffer.readBoolean());
   }

   public void handle(C2SMessageJetpackThrust message, MessageContext context) {
      context.execute(() -> {
         ServerPlayer player = context.getPlayer().map(p -> (ServerPlayer) p).orElse(null);
         if (player != null && !player.isSpectator()) {
            ExoSuitFlightHandler.setPlayerThrusting(player, message.isThrusting);
         }
      });
      context.setHandled(true);
   }
}
