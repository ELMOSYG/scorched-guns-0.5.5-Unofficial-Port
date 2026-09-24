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
      this.heldItem = heldItem;
   }

   public S2CMessageMeleeAttack(RegistryFriendlyByteBuf buf) {
      super();
      this.heldItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
   }

   public void encode(S2CMessageMeleeAttack message, RegistryFriendlyByteBuf buf) {
      ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, message.heldItem);
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
