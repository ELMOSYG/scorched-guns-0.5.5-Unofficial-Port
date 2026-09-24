package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.network.FriendlyByteBuf;
import top.ribs.scguns.client.network.ClientPlayHandler;

public class S2CMessageRemoveProjectile {
   private int entityId;

   public S2CMessageRemoveProjectile() {
      super();
   }

   public S2CMessageRemoveProjectile(int entityId) {
      super();
      this.entityId = entityId;
   }

   public void encode(S2CMessageRemoveProjectile message, FriendlyByteBuf buffer) {
      buffer.writeInt(message.entityId);
   }

   public S2CMessageRemoveProjectile decode(FriendlyByteBuf buffer) {
      return new S2CMessageRemoveProjectile(buffer.readInt());
   }

   public void handle(S2CMessageRemoveProjectile message, MessageContext context) {
      context.execute(() -> ClientPlayHandler.handleRemoveProjectile(message));
      context.setHandled(true);
   }

   public int getEntityId() {
      return this.entityId;
   }
}
