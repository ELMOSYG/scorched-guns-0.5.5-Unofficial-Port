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
   private static final float[] BANZAI_SCALING_FACTORS = new float[]{3.0F, 5.5F, 7.0F};
   private static final String WALL_COLLISION_COOLDOWN_TAG = "WallCollisionCooldown";
   private static final String MELEE_COOLDOWN_TAG = "MeleeCooldown";
   private static final int KNOCKBACK_GRACE_PERIOD_TICKS = 5;
   private static final String KNOCKBACK_GRACE_TAG = "KnockbackGracePeriod";
   private static boolean isBanzai = false;
   private static final int WALL_COLLISION_COOLDOWN_TICKS = 20;
   private static final double WALL_CHECK_DISTANCE = 1.0;
   private static final double[] WALL_CHECK_ANGLES = new double[]{0.0, 10.0, -10.0, 20.0, -20.0, 30.0, -30.0};
   private static final String BANZAI_DAMAGE_COOLDOWN_TAG = "BanzaiDamageCooldown";
   private static final int BANZAI_DAMAGE_COOLDOWN_TICKS = 25;
   private static final double BANZAI_AOE_RADIUS = 1.5;
   private static ItemStack banzaiActiveItem = ItemStack.EMPTY;

   public MeleeAttackHandler() {
      super();
   }

   public static boolean isBanzaiActive() {
      return isBanzai;
   }

   public static void startBanzai(ServerPlayer player) {
      ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
      if (heldItem.getItem() instanceof GunItem gunItem) {
         if (!gunItem.hasBayonet(heldItem)) {
            performMeleeAttack(player);
         } else {
            isBanzai = true;
            banzaiActiveItem = heldItem.copy();
         }
      }
   }

   public static void stopBanzai() {
      isBanzai = false;
      banzaiActiveItem = ItemStack.EMPTY;
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
                  performMeleeAttackOnTarget(player, target, false);
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

   private static void performMeleeAttackOnTarget(ServerPlayer player, LivingEntity target, boolean isBanzaiAttack) {
      ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
      if (heldItem.getItem() instanceof GunItem gunItem) {
         float var15 = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
         float additionalDamage = GunModifierHelper.getAdditionalDamage(heldItem, true);
         float enchantmentDamage = getEnchantmentDamageFromBayonet(heldItem, target, gunItem);
         Gun modifiedGun = gunItem.getModifiedGun(heldItem);
         float meleeDamage = modifiedGun.getGeneral().getMeleeDamage();
         float attackDamage = var15 + additionalDamage + enchantmentDamage + meleeDamage;
         if (isBanzaiAttack) {
            float speedDamageMultiplier = getBanzaiDamageMultiplier(player, heldItem);
            attackDamage *= speedDamageMultiplier;
            attackDamage = (float)((double)Math.round((double)attackDamage * 100.0) / 100.0);
         }

         DamageSource damageSource = player.serverLevel().damageSources().playerAttack(player);
         if (isBanzaiAttack) {
            for (LivingEntity aoeTarget : findTargetsInArea(player, 2.5)) {
               if (aoeTarget.hurt(damageSource, attackDamage)) {
                  spawnSuccessfulHitParticles(player, aoeTarget);
                  player.level()
                     .playSound(null, aoeTarget.getX(), aoeTarget.getY(), aoeTarget.getZ(), SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                  applyKnockback(player, aoeTarget, heldItem);
                  applySpecialEnchantmentsFromBayonet(heldItem, aoeTarget, player, gunItem);
                  triggerBanzaiImpactIfNecessary(heldItem);
               }
            }
         } else {
            LivingEntity raycastTarget = raycastForMeleeAttack(player, heldItem);
            if (raycastTarget != null && raycastTarget.hurt(damageSource, attackDamage)) {
               spawnSuccessfulHitParticles(player, raycastTarget);
               applyKnockback(player, raycastTarget, heldItem);
               applySpecialEnchantmentsFromBayonet(heldItem, raycastTarget, player, gunItem);
               triggerBanzaiImpactIfNecessary(heldItem);
            }
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
      if (isBanzai) {
         ItemStack currentHeldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
         if (!ItemStack.matches(currentHeldItem, banzaiActiveItem)) {
            stopBanzai();
         } else {
            CompoundTag playerData = player.getPersistentData();
            long currentTime = player.level().getGameTime();
            boolean inGracePeriod = playerData.contains("KnockbackGracePeriod") && currentTime < playerData.getLong("KnockbackGracePeriod");
            if (!player.isSprinting() && !inGracePeriod) {
               stopBanzai();
            } else if (checkForWallCollision(player)) {
               knockPlayerBack(player);
               sendWallImpactParticles(player);
               triggerBanzaiImpactIfNecessary(currentHeldItem);
               playerData.putLong("KnockbackGracePeriod", currentTime + 5L);
            } else if (!playerData.contains("BanzaiDamageCooldown") || currentTime >= playerData.getLong("BanzaiDamageCooldown")) {
               List<LivingEntity> targets = findTargetsInArea(player, 1.5);
               if (!targets.isEmpty()) {
                  playerData.putLong("BanzaiDamageCooldown", currentTime + 25L);

                  for (LivingEntity target : targets) {
                     if (target != player) {
                        performMeleeAttackOnTarget(player, target, true);
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean checkForWallCollision(ServerPlayer player) {
      CompoundTag playerData = player.getPersistentData();
      long currentTime = player.level().getGameTime();
      if (playerData.contains("WallCollisionCooldown") && currentTime < playerData.getLong("WallCollisionCooldown")) {
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

               for (double angle : WALL_CHECK_ANGLES) {
                  Vec3 rotatedVector = rotateVector(lookVector, angle);
                  Vec3 reachVector = checkPosition.add(rotatedVector.scale(1.0));
                  BlockHitResult hitResult = player.level().clip(new ClipContext(checkPosition, reachVector, Block.COLLIDER, Fluid.NONE, player));
                  if (hitResult.getType() == Type.BLOCK) {
                     playerData.putLong("WallCollisionCooldown", currentTime + 20L);
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
      Vec3 reachVector = eyePosition.add(lookVector.scale(1.0));
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
         scalingFactor = BANZAI_SCALING_FACTORS[banzaiLevel - 1];
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
