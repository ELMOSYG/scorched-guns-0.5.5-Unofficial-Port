package top.ribs.scguns.block;


import net.minecraft.world.entity.LivingEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import top.ribs.scguns.init.ModParticleTypes;

public class GuanoCandleBlock extends Block {
   public static final BooleanProperty LIT = BlockStateProperties.LIT;
   protected static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 9.0, 11.0);
   private static final int CHECK_INTERVAL = 200;
   private static final int SPAWN_CHANCE = 90;
   private static final int SEARCH_RADIUS = 32;
   private static final int MAX_BATS = 8;

   public GuanoCandleBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(LIT, Boolean.FALSE));
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(LIT, Boolean.FALSE);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{LIT});
   }

   @Override
   public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

      ItemStack itemstack = stack;
      if (!(Boolean)state.getValue(LIT) && itemstack.is(Items.FLINT_AND_STEEL)) {
         world.setBlock(pos, (BlockState)state.setValue(LIT, Boolean.TRUE), 3);
         world.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
         if (!player.isCreative()) {
            itemstack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(player.getUsedItemHand()));
         }

         if (!world.isClientSide) {
            world.scheduleTick(pos, this, 200);
         }

         return ItemInteractionResult.sidedSuccess(world.isClientSide);
      } else if ((Boolean)state.getValue(LIT) && player.isCrouching() && itemstack.isEmpty()) {
         world.setBlock(pos, (BlockState)state.setValue(LIT, Boolean.FALSE), 3);
         world.playSound(player, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
         return ItemInteractionResult.sidedSuccess(world.isClientSide);
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
      if ((Boolean)state.getValue(LIT)) {
         double d0 = (double)pos.getX() + 0.5;
         double d1 = (double)pos.getY() + 0.75;
         double d2 = (double)pos.getZ() + 0.5;
         world.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0, 0.0, 0.0);
         world.addParticle(ParticleTypes.FLAME, d0, d1, d2, 0.0, 0.0, 0.0);
      }

      if (random.nextInt(10) == 0) {
         double d0 = (double)pos.getX() + 0.5;
         double d1 = (double)pos.getY() + 0.5;
         double d2 = (double)pos.getZ() + 0.5;
         double offsetX = (random.nextDouble() - 0.5) * 0.3;
         double offsetZ = (random.nextDouble() - 0.5) * 0.3;
         double offsetY = random.nextDouble() * 0.1;
         world.addParticle((ParticleOptions)ModParticleTypes.SULFUR_DUST.get(), d0 + offsetX, d1 + offsetY, d2 + offsetZ, 0.0, 0.03, 0.0);
      }
   }

   public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
      if ((Boolean)state.getValue(LIT) && !world.isClientSide) {
         world.scheduleTick(pos, this, 200);
      }
   }

   public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
      if ((Boolean)state.getValue(LIT)) {
         if (random.nextInt(90) == 0) {
            this.trySpawnBat(world, pos, random);
         }

         world.scheduleTick(pos, this, 200);
      }
   }

   private void trySpawnBat(ServerLevel world, BlockPos pos, RandomSource random) {
      AABB searchArea = new AABB(pos).inflate(32.0);
      List<Bat> nearbyBats = world.getEntitiesOfClass(Bat.class, searchArea);
      if (nearbyBats.size() < 8) {
         for (int attempts = 0; attempts < 10; attempts++) {
            double offsetX = (random.nextDouble() - 0.5) * 10.0;
            double offsetY = random.nextDouble() * 5.0;
            double offsetZ = (random.nextDouble() - 0.5) * 10.0;
            BlockPos spawnPos = pos.offset((int)offsetX, (int)offsetY, (int)offsetZ);
            if (world.getBlockState(spawnPos).isAir() && world.getBlockState(spawnPos.below()).isSolidRender(world, spawnPos.below())) {
               Bat bat = (Bat)EntityType.BAT.create(world);
               if (bat != null) {
                  bat.moveTo(
                     (double)spawnPos.getX() + 0.5, (double)spawnPos.getY(), (double)spawnPos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F
                  );
                  world.addFreshEntity(bat);
                  world.playSound(null, spawnPos, SoundEvents.BAT_AMBIENT, SoundSource.NEUTRAL, 0.5F, 1.0F);
                  break;
               }
            }
         }
      }
   }
}
