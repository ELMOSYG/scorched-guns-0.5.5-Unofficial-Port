package top.ribs.scguns.network.message;


import top.ribs.scguns.util.DistHelper;
import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import top.ribs.scguns.network.ClientMessageHandler;

public class S2CMessageUpdateAmmo {
   private int ammoCount;

   public S2CMessageUpdateAmmo() {
      super();
   }

   public S2CMessageUpdateAmmo(int ammoCount) {
      super();
      this.ammoCount = ammoCount;
   }

   public void encode(S2CMessageUpdateAmmo message, FriendlyByteBuf buffer) {
      buffer.writeInt(message.ammoCount);
   }

   public S2CMessageUpdateAmmo decode(FriendlyByteBuf buffer) {
      S2CMessageUpdateAmmo message = new S2CMessageUpdateAmmo();
      message.ammoCount = buffer.readInt();
      return message;
   }

   public void handle(S2CMessageUpdateAmmo message, MessageContext context) {
      context.execute(() -> DistHelper.runWhenOn(Dist.CLIENT, () -> {
               if (ClientMessageHandler.handleUpdateAmmo(message.ammoCount)) {
                  context.setHandled(true);
               }
            }));
   }
}
