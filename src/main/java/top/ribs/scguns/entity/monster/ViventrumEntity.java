package top.ribs.scguns.entity.monster;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.entity.ai.AIType;
import top.ribs.scguns.entity.ai.GunAttackGoal;
import top.ribs.scguns.init.ModItems;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.item.GunItem;

public class ViventrumEntity extends TamableAnimal {
   private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(ViventrumEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> ATTACK_TIMEOUT = SynchedEntityData.defineId(ViventrumEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> PATROLLING = SynchedEntityData.defineId(ViventrumEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Optional<BlockPos>> PATROL_ORIGIN = SynchedEntityData.defineId(
      ViventrumEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS
   );
   private static final EntityDataAccessor<Boolean> PARTYING = SynchedEntityData.defineId(ViventrumEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DEFENSIVE = SynchedEntityData.defineId(ViventrumEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> ARMOR_PLATES = SynchedEntityData.defineId(ViventrumEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> HEAVY_ARMOR_PLATES = SynchedEntityData.defineId(ViventrumEntity.class, EntityDataSerializers.INT);
   private static final int MAX_ARMOR_PLATES = 4;
   private static final int PATROL_RADIUS = 9;
   private static final int PATROL_MOVE_INTERVAL = 100;
   private static final int PATROL_DURATION = 80;
   private int patrolTimer = 0;
   private BlockPos currentPatrolTarget = null;
   private static final float DEFENSIVE_HEALTH_THRESHOLD = 0.25F;
   private static final float DEFENSIVE_ARMOR_BONUS = 15.0F;
   private static final float DEFENSIVE_DAMAGE_REDUCTION = 0.6F;
   private static final ResourceLocation DEFENSIVE_BONUS_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("scguns", "f0a1b2c3-d4e5-4678-9abc-def012345678");

   public ViventrumEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
      super(entityType, level);
   }

   public static Builder createAttributes() {
      return Animal.createLivingAttributes()
         .add(Attributes.MAX_HEALTH, 24.0)
         .add(Attributes.FOLLOW_RANGE, 16.0)
         .add(Attributes.MOVEMENT_SPEED, 0.28)
         .add(Attributes.ARMOR, 2.0)
         .add(Attributes.ATTACK_DAMAGE, 4.0)
         .add(Attributes.ATTACK_KNOCKBACK, 0.3F)
         .add(Attributes.FLYING_SPEED, 0.3);
   }

   public HumanoidArm getMainArm() {
      return HumanoidArm.LEFT;
   }

   @Override
   public boolean isFood(ItemStack stack) {
      return false;
   }

   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData
   ) {
      return super.finalizeSpawn(level, difficulty, reason, spawnData);
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(ATTACKING, false);
      builder.define(ATTACK_TIMEOUT, 0);
      builder.define(PATROLLING, false);
      builder.define(PATROL_ORIGIN, Optional.empty());
      builder.define(PARTYING, false);
      builder.define(DEFENSIVE, false);
      builder.define(ARMOR_PLATES, 0);
      builder.define(HEAVY_ARMOR_PLATES, 0);
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide) {
         if (this.isTame()) {
            this.updateDefensiveState();
         }

         if (this.isAttacking() && this.getAttackTimeout() > 0) {
            this.setAttackTimeout(this.getAttackTimeout() - 1);
            if (this.getAttackTimeout() == 5) {
               LivingEntity target = this.getTarget();
               if (target != null && this.distanceToSqr(target) <= (double)(this.getBbWidth() * 2.0F * this.getBbWidth() * 2.0F + target.getBbWidth())) {
                  this.doHurtTarget(target);
               }
            }

            if (this.getAttackTimeout() <= 0) {
               this.setAttacking(false);
            }
         }

         if (this.isPatrolling()) {
            this.handlePatrolling();
         }
      }
   }

   private void updateDefensiveState() {
      float healthPercent = this.getHealth() / this.getMaxHealth();
      boolean shouldBeDefensive = healthPercent <= 0.25F;
      if (shouldBeDefensive != this.isDefensive()) {
         this.setDefensive(shouldBeDefensive);
         if (shouldBeDefensive) {
            this.getNavigation().stop();
            this.setTarget(null);
         }
      }
   }

   private void handlePatrolling() {
      Optional<BlockPos> patrolOrigin = this.getPatrolOrigin();
      if (patrolOrigin.isPresent()) {
         if (this.patrolTimer <= 0) {
            if ((double)this.random.nextFloat() < 0.5) {
               this.currentPatrolTarget = patrolOrigin.get().offset(this.random.nextInt(18) - 9, 0, this.random.nextInt(18) - 9);
               this.getNavigation()
                  .moveTo(
                     (double)this.currentPatrolTarget.getX() + 0.5,
                     (double)this.currentPatrolTarget.getY(),
                     (double)this.currentPatrolTarget.getZ() + 0.5,
                     0.8
                  );
               this.patrolTimer = 80;
            } else {
               this.getNavigation().stop();
               this.currentPatrolTarget = null;
               this.patrolTimer = 50;
            }
         } else {
            this.patrolTimer--;
            if (this.currentPatrolTarget != null
               && this.distanceToSqr(
                     (double)this.currentPatrolTarget.getX(), (double)this.currentPatrolTarget.getY(), (double)this.currentPatrolTarget.getZ()
                  )
                  < 4.0) {
               this.getNavigation().stop();
               this.currentPatrolTarget = null;
               this.patrolTimer = 20;
            }
         }

         if (this.distanceToSqr((double)patrolOrigin.get().getX(), (double)patrolOrigin.get().getY(), (double)patrolOrigin.get().getZ()) > 144.0) {
            this.getNavigation()
               .moveTo(
                  (double)patrolOrigin.get().getX() + 0.5, (double)patrolOrigin.get().getY(), (double)patrolOrigin.get().getZ() + 0.5, 1.0
               );
            this.currentPatrolTarget = null;
            this.patrolTimer = 40;
         }
      }
   }

   protected void registerGoals() {
      ItemStack mainHandItem = this.getMainHandItem();
      boolean hasGun = mainHandItem.getItem() instanceof GunItem;
      if (!hasGun) {
         this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false) {
            public boolean canUse() {
               return !ViventrumEntity.this.isDefensive() && super.canUse();
            }

            public boolean canContinueToUse() {
               return !ViventrumEntity.this.isDefensive() && super.canContinueToUse();
            }

            @Override
            protected void checkAndPerformAttack(LivingEntity pEnemy) {
               double pDistToEnemySqr = this.mob.distanceToSqr(pEnemy);
               if (pDistToEnemySqr <= top.ribs.scguns.util.CombatHelper.attackReachSqr(this.mob, pEnemy) && this.getTicksUntilNextAttack() <= 0 && !ViventrumEntity.this.isAttacking()) {
                  ViventrumEntity.this.setAttacking(true);
                  this.resetAttackCooldown();
                  this.mob.swing(InteractionHand.MAIN_HAND);
               }
            }
         });
      } else {
         int difficulty = this.level().getDifficulty().getId() + 1;
         this.goalSelector.addGoal(2, new GunAttackGoal<ViventrumEntity>(this, mainHandItem, 1.0F, AIType.SMART, difficulty) {
            @Override
            public boolean canUse() {
               return !ViventrumEntity.this.isDefensive() && super.canUse();
            }

            public boolean canContinueToUse() {
               return !ViventrumEntity.this.isDefensive() && super.canContinueToUse();
            }
         });
      }

      this.goalSelector.addGoal(1, new FloatGoal(this));
      this.goalSelector.addGoal(3, new SitWhenOrderedToGoal(this));
      if (this.isTame()) {
         this.goalSelector.addGoal(4, new ViventrumEntity.ViventrumFollowOwnerGoal(this, 1.3, 10.0F, 2.0F, false));
      }

      this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Mob.class, 8.0F));
      this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
      this.targetSelector.addGoal(3, new HurtByTargetGoal(this, new Class[0]));
      this.targetSelector.addGoal(4, new NearestAttackableTargetGoal(this, Zombie.class, true));
   }

   @NotNull
   public InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
      ItemStack itemstack = player.getItemInHand(hand);
      if (this.level().isClientSide) {
         boolean flag = this.isOwnedBy(player) || this.isTame() || itemstack.is(Items.DIAMOND) && !this.isTame();
         return flag ? InteractionResult.CONSUME : InteractionResult.PASS;
      } else if (this.isTame()) {
         if (player.isShiftKeyDown() && itemstack.isEmpty()) {
            ItemStack heldItem = this.getMainHandItem();
            if (!heldItem.isEmpty()) {
               this.spawnAtLocation(heldItem);
               this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
               return InteractionResult.SUCCESS;
            } else {
               ItemStack helmet = this.getItemBySlot(EquipmentSlot.HEAD);
               if (!helmet.isEmpty()) {
                  this.spawnAtLocation(helmet);
                  this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                  return InteractionResult.SUCCESS;
               } else {
                  return InteractionResult.PASS;
               }
            }
         } else if (player.isShiftKeyDown() && itemstack.getItem() == ModItems.ARMOR_PLATE.get()) {
            if (this.getArmorPlates() + this.getHeavyArmorPlates() >= 4) {
               player.displayClientMessage(Component.translatable("message.mechanical_entity.max_armor_plates"), true);
               return InteractionResult.FAIL;
            } else {
               this.addArmorPlate(false);
               if (!player.getAbilities().instabuild) {
                  itemstack.shrink(1);
               }

               this.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.5F, 1.0F);
               int currentTotal = this.getArmorPlates() + this.getHeavyArmorPlates();
               player.displayClientMessage(Component.translatable("message.mechanical_entity.armor_plating_added", new Object[]{currentTotal, 4}), true);
               return InteractionResult.SUCCESS;
            }
         } else if (player.isShiftKeyDown() && itemstack.getItem() == ModItems.HEAVY_ARMOR_PLATE.get()) {
            if (this.getArmorPlates() + this.getHeavyArmorPlates() >= 4) {
               player.displayClientMessage(Component.translatable("message.mechanical_entity.max_armor_plates"), true);
               return InteractionResult.FAIL;
            } else {
               this.addArmorPlate(true);
               if (!player.getAbilities().instabuild) {
                  itemstack.shrink(1);
               }

               this.playSound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 0.5F, 1.0F);
               int currentTotal = this.getArmorPlates() + this.getHeavyArmorPlates();
               player.displayClientMessage(Component.translatable("message.mechanical_entity.heavy_armor_plating_added", new Object[]{currentTotal, 4}), true);
               return InteractionResult.SUCCESS;
            }
         } else if (!player.isShiftKeyDown() || !(itemstack.getItem() instanceof AxeItem) || this.getArmorPlates() <= 0 && this.getHeavyArmorPlates() <= 0) {
            if (player.isShiftKeyDown() && itemstack.getItem() instanceof ArmorItem armorItem && armorItem.getEquipmentSlot() == EquipmentSlot.HEAD) {
               if (itemstack.is(ModTags.Items.VIVENTRUM_BANNED_ITEMS)) {
                  player.displayClientMessage(Component.translatable("message.viventrum.item_too_heavy"), true);
                  return InteractionResult.FAIL;
               } else {
                  ItemStack currentHelmet = this.getItemBySlot(EquipmentSlot.HEAD);
                  if (!currentHelmet.isEmpty()) {
                     this.spawnAtLocation(currentHelmet);
                  }

                  this.setItemSlot(EquipmentSlot.HEAD, itemstack.copy());
                  if (!player.getAbilities().instabuild) {
                     itemstack.shrink(1);
                  }

                  return InteractionResult.SUCCESS;
               }
            } else if (player.isShiftKeyDown() && !itemstack.isEmpty() && !(itemstack.getItem() instanceof ArmorItem)) {
               if (itemstack.is(ModTags.Items.VIVENTRUM_BANNED_ITEMS)) {
                  player.displayClientMessage(Component.translatable("message.viventrum.item_too_heavy"), true);
                  return InteractionResult.FAIL;
               } else {
                  ItemStack currentWeapon = this.getMainHandItem();
                  if (!currentWeapon.isEmpty()) {
                     this.spawnAtLocation(currentWeapon);
                  }

                  ItemStack singleItem = itemstack.copy();
                  singleItem.setCount(1);
                  this.setItemInHand(InteractionHand.MAIN_HAND, singleItem);
                  if (!player.getAbilities().instabuild) {
                     itemstack.shrink(1);
                  }

                  return InteractionResult.SUCCESS;
               }
            } else if (player.isShiftKeyDown() && itemstack.getItem() == ModItems.ARMOR_PLATE.get()) {
               if (this.getArmorPlates() >= 4) {
                  player.displayClientMessage(Component.translatable("message.mechanical_entity.max_armor_plates"), true);
                  return InteractionResult.FAIL;
               } else {
                  this.addArmorPlate(false);
                  if (!player.getAbilities().instabuild) {
                     itemstack.shrink(1);
                  }

                  this.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.5F, 1.0F);
                  int currentPlates = this.getArmorPlates();
                  player.displayClientMessage(Component.translatable("message.mechanical_entity.armor_plating_added", new Object[]{currentPlates, 4}), true);
                  return InteractionResult.SUCCESS;
               }
            } else if (player.isShiftKeyDown() && itemstack.getItem() == ModItems.HEAVY_ARMOR_PLATE.get()) {
               if (this.getArmorPlates() >= 4) {
                  player.displayClientMessage(Component.translatable("message.mechanical_entity.max_armor_plates"), true);
                  return InteractionResult.FAIL;
               } else {
                  this.addArmorPlate(true);
                  if (!player.getAbilities().instabuild) {
                     itemstack.shrink(1);
                  }

                  this.playSound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 0.5F, 1.0F);
                  int currentPlates = this.getArmorPlates();
                  player.displayClientMessage(Component.translatable("message.mechanical_entity.heavy_armor_plating_added", new Object[]{currentPlates, 4}), true);
                  return InteractionResult.SUCCESS;
               }
            } else if (player.isShiftKeyDown() && itemstack.getItem() == Items.IRON_AXE && this.getArmorPlates() > 0) {
               this.removeArmorPlate();
               this.playSound(SoundEvents.ARMOR_STAND_BREAK, 0.5F, 1.0F);
               ItemStack droppedPlate = new ItemStack((ItemLike)ModItems.ARMOR_PLATE.get());
               this.spawnAtLocation(droppedPlate);
               int currentPlates = this.getArmorPlates();
               player.displayClientMessage(Component.translatable("message.mechanical_entity.armor_plating_removed", new Object[]{currentPlates, 4}), true);
               return InteractionResult.SUCCESS;
            } else if (!player.isShiftKeyDown() && itemstack.is((Item)ModItems.REPAIR_KIT.get()) && this.getHealth() < this.getMaxHealth()) {
               if (!player.getAbilities().instabuild) {
                  itemstack.shrink(1);
               }

               this.heal(10.0F);
               this.playSound(SoundEvents.GENERIC_EAT, 0.5F, 1.0F);
               if (this.level() instanceof ServerLevel serverLevel) {
                  serverLevel.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 0.5, this.getZ(), 3, 0.3, 0.3, 0.3, 0.1);
               }

               return InteractionResult.SUCCESS;
            } else if (!player.isShiftKeyDown() && itemstack.isEmpty()) {
               Component entityName = (Component)(this.hasCustomName() ? this.getCustomName() : Component.translatable("entity.scguns.viventrum"));
               if (this.isOrderedToSit()) {
                  this.setOrderedToSit(false);
                  this.setPatrolling(false);
                  player.displayClientMessage(Component.translatable("message.viventrum.following", new Object[]{entityName}), true);
               } else if (this.isPatrolling()) {
                  this.setPatrolling(false);
                  this.setOrderedToSit(true);
                  player.displayClientMessage(Component.translatable("message.viventrum.sitting", new Object[]{entityName}), true);
               } else {
                  this.setPatrolling(true);
                  this.setPatrolOrigin(this.blockPosition());
                  this.spawnPatrolOriginParticles();
                  player.displayClientMessage(Component.translatable("message.viventrum.patrolling", new Object[]{entityName}), true);
               }

               return InteractionResult.SUCCESS;
            } else {
               return InteractionResult.SUCCESS;
            }
         } else {
            boolean wasHeavy = this.removeArmorPlate();
            this.playSound(SoundEvents.ARMOR_STAND_BREAK, 0.5F, 1.0F);
            ItemStack droppedPlate = new ItemStack(wasHeavy ? (ItemLike)ModItems.HEAVY_ARMOR_PLATE.get() : (ItemLike)ModItems.ARMOR_PLATE.get());
            this.spawnAtLocation(droppedPlate);
            int currentTotal = this.getArmorPlates() + this.getHeavyArmorPlates();
            player.displayClientMessage(Component.translatable("message.mechanical_entity.armor_plating_removed", new Object[]{currentTotal, 4}), true);
            return InteractionResult.SUCCESS;
         }
      } else if (itemstack.is(Items.DIAMOND)) {
         if (!player.getAbilities().instabuild) {
            itemstack.shrink(1);
         }

         if (this.random.nextInt(3) == 0 && !EventHooks.onAnimalTame(this, player)) {
            this.tame(player);
            this.navigation.stop();
            this.setTarget(null);
            this.setOrderedToSit(true);
            this.level().broadcastEntityEvent(this, (byte)7);
         } else {
            this.level().broadcastEntityEvent(this, (byte)6);
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.mobInteract(player, hand);
      }
   }

   public boolean canTakeItem(ItemStack stack) {
      EquipmentSlot slot = this.getEquipmentSlotForItem(stack);
      return !this.getItemBySlot(slot).isEmpty() ? false : slot == EquipmentSlot.HEAD;
   }

   public boolean canReplaceCurrentItem(ItemStack candidate, ItemStack existing) {
      return existing.isEmpty() ? this.getEquipmentSlotForItem(candidate) == EquipmentSlot.HEAD : false;
   }

   public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
      ItemStack oldStack = this.getItemBySlot(slot);
      super.setItemSlot(slot, stack);
      if (!this.level().isClientSide && slot == EquipmentSlot.MAINHAND) {
         boolean hadGun = oldStack.getItem() instanceof GunItem;
         boolean hasGun = stack.getItem() instanceof GunItem;
         if (hadGun != hasGun) {
            this.goalSelector.removeAllGoals(goal -> true);
            this.registerGoals();
         }
      }
   }

   public void setAttacking(boolean attacking) {
      this.entityData.set(ATTACKING, attacking);
      if (attacking) {
         this.setAttackTimeout(10);
      }
   }

   public void setAttackTimeout(int timeout) {
      this.entityData.set(ATTACK_TIMEOUT, timeout);
   }

   public int getAttackTimeout() {
      return (Integer)this.entityData.get(ATTACK_TIMEOUT);
   }

   public boolean isAttacking() {
      return (Boolean)this.entityData.get(ATTACKING);
   }

   public boolean isPatrolling() {
      return (Boolean)this.entityData.get(PATROLLING);
   }

   public void setPatrolling(boolean patrolling) {
      this.entityData.set(PATROLLING, patrolling);
   }

   public Optional<BlockPos> getPatrolOrigin() {
      return (Optional<BlockPos>)this.entityData.get(PATROL_ORIGIN);
   }

   public void setPatrolOrigin(BlockPos pos) {
      if (pos != null) {
         this.entityData.set(PATROL_ORIGIN, Optional.of(pos));
      } else {
         this.entityData.set(PATROL_ORIGIN, Optional.empty());
      }
   }

   public boolean isPartying() {
      return (Boolean)this.entityData.get(PARTYING);
   }

   public void setPartying(boolean partying) {
      this.entityData.set(PARTYING, partying);
   }

   public boolean isDefensive() {
      return (Boolean)this.entityData.get(DEFENSIVE);
   }

   public void setDefensive(boolean defensive) {
      this.entityData.set(DEFENSIVE, defensive);
      if (defensive) {
         Objects.requireNonNull(this.getAttribute(Attributes.ARMOR))
            .addOrUpdateTransientModifier(new AttributeModifier(DEFENSIVE_BONUS_MODIFIER_ID, 15.0, Operation.ADD_VALUE));
      } else {
         Objects.requireNonNull(this.getAttribute(Attributes.ARMOR)).removeModifier(DEFENSIVE_BONUS_MODIFIER_ID);
      }
   }

   public boolean hurt(DamageSource source, float amount) {
      if (this.isInvulnerableTo(source)) {
         return false;
      } else {
         if (this.isTame() && this.isDefensive()) {
            amount *= 0.39999998F;
         }

         return super.hurt(source, amount);
      }
   }

   public void spawnPatrolOriginParticles() {
      if (this.level() instanceof ServerLevel) {
         BlockPos pos = this.getPatrolOrigin().orElse(this.blockPosition());
         ((ServerLevel)this.level())
            .sendParticles(
               ParticleTypes.HAPPY_VILLAGER, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, 10, 0.5, 0.5, 0.5, 0.1
            );
      }
   }

   protected void updateWalkAnimation(float partialTick) {
      float f = this.getPose() == Pose.STANDING ? Math.min(partialTick * 6.0F, 1.0F) : 0.0F;
      this.walkAnimation.update(f, 0.2F);
   }



   public void setRecordPlayingNearby(@NotNull BlockPos pos, boolean playing) {
      this.setPartying(playing && this.isTame());
   }

   public int getArmorPlates() {
      return (Integer)this.entityData.get(ARMOR_PLATES);
   }

   public int getHeavyArmorPlates() {
      return (Integer)this.entityData.get(HEAVY_ARMOR_PLATES);
   }

   public void setArmorPlates(int plates) {
      this.entityData.set(ARMOR_PLATES, plates);
      this.updateArmorFromPlates();
   }

   public void setHeavyArmorPlates(int plates) {
      this.entityData.set(HEAVY_ARMOR_PLATES, plates);
      this.updateArmorFromPlates();
   }

   public void addArmorPlate(boolean isHeavy) {
      int total = this.getArmorPlates() + this.getHeavyArmorPlates();
      if (total < 4) {
         if (isHeavy) {
            this.setHeavyArmorPlates(this.getHeavyArmorPlates() + 1);
         } else {
            this.setArmorPlates(this.getArmorPlates() + 1);
         }
      }
   }

   public boolean removeArmorPlate() {
      if (this.getHeavyArmorPlates() > 0) {
         this.setHeavyArmorPlates(this.getHeavyArmorPlates() - 1);
         return true;
      } else if (this.getArmorPlates() > 0) {
         this.setArmorPlates(this.getArmorPlates() - 1);
         return false;
      } else {
         return false;
      }
   }

   private void updateArmorFromPlates() {
      int regularPlates = this.getArmorPlates();
      int heavyPlates = this.getHeavyArmorPlates();
      int totalArmor = regularPlates + heavyPlates * 2;
      Objects.requireNonNull(this.getAttribute(Attributes.ARMOR)).setBaseValue(2.0 + (double)totalArmor);
   }

   protected void checkFallDamage(double y, boolean onGround, @NotNull BlockState state, @NotNull BlockPos pos) {
   }

   @Nullable
   protected SoundEvent getAmbientSound() {
      return SoundEvents.VEX_AMBIENT;
   }

   @Nullable
   protected SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
      return SoundEvents.VEX_HURT;
   }

   @Nullable
   protected SoundEvent getDeathSound() {
      return SoundEvents.VEX_DEATH;
   }

   protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
      super.dropCustomDeathLoot(level, damageSource, hitByPlayer);
      ItemStack mainHandItem = this.getMainHandItem();
      if (!mainHandItem.isEmpty()) {
         this.spawnAtLocation(mainHandItem);
         this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      }

      ItemStack helmet = this.getItemBySlot(EquipmentSlot.HEAD);
      if (!helmet.isEmpty()) {
         this.spawnAtLocation(helmet);
         this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
      }

      int regularPlates = this.getArmorPlates();
      if (regularPlates > 0) {
         ItemStack armorPlates = new ItemStack((ItemLike)ModItems.ARMOR_PLATE.get(), regularPlates);
         this.spawnAtLocation(armorPlates);
      }

      int heavyPlates = this.getHeavyArmorPlates();
      if (heavyPlates > 0) {
         ItemStack heavyArmorPlates = new ItemStack((ItemLike)ModItems.HEAVY_ARMOR_PLATE.get(), heavyPlates);
         this.spawnAtLocation(heavyArmorPlates);
      }
   }

   public void addAdditionalSaveData(@NotNull CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean("Attacking", this.isAttacking());
      tag.putInt("AttackTimeout", this.getAttackTimeout());
      tag.putBoolean("Patrolling", this.isPatrolling());
      tag.putBoolean("Defensive", this.isDefensive());
      tag.putInt("ArmorPlates", this.getArmorPlates());
      tag.putInt("HeavyArmorPlates", this.getHeavyArmorPlates());
      this.getPatrolOrigin().ifPresent(pos -> {
         tag.putInt("PatrolOriginX", pos.getX());
         tag.putInt("PatrolOriginY", pos.getY());
         tag.putInt("PatrolOriginZ", pos.getZ());
      });
   }

   public void readAdditionalSaveData(@NotNull CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.setAttacking(tag.getBoolean("Attacking"));
      this.setAttackTimeout(tag.getInt("AttackTimeout"));
      this.setPatrolling(tag.getBoolean("Patrolling"));
      if (tag.contains("Defensive")) {
         this.setDefensive(tag.getBoolean("Defensive"));
      }

      if (tag.contains("ArmorPlates")) {
         this.setArmorPlates(tag.getInt("ArmorPlates"));
      }

      if (tag.contains("HeavyArmorPlates")) {
         this.setHeavyArmorPlates(tag.getInt("HeavyArmorPlates"));
      }

      if (tag.contains("PatrolOriginX") && tag.contains("PatrolOriginY") && tag.contains("PatrolOriginZ")) {
         BlockPos patrolOrigin = new BlockPos(tag.getInt("PatrolOriginX"), tag.getInt("PatrolOriginY"), tag.getInt("PatrolOriginZ"));
         this.setPatrolOrigin(patrolOrigin);
      }
   }

   public void setTame(boolean tamed, boolean applyTamingSideEffects) {
      super.setTame(tamed, applyTamingSideEffects);
      if (tamed) {
         this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0);
         this.setHealth(40.0F);
      } else {
         this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(24.0);
      }

      this.goalSelector.removeAllGoals(goal -> true);
      this.registerGoals();
   }

   @Nullable
   public AgeableMob getBreedOffspring(@NotNull ServerLevel serverLevel, @NotNull AgeableMob ageableMob) {
      return null;
   }

   public static class ViventrumFollowOwnerGoal extends FollowOwnerGoal {
      private final ViventrumEntity viventrum;

      public ViventrumFollowOwnerGoal(ViventrumEntity viventrum, double speed, float minDistance, float maxDistance, boolean leavesAllowed) {
         super(viventrum, speed, minDistance, maxDistance);
         this.viventrum = viventrum;
      }

      public boolean canUse() {
         return super.canUse() && !this.viventrum.isPatrolling();
      }

      public boolean canContinueToUse() {
         return super.canContinueToUse() && !this.viventrum.isPatrolling();
      }

      public void start() {
         if (!this.viventrum.isPatrolling()) {
            super.start();
         }
      }

      public void tick() {
         if (!this.viventrum.isPatrolling()) {
            super.tick();
         }
      }
   }
}
