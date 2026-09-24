package top.ribs.scguns.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import top.ribs.scguns.Config;
import top.ribs.scguns.init.ModEffects;
import top.ribs.scguns.init.ModSounds;

public class StunRingingSound extends AbstractTickableSoundInstance {
   public StunRingingSound() {
      super((SoundEvent)ModSounds.ENTITY_STUN_GRENADE_RING.get(), SoundSource.MASTER, SoundInstance.createUnseededRandom());
      this.looping = true;
      this.attenuation = Attenuation.NONE;
      this.tick();
   }

   public void tick() {
      Player player = Minecraft.getInstance().player;
      if (player != null && player.isAlive()) {
         MobEffectInstance effect = player.getEffect(ModEffects.DEAFENED);
         if (effect != null) {
            this.x = (double)((float)player.getX());
            this.y = (double)((float)player.getY());
            this.z = (double)((float)player.getZ());
            float percent = Math.min((float)effect.getDuration() / (float)((Integer)Config.SERVER.soundFadeThreshold.get()).intValue(), 1.0F);
            this.volume = (float)((double)percent * (Double)Config.SERVER.ringVolume.get());
            return;
         }
      }

      this.stop();
   }
}
