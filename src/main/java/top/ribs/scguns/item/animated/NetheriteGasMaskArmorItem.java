package top.ribs.scguns.item.animated;


import net.minecraft.core.Holder;
import java.util.function.Consumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.Animation.LoopType;
import software.bernie.geckolib.animation.PlayState;
import top.ribs.scguns.client.render.armor.NetheriteGasMaskArmorRenderer;

public class NetheriteGasMaskArmorItem extends ArmorItem implements GeoItem {
   private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

   public NetheriteGasMaskArmorItem(Holder<ArmorMaterial> pMaterial, Type pType, Properties pProperties) {
      super(pMaterial, pType, pProperties);
   }

   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(new IClientItemExtensions() {
         private NetheriteGasMaskArmorRenderer renderer;

         @NotNull
         public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
            if (this.renderer == null) {
               this.renderer = new NetheriteGasMaskArmorRenderer();
            }

            this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
            return this.renderer;
         }
      });
   }

   private PlayState predicate(AnimationState animationState) {
      animationState.getController().setAnimation(RawAnimation.begin().then("animation.netherite_gas_mask.idle", LoopType.LOOP));
      return PlayState.CONTINUE;
   }

   public void registerControllers(ControllerRegistrar controllerRegistrar) {
      controllerRegistrar.add(new AnimationController[]{new AnimationController(this, "controller", 0, this::predicate)});
   }

   public boolean isFireResistant() {
      return true;
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
