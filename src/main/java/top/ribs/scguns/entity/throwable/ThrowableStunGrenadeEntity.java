package top.ribs.scguns.entity.throwable;

import com.mrcrayfish.framework.api.network.LevelLocation;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.ribs.scguns.Config;
import top.ribs.scguns.init.ModEffects;
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.init.ModItems;
import top.ribs.scguns.init.ModSounds;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageStunGrenade;
import top.ribs.scguns.util.ExplosionHelper;

@EventBusSubscriber
public class ThrowableStunGrenadeEntity extends ThrowableGrenadeEntity {
   public ThrowableStunGrenadeEntity(EntityType<? extends ThrowableGrenadeEntity> entityType, Level world) {
      super(entityType, world);
   }

   public ThrowableStunGrenadeEntity(EntityType<? extends ThrowableGrenadeEntity> entityType, Level world, LivingEntity player) {
      super(entityType, world, player);
      this.setItem(new ItemStack((ItemLike)ModItems.STUN_GRENADE.get()));
   }

   public ThrowableStunGrenadeEntity(Level world, LivingEntity player, int maxCookTime) {
      super((EntityType<? extends ThrowableItemEntity>)ModEntities.THROWABLE_STUN_GRENADE.get(), world, player);
      this.setItem(new ItemStack((ItemLike)ModItems.STUN_GRENADE.get()));
      this.setMaxLife(maxCookTime);
      this.setShouldBounce(false);
   }

   @SubscribeEvent
   public static void blindMobs(LivingChangeTargetEvent event) {
      if ((Boolean)Config.COMMON.stunGrenades.blind.blindMobs.get()
         && event.getOriginalAboutToBeSetTarget() != null
         && event.getEntity() instanceof Mob
         && event.getEntity().hasEffect(ModEffects.BLINDED)) {
         ((Mob)event.getEntity()).setTarget(null);
      }
   }

   @Override
   public void onDeath() {
      double y = this.getY() + (double)this.getType().getDimensions().height() * 0.5;
      this.level()
         .playSound(
            null,
            this.getX(),
            y,
            this.getZ(),
            (SoundEvent)ModSounds.ENTITY_STUN_GRENADE_EXPLOSION.get(),
            SoundSource.BLOCKS,
            2.0F,
            (1.0F + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2F) * 0.7F
         );
      if (!this.level().isClientSide) {
         PacketHandler.getPlayChannel()
            .sendToNearbyPlayers(
               () -> LevelLocation.create((ServerLevel) this.level(), this.getX(), y, this.getZ(), 64.0),
               new S2CMessageStunGrenade(this.getX(), y, this.getZ())
            );
         double diameter = Math.max(
                  (Double)Config.COMMON.stunGrenades.deafen.criteria.radius.get(), (Double)Config.COMMON.stunGrenades.blind.criteria.radius.get()
               )
               * 2.0
            + 1.0;
         int minX = Mth.floor(this.getX() - diameter);
         int maxX = Mth.floor(this.getX() + diameter);
         int minY = Mth.floor(y - diameter);
         int maxY = Mth.floor(y + diameter);
         int minZ = Mth.floor(this.getZ() - diameter);
         int maxZ = Mth.floor(this.getZ() + diameter);
         Vec3 grenade = new Vec3(this.getX(), y, this.getZ());

         for (LivingEntity entity : this.level()
            .getEntitiesOfClass(LivingEntity.class, new AABB((double)minX, (double)minY, (double)minZ, (double)maxX, (double)maxY, (double)maxZ))) {
            // 0.5.5's guard, unchanged: in 1.20.1 it skipped nothing but marker armor stands, so
            // creative and spectator players were affected like anyone else. See ExplosionHelper
            // for the javap evidence - an earlier version of that helper skipped players, which
            // made the flashbang do nothing at all to a creative player.
            if (!ExplosionHelper.ignoresExplosion(entity)) {
               Vec3 eyes = entity.getEyePosition(1.0F);
               Vec3 directionGrenade = grenade.subtract(eyes);
               double distance = directionGrenade.length();
               double angle = Math.toDegrees(Math.acos(entity.getViewVector(1.0F).dot(directionGrenade.normalize())));
               if (this.calculateAndApplyEffect(
                     ModEffects.DEAFENED.get(), Config.COMMON.stunGrenades.deafen.criteria, entity, grenade, eyes, distance, angle
                  )
                  && (Boolean)Config.COMMON.stunGrenades.deafen.panicMobs.get()) {
                  entity.setLastHurtByMob(entity);
               }

               if (this.calculateAndApplyEffect(
                     ModEffects.BLINDED.get(), Config.COMMON.stunGrenades.blind.criteria, entity, grenade, eyes, distance, angle
                  )
                  && (Boolean)Config.COMMON.stunGrenades.blind.blindMobs.get()
                  && entity instanceof Mob) {
                  ((Mob)entity).setTarget(null);
               }
            }
         }
      }
   }

   private boolean calculateAndApplyEffect(
      MobEffect effect, Config.EffectCriteria criteria, LivingEntity entity, Vec3 grenade, Vec3 eyes, double distance, double angle
   ) {
      double angleMax = (Double)criteria.angleEffect.get() * 0.5;
      if (distance <= (Double)criteria.radius.get()
         && angleMax > 0.0
         && angle <= angleMax
         && (
            effect != ModEffects.BLINDED.get()
               || !(Boolean)Config.COMMON.stunGrenades.blind.criteria.raytraceOpaqueBlocks.get()
               || this.rayTraceOpaqueBlocks(this.level(), eyes, grenade, false, false, false) == null
         )) {
         int durationBlinded = (int)Math.round(
            (double)((Integer)criteria.durationMax.get()).intValue()
               - (double)((Integer)criteria.durationMax.get() - (Integer)criteria.durationMin.get()) * (distance / (Double)criteria.radius.get())
         );
         durationBlinded = (int)((double)durationBlinded * (1.0 - angle * (1.0 - (Double)criteria.angleAttenuationMax.get()) / angleMax));
         entity.addEffect(new MobEffectInstance(top.ribs.scguns.util.ScEffects.holder(effect), durationBlinded, 0, false, false));
         return !(entity instanceof Player);
      } else {
         return false;
      }
   }

   @Nullable
   public HitResult rayTraceOpaqueBlocks(
      Level world, Vec3 start, Vec3 end, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock
   ) {
      if (!Double.isNaN(start.x) && !Double.isNaN(start.y) && !Double.isNaN(start.z)) {
         if (!Double.isNaN(end.x) && !Double.isNaN(end.y) && !Double.isNaN(end.z)) {
            int endX = Mth.floor(end.x);
            int endY = Mth.floor(end.y);
            int endZ = Mth.floor(end.z);
            int startX = Mth.floor(start.x);
            int startY = Mth.floor(start.y);
            int startZ = Mth.floor(start.z);
            BlockPos pos = new BlockPos(startX, startY, startZ);
            BlockState stateInside = world.getBlockState(pos);
            if (stateInside.getLightBlock(world, pos) != 0 && (!ignoreBlockWithoutBoundingBox || stateInside.getCollisionShape(world, pos) != Shapes.empty())) {
               HitResult raytraceresult = world.clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, this));
               if (raytraceresult != null) {
                  return raytraceresult;
               }
            }

            HitResult raytraceresult2 = null;
            int limit = 200;

            while (limit-- >= 0) {
               if (!Double.isNaN(start.x) && !Double.isNaN(start.y) && !Double.isNaN(start.z)) {
                  if (startX == endX && startY == endY && startZ == endZ) {
                     return returnLastUncollidableBlock ? raytraceresult2 : null;
                  }

                  boolean completedX = true;
                  boolean completedY = true;
                  boolean completedZ = true;
                  double d0 = 999.0;
                  double d1 = 999.0;
                  double d2 = 999.0;
                  if (endX > startX) {
                     d0 = (double)(startX + 1);
                  } else if (endX < startX) {
                     d0 = (double)startX;
                  } else {
                     completedX = false;
                  }

                  if (endY > startY) {
                     d1 = (double)(startY + 1);
                  } else if (endY < startY) {
                     d1 = (double)startY;
                  } else {
                     completedY = false;
                  }

                  if (endZ > startZ) {
                     d2 = (double)(startZ + 1);
                  } else if (endZ < startZ) {
                     d2 = (double)startZ;
                  } else {
                     completedZ = false;
                  }

                  double d3 = 999.0;
                  double d4 = 999.0;
                  double d5 = 999.0;
                  double d6 = end.x - start.x;
                  double d7 = end.y - start.y;
                  double d8 = end.z - start.z;
                  if (completedX) {
                     d3 = (d0 - start.x) / d6;
                  }

                  if (completedY) {
                     d4 = (d1 - start.y) / d7;
                  }

                  if (completedZ) {
                     d5 = (d2 - start.z) / d8;
                  }

                  if (d3 == 0.0) {
                     d3 = -1.0E-4;
                  }

                  if (d4 == 0.0) {
                     d4 = -1.0E-4;
                  }

                  if (d5 == 0.0) {
                     d5 = -1.0E-4;
                  }

                  Direction direction;
                  if (d3 < d4 && d3 < d5) {
                     direction = endX > startX ? Direction.WEST : Direction.EAST;
                     start = new Vec3(d0, start.y + d7 * d3, start.z + d8 * d3);
                  } else if (d4 < d5) {
                     direction = endY > startY ? Direction.DOWN : Direction.UP;
                     start = new Vec3(start.x + d6 * d4, d1, start.z + d8 * d4);
                  } else {
                     direction = endZ > startZ ? Direction.NORTH : Direction.SOUTH;
                     start = new Vec3(start.x + d6 * d5, start.y + d7 * d5, d2);
                  }

                  startX = Mth.floor(start.x) - (direction == Direction.EAST ? 1 : 0);
                  startY = Mth.floor(start.y) - (direction == Direction.UP ? 1 : 0);
                  startZ = Mth.floor(start.z) - (direction == Direction.SOUTH ? 1 : 0);
                  pos = new BlockPos(startX, startY, startZ);
                  BlockState state = world.getBlockState(pos);
                  if (state.getLightBlock(world, pos) == 0
                     || ignoreBlockWithoutBoundingBox
                        && !state.is(Blocks.NETHER_PORTAL)
                        && !state.is(Blocks.END_PORTAL)
                        && state.getCollisionShape(world, pos) == Shapes.empty()) {
                     continue;
                  }

                  return world.clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, this));
               }

               return null;
            }

            return returnLastUncollidableBlock ? raytraceresult2 : null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }
}
