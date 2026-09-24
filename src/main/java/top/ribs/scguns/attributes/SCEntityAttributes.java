package top.ribs.scguns.attributes;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.MOD
)
public class SCEntityAttributes {
   public SCEntityAttributes() {
      super();
   }

   @SubscribeEvent
   public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
      event.add(EntityType.PLAYER, SCAttributes.PROJECTILE_SPEED);
      event.add(EntityType.PLAYER, SCAttributes.RELOAD_SPEED);
      event.add(EntityType.PLAYER, SCAttributes.ADDITIONAL_BULLET_DAMAGE);
      event.add(EntityType.PLAYER, SCAttributes.BULLET_DAMAGE_MULTIPLIER);
      event.add(EntityType.PLAYER, SCAttributes.SPREAD_MULTIPLIER);
   }
}
