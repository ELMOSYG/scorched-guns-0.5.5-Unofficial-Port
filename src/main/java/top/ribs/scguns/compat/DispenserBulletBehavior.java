package top.ribs.scguns.compat;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import top.ribs.scguns.common.Turret;
import top.ribs.scguns.common.TurretManager;
import top.ribs.scguns.entity.projectile.turret.TurretProjectileEntity;

/**
 * Lets a dispenser fire this mod's ammo the way a turret does.
 *
 * <p>The turret already knows how to turn a loaded ammo item into damage, armour penetration and special behaviour, so
 * the dispenser reuses that data instead of inventing its own: the ammo type is looked up from the turret definitions,
 * and the shot is a plain turret projectile fired along the dispenser's facing.</p>
 */
public final class DispenserBulletBehavior implements DispenseItemBehavior {

   /** Item -> the turret ammo definition that accepts it. Built once, then reused. */
   private static final Map<Item, Turret.Ammunition.AmmoType> AMMO_BY_ITEM = new HashMap<>();
   private static boolean loaded;

   @Override
   public ItemStack dispense(BlockSource source, ItemStack stack) {
      Level level = source.level();
      Turret.Ammunition.AmmoType ammo = findAmmo(stack.getItem());
      if (ammo == null) {
         // Data packs may load after the first setup pass, so retry once with a fresh read before giving up.
         loaded = false;
         ammo = findAmmo(stack.getItem());
      }

      if (ammo == null) {
         top.ribs.scguns.ScorchedGuns.LOGGER.debug(
            "scguns: no turret ammo definition for {}, dispensing it as an item", stack.getItem());
         return stack;
      }

      Direction facing = source.state().getValue(DispenserBlock.FACING);
      var center = source.center();

      TurretProjectileEntity projectile = new TurretProjectileEntity(level);
      projectile.setPos(
         center.x + (double) facing.getStepX() * 0.6,
         center.y + (double) facing.getStepY() * 0.6,
         center.z + (double) facing.getStepZ() * 0.6);

      double speed = 3.0;
      projectile.shoot(facing.getStepX(), facing.getStepY(), facing.getStepZ(), (float) speed, 1.0F);
      projectile.setBaseDamage(ammo.getDamage());
      projectile.setArmorPenetration(ammo.getArmorPenetration());

      ResourceLocation bulletType = ammo.getBulletType();
      if (bulletType != null) {
         String type = bulletType.toString();
         if (type.equals("scguns:bear_pack_shell")) {
            projectile.setMobPenetration(1);
         } else if (type.equals("scguns:gibbs_round")) {
            projectile.setGibbsRound(true);
         } else if (type.equals("scguns:shatter_round")) {
            projectile.setShatterRound(true);
         }
      }

      level.addFreshEntity(projectile);
      stack.shrink(1);
      return stack;
   }

   private static Turret.Ammunition.AmmoType findAmmo(Item item) {
      if (!loaded) {
         loaded = true;
         try {
            for (ResourceLocation id : TurretManager.serializeTurrets().keySet()) {
               Turret turret = TurretManager.getTurret(id);
               if (turret == null || turret.getAmmunition() == null) {
                  continue;
               }

               for (Turret.Ammunition.AmmoType ammo : turret.getAmmunition().getAcceptedAmmo()) {
                  Item accepted = ammo.getItem();
                  if (accepted != null) {
                     AMMO_BY_ITEM.putIfAbsent(accepted, ammo);
                  }
               }
            }
            top.ribs.scguns.ScorchedGuns.LOGGER.info(
               "scguns: dispenser bullets can fire {} ammo items known to turrets", AMMO_BY_ITEM.size());
         } catch (Throwable failure) {
            top.ribs.scguns.ScorchedGuns.LOGGER.warn("Could not read turret ammo for dispenser shots", failure);
         }
      }

      if (AMMO_BY_ITEM.size() > 0 && !AMMO_BY_ITEM.containsKey(item)) {
         // Nothing to do here; kept for the debug branch above.
      }

      return AMMO_BY_ITEM.get(item);
   }
}
