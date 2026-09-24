package top.ribs.scguns.enchantment;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.ribs.scguns.init.ModEnchantments;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.util.ScEnchants;

import java.util.Random;

/**
 * Corroded enchantment behaviour.
 *
 * <p>1.21 enchantments are datapack entries, so this is no longer an
 * {@code Enchantment} subclass: the definition lives in
 * {@code data/scguns/enchantment/corroded.json} and the gameplay logic that used
 * to be an override stays here as static helpers plus its damage hook.</p>
 */
@EventBusSubscriber(modid = "scguns")
public final class CorrodedEnchantment {
    private static final Random RANDOM = new Random();

    private CorrodedEnchantment() {
    }

    public static void doPostAttack(LivingEntity attacker, Entity target, int level) {
        if (target instanceof LivingEntity livingTarget) {
            if (isBotEntity(livingTarget)) {
                spawnCorrodedParticles(livingTarget, level);
                return;
            }
            if (RANDOM.nextFloat() < 0.3F) {
                int poisonDuration = 60 + level * 20;
                livingTarget.addEffect(new MobEffectInstance(MobEffects.POISON, poisonDuration, 0));
            }
        }
    }

    private static void spawnCorrodedParticles(LivingEntity entity, int level) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < level * 5; i++) {
                double offsetX = (RANDOM.nextDouble() - 0.5) * (double) entity.getBbWidth();
                double offsetY = RANDOM.nextDouble() * (double) entity.getBbHeight();
                double offsetZ = (RANDOM.nextDouble() - 0.5) * (double) entity.getBbWidth();
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        entity.getX() + offsetX, entity.getY() + offsetY, entity.getZ() + offsetZ,
                        1, 0.0, 0.0, 0.0, 0.1);
            }
        }
    }

    public static boolean isBotEntity(LivingEntity entity) {
        return entity.getType().is(ModTags.Entities.BOT);
    }

    public static float getBotDamageBonus(int level) {
        return (float) level * 2.0F;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (!weapon.isEmpty()) {
                int corrodedLevel = ScEnchants.level(weapon, ModEnchantments.CORRODED);
                if (corrodedLevel > 0 && event.getEntity() instanceof LivingEntity target) {
                    if (isBotEntity(target)) {
                        event.setAmount(event.getAmount() + getBotDamageBonus(corrodedLevel));
                    }
                }
            }
        }
    }
}
