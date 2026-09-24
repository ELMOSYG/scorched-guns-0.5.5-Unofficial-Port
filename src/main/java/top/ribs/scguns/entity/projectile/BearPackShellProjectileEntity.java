package top.ribs.scguns.entity.projectile;



import top.ribs.scguns.util.ScEnchants;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModDamageTypes;
import top.ribs.scguns.init.ModEnchantments;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.interfaces.IDamageable;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageBlood;
import top.ribs.scguns.network.message.S2CMessageProjectileHitBlock;
import top.ribs.scguns.network.message.S2CMessageProjectileHitEntity;
import top.ribs.scguns.util.GunEnchantmentHelper;
import top.ribs.scguns.util.math.ExtendedEntityRayTraceResult;

public class BearPackShellProjectileEntity extends ProjectileEntity {
   private static final float SHIELD_DISABLE_CHANCE = 0.4F;
   private static final float SHIELD_DAMAGE_PENETRATION = 0.4F;

   public BearPackShellProjectileEntity(EntityType<? extends Entity> entityType, Level worldIn) {
      super(entityType, worldIn);
   }

   public BearPackShellProjectileEntity(
      EntityType<? extends Entity> entityType, Level worldIn, LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun
   ) {
      super(entityType, worldIn, shooter, weapon, item, modifiedGun);
   }

   protected int getEffectiveCollateralLevel() {
      int actualCollateral = ScEnchants.level(this.getWeapon(), ModEnchantments.COLLATERAL);
      return 2 + actualCollateral;
   }

   @Override
   public void tick() {
      super.tick();
      this.updateHeading();
      this.onProjectileTick();
      if (!this.level().isClientSide()) {
         Vec3 startVec = this.position();
         Vec3 endVec = startVec.add(this.getDeltaMovement());
         HitResult result = rayTraceBlocks(this.level(), new ClipContext(startVec, endVec, Block.COLLIDER, Fluid.NONE, this), IGNORE_LEAVES);
         if (result.getType() != Type.MISS) {
            endVec = result.getLocation();
         }

         List<ProjectileEntity.EntityResult> hitEntities = null;
         int effectiveLevel = this.getEffectiveCollateralLevel();
         if (effectiveLevel == 0) {
            ProjectileEntity.EntityResult entityResult = this.findEntityOnPath(startVec, endVec);
            if (entityResult != null) {
               hitEntities = Collections.singletonList(entityResult);
            }
         } else {
            hitEntities = this.findEntitiesOnPath(startVec, endVec);
         }

         if (hitEntities != null && !hitEntities.isEmpty()) {
            for (ProjectileEntity.EntityResult entityResult : hitEntities) {
               EntityHitResult entityHitResult = new ExtendedEntityRayTraceResult(entityResult);
               if (entityHitResult.getEntity() instanceof Player player && this.shooter instanceof Player && !((Player)this.shooter).canHarmPlayer(player)) {
                  entityHitResult = null;
               }

               if (entityHitResult != null) {
                  this.onHit(entityHitResult, startVec, endVec);
               }
            }
         } else {
            this.onHit(result, startVec, endVec);
         }
      }

      double nextPosX = this.getX() + this.getDeltaMovement().x();
      double nextPosY = this.getY() + this.getDeltaMovement().y();
      double nextPosZ = this.getZ() + this.getDeltaMovement().z();
      this.setPos(nextPosX, nextPosY, nextPosZ);
      if (this.projectile.isGravity()) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, this.modifiedGravity, 0.0));
      }

      if (this.tickCount >= this.life) {
         if (this.isAlive()) {
            this.onExpired();
         }

         this.remove(RemovalReason.KILLED);
      }
   }

   @Override
   protected void onHitEntity(Entity entity, Vec3 hitVec, Vec3 startVec, Vec3 endVec, boolean headshot) {
      if (entity.getId() != this.shooterId) {
         float damage = this.getDamage();
         float newDamage = this.getCriticalDamage(this.getWeapon(), this.random, damage);
         boolean critical = damage != newDamage;
         ResourceLocation advantage = this.getProjectile().getAdvantage();
         damage = newDamage * this.advantageMultiplier(entity);
         boolean wasAlive = entity instanceof LivingEntity && entity.isAlive();
         if (headshot) {
            damage = (float)((double)damage * (Double)Config.COMMON.gameplay.headShotDamageMultiplier.get());
         }

         if (entity instanceof LivingEntity livingTarget) {
            damage = this.applyProjectileProtection(livingTarget, damage);
            damage = this.calculateArmorBypassDamage(livingTarget, damage);
         }

         DamageSource source = ModDamageTypes.Sources.projectile(this.level().registryAccess(), this, this.shooter);
         boolean blocked = ProjectileEntity.ProjectileHelper.handleShieldHit(entity, this, damage, 0.4F);
         if (blocked) {
            float penetratingDamage = damage * 0.4F;
            entity.hurt(source, penetratingDamage);
         } else if (!entity.getType().is(ModTags.Entities.GHOST) || advantage.equals(ModTags.Entities.UNDEAD.location())) {
            entity.hurt(source, damage);
            if (entity instanceof LivingEntity livingEntity) {
               ResourceLocation effectLocation = this.getProjectile().getImpactEffect();
               if (effectLocation != null) {
                  float effectChance = this.getProjectile().getImpactEffectChance();
                  if (this.random.nextFloat() < effectChance) {
                     MobEffect effect = (MobEffect)BuiltInRegistries.MOB_EFFECT.get(effectLocation);
                     if (effect != null) {
                        int duration = this.getProjectile().getImpactEffectDuration();
                        livingEntity.addEffect(new MobEffectInstance(top.ribs.scguns.util.ScEffects.holder(effect), duration, this.getProjectile().getImpactEffectAmplifier()));
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

         if (wasAlive && entity instanceof LivingEntity livingEntityx && !livingEntityx.isAlive()) {
            this.checkForDiamondSteelBonus(livingEntityx, hitVec);
         }

         PacketHandler.getPlayChannel().sendToTrackingEntity(() -> entity, new S2CMessageBlood(hitVec.x, hitVec.y, hitVec.z, entity.getType()));
      }
   }

   @Override
   protected void onHitBlock(BlockState state, BlockPos pos, Direction face, double x, double y, double z) {
      PacketHandler.getPlayChannel().sendToTrackingChunk(() -> this.level().getChunkAt(pos), new S2CMessageProjectileHitBlock(x, y, z, pos, face));
      if ((Boolean)Config.COMMON.gameplay.griefing.enableGlassBreaking.get() && state.is(ModTags.Blocks.FRAGILE)) {
         float destroySpeed = state.getDestroySpeed(this.level(), pos);
         if (destroySpeed >= 0.0F) {
            float chance = ((Double)Config.COMMON.gameplay.griefing.fragileBaseBreakChance.get()).floatValue() / (destroySpeed + 1.0F);
            if (this.random.nextFloat() < chance) {
               this.level().destroyBlock(pos, (Boolean)Config.COMMON.gameplay.griefing.fragileBlockDrops.get());
            }
         }
      }

      if (state.getBlock() instanceof DoorBlock) {
         boolean isOpen = (Boolean)state.getValue(DoorBlock.OPEN);
         if (!isOpen) {
            this.level().setBlock(pos, (BlockState)state.setValue(DoorBlock.OPEN, true), 10);
            this.level().playSound(null, pos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
         }
      }

      if (state.getBlock() instanceof IDamageable) {
         ((IDamageable)state.getBlock()).onBlockDamaged(this.level(), state, pos, this, this.getDamage(), (int)Math.ceil((double)this.getDamage() / 2.0) + 1);
      }
   }

   @Override
   protected void onProjectileTick() {
      if (this.level().isClientSide && this.tickCount > 1 && this.tickCount < this.life && this.tickCount % 5 == 0) {
         double offsetX = (this.random.nextDouble() - 0.5) * 0.5;
         double offsetY = (this.random.nextDouble() - 0.5) * 0.5;
         double offsetZ = (this.random.nextDouble() - 0.5) * 0.5;
         double velocityX = (this.random.nextDouble() - 0.5) * 0.1;
         double velocityY = (this.random.nextDouble() - 0.5) * 0.1;
         double velocityZ = (this.random.nextDouble() - 0.5) * 0.1;
         this.level()
            .addParticle(
               ParticleTypes.SOUL_FIRE_FLAME, true, this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ, velocityX, velocityY, velocityZ
            );
      }
   }
}
