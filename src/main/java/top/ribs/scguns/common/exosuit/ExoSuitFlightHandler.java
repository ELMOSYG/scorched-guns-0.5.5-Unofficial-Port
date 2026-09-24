package top.ribs.scguns.common.exosuit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.ribs.scguns.init.ModSounds;
import top.ribs.scguns.item.animated.ExoSuitItem;
import top.ribs.scguns.network.PacketHandler;
import top.ribs.scguns.network.message.C2SMessageJetpackState;
import top.ribs.scguns.network.message.C2SMessageJetpackThrust;

@EventBusSubscriber(
   modid = "scguns"
)
public class ExoSuitFlightHandler {
   private static final float FLIGHT_UPWARD_SPEED = 0.8F;
   private static final float FLIGHT_DESCENT_SPEED = 0.4F;
   private static final float NATURAL_SINK_SPEED = 0.02F;
   private static final float HORIZONTAL_ACCELERATION = 0.001F;
   private static final float HORIZONTAL_DRAG = 0.85F;
   private static final float MAX_HORIZONTAL_SPEED = 0.04F;
   private static final float SPRINT_MULTIPLIER = 1.15F;
   private static final float STOP_THRESHOLD = 0.005F;
   private static final double JETPACK_OFFSET_DISTANCE = -0.5;
   private static final double JETPACK_HEIGHT_OFFSET = 0.7;
   private static final double JETPACK_SIDE_OFFSET = 0.4;
   private static final int PARTICLE_COUNT = 1;
   private static final Map<UUID, Long> lastEnergyConsumptionTime = new HashMap<>();
   private static final long ENERGY_CONSUMPTION_INTERVAL = 1000L;
   private static final Map<UUID, Boolean> serverJetpackStates = new HashMap<>();
   private static final Map<UUID, Boolean> playerThrustStates = new HashMap<>();
   private static final Map<UUID, Long> lastThrustTime = new HashMap<>();
   private static final long THRUST_TIMEOUT = 100L;
   private static long lastThrustSoundTime = 0L;
   private static final long THRUST_SOUND_COOLDOWN = 300L;
   private static boolean wasGamePaused = false;
   private static boolean wasJumpPressed = false;
   private static long lastJumpPressTime = 0L;
   private static final long DOUBLE_TAP_WINDOW = 300L;
   private static boolean clientJetpackActive = false;
   private static long lastLoopSoundTime = 0L;
   private static final long LOOP_SOUND_DURATION = 3000L;
   private static boolean isLoopSoundPlaying = false;
   private static boolean wasThrustingLastTick = false;
   private static boolean exoSuitFlightEnabled = false;
   private static float originalFlySpeed = 0.05F;
   private static boolean originalMayFly = false;
   private static boolean wasDescending = false;
   private static long landingProtectionTime = 0L;
   private static final long LANDING_PROTECTION_DURATION = 500L;

   public ExoSuitFlightHandler() {
      super();
   }

   public static void setJetpackActive(Player player, boolean active) {
      serverJetpackStates.put(player.getUUID(), active);
   }

   public static void setPlayerThrusting(Player player, boolean thrusting) {
      UUID playerId = player.getUUID();
      playerThrustStates.put(playerId, thrusting);
      if (thrusting) {
         lastThrustTime.put(playerId, System.currentTimeMillis());
      }
   }

   public static boolean isPlayerThrusting(Player player) {
      UUID playerId = player.getUUID();
      Boolean thrusting = playerThrustStates.get(playerId);
      if (thrusting != null && thrusting) {
         Long lastThrust = lastThrustTime.get(playerId);
         return lastThrust == null ? false : System.currentTimeMillis() - lastThrust < 100L;
      } else {
         return false;
      }
   }

   public static boolean isJetpackActiveOnServer(Player player) {
      return serverJetpackStates.getOrDefault(player.getUUID(), false);
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (!event.getEntity().level().isClientSide) {
         Player player = event.getEntity();
         if (player.tickCount % 10 == 0) {
            handleJetpackEnergyConsumption(player);
         }
      }
   }

   private static void handleJetpackEnergyConsumption(Player player) {
      boolean jetpackActive = isJetpackActiveOnServer(player);
      if (jetpackActive) {
         if (!hasJetpackModule(player)) {
            setJetpackActive(player, false);
         } else {
            boolean utilityEnabled = ExoSuitPowerManager.isPowerEnabled(player, "utility");
            if (!utilityEnabled) {
               setJetpackActive(player, false);
            } else {
               boolean canFunction = ExoSuitPowerManager.canUpgradeFunction(player, "utility");
               if (!canFunction) {
                  setJetpackActive(player, false);
               } else {
                  boolean isThrusting = isPlayerThrusting(player);
                  if (isThrusting) {
                     UUID playerId = player.getUUID();
                     long currentTime = System.currentTimeMillis();
                     Long lastConsumption = lastEnergyConsumptionTime.get(playerId);
                     if (lastConsumption == null || currentTime - lastConsumption >= 1000L) {
                        ItemStack jetpackUpgrade = findJetpackModuleFromPlayer(player);
                        if (jetpackUpgrade.isEmpty()) {
                           setJetpackActive(player, false);
                           return;
                        }

                        if (!ExoSuitPowerManager.consumeEnergyForUpgrade(player, "utility", jetpackUpgrade)) {
                           setJetpackActive(player, false);
                           return;
                        }

                        lastEnergyConsumptionTime.put(playerId, currentTime);
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean hasJetpackModule(Player player) {
      return !findJetpackModuleFromPlayer(player).isEmpty();
   }

   private static ItemStack findJetpackModuleFromPlayer(Player player) {
      ItemStack chestplate = getEquippedChestplate(player);
      return chestplate.isEmpty() ? ItemStack.EMPTY : findJetpackModule(chestplate);
   }

   private static ItemStack getEquippedChestplate(Player player) {
      for (ItemStack armorStack : player.getArmorSlots()) {
         if (armorStack.getItem() instanceof ExoSuitItem exosuit && exosuit.getType() == Type.CHESTPLATE) {
            return armorStack;
         }
      }

      return ItemStack.EMPTY;
   }

   @OnlyIn(Dist.CLIENT)
   @SubscribeEvent
   public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
      Player player = event.getEntity();
      if (isUsingExoSuitJetpack(player) && exoSuitFlightEnabled && player.getAbilities().flying && event.getInput().shiftKeyDown) {
         event.getInput().shiftKeyDown = true;
      }
   }

   private static boolean hasExoSuitJetpackSetup(Player player) {
      ItemStack chestplate = player.getInventory().getArmor(2);
      if (!(chestplate.getItem() instanceof ExoSuitItem)) {
         return false;
      } else {
         ItemStack jetpackUpgrade = findJetpackModule(chestplate);
         if (jetpackUpgrade.isEmpty()) {
            return false;
         } else {
            ExoSuitUpgrade.Effects totalEffects = ExoSuitEffectsHandler.getTotalEffects(player);
            return totalEffects.hasFlight();
         }
      }
   }

   private static boolean isUsingExoSuitJetpack(Player player) {
      return player.level().isClientSide
         ? hasExoSuitJetpackSetup(player) && clientJetpackActive
         : hasExoSuitJetpackSetup(player) && isJetpackActiveOnServer(player);
   }

   private static ItemStack findJetpackModule(ItemStack chestplate) {
      for (int slot = 0; slot < 4; slot++) {
         ItemStack upgradeItem = ExoSuitData.getUpgradeInSlot(chestplate, slot);
         if (!upgradeItem.isEmpty()) {
            ExoSuitUpgrade upgrade = ExoSuitUpgradeManager.getUpgradeForItem(upgradeItem);
            if (upgrade != null && upgrade.getType().equals("utility") && upgrade.getEffects().hasFlight()) {
               return upgradeItem;
            }
         }
      }

      return ItemStack.EMPTY;
   }

   @SubscribeEvent
   public static void onLivingHurt(LivingIncomingDamageEvent event) {
      if (event.getEntity() instanceof Player player) {
         if (event.getSource().type().msgId().equals("fall")) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - landingProtectionTime < 500L) {
               // 0.5.5 cancelled here: the jetpack landing protection window suppresses fall damage.
               event.setCanceled(true);
               return;
            }

            if (isUsingExoSuitJetpack(player) && exoSuitFlightEnabled && wasDescending) {
               // 0.5.5 cancelled here: fall damage is voided while flying the exosuit jetpack downwards.
               event.setCanceled(true);
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post event) {
      {
         Minecraft mc = Minecraft.getInstance();
         Player player = mc.player;
         if (player != null && player.level().isClientSide) {
            if (hasExoSuitJetpackSetup(player)) {
               boolean isGamePaused = mc.isPaused();
               if (wasGamePaused && !isGamePaused) {
                  wasGamePaused = false;
                  handleJetpackFlightWithoutParticles(player);
               } else {
                  wasGamePaused = isGamePaused;
                  if (!isGamePaused) {
                     handleJetpackFlight(player);
                  }
               }
            } else {
               if (clientJetpackActive || exoSuitFlightEnabled) {
                  disableExoSuitFlight(player);
                  stopJetpackSounds(player);
               }
            }
         }
      }
   }

   private static void handleJetpackFlightWithoutParticles(Player player) {
      if (!player.isSpectator()) {
         if (!hasExoSuitJetpackSetup(player)) {
            if (clientJetpackActive) {
               disableExoSuitFlight(player);
            }
         } else {
            ExoSuitUpgrade.Effects totalEffects = ExoSuitEffectsHandler.getTotalEffects(player);
            float flightSpeed = totalEffects.getFlightSpeed();
            if (flightSpeed <= 0.0F) {
               flightSpeed = 0.1F;
            }

            if (!player.getAbilities().mayfly && clientJetpackActive) {
               enableExoSuitFlight(player, flightSpeed);
            }

            Minecraft mc = Minecraft.getInstance();
            if (clientJetpackActive && player.getAbilities().mayfly && player.getAbilities().flying && !player.isCreative()) {
               handleFlightMovement(player, mc, flightSpeed);
            }
         }
      }
   }

   private static void enableExoSuitFlight(Player player, float flightSpeed) {
      if (!exoSuitFlightEnabled) {
         originalMayFly = player.getAbilities().mayfly;
         originalFlySpeed = player.getAbilities().getFlyingSpeed();
      }

      player.getAbilities().mayfly = true;
      player.getAbilities().setFlyingSpeed(flightSpeed);
      player.onUpdateAbilities();
      exoSuitFlightEnabled = true;
   }

   private static void disableExoSuitFlight(Player player) {
      if (exoSuitFlightEnabled) {
         player.getAbilities().mayfly = originalMayFly;
         player.getAbilities().flying = false;
         player.getAbilities().setFlyingSpeed(originalFlySpeed);
         player.onUpdateAbilities();
         exoSuitFlightEnabled = false;
      }

      clientJetpackActive = false;
      wasDescending = false;
      if (player.level().isClientSide) {
         stopJetpackSounds(player);
      }
   }

   @OnlyIn(Dist.CLIENT)
   private static void handleFlightMovement(Player player, Minecraft mc, float flightSpeed) {
      if (player.getAbilities().flying && clientJetpackActive && exoSuitFlightEnabled && !player.isSpectator()) {
         if (!hasExoSuitJetpackSetup(player)) {
            disableExoSuitFlight(player);
            stopJetpackSounds(player);
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.5F, 0.6F, false);
         } else {
            Vec3 currentVelocity = player.getDeltaMovement();
            boolean isThrusting = false;
            boolean currentlyDescending = false;
            double newY;
            if (mc.options.keyJump.isDown()) {
               newY = (double)(0.8F * flightSpeed);
               isThrusting = true;
            } else if (mc.options.keyShift.isDown()) {
               newY = (double)(-0.4F * flightSpeed);
               currentlyDescending = true;
               isThrusting = true;
            } else {
               newY = -0.02F;
            }

            long currentTime = System.currentTimeMillis();
            if (currentlyDescending && !player.onGround()) {
               wasDescending = true;
            } else if (player.onGround() && wasDescending) {
               landingProtectionTime = currentTime;
               wasDescending = false;
            } else if (!currentlyDescending && !player.onGround()) {
               wasDescending = false;
            }

            handleJetpackSounds(player, isThrusting);
            double newX = currentVelocity.x;
            double newZ = currentVelocity.z;
            Vec3 inputVector = getMovementInput(player, mc);
            if (inputVector.lengthSqr() > 0.0) {
               double speedMultiplier = 1.0;
               if (player.isSprinting()) {
                  speedMultiplier = 1.15F;
               }

               double maxSpeedForThisTick = (double)(0.04F * Math.min(flightSpeed, 1.0F)) * speedMultiplier;
               newX += inputVector.x * 0.001F;
               newZ += inputVector.z * 0.001F;
               double horizontalSpeed = Math.sqrt(newX * newX + newZ * newZ);
               if (horizontalSpeed > maxSpeedForThisTick) {
                  double ratio = maxSpeedForThisTick / horizontalSpeed;
                  newX *= ratio;
                  newZ *= ratio;
               }

               if (player.getAbilities().flying && !player.onGround()) {
                  isThrusting = true;
               }
            } else {
               newX *= 0.85F;
               newZ *= 0.85F;
               if (Math.abs(newX) < 0.005F) {
                  newX = 0.0;
               }

               if (Math.abs(newZ) < 0.005F) {
                  newZ = 0.0;
               }
            }

            sendThrustStateToServer(isThrusting);
            player.setDeltaMovement(newX, newY, newZ);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   private static void sendThrustStateToServer(boolean thrusting) {
      PacketHandler.getPlayChannel().sendToServer(new C2SMessageJetpackThrust(thrusting));
   }

   @OnlyIn(Dist.CLIENT)
   private static void handleJetpackSounds(Player player, boolean isThrusting) {
      long currentTime = System.currentTimeMillis();
      if (clientJetpackActive && (!isLoopSoundPlaying || currentTime - lastLoopSoundTime >= 3000L)) {
         player.level()
            .playLocalSound(player.getX(), player.getY(), player.getZ(), (SoundEvent)ModSounds.JETPACK_LOOP.get(), SoundSource.PLAYERS, 0.5F, 1.0F, false);
         lastLoopSoundTime = currentTime;
         isLoopSoundPlaying = true;
      }

      if (isThrusting && !wasThrustingLastTick && currentTime - lastThrustSoundTime >= 300L) {
         player.level()
            .playLocalSound(
               player.getX(),
               player.getY(),
               player.getZ(),
               (SoundEvent)ModSounds.JETPACK.get(),
               SoundSource.PLAYERS,
               0.7F,
               1.0F + (player.level().random.nextFloat() * 0.1F - 0.05F),
               false
            );
         lastThrustSoundTime = currentTime;
      }

      wasThrustingLastTick = isThrusting;
   }

   private static void handleJetpackFlight(Player player) {
      if (!player.isSpectator()) {
         if (!hasExoSuitJetpackSetup(player)) {
            if (clientJetpackActive || exoSuitFlightEnabled) {
               disableExoSuitFlight(player);
            }
         } else if (!canJetpackFunction(player)) {
            if (clientJetpackActive || exoSuitFlightEnabled) {
               disableExoSuitFlight(player);
            }
         } else {
            ExoSuitUpgrade.Effects totalEffects = ExoSuitEffectsHandler.getTotalEffects(player);
            float flightSpeed = totalEffects.getFlightSpeed();
            if (flightSpeed <= 0.0F) {
               flightSpeed = 0.1F;
            }

            Minecraft mc = Minecraft.getInstance();
            boolean jumpPressed = mc.options.keyJump.isDown();
            long currentTime = System.currentTimeMillis();
            if (jumpPressed && !wasJumpPressed) {
               long timeSinceLastPress = currentTime - lastJumpPressTime;
               if (timeSinceLastPress <= 300L && timeSinceLastPress > 50L) {
                  if (ExoSuitPowerManager.isPowerEnabled(player, "utility")) {
                     clientJetpackActive = !clientJetpackActive;
                     sendJetpackStateToServer(clientJetpackActive);
                     if (clientJetpackActive) {
                        enableExoSuitFlight(player, flightSpeed);
                        player.getAbilities().flying = true;
                        player.onUpdateAbilities();
                     } else {
                        disableExoSuitFlight(player);
                     }
                  }

                  lastJumpPressTime = 0L;
               } else {
                  lastJumpPressTime = currentTime;
               }
            }

            wasJumpPressed = jumpPressed;
            if (clientJetpackActive && exoSuitFlightEnabled && player.getAbilities().mayfly && player.getAbilities().flying && !player.isCreative()) {
               handleFlightMovement(player, mc, flightSpeed);
               spawnJetpackParticles(player, mc);
               Minecraft mcInstance = Minecraft.getInstance();
               boolean isThrusting = mcInstance.options.keyJump.isDown();
               handleJetpackSounds(player, isThrusting);
            }
         }
      }
   }

   private static boolean canJetpackFunction(Player player) {
      if (!ExoSuitPowerManager.isPowerEnabled(player, "utility")) {
         return false;
      } else {
         return !ExoSuitPowerManager.canUpgradeFunction(player, "utility") ? false : hasJetpackModule(player);
      }
   }

   @OnlyIn(Dist.CLIENT)
   private static void sendJetpackStateToServer(boolean active) {
      PacketHandler.getPlayChannel().sendToServer(new C2SMessageJetpackState(active));
   }

   @OnlyIn(Dist.CLIENT)
   private static void stopJetpackSounds(Player player) {
      isLoopSoundPlaying = false;
      wasThrustingLastTick = false;
      lastLoopSoundTime = 0L;
   }

   @OnlyIn(Dist.CLIENT)
   private static void spawnJetpackParticles(Player player, Minecraft mc) {
      Level level = player.level();
      float yaw = player.getYRot() * (float) (Math.PI / 180.0);
      double backwardX = -Math.sin((double)yaw);
      double backwardZ = Math.cos((double)yaw);
      double sideX = -backwardZ;
      double baseX = player.getX() + backwardX * -0.5;
      double baseY = player.getY() + 0.7;
      double baseZ = player.getZ() + backwardZ * -0.5;
      double leftJetX = baseX + sideX * 0.4;
      double leftJetZ = baseZ + backwardX * 0.4;
      double rightJetX = baseX - sideX * 0.4;
      double rightJetZ = baseZ - backwardX * 0.4;
      spawnJetParticles(level, leftJetX, baseY, leftJetZ, player);
      spawnJetParticles(level, rightJetX, baseY, rightJetZ, player);
   }

   @OnlyIn(Dist.CLIENT)
   private static void spawnJetParticles(Level level, double x, double y, double z, Player player) {
      for (int i = 0; i < 1; i++) {
         double offsetX = (level.random.nextDouble() - 0.5) * 0.2;
         double offsetY = (level.random.nextDouble() - 0.5) * 0.1;
         double offsetZ = (level.random.nextDouble() - 0.5) * 0.2;
         double velocityX = (level.random.nextDouble() - 0.5) * 0.1;
         double velocityY = -0.1 - level.random.nextDouble() * 0.2;
         double velocityZ = (level.random.nextDouble() - 0.5) * 0.2;
         level.addParticle(ParticleTypes.FLAME, x + offsetX, y + offsetY, z + offsetZ, velocityX, velocityY, velocityZ);
         if (level.random.nextFloat() < 0.3F) {
            level.addParticle(ParticleTypes.SMOKE, x + offsetX, y + offsetY - 0.1, z + offsetZ, velocityX * 0.5, velocityY * 0.5, velocityZ * 0.5);
         }
      }
   }

   // This class is subscribed on both dists (modid only), and NeoForge resolves every
   // declared method signature while auto-registering it. A `Minecraft` parameter here
   // therefore made the dedicated server try to load a client-only class and abort:
   // "Attempted to load class net/minecraft/client/Minecraft for invalid dist
   // DEDICATED_SERVER". Every other client-typed method in this file is marked
   // @OnlyIn(Dist.CLIENT) (stripped on the server); this one was missed.
   @OnlyIn(Dist.CLIENT)
   private static Vec3 getMovementInput(Player player, Minecraft mc) {
      float forward = 0.0F;
      float strafe = 0.0F;
      if (mc.options.keyUp.isDown()) {
         forward++;
      }

      if (mc.options.keyDown.isDown()) {
         forward--;
      }

      if (mc.options.keyLeft.isDown()) {
         strafe++;
      }

      if (mc.options.keyRight.isDown()) {
         strafe--;
      }

      if (forward != 0.0F && strafe != 0.0F) {
         forward *= 0.707F;
         strafe *= 0.707F;
      }

      if (forward == 0.0F && strafe == 0.0F) {
         return Vec3.ZERO;
      } else {
         float yaw = player.getYRot() * (float) (Math.PI / 180.0);
         double x = (double)strafe * Math.cos((double)yaw) - (double)forward * Math.sin((double)yaw);
         double z = (double)forward * Math.cos((double)yaw) + (double)strafe * Math.sin((double)yaw);
         return new Vec3(x, 0.0, z);
      }
   }

   public static void onPlayerLogout(Player player) {
      UUID playerId = player.getUUID();
      lastEnergyConsumptionTime.remove(playerId);
      serverJetpackStates.remove(playerId);
      playerThrustStates.remove(playerId);
      lastThrustTime.remove(playerId);
   }
}
