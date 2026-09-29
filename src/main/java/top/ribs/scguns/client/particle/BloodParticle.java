package top.ribs.scguns.client.particle;

import top.ribs.scguns.Config;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import top.ribs.scguns.init.ModTags;

@OnlyIn(Dist.CLIENT)
public class BloodParticle extends TextureSheetParticle {
   /**
    * @param world client level
    * @param x spawn x
    * @param y spawn y
    * @param z spawn z
    *
    * <p><b>The spray is decided here, not by the caller.</b> Both call sites pass the three
    * "speed" parameters as something else entirely - the projectile path passes {@code 0.5, 0, 0.5}
    * and the beam path passes the weapon's beam colour - and the factory turns them into the
    * particle's <em>colour</em> via {@code setColor}, not into motion. So 0.5.5's motion is not
    * those numbers: it passes {@code 0.1, 0.1, 0.1} to the base constructor, and the base
    * constructor adds the vanilla {@code +-0.4} random spread to every axis. That is what the
    * droplets in 1.20.1 actually do.</p>
    *
    * <p><b>This class now reproduces exactly that (PORTING_STATUS section 82.37).</b> The player
    * compared the two platforms back to back - their 1.20.1 instance runs the very same
    * {@code ScorchedGuns-0.5.5-1.20.1.jar} this port is built from - and reported the spray working
    * there and missing here. Everything that could differ was checked and does not: the mod's own
    * blood code, the hit position, the packet and the assets are identical, and 1.21.1's
    * {@code Particle}, {@code SingleQuadParticle} and {@code TextureSheetParticle} are
    * instruction-for-instruction the 1.20.1 classes. So the distribution here is written out
    * explicitly - {@code 0.1 + (random * 2 - 1) * 0.4} on each axis, {@code gravity = 1.5}, and
    * {@code lifetime = 12 / (random * 0.9 + 0.1)} - which is 0.5.5's motion to the value, not an
    * approximation of it.</p>
    *
    * <p>The only addition is the {@code bloodParticleSpeed} multiplier: at its default of 1.0 this
    * is 0.5.5 exactly, and the player can scale the whole burst up if they want more of it. It is
    * applied to the finished velocity rather than to the {@code 0.1} seed, because the spread the
    * base constructor adds is not affected by the seed - scaling only the seed would leave the fan
    * at full width and quietly do nothing.</p>
    */
   public BloodParticle(ClientLevel world, double x, double y, double z) {
      super(world, x, y, z, 0.0, 0.0, 0.0);
      float speedMultiplier = Config.CLIENT.particle.bloodParticleSpeed.get().floatValue();
      // Each axis exactly as the vanilla Particle constructor would have set it from a 0.1 seed,
      // then scaled: the three draws are independent, which is what gives 0.5.5 its chaotic burst
      // rather than a neat ring.
      this.xd = (0.1 + (this.random.nextDouble() * 2.0 - 1.0) * 0.4) * speedMultiplier;
      this.yd = (0.1 + (this.random.nextDouble() * 2.0 - 1.0) * 0.4) * speedMultiplier;
      this.zd = (0.1 + (this.random.nextDouble() * 2.0 - 1.0) * 0.4) * speedMultiplier;
      this.gravity = 1.5F;
      this.quadSize = 0.1F;
      this.lifetime = (int)(12.0F / (this.random.nextFloat() * 0.9F + 0.1F));
      // TEMPORARY PROBE (removed before the commit): what the client actually does with a droplet.
      top.ribs.scguns.ScorchedGuns.LOGGER.info(
         "SCGUNS-BLOOD drop spawn y={} xd={} yd={} zd={} gravity={} life={}",
         this.y, this.xd, this.yd, this.zd, this.gravity, this.lifetime);
   }

   public void setCustomColor(float r, float g, float b, float a) {
      this.setColor(r, g, b);
      this.alpha = a;
   }

   public void setColorBasedOnEntity(EntityType<?> entityType) {
      if (entityType.is(ModTags.Entities.RED_BLOOD)) {
         this.setColor(0.541F, 0.027F, 0.027F);
      } else if (entityType.is(ModTags.Entities.WHITE_BLOOD)) {
         this.setColor(1.0F, 1.0F, 1.0F);
      } else if (entityType.is(ModTags.Entities.GREEN_BLOOD)) {
         this.setColor(0.0F, 1.0F, 0.0F);
      } else if (entityType.is(ModTags.Entities.BLUE_BLOOD)) {
         this.setColor(0.0F, 0.0F, 1.0F);
      } else if (entityType.is(ModTags.Entities.YELLOW_BLOOD)) {
         this.setColor(1.0F, 1.0F, 0.0F);
      } else if (entityType.is(ModTags.Entities.PURPLE_BLOOD)) {
         this.setColor(0.5F, 0.0F, 0.5F);
      } else if (entityType.is(ModTags.Entities.BLACK_BLOOD)) {
         this.setColor(0.0F, 0.0F, 0.0F);
      } else {
         this.setColor(0.541F, 0.027F, 0.027F);
      }
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public void tick() {
      super.tick();
      // TEMPORARY PROBE (removed before the commit): does the droplet move, and does it land?
      if (this.age % 4 == 0) {
         top.ribs.scguns.ScorchedGuns.LOGGER.info(
            "SCGUNS-BLOOD tick age={} y={} xd={} yd={} zd={} onGround={} quad={} removed={} renderType={}",
            this.age, this.y, this.xd, this.yd, this.zd, this.onGround, this.quadSize, this.removed,
            this.getRenderType());
      }
      if (this.onGround) {
         this.xd = 0.0;
         this.zd = 0.0;
         this.quadSize *= 0.95F;
      }
   }

   public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
      // TEMPORARY PROBE (removed before the commit): is the quad really being drawn, with a real
      // sprite? A degenerate sprite (u0 == u1) or a zero alpha would make it invisible.
      if (this.age <= 1 && this.sprite != null) {
         top.ribs.scguns.ScorchedGuns.LOGGER.info(
            "SCGUNS-BLOOD render age={} pos={},{},{} quad={} sprite={} u={}-{} v={}-{} light={} alpha={} rCol={}",
            this.age, this.x, this.y, this.z, this.getQuadSize(partialTicks), this.sprite.contents().name(),
            this.getU0(), this.getU1(), this.getV0(), this.getV1(), this.getLightColor(partialTicks),
            this.alpha, this.rCol);
      }
      Vec3 projectedView = renderInfo.getPosition();
      float x = (float)(Mth.lerp((double)partialTicks, this.xo, this.x) - projectedView.x());
      float y = (float)(Mth.lerp((double)partialTicks, this.yo, this.y) - projectedView.y());
      float z = (float)(Mth.lerp((double)partialTicks, this.zo, this.z) - projectedView.z());
      if (this.onGround) {
         y = (float)((double)y + 0.01);
      }

      Quaternionf rotation = Direction.NORTH.getRotation();
      if (this.roll == 0.0F) {
         if (!this.onGround) {
            rotation = renderInfo.rotation();
         }
      } else {
         rotation = new Quaternionf(renderInfo.rotation());
         float angle = Mth.lerp(partialTicks, this.oRoll, this.roll);
         rotation.mul(Axis.ZP.rotation(angle));
      }

      Vector3f[] vertices = new Vector3f[]{
         new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)
      };
      float scale = this.getQuadSize(partialTicks);

      for (int i = 0; i < 4; i++) {
         Vector3f vertex = vertices[i];
         vertex.rotate(rotation);
         vertex.mul(scale);
         vertex.add(x, y, z);
      }

      float minU = this.getU0();
      float maxU = this.getU1();
      float minV = this.getV0();
      float maxV = this.getV1();
      int light = this.getLightColor(partialTicks);
      buffer.addVertex((float)vertices[0].x(), (float)vertices[0].y(), (float)vertices[0].z())
         .setUv(maxU, maxV)
         .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
         .setLight(light)
         ;
      buffer.addVertex((float)vertices[1].x(), (float)vertices[1].y(), (float)vertices[1].z())
         .setUv(maxU, minV)
         .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
         .setLight(light)
         ;
      buffer.addVertex((float)vertices[2].x(), (float)vertices[2].y(), (float)vertices[2].z())
         .setUv(minU, minV)
         .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
         .setLight(light)
         ;
      buffer.addVertex((float)vertices[3].x(), (float)vertices[3].y(), (float)vertices[3].z())
         .setUv(minU, maxV)
         .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
         .setLight(light)
         ;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Factory implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet spriteSet;

      public Factory(SpriteSet spriteSet) {
         super();
         this.spriteSet = spriteSet;
      }

      public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         BloodParticle particle = new BloodParticle(worldIn, x, y, z);
         particle.setColor((float)xSpeed, (float)ySpeed, (float)zSpeed);
         particle.pickSprite(this.spriteSet);
         return particle;
      }
   }
}
