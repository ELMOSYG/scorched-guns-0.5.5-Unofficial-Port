package top.ribs.scguns.client.render.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public interface IOverrideModel {
   default void tick(Player entity) {
   }

   void render(
      float var1,
      ItemDisplayContext var2,
      ItemStack var3,
      ItemStack var4,
      @Nullable LivingEntity var5,
      PoseStack var6,
      MultiBufferSource var7,
      int var8,
      int var9
   );
}
