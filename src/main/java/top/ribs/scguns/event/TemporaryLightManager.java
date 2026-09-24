package top.ribs.scguns.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import top.ribs.scguns.Config;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.block.TemporaryLightBlock;
import top.ribs.scguns.init.ModBlocks;

public class TemporaryLightManager {
   private static final int DEFAULT_LIGHT_DURATION = 8;
   private static final int BEAM_LIGHT_DURATION = 20;
   private static final int LIGHT_LEVEL = 7;

   public TemporaryLightManager() {
      super();
   }

   public static void addTemporaryLight(Level level, BlockPos pos, boolean isBeamWeapon) {
      if (!level.isClientSide) {
         try {
            if (Config.CLIENT == null || Config.CLIENT.display == null) {
               return;
            }

            boolean fireLightsEnabled;
            try {
               fireLightsEnabled = (Boolean)Config.CLIENT.display.fireLights.get();
            } catch (IllegalStateException var9) {
               return;
            }

            if (!fireLightsEnabled) {
               return;
            }

            BlockState currentState = level.getBlockState(pos);
            if (!canPlaceLightAt(level, pos, currentState)) {
               return;
            }

            int duration = isBeamWeapon ? 20 : 8;
            if (currentState.getBlock() instanceof TemporaryLightBlock) {
               int currentLifetime = (Integer)currentState.getValue(TemporaryLightBlock.LIFETIME);
               int newLifetime;
               if (isBeamWeapon) {
                  newLifetime = Math.min(currentLifetime + duration, 40);
               } else {
                  newLifetime = duration;
               }

               BlockState newState = (BlockState)currentState.setValue(TemporaryLightBlock.LIFETIME, newLifetime);
               level.setBlock(pos, newState, 2);
               return;
            }

            BlockState lightState = (BlockState)((BlockState)((Block)ModBlocks.TEMPORARY_LIGHT.get()).defaultBlockState().setValue(TemporaryLightBlock.LIGHT_LEVEL, 7))
               .setValue(TemporaryLightBlock.LIFETIME, duration);
            level.setBlock(pos, lightState, 3);
            level.sendBlockUpdated(pos, currentState, lightState, 3);
            if (level instanceof ServerLevel serverLevel) {
               serverLevel.scheduleTick(pos, (Block)ModBlocks.TEMPORARY_LIGHT.get(), 1);
            }
         } catch (Exception var10) {
            ScorchedGuns.LOGGER.error("Error in addTemporaryLight: " + var10.getMessage(), var10);
         }
      }
   }

   private static boolean canPlaceLightAt(Level level, BlockPos pos, BlockState currentState) {
      if (!level.hasChunkAt(pos)) {
         return false;
      } else if (!currentState.getFluidState().isEmpty()) {
         return false;
      } else if (currentState.isAir()) {
         return true;
      } else if (currentState.getBlock() instanceof TemporaryLightBlock) {
         return true;
      } else {
         return currentState.is(Blocks.LIGHT) ? true : currentState.canBeReplaced();
      }
   }

   public static void emergencyCleanup(Level level) {
      if (level != null && !level.isClientSide) {
         try {
            ScorchedGuns.LOGGER.info("Emergency cleanup called for temporary lights in dimension: " + level.dimension().location());
         } catch (Exception var2) {
            ScorchedGuns.LOGGER.error("Error during emergency cleanup: " + var2.getMessage(), var2);
         }
      }
   }
}
