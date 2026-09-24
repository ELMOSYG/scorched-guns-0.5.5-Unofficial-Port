package com.scg2tlm.elmomod.client;


import net.neoforged.fml.common.EventBusSubscriber;
import com.github.tartaricacid.touhoulittlemaid.api.event.client.DefaultGeckoAnimationEvent;
import com.github.tartaricacid.touhoulittlemaid.api.event.client.DefaultGeckoAnimationEvent.AnimationType;
import com.scg2tlm.elmomod.ExampleMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

// Client-only; registered from ExampleMod behind both a dist check and the TLM check, so a
// dedicated server (or a client without TLM) never loads this class.
public class DefaultGeckoAnimationInjector {
    @SubscribeEvent
    public static void onDefaultGeckoAnimation(DefaultGeckoAnimationEvent event) {
        event.addAnimation(AnimationType.TAC, ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "animation/melee.animation.json"));
    }
}
