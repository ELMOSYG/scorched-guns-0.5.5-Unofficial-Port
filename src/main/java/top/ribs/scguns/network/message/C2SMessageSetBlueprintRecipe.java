package top.ribs.scguns.network.message;


import top.ribs.scguns.util.NbtHelper;
import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.item.BlueprintItem;

public class C2SMessageSetBlueprintRecipe {
   private InteractionHand hand;
   private String recipeId;

   public C2SMessageSetBlueprintRecipe() {
      super();
   }

   public C2SMessageSetBlueprintRecipe(InteractionHand hand, String recipeId) {
      super();
      this.hand = hand;
      this.recipeId = recipeId;
   }

   public void encode(C2SMessageSetBlueprintRecipe message, FriendlyByteBuf buffer) {
      buffer.writeEnum(message.hand);
      buffer.writeUtf(message.recipeId);
   }

   public C2SMessageSetBlueprintRecipe decode(FriendlyByteBuf buffer) {
      return new C2SMessageSetBlueprintRecipe((InteractionHand)buffer.readEnum(InteractionHand.class), buffer.readUtf());
   }

   public void handle(C2SMessageSetBlueprintRecipe message, MessageContext context) {
      context.execute(() -> {
         ServerPlayer player = context.getPlayer().map(p -> (ServerPlayer) p).orElse(null);
         if (player != null) {
            ItemStack blueprint = player.getItemInHand(message.hand);
            if (blueprint.getItem() instanceof BlueprintItem) {
               NbtHelper.getOrCreateTag(blueprint).putString("ActiveRecipe", message.recipeId);
            }
         }
      });
      context.setHandled(true);
   }
}
