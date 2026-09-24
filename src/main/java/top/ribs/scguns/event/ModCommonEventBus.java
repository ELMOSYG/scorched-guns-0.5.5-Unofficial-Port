package top.ribs.scguns.event;


import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import top.ribs.scguns.entity.monster.AdjudicatorEntity;
import top.ribs.scguns.entity.monster.BlundererEntity;
import top.ribs.scguns.entity.monster.CogKnightEntity;
import top.ribs.scguns.entity.monster.CogMinionEntity;
import top.ribs.scguns.entity.monster.DissidentEntity;
import top.ribs.scguns.entity.monster.FinforcerEntity;
import top.ribs.scguns.entity.monster.HiveEntity;
import top.ribs.scguns.entity.monster.HornlinEntity;
import top.ribs.scguns.entity.monster.MotherGhastEntity;
import top.ribs.scguns.entity.monster.PraetorEntity;
import top.ribs.scguns.entity.monster.ScampTankEntity;
import top.ribs.scguns.entity.monster.ScamplerEntity;
import top.ribs.scguns.entity.monster.SignalBeaconEntity;
import top.ribs.scguns.entity.monster.SkyCarrierEntity;
import top.ribs.scguns.entity.monster.SubjugatorEntity;
import top.ribs.scguns.entity.monster.SulfurheadEntity;
import top.ribs.scguns.entity.monster.SupplyScampEntity;
import top.ribs.scguns.entity.monster.SwarmEntity;
import top.ribs.scguns.entity.monster.TheMerchantEntity;
import top.ribs.scguns.entity.monster.TraumaUnitEntity;
import top.ribs.scguns.entity.monster.ViventrumEntity;
import top.ribs.scguns.entity.monster.ZombifiedHornlinEntity;
import top.ribs.scguns.init.ModEntities;

@EventBusSubscriber(
   modid = "scguns",
   bus = Bus.MOD
)
public class ModCommonEventBus {
   public ModCommonEventBus() {
      super();
   }

   @SubscribeEvent
   public static void entityAttributes(EntityAttributeCreationEvent event) {
      event.put((EntityType)ModEntities.COG_MINION.get(), CogMinionEntity.createAttributes().build());
      event.put((EntityType)ModEntities.SUPPLY_SCAMP.get(), SupplyScampEntity.createAttributes().build());
      event.put((EntityType)ModEntities.COG_KNIGHT.get(), CogKnightEntity.createAttributes().build());
      event.put((EntityType)ModEntities.SKY_CARRIER.get(), SkyCarrierEntity.createAttributes().build());
      event.put((EntityType)ModEntities.DISSIDENT.get(), DissidentEntity.createAttributes().build());
      event.put((EntityType)ModEntities.VIVENTRUM.get(), ViventrumEntity.createAttributes().build());
      event.put((EntityType)ModEntities.SCAMP_TANK.get(), ScampTankEntity.createAttributes().build());
      event.put((EntityType)ModEntities.BLUNDERER.get(), BlundererEntity.createAttributes().build());
      event.put((EntityType)ModEntities.HIVE.get(), HiveEntity.createAttributes().build());
      event.put((EntityType)ModEntities.SWARM.get(), SwarmEntity.createAttributes().build());
      event.put((EntityType)ModEntities.SIGNAL_BEACON.get(), SignalBeaconEntity.createAttributes().build());
      event.put((EntityType)ModEntities.HORNLIN.get(), HornlinEntity.createAttributes().build());
      event.put((EntityType)ModEntities.ZOMBIFIED_HORNLIN.get(), ZombifiedHornlinEntity.createAttributes().build());
      event.put((EntityType)ModEntities.THE_MERCHANT.get(), TheMerchantEntity.createAttributes().build());
      event.put((EntityType)ModEntities.TRAUMA_UNIT.get(), TraumaUnitEntity.createAttributes().build());
      event.put((EntityType)ModEntities.SCAMPLER.get(), ScamplerEntity.createAttributes().build());
      event.put((EntityType)ModEntities.SULFURHEAD.get(), SulfurheadEntity.createAttributes().build());
      event.put((EntityType)ModEntities.ADJUDICATOR.get(), AdjudicatorEntity.createAttributes().build());
      event.put((EntityType)ModEntities.SUBJUGATOR.get(), SubjugatorEntity.createAttributes().build());
      event.put((EntityType)ModEntities.MOTHER_GHAST.get(), MotherGhastEntity.createAttributes().build());
      event.put((EntityType)ModEntities.FINFORCER.get(), FinforcerEntity.createAttributes().build());
      event.put((EntityType)ModEntities.PRAETOR.get(), PraetorEntity.createAttributes().build());
   }

   @SubscribeEvent
   public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
      event.register((EntityType)ModEntities.COG_MINION.get(), SpawnPlacementTypes.ON_GROUND, Types.WORLD_SURFACE, Monster::checkMonsterSpawnRules, Operation.OR);
      event.register((EntityType)ModEntities.SUPPLY_SCAMP.get(), SpawnPlacementTypes.ON_GROUND, Types.WORLD_SURFACE, Animal::checkAnimalSpawnRules, Operation.OR);
      event.register((EntityType)ModEntities.BLUNDERER.get(), SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, Operation.OR);
      event.register((EntityType)ModEntities.COG_KNIGHT.get(), SpawnPlacementTypes.ON_GROUND, Types.WORLD_SURFACE, Monster::checkMonsterSpawnRules, Operation.OR);
      event.register((EntityType)ModEntities.TRAUMA_UNIT.get(), SpawnPlacementTypes.ON_GROUND, Types.WORLD_SURFACE, Monster::checkMonsterSpawnRules, Operation.OR);
      event.register((EntityType)ModEntities.DISSIDENT.get(), SpawnPlacementTypes.ON_GROUND, Types.WORLD_SURFACE, Monster::checkMonsterSpawnRules, Operation.OR);
      event.register((EntityType)ModEntities.PRAETOR.get(), SpawnPlacementTypes.ON_GROUND, Types.WORLD_SURFACE, Monster::checkMonsterSpawnRules, Operation.OR);
      event.register((EntityType)ModEntities.SULFURHEAD.get(), SpawnPlacementTypes.ON_GROUND, Types.WORLD_SURFACE, Monster::checkMonsterSpawnRules, Operation.OR);
      event.register((EntityType)ModEntities.HIVE.get(), SpawnPlacementTypes.ON_GROUND, Types.WORLD_SURFACE, Monster::checkMonsterSpawnRules, Operation.OR);
      event.register((EntityType)ModEntities.HORNLIN.get(), SpawnPlacementTypes.ON_GROUND, Types.WORLD_SURFACE, Monster::checkMonsterSpawnRules, Operation.OR);
      event.register((EntityType)ModEntities.ZOMBIFIED_HORNLIN.get(), SpawnPlacementTypes.ON_GROUND, Types.WORLD_SURFACE, Monster::checkMonsterSpawnRules, Operation.OR);
   }
}
