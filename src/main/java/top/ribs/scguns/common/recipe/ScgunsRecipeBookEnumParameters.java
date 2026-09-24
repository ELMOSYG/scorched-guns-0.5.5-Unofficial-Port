package top.ribs.scguns.common.recipe;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import top.ribs.scguns.init.ModBlocks;
import top.ribs.scguns.init.ModItems;

/**
 * Constructor arguments for the mod's extended {@link RecipeBookCategories} constants.
 *
 * <p>{@code META-INF/enumextensions.json} points at these fields by name, because that enum's
 * constructor takes the tab icons as a {@code Supplier<List<ItemStack>>}.</p>
 *
 * <p>Deliberately in a <b>common</b> package although the enum it feeds is client-only, mirroring
 * Farmer's Delight's {@code common/EnumParameters}. Measured the hard way: with the proxies in a
 * {@code client} package the transformer could not resolve them and <b>silently dropped the whole
 * extension file</b> - including the {@code RecipeBookType} entry that needs no parameters at all,
 * which is what proved it was the file and not the entry (HANDOFF section 39.5). The lambda bodies are
 * only run when a tab is drawn, so referencing registry items here loads nothing early.</p>
 */
public final class ScgunsRecipeBookEnumParameters {
   /**
    * Search tab icon.
    *
    * <p>Vanilla draws its own search tab with a compass ({@code CRAFTING_SEARCH}), so the mod keeps
    * that convention. The gun bench block was used here before and players read it as "this tab is
    * the vanilla crafting table", because the bench texture is itself a workbench top (HANDOFF
    * section 39.7): a station icon on a tab that lists <b>this</b> station's recipes only adds
    * confusion, and the compass cannot be mistaken for a station.</p>
    */
   public static final EnumProxy<RecipeBookCategories> PROXY_SEARCH =
      new EnumProxy<>(RecipeBookCategories.class,
         (Supplier<List<ItemStack>>) () -> List.of(new ItemStack(Items.COMPASS)));

   /** Gun bench tab icon: the blueprint is the item this station keys its recipes on. */
   public static final EnumProxy<RecipeBookCategories> PROXY_MISC =
      new EnumProxy<>(RecipeBookCategories.class,
         (Supplier<List<ItemStack>>) () -> List.of(new ItemStack(ModItems.COPPER_BLUEPRINT.get())));

   /** Turret tab icon (HANDOFF section 44). */
   public static final EnumProxy<RecipeBookCategories> PROXY_TURRET =
      new EnumProxy<>(RecipeBookCategories.class,
         (Supplier<List<ItemStack>>) () -> List.of(new ItemStack(ModBlocks.BASIC_TURRET.get())));

   /** Exo suit tab icon: the chestplate stands for the set, as vanilla does for equipment. */
   public static final EnumProxy<RecipeBookCategories> PROXY_EXO_SUIT =
      new EnumProxy<>(RecipeBookCategories.class,
         (Supplier<List<ItemStack>>) () -> List.of(new ItemStack(ModItems.EXO_SUIT_CHESTPLATE.get())));

   private ScgunsRecipeBookEnumParameters() {
   }
}
