package top.ribs.scguns.entity.raid;


import top.ribs.scguns.util.NbtHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.LevelEvent.Load;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.Config;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.config.GunnerMobSpawner;
import top.ribs.scguns.config.RaidConfig;
import top.ribs.scguns.entity.ai.GunAttackGoal;
import top.ribs.scguns.entity.player.PlayerGunProgression;
import top.ribs.scguns.item.GunItem;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME
)
public class RaidManager {
   private static final ResourceLocation BOSS_HEALTH_MODIFIER_UUID = ResourceLocation.fromNamespaceAndPath("scguns", "a1b2c3d4-e5f6-7890-abcd-ef1234567890");
   private static final ResourceLocation MOUNT_HEALTH_MODIFIER_UUID = ResourceLocation.fromNamespaceAndPath("scguns", "b2c3d4e5-f6a7-8901-bcde-f12345678901");
   private static final long NIGHT_START = 13000L;
   private static final long RAID_SPAWN_TIME = 18000L;
   private static final int SAVE_INTERVAL = 100;
   private static final Map<ResourceLocation, RaidManager> INSTANCES = new HashMap<>();
   private UUID currentActiveRaidId = null;
   private static final Map<UUID, ActiveRaid> activeRaids = new HashMap<>();
   private boolean needsRestore = true;

   public RaidManager() {
      super();
   }

   public static RaidManager get(ServerLevel level) {
      ResourceLocation dimension = level.dimension().location();
      return INSTANCES.computeIfAbsent(dimension, k -> new RaidManager());
   }

   public boolean hasActiveRaid() {
      if (this.currentActiveRaidId == null) {
         return false;
      } else {
         ActiveRaid raid = activeRaids.get(this.currentActiveRaidId);
         return raid != null && raid.isActive();
      }
   }

   public static boolean hasActiveRaidInDimension(ServerLevel level) {
      RaidManager manager = get(level);
      return manager.hasActiveRaid();
   }

   @Nullable
   public ActiveRaid getCurrentActiveRaid() {
      return this.currentActiveRaidId == null ? null : activeRaids.get(this.currentActiveRaidId);
   }

   public static void surrenderRaid(ServerLevel level) {
      RaidManager manager = get(level);
      ActiveRaid raid = manager.getCurrentActiveRaid();
      if (raid != null && raid.isActive()) {
         LivingEntity boss = raid.getBoss();
         if (boss != null && boss.isAlive()) {
            boss.discard();
         }

         Entity mount = raid.getMount();
         if (mount != null && mount.isAlive()) {
            mount.discard();
         }

         for (UUID henchmanUUID : raid.getHenchmenUUIDs()) {
            Entity henchman = level.getEntity(henchmanUUID);
            if (henchman != null && henchman.isAlive()) {
               henchman.discard();
            }
         }

         raid.announceToNearbyPlayers(Component.translatable("raid.scguns.surrendered").withStyle(ChatFormatting.YELLOW), 64.0);
         if (raid.getBossBar() != null) {
            raid.getBossBar().setVisible(false);
            raid.getBossBar().removeAllPlayers();
         }

         raid.setActive(false);
         manager.currentActiveRaidId = null;
         activeRaids.remove(raid.getRaidId());
         RaidSaveData saveData = RaidSaveData.get(level);
         saveData.removeActiveRaid(raid.getRaidId());
      }
   }

   @SubscribeEvent
   public static void onLevelLoad(Load event) {
      if (event.getLevel() instanceof ServerLevel serverLevel && serverLevel == serverLevel.getServer().overworld()) {
         RaidManager manager = get(serverLevel);
         if (manager.needsRestore) {
            manager.restoreRaidsFromSave(serverLevel);
            manager.needsRestore = false;
         }
      }
   }

   private void restoreRaidsFromSave(ServerLevel level) {
      RaidSaveData saveData = RaidSaveData.get(level);
      Collection<RaidSaveData.ActiveRaidData> savedRaids = saveData.getActiveRaidData();
      saveData.cleanupInvalidRaids(level);

      for (RaidSaveData.ActiveRaidData data : savedRaids) {
         RaidConfig.RaidData config = RaidConfig.getRaidById(data.configRaidId());
         if (config != null) {
            ActiveRaid raid = ActiveRaid.restore(data, config, level);
            activeRaids.put(raid.getRaidId(), raid);
            if (this.currentActiveRaidId == null && raid.isActive()) {
               this.currentActiveRaidId = raid.getRaidId();
            }

            raid.updateBossBarPlayers();
         }
      }
   }

   public void startRaidFromPlayer(RaidConfig.RaidData config, ServerLevel level, ServerPlayer player) {
      if (!this.hasActiveRaid()) {
         Vec3 playerPos = player.position();
         Vec3 spawnPos = this.findRaidSpawnLocation(level, playerPos);
         if (spawnPos != null) {
            this.startRaid(config, level, spawnPos);
         } else {
            // The player asked for this one, so say why nothing happened: a raid only ever starts on open
            // ground (HANDOFF sections 76 and 78), which a roofed dimension like the Nether has none of.
            player.displayClientMessage(
               Component.translatable("raid.scguns.no_surface").withStyle(ChatFormatting.RED), false);
         }
      }
   }

   @SubscribeEvent
   public static void onLevelTick(LevelTickEvent.Post event) {
      {
         if (event.getLevel() instanceof ServerLevel serverLevel) {
            RaidManager manager = get(serverLevel);
            manager.tick(serverLevel);
         }
      }
   }

   public void tick(ServerLevel level) {
      this.tickActiveRaids(level);
      if ((Boolean)Config.COMMON.raids.raidsEnabled.get()) {
         this.checkForNightlyRaidSpawn(level);
         if (level.getGameTime() % 100L == 0L) {
            this.saveActiveRaids(level);
            level.getDataStorage().save();
         }
      }
   }

   private void tickActiveRaids(ServerLevel level) {
      Iterator<Entry<UUID, ActiveRaid>> iterator = activeRaids.entrySet().iterator();

      while (iterator.hasNext()) {
         Entry<UUID, ActiveRaid> entry = iterator.next();
         ActiveRaid raid = entry.getValue();
         if (!raid.isActive()) {
            if (raid.getRaidId().equals(this.currentActiveRaidId)) {
               this.currentActiveRaidId = null;
            }

            RaidSaveData saveData = RaidSaveData.get(level);
            saveData.removeActiveRaid(raid.getRaidId());
            iterator.remove();
         } else {
            raid.tick();
            if (raid.shouldSpawnHenchmen()) {
               this.spawnHenchmen(raid, level);
               raid.resetSpawnTimer();
            }
         }
      }
   }

   private void saveActiveRaids(ServerLevel level) {
      RaidSaveData saveData = RaidSaveData.get(level);

      for (ActiveRaid raid : activeRaids.values()) {
         if (raid.isActive()) {
            saveData.saveActiveRaid(raid);
         }
      }
   }

   private void checkForNightlyRaidSpawn(ServerLevel level) {
      if (!level.dimensionType().hasFixedTime()) {
         ResourceLocation dimension = level.dimension().location();
         long dayTime = level.getDayTime() % 24000L;
         long currentDay = level.getDayTime() / 24000L;
         RaidSaveData saveData = RaidSaveData.get(level);
         if (dayTime >= 13000L && dayTime < 13020L) {
            if (this.hasActiveRaid()) {
               return;
            }

            RaidSaveData.ScheduledRaidData scheduled = saveData.getScheduledRaid(dimension);
            if (scheduled != null && scheduled.scheduledDay() < currentDay) {
               saveData.removeScheduledRaid(dimension);
               scheduled = null;
            }

            if (scheduled != null) {
               return;
            }

            if (!(Boolean)Config.COMMON.raids.raidsEnabled.get()) {
               return;
            }

            if (dayTime == 13000L) {
               float raidChance = ((Double)Config.COMMON.raids.nightlyRaidChance.get()).floatValue();
               float roll = level.random.nextFloat();
               if (roll < raidChance) {
                  this.scheduleRaidForTonight(level, dimension, currentDay, saveData);
               }
            }
         }

         if (dayTime >= 18000L && dayTime < 18020L) {
            if (this.hasActiveRaid()) {
               return;
            }

            RaidSaveData.ScheduledRaidData scheduledx = saveData.getScheduledRaid(dimension);
            if (scheduledx == null) {
               return;
            }

            if (scheduledx.scheduledDay() != currentDay) {
               saveData.removeScheduledRaid(dimension);
               return;
            }

            ServerPlayer player = level.getServer().getPlayerList().getPlayer(scheduledx.targetPlayerUUID());
            if (player == null || player.isRemoved() || player.isSpectator()) {
               saveData.removeScheduledRaid(dimension);
               return;
            }

            // No natural raid for a player below sea level or under a roof (HANDOFF section 78): they are
            // mining or sheltering, and a raid on the surface above them is nothing but a lost boss. This
            // is the same rule vanilla applies to phantoms. The schedule is dropped for tonight (the next
            // night rolls again); a raid flare still works, because that is the player asking for one.
            if (!canGetNaturalRaid(level, player.position())) {
               saveData.removeScheduledRaid(dimension);
               return;
            }

            Vec3 playerPos = player.position();
            Vec3 spawnPos = this.findRaidSpawnLocation(level, playerPos);
            if (spawnPos == null) {
               return;
            }

            RaidConfig.RaidData config = RaidConfig.getRaidById(scheduledx.raidId());
            if (config == null) {
               saveData.removeScheduledRaid(dimension);
               return;
            }

            this.startRaid(config, level, spawnPos);
            saveData.removeScheduledRaid(dimension);
         }
      }
   }

   private void scheduleRaidForTonight(ServerLevel level, ResourceLocation dimension, long currentDay, RaidSaveData saveData) {
      if (!this.hasActiveRaid()) {
         List<ServerPlayer> validPlayers = new ArrayList<>();

         for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && !player.isCreative()) {
               validPlayers.add(player);
            }
         }

         if (!validPlayers.isEmpty()) {
            ServerPlayer targetPlayer = validPlayers.get(level.random.nextInt(validPlayers.size()));
            PlayerGunProgression progression = PlayerGunProgression.get(targetPlayer);
            int raidLevel = progression.getCurrentRaidLevel();
            if (raidLevel != 0) {
               RaidConfig.RaidData selectedRaid = this.selectRaidForLevel(raidLevel, level.random);
               if (selectedRaid != null) {
                  saveData.scheduleRaid(dimension, targetPlayer, selectedRaid.raidId(), currentDay);
                  targetPlayer.sendSystemMessage(Component.translatable("raid.scguns.warning"));
               }
            }
         }
      }
   }

   @Nullable
   private RaidConfig.RaidData selectRaidForLevel(int playerRaidLevel, RandomSource random) {
      List<RaidConfig.RaidData> availableRaids = RaidConfig.getRaidsForLevel(playerRaidLevel);
      if (availableRaids.isEmpty()) {
         return null;
      } else if (availableRaids.size() == 1) {
         return availableRaids.get(0);
      } else {
         List<RaidConfig.RaidData> highestLevelRaids = RaidConfig.getRaidsAtLevel(playerRaidLevel);
         float roll = random.nextFloat();
         return !highestLevelRaids.isEmpty() && roll < 0.6F
            ? highestLevelRaids.get(random.nextInt(highestLevelRaids.size()))
            : availableRaids.get(random.nextInt(availableRaids.size()));
      }
   }

   /**
    * Whether a **natural** (nightly) raid may start for this player (HANDOFF section 78).
    *
    * <p>One condition only: **the player is at or above sea level**. Below it - mining, in a cave, diving
    * - the automatic raid leaves them alone. Everything else is deliberately **differentiated from
    * phantoms**: vanilla's {@code PlayerSpawnPhantomsEvent#shouldSpawnPhantoms} also requires
    * {@code level.canSeeSky(pos)}, so a player indoors or under a tree never gets phantoms, whereas here a
    * roof over the player's head must not stop a raid - the raid is placed on the surface outside and the
    * player walks out to it.</p>
    *
    * <p>Two earlier attempts are worth remembering: an invented "underground = eight blocks below the
    * column's heightmap" test, which called a player inside a house underground because their roof raises
    * the heightmap, and then the phantom rule verbatim, whose sky check did exactly the same thing.</p>
    */
   public static boolean canGetNaturalRaid(ServerLevel level, Vec3 position) {
      return position.y >= (double)level.getSeaLevel();
   }

   /**
    * Where a raid spawns: **on the surface**, near the player (HANDOFF sections 75, 76 and 78).
    *
    * <p>0.5.5 decided "surface or cave" with {@code playerY < 50} and, when that said underground, walked
    * a cave search from -5 upwards - so a player standing in the open at y&lt;50 (a canyon floor, a deep
    * valley, diving in an ocean) got the raid placed in a cave below them, and a raid aimed at a player
    * who was mining turned up in a cave pocket they could not find.</p>
    *
    * <p>The rule is now simply the surface: each candidate column is asked for its own ground level (the
    * heightmap, which can never point into a cave - a cave ceiling is itself motion blocking) and that
    * has to be standable and open to the sky. <b>No dimension gets an exception</b>: a dimension whose
    * "ground" is a roof (the Nether's bedrock ceiling) has no open surface at all, and a raid there is
    * refused rather than dumped on the roof. Nothing else in this class places mobs at the player's own
    * level any more.</p>
    *
    * <p>Public because {@code /scguns raid check} reports the very spot this method picks (HANDOFF
    * section 79): a tester standing under a roof, or in a dimension whose floor is a bedrock ceiling,
    * gets no raid at all and cannot tell that refusal apart from a failed dice roll, so the command
    * runs this same search instead of reimplementing it.</p>
    */
   @Nullable
   public Vec3 findRaidSpawnLocation(ServerLevel level, Vec3 center) {
      RandomSource random = level.getRandom();

      for (int attempt = 0; attempt < 15; attempt++) {
         double angle = random.nextDouble() * Math.PI * 2.0;
         double distance = 25.0 + random.nextDouble() * 15.0;
         int x = (int)(center.x + Math.cos(angle) * distance);
         int z = (int)(center.z + Math.sin(angle) * distance);
         BlockPos candidate = this.findSurfaceSpawn(level, BlockPos.containing(x, center.y, z));
         if (candidate != null) {
            return new Vec3((double)candidate.getX() + 0.5, (double)candidate.getY(), (double)candidate.getZ() + 0.5);
         }
      }

      return null;
   }

   /**
    * The column's own ground level, which has to be standable and open to the sky. A covered spot (under
    * a leaf canopy, an overhang, a roof, or a dimension's bedrock ceiling) is rejected because the player
    * would not see the raid there.
    */
   @Nullable
   private BlockPos findSurfaceSpawn(ServerLevel level, BlockPos column) {
      BlockPos ground = level.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, column);
      return this.isStandableSpawn(level, ground) && level.canSeeSky(ground) ? ground : null;
   }

   /** Solid floor, the position and the two blocks above it free - a mob fits. */
   private boolean isStandableSpawn(ServerLevel level, BlockPos pos) {
      return level.getBlockState(pos.below()).isSolid()
         && level.getBlockState(pos).isAir()
         && level.getBlockState(pos.above()).isAir()
         && level.getBlockState(pos.above(2)).isAir();
   }

   public void startRaid(RaidConfig.RaidData config, ServerLevel level, Vec3 spawnPos) {
      if (!this.hasActiveRaid()) {
         ServerPlayer targetPlayer = this.findNearestPlayer(level, spawnPos);
         long startTime = level.getGameTime();
         Integer raidLevel = config.raidLevel();
         ActiveRaid raid = new ActiveRaid(raidLevel, config, level, spawnPos, startTime);
         if (targetPlayer != null) {
            raid.setTargetPlayer(targetPlayer.getUUID());
         }

         Mob boss = this.spawnBoss(raid, level, spawnPos);
         if (boss != null) {
            raid.setBossUUID(boss.getUUID());
            if (targetPlayer != null && boss instanceof PathfinderMob pathfinder) {
               pathfinder.setTarget(targetPlayer);
            }

            if (config.boss().mount() != null) {
               Entity mount = boss.getVehicle();
               if (mount != null) {
                  raid.setMountUUID(mount.getUUID());
               }
            }

            raid.setBossConfirmed(true);
            activeRaids.put(raid.getRaidId(), raid);
            this.currentActiveRaidId = raid.getRaidId();
            String announcement = config.spawnConditions().announcementMessage();
            Component announcementComponent;
            if (announcement.startsWith("translation:")) {
               String translationKey = announcement.substring(12);
               announcementComponent = Component.translatable(translationKey);
            } else {
               announcementComponent = Component.literal(announcement);
            }

            raid.announceToNearbyPlayers(announcementComponent, (double)config.spawnConditions().searchRadius());
            this.spawnHenchmen(raid, level);
            raid.resetSpawnTimer();
            RaidSaveData saveData = RaidSaveData.get(level);
            saveData.saveActiveRaid(raid);
         }
      }
   }

   @Nullable
   private ServerPlayer findNearestPlayer(ServerLevel level, Vec3 pos) {
      ServerPlayer nearest = null;
      double nearestDist = Double.MAX_VALUE;

      for (ServerPlayer player : level.players()) {
         if (!player.isSpectator() && !player.isCreative()) {
            double dist = player.position().distanceTo(pos);
            if (dist < nearestDist) {
               nearestDist = dist;
               nearest = player;
            }
         }
      }

      return nearest;
   }

   @Nullable
   private Mob spawnBoss(ActiveRaid raid, ServerLevel level, Vec3 spawnPos) {
      RaidConfig.BossData bossData = raid.getConfig().boss();
      EntityType<?> entityType = bossData.entityType();
      if (entityType.create(level) instanceof Mob boss) {
         boss.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
         if (bossData.customName() != null) {
            Component nameComponent;
            if (bossData.customName().startsWith("translation:")) {
               String translationKey = bossData.customName().substring(12);
               nameComponent = Component.translatable(translationKey);
            } else {
               nameComponent = Component.literal(bossData.customName());
            }

            boss.setCustomName(nameComponent);
            boss.setCustomNameVisible(true);
         }

         this.applyHealthConfig(boss, bossData.healthConfig(), BOSS_HEALTH_MODIFIER_UUID);
         this.applyEffects(boss, bossData.effects());
         if (bossData.weapon() != null) {
            ItemStack weaponStack = this.createModifiedGun(boss, bossData.weapon().item());
            if (bossData.weapon().nbt() != null) {
               CompoundTag existingTag = NbtHelper.getOrCreateTag(weaponStack);
               existingTag.merge(bossData.weapon().nbt());
            }

            boss.setItemSlot(EquipmentSlot.MAINHAND, weaponStack);
            boss.setDropChance(EquipmentSlot.MAINHAND, bossData.weapon().dropChance());
         }

         for (RaidConfig.ArmorEntry armorEntry : bossData.armor()) {
            String targetPlayer = armorEntry.slot();

            EquipmentSlot slot = switch (targetPlayer) {
               case "head" -> EquipmentSlot.HEAD;
               case "chest" -> EquipmentSlot.CHEST;
               case "legs" -> EquipmentSlot.LEGS;
               case "feet" -> EquipmentSlot.FEET;
               default -> null;
            };
            if (slot != null) {
               ItemStack armorStack = new ItemStack(armorEntry.item());
               if (armorEntry.nbt() != null) {
                  NbtHelper.setTag(armorStack, armorEntry.nbt().copy());
               }

               boss.setItemSlot(slot, armorStack);
               boss.setDropChance(slot, armorEntry.dropChance());
            }
         }

         boss.addTag("RaidBoss");
         boss.addTag("RaidMember_" + raid.getRaidId());
         boss.addTag("MobGunner");
         boss.addTag("AI_" + bossData.aiType().name());
         boss.setPersistenceRequired();
         if (boss instanceof PathfinderMob pathfinderBoss) {
            ItemStack heldItem = boss.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem) {
               pathfinderBoss.goalSelector.addGoal(2, new GunAttackGoal<>(pathfinderBoss, heldItem, 1.2F, bossData.aiType(), bossData.aiDifficulty()));
            }

            GunnerMobSpawner.extendFollowRange(pathfinderBoss);
            if (boss instanceof AbstractPiglin abstractPiglin) {
               abstractPiglin.setImmuneToZombification(true);
               if (boss instanceof Piglin piglin) {
                  piglin.setAggressive(true);
               }

               ServerPlayer targetPlayer = this.findNearestPlayer(level, spawnPos);
               if (targetPlayer != null) {
                  try {
                     Brain<?> brain = abstractPiglin.getBrain();
                     brain.eraseMemory(MemoryModuleType.ANGRY_AT);
                     brain.setMemory(MemoryModuleType.ANGRY_AT, targetPlayer.getUUID());
                     brain.eraseMemory(MemoryModuleType.UNIVERSAL_ANGER);
                     brain.setMemory(MemoryModuleType.UNIVERSAL_ANGER, true);
                     brain.setMemory(MemoryModuleType.ATTACK_TARGET, targetPlayer);
                     brain.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
                     brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER, targetPlayer);
                     abstractPiglin.setTarget(targetPlayer);
                     abstractPiglin.setLastHurtByMob(targetPlayer);
                  } catch (Exception var12) {
                     abstractPiglin.setTarget(targetPlayer);
                  }
               }
            }
         }

         level.addFreshEntity(boss);
         RaidConfig.MountData mountData = bossData.mount();
         if (mountData != null) {
            Mob mount = this.spawnMount(raid, mountData, level, spawnPos);
            if (mount != null) {
               boss.startRiding(mount, true);
            }
         }

         return boss;
      } else {
         return null;
      }
   }

   @Nullable
   private Mob spawnMount(ActiveRaid raid, RaidConfig.MountData mountData, ServerLevel level, Vec3 spawnPos) {
      EntityType<?> mountType = mountData.entityType();
      if (mountType.create(level) instanceof Mob mount) {
         mount.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
         this.applyHealthConfig(mount, mountData.healthConfig(), MOUNT_HEALTH_MODIFIER_UUID);
         this.applyEffects(mount, mountData.effects());

         for (RaidConfig.ArmorEntry armorEntry : mountData.armor()) {
            String armorSlotName = armorEntry.slot();

            EquipmentSlot slot = switch (armorSlotName) {
               case "head" -> EquipmentSlot.HEAD;
               case "chest" -> EquipmentSlot.CHEST;
               case "legs" -> EquipmentSlot.LEGS;
               case "feet" -> EquipmentSlot.FEET;
               default -> null;
            };
            if (slot != null) {
               ItemStack armorStack = new ItemStack(armorEntry.item());
               if (armorEntry.nbt() != null) {
                  NbtHelper.setTag(armorStack, armorEntry.nbt().copy());
               }

               mount.setItemSlot(slot, armorStack);
               mount.setDropChance(slot, armorEntry.dropChance());
            }
         }

         mount.addTag("RaidMount");
         mount.addTag("RaidMember_" + raid.getRaidId());
         mount.addTag("MobGunner");
         mount.setPersistenceRequired();
         if (!mountData.mountDropsLoot()) {
            mount.addTag("NoLootDrop");
         }

         level.addFreshEntity(mount);
         return mount;
      } else {
         return null;
      }
   }

   private void spawnHenchmen(ActiveRaid raid, ServerLevel level) {
      RaidConfig.HenchmenData henchmenData = raid.getConfig().henchmen();
      LivingEntity boss = raid.getBoss();
      if (boss != null && boss.isAlive()) {
         Vec3 bossPos = boss.position();

         for (int i = 0; i < henchmenData.spawnAttemptsPerWave() && raid.canSpawnMoreHenchmen(); i++) {
            RaidConfig.HenchmanType type = henchmenData.selectRandomType(level.getRandom());
            if (type != null) {
               Vec3 spawnPos = this.findHenchmanSpawnPos(level, bossPos, henchmenData.spawnRadius());
               if (spawnPos != null) {
                  Mob henchman = this.spawnHenchman(raid, type, level, spawnPos);
                  if (henchman != null) {
                     raid.addHenchman(henchman.getUUID());
                  }
               }
            }
         }
      }
   }

   @Nullable
   private Vec3 findHenchmanSpawnPos(ServerLevel level, Vec3 center, int radius) {
      RandomSource random = level.getRandom();

      for (int attempt = 0; attempt < 10; attempt++) {
         double angle = random.nextDouble() * Math.PI * 2.0;
         double distance = random.nextDouble() * (double)radius;
         double x = center.x + Math.cos(angle) * distance;
         double z = center.z + Math.sin(angle) * distance;
         // Same rule as the raid itself: the surface, open to the sky (HANDOFF section 76).
         BlockPos groundPos = this.findSurfaceSpawn(level, BlockPos.containing(x, center.y, z));
         if (groundPos != null) {
            return new Vec3((double)groundPos.getX() + 0.5, (double)groundPos.getY(), (double)groundPos.getZ() + 0.5);
         }
      }

      return null;
   }

   private Mob spawnHenchman(ActiveRaid raid, RaidConfig.HenchmanType type, ServerLevel level, Vec3 spawnPos) {
      EntityType<?> entityType = type.entityType();
      if (entityType.create(level) instanceof Mob henchman) {
         henchman.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
         this.applyHealthConfig(henchman, type.healthConfig(), ResourceLocation.fromNamespaceAndPath("scguns", UUID.randomUUID().toString()));
         this.applyEffects(henchman, type.effects());
         henchman.addTag("RaidHenchman");
         henchman.addTag("RaidMember_" + raid.getRaidId());
         henchman.addTag("AI_" + type.aiType().name());
         henchman.setPersistenceRequired();
         if (!type.weapons().isEmpty()) {
            Item weaponItem = type.weapons().get(level.random.nextInt(type.weapons().size()));
            ItemStack weaponStack = this.createModifiedGun(henchman, weaponItem);
            henchman.setItemSlot(EquipmentSlot.MAINHAND, weaponStack);
            henchman.setDropChance(EquipmentSlot.MAINHAND, 0.05F);
         }

         henchman.addTag("MobGunner");

         for (RaidConfig.ArmorEntry armorEntry : type.armor()) {
            if (!(level.random.nextFloat() > armorEntry.dropChance())) {
               String abstractPiglin = armorEntry.slot();

               EquipmentSlot slot = switch (abstractPiglin) {
                  case "head" -> EquipmentSlot.HEAD;
                  case "chest" -> EquipmentSlot.CHEST;
                  case "legs" -> EquipmentSlot.LEGS;
                  case "feet" -> EquipmentSlot.FEET;
                  default -> null;
               };
               if (slot != null) {
                  ItemStack armorStack = new ItemStack(armorEntry.item());
                  if (armorEntry.nbt() != null) {
                     NbtHelper.setTag(armorStack, armorEntry.nbt().copy());
                  }

                  henchman.setItemSlot(slot, armorStack);
                  henchman.setDropChance(slot, 0.05F);
               }
            }
         }

         if (henchman instanceof PathfinderMob pathfinderMob) {
            ItemStack heldItem = henchman.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem) {
               pathfinderMob.goalSelector.addGoal(2, new GunAttackGoal<>(pathfinderMob, heldItem, 1.2F, type.aiType(), type.aiDifficulty()));
            }

            GunnerMobSpawner.extendFollowRange(pathfinderMob);
            ServerPlayer targetPlayer = raid.getTargetPlayer(level);
            if (targetPlayer != null) {
               pathfinderMob.setTarget(targetPlayer);
            }

            if (henchman instanceof AbstractPiglin abstractPiglin) {
               abstractPiglin.setImmuneToZombification(true);
               if (henchman instanceof Piglin piglin) {
                  piglin.setAggressive(true);
               }

               if (targetPlayer != null) {
                  try {
                     Brain<?> brain = abstractPiglin.getBrain();
                     brain.eraseMemory(MemoryModuleType.ANGRY_AT);
                     brain.setMemory(MemoryModuleType.ANGRY_AT, targetPlayer.getUUID());
                     brain.eraseMemory(MemoryModuleType.UNIVERSAL_ANGER);
                     brain.setMemory(MemoryModuleType.UNIVERSAL_ANGER, true);
                     brain.setMemory(MemoryModuleType.ATTACK_TARGET, targetPlayer);
                     brain.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
                     brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER, targetPlayer);
                     abstractPiglin.setTarget(targetPlayer);
                     abstractPiglin.setLastHurtByMob(targetPlayer);
                  } catch (Exception var12) {
                     abstractPiglin.setTarget(targetPlayer);
                  }
               }
            }
         }

         level.addFreshEntity(henchman);
         return henchman;
      } else {
         return null;
      }
   }

   private ItemStack createModifiedGun(Mob mob, Item gun) {
      ItemStack gunStack = new ItemStack(gun);
      // A freshly built ItemStack has no custom data, so guarding on
      // getTag(...) != null skipped this every single time and left the raider
      // holding a gun with no AmmoCount. GunAttackGoal then read that missing
      // tag and crashed the server with a NullPointerException. (0.5.5 had the
      // same guard, so the bug is inherited, not introduced by the port.)
      if (gun instanceof GunItem gunItem) {
         Gun gunModified = gunItem.getModifiedGun(gunStack);
         NbtHelper.getOrCreateTag(gunStack)
            .putInt("AmmoCount", mob.getRandom().nextInt(gunModified.getReloads().getMaxAmmo()));
      }

      return gunStack;
   }

   private void applyHealthConfig(Mob mob, RaidConfig.HealthConfig healthConfig, ResourceLocation modifierUUID) {
      AttributeInstance healthAttr = mob.getAttribute(Attributes.MAX_HEALTH);
      if (healthAttr != null) {
         if (healthConfig.useMultiplier()) {
            healthAttr.addPermanentModifier(
               new AttributeModifier(modifierUUID, (double)healthConfig.healthMultiplier().floatValue() - 1.0, Operation.ADD_MULTIPLIED_BASE)
            );
         } else {
            double currentHealth = healthAttr.getBaseValue();
            healthAttr.addPermanentModifier(
               new AttributeModifier(modifierUUID, (double)healthConfig.fixedHealth().floatValue() - currentHealth, Operation.ADD_VALUE)
            );
         }

         mob.setHealth(mob.getMaxHealth());
      }
   }

   private void applyEffects(Mob mob, List<RaidConfig.EffectEntry> effects) {
      for (RaidConfig.EffectEntry effectEntry : effects) {
         mob.addEffect(
            new MobEffectInstance(
               BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effectEntry.effect()),
               effectEntry.duration(),
               effectEntry.amplifier(),
               effectEntry.ambient(),
               effectEntry.visible()
            )
         );
      }
   }

   @SubscribeEvent
   public static void onEntityDeath(LivingDeathEvent event) {
      if (event.getEntity().level() instanceof ServerLevel level) {
         Entity entity = event.getEntity();
         if (entity instanceof Mob) {
            Mob mob = (Mob)entity;
            RaidManager manager = get(level);

            for (ActiveRaid raid : activeRaids.values()) {
               if (mob.getUUID().equals(raid.getBossUUID())) {
                  RaidConfig.BossData bossData = raid.getConfig().boss();
                  if (bossData.specialLootTable() != null) {
                     manager.dropSpecialLoot(mob, bossData.specialLootTable(), level);
                  }

                  raid.onBossDefeated();
                  break;
               }

               if (raid.getHenchmenUUIDs().contains(mob.getUUID())) {
                  raid.removeHenchman(mob.getUUID());
                  break;
               }
            }
         }
      }
   }

   private void dropSpecialLoot(Mob boss, ResourceLocation lootTableLocation, ServerLevel level) {
      LootTable lootTable = level.getServer()
         .reloadableRegistries()
         .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, lootTableLocation));
      Builder builder = new Builder(level)
         .withParameter(LootContextParams.THIS_ENTITY, boss)
         .withParameter(LootContextParams.ORIGIN, boss.position())
         .withParameter(LootContextParams.DAMAGE_SOURCE, boss.getLastDamageSource() != null ? boss.getLastDamageSource() : level.damageSources().generic());
      if (boss.getKillCredit() instanceof Player player) {
         builder.withParameter(LootContextParams.ATTACKING_ENTITY, player).withLuck(player.getLuck());
      }

      LootParams params = builder.create(LootContextParamSets.ENTITY);
      lootTable.getRandomItems(params).forEach(itemStack -> {
         ItemEntity itemEntity = new ItemEntity(level, boss.getX(), boss.getY(), boss.getZ(), itemStack);
         level.addFreshEntity(itemEntity);
      });
   }

   public Collection<ActiveRaid> getActiveRaids() {
      return activeRaids.values();
   }

   @SubscribeEvent
   public static void onServerStopping(ServerStoppingEvent event) {
      ServerLevel overworld = event.getServer().overworld();
      RaidManager manager = get(overworld);
      manager.saveActiveRaids(overworld);
      RaidSaveData.get(overworld);
      overworld.getDataStorage().save();
      INSTANCES.clear();
   }
}
