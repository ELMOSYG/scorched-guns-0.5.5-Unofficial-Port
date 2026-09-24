package top.ribs.scguns.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.ribs.scguns.common.MobGuideHelper;
import top.ribs.scguns.entity.monster.SupplyScampEntity;
import top.ribs.scguns.init.ModEntities;

public class ScampPackageItem extends Item {
   public ScampPackageItem(Properties properties) {
      super(properties);
   }

   public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (!world.isClientSide) {
         ServerLevel serverLevel = (ServerLevel)world;
         HitResult result = player.pick(10.0, 0.0F, false);
         if (result.getType() == Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult)result;
            BlockPos pos = blockHit.getBlockPos().relative(blockHit.getDirection());
            Vec3 spawnPos = new Vec3((double)pos.getX() + 0.5, (double)pos.getY() + 1.0, (double)pos.getZ() + 0.5);
            SupplyScampEntity supplyScamp = new SupplyScampEntity((EntityType<? extends TamableAnimal>)ModEntities.SUPPLY_SCAMP.get(), serverLevel);
            supplyScamp.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            supplyScamp.tame(player);
            supplyScamp.setOrderedToSit(true);
            supplyScamp.setSitting(true);
            serverLevel.addFreshEntity(supplyScamp);
            ItemStack guideBook = MobGuideHelper.createGuideBook((EntityType<?>)ModEntities.SUPPLY_SCAMP.get());
            if (guideBook != null && !player.getInventory().add(guideBook)) {
               player.drop(guideBook, false);
            }

            itemStack.shrink(1);
         }
      }

      return InteractionResultHolder.sidedSuccess(itemStack, world.isClientSide());
   }

   @Override
   public void appendHoverText(@NotNull ItemStack stack, Item.TooltipContext level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
      super.appendHoverText(stack, level, tooltip, flag);
      tooltip.add(Component.translatable("info.scguns.mob_guide").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}));
   }
}
