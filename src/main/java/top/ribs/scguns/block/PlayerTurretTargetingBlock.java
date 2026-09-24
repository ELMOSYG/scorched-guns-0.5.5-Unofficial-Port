package top.ribs.scguns.block;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class PlayerTurretTargetingBlock extends TurretTargetingBlock {
   public PlayerTurretTargetingBlock(Properties properties) {
      super(properties);
   }

   @Override
   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }
}
