package top.ribs.scguns.fluid;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidType.Properties;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class ViciousAcidFluidType extends FluidType {
   private final ResourceLocation stillTexture;
   private final ResourceLocation flowingTexture;

   public ViciousAcidFluidType(ResourceLocation stillTexture, ResourceLocation flowingTexture) {
      super(Properties.create().density(1100).viscosity(1200).temperature(300).canSwim(true).canDrown(true).canPushEntity(true).supportsBoating(false));
      this.stillTexture = stillTexture;
      this.flowingTexture = flowingTexture;
   }

   public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
      consumer.accept(
         new IClientFluidTypeExtensions() {
            public ResourceLocation getStillTexture() {
               return ViciousAcidFluidType.this.stillTexture;
            }

            public ResourceLocation getFlowingTexture() {
               return ViciousAcidFluidType.this.flowingTexture;
            }

            public int getTintColor() {
               return -7357388;
            }

            @NotNull
            public Vector3f modifyFogColor(
               Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor
            ) {
               return new Vector3f(0.56F, 0.74F, 0.2F);
            }

            public void modifyFogRender(
               Camera camera, FogMode mode, float renderDistance, float partialTick, float nearDistance, float farDistance, FogShape shape
            ) {
               RenderSystem.setShaderFogStart(0.5F);
               RenderSystem.setShaderFogEnd(3.0F);
            }
         }
      );
   }
}
