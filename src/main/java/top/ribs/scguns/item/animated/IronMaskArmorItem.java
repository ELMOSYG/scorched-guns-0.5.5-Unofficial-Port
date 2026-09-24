package top.ribs.scguns.item.animated;


import net.minecraft.core.Holder;
import java.util.function.Consumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.Animation.LoopType;
import software.bernie.geckolib.animation.PlayState;
import top.ribs.scguns.client.render.armor.IronMaskArmorRenderer;
import top.ribs.scguns.entity.monster.PraetorEntity;
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.init.ModSounds;

public class IronMaskArmorItem extends ArmorItem implements GeoItem {
   private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

   public IronMaskArmorItem(Holder<ArmorMaterial> pMaterial, Type pType, Properties pProperties) {
      super(pMaterial, pType, pProperties);
   }

   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(new IClientItemExtensions() {
         private IronMaskArmorRenderer renderer;

         @NotNull
         public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
            if (this.renderer == null) {
               this.renderer = new IronMaskArmorRenderer();
            }

            this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
            return this.renderer;
         }
      });
   }

   public InteractionResult useOn(UseOnContext pContext) {
      Player player = pContext.getPlayer();
      Level level = pContext.getLevel();
      BlockPos pos = pContext.getClickedPos();
      if (player != null && player.isShiftKeyDown() && this.isValidPraetorRitualStructure(level, pos)) {
         if (!level.isClientSide()) {
            if (!player.getAbilities().instabuild) {
               pContext.getItemInHand().shrink(1);
            }

            BlockPos centerPos = this.findCenterPosition(level, pos);
            level.setBlock(centerPos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(centerPos.above(), Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(centerPos.above().west(), Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(centerPos.above().east(), Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(centerPos.above().north(), Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(centerPos.above().south(), Blocks.AIR.defaultBlockState(), 3);
            this.spawnCreationEffects((ServerLevel)level, centerPos.above());
            level.playSound(null, centerPos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 0.6F);
            level.playSound(null, centerPos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 0.6F, 1.2F);
            double random = level.random.nextDouble();
            if (random < 0.5) {
               Vindicator vindicator = new Vindicator(EntityType.VINDICATOR, level);
               vindicator.moveTo((double)centerPos.getX() + 0.5, (double)centerPos.getY(), (double)centerPos.getZ() + 0.5, 0.0F, 0.0F);
               level.addFreshEntity(vindicator);
               level.playSound(null, centerPos, SoundEvents.VINDICATOR_CELEBRATE, SoundSource.HOSTILE, 1.0F, 0.8F);
            } else if (random < 0.75) {
               PraetorEntity praetor = this.createNeutralPraetor(level, centerPos, player);
               level.addFreshEntity(praetor);
               level.playSound(null, centerPos, (SoundEvent)ModSounds.PRAETOR_IDLE.get(), SoundSource.HOSTILE, 1.0F, 1.2F);
            } else {
               PraetorEntity praetor = new PraetorEntity((EntityType<? extends PraetorEntity>)ModEntities.PRAETOR.get(), level);
               praetor.moveTo((double)centerPos.getX() + 0.5, (double)centerPos.getY(), (double)centerPos.getZ() + 0.5, 0.0F, 0.0F);
               level.addFreshEntity(praetor);
               level.playSound(null, centerPos, (SoundEvent)ModSounds.PRAETOR_IDLE.get(), SoundSource.HOSTILE, 1.0F, 0.8F);
            }
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.useOn(pContext);
      }
   }

   public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
      return pPlayer.isShiftKeyDown() ? InteractionResultHolder.pass(pPlayer.getItemInHand(pHand)) : super.use(pLevel, pPlayer, pHand);
   }

   private boolean isValidPraetorRitualStructure(Level level, BlockPos pos) {
      BlockState clickedBlock = level.getBlockState(pos);
      if (!clickedBlock.is(Blocks.CLAY)) {
         return false;
      } else if (this.isTShapeNorth(level, pos)) {
         return true;
      } else if (this.isTShapeSouth(level, pos)) {
         return true;
      } else if (this.isTShapeEast(level, pos)) {
         return true;
      } else if (this.isTShapeWest(level, pos)) {
         return true;
      } else if (this.isTShapeNorth(level, pos.below())) {
         return true;
      } else if (this.isTShapeSouth(level, pos.below())) {
         return true;
      } else if (this.isTShapeEast(level, pos.below())) {
         return true;
      } else if (this.isTShapeWest(level, pos.below())) {
         return true;
      } else if (this.isTShapeNorth(level, pos.north())) {
         return true;
      } else if (this.isTShapeSouth(level, pos.south())) {
         return true;
      } else {
         return this.isTShapeEast(level, pos.east()) ? true : this.isTShapeWest(level, pos.west());
      }
   }

   private boolean isTShapeNorth(Level level, BlockPos basePos) {
      return level.getBlockState(basePos).is(Blocks.CLAY)
         && level.getBlockState(basePos.above()).is(Blocks.CLAY)
         && level.getBlockState(basePos.above().west()).is(Blocks.CLAY)
         && level.getBlockState(basePos.above().east()).is(Blocks.CLAY);
   }

   private boolean isTShapeSouth(Level level, BlockPos basePos) {
      return level.getBlockState(basePos).is(Blocks.CLAY)
         && level.getBlockState(basePos.above()).is(Blocks.CLAY)
         && level.getBlockState(basePos.above().west()).is(Blocks.CLAY)
         && level.getBlockState(basePos.above().east()).is(Blocks.CLAY);
   }

   private boolean isTShapeEast(Level level, BlockPos basePos) {
      return level.getBlockState(basePos).is(Blocks.CLAY)
         && level.getBlockState(basePos.above()).is(Blocks.CLAY)
         && level.getBlockState(basePos.above().north()).is(Blocks.CLAY)
         && level.getBlockState(basePos.above().south()).is(Blocks.CLAY);
   }

   private boolean isTShapeWest(Level level, BlockPos basePos) {
      return level.getBlockState(basePos).is(Blocks.CLAY)
         && level.getBlockState(basePos.above()).is(Blocks.CLAY)
         && level.getBlockState(basePos.above().north()).is(Blocks.CLAY)
         && level.getBlockState(basePos.above().south()).is(Blocks.CLAY);
   }

   private BlockPos findCenterPosition(Level level, BlockPos pos) {
      if (this.isTShapeNorth(level, pos) || this.isTShapeSouth(level, pos) || this.isTShapeEast(level, pos) || this.isTShapeWest(level, pos)) {
         return pos;
      } else if (this.isTShapeNorth(level, pos.below())
         || this.isTShapeSouth(level, pos.below())
         || this.isTShapeEast(level, pos.below())
         || this.isTShapeWest(level, pos.below())) {
         return pos.below();
      } else if (this.isTShapeNorth(level, pos.north())
         || this.isTShapeSouth(level, pos.north())
         || this.isTShapeEast(level, pos.north())
         || this.isTShapeWest(level, pos.north())) {
         return pos.north();
      } else if (this.isTShapeNorth(level, pos.south())
         || this.isTShapeSouth(level, pos.south())
         || this.isTShapeEast(level, pos.south())
         || this.isTShapeWest(level, pos.south())) {
         return pos.south();
      } else if (this.isTShapeNorth(level, pos.east())
         || this.isTShapeSouth(level, pos.east())
         || this.isTShapeEast(level, pos.east())
         || this.isTShapeWest(level, pos.east())) {
         return pos.east();
      } else {
         return !this.isTShapeNorth(level, pos.west())
               && !this.isTShapeSouth(level, pos.west())
               && !this.isTShapeEast(level, pos.west())
               && !this.isTShapeWest(level, pos.west())
            ? pos
            : pos.west();
      }
   }

   private void spawnCreationEffects(ServerLevel level, BlockPos pos) {
      for (int i = 0; i < 30; i++) {
         double x = (double)pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 2.5;
         double y = (double)pos.getY() + level.random.nextDouble() * 2.5;
         double z = (double)pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 2.5;
         level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.0, 0.0, 0.0, 0.05);
      }

      for (int i = 0; i < 20; i++) {
         double x = (double)pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 2.0;
         double y = (double)pos.getY() + level.random.nextDouble() * 2.0;
         double z = (double)pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 2.0;
         level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0.0, 0.1, 0.0, 0.05);
      }
   }

   private PraetorEntity createNeutralPraetor(Level level, BlockPos centerPos, Player creator) {
      PraetorEntity praetor = new PraetorEntity((EntityType<? extends PraetorEntity>)ModEntities.PRAETOR.get(), level);
      praetor.moveTo((double)centerPos.getX() + 0.5, (double)centerPos.getY(), (double)centerPos.getZ() + 0.5, 0.0F, 0.0F);
      AttributeInstance maxHealthAttribute = praetor.getAttribute(Attributes.MAX_HEALTH);
      if (maxHealthAttribute != null) {
         maxHealthAttribute.setBaseValue(maxHealthAttribute.getBaseValue() * 0.6);
         praetor.setHealth((float)maxHealthAttribute.getValue());
      }

      if (creator != null) {
         praetor.targetSelector.getAvailableGoals().removeIf(goal -> goal.getGoal() instanceof NearestAttackableTargetGoal);
         praetor.targetSelector
            .addGoal(
               2,
               new NearestAttackableTargetGoal(
                  praetor, Player.class, 10, true, false, target -> !(target instanceof Player player) || (player != creator && !player.isCreative() && !player.isSpectator())
               )
            );
      }

      return praetor;
   }

   private PlayState predicate(AnimationState animationState) {
      animationState.getController().setAnimation(RawAnimation.begin().then("animation.iron_mask.idle", LoopType.LOOP));
      return PlayState.CONTINUE;
   }

   public void registerControllers(ControllerRegistrar controllerRegistrar) {
      controllerRegistrar.add(new AnimationController[]{new AnimationController(this, "controller", 0, this::predicate)});
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
