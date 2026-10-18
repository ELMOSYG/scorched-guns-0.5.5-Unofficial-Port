package top.ribs.scguns.item;





import top.ribs.scguns.util.ScEnchants;
import net.minecraft.core.Holder;
import top.ribs.scguns.util.NbtHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import top.ribs.scguns.Config;
import top.ribs.scguns.client.GunItemStackRenderer;
import top.ribs.scguns.client.KeyBinds;
import top.ribs.scguns.common.FireMode;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.common.NetworkGunManager;
import top.ribs.scguns.common.ReloadType;
import top.ribs.scguns.init.ModEnchantments;
import top.ribs.scguns.init.ModItems;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.item.attachment.IAttachment;
import top.ribs.scguns.util.GunEnchantmentHelper;
import top.ribs.scguns.util.GunModifierHelper;

public class GunItem extends Item implements IColored, IMeta {
   // Keyed by the Gun compound only, not the whole stack tag: the tag also carries AmmoCount, so keying on all of it
   // invalidated the cache on every shot. A WeakHashMap made it worse still - NbtHelper.getTag returns a fresh tag
   // each call, so every entry was collectable immediately and nothing ever hit. With this key the definition is
   // reused until an attachment actually changes, instead of being deep-copied and re-parsed on every lookup.
   private final Map<Integer, Gun> modifiedGunCache = new HashMap<>();

   private static final int MODIFIED_GUN_CACHE_LIMIT = 64;
   private Gun gun = new Gun();

   private boolean reportedRepair;


   public GunItem(Properties properties) {
      super(properties);
   }

   public void setGun(NetworkGunManager.Supplier supplier) {
      this.gun = supplier.getGun();
   }

   public Gun getGun() {
      return this.gun;
   }

   @Override
   public void appendHoverText(ItemStack stack, Item.TooltipContext worldIn, List<Component> tooltip, TooltipFlag flag) {
      Gun modifiedGun = this.getModifiedGun(stack);
      ResourceLocation advantage = modifiedGun.getProjectile().getAdvantage();
      float baseDamage = modifiedGun.getProjectile().getDamage();
      baseDamage = GunModifierHelper.getModifiedProjectileDamage(stack, baseDamage);
      baseDamage = GunEnchantmentHelper.getAcceleratorDamage(stack, baseDamage);
      baseDamage = GunEnchantmentHelper.getHeavyShotDamage(stack, baseDamage);
      baseDamage *= ((Double)Config.COMMON.gameplay.globalDamageMultiplier.get()).floatValue();
      String additionalDamageText = "";
      CompoundTag tagCompound = NbtHelper.getTag(stack);
      if (tagCompound != null && tagCompound.contains("AdditionalDamage", 99)) {
         float additionalDamage = tagCompound.getFloat("AdditionalDamage");
         additionalDamage += GunModifierHelper.getAdditionalDamage(stack, false);
         if (additionalDamage > 0.0F) {
            additionalDamageText = ChatFormatting.GREEN + " +" + ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format((double)additionalDamage);
         } else if (additionalDamage < 0.0F) {
            additionalDamageText = ChatFormatting.RED + " " + ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format((double)additionalDamage);
         }
      }

      tooltip.add(
         Component.translatable("info.scguns.damage")
            .append(": ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format((double)baseDamage) + additionalDamageText).withStyle(ChatFormatting.WHITE))
      );
      float baseArmorPen = modifiedGun.getProjectile().getArmorPen();
      float puncturingPen = GunEnchantmentHelper.getPuncturingArmorBypass(stack);
      float totalArmorPen = baseArmorPen + puncturingPen;
      if (totalArmorPen > 0.0F) {
         String armorPenText = ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format((double)totalArmorPen);
         tooltip.add(
            Component.translatable("info.scguns.armor_penetration")
               .append(": ")
               .withStyle(ChatFormatting.GRAY)
               .append(Component.literal(armorPenText).withStyle(ChatFormatting.YELLOW))
         );
      }

      if (!advantage.equals(ModTags.Entities.NONE.location())) {
         tooltip.add(
            Component.translatable("info.scguns.advantage")
               .withStyle(ChatFormatting.GRAY)
               .append(Component.translatable("advantage." + advantage).withStyle(ChatFormatting.GOLD))
         );
      }

      String fireMode = modifiedGun.getGeneral().getFireMode().id().toString();
      tooltip.add(
         Component.translatable("info.scguns.fire_mode")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.translatable("fire_mode." + fireMode).withStyle(ChatFormatting.WHITE))
      );
      Item ammo = modifiedGun.getProjectile().getItem();
      Item reloadItem = modifiedGun.getReloads().getReloadItem();
      if (modifiedGun.getReloads().getReloadType() == ReloadType.SINGLE_ITEM) {
         ammo = reloadItem;
      }

      if (tagCompound != null) {
         if (tagCompound.getBoolean("IgnoreAmmo")) {
            tooltip.add(Component.translatable("info.scguns.ignore_ammo").withStyle(ChatFormatting.AQUA));
         } else {
            int ammoCount = tagCompound.getInt("AmmoCount");
            tooltip.add(
               Component.translatable("info.scguns.ammo")
                  .append(": ")
                  .withStyle(ChatFormatting.GRAY)
                  .append(Component.literal(ammoCount + "/" + GunModifierHelper.getModifiedAmmoCapacity(stack, modifiedGun)).withStyle(ChatFormatting.WHITE))
            );
         }
      }

      float totalMeleeDamage = this.getTotalMeleeDamage(stack);
      if (totalMeleeDamage > 0.0F) {
         String meleeDamageText = (double)totalMeleeDamage % 1.0 == 0.0 ? String.format("%d", (int)totalMeleeDamage) : String.format("%.1f", totalMeleeDamage);
         tooltip.add(
            Component.translatable("info.scguns.melee_damage")
               .append(": ")
               .withStyle(ChatFormatting.GRAY)
               .append(Component.literal(meleeDamageText).withStyle(ChatFormatting.WHITE))
         );
      }

      ResourceLocation effectLocation = modifiedGun.getProjectile().getImpactEffect();
      if (effectLocation != null) {
         MobEffect effect = (MobEffect)BuiltInRegistries.MOB_EFFECT.get(effectLocation);
         if (effect != null) {
            tooltip.add(
               Component.translatable("info.scguns.impact_effect")
                  .withStyle(ChatFormatting.GRAY)
                  .append(": ")
                  .append(Component.translatable(effect.getDescriptionId()).withStyle(ChatFormatting.BLUE))
            );
         }
      }

      Gun.WeaponType weaponType = modifiedGun.getGeneral().getWeaponType();
      if (weaponType != null) {
         String weaponTypeKey = "desc.scguns." + weaponType.name().toLowerCase();
         tooltip.add(
            Component.translatable("info.scguns.weapon_type")
               .withStyle(ChatFormatting.GRAY)
               .append(": ")
               .append(Component.translatable(weaponTypeKey).withStyle(ChatFormatting.AQUA))
         );
      }

      if (ammo != null) {
         tooltip.add(
            Component.translatable("info.scguns.ammo_type", new Object[]{Component.translatable(ammo.getDescriptionId()).withStyle(ChatFormatting.WHITE)})
               .withStyle(ChatFormatting.GRAY)
         );
      }

      if (modifiedGun.getGeneral().allowsAmmoChange() && !modifiedGun.getGeneral().getAvailableAmmoTypes().isEmpty()) {
         if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("info.scguns.available_ammo_types").withStyle(ChatFormatting.GRAY));
            int currentIndex = modifiedGun.getGeneral().getCurrentAmmoTypeIndex();
            List<String> ammoTypes = modifiedGun.getGeneral().getAvailableAmmoTypes();

            for (int i = 0; i < ammoTypes.size(); i++) {
               String ammoType = ammoTypes.get(i);
               ResourceLocation ammoLocation = ResourceLocation.parse(ammoType.contains(":") ? ammoType : "scguns:" + ammoType);
               Item ammoItem = (Item)BuiltInRegistries.ITEM.get(ammoLocation);
               if (ammoItem != null) {
                  Component ammoName = Component.translatable(ammoItem.getDescriptionId());
                  if (i == currentIndex) {
                     tooltip.add(
                        Component.literal("  • ")
                           .withStyle(ChatFormatting.YELLOW)
                           .append(ammoName.copy().withStyle(ChatFormatting.YELLOW))
                           .append(Component.literal(" ✓").withStyle(ChatFormatting.GREEN))
                     );
                  } else {
                     tooltip.add(Component.literal("  • ").withStyle(ChatFormatting.GRAY).append(ammoName.copy().withStyle(ChatFormatting.WHITE)));
                  }
               }
            }
         } else {
            tooltip.add(Component.translatable("info.scguns.ammo_swap_available").withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.ITALIC}));
         }
      }

      tooltip.add(
         Component.translatable("info.scguns.attachment_help", new Object[]{KeyBinds.KEY_ATTACHMENTS.getTranslatedKeyMessage().getString().toUpperCase(Locale.ENGLISH)})
            .withStyle(ChatFormatting.YELLOW)
      );
   }

   public float getBayonetAdditionalDamage(ItemStack gunStack) {
      float additionalDamage = 0.0F;

      for (IAttachment.Type type : IAttachment.Type.values()) {
         ItemStack attachmentStack = Gun.getAttachment(type, gunStack);
         if (attachmentStack != null && attachmentStack.getItem() instanceof BayonetItem) {
            additionalDamage += ((BayonetItem)attachmentStack.getItem()).getAdditionalDamage();
         }
      }

      return additionalDamage;
   }

   public float getTotalMeleeDamage(ItemStack stack) {
      Gun gun = this.getModifiedGun(stack);
      float baseMeleeDamage = gun.getGeneral().getMeleeDamage();
      float bayonetDamage = this.getBayonetAdditionalDamage(stack);
      return baseMeleeDamage + bayonetDamage;
   }

   public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
      return true;
   }

   @Override
   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      Gun gun = ((GunItem)stack.getItem()).getModifiedGun(stack);
      return gun.getGeneral().getRate() * 4;
   }

   public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
      return slotChanged;
   }

   public boolean isBarVisible(ItemStack stack) {
      return stack.isDamaged();
   }

   public int getBarWidth(ItemStack stack) {
      NbtHelper.getOrCreateTag(stack);
      this.getModifiedGun(stack);
      return Math.round(13.0F - (float)stack.getDamageValue() * 13.0F / (float)this.getMaxDamage(stack));
   }

   public int getBarColor(ItemStack stack) {
      if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
         return 8421504;
      } else if ((double)stack.getDamageValue() >= (double)stack.getMaxDamage() / 1.5) {
         return Objects.requireNonNull(ChatFormatting.RED.getColor());
      } else {
         float stackMaxDamage = (float)this.getMaxDamage(stack);
         float f = Math.max(0.0F, (stackMaxDamage - (float)stack.getDamageValue()) / stackMaxDamage);
         return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
      }
   }

      public Gun getModifiedGun(ItemStack stack) {
      CompoundTag tagCompound = NbtHelper.getTag(stack);
      if (tagCompound == null || !tagCompound.contains("Gun", 10)) {
         return this.gun;
      }

      CompoundTag gunTag = tagCompound.getCompound("Gun");
      boolean custom = tagCompound.getBoolean("Custom");
      int cacheKey = 31 * gunTag.hashCode() + (custom ? 1 : 0);
      Gun stored = this.modifiedGunCache.get(cacheKey);
      if (stored == null) {
         if (custom) {
            stored = Gun.create(gunTag);
         } else {
            stored = this.gun.copy();
            stored.deserializeNBT(gunTag);
         }

         if (this.modifiedGunCache.size() >= MODIFIED_GUN_CACHE_LIMIT) {
            this.modifiedGunCache.clear();
         }

         this.modifiedGunCache.put(cacheKey, stored);
      }

      // Repair on read: a definition that came back without usable data is replaced by this gun's own one for
      // the rest of the read, so a stack whose NBT holds a broken "Gun" compound still works. Nothing is written
      // back here - this runs on both sides and on every lookup - the next legitimate write replaces the tag.
      if (!this.hasUsableDefinition(stored)) {
         this.reportRepairedDefinition(stack);
         // The stack may also carry an ammo count that was written against the unusable definition - the empty
         // one reports a default magazine - so clamp it to what this gun's own definition allows. Only written
         // when it is actually out of range, so this stays idempotent.
         int maxAmmo = this.gun == null || this.gun.getReloads() == null ? 0 : this.gun.getReloads().getMaxAmmo();
         if (maxAmmo > 0 && tagCompound.getInt("AmmoCount") > maxAmmo) {
            // Write through a write accessor, bound and used in the same statement: tagCompound came from
            // getTag() and is read-only (mutating it would not mark the stack dirty, so the change would never
            // reach the client), and a tag bound from getOrCreateTag() goes stale the moment anything else
            // re-detaches this stack's component.
            NbtHelper.getOrCreateTag(stack).putInt("AmmoCount", maxAmmo);
         }

         return this.gun;
      }

      return stored;
   }

   /**
    * Whether a gun definition read out of an item's NBT can actually be used.
    *
    * <p>The failure this guards against showed up as every gun reading its projectile as air with the default
    * 30 round magazine: an empty definition, which is what a definition serialised while it held a reference
    * back to its own Gun looked like once it was read again. A definition is treated as unusable when it is
    * missing entirely, when it has no projectile where this gun's own definition has one, or when that
    * projectile resolves to air.</p>
    */
   private boolean hasUsableDefinition(Gun candidate) {
      if (candidate == null || candidate.getGeneral() == null || candidate.getProjectile() == null) {
         return false;
      }

      Item storedAmmo = candidate.getProjectile().getItem();
      Gun stock = this.gun;
      Item stockAmmo = stock == null || stock.getProjectile() == null ? null : stock.getProjectile().getItem();
      if (stockAmmo == null || stockAmmo == Items.AIR) {
         // This gun has no ammo of its own to compare against, so accept whatever was stored.
         return true;
      }

      return storedAmmo != null && storedAmmo != Items.AIR;
   }

   /** Says so once per item type, so a repaired stack is visible in the log without flooding it. */
   private void reportRepairedDefinition(ItemStack stack) {
      if (this.reportedRepair) {
         return;
      }

      this.reportedRepair = true;
      top.ribs.scguns.ScorchedGuns.LOGGER.warn(
         "Read an unusable gun definition from a {} stack and used the built-in definition instead",
         stack.getItem());
   }




   /**



    * Whether this gun accepts the given enchantment.



    *



    * <p>Vanilla 1.21.1 has no such method on {@code Item}; it exists in the other 1.21.1 Scorched Guns port and



    * addons mix into it there to narrow the enchantment list. tepox_scguns_enchantments injects into



    * GunItem.supportsEnchantment, and a missing target is a fatal mixin failure that leaves the mod in a



    * broken state and takes the whole game down with it, so the method has to exist here even though



    * nothing in this mod calls it.</p>



    */



   public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {



      return this.isEnchantable(stack);



   }




   public boolean isEnchantable(ItemStack stack) {
      return this.getMaxStackSize(stack) == 1;
   }

   public int getEnchantmentValue() {
      return 13;
   }

   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(new IClientItemExtensions() {
         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return new GunItemStackRenderer();
         }
      });
   }

   public boolean isFoil(ItemStack stack) {
      return false;
   }

   public boolean isValidRepairItem(ItemStack pToRepair, ItemStack pRepair) {
      return pRepair.is((Item)ModItems.REPAIR_KIT.get());
   }

   public ItemStack getAttachment(ItemStack heldItem, IAttachment.Type type) {
      return Gun.getAttachment(type, heldItem);
   }

   public boolean hasBayonet(ItemStack gunStack) {
      if (this.isBuiltInBayonetGun(gunStack)) {
         return true;
      } else {
         for (IAttachment.Type type : IAttachment.Type.values()) {
            ItemStack attachmentStack = Gun.getAttachment(type, gunStack);
            if (attachmentStack != null && attachmentStack.getItem() instanceof BayonetItem) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean isBuiltInBayonetGun(ItemStack gunStack) {
      return gunStack.is(ModTags.Items.BUILT_IN_BAYONET);
   }

   public boolean hasExtendedBarrel(ItemStack gunStack) {
      for (IAttachment.Type type : IAttachment.Type.values()) {
         ItemStack attachmentStack = Gun.getAttachment(type, gunStack);
         if (attachmentStack != null && attachmentStack.getItem() instanceof ExtendedBarrelItem) {
            return true;
         }
      }

      return false;
   }

   public boolean isOneHandedCarbineCandidate(ItemStack gunStack) {
      return gunStack.is(ModTags.Items.ONE_HANDED_CARBINE);
   }

   public boolean isOneHandedCarbineActive(ItemStack gunStack) {
      return this.isOneHandedCarbineCandidate(gunStack) && !Gun.hasExtendedBarrel(gunStack) && !Gun.hasStock(gunStack);
   }

   public void onAttachmentChanged(ItemStack stack) {
      CompoundTag tag = NbtHelper.getOrCreateTag(stack);
      tag.putBoolean("AttachmentChanged", true);
   }

   public int getBayonetBanzaiLevel(ItemStack gunStack) {
      for (IAttachment.Type type : IAttachment.Type.values()) {
         ItemStack attachmentStack = Gun.getAttachment(type, gunStack);
         if (attachmentStack != null && attachmentStack.getItem() instanceof BayonetItem) {
            Map<Holder<Enchantment>, Integer> enchantments = ScEnchants.getEnchantments(attachmentStack);
            if (ScEnchants.contains(enchantments, ModEnchantments.BANZAI)) {
               return ScEnchants.get(enchantments, ModEnchantments.BANZAI);
            }
         }
      }

      return 0;
   }

   public Gun getGunProperties() {
      return this.gun;
   }

   public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
      return stack.is(ModTags.Items.MINING_GUN) ? true : super.isBookEnchantable(stack, book);
   }
}
