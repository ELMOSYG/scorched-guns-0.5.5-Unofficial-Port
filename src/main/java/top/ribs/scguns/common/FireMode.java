package top.ribs.scguns.common;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public record FireMode(ResourceLocation id) {
   public static final FireMode SEMI_AUTO = new FireMode(ResourceLocation.fromNamespaceAndPath("scguns", "semi_automatic"));
   public static final FireMode AUTOMATIC = new FireMode(ResourceLocation.fromNamespaceAndPath("scguns", "automatic"));
   public static final FireMode PULSE = new FireMode(ResourceLocation.fromNamespaceAndPath("scguns", "pulse"));
   public static final FireMode BEAM = new FireMode(ResourceLocation.fromNamespaceAndPath("scguns", "beam"));
   public static final FireMode SEMI_BEAM = new FireMode(ResourceLocation.fromNamespaceAndPath("scguns", "semi_beam"));
   public static final FireMode BURST = new FireMode(ResourceLocation.fromNamespaceAndPath("scguns", "burst"));
   public static final FireMode BEAM_BURST = new FireMode(ResourceLocation.fromNamespaceAndPath("scguns", "burst"));
   public static final FireMode AUTOMATIC_BEAM = new FireMode(ResourceLocation.fromNamespaceAndPath("scguns", "automatic_beam"));
   private static final Map<ResourceLocation, FireMode> fireModeMap = new HashMap<>();

   public FireMode(ResourceLocation id) {
      this.id = id;
   }

   public static void registerType(FireMode mode) {
      fireModeMap.putIfAbsent(mode.id(), mode);
   }

   /**
    * True for every mode that fires a beam. {@code AUTOMATIC_BEAM} repeats {@code SEMI_BEAM} at the weapon's rate, so
    * it belongs to this family everywhere the code asks "is this a beam?" - but not where it distinguishes the two,
    * which is why the ammo branch in {@code BeamWeaponHandler} keeps testing {@code BEAM} on its own.
    */
   public static boolean isBeam(FireMode mode) {
      return mode == BEAM || mode == SEMI_BEAM || mode == AUTOMATIC_BEAM;
   }

   public static FireMode getType(ResourceLocation id) {
      return fireModeMap.getOrDefault(id, SEMI_AUTO);
   }

   public int ordinal() {
      return 0;
   }

   static {
      registerType(SEMI_AUTO);
      registerType(AUTOMATIC);
      registerType(PULSE);
      registerType(BEAM);
      registerType(SEMI_BEAM);
      registerType(BURST);
      registerType(BEAM_BURST);
      registerType(AUTOMATIC_BEAM);
   }
}
