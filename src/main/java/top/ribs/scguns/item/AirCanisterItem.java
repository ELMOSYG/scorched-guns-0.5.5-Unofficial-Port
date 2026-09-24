package top.ribs.scguns.item;




import top.ribs.scguns.util.Caps;
import top.ribs.scguns.util.NbtHelper;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class AirCanisterItem extends Item {
   private final int capacity;
   private static final int AIR_PER_USE = 50;
   private static final int HUNGER_COST = 1;
   private static final int USE_COOLDOWN = 10;

   public AirCanisterItem(Properties properties, int capacity) {
      super(properties);
      this.capacity = capacity;
   }

   
   /** Capability factory; registered in ModCapabilities. */
   public IEnergyStorage createEnergyStorage(ItemStack stack) {
      return new AirCanisterItem.AirStorage(stack, this.capacity);
   }


   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (player.getCooldowns().isOnCooldown(this)) {
         return InteractionResultHolder.pass(stack);
      } else {
         FoodData foodData = player.getFoodData();
         int currentAir = this.getAirStored(stack);
         int maxAir = this.getMaxAirStored(stack);
         if (currentAir >= maxAir) {
            return InteractionResultHolder.pass(stack);
         } else if (foodData.getFoodLevel() < 1 && !player.isCreative()) {
            return InteractionResultHolder.pass(stack);
         } else {
            IEnergyStorage airStorage = (IEnergyStorage)stack.getCapability(Capabilities.EnergyStorage.ITEM);
            int airAdded = airStorage.receiveEnergy(50, false);
            if (airAdded > 0) {
               if (!player.isCreative() && !level.isClientSide) {
                  foodData.addExhaustion(0.5F);
               }

               level.playSound(null, player.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.5F, 1.2F);
               player.getCooldowns().addCooldown(this, 10);
               this.updateDurabilityDisplay(stack);
               return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            } else {
               return InteractionResultHolder.pass(stack);
            }
         }
      }
   }

   public boolean isBarVisible(ItemStack stack) {
      return true;
   }

   public int getBarWidth(ItemStack stack) {
      int airStored = this.getAirStored(stack);
      int maxAir = this.getMaxAirStored(stack);
      return maxAir == 0 ? 0 : Math.round(13.0F * (float)airStored / (float)maxAir);
   }

   public int getBarColor(ItemStack stack) {
      int airStored = this.getAirStored(stack);
      int maxAir = this.getMaxAirStored(stack);
      if (maxAir == 0) {
         return 8421504;
      } else {
         float ratio = (float)airStored / (float)maxAir;
         if (ratio < 0.25F) {
            return 16729156;
         } else {
            return ratio < 0.5F ? 16755200 : 43775;
         }
      }
   }

   @Override
   public void appendHoverText(ItemStack stack, Item.TooltipContext world, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, world, tooltip, flag);
      int airStored = this.getAirStored(stack);
      int maxAir = this.getMaxAirStored(stack);
      tooltip.add(
         Component.translatable("info.scguns.air_stored")
            .append(": ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(airStored + " / " + maxAir).withStyle(ChatFormatting.AQUA))
      );
      if (Screen.hasShiftDown()) {
         tooltip.add(Component.translatable("info.scguns.air_canister.usage").withStyle(ChatFormatting.YELLOW));
      } else {
         tooltip.add(Component.translatable("tooltip.scguns.hold_shift").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}));
      }
   }

   public int getAirStored(ItemStack stack) {
      return Caps.energyStored(stack, 0);
   }

   public int getMaxAirStored(ItemStack stack) {
      return Caps.maxEnergyStored(stack, this.capacity);
   }

   private void updateDurabilityDisplay(ItemStack stack) {
   }

   public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
      return slotChanged;
   }

   @Override
   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 10;
   }

   public static class AirStorage implements IEnergyStorage {
      private final ItemStack stack;
      private final int capacity;
      private int air;

      public AirStorage(ItemStack stack, int capacity) {
         super();
         this.stack = stack;
         this.capacity = capacity;
         this.air = this.loadAirFromNBT();
      }

      public int receiveEnergy(int maxReceive, boolean simulate) {
         int airReceived = Math.min(this.capacity - this.air, maxReceive);
         if (!simulate && airReceived > 0) {
            this.air += airReceived;
            this.updateAirTag();
         }

         return airReceived;
      }

      public int extractEnergy(int maxExtract, boolean simulate) {
         int airExtracted = Math.min(this.air, maxExtract);
         if (!simulate && airExtracted > 0) {
            this.air -= airExtracted;
            this.updateAirTag();
         }

         return airExtracted;
      }

      public int getEnergyStored() {
         return this.air;
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

      private void updateAirTag() {
         CompoundTag tag = NbtHelper.getOrCreateTag(this.stack);
         tag.putInt("AirStored", this.air);
      }

      private int loadAirFromNBT() {
         CompoundTag tag = NbtHelper.getTag(this.stack);
         return tag != null && tag.contains("AirStored", 3) ? tag.getInt("AirStored") : 0;
      }
   }
}
