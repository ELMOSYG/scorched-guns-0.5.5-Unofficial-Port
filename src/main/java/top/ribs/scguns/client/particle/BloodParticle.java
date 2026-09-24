package top.ribs.scguns.client.particle;

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
   public BloodParticle(ClientLevel world, double x, double y, double z) {
      super(world, x, y, z, 0.1, 0.1, 0.1);
      this.gravity = 1.5F;
      this.quadSize = 0.1F;
      this.lifetime = (int)(12.0F / (this.random.nextFloat() * 0.9F + 0.1F));
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
