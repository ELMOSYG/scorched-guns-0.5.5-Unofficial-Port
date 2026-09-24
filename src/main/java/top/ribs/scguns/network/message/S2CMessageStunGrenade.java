package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.network.FriendlyByteBuf;
import top.ribs.scguns.client.network.ClientPlayHandler;

public class S2CMessageStunGrenade {
   private double x;
   private double y;
   private double z;

   public S2CMessageStunGrenade() {
      super();
   }

   public S2CMessageStunGrenade(double x, double y, double z) {
      super();
      this.z = z;
      this.y = y;
      this.x = x;
   }

   public void encode(S2CMessageStunGrenade message, FriendlyByteBuf buffer) {
      buffer.writeDouble(message.x);
      buffer.writeDouble(message.y);
      buffer.writeDouble(message.z);
   }

   public S2CMessageStunGrenade decode(FriendlyByteBuf buffer) {
      double x = buffer.readDouble();
      double y = buffer.readDouble();
      double z = buffer.readDouble();
      return new S2CMessageStunGrenade(x, y, z);
   }

   public void handle(S2CMessageStunGrenade message, MessageContext context) {
      context.execute(() -> ClientPlayHandler.handleExplosionStunGrenade(message));
      context.setHandled(true);
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }
}
