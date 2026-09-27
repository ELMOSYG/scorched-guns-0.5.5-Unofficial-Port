package top.ribs.scguns.compat.guardvillagers;

import java.util.EnumSet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.entity.ai.AIType;
import top.ribs.scguns.entity.ai.GunAttackGoal;
import top.ribs.scguns.entity.ai.MobGunFire;
import top.ribs.scguns.init.ModSounds;
import top.ribs.scguns.item.GunItem;
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

   private static final double APPROACH_SPEED = 1.0;
   private static final double RETREAT_SPEED = 1.2;

   /** The target has to be visible this many ticks before the guard shoots, as the reference requires. */
   private static final int AIM_GATE = 5;

   private final T guard;
   private final float accuracyModifier;
   private final double attackRadiusSqr;
   private final int aimTicks;

   private int seeTime;
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
      this.timeUntilShoot = this.aimTicks;
      this.reloading = false;
      this.reloadTick = 0;
   }

   @Override
   public void stop() {
      this.guard.setAggressive(false);
      this.seeTime = 0;
      this.retreating = false;
      this.reloading = false;
      this.reloadTick = 0;
      this.guard.getNavigation().stop();
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

      if (this.isMagazineEmpty(heldItem)) {
         this.reload(gun);
         return;
      }

      this.position(target, distanceSqr, canSee);
      this.tryToFire(target, heldItem, gun, distanceSqr, canSee);
   }

   /** Back out of melee, hold the gun's range, or close in - the reference's positioning, kept as it was. */
   private void position(LivingEntity target, double distanceSqr, boolean canSee) {
      if (distanceSqr < RETREAT_RANGE_SQR) {
         this.retreating = true;
         if (this.guard.getNavigation().isDone()) {
            Vec3 away = LandRandomPos.getPosAway(this.guard, 10, 7, target.position());
            if (away != null) {
               this.guard.getNavigation().moveTo(away.x, away.y, away.z, RETREAT_SPEED);
            }
         }

         return;
      }

      this.retreating = false;
      if (distanceSqr < this.attackRadiusSqr && canSee && this.seeTime >= AIM_GATE) {
         this.guard.getNavigation().stop();
      } else {
         this.guard.getNavigation().moveTo(target, APPROACH_SPEED);
      }
   }

   private void tryToFire(LivingEntity target, ItemStack heldItem, Gun gun, double distanceSqr, boolean canSee) {
      if (this.timeUntilShoot > 0) {
         this.timeUntilShoot--;
      }

      if (!canSee || this.retreating || this.seeTime < AIM_GATE || this.timeUntilShoot > 0) {
         return;
      }

      if (this.allyInLineOfFire(target)) {
         // Do not shoot a villager in the back: step aside and hold fire for a moment.
         this.stepAside(target);
         return;
      }

      if (MobGunFire.fire(this.guard, target, heldItem, this.accuracyModifier)) {
         this.timeUntilShoot = MobGunFire.fireInterval(heldItem, gun);
      } else {
         this.timeUntilShoot = AIM_GATE;
      }
   }

   /** Whether an ally is standing in the firing line, using the same ally rule the projectile gate uses. */
   private boolean allyInLineOfFire(LivingEntity target) {
      Vec3 look = this.guard.getViewVector(1.0F);
      AABB reach = this.guard.getBoundingBox().expandTowards(look.scale(ALLY_CHECK_REACH)).inflate(1.0);
      for (Entity entity : this.guard.level().getEntities(this.guard, reach)) {
         if (entity == target || entity == this.guard) {
            continue;
         }

         if (!GuardVillagersCompat.isFriendlyShot(this.guard, entity)) {
            continue;
         }

         // `entity -> guard` dotted with the view direction is negative when the ally is in front of the
         // guard, which is the reference's own test.
         Vec3 towardsGuard = entity.position().vectorTo(this.guard.position()).normalize();
         if (towardsGuard.dot(look) < 0.0 && this.guard.hasLineOfSight(entity)) {
            return true;
         }
      }

      return false;
   }

   private void stepAside(LivingEntity target) {
      Vec3 aside = LandRandomPos.getPosAway(this.guard, 8, 6, target.position());
      if (aside != null) {
         this.guard.getNavigation().moveTo(aside.x, aside.y, aside.z, RETREAT_SPEED);
      }

      this.timeUntilShoot = Math.max(this.timeUntilShoot, 10);
   }

   private boolean isMagazineEmpty(ItemStack heldItem) {
      return GunAttackGoal.getAmmoCount(heldItem) <= 0;
   }

   /**
    * Reloads like every other gunner (HANDOFF sections 81, 82.9): the magazine is filled to the gun's
    * capacity after the gun's own reload timer, with the mod's reload and cock sounds. A guard that could
    * not reload would fire once and then stand there, which is the complaint section 81 came from.
    */
   private void reload(Gun gun) {
      if (!this.reloading) {
         this.reloading = true;
         this.reloadTick = gun.getReloads().getReloadTimer();
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
         return;
      }

      NbtHelper.getOrCreateTag(this.guard.getMainHandItem()).putInt("AmmoCount", gun.getReloads().getMaxAmmo());
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
