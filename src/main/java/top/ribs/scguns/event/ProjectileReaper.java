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
         // Collect first, discard afterwards: getAllEntities() is a live view, and removing an entity while walking
         // it threw ArrayIndexOutOfBoundsException inside fastutil's map iterator (crash report 2026-11-16 12:31).
         java.util.List<ProjectileEntity> parked = new java.util.ArrayList<>();
         for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ProjectileEntity projectile
               && !projectile.isRemoved()
               && !level.isPositionEntityTicking(projectile.blockPosition())) {
               parked.add(projectile);
            }
         }

         for (ProjectileEntity projectile : parked) {
            projectile.discard();
            removed++;
         }
      }

      // DIAGNOSTIC: temporary, info level so a report shows whether the sweep runs at all and what it sees.
      int seen = 0;
      int parkedTotal = 0;
      for (ServerLevel level : event.getServer().getAllLevels()) {
         for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ProjectileEntity) {
               seen++;
               if (!level.isPositionEntityTicking(entity.blockPosition())) {
                  parkedTotal++;
               }
            }
         }
      }

      top.ribs.scguns.ScorchedGuns.LOGGER.info(
         "scguns: projectile sweep - projectiles={}, outside ticking area={}, removed={}", seen, parkedTotal, removed);
   }
}
