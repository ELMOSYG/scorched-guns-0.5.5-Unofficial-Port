package top.ribs.scguns.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import top.ribs.scguns.entity.projectile.ProjectileEntity;

public interface IDamageable {
   @Deprecated
   default void onBlockDamaged(Level world, BlockState state, BlockPos pos, int damage) {
   }

   default void onBlockDamaged(Level world, BlockState state, BlockPos pos, ProjectileEntity projectile, float rawDamage, int damage) {
      this.onBlockDamaged(world, state, pos, damage);
   }
}
