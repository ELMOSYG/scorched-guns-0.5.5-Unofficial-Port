package top.ribs.scguns.item;



import top.ribs.scguns.util.NbtHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.Config;
import top.ribs.scguns.item.ammo_boxes.CreativeAmmoBoxItem;

public abstract class AmmoBoxItem extends Item {
   public static final String TAG_ITEMS = "Items";
   private static final int BAR_COLOR = Mth.color(0.4F, 0.4F, 1.0F);

   public AmmoBoxItem(Properties properties) {
      super(properties);
   }

   public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
      if (stack.getCount() == 1 && action == ClickAction.SECONDARY) {
         ItemStack itemStackInSlot = slot.getItem();
         if (itemStackInSlot.isEmpty()) {
            this.playRemoveOneSound(player);
            removeOne(stack).ifPresent(removedStack -> add(stack, slot.safeInsert(removedStack)));
         } else if (itemStackInSlot.is(ItemTags.create(this.getAmmoTag()))) {
            int maxInsertCount = getMaxItemCount(stack) - getTotalItemCount(stack);
            int itemsToInsert = Math.min(itemStackInSlot.getCount(), maxInsertCount);
            int insertedItems = add(stack, slot.safeTake(itemStackInSlot.getCount(), itemsToInsert, player));
            if (insertedItems > 0) {
               this.playInsertSound(player);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack otherStack, Slot slot, ClickAction action, Player player, SlotAccess slotAccess) {
      if (stack.getCount() != 1) {
         return false;
      } else if (action == ClickAction.SECONDARY && slot.allowModification(player)) {
         if (otherStack.isEmpty()) {
            removeOne(stack).ifPresent(removedStack -> {
               this.playRemoveOneSound(player);
               slotAccess.set(removedStack);
            });
         } else if (otherStack.is(ItemTags.create(this.getAmmoTag()))) {
            int maxInsertCount = getMaxItemCount(stack) - getTotalItemCount(stack);
            int itemsToInsert = Math.min(otherStack.getCount(), maxInsertCount);
            int insertedItems = add(stack, otherStack.copyWithCount(itemsToInsert));
            if (insertedItems > 0) {
               this.playInsertSound(player);
               otherStack.shrink(insertedItems);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean isBarVisible(@NotNull ItemStack stack) {
      return getTotalItemCount(stack) > 0;
   }

   public int getBarWidth(@NotNull ItemStack stack) {
      return Math.min(1 + 12 * getTotalItemCount(stack) / getMaxItemCount(stack), 13);
   }

   protected abstract ResourceLocation getAmmoTag();

   public int getBarColor(ItemStack stack) {
      return BAR_COLOR;
   }

   public static int add(ItemStack pouchStack, ItemStack insertedStack) {
      if (!insertedStack.isEmpty() && insertedStack.is(ItemTags.create(((AmmoBoxItem)pouchStack.getItem()).getAmmoTag()))) {
         CompoundTag compoundTag = NbtHelper.getOrCreateTag(pouchStack);
         if (!compoundTag.contains("Items")) {
            compoundTag.put("Items", new ListTag());
         }

         int maxItemCount = getMaxItemCount(pouchStack);
         int itemsToInsert = Math.min(insertedStack.getCount(), maxItemCount - getTotalItemCount(pouchStack));
         if (itemsToInsert == 0) {
            return 0;
         } else {
            ListTag listTag = compoundTag.getList("Items", 10);

            for (int i = 0; i < listTag.size(); i++) {
               CompoundTag itemTag = listTag.getCompound(i);
               ItemStack existingStack = top.ribs.scguns.util.NbtHelper.itemFromTag(itemTag);
               if (ItemStack.isSameItemSameComponents(existingStack, insertedStack)) {
                  int remainingSpace = Math.min(existingStack.getMaxStackSize() - existingStack.getCount(), itemsToInsert);
                  existingStack.grow(remainingSpace);
                  itemsToInsert -= remainingSpace;
                  listTag.set(i, NbtHelper.tagFromItem(existingStack));
                  if (itemsToInsert <= 0) {
                     break;
                  }
               }
            }

            while (itemsToInsert > 0) {
               int countToInsert = Math.min(insertedStack.getMaxStackSize(), itemsToInsert);
               ItemStack newItemStack = insertedStack.copyWithCount(countToInsert);
               listTag.add(NbtHelper.tagFromItem(newItemStack));
               itemsToInsert -= countToInsert;
            }

            compoundTag.put("Items", listTag);
            return insertedStack.getCount() - itemsToInsert;
         }
      } else {
         return 0;
      }
   }

   public static int getTotalItemCount(ItemStack stack) {
      return getContents(stack).mapToInt(ItemStack::getCount).sum();
   }

   private static Optional<ItemStack> removeOne(ItemStack stack) {
      CompoundTag compoundTag = NbtHelper.getOrCreateTag(stack);
      if (!compoundTag.contains("Items")) {
         return Optional.empty();
      } else {
         ListTag listTag = compoundTag.getList("Items", 10);
         if (listTag.isEmpty()) {
            return Optional.empty();
         } else {
            CompoundTag itemTag = listTag.getCompound(0);
            ItemStack itemStack = top.ribs.scguns.util.NbtHelper.itemFromTag(itemTag);
            listTag.remove(0);
            if (listTag.isEmpty()) {
               // 1.21 has no ItemStack#removeTagKey; drop the key from the live custom-data tag
               // and let NbtHelper.setTag clear the component once the tag is empty, which is
               // exactly what removeTagKey used to do.
               CompoundTag boxTag = NbtHelper.getTagForWrite(stack);
               if (boxTag != null && boxTag.contains("Items")) {
                  boxTag.remove("Items");
                  NbtHelper.setTag(stack, boxTag);
               }
            }

            return Optional.of(itemStack);
         }
      }
   }

   @NotNull
   public Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
      NonNullList<ItemStack> nonNullList = NonNullList.create();
      getContents(stack).forEach(nonNullList::add);
      // 1.21 BundleTooltip takes a BundleContents (it derives the weight itself) instead of
      // the old (list, totalCount) pair.
      return Optional.of(new BundleTooltip(new BundleContents(nonNullList)));
   }

   public void onDestroyed(@NotNull ItemEntity itemEntity) {
      ItemUtils.onContainerDestroyed(itemEntity, getContents(itemEntity.getItem()).toList());
   }

   @Override
   public void appendHoverText(@NotNull ItemStack stack, Item.TooltipContext level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
      if (Screen.hasShiftDown()) {
         tooltipComponents.add(Component.translatable(this.getDescriptionKey()).withStyle(ChatFormatting.GRAY));
      } else {
         tooltipComponents.add(Component.translatable("tooltip.scguns.hold_shift").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}));
      }
   }

   protected abstract String getDescriptionKey();

   private void playRemoveOneSound(Entity entity) {
      entity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
   }

   protected void playInsertSound(Entity entity) {
      entity.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
   }

   public static int getMaxItemCount(ItemStack stack) {
      Item item = stack.getItem();
      if (item instanceof AmmoBoxItem) {
         double multiplier = (Double)Config.COMMON.gameplay.ammoBoxCapacityMultiplier.get();
         return (int)((double)((AmmoBoxItem)item).getBaseMaxItemCount() * multiplier);
      } else {
         return 256;
      }
   }

   protected abstract int getBaseMaxItemCount();

   public static Stream<ItemStack> getContents(ItemStack stack) {
      if (stack.getItem() instanceof CreativeAmmoBoxItem) {
         TagKey<Item> ammoTag = ItemTags.create(((CreativeAmmoBoxItem)stack.getItem()).getAmmoTag());
         return BuiltInRegistries.ITEM
            .stream()
            .filter(item -> item.builtInRegistryHolder().is(ammoTag))
            .map(item -> new ItemStack(item, Integer.MAX_VALUE));
      } else {
         CompoundTag compoundTag = NbtHelper.getTag(stack);
         if (compoundTag == null) {
            return Stream.empty();
         } else {
            ListTag listTag = compoundTag.getList("Items", 10);
            return listTag.stream().map(CompoundTag.class::cast).map(NbtHelper::itemFromTag);
         }
      }
   }
}
