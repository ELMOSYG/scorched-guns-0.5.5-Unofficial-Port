package top.ribs.scguns.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.util.LogicalSidedProvider;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
   modid = "scguns"
)
public class DelayedTask {
   public static List<DelayedTask.Impl> tasks = new ArrayList<>();

   public DelayedTask() {
      super();
   }

   @SubscribeEvent
   public static void onServerStart(ServerStartedEvent event) {
      tasks.clear();
   }

   @SubscribeEvent
   public static void onServerStopping(ServerStoppingEvent event) {
      tasks.clear();
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent.Post event) {
      {
         MinecraftServer server = (MinecraftServer)LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);
         Iterator<DelayedTask.Impl> it = tasks.iterator();

         while (it.hasNext()) {
            DelayedTask.Impl impl = it.next();
            if (impl.executionTick <= server.getTickCount()) {
               impl.runnable.run();
               it.remove();
            }
         }
      }
   }

   public static void runAfter(int ticks, Runnable run) {
      MinecraftServer server = (MinecraftServer)LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);
      if (!server.isSameThread()) {
         throw new IllegalStateException("Tried to add a delayed task off the main thread");
      } else {
         tasks.add(new DelayedTask.Impl(server.getTickCount() + ticks, run));
      }
   }

   private static class Impl {
      private final int executionTick;
      private final Runnable runnable;

      private Impl(int executionTick, Runnable runnable) {
         super();
         this.executionTick = executionTick;
         this.runnable = runnable;
      }
   }
}
