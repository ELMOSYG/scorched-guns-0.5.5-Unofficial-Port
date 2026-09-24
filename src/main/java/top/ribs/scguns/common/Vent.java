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

public class Vent  {
   protected Vent.Activation activation = new Vent.Activation();
   protected Vent.Power power = new Vent.Power();
   protected Vent.Production production = new Vent.Production();
   protected Vent.Placement placement = new Vent.Placement();
   protected Vent.Particles particles = new Vent.Particles();

   public Vent() {
      super();
   }

   public Vent.Activation getActivation() {
      return this.activation;
   }

   public Vent.Power getPower() {
      return this.power;
   }

   public Vent.Production getProduction() {
      return this.production;
   }

   public Vent.Placement getPlacement() {
      return this.placement;
   }

   public Vent.Particles getParticles() {
      return this.particles;
   }

   public CompoundTag serializeNBT() {
      CompoundTag tag = new CompoundTag();
      tag.put("Activation", this.activation.serializeNBT());
      tag.put("Power", this.power.serializeNBT());
      tag.put("Production", this.production.serializeNBT());
      tag.put("Placement", this.placement.serializeNBT());
      tag.put("Particles", this.particles.serializeNBT());
      return tag;
   }

   public void deserializeNBT(CompoundTag tag) {
      if (tag.contains("Activation", 10)) {
         this.activation.deserializeNBT(tag.getCompound("Activation"));
      }

      if (tag.contains("Power", 10)) {
         this.power.deserializeNBT(tag.getCompound("Power"));
      }

      if (tag.contains("Production", 10)) {
         this.production.deserializeNBT(tag.getCompound("Production"));
      }

      if (tag.contains("Placement", 10)) {
         this.placement.deserializeNBT(tag.getCompound("Placement"));
      }

      if (tag.contains("Particles", 10)) {
         this.particles.deserializeNBT(tag.getCompound("Particles"));
      }
   }

   public JsonObject toJsonObject() {
      JsonObject object = new JsonObject();
      object.add("activation", this.activation.toJsonObject());
      object.add("power", this.power.toJsonObject());
      object.add("production", this.production.toJsonObject());
      object.add("placement", this.placement.toJsonObject());
      object.add("particles", this.particles.toJsonObject());
      return object;
   }

   public static Vent create(CompoundTag tag) {
      Vent vent = new Vent();
      vent.deserializeNBT(tag);
      return vent;
   }

   public Vent copy() {
      Vent vent = new Vent();
      vent.activation = this.activation.copy();
      vent.power = this.power.copy();
      vent.production = this.production.copy();
      vent.placement = this.placement.copy();
      vent.particles = this.particles.copy();
      return vent;
   }

   public static class Activation  {
      private ResourceLocation baseBlock = ResourceLocation.parse("minecraft:magma_block");
      private boolean requiresWaterlogged = false;

      public Activation() {
         super();
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         tag.putString("BaseBlock", this.baseBlock.toString());
         tag.putBoolean("RequiresWaterlogged", this.requiresWaterlogged);
         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("BaseBlock", 8)) {
            this.baseBlock = ResourceLocation.parse(tag.getString("BaseBlock"));
         }

         if (tag.contains("RequiresWaterlogged", 99)) {
            this.requiresWaterlogged = tag.getBoolean("RequiresWaterlogged");
         }
      }

      public JsonObject toJsonObject() {
         JsonObject object = new JsonObject();
         if (!this.baseBlock.equals(ResourceLocation.parse("minecraft:magma_block"))) {
            object.addProperty("baseBlock", this.baseBlock.toString());
         }

         if (this.requiresWaterlogged) {
            object.addProperty("requiresWaterlogged", true);
         }

         return object;
      }

      public Vent.Activation copy() {
         Vent.Activation activation = new Vent.Activation();
         activation.baseBlock = this.baseBlock;
         activation.requiresWaterlogged = this.requiresWaterlogged;
         return activation;
      }

      public ResourceLocation getBaseBlock() {
         return this.baseBlock;
      }

      public boolean requiresWaterlogged() {
         return this.requiresWaterlogged;
      }

      public void setBaseBlock(ResourceLocation baseBlock) {
         this.baseBlock = baseBlock;
      }

      public void setRequiresWaterlogged(boolean requiresWaterlogged) {
         this.requiresWaterlogged = requiresWaterlogged;
      }
   }

   public static class Particles  {
      private boolean showActive = true;
      @Optional
      private ResourceLocation activeSound;

      public Particles() {
         super();
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         tag.putBoolean("ShowActive", this.showActive);
         if (this.activeSound != null) {
            tag.putString("ActiveSound", this.activeSound.toString());
         }

         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("ShowActive", 99)) {
            this.showActive = tag.getBoolean("ShowActive");
         }

         if (tag.contains("ActiveSound", 8)) {
            this.activeSound = ResourceLocation.parse(tag.getString("ActiveSound"));
         }
      }

      public JsonObject toJsonObject() {
         JsonObject object = new JsonObject();
         if (!this.showActive) {
            object.addProperty("showActive", false);
         }

         if (this.activeSound != null) {
            object.addProperty("activeSound", this.activeSound.toString());
         }

         return object;
      }

      public Vent.Particles copy() {
         Vent.Particles particles = new Vent.Particles();
         particles.showActive = this.showActive;
         particles.activeSound = this.activeSound;
         return particles;
      }

      public boolean showActive() {
         return this.showActive;
      }

      @Nullable
      public ResourceLocation getActiveSound() {
         return this.activeSound;
      }

      public void setShowActive(boolean showActive) {
         this.showActive = showActive;
      }

      public void setActiveSound(ResourceLocation activeSound) {
         this.activeSound = activeSound;
      }
   }

   public static class Placement  {
      private boolean enabled = false;
      @Optional
      private ResourceLocation blockToPlace;
      private int radius = 8;
      private float placementChance = 0.35F;

      public Placement() {
         super();
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         tag.putBoolean("Enabled", this.enabled);
         if (this.blockToPlace != null) {
            tag.putString("BlockToPlace", this.blockToPlace.toString());
         }

         tag.putInt("Radius", this.radius);
         tag.putFloat("PlacementChance", this.placementChance);
         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("Enabled", 99)) {
            this.enabled = tag.getBoolean("Enabled");
         }

         if (tag.contains("BlockToPlace", 8)) {
            this.blockToPlace = ResourceLocation.parse(tag.getString("BlockToPlace"));
         }

         if (tag.contains("Radius", 99)) {
            this.radius = tag.getInt("Radius");
         }

         if (tag.contains("PlacementChance", 99)) {
            this.placementChance = tag.getFloat("PlacementChance");
         }
      }

      public JsonObject toJsonObject() {
         JsonObject object = new JsonObject();
         if (this.enabled) {
            object.addProperty("enabled", true);
         }

         if (this.blockToPlace != null) {
            object.addProperty("blockToPlace", this.blockToPlace.toString());
         }

         if (this.radius != 8) {
            object.addProperty("radius", this.radius);
         }

         if (this.placementChance != 0.35F) {
            object.addProperty("placementChance", this.placementChance);
         }

         return object;
      }

      public Vent.Placement copy() {
         Vent.Placement placement = new Vent.Placement();
         placement.enabled = this.enabled;
         placement.blockToPlace = this.blockToPlace;
         placement.radius = this.radius;
         placement.placementChance = this.placementChance;
         return placement;
      }

      public boolean isEnabled() {
         return this.enabled;
      }

      @Nullable
      public ResourceLocation getBlockToPlace() {
         return this.blockToPlace;
      }

      public int getRadius() {
         return this.radius;
      }

      public float getPlacementChance() {
         return this.placementChance;
      }

      public void setEnabled(boolean enabled) {
         this.enabled = enabled;
      }

      public void setBlockToPlace(ResourceLocation blockToPlace) {
         this.blockToPlace = blockToPlace;
      }

      public void setRadius(int radius) {
         this.radius = radius;
      }

      public void setPlacementChance(float placementChance) {
         this.placementChance = placementChance;
      }
   }

   public static class Power  {
      private int maxPower = 5;
      private int baseTickInterval = 100;
      private int tickWiggleRoom = 60;

      public Power() {
         super();
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         tag.putInt("MaxPower", this.maxPower);
         tag.putInt("BaseTickInterval", this.baseTickInterval);
         tag.putInt("TickWiggleRoom", this.tickWiggleRoom);
         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("MaxPower", 99)) {
            this.maxPower = tag.getInt("MaxPower");
         }

         if (tag.contains("BaseTickInterval", 99)) {
            this.baseTickInterval = tag.getInt("BaseTickInterval");
         }

         if (tag.contains("TickWiggleRoom", 99)) {
            this.tickWiggleRoom = tag.getInt("TickWiggleRoom");
         }
      }

      public JsonObject toJsonObject() {
         JsonObject object = new JsonObject();
         if (this.maxPower != 5) {
            object.addProperty("maxPower", this.maxPower);
         }

         if (this.baseTickInterval != 100) {
            object.addProperty("baseTickInterval", this.baseTickInterval);
         }

         if (this.tickWiggleRoom != 60) {
            object.addProperty("tickWiggleRoom", this.tickWiggleRoom);
         }

         return object;
      }

      public Vent.Power copy() {
         Vent.Power power = new Vent.Power();
         power.maxPower = this.maxPower;
         power.baseTickInterval = this.baseTickInterval;
         power.tickWiggleRoom = this.tickWiggleRoom;
         return power;
      }

      public int getMaxPower() {
         return this.maxPower;
      }

      public int getBaseTickInterval() {
         return this.baseTickInterval;
      }

      public int getTickWiggleRoom() {
         return this.tickWiggleRoom;
      }

      public void setMaxPower(int maxPower) {
         this.maxPower = maxPower;
      }

      public void setBaseTickInterval(int baseTickInterval) {
         this.baseTickInterval = baseTickInterval;
      }

      public void setTickWiggleRoom(int tickWiggleRoom) {
         this.tickWiggleRoom = tickWiggleRoom;
      }
   }

   public static class Production  {
      private List<Vent.Production.OutputItem> outputs = new ArrayList<>();
      private float productionChance = 0.4F;

      public Production() {
         super();
      }

      public CompoundTag serializeNBT() {
         CompoundTag tag = new CompoundTag();
         CompoundTag outputsTag = new CompoundTag();

         for (int i = 0; i < this.outputs.size(); i++) {
            outputsTag.put("Output" + i, this.outputs.get(i).serializeNBT());
         }

         tag.put("Outputs", outputsTag);
         tag.putFloat("ProductionChance", this.productionChance);
         return tag;
      }

      public void deserializeNBT(CompoundTag tag) {
         if (tag.contains("Outputs", 10)) {
            CompoundTag outputsTag = tag.getCompound("Outputs");
            this.outputs.clear();

            for (int i = 0; outputsTag.contains("Output" + i); i++) {
               Vent.Production.OutputItem output = new Vent.Production.OutputItem();
               output.deserializeNBT(outputsTag.getCompound("Output" + i));
               this.outputs.add(output);
            }
         }

         if (tag.contains("ProductionChance", 99)) {
            this.productionChance = tag.getFloat("ProductionChance");
         }
      }

      public JsonObject toJsonObject() {
         JsonObject object = new JsonObject();
         if (!this.outputs.isEmpty()) {
            JsonArray outputsArray = new JsonArray();

            for (Vent.Production.OutputItem output : this.outputs) {
               outputsArray.add(output.toJsonObject());
            }

            object.add("outputs", outputsArray);
         }

         if (this.productionChance != 0.4F) {
            object.addProperty("productionChance", this.productionChance);
         }

         return object;
      }

      public Vent.Production copy() {
         Vent.Production production = new Vent.Production();

         for (Vent.Production.OutputItem output : this.outputs) {
            production.outputs.add(output.copy());
         }

         production.productionChance = this.productionChance;
         return production;
      }

      public List<Vent.Production.OutputItem> getOutputs() {
         return this.outputs;
      }

      public float getProductionChance() {
         return this.productionChance;
      }

      public void clearOutputs() {
         this.outputs.clear();
      }

      public void addOutput(Vent.Production.OutputItem output) {
         this.outputs.add(output);
      }

      public void setProductionChance(float productionChance) {
         this.productionChance = productionChance;
      }

      public static class OutputItem  {
         private ResourceLocation item;
         private int weight = 1;

         public OutputItem() {
            super();
         }

         public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Item", this.item.toString());
            tag.putInt("Weight", this.weight);
            return tag;
         }

         public void deserializeNBT(CompoundTag tag) {
            if (tag.contains("Item", 8)) {
               this.item = ResourceLocation.parse(tag.getString("Item"));
            }

            if (tag.contains("Weight", 99)) {
               this.weight = tag.getInt("Weight");
            }
         }

         public JsonObject toJsonObject() {
            JsonObject object = new JsonObject();
            object.addProperty("item", this.item.toString());
            if (this.weight != 1) {
               object.addProperty("weight", this.weight);
            }

            return object;
         }

         public Vent.Production.OutputItem copy() {
            Vent.Production.OutputItem output = new Vent.Production.OutputItem();
            output.item = this.item;
            output.weight = this.weight;
            return output;
         }

         @Nullable
         public Item getItem() {
            return (Item)BuiltInRegistries.ITEM.get(this.item);
         }

         public int getWeight() {
            return this.weight;
         }

         public void setItem(ResourceLocation item) {
            this.item = item;
         }

         public void setWeight(int weight) {
            this.weight = weight;
         }
      }
   }
}
