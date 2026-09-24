package top.ribs.scguns.client.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import top.ribs.scguns.Config;
import top.ribs.scguns.init.ModEffects;

/**
 * The white overlay a stun grenade puts over the screen while the player is blinded.
 *
 * <p>0.5.5 drew this from a {@code GameRenderer} mixin injecting after the first
 * {@code ProfilerFiller.popPush} call. The injection itself still applies on 1.21.1 - the mixin
 * loads and Mixin reports no failure - but that point in the refactored 1.21.1
 * {@code GameRenderer.render(DeltaTracker, boolean)} now comes <b>before</b> the world is drawn,
 * so the overlay was painted and then painted over: the flashbang blinded the player server-side
 * and nothing appeared on screen. That is the "the flashbang does nothing" report.</p>
 *
 * <p>Drawing it as a GUI layer above everything is the 1.21.1 way to be on top of the world and
 * the HUD for certain, without depending on where in a vanilla method an injection lands. The
 * fade maths are the ones 0.5.5 used, with the same two config values.</p>
 */
@EventBusSubscriber(modid = "scguns", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BlindnessOverlay {
    private static final ResourceLocation LAYER_ID =
        ResourceLocation.fromNamespaceAndPath("scguns", "blindness_overlay");

    private BlindnessOverlay() {
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, BlindnessOverlay::render);
    }

    private static void render(GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        MobEffectInstance effect = player.getEffect(ModEffects.BLINDED);
        if (effect == null) {
            return;
        }

        int fadeThreshold = (Integer)Config.SERVER.alphaFadeThreshold.get();
        float percent = fadeThreshold <= 0
            ? 1.0F
            : Math.min((float)effect.getDuration() / (float)fadeThreshold, 1.0F);
        int alpha = (int)((double)(percent * (float)((Integer)Config.SERVER.alphaOverlay.get()).intValue()) + 0.5);

        guiGraphics.fill(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(),
            (alpha << 24) | 0xFFFFFF);
    }
}
