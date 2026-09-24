package top.ribs.scguns.client;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class GunModel implements BakedModel {
   private static final GunModel INSTANCE = new GunModel();
   private BakedModel model;

   public GunModel() {
      super();
   }

   public void setModel(BakedModel model) {
      this.model = model;
   }

   public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
      return this.model.getQuads(state, direction, random);
   }

   public boolean useAmbientOcclusion() {
      return this.model.useAmbientOcclusion();
   }

   public boolean isGui3d() {
      return this.model.isGui3d();
   }

   public boolean usesBlockLight() {
      return this.model.usesBlockLight();
   }

   public boolean isCustomRenderer() {
      return false;
   }

   public TextureAtlasSprite getParticleIcon() {
      return this.model.getParticleIcon();
   }

   public ItemOverrides getOverrides() {
      return this.model.getOverrides();
   }

   public static BakedModel wrap(BakedModel model) {
      INSTANCE.setModel(model);
      return INSTANCE;
   }

   public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
      return List.of(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
   }
}
