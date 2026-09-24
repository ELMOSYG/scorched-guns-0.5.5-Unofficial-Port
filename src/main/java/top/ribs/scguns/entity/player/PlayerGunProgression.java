package top.ribs.scguns.entity.player;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.init.ModTags;

public class PlayerGunProgression {
   private static final String NBT_KEY = "SCGunsProgression";
   private static final String TIER_KEY = "CurrentTier";
   private static final String RAID_LEVEL_KEY = "RaidLevel";
   private GunTier currentTier = GunTiers.NONE;
   private int currentRaidLevel = 0;

   public PlayerGunProgression() {
      super();
   }

   public GunTier getCurrentTier() {
      return this.currentTier;
   }

   public int getCurrentRaidLevel() {
      return this.currentRaidLevel;
   }

   public List<GunTier> getAvailableMobTiers() {
      return this.currentTier.getAvailableMobTiers();
   }

   public boolean updateTier(GunTier newTier) {
      if (newTier == null) {
         return false;
      } else if (newTier.getLevel() > this.currentTier.getLevel()) {
         this.currentTier = newTier;
         this.currentRaidLevel = newTier.getRaidLevel();
         return true;
      } else {
         return false;
      }
   }

   public void setTier(GunTier tier) {
      if (tier == null) {
         this.currentTier = GunTiers.NONE;
         this.currentRaidLevel = 0;
      } else {
         this.currentTier = tier;
         this.currentRaidLevel = tier.getRaidLevel();
      }
   }

   public void setRaidLevel(int level) {
      this.currentRaidLevel = Math.max(0, level);
   }

   public boolean checkAndUpdateFromItem(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         for (GunTier tier : GunTierRegistry.getAllTiers()) {
            if (tier.getTagName() != null && ModTags.Items.isInTierTag(stack, tier)) {
               return this.updateTier(tier);
            }
         }

         return false;
      }
   }

   public CompoundTag saveNBT() {
      CompoundTag tag = new CompoundTag();
      tag.putString("TierId", this.currentTier.getId());
      tag.putInt("CurrentTier", this.currentTier.getLevel());
      tag.putInt("RaidLevel", this.currentRaidLevel);
      return tag;
   }

   public void loadNBT(CompoundTag tag) {
      if (tag.contains("TierId")) {
         String tierId = tag.getString("TierId");
         GunTier tier = GunTierRegistry.getTier(tierId);
         if (tier != null) {
            this.currentTier = tier;
         } else {
            this.currentTier = this.migrateLegacyTier(tag);
         }
      } else {
         this.currentTier = this.migrateLegacyTier(tag);
      }

      if (tag.contains("RaidLevel")) {
         this.currentRaidLevel = tag.getInt("RaidLevel");
      } else {
         this.currentRaidLevel = this.currentTier.getRaidLevel();
      }
   }

   @Nullable
   private GunTier migrateLegacyTier(CompoundTag tag) {
      if (tag.contains("TierName")) {
         String tierName = tag.getString("TierName").toLowerCase();
         GunTier tier = GunTierRegistry.getTier(tierName);
         if (tier != null) {
            return tier;
         }
      }

      if (tag.contains("CurrentTier")) {
         int level = tag.getInt("CurrentTier");
         GunTier tier = GunTierRegistry.getTierByLevel(level);
         if (tier != null) {
            return tier;
         }
      }

      return GunTiers.NONE;
   }

   public static PlayerGunProgression get(Player player) {
      CompoundTag persistentData = player.getPersistentData();
      PlayerGunProgression progression = new PlayerGunProgression();
      if (persistentData.contains("SCGunsProgression")) {
         progression.loadNBT(persistentData.getCompound("SCGunsProgression"));
      }

      return progression;
   }

   public static void save(Player player, PlayerGunProgression progression) {
      CompoundTag persistentData = player.getPersistentData();
      persistentData.put("SCGunsProgression", progression.saveNBT());
   }

   public static boolean updateAndSave(Player player, ItemStack stack) {
      PlayerGunProgression progression = get(player);
      boolean updated = progression.checkAndUpdateFromItem(stack);
      if (updated) {
         save(player, progression);
      }

      return updated;
   }
}
