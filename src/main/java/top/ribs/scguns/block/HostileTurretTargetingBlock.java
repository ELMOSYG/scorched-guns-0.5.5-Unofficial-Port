package top.ribs.scguns.block;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class HostileTurretTargetingBlock extends TurretTargetingBlock {
   public HostileTurretTargetingBlock(Properties properties) {
      super(properties);
   }

   @Override
   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }
}
