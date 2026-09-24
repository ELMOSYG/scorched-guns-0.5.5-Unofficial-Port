package com.scg2tlm.elmomod.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitAttribute;
import com.scg2tlm.elmomod.SCG2TLMConfig;
import com.scg2tlm.elmomod.compat.SC2GunCompat;
import com.scg2tlm.elmomod.compat.Sc2NativeAi;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 女仆与盾牌的两条 SC2 枪械相关规则。
 *
 * <h3>一、双手枪械不能举盾（{@code canUseShield}）</h3>
 * <p>TLM 里「举盾」有两条路，但都过同一个开关 {@code EntityMaid.canUseShield()}：</p>
 * <ol>
 *   <li>{@code MaidUseShieldTask.checkExtraStartConditions} —— 有目标且距离 &lt; 8 格时举盾格挡；</li>
 *   <li>{@code EntityMaid.hurt} —— 被<b>弹射物</b>打中时的被动举盾（{@code passiveUseShieldTick}）。</li>
 * </ol>
 * <p>所以在这里返回 false 就同时管住两条路，不用去分别改任务和受击逻辑。</p>
 *
 * <h3>二、挨子弹也举盾（{@code hurt}）</h3>
 * <p>SC2 的子弹用伤害类型 {@code scguns:bullet}，<b>没有加进 {@code minecraft:is_projectile} 标签</b>
 * （见 {@link SC2GunCompat#isBulletDamage}），于是 TLM 那条只认 {@code IS_PROJECTILE} 的被动举盾
 * 在枪战里从来不触发。这里在 {@code hurt} 开头补一份同样的逻辑：挨 SC2 子弹时也举盾。</p>
 *
 * <p>复制的是 TLM 自己的三步（先看是不是已经举着盾 → {@code startUsingItem(OFF_HAND)} →
 * 把剩余刻数设成 {@code MAID_PASSIVE_USE_SHIELD_TICK} 属性值），所以后续递减、收盾全部沿用 TLM 原逻辑。
 * TLM 原来那段在后面还会跑一次，但它开头也判「是否已举盾」，不会重复动作。</p>
 */
@Mixin(EntityMaid.class)
public class EntityMaidShieldMixin {

    @Inject(method = "canUseShield", at = @At("HEAD"), cancellable = true, remap = false)
    private void scg2tlm$twoHandedNoShield(CallbackInfoReturnable<Boolean> cir) {
        EntityMaid maid = (EntityMaid) (Object) this;
        ItemStack mainHand = maid.getMainHandItem();
        if (SC2GunCompat.isTwoHandedGun(mainHand)) {
            cir.setReturnValue(false);
        }
    }

    // TLM 的 EntityMaid#hurt：1.20.1 用的是 SRG 名 m_6469_（当时 remap=false 也匹配得上），
    // 1.21.1 的 NeoForge 运行期是官方名，SRG 名已经不存在 ⇒ 改成 hurt（javap 核对过
    // `public boolean hurt(DamageSource, float)`）。
    @Inject(method = "hurt", at = @At("HEAD"), remap = false)
    private void scg2tlm$bulletRaisesShield(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!SCG2TLMConfig.BULLET_RAISES_SHIELD.get()) return;
        if (!SC2GunCompat.isBulletDamage(source)) return;

        EntityMaid maid = (EntityMaid) (Object) this;
        if (!maid.canUseShield()) return;
        // 已经举着盾（副手正在使用）就别重复来一遍。
        if (maid.isUsingItem() && maid.getUsedItemHand() == InteractionHand.OFF_HAND) return;

        maid.startUsingItem(InteractionHand.OFF_HAND);
        // 1.21 的 LivingEntity#getAttribute 收 Holder<Attribute>；MAID_PASSIVE_USE_SHIELD_TICK
        // 是 DeferredHolder（本身就 implements Holder），TLM 自己的 EntityMaid#hurt 也是这么传的，
        // 所以这里不再 .get()。
        AttributeInstance attribute = maid.getAttribute(InitAttribute.MAID_PASSIVE_USE_SHIELD_TICK);
        ((EntityMaidShieldAccessor) maid).setPassiveUseShieldTick(attribute != null ? (int) attribute.getValue() : 100);
    }

    /**
     * 「SC2 原生枪手 AI」的维护入口（默认关闭）。
     *
     * <p>挂 {@code GunAttackGoal} 这件事**不能**只依赖「任务注册的某个行为」是否在跑 ——
     * 第一版就是这么做的，结果一旦那个行为没跑起来，表现就是「女仆完全没有攻击 AI」。
     * 改成挂在 {@code EntityMaid#tick} 上，只依赖三件稳定的事：开关开着、当前任务是我们自己的枪械任务、
     * 手里是 SC2 枪；并且每 5 刻才检查一次，开销可忽略。</p>
     */
    // 同上：m_8119_ 是 1.20.1 的 SRG 名，1.21.1 用官方名 tick。
    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void scg2tlm$maintainNativeAi(CallbackInfo ci) {
        if (!SCG2TLMConfig.NATIVE_SCGUNS_AI.get()) return;
        EntityMaid maid = (EntityMaid) (Object) this;
        if (!(maid.level() instanceof ServerLevel)) return;
        if (maid.tickCount % 5 != 0) return;
        Sc2NativeAi.maintain(maid);
    }
}
