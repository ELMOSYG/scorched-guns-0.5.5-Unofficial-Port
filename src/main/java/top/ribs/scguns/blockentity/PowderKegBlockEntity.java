package top.ribs.scguns.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import top.ribs.scguns.init.ModBlockEntities;

public class PowderKegBlockEntity extends BlockEntity {
   private int smokeTimer = 0;
   private static final int SMOKE_INTERVAL = 20;

   public PowderKegBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.POWDER_KEG.get(), pos, state);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, PowderKegBlockEntity blockEntity) {
      if (level.isClientSide()) {
         blockEntity.clientTick(level, pos, state);
      }
   }

   private void clientTick(Level level, BlockPos pos, BlockState state) {
      if (++this.smokeTimer >= 20) {
         this.smokeTimer = 0;
         level.addParticle(
            ParticleTypes.SMOKE,
            (double)pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.2,
            (double)pos.getY() + 1.0,
            (double)pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.2,
            0.0,
            0.05,
            0.0
         );
      }
   }
}
