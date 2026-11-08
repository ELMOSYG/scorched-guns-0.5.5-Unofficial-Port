package top.ribs.scguns.network.message;


import net.minecraft.network.RegistryFriendlyByteBuf;
import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.client.handler.ClientMeleeAttackHandler;
import top.ribs.scguns.item.GunItem;

public class S2CMessageMeleeAttack {
   private ItemStack heldItem;

   public S2CMessageMeleeAttack() {
      super();
   }

   public S2CMessageMeleeAttack(ItemStack heldItem) {
      super();
      // Copy, never keep the caller's stack. The packet is encoded later on the network thread while the server
      // is still ticking: attacking during a reload mutates the same stack's tag (IsReloading and friends), and
      // encoding a stack whose NBT map is being written threw ConcurrentModificationException and dropped the
      // player from the server with a protocol error.
      this.heldItem = heldItem == null || heldItem.isEmpty() ? ItemStack.EMPTY : heldItem.copy();
   }

   public S2CMessageMeleeAttack(RegistryFriendlyByteBuf buf) {
      super();
      this.heldItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
   }

   public void encode(S2CMessageMeleeAttack message, RegistryFriendlyByteBuf buf) {
      ItemStack stack = message.heldItem == null ? ItemStack.EMPTY : message.heldItem;
      ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
   }

   public S2CMessageMeleeAttack decode(RegistryFriendlyByteBuf buf) {
      return new S2CMessageMeleeAttack(buf);
   }

   public void handle(S2CMessageMeleeAttack message, MessageContext context) {
      context.execute(() -> {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            ItemStack currentHeldItem = player.getMainHandItem();
            if (currentHeldItem.getItem() instanceof GunItem gunItem) {
               ClientMeleeAttackHandler.startMeleeAnimation(gunItem, currentHeldItem);
            }
         }
      });
      context.setHandled(true);
   }
}
