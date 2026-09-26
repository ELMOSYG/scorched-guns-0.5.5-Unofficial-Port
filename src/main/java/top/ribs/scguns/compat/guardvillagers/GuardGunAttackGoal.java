package top.ribs.scguns.compat.guardvillagers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.entity.ai.MobGunFire;
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
      // Deliberately NO setFlags(MOVE|LOOK) - the same as the mod's own GunAttackGoal. Reserving those
      // flags is what made this goal "take over" a guard (HANDOFF section 82.7): while it ran - which is
      // whenever the guard had a target and a gun - the goal selector could not start any other goal
      // needing MOVE or LOOK, so Guard Villagers' melee, patrol, walk-back-to-checkpoint, return-to-
      // village, door and stroll goals were all dead, and an armed guard stopped behaving like a guard.
      // Without flags it only competes on priority for the tick order, and those goals keep running.
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
      // A target is required, not just a gun: a guard that has nothing to fight should get on with its
      // own business (patrol, go home, stroll) instead of standing there holding a gun.
      return this.mob.getTarget() != null && this.mob.getMainHandItem().getItem() instanceof GunItem;
   }

   public void start() {
      this.isReloading = false;
      // The cadence counters are NOT reset here. A guard's target flickers: Guard Villagers' own goals
      // keep running now, and any of them clearing the target stops this goal for a tick and starts it
      // again - which, when start() reset the cadence, meant attackTime restarted its countdown over and
      // over and the guard never got a shot off (HANDOFF section 82.7). They live on the goal instance,
      // which the goal selector reuses, so leaving them alone lets the cadence continue across restarts.
      ItemStack gunStack = this.mob.getMainHandItem();
      if (gunStack.getItem() instanceof GunItem gunItem) {
         Gun modifiedGun = gunItem.getModifiedGun(gunStack);
         if (modifiedGun != null && modifiedGun.getReloads() != null
            && NbtHelper.getOrCreateTag(gunStack).getInt("AmmoCount") <= 0) {
            // A guard has no ammo pouch: an empty gun is topped up when the goal starts, rather than left
            // useless because the loot table handed it over unloaded.
            NbtHelper.getOrCreateTag(gunStack).putInt("AmmoCount", modifiedGun.getReloads().getMaxAmmo());
         }
      }
   }

   public void stop() {
      // No getNavigation().stop() here: this goal does not steer the guard at all (see tick), and telling
      // the navigation to stop on every target flicker fought Guard Villagers' own movement goals.
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
            // A full magazine means what the attachments allow, not the bare gun data.
            NbtHelper.getOrCreateTag(gunStack).putInt("AmmoCount", MobGunFire.magazineSize(gunStack, modifiedGun));
            this.isReloading = false;
            this.playSound(modifiedGun.getSounds().getCock());
         }

         return;
      }

      double distance = this.mob.distanceTo(target);
      double fireRange = modifiedGun.getIdealAttackRange() * 1.5;

      // Aim only. Movement belongs to Guard Villagers here: this goal reserves no flags and steers no
      // navigation, so a guard walks, patrols, goes home, opens doors and closes into melee exactly as it
      // did before it was ever handed a gun (HANDOFF section 82.7). An earlier version wanted to keep the
      // gun's ideal range and back away when the target got close - that is a raider's behaviour, and two
      // goals steering the navigation at once only made the guard stutter.
      this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
      this.mob.setAggressive(true);
      boolean canSee = this.mob.hasLineOfSight(target);

      int ammo = NbtHelper.getOrCreateTag(gunStack).getInt("AmmoCount");
      if (ammo <= 0 || !canSee || distance > fireRange) {
         return;
      }

      // The cadence is GunAttackGoal's, not the gun's raw rate (HANDOFF section 82). Firing on
      // `gameTime - lastFireTime >= rate` alone made a guard empty a 2-tick semi-automatic every two
      // ticks - ten shots a second, sustained - and it ignored mobFireRateMultiplier, so a server that
      // slowed its gunners down saw no difference in the guards either. The gun's own rate now comes from
      // MobGunFire, which walks the enchantment and attachment chain the player's gun uses.
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
         boolean fired = MobGunFire.fire(this.mob, target, gunStack, this.accuracy);
         this.remainingBursts--;
         this.burstIntervalTimer = 2 + this.mob.level().random.nextInt(6);
         this.attackTime = MobGunFire.fireInterval(gunStack, modifiedGun);
         if (fired && NbtHelper.getOrCreateTag(gunStack).getInt("AmmoCount") <= 0) {
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

}
