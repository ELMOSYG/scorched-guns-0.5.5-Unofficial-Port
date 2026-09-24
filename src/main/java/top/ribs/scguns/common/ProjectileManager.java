package top.ribs.scguns.common;


import net.minecraft.core.registries.BuiltInRegistries;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import top.ribs.scguns.entity.projectile.ProjectileEntity;
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.interfaces.IProjectileFactory;

public class ProjectileManager {
   private static ProjectileManager instance = null;
   private final IProjectileFactory DEFAULT_FACTORY = (worldIn, entity, weapon, item, modifiedGun) -> new ProjectileEntity(
         (EntityType<? extends Entity>)ModEntities.PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun
      );
   private final Map<ResourceLocation, IProjectileFactory> projectileFactoryMap = new HashMap<>();

   public ProjectileManager() {
      super();
   }

   public static ProjectileManager getInstance() {
      if (instance == null) {
         instance = new ProjectileManager();
      }

      return instance;
   }

   public void registerFactory(Item ammo, IProjectileFactory factory) {
      this.projectileFactoryMap.put(BuiltInRegistries.ITEM.getKey(ammo), factory);
   }

   public IProjectileFactory getFactory(ResourceLocation id) {
      return this.projectileFactoryMap.getOrDefault(id, this.DEFAULT_FACTORY);
   }
}
