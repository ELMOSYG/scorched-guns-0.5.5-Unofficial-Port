package top.ribs.scguns.common;






import top.ribs.scguns.util.ScEnchants;
import top.ribs.scguns.util.DistHelper;
import top.ribs.scguns.util.NbtHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.apache.commons.lang3.tuple.Pair;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.annotation.Ignored;
import top.ribs.scguns.annotation.Optional;
import top.ribs.scguns.client.ClientHandler;
import top.ribs.scguns.common.exosuit.ExoSuitAmmoHelper;
import top.ribs.scguns.debug.Debug;
import top.ribs.scguns.debug.IDebugWidget;
import top.ribs.scguns.debug.IEditorMenu;
import top.ribs.scguns.debug.client.screen.widget.DebugButton;
import top.ribs.scguns.debug.client.screen.widget.DebugSlider;
import top.ribs.scguns.debug.client.screen.widget.DebugToggle;
import top.ribs.scguns.init.ModEnchantments;
import top.ribs.scguns.item.AmmoBoxItem;
import top.ribs.scguns.item.ExtendedBarrelItem;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.LaserSightItem;
import top.ribs.scguns.item.ScopeItem;
import top.ribs.scguns.item.StockItem;
import top.ribs.scguns.item.attachment.IAttachment;
import top.ribs.scguns.item.attachment.impl.Scope;
import top.ribs.scguns.util.GunJsonUtil;
import top.ribs.scguns.util.SuperBuilder;
import top.theillusivec4.curios.api.CuriosApi;

public class Gun implements IEditorMenu {
   protected Gun.General general = new Gun.General();
   protected Gun.Reloads reloads = new Gun.Reloads();
   protected Gun.Projectile projectile = new Gun.Projectile();
   protected List<Gun.Projectile> alternateProjectiles = new ArrayList<>();
   protected Gun.Sounds sounds = new Gun.Sounds();
   protected Gun.Display display = new Gun.Display();
   protected Gun.Modules modules = new Gun.Modules();
   private final GripType baseGripType = GripType.ONE_HANDED;

   public Gun() {
      super();
   }

   public double getIdealAttackRange() {
      return switch (this.general.getWeaponType()) {
         case pistol, magnum, smg, shotgun, flamethrower -> 12.0;
         case rifle, lmg, plasma, shock, heavy, special -> 18.0;
         case laser, sniper -> 30.0;
      };
   }

   public double getMinAttackRange() {
      return switch (this.general.getWeaponType()) {
         case pistol, magnum, smg -> 2.0;
         case shotgun, flamethrower -> 1.0;
         case rifle, lmg, plasma, shock, heavy, special -> 2.5;
         case laser, sniper -> 3.0;
      };
   }

   public static int getMaxAmmo(ItemStack stack) {
      return ((GunItem)stack.getItem()).getModifiedGun(stack).getReloads().getMaxAmmo();
   }

   public static int getAmmoCount(ItemStack stack) {
      return NbtHelper.getOrCreateTag(stack).getInt("AmmoCount");
   }

   public static boolean hasAmmo(ItemStack heldItem) {
      return getAmmoCount(heldItem) > 0;
   }

   public static boolean hasUnlimitedReloads(ItemStack heldItem) {
      return ((GunItem)heldItem.getItem()).getModifiedGun(heldItem).getReloads().getInfiniteAmmo();
   }

   public static int getBurstCooldown(ItemStack stack) {
      return ((GunItem)stack.getItem()).getModifiedGun(stack).getGeneral().getBurstCooldown();
   }

   public GripType determineGripType(ItemStack stack) {
      GripType baseGripType = this.general.getBaseGripType();
      if (stack.getItem() instanceof GunItem gunItem && gunItem.isOneHandedCarbineCandidate(stack) && (hasExtendedBarrel(stack) || hasStock(stack))) {
         return GripType.TWO_HANDED;
      }

      return baseGripType;
   }

   public GripType getBaseGripType() {
      return this.baseGripType;
   }

   public static boolean hasExtendedBarrel(ItemStack stack) {
      for (IAttachment.Type type : IAttachment.Type.values()) {
         ItemStack attachmentStack = getAttachment(type, stack);
         if (attachmentStack.getItem() instanceof ExtendedBarrelItem) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasStock(ItemStack stack) {
      for (IAttachment.Type type : IAttachment.Type.values()) {
         ItemStack attachmentStack = getAttachment(type, stack);
         if (attachmentStack.getItem() instanceof StockItem) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasBurstFire(ItemStack stack) {
      return ((GunItem)stack.getItem()).getModifiedGun(stack).getGeneral().getFireMode() == FireMode.BURST;
   }

   public static boolean canShoot(ItemStack heldItem) {
      return !hasBurstFire(heldItem) || getFireTimer(heldItem) == 0;
   }

   public static int getBurstCount(ItemStack heldItem) {
      return ((GunItem)heldItem.getItem()).getModifiedGun(heldItem).getGeneral().getBurstAmount();
   }

   public Gun.General getGeneral() {
      return this.general;
   }

   public Gun.Reloads getReloads() {
      return this.reloads;
   }

   public Gun.Projectile getProjectile() {
      if (this.general.allowsAmmoChange() && !this.alternateProjectiles.isEmpty()) {
         int currentIndex = this.general.getCurrentAmmoTypeIndex();
         if (currentIndex > 0 && currentIndex <= this.alternateProjectiles.size()) {
            return this.alternateProjectiles.get(currentIndex - 1);
         }
      }

      return this.projectile;
   }

   public Gun.Projectile getProjectile(ItemStack gunStack) {
      if (this.general.allowsAmmoChange() && !this.alternateProjectiles.isEmpty()) {
         CompoundTag tag = NbtHelper.getOrCreateTag(gunStack);
         if (tag.contains("Gun", 10)) {
            CompoundTag gunTag = tag.getCompound("Gun");
            if (gunTag.contains("General", 10)) {
               CompoundTag generalTag = gunTag.getCompound("General");
               if (generalTag.contains("CurrentAmmoTypeIndex", 99)) {
                  int currentIndex = generalTag.getInt("CurrentAmmoTypeIndex");
                  if (currentIndex > 0 && currentIndex <= this.alternateProjectiles.size()) {
                     return this.alternateProjectiles.get(currentIndex - 1);
                  }
               }
            }
         }
      }

      return this.projectile;
   }

   public Item getCurrentAmmoItem() {
      return this.getProjectile().getItem();
   }

   public Item getCurrentAmmoItem(ItemStack gunStack) {
      return this.getProjectile(gunStack).getItem();
   }

   public Gun.Sounds getSounds() {
      return this.sounds;
   }

   public Gun.Display getDisplay() {
      return this.display;
   }

   public Gun.Modules getModules() {
      return this.modules;
   }

   @Override
   public Component getEditorLabel() {
      return Component.translatable("Gun");
   }

   @Override
   public void getEditorWidgets(List<Pair<Component, Supplier<IDebugWidget>>> widgets) {
      DistHelper.runWhenOn(Dist.CLIENT, () -> {
               ItemStack heldItem = Objects.requireNonNull(Minecraft.getInstance().player).getMainHandItem();
               ItemStack scope = getScopeStack(heldItem);
               if (scope.getItem() instanceof ScopeItem scopeItem) {
                  widgets.add(
                     Pair.of(
                        scope.getItem().getName(scope),
                        (Supplier<IDebugWidget>)() -> new DebugButton(
                              Component.translatable("Edit"), btn -> Minecraft.getInstance().setScreen(ClientHandler.createEditorScreen(Debug.getScope(scopeItem)))
                           )
                     )
                  );
               }

               widgets.add(
                  Pair.of(
                     this.modules.getEditorLabel(),
                     (Supplier<IDebugWidget>)() -> new DebugButton(
                           Component.translatable(">"), btn -> Minecraft.getInstance().setScreen(ClientHandler.createEditorScreen(this.modules))
                        )
                  )
               );
            }
      );
   }

   public static int getFireTimer(ItemStack stack) {
      return NbtHelper.getOrCreateTag(stack).getInt("FireTimer");
   }

   public CompoundTag serializeNBT() {
      CompoundTag tag = new CompoundTag();
      tag.put("General", this.general.serializeNBT());
      tag.put("Reloads", this.reloads.serializeNBT());
      tag.put("Projectile", this.projectile.serializeNBT());
      if (!this.alternateProjectiles.isEmpty()) {
         ListTag alternateList = new ListTag();

         for (Gun.Projectile altProjectile : this.alternateProjectiles) {
            alternateList.add(altProjectile.serializeNBT());
         }

         tag.put("AlternateProjectiles", alternateList);
      }

      tag.put("Sounds", this.sounds.serializeNBT());
      tag.put("Display", this.display.serializeNBT());
      tag.put("Modules", this.modules.serializeNBT());
      return tag;
   }

   public void deserializeNBT(CompoundTag tag) {
      if (tag.contains("General", 10)) {
         this.general.deserializeNBT(tag.getCompound("General"));
      }

      if (tag.contains("Reloads", 10)) {
         this.reloads.deserializeNBT(tag.getCompound("Reloads"));
      }

      if (tag.contains("Projectile", 10)) {
         this.projectile.deserializeNBT(tag.getCompound("Projectile"));
      }

      if (tag.contains("AlternateProjectiles", 9)) {
         this.alternateProjectiles.clear();
         ListTag alternateList = tag.getList("AlternateProjectiles", 10);

         for (int i = 0; i < alternateList.size(); i++) {
            Gun.Projectile altProjectile = new Gun.Projectile();
            altProjectile.deserializeNBT(alternateList.getCompound(i));
            this.alternateProjectiles.add(altProjectile);
         }
      }

      if (tag.contains("Sounds", 10)) {
         this.sounds.deserializeNBT(tag.getCompound("Sounds"));
      }

      if (tag.contains("Display", 10)) {
         this.display.deserializeNBT(tag.getCompound("Display"));
      }

      if (tag.contains("Modules", 10)) {
         this.modules.deserializeNBT(tag.getCompound("Modules"));
      }
   }

   public JsonObject toJsonObject() {
      JsonObject object = new JsonObject();
      object.add("general", this.general.toJsonObject());
      object.add("reloads", this.reloads.toJsonObject());
      object.add("projectile", this.projectile.toJsonObject());

      for (int i = 0; i < this.alternateProjectiles.size(); i++) {
         object.add("projectile_" + (i + 1), this.alternateProjectiles.get(i).toJsonObject());
      }

      GunJsonUtil.addObjectIfNotEmpty(object, "sounds", this.sounds.toJsonObject());
      GunJsonUtil.addObjectIfNotEmpty(object, "display", this.display.toJsonObject());
      GunJsonUtil.addObjectIfNotEmpty(object, "modules", this.modules.toJsonObject());
      return object;
   }

   public void loadAlternateProjectilesFromJson(JsonObject jsonObject) {
      this.alternateProjectiles.clear();
      if (this.general.allowsAmmoChange()) {
         this.general.getAvailableAmmoTypes().clear();
         if (this.projectile.item != null) {
            this.general.getAvailableAmmoTypes().add(this.projectile.item.toString());
         }
      }

      int index = 1;

      while (jsonObject.has("projectile_" + index)) {
         try {
            JsonObject projJson = jsonObject.getAsJsonObject("projectile_" + index);
            Gun.Projectile altProjectile = new Gun.Projectile();
            CompoundTag nbt = new CompoundTag();
            projJson.entrySet().forEach(entry -> {
               String jsonKey = (String)entry.getKey();
               String nbtKey = Character.toUpperCase(jsonKey.charAt(0)) + jsonKey.substring(1);
               JsonElement value = (JsonElement)entry.getValue();
               if (value.isJsonPrimitive()) {
                  JsonPrimitive prim = value.getAsJsonPrimitive();
                  if (prim.isString()) {
                     String stringValue = prim.getAsString();
                     if (jsonKey.equals("item") && !stringValue.contains(":")) {
                        stringValue = "scguns:" + stringValue;
                     }

                     nbt.putString(nbtKey, stringValue);
                  } else if (prim.isNumber()) {
                     if (prim.getAsString().contains(".")) {
                        nbt.putDouble(nbtKey, prim.getAsDouble());
                     } else {
                        nbt.putInt(nbtKey, prim.getAsInt());
                     }
                  } else if (prim.isBoolean()) {
                     nbt.putBoolean(nbtKey, prim.getAsBoolean());
                  }
               }
            });
            altProjectile.deserializeNBT(nbt);
            this.alternateProjectiles.add(altProjectile);
            if (this.general.allowsAmmoChange() && altProjectile.item != null) {
               this.general.getAvailableAmmoTypes().add(altProjectile.item.toString());
            }

            index++;
         } catch (Exception var6) {
            ScorchedGuns.LOGGER.error("Failed to load projectile_{}: {}", index, var6.getMessage());
            var6.printStackTrace();
            break;
         }
      }
   }

   public static Gun create(CompoundTag tag) {
      Gun gun = new Gun();
      gun.deserializeNBT(tag);
      if (tag.contains("MeleeDamage", 99)) {
         gun.getGeneral().setMeleeDamage(tag.getFloat("MeleeDamage"));
      }

      return gun;
   }

   public Gun copy() {
      Gun gun = new Gun();
      gun.general = this.general.copy();
      gun.reloads = this.reloads.copy();
      gun.projectile = this.projectile.copy();

      for (Gun.Projectile altProjectile : this.alternateProjectiles) {
         gun.alternateProjectiles.add(altProjectile.copy());
      }

      gun.sounds = this.sounds.copy();
      gun.display = this.display.copy();
      gun.modules = this.modules.copy();
      return gun;
   }

   public boolean canAttachType(@Nullable IAttachment.Type type) {
      if (this.modules.attachments != null && type != null) {
         return switch (type) {
            case SCOPE -> this.modules.attachments.scope != null;
            case BARREL -> this.modules.attachments.barrel != null;
            case STOCK -> this.modules.attachments.stock != null;
            case UNDER_BARREL -> this.modules.attachments.underBarrel != null;
            case MAGAZINE -> this.modules.attachments.magazine != null;
         };
      } else {
         return false;
      }
   }

   @Nullable
   public Gun.ScaledPositioned getAttachmentPosition(IAttachment.Type type) {
      if (this.modules.attachments != null) {
         return switch (type) {
            case SCOPE -> this.modules.attachments.scope;
            case BARREL -> this.modules.attachments.barrel;
            case STOCK -> this.modules.attachments.stock;
            case UNDER_BARREL -> this.modules.attachments.underBarrel;
            case MAGAZINE -> this.modules.attachments.magazine;
         };
      } else {
         return null;
      }
   }

   public boolean canAimDownSight() {
      return this.canAttachType(IAttachment.Type.SCOPE) || this.modules.zoom != null;
   }

   public static ItemStack getScopeStack(ItemStack gun) {
      CompoundTag compound = NbtHelper.getTag(gun);
      if (compound != null && compound.contains("Attachments", 10)) {
         CompoundTag attachment = compound.getCompound("Attachments");
         if (attachment.contains("Scope", 10)) {
            return top.ribs.scguns.util.NbtHelper.itemFromTag(attachment.getCompound("Scope"));
         }
      }

      return ItemStack.EMPTY;
   }

   public static boolean hasAttachmentEquipped(ItemStack stack, Gun gun, IAttachment.Type type) {
      if (!gun.canAttachType(type)) {
         return false;
      } else {
         CompoundTag compound = NbtHelper.getTag(stack);
         if (compound != null && compound.contains("Attachments", 10)) {
            CompoundTag attachment = compound.getCompound("Attachments");
            return attachment.contains(type.getTagKey(), 10);
         } else {
            return false;
         }
      }
   }

   public static boolean hasCustomAttachment(ItemStack stack, IAttachment.Type type, Item customAttachment) {
      CompoundTag compound = NbtHelper.getTag(stack);
      if (compound != null && compound.contains("Attachments", 10)) {
         CompoundTag attachment = compound.getCompound("Attachments");
         if (attachment.contains(type.getTagKey(), 10)) {
            ItemStack attachmentStack = top.ribs.scguns.util.NbtHelper.itemFromTag(attachment.getCompound(type.getTagKey()));
            return attachmentStack.getItem() == customAttachment;
         }
      }

      return false;
   }

   public static boolean hasAttachmentEquipped(ItemStack stack, IAttachment.Type type) {
      CompoundTag compound = NbtHelper.getTag(stack);
      if (compound != null && compound.contains("Attachments", 10)) {
         CompoundTag attachment = compound.getCompound("Attachments");
         return attachment.contains(type.getTagKey(), 10);
      } else {
         return false;
      }
   }

   public static ItemStack getAttachment(IAttachment.Type type, ItemStack gun) {
      CompoundTag compound = NbtHelper.getTag(gun);
      if (compound != null && compound.contains("Attachments", 10)) {
         CompoundTag attachment = compound.getCompound("Attachments");
         if (attachment.contains(type.getTagKey(), 10)) {
            return top.ribs.scguns.util.NbtHelper.itemFromTag(attachment.getCompound(type.getTagKey()));
         }
      }

      return ItemStack.EMPTY;
   }

   @Nullable
   public static Scope getScope(ItemStack gun) {
      CompoundTag compound = NbtHelper.getTag(gun);
      if (compound != null && compound.contains("Attachments", 10)) {
         CompoundTag attachment = compound.getCompound("Attachments");
         if (attachment.contains("Scope", 10)) {
            ItemStack scopeStack = top.ribs.scguns.util.NbtHelper.itemFromTag(attachment.getCompound("Scope"));
            Scope scope = null;
            if (scopeStack.getItem() instanceof ScopeItem scopeItem) {
               if (ScorchedGuns.isDebugging()) {
                  return Debug.getScope(scopeItem);
               }

               scope = scopeItem.getProperties();
            }

            return scope;
         }
      }

      return null;
   }

   public static boolean hasLaserSight(ItemStack gun) {
      return hasAttachmentEquipped(gun, IAttachment.Type.SCOPE) && getAttachment(IAttachment.Type.SCOPE, gun).getItem() instanceof LaserSightItem;
   }

   /**
    * Writes one attachment back into the gun.
    *
    * <p>0.5.5 had no such method, and that is why attachments never wore out: {@link #getAttachment}
    * decodes a <b>copy</b> out of the gun's tag, so the {@code hurtAndBreak} call in
    * {@code GunEventBus.damageAttachments} damaged that copy and threw it away, while its "would
    * break" branch could therefore only ever fire for a one-durability attachment (HANDOFF section
    * 60). The upstream 1.21.1 port carries the same fix in the same shape.</p>
    *
    * <p>The tag is taken through {@code NbtHelper.getOrCreateTag} so the write belongs to this stack
    * alone and is therefore visible to the item-sync comparison - see NbtHelper's class javadoc.</p>
    */
   public static void setAttachment(ItemStack gun, IAttachment.Type type, ItemStack attachment,
                                    net.minecraft.core.HolderLookup.Provider provider) {
      CompoundTag tag = NbtHelper.getOrCreateTag(gun);
      CompoundTag attachments = tag.contains("Attachments", 10) ? tag.getCompound("Attachments") : new CompoundTag();
      if (attachment.isEmpty()) {
         attachments.remove(type.getTagKey());
      } else {
         CompoundTag encoded = top.ribs.scguns.util.NbtHelper.tagFromItem(attachment, provider);
         if (encoded.isEmpty()) {
            // Never overwrite a real attachment with nothing: an encode failure means we could not
            // write it, not that the attachment is gone (HANDOFF sections 58 and 59).
            return;
         }

         attachments.put(type.getTagKey(), encoded);
      }

      tag.put("Attachments", attachments);
   }

   public static void removeAttachment(ItemStack gun, String attachmentStack) {
      CompoundTag compound = NbtHelper.getTagForWrite(gun);
      if (compound != null && compound.contains("Attachments", 10)) {
         CompoundTag attachment = compound.getCompound("Attachments");
         if (attachment.contains(attachmentStack, 10)) {
            attachment.remove(attachmentStack);
         }
      }
   }

   public static ItemStack removeScopeStack(ItemStack gun) {
      CompoundTag compound = NbtHelper.getTag(gun);
      if (compound != null && compound.contains("Attachments", 10)) {
         CompoundTag attachment = compound.getCompound("Attachments");
         if (attachment.contains("Scope", 10)) {
            attachment.remove("Scope");
         }
      }

      return ItemStack.EMPTY;
   }

   public static ItemStack removeBarrelStack(ItemStack gun) {
      CompoundTag compound = NbtHelper.getTag(gun);
      if (compound != null && compound.contains("Attachments", 10)) {
         CompoundTag attachment = compound.getCompound("Attachments");
         if (attachment.contains("Barrel", 10)) {
            attachment.remove("Barrel");
         }
      }

      return ItemStack.EMPTY;
   }

   public static ItemStack removeStockStack(ItemStack gun) {
      CompoundTag compound = NbtHelper.getTag(gun);
      if (compound != null && compound.contains("Attachments", 10)) {
         CompoundTag attachment = compound.getCompound("Attachments");
         if (attachment.contains("Stock", 10)) {
            attachment.remove("Stock");
         }
      }

      return ItemStack.EMPTY;
   }

   public static ItemStack removeUnderBarrelStack(ItemStack gun) {
      CompoundTag compound = NbtHelper.getTag(gun);
      if (compound != null && compound.contains("Attachments", 10)) {
         CompoundTag attachment = compound.getCompound("Attachments");
         if (attachment.contains("Under_Barrel", 10)) {
            attachment.remove("Under_Barrel");
         }
      }

      return ItemStack.EMPTY;
   }

   public static ItemStack removeMagazineStack(ItemStack gun) {
      CompoundTag compound = NbtHelper.getTag(gun);
      if (compound != null && compound.contains("Attachments", 10)) {
         CompoundTag attachment = compound.getCompound("Attachments");
         if (attachment.contains("Magazine", 10)) {
            attachment.remove("Magazine");
         }
      }

      return ItemStack.EMPTY;
   }

   public static float getAdditionalDamage(ItemStack gunStack) {
      CompoundTag tag = NbtHelper.getOrCreateTag(gunStack);
      return tag.getFloat("AdditionalDamage");
   }

   public static AmmoContext findAmmo(Player player, Item item) {
      if (player.isCreative()) {
         ItemStack ammo = new ItemStack(item, Integer.MAX_VALUE);
         return new AmmoContext(ammo, null);
      } else {
         for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isAmmo(stack, item)) {
               return new AmmoContext(stack, player.getInventory());
            }
         }

         for (ItemStack itemStack : player.getInventory().items) {
            Item contents = itemStack.getItem();
            if (contents instanceof AmmoBoxItem) {
               AmmoBoxItem pouch = (AmmoBoxItem)contents;

               for (ItemStack ammoStack : AmmoBoxItem.getContents(itemStack).toList()) {
                  if (isAmmo(ammoStack, item)) {
                     return new AmmoContext(ammoStack, null);
                  }
               }
            }
         }

         ItemStack exoSuitAmmo = ExoSuitAmmoHelper.findAmmoInExoSuit(player, item);
         if (!exoSuitAmmo.isEmpty()) {
            return new AmmoContext(exoSuitAmmo, null);
         } else {
            AtomicReference<AmmoContext> ammoContextRef = new AtomicReference<>(AmmoContext.NONE);
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
               IItemHandlerModifiable curios = handler.getEquippedCurios();

               for (int ix = 0; ix < curios.getSlots(); ix++) {
                  ItemStack stack = curios.getStackInSlot(ix);
                  Item patt115265$temp = stack.getItem();
                  if (patt115265$temp instanceof AmmoBoxItem) {
                     AmmoBoxItem pouchx = (AmmoBoxItem)patt115265$temp;

                     for (ItemStack ammoStackx : AmmoBoxItem.getContents(stack).toList()) {
                        if (isAmmo(ammoStackx, item)) {
                           ammoContextRef.set(new AmmoContext(ammoStackx, null));
                           return;
                        }
                     }
                  }
               }
            });
            return ammoContextRef.get();
         }
      }
   }

   public static boolean isAmmo(ItemStack stack, Item item) {
      return stack != null && stack.getItem() == item;
   }

   public static ItemStack[] findAmmoStack(Player player, Item item) {
      if (player.isCreative()) {
         return new ItemStack[]{new ItemStack(item, Integer.MAX_VALUE)};
      } else {
         List<ItemStack> ammoStacks = new ArrayList<>();

         for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
               ammoStacks.add(stack);
            }
         }

         for (ItemStack itemStack : player.getInventory().items) {
            Item contents = itemStack.getItem();
            if (contents instanceof AmmoBoxItem) {
               AmmoBoxItem pouch = (AmmoBoxItem)contents;

               for (ItemStack ammoStack : AmmoBoxItem.getContents(itemStack).toList()) {
                  if (ammoStack.getItem() == item) {
                     ammoStacks.add(ammoStack);
                  }
               }
            }
         }

         List<ItemStack> exoSuitAmmo = ExoSuitAmmoHelper.findAllAmmoInExoSuit(player, item);
         ammoStacks.addAll(exoSuitAmmo);
         CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            IItemHandlerModifiable curios = handler.getEquippedCurios();

            for (int i = 0; i < curios.getSlots(); i++) {
               ItemStack stackx = curios.getStackInSlot(i);
               if (stackx.getItem() instanceof AmmoBoxItem) {
                  for (ItemStack ammoStackx : AmmoBoxItem.getContents(stackx).toList()) {
                     if (ammoStackx.getItem() == item) {
                        ammoStacks.add(ammoStackx);
                     }
                  }
               }
            }
         });
         return ammoStacks.toArray(new ItemStack[0]);
      }
   }

   public static int getReserveAmmoCount(Player player, Item item) {
      if (player.isCreative()) {
         return Integer.MAX_VALUE;
      } else {
         AtomicInteger ammoCount = new AtomicInteger();

         for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isAmmo(stack, item)) {
               ammoCount.addAndGet(stack.getCount());
            }
         }

         for (ItemStack itemStack : player.getInventory().items) {
            Item contents = itemStack.getItem();
            if (contents instanceof AmmoBoxItem) {
               AmmoBoxItem pouch = (AmmoBoxItem)contents;

               for (ItemStack ammoStack : AmmoBoxItem.getContents(itemStack).toList()) {
                  if (isAmmo(ammoStack, item)) {
                     ammoCount.addAndGet(ammoStack.getCount());
                  }
               }
            }
         }

         int exoSuitAmmoCount = ExoSuitAmmoHelper.getAmmoCountInExoSuit(player, item);
         ammoCount.addAndGet(exoSuitAmmoCount);
         CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            IItemHandlerModifiable curios = handler.getEquippedCurios();

            for (int ix = 0; ix < curios.getSlots(); ix++) {
               ItemStack stack = curios.getStackInSlot(ix);
               Item patt119361$temp = stack.getItem();
               if (patt119361$temp instanceof AmmoBoxItem) {
                  AmmoBoxItem pouchx = (AmmoBoxItem)patt119361$temp;

                  for (ItemStack ammoStackx : AmmoBoxItem.getContents(stack).toList()) {
                     if (isAmmo(ammoStackx, item)) {
                        ammoCount.addAndGet(ammoStackx.getCount());
                     }
                  }
               }
            }
         });
         return ammoCount.get();
      }
   }

   public static float getFovModifier(ItemStack stack, Gun modifiedGun) {
      float modifier = 0.0F;
      if (hasAttachmentEquipped(stack, modifiedGun, IAttachment.Type.SCOPE)) {
         Scope scope = getScope(stack);
         if (scope != null && scope.getFovModifier() < 1.0F) {
            return Mth.clamp(scope.getFovModifier(), 0.01F, 1.0F);
         }
      }

      Gun.Modules.Zoom zoom = modifiedGun.getModules().getZoom();
      return zoom != null ? modifier + zoom.getFovModifier() : 0.0F;
   }

   public static class Builder {
      private final Gun gun = new Gun();

      private Builder() {
         super();
      }

      public static Gun.Builder create() {
         return new Gun.Builder();
      }

      public Gun build() {
         return this.gun.copy();
      }

      public Gun.Builder setFireMode(FireMode fireMode) {
         this.gun.general.fireMode = fireMode;
         return this;
      }

      public Gun.Builder setCasingType(Item item) {
         this.gun.projectile.casingType = BuiltInRegistries.ITEM.getKey(item);
         return this;
      }

      public Gun.Builder setBurstAmount(int burstAmount) {
         this.gun.general.burstAmount = burstAmount;
         return this;
      }

      public Gun.Builder setFireRate(int rate) {
         this.gun.general.rate = rate;
         return this;
      }

      public Gun.Builder setFireTimer(int fireTimer) {
         this.gun.general.fireTimer = fireTimer;
         return this;
      }

      public Gun.Builder setGripType(GripType gripType) {
         this.gun.general.gripType = gripType;
         return this;
      }

      public Gun.Builder setReloadItem(Item item) {
         this.gun.reloads.reloadItem = BuiltInRegistries.ITEM.getKey(item);
         return this;
      }

      public Gun.Builder setMaxAmmo(int maxAmmo) {
         this.gun.reloads.maxAmmo = maxAmmo;
         return this;
      }

      public Gun.Builder setReloadType(ReloadType reloadType) {
         this.gun.reloads.reloadType = reloadType;
         return this;
      }

      public Gun.Builder setReloadTimer(int reloadTimer) {
         this.gun.reloads.reloadTimer = reloadTimer;
         return this;
      }

      public Gun.Builder setEmptyMagTimer(int emptyMagTimer) {
         this.gun.reloads.emptyMagTimer = emptyMagTimer;
         return this;
      }

      public Gun.Builder setReloadAmount(int reloadAmount) {
         this.gun.reloads.reloadAmount = reloadAmount;
         return this;
      }

      public Gun.Builder setRecoilAngle(float recoilAngle) {
         this.gun.projectile.recoilAngle = recoilAngle;
         return this;
      }

      public Gun.Builder setRecoilKick(float recoilKick) {
         this.gun.projectile.recoilKick = recoilKick;
         return this;
      }

      public Gun.Builder setRecoilDurationOffset(float recoilDurationOffset) {
         this.gun.general.recoilDurationOffset = recoilDurationOffset;
         return this;
      }

      public Gun.Builder setRecoilAdsReduction(float recoilAdsReduction) {
         this.gun.general.recoilAdsReduction = recoilAdsReduction;
         return this;
      }

      public Gun.Builder setProjectileAmount(int projectileAmount) {
         this.gun.projectile.projectileAmount = projectileAmount;
         return this;
      }

      public Gun.Builder setAlwaysSpread(boolean alwaysSpread) {
         this.gun.projectile.alwaysSpread = alwaysSpread;
         return this;
      }

      public Gun.Builder setSpread(float spread) {
         this.gun.projectile.spread = spread;
         return this;
      }

      public Gun.Builder setRestingSpread(float restingSpread) {
         this.gun.general.restingSpread = restingSpread;
         return this;
      }

      public Gun.Builder setSpreadAdsReduction(float spreadAdsReduction) {
         this.gun.general.spreadAdsReduction = spreadAdsReduction;
         return this;
      }

      public Gun.Builder setAmmo(Item item) {
         this.gun.projectile.item = BuiltInRegistries.ITEM.getKey(item);
         return this;
      }

      public Gun.Builder setEjectsCasing(boolean ejectsCasing) {
         this.gun.projectile.ejectsCasing = ejectsCasing;
         return this;
      }

      public Gun.Builder setProjectileVisible(boolean visible) {
         this.gun.projectile.visible = visible;
         return this;
      }

      public Gun.Builder setProjectileSize(float size) {
         this.gun.projectile.size = size;
         return this;
      }

      public Gun.Builder setProjectileSpeed(double speed) {
         this.gun.projectile.speed = speed;
         return this;
      }

      public Gun.Builder setProjectileLife(int life) {
         this.gun.projectile.life = life;
         return this;
      }

      public Gun.Builder setProjectileAffectedByGravity(boolean gravity) {
         this.gun.projectile.gravity = gravity;
         return this;
      }

      public Gun.Builder setProjectileTrailColor(int trailColor) {
         this.gun.projectile.trailColor = trailColor;
         return this;
      }

      public Gun.Builder setProjectileTrailLengthMultiplier(int trailLengthMultiplier) {
         this.gun.projectile.trailLengthMultiplier = (double)trailLengthMultiplier;
         return this;
      }

      public Gun.Builder setDamage(float damage) {
         this.gun.projectile.damage = damage;
         return this;
      }

      public Gun.Builder setAdvantage(ResourceLocation advantage) {
         this.gun.projectile.advantage = advantage;
         return this;
      }

      public Gun.Builder setReduceDamageOverLife(boolean damageReduceOverLife) {
         this.gun.projectile.damageReduceOverLife = damageReduceOverLife;
         return this;
      }

      public Gun.Builder setFireSound(SoundEvent sound) {
         this.gun.sounds.fire = BuiltInRegistries.SOUND_EVENT.getKey(sound);
         return this;
      }

      public Gun.Builder setReloadSound(SoundEvent sound) {
         this.gun.sounds.reload = BuiltInRegistries.SOUND_EVENT.getKey(sound);
         return this;
      }

      public Gun.Builder setCockSound(SoundEvent sound) {
         this.gun.sounds.cock = BuiltInRegistries.SOUND_EVENT.getKey(sound);
         return this;
      }

      public Gun.Builder setSilencedFireSound(SoundEvent sound) {
         this.gun.sounds.silencedFire = BuiltInRegistries.SOUND_EVENT.getKey(sound);
         return this;
      }

      public Gun.Builder setEnchantedFireSound(SoundEvent sound) {
         this.gun.sounds.enchantedFire = BuiltInRegistries.SOUND_EVENT.getKey(sound);
         return this;
      }

      public Gun.Builder setPreFireSound(SoundEvent sound) {
         this.gun.sounds.preFire = BuiltInRegistries.SOUND_EVENT.getKey(sound);
         return this;
      }

      @Deprecated(
         since = "1.3.0",
         forRemoval = true
      )
      public Gun.Builder setMuzzleFlash(double size, double xOffset, double yOffset, double zOffset) {
         Gun.Display.Flash flash = new Gun.Display.Flash();
         flash.size = size;
         flash.xOffset = xOffset;
         flash.yOffset = yOffset;
         flash.zOffset = zOffset;
         this.gun.display.flash = flash;
         return this;
      }

      public Gun.Builder setZoom(float fovModifier, double xOffset, double yOffset, double zOffset) {
         Gun.Modules.Zoom zoom = new Gun.Modules.Zoom();
         zoom.fovModifier = fovModifier;
         zoom.xOffset = xOffset;
         zoom.yOffset = yOffset;
         zoom.zOffset = zOffset;
         this.gun.modules.zoom = zoom;
         return this;
      }

      @Deprecated(
         since = "1.3.0",
         forRemoval = true
      )
      public Gun.Builder setZoom(Gun.Modules.Zoom.Builder builder) {
         this.gun.modules.zoom = builder.build();
         return this;
      }

      @Deprecated(
         since = "1.3.0",
         forRemoval = true
      )
      public Gun.Builder setScope(float scale, double xOffset, double yOffset, double zOffset) {
         Gun.ScaledPositioned positioned = new Gun.ScaledPositioned();
         positioned.scale = (double)scale;
         positioned.xOffset = xOffset;
         positioned.yOffset = yOffset;
         positioned.zOffset = zOffset;
         this.gun.modules.attachments.scope = positioned;
         return this;
      }

      @Deprecated(
         since = "1.3.0",
         forRemoval = true
      )
      public Gun.Builder setBarrel(float scale, double xOffset, double yOffset, double zOffset) {
         Gun.ScaledPositioned positioned = new Gun.ScaledPositioned();
         positioned.scale = (double)scale;
         positioned.xOffset = xOffset;
         positioned.yOffset = yOffset;
         positioned.zOffset = zOffset;
         this.gun.modules.attachments.barrel = positioned;
         return this;
      }

      @Deprecated(
         since = "1.3.0",
         forRemoval = true
      )
      public Gun.Builder setStock(float scale, double xOffset, double yOffset, double zOffset) {
         Gun.ScaledPositioned positioned = new Gun.ScaledPositioned();
         positioned.scale = (double)scale;
         positioned.xOffset = xOffset;
         positioned.yOffset = yOffset;
         positioned.zOffset = zOffset;
         this.gun.modules.attachments.stock = positioned;
         return this;
      }

      @Deprecated(
         since = "1.3.0",
         forRemoval = true
      )
      public Gun.Builder setUnderBarrel(float scale, double xOffset, double yOffset, double zOffset) {
         Gun.ScaledPositioned positioned = new Gun.ScaledPositioned();
         positioned.scale = (double)scale;
         positioned.xOffset = xOffset;
         positioned.yOffset = yOffset;
         positioned.zOffset = zOffset;
         this.gun.modules.attachments.underBarrel = positioned;
         return this;
      }

      public Gun.Builder setMagazine(float scale, double xOffset, double yOffset, double zOffset) {
         Gun.ScaledPositioned positioned = new Gun.ScaledPositioned();
         positioned.scale = (double)scale;
         positioned.xOffset = xOffset;
         positioned.yOffset = yOffset;
         positioned.zOffset = zOffset;
         this.gun.modules.attachments.magazine = positioned;
         return this;
      }
   }

   public static class Display  {
      @Optional
      @Nullable
      protected Gun.Display.Flash flash;
      @Optional
      @Nullable
      protected Gun.Display.BeamOrigin beamOrigin;
      @Optional
      private String muzzleFlashType;

      public Display() {
         super();
      }

      @Nullable
      public Gun.Display.Flash getFlash() {
         return this.flash;
      }

      public String getMuzzleFlashType() {
         return this.muzzleFlashType;
      }

      public void setMuzzleFlashType(String muzzleFlashType) {
         this.muzzleFlashType = muzzleFlashType;
      }

      @Nullable
      public Gun.Display.BeamOrigin getBeamOrigin() {
         return this.beamOrigin;
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         if (this.flash != null) {
            tag.put("Flash", this.flash.serializeNBT());
         }

         if (this.beamOrigin != null) {
            tag.put("BeamOrigin", this.beamOrigin.serializeNBT());
         }

         if (this.muzzleFlashType != null) {
            tag.putString("MuzzleFlashType", this.muzzleFlashType);
         }

         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("Flash", 10)) {
            CompoundTag flashTag = tag.getCompound("Flash");
            if (!flashTag.isEmpty()) {
               Gun.Display.Flash flash = new Gun.Display.Flash();
               flash.deserializeNBT(flashTag);
               this.flash = flash;
            } else {
               this.flash = null;
            }
         }

         if (tag.contains("BeamOrigin", 10)) {
            CompoundTag originTag = tag.getCompound("BeamOrigin");
            if (!originTag.isEmpty()) {
               Gun.Display.BeamOrigin origin = new Gun.Display.BeamOrigin();
               origin.deserializeNBT(originTag);
               this.beamOrigin = origin;
            }
         }

         if (tag.contains("MuzzleFlashType", 8)) {
            this.muzzleFlashType = tag.getString("MuzzleFlashType");
         }
      }

      public JsonObject toJsonObject() {
         JsonObject object = new JsonObject();
         if (this.flash != null) {
            GunJsonUtil.addObjectIfNotEmpty(object, "flash", this.flash.toJsonObject());
         }

         if (this.beamOrigin != null) {
            GunJsonUtil.addObjectIfNotEmpty(object, "beamOrigin", this.beamOrigin.toJsonObject());
         }

         if (this.muzzleFlashType != null) {
            object.addProperty("muzzleFlashType", this.muzzleFlashType);
         }

         return object;
      }

      public Gun.Display copy() {
         Gun.Display display = new Gun.Display();
         if (this.flash != null) {
            display.flash = this.flash.copy();
         }

         display.muzzleFlashType = this.muzzleFlashType;
         return display;
      }

      public static class BeamOrigin extends Gun.Positioned {
         private double horizontalOffset = 0.1;
         private double verticalOffset = -0.1;
         private double forwardOffset = 0.3;
         private double aimHorizontalOffset = 0.0;

         public BeamOrigin() {
            super();
         }

         @Override
         public CompoundTag serializeNBT() {
            CompoundTag tag = super.serializeNBT();
            tag.putDouble("HorizontalOffset", this.horizontalOffset);
            tag.putDouble("VerticalOffset", this.verticalOffset);
            tag.putDouble("ForwardOffset", this.forwardOffset);
            tag.putDouble("AimHorizontalOffset", this.aimHorizontalOffset);
            return tag;
         }

         @Override
         public void deserializeNBT(CompoundTag tag) {
            super.deserializeNBT(tag);
            if (tag.contains("HorizontalOffset", 99)) {
               this.horizontalOffset = tag.getDouble("HorizontalOffset");
            }

            if (tag.contains("VerticalOffset", 99)) {
               this.verticalOffset = tag.getDouble("VerticalOffset");
            }

            if (tag.contains("ForwardOffset", 99)) {
               this.forwardOffset = tag.getDouble("ForwardOffset");
            }

            if (tag.contains("AimHorizontalOffset", 99)) {
               this.aimHorizontalOffset = tag.getDouble("AimHorizontalOffset");
            }
         }

         @Override
         public JsonObject toJsonObject() {
            JsonObject object = super.toJsonObject();
            if (this.horizontalOffset != 0.1) {
               object.addProperty("horizontalOffset", this.horizontalOffset);
            }

            if (this.verticalOffset != -0.1) {
               object.addProperty("verticalOffset", this.verticalOffset);
            }

            if (this.forwardOffset != 0.3) {
               object.addProperty("forwardOffset", this.forwardOffset);
            }

            if (this.aimHorizontalOffset != 0.0) {
               object.addProperty("aimHorizontalOffset", this.aimHorizontalOffset);
            }

            return object;
         }

         public Gun.Display.BeamOrigin copy() {
            Gun.Display.BeamOrigin origin = new Gun.Display.BeamOrigin();
            origin.horizontalOffset = this.horizontalOffset;
            origin.verticalOffset = this.verticalOffset;
            origin.forwardOffset = this.forwardOffset;
            origin.aimHorizontalOffset = this.aimHorizontalOffset;
            origin.xOffset = this.xOffset;
            origin.yOffset = this.yOffset;
            origin.zOffset = this.zOffset;
            return origin;
         }

         public double getHorizontalOffset() {
            return this.horizontalOffset;
         }

         public double getVerticalOffset() {
            return this.verticalOffset;
         }

         public double getForwardOffset() {
            return this.forwardOffset;
         }

         public double getAimHorizontalOffset() {
            return this.aimHorizontalOffset;
         }
      }

      public static class Flash extends Gun.Positioned {
         private double size = 0.5;
         private String textureLocation = "muzzle_flash_1";
         private boolean spawnParticles = false;
         private boolean alternateMuzzleFlash = false;
         private Vec3 alternatePosition = Vec3.ZERO;
         private int particleCount = 5;
         private String particleType = "minecraft:flame";
         private double particleSpread = 0.1;
         private double particleRingRadius = 0.0;

         public Flash() {
            super();
         }

         public Gun.Display.Flash copy() {
            Gun.Display.Flash flash = new Gun.Display.Flash();
            flash.xOffset = this.xOffset;
            flash.yOffset = this.yOffset;
            flash.zOffset = this.zOffset;
            flash.size = this.size;
            flash.textureLocation = this.textureLocation;
            flash.spawnParticles = this.spawnParticles;
            flash.alternateMuzzleFlash = this.alternateMuzzleFlash;
            flash.alternatePosition = this.alternatePosition;
            flash.particleCount = this.particleCount;
            flash.particleType = this.particleType;
            flash.particleSpread = this.particleSpread;
            flash.particleRingRadius = this.particleRingRadius;
            return flash;
         }

         @Override
         public CompoundTag serializeNBT() {
            CompoundTag tag = super.serializeNBT();
            tag.putDouble("Size", this.size);
            tag.putString("TextureLocation", this.textureLocation);
            tag.putBoolean("AlternateMuzzleFlash", this.alternateMuzzleFlash);
            CompoundTag altPos = new CompoundTag();
            altPos.putDouble("X", this.alternatePosition.x);
            altPos.putDouble("Y", this.alternatePosition.y);
            altPos.putDouble("Z", this.alternatePosition.z);
            tag.put("AlternatePosition", altPos);
            tag.putInt("ParticleCount", this.particleCount);
            tag.putString("ParticleType", this.particleType);
            tag.putDouble("ParticleSpread", this.particleSpread);
            tag.putDouble("ParticleRingRadius", this.particleRingRadius);
            tag.putBoolean("SpawnParticles", this.spawnParticles);
            return tag;
         }

         @Override
         public void deserializeNBT(CompoundTag tag) {
            super.deserializeNBT(tag);
            if (tag.contains("Size", 99)) {
               this.size = tag.getDouble("Size");
            }

            if (tag.contains("TextureLocation", 8)) {
               this.textureLocation = tag.getString("TextureLocation");
            }

            if (tag.contains("AlternateMuzzleFlash", 99)) {
               this.alternateMuzzleFlash = tag.getBoolean("AlternateMuzzleFlash");
            }

            if (tag.contains("AlternatePosition", 10)) {
               CompoundTag altPos = tag.getCompound("AlternatePosition");
               this.alternatePosition = new Vec3(altPos.getDouble("X"), altPos.getDouble("Y"), altPos.getDouble("Z"));
            }

            if (tag.contains("ParticleCount", 99)) {
               this.particleCount = tag.getInt("ParticleCount");
            }

            if (tag.contains("ParticleType", 8)) {
               this.particleType = tag.getString("ParticleType");
            }

            if (tag.contains("ParticleSpread", 99)) {
               this.particleSpread = tag.getDouble("ParticleSpread");
            }

            if (tag.contains("ParticleRingRadius", 99)) {
               this.particleRingRadius = tag.getDouble("ParticleRingRadius");
            }

            if (tag.contains("SpawnParticles", 99)) {
               this.spawnParticles = tag.getBoolean("SpawnParticles");
            }
         }

         @Override
         public JsonObject toJsonObject() {
            JsonObject object = super.toJsonObject();
            if (this.size != 0.5) {
               object.addProperty("size", this.size);
            }

            object.addProperty("textureLocation", this.textureLocation);
            if (this.alternateMuzzleFlash) {
               object.addProperty("alternateMuzzleFlash", true);
               JsonObject altPos = new JsonObject();
               altPos.addProperty("x", this.alternatePosition.x);
               altPos.addProperty("y", this.alternatePosition.y);
               altPos.addProperty("z", this.alternatePosition.z);
               object.add("alternatePosition", altPos);
            }

            if (this.particleCount != 5) {
               object.addProperty("particleCount", this.particleCount);
            }

            if (!this.particleType.equals("minecraft:flame")) {
               object.addProperty("particleType", this.particleType);
            }

            if (this.particleSpread != 0.1) {
               object.addProperty("particleSpread", this.particleSpread);
            }

            if (this.particleRingRadius != 0.0) {
               object.addProperty("particleRingRadius", this.particleRingRadius);
            }

            if (this.spawnParticles) {
               object.addProperty("spawnParticles", true);
            }

            return object;
         }

         public SimpleParticleType getParticleType() {
            try {
               ResourceLocation particleLocation = ResourceLocation.parse(this.particleType);
               ParticleType<?> registryType = (ParticleType<?>)BuiltInRegistries.PARTICLE_TYPE.get(particleLocation);
               if (registryType instanceof SimpleParticleType) {
                  return (SimpleParticleType)registryType;
               }
            } catch (Exception var4) {
            }

            return ParticleTypes.FLAME;
         }

         public boolean shouldSpawnParticles() {
            return this.spawnParticles;
         }

         public double getParticleSpread() {
            return this.particleSpread;
         }

         public double getParticleRingRadius() {
            return this.particleRingRadius;
         }

         public double getSize() {
            return this.size;
         }

         public String getTextureLocation() {
            return this.textureLocation;
         }

         public boolean hasAlternateMuzzleFlash() {
            return this.alternateMuzzleFlash;
         }

         public Vec3 getAlternatePosition() {
            return this.alternatePosition;
         }

         public int getParticleCount() {
            return this.particleCount;
         }
      }
   }

   public static class General  {
      private Gun.WeaponType weaponType = Gun.WeaponType.pistol;
      @Ignored
      private FireMode fireMode = FireMode.SEMI_AUTO;
      @Optional
      private int burstAmount;
      @Optional
      private int burstCooldown;
      private int rate;
      private int hotBarrelRate;
      @Optional
      private int fireTimer;
      @Ignored
      private GripType gripType = GripType.ONE_HANDED;
      @Ignored
      private GripType baseGripType = GripType.ONE_HANDED;
      @Optional
      private float recoilDurationOffset;
      @Optional
      private float recoilAdsReduction = 0.2F;
      @Optional
      private float restingSpread = 0.0F;
      @Optional
      private float spreadAdsReduction = 0.5F;
      @Optional
      private boolean infiniteAmmo;
      @Optional
      private float meleeDamage = 0.0F;
      @Optional
      public int meleeCooldownTicks = 15;
      @Optional
      private float meleeReach = 3.0F;
      @Optional
      private String beamColor;
      @Optional
      private String secondaryBeamColor;
      @Optional
      private String enchantedBeamColor;
      @Optional
      private String enchantedSecondaryBeamColor;
      @Optional
      private int beamAmmoConsumptionDelay = 1000;
      @Optional
      private int beamDamageDelay = 300;
      @Optional
      private double beamMaxDistance = 50.0;
      @Optional
      private boolean enableMining = false;
      @Optional
      private float miningSpeed = 1.0F;
      @Optional
      private boolean hasCameraShake = true;
      @Optional
      private boolean isRevolver = false;
      @Optional
      private float speedModifier = 1.0F;
      @Optional
      private boolean isSilenced = false;
      @Optional
      private boolean enableGunLight = true;
      @Optional
      private boolean usesCustomMeleeAnimation = false;
      @Optional
      private boolean allowAmmoChange = false;
      @Optional
      private List<String> availableAmmoTypes = new ArrayList<>();
      @Optional
      private int currentAmmoTypeIndex = 0;

      public General() {
         super();
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         tag.putString("WeaponType", this.weaponType.toString());
         tag.putString("FireMode", this.fireMode.id().toString());
         tag.putInt("BurstAmount", this.burstAmount);
         tag.putInt("BurstCooldown", this.burstCooldown);
         tag.putInt("Rate", this.rate);
         tag.putInt("HotBarrelRate", this.hotBarrelRate);
         tag.putInt("FireTimer", this.fireTimer);
         tag.putString("GripType", this.gripType.id().toString());
         tag.putString("BaseGripType", this.baseGripType.id().toString());
         tag.putFloat("RecoilDurationOffset", this.recoilDurationOffset);
         tag.putFloat("RecoilAdsReduction", this.recoilAdsReduction);
         tag.putFloat("RestingSpread", this.restingSpread);
         tag.putFloat("SpreadAdsReduction", this.spreadAdsReduction);
         tag.putFloat("MeleeDamage", this.meleeDamage);
         tag.putFloat("MeleeCooldownTicks", (float)this.meleeCooldownTicks);
         tag.putFloat("MeleeReach", this.meleeReach);
         tag.putBoolean("InfiniteAmmo", this.infiniteAmmo);
         if (this.beamColor != null && !this.beamColor.isEmpty()) {
            tag.putString("BeamColor", this.beamColor);
         }

         if (this.secondaryBeamColor != null && !this.secondaryBeamColor.isEmpty()) {
            tag.putString("SecondaryBeamColor", this.secondaryBeamColor);
         }

         if (this.secondaryBeamColor != null && !this.secondaryBeamColor.isEmpty()) {
            tag.putString("SecondaryBeamColor", this.secondaryBeamColor);
         }

         if (this.enchantedBeamColor != null && !this.enchantedBeamColor.isEmpty()) {
            tag.putString("EnchantedBeamColor", this.enchantedBeamColor);
         }

         if (this.enchantedSecondaryBeamColor != null && !this.enchantedSecondaryBeamColor.isEmpty()) {
            tag.putString("EnchantedSecondaryBeamColor", this.enchantedSecondaryBeamColor);
         }

         tag.putInt("BeamAmmoConsumptionDelay", this.beamAmmoConsumptionDelay);
         tag.putInt("BeamDamageDelay", this.beamDamageDelay);
         tag.putDouble("BeamMaxDistance", this.beamMaxDistance);
         tag.putBoolean("EnableMining", this.enableMining);
         tag.putFloat("MiningSpeed", this.miningSpeed);
         tag.putBoolean("HasCameraShake", this.hasCameraShake);
         tag.putBoolean("IsRevolver", this.isRevolver);
         tag.putFloat("SpeedModifier", this.speedModifier);
         tag.putBoolean("IsSilenced", this.isSilenced);
         tag.putBoolean("EnableGunLight", this.enableGunLight);
         tag.putBoolean("UsesCustomMeleeAnimation", this.usesCustomMeleeAnimation);
         tag.putBoolean("AllowAmmoChange", this.allowAmmoChange);
         tag.putInt("CurrentAmmoTypeIndex", this.currentAmmoTypeIndex);
         if (!this.availableAmmoTypes.isEmpty()) {
            ListTag listTag = new ListTag();

            for (String ammoType : this.availableAmmoTypes) {
               listTag.add(StringTag.valueOf(ammoType));
            }

            tag.put("AvailableAmmoTypes", listTag);
         }

         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("WeaponType", 8)) {
            this.weaponType = Gun.WeaponType.valueOf(tag.getString("WeaponType"));
         }

         if (tag.contains("FireMode", 8)) {
            this.fireMode = FireMode.getType(ResourceLocation.tryParse(tag.getString("FireMode")));
         }

         if (tag.contains("BurstAmount", 99)) {
            this.burstAmount = tag.getInt("BurstAmount");
         }

         if (tag.contains("BurstCooldown", 99)) {
            this.burstCooldown = tag.getInt("BurstCooldown");
         }

         if (tag.contains("Rate", 99)) {
            this.rate = tag.getInt("Rate");
         }

         if (tag.contains("HotBarrelRate", 99)) {
            this.hotBarrelRate = tag.getInt("HotBarrelRate");
         }

         if (tag.contains("FireTimer", 99)) {
            this.fireTimer = tag.getInt("FireTimer");
         }

         if (tag.contains("GripType", 8)) {
            this.gripType = GripType.getType(ResourceLocation.tryParse(tag.getString("GripType")));
         }

         if (tag.contains("BaseGripType", 8)) {
            this.baseGripType = GripType.getType(ResourceLocation.tryParse(tag.getString("BaseGripType")));
         }

         if (tag.contains("RecoilDurationOffset", 99)) {
            this.recoilDurationOffset = tag.getFloat("RecoilDurationOffset");
         }

         if (tag.contains("RecoilAdsReduction", 99)) {
            this.recoilAdsReduction = tag.getFloat("RecoilAdsReduction");
         }

         if (tag.contains("RestingSpread", 99)) {
            this.restingSpread = tag.getFloat("RestingSpread");
         }

         if (tag.contains("SpreadAdsReduction", 99)) {
            this.spreadAdsReduction = tag.getFloat("SpreadAdsReduction");
         }

         if (tag.contains("MeleeDamage", 99)) {
            this.meleeDamage = tag.getFloat("MeleeDamage");
         }

         if (tag.contains("MeleeCooldownTicks", 99)) {
            this.meleeCooldownTicks = tag.getInt("MeleeCooldownTicks");
         }

         if (tag.contains("MeleeReach", 99)) {
            this.meleeReach = tag.getFloat("MeleeReach");
         }

         if (tag.contains("InfiniteAmmo", 99)) {
            this.infiniteAmmo = tag.getBoolean("InfiniteAmmo");
         }

         if (tag.contains("BeamColor", 8)) {
            this.beamColor = tag.getString("BeamColor");
         }

         if (tag.contains("SecondaryBeamColor", 8)) {
            this.secondaryBeamColor = tag.getString("SecondaryBeamColor");
         }

         if (tag.contains("EnchantedBeamColor", 8)) {
            this.enchantedBeamColor = tag.getString("EnchantedBeamColor");
         }

         if (tag.contains("EnchantedSecondaryBeamColor", 8)) {
            this.enchantedSecondaryBeamColor = tag.getString("EnchantedSecondaryBeamColor");
         }

         if (tag.contains("BeamAmmoConsumptionDelay", 99)) {
            this.beamAmmoConsumptionDelay = tag.getInt("BeamAmmoConsumptionDelay");
         }

         if (tag.contains("BeamDamageDelay", 99)) {
            this.beamDamageDelay = tag.getInt("BeamDamageDelay");
         }

         if (tag.contains("BeamMaxDistance", 99)) {
            this.beamMaxDistance = tag.getDouble("BeamMaxDistance");
         }

         if (tag.contains("EnableMining", 99)) {
            this.enableMining = tag.getBoolean("EnableMining");
         }

         if (tag.contains("MiningSpeed", 99)) {
            this.miningSpeed = tag.getFloat("MiningSpeed");
         }

         if (tag.contains("HasCameraShake", 99)) {
            this.hasCameraShake = tag.getBoolean("HasCameraShake");
         }

         if (tag.contains("IsRevolver", 99)) {
            this.isRevolver = tag.getBoolean("IsRevolver");
         }

         if (tag.contains("SpeedModifier", 99)) {
            this.speedModifier = tag.getFloat("SpeedModifier");
         }

         if (tag.contains("IsSilenced", 99)) {
            this.isSilenced = tag.getBoolean("IsSilenced");
         }

         if (tag.contains("EnableGunLight", 99)) {
            this.enableGunLight = tag.getBoolean("EnableGunLight");
         }

         if (tag.contains("UsesCustomMeleeAnimation", 99)) {
            this.usesCustomMeleeAnimation = tag.getBoolean("UsesCustomMeleeAnimation");
         }

         if (tag.contains("AllowAmmoChange", 99)) {
            this.allowAmmoChange = tag.getBoolean("AllowAmmoChange");
         }

         if (tag.contains("CurrentAmmoTypeIndex", 99)) {
            this.currentAmmoTypeIndex = tag.getInt("CurrentAmmoTypeIndex");
         }

         if (tag.contains("AvailableAmmoTypes", 9)) {
            this.availableAmmoTypes.clear();
            ListTag list = tag.getList("AvailableAmmoTypes", 8);

            for (int i = 0; i < list.size(); i++) {
               this.availableAmmoTypes.add(list.getString(i));
            }
         }
      }

      public JsonObject toJsonObject() {
         Preconditions.checkArgument(this.rate > 0, "Rate must be more than zero");
         Preconditions.checkArgument(this.hotBarrelRate >= 0, "Hot barrel rate must be more than or equal to zero");
         Preconditions.checkArgument(
            this.recoilDurationOffset >= 0.0F && this.recoilDurationOffset <= 1.0F, "Recoil duration offset must be between 0.0 and 1.0"
         );
         Preconditions.checkArgument(this.recoilAdsReduction >= 0.0F && this.recoilAdsReduction <= 1.0F, "Recoil ads reduction must be between 0.0 and 1.0");
         Preconditions.checkArgument(this.restingSpread >= 0.0F, "Resting spread must be more than or equal to zero");
         Preconditions.checkArgument(this.spreadAdsReduction >= 0.0F && this.spreadAdsReduction <= 1.0F, "Spread ADS reduction must be between 0.0 and 1.0");
         JsonObject object = new JsonObject();
         if (this.infiniteAmmo) {
            object.addProperty("infiniteAmmo", true);
         }

         object.addProperty("fireMode", this.fireMode.id().toString());
         object.addProperty("weaponType", this.weaponType.toString());
         if (this.burstAmount != 0) {
            object.addProperty("burstAmount", this.burstAmount);
         }

         if (this.burstCooldown != 0) {
            object.addProperty("burstCooldown", this.burstCooldown);
         }

         object.addProperty("rate", this.rate);
         if (this.fireTimer != 0) {
            object.addProperty("fireTimer", this.fireTimer);
         }

         object.addProperty("gripType", this.gripType.id().toString());
         object.addProperty("baseGripType", this.baseGripType.id().toString());
         if (this.recoilDurationOffset != 0.0F) {
            object.addProperty("recoilDurationOffset", this.recoilDurationOffset);
         }

         if (this.recoilAdsReduction != 0.2F) {
            object.addProperty("recoilAdsReduction", this.recoilAdsReduction);
         }

         if (this.restingSpread != 0.0F) {
            object.addProperty("restingSpread", this.restingSpread);
         }

         if (this.spreadAdsReduction != 0.5F) {
            object.addProperty("spreadAdsReduction", this.spreadAdsReduction);
         }

         if (this.meleeDamage != 0.0F) {
            object.addProperty("meleeDamage", this.meleeDamage);
         }

         if (this.meleeCooldownTicks != 15) {
            object.addProperty("meleeCooldownTicks", this.meleeCooldownTicks);
         }

         if (this.meleeReach != 3.0F) {
            object.addProperty("meleeReach", this.meleeReach);
         }

         if (this.beamColor != null && !this.beamColor.isEmpty()) {
            object.addProperty("beamColor", this.beamColor);
         }

         if (this.secondaryBeamColor != null && !this.secondaryBeamColor.isEmpty()) {
            object.addProperty("secondaryBeamColor", this.secondaryBeamColor);
         }

         if (this.enchantedBeamColor != null && !this.enchantedBeamColor.isEmpty()) {
            object.addProperty("enchantedBeamColor", this.enchantedBeamColor);
         }

         if (this.enchantedSecondaryBeamColor != null && !this.enchantedSecondaryBeamColor.isEmpty()) {
            object.addProperty("enchantedSecondaryBeamColor", this.enchantedSecondaryBeamColor);
         }

         if (this.beamAmmoConsumptionDelay != 1000) {
            object.addProperty("beamAmmoConsumptionDelay", this.beamAmmoConsumptionDelay);
         }

         if (this.beamDamageDelay != 300) {
            object.addProperty("beamDamageDelay", this.beamDamageDelay);
         }

         if (this.beamMaxDistance != 50.0) {
            object.addProperty("beamMaxDistance", this.beamMaxDistance);
         }

         if (this.miningSpeed != 1.0F) {
            object.addProperty("miningSpeed", this.miningSpeed);
         }

         if (this.enableMining) {
            object.addProperty("enableMining", true);
         }

         if (!this.hasCameraShake) {
            object.addProperty("hasCameraShake", false);
         }

         if (this.isRevolver) {
            object.addProperty("isRevolver", true);
         }

         if (this.speedModifier != 1.0F) {
            object.addProperty("speedModifier", this.speedModifier);
         }

         if (this.isSilenced) {
            object.addProperty("isSilenced", true);
         }

         if (!this.enableGunLight) {
            object.addProperty("enableGunLight", false);
         }

         if (this.usesCustomMeleeAnimation) {
            object.addProperty("usesCustomMeleeAnimation", true);
         }

         if (this.allowAmmoChange) {
            object.addProperty("allowAmmoChange", true);
         }

         return object;
      }

      public Gun.General copy() {
         Gun.General general = new Gun.General();
         general.weaponType = this.weaponType;
         general.fireMode = this.fireMode;
         general.burstAmount = this.burstAmount;
         general.burstCooldown = this.burstCooldown;
         general.rate = this.rate;
         general.hotBarrelRate = this.hotBarrelRate;
         general.fireTimer = this.fireTimer;
         general.gripType = this.gripType;
         general.baseGripType = this.baseGripType;
         general.recoilDurationOffset = this.recoilDurationOffset;
         general.recoilAdsReduction = this.recoilAdsReduction;
         general.restingSpread = this.restingSpread;
         general.spreadAdsReduction = this.spreadAdsReduction;
         general.infiniteAmmo = this.infiniteAmmo;
         general.meleeDamage = this.meleeDamage;
         general.meleeCooldownTicks = this.meleeCooldownTicks;
         general.meleeReach = this.meleeReach;
         general.beamMaxDistance = this.beamMaxDistance;
         general.beamAmmoConsumptionDelay = this.beamAmmoConsumptionDelay;
         general.beamDamageDelay = this.beamDamageDelay;
         general.beamColor = this.beamColor;
         general.secondaryBeamColor = this.secondaryBeamColor;
         general.enchantedBeamColor = this.enchantedBeamColor;
         general.enchantedSecondaryBeamColor = this.enchantedSecondaryBeamColor;
         general.enableMining = this.enableMining;
         general.miningSpeed = this.miningSpeed;
         general.hasCameraShake = this.hasCameraShake;
         general.isRevolver = this.isRevolver;
         general.speedModifier = this.speedModifier;
         general.isSilenced = this.isSilenced;
         general.enableGunLight = this.enableGunLight;
         general.usesCustomMeleeAnimation = this.usesCustomMeleeAnimation;
         general.allowAmmoChange = this.allowAmmoChange;
         general.availableAmmoTypes = new ArrayList<>(this.availableAmmoTypes);
         general.currentAmmoTypeIndex = this.currentAmmoTypeIndex;
         return general;
      }

      public boolean usesCustomMeleeAnimation() {
         return this.usesCustomMeleeAnimation;
      }

      public boolean isEnableGunLight() {
         return this.enableGunLight;
      }

      public float getSpeedModifier() {
         return this.speedModifier;
      }

      public boolean isRevolver() {
         return this.isRevolver;
      }

      public boolean hasCameraShake() {
         return this.hasCameraShake;
      }

      public boolean canMine() {
         return this.enableMining;
      }

      public float getMiningSpeed() {
         return this.miningSpeed;
      }

      public double getBeamMaxDistance() {
         return this.beamMaxDistance;
      }

      public int getBeamAmmoConsumptionDelay() {
         return this.beamAmmoConsumptionDelay;
      }

      public String getEnchantedBeamColor() {
         return this.enchantedBeamColor;
      }

      public String getEnchantedSecondaryBeamColor() {
         return this.enchantedSecondaryBeamColor;
      }

      public int getBeamDamageDelay() {
         return this.beamDamageDelay;
      }

      public float getMeleeDamage() {
         return this.meleeDamage;
      }

      public int getMeleeCooldownTicks() {
         return this.meleeCooldownTicks;
      }

      public void setMeleeDamage(float meleeDamage) {
         this.meleeDamage = meleeDamage;
      }

      public float getMeleeReach() {
         return this.meleeReach;
      }

      public Gun.WeaponType getWeaponType() {
         return this.weaponType;
      }

      public FireMode getFireMode() {
         return this.fireMode;
      }

      public String getBeamColor() {
         return this.beamColor;
      }

      public String getSecondaryBeamColor() {
         return this.secondaryBeamColor;
      }

      public int getRate() {
         return this.rate;
      }

      public int getHotBarrelRate() {
         return this.hotBarrelRate;
      }

      public int getFireTimer() {
         return this.fireTimer;
      }

      public GripType getGripType(ItemStack stack) {
         return this.getModifiedGun(stack).determineGripType(stack);
      }

      private Gun getModifiedGun(ItemStack stack) {
         return !stack.isEmpty() && stack.getItem() instanceof GunItem item ? item.getModifiedGun(stack) : new Gun();
      }

      public float getRecoilDurationOffset() {
         return this.recoilDurationOffset;
      }

      public float getRecoilAdsReduction() {
         return this.recoilAdsReduction;
      }

      public boolean isAuto() {
         return this.fireMode == FireMode.AUTOMATIC;
      }

      public boolean getInfiniteAmmo() {
         return this.infiniteAmmo;
      }

      public float getRestingSpread() {
         return this.restingSpread;
      }

      public float getSpreadAdsReduction() {
         return this.spreadAdsReduction;
      }

      public int getBurstAmount() {
         return this.burstAmount;
      }

      public int getBurstCooldown() {
         return this.burstCooldown;
      }

      public GripType getBaseGripType() {
         return this.baseGripType;
      }

      public boolean isSilenced() {
         return this.isSilenced;
      }

      public boolean allowsAmmoChange() {
         return this.allowAmmoChange;
      }

      public List<String> getAvailableAmmoTypes() {
         return this.availableAmmoTypes;
      }

      public int getCurrentAmmoTypeIndex() {
         return this.currentAmmoTypeIndex;
      }

      public void setCurrentAmmoTypeIndex(int index) {
         this.currentAmmoTypeIndex = index;
      }
   }

   public static class Modules implements IEditorMenu {
      private transient Gun.Modules.Zoom cachedZoom;
      @Optional
      @Nullable
      private Gun.Modules.Zoom zoom;
      private Gun.Modules.Attachments attachments = new Gun.Modules.Attachments();

      public Modules() {
         super();
      }

      @Nullable
      public Gun.Modules.Zoom getZoom() {
         return this.zoom;
      }

      public Gun.Modules.Attachments getAttachments() {
         return this.attachments;
      }

      @Override
      public Component getEditorLabel() {
         return Component.translatable("Modules");
      }

      @Override
      public void getEditorWidgets(List<Pair<Component, Supplier<IDebugWidget>>> widgets) {
         DistHelper.runWhenOn(Dist.CLIENT, () -> {
               widgets.add(Pair.of(Component.translatable("Enabled Iron Sights"), (Supplier<IDebugWidget>)() -> new DebugToggle(this.zoom != null, val -> {
                     if (val) {
                        if (this.cachedZoom != null) {
                           this.zoom = this.cachedZoom;
                        } else {
                           this.zoom = new Gun.Modules.Zoom();
                           this.cachedZoom = this.zoom;
                        }
                     } else {
                        this.cachedZoom = this.zoom;
                        this.zoom = null;
                     }
                  })));
               widgets.add(Pair.of(Component.translatable("Adjust Iron Sights"), (Supplier<IDebugWidget>)() -> new DebugButton(Component.translatable(">"), btn -> {
                     if (btn.active && this.zoom != null) {
                        Minecraft.getInstance().setScreen(ClientHandler.createEditorScreen(this.zoom));
                     }
                  }, () -> this.zoom != null)));
            });
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         if (this.zoom != null) {
            tag.put("Zoom", this.zoom.serializeNBT());
         }

         tag.put("Attachments", this.attachments.serializeNBT());
         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("Zoom", 10)) {
            Gun.Modules.Zoom zoom = new Gun.Modules.Zoom();
            zoom.deserializeNBT(tag.getCompound("Zoom"));
            this.zoom = zoom;
         }

         if (tag.contains("Attachments", 10)) {
            this.attachments.deserializeNBT(tag.getCompound("Attachments"));
         }
      }

      public JsonObject toJsonObject() {
         JsonObject object = new JsonObject();
         if (this.zoom != null) {
            object.add("zoom", this.zoom.toJsonObject());
         }

         GunJsonUtil.addObjectIfNotEmpty(object, "attachments", this.attachments.toJsonObject());
         return object;
      }

      public Gun.Modules copy() {
         Gun.Modules modules = new Gun.Modules();
         if (this.zoom != null) {
            modules.zoom = this.zoom.copy();
         }

         modules.attachments = this.attachments.copy();
         return modules;
      }

      public static class Attachments  {
         @Optional
         @Nullable
         private Gun.ScaledPositioned scope;
         @Optional
         @Nullable
         private Gun.ScaledPositioned barrel;
         @Optional
         @Nullable
         private Gun.ScaledPositioned stock;
         @Optional
         @Nullable
         private Gun.ScaledPositioned underBarrel;
         @Optional
         @Nullable
         private Gun.ScaledPositioned magazine;

         public Attachments() {
            super();
         }

         @Nullable
         public Gun.ScaledPositioned getScope() {
            return this.scope;
         }

         @Nullable
         public Gun.ScaledPositioned getBarrel() {
            return this.barrel;
         }

         @Nullable
         public Gun.ScaledPositioned getStock() {
            return this.stock;
         }

         @Nullable
         public Gun.ScaledPositioned getUnderBarrel() {
            return this.underBarrel;
         }

         @Nullable
         public Gun.ScaledPositioned getMagazine() {
            return this.magazine;
         }

         public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            if (this.scope != null) {
               tag.put("Scope", this.scope.serializeNBT());
            }

            if (this.barrel != null) {
               tag.put("Barrel", this.barrel.serializeNBT());
            }

            if (this.stock != null) {
               tag.put("Stock", this.stock.serializeNBT());
            }

            if (this.underBarrel != null) {
               tag.put("UnderBarrel", this.underBarrel.serializeNBT());
            }

            if (this.magazine != null) {
               tag.put("Magazine", this.magazine.serializeNBT());
            }

            return tag;
         }

         public void deserializeNBT(CompoundTag tag) {
            if (tag.contains("Scope", 10)) {
               this.scope = this.createScaledPositioned(tag, "Scope");
            }

            if (tag.contains("Barrel", 10)) {
               this.barrel = this.createScaledPositioned(tag, "Barrel");
            }

            if (tag.contains("Stock", 10)) {
               this.stock = this.createScaledPositioned(tag, "Stock");
            }

            if (tag.contains("UnderBarrel", 10)) {
               this.underBarrel = this.createScaledPositioned(tag, "UnderBarrel");
            }

            if (tag.contains("Magazine", 10)) {
               this.magazine = this.createScaledPositioned(tag, "Magazine");
            }
         }

         public JsonObject toJsonObject() {
            JsonObject object = new JsonObject();
            if (this.scope != null) {
               object.add("scope", this.scope.toJsonObject());
            }

            if (this.barrel != null) {
               object.add("barrel", this.barrel.toJsonObject());
            }

            if (this.stock != null) {
               object.add("stock", this.stock.toJsonObject());
            }

            if (this.underBarrel != null) {
               object.add("underBarrel", this.underBarrel.toJsonObject());
            }

            if (this.magazine != null) {
               object.add("magazine", this.magazine.toJsonObject());
            }

            return object;
         }

         public Gun.Modules.Attachments copy() {
            Gun.Modules.Attachments attachments = new Gun.Modules.Attachments();
            if (this.scope != null) {
               attachments.scope = this.scope.copy();
            }

            if (this.barrel != null) {
               attachments.barrel = this.barrel.copy();
            }

            if (this.stock != null) {
               attachments.stock = this.stock.copy();
            }

            if (this.underBarrel != null) {
               attachments.underBarrel = this.underBarrel.copy();
            }

            if (this.magazine != null) {
               attachments.magazine = this.magazine.copy();
            }

            return attachments;
         }

         @Nullable
         private Gun.ScaledPositioned createScaledPositioned(CompoundTag tag, String key) {
            CompoundTag attachment = tag.getCompound(key);
            return attachment.isEmpty() ? null : new Gun.ScaledPositioned(attachment);
         }
      }

      public static class Zoom extends Gun.Positioned implements IEditorMenu {
         @Optional
         private float fovModifier;

         public Zoom() {
            super();
         }

         @Override
         public CompoundTag serializeNBT() {
            CompoundTag tag = super.serializeNBT();
            tag.putFloat("FovModifier", this.fovModifier);
            return tag;
         }

         @Override
         public void deserializeNBT(CompoundTag tag) {
            super.deserializeNBT(tag);
            if (tag.contains("FovModifier", 99)) {
               this.fovModifier = tag.getFloat("FovModifier");
            }
         }

         @Override
         public JsonObject toJsonObject() {
            JsonObject object = super.toJsonObject();
            object.addProperty("fovModifier", this.fovModifier);
            return object;
         }

         public Gun.Modules.Zoom copy() {
            Gun.Modules.Zoom zoom = new Gun.Modules.Zoom();
            zoom.fovModifier = this.fovModifier;
            zoom.xOffset = this.xOffset;
            zoom.yOffset = this.yOffset;
            zoom.zOffset = this.zOffset;
            return zoom;
         }

         @Override
         public Component getEditorLabel() {
            return Component.translatable("Zoom");
         }

         @Override
         public void getEditorWidgets(List<Pair<Component, Supplier<IDebugWidget>>> widgets) {
            DistHelper.runWhenOn(Dist.CLIENT, () -> widgets.add(
                        Pair.of(
                           Component.translatable("FOV Modifier"),
                           (Supplier<IDebugWidget>)() -> new DebugSlider(
                                 0.0, 1.0, (double)this.fovModifier, 0.01, 3, val -> this.fovModifier = val.floatValue()
                              )
                        )
                     )
            );
         }

         public float getFovModifier() {
            return this.fovModifier;
         }

         public static Gun.Modules.Zoom.Builder builder() {
            return new Gun.Modules.Zoom.Builder();
         }

         protected abstract static class AbstractBuilder<T extends Gun.Modules.Zoom.AbstractBuilder<T>> extends Gun.Positioned.AbstractBuilder<T> {
            protected final Gun.Modules.Zoom zoom;

            protected AbstractBuilder() {
               this(new Gun.Modules.Zoom());
            }

            protected AbstractBuilder(Gun.Modules.Zoom zoom) {
               super(zoom);
               this.zoom = zoom;
            }

            public T setFovModifier(float fovModifier) {
               this.zoom.fovModifier = fovModifier;
               return this.self();
            }

            public Gun.Modules.Zoom build() {
               return this.zoom.copy();
            }
         }

         public static class Builder extends Gun.Modules.Zoom.AbstractBuilder<Gun.Modules.Zoom.Builder> {
            public Builder() {
               super();
            }
         }
      }
   }

   public static class Positioned  {
      @Optional
      protected double xOffset;
      @Optional
      protected double yOffset;
      @Optional
      protected double zOffset;

      public Positioned() {
         super();
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         tag.putDouble("XOffset", this.xOffset);
         tag.putDouble("YOffset", this.yOffset);
         tag.putDouble("ZOffset", this.zOffset);
         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("XOffset", 99)) {
            this.xOffset = tag.getDouble("XOffset");
         }

         if (tag.contains("YOffset", 99)) {
            this.yOffset = tag.getDouble("YOffset");
         }

         if (tag.contains("ZOffset", 99)) {
            this.zOffset = tag.getDouble("ZOffset");
         }
      }

      public JsonObject toJsonObject() {
         JsonObject object = new JsonObject();
         if (this.xOffset != 0.0) {
            object.addProperty("xOffset", this.xOffset);
         }

         if (this.yOffset != 0.0) {
            object.addProperty("yOffset", this.yOffset);
         }

         if (this.zOffset != 0.0) {
            object.addProperty("zOffset", this.zOffset);
         }

         return object;
      }

      public double getXOffset() {
         return this.xOffset;
      }

      public double getYOffset() {
         return this.yOffset;
      }

      public double getZOffset() {
         return this.zOffset;
      }

      public Gun.Positioned copy() {
         Gun.Positioned positioned = new Gun.Positioned();
         positioned.xOffset = this.xOffset;
         positioned.yOffset = this.yOffset;
         positioned.zOffset = this.zOffset;
         return positioned;
      }

      protected abstract static class AbstractBuilder<T extends Gun.Positioned.AbstractBuilder<T>> extends SuperBuilder<Gun.Positioned, T> {
         private final Gun.Positioned positioned;

         private AbstractBuilder() {
            this(new Gun.Positioned());
         }

         protected AbstractBuilder(Gun.Positioned positioned) {
            super();
            this.positioned = positioned;
         }

         public T setOffset(double xOffset, double yOffset, double zOffset) {
            this.positioned.xOffset = xOffset;
            this.positioned.yOffset = yOffset;
            this.positioned.zOffset = zOffset;
            return this.self();
         }

         public T setXOffset(double xOffset) {
            this.positioned.xOffset = xOffset;
            return this.self();
         }

         public T setYOffset(double yOffset) {
            this.positioned.yOffset = yOffset;
            return this.self();
         }

         public T setZOffset(double zOffset) {
            this.positioned.zOffset = zOffset;
            return this.self();
         }

         public Gun.Positioned build() {
            return this.positioned.copy();
         }
      }

      public static class Builder extends Gun.Positioned.AbstractBuilder<Gun.Positioned.Builder> {
         public Builder() {
            super();
         }
      }
   }

   public static class Projectile  {
      @Optional
      private int energyUse = 0;
      @Optional
      private int durabilityDamage = 1;
      public ResourceLocation item;
      public ResourceLocation casingType;
      @Optional
      private boolean ejectsCasing;
      @Optional
      private boolean visible;
      private float damage;
      @Optional
      private float armorPen;
      @Optional
      private ResourceLocation advantage = ResourceLocation.fromNamespaceAndPath("scguns", "none");
      private float size;
      private double speed;
      private int life;
      @Optional
      private boolean gravity;
      @Optional
      private boolean damageReduceOverLife;
      @Optional
      private int trailColor = 16765577;
      @Optional
      private double trailLengthMultiplier = 1.0;
      @Optional
      private boolean hideTrail;
      @Optional
      @Nullable
      private ResourceLocation casingParticle;
      @Optional
      private boolean ejectDuringReload;
      @Optional
      private boolean firesArrows = false;
      @Optional
      @Nullable
      private ResourceLocation impactEffect;
      @Optional
      private int impactEffectDuration = 100;
      @Optional
      private int impactEffectAmplifier = 0;
      @Optional
      private float impactEffectChance = 1.0F;
      @Optional
      private boolean isSoulFire = false;
      @Optional
      private boolean hideProjectile = false;
      @Optional
      private double trailThickness = 1.0;
      @Optional
      private float knockbackStrength = 0.3F;
      @Optional
      private float damageFalloffStart = 0.0F;
      @Optional
      private float damageFalloffEnd = 0.0F;
      @Optional
      private float damageFalloffMinMultiplier = 1.0F;
      @Optional
      private int projectileAmount = 1;
      @Optional
      private float recoilAngle = 0.0F;
      @Optional
      private float recoilKick = 0.0F;
      @Optional
      private boolean alwaysSpread = false;
      @Optional
      private float spread = 0.0F;
      @Optional
      private float criticalChance = 0.0F;
      @Optional
      private float critDamageMultiplier = 1.5F;
      @Optional
      private boolean playerKnockBack = false;
      @Optional
      private float playerKnockBackStrength = 0.0F;

      public Projectile() {
         super();
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         tag.putInt("EnergyUse", this.energyUse);
         tag.putInt("DurabilityDamage", this.durabilityDamage);
         tag.putString("Item", this.item.toString());
         tag.putBoolean("EjectsCasing", this.ejectsCasing);
         tag.putBoolean("Visible", this.visible);
         tag.putFloat("Damage", this.damage);
         tag.putFloat("ArmorPen", this.armorPen);
         tag.putString("Advantage", this.advantage.toString());
         tag.putFloat("Size", this.size);
         tag.putDouble("Speed", this.speed);
         tag.putInt("Life", this.life);
         tag.putBoolean("Gravity", this.gravity);
         tag.putBoolean("DamageReduceOverLife", this.damageReduceOverLife);
         tag.putInt("TrailColor", this.trailColor);
         tag.putBoolean("HideTrail", this.hideTrail);
         tag.putDouble("TrailLengthMultiplier", this.trailLengthMultiplier);
         if (this.casingType != null) {
            tag.putString("CasingType", this.casingType.toString());
         }

         if (this.casingParticle != null) {
            tag.putString("CasingParticle", this.casingParticle.toString());
         }

         tag.putBoolean("EjectDuringReload", this.ejectDuringReload);
         tag.putBoolean("FiresArrows", this.firesArrows);
         if (this.impactEffect != null) {
            tag.putString("ImpactEffect", this.impactEffect.toString());
            tag.putInt("ImpactEffectDuration", this.impactEffectDuration);
            tag.putInt("ImpactEffectAmplifier", this.impactEffectAmplifier);
            tag.putFloat("ImpactEffectChance", this.impactEffectChance);
         }

         tag.putBoolean("IsSoulFire", this.isSoulFire);
         tag.putBoolean("HideProjectile", this.hideProjectile);
         tag.putDouble("TrailThickness", this.trailThickness);
         tag.putFloat("KnockbackStrength", this.knockbackStrength);
         tag.putFloat("DamageFalloffStart", this.damageFalloffStart);
         tag.putFloat("DamageFalloffEnd", this.damageFalloffEnd);
         tag.putFloat("DamageFalloffMinMultiplier", this.damageFalloffMinMultiplier);
         tag.putInt("ProjectileAmount", this.projectileAmount);
         tag.putFloat("RecoilAngle", this.recoilAngle);
         tag.putFloat("RecoilKick", this.recoilKick);
         tag.putFloat("Spread", this.spread);
         tag.putBoolean("AlwaysSpread", this.alwaysSpread);
         tag.putFloat("CriticalChance", this.criticalChance);
         tag.putFloat("CritDamageMultiplier", this.critDamageMultiplier);
         tag.putBoolean("PlayerKnockBack", this.playerKnockBack);
         tag.putFloat("PlayerKnockBackStrength", this.playerKnockBackStrength);
         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("EnergyUse", 99)) {
            this.energyUse = tag.getInt("EnergyUse");
         }

         if (tag.contains("DurabilityDamage", 99)) {
            this.durabilityDamage = tag.getInt("DurabilityDamage");
         }

         if (tag.contains("Item", 8)) {
            this.item = ResourceLocation.parse(tag.getString("Item"));
         }

         if (tag.contains("CasingType", 8)) {
            this.casingType = ResourceLocation.parse(tag.getString("CasingType"));
         }

         if (tag.contains("EjectsCasing", 99)) {
            this.ejectsCasing = tag.getBoolean("EjectsCasing");
         }

         if (tag.contains("Visible", 99)) {
            this.visible = tag.getBoolean("Visible");
         }

         if (tag.contains("Damage", 99)) {
            this.damage = tag.getFloat("Damage");
         }

         if (tag.contains("ArmorPen", 99)) {
            this.armorPen = tag.getFloat("ArmorPen");
         }

         if (tag.contains("Advantage", 8)) {
            this.advantage = ResourceLocation.parse(tag.getString("Advantage"));
         }

         if (tag.contains("Size", 99)) {
            this.size = tag.getFloat("Size");
         }

         if (tag.contains("Speed", 99)) {
            this.speed = tag.getDouble("Speed");
         }

         if (tag.contains("Life", 99)) {
            this.life = tag.getInt("Life");
         }

         if (tag.contains("Gravity", 99)) {
            this.gravity = tag.getBoolean("Gravity");
         }

         if (tag.contains("HideTrail", 99)) {
            this.hideTrail = tag.getBoolean("HideTrail");
         }

         if (tag.contains("DamageReduceOverLife", 99)) {
            this.damageReduceOverLife = tag.getBoolean("DamageReduceOverLife");
         }

         if (tag.contains("TrailColor", 99)) {
            this.trailColor = tag.getInt("TrailColor");
         }

         if (tag.contains("TrailLengthMultiplier", 99)) {
            this.trailLengthMultiplier = tag.getDouble("TrailLengthMultiplier");
         }

         if (tag.contains("CasingParticle", 8)) {
            this.casingParticle = ResourceLocation.parse(tag.getString("CasingParticle"));
         }

         if (tag.contains("EjectDuringReload", 99)) {
            this.ejectDuringReload = tag.getBoolean("EjectDuringReload");
         }

         if (tag.contains("FiresArrows", 99)) {
            this.firesArrows = tag.getBoolean("FiresArrows");
         }

         if (tag.contains("ImpactEffect", 8)) {
            this.impactEffect = ResourceLocation.parse(tag.getString("ImpactEffect"));
            this.impactEffectDuration = tag.getInt("ImpactEffectDuration");
            this.impactEffectAmplifier = tag.getInt("ImpactEffectAmplifier");
            this.impactEffectChance = tag.getFloat("ImpactEffectChance");
         }

         if (tag.contains("IsSoulFire", 99)) {
            this.isSoulFire = tag.getBoolean("IsSoulFire");
         }

         if (tag.contains("HideProjectile", 99)) {
            this.hideProjectile = tag.getBoolean("HideProjectile");
         }

         if (tag.contains("TrailThickness", 99)) {
            this.trailThickness = tag.getDouble("TrailThickness");
         }

         if (tag.contains("KnockbackStrength", 99)) {
            this.knockbackStrength = tag.getFloat("KnockbackStrength");
         }

         if (tag.contains("DamageFalloffStart", 99)) {
            this.damageFalloffStart = tag.getFloat("DamageFalloffStart");
         }

         if (tag.contains("DamageFalloffEnd", 99)) {
            this.damageFalloffEnd = tag.getFloat("DamageFalloffEnd");
         }

         if (tag.contains("DamageFalloffMinMultiplier", 99)) {
            this.damageFalloffMinMultiplier = tag.getFloat("DamageFalloffMinMultiplier");
         }

         if (tag.contains("ProjectileAmount", 99)) {
            this.projectileAmount = tag.getInt("ProjectileAmount");
         }

         if (tag.contains("RecoilAngle", 99)) {
            this.recoilAngle = tag.getFloat("RecoilAngle");
         }

         if (tag.contains("RecoilKick", 99)) {
            this.recoilKick = tag.getFloat("RecoilKick");
         }

         if (tag.contains("Spread", 99)) {
            this.spread = tag.getFloat("Spread");
         }

         if (tag.contains("AlwaysSpread", 99)) {
            this.alwaysSpread = tag.getBoolean("AlwaysSpread");
         }

         if (tag.contains("CriticalChance", 99)) {
            this.criticalChance = tag.getFloat("CriticalChance");
         }

         if (tag.contains("CritDamageMultiplier", 99)) {
            this.critDamageMultiplier = tag.getFloat("CritDamageMultiplier");
         }

         if (tag.contains("PlayerKnockBack", 99)) {
            this.playerKnockBack = tag.getBoolean("PlayerKnockBack");
         }

         if (tag.contains("PlayerKnockBackStrength", 99)) {
            this.playerKnockBackStrength = tag.getFloat("PlayerKnockBackStrength");
         }
      }

      public JsonObject toJsonObject() {
         Preconditions.checkArgument(this.damage >= 0.0F, "Damage must be more than or equal to zero");
         Preconditions.checkArgument(this.size >= 0.0F, "Projectile size must be more than or equal to zero");
         Preconditions.checkArgument(this.speed >= 0.0, "Projectile speed must be more than or equal to zero");
         Preconditions.checkArgument(this.life > 0, "Projectile life must be more than zero");
         Preconditions.checkArgument(this.trailLengthMultiplier >= 0.0, "Projectile trail length multiplier must be more than or equal to zero");
         Preconditions.checkArgument(this.projectileAmount >= 1, "Projectile amount must be more than or equal to one");
         Preconditions.checkArgument(this.spread >= 0.0F, "Spread must be more than or equal to zero");
         JsonObject object = new JsonObject();
         object.addProperty("item", this.item.toString());
         if (this.energyUse != 0) {
            object.addProperty("energyUse", this.energyUse);
         }

         if (this.ejectsCasing) {
            object.addProperty("ejectsCasing", true);
         }

         if (this.visible) {
            object.addProperty("visible", true);
         }

         object.addProperty("damage", this.damage);
         if (this.armorPen != 0.0F) {
            object.addProperty("armorPen", this.armorPen);
         }

         if (this.advantage != null) {
            object.addProperty("advantage", this.advantage.toString());
         }

         object.addProperty("size", this.size);
         object.addProperty("speed", this.speed);
         object.addProperty("life", this.life);
         if (this.gravity) {
            object.addProperty("gravity", true);
         }

         if (this.damageReduceOverLife) {
            object.addProperty("damageReduceOverLife", true);
         }

         if (this.trailColor != 16776960) {
            object.addProperty("trailColor", this.trailColor);
         }

         if (this.trailLengthMultiplier != 1.0) {
            object.addProperty("trailLengthMultiplier", this.trailLengthMultiplier);
         }

         if (this.casingType != null) {
            object.addProperty("casingType", this.casingType.toString());
         }

         if (this.casingParticle != null) {
            object.addProperty("casingParticle", this.casingParticle.toString());
         }

         if (this.ejectDuringReload) {
            object.addProperty("ejectDuringReload", true);
         }

         if (this.firesArrows) {
            object.addProperty("firesArrows", true);
         }

         if (this.impactEffect != null) {
            object.addProperty("impactEffect", this.impactEffect.toString());
            if (this.impactEffectDuration != 100) {
               object.addProperty("impactEffectDuration", this.impactEffectDuration);
            }

            if (this.impactEffectAmplifier != 0) {
               object.addProperty("impactEffectAmplifier", this.impactEffectAmplifier);
            }

            if (this.impactEffectChance != 1.0F) {
               object.addProperty("impactEffectChance", this.impactEffectChance);
            }
         }

         if (this.isSoulFire) {
            object.addProperty("isSoulFire", true);
         }

         if (this.hideProjectile) {
            object.addProperty("hideProjectile", true);
         }

         if (this.hideTrail) {
            object.addProperty("hideTrail", true);
         }

         if (this.trailThickness != 1.0) {
            object.addProperty("trailThickness", this.trailThickness);
         }

         if (this.knockbackStrength != 0.3F) {
            object.addProperty("knockbackStrength", this.knockbackStrength);
         }

         if (this.damageFalloffStart != 0.0F) {
            object.addProperty("damageFalloffStart", this.damageFalloffStart);
         }

         if (this.damageFalloffEnd != 0.0F) {
            object.addProperty("damageFalloffEnd", this.damageFalloffEnd);
         }

         if (this.damageFalloffMinMultiplier != 1.0F) {
            object.addProperty("damageFalloffMinMultiplier", this.damageFalloffMinMultiplier);
         }

         if (this.projectileAmount != 1) {
            object.addProperty("projectileAmount", this.projectileAmount);
         }

         if (this.recoilAngle != 0.0F) {
            object.addProperty("recoilAngle", this.recoilAngle);
         }

         if (this.recoilKick != 0.0F) {
            object.addProperty("recoilKick", this.recoilKick);
         }

         if (this.spread != 0.0F) {
            object.addProperty("spread", this.spread);
         }

         if (this.alwaysSpread) {
            object.addProperty("alwaysSpread", true);
         }

         if (this.criticalChance != 0.0F) {
            object.addProperty("criticalChance", this.criticalChance);
         }

         if (this.critDamageMultiplier != 1.5F) {
            object.addProperty("critDamageMultiplier", this.critDamageMultiplier);
         }

         if (this.playerKnockBack) {
            object.addProperty("playerKnockBack", true);
         }

         if (this.playerKnockBackStrength != 0.0F) {
            object.addProperty("playerKnockBackStrength", this.playerKnockBackStrength);
         }

         if (this.durabilityDamage != 1) {
            object.addProperty("durabilityDamage", this.durabilityDamage);
         }

         return object;
      }

      public Gun.Projectile copy() {
         Gun.Projectile projectile = new Gun.Projectile();
         projectile.energyUse = this.energyUse;
         projectile.item = this.item;
         projectile.ejectsCasing = this.ejectsCasing;
         projectile.visible = this.visible;
         projectile.damage = this.damage;
         projectile.armorPen = this.armorPen;
         projectile.advantage = this.advantage;
         projectile.size = this.size;
         projectile.speed = this.speed;
         projectile.life = this.life;
         projectile.gravity = this.gravity;
         projectile.damageReduceOverLife = this.damageReduceOverLife;
         projectile.trailColor = this.trailColor;
         projectile.trailLengthMultiplier = this.trailLengthMultiplier;
         projectile.casingType = this.casingType;
         projectile.casingParticle = this.casingParticle;
         projectile.ejectDuringReload = this.ejectDuringReload;
         projectile.firesArrows = this.firesArrows;
         projectile.impactEffect = this.impactEffect;
         projectile.impactEffectDuration = this.impactEffectDuration;
         projectile.impactEffectAmplifier = this.impactEffectAmplifier;
         projectile.impactEffectChance = this.impactEffectChance;
         projectile.isSoulFire = this.isSoulFire;
         projectile.hideProjectile = this.hideProjectile;
         projectile.hideTrail = this.hideTrail;
         projectile.trailThickness = this.trailThickness;
         projectile.knockbackStrength = this.knockbackStrength;
         projectile.damageFalloffStart = this.damageFalloffStart;
         projectile.damageFalloffEnd = this.damageFalloffEnd;
         projectile.damageFalloffMinMultiplier = this.damageFalloffMinMultiplier;
         projectile.projectileAmount = this.projectileAmount;
         projectile.recoilAngle = this.recoilAngle;
         projectile.recoilKick = this.recoilKick;
         projectile.spread = this.spread;
         projectile.alwaysSpread = this.alwaysSpread;
         projectile.criticalChance = this.criticalChance;
         projectile.critDamageMultiplier = this.critDamageMultiplier;
         projectile.playerKnockBack = this.playerKnockBack;
         projectile.playerKnockBackStrength = this.playerKnockBackStrength;
         projectile.durabilityDamage = this.durabilityDamage;
         return projectile;
      }

      public int getDurabilityDamage() {
         return this.durabilityDamage;
      }

      public float getKnockbackStrength() {
         return this.knockbackStrength;
      }

      public boolean shouldHideTrail() {
         return this.hideTrail;
      }

      public double getTrailThickness() {
         return this.trailThickness;
      }

      public float getArmorPen() {
         return this.armorPen;
      }

      public boolean isSoulFire() {
         return this.isSoulFire;
      }

      @Nullable
      public ResourceLocation getImpactEffect() {
         return this.impactEffect;
      }

      public int getImpactEffectDuration() {
         return this.impactEffectDuration;
      }

      public int getImpactEffectAmplifier() {
         return this.impactEffectAmplifier;
      }

      public float getImpactEffectChance() {
         return this.impactEffectChance;
      }

      @org.jetbrains.annotations.Nullable
      public Item getItem() {
         return (Item)BuiltInRegistries.ITEM.get(this.item);
      }

      @org.jetbrains.annotations.Nullable
      public Item getItem(Gun.General general) {
         if (general != null && general.allowsAmmoChange() && !general.getAvailableAmmoTypes().isEmpty()) {
            int currentIndex = general.getCurrentAmmoTypeIndex();
            if (currentIndex >= 0 && currentIndex < general.getAvailableAmmoTypes().size()) {
               String ammoType = general.getAvailableAmmoTypes().get(currentIndex);
               ResourceLocation ammoLocation = ResourceLocation.parse(ammoType.contains(":") ? ammoType : "scguns:" + ammoType);
               return (Item)BuiltInRegistries.ITEM.get(ammoLocation);
            }
         }

         return this.getItem();
      }

      public boolean firesArrows() {
         return this.firesArrows;
      }

      public int getEnergyUse() {
         return this.energyUse;
      }

      public void setEnergyUse(int energyUse) {
         this.energyUse = energyUse;
      }

      public boolean ejectsCasing() {
         return this.ejectsCasing;
      }

      public void setCasingType(ResourceLocation casingType) {
         this.casingType = casingType;
      }

      public ResourceLocation getCasingType() {
         return this.casingType;
      }

      public boolean ejectDuringReload() {
         return this.ejectDuringReload;
      }

      public boolean isVisible() {
         return !this.visible;
      }

      public float getDamage() {
         return this.damage;
      }

      public ResourceLocation getAdvantage() {
         return this.advantage;
      }

      public float getSize() {
         return this.size;
      }

      public double getSpeed() {
         return this.speed;
      }

      public int getLife() {
         return this.life;
      }

      public boolean isGravity() {
         return this.gravity;
      }

      public boolean isDamageReduceOverLife() {
         return this.damageReduceOverLife;
      }

      public int getTrailColor() {
         return this.trailColor;
      }

      public boolean shouldHideProjectile() {
         return this.hideProjectile;
      }

      public double getTrailLengthMultiplier() {
         return this.trailLengthMultiplier;
      }

      public void setDamage(float v) {
         this.damage = v;
      }

      public float getSpread() {
         return this.spread;
      }

      public int getProjectileAmount() {
         return this.projectileAmount;
      }

      public float getRecoilAngle() {
         return this.recoilAngle;
      }

      public float getRecoilKick() {
         return this.recoilKick;
      }

      public boolean isAlwaysSpread() {
         return this.alwaysSpread;
      }

      public float getCriticalChance() {
         return this.criticalChance;
      }

      public float getCritDamageMultiplier() {
         return this.critDamageMultiplier;
      }

      public boolean hasPlayerKnockBack() {
         return this.playerKnockBack;
      }

      public float getPlayerKnockBackStrength() {
         return this.playerKnockBackStrength;
      }

      public ResourceLocation getCasingParticle() {
         return this.casingParticle;
      }

      public float getDamageFalloffStart() {
         return this.damageFalloffStart;
      }

      public float getDamageFalloffEnd() {
         return this.damageFalloffEnd;
      }

      public float getDamageFalloffMinMultiplier() {
         return this.damageFalloffMinMultiplier;
      }
   }

   public static class Reloads  {
      @Optional
      @Ignored
      private ResourceLocation reloadItem = ResourceLocation.fromNamespaceAndPath("scguns", "scrap");
      private int maxAmmo = 30;
      @Ignored
      private ReloadType reloadType = ReloadType.MANUAL;
      private int reloadTimer = 20;
      private int emptyMagTimer = 5;
      private int reloadAmount = 1;
      @Optional
      private boolean infiniteAmmo = false;
      @Optional
      private ResourceLocation reloadByproduct;
      @Optional
      private float byproductChance = 0.0F;

      public Reloads() {
         super();
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         tag.putString("ReloadItem", this.reloadItem.toString());
         tag.putInt("MaxAmmo", this.maxAmmo);
         tag.putString("ReloadType", this.reloadType.id().toString());
         tag.putInt("ReloadTimer", this.reloadTimer);
         tag.putInt("EmptyMagTimer", this.emptyMagTimer);
         tag.putInt("ReloadAmount", this.reloadAmount);
         tag.putFloat("ByproductChance", this.byproductChance);
         if (this.reloadByproduct != null) {
            tag.putString("ReloadByproduct", this.reloadByproduct.toString());
         }

         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("ReloadItem", 8)) {
            this.reloadItem = ResourceLocation.parse(tag.getString("ReloadItem"));
         }

         if (tag.contains("MaxAmmo", 99)) {
            this.maxAmmo = tag.getInt("MaxAmmo");
         }

         if (tag.contains("ReloadType", 8)) {
            this.reloadType = ReloadType.getType(ResourceLocation.tryParse(tag.getString("ReloadType")));
         }

         if (tag.contains("ReloadTimer", 99)) {
            this.reloadTimer = tag.getInt("ReloadTimer");
         }

         if (tag.contains("EmptyMagTimer", 99)) {
            this.emptyMagTimer = tag.getInt("EmptyMagTimer");
         }

         if (tag.contains("ReloadAmount", 99)) {
            this.reloadAmount = tag.getInt("ReloadAmount");
         }

         if (tag.contains("ByproductChance", 99)) {
            this.byproductChance = tag.getFloat("ByproductChance");
         }

         if (tag.contains("ReloadByproduct", 8)) {
            this.reloadByproduct = ResourceLocation.parse(tag.getString("ReloadByproduct"));
         }
      }

      public JsonObject toJsonObject() {
         Preconditions.checkArgument(this.maxAmmo > 0, "Max ammo must be more than zero");
         Preconditions.checkArgument(this.reloadTimer >= 0, "Reload timer must be more than or equal to zero");
         Preconditions.checkArgument(this.emptyMagTimer >= 0, "Empty mag additional reload timer must be more than or equal to zero");
         Preconditions.checkArgument(this.reloadAmount >= 1, "Reloading amount must be more than or equal to zero");
         JsonObject object = new JsonObject();
         if (this.reloadItem != null) {
            object.addProperty("reloadItem", this.reloadItem.toString());
         }

         object.addProperty("maxAmmo", this.maxAmmo);
         object.addProperty("reloadType", this.reloadType.id().toString());
         object.addProperty("reloadTimer", this.reloadTimer);
         object.addProperty("emptyMagTimer", this.emptyMagTimer);
         if (this.reloadAmount != 1) {
            object.addProperty("reloadAmount", this.reloadAmount);
         }

         if (this.byproductChance != 0.0F) {
            object.addProperty("byproductChance", this.byproductChance);
         }

         if (this.reloadByproduct != null) {
            object.addProperty("reloadByproduct", this.reloadByproduct.toString());
         }

         return object;
      }

      public Gun.Reloads copy() {
         Gun.Reloads reloads = new Gun.Reloads();
         reloads.reloadItem = this.reloadItem;
         reloads.maxAmmo = this.maxAmmo;
         reloads.reloadType = this.reloadType;
         reloads.reloadTimer = this.reloadTimer;
         reloads.emptyMagTimer = this.emptyMagTimer;
         reloads.reloadAmount = this.reloadAmount;
         reloads.byproductChance = this.byproductChance;
         reloads.reloadByproduct = this.reloadByproduct;
         return reloads;
      }

      public static boolean hasInfiniteAmmo(ItemStack gunStack) {
         CompoundTag tag = NbtHelper.getOrCreateTag(gunStack);
         Gun modifiedGun = ((GunItem)gunStack.getItem()).getModifiedGun(gunStack);
         return tag.getBoolean("IgnoreAmmo") || modifiedGun.getGeneral().getInfiniteAmmo();
      }

      @Nullable
      public Item getReloadByproduct() {
         return this.reloadByproduct != null ? (Item)BuiltInRegistries.ITEM.get(this.reloadByproduct) : null;
      }

      public float getByproductChance() {
         return this.byproductChance;
      }

      public void setByproductChance(float chance) {
         this.byproductChance = Mth.clamp(chance, 0.0F, 1.0F);
      }

      public boolean shouldGiveByproduct(RandomSource random, ItemStack gunStack) {
         if (this.reloadByproduct != null && !(this.byproductChance <= 0.0F)) {
            float finalChance = this.byproductChance;
            int shellCatcherLevel = ScEnchants.level(gunStack, ModEnchantments.SHELL_CATCHER);
            if (shellCatcherLevel > 0) {
               finalChance += (float)shellCatcherLevel * 0.05F;
               finalChance = Math.min(finalChance, 1.0F);
            }

            return random.nextFloat() < finalChance;
         } else {
            return false;
         }
      }

      public void setReloadByproduct(ResourceLocation item) {
         this.reloadByproduct = item;
      }

      public boolean getInfiniteAmmo() {
         return this.infiniteAmmo;
      }

      public Item getReloadItem() {
         return (Item)BuiltInRegistries.ITEM.get(this.reloadItem);
      }

      public int getMaxAmmo() {
         return this.maxAmmo;
      }

      public ReloadType getReloadType() {
         return this.reloadType;
      }

      public int getReloadTimer() {
         return this.reloadTimer;
      }

      public int getEmptyMagTimer() {
         return this.emptyMagTimer;
      }

      public int getReloadAmount() {
         return this.reloadAmount;
      }
   }

   public static class ScaledPositioned extends Gun.Positioned {
      @Optional
      protected double scale = 1.0;

      public ScaledPositioned() {
         super();
      }

      public ScaledPositioned(CompoundTag tag) {
         super();
         this.deserializeNBT(tag);
      }

      @Override
      public CompoundTag serializeNBT() {
         CompoundTag tag = super.serializeNBT();
         tag.putDouble("Scale", this.scale);
         return tag;
      }

      @Override
      public void deserializeNBT(CompoundTag tag) {
         super.deserializeNBT(tag);
         if (tag.contains("Scale", 99)) {
            this.scale = tag.getDouble("Scale");
         }
      }

      @Override
      public JsonObject toJsonObject() {
         JsonObject object = super.toJsonObject();
         if (this.scale != 1.0) {
            object.addProperty("scale", this.scale);
         }

         return object;
      }

      public double getScale() {
         return this.scale;
      }

      public Gun.ScaledPositioned copy() {
         Gun.ScaledPositioned positioned = new Gun.ScaledPositioned();
         positioned.xOffset = this.xOffset;
         positioned.yOffset = this.yOffset;
         positioned.zOffset = this.zOffset;
         positioned.scale = this.scale;
         return positioned;
      }
   }

   public static class Sounds  {
      @Optional
      @Nullable
      private ResourceLocation fire;
      @Optional
      @Nullable
      private ResourceLocation reload;
      @Optional
      @Nullable
      private ResourceLocation cock;
      @Optional
      @Nullable
      private ResourceLocation silencedFire;
      @Optional
      @Nullable
      private ResourceLocation enchantedFire;
      @Optional
      @Nullable
      private ResourceLocation preFire;
      @Optional
      @Nullable
      private ResourceLocation flyby;
      @Optional
      @Nullable
      private ResourceLocation preReload;

      public Sounds() {
         super();
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         if (this.fire != null) {
            tag.putString("Fire", this.fire.toString());
         }

         if (this.reload != null) {
            tag.putString("Reloads", this.reload.toString());
         }

         if (this.cock != null) {
            tag.putString("Cock", this.cock.toString());
         }

         if (this.silencedFire != null) {
            tag.putString("SilencedFire", this.silencedFire.toString());
         }

         if (this.enchantedFire != null) {
            tag.putString("EnchantedFire", this.enchantedFire.toString());
         }

         if (this.preFire != null) {
            tag.putString("PreFire", this.preFire.toString());
         }

         if (this.preReload != null) {
            tag.putString("PreReload", this.preReload.toString());
         }

         if (this.flyby != null) {
            tag.putString("Flyby", this.flyby.toString());
         }

         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("Fire", 8)) {
            this.fire = this.createSound(tag, "Fire");
         }

         if (tag.contains("Reloads", 8)) {
            this.reload = this.createSound(tag, "Reloads");
         }

         if (tag.contains("Cock", 8)) {
            this.cock = this.createSound(tag, "Cock");
         }

         if (tag.contains("SilencedFire", 8)) {
            this.silencedFire = this.createSound(tag, "SilencedFire");
         }

         if (tag.contains("EnchantedFire", 8)) {
            this.enchantedFire = this.createSound(tag, "EnchantedFire");
         }

         if (tag.contains("PreFire", 8)) {
            this.preFire = this.createSound(tag, "PreFire");
         }

         if (tag.contains("PreReload", 8)) {
            this.preReload = this.createSound(tag, "PreReload");
         }

         if (tag.contains("Flyby", 8)) {
            this.flyby = this.createSound(tag, "Flyby");
         }
      }

      public JsonObject toJsonObject() {
         JsonObject object = new JsonObject();
         if (this.fire != null) {
            object.addProperty("fire", this.fire.toString());
         }

         if (this.reload != null) {
            object.addProperty("reload", this.reload.toString());
         }

         if (this.cock != null) {
            object.addProperty("cock", this.cock.toString());
         }

         if (this.silencedFire != null) {
            object.addProperty("silencedFire", this.silencedFire.toString());
         }

         if (this.enchantedFire != null) {
            object.addProperty("enchantedFire", this.enchantedFire.toString());
         }

         if (this.preFire != null) {
            object.addProperty("preFire", this.preFire.toString());
         }

         if (this.preReload != null) {
            object.addProperty("preReload", this.preReload.toString());
         }

         return object;
      }

      public Gun.Sounds copy() {
         Gun.Sounds sounds = new Gun.Sounds();
         sounds.fire = this.fire;
         sounds.reload = this.reload;
         sounds.cock = this.cock;
         sounds.silencedFire = this.silencedFire;
         sounds.enchantedFire = this.enchantedFire;
         sounds.preFire = this.preFire;
         sounds.preReload = this.preReload;
         sounds.flyby = this.flyby;
         return sounds;
      }

      @Nullable
      private ResourceLocation createSound(CompoundTag tag, String key) {
         String sound = tag.getString(key);
         return sound.isEmpty() ? null : ResourceLocation.parse(sound);
      }

      @Nullable
      public ResourceLocation getFire() {
         return this.fire;
      }

      @Nullable
      public ResourceLocation getReload() {
         return this.reload;
      }

      @Nullable
      public ResourceLocation getCock() {
         return this.cock;
      }

      @Nullable
      public ResourceLocation getSilencedFire() {
         return this.silencedFire;
      }

      @Nullable
      public ResourceLocation getEnchantedFire() {
         return this.enchantedFire;
      }

      @Nullable
      public ResourceLocation getPreFire() {
         return this.preFire;
      }

      @Nullable
      public ResourceLocation getPreReload() {
         return this.preReload;
      }

      @Nullable
      public ResourceLocation getFlybySound() {
         return this.flyby;
      }
   }

   public static enum WeaponType {
      pistol,
      magnum,
      smg,
      rifle,
      lmg,
      shotgun,
      sniper,
      heavy,
      flamethrower,
      shock,
      plasma,
      laser,
      special;

      private WeaponType() {
      }
   }
}
