package top.ribs.scguns.item.attachment.impl;


import top.ribs.scguns.util.DistHelper;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import org.apache.commons.lang3.tuple.Pair;
import top.ribs.scguns.debug.IDebugWidget;
import top.ribs.scguns.debug.IEditorMenu;
import top.ribs.scguns.debug.client.screen.widget.DebugSlider;
import top.ribs.scguns.interfaces.IGunModifier;

public class Scope extends Attachment implements IEditorMenu {
   protected float aimFovModifier;
   protected float additionalZoom;
   protected double reticleOffset;
   protected boolean stable;
   protected double viewFinderDist;

   private Scope() {
      super();
   }

   private Scope(float additionalZoom, double reticleOffset, IGunModifier... modifier) {
      super(modifier);
      this.aimFovModifier = 1.0F;
      this.additionalZoom = additionalZoom;
      this.reticleOffset = reticleOffset;
   }

   private Scope(float aimFovModifier, float additionalZoom, double reticleOffset, boolean stable, double viewFinderDist, IGunModifier... modifiers) {
      super(modifiers);
      this.aimFovModifier = aimFovModifier;
      this.additionalZoom = additionalZoom;
      this.reticleOffset = reticleOffset;
      this.stable = stable;
      this.viewFinderDist = viewFinderDist;
   }

   public float getFovModifier() {
      return this.aimFovModifier;
   }

   @Override
   public Component getEditorLabel() {
      return Component.translatable("Scope");
   }

   @Override
   public void getEditorWidgets(List<Pair<Component, Supplier<IDebugWidget>>> widgets) {
      DistHelper.runWhenOn(Dist.CLIENT, () -> {
               widgets.add(
                  Pair.of(
                     Component.translatable("Aim FOV Modifier"),
                     (Supplier<IDebugWidget>)() -> new DebugSlider(
                           0.0, 1.0, (double)this.aimFovModifier, 0.05, 3, value -> this.aimFovModifier = value.floatValue()
                        )
                  )
               );
               widgets.add(
                  Pair.of(
                     Component.translatable("Zoom (Legacy)"),
                     (Supplier<IDebugWidget>)() -> new DebugSlider(
                           0.0, 0.5, (double)this.additionalZoom, 0.05, 3, value -> this.additionalZoom = value.floatValue()
                        )
                  )
               );
               widgets.add(
                  Pair.of(
                     Component.translatable("Reticle Offset"),
                     (Supplier<IDebugWidget>)() -> new DebugSlider(0.0, 4.0, this.reticleOffset, 0.025, 4, value -> this.reticleOffset = value)
                  )
               );
               widgets.add(
                  Pair.of(
                     Component.translatable("View Finder Distance"),
                     (Supplier<IDebugWidget>)() -> new DebugSlider(0.0, 5.0, this.viewFinderDist, 0.05, 3, value -> this.viewFinderDist = value)
                  )
               );
            }
      );
   }

   public Scope copy() {
      Scope scope = new Scope();
      scope.aimFovModifier = this.aimFovModifier;
      scope.additionalZoom = this.additionalZoom;
      scope.reticleOffset = this.reticleOffset;
      scope.stable = this.stable;
      scope.viewFinderDist = this.viewFinderDist;
      return scope;
   }

   public static Scope.Builder builder() {
      return new Scope.Builder();
   }

   public static class Builder {
      private float aimFovModifier = 1.0F;
      private final float additionalZoom = 0.0F;
      private final double reticleOffset = 0.0;
      private final boolean stable = false;
      private final double viewFinderDist = 0.0;
      private IGunModifier[] modifiers = new IGunModifier[0];

      private Builder() {
         super();
      }

      public Scope.Builder aimFovModifier(float fovModifier) {
         this.aimFovModifier = fovModifier;
         return this;
      }

      public Scope.Builder modifiers(IGunModifier... modifiers) {
         this.modifiers = modifiers;
         return this;
      }

      public Scope build() {
         return new Scope(this.aimFovModifier, 0.0F, 0.0, false, 0.0, this.modifiers);
      }
   }
}
