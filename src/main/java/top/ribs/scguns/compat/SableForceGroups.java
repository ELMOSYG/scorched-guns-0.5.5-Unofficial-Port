package top.ribs.scguns.compat;

import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.ribs.scguns.ScorchedGuns;

/**
 * The two force groups this mod's shots and explosions use on a Sable physics structure.
 *
 * <p>Sable records point forces per group and applies them during its own physics step, so registering a group is how
 * a mod makes its forces visible to the solver (and to Sable's force overlay). The mod previously borrowed Sable's
 * player-punch curve instead, which is why an explosion did nothing to a structure that a punch would have moved.</p>
 */
public final class SableForceGroups {
   public static final DeferredRegister<ForceGroup> FORCE_GROUPS =
      DeferredRegister.create(ForceGroups.REGISTRY_KEY, ScorchedGuns.MODID);

   /** A bullet or shell striking a structure. */
   public static final DeferredHolder<ForceGroup, ForceGroup> IMPACT = FORCE_GROUPS.register("gun_impact",
      () -> new ForceGroup(
         Component.translatable("force_group.scguns.gun_impact"),
         Component.translatable("force_group.scguns.gun_impact.desc"),
         0xC8A050, true));

   /** The blast wave of one of the mod's explosions. */
   public static final DeferredHolder<ForceGroup, ForceGroup> EXPLOSION = FORCE_GROUPS.register("gun_explosion",
      () -> new ForceGroup(
         Component.translatable("force_group.scguns.gun_explosion"),
         Component.translatable("force_group.scguns.gun_explosion.desc"),
         0xE07040, true));

   private SableForceGroups() {
   }

   public static void register(IEventBus bus) {
      FORCE_GROUPS.register(bus);
   }
}
