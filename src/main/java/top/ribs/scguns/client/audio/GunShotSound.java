package top.ribs.scguns.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import top.ribs.scguns.Config;

public class GunShotSound extends AbstractSoundInstance {
   public GunShotSound(
      ResourceLocation soundIn, SoundSource categoryIn, float x, float y, float z, float volume, float pitch, boolean reload, RandomSource source
   ) {
      super(soundIn, categoryIn, source);
      this.x = (double)x;
      this.y = (double)y;
      this.z = (double)z;
      this.pitch = pitch;
      this.attenuation = Attenuation.NONE;
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         float distance = reload ? ((Double)Config.SERVER.reloadMaxDistance.get()).floatValue() : ((Double)Config.SERVER.gunShotMaxDistance.get()).floatValue();
         this.volume = volume * (1.0F - Math.min(1.0F, (float)Math.sqrt(player.distanceToSqr((double)x, (double)y, (double)z)) / distance));
         this.volume = this.volume * this.volume;
      }
   }
}
