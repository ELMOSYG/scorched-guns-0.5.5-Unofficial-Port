package com.scg2tlm.elmomod.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * {@code EntityMaid} 私有字段入口。
 *
 * <p>目前只需要 {@code passiveUseShieldTick}：被动举盾的剩余刻数。TLM 自己在
 * {@code hurt} 里挨到弹射物时把它设成属性值，然后在 tick 里递减、到点收盾 ——
 * 我们让「挨子弹」走同一条路，所以也要能写这个字段（见 {@link EntityMaidShieldMixin}）。</p>
 */
@Mixin(EntityMaid.class)
public interface EntityMaidShieldAccessor {

    @Accessor(value = "passiveUseShieldTick", remap = false)
    void setPassiveUseShieldTick(int ticks);
}
