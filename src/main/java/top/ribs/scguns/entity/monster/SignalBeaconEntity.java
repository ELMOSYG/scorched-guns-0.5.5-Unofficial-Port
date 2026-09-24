package top.ribs.scguns.entity.monster;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.init.ModEntities;

public class SignalBeaconEntity extends Mob {
   private static final int LIFESPAN_TICKS = 100;
   private int lifespan;
   private boolean hasSpawnedCarriers = false;

   public SignalBeaconEntity(EntityType<? extends Mob> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.lifespan = 100;
   }

   public int getRemainingLifespan() {
      return this.lifespan;
   }

   public static Builder createAttributes() {
      return Animal.createLivingAttributes()
         .add(Attributes.MAX_HEALTH, 12.0)
         .add(Attributes.FOLLOW_RANGE, 0.0)
         .add(Attributes.ARMOR_TOUGHNESS, 0.1F)
         .add(Attributes.ATTACK_KNOCKBACK, 0.0)
         .add(Attributes.ATTACK_DAMAGE, 0.0);
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && --this.lifespan <= 0 && !this.hasSpawnedCarriers && !this.isDeadOrDying()) {
         this.hasSpawnedCarriers = true;
         this.spawnSkyCarriers();
         this.discard();
      }

      if (this.tickCount % 20 == 0) {
         this.level()
            .playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
      }
   }

   public void die(DamageSource pDamageSource) {
      this.hasSpawnedCarriers = true;
      super.die(pDamageSource);
   }

   private void spawnSkyCarriers() {
      if (this.level() instanceof ServerLevel serverLevel) {
         int var9 = 1 + this.random.nextInt(4);
         int successfulSpawns = 0;
         int maxAttempts = var9 * 5;
         Vec3 beaconPosition = this.position();

         for (int attempt = 0; attempt < maxAttempts && successfulSpawns < var9; attempt++) {
            Vec3 spawnPos = this.findValidSpawnPosition(serverLevel);
            if (spawnPos != null) {
               SkyCarrierEntity skyCarrier = (SkyCarrierEntity)((EntityType)ModEntities.SKY_CARRIER.get()).create(serverLevel);
               if (skyCarrier != null) {
                  skyCarrier.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, this.random.nextFloat() * 360.0F, 0.0F);
                  skyCarrier.setInitialTarget(beaconPosition);
                  skyCarrier.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 1.0);
                  serverLevel.addFreshEntity(skyCarrier);
                  serverLevel.sendParticles(ParticleTypes.CLOUD, skyCarrier.getX(), skyCarrier.getY(), skyCarrier.getZ(), 10, 0.5, 0.2, 0.2, 0.1);
                  serverLevel.playSound(
                     null, skyCarrier.getX(), skyCarrier.getY(), skyCarrier.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 1.0F
                  );
                  successfulSpawns++;
               }
            }
         }
      }
   }

   private Vec3 findValidSpawnPosition(ServerLevel serverLevel) {
      for (int radiusAttempt = 0; radiusAttempt < 3; radiusAttempt++) {
         double baseDistance = 20.0 + (double)radiusAttempt * 10.0;

         for (int positionAttempt = 0; positionAttempt < 8; positionAttempt++) {
            double distance = baseDistance + this.random.nextDouble() * 5.0;
            double angle = this.random.nextDouble() * 2.0 * Math.PI;
            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;

            for (int heightOffset = 10; heightOffset >= -5; heightOffset -= 3) {
               double spawnX = this.getX() + offsetX;
               double spawnY = this.getY() + (double)heightOffset;
               double spawnZ = this.getZ() + offsetZ;
               BlockPos spawnBlockPos = new BlockPos((int)spawnX, (int)spawnY, (int)spawnZ);
               if (this.isValidSpawnPosition(serverLevel, spawnBlockPos, spawnX, spawnY, spawnZ)) {
                  return new Vec3(spawnX, spawnY, spawnZ);
               }
            }
         }
      }

      return null;
   }

   private boolean isValidSpawnPosition(ServerLevel serverLevel, BlockPos blockPos, double exactX, double exactY, double exactZ) {
      AABB boundingBox = new AABB(exactX - 1.5, exactY - 1.0, exactZ - 1.5, exactX + 1.5, exactY + 2.0, exactZ + 1.5);

      for (BlockPos pos : BlockPos.betweenClosed(
         (int)boundingBox.minX,
         (int)boundingBox.minY,
         (int)boundingBox.minZ,
         (int)boundingBox.maxX,
         (int)boundingBox.maxY,
         (int)boundingBox.maxZ
      )) {
         if (!serverLevel.getBlockState(pos).isAir() && !serverLevel.getBlockState(pos).canBeReplaced() && serverLevel.getBlockState(pos).getBlock().defaultBlockState().blocksMotion()) {
            return false;
         }
      }

      BlockPos groundCheck = new BlockPos((int)exactX, (int)exactY - 5, (int)exactZ);

      for (int i = 0; i < 5 && serverLevel.getBlockState(groundCheck.below(i)).isAir(); i++) {
         if (i == 4) {
            return false;
         }
      }

      List<Entity> entitiesInArea = serverLevel.getEntitiesOfClass(Entity.class, boundingBox);
      return !entitiesInArea.isEmpty() ? false : serverLevel.getWorldBorder().isWithinBounds(blockPos);
   }
}
