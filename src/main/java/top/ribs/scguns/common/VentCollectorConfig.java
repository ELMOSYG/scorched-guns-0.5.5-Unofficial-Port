package top.ribs.scguns.common;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class VentCollectorConfig  {
   protected VentCollectorConfig.Filters filters = new VentCollectorConfig.Filters();
   protected VentCollectorConfig.Processing processing = new VentCollectorConfig.Processing();

   public VentCollectorConfig() {
      super();
   }

   public VentCollectorConfig.Filters getFilters() {
      return this.filters;
   }

   public VentCollectorConfig.Processing getProcessing() {
      return this.processing;
   }

   public CompoundTag serializeNBT() {
      CompoundTag tag = new CompoundTag();
      tag.put("Filters", this.filters.serializeNBT());
      tag.put("Processing", this.processing.serializeNBT());
      return tag;
   }

   public void deserializeNBT(CompoundTag tag) {
      if (tag.contains("Filters", 10)) {
         this.filters.deserializeNBT(tag.getCompound("Filters"));
      }

      if (tag.contains("Processing", 10)) {
         this.processing.deserializeNBT(tag.getCompound("Processing"));
      }
   }

   public JsonObject toJsonObject() {
      JsonObject object = new JsonObject();
      object.add("filters", this.filters.toJsonObject());
      object.add("processing", this.processing.toJsonObject());
      return object;
   }

   public static VentCollectorConfig create(CompoundTag tag) {
      VentCollectorConfig config = new VentCollectorConfig();
      config.deserializeNBT(tag);
      return config;
   }

   public VentCollectorConfig copy() {
      VentCollectorConfig config = new VentCollectorConfig();
      config.filters = this.filters.copy();
      config.processing = this.processing.copy();
      return config;
   }

   public static class Filters  {
      private int maxCharge = 64;
      private float consumptionChance = 0.5F;
      private int processCooldown = 2;
      private final List<VentCollectorConfig.Filters.FilterItem> filterItems = new ArrayList<>();

      public Filters() {
         super();
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         tag.putInt("MaxCharge", this.maxCharge);
         tag.putFloat("ConsumptionChance", this.consumptionChance);
         tag.putInt("ProcessCooldown", this.processCooldown);
         CompoundTag itemsTag = new CompoundTag();

         for (int i = 0; i < this.filterItems.size(); i++) {
            itemsTag.put("FilterItem" + i, this.filterItems.get(i).serializeNBT());
         }

         tag.put("FilterItems", itemsTag);
         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("MaxCharge", 99)) {
            this.maxCharge = tag.getInt("MaxCharge");
         }

         if (tag.contains("ConsumptionChance", 99)) {
            this.consumptionChance = tag.getFloat("ConsumptionChance");
         }

         if (tag.contains("ProcessCooldown", 99)) {
            this.processCooldown = tag.getInt("ProcessCooldown");
         }

         if (tag.contains("FilterItems", 10)) {
            CompoundTag itemsTag = tag.getCompound("FilterItems");
            this.filterItems.clear();

            for (int i = 0; itemsTag.contains("FilterItem" + i); i++) {
               VentCollectorConfig.Filters.FilterItem item = new VentCollectorConfig.Filters.FilterItem();
               item.deserializeNBT(itemsTag.getCompound("FilterItem" + i));
               this.filterItems.add(item);
            }
         }
      }

      public JsonObject toJsonObject() {
         JsonObject object = new JsonObject();
         if (this.maxCharge != 64) {
            object.addProperty("maxCharge", this.maxCharge);
         }

         if (this.consumptionChance != 0.5F) {
            object.addProperty("consumptionChance", this.consumptionChance);
         }

         if (this.processCooldown != 2) {
            object.addProperty("processCooldown", this.processCooldown);
         }

         if (!this.filterItems.isEmpty()) {
            JsonArray itemsArray = new JsonArray();

            for (VentCollectorConfig.Filters.FilterItem item : this.filterItems) {
               itemsArray.add(item.toJsonObject());
            }

            object.add("filterItems", itemsArray);
         }

         return object;
      }

      public VentCollectorConfig.Filters copy() {
         VentCollectorConfig.Filters filters = new VentCollectorConfig.Filters();
         filters.maxCharge = this.maxCharge;
         filters.consumptionChance = this.consumptionChance;
         filters.processCooldown = this.processCooldown;

         for (VentCollectorConfig.Filters.FilterItem item : this.filterItems) {
            filters.filterItems.add(item.copy());
         }

         return filters;
      }

      public int getMaxCharge() {
         return this.maxCharge;
      }

      public float getConsumptionChance() {
         return this.consumptionChance;
      }

      public int getProcessCooldown() {
         return this.processCooldown;
      }

      public List<VentCollectorConfig.Filters.FilterItem> getFilterItems() {
         return this.filterItems;
      }

      public void setMaxCharge(int maxCharge) {
         this.maxCharge = maxCharge;
      }

      public void setConsumptionChance(float consumptionChance) {
         this.consumptionChance = consumptionChance;
      }

      public void setProcessCooldown(int processCooldown) {
         this.processCooldown = processCooldown;
      }

      public void clearFilterItems() {
         this.filterItems.clear();
      }

      public void addFilterItem(VentCollectorConfig.Filters.FilterItem item) {
         this.filterItems.add(item);
      }

      public static class FilterItem  {
         private ResourceLocation identifier;
         private boolean isTag = true;
         private int chargeAmount;

         public FilterItem() {
            super();
         }

         public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Identifier", this.identifier.toString());
            tag.putBoolean("IsTag", this.isTag);
            tag.putInt("ChargeAmount", this.chargeAmount);
            return tag;
         }

         public void deserializeNBT(CompoundTag tag) {
            if (tag.contains("Identifier", 8)) {
               this.identifier = ResourceLocation.parse(tag.getString("Identifier"));
            }

            if (tag.contains("IsTag", 99)) {
               this.isTag = tag.getBoolean("IsTag");
            }

            if (tag.contains("ChargeAmount", 99)) {
               this.chargeAmount = tag.getInt("ChargeAmount");
            }
         }

         public JsonObject toJsonObject() {
            JsonObject object = new JsonObject();
            if (this.isTag) {
               object.addProperty("tag", this.identifier.toString());
            } else {
               object.addProperty("item", this.identifier.toString());
            }

            object.addProperty("chargeAmount", this.chargeAmount);
            return object;
         }

         public VentCollectorConfig.Filters.FilterItem copy() {
            VentCollectorConfig.Filters.FilterItem item = new VentCollectorConfig.Filters.FilterItem();
            item.identifier = this.identifier;
            item.isTag = this.isTag;
            item.chargeAmount = this.chargeAmount;
            return item;
         }

         public ResourceLocation getIdentifier() {
            return this.identifier;
         }

         public boolean isTag() {
            return this.isTag;
         }

         public int getChargeAmount() {
            return this.chargeAmount;
         }

         public void setIdentifier(ResourceLocation identifier) {
            this.identifier = identifier;
         }

         public void setIsTag(boolean isTag) {
            this.isTag = isTag;
         }

         public void setChargeAmount(int chargeAmount) {
            this.chargeAmount = chargeAmount;
         }
      }
   }

   public static class Processing  {
      private float powerSpeedMultiplier = 0.35F;
      private int pushCooldown = 5;

      public Processing() {
         super();
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         tag.putFloat("PowerSpeedMultiplier", this.powerSpeedMultiplier);
         tag.putInt("PushCooldown", this.pushCooldown);
         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("PowerSpeedMultiplier", 99)) {
            this.powerSpeedMultiplier = tag.getFloat("PowerSpeedMultiplier");
         }

         if (tag.contains("PushCooldown", 99)) {
            this.pushCooldown = tag.getInt("PushCooldown");
         }
      }

      public JsonObject toJsonObject() {
         JsonObject object = new JsonObject();
         if (this.powerSpeedMultiplier != 0.35F) {
            object.addProperty("powerSpeedMultiplier", this.powerSpeedMultiplier);
         }

         if (this.pushCooldown != 5) {
            object.addProperty("pushCooldown", this.pushCooldown);
         }

         return object;
      }

      public VentCollectorConfig.Processing copy() {
         VentCollectorConfig.Processing processing = new VentCollectorConfig.Processing();
         processing.powerSpeedMultiplier = this.powerSpeedMultiplier;
         processing.pushCooldown = this.pushCooldown;
         return processing;
      }

      public float getPowerSpeedMultiplier() {
         return this.powerSpeedMultiplier;
      }

      public int getPushCooldown() {
         return this.pushCooldown;
      }

      public void setPowerSpeedMultiplier(float powerSpeedMultiplier) {
         this.powerSpeedMultiplier = powerSpeedMultiplier;
      }

      public void setPushCooldown(int pushCooldown) {
         this.pushCooldown = pushCooldown;
      }
   }
}
