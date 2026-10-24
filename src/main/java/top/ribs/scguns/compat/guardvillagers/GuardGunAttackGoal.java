package top.ribs.scguns.compat.guardvillagers;

import java.util.EnumSet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.common.ReloadType;
import top.ribs.scguns.entity.ai.AIType;
import top.ribs.scguns.entity.ai.GunAttackGoal;
import top.ribs.scguns.entity.ai.MobGunFire;
import top.ribs.scguns.init.ModSounds;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.util.GunEnchantmentHelper;
import top.ribs.scguns.util.NbtHelper;
import net.minecraft.sounds.SoundSource;

/**
 * The gun AI a Guard Villagers guard uses (HANDOFF sections 82.9, 82.15).
 *
 * <h2>Why guards get their own AI after all</h2>
 *
 * <p>Sections 82.7 to 82.9 gave guards the mod's own raider AI ({@link GunAttackGoal}) and stripped its goal
 * flags, because reserving MOVE|LOOK while a guard had a target silently stopped Guard Villagers' patrol,
 * return-to-village, door and stroll goals. That fixed the takeover, and produced a different complaint the
 * player named exactly: that AI is <b>not smart</b>. It is not stupid code - it is a goal that is not
 * allowed to move. It calls the navigation to approach the gun's range and to back out of melee, but with no
 * flags to hold, Guard Villagers' own goals share that navigation and win it, so the guard drifts, fires
 * from wherever it happens to be, and reads as a mob that does not know how to fight.</p>
 *
 * <p>A combat AI that positions itself therefore has to own movement, which is exactly how vanilla's own
 * goals work - {@code MeleeAttackGoal}, which Guard Villagers' melee goal extends, reserves MOVE and LOOK
 * too. This goal is ported from the standalone 1.21.1 guard compat the player pointed at, which is the
 * "intelligent" behaviour: hold the gun's range, back out of melee, do not fire until the target has been
 * visible for a moment, step aside when an ally is in the line of fire, and reload when the magazine runs
 * dry. Those flags are only held while this goal is running, i.e. while the guard has a target and a gun;
 * the guard's patrol, village, door and stroll goals resume the moment it stops.</p>
 *
 * <h2>What is deliberately NOT ported from that version</h2>
 *
 * <ul>
 *   <li><b>Its firing.</b> The reference ticks every projectile it spawns ({@code projectile.tick()} in the
 *       spawn loop) - that is the section 82.11 shotgun stutter - and computes its cadence as
 *       {@code rate / 50}, which is the section 82.6 "wrong fire rate" bug (a 0.20.1 millisecond value read
 *       as ticks). Every shot here goes through {@link MobGunFire}, the one pipeline the player's own path,
 *       raiders and the maid compat share: rate chain after enchantments and attachments, the ammo rules
 *       (including Reclaimed ghost rounds), the silenced/enchanted fire sound and the casing.</li>
 *   <li><b>Its own ammo handling.</b> The reference drops a round by hand and clears the loaded projectile;
 *       {@link MobGunFire} spends ammo exactly as the player's path does.</li>
 *   <li><b>Its friendly-fire test.</b> The reference hardcodes villagers and iron golems. This uses
 *       {@code GuardVillagersCompat.isFriendlyShot}, the same ally rule that gates the projectiles and the
 *       damage events (section 82), so "do not shoot through an ally" and "an ally is not hurt" cannot
 *       disagree.</li>
 *   <li><b>Its fixed 15 block fighting distance.</b> This reads the gun's own {@code idealAttackRange}, so a
 *       shotgun and a marksman rifle are not used from the same distance.</li>
 * </ul>
 *
 * <p>It names no Guard Villagers class either, like every other file in this package: the mixins reach the
 * guard; this goal only needs a {@code PathfinderMob}.</p>
 */
public class GuardGunAttackGoal<T extends PathfinderMob> extends Goal {
   /** Inside this distance the guard steps back out of melee instead of standing in it, as the reference does. */
   private static final double RETREAT_RANGE = 6.0;
   private static final double RETREAT_RANGE_SQR = RETREAT_RANGE * RETREAT_RANGE;

   /** How far ahead an ally is looked for before a shot is taken. */
   private static final double ALLY_CHECK_REACH = 6.0;

   /**
    * How fast the guard closes in - Guard Villagers' own pace, not the mob's full attribute speed.
    *
    * <p>{@code Guard$GuardMeleeGoal} walks a guard at 0.6 of its movement speed attribute, so an armed guard
    * charging at 1.0 (the full attribute, which is what the ported AI asked for) crossed the village at
    * nearly twice the pace of the same guard holding a sword - the player's "an armed guard moves too fast"
    * (HANDOFF section 82.19). Same number, same gait.</p>
    */
   private static final double APPROACH_SPEED = 0.6;

   /** Backing off and stepping aside use Guard Villagers' other walk constant. */
   private static final double RETREAT_SPEED = 0.65;

   /** The target has to be visible this many ticks before the guard shoots, as the reference requires. */
   private static final int AIM_GATE = 5;
   /** How long a guard waits after finding an ally in its line of fire before trying again. */
   private static final int BLOCKED_RETRY_TICKS = 10;
   /** Anti-zero floor only: the gun supplies the real duration, this just stops a 0-tick reload existing. */
   private static final int MIN_RELOAD_TICKS = 4;
   /** Upper bound on a persisted reload timer, so a stale tag can never wedge the goal. */
   private static final int MAX_RELOAD_TICKS = 600;

   // The reload lives on the entity, not in this goal object. A goal is thrown away and rebuilt whenever the guard
   // re-acquires a target or the weapon is reassessed, and start() used to zero the timer - so a guard whose reload
   // takes longer than the gap between rebuilds never finished reloading at all.
   private static final String TAG_RELOADING = "ScgunsGuardReloading";
   private static final String TAG_RELOAD_TICKS = "ScgunsGuardReloadTicks";
   private static final String TAG_SHOTS = "ScgunsGuardShots";
   // Reload state written by an older build cannot be trusted: a save can carry Reloading=true with a timer
   // nothing ever decrements, or a timer from before the value meant what it means now, and the guard then stands
   // there reloading forever. Stamping the format lets a guard in an existing world discard the stale state and
   // start clean instead of having to be killed.
   private static final String TAG_RELOAD_STAMP = "ScgunsGuardReloadVersion";
   private static final int RELOAD_STATE_VERSION = 3;

   private final T guard;
   private final float accuracyModifier;
   private final double attackRadiusSqr;
   /** Where the guard stops firing and walks closer instead; capped by the config. */
   private final double fireRangeSqr;
   private final int aimTicks;

   private int seeTime;
   /** Ticks left before the guard tries a blocked shot again; stops the per-tick shuffle. */
   private int blockedTicks;
   /** True while this goal owns the crouch pose, so it never leaves a guard crouched. */
   private boolean posedCrouch;
   /** Shots fired since the last reload. Drives the magazine directly instead of trusting the gun's NBT. */
   private int shotsSinceReload;
   private int timeUntilShoot;
   private boolean retreating;
   private boolean reloading;
   private int reloadTick;

   public GuardGunAttackGoal(T guard, ItemStack gunStack, int difficulty, float accuracy) {
      this.guard = guard;
      this.accuracyModifier = accuracy;
      // The guard fights at this gun's range rather than at a fixed distance: a shotgun's ideal range is a
      // few blocks and a marksman rifle's is far more, and the reference's flat 15 blocks made every guard
      // fight like it was holding the same weapon.
      double idealRange = 15.0;
      if (gunStack.getItem() instanceof GunItem gunItem) {
         Gun gun = gunItem.getModifiedGun(gunStack);
         if (gun != null) {
            idealRange = Math.max(RETREAT_RANGE + 1.0, gun.getIdealAttackRange());
         }
      }

      this.attackRadiusSqr = idealRange * idealRange;
      // Positioning only ever said "stop walking here"; nothing stopped the guard firing from fifty blocks away
      // while it was still closing in. This is that missing gate, capped so a pack can shorten the range.
      this.fireRangeSqr = Math.pow(
         Math.min(idealRange, (Double)top.ribs.scguns.Config.COMMON.compat.guardGunMaxRange.get()), 2.0
      );
      // A tougher guard needs less time on target before its first shot (1..4 from the config data).
      this.aimTicks = (int)Mth.clamp(9 - difficulty, AIM_GATE, 8);
      // Positioning is this goal's job while it runs - see the class documentation.
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   @Override
   public boolean canUse() {
      LivingEntity target = this.guard.getTarget();
      if (target == null || !target.isAlive()) {
         return false;
      }

      return this.guard.getMainHandItem().getItem() instanceof GunItem;
   }

   @Override
   public boolean canContinueToUse() {
      return this.canUse() || (this.guard.getTarget() != null && !this.guard.getNavigation().isDone());
   }

   @Override
   public void start() {
      this.guard.setAggressive(true);
      this.seeTime = 0;
      this.blockedTicks = 0;
      this.timeUntilShoot = this.aimTicks;
      // Adopt any reload that was already in progress rather than restarting it from zero - unless the state was
      // written by an older build, in which case it is dropped and a stamp records the format that is now in use.
      CompoundTag data = this.guard.getPersistentData();

      if (data.getInt(TAG_RELOAD_STAMP) != RELOAD_STATE_VERSION) {
         this.reloading = false;
         this.reloadTick = 0;
         this.shotsSinceReload = 0;
         data.putBoolean(TAG_RELOADING, false);
         data.putInt(TAG_RELOAD_TICKS, 0);
         data.putInt(TAG_SHOTS, 0);
         data.putInt(TAG_RELOAD_STAMP, RELOAD_STATE_VERSION);
      } else {
         this.reloading = data.getBoolean(TAG_RELOADING);
         this.reloadTick = Mth.clamp(data.getInt(TAG_RELOAD_TICKS), 0, MAX_RELOAD_TICKS);
         this.shotsSinceReload = Math.max(0, data.getInt(TAG_SHOTS));
      }
   }

   @Override
   public void stop() {
      this.guard.setAggressive(false);
      this.seeTime = 0;
      this.retreating = false;
      this.reloading = false;
      this.reloadTick = 0;
      this.guard.getNavigation().stop();
      this.updateAimPose(false);
      if (this.guard.isUsingItem()) {
         this.guard.stopUsingItem();
      }
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return true;
   }

   @Override
   public void tick() {
      LivingEntity target = this.guard.getTarget();
      if (target == null) {
         return;
      }

      ItemStack heldItem = this.guard.getMainHandItem();
      if (!(heldItem.getItem() instanceof GunItem gunItem)) {
         return;
      }

      Gun gun = gunItem.getModifiedGun(heldItem);
      if (gun == null) {
         return;
      }

      double distanceSqr = this.guard.distanceToSqr(target);
      boolean canSee = this.guard.getSensing().hasLineOfSight(target);
      if (canSee != this.seeTime > 0) {
         this.seeTime = 0;
      }

      this.seeTime += canSee ? 1 : -1;
      this.guard.getLookControl().setLookAt(target, 30.0F, 30.0F);

      if (this.isMagazineEmpty(heldItem, gun)) {
         // Do NOT set `reloading` here: reload() does that itself, and setting it first made the method skip the
         // branch that initialises reloadTick, so reloadTick stayed 0 and the magazine refilled on the very next
         // tick - a zero-tick reload, which read as a guard that never stops firing.
         this.reload(gun);
         this.updateAimPose(false);
         return;
      }

      this.updateAimPose(canSee && distanceSqr <= this.fireRangeSqr);
      this.position(target, distanceSqr, canSee);
      this.tryToFire(target, heldItem, gun, distanceSqr, canSee);
   }

   /** Back out of melee, hold the gun's range, or close in - the reference's positioning, kept as it was. */
   private void position(LivingEntity target, double distanceSqr, boolean canSee) {
      // Guard Villagers' RaiseShieldGoal puts a shield in the offhand and starts using it, which slows the guard and
      // competes for navigation. Its goal owns the movement while the shield is up, so attempting to retreat here
      // just lost the race and left the guard standing against the enemy it was supposed to back away from. While
      // the shield is up this goal holds position and keeps shooting: a shield in front is worth staying for.
      if (this.guard.isUsingItem()) {
         this.retreating = false;
         this.guard.getNavigation().stop();
         return;
      }

      if (distanceSqr < RETREAT_RANGE_SQR) {
         this.retreating = true;
         if (this.guard.getNavigation().isDone()) {
            Vec3 away = LandRandomPos.getPosAway(this.guard, 10, 7, target.position());
            if (away != null) {
               this.guard.getNavigation().moveTo(away.x, away.y, away.z, moveSpeed(RETREAT_SPEED));
            }
         }

         return;
      }

      this.retreating = false;
      if (distanceSqr < this.attackRadiusSqr && canSee && this.seeTime >= AIM_GATE) {
         this.guard.getNavigation().stop();
      } else {
         this.guard.getNavigation().moveTo(target, moveSpeed(APPROACH_SPEED));
      }
   }

   private void tryToFire(LivingEntity target, ItemStack heldItem, Gun gun, double distanceSqr, boolean canSee) {
      if (this.timeUntilShoot > 0) {
         this.timeUntilShoot--;
      }

      if (!canSee || this.retreating || this.seeTime < AIM_GATE || this.timeUntilShoot > 0) {
         return;
      }

      if (distanceSqr > this.fireRangeSqr) {
         // Too far to be worth a shot: keep closing. Without this the guard fired at anything it could see.
         return;
      }

      if (this.blockedTicks > 0) {
         this.blockedTicks--;
         return;
      }

      if (this.allyInLineOfFire(target)) {
         // An ally is in the way: step aside and hold fire for a moment. The cooldown matters - re-testing every
         // tick made the guard shuffle in place instead of walking to a clear angle, which read as a broken
         // sidestep.
         this.stepAside(target);
         this.blockedTicks = BLOCKED_RETRY_TICKS;
         return;
      }

      if (MobGunFire.fire(this.guard, target, heldItem, this.accuracyModifier)) {
         this.shotsSinceReload++;
         this.guard.getPersistentData().putInt(TAG_SHOTS, this.shotsSinceReload);
         this.timeUntilShoot = MobGunFire.fireInterval(heldItem, gun);
      } else {
         this.timeUntilShoot = AIM_GATE;
      }
   }

   /**
    * Crouches while the guard is lining up a shot, and only while this goal is the thing that posed it.
    *
    * <p>Guards have no gun-holding animation: applying the player's arm pose to a mob leaves the hands doing one
    * thing and the body another, which looked worse than no animation at all (tested). The crouch, though, is a
    * pose the vanilla model already knows how to draw, so a guard that is aiming reads as aiming without touching
    * a single animation curve. The hitbox is unchanged - {@code getDimensions} only shrinks for players.</p>
    */
   private void updateAimPose(boolean aiming) {
      boolean crouchForReload = this.reloading
         && ((Boolean)top.ribs.scguns.Config.COMMON.compat.guardGunCrouchWhenReloading.get()).booleanValue();
      boolean crouchForAim = aiming
         && ((Boolean)top.ribs.scguns.Config.COMMON.compat.guardGunCrouchWhenShooting.get()).booleanValue();
      boolean wanted = (crouchForReload || crouchForAim)
         && !this.guard.isInWater()
         && !this.guard.isPassenger()
         && !this.guard.isSleeping();

      if (wanted) {
         if (this.guard.getPose() != Pose.CROUCHING) {
            this.guard.setPose(Pose.CROUCHING);
         }

         this.posedCrouch = true;
         return;
      }

      if (this.posedCrouch) {
         this.posedCrouch = false;
         if (this.guard.getPose() == Pose.CROUCHING) {
            this.guard.setPose(Pose.STANDING);
         }
      }
   }

   /**
    * Whether an ally is standing in the firing line.
    *
    * <p>The first version sampled a box six blocks in front of the guard. A shot travels far further than that,
    * so a villager ten or fifteen blocks away, dead in the line of fire, was never sampled and the guard fired
    * straight through it. This walks the actual shot instead: the first living entity between the guard's eyes
    * and the target decides.</p>
    *
    * <p>Guard Villagers' own ranged goal only ever reasons about the mob it is aiming at - its musket path
    * ({@code ModCompat.shootGun}) fires along the look vector without testing what is in the way - so there is no
    * reference implementation to copy here. Being stricter than the reference is the point.</p>
    */
   private boolean allyInLineOfFire(LivingEntity target) {
      if (!((Boolean)top.ribs.scguns.Config.COMMON.compat.guardFriendlyFire.get()).booleanValue()) {
         return false;
      }

      Vec3 from = this.guard.getEyePosition();
      Vec3 to = target.getBoundingBox().getCenter();
      EntityHitResult hit = ProjectileUtil.getEntityHitResult(
         this.guard.level(),
         this.guard,
         from,
         to,
         this.guard.getBoundingBox().expandTowards(to.subtract(from)).inflate(1.0),
         entity -> entity != this.guard && entity != target && entity instanceof LivingEntity
      );
      if (hit == null || !GuardVillagersCompat.isFriendlyShot(this.guard, hit.getEntity())) {
         return false;
      }

      // The reference also requires the ally to sit within an angle of the shot, and exposes that as a config
      // cosine. The ray already proves the ally is on the line, so this only has to honour a tighter setting.
      Vec3 toAlly = hit.getEntity().position().subtract(this.guard.position()).normalize();
      double alignment = toAlly.dot(this.guard.getViewVector(1.0F));
      return alignment >= (Double)top.ribs.scguns.Config.COMMON.compat.guardFriendlyFireCheckValue.get();
   }

   private void stepAside(LivingEntity target) {
      Vec3 aside = LandRandomPos.getPosAway(this.guard, 8, 6, target.position());
      if (aside != null) {
         this.guard.getNavigation().moveTo(aside.x, aside.y, aside.z, moveSpeed(RETREAT_SPEED));
      }

      this.timeUntilShoot = Math.max(this.timeUntilShoot, 10);
   }

   /**
    * Whether the magazine is spent.
    *
    * <p>This counts shots instead of trusting {@code AmmoCount} on the stack. A gun handed out by an equipment loot
    * table - which is how a guard gets its gun now - carries no custom data at all, so the stack reported zero
    * rounds, and any write to it could be lost depending on which copy the mob's hand held. Counting here makes the
    * cycle deterministic: fire {@code maxAmmo} shots, reload, repeat. The stack is still topped up on reload so a
    * player who takes the gun sees a sensible magazine.</p>
    */
   private boolean isMagazineEmpty(ItemStack heldItem, Gun gun) {
      // Either signal means the magazine is spent. The stack is authoritative when it carries an ammo count, and a
      // gun handed out by an equipment loot table carries none at all - which reads as zero rounds, so it has to
      // count as empty or the guard would stand there with an empty gun that never reloads: the fire path refuses
      // to shoot with no ammo, and this test said the magazine was fine.
      if (GunAttackGoal.getAmmoCount(heldItem) <= 0) {
         return true;
      }

      return this.shotsSinceReload >= this.magazineCapacity(gun);
   }

   /**
    * How many shots the gun holds before it has to be reloaded.
    *
    * <p>Read from the reload type, not just {@code maxAmmo}. A single-item gun loads one round and fires one round,
    * so its capacity is one no matter what the data says; a magazine-fed or manual gun uses {@code maxAmmo}, and a
    * zero or missing value is treated as one instead of producing a gun that reloads forever without ever
    * finishing.</p>
    */
   private int magazineCapacity(Gun gun) {
      var reloads = gun.getReloads();

      if (reloads.getReloadType() == ReloadType.SINGLE_ITEM) {
         return 1;
      }

      return Math.max(1, reloads.getMaxAmmo());
   }

   /** Guard Villagers' own pace for this action, scaled by the config multiplier (HANDOFF section 82.19). */
   private static double moveSpeed(double base) {
      return base * (Double)Config.COMMON.compat.guardGunMoveSpeed.get();
   }

   /**
    * Reloads like every other gunner (HANDOFF sections 81, 82.9): the magazine is filled to the gun's
    * capacity after the gun's own reload timer, with the mod's reload and cock sounds. A guard that could
    * not reload would fire once and then stand there, which is the complaint section 81 came from.
    */
   private void reload(Gun gun) {
      if (!this.reloading) {
         this.reloading = true;
         // The gun's own reload time, exactly as a player would experience it: getRealReloadSpeed picks the
         // magazine path or the per-round path from the reload type, applies Quick Hands and the attachments that
         // change reload speed, and floors the result the same way. Using the raw reloadTimer ignored all of that.
         int reloadTicks;

         try {
            reloadTicks = GunEnchantmentHelper.getRealReloadSpeed(this.guard.getMainHandItem());
         } catch (Throwable broken) {
            // An addon gun with incomplete data: fall back to its raw timer rather than not reloading at all.
            reloadTicks = gun.getReloads().getReloadTimer();
         }

         this.reloadTick = Math.max(reloadTicks, MIN_RELOAD_TICKS);
         CompoundTag persist = this.guard.getPersistentData();
         persist.putBoolean(TAG_RELOADING, true);
         persist.putInt(TAG_RELOAD_TICKS, this.reloadTick);
         persist.putInt(TAG_RELOAD_STAMP, RELOAD_STATE_VERSION);
         this.guard
            .level()
            .playSound(
               null,
               this.guard.getX(),
               this.guard.getY(),
               this.guard.getZ(),
               ModSounds.ITEM_PISTOL_RELOAD.get(),
               SoundSource.HOSTILE,
               1.0F,
               1.0F
            );
         return;
      }

      if (this.reloadTick > 0) {
         this.guard.getNavigation().stop();
         this.reloadTick--;
         this.guard.getPersistentData().putInt(TAG_RELOAD_TICKS, this.reloadTick);
         return;
      }

      NbtHelper.getOrCreateTag(this.guard.getMainHandItem()).putInt("AmmoCount", gun.getReloads().getMaxAmmo());
      this.shotsSinceReload = 0;
      CompoundTag done = this.guard.getPersistentData();
      done.putBoolean(TAG_RELOADING, false);
      done.putInt(TAG_RELOAD_TICKS, 0);
      done.putInt(TAG_SHOTS, 0);
      this.guard
         .level()
         .playSound(
            null,
            this.guard.getX(),
            this.guard.getY(),
            this.guard.getZ(),
            ModSounds.ITEM_PISTOL_COCK.get(),
            SoundSource.HOSTILE,
            1.0F,
            1.0F
         );
      this.reloading = false;
   }
}
