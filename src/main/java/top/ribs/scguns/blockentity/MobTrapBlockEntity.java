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
   /** Mobs that spawn for the first player, plus this many per extra player. */
   private static final int BASE_MOBS = 4;
   private static final int MOBS_PER_EXTRA_PLAYER = 1;
   /** How many spawned mobs may be alive at the same time. */
   private static final int BASE_SIMULTANEOUS = 2;
   private static final int SIMULTANEOUS_PER_EXTRA_PLAYER = 1;
   private static final int MAX_STORED_MOBS = 20;

   private final List<EntityType<?>> storedMobs = new ArrayList<>();
   /** Mobs this trap has already released, used to tell whether the trap is cleared. */
   private final List<UUID> spawnedMobs = new ArrayList<>();

   private boolean active = false;
   private int totalToSpawn = 0;
   private int spawnedCount = 0;
   private int spawnedTypesCursor = 0;
   private int spawnTimer = 0;
   private int idleTimer = 0;

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

      if (this.idleTimer > 0) {
         this.idleTimer--;
      }

      List<Player> players = this.playersNearby(serverLevel, pos);

      if (!this.active) {
         if (this.storedMobs.isEmpty() || players.isEmpty()) {
            return;
         }

         this.begin(players.size());
         return;
      }

      if (players.isEmpty()) {
         // Nobody is fighting any more: hold the wave where it is instead of spending the trap.
         return;
      }

      this.cleanupDeadMobs(serverLevel);

      if (this.spawnedCount >= this.totalToSpawn && this.spawnedMobs.isEmpty()) {
         this.finish(serverLevel, pos);
         return;
      }

      if (this.spawnedCount >= this.totalToSpawn) {
         return;
      }

      if (this.spawnedMobs.size() >= this.simultaneousLimit(players.size())) {
         return;
      }

      if (this.spawnTimer > 0) {
         this.spawnTimer--;
         return;
      }

      this.spawnTimer = TICKS_BETWEEN_SPAWNS;
      this.spawnOne(serverLevel, pos);
   }

   /** Wakes the trap early, for the player who walks straight onto it. */
   public void wake() {
      if (!this.active && !this.storedMobs.isEmpty()) {
         this.begin(1);
      }
   }

   /** Wakes the trap and works out how much it is going to spawn for the players present. */
   private void begin(int playerCount) {
      this.active = true;
      this.spawnedCount = 0;
      this.spawnedTypesCursor = 0;
      this.spawnTimer = 0;
      this.spawnedMobs.clear();
      this.totalToSpawn = BASE_MOBS + Math.max(0, playerCount - 1) * MOBS_PER_EXTRA_PLAYER;
      this.setChanged();
   }

   private List<Player> playersNearby(ServerLevel level, BlockPos pos) {
      AABB detectionBox = new AABB(
         (double) pos.getX() - DETECTION_RADIUS,
         (double) pos.getY() - DETECTION_HEIGHT,
         (double) pos.getZ() - DETECTION_RADIUS,
         (double) pos.getX() + DETECTION_RADIUS,
         (double) pos.getY() + DETECTION_HEIGHT,
         (double) pos.getZ() + DETECTION_RADIUS
      );

      return level.getEntitiesOfClass(Player.class, detectionBox,
         player -> !player.isCreative() && !player.isSpectator() && player.isAlive());
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

      EntityType<?> type = this.storedMobs.get(this.spawnedTypesCursor % this.storedMobs.size());
      this.spawnedTypesCursor++;
      Entity entity = type.create(level);
      if (entity == null) {
         // A type that cannot be created would otherwise leave the trap waiting for a mob that never arrives.
         this.spawnedCount++;
         return;
      }

      double x = (double) pos.getX() + 0.5 + Mth.nextDouble(level.random, -2.0, 2.0);
      double y = (double) pos.getY();
      double z = (double) pos.getZ() + 0.5 + Mth.nextDouble(level.random, -2.0, 2.0);
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

      for (int i = 0; i < 12; i++) {
         level.sendParticles(ParticleTypes.SMALL_FLAME,
            entity.getX(), entity.getY() + 0.5, entity.getZ(), 1, 0.12, 0.18, 0.12, 0.0);
      }
   }

   /** Every mob is dead: the trap has done its job, so the block goes away. */
   private void finish(ServerLevel level, BlockPos pos) {
      this.active = false;
      this.storedMobs.clear();
      this.spawnedMobs.clear();
      this.spawnedCount = 0;
      this.totalToSpawn = 0;
      this.setChanged();
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
