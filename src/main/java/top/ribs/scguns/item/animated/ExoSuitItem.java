package top.ribs.scguns.item.animated;






import net.minecraft.world.item.Item;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Holder;
import top.ribs.scguns.util.Caps;
import top.ribs.scguns.util.NbtHelper;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.Animation.LoopType;
import software.bernie.geckolib.animation.PlayState;
import top.ribs.scguns.client.KeyBinds;
import top.ribs.scguns.client.render.armor.ExoSuitRenderer;
import top.ribs.scguns.client.screen.ExoSuitMenu;
import top.ribs.scguns.common.exosuit.ExoSuitData;
import top.ribs.scguns.common.exosuit.ExoSuitUpgrade;
import top.ribs.scguns.common.exosuit.ExoSuitUpgradeManager;
import top.ribs.scguns.item.exosuit.DamageableUpgradeItem;
import top.ribs.scguns.item.exosuit.GasMaskModuleItem;
import top.ribs.scguns.item.exosuit.NightVisionModuleItem;
import top.ribs.scguns.item.exosuit.RebreatherModuleItem;
import top.ribs.scguns.item.exosuit.TargetTrackerModuleItem;

public class ExoSuitItem extends ArmorItem implements GeoItem {
   private AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

   public ExoSuitItem(Holder<ArmorMaterial> pMaterial, Type pType, Properties pProperties) {
      super(pMaterial, pType, pProperties);
   }

   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(new IClientItemExtensions() {
         private ExoSuitRenderer renderer;

         @NotNull
         public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
            if (this.renderer == null) {
               this.renderer = new ExoSuitRenderer();
            }

            this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
            return this.renderer;
         }
      });
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (player.isShiftKeyDown()) {
         if (!level.isClientSide) {
            this.openExoSuitScreen((ServerPlayer)player, hand);
         }

         return InteractionResultHolder.success(itemStack);
      } else {
         return super.use(level, player, hand);
      }
   }

   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (player != null && player.isShiftKeyDown()) {
         if (!context.getLevel().isClientSide) {
            this.openExoSuitScreen((ServerPlayer)player, context.getHand());
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   private void openExoSuitScreen(ServerPlayer player, InteractionHand hand) {
      // 1.21 replaced NetworkHooks.openScreen with ServerPlayer#openMenu; the extra data
      // writer must write exactly what ExoSuitMenu's FriendlyByteBuf constructor reads.
      player.openMenu(new ExoSuitItem.ExoSuitMenuProvider(hand), buf -> buf.writeEnum(hand));
   }

   @Override
   public void appendHoverText(ItemStack stack, Item.TooltipContext level, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, level, tooltip, flag);
      tooltip.add(Component.translatable("tooltip.scguns.exosuit.frame"));
      if (ExoSuitData.hasUpgrades(stack)) {
         tooltip.add(Component.translatable("tooltip.scguns.exosuit.has_upgrades"));
         int upgradeCount = this.getCurrentUpgradeCount(stack);
         int maxSlots = this.getMaxUpgradeSlots();
         tooltip.add(Component.translatable("tooltip.scguns.exosuit.slots_used", new Object[]{upgradeCount, maxSlots}).withStyle(ChatFormatting.GRAY));
         this.addPowerCoreInfo(stack, tooltip);
         this.addPouchInfo(stack, tooltip);
         this.addUpgradeDurabilityInfo(stack, tooltip);
      } else {
         tooltip.add(Component.translatable("tooltip.scguns.exosuit.no_upgrades"));
      }

      this.addSlotInformation(tooltip);
      tooltip.add(
         Component.translatable("info.scguns.exosuit_help", new Object[]{KeyBinds.KEY_ATTACHMENTS.getTranslatedKeyMessage().getString().toUpperCase(Locale.ENGLISH)})
            .withStyle(ChatFormatting.YELLOW)
      );
   }

   private void addPowerCoreInfo(ItemStack stack, List<Component> tooltip) {
      if (this.getType() == Type.CHESTPLATE) {
         ItemStack powerCore = this.findPowerCore(stack);
         if (!powerCore.isEmpty()) {
            int energyStored = Caps.energyStored(powerCore, 0);
            int maxEnergy = Caps.maxEnergyStored(powerCore, 0);
            if (maxEnergy > 0) {
               int energyPercent = energyStored * 100 / maxEnergy;
               ChatFormatting energyColor = this.getEnergyColor(energyPercent);
               tooltip.add(Component.literal(""));
               tooltip.add(Component.translatable("tooltip.scguns.exosuit.power_core").withStyle(ChatFormatting.YELLOW));
               tooltip.add(Component.translatable("tooltip.scguns.exosuit.energy_level", new Object[]{energyPercent}).withStyle(energyColor));
            }
         }
      }
   }

   private ItemStack findPowerCore(ItemStack stack) {
      for (int slot = 0; slot < this.getMaxUpgradeSlots(); slot++) {
         ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(stack, slot);
         if (!upgradeItem.isEmpty()) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
            if (upgrade != null && upgrade.getType().equals("power_core")) {
               return upgradeItem;
            }
         }
      }

      return ItemStack.EMPTY;
   }

   private ChatFormatting getEnergyColor(int energyPercent) {
      if (energyPercent > 75) {
         return ChatFormatting.GREEN;
      } else if (energyPercent > 50) {
         return ChatFormatting.YELLOW;
      } else if (energyPercent > 25) {
         return ChatFormatting.GOLD;
      } else {
         return energyPercent > 0 ? ChatFormatting.RED : ChatFormatting.DARK_RED;
      }
   }

   private void addUpgradeDurabilityInfo(ItemStack stack, List<Component> tooltip) {
      boolean hasAnyDamageableUpgrades = false;

      for (int slot = 0; slot < this.getMaxUpgradeSlots(); slot++) {
         ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(stack, slot);
         if (!upgradeItem.isEmpty()) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
            if (upgrade != null && this.isUpgradeDamageable(upgradeItem)) {
               if (!hasAnyDamageableUpgrades) {
                  tooltip.add(Component.literal(""));
                  tooltip.add(Component.translatable("tooltip.scguns.exosuit.upgrade_condition").withStyle(ChatFormatting.YELLOW));
                  hasAnyDamageableUpgrades = true;
               }

               String upgradeName = this.getUpgradeDisplayName(upgrade, upgradeItem);
               int durabilityPercent = this.getDurabilityPercentage(upgradeItem);
               ChatFormatting color = this.getDurabilityColor(durabilityPercent);
               tooltip.add(Component.translatable("tooltip.scguns.exosuit.upgrade_durability", new Object[]{upgradeName, durabilityPercent}).withStyle(color));
            }
         }
      }
   }

   private String getUpgradeDisplayName(ExoSuitUpgrade upgrade, ItemStack upgradeItem) {
      String upgradeType = upgrade.getType();
      if ("hud".equals(upgradeType)) {
         if (upgradeItem.getItem() instanceof NightVisionModuleItem) {
            return Component.translatable("upgrade.scguns.exosuit.night_vision").getString();
         } else if (upgradeItem.getItem() instanceof TargetTrackerModuleItem) {
            return Component.translatable("upgrade.scguns.exosuit.target_tracker").getString();
         } else if (upgradeItem.getItem() instanceof GasMaskModuleItem) {
            return Component.translatable("upgrade.scguns.exosuit.gas_mask").getString();
         } else {
            return upgradeItem.getItem() instanceof RebreatherModuleItem
               ? Component.translatable("upgrade.scguns.exosuit.rebreather").getString()
               : Component.translatable("upgrade.scguns.exosuit.hud_system").getString();
         }
      } else if ("breathing".equals(upgradeType)) {
         if (upgradeItem.getItem() instanceof GasMaskModuleItem) {
            return Component.translatable("upgrade.scguns.exosuit.gas_mask").getString();
         } else {
            return upgradeItem.getItem() instanceof RebreatherModuleItem
               ? Component.translatable("upgrade.scguns.exosuit.rebreather").getString()
               : Component.translatable("upgrade.scguns.exosuit.life_support").getString();
         }
      } else {
         return switch (upgradeType) {
            case "plating" -> Component.translatable("upgrade.scguns.exosuit.armor_plating").getString();
            case "pauldron" -> Component.translatable("upgrade.scguns.exosuit.pauldron").getString();
            case "power_core" -> Component.translatable("upgrade.scguns.exosuit.power_core").getString();
            case "utility" -> Component.translatable("upgrade.scguns.exosuit.utility_module").getString();
            case "knee_guard" -> Component.translatable("upgrade.scguns.exosuit.knee_guard").getString();
            case "mobility" -> Component.translatable("upgrade.scguns.exosuit.mobility_system").getString();
            default -> {
               String itemName = upgradeItem.getDisplayName().getString();
               if (itemName.startsWith("Heavy ")) {
                  itemName = itemName.substring(6);
               }

               yield itemName;
            }
         };
      }
   }

   private int getDurabilityPercentage(ItemStack upgradeItem) {
      if (!this.isUpgradeDamageable(upgradeItem)) {
         return 100;
      } else {
         int maxDamage = upgradeItem.getMaxDamage();
         int currentDamage = upgradeItem.getDamageValue();
         if (maxDamage <= 0) {
            return 100;
         } else {
            int remainingDurability = maxDamage - currentDamage;
            return Math.max(0, remainingDurability * 100 / maxDamage);
         }
      }
   }

   private ChatFormatting getDurabilityColor(int durabilityPercent) {
      if (durabilityPercent > 75) {
         return ChatFormatting.GREEN;
      } else if (durabilityPercent > 50) {
         return ChatFormatting.YELLOW;
      } else if (durabilityPercent > 25) {
         return ChatFormatting.GOLD;
      } else {
         return durabilityPercent > 0 ? ChatFormatting.RED : ChatFormatting.DARK_RED;
      }
   }

   private boolean isUpgradeDamageable(ItemStack upgradeItem) {
      if (upgradeItem.isEmpty()) {
         return false;
      } else if (upgradeItem.getItem() instanceof DamageableUpgradeItem) {
         return true;
      } else {
         return upgradeItem.isDamageableItem() ? true : upgradeItem.getMaxDamage() > 0;
      }
   }

   private void addPouchInfo(ItemStack stack, List<Component> tooltip) {
      if (this.getType() == Type.CHESTPLATE) {
         ItemStack pouchUpgrade = this.findPouchUpgrade(stack);
         if (!pouchUpgrade.isEmpty()) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(pouchUpgrade);
            if (upgrade != null) {
               tooltip.add(Component.literal(""));
               tooltip.add(Component.translatable("tooltip.scguns.exosuit.pouches").withStyle(ChatFormatting.YELLOW));
               String pouchName = pouchUpgrade.getDisplayName().getString();
               tooltip.add(Component.translatable("tooltip.scguns.exosuit.pouch_equipped", new Object[]{pouchName}).withStyle(ChatFormatting.GREEN));
               if (this.isPouchEmpty(stack, pouchUpgrade)) {
                  tooltip.add(Component.translatable("tooltip.scguns.exosuit.pouch_empty").withStyle(ChatFormatting.DARK_GREEN));
               } else {
                  tooltip.add(Component.translatable("tooltip.scguns.exosuit.pouch_has_items").withStyle(ChatFormatting.GOLD));
               }
            }
         }
      }
   }

   private ItemStack findPouchUpgrade(ItemStack stack) {
      for (int slot = 0; slot < this.getMaxUpgradeSlots(); slot++) {
         ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(stack, slot);
         if (!upgradeItem.isEmpty()) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
            if (upgrade != null && upgrade.getType().equals("pouches")) {
               return upgradeItem;
            }
         }
      }

      return ItemStack.EMPTY;
   }

   private boolean isPouchEmpty(ItemStack chestplate, ItemStack pouchUpgrade) {
      try {
         String pouchId = pouchUpgrade.getItem().toString();
         CompoundTag pouchData = NbtHelper.getOrCreateTag(chestplate).getCompound("PouchData");
         if (!pouchData.contains(pouchId)) {
            return true;
         } else {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(pouchUpgrade);
            if (upgrade == null) {
               return true;
            } else {
               ItemStackHandler handler = new ItemStackHandler(upgrade.getDisplay().getStorageSize());
               handler.deserializeNBT(net.minecraft.client.Minecraft.getInstance().level.registryAccess(), pouchData.getCompound(pouchId));

               for (int i = 0; i < handler.getSlots(); i++) {
                  if (!handler.getStackInSlot(i).isEmpty()) {
                     return false;
                  }
               }

               return true;
            }
         }
      } catch (Exception var8) {
         return true;
      }
   }

   private void addSlotInformation(List<Component> tooltip) {
      switch (this.getType()) {
         case HELMET:
            tooltip.add(Component.translatable("tooltip.scguns.exosuit.slots.helmet"));
            break;
         case CHESTPLATE:
            tooltip.add(Component.translatable("tooltip.scguns.exosuit.slots.chest"));
            break;
         case LEGGINGS:
            tooltip.add(Component.translatable("tooltip.scguns.exosuit.slots.legs"));
            break;
         case BOOTS:
            tooltip.add(Component.translatable("tooltip.scguns.exosuit.slots.boots"));
      }
   }

   public int getMaxUpgradeSlots() {
      return switch (this.getType()) {
         case HELMET -> 3;
         case CHESTPLATE -> 4;
         case LEGGINGS -> 3;
         case BOOTS -> 2;
         default -> throw new IncompatibleClassChangeError();
      };
   }

   public int getCurrentUpgradeCount(ItemStack stack) {
      CompoundTag upgradeData = ExoSuitData.getUpgradeData(stack);
      if (upgradeData.contains("Upgrades")) {
         ListTag upgradeList = upgradeData.getList("Upgrades", 10);
         return upgradeList.size();
      } else {
         return 0;
      }
   }

   private PlayState predicate(AnimationState animationState) {
      animationState.getController().setAnimation(RawAnimation.begin().then("animation.exo_suit.idle", LoopType.LOOP));
      return PlayState.CONTINUE;
   }

   public void registerControllers(ControllerRegistrar controllerRegistrar) {
      controllerRegistrar.add(new AnimationController[]{new AnimationController(this, "controller", 0, this::predicate)});
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   public static record ExoSuitMenuProvider(InteractionHand hand) implements MenuProvider {
      public ExoSuitMenuProvider(InteractionHand hand) {
         this.hand = hand;
      }

      @NotNull
      public Component getDisplayName() {
         return Component.translatable("container.scguns.exosuit");
      }

      public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
         return new ExoSuitMenu(id, playerInventory, this.hand);
      }
   }
}
