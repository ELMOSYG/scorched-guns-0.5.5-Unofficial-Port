package top.ribs.scguns.interfaces;

import net.minecraft.world.item.ItemStack;

public interface IGunModifier {
   default float modifyFireSoundVolume(float volume) {
      return volume;
   }

   default float recoilModifier(ItemStack weapon) {
      return this.recoilModifier();
   }

   default double modifySensitivity(double sensitivity) {
      return sensitivity;
   }

   default float kickModifier(ItemStack weapon) {
      return this.kickModifier();
   }

   default boolean silencedFire() {
      return false;
   }

   default double modifyFireSoundRadius(double radius) {
      return radius;
   }

   default float additionalDamage() {
      return 0.0F;
   }

   default float modifyProjectileDamage(float damage) {
      return damage;
   }

   default float modifyDamageFalloffStart(float falloffStart) {
      return falloffStart;
   }

   default float modifyDamageFalloffEnd(float falloffEnd) {
      return falloffEnd;
   }

   default double modifyProjectileSpeed(double speed) {
      return speed;
   }

   default float modifyProjectileSpread(float spread) {
      return spread;
   }

   default double additionalProjectileGravity() {
      return 0.0;
   }

   default double modifyProjectileGravity(double gravity) {
      return gravity;
   }

   default int modifyProjectileLife(int life) {
      return life;
   }

   default float recoilModifier() {
      return 1.0F;
   }

   default float kickModifier() {
      return 1.0F;
   }

   default double modifyMuzzleFlashScale(double scale) {
      return scale;
   }

   default double modifyAimDownSightSpeed(double speed) {
      return speed;
   }

   default int modifyFireRate(int rate) {
      return rate;
   }

   default float criticalChance() {
      return 0.0F;
   }

   default double modifyReloadSpeed(double reloadSpeed) {
      return reloadSpeed;
   }

   default int modifyAmmoCapacity(int baseCapacity) {
      return baseCapacity;
   }

   default boolean isMeleeOnly() {
      return true;
   }

   default double modifyDrawSpeed(double speed) {
      return speed;
   }

   default double modifyMouseSensitivity(double sensitivity) {
      return sensitivity;
   }
}
