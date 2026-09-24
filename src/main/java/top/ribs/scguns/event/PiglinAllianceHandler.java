package top.ribs.scguns.event;

import java.lang.reflect.Field;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.ribs.scguns.entity.monster.ZombifiedHornlinEntity;

@EventBusSubscriber(
   modid = "scguns"
)
public class PiglinAllianceHandler {
   private static final double ALLIANCE_RANGE = 26.0;
   private static final int ALERT_RANGE_Y = 10;

   public PiglinAllianceHandler() {
      super();
   }

   @SubscribeEvent
   public static void onZombifiedPiglinHurt(LivingIncomingDamageEvent event) {
      if (!(event.getAmount() <= 0.0F)) {
         if (event.getEntity() instanceof ZombifiedPiglin piglin) {
            if (event.getSource().getEntity() instanceof Player player) {
               if (!piglin.level().isClientSide) {
                  AABB quickCheck = AABB.unitCubeFromLowerCorner(piglin.position()).inflate(26.0, 10.0, 26.0);
                  if (!piglin.level().getEntitiesOfClass(ZombifiedHornlinEntity.class, quickCheck, EntitySelector.NO_SPECTATORS).isEmpty()) {
                     piglin.level()
                        .getEntitiesOfClass(ZombifiedHornlinEntity.class, quickCheck, EntitySelector.NO_SPECTATORS)
                        .stream()
                        .filter(hornlin -> hornlin.getTarget() == null)
                        .filter(hornlin -> !hornlin.isAlliedTo(player))
                        .forEach(hornlin -> {
                           hornlin.setLastHurtByPlayer(player);
                           hornlin.startPersistentAngerTimer();
                           hornlin.setPersistentAngerTarget(player.getUUID());
                           hornlin.setTarget(player);

                           try {
                              Field playFirstAngerSoundInField = ZombifiedHornlinEntity.class.getDeclaredField("playFirstAngerSoundIn");
                              playFirstAngerSoundInField.setAccessible(true);
                              playFirstAngerSoundInField.setInt(hornlin, 0);
                              Field ticksUntilNextAlertField = ZombifiedHornlinEntity.class.getDeclaredField("ticksUntilNextAlert");
                              ticksUntilNextAlertField.setAccessible(true);
                              ticksUntilNextAlertField.setInt(hornlin, 0);
                           } catch (Exception var4x) {
                           }
                        });
                  }
               }
            }
         }
      }
   }
}
