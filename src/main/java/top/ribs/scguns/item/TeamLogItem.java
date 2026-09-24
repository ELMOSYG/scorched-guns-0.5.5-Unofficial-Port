package top.ribs.scguns.item;



import top.ribs.scguns.util.NbtHelper;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class TeamLogItem extends Item {
   public TeamLogItem(Properties properties) {
      super(properties);
   }

   @NotNull
   public InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (!level.isClientSide()) {
         EntityHitResult hitResult = this.rayTraceEntities(level, player);
         if (hitResult != null && hitResult.getEntity() instanceof LivingEntity targetEntity) {
            if (player.isShiftKeyDown()) {
               boolean added = this.addEntityTypeToBlacklist(itemStack, targetEntity);
               if (added) {
                  player.displayClientMessage(Component.literal("Added all " + targetEntity.getType().getDescription().getString() + " to blacklist"), true);
               } else {
                  player.displayClientMessage(Component.literal(targetEntity.getType().getDescription().getString() + " is already blacklisted"), true);
               }
            } else {
               boolean added = this.addEntityToTeamLog(itemStack, targetEntity);
               if (added) {
                  player.displayClientMessage(Component.literal("Added " + targetEntity.getName().getString() + " to Team Log"), true);
               } else {
                  player.displayClientMessage(Component.literal(targetEntity.getName().getString() + " is already in Team Log"), true);
               }
            }

            this.logCurrentEntities(itemStack, player);
         }
      }

      return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
   }

   EntityHitResult rayTraceEntities(Level level, Player player) {
      Vec3 eyePosition = player.getEyePosition();
      Vec3 lookVector = player.getLookAngle();
      Vec3 endPos = eyePosition.add(lookVector.scale(5.0));
      AABB searchBox = player.getBoundingBox().expandTowards(lookVector.scale(5.0)).inflate(1.0, 1.0, 1.0);
      List<Entity> entities = level.getEntities(player, searchBox, entityx -> entityx instanceof LivingEntity && entityx != player);
      EntityHitResult closestHitResult = null;
      double closestDistance = Double.MAX_VALUE;

      for (Entity entity : entities) {
         AABB boundingBox = entity.getBoundingBox();
         Vec3 intercept = (Vec3)boundingBox.clip(eyePosition, endPos).orElse(null);
         if (intercept != null) {
            double distance = eyePosition.distanceTo(intercept);
            if (distance < closestDistance) {
               closestDistance = distance;
               closestHitResult = new EntityHitResult(entity, intercept);
            }
         }
      }

      return closestHitResult;
   }

   boolean addEntityToTeamLog(ItemStack stack, LivingEntity targetEntity) {
      CompoundTag tag = NbtHelper.getOrCreateTag(stack);
      ListTag listTag = tag.getList("Entities", 10);

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
      tag.put("Entities", listTag);
      NbtHelper.setTag(stack, tag);
      return true;
   }

   boolean addEntityTypeToBlacklist(ItemStack stack, LivingEntity targetEntity) {
      CompoundTag tag = NbtHelper.getOrCreateTag(stack);
      ListTag blacklistTag = tag.getList("Blacklist", 8);
      String entityTypeKey = EntityType.getKey(targetEntity.getType()).toString();

      for (int i = 0; i < blacklistTag.size(); i++) {
         if (blacklistTag.getString(i).equals(entityTypeKey)) {
            return false;
         }
      }

      blacklistTag.add(StringTag.valueOf(entityTypeKey));
      tag.put("Blacklist", blacklistTag);
      NbtHelper.setTag(stack, tag);
      return true;
   }

   void logCurrentEntities(ItemStack stack, Player player) {
      CompoundTag tag = NbtHelper.getTag(stack);
      if (tag != null) {
         if (tag.contains("Entities", 9)) {
            ListTag listTag = tag.getList("Entities", 10);
            if (!listTag.isEmpty()) {
               player.displayClientMessage(Component.literal("Entities in Team Log:"), true);

               for (int i = 0; i < listTag.size(); i++) {
                  CompoundTag entityTag = listTag.getCompound(i);
                  String entityName = entityTag.getString("Name");
                  String entityType = entityTag.getString("EntityType");
                  player.displayClientMessage(Component.literal("- " + entityName + " (" + entityType + ")"), true);
               }
            } else {
               player.displayClientMessage(Component.literal("No specific entities logged."), true);
            }
         }

         if (tag.contains("Blacklist", 9)) {
            ListTag blacklistTag = tag.getList("Blacklist", 8);
            if (!blacklistTag.isEmpty()) {
               player.displayClientMessage(Component.literal("Blacklisted Entity Types:"), true);

               for (int i = 0; i < blacklistTag.size(); i++) {
                  String entityType = blacklistTag.getString(i);
                  player.displayClientMessage(Component.literal("- " + entityType), true);
               }
            } else {
               player.displayClientMessage(Component.literal("No entity types blacklisted."), true);
            }
         }
      } else {
         player.displayClientMessage(Component.literal("No entities or types logged in the item."), true);
      }
   }

   @Override
   public void appendHoverText(ItemStack stack, Item.TooltipContext level, List<Component> tooltip, TooltipFlag flag) {
      CompoundTag tag = NbtHelper.getTag(stack);
      if (tag != null) {
         if (tag.contains("Entities", 9)) {
            ListTag listTag = tag.getList("Entities", 10);
            if (!listTag.isEmpty()) {
               tooltip.add(Component.literal("Logged Entities:"));

               for (int i = 0; i < listTag.size(); i++) {
                  CompoundTag entityTag = listTag.getCompound(i);
                  String entityName = entityTag.getString("Name");
                  String entityType = entityTag.getString("EntityType");
                  tooltip.add(Component.literal("- " + entityName + " (" + entityType + ")"));
               }
            }
         }

         if (tag.contains("Blacklist", 9)) {
            ListTag blacklistTag = tag.getList("Blacklist", 8);
            if (!blacklistTag.isEmpty()) {
               tooltip.add(Component.literal("Blacklisted Entity Types:"));

               for (int i = 0; i < blacklistTag.size(); i++) {
                  String entityType = blacklistTag.getString(i);
                  tooltip.add(Component.literal("- " + entityType));
               }
            }
         }
      } else {
         tooltip.add(Component.literal("No entities or types logged"));
      }
   }

   public boolean isFoil(@NotNull ItemStack stack) {
      CompoundTag tag = NbtHelper.getTag(stack);
      if (tag != null) {
         if (tag.contains("Entities", 9)) {
            ListTag listTag = tag.getList("Entities", 10);
            if (!listTag.isEmpty()) {
               return true;
            }
         }

         if (tag.contains("Blacklist", 9)) {
            ListTag blacklistTag = tag.getList("Blacklist", 8);
            if (!blacklistTag.isEmpty()) {
               return true;
            }
         }

         if (tag.contains("Whitelist", 9)) {
            ListTag whitelistTag = tag.getList("Whitelist", 10);
            if (!whitelistTag.isEmpty()) {
               return true;
            }
         }

         if (tag.contains("WhitelistEntityTypes", 9)) {
            ListTag whitelistTypeTag = tag.getList("WhitelistEntityTypes", 8);
            if (!whitelistTypeTag.isEmpty()) {
               return true;
            }
         }
      }

      return false;
   }

   public ItemStack getCraftingRemainingItem(ItemStack itemStack) {
      return ItemStack.EMPTY;
   }

   public boolean hasCraftingRemainingItem(ItemStack stack) {
      return false;
   }
}
