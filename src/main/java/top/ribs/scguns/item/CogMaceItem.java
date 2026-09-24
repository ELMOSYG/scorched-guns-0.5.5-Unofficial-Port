package top.ribs.scguns.item;


import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Item.Properties;

public class CogMaceItem extends SwordItem {
   public CogMaceItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
      // 1.20.1 passed (damageModifier, attackSpeedModifier) to SwordItem; 1.21 moved those
      // attributes into the item properties, so build the identical pair here.
      super(tier, properties.attributes(SwordItem.createAttributes(tier, attackDamage, attackSpeed)));
   }

   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      attacker.resetFallDistance();
      stack.hurtAndBreak(1, attacker, net.minecraft.world.entity.LivingEntity.getSlotForHand(attacker.getUsedItemHand()));
      return super.hurtEnemy(stack, target, attacker);
   }
}
