package top.ribs.scguns.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.resources.ResourceLocation;

public final class GunRenderType extends RenderType {
   private static final RenderType BULLET_TRAIL = RenderType.create(
      "scguns:projectile_trail",
      DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
      Mode.QUADS,
      256,
      true,
      true,
      CompositeState.builder().setShaderState(RenderStateShard.POSITION_COLOR_LIGHTMAP_SHADER).setCullState(NO_CULL).setTransparencyState(TRANSLUCENT_TRANSPARENCY).createCompositeState(false)
   );

   private GunRenderType(
      String nameIn,
      VertexFormat formatIn,
      Mode drawModeIn,
      int bufferSizeIn,
      boolean useDelegateIn,
      boolean needsSortingIn,
      Runnable setupTaskIn,
      Runnable clearTaskIn
   ) {
      super(nameIn, formatIn, drawModeIn, bufferSizeIn, useDelegateIn, needsSortingIn, setupTaskIn, clearTaskIn);
   }

   public static RenderType getBulletTrail() {
      return BULLET_TRAIL;
   }

   public static RenderType getMuzzleFlash(ResourceLocation flashTexture) {
      return RenderType.create(
         "scguns:muzzle_flash",
         DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
         Mode.QUADS,
         256,
         true,
         false,
         CompositeState.builder()
            .setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
            .setTextureState(new TextureStateShard(flashTexture, false, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .createCompositeState(true)
      );
   }
}
