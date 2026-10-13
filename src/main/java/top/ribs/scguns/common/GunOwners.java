package top.ribs.scguns.common;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Which {@link Gun} a {@link Gun.General} section belongs to, for the compatibility accessor that addons written
 * against other Scorched Guns ports call as {@code gun.getGeneral().getProjectileAmount()}.
 *
 * <p>It lives here rather than as a field on {@code Gun.General} because gun definitions are validated by
 * reflecting over the declared fields of {@code General} and rejecting any that is null - an extra back-reference
 * field there makes every gun fail to load with "Missing required properties for scguns:guns/...", which leaves
 * every gun with an empty definition (projectile air, the default magazine) and unable to fire.</p>
 *
 * <p>Keys are weak, so a General is never kept alive by this map.</p>
 */
public final class GunOwners {
   private static final Map<Gun.General, Gun> OWNERS = new WeakHashMap<>();

   private GunOwners() {
   }

   public static void put(Gun.General general, Gun gun) {
      OWNERS.put(general, gun);
   }

   public static Gun get(Gun.General general) {
      return OWNERS.get(general);
   }
}
