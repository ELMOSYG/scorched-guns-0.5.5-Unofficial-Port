package top.ribs.scguns.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import top.ribs.scguns.entity.throwable.ThrowableShotballEntity;

public class ThrowableShotballItem extends AmmoItem {
   private static final int MAX_CHARGE_TIME = 60;

   public ThrowableShotballItem(Properties properties) {
      super(properties);
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.BOW;
   }

   @Override
   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 60;
   }

   public void onUseTick(Level level, LivingEntity player, ItemStack stack, int count) {
      int duration = this.getUseDuration(stack, player) - count;
      if (duration == 5) {
         player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_START.value(), SoundSource.PLAYERS, 0.5F, 1.2F, false);
      }
   }

   public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
      ItemStack stack = playerIn.getItemInHand(handIn);
      if (playerIn.isUnderWater()) {
         return InteractionResultHolder.fail(stack);
      } else {
         playerIn.startUsingItem(handIn);
         return InteractionResultHolder.consume(stack);
      }
   }

   public ItemStack finishUsingItem(ItemStack stack, Level worldIn, LivingEntity entityLiving) {
      if (!worldIn.isClientSide() && !entityLiving.isUnderWater()) {
         if (!(entityLiving instanceof Player) || !((Player)entityLiving).isCreative()) {
            stack.shrink(1);
         }

         ThrowableShotballEntity shotball = new ThrowableShotballEntity(worldIn, entityLiving);
         shotball.shootFromRotation(entityLiving, entityLiving.getXRot(), entityLiving.getYRot(), 0.0F, 1.5F, 0.5F);
         worldIn.addFreshEntity(shotball);
         worldIn.playSound(null, entityLiving.getX(), entityLiving.getY(), entityLiving.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 1.0F, 0.8F);
         if (entityLiving instanceof Player) {
            ((Player)entityLiving).awardStat(Stats.ITEM_USED.get(this));
         }
      }

      return stack;
   }

   public void releaseUsing(ItemStack stack, Level worldIn, LivingEntity entityLiving, int timeLeft) {
      if (!worldIn.isClientSide() && !entityLiving.isUnderWater()) {
         int chargeDuration = this.getUseDuration(stack, entityLiving) - timeLeft;
         if (chargeDuration >= 5) {
            if (!(entityLiving instanceof Player) || !((Player)entityLiving).isCreative()) {
               stack.shrink(1);
            }

            ThrowableShotballEntity shotball = new ThrowableShotballEntity(worldIn, entityLiving);
            float power = Math.min(5.0F, 1.0F + (float)chargeDuration / 60.0F);
            float accuracy = Math.max(0.5F, 1.0F - (float)chargeDuration / 60.0F * 0.5F);
            shotball.shootFromRotation(entityLiving, entityLiving.getXRot(), entityLiving.getYRot(), 0.0F, power, accuracy);
            worldIn.addFreshEntity(shotball);
            worldIn.playSound(
               null,
               entityLiving.getX(),
               entityLiving.getY(),
               entityLiving.getZ(),
               SoundEvents.SNOWBALL_THROW,
               SoundSource.NEUTRAL,
               1.0F,
               0.6F + power * 0.4F
            );
            if (entityLiving instanceof Player) {
               ((Player)entityLiving).awardStat(Stats.ITEM_USED.get(this));
            }
         }
      }
   }
}
