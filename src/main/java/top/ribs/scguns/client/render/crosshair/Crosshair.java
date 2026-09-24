package top.ribs.scguns.client.render.crosshair;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import top.ribs.scguns.client.handler.CrosshairHandler;
import top.ribs.scguns.interfaces.IResourceLocation;

public abstract class Crosshair implements IResourceLocation {
   public static final Crosshair DEFAULT = new Crosshair(ResourceLocation.parse("default")) {
   };
   private final ResourceLocation id;

   protected Crosshair(ResourceLocation id) {
      super();
      this.id = id;
   }

   public void render(Minecraft mc, PoseStack stack, int windowWidth, int windowHeight, float partialTicks) {
   }

   public void tick() {
   }

   public void onGunFired() {
   }

   @Override
   public final ResourceLocation getLocation() {
      return this.id;
   }

   public final boolean isDefault() {
      return this == DEFAULT;
   }

   static {
      CrosshairHandler.get().register(DEFAULT);
   }
}
