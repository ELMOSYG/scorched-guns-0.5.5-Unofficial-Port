package top.ribs.scguns.client.handler;

import java.lang.reflect.Field;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.ChannelAccess.ChannelHandle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import top.ribs.scguns.Config;
import top.ribs.scguns.client.audio.StunRingingSound;
import top.ribs.scguns.init.ModEffects;

public class SoundHandler {
   private static SoundHandler instance;
   private final Map<SoundInstance, Float> soundVolumes = new ConcurrentHashMap<>();
   private boolean isDeafened;
   private Field playingSounds;
   private SoundEngine soundEngine;
   private StunRingingSound ringing;

   public static SoundHandler get() {
      if (instance == null) {
         instance = new SoundHandler();
      }

      return instance;
   }

   private SoundHandler() {
      super();
      this.initReflection();
   }

   private void initReflection() {
      this.playingSounds = ObfuscationReflectionHelper.findField(SoundEngine.class, "instanceToChannel");
   }

   @SubscribeEvent
   public void deafenPlayer(ClientTickEvent.Post event) {
      if (Minecraft.getInstance().player != null && this.soundEngine != null) {
         MobEffectInstance effect = Minecraft.getInstance().player.getEffect(ModEffects.DEAFENED);
         if (effect != null || this.isDeafened) {
            if (!((Double)Config.SERVER.ringVolume.get() > 0.0) || this.ringing != null && Minecraft.getInstance().getSoundManager().isActive(this.ringing)) {
               Map<SoundInstance, ChannelHandle> playingSounds;
               try {
                  playingSounds = (Map<SoundInstance, ChannelHandle>)this.playingSounds.get(this.soundEngine);
               } catch (IllegalAccessException | IllegalArgumentException var8) {
                  return;
               }

               if (effect != null) {
                  try {
                     playingSounds.forEach((sound, entryx) -> {
                        if (sound != null && !(sound instanceof TickableSoundInstance) && !this.isStunGrenade(sound.getSound().getLocation())) {
                           float volume = sound instanceof SoundHandler.SoundMuted ? ((SoundHandler.SoundMuted)sound).getVolumeInitial() : sound.getVolume();
                           this.soundVolumes.put(sound, volume);
                           entryx.execute(soundSource -> soundSource.setVolume(this.getMutedVolume((float)effect.getDuration(), volume)));
                        }
                     });
                  } catch (ConcurrentModificationException var7) {
                  }

                  this.isDeafened = true;
               } else if (this.isDeafened) {
                  this.isDeafened = false;

                  for (Entry<SoundInstance, Float> entry : this.soundVolumes.entrySet()) {
                     ChannelHandle entry1 = playingSounds.get(entry.getKey());
                     if (entry1 != null) {
                        entry1.execute(soundSource -> soundSource.setVolume(entry.getValue()));
                     }
                  }

                  this.soundVolumes.clear();
               }
            } else {
               this.ringing = new StunRingingSound();
               Minecraft.getInstance().getSoundManager().play(this.ringing);
            }
         }
      }
   }

   @SubscribeEvent
   public void lowerInitialVolume(PlaySoundEvent event) {
      if (this.soundEngine == null) {
         this.soundEngine = event.getEngine();
      }

      if (this.isDeafened && Minecraft.getInstance().player != null && !(event.getSound() instanceof TickableSoundInstance)) {
         ResourceLocation loc = event.getSound().getLocation();
         MobEffectInstance effect = Minecraft.getInstance().player.getEffect(ModEffects.DEAFENED);
         int duration = effect != null ? effect.getDuration() : 0;
         boolean isStunGrenade = this.isStunGrenade(loc);
         if (duration != 0 || !isStunGrenade) {
            event.getSound().resolve(Minecraft.getInstance().getSoundManager());
            event.setSound(new SoundHandler.SoundMuted(event.getSound(), duration, isStunGrenade));
         }
      }
   }

   private boolean isStunGrenade(ResourceLocation loc) {
      return loc.toString().equals("scguns:grenade_stun_explosion");
   }

   private float getMutedVolume(float duration, float volumeBase) {
      float volumeMin = (float)((double)volumeBase * (Double)Config.SERVER.soundPercentage.get());
      float percent = Math.min(duration / (float)((Integer)Config.SERVER.soundFadeThreshold.get()).intValue(), 1.0F);
      return volumeMin + (1.0F - percent) * (volumeBase - volumeMin);
   }

   public static class SoundMuted implements SoundInstance {
      private final SoundInstance parent;
      private final float volume;
      private float volumeInitial;

      public SoundMuted(SoundInstance parent, int duration, boolean isStunGrenade) {
         super();
         this.parent = parent;
         this.volumeInitial = Mth.clamp(parent.getVolume(), 0.0F, 1.0F);
         this.volume = SoundHandler.get().getMutedVolume((float)duration, this.volumeInitial);
         if (isStunGrenade) {
            this.volumeInitial = this.volume;
         }
      }

      public float getVolume() {
         return this.volume;
      }

      public float getVolumeInitial() {
         return this.volumeInitial;
      }

      public ResourceLocation getLocation() {
         return this.parent.getLocation();
      }

      @Nullable
      public WeighedSoundEvents resolve(SoundManager handler) {
         return this.parent.resolve(handler);
      }

      public Sound getSound() {
         return this.parent.getSound();
      }

      public SoundSource getSource() {
         return this.parent.getSource();
      }

      public boolean isLooping() {
         return this.parent.isLooping();
      }

      public boolean isRelative() {
         return false;
      }

      public int getDelay() {
         return this.parent.getDelay();
      }

      public float getPitch() {
         return this.parent.getPitch();
      }

      public double getX() {
         return this.parent.getX();
      }

      public double getY() {
         return this.parent.getY();
      }

      public double getZ() {
         return this.parent.getZ();
      }

      public Attenuation getAttenuation() {
         return this.parent.getAttenuation();
      }
   }
}
