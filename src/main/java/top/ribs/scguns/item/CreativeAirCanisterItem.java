package top.ribs.scguns.item;



import net.minecraft.world.item.Item;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class CreativeAirCanisterItem extends AirCanisterItem {
   private static final int INFINITE_CAPACITY = 999999;

   public CreativeAirCanisterItem(Properties properties) {
      super(properties, 999999);
   }

   @Override
   
   /** Capability factory; registered in ModCapabilities. */
   public IEnergyStorage createEnergyStorage(ItemStack stack) {
      return new CreativeAirCanisterItem.CreativeAirStorage();
   }


   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      return InteractionResultHolder.pass(player.getItemInHand(hand));
   }

   @Override
   public int getBarColor(ItemStack stack) {
      return 16711935;
   }

   @Override
   public void appendHoverText(ItemStack stack, Item.TooltipContext world, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(
         Component.translatable("info.scguns.air_stored")
            .append(": ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal("∞").withStyle(ChatFormatting.LIGHT_PURPLE))
      );
   }

   @Override
   public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
      return slotChanged;
   }

   public static class CreativeAirStorage implements IEnergyStorage {
      public CreativeAirStorage() {
         super();
      }

      public int receiveEnergy(int maxReceive, boolean simulate) {
         return 0;
      }

      public int extractEnergy(int maxExtract, boolean simulate) {
         return maxExtract;
      }

      public int getEnergyStored() {
         return 999999;
      }

      public int getMaxEnergyStored() {
         return 999999;
      }

      public boolean canExtract() {
         return true;
      }

      public boolean canReceive() {
         return false;
      }
   }
}
