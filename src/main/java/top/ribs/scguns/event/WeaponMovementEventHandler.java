package top.ribs.scguns.event;



import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.util.ScEnchants;
import java.util.UUID;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.ribs.scguns.init.ModEnchantments;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.animated.ExoSuitItem;

@EventBusSubscriber(
   modid = "scguns"
)
public class WeaponMovementEventHandler {
   private static final ResourceLocation HEAVY_WEAPON_MODIFIER_UUID = ResourceLocation.fromNamespaceAndPath("scguns", "ff624994-88ae-4002-b5c9-b41b6d658030");
   private static final ResourceLocation RELOAD_SPEED_MODIFIER_UUID = ResourceLocation.fromNamespaceAndPath("scguns", "aa815773-99bf-4113-c6d8-e52c7f769041");
   private static final ResourceLocation LIGHTWEIGHT_SPEED_MODIFIER_UUID = ResourceLocation.fromNamespaceAndPath("scguns", "bb926884-10cf-5224-d7e9-f63d8e890152");
   private static final double LIGHTWEIGHT_REDUCTION_PER_LEVEL = 0.2;
   private static final double LIGHTWEIGHT_SPEED_BONUS_PER_LEVEL = 0.05;
   private static final double RELOAD_SPEED_PENALTY = 0.75;
   private static final double LIGHTWEIGHT_RELOAD_BONUS_PER_LEVEL = 0.08;
   private static final double SWIFT_SNEAK_RELOAD_BONUS_PER_LEVEL = 0.05;
   private static int tickCounter = 0;
   private static final int UPDATE_INTERVAL = 20;

   public WeaponMovementEventHandler() {
      super();
   }

   @SubscribeEvent
   public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
      if (event.getEntity() instanceof Player player) {
         EquipmentSlot slot = event.getSlot();
         if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND || slot == EquipmentSlot.LEGS) {
            updateSpeedAttribute(player);
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (!event.getEntity().level().isClientSide) {
         tickCounter++;
         if (tickCounter >= 20) {
            tickCounter = 0;
            updateSpeedAttribute(event.getEntity());
         }
      }
   }

   private static void updateSpeedAttribute(Player player) {
      ItemStack mainHandItem = player.getMainHandItem();
      ItemStack offHandItem = player.getOffhandItem();
      ItemStack legsItem = player.getItemBySlot(EquipmentSlot.LEGS);
      AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (movementSpeed != null) {
         movementSpeed.removeModifier(HEAVY_WEAPON_MODIFIER_UUID);
         movementSpeed.removeModifier(RELOAD_SPEED_MODIFIER_UUID);
         movementSpeed.removeModifier(LIGHTWEIGHT_SPEED_MODIFIER_UUID);
         boolean hasExoSuitLegs = legsItem.getItem() instanceof ExoSuitItem;
         boolean isReloading = (Boolean)ModSyncedDataKeys.RELOADING.getValue(player);
         boolean holdingGun = mainHandItem.getItem() instanceof GunItem || offHandItem.getItem() instanceof GunItem;
         if (isReloading && !holdingGun) {
            ModSyncedDataKeys.RELOADING.setValue(player, false);
            isReloading = false;
         }

         float mainHandSpeedModifier = getEffectiveSpeedModifier(mainHandItem, hasExoSuitLegs);
         float offHandSpeedModifier = getEffectiveSpeedModifier(offHandItem, hasExoSuitLegs);
         float finalSpeedModifier = Math.min(mainHandSpeedModifier, offHandSpeedModifier);
         ItemStack relevantWeapon = mainHandSpeedModifier < offHandSpeedModifier ? mainHandItem : offHandItem;
         if (finalSpeedModifier < 1.0F && isHeavyWeapon(relevantWeapon)) {
            int lightweightLevel = ScEnchants.level(relevantWeapon, ModEnchantments.LIGHTWEIGHT);
            double reduction = 0.2 * (double)lightweightLevel;
            finalSpeedModifier = (float)Math.min(1.0, (double)finalSpeedModifier + reduction);
         }

         if (finalSpeedModifier != 1.0F) {
            AttributeModifier modifier = new AttributeModifier(HEAVY_WEAPON_MODIFIER_UUID, (double)finalSpeedModifier - 1.0, Operation.ADD_MULTIPLIED_BASE);
            movementSpeed.addTransientModifier(modifier);
         }

         if (holdingGun) {
            ItemStack gunWithLightweight = null;
            int maxLightweightLevel = 0;
            if (mainHandItem.getItem() instanceof GunItem) {
               int level = ScEnchants.level(mainHandItem, ModEnchantments.LIGHTWEIGHT);
               if (level > maxLightweightLevel) {
                  maxLightweightLevel = level;
                  gunWithLightweight = mainHandItem;
               }
            }

            if (offHandItem.getItem() instanceof GunItem) {
               int level = ScEnchants.level(offHandItem, ModEnchantments.LIGHTWEIGHT);
               if (level > maxLightweightLevel) {
                  maxLightweightLevel = level;
                  gunWithLightweight = offHandItem;
               }
            }

            if (gunWithLightweight != null) {
               double speedBonus = 0.0;
               if (isHeavyWeapon(gunWithLightweight)) {
                  if (maxLightweightLevel >= 2) {
                     speedBonus = 0.05;
                  }
               } else {
                  speedBonus = 0.05 * (double)maxLightweightLevel;
               }

               if (speedBonus > 0.0) {
                  AttributeModifier lightweightSpeedModifier = new AttributeModifier(LIGHTWEIGHT_SPEED_MODIFIER_UUID, speedBonus, Operation.ADD_MULTIPLIED_BASE);
                  movementSpeed.addTransientModifier(lightweightSpeedModifier);
               }
            }
         }

         if (isReloading && holdingGun) {
            double reloadSpeedModifier = 0.75;
            ItemStack gunItem = mainHandItem.getItem() instanceof GunItem ? mainHandItem : offHandItem;
            int lightweightLevel = ScEnchants.level(gunItem, ModEnchantments.LIGHTWEIGHT);
            double lightweightBonus = 0.08 * (double)lightweightLevel;
            int swiftSneakLevel = ScEnchants.level(legsItem, Enchantments.SWIFT_SNEAK);
            double swiftSneakBonus = 0.05 * (double)swiftSneakLevel;
            reloadSpeedModifier = Math.min(1.0, reloadSpeedModifier + lightweightBonus + swiftSneakBonus);
            AttributeModifier reloadModifier = new AttributeModifier(RELOAD_SPEED_MODIFIER_UUID, reloadSpeedModifier - 1.0, Operation.ADD_MULTIPLIED_BASE);
            movementSpeed.addTransientModifier(reloadModifier);
         }
      }
   }

   private static float getEffectiveSpeedModifier(ItemStack stack, boolean hasExoSuitLegs) {
      if (stack.isEmpty()) {
         return 1.0F;
      } else if (stack.getItem() instanceof GunItem gunItem) {
         float baseModifier = gunItem.getGunProperties().getGeneral().getSpeedModifier();
         return hasExoSuitLegs && baseModifier < 1.0F && isHeavyWeapon(stack) ? 1.0F : baseModifier;
      } else {
         return 1.0F;
      }
   }

   private static boolean isHeavyWeapon(ItemStack itemStack) {
      return !itemStack.isEmpty() && itemStack.is(ModTags.Items.HEAVY_WEAPON);
   }
}
