package top.ribs.scguns.common.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import com.mojang.serialization.MapCodec;

/**
 * Base class for the Scorched Guns recipe serialisers.
 *
 * <p>1.21 removed the JSON/network methods from {@code RecipeSerializer} and asks
 * for a {@link MapCodec} plus a {@link StreamCodec} instead. The 0.5.5 serialisers
 * keep their original {@code fromJson}/{@code fromNetwork}/{@code toNetwork}
 * implementations and this class supplies the two codecs on top of them, so the
 * recipe logic itself stays exactly as it was.</p>
 *
 * <p>Note on ids: 1.21 recipes no longer carry their id (the {@code RecipeManager}
 * keeps it in a {@code RecipeHolder}), while the datapack path is no longer handed
 * to the serialiser. Recipes created through the codec therefore get a placeholder
 * id; code that needs the real one has to read it from the holder.</p>
 */
public abstract class ScRecipeSerializer<T extends Recipe<?>> implements RecipeSerializer<T> {
    /** Stand-in for the id the codec based path can no longer provide. */
    public static final ResourceLocation UNKNOWN_ID = ResourceLocation.fromNamespaceAndPath("scguns", "unknown");

    protected abstract T fromJson(ResourceLocation recipeId, JsonObject json);

    protected abstract T fromNetwork(ResourceLocation recipeId, RegistryFriendlyByteBuf buffer);

    protected abstract void toNetwork(RegistryFriendlyByteBuf buffer, T recipe);

    /** Datapack writers; the recipes are shipped as json, so an empty object is enough. */
    protected JsonObject toJson(T recipe) {
        return new JsonObject();
    }

    private final MapCodec<T> codec = new LegacyRecipeCodec<>(
            json -> fromJson(UNKNOWN_ID, json),
            this::toJson);

    private final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec = StreamCodec.of(
            this::toNetwork,
            buffer -> fromNetwork(UNKNOWN_ID, buffer));

    @Override
    public MapCodec<T> codec() {
        return this.codec;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
        return this.streamCodec;
    }
}
