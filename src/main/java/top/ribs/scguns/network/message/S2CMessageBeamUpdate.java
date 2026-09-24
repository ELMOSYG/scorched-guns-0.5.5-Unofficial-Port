package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.client.network.ClientPlayHandler;

public class S2CMessageBeamUpdate {
   private UUID playerId;
   private Vec3 startPos;
   private Vec3 endPos;

   public S2CMessageBeamUpdate() {
      super();
   }

   public S2CMessageBeamUpdate(UUID playerId, Vec3 startPos, Vec3 endPos) {
      super();
      this.playerId = playerId;
      this.startPos = startPos;
      this.endPos = endPos;
   }

   public void encode(S2CMessageBeamUpdate message, FriendlyByteBuf buffer) {
      buffer.writeUUID(message.playerId);
      buffer.writeDouble(message.startPos.x);
      buffer.writeDouble(message.startPos.y);
      buffer.writeDouble(message.startPos.z);
      buffer.writeDouble(message.endPos.x);
      buffer.writeDouble(message.endPos.y);
      buffer.writeDouble(message.endPos.z);
   }

   public S2CMessageBeamUpdate decode(FriendlyByteBuf buffer) {
      UUID playerId = buffer.readUUID();
      Vec3 startPos = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
      Vec3 endPos = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
      return new S2CMessageBeamUpdate(playerId, startPos, endPos);
   }

   public void handle(S2CMessageBeamUpdate message, MessageContext context) {
      context.execute(() -> ClientPlayHandler.handleBeamUpdate(message));
      context.setHandled(true);
   }

   public UUID getPlayerId() {
      return this.playerId;
   }

   public Vec3 getStartPos() {
      return this.startPos;
   }

   public Vec3 getEndPos() {
      return this.endPos;
   }
}
