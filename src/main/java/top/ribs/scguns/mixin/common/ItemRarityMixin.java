package top.ribs.scguns.mixin.common;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.ribs.scguns.util.RarityUtils;

@Mixin({ItemStack.class})
public abstract class ItemRarityMixin {
   public ItemRarityMixin() {
      super();
   }

   @Shadow
   public abstract Item getItem();

   @Inject(
      method = {"getRarity"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void changeRarity(CallbackInfoReturnable<Rarity> ci) {
      ci.setReturnValue(RarityUtils.GetRarityFromItem(this.getItem(), (Rarity)ci.getReturnValue()));
   }
}
