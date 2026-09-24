package top.ribs.scguns.client.handler;

import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.event.GunFireEvent;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.util.ExoSuitRecoilHelper;
import top.ribs.scguns.util.GunEnchantmentHelper;
import top.ribs.scguns.util.GunModifierHelper;

public class RecoilHandler {
   private static RecoilHandler instance;
   private final Random random = new Random();
   private double gunRecoilNormal;
   private double gunRecoilAngle;
   private float gunRecoilRandom;
   private float cameraRecoil;
   private float progressCameraRecoil;
   private boolean enableRecoil = true;
   private static int recoilRand;

   public static RecoilHandler get() {
      if (instance == null) {
         instance = new RecoilHandler();
      }

      return instance;
   }

   private RecoilHandler() {
      super();
   }

   public void updateConfig() {
      try {
         if (Config.SERVER != null && Config.SERVER.enableCameraRecoil != null) {
            this.enableRecoil = (Boolean)Config.SERVER.enableCameraRecoil.get();
         }
      } catch (IllegalStateException var2) {
      }
   }

   @SubscribeEvent
   public void preShoot(GunFireEvent.Pre event) {
      if (event.isClient()) {
         if (this.enableRecoil) {
            recoilRand = this.random.nextInt(2);
         }
      }
   }

   @SubscribeEvent
   public void onGunFire(GunFireEvent.Post event) {
      if (event.isClient()) {
         if (this.enableRecoil) {
            ItemStack heldItem = event.getStack();
            GunItem gunItem = (GunItem)heldItem.getItem();
            Gun modifiedGun = gunItem.getModifiedGun(heldItem);
            if (Minecraft.getInstance().player != null) {
               float baseRecoilAngle = modifiedGun.getProjectile().getRecoilAngle();
               float exoSuitModifiedRecoil = ExoSuitRecoilHelper.getModifiedRecoilAngle(Minecraft.getInstance().player, baseRecoilAngle);
               float recoilModifier = 1.0F - GunModifierHelper.getRecoilModifier(heldItem);
               if (Minecraft.getInstance().player != null) {
                  float enchantmentMultiplier = GunEnchantmentHelper.getRecoilModifier(Minecraft.getInstance().player, heldItem);
                  recoilModifier *= enchantmentMultiplier;
               }

               recoilModifier *= (float)this.getAdsRecoilReduction(modifiedGun);
               this.cameraRecoil = exoSuitModifiedRecoil * recoilModifier;
               this.progressCameraRecoil = 0.0F;
               this.gunRecoilRandom = this.random.nextFloat();
            }
         }
      }
   }

   @SubscribeEvent
   public void onRenderTick(RenderFrameEvent.Post event) {
      if (!(this.cameraRecoil <= 0.0F)) {
         if (this.enableRecoil) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
               // 0.5.5 read Minecraft#getDeltaFrameTime() -- the ticks elapsed this
               // frame. 1.21's equivalent is DeltaTracker#getGameTimeDeltaTicks().
               // Using the partial tick here made the camera recoil recovery
               // advance by an unrelated amount, so the climb and the return
               // fought each other instead of running in sequence.
               float recoilAmount = this.cameraRecoil * mc.getTimer().getGameTimeDeltaTicks() * 0.15F;
               float startProgress = this.progressCameraRecoil / this.cameraRecoil;
               float endProgress = (this.progressCameraRecoil + recoilAmount) / this.cameraRecoil;
               float pitch = mc.player.getXRot();
               float yaw = mc.player.getYRot();
               if (startProgress < 0.2F) {
                  mc.player.setXRot(pitch - (endProgress - startProgress) / 0.2F * this.cameraRecoil);
                  if (recoilRand == 1) {
                     mc.player.setYRot(yaw - (endProgress - startProgress) / 0.2F * this.cameraRecoil / 2.0F);
                  } else {
                     mc.player.setYRot(yaw + (endProgress - startProgress) / 0.2F * this.cameraRecoil / 2.0F);
                  }
               } else {
                  mc.player.setXRot(pitch + (endProgress - startProgress) / 0.8F * this.cameraRecoil);
                  if (recoilRand == 1) {
                     mc.player.setYRot(yaw + (endProgress - startProgress) / 0.8F * this.cameraRecoil / 2.0F);
                  } else {
                     mc.player.setYRot(yaw - (endProgress - startProgress) / 0.8F * this.cameraRecoil / 2.0F);
                  }
               }

               this.progressCameraRecoil += recoilAmount;
               if (this.progressCameraRecoil >= this.cameraRecoil) {
                  this.cameraRecoil = 0.0F;
                  this.progressCameraRecoil = 0.0F;
               }
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public void onRenderOverlay(RenderHandEvent event) {
      if (event.getHand() == InteractionHand.MAIN_HAND) {
         ItemStack heldItem = event.getItemStack();
         if (heldItem.getItem() instanceof GunItem gunItem) {
            Gun var11 = gunItem.getModifiedGun(heldItem);

            assert Minecraft.getInstance().player != null;

            ItemCooldowns tracker = Minecraft.getInstance().player.getCooldowns();
            float cooldown = tracker.getCooldownPercent(gunItem, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
            cooldown = cooldown >= var11.getGeneral().getRecoilDurationOffset()
               ? (cooldown - var11.getGeneral().getRecoilDurationOffset()) / (1.0F - var11.getGeneral().getRecoilDurationOffset())
               : 0.0F;
            if ((double)cooldown >= 0.8) {
               float amount = (1.0F - cooldown) / 0.2F;
               this.gunRecoilNormal = (double)(1.0F - --amount * amount * amount * amount);
            } else {
               float amount = cooldown / 0.8F;
               this.gunRecoilNormal = (double)amount < 0.5 ? (double)(2.0F * amount * amount) : (double)(-1.0F + (4.0F - 2.0F * amount) * amount);
            }

            float baseRecoilAngle = var11.getProjectile().getRecoilAngle();
            if (Minecraft.getInstance().player != null) {
               float exoSuitModifiedRecoil = ExoSuitRecoilHelper.getModifiedRecoilAngle(Minecraft.getInstance().player, baseRecoilAngle);
               float recoilModifier = 1.0F - GunModifierHelper.getRecoilModifier(heldItem);
               float enchantmentMultiplier = GunEnchantmentHelper.getRecoilModifier(Minecraft.getInstance().player, heldItem);
               recoilModifier *= enchantmentMultiplier;
               this.gunRecoilAngle = (double)(exoSuitModifiedRecoil * recoilModifier);
            } else {
               this.gunRecoilAngle = (double)baseRecoilAngle;
            }
         }
      }
   }

   public double getAdsRecoilReduction(Gun gun) {
      return 1.0 - (double)gun.getGeneral().getRecoilAdsReduction() * AimingHandler.get().getNormalisedAdsProgress();
   }

   public double getGunRecoilNormal() {
      return this.gunRecoilNormal;
   }

   public double getGunRecoilAngle() {
      return this.gunRecoilAngle;
   }

   public float getGunRecoilRandom() {
      return this.gunRecoilRandom;
   }
}
