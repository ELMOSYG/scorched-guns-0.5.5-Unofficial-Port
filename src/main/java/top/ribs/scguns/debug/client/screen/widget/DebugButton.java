package top.ribs.scguns.debug.client.screen.widget;

import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.Component;
import top.ribs.scguns.debug.IDebugWidget;

public class DebugButton extends Button implements IDebugWidget {
   private final Supplier<Boolean> enabled;

   public DebugButton(Component label, OnPress onPress) {
      super(0, 0, 0, 20, label, onPress, DEFAULT_NARRATION);
      this.enabled = () -> true;
   }

   public DebugButton(Component label, OnPress onPress, Supplier<Boolean> enabled) {
      super(0, 0, 0, 20, label, onPress, DEFAULT_NARRATION);
      this.enabled = enabled;
   }

   protected void renderWidget(GuiGraphics pGuiGraphics, int mouseX, int mouseY, float partialTicks) {
      this.active = this.enabled.get();
      super.renderWidget(pGuiGraphics, mouseX, mouseY, partialTicks);
   }
}
