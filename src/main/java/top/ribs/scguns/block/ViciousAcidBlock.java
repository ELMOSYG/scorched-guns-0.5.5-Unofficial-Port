package top.ribs.scguns.block;




import net.minecraft.world.entity.EquipmentSlot;
import top.ribs.scguns.util.ScEnchants;
import net.minecraft.core.Holder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.FlowingFluid;
import top.ribs.scguns.init.ModParticleTypes;

public class ViciousAcidBlock extends LiquidBlock {
   private static final int DAMAGE_INTERVAL = 10;
   private static final float DAMAGE_AMOUNT = 2.0F;
   private static final int ARMOR_DAMAGE = 3;
   private static final int HELD_ITEM_DAMAGE = 5;
   private static final int DROPPED_ITEM_DAMAGE = 5;
   private static final float ENCHANTMENT_REMOVAL_CHANCE = 0.15F;

   public ViciousAcidBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
      // LiquidBlock resolves its state cache from the fluid instance in 1.21.1, so the supplier has to
      // be resolved here. ModBlocks runs in the BLOCK registry event, which NeoForge fires after FLUID.
      super(fluid.get(), properties);
   }

   public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
      if (!level.isClientSide) {
         if (entity instanceof LivingEntity livingEntity) {
            if (entity.tickCount % 10 == 0) {
               entity.hurt(level.damageSources().magic(), 2.0F);
               this.damageAndTryCurseClear(livingEntity.getArmorSlots(), 3, level, entity.blockPosition(), entity);
               this.damageAndTryClearHeldItems(livingEntity, level, entity.blockPosition());
            }
         } else if (entity instanceof ItemEntity itemEntity && entity.tickCount % 10 == 0) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1) {
               return;
            }

            if (this.tryRemoveEnchantment(stack, level, entity.blockPosition())) {
               return;
            }

            if (stack.isDamageableItem()) {
               int newDamage = stack.getDamageValue() + 5;
               if (newDamage >= stack.getMaxDamage() - 1) {
                  stack.setDamageValue(stack.getMaxDamage() - 1);
               } else {
                  stack.setDamageValue(newDamage);
               }
            }
         }
      }
   }

   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      if ((Integer)state.getValue(LEVEL) == 0 && random.nextInt(5) == 0) {
         double x = (double)pos.getX() + 0.3 + random.nextDouble() * 0.4;
         double y = (double)pos.getY() + 0.95;
         double z = (double)pos.getZ() + 0.3 + random.nextDouble() * 0.4;
         level.addParticle((ParticleOptions)ModParticleTypes.ACID_BUBBLE.get(), x, y, z, 0.0, 0.0, 0.0);
      }
   }

   private void damageAndTryCurseClear(Iterable<ItemStack> items, int damage, Level level, BlockPos pos, Entity entity) {
      for (ItemStack stack : items) {
         if (!stack.isEmpty()
            && (!stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage() - 1)
            && !this.tryRemoveEnchantment(stack, level, entity.blockPosition())
            && stack.isDamageableItem()
            && entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)entity;
            int newDamage = stack.getDamageValue() + damage;
            if (newDamage >= stack.getMaxDamage() - 1) {
               stack.setDamageValue(stack.getMaxDamage() - 1);
            } else {
               stack.hurtAndBreak(damage, living, living.getEquipmentSlotForItem(stack));
            }
         }
      }
   }

   private void damageAndTryClearHeldItems(LivingEntity entity, Level level, BlockPos pos) {
      ItemStack mainHand = entity.getMainHandItem();
      if (!mainHand.isEmpty()
         && mainHand.isDamageableItem()
         && mainHand.getDamageValue() < mainHand.getMaxDamage() - 1
         && !this.tryRemoveEnchantment(mainHand, level, entity.blockPosition())
         && mainHand.isDamageableItem()) {
         int newDamage = mainHand.getDamageValue() + 5;
         if (newDamage >= mainHand.getMaxDamage() - 1) {
            mainHand.setDamageValue(mainHand.getMaxDamage() - 1);
         } else {
            mainHand.hurtAndBreak(5, entity, LivingEntity.getSlotForHand(InteractionHand.MAIN_HAND));
         }
      }

      ItemStack offHand = entity.getOffhandItem();
      if (!offHand.isEmpty()
         && offHand.isDamageableItem()
         && offHand.getDamageValue() < offHand.getMaxDamage() - 1
         && !this.tryRemoveEnchantment(offHand, level, entity.blockPosition())
         && offHand.isDamageableItem()) {
         int newDamage = offHand.getDamageValue() + 5;
         if (newDamage >= offHand.getMaxDamage() - 1) {
            offHand.setDamageValue(offHand.getMaxDamage() - 1);
         } else {
            offHand.hurtAndBreak(5, entity, LivingEntity.getSlotForHand(InteractionHand.OFF_HAND));
         }
      }
   }

   private boolean tryRemoveEnchantment(ItemStack stack, Level level, BlockPos pos) {
      if (!stack.isEmpty() && !(level.random.nextFloat() > 0.15F)) {
         Map<Holder<Enchantment>, Integer> enchantments = ScEnchants.getEnchantments(stack);
         if (enchantments.isEmpty()) {
            return false;
         } else {
            List<Holder<Enchantment>> allEnchants = new ArrayList<>(enchantments.keySet());
            Holder<Enchantment> toRemove = allEnchants.get(level.random.nextInt(allEnchants.size()));
            enchantments.remove(toRemove);
            ScEnchants.setEnchantments(stack, enchantments);
            this.spawnEnchantmentRemovalParticles(level, pos);
            return true;
         }
      } else {
         return false;
      }
   }

   private void spawnEnchantmentRemovalParticles(Level level, BlockPos pos) {
      if (level instanceof ServerLevel serverLevel) {
         for (int i = 0; i < 20; i++) {
            double offsetX = level.random.nextGaussian() * 0.3;
            double offsetY = level.random.nextDouble() * 0.5 + 0.5;
            double offsetZ = level.random.nextGaussian() * 0.3;
            serverLevel.sendParticles(
               ParticleTypes.ELECTRIC_SPARK,
               (double)pos.getX() + 0.5 + offsetX,
               (double)pos.getY() + offsetY,
               (double)pos.getZ() + 0.5 + offsetZ,
               1,
               0.0,
               0.1,
               0.0,
               0.05
            );
         }

         for (int i = 0; i < 10; i++) {
            double offsetX = level.random.nextGaussian() * 0.2;
            double offsetY = level.random.nextDouble() * 0.5 + 0.5;
            double offsetZ = level.random.nextGaussian() * 0.2;
            serverLevel.sendParticles(
               ParticleTypes.SMOKE,
               (double)pos.getX() + 0.5 + offsetX,
               (double)pos.getY() + offsetY,
               (double)pos.getZ() + 0.5 + offsetZ,
               1,
               0.0,
               0.1,
               0.0,
               0.02
            );
         }
      }
   }
}
