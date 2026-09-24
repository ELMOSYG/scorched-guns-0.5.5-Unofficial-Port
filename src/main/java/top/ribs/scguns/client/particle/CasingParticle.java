package top.ribs.scguns.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CasingParticle extends TextureSheetParticle {
   CasingParticle(ClientLevel pLevel, double pX, double pY, double pZ) {
      super(pLevel, pX, pY, pZ, 0.0, 0.0, 0.0);
      this.gravity = 0.75F;
      this.friction = 0.999F;
      this.hasPhysics = true;
      this.xd *= 0.8F;
      this.yd *= 0.8F;
      this.zd *= 0.8F;
      this.yd = (double)(this.random.nextFloat() * 0.225F + 0.22F);
      this.quadSize = 0.35F;
      this.lifetime = (int)(16.0 / (Math.random() * 0.8 + 0.2));
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   public float getQuadSize(float pScaleFactor) {
      float f = ((float)this.age + pScaleFactor) / (float)this.lifetime;
      return this.quadSize * (1.0F - f * f);
   }

   public void tick() {
      super.tick();
      if (!this.removed) {
         float f = (float)this.age / (float)this.lifetime;
         if (this.random.nextFloat() > f) {
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet pSprites) {
         super();
         this.sprite = pSprites;
      }

      public Particle createParticle(
         SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
      ) {
         CasingParticle casingParticle = new CasingParticle(pLevel, pX, pY, pZ);
         casingParticle.setSpriteFromAge(this.sprite);
         return casingParticle;
      }
   }
}
