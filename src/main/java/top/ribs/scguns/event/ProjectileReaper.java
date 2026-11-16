package top.ribs.scguns.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import top.ribs.scguns.entity.projectile.ProjectileEntity;

/**
 * Removes projectiles that ended up outside the ticked part of the world.
 *
 * <p>A round whose chunk is loaded but not <em>entity ticking</em> - the band at the edge of the simulation distance,
 * and the area a projectile reaches when the player walks away - stays in the level's entity list while its
 * {@code tick()} is never called. Its lifetime therefore never advances and it sits there forever; {@code /tp @e}
 * will happily hand you dozens of them. Marking projectiles as "not saved" stops them from coming back after a
 * reload, but it does nothing for one that is simply parked in a loaded-but-not-ticking chunk, which is what the
 * plasma and sculk rounds do.</p>
 *
 * <p>The sweep runs once every ten seconds on the server thread and only touches this mod's own projectiles, so the
 * cost is one list walk over entities that already exist in loaded chunks.</p>
 */
@EventBusSubscriber(modid = "scguns", bus = EventBusSubscriber.Bus.GAME)
public final class ProjectileReaper {

   private static final int SWEEP_INTERVAL_TICKS = 200;

   private ProjectileReaper() {
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent.Post event) {
      if (event.getServer().getTickCount() % SWEEP_INTERVAL_TICKS != 0) {
         return;
      }

      int removed = 0;

      for (ServerLevel level : event.getServer().getAllLevels()) {
         // The band between the simulation distance and the render distance is the problem: entities there are still
         // sent to clients (so an entity radar shows them) but are not ticked, which means a projectile's lifetime
         // never advances. Compare against the simulation distance directly instead of asking the chunk system.
         // Three blocks of margin: the boundary itself belongs to the ticking area, and a round parked exactly on it
         // would be kept forever (eight chunks is exactly 128 blocks).
         int simulationBlocks = Math.max(16, level.getServer().getPlayerList().getSimulationDistance() * 16 - 3);
         double limitSqr = (double) simulationBlocks * (double) simulationBlocks;

         java.util.List<Entity> doomed = new java.util.ArrayList<>();
         for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof ProjectileEntity projectile) || projectile.isRemoved()) {
               continue;
            }

            boolean nearPlayer = level.players().stream()
               .anyMatch(player -> player.distanceToSqr(projectile) <= limitSqr);
            if (!nearPlayer) {
               doomed.add(projectile);
            }
         }

         // Collected first: getAllEntities() is a live view and removing while walking it crashed the server
         // (crash report 2026-11-16 12:31).
         for (Entity projectile : doomed) {
            projectile.discard();
            removed++;
         }
      }

      // Quiet by default: only report when something was actually reaped, and only at debug level.
      if (removed > 0) {
         top.ribs.scguns.ScorchedGuns.LOGGER.debug("scguns: reaped {} parked projectiles", removed);
      }
   }
}
