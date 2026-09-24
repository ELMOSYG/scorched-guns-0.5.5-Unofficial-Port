package top.ribs.scguns.event;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import top.ribs.scguns.item.ammo_boxes.RockPouch;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME
)
public class RockPickupEventHandler {
   public RockPickupEventHandler() {
      super();
   }

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public static void onEntityItemPickup(ItemEntityPickupEvent.Pre event) {
      Player player = event.getPlayer();
      ItemStack pickedItem = event.getItemEntity().getItem();
      if (isRockItem(pickedItem) && addRockToPouch(player, pickedItem)) {
         event.setCanPickup(TriState.FALSE);
         event.getItemEntity().getItem().setCount(0);
      }
   }

   private static boolean isRockItem(ItemStack stack) {
      return stack.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("scguns", "rocks")));
   }

   private static boolean addRockToPouch(Player player, ItemStack rockStack) {
      for (ItemStack itemStack : player.getInventory().items) {
         if (itemStack.getItem() instanceof RockPouch) {
            int insertedItems = RockPouch.add(itemStack, rockStack);
            if (insertedItems > 0) {
               rockStack.shrink(insertedItems);
               return true;
            }
         }
      }

      AtomicBoolean result = new AtomicBoolean(false);
      CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
         IItemHandlerModifiable curios = handler.getEquippedCurios();

         for (int i = 0; i < curios.getSlots(); i++) {
            ItemStack stack = curios.getStackInSlot(i);
            if (stack.getItem() instanceof RockPouch) {
               int insertedItemsx = RockPouch.add(stack, rockStack);
               if (insertedItemsx > 0) {
                  rockStack.shrink(insertedItemsx);
                  result.set(true);
                  break;
               }
            }
         }
      });
      return result.get();
   }
}
