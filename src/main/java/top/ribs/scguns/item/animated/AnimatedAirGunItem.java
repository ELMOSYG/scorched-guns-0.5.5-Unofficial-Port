package top.ribs.scguns.item.animated;




import net.minecraft.world.item.Item;
import top.ribs.scguns.util.DistHelper;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.interfaces.IAirGun;
import top.ribs.scguns.util.AirSourceHelper;

public class AnimatedAirGunItem extends AnimatedGunItem implements IAirGun {
   public AnimatedAirGunItem(
      Properties properties,
      String path,
      SoundEvent reloadSoundMagOut,
      SoundEvent reloadSoundMagIn,
      SoundEvent reloadSoundEnd,
      SoundEvent boltPullSound,
      SoundEvent boltReleaseSound
   ) {
      super(properties, path, reloadSoundMagOut, reloadSoundMagIn, reloadSoundEnd, boltPullSound, boltReleaseSound);
   }

   @Override
   public boolean isBarVisible(ItemStack stack) {
      Boolean result = (Boolean)DistHelper.callWhenOn(Dist.CLIENT, () -> {
            Player player = getClientPlayer();
            if (player == null) {
               return stack.isDamaged();
            } else {
               AirSourceHelper.AirSource airSource = AirSourceHelper.getBestAirSource(player);
               return airSource.isAvailable() || stack.isDamaged();
            }
         });
      return result != null && result;
   }

   @Override
   public int getBarWidth(ItemStack stack) {
      Integer width = (Integer)DistHelper.callWhenOn(Dist.CLIENT, () -> {
            Player player = getClientPlayer();
            if (player != null) {
               AirSourceHelper.AirInfo airInfo = AirSourceHelper.getAirInfo(player);
               if (airInfo.sourceType() != AirSourceHelper.AirSource.Type.NONE) {
                  return airInfo.barWidth();
               }
            }

            return Math.round(13.0F - (float)stack.getDamageValue() * 13.0F / (float)stack.getMaxDamage());
         });
      return width != null ? width : 0;
   }

   @Override
   public int getBarColor(ItemStack stack) {
      Integer color = (Integer)DistHelper.callWhenOn(Dist.CLIENT, () -> {
            Player player = getClientPlayer();
            if (player != null) {
               AirSourceHelper.AirInfo airInfo = AirSourceHelper.getAirInfo(player);
               if (airInfo.sourceType() != AirSourceHelper.AirSource.Type.NONE) {
                  return airInfo.barColor();
               }
            }

            if ((double)stack.getDamageValue() >= (double)stack.getMaxDamage() / 1.5) {
               return Objects.requireNonNull(ChatFormatting.RED.getColor());
            } else {
               float f = Math.max(0.0F, ((float)stack.getMaxDamage() - (float)stack.getDamageValue()) / (float)stack.getMaxDamage());
               return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
            }
         });
      return color != null ? color : Mth.hsvToRgb(1.0F, 1.0F, 1.0F);
   }

   @Override
   public void appendHoverText(ItemStack stack, Item.TooltipContext world, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, world, tooltip, flag);
      Gun gun = this.getModifiedGun(stack);
      int airUsage = gun.getProjectile().getEnergyUse();
      tooltip.add(
         Component.translatable("info.airgun.air_usage")
            .append(": ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.valueOf(airUsage)).withStyle(ChatFormatting.WHITE))
      );
      if (world.level() != null && world.level().isClientSide) {
         Player player = getClientPlayer();
         if (player != null) {
            AirSourceHelper.AirSource airSource = AirSourceHelper.getBestAirSource(player);
            switch (airSource.getType()) {
               case CREATE_BACKTANK:
                  tooltip.add(Component.translatable("info.airgun.using_backtank").withStyle(ChatFormatting.GREEN));
                  break;
               case AIR_CANISTER:
                  tooltip.add(Component.translatable("info.airgun.using_canister").withStyle(ChatFormatting.AQUA));
                  break;
               case NONE:
               default:
                  tooltip.add(Component.translatable("info.airgun.requires_air_source").withStyle(ChatFormatting.RED));
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   private static Player getClientPlayer() {
      return Minecraft.getInstance().player;
   }
}
