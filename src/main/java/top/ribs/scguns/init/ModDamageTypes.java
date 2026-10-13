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
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.entity.projectile.ProjectileEntity;
import top.ribs.scguns.util.GunModifierHelper;

public class ModDamageTypes {
   /** Past vanilla's 1e-4 degeneracy check, small enough not to be felt. */
   private static final double EFFECT_KNOCKBACK_OFFSET = 0.05;

   public static final ResourceKey<DamageType> BULLET = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("scguns", "bullet"));
   public static final ResourceKey<DamageType> MELEE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("scguns", "melee"));
   public static final ResourceKey<DamageType> LACERATED = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("scguns", "lacerated"));
   public static final ResourceKey<DamageType> SULFUR_POISON = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("scguns", "sulfur_poison"));

   public ModDamageTypes() {
      super();
   }

   public static class Sources {
      public Sources() {
         super();
      }

      /** The lacerated (bleeding) effect's damage: its own type, so it has its own death message. */
      public static DamageSource lacerated(RegistryAccess access, LivingEntity victim) {
         return effect(access, ModDamageTypes.LACERATED, victim);
      }

      /** Sulfur poisoning's damage, likewise its own type rather than plain magic. */
      public static DamageSource sulfurPoison(RegistryAccess access, LivingEntity victim) {
         return effect(access, ModDamageTypes.SULFUR_POISON, victim);
      }

      /**
       * Damage from a mob effect, which has no projectile and no attacker.
       *
       * <p>The position matters. A bare {@code new DamageSource(holder)} carries neither a position nor an
       * entity, so 1.21's {@code DamageSource#getSourcePosition} returns null and {@code LivingEntity#hurt} has
       * no direction to knock the victim along: it calls {@code knockback(0.4, 0, 0)}, which vanilla turns into
       * {@code (random() - random()) * 0.01} - a shove in a random direction on every effect tick. The offset used
       * here is past vanilla's degeneracy check but small enough that the push is imperceptible and always the
       * same direction. Same reasoning as the positional beam source above (HANDOFF 83.10).</p>
       */
      private static DamageSource effect(RegistryAccess access, ResourceKey<DamageType> type, LivingEntity victim) {
         return new DamageSource(getHolder(access, type), victim, victim,
            victim.position().add(EFFECT_KNOCKBACK_OFFSET, 0.0, 0.0));
      }

      private static Reference<DamageType> getHolder(RegistryAccess access, ResourceKey<DamageType> damageTypeKey) {
         return access.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(damageTypeKey);
      }

      public static DamageSource projectile(RegistryAccess access, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
         return new DamageSource(getHolder(access, ModDamageTypes.BULLET), directEntity, causingEntity);
      }

      /**
       * A damage source that carries an explicit position, for damage that has no projectile entity behind it.
       *
       * <p>Needed because 1.21's {@code DamageSource#getSourcePosition} reads
       * <b>only the direct entity</b> and never the causing entity:
       * {@code damageSourcePosition != null ? damageSourcePosition : (directEntity != null ? directEntity.position() : null)}.
       * A hitscan beam has no projectile, so with a null direct entity the position is null - and
       * then {@code LivingEntity#hurt} has neither branch to fill its knockback direction in and
       * calls {@code knockback(0.4, 0, 0)}. Vanilla's {@code knockback} replaces a near-zero vector
       * with {@code (random() - random()) * 0.01}, so the mob was shoved in a <b>random</b>
       * direction (HANDOFF 83.10). Passing the hit point gives it the direction the beam came
       * from, and also fixes the direction of the damage-tilt flash, which uses the same pair.</p>
       */
      public static DamageSource projectile(RegistryAccess access, @Nullable Entity directEntity, @Nullable Entity causingEntity, @Nullable Vec3 sourcePosition) {
         return new ModDamageTypes.Sources.BulletDamageSource(getHolder(access, ModDamageTypes.BULLET), directEntity, causingEntity, sourcePosition);
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

         public BulletDamageSource(Holder<DamageType> pType, @Nullable Entity pDirectEntity, @Nullable Entity pCausingEntity, @Nullable Vec3 pSourcePosition) {
            super(pType, pDirectEntity, pCausingEntity, pSourcePosition);
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
