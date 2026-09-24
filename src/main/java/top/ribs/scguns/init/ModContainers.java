package top.ribs.scguns.init;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MenuType.MenuSupplier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import top.ribs.scguns.client.screen.AttachmentContainer;

public class ModContainers {
   public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(BuiltInRegistries.MENU, "scguns");
   public static final DeferredHolder<MenuType<AttachmentContainer>, MenuType<AttachmentContainer>> ATTACHMENTS = register("attachments", AttachmentContainer::new);

   public ModContainers() {
      super();
   }

   @SuppressWarnings("unchecked")
   private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<T>, MenuType<T>> register(String id, MenuSupplier<T> factory) {
      DeferredHolder<MenuType<?>, MenuType<T>> holder = REGISTER.register(
         id, () -> new MenuType<>(factory, FeatureFlags.DEFAULT_FLAGS)
      );
      return (DeferredHolder<MenuType<T>, MenuType<T>>) (Object) holder;
   }
}
