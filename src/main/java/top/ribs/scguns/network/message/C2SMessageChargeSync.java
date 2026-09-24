package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class C2SMessageChargeSync {
   private float chargeProgress;

   public C2SMessageChargeSync() {
      super();
   }

   public C2SMessageChargeSync(float chargeProgress) {
      super();
      this.chargeProgress = chargeProgress;
   }

   public void encode(C2SMessageChargeSync message, FriendlyByteBuf buffer) {
      buffer.writeFloat(message.chargeProgress);
   }

   public C2SMessageChargeSync decode(FriendlyByteBuf buffer) {
      return new C2SMessageChargeSync(buffer.readFloat());
   }

   public void handle(C2SMessageChargeSync message, MessageContext context) {
      context.execute(() -> {
         ServerPlayer player = context.getPlayer().map(p -> (ServerPlayer) p).orElse(null);
         if (player != null) {
            player.getPersistentData().putFloat("ChargeProgress", message.chargeProgress);
         }
      });
      context.setHandled(true);
   }

   public float getChargeProgress() {
      return this.chargeProgress;
   }
}
