package top.ribs.scguns.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.ribs.scguns.entity.monster.BlundererEntity;
import top.ribs.scguns.init.ModEntities;

@EventBusSubscriber(
   modid = "scguns"
)
public class RaidEventHandler {
   public RaidEventHandler() {
      super();
   }

   @SubscribeEvent
   public static void onRaidSpawn(EntityJoinLevelEvent event) {
      if (event.getEntity() instanceof Raider raider && event.getLevel() instanceof ServerLevel serverLevel) {
         Raid raid = serverLevel.getRaidAt(raider.blockPosition());
         if (raid != null) {
            int wave = raid.getGroupsSpawned();
            if (shouldAddBlunderer(raid, wave) && !isBlundererAlreadyInRaid(raid)) {
               BlundererEntity blunderer = (BlundererEntity)((EntityType)ModEntities.BLUNDERER.get()).create(serverLevel);
               if (blunderer != null) {
                  BlockPos pos = raider.blockPosition();
                  blunderer.setPos((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
                  raid.joinRaid(wave, blunderer, pos, false);
                  serverLevel.addFreshEntity(blunderer);
               }
            }
         }
      }
   }

   private static boolean shouldAddBlunderer(Raid raid, int wave) {
      return wave > 1;
   }

   private static boolean isBlundererAlreadyInRaid(Raid raid) {
      for (Raider raider : raid.getAllRaiders()) {
         if (raider instanceof BlundererEntity) {
            return true;
         }
      }

      return false;
   }
}
