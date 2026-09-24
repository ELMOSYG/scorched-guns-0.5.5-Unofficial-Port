package top.ribs.scguns.event;

import net.minecraft.world.entity.ambient.Bat;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.ribs.scguns.entity.ai.BatGlowberryGoal;

@EventBusSubscriber(
   modid = "scguns"
)
public class BatAIEventHandler {
   public BatAIEventHandler() {
      super();
   }

   @SubscribeEvent
   public static void onBatSpawn(EntityJoinLevelEvent event) {
      if (event.getEntity() instanceof Bat bat && !event.getLevel().isClientSide()) {
         bat.goalSelector.addGoal(2, new BatGlowberryGoal(bat, 0.6, 8));
      }
   }
}
