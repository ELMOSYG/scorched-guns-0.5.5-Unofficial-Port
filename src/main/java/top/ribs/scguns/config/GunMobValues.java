package top.ribs.scguns.config;

import net.minecraft.world.Difficulty;
import top.ribs.scguns.Config;

public class GunMobValues {
   public static boolean enabled = true;
   public static double gunnerSpawnChance = 0.25;
   public static boolean scaleToDifficulty = true;
   public static boolean elitesEnabled = true;
   public static double eliteChance = 0.15;

   public GunMobValues() {
      super();
   }

   public static void init() {
      enabled = (Boolean)Config.COMMON.gunnerMobs.gunnerMobSpawning.get();
      gunnerSpawnChance = (Double)Config.COMMON.gunnerMobs.gunnerSpawnChance.get();
      scaleToDifficulty = (Boolean)Config.COMMON.gunnerMobs.scaleToDifficulty.get();
      elitesEnabled = (Boolean)Config.COMMON.gunnerMobs.eliteSpawning.get();
      eliteChance = (Double)Config.COMMON.gunnerMobs.eliteChance.get();
   }

   public static double getGunnerSpawnChance(Difficulty difficulty) {
      return !scaleToDifficulty ? gunnerSpawnChance : gunnerSpawnChance * getDifficultyMultiplier(difficulty);
   }

   public static double getEliteChance(Difficulty difficulty) {
      return !scaleToDifficulty ? eliteChance : eliteChance * getDifficultyMultiplier(difficulty);
   }

   private static double getDifficultyMultiplier(Difficulty difficulty) {
      return switch (difficulty) {
         case PEACEFUL -> 0.5;
         case EASY -> 0.75;
         case NORMAL -> 1.0;
         case HARD -> 1.5;
         default -> throw new IncompatibleClassChangeError();
      };
   }
}
