package top.ribs.scguns.event;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.ribs.scguns.entity.projectile.ProjectileEntity;

/**
 * Armour penetration for every scguns bullet, in one place.
 *
 * <p>The mod used to reduce damage with its own armour formula inside {@code ProjectileEntity}, and the damage
 * source then went through vanilla armour as well. Two reductions stacked, so armoured mobs were far tougher than
 * the numbers suggested and the puncturing enchantment (which only touched the mod's own pass) barely showed.</p>
 *
 * <p>Bypass is now expressed the way the tooltip reads: N points of armour simply do not count. Vanilla armour is
 * left untouched; the incoming damage is multiplied by the ratio between the reduction the target would have had
 * and the reduction it is meant to have left, so every damage path (projectiles, beams, melee, turrets) is
 * treated the same way without touching each of them.</p>
 */
@EventBusSubscriber(modid = "scguns", bus = EventBusSubscriber.Bus.GAME)
public final class ArmorPiercingHandler {

   /** Vanilla's maximum armour value; bypass is expressed as a fraction of it (15 means 75% penetration). */
   private static final float MAX_ARMOR_POINTS = 20.0F;

   /**
    * The vanilla armour reduction, exactly as {@code LivingEntity#getDamageAfterArmorAbsorb} computes it.
    *
    * <p>Armour is not a flat percentage per point: the formula blends a hard cap ({@code armour / 5}) with a
    * damage-dependent term ({@code armour - damage / (2 + toughness / 4)}), takes the larger of the two and divides by
    * 25. Replacing it with "4% per point" produced the wrong compensation - a 15 bypass shot at 15 armour came out at
    * 8.8 instead of 6, because the approximation over-compensated.</p>
    */
   private static float vanillaReduction(float armor, float toughness, float damage) {
      if (armor <= 0.0F) {
         return 0.0F;
      }
      float divisor = 2.0F + toughness / 4.0F;
      float effective = Math.max(armor / 5.0F, armor - damage / divisor);
      effective = Math.min(20.0F, effective);
      return Math.max(0.0F, effective / 25.0F);
   }

   private ArmorPiercingHandler() {
   }

   @SubscribeEvent
   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      LivingEntity target = event.getEntity();
      if (target == null || target.level().isClientSide) {
         return;
      }

      // The projectile can be either the direct entity or the causing entity depending on how the source was built,
      // so look at both. A zero here means "no information", and the vanilla maths is left alone.
      float bypass = 0.0F;
      if (event.getSource().getDirectEntity() instanceof ProjectileEntity direct) {
         bypass = direct.getArmorBypassAmount();
      } else if (event.getSource().getEntity() instanceof ProjectileEntity causing) {
         bypass = causing.getArmorBypassAmount();
      }

      top.ribs.scguns.ScorchedGuns.LOGGER.info(
         "scguns: piercing check - source={}, direct={}, entity={}, bypass={}, amount={}, armor={}",
         event.getSource().typeHolder().unwrapKey().map(key -> key.location().toString()).orElse("?"),
         event.getSource().getDirectEntity(), event.getSource().getEntity(), bypass, event.getAmount(),
         target.getArmorValue());

      if (bypass <= 0.0F) {
         return;
      }

      float armor = target.getArmorValue();
      if (armor <= 0.0F) {
         return;
      }

      float toughness = (float) target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
      float damage = event.getAmount();
      // Bypass is expressed against the vanilla maximum of twenty armour points: 15 bypass means 75% penetration,
      // the same idea as the vanilla breach enchantment, just written as armour points because that is what the gun
      // tooltips already show.
      float penetration = Math.min(1.0F, bypass / MAX_ARMOR_POINTS);
      float reducedArmor = armor * (1.0F - penetration);

      float withArmor = vanillaReduction(armor, toughness, damage);
      float afterBypass = vanillaReduction(reducedArmor, toughness, damage);
      if (withArmor <= 0.0F || withArmor >= 1.0F) {
         return;
      }

      // Vanilla armour reduction depends on the damage being reduced, so scaling the raw amount changes the
      // reduction that is applied to it: one division by (1 - reduction) overshoots (5 damage with 15 bypass landed
      // at 6 on iron armour). Solve for the raw amount that ends up as the intended post-armour value instead, with a
      // short fixed-point iteration - four passes are well below the noise of a whole health point.
      float intended = damage * (1.0F - withArmor);
      float scaled = damage;
      for (int pass = 0; pass < 4; pass++) {
         float reduction = vanillaReduction(reducedArmor, toughness, scaled);
         if (reduction >= 1.0F) {
            break;
         }
         scaled = intended / (1.0F - reduction);
         if (Float.isNaN(scaled) || Float.isInfinite(scaled)) {
            scaled = damage;
            break;
         }
      }

      top.ribs.scguns.ScorchedGuns.LOGGER.info(
         "scguns: piercing applied - bypass={}, penetration={}, armor={} -> {}, damage {} -> {}",
         bypass, penetration, armor, reducedArmor, damage, scaled);

      if (scaled > damage) {
         event.setAmount(scaled);
      }
   }
}
