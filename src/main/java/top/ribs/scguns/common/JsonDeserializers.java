package top.ribs.scguns.common;

import com.google.gson.JsonDeserializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.CraftingHelper;
import top.ribs.scguns.client.util.Easings;

public class JsonDeserializers {
   public static final JsonDeserializer<ItemStack> ITEM_STACK = (json, typeOfT, context) -> ItemStack.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json.getAsJsonObject()).getOrThrow();
   public static final JsonDeserializer<ResourceLocation> RESOURCE_LOCATION = (json, typeOfT, context) -> ResourceLocation.parse(json.getAsString());
   public static final JsonDeserializer<FireMode> FIRE_MODE = (json, typeOfT, context) -> FireMode.getType(ResourceLocation.tryParse(json.getAsString()));
   public static final JsonDeserializer<ReloadType> RELOAD_TYPE = (json, typeOfT, context) -> ReloadType.getType(ResourceLocation.tryParse(json.getAsString()));
   public static final JsonDeserializer<GripType> GRIP_TYPE = (json, typeOfT, context) -> GripType.getType(ResourceLocation.tryParse(json.getAsString()));
   public static final JsonDeserializer<Easings> EASING = (json, typeOfT, context) -> Easings.byName(json.getAsString());

   public JsonDeserializers() {
      super();
   }
}
