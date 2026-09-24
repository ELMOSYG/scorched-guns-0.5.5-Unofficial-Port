package top.ribs.scguns.item;

import javax.annotation.Nonnull;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

public class CurioAmmoBox implements ICurio {
   private final ItemStack stack;

   public CurioAmmoBox(ItemStack stack) {
      super();
      this.stack = stack;
   }

   @Nonnull
   public ItemStack getStack() {
      return this.stack;
   }

   public void curioTick(SlotContext slotContext) {
   }
}
