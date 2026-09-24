package top.ribs.scguns.common.exosuit;


import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import top.ribs.scguns.annotation.Optional;

public class ExoSuitUpgrade  {
   protected String type = "plating";
   protected ExoSuitUpgrade.Effects effects = new ExoSuitUpgrade.Effects();
   protected ExoSuitUpgrade.Display display = new ExoSuitUpgrade.Display();

   public ExoSuitUpgrade() {
      super();
   }

   public String getType() {
      return this.type;
   }

   public ExoSuitUpgrade.Effects getEffects() {
      return this.effects;
   }

   public ExoSuitUpgrade.Display getDisplay() {
      return this.display;
   }

   public CompoundTag serializeNBT() {
      CompoundTag tag = new CompoundTag();
      tag.putString("Type", this.type);
      tag.put("Effects", this.effects.serializeNBT());
      tag.put("Display", this.display.serializeNBT());
      return tag;
   }

   public void deserializeNBT(CompoundTag tag) {
      if (tag.contains("Type", 8)) {
         this.type = tag.getString("Type");
      }

      if (tag.contains("Effects")) {
         this.effects.deserializeNBT(tag.getCompound("Effects"));
      }

      if (tag.contains("Display")) {
         this.display.deserializeNBT(tag.getCompound("Display"));
      }
   }

   public JsonObject toJsonObject() {
      JsonObject object = new JsonObject();
      object.addProperty("type", this.type);
      object.add("effects", this.effects.toJsonObject());
      JsonObject displayObj = this.display.toJsonObject();
      if (displayObj.size() > 0) {
         object.add("display", displayObj);
      }

      return object;
   }

   public ExoSuitUpgrade copy() {
      ExoSuitUpgrade upgrade = new ExoSuitUpgrade();
      upgrade.type = this.type;
      upgrade.effects = this.effects.copy();
      upgrade.display = this.display.copy();
      return upgrade;
   }

   public static class Builder {
      private final ExoSuitUpgrade upgrade = new ExoSuitUpgrade();

      private Builder() {
         super();
      }

      public static ExoSuitUpgrade.Builder create() {
         return new ExoSuitUpgrade.Builder();
      }

      public ExoSuitUpgrade.Builder setType(String type) {
         this.upgrade.type = type;
         return this;
      }

      public ExoSuitUpgrade.Builder setArmorBonus(float armor) {
         this.upgrade.effects.armorBonus = armor;
         return this;
      }

      public ExoSuitUpgrade.Builder setArmorToughness(float toughness) {
         this.upgrade.effects.armorToughness = toughness;
         return this;
      }

      public ExoSuitUpgrade.Builder setKnockbackResistance(float resistance) {
         this.upgrade.effects.knockbackResistance = resistance;
         return this;
      }

      public ExoSuitUpgrade.Builder setSpeedModifier(float speed) {
         this.upgrade.effects.speedModifier = speed;
         return this;
      }

      public ExoSuitUpgrade.Builder setJumpBoost(float jump) {
         this.upgrade.effects.jumpBoost = jump;
         return this;
      }

      public ExoSuitUpgrade.Builder setNightVision(boolean nightVision) {
         this.upgrade.effects.nightVision = nightVision;
         return this;
      }

      public ExoSuitUpgrade.Builder setRecoilAngleReduction(float reduction) {
         this.upgrade.effects.recoilAngleReduction = reduction;
         return this;
      }

      public ExoSuitUpgrade.Builder setRecoilKickReduction(float reduction) {
         this.upgrade.effects.recoilKickReduction = reduction;
         return this;
      }

      public ExoSuitUpgrade.Builder setSpreadReduction(float reduction) {
         this.upgrade.effects.spreadReduction = reduction;
         return this;
      }

      public ExoSuitUpgrade.Builder setModel(String model) {
         this.upgrade.display.model = model;
         return this;
      }

      public ExoSuitUpgrade.Builder setStorageSize(int size) {
         this.upgrade.display.storageSize = size;
         return this;
      }

      public ExoSuitUpgrade.Builder setGridDimensions(int width, int height) {
         this.upgrade.display.gridWidth = width;
         this.upgrade.display.gridHeight = height;
         return this;
      }

      public ExoSuitUpgrade.Builder setContainerType(String containerType) {
         this.upgrade.display.containerType = containerType;
         return this;
      }

      public ExoSuitUpgrade build() {
         return this.upgrade.copy();
      }
   }

   public static class Display  {
      @Optional
      String model = "";
      @Optional
      int storageSize = 0;
      @Optional
      int gridWidth = 0;
      @Optional
      int gridHeight = 0;
      @Optional
      String containerType = "";

      public Display() {
         super();
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         tag.putString("Model", this.model);
         tag.putInt("StorageSize", this.storageSize);
         tag.putInt("GridWidth", this.gridWidth);
         tag.putInt("GridHeight", this.gridHeight);
         tag.putString("ContainerType", this.containerType);
         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("Model", 8)) {
            this.model = tag.getString("Model");
         }

         if (tag.contains("StorageSize", 99)) {
            this.storageSize = tag.getInt("StorageSize");
         }

         if (tag.contains("GridWidth", 99)) {
            this.gridWidth = tag.getInt("GridWidth");
         }

         if (tag.contains("GridHeight", 99)) {
            this.gridHeight = tag.getInt("GridHeight");
         }

         if (tag.contains("ContainerType", 8)) {
            this.containerType = tag.getString("ContainerType");
         }
      }

      public JsonObject toJsonObject() {
         JsonObject object = new JsonObject();
         if (!this.model.isEmpty()) {
            object.addProperty("model", this.model);
         }

         if (this.storageSize > 0) {
            object.addProperty("storageSize", this.storageSize);
         }

         if (this.gridWidth > 0) {
            object.addProperty("gridWidth", this.gridWidth);
         }

         if (this.gridHeight > 0) {
            object.addProperty("gridHeight", this.gridHeight);
         }

         if (!this.containerType.isEmpty()) {
            object.addProperty("containerType", this.containerType);
         }

         return object;
      }

      public ExoSuitUpgrade.Display copy() {
         ExoSuitUpgrade.Display display = new ExoSuitUpgrade.Display();
         display.model = this.model;
         display.storageSize = this.storageSize;
         display.gridWidth = this.gridWidth;
         display.gridHeight = this.gridHeight;
         display.containerType = this.containerType;
         return display;
      }

      public String getModel() {
         return this.model;
      }

      public int getStorageSize() {
         return this.storageSize;
      }

      public int getGridWidth() {
         return this.gridWidth;
      }

      public int getGridHeight() {
         return this.gridHeight;
      }

      public String getContainerType() {
         return this.containerType;
      }
   }

   public static class Effects  {
      @Optional
      float armorBonus = 0.0F;
      @Optional
      float armorToughness = 0.0F;
      @Optional
      float knockbackResistance = 0.0F;
      @Optional
      float speedModifier = 0.0F;
      @Optional
      float jumpBoost = 0.0F;
      @Optional
      float fallDamageReduction = 0.0F;
      @Optional
      boolean nightVision = false;
      @Optional
      boolean flight = false;
      @Optional
      float flightSpeed = 0.0F;
      @Optional
      float energyUse = 0.0F;
      @Optional
      float recoilAngleReduction = 0.0F;
      @Optional
      float recoilKickReduction = 0.0F;
      @Optional
      float spreadReduction = 0.0F;

      public Effects() {
         super();
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         tag.putFloat("ArmorBonus", this.armorBonus);
         tag.putFloat("ArmorToughness", this.armorToughness);
         tag.putFloat("KnockbackResistance", this.knockbackResistance);
         tag.putFloat("SpeedModifier", this.speedModifier);
         tag.putFloat("JumpBoost", this.jumpBoost);
         tag.putFloat("FallDamageReduction", this.fallDamageReduction);
         tag.putBoolean("NightVision", this.nightVision);
         tag.putBoolean("Flight", this.flight);
         tag.putFloat("FlightSpeed", this.flightSpeed);
         tag.putFloat("EnergyUse", this.energyUse);
         tag.putFloat("RecoilAngleReduction", this.recoilAngleReduction);
         tag.putFloat("RecoilKickReduction", this.recoilKickReduction);
         tag.putFloat("SpreadReduction", this.spreadReduction);
         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("ArmorBonus", 99)) {
            this.armorBonus = tag.getFloat("ArmorBonus");
         }

         if (tag.contains("ArmorToughness", 99)) {
            this.armorToughness = tag.getFloat("ArmorToughness");
         }

         if (tag.contains("KnockbackResistance", 99)) {
            this.knockbackResistance = tag.getFloat("KnockbackResistance");
         }

         if (tag.contains("SpeedModifier", 99)) {
            this.speedModifier = tag.getFloat("SpeedModifier");
         }

         if (tag.contains("JumpBoost", 99)) {
            this.jumpBoost = tag.getFloat("JumpBoost");
         }

         if (tag.contains("FallDamageReduction", 99)) {
            this.fallDamageReduction = tag.getFloat("FallDamageReduction");
         }

         if (tag.contains("NightVision", 99)) {
            this.nightVision = tag.getBoolean("NightVision");
         }

         if (tag.contains("Flight", 99)) {
            this.flight = tag.getBoolean("Flight");
         }

         if (tag.contains("FlightSpeed", 99)) {
            this.flightSpeed = tag.getFloat("FlightSpeed");
         }

         if (tag.contains("EnergyUse", 99)) {
            this.energyUse = tag.getFloat("EnergyUse");
         }

         if (tag.contains("RecoilAngleReduction", 99)) {
            this.recoilAngleReduction = tag.getFloat("RecoilAngleReduction");
         }

         if (tag.contains("RecoilKickReduction", 99)) {
            this.recoilKickReduction = tag.getFloat("RecoilKickReduction");
         }

         if (tag.contains("SpreadReduction", 99)) {
            this.spreadReduction = tag.getFloat("SpreadReduction");
         }
      }

      public JsonObject toJsonObject() {
         JsonObject object = new JsonObject();
         if (this.armorBonus != 0.0F) {
            object.addProperty("armorBonus", this.armorBonus);
         }

         if (this.armorToughness != 0.0F) {
            object.addProperty("armorToughness", this.armorToughness);
         }

         if (this.knockbackResistance != 0.0F) {
            object.addProperty("knockbackResistance", this.knockbackResistance);
         }

         if (this.speedModifier != 0.0F) {
            object.addProperty("speedModifier", this.speedModifier);
         }

         if (this.jumpBoost != 0.0F) {
            object.addProperty("jumpBoost", this.jumpBoost);
         }

         if (this.fallDamageReduction != 0.0F) {
            object.addProperty("fallDamageReduction", this.fallDamageReduction);
         }

         if (this.nightVision) {
            object.addProperty("nightVision", true);
         }

         if (this.flight) {
            object.addProperty("flight", true);
         }

         if (this.flightSpeed != 0.0F) {
            object.addProperty("flightSpeed", this.flightSpeed);
         }

         if (this.energyUse != 0.0F) {
            object.addProperty("energyUse", this.energyUse);
         }

         if (this.recoilAngleReduction != 0.0F) {
            object.addProperty("recoilAngleReduction", this.recoilAngleReduction);
         }

         if (this.recoilKickReduction != 0.0F) {
            object.addProperty("recoilKickReduction", this.recoilKickReduction);
         }

         if (this.spreadReduction != 0.0F) {
            object.addProperty("spreadReduction", this.spreadReduction);
         }

         return object;
      }

      public ExoSuitUpgrade.Effects copy() {
         ExoSuitUpgrade.Effects effects = new ExoSuitUpgrade.Effects();
         effects.armorBonus = this.armorBonus;
         effects.armorToughness = this.armorToughness;
         effects.knockbackResistance = this.knockbackResistance;
         effects.speedModifier = this.speedModifier;
         effects.jumpBoost = this.jumpBoost;
         effects.fallDamageReduction = this.fallDamageReduction;
         effects.nightVision = this.nightVision;
         effects.flight = this.flight;
         effects.flightSpeed = this.flightSpeed;
         effects.energyUse = this.energyUse;
         effects.recoilAngleReduction = this.recoilAngleReduction;
         effects.recoilKickReduction = this.recoilKickReduction;
         effects.spreadReduction = this.spreadReduction;
         return effects;
      }

      public float getArmorBonus() {
         return this.armorBonus;
      }

      public float getArmorToughness() {
         return this.armorToughness;
      }

      public float getKnockbackResistance() {
         return this.knockbackResistance;
      }

      public float getSpeedModifier() {
         return this.speedModifier;
      }

      public float getJumpBoost() {
         return this.jumpBoost;
      }

      public float getFallDamageReduction() {
         return this.fallDamageReduction;
      }

      public boolean hasNightVision() {
         return this.nightVision;
      }

      public boolean hasFlight() {
         return this.flight;
      }

      public float getFlightSpeed() {
         return this.flightSpeed;
      }

      public float getRecoilAngleReduction() {
         return this.recoilAngleReduction;
      }

      public float getRecoilKickReduction() {
         return this.recoilKickReduction;
      }

      public float getSpreadReduction() {
         return this.spreadReduction;
      }

      public float getEnergyUse() {
         return this.energyUse;
      }
   }
}
