package top.ribs.scguns.compat.guardvillagers;

import java.util.EnumSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.entity.ai.AIGunEvent;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.util.NbtHelper;

/**
 * The gun AI a Guard Villagers guard uses (HANDOFF section 82), ported from the standalone 1.20.1
 * {@code guard_scg_guns} mod.
 *
 * <p>Deliberately <b>not</b> a {@link top.ribs.scguns.entity.ai.GunAttackGoal}: that one is written for
 * hostile raiders - it charges, strafes into gunfire and fires in bursts with a random AI type. A guard
 * is a defender, so this goal keeps the gun's own ideal range, backs off instead of closing in, holds
 * fire when an ally is in the way, and reloads on a timer capped at two seconds rather than the gun's
 * full reload animation.</p>
 *
 * <p>Its only class dependency is {@link PathfinderMob} and the gun API: nothing here names a Guard
 * Villagers class, so a server without that mod can load this goal (and never constructs it, because
 * only {@link GuardVillagersCompat#isGuard} mobs get it).</p>
 */
public class GuardGunAttackGoal extends Goal {
   private final PathfinderMob mob;
   private final float accuracy;
   private int seeTime;
   private int repathTime;
   private boolean isReloading;
   private long reloadEndGameTime;
   /** Ticks to wait before the next shot: the gun's rate scaled by {@code mobFireRateMultiplier}. */
   private int attackTime;
   // Burst bookkeeping, mirroring GunAttackGoal (HANDOFF section 82): a gunner fires a short burst and
   // then pauses. Without it a guard with a 2-tick semi-automatic and a large magazine sprays.
   private int burstIntervalTimer;
   private int remainingBursts;
   private int burstResetTimer;
   private final int burstAmount;
   private final int burstTimer;

   public GuardGunAttackGoal(PathfinderMob mob, float accuracy, int difficulty) {
      this.mob = mob;
      this.accuracy = accuracy;
      this.setFlags(EnumSet.of(Goal.Flag.LOOK, Goal.Flag.MOVE));
      this.burstAmount = 2 + difficulty / 2;
      float difficultyMultiplier = switch (mob.level().getDifficulty()) {
         case PEACEFUL -> 2.0F;
         case EASY -> 1.5F;
         case NORMAL -> 1.0F;
         case HARD -> 0.6F;
      };
      float configMultiplier = ((Double)Config.COMMON.gameplay.mobBurstDelayMultiplier.get()).floatValue();
      this.burstTimer = Math.max(1, (int)((float)(30 - difficulty * 4) * difficultyMultiplier * configMultiplier));
   }

   public boolean canUse() {
      LivingEntity target = this.mob.getTarget();
      return target != null
         && target.isAlive()
         && !target.isRemoved()
         && this.mob.getMainHandItem().getItem() instanceof GunItem;
   }

   public boolean canContinueToUse() {
      // A target is required, not just a gun: otherwise the goal keeps holding MOVE|LOOK while the guard
      // has nothing to fight and the guard stands still instead of going home or strolling.
      return this.mob.getTarget() != null && this.mob.getMainHandItem().getItem() instanceof GunItem;
   }

   public void start() {
      this.repathTime = 0;
      this.isReloading = false;
      this.remainingBursts = 0;
      this.burstResetTimer = 0;
      this.burstIntervalTimer = 0;
      ItemStack gunStack = this.mob.getMainHandItem();
      if (gunStack.getItem() instanceof GunItem gunItem) {
         Gun modifiedGun = gunItem.getModifiedGun(gunStack);
         this.attackTime = modifiedGun == null ? 0 : this.fireInterval(modifiedGun);
         if (modifiedGun != null && modifiedGun.getReloads() != null
            && NbtHelper.getOrCreateTag(gunStack).getInt("AmmoCount") <= 0) {
            // A guard has no ammo pouch: an empty gun is topped up once when the goal starts, rather
            // than left useless because the loot table handed it over unloaded.
            NbtHelper.getOrCreateTag(gunStack).putInt("AmmoCount", modifiedGun.getReloads().getMaxAmmo());
         }
      } else {
         this.attackTime = 0;
      }
   }

   public void stop() {
      this.mob.getNavigation().stop();
      this.mob.setAggressive(false);
   }

   public boolean requiresUpdateEveryTick() {
      return true;
   }

   public void tick() {
      LivingEntity target = this.mob.getTarget();
      if (target == null || !target.isAlive() || target.isRemoved()) {
         return;
      }

      ItemStack gunStack = this.mob.getMainHandItem();
      if (!(gunStack.getItem() instanceof GunItem gunItem)) {
         return;
      }

      Gun modifiedGun = gunItem.getModifiedGun(gunStack);
      if (modifiedGun == null || modifiedGun.getGeneral() == null || modifiedGun.getReloads() == null) {
         return;
      }

      long gameTime = this.mob.level().getGameTime();
      if (this.isReloading) {
         if (gameTime >= this.reloadEndGameTime) {
            NbtHelper.getOrCreateTag(gunStack).putInt("AmmoCount", modifiedGun.getReloads().getMaxAmmo());
            this.isReloading = false;
            this.playSound(modifiedGun.getSounds().getCock());
         }

         return;
      }

      double distance = this.mob.distanceTo(target);
      double idealRange = modifiedGun.getIdealAttackRange();
      double fireRange = idealRange * 1.5;

      this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
      this.mob.setAggressive(true);

      boolean canSee = this.mob.hasLineOfSight(target);
      this.seeTime = canSee ? Math.min(this.seeTime + 1, 40) : 0;

      // Too close for a gun: back away instead of standing in melee range. The guard's own melee goal
      // can still take over there, which is intended - this only stops the shooting from happening at
      // point-blank range with an ally behind the target.
      if (distance <= 4.0) {
         this.mob.getMoveControl().strafe(this.mob.isUsingItem() ? -0.5F : -3.0F, 0.0F);
      }

      if (distance * distance > fireRange * fireRange || this.seeTime < 5) {
         if (--this.repathTime <= 0) {
            this.repathTime = 1 + this.mob.getRandom().nextInt(2);
            this.mob.getNavigation().moveTo(target, 1.0D);
         }
      } else {
         this.mob.getNavigation().stop();
      }

      // An ally in the firing line: step aside rather than shoot through them, and end the burst so the
      // guard is not firing again the moment it has moved.
      if (this.friendlyInLineOfSight() && distance * distance <= fireRange * fireRange) {
         Vec3 reposition = LandRandomPos.getPosTowards(this.mob, 5, 7, target.position());
         if (reposition != null && this.mob.getNavigation().isDone()) {
            this.mob.getNavigation().moveTo(reposition.x, reposition.y, reposition.z, 0.9D);
            this.remainingBursts = 0;
            this.attackTime = this.fireInterval(modifiedGun);
         }
      }

      int ammo = NbtHelper.getOrCreateTag(gunStack).getInt("AmmoCount");
      if (ammo <= 0 || !canSee || distance > fireRange) {
         return;
      }

      // The cadence is GunAttackGoal's, not the gun's raw rate (HANDOFF section 82). Firing on
      // `gameTime - lastFireTime >= rate` alone made a guard empty a 2-tick semi-automatic every two
      // ticks - ten shots a second, sustained - and it ignored mobFireRateMultiplier, so a server that
      // slowed its gunners down saw no difference in the guards either.
      if (--this.attackTime > 0) {
         return;
      }

      if (this.remainingBursts <= 0 && this.burstResetTimer <= 0) {
         this.remainingBursts = 1 + this.mob.level().random.nextInt(this.burstAmount);
         this.burstIntervalTimer = 1 + this.mob.level().random.nextInt(this.burstTimer);
         float configMultiplier = ((Double)Config.COMMON.gameplay.mobBurstDelayMultiplier.get()).floatValue();
         this.burstResetTimer = Math.max(5, (int)((float)(40 + this.mob.level().random.nextInt(40)) * configMultiplier));
      }

      if (this.remainingBursts > 0 && --this.burstIntervalTimer <= 0) {
         this.fire(target, gunStack, modifiedGun);
         this.remainingBursts--;
         this.burstIntervalTimer = 2 + this.mob.level().random.nextInt(6);
         this.attackTime = this.fireInterval(modifiedGun);
         NbtHelper.getOrCreateTag(gunStack).putInt("AmmoCount", ammo - 1);
         if (ammo - 1 <= 0) {
            this.isReloading = true;
            // The gun's own reload time, like every other gunner: it used to be clamped to 10..40 ticks,
            // which made a guard with a slow-reloading gun fire noticeably more often than a raider with
            // the same gun.
            this.reloadEndGameTime = gameTime + modifiedGun.getReloads().getReloadTimer();
            this.playSound(modifiedGun.getSounds().getReload());
         }
      }

      if (this.remainingBursts <= 0) {
         this.burstResetTimer--;
      }
   }

   /** Ticks between shots: the gun's rate, scaled by the same config the mod's own gunners use. */
   private int fireInterval(Gun modifiedGun) {
      float multiplier = ((Double)Config.COMMON.gameplay.mobFireRateMultiplier.get()).floatValue();
      return Math.max(1, (int)((float)modifiedGun.getGeneral().getRate() * multiplier));
   }

   private void fire(LivingEntity target, ItemStack gunStack, Gun modifiedGun) {
      this.rotateToFace(target);
      AIGunEvent.performGunAttack(this.mob, target, gunStack, modifiedGun, this.accuracy);
      if (target instanceof Mob mobTarget) {
         mobTarget.setLastHurtByMob(this.mob);
         mobTarget.setTarget(this.mob);
      }

      ResourceLocation fireSound = modifiedGun.getSounds().getFire();
      if (fireSound != null) {
         float volume = (Integer)Config.COMMON.gameplay.mobGunfireVolume.get() - 0.5F;
         float pitch = 0.9F + this.mob.level().getRandom().nextFloat() * 0.2F;
         this.mob
            .level()
            .playSound(
               null,
               this.mob.getX(),
               this.mob.getY() + (double)this.mob.getEyeHeight(),
               this.mob.getZ(),
               SoundEvent.createVariableRangeEvent(fireSound),
               SoundSource.NEUTRAL,
               volume,
               pitch
            );
      }
   }

   private void playSound(ResourceLocation sound) {
      if (sound != null) {
         this.mob
            .level()
            .playSound(
               null,
               this.mob.getX(),
               this.mob.getY(),
               this.mob.getZ(),
               SoundEvent.createVariableRangeEvent(sound),
               SoundSource.NEUTRAL,
               1.0F,
               1.0F
            );
      }
   }

   /**
    * An ally standing in the firing line - a villager, an iron golem, or another guard. Replaces the
    * 1.20.1 version's call into {@code RangedCrossbowAttackPassiveGoal.friendlyInLineOfSight}, which no
    * longer exists under that name, so this compat does not depend on Guard Villagers' internals at all.
    */
   private boolean friendlyInLineOfSight() {
      Vec3 look = this.mob.getViewVector(1.0F);
      Vec3 reach = look.scale(6.0);
      for (var entity : this.mob.level().getEntities(this.mob, this.mob.getBoundingBox().expandTowards(reach).inflate(1.0))) {
         if (entity == this.mob.getTarget() || entity == this.mob) {
            continue;
         }

         boolean friendly = entity instanceof net.minecraft.world.entity.npc.AbstractVillager
            || entity.getType() == EntityType.IRON_GOLEM
            || GuardVillagersCompat.isGuard(entity);
         if (!friendly) {
            continue;
         }

         Vec3 toEntity = entity.position().vectorTo(this.mob.position()).normalize();
         if (toEntity.dot(look) < 0.0 && this.mob.hasLineOfSight(entity)) {
            return true;
         }
      }

      return false;
   }

   private void rotateToFace(LivingEntity target) {
      double dx = target.getX() - this.mob.getX();
      double dz = target.getZ() - this.mob.getZ();
      double dy = target.getEyeY() - this.mob.getEyeY();
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
      float pitch = (float)(-Math.atan2(dy, horizontal) * 180.0 / Math.PI);
      this.mob.setYRot(yaw);
      this.mob.setXRot(pitch);
      this.mob.yHeadRot = yaw;
      this.mob.xRotO = pitch;
   }
}
