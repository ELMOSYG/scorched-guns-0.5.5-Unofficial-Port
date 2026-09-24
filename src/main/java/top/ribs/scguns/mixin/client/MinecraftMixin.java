package top.ribs.scguns.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.ribs.scguns.item.GunItem;

/**
 * Suppresses the vanilla "item used" swing for guns.
 *
 * <p>0.5.5 did this with an {@code @Inject} at the {@code itemUsed} call plus
 * {@code locals = LocalCapture.CAPTURE_FAILHARD}, reading the held stack out of a
 * captured local. That is not portable: the captured locals are a property of the
 * target method's LocalVariableTable, and 1.21.1's {@code Minecraft#startUseItem}
 * no longer has the {@code InteractionHand[]} local the callback declared (the real
 * locals are {@code interactionhand}, {@code inputEvent}, {@code itemstack},
 * {@code blockhitresult} and one int). A failed local capture is a hard error, so
 * the mixin would have crashed the client at the first right-click.</p>
 *
 * <p>Redirecting the call instead derives the callback signature from the
 * <i>redirected method</i> (receiver + arguments), which does not depend on locals
 * at all, and the hand is known exactly. The only behavioural difference from
 * cancelling the whole method is that whatever follows the call in
 * {@code startUseItem} still runs.</p>
 */
@Mixin({Minecraft.class})
public class MinecraftMixin {
   public MinecraftMixin() {
      super();
   }

   @Redirect(
      method = {"startUseItem"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;itemUsed(Lnet/minecraft/world/InteractionHand;)V",
         ordinal = 0
      )
   )
   private void skipItemUsedForGuns(ItemInHandRenderer renderer, InteractionHand hand) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return;
      }

      ItemStack stack = player.getItemInHand(hand);
      if (!(stack.getItem() instanceof GunItem)) {
         renderer.itemUsed(hand);
      }
   }
}
