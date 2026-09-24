package top.ribs.scguns.particles;


import net.minecraft.core.registries.BuiltInRegistries;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import top.ribs.scguns.init.ModParticleTypes;

public class BulletHoleData implements ParticleOptions {
   public static final Codec<BulletHoleData> CODEC = RecordCodecBuilder.create(
      builder -> builder.group(
               Codec.INT.fieldOf("dir").forGetter(data -> data.direction.ordinal()),
               Codec.LONG.fieldOf("pos").forGetter(p_239806_0_ -> p_239806_0_.pos.asLong())
            )
            .apply(builder, BulletHoleData::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, BulletHoleData> STREAM_CODEC =
      StreamCodec.composite(
         ByteBufCodecs.VAR_INT, data -> data.direction.ordinal(),
         ByteBufCodecs.VAR_LONG, data -> data.pos.asLong(),
         BulletHoleData::new);
   private final Direction direction;
   private final BlockPos pos;

   public BulletHoleData(int dir, long pos) {
      super();
      this.direction = Direction.values()[dir];
      this.pos = BlockPos.of(pos);
   }

   public BulletHoleData(Direction dir, BlockPos pos) {
      super();
      this.direction = dir;
      this.pos = pos;
   }

   public Direction getDirection() {
      return this.direction;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public ParticleType<?> getType() {
      return (ParticleType<?>)ModParticleTypes.BULLET_HOLE.get();
   }

   public void writeToNetwork(FriendlyByteBuf buffer) {
      buffer.writeEnum(this.direction);
      buffer.writeBlockPos(this.pos);
   }

   public String writeToString() {
      return BuiltInRegistries.PARTICLE_TYPE.getKey(this.getType()) + " " + this.direction.getName();
   }

   public static Codec<BulletHoleData> codec(ParticleType<BulletHoleData> type) {
      return CODEC;
   }
}
