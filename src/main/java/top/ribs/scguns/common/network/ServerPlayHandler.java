package top.ribs.scguns.common.network;





import net.minecraft.server.level.ServerLevel;
import top.ribs.scguns.util.ScEnchants;
import top.ribs.scguns.util.NbtHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import com.mrcrayfish.framework.api.network.LevelLocation;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.AbstractArrow.Pickup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.Config;
import top.ribs.scguns.client.screen.AttachmentContainer;
import top.ribs.scguns.common.BeamWeaponHandler;
import top.ribs.scguns.common.FireMode;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.common.GunEffectsHandler;
import top.ribs.scguns.common.ProjectileManager;
import top.ribs.scguns.common.ReloadType;
import top.ribs.scguns.common.ShootTracker;
import top.ribs.scguns.common.SpreadTracker;
import top.ribs.scguns.entity.projectile.ProjectileEntity;
import top.ribs.scguns.event.GunEventBus;
import top.ribs.scguns.event.GunFireEvent;
import top.ribs.scguns.init.ModEnchantments;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.interfaces.IProjectileFactory;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.ammo_boxes.CreativeAmmoBoxItem;
import top.ribs.scguns.item.animated.ExoSuitItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.C2SMessageShoot;
import top.ribs.scguns.network.message.S2CMessageBulletTrail;
import top.ribs.scguns.network.message.S2CMessageGunSound;
import top.ribs.scguns.util.GunEnchantmentHelper;
import top.ribs.scguns.util.GunModifierHelper;
import top.theillusivec4.curios.api.CuriosApi;

public class ServerPlayHandler {
   public ServerPlayHandler() {
      super();
   }

   public static void handleShoot(C2SMessageShoot message, ServerPlayer player) {
      if (!player.isSpectator() && player.getUseItem().getItem() != Items.SHIELD) {
         Level world = player.level();
         ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
         if (heldItem.getItem() instanceof GunItem item) {
            if (!Gun.hasAmmo(heldItem) && !player.isCreative()) {
               world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, 0.8F);
            } else {
               Gun modifiedGun = item.getModifiedGun(heldItem);
               if (modifiedGun != null) {
                  if (!NeoForge.EVENT_BUS.post(new GunFireEvent.Pre(player, heldItem)).isCanceled()) {
                     player.setYRot(Mth.wrapDegrees(message.getRotationYaw()));
                     player.setXRot(Mth.clamp(message.getRotationPitch(), -90.0F, 90.0F));
                     ShootTracker tracker = ShootTracker.getShootTracker(player);
                     if (!tracker.hasCooldown(item) || tracker.getRemaining(item) <= (long)((Integer)Config.SERVER.cooldownThreshold.get()).intValue()) {
                        tracker.putCooldown(heldItem, item, modifiedGun);
                        if ((Boolean)ModSyncedDataKeys.RELOADING.getValue(player)) {
                           ModSyncedDataKeys.RELOADING.setValue(player, false);
                        }

                        if (!modifiedGun.getProjectile().isAlwaysSpread() && modifiedGun.getProjectile().getSpread() > 0.0F) {
                           SpreadTracker.get(player).update(player, item);
                        }

                        if (FireMode.BEAM.equals(modifiedGun.getGeneral().getFireMode()) || FireMode.SEMI_BEAM.equals(modifiedGun.getGeneral().getFireMode())) {
                           BeamWeaponHandler.handleBeamWeapon(player, heldItem, modifiedGun);
                        } else if (modifiedGun.getProjectile(heldItem).firesArrows()) {
                           int count = modifiedGun.getProjectile().getProjectileAmount();

                           for (int i = 0; i < count; i++) {
                              Arrow arrow = getArrow(player, world, heldItem, modifiedGun);
                              arrow.pickup = Pickup.ALLOWED;
                              world.addFreshEntity(arrow);
                           }
                        } else {
                           fireProjectiles(world, player, heldItem, item, modifiedGun);
                        }

                        if (!FireMode.BEAM.equals(modifiedGun.getGeneral().getFireMode()) && !FireMode.SEMI_BEAM.equals(modifiedGun.getGeneral().getFireMode())
                           )
                         {
                           consumeAmmo(player, heldItem);
                        }

                        handleCasingEjection(player, heldItem, modifiedGun, world);
                        handleWeaponDamage(player, heldItem, world);
                        ResourceLocation fireSound = getFireSound(heldItem, modifiedGun);
                        if (fireSound != null) {
                           playFireSound(player, world, heldItem, modifiedGun, fireSound);
                        }

                        GunEffectsHandler.handleGunEffects(player, heldItem, modifiedGun);
                        NeoForge.EVENT_BUS.post(new GunFireEvent.Post(player, heldItem)).isCanceled();
                        player.awardStat(Stats.ITEM_USED.get(item));
                     }
                  }
               }
            }
         }
      }
   }

   private static void consumeAmmo(ServerPlayer player, ItemStack heldItem) {
      if (!player.isCreative()) {
         CompoundTag tag = NbtHelper.getOrCreateTag(heldItem);
         if (!tag.getBoolean("IgnoreAmmo")) {
            int level = ScEnchants.level(heldItem, ModEnchantments.RECLAIMED);
            if (level == 0 || player.level().random.nextInt(4 - Mth.clamp(level, 1, 2)) != 0) {
               int currentAmmo = tag.getInt("AmmoCount");
               tag.putInt("AmmoCount", Math.max(0, currentAmmo - 1));
            }
         }
      }
   }

   private static void handleCasingEjection(ServerPlayer player, ItemStack heldItem, Gun modifiedGun, Level world) {
      if ((Boolean)Config.COMMON.gameplay.spawnCasings.get()
         && modifiedGun.getProjectile(heldItem).casingType != null
         && !player.getAbilities().instabuild
         && !modifiedGun.getProjectile(heldItem).ejectDuringReload()) {
         ItemStack casingStack = new ItemStack(Objects.requireNonNull((Item)BuiltInRegistries.ITEM.get(modifiedGun.getProjectile(heldItem).casingType)));
         double baseChance = 0.4;
         int enchantmentLevel = ScEnchants.level(heldItem, ModEnchantments.SHELL_CATCHER);
         double finalChance = baseChance + (double)enchantmentLevel * 0.15;
         if (Math.random() < finalChance) {
            if (enchantmentLevel > 0) {
               if (!GunEventBus.addCasingDirectly(player, casingStack)) {
                  GunEventBus.spawnCasingInWorld(world, player, casingStack);
               }
            } else if (!GunEventBus.addCasingToPouch(player, casingStack)) {
               GunEventBus.spawnCasingInWorld(world, player, casingStack);
            }
         }
      }
   }

   private static void handleWeaponDamage(ServerPlayer player, ItemStack heldItem, Level world) {
      if (!player.isCreative()) {
         if ((Boolean)Config.COMMON.gameplay.enableGunDamage.get()) {
            GunEventBus.damageGun(heldItem, world, player);
         }

         if ((Boolean)Config.COMMON.gameplay.enableAttachmentDamage.get()) {
            GunEventBus.damageAttachments(heldItem, world, player);
         }
      }
   }

   @NotNull
   private static Arrow getArrow(ServerPlayer player, Level world, ItemStack heldItem, Gun modifiedGun) {
      Arrow arrow = new Arrow(world, player, new ItemStack(Items.ARROW), ItemStack.EMPTY);
      float speed = (float)modifiedGun.getProjectile(heldItem).getSpeed() * 0.35F;
      float pitch = player.getXRot();
      float yaw = player.getYRot();
      float f = -Mth.sin(yaw * (float) (Math.PI / 180.0)) * Mth.cos(pitch * (float) (Math.PI / 180.0));
      float f1 = -Mth.sin(pitch * (float) (Math.PI / 180.0));
      float f2 = Mth.cos(yaw * (float) (Math.PI / 180.0)) * Mth.cos(pitch * (float) (Math.PI / 180.0));
      Vec3 motion = new Vec3((double)f, (double)f1, (double)f2);
      Vec3 spawnPos = player.getEyePosition().add(motion.x * 0.5, -0.1, motion.z * 0.5);
      arrow.setPos(spawnPos);
      arrow.setDeltaMovement(motion.x * (double)speed, motion.y * (double)speed, motion.z * (double)speed);
      float horizontalDistance = Mth.sqrt((float)(motion.x * motion.x + motion.z * motion.z));
      arrow.setYRot((float)(Mth.atan2(motion.x, motion.z) * (180.0 / Math.PI)));
      arrow.setXRot((float)(Mth.atan2(motion.y, (double)horizontalDistance) * (180.0 / Math.PI)));
      arrow.yRotO = arrow.getYRot();
      arrow.xRotO = arrow.getXRot();
      arrow.setBaseDamage((double)modifiedGun.getProjectile(heldItem).getDamage() * 0.15);
      return arrow;
   }

   private static void fireProjectiles(Level world, ServerPlayer player, ItemStack heldItem, GunItem item, Gun modifiedGun) {
      int count = modifiedGun.getProjectile().getProjectileAmount();
      Gun.Projectile projectileProps = modifiedGun.getProjectile(heldItem);
      ProjectileEntity[] spawnedProjectiles = new ProjectileEntity[count];

      for (int i = 0; i < count; i++) {
         IProjectileFactory factory = ProjectileManager.getInstance().getFactory(BuiltInRegistries.ITEM.getKey(projectileProps.getItem()));
         ProjectileEntity projectileEntity = factory.create(world, player, heldItem, item, modifiedGun);
         projectileEntity.setWeapon(heldItem);
         projectileEntity.setAdditionalDamage(Gun.getAdditionalDamage(heldItem));
         world.addFreshEntity(projectileEntity);
         spawnedProjectiles[i] = projectileEntity;
         projectileEntity.tick();
      }

      if (!projectileProps.shouldHideProjectile()) {
         sendProjectileTrail(player, spawnedProjectiles, projectileProps, false);
      }
   }

   private static void sendProjectileTrail(ServerPlayer player, ProjectileEntity[] projectiles, Gun.Projectile projectileProps, boolean b) {
      if (!projectileProps.shouldHideTrail()) {
         double spawnX = player.getX();
         double spawnY = player.getY() + 1.0;
         double spawnZ = player.getZ();
         double radius = (Double)Config.COMMON.network.projectileTrackingRange.get();
         ParticleOptions data = GunEnchantmentHelper.getParticle(player.getMainHandItem());
         S2CMessageBulletTrail messageBulletTrail = new S2CMessageBulletTrail(projectiles, projectileProps, player.getId(), data, true);
         PacketHandler.getPlayChannel().sendToNearbyPlayers(() -> LevelLocation.create((ServerLevel) player.level(), spawnX, spawnY, spawnZ, radius), messageBulletTrail);
      }
   }

   private static void playFireSound(ServerPlayer player, Level world, ItemStack heldItem, Gun modifiedGun, ResourceLocation fireSound) {
      double posX = player.getX();
      double posY = player.getY() + (double)player.getEyeHeight();
      double posZ = player.getZ();
      float volume = GunModifierHelper.getFireSoundVolume(heldItem);
      float pitch = 0.9F + world.random.nextFloat() * 0.2F;
      double radius = GunModifierHelper.getModifiedFireSoundRadius(heldItem, (Double)Config.SERVER.gunShotMaxDistance.get());
      boolean muzzle = modifiedGun.getDisplay().getFlash() != null;
      S2CMessageGunSound messageSound = new S2CMessageGunSound(
         fireSound, SoundSource.PLAYERS, (float)posX, (float)posY, (float)posZ, volume, pitch, player.getId(), muzzle, false
      );
      PacketHandler.getPlayChannel().sendToNearbyPlayers(() -> LevelLocation.create((ServerLevel) player.level(), posX, posY, posZ, radius), messageSound);
   }

   public static void handleStopBeam(ServerPlayer player) {
      BeamWeaponHandler.stopBeam(player);
   }

   public static EntityHitResult rayTraceEntities(Level world, Entity shooter, Vec3 startVec, Vec3 endVec) {
      endVec.subtract(startVec).normalize();
      double maxDistance = startVec.distanceTo(endVec);
      AABB searchArea = new AABB(startVec, endVec).inflate(1.0);
      Entity closestEntity = null;
      Vec3 hitVec = null;
      double minDistance = maxDistance;

      for (Entity entity : world.getEntities(shooter, searchArea, entityx -> !entityx.isSpectator() && entityx.isPickable() && entityx.isAlive())) {
         AABB entityBB = entity.getBoundingBox().inflate(0.3);
         Optional<Vec3> optionalHit = entityBB.clip(startVec, endVec);
         if (optionalHit.isPresent()) {
            double distance = startVec.distanceTo(optionalHit.get());
            if (distance < minDistance) {
               minDistance = distance;
               closestEntity = entity;
               hitVec = optionalHit.get();
            }
         }
      }

      return closestEntity != null ? new EntityHitResult(closestEntity, hitVec) : null;
   }

   public static void handlePreFireSound(ServerPlayer player) {
      Level world = player.level();
      ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
      if (heldItem.getItem() instanceof GunItem item && (Gun.hasAmmo(heldItem) || player.isCreative())) {
         Gun modifiedGun = item.getModifiedGun(heldItem);
         ResourceLocation fireSound = getPreFireSound(heldItem, modifiedGun);
         if (fireSound != null) {
            double posX = player.getX();
            double posY = player.getY() + (double)player.getEyeHeight();
            double posZ = player.getZ();
            float volume = GunModifierHelper.getFireSoundVolume(heldItem);
            float pitch = 0.9F + world.random.nextFloat() * 0.2F;
            double radius = GunModifierHelper.getModifiedFireSoundRadius(heldItem, (Double)Config.SERVER.gunShotMaxDistance.get());
            S2CMessageGunSound messageSound = new S2CMessageGunSound(
               fireSound, SoundSource.PLAYERS, (float)posX, (float)posY, (float)posZ, volume, pitch, player.getId(), false, false
            );
            PacketHandler.getPlayChannel().sendToNearbyPlayers(() -> LevelLocation.create((ServerLevel) player.level(), posX, posY, posZ, radius), messageSound);
         }
      }
   }

   private static ResourceLocation getFireSound(ItemStack stack, Gun modifiedGun) {
      ResourceLocation fireSound = null;
      if (GunModifierHelper.isSilencedFire(stack)) {
         fireSound = modifiedGun.getSounds().getSilencedFire();
      } else if (stack.isEnchanted()) {
         fireSound = modifiedGun.getSounds().getEnchantedFire();
      }

      return fireSound != null ? fireSound : modifiedGun.getSounds().getFire();
   }

   private static ResourceLocation getPreFireSound(ItemStack stack, Gun modifiedGun) {
      return modifiedGun.getSounds().getPreFire();
   }

   public static void handleUnload(ServerPlayer player) {
      ItemStack stack = player.getMainHandItem();
      if (stack.getItem() instanceof GunItem gunItem) {
         Gun gun = gunItem.getModifiedGun(stack);
         CompoundTag tag = NbtHelper.getTagForWrite(stack);
         if (player.getInventory().items.stream().anyMatch(i -> i.getItem() instanceof CreativeAmmoBoxItem) || hasCreativeAmmoBoxInCurios(player)) {
            return;
         }

         if (gun.getReloads().getReloadType() != ReloadType.SINGLE_ITEM && tag != null && tag.contains("AmmoCount", 3)) {
            int count = tag.getInt("AmmoCount");
            tag.putInt("AmmoCount", 0);
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(gun.getCurrentAmmoItem(stack));
            Item item = (Item)BuiltInRegistries.ITEM.get(id);
            if (item == null) {
               return;
            }

            int maxStackSize = item.getMaxStackSize(new ItemStack(item));
            int stacks = count / maxStackSize;

            for (int i = 0; i < stacks; i++) {
               spawnAmmo(player, new ItemStack(item, maxStackSize));
            }

            int remaining = count % maxStackSize;
            if (remaining > 0) {
               spawnAmmo(player, new ItemStack(item, remaining));
            }
         }
      }
   }

   public static boolean hasCreativeAmmoBoxInCurios(ServerPlayer player) {
      AtomicBoolean found = new AtomicBoolean(false);
      CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
         IItemHandlerModifiable curios = handler.getEquippedCurios();

         for (int i = 0; i < curios.getSlots(); i++) {
            if (curios.getStackInSlot(i).getItem() instanceof CreativeAmmoBoxItem) {
               found.set(true);
               return;
            }
         }
      });
      return found.get();
   }

   public static void handleExtraAmmo(ServerPlayer player) {
      ItemStack stack = player.getMainHandItem();
      if (stack.getItem() instanceof GunItem gunItem) {
         boolean hasCreativeBox = player.getInventory().items.stream().anyMatch(i -> i.getItem() instanceof CreativeAmmoBoxItem)
            || hasCreativeAmmoBoxInCurios(player);
         Gun gun = gunItem.getModifiedGun(stack);
         CompoundTag tag = NbtHelper.getTagForWrite(stack);
         if (tag != null && tag.contains("AmmoCount", 3)) {
            int currentAmmo = tag.getInt("AmmoCount");
            int modifiedCapacity = GunModifierHelper.getModifiedAmmoCapacity(stack, gun);
            if (currentAmmo > modifiedCapacity) {
               tag.putInt("AmmoCount", modifiedCapacity);
               if (!hasCreativeBox) {
                  ResourceLocation id = BuiltInRegistries.ITEM.getKey(gun.getCurrentAmmoItem(stack));
                  Item item = (Item)BuiltInRegistries.ITEM.get(id);
                  if (item != null) {
                     int residue = currentAmmo - modifiedCapacity;
                     spawnAmmo(player, new ItemStack(item, residue));
                  }
               }
            }
         }
      }
   }

   private static void spawnAmmo(ServerPlayer player, ItemStack stack) {
      player.getInventory().add(stack);
      if (stack.getCount() > 0) {
         player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack.copy()));
      }
   }

   public static void handleAttachments(ServerPlayer player) {
      ItemStack heldItem = player.getMainHandItem();
      if (heldItem.getItem() instanceof GunItem) {
         player.openMenu(new SimpleMenuProvider(
               (windowId, playerInventory, player1) -> new AttachmentContainer(windowId, playerInventory, heldItem),
               Component.translatable("container.scguns.attachments")
            ));
      } else if (heldItem.getItem() instanceof ExoSuitItem) {
         player.openMenu(new ExoSuitItem.ExoSuitMenuProvider(InteractionHand.MAIN_HAND), buf -> buf.writeEnum(InteractionHand.MAIN_HAND));
      }
   }

   public static class RatKingAndQueenModel {
      public RatKingAndQueenModel() {
         super();
      }

      public static class GunFireEventRatHandler {
         private static int shotCount = 0;

         public GunFireEventRatHandler() {
            super();
         }

         public static int getShotCount() {
            return shotCount;
         }

         public static void incrementShotCount() {
            shotCount++;
         }

         public static boolean shouldUseAlternateAnimation() {
            return shotCount % 2 == 1;
         }
      }
   }
}
