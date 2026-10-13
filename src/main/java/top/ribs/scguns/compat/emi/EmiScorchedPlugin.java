package top.ribs.scguns.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import top.ribs.scguns.client.screen.GunBenchRecipe;
import top.ribs.scguns.client.screen.LightningBatteryRecipe;
import top.ribs.scguns.client.screen.MaceratorRecipe;
import top.ribs.scguns.client.screen.MechanicalPressRecipe;
import top.ribs.scguns.client.screen.PoweredMaceratorRecipe;
import top.ribs.scguns.client.screen.PoweredMechanicalPressRecipe;
import top.ribs.scguns.common.recipe.ContainerRecipeInput;
import top.ribs.scguns.init.ModBlocks;

/**
 * EMI support for the Scorched Guns workstations.
 *
 * <p>EMI never ran this mod's JEI plugin: its JEI bridge only covers plugin calls it emulates, and registering a
 * recipe category is not one of them, so none of the six workstations appeared in EMI and favouriting a gun showed
 * no recipe at all. EMI has its own API, so this plugin mirrors each JEI category with an EMI one.</p>
 *
 * <p>Nothing here is reached unless EMI is installed, because EMI is the side that reads the service file that
 * names this class - the mod never calls into it, so a game without EMI never loads any of it.</p>
 */
@EmiEntrypoint
public class EmiScorchedPlugin implements EmiPlugin {
   public static final EmiRecipeCategory GUN_BENCH = category("gun_bench", ModBlocks.GUN_BENCH.get());
   public static final EmiRecipeCategory MACERATOR = category("macerating", ModBlocks.MACERATOR.get());
   public static final EmiRecipeCategory POWERED_MACERATOR = category("powered_macerating", ModBlocks.POWERED_MACERATOR.get());
   public static final EmiRecipeCategory MECHANICAL_PRESS = category("mechanical_pressing", ModBlocks.MECHANICAL_PRESS.get());
   public static final EmiRecipeCategory POWERED_MECHANICAL_PRESS =
      category("powered_mechanical_pressing", ModBlocks.POWERED_MECHANICAL_PRESS.get());
   public static final EmiRecipeCategory LIGHTNING_BATTERY = category("lightning_battery", ModBlocks.LIGHTNING_BATTERY.get());

   private static EmiRecipeCategory category(String path, net.minecraft.world.level.block.Block icon) {
      return new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath("scguns", path), EmiStack.of(icon));
   }

   @Override
   public void register(EmiRegistry registry) {
      add(registry, GUN_BENCH, ModBlocks.GUN_BENCH.get(), GunBenchRecipe.Type.INSTANCE);
      add(registry, MACERATOR, ModBlocks.MACERATOR.get(), MaceratorRecipe.Type.INSTANCE);
      add(registry, POWERED_MACERATOR, ModBlocks.POWERED_MACERATOR.get(), PoweredMaceratorRecipe.Type.INSTANCE);
      add(registry, MECHANICAL_PRESS, ModBlocks.MECHANICAL_PRESS.get(), MechanicalPressRecipe.Type.INSTANCE);
      add(registry, POWERED_MECHANICAL_PRESS, ModBlocks.POWERED_MECHANICAL_PRESS.get(),
         PoweredMechanicalPressRecipe.Type.INSTANCE);
      add(registry, LIGHTNING_BATTERY, ModBlocks.LIGHTNING_BATTERY.get(), LightningBatteryRecipe.Type.INSTANCE);
   }

   private static <T extends Recipe<ContainerRecipeInput>> void add(EmiRegistry registry, EmiRecipeCategory category,
      net.minecraft.world.level.block.Block workstation, RecipeType<T> type) {
      registry.addCategory(category);
      registry.addWorkstation(category, EmiStack.of(workstation));

      for (RecipeHolder<T> holder : registry.getRecipeManager().getAllRecipesFor(type)) {
         registry.addRecipe(new EmiScorchedRecipe(category, holder.id(), holder.value()));
      }
   }
}
