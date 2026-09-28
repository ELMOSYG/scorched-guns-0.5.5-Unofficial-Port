package top.ribs.scguns.compat.guardvillagers;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.ribs.scguns.Config;
import top.ribs.scguns.config.GunnerMobConfig;
import top.ribs.scguns.config.GunnerMobSpawner;
import top.ribs.scguns.item.GunItem;

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
    * Where a guard's one spawn-chance roll is remembered: in the entity's own saved data, not in a static
    * map (HANDOFF section 82.20).
    *
    * <p>A {@code WeakHashMap} looked sufficient and was not. Guard Villagers' own equipment arrives a few
    * ticks after a guard joins, and the compat used {@code tickCount} as its "still spawning" clock - but
    * {@code Entity.tickCount} is <b>not saved</b> (vanilla only reads it for crystal sounds), so it restarts
    * at zero every time a chunk brings the guard back. The spawn window therefore reopened on every reload:
    * a guard whose gun a player had taken was handed a new one, and a guard carrying a sword had it
    * replaced, which is exactly what the player reported.</p>
    *
    * <p>{@code getPersistentData()} is written to and read from the entity's NBT ({@code NeoForgeData}), so
    * these survive a save, a reload and a server restart.</p>
    */
   private static final String GUN_ROLLED = "ScgunsGunRolled";

   /** Whether that roll was won, i.e. whether this guard is a gun guard at all. */
   private static final String GUN_ARMED = "ScgunsGunArmed";

   /** The game time the roll was made, which is the clock the re-arm window is measured against. */
   private static final String GUN_ROLLED_AT = "ScgunsGunRolledAt";

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
    * How long after a guard joins the level Guard Villagers may still overwrite its equipment, in ticks.
    *
    * <p>Guard Villagers equips a fresh guard with its own sword or crossbow a few ticks after the entity
    * joins (the section 82 probe saw a gun at join and an iron sword twenty ticks later), so the compat has
    * to be willing to put the gun back for that long. It is deliberately a <b>window</b> and not "forever":
    * a gun a player takes away must stay taken (HANDOFF section 82.16).</p>
    */
   public static final int GUARD_REARM_WINDOW_TICKS = 100;

   /**
    * Puts a guard's gun back after <b>Guard Villagers itself</b> replaced it (HANDOFF sections 82.16, 82.20).
    *
    * <p>The caller has already checked that the main hand now holds something - an empty hand means the gun
    * was taken, and arming the guard again there makes the compat an infinite gun dispenser. Two more
    * conditions: the guard must have <b>won</b> its one roll, and the re-arm must happen inside
    * {@link #GUARD_REARM_WINDOW_TICKS} of that roll, measured against the <b>saved</b> game time of the roll.
    * The saved time is the whole point of section 82.20: {@code tickCount} restarts at zero on every chunk
    * reload, so a window measured against it reopened every time the guard came back.</p>
    */
   public static boolean rearmReplacedGuardGun(PathfinderMob mob) {
      if (!isGuard(mob) || !mob.isAlive()) {
         return false;
      }

      CompoundTag saved = mob.getPersistentData();
      if (!saved.getBoolean(GUN_ROLLED) || !saved.getBoolean(GUN_ARMED)) {
         return false;
      }

      if (mob.level().getGameTime() - saved.getLong(GUN_ROLLED_AT) > GUARD_REARM_WINDOW_TICKS) {
         return false;
      }

      return GunnerMobSpawner.equipGuardGun(mob, accuracy());
   }

   /**
    * Gives a guard its gun, once, and remembers the roll in the guard's own saved data. Returns whether the
    * guard is holding a gun afterwards.
    *
    * <p>Guard Villagers equips a freshly spawned guard with its own sword or crossbow <b>after</b> the entity
    * joins the level, so the gun equipped on join can be overwritten - the probe for section 82 showed a
    * guard holding {@code scguns:winnie} at join and an iron sword twenty ticks later.
    * {@link #rearmReplacedGuardGun} is what puts it back, and the roll is made exactly once per guard, so
    * the retries cannot turn a 25% chance into a certainty.</p>
    *
    * <p>This is deliberately a no-op for a guard that has already rolled: its gun is a spawn-time property.
    * A player who takes the gun keeps it, and a guard that never won one stays armed with what Guard
    * Villagers gave it, across chunk unloads and server restarts included.</p>
    */
   public static boolean equipGuardGun(PathfinderMob mob) {
      if (!isGuard(mob)) {
         return false;
      }

      CompoundTag saved = mob.getPersistentData();
      if (saved.getBoolean(GUN_ROLLED)) {
         return mob.getMainHandItem().getItem() instanceof GunItem;
      }

      GunnerMobConfig.MobGunnerData gunnerData = GunnerMobConfig.getGunnerData(mob.getType());
      boolean armed = gunnerData != null
         && mob.getRandom().nextFloat() < gunnerData.spawnChance()
         && GunnerMobSpawner.equipGuardGun(mob, accuracy());
      saved.putBoolean(GUN_ROLLED, true);
      saved.putBoolean(GUN_ARMED, armed);
      saved.putLong(GUN_ROLLED_AT, mob.level().getGameTime());
      return armed;
   }

   /** The configured guard accuracy: how tightly a guard's shots group. */
   public static float accuracy() {
      return ((Double)Config.COMMON.compat.guardGunAccuracy.get()).floatValue();
   }

   /**
    * Whether this entity's main hand holds one of the mod's guns (HANDOFF section 82.14).
    *
    * <p>Takes {@code Object} and reads the hand reflectively on purpose. The callers are mixins that live
    * <b>inside</b> Guard Villagers' own classes, and an entity type in an injector signature is resolved
    * while Mixin is still preparing configs - which is what stops the game from starting
    * ({@code MixinTargetAlreadyLoadedException: target LivingEntity was loaded too early}, section 82.10).
    * Method bodies are resolved lazily, so this is the safe side of the same problem.</p>
    */
   public static boolean isHoldingGun(Object entity) {
      if (entity == null) {
         return false;
      }

      try {
         Object held = entity.getClass().getMethod("getMainHandItem").invoke(entity);
         return held instanceof ItemStack stack && stack.getItem() instanceof GunItem;
      } catch (Throwable broken) {
         return false;
      }
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
