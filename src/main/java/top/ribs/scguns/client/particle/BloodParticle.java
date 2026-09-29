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
    * particle's <em>colour</em> via {@code setColor}, not into motion. So the motion used to come
    * from a hardcoded {@code 0.1, 0.1, 0.1} here: a barely-visible isotropic scatter under
    * {@code gravity = 1.5}, which reads as the blood simply falling to the ground the instant it
    * appears. 0.5.5 does exactly the same thing, so this has never had a splash - it is not a port
    * regression, it was just never noticed because "no spray" looks like a working particle.</p>
    *
    * <p>So the velocity is set after {@code super} (the base class's {@code random} only exists by
    * then, and its speed arguments are consumed inside the base constructor): a random horizontal
    * direction, a spread that varies per particle so the spray has depth, and an upward bias so the
    * droplets arc and come back down instead of falling straight out of the hit.</p>
    *
    * <p>The player still read the result as "blood appears on the ground" (PORTING_STATUS section 82.35), and
    * the numbers say why: the whole fan was 0.12 - 0.30 blocks per tick, and
    * {@code lifetime = 12 / (0.1 .. 1.0)} let a droplet live up to <em>120</em> ticks, so a few long
    * lived ones hung around after landing and the burst read as one clump dropping. The fan is wider
    * now, gravity is gentler so the arc is visible, and the life is capped at half a second or so -
    * the splatter is the first few ticks, and what stays afterwards is the droplets lying on the
    * ground.</p>
    *
    * <p><b>The shape, not the size, was what was still wrong (PORTING_STATUS section 82.36).</b> Every
    * droplet used to be thrown almost flat and gravity pulled it down within a quarter of a second, so
    * there was never anything to see fall. Measured by {@code tools/audit_blood_trajectory.py}, which
    * replays the vanilla tick from these very constants: 0.5.5 (and the official 1.21.1 1.5.2, and
    * every other port - they are all the same here) rises <b>0.10</b> blocks above the wound and stays
    * airborne for <b>5.5</b> ticks, while travelling 1.5 blocks sideways. That is a horizontal squirt
    * that is already lying on the ground before the eye can follow it. The droplets are now thrown
    * mostly <em>upward</em> - 0.25 - 0.50 against 0.06 - 0.26 sideways, under gravity 1.0 - so they
    * rise about <b>1.4</b> blocks, hang and arc for <b>17.5</b> ticks (0.9 s) and come down 2.6 blocks
    * out, with nine of the twelve landing while they are still alive.</p>
    */
   public BloodParticle(ClientLevel world, double x, double y, double z) {
      super(world, x, y, z, 0.0, 0.0, 0.0);
      float speedMultiplier = Config.CLIENT.particle.bloodParticleSpeed.get().floatValue();
      double angle = this.random.nextDouble() * Math.PI * 2.0;
      double spread = (0.06 + this.random.nextDouble() * 0.20) * speedMultiplier;
      this.xd = Math.cos(angle) * spread;
      this.zd = Math.sin(angle) * spread;
      this.yd = (0.25 + this.random.nextDouble() * 0.25) * speedMultiplier;
      this.gravity = 1.0F;
      this.quadSize = 0.1F;
      this.lifetime = 16 + this.random.nextInt(14);
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
      if (this.onGround) {
         this.xd = 0.0;
         this.zd = 0.0;
         this.quadSize *= 0.95F;
      }
   }

   public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
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
