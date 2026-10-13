package top.ribs.scguns.compat;

import com.illusivesoulworks.polymorph.api.client.base.PersistentRecipesWidget;
import net.minecraft.world.inventory.Slot;
import top.ribs.scguns.client.screen.GunBenchScreen;

/**
 * The recipe selector Polymorph draws beside the gun bench's output slot.
 *
 * <p>Polymorph attaches its selector to a screen through {@link GunBenchPolymorphClient}; a custom screen gets none
 * unless one is registered, which is why the bench showed no selector at all even with the recipe data in place.</p>
 */
public class GunBenchRecipesWidget extends PersistentRecipesWidget {
   public GunBenchRecipesWidget(GunBenchScreen screen) {
      super(screen);
   }

   /**
    * The slot Polymorph anchors the selector to.
    *
    * <p>The bench deliberately adds its output as view slot 0 - the recipe book's ghost placement depends on it
    * (see GunBenchMenu) - so this is the output, not the first attachment slot.</p>
    */
   @Override
   public Slot getOutputSlot() {
      return this.containerScreen.getMenu().slots.get(0);
   }
}
