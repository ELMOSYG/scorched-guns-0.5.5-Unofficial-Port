package top.ribs.scguns.interfaces;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;

public interface IWeapon {
   Item getWeapon();

   double getMoveSpeedAmp();

   int getAttackCooldown();

   int getWeaponLoadTime();

   float getProjectileSpeed();

   SoundEvent getShootSound();

   SoundEvent getLoadSound();

   void performRangedAttackIWeapon(Mob var1, double var2, double var4, double var6, float var8);

   boolean isLoaded();

   void setLoaded(int var1);
}
