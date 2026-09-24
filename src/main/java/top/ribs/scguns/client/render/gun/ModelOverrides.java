package top.ribs.scguns.client.render.gun;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.ribs.scguns.item.GunItem;

@EventBusSubscriber(
   modid = "scguns",
   value = {Dist.CLIENT}
)
public class ModelOverrides {
   private static final Map<Item, IOverrideModel> MODEL_MAP = new HashMap<>();

   public ModelOverrides() {
      super();
   }

   public static void register(Item item, IOverrideModel model) {
      // 0.5.5 also called MinecraftForge.EVENT_BUS.register(model) here, but no model
      // declares @SubscribeEvent at the instance level: the few that listen for events
      // keep their handler inside a nested class that carries its own
      // @EventBusSubscriber. Forge tolerated registering an object with nothing to
      // subscribe; NeoForge aborts mod loading with "class X has no @SubscribeEvent
      // methods, but register was called anyway", so the registration is gone.
      // Ticking is driven by onClientPlayerTick below, which calls model.tick(player).
      MODEL_MAP.putIfAbsent(item, model);
   }

   public static boolean hasModel(ItemStack stack) {
      return MODEL_MAP.containsKey(stack.getItem());
   }

   @Nullable
   public static IOverrideModel getModel(ItemStack stack) {
      return MODEL_MAP.get(stack.getItem());
   }

   @SubscribeEvent
   public static void onClientPlayerTick(PlayerTickEvent.Pre event) {
      // 1.20.1 read TickEvent#side; the 1.21 per-entity tick events fire on both
      // sides, so the side has to come from the level.
      if (event.getEntity().level().isClientSide()) {
         tick(event.getEntity());
      }
   }

   private static void tick(Player player) {
      ItemStack heldItem = player.getMainHandItem();
      if (!heldItem.isEmpty() && heldItem.getItem() instanceof GunItem) {
         IOverrideModel model = getModel(heldItem);
         if (model != null) {
            model.tick(player);
         }
      }
   }
}
