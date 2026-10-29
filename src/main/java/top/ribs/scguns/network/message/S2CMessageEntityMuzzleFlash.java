package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.client.handler.GunRenderingHandler;

public class S2CMessageEntityMuzzleFlash {
   private int entityId;
   private float randomValue;
   private Vec3 flashPosition;
   private boolean useEnchantedTexture;

   public S2CMessageEntityMuzzleFlash() {
      super();
      this.flashPosition = Vec3.ZERO;
      this.useEnchantedTexture = true;
   }

   public S2CMessageEntityMuzzleFlash(int entityId, float randomValue, Vec3 flashPosition, boolean useEnchantedTexture) {
      super();
      this.entityId = entityId;
      this.randomValue = randomValue;
      this.flashPosition = flashPosition;
      this.useEnchantedTexture = useEnchantedTexture;
   }

   public void encode(S2CMessageEntityMuzzleFlash message, FriendlyByteBuf buffer) {
      buffer.writeInt(message.entityId);
      buffer.writeFloat(message.randomValue);
      buffer.writeDouble(message.flashPosition.x);
      buffer.writeDouble(message.flashPosition.y);
      buffer.writeDouble(message.flashPosition.z);
      buffer.writeBoolean(message.useEnchantedTexture);
   }

   public S2CMessageEntityMuzzleFlash decode(FriendlyByteBuf buffer) {
      int entityId = buffer.readInt();
      float randomValue = buffer.readFloat();
      Vec3 flashPosition = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
      boolean useEnchantedTexture = buffer.readBoolean();
      return new S2CMessageEntityMuzzleFlash(entityId, randomValue, flashPosition, useEnchantedTexture);
   }

   /** How long a gun's moving parts take to settle after a shot, in ticks. */
   private static final int RECOIL_TICKS = 10;

   public void handle(S2CMessageEntityMuzzleFlash message, MessageContext context) {
      context.execute(() -> {
         // Reuse the flash broadcast to tell clients which entity just fired: only the local player's own
         // cooldown can be read directly, so this is how another player's bolt gets to move at all.
         top.ribs.scguns.client.GunRecoilSync.record(message.entityId, RECOIL_TICKS);
         GunRenderingHandler.get().showMuzzleFlashForPlayer(message.entityId);
         GunRenderingHandler.entityIdToRandomValue.put(message.entityId, message.randomValue);
         GunRenderingHandler.entityIdToFlashPosition.put(message.entityId, message.flashPosition);
         GunRenderingHandler.entityIdToUseEnchantedTexture.put(message.entityId, message.useEnchantedTexture);
      });
      context.setHandled(true);
   }
}
