package top.ribs.scguns.entity.projectile.turret;


import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.Config;
import top.ribs.scguns.init.ModItems;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.init.ModSounds;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageTurretBulletTrail;
import top.ribs.scguns.util.PhysicsStructureHelper;

public class TurretProjectileEntity extends AbstractArrow {
   @Override
   protected ItemStack getDefaultPickupItem() {
      return new ItemStack(ModItems.STANDARD_BULLET.get());
   }

   private static final float GIBBS_ROUND_XP_MULTIPLIER = 0.25F;
   private static final int GIBBS_ROUND_LOOTING_LEVEL = 4;
   private static final int SHRAPNEL_COUNT = 20;
   private static final float SHRAPNEL_RANGE = 5.0F;
   private static final float SHRAPNEL_DAMAGE_MULTIPLIER = 0.3F;
   private boolean trailSpawned = false;
   private float armorPenetration = 0.0F;
   private int mobPenetration = 0;
   private int entitiesHit = 0;
   private boolean isGibbsRound = false;
   private boolean isShatterRound = false;

   public TurretProjectileEntity(EntityType<? extends AbstractArrow> type, Level world) {
      super(type, world);
      this.setNoGravity(true);
   }

   public TurretProjectileEntity(Level world) {
      super((EntityType)ModEntities.TURRET_PROJECTILE.get(), world);
      this.setNoGravity(true);
   }

   // 0.5.5 registered this class on the game bus to add a flat looting bonus via
   // Forge's LootingLevelEvent. NeoForge 21.1 has no such event and its
   // LivingDropsEvent carries no modifiable looting level, so the handler could not
   // be ported; registering a class with no @SubscribeEvent methods aborts mod
   // loading ("class ... has no @SubscribeEvent methods, but register was called
   // anyway"), so the registration is gone. See HANDOFF.md "known deviations".



   @NotNull
   protected ItemStack getPickupItem() {
      return ItemStack.EMPTY;
   }

   public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
      super.shoot(x, y, z, velocity, inaccuracy);
      this.setDeltaMovement(this.getDeltaMovement().normalize().scale((double)velocity));
   }

   protected void onHitEntity(EntityHitResult pResult) {
      Entity entity = pResult.getEntity();
      if (entity instanceof LivingEntity livingEntity) {
         float damageAmount = (float)this.getBaseDamage();
         if (this.armorPenetration > 0.0F) {
            float originalArmor = (float)livingEntity.getArmorValue();
            float reducedArmor = Math.max(0.0F, originalArmor - this.armorPenetration);
            float armorReduction = (originalArmor - reducedArmor) / 25.0F;
            damageAmount += damageAmount * armorReduction;
         }

         boolean wasAlive = livingEntity.isAlive();
         if (livingEntity.hurt(this.damageSources().arrow(this, this.getOwner()), damageAmount) && livingEntity.isAlive()) {
            this.doPostHurtEffects(livingEntity);
         }

         if (wasAlive && !livingEntity.isAlive() && this.isGibbsRound) {
            this.spawnGibbsXPBonus(livingEntity, entity.position());
         }

         if (this.isShatterRound) {
            this.explodeShrapnel(pResult.getLocation());
         }

         livingEntity.setArrowCount(livingEntity.getArrowCount() - 1);
         entity.invulnerableTime = 0;
         this.entitiesHit++;
         if (this.mobPenetration > 0 && this.entitiesHit <= this.mobPenetration) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.8));
            return;
         }
      }

      this.discard();
   }

   private void spawnGibbsXPBonus(LivingEntity killedEntity, Vec3 position) {
      if (!this.level().isClientSide) {
         int baseXP = killedEntity.getExperienceReward((net.minecraft.server.level.ServerLevel) this.level(), null);
         int gibbsXP = Math.round((float)baseXP * 0.25F);
         if (gibbsXP > 0) {
            ExperienceOrb xpOrb = new ExperienceOrb(this.level(), position.x, position.y, position.z, gibbsXP);
            this.level().addFreshEntity(xpOrb);
         }
      }
   }

   public void setEnchantmentEffectsFromEntity(LivingEntity pShooter, float pVelocity) {
   }

   public boolean isCritArrow() {
      return false;
   }

   protected void onHitBlock(BlockHitResult result) {
      super.onHitBlock(result);
      // A shell that entered a structure can arrive with its motion spent, so the hit face stands
      // in for the flight direction when the motion is degenerate (see ProjectileEntity#onHit).
      Vec3 pushDirection = this.getDeltaMovement();
      if (pushDirection.lengthSqr() < 1.0E-8) {
         pushDirection = Vec3.atLowerCornerOf(result.getDirection().getNormal()).scale(-1.0);
      }
      this.pushPhysicsStructure(result.getLocation(), pushDirection);
      if (this.isShatterRound) {
         this.explodeShrapnel(result.getLocation());
      }

      this.discard();
   }

   /**
    * Turret shells push a physics structure the same way gun projectiles do - see
    * ProjectileEntity#pushPhysicsStructure. Their damage is lower than a rifle's, so they nudge
    * rather than shove.
    *
    * <p>A turret has no shooter to stand anywhere, so the force is applied at the impact point
    * (the helper's fallback) rather than at a player's position.</p>
    */
   private void pushPhysicsStructure(Vec3 hitVec, Vec3 direction) {
      if (this.level().isClientSide || !ScorchedGuns.physicsStructuresLoaded) {
         return;
      }

      double punchesPerTenDamage = (double)Config.COMMON.gameplay.physicsStructureImpulse.get();
      if (punchesPerTenDamage <= 0.0) {
         return;
      }

      Entity owner = this.getOwner();
      Vec3 forcePoint = owner == null ? null : owner.position();

      try {
         PhysicsStructureHelper.applyShotImpulse(this.level(), hitVec, forcePoint,
            direction, punchesPerTenDamage * this.getBaseDamage() / 10.0);
      } catch (RuntimeException e) {
         // A physics-mod reaction must never take gunfire down with it.
         ScorchedGuns.LOGGER.debug("Physics structure impulse failed", e);
      }
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide && !this.trailSpawned && this.tickCount == 1) {
         this.spawnBulletTrail();
         this.trailSpawned = true;
      }

      if (this.inGround || this.tickCount > 100) {
         this.discard();
      }
   }

   private void spawnBulletTrail() {
      Vec3 position = this.position();
      Vec3 motion = this.getDeltaMovement();
      int trailColor = 16737792;
      double trailLength = 1.0;
      int maxAge = 100;
      double trailThickness = 0.8;
      S2CMessageTurretBulletTrail message = new S2CMessageTurretBulletTrail(this.getId(), position, motion, trailColor, trailLength, maxAge, trailThickness);
      PacketHandler.getPlayChannel().sendToTrackingEntity(() -> this, message);
   }

   protected void onHit(HitResult hitResult) {
      super.onHit(hitResult);
      this.discard();
   }

   @NotNull
   protected SoundEvent getDefaultHitGroundSoundEvent() {
      return (SoundEvent)ModSounds.BULLET_FLYBY.get();
   }

   public void addAdditionalSaveData(CompoundTag compound) {
      super.addAdditionalSaveData(compound);
      compound.putDouble("TurretDamage", this.getBaseDamage());
      compound.putBoolean("TrailSpawned", this.trailSpawned);
      compound.putFloat("ArmorPenetration", this.armorPenetration);
      compound.putInt("MobPenetration", this.mobPenetration);
      compound.putInt("EntitiesHit", this.entitiesHit);
      compound.putBoolean("IsGibbsRound", this.isGibbsRound);
      compound.putBoolean("IsShatterRound", this.isShatterRound);
   }

   public void readAdditionalSaveData(CompoundTag compound) {
      super.readAdditionalSaveData(compound);
      if (compound.contains("TurretDamage")) {
         this.setBaseDamage(compound.getDouble("TurretDamage"));
      }

      this.trailSpawned = compound.getBoolean("TrailSpawned");
      if (compound.contains("ArmorPenetration")) {
         this.armorPenetration = compound.getFloat("ArmorPenetration");
      }

      if (compound.contains("MobPenetration")) {
         this.mobPenetration = compound.getInt("MobPenetration");
      }

      if (compound.contains("EntitiesHit")) {
         this.entitiesHit = compound.getInt("EntitiesHit");
      }

      if (compound.contains("IsGibbsRound")) {
         this.isGibbsRound = compound.getBoolean("IsGibbsRound");
      }

      if (compound.contains("IsShatterRound")) {
         this.isShatterRound = compound.getBoolean("IsShatterRound");
      }
   }

   public void playSound(SoundEvent soundEvent, float volume, float pitch) {
   }



   public void handleInsidePortal(BlockPos pos) {
      this.discard();
   }

   private void explodeShrapnel(Vec3 explosionPos) {
      if (!this.level().isClientSide) {
         this.createShrapnelExplosionEffects(explosionPos);
         this.fireShrapnel(explosionPos);
      }
   }

   private void createShrapnelExplosionEffects(Vec3 pos) {
      ServerLevel serverLevel = (ServerLevel)this.level();
      this.level()
         .playSound(null, pos.x, pos.y, pos.z, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 2.0F, 0.8F + this.random.nextFloat() * 0.4F);
      this.level()
         .playSound(null, pos.x, pos.y, pos.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 1.5F, 1.2F + this.random.nextFloat() * 0.3F);

      for (int i = 0; i < 40; i++) {
         double angle = this.random.nextDouble() * Math.PI * 2.0;
         double pitch = (this.random.nextDouble() - 0.5) * Math.PI * 0.5;
         double speed = 0.3 + this.random.nextDouble() * 0.4;
         double offsetX = Math.cos(angle) * Math.cos(pitch) * speed;
         double offsetY = Math.sin(pitch) * speed;
         double offsetZ = Math.sin(angle) * Math.cos(pitch) * speed;
         serverLevel.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 1, offsetX, offsetY, offsetZ, 0.02);
      }

      serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 1, 0.1, 0.1, 0.1, 0.0);
   }

   private void fireShrapnel(Vec3 origin) {
      float shrapnelDamage = (float)this.getBaseDamage() * 0.3F;

      for (int i = 0; i < 20; i++) {
         Vec3 direction = this.generateRandomDirection();
         Vec3 endPos = origin.add(direction.scale(5.0));
         this.traceShrapnelRay(origin, endPos, shrapnelDamage);
      }
   }

   private Vec3 generateRandomDirection() {
      float x;
      float y;
      float z;
      float lengthSquared;
      do {
         x = this.random.nextFloat() * 2.0F - 1.0F;
         y = this.random.nextFloat() * 2.0F - 1.0F;
         z = this.random.nextFloat() * 2.0F - 1.0F;
         lengthSquared = x * x + y * y + z * z;
      } while (lengthSquared > 1.0F || lengthSquared < 0.001F);

      float length = Mth.sqrt(lengthSquared);
      return new Vec3((double)(x / length), (double)(y / length), (double)(z / length));
   }

   private void traceShrapnelRay(Vec3 start, Vec3 end, float damage) {
      AABB searchBox = new AABB(start, end).inflate(1.0);
      Entity owner = this.getOwner();
      List<Entity> hitEntities = this.level()
         .getEntities(
            this,
            searchBox,
            entity -> entity != null
                  && entity.isPickable()
                  && !entity.isSpectator()
                  && (owner == null || entity != owner)
                  && (owner == null || entity.getId() != owner.getId())
         );
      Vec3 traceEnd = end;
      Entity closestEntity = null;
      double closestDistance = Double.MAX_VALUE;
      Vec3 closestHitPos = null;

      for (Entity entity : hitEntities) {
         AABB boundingBox = entity.getBoundingBox();
         Optional<Vec3> hitPos = boundingBox.clip(start, end);
         if (hitPos.isPresent()) {
            double distance = start.distanceToSqr(hitPos.get());
            if (distance < closestDistance) {
               closestDistance = distance;
               closestEntity = entity;
               closestHitPos = hitPos.get();
            }
         }
      }

      if (closestEntity != null) {
         closestEntity.hurt(this.damageSources().arrow(this, owner), damage);
         closestEntity.invulnerableTime = Math.min(closestEntity.invulnerableTime, 3);
         traceEnd = closestHitPos;
      } else {
         ClipContext clipContext = new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, this);
         BlockHitResult blockHit = this.level().clip(clipContext);
         if (blockHit.getType() != Type.MISS) {
            traceEnd = blockHit.getLocation();
         }
      }

      this.createShrapnelTracer(start, traceEnd);
   }

   private void createShrapnelTracer(Vec3 start, Vec3 end) {
      ServerLevel serverLevel = (ServerLevel)this.level();
      Vec3 direction = end.subtract(start);
      double distance = direction.length();
      if (!(distance < 0.1)) {
         direction = direction.normalize();
         int maxSegments = Math.min(12, (int)(distance * 1.5));

         for (int i = 1; i <= maxSegments; i++) {
            double progress = (double)i / (double)maxSegments;
            Vec3 particlePos = start.add(direction.scale(distance * progress));
            double densityFactor = Math.max(0.2, 1.0 - progress * 0.8);
            int particlesAtThisPoint = Math.max(1, (int)(4.0 * densityFactor));
            double spreadRadius = 0.02 + progress * 0.1;

            for (int j = 0; j < particlesAtThisPoint; j++) {
               double offsetX = (this.random.nextDouble() - 0.5) * spreadRadius;
               double offsetY = (this.random.nextDouble() - 0.5) * spreadRadius;
               double offsetZ = (this.random.nextDouble() - 0.5) * spreadRadius;
               serverLevel.sendParticles(
                  ParticleTypes.CRIT,
                  particlePos.x + offsetX,
                  particlePos.y + offsetY,
                  particlePos.z + offsetZ,
                  1,
                  0.0,
                  0.0,
                  0.0,
                  0.0
               );
            }
         }
      }
   }

   public float getArmorPenetration() {
      return this.armorPenetration;
   }

   public void setArmorPenetration(float armorPenetration) {
      this.armorPenetration = armorPenetration;
   }

   public int getMobPenetration() {
      return this.mobPenetration;
   }

   public void setMobPenetration(int mobPenetration) {
      this.mobPenetration = mobPenetration;
   }

   public boolean isGibbsRound() {
      return this.isGibbsRound;
   }

   public void setGibbsRound(boolean isGibbsRound) {
      this.isGibbsRound = isGibbsRound;
   }

   public boolean isShatterRound() {
      return this.isShatterRound;
   }

   public void setShatterRound(boolean isShatterRound) {
      this.isShatterRound = isShatterRound;
   }
}
