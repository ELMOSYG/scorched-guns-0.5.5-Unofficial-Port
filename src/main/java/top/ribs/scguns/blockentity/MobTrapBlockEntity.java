package top.ribs.scguns.blockentity;




import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import top.ribs.scguns.util.NbtHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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

public class MobTrapBlockEntity extends BlockEntity {
   private final List<EntityType<?>> storedMobs = new ArrayList<>();
   private static final int MAX_MOBS = 20;
   private final int radius = 10;
   private final int heightRadius = 3;
   private int tickCounter = 0;

   public MobTrapBlockEntity(BlockPos pPos, BlockState pBlockState) {
      super((BlockEntityType)ModBlockEntities.MOB_TRAP.get(), pPos, pBlockState);
   }

   public boolean addMob(EntityType<?> mobType) {
      if (this.storedMobs.size() < 20) {
         this.storedMobs.add(mobType);
         this.setChanged();
         return true;
      } else {
         return false;
      }
   }

   public void releaseMobs(ServerLevel level, BlockPos pos) {
      if (!this.storedMobs.isEmpty()) {
         for (EntityType<?> mob : this.storedMobs) {
            Entity entity = mob.create(level);
            if (entity != null) {
               entity.moveTo(
                  (double)pos.getX() + level.random.nextDouble(),
                  (double)pos.getY(),
                  (double)pos.getZ() + level.random.nextDouble(),
                  level.random.nextFloat() * 360.0F,
                  0.0F
               );
               if (entity instanceof Mob mobEntity) {
                  mobEntity.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
               }

               level.addFreshEntity(entity);

               for (int i = 0; i < 20; i++) {
                  level.sendParticles(ParticleTypes.SMALL_FLAME, entity.getX(), entity.getY(), entity.getZ(), 1, 0.1, 0.1, 0.1, 0.02);
               }
            }
         }

         this.storedMobs.clear();
         this.setChanged();
         level.destroyBlock(pos, true);
      }
   }

   public List<EntityType<?>> getStoredMobs() {
      return this.storedMobs;
   }

   public void serverTick(Level level, BlockPos pos) {
      if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
         this.tickCounter++;
         if (this.tickCounter >= 20) {
            this.tickCounter = 0;
            AABB detectionBox = new AABB(
               (double)(pos.getX() - this.radius),
               (double)(pos.getY() - this.heightRadius),
               (double)(pos.getZ() - this.radius),
               (double)(pos.getX() + this.radius),
               (double)(pos.getY() + this.heightRadius),
               (double)(pos.getZ() + this.radius)
            );
            List<Player> players = serverLevel.getEntitiesOfClass(Player.class, detectionBox, player -> !player.isCreative() && !player.isSpectator());
            if (!players.isEmpty()) {
               this.releaseMobs(serverLevel, pos);
            }
         }
      }
   }

   public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      this.storedMobs.clear();
      ListTag mobList = tag.getList("StoredMobs", 8);

      for (int i = 0; i < mobList.size(); i++) {
         String mobId = mobList.getString(i);
         EntityType<?> entityType = (EntityType<?>)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(mobId));
         if (entityType != null) {
            this.storedMobs.add(entityType);
         }
      }
   }

   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      ListTag mobList = new ListTag();

      for (EntityType<?> mob : this.storedMobs) {
         mobList.add(StringTag.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(mob).toString()));
      }

      tag.put("StoredMobs", mobList);
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
