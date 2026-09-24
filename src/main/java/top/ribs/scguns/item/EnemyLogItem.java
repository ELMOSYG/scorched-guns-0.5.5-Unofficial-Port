package top.ribs.scguns.item;




import net.minecraft.world.item.Item;
import top.ribs.scguns.util.NbtHelper;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

public class EnemyLogItem extends TeamLogItem {
   public EnemyLogItem(Properties properties) {
      super(properties);
   }

   @Override
   public void appendHoverText(ItemStack stack, Item.TooltipContext level, List<Component> tooltip, TooltipFlag flag) {
      CompoundTag tag = NbtHelper.getTag(stack);
      if (tag != null) {
         if (tag.contains("Whitelist", 9)) {
            ListTag listTag = tag.getList("Whitelist", 10);
            if (!listTag.isEmpty()) {
               tooltip.add(Component.literal("Whitelisted Entities:"));

               for (int i = 0; i < listTag.size(); i++) {
                  CompoundTag entityTag = listTag.getCompound(i);
                  String entityName = entityTag.getString("Name");
                  String entityType = entityTag.getString("EntityType");
                  tooltip.add(Component.literal("- " + entityName + " (" + entityType + ")"));
               }
            }
         }

         if (tag.contains("WhitelistEntityTypes", 9)) {
            ListTag whitelistTag = tag.getList("WhitelistEntityTypes", 8);
            if (!whitelistTag.isEmpty()) {
               tooltip.add(Component.literal("Whitelisted Entity Types:"));

               for (int i = 0; i < whitelistTag.size(); i++) {
                  String entityType = whitelistTag.getString(i);
                  tooltip.add(Component.literal("- " + entityType));
               }
            }
         }
      } else {
         tooltip.add(Component.literal("No entities or types whitelisted"));
      }
   }

   @NotNull
   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (!level.isClientSide()) {
         EntityHitResult hitResult = this.rayTraceEntities(level, player);
         if (hitResult != null && hitResult.getEntity() instanceof LivingEntity targetEntity) {
            if (player.isShiftKeyDown()) {
               boolean added = this.addEntityTypeToWhitelist(itemStack, targetEntity);
               if (added) {
                  player.displayClientMessage(Component.literal("Added all " + targetEntity.getType().getDescription().getString() + " to whitelist"), true);
               } else {
                  player.displayClientMessage(Component.literal(targetEntity.getType().getDescription().getString() + " is already whitelisted"), true);
               }
            } else {
               boolean added = this.addEntityToWhitelist(itemStack, targetEntity);
               if (added) {
                  player.displayClientMessage(Component.literal("Added " + targetEntity.getName().getString() + " to Whitelist"), true);
               } else {
                  player.displayClientMessage(Component.literal(targetEntity.getName().getString() + " is already in Whitelist"), true);
               }
            }

            this.logCurrentWhitelist(itemStack, player);
         }
      }

      return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
   }

   boolean addEntityToWhitelist(ItemStack stack, LivingEntity targetEntity) {
      CompoundTag tag = NbtHelper.getOrCreateTag(stack);
      ListTag listTag = tag.getList("Whitelist", 10);

      for (int i = 0; i < listTag.size(); i++) {
         CompoundTag existingTag = listTag.getCompound(i);
         if (existingTag.getUUID("UUID").equals(targetEntity.getUUID())) {
            return false;
         }
      }

      CompoundTag entityTag = new CompoundTag();
      entityTag.putUUID("UUID", targetEntity.getUUID());
      entityTag.putString("Name", targetEntity.getName().getString());
      entityTag.putString("EntityType", EntityType.getKey(targetEntity.getType()).toString());
      listTag.add(entityTag);
      tag.put("Whitelist", listTag);
      NbtHelper.setTag(stack, tag);
      return true;
   }

   boolean addEntityTypeToWhitelist(ItemStack stack, LivingEntity targetEntity) {
      CompoundTag tag = NbtHelper.getOrCreateTag(stack);
      ListTag whitelistTag = tag.getList("WhitelistEntityTypes", 8);
      String entityTypeKey = EntityType.getKey(targetEntity.getType()).toString();

      for (int i = 0; i < whitelistTag.size(); i++) {
         if (whitelistTag.getString(i).equals(entityTypeKey)) {
            return false;
         }
      }

      whitelistTag.add(StringTag.valueOf(entityTypeKey));
      tag.put("WhitelistEntityTypes", whitelistTag);
      NbtHelper.setTag(stack, tag);
      return true;
   }

   void logCurrentWhitelist(ItemStack stack, Player player) {
      CompoundTag tag = NbtHelper.getTag(stack);
      if (tag != null) {
         if (tag.contains("Whitelist", 9)) {
            ListTag listTag = tag.getList("Whitelist", 10);
            if (!listTag.isEmpty()) {
               player.displayClientMessage(Component.literal("Entities in Whitelist:"), true);

               for (int i = 0; i < listTag.size(); i++) {
                  CompoundTag entityTag = listTag.getCompound(i);
                  String entityName = entityTag.getString("Name");
                  String entityType = entityTag.getString("EntityType");
                  player.displayClientMessage(Component.literal("- " + entityName + " (" + entityType + ")"), true);
               }
            } else {
               player.displayClientMessage(Component.literal("No specific entities whitelisted."), true);
            }
         }

         if (tag.contains("WhitelistEntityTypes", 9)) {
            ListTag whitelistTag = tag.getList("WhitelistEntityTypes", 8);
            if (!whitelistTag.isEmpty()) {
               player.displayClientMessage(Component.literal("Whitelisted Entity Types:"), true);

               for (int i = 0; i < whitelistTag.size(); i++) {
                  String entityType = whitelistTag.getString(i);
                  player.displayClientMessage(Component.literal("- " + entityType), true);
               }
            } else {
               player.displayClientMessage(Component.literal("No entity types whitelisted."), true);
            }
         }
      } else {
         player.displayClientMessage(Component.literal("No entities or types logged in the item."), true);
      }
   }
}
