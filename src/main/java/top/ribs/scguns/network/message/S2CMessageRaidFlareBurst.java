package top.ribs.scguns.network.message;

import com.mrcrayfish.framework.api.network.MessageContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import top.ribs.scguns.client.network.ClientPlayHandler;

public class S2CMessageRaidFlareBurst {
   private double x;
   private double y;
   private double z;
   private String patternType;
   private double scale;
   private int repetitions;
   private List<S2CMessageRaidFlareBurst.ParticleData> particles;

   public S2CMessageRaidFlareBurst() {
      super();
   }

   public S2CMessageRaidFlareBurst(
      double x, double y, double z, String patternType, double scale, int repetitions, List<S2CMessageRaidFlareBurst.ParticleData> particles
   ) {
      super();
      this.x = x;
      this.y = y;
      this.z = z;
      this.patternType = patternType;
      this.scale = scale;
      this.repetitions = repetitions;
      this.particles = particles;
   }

   public void encode(S2CMessageRaidFlareBurst message, FriendlyByteBuf buf) {
      buf.writeDouble(message.x);
      buf.writeDouble(message.y);
      buf.writeDouble(message.z);
      buf.writeUtf(message.patternType);
      buf.writeDouble(message.scale);
      buf.writeInt(message.repetitions);
      buf.writeInt(message.particles.size());

      for (S2CMessageRaidFlareBurst.ParticleData particle : message.particles) {
         buf.writeUtf(particle.particleId);
         buf.writeInt(particle.count);
         buf.writeDouble(particle.spread);
         buf.writeDouble(particle.speed);
         buf.writeInt(particle.color);
      }
   }

   public S2CMessageRaidFlareBurst decode(FriendlyByteBuf buf) {
      double x = buf.readDouble();
      double y = buf.readDouble();
      double z = buf.readDouble();
      String patternType = buf.readUtf();
      double scale = buf.readDouble();
      int repetitions = buf.readInt();
      int particleCount = buf.readInt();
      List<S2CMessageRaidFlareBurst.ParticleData> particles = new ArrayList<>();

      for (int i = 0; i < particleCount; i++) {
         String particleId = buf.readUtf();
         int count = buf.readInt();
         double spread = buf.readDouble();
         double speed = buf.readDouble();
         int color = buf.readInt();
         particles.add(new S2CMessageRaidFlareBurst.ParticleData(particleId, count, spread, speed, color));
      }

      return new S2CMessageRaidFlareBurst(x, y, z, patternType, scale, repetitions, particles);
   }

   public void handle(S2CMessageRaidFlareBurst message, MessageContext context) {
      context.execute(() -> ClientPlayHandler.handleRaidFlareBurst(message));
      context.setHandled(true);
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public String getPatternType() {
      return this.patternType;
   }

   public double getScale() {
      return this.scale;
   }

   public int getRepetitions() {
      return this.repetitions;
   }

   public List<S2CMessageRaidFlareBurst.ParticleData> getParticles() {
      return this.particles;
   }

   public static class ParticleData {
      public final String particleId;
      public final int count;
      public final double spread;
      public final double speed;
      public final int color;

      public ParticleData(String particleId, int count, double spread, double speed, int color) {
         super();
         this.particleId = particleId;
         this.count = count;
         this.spread = spread;
         this.speed = speed;
         this.color = color;
      }
   }
}
