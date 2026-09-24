package top.ribs.scguns.util;



import top.ribs.scguns.util.ScEnchants;
import net.minecraft.core.Holder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.cache.HotBarrelCache;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.common.ReloadType;
import top.ribs.scguns.init.ModEffects;
import top.ribs.scguns.init.ModEnchantments;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.particles.TrailData;

public class GunEnchantmentHelper {
   private static final Map<Holder<MobEffect>, Integer> ELEMENTAL_EFFECTS = new HashMap<>();

   public GunEnchantmentHelper() {
      super();
   }

   public static float getChargeDamage(ItemStack weapon, float damage, float chargeProgress) {
      if (weapon.getItem() instanceof GunItem gunItem) {
         Gun modifiedGun = gunItem.getModifiedGun(weapon);
         if (modifiedGun.getGeneral().getFireTimer() <= 0) {
            return damage;
         } else {
            float minDamagePercent = 0.85F;
            float fullChargeThreshold = 0.95F;
            float effectiveCharge = chargeProgress >= fullChargeThreshold ? 1.0F : chargeProgress;
            float normalizedCharge = effectiveCharge < fullChargeThreshold ? effectiveCharge / fullChargeThreshold : 1.0F;
            float damageReductionFactor = minDamagePercent + (float)(Math.sqrt((double)normalizedCharge) * (double)(1.0F - minDamagePercent));
            return damage * damageReductionFactor;
         }
      } else {
         return damage;
      }
   }

   public static int getRealReloadSpeed(ItemStack weapon) {
      Gun modifiedGun = ((GunItem)weapon.getItem()).getModifiedGun(weapon);
      return modifiedGun.getReloads().getReloadType() == ReloadType.MAG_FED ? getMagReloadSpeed(weapon) : getReloadInterval(weapon);
   }

   public static int getReloadInterval(ItemStack weapon) {
      Gun modifiedGun = ((GunItem)weapon.getItem()).getModifiedGun(weapon);
      ReloadType reloadType = modifiedGun.getReloads().getReloadType();
      int level = ScEnchants.level(weapon, ModEnchantments.QUICK_HANDS);
      double decreaseFactor = 1.0 - 0.25 * (double)level;
      if (reloadType == ReloadType.MANUAL) {
         int bulletReloadTime = modifiedGun.getReloads().getReloadTimer();
         double interval = (double)bulletReloadTime * decreaseFactor;
         interval = GunModifierHelper.getModifiedReloadSpeed(weapon, interval);
         return Math.max((int)Math.round(interval), 1);
      } else if (reloadType == ReloadType.SINGLE_ITEM) {
         int bulletReloadTime = modifiedGun.getReloads().getReloadTimer();
         double interval = (double)bulletReloadTime * decreaseFactor;
         interval = GunModifierHelper.getModifiedReloadSpeed(weapon, interval);
         return Math.max((int)Math.round(interval), 1);
      } else {
         int baseInterval = 10;
         double interval = (double)baseInterval * decreaseFactor;
         interval = GunModifierHelper.getModifiedReloadSpeed(weapon, interval);
         return Math.max((int)Math.round(interval), 1);
      }
   }

   public static int getMagReloadSpeed(ItemStack weapon) {
      Gun modifiedGun = ((GunItem)weapon.getItem()).getModifiedGun(weapon);
      int baseSpeed = modifiedGun.getReloads().getReloadTimer();
      int level = ScEnchants.level(weapon, ModEnchantments.QUICK_HANDS);
      double decreaseFactor = 1.0 - 0.25 * (double)level;
      double speed = (double)baseSpeed * decreaseFactor;
      speed = GunModifierHelper.getModifiedReloadSpeed(weapon, speed);
      return Math.max((int)Math.round(speed), 4);
   }

   public static double getAimDownSightSpeed(ItemStack weapon) {
      int level = ScEnchants.level(weapon, ModEnchantments.LIGHTWEIGHT);
      return level > 0 ? 1.2 : 1.0;
   }

   public static double getProjectileSpeedModifier(ItemStack weapon) {
      int acceleratorLevel = ScEnchants.level(weapon, ModEnchantments.ACCELERATOR);
      int heavyShotLevel = ScEnchants.level(weapon, ModEnchantments.HEAVY_SHOT);
      double speedModifier = 1.0;
      if (acceleratorLevel > 0) {
         speedModifier += 0.25 * (double)acceleratorLevel;
      }

      if (heavyShotLevel > 0) {
         speedModifier -= 0.1 * (double)heavyShotLevel;
      }

      return Mth.clamp(speedModifier, 0.1, 5.0);
   }

   public static int getRate(ItemStack weapon, Gun modifiedGun) {
      int baseRate = modifiedGun.getGeneral().getRate();
      int triggerFingerLevel = ScEnchants.level(weapon, ModEnchantments.TRIGGER_FINGER);
      int heavyShotLevel = ScEnchants.level(weapon, ModEnchantments.HEAVY_SHOT);
      int puncturingLevel = ScEnchants.level(weapon, ModEnchantments.PUNCTURING);
      float rateModifier = getRateModifier(triggerFingerLevel, heavyShotLevel, puncturingLevel);
      int modifiedRate = Math.round((float)baseRate * rateModifier);
      modifiedRate = GunModifierHelper.getModifiedRate(weapon, modifiedRate);
      return Math.max(modifiedRate, 1);
   }

   public static float getRecoilModifier(ItemStack weapon) {
      int heavyShotLevel = ScEnchants.level(weapon, ModEnchantments.HEAVY_SHOT);
      int puncturingLevel = ScEnchants.level(weapon, ModEnchantments.PUNCTURING);
      float modifier = 1.0F;
      modifier += 0.25F * (float)heavyShotLevel;
      return modifier + 0.1F * (float)puncturingLevel;
   }

   public static float getRecoilModifier(Player player, ItemStack weapon) {
      float baseModifier = getRecoilModifier(weapon);
      if (player != null) {
         baseModifier = getHotBarrelRecoil(player, weapon, baseModifier);
      }

      return baseModifier;
   }

   public static float getKickModifier(ItemStack weapon) {
      int heavyShotLevel = ScEnchants.level(weapon, ModEnchantments.HEAVY_SHOT);
      return 1.0F + 0.05F * (float)heavyShotLevel;
   }

   public static float getKickModifier(Player player, ItemStack weapon) {
      float baseModifier = getKickModifier(weapon);
      if (player != null) {
         int hotBarrelLevel = HotBarrelCache.getHotBarrelLevel(player, weapon);
         float kickIncreaseFactor = 1.0F + (float)hotBarrelLevel / 100.0F * 0.5F;
         baseModifier *= kickIncreaseFactor;
      }

      return baseModifier;
   }

   private static float getRateModifier(int triggerFingerLevel, int heavyShotLevel, int puncturingLevel) {
      float heavyShotModifier = 1.0F + 0.15F * (float)heavyShotLevel;
      float puncturingModifier = 1.0F + 0.06F * (float)puncturingLevel;
      float triggerFingerModifier = 1.0F - 0.12F * (float)triggerFingerLevel;
      float combinedModifier = heavyShotModifier * puncturingModifier * triggerFingerModifier;
      return Mth.clamp(combinedModifier, 0.5F, 2.0F);
   }

   public static float getHeavyShotDamage(ItemStack weapon, float damage) {
      int level = ScEnchants.level(weapon, ModEnchantments.HEAVY_SHOT);
      if (level > 0) {
         damage += damage * 0.125F * (float)level;
      }

      return damage;
   }

   public static float getHeavyShotKnockback(ItemStack weapon, float baseKnockback) {
      int level = ScEnchants.level(weapon, ModEnchantments.HEAVY_SHOT);
      return level > 0 ? baseKnockback + 0.2F * (float)level : baseKnockback;
   }

   public static float getAcceleratorDamage(ItemStack weapon, float damage) {
      int acceleratorLevel = ScEnchants.level(weapon, ModEnchantments.ACCELERATOR);
      if (acceleratorLevel > 0) {
         damage += damage * 0.06F * (float)acceleratorLevel;
      }

      return damage;
   }

   public static float getHotBarrelDamage(Player player, ItemStack weapon, float baseDamage) {
      int hotBarrelLevel = HotBarrelCache.getHotBarrelLevel(player, weapon);
      float damageBoost = (float)hotBarrelLevel / 100.0F * 0.6F;
      return baseDamage + baseDamage * damageBoost;
   }

   public static float getHotBarrelRecoil(Player player, ItemStack weapon, float baseRecoil) {
      int hotBarrelLevel = HotBarrelCache.getHotBarrelLevel(player, weapon);
      float recoilIncreaseFactor = 1.0F + (float)hotBarrelLevel / 100.0F * 0.75F;
      return baseRecoil * recoilIncreaseFactor;
   }

   public static float getHotBarrelSpread(Player player, ItemStack weapon, float baseSpread) {
      int hotBarrelLevel = HotBarrelCache.getHotBarrelLevel(player, weapon);
      float spreadIncrease = (float)hotBarrelLevel / 100.0F * 1.5F;
      return baseSpread + baseSpread * spreadIncrease;
   }

   public static boolean shouldSetOnFire(Player player, ItemStack weapon) {
      int hotBarrelLevel = HotBarrelCache.getHotBarrelLevel(player, weapon);
      return hotBarrelLevel >= 60;
   }

   public static ParticleOptions getParticle(ItemStack weapon) {
      Map<Holder<Enchantment>, Integer> enchantments = ScEnchants.getEnchantments(weapon);
      if (ScEnchants.contains(enchantments, ModEnchantments.PUNCTURING)) {
         return ParticleTypes.ENCHANTED_HIT;
      } else if (ScEnchants.contains(enchantments, ModEnchantments.HEAVY_SHOT)) {
         return ParticleTypes.MYCELIUM;
      } else {
         return (ParticleOptions)(ScEnchants.contains(enchantments, ModEnchantments.ELEMENTAL_POP) ? ParticleTypes.CRIMSON_SPORE : new TrailData(weapon.isEnchanted()));
      }
   }

   public static float getPuncturingChance(ItemStack weapon) {
      int level = ScEnchants.level(weapon, ModEnchantments.PUNCTURING);
      return (float)level * 0.05F;
   }

   public static float getPuncturingArmorBypass(ItemStack weapon) {
      int puncturingLevel = ScEnchants.level(weapon, ModEnchantments.PUNCTURING);
      return puncturingLevel > 0 ? 5.0F * (float)puncturingLevel : 0.0F;
   }

   public static float getPuncturingDamageReduction(ItemStack weapon, LivingEntity target, float damage) {
      return damage;
   }

   public static float getWaterProofDamage(ItemStack weapon, Player player, float damage) {
      int waterProofLevel = ScEnchants.level(weapon, ModEnchantments.WATER_PROOF);
      return waterProofLevel > 0 && player != null && player.isUnderWater() ? damage * 1.15F : damage;
   }

   public static void applyElementalPopEffect(ItemStack weapon, LivingEntity target) {
      int enchantmentLevel = ScEnchants.level(weapon, ModEnchantments.ELEMENTAL_POP);
      if (enchantmentLevel > 0) {
         Random random = new Random();

         for (Entry<Holder<MobEffect>, Integer> entry : ELEMENTAL_EFFECTS.entrySet()) {
            MobEffect effect = entry.getKey().value();
            int baseChance = entry.getValue();
            int finalChance = baseChance + enchantmentLevel * 3;
            if (random.nextInt(100) < finalChance) {
               int duration = getRandomEffectDuration(effect, enchantmentLevel, random);
               int amplifier = getRandomEffectAmplifier(enchantmentLevel, random);
               target.addEffect(new MobEffectInstance(top.ribs.scguns.util.ScEffects.holder(effect), duration, amplifier));
               triggerVisualSplashEffect(target, effect);
               break;
            }
         }
      }
   }

   private static int getRandomEffectDuration(MobEffect effect, int enchantmentLevel, Random random) {
      int baseDuration = 60;
      int maxDuration = 200;
      if (effect.isInstantenous()) {
         return 1;
      } else {
         int duration = baseDuration + random.nextInt(maxDuration - baseDuration) + enchantmentLevel * 20;
         return Math.min(duration, maxDuration);
      }
   }

   private static int getRandomEffectAmplifier(int enchantmentLevel, Random random) {
      int baseAmplifier = 0;
      int maxAmplifier = 2;
      int amplifier = baseAmplifier + random.nextInt(enchantmentLevel + 1);
      return Math.min(amplifier, maxAmplifier);
   }

   private static void triggerVisualSplashEffect(LivingEntity target, MobEffect effect) {
      Level level = target.level();
      Vec3 position = target.position();
      int color = effect.getColor();
      double red = (double)(color >> 16 & 0xFF) / 255.0;
      double green = (double)(color >> 8 & 0xFF) / 255.0;
      double blue = (double)(color & 0xFF) / 255.0;

      for (int i = 0; i < 20; i++) {
         double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
         double offsetY = level.random.nextDouble() * 2.0;
         double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;
         level.addParticle(
            ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, (float)red, (float)green, (float)blue),
            position.x + offsetX,
            position.y + offsetY,
            position.z + offsetZ,
            0.0,
            0.0,
            0.0
         );
      }
   }

   public static int getQuickHands(ItemStack stack) {
      return ScEnchants.level(stack, ModEnchantments.QUICK_HANDS);
   }

   public static int getLightweight(ItemStack stack) {
      return ScEnchants.level(stack, ModEnchantments.LIGHTWEIGHT);
   }

   static {
      ELEMENTAL_EFFECTS.put(MobEffects.MOVEMENT_SPEED, 5);
      ELEMENTAL_EFFECTS.put(MobEffects.POISON, 6);
      ELEMENTAL_EFFECTS.put(MobEffects.WITHER, 3);
      ELEMENTAL_EFFECTS.put(MobEffects.HEAL, 6);
      ELEMENTAL_EFFECTS.put(MobEffects.HARM, 6);
      ELEMENTAL_EFFECTS.put(MobEffects.REGENERATION, 5);
      ELEMENTAL_EFFECTS.put(MobEffects.FIRE_RESISTANCE, 5);
      ELEMENTAL_EFFECTS.put(MobEffects.LEVITATION, 3);
      ELEMENTAL_EFFECTS.put(MobEffects.MOVEMENT_SLOWDOWN, 7);
      ELEMENTAL_EFFECTS.put(MobEffects.WEAKNESS, 3);
      ELEMENTAL_EFFECTS.put(MobEffects.ABSORPTION, 3);
      ELEMENTAL_EFFECTS.put(MobEffects.DAMAGE_RESISTANCE, 6);
      ELEMENTAL_EFFECTS.put(MobEffects.INVISIBILITY, 3);
      ELEMENTAL_EFFECTS.put(MobEffects.BLINDNESS, 2);
      ELEMENTAL_EFFECTS.put(MobEffects.HUNGER, 4);
      ELEMENTAL_EFFECTS.put(MobEffects.DIG_SLOWDOWN, 4);
      ELEMENTAL_EFFECTS.put(MobEffects.CONFUSION, 2);
      ELEMENTAL_EFFECTS.put(MobEffects.WATER_BREATHING, 4);
      ELEMENTAL_EFFECTS.put(MobEffects.NIGHT_VISION, 3);
      ELEMENTAL_EFFECTS.put(ModEffects.SULFUR_POISONING, 5);
      ELEMENTAL_EFFECTS.put(ModEffects.BLINDED, 2);
      ELEMENTAL_EFFECTS.put(ModEffects.DEAFENED, 2);
      ELEMENTAL_EFFECTS.put(ModEffects.LACERATED, 4);
   }
}
