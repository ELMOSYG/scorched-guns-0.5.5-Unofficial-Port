package top.ribs.scguns.event;

import top.ribs.scguns.Config;

/** One place that answers "may this hit send blood particles" for the whole mod. */
public final class BloodCompat {

   private BloodCompat() {
   }

   /**
    * Whether blood particles may be sent to clients.
    *
    * <p>The player facing option lives in the client config, which a server cannot control. This adds a server side
    * switch so an owner can turn blood off for everyone without asking each player to change their settings.</p>
    */
   public static boolean allowed() {
      return Config.SERVER == null || !Config.SERVER.disableBloodParticles.get();
   }
}
