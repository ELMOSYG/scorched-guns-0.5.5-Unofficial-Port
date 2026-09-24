package top.ribs.scguns.network.message;


import top.ribs.scguns.util.NbtHelper;
import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.item.BlueprintItem;

public class C2SMessageClearBlueprintRecipe {
   private InteractionHand hand;

   public C2SMessageClearBlueprintRecipe() {
      super();
   }

   public C2SMessageClearBlueprintRecipe(InteractionHand hand) {
      super();
      this.hand = hand;
   }

   public void encode(C2SMessageClearBlueprintRecipe message, FriendlyByteBuf buffer) {
      buffer.writeEnum(message.hand);
   }

   public C2SMessageClearBlueprintRecipe decode(FriendlyByteBuf buffer) {
      return new C2SMessageClearBlueprintRecipe((InteractionHand)buffer.readEnum(InteractionHand.class));
   }

   public void handle(C2SMessageClearBlueprintRecipe message, MessageContext context) {
      context.execute(() -> {
         ServerPlayer player = context.getPlayer().map(p -> (ServerPlayer) p).orElse(null);
         if (player != null) {
            ItemStack blueprint = player.getItemInHand(message.hand);
            if (blueprint.getItem() instanceof BlueprintItem) {
               // 1.21: the write must go through a tag that belongs to this stack
               // alone, otherwise the removal is invisible to ItemStack.matches and
               // the client keeps showing the cleared recipe.
               CompoundTag blueprintTag = NbtHelper.getTagForWrite(blueprint);
               if (blueprintTag != null && blueprintTag.contains("ActiveRecipe")) {
                  blueprintTag.remove("ActiveRecipe");
                  if (blueprintTag.isEmpty()) {
                     NbtHelper.setTag(blueprint, null);
                  }
               }
            }
         }
      });
      context.setHandled(true);
   }
}
