package top.ribs.scguns.item;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class AnthralitePaxelItem extends TieredItem {
   private final Tier tier;

   public AnthralitePaxelItem(Tier tier, Properties properties) {
      super(tier, properties);
      this.tier = tier;
   }

   @Override
   public boolean isCorrectToolForDrops(ItemStack stack, BlockState blockstate) {
      // 1.21 replaced Forge's Tier#getLevel with the tier's "incorrect blocks" tag; for the
      // anthralite tier (old level 2 == iron) that tag is INCORRECT_FOR_IRON_TOOL, so this
      // keeps the old level-based checks exactly.
      if (blockstate.is(this.tier.getIncorrectBlocksForDrops())) {
         return false;
      } else {
         return blockstate.is(BlockTags.MINEABLE_WITH_AXE)
            || blockstate.is(BlockTags.MINEABLE_WITH_HOE)
            || blockstate.is(BlockTags.MINEABLE_WITH_PICKAXE)
            || blockstate.is(BlockTags.MINEABLE_WITH_SHOVEL);
      }
   }


   @Override
   public float getDestroySpeed(ItemStack itemstack, BlockState blockstate) {
      return this.tier.getSpeed();
   }

   @Override
   public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
      return super.getDefaultAttributeModifiers(stack)
         .withModifierAdded(
            Attributes.ATTACK_DAMAGE,
            new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, (double)this.tier.getAttackDamageBonus() + 4.5, Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND
         )
         .withModifierAdded(
            Attributes.ATTACK_SPEED,
            new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -3.05, Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND
         );
   }

   @Override
   public boolean mineBlock(ItemStack itemstack, Level world, BlockState blockstate, BlockPos pos, LivingEntity entity) {
      itemstack.hurtAndBreak(1, entity, EquipmentSlot.MAINHAND);
      return true;
   }

   @Override
   public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
      itemstack.hurtAndBreak(2, entity, EquipmentSlot.MAINHAND);
      return true;
   }
}
