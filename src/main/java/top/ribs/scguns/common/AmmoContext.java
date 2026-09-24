package top.ribs.scguns.common;

import javax.annotation.Nullable;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public record AmmoContext(ItemStack stack, @Nullable Container container) {
   public static final AmmoContext NONE = new AmmoContext(ItemStack.EMPTY, null);

   public AmmoContext(ItemStack stack, @Nullable Container container) {
      this.stack = stack;
      this.container = container;
   }
}
