package top.ribs.scguns.init;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import top.ribs.scguns.effect.IncurableEffect;
import top.ribs.scguns.effect.LaceratedEffect;
import top.ribs.scguns.effect.SulfurPoisoningEffect;

public class ModEffects {
   public static final DeferredRegister<MobEffect> REGISTER = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, "scguns");
   public static final DeferredHolder<MobEffect, IncurableEffect> BLINDED = REGISTER.register("blinded", () -> new IncurableEffect(MobEffectCategory.HARMFUL, 0));
   public static final DeferredHolder<MobEffect, IncurableEffect> DEAFENED = REGISTER.register("deafened", () -> new IncurableEffect(MobEffectCategory.HARMFUL, 0));
   public static final DeferredHolder<MobEffect, SulfurPoisoningEffect> SULFUR_POISONING = REGISTER.register(
      "sulfur_poisoning", () -> new SulfurPoisoningEffect(MobEffectCategory.HARMFUL, 16769333)
   );
   public static final DeferredHolder<MobEffect, LaceratedEffect> LACERATED = REGISTER.register("lacerated", () -> new LaceratedEffect(MobEffectCategory.HARMFUL, 0));

   public ModEffects() {
      super();
   }
}
