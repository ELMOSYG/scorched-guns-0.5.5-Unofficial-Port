package top.ribs.scguns.common.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import top.ribs.scguns.client.screen.GunBenchRecipe;
import top.ribs.scguns.init.ModItems;

/**
 * Which item unlocks a gun bench recipe in the recipe book.
 *
 * <p>One definition, used by three places that must agree: the login scan that awards recipes a player
 * already qualifies for ({@code GunProgressionEventHandler}), the {@code /scguns recipebook}
 * diagnostic, and the generated unlock advancements
 * ({@code tools/gen_gun_bench_unlocks.py}, which reads the same rule out of the recipe data). They used
 * to disagree: the scan skipped every recipe without a blueprint, so the four turrets could only ever
 * unlock through their advancement (HANDOFF section 43).</p>
 *
 * <p>For 142 of the 146 recipes the key is the recipe's blueprint. The four the player has no blueprint
 * for are exactly the four turrets (auto, basic, shotgun, sniper); their key item is the turret
 * platform, which they already require as their {@code gun_grip} ingredient - it is the turret
 * equivalent of a blueprint. Those four used to unlock on the player's first tick, which both made the
 * platform pointless and handed out recipes nobody had earned.</p>
 */
public final class GunBenchUnlockKeys {
   private GunBenchUnlockKeys() {
   }

   /** The stack that unlocks {@code recipe}, or an empty stack when nothing can. */
   public static ItemStack keyFor(GunBenchRecipe recipe) {
      Ingredient blueprint = recipe.getBlueprint();
      if (!blueprint.isEmpty()) {
         ItemStack[] options = blueprint.getItems();
         if (options.length > 0) {
            return options[0];
         }
      }

      return new ItemStack(ModItems.TURRET_PLATFORM.get());
   }

   /** Whether {@code carried} is the item that unlocks {@code recipe}. */
   public static boolean matches(GunBenchRecipe recipe, ItemStack carried) {
      if (carried.isEmpty()) {
         return false;
      }

      return keyFor(recipe).getItem() == carried.getItem();
   }
}
