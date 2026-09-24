package top.ribs.scguns.common.properties;



import top.ribs.scguns.util.DistHelper;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import org.apache.commons.lang3.tuple.Pair;
import top.ribs.scguns.annotation.Optional;
import top.ribs.scguns.client.util.Easings;
import top.ribs.scguns.debug.Debug;
import top.ribs.scguns.debug.IDebugWidget;
import top.ribs.scguns.debug.IEditorMenu;
import top.ribs.scguns.debug.client.screen.widget.DebugEnum;
import top.ribs.scguns.debug.client.screen.widget.DebugToggle;

public class SightAnimation implements IEditorMenu {
   public static final SightAnimation DEFAULT = new SightAnimation();
   @Optional
   protected Easings viewportCurve = Easings.LINEAR;
   @Optional
   protected Easings sightCurve = Easings.EASE_OUT_QUAD;
   @Optional
   protected Easings fovCurve = Easings.LINEAR;
   @Optional
   protected Easings aimTransformCurve = Easings.EASE_IN_QUAD;

   public SightAnimation() {
      super();
   }

   public Easings getViewportCurve() {
      return this.viewportCurve;
   }

   public Easings getSightCurve() {
      return this.sightCurve;
   }

   public Easings getFovCurve() {
      return this.fovCurve;
   }

   public Easings getAimTransformCurve() {
      return this.aimTransformCurve;
   }

   @Override
   public Component getEditorLabel() {
      return Component.literal("Sight Animation");
   }

   @Override
   public void getEditorWidgets(List<Pair<Component, Supplier<IDebugWidget>>> widgets) {
      DistHelper.runWhenOn(Dist.CLIENT, () -> {
               widgets.add(
                  Pair.of(
                     Component.literal("Debug: ")
                        .withStyle(new ChatFormatting[]{ChatFormatting.BOLD, ChatFormatting.GOLD})
                        .append(Component.literal("Force Aim").withStyle(ChatFormatting.WHITE)),
                     (Supplier<IDebugWidget>)() -> new DebugToggle(Debug.isForceAim(), Debug::setForceAim)
                  )
               );
               widgets.add(
                  Pair.of(
                     Component.literal("Viewport Curve"),
                     (Supplier<IDebugWidget>)() -> new DebugEnum<>(Easings.class, this.viewportCurve, value -> this.viewportCurve = value)
                  )
               );
               widgets.add(
                  Pair.of(
                     Component.literal("Sight Curve"),
                     (Supplier<IDebugWidget>)() -> new DebugEnum<>(Easings.class, this.sightCurve, value -> this.sightCurve = value)
                  )
               );
               widgets.add(
                  Pair.of(
                     Component.literal("FOV Curve"),
                     (Supplier<IDebugWidget>)() -> new DebugEnum<>(Easings.class, this.fovCurve, value -> this.fovCurve = value)
                  )
               );
               widgets.add(
                  Pair.of(
                     Component.literal("Aim Transform Curve"),
                     (Supplier<IDebugWidget>)() -> new DebugEnum<>(Easings.class, this.aimTransformCurve, value -> this.aimTransformCurve = value)
                  )
               );
            }
      );
   }

   public CompoundTag serializeNBT() {
      CompoundTag tag = new CompoundTag();
      tag.putString("ViewportCurve", this.viewportCurve.name().toLowerCase(Locale.ROOT));
      tag.putString("SightCurve", this.sightCurve.name().toLowerCase(Locale.ROOT));
      tag.putString("FovCurve", this.fovCurve.name().toLowerCase(Locale.ROOT));
      tag.putString("AimTransformCurve", this.aimTransformCurve.name().toLowerCase(Locale.ROOT));
      return tag;
   }

   public void deserializeNBT(CompoundTag tag) {
      if (tag.contains("ViewportCurve", 8)) {
         this.viewportCurve = Easings.byName(tag.getString("ViewportCurve"));
      }

      if (tag.contains("SightCurve", 8)) {
         this.sightCurve = Easings.byName(tag.getString("SightCurve"));
      }

      if (tag.contains("FovCurve", 8)) {
         this.fovCurve = Easings.byName(tag.getString("FovCurve"));
      }

      if (tag.contains("AimTransformCurve", 8)) {
         this.aimTransformCurve = Easings.byName(tag.getString("AimTransformCurve"));
      }
   }

   public JsonObject toJsonObject() {
      JsonObject object = new JsonObject();
      if (this.viewportCurve != Easings.LINEAR) {
         object.addProperty("viewportCurve", this.viewportCurve.getName());
      }

      if (this.sightCurve != Easings.EASE_OUT_QUAD) {
         object.addProperty("sightCurve", this.sightCurve.getName());
      }

      if (this.fovCurve != Easings.LINEAR) {
         object.addProperty("fovCurve", this.fovCurve.getName());
      }

      if (this.aimTransformCurve != Easings.EASE_IN_QUAD) {
         object.addProperty("aimTransformCurve", this.aimTransformCurve.getName());
      }

      return object;
   }

   public SightAnimation copy() {
      SightAnimation sightAnimation = new SightAnimation();
      sightAnimation.viewportCurve = this.viewportCurve;
      sightAnimation.sightCurve = this.sightCurve;
      sightAnimation.fovCurve = this.fovCurve;
      sightAnimation.aimTransformCurve = this.aimTransformCurve;
      return sightAnimation;
   }

   public static SightAnimation.Builder builder() {
      return new SightAnimation.Builder();
   }

   public static class Builder {
      private final SightAnimation sightAnimation = new SightAnimation();

      protected Builder() {
         super();
      }

      public SightAnimation.Builder setViewportCurve(Easings viewportCurve) {
         this.sightAnimation.viewportCurve = viewportCurve;
         return this;
      }

      public SightAnimation.Builder setSightCurve(Easings sightCurve) {
         this.sightAnimation.sightCurve = sightCurve;
         return this;
      }

      public SightAnimation.Builder setFovCurve(Easings fovCurve) {
         this.sightAnimation.fovCurve = fovCurve;
         return this;
      }

      public SightAnimation.Builder setAimTransformCurve(Easings aimTransformCurve) {
         this.sightAnimation.aimTransformCurve = aimTransformCurve;
         return this;
      }

      public SightAnimation build() {
         return this.sightAnimation.copy();
      }
   }
}
