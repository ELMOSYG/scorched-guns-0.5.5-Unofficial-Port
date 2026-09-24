package top.ribs.scguns.util;


import top.ribs.scguns.util.Caps;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.item.AirCanisterItem;
import top.theillusivec4.curios.api.CuriosApi;

public class AirSourceHelper {
   public AirSourceHelper() {
   }

   public static List<ItemStack> findAirCanistersWithAir(Player player) {
      List<ItemStack> airCanisters = new ArrayList<>();

      for (ItemStack stack : player.getInventory().items) {
         if (isAirCanisterWithAir(stack)) {
            airCanisters.add(stack);
         }
      }

      CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
         IItemHandlerModifiable curios = handler.getEquippedCurios();

         for (int i = 0; i < curios.getSlots(); i++) {
            ItemStack stackx = curios.getStackInSlot(i);
            if (isAirCanisterWithAir(stackx)) {
               airCanisters.add(stackx);
            }
         }
      });
      return airCanisters;
   }

   public static AirSourceHelper.AirSource getBestAirSource(Player player) {
      if (ScorchedGuns.createLoaded) {
         List<ItemStack> backtanks = BacktankUtil.getAllWithAir(player);
         if (!backtanks.isEmpty()) {
            return new AirSourceHelper.AirSource(AirSourceHelper.AirSource.Type.CREATE_BACKTANK, backtanks.get(0));
         }
      }

      List<ItemStack> airCanisters = findAirCanistersWithAir(player);
      return !airCanisters.isEmpty()
         ? new AirSourceHelper.AirSource(AirSourceHelper.AirSource.Type.AIR_CANISTER, airCanisters.get(0))
         : AirSourceHelper.AirSource.NONE;
   }

   public static boolean consumeAir(Player player, float airCost) {
      AirSourceHelper.AirSource airSource = getBestAirSource(player);
      switch (airSource.getType()) {
         case CREATE_BACKTANK:
            if (ScorchedGuns.createLoaded) {
               if (BacktankUtil.hasAirRemaining(airSource.getStack()) && BacktankUtil.getAir(airSource.getStack()) >= airCost) {
                  BacktankUtil.consumeAir(player, airSource.getStack(), (int)airCost);
                  return true;
               }

               return false;
            }

            return false;
         case AIR_CANISTER:
            IEnergyStorage energyStorage = (IEnergyStorage)airSource.getStack().getCapability(Capabilities.EnergyStorage.ITEM);
            if ((float)energyStorage.getEnergyStored() >= airCost) {
               int airExtracted = energyStorage.extractEnergy((int)airCost, false);
               return (float)airExtracted >= airCost;
            }

            return false;
         case NONE:
         default:
            return false;
      }
   }

   public static AirSourceHelper.AirInfo getAirInfo(Player player) {
      AirSourceHelper.AirSource airSource = getBestAirSource(player);
      switch (airSource.getType()) {
         case CREATE_BACKTANK:
            if (ScorchedGuns.createLoaded) {
               ItemStack backtank = airSource.getStack();
               int maxAir = BacktankUtil.maxAir(backtank);
               float air = BacktankUtil.getAir(backtank);
               int barWidth = Math.round(13.0F * air / (float)maxAir);
               int barColor = BacktankUtil.getBarColor(backtank, 1);
               return new AirSourceHelper.AirInfo(barWidth, barColor, AirSourceHelper.AirSource.Type.CREATE_BACKTANK);
            }
            break;
         case AIR_CANISTER:
            ItemStack canister = airSource.getStack();
            if (canister.getItem() instanceof AirCanisterItem airCanisterItem) {
               int stored = airCanisterItem.getAirStored(canister);
               int max = airCanisterItem.getMaxAirStored(canister);
               int barWidth = max > 0 ? Math.round(13.0F * (float)stored / (float)max) : 0;
               float ratio = max > 0 ? (float)stored / (float)max : 0.0F;
               int barColor;
               if (ratio < 0.25F) {
                  barColor = 16729156;
               } else if (ratio < 0.5F) {
                  barColor = 16755200;
               } else {
                  barColor = 43775;
               }

               return new AirSourceHelper.AirInfo(barWidth, barColor, AirSourceHelper.AirSource.Type.AIR_CANISTER);
            }
      }

      return AirSourceHelper.AirInfo.NONE;
   }

   private static boolean isAirCanisterWithAir(ItemStack stack) {
      if (!(stack.getItem() instanceof AirCanisterItem)) {
         return false;
      } else {
         IEnergyStorage energyStorage = (IEnergyStorage)stack.getCapability(Capabilities.EnergyStorage.ITEM);
         return energyStorage.getEnergyStored() > 0;
      }
   }

   public static record AirInfo(int barWidth, int barColor, AirSourceHelper.AirSource.Type sourceType) {
      public static final AirSourceHelper.AirInfo NONE = new AirSourceHelper.AirInfo(0, 8421504, AirSourceHelper.AirSource.Type.NONE);

      public AirInfo(int barWidth, int barColor, AirSourceHelper.AirSource.Type sourceType) {
         this.barWidth = barWidth;
         this.barColor = barColor;
         this.sourceType = sourceType;
      }
   }

   public static class AirSource {
      public static final AirSourceHelper.AirSource NONE = new AirSourceHelper.AirSource(AirSourceHelper.AirSource.Type.NONE, ItemStack.EMPTY);
      private final AirSourceHelper.AirSource.Type type;
      private final ItemStack stack;

      public AirSource(AirSourceHelper.AirSource.Type type, ItemStack stack) {
         this.type = type;
         this.stack = stack;
      }

      public AirSourceHelper.AirSource.Type getType() {
         return this.type;
      }

      public ItemStack getStack() {
         return this.stack;
      }

      public boolean isAvailable() {
         return this.type != AirSourceHelper.AirSource.Type.NONE && !this.stack.isEmpty();
      }

      public static enum Type {
         NONE,
         CREATE_BACKTANK,
         AIR_CANISTER;

         private Type() {
         }
      }
   }
}
