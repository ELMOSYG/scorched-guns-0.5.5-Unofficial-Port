package top.ribs.scguns.client.handler;

/**
 * Gamepad / controller support.
 *
 * <p>0.5.5 built this on MrCrayfish's Controllable (input events plus button
 * bindings). No Controllable build for 1.21.1 exists in this project's
 * dependency set, so the integration is disabled rather than rewritten: the
 * entry points the rest of the client calls stay, they just report "no
 * controller input", and guns fall back to keyboard and mouse.</p>
 *
 * <p>Re-enabling it means restoring the Controllable dependency in
 * {@code build.gradle} and porting the bindings to its 1.21 API.</p>
 */
public class ControllerHandler {
    public static void init() {
        // no controller integration available
    }

    /** Whether the controller aim button is held. */
    public static boolean isAiming() {
        return false;
    }

    /** Whether the controller fire button is held. */
    public static boolean isShooting() {
        return false;
    }
}
