package top.ribs.scguns.entity.monster;


import net.minecraft.core.Holder;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import top.ribs.scguns.util.MobType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.entity.projectile.EnemyProjectileEntity;
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.init.ModSounds;
import top.ribs.scguns.init.ModTags;

public class ScampTankEntity extends Monster implements RangedAttackMob {
   private static final EntityDataAccessor<Integer> MAIN_TURRET_FLASH_TIMER = SynchedEntityData.defineId(
      ScampTankEntity.class, EntityDataSerializers.INT
   );
   private static final EntityDataAccessor<Integer> MACHINE_GUN_FLASH_TIMER = SynchedEntityData.defineId(
      ScampTankEntity.class, EntityDataSerializers.INT
   );
   private static final EntityDataAccessor<Boolean> IS_CHARGING = SynchedEntityData.defineId(ScampTankEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> IS_IN_SECOND_PHASE = SynchedEntityData.defineId(ScampTankEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> IS_IN_THIRD_PHASE = SynchedEntityData.defineId(ScampTankEntity.class, EntityDataSerializers.BOOLEAN);
   private boolean hasTriggeredWeaponDestruction = false;
   private int mainCannonCooldown = 0;
   private int machineGunCooldown = 0;
   private static final int MAIN_CANNON_COOLDOWN_TICKS = 40;
   private static final int MACHINE_GUN_COOLDOWN_TICKS = 5;
   private static final double MAIN_CANNON_RANGE = 35.0;
   private static final double MACHINE_GUN_RANGE = 12.0;
   private static final double PREFERRED_COMBAT_RANGE = 12.0;
   private static final double MIN_COMBAT_RANGE = 4.0;
   private static final double DETECTION_RANGE = 50.0;
   private int beaconSpawnCooldown = 0;
   private static final int BEACON_SPAWN_COOLDOWN = 60;
   private static final int MAX_SKY_CARRIERS_IN_AREA = 4;
   private static final double SKY_CARRIER_CHECK_RADIUS = 40.0;
   private Vec3 chargeDirection = Vec3.ZERO;
   private int chargeCooldown = 0;
   private int chargeWarmupTicks = 0;
   private int chargeActiveTicks = 0;
   private static final int CHARGE_WARMUP_DURATION = 20;
   private static final int CHARGE_DURATION = 35;
   private static final int CHARGE_COOLDOWN_DURATION = 60;
   private static final double CHARGE_SPEED = 1.5;
   private static final double CHARGE_DAMAGE = 8.0;
   private static final double CHARGE_RANGE = 45.0;
   private int postChargeRotationTicks = 0;
   private static final int POST_CHARGE_ROTATION_DURATION = 30;
   private boolean isRegenerating = false;
   private int regenerationTicks = 0;
   private static final int REGENERATION_DURATION = 60;
   private static final float REGENERATION_TARGET_HEALTH = 700.0F;
   private static final float REGENERATION_RATE = 2.0F;
   private boolean hasTriggeredThirdPhase = false;
   private boolean isRegeneratingThirdPhase = false;
   private int regenerationTicksThirdPhase = 0;
   private static final int THIRD_PHASE_REGENERATION_DURATION = 60;
   private static final float THIRD_PHASE_REGENERATION_TARGET_HEALTH = 350.0F;
   private static final float THIRD_PHASE_REGENERATION_RATE = 2.5F;
   private int scamplerSpawnCooldown = 0;
   private static final int SCAMPLER_SPAWN_COOLDOWN = 30;
   private static final int MAX_SCAMPLERS_IN_AREA = 10;
   private static final double SCAMPLER_CHECK_RADIUS = 30.0;
   private int thirdPhaseBeaconCooldown = 0;
   private static final int THIRD_PHASE_BEACON_COOLDOWN = 60;
   private static final float THIRD_PHASE_BEACON_CHANCE = 0.65F;
   private int repositionCooldown = 0;
   private int terrainDestructionCooldown = 0;
   private static final int TERRAIN_DESTRUCTION_COOLDOWN_TICKS = 10;
   private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(), BossBarColor.YELLOW, BossBarOverlay.PROGRESS);
   private final int avoidanceTimer = 0;
   private int noLineOfSightTimer = 0;
   private static final int NO_LOS_THRESHOLD = 60;
   private boolean isAggressivelyRepositioning = false;
   private Vec3 lastKnownTargetPosition = null;
   private int frustratedShotAttempts = 0;

   public ScampTankEntity(EntityType<? extends ScampTankEntity> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.bossEvent.setVisible(false);
      this.xpReward = 50;
      this.setPersistenceRequired();
      this.setHealth(this.getMaxHealth());
   }

   public boolean requiresCustomPersistence() {
      return true;
   }

   public boolean removeWhenFarAway(double distanceToClosestPlayer) {
      return false;
   }

   public boolean isInSecondPhase() {
      return (Boolean)this.entityData.get(IS_IN_SECOND_PHASE);
   }

   public boolean isInvulnerableTo(DamageSource source) {
      return source.is(DamageTypeTags.IS_FIRE) || super.isInvulnerableTo(source);
   }

   public void setInSecondPhase(boolean inSecondPhase) {
      this.entityData.set(IS_IN_SECOND_PHASE, inSecondPhase);
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 1000.0)
         .add(Attributes.FOLLOW_RANGE, 35.0)
         .add(Attributes.MOVEMENT_SPEED, 0.35)
         .add(Attributes.ARMOR_TOUGHNESS, 0.8F)
         .add(Attributes.ARMOR, 12.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
         .add(Attributes.ATTACK_KNOCKBACK, 0.5)
         .add(Attributes.ATTACK_DAMAGE, 3.0)
         .add(Attributes.STEP_HEIGHT, 2.0);
   }

   public void onRemovedFromLevel() {
      super.onRemovedFromLevel();
      if (!this.level().isClientSide) {
         this.bossEvent.removeAllPlayers();
      }
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
            && effect != MobEffects.HEAL
         ? super.canBeAffected(pPotionEffect)
         : false;
   }

   public void die(@NotNull DamageSource pCause) {
      if (this.isRegenerating) {
         this.isRegenerating = false;
         this.removeEffect(MobEffects.DAMAGE_RESISTANCE);
      }

      if (this.isRegeneratingThirdPhase) {
         this.isRegeneratingThirdPhase = false;
         this.removeEffect(MobEffects.DAMAGE_RESISTANCE);
      }

      this.entityData.set(IS_CHARGING, false);
      this.chargeWarmupTicks = 0;
      this.chargeActiveTicks = 0;
      this.postChargeRotationTicks = 0;
      if (!this.level().isClientSide) {
         this.bossEvent.removeAllPlayers();
      }

      super.die(pCause);
   }

   public void setCustomName(Component name) {
      super.setCustomName(name);
      this.bossEvent.setName(name != null ? name : this.getDisplayName());
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(MAIN_TURRET_FLASH_TIMER, 0);
      builder.define(MACHINE_GUN_FLASH_TIMER, 0);
      builder.define(IS_CHARGING, false);
      builder.define(IS_IN_SECOND_PHASE, false);
      builder.define(IS_IN_THIRD_PHASE, false);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(1, new ScampTankEntity.TankChargeGoal());
      this.goalSelector.addGoal(2, new ScampTankEntity.TankChaseGoal());
      this.goalSelector.addGoal(3, new ScampTankEntity.TankLookGoal());
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
      this.targetSelector.addGoal(2, new ScampTankEntity.ExtendedRangeTargetGoal());
   }

   public boolean hurt(DamageSource source, float amount) {
      if (source.is(DamageTypeTags.IS_EXPLOSION)) {
         amount *= 0.5F;
      }

      return super.hurt(source, amount);
   }

   public boolean isInThirdPhase() {
      return (Boolean)this.entityData.get(IS_IN_THIRD_PHASE);
   }

   public void setInThirdPhase(boolean inThirdPhase) {
      this.entityData.set(IS_IN_THIRD_PHASE, inThirdPhase);
   }

   private void triggerWeaponDestruction() {
      if (!this.level().isClientSide && !this.hasTriggeredWeaponDestruction && this.isAlive()) {
         this.hasTriggeredWeaponDestruction = true;
         this.setInSecondPhase(true);
         this.setHealth(700.0F);
         this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1));
         this.isRegenerating = true;
         this.regenerationTicks = 60;
         this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 0.8F);
         if (this.level() instanceof ServerLevel serverLevel) {
            double turretX = this.getX();
            double turretY = this.getY() + (double)this.getBbHeight() * 0.8;
            double turretZ = this.getZ();

            for (int i = 0; i < 30; i++) {
               double offsetX = (this.random.nextDouble() - 0.5) * 4.0;
               double offsetY = (this.random.nextDouble() - 0.5) * 2.0;
               double offsetZ = (this.random.nextDouble() - 0.5) * 4.0;
               serverLevel.sendParticles(ParticleTypes.EXPLOSION, turretX + offsetX, turretY + offsetY, turretZ + offsetZ, 1, 0.0, 0.0, 0.0, 0.0);
            }

            double machineGunX = this.getX() + Math.cos(Math.toRadians((double)(this.getYRot() + 90.0F))) * 2.0;
            double machineGunY = this.getY() + (double)this.getBbHeight() * 0.6;
            double machineGunZ = this.getZ() + Math.sin(Math.toRadians((double)(this.getYRot() + 90.0F))) * 2.0;

            for (int i = 0; i < 20; i++) {
               double offsetX = (this.random.nextDouble() - 0.5) * 2.0;
               double offsetY = this.random.nextDouble() - 0.5;
               double offsetZ = (this.random.nextDouble() - 0.5) * 2.0;
               serverLevel.sendParticles(ParticleTypes.EXPLOSION, machineGunX + offsetX, machineGunY + offsetY, machineGunZ + offsetZ, 1, 0.0, 0.0, 0.0, 0.0);
            }

            for (int i = 0; i < 50; i++) {
               double smokeX = turretX + (this.random.nextDouble() - 0.5) * 6.0;
               double smokeY = turretY + this.random.nextDouble() * 3.0;
               double smokeZ = turretZ + (this.random.nextDouble() - 0.5) * 6.0;
               serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, smokeX, smokeY, smokeZ, 1, 0.0, 0.1, 0.0, 0.02);
            }
         }
      }
   }

   private void triggerThirdPhase() {
      if (!this.level().isClientSide && !this.hasTriggeredThirdPhase && this.isAlive()) {
         this.hasTriggeredThirdPhase = true;
         this.setInThirdPhase(true);
         this.setHealth(350.0F);
         this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 2));
         this.isRegeneratingThirdPhase = true;
         this.regenerationTicksThirdPhase = 60;
         this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.0F, 0.6F);
         if (this.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 50; i++) {
               double offsetX = (this.random.nextDouble() - 0.5) * 6.0;
               double offsetY = (this.random.nextDouble() - 0.5) * 4.0;
               double offsetZ = (this.random.nextDouble() - 0.5) * 6.0;
               serverLevel.sendParticles(
                  ParticleTypes.SOUL_FIRE_FLAME, this.getX() + offsetX, this.getY() + 2.0 + offsetY, this.getZ() + offsetZ, 1, 0.0, 0.0, 0.0, 0.0
               );
            }

            for (int i = 0; i < 40; i++) {
               double smokeX = this.getX() + (this.random.nextDouble() - 0.5) * 8.0;
               double smokeY = this.getY() + this.random.nextDouble() * 4.0;
               double smokeZ = this.getZ() + (this.random.nextDouble() - 0.5) * 8.0;
               serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, smokeX, smokeY, smokeZ, 1, 0.0, 0.15, 0.0, 0.03);
            }
         }
      }
   }

   private int countNearbyScamplers() {
      if (this.level().isClientSide) {
         return 0;
      } else {
         AABB searchArea = new AABB(
            this.getX() - 30.0, this.getY() - 10.0, this.getZ() - 30.0, this.getX() + 30.0, this.getY() + 10.0, this.getZ() + 30.0
         );
         List<ScamplerEntity> scamplers = this.level().getEntitiesOfClass(ScamplerEntity.class, searchArea);
         return scamplers.size();
      }
   }

   private void spawnScampler() {
      if (!this.level().isClientSide) {
         if (this.countNearbyScamplers() < 10) {
            for (int attempt = 0; attempt < 10; attempt++) {
               double angle = this.random.nextDouble() * Math.PI * 2.0;
               double distance = 3.0 + this.random.nextDouble() * 4.0;
               double spawnX = this.getX() + Math.cos(angle) * distance;
               double spawnZ = this.getZ() + Math.sin(angle) * distance;
               double spawnY = this.getY();
               BlockPos spawnPos = new BlockPos((int)spawnX, (int)spawnY, (int)spawnZ);

               for (int y = 0; y < 5; y++) {
                  BlockPos checkPos = spawnPos.below(y);
                  if (!this.level().getBlockState(checkPos).isAir()) {
                     spawnY = (double)(checkPos.getY() + 1);
                     break;
                  }
               }

               BlockPos finalSpawnPos = new BlockPos((int)spawnX, (int)spawnY, (int)spawnZ);
               if (this.level().getBlockState(finalSpawnPos).isAir() && this.level().getBlockState(finalSpawnPos.above()).isAir()) {
                  ScamplerEntity scampler = new ScamplerEntity((EntityType<? extends Monster>)ModEntities.SCAMPLER.get(), this.level());
                  scampler.moveTo(spawnX, spawnY, spawnZ, this.random.nextFloat() * 360.0F, 0.0F);
                  if (this.getTarget() != null) {
                     scampler.setTarget(this.getTarget());
                  }

                  this.level().addFreshEntity(scampler);
                  if (this.level() instanceof ServerLevel serverLevel) {
                     for (int i = 0; i < 15; i++) {
                        serverLevel.sendParticles(ParticleTypes.POOF, spawnX, spawnY + 0.5, spawnZ, 1, 0.3, 0.3, 0.3, 0.1);
                     }

                     serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, spawnX, spawnY + 0.5, spawnZ, 5, 0.2, 0.2, 0.2, 0.05);
                  }

                  this.level().playSound(null, spawnX, spawnY, spawnZ, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 0.8F, 1.5F);
                  break;
               }
            }
         }
      }
   }

   private void checkAndDestroyTerrain() {
      if (!this.level().isClientSide && this.terrainDestructionCooldown <= 0) {
         AABB destructionBox = this.getBoundingBox().inflate(0.5, 0.2, 0.5);
         BlockPos minPos = new BlockPos(
            (int)Math.floor(destructionBox.minX), (int)Math.floor(destructionBox.minY), (int)Math.floor(destructionBox.minZ)
         );
         BlockPos maxPos = new BlockPos(
            (int)Math.ceil(destructionBox.maxX), (int)Math.ceil(destructionBox.maxY + 1.0), (int)Math.ceil(destructionBox.maxZ)
         );
         boolean destroyedAny = false;

         for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            BlockState state = this.level().getBlockState(pos);
            if (this.canDestroyBlock(state, pos)) {
               this.destroyBlock(pos, state, false);
               destroyedAny = true;
            }
         }

         if (destroyedAny) {
            this.terrainDestructionCooldown = 10;
         }
      }
   }

   private void chargeDestroyTerrain() {
      if (!this.level().isClientSide) {
         Vec3 chargeDir = this.chargeDirection.normalize();
         double checkDistance = 3.0;

         for (double d = 0.0; d <= checkDistance; d += 0.5) {
            Vec3 checkPos = this.position().add(chargeDir.scale(d));

            for (int x = -1; x <= 1; x++) {
               for (int y = 0; y <= 2; y++) {
                  for (int z = -1; z <= 1; z++) {
                     BlockPos pos = new BlockPos(
                        (int)(checkPos.x + (double)x), (int)(checkPos.y + (double)y), (int)(checkPos.z + (double)z)
                     );
                     BlockState state = this.level().getBlockState(pos);
                     if (this.canDestroyBlock(state, pos)) {
                        this.destroyBlock(pos, state, true);
                     }
                  }
               }
            }
         }
      }
   }

   private boolean canDestroyBlock(BlockState state, BlockPos pos) {
      return state.isAir() ? false : state.is(ModTags.Blocks.TANK_BREAKABLE);
   }

   private void destroyBlock(BlockPos pos, BlockState state, boolean isCharging) {
      if (this.level() instanceof ServerLevel serverLevel) {
         if (serverLevel.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) && this.random.nextFloat() < (isCharging ? 0.1F : 0.2F)) {
            Block.dropResources(state, serverLevel, pos, null, this, ItemStack.EMPTY);
         }

         this.level().destroyBlock(pos, false);
         serverLevel.sendParticles(
            isCharging ? ParticleTypes.EXPLOSION : ParticleTypes.CLOUD,
            (double)pos.getX() + 0.5,
            (double)pos.getY() + 0.5,
            (double)pos.getZ() + 0.5,
            isCharging ? 3 : 5,
            0.25,
            0.25,
            0.25,
            0.1
         );
         this.level()
            .playSound(
               null, pos, isCharging ? SoundEvents.STONE_BREAK : SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, isCharging ? 1.5F : 1.0F, isCharging ? 0.7F : 0.9F
            );
         if (isCharging) {
            for (int i = 0; i < 10; i++) {
               serverLevel.sendParticles(
                  ParticleTypes.ITEM_SNOWBALL,
                  (double)pos.getX() + 0.5,
                  (double)pos.getY() + 0.5,
                  (double)pos.getZ() + 0.5,
                  1,
                  this.random.nextGaussian() * 0.3,
                  this.random.nextDouble() * 0.3 + 0.2,
                  this.random.nextGaussian() * 0.3,
                  0.15
               );
            }
         }
      }
   }

   public void tick() {
      super.tick();
      if (!this.isAlive()) {
         if (this.isRegenerating) {
            this.isRegenerating = false;
            this.removeEffect(MobEffects.DAMAGE_RESISTANCE);
         }

         if (this.isRegeneratingThirdPhase) {
            this.isRegeneratingThirdPhase = false;
            this.removeEffect(MobEffects.DAMAGE_RESISTANCE);
         }

         this.entityData.set(IS_CHARGING, false);
         if (!this.level().isClientSide) {
            this.bossEvent.removeAllPlayers();
         }
      } else {
         if (!this.level().isClientSide) {
            if (this.terrainDestructionCooldown > 0) {
               this.terrainDestructionCooldown--;
            }

            Vec3 movement = this.getDeltaMovement();
            double speed = movement.horizontalDistance();
            if (speed > 0.1) {
               this.checkAndDestroyTerrain();
            }

            this.updateFlashTimers();
            this.updateAttackCooldowns();
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
            if (this.isInThirdPhase()) {
               this.bossEvent.setColor(BossBarColor.PURPLE);
               this.triggerThirdPhase();
               this.handleThirdPhase();
            } else if (this.isInSecondPhase()) {
               this.bossEvent.setColor(BossBarColor.RED);
               this.triggerWeaponDestruction();
               this.handleChargingPhase();
               if (this.getHealth() / this.getMaxHealth() <= 0.25F && !this.isRegenerating) {
                  this.setInThirdPhase(true);
               }

               if (this.isRegenerating && this.regenerationTicks > 0) {
                  float currentHealth = this.getHealth();
                  if (currentHealth < 700.0F) {
                     this.setHealth(Math.min(currentHealth + 2.0F, 700.0F));
                  }

                  this.regenerationTicks--;
                  if (this.regenerationTicks <= 0) {
                     this.isRegenerating = false;
                     this.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                  }
               }

               if (this.beaconSpawnCooldown > 0) {
                  this.beaconSpawnCooldown--;
               }
            } else {
               this.bossEvent.setColor(BossBarColor.YELLOW);
               if (this.getHealth() / this.getMaxHealth() <= 0.25F) {
                  this.setInSecondPhase(true);
               } else {
                  this.handleCombat();
               }
            }

            for (ServerPlayer player : Objects.requireNonNull(this.level().getServer()).getPlayerList().getPlayers()) {
               double distance = this.distanceToSqr(player);
               if (distance < 2500.0 && this.isAlive()) {
                  this.bossEvent.addPlayer(player);
               } else {
                  this.bossEvent.removePlayer(player);
               }
            }

            if (this.repositionCooldown > 0) {
               this.repositionCooldown--;
            }

            if (this.chargeCooldown > 0) {
               this.chargeCooldown--;
            }

            if (this.scamplerSpawnCooldown > 0) {
               this.scamplerSpawnCooldown--;
            }

            if (this.thirdPhaseBeaconCooldown > 0) {
               this.thirdPhaseBeaconCooldown--;
            }
         }

         if (this.level().isClientSide && this.isAlive()) {
            this.addMovementParticles();
            if (this.isCharging()) {
               this.addChargingParticles();
            }

            if (this.isInThirdPhase()) {
               this.addThirdPhaseParticles();
            }
         }
      }
   }

   private void handleThirdPhase() {
      if (this.isAlive()) {
         if (this.isRegeneratingThirdPhase && this.regenerationTicksThirdPhase > 0) {
            float currentHealth = this.getHealth();
            if (currentHealth < 350.0F) {
               this.setHealth(Math.min(currentHealth + 2.5F, 350.0F));
            }

            this.regenerationTicksThirdPhase--;
            if (this.regenerationTicksThirdPhase <= 0) {
               this.isRegeneratingThirdPhase = false;
               this.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            }
         }

         if (this.scamplerSpawnCooldown <= 0) {
            this.spawnScampler();
            this.scamplerSpawnCooldown = 30;
         }

         if (this.thirdPhaseBeaconCooldown <= 0) {
            if (this.random.nextFloat() < 0.65F) {
               this.spawnThirdPhaseBeacon();
               this.thirdPhaseBeaconCooldown = 60;
            } else {
               this.thirdPhaseBeaconCooldown = 60;
            }
         }

         if (this.getNavigation().isDone() && this.random.nextInt(100) < 3) {
            double wanderX = this.getX() + (this.random.nextDouble() - 0.5) * 16.0;
            double wanderZ = this.getZ() + (this.random.nextDouble() - 0.5) * 16.0;
            this.getNavigation().moveTo(wanderX, this.getY(), wanderZ, 0.4);
         }
      }
   }

   private void spawnThirdPhaseBeacon() {
      if (!this.level().isClientSide) {
         if (this.countNearbySkyCatriers() < 4) {
            LivingEntity target = this.getTarget();
            Vec3 targetDirection;
            if (target != null) {
               double dx = target.getX() - this.getX();
               double dz = target.getZ() - this.getZ();
               double length = Math.sqrt(dx * dx + dz * dz);
               double angle = Math.atan2(dz, dx) + (this.random.nextDouble() - 0.5) * Math.PI * 0.3;
               double distance = 8.0 + this.random.nextDouble() * 12.0;
               targetDirection = new Vec3(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
            } else {
               double angle = this.random.nextDouble() * Math.PI * 2.0;
               double distance = 6.0 + this.random.nextDouble() * 14.0;
               targetDirection = new Vec3(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
            }

            BeaconProjectileEntity beaconProjectile = new BeaconProjectileEntity(
               (EntityType<? extends BeaconProjectileEntity>)ModEntities.BEACON_PROJECTILE.get(), this.level(), this
            );
            double launchX = this.getX();
            double launchY = this.getY() + (double)this.getBbHeight() + 1.0;
            double launchZ = this.getZ();
            beaconProjectile.setPos(launchX, launchY, launchZ);
            Vec3 landingPos = this.position().add(targetDirection);
            beaconProjectile.setLandingTarget(landingPos.x, landingPos.z);
            double dx = landingPos.x - launchX;
            double dz = landingPos.z - launchZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            double launchVelocity = 1.4;
            double launchAngle = Math.PI / 5;
            Vec3 launchVector = new Vec3(
               dx / distance * launchVelocity * Math.cos(launchAngle),
               launchVelocity * Math.sin(launchAngle),
               dz / distance * launchVelocity * Math.cos(launchAngle)
            );
            beaconProjectile.setDeltaMovement(launchVector);
            this.level().addFreshEntity(beaconProjectile);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.DISPENSER_LAUNCH, SoundSource.HOSTILE, 1.2F, 0.6F);
            if (this.level() instanceof ServerLevel serverLevel) {
               for (int i = 0; i < 8; i++) {
                  serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, launchX, launchY, launchZ, 1, 0.2, 0.1, 0.2, 0.08);
               }

               for (int i = 0; i < 5; i++) {
                  serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, launchX, launchY, launchZ, 1, 0.3, 0.1, 0.3, 0.1);
               }
            }
         }
      }
   }

   private void addThirdPhaseParticles() {
      if (this.random.nextInt(3) == 0) {
         this.level()
            .addParticle(
               ParticleTypes.SOUL_FIRE_FLAME,
               this.getX() + this.random.nextGaussian() * 3.0,
               this.getY() + 1.0 + this.random.nextDouble() * 2.0,
               this.getZ() + this.random.nextGaussian() * 3.0,
               this.random.nextGaussian() * 0.02,
               0.05,
               this.random.nextGaussian() * 0.02
            );
      }

      if (this.random.nextInt(5) == 0) {
         this.level()
            .addParticle(
               ParticleTypes.LARGE_SMOKE,
               this.getX() + this.random.nextGaussian() * 2.5,
               this.getY() + 2.0,
               this.getZ() + this.random.nextGaussian() * 2.5,
               0.0,
               0.08,
               0.0
            );
      }

      if (this.random.nextInt(8) == 0) {
         this.level()
            .addParticle(
               ParticleTypes.ELECTRIC_SPARK,
               this.getX() + this.random.nextGaussian() * 2.0,
               this.getY() + 1.0 + this.random.nextDouble(),
               this.getZ() + this.random.nextGaussian() * 2.0,
               this.random.nextGaussian() * 0.1,
               this.random.nextDouble() * 0.1,
               this.random.nextGaussian() * 0.1
            );
      }
   }

   private void handleChargingPhase() {
      if (this.isAlive()) {
         LivingEntity target = this.getTarget();
         if (target != null) {
            float targetPostChargeYaw = 0.0F;
            if (this.chargeWarmupTicks > 0) {
               this.chargeWarmupTicks--;
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.1, 1.0, 0.1));
               if (this.chargeWarmupTicks == 0) {
                  this.chargeActiveTicks = 35;
                  this.entityData.set(IS_CHARGING, true);
                  double dx = target.getX() - this.getX();
                  double dz = target.getZ() - this.getZ();
                  double length = Math.sqrt(dx * dx + dz * dz);
                  this.chargeDirection = length > 0.0 ? new Vec3(dx / length, 0.0, dz / length) : Vec3.ZERO;
                  this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 0.7F);
               }
            } else if (this.chargeActiveTicks > 0) {
               this.chargeActiveTicks--;
               this.executeCharge();
               if (this.chargeActiveTicks == 0) {
                  this.entityData.set(IS_CHARGING, false);
                  this.chargeCooldown = 60;
                  this.setDeltaMovement(this.getDeltaMovement().multiply(0.2, 1.0, 0.2));
                  this.spawnSupplyBeacons();
                  double dx = target.getX() - this.getX();
                  double dz = target.getZ() - this.getZ();
                  this.postChargeRotationTicks = 30;
               }
            } else if (this.postChargeRotationTicks > 0) {
               this.postChargeRotationTicks--;
               double dx = target.getX() - this.getX();
               double dz = target.getZ() - this.getZ();
               targetPostChargeYaw = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI) - 90.0);
               float currentYaw = this.getYRot();
               float newYaw = this.lerpAngle(currentYaw, targetPostChargeYaw, 0.5F);
               this.setYRot(newYaw);
               this.yBodyRot = newYaw;
               this.setYHeadRot(newYaw);
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.0, 1.0, 0.0));
               this.getNavigation().stop();
               if (this.postChargeRotationTicks == 0) {
                  this.setYRot(targetPostChargeYaw);
                  this.yBodyRot = targetPostChargeYaw;
                  this.setYHeadRot(targetPostChargeYaw);
               }
            }
         }
      }
   }

   private float lerpAngle(float current, float target, float factor) {
      float difference = target - current;

      while (difference > 180.0F) {
         difference -= 360.0F;
      }

      while (difference < -180.0F) {
         difference += 360.0F;
      }

      return current + difference * factor;
   }

   private void executeCharge() {
      Vec3 movement = this.chargeDirection.scale(1.5);
      this.setDeltaMovement(movement.x, this.getDeltaMovement().y, movement.z);
      this.chargeDestroyTerrain();
      AABB hitBox = this.getBoundingBox().expandTowards(movement).inflate(0.5);

      for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, hitBox, entityx -> entityx != this && entityx.isAlive() && !entityx.isSpectator())) {
         if (entity.hurt(this.damageSources().mobAttack(this), 8.0F)) {
            double dx = entity.getX() - this.getX();
            double dz = entity.getZ() - this.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 0.0) {
               entity.setDeltaMovement(entity.getDeltaMovement().add(dx / distance * 1.5, 0.3, dz / distance * 1.5));
            }

            this.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.MUD_HIT, SoundSource.HOSTILE, 1.5F, 0.8F);
         }
      }

      BlockPos frontPos = new BlockPos(
         (int)(this.getX() + this.chargeDirection.x * 2.5), (int)this.getY(), (int)(this.getZ() + this.chargeDirection.z * 2.5)
      );
      BlockState blockState = this.level().getBlockState(frontPos);
      if (!blockState.isAir() && blockState.blocksMotion()) {
         this.chargeActiveTicks = 0;
         this.entityData.set(IS_CHARGING, false);
         this.chargeCooldown = 20;
         if (this.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 25; i++) {
               serverLevel.sendParticles(
                  ParticleTypes.CRIT,
                  (double)frontPos.getX() + 0.5,
                  (double)frontPos.getY() + 1.0,
                  (double)frontPos.getZ() + 0.5,
                  1,
                  0.5,
                  0.5,
                  0.5,
                  0.1
               );
            }
         }

         this.level()
            .playSound(
               null,
               (double)frontPos.getX(),
               (double)frontPos.getY(),
               (double)frontPos.getZ(),
               SoundEvents.ANVIL_FALL,
               SoundSource.HOSTILE,
               2.0F,
               0.6F
            );
      }
   }

   private void addChargingParticles() {
      if (this.random.nextInt(2) == 0) {
         double backX = this.getX() - Math.cos(Math.toRadians((double)this.getYRot())) * 2.5;
         double backZ = this.getZ() - Math.sin(Math.toRadians((double)this.getYRot())) * 2.5;
         this.level()
            .addParticle(
               ParticleTypes.LARGE_SMOKE,
               backX + this.random.nextGaussian() * 0.3,
               this.getY() + 2.0 + this.random.nextDouble() * 0.5,
               backZ + this.random.nextGaussian() * 0.3,
               this.random.nextGaussian() * 0.05,
               0.1,
               this.random.nextGaussian() * 0.05
            );
      }

      if (this.random.nextInt(1) == 0) {
         this.addIntenseTreadParticles();
      }

      if (this.random.nextInt(3) == 0) {
         this.level()
            .addParticle(
               ParticleTypes.ELECTRIC_SPARK,
               this.getX() + this.random.nextGaussian() * 2.0,
               this.getY() + 1.0 + this.random.nextDouble(),
               this.getZ() + this.random.nextGaussian() * 2.0,
               this.random.nextGaussian() * 0.2,
               this.random.nextDouble() * 0.2,
               this.random.nextGaussian() * 0.2
            );
      }
   }

   private void addIntenseTreadParticles() {
      for (int side = 0; side < 2; side++) {
         double sideOffset = side == 0 ? -1.5 : 1.5;
         double treadX = this.getX() + sideOffset * Math.cos(Math.toRadians((double)(this.getYRot() + 90.0F)));
         double treadZ = this.getZ() + sideOffset * Math.sin(Math.toRadians((double)(this.getYRot() + 90.0F)));

         for (int i = 0; i < 4; i++) {
            this.level()
               .addParticle(
                  ParticleTypes.POOF,
                  treadX + this.random.nextGaussian() * 0.5,
                  this.getY() + 0.1,
                  treadZ + this.random.nextGaussian() * 0.5,
                  this.random.nextGaussian() * 0.3,
                  this.random.nextDouble() * 0.2,
                  this.random.nextGaussian() * 0.3
               );
         }
      }
   }

   private boolean hasCleanLineOfSight(LivingEntity target) {
      if (target == null) {
         return false;
      } else {
         double[][] checkPoints = new double[][]{
            {0.0, (double)this.getBbHeight() * 0.8, 0.0}, {0.0, (double)this.getBbHeight() * 0.6, 0.0}, {0.0, (double)this.getBbHeight() * 1.2, 0.0}
         };

         for (double[] point : checkPoints) {
            Vec3 tankPos = new Vec3(this.getX() + point[0], this.getY() + point[1], this.getZ() + point[2]);
            Vec3 targetPos = new Vec3(target.getX(), target.getY() + (double)target.getBbHeight() * 0.5, target.getZ());
            if (this.level().clip(new ClipContext(tankPos, targetPos, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, this)).getType()
               == Type.MISS) {
               return true;
            }
         }

         return false;
      }
   }

   public void setTarget(@Nullable LivingEntity target) {
      super.setTarget(target);
      if (target != null && !this.level().isClientSide) {
         this.bossEvent.setVisible(true);
      }
   }

   private void handleCombat() {
      LivingEntity target = this.getTarget();
      if (target == null) {
         this.noLineOfSightTimer = 0;
      } else {
         if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            this.setTarget(null);
            return;
         }

         boolean hasLOS = this.hasCleanLineOfSight(target);
         double distanceToTarget = this.distanceToSqr(target);
         if (!hasLOS) {
            this.noLineOfSightTimer++;
            this.lastKnownTargetPosition = target.position();
            if (this.noLineOfSightTimer >= 60 && !this.isAggressivelyRepositioning) {
               this.initiateAggressiveRepositioning();
            }

            if (this.noLineOfSightTimer < 20 && this.lastKnownTargetPosition != null) {
               this.attemptPredictiveShot(target);
            }
         } else {
            this.noLineOfSightTimer = 0;
            this.isAggressivelyRepositioning = false;
            this.frustratedShotAttempts = 0;
            if (distanceToTarget <= 144.0 && this.machineGunCooldown <= 0) {
               this.fireMachineGun(target);
               this.machineGunCooldown = 5;
            } else if (distanceToTarget > 144.0 && distanceToTarget <= 1225.0 && this.mainCannonCooldown <= 0) {
               this.fireMainCannon(target);
               this.mainCannonCooldown = 40;
            }
         }
      }
   }

   private void initiateAggressiveRepositioning() {
      LivingEntity target = this.getTarget();
      if (target != null) {
         this.isAggressivelyRepositioning = true;
         this.frustratedShotAttempts++;
         this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5F, 0.5F);
         if (this.frustratedShotAttempts >= 3 && !this.isInSecondPhase()) {
            this.fireSuppressionBarrage(this.lastKnownTargetPosition);
            this.frustratedShotAttempts = 0;
         }
      }
   }

   private void attemptPredictiveShot(LivingEntity target) {
      if (this.mainCannonCooldown <= 0 && this.lastKnownTargetPosition != null) {
         Vec3 targetVelocity = target.getDeltaMovement();
         Vec3 predictedPos = this.lastKnownTargetPosition
            .add(targetVelocity.x * 20.0, targetVelocity.y * 10.0, targetVelocity.z * 20.0);
         double turretHeight = (double)this.getBbHeight() * 1.2;
         double spawnX = this.getX();
         double spawnY = this.getY() + turretHeight;
         double spawnZ = this.getZ();
         ScampRocketEntity rocket = new ScampRocketEntity((EntityType<? extends ScampRocketEntity>)ModEntities.SCAMP_ROCKET.get(), this.level(), this);
         rocket.setPos(spawnX, spawnY, spawnZ);
         rocket.setDamage(5.0);
         rocket.setExplosionRadius(4.0F);
         double dx = predictedPos.x - spawnX;
         double dy = predictedPos.y - spawnY;
         double dz = predictedPos.z - spawnZ;
         rocket.shoot(dx, dy, dz, 2.8F, 2.0F);
         this.level().addFreshEntity(rocket);
         this.mainCannonCooldown = 20;
      }
   }

   private void fireSuppressionBarrage(Vec3 targetArea) {
      if (!this.level().isClientSide) {
         for (int i = 0; i < 5; i++) {
            ScampRocketEntity rocket = new ScampRocketEntity((EntityType<? extends ScampRocketEntity>)ModEntities.SCAMP_ROCKET.get(), this.level(), this);
            double spawnX = this.getX();
            double spawnY = this.getY() + (double)this.getBbHeight() * 1.2;
            double spawnZ = this.getZ();
            rocket.setPos(spawnX, spawnY, spawnZ);
            rocket.setDamage(4.0);
            rocket.setExplosionRadius(3.0F);
            double spreadX = (this.random.nextDouble() - 0.5) * 8.0;
            double spreadZ = (this.random.nextDouble() - 0.5) * 8.0;
            double dx = targetArea.x + spreadX - spawnX;
            double dy = targetArea.y - spawnY;
            double dz = targetArea.z + spreadZ - spawnZ;
            rocket.shoot(dx, dy, dz, 2.5F, 3.0F);
            this.level().addFreshEntity(rocket);
         }

         this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 2.0F, 0.6F);
         this.mainCannonCooldown = 80;
      }
   }

   private void updateFlashTimers() {
      int mainTimer = (Integer)this.entityData.get(MAIN_TURRET_FLASH_TIMER);
      if (mainTimer > 0) {
         this.entityData.set(MAIN_TURRET_FLASH_TIMER, mainTimer - 1);
      }

      int machineTimer = (Integer)this.entityData.get(MACHINE_GUN_FLASH_TIMER);
      if (machineTimer > 0) {
         this.entityData.set(MACHINE_GUN_FLASH_TIMER, machineTimer - 1);
      }
   }

   private void updateAttackCooldowns() {
      if (this.mainCannonCooldown > 0) {
         this.mainCannonCooldown--;
      }

      if (this.machineGunCooldown > 0) {
         this.machineGunCooldown--;
      }
   }

   public boolean isCharging() {
      return (Boolean)this.entityData.get(IS_CHARGING);
   }

   private void addMovementParticles() {
      Vec3 deltaMovement = this.getDeltaMovement();
      double speed = deltaMovement.horizontalDistance();
      if (speed > 0.02) {
         float intensity = (float)Math.min(speed * 2.0, 1.0);
         if (this.random.nextInt(2) == 0) {
            this.addTreadDustParticles(intensity);
         }

         if (this.random.nextInt(3) == 0) {
            this.addExhaustSmoke(intensity);
         }

         if (speed > 0.1 && this.random.nextInt(4) == 0) {
            this.addGroundImpactParticles(intensity);
         }

         if (speed > 0.03 && this.random.nextInt(8) == 0) {
            this.addMechanicalSparks();
         }
      }
   }

   private void addTreadDustParticles(float intensity) {
      double leftX = this.getX() - 1.5 * Math.cos(Math.toRadians((double)(this.getYRot() + 90.0F)));
      double leftZ = this.getZ() - 1.5 * Math.sin(Math.toRadians((double)(this.getYRot() + 90.0F)));

      for (int i = 0; i < (int)(3.0F * intensity); i++) {
         double offsetX = this.random.nextGaussian() * 0.3;
         double offsetZ = this.random.nextGaussian() * 0.3;
         this.level()
            .addParticle(
               ParticleTypes.POOF,
               leftX + offsetX,
               this.getY() + 0.1,
               leftZ + offsetZ,
               this.random.nextGaussian() * 0.1,
               this.random.nextDouble() * 0.1,
               this.random.nextGaussian() * 0.1
            );
      }

      double rightX = this.getX() + 1.5 * Math.cos(Math.toRadians((double)(this.getYRot() + 90.0F)));
      double rightZ = this.getZ() + 1.5 * Math.sin(Math.toRadians((double)(this.getYRot() + 90.0F)));

      for (int i = 0; i < (int)(3.0F * intensity); i++) {
         double offsetX = this.random.nextGaussian() * 0.3;
         double offsetZ = this.random.nextGaussian() * 0.3;
         this.level()
            .addParticle(
               ParticleTypes.POOF,
               rightX + offsetX,
               this.getY() + 0.1,
               rightZ + offsetZ,
               this.random.nextGaussian() * 0.1,
               this.random.nextDouble() * 0.1,
               this.random.nextGaussian() * 0.1
            );
      }

      if (this.random.nextInt(3) == 0) {
         this.level()
            .addParticle(
               ParticleTypes.CLOUD,
               this.getX() + this.random.nextGaussian() * 2.0,
               this.getY() + 0.2,
               this.getZ() + this.random.nextGaussian() * 2.0,
               0.0,
               0.05,
               0.0
            );
      }
   }

   private void addExhaustSmoke(float intensity) {
      double backX = this.getX() - 2.0 * Math.cos(Math.toRadians((double)this.getYRot()));
      double backZ = this.getZ() - 2.0 * Math.sin(Math.toRadians((double)this.getYRot()));

      for (int i = 0; i < (int)(2.0F * intensity); i++) {
         this.level()
            .addParticle(
               ParticleTypes.SMOKE,
               backX + this.random.nextGaussian() * 0.5,
               this.getY() + 2.0 + this.random.nextDouble() * 0.5,
               backZ + this.random.nextGaussian() * 0.5,
               this.random.nextGaussian() * 0.02,
               0.05 + this.random.nextDouble() * 0.03,
               this.random.nextGaussian() * 0.02
            );
      }

      if (this.random.nextInt(5) == 0) {
         this.level().addParticle(ParticleTypes.LARGE_SMOKE, backX, this.getY() + 2.2, backZ, 0.0, 0.08, 0.0);
      }
   }

   private void addGroundImpactParticles(float intensity) {
      for (int i = 0; i < (int)(4.0F * intensity); i++) {
         double offsetX = this.random.nextGaussian() * 1.5;
         double offsetZ = this.random.nextGaussian() * 1.5;
         this.level()
            .addParticle(
               ParticleTypes.CLOUD,
               this.getX() + offsetX,
               this.getY(),
               this.getZ() + offsetZ,
               this.random.nextGaussian() * 0.1,
               this.random.nextDouble() * 0.2,
               this.random.nextGaussian() * 0.1
            );
      }

      if (this.random.nextInt(6) == 0) {
         this.level()
            .addParticle(
               ParticleTypes.CAMPFIRE_COSY_SMOKE,
               this.getX() + this.random.nextGaussian() * 2.0,
               this.getY() + 0.1,
               this.getZ() + this.random.nextGaussian() * 2.0,
               0.0,
               0.03,
               0.0
            );
      }
   }

   private void addMechanicalSparks() {
      double sparkX = this.getX() + this.random.nextGaussian() * 2.0;
      double sparkZ = this.getZ() + this.random.nextGaussian() * 2.0;
      if (this.random.nextInt(3) == 0) {
         this.level()
            .addParticle(
               ParticleTypes.ELECTRIC_SPARK,
               sparkX,
               this.getY() + 1.0,
               sparkZ,
               this.random.nextGaussian() * 0.1,
               this.random.nextDouble() * 0.15,
               this.random.nextGaussian() * 0.1
            );
      }
   }

   private void fireMainCannon(LivingEntity target) {
      double turretHeight = (double)this.getBbHeight() * 1.2;
      double turretForwardOffset = 2.0;
      double spawnX = this.getX() + Math.cos(Math.toRadians((double)(this.getYRot() + 90.0F))) * turretForwardOffset;
      double spawnY = this.getY() + turretHeight;
      double spawnZ = this.getZ() + Math.sin(Math.toRadians((double)(this.getYRot() + 90.0F))) * turretForwardOffset;
      ScampRocketEntity rocket = new ScampRocketEntity((EntityType<? extends ScampRocketEntity>)ModEntities.SCAMP_ROCKET.get(), this.level(), this);
      rocket.setPos(spawnX, spawnY, spawnZ);
      rocket.setDamage(5.0);
      rocket.setExplosionRadius(3.5F);
      double dx = target.getX() + target.getDeltaMovement().x * 10.0 - spawnX;
      double dy = target.getEyeY() - spawnY;
      double dz = target.getZ() + target.getDeltaMovement().z * 10.0 - spawnZ;
      rocket.shoot(dx, dy, dz, 2.6F, 1.0F);
      this.level().addFreshEntity(rocket);
      this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.2F, 0.7F);
      this.triggerMainTurretFlash();
   }

   private void fireMachineGun(LivingEntity target) {
      double machineGunHeight = (double)this.getBbHeight() * 0.8;
      double machineGunOffset = 1.0;
      double spawnX = this.getX() + Math.cos(Math.toRadians((double)(this.getYRot() + 90.0F))) * machineGunOffset;
      double spawnY = this.getY() + machineGunHeight;
      double spawnZ = this.getZ() + Math.sin(Math.toRadians((double)(this.getYRot() + 90.0F))) * machineGunOffset;
      EnemyProjectileEntity bolt = new EnemyProjectileEntity(this.level(), this);
      bolt.setPos(spawnX, spawnY, spawnZ);
      double dx = target.getX() - spawnX;
      double dy = target.getEyeY() - spawnY;
      double dz = target.getZ() - spawnZ;
      bolt.shoot(dx, dy, dz, 3.0F, 1.5F);
      this.level().addFreshEntity(bolt);
      this.level()
         .playSound(null, this.getX(), this.getY(), this.getZ(), (SoundEvent)ModSounds.BRUISER_SILENCED_FIRE.get(), SoundSource.HOSTILE, 0.8F, 1.2F);
      this.triggerMachineGunFlash();
   }

   private int countNearbySkyCatriers() {
      if (this.level().isClientSide) {
         return 0;
      } else {
         AABB searchArea = new AABB(
            this.getX() - 40.0, this.getY() - 20.0, this.getZ() - 40.0, this.getX() + 40.0, this.getY() + 40.0, this.getZ() + 40.0
         );
         List<SkyCarrierEntity> skyCarriers = this.level().getEntitiesOfClass(SkyCarrierEntity.class, searchArea);
         return skyCarriers.size();
      }
   }

   private void shootBeaconProjectile() {
      if (!this.level().isClientSide) {
         LivingEntity target = this.getTarget();
         Vec3 targetDirection;
         if (target != null) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            double angle = Math.atan2(dz, dx) + (this.random.nextDouble() - 0.5) * Math.PI * 0.5;
            double distance = 15.0 + this.random.nextDouble() * 10.0;
            targetDirection = new Vec3(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
         } else {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double distance = 10.0 + this.random.nextDouble() * 20.0;
            targetDirection = new Vec3(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
         }

         BeaconProjectileEntity beaconProjectile = new BeaconProjectileEntity(
            (EntityType<? extends BeaconProjectileEntity>)ModEntities.BEACON_PROJECTILE.get(), this.level(), this
         );
         double launchX = this.getX();
         double launchY = this.getY() + (double)this.getBbHeight() + 1.0;
         double launchZ = this.getZ();
         beaconProjectile.setPos(launchX, launchY, launchZ);
         Vec3 landingPos = this.position().add(targetDirection);
         beaconProjectile.setLandingTarget(landingPos.x, landingPos.z);
         double dx = landingPos.x - launchX;
         double dz = landingPos.z - launchZ;
         double distance = Math.sqrt(dx * dx + dz * dz);
         double launchVelocity = 1.2;
         double launchAngle = Math.PI / 6;
         Vec3 launchVector = new Vec3(
            dx / distance * launchVelocity * Math.cos(launchAngle),
            launchVelocity * Math.sin(launchAngle),
            dz / distance * launchVelocity * Math.cos(launchAngle)
         );
         beaconProjectile.setDeltaMovement(launchVector);
         this.level().addFreshEntity(beaconProjectile);
         this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.DISPENSER_LAUNCH, SoundSource.HOSTILE, 1.5F, 0.8F);
         if (this.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 10; i++) {
               serverLevel.sendParticles(ParticleTypes.SMOKE, launchX, launchY, launchZ, 1, 0.3, 0.1, 0.3, 0.1);
            }
         }
      }
   }

   private boolean shouldSpawnBeacon() {
      if (this.beaconSpawnCooldown > 0) {
         return false;
      } else {
         return this.countNearbySkyCatriers() >= 4 ? false : this.random.nextFloat() < 0.3F;
      }
   }

   private void spawnSupplyBeacons() {
      if (!this.level().isClientSide) {
         if (this.shouldSpawnBeacon()) {
            this.shootBeaconProjectile();
            this.beaconSpawnCooldown = 60;
         }
      }
   }

   public void triggerMainTurretFlash() {
      this.entityData.set(MAIN_TURRET_FLASH_TIMER, 4);
   }

   public void triggerMachineGunFlash() {
      this.entityData.set(MACHINE_GUN_FLASH_TIMER, 4);
   }

   public boolean isMainTurretFlashVisible() {
      return (Integer)this.entityData.get(MAIN_TURRET_FLASH_TIMER) > 0;
   }

   public boolean isMachineGunFlashVisible() {
      return (Integer)this.entityData.get(MACHINE_GUN_FLASH_TIMER) > 0;
   }

   public void performRangedAttack(@NotNull LivingEntity target, float distanceFactor) {
   }

   @NotNull
   public MobType getMobType() {
      return MobType.UNDEFINED;
   }

   @Nullable
   protected SoundEvent getAmbientSound() {
      return SoundEvents.IRON_GOLEM_STEP;
   }

   @Nullable
   protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) {
      return SoundEvents.IRON_GOLEM_HURT;
   }

   @Nullable
   protected SoundEvent getDeathSound() {
      return SoundEvents.IRON_GOLEM_DEATH;
   }

   private class ExtendedRangeTargetGoal extends Goal {
      private int targetSearchDelay = 0;

      public ExtendedRangeTargetGoal() {
         super();
         this.setFlags(EnumSet.of(Flag.TARGET));
      }

      public boolean canUse() {
         if (--this.targetSearchDelay <= 0) {
            this.targetSearchDelay = 20;
            Player target = this.findNearestPlayer();
            if (target != null) {
               ScampTankEntity.this.setTarget(target);
               return true;
            }
         }

         return false;
      }

      public void start() {
         Player target = this.findNearestPlayer();
         ScampTankEntity.this.setTarget(target);
      }

      private Player findNearestPlayer() {
         AABB searchBox = ScampTankEntity.this.getBoundingBox().inflate(50.0);
         List<Player> players = ScampTankEntity.this.level().getEntitiesOfClass(Player.class, searchBox);
         Player closest = null;
         double closestDistance = 2500.0;

         for (Player player : players) {
            if (!player.isCreative() && !player.isSpectator()) {
               double distance = ScampTankEntity.this.distanceToSqr(player);
               if (distance < closestDistance) {
                  closest = player;
                  closestDistance = distance;
               }
            }
         }

         return closest;
      }
   }

   private class TankChargeGoal extends Goal {
      public TankChargeGoal() {
         super();
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         if (!ScampTankEntity.this.isAlive()) {
            return false;
         } else if (!ScampTankEntity.this.isInSecondPhase() || ScampTankEntity.this.isInThirdPhase()) {
            return false;
         } else if (ScampTankEntity.this.chargeCooldown > 0) {
            return false;
         } else if (ScampTankEntity.this.chargeWarmupTicks > 0 || ScampTankEntity.this.chargeActiveTicks > 0) {
            return false;
         } else if (ScampTankEntity.this.postChargeRotationTicks > 0) {
            return false;
         } else {
            LivingEntity target = ScampTankEntity.this.getTarget();
            if (target != null && target.isAlive()) {
               double distance = (double)ScampTankEntity.this.distanceTo(target);
               return distance <= 45.0 && ScampTankEntity.this.hasLineOfSight(target);
            } else {
               return false;
            }
         }
      }

      public boolean canContinueToUse() {
         return !ScampTankEntity.this.isAlive()
            ? false
            : ScampTankEntity.this.chargeWarmupTicks > 0 || ScampTankEntity.this.chargeActiveTicks > 0 || ScampTankEntity.this.postChargeRotationTicks > 0;
      }

      public void start() {
         LivingEntity target = ScampTankEntity.this.getTarget();
         if (target != null) {
            Vec3 targetVelocity = target.getDeltaMovement();
            double predictionTime = 1.25;
            double predictedX = target.getX() + targetVelocity.x * predictionTime * 20.0;
            double predictedZ = target.getZ() + targetVelocity.z * predictionTime * 20.0;
            double dx = predictedX - ScampTankEntity.this.getX();
            double dz = predictedZ - ScampTankEntity.this.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 0.0) {
               ScampTankEntity.this.chargeDirection = new Vec3(dx / distance, 0.0, dz / distance);
               float yaw = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI) - 90.0);
               ScampTankEntity.this.setYRot(yaw);
               ScampTankEntity.this.yBodyRot = yaw;
               ScampTankEntity.this.chargeWarmupTicks = 20;
               ScampTankEntity.this.level()
                  .playSound(
                     null,
                     ScampTankEntity.this.getX(),
                     ScampTankEntity.this.getY(),
                     ScampTankEntity.this.getZ(),
                     SoundEvents.PISTON_EXTEND,
                     SoundSource.HOSTILE,
                     2.0F,
                     0.5F
                  );
            }
         }
      }

      public void tick() {
         ScampTankEntity.this.getNavigation().stop();
      }

      public void stop() {
         ScampTankEntity.this.entityData.set(ScampTankEntity.IS_CHARGING, false);
      }
   }

   private class TankChaseGoal extends Goal {
      private int pathUpdateTimer = 0;
      private double targetX;
      private double targetZ;
      private boolean hasDestination = false;

      public TankChaseGoal() {
         super();
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         if (!ScampTankEntity.this.isAlive()) {
            return false;
         } else if (ScampTankEntity.this.isInThirdPhase()) {
            return true;
         } else if (!ScampTankEntity.this.isInSecondPhase() && ScampTankEntity.this.postChargeRotationTicks <= 0) {
            LivingEntity target = ScampTankEntity.this.getTarget();
            if (target != null && target.isAlive()) {
               double distance = (double)ScampTankEntity.this.distanceTo(target);
               return distance > 4.0 || 0 > 0;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }

      public boolean canContinueToUse() {
         if (!ScampTankEntity.this.isAlive()) {
            return false;
         } else if (ScampTankEntity.this.isInThirdPhase()) {
            return this.hasDestination && !ScampTankEntity.this.getNavigation().isDone();
         } else if (!ScampTankEntity.this.isInSecondPhase() && ScampTankEntity.this.postChargeRotationTicks <= 0) {
            LivingEntity target = ScampTankEntity.this.getTarget();
            return target != null && target.isAlive();
         } else {
            return false;
         }
      }

      public void start() {
         this.pathUpdateTimer = 0;
         this.calculateDestination();
      }

      public void tick() {
         LivingEntity target = ScampTankEntity.this.getTarget();
         if (target != null) {
            this.pathUpdateTimer++;
            if (ScampTankEntity.this.isAggressivelyRepositioning) {
               if (this.pathUpdateTimer % 10 == 0) {
                  this.calculateAggressiveDestination(target);
               }
            } else if (this.pathUpdateTimer >= 40) {
               this.pathUpdateTimer = 0;
               this.calculateDestination();
            }

            if (this.hasDestination) {
               double distToDestination = Math.sqrt(
                  Math.pow(ScampTankEntity.this.getX() - this.targetX, 2.0) + Math.pow(ScampTankEntity.this.getZ() - this.targetZ, 2.0)
               );
               if (distToDestination < 3.0) {
                  this.hasDestination = false;
                  if (ScampTankEntity.this.isAggressivelyRepositioning && ScampTankEntity.this.hasCleanLineOfSight(target)) {
                     ScampTankEntity.this.isAggressivelyRepositioning = false;
                  }
               }
            }
         }
      }

      private void calculateDestination() {
         LivingEntity target = ScampTankEntity.this.getTarget();
         if (target != null) {
            double distance = (double)ScampTankEntity.this.distanceTo(target);
            if (distance > 15.0) {
               double dx = target.getX() - ScampTankEntity.this.getX();
               double dz = target.getZ() - ScampTankEntity.this.getZ();
               double length = Math.sqrt(dx * dx + dz * dz);
               if (length > 0.0) {
                  double targetDistance = 12.0;
                  this.targetX = target.getX() - dx / length * targetDistance;
                  this.targetZ = target.getZ() - dz / length * targetDistance;
                  ScampTankEntity.this.getNavigation().moveTo(this.targetX, target.getY(), this.targetZ, 0.8);
                  this.hasDestination = true;
               }
            } else if (distance < 4.0) {
               double dx = ScampTankEntity.this.getX() - target.getX();
               double dz = ScampTankEntity.this.getZ() - target.getZ();
               double length = Math.sqrt(dx * dx + dz * dz);
               if (length > 0.0) {
                  this.targetX = ScampTankEntity.this.getX() + dx / length * 8.0;
                  this.targetZ = ScampTankEntity.this.getZ() + dz / length * 8.0;
                  ScampTankEntity.this.getNavigation().moveTo(this.targetX, target.getY(), this.targetZ, 0.6);
                  this.hasDestination = true;
               }
            }
         }
      }

      private void calculateAggressiveDestination(LivingEntity target) {
         double angle = Math.atan2(target.getZ() - ScampTankEntity.this.getZ(), target.getX() - ScampTankEntity.this.getX());
         double[] angleOffsets = new double[]{Math.PI / 3, -Math.PI / 3, Math.PI / 2, -Math.PI / 2, Math.PI * 2.0 / 3.0, -Math.PI * 2.0 / 3.0};

         for (double offset : angleOffsets) {
            double testAngle = angle + offset;
            double testDistance = 12.0;
            this.targetX = target.getX() + Math.cos(testAngle) * testDistance;
            this.targetZ = target.getZ() + Math.sin(testAngle) * testDistance;
            BlockPos testPos = new BlockPos((int)this.targetX, (int)ScampTankEntity.this.getY(), (int)this.targetZ);
            if (ScampTankEntity.this.level().getBlockState(testPos).isAir()) {
               ScampTankEntity.this.getNavigation().moveTo(this.targetX, target.getY(), this.targetZ, 1.2);
               this.hasDestination = true;
               break;
            }
         }
      }

      public void stop() {
         ScampTankEntity.this.getNavigation().stop();
         this.hasDestination = false;
      }
   }

   private class TankLookGoal extends Goal {
      private float targetYaw = 0.0F;
      private static final float MAX_ROTATION_SPEED = 4.0F;

      public TankLookGoal() {
         super();
         this.setFlags(EnumSet.of(Flag.LOOK));
      }

      public boolean canUse() {
         return !ScampTankEntity.this.isAlive()
            ? false
            : ScampTankEntity.this.getTarget() != null && !ScampTankEntity.this.isInSecondPhase() && ScampTankEntity.this.postChargeRotationTicks <= 0 && 0 <= 0;
      }

      public void tick() {
         LivingEntity target = ScampTankEntity.this.getTarget();
         if (target != null) {
            double dx = target.getX() - ScampTankEntity.this.getX();
            double dz = target.getZ() - ScampTankEntity.this.getZ();
            this.targetYaw = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI) - 90.0);
            float currentYaw = ScampTankEntity.this.getYRot();
            float yawDiff = this.targetYaw - currentYaw;

            while (yawDiff > 180.0F) {
               yawDiff -= 360.0F;
            }

            while (yawDiff < -180.0F) {
               yawDiff += 360.0F;
            }

            float rotation = Math.signum(yawDiff) * Math.min(Math.abs(yawDiff), 4.0F);
            float newYaw = currentYaw + rotation;
            if (Math.abs(yawDiff) > 1.0F) {
               ScampTankEntity.this.setYRot(newYaw);
               ScampTankEntity.this.yBodyRot = newYaw;
               ScampTankEntity.this.setYHeadRot(newYaw);
            }
         }
      }
   }
}
