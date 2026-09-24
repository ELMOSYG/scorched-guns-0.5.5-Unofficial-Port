package top.ribs.scguns.block;


import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import top.ribs.scguns.blockentity.NitroKegBlockEntity;
import top.ribs.scguns.entity.block.PrimedNitroKeg;
import top.ribs.scguns.init.ModBlockEntities;

public class NitroKegBlock extends BaseEntityBlock {
   public static final MapCodec<NitroKegBlock> CODEC = simpleCodec(NitroKegBlock::new);

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   public static final BooleanProperty UNSTABLE = BlockStateProperties.UNSTABLE;

   public NitroKegBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(UNSTABLE, false));
   }

   public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
      if (!oldState.is(state.getBlock()) && level.hasNeighborSignal(pos)) {
         explode(level, pos);
         level.removeBlock(pos, false);
      }
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
      if (level.hasNeighborSignal(pos)) {
         explode(level, pos);
         level.removeBlock(pos, false);
      }
   }

   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      if (!level.isClientSide() && !player.isCreative() && (Boolean)state.getValue(UNSTABLE)) {
         explode(level, pos);
      }

      return super.playerWillDestroy(level, pos, state, player);
   }

   public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
      if (!level.isClientSide) {
         PrimedNitroKeg primedNitroKeg = new PrimedNitroKeg(
            level, (double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, explosion.getIndirectSourceEntity()
         );
         int fuse = primedNitroKeg.getFuse();
         primedNitroKeg.setFuse((short)(level.random.nextInt(fuse / 4) + fuse / 8));
         level.addFreshEntity(primedNitroKeg);
      }
   }

   public static void explode(Level level, BlockPos pos) {
      explode(level, pos, null);
   }

   private static void explode(Level level, BlockPos pos, @Nullable LivingEntity entity) {
      if (!level.isClientSide) {
         PrimedNitroKeg primedNitroKeg = new PrimedNitroKeg(
            level, (double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, entity
         );
         level.addFreshEntity(primedNitroKeg);
         level.playSound(
            null, primedNitroKeg.getX(), primedNitroKeg.getY(), primedNitroKeg.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F
         );
         level.gameEvent(entity, GameEvent.PRIME_FUSE, pos);
      }
   }

   public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      ItemStack itemstack = player.getItemInHand(hand);
      if (!itemstack.is(Items.FLINT_AND_STEEL) && !itemstack.is(Items.FIRE_CHARGE)) {
         return super.useItemOn(stack, state, level, pos, player, hand, hit);
      } else {
         explode(level, pos, player);
         level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
         Item item = itemstack.getItem();
         if (!player.isCreative()) {
            if (itemstack.is(Items.FLINT_AND_STEEL)) {
               itemstack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(player.getUsedItemHand()));
            } else {
               itemstack.shrink(1);
            }
         }

         player.awardStat(Stats.ITEM_USED.get(item));
         return ItemInteractionResult.sidedSuccess(level.isClientSide);
      }
   }

   public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
      if (!level.isClientSide) {
         BlockPos blockpos = hit.getBlockPos();
         Entity entity = projectile.getOwner();
         if (projectile.isOnFire() && projectile.mayInteract(level, blockpos)) {
            explode(level, blockpos, entity instanceof LivingEntity ? (LivingEntity)entity : null);
            level.removeBlock(blockpos, false);
         }
      }
   }

   public boolean dropFromExplosion(Explosion explosion) {
      return false;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{UNSTABLE});
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new NitroKegBlockEntity(pos, state);
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, (BlockEntityType)ModBlockEntities.NITRO_KEG.get(), NitroKegBlockEntity::tick);
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }
}
