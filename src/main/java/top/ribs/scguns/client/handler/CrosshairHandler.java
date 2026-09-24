package top.ribs.scguns.client.handler;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.config.ModConfigEvent.Reloading;
import top.ribs.scguns.Config;
import top.ribs.scguns.client.render.crosshair.Crosshair;
import top.ribs.scguns.client.render.crosshair.DynamicCrosshair;
import top.ribs.scguns.client.render.crosshair.SpecialHitMarker;
import top.ribs.scguns.client.render.crosshair.TechCrosshair;
import top.ribs.scguns.client.render.crosshair.TexturedCrosshair;
import top.ribs.scguns.event.GunFireEvent;
import top.ribs.scguns.item.GunItem;

public class CrosshairHandler {
   private static CrosshairHandler instance;
   private final Map<ResourceLocation, Crosshair> idToCrosshair = new HashMap<>();
   private final List<Crosshair> registeredCrosshairs = new ArrayList<>();
   private Crosshair currentCrosshair = null;

   public static CrosshairHandler get() {
      if (instance == null) {
         instance = new CrosshairHandler();
      }

      return instance;
   }

   private CrosshairHandler() {
      super();
      this.register(new TexturedCrosshair(ResourceLocation.fromNamespaceAndPath("scguns", "better_default")));
      this.register(new TexturedCrosshair(ResourceLocation.fromNamespaceAndPath("scguns", "circle")));
      this.register(new TexturedCrosshair(ResourceLocation.fromNamespaceAndPath("scguns", "filled_circle"), false));
      this.register(new TexturedCrosshair(ResourceLocation.fromNamespaceAndPath("scguns", "square")));
      this.register(new TexturedCrosshair(ResourceLocation.fromNamespaceAndPath("scguns", "round")));
      this.register(new TexturedCrosshair(ResourceLocation.fromNamespaceAndPath("scguns", "arrow")));
      this.register(new TexturedCrosshair(ResourceLocation.fromNamespaceAndPath("scguns", "dot")));
      this.register(new TexturedCrosshair(ResourceLocation.fromNamespaceAndPath("scguns", "box")));
      this.register(new TexturedCrosshair(ResourceLocation.fromNamespaceAndPath("scguns", "hit_marker")));
      this.register(new TexturedCrosshair(ResourceLocation.fromNamespaceAndPath("scguns", "line")));
      this.register(new TexturedCrosshair(ResourceLocation.fromNamespaceAndPath("scguns", "t")));
      this.register(new TexturedCrosshair(ResourceLocation.fromNamespaceAndPath("scguns", "smiley")));
      this.register(new TechCrosshair());
      this.register(new DynamicCrosshair());
   }

   public void register(Crosshair crosshair) {
      if (!this.idToCrosshair.containsKey(crosshair.getLocation())) {
         this.idToCrosshair.put(crosshair.getLocation(), crosshair);
         this.registeredCrosshairs.add(crosshair);
      }
   }

   public void setCrosshair(ResourceLocation id) {
      this.currentCrosshair = this.idToCrosshair.getOrDefault(id, Crosshair.DEFAULT);
   }

   @Nullable
   public Crosshair getCurrentCrosshair() {
      if (this.currentCrosshair == null && this.registeredCrosshairs.size() > 0) {
         ResourceLocation id = ResourceLocation.tryParse((String)Config.CLIENT.display.crosshair.get());
         this.currentCrosshair = id != null ? this.idToCrosshair.getOrDefault(id, Crosshair.DEFAULT) : Crosshair.DEFAULT;
      }

      return this.currentCrosshair;
   }

   public List<Crosshair> getRegisteredCrosshairs() {
      return ImmutableList.copyOf(this.registeredCrosshairs);
   }

   @SubscribeEvent
   public void onRenderOverlay(Pre event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         // 1.21 identifies the layer with a ResourceLocation (VanillaGuiLayers) and
         // the event no longer carries the window; GuiGraphics reports the sizes.
         if (event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            ItemStack heldItem = mc.player.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem) {
               PoseStack stack = event.getGuiGraphics().pose();
               stack.pushPose();
               int scaledWidth = event.getGuiGraphics().guiWidth();
               int scaledHeight = event.getGuiGraphics().guiHeight();
               if (HUDRenderHandler.isRenderingHitMarker()) {
                  Crosshair hitMarker = new SpecialHitMarker();
                  hitMarker.render(mc, stack, scaledWidth, scaledHeight, event.getPartialTick().getGameTimeDeltaPartialTick(false));
               }

               Crosshair crosshair = this.getCurrentCrosshair();
               if (AimingHandler.get().getNormalisedAdsProgress() > 0.5 && mc.options.getCameraType().isFirstPerson()) {
                  // 0.5.5 cancelled here to hide the vanilla crosshair while aiming down sights.
                  event.setCanceled(true);
               } else if (crosshair != null && !crosshair.isDefault()) {
                  // 0.5.5 cancelled here so the vanilla crosshair is not drawn underneath the custom one.
                  event.setCanceled(true);
                  if (mc.player.getUseItem().getItem() != Items.SHIELD) {
                     crosshair.render(mc, stack, scaledWidth, scaledHeight, event.getPartialTick().getGameTimeDeltaPartialTick(false));
                     stack.popPose();
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onClientTick(ClientTickEvent.Post event) {
      {
         Crosshair crosshair = this.getCurrentCrosshair();
         if (crosshair != null && !crosshair.isDefault()) {
            crosshair.tick();
         }
      }
   }

   @SubscribeEvent
   public void onGunFired(GunFireEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && event.getEntity() == mc.player) {
         Crosshair crosshair = this.getCurrentCrosshair();
         if (crosshair != null && !crosshair.isDefault()) {
            crosshair.onGunFired();
         }
      }
   }

   public static void onConfigReload(Reloading event) {
      ModConfig config = event.getConfig();
      if (config.getType() == Type.CLIENT && config.getModId().equals("scguns")) {
         ResourceLocation id = ResourceLocation.tryParse((String)Config.CLIENT.display.crosshair.get());
         if (id != null) {
            get().setCrosshair(id);
         }
      }
   }
}
