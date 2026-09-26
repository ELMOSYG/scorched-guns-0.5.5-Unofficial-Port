package top.ribs.scguns.compat.guardvillagers;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.ribs.scguns.Config;
import top.ribs.scguns.config.GunnerMobConfig;
import top.ribs.scguns.config.GunnerMobSpawner;
import top.ribs.scguns.item.GunItem;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Guard Villagers compatibility, host side (HANDOFF section 82).
 *
 * <p>Guards get scorched guns the same way the mod's own gunners do: an entry for
 * {@code guardvillagers:guard} in {@code data/scguns/entity/gunner_mobs.json}, equipped when the guard
 * joins the level. That keeps the weapon list, the AI difficulty and the drop chance in the file players
 * already edit, instead of a loot-table override that quietly replaces Guard Villagers' own equipment
 * table (which is what the standalone 1.20.1 compat had to do).</p>
 *
 * <h2>Why nothing here names a Guard class</h2>
 *
 * <p>This class must be safe to <b>load and call</b> on a server without Guard Villagers, so it only ever
 * compares the entity type's registry id ({@link #isGuard}); the one place that needs the real class,
 * {@link GuardFriendlyRules}, is reached from {@link #isFriendlyShot} behind that id check. Java resolves
 * a class on the first execution of the instruction that names it, so that class is never loaded unless a
 * guard actually exists - which cannot happen without the mod. Nothing in the host reaches for a Guard
 * Villagers type directly, and the build audit enforces that.</p>
 */
public final class GuardVillagersCompat {
   /** {@code guardvillagers:guard} - the guard entity's registry id. */
   public static final ResourceLocation GUARD_ID = ResourceLocation.fromNamespaceAndPath("guardvillagers", "guard");

   /**
    * Which guards have already had their spawn-chance roll, and whether they won it. Weak keys: nothing
    * here should keep an entity alive. Runtime state only - after a reload a guard simply rolls again.
    */
   private static final Map<Entity, Boolean> GUARD_GUN_ROLLS = new WeakHashMap<>();

   private GuardVillagersCompat() {
   }

   /** Whether Guard Villagers is installed - for the diagnostic messages only. */
   public static boolean isLoaded() {
      return ModList.get().isLoaded("guardvillagers");
   }

   /** Whether this entity is a Guard Villagers guard. Type-id only: no Guard class is referenced. */
   public static boolean isGuard(Entity entity) {
      return entity != null && GUARD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
   }

   /**
    * Gives a guard its gun, once. Returns whether the guard is holding a gun afterwards.
    *
    * <p>Called three times on purpose. Guard Villagers equips a freshly spawned guard with its own sword
    * or crossbow <b>after</b> the entity joins the level, so the gun equipped on join is overwritten -
    * the probe for this section showed a guard holding {@code scguns:winnie} at join and an iron sword
    * twenty ticks later. The join hook, the first ticks of the mob's life and the equipment-change hook
    * therefore all come here; the spawn-chance roll is made once per guard and remembered, so the retries
    * cannot turn a 30% chance into a certainty.</p>
    */
   public static boolean equipGuardGun(PathfinderMob mob) {
      if (!isGuard(mob)) {
         return false;
      }

      if (mob.getMainHandItem().getItem() instanceof GunItem) {
         return true;
      }

      Boolean rolled = GUARD_GUN_ROLLS.get(mob);
      if (rolled == null) {
         GunnerMobConfig.MobGunnerData data = GunnerMobConfig.getGunnerData(mob.getType());
         boolean armed = data != null
            && mob.getRandom().nextFloat() < data.spawnChance()
            && GunnerMobSpawner.equipGuardGun(mob, accuracy());
         GUARD_GUN_ROLLS.put(mob, armed);
         return armed;
      }

      if (!rolled) {
         return false;
      }

      // The guard already won its roll and has been disarmed since (Guard Villagers refilled the slot):
      // re-arm without rolling again.
      return GunnerMobSpawner.equipGuardGun(mob, accuracy());
   }

   /** The configured guard accuracy: how tightly a guard's shots group. */
   public static float accuracy() {
      return ((Double)Config.COMMON.compat.guardGunAccuracy.get()).floatValue();
   }

   /**
    * Whether a shot from {@code shooter} at {@code target} would hit an ally. Always false unless the
    * shooter is a guard, so every projectile and damage path can call it unconditionally.
    */
   public static boolean isFriendlyShot(Entity shooter, Entity target) {
      if (!isGuard(shooter) || target == null || target == shooter) {
         return false;
      }

      return GuardFriendlyRules.isAlly(shooter, target);
   }
}
