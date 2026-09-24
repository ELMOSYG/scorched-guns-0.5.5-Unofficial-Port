package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import top.ribs.scguns.common.network.ServerPlayHandler;

public class C2SMessageStopBeam {
   public C2SMessageStopBeam() {
      super();
   }

   public void encode(C2SMessageStopBeam message, FriendlyByteBuf buffer) {
   }

   public C2SMessageStopBeam decode(FriendlyByteBuf buffer) {
      return new C2SMessageStopBeam();
   }

   public void handle(C2SMessageStopBeam message, MessageContext context) {
      context.execute(() -> {
         ServerPlayer player = context.getPlayer().map(p -> (ServerPlayer) p).orElse(null);
         if (player != null) {
            ServerPlayHandler.handleStopBeam(player);
         }
      });
      context.setHandled(true);
   }
}
