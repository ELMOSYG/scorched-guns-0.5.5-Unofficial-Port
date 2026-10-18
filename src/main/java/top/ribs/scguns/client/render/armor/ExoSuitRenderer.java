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

   // Resolution of the four upgrade slots is expensive: every slot read hits NBT, and looking an upgrade up walks
   // the registered upgrade map. The renderer asks about upgrades once per bone, so on a thirty-bone suit that was
   // a hundred-plus NBT reads every frame. These four slots are filled once and then only re-read when the stack
   // changes or a frame has passed, which keeps the same behaviour with four reads per frame instead.
   private final ItemStack[] upgradeStacks = new ItemStack[] {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};

   private final ExoSuitUpgrade[] upgradeTypes = new ExoSuitUpgrade[4];

   private ItemStack lastResolvedStack = ItemStack.EMPTY;

   private long lastResolvedAt = 0L;

   private void ensureUpgrades(ItemStack armorStack) {
      // The render hot path never touches NBT: the cache resolved these four slots once this tick.
      if (this.currentSlot != null) {
         top.ribs.scguns.client.util.ExoSuitStateCache.Slots cached =
               top.ribs.scguns.client.util.ExoSuitStateCache.get(this.currentSlot);
         for (int slot = 0; slot < 4; slot++) {
            this.upgradeStacks[slot] = cached.stack(slot);
            this.upgradeTypes[slot] = cached.upgrade(slot);
         }

         return;
      }

      long now = System.nanoTime();
      if (armorStack == this.lastResolvedStack && now - this.lastResolvedAt < 50000000L) {
         return;
      }

      for (int slot = 0; slot < 4; slot++) {
         ItemStack upgradeItem = armorStack == null || armorStack.isEmpty()
               ? ItemStack.EMPTY
               : ExoSuitData.getUpgradeInSlot(armorStack, slot);
         this.upgradeStacks[slot] = upgradeItem.isEmpty() ? ItemStack.EMPTY : upgradeItem;
         this.upgradeTypes[slot] = upgradeItem.isEmpty() ? null : this.cachedUpgrade(upgradeItem);
      }

      this.lastResolvedStack = armorStack;
      this.lastResolvedAt = now;
   }

   private boolean slotHasType(int slot, String upgradeType) {
      ExoSuitUpgrade upgrade = this.upgradeTypes[slot];
      return upgrade != null && upgrade.getType().equals(upgradeType);
   }

   @Override
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
            this.setHiddenIfChanged(bone, shouldHideBase);
         } else if (this.isStandardArmorComponent(boneName)) {
            boolean showStandard = hasPlatingUpgrade && activeArmorType == ExoSuitRenderer.ArmorComponentType.STANDARD;
            this.setHiddenIfChanged(bone, !showStandard);
         } else if (this.isHeavyArmorComponent(boneName)) {
            boolean showHeavy = hasPlatingUpgrade && activeArmorType == ExoSuitRenderer.ArmorComponentType.HEAVY;
            this.setHiddenIfChanged(bone, !showHeavy);
         } else if (this.isLightArmorComponent(boneName)) {
            boolean showLight = hasPlatingUpgrade && activeArmorType == ExoSuitRenderer.ArmorComponentType.LIGHT;
            this.setHiddenIfChanged(bone, !showLight);
         } else if (this.isNightVisionComponent(boneName)) {
            boolean showNightVision = this.hasSpecificHudUpgrade(armorStack, NightVisionModuleItem.class);
            this.setHiddenIfChanged(bone, !showNightVision);
         } else if (this.isTargetTrackerComponent(boneName)) {
            boolean showTargetTracker = this.hasSpecificHudUpgrade(armorStack, TargetTrackerModuleItem.class);
            this.setHiddenIfChanged(bone, !showTargetTracker);
         } else if (this.isBreathingComponent(boneName)) {
            boolean hasBreathingUpgrade = this.hasUpgradeOfType(armorStack, "breathing");
            if (!hasBreathingUpgrade) {
               this.setHiddenIfChanged(bone, true);
            } else {
               ItemStack breathingUpgrade = this.findBreathingUpgrade(armorStack);
               if (!breathingUpgrade.isEmpty()) {
                  boolean isRebreather = breathingUpgrade.getItem() instanceof RebreatherModuleItem;
                  boolean isGasMask = breathingUpgrade.getItem() instanceof GasMaskModuleItem;
                  if (boneName.equals("breathing_unit_1")) {
                     this.setHiddenIfChanged(bone, !isRebreather);
                  } else if (boneName.equals("breathing_unit_2")) {
                     this.setHiddenIfChanged(bone, !isGasMask);
                  }
               } else {
                  this.setHiddenIfChanged(bone, true);
               }
            }
         } else if (this.isJetpackComponent(boneName)) {
            boolean hasJetpackUpgrade = this.hasUpgradeOfType(armorStack, "jetpack") || this.hasJetpackUtilityUpgrade(armorStack);
            this.setHiddenIfChanged(bone, !hasJetpackUpgrade);
         } else if (this.isPauldronComponent(boneName)) {
            boolean hasPauldronUpgrade = this.hasUpgradeOfType(armorStack, "pauldron");
            if (!hasPauldronUpgrade) {
               this.setHiddenIfChanged(bone, true);
            } else {
               ItemStack pauldronUpgrade = this.findPauldronUpgrade(armorStack);
               if (!pauldronUpgrade.isEmpty()) {
                  ExoSuitUpgrade upgrade = this.cachedUpgrade(pauldronUpgrade);
                  if (upgrade != null) {
                     String pauldronModel = upgrade.getDisplay().getModel();
                     if (boneName.contains("heavy_")) {
                        boolean showHeavyPauldron = pauldronModel.contains("heavy");
                        this.setHiddenIfChanged(bone, !showHeavyPauldron);
                     } else if (boneName.contains("standard_")) {
                        boolean showStandardPauldron = pauldronModel.contains("standard") || !pauldronModel.contains("heavy");
                        this.setHiddenIfChanged(bone, !showStandardPauldron);
                     } else {
                        this.setHiddenIfChanged(bone, false);
                     }
                  } else {
                     this.setHiddenIfChanged(bone, boneName.contains("heavy_"));
                  }
               } else {
                  this.setHiddenIfChanged(bone, true);
               }
            }
         } else if (this.isPouchesComponent(boneName)) {
            boolean showPouches = this.hasUpgradeOfType(armorStack, "pouches");
            if (!showPouches) {
               this.setHiddenIfChanged(bone, true);
            } else {
               ItemStack pouchesUpgrade = this.findPouchesUpgrade(armorStack);
               if (!pouchesUpgrade.isEmpty()) {
                  ExoSuitUpgrade upgrade = this.cachedUpgrade(pouchesUpgrade);
                  if (upgrade != null) {
                     String pouchesModel = upgrade.getDisplay().getModel();
                     if (boneName.contains("heavy_")) {
                        boolean showHeavyPouches = pouchesModel.contains("heavy");
                        this.setHiddenIfChanged(bone, !showHeavyPouches);
                     } else if (boneName.contains("standard_")) {
                        boolean showStandardPouches = pouchesModel.contains("standard") || !pouchesModel.contains("heavy");
                        this.setHiddenIfChanged(bone, !showStandardPouches);
                     } else {
                        this.setHiddenIfChanged(bone, false);
                     }
                  } else {
                     this.setHiddenIfChanged(bone, boneName.contains("heavy_"));
                  }
               } else {
                  this.setHiddenIfChanged(bone, true);
               }
            }
         } else if (this.isBackpackComponent(boneName)) {
            boolean showBackpack = this.hasUpgradeOfType(armorStack, "backpack") || this.hasUpgradeOfType(armorStack, "utility");
            this.setHiddenIfChanged(bone, !showBackpack);
         } else if (this.isKneeGuardComponent(boneName)) {
            boolean hasKneeGuardUpgrade = this.hasUpgradeOfType(armorStack, "knee_guard");
            boolean hasPlatingInKneeGuardSlot = this.hasPlatingInKneeGuardSlot(armorStack);
            if (!hasKneeGuardUpgrade && !hasPlatingInKneeGuardSlot) {
               this.setHiddenIfChanged(bone, true);
            } else {
               ItemStack kneeGuardUpgrade = this.findKneeGuardOrPlatingUpgrade(armorStack);
               if (!kneeGuardUpgrade.isEmpty()) {
                  ExoSuitUpgrade upgrade;
                  if (hasPlatingInKneeGuardSlot && !hasKneeGuardUpgrade) {
                     upgrade = ExoSuitUpgradeManager.getUpgradeForItemInSlot(kneeGuardUpgrade, "knee_guard");
                  } else {
                     upgrade = this.cachedUpgrade(kneeGuardUpgrade);
                  }

                  if (upgrade != null) {
                     String kneeGuardModel = upgrade.getDisplay().getModel();
                     if (boneName.contains("heavy_")) {
                        boolean showHeavyKneeGuard = kneeGuardModel.contains("heavy");
                        this.setHiddenIfChanged(bone, !showHeavyKneeGuard);
                     } else if (boneName.contains("standard_")) {
                        boolean showStandardKneeGuard = kneeGuardModel.contains("standard") || !kneeGuardModel.contains("heavy");
                        this.setHiddenIfChanged(bone, !showStandardKneeGuard);
                     } else {
                        this.setHiddenIfChanged(bone, false);
                     }
                  } else {
                     this.setHiddenIfChanged(bone, boneName.contains("heavy_"));
                  }
               } else {
                  this.setHiddenIfChanged(bone, true);
               }
            }
         } else if (this.isMobilityModuleComponent(boneName)) {
            boolean hasMobilityUpgrade = this.hasUpgradeOfType(armorStack, "mobility");
            if (!hasMobilityUpgrade) {
               this.setHiddenIfChanged(bone, true);
            } else {
               ItemStack mobilityUpgrade = this.findMobilityUpgrade(armorStack);
               if (!mobilityUpgrade.isEmpty()) {
                  boolean isRabbitModule = mobilityUpgrade.getItem() instanceof RabbitModuleItem;
                  boolean isShockAbsorberModule = mobilityUpgrade.getItem().toString().contains("shock_absorber");
                  if (boneName.contains("rabbit_")) {
                     this.setHiddenIfChanged(bone, !isRabbitModule);
                  } else if (boneName.contains("shock_absorber_")) {
                     this.setHiddenIfChanged(bone, !isShockAbsorberModule);
                  } else {
                     this.setHiddenIfChanged(bone, false);
                  }
               } else {
                  this.setHiddenIfChanged(bone, true);
               }
            }
         } else {
            if (this.isFutureUpgradeBone(boneName)) {
               this.setHiddenIfChanged(bone, true);
            }
         }
      }
   }

   private ExoSuitRenderer.ArmorComponentType determineActiveArmorType(ItemStack armorStack) {
      if (this.hasUpgradeInSlot(armorStack, 0)) {
         this.ensureUpgrades(armorStack);
      ItemStack upgradeItem = this.upgradeStacks[0];
         ExoSuitUpgrade upgrade = this.cachedUpgrade(upgradeItem);
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
         this.ensureUpgrades(armorStack);
         return this.upgradeTypes[slot] != null;
      } catch (Exception var6) {
         System.err.println("ExoSuit: Error checking upgrade slot " + slot + ": " + var6.getMessage());
         return false;
      }
   }

   private boolean hasUpgradeOfType(ItemStack armorStack, String upgradeType) {
      try {
         this.ensureUpgrades(armorStack);
         for (int slot = 0; slot < 4; slot++) {
            if (this.slotHasType(slot, upgradeType)) {
               return true;
            }
         }

         return false;
      } catch (Exception var6) {
         return false;
      }
   }

   private boolean hasPlatingInKneeGuardSlot(ItemStack armorStack) {
      try {
         this.ensureUpgrades(armorStack);
         ItemStack upgradeItem = this.upgradeStacks[1];
         if (upgradeItem.isEmpty()) {
            return false;
         } else {
            ExoSuitUpgrade upgrade = this.cachedUpgrade(upgradeItem);
            return upgrade != null && upgrade.getType().equals("plating");
         }
      } catch (Exception var4) {
         return false;
      }
   }

   private ItemStack findKneeGuardOrPlatingUpgrade(ItemStack armorStack) {
      this.ensureUpgrades(armorStack);
      for (int slot = 0; slot < 4; slot++) {
         ItemStack upgradeItem = this.upgradeStacks[slot];
         if (!upgradeItem.isEmpty()) {
            ExoSuitUpgrade upgrade = this.upgradeTypes[slot];
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
         this.ensureUpgrades(armorStack);
         for (int slot = 0; slot < 4; slot++) {
            ItemStack upgradeItem = this.upgradeStacks[slot];
            if (!upgradeItem.isEmpty()) {
               ExoSuitUpgrade upgrade = this.upgradeTypes[slot];
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
      this.ensureUpgrades(armorStack);
      for (int slot = 0; slot < 4; slot++) {
         if (this.slotHasType(slot, "breathing")) {
            return this.upgradeStacks[slot];
         }
      }

      return ItemStack.EMPTY;
   }

   private ItemStack findPauldronUpgrade(ItemStack armorStack) {
      this.ensureUpgrades(armorStack);
      for (int slot = 0; slot < 4; slot++) {
         if (this.slotHasType(slot, "pauldron")) {
            return this.upgradeStacks[slot];
         }
      }

      return ItemStack.EMPTY;
   }

   private ItemStack findPouchesUpgrade(ItemStack armorStack) {
      this.ensureUpgrades(armorStack);
      for (int slot = 0; slot < 4; slot++) {
         if (this.slotHasType(slot, "pouches")) {
            return this.upgradeStacks[slot];
         }
      }

      return ItemStack.EMPTY;
   }

   private ItemStack findMobilityUpgrade(ItemStack armorStack) {
      this.ensureUpgrades(armorStack);
      for (int slot = 0; slot < 4; slot++) {
         if (this.slotHasType(slot, "mobility")) {
            return this.upgradeStacks[slot];
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
      this.ensureUpgrades(armorStack);
      for (int slot = 0; slot < 4; slot++) {
         ItemStack upgradeItem = this.upgradeStacks[slot];
         if (!upgradeItem.isEmpty()) {
            ExoSuitUpgrade upgrade = this.upgradeTypes[slot];
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

   /**
    * Upgrade definition for a slot stack, taken from the per-tick cache when possible.
    *
    * <p>The cache holds the four resolved upgrades of the piece being rendered, so matching the stack against them
    * answers the question without walking the registered upgrade table again. Anything the cache cannot answer
    * (a stack that is not one of those four, or rendering without a slot) falls through to the registry lookup, so
    * behaviour is unchanged.</p>
    */
   private ExoSuitUpgrade cachedUpgrade(ItemStack upgradeItem) {
      if (this.currentSlot != null && upgradeItem != null && !upgradeItem.isEmpty()) {
         top.ribs.scguns.client.util.ExoSuitStateCache.Slots cached =
               top.ribs.scguns.client.util.ExoSuitStateCache.get(this.currentSlot);
         for (int slot = 0; slot < 4; slot++) {
            if (cached.stack(slot) == upgradeItem) {
               return cached.upgrade(slot);
            }
         }
      }

      return ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
   }

   /**
    * Writes a bone's hidden flag only when it actually changes.
    *
    * <p>GeckoLib bones are shared by the baked model, and toggling hidden marks the bone (and its children) dirty, so
    * re-writing the same value every frame rebuilt that state over and over. With every upgrade slot filled the suit
    * touches most of its bones, which is why a fully kitted suit dropped framerate far more than an empty one.
    * Comparing first means a stationary suit writes nothing at all.</p>
    */
   private void setHiddenIfChanged(GeoBone bone, boolean hidden) {
      if (bone.isHidden() != hidden) {
         bone.setHidden(hidden);
      }
   }
}
