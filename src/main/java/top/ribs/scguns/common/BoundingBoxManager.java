package top.ribs.scguns.common;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.bus.api.SubscribeEvent;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.headshot.BasicHeadshotBox;
import top.ribs.scguns.common.headshot.ChildHeadshotBox;
import top.ribs.scguns.common.headshot.DynamicHeadshotBox;
import top.ribs.scguns.common.headshot.NoChildHeadshotBox;
import top.ribs.scguns.common.headshot.NoChildRotatedHeadshotBox;
import top.ribs.scguns.common.headshot.RotatedHeadshotBox;
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.interfaces.IHeadshotBox;

public class BoundingBoxManager {
   private static final Map<EntityType<?>, IHeadshotBox<?>> headshotBoxes = new HashMap<>();
   private static final WeakHashMap<Player, LinkedList<AABB>> playerBoxes = new WeakHashMap<>();
   private static final Map<EntityType<?>, IHeadshotBox<?>> dynamicHeadshotBoxes = new HashMap<>();

   public BoundingBoxManager() {
      super();
   }

   public static <T extends LivingEntity> void registerHeadshotBox(EntityType<T> type, IHeadshotBox<T> headshotBox) {
      headshotBoxes.putIfAbsent(type, headshotBox);
   }

   @Nullable
   public static IHeadshotBox<LivingEntity> getHeadshotBoxes(EntityType<?> type) {
      IHeadshotBox<?> box = headshotBoxes.get(type);
      if (box != null) {
         return (IHeadshotBox<LivingEntity>)box;
      } else {
         box = dynamicHeadshotBoxes.get(type);
         if (box != null) {
            return (IHeadshotBox<LivingEntity>)box;
         } else if (type.getCategory() != MobCategory.MONSTER && type.getCategory() != MobCategory.CREATURE && type.getCategory() != MobCategory.AMBIENT) {
            return null;
         } else {
            DynamicHeadshotBox<LivingEntity> dynamicBox = new DynamicHeadshotBox<>();
            dynamicHeadshotBoxes.put(type, dynamicBox);
            return dynamicBox;
         }
      }
   }

   public static void clearDynamicBoxCache() {
      dynamicHeadshotBoxes.clear();
   }

   @SubscribeEvent(
      receiveCanceled = true
   )
   public void onPlayerTick(PlayerTickEvent.Post event) {
      if ((Boolean)Config.COMMON.gameplay.improvedHitboxes.get()) {
         if (!event.getEntity().level().isClientSide) {
            if (event.getEntity().isSpectator()) {
               playerBoxes.remove(event.getEntity());
               return;
            }

            LinkedList<AABB> boxes = playerBoxes.computeIfAbsent(event.getEntity(), player -> new LinkedList<>());
            boxes.addFirst(event.getEntity().getBoundingBox());
            if (boxes.size() > 20) {
               boxes.removeLast();
            }
         }
      }
   }

   @SubscribeEvent(
      receiveCanceled = true
   )
   public void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
      playerBoxes.remove(event.getEntity());
   }

   public static AABB getBoundingBox(Player entity, int ping) {
      if (playerBoxes.containsKey(entity)) {
         LinkedList<AABB> boxes = playerBoxes.get(entity);
         int index = Mth.clamp(ping, 0, boxes.size() - 1);
         return boxes.get(index);
      } else {
         return entity.getBoundingBox();
      }
   }

   static {
      registerHeadshotBox(
         EntityType.PLAYER,
         entity -> {
            AABB headBox = new AABB(-0.25, 0.0, -0.25, 0.25, 0.5, 0.25);
            double scale = 0.9375;
            if (entity.isSwimming()) {
               headBox = headBox.move(0.0, 0.1875, 0.0);
               Vec3 pos = Vec3.directionFromRotation(entity.getXRot(), entity.yBodyRot).normalize().scale(0.8);
               headBox = headBox.move(pos);
            } else {
               headBox = headBox.move(0.0, entity.isShiftKeyDown() ? 1.25 : 1.5, 0.0);
            }

            return new AABB(
               headBox.minX * scale,
               headBox.minY * scale,
               headBox.minZ * scale,
               headBox.maxX * scale,
               headBox.maxY * scale,
               headBox.maxZ * scale
            );
         }
      );
      registerHeadshotBox(EntityType.ZOMBIE, new ChildHeadshotBox(8.0, 24.0, 0.75, 0.5));
      registerHeadshotBox(EntityType.ZOMBIFIED_PIGLIN, new ChildHeadshotBox(8.0, 24.0, 0.75, 0.5));
      registerHeadshotBox(EntityType.HUSK, new ChildHeadshotBox(8.0, 24.0, 0.75, 0.5));
      registerHeadshotBox(EntityType.SKELETON, new BasicHeadshotBox(8.0, 24.0));
      registerHeadshotBox(EntityType.WITHER_SKELETON, new BasicHeadshotBox(8.0, 26.0));
      registerHeadshotBox(EntityType.STRAY, new BasicHeadshotBox(8.0, 24.0));
      registerHeadshotBox(EntityType.CREEPER, new BasicHeadshotBox(8.0, 18.0));
      registerHeadshotBox(EntityType.SPIDER, new RotatedHeadshotBox(8.0, 5.0, 7.0, false, true));
      registerHeadshotBox(EntityType.DROWNED, new BasicHeadshotBox(8.0, 24.0));
      registerHeadshotBox(EntityType.VILLAGER, new NoChildHeadshotBox(8.0, 9.0, 23.0));
      registerHeadshotBox(EntityType.ZOMBIE_VILLAGER, new NoChildHeadshotBox(8.0, 9.0, 23.0));
      registerHeadshotBox(EntityType.VINDICATOR, new NoChildHeadshotBox(8.0, 9.0, 23.0));
      registerHeadshotBox(EntityType.EVOKER, new BasicHeadshotBox(8.0, 9.0, 23.0));
      registerHeadshotBox(EntityType.PILLAGER, new BasicHeadshotBox(8.0, 9.0, 23.0));
      registerHeadshotBox(EntityType.ILLUSIONER, new BasicHeadshotBox(8.0, 9.0, 23.0));
      registerHeadshotBox(EntityType.WANDERING_TRADER, new BasicHeadshotBox(8.0, 9.0, 23.0));
      registerHeadshotBox(EntityType.WITCH, new BasicHeadshotBox(8.0, 9.0, 23.0));
      registerHeadshotBox(EntityType.SHEEP, new RotatedHeadshotBox(7.5, 8.0, 15.0, 9.5, false, true));
      registerHeadshotBox(EntityType.CHICKEN, new NoChildRotatedHeadshotBox(4.0, 6.0, 9.0, 5.0, false, true));
      registerHeadshotBox(EntityType.COW, new NoChildRotatedHeadshotBox(7.5, 8.0, 16.0, 10.5, false, true));
      registerHeadshotBox(EntityType.MOOSHROOM, new NoChildRotatedHeadshotBox(7.5, 8.0, 16.0, 10.5, false, true));
      registerHeadshotBox(EntityType.PIG, new NoChildRotatedHeadshotBox(8.0, 8.0, 10.0, false, true));
      registerHeadshotBox(EntityType.HORSE, new RotatedHeadshotBox(10.0, 26.0, 16.0, false, true));
      registerHeadshotBox(EntityType.SKELETON_HORSE, new RotatedHeadshotBox(10.0, 26.0, 16.0, false, true));
      registerHeadshotBox(EntityType.DONKEY, new RotatedHeadshotBox(7.5, 8.0, 20.0, 13.0, false, true));
      registerHeadshotBox(EntityType.MULE, new RotatedHeadshotBox(7.5, 8.0, 21.0, 14.0, false, true));
      registerHeadshotBox(EntityType.LLAMA, new RotatedHeadshotBox(8.0, 26.0, 10.0, false, true));
      registerHeadshotBox(EntityType.TRADER_LLAMA, new RotatedHeadshotBox(8.0, 26.0, 10.0, false, true));
      registerHeadshotBox(EntityType.POLAR_BEAR, new RotatedHeadshotBox(9.0, 12.0, 20.0, false, true));
      registerHeadshotBox(EntityType.SNOW_GOLEM, new BasicHeadshotBox(10.0, 20.5));
      registerHeadshotBox(EntityType.TURTLE, new RotatedHeadshotBox(6.0, 5.0, 1.0, 10.0, false, true));
      registerHeadshotBox(EntityType.IRON_GOLEM, new RotatedHeadshotBox(8.0, 10.0, 33.0, 3.5, false, true));
      registerHeadshotBox(EntityType.PHANTOM, new RotatedHeadshotBox(6.0, 3.0, 1.5, 6.5, true, true));
      registerHeadshotBox(EntityType.HOGLIN, new RotatedHeadshotBox(14.0, 16.0, 7.0, 19.0, false, true));
      registerHeadshotBox(EntityType.ZOGLIN, new RotatedHeadshotBox(14.0, 16.0, 7.0, 19.0, false, true));
      registerHeadshotBox(EntityType.PIGLIN, new ChildHeadshotBox(8.0, 24.0, 0.75, 0.5));
      registerHeadshotBox((EntityType)ModEntities.HORNLIN.get(), new BasicHeadshotBox(8.0, 24.0));
      registerHeadshotBox((EntityType)ModEntities.ZOMBIFIED_HORNLIN.get(), new BasicHeadshotBox(8.0, 24.0));
      registerHeadshotBox((EntityType)ModEntities.THE_MERCHANT.get(), new BasicHeadshotBox(8.0, 29.0));
      registerHeadshotBox((EntityType)ModEntities.COG_MINION.get(), new BasicHeadshotBox(8.0, 24.0));
      registerHeadshotBox((EntityType)ModEntities.COG_KNIGHT.get(), new BasicHeadshotBox(8.0, 24.0));
      registerHeadshotBox((EntityType)ModEntities.TRAUMA_UNIT.get(), new BasicHeadshotBox(8.0, 24.0));
      registerHeadshotBox((EntityType)ModEntities.BLUNDERER.get(), new BasicHeadshotBox(8.0, 24.0));
      registerHeadshotBox((EntityType)ModEntities.HIVE.get(), new BasicHeadshotBox(8.0, 18.0));
      registerHeadshotBox((EntityType)ModEntities.SULFURHEAD.get(), new BasicHeadshotBox(8.0, 18.0));
      registerHeadshotBox((EntityType)ModEntities.DISSIDENT.get(), new BasicHeadshotBox(8.0, 18.0));
      registerHeadshotBox((EntityType)ModEntities.SCAMP_TANK.get(), new BasicHeadshotBox(8.0, 30.0));
   }
}
