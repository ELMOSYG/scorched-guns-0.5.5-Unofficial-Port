package top.ribs.scguns.util;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.Config;

/**
 * Wears a mob's gun when it spawns (HANDOFF section 82.31).
 *
 * <p>Gunners, guards and raid bosses all spawned with factory-new guns, because the durability half of the
 * equipment handling only existed on the JSON-driven path ({@code EntityEquipmentConfig}, which reads
 * {@code min_durability}/{@code max_durability} out of {@code data/scguns/entity/equipment/*.json}). Everything
 * equipped by code got a fresh {@link ItemStack}, so the gun a player pried off a gunner was always pristine.
 * The defaults here are that same 0.2 - 0.6 range, so the two paths finally agree.</p>
 *
 * <p>Deliberately not applied on the JSON path: those files state their own range per entry, and rolling a
 * second time there would throw the configured value away.</p>
 */
public class MobGunDurability {
   public MobGunDurability() {
      super();
   }

   /**
    * Damages a mob's gun to a random fraction of its maximum durability. Anything that cannot take damage is
    * left alone, so this is safe to call for every item a mob is handed.
    */
   public static ItemStack roll(ItemStack stack, RandomSource random) {
      if (stack.isEmpty() || !stack.isDamageableItem()) {
         return stack;
      }

      float min = Config.COMMON.gunnerMobs.mobGunMinDurability.get().floatValue();
      float max = Config.COMMON.gunnerMobs.mobGunMaxDurability.get().floatValue();
      float lowest = Math.min(min, max);
      float highest = Math.max(min, max);
      float remaining = lowest + random.nextFloat() * (highest - lowest);

      int maxDamage = stack.getMaxDamage();
      int damage = Mth.clamp((int)((float)maxDamage * (1.0F - remaining)), 0, maxDamage - 1);
      stack.setDamageValue(damage);
      return stack;
   }
}
