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
}
