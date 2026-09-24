package com.scg2tlm.elmomod.compat;


import net.neoforged.fml.common.EventBusSubscriber;
import com.scg2tlm.elmomod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import top.ribs.scguns.item.GunItem;

import java.lang.reflect.Method;

// Client-only and registered from ExampleMod behind a dist check (see MaidProjectileHandler for
// why this class is not annotated with @EventBusSubscriber).
public class BetterCombatHandler {
    private static final String BC_MOD_ID = "bettercombat";
    private static Boolean hasBC;
    private static Class<?> animatableClass;
    private static Method stopAttackAnimation;
    private static ItemStack lastMainHand = ItemStack.EMPTY;

    // 1.21 把客户端 tick 拆成了 ClientTickEvent.Pre/Post；原来跑在 END 相位，即现在的 Post，
    // 所以 event.phase 那道判断随之消失（与本体其他 handler 的写法一致）。
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack current = mc.player.getMainHandItem();
        if (ItemStack.matches(lastMainHand, current)) return;
        lastMainHand = current.copy();

        if (!(current.getItem() instanceof GunItem)) return;
        if (!SC2GunCompat.isSC2Gun(current)) return;

        if (hasBC == null) {
            hasBC = ModList.get().isLoaded(BC_MOD_ID);
        }
        if (!hasBC) return;

        stopBCAttackAnimation(mc.player);
    }

    private static void stopBCAttackAnimation(Object player) {
        try {
            if (animatableClass == null) {
                animatableClass = Class.forName("net.bettercombat.client.animation.PlayerAttackAnimatable");
                stopAttackAnimation = animatableClass.getMethod("stopAttackAnimation", float.class);
            }
            if (animatableClass.isInstance(player)) {
                stopAttackAnimation.invoke(player, 0.0f);
            }
        } catch (Exception e) {
            hasBC = false;
        }
    }
}
