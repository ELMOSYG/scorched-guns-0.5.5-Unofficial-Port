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

      float bypass = 0.0F;
      if (event.getSource().getDirectEntity() instanceof ProjectileEntity projectile) {
         bypass = projectile.getArmorBypassAmount();
      }
      if (bypass <= 0.0F) {
         return;
      }

      float armor = target.getArmorValue();
      if (armor <= 0.0F) {
         return;
      }

      float toughness = (float) target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
      float damage = event.getAmount();
      float withArmor = vanillaReduction(armor, toughness, damage);
      float afterBypass = vanillaReduction(Math.max(0.0F, armor - bypass), toughness, damage);
      if (withArmor <= 0.0F || withArmor >= 1.0F) {
         return;
      }

      // Raw damage is scaled so that the post-armour result equals the damage a target with the reduced armour would
      // have taken. Vanilla still applies its own reduction to the scaled amount, hence the division.
      float compensation = (1.0F - afterBypass) / (1.0F - withArmor);
      if (compensation > 1.0F) {
         event.setAmount(damage * compensation);
      }
   }
}
