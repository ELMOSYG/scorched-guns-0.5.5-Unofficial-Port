package top.ribs.scguns.debug;


import top.ribs.scguns.util.DistHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.commons.lang3.tuple.Pair;
import top.ribs.scguns.client.ClientHandler;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.debug.client.screen.widget.DebugButton;
import top.ribs.scguns.debug.client.screen.widget.DebugToggle;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.ScopeItem;
import top.ribs.scguns.item.attachment.impl.Scope;

@EventBusSubscriber(
   modid = "scguns"
)
public class Debug {
   private static final Map<Item, Gun> GUNS = new HashMap<>();
   private static final Map<Item, Scope> SCOPES = new HashMap<>();
   private static boolean forceAim = false;

   public Debug() {
      super();
   }

   @SubscribeEvent
   public static void onServerStarting(ServerStartedEvent event) {
      event.getServer().execute(() -> {
         GUNS.clear();
         SCOPES.clear();
      });
   }

   public static Gun getGun(GunItem item) {
      return GUNS.computeIfAbsent(item, item1 -> item.getGun().copy());
   }

   public static Scope getScope(ScopeItem item) {
      return SCOPES.computeIfAbsent(item, item1 -> item.getProperties().copy());
   }

   public static boolean isForceAim() {
      return forceAim;
   }

   public static void setForceAim(boolean forceAim) {
      Debug.forceAim = forceAim;
   }

   public static class Menu implements IEditorMenu {
      public Menu() {
         super();
      }

      @Override
      public Component getEditorLabel() {
         return Component.literal("Editor Menu");
      }

      @Override
      public void getEditorWidgets(List<Pair<Component, Supplier<IDebugWidget>>> widgets) {
         DistHelper.runWhenOn(Dist.CLIENT, () -> {
                  ItemStack heldItem = Objects.requireNonNull(Minecraft.getInstance().player).getMainHandItem();
                  if (heldItem.getItem() instanceof GunItem gunItem) {
                     widgets.add(
                        Pair.of(
                           Component.translatable(gunItem.getDescriptionId()),
                           (Supplier<IDebugWidget>)() -> new DebugButton(
                                 Component.literal("Edit"), btn -> Minecraft.getInstance().setScreen(ClientHandler.createEditorScreen(Debug.getGun(gunItem)))
                              )
                        )
                     );
                  }

                  widgets.add(
                     Pair.of(
                        Component.literal("Settings"),
                        (Supplier<IDebugWidget>)() -> new DebugButton(
                              Component.literal(">"), btn -> Minecraft.getInstance().setScreen(ClientHandler.createEditorScreen(new Debug.Settings()))
                           )
                     )
                  );
               }
         );
      }
   }

   public static class Settings implements IEditorMenu {
      public Settings() {
         super();
      }

      @Override
      public Component getEditorLabel() {
         return Component.literal("Settings");
      }

      @Override
      public void getEditorWidgets(List<Pair<Component, Supplier<IDebugWidget>>> widgets) {
         DistHelper.runWhenOn(Dist.CLIENT, () -> widgets.add(
                     Pair.of(Component.literal("Force Aim"), (Supplier<IDebugWidget>)() -> new DebugToggle(Debug.forceAim, value -> Debug.forceAim = value))
                  )
         );
      }
   }
}
