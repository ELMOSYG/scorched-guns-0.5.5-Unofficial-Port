package top.ribs.scguns.attributes;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class SCAttributes {
   public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(BuiltInRegistries.ATTRIBUTE, "scguns");
   public static final DeferredHolder<Attribute, Attribute> PROJECTILE_SPEED = ATTRIBUTES.register(
      "projectile_speed", () -> new RangedAttribute("attribute.scguns.projectile_speed", 1.0, 0.01, 100.0).setSyncable(true)
   );
   public static final DeferredHolder<Attribute, Attribute> ADDITIONAL_BULLET_DAMAGE = ATTRIBUTES.register(
      "additional_bullet_damage", () -> new RangedAttribute("attribute.scguns.additional_bullet_damage", 0.0, -1000.0, 10000.0).setSyncable(true)
   );
   public static final DeferredHolder<Attribute, Attribute> RELOAD_SPEED = ATTRIBUTES.register(
      "reload_speed", () -> new RangedAttribute("attribute.scguns.reload_speed", 1.0, 0.01, 1000.0).setSyncable(true)
   );
   public static final DeferredHolder<Attribute, Attribute> BULLET_DAMAGE_MULTIPLIER = ATTRIBUTES.register(
      "bullet_damage_multiplier", () -> new RangedAttribute("attribute.scguns.bullet_damage_multiplier", 1.0, 0.0, 1000.0).setSyncable(true)
   );
   public static final DeferredHolder<Attribute, Attribute> SPREAD_MULTIPLIER = ATTRIBUTES.register(
      "spread_multiplier", () -> new RangedAttribute("attribute.scguns.spread_multiplier", 1.0, 0.0, 1000.0).setSyncable(true)
   );

   public SCAttributes() {
      super();
   }
}
