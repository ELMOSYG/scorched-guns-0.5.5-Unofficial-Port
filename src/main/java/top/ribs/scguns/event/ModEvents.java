package top.ribs.scguns.event;


import net.minecraft.world.item.trading.ItemCost;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.List;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.ribs.scguns.init.ModItems;
import top.ribs.scguns.init.ModVillagers;

@EventBusSubscriber(
   modid = "scguns"
)
public class ModEvents {
   public ModEvents() {
      super();
   }

   @SubscribeEvent
   public static void addCustomTrades(VillagerTradesEvent event) {
      if (event.getType() == ModVillagers.GUNSMITH.get()) {
         Int2ObjectMap<List<ItemListing>> trades = event.getTrades();
         ((List<ItemListing>)trades.get(1))
            .add((pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.EMERALD, 8), new ItemStack((ItemLike)ModItems.GUN_GRIP.get(), 1), 10, 2, 0.02F));
         ((List<ItemListing>)trades.get(1))
            .add((pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.EMERALD, 9), new ItemStack((ItemLike)ModItems.GUN_BARREL.get(), 1), 10, 2, 0.02F));
         ((List<ItemListing>)trades.get(1))
            .add(
               (pTrader, pRandom) -> new MerchantOffer(new ItemCost((ItemLike)ModItems.SMALL_COPPER_CASING.get(), 6), new ItemStack(Items.EMERALD, 1), 10, 2, 0.02F)
            );
         ((List<ItemListing>)trades.get(1)).add((pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.IRON_INGOT, 4), new ItemStack(Items.EMERALD, 1), 12, 5, 0.02F));
         ((List<ItemListing>)trades.get(1))
            .add((pTrader, pRandom) -> new MerchantOffer(new ItemCost((ItemLike)ModItems.BUCKSHOT.get(), 4), new ItemStack(Items.EMERALD, 1), 16, 5, 0.02F));
         ((List<ItemListing>)trades.get(1))
            .add((pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.EMERALD, 15), new ItemStack((ItemLike)ModItems.LONGARM.get(), 1), 3, 10, 0.05F));
         ((List<ItemListing>)trades.get(1))
            .add(
               (pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.EMERALD, 2), new ItemStack((ItemLike)ModItems.SMALL_IRON_CASING.get(), 8), 10, 10, 0.05F)
            );
         ((List<ItemListing>)trades.get(1))
            .add(
               (pTrader, pRandom) -> new MerchantOffer(new ItemCost((ItemLike)ModItems.ANTHRALITE_INGOT.get(), 3), new ItemStack(Items.EMERALD, 1), 12, 10, 0.05F)
            );
         ((List<ItemListing>)trades.get(2))
            .add(
               (pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.EMERALD, 25), new ItemStack((ItemLike)ModItems.COPPER_FLARE.get(), 1), 2, 12, 0.05F)
            );
         ((List<ItemListing>)trades.get(3))
            .add(
               (pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.EMERALD, 9), new ItemStack((ItemLike)ModItems.LASER_SIGHT.get(), 1), 6, 15, 0.05F)
            );
         ((List<ItemListing>)trades.get(3))
            .add(
               (pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.EMERALD, 8), new ItemStack((ItemLike)ModItems.RIFLE_AMMO_BOX.get(), 1), 6, 15, 0.05F)
            );
         ((List<ItemListing>)trades.get(3))
            .add((pTrader, pRandom) -> new MerchantOffer(new ItemCost((ItemLike)ModItems.GUN_PARTS.get(), 1), new ItemStack(Items.EMERALD, 4), 10, 15, 0.05F));
         ((List<ItemListing>)trades.get(3)).add((pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.GUNPOWDER, 8), new ItemStack(Items.EMERALD, 1), 16, 15, 0.05F));
         ((List<ItemListing>)trades.get(3))
            .add(
               (pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.EMERALD, 8), new ItemStack((ItemLike)ModItems.EXTENDED_MAG.get(), 1), 10, 10, 0.05F)
            );
         ((List<ItemListing>)trades.get(3))
            .add((pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.EMERALD, 15), new ItemStack((ItemLike)ModItems.MUSKET.get(), 1), 2, 12, 0.05F));
         ((List<ItemListing>)trades.get(4))
            .add(
               (pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.EMERALD, 25), new ItemStack((ItemLike)ModItems.FENCER_CARABINE.get(), 1), 4, 20, 0.05F)
            );
         ((List<ItemListing>)trades.get(4))
            .add(
               (pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.EMERALD, 16), new ItemStack((ItemLike)ModItems.LONG_SCOPE.get(), 1), 4, 20, 0.05F)
            );
         ((List<ItemListing>)trades.get(4))
            .add(
               (pTrader, pRandom) -> new MerchantOffer(new ItemCost((ItemLike)ModItems.MEDIUM_BRASS_CASING.get(), 6), new ItemStack(Items.EMERALD, 1), 10, 20, 0.05F)
            );
         ((List<ItemListing>)trades.get(4)).add((pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.BLAZE_POWDER, 4), new ItemStack(Items.EMERALD, 1), 12, 20, 0.05F));
         ((List<ItemListing>)trades.get(4))
            .add((pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack((ItemLike)ModItems.BLANK_MOLD.get(), 1), 5, 20, 0.05F));
         ((List<ItemListing>)trades.get(4))
            .add((pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.EMERALD, 32), new ItemStack((ItemLike)ModItems.SAKETINI.get(), 1), 1, 25, 0.05F));
         ((List<ItemListing>)trades.get(4))
            .add(
               (pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.EMERALD, 35), new ItemStack((ItemLike)ModItems.WINNIE_MILLEND.get(), 1), 1, 25, 0.05F)
            );
         ((List<ItemListing>)trades.get(5))
            .add(
               (pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.EMERALD, 25), new ItemStack((ItemLike)ModItems.IRON_FLARE.get(), 1), 1, 30, 0.05F)
            );
         ((List<ItemListing>)trades.get(5))
            .add(
               (pTrader, pRandom) -> new MerchantOffer(new ItemCost((ItemLike)ModItems.NITRO_POWDER.get(), 1), new ItemStack(Items.EMERALD, 28), 8, 25, 0.05F)
            );
         ((List<ItemListing>)trades.get(5))
            .add(
               (pTrader, pRandom) -> new MerchantOffer(new ItemCost((ItemLike)ModItems.ANCIENT_BRASS.get(), 32), new ItemStack((ItemLike)ModItems.WRECKER_FLARE.get(), 1), 8, 25, 0.05F)
            );
      }
   }

   @SubscribeEvent
   public static void addCustomWanderingTrades(WandererTradesEvent event) {
      List<ItemListing> rareTrades = event.getRareTrades();
      List<ItemListing> trades = event.getGenericTrades();
      trades.add(
         (pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.EMERALD, 24), new ItemStack((ItemLike)ModItems.SCAMP_PACKAGE.get(), 1), 1, 12, 0.15F)
      );
      trades.add(
         (pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.EMERALD, 24), new ItemStack((ItemLike)ModItems.WRECKER_FLARE.get(), 1), 1, 12, 0.15F)
      );
      trades.add(
         (pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.EMERALD, 29), new ItemStack((ItemLike)ModItems.WRECKER_FLARE.get(), 1), 1, 12, 0.15F)
      );
      trades.add(
         (pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.EMERALD, 29), new ItemStack((ItemLike)ModItems.OCEAN_FLARE.get(), 1), 1, 12, 0.15F)
      );
      rareTrades.add(
         (pTrader, pRandom) -> new MerchantOffer(new ItemCost(Items.EMERALD, 23), new ItemStack((ItemLike)ModItems.VEHEMENT_COAL.get(), 1), 1, 12, 0.15F)
      );
   }
}
