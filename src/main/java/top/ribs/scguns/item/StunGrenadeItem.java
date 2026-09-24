package top.ribs.scguns.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import top.ribs.scguns.entity.throwable.ThrowableGrenadeEntity;
import top.ribs.scguns.entity.throwable.ThrowableStunGrenadeEntity;
import top.ribs.scguns.init.ModSounds;

public class StunGrenadeItem extends GrenadeItem {
   public StunGrenadeItem(Properties properties, int maxCookTime) {
      super(properties, maxCookTime);
   }

   @Override
   public ThrowableGrenadeEntity create(Level world, LivingEntity entity, int timeLeft) {
      return new ThrowableStunGrenadeEntity(world, entity, 40);
   }

   @Override
   public boolean canCook() {
      return false;
   }

   @Override
   protected void onThrown(Level world, ThrowableGrenadeEntity entity) {
      world.playSound(
         null, entity.getX(), entity.getY(), entity.getZ(), (SoundEvent)ModSounds.ITEM_GRENADE_PIN.get(), SoundSource.PLAYERS, 1.0F, 1.0F
      );
   }
}
