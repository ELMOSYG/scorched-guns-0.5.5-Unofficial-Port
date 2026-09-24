package top.ribs.scguns.event;



import net.neoforged.bus.api.ICancellableEvent;
import top.ribs.scguns.util.NbtHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import top.ribs.scguns.init.ModSyncedDataKeys;
import top.ribs.scguns.item.animated.AnimatedGunItem;

public class GunFireEvent extends PlayerEvent implements ICancellableEvent {
   private final ItemStack stack;

   public GunFireEvent(Player player, ItemStack stack) {
      super(player);
      this.stack = stack;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public boolean isClient() {
      return this.getEntity().getCommandSenderWorld().isClientSide();
   }

   public static class Post extends GunFireEvent {
      public Post(Player player, ItemStack stack) {
         super(player, stack);
         if (NbtHelper.getTag(stack) != null && stack.getItem() instanceof AnimatedGunItem gunItem) {
            long id = GeoItem.getId(stack);
            AnimationController<GeoAnimatable> animationController = (AnimationController<GeoAnimatable>)gunItem.getAnimatableInstanceCache()
               .getManagerForId(id)
               .getAnimationControllers()
               .get("controller");
            animationController.forceAnimationReset();
            if ((Boolean)ModSyncedDataKeys.AIMING.getValue(player)) {
               animationController.tryTriggerAnimation("aim_shoot");
            } else {
               animationController.tryTriggerAnimation("shoot");
            }
         }
      }

      public LivingEntity getShooter() {
         return this.getEntity();
      }
   }

   public static class Pre extends GunFireEvent {
      public Pre(Player player, ItemStack stack) {
         super(player, stack);
      }
   }
}
