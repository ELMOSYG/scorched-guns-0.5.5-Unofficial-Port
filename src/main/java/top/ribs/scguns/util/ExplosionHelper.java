package top.ribs.scguns.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;

/**
 * 1.20.1 {@code Entity#ignoreExplosion()} compatibility.
 *
 * <p>1.21 changed the method to {@code ignoreExplosion(Explosion)} and the base
 * implementation now simply returns {@code false}; the only overrides left in
 * vanilla are {@code ArmorStand} and {@code Warden}. The 0.5.5 code calls the
 * no-argument form from hand written explosion loops (the grenade rounds and the
 * stun grenade) that do not own an {@code Explosion} instance, so those call
 * sites go through this helper instead.</p>
 *
 * <p><b>What 1.20.1 actually did</b> (checked with javap against the real runtime
 * jar, {@code forge-1.20.1-47.2.21-client.jar}, instead of assumed): the method the
 * 0.5.5 bytecode calls is {@code LivingEntity.m_6128_}, declared once in
 * {@code Entity} as literally {@code return false}, and the only subclass
 * overriding it is {@code ArmorStand} (whose body is {@code isMarker()}).
 * {@code Player} did <b>not</b> override it, so a creative or spectator player was
 * <b>not</b> skipped - {@code if (!entity.ignoreExplosion())} was effectively
 * {@code if (true)} for every player.</p>
 *
 * <p>An earlier version of this helper returned true for invulnerable and
 * spectator players, believing that was the 1.20.1 behaviour. It was not, and it
 * silently made the stun grenade do nothing at all to a creative player - the
 * "the flashbang has no effect" report. Only armor stands are skipped here,
 * matching 1.20.1. (1.21.1's own {@code ArmorStand} override tests
 * {@code isInvisible()} rather than {@code isMarker()}; 1.20.1's is kept because
 * that is what this port reproduces.)</p>
 */
public final class ExplosionHelper {
    private ExplosionHelper() {
    }

    public static boolean ignoresExplosion(Entity entity) {
        return entity instanceof ArmorStand armorStand && armorStand.isMarker();
    }
}
