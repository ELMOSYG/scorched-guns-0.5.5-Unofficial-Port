package top.ribs.scguns.event;


import net.neoforged.bus.api.ICancellableEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class GunReloadEvent extends PlayerEvent implements ICancellableEvent {
   private final ItemStack stack;

   public GunReloadEvent(Player player, ItemStack stack) {
      super(player);
      this.stack = stack;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public boolean isClient() {
      return this.getEntity().getCommandSenderWorld().isClientSide();
   }

   public static class Post extends GunReloadEvent {
      public Post(Player player, ItemStack stack) {
         super(player, stack);
      }
   }

   public static class Pre extends GunReloadEvent {
      public Pre(Player player, ItemStack stack) {
         super(player, stack);
      }
   }
}
