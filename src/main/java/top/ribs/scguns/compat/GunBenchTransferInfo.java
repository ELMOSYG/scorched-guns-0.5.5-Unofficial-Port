package top.ribs.scguns.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.Ingredient;
import top.ribs.scguns.client.screen.GunBenchMenu;
import top.ribs.scguns.client.screen.GunBenchRecipe;
import top.ribs.scguns.client.screen.ModMenuTypes;

public class GunBenchTransferInfo implements IRecipeTransferInfo<GunBenchMenu, GunBenchRecipe> {
   public GunBenchTransferInfo() {
      super();
   }

   public Class<GunBenchMenu> getContainerClass() {
      return GunBenchMenu.class;
   }

   public Optional<MenuType<GunBenchMenu>> getMenuType() {
      return Optional.of((MenuType<GunBenchMenu>)ModMenuTypes.GUN_BENCH.get());
   }

   public RecipeType<GunBenchRecipe> getRecipeType() {
      return GunBenchCategory.GUN_BENCH_TYPE;
   }

   public boolean canHandle(GunBenchMenu container, GunBenchRecipe recipe) {
      return true;
   }

   public List<Slot> getRecipeSlots(GunBenchMenu container, GunBenchRecipe recipe) {
      List<Slot> slots = new ArrayList<>();
      // Attachment slot i is menu slot MENU_SLOT_GRID_START + i, not slot i: menu slot 0 is the
      // output. Indexing with the ingredient number moved the output slot and missed the last
      // attachment slot entirely (HANDOFF section 42.8). The ten attachment slots come from
      // getModuleIngredients(), because the recipe's combined list ends with the blueprint, which
      // has its own slot in the menu and is appended below.
      NonNullList<Ingredient> ingredients = recipe.getModuleIngredients();

      for (int i = 0; i < ingredients.size(); i++) {
         if (!((Ingredient)ingredients.get(i)).isEmpty()) {
            slots.add(container.getSlot(GunBenchMenu.MENU_SLOT_GRID_START + i));
         }
      }

      if (!recipe.getBlueprint().isEmpty()) {
         slots.add(container.getSlot(GunBenchMenu.MENU_SLOT_BLUEPRINT));
      }

      return slots;
   }

   public List<Slot> getInventorySlots(GunBenchMenu container, GunBenchRecipe recipe) {
      List<Slot> slots = new ArrayList<>();

      for (int i = 12; i < container.slots.size(); i++) {
         slots.add(container.getSlot(i));
      }

      return slots;
   }
}
