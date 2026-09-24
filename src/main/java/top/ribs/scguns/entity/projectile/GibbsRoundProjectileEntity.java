package top.ribs.scguns.entity.projectile;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.init.ModDamageTypes;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageBlood;
import top.ribs.scguns.network.message.S2CMessageProjectileHitEntity;
import top.ribs.scguns.util.GunEnchantmentHelper;

public class GibbsRoundProjectileEntity extends ProjectileEntity {
   private static final float ADVANCED_SHIELD_DISABLE_CHANCE = 0.45F;
   private static final float HEADSHOT_EFFECT_DURATION_MULTIPLIER = 1.35F;
   private static final float GIBBS_ROUND_XP_MULTIPLIER = 0.25F;
   private static final int GIBBS_ROUND_LOOTING_LEVEL = 4;

   public GibbsRoundProjectileEntity(EntityType<? extends Entity> entityType, Level worldIn) {
      super(entityType, worldIn);
   }

   public GibbsRoundProjectileEntity(
      EntityType<? extends Entity> entityType, Level worldIn, LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun
   ) {
      super(entityType, worldIn, shooter, weapon, item, modifiedGun);
   }

   // 0.5.5 registered this class on the game bus to add a flat looting bonus via
   // Forge's LootingLevelEvent. NeoForge 21.1 has no such event and its
   // LivingDropsEvent carries no modifiable looting level, so the handler could not
   // be ported; registering a class with no @SubscribeEvent methods aborts mod
   // loading ("class ... has no @SubscribeEvent methods, but register was called
   // anyway"), so the registration is gone. See HANDOFF.md "known deviations".



   @Override
   protected void onHitEntity(Entity entity, Vec3 hitVec, Vec3 startVec, Vec3 endVec, boolean headshot) {
      float damage = this.getDamage();
      float newDamage = this.getCriticalDamage(this.getWeapon(), this.random, damage);
      boolean critical = damage != newDamage;
      ResourceLocation advantage = this.getProjectile().getAdvantage();
      damage = newDamage * this.advantageMultiplier(entity);
      boolean wasAlive = entity instanceof LivingEntity && entity.isAlive();
      if (headshot) {
         damage = (float)((double)damage * (Double)Config.COMMON.gameplay.headShotDamageMultiplier.get());
      }

      if (entity instanceof LivingEntity livingTarget) {
         damage = this.applyProjectileProtection(livingTarget, damage);
         damage = this.calculateArmorBypassDamage(livingTarget, damage);
      }

      if (entity instanceof LivingEntity livingTarget) {
         damage = GunEnchantmentHelper.getPuncturingDamageReduction(this.getWeapon(), livingTarget, damage);
         damage = this.applyProjectileProtection(livingTarget, damage);
         damage = this.calculateArmorBypassDamage(livingTarget, damage);
      }

      boolean blocked = ProjectileEntity.ProjectileHelper.handleShieldHit(entity, this, damage, 0.45F);
      if (!blocked) {
         DamageSource source = ModDamageTypes.Sources.projectile(this.level().registryAccess(), this, (LivingEntity)this.getOwner());
         if (!entity.getType().is(ModTags.Entities.GHOST) || advantage.equals(ModTags.Entities.UNDEAD.location())) {
            entity.hurt(source, damage);
            if (entity instanceof LivingEntity livingEntity) {
               ResourceLocation effectLocation = this.getProjectile().getImpactEffect();
               if (effectLocation != null) {
                  float effectChance = this.getProjectile().getImpactEffectChance();
                  if (this.random.nextFloat() < effectChance) {
                     MobEffect effect = (MobEffect)BuiltInRegistries.MOB_EFFECT.get(effectLocation);
                     if (effect != null) {
                        int duration = this.getProjectile().getImpactEffectDuration();
                        if (headshot) {
                           duration = (int)((float)duration * 1.35F);
                        }

                        livingEntity.addEffect(new MobEffectInstance(top.ribs.scguns.util.ScEffects.holder(effect), duration, this.getProjectile().getImpactEffectAmplifier()));
                     }
                  }
               }
            }
         }

         if (entity instanceof LivingEntity) {
            GunEnchantmentHelper.applyElementalPopEffect(this.getWeapon(), (LivingEntity)entity);
         }
      }

      if (this.shooter instanceof Player) {
         int hitType = critical ? 2 : (headshot ? 1 : 0);
         PacketHandler.getPlayChannel()
            .sendToPlayer(
               () -> (ServerPlayer)this.shooter,
               new S2CMessageProjectileHitEntity(hitVec.x, hitVec.y, hitVec.z, hitType, entity instanceof Player)
            );
      }

      if (wasAlive && entity instanceof LivingEntity livingEntityx && !livingEntityx.isAlive()) {
         this.checkForDiamondSteelBonus(livingEntityx, hitVec);
         this.checkForGibbsXPBonus(livingEntityx, hitVec);
      }

      PacketHandler.getPlayChannel().sendToTrackingEntity(() -> entity, new S2CMessageBlood(hitVec.x, hitVec.y, hitVec.z, entity.getType()));
   }

   private void checkForGibbsXPBonus(LivingEntity killedEntity, Vec3 position) {
      if (!this.level().isClientSide && this.getShooter() instanceof Player) {
         int baseXP = killedEntity.getExperienceReward((net.minecraft.server.level.ServerLevel) this.level(), null);
         int gibbsXP = Math.round((float)baseXP * 0.25F);
         if (gibbsXP > 0) {
            ExperienceOrb xpOrb = new ExperienceOrb(this.level(), position.x, position.y, position.z, gibbsXP);
            this.level().addFreshEntity(xpOrb);
         }
      }
   }

   @Override
   protected void onHitBlock(BlockState state, BlockPos pos, Direction face, double x, double y, double z) {
      super.onHitBlock(state, pos, face, x, y, z);
   }

   @Override
   public void onExpired() {
   }
}
