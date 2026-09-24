package top.ribs.scguns.item.exosuit;




import top.ribs.scguns.util.Caps;
import top.ribs.scguns.util.NbtHelper;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import top.ribs.scguns.Config;

public class ExoSuitCoreItem extends Item {
   private final ExoSuitCoreItem.CoreTier tier;

   public ExoSuitCoreItem(Properties properties, ExoSuitCoreItem.CoreTier tier) {
      super(properties);
      this.tier = tier;
   }

   
   /** Capability factory; registered in ModCapabilities. */
   public IEnergyStorage createEnergyStorage(ItemStack stack) {
      return new ExoSuitCoreItem.SimpleExoSuitEnergyStorage(stack, this.tier.getCapacity());
   }


   public boolean isBarVisible(ItemStack stack) {
      return true;
   }

   public int getBarWidth(ItemStack stack) {
      int energyStored = this.getEnergyStored(stack);
      int maxEnergy = this.getMaxEnergyStored(stack);
      return maxEnergy == 0 ? 0 : Math.round(13.0F * (float)energyStored / (float)maxEnergy);
   }

   public int getBarColor(ItemStack stack) {
      float ratio = (float)this.getEnergyStored(stack) / (float)this.getMaxEnergyStored(stack);
      if (ratio > 0.66F) {
         return 65535;
      } else {
         return ratio > 0.33F ? 16776960 : 16729156;
      }
   }

   public int getEnergyStored(ItemStack stack) {
      return Caps.energyStored(stack, 0);
   }

   public int getMaxEnergyStored(ItemStack stack) {
      return Caps.maxEnergyStored(stack, 0);
   }

   @Override
   public void appendHoverText(ItemStack stack, Item.TooltipContext worldIn, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, worldIn, tooltip, flag);
      int energyStored = this.getEnergyStored(stack);
      int maxEnergy = this.getMaxEnergyStored(stack);
      tooltip.add(
         Component.translatable("tooltip.scguns.energy")
            .append(": ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%,d", energyStored)).withStyle(ChatFormatting.BLUE))
            .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.format("%,d", maxEnergy) + " FE").withStyle(ChatFormatting.BLUE))
      );
   }

   public static enum CoreTier {
      BASIC("Basic"),
      ADVANCED("Advanced"),
      ELITE("Elite");

      private final String displayName;

      private CoreTier(String displayName) {
         this.displayName = displayName;
      }

      public String getDisplayName() {
         return this.displayName;
      }

      public int getCapacity() {
         return switch (this) {
            case BASIC -> Config.COMMON.exoSuitCores.basicCoreCapacity.get();
            case ADVANCED -> Config.COMMON.exoSuitCores.advancedCoreCapacity.get();
            case ELITE -> Config.COMMON.exoSuitCores.eliteCoreCapacity.get();
         };
      }
   }

   public static class SimpleExoSuitEnergyStorage implements IEnergyStorage {
      private final ItemStack stack;
      private final int capacity;

      public SimpleExoSuitEnergyStorage(ItemStack stack, int capacity) {
         super();
         this.stack = stack;
         this.capacity = capacity;
      }

      public int receiveEnergy(int maxReceive, boolean simulate) {
         int currentEnergy = this.getEnergyFromNBT();
         int energyReceived = Math.min(this.capacity - currentEnergy, maxReceive);
         if (!simulate && energyReceived > 0) {
            this.setEnergyToNBT(currentEnergy + energyReceived);
         }

         return energyReceived;
      }

      public int extractEnergy(int maxExtract, boolean simulate) {
         int currentEnergy = this.getEnergyFromNBT();
         int energyExtracted = Math.min(currentEnergy, maxExtract);
         if (!simulate && energyExtracted > 0) {
            this.setEnergyToNBT(currentEnergy - energyExtracted);
         }

         return energyExtracted;
      }

      public int getEnergyStored() {
         return this.getEnergyFromNBT();
      }

      public int getMaxEnergyStored() {
         return this.capacity;
      }

      public boolean canExtract() {
         return true;
      }

      public boolean canReceive() {
         return true;
      }

      private int getEnergyFromNBT() {
         CompoundTag tag = NbtHelper.getTag(this.stack);
         return tag != null && tag.contains("Energy", 3) ? tag.getInt("Energy") : 0;
      }

      private void setEnergyToNBT(int energy) {
         CompoundTag tag = NbtHelper.getOrCreateTag(this.stack);
         tag.putInt("Energy", Math.max(0, Math.min(energy, this.capacity)));
      }
   }
}
