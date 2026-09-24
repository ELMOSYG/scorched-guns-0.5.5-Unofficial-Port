package top.ribs.scguns.item.animated;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item.Properties;

public class AnimatedDualWieldGunItem extends AnimatedGunItem {
   public AnimatedDualWieldGunItem(
      Properties properties,
      String path,
      SoundEvent reloadSoundMagOut,
      SoundEvent reloadSoundMagIn,
      SoundEvent reloadSoundEnd,
      SoundEvent boltPullSound,
      SoundEvent boltReleaseSound
   ) {
      super(properties, path, reloadSoundMagOut, reloadSoundMagIn, reloadSoundEnd, boltPullSound, boltReleaseSound);
   }

   public boolean isFireResistant() {
      return true;
   }
}
