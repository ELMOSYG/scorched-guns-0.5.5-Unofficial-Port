package top.ribs.scguns.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
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
 * NeoForge 21.1 has no such event and {@code LivingDropsEvent} does not expose a modifiable looting level, so the
 * port dropped the feature. This restores it through {@code LivingDropsEvent} instead: when the killing blow came
 * from one of those projectiles, the drops get the same bonus the original a looting level of that size would have
 * produced - vanilla's own rule is "each level adds zero or one", i.e. {@code nextInt(level + 1)}.</p>
 *
 * <p>The class lives here under this name because addons built for the other 1.21.1 port mix into
 * {@code top.ribs.scguns.util.ProjectileLootingHelper}. Keeping the logic in one place under that name lets those
 * addons' mixins apply instead of silently failing with {@code @Mixin target ... was not found}.</p>
 */
@EventBusSubscriber(modid = "scguns", bus = EventBusSubscriber.Bus.GAME)
public final class ProjectileLootingHelper {
   /** Beowulf rounds behave like three levels of Looting. */
   public static final int BEOWULF_LOOTING_LEVEL = 3;

   /** Gibbs rounds (and turret fire, which reuses this constant) behave like four levels. */
   public static final int GIBBS_LOOTING_LEVEL = 4;

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

   @SubscribeEvent
   public static void onLivingDrops(LivingDropsEvent event) {
      int bonus = lootingBonusFor(event.getSource().getDirectEntity());
      if (bonus <= 0) {
         return;
      }

      for (ItemEntity drop : event.getDrops()) {
         int extra = event.getEntity().getRandom().nextInt(bonus + 1);
         if (extra > 0) {
            drop.getItem().grow(extra);
         }
      }
   }
}
