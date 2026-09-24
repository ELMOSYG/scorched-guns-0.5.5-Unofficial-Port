package top.ribs.scguns.util;


import top.ribs.scguns.util.ScEnchants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.init.ModEnchantments;
import top.ribs.scguns.item.GunItem;

public class GunCurseUtil {
   private static final float MOB_GUN_CURSE_CHANCE = 0.85F;

   public GunCurseUtil() {
      super();
   }

   public static ItemStack applyCurseIfRoll(ItemStack stack, RandomSource random) {
      if (!stack.isEmpty() && stack.getItem() instanceof GunItem) {
         if (random.nextFloat() < 0.85F) {
            ScEnchants.enchant(stack, ModEnchantments.GUN_RUST, 1);
         }

         return stack;
      } else {
         return stack;
      }
   }

   public static boolean isCursed(ItemStack stack) {
      return ScEnchants.level(stack, ModEnchantments.GUN_RUST) > 0;
   }

   public static void removeCurse(ItemStack stack) {
      if (isCursed(stack)) {
         stack.remove(DataComponents.ENCHANTMENTS);
      }
   }
}
