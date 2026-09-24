package top.ribs.scguns.item.animated;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item.Properties;

public class AnimatedScorchedEnergyGunItem extends AnimatedEnergyGunItem {
   public AnimatedScorchedEnergyGunItem(
      Properties properties,
      String path,
      SoundEvent reloadSoundMagOut,
      SoundEvent reloadSoundMagIn,
      SoundEvent reloadSoundEnd,
      SoundEvent boltPullSound,
      SoundEvent boltReleaseSound,
      int capacity
   ) {
      super(properties, path, reloadSoundMagOut, reloadSoundMagIn, reloadSoundEnd, boltPullSound, boltReleaseSound, capacity);
   }

   public boolean isFireResistant() {
      return true;
   }
}
