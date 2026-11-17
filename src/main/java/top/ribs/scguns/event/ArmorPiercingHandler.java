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

   /** Vanilla armour: roughly 4% reduction per armour point, capped at 80%. */
   private static float vanillaReduction(float armor) {
      return Math.min(0.8F, Math.max(0.0F, armor) * 0.04F);
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

      float withArmor = vanillaReduction(armor);
      float afterBypass = vanillaReduction(Math.max(0.0F, armor - bypass));
      if (withArmor <= 0.0F) {
         return;
      }

      float compensation = (1.0F - afterBypass) / (1.0F - withArmor);
      if (compensation > 1.0F) {
         event.setAmount(event.getAmount() * compensation);
      }
   }
}
