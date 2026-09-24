package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.client.network.ClientPlayHandler;

public class S2CMessageEntityCasingEject {
   private int entityId;
   private ResourceLocation particleLocation;

   public S2CMessageEntityCasingEject() {
      super();
   }

   public S2CMessageEntityCasingEject(int entityId, ResourceLocation particleLocation) {
      super();
      this.entityId = entityId;
      this.particleLocation = particleLocation;
   }

   public void encode(S2CMessageEntityCasingEject message, FriendlyByteBuf buffer) {
      buffer.writeInt(message.entityId);
      buffer.writeResourceLocation(message.particleLocation);
   }

   public S2CMessageEntityCasingEject decode(FriendlyByteBuf buffer) {
      return new S2CMessageEntityCasingEject(buffer.readInt(), buffer.readResourceLocation());
   }

   public void handle(S2CMessageEntityCasingEject message, MessageContext context) {
      context.execute(() -> ClientPlayHandler.handleEntityCasingEject(message));
      context.setHandled(true);
   }

   public int getEntityId() {
      return this.entityId;
   }

   public ResourceLocation getParticleLocation() {
      return this.particleLocation;
   }
}
