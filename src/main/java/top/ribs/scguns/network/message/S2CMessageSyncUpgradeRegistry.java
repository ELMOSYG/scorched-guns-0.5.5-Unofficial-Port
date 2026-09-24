package top.ribs.scguns.network.message;


import top.ribs.scguns.util.DistHelper;
import com.mrcrayfish.framework.api.network.MessageContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import top.ribs.scguns.client.network.ClientPlayHandler;

public class S2CMessageSyncUpgradeRegistry {
   private Map<ResourceLocation, CompoundTag> upgradeData;

   public S2CMessageSyncUpgradeRegistry() {
      super();
      this.upgradeData = new HashMap<>();
   }

   public S2CMessageSyncUpgradeRegistry(Map<ResourceLocation, CompoundTag> upgradeData) {
      super();
      this.upgradeData = upgradeData;
   }

   public void encode(S2CMessageSyncUpgradeRegistry message, FriendlyByteBuf buffer) {
      buffer.writeInt(message.upgradeData.size());

      for (Entry<ResourceLocation, CompoundTag> entry : message.upgradeData.entrySet()) {
         buffer.writeResourceLocation(entry.getKey());
         buffer.writeNbt(entry.getValue());
      }
   }

   public S2CMessageSyncUpgradeRegistry decode(FriendlyByteBuf buffer) {
      int size = buffer.readInt();
      Map<ResourceLocation, CompoundTag> upgradeData = new HashMap<>();

      for (int i = 0; i < size; i++) {
         ResourceLocation itemId = buffer.readResourceLocation();
         CompoundTag tag = buffer.readNbt();
         upgradeData.put(itemId, tag);
      }

      return new S2CMessageSyncUpgradeRegistry(upgradeData);
   }

   public void handle(S2CMessageSyncUpgradeRegistry message, MessageContext context) {
      context.execute(() -> DistHelper.runWhenOn(Dist.CLIENT, () -> ClientPlayHandler.handleSyncUpgradeRegistry(message)));
      context.setHandled(true);
   }

   public Map<ResourceLocation, CompoundTag> getUpgradeData() {
      return this.upgradeData;
   }
}
