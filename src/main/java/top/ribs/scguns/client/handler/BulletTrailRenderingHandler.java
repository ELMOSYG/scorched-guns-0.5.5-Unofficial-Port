package top.ribs.scguns.client.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.Clone;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Matrix4f;
import top.ribs.scguns.client.BulletTrail;
import top.ribs.scguns.init.ModTags;

public class BulletTrailRenderingHandler {
   private static BulletTrailRenderingHandler instance;
   private final Map<Integer, BulletTrail> bullets = new HashMap<>();

   public static BulletTrailRenderingHandler get() {
      if (instance == null) {
         instance = new BulletTrailRenderingHandler();
      }

      return instance;
   }

   private BulletTrailRenderingHandler() {
      super();
   }

   public void add(BulletTrail trail) {
      this.bullets.put(trail.getEntityId(), trail);
   }

   public void remove(int entityId) {
      this.bullets.remove(entityId);
   }

   @SubscribeEvent
   public void onClientTick(ClientTickEvent.Post event) {
      Level world = Minecraft.getInstance().level;
      if (world != null) {
         // 1.21 splits the client tick into ClientTickEvent.Pre/Post; this ran on
         // the END phase in 1.20.1, which is Post.
         this.bullets.values().forEach(BulletTrail::tick);
         this.bullets.values().removeIf(BulletTrail::isDead);
      } else if (!this.bullets.isEmpty()) {
         this.bullets.clear();
      }
   }

   public void render(PoseStack stack, float partialSticks) {
      for (BulletTrail bulletTrail : this.bullets.values()) {
         this.renderBulletTrail(bulletTrail, stack, partialSticks);
      }
   }

   @SubscribeEvent
   public void onRespawn(Clone event) {
      this.bullets.clear();
   }

   @SubscribeEvent
   public void onLoggedOut(LoggingOut event) {
      this.bullets.clear();
   }

   private void renderBulletTrail(BulletTrail trail, PoseStack poseStack, float deltaTicks) {
      Minecraft mc = Minecraft.getInstance();
      Entity entity = mc.getCameraEntity();
      Level world = mc.level;
      if (entity != null && !trail.isDead() && world != null) {
         Entity projectileEntity = world.getEntity(trail.getEntityId());
         if (projectileEntity != null) {
            if (!projectileEntity.getType().is(ModTags.Entities.DISABLE_BULLET_TRAIL)) {
               if (trail.isTrailVisible()) {
                  poseStack.pushPose();
                  Vec3 view = mc.gameRenderer.getMainCamera().getPosition();
                  Vec3 position = trail.getPosition();
                  Vec3 motion = trail.getMotion();
                  double bulletX = position.x + motion.x * (double)deltaTicks;
                  double bulletY = position.y + motion.y * (double)deltaTicks;
                  double bulletZ = position.z + motion.z * (double)deltaTicks;
                  poseStack.translate(bulletX - view.x(), bulletY - view.y(), bulletZ - view.z());
                  poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(deltaTicks, trail.getYaw(), trail.getYaw()) - 90.0F));
                  poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(deltaTicks, trail.getPitch(), trail.getPitch())));
                  poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
                  poseStack.scale(0.05625F, 0.05625F, 0.05625F);
                  poseStack.translate(-4.0F, 0.0F, 0.0F);
                  BufferSource renderTypeBuffer = mc.renderBuffers().bufferSource();
                  VertexConsumer vertexConsumer = renderTypeBuffer.getBuffer(RenderType.energySwirl(this.getTexture(projectileEntity), 0.0F, 0.15625F));
                  Pose posestack$pose = poseStack.last();
                  Matrix4f matrix4f = posestack$pose.pose();
                  double speed = Math.sqrt(motion.x * motion.x + motion.y * motion.y + motion.z * motion.z);
                  float speedFactor = (float)Math.max(1.0, speed * 0.4);
                  int baseSize = 30;
                  int size = (int)Math.min(
                     (double)((trail.getAge() + 1) * 30) * trail.getTrailThickness() * (double)speedFactor,
                     (double)baseSize * trail.getTrailThickness() * (double)speedFactor
                  );
                  int color = trail.getTrailColor();
                  int red = color >> 16 & 0xFF;
                  int green = color >> 8 & 0xFF;
                  int blue = color & 0xFF;
                  float brightnessFactor = (float)Math.min(1.2, 1.0 + speed * 0.08);
                  red = Math.min(255, (int)((float)red * brightnessFactor));
                  green = Math.min(255, (int)((float)green * brightnessFactor));
                  blue = Math.min(255, (int)((float)blue * brightnessFactor));
                  int light = 15728880;
                  if (trail.isTrailVisible()) {
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, -1 - size, -1, -1, 0.0F, 0.15625F, -1, 0, 0, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, -1 - size, -1, 1, 0.15625F, 0.15625F, -1, 0, 0, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, -1 - size, 1, 1, 0.15625F, 0.3125F, -1, 0, 0, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, -1 - size, 1, -1, 0.0F, 0.3125F, -1, 0, 0, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, -1 - size, -1, -1, 0.0F, 0.15625F, -1, 0, 0, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, -1 - size, 1, -1, 0.0F, 0.3125F, -1, 0, 0, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, 1, 1, -1, 0.15625F, 0.3125F, 1, 0, 0, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, 1, -1, -1, 0.15625F, 0.15625F, 1, 0, 0, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, 1, -1, -1, 0.0F, 0.15625F, 0, 0, -1, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, 1, 1, -1, 0.0F, 0.3125F, 0, 0, -1, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, 1, 1, 1, 0.15625F, 0.3125F, 0, 0, -1, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, 1, -1, 1, 0.15625F, 0.15625F, 0, 0, -1, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, 1, -1, 1, 0.0F, 0.15625F, 1, 0, 0, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, 1, 1, 1, 0.0F, 0.3125F, 1, 0, 0, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, -1 - size, 1, 1, 0.15625F, 0.3125F, 1, 0, 0, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, -1 - size, -1, 1, 0.15625F, 0.15625F, 1, 0, 0, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, -1 - size, -1, 1, 0.0F, 0.15625F, 0, 0, 1, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, -1 - size, 1, 1, 0.0F, 0.3125F, 0, 0, 1, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, -1 - size, 1, -1, 0.15625F, 0.3125F, 0, 0, 1, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, -1 - size, -1, -1, 0.15625F, 0.15625F, 0, 0, 1, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, 1, 1, -1, 0.0F, 0.15625F, 1, 0, 0, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, 1, 1, 1, 0.15625F, 0.15625F, 1, 0, 0, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, 1, -1, 1, 0.15625F, 0.3125F, 1, 0, 0, light);
                     this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, 1, -1, -1, 0.0F, 0.3125F, 1, 0, 0, light);

                     for (int j = 0; j < 4; j++) {
                        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                        this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, -1 - size, -1, 1, 0.0F, 0.0F, 0, 1, 0, light);
                        this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, 1, -1, 1, 0.5F, 0.0F, 0, 1, 0, light);
                        this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, 1, 1, 1, 0.5F, 0.15625F, 0, 1, 0, light);
                        this.addVertex(red, green, blue, matrix4f, posestack$pose, vertexConsumer, -1 - size, 1, 1, 0.0F, 0.15625F, 0, 1, 0, light);
                     }
                  }

                  renderTypeBuffer.endBatch();
                  poseStack.popPose();
               }
            }
         }
      }
   }

   public ResourceLocation getTexture(Entity entity) {
      ResourceLocation id = EntityType.getKey(entity.getType());
      return ResourceLocation.parse(String.format("%s:textures/trail/%s.png", id.getNamespace(), id.getPath()));
   }

   public void addVertex(
      int red,
      int green,
      int blue,
      Matrix4f pMatrix,
      Pose pNormal,
      VertexConsumer pConsumer,
      int pX,
      int pY,
      int pZ,
      float pU,
      float pV,
      int pNormalX,
      int pNormalZ,
      int pNormalY,
      int pPackedLight
   ) {
      pConsumer.addVertex(pMatrix, (float)pX, (float)pY, (float)pZ)
         .setColor(red, green, blue, 255)
         .setUv(pU, pV)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(pPackedLight)
         .setNormal(pNormal, (float)pNormalX, (float)pNormalY, (float)pNormalZ)
         ;
   }

   @SubscribeEvent
   public void onRenderLevelStage(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         get().render(event.getPoseStack(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
      }
   }
}
