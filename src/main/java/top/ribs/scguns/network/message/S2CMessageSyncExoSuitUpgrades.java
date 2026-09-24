package top.ribs.scguns.network.message;


import top.ribs.scguns.util.DistHelper;
import com.mrcrayfish.framework.api.network.MessageContext;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import top.ribs.scguns.client.network.ClientPlayHandler;

public class S2CMessageSyncExoSuitUpgrades {
   private UUID playerId;
   private EquipmentSlot armorSlot;
   private CompoundTag upgradeData;

   public S2CMessageSyncExoSuitUpgrades() {
      super();
   }

   public S2CMessageSyncExoSuitUpgrades(UUID playerId, EquipmentSlot armorSlot, CompoundTag upgradeData) {
      super();
      this.playerId = playerId;
      this.armorSlot = armorSlot;
      this.upgradeData = upgradeData;
   }

   public void encode(S2CMessageSyncExoSuitUpgrades message, FriendlyByteBuf buffer) {
      buffer.writeUUID(message.playerId);
      buffer.writeEnum(message.armorSlot);
      buffer.writeNbt(message.upgradeData);
   }

   public S2CMessageSyncExoSuitUpgrades decode(FriendlyByteBuf buffer) {
      UUID playerId = buffer.readUUID();
      EquipmentSlot armorSlot = (EquipmentSlot)buffer.readEnum(EquipmentSlot.class);
      CompoundTag upgradeData = buffer.readNbt();
      return new S2CMessageSyncExoSuitUpgrades(playerId, armorSlot, upgradeData);
   }

   public void handle(S2CMessageSyncExoSuitUpgrades message, MessageContext context) {
      context.execute(() -> DistHelper.runWhenOn(Dist.CLIENT, () -> ClientPlayHandler.handleSyncExoSuitUpgrades(message)));
      context.setHandled(true);
   }

   public UUID getPlayerId() {
      return this.playerId;
   }

   public EquipmentSlot getArmorSlot() {
      return this.armorSlot;
   }

   public CompoundTag getUpgradeData() {
      return this.upgradeData;
   }
}
