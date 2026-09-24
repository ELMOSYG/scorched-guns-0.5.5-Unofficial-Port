package top.ribs.scguns.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.Config;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.compat.CompatManager;
import top.ribs.scguns.init.ModBlocks;
import top.ribs.scguns.init.ModDamageTypes;
import top.ribs.scguns.init.ModParticleTypes;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageProjectileHitEntity;
import top.ribs.scguns.util.GunEnchantmentHelper;

public class FireRoundEntity extends ProjectileEntity {
   private static final float SHIELD_IGNITE_CHANCE = 0.4F;
   private static final int SCULK_CLEARING_RADIUS = 2;

   public FireRoundEntity(EntityType<? extends Entity> entityType, Level worldIn) {
      super(entityType, worldIn);
   }

   public FireRoundEntity(EntityType<? extends Entity> entityType, Level worldIn, LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun) {
      super(entityType, worldIn, shooter, weapon, item, modifiedGun);
   }

   private boolean isSoulFireGun() {
      return this.getProjectile().isSoulFire();
   }

   @Override
   protected void onProjectileTick() {
      if (this.level().isClientSide && this.tickCount > 1 && this.tickCount < this.life) {
         boolean hideProjectile = this.getProjectile().shouldHideProjectile();
         if (hideProjectile) {
            this.spawnSprayStyleParticles();
         } else {
            this.spawnGlobStyleParticles();
         }
      }
   }

   private void spawnSprayStyleParticles() {
      if (this.tickCount % 2 == 0) {
         int particleCount = 1 + this.random.nextInt(2);

         for (int i = 0; i < particleCount; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 1.1;
            double offsetY = (this.random.nextDouble() - 0.5) * 1.1;
            double offsetZ = (this.random.nextDouble() - 0.5) * 1.1;
            double velocityX = (this.random.nextDouble() - 0.5) * 0.1;
            double velocityY = (this.random.nextDouble() - 0.5) * 0.1;
            double velocityZ = (this.random.nextDouble() - 0.5) * 0.1;
            if (this.isSoulFireGun()) {
               if (this.random.nextFloat() < 0.8F) {
                  this.level()
                     .addParticle(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        true,
                        this.getX() + offsetX * 0.8,
                        this.getY() + offsetY * 0.8,
                        this.getZ() + offsetZ * 0.8,
                        velocityX * 0.5,
                        velocityY * 0.5,
                        velocityZ * 0.5
                     );
               }

               if (this.random.nextFloat() < 0.4F) {
                  this.level()
                     .addParticle(
                        (ParticleOptions)ModParticleTypes.SOUL_FIREBALL.get(),
                        true,
                        this.getX() + offsetX * 0.6,
                        this.getY() + offsetY * 0.6,
                        this.getZ() + offsetZ * 0.6,
                        velocityX * 0.3,
                        velocityY * 0.3,
                        velocityZ * 0.3
                     );
               }
            } else {
               if (this.random.nextFloat() < 0.4F) {
                  this.level()
                     .addParticle(
                        (ParticleOptions)ModParticleTypes.FIREBALL.get(),
                        true,
                        this.getX() + offsetX * 0.6,
                        this.getY() + offsetY * 0.6,
                        this.getZ() + offsetZ * 0.6,
                        velocityX * 0.3,
                        velocityY * 0.3,
                        velocityZ * 0.3
                     );
               }

               if (this.random.nextFloat() < 0.6F) {
                  this.level()
                     .addParticle(
                        ParticleTypes.SMALL_FLAME,
                        true,
                        this.getX() + offsetX * 0.6,
                        this.getY() + offsetY * 0.6,
                        this.getZ() + offsetZ * 0.6,
                        velocityX * 0.3,
                        velocityY * 0.3,
                        velocityZ * 0.3
                     );
               }
            }
         }

         if (this.tickCount % 2 == 0) {
            int smokeCount = 1 + this.random.nextInt(1);

            for (int ix = 0; ix < smokeCount; ix++) {
               double offsetX = (this.random.nextDouble() - 0.5) * 0.6;
               double offsetY = (this.random.nextDouble() - 0.5) * 0.6;
               double offsetZ = (this.random.nextDouble() - 0.5) * 0.6;
               this.level()
                  .addParticle(ParticleTypes.SMOKE, true, this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ, 0.0, 0.05, 0.0);
            }
         }
      }
   }

   private void spawnGlobStyleParticles() {
      if (this.tickCount % 2 == 0) {
         double offsetX = (this.random.nextDouble() - 0.5) * 0.5;
         double offsetY = (this.random.nextDouble() - 0.5) * 0.5;
         double offsetZ = (this.random.nextDouble() - 0.5) * 0.5;
         if (this.isSoulFireGun()) {
            this.level()
               .addParticle(ParticleTypes.SOUL_FIRE_FLAME, true, this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ, 0.0, 0.0, 0.0);
         } else {
            this.level()
               .addParticle(ParticleTypes.FLAME, true, this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ, 0.0, 0.0, 0.0);
            this.level()
               .addParticle(ParticleTypes.LAVA, true, this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ, 0.0, 0.0, 0.0);
         }
      }

      if (this.tickCount % 6 == 0) {
         double offsetX = (this.random.nextDouble() - 0.5) * 0.5;
         double offsetY = (this.random.nextDouble() - 0.5) * 0.5;
         double offsetZ = (this.random.nextDouble() - 0.5) * 0.5;
         this.level().addParticle(ParticleTypes.SMOKE, true, this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ, 0.0, 0.0, 0.0);
      }
   }

   @Override
   protected void onHitEntity(Entity entity, Vec3 hitVec, Vec3 startVec, Vec3 endVec, boolean headshot) {
      float damage = this.getDamage();
      float newDamage = this.getCriticalDamage(this.getWeapon(), this.random, damage);
      boolean critical = damage != newDamage;
      ResourceLocation advantage = this.getProjectile().getAdvantage();
      damage = newDamage * this.advantageMultiplier(entity);
      if (headshot) {
         damage = (float)((double)damage * (Double)Config.COMMON.gameplay.headShotDamageMultiplier.get());
      }

      if (entity instanceof LivingEntity livingTarget) {
         damage = this.applyProjectileProtection(livingTarget, damage);
      }

      DamageSource source = ModDamageTypes.Sources.projectile(this.level().registryAccess(), this, (LivingEntity)this.getOwner());
      boolean blocked = ProjectileEntity.ProjectileHelper.handleShieldHit(entity, this, damage);
      if (blocked) {
         if (entity instanceof Player player && this.random.nextFloat() < 0.4F) {
            ItemStack shield = player.getUseItem();
            if (shield.getItem() instanceof ShieldItem) {
               player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.AIR));
               player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), shield));
               player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
         }
      } else {
         if (!entity.getType().is(ModTags.Entities.GHOST) || advantage.equals(ModTags.Entities.UNDEAD.location())) {
            entity.hurt(source, damage);
         }

         if (this.isSoulFireGun()) {
            ScorchedGuns.setSoulFireOnEntity(entity, 5);
         } else {
            entity.igniteForSeconds(5);
         }
      }

      if (entity instanceof LivingEntity) {
         GunEnchantmentHelper.applyElementalPopEffect(this.getWeapon(), (LivingEntity)entity);
      }

      if (this.shooter instanceof Player) {
         int hitType = critical ? 2 : (headshot ? 1 : 0);
         PacketHandler.getPlayChannel()
            .sendToPlayer(
               () -> (ServerPlayer)this.shooter,
               new S2CMessageProjectileHitEntity(hitVec.x, hitVec.y, hitVec.z, hitType, entity instanceof Player)
            );
      }

      this.spawnExplosionParticles(hitVec);
      this.clearSculkInArea(BlockPos.containing(hitVec));
   }

   @Override
   protected void onHitBlock(BlockState state, BlockPos pos, Direction face, double x, double y, double z) {
      this.spawnExplosionParticles(new Vec3(x, y, z));
      if ((Boolean)Config.COMMON.gameplay.enableFirePlacement.get()) {
         this.setBlockOnFire(pos, face);
      }

      this.clearSculkInArea(pos);
   }

   @Override
   public void onExpired() {
      Vec3 position = new Vec3(this.getX(), this.getY(), this.getZ());
      this.spawnExplosionParticles(position);
      this.clearSculkInArea(BlockPos.containing(position));
   }

   private void clearSculkInArea(BlockPos center) {
      if (!this.level().isClientSide) {
         if (!CompatManager.SCULK_HORDE_LOADED) {
            int clearedBlocks = 0;
            int maxClearBlocks = 8;

            for (int x = -2; x <= 2; x++) {
               for (int y = -2; y <= 2; y++) {
                  for (int z = -2; z <= 2; z++) {
                     if (clearedBlocks >= maxClearBlocks) {
                        return;
                     }

                     BlockPos checkPos = center.offset(x, y, z);
                     BlockState blockState = this.level().getBlockState(checkPos);
                     if (blockState.is(ModTags.Blocks.SCULK_BLOCKS)) {
                        this.level().destroyBlock(checkPos, true);
                        this.spawnCleansingParticles(checkPos);
                        this.level().playSound(null, checkPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.2F + this.random.nextFloat() * 0.4F);
                        clearedBlocks++;
                     }
                  }
               }
            }

            if (clearedBlocks > 0) {
               this.level().playSound(null, center, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.5F, 1.5F);
            }
         }
      }
   }

   private void spawnCleansingParticles(BlockPos pos) {
      if (this.level() instanceof ServerLevel serverLevel) {
         for (int i = 0; i < 5; i++) {
            double offsetX = (double)pos.getX() + 0.5 + (this.random.nextDouble() - 0.5) * 0.8;
            double offsetY = (double)pos.getY() + 0.5 + (this.random.nextDouble() - 0.5) * 0.8;
            double offsetZ = (double)pos.getZ() + 0.5 + (this.random.nextDouble() - 0.5) * 0.8;
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH, offsetX, offsetY, offsetZ, 2, 0.2, 0.2, 0.2, 0.1);
            serverLevel.sendParticles(ParticleTypes.SMOKE, offsetX, offsetY, offsetZ, 1, 0.1, 0.3, 0.1, 0.05);
         }

         serverLevel.sendParticles(
            ParticleTypes.FLAME, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.02
         );
      }
   }

   private void spawnExplosionParticles(Vec3 position) {
      if (!this.level().isClientSide) {
         ServerLevel serverLevel = (ServerLevel)this.level();
         if (this.isSoulFireGun()) {
            for (int i = 0; i < 20; i++) {
               double offsetX = (this.random.nextDouble() - 0.5) * 0.2;
               double offsetY = (this.random.nextDouble() - 0.5) * 0.2;
               double offsetZ = (this.random.nextDouble() - 0.5) * 0.2;
               double speedX = (this.random.nextDouble() - 0.5) * 0.3;
               double speedY = (this.random.nextDouble() - 0.5) * 0.3;
               double speedZ = (this.random.nextDouble() - 0.5) * 0.3;
               serverLevel.sendParticles(
                  ParticleTypes.SOUL_FIRE_FLAME,
                  position.x + offsetX,
                  position.y + offsetY,
                  position.z + offsetZ,
                  1,
                  speedX,
                  speedY,
                  speedZ,
                  0.05
               );
            }
         } else {
            for (int i = 0; i < 15; i++) {
               double offsetX = (this.random.nextDouble() - 0.5) * 0.2;
               double offsetY = (this.random.nextDouble() - 0.5) * 0.2;
               double offsetZ = (this.random.nextDouble() - 0.5) * 0.2;
               double speedX = (this.random.nextDouble() - 0.5) * 0.5;
               double speedY = (this.random.nextDouble() - 0.5) * 0.5;
               double speedZ = (this.random.nextDouble() - 0.5) * 0.5;
               serverLevel.sendParticles(
                  ParticleTypes.LAVA,
                  position.x + offsetX,
                  position.y + offsetY,
                  position.z + offsetZ,
                  1,
                  speedX,
                  speedY,
                  speedZ,
                  0.1
               );
               serverLevel.sendParticles(
                  ParticleTypes.DRIPPING_LAVA,
                  position.x + offsetX,
                  position.y + offsetY,
                  position.z + offsetZ,
                  1,
                  speedX,
                  speedY,
                  speedZ,
                  0.1
               );
               serverLevel.sendParticles(
                  ParticleTypes.SMALL_FLAME,
                  position.x + offsetX,
                  position.y + offsetY,
                  position.z + offsetZ,
                  1,
                  speedX,
                  speedY,
                  speedZ,
                  0.1
               );
            }
         }
      }
   }

   private void setBlockOnFire(BlockPos pos, Direction face) {
      this.tryPlaceWallFire(pos, face);
      if (this.random.nextFloat() < 0.6F) {
         Direction[] adjacentFaces = this.getAdjacentFaces(face);

         for (Direction adjacentFace : adjacentFaces) {
            if (this.random.nextFloat() < 0.4F) {
               this.tryPlaceWallFire(pos, adjacentFace);
            }
         }
      }

      if (face != Direction.UP && this.random.nextFloat() < 0.7F) {
         this.tryPlaceWallFire(pos, Direction.UP);
      }
   }

   private boolean canSustainFireOnFace(BlockState blockState, BlockPos pos, Direction face) {
      return blockState.isFlammable(this.level(), pos, face) ? true : blockState.isSolidRender(this.level(), pos);
   }

   private BlockState getWallFireState(Direction attachedFace) {
      if (this.isSoulFireGun()) {
         return ((Block)ModBlocks.FAKE_SOUL_FIRE.get()).defaultBlockState();
      } else {
         BlockState fireState = Blocks.FIRE.defaultBlockState();

         return switch (attachedFace) {
            case UP -> (BlockState)fireState.setValue(FireBlock.UP, true);
            case NORTH -> (BlockState)fireState.setValue(FireBlock.NORTH, true);
            case SOUTH -> (BlockState)fireState.setValue(FireBlock.SOUTH, true);
            case EAST -> (BlockState)fireState.setValue(FireBlock.EAST, true);
            case WEST -> (BlockState)fireState.setValue(FireBlock.WEST, true);
            default -> fireState;
         };
      }
   }

   private void tryPlaceWallFire(BlockPos pos, Direction face) {
      BlockPos offsetPos = pos.relative(face);
      if (this.level().isEmptyBlock(offsetPos)) {
         BlockState hitBlockState = this.level().getBlockState(pos);
         if (this.canSustainFireOnFace(hitBlockState, pos, face)) {
            BlockState fireState = this.getWallFireState(face.getOpposite());
            this.level().setBlock(offsetPos, fireState, 11);
         }
      }
   }

   private Direction[] getAdjacentFaces(Direction face) {
      return switch (face) {
         case UP, DOWN -> new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
         case NORTH, SOUTH -> new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST};
         case EAST, WEST -> new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH};
         default -> throw new IncompatibleClassChangeError();
      };
   }
}
