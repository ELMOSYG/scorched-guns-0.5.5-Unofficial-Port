package top.ribs.scguns.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface IExplosionDamageable {
   void onProjectileExploded(Level var1, BlockState var2, BlockPos var3, Entity var4);
}
