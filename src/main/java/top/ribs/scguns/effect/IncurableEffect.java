package top.ribs.scguns.effect;

import java.util.Collections;
import java.util.List;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;

public class IncurableEffect extends MobEffect {
   public IncurableEffect(MobEffectCategory typeIn, int liquidColorIn) {
      super(typeIn, liquidColorIn);
   }

   public List<ItemStack> getCurativeItems() {
      return Collections.emptyList();
   }
}
