package top.ribs.scguns.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import top.ribs.scguns.interfaces.IGunModifier;
import top.ribs.scguns.item.attachment.impl.UnderBarrel;

public class BayonetItem extends UnderBarrelItem {
   private final float attackDamage;
   private final float attackSpeed;
   private final IGunModifier modifier;

   public BayonetItem(UnderBarrel underBarrel, Properties properties, float attackDamage, float attackSpeed) {
      super(underBarrel, properties);
      this.attackDamage = attackDamage;
      this.attackSpeed = attackSpeed;
      this.modifier = underBarrel.getModifier();
   }

   public BayonetItem(UnderBarrel underBarrel, Properties properties, boolean colored, float attackDamage, float attackSpeed) {
      super(underBarrel, properties, colored);
      this.attackDamage = attackDamage;
      this.attackSpeed = attackSpeed;
      this.modifier = underBarrel.getModifier();
   }

   public float getAdditionalDamage() {
      return this.modifier.additionalDamage();
   }



   public boolean isEnchantable(ItemStack stack) {
      return true;
   }

   public int getEnchantmentValue() {
      return 10;
   }

   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
      return true;
   }

   public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity entityLiving) {
      if (state.getDestroySpeed(world, pos) != 0.0F) {
         stack.hurtAndBreak(2, entityLiving, EquipmentSlot.MAINHAND);
      }

      return true;
   }

   public boolean canAttackBlock(BlockState state, Level world, BlockPos pos, Player player) {
      return !player.isCreative();
   }

   @Override
   public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
      return super.getDefaultAttributeModifiers(stack)
         .withModifierAdded(
            Attributes.ATTACK_DAMAGE,
            new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, (double)this.attackDamage, Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND
         )
         .withModifierAdded(
            Attributes.ATTACK_SPEED,
            new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, (double)this.attackSpeed, Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND
         );
   }

   public float getDamage() {
      return this.attackDamage;
   }

   public float getDestroySpeed(ItemStack stack, BlockState state) {
      return state.is(Blocks.COBWEB) ? 15.0F : 1.0F;
   }

   public boolean isCorrectToolForDrops(BlockState block) {
      return block.is(Blocks.COBWEB);
   }
}
