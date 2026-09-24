package com.scg2tlm.elmomod.compat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.logging.LogUtils;
import com.scg2tlm.elmomod.SCG2TLMConfig;
import com.scg2tlm.elmomod.compat.task.TaskSC2GunAttack;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import top.ribs.scguns.entity.ai.AIType;
import top.ribs.scguns.entity.ai.GunAttackGoal;
import top.ribs.scguns.item.GunItem;

/**
 * 「让女仆用 SC2 自己的枪手 AI」这个可选项的实现（默认关闭）。
 *
 * <h3>为什么可行</h3>
 * <p>SC2 的枪手怪用的是一条自足的 Goal：{@code top.ribs.scguns.entity.ai.GunAttackGoal<T extends PathfinderMob>}
 * （SC2 自己的用法见 {@code AdjudicatorEntity:213}）。它内部自己管理想/最小交战距离、瞄准、
 * 连发、换弹、走位横移，以及按 {@code AIType} + 难度算命中率；{@code canUse()} 只要求
 * 「有 {@code getTarget()} 且手里是枪」。而 {@code EntityMaid} 是 {@code PathfinderMob}，
 * 满足泛型约束 —— 所以可以直接挂，<b>完全不用碰 TLM 的枪械兼容层</b>。</p>
 *
 * <h3>挂载时机（v2：改成 tick 维护，不再依赖我们注册的行为）</h3>
 * <p>第一版把「挂载」放在任务注册的一个行为里，结果一旦那个行为没跑起来，就表现为
 * 「女仆完全没有攻击 AI」。现在改成由 {@code EntityMaid#tick} 的注入每 5 刻维护一次
 * （{@code EntityMaidShieldMixin} 里调 {@link #maintain(EntityMaid)}），
 * 只依赖三件稳定的事：<b>开关开着</b>、<b>当前任务是我们自己的枪械任务</b>、<b>手里是 SC2 枪</b>。</p>
 *
 * <h3>目标</h3>
 * <p>SC2 的 Goal 读 {@code Mob#getTarget()}，而 TLM/原版的 {@code StartAttacking} 把目标放在
 * 脑内记忆 {@code ATTACK_TARGET} 里 —— 所以这里每 tick 把记忆镜像到 {@code setTarget()}。
 * 没有这一步 SC2 的 Goal 永远进不去（{@code canUse()} 会一直 false）。</p>
 */
public final class Sc2NativeAi {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 已挂上的 Goal（换成别的枪 / 离开任务时要摘掉）。 */
    private static final Map<EntityMaid, Goal> ACTIVE = new WeakHashMap<>();
    /** 挂载时用的那把枪 —— 枪变了（或换成非枪）就要重挂。 */
    private static final Map<EntityMaid, ItemStack> BUILT_FOR = new WeakHashMap<>();
    /** 上一次写日志的状态，避免刷屏。 */
    private static final Map<EntityMaid, String> LAST_STATE = new WeakHashMap<>();

    private Sc2NativeAi() {
    }

    public static boolean isEnabled() {
        return SCG2TLMConfig.NATIVE_SCGUNS_AI.get();
    }

    /** 配置里的性格字符串 → SC2 的 {@code AIType}，读不出来就退回 TACTICAL。 */
    public static AIType resolveAiType() {
        String raw = SCG2TLMConfig.NATIVE_SCGUNS_AI_TYPE.get();
        if (raw != null) {
            for (AIType type : AIType.values()) {
                if (type.name().equalsIgnoreCase(raw.trim())) return type;
            }
        }
        return AIType.TACTICAL;
    }

    /** 当前任务是不是我们自己的枪械任务（本模式只在这个任务里生效）。 */
    private static boolean onOurTask(EntityMaid maid) {
        try {
            return maid.getTask() != null && TaskSC2GunAttack.UID.equals(maid.getTask().getUid());
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean holdingGun(EntityMaid maid) {
        ItemStack gun = maid.getMainHandItem();
        return SC2GunCompat.isSC2Gun(gun) && gun.getItem() instanceof GunItem;
    }

    /**
     * 由 {@code EntityMaid#tick} 注入每 5 刻调用一次：维护 Goal 的挂载，并把
     * {@code ATTACK_TARGET} 镜像到 {@code Mob#setTarget()}。
     */
    public static void maintain(EntityMaid maid) {
        if (maid.level().isClientSide) return;

        if (!isEnabled()) {
            detach(maid, "开关已关闭");
            return;
        }
        if (!onOurTask(maid)) {
            detach(maid, "当前不是 SC2 枪械任务");
            return;
        }
        if (!holdingGun(maid)) {
            detach(maid, "手里不是 SC2 枪");
            mirrorTarget(maid);
            return;
        }

        ItemStack gun = maid.getMainHandItem();
        Goal current = ACTIVE.get(maid);
        ItemStack builtFor = BUILT_FOR.get(maid);
        if (current != null && (builtFor == null || !ItemStack.isSameItemSameComponents(gun, builtFor))) {
            detach(maid, "换枪了");
            current = null;
        }

        if (current == null) {
            try {
                Goal goal = new GunAttackGoal<EntityMaid>(maid, gun.copy(),
                        SCG2TLMConfig.NATIVE_SCGUNS_AI_SPEED.get().floatValue(),
                        resolveAiType(),
                        SCG2TLMConfig.NATIVE_SCGUNS_AI_DIFFICULTY.get());
                maid.goalSelector.addGoal(1, goal);
                ACTIVE.put(maid, goal);
                BUILT_FOR.put(maid, gun.copy());
                logState(maid, "attached:" + gun.getItem() + ":" + resolveAiType());
                LOGGER.info("[scg2_maid_compat] 已给 {} 挂上 SC2 原生枪手 AI（枪={}，性格={}，速度={}，难度={}）",
                        maid.getName().getString(), gun.getHoverName().getString(), resolveAiType(),
                        SCG2TLMConfig.NATIVE_SCGUNS_AI_SPEED.get(), SCG2TLMConfig.NATIVE_SCGUNS_AI_DIFFICULTY.get());
            } catch (Throwable t) {
                LOGGER.warn("[scg2_maid_compat] 挂载 SC2 原生枪手 AI 失败", t);
            }
        }

        mirrorTarget(maid);

        // 每 5 秒把「Goal 挂着没有 / 有没有目标」写一行，方便排查「女仆不攻击」
        if (maid.tickCount % 100 == 0 && ACTIVE.containsKey(maid)) {
            LivingEntity target = maid.getTarget();
            LOGGER.info("[scg2_maid_compat] 原生 AI 自检：{} goal={} target={} 距离={}",
                    maid.getName().getString(),
                    ACTIVE.get(maid) != null,
                    target == null ? "无" : target.getName().getString(),
                    target == null ? "-" : String.format("%.1f", Math.sqrt(maid.distanceToSqr(target))));
        }
    }

    /** 摘掉挂上的 Goal（任务结束 / 换枪 / 关掉开关时）。 */
    public static void detach(EntityMaid maid, String reason) {
        Goal goal = ACTIVE.remove(maid);
        BUILT_FOR.remove(maid);
        if (goal != null) {
            try {
                maid.goalSelector.removeGoal(goal);
            } catch (Throwable ignored) {
                // 已经被清掉了也无所谓
            }
            LOGGER.info("[scg2_maid_compat] 已摘除 {} 的 SC2 原生枪手 AI（{}）", maid.getName().getString(), reason);
            logState(maid, "detached:" + reason);
        }
    }

    private static void mirrorTarget(EntityMaid maid) {
        LivingEntity target = maid.getBrain()
                .getMemory(MemoryModuleType.ATTACK_TARGET)
                .filter(LivingEntity::isAlive)
                .orElse(null);
        if (target != null) {
            // SC2 的 GunAttackGoal 读的是 getTarget()，不是脑内记忆
            maid.setTarget(target);
        } else if (maid.getTarget() != null && !maid.getTarget().isAlive()) {
            maid.setTarget(null);
        }
    }

    private static void logState(EntityMaid maid, String state) {
        String previous = LAST_STATE.put(maid, state);
        if (state.equals(previous)) return;
        LOGGER.info("[scg2_maid_compat] 原生 AI 状态变化：{} -> {}", state, maid.getName().getString());
    }

    /** 调试用：当前这把女仆挂着 Goal 没有。 */
    @Nullable
    public static Goal activeGoalOf(EntityMaid maid) {
        return ACTIVE.get(maid);
    }
}
