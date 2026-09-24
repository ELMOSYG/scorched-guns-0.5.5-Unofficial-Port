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
public class PlasmaExplosionParticle extends TextureSheetParticle {
   private final SpriteSet sprites;

   protected PlasmaExplosionParticle(ClientLevel pLevel, double pX, double pY, double pZ, double pQuadSizeMultiplier, SpriteSet pSprites) {
      super(pLevel, pX, pY, pZ, 0.0, 0.0, 0.0);
      this.lifetime = 3 + this.random.nextInt(5);
      this.quadSize = 1.1F * (1.0F - (float)pQuadSizeMultiplier * 0.5F);
      this.sprites = pSprites;
      this.setSpriteFromAge(pSprites);
   }

   public void tick() {
      super.tick();
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
         return new PlasmaExplosionParticle(pLevel, pX, pY, pZ, pXSpeed, this.sprites);
      }
   }
}
