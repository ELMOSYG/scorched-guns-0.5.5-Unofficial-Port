package top.ribs.scguns.network.message;



import net.minecraft.network.RegistryFriendlyByteBuf;
import top.ribs.scguns.util.DistHelper;
import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;

public class S2CShowTotemAnimationMessage {
   private ItemStack itemStack;

   public S2CShowTotemAnimationMessage() {
      super();
   }

   public S2CShowTotemAnimationMessage(ItemStack itemStack) {
      super();
      this.itemStack = itemStack;
   }

   public void encode(S2CShowTotemAnimationMessage message, RegistryFriendlyByteBuf buffer) {
      ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, message.itemStack);
   }

   public S2CShowTotemAnimationMessage decode(RegistryFriendlyByteBuf buffer) {
      S2CShowTotemAnimationMessage message = new S2CShowTotemAnimationMessage();
      message.itemStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
      return message;
   }

   public void handle(S2CShowTotemAnimationMessage message, MessageContext context) {
      context.execute(() -> DistHelper.runWhenOn(Dist.CLIENT, () -> {
               Minecraft minecraft = Minecraft.getInstance();
               minecraft.gameRenderer.displayItemActivation(message.itemStack);
            }));
      context.setHandled(true);
   }
}
