package top.ribs.scguns.entity.ai;


import top.ribs.scguns.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.event.GunEventBus;
import top.ribs.scguns.init.ModEffects;
import top.ribs.scguns.init.ModSounds;
import top.ribs.scguns.item.GunItem;

public class GunAttackGoal<T extends PathfinderMob> extends Goal {
   protected final T shooter;
   protected final double speedModifier;
   protected int seeTime;
   protected int attackTime;
   protected final float attackRadiusSqr;
   protected double idealRange;
   protected double minRange;
   protected float accuracyModifier = 1.0F;
   protected int strafingTime = -1;
   protected boolean shouldStrafe = false;
   protected float strafeAmount = 0.0F;
   protected int aimingStabilityTimer = 0;
   protected static final int MIN_AIM_TIME = 10;
   protected int burstIntervalTimer = 0;
   protected int remainingBursts = 0;
   protected int burstResetTimer = 0;
   protected int reloadTick = 0;
   protected boolean isReloading = false;
   protected boolean isPanicked = false;
   protected int panickTimer = 0;
   protected AIType aiType;
   protected Vec3 lastKnownPosition;
   protected int burstAmount = 3;
   protected int burstTimer = 15;
   protected static final float ROTATION_SPEED = 15.0F;

   public GunAttackGoal(T shooter, ItemStack gunStack, float speedModifier, AIType aiType, int difficulty) {
      super();
      this.shooter = shooter;
      this.speedModifier = (double)speedModifier;
      this.attackTime = -1;
      this.aiType = aiType;
      shooter.addTag("AI_" + aiType.name());
      if (gunStack.getItem() instanceof GunItem gunItem) {
         Gun gun = gunItem.getModifiedGun(gunStack);
         this.idealRange = gun.getIdealAttackRange();
         this.minRange = gun.getMinAttackRange();
      } else {
         this.idealRange = 15.0;
         this.minRange = 8.0;
      }

      this.attackRadiusSqr = (float)(this.idealRange * this.idealRange);
      if (this.shooter.getTarget() != null) {
         this.lastKnownPosition = this.shooter.getTarget().position();
      }
      float baseAccuracy = switch (aiType) {
         case TACTICAL -> 2.5F;
         case SMART -> 2.2F;
         case DEFAULT -> 2.0F;
         case RECKLESS -> 1.2F;
         case COWARD -> 1.5F;
      };
      float difficultyBonus = 1.0F + (float)(difficulty - 1) * 0.3F;
      this.accuracyModifier = baseAccuracy * difficultyBonus;
      this.burstAmount = 2 + difficulty / 2;
      float burstDelayMultiplier = this.getBurstDelayMultiplier(shooter.level().getDifficulty());
      float configBurstMultiplier = ((Double)Config.COMMON.gameplay.mobBurstDelayMultiplier.get()).floatValue();
      this.burstTimer = Math.max(1, (int)((float)(30 - difficulty * 4) * burstDelayMultiplier * configBurstMultiplier));
   }

   private float getBurstDelayMultiplier(Difficulty difficulty) {
      return switch (difficulty) {
         case PEACEFUL -> 2.0F;
         case EASY -> 1.5F;
         case NORMAL -> 1.0F;
         case HARD -> 0.6F;
         default -> throw new IncompatibleClassChangeError();
      };
   }

   public boolean canUse() {
      return this.shooter.getTarget() != null && this.isHoldingGun() && !this.shooter.getTarget().isDeadOrDying();
   }

   protected boolean isHoldingGun() {
      return this.shooter.isHolding(itemStack -> itemStack.getItem() instanceof GunItem);
   }

   /**
    * Ammo count of a mob's gun, treating "no custom data at all" as empty.
    *
    * <p>A gun handed to a mob by anything other than the creative tab can have
    * no custom data component, and {@code getTag} returns null for that. 0.5.5
    * dereferenced it directly, so a raider whose gun was created without an
    * {@code AmmoCount} crashed the server from {@link #tick()}. Reading it as 0
    * instead makes the mob reload, which is the behaviour we want anyway.</p>
    */
   protected static int getAmmoCount(ItemStack stack) {
      CompoundTag tag = NbtHelper.getTag(stack);
      return tag == null ? 0 : tag.getInt("AmmoCount");
   }

   public void start() {
      super.start();
      this.shooter.setAggressive(true);
   }

   public void stop() {
      super.stop();
      this.shooter.setAggressive(false);
      this.seeTime = 0;
      this.attackTime = -1;
      this.shooter.stopUsingItem();
      this.reloadTick = 0;
      this.isReloading = false;
      this.strafingTime = -1;
      this.shouldStrafe = false;
      this.aimingStabilityTimer = 0;
      this.shooter.removeTag("AI_" + this.aiType.name());
   }

   public boolean requiresUpdateEveryTick() {
      return true;
   }

   public void tick() {
      LivingEntity target = this.shooter.getTarget();
      ItemStack heldItem = this.shooter.getMainHandItem();
      if (this.shooter.hasEffect(ModEffects.BLINDED) || this.shooter.hasEffect(ModEffects.DEAFENED)) {
         this.isPanicked = true;
      }

      if (target != null && heldItem.getItem() instanceof GunItem gunItem) {
         Gun gun = gunItem.getModifiedGun(heldItem);
         double distanceToTarget = this.shooter.distanceToSqr(target.getX(), target.getY(), target.getZ());
         boolean canSeeTarget = this.shooter.getSensing().hasLineOfSight(target);
         boolean sawTargetPreviously = this.seeTime > 0;
         if (canSeeTarget != sawTargetPreviously) {
            this.seeTime = 0;
         }

         if (this.isReloading) {
            this.seeTime++;
         } else if (canSeeTarget) {
            this.lastKnownPosition = new Vec3(target.getX(), target.getY(), target.getZ());
            this.seeTime++;
         } else {
            this.seeTime--;
         }

         if (this.aiType == AIType.COWARD && (this.shooter.getHealth() < this.shooter.getMaxHealth() / 3.0F || this.shooter.invulnerableTime != 0)
            || this.shooter.hasEffect(ModEffects.BLINDED)) {
            this.isPanicked = true;
            this.panickTimer = 20;
         }

         if (this.isPanicked) {
            if (this.shooter.tickCount % 10 == 0) {
               Vec3 vec3 = DefaultRandomPos.getPos(this.shooter, 5, 4);
               if (vec3 != null) {
                  this.shooter.getNavigation().moveTo(vec3.x, vec3.y, vec3.z, this.speedModifier * 1.5);
               }
            }

            this.panickTimer--;
            if (this.panickTimer <= 0) {
               this.isPanicked = false;
            }

            return;
         }

         if (getAmmoCount(heldItem) <= 0) {
            if (!this.isReloading) {
               if (this.aiType == AIType.TACTICAL) {
                  Vec3 coverLocation = this.findCoverLocation();
                  this.shooter.getNavigation().moveTo(coverLocation.x, coverLocation.y, coverLocation.z, this.speedModifier);
               }

               this.isReloading = true;
               this.reloadTick = gun.getReloads().getReloadTimer();
               this.shooter
                  .level()
                  .playSound(
                     null,
                     this.shooter.getX(),
                     this.shooter.getY(),
                     this.shooter.getZ(),
                     (SoundEvent)ModSounds.ITEM_PISTOL_RELOAD.get(),
                     SoundSource.HOSTILE,
                     1.0F,
                     1.0F
                  );
            } else if (this.reloadTick == 0) {
               NbtHelper.getOrCreateTag(heldItem).putInt("AmmoCount", gun.getReloads().getMaxAmmo());
               this.shooter
                  .level()
                  .playSound(
                     null,
                     this.shooter.getX(),
                     this.shooter.getY(),
                     this.shooter.getZ(),
                     (SoundEvent)ModSounds.ITEM_PISTOL_COCK.get(),
                     SoundSource.HOSTILE,
                     1.0F,
                     1.0F
                  );
               this.isReloading = false;
            } else {
               this.shooter.getNavigation().stop();
               this.reloadTick--;
            }

            return;
         }

         boolean inRange = distanceToTarget <= (double)this.attackRadiusSqr;
         boolean tooClose = distanceToTarget < this.minRange * this.minRange;
         boolean isRetreating = false;
         boolean isMovingFast = this.shooter.getDeltaMovement().horizontalDistanceSqr() > 0.01;
         boolean isNavigating = !this.shooter.getNavigation().isDone();
         if (inRange && canSeeTarget) {
            if (tooClose && this.aiType != AIType.RECKLESS) {
               Vec3 awayVector = this.shooter.position().subtract(target.position()).normalize();
               Vec3 retreatPos = this.shooter.position().add(awayVector.scale(2.0));
               this.shooter.getNavigation().moveTo(retreatPos.x, retreatPos.y, retreatPos.z, this.speedModifier * 0.8);
               this.shouldStrafe = false;
               this.strafingTime = -1;
               this.aimingStabilityTimer = 0;
               isRetreating = true;
               if (this.aiType == AIType.SMART && distanceToTarget > this.minRange * this.minRange * 0.8) {
                  this.shooter.getNavigation().stop();
                  isRetreating = false;
               }
            } else {
               this.shooter.getNavigation().stop();
               if (this.aiType != AIType.RECKLESS) {
                  if (this.strafingTime < 0) {
                     if (this.shooter.getRandom().nextFloat() < 0.2F) {
                        this.shouldStrafe = true;
                        this.strafingTime = 20 + this.shooter.getRandom().nextInt(20);
                        this.strafeAmount = this.shooter.getRandom().nextBoolean() ? 0.5F : -0.5F;
                     } else {
                        this.shouldStrafe = false;
                        this.strafingTime = 40 + this.shooter.getRandom().nextInt(40);
                     }
                  }

                  if (this.strafingTime > 0) {
                     this.strafingTime--;
                     if (this.shouldStrafe) {
                        this.shooter.getMoveControl().strafe(0.0F, this.strafeAmount * 0.5F);
                     }
                  }
               }
            }
         } else {
            if (this.shooter.tickCount % 20 == 0 || this.shooter.getNavigation().isDone()) {
               if (this.aiType == AIType.RECKLESS) {
                  this.shooter.getNavigation().moveTo(target, this.speedModifier * 1.2);
               } else {
                  this.shooter.getNavigation().moveTo(target, this.speedModifier);
               }
            }

            this.shouldStrafe = false;
            this.strafingTime = -1;
            this.aimingStabilityTimer = 0;
         }

         if (canSeeTarget && !isRetreating && !isMovingFast) {
            this.updateSmoothRotation(target);
            if (this.aimingStabilityTimer < 10) {
               this.aimingStabilityTimer++;
            }
         } else if (canSeeTarget && this.aiType == AIType.SMART && isNavigating) {
            this.aimingStabilityTimer = 0;
         } else {
            this.aimingStabilityTimer = 0;
         }

         boolean isStableAndAimed = this.aimingStabilityTimer >= 10;
         boolean canShootWhileMoving = this.aiType == AIType.RECKLESS || this.aiType != AIType.SMART && !isNavigating;
         boolean smartShouldShoot = this.aiType == AIType.SMART && !isNavigating && !isMovingFast && this.shooter.getNavigation().isDone();
         if (inRange
            && canSeeTarget
            && this.seeTime >= 5
            && !isRetreating
            && (canShootWhileMoving || smartShouldShoot)
            && !isMovingFast
            && isStableAndAimed
            && getAmmoCount(this.shooter.getMainHandItem()) > 0
            && --this.attackTime <= 0) {
            float configBurstMultiplier = ((Double)Config.COMMON.gameplay.mobBurstDelayMultiplier.get()).floatValue();
            if (this.remainingBursts <= 0 && this.burstResetTimer <= 0) {
               this.remainingBursts = 1 + this.shooter.level().random.nextInt(this.burstAmount);
               this.burstIntervalTimer = 1 + this.shooter.level().random.nextInt(this.burstTimer);
               this.burstResetTimer = Math.max(5, (int)((float)(40 + this.shooter.level().random.nextInt(40)) * configBurstMultiplier));
            }

            if (this.shooter.hasEffect(ModEffects.BLINDED) && !this.aiType.equals(AIType.TACTICAL)) {
               this.burstResetTimer = 0;
            }

            if (this.remainingBursts > 0 && --this.burstIntervalTimer <= 0) {
               this.shoot(target, gun);
               this.remainingBursts--;
               this.burstIntervalTimer = 2 + this.shooter.level().random.nextInt(6);
            }

            if (this.remainingBursts <= 0) {
               this.burstResetTimer--;
            }
         }
      }
   }

   private void updateSmoothRotation(LivingEntity target) {
      Vec3 targetPos = target.position().add(0.0, (double)target.getEyeHeight() * 0.8, 0.0);
      Vec3 shooterPos = this.shooter.position().add(0.0, (double)this.shooter.getEyeHeight(), 0.0);
      Vec3 toTarget = targetPos.subtract(shooterPos);
      double horizontalDist = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
      float desiredYaw = (float)(Math.atan2(toTarget.z, toTarget.x) * (180.0 / Math.PI)) - 90.0F;
      float desiredPitch = (float)(-(Math.atan2(toTarget.y, horizontalDist) * (180.0 / Math.PI)));
      float yawDiff = desiredYaw - this.shooter.getYRot();

      while (yawDiff > 180.0F) {
         yawDiff -= 360.0F;
      }

      while (yawDiff < -180.0F) {
         yawDiff += 360.0F;
      }

      float newYaw = this.shooter.getYRot() + Math.max(-15.0F, Math.min(15.0F, yawDiff));
      float newPitch = this.shooter.getXRot() + Math.max(-15.0F, Math.min(15.0F, desiredPitch - this.shooter.getXRot()));
      this.shooter.setYRot(newYaw);
      this.shooter.setXRot(newPitch);
      this.shooter.yBodyRot = newYaw;
      this.shooter.yBodyRotO = this.shooter.yBodyRot;
      this.shooter.yHeadRot = newYaw;
      this.shooter.yHeadRotO = this.shooter.yHeadRot;
   }

   private void shoot(LivingEntity target, Gun gun) {
      if (!this.shooter.hasEffect(ModEffects.BLINDED) || !this.shooter.getRandom().nextBoolean()) {
         ItemStack heldItem = this.shooter.getMainHandItem();
         // One shared firing pipeline for every mob (HANDOFF section 82.9): the rate chain, the player's
         // ammo rules, the silenced/enchanted fire sound and the casing all live in MobGunFire, so this AI
         // and the guards' cannot drift apart. What stays here is the raider behaviour around it - the
         // bursts, the approach to the gun's ideal range, cover and panic.
         MobGunFire.fire(this.shooter, target, heldItem, this.accuracyModifier);
         this.attackTime = MobGunFire.fireInterval(heldItem, gun);
      }
   }

   private Vec3 findCoverLocation() {
      Vec3 targetPos = new Vec3(this.shooter.getTarget().getX(), this.shooter.getTarget().getY(), this.shooter.getTarget().getZ());
      Vec3 mobPos = this.shooter.position();
      return mobPos.add(mobPos.subtract(targetPos).normalize().scale(3.0));
   }
}
