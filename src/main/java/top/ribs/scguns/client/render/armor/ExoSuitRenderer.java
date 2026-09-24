package top.ribs.scguns.client.render.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.ribs.scguns.common.exosuit.ExoSuitData;
import top.ribs.scguns.common.exosuit.ExoSuitUpgrade;
import top.ribs.scguns.common.exosuit.ExoSuitUpgradeManager;
import top.ribs.scguns.item.animated.ExoSuitItem;
import top.ribs.scguns.item.exosuit.GasMaskModuleItem;
import top.ribs.scguns.item.exosuit.NightVisionModuleItem;
import top.ribs.scguns.item.exosuit.RabbitModuleItem;
import top.ribs.scguns.item.exosuit.RebreatherModuleItem;
import top.ribs.scguns.item.exosuit.TargetTrackerModuleItem;

public class ExoSuitRenderer extends GeoArmorRenderer<ExoSuitItem> {
   public ExoSuitRenderer() {
      super(new ExoSuitModel());
   }

   public void renderRecursively(
      PoseStack poseStack,
      ExoSuitItem animatable,
      GeoBone bone,
      RenderType renderType,
      MultiBufferSource bufferSource,
      VertexConsumer buffer,
      boolean isReRender,
      float partialTick,
      int packedLight,
      int packedOverlay,
      int color
   ) {
      ItemStack liveArmorStack = this.getCurrentStack();
      if (this.currentEntity instanceof LivingEntity livingEntity && this.currentSlot != null) {
         liveArmorStack = livingEntity.getItemBySlot(this.currentSlot);
      }

      this.handleComponentVisibility(bone, liveArmorStack);
      // GeckoLib 4.6+ replaced the four red/green/blue/alpha floats with one packed ARGB colour.
      super.renderRecursively(
         poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color
      );
   }

   private void handleComponentVisibility(GeoBone bone, ItemStack armorStack) {
      if (armorStack != null && armorStack.getItem() instanceof ExoSuitItem) {
         String boneName = bone.getName();
         boolean hasPlatingUpgrade = this.hasUpgradeInSlot(armorStack, 0);
         ExoSuitRenderer.ArmorComponentType activeArmorType = this.determineActiveArmorType(armorStack);
         if (this.isBaseComponent(boneName)) {
            boolean shouldHideBase = activeArmorType.shouldHideBase() && hasPlatingUpgrade;
            bone.setHidden(shouldHideBase);
         } else if (this.isStandardArmorComponent(boneName)) {
            boolean showStandard = hasPlatingUpgrade && activeArmorType == ExoSuitRenderer.ArmorComponentType.STANDARD;
            bone.setHidden(!showStandard);
         } else if (this.isHeavyArmorComponent(boneName)) {
            boolean showHeavy = hasPlatingUpgrade && activeArmorType == ExoSuitRenderer.ArmorComponentType.HEAVY;
            bone.setHidden(!showHeavy);
         } else if (this.isLightArmorComponent(boneName)) {
            boolean showLight = hasPlatingUpgrade && activeArmorType == ExoSuitRenderer.ArmorComponentType.LIGHT;
            bone.setHidden(!showLight);
         } else if (this.isNightVisionComponent(boneName)) {
            boolean showNightVision = this.hasSpecificHudUpgrade(armorStack, NightVisionModuleItem.class);
            bone.setHidden(!showNightVision);
         } else if (this.isTargetTrackerComponent(boneName)) {
            boolean showTargetTracker = this.hasSpecificHudUpgrade(armorStack, TargetTrackerModuleItem.class);
            bone.setHidden(!showTargetTracker);
         } else if (this.isBreathingComponent(boneName)) {
            boolean hasBreathingUpgrade = this.hasUpgradeOfType(armorStack, "breathing");
            if (!hasBreathingUpgrade) {
               bone.setHidden(true);
            } else {
               ItemStack breathingUpgrade = this.findBreathingUpgrade(armorStack);
               if (!breathingUpgrade.isEmpty()) {
                  boolean isRebreather = breathingUpgrade.getItem() instanceof RebreatherModuleItem;
                  boolean isGasMask = breathingUpgrade.getItem() instanceof GasMaskModuleItem;
                  if (boneName.equals("breathing_unit_1")) {
                     bone.setHidden(!isRebreather);
                  } else if (boneName.equals("breathing_unit_2")) {
                     bone.setHidden(!isGasMask);
                  }
               } else {
                  bone.setHidden(true);
               }
            }
         } else if (this.isJetpackComponent(boneName)) {
            boolean hasJetpackUpgrade = this.hasUpgradeOfType(armorStack, "jetpack") || this.hasJetpackUtilityUpgrade(armorStack);
            bone.setHidden(!hasJetpackUpgrade);
         } else if (this.isPauldronComponent(boneName)) {
            boolean hasPauldronUpgrade = this.hasUpgradeOfType(armorStack, "pauldron");
            if (!hasPauldronUpgrade) {
               bone.setHidden(true);
            } else {
               ItemStack pauldronUpgrade = this.findPauldronUpgrade(armorStack);
               if (!pauldronUpgrade.isEmpty()) {
                  ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(pauldronUpgrade);
                  if (upgrade != null) {
                     String pauldronModel = upgrade.getDisplay().getModel();
                     if (boneName.contains("heavy_")) {
                        boolean showHeavyPauldron = pauldronModel.contains("heavy");
                        bone.setHidden(!showHeavyPauldron);
                     } else if (boneName.contains("standard_")) {
                        boolean showStandardPauldron = pauldronModel.contains("standard") || !pauldronModel.contains("heavy");
                        bone.setHidden(!showStandardPauldron);
                     } else {
                        bone.setHidden(false);
                     }
                  } else {
                     bone.setHidden(boneName.contains("heavy_"));
                  }
               } else {
                  bone.setHidden(true);
               }
            }
         } else if (this.isPouchesComponent(boneName)) {
            boolean showPouches = this.hasUpgradeOfType(armorStack, "pouches");
            if (!showPouches) {
               bone.setHidden(true);
            } else {
               ItemStack pouchesUpgrade = this.findPouchesUpgrade(armorStack);
               if (!pouchesUpgrade.isEmpty()) {
                  ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(pouchesUpgrade);
                  if (upgrade != null) {
                     String pouchesModel = upgrade.getDisplay().getModel();
                     if (boneName.contains("heavy_")) {
                        boolean showHeavyPouches = pouchesModel.contains("heavy");
                        bone.setHidden(!showHeavyPouches);
                     } else if (boneName.contains("standard_")) {
                        boolean showStandardPouches = pouchesModel.contains("standard") || !pouchesModel.contains("heavy");
                        bone.setHidden(!showStandardPouches);
                     } else {
                        bone.setHidden(false);
                     }
                  } else {
                     bone.setHidden(boneName.contains("heavy_"));
                  }
               } else {
                  bone.setHidden(true);
               }
            }
         } else if (this.isBackpackComponent(boneName)) {
            boolean showBackpack = this.hasUpgradeOfType(armorStack, "backpack") || this.hasUpgradeOfType(armorStack, "utility");
            bone.setHidden(!showBackpack);
         } else if (this.isKneeGuardComponent(boneName)) {
            boolean hasKneeGuardUpgrade = this.hasUpgradeOfType(armorStack, "knee_guard");
            boolean hasPlatingInKneeGuardSlot = this.hasPlatingInKneeGuardSlot(armorStack);
            if (!hasKneeGuardUpgrade && !hasPlatingInKneeGuardSlot) {
               bone.setHidden(true);
            } else {
               ItemStack kneeGuardUpgrade = this.findKneeGuardOrPlatingUpgrade(armorStack);
               if (!kneeGuardUpgrade.isEmpty()) {
                  ExoSuitUpgrade upgrade;
                  if (hasPlatingInKneeGuardSlot && !hasKneeGuardUpgrade) {
                     upgrade = ExoSuitUpgradeManager.getUpgradeForItemInSlot(kneeGuardUpgrade, "knee_guard");
                  } else {
                     upgrade = ExoSuitUpgradeManager.getUpgradeForItem(kneeGuardUpgrade);
                  }

                  if (upgrade != null) {
                     String kneeGuardModel = upgrade.getDisplay().getModel();
                     if (boneName.contains("heavy_")) {
                        boolean showHeavyKneeGuard = kneeGuardModel.contains("heavy");
                        bone.setHidden(!showHeavyKneeGuard);
                     } else if (boneName.contains("standard_")) {
                        boolean showStandardKneeGuard = kneeGuardModel.contains("standard") || !kneeGuardModel.contains("heavy");
                        bone.setHidden(!showStandardKneeGuard);
                     } else {
                        bone.setHidden(false);
                     }
                  } else {
                     bone.setHidden(boneName.contains("heavy_"));
                  }
               } else {
                  bone.setHidden(true);
               }
            }
         } else if (this.isMobilityModuleComponent(boneName)) {
            boolean hasMobilityUpgrade = this.hasUpgradeOfType(armorStack, "mobility");
            if (!hasMobilityUpgrade) {
               bone.setHidden(true);
            } else {
               ItemStack mobilityUpgrade = this.findMobilityUpgrade(armorStack);
               if (!mobilityUpgrade.isEmpty()) {
                  boolean isRabbitModule = mobilityUpgrade.getItem() instanceof RabbitModuleItem;
                  boolean isShockAbsorberModule = mobilityUpgrade.getItem().toString().contains("shock_absorber");
                  if (boneName.contains("rabbit_")) {
                     bone.setHidden(!isRabbitModule);
                  } else if (boneName.contains("shock_absorber_")) {
                     bone.setHidden(!isShockAbsorberModule);
                  } else {
                     bone.setHidden(false);
                  }
               } else {
                  bone.setHidden(true);
               }
            }
         } else {
            if (this.isFutureUpgradeBone(boneName)) {
               bone.setHidden(true);
            }
         }
      }
   }

   private ExoSuitRenderer.ArmorComponentType determineActiveArmorType(ItemStack armorStack) {
      if (this.hasUpgradeInSlot(armorStack, 0)) {
         ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, 0);
         ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
         if (upgrade != null) {
            String modelType = upgrade.getDisplay().getModel();

            return switch (modelType) {
               case "heavy_plating", "heavy" -> ExoSuitRenderer.ArmorComponentType.HEAVY;
               case "standard_plating", "standard" -> ExoSuitRenderer.ArmorComponentType.STANDARD;
               case "light_plating", "light" -> ExoSuitRenderer.ArmorComponentType.LIGHT;
               case "medium_plating", "medium" -> ExoSuitRenderer.ArmorComponentType.MEDIUM;
               default -> ExoSuitRenderer.ArmorComponentType.BASE;
            };
         }
      }

      return ExoSuitRenderer.ArmorComponentType.BASE;
   }

   private boolean hasUpgradeInSlot(ItemStack armorStack, int slot) {
      try {
         ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
         boolean hasUpgrade = !upgradeItem.isEmpty();
         if (hasUpgrade) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
            return upgrade != null;
         } else {
            return false;
         }
      } catch (Exception var6) {
         System.err.println("ExoSuit: Error checking upgrade slot " + slot + ": " + var6.getMessage());
         return false;
      }
   }

   private boolean hasUpgradeOfType(ItemStack armorStack, String upgradeType) {
      try {
         for (int slot = 0; slot < 4; slot++) {
            ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
            if (!upgradeItem.isEmpty()) {
               ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
               if (upgrade != null && upgrade.getType().equals(upgradeType)) {
                  return true;
               }
            }
         }

         return false;
      } catch (Exception var6) {
         return false;
      }
   }

   private boolean hasPlatingInKneeGuardSlot(ItemStack armorStack) {
      try {
         ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, 1);
         if (upgradeItem.isEmpty()) {
            return false;
         } else {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
            return upgrade != null && upgrade.getType().equals("plating");
         }
      } catch (Exception var4) {
         return false;
      }
   }

   private ItemStack findKneeGuardOrPlatingUpgrade(ItemStack armorStack) {
      for (int slot = 0; slot < 4; slot++) {
         ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
         if (!upgradeItem.isEmpty()) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
            if (upgrade != null) {
               if (upgrade.getType().equals("knee_guard")) {
                  return upgradeItem;
               }

               if (slot == 1 && upgrade.getType().equals("plating")) {
                  return upgradeItem;
               }
            }
         }
      }

      return ItemStack.EMPTY;
   }

   private boolean hasSpecificHudUpgrade(ItemStack armorStack, Class<?> upgradeClass) {
      try {
         for (int slot = 0; slot < 4; slot++) {
            ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
            if (!upgradeItem.isEmpty()) {
               ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
               if (upgrade != null && upgrade.getType().equals("hud") && upgradeClass.isInstance(upgradeItem.getItem())) {
                  return true;
               }
            }
         }

         return false;
      } catch (Exception var6) {
         return false;
      }
   }

   private ItemStack findBreathingUpgrade(ItemStack armorStack) {
      for (int slot = 0; slot < 4; slot++) {
         ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
         if (!upgradeItem.isEmpty()) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
            if (upgrade != null && upgrade.getType().equals("breathing")) {
               return upgradeItem;
            }
         }
      }

      return ItemStack.EMPTY;
   }

   private ItemStack findPauldronUpgrade(ItemStack armorStack) {
      for (int slot = 0; slot < 4; slot++) {
         ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
         if (!upgradeItem.isEmpty()) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
            if (upgrade != null && upgrade.getType().equals("pauldron")) {
               return upgradeItem;
            }
         }
      }

      return ItemStack.EMPTY;
   }

   private ItemStack findPouchesUpgrade(ItemStack armorStack) {
      for (int slot = 0; slot < 4; slot++) {
         ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
         if (!upgradeItem.isEmpty()) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
            if (upgrade != null && upgrade.getType().equals("pouches")) {
               return upgradeItem;
            }
         }
      }

      return ItemStack.EMPTY;
   }

   private ItemStack findMobilityUpgrade(ItemStack armorStack) {
      for (int slot = 0; slot < 4; slot++) {
         ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
         if (!upgradeItem.isEmpty()) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
            if (upgrade != null && upgrade.getType().equals("mobility")) {
               return upgradeItem;
            }
         }
      }

      return ItemStack.EMPTY;
   }

   private boolean isBaseComponent(String boneName) {
      return boneName.equals("base_helmet")
         || boneName.equals("base_torso")
         || boneName.equals("base_right_arm")
         || boneName.equals("base_left_arm")
         || boneName.equals("base_right_leg")
         || boneName.equals("base_left_leg")
         || boneName.equals("base_right_boot")
         || boneName.equals("base_left_boot");
   }

   private boolean isStandardArmorComponent(String boneName) {
      return boneName.equals("standard_helmet")
         || boneName.equals("standard_torso")
         || boneName.equals("standard_right_arm")
         || boneName.equals("standard_left_arm")
         || boneName.equals("standard_right_leg")
         || boneName.equals("standard_left_leg")
         || boneName.equals("standard_right_boot")
         || boneName.equals("standard_left_boot");
   }

   private boolean isHeavyArmorComponent(String boneName) {
      return boneName.equals("heavy_helmet")
         || boneName.equals("heavy_torso")
         || boneName.equals("heavy_right_arm")
         || boneName.equals("heavy_left_arm")
         || boneName.equals("heavy_right_leg")
         || boneName.equals("heavy_left_leg")
         || boneName.equals("heavy_right_boot")
         || boneName.equals("heavy_left_boot");
   }

   private boolean isLightArmorComponent(String boneName) {
      return boneName.equals("light_helmet")
         || boneName.equals("light_torso")
         || boneName.equals("light_right_arm")
         || boneName.equals("light_left_arm")
         || boneName.equals("light_right_leg")
         || boneName.equals("light_left_leg")
         || boneName.equals("light_right_boot")
         || boneName.equals("light_left_boot");
   }

   private boolean isJetpackComponent(String boneName) {
      return boneName.equals("jetpack");
   }

   private boolean hasJetpackUtilityUpgrade(ItemStack armorStack) {
      for (int slot = 0; slot < 4; slot++) {
         ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(armorStack, slot);
         if (!upgradeItem.isEmpty()) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
            if (upgrade != null && upgrade.getType().equals("utility")) {
               String model = upgrade.getDisplay().getModel();
               return model != null && model.contains("jetpack");
            }
         }
      }

      return false;
   }

   private boolean isNightVisionComponent(String boneName) {
      return boneName.equals("night_vision");
   }

   private boolean isTargetTrackerComponent(String boneName) {
      return boneName.equals("target_tracker");
   }

   private boolean isBreathingComponent(String boneName) {
      return boneName.equals("breathing_unit_1") || boneName.equals("breathing_unit_2");
   }

   private boolean isPauldronComponent(String boneName) {
      return boneName.equals("heavy_right_pauldron")
         || boneName.equals("heavy_left_pauldron")
         || boneName.equals("standard_right_pauldron")
         || boneName.equals("standard_left_pauldron");
   }

   private boolean isPouchesComponent(String boneName) {
      return boneName.equals("standard_pouches") || boneName.equals("heavy_pouches");
   }

   private boolean isBackpackComponent(String boneName) {
      return boneName.equals("backpack");
   }

   private boolean isKneeGuardComponent(String boneName) {
      return boneName.equals("standard_left_knee_guard")
         || boneName.equals("standard_right_knee_guard")
         || boneName.equals("heavy_left_knee_guard")
         || boneName.equals("heavy_right_knee_guard");
   }

   private boolean isMobilityModuleComponent(String boneName) {
      return boneName.equals("rabbit_left_module")
         || boneName.equals("rabbit_right_module")
         || boneName.equals("shock_absorber_left_module")
         || boneName.equals("shock_absorber_right_module");
   }

   private boolean isFutureUpgradeBone(String boneName) {
      return !boneName.equals("night_vision")
            && !boneName.equals("target_tracker")
            && !boneName.equals("breathing_unit_1")
            && !boneName.equals("breathing_unit_2")
         ? boneName.contains("plating_")
            || boneName.contains("upgrade_")
            || boneName.contains("power_")
            || boneName.contains("enhancement_")
            || boneName.contains("stealth_")
            || boneName.contains("tech_")
            || boneName.contains("utility_")
         : false;
   }

   public static enum ArmorComponentType {
      BASE(false),
      STANDARD(true),
      HEAVY(true),
      LIGHT(false),
      MEDIUM(true),
      UTILITY(false),
      STEALTH(false),
      TECH(false);

      private final boolean hidesBase;

      private ArmorComponentType(boolean hidesBase) {
         this.hidesBase = hidesBase;
      }

      public boolean shouldHideBase() {
         return this.hidesBase;
      }
   }
}
