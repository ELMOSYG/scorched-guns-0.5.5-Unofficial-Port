package top.ribs.scguns.mixin.common;


import top.ribs.scguns.util.NbtHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.ribs.scguns.item.GunItem;

/**
 * Scales a dropped gun up when it travels through the End portal.
 *
 * <p>0.5.5 injected just before the
 * {@code Entity#changeDimension(ServerLevel)} call inside
 * {@code EndPortalBlock#entityInside}. 1.21 removed that call from
 * {@code entityInside} -- it now only does {@code entity.setAsInsidePortal(this, pos)}
 * -- and moved the actual travel to
 * {@code EndPortalBlock#getPortalDestination(ServerLevel, Entity, BlockPos)}, which
 * {@code Entity#handlePortal} calls when the portal timer expires. Injecting on the
 * old instruction therefore has no injection point at all, and because
 * {@code scguns.mixins.json} sets {@code injectors.defaultRequire = 1} that is a hard
 * "Critical injection failure" at startup, not a silent no-op.</p>
 *
 * <p>This only became visible once {@code MixinPlugin} stopped disabling every mixin
 * (see HANDOFF 15).</p>
 */
@Mixin({EndPortalBlock.class})
public class EndPortalBlockMixin {
   public EndPortalBlockMixin() {
      super();
   }

   @Inject(
      method = {"getPortalDestination"},
      at = {@At("HEAD")}
   )
   private void beforeChangeDimension(
      ServerLevel level, Entity entityIn, BlockPos pos, CallbackInfoReturnable<DimensionTransition> cir
   ) {
      // `level` is the level the entity is currently in, so this is the same
      // "leaving the End" condition 0.5.5 checked against its `worldIn`.
      if (level.dimension() == Level.END && entityIn instanceof ItemEntity) {
         ItemStack stack = ((ItemEntity)entityIn).getItem();
         if (stack.getItem() instanceof GunItem) {
            ItemStack gun = stack.copy();
            NbtHelper.getOrCreateTag(gun).putFloat("Scale", 2.0F);
            ((ItemEntity)entityIn).setItem(gun);
         }
      }
   }
}
