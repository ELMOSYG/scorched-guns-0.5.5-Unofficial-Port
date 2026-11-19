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
      event.add(EntityType.PLAYER, SCAttributes.BULLET_RESISTANCE);

      // Every living entity that already carries attributes gets the new one too, with 1.0 as the
      // default. Nothing changes for mobs that were never configured, and a data pack or KubeJS can
      // still give individual types a different value.
      for (EntityType<?> type : event.getTypes()) {
         @SuppressWarnings("unchecked")
         EntityType<? extends net.minecraft.world.entity.LivingEntity> livingType =
            (EntityType<? extends net.minecraft.world.entity.LivingEntity>) type;
         event.add(livingType, SCAttributes.BULLET_RESISTANCE);
      }
   }
}
