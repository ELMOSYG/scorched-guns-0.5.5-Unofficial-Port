package top.ribs.scguns.effect;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import top.ribs.scguns.entity.projectile.ProjectileEntity;
import top.ribs.scguns.init.ModDamageTypes;
import top.ribs.scguns.util.ScEnchants;

public abstract class CustomExplosion extends Explosion {
   final CustomExplosion.CustomBlockInteraction customBlockInteraction;
   final Level level;
   final Entity source;
   final double x;
   final double y;
   final double z;
   final float radius;
   private static final float MAX_EXPLOSION_DAMAGE = 7.0F;

   public CustomExplosion(
      Level pLevel,
      @Nullable Entity pSource,
      double pToBlowX,
      double pToBlowY,
      double pToBlowZ,
      float pRadius,
      boolean pFire,
      CustomExplosion.CustomBlockInteraction customBlockInteraction
   ) {
      super(pLevel, pSource, pToBlowX, pToBlowY, pToBlowZ, pRadius + 1.0F, pFire, BlockInteraction.KEEP);
      this.customBlockInteraction = customBlockInteraction;
      this.level = pLevel;
      this.source = pSource;
      this.x = pToBlowX;
      this.y = pToBlowY;
      this.z = pToBlowZ;
      this.radius = pRadius + 1.0F;
   }

   public void explode() {
      if (this.customBlockInteraction == CustomExplosion.CustomBlockInteraction.NONE) {
         this.level.gameEvent(this.source, GameEvent.EXPLODE, new Vec3(this.x, this.y, this.z));
         Set<BlockPos> set = Sets.newHashSet();
         float f2 = this.radius * 2.0F;
         int k = Mth.floor(this.x - (double)f2 - 1.0);
         int l = Mth.floor(this.x + (double)f2 + 1.0);
         int i2 = Mth.floor(this.y - (double)f2 - 1.0);
         int i1 = Mth.floor(this.y + (double)f2 + 1.0);
         int j2 = Mth.floor(this.z - (double)f2 - 1.0);
         int j1 = Mth.floor(this.z + (double)f2 + 1.0);
         List<Entity> list = this.level.getEntities(this.source, new AABB((double)k, (double)i2, (double)j2, (double)l, (double)i1, (double)j1));
         EventHooks.onExplosionDetonate(this.level, this, list, (double)f2);
         Vec3 vec3 = new Vec3(this.x, this.y, this.z);

         for (Entity entity : list) {
            if (!(entity instanceof ItemEntity) && !entity.ignoreExplosion(this)) {
               double d12 = Math.sqrt(entity.distanceToSqr(vec3)) / (double)f2;
               if (d12 <= 1.0) {
                  double d5 = entity.getX() - this.x;
                  double d7 = (entity instanceof PrimedTnt ? entity.getY() : entity.getEyeY()) - this.y;
                  double d9 = entity.getZ() - this.z;
                  double d13 = Math.sqrt(d5 * d5 + d7 * d7 + d9 * d9);
                  if (d13 != 0.0) {
                     d5 /= d13;
                     d7 /= d13;
                     d9 /= d13;
                     double d14 = (double)getSeenPercent(vec3, entity);
                     double d10 = (1.0 - d12) * d14;
                     float explosionDamage = (float)((d10 * d10 + d10) / 2.0 * 4.0 * (double)f2 + 1.0) * 2.0F;
                     explosionDamage = Math.min(explosionDamage, 7.0F);
                     entity.hurt(
                        ModDamageTypes.Sources.projectile(
                           this.level.registryAccess(), (ProjectileEntity)this.source, (LivingEntity)((ProjectileEntity)this.source).getOwner()
                        ),
                        explosionDamage
                     );
                     double d11;
                     if (entity instanceof LivingEntity livingentity) {
                        d11 = getExplosionKnockbackAfterDampener(livingentity, d10);
                     } else {
                        d11 = d10;
                     }

                     d5 *= d11;
                     d7 *= d11;
                     d9 *= d11;
                     Vec3 vec31 = new Vec3(d5, d7, d9);
                     entity.setDeltaMovement(entity.getDeltaMovement().add(vec31));
                     if (entity instanceof Player) {
                        Player player = (Player)entity;
                        if (!player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying)) {
                           this.getHitPlayers().put(player, vec31);
                        }
                     }
                  }
               }
            }
         }
      } else {
         super.explode();
      }
   }

   public void finalizeExplosion(boolean pSpawnParticles) {
      if (this.customBlockInteraction != CustomExplosion.CustomBlockInteraction.NONE) {
         super.finalizeExplosion(pSpawnParticles);
      } else {
         this.level.addParticle(ParticleTypes.EXPLOSION, this.x, this.y, this.z, 0.5, 0.0, 0.0);
         this.level.playLocalSound(this.x, this.y, this.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 2.0F, 1.0F, false);
      }
   }

   /**
    * 1.21 {@link Explosion} keeps its {@link DamageSource} private and no longer exposes a getter,
    * so the 1.20.1 accessor is rebuilt here from the same inputs the vanilla constructor uses.
    */
   public DamageSource getDamageSource() {
      return this.level.damageSources().explosion(this);
   }

   /**
    * Vanilla 1.20.1 {@code ProtectionEnchantment.getExplosionKnockbackAfterDampener}: blast
    * protection shaves 15% of the explosion knockback per level. 1.21 turns the enchantment into a
    * datapack entry, so the level is read through {@link ScEnchants}.
    */
   private static double getExplosionKnockbackAfterDampener(LivingEntity entity, double knockback) {
      int level = ScEnchants.level(entity, Enchantments.BLAST_PROTECTION);
      return level > 0 ? knockback * Math.max(0.0, 1.0 - (double)level * 0.15) : knockback;
   }

   public static enum CustomBlockInteraction {
      NONE,
      KEEP,
      DESTROY;

      private CustomBlockInteraction() {
      }
   }
}
