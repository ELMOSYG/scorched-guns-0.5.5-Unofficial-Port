package top.ribs.scguns.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import top.ribs.scguns.client.screen.GunBenchRecipe;
import top.ribs.scguns.client.screen.LightningBatteryRecipe;
import top.ribs.scguns.client.screen.MaceratorRecipe;
import top.ribs.scguns.client.screen.MechanicalPressRecipe;
import top.ribs.scguns.client.screen.PoweredMaceratorRecipe;
import top.ribs.scguns.client.screen.PoweredMechanicalPressRecipe;
import top.ribs.scguns.common.recipe.ContainerRecipeInput;
import top.ribs.scguns.init.ModBlocks;

/**
 * EMI pages for the six Scorched Guns workstations.
 *
 * <p>Each page uses the workstation's own GUI picture and the slot coordinates from the matching JEI category, so
 * EMI and JEI show the same workstation; EMI's JEI bridge does not register plugin categories, which is why these
 * exist. Every slot also carries the recipe context and the backing recipe, which is what links a gun to the
 * recipes that produce it in EMI's item list and favourites panel.</p>
 *
 * <p>Nothing here runs unless EMI is installed: EMI is the side that reads the service file naming this class.</p>
 */
@EmiEntrypoint
public class EmiScorchedPlugin implements EmiPlugin {
   public static final EmiRecipeCategory GUN_BENCH = category("gun_bench", ModBlocks.GUN_BENCH.get(), "block.scguns.gun_bench");
   public static final EmiRecipeCategory MACERATOR = category("macerating", ModBlocks.MACERATOR.get(), "block.scguns.macerator");
   public static final EmiRecipeCategory POWERED_MACERATOR =
      category("powered_macerating", ModBlocks.POWERED_MACERATOR.get(), "block.scguns.powered_macerator");
   public static final EmiRecipeCategory MECHANICAL_PRESS =
      category("mechanical_pressing", ModBlocks.MECHANICAL_PRESS.get(), "block.scguns.mechanical_press");
   public static final EmiRecipeCategory POWERED_MECHANICAL_PRESS = category("powered_mechanical_pressing",
      ModBlocks.POWERED_MECHANICAL_PRESS.get(), "block.scguns.powered_mechanical_press");
   public static final EmiRecipeCategory LIGHTNING_BATTERY =
      category("lightning_battery", ModBlocks.LIGHTNING_BATTERY.get(), "block.scguns.lightning_battery");

   private static EmiRecipeCategory category(String path, Block icon, String translationKey) {
      return new ScorchedEmiCategory(ResourceLocation.fromNamespaceAndPath("scguns", path), icon, translationKey);
   }

   private static ResourceLocation gui(String file) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/gui/" + file + ".png");
   }

   @Override
   public void register(EmiRegistry registry) {
      ScorchedEmiLayout bench = new ScorchedEmiLayout(GUN_BENCH, gui("gun_bench_gui"), 22, 12, 141, 59);
      ScorchedEmiLayout macerator = new ScorchedEmiLayout(MACERATOR, gui("macerator_gui"), 40, 26, 93, 54);
      ScorchedEmiLayout poweredMacerator = new ScorchedEmiLayout(POWERED_MACERATOR, gui("powered_macerator_gui"), 11, 16, 128, 54);
      ScorchedEmiLayout press = new ScorchedEmiLayout(MECHANICAL_PRESS, gui("mechanical_press_gui"), 20, 26, 120, 54);
      ScorchedEmiLayout poweredPress = new ScorchedEmiLayout(POWERED_MECHANICAL_PRESS, gui("powered_mechanical_press_gui"), 11, 16, 128, 54);
      ScorchedEmiLayout battery = new ScorchedEmiLayout(LIGHTNING_BATTERY, gui("lightning_battery_gui"), 41, 16, 96, 54);

      workstation(registry, GUN_BENCH, ModBlocks.GUN_BENCH.get());
      workstation(registry, MACERATOR, ModBlocks.MACERATOR.get());
      workstation(registry, POWERED_MACERATOR, ModBlocks.POWERED_MACERATOR.get());
      workstation(registry, MECHANICAL_PRESS, ModBlocks.MECHANICAL_PRESS.get());
      workstation(registry, POWERED_MECHANICAL_PRESS, ModBlocks.POWERED_MECHANICAL_PRESS.get());
      workstation(registry, LIGHTNING_BATTERY, ModBlocks.LIGHTNING_BATTERY.get());

      // Slot coordinates below are taken from the JEI categories verbatim, including the -22/-12 shift that turns
      // the bench's GUI-absolute positions into crop-relative ones.
      int[] benchX = {26, 44, 62, 80, 26, 44, 62, 80, 26, 62, 116};
      int[] benchY = {17, 17, 17, 17, 35, 35, 35, 35, 53, 53, 17};
      for (RecipeHolder<GunBenchRecipe> holder : sorted(registry, GunBenchRecipe.Type.INSTANCE)) {
         GunBenchRecipe recipe = holder.value();
         List<EmiScorchedRecipe.Slot> slots = new ArrayList<>();
         NonNullList<Ingredient> modules = recipe.getModuleIngredients();
         for (int i = 0; i < modules.size() && i < 10; i++) {
            if (!modules.get(i).isEmpty()) {
               slots.add(new EmiScorchedRecipe.Slot(benchX[i] - 22, benchY[i] - 12, EmiIngredient.of(modules.get(i))));
            }
         }

         if (!recipe.getBlueprint().isEmpty()) {
            slots.add(new EmiScorchedRecipe.Slot(benchX[10] - 22, benchY[10] - 12, EmiIngredient.of(recipe.getBlueprint())));
         }

         registry.addRecipe(new EmiScorchedRecipe(bench, holder, slots, result(recipe), 118, 32));
      }

      for (RecipeHolder<MaceratorRecipe> holder : sorted(registry, MaceratorRecipe.Type.INSTANCE)) {
         registry.addRecipe(new EmiScorchedRecipe(macerator, holder, grid(holder.value().getIngredients(), 4, 1, 2),
            result(holder.value()), 74, 1));
      }

      for (RecipeHolder<PoweredMaceratorRecipe> holder : sorted(registry, PoweredMaceratorRecipe.Type.INSTANCE)) {
         registry.addRecipe(new EmiScorchedRecipe(poweredMacerator, holder, grid(holder.value().getIngredients(), 33, 11, 2),
            result(holder.value()), 103, 11));
      }

      for (RecipeHolder<MechanicalPressRecipe> holder : sorted(registry, MechanicalPressRecipe.Type.INSTANCE)) {
         MechanicalPressRecipe recipe = holder.value();
         registry.addRecipe(new EmiScorchedRecipe(press, holder, pressSlots(recipe.getIngredients(), recipe.getMoldItem(), 6, 1),
            result(recipe), 94, 1));
      }

      for (RecipeHolder<PoweredMechanicalPressRecipe> holder : sorted(registry, PoweredMechanicalPressRecipe.Type.INSTANCE)) {
         PoweredMechanicalPressRecipe recipe = holder.value();
         registry.addRecipe(new EmiScorchedRecipe(poweredPress, holder,
            pressSlots(recipe.getIngredients(), recipe.getMoldItem(), 15, 11), result(recipe), 103, 11));
      }

      for (RecipeHolder<LightningBatteryRecipe> holder : sorted(registry, LightningBatteryRecipe.Type.INSTANCE)) {
         LightningBatteryRecipe recipe = holder.value();
         List<EmiScorchedRecipe.Slot> slots = new ArrayList<>();
         if (!recipe.getInput().isEmpty()) {
            slots.add(new EmiScorchedRecipe.Slot(15, 19, EmiIngredient.of(recipe.getInput())));
         }

         registry.addRecipe(new EmiScorchedRecipe(battery, holder, slots, result(recipe), 75, 19));
      }
   }

   private static void workstation(EmiRegistry registry, EmiRecipeCategory category, Block block) {
      registry.addCategory(category);
      registry.addWorkstation(category, EmiStack.of(block));
   }

   /** Up to four ingredients in a two-wide grid, as both macerator categories lay them out. */
   private static List<EmiScorchedRecipe.Slot> grid(NonNullList<Ingredient> ingredients, int baseX, int baseY, int spacing) {
      List<EmiScorchedRecipe.Slot> slots = new ArrayList<>();
      for (int i = 0; i < 4 && i < ingredients.size(); i++) {
         if (!ingredients.get(i).isEmpty()) {
            slots.add(new EmiScorchedRecipe.Slot(baseX + i % 2 * spacing, baseY + i / 2 * spacing,
               EmiIngredient.of(ingredients.get(i))));
         }
      }

      return slots;
   }

   /** Up to three ingredients in a row with the mold below the middle one, as both press categories do. */
   private static List<EmiScorchedRecipe.Slot> pressSlots(NonNullList<Ingredient> ingredients, Ingredient mold, int baseX, int baseY) {
      List<EmiScorchedRecipe.Slot> slots = new ArrayList<>();
      for (int i = 0; i < 3 && i < ingredients.size(); i++) {
         if (!ingredients.get(i).isEmpty()) {
            slots.add(new EmiScorchedRecipe.Slot(baseX + i * 18, baseY, EmiIngredient.of(ingredients.get(i))));
         }
      }

      if (mold != null && !mold.isEmpty()) {
         slots.add(new EmiScorchedRecipe.Slot(baseX + 18, baseY + 18, EmiIngredient.of(mold)));
      }

      return slots;
   }

   private static EmiStack result(Recipe<ContainerRecipeInput> recipe) {
      return EmiStack.of(recipe.getResultItem(RegistryAccess.EMPTY));
   }

   /** Registry order is hash order, which shuffled the pages; sorting by id matches how JEI presents them. */
   private static <T extends Recipe<ContainerRecipeInput>> List<RecipeHolder<T>> sorted(EmiRegistry registry, RecipeType<T> type) {
      List<RecipeHolder<T>> holders = new ArrayList<>(registry.getRecipeManager().getAllRecipesFor(type));
      holders.sort(Comparator.comparing(holder -> holder.id().toString()));
      return holders;
   }
}
