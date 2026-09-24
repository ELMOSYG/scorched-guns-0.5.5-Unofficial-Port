package top.ribs.scguns.entity.projectile;







import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import top.ribs.scguns.util.ScEnchants;
import top.ribs.scguns.util.NbtHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import com.mrcrayfish.framework.api.network.LevelLocation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.TargetBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import top.ribs.scguns.Config;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.attributes.SCAttributes;
import top.ribs.scguns.block.NitroKegBlock;
import top.ribs.scguns.block.PowderKegBlock;
import top.ribs.scguns.cache.HotBarrelCache;
import top.ribs.scguns.common.BoundingBoxManager;
import top.ribs.scguns.common.ChargeHandler;
import top.ribs.scguns.common.FireMode;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.common.SpreadTracker;
import top.ribs.scguns.config.ProjectileAdvantageConfig;
import top.ribs.scguns.effect.RocketExplosion;
import top.ribs.scguns.event.GunProjectileHitEvent;
import top.ribs.scguns.init.ModBlocks;
import top.ribs.scguns.init.ModDamageTypes;
import top.ribs.scguns.init.ModEnchantments;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.interfaces.IDamageable;
import top.ribs.scguns.interfaces.IExplosionDamageable;
import top.ribs.scguns.interfaces.IHeadshotBox;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.animated.AnimatedDiamondSteelAirGunItem;
import top.ribs.scguns.item.animated.AnimatedDiamondSteelGunItem;
import top.ribs.scguns.item.animated.AnimatedDiamondSteelUnderWaterGunItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.S2CMessageBlood;
import top.ribs.scguns.network.message.S2CMessageProjectileHitBlock;
import top.ribs.scguns.network.message.S2CMessageProjectileHitEntity;
import top.ribs.scguns.network.message.S2CMessageRemoveProjectile;
import top.ribs.scguns.util.BufferUtil;
import top.ribs.scguns.util.GunEnchantmentHelper;
import top.ribs.scguns.util.GunModifierHelper;
import top.ribs.scguns.util.ReflectionUtil;
import top.ribs.scguns.util.PhysicsStructureHelper;
import top.ribs.scguns.util.math.ExtendedEntityRayTraceResult;
import top.ribs.scguns.world.ProjectileExplosion;

public class ProjectileEntity extends Entity implements IEntityWithComplexSpawn {
   static final Predicate<Entity> PROJECTILE_TARGETS = input -> input != null && input.isPickable() && !input.isSpectator();
   public static final Predicate<BlockState> IGNORE_LEAVES = input -> input != null
         && (Boolean)Config.COMMON.gameplay.ignoreLeaves.get()
         && input.getBlock() instanceof LeavesBlock;
   protected int shooterId;
   protected LivingEntity shooter;
   protected Gun modifiedGun;
   protected Gun.General general;
   protected Gun.Projectile projectile;
   private ItemStack weapon = ItemStack.EMPTY;
   private ItemStack item = ItemStack.EMPTY;
   protected float additionalDamage = 0.0F;
   protected float attributeAdditionalDamage = 0.0F;
   protected double attributeDamageMultiplier = 0.0;
   protected EntityDimensions entitySize;
   protected double modifiedGravity;
   protected int life;
   private int soundTime = 0;
   private float chargeProgress;
   protected float armorBypassAmount = 2.0F;
   private float modifiedKnockback;
   private double distanceTraveled = 0.0;

   public ProjectileEntity(EntityType<? extends Entity> entityType, Level worldIn) {
      super(entityType, worldIn);
   }

   public ProjectileEntity(EntityType<? extends Entity> entityType, Level worldIn, LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun) {
      this(entityType, worldIn);
      this.shooterId = shooter.getId();
      this.shooter = shooter;
      this.modifiedGun = modifiedGun;
      this.general = modifiedGun.getGeneral();
      this.projectile = modifiedGun.getProjectile(weapon);
      if (shooter instanceof ServerPlayer player) {
         this.chargeProgress = player.getPersistentData().getFloat("ChargeProgress");
      } else if (shooter instanceof Player player) {
         this.chargeProgress = ChargeHandler.getChargeProgress(player, weapon);
      } else {
         this.chargeProgress = 0.0F;
      }

      if (shooter instanceof Player player) {
         ChargeHandler.clearLastChargeProgress(player.getUUID());
      }

      float baseArmorBypass = this.projectile.getArmorPen();
      float puncturingBypass = GunEnchantmentHelper.getPuncturingArmorBypass(weapon);
      this.setArmorBypassAmount(baseArmorBypass + puncturingBypass);
      float baseKnockback = this.projectile.getKnockbackStrength();
      this.modifiedKnockback = GunEnchantmentHelper.getHeavyShotKnockback(weapon, baseKnockback);
      AttributeInstance additionalDamageAttr = shooter.getAttribute(SCAttributes.ADDITIONAL_BULLET_DAMAGE);
      this.attributeAdditionalDamage = additionalDamageAttr != null ? (float)additionalDamageAttr.getValue() : 0.0F;
      AttributeInstance damageMultAttr = shooter.getAttribute(SCAttributes.BULLET_DAMAGE_MULTIPLIER);
      this.attributeDamageMultiplier = damageMultAttr != null ? damageMultAttr.getValue() : 1.0;
      this.entitySize = EntityDimensions.scalable(this.projectile.getSize(), this.projectile.getSize());
      this.modifiedGravity = modifiedGun.getProjectile().isGravity() ? GunModifierHelper.getModifiedProjectileGravity(weapon, -0.04) : 0.0;
      this.life = GunModifierHelper.getModifiedProjectileLife(weapon, this.projectile.getLife());
      Vec3 dir = this.getDirection(shooter, weapon, item, modifiedGun);
      double speedModifier = GunEnchantmentHelper.getProjectileSpeedModifier(weapon);
      double speed = GunModifierHelper.getModifiedProjectileSpeed(weapon, this.projectile.getSpeed() * speedModifier);
      if (modifiedGun.getGeneral().getFireMode() == FireMode.PULSE) {
         float chargeSpeedMultiplier = this.calculateChargeSpeedMultiplier(this.chargeProgress);
         speed *= (double)chargeSpeedMultiplier;
      }

      AttributeInstance speedAttr = shooter.getAttribute(SCAttributes.PROJECTILE_SPEED);
      speed *= speedAttr != null ? speedAttr.getValue() : 1.0;
      this.setDeltaMovement(dir.x * speed, dir.y * speed, dir.z * speed);
      this.updateHeading();
      double posX = shooter.xOld + (shooter.getX() - shooter.xOld) / 2.0;
      double posY = shooter.yOld + (shooter.getY() - shooter.yOld) / 2.0 + (double)shooter.getEyeHeight();
      double posZ = shooter.zOld + (shooter.getZ() - shooter.zOld) / 2.0;
      this.setPos(posX, posY, posZ);
      Item ammo = this.projectile.getItem();
      if (ammo != null) {
         int customModelData = -1;
         if (NbtHelper.getTag(weapon) != null && NbtHelper.getTag(weapon).contains("Model", 10)) {
            ItemStack model = top.ribs.scguns.util.NbtHelper.itemFromTag(NbtHelper.getTag(weapon).getCompound("Model"));
            if (NbtHelper.getTag(model) != null && NbtHelper.getTag(model).contains("CustomModelData")) {
               customModelData = NbtHelper.getTag(model).getInt("CustomModelData");
            }
         }

         ItemStack ammoStack = new ItemStack(ammo);
         if (customModelData != -1) {
            NbtHelper.getOrCreateTag(ammoStack).putInt("CustomModelData", customModelData);
         }

         this.item = ammoStack;
      }
   }

   public float getModifiedKnockback() {
      return this.modifiedKnockback;
   }

   private float calculateChargeSpeedMultiplier(float chargeProgress) {
      chargeProgress = Mth.clamp(chargeProgress, 0.0F, 1.0F);
      float minChargeSpeedMultiplier = 0.4F;
      float maxChargeSpeedMultiplier = 1.0F;
      return minChargeSpeedMultiplier + (maxChargeSpeedMultiplier - minChargeSpeedMultiplier) * chargeProgress;
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
   }

   public void setArmorBypassAmount(float amount) {
      this.armorBypassAmount = amount;
   }

   protected float calculateArmorBypassDamage(LivingEntity target, float damage) {
      int armorValue = target.getArmorValue();
      float baseReduction = Math.min(0.75F, (float)armorValue * 0.004F);
      if (this.armorBypassAmount <= 0.0F) {
         return damage * (1.0F - baseReduction);
      } else {
         float bypassPercent = this.armorBypassAmount / 10.0F;
         float effectiveArmor = (float)armorValue * (1.0F - bypassPercent);
         float finalReduction = Math.min(0.75F, effectiveArmor * 0.004F);
         return damage * (1.0F - finalReduction);
      }
   }

   public float getDamage() {
      float damage = this.getaFloat();
      damage = GunModifierHelper.getModifiedDamage(this.weapon, this.modifiedGun, damage);
      damage = GunEnchantmentHelper.getAcceleratorDamage(this.weapon, damage);
      damage = GunEnchantmentHelper.getHeavyShotDamage(this.weapon, damage);
      if (this.shooter instanceof Player player) {
         damage = GunEnchantmentHelper.getHotBarrelDamage(player, this.weapon, damage);
      }

      damage = GunEnchantmentHelper.getChargeDamage(this.weapon, damage, this.chargeProgress);
      damage *= ((Double)Config.COMMON.gameplay.globalDamageMultiplier.get()).floatValue();
      if (this.getPersistentData().contains("AIDamageScale")) {
         float scale = this.getPersistentData().getFloat("AIDamageScale");
         damage *= scale;
      }

      return Math.max(0.0F, damage);
   }

   public EntityDimensions getDimensions(Pose pose) {
      return this.entitySize;
   }

   private Vec3 getDirection(LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun) {
      float baseSpread = modifiedGun.getProjectile().getSpread();
      float gunSpread;
      if (shooter instanceof Player player) {
         gunSpread = GunModifierHelper.getModifiedSpread(player, weapon, baseSpread);
      } else {
         gunSpread = GunModifierHelper.getModifiedSpread(weapon, baseSpread);
      }

      if (modifiedGun.getGeneral().getFireMode() == FireMode.PULSE) {
         float chargeSpreadMultiplier = this.calculateChargeSpreadMultiplier(this.chargeProgress);
         gunSpread *= chargeSpreadMultiplier;
      }

      if (gunSpread == 0.0F) {
         return getVectorFromRotation(shooter.getXRot(), shooter.getYRot());
      } else {
         if (shooter instanceof Player player) {
            if (!modifiedGun.getProjectile().isAlwaysSpread()) {
               float spreadTrackerMultiplier = SpreadTracker.get(player).getSpread(item);
               gunSpread *= spreadTrackerMultiplier;
            }

            if ((Boolean)ModSyncedDataKeys.AIMING.getValue(player)) {
               gunSpread *= 0.7F;
            }
         }

         AttributeInstance spreadAttr = shooter.getAttribute(SCAttributes.SPREAD_MULTIPLIER);
         float attrMultiplier = spreadAttr != null ? (float)spreadAttr.getValue() : 1.0F;
         gunSpread *= attrMultiplier;
         float spreadRadians = gunSpread * (float) (Math.PI / 180.0);
         float angleY = this.random.nextFloat() * 2.0F * (float) Math.PI;
         float angleX = this.random.nextFloat() * spreadRadians;
         Vec3 forward = getVectorFromRotation(shooter.getXRot(), shooter.getYRot());
         Vec3 right;
         if (Math.abs(forward.y) < 0.999) {
            right = new Vec3(0.0, 1.0, 0.0).cross(forward).normalize();
         } else {
            right = new Vec3(1.0, 0.0, 0.0);
         }

         Vec3 up = forward.cross(right).normalize();
         Vec3 spreadVector = this.rotateVector(forward, right, angleX);
         spreadVector = this.rotateVector(spreadVector, forward, angleY);
         return spreadVector.normalize();
      }
   }

   private float calculateChargeSpreadMultiplier(float chargeProgress) {
      chargeProgress = Mth.clamp(chargeProgress, 0.0F, 1.0F);
      float minChargeSpreadMultiplier = 2.0F;
      float maxChargeSpreadMultiplier = 0.3F;
      return minChargeSpreadMultiplier - (minChargeSpreadMultiplier - maxChargeSpreadMultiplier) * chargeProgress * chargeProgress;
   }

   private Vec3 rotateVector(Vec3 vector, Vec3 axis, float angle) {
      float sin = Mth.sin(angle);
      float cos = Mth.cos(angle);
      float dot = (float)vector.dot(axis);
      return new Vec3(
         vector.x * (double)cos
            + (axis.y * vector.z - axis.z * vector.y) * (double)sin
            + axis.x * (double)dot * (double)(1.0F - cos),
         vector.y * (double)cos
            + (axis.z * vector.x - axis.x * vector.z) * (double)sin
            + axis.y * (double)dot * (double)(1.0F - cos),
         vector.z * (double)cos
            + (axis.x * vector.y - axis.y * vector.x) * (double)sin
            + axis.z * (double)dot * (double)(1.0F - cos)
      );
   }

   public float getaFloat() {
      float initialDamage = this.projectile.getDamage() + this.additionalDamage + this.attributeAdditionalDamage;
      initialDamage *= (float)this.attributeDamageMultiplier;
      if (this.projectile.isDamageReduceOverLife()) {
         float modifier = ((float)this.projectile.getLife() - (float)(this.tickCount - 1)) / (float)this.projectile.getLife();
         initialDamage *= modifier;
      }

      if (this.projectile.getDamageFalloffStart() > 0.0F && this.projectile.getDamageFalloffEnd() > this.projectile.getDamageFalloffStart()) {
         float falloffStart = this.projectile.getDamageFalloffStart();
         float falloffEnd = this.projectile.getDamageFalloffEnd();
         float minMultiplier = this.projectile.getDamageFalloffMinMultiplier();
         if (!this.weapon.isEmpty()) {
            falloffStart = GunModifierHelper.getModifiedDamageFalloffStart(this.weapon, falloffStart);
            falloffEnd = GunModifierHelper.getModifiedDamageFalloffEnd(this.weapon, falloffEnd);
         }

         if (this.distanceTraveled > (double)falloffStart) {
            if (this.distanceTraveled >= (double)falloffEnd) {
               initialDamage *= minMultiplier;
            } else {
               float falloffProgress = (float)((this.distanceTraveled - (double)falloffStart) / (double)(falloffEnd - falloffStart));
               float damageMultiplier = 1.0F - falloffProgress * (1.0F - minMultiplier);
               initialDamage *= damageMultiplier;
            }
         }
      }

      return initialDamage / (float)this.projectile.getProjectileAmount();
   }

   public void setWeapon(ItemStack weapon) {
      this.weapon = weapon.copy();
   }

   public ItemStack getWeapon() {
      return this.weapon;
   }

   public void setItem(ItemStack item) {
      this.item = item;
   }

   public ItemStack getItem() {
      return this.item;
   }

   public void setAdditionalDamage(float additionalDamage) {
      this.additionalDamage = additionalDamage;
   }

   public double getModifiedGravity() {
      return this.modifiedGravity;
   }

   public void tick() {
      super.tick();
      this.updateHeading();
      this.onProjectileTick();
      if (this.tickCount > 0) {
         Vec3 deltaMovement = this.getDeltaMovement();
         this.distanceTraveled = this.distanceTraveled + deltaMovement.length();
      }

      if (!this.level().isClientSide()) {
         Vec3 startVec = this.position();
         Vec3 endVec = startVec.add(this.getDeltaMovement());
         BlockHitResult fluidResult = this.level().clip(new ClipContext(startVec, endVec, Block.COLLIDER, Fluid.ANY, this));
         AABB range = new AABB(
            startVec.x - 5.0,
            startVec.y - 5.0,
            startVec.z - 5.0,
            startVec.x + 5.0,
            startVec.y + 5.0,
            startVec.z + 5.0
         );
         List<Player> players = this.level().getEntitiesOfClass(Player.class, range);
         ResourceLocation flybySound = this.modifiedGun.getSounds().getFlybySound();
         if (!players.isEmpty()
            && flybySound != null
            && this.modifiedGun.getProjectile().getProjectileAmount() == 1
            && this.tickCount > 3
            && this.soundTime < this.tickCount - 3) {
            this.level()
               .playSound(
                  null,
                  startVec.x,
                  startVec.y,
                  startVec.z,
                  Objects.requireNonNull((SoundEvent)BuiltInRegistries.SOUND_EVENT.get(flybySound)),
                  SoundSource.NEUTRAL,
                  0.5F + this.level().getRandom().nextFloat() * 0.4F,
                  0.8F + this.level().getRandom().nextFloat() * 0.4F
               );
            this.soundTime = this.tickCount;
         }

         if (fluidResult.getType() == Type.BLOCK) {
            BlockPos blockPos = fluidResult.getBlockPos();
            BlockState blockState = this.level().getBlockState(blockPos);
            FluidState fluidState = blockState.getFluidState();
            if (fluidState.is(FluidTags.WATER)) {
               this.onWaterImpact(fluidResult.getLocation());
            } else if (fluidState.is(FluidTags.LAVA)) {
               this.onLavaImpact(fluidResult.getLocation());
            }
         }

         HitResult result = rayTraceBlocks(this.level(), new ClipContext(startVec, endVec, Block.COLLIDER, Fluid.NONE, this), IGNORE_LEAVES);
         if (result.getType() != Type.MISS) {
            endVec = result.getLocation();
         }

         List<ProjectileEntity.EntityResult> hitEntities = null;
         int level = ScEnchants.level(this.weapon, ModEnchantments.COLLATERAL);
         if (level == 0) {
            ProjectileEntity.EntityResult entityResult = this.findEntityOnPath(startVec, endVec);
            if (entityResult != null) {
               hitEntities = Collections.singletonList(entityResult);
            }
         } else {
            hitEntities = this.findEntitiesOnPath(startVec, endVec);
         }

         if (hitEntities != null && !hitEntities.isEmpty()) {
            for (ProjectileEntity.EntityResult entityResult : hitEntities) {
               ExtendedEntityRayTraceResult var19 = new ExtendedEntityRayTraceResult(entityResult);
               if (var19.getEntity() instanceof Player player && this.shooter instanceof Player && !((Player)this.shooter).canHarmPlayer(player)) {
                  var19 = null;
               }

               if (var19 != null) {
                  this.onHit(var19, startVec, endVec);
               }
            }
         } else {
            this.onHit(result, startVec, endVec);
         }
      }

      double nextPosX = this.getX() + this.getDeltaMovement().x();
      double nextPosY = this.getY() + this.getDeltaMovement().y();
      double nextPosZ = this.getZ() + this.getDeltaMovement().z();
      this.setPos(nextPosX, nextPosY, nextPosZ);
      if (this.projectile.isGravity()) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, this.modifiedGravity, 0.0));
      }

      if (this.tickCount >= this.life) {
         if (this.isAlive()) {
            this.onExpired();
         }

         this.remove(RemovalReason.KILLED);
      }
   }

   protected void onProjectileTick() {
   }

   protected void onExpired() {
   }

   @Nullable
   protected ProjectileEntity.EntityResult findEntityOnPath(Vec3 startVec, Vec3 endVec) {
      Vec3 hitVec = null;
      Entity hitEntity = null;
      boolean headshot = false;
      List<Entity> entities = this.level().getEntities(this, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0), PROJECTILE_TARGETS);
      double closestDistance = Double.MAX_VALUE;

      for (Entity entity : entities) {
         if (!entity.equals(this.shooter)) {
            ProjectileEntity.EntityResult result = this.getHitResult(entity, startVec, endVec);
            if (result != null) {
               Vec3 hitPos = result.getHitPos();
               double distanceToHit = startVec.distanceTo(hitPos);
               if (distanceToHit < closestDistance) {
                  hitVec = hitPos;
                  hitEntity = entity;
                  closestDistance = distanceToHit;
                  headshot = result.isHeadshot();
               }
            }
         }
      }

      return hitEntity != null ? new ProjectileEntity.EntityResult(hitEntity, hitVec, headshot) : null;
   }

   protected List<ProjectileEntity.EntityResult> findEntitiesOnPath(Vec3 startVec, Vec3 endVec) {
      List<ProjectileEntity.EntityResult> hitEntities = new ArrayList<>();

      for (Entity entity : this.level().getEntities(this, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0), PROJECTILE_TARGETS)) {
         ProjectileEntity.EntityResult result = this.getHitResult(entity, startVec, endVec);
         if (result != null) {
            hitEntities.add(result);
         }
      }

      return hitEntities;
   }

   @Nullable
   public ProjectileEntity.EntityResult getHitResult(Entity entity, Vec3 startVec, Vec3 endVec) {
      double expandHeight = entity instanceof Player && !entity.isCrouching() ? 0.0625 : 0.0;
      AABB boundingBox = entity.getBoundingBox();
      if ((Boolean)Config.COMMON.gameplay.improvedHitboxes.get() && entity instanceof ServerPlayer && this.shooter instanceof ServerPlayer) {
         int ping = (int)Math.floor((double)((ServerPlayer)this.shooter).connection.latency() / 1000.0 * 20.0 + 0.5);
         boundingBox = BoundingBoxManager.getBoundingBox((Player)entity, ping);
      }

      boundingBox = boundingBox.expandTowards(0.0, expandHeight, 0.0);
      Vec3 hitPos = (Vec3)boundingBox.clip(startVec, endVec).orElse(null);
      Vec3 grownHitPos = (Vec3)boundingBox.inflate(
            (Double)Config.COMMON.gameplay.growBoundingBoxAmount.get(), 0.0, (Double)Config.COMMON.gameplay.growBoundingBoxAmount.get()
         )
         .clip(startVec, endVec)
         .orElse(null);
      if (hitPos == null && grownHitPos != null) {
         HitResult raytraceresult = rayTraceBlocks(this.level(), new ClipContext(startVec, grownHitPos, Block.COLLIDER, Fluid.NONE, this), IGNORE_LEAVES);
         if (raytraceresult.getType() == Type.BLOCK) {
            return null;
         }

         hitPos = grownHitPos;
      }

      boolean headshot = false;
      if ((Boolean)Config.COMMON.gameplay.enableHeadShots.get() && entity instanceof LivingEntity) {
         IHeadshotBox<LivingEntity> headshotBox = BoundingBoxManager.getHeadshotBoxes(entity.getType());
         if (headshotBox != null) {
            AABB box = headshotBox.getHeadshotBox((LivingEntity)entity);
            if (box != null) {
               box = box.move(boundingBox.getCenter().x, boundingBox.minY, boundingBox.getCenter().z);
               Optional<Vec3> headshotHitPos = box.clip(startVec, endVec);
               if (!headshotHitPos.isPresent()) {
                  box = box.inflate(
                     (Double)Config.COMMON.gameplay.growBoundingBoxAmount.get(), 0.0, (Double)Config.COMMON.gameplay.growBoundingBoxAmount.get()
                  );
                  headshotHitPos = box.clip(startVec, endVec);
               }

               if (headshotHitPos.isPresent() && (hitPos == null || headshotHitPos.get().distanceTo(hitPos) < 0.5)) {
                  hitPos = headshotHitPos.get();
                  headshot = true;
               }
            }
         }
      }

      return hitPos == null ? null : new ProjectileEntity.EntityResult(entity, hitPos, headshot);
   }

   /**
    * A physics structure (Sable sub-level) is not part of the main level's block data, so a shot
    * has no effect on it unless we ask the physics body to react.
    *
    * <p>The push is measured in player punches (config {@code physicsStructureImpulse}): the
    * projectile's damage over the 10-damage reference gives how many punches this round is worth,
    * and the helper applies Sable's own punch force. Sable applies a punch at the <b>player's own
    * position</b> (not at the block that was hit), so the shooter's position is passed along as the
    * point where the force lands; a shot therefore shoves a structure the way a punch from where
    * the shooter stands would. The helper caps the resulting push and spin so no burst of fire can
    * launch a contraption away.</p>
    */
   private void pushPhysicsStructure(Vec3 hitVec, Vec3 direction) {
      if (this.level().isClientSide || !ScorchedGuns.physicsStructuresLoaded) {
         return;
      }

      double punchesPerTenDamage = (Double)Config.COMMON.gameplay.physicsStructureImpulse.get();
      if (punchesPerTenDamage <= 0.0) {
         return;
      }

      // Where the force is applied: the shooter, like a punch. Null (a dispenser, a turret, or a
      // projectile that lost its shooter) falls back to the impact point in the helper.
      Vec3 forcePoint = this.shooter == null || !this.shooter.isAlive()
         ? null : this.shooter.position();

      try {
         PhysicsStructureHelper.applyShotImpulse(this.level(), hitVec, forcePoint,
            direction, punchesPerTenDamage * (double)this.getDamage() / 10.0);
      } catch (RuntimeException e) {
         // A physics-mod reaction must never take gunfire down with it.
         ScorchedGuns.LOGGER.debug("Physics structure impulse failed", e);
      }
   }

   public void onHit(HitResult result, Vec3 startVec, Vec3 endVec) {
      if (!NeoForge.EVENT_BUS.post(new GunProjectileHitEvent(result, this)).isCanceled()) {
         if (result instanceof BlockHitResult blockHitResult) {
            if (blockHitResult.getType() != Type.MISS) {
               Vec3 hitVec = result.getLocation();
               // The projectile's motion can be (almost) spent by the time the hit is reported:
               // Sable moves a projectile that entered a structure into that structure's frame,
               // and the motion does not always survive. Fall back to the face it hit, reversed,
               // which is the direction the shot was travelling - the helper falls back further to
               // the shooter's position if even that is unusable.
               Vec3 pushDirection = this.getDeltaMovement();
               if (pushDirection.lengthSqr() < 1.0E-8) {
                  pushDirection = Vec3.atLowerCornerOf(blockHitResult.getDirection().getNormal())
                     .scale(-1.0);
               }
               ScorchedGuns.LOGGER.debug("Projectile {} hit block at ({}, {}, {}) face={} motion={}",
                  this.getUUID(), hitVec.x, hitVec.y, hitVec.z, blockHitResult.getDirection(),
                  this.getDeltaMovement().length());
               this.pushPhysicsStructure(hitVec, pushDirection);
               BlockPos pos = blockHitResult.getBlockPos();
               BlockState state = this.level().getBlockState(pos);
               net.minecraft.world.level.block.Block block = state.getBlock();
               if ((Boolean)Config.COMMON.gameplay.griefing.enableGlassBreaking.get() && state.is(ModTags.Blocks.FRAGILE)) {
                  float destroySpeed = state.getDestroySpeed(this.level(), pos);
                  if (destroySpeed >= 0.0F) {
                     float chance = ((Double)Config.COMMON.gameplay.griefing.fragileBaseBreakChance.get()).floatValue() / (destroySpeed + 1.0F);
                     if (this.random.nextFloat() < chance) {
                        this.level().destroyBlock(pos, (Boolean)Config.COMMON.gameplay.griefing.fragileBlockDrops.get());
                     }
                  }
               }

               if (!state.canBeReplaced()) {
                  this.remove(RemovalReason.KILLED);
               }

               if (block instanceof IDamageable) {
                  ((IDamageable)block).onBlockDamaged(this.level(), state, pos, this, this.getDamage(), (int)Math.ceil((double)this.getDamage() / 2.0) + 1);
               }

               this.onHitBlock(state, pos, blockHitResult.getDirection(), hitVec.x, hitVec.y, hitVec.z);
               if (block instanceof TargetBlock targetBlock) {
                  int power = ReflectionUtil.updateTargetBlock(targetBlock, this.level(), state, blockHitResult, this);
                  if (this.shooter instanceof ServerPlayer serverPlayer) {
                     serverPlayer.awardStat(Stats.TARGET_HIT);
                     CriteriaTriggers.TARGET_BLOCK_HIT.trigger(serverPlayer, this, blockHitResult.getLocation(), power);
                  }
               }

               if (block instanceof BellBlock bell) {
                  bell.attemptToRing(this.level(), pos, blockHitResult.getDirection());
               }
            }
         } else {
            if (result instanceof ExtendedEntityRayTraceResult entityHitResult) {
               Entity entity = entityHitResult.getEntity();
               if (entity.getId() == this.shooterId) {
                  return;
               }

               if (this.shooter instanceof Player player) {
                  if (entity.hasIndirectPassenger(player)) {
                     return;
                  }

                  HotBarrelCache.getHotBarrelLevel(player, this.weapon);
                  boolean shouldFire = GunEnchantmentHelper.shouldSetOnFire(player, this.weapon);
                  if (shouldFire) {
                     entity.igniteForSeconds(5);
                  }
               } else if (ScEnchants.level(this.weapon, ModEnchantments.HOT_BARREL) > 0) {
                  entity.igniteForSeconds(5);
               }

               this.onHitEntity(entity, result.getLocation(), startVec, endVec, entityHitResult.isHeadshot());
               int collateralLevel = ScEnchants.level(this.weapon, ModEnchantments.COLLATERAL);
               ResourceLocation advantage = this.getProjectile().getAdvantage();
               if (!entity.getType().is(ModTags.Entities.GHOST) || !advantage.equals(ModTags.Entities.UNDEAD.location()) || collateralLevel == 0) {
                  this.remove(RemovalReason.KILLED);
               }

               entity.invulnerableTime = 0;
            }
         }
      }
   }

   protected void onLavaImpact(Vec3 impactPos) {
      if (!this.level().isClientSide() && (Boolean)Config.CLIENT.particle.enableLavaImpactParticles.get()) {
         ServerLevel serverLevel = (ServerLevel)this.level();

         for (int i = 0; i < 5; i++) {
            double ySpeed = 0.2 + this.random.nextDouble() * 0.3;
            serverLevel.sendParticles(ParticleTypes.LAVA, impactPos.x, impactPos.y, impactPos.z, 1, 0.02, 0.0, 0.02, ySpeed);
         }

         for (int i = 0; i < 3; i++) {
            double xSpeed = (this.random.nextDouble() - 0.5) * 0.1;
            double ySpeed = 0.2 + this.random.nextDouble() * 0.2;
            double zSpeed = (this.random.nextDouble() - 0.5) * 0.1;
            serverLevel.sendParticles(ParticleTypes.SMOKE, impactPos.x, impactPos.y, impactPos.z, 1, xSpeed, ySpeed, zSpeed, 0.05);
         }

         serverLevel.sendParticles(ParticleTypes.LAVA, impactPos.x, impactPos.y + 0.05, impactPos.z, 3, 0.1, 0.1, 0.1, 0.2);
      }

      this.level()
         .playSound(
            null,
            impactPos.x,
            impactPos.y,
            impactPos.z,
            SoundEvents.LAVA_POP,
            SoundSource.NEUTRAL,
            0.6F,
            1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F
         );
   }

   protected void onWaterImpact(Vec3 impactPos) {
      if (!this.level().isClientSide()) {
         boolean enableParticles = true;

         try {
            enableParticles = (Boolean)Config.CLIENT.particle.enableWaterImpactParticles.get();
         } catch (IllegalStateException var16) {
            enableParticles = true;
         }

         if (enableParticles) {
            ServerLevel serverLevel = (ServerLevel)this.level();
            boolean isSubmerged = this.isInWater();
            int splashParticles = isSubmerged ? 10 : 40;
            int bubbleParticles = isSubmerged ? 10 : 30;
            int snowflakeParticles = isSubmerged ? 5 : 15;
            int fallingWaterParticles = isSubmerged ? 5 : 20;

            for (int i = 0; i < fallingWaterParticles; i++) {
               double ySpeed = 0.5 + this.random.nextDouble() * 0.5;
               serverLevel.sendParticles(ParticleTypes.FALLING_WATER, impactPos.x, impactPos.y, impactPos.z, 1, 0.05, 0.0, 0.05, ySpeed);
            }

            for (int i = 0; i < snowflakeParticles; i++) {
               double xSpeed = (this.random.nextDouble() - 0.5) * 0.2;
               double ySpeed = 0.3 + this.random.nextDouble() * 0.3;
               double zSpeed = (this.random.nextDouble() - 0.5) * 0.2;
               serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, impactPos.x, impactPos.y, impactPos.z, 1, xSpeed, ySpeed, zSpeed, 0.1);
            }

            serverLevel.sendParticles(ParticleTypes.SPLASH, impactPos.x, impactPos.y + 0.1, impactPos.z, splashParticles, 0.2, 0.2, 0.2, 0.4);
            serverLevel.sendParticles(ParticleTypes.BUBBLE_POP, impactPos.x, impactPos.y, impactPos.z, bubbleParticles, 0.5, 0.3, 0.5, 0.2);
         }

         this.level()
            .playSound(
               null,
               impactPos.x,
               impactPos.y,
               impactPos.z,
               SoundEvents.PLAYER_SPLASH,
               SoundSource.NEUTRAL,
               1.2F,
               1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F
            );
      }
   }

   public float advantageMultiplier(Entity entity) {
      ResourceLocation advantage = this.getProjectile().getAdvantage();
      if (advantage.equals(ModTags.Entities.NONE.location())) {
         return 1.0F;
      } else {
         ProjectileAdvantageConfig.AdvantageData advantageData = ProjectileAdvantageConfig.getAdvantageData(advantage.toString());
         if (advantageData == null) {
            return 1.0F;
         } else {
            boolean hasMatchingTag = false;

            for (String targetTag : advantageData.targetTags()) {
               ResourceLocation tagLocation = ResourceLocation.parse(targetTag);
               TagKey<EntityType<?>> entityTag = TagKey.create(Registries.ENTITY_TYPE, tagLocation);
               if (entity.getType().is(entityTag)) {
                  hasMatchingTag = true;
                  break;
               }
            }

            if (hasMatchingTag) {
               if (advantageData.causesFire() && advantageData.fireDuration() > 0) {
                  entity.igniteForSeconds(advantageData.fireDuration());
               }

               return advantageData.multiplier();
            } else {
               return 1.0F;
            }
         }
      }
   }

   protected void onHitEntity(Entity entity, Vec3 hitVec, Vec3 startVec, Vec3 endVec, boolean headshot) {
      float damage = this.getDamage();
      float newDamage = this.getCriticalDamage(this.weapon, this.random, damage);
      boolean critical = damage != newDamage;
      damage = newDamage * this.advantageMultiplier(entity);
      boolean wasAlive = entity instanceof LivingEntity && entity.isAlive();
      if (this.shooter instanceof Player player) {
         damage = GunEnchantmentHelper.getWaterProofDamage(this.weapon, player, damage);
      }

      if (headshot) {
         damage = (float)((double)damage * (Double)Config.COMMON.gameplay.headShotDamageMultiplier.get());
      }

      if (entity instanceof LivingEntity livingTarget) {
         damage = GunEnchantmentHelper.getPuncturingDamageReduction(this.weapon, livingTarget, damage);
         damage = this.applyProjectileProtection(livingTarget, damage);
         damage = this.calculateArmorBypassDamage(livingTarget, damage);
      }

      DamageSource source = ModDamageTypes.Sources.projectile(this.level().registryAccess(), this, this.shooter);
      boolean blocked = ProjectileEntity.ProjectileHelper.handleShieldHit(entity, this, damage);
      if (!blocked && (!entity.getType().is(ModTags.Entities.GHOST) || this.getProjectile().getAdvantage().equals(ModTags.Entities.UNDEAD.location()))) {
         if (damage > 0.0F) {
            entity.hurt(source, damage);
         }

         if (entity instanceof LivingEntity livingEntity) {
            ResourceLocation effectLocation = this.projectile.getImpactEffect();
            if (effectLocation != null) {
               float effectChance = this.projectile.getImpactEffectChance();
               if (this.random.nextFloat() < effectChance) {
                  MobEffect effect = (MobEffect)BuiltInRegistries.MOB_EFFECT.get(effectLocation);
                  if (effect != null) {
                     livingEntity.addEffect(new MobEffectInstance(top.ribs.scguns.util.ScEffects.holder(effect), this.projectile.getImpactEffectDuration(), this.projectile.getImpactEffectAmplifier()));
                  }
               }
            }
         }
      }

      if (entity instanceof LivingEntity) {
         GunEnchantmentHelper.applyElementalPopEffect(this.weapon, (LivingEntity)entity);
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
      }

      PacketHandler.getPlayChannel().sendToTrackingEntity(() -> entity, new S2CMessageBlood(hitVec.x, hitVec.y, hitVec.z, entity.getType()));
   }

   void checkForDiamondSteelBonus(LivingEntity killedEntity, Vec3 position) {
      if (!this.level().isClientSide && this.getShooter() instanceof Player player) {
         ItemStack weapon = player.getMainHandItem();
         if (weapon.getItem() instanceof AnimatedDiamondSteelGunItem
            || weapon.getItem() instanceof AnimatedDiamondSteelAirGunItem
            || weapon.getItem() instanceof AnimatedDiamondSteelUnderWaterGunItem) {
            int baseXP = killedEntity.getExperienceReward((net.minecraft.server.level.ServerLevel) this.level(), null);
            int bonusXP = Math.round((float)baseXP * 0.2F);
            if (bonusXP > 0) {
               ExperienceOrb xpOrb = new ExperienceOrb(this.level(), position.x, position.y, position.z, bonusXP);
               this.level().addFreshEntity(xpOrb);
            }
         }
      }
   }

   public float applyProjectileProtection(LivingEntity target, float damage) {
      int protectionLevel = ScEnchants.level(target, Enchantments.PROJECTILE_PROTECTION);
      if (protectionLevel > 0) {
         float reduction = (float)protectionLevel * 0.1F;
         reduction = Math.min(reduction, 0.8F);
         damage *= 1.0F - reduction;
      }

      return damage;
   }

   protected void onHitBlock(BlockState state, BlockPos pos, Direction face, double x, double y, double z) {
      PacketHandler.getPlayChannel().sendToTrackingChunk(() -> this.level().getChunkAt(pos), new S2CMessageProjectileHitBlock(x, y, z, pos, face));
      net.minecraft.world.level.block.Block block = state.getBlock();
      if (!this.primeTNT(state, pos)) {
         if (block instanceof DoorBlock) {
            boolean isOpen = (Boolean)state.getValue(DoorBlock.OPEN);
            if (!isOpen) {
               this.level().setBlock(pos, (BlockState)state.setValue(DoorBlock.OPEN, true), 10);
               this.level().playSound(null, pos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
         }

         if (!state.canBeReplaced()) {
            this.remove(RemovalReason.KILLED);
         }

         if (block instanceof IDamageable) {
            ((IDamageable)block).onBlockDamaged(this.level(), state, pos, this, this.getDamage(), (int)Math.ceil((double)this.getDamage() / 2.0) + 1);
         }
      }
   }

   boolean primeTNT(BlockState state, BlockPos pos) {
      net.minecraft.world.level.block.Block block = state.getBlock();
      if (block == Blocks.TNT) {
         if (!this.level().isClientSide()) {
            TntBlock.explode(this.level(), pos);
            this.level().removeBlock(pos, false);
         }

         return true;
      } else if (block == ModBlocks.POWDER_KEG.get()) {
         if (!this.level().isClientSide()) {
            PowderKegBlock.explode(this.level(), pos);
            this.level().removeBlock(pos, false);
         }

         return true;
      } else if (block == ModBlocks.NITRO_KEG.get()) {
         if (!this.level().isClientSide()) {
            NitroKegBlock.explode(this.level(), pos);
            this.level().removeBlock(pos, false);
         }

         return true;
      } else {
         return true;
      }
   }

   protected void readAdditionalSaveData(CompoundTag compound) {
      this.projectile = new Gun.Projectile();
      this.projectile.deserializeNBT(compound.getCompound("Projectile"));
      this.general = new Gun.General();
      this.general.deserializeNBT(compound.getCompound("General"));
      this.modifiedGravity = compound.getDouble("ModifiedGravity");
      this.life = compound.getInt("MaxLife");
      this.distanceTraveled = compound.getDouble("DistanceTraveled");
   }

   protected void addAdditionalSaveData(CompoundTag compound) {
      compound.put("Projectile", this.projectile.serializeNBT());
      compound.put("General", this.general.serializeNBT());
      compound.putDouble("ModifiedGravity", this.modifiedGravity);
      compound.putInt("MaxLife", this.life);
      compound.putDouble("DistanceTraveled", this.distanceTraveled);
   }

   public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
      buffer.writeNbt(this.projectile.serializeNBT());
      buffer.writeNbt(this.general.serializeNBT());
      buffer.writeInt(this.shooterId);
      BufferUtil.writeItemStackToBufIgnoreTag(buffer, this.item);
      buffer.writeDouble(this.modifiedGravity);
      buffer.writeVarInt(this.life);
   }

   public void readSpawnData(RegistryFriendlyByteBuf buffer) {
      this.projectile = new Gun.Projectile();
      this.projectile.deserializeNBT(buffer.readNbt());
      this.general = new Gun.General();
      this.general.deserializeNBT(buffer.readNbt());
      this.shooterId = buffer.readInt();
      this.item = BufferUtil.readItemStackFromBufIgnoreTag(buffer);
      this.modifiedGravity = buffer.readDouble();
      this.life = buffer.readVarInt();
      this.entitySize = EntityDimensions.scalable(this.projectile.getSize(), this.projectile.getSize());
   }

   public void updateHeading() {
      double horizontalDistance = this.getDeltaMovement().horizontalDistance();
      this.setYRot((float)(Mth.atan2(this.getDeltaMovement().x(), this.getDeltaMovement().z()) * (180.0 / Math.PI)));
      this.setXRot((float)(Mth.atan2(this.getDeltaMovement().y(), horizontalDistance) * (180.0 / Math.PI)));
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
   }

   public Gun.Projectile getProjectile() {
      return this.projectile;
   }

   static Vec3 getVectorFromRotation(float pitch, float yaw) {
      float f = Mth.cos(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f1 = Mth.sin(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f2 = -Mth.cos(-pitch * (float) (Math.PI / 180.0));
      float f3 = Mth.sin(-pitch * (float) (Math.PI / 180.0));
      return new Vec3((double)(f1 * f2), (double)f3, (double)(f * f2));
   }

   public LivingEntity getShooter() {
      return this.shooter;
   }

   public int getShooterId() {
      return this.shooterId;
   }

   public float getCriticalDamage(ItemStack weapon, RandomSource rand, float damage) {
      float chance = GunModifierHelper.getCriticalChance(weapon);
      if (rand.nextFloat() < chance) {
         float critMultiplier = this.modifiedGun.getProjectile().getCritDamageMultiplier();
         return damage * critMultiplier;
      } else {
         return damage;
      }
   }

   public boolean shouldRenderAtSqrDistance(double distance) {
      return true;
   }

   public void onRemovedFromWorld() {
      if (!this.level().isClientSide) {
         PacketHandler.getPlayChannel().sendToNearbyPlayers(this::getDeathTargetPoint, new S2CMessageRemoveProjectile(this.getId()));
      }
   }

   LevelLocation getDeathTargetPoint() {
      return LevelLocation.create((ServerLevel) this.level(), this.getX(), this.getY(), this.getZ(), 256.0);
   }



   static BlockHitResult rayTraceBlocks(Level world, ClipContext context, Predicate<BlockState> ignorePredicate) {
      if (ScorchedGuns.physicsStructuresLoaded) {
         // Physics-structure mods (Sable, and Valkyrien Skies before it) replace
         // BlockGetter#clip with a version that also raycasts their moving structures and
         // then falls back to the vanilla traversal. The per-block walk below reads
         // world.getBlockState() directly, so it only ever sees the main level - which is
         // why bullets used to fly straight through sub-levels/contraptions. 0.5.5 made the
         // same substitution for Valkyrien Skies (one clipIncludeShips call instead of the
         // walk); as then, ignorePredicate (leaves) does not apply in this mode.
         return world.clip(context);
      }

      return performRayTrace(
         context,
         (rayTraceContext, blockPos) -> {
            {
               BlockState blockState = world.getBlockState(blockPos);
               if (ignorePredicate.test(blockState)) {
                  return null;
               } else {
                  FluidState fluidState = world.getFluidState(blockPos);
                  Vec3 startVec = rayTraceContext.getFrom();
                  Vec3 endVec = rayTraceContext.getTo();
                  VoxelShape blockShape = rayTraceContext.getBlockShape(blockState, world, blockPos);
                  BlockHitResult blockResult = world.clipWithInteractionOverride(startVec, endVec, blockPos, blockShape, blockState);
                  VoxelShape fluidShape = rayTraceContext.getFluidShape(fluidState, world, blockPos);
                  BlockHitResult fluidResult = fluidShape.clip(startVec, endVec, blockPos);
                  double blockDistance = blockResult == null ? Double.MAX_VALUE : rayTraceContext.getFrom().distanceToSqr(blockResult.getLocation());
                  double fluidDistance = fluidResult == null ? Double.MAX_VALUE : rayTraceContext.getFrom().distanceToSqr(fluidResult.getLocation());
                  return blockDistance <= fluidDistance ? blockResult : fluidResult;
               }
            }
         },
         rayTraceContext -> {
            Vec3 Vector3d = rayTraceContext.getFrom().subtract(rayTraceContext.getTo());
            return BlockHitResult.miss(
               rayTraceContext.getTo(),
               Direction.getNearest(Vector3d.x, Vector3d.y, Vector3d.z),
               BlockPos.containing(rayTraceContext.getTo())
            );
         }
      );
   }

   private static <T> T performRayTrace(ClipContext context, BiFunction<ClipContext, BlockPos, T> hitFunction, Function<ClipContext, T> p_217300_2_) {
      Vec3 startVec = context.getFrom();
      Vec3 endVec = context.getTo();
      if (startVec.equals(endVec)) {
         return p_217300_2_.apply(context);
      } else {
         double startX = Mth.lerp(-1.0E-7, endVec.x, startVec.x);
         double startY = Mth.lerp(-1.0E-7, endVec.y, startVec.y);
         double startZ = Mth.lerp(-1.0E-7, endVec.z, startVec.z);
         double endX = Mth.lerp(-1.0E-7, startVec.x, endVec.x);
         double endY = Mth.lerp(-1.0E-7, startVec.y, endVec.y);
         double endZ = Mth.lerp(-1.0E-7, startVec.z, endVec.z);
         int blockX = Mth.floor(endX);
         int blockY = Mth.floor(endY);
         int blockZ = Mth.floor(endZ);
         MutableBlockPos mutablePos = new MutableBlockPos(blockX, blockY, blockZ);
         T t = hitFunction.apply(context, mutablePos);
         if (t != null) {
            return t;
         } else {
            double deltaX = startX - endX;
            double deltaY = startY - endY;
            double deltaZ = startZ - endZ;
            int signX = Mth.sign(deltaX);
            int signY = Mth.sign(deltaY);
            int signZ = Mth.sign(deltaZ);
            double d9 = signX == 0 ? Double.MAX_VALUE : (double)signX / deltaX;
            double d10 = signY == 0 ? Double.MAX_VALUE : (double)signY / deltaY;
            double d11 = signZ == 0 ? Double.MAX_VALUE : (double)signZ / deltaZ;
            double d12 = d9 * (signX > 0 ? 1.0 - Mth.frac(endX) : Mth.frac(endX));
            double d13 = d10 * (signY > 0 ? 1.0 - Mth.frac(endY) : Mth.frac(endY));
            double d14 = d11 * (signZ > 0 ? 1.0 - Mth.frac(endZ) : Mth.frac(endZ));

            while (d12 <= 1.0 || d13 <= 1.0 || d14 <= 1.0) {
               if (d12 < d13) {
                  if (d12 < d14) {
                     blockX += signX;
                     d12 += d9;
                  } else {
                     blockZ += signZ;
                     d14 += d11;
                  }
               } else if (d13 < d14) {
                  blockY += signY;
                  d13 += d10;
               } else {
                  blockZ += signZ;
                  d14 += d11;
               }

               T t1 = hitFunction.apply(context, mutablePos.set(blockX, blockY, blockZ));
               if (t1 != null) {
                  return t1;
               }
            }

            return p_217300_2_.apply(context);
         }
      }
   }

   public static void createExplosion(Entity entity, float radius, boolean forceNone) {
      Level world = entity.level();
      if (!world.isClientSide()) {
         DamageSource source = entity instanceof ProjectileEntity projectile ? entity.damageSources().explosion(entity, projectile.getShooter()) : null;
         boolean allowBlockRemoval = (Boolean)Config.COMMON.gameplay.griefing.enableBlockRemovalOnExplosions.get() && !forceNone;
         if (allowBlockRemoval && entity instanceof ProjectileEntity projectilex) {
            LivingEntity shooter = projectilex.getShooter();
            if (shooter != null && !(shooter instanceof Player)) {
               allowBlockRemoval = (Boolean)Config.COMMON.gameplay.griefing.enableMobExplosionBlockRemoval.get();
            }
         }

         BlockInteraction mode = allowBlockRemoval ? BlockInteraction.DESTROY : BlockInteraction.KEEP;
         Explosion explosion = new ProjectileExplosion(
            world, entity, source, null, entity.getX(), entity.getY(), entity.getZ(), radius, false, mode
         ) {
            @Override
            protected float getEntityDamageAmount(Entity entity, double distance) {
               return 0.0F;
            }
         };
         if (!EventHooks.onExplosionStart(world, explosion)) {
            explosion.explode();
            explosion.finalizeExplosion(true);
            explosion.getToBlow().forEach(pos -> {
               if (world.getBlockState(pos).getBlock() instanceof IExplosionDamageable) {
                  ((IExplosionDamageable)world.getBlockState(pos).getBlock()).onProjectileExploded(world, world.getBlockState(pos), pos, entity);
               }
            });
            if (!explosion.interactsWithBlocks()) {
               explosion.clearToBlow();
            }

            for (ServerPlayer player : ((ServerLevel)world).players()) {
               if (player.distanceToSqr(entity.getX(), entity.getY(), entity.getZ()) < 4096.0) {
                  player.connection
                     .send(
                        new ClientboundExplodePacket(
                           entity.getX(),
                           entity.getY(),
                           entity.getZ(),
                           radius,
                           explosion.getToBlow(),
                           (Vec3)explosion.getHitPlayers().get(player),
                           explosion.getBlockInteraction(),
                           explosion.getSmallExplosionParticles(),
                           explosion.getLargeExplosionParticles(),
                           explosion.getExplosionSound()
                        )
                     );
               }
            }
         }
      }
   }

   public static void createRocketExplosion(Entity entity, float radius, float damage, boolean forceNone) {
      Level world = entity.level();
      if (!world.isClientSide()) {
         DamageSource source = entity instanceof ProjectileEntity projectile ? entity.damageSources().explosion(entity, projectile.getShooter()) : null;
         boolean allowBlockRemoval = (Boolean)Config.COMMON.gameplay.griefing.enableBlockRemovalOnExplosions.get() && !forceNone;
         if (allowBlockRemoval && entity instanceof ProjectileEntity projectilex) {
            LivingEntity shooter = projectilex.getShooter();
            if (shooter != null && !(shooter instanceof Player)) {
               allowBlockRemoval = (Boolean)Config.COMMON.gameplay.griefing.enableMobExplosionBlockRemoval.get();
            }
         }

         BlockInteraction mode = allowBlockRemoval ? BlockInteraction.DESTROY : BlockInteraction.KEEP;
         Explosion explosion = new RocketExplosion(
            world, entity, source, null, entity.getX(), entity.getY(), entity.getZ(), radius, damage, false, mode
         );
         if (!EventHooks.onExplosionStart(world, explosion)) {
            explosion.explode();
            explosion.finalizeExplosion(true);
            explosion.getToBlow().forEach(pos -> {
               if (world.getBlockState(pos).getBlock() instanceof IExplosionDamageable) {
                  ((IExplosionDamageable)world.getBlockState(pos).getBlock()).onProjectileExploded(world, world.getBlockState(pos), pos, entity);
               }
            });
            if (!explosion.interactsWithBlocks()) {
               explosion.clearToBlow();
            }
         }
      }
   }

   public static void createFireExplosion(Entity entity, float radius, boolean forceNone) {
      Level world = entity.level();
      if (!world.isClientSide()) {
         DamageSource source = entity instanceof ProjectileEntity projectile ? entity.damageSources().explosion(entity, projectile.getShooter()) : null;
         BlockInteraction mode = BlockInteraction.KEEP;
         Explosion explosion = new ProjectileExplosion(
            world, entity, source, null, entity.getX(), entity.getY(), entity.getZ(), radius * 0.5F, true, mode
         ) {
            @Override
            protected float getEntityDamageAmount(Entity entity, double distance) {
               return 0.0F;
            }
         };
         if (!EventHooks.onExplosionStart(world, explosion)) {
            explosion.explode();
            explosion.finalizeExplosion(true);
            BlockPos centerPos = entity.blockPosition();
            AABB effectArea = new AABB(centerPos).inflate((double)radius);

            for (LivingEntity livingEntity : world.getEntitiesOfClass(LivingEntity.class, effectArea)) {
               double distance = (double)livingEntity.distanceTo(entity);
               if (distance <= (double)radius) {
                  livingEntity.igniteForSeconds(8);
                  float damage = (float)(4.0 * (1.0 - distance / (double)radius));
                  livingEntity.hurt(world.damageSources().inFire(), damage);
               }
            }
         }
      }
   }

   public static void createSoulFireExplosion(Entity entity, float radius, boolean forceNone) {
      Level world = entity.level();
      if (!world.isClientSide()) {
         DamageSource source = entity instanceof ProjectileEntity projectile ? entity.damageSources().explosion(entity, projectile.getShooter()) : null;
         BlockInteraction mode = BlockInteraction.KEEP;
         Explosion explosion = new ProjectileExplosion(
            world, entity, source, null, entity.getX(), entity.getY(), entity.getZ(), radius * 0.5F, false, mode
         ) {
            @Override
            protected float getEntityDamageAmount(Entity entity, double distance) {
               return 0.0F;
            }
         };
         if (!EventHooks.onExplosionStart(world, explosion)) {
            explosion.explode();
            explosion.finalizeExplosion(true);
            BlockPos centerPos = entity.blockPosition();
            AABB effectArea = new AABB(centerPos).inflate((double)radius);

            for (LivingEntity livingEntity : world.getEntitiesOfClass(LivingEntity.class, effectArea)) {
               double distance = (double)livingEntity.distanceTo(entity);
               if (distance <= (double)radius) {
                  livingEntity.igniteForSeconds(8);
                  float damage = (float)(4.0 * (1.0 - distance / (double)radius));
                  livingEntity.hurt(world.damageSources().inFire(), damage);
               }
            }

            int radiusInt = (int)Math.ceil((double)radius);
            int radiusSquared = radiusInt * radiusInt;

            for (int x = -radiusInt; x <= radiusInt; x++) {
               for (int z = -radiusInt; z <= radiusInt; z++) {
                  BlockPos columnPos = centerPos.offset(x, 0, z);
                  if (centerPos.distSqr(columnPos) <= (double)radiusSquared) {
                     for (int y = -radiusInt; y <= radiusInt; y++) {
                        BlockPos pos = centerPos.offset(x, y, z);
                        BlockState stateAtPos = world.getBlockState(pos);
                        BlockState stateBelow = world.getBlockState(pos.below());
                        if (stateAtPos.isAir() && (stateBelow.isFaceSturdy(world, pos.below(), Direction.UP) || !stateBelow.isAir())) {
                           world.setBlock(pos, ((net.minecraft.world.level.block.Block)ModBlocks.FAKE_SOUL_FIRE.get()).defaultBlockState(), 3);
                           break;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public Entity getOwner() {
      return this.shooter;
   }

   public static class EntityResult {
      private final Entity entity;
      private final Vec3 hitVec;
      private final boolean headshot;

      public EntityResult(Entity entity, Vec3 hitVec, boolean headshot) {
         super();
         this.entity = entity;
         this.hitVec = hitVec;
         this.headshot = headshot;
      }

      public Entity getEntity() {
         return this.entity;
      }

      public Vec3 getHitPos() {
         return this.hitVec;
      }

      public boolean isHeadshot() {
         return this.headshot;
      }
   }

   public static class ProjectileHelper {
      public static final float DEFAULT_SHIELD_DISABLE_CHANCE = 0.3F;

      public ProjectileHelper() {
         super();
      }

      public static boolean handleShieldHit(Entity target, Entity projectile, float damage, float shieldDisableChance) {
         if (!(target instanceof Player player)) {
            return false;
         } else {
            ItemStack mainHandItem = player.getMainHandItem();
            ItemStack offHandItem = player.getOffhandItem();
            boolean isBlockingMainHand = player.isBlocking() && mainHandItem.getItem() instanceof ShieldItem;
            boolean isBlockingOffHand = player.isBlocking() && offHandItem.getItem() instanceof ShieldItem;
            if (!isBlockingMainHand && !isBlockingOffHand) {
               return false;
            } else {
               ItemStack shield = isBlockingMainHand ? mainHandItem : offHandItem;
               InteractionHand hand = isBlockingMainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
               if (projectile.level().getRandom().nextFloat() < shieldDisableChance) {
                  player.getCooldowns().addCooldown(shield.getItem(), 100);
                  player.stopUsingItem();
                  player.level().broadcastEntityEvent(player, (byte)30);
                  player.level()
                     .playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.SHIELD_BREAK,
                        SoundSource.PLAYERS,
                        1.0F,
                        0.8F + player.level().getRandom().nextFloat() * 0.4F
                     );
                  return false;
               } else {
                  player.hurt(player.damageSources().generic(), 0.5F);
                  shield.hurtAndBreak(12, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(player.getUsedItemHand()));
                  player.level()
                     .playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.SHIELD_BLOCK,
                        SoundSource.PLAYERS,
                        1.0F,
                        0.8F + player.level().getRandom().nextFloat() * 0.4F
                     );
                  return true;
               }
            }
         }
      }

      public static boolean handleShieldHit(Entity target, Entity projectile, float damage) {
         return handleShieldHit(target, projectile, damage, 0.3F);
      }
   }
}
