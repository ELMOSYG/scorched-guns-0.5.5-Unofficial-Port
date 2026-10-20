package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import top.ribs.scguns.client.network.ClientPlayHandler;
import top.ribs.scguns.util.DistHelper;

/**
 * Carries the turret definitions to a client.
 *
 * <p>A turret's {@code tick()} returns early while its configuration is missing, so on a client that never received
 * these the barrel simply never turns. The definitions are loaded by a server-side reload listener, which is why a
 * single-player session appeared to fix it: there the client and the server share one JVM, so the static config map
 * that the server filled was visible to the renderer as well. Joining a server directly left the map empty.</p>
 *
 * <p>Each entry rides as its raw JSON text: the parser that turns it into a {@code Turret} already exists and is
 * shared, so there is no second serialisation format to keep in sync.</p>
 */
public class S2CMessageSyncTurrets {
   private Map<ResourceLocation, CompoundTag> turretData;

   public S2CMessageSyncTurrets() {
      super();
      this.turretData = new HashMap<>();
   }

   public S2CMessageSyncTurrets(Map<ResourceLocation, CompoundTag> turretData) {
      super();
      this.turretData = turretData;
   }

   public void encode(S2CMessageSyncTurrets message, FriendlyByteBuf buffer) {
      buffer.writeInt(message.turretData.size());

      for (Entry<ResourceLocation, CompoundTag> entry : message.turretData.entrySet()) {
         buffer.writeResourceLocation(entry.getKey());
         buffer.writeNbt(entry.getValue());
      }
   }

   public S2CMessageSyncTurrets decode(FriendlyByteBuf buffer) {
      int size = buffer.readInt();
      Map<ResourceLocation, CompoundTag> turretData = new HashMap<>();

      for (int i = 0; i < size; i++) {
         ResourceLocation turretId = buffer.readResourceLocation();
         CompoundTag tag = buffer.readNbt();
         turretData.put(turretId, tag);
      }

      return new S2CMessageSyncTurrets(turretData);
   }

   public void handle(S2CMessageSyncTurrets message, MessageContext context) {
      context.execute(() -> DistHelper.runWhenOn(Dist.CLIENT, () -> ClientPlayHandler.handleSyncTurrets(message)));
      context.setHandled(true);
   }

   public Map<ResourceLocation, CompoundTag> getTurretData() {
      return this.turretData;
   }
}
