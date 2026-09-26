package top.ribs.scguns.config;



import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.util.NbtHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.entity.ai.AIType;
import top.ribs.scguns.entity.ai.GunAttackGoal;
import top.ribs.scguns.entity.player.GunTier;
import top.ribs.scguns.entity.player.PlayerGunProgression;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.compat.guardvillagers.GuardGunAttackGoal;
import top.ribs.scguns.compat.guardvillagers.GuardVillagersCompat;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.util.GunCurseUtil;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.GAME
)
public class GunnerMobSpawner {
   public static final ResourceLocation GUN_FOLLOW_RANGE_MODIFIER_UUID = ResourceLocation.fromNamespaceAndPath("scguns", "modifier/" + java.util.UUID.randomUUID());

   public GunnerMobSpawner() {
      super();
   }

   @SubscribeEvent
   public static void onSpecialSpawn(FinalizeSpawnEvent event) {
      if (GunMobValues.enabled) {
         LivingEntity entity = event.getEntity();
         if (entity instanceof PathfinderMob mob) {
            if (entity.getType().is(ModTags.Entities.GUNNER)) {
               GunnerMobConfig.MobGunnerData gunnerData = GunnerMobConfig.getGunnerData(entity.getType());
               if (gunnerData != null) {
                  if (!(entity.getRandom().nextFloat() >= gunnerData.spawnChance())) {
                     mob.addTag("MobGunner");
                     mob.addTag("ThematicGunner");
                  }
               } else {
                  Player nearestPlayer = entity.level().getNearestPlayer(entity, 64.0);
                  if (nearestPlayer != null) {
                     PlayerGunProgression progression = PlayerGunProgression.get(nearestPlayer);
                     List<GunTier> availableTiers = progression.getAvailableMobTiers();
                     boolean hasValidTiers = false;

                     for (GunTier tier : availableTiers) {
                        if (TieredWeaponConfig.hasTierWeapons(tier)) {
                           hasValidTiers = true;
                           break;
                        }
                     }

                     double spawnChance = GunMobValues.getGunnerSpawnChance(entity.level().getDifficulty());
                     if (hasValidTiers && (double)entity.getRandom().nextFloat() < spawnChance) {
                        mob.addTag("MobGunner");
                        mob.addTag("ProgressionGunner");
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
      if (event.getEntity() instanceof PathfinderMob mob) {
         ItemStack heldItem = mob.getMainHandItem();
         if (heldItem.getItem() instanceof GunItem) {
            reassessWeaponGoal(mob);
         } else if (event.getSlot() == EquipmentSlot.MAINHAND) {
            // Guard Villagers refills a guard's main hand with its own sword or crossbow, sometimes well
            // after the guard spawned - this is what puts the gun back (HANDOFF section 82). The compat
            // remembers the guard's roll, so a guard that never won one stays armed with a sword.
            GuardVillagersCompat.equipGuardGun(mob);
         }
      }
   }

   @SubscribeEvent
   public static void onLivingUpdate(EntityTickEvent.Post event) {
      if (event.getEntity() instanceof PathfinderMob mob) {
         if (mob instanceof AbstractPiglin abstractPiglin && abstractPiglin.level().dimension() == Level.OVERWORLD && abstractPiglin.tickCount % 20 == 0) {
            ItemStack helmet = abstractPiglin.getItemBySlot(EquipmentSlot.HEAD);
            if (helmet.is(ModTags.Items.GAS_MASK)) {
               abstractPiglin.setImmuneToZombification(true);
            } else {
               abstractPiglin.setImmuneToZombification(false);
            }
         }

         if (mob.tickCount < 2) {
            ItemStack heldItem = mob.getMainHandItem();
            if (GunMobValues.enabled) {
               if (mob.getTags().contains("MobGunner") && !(heldItem.getItem() instanceof GunItem)) {
                  if (mob.getTags().contains("ThematicGunner")) {
                     equipThematicGun(mob);
                  } else if (mob.getTags().contains("ProgressionGunner")) {
                     equipProgressionGun(mob);
                  }
               }

               // A guard's own equipment arrives after the join event, so this is the hook that really
               // arms one (HANDOFF section 82). Non-guards fall straight through.
               if (!(heldItem.getItem() instanceof GunItem)) {
                  GuardVillagersCompat.equipGuardGun(mob);
               }

               if (mob.getMainHandItem().getItem() instanceof GunItem) {
                  reassessWeaponGoal(mob);
               }
            }
         }
      }
   }

   private static void equipThematicGun(PathfinderMob mob) {
      GunnerMobConfig.MobGunnerData gunnerData = GunnerMobConfig.getGunnerData(mob.getType());
      if (gunnerData != null) {
         Item gun = gunnerData.getRandomWeapon(mob.getRandom());
         if (gun == null) {
            return;
         }

         AIType aiType = AIType.values()[mob.getRandom().nextInt(AIType.values().length)];
         boolean elite = (double)mob.getRandom().nextFloat() < GunMobValues.eliteChance && GunMobValues.elitesEnabled;
         int aiLevel = gunnerData.aiDifficulty() + (elite ? 1 : 0);
         if (elite) {
            mob.addTag("EliteGunner");
            mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
         } else {
            // weapon_drop_chance was parsed out of gunner_mobs.json and then never applied (HANDOFF
            // section 82): every thematic gunner dropped its gun at the vanilla mob rate instead of the
            // configured one. Elites above keep 0.0 on purpose - their gun never drops.
            mob.setDropChance(EquipmentSlot.MAINHAND, gunnerData.weaponDropChance());
         }

         for (GunnerMobConfig.ArmorPiece armorPiece : gunnerData.allowedArmor()) {
            if (mob.getRandom().nextFloat() < armorPiece.spawnChance()) {
               String var9 = armorPiece.slot();

               EquipmentSlot slot = switch (var9) {
                  case "head" -> EquipmentSlot.HEAD;
                  case "chest" -> EquipmentSlot.CHEST;
                  case "legs" -> EquipmentSlot.LEGS;
                  case "feet" -> EquipmentSlot.FEET;
                  default -> null;
               };
               if (slot != null) {
                  mob.setItemSlot(slot, new ItemStack(armorPiece.item()));
               }
            }
         }

         if (!mob.level().isClientSide && !hasGunAttackGoal(mob)) {
            ItemStack modifiedGun = createModifiedGun(mob, gun);
            mob.goalSelector.addGoal(2, new GunAttackGoal<>(mob, modifiedGun, 1.2F, aiType, aiLevel));
            mob.addTag("GunAttackAssigned");
         }

         ItemStack modifiedGun = createModifiedGun(mob, gun);
         GunCurseUtil.applyCurseIfRoll(modifiedGun, mob.getRandom());
         mob.setItemSlot(EquipmentSlot.MAINHAND, modifiedGun);
         extendFollowRange(mob);
      }
   }

   private static void equipProgressionGun(PathfinderMob mob) {
      Player nearestPlayer = mob.level().getNearestPlayer(mob, 64.0);
      if (nearestPlayer != null) {
         PlayerGunProgression progression = PlayerGunProgression.get(nearestPlayer);
         List<GunTier> availableTiers = progression.getAvailableMobTiers();
         if (!availableTiers.isEmpty()) {
            boolean isElite = (double)mob.getRandom().nextFloat() < GunMobValues.eliteChance && GunMobValues.elitesEnabled;
            List<GunTier> validTiers = new ArrayList<>();

            for (GunTier tier : availableTiers) {
               if (isElite) {
                  if (EliteTierConfig.hasEliteData(tier)) {
                     validTiers.add(tier);
                  }
               } else if (TieredWeaponConfig.hasTierWeapons(tier)) {
                  validTiers.add(tier);
               }
            }

            if (!validTiers.isEmpty()) {
               float rand = mob.getRandom().nextFloat();
               GunTier selectedTier;
               if (rand < 0.6F) {
                  int index = mob.getRandom().nextInt(Math.max(1, validTiers.size() / 2));
                  selectedTier = validTiers.get(index);
               } else if (rand < 0.9F && validTiers.size() > 1) {
                  int midStart = validTiers.size() / 3;
                  int midEnd = validTiers.size() * 2 / 3;
                  if (midEnd <= midStart) {
                     midEnd = midStart + 1;
                  }

                  if (midEnd >= validTiers.size()) {
                     midEnd = validTiers.size() - 1;
                  }

                  int index = midStart + mob.getRandom().nextInt(midEnd - midStart + 1);
                  selectedTier = validTiers.get(index);
               } else {
                  int index = Math.max(0, validTiers.size() - 1 - mob.getRandom().nextInt(Math.max(1, validTiers.size() / 3)));
                  selectedTier = validTiers.get(index);
               }

               Item gun;
               if (isElite) {
                  EliteTierConfig.EliteData eliteData = EliteTierConfig.getEliteData(selectedTier);
                  if (eliteData == null) {
                     return;
                  }

                  gun = eliteData.getRandomWeapon(mob.getRandom());
                  if (gun == null) {
                     return;
                  }

                  mob.addTag("EliteGunner");
                  mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

                  for (EliteTierConfig.ArmorPiece armorPiece : eliteData.armor()) {
                     if (mob.getRandom().nextFloat() < armorPiece.chance()) {
                        String var13 = armorPiece.slot();

                        EquipmentSlot slot = switch (var13) {
                           case "head" -> EquipmentSlot.HEAD;
                           case "chest" -> EquipmentSlot.CHEST;
                           case "legs" -> EquipmentSlot.LEGS;
                           case "feet" -> EquipmentSlot.FEET;
                           default -> null;
                        };
                        if (slot != null) {
                           mob.setItemSlot(slot, new ItemStack(armorPiece.item()));
                        }
                     }
                  }
               } else {
                  gun = TieredWeaponConfig.getRandomWeaponForTier(selectedTier, mob.getRandom());
                  if (gun == null) {
                     return;
                  }
               }

               AIType aiType = AIType.values()[mob.getRandom().nextInt(AIType.values().length)];
               int aiLevel = 2 + (isElite ? 1 : 0);
               if (!mob.level().isClientSide && !hasGunAttackGoal(mob)) {
                  ItemStack modifiedGun = createModifiedGun(mob, gun);
                  mob.goalSelector.addGoal(2, new GunAttackGoal<>(mob, modifiedGun, 1.2F, aiType, aiLevel));
                  mob.addTag("GunAttackAssigned");
               }

               ItemStack modifiedGun = createModifiedGun(mob, gun);
               GunCurseUtil.applyCurseIfRoll(modifiedGun, mob.getRandom());
               mob.setItemSlot(EquipmentSlot.MAINHAND, modifiedGun);
               extendFollowRange(mob);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
      if (GunMobValues.enabled) {
         if (event.getEntity() instanceof PathfinderMob mob) {
            mob.removeTag("GunAttackAssigned");
            ItemStack heldItem = mob.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem) {
               reassessWeaponGoal(mob);
            } else if (!GuardVillagersCompat.equipGuardGun(mob)) {
               // Guard Villagers compat (HANDOFF section 82): a guard spawns holding a sword or a
               // crossbow like any other guard, and is equipped from this config's
               // "guardvillagers:guard" entry instead. Every other mob falls through to the reset.
               resetFollowRange(mob);
            }
         }
      }
   }

   /**
    * Whether the mob already has gun AI. The Guard Villagers compat adds
    * {@link GuardGunAttackGoal} instead of {@link GunAttackGoal}, so it has to count here - otherwise
    * {@link #reassessWeaponGoal} would add the hostile raider AI to a guard that already has its own.
    */
   public static boolean hasGunAttackGoal(PathfinderMob mob) {
      return mob.goalSelector.getAvailableGoals().stream().anyMatch(goal -> {
         return goal.getGoal() instanceof GunAttackGoal || goal.getGoal() instanceof GuardGunAttackGoal;
      });
   }

   /**
    * Equips a Guard Villagers guard from this config's {@code guardvillagers:guard} entry, with the
    * guard's own gun AI (HANDOFF section 82). The caller decides <i>whether</i> to arm the guard (see
    * {@code GuardVillagersCompat.equipGuardGun}, which owns the spawn-chance roll); this method just does
    * it, and is also the re-arm path for a guard whose slot Guard Villagers has refilled with a sword.
    */
   public static boolean equipGuardGun(PathfinderMob mob, float accuracy) {
      if (!GuardVillagersCompat.isGuard(mob)) {
         return false;
      }

      GunnerMobConfig.MobGunnerData gunnerData = GunnerMobConfig.getGunnerData(mob.getType());
      if (gunnerData == null) {
         return false;
      }

      Item gun = gunnerData.getRandomWeapon(mob.getRandom());
      if (gun == null) {
         return false;
      }

      ItemStack modifiedGun = createModifiedGun(mob, gun);
      GunCurseUtil.applyCurseIfRoll(modifiedGun, mob.getRandom());
      mob.setItemSlot(EquipmentSlot.MAINHAND, modifiedGun);
      mob.setDropChance(EquipmentSlot.MAINHAND, gunnerData.weaponDropChance());
      if (!hasGunAttackGoal(mob)) {
         // The mod's own gunner AI, not a guard-specific one (HANDOFF section 82.9): guards fight at the
         // gun's ideal range like every other gunner, and this AI reserves no goal flags, so Guard
         // Villagers' patrol, return-to-village, door and stroll goals keep running while it does.
         mob.goalSelector.addGoal(2, new GuardGunAttackGoal(mob, modifiedGun, gunnerData.aiDifficulty(), accuracy));
      }

      mob.addTag("GunAttackAssigned");
      // A guard keeps its own follow range. Giving it the raider's 64 blocks (what the thematic gunners
      // get) sent armed guards chasing across the countryside and away from the village they exist to
      // defend; resetFollowRange also undoes the modifier a previous version left on an armed guard.
      resetFollowRange(mob);
      return true;
   }

   public static void reassessWeaponGoal(PathfinderMob mob) {
      if (!mob.level().isClientSide && !hasGunAttackGoal(mob)) {
         for (String tag : mob.getTags()) {
            if (tag.startsWith("RaidMember_")) {
               return;
            }
         }

         AIType aiType = AIType.values()[mob.getRandom().nextInt(AIType.values().length)];
         int aiDifficulty = mob.getRandom().nextInt(4) + 1;
         ItemStack heldItem = mob.getMainHandItem();
         mob.goalSelector.addGoal(2, new GunAttackGoal<>(mob, heldItem, 1.2F, aiType, aiDifficulty));
         mob.addTag("GunAttackAssigned");
         extendFollowRange(mob);
      }
   }

   private static ItemStack createModifiedGun(PathfinderMob mob, Item gun) {
      ItemStack gunStack = new ItemStack(gun);
      // `getTagForWrite` returns null for a stack that has no custom data yet, and this stack was just
      // created - so the ammo pre-fill below never ran, and every wild gunner (pillager, vindicator,
      // piglin, ...) carried a gun with no custom data at all (HANDOFF section 81). The mob's own
      // getAmmoCount then read 0, so its first move on acquiring a target was a reload instead of a shot.
      // The two sibling copies of this pattern - RaidManager.createModifiedGun and
      // EntityEquipmentConfig - were fixed in section 13; this third one was missed.
      if (gun instanceof GunItem gunItem) {
         Gun gunModified = gunItem.getModifiedGun(gunStack);
         NbtHelper.getOrCreateTag(gunStack)
            .putInt("AmmoCount", mob.getRandom().nextInt(gunModified.getReloads().getMaxAmmo()));
      }

      return gunStack;
   }

   public static void extendFollowRange(PathfinderMob mob) {
      if (mob.getAttribute(Attributes.FOLLOW_RANGE) != null) {
         double additionalRange = 64.0 - mob.getAttribute(Attributes.FOLLOW_RANGE).getBaseValue();
         AttributeModifier modifier = new AttributeModifier(GUN_FOLLOW_RANGE_MODIFIER_UUID, additionalRange, Operation.ADD_VALUE);
         if (!mob.getAttribute(Attributes.FOLLOW_RANGE).hasModifier(modifier.id())) {
            mob.getAttribute(Attributes.FOLLOW_RANGE).addPermanentModifier(modifier);
         }
      }
   }

   public static void resetFollowRange(PathfinderMob mob) {
      if (mob.getAttribute(Attributes.FOLLOW_RANGE) != null) {
         mob.getAttribute(Attributes.FOLLOW_RANGE).removeModifier(GUN_FOLLOW_RANGE_MODIFIER_UUID);
      }
   }
}
