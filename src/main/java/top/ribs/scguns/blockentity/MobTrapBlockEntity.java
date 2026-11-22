package top.ribs.scguns.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import top.ribs.scguns.init.ModBlockEntities;
import top.ribs.scguns.util.NbtHelper;

/**
 * A one-shot trap that spawns its stored mobs in waves and disappears only once every spawned mob is gone.
 *
 * <p>The block used to dump its whole contents on the first player who stepped on it, which turned every trap into
 * an instant swarm. It now behaves like a trial spawner: a survival or adventure player within the detection radius
 * wakes it up, mobs come one at a time while a small number of them may be alive at once, and the block is removed
 * when the last of them dies. Leaving the area simply pauses the spawn timer, so a trap is never wasted by walking
 * past it.</p>
 */
public class MobTrapBlockEntity extends BlockEntity {
   /** How far away a player can wake the trap up, in blocks. */
   private static final int DETECTION_RADIUS = 14;
   private static final int DETECTION_HEIGHT = 5;
   /** Ticks between two spawns. Twenty ticks is one second; forty matches a trial spawner. */
   private static final int TICKS_BETWEEN_SPAWNS = 40;
   /** How many spawned mobs may be alive at the same time. */
   private static final int BASE_SIMULTANEOUS = 2;
   private static final int SIMULTANEOUS_PER_EXTRA_PLAYER = 1;
   private static final int MAX_STORED_MOBS = 20;
   /** Ticks between spotting a player and the first spawn. */
   private static final int WAKE_DELAY = 40;
   /** Ticks between the last mob dying and the block disappearing. */
   private static final int FINISH_DELAY = 40;

   private final List<EntityType<?>> storedMobs = new ArrayList<>();
   /** Mobs this trap has already released, used to tell whether the trap is cleared. */
   private final List<UUID> spawnedMobs = new ArrayList<>();

   private boolean active = false;
   private int totalToSpawn = 0;
   private int spawnedCount = 0;
   private int spawnedTypesCursor = 0;
   private int spawnTimer = 0;
   private int idleTimer = 0;
   private int ambientTimer = 0;
   private int wakeTimer = 0;
   private int finishTimer = 0;

   public MobTrapBlockEntity(BlockPos pPos, BlockState pBlockState) {
      super((BlockEntityType)ModBlockEntities.MOB_TRAP.get(), pPos, pBlockState);
   }

   public boolean addMob(EntityType<?> mobType) {
      if (this.active) {
         return false;
      }

      if (this.storedMobs.size() < MAX_STORED_MOBS) {
         this.storedMobs.add(mobType);
         this.setChanged();
         return true;
      } else {
         return false;
      }
   }

   public List<EntityType<?>> getStoredMobs() {
      return this.storedMobs;
   }

   public boolean isActive() {
      return this.active;
   }

   public void serverTick(Level level, BlockPos pos) {
      if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
         return;
      }

      if (!top.ribs.scguns.Config.COMMON.gameplay.mobTrapSpawnsInWaves.get()) {
         this.legacyTick(serverLevel, pos);
         return;
      }

      // Detection phase: the trap waits a moment after spotting a player, so walking past does not immediately
      // start a fight behind the player's back.
      if (!this.active) {
         if (this.storedMobs.isEmpty()) {
            return;
         }

         if (this.wakeTimer > 0) {
            this.wakeTimer--;
            if (this.wakeTimer == 0) {
               this.begin();
            }

            return;
         }

         if (this.playersNearby(serverLevel, pos, true).isEmpty()) {
            return;
         }

         this.onDetected(serverLevel);
         return;
      }

      // Spawning phase: no more player checks. Once a trap has started it finishes what it was loaded with.
      this.cleanupDeadMobs(serverLevel);
      this.emitAmbientParticles(serverLevel, pos);

      if (this.spawnedCount >= this.totalToSpawn && this.spawnedMobs.isEmpty()) {
         this.finishTimer--;
         if (this.finishTimer <= 0) {
            this.finish(serverLevel, pos);
         }

         return;
      }

      if (this.spawnedCount >= this.totalToSpawn) {
         return;
      }

      if (this.spawnedMobs.size() >= this.simultaneousLimit(this.playersNearby(serverLevel, pos, false).size())) {
         return;
      }

      if (this.spawnTimer > 0) {
         this.spawnTimer--;
         return;
      }

      this.spawnTimer = TICKS_BETWEEN_SPAWNS;
      this.spawnOne(serverLevel, pos);
   }

   /**
    * The trap has spotted a player and starts charging.
    *
    * <p>The detect sound and the rising detection particles belong here, at the moment the trap notices someone,
    * not at the moment mobs start appearing - that is what makes the two-second delay readable as a warning.</p>
    */
   private void onDetected(ServerLevel level) {
      level.playSound(null, this.worldPosition, SoundEvents.TRIAL_SPAWNER_DETECT_PLAYER, SoundSource.BLOCKS, 1.0F, 1.0F);

      for (int i = 0; i < 16; i++) {
         double angle = level.random.nextDouble() * Math.PI * 2.0;
         double radius = 0.5 + level.random.nextDouble() * 0.6;
         level.sendParticles(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER,
            (double) this.worldPosition.getX() + 0.5 + Math.cos(angle) * radius,
            (double) this.worldPosition.getY() + 0.2 + level.random.nextDouble() * 0.9,
            (double) this.worldPosition.getZ() + 0.5 + Math.sin(angle) * radius,
            1, 0.0, 0.02, 0.0, 0.0);
      }

      this.wakeTimer = WAKE_DELAY;
      this.setChanged();
   }

   /** Wakes the trap early, for the player who walks straight onto it. */
   public void wake(ServerLevel level, BlockPos pos) {
      if (!top.ribs.scguns.Config.COMMON.gameplay.mobTrapSpawnsInWaves.get()) {
         this.releaseAll(level, pos);
         return;
      }

      if (!this.active && !this.storedMobs.isEmpty() && this.wakeTimer <= 0) {
         this.onDetected(level);
      }
   }

   /** Original behaviour: wait for a player, then dump the whole list and vanish. */
   private void legacyTick(ServerLevel level, BlockPos pos) {
      if (this.storedMobs.isEmpty()) {
         return;
      }

      this.idleTimer = (this.idleTimer + 1) % 20;
      if (this.idleTimer != 0) {
         return;
      }

      if (!this.playersNearby(level, pos, false).isEmpty()) {
         this.releaseAll(level, pos);
      }
   }

   /** Spawns every stored mob at once and removes the block, the way 0.5.5 did. */
   private void releaseAll(ServerLevel level, BlockPos pos) {
      if (this.storedMobs.isEmpty()) {
         return;
      }

      for (EntityType<?> mob : this.storedMobs) {
         Entity entity = mob.create(level);
         if (entity != null) {
            entity.moveTo(
               (double) pos.getX() + level.random.nextDouble(),
               (double) pos.getY(),
               (double) pos.getZ() + level.random.nextDouble(),
               level.random.nextFloat() * 360.0F,
               0.0F
            );
            if (entity instanceof Mob mobEntity) {
               mobEntity.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
            }

            level.addFreshEntity(entity);
         }
      }

      this.storedMobs.clear();
      this.setChanged();
      level.destroyBlock(pos, true);
   }

   /**
    * Wakes the trap.
    *
    * <p>The number of mobs is whatever was loaded into the trap - addons and datapacks put a deliberate list in there,
    * so the trap must not invent a different total. Only the pacing is new: one mob at a time instead of the whole
    * list at once.</p>
    */
   private void begin() {
      this.active = true;
      this.spawnedCount = 0;
      this.spawnedTypesCursor = 0;
      this.spawnTimer = 0;
      this.spawnedMobs.clear();
      this.totalToSpawn = this.storedMobs.size();
      if (this.level instanceof ServerLevel serverLevel) {
         serverLevel.playSound(null, this.worldPosition, SoundEvents.TRIAL_SPAWNER_OPEN_SHUTTER, SoundSource.BLOCKS,
            1.0F, 1.0F);
      }

      this.wakeTimer = 0;
      this.finishTimer = FINISH_DELAY;
      this.setChanged();


   }

   private List<Player> playersNearby(ServerLevel level, BlockPos pos, boolean requireSight) {
      AABB detectionBox = new AABB(
         (double) pos.getX() - DETECTION_RADIUS,
         (double) pos.getY() - DETECTION_HEIGHT,
         (double) pos.getZ() - DETECTION_RADIUS,
         (double) pos.getX() + DETECTION_RADIUS,
         (double) pos.getY() + DETECTION_HEIGHT,
         (double) pos.getZ() + DETECTION_RADIUS
      );

      return level.getEntitiesOfClass(Player.class, detectionBox,
         player -> !player.isCreative() && !player.isSpectator() && player.isAlive()
            && (!requireSight || this.canSee(level, player)));
   }

   /**
    * Whether the trap can actually see this player.
    *
    * <p>Plain proximity let a trap on the other side of a wall wake up and start spawning, which is how players got
    * jumped by mobs that seemed to appear out of a solid block. A single block ray is enough to stop that.</p>
    */
   private boolean canSee(ServerLevel level, Player player) {
      net.minecraft.world.phys.Vec3 from = net.minecraft.world.phys.Vec3.atCenterOf(this.worldPosition)
         .add(0.0, 0.5, 0.0);
      net.minecraft.world.phys.Vec3 to = player.getEyePosition();
      net.minecraft.world.phys.BlockHitResult hit = level.clip(new net.minecraft.world.level.ClipContext(
         from, to,
         net.minecraft.world.level.ClipContext.Block.COLLIDER,
         net.minecraft.world.level.ClipContext.Fluid.NONE,
         player));
      return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
   }

   private int simultaneousLimit(int playerCount) {
      return BASE_SIMULTANEOUS + Math.max(0, playerCount - 1) * SIMULTANEOUS_PER_EXTRA_PLAYER;
   }

   /** Drops the record of every spawned mob that has died or left the world. */
   private void cleanupDeadMobs(ServerLevel level) {
      this.spawnedMobs.removeIf(uuid -> {
         Entity entity = level.getEntity(uuid);
         return entity == null || !entity.isAlive();
      });
   }

   private void spawnOne(ServerLevel level, BlockPos pos) {
      if (this.storedMobs.isEmpty()) {
         return;
      }

      EntityType<?> type = this.storedMobs.get(Math.min(this.spawnedTypesCursor, this.storedMobs.size() - 1));
      this.spawnedTypesCursor++;
      Entity entity = type.create(level);
      if (entity == null) {
         // A type that cannot be created would otherwise leave the trap waiting for a mob that never arrives.
         this.spawnedCount++;
         return;
      }

      BlockPos spawnPos = this.findSafeSpawn(level, pos, entity);
      double x = (double) spawnPos.getX() + 0.5;
      double y = (double) spawnPos.getY();
      double z = (double) spawnPos.getZ() + 0.5;
      entity.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);

      if (entity instanceof Mob mobEntity) {
         mobEntity.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
         // Spawner mobs must not quietly despawn: the trap only clears when they are actually dead.
         mobEntity.setPersistenceRequired();
      }

      level.addFreshEntity(entity);
      this.spawnedMobs.add(entity.getUUID());
      this.spawnedCount++;
      this.setChanged();

      level.playSound(null, pos, SoundEvents.TRIAL_SPAWNER_SPAWN_MOB, SoundSource.BLOCKS, 1.0F,
         (level.random.nextFloat() - level.random.nextFloat()) * 0.2F + 1.0F);

      for (int i = 0; i < 12; i++) {
         level.sendParticles(ParticleTypes.SMALL_FLAME,
            entity.getX(), entity.getY() + 0.5, entity.getZ(), 1, 0.12, 0.18, 0.12, 0.0);
      }
   }

   /** Rising flame particles while the trap is working, the visual cue a trial spawner gives. */
   private void emitAmbientParticles(ServerLevel level, BlockPos pos) {
      this.ambientTimer++;
      if (this.ambientTimer < 10) {
         return;
      }

      this.ambientTimer = 0;

      for (int i = 0; i < 3; i++) {
         double x = (double) pos.getX() + 0.5 + Mth.nextDouble(level.random, -0.35, 0.35);
         double y = (double) pos.getY() + 0.1 + level.random.nextDouble() * 1.1;
         double z = (double) pos.getZ() + 0.5 + Mth.nextDouble(level.random, -0.35, 0.35);
         level.sendParticles(ParticleTypes.SMALL_FLAME, x, y, z, 1, 0.0, 0.01, 0.0, 0.0);
      }
   }

   /**
    * Finds a spot around the trap where the mob fits: air for its full hitbox, and a solid floor underneath.
    *
    * <p>Spawning on a fixed offset used to push mobs into walls, where they stayed stuck and the trap could never
    * clear. Sixteen attempts cover the surrounding ring; the trap's own position is the fallback.</p>
    */
   private BlockPos findSafeSpawn(ServerLevel level, BlockPos pos, Entity entity) {
      for (int attempt = 0; attempt < 16; attempt++) {
         int dx = Mth.nextInt(level.random, -3, 3);
         int dz = Mth.nextInt(level.random, -3, 3);
         if (dx == 0 && dz == 0) {
            continue;
         }

         BlockPos candidate = pos.offset(dx, 0, dz);
         if (!level.isEmptyBlock(candidate) || !level.isEmptyBlock(candidate.above())) {
            continue;
         }

         net.minecraft.world.level.block.state.BlockState floor = level.getBlockState(candidate.below());
         if (floor.isAir() || floor.liquid()) {
            continue;
         }

         entity.moveTo((double) candidate.getX() + 0.5, (double) candidate.getY(), (double) candidate.getZ() + 0.5,
            entity.getYRot(), entity.getXRot());
         if (!level.noCollision(entity)) {
            continue;
         }

         return candidate;
      }

      return pos;
   }

   /** Every mob is dead: the trap has done its job, so the block goes away. */
   private void finish(ServerLevel level, BlockPos pos) {
      this.active = false;
      this.storedMobs.clear();
      this.spawnedMobs.clear();
      this.spawnedCount = 0;
      this.totalToSpawn = 0;
      this.setChanged();
      level.playSound(null, pos, SoundEvents.TRIAL_SPAWNER_CLOSE_SHUTTER, SoundSource.BLOCKS, 1.0F, 1.0F);

      for (int i = 0; i < 20; i++) {
         level.sendParticles(ParticleTypes.LARGE_SMOKE,
            (double) pos.getX() + 0.5 + Mth.nextDouble(level.random, -0.5, 0.5),
            (double) pos.getY() + 0.4 + Mth.nextDouble(level.random, 0.0, 0.8),
            (double) pos.getZ() + 0.5 + Mth.nextDouble(level.random, -0.5, 0.5),
            1, 0.02, 0.03, 0.02, 0.0);
      }

      level.playSound(null, pos, SoundEvents.TRIAL_SPAWNER_BREAK, SoundSource.BLOCKS, 0.8F, 0.9F);
      level.destroyBlock(pos, false);
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      this.storedMobs.clear();
      ListTag mobList = tag.getList("StoredMobs", 8);
      for (int i = 0; i < mobList.size(); i++) {
         String mobId = mobList.getString(i);
         EntityType<?> entityType = (EntityType<?>) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(mobId));
         if (entityType != null) {
            this.storedMobs.add(entityType);
         }
      }

      this.spawnedMobs.clear();
      ListTag spawnedUuids = tag.getList("SpawnedMobs", 8);
      for (int i = 0; i < spawnedUuids.size(); i++) {
         try {
            this.spawnedMobs.add(UUID.fromString(spawnedUuids.getString(i)));
         } catch (IllegalArgumentException ignored) {
            // A corrupt entry is simply forgotten; the trap clears on what is still alive.
         }
      }

      this.active = tag.getBoolean("Active");
      this.totalToSpawn = tag.getInt("TotalToSpawn");
      this.spawnedCount = tag.getInt("SpawnedCount");
      this.spawnedTypesCursor = tag.getInt("SpawnedTypesCursor");
      this.spawnTimer = tag.getInt("SpawnTimer");
      this.wakeTimer = tag.getInt("WakeTimer");
      this.finishTimer = tag.getInt("FinishTimer");
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      ListTag mobList = new ListTag();
      for (EntityType<?> mob : this.storedMobs) {
         mobList.add(StringTag.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(mob).toString()));
      }

      tag.put("StoredMobs", mobList);
      ListTag spawnedUuids = new ListTag();
      for (UUID uuid : this.spawnedMobs) {
         spawnedUuids.add(StringTag.valueOf(uuid.toString()));
      }

      tag.put("SpawnedMobs", spawnedUuids);
      tag.putBoolean("Active", this.active);
      tag.putInt("TotalToSpawn", this.totalToSpawn);
      tag.putInt("SpawnedCount", this.spawnedCount);
      tag.putInt("SpawnedTypesCursor", this.spawnedTypesCursor);
      tag.putInt("SpawnTimer", this.spawnTimer);
      tag.putInt("WakeTimer", this.wakeTimer);
      tag.putInt("FinishTimer", this.finishTimer);
   }

   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      CompoundTag tag = new CompoundTag();
      this.saveAdditional(tag, registries);
      return tag;
   }

   /** 1.20.1 entry point, kept for API compatibility; 1.21 callers pass the registries they already hold. */
   public void handleUpdateTag(CompoundTag tag) {
      this.handleUpdateTag(tag, this.level == null ? RegistryAccess.EMPTY : this.level.registryAccess());
   }

   public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
      this.loadAdditional(tag, registries);
   }

   @Nullable
   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
      this.handleUpdateTag(pkt.getTag(), registries);
   }

   /** 1.20.1 entry point, kept for API compatibility. */
   public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
      this.onDataPacket(net, pkt, this.level == null ? RegistryAccess.EMPTY : this.level.registryAccess());
   }
}
