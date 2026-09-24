package top.ribs.scguns.compat;


import net.minecraft.world.item.crafting.RecipeHolder;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ItemLike;
import top.ribs.scguns.client.screen.GunBenchRecipe;
import top.ribs.scguns.client.screen.GunBenchScreen;
import top.ribs.scguns.client.screen.LightningBatteryRecipe;
import top.ribs.scguns.client.screen.LightningBatteryScreen;
import top.ribs.scguns.client.screen.MaceratorRecipe;
import top.ribs.scguns.client.screen.MaceratorScreen;
import top.ribs.scguns.client.screen.MechanicalPressRecipe;
import top.ribs.scguns.client.screen.MechanicalPressScreen;
import top.ribs.scguns.client.screen.PoweredMaceratorRecipe;
import top.ribs.scguns.client.screen.PoweredMaceratorScreen;
import top.ribs.scguns.client.screen.PoweredMechanicalPressRecipe;
import top.ribs.scguns.client.screen.PoweredMechanicalPressScreen;
import top.ribs.scguns.init.ModBlocks;
import top.ribs.scguns.init.ModItems;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@JeiPlugin
public class JEIScorchedPlugin implements IModPlugin {
   public JEIScorchedPlugin() {
      super();
   }

   public ResourceLocation getPluginUid() {
      return ResourceLocation.fromNamespaceAndPath("scguns", "jei_plugin");
   }

   public void registerCategories(IRecipeCategoryRegistration registration) {
      IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
      registration.addRecipeCategories(new IRecipeCategory[]{new GunBenchCategory(guiHelper)});
      registration.addRecipeCategories(new IRecipeCategory[]{new MaceratorCategory(guiHelper)});
      registration.addRecipeCategories(new IRecipeCategory[]{new PoweredMaceratorCategory(guiHelper)});
      registration.addRecipeCategories(new IRecipeCategory[]{new MechanicalPressCategory(guiHelper)});
      registration.addRecipeCategories(new IRecipeCategory[]{new PoweredMechanicalPressCategory(guiHelper)});
      registration.addRecipeCategories(new IRecipeCategory[]{new LightningBatteryCategory(guiHelper)});
   }

   public static MutableComponent getTranslation(String key, Object... args) {
      return Component.translatable("scguns." + key, args);
   }

   public void registerRecipes(IRecipeRegistration registration) {
      assert Minecraft.getInstance().level != null;

      RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModItems.REPAIR_KIT.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.repair_kit")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModBlocks.VENT_COLLECTOR.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.vent_collector")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModBlocks.NITER_GLASS.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.niter_glass")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModItems.BLASPHEMY.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.blasphemy")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModBlocks.LIGHTNING_BATTERY.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.lightning_battery")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModBlocks.LIGHTNING_ROD_CONNECTOR.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{getTranslation("jei.info.lightning_rod_connector")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModItems.PISTOL_AMMO_BOX.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.pistol_ammo_box")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModItems.RIFLE_AMMO_BOX.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.rifle_ammo_box")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModItems.SHOTGUN_AMMO_BOX.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.shotgun_ammo_box")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModItems.MAGNUM_AMMO_BOX.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.magnum_ammo_box")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModItems.ENERGY_AMMO_BOX.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.energy_ammo_box")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModItems.ROCKET_AMMO_BOX.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.rocket_ammo_box")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModItems.SPECIAL_AMMO_BOX.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.special_ammo_box")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModItems.EMPTY_CASING_POUCH.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.empty_casing_pouch")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModItems.CREATIVE_AMMO_BOX.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.creative_ammo_box")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModItems.DISHES_POUCH.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.dishes_pouch")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModItems.ROCK_POUCH.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.rock_pouch")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModItems.ANCIENT_BRASS.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.ancient_brass")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModItems.TEAM_LOG.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.team_log")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModItems.ENEMY_LOG.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.enemy_log")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModItems.SUPER_SHOTGUN.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.super_shotgun")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModItems.AUREOUS_SLAG.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.aureous_slag")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModBlocks.HOSTILE_TURRET_TARGETING_BLOCK.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{getTranslation("jei.info.hostile_turret_targeting_block")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModBlocks.PLAYER_TURRET_TARGETING_BLOCK.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{getTranslation("jei.info.player_turret_targeting_block")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModBlocks.TURRET_TARGETING_BLOCK.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{getTranslation("jei.info.turret_targeting_block")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModBlocks.FIRE_RATE_TURRET_MODULE.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{getTranslation("jei.info.fire_rate_turret_module")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModBlocks.DAMAGE_TURRET_MODULE.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{getTranslation("jei.info.damage_turret_module")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModBlocks.RANGE_TURRET_MODULE.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.range_turret_module")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModBlocks.SHELL_CATCHER_TURRET_MODULE.get()),
         VanillaTypes.ITEM_STACK,
         new Component[]{getTranslation("jei.info.shell_catcher_turret_module")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModBlocks.CRYONITER.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.cryoniter")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModBlocks.THERMOLITH.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.thermolith")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModBlocks.PENETRATOR.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.penetrator")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModItems.NITER_DUST.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.niter_dust")}
      );
      registration.addIngredientInfo(
         new ItemStack((ItemLike)ModBlocks.ADVANCED_COMPOSTER.get()), VanillaTypes.ITEM_STACK, new Component[]{getTranslation("jei.info.advanced_composter")}
      );
      List<GunBenchRecipe> gunBenchRecipes = recipeManager.getAllRecipesFor(GunBenchRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).toList();
      List<MaceratorRecipe> maceratorRecipes = recipeManager.getAllRecipesFor(MaceratorRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).toList();
      List<PoweredMaceratorRecipe> poweredMaceratorRecipes = recipeManager.getAllRecipesFor(PoweredMaceratorRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).toList();
      List<MechanicalPressRecipe> mechanicalPressRecipes = recipeManager.getAllRecipesFor(MechanicalPressRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).toList();
      List<PoweredMechanicalPressRecipe> poweredMechanicalPressRecipes = recipeManager.getAllRecipesFor(PoweredMechanicalPressRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).toList();
      registration.addRecipes(GunBenchCategory.GUN_BENCH_TYPE, gunBenchRecipes);
      registration.addRecipes(MaceratorCategory.MACERATING_TYPE, maceratorRecipes);
      registration.addRecipes(PoweredMaceratorCategory.POWERED_MACERATING_TYPE, poweredMaceratorRecipes);
      registration.addRecipes(MechanicalPressCategory.MECHANICAL_PRESS_TYPE, mechanicalPressRecipes);
      registration.addRecipes(PoweredMechanicalPressCategory.POWERED_MECHANICAL_PRESS_TYPE, poweredMechanicalPressRecipes);
      registration.addRecipes(LightningBatteryCategory.LIGHTNING_BATTERY_TYPE, recipeManager.getAllRecipesFor(LightningBatteryRecipe.Type.INSTANCE).stream().map(RecipeHolder::value).toList());
   }

   public void registerGuiHandlers(IGuiHandlerRegistration registration) {
      registration.addRecipeClickArea(GunBenchScreen.class, 100, 47, 30, 20, new RecipeType[]{GunBenchCategory.GUN_BENCH_TYPE});
      registration.addRecipeClickArea(MaceratorScreen.class, 80, 25, 30, 20, new RecipeType[]{MaceratorCategory.MACERATING_TYPE});
      registration.addRecipeClickArea(PoweredMaceratorScreen.class, 80, 25, 25, 20, new RecipeType[]{PoweredMaceratorCategory.POWERED_MACERATING_TYPE});
      registration.addRecipeClickArea(MechanicalPressScreen.class, 80, 25, 25, 20, new RecipeType[]{MechanicalPressCategory.MECHANICAL_PRESS_TYPE});
      registration.addRecipeClickArea(
         PoweredMechanicalPressScreen.class, 80, 25, 25, 20, new RecipeType[]{PoweredMechanicalPressCategory.POWERED_MECHANICAL_PRESS_TYPE}
      );
      registration.addRecipeClickArea(LightningBatteryScreen.class, 80, 32, 25, 20, new RecipeType[]{LightningBatteryCategory.LIGHTNING_BATTERY_TYPE});
   }

   public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
      registration.addRecipeCatalyst(new ItemStack((ItemLike)ModBlocks.GUN_BENCH.get()), new RecipeType[]{GunBenchCategory.GUN_BENCH_TYPE});
      registration.addRecipeCatalyst(new ItemStack((ItemLike)ModBlocks.MACERATOR.get()), new RecipeType[]{MaceratorCategory.MACERATING_TYPE});
      registration.addRecipeCatalyst(
         new ItemStack((ItemLike)ModBlocks.POWERED_MACERATOR.get()), new RecipeType[]{PoweredMaceratorCategory.POWERED_MACERATING_TYPE}
      );
      registration.addRecipeCatalyst(new ItemStack((ItemLike)ModBlocks.MECHANICAL_PRESS.get()), new RecipeType[]{MechanicalPressCategory.MECHANICAL_PRESS_TYPE});
      registration.addRecipeCatalyst(
         new ItemStack((ItemLike)ModBlocks.POWERED_MECHANICAL_PRESS.get()), new RecipeType[]{PoweredMechanicalPressCategory.POWERED_MECHANICAL_PRESS_TYPE}
      );
      registration.addRecipeCatalyst(
         new ItemStack((ItemLike)ModBlocks.LIGHTNING_BATTERY.get()), new RecipeType[]{LightningBatteryCategory.LIGHTNING_BATTERY_TYPE}
      );
   }

   public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
      registration.addRecipeTransferHandler(new GunBenchTransferInfo());
   }
}
