package top.ribs.scguns.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import top.ribs.scguns.init.ModParticleTypes;

public class GasExplosion {
   private final Level level;
   private final BlockPos centerPos;
   private final float radius;
   private final int duration;
   private final RandomSource random;

   public GasExplosion(Level level, BlockPos centerPos, float radius, int duration) {
      super();
      this.level = level;
      this.centerPos = centerPos;
      this.radius = radius;
      this.duration = duration;
      this.random = level.random;
   }

   public void explode() {
      this.level
         .playSound(
            null,
            (double)this.centerPos.getX(),
            (double)this.centerPos.getY(),
            (double)this.centerPos.getZ(),
            SoundEvents.CAT_HISS,
            SoundSource.BLOCKS,
            2.0F,
            1.0F
         );

      for (int i = 0; i < this.duration; i++) {
         this.level.scheduleTick(this.centerPos, this.level.getBlockState(this.centerPos).getBlock(), i * 20);
      }
   }

   public void tick(int tickCount) {
      if (tickCount < this.duration) {
         this.spawnGasCloudParticles();
         this.applyEffectsToEntities();
      }
   }

   private void spawnGasCloudParticles() {
      for (int i = 0; i < 20; i++) {
         double xOffset = this.random.nextGaussian() * (double)this.radius;
         double yOffset = this.random.nextGaussian() * (double)(this.radius / 2.0F);
         double zOffset = this.random.nextGaussian() * (double)this.radius;
         double x = (double)this.centerPos.getX() + 0.5 + xOffset;
         double y = (double)this.centerPos.getY() + 0.5 + yOffset;
         double z = (double)this.centerPos.getZ() + 0.5 + zOffset;
         this.level.addParticle((ParticleOptions)ModParticleTypes.SULFUR_DUST.get(), x, y, z, 0.0, 0.0, 0.0);
      }
   }

   private void applyEffectsToEntities() {
      for (LivingEntity entity : this.level.getEntitiesOfClass(LivingEntity.class, new AABB(this.centerPos).inflate((double)this.radius))) {
         if (!(entity instanceof Player player) || !player.isCreative() && !player.isSpectator()) {
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 2));
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
         }
      }
   }
}
