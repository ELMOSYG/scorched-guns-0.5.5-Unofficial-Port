package top.ribs.scguns.item.animated;


import net.minecraft.core.Holder;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
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
import top.ribs.scguns.Config;
import top.ribs.scguns.client.render.armor.BrassMaskArmorRenderer;
import top.ribs.scguns.entity.monster.DissidentEntity;
import top.ribs.scguns.init.ModEntities;

public class BrassMaskArmorItem extends ArmorItem implements GeoItem {
   private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

   public BrassMaskArmorItem(Holder<ArmorMaterial> pMaterial, Type pType, Properties pProperties) {
      super(pMaterial, pType, pProperties);
   }

   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(new IClientItemExtensions() {
         private BrassMaskArmorRenderer renderer;

         @NotNull
         public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
            if (this.renderer == null) {
               this.renderer = new BrassMaskArmorRenderer();
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
      if (player != null && player.isShiftKeyDown() && this.isValidHomUnculusStructure(level, pos)) {
         if (!level.isClientSide()) {
            if (!player.getAbilities().instabuild) {
               pContext.getItemInHand().shrink(1);
            }

            BlockPos bottomPos;
            BlockPos topPos;
            if (level.getBlockState(pos.above()).is(Blocks.CLAY)) {
               bottomPos = pos;
               topPos = pos.above();
            } else {
               bottomPos = pos.below();
               topPos = pos;
            }

            level.setBlock(bottomPos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(topPos, Blocks.AIR.defaultBlockState(), 3);
            this.spawnCreationEffects((ServerLevel)level, bottomPos.above());
            level.playSound(null, bottomPos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 0.8F, 0.8F + level.random.nextFloat() * 0.4F);
            boolean disableVillagers = (Boolean)Config.COMMON.gameplay.disableVillagerSpawning.get();
            if (disableVillagers) {
               double dissidentChance = (Double)Config.COMMON.gameplay.dissidentSpawnChance.get();
               if (level.random.nextDouble() < dissidentChance) {
                  DissidentEntity dissident = this.createNeutralDissident(level, bottomPos, player);
                  level.addFreshEntity(dissident);
                  level.playSound(null, bottomPos, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 1.0F, 0.8F);
               }
            } else {
               boolean createDissident = level.random.nextBoolean();
               if (createDissident) {
                  DissidentEntity dissident = this.createNeutralDissident(level, bottomPos, player);
                  level.addFreshEntity(dissident);
                  level.playSound(null, bottomPos, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 1.0F, 0.8F);
               } else {
                  Villager villager = new Villager(EntityType.VILLAGER, level);
                  villager.moveTo((double)bottomPos.getX() + 0.5, (double)bottomPos.getY(), (double)bottomPos.getZ() + 0.5, 0.0F, 0.0F);
                  List<VillagerType> villagerTypes = BuiltInRegistries.VILLAGER_TYPE.stream().toList();
                  VillagerType randomType = villagerTypes.get(level.random.nextInt(villagerTypes.size()));
                  villager.setVillagerData(villager.getVillagerData().setType(randomType).setProfession(VillagerProfession.NONE));
                  level.addFreshEntity(villager);
                  level.playSound(null, bottomPos, SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.2F);
               }
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

   private boolean isValidHomUnculusStructure(Level level, BlockPos pos) {
      BlockState clickedBlock = level.getBlockState(pos);
      if (!clickedBlock.is(Blocks.CLAY)) {
         return false;
      } else {
         return level.getBlockState(pos.above()).is(Blocks.CLAY) ? true : level.getBlockState(pos.below()).is(Blocks.CLAY);
      }
   }

   private void spawnCreationEffects(ServerLevel level, BlockPos pos) {
      for (int i = 0; i < 20; i++) {
         double x = (double)pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 2.0;
         double y = (double)pos.getY() + level.random.nextDouble() * 2.0;
         double z = (double)pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 2.0;
         level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0.0, 0.0, 0.0, 0.1);
      }

      for (int i = 0; i < 15; i++) {
         double x = (double)pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 1.5;
         double y = (double)pos.getY() + level.random.nextDouble() * 1.5;
         double z = (double)pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 1.5;
         level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0.0, 0.1, 0.0, 0.05);
      }
   }

   private DissidentEntity createNeutralDissident(Level level, BlockPos bottomPos, Player creator) {
      DissidentEntity dissident = new DissidentEntity((EntityType<? extends Monster>)ModEntities.DISSIDENT.get(), level);
      dissident.moveTo((double)bottomPos.getX() + 0.5, (double)bottomPos.getY(), (double)bottomPos.getZ() + 0.5, 0.0F, 0.0F);
      AttributeInstance maxHealthAttribute = dissident.getAttribute(Attributes.MAX_HEALTH);
      if (maxHealthAttribute != null) {
         maxHealthAttribute.setBaseValue(maxHealthAttribute.getBaseValue() * 0.6);
         dissident.setHealth((float)maxHealthAttribute.getValue());
      }

      if (creator != null) {
         dissident.targetSelector.getAvailableGoals().removeIf(goal -> goal.getGoal() instanceof NearestAttackableTargetGoal);
         dissident.targetSelector
            .addGoal(
               2,
               new NearestAttackableTargetGoal(
                  dissident, Player.class, 10, true, false, target -> !(target instanceof Player player) || (player != creator && !player.isCreative() && !player.isSpectator())
               )
            );
      }

      return dissident;
   }

   private PlayState predicate(AnimationState animationState) {
      animationState.getController().setAnimation(RawAnimation.begin().then("animation.brass_mask.idle", LoopType.LOOP));
      return PlayState.CONTINUE;
   }

   public void registerControllers(ControllerRegistrar controllerRegistrar) {
      controllerRegistrar.add(new AnimationController[]{new AnimationController(this, "controller", 0, this::predicate)});
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
