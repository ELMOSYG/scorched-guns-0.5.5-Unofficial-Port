package top.ribs.scguns.event;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import top.ribs.scguns.item.ammo_boxes.DishesPouch;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME
)
public class DishesEventHandler {
   public DishesEventHandler() {
      super();
   }

   @SubscribeEvent
   public static void onItemUseFinish(Finish event) {
      if (event.getEntity() instanceof Player) {
         Player player = (Player)event.getEntity();
         ItemStack resultStack = event.getResultStack();
         if (isDishItem(resultStack) && addDishToPouch(player, resultStack)) {
            event.setResultStack(ItemStack.EMPTY);
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public static void onEntityItemPickup(ItemEntityPickupEvent.Pre event) {
      Player player = event.getPlayer();
      ItemStack pickedItem = event.getItemEntity().getItem();
      if (isDishItem(pickedItem) && addDishToPouch(player, pickedItem)) {
         event.setCanPickup(TriState.FALSE);
         event.getItemEntity().getItem().setCount(0);
      }
   }

   private static boolean isDishItem(ItemStack stack) {
      return stack.is(Items.BOWL)
         || stack.is(Items.GLASS_BOTTLE)
         || stack.getItem().getCraftingRemainingItem() == Items.BOWL
         || stack.getItem().getCraftingRemainingItem() == Items.GLASS_BOTTLE;
   }

   private static boolean addDishToPouch(Player player, ItemStack dishStack) {
      for (ItemStack itemStack : player.getInventory().items) {
         if (itemStack.getItem() instanceof DishesPouch) {
            int insertedItems = DishesPouch.add(itemStack, dishStack);
            if (insertedItems > 0) {
               dishStack.shrink(insertedItems);
               return true;
            }
         }
      }

      AtomicBoolean result = new AtomicBoolean(false);
      CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
         IItemHandlerModifiable curios = handler.getEquippedCurios();

         for (int i = 0; i < curios.getSlots(); i++) {
            ItemStack stack = curios.getStackInSlot(i);
            if (stack.getItem() instanceof DishesPouch) {
               int insertedItemsx = DishesPouch.add(stack, dishStack);
               if (insertedItemsx > 0) {
                  dishStack.shrink(insertedItemsx);
                  result.set(true);
                  break;
               }
            }
         }
      });
      return result.get();
   }
}
