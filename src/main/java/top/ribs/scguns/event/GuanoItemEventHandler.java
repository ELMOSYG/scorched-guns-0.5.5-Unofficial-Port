package top.ribs.scguns.event;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.ribs.scguns.init.ModBlocks;
import top.ribs.scguns.init.ModItems;

@EventBusSubscriber(
   modid = "scguns"
)
public class GuanoItemEventHandler {
   private static final Set<ItemEntity> guanoItems = new HashSet<>();

   public GuanoItemEventHandler() {
      super();
   }

   @SubscribeEvent
   public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
      if (!event.getLevel().isClientSide()) {
         if (event.getEntity() instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();
            if (stack.is((Item)ModItems.BAT_GUANO.get())) {
               guanoItems.add(itemEntity);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent.Post event) {
      {
         Iterator<ItemEntity> iterator = guanoItems.iterator();

         while (iterator.hasNext()) {
            ItemEntity itemEntity = iterator.next();
            if (!itemEntity.isAlive() || itemEntity.isRemoved()) {
               iterator.remove();
            } else if (itemEntity.onGround() && itemEntity.tickCount % 15 == 0 && tryFormGuanoLayer(itemEntity)) {
               iterator.remove();
            }
         }
      }
   }

   private static boolean tryFormGuanoLayer(ItemEntity itemEntity) {
      Level level = itemEntity.level();
      BlockPos pos = itemEntity.blockPosition();
      BlockState stateAtPos = level.getBlockState(pos);
      BlockState stateBelow = level.getBlockState(pos.below());
      boolean shouldRemove = false;
      if (stateAtPos.is((Block)ModBlocks.BAT_GUANO_LAYER.get())) {
         int layers = (Integer)stateAtPos.getValue(SnowLayerBlock.LAYERS);
         if (layers < 8) {
            level.setBlock(pos, (BlockState)stateAtPos.setValue(SnowLayerBlock.LAYERS, layers + 1), 3);
            itemEntity.getItem().shrink(1);
            if (itemEntity.getItem().isEmpty()) {
               shouldRemove = true;
            }
         }
      } else if (!stateBelow.isAir() && stateBelow.isSolidRender(level, pos.below()) && stateAtPos.isAir()) {
         BlockState newLayer = (BlockState)((Block)ModBlocks.BAT_GUANO_LAYER.get()).defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1);
         level.setBlock(pos, newLayer, 3);
         itemEntity.getItem().shrink(1);
         if (itemEntity.getItem().isEmpty()) {
            shouldRemove = true;
         }
      }

      if (shouldRemove) {
         itemEntity.discard();
      }

      return shouldRemove;
   }
}
