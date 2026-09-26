package top.ribs.scguns.entity.ai;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.event.GunEventBus;
import top.ribs.scguns.init.ModEnchantments;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.util.GunEnchantmentHelper;
import top.ribs.scguns.util.GunModifierHelper;
import top.ribs.scguns.util.NbtHelper;
import top.ribs.scguns.util.ScEnchants;

/**
 * One shot from a mob's gun, with everything the player's own firing path would do (HANDOFF section 82.8).
 *
 * <p>Ported from the maid compat's {@code SC2GunCompat}, which had to rebuild all of this by hand because a
 * maid never goes through the player's packet-driven firing path either. The guard gun goal was hand-rolling
 * its own version, and every one of these was missing from it:</p>
 *
 * <ul>
 *   <li><b>the fire rate chain.</b> {@code GunEnchantmentHelper.getRate} applies the three rate
 *       enchantments (Trigger Finger, Heavy Shot, Puncturing) and then the attachments, so calling it once
 *       covers both; {@code GunModifierHelper.getModifiedRate} on its own only ever saw attachments, which
 *       is why a maid's gun ignored rate enchantments. The raw {@code general.rate} the goal used before
 *       ignored both.</li>
 *   <li><b>the ammo rules.</b> The player's path honours {@code IgnoreAmmo} and the Reclaimed ("ghost
 *       round") enchantment, which gives a chance not to spend a round:
 *       {@code level == 0 || random.nextInt(4 - clamp(level, 1, 2)) != 0}. A mob's plain {@code -1} spent a
 *       round every time, so ghost rounds and ammo-free guns did nothing in a mob's hands.</li>
 *   <li><b>which fire sound.</b> A silenced gun has its own sound and an enchanted one too; the goal always
 *       played the plain shot.</li>
 *   <li><b>the casing</b> fell out of the gun's own config, on the same condition the mod uses for players
 *       ({@code ejectsCasing() && !ejectDuringReload()}).</li>
 * </ul>
 *
 * <p>It also does the small things that make a shot read as a shot: face the target, swing the arm, and let
 * the victim know who shot it (which is what makes the target fight back instead of standing there).</p>
 *
 * <p>Left out on purpose: the mod's own {@link GunAttackGoal} keeps its 0.5.5 behaviour for raiders, so
 * nothing about existing mobs changes until that is asked for - pointing it here is a two-line change.</p>
 */
public final class MobGunFire {
   private MobGunFire() {
   }

   /**
    * Ticks between shots for a mob: the gun's rate after enchantments and attachments, scaled by the same
    * {@code mobFireRateMultiplier} every other gunner obeys.
    */
   public static int fireInterval(ItemStack stack, Gun gun) {
      int rate;
      try {
         rate = GunEnchantmentHelper.getRate(stack, gun);
      } catch (Throwable broken) {
         // An addon gun with incomplete data: fall back to the attachments-only path rather than not firing.
         rate = Math.max(1, GunModifierHelper.getModifiedRate(stack, gun.getGeneral().getRate()));
      }

      float multiplier = ((Double)Config.COMMON.gameplay.mobFireRateMultiplier.get()).floatValue();
      return Math.max(1, Math.round((float)rate * multiplier));
   }

   /** How much a full magazine holds, including magazine attachments. */
   public static int magazineSize(ItemStack stack, Gun gun) {
      return GunModifierHelper.getModifiedAmmoCapacity(stack, gun);
   }

   /**
    * Fires one shot, if the gun has one to fire. Returns whether a shot went off, so the caller's cadence
    * stays in charge of when the next attempt happens.
    */
   public static boolean fire(Mob shooter, LivingEntity target, ItemStack stack, float accuracy) {
      if (shooter.level().isClientSide() || !(stack.getItem() instanceof GunItem gunItem)) {
         return false;
      }

      if (!Gun.hasAmmo(stack)) {
         return false;
      }

      Gun gun = gunItem.getModifiedGun(stack);
      if (gun == null || gun.getGeneral() == null) {
         gun = gunItem.getGun();
      }

      rotateToFace(shooter, target);
      shooter.swing(InteractionHand.MAIN_HAND);
      AIGunEvent.performGunAttack(shooter, target, stack, gun, accuracy);

      if (target instanceof Mob mobTarget) {
         mobTarget.setLastHurtByMob(shooter);
         mobTarget.setTarget(shooter);
      }

      playFireSound(shooter, stack, gun);
      consumeAmmo(shooter, stack);

      if (gun.getProjectile() != null && gun.getProjectile().ejectsCasing()
         && !gun.getProjectile().ejectDuringReload()) {
         GunEventBus.ejectCasing(shooter.level(), shooter, false);
      }

      return true;
   }

   /** The player's ammo rule, verbatim from {@code ServerPlayHandler.consumeAmmo}. */
   private static void consumeAmmo(Mob shooter, ItemStack stack) {
      var tag = NbtHelper.getOrCreateTag(stack);
      if (tag.getBoolean("IgnoreAmmo")) {
         return;
      }

      int level = ScEnchants.level(stack, ModEnchantments.RECLAIMED);
      if (level == 0 || shooter.getRandom().nextInt(4 - Mth.clamp(level, 1, 2)) != 0) {
         tag.putInt("AmmoCount", Math.max(0, tag.getInt("AmmoCount") - 1));
      }
   }

   private static void playFireSound(Mob shooter, ItemStack stack, Gun gun) {
      if (gun.getSounds() == null) {
         return;
      }

      ResourceLocation sound = null;
      if (GunModifierHelper.isSilencedFire(stack)) {
         sound = gun.getSounds().getSilencedFire();
      } else if (stack.isEnchanted()) {
         sound = gun.getSounds().getEnchantedFire();
      }

      if (sound == null) {
         sound = gun.getSounds().getFire();
      }

      if (sound == null) {
         return;
      }

      float volume = (Integer)Config.COMMON.gameplay.mobGunfireVolume.get() - 0.5F;
      float pitch = 0.9F + shooter.level().getRandom().nextFloat() * 0.2F;
      shooter
         .level()
         .playSound(
            null,
            shooter.getX(),
            shooter.getY() + (double)shooter.getEyeHeight(),
            shooter.getZ(),
            SoundEvent.createVariableRangeEvent(sound),
            SoundSource.HOSTILE,
            volume,
            pitch
         );
   }

   private static void rotateToFace(Mob shooter, LivingEntity target) {
      double dx = target.getX() - shooter.getX();
      double dz = target.getZ() - shooter.getZ();
      double dy = target.getEyeY() - shooter.getEyeY();
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
      float pitch = (float)(-Math.atan2(dy, horizontal) * 180.0 / Math.PI);
      shooter.setYRot(yaw);
      shooter.setXRot(pitch);
      shooter.yHeadRot = yaw;
      shooter.xRotO = pitch;
   }
}
