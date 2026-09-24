package top.ribs.scguns.entity.projectile;



import top.ribs.scguns.util.ScEnchants;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModDamageTypes;
import top.ribs.scguns.init.ModEnchantments;
import top.ribs.scguns.init.ModParticleTypes;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageBlood;
import top.ribs.scguns.network.message.S2CMessageProjectileHitBlock;
import top.ribs.scguns.network.message.S2CMessageProjectileHitEntity;

public class OsborneSlugProjectileEntity extends ProjectileEntity {
   private static final float SHIELD_DISABLE_CHANCE = 0.9F;
   private static final float SHIELD_DAMAGE_PENETRATION = 0.75F;
   private static final float MAX_BREAKABLE_HARDNESS = 6.0F;
   private static final float HEADSHOT_EFFECT_DURATION_MULTIPLIER = 1.5F;
   private static final float PENETRATION_EFFECT_REDUCTION = 0.8F;
   private static final List<Block> UNBREAKABLE_BLOCKS = Arrays.asList(
      Blocks.BEDROCK, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.END_PORTAL_FRAME, Blocks.ANCIENT_DEBRIS, Blocks.REINFORCED_DEEPSLATE
   );
   private int remainingPenetrations;

   public OsborneSlugProjectileEntity(EntityType<? extends Entity> entityType, Level worldIn) {
      super(entityType, worldIn);
      this.remainingPenetrations = 3;
      this.setArmorBypassAmount(5.0F);
   }

   public OsborneSlugProjectileEntity(
      EntityType<? extends Entity> entityType, Level worldIn, LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun
   ) {
      super(entityType, worldIn, shooter, weapon, item, modifiedGun);
      int collateralLevel = ScEnchants.level(weapon, ModEnchantments.COLLATERAL);
      this.remainingPenetrations = 3 + collateralLevel;
   }

   @Override
   public void tick() {
      if (!this.level().isClientSide()) {
         Vec3 startVec = this.position();
         Vec3 endVec = startVec.add(this.getDeltaMovement());
         BlockHitResult blockResult = rayTraceBlocks(
            this.level(), new ClipContext(startVec, endVec, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, this), IGNORE_LEAVES
         );
         List<ProjectileEntity.EntityResult> hitEntities = this.findEntitiesOnPath(startVec, endVec);
         boolean hitSomething = false;

         while (this.remainingPenetrations > 0) {
            double blockDist = blockResult.getType() != Type.MISS ? startVec.distanceToSqr(blockResult.getLocation()) : Double.MAX_VALUE;
            ProjectileEntity.EntityResult closestEntity = null;
            double closestEntityDist = Double.MAX_VALUE;
            if (hitEntities != null && !hitEntities.isEmpty()) {
               for (ProjectileEntity.EntityResult entity : hitEntities) {
                  double dist = startVec.distanceToSqr(entity.getHitPos());
                  if (dist < closestEntityDist) {
                     closestEntityDist = dist;
                     closestEntity = entity;
                  }
               }
            }

            if (blockDist < closestEntityDist && blockResult.getType() != Type.MISS) {
               BlockState state = this.level().getBlockState(blockResult.getBlockPos());
               if (!this.canBreakBlock(state, blockResult.getBlockPos())) {
                  this.remove(RemovalReason.KILLED);
                  return;
               }

               Vec3 hitLoc = blockResult.getLocation();
               this.onHitBlock(state, blockResult.getBlockPos(), blockResult.getDirection(), hitLoc.x, hitLoc.y, hitLoc.z);
               this.remainingPenetrations--;
               hitSomething = true;
               startVec = hitLoc.add(this.getDeltaMovement().scale(0.01));
               endVec = startVec.add(this.getDeltaMovement());
               blockResult = rayTraceBlocks(
                  this.level(), new ClipContext(startVec, endVec, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, this), IGNORE_LEAVES
               );
            } else {
               if (closestEntity == null) {
                  break;
               }

               this.onHitEntity(closestEntity.getEntity(), closestEntity.getHitPos(), startVec, endVec, closestEntity.isHeadshot());
               this.remainingPenetrations--;
               hitSomething = true;
               hitEntities.remove(closestEntity);
            }
         }

         if (hitSomething && this.remainingPenetrations <= 0) {
            this.remove(RemovalReason.KILLED);
            return;
         }
      }

      this.setPos(this.getX() + this.getDeltaMovement().x, this.getY() + this.getDeltaMovement().y, this.getZ() + this.getDeltaMovement().z);
      if (this.projectile.isGravity()) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, this.modifiedGravity, 0.0));
      }

      this.updateHeading();
      if (this.tickCount >= this.life) {
         this.onExpired();
         this.remove(RemovalReason.KILLED);
      }
   }

   @Override
   protected void onHitBlock(BlockState state, BlockPos pos, Direction face, double x, double y, double z) {
      boolean canBreak = this.canBreakBlock(state, pos);
      if (!this.level().isClientSide) {
         if (canBreak) {
            this.level().destroyBlock(pos, true);
            ((ServerLevel)this.level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z, 30, 0.0, 0.0, 0.0, 0.15);
            this.level().playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.8, 0.8, 0.8));
         } else {
            ((ServerLevel)this.level()).sendParticles(ParticleTypes.CRIT, x, y, z, 10, 0.0, 0.0, 0.0, 0.1);
            PacketHandler.getPlayChannel().sendToTrackingChunk(() -> this.level().getChunkAt(pos), new S2CMessageProjectileHitBlock(x, y, z, pos, face));
            if (!(Boolean)Config.COMMON.gameplay.griefing.enableBlockBreaking.get()) {
               this.remove(RemovalReason.KILLED);
            }
         }
      }
   }

   private boolean canBreakBlock(BlockState state, BlockPos pos) {
      if (!(Boolean)Config.COMMON.gameplay.griefing.enableBlockBreaking.get()) {
         return false;
      } else if (UNBREAKABLE_BLOCKS.contains(state.getBlock())) {
         return false;
      } else {
         float hardness = state.getDestroySpeed(this.level(), pos);
         if (hardness < 0.0F || hardness > 6.0F) {
            return false;
         } else {
            return state.hasBlockEntity() ? false : state.getFluidState().isEmpty();
         }
      }
   }

   @Override
   protected void onHitEntity(Entity entity, Vec3 hitVec, Vec3 startVec, Vec3 endVec, boolean headshot) {
      if (entity.getId() != this.shooterId) {
         float damage = this.getDamage();
         damage = this.getCriticalDamage(this.getWeapon(), this.random, damage);
         damage *= this.advantageMultiplier(entity);
         if (headshot) {
            damage = (float)((double)damage * (Double)Config.COMMON.gameplay.headShotDamageMultiplier.get());
         }

         if (entity instanceof LivingEntity livingTarget) {
            damage = this.applyProjectileProtection(livingTarget, damage);
            damage = this.calculateArmorBypassDamage(livingTarget, damage);
         }

         if (entity instanceof LivingEntity livingTarget) {
            damage = this.calculateArmorBypassDamage(livingTarget, damage);
         }

         DamageSource source = ModDamageTypes.Sources.projectile(this.level().registryAccess(), this, (LivingEntity)this.getOwner());
         boolean blocked = ProjectileEntity.ProjectileHelper.handleShieldHit(entity, this, damage, 0.9F);
         if (blocked) {
            float penetratingDamage = damage * 0.75F;
            entity.hurt(source, penetratingDamage);
            if (entity instanceof LivingEntity livingEntity) {
               this.applyEffect(livingEntity, 0.75F, headshot);
            }
         } else {
            entity.hurt(source, damage);
            if (entity instanceof LivingEntity livingEntity) {
               float effectPower = (float)Math.pow(0.8F, (double)(3 - this.remainingPenetrations));
               this.applyEffect(livingEntity, effectPower, headshot);
            }
         }

         if (this.shooter instanceof Player) {
            PacketHandler.getPlayChannel()
               .sendToPlayer(
                  () -> (ServerPlayer)this.shooter,
                  new S2CMessageProjectileHitEntity(hitVec.x, hitVec.y, hitVec.z, 0, entity instanceof Player)
               );
         }

         PacketHandler.getPlayChannel().sendToTrackingEntity(() -> entity, new S2CMessageBlood(hitVec.x, hitVec.y, hitVec.z, entity.getType()));
         if (this.remainingPenetrations > 0) {
            this.remainingPenetrations--;
            entity.invulnerableTime = 0;
            Vec3 motion = this.getDeltaMovement();
            this.setPos(this.getX() + motion.x * 0.2, this.getY() + motion.y * 0.2, this.getZ() + motion.z * 0.2);
            this.setDeltaMovement(motion.multiply(0.8, 0.8, 0.8));
         }
      }
   }

   private void applyEffect(LivingEntity target, float powerMultiplier, boolean headshot) {
      ResourceLocation effectLocation = this.getProjectile().getImpactEffect();
      if (effectLocation != null) {
         float effectChance = this.getProjectile().getImpactEffectChance() * powerMultiplier;
         if (headshot) {
            effectChance = Math.min(1.0F, effectChance * 1.25F);
         }

         double distanceToShooter = (double)target.distanceTo(this.getOwner());
         if (distanceToShooter < 10.0) {
            effectChance = Math.min(1.0F, effectChance * (1.0F + 0.2F * (1.0F - (float)(distanceToShooter / 10.0))));
         }

         if (this.random.nextFloat() < effectChance) {
            MobEffect effect = (MobEffect)BuiltInRegistries.MOB_EFFECT.get(effectLocation);
            if (effect != null) {
               int duration = this.getProjectile().getImpactEffectDuration();
               if (headshot) {
                  duration = (int)((float)duration * 1.5F);
               }

               duration = (int)((float)duration * powerMultiplier);
               int baseAmplifier = this.getProjectile().getImpactEffectAmplifier();
               int reducedAmplifier = Math.max(0, (int)((float)baseAmplifier * powerMultiplier));
               target.addEffect(new MobEffectInstance(top.ribs.scguns.util.ScEffects.holder(effect), duration, reducedAmplifier));
            }
         }
      }
   }

   public void remove(@NotNull RemovalReason reason) {
      if (reason != RemovalReason.KILLED || this.remainingPenetrations <= 0 || this.tickCount >= this.life) {
         super.remove(reason);
      }
   }

   @Override
   protected void onProjectileTick() {
      if (this.level().isClientSide && this.tickCount > 1 && this.tickCount < this.life && this.tickCount % 2 == 0) {
         double offsetX = (this.random.nextDouble() - 0.5) * 0.5;
         double offsetY = (this.random.nextDouble() - 0.5) * 0.5;
         double offsetZ = (this.random.nextDouble() - 0.5) * 0.5;
         double velocityX = (this.random.nextDouble() - 0.5) * 0.1;
         double velocityY = (this.random.nextDouble() - 0.5) * 0.1;
         double velocityZ = (this.random.nextDouble() - 0.5) * 0.1;
         if (this.remainingPenetrations < 3) {
            this.level()
               .addParticle(
                  ParticleTypes.ENCHANTED_HIT,
                  true,
                  this.getX() + offsetX,
                  this.getY() + offsetY,
                  this.getZ() + offsetZ,
                  velocityX * 2.0,
                  velocityY * 2.0,
                  velocityZ * 2.0
               );
            this.level()
               .addParticle(
                  ParticleTypes.SMOKE,
                  true,
                  this.getX() + offsetX,
                  this.getY() + offsetY,
                  this.getZ() + offsetZ,
                  velocityX,
                  velocityY,
                  velocityZ
               );
         } else {
            this.level()
               .addParticle(
                  ParticleTypes.ENCHANTED_HIT,
                  true,
                  this.getX() + offsetX,
                  this.getY() + offsetY,
                  this.getZ() + offsetZ,
                  velocityX,
                  velocityY,
                  velocityZ
               );
         }
      }
   }

   @Override
   public void onExpired() {
      this.spawnExplosionParticles(new Vec3(this.getX(), this.getY() + 0.1, this.getZ()));
   }

   private void spawnExplosionParticles(Vec3 position) {
      if (!this.level().isClientSide) {
         ServerLevel serverLevel = (ServerLevel)this.level();
         int particleCount = this.remainingPenetrations < 3 ? 8 : 5;
         serverLevel.sendParticles(
            (SimpleParticleType)ModParticleTypes.RAMROD_IMPACT.get(),
            position.x,
            position.y,
            position.z,
            particleCount,
            0.0,
            0.0,
            0.0,
            0.1
         );

         for (int i = 0; i < particleCount; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 0.2;
            double offsetY = (this.random.nextDouble() - 0.5) * 0.2;
            double offsetZ = (this.random.nextDouble() - 0.5) * 0.2;
            double speedX = (this.random.nextDouble() - 0.5) * 0.5;
            double speedY = (this.random.nextDouble() - 0.5) * 0.5;
            double speedZ = (this.random.nextDouble() - 0.5) * 0.5;
            serverLevel.sendParticles(
               ParticleTypes.ENCHANTED_HIT, position.x + offsetX, position.y + offsetY, position.z + offsetZ, 1, speedX, speedY, speedZ, 0.1
            );
         }
      }
   }
}
