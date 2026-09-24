package top.ribs.scguns.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class PlayerScampManager {
   private static final Map<UUID, PlayerScampManager.PlayerScampData> PLAYER_DATA = new HashMap<>();

   public PlayerScampManager() {
      super();
   }

   public static PlayerScampManager.PlayerScampData getOrCreatePlayerData(Player player) {
      return PLAYER_DATA.computeIfAbsent(player.getUUID(), k -> new PlayerScampManager.PlayerScampData());
   }

   public static void init() {
      PLAYER_DATA.clear();
   }

   public static void savePlayerData(Player player) {
      PlayerScampManager.PlayerScampData data = PLAYER_DATA.get(player.getUUID());
      if (data != null && data.isDirty) {
         CompoundTag persistentData = player.getPersistentData();
         CompoundTag scampData = new CompoundTag();
         data.saveToNBT(scampData);
         persistentData.put("ScampManagerData", scampData);
         data.isDirty = false;
      }
   }

   public static void loadPlayerData(Player player) {
      CompoundTag persistentData = player.getPersistentData();
      if (persistentData.contains("ScampManagerData")) {
         PlayerScampManager.PlayerScampData data = new PlayerScampManager.PlayerScampData();
         data.loadFromNBT(persistentData.getCompound("ScampManagerData"));
         PLAYER_DATA.put(player.getUUID(), data);
      }
   }

   public static class PlayerScampData {
      private UUID linkedScampId;
      private BlockPos containerPos;
      private boolean isDirty = false;

      public PlayerScampData() {
         super();
      }

      public UUID getLinkedScampId() {
         return this.linkedScampId;
      }

      public void setLinkedScampId(UUID scampId) {
         this.linkedScampId = scampId;
         this.isDirty = true;
      }

      public BlockPos getContainerPos() {
         return this.containerPos;
      }

      public void setContainerPos(BlockPos pos) {
         this.containerPos = pos;
         this.isDirty = true;
      }

      public void saveToNBT(CompoundTag tag) {
         if (this.linkedScampId != null) {
            tag.putUUID("LinkedScampId", this.linkedScampId);
         }

         if (this.containerPos != null) {
            tag.putInt("ContainerX", this.containerPos.getX());
            tag.putInt("ContainerY", this.containerPos.getY());
            tag.putInt("ContainerZ", this.containerPos.getZ());
         }
      }

      public void loadFromNBT(CompoundTag tag) {
         if (tag.hasUUID("LinkedScampId")) {
            this.linkedScampId = tag.getUUID("LinkedScampId");
         }

         if (tag.contains("ContainerX")) {
            this.containerPos = new BlockPos(tag.getInt("ContainerX"), tag.getInt("ContainerY"), tag.getInt("ContainerZ"));
         }
      }
   }
}
