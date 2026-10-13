package top.ribs.scguns.client.render.gun.animated;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Whose gun is being drawn right now.
 *
 * <p>Item rendering only receives an {@code ItemStack}, so the gun renderer had to fall back to
 * {@code Minecraft.getInstance().player} wherever it needed a player. That is the local player, which is
 * wrong for everyone else in the world: another player's gun was posed and lit from the local player's
 * synced shooting, reloading and aiming flags, so one player's actions drove every other player's
 * animation and left their bones wherever those animations put them.</p>
 *
 * <p>{@code RenderPlayerEvent} wraps the whole third person render, held item included, so recording the
 * subject there and reading it back from the item renderer gives each player their own state. A single
 * field is enough because client entity rendering is single threaded.</p>
 */
public final class GunRenderContext {
    private static Player subject;

    private GunRenderContext() {
    }

    public static void push(Player player) {
        subject = player;
    }

    public static void pop() {
        subject = null;
    }

    /**
     * The player whose gun is being drawn, or the local player when nothing was recorded - the first person
     * hand and the GUI both draw the local player's own gun, and they run outside the player render event.
     */
    public static Player subject() {
        return subject != null ? subject : Minecraft.getInstance().player;
    }

    /**
     * The subject as a client player, for the arm and skin work, which needs the renderer side type.
     * Null when there is no such player, matching {@code Minecraft#player}.
     */
    public static AbstractClientPlayer clientSubject() {
        Player player = subject();
        return player instanceof AbstractClientPlayer clientPlayer ? clientPlayer : null;
    }
}
