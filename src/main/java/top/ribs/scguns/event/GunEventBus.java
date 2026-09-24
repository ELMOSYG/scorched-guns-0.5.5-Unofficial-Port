package top.ribs.scguns.event;







import net.minecraft.core.HolderLookup;
import net.minecraft.core.Holder;
import top.ribs.scguns.util.ScEnchants;
import top.ribs.scguns.util.Caps;
import top.ribs.scguns.util.NbtHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import top.ribs.scguns.Config;
import top.ribs.scguns.ScorchedGuns;
import top.ribs.scguns.cache.HotBarrelCache;
import top.ribs.scguns.client.handler.MeleeAttackHandler;
import top.ribs.scguns.common.BeamHandlerCommon;
import top.ribs.scguns.common.FireMode;
import top.ribs.scguns.common.GripType;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.common.GunModifiers;
import top.ribs.scguns.common.exosuit.ExoSuitData;
import top.ribs.scguns.common.exosuit.ExoSuitUpgrade;
import top.ribs.scguns.common.exosuit.ExoSuitUpgradeManager;
import top.ribs.scguns.common.network.ServerPlayHandler;
import top.ribs.scguns.init.ModEnchantments;
import top.ribs.scguns.init.ModSounds;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.interfaces.IAirGun;
import top.ribs.scguns.interfaces.IEnergyGun;
import top.ribs.scguns.interfaces.IGunModifier;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.NonUnderwaterGunItem;
import top.ribs.scguns.item.UnderwaterGunItem;
import top.ribs.scguns.item.ammo_boxes.EmptyCasingPouchItem;
import top.ribs.scguns.item.animated.AnimatedDiamondSteelUnderWaterGunItem;
import top.ribs.scguns.item.animated.AnimatedDualWieldGunItem;
import top.ribs.scguns.item.animated.AnimatedGunItem;
import top.ribs.scguns.item.animated.AnimatedUnderWaterGunItem;
import top.ribs.scguns.item.animated.ExoSuitItem;
import top.ribs.scguns.item.animated.NonUnderwaterAnimatedGunItem;
import top.ribs.scguns.item.attachment.IAttachment;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.C2SMessageReload;
import top.ribs.scguns.network.message.S2CMessageHotBarrelSync;
import top.ribs.scguns.util.AirSourceHelper;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME
)
public class GunEventBus {
   public GunEventBus() {
      super();
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent.Post event) {
      {
         MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
         if (server != null) {
            for (ServerLevel world : server.getAllLevels()) {
               BeamHandlerCommon.BeamMiningManager.tickMiningProgress(world);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onServerStopping(ServerStoppingEvent event) {
      MinecraftServer server = event.getServer();

      for (ServerLevel world : server.getAllLevels()) {
         TemporaryLightManager.emergencyCleanup(world);
      }
   }

   @SubscribeEvent
   public static void preShoot(GunFireEvent.Pre event) {
      Player player = event.getEntity();
      Level level = event.getEntity().level();
      ItemStack heldItem = player.getMainHandItem();
      CompoundTag tag = NbtHelper.getTagForWrite(heldItem);
      if (MeleeAttackHandler.isBanzaiActive()) {
         MeleeAttackHandler.stopBanzai();
      }

      if (level.isClientSide() && heldItem.getItem() instanceof AnimatedGunItem animatedGunItem) {
         try {
            long id = GeoItem.getId(heldItem);
            AnimationController<GeoAnimatable> animationController = (AnimationController<GeoAnimatable>)animatedGunItem.getAnimatableInstanceCache()
               .getManagerForId(id)
               .getAnimationControllers()
               .get("controller");
            if (animationController != null) {
               if (heldItem.isDamageableItem() && heldItem.getDamageValue() >= heldItem.getMaxDamage() - 1) {
                  player.displayClientMessage(Component.translatable("message.scguns.gun_broken").withStyle(ChatFormatting.RED), true);
                  event.setCanceled(true);
                  return;
               }

               if (animatedGunItem.isAnimationPlaying(animationController, "reload_stop")) {
                  // 0.5.5 cancelled here: no shot may fire while the reload-stop animation is playing.
                  event.setCanceled(true);
                  return;
               }

               if (tag != null && tag.getBoolean("scguns:IsReloading")) {
                  if (animatedGunItem.isAnimationPlaying(animationController, "reload_loop")
                     || animatedGunItem.isAnimationPlaying(animationController, "reload_start")) {
                     tag.putBoolean("scguns:ReloadComplete", true);
                     animationController.tryTriggerAnimation("reload_stop");
                     tag.remove("scguns:IsReloading");
                     ModSyncedDataKeys.RELOADING.setValue(player, false);
                     PacketHandler.getPlayChannel().sendToServer(new C2SMessageReload(false));
                  }

                  event.setCanceled(true);
                  return;
               }
            }
         } catch (Exception var15) {
            ScorchedGuns.LOGGER.error("Error in preShoot animation handling: " + var15.getMessage());
         }
      }

      if (heldItem.getItem() instanceof GunItem gunItem) {
         Gun gun = gunItem.getModifiedGun(heldItem);
         GripType gripType = gun.determineGripType(heldItem);
         if (player.isUsingItem() && player.getOffhandItem().getItem() == Items.SHIELD && (gripType == GripType.ONE_HANDED || gripType == GripType.ONE_HANDED_2)) {
            event.setCanceled(true);
            return;
         }

         if (NbtHelper.getTag(heldItem) != null && tag != null && tag.contains("DrawnTick") && tag.getInt("DrawnTick") < 15) {
            // 0.5.5 cancelled here: the gun is still being drawn (first 15 ticks), so no shot is fired.
            event.setCanceled(true);
            return;
         }

         int energyUse = gun.getProjectile().getEnergyUse();
         if (!player.isCreative()) {
            if (heldItem.getItem() instanceof IEnergyGun) {
               IEnergyStorage energyStorage = (IEnergyStorage)heldItem.getCapability(Capabilities.EnergyStorage.ITEM);
               if (energyStorage.getEnergyStored() < energyUse) {
                  player.displayClientMessage(Component.translatable("message.energy_gun.no_energy").withStyle(ChatFormatting.RED), true);
                  event.setCanceled(true);
                  return;
               }

               energyStorage.extractEnergy(energyUse, false);
            }

            if (heldItem.getItem() instanceof IAirGun) {
               float airCostPerShot = calculateAirCostPerShot(gun);
               if (!AirSourceHelper.consumeAir(player, airCostPerShot)) {
                  AirSourceHelper.AirSource airSource = AirSourceHelper.getBestAirSource(player);
                  if (airSource.getType() == AirSourceHelper.AirSource.Type.NONE) {
                     if (ScorchedGuns.createLoaded) {
                        player.displayClientMessage(Component.translatable("message.airgun.no_air_source").withStyle(ChatFormatting.RED), true);
                     } else {
                        player.displayClientMessage(Component.translatable("message.airgun.requires_canister").withStyle(ChatFormatting.RED), true);
                     }
                  } else {
                     player.displayClientMessage(Component.translatable("message.airgun.no_air").withStyle(ChatFormatting.RED), true);
                  }

                  event.setCanceled(true);
                  return;
               }
            }
         }

         if ((heldItem.getItem() instanceof NonUnderwaterGunItem || heldItem.getItem() instanceof NonUnderwaterAnimatedGunItem) && player.isUnderWater()) {
            event.setCanceled(true);
            return;
         }

         if (heldItem.isDamageableItem() && tag != null) {
            if (heldItem.getDamageValue() == heldItem.getMaxDamage() - 1) {
               level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
               player.displayClientMessage(Component.translatable("message.scguns.gun_broken").withStyle(ChatFormatting.RED), true);
               event.getEntity().getCooldowns().addCooldown(event.getStack().getItem(), gun.getGeneral().getRate());
               event.setCanceled(true);
               return;
            }

            int maxDamage = heldItem.getMaxDamage();
            int currentDamage = heldItem.getDamageValue();
            int gunRustLevel = ScEnchants.level(heldItem, ModEnchantments.GUN_RUST);
            double jamChance = 0.0;
            if (gunRustLevel > 0) {
               jamChance = 0.1 + (double)gunRustLevel * 0.05;
            }

            if ((double)currentDamage >= (double)maxDamage * 0.8) {
               jamChance += 0.025;
            }

            if (jamChance > 0.0 && Math.random() < jamChance) {
               event.getEntity().playSound((SoundEvent)ModSounds.ITEM_PISTOL_COCK.get(), 1.0F, 1.0F);
               player.displayClientMessage(Component.translatable("message.scguns.gun_jammed").withStyle(ChatFormatting.YELLOW), true);
               int coolDown = gun.getGeneral().getRate() * 10;
               if (coolDown > 30) {
                  coolDown = 30;
               }

               event.getEntity().getCooldowns().addCooldown(event.getStack().getItem(), coolDown);
               event.setCanceled(true);
               return;
            }

            if (tag.getInt("AmmoCount") >= 1) {
               broken(heldItem, level, player);
            }
         }
      }
   }

   @SubscribeEvent
   public static void postShoot(GunFireEvent.Post event) {
      Player player = event.getEntity();
      Level level = event.getEntity().level();
      ItemStack heldItem = player.getMainHandItem();
      if (heldItem.getItem() instanceof AnimatedGunItem gunItem) {
         Gun gun = gunItem.getModifiedGun(heldItem);
         if (gun.getGeneral().isRevolver()) {
            ((AnimatedGunItem)heldItem.getItem()).getRotationHandler().incrementCylinderRotation(30.0F);
         }

         if (heldItem.getItem().toString().contains("cogloader")) {
            ((AnimatedGunItem)heldItem.getItem()).getRotationHandler().incrementMagazineRotation(15.0F);
         }

         if (heldItem.getItem().toString().contains("scrapper")) {
            float maxAmmo = (float)Gun.getMaxAmmo(heldItem);
            float currentAmmo = (float)Gun.getAmmoCount(heldItem);
            float slidePosition = Math.min((maxAmmo - currentAmmo) / maxAmmo, 1.0F);
            // 1.21: re-acquire instead of holding a tag across Gun.getAmmoCount(),
            // which re-detaches the component and would leave a stale local behind.
            NbtHelper.getOrCreateTag(heldItem).putFloat("MagazinePosition", slidePosition);
         }

         long id = GeoItem.getId(heldItem);
         AnimationController<GeoAnimatable> controller = (AnimationController<GeoAnimatable>)gunItem.getAnimatableInstanceCache()
            .getManagerForId(id)
            .getAnimationControllers()
            .get("controller");
         // Null on a dedicated server (animation controllers are client-side) and before the client has
         // registered them; the gameplay work further down this listener must still run either way.
         if (controller != null) {
            controller.forceAnimationReset();
            boolean isCarbine = gunItem.isInCarbineMode(heldItem);
            if (gunItem instanceof AnimatedDualWieldGunItem) {
               ServerPlayHandler.RatKingAndQueenModel.GunFireEventRatHandler.incrementShotCount();
               boolean useAlternate = ServerPlayHandler.RatKingAndQueenModel.GunFireEventRatHandler.shouldUseAlternateAnimation();
               if ((Boolean)ModSyncedDataKeys.AIMING.getValue(player)) {
                  controller.tryTriggerAnimation(useAlternate ? "aim_shoot1" : "aim_shoot");
               } else {
                  controller.tryTriggerAnimation(useAlternate ? "shoot1" : "shoot");
               }
            } else if ((Boolean)ModSyncedDataKeys.AIMING.getValue(player)) {
               controller.tryTriggerAnimation(isCarbine ? "carbine_aim_shoot" : "aim_shoot");
            } else {
               controller.tryTriggerAnimation(isCarbine ? "carbine_shoot" : "shoot");
            }
         }
      }

      if (heldItem.getItem() instanceof GunItem gunItem) {
         Gun gunx = gunItem.getModifiedGun(heldItem);
         int hotBarrelLevel = ScEnchants.level(heldItem, ModEnchantments.HOT_BARREL);
         if (gunx.getProjectile().hasPlayerKnockBack()) {
            applyGunKnockback(player, gunx);
         }

         if (hotBarrelLevel > 0) {
            int hotBarrelFillRate = gunx.getGeneral().getHotBarrelRate();
            HotBarrelCache.increaseHotBarrel(player, heldItem, hotBarrelFillRate);
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
               int newLevel = HotBarrelCache.getHotBarrelLevel(player, heldItem);
               PacketHandler.getPlayChannel().sendToPlayer(() -> serverPlayer, new S2CMessageHotBarrelSync(newLevel, heldItem.getItem().getDescriptionId()));
            }
         }

         if (gunx.getGeneral().isEnableGunLight()) {
            Vec3 lookVec = player.getLookAngle();
            BlockPos lightPos = player.blockPosition().offset((int)(lookVec.x * 2.0), 2, (int)(lookVec.z * 2.0));
            boolean isBeamWeapon = gunx.getGeneral().getFireMode() == FireMode.BEAM;
            TemporaryLightManager.addTemporaryLight(level, lightPos, isBeamWeapon);
         }

         int shotCount = ServerPlayHandler.RatKingAndQueenModel.GunFireEventRatHandler.getShotCount();
         boolean mirror = heldItem.getItem() instanceof AnimatedDualWieldGunItem && shotCount % 2 == 1;
         if ((Boolean)Config.COMMON.gameplay.spawnCasings.get()
            && gunx.getProjectile().ejectsCasing()
            && !gunx.getProjectile().ejectDuringReload()
            && (NbtHelper.getOrCreateTag(heldItem).getInt("AmmoCount") >= 1 || player.getAbilities().instabuild)) {
            ejectCasing(level, player, mirror);
         }

         if (heldItem.isDamageableItem() && (double)heldItem.getDamageValue() >= (double)heldItem.getMaxDamage() / 1.5 && Math.random() < 0.15) {
            level.playSound(player, player.blockPosition(), (SoundEvent)ModSounds.COPPER_GUN_JAM.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
         }
      }
   }

   private static void applyGunKnockback(Player player, Gun gun) {
      Vec3 lookVec = player.getLookAngle();
      float baseStrength = gun.getProjectile().getPlayerKnockBackStrength();
      float totalKnockbackResistance = 0.0F;
      totalKnockbackResistance += (float)player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * 0.5F;

      for (ItemStack armorPiece : player.getArmorSlots()) {
         Item material = armorPiece.getItem();
         if (material instanceof ArmorItem) {
            ArmorItem armor = (ArmorItem)material;
            ArmorMaterial materialx = armor.getMaterial().value();
            totalKnockbackResistance += materialx.knockbackResistance() * 0.25F;
         }
      }

      totalKnockbackResistance = Math.min(0.75F, totalKnockbackResistance);
      float effectiveStrength = baseStrength * (1.0F - totalKnockbackResistance);
      if (effectiveStrength > 0.0F) {
         double verticalBoost;
         if (lookVec.y < -0.5 && !player.onGround() && player.getDeltaMovement().y > 0.0) {
            verticalBoost = (double)effectiveStrength * 1.25;
         } else {
            verticalBoost = 0.1 * (double)effectiveStrength;
         }

         player.setDeltaMovement(
            player.getDeltaMovement().add(-lookVec.x * (double)effectiveStrength, verticalBoost, -lookVec.z * (double)effectiveStrength)
         );
         if (verticalBoost > 0.5) {
            player.fallDistance = 0.0F;
         }

         if (player instanceof ServerPlayer) {
            ((ServerPlayer)player).connection.send(new ClientboundSetEntityMotionPacket(player));
         }
      }
   }

   // 0.5.5 declared this against the base `PlayerTickEvent` (a concrete class in Forge
   // that fired on both phases). NeoForge's bus refuses listeners for abstract event
   // classes, so this is pinned to Post - once per tick, like the rest of the port.
   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      Player player = event.getEntity();
      ItemStack heldItem = player.getMainHandItem();
      if (heldItem.getItem() instanceof GunItem && ScEnchants.level(heldItem, ModEnchantments.HOT_BARREL) > 0) {
         int levelBefore = HotBarrelCache.getHotBarrelLevel(player, heldItem);
         if (levelBefore > 0) {
         }

         HotBarrelCache.tickHotBarrel(player, heldItem);
      } else {
         for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack itemStack = player.getInventory().getItem(i);
            if (itemStack.getItem() instanceof GunItem
               && ScEnchants.level(itemStack, ModEnchantments.HOT_BARREL) > 0
               && HotBarrelCache.getHotBarrelLevel(player, itemStack) > 0) {
               HotBarrelCache.clearHotBarrel(player, itemStack);
            }
         }
      }

      if (player.tickCount % 1200 == 0) {
         HotBarrelCache.cleanupOldEntries();
      }
   }

   private static float calculateAirCostPerShot(Gun gun) {
      return (float)gun.getProjectile().getEnergyUse();
   }

   public static void broken(ItemStack stack, Level level, Player player) {
      int maxDamage = stack.getMaxDamage();
      int currentDamage = stack.getDamageValue();
      if (currentDamage >= maxDamage - 2) {
         level.playSound(player, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
      }
   }

   public static void damageGun(ItemStack stack, Level level, Player player) {
      if (!player.getAbilities().instabuild && (Boolean)Config.COMMON.gameplay.enableGunDamage.get() && stack.isDamageableItem()) {
         int maxDamage = stack.getMaxDamage();
         int currentDamage = stack.getDamageValue();
         boolean isUnderwater = player.isUnderWater();
         boolean isUnderwaterGun = stack.getItem() instanceof UnderwaterGunItem
            || stack.getItem() instanceof AnimatedUnderWaterGunItem
            || stack.getItem() instanceof AnimatedDiamondSteelUnderWaterGunItem;
         int damageAmount = 1;
         if (stack.getItem() instanceof GunItem gunItem) {
            Gun gun = gunItem.getModifiedGun(stack);
            damageAmount = gun.getProjectile(stack).getDurabilityDamage();
         }

         int waterProofLevel = ScEnchants.level(stack, ModEnchantments.WATER_PROOF);
         int acceleratorLevel = ScEnchants.level(stack, ModEnchantments.ACCELERATOR);
         if (isUnderwater) {
            if (isUnderwaterGun) {
               if (waterProofLevel > 0 && Math.random() < 0.25) {
                  damageAmount = 0;
               }
            } else if (waterProofLevel <= 0) {
               damageAmount = 3;
            }
         }

         if (acceleratorLevel > 0 && damageAmount > 0) {
            float catastrophicChance = 0.07F * (float)acceleratorLevel;
            if (Math.random() < (double)catastrophicChance) {
               damageAmount *= 8;
            } else {
               float extraWearChance = 0.25F * (float)acceleratorLevel;
               if (Math.random() < (double)extraWearChance) {
                  damageAmount *= 2;
               }
            }
         }

         if (stack.getItem() instanceof GunItem gunItem) {
            ItemStack stockStack = Gun.getAttachment(IAttachment.Type.STOCK, stack);
            if (!stockStack.isEmpty() && stockStack.getItem() instanceof IAttachment<?> attachment) {
               for (IGunModifier modifier : attachment.getProperties().getModifiers()) {
                  if (modifier == GunModifiers.BUMP_STOCK_MODIFIER && damageAmount > 0) {
                     if (Math.random() < 0.3) {
                        damageAmount *= 2;
                     }
                     break;
                  }
               }
            }
         }

         if (currentDamage >= maxDamage - damageAmount) {
            if (currentDamage >= maxDamage - damageAmount - 1) {
               level.playSound(player, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
         } else {
            stack.hurtAndBreak(damageAmount, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
         }
      }
   }

   public static void damageAttachments(ItemStack stack, Level level, Player player) {
      if (!player.getAbilities().instabuild && (Boolean)Config.COMMON.gameplay.enableAttachmentDamage.get() && stack.getItem() instanceof GunItem) {
         damageAttachment(stack, level, player, IAttachment.Type.SCOPE);
         damageAttachment(stack, level, player, IAttachment.Type.BARREL);
         damageAttachment(stack, level, player, IAttachment.Type.STOCK);
         damageAttachment(stack, level, player, IAttachment.Type.MAGAZINE);
         damageAttachment(stack, level, player, IAttachment.Type.UNDER_BARREL);
      }
   }

   /**
    * Wears one attachment down by a point, or breaks it off the gun.
    *
    * <p>0.5.5 spelled the five types out inline and damaged the stack {@link Gun#getAttachment}
    * returned - which is a <b>decode of the gun's tag</b>, so the damage was written to a throwaway
    * copy and the attachment never wore out at all (and the break branch was unreachable). The
    * write-back through {@link Gun#setAttachment} is what makes wear real, matching the upstream
    * 1.21.1 port (HANDOFF section 60).</p>
    */
   private static void damageAttachment(ItemStack gunStack, Level level, Player player, IAttachment.Type type) {
      if (!Gun.hasAttachmentEquipped(gunStack, type)) {
         return;
      }

      ItemStack attachmentStack = Gun.getAttachment(type, gunStack);
      if (!attachmentStack.isDamageableItem()) {
         return;
      }

      int maxDamage = attachmentStack.getMaxDamage();
      int currentDamage = attachmentStack.getDamageValue();
      if (currentDamage >= maxDamage - 1) {
         level.playSound(player, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
         Gun.removeAttachment(gunStack, type.getTagKey());
      } else {
         // Stays below maxDamage on purpose: the breaking sound above and the removal are ours, so
         // hurtAndBreak must never be the thing that decides an attachment is destroyed.
         attachmentStack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
         Gun.setAttachment(gunStack, type, attachmentStack, player.registryAccess());
      }
   }

   public static void ejectCasing(Level level, LivingEntity livingEntity, boolean mirror) {
      if (level.isClientSide()) {
         if (livingEntity instanceof Player playerEntity) {
            ItemStack heldItem = playerEntity.getMainHandItem();
            Gun gun = ((GunItem)heldItem.getItem()).getModifiedGun(heldItem);
            Vec3 lookVec = playerEntity.getLookAngle();
            Vec3 rightVec = new Vec3(-lookVec.z, 0.0, lookVec.x).normalize();
            Vec3 forwardVec = new Vec3(lookVec.x, 0.0, lookVec.z).normalize();
            double offsetX = (mirror ? -rightVec.x : rightVec.x) * 0.5 + forwardVec.x * 0.5;
            double offsetY = (double)playerEntity.getEyeHeight() - 0.4;
            double offsetZ = (mirror ? -rightVec.z : rightVec.z) * 0.5 + forwardVec.z * 0.5;
            Vec3 particlePos = playerEntity.getPosition(1.0F).add(offsetX, offsetY, offsetZ);
            ResourceLocation particleLocation = gun.getProjectile().getCasingParticle();
            if (particleLocation != null) {
               ParticleType<?> particleType = (ParticleType<?>)BuiltInRegistries.PARTICLE_TYPE.get(particleLocation);
               if (particleType instanceof SimpleParticleType simpleParticleType) {
                  level.addParticle(simpleParticleType, particlePos.x, particlePos.y, particlePos.z, 0.0, 0.0, 0.0);
               }
            }
         }
      }
   }

   public static void spawnCasingInWorld(Level level, Player player, ItemStack casingStack) {
      ItemEntity casingEntity = new ItemEntity(level, player.getX(), player.getY() + 1.5, player.getZ(), casingStack);
      casingEntity.setPickUpDelay(40);
      casingEntity.setDeltaMovement(0.0, 0.2, 0.0);
      level.addFreshEntity(casingEntity);
   }

   public static boolean addCasingToPouch(Player player, ItemStack casingStack) {
      ItemStack casingCopy = casingStack.copy();

      for (ItemStack itemStack : player.getInventory().items) {
         if (itemStack.getItem() instanceof EmptyCasingPouchItem) {
            int insertedItems = EmptyCasingPouchItem.add(itemStack, casingCopy);
            if (insertedItems > 0) {
               return true;
            }
         }
      }

      if (addCasingToExoSuitPouches(player, casingCopy)) {
         return true;
      } else {
         boolean[] result = new boolean[]{false};
         CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            IItemHandlerModifiable curios = handler.getEquippedCurios();

            for (int i = 0; i < curios.getSlots(); i++) {
               ItemStack stack = curios.getStackInSlot(i);
               if (stack.getItem() instanceof EmptyCasingPouchItem) {
                  int insertedItemsx = EmptyCasingPouchItem.add(stack, casingCopy);
                  if (insertedItemsx > 0) {
                     result[0] = true;
                     return;
                  }
               }
            }
         });
         return result[0];
      }
   }

   public static boolean addCasingDirectly(Player player, ItemStack casingStack) {
      for (ItemStack itemStack : player.getInventory().items) {
         if (itemStack.getItem() instanceof EmptyCasingPouchItem) {
            int insertedItems = EmptyCasingPouchItem.add(itemStack, casingStack);
            if (insertedItems > 0) {
               return true;
            }
         }
      }

      if (addCasingToExoSuitPouches(player, casingStack)) {
         return true;
      } else {
         boolean[] result = new boolean[]{false};
         CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            IItemHandlerModifiable curios = handler.getEquippedCurios();

            for (int i = 0; i < curios.getSlots(); i++) {
               ItemStack stack = curios.getStackInSlot(i);
               if (stack.getItem() instanceof EmptyCasingPouchItem) {
                  int insertedItemsx = EmptyCasingPouchItem.add(stack, casingStack);
                  if (insertedItemsx > 0) {
                     result[0] = true;
                     return;
                  }
               }
            }
         });
         return result[0] ? true : player.getInventory().add(casingStack);
      }
   }

   private static boolean addCasingToExoSuitPouches(Player player, ItemStack casingStack) {
      ItemStack chestplate = getEquippedChestplate(player);
      if (chestplate.isEmpty()) {
         return false;
      } else {
         ItemStack pouchUpgrade = findPouchUpgrade(chestplate);
         if (pouchUpgrade.isEmpty()) {
            return false;
         } else {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(pouchUpgrade);
            if (upgrade == null) {
               return false;
            } else {
               String pouchId = getPouchId(pouchUpgrade);
               ItemStackHandler pouchInventory = getPouchInventory(chestplate, pouchId, upgrade.getDisplay().getStorageSize());

               for (int i = 0; i < pouchInventory.getSlots(); i++) {
                  ItemStack stack = pouchInventory.getStackInSlot(i);
                  if (!stack.isEmpty() && stack.getItem() instanceof EmptyCasingPouchItem) {
                     int insertedItems = EmptyCasingPouchItem.add(stack, casingStack);
                     if (insertedItems > 0) {
                        savePouchInventory(chestplate, pouchId, pouchInventory);
                        return true;
                     }
                  }
               }

               return false;
            }
         }
      }
   }

   private static ItemStack getEquippedChestplate(Player player) {
      for (ItemStack armorStack : player.getArmorSlots()) {
         if (armorStack.getItem() instanceof ExoSuitItem exosuit && exosuit.getType() == Type.CHESTPLATE) {
            return armorStack;
         }
      }

      return ItemStack.EMPTY;
   }

   private static ItemStack findPouchUpgrade(ItemStack chestplate) {
      for (int slot = 0; slot < 4; slot++) {
         ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(chestplate, slot);
         if (!upgradeItem.isEmpty()) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
            if (upgrade != null && upgrade.getType().equals("pouches")) {
               return upgradeItem;
            }
         }
      }

      return ItemStack.EMPTY;
   }

   private static String getPouchId(ItemStack pouchUpgrade) {
      return pouchUpgrade.getItem().toString();
   }

   private static ItemStackHandler getPouchInventory(ItemStack chestplate, String pouchId, int size) {
      CompoundTag pouchData = NbtHelper.getOrCreateTag(chestplate).getCompound("PouchData");
      ItemStackHandler handler = new ItemStackHandler(size);
      if (pouchData.contains(pouchId)) {
         handler.deserializeNBT(net.minecraft.client.Minecraft.getInstance().level.registryAccess(), pouchData.getCompound(pouchId));
      }

      return handler;
   }

   private static void savePouchInventory(ItemStack chestplate, String pouchId, ItemStackHandler handler) {
      CompoundTag pouchData = NbtHelper.getOrCreateTag(chestplate).getCompound("PouchData");
      pouchData.put(pouchId, handler.serializeNBT(net.minecraft.client.Minecraft.getInstance().level.registryAccess()));
      NbtHelper.getOrCreateTag(chestplate).put("PouchData", pouchData);
   }
}
