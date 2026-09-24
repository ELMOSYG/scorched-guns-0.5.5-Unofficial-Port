package top.ribs.scguns.particles;


import net.minecraft.core.registries.BuiltInRegistries;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import top.ribs.scguns.init.ModParticleTypes;

public class TrailData implements ParticleOptions {
   public static final Codec<TrailData> CODEC = RecordCodecBuilder.create(
      builder -> builder.group(Codec.BOOL.fieldOf("enchanted").forGetter(data -> data.enchanted)).apply(builder, TrailData::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, TrailData> STREAM_CODEC =
      StreamCodec.composite(ByteBufCodecs.BOOL, TrailData::isEnchanted, TrailData::new);
   private final boolean enchanted;

   public TrailData(boolean enchanted) {
      super();
      this.enchanted = enchanted;
   }

   public boolean isEnchanted() {
      return this.enchanted;
   }

   public ParticleType<?> getType() {
      return (ParticleType<?>)ModParticleTypes.TRAIL.get();
   }

   public void writeToNetwork(FriendlyByteBuf buffer) {
      buffer.writeBoolean(this.enchanted);
   }

   public String writeToString() {
      return BuiltInRegistries.PARTICLE_TYPE.getKey(this.getType()) + " " + this.enchanted;
   }
}
