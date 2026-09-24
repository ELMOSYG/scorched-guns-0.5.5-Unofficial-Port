package top.ribs.scguns.entity.ai;

import java.util.EnumSet;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import top.ribs.scguns.entity.util.GoldConsumptionHelper;
import top.ribs.scguns.entity.util.IGoldConsumingEntity;

public class GoldSeekingGoal extends Goal {
   private final Mob entity;
   private final IGoldConsumingEntity goldConsumer;
   private final double speed;
   private final float searchRange;
   private ItemEntity targetGold;

   public GoldSeekingGoal(Mob entity, IGoldConsumingEntity goldConsumer, double speed, float searchRange) {
      super();
      this.entity = entity;
      this.goldConsumer = goldConsumer;
      this.speed = speed;
      this.searchRange = searchRange;
      this.setFlags(EnumSet.of(Flag.MOVE));
   }

   public boolean canUse() {
      if (!this.goldConsumer.isEatingGold() && !this.goldConsumer.isPreparingToEat()) {
         this.targetGold = this.findNearestGold();
         return this.targetGold != null;
      } else {
         return false;
      }
   }

   public boolean canContinueToUse() {
      return !this.goldConsumer.isEatingGold() && !this.goldConsumer.isPreparingToEat() ? this.targetGold != null && !this.targetGold.isRemoved() : false;
   }

   public void start() {
      if (this.targetGold != null) {
         this.entity.getNavigation().moveTo(this.targetGold, this.speed);
      }
   }

   public void stop() {
      this.targetGold = null;
      this.entity.getNavigation().stop();
   }

   public void tick() {
      if (this.targetGold != null && this.entity.getNavigation().isDone()) {
         this.entity.getNavigation().moveTo(this.targetGold, this.speed);
      }
   }

   private ItemEntity findNearestGold() {
      AABB searchArea = AABB.unitCubeFromLowerCorner(this.entity.position()).inflate((double)this.searchRange);
      return this.entity
         .level()
         .getEntitiesOfClass(ItemEntity.class, searchArea)
         .stream()
         .filter(item -> !item.isRemoved())
         .filter(item -> GoldConsumptionHelper.isGoldItem(item.getItem()))
         .min((item1, item2) -> Double.compare(this.entity.distanceToSqr(item1), this.entity.distanceToSqr(item2)))
         .orElse(null);
   }
}
