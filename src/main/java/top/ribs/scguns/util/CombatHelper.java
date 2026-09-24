package top.ribs.scguns.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Melee reach helper.
 *
 * <p>Forge added {@code Mob#getAttackReachSqr(LivingEntity)}; 1.21 dropped it, so
 * the vanilla formula it used (the mob's squared attack box width plus the
 * target's width) is spelled out here for the gunner AI goals.</p>
 */
public final class CombatHelper {
    private CombatHelper() {
    }

    public static double attackReachSqr(Mob mob, LivingEntity target) {
        float width = mob.getBbWidth();
        return (double) (width * 2.0F * width * 2.0F + target.getBbWidth());
    }
}
