package top.ribs.scguns.entity.ai;

import java.util.EnumSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.AABB;
import top.ribs.scguns.entity.monster.HornlinEntity;
import top.ribs.scguns.entity.monster.ZombifiedHornlinEntity;
import top.ribs.scguns.entity.util.GoldConsumptionHelper;
import top.ribs.scguns.entity.util.IGoldConsumingEntity;
import top.ribs.scguns.init.ModItems;

public class ConsumeGoldGoal extends Goal {
   private final Mob entity;
   private final IGoldConsumingEntity goldConsumer;
   private int goldEatingTime = 0;
   private int eatingPreparationTime = 0;
   private ItemStack heldFoodItem = ItemStack.EMPTY;
   private static final int EATING_DURATION = 40;
   private static final int PREPARATION_DURATION = 15;
   private static final float PICKUP_RANGE = 1.5F;
   private static final int COOLDOWN_AFTER_EATING = 40;
   private static final int COOLDOWN_AFTER_CANCEL = 20;
   private static final float GOLD_VALUE_FOR_SLAG = 10.0F;
   private static final float POISON_GOLD_REDUCTION = 8.0F;
   private static final int SLAG_PRODUCTION_COOLDOWN = 150;

   public ConsumeGoldGoal(Mob entity, IGoldConsumingEntity goldConsumer) {
      super();
      this.entity = entity;
      this.goldConsumer = goldConsumer;
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   public boolean canUse() {
      if (this.goldConsumer.isEatingGold() || this.goldConsumer.isPreparingToEat()) {
         return true;
      } else {
         return this.goldConsumer.getGoldEatingCooldown() > 0 ? false : this.findNearbyGold() != null;
      }
   }

   public boolean canContinueToUse() {
      return this.goldConsumer.isEatingGold() || this.goldConsumer.isPreparingToEat() || this.eatingPreparationTime > 0;
   }

   public void start() {
      this.entity.getNavigation().stop();
      if (!this.goldConsumer.isEatingGold() && !this.goldConsumer.isPreparingToEat()) {
         ItemEntity nearbyGold = this.findNearbyGold();
         if (nearbyGold != null) {
            this.prepareToEat(nearbyGold);
         }
      }
   }

   public void stop() {
      if (this.goldConsumer.isEatingGold() || this.goldConsumer.isPreparingToEat()) {
         if (!this.heldFoodItem.isEmpty()) {
            this.entity.spawnAtLocation(this.heldFoodItem);
         }

         this.setEntityState(ItemStack.EMPTY, false, false);
      }

      this.resetEatingState();
   }

   public void tick() {
      if (this.eatingPreparationTime > 0) {
         this.eatingPreparationTime--;
         this.entity.getNavigation().stop();
         if (this.eatingPreparationTime <= 0) {
            this.startEating();
         }
      } else if (this.goldConsumer.isEatingGold()) {
         this.goldEatingTime--;
         if (this.goldEatingTime % 8 == 0) {
            GoldConsumptionHelper.showEatingParticles(this.entity);
         }

         if (this.goldEatingTime <= 0) {
            this.finishEating();
         }
      } else {
         this.handleSlagProduction();
      }
   }

   private ItemEntity findNearbyGold() {
      AABB closeArea = new AABB(
         this.entity.getX() - 1.5,
         this.entity.getY() - 0.5,
         this.entity.getZ() - 1.5,
         this.entity.getX() + 1.5,
         this.entity.getY() + 1.5,
         this.entity.getZ() + 1.5
      );
      return this.entity
         .level()
         .getEntitiesOfClass(ItemEntity.class, closeArea)
         .stream()
         .filter(item -> !item.isRemoved())
         .filter(item -> GoldConsumptionHelper.isGoldItem(item.getItem()))
         .filter(item -> this.entity.distanceTo(item) <= 1.5F)
         .findFirst()
         .orElse(null);
   }

   private void prepareToEat(ItemEntity goldItem) {
      this.goldConsumer.setTargetGoldItem(goldItem);
      if (goldItem != null && !goldItem.isRemoved()) {
         ItemStack groundStack = goldItem.getItem();
         this.heldFoodItem = groundStack.copy();
         this.heldFoodItem.setCount(1);
         this.setEntityState(this.heldFoodItem, false, true);
         groundStack.shrink(1);
         if (groundStack.isEmpty()) {
            goldItem.discard();
         }

         this.eatingPreparationTime = 15;
         this.entity.getNavigation().stop();
         this.entity.playSound(SoundEvents.ITEM_PICKUP, 0.8F, 1.2F + this.entity.getRandom().nextFloat() * 0.4F);
      } else {
         this.cancelEating();
      }
   }

   private void startEating() {
      this.setEntityState(this.heldFoodItem, true, false);
      this.goldEatingTime = 40;
      this.entity.playSound(SoundEvents.GENERIC_EAT, 0.8F, 1.0F + this.entity.getRandom().nextFloat() * 0.2F);
   }

   private void finishEating() {
      if (!this.heldFoodItem.isEmpty()) {
         this.applyFoodEffects(this.heldFoodItem);
      }

      this.entity.playSound(SoundEvents.PLAYER_BURP, 0.8F, 1.3F);
      this.setEntityState(ItemStack.EMPTY, false, false);
      this.resetEatingState();
      this.goldConsumer.setGoldEatingCooldown(40);
   }

   private void applyFoodEffects(ItemStack foodStack) {
      if (GoldConsumptionHelper.isPoisonItem(foodStack)) {
         GoldConsumptionHelper.applyPoisonEffects(this.entity, foodStack);
         float currentGold = this.goldConsumer.getAccumulatedGoldValue();
         this.goldConsumer.setAccumulatedGoldValue(Math.max(0.0F, currentGold - 8.0F));
      } else {
         float healthToRestore = GoldConsumptionHelper.getHealthFromGold(foodStack);
         float goldNuggetValue = GoldConsumptionHelper.getGoldNuggetValue(foodStack);
         this.entity.heal(healthToRestore);
         this.goldConsumer.addAccumulatedGoldValue(goldNuggetValue);
      }
   }

   private void cancelEating() {
      if (!this.heldFoodItem.isEmpty()) {
         this.entity.spawnAtLocation(this.heldFoodItem);
      }

      this.setEntityState(ItemStack.EMPTY, false, false);
      this.resetEatingState();
      this.goldConsumer.setGoldEatingCooldown(20);
   }

   private void resetEatingState() {
      this.eatingPreparationTime = 0;
      this.goldEatingTime = 0;
      this.heldFoodItem = ItemStack.EMPTY;
      this.goldConsumer.setTargetGoldItem(null);
   }

   private void handleSlagProduction() {
      if (this.goldConsumer.getSlagProductionCooldown() <= 0 && !this.goldConsumer.isEatingGold() && !this.goldConsumer.isPreparingToEat()) {
         if (this.goldConsumer.getAccumulatedGoldValue() >= 10.0F) {
            this.produceSlag();
         }
      }
   }

   private void produceSlag() {
      float currentGold = this.goldConsumer.getAccumulatedGoldValue();
      this.goldConsumer.setAccumulatedGoldValue(Math.max(0.0F, currentGold - 10.0F));
      this.entity.playSound(SoundEvents.PLAYER_BURP, 1.2F, 0.5F + this.entity.getRandom().nextFloat() * 0.3F);
      GoldConsumptionHelper.showSlagProductionParticles(this.entity);
      ItemStack slagStack = new ItemStack((ItemLike)ModItems.AUREOUS_SLAG.get());
      this.entity.spawnAtLocation(slagStack);
      this.goldConsumer.setSlagProductionCooldown(150);
   }

   private void setEntityState(ItemStack heldItem, boolean eating, boolean preparing) {
      if (this.entity instanceof HornlinEntity hornlin) {
         hornlin.setHeldFoodItem(heldItem);
         hornlin.setEatingGold(eating);
         hornlin.setPreparingToEat(preparing);
      } else if (this.entity instanceof ZombifiedHornlinEntity zombified) {
         zombified.setHeldFoodItem(heldItem);
         zombified.setEatingGold(eating);
         zombified.setPreparingToEat(preparing);
      }
   }
}
