package top.ribs.scguns.event;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import top.ribs.scguns.util.NbtHelper;

/**
 * Hands {@link NbtHelper} the registry access its item codecs need (HANDOFF section 58).
 *
 * <p>{@code ItemStack.CODEC} encodes enchantments as registry references, so encoding an enchanted
 * stack with plain {@code NbtOps} fails outright. Without a provider the failure was silent and the
 * stack became an empty tag, which is how an enchanted attachment disappeared the moment it was
 * installed on a gun.</p>
 *
 * <p>Which side owns the provider matters, and getting it wrong is worse than not having one: in
 * singleplayer the client and the server share a JVM but <b>not</b> an enchantment registry instance -
 * the client builds its own from the login packet. A stack decoded with the client's registry holds
 * client-owned enchantment holders, and the server cannot find a network id for those, so sending the
 * container contents threw {@code IllegalArgumentException: Can't find id for
 * 'Reference{ResourceKey[minecraft:enchantment / minecraft:mending]}'} and disconnected the player
 * (HANDOFF section 59). The rules below therefore hand the provider to the side that is actually
 * present:</p>
 *
 * <ul>
 *   <li>a loaded {@link ServerLevel} (or a started server) always wins - it stores and sends the
 *       data, so its registry is the authoritative one;</li>
 *   <li>a client level may install its own registry only while <b>no</b> server is running in this
 *       JVM, which is the dedicated-server case;</li>
 *   <li>{@code ServerStoppedEvent} clears that server ownership again, so a later client world in the
 *       same JVM refreshes its own registry instead of keeping holders from a dead server.</li>
 * </ul>
 *
 * <p>The provider is never cleared on unload: the registries behind it are static for the session, and
 * a stale provider is only ever replaced by a fresher one.</p>
 */
@EventBusSubscriber(modid = "scguns", bus = EventBusSubscriber.Bus.GAME)
public final class RegistryAccessListener {
   /** True while a server exists in this JVM and therefore owns the provider. */
   private static boolean serverOwnsRegistryAccess;

   private RegistryAccessListener() {
   }

   @SubscribeEvent
   public static void onLevelLoad(LevelEvent.Load event) {
      if (event.getLevel() instanceof ServerLevel) {
         serverOwnsRegistryAccess = true;
         NbtHelper.setRegistryAccess(event.getLevel().registryAccess());
      } else if (!serverOwnsRegistryAccess) {
         NbtHelper.setRegistryAccess(event.getLevel().registryAccess());
      }
   }

   @SubscribeEvent
   public static void onServerStarted(ServerStartedEvent event) {
      serverOwnsRegistryAccess = true;
      NbtHelper.setRegistryAccess(event.getServer().registryAccess());
   }

   @SubscribeEvent
   public static void onServerStopped(ServerStoppedEvent event) {
      // Saving is finished by the time this fires, so nothing is lost. Releasing ownership lets a
      // client that connects to a dedicated server afterwards install its own registry; keeping the
      // dead server's would mean encoding holders its client no longer has ids for.
      serverOwnsRegistryAccess = false;
   }
}
