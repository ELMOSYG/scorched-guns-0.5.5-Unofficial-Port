package top.ribs.scguns.effect;

import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageBlood;

public class LaceratedEffect extends MobEffect {
   private static final int MIN_DAMAGE_INTERVAL = 35;
   private static final int MAX_DAMAGE_INTERVAL = 80;
   private static final float MIN_DAMAGE = 1.0F;
   private static final float MAX_DAMAGE = 4.0F;

   public LaceratedEffect(MobEffectCategory category, int color) {
      super(category, color);
   }

   public boolean applyEffectTick(LivingEntity entity, int amplifier) {
      if (!entity.getType().is(ModTags.Entities.CANNOT_BE_LACERATED)) {
         RandomSource random = entity.getRandom();
         int damageInterval = 35 + random.nextInt(46);
         if (random.nextFloat() < 1.0F / (float)damageInterval) {
            float baseDamage = 1.0F + random.nextFloat() * 3.0F;
            float damage = baseDamage * (1.0F + (float)amplifier * 0.3F);
            entity.hurt(entity.damageSources().magic(), damage);
            if (!entity.level().isClientSide) {
               double x = entity.getX();
               double y = entity.getY() + (double)entity.getBbHeight() * 0.5;
               double z = entity.getZ();
               S2CMessageBlood message = new S2CMessageBlood(x, y, z, entity.getType());
               PacketHandler.getPlayChannel().sendToTrackingEntity(() -> entity, message);
            }
         }

         super.applyEffectTick(entity, amplifier);
      }

      // 1.21 removes the effect when applyEffectTick returns false; 0.5.5 never expired early.
      return true;
   }

   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return true;
   }
}
