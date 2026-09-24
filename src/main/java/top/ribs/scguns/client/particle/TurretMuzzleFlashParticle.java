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
public class TurretMuzzleFlashParticle extends TextureSheetParticle {
   private final SpriteSet sprites;

   protected TurretMuzzleFlashParticle(ClientLevel pLevel, double pX, double pY, double pZ, double pQuadSizeMultiplier, SpriteSet pSprites) {
      super(pLevel, pX, pY, pZ, 0.0, 0.0, 0.0);
      this.lifetime = 2;
      this.quadSize = 0.2F * (1.0F - (float)pQuadSizeMultiplier * 0.5F);
      this.sprites = pSprites;
      this.setSpriteFromAge(pSprites);
      this.xd = 0.0;
      this.yd = 0.0;
      this.zd = 0.0;
   }

   public void tick() {
      super.tick();
      this.xd = 0.0;
      this.yd = 0.0;
      this.zd = 0.0;
      float lifeRatio = (float)this.age / (float)this.lifetime;
      if ((double)lifeRatio < 0.5) {
         this.quadSize = 0.5F + 0.5F * lifeRatio;
      } else {
         this.quadSize = 0.5F + 0.5F * (1.0F - lifeRatio);
      }

      this.setSpriteFromAge(this.sprites);
   }

   public int getLightColor(float pPartialTick) {
      return 15728880;
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_LIT;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet pSprites) {
         super();
         this.sprites = pSprites;
      }

      public Particle createParticle(
         SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
      ) {
         TurretMuzzleFlashParticle particle = new TurretMuzzleFlashParticle(pLevel, pX, pY, pZ, pXSpeed, this.sprites);
         particle.setLifetime(2);
         return particle;
      }
   }
}
