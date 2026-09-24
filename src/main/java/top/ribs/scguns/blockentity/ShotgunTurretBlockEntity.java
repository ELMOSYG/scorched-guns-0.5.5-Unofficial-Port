package top.ribs.scguns.blockentity;

import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import top.ribs.scguns.block.ShotgunTurretBlock;
import top.ribs.scguns.block.TurretTargetingBlock;
import top.ribs.scguns.client.screen.ShotgunTurretMenu;
import top.ribs.scguns.init.ModBlockEntities;

public class ShotgunTurretBlockEntity extends TurretBlockEntity {
   private static final ResourceLocation TURRET_ID = ResourceLocation.fromNamespaceAndPath("scguns", "shotgun_turret");

   public ShotgunTurretBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType<?>)ModBlockEntities.SHOTGUN_TURRET.get(), pos, state, TURRET_ID);
   }

   @NotNull
   public Component getDisplayName() {
      return Component.translatable("container.shotgun_turret");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
      boolean hasTargetingModule = false;
      if (this.level != null) {
         for (Direction direction : Direction.values()) {
            BlockState blockState = this.level.getBlockState(this.worldPosition.relative(direction));
            if (blockState.getBlock() instanceof TurretTargetingBlock) {
               hasTargetingModule = true;
               break;
            }
         }
      }

      if (!hasTargetingModule) {
         if (this.level != null && !this.level.isClientSide) {
            player.sendSystemMessage(Component.translatable("message.scguns.turret_needs_targeting_module").withStyle(ChatFormatting.YELLOW));
         }

         return null;
      } else {
         return new ShotgunTurretMenu(id, playerInventory, this);
      }
   }

   @Override
   protected boolean isPowered(BlockState state) {
      return (Boolean)state.getValue(ShotgunTurretBlock.POWERED);
   }
}
