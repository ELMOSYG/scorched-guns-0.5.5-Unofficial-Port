package top.ribs.scguns.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

/**
 * 1.21 wraps mob effects in {@link Holder}, while 1.20.1 code hands the raw
 * {@link MobEffect} around. This turns a raw effect back into the holder the
 * modern API expects.
 */
public final class ScEffects {
    private ScEffects() {
    }

    public static Holder<MobEffect> holder(MobEffect effect) {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }
}
