package top.ribs.scguns.init;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import top.ribs.scguns.client.screen.GunBenchRecipe;
import top.ribs.scguns.client.screen.LightningBatteryRecipe;
import top.ribs.scguns.client.screen.MaceratorRecipe;
import top.ribs.scguns.client.screen.MechanicalPressRecipe;
import top.ribs.scguns.client.screen.PoweredMaceratorRecipe;
import top.ribs.scguns.client.screen.PoweredMechanicalPressRecipe;

public class ModRecipes {
   public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, "scguns");
   public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MechanicalPressRecipe>> MECHANICAL_PRESS_SERIALIZER = SERIALIZERS.register(
      "mechanical_pressing", () -> MechanicalPressRecipe.Serializer.INSTANCE
   );
   public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PoweredMechanicalPressRecipe>> POWERED_MECHANICAL_PRESS_SERIALIZER = SERIALIZERS.register(
      "powered_mechanical_pressing", () -> PoweredMechanicalPressRecipe.Serializer.INSTANCE
   );
   public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MaceratorRecipe>> MACERATOR_SERIALIZER = SERIALIZERS.register(
      "macerating", () -> MaceratorRecipe.Serializer.INSTANCE
   );
   public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PoweredMaceratorRecipe>> POWERED_MACERATOR_SERIALIZER = SERIALIZERS.register(
      "powered_macerating", () -> PoweredMaceratorRecipe.Serializer.INSTANCE
   );
   public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GunBenchRecipe>> GUN_BENCH_SERIALIZER = SERIALIZERS.register(
      "gun_bench", () -> GunBenchRecipe.Serializer.INSTANCE
   );
   public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LightningBatteryRecipe>> LIGHTNING_BATTERY_SERIALIZER = SERIALIZERS.register(
      "lightning_battery", () -> LightningBatteryRecipe.Serializer.INSTANCE
   );

   public ModRecipes() {
      super();
   }

   public static void register(IEventBus eventBus) {
      SERIALIZERS.register(eventBus);
   }
}
