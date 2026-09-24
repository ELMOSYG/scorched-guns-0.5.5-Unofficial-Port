package top.ribs.scguns.common.recipe;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * 1.21 recipe lookup adapter.
 *
 * <p>Recipes and {@code RecipeManager#getRecipeFor} take a {@link RecipeInput}
 * now; the Scorched Guns machines already own a {@link Container}, so this wraps
 * one without copying the stacks.</p>
 */
public class ContainerRecipeInput implements RecipeInput {
    private final Container container;

    public ContainerRecipeInput(Container container) {
        this.container = container;
    }

    public Container container() {
        return this.container;
    }

    @Override
    public ItemStack getItem(int index) {
        return this.container.getItem(index);
    }

    @Override
    public int size() {
        return this.container.getContainerSize();
    }
}
