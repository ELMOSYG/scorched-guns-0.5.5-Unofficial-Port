package top.ribs.scguns.compat;

import com.illusivesoulworks.polymorph.common.capability.AbstractBlockEntityRecipeData;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.blockentity.GunBenchBlockEntity;
import top.ribs.scguns.client.screen.GunBenchMenu;

/**
 * What Polymorph needs to know about a gun bench: which item stacks the recipe is matched against.
 *
 * <p>Extends Polymorph's own base so the selection, listener and NBT handling come for free; the only thing it has
 * to supply is the input list. The ten attachment slots are the recipe's inputs - the output slot and the
 * blueprint in slots 10 and 11 are not offered to the recipe manager.</p>
 */
public class GunBenchRecipeData extends AbstractBlockEntityRecipeData<GunBenchBlockEntity> {
   public GunBenchRecipeData(GunBenchBlockEntity bench) {
      super(bench);
   }

   @Override
   public boolean isEmpty() {
      // The blueprint in slot 11 is not an ingredient, so a bench holding only a blueprint is empty as far as
      // recipe matching is concerned.
      for (int slot = 0; slot < GunBenchMenu.SLOT_OUTPUT; slot++) {
         if (!this.getOwner().getInventory().getItem(slot).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Override
   protected NonNullList<ItemStack> getInput() {
      NonNullList<ItemStack> input = NonNullList.create();
      for (int slot = 0; slot < GunBenchMenu.SLOT_OUTPUT; slot++) {
         input.add(this.getOwner().getInventory().getItem(slot));
      }

      return input;
   }
}
