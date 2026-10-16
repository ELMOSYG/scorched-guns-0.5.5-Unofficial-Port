package top.ribs.scguns.util;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.player.Player;

/**
 * 1.20.1 had {@code net.minecraft.world.entity.MobType}; 1.21 removed it and the
 * enchantment damage bonuses now key off entity type tags instead. This shim
 * keeps the old enum and derives it from those same tags, so the gun damage
 * code keeps its original structure and results.
 */
public enum MobType {
    UNDEAD,
    ARTHROPOD,
    ILLAGER,
    WATER,
    UNDEFINED;

    public static MobType of(Entity entity) {
        if (entity == null) {
            return UNDEFINED;
        }
        if (entity instanceof Player) {
            return UNDEFINED;
        }
        // Ask the entity first. Some entities change the type they report at runtime - the training dummy counts as
        // undead while it wears a zombie head - and that is the only way such a change can be observed, because the
        // tags below are keyed on the entity type, which never changes. Reflective on purpose: a compile-time
        // reference to another mod's class is what made scguns fail to load when Polymorph was missing.
        MobType reported = reportedType(entity);
        if (reported != null) {
            return reported;
        }
        if (entity.getType().is(EntityTypeTags.SENSITIVE_TO_SMITE)) {
            return UNDEAD;
        }
        if (entity.getType().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
            return ARTHROPOD;
        }
        if (entity instanceof AbstractIllager) {
            return ILLAGER;
        }
        if (entity.getType().is(EntityTypeTags.SENSITIVE_TO_IMPALING)) {
            return WATER;
        }
        return UNDEFINED;
    }

    private static final java.util.Map<Class<?>, java.util.Optional<java.lang.reflect.Method>> REPORTED_TYPE_METHODS = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * The type the entity reports through {@code getMobType()}, or null when it has no such method (or reports
     * {@code UNDEFINED}). Used so a mod that changes an entity's type at runtime - a training dummy wearing a
     * zombie head, for instance - is seen by gun damage bonuses.
     */
    private static MobType reportedType(Entity entity) {
        java.lang.reflect.Method method = REPORTED_TYPE_METHODS
           .computeIfAbsent(entity.getClass(), MobType::findMobTypeMethod)
           .orElse(null);
        if (method == null) {
            return null;
        }

        try {
            Object value = method.invoke(entity);
            if (value == null) {
                return null;
            }

            String name = value.toString().toUpperCase(java.util.Locale.ROOT);
            if (name.contains("UNDEAD")) {
                return UNDEAD;
            }
            if (name.contains("ARTHROPOD")) {
                return ARTHROPOD;
            }
            if (name.contains("ILLAGER")) {
                return ILLAGER;
            }
            if (name.contains("WATER")) {
                return WATER;
            }
        } catch (Throwable ignored) {
            // The entity refused to answer; the tag based derivation below still decides.
        }

        return null;
    }

    private static java.util.Optional<java.lang.reflect.Method> findMobTypeMethod(Class<?> type) {
        try {
            java.lang.reflect.Method method = type.getMethod("getMobType");
            return java.util.Optional.of(method);
        } catch (Throwable absent) {
            return java.util.Optional.empty();
        }
    }
}
