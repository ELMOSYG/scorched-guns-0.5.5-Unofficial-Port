package top.ribs.scguns.common.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.client.screen.GunBenchRecipe;
import top.ribs.scguns.init.ModBlocks;
import top.ribs.scguns.item.animated.ExoSuitItem;

/**
 * Which recipe-book tab a gun bench recipe belongs in.
 *
 * <p>The bench's book started as a single tab holding all 146 recipes. Searching it for turrets or for
 * the exo suit pieces meant scrolling, so those two groups get their own tabs, exactly like vanilla's
 * crafting book splits equipment from building blocks (HANDOFF section 44).</p>
 *
 * <p>Deliberately in a <b>common</b> package and free of any client type: the client turns these into
 * {@code RecipeBookCategories} through the enum extension, while {@code /scguns recipebook} reports the
 * same split server-side, so the two cannot drift apart.</p>
 *
 * <p>Classification is by what a recipe produces, not by recipe id or by the page a file happened to
 * live in: the result being a turret block makes it a turret, and any {@link ExoSuitItem} makes it an
 * exo suit piece. A recipe added later lands in the right tab on its own.</p>
 */
public final class GunBenchBookTabs {
   private GunBenchBookTabs() {
   }

   /** The bench's tabs, besides the search tab that aggregates them. */
   public enum Tab {
      /** Everything the other two tabs do not claim: the guns and their parts. */
      GUNS,
      /** The four turrets. */
      TURRETS,
      /** The four exo suit armour pieces. */
      EXO_SUIT
   }

   public static Tab of(GunBenchRecipe recipe) {
      ItemStack result = recipe.getResultItem(RegistryAccess.EMPTY);
      if (result.isEmpty()) {
         return Tab.GUNS;
      }

      Item item = result.getItem();
      if (item instanceof BlockItem blockItem && isTurretBlock(blockItem)) {
         return Tab.TURRETS;
      }

      if (item instanceof ExoSuitItem) {
         return Tab.EXO_SUIT;
      }

      return Tab.GUNS;
   }

   /**
    * The mod's turret blocks by identity.
    *
    * <p>They have no common base class to test against - all four extend {@code BaseEntityBlock}
    * directly - so the set is spelled out. A new turret has to be added here; the packaged-jar check in
    * {@code tools/verify_installed_jar.py} counts how many recipes each tab holds, so a turret that
    * lands in the wrong tab shows up.</p>
    */
   private static boolean isTurretBlock(BlockItem item) {
      return item.getBlock() == ModBlocks.AUTO_TURRET.get()
         || item.getBlock() == ModBlocks.BASIC_TURRET.get()
         || item.getBlock() == ModBlocks.SHOTGUN_TURRET.get()
         || item.getBlock() == ModBlocks.SNIPER_TURRET.get();
   }
}
