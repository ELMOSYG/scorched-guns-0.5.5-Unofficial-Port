package top.ribs.scguns.network.message;


import top.ribs.scguns.util.ScEnchants;
import com.mrcrayfish.framework.api.network.MessageContext;
import java.util.Random;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import top.ribs.scguns.util.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import top.ribs.scguns.common.GripType;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.enchantment.CorrodedEnchantment;
import top.ribs.scguns.init.ModEnchantments;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.item.BayonetItem;
import top.ribs.scguns.item.GunItem;

public class C2SMessageOffhandMelee {
   private int targetId;
   private float x;
   private float y;
   private float z;
   private static final float OFFHAND_DAMAGE_MULTIPLIER = 0.65F;
   private static final float OFFHAND_COOLDOWN_MULTIPLIER = 1.25F;

   public C2SMessageOffhandMelee() {
      super();
   }

   public C2SMessageOffhandMelee(int targetId, float x, float y, float z) {
      super();
      this.targetId = targetId;
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public void encode(C2SMessageOffhandMelee message, FriendlyByteBuf buffer) {
      buffer.writeInt(message.targetId);
      buffer.writeFloat(message.x);
      buffer.writeFloat(message.y);
      buffer.writeFloat(message.z);
   }

   public C2SMessageOffhandMelee decode(FriendlyByteBuf buffer) {
      return new C2SMessageOffhandMelee(buffer.readInt(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
   }

   public void handle(C2SMessageOffhandMelee message, MessageContext context) {
      context.execute(() -> {
         ServerPlayer player = context.getPlayer().map(p -> (ServerPlayer) p).orElse(null);
         if (player != null && !player.isSpectator()) {
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            if (mainHand.getItem() instanceof GunItem gunItem) {
               Gun gun = gunItem.getModifiedGun(mainHand);
               GripType gripType = gun.getGeneral().getGripType(mainHand);
               if (gripType == GripType.ONE_HANDED) {
                  if (offHand.getItem() instanceof SwordItem || offHand.getItem() instanceof BayonetItem) {
                     float attackStrength = player.getAttackStrengthScale(0.5F);
                     if (!(attackStrength < 0.95F)) {
                        player.swing(InteractionHand.OFF_HAND, true);
                        float attackSpeed = this.getWeaponAttackSpeed(offHand);
                        player.resetAttackStrengthTicker();
                        float cooldownTicks = 20.0F / attackSpeed * 1.25F;
                        player.getCooldowns().addCooldown(offHand.getItem(), (int)cooldownTicks);
                        if (message.targetId != -1) {
                           Entity target = player.level().getEntity(message.targetId);
                           if (target != null) {
                              double distance = (double)player.distanceTo(target);
                              if (!(distance > 6.0)) {
                                 this.performOffhandMeleeAttack(player, target, offHand, attackStrength);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      });
      context.setHandled(true);
   }

   private void performOffhandMeleeAttack(ServerPlayer player, Entity target, ItemStack weapon, float attackStrength) {
      float baseDamage = this.getWeaponDamage(weapon) * 0.65F;
      if (target instanceof LivingEntity livingTarget) {
         baseDamage += ScEnchants.getDamageBonus(weapon, top.ribs.scguns.util.MobType.of(livingTarget));
         int corrodedLevel = ScEnchants.level(weapon, ModEnchantments.CORRODED);
         if (corrodedLevel > 0 && this.isBotEntity(livingTarget)) {
            baseDamage += CorrodedEnchantment.getBotDamageBonus(corrodedLevel);
         }
      }

      float damage = baseDamage * (0.2F + attackStrength * attackStrength * 0.8F);
      boolean isCritical = player.fallDistance > 0.0F && !player.onGround() && !player.isInWater() && !player.hasEffect(MobEffects.BLINDNESS) && !player.isPassenger();
      if (isCritical) {
         damage *= 1.3F;
      }

      DamageSource damageSource = player.damageSources().playerAttack(player);
      boolean damaged = target.hurt(damageSource, damage);
      if (damaged) {
         if (target instanceof LivingEntity livingTargetx) {
            float knockback = 0.25F;
            int knockbackLevel = ScEnchants.getKnockbackBonus(player);
            if (knockbackLevel > 0) {
               knockback += (float)knockbackLevel * 0.4F;
            }

            livingTargetx.knockback(
               (double)knockback, Math.sin((double)player.getYRot() * Math.PI / 180.0), -Math.cos((double)player.getYRot() * Math.PI / 180.0)
            );
            int fireAspectLevel = ScEnchants.getFireAspect(player);
            if (fireAspectLevel > 0) {
               livingTargetx.igniteForSeconds(fireAspectLevel * 4);
            }

            float sweepingLevel = ScEnchants.getSweepingDamageRatio(player);
            if (sweepingLevel > 0.0F && weapon.getItem() instanceof SwordItem) {
               this.applySweepingDamage(player, target, weapon, sweepingLevel, damage * 0.25F);
            }

            int corrodedLevel = ScEnchants.level(weapon, ModEnchantments.CORRODED);
            if (corrodedLevel > 0) {
               this.applyCorrodedEffects(player, livingTargetx, weapon, corrodedLevel);
            }

            this.spawnEnchantmentParticles(player, weapon, livingTargetx);
         }

         if (weapon.isDamageableItem()) {
            weapon.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(InteractionHand.OFF_HAND));
         }

         SoundEvent soundEvent = isCritical ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_STRONG;
         player.level().playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, SoundSource.PLAYERS, 0.8F, 1.1F);
         if (target instanceof LivingEntity) {
            ((ServerLevel)player.level())
               .sendParticles(
                  ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + (double)target.getBbHeight() * 0.5, target.getZ(), 1, 0.0, 0.0, 0.0, 0.0
               );
         }

         if (isCritical && target instanceof LivingEntity) {
            ((ServerLevel)player.level())
               .sendParticles(
                  ParticleTypes.CRIT, target.getX(), target.getY() + (double)target.getBbHeight() * 0.5, target.getZ(), 6, 0.15, 0.15, 0.15, 0.0
               );
         }
      }
   }

   private void applyCorrodedEffects(ServerPlayer player, LivingEntity target, ItemStack weapon, int corrodedLevel) {
      if (this.isBotEntity(target)) {
         this.spawnCorrodedParticles(target, corrodedLevel);
      } else if (player.level().getRandom().nextFloat() < 0.3F) {
         int poisonDuration = 60 + corrodedLevel * 20;
         target.addEffect(new MobEffectInstance(MobEffects.POISON, poisonDuration, 0));
      }
   }

   private void spawnCorrodedParticles(LivingEntity entity, int level) {
      if (entity.level() instanceof ServerLevel serverLevel) {
         Random random = new Random();

         for (int i = 0; i < level * 5; i++) {
            double offsetX = (random.nextDouble() - 0.5) * (double)entity.getBbWidth();
            double offsetY = random.nextDouble() * (double)entity.getBbHeight();
            double offsetZ = (random.nextDouble() - 0.5) * (double)entity.getBbWidth();
            serverLevel.sendParticles(
               ParticleTypes.ELECTRIC_SPARK, entity.getX() + offsetX, entity.getY() + offsetY, entity.getZ() + offsetZ, 1, 0.0, 0.0, 0.0, 0.1
            );
         }
      }
   }

   private boolean isBotEntity(LivingEntity entity) {
      return entity.getType().is(ModTags.Entities.BOT);
   }

   private void spawnEnchantmentParticles(ServerPlayer player, ItemStack weapon, LivingEntity target) {
      if (player.level() instanceof ServerLevel serverLevel) {
         int var10 = ScEnchants.level(weapon, Enchantments.SHARPNESS);
         int smiteLevel = ScEnchants.level(weapon, Enchantments.SMITE);
         int baneLevel = ScEnchants.level(weapon, Enchantments.BANE_OF_ARTHROPODS);
         boolean hasRelevantEnchantment = false;
         if (smiteLevel > 0 && top.ribs.scguns.util.MobType.of(target) == MobType.UNDEAD) {
            hasRelevantEnchantment = true;
         } else if (baneLevel > 0 && top.ribs.scguns.util.MobType.of(target) == MobType.ARTHROPOD) {
            hasRelevantEnchantment = true;
         } else if (var10 > 0) {
            hasRelevantEnchantment = true;
         }

         if (hasRelevantEnchantment) {
            serverLevel.sendParticles(
               ParticleTypes.ENCHANTED_HIT,
               target.getX(),
               target.getY() + (double)target.getBbHeight() * 0.5,
               target.getZ(),
               8,
               (double)target.getBbWidth() * 0.5,
               (double)target.getBbHeight() * 0.25,
               (double)target.getBbWidth() * 0.5,
               0.02
            );
         }

         int fireAspectLevel = ScEnchants.level(weapon, Enchantments.FIRE_ASPECT);
         if (fireAspectLevel > 0) {
            serverLevel.sendParticles(
               ParticleTypes.FLAME,
               target.getX(),
               target.getY() + (double)target.getBbHeight() * 0.5,
               target.getZ(),
               fireAspectLevel * 2,
               (double)target.getBbWidth() * 0.3,
               (double)target.getBbHeight() * 0.2,
               (double)target.getBbWidth() * 0.3,
               0.05
            );
         }
      }
   }

   private void applySweepingDamage(ServerPlayer player, Entity target, ItemStack weapon, float sweepingLevel, float sweepDamage) {
      double range = 1.0 + (double)sweepingLevel;

      for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(range, 0.25, range))) {
         if (entity != player && entity != target && !player.isAlliedTo(entity)) {
            if (entity instanceof ArmorStand) {
               ArmorStand armorStand = (ArmorStand)entity;
               if (armorStand.isMarker()) {
                  continue;
               }
            }

            if (player.distanceToSqr(entity) < range * range) {
               entity.knockback(0.4F, Math.sin((double)player.getYRot() * Math.PI / 180.0), -Math.cos((double)player.getYRot() * Math.PI / 180.0));
               entity.hurt(player.damageSources().playerAttack(player), sweepDamage);
            }
         }
      }
   }

   private float getWeaponAttackSpeed(ItemStack weapon) {
      ItemAttributeModifiers attributeModifiers = weapon.getAttributeModifiers();
      float attackSpeed = 4.0F;

      for (ItemAttributeModifiers.Entry entry : attributeModifiers.modifiers()) {
         if (entry.attribute().is(Attributes.ATTACK_SPEED) && entry.slot().test(EquipmentSlot.MAINHAND)) {
            attackSpeed += (float)entry.modifier().amount();
         }
      }

      return Math.max(0.1F, attackSpeed);
   }

   private float getWeaponDamage(ItemStack weapon) {
      ItemAttributeModifiers attributeModifiers = weapon.getAttributeModifiers();
      float damage = 1.0F;

      for (ItemAttributeModifiers.Entry entry : attributeModifiers.modifiers()) {
         if (entry.attribute().is(Attributes.ATTACK_DAMAGE) && entry.slot().test(EquipmentSlot.MAINHAND)) {
            damage += (float)entry.modifier().amount();
         }
      }

      return damage;
   }
}
