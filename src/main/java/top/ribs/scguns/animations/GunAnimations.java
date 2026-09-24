package top.ribs.scguns.animations;

import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.Animation.LoopType;

public final class GunAnimations {
   public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
   public static final RawAnimation CARBINE_IDLE = RawAnimation.begin().thenLoop("carbine_idle");
   public static final RawAnimation SHOOT = RawAnimation.begin().then("shoot", LoopType.PLAY_ONCE).thenLoop("idle");
   public static final RawAnimation SHOOT1 = RawAnimation.begin().then("shoot1", LoopType.PLAY_ONCE).thenLoop("idle");
   public static final RawAnimation CARBINE_SHOOT = RawAnimation.begin().then("carbine_shoot", LoopType.PLAY_ONCE).thenLoop("carbine_idle");
   public static final RawAnimation AIM_SHOOT = RawAnimation.begin().then("aim_shoot", LoopType.PLAY_ONCE).thenLoop("idle");
   public static final RawAnimation AIM_SHOOT1 = RawAnimation.begin().then("aim_shoot1", LoopType.PLAY_ONCE).thenLoop("idle");
   public static final RawAnimation CARBINE_AIM_SHOOT = RawAnimation.begin().then("carbine_aim_shoot", LoopType.PLAY_ONCE).thenLoop("carbine_idle");
   public static final RawAnimation RELOAD = RawAnimation.begin().then("reload", LoopType.PLAY_ONCE).thenLoop("idle");
   public static final RawAnimation CARBINE_RELOAD = RawAnimation.begin().then("carbine_reload", LoopType.PLAY_ONCE).thenLoop("carbine_idle");
   public static final RawAnimation RELOAD_ALT = RawAnimation.begin().then("reload_alt", LoopType.PLAY_ONCE).thenLoop("idle");
   public static final RawAnimation RELOAD_START = RawAnimation.begin().then("reload_start", LoopType.PLAY_ONCE).thenLoop("reload_loop");
   public static final RawAnimation CARBINE_RELOAD_START = RawAnimation.begin()
      .then("carbine_reload_start", LoopType.PLAY_ONCE)
      .thenLoop("carbine_reload_loop");
   public static final RawAnimation RELOAD_LOOP = RawAnimation.begin().then("reload_loop", LoopType.LOOP);
   public static final RawAnimation CARBINE_RELOAD_LOOP = RawAnimation.begin().then("carbine_reload_loop", LoopType.LOOP);
   public static final RawAnimation RELOAD_STOP = RawAnimation.begin().then("reload_stop", LoopType.PLAY_ONCE).thenLoop("idle");
   public static final RawAnimation CARBINE_RELOAD_STOP = RawAnimation.begin().then("carbine_reload_stop", LoopType.PLAY_ONCE).thenLoop("carbine_idle");
   public static final RawAnimation INSPECT = RawAnimation.begin().then("inspect", LoopType.PLAY_ONCE).thenLoop("idle");
   public static final RawAnimation CARBINE_INSPECT = RawAnimation.begin().then("carbine_inspect", LoopType.PLAY_ONCE).thenLoop("carbine_idle");
   public static final RawAnimation DRAW = RawAnimation.begin().then("draw", LoopType.PLAY_ONCE).thenLoop("idle");
   public static final RawAnimation CARBINE_DRAW = RawAnimation.begin().then("carbine_draw", LoopType.PLAY_ONCE).thenLoop("carbine_idle");
   public static final RawAnimation JAM = RawAnimation.begin().then("jam", LoopType.PLAY_ONCE).thenLoop("idle");
   public static final RawAnimation MELEE = RawAnimation.begin().then("melee", LoopType.PLAY_ONCE).thenLoop("idle");
   public static final RawAnimation CARBINE_MELEE = RawAnimation.begin().then("carbine_melee", LoopType.PLAY_ONCE).thenLoop("carbine_idle");
   public static final RawAnimation BAYONET = RawAnimation.begin().then("bayonet", LoopType.PLAY_ONCE).thenLoop("idle");
   public static final RawAnimation CARBINE_BAYONET = RawAnimation.begin().then("carbine_bayonet", LoopType.PLAY_ONCE).thenLoop("carbine_idle");

   public GunAnimations() {
      super();
   }
}
