package top.ribs.scguns.client.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.bus.api.SubscribeEvent;
import top.ribs.scguns.client.render.entity.BeamRenderer;
import top.ribs.scguns.common.FireMode;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModParticleTypes;
import top.ribs.scguns.item.GunItem;

@OnlyIn(Dist.CLIENT)
public class BeamHandler {
   public static final Map<UUID, BeamHandler.BeamInfo> activeBeams = new HashMap<>();
   private static final float SMOOTHING_FACTOR = 0.3F;
   private static final float INITIAL_SMOOTHING_FACTOR = 0.6F;
   private static final long CONTINUOUS_BEAM_DURATION_MS = 200L;
   private static final long SEMI_BEAM_DURATION_MS = 50L;
   private static final long CLEANUP_DELAY_MS = 50L;

   public BeamHandler() {
      super();
   }

   public static float[] getBeamColorForWeapon(ItemStack heldItem, Gun modifiedGun) {
      boolean isEnchanted = heldItem.isEnchanted();
      String primaryColorHex = isEnchanted && modifiedGun.getGeneral().getEnchantedBeamColor() != null
         ? modifiedGun.getGeneral().getEnchantedBeamColor()
         : modifiedGun.getGeneral().getBeamColor();
      String secondaryColorHex = isEnchanted && modifiedGun.getGeneral().getEnchantedSecondaryBeamColor() != null
         ? modifiedGun.getGeneral().getEnchantedSecondaryBeamColor()
         : modifiedGun.getGeneral().getSecondaryBeamColor();
      float[] primaryColor = parseBeamColor(primaryColorHex);
      float[] secondaryColor = parseBeamColor(secondaryColorHex);

      assert Minecraft.getInstance().level != null;

      float time = ((float)Minecraft.getInstance().level.getGameTime() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)) % 100.0F;
      float progress = (Mth.sin((float)((double)time / 100.0 * Math.PI * 2.0)) + 1.0F) / 2.0F;
      return interpolateColors(primaryColor, secondaryColor, progress);
   }

   public static void stopBeam(UUID playerId) {
      BeamHandler.BeamInfo beamInfo = activeBeams.get(playerId);
      if (beamInfo != null) {
         beamInfo.isBeamActive = false;
         long duration = beamInfo.isBeamFireMode ? 200L : 50L;
         beamInfo.expiryTime = System.currentTimeMillis() + duration;
      }
   }

   private static void cleanupInactiveBeams() {
      long currentTime = System.currentTimeMillis();
      Iterator<Entry<UUID, BeamHandler.BeamInfo>> iterator = activeBeams.entrySet().iterator();

      while (iterator.hasNext()) {
         Entry<UUID, BeamHandler.BeamInfo> entry = iterator.next();
         BeamHandler.BeamInfo beamInfo = entry.getValue();
         if (!beamInfo.isBeamActive && currentTime > beamInfo.expiryTime + 50L || currentTime - beamInfo.lastUpdateTime > 500L) {
            iterator.remove();
         }
      }
   }

   public static void updateBeam(UUID playerId, Vec3 startPos, Vec3 endPos) {
      BeamHandler.BeamInfo beamInfo = activeBeams.get(playerId);
      if (beamInfo != null) {
         beamInfo.updatePositions(startPos, endPos);
         beamInfo.isBeamActive = true;
      } else {
         assert Minecraft.getInstance().level != null;

         Player player = Minecraft.getInstance().level.getPlayerByUUID(playerId);
         boolean isBeamFireMode = false;
         if (player != null) {
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem gunItem) {
               Gun modifiedGun = gunItem.getModifiedGun(heldItem);
               if (modifiedGun != null) {
                  isBeamFireMode = modifiedGun.getGeneral().getFireMode().equals(FireMode.BEAM);
               }
            }
         }

         BeamHandler.BeamInfo newBeamInfo = new BeamHandler.BeamInfo(startPos, endPos, System.currentTimeMillis(), isBeamFireMode);
         newBeamInfo.isBeamActive = true;
         activeBeams.put(playerId, newBeamInfo);
      }
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null) {
            cleanupInactiveBeams();
            LocalPlayer player = mc.player;
            if (player != null) {
               PoseStack poseStack = event.getPoseStack();
               BufferSource bufferSource = mc.renderBuffers().bufferSource();
               setupRenderContext(poseStack, event.getPartialTick().getGameTimeDeltaPartialTick(false), player);
               renderActiveBeams(mc, event.getPartialTick().getGameTimeDeltaPartialTick(false), poseStack, bufferSource);
               bufferSource.endBatch();
               poseStack.popPose();
            }
         }
      }
   }

   private static void setupRenderContext(PoseStack poseStack, float partialTicks, LocalPlayer player) {
      ItemStack heldItem = player.getMainHandItem();
      if (heldItem.getItem() instanceof GunItem gunItem) {
         Gun modifiedGun = gunItem.getModifiedGun(heldItem);
         if (modifiedGun != null && modifiedGun.getGeneral().getFireMode().equals(FireMode.BEAM)) {
            player.bob = 0.0F;
            player.oBob = 0.0F;
            player.walkDist = 0.0F;
            player.walkDistO = 0.0F;
         }
      }

      Vec3 renderPos = calculateBasePosition(player, partialTicks);
      poseStack.pushPose();
      poseStack.translate(-renderPos.x, -renderPos.y, -renderPos.z);
   }

   private static void renderActiveBeams(Minecraft mc, float partialTicks, PoseStack poseStack, BufferSource bufferSource) {
      activeBeams.forEach((uuid, beamInfo) -> {
         if (shouldRenderBeam(beamInfo)) {
            assert mc.level != null;

            Player beamPlayer = mc.level.getPlayerByUUID(uuid);
            if (beamPlayer != null && !beamPlayer.isRemoved()) {
               ItemStack heldItem = beamPlayer.getMainHandItem();
               if (heldItem.getItem() instanceof GunItem gunItem) {
                  Gun modifiedGun = gunItem.getModifiedGun(heldItem);
                  if (modifiedGun != null) {
                     renderBeam(beamPlayer, beamInfo, modifiedGun, heldItem, partialTicks, poseStack, bufferSource);
                  }
               }
            }
         }
      });
   }

   private static boolean shouldRenderBeam(BeamHandler.BeamInfo beamInfo) {
      return beamInfo.isBeamActive || System.currentTimeMillis() <= beamInfo.expiryTime;
   }

   private static void renderBeam(
      Player beamPlayer, BeamHandler.BeamInfo beamInfo, Gun modifiedGun, ItemStack heldItem, float partialTicks, PoseStack poseStack, BufferSource bufferSource
   ) {
      float originalBob = beamPlayer.bob;
      float originalOBob = beamPlayer.oBob;
      float originalWalkDist = beamPlayer.walkDist;
      float originalWalkDistO = beamPlayer.walkDistO;
      if (heldItem.getItem() instanceof GunItem && ((GunItem)heldItem.getItem()).getModifiedGun(heldItem).getGeneral().getFireMode().equals(FireMode.BEAM)) {
         beamPlayer.bob = 0.0F;
         beamPlayer.oBob = 0.0F;
         beamPlayer.walkDist = 0.0F;
         beamPlayer.walkDistO = 0.0F;
      }

      float[] interpolatedColor = getBeamColorForWeapon(heldItem, modifiedGun);
      float smoothingFactor = System.currentTimeMillis() - beamInfo.startTime < 100L ? 0.6F : 0.3F;
      Vec3 beamOrigin = calculateBeamOrigin(beamPlayer, modifiedGun, partialTicks);
      beamInfo.smoothedStartPos = beamOrigin;
      Vec3 lookVec = Vec3.directionFromRotation(
         Mth.lerp(partialTicks, beamPlayer.xRotO, beamPlayer.getXRot()), Mth.lerp(partialTicks, beamPlayer.yRotO, beamPlayer.getYRot())
      );
      Vec3 targetEndPos = beamOrigin.add(lookVec.scale(beamInfo.endPos.subtract(beamInfo.startPos).length()));
      beamInfo.smoothedEndPos = smoothPosition(beamInfo.smoothedEndPos, targetEndPos, smoothingFactor);
      beamInfo.lastStartPos = beamInfo.smoothedStartPos;
      beamInfo.lastEndPos = beamInfo.smoothedEndPos;
      beamInfo.updateFade(partialTicks);
      Vec3 currentStart = beamInfo.smoothedStartPos;
      List<Vec3> renderPoints = new ArrayList<>(beamInfo.glassPenetrationPoints);
      renderPoints.add(beamInfo.smoothedEndPos);

      for (Vec3 nextPoint : renderPoints) {
         BeamRenderer.renderBeam(
            poseStack, bufferSource, partialTicks, currentStart, nextPoint, currentStart, nextPoint, interpolatedColor, beamInfo.fadeProgress
         );
         currentStart = nextPoint;
      }

      beamPlayer.bob = originalBob;
      beamPlayer.oBob = originalOBob;
      beamPlayer.walkDist = originalWalkDist;
      beamPlayer.walkDistO = originalWalkDistO;
   }

   private static Vec3 calculateBeamOrigin(Player player, Gun modifiedGun, float partialTicks) {
      Minecraft mc = Minecraft.getInstance();
      boolean isThirdPerson = mc.options.getCameraType().ordinal() > 0;
      return !isThirdPerson
         ? calculateFirstPersonBeamOrigin(player, modifiedGun, partialTicks)
         : calculateThirdPersonBeamOrigin(player, modifiedGun, partialTicks);
   }

   private static Vec3 calculateFirstPersonBeamOrigin(Player player, Gun modifiedGun, float partialTicks) {
      Vec3 basePos = calculateBasePosition(player, partialTicks);
      Vec3 lookVec = Vec3.directionFromRotation(
         Mth.lerp(partialTicks, player.xRotO, player.getXRot()), Mth.lerp(partialTicks, player.yRotO, player.getYRot())
      );
      Vec3 upVec = Vec3.directionFromRotation(
         Mth.lerp(partialTicks, player.xRotO, player.getXRot()) - 90.0F, Mth.lerp(partialTicks, player.yRotO, player.getYRot())
      );
      Vec3 rightVec = lookVec.cross(upVec).normalize();
      float aimProgress = AimingHandler.get().getAimProgress(player, partialTicks);
      double[] offsets = calculateBeamOffsets(modifiedGun, aimProgress);
      return basePos.add(rightVec.scale(offsets[0])).add(upVec.scale(offsets[1])).add(lookVec.scale(offsets[2]));
   }

   private static Vec3 calculateThirdPersonBeamOrigin(Player player, Gun modifiedGun, float partialTicks) {
      Vec3 basePos = new Vec3(
         Mth.lerp((double)partialTicks, player.xo, player.getX()),
         Mth.lerp((double)partialTicks, player.yo, player.getY()) + (double)player.getEyeHeight() - 0.2,
         Mth.lerp((double)partialTicks, player.zo, player.getZ())
      );
      Vec3 lookVec = Vec3.directionFromRotation(
         Mth.lerp(partialTicks, player.xRotO, player.getXRot()), Mth.lerp(partialTicks, player.yRotO, player.getYRot())
      );
      Vec3 upVec = Vec3.directionFromRotation(
         Mth.lerp(partialTicks, player.xRotO, player.getXRot()) - 90.0F, Mth.lerp(partialTicks, player.yRotO, player.getYRot())
      );
      Vec3 rightVec = lookVec.cross(upVec).normalize();
      float aimProgress = AimingHandler.get().getAimProgress(player, partialTicks);
      calculateBeamOffsets(modifiedGun, aimProgress);
      return basePos.add(rightVec.scale(0.1)).add(upVec.scale(-0.1)).add(lookVec.scale(2.15));
   }

   private static Vec3 calculateBasePosition(Player player, float partialTicks) {
      double x = Mth.lerp((double)partialTicks, player.xo, player.getX());
      double y = Mth.lerp((double)partialTicks, player.yo, player.getY()) + (double)player.getEyeHeight();
      double z = Mth.lerp((double)partialTicks, player.zo, player.getZ());
      return new Vec3(x, y, z);
   }

   private static double[] calculateBeamOffsets(Gun modifiedGun, float aimProgress) {
      Gun.Display.BeamOrigin beamOriginConfig = modifiedGun.getDisplay().getBeamOrigin();
      double horizontalOffset;
      double verticalOffset;
      double forwardOffset;
      if (beamOriginConfig != null) {
         horizontalOffset = Mth.lerp((double)aimProgress, beamOriginConfig.getHorizontalOffset(), beamOriginConfig.getAimHorizontalOffset());
         verticalOffset = beamOriginConfig.getVerticalOffset();
         forwardOffset = beamOriginConfig.getForwardOffset();
      } else {
         horizontalOffset = Mth.lerp((double)aimProgress, 0.1, 0.0);
         verticalOffset = -0.1;
         forwardOffset = 0.3;
      }

      return new double[]{horizontalOffset, verticalOffset, forwardOffset};
   }

   private static Vec3 smoothPosition(Vec3 previous, Vec3 current, float smoothingFactor) {
      return previous == null
         ? current
         : new Vec3(
            Mth.lerp((double)smoothingFactor, previous.x, current.x),
            Mth.lerp((double)smoothingFactor, previous.y, current.y),
            Mth.lerp((double)smoothingFactor, previous.z, current.z)
         );
   }

   private static float[] interpolateColors(float[] colorA, float[] colorB, float progress) {
      float r = Mth.lerp(progress, colorA[0], colorB[0]);
      float g = Mth.lerp(progress, colorA[1], colorB[1]);
      float b = Mth.lerp(progress, colorA[2], colorB[2]);
      float a = Mth.lerp(progress, colorA[3], colorB[3]);
      return new float[]{r, g, b, a};
   }

   private static float[] parseBeamColor(String beamColorHex) {
      if (beamColorHex != null && !beamColorHex.isEmpty()) {
         try {
            if (beamColorHex.startsWith("#")) {
               beamColorHex = beamColorHex.substring(1);
            }

            long colorLong = Long.parseLong(beamColorHex, 16);
            float a;
            float r;
            float g;
            float b;
            if (beamColorHex.length() == 8) {
               a = (float)(colorLong >> 24 & 255L) / 255.0F;
               r = (float)(colorLong >> 16 & 255L) / 255.0F;
               g = (float)(colorLong >> 8 & 255L) / 255.0F;
               b = (float)(colorLong & 255L) / 255.0F;
            } else {
               a = 1.0F;
               r = (float)(colorLong >> 16 & 255L) / 255.0F;
               g = (float)(colorLong >> 8 & 255L) / 255.0F;
               b = (float)(colorLong & 255L) / 255.0F;
            }

            return new float[]{r, g, b, a};
         } catch (NumberFormatException var7) {
            return new float[]{1.0F, 1.0F, 1.0F, 1.0F};
         }
      } else {
         return new float[]{1.0F, 1.0F, 1.0F, 1.0F};
      }
   }

   public static void spawnBeamImpactParticles(ClientLevel world, Vec3 hitPosition, Player player) {
      ItemStack heldItem = player.getMainHandItem();
      if (heldItem.getItem() instanceof GunItem gunItem) {
         Gun modifiedGun = gunItem.getModifiedGun(heldItem);
         float[] interpolatedColor = getBeamColorForWeapon(heldItem, modifiedGun);

         for (int i = 0; i < 10; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 0.2;
            double offsetY = (world.random.nextDouble() - 0.5) * 0.2;
            double offsetZ = (world.random.nextDouble() - 0.5) * 0.2;
            world.addParticle(
               (ParticleOptions)ModParticleTypes.BLOOD.get(),
               false,
               hitPosition.x + offsetX,
               hitPosition.y + offsetY,
               hitPosition.z + offsetZ,
               (double)interpolatedColor[0],
               (double)interpolatedColor[1],
               (double)interpolatedColor[2]
            );
         }
      }
   }

   public static class BeamInfo {
      public Vec3 startPos;
      public Vec3 endPos;
      public Vec3 lastStartPos;
      public Vec3 lastEndPos;
      public Vec3 smoothedStartPos;
      public Vec3 smoothedEndPos;
      public float lastYaw;
      public float lastPitch;
      public Vec3 lastPlayerPos;
      public long startTime;
      public long lastDamageTime;
      public long lastUpdateTime;
      public int ticksActive;
      public boolean isBeamActive;
      public long expiryTime;
      public boolean isBeamFireMode;
      public float fadeProgress = 0.0F;
      private static final float FADE_SPEED = 2.0F;
      public List<Vec3> glassPenetrationPoints;

      public BeamInfo(Vec3 startPos, Vec3 endPos, long startTime, boolean isBeamFireMode) {
         super();
         this.startPos = startPos;
         this.endPos = endPos;
         this.lastStartPos = startPos;
         this.lastEndPos = endPos;
         this.smoothedStartPos = startPos;
         this.smoothedEndPos = endPos;
         this.lastYaw = 0.0F;
         this.lastPitch = 0.0F;
         this.lastPlayerPos = new Vec3(0.0, 0.0, 0.0);
         this.startTime = startTime;
         this.lastDamageTime = startTime;
         this.lastUpdateTime = startTime;
         this.ticksActive = 0;
         this.expiryTime = 0L;
         this.isBeamFireMode = isBeamFireMode;
         this.glassPenetrationPoints = new ArrayList<>();
      }

      public void updatePositions(Vec3 newStartPos, Vec3 newEndPos) {
         this.lastStartPos = this.startPos;
         this.lastEndPos = this.endPos;
         this.startPos = newStartPos;
         this.endPos = newEndPos;
         this.lastUpdateTime = System.currentTimeMillis();
      }

      public void updateFade(float partialTicks) {
         if (!this.isBeamActive && this.fadeProgress < 1.0F) {
            this.fadeProgress = Math.min(1.0F, this.fadeProgress + 2.0F * partialTicks);
         } else if (this.isBeamActive) {
            this.fadeProgress = 0.0F;
         }
      }
   }
}
