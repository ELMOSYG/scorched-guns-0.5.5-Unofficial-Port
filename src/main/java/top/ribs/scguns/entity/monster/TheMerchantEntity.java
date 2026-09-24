package top.ribs.scguns.entity.monster;


import top.ribs.scguns.util.ScTrades;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.blockentity.EnemyTurretBlockEntity;
import top.ribs.scguns.config.MerchantTradeConfig;
import top.ribs.scguns.init.ModBlocks;

public class TheMerchantEntity extends PathfinderMob implements Merchant {
   private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(TheMerchantEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DESPAWN_TIMER = SynchedEntityData.defineId(TheMerchantEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DAMAGE_COUNT = SynchedEntityData.defineId(TheMerchantEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> WALKING_TO_SUMMONER = SynchedEntityData.defineId(TheMerchantEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> SUMMONED_BY_PACT = SynchedEntityData.defineId(TheMerchantEntity.class, EntityDataSerializers.BOOLEAN);
   private boolean summonedByPact = false;
   private static final int DEFAULT_DESPAWN_TIME = 5000;
   private static final int MAX_DAMAGE_BEFORE_VANISH = 3;
   private static final Logger LOGGER = LogManager.getLogger(TheMerchantEntity.class);
   private int teleportCooldown = 0;
   private static final int TELEPORT_COOLDOWN_TICKS = 3000;
   private static final double TELEPORT_RANGE = 8.0;
   private long tradesSeed = 0L;
   private boolean tradesInitialized = false;
   private UUID summonerUUID;
   @Nullable
   private Player tradingPlayer;
   private MerchantOffers offers = new MerchantOffers();

   public TheMerchantEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel, boolean summonedByPact) {
      super(pEntityType, pLevel);
      this.summonedByPact = summonedByPact;
      this.setSummonedByPact(summonedByPact);
      if (summonedByPact) {
         this.setDespawnTimer(5000);
         this.setDamageCount(0);
      }

      this.initializeTrades();
   }

   public TheMerchantEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
      this(pEntityType, pLevel, false);
   }

   private void initializeTrades() {
      if (!this.tradesInitialized) {
         if (this.tradesSeed == 0L) {
            this.tradesSeed = this.random.nextLong();
         }

         Random tradeRandom = new Random(this.tradesSeed);
         this.offers = MerchantTradeConfig.createRandomizedOffers(tradeRandom);
         this.tradesInitialized = true;
      }
   }

   @NotNull
   public InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
      if (this.level().isClientSide() || !this.isAlive()) {
         return InteractionResult.sidedSuccess(this.level().isClientSide());
      } else if (this.canTradeWith(player)) {
         this.setTradingPlayer(player);
         this.openTradingScreen(player, this.getDisplayName(), 1);
         return InteractionResult.sidedSuccess(this.level().isClientSide());
      } else {
         return InteractionResult.PASS;
      }
   }

   private boolean canTradeWith(Player player) {
      return true;
   }

   public void setTradingPlayer(@Nullable Player player) {
      this.tradingPlayer = player;
   }

   @Nullable
   public Player getTradingPlayer() {
      return this.tradingPlayer;
   }

   @NotNull
   public MerchantOffers getOffers() {
      return this.offers;
   }

   public void overrideOffers(@NotNull MerchantOffers offers) {
      this.offers = offers;
   }

   public void notifyTrade(MerchantOffer offer) {
      offer.increaseUses();
      this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SPIDER_AMBIENT, SoundSource.NEUTRAL, 1.2F, 0.6F);
      if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
         for (int i = 0; i < 8; i++) {
            double angle = (double)i * Math.PI * 2.0 / 8.0;
            double x = this.getX() + Math.cos(angle) * 1.5;
            double z = this.getZ() + Math.sin(angle) * 1.5;
            serverLevel.sendParticles(ParticleTypes.WITCH, x, this.getY() + 1.5, z, 1, 0.1, 0.1, 0.1, 0.02);
         }

         serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.0, this.getZ(), 3, 0.3, 0.3, 0.3, 0.01);
      }
   }

   public void notifyTradeUpdated(@NotNull ItemStack stack) {
   }

   public int getVillagerXp() {
      return 0;
   }

   public void overrideXp(int xp) {
   }

   public boolean showProgressBar() {
      return false;
   }

   @NotNull
   public SoundEvent getNotifyTradeSound() {
      return SoundEvents.SPIDER_STEP;
   }

   public boolean isClientSide() {
      return this.level().isClientSide();
   }

   public static Builder createAttributes() {
      return Animal.createLivingAttributes()
         .add(Attributes.MAX_HEALTH, 130.0)
         .add(Attributes.FOLLOW_RANGE, 24.0)
         .add(Attributes.MOVEMENT_SPEED, 0.25)
         .add(Attributes.ARMOR_TOUGHNESS, 0.5)
         .add(Attributes.ARMOR, 2.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
         .add(Attributes.ATTACK_KNOCKBACK, 0.0)
         .add(Attributes.ATTACK_DAMAGE, 0.0);
   }

   public boolean isSummonedByPact() {
      return (Boolean)this.entityData.get(SUMMONED_BY_PACT);
   }

   public void setSummonedByPact(boolean summoned) {
      this.entityData.set(SUMMONED_BY_PACT, summoned);
      this.summonedByPact = summoned;
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         if (this.isSummonedByPact()) {
            this.handleDespawnTimer();
            this.handleWalkingToSummoner();
            if (this.teleportCooldown > 0) {
               this.teleportCooldown--;
            }

            if (this.teleportCooldown <= 0 && this.isSuffocating()) {
               this.attemptEmergencyTeleport();
            }
         }

         if (!this.isWalkingToSummoner()) {
            this.handleLookAtNearbyPlayers();
         }
      }
   }

   public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
      if (!this.level().isClientSide() && this.isSummonedByPact()) {
         boolean isSuffocationDamage = pSource == this.damageSources().inWall() || pSource == this.damageSources().cramming();
         if (isSuffocationDamage && this.teleportCooldown <= 0 && this.attemptEmergencyTeleport()) {
            return false;
         }

         if (this.teleportCooldown <= 0 && this.random.nextFloat() < 0.7F) {
            this.attemptEmergencyTeleport();
         }

         int damageCount = this.getDamageCount() + 1;
         this.setDamageCount(damageCount);
         this.createDamageEffect();
         if (damageCount >= 3) {
            this.despawnWithSpiders();
            return false;
         }
      }

      return super.hurt(pSource, pAmount);
   }

   private boolean isSuffocating() {
      BlockPos pos = this.blockPosition();
      BlockState blockState = this.level().getBlockState(pos);
      BlockState blockStateAbove = this.level().getBlockState(pos.above());
      return !blockState.isAir() || !blockStateAbove.isAir();
   }

   private boolean attemptEmergencyTeleport() {
      Vec3 currentPos = this.position();
      ServerLevel serverLevel = (ServerLevel)this.level();

      for (int attempts = 0; attempts < 16; attempts++) {
         double angle = (double)attempts * Math.PI * 2.0 / 16.0;
         double distance = 3.0 + this.random.nextDouble() * 8.0;
         double newX = currentPos.x + Math.cos(angle) * distance;
         double newZ = currentPos.z + Math.sin(angle) * distance;

         for (int yOffset = 2; yOffset >= -3; yOffset--) {
            double newY = currentPos.y + (double)yOffset;
            if (this.isValidTeleportPosition(serverLevel, newX, newY, newZ)) {
               this.teleportTo(newX, newY, newZ);
               this.createTeleportEffect(currentPos);
               this.createTeleportEffect(this.position());
               this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.2F);
               this.teleportCooldown = 3000;
               return true;
            }
         }
      }

      return false;
   }

   private boolean isValidTeleportPosition(ServerLevel level, double x, double y, double z) {
      BlockPos spawnPos = new BlockPos((int)x, (int)y, (int)z);
      BlockPos headPos = spawnPos.above();
      if (level.getBlockState(spawnPos).isAir() && level.getBlockState(headPos).isAir()) {
         boolean hasGroundSupport = false;

         for (int checkY = (int)y; checkY >= (int)y - 3; checkY--) {
            BlockPos checkPos = new BlockPos((int)x, checkY, (int)z);
            if (level.getBlockState(checkPos).isSolidRender(level, checkPos)) {
               hasGroundSupport = true;
               break;
            }
         }

         return !level.getBlockState(spawnPos).liquid() && !level.getBlockState(spawnPos).is(BlockTags.FIRE) ? hasGroundSupport : false;
      } else {
         return false;
      }
   }

   private void createTeleportEffect(Vec3 position) {
      if (this.level() instanceof ServerLevel serverLevel) {
         for (int i = 0; i < 20; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 2.0;
            double offsetY = this.random.nextDouble() * 2.0;
            double offsetZ = (this.random.nextDouble() - 0.5) * 2.0;
            serverLevel.sendParticles(
               ParticleTypes.WITCH, position.x + offsetX, position.y + offsetY, position.z + offsetZ, 1, 0.1, 0.1, 0.1, 0.05
            );
         }

         for (int i = 0; i < 10; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 1.5;
            double offsetY = this.random.nextDouble() * 1.5;
            double offsetZ = (this.random.nextDouble() - 0.5) * 1.5;
            serverLevel.sendParticles(
               ParticleTypes.LARGE_SMOKE, position.x + offsetX, position.y + offsetY + 0.5, position.z + offsetZ, 1, 0.0, 0.05, 0.0, 0.02
            );
         }
      }
   }

   private void handleDespawnTimer() {
      int currentTimer = this.getDespawnTimer();
      if (currentTimer > 0) {
         this.setDespawnTimer(currentTimer - 1);
      } else {
         this.despawnPeacefully();
      }
   }

   private void despawnPeacefully() {
      this.createSmokeEffect();
      this.discard();
   }

   private void handleWalkingToSummoner() {
      if (this.isWalkingToSummoner()) {
         Player summoner = this.getSummoner();
         if (summoner != null) {
            double distance = (double)this.distanceTo(summoner);
            if (distance <= 3.0) {
               this.setWalkingToSummoner(false);
               this.getNavigation().stop();
               this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SPIDER_AMBIENT, SoundSource.NEUTRAL, 1.5F, 0.5F);
            } else {
               this.getNavigation().moveTo(summoner, 1.0);
            }
         } else {
            this.setWalkingToSummoner(false);
            this.getNavigation().stop();
         }
      }
   }

   private void handleLookAtNearbyPlayers() {
      Player target = this.getSummoner();
      if (target == null || target.distanceToSqr(this) > 64.0) {
         target = this.level().getNearestPlayer(this, 8.0);
      }

      if (target != null && target.distanceToSqr(this) <= 64.0) {
         Vec3 targetPos = target.getEyePosition();
         Vec3 merchantPos = this.getEyePosition();
         Vec3 lookVector = targetPos.subtract(merchantPos).normalize();
         double yaw = Math.atan2(-lookVector.x, lookVector.z) * (180.0 / Math.PI);
         double pitch = Math.asin(-lookVector.y) * (180.0 / Math.PI);
         float targetYaw = (float)yaw;
         float targetPitch = (float)Mth.clamp(pitch, -20.0, 20.0);
         float yawDiff = Mth.wrapDegrees(targetYaw - this.getYRot());
         float pitchDiff = targetPitch - this.getXRot();
         this.setYRot(this.getYRot() + Mth.clamp(yawDiff, -3.0F, 3.0F));
         this.setXRot(this.getXRot() + Mth.clamp(pitchDiff, -2.0F, 2.0F));
         this.yHeadRot = this.getYRot();
      }
   }

   private void createDamageEffect() {
      if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
         for (int i = 0; i < 15; i++) {
            double offsetX = this.random.nextDouble() - 0.5;
            double offsetY = this.random.nextDouble();
            double offsetZ = this.random.nextDouble() - 0.5;
            serverLevel.sendParticles(
               ParticleTypes.ANGRY_VILLAGER, this.getX() + offsetX, this.getY() + offsetY + 1.0, this.getZ() + offsetZ, 1, 0.0, 0.05, 0.0, 0.01
            );
         }

         for (int i = 0; i < 8; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 1.5;
            double offsetY = this.random.nextDouble() * 1.5;
            double offsetZ = (this.random.nextDouble() - 0.5) * 1.5;
            serverLevel.sendParticles(
               ParticleTypes.SMOKE, this.getX() + offsetX, this.getY() + offsetY + 1.0, this.getZ() + offsetZ, 1, 0.0, 0.05, 0.0, 0.01
            );
         }

         this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SPIDER_HURT, SoundSource.NEUTRAL, 1.5F, 0.4F);
      }
   }

   private void despawnWithSpiders() {
      this.spawnRevengeSpiders();
      this.spawnRevengeTurrets();
      this.createDespawnEffect();
      this.discard();
   }

   private void spawnRevengeTurrets() {
      if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
         int turretCount = 3;

         for (int i = 0; i < turretCount; i++) {
            double angle = (double)i * Math.PI * 2.0 / (double)turretCount;
            double distance = 3.0 + this.random.nextDouble() * 2.0;
            double x = this.getX() + Math.cos(angle) * distance;
            double z = this.getZ() + Math.sin(angle) * distance;
            BlockPos spawnPos = this.findSuitableGroundPosition(serverLevel, x, this.getY(), z);
            if (spawnPos != null) {
               BlockState turretState = ((Block)ModBlocks.ENEMY_TURRET.get()).defaultBlockState();
               serverLevel.setBlock(spawnPos, turretState, 3);
               if (serverLevel.getBlockEntity(spawnPos) instanceof EnemyTurretBlockEntity turretEntity) {
                  turretEntity.setDamageMultiplier(1.25F);
                  turretEntity.setFireRateMultiplier(0.5F);
               }

               this.createTurretSpawnEffect(serverLevel, spawnPos);
            }
         }

         this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.HOSTILE, 2.0F, 0.5F);
      }
   }

   private BlockPos findSuitableGroundPosition(ServerLevel level, double x, double y, double z) {
      BlockPos basePos = new BlockPos((int)x, (int)y, (int)z);

      for (int yOffset = -2; yOffset <= 4; yOffset++) {
         BlockPos testPos = basePos.offset(0, yOffset, 0);
         BlockPos groundPos = testPos.below();
         if (level.getBlockState(groundPos).isSolidRender(level, groundPos) && level.getBlockState(testPos).isAir() && level.getBlockState(testPos.above()).isAir()) {
            return testPos;
         }
      }

      return null;
   }

   private void createTurretSpawnEffect(ServerLevel serverLevel, BlockPos pos) {
      double x = (double)pos.getX() + 0.5;
      double y = (double)pos.getY() + 0.5;
      double z = (double)pos.getZ() + 0.5;

      for (int i = 0; i < 15; i++) {
         double offsetX = (this.random.nextDouble() - 0.5) * 1.5;
         double offsetY = this.random.nextDouble() * 2.0;
         double offsetZ = (this.random.nextDouble() - 0.5) * 1.5;
         serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, 0.1, 0.0, 0.05);
      }

      for (int i = 0; i < 8; i++) {
         double offsetX = this.random.nextDouble() - 0.5;
         double offsetY = this.random.nextDouble();
         double offsetZ = this.random.nextDouble() - 0.5;
         serverLevel.sendParticles(ParticleTypes.FLAME, x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, 0.05, 0.0, 0.02);
      }
   }

   private void spawnRevengeSpiders() {
      if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
         int spiderCount = 4 + this.random.nextInt(4);

         for (int i = 0; i < spiderCount; i++) {
            double angle = (double)i * Math.PI * 2.0 / (double)spiderCount;
            double distance = 2.0 + this.random.nextDouble() * 2.5;
            double x = this.getX() + Math.cos(angle) * distance;
            double z = this.getZ() + Math.sin(angle) * distance;
            double y = this.getY();
            BlockPos spawnPos = new BlockPos((int)x, (int)y, (int)z);

            for (int yOffset = -1; yOffset <= 2; yOffset++) {
               BlockPos testPos = spawnPos.offset(0, yOffset, 0);
               if (serverLevel.getBlockState(testPos).isAir() && serverLevel.getBlockState(testPos.below()).isSolidRender(serverLevel, testPos.below())) {
                  CaveSpider spider = new CaveSpider(EntityType.CAVE_SPIDER, serverLevel);
                  spider.setPos((double)testPos.getX() + 0.5, (double)testPos.getY(), (double)testPos.getZ() + 0.5);
                  spider.setTarget(this.level().getNearestPlayer(spider, 16.0));
                  if (serverLevel.addFreshEntity(spider)) {
                     serverLevel.sendParticles(ParticleTypes.SMOKE, spider.getX(), spider.getY() + 0.5, spider.getZ(), 5, 0.2, 0.2, 0.2, 0.02);
                  }
                  break;
               }
            }
         }

         this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.3F);
      }
   }

   private void createDespawnEffect() {
      if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
         for (int i = 0; i < 30; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 3.0;
            double offsetY = this.random.nextDouble() * 3.0;
            double offsetZ = (this.random.nextDouble() - 0.5) * 3.0;
            serverLevel.sendParticles(
               ParticleTypes.LARGE_SMOKE, this.getX() + offsetX, this.getY() + offsetY + 1.0, this.getZ() + offsetZ, 1, 0.0, 0.15, 0.0, 0.04
            );
         }

         for (int i = 0; i < 20; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 2.5;
            double offsetY = this.random.nextDouble() * 2.5;
            double offsetZ = (this.random.nextDouble() - 0.5) * 2.5;
            serverLevel.sendParticles(
               ParticleTypes.WITCH, this.getX() + offsetX, this.getY() + offsetY + 1.0, this.getZ() + offsetZ, 1, 0.0, 0.1, 0.0, 0.03
            );
         }

         this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SPIDER_DEATH, SoundSource.NEUTRAL, 1.8F, 0.3F);
      }
   }

   private void createSmokeEffect() {
      if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
         for (int i = 0; i < 20; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 2.0;
            double offsetY = this.random.nextDouble() * 2.0;
            double offsetZ = (this.random.nextDouble() - 0.5) * 2.0;
            serverLevel.sendParticles(
               ParticleTypes.LARGE_SMOKE, this.getX() + offsetX, this.getY() + offsetY + 1.0, this.getZ() + offsetZ, 1, 0.0, 0.1, 0.0, 0.02
            );
         }

         for (int i = 0; i < 10; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 1.5;
            double offsetY = this.random.nextDouble() * 1.5;
            double offsetZ = (this.random.nextDouble() - 0.5) * 1.5;
            serverLevel.sendParticles(
               ParticleTypes.SMOKE, this.getX() + offsetX, this.getY() + offsetY + 1.0, this.getZ() + offsetZ, 1, 0.0, 0.05, 0.0, 0.01
            );
         }

         for (int i = 0; i < 10; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 1.5;
            double offsetY = this.random.nextDouble() * 1.5;
            double offsetZ = (this.random.nextDouble() - 0.5) * 1.5;
            serverLevel.sendParticles(
               ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX() + offsetX, this.getY() + offsetY + 1.0, this.getZ() + offsetZ, 1, 0.0, 0.05, 0.0, 0.01
            );
         }

         this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.2F);
      }
   }

   public void createSpawnEffect() {
      if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
         for (int i = 0; i < 40; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 2.5;
            double offsetY = this.random.nextDouble() * 2.5;
            double offsetZ = (this.random.nextDouble() - 0.5) * 2.5;
            serverLevel.sendParticles(
               ParticleTypes.LARGE_SMOKE, this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ, 1, 0.0, 0.15, 0.0, 0.03
            );
         }

         for (int i = 0; i < 15; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 2.0;
            double offsetY = this.random.nextDouble() * 2.0;
            double offsetZ = (this.random.nextDouble() - 0.5) * 2.0;
            serverLevel.sendParticles(
               ParticleTypes.WITCH, this.getX() + offsetX, this.getY() + offsetY + 1.0, this.getZ() + offsetZ, 1, 0.0, 0.1, 0.0, 0.02
            );
         }

         this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 0.8F);
      }
   }

   public void setSummoner(Player player) {
      this.summonerUUID = player.getUUID();
   }

   @Nullable
   public Player getSummoner() {
      return this.summonerUUID != null && !this.level().isClientSide()
         ? Objects.requireNonNull(this.level().getServer()).getPlayerList().getPlayer(this.summonerUUID)
         : null;
   }

   public boolean isWalkingToSummoner() {
      return (Boolean)this.entityData.get(WALKING_TO_SUMMONER);
   }

   public void setWalkingToSummoner(boolean walking) {
      this.entityData.set(WALKING_TO_SUMMONER, walking);
   }

   public void setAttacking(boolean attacking) {
      this.entityData.set(ATTACKING, attacking);
   }

   public boolean isAttacking() {
      return (Boolean)this.entityData.get(ATTACKING);
   }

   public void setDespawnTimer(int timer) {
      this.entityData.set(DESPAWN_TIMER, timer);
   }

   public int getDespawnTimer() {
      return (Integer)this.entityData.get(DESPAWN_TIMER);
   }

   public void setDamageCount(int count) {
      this.entityData.set(DAMAGE_COUNT, count);
   }

   public int getDamageCount() {
      return (Integer)this.entityData.get(DAMAGE_COUNT);
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(ATTACKING, false);
      builder.define(DESPAWN_TIMER, 5000);
      builder.define(DAMAGE_COUNT, 0);
      builder.define(WALKING_TO_SUMMONER, false);
      builder.define(SUMMONED_BY_PACT, false);
   }

   protected void updateWalkAnimation(float pPartialTick) {
      float f;
      if (this.getPose() == Pose.STANDING) {
         f = Math.min(pPartialTick * 6.0F, 1.0F);
      } else {
         f = 0.0F;
      }

      this.walkAnimation.update(f, 0.2F);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(1, new FloatGoal(this));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
   }

   @Nullable
   protected SoundEvent getAmbientSound() {
      return SoundEvents.SPIDER_AMBIENT;
   }

   @Nullable
   protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) {
      return SoundEvents.SPIDER_HURT;
   }

   @Nullable
   protected SoundEvent getDeathSound() {
      return SoundEvents.SPIDER_DEATH;
   }

   protected float getSoundVolume() {
      return 1.0F;
   }

   public float getVoicePitch() {
      return 0.4F;
   }

   public boolean requiresCustomPersistence() {
      return true;
   }

   public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
      return false;
   }

   public void addAdditionalSaveData(CompoundTag compound) {
      super.addAdditionalSaveData(compound);
      compound.putBoolean("SummonedByPact", this.summonedByPact);
      compound.putLong("TradesSeed", this.tradesSeed);
      compound.putBoolean("TradesInitialized", this.tradesInitialized);
      ListTag offersTag = new ListTag();

      for (MerchantOffer offer : this.offers) {
         MerchantOffer.CODEC
            .encodeStart(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), offer)
            .result()
            .filter(tag -> tag instanceof CompoundTag)
            .ifPresent(offersTag::add);
      }

      compound.put("Offers", offersTag);
      if (this.summonerUUID != null) {
         compound.putUUID("SummonerUUID", this.summonerUUID);
      }
   }

   public void readAdditionalSaveData(CompoundTag compound) {
      super.readAdditionalSaveData(compound);
      this.summonedByPact = compound.getBoolean("SummonedByPact");
      this.setSummonedByPact(this.summonedByPact);
      if (compound.contains("TradesSeed")) {
         this.tradesSeed = compound.getLong("TradesSeed");
      }

      if (compound.contains("TradesInitialized")) {
         this.tradesInitialized = compound.getBoolean("TradesInitialized");
      }

      if (compound.contains("SummonerUUID")) {
         this.summonerUUID = compound.getUUID("SummonerUUID");
      }

      this.offers = new MerchantOffers();
      if (compound.contains("Offers", 9)) {
         ListTag offersTag = compound.getList("Offers", 10);

         for (int i = 0; i < offersTag.size(); i++) {
            try {
               CompoundTag offerTag = offersTag.getCompound(i);
               MerchantOffer offer = MerchantOffer.CODEC
                  .parse(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), offerTag)
                  .result()
                  .orElse(null);
               if (offer != null && !offer.getBaseCostA().isEmpty() && !offer.getResult().isEmpty()) {
                  this.offers.add(offer);
               } else {
                  LOGGER.warn("Skipping invalid offer at index {}: empty buy or sell item", i);
               }
            } catch (Exception var6) {
               LOGGER.error("Failed to load merchant offer at index {}: {}", i, var6.getMessage());
            }
         }
      }

      if (this.offers.isEmpty() && !this.tradesInitialized) {
         this.initializeTrades();
      }
   }
}
