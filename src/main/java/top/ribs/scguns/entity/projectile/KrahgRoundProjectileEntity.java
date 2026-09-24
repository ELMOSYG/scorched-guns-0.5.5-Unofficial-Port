package top.ribs.scguns.entity.projectile;


import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModDamageTypes;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageBlood;
import top.ribs.scguns.network.message.S2CMessageProjectileHitBlock;
import top.ribs.scguns.network.message.S2CMessageProjectileHitEntity;
import top.ribs.scguns.util.GunEnchantmentHelper;

public class KrahgRoundProjectileEntity extends ProjectileEntity {
   private static final float KRAHG_SHIELD_DISABLE_CHANCE = 0.6F;
   private static final float SHIELD_DAMAGE_PENETRATION = 0.4F;
   private static final float MAX_BREAKABLE_HARDNESS = 4.0F;
   private static final float BLOCK_BREAK_CHANCE = 0.65F;
   private static final float HEADSHOT_EFFECT_DURATION_MULTIPLIER = 1.5F;
   private boolean hasPassedThroughBlock = false;
   private static final List<Block> UNBREAKABLE_BLOCKS = Arrays.asList(
      Blocks.BEDROCK, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.END_PORTAL_FRAME, Blocks.ANCIENT_DEBRIS, Blocks.REINFORCED_DEEPSLATE
   );

   public KrahgRoundProjectileEntity(EntityType<? extends Entity> entityType, Level worldIn) {
      super(entityType, worldIn);
   }

   public KrahgRoundProjectileEntity(
      EntityType<? extends Entity> entityType, Level worldIn, LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun
   ) {
      super(entityType, worldIn, shooter, weapon, item, modifiedGun);
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

         while (!this.hasPassedThroughBlock || !Objects.requireNonNull(hitEntities).isEmpty()) {
            double blockDist = blockResult.getType() != Type.MISS ? startVec.distanceToSqr(blockResult.getLocation()) : Double.MAX_VALUE;
            ProjectileEntity.EntityResult closestEntity = null;
            double closestEntityDist = Double.MAX_VALUE;

            assert hitEntities != null;

            if (!hitEntities.isEmpty()) {
               for (ProjectileEntity.EntityResult entity : hitEntities) {
                  if (entity.getEntity().getId() != this.shooterId) {
                     double dist = startVec.distanceToSqr(entity.getHitPos());
                     if (dist < closestEntityDist) {
                        closestEntityDist = dist;
                        closestEntity = entity;
                     }
                  }
               }
            }

            if (!(blockDist < closestEntityDist) || blockResult.getType() == Type.MISS) {
               if (closestEntity != null) {
                  this.onHitEntity(closestEntity.getEntity(), closestEntity.getHitPos(), startVec, endVec, closestEntity.isHeadshot());
                  this.remove(RemovalReason.KILLED);
                  return;
               }
               break;
            }

            BlockState state = this.level().getBlockState(blockResult.getBlockPos());
            if (!this.canBreakBlock(state, blockResult.getBlockPos()) || this.hasPassedThroughBlock) {
               Vec3 hitLoc = blockResult.getLocation();
               this.onHitBlock(state, blockResult.getBlockPos(), blockResult.getDirection(), hitLoc.x, hitLoc.y, hitLoc.z);
               this.remove(RemovalReason.KILLED);
               return;
            }

            Vec3 hitLoc = blockResult.getLocation();
            this.onHitBlock(state, blockResult.getBlockPos(), blockResult.getDirection(), hitLoc.x, hitLoc.y, hitLoc.z);
            this.hasPassedThroughBlock = true;
            startVec = hitLoc.add(this.getDeltaMovement().scale(0.01));
            endVec = startVec.add(this.getDeltaMovement());
            blockResult = rayTraceBlocks(
               this.level(), new ClipContext(startVec, endVec, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, this), IGNORE_LEAVES
            );
            hitEntities = this.findEntitiesOnPath(startVec, endVec);
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.8, 0.8, 0.8));
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
            ((ServerLevel)this.level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z, 20, 0.0, 0.0, 0.0, 0.15);
            this.level().playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
         } else {
            ((ServerLevel)this.level()).sendParticles(ParticleTypes.CRIT, x, y, z, 10, 0.0, 0.0, 0.0, 0.1);
            PacketHandler.getPlayChannel().sendToTrackingChunk(() -> this.level().getChunkAt(pos), new S2CMessageProjectileHitBlock(x, y, z, pos, face));
         }
      }

      if (!canBreak) {
         this.remove(RemovalReason.KILLED);
      }
   }

   private boolean canBreakBlock(BlockState state, BlockPos pos) {
      if (!(Boolean)Config.COMMON.gameplay.griefing.enableBlockBreaking.get()) {
         return false;
      } else if (UNBREAKABLE_BLOCKS.contains(state.getBlock())) {
         return false;
      } else {
         float hardness = state.getDestroySpeed(this.level(), pos);
         if (hardness < 0.0F || hardness > 4.0F) {
            return false;
         } else if (state.hasBlockEntity()) {
            return false;
         } else {
            return !state.getFluidState().isEmpty() ? false : this.random.nextFloat() < 0.65F;
         }
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
         damage = this.calculateArmorBypassDamage(livingTarget, damage);
      }

      if (entity instanceof LivingEntity livingTarget) {
         damage = GunEnchantmentHelper.getPuncturingDamageReduction(this.getWeapon(), livingTarget, damage);
         damage = this.applyProjectileProtection(livingTarget, damage);
         damage = this.calculateArmorBypassDamage(livingTarget, damage);
      }

      DamageSource source = ModDamageTypes.Sources.projectile(this.level().registryAccess(), this, (LivingEntity)this.getOwner());
      boolean blocked = ProjectileEntity.ProjectileHelper.handleShieldHit(entity, this, damage, 0.6F);
      if (blocked) {
         float penetratingDamage = damage * 0.4F;
         entity.hurt(source, penetratingDamage);
         if (entity instanceof LivingEntity livingEntity) {
            ResourceLocation effectLocation = this.getProjectile().getImpactEffect();
            if (effectLocation != null) {
               float effectChance = this.getProjectile().getImpactEffectChance() * 0.4F;
               if (this.random.nextFloat() < effectChance) {
                  MobEffect effect = (MobEffect)BuiltInRegistries.MOB_EFFECT.get(effectLocation);
                  if (effect != null) {
                     int reducedDuration = (int)((float)this.getProjectile().getImpactEffectDuration() * 0.4F);
                     livingEntity.addEffect(new MobEffectInstance(top.ribs.scguns.util.ScEffects.holder(effect), reducedDuration, this.getProjectile().getImpactEffectAmplifier()));
                  }
               }
            }
         }
      } else if (!entity.getType().is(ModTags.Entities.GHOST) || advantage.equals(ModTags.Entities.UNDEAD.location())) {
         entity.hurt(source, damage);
         if (entity instanceof LivingEntity livingEntityx) {
            ResourceLocation effectLocation = this.getProjectile().getImpactEffect();
            if (effectLocation != null) {
               float effectChance = this.getProjectile().getImpactEffectChance();
               if (headshot) {
                  effectChance = Math.min(1.0F, effectChance * 1.25F);
               }

               if (this.hasPassedThroughBlock) {
                  effectChance = Math.min(1.0F, effectChance * 1.15F);
               }

               if (this.random.nextFloat() < effectChance) {
                  MobEffect effect = (MobEffect)BuiltInRegistries.MOB_EFFECT.get(effectLocation);
                  if (effect != null) {
                     int duration = this.getProjectile().getImpactEffectDuration();
                     if (headshot) {
                        duration = (int)((float)duration * 1.5F);
                     }

                     if (this.hasPassedThroughBlock) {
                        duration = (int)((float)duration * 1.25F);
                     }

                     livingEntityx.addEffect(new MobEffectInstance(top.ribs.scguns.util.ScEffects.holder(effect), duration, this.getProjectile().getImpactEffectAmplifier()));
                  }
               }
            }
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

      PacketHandler.getPlayChannel().sendToTrackingEntity(() -> entity, new S2CMessageBlood(hitVec.x, hitVec.y, hitVec.z, entity.getType()));
      entity.invulnerableTime = 0;
   }

   @Override
   public void onExpired() {
   }
}
