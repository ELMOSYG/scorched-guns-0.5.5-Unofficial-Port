package top.ribs.scguns.event;


import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.ribs.scguns.init.ModBlocks;
import top.ribs.scguns.init.ModItems;

@EventBusSubscriber(
   modid = "scguns"
)
public class BatPoopEvent {
   private static final String WELL_FED_TAG = "scguns:well_fed";
   private static final String WELL_FED_TIMER_TAG = "scguns:well_fed_timer";
   private static final String LAST_POOP_TIME_TAG = "scguns:last_poop_time";
   private static final int POOP_COOLDOWN = 1000;
   private static final float BASE_POOP_CHANCE = 5.5E-4F;
   private static final float WELL_FED_MULTIPLIER = 2.5F;
   private static final int WELL_FED_DURATION = 2000;
   private static final int MAX_CHECK_DEPTH = 48;

   public BatPoopEvent() {
      super();
   }

   @SubscribeEvent
   public static void onBatTick(EntityTickEvent.Post event) {
      if (event.getEntity() instanceof Bat bat) {
         if (!bat.level().isClientSide()) {
            updateWellFedStatus(bat);
            if (bat.isResting()) {
               int lastPoopTime = bat.getPersistentData().getInt("scguns:last_poop_time");
               if (bat.tickCount - lastPoopTime >= 1000) {
                  float poopChance = 5.5E-4F;
                  if (isWellFed(bat)) {
                     poopChance *= 2.5F;
                  }

                  if (bat.getRandom().nextFloat() < poopChance && !hasFullGuanoLayerBelow(bat)) {
                     dropGuano(bat);
                     bat.getPersistentData().putInt("scguns:last_poop_time", bat.tickCount);
                  }
               }
            }
         }
      }
   }

   private static boolean hasFullGuanoLayerBelow(Bat bat) {
      BlockPos startPos = bat.blockPosition().below();

      for (int i = 0; i < 48; i++) {
         BlockPos checkPos = startPos.below(i);
         BlockState state = bat.level().getBlockState(checkPos);
         if (!state.isAir()) {
            if (state.is((Block)ModBlocks.BAT_GUANO_LAYER.get())) {
               int layers = (Integer)state.getValue(SnowLayerBlock.LAYERS);
               return layers >= 8;
            }

            return false;
         }
      }

      return false;
   }

   private static void dropGuano(Bat bat) {
      ItemStack guanoStack = new ItemStack((ItemLike)ModItems.BAT_GUANO.get());
      ItemEntity guanoEntity = new ItemEntity(bat.level(), bat.getX(), bat.getY() - 0.3, bat.getZ(), guanoStack);
      guanoEntity.setDeltaMovement((bat.getRandom().nextDouble() - 0.5) * 0.02, -0.1, (bat.getRandom().nextDouble() - 0.5) * 0.02);
      bat.level().addFreshEntity(guanoEntity);
   }

   public static void setWellFed(Bat bat) {
      bat.getPersistentData().putBoolean("scguns:well_fed", true);
      bat.getPersistentData().putInt("scguns:well_fed_timer", 2000);
   }

   public static boolean isWellFed(Bat bat) {
      return bat.getPersistentData().getBoolean("scguns:well_fed");
   }

   private static void updateWellFedStatus(Bat bat) {
      if (isWellFed(bat)) {
         int timer = bat.getPersistentData().getInt("scguns:well_fed_timer");
         if (--timer <= 0) {
            bat.getPersistentData().putBoolean("scguns:well_fed", false);
            bat.getPersistentData().remove("scguns:well_fed_timer");
         } else {
            bat.getPersistentData().putInt("scguns:well_fed_timer", timer);
         }
      }
   }
}
