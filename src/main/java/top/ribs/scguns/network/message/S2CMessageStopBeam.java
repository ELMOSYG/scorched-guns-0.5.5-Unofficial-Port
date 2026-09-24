package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import top.ribs.scguns.client.network.ClientPlayHandler;

public class S2CMessageStopBeam {
   private UUID playerId;

   public S2CMessageStopBeam() {
      super();
   }

   public S2CMessageStopBeam(UUID playerId) {
      super();
      this.playerId = playerId;
   }

   public void encode(S2CMessageStopBeam message, FriendlyByteBuf buffer) {
      buffer.writeUUID(message.playerId);
   }

   public S2CMessageStopBeam decode(FriendlyByteBuf buffer) {
      return new S2CMessageStopBeam(buffer.readUUID());
   }

   public void handle(S2CMessageStopBeam message, MessageContext context) {
      context.execute(() -> ClientPlayHandler.handleStopBeam(message));
      context.setHandled(true);
   }

   public UUID getPlayerId() {
      return this.playerId;
   }
}
