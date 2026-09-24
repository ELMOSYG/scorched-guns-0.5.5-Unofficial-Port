package top.ribs.scguns.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.ribs.scguns.item.CogMaceItem;

@EventBusSubscriber(
   modid = "scguns"
)
public class CogMaceEventHandler {
   public CogMaceEventHandler() {
      super();
   }

   @SubscribeEvent
   public static void onLivingHurt(LivingIncomingDamageEvent event) {
      if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker.getMainHandItem().getItem() instanceof CogMaceItem) {
         LivingEntity target = event.getEntity();
         double movementSpeed = attacker.getAttributeValue(Attributes.MOVEMENT_SPEED);
         double speedBonus = Math.max(0.0, Math.min(movementSpeed - 0.1, 0.3)) * 30.0;
         if (attacker.isSprinting()) {
            speedBonus *= 1.5;
         }

         double fallBonus = Math.min((double)attacker.fallDistance * 1.0, 5.0);
         float totalBonus = (float)(speedBonus + fallBonus);
         totalBonus = Math.min(totalBonus, 10.0F);
         if (totalBonus > 0.1F) {
            if (target.isBlocking()) {
               totalBonus *= 0.5F;
            }

            event.setAmount(event.getAmount() + totalBonus);
         }
      }
   }
}
