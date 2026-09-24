package top.ribs.scguns.entity.monster;



import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.Holder;
import java.time.LocalDate;
import java.time.Month;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.Config;
import top.ribs.scguns.client.screen.SupplyScampMenuProvider;
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.init.ModItems;
import top.ribs.scguns.init.ModSounds;

public class SupplyScampEntity extends TamableAnimal {
   private static final EntityDataAccessor<Boolean> PANICKING = SynchedEntityData.defineId(SupplyScampEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(SupplyScampEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> PATROLLING = SynchedEntityData.defineId(SupplyScampEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> MASK_COLOR = SynchedEntityData.defineId(SupplyScampEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Optional<BlockPos>> PATROL_ORIGIN = SynchedEntityData.defineId(
      SupplyScampEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS
   );
   private static final EntityDataAccessor<Boolean> WEARING_PUMPKIN = SynchedEntityData.defineId(SupplyScampEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> PARTYING = SynchedEntityData.defineId(SupplyScampEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> ARMOR_PLATES = SynchedEntityData.defineId(SupplyScampEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> HEAVY_ARMOR_PLATES = SynchedEntityData.defineId(SupplyScampEntity.class, EntityDataSerializers.INT);
   private static final int MAX_ARMOR_PLATES = 4;
   private static final int PATROL_COOLDOWN = 15;
   private int patrolCooldownTimer = 15;
   private static final int ITEM_COOLDOWN = 12;
   private int itemCooldownTimer = 12;
   private BlockPos scheduledBarrelClose = null;
   private int barrelCloseTimer = 0;
   private static final int PATROL_RADIUS = 9;
   private static final int PATROL_MOVE_INTERVAL = 100;
   private static final int PATROL_DURATION = 80;
   private static final EntityDataAccessor<Boolean> STATIONARY = SynchedEntityData.defineId(SupplyScampEntity.class, EntityDataSerializers.BOOLEAN);
   private static final double ITEM_DETECTION_RANGE = 9.0;
   private static final double ITEM_PICKUP_RANGE = 2.5;
   private int patrolTimer = 0;
   private BlockPos currentPatrolTarget = null;
   private static final int INVENTORY_SIZE = 27;
   public final SimpleContainer inventory = new SimpleContainer(27);
   private static final int[] DYE_COLOR_TO_MASK_INDEX = new int[]{15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0};

   public SupplyScampEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
      super(entityType, level);
      this.setCanPickUpLoot(true);
   }

   public boolean canBeAffected(@NotNull MobEffectInstance pPotionEffect) {
      Holder<MobEffect> effect = pPotionEffect.getEffect();
      return effect != MobEffects.POISON
            && effect != MobEffects.WITHER
            && effect != MobEffects.HUNGER
            && effect != MobEffects.REGENERATION
            && effect != MobEffects.SATURATION
            && effect != MobEffects.CONFUSION
            && effect != MobEffects.BLINDNESS
            && effect != MobEffects.WEAKNESS
            && effect != MobEffects.MOVEMENT_SLOWDOWN
            && effect != MobEffects.DIG_SLOWDOWN
            && effect != MobEffects.HARM
         ? super.canBeAffected(pPotionEffect)
         : false;
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide && this.isAlive() && this.isTame()) {
         if (this.scheduledBarrelClose != null && this.barrelCloseTimer > 0) {
            this.barrelCloseTimer--;
            if (this.barrelCloseTimer <= 0) {
               BlockState barrelState = this.level().getBlockState(this.scheduledBarrelClose);
               if (barrelState.getBlock() instanceof BarrelBlock && (Boolean)barrelState.getValue(BarrelBlock.OPEN)) {
                  this.level().setBlock(this.scheduledBarrelClose, (BlockState)barrelState.setValue(BarrelBlock.OPEN, false), 3);
                  this.playSound(SoundEvents.BARREL_CLOSE, 0.5F, 1.0F);
               }

               this.scheduledBarrelClose = null;
            }
         }

         if (this.patrolCooldownTimer <= 0) {
            if (this.isPatrolling()) {
               this.handlePatrolling();
            }

            this.patrolCooldownTimer = 15;
         } else {
            this.patrolCooldownTimer--;
         }

         if (this.itemCooldownTimer <= 0) {
            if (this.isPatrolling() || !this.isOrderedToSit() && !this.isSitting()) {
               this.checkForItems();
            }

            this.itemCooldownTimer = 12;
         } else {
            this.itemCooldownTimer--;
         }
      }
   }

   private void handlePatrolling() {
      Optional<BlockPos> patrolOrigin = this.getPatrolOrigin();
      if (patrolOrigin.isPresent()) {
         int totalItems = 0;

         for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (!stack.isEmpty()) {
               totalItems += stack.getCount();
            }
         }

         if (totalItems >= 3) {
            BlockPos nearestBarrel = this.findNearestBarrel();
            if (nearestBarrel != null) {
               double distanceToBarrel = this.distanceToSqr(Vec3.atCenterOf(nearestBarrel));
               if (!(distanceToBarrel <= 9.0)) {
                  this.getNavigation()
                     .moveTo((double)nearestBarrel.getX() + 0.5, (double)nearestBarrel.getY(), (double)nearestBarrel.getZ() + 0.5, 1.0);
                  return;
               }

               if (this.level().getBlockEntity(nearestBarrel) instanceof Container container) {
                  this.depositAllItems(container);
               }
            }
         }

         if (totalItems < 25) {
            ItemEntity nearestItem = this.findNearestItem();
            if (nearestItem != null && this.distanceToSqr(nearestItem) <= 81.0) {
               if (this.distanceToSqr(nearestItem) > 6.25) {
                  this.getNavigation().moveTo(nearestItem, 1.0);
                  return;
               }

               this.pickUpItem(nearestItem);
            }
         }

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
            if (this.currentPatrolTarget != null && this.distanceToSqr(Vec3.atCenterOf(this.currentPatrolTarget)) < 4.0) {
               this.getNavigation().stop();
               this.currentPatrolTarget = null;
               this.patrolTimer = 20;
            }
         }

         if (this.distanceToSqr(Vec3.atCenterOf((Vec3i)patrolOrigin.get())) > 144.0) {
            this.getNavigation()
               .moveTo(
                  (double)patrolOrigin.get().getX() + 0.5, (double)patrolOrigin.get().getY(), (double)patrolOrigin.get().getZ() + 0.5, 1.0
               );
            this.currentPatrolTarget = null;
            this.patrolTimer = 40;
         }
      }
   }

   private BlockPos findNearestBarrel() {
      MutableBlockPos mutablePos = new MutableBlockPos();
      BlockPos nearestBarrel = null;
      double nearestDistance = Double.MAX_VALUE;
      int searchRange = 16;

      for (int x = -searchRange; x <= searchRange; x++) {
         for (int y = -4; y <= 4; y++) {
            for (int z = -searchRange; z <= searchRange; z++) {
               mutablePos.set(this.blockPosition().getX() + x, this.blockPosition().getY() + y, this.blockPosition().getZ() + z);
               if (this.level().getBlockState(mutablePos).getBlock().toString().contains("barrel")) {
                  BlockEntity blockEntity = this.level().getBlockEntity(mutablePos);
                  if (blockEntity instanceof Container) {
                     double distance = this.distanceToSqr(Vec3.atCenterOf(mutablePos));
                     if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestBarrel = mutablePos.immutable();
                     }
                  }
               }
            }
         }
      }

      return nearestBarrel;
   }

   private void depositAllItems(Container container) {
      BlockPos barrelPos = null;

      for (int x = -2; x <= 2; x++) {
         for (int y = -1; y <= 1; y++) {
            for (int z = -2; z <= 2; z++) {
               BlockPos checkPos = this.blockPosition().offset(x, y, z);
               BlockState blockState = this.level().getBlockState(checkPos);
               BlockEntity blockEntity = this.level().getBlockEntity(checkPos);
               if (blockState.getBlock() instanceof BarrelBlock && blockEntity == container) {
                  barrelPos = checkPos;
                  break;
               }
            }
         }
      }

      if (barrelPos != null) {
         BlockState barrelState = this.level().getBlockState(barrelPos);
         if (barrelState.getBlock() instanceof BarrelBlock && !(Boolean)barrelState.getValue(BarrelBlock.OPEN)) {
            this.level().setBlock(barrelPos, (BlockState)barrelState.setValue(BarrelBlock.OPEN, true), 3);
            this.scheduleBarrelClose(barrelPos);
         }
      }

      for (int i = 0; i < this.inventory.getContainerSize(); i++) {
         ItemStack itemStack = this.inventory.getItem(i);
         if (!itemStack.isEmpty()) {
            ItemStack remainingStack = this.tryAddItemToContainer(container, itemStack);
            this.inventory.setItem(i, remainingStack);
         }
      }

      this.playSound(SoundEvents.BARREL_OPEN, 0.5F, 1.0F);
   }

   private void scheduleBarrelClose(BlockPos barrelPos) {
      this.scheduledBarrelClose = barrelPos;
      this.barrelCloseTimer = 40;
   }

   private ItemStack tryAddItemToContainer(Container container, ItemStack itemStack) {
      for (int j = 0; j < container.getContainerSize(); j++) {
         ItemStack containerStack = container.getItem(j);
         if (containerStack.isEmpty()) {
            container.setItem(j, itemStack.copy());
            return ItemStack.EMPTY;
         }

         if (ItemStack.isSameItemSameComponents(containerStack, itemStack)) {
            int maxStackSize = containerStack.getMaxStackSize();
            int spaceInSlot = maxStackSize - containerStack.getCount();
            if (spaceInSlot > 0) {
               int transferAmount = Math.min(itemStack.getCount(), spaceInSlot);
               containerStack.grow(transferAmount);
               itemStack.shrink(transferAmount);
               if (itemStack.isEmpty()) {
                  return ItemStack.EMPTY;
               }
            }
         }
      }

      return itemStack;
   }

   private void checkForItems() {
      if (!this.isPatrolling()) {
         ItemEntity nearestItem = this.findNearestItem();
         if (nearestItem != null) {
            double distance = this.distanceToSqr(nearestItem);
            if (distance <= 6.25) {
               if (this.inventory.canAddItem(nearestItem.getItem())) {
                  ItemStack remaining = this.inventory.addItem(nearestItem.getItem());
                  if (remaining.isEmpty()) {
                     nearestItem.discard();
                  } else {
                     nearestItem.setItem(remaining);
                  }

                  this.playSound(SoundEvents.ITEM_PICKUP, 0.2F, ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
               }
            } else if (distance <= 81.0) {
               this.getNavigation().moveTo(nearestItem, 1.0);
            }
         }
      }
   }

   private ItemEntity findNearestItem() {
      List<ItemEntity> nearbyItems = this.level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(9.0), item -> this.inventory.canAddItem(item.getItem()));
      return nearbyItems.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
   }

   public void pickUpItem(ItemEntity itemEntity) {
      ItemStack itemStack = itemEntity.getItem();
      ItemStack remaining = this.inventory.addItem(itemStack);
      if (remaining.isEmpty()) {
         itemEntity.discard();
      } else {
         itemEntity.setItem(remaining);
      }

      this.playSound(SoundEvents.ITEM_PICKUP, 0.2F, ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
   }

   public void setStationary(boolean stationary) {
      this.entityData.set(STATIONARY, stationary);
   }

   public Optional<BlockPos> getPatrolOrigin() {
      return (Optional<BlockPos>)this.entityData.get(PATROL_ORIGIN);
   }



   public void die(DamageSource source) {
      super.die(source);
      if (!this.level().isClientSide && source.getEntity() instanceof Player) {
         float baseChance = ((Double)Config.COMMON.gameplay.cogBeaconSpawnChance.get()).floatValue();
         float spawnChance = baseChance * 2.0F;
         if (baseChance > 0.0F && this.random.nextFloat() < spawnChance) {
            SignalBeaconEntity beacon = new SignalBeaconEntity((EntityType<? extends Mob>)ModEntities.SIGNAL_BEACON.get(), this.level());
            beacon.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
            this.level().addFreshEntity(beacon);
         }
      }
   }

   public boolean isPanicked() {
      return (Boolean)this.entityData.get(PANICKING);
   }

   public boolean isSitting() {
      return (Boolean)this.entityData.get(SITTING);
   }

   public void setSitting(boolean sitting) {
      this.entityData.set(SITTING, sitting);
   }

   public int getMaskColor() {
      return (Integer)this.entityData.get(MASK_COLOR);
   }

   public void setMaskColor(int color) {
      this.entityData.set(MASK_COLOR, color);
   }

   public static Builder createAttributes() {
      return Mob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 28.0)
         .add(Attributes.FOLLOW_RANGE, 24.0)
         .add(Attributes.MOVEMENT_SPEED, 0.31)
         .add(Attributes.ARMOR_TOUGHNESS, 3.0)
         .add(Attributes.ARMOR, 6.0);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(1, new SupplyScampEntity.SupplyScampPanicGoal(this, 2.5));
      this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
      if (this.isTame()) {
         this.goalSelector.addGoal(3, new SupplyScampEntity.SupplyScampFollowOwnerGoal(this, 1.3, 10.0F, 2.0F, false));
      } else {
         this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
      }

      this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
   }

   @NotNull
   public InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
      ItemStack itemstack = player.getItemInHand(hand);
      if (this.level().isClientSide) {
         boolean flag = this.isOwnedBy(player) || this.isTame() || itemstack.is((Item)ModItems.ANCIENT_BRASS.get()) && !this.isTame();
         return flag ? InteractionResult.CONSUME : InteractionResult.PASS;
      } else if (this.isTame()) {
         if (itemstack.is((Item)ModItems.REPAIR_KIT.get()) && this.getHealth() < this.getMaxHealth()) {
            if (!player.getAbilities().instabuild) {
               itemstack.shrink(1);
            }

            float healAmount = 10.0F;
            this.heal(healAmount);
            this.playSound(SoundEvents.GENERIC_EAT, 0.5F, 1.0F);
            if (this.level() instanceof ServerLevel serverLevel) {
               serverLevel.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 0.5, this.getZ(), 3, 0.3, 0.3, 0.3, 0.1);
            }

            return InteractionResult.SUCCESS;
         } else if (itemstack.getItem() == Items.SHEARS && this.isWearingPumpkin()) {
            if (!this.level().isClientSide) {
               this.setWearingPumpkin(false);
               this.gameEvent(GameEvent.SHEAR, player);
               itemstack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(player.getUsedItemHand()));
               this.spawnAtLocation(Items.CARVED_PUMPKIN);
               this.playSound(SoundEvents.PUMPKIN_CARVE, 1.0F, 1.0F);
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
         } else if ((itemstack.is(Items.CARVED_PUMPKIN) || itemstack.is(Items.JACK_O_LANTERN)) && !this.isWearingPumpkin()) {
            if (!this.level().isClientSide) {
               this.setWearingPumpkin(true);
               if (!player.getAbilities().instabuild) {
                  itemstack.shrink(1);
               }

               this.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1.0F, 1.0F);
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
         } else if (itemstack.getItem() instanceof DyeItem) {
            DyeColor dyeColor = ((DyeItem)itemstack.getItem()).getDyeColor();
            this.setMaskColor(DYE_COLOR_TO_MASK_INDEX[dyeColor.getId()]);
            if (!player.getAbilities().instabuild) {
               itemstack.shrink(1);
            }

            return InteractionResult.SUCCESS;
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
            if (!player.isShiftKeyDown() || !itemstack.isEmpty()) {
               player.openMenu(new SupplyScampMenuProvider(this));
            } else if (this.isOrderedToSit()) {
               this.setOrderedToSit(false);
               this.setSitting(false);
               this.setPatrolling(true);
               this.setPatrolOrigin(this.blockPosition());
               this.spawnPatrolOriginParticles();
               BlockPos nearestBarrel = this.findNearestBarrel();
               if (nearestBarrel == null) {
                  player.displayClientMessage(Component.translatable("message.supply_scamp.patrolling_no_barrel"), true);
               } else {
                  player.displayClientMessage(Component.translatable("message.supply_scamp.patrolling"), true);
               }
            } else if (this.isPatrolling()) {
               this.setPatrolling(false);
               this.setOrderedToSit(false);
               player.displayClientMessage(Component.translatable("message.supply_scamp.following"), true);
            } else {
               this.setOrderedToSit(true);
               this.setSitting(true);
               this.setPatrolling(false);
               player.displayClientMessage(Component.translatable("message.supply_scamp.sitting"), true);
            }

            return InteractionResult.SUCCESS;
         } else {
            boolean wasHeavy = this.removeArmorPlate();
            this.playSound(SoundEvents.ARMOR_STAND_BREAK, 0.5F, 1.0F);
            ItemStack droppedPlate = new ItemStack(wasHeavy ? (ItemLike)ModItems.HEAVY_ARMOR_PLATE.get() : (ItemLike)ModItems.ARMOR_PLATE.get());
            this.spawnAtLocation(droppedPlate);
            int currentTotal = this.getArmorPlates() + this.getHeavyArmorPlates();
            player.displayClientMessage(Component.translatable("message.mechanical_entity.armor_plating_removed", new Object[]{currentTotal, 4}), true);
            return InteractionResult.SUCCESS;
         }
      } else if (itemstack.is((Item)ModItems.ANCIENT_BRASS.get())) {
         if (!player.getAbilities().instabuild) {
            itemstack.shrink(1);
         }

         if (this.random.nextInt(3) == 0 && !EventHooks.onAnimalTame(this, player)) {
            this.tame(player);
            this.navigation.stop();
            this.setTarget(null);
            this.level().broadcastEntityEvent(this, (byte)7);
         } else {
            this.level().broadcastEntityEvent(this, (byte)6);
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.mobInteract(player, hand);
      }
   }

   public boolean hurt(DamageSource source, float amount) {
      return this.isInvulnerableTo(source) ? false : super.hurt(source, amount);
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
      Objects.requireNonNull(this.getAttribute(Attributes.ARMOR)).setBaseValue(6.0 + (double)totalArmor);
   }

   @Nullable
   public AgeableMob getBreedOffspring(@NotNull ServerLevel serverLevel, AgeableMob ageableMob) {
      return null;
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(PANICKING, false);
      builder.define(SITTING, false);
      builder.define(PATROLLING, false);
      builder.define(MASK_COLOR, 0);
      builder.define(PATROL_ORIGIN, Optional.empty());
      builder.define(STATIONARY, false);
      builder.define(WEARING_PUMPKIN, false);
      builder.define(PARTYING, false);
      builder.define(ARMOR_PLATES, 0);
      builder.define(HEAVY_ARMOR_PLATES, 0);
   }

   public boolean isPartying() {
      return (Boolean)this.entityData.get(PARTYING);
   }

   public void setPartying(boolean partying) {
      this.entityData.set(PARTYING, partying);
   }

   public void setRecordPlayingNearby(@NotNull BlockPos pos, boolean playing) {
      this.setPartying(playing && this.isTame());
   }

   public boolean isWearingPumpkin() {
      return (Boolean)this.entityData.get(WEARING_PUMPKIN);
   }

   public void setWearingPumpkin(boolean wearing) {
      this.entityData.set(WEARING_PUMPKIN, wearing);
   }

   private static boolean isHalloweenSeason() {
      LocalDate date = LocalDate.now();
      return date.getMonth() == Month.OCTOBER;
   }

   protected void updateWalkAnimation(float partialTick) {
      float f = this.getPose() == Pose.STANDING ? Math.min(partialTick * 6.0F, 1.0F) : 0.0F;
      this.walkAnimation.update(f, 0.2F);
   }

   @Nullable
   protected SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
      return (SoundEvent)ModSounds.SCAMP_HURT.get();
   }

   @Nullable
   protected SoundEvent getDeathSound() {
      return (SoundEvent)ModSounds.SCAMP_DIE.get();
   }

   public void remove(@NotNull RemovalReason reason) {
      if (reason == RemovalReason.KILLED) {
         for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack itemStack = this.inventory.getItem(i);
            if (!itemStack.isEmpty()) {
               this.spawnAtLocation(itemStack);
            }
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

      super.remove(reason);
   }

   public void setPersistenceRequired() {
      super.setPersistenceRequired();
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      @NotNull ServerLevelAccessor level,
      @NotNull DifficultyInstance difficulty,
      @NotNull MobSpawnType reason,
      @Nullable SpawnGroupData spawnData
   ) {
      spawnData = super.finalizeSpawn(level, difficulty, reason, spawnData);
      if (isHalloweenSeason() && this.random.nextFloat() < 0.85F) {
         this.setWearingPumpkin(true);
      }

      return spawnData;
   }

   public void addAdditionalSaveData(@NotNull CompoundTag compound) {
      super.addAdditionalSaveData(compound);
      ListTag listnbt = new ListTag();

      for (int i = 0; i < this.inventory.getContainerSize(); i++) {
         ItemStack itemstack = this.inventory.getItem(i);
         if (!itemstack.isEmpty()) {
            CompoundTag compoundnbt = new CompoundTag();
            compoundnbt.putByte("Slot", (byte)i);
            itemstack.save(this.level().registryAccess(), compoundnbt);
            listnbt.add(compoundnbt);
         }
      }

      compound.putBoolean("Patrolling", this.isPatrolling());
      this.getPatrolOrigin().ifPresent(pos -> {
         compound.putInt("PatrolOriginX", pos.getX());
         compound.putInt("PatrolOriginY", pos.getY());
         compound.putInt("PatrolOriginZ", pos.getZ());
      });
      compound.put("Items", listnbt);
      compound.putInt("MaskColor", this.getMaskColor());
      compound.putBoolean("WearingPumpkin", this.isWearingPumpkin());
      compound.putInt("ArmorPlates", this.getArmorPlates());
      compound.putInt("HeavyArmorPlates", this.getHeavyArmorPlates());
      if (this.scheduledBarrelClose != null) {
         compound.putInt("BarrelCloseX", this.scheduledBarrelClose.getX());
         compound.putInt("BarrelCloseY", this.scheduledBarrelClose.getY());
         compound.putInt("BarrelCloseZ", this.scheduledBarrelClose.getZ());
         compound.putInt("BarrelCloseTimer", this.barrelCloseTimer);
      }
   }

   public void readAdditionalSaveData(CompoundTag compound) {
      super.readAdditionalSaveData(compound);
      ListTag listnbt = compound.getList("Items", 10);

      for (int i = 0; i < listnbt.size(); i++) {
         CompoundTag compoundnbt = listnbt.getCompound(i);
         int j = compoundnbt.getByte("Slot") & 255;
         if (j < this.inventory.getContainerSize()) {
            this.inventory.setItem(j, top.ribs.scguns.util.NbtHelper.itemFromTag(compoundnbt));
         }
      }

      this.setPatrolling(compound.getBoolean("Patrolling"));
      if (compound.contains("PatrolOriginX") && compound.contains("PatrolOriginY") && compound.contains("PatrolOriginZ")) {
         BlockPos patrolOrigin = new BlockPos(compound.getInt("PatrolOriginX"), compound.getInt("PatrolOriginY"), compound.getInt("PatrolOriginZ"));
         this.setPatrolOrigin(patrolOrigin);
      }

      if (compound.contains("MaskColor", 3)) {
         this.setMaskColor(compound.getInt("MaskColor"));
      }

      if (compound.contains("WearingPumpkin")) {
         this.setWearingPumpkin(compound.getBoolean("WearingPumpkin"));
      }

      if (compound.contains("ArmorPlates")) {
         this.setArmorPlates(compound.getInt("ArmorPlates"));
      }

      if (compound.contains("HeavyArmorPlates")) {
         this.setHeavyArmorPlates(compound.getInt("HeavyArmorPlates"));
      }

      if (compound.contains("BarrelCloseX")) {
         this.scheduledBarrelClose = new BlockPos(compound.getInt("BarrelCloseX"), compound.getInt("BarrelCloseY"), compound.getInt("BarrelCloseZ"));
         this.barrelCloseTimer = compound.getInt("BarrelCloseTimer");
      }
   }

   public void setPatrolOrigin(BlockPos pos) {
      if (pos != null) {
         this.entityData.set(PATROL_ORIGIN, Optional.of(pos));
      } else {
         this.entityData.set(PATROL_ORIGIN, Optional.empty());
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

   public boolean isPatrolling() {
      return (Boolean)this.entityData.get(PATROLLING);
   }

   public void setPatrolling(boolean patrolling) {
      this.entityData.set(PATROLLING, patrolling);
   }

   public void setTame(boolean tamed, boolean applyTamingSideEffects) {
      super.setTame(tamed, applyTamingSideEffects);
      if (tamed) {
         Objects.requireNonNull(this.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(42.0);
         this.setHealth(42.0F);
      } else {
         Objects.requireNonNull(this.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(24.0);
      }

      this.goalSelector.removeAllGoals(goal -> true);
      this.registerGoals();
   }

   public boolean isFood(ItemStack stack) {
      return stack.is((Item)ModItems.ANCIENT_BRASS.get());
   }

   public boolean canBeLeashed() {
      return this.isTame() && super.canBeLeashed();
   }

   private void setPanicking(boolean b) {
      this.entityData.set(PANICKING, b);
   }

   public static class SupplyScampFollowOwnerGoal extends FollowOwnerGoal {
      private final SupplyScampEntity scamp;

      public SupplyScampFollowOwnerGoal(SupplyScampEntity scamp, double speed, float minDistance, float maxDistance, boolean leavesAllowed) {
         super(scamp, speed, minDistance, maxDistance);
         this.scamp = scamp;
      }

      public boolean canUse() {
         return super.canUse() && !this.scamp.isPatrolling();
      }

      public boolean canContinueToUse() {
         return super.canContinueToUse() && !this.scamp.isPatrolling();
      }

      public void start() {
         if (!this.scamp.isPatrolling()) {
            super.start();
         }
      }

      public void tick() {
         if (!this.scamp.isPatrolling()) {
            super.tick();
         }
      }
   }

   public static class SupplyScampPanicGoal extends PanicGoal {
      private final SupplyScampEntity scamp;

      public SupplyScampPanicGoal(SupplyScampEntity scamp, double speedModifier) {
         super(scamp, speedModifier);
         this.scamp = scamp;
      }

      public boolean canUse() {
         return this.scamp.isTame() && this.scamp.getLastHurtByMob() instanceof Player ? false : super.canUse();
      }

      public void start() {
         this.scamp.setPanicking(true);
         super.start();
      }

      public void stop() {
         this.scamp.setPanicking(false);
         super.stop();
      }
   }
}
