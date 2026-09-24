package top.ribs.scguns.init;


import net.minecraft.core.registries.BuiltInRegistries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import top.ribs.scguns.particles.BulletHoleData;
import top.ribs.scguns.particles.TrailData;

public class ModParticleTypes {
   /**
    * 1.21 moved {@code ParticleType#codec} from {@link Codec} to {@link MapCodec}, so the two particle
    * types that carry data describe themselves with the same record shape as the 1.20.1 codecs in
    * {@link BulletHoleData} / {@link TrailData}.
    */
   private static final MapCodec<BulletHoleData> BULLET_HOLE_CODEC = RecordCodecBuilder.mapCodec(
      builder -> builder.group(
               Codec.INT.fieldOf("dir").forGetter(data -> data.getDirection().ordinal()),
               Codec.LONG.fieldOf("pos").forGetter(data -> data.getPos().asLong())
            )
            .apply(builder, BulletHoleData::new)
   );
   private static final MapCodec<TrailData> TRAIL_CODEC = RecordCodecBuilder.mapCodec(
      builder -> builder.group(Codec.BOOL.fieldOf("enchanted").forGetter(TrailData::isEnchanted)).apply(builder, TrailData::new)
   );
   public static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, "scguns");
   public static final DeferredHolder<ParticleType<?>, ParticleType<BulletHoleData>> BULLET_HOLE = REGISTER.register(
      "bullet_hole", () -> new ParticleType<BulletHoleData>(false) {
            public MapCodec<BulletHoleData> codec() {
               return BULLET_HOLE_CODEC;
            }
            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, BulletHoleData> streamCodec() {
               return BulletHoleData.STREAM_CODEC;
            }
         }
   );
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOOD = REGISTER.register("blood", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, ParticleType<TrailData>> TRAIL = REGISTER.register(
      "trail", () -> new ParticleType<TrailData>(false) {
            public MapCodec<TrailData> codec() {
               return TRAIL_CODEC;
            }
            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, TrailData> streamCodec() {
               return TrailData.STREAM_CODEC;
            }
         }
   );
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> COPPER_CASING_PARTICLE = REGISTER.register("copper_casing", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> IRON_CASING_PARTICLE = REGISTER.register("iron_casing", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DIAMOND_STEEL_CASING_PARTICLE = REGISTER.register(
      "diamond_steel_casing", () -> new SimpleParticleType(true)
   );
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHULK_CASING_PARTICLE = REGISTER.register("shulk_casing", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BRASS_CASING_PARTICLE = REGISTER.register("brass_casing", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHELL_PARTICLE = REGISTER.register("shell", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BEARPACK_PARTICLE = REGISTER.register("bearpack_shell", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ROCKET_TRAIL = REGISTER.register("rocket_trail", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SONIC_BLAST = REGISTER.register("sonic_blast", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GREEN_FLAME = REGISTER.register("green_flame", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PLASMA_RING = REGISTER.register("plasma_ring", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SULFUR_SMOKE = REGISTER.register("sulfur_smoke", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SULFUR_DUST = REGISTER.register("sulfur_dust", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PLASMA_EXPLOSION = REGISTER.register("plasma_explosion", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RAMROD_IMPACT = REGISTER.register("ramrod_impact", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BEOWULF_IMPACT = REGISTER.register("beowulf_impact", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ROCKET_EXPLOSION = REGISTER.register("rocket_explosion", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GRENADE_EXPLOSION = REGISTER.register("grenade_explosion", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIRE_GRENADE_EXPLOSION = REGISTER.register(
      "fire_grenade_explosion", () -> new SimpleParticleType(true)
   );
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIREBALL = REGISTER.register("fireball", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SOUL_FIREBALL = REGISTER.register("soul_fireball", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TURRET_MUZZLE_FLASH = REGISTER.register("turret_muzzle_flash", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LASER = REGISTER.register("laser", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SMALL_LASER = REGISTER.register("small_laser", () -> new SimpleParticleType(true));
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ACID_BUBBLE = REGISTER.register("acid_bubble", () -> new SimpleParticleType(true));

   public ModParticleTypes() {
      super();
   }
}
