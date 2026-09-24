package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.client.handler.BeamHandler;

public class S2CMessageBeamImpact {
   private Vec3 hitPosition;
   private UUID playerUUID;

   public S2CMessageBeamImpact() {
      super();
   }

   public S2CMessageBeamImpact(Vec3 hitPosition, UUID playerUUID) {
      super();
      this.hitPosition = hitPosition;
      this.playerUUID = playerUUID;
   }

   public void encode(S2CMessageBeamImpact message, FriendlyByteBuf buffer) {
      buffer.writeDouble(message.hitPosition.x);
      buffer.writeDouble(message.hitPosition.y);
      buffer.writeDouble(message.hitPosition.z);
      buffer.writeUUID(message.playerUUID);
   }

   public S2CMessageBeamImpact decode(FriendlyByteBuf buffer) {
      double x = buffer.readDouble();
      double y = buffer.readDouble();
      double z = buffer.readDouble();
      Vec3 hitPosition = new Vec3(x, y, z);
      UUID playerUUID = buffer.readUUID();
      return new S2CMessageBeamImpact(hitPosition, playerUUID);
   }

   public void handle(S2CMessageBeamImpact message, MessageContext context) {
      context.execute(() -> {
         ClientLevel world = Minecraft.getInstance().level;
         if (world != null) {
            Player player = world.getPlayerByUUID(message.playerUUID);
            if (player != null) {
               BeamHandler.spawnBeamImpactParticles(world, message.hitPosition, player);
            }
         }
      });
      context.setHandled(true);
   }
}
