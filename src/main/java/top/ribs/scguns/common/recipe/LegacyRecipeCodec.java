package top.ribs.scguns.common.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Bridges a legacy "flat JSON object" recipe reader/writer into the {@link MapCodec}
 * the 1.21 {@code RecipeSerializer} contract asks for.
 *
 * <p>The 1.20.1 serialisers read and wrote a recipe as a plain JSON object with
 * {@code GsonHelper} calls. Rather than rewriting every recipe into a
 * {@code RecordCodecBuilder} definition, this codec converts the map that
 * serialisation hands over into the very {@link JsonObject} those readers already
 * understand, and merges the writer's object back into the record builder.</p>
 */
final class LegacyRecipeCodec<T> extends MapCodec<T> {
    private final Function<JsonObject, T> decoder;
    private final Function<T, JsonObject> encoder;

    LegacyRecipeCodec(Function<JsonObject, T> decoder, Function<T, JsonObject> encoder) {
        this.decoder = decoder;
        this.encoder = encoder;
    }

    @Override
    public <O> Stream<O> keys(DynamicOps<O> ops) {
        return Stream.empty();
    }

    @Override
    public <O> DataResult<T> decode(DynamicOps<O> ops, MapLike<O> input) {
        O map = ops.createMap(input.entries());
        JsonElement converted = ops.convertTo(JsonOps.INSTANCE, map);
        if (!(converted instanceof JsonObject json)) {
            return DataResult.error(() -> "recipe must be a json object");
        }
        try {
            return DataResult.success(this.decoder.apply(json));
        } catch (RuntimeException e) {
            return DataResult.error(() -> "invalid recipe: " + e.getMessage());
        }
    }

    @Override
    public <O> RecordBuilder<O> encode(T input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
        JsonObject json = this.encoder.apply(input);
        for (var entry : json.entrySet()) {
            prefix.add(entry.getKey(), JsonOps.INSTANCE.convertTo(ops, entry.getValue()));
        }
        return prefix;
    }

    /** Unused, kept for the Pair based MapCodec contract in newer DFU versions. */
    @SuppressWarnings("unused")
    private static <T, O> Pair<O, T> unused() {
        return null;
    }
}
