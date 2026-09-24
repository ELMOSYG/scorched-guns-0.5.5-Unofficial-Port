package com.scg2tlm.elmomod.compat.task;

import com.google.common.collect.ImmutableMap;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.scg2tlm.elmomod.SCG2TLMConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;

/**
 * 「空闲补弹」（用户要求）：<b>身边没有敌人、弹匣又不满</b>时，女仆自己把弹匣压满。
 *
 * <h2>为什么需要单独一个行为</h2>
 * <p>换弹本来完全由 {@link MaidSC2GunShootTask} 驱动，而那个行为要求
 * {@code ATTACK_TARGET} 存在（{@code checkExtraStartConditions}）——
 * 所以一波打完、目标消失之后，行为立刻停下来，女仆就顶着一个半空的弹匣站着，
 * 下一波遇敌还得先现换一次（这段时间毫无输出）。</p>
 * <p>本行为的内存要求正好相反：{@code ATTACK_TARGET = VALUE_ABSENT}，
 * 只在<b>没有敌人</b>时运行。优先级排在最后，任何战斗行为都能压过它。</p>
 *
 * <h2>为什么要等一会儿（用户要求，HANDOFF §46）</h2>
 * <p>最初（以及上游 1.20.1 的原始版本）是「目标一消失就立刻换弹」✗ —— 打死后女仆转身就换弹，
 * 看起来像"刚打完就装弹"。现在<b>无敌人满 {@code idle_reload_delay_ticks}（默认 100 刻 = 5 秒）</b>
 * 才开始补弹 ✓。因为本行为只在没有目标时运行 ✓，「它运行了多少刻」就等于「敌人离开多久」✓；
 * 期间若出现敌人，战斗行为抢占 ⇒ {@link #stop} 清零 ⇒ 重新计时 ✓。</p>
 *
 * <h2>和射击行为的关系</h2>
 * <ul>
 *   <li>换弹状态是<b>按女仆共享</b>的（{@code SCG2TLM$RELOAD}），两边用同一套
 *       {@link MaidSC2GunShootTask#tickReload} 推进，所以「换弹到一半目标死了」时
 *       本行为会自动接手把它跑完，不会卡住；</li>
 *   <li>{@code tickReload} 里有「同一游戏刻只递减一次」的守卫，
 *       两个行为即使同刻都 RUNNING 也不会把进度扣两次。</li>
 * </ul>
 */
public class MaidSC2GunIdleReloadTask extends Behavior<EntityMaid> {
    /** 背包扫描（hasAmmoInInventory）不便宜，空闲时也不必每刻判一次。 */
    private static final int CHECK_INTERVAL = 10;
    /**
     * 换弹完成后「举枪」保持几刻再放下。
     *
     * <p>{@code tickReload} 完成时会把 {@code maid.setSwingingArms(true)}（= 举枪，拉栓那一下），
     * 战斗路径里接下来每刻都有射击行为<b>按状态重设</b>这个标志，所以不会卡住；
     * 但空闲补弹时射击行为根本没运行 ⇒ 没人放下来 ⇒ 女仆会<b>一直举着枪</b>（用户报的现象）。
     * 这里留几刻让拉栓动作演完，然后自己放下。</p>
     */
    private static final int SWING_HOLD_TICKS = 8;

    private int checkCooldown = 0;
    private int swingResetTicks = -1;
    /**
     * Ticks spent idle in this run of the behavior.
     *
     * <p>This behavior's memory requirement is {@code ATTACK_TARGET = VALUE_ABSENT}, so while it is
     * running the maid <b>by definition</b> has no target: counting its own ticks measures exactly
     * "how long the enemies have been gone". A new enemy preempts the behavior (any combat behavior
     * outranks it) and {@link #stop} resets the counter, so the wait always starts over from the
     * moment the fight ends again (HANDOFF section 46).</p>
     */
    private int idleTicks = 0;

    public MaidSC2GunIdleReloadTask() {
        super(ImmutableMap.of(
                MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED
        ), 1200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        if (!SCG2TLMConfig.IDLE_RELOAD.get()) return false;
        if (maid.isMaidInSittingPose() || maid.isPassenger()) return false;
        return MaidSC2GunShootTask.shouldIdleReload(maid, maid.getMainHandItem());
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        if (!SCG2TLMConfig.IDLE_RELOAD.get()) return false;
        ItemStack gun = maid.getMainHandItem();
        // 换弹进行中就一定继续（哪怕这时冒出敌人）—— 半途而废等于白干，
        // 而「敌人出现」交给射击行为并行走它的换弹推进即可。
        if (MaidSC2GunShootTask.isReloading(maid)) {
            return com.scg2tlm.elmomod.compat.SC2GunCompat.isSC2Gun(gun);
        }
        return MaidSC2GunShootTask.shouldIdleReload(maid, gun);
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        ItemStack gun = maid.getMainHandItem();
        if (!com.scg2tlm.elmomod.compat.SC2GunCompat.isSC2Gun(gun)) return;

        if (MaidSC2GunShootTask.isReloading(maid)) {
            MaidSC2GunShootTask.ReloadTick rt = MaidSC2GunShootTask.tickReload(maid, gun, gameTime);
            if (rt == MaidSC2GunShootTask.ReloadTick.FINISHED) {
                swingResetTicks = SWING_HOLD_TICKS;
            } else if (rt == MaidSC2GunShootTask.ReloadTick.FAILED) {
                swingResetTicks = 0;
            }
            return;
        }
        // 换弹完成了 → 过几刻把枪放下来（否则会一直举着枪）
        if (swingResetTicks >= 0 && --swingResetTicks <= 0) {
            swingResetTicks = -1;
            maid.setSwingingArms(false);
        }
        idleTicks++;
        if (!MaidSC2GunShootTask.shouldIdleReload(maid, gun)) return;
        // 用户要求：敌人消失后要等一会儿再补弹，而不是敌人一没就立刻换弹
        // （否则人刚倒下、女仆转身就开始换弹）。等待期间若有敌人出现，
        // 战斗行为会抢占本行为 ⇒ stop() 把计数清零 ⇒ 重新计时。
        if (idleTicks < (Integer)SCG2TLMConfig.IDLE_RELOAD_DELAY.get()) return;
        if (checkCooldown > 0) {
            checkCooldown--;
            return;
        }
        checkCooldown = CHECK_INTERVAL;
        MaidSC2GunShootTask.beginReload(maid, gun, gameTime);
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long gameTime) {
        checkCooldown = 0;
        idleTicks = 0;
        swingResetTicks = -1;
        // 放下枪：本行为是「空闲」路径，没有射击行为接着按状态重设这个标志，
        // 不主动清就会一直举着枪（用户报的 bug）。
        maid.setSwingingArms(false);
        // 故意的：<b>不</b>清换弹状态。射击行为会在需要时接手把这次换弹跑完。
    }
}
