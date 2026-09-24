package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.client.network.ClientPlayHandler;

public class S2CMessageBeamPenetration {
   private UUID playerId;
   private List<S2CMessageBeamPenetration.GlassPenetrationData> penetrations;

   public S2CMessageBeamPenetration() {
      super();
      this.penetrations = new ArrayList<>();
   }

   public S2CMessageBeamPenetration(UUID playerId, List<BlockHitResult> glassPenetrations) {
      super();
      this.playerId = playerId;
      this.penetrations = glassPenetrations.stream()
         .map(hit -> new S2CMessageBeamPenetration.GlassPenetrationData(hit.getLocation(), hit.getDirection(), hit.getBlockPos()))
         .collect(Collectors.toList());
   }

   public void encode(S2CMessageBeamPenetration message, FriendlyByteBuf buffer) {
      buffer.writeUUID(message.playerId);
      buffer.writeInt(message.penetrations.size());

      for (S2CMessageBeamPenetration.GlassPenetrationData penetration : message.penetrations) {
         buffer.writeDouble(penetration.position.x);
         buffer.writeDouble(penetration.position.y);
         buffer.writeDouble(penetration.position.z);
         buffer.writeEnum(penetration.face);
         buffer.writeBlockPos(penetration.blockPos);
      }
   }

   public S2CMessageBeamPenetration decode(FriendlyByteBuf buffer) {
      UUID playerId = buffer.readUUID();
      int size = buffer.readInt();
      List<BlockHitResult> penetrations = new ArrayList<>();

      for (int i = 0; i < size; i++) {
         Vec3 position = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
         Direction face = (Direction)buffer.readEnum(Direction.class);
         BlockPos blockPos = buffer.readBlockPos();
         penetrations.add(new BlockHitResult(position, face, blockPos, false));
      }

      return new S2CMessageBeamPenetration(playerId, penetrations);
   }

   public void handle(S2CMessageBeamPenetration message, MessageContext context) {
      context.execute(() -> ClientPlayHandler.handleBeamPenetration(message));
      context.setHandled(true);
   }

   public UUID getPlayerId() {
      return this.playerId;
   }

   public List<S2CMessageBeamPenetration.GlassPenetrationData> getPenetrations() {
      return this.penetrations;
   }

   public static class GlassPenetrationData {
      private final Vec3 position;
      private final Direction face;
      private final BlockPos blockPos;

      public GlassPenetrationData(Vec3 position, Direction face, BlockPos blockPos) {
         super();
         this.position = position;
         this.face = face;
         this.blockPos = blockPos;
      }

      public Vec3 getPosition() {
         return this.position;
      }

      public Direction getFace() {
         return this.face;
      }

      public BlockPos getBlockPos() {
         return this.blockPos;
      }
   }
}
