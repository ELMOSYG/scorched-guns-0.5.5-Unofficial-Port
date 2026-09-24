package com.scg2tlm.elmomod.mixin;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.ribs.scguns.blockentity.PolarGeneratorBlockEntity;
import top.ribs.scguns.util.Caps;

import java.util.ConcurrentModificationException;
import java.util.function.Consumer;

@Mixin(PolarGeneratorBlockEntity.class)
public class PolarGeneratorBlockEntityMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 1.20.1 里 {@code tick} 写的是 {@code LazyOptional#ifPresent}，本 mixin 把它整个包一层，
     * 只为在邻居的 {@code ConcurrentModificationException} 上兜底（见类名下的历史原因：
     * 能量转移过程中邻居的 handler 被并发改动时，原版那一 tick 会直接崩）。
     *
     * <p>移植到 NeoForge 后 {@code LazyOptional} 没了，同样的调用点变成了本体的
     * {@link Caps#ifPresent}（可空值 + {@link Consumer}，见 {@code PolarGeneratorBlockEntity#tick}），
     * 所以这里重定向的就是它，行为一字未改。</p>
     */
    @Redirect(
        method = "tick",
        at = @At(value = "INVOKE", target = "Ltop/ribs/scguns/util/Caps;ifPresent(Ljava/lang/Object;Ljava/util/function/Consumer;)V", remap = false),
        remap = false
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void redirectIfPresent(Object instance, Consumer consumer) {
        try {
            Caps.ifPresent(instance, consumer);
        } catch (ConcurrentModificationException e) {
            LOGGER.warn("Caught ConcurrentModificationException in PolarGenerator energy transfer, skipping this tick");
        }
    }
}
