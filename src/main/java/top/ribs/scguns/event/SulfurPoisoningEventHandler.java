package top.ribs.scguns.event;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.ribs.scguns.effect.SulfurPoisoningEffect;

@EventBusSubscriber(
   modid = "scguns"
)
public class SulfurPoisoningEventHandler {
   public SulfurPoisoningEventHandler() {
      super();
   }

   @SubscribeEvent
   public static void onLivingHurt(LivingIncomingDamageEvent event) {
      LivingEntity entity = event.getEntity();
      if (SulfurPoisoningEffect.hasFireVulnerability(entity)
         && (
            event.getSource().is(DamageTypes.IN_FIRE)
               || event.getSource().is(DamageTypes.ON_FIRE)
               || event.getSource().is(DamageTypes.LAVA)
               || event.getSource().is(DamageTypes.HOT_FLOOR)
         )) {
         float multiplier = SulfurPoisoningEffect.getFireDamageMultiplier(entity);
         event.setAmount(event.getAmount() * multiplier);
      }
   }
}
