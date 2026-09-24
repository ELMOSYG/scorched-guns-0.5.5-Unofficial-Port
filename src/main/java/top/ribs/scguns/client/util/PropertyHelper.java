package top.ribs.scguns.client.util;

import com.mrcrayfish.framework.api.client.FrameworkClientAPI;
import com.mrcrayfish.framework.api.serialize.DataArray;
import com.mrcrayfish.framework.api.serialize.DataNumber;
import com.mrcrayfish.framework.api.serialize.DataObject;
import com.mrcrayfish.framework.api.serialize.DataType;
import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.cache.ObjectCache;
import top.ribs.scguns.client.MetaLoader;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.common.properties.SightAnimation;
import top.ribs.scguns.item.IMeta;
import top.ribs.scguns.item.attachment.IAttachment;
import top.ribs.scguns.item.attachment.IBarrel;
import top.ribs.scguns.item.attachment.IScope;

public final class PropertyHelper {
   public static final String CACHE_KEY = "properties";
   public static final String MODEL_KEY = "scguns:model";
   public static final String WEAPON_KEY = "scguns:weapon";
   public static final String SCOPE_KEY = "scguns:scope";
   public static final String BARREL_KEY = "scguns:barrel";
   public static final Vec3 GUN_DEFAULT_ORIGIN = new Vec3(8.0, 0.0, 8.0);
   public static final Vec3 ATTACHMENT_DEFAULT_ORIGIN = new Vec3(8.0, 8.0, 8.0);
   public static final Vec3 DEFAULT_SCALE = new Vec3(1.0, 1.0, 1.0);
   public static final Vec3 RED = new Vec3(255.0, 0.0, 0.0);

   public PropertyHelper() {
      super();
   }

   public static void resetCache() {
      ObjectCache.getInstance("properties").reset();
   }

   private static DataObject getCustomData(ItemStack stack) {
      return stack.getItem() instanceof IMeta ? MetaLoader.getInstance().getData(stack.getItem()) : FrameworkClientAPI.getOpenModelData(stack, null, null, 0);
   }

   public static Vec3 getScopeCamera(ItemStack stack) {
      DataObject customObject = getCustomData(stack);
      if (customObject.has("scguns:scope", DataType.OBJECT)) {
         DataObject scopeObject = customObject.getDataObject("scguns:scope");
         if (scopeObject.has("camera", DataType.ARRAY)) {
            DataArray cameraArray = scopeObject.getDataArray("camera");
            return arrayToVec3(cameraArray, Vec3.ZERO);
         }
      }

      return ATTACHMENT_DEFAULT_ORIGIN;
   }

   public static Vec3 getIronSightCamera(ItemStack stack, Gun modifiedGun, Vec3 gunOrigin) {
      DataObject ironSightObject = getObjectByPath(stack, "scguns:weapon", "ironSight");
      if (ironSightObject.has("camera", DataType.ARRAY)) {
         DataArray cameraArray = ironSightObject.getDataArray("camera");
         return arrayToVec3(cameraArray, Vec3.ZERO);
      } else {
         Gun.Modules.Zoom zoom = modifiedGun.getModules().getZoom();
         if (zoom != null) {
            double cameraX = 8.0 - modifiedGun.getModules().getZoom().getXOffset();
            double cameraY = modifiedGun.getModules().getZoom().getYOffset();
            double cameraZ = 8.0 - modifiedGun.getModules().getZoom().getZOffset();
            return new Vec3(cameraX, cameraY, cameraZ);
         } else {
            return Vec3.ZERO;
         }
      }
   }

   public static boolean isLegacyIronSight(ItemStack stack) {
      DataObject ironSightObject = getObjectByPath(stack, "scguns:weapon", "ironSight");
      return !ironSightObject.has("camera", DataType.ARRAY);
   }

   public static Vec3 getModelOrigin(ItemStack stack, Vec3 defaultOrigin) {
      DataObject customObject = getCustomData(stack);
      if (customObject.has("scguns:model", DataType.OBJECT)) {
         DataObject modelObject = customObject.getDataObject("scguns:model");
         if (modelObject.has("origin", DataType.ARRAY)) {
            DataArray originArray = modelObject.getDataArray("origin");
            return arrayToVec3(originArray, defaultOrigin);
         }
      }

      return defaultOrigin;
   }

   public static Vec3 getAttachmentPosition(ItemStack stack, Gun modifiedGun, IAttachment.Type type) {
      DataObject scopeObject = getObjectByPath(stack, "scguns:weapon", "attachments", type.getSerializeKey());
      if (scopeObject.has("translation", DataType.ARRAY)) {
         DataArray translationArray = scopeObject.getDataArray("translation");
         return arrayToVec3(translationArray, Vec3.ZERO);
      } else {
         Gun.ScaledPositioned positioned = modifiedGun.getAttachmentPosition(type);
         if (positioned != null) {
            double displayX = positioned.getXOffset();
            double displayY = positioned.getYOffset();
            double displayZ = positioned.getZOffset();
            return new Vec3(displayX, displayY, displayZ).add(GUN_DEFAULT_ORIGIN);
         } else {
            return Vec3.ZERO;
         }
      }
   }

   public static Vec3 getAttachmentScale(ItemStack weapon, Gun modifiedGun, IAttachment.Type type) {
      DataObject scopeObject = getObjectByPath(weapon, "scguns:weapon", "attachments", type.getSerializeKey());
      if (scopeObject.has("scale", DataType.ARRAY)) {
         DataArray scaleArray = scopeObject.getDataArray("scale");
         return arrayToVec3(scaleArray, DEFAULT_SCALE);
      } else {
         Gun.ScaledPositioned positioned = modifiedGun.getAttachmentPosition(type);
         return positioned != null ? new Vec3(positioned.getScale(), positioned.getScale(), positioned.getScale()) : DEFAULT_SCALE;
      }
   }

   public static boolean hasMuzzleFlash(ItemStack weapon, Gun modifiedGun) {
      DataObject weaponObject = getObjectByPath(weapon, "scguns:weapon");
      return weaponObject.has("muzzleFlash", DataType.OBJECT) || modifiedGun.getDisplay().getFlash() != null;
   }

   public static Vec3 getMuzzleFlashPosition(ItemStack weapon, Gun modifiedGun) {
      if (Gun.hasAttachmentEquipped(weapon, modifiedGun, IAttachment.Type.BARREL)) {
         ItemStack barrelStack = Gun.getAttachment(IAttachment.Type.BARREL, weapon);
         if (barrelStack.getItem() instanceof IBarrel) {
            DataObject barrelObject = getObjectByPath(barrelStack, "scguns:barrel");
            if (barrelObject.has("muzzleFlash", DataType.OBJECT)) {
               DataObject muzzleObject = barrelObject.getDataObject("muzzleFlash");
               DataArray translationArray = muzzleObject.getDataArray("translation");
               Vec3 muzzlePosition = arrayToVec3(translationArray, Vec3.ZERO);
               Vec3 barrelOrigin = getModelOrigin(barrelStack, ATTACHMENT_DEFAULT_ORIGIN);
               Vec3 barrelPosition = getAttachmentPosition(weapon, modifiedGun, IAttachment.Type.BARREL);
               Vec3 barrelScale = getAttachmentScale(weapon, modifiedGun, IAttachment.Type.BARREL);
               return muzzlePosition.subtract(barrelOrigin).multiply(barrelScale).add(barrelPosition);
            }
         }
      }

      DataObject weaponObject = getObjectByPath(weapon, "scguns:weapon");
      if (weaponObject.has("muzzleFlash", DataType.OBJECT)) {
         DataObject muzzleObject = weaponObject.getDataObject("muzzleFlash");
         DataArray translationArray = muzzleObject.getDataArray("translation");
         return arrayToVec3(translationArray, Vec3.ZERO);
      } else {
         Gun.Positioned muzzleFlash = modifiedGun.getDisplay().getFlash();
         if (muzzleFlash != null) {
            double displayX = muzzleFlash.getXOffset();
            double displayY = muzzleFlash.getYOffset();
            double displayZ = muzzleFlash.getZOffset();
            return new Vec3(displayX, displayY, displayZ).add(GUN_DEFAULT_ORIGIN);
         } else {
            return Vec3.ZERO;
         }
      }
   }

   public static Vec3 getMuzzleFlashScale(ItemStack weapon, Gun modifiedGun) {
      DataObject weaponObject = getObjectByPath(weapon, "scguns:weapon");
      if (weaponObject.has("muzzleFlash", DataType.OBJECT)) {
         DataObject muzzleObject = weaponObject.getDataObject("muzzleFlash");
         if (muzzleObject.has("scale", DataType.ARRAY)) {
            DataArray scaleArray = muzzleObject.getDataArray("scale");
            return arrayToVec3(scaleArray, DEFAULT_SCALE);
         } else {
            return DEFAULT_SCALE;
         }
      } else {
         Gun.Display.Flash muzzleFlash = modifiedGun.getDisplay().getFlash();
         if (muzzleFlash != null) {
            double scale = muzzleFlash.getSize();
            return new Vec3(scale, scale, 1.0);
         } else {
            return DEFAULT_SCALE;
         }
      }
   }

   public static boolean isUsingBarrelMuzzleFlash(ItemStack barrel) {
      DataObject customObject = getObjectByPath(barrel, "scguns:barrel");
      return customObject.has("muzzleFlash", DataType.OBJECT);
   }

   public static SightAnimation getSightAnimations(ItemStack weapon, Gun modifiedGun) {
      if (Gun.hasAttachmentEquipped(weapon, modifiedGun, IAttachment.Type.SCOPE)) {
         ItemStack scopeStack = Gun.getScopeStack(weapon);
         if (scopeStack.getItem() instanceof IScope scope) {
            DataObject scopeObject = getObjectByPath(scopeStack, "scguns:scope");
            if (scopeObject.get("sightAnimation") instanceof DataObject sightObject) {
               return objectToSightAnimation(sightObject);
            }
         }
      }

      DataObject customObject = getObjectByPath(weapon, "scguns:weapon", "ironSight");
      return customObject.get("sightAnimation") instanceof DataObject sightObject ? objectToSightAnimation(sightObject) : SightAnimation.DEFAULT;
   }

   public static double getViewportFov(ItemStack weapon, Gun modifiedGun) {
      if (Gun.hasAttachmentEquipped(weapon, modifiedGun, IAttachment.Type.SCOPE)) {
         ItemStack scopeStack = Gun.getScopeStack(weapon);
         DataObject customObject = getObjectByPath(scopeStack, "scguns:scope");
         if (customObject.has("viewportFov", DataType.NUMBER)) {
            return Mth.clamp(customObject.getDataNumber("viewportFov").asDouble(), 1.0, 100.0);
         }
      }

      DataObject customObject = getObjectByPath(weapon, "scguns:weapon", "ironSight");
      return customObject.has("viewportFov", DataType.NUMBER) ? Mth.clamp(customObject.getDataNumber("viewportFov").asDouble(), 1.0, 100.0) : 0.0;
   }

   private static SightAnimation objectToSightAnimation(DataObject object) {
      ObjectCache cache = ObjectCache.getInstance("properties");
      Optional<SightAnimation> cachedValue = cache.get(object.getId());
      return cachedValue.orElseGet(() -> cache.store(object.getId(), () -> {
            SightAnimation.Builder builder = SightAnimation.builder();
            getOptionalString(object, "viewportCurve").ifPresent(s -> builder.setViewportCurve(Easings.byName(s)));
            getOptionalString(object, "sightCurve").ifPresent(s -> builder.setSightCurve(Easings.byName(s)));
            getOptionalString(object, "fovCurve").ifPresent(s -> builder.setFovCurve(Easings.byName(s)));
            getOptionalString(object, "aimTransformCurve").ifPresent(s -> builder.setAimTransformCurve(Easings.byName(s)));
            return builder.build();
         }));
   }

   private static Optional<String> getOptionalString(DataObject src, String key) {
      return src.has(key, DataType.STRING) ? Optional.ofNullable(src.getDataString(key).asString()) : Optional.empty();
   }

   public static DataObject getObjectByPath(ItemStack stack, String... path) {
      DataObject result = getCustomData(stack);

      for (String key : path) {
         if (!result.has(key, DataType.OBJECT)) {
            return DataObject.EMPTY;
         }

         result = result.getDataObject(key);
      }

      return result;
   }

   public static Vec3 arrayToVec3(DataArray array, Vec3 defaultValue) {
      if (array.length() != 3) {
         return defaultValue;
      } else {
         ObjectCache cache = ObjectCache.getInstance("properties");
         Optional<Vec3> cachedValue = cache.get(array.getId());
         return cachedValue.orElseGet(() -> cache.store(array.getId(), () -> {
               if (array.values().stream().allMatch(entry -> entry.getType() == DataType.NUMBER)) {
                  double x = ((DataNumber)array.get(0)).asDouble();
                  double y = ((DataNumber)array.get(1)).asDouble();
                  double z = ((DataNumber)array.get(2)).asDouble();
                  return new Vec3(x, y, z);
               } else {
                  return defaultValue;
               }
            }));
      }
   }
}
