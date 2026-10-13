package top.ribs.scguns.item.ammo;

import net.minecraft.world.item.Item.Properties;

/**
 * Compatibility alias for addons built against the other 1.21.1 Scorched Guns port.
 *
 * <p>That port keeps its ammo item at {@code top.ribs.scguns.item.ammo.AmmoItem}, while this one (like 0.5.5)
 * keeps it at {@code top.ribs.scguns.item.AmmoItem}. scgunsww1 loads the class by name while its mod object is
 * constructed, so with only the original path present it dies with NoClassDefFoundError before the game starts.
 * This subclass makes the other path resolve; ammo it creates is still an {@code item.AmmoItem}, so the mod's own
 * checks keep working.</p>
 */
public class AmmoItem extends top.ribs.scguns.item.AmmoItem {
   public AmmoItem(Properties properties) {
      super(properties);
   }
}
