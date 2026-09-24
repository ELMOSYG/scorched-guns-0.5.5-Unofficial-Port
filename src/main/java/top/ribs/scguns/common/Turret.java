package top.ribs.scguns.common;



import net.minecraft.core.registries.BuiltInRegistries;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import top.ribs.scguns.annotation.Optional;

public class Turret  {
   protected Turret.Targeting targeting = new Turret.Targeting();
   protected Turret.Combat combat = new Turret.Combat();
   protected Turret.Ammunition ammunition = new Turret.Ammunition();
   protected Turret.Behavior behavior = new Turret.Behavior();
   protected Turret.Display display = new Turret.Display();

   public Turret() {
      super();
   }

   public Turret.Targeting getTargeting() {
      return this.targeting;
   }

   public Turret.Combat getCombat() {
      return this.combat;
   }

   public Turret.Ammunition getAmmunition() {
      return this.ammunition;
   }

   public Turret.Behavior getBehavior() {
      return this.behavior;
   }

   public Turret.Display getDisplay() {
      return this.display;
   }

   public CompoundTag serializeNBT() {
      CompoundTag tag = new CompoundTag();
      tag.put("Targeting", this.targeting.serializeNBT());
      tag.put("Combat", this.combat.serializeNBT());
      tag.put("Ammunition", this.ammunition.serializeNBT());
      tag.put("Behavior", this.behavior.serializeNBT());
      tag.put("Display", this.display.serializeNBT());
      return tag;
   }

   public void deserializeNBT(CompoundTag tag) {
      if (tag.contains("Targeting", 10)) {
         this.targeting.deserializeNBT(tag.getCompound("Targeting"));
      }

      if (tag.contains("Combat", 10)) {
         this.combat.deserializeNBT(tag.getCompound("Combat"));
      }

      if (tag.contains("Ammunition", 10)) {
         this.ammunition.deserializeNBT(tag.getCompound("Ammunition"));
      }

      if (tag.contains("Behavior", 10)) {
         this.behavior.deserializeNBT(tag.getCompound("Behavior"));
      }

      if (tag.contains("Display", 10)) {
         this.display.deserializeNBT(tag.getCompound("Display"));
      }
   }

   public JsonObject toJsonObject() {
      JsonObject object = new JsonObject();
      object.add("targeting", this.targeting.toJsonObject());
      object.add("combat", this.combat.toJsonObject());
      object.add("ammunition", this.ammunition.toJsonObject());
      object.add("behavior", this.behavior.toJsonObject());
      object.add("display", this.display.toJsonObject());
      return object;
   }

   public static Turret create(CompoundTag tag) {
      Turret turret = new Turret();
      turret.deserializeNBT(tag);
      return turret;
   }

   public Turret copy() {
      Turret turret = new Turret();
      turret.targeting = this.targeting.copy();
      turret.combat = this.combat.copy();
      turret.ammunition = this.ammunition.copy();
      turret.behavior = this.behavior.copy();
      turret.display = this.display.copy();
      return turret;
   }

   public static class Ammunition  {
      private List<Turret.Ammunition.AmmoType> acceptedAmmo = new ArrayList<>();
      @Optional
      private float casingEjectChance = 0.65F;

      public Ammunition() {
         super();
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         CompoundTag ammoTag = new CompoundTag();

         for (int i = 0; i < this.acceptedAmmo.size(); i++) {
            ammoTag.put("Ammo" + i, this.acceptedAmmo.get(i).serializeNBT());
         }

         tag.put("AcceptedAmmo", ammoTag);
         tag.putFloat("CasingEjectChance", this.casingEjectChance);
         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("AcceptedAmmo", 10)) {
            CompoundTag ammoTag = tag.getCompound("AcceptedAmmo");
            this.acceptedAmmo.clear();

            for (int i = 0; ammoTag.contains("Ammo" + i); i++) {
               Turret.Ammunition.AmmoType ammo = new Turret.Ammunition.AmmoType();
               ammo.deserializeNBT(ammoTag.getCompound("Ammo" + i));
               this.acceptedAmmo.add(ammo);
            }
         }

         if (tag.contains("CasingEjectChance", 99)) {
            this.casingEjectChance = tag.getFloat("CasingEjectChance");
         }
      }

      public JsonObject toJsonObject() {
         JsonObject object = new JsonObject();
         if (!this.acceptedAmmo.isEmpty()) {
            JsonArray ammoArray = new JsonArray();

            for (Turret.Ammunition.AmmoType ammo : this.acceptedAmmo) {
               ammoArray.add(ammo.toJsonObject());
            }

            object.add("acceptedAmmo", ammoArray);
         }

         if (this.casingEjectChance != 0.65F) {
            object.addProperty("casingEjectChance", this.casingEjectChance);
         }

         return object;
      }

      public Turret.Ammunition copy() {
         Turret.Ammunition ammunition = new Turret.Ammunition();

         for (Turret.Ammunition.AmmoType ammo : this.acceptedAmmo) {
            ammunition.acceptedAmmo.add(ammo.copy());
         }

         ammunition.casingEjectChance = this.casingEjectChance;
         return ammunition;
      }

      public List<Turret.Ammunition.AmmoType> getAcceptedAmmo() {
         return this.acceptedAmmo;
      }

      public float getCasingEjectChance() {
         return this.casingEjectChance;
      }

      public void clearAcceptedAmmo() {
         this.acceptedAmmo.clear();
      }

      public void addAmmoType(Turret.Ammunition.AmmoType ammo) {
         this.acceptedAmmo.add(ammo);
      }

      public void setCasingEjectChance(float casingEjectChance) {
         this.casingEjectChance = casingEjectChance;
      }

      public static class AmmoType  {
         private ResourceLocation item;
         private ResourceLocation bulletType;
         @Optional
         private ResourceLocation casingType;
         private double damage = 5.0;
         @Optional
         private float armorPenetration = 0.0F;

         public AmmoType() {
            super();
         }

         public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Item", this.item.toString());
            tag.putString("BulletType", this.bulletType.toString());
            if (this.casingType != null) {
               tag.putString("CasingType", this.casingType.toString());
            }

            tag.putDouble("Damage", this.damage);
            tag.putFloat("ArmorPenetration", this.armorPenetration);
            return tag;
         }

         public void deserializeNBT(CompoundTag tag) {
            if (tag.contains("Item", 8)) {
               this.item = ResourceLocation.parse(tag.getString("Item"));
            }

            if (tag.contains("BulletType", 8)) {
               this.bulletType = ResourceLocation.parse(tag.getString("BulletType"));
            }

            if (tag.contains("CasingType", 8)) {
               this.casingType = ResourceLocation.parse(tag.getString("CasingType"));
            }

            if (tag.contains("Damage", 99)) {
               this.damage = tag.getDouble("Damage");
            }

            if (tag.contains("ArmorPenetration", 99)) {
               this.armorPenetration = tag.getFloat("ArmorPenetration");
            }
         }

         public JsonObject toJsonObject() {
            JsonObject object = new JsonObject();
            object.addProperty("item", this.item.toString());
            object.addProperty("bulletType", this.bulletType.toString());
            if (this.casingType != null) {
               object.addProperty("casingType", this.casingType.toString());
            }

            object.addProperty("damage", this.damage);
            if (this.armorPenetration != 0.0F) {
               object.addProperty("armorPenetration", this.armorPenetration);
            }

            return object;
         }

         public Turret.Ammunition.AmmoType copy() {
            Turret.Ammunition.AmmoType ammo = new Turret.Ammunition.AmmoType();
            ammo.item = this.item;
            ammo.bulletType = this.bulletType;
            ammo.casingType = this.casingType;
            ammo.damage = this.damage;
            ammo.armorPenetration = this.armorPenetration;
            return ammo;
         }

         @Nullable
         public Item getItem() {
            return (Item)BuiltInRegistries.ITEM.get(this.item);
         }

         public ResourceLocation getBulletType() {
            return this.bulletType;
         }

         @Nullable
         public ResourceLocation getCasingType() {
            return this.casingType;
         }

         public double getDamage() {
            return this.damage;
         }

         public float getArmorPenetration() {
            return this.armorPenetration;
         }

         public void setItem(ResourceLocation item) {
            this.item = item;
         }

         public void setBulletType(ResourceLocation bulletType) {
            this.bulletType = bulletType;
         }

         public void setCasingType(ResourceLocation casingType) {
            this.casingType = casingType;
         }

         public void setDamage(double damage) {
            this.damage = damage;
         }

         public void setArmorPenetration(float armorPenetration) {
            this.armorPenetration = armorPenetration;
         }
      }
   }

   public static class Behavior  {
      private float restingYaw = 0.0F;
      private float restingPitch = -30.0F;
      private int disableTime = 200;
      @Optional
      private boolean hasOpenAnimation = false;

      public Behavior() {
         super();
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         tag.putFloat("RestingYaw", this.restingYaw);
         tag.putFloat("RestingPitch", this.restingPitch);
         tag.putInt("DisableTime", this.disableTime);
         tag.putBoolean("HasOpenAnimation", this.hasOpenAnimation);
         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("RestingYaw", 99)) {
            this.restingYaw = tag.getFloat("RestingYaw");
         }

         if (tag.contains("RestingPitch", 99)) {
            this.restingPitch = tag.getFloat("RestingPitch");
         }

         if (tag.contains("DisableTime", 99)) {
            this.disableTime = tag.getInt("DisableTime");
         }

         if (tag.contains("HasOpenAnimation", 99)) {
            this.hasOpenAnimation = tag.getBoolean("HasOpenAnimation");
         }
      }

      public JsonObject toJsonObject() {
         JsonObject object = new JsonObject();
         if (this.restingYaw != 0.0F) {
            object.addProperty("restingYaw", this.restingYaw);
         }

         if (this.restingPitch != -30.0F) {
            object.addProperty("restingPitch", this.restingPitch);
         }

         if (this.disableTime != 200) {
            object.addProperty("disableTime", this.disableTime);
         }

         if (this.hasOpenAnimation) {
            object.addProperty("hasOpenAnimation", true);
         }

         return object;
      }

      public Turret.Behavior copy() {
         Turret.Behavior behavior = new Turret.Behavior();
         behavior.restingYaw = this.restingYaw;
         behavior.restingPitch = this.restingPitch;
         behavior.disableTime = this.disableTime;
         behavior.hasOpenAnimation = this.hasOpenAnimation;
         return behavior;
      }

      public float getRestingYaw() {
         return this.restingYaw;
      }

      public float getRestingPitch() {
         return this.restingPitch;
      }

      public int getDisableTime() {
         return this.disableTime;
      }

      public boolean hasOpenAnimation() {
         return this.hasOpenAnimation;
      }

      public void setRestingYaw(float restingYaw) {
         this.restingYaw = restingYaw;
      }

      public void setRestingPitch(float restingPitch) {
         this.restingPitch = restingPitch;
      }

      public void setDisableTime(int disableTime) {
         this.disableTime = disableTime;
      }

      public void setHasOpenAnimation(boolean hasOpenAnimation) {
         this.hasOpenAnimation = hasOpenAnimation;
      }
   }

   public static class Combat  {
      private int cooldown = 16;
      private float inaccuracy = 0.05F;
      @Optional
      private int pelletCount = 1;
      @Optional
      private float spreadAngle = 0.0F;
      private float recoilMax = 4.0F;
      private float recoilSpeed = 0.3F;
      @Optional
      private int damageModifier = 0;
      private double projectileSpeed = 3.0;
      @Optional
      private ResourceLocation fireSound;

      public Combat() {
         super();
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         tag.putInt("Cooldown", this.cooldown);
         tag.putFloat("Inaccuracy", this.inaccuracy);
         tag.putInt("PelletCount", this.pelletCount);
         tag.putFloat("SpreadAngle", this.spreadAngle);
         tag.putFloat("RecoilMax", this.recoilMax);
         tag.putFloat("RecoilSpeed", this.recoilSpeed);
         tag.putInt("DamageModifier", this.damageModifier);
         tag.putDouble("ProjectileSpeed", this.projectileSpeed);
         if (this.fireSound != null) {
            tag.putString("FireSound", this.fireSound.toString());
         }

         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("Cooldown", 99)) {
            this.cooldown = tag.getInt("Cooldown");
         }

         if (tag.contains("Inaccuracy", 99)) {
            this.inaccuracy = tag.getFloat("Inaccuracy");
         }

         if (tag.contains("PelletCount", 99)) {
            this.pelletCount = tag.getInt("PelletCount");
         }

         if (tag.contains("SpreadAngle", 99)) {
            this.spreadAngle = tag.getFloat("SpreadAngle");
         }

         if (tag.contains("RecoilMax", 99)) {
            this.recoilMax = tag.getFloat("RecoilMax");
         }

         if (tag.contains("RecoilSpeed", 99)) {
            this.recoilSpeed = tag.getFloat("RecoilSpeed");
         }

         if (tag.contains("DamageModifier", 99)) {
            this.damageModifier = tag.getInt("DamageModifier");
         }

         if (tag.contains("ProjectileSpeed", 99)) {
            this.projectileSpeed = tag.getDouble("ProjectileSpeed");
         }

         if (tag.contains("FireSound", 8)) {
            this.fireSound = ResourceLocation.parse(tag.getString("FireSound"));
         }
      }

      public JsonObject toJsonObject() {
         JsonObject object = new JsonObject();
         if (this.cooldown != 16) {
            object.addProperty("cooldown", this.cooldown);
         }

         if (this.inaccuracy != 0.05F) {
            object.addProperty("inaccuracy", this.inaccuracy);
         }

         if (this.pelletCount != 1) {
            object.addProperty("pelletCount", this.pelletCount);
         }

         if (this.spreadAngle != 0.0F) {
            object.addProperty("spreadAngle", this.spreadAngle);
         }

         if (this.recoilMax != 4.0F) {
            object.addProperty("recoilMax", this.recoilMax);
         }

         if (this.recoilSpeed != 0.3F) {
            object.addProperty("recoilSpeed", this.recoilSpeed);
         }

         if (this.damageModifier != 0) {
            object.addProperty("damageModifier", this.damageModifier);
         }

         if (this.projectileSpeed != 3.0) {
            object.addProperty("projectileSpeed", this.projectileSpeed);
         }

         if (this.fireSound != null) {
            object.addProperty("fireSound", this.fireSound.toString());
         }

         return object;
      }

      public Turret.Combat copy() {
         Turret.Combat combat = new Turret.Combat();
         combat.cooldown = this.cooldown;
         combat.inaccuracy = this.inaccuracy;
         combat.pelletCount = this.pelletCount;
         combat.spreadAngle = this.spreadAngle;
         combat.recoilMax = this.recoilMax;
         combat.recoilSpeed = this.recoilSpeed;
         combat.damageModifier = this.damageModifier;
         combat.projectileSpeed = this.projectileSpeed;
         combat.fireSound = this.fireSound;
         return combat;
      }

      public int getCooldown() {
         return this.cooldown;
      }

      public float getInaccuracy() {
         return this.inaccuracy;
      }

      public int getPelletCount() {
         return this.pelletCount;
      }

      public float getSpreadAngle() {
         return this.spreadAngle;
      }

      public float getRecoilMax() {
         return this.recoilMax;
      }

      public float getRecoilSpeed() {
         return this.recoilSpeed;
      }

      public int getDamageModifier() {
         return this.damageModifier;
      }

      public double getProjectileSpeed() {
         return this.projectileSpeed;
      }

      @Nullable
      public ResourceLocation getFireSound() {
         return this.fireSound;
      }

      public void setCooldown(int cooldown) {
         this.cooldown = cooldown;
      }

      public void setInaccuracy(float inaccuracy) {
         this.inaccuracy = inaccuracy;
      }

      public void setPelletCount(int pelletCount) {
         this.pelletCount = pelletCount;
      }

      public void setSpreadAngle(float spreadAngle) {
         this.spreadAngle = spreadAngle;
      }

      public void setRecoilMax(float recoilMax) {
         this.recoilMax = recoilMax;
      }

      public void setRecoilSpeed(float recoilSpeed) {
         this.recoilSpeed = recoilSpeed;
      }

      public void setDamageModifier(int damageModifier) {
         this.damageModifier = damageModifier;
      }

      public void setProjectileSpeed(double projectileSpeed) {
         this.projectileSpeed = projectileSpeed;
      }

      public void setFireSound(ResourceLocation fireSound) {
         this.fireSound = fireSound;
      }
   }

   public static class Display  {
      @Optional
      private double muzzleLength = 1.0;
      @Optional
      private double muzzleOffsetY = 1.4;

      public Display() {
         super();
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         tag.putDouble("MuzzleLength", this.muzzleLength);
         tag.putDouble("MuzzleOffsetY", this.muzzleOffsetY);
         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("MuzzleLength", 99)) {
            this.muzzleLength = tag.getDouble("MuzzleLength");
         }

         if (tag.contains("MuzzleOffsetY", 99)) {
            this.muzzleOffsetY = tag.getDouble("MuzzleOffsetY");
         }
      }

      public JsonObject toJsonObject() {
         JsonObject object = new JsonObject();
         if (this.muzzleLength != 1.0) {
            object.addProperty("muzzleLength", this.muzzleLength);
         }

         if (this.muzzleOffsetY != 1.4) {
            object.addProperty("muzzleOffsetY", this.muzzleOffsetY);
         }

         return object;
      }

      public Turret.Display copy() {
         Turret.Display display = new Turret.Display();
         display.muzzleLength = this.muzzleLength;
         display.muzzleOffsetY = this.muzzleOffsetY;
         return display;
      }

      public double getMuzzleLength() {
         return this.muzzleLength;
      }

      public double getMuzzleOffsetY() {
         return this.muzzleOffsetY;
      }

      public void setMuzzleLength(double muzzleLength) {
         this.muzzleLength = muzzleLength;
      }

      public void setMuzzleOffsetY(double muzzleOffsetY) {
         this.muzzleOffsetY = muzzleOffsetY;
      }
   }

   public static class Targeting  {
      private double range = 12.0;
      private double verticalRange = 12.0;
      private double minFiringDistance = 1.3;
      private float rotationSpeed = 0.5F;
      private float positionSmoothing = 0.2F;
      private float maxPitch = 60.0F;
      private float minPitch = -25.0F;
      private int predictionMultiplier = 7;
      private boolean requiresLineOfSight = true;

      public Targeting() {
         super();
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         tag.putDouble("Range", this.range);
         tag.putDouble("VerticalRange", this.verticalRange);
         tag.putDouble("MinFiringDistance", this.minFiringDistance);
         tag.putFloat("RotationSpeed", this.rotationSpeed);
         tag.putFloat("PositionSmoothing", this.positionSmoothing);
         tag.putFloat("MaxPitch", this.maxPitch);
         tag.putFloat("MinPitch", this.minPitch);
         tag.putInt("PredictionMultiplier", this.predictionMultiplier);
         tag.putBoolean("RequiresLineOfSight", this.requiresLineOfSight);
         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("Range", 99)) {
            this.range = tag.getDouble("Range");
         }

         if (tag.contains("VerticalRange", 99)) {
            this.verticalRange = tag.getDouble("VerticalRange");
         }

         if (tag.contains("MinFiringDistance", 99)) {
            this.minFiringDistance = tag.getDouble("MinFiringDistance");
         }

         if (tag.contains("RotationSpeed", 99)) {
            this.rotationSpeed = tag.getFloat("RotationSpeed");
         }

         if (tag.contains("PositionSmoothing", 99)) {
            this.positionSmoothing = tag.getFloat("PositionSmoothing");
         }

         if (tag.contains("MaxPitch", 99)) {
            this.maxPitch = tag.getFloat("MaxPitch");
         }

         if (tag.contains("MinPitch", 99)) {
            this.minPitch = tag.getFloat("MinPitch");
         }

         if (tag.contains("PredictionMultiplier", 99)) {
            this.predictionMultiplier = tag.getInt("PredictionMultiplier");
         }

         if (tag.contains("RequiresLineOfSight", 99)) {
            this.requiresLineOfSight = tag.getBoolean("RequiresLineOfSight");
         }
      }

      public JsonObject toJsonObject() {
         JsonObject object = new JsonObject();
         if (this.range != 12.0) {
            object.addProperty("range", this.range);
         }

         if (this.verticalRange != 12.0) {
            object.addProperty("verticalRange", this.verticalRange);
         }

         if (this.minFiringDistance != 1.3) {
            object.addProperty("minFiringDistance", this.minFiringDistance);
         }

         if (this.rotationSpeed != 0.5F) {
            object.addProperty("rotationSpeed", this.rotationSpeed);
         }

         if (this.positionSmoothing != 0.2F) {
            object.addProperty("positionSmoothing", this.positionSmoothing);
         }

         if (this.maxPitch != 60.0F) {
            object.addProperty("maxPitch", this.maxPitch);
         }

         if (this.minPitch != -25.0F) {
            object.addProperty("minPitch", this.minPitch);
         }

         if (this.predictionMultiplier != 7) {
            object.addProperty("predictionMultiplier", this.predictionMultiplier);
         }

         if (!this.requiresLineOfSight) {
            object.addProperty("requiresLineOfSight", false);
         }

         return object;
      }

      public Turret.Targeting copy() {
         Turret.Targeting targeting = new Turret.Targeting();
         targeting.range = this.range;
         targeting.verticalRange = this.verticalRange;
         targeting.minFiringDistance = this.minFiringDistance;
         targeting.rotationSpeed = this.rotationSpeed;
         targeting.positionSmoothing = this.positionSmoothing;
         targeting.maxPitch = this.maxPitch;
         targeting.minPitch = this.minPitch;
         targeting.predictionMultiplier = this.predictionMultiplier;
         targeting.requiresLineOfSight = this.requiresLineOfSight;
         return targeting;
      }

      public double getRange() {
         return this.range;
      }

      public double getVerticalRange() {
         return this.verticalRange;
      }

      public double getMinFiringDistance() {
         return this.minFiringDistance;
      }

      public float getRotationSpeed() {
         return this.rotationSpeed;
      }

      public float getPositionSmoothing() {
         return this.positionSmoothing;
      }

      public float getMaxPitch() {
         return this.maxPitch;
      }

      public float getMinPitch() {
         return this.minPitch;
      }

      public int getPredictionMultiplier() {
         return this.predictionMultiplier;
      }

      public boolean requiresLineOfSight() {
         return this.requiresLineOfSight;
      }

      public void setRange(double range) {
         this.range = range;
      }

      public void setVerticalRange(double verticalRange) {
         this.verticalRange = verticalRange;
      }

      public void setMinFiringDistance(double minFiringDistance) {
         this.minFiringDistance = minFiringDistance;
      }

      public void setRotationSpeed(float rotationSpeed) {
         this.rotationSpeed = rotationSpeed;
      }

      public void setPositionSmoothing(float positionSmoothing) {
         this.positionSmoothing = positionSmoothing;
      }

      public void setMaxPitch(float maxPitch) {
         this.maxPitch = maxPitch;
      }

      public void setMinPitch(float minPitch) {
         this.minPitch = minPitch;
      }

      public void setPredictionMultiplier(int predictionMultiplier) {
         this.predictionMultiplier = predictionMultiplier;
      }

      public void setRequiresLineOfSight(boolean requiresLineOfSight) {
         this.requiresLineOfSight = requiresLineOfSight;
      }
   }
}
