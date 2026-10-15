package top.ribs.scguns.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Damage after armour, computed the way vanilla does it - but with the gun's penetration taken off the armour value
 * first, so penetration actually removes part of what the target's armour would have absorbed.
 *
 * <p>The mod used to apply its own reduction of {@code armour * 0.004} (6% for a full set of iron) and let
 * penetration scale that number down. Since vanilla's own reduction still ran afterwards, penetration could only
 * ever move those 6%: three points and eighteen points both changed a hit by well under one damage. The projectile
 * damage type is now in {@code minecraft:bypasses_armor}, so vanilla no longer applies its reduction, and this is
 * the single place that decides how much armour absorbs. With no penetration the formula below is vanilla's, so
 * nothing changes for a gun without any.</p>
 */
public final class ArmorPenetration {
   private ArmorPenetration() {
   }

   public static float afterArmor(LivingEntity target, float armorPen, float damage) {
      float armor = (float)target.getArmorValue();
      float toughness = (float)target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
      float effective = Math.max(0.0F, armor - armorPen);
      float scale = 2.0F + toughness / 4.0F;
      float absorbed = Mth.clamp(effective - damage / scale, effective * 0.2F, 20.0F);
      return damage * (1.0F - absorbed / 25.0F);
   }
}
