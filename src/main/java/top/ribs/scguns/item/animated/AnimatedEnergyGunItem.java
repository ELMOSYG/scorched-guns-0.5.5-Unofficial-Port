package top.ribs.scguns.item.animated;





import net.minecraft.world.item.Item;
import top.ribs.scguns.util.Caps;
import top.ribs.scguns.util.NbtHelper;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import top.ribs.scguns.interfaces.IEnergyGun;

public class AnimatedEnergyGunItem extends AnimatedGunItem implements IEnergyGun {
   private final int capacity;

   public AnimatedEnergyGunItem(
      Properties properties,
      String path,
      SoundEvent reloadSoundMagOut,
      SoundEvent reloadSoundMagIn,
      SoundEvent reloadSoundEnd,
      SoundEvent boltPullSound,
      SoundEvent boltReleaseSound,
      int capacity
   ) {
      super(properties, path, reloadSoundMagOut, reloadSoundMagIn, reloadSoundEnd, boltPullSound, boltReleaseSound);
      this.capacity = capacity;
   }

   
   /** Capability factory; registered in ModCapabilities. */
   public IEnergyStorage createEnergyStorage(ItemStack stack) {
      return new AnimatedEnergyGunItem.ItemEnergyStorage(stack, this.capacity);
   }


   @Override
   public boolean isBarVisible(ItemStack stack) {
      return true;
   }

   @Override
   public int getBarWidth(ItemStack stack) {
      return Math.round(13.0F * (float)this.getEnergyStored(stack) / (float)this.getMaxEnergyStored(stack));
   }

   @Override
   public int getBarColor(ItemStack stack) {
      return 65280;
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
         Component.translatable("info.scguns.energy")
            .append(": ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(energyStored + " / " + maxEnergy + " FE").withStyle(ChatFormatting.GREEN))
      );
   }

   public static class ItemEnergyStorage implements IEnergyStorage {
      private final ItemStack stack;
      private final int capacity;
      private int energy;

      public ItemEnergyStorage(ItemStack stack, int capacity) {
         super();
         this.stack = stack;
         this.capacity = capacity;
         this.energy = this.loadEnergyFromNBT();
      }

      public int receiveEnergy(int maxReceive, boolean simulate) {
         int energyReceived = Math.min(this.capacity - this.energy, maxReceive);
         if (!simulate) {
            this.energy += energyReceived;
            this.updateEnergyTag();
         }

         return energyReceived;
      }

      public int extractEnergy(int maxExtract, boolean simulate) {
         int energyExtracted = Math.min(this.energy, maxExtract);
         if (!simulate) {
            this.energy -= energyExtracted;
            this.updateEnergyTag();
         }

         return energyExtracted;
      }

      public int getEnergyStored() {
         return this.energy;
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

      private void updateEnergyTag() {
         CompoundTag tag = NbtHelper.getOrCreateTag(this.stack);
         tag.putInt("Energy", this.energy);
      }

      private int loadEnergyFromNBT() {
         CompoundTag tag = NbtHelper.getTag(this.stack);
         return tag != null && tag.contains("Energy", 3) ? tag.getInt("Energy") : 0;
      }
   }
}
