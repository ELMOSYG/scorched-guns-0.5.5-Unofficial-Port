package top.ribs.scguns.util;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import top.ribs.scguns.entity.projectile.BeowulfProjectileEntity;
import top.ribs.scguns.entity.projectile.GibbsRoundProjectileEntity;
import top.ribs.scguns.entity.projectile.turret.TurretProjectileEntity;

/**
 * The extra looting that a few projectiles grant their kills.
 *
 * <p>Upstream 0.5.5 registered a Forge {@code LootingLevelEvent} handler in each projectile class and simply added
 * a flat looting level ({@code +3} for the Beowulf round, {@code +4} for the Gibbs round and for turret fire).
 * NeoForge 21.1 has no such event, so the port dropped the feature.</p>
 *
 * <p>The upstream 0.5.5 behaviour is therefore "raise the looting level by 3 or 4" - the loot tables' own
 * {@code minecraft:enchanted_count_increase} functions then decide what that means, which is why a gun or a
 * shield never multiplied: no table attaches that function to them.</p>
 *
 * <p>Two ways to get it back live here:</p>
 * <ul>
 *    <li>{@link #applyProjectileLootingBonus} is a pure function of the vanilla looting-level computation. Addons
 *        built for the other 1.21.1 port mix into this exact class and call this exact method, so it has to exist
 *        with this name and signature or their mixin fails to apply
 *        ({@code @Inject ... could not find any target method}).</li>
 *    <li>{@link #onLivingDrops} is this port's own fallback for when no such addon is installed: it adds the same
 *        amount using vanilla's "each level adds zero or one" rule, i.e. {@code nextInt(level + 1)}. When the
 *        addon that patches the level calculation itself is present, this steps aside so the bonus is not
 *        counted twice.</li>
 * </ul>
 */
@EventBusSubscriber(modid = "scguns", bus = EventBusSubscriber.Bus.GAME)
public final class ProjectileLootingHelper {
   /** Beowulf rounds behave like three levels of Looting. */
   public static final int BEOWULF_LOOTING_LEVEL = 3;

   /** Gibbs rounds (and turret fire, which reuses this constant) behave like four levels. */
   public static final int GIBBS_LOOTING_LEVEL = 4;

   /**
    * Mod id of the addon that patches the vanilla looting-level calculation and calls
    * {@link #applyProjectileLootingBonus} itself. While it is present, {@link #onLivingDrops} must not also apply
    * the bonus.
    */
   private static final String LEVEL_PATCHING_ADDON = "tepox_scguns_enchantments";

   private ProjectileLootingHelper() {
   }

   /** The looting bonus the given killing projectile grants, or {@code 0} for everything else. */
   public static int lootingBonusFor(Entity killer) {
      if (killer instanceof BeowulfProjectileEntity) {
         return BEOWULF_LOOTING_LEVEL;
      }

      if (killer instanceof GibbsRoundProjectileEntity) {
         return GIBBS_LOOTING_LEVEL;
      }

      return killer instanceof TurretProjectileEntity ? GIBBS_LOOTING_LEVEL : 0;
   }

   /**
    * Raises a looting level when the killing blow came from one of this mod's projectiles.
    *
    * <p>This is the method addons mix into, so its name and signature are part of the public surface: keep them.
    * It returns {@code level} unchanged for every other enchantment or when the damage did not come from one of
    * our projectiles, which makes it safe to call from a mixin that patches the vanilla level calculation.</p>
    */
   public static int applyProjectileLootingBonus(Holder<Enchantment> enchantment, LootContext context, int level) {
      if (enchantment == null || !enchantment.is(Enchantments.LOOTING)) {
         return level;
      }

      DamageSource damageSource = context.getParamOrNull(LootContextParams.DAMAGE_SOURCE);
      if (damageSource == null) {
         return level;
      }

      int bonus = lootingBonusFor(damageSource.getDirectEntity());
      return bonus > 0 ? level + bonus : level;
   }

   @SubscribeEvent
   public static void onLivingDrops(LivingDropsEvent event) {
      // The addon patches the level calculation itself; counting here as well would double the bonus.
      if (ModList.get().isLoaded(LEVEL_PATCHING_ADDON)) {
         return;
      }

      int bonus = lootingBonusFor(event.getSource().getDirectEntity());
      if (bonus <= 0) {
         return;
      }

      for (ItemEntity drop : event.getDrops()) {
         ItemStack stack = drop.getItem();
         int max = stack.getMaxStackSize();

         // Growing a stack past its limit is how a single dropped gun turned into three: guns and shields are
         // unstackable, so a count of 3 is invalid and the game splits it into three separate items on pickup.
         // Looting only ever adds to stacks that can hold more than one, and never past their limit.
         if (max <= 1 || stack.getCount() >= max) {
            continue;
         }

         int extra = event.getEntity().getRandom().nextInt(bonus + 1);
         extra = Math.min(extra, max - stack.getCount());

         if (extra > 0) {
            stack.grow(extra);
         }
      }
   }
}
