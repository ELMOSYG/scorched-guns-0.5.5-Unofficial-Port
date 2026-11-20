package com.scg2tlm.elmomod.compat;

import java.util.Map;
import java.util.WeakHashMap;
import com.scg2tlm.elmomod.ExampleMod;
import com.scg2tlm.elmomod.SCG2TLMConfig;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.ribs.scguns.entity.projectile.ProjectileEntity;

/**
 * Skips the red damage particles when a maid is hit by one of this mod's rounds.
 *
 * <p>Purely cosmetic. The question of whether maids should take part in gunfights is answered by the faction rules;
 * this only keeps the hit from reading as a splash of blood, which is the part some players find uncomfortable. Only
 * gun hits are affected - melee, falls and everything else keep their normal feedback.</p>
 */
public final class MaidGunParticleSuppressor {

    /** Entity id -> the tick it was shot on. Weak entries so nothing is retained. */
    private static final Map<Entity, Integer> SHOT_THIS_TICK = new WeakHashMap<>();

    private MaidGunParticleSuppressor() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!SCG2TLMConfig.HIDE_GUN_HIT_PARTICLES.get()) {
            return;
        }

        // Namespace check instead of an EntityMaid instanceof: this class is registered explicitly (see
        // ExampleMod) and must not resolve any Touhou Little Maid type, so it stays harmless without the mod.
        net.minecraft.resources.ResourceLocation typeId =
            net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        if (typeId == null || !ExampleMod.TLM_MODID.equals(typeId.getNamespace())) {
            return;
        }

        Entity direct = event.getSource().getDirectEntity();
        Entity causing = event.getSource().getEntity();
        if (direct instanceof ProjectileEntity || causing instanceof ProjectileEntity) {
            SHOT_THIS_TICK.put(event.getEntity(), event.getEntity().tickCount);
        }
    }

    /** Whether this entity was shot by one of our rounds on its current tick. */
    public static boolean wasShotThisTick(Entity entity) {
        Integer tick = SHOT_THIS_TICK.get(entity);
        return tick != null && tick == entity.tickCount;
    }
}
