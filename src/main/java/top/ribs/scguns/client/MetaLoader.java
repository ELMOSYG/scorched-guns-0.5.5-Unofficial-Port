package top.ribs.scguns.client;


import net.minecraft.core.registries.BuiltInRegistries;
import com.mrcrayfish.framework.api.serialize.DataObject;
import com.mrcrayfish.framework.client.resources.IDataLoader;
import com.mrcrayfish.framework.client.resources.IResourceSupplier;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.util.strategy.IdentityStrategy;
import org.apache.commons.lang3.tuple.Pair;
import top.ribs.scguns.item.IMeta;

public final class MetaLoader implements IDataLoader<MetaLoader.ItemResource> {
   private static MetaLoader instance;
   private final Object2ObjectMap<Item, DataObject> itemToData = (Object2ObjectMap<Item, DataObject>)Util.make(
      new Object2ObjectOpenCustomHashMap(IdentityStrategy.IDENTITY), map -> map.defaultReturnValue(DataObject.EMPTY)
   );

   public static MetaLoader getInstance() {
      if (instance == null) {
         instance = new MetaLoader();
      }

      return instance;
   }

   private MetaLoader() {
   }

   public DataObject getData(Item item) {
      return (DataObject)this.itemToData.get(item);
   }

   public List<MetaLoader.ItemResource> getResourceSuppliers() {
      List<MetaLoader.ItemResource> resources = new ArrayList<>();
      // 1.21 registries are Iterable; the 1.20.1 Registry#getValues() collection is gone.
      for (Item item : BuiltInRegistries.ITEM) {
         if (item instanceof IMeta) {
            ResourceLocation key = item.builtInRegistryHolder().key().location();
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "models/item/" + key.getPath() + ".scmeta");
            resources.add(new MetaLoader.ItemResource(item, location));
         }
      }

      return resources;
   }

   public void process(List<Pair<MetaLoader.ItemResource, DataObject>> list) {
      this.itemToData.clear();
      list.forEach(pair -> {
         DataObject object = (DataObject)pair.getRight();
         if (!object.isEmpty()) {
            MetaLoader.ItemResource resource = (MetaLoader.ItemResource)pair.getLeft();
            this.itemToData.put(resource.item(), object);
         }
      });
   }

   public boolean ignoreMissing() {
      return true;
   }

   public static record ItemResource(Item item, ResourceLocation location) implements IResourceSupplier {
      public ItemResource(Item item, ResourceLocation location) {
         this.item = item;
         this.location = location;
      }

      public ResourceLocation getLocation() {
         return this.location;
      }
   }
}
