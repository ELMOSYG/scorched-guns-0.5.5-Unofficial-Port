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
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.Clone;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Matrix4f;
import top.ribs.scguns.Config;
import top.ribs.scguns.client.BulletTrail;
import top.ribs.scguns.init.ModTags;

public class BulletTrailRenderingHandler {
   private static BulletTrailRenderingHandler instance;
   private final Map<Integer, BulletTrail> bullets = new HashMap<>();
   // HANDOFF 82.12. RenderType.energySwirl() builds a brand new RenderType (and with it a
   // whole CompositeState and its state shards) on every call, and the texture path was
   // formatted and parsed from a string on every call as well - per trail, per frame. A
   // shotgun keeps up to 26 trails alive at once, so that was 26 of each per frame. Both
   // values are pure functions of the projectile's entity type, so they are cached here.
   // They stay valid across levels (a RenderType is only a description of render state),
   // which is why onRespawn/onLoggedOut do not clear them.
   private final Map<EntityType<?>, ResourceLocation> textureCache = new HashMap<>();
   private final Map<EntityType<?>, RenderType> renderTypeCache = new HashMap<>();

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
      if (this.bullets.isEmpty()) {
         return;
      }

      // HANDOFF 82.12: the batch used to be ended *inside* the per-trail call, so one
      // shotgun blast (26 pellets, 26 live trails) ended the batch 26 times per frame -
      // 26 draw calls and 26 fresh BufferBuilders, for geometry that belongs in one
      // buffer anyway now that every pellet of a gun resolves to the same cached
      // RenderType. TurretBulletTrailRenderingHandler never ended it per trail at all.
      BufferSource renderTypeBuffer = Minecraft.getInstance().renderBuffers().bufferSource();
      int configuredDelay = Config.clientOr(Config.CLIENT.display.bulletTrailRenderDelay);
      for (BulletTrail bulletTrail : this.bullets.values()) {
         this.renderBulletTrail(bulletTrail, stack, partialSticks, renderTypeBuffer, configuredDelay);
      }

      renderTypeBuffer.endBatch();
   }

   @SubscribeEvent
   public void onRespawn(Clone event) {
      this.bullets.clear();
   }

   @SubscribeEvent
   public void onLoggedOut(LoggingOut event) {
      this.bullets.clear();
   }

   private void renderBulletTrail(BulletTrail trail, PoseStack poseStack, float deltaTicks, BufferSource renderTypeBuffer, int configuredDelay) {
      Minecraft mc = Minecraft.getInstance();
      Entity entity = mc.getCameraEntity();
      Level world = mc.level;
      if (entity != null && !trail.isDead() && world != null) {
         Entity projectileEntity = world.getEntity(trail.getEntityId());
         if (projectileEntity != null) {
            if (!projectileEntity.getType().is(ModTags.Entities.DISABLE_BULLET_TRAIL)) {
               // HANDOFF 82.13: no trail during the first ticks after the shot. The beam hangs
               // behind its projectile, so while that projectile is still at the muzzle the beam
               // lies behind the shooter's own camera and sweeps across the screen. The old code
               // hid that by accident - every trail was advanced twice per client tick by the
               // duplicated registration removed in 82.12, which threw the beam clear before a
               // frame could show it. Waiting out the delay gets the same view honestly. Turret
               // trails carry no delay: the camera is never at a turret's muzzle.
               if (trail.getAge() < this.renderDelay(trail, configuredDelay)) {
                  return;
               }

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
                  VertexConsumer vertexConsumer = renderTypeBuffer.getBuffer(this.getRenderType(projectileEntity));
                  Pose posestack$pose = poseStack.last();
                  Matrix4f matrix4f = posestack$pose.pose();
                  double speed = Math.sqrt(motion.x * motion.x + motion.y * motion.y + motion.z * motion.z);
                  float speedFactor = (float)Math.max(1.0, speed * 0.4);
                  int baseSize = 30;
                  // HANDOFF 82.13: the drawn beam is a fixed multiple of the projectile's speed and
                  // the gun's configured trail thickness, which for a shotgun is a ~1.8 block bar
                  // per pellet - 26 of them across the view. This is the knob for shortening it.
                  double lengthMultiplier = Config.clientOr(Config.CLIENT.display.bulletTrailLengthMultiplier);
                  int size = (int)(Math.min(
                     (double)((trail.getAge() + 1) * 30) * trail.getTrailThickness() * (double)speedFactor,
                     (double)baseSize * trail.getTrailThickness() * (double)speedFactor
                  ) * lengthMultiplier);
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

                  poseStack.popPose();
               }
            }
         }
      }
   }

   /**
    * How long this trail stays invisible after the shot (HANDOFF 82.13).
    *
    * <p>The configured delay is what the player asked for, but two guns ship trails shorter than
    * it ({@code inquisitor} lives 8 ticks, {@code spitfire} 10), and a flat delay would erase their
    * trails completely. A trail therefore always keeps a few visible ticks at the end of its life.
    */
   private int renderDelay(BulletTrail trail, int configured) {
      return Math.min(configured, Math.max(0, trail.getMaxAge() - MIN_VISIBLE_TRAIL_TICKS));
   }

   /** The tail of a trail's life that the render delay may never take away (see {@link #renderDelay}). */
   private static final int MIN_VISIBLE_TRAIL_TICKS = 4;

   /** The trail RenderType for this projectile type, built once per type (HANDOFF 82.12). */
   private RenderType getRenderType(Entity entity) {
      EntityType<?> type = entity.getType();
      RenderType cached = this.renderTypeCache.get(type);
      if (cached == null) {
         cached = RenderType.energySwirl(this.getTexture(entity), 0.0F, 0.15625F);
         this.renderTypeCache.put(type, cached);
      }

      return cached;
   }

   public ResourceLocation getTexture(Entity entity) {
      EntityType<?> type = entity.getType();
      ResourceLocation cached = this.textureCache.get(type);
      if (cached == null) {
         ResourceLocation id = EntityType.getKey(type);
         cached = ResourceLocation.parse(String.format("%s:textures/trail/%s.png", id.getNamespace(), id.getPath()));
         this.textureCache.put(type, cached);
      }

      return cached;
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

   // HANDOFF 82.12: no onRenderLevelStage here. LevelRendererMixin already calls render()
   // once per frame, so subscribing to RenderLevelStageEvent.AFTER_PARTICLES as well drew
   // every trail twice there - and the duplicate NeoForge.EVENT_BUS registration in
   // ScorchedGuns made it twice again. The mixin is the hook that stays: it runs at the
   // point vanilla's own level PoseStack is known to be at its base state, which is the
   // space this renderer writes into.
}
