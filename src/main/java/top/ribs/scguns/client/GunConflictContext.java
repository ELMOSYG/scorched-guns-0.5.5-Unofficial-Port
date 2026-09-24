package top.ribs.scguns.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import top.ribs.scguns.item.GunItem;

/**
 * Key-bind conflict context for "in game and holding a gun".
 *
 * <p>0.5.5 implemented MrCrayfish's Controllable {@code IBindingContext} so the
 * controller bindings could fall back to something else inside this context.
 * No Controllable build for 1.21.1 exists in this project's dependency set
 * (see {@link top.ribs.scguns.client.handler.ControllerHandler}), so the
 * interface and its {@code conflicts(IBindingContext)} method are gone while
 * the context itself keeps reporting the same "active" state. Re-enabling it
 * means restoring the Controllable dependency in {@code build.gradle} and
 * re-implementing its 1.21 {@code IBindingContext}.</p>
 */
public enum GunConflictContext {
   IN_GAME_HOLDING_WEAPON {
      public boolean isActive() {
         return !KeyConflictContext.GUI.isActive()
            && Minecraft.getInstance().player != null
            && Minecraft.getInstance().player.getMainHandItem().getItem() instanceof GunItem;
      }
   };

   private GunConflictContext() {
   }
}
