package top.ribs.scguns.entity.util;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public interface IGoldConsumingEntity {
   boolean isEatingGold();

   boolean isPreparingToEat();

   ItemStack getHeldFoodItem();

   ItemEntity getTargetGoldItem();

   void setTargetGoldItem(ItemEntity var1);

   float getAccumulatedGoldValue();

   void setAccumulatedGoldValue(float var1);

   void addAccumulatedGoldValue(float var1);

   int getGoldEatingCooldown();

   void setGoldEatingCooldown(int var1);

   int getSlagProductionCooldown();

   void setSlagProductionCooldown(int var1);
}
