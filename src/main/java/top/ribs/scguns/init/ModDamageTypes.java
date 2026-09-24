package top.ribs.scguns.init;

import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.ribs.scguns.entity.projectile.ProjectileEntity;
import top.ribs.scguns.util.GunModifierHelper;

public class ModDamageTypes {
   public static final ResourceKey<DamageType> BULLET = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("scguns", "bullet"));
   public static final ResourceKey<DamageType> MELEE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("scguns", "melee"));

   public ModDamageTypes() {
      super();
   }

   public static class Sources {
      public Sources() {
         super();
      }

      private static Reference<DamageType> getHolder(RegistryAccess access, ResourceKey<DamageType> damageTypeKey) {
         return access.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(damageTypeKey);
      }

      public static DamageSource projectile(RegistryAccess access, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
         return new DamageSource(getHolder(access, ModDamageTypes.BULLET), directEntity, causingEntity);
      }

      private static DamageSource source(
         RegistryAccess access, ResourceKey<DamageType> damageTypeKey, @Nullable Entity directEntity, @Nullable Entity causingEntity
      ) {
         return new ModDamageTypes.Sources.BulletDamageSource(getHolder(access, damageTypeKey), directEntity, causingEntity);
      }

      public static DamageSource projectile(RegistryAccess access, ProjectileEntity projectile, LivingEntity entity) {
         return source(access, ModDamageTypes.BULLET, projectile, entity);
      }

      public static DamageSource melee(RegistryAccess access, LivingEntity entity) {
         return source(access, ModDamageTypes.MELEE, null, entity);
      }

      public static class BulletDamageSource extends DamageSource {
         private static final String[] msgSuffix = new String[]{
            "scguns.bullet.killed", "scguns.bullet.eliminated", "scguns.bullet.executed", "scguns.bullet.annihilated", "scguns.bullet.decimated"
         };

         public BulletDamageSource(Holder<DamageType> pType, Entity pDirectEntity, Entity pCausingEntity) {
            super(pType, pDirectEntity, pCausingEntity);
         }

         public Component getLocalizedDeathMessage(LivingEntity pLivingEntity) {
            String s = "death.attack." + this.getMsgId();
            if (this.getEntity() == null && this.getDirectEntity() == null) {
               LivingEntity living = pLivingEntity.getKillCredit();
               return living != null
                  ? Component.translatable(s + ".player", new Object[]{pLivingEntity.getDisplayName(), living.getDisplayName()})
                  : Component.translatable(s, new Object[]{pLivingEntity.getDisplayName()});
            } else {
               Component component = this.getEntity() == null ? this.getDirectEntity().getDisplayName() : this.getEntity().getDisplayName();
               ItemStack stack = this.getEntity() instanceof LivingEntity livingentity ? livingentity.getMainHandItem() : ItemStack.EMPTY;
               boolean isSilenced = GunModifierHelper.isSilencedFire(stack);
               return isSilenced
                  ? Component.translatable(s + ".silenced", new Object[]{pLivingEntity.getDisplayName()})
                  : (
                     !stack.isEmpty() && stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)
                        ? Component.translatable(s + ".item", new Object[]{pLivingEntity.getDisplayName(), component, stack.getDisplayName()})
                        : Component.translatable(s, new Object[]{pLivingEntity.getDisplayName(), component})
                  );
            }
         }

         public String getMsgId() {
            return msgSuffix[ThreadLocalRandom.current().nextInt(5)];
         }
      }
   }
}
