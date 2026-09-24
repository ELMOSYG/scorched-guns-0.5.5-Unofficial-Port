package top.ribs.scguns.entity.client;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.entity.projectile.EnemyProjectileEntity;

public class EnemyProjectileRenderer extends ArrowRenderer<EnemyProjectileEntity> {
   public EnemyProjectileRenderer(Context context) {
      super(context);
   }

   public ResourceLocation getTextureLocation(EnemyProjectileEntity entity) {
      return ResourceLocation.fromNamespaceAndPath("scguns", "textures/entity/projectiles/brass_bolt.png");
   }
}
