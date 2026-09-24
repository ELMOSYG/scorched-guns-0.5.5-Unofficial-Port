package top.ribs.scguns.event;

import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.ribs.scguns.entity.monster.HornlinEntity;

@EventBusSubscriber(
   modid = "scguns"
)
public class HornlinAllianceHandler {
   private static final double ALLIANCE_RANGE = 26.0;
   private static final int ALERT_RANGE_Y = 10;

   public HornlinAllianceHandler() {
      super();
   }

   @SubscribeEvent
   public static void onPiglinHurt(LivingIncomingDamageEvent event) {
      if (!(event.getAmount() <= 0.0F)) {
         if (event.getEntity() instanceof Piglin piglin) {
            if (event.getSource().getEntity() instanceof Player player) {
               if (!piglin.level().isClientSide) {
                  AABB quickCheck = AABB.unitCubeFromLowerCorner(piglin.position()).inflate(26.0, 10.0, 26.0);
                  if (!piglin.level().getEntitiesOfClass(HornlinEntity.class, quickCheck, EntitySelector.NO_SPECTATORS).isEmpty()) {
                     piglin.level()
                        .getEntitiesOfClass(HornlinEntity.class, quickCheck, EntitySelector.NO_SPECTATORS)
                        .stream()
                        .filter(hornlin -> hornlin.getTarget() == null)
                        .filter(hornlin -> !hornlin.isAlliedTo(player))
                        .forEach(hornlin -> hornlin.setTarget(player));
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onHornlinHurt(LivingIncomingDamageEvent event) {
      if (!(event.getAmount() <= 0.0F)) {
         if (event.getEntity() instanceof HornlinEntity hornlin) {
            if (event.getSource().getEntity() instanceof Player player) {
               if (!hornlin.level().isClientSide) {
                  AABB quickCheck = AABB.unitCubeFromLowerCorner(hornlin.position()).inflate(26.0, 10.0, 26.0);
                  hornlin.level()
                     .getEntitiesOfClass(HornlinEntity.class, quickCheck, EntitySelector.NO_SPECTATORS)
                     .stream()
                     .filter(entity -> entity != hornlin)
                     .filter(entity -> entity.getTarget() == null)
                     .filter(entity -> !entity.isAlliedTo(player))
                     .forEach(entity -> entity.setTarget(player));
                  if (!hornlin.level().getEntitiesOfClass(Piglin.class, quickCheck, EntitySelector.NO_SPECTATORS).isEmpty()) {
                     hornlin.level()
                        .getEntitiesOfClass(Piglin.class, quickCheck, EntitySelector.NO_SPECTATORS)
                        .stream()
                        .filter(entity -> entity.getTarget() == null)
                        .filter(entity -> !entity.isAlliedTo(player))
                        .forEach(entity -> {
                           try {
                              Brain<Piglin> brain = entity.getBrain();
                              brain.setMemory(MemoryModuleType.ANGRY_AT, player.getUUID());
                              brain.setMemory(MemoryModuleType.ATTACK_TARGET, player);
                              entity.setTarget(player);
                           } catch (Exception var3) {
                              entity.setTarget(player);
                           }
                        });
                  }
               }
            }
         }
      }
   }
}
