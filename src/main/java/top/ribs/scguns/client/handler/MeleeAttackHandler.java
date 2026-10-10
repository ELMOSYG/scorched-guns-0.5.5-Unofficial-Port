package top.ribs.scguns.client.handler;





import top.ribs.scguns.util.ScEnchants;
import net.minecraft.core.Holder;
import top.ribs.scguns.util.DistHelper;
import top.ribs.scguns.util.NbtHelper;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import top.ribs.scguns.util.MobType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.api.distmarker.Dist;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.common.ReloadType;
import top.ribs.scguns.enchantment.CorrodedEnchantment;
import top.ribs.scguns.Config;
import top.ribs.scguns.event.GunEventBus;
import top.ribs.scguns.init.ModEnchantments;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.item.BayonetItem;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.animated.AnimatedGunItem;
import top.ribs.scguns.item.attachment.IAttachment;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.C2SMessageReload;
import top.ribs.scguns.network.message.S2CMessageMeleeAttack;
import top.ribs.scguns.util.GunModifierHelper;

public class MeleeAttackHandler {
   private static final float ENCHANTMENT_DAMAGE_SCALING_FACTOR = 0.7F;
   private static final float BASE_SPEED_DAMAGE_SCALING_FACTOR = 0.0F;
   private static final String WALL_COLLISION_COOLDOWN_TAG = "WallCollisionCooldown";
   private static final String MELEE_COOLDOWN_TAG = "MeleeCooldown";
   private static final String KNOCKBACK_GRACE_TAG = "KnockbackGracePeriod";
   /** When the sprint flag was first seen missing, so the tolerance can be measured in the player's saved data. */
   private static final String BANZAI_SPRINT_LOST_TAG = "BanzaiSprintLostAt";
   private static final String BANZAI_DAMAGE_COOLDOWN_TAG = "BanzaiDamageCooldown";
   private static boolean isBanzai = false;
   /**
    * The wall check's ray angles, derived from {@code bayonetCharge.wallCheckSpreadDegrees} (HANDOFF section
    * 82.32). Cached because this is read on every charge tick and building an array there would allocate once a
    * tick; the cache is rebuilt whenever the configured spread changes.
    */
   private static double cachedWallSpread = Double.NaN;
   private static double[] cachedWallAngles = new double[0];
   private static ItemStack banzaiActiveItem = ItemStack.EMPTY;
   /** Who is charging, so the state below can be synced to that player's client (HANDOFF section 82.22). */
   private static ServerPlayer banzaiPlayer;

   /**
    * The item each charging player started the charge with, keyed by UUID.
    *
    * <p>A single static item only works with one player: with two, one player's ticker compared against the
    * other player's item, decided the charge was over and cleared the wrong player's synced BANZAI key.</p>
    */
   private static final java.util.Map<java.util.UUID, ItemStack> BANZAI_ACTIVE_ITEMS = new java.util.concurrent.ConcurrentHashMap<>();

   /** The bayonet charge's own options - every number this mechanic uses lives in that section. */
   private static Config.BayonetCharge charge() {
      return Config.COMMON.bayonetCharge;
   }

   /**
    * The wall check rays: straight ahead, then a third, two thirds and all of the configured spread to each
    * side. A spread of 30 degrees reproduces the 0, 10, 20, 30 degree rays the code used to hard-code.
    */
   private static double[] wallCheckAngles() {
      double spread = charge().wallCheckSpreadDegrees.get();
      if (spread != cachedWallSpread) {
         cachedWallSpread = spread;
         double third = spread / 3.0;
         cachedWallAngles = new double[]{0.0, third, -third, third * 2.0, -third * 2.0, spread, -spread};
      }

      return cachedWallAngles;
   }

   /**
    * The charge's damage multiplier for a bayonet's banzai level: {@code 1.0 + speed * factor}, with the factor
    * coming from the configured level scaling.
    */
   private static float banzaiDamageFactor(int banzaiLevel) {
      return switch (banzaiLevel) {
         case 1 -> charge().damageScalingLevel1.get().floatValue();
         case 2 -> charge().damageScalingLevel2.get().floatValue();
         case 3 -> charge().damageScalingLevel3.get().floatValue();
         default -> BASE_SPEED_DAMAGE_SCALING_FACTOR;
      };
   }

   /** Whether the charge is switched on at all. */
   public static boolean isBanzaiEnabled() {
      return (Boolean)charge().enabled.get();
   }

   /** Whether a charge requires a sprint to start. */
   public static boolean isSprintRequiredToStart() {
      return (Boolean)charge().requireSprintToStart.get();
   }

   /** Whether running a charge into a wall knocks the player back. */
   public static boolean isWallImpactEnabled() {
      return (Boolean)charge().wallImpactEnabled.get();
   }

   /** Whether a successful stab throws the player back off the target. */
   public static boolean isKnockPlayerBackOnHit() {
      return (Boolean)charge().knockPlayerBackOnHit.get();
   }

   /** Whether a successful stab spends the charge. */
   public static boolean isEndChargeOnHit() {
      return (Boolean)charge().endChargeOnHit.get();
   }

   /**
    * Which mechanic the charge uses (HANDOFF section 82.34). Off by default: the original area charge is what
    * ships, and the single target thrust is an option.
    */
   public static boolean isSingleTargetStab() {
      return (Boolean)charge().singleTargetStab.get();
   }

   public MeleeAttackHandler() {
      super();
   }

   /**
    * The server's own view of the charge. Not the client's - see {@link #isBanzaiCharging(Player)}.
    */
   public static boolean isBanzaiActive() {
      return isBanzai;
   }

   /**
    * Whether this player is charging with a bayonet, as the client knows it (HANDOFF section 82.22).
    *
    * <p>The animation lives on the client and the decision lives on the server, so it is carried by
    * {@link ModSyncedDataKeys#BANZAI}. Reading the static field instead only ever worked in single player.</p>
    */
   public static boolean isBanzaiCharging(Player player) {
      return player != null && Boolean.TRUE.equals(ModSyncedDataKeys.BANZAI.getValue(player));
   }

   public static void startBanzai(ServerPlayer player) {
      ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
      if (heldItem.getItem() instanceof GunItem gunItem) {
         if (!gunItem.hasBayonet(heldItem)) {
            performMeleeAttack(player);
         } else {
            isBanzai = true;
            banzaiActiveItem = heldItem.copy();
      BANZAI_ACTIVE_ITEMS.put(player.getUUID(), heldItem.copy());
            banzaiPlayer = player;
            player.getPersistentData().remove(BANZAI_SPRINT_LOST_TAG);
            ModSyncedDataKeys.BANZAI.setValue(player, true);
         }
      }
   }

   public static void stopBanzai() {
      ServerPlayer player = banzaiPlayer;
      if (player != null) {
         stopBanzai(player);
      } else {
         isBanzai = false;
      }
   }

   /**
    * Ends the charge for one player. The synced BANZAI key belongs to that player, so clearing it is what
    * stops their client's charge animation - clearing another player's (as the single static field used to do)
    * left the real owner animating forever.
    */
   public static void stopBanzai(ServerPlayer player) {
      if (player != null) {
         player.getPersistentData().remove(BANZAI_SPRINT_LOST_TAG);
         BANZAI_ACTIVE_ITEMS.remove(player.getUUID());
         ModSyncedDataKeys.BANZAI.setValue(player, false);
      }
      if (banzaiPlayer == player) {
         banzaiPlayer = null;
         isBanzai = false;
         banzaiActiveItem = ItemStack.EMPTY;
      }
   }

   public static void performMeleeAttack(ServerPlayer player) {
      if (player != null) {
         ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
         if (heldItem.getItem() instanceof GunItem gunItem) {
            if (!isMeleeOnCooldown(player, heldItem)) {
               if (heldItem.getItem() instanceof AnimatedGunItem animatedGunItem) {
                  CompoundTag tag = NbtHelper.getTagForWrite(heldItem);
                  long id = GeoItem.getId(heldItem);
                  AnimationController<GeoAnimatable> animationController = (AnimationController<GeoAnimatable>)animatedGunItem.getAnimatableInstanceCache()
                     .getManagerForId(id)
                     .getAnimationControllers()
                     .get("controller");
                  if (tag != null && tag.getBoolean("scguns:IsReloading")) {
                     Gun gun = gunItem.getModifiedGun(heldItem);
                     if (gun.getReloads().getReloadType() == ReloadType.MAG_FED) {
                        tag.remove("scguns:IsReloading");
                        ModSyncedDataKeys.RELOADING.setValue(player, false);
                        PacketHandler.getPlayChannel().sendToServer(new C2SMessageReload(false));
                        if (animationController != null) {
                           animationController.forceAnimationReset();
                        }
                     } else if (gun.getReloads().getReloadType() == ReloadType.MANUAL
                        && animationController != null
                        && (
                           animatedGunItem.isAnimationPlaying(animationController, "reload_loop")
                              || animatedGunItem.isAnimationPlaying(animationController, "reload_start")
                        )) {
                        tag.putBoolean("scguns:ReloadComplete", true);
                        animationController.tryTriggerAnimation("reload_stop");
                        tag.remove("scguns:IsReloading");
                        ModSyncedDataKeys.RELOADING.setValue(player, false);
                        PacketHandler.getPlayChannel().sendToServer(new C2SMessageReload(false));
                     }
                  }
               }

               setMeleeCooldown(player, heldItem, gunItem);
               player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F);
               if (heldItem.getItem() instanceof AnimatedGunItem) {
                  Gun gun = gunItem.getModifiedGun(heldItem);
                  if (gun.getGeneral().usesCustomMeleeAnimation()) {
                     CompoundTag tag = NbtHelper.getOrCreateTag(heldItem);
                     tag.putBoolean("scguns:IsMelee", true);
                     tag.putLong("MeleeStartTime", System.currentTimeMillis());
                     ModSyncedDataKeys.MELEE.setValue(player, true);
                  }

                  AnimationController<GeoAnimatable> controller = (AnimationController<GeoAnimatable>)((AnimatedGunItem)heldItem.getItem())
                     .getAnimatableInstanceCache()
                     .getManagerForId(GeoItem.getId(heldItem))
                     .getAnimationControllers()
                     .get("controller");
                  if (controller != null && ((AnimatedGunItem)heldItem.getItem()).isAnimationPlaying(controller, "inspect")) {
                     controller.tryTriggerAnimation("idle");
                  }
               }

               PacketHandler.getPlayChannel().sendToPlayer(() -> player, new S2CMessageMeleeAttack(heldItem));
               LivingEntity target = findTargetWithinReach(player, heldItem);
               if (target != null && target != player) {
                  performMeleeAttackOnTarget(player, target);
                  damageGunAndAttachments(heldItem, player);
               } else {
                  HitResult hitResult = rayTraceBlocks(player, heldItem);
                  if (hitResult.getType() == Type.BLOCK) {
                     BlockHitResult blockHitResult = (BlockHitResult)hitResult;
                     BlockPos pos = blockHitResult.getBlockPos();
                     BlockState blockState = player.level().getBlockState(pos);
                     ClientboundLevelParticlesPacket particlePacket = getClientboundLevelParticlesPacket(blockHitResult, blockState);
                     player.connection.send(particlePacket);
                  }
               }
            }
         }
      }
   }

   @NotNull
   private static ClientboundLevelParticlesPacket getClientboundLevelParticlesPacket(BlockHitResult blockHitResult, BlockState blockState) {
      Vec3 hitVec = blockHitResult.getLocation();
      BlockParticleOption particleData = new BlockParticleOption(ParticleTypes.BLOCK, blockState);
      return new ClientboundLevelParticlesPacket(particleData, true, hitVec.x, hitVec.y, hitVec.z, 0.0F, 0.0F, 0.0F, 0.1F, 10);
   }

   private static HitResult rayTraceBlocks(Player player, ItemStack heldItem) {
      GunItem gunItem = (GunItem)heldItem.getItem();
      float reach = gunItem.getModifiedGun(heldItem).getGeneral().getMeleeReach();
      Vec3 eyePosition = player.getEyePosition(1.0F);
      Vec3 lookVector = player.getLookAngle();
      Vec3 reachVector = eyePosition.add(lookVector.scale((double)reach));
      return player.level().clip(new ClipContext(eyePosition, reachVector, Block.OUTLINE, Fluid.NONE, player));
   }

   private static LivingEntity findTargetWithinReach(Player player, ItemStack heldItem) {
      GunItem gunItem = (GunItem)heldItem.getItem();
      float reach = gunItem.getModifiedGun(heldItem).getGeneral().getMeleeReach();
      AABB boundingBox = player.getBoundingBox().inflate((double)reach, (double)reach, (double)reach);
      return player.level()
         .getEntitiesOfClass(LivingEntity.class, boundingBox, entity -> entity != player && entity.isAlive())
         .stream()
         .min(Comparator.comparingDouble(player::distanceToSqr))
         .orElse(null);
   }

   public static boolean isMeleeOnCooldown(Player player, ItemStack heldItem) {
      CompoundTag tag = NbtHelper.getOrCreateTag(heldItem);
      long currentTime = player.level().getGameTime();
      return tag.contains("MeleeCooldown") && currentTime < tag.getLong("MeleeCooldown");
   }

   public static void setMeleeCooldown(Player player, ItemStack heldItem, GunItem gunItem) {
      CompoundTag tag = NbtHelper.getOrCreateTag(heldItem);
      long currentTime = player.level().getGameTime();
      int cooldownTicks = gunItem.getModifiedGun(heldItem).getGeneral().getMeleeCooldownTicks();
      tag.putLong("MeleeCooldown", currentTime + (long)cooldownTicks);
      NbtHelper.setTag(heldItem, tag);
   }

   /**
    * The original area charge (HANDOFF section 82.34): one pass damages everything within {@code hitRadius},
    * with the damage scaled by the player's speed, and nothing else - no recoil, no execution, no end of the
    * charge. This is what ships; {@code singleTargetStab} replaces it rather than adding to it.
    */
   private static void areaSweep(ServerPlayer player, CompoundTag playerData, long currentTime) {
      ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
      if (!(heldItem.getItem() instanceof GunItem gunItem)) {
         return;
      }

      List<LivingEntity> targets = findTargetsInArea(player, (Double)charge().hitRadius.get());
      if (targets.isEmpty()) {
         return;
      }

      playerData.putLong(BANZAI_DAMAGE_COOLDOWN_TAG, currentTime + (long)charge().damageIntervalTicks.get());
      float attackDamage = meleeDamageOf(player, heldItem, gunItem, player) * getBanzaiDamageMultiplier(player, heldItem);
      attackDamage = (float)((double)Math.round((double)attackDamage * 100.0) / 100.0);
      DamageSource damageSource = player.serverLevel().damageSources().playerAttack(player);

      for (LivingEntity target : targets) {
         if (target == player || !target.isAlive()) {
            continue;
         }

         if (target.hurt(damageSource, attackDamage)) {
            spawnSuccessfulHitParticles(player, target);
            player.level()
               .playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 1.0F, 1.0F);
            applyKnockback(player, target, heldItem);
            applySpecialEnchantmentsFromBayonet(heldItem, target, player, gunItem);
            triggerBanzaiImpactIfNecessary(heldItem);
         }
      }
   }

   /**
    * One bayonet stab (HANDOFF section 82.33), the {@code singleTargetStab} mechanic. It damages the one enemy
    * it runs into with the gun's own melee damage - no speed scaling - throws the player back off it, and
    * executes a hostile mob outright if the stab already left it below the configured health.
    */
   private static void stabWithBayonet(ServerPlayer player, LivingEntity target) {
      ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
      if (!(heldItem.getItem() instanceof GunItem gunItem) || !target.isAlive()) {
         return;
      }

      float attackDamage = meleeDamageOf(player, heldItem, gunItem, target);

      DamageSource damageSource = player.serverLevel().damageSources().playerAttack(player);
      boolean executed = isExecutionTarget(target);
      boolean connected = executed
         // An execution is a kill, and it has to be the player's kill: hurt() with more damage than anything
         // has health keeps loot, experience and kill credit attached to the attacker, which kill() would not.
         ? target.hurt(damageSource, Float.MAX_VALUE)
         : target.hurt(damageSource, attackDamage);

      if (connected) {
         spawnSuccessfulHitParticles(player, target);
         player.level()
            .playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.0F);
         if (!executed) {
            applyKnockback(player, target, heldItem);
         }

         applySpecialEnchantmentsFromBayonet(heldItem, target, player, gunItem);
         triggerBanzaiImpactIfNecessary(heldItem);
      }

      if (isKnockPlayerBackOnHit() && connected) {
         // The thrust throws the player back, exactly as the wall impact does: charging something through
         // costs the player their momentum.
         knockPlayerBack(player);
      }

      if (isEndChargeOnHit() && connected) {
         stopBanzai(player);
      }
   }

   /**
    * The one enemy a charge can stab: the closest living entity in front of the player, within the search
    * radius and inside the thrust's reach.
    */
   private static LivingEntity findChargeTarget(ServerPlayer player) {
      double reach = (Double)charge().hitRadius.get();
      Vec3 look = player.getLookAngle();
      Vec3 eye = player.getEyePosition();
      LivingEntity closest = null;
      double closestDistance = Double.MAX_VALUE;

      for (LivingEntity candidate : findTargetsInArea(player, (Double)charge().damageRadius.get())) {
         if (!candidate.isAlive() || candidate.isSpectator()) {
            continue;
         }

         Vec3 towards = candidate.position().add(0.0, (double)candidate.getBbHeight() * 0.5, 0.0).subtract(eye);
         if (towards.dot(look) <= 0.0) {
            continue;   // behind the player: a charge stabs what it runs into, not what it left behind
         }

         double distance = towards.length();
         if (distance <= reach && distance < closestDistance) {
            closest = candidate;
            closestDistance = distance;
         }
      }

      return closest;
   }

   /** The gun's melee damage against this target, the same sum an ordinary bayonet stab deals. */
   private static float meleeDamageOf(ServerPlayer player, ItemStack heldItem, GunItem gunItem, LivingEntity target) {
      float attackAttribute = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
      float additionalDamage = GunModifierHelper.getAdditionalDamage(heldItem, true);
      float enchantmentDamage = getEnchantmentDamageFromBayonet(heldItem, target, gunItem);
      float meleeDamage = gunItem.getModifiedGun(heldItem).getGeneral().getMeleeDamage();
      return attackAttribute + additionalDamage + enchantmentDamage + meleeDamage;
   }

   /**
    * Whether this stab should execute: a hostile mob whose health is already below the configured threshold.
    * {@code Enemy} is vanilla's marker for hostile mobs, so this covers modded ones too.
    */
   private static boolean isExecutionTarget(LivingEntity target) {
      if (!(Boolean)charge().executeEnabled.get() || !(target instanceof Enemy)) {
         return false;
      }

      return (double)target.getHealth() < (Double)charge().executeHealthThreshold.get();
   }

   private static void performMeleeAttackOnTarget(ServerPlayer player, LivingEntity target) {
      ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
      if (heldItem.getItem() instanceof GunItem gunItem) {
         float attackDamage = meleeDamageOf(player, heldItem, gunItem, target);
         DamageSource damageSource = player.serverLevel().damageSources().playerAttack(player);
         LivingEntity raycastTarget = raycastForMeleeAttack(player, heldItem);
         if (raycastTarget != null && raycastTarget.hurt(damageSource, attackDamage)) {
            spawnSuccessfulHitParticles(player, raycastTarget);
            applyKnockback(player, raycastTarget, heldItem);
            applySpecialEnchantmentsFromBayonet(heldItem, raycastTarget, player, gunItem);
            triggerBanzaiImpactIfNecessary(heldItem);
         }
      }
   }

   private static void spawnSuccessfulHitParticles(ServerPlayer player, LivingEntity target) {
      Vec3 targetPos = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);
      ClientboundLevelParticlesPacket sweepPacket = new ClientboundLevelParticlesPacket(
         ParticleTypes.SWEEP_ATTACK, true, targetPos.x, targetPos.y, targetPos.z, 0.0F, 0.0F, 0.0F, 0.0F, 1
      );
      player.connection.send(sweepPacket);

      for (int i = 0; i < 5; i++) {
         double offsetX = (Math.random() - 0.5) * 0.5;
         double offsetY = (Math.random() - 0.5) * 0.5;
         double offsetZ = (Math.random() - 0.5) * 0.5;
         ClientboundLevelParticlesPacket critPacket = new ClientboundLevelParticlesPacket(
            ParticleTypes.CRIT, true, targetPos.x + offsetX, targetPos.y + offsetY, targetPos.z + offsetZ, 0.0F, 0.1F, 0.0F, 0.1F, 1
         );
         player.connection.send(critPacket);
      }
   }

   private static void triggerBanzaiImpactIfNecessary(ItemStack heldItem) {
      if (((GunItem)heldItem.getItem()).hasBayonet(heldItem)) {
         GunRenderingHandler.get().triggerBanzaiImpact();
      }
   }

   private static void applySpecialEnchantmentsFromBayonet(ItemStack gunStack, LivingEntity target, Player player, GunItem gunItem) {
      for (IAttachment.Type type : IAttachment.Type.values()) {
         ItemStack attachmentStack = gunItem.getAttachment(gunStack, type);
         if (attachmentStack.getItem() instanceof BayonetItem) {
            Map<Holder<Enchantment>, Integer> enchantments = ScEnchants.getEnchantments(attachmentStack);

            for (Entry<Holder<Enchantment>, Integer> entry : enchantments.entrySet()) {
               Holder<Enchantment> enchantment = entry.getKey();
               int level = entry.getValue();
               applyEnchantmentEffects(enchantment, level, target, player);
               if (ScEnchants.is(enchantment, ModEnchantments.CORRODED)) {
                  applyCorrodedEffects(player, target, level);
               }
            }
         }
      }
   }

   private static void applyCorrodedEffects(Player player, LivingEntity target, int level) {
      if (isBotEntity(target)) {
         spawnCorrodedParticles(player, target, level);
      } else if (player.level().getRandom().nextFloat() < 0.3F) {
         int poisonDuration = 60 + level * 20;
         target.addEffect(new MobEffectInstance(MobEffects.POISON, poisonDuration, 0));
      }
   }

   private static void spawnCorrodedParticles(Player player, LivingEntity target, int level) {
      if (player.level() instanceof ServerLevel serverLevel) {
         Random random = new Random();

         for (int i = 0; i < level * 5; i++) {
            double offsetX = (random.nextDouble() - 0.5) * (double)target.getBbWidth();
            double offsetY = random.nextDouble() * (double)target.getBbHeight();
            double offsetZ = (random.nextDouble() - 0.5) * (double)target.getBbWidth();
            ClientboundLevelParticlesPacket particlePacket = new ClientboundLevelParticlesPacket(
               ParticleTypes.ELECTRIC_SPARK, true, target.getX() + offsetX, target.getY() + offsetY, target.getZ() + offsetZ, 0.0F, 0.0F, 0.0F, 0.1F, 1
            );
            if (player instanceof ServerPlayer serverPlayer) {
               serverPlayer.connection.send(particlePacket);
            }
         }
      }
   }

   public static void performNormalMeleeAttack(ServerPlayer player) {
      performMeleeAttack(player);
   }

   public static void handleBanzaiMode(ServerPlayer player) {
      // The synced key is the per-player source of truth. The static isBanzai flag is global, so on a server
      // this ticker used to run for whichever player happened to own it, and cleared the wrong player's key.
      ItemStack startedWith = BANZAI_ACTIVE_ITEMS.get(player.getUUID());
      if (startedWith != null && isBanzaiCharging(player)) {
         ItemStack currentHeldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
         if (!ItemStack.matches(currentHeldItem, startedWith)) {
            stopBanzai(player);
         } else {
            CompoundTag playerData = player.getPersistentData();
            long currentTime = player.level().getGameTime();
            boolean inGracePeriod = playerData.contains(KNOCKBACK_GRACE_TAG) && currentTime < playerData.getLong(KNOCKBACK_GRACE_TAG);
            // The wall impact comes first: it is the charge's own reaction to running into something, and it
            // is exactly what kills the sprint flag. Checking the flag first is what made 0.5.5's grace
            // period unreachable (HANDOFF section 82.23). Switching it off in config removes the knockback and
            // its grace; running into a wall then simply stops the charge the ordinary way, when the player
            // stops moving.
            if (isWallImpactEnabled() && checkForWallCollision(player)) {
               knockPlayerBack(player);
               sendWallImpactParticles(player);
               triggerBanzaiImpactIfNecessary(currentHeldItem);
               playerData.putLong(KNOCKBACK_GRACE_TAG, currentTime + (long)charge().knockbackGraceTicks.get());
            } else if (!inGracePeriod && !keepCharging(player, playerData, currentTime)) {
               stopBanzai(player);
            } else if (!playerData.contains(BANZAI_DAMAGE_COOLDOWN_TAG) || currentTime >= playerData.getLong(BANZAI_DAMAGE_COOLDOWN_TAG)) {
               // Two mechanics share this charge (HANDOFF sections 82.33 and 82.34). The original area sweep
               // is what ships, and it stays the default; singleTargetStab switches to the one target thrust,
               // which deals the gun's own melee damage, throws the player back and can execute a weakened
               // hostile mob.
               if (isSingleTargetStab()) {
                  LivingEntity target = findChargeTarget(player);
                  if (target != null) {
                     playerData.putLong(BANZAI_DAMAGE_COOLDOWN_TAG, currentTime + (long)charge().damageIntervalTicks.get());
                     stabWithBayonet(player, target);
                  }
               } else {
                  areaSweep(player, playerData, currentTime);
               }
            }
         }
      }
   }

   /**
    * Whether a charge should continue (HANDOFF section 82.23).
    *
    * <p>Sprinting is what starts a charge and what scales its damage, but it must not be what ends one: the
    * flag is dropped by vanilla on every block collision, in water and when the food bar empties, and for
    * players who sprint by double-tapping W it only comes back on a fresh double tap. A charge is meant to
    * run into things, so treating a missing flag as "stop" ended charges at the first wall - and it also made
    * the knockback grace period below unreachable.</p>
    *
    * <p>So a charge continues while the player is still running forward: sprinting, or moving forward fast
    * enough. A lost flag is tolerated for {@code bayonetCharge.sprintLossToleranceTicks} ticks, which keeps a
    * wall bump or a water splash from ending a charge while still ending one for a player who genuinely can
    * no longer sprint.</p>
    */
   private static boolean keepCharging(ServerPlayer player, CompoundTag playerData, long currentTime) {
      if (!player.isSprinting() && horizontalForwardSpeed(player) <= charge().minimumForwardSpeed.get()) {
         return false;
      }

      if (player.isSprinting()) {
         playerData.remove(BANZAI_SPRINT_LOST_TAG);
         return true;
      }

      if (!playerData.contains(BANZAI_SPRINT_LOST_TAG)) {
         playerData.putLong(BANZAI_SPRINT_LOST_TAG, currentTime);
         return true;
      }

      return currentTime - playerData.getLong(BANZAI_SPRINT_LOST_TAG) <= (long)charge().sprintLossToleranceTicks.get();
   }

   /**
    * The player's speed along the horizontal direction they are looking, in blocks per tick. The look vector
    * is flattened first so that looking at the ground while charging does not shrink the projection.
    */
   private static double horizontalForwardSpeed(ServerPlayer player) {
      Vec3 look = player.getLookAngle();
      Vec3 motion = player.getDeltaMovement();
      double horizontalLook = Math.sqrt(look.x * look.x + look.z * look.z);
      if (horizontalLook < 1.0E-4) {
         return Math.sqrt(motion.x * motion.x + motion.z * motion.z);
      }

      return (look.x * motion.x + look.z * motion.z) / horizontalLook;
   }

   private static boolean checkForWallCollision(ServerPlayer player) {
      CompoundTag playerData = player.getPersistentData();
      long currentTime = player.level().getGameTime();
      if (playerData.contains(WALL_COLLISION_COOLDOWN_TAG) && currentTime < playerData.getLong(WALL_COLLISION_COOLDOWN_TAG)) {
         return false;
      } else {
         Vec3 eyePosition = player.getEyePosition(1.0F);
         Vec3 lookVector = player.getLookAngle();
         Vec3 playerMotion = player.getDeltaMovement();
         if (lookVector.dot(playerMotion) <= 0.0) {
            return false;
         } else {
            double[] heightOffsets = new double[]{0.0, 0.5, -0.5};

            for (double heightOffset : heightOffsets) {
               Vec3 checkPosition = eyePosition.add(0.0, heightOffset, 0.0);

               for (double angle : wallCheckAngles()) {
                  Vec3 rotatedVector = rotateVector(lookVector, angle);
                  Vec3 reachVector = checkPosition.add(rotatedVector.scale(charge().wallCheckDistance.get()));
                  BlockHitResult hitResult = player.level().clip(new ClipContext(checkPosition, reachVector, Block.COLLIDER, Fluid.NONE, player));
                  if (hitResult.getType() == Type.BLOCK) {
                     playerData.putLong(WALL_COLLISION_COOLDOWN_TAG, currentTime + (long)charge().wallImpactCooldownTicks.get());
                     return true;
                  }
               }
            }

            return false;
         }
      }
   }

   private static Vec3 rotateVector(Vec3 lookVector, double angle) {
      double angleRadians = Math.toRadians(angle);
      double x = lookVector.x * Math.cos(angleRadians) - lookVector.z * Math.sin(angleRadians);
      double z = lookVector.x * Math.sin(angleRadians) + lookVector.z * Math.cos(angleRadians);
      return new Vec3(x, lookVector.y, z);
   }

   private static void knockPlayerBack(ServerPlayer player) {
      Vec3 knockbackDirection = player.getLookAngle().scale(-0.5);
      player.push(knockbackDirection.x, 0.3, knockbackDirection.z);
      player.hurtMarked = true;
   }

   private static void sendWallImpactParticles(ServerPlayer player) {
      Vec3 eyePosition = player.getEyePosition(1.0F);
      Vec3 lookVector = player.getLookAngle();
      Vec3 reachVector = eyePosition.add(lookVector.scale(charge().wallCheckDistance.get()));
      ClipContext context = new ClipContext(eyePosition, reachVector, Block.COLLIDER, Fluid.NONE, player);
      BlockHitResult hitResult = player.level().clip(context);
      if (hitResult.getType() == Type.BLOCK) {
         BlockPos pos = hitResult.getBlockPos();
         BlockState blockState = player.level().getBlockState(pos);
         ClientboundLevelParticlesPacket particlePacket = getClientboundLevelParticlesPacket(hitResult, blockState);
         player.connection.send(particlePacket);
         player.level()
            .playSound(
               null,
               hitResult.getLocation().x,
               hitResult.getLocation().y,
               hitResult.getLocation().z,
               SoundEvents.SHIELD_BLOCK,
               SoundSource.PLAYERS,
               1.0F,
               1.0F
            );
      }
   }

   private static float getBanzaiDamageMultiplier(ServerPlayer player, ItemStack heldItem) {
      double speed = player.getDeltaMovement().length();
      int banzaiLevel = ((GunItem)heldItem.getItem()).getBayonetBanzaiLevel(heldItem);
      float scalingFactor = 0.0F;
      if (banzaiLevel > 0 && banzaiLevel <= 3) {
         scalingFactor = banzaiDamageFactor(banzaiLevel);
      }

      return 1.0F + (float)speed * scalingFactor;
   }

   private static float getEnchantmentDamageFromBayonet(ItemStack gunStack, LivingEntity target, GunItem gunItem) {
      float enchantmentDamage = 0.0F;

      for (IAttachment.Type type : IAttachment.Type.values()) {
         ItemStack attachmentStack = gunItem.getAttachment(gunStack, type);
         if (attachmentStack.getItem() instanceof BayonetItem) {
            Map<Holder<Enchantment>, Integer> enchantments = ScEnchants.getEnchantments(attachmentStack);

            for (Entry<Holder<Enchantment>, Integer> entry : enchantments.entrySet()) {
               Holder<Enchantment> enchantment = entry.getKey();
               int level = entry.getValue();
               if (isDamageEnchantment(enchantment)) {
                  float damageBonus = getDamageEnchantmentBonus(enchantment, level, top.ribs.scguns.util.MobType.of(target));
                  enchantmentDamage += damageBonus * 0.7F;
               } else if (ScEnchants.is(enchantment, ModEnchantments.CORRODED) && isBotEntity(target)) {
                  float corrodedBonus = CorrodedEnchantment.getBotDamageBonus(level);
                  enchantmentDamage += corrodedBonus * 0.7F;
               }
            }
         }
      }

      return enchantmentDamage;
   }

   private static boolean isBotEntity(LivingEntity entity) {
      return entity.getType().is(ModTags.Entities.BOT);
   }

   /**
    * 1.21 turned Sharpness/Smite/Bane of Arthropods into datapack entries, so the
    * {@code DamageEnchantment} class (and its {@code getDamageBonus(int, MobType)})
    * no longer exists. These two helpers reproduce the 1.20.1
    * {@code DamageEnchantment#getDamageBonus} formula for the three vanilla damage
    * enchantments so the bayonet bonus is unchanged.
    */
   private static boolean isDamageEnchantment(Holder<Enchantment> enchantment) {
      return ScEnchants.is(enchantment, Enchantments.SHARPNESS)
         || ScEnchants.is(enchantment, Enchantments.SMITE)
         || ScEnchants.is(enchantment, Enchantments.BANE_OF_ARTHROPODS);
   }

   private static float getDamageEnchantmentBonus(Holder<Enchantment> enchantment, int level, MobType mobType) {
      if (ScEnchants.is(enchantment, Enchantments.SHARPNESS)) {
         return 1.0F + (float)Math.max(0, level - 1) * 0.5F;
      }

      if (ScEnchants.is(enchantment, Enchantments.SMITE) && mobType == MobType.UNDEAD) {
         return (float)level * 2.5F;
      }

      if (ScEnchants.is(enchantment, Enchantments.BANE_OF_ARTHROPODS) && mobType == MobType.ARTHROPOD) {
         return (float)level * 2.5F;
      }

      return 0.0F;
   }

   private static void applyEnchantmentEffects(Holder<Enchantment> enchantment, int level, LivingEntity target, Player player) {
      if (ScEnchants.is(enchantment, Enchantments.FIRE_ASPECT)) {
         target.igniteForSeconds(level * 4);
         DistHelper.runWhenOn(Dist.CLIENT, () -> ClientMeleeAttackHandler.spawnParticleEffect(player, target, ParticleTypes.FLAME));
      } else if (ScEnchants.is(enchantment, Enchantments.KNOCKBACK)) {
         Vec3 direction = target.position().subtract(player.position()).normalize();
         target.knockback((double)((float)level * 0.5F), -direction.x(), -direction.z());
      } else if (ScEnchants.is(enchantment, Enchantments.SMITE) && top.ribs.scguns.util.MobType.of(target) == MobType.UNDEAD) {
         spawnEnchantmentHitParticles(player, target);
      } else if (ScEnchants.is(enchantment, Enchantments.BANE_OF_ARTHROPODS) && top.ribs.scguns.util.MobType.of(target) == MobType.ARTHROPOD) {
         spawnEnchantmentHitParticles(player, target);
      } else if (ScEnchants.is(enchantment, Enchantments.SHARPNESS)) {
         spawnEnchantmentHitParticles(player, target);
      }
   }

   private static void spawnEnchantmentHitParticles(Player player, LivingEntity target) {
      if (player instanceof ServerPlayer serverPlayer) {
         ClientboundLevelParticlesPacket particlePacket = new ClientboundLevelParticlesPacket(
            ParticleTypes.ENCHANTED_HIT,
            true,
            target.getX(),
            target.getY() + (double)target.getBbHeight() * 0.5,
            target.getZ(),
            target.getBbWidth() * 0.5F,
            target.getBbHeight() * 0.25F,
            target.getBbWidth() * 0.5F,
            0.02F,
            8
         );
         serverPlayer.connection.send(particlePacket);
      }
   }

   private static void applyKnockback(Player player, LivingEntity target, ItemStack stack) {
      int knockbackLevel = ScEnchants.level(stack, Enchantments.KNOCKBACK);
      Vec3 direction = target.position().subtract(player.position()).normalize();
      target.knockback((double)(0.4F + (float)knockbackLevel * 0.5F), -direction.x(), -direction.z());
   }

   private static LivingEntity raycastForMeleeAttack(Player player, ItemStack heldItem) {
      GunItem gunItem = (GunItem)heldItem.getItem();
      float reach = gunItem.getModifiedGun(heldItem).getGeneral().getMeleeReach();
      Vec3 startVec = player.getEyePosition(1.0F);
      Vec3 lookVec = player.getLookAngle();
      Vec3 endVec = startVec.add(lookVec.scale((double)reach));
      AABB boundingBox = new AABB(startVec, endVec);
      return player.level()
         .getEntitiesOfClass(LivingEntity.class, boundingBox, entity -> entity != player && entity.isAlive())
         .stream()
         .min(Comparator.comparingDouble(player::distanceToSqr))
         .orElse(null);
   }

   private static List<LivingEntity> findTargetsInArea(Player player, double radius) {
      Vec3 position = player.position();
      AABB boundingBox = new AABB(position.subtract(radius, radius, radius), position.add(radius, radius, radius));
      return player.level().getEntitiesOfClass(LivingEntity.class, boundingBox, entity -> entity != player && entity.isAlive());
   }

   private static void damageGunAndAttachments(ItemStack stack, Player player) {
      Level level = player.level();
      GunEventBus.damageGun(stack, level, player);
      GunEventBus.damageAttachments(stack, level, player);
   }
}
