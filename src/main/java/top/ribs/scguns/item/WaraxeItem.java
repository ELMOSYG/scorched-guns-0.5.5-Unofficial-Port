package top.ribs.scguns.item;

import java.util.List;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WaraxeItem extends AxeItem {
   private final Vec3 slamRadius = new Vec3(5.0, 0.25, 5.0);

   public WaraxeItem(Properties properties) {
      // 1.20.1 passed (damageModifier, attackSpeedModifier) to AxeItem; 1.21 moved those
      // attributes into the item properties, so build the identical pair here.
      super(Tiers.IRON, properties.attributes(DiggerItem.createAttributes(Tiers.IRON, 7.0F, -3.2F)));
   }

   public void applyHoldingPose(HumanoidModel<LivingEntity> model, float ageInTicks, LivingEntity entity, ItemStack stack, float delta) {
      boolean leftHanded = entity.getMainArm() == HumanoidArm.LEFT;
      float attackTime = 1.0F - model.attackTime;
      attackTime *= attackTime;
      attackTime *= attackTime;
      attackTime = 1.0F - attackTime;
      float swingRotation = -Mth.sin(attackTime * (float) Math.PI) / 1.25F;
      float baseRightArmX = (float) (Math.PI / 3);
      float baseRightArmY = (float) (Math.PI / 6);
      float baseLeftArmX = 1.3762634F;
      float baseLeftArmY = (float) (Math.PI / 12);
      float swingOffset = swingRotation * 0.3490659F;
      model.rightArm.xRot = leftHanded ? -baseLeftArmX - swingRotation : -baseRightArmX - swingRotation;
      model.rightArm.yRot = leftHanded ? -baseLeftArmY - swingOffset : -baseRightArmY;
      model.leftArm.xRot = leftHanded ? -baseRightArmX - swingRotation : -baseLeftArmX - swingRotation;
      model.leftArm.yRot = leftHanded ? baseRightArmY : baseLeftArmY + swingOffset;
   }

   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      if (!attacker.level().isClientSide()) {
         float attackStrength = attacker instanceof Player player ? player.getAttackStrengthScale(0.5F) : 1.0F;
         boolean fullCharge = attackStrength > 0.9F;
         if (fullCharge && attacker.fallDistance > 0.0F) {
            this.performSlamAttack(attacker, target);
         } else {
            this.spawnSweepParticles(attacker);
         }

         attacker.level()
            .playSound(
               null,
               target.getX(),
               target.getY(),
               target.getZ(),
               SoundEvents.PLAYER_ATTACK_STRONG,
               attacker.getSoundSource(),
               1.0F,
               0.8F + attacker.getRandom().nextFloat() * 0.4F
            );
      }

      return super.hurtEnemy(stack, target, attacker);
   }

   private void performSlamAttack(LivingEntity attacker, LivingEntity target) {
      AABB aabb = target.getBoundingBox().inflate(this.slamRadius.x(), this.slamRadius.y(), this.slamRadius.z());
      List<LivingEntity> entities = attacker.level().getEntitiesOfClass(LivingEntity.class, aabb);
      attacker.level()
         .playSound(
            null,
            target.getX(),
            target.getY(),
            target.getZ(),
            SoundEvents.GENERIC_EXPLODE,
            attacker.getSoundSource(),
            1.2F,
            0.8F + attacker.getRandom().nextFloat() * 0.4F
         );
      this.spawnShockwaveParticles(target);

      for (LivingEntity entity : entities) {
         if (entity != attacker && entity != target) {
            float damage = attacker instanceof Player ? 4.0F : 3.0F;
            entity.hurt(attacker.damageSources().mobAttack(attacker), damage);
            this.spawnHitParticles(entity);
         }
      }

      if (attacker instanceof Player player) {
         player.getCooldowns().addCooldown(this, 40);
      }
   }

   private void spawnSweepParticles(LivingEntity entity) {
      if (!entity.level().isClientSide()) {
         double angle = Math.toRadians((double)(-entity.getYRot()));
         double offsetX = -Math.sin(angle) * 0.7;
         double offsetZ = Math.cos(angle) * 0.7;

         for (int i = 0; i < 3; i++) {
            ((ServerLevel)entity.level())
               .sendParticles(ParticleTypes.SWEEP_ATTACK, entity.getX() + offsetX, entity.getY(0.5), entity.getZ() + offsetZ, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   private void spawnShockwaveParticles(LivingEntity target) {
      if (!target.level().isClientSide()) {
         ServerLevel serverLevel = (ServerLevel)target.level();

         for (int i = 0; i < 20; i++) {
            double angle = (Math.PI * 2) * (double)i / 20.0;
            double offsetX = Math.cos(angle) * 1.5;
            double offsetZ = Math.sin(angle) * 1.5;
            serverLevel.sendParticles(
               ParticleTypes.CLOUD, target.getX() + offsetX, target.getY() + 0.1, target.getZ() + offsetZ, 1, 0.0, 0.0, 0.0, 0.0
            );
         }

         serverLevel.sendParticles(ParticleTypes.POOF, target.getX(), target.getY() + 0.1, target.getZ(), 3, 0.0, 0.0, 0.0, 0.0);
      }
   }

   private void spawnHitParticles(LivingEntity entity) {
      if (!entity.level().isClientSide()) {
         ((ServerLevel)entity.level()).sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY(0.5), entity.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
      }
   }
}
