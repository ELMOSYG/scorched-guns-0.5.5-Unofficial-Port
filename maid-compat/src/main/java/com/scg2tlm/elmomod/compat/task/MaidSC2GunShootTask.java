package com.scg2tlm.elmomod.compat.task;



import top.ribs.scguns.util.NbtHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import com.scg2tlm.elmomod.SCG2TLMConfig;
import com.scg2tlm.elmomod.compat.SC2AdvancementTriggers;
import com.scg2tlm.elmomod.compat.SC2GunCompat;
import com.scg2tlm.elmomod.mixin.EntityMaidTaskDataInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.IChatBubbleData;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.implement.TextChatBubbleData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import com.google.common.collect.ImmutableMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.item.GunItem;

import java.util.Optional;
import java.util.UUID;

public class MaidSC2GunShootTask extends Behavior<EntityMaid> {
    private int attackCooldown = 0;
    // 换弹进度不再放实例字段：见 SCG2TLM$RELOAD。实例字段会导致多实例之间状态分裂。
    private int reloadFailCooldown = 0;

    /**
     * 每只女仆的换弹状态，<b>按女仆共享</b>，与行为实例解耦。
     *
     * <h3>为什么不能放在行为实例字段里</h3>
     * <p>{@code TaskSC2GunAttack} 在 {@code createBrainTasks} 与 {@code createRideBrainTasks}
     * 里各 {@code new} 了一个本行为，所以同一只女仆可能存在两个实例。换弹状态一旦是
     * <b>实例字段</b>，就会出现「有人在计时、另一个人不知道」的分裂状态。</p>
     *
     * <h3>实测到的死锁（本次修复的对象）</h3>
     * <p>旧实现用一张 {@code RELOADING_GUNS} 表做跨实例去重：一个实例开始换弹时把枪放进去，
     * 另一个实例想开始就<b>直接 return</b>；而唯一的释放点是「本实例 tick 里的倒计时分支」。
     * 于是这条路径会永久卡死：</p>
     * <ol>
     *   <li>实例 A 开始换弹：{@code isReloading(A)=true}，枪进守卫表；</li>
     *   <li>女仆击杀目标 → {@code stop()} 清空 A 的状态（{@code isReloading(A)=false}），
     *       <b>此刻起没有任何实例在倒计时</b>；</li>
     *   <li>新目标出现 → {@code start()}，{@code tick()} 因 {@code isReloading=false} 走不到倒计时分支；</li>
     *   <li>再次 {@code startReload()} → 枪仍在守卫表 → <b>永久 return</b>。</li>
     * </ol>
     * <p>表现就是「换弹动作发生了，但弹药永远不恢复」。现在换弹倒计时按<b>女仆</b>存放，
     * 任何实例都能接手把它跑完，实例之间不再有分歧。</p>
     *
     * <p>注意与 NBT 标签 {@code "scg2tlm:reloading"} 的分工：那个标签只用于<b>动画同步</b>
     * （客户端读它决定播换弹动画），是表现层；这张表才是<b>机制层</b>的唯一真相。</p>
     */
    private static final java.util.Map<EntityMaid, ReloadState> SCG2TLM$RELOAD =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private static final class ReloadState {
        /** 还有几刻换完。<= 0 表示没有进行中的换弹。 */
        int timer;
        /** 上次递减发生在哪一刻 —— 防止两个行为实例（WORK / RIDE_WORK）同刻各减一次。 */
        long lastGameTime = -1L;

        boolean active() {
            return timer > 0;
        }
    }

    private static ReloadState reloadStateOf(EntityMaid maid) {
        return SCG2TLM$RELOAD.computeIfAbsent(maid, m -> new ReloadState());
    }

    /** 这只女仆当前有没有进行中的换弹（供空闲补弹行为判断「要不要接手跑完」）。 */
    static boolean isReloading(EntityMaid maid) {
        ReloadState st = SCG2TLM$RELOAD.get(maid);
        return st != null && st.active();
    }

    /** {@link #tickReload} 的结果。 */
    enum ReloadTick { NOT_RELOADING, IN_PROGRESS, FINISHED, FAILED }

    /**
     * 推进一次进行中的换弹 —— <b>射击行为与「空闲补弹」行为共用</b>。
     *
     * <p>换弹的真相在按女仆共享的 {@link #SCG2TLM$RELOAD} 里，所以任何行为都能接手把它跑完。
     * 两个行为实例（甚至射击行为 + 空闲补弹行为）可能同刻都 RUNNING，
     * 因此用 {@link ReloadState#lastGameTime} 保证<b>同一游戏刻只递减一次</b>。</p>
     */
    static ReloadTick tickReload(EntityMaid maid, ItemStack gun, long gameTime) {
        ReloadState st = SCG2TLM$RELOAD.get(maid);
        if (st == null || !st.active()) return ReloadTick.NOT_RELOADING;
        if (st.lastGameTime == gameTime) return ReloadTick.IN_PROGRESS;
        st.lastGameTime = gameTime;
        if (--st.timer > 0) {
            NbtHelper.getOrCreateTag(gun).putInt("scg2tlm:reloading", st.timer);
            return ReloadTick.IN_PROGRESS;
        }
        boolean ok = SC2GunCompat.reloadFromInventory(maid, gun);
        st.timer = 0;
        NbtHelper.getOrCreateTag(gun).remove("scg2tlm:reloading");
        setReloadAnimFlag(maid, false);
        if (!ok) {
            maid.setSwingingArms(false);
            return ReloadTick.FAILED;
        }
        ResourceLocation gunId = BuiltInRegistries.ITEM.getKey(gun.getItem());
        if (gunId != null && "scguns".equals(gunId.getNamespace())
                && ("grandle_og".equals(gunId.getPath()) || "grandle".equals(gunId.getPath()))) {
            SC2AdvancementTriggers.triggerForMaidOwner(maid, SC2AdvancementTriggers.RELOAD_DING);
        }
        // 换弹完成音效：用配置的 reload_end_sound（默认 scguns:item.bolt.bolt）。
        SC2GunCompat.playReloadEndSound(maid, gun);
        maid.setSwingingArms(true);
        return ReloadTick.FINISHED;
    }

    /**
     * 开始一次换弹（音效 + 取回被打断的进度 + 动画标记），射击行为与空闲补弹共用。
     * 已经在换弹就直接返回。
     */
    static void beginReload(EntityMaid maid, ItemStack gun, long gameTime) {
        ReloadState st = reloadStateOf(maid);
        if (st.active()) return;
        SC2GunCompat.playPreReloadSound(maid, gun);
        SC2GunCompat.playReloadSound(maid, gun);

        // 上次被中断时存下的进度：同一把枪就用它接着走，而不是从头再来。
        int full = SC2GunCompat.getReloadTime(gun);
        int resume = takeSavedReloadProgress(maid, gun, gameTime);
        st.timer = resume > 0 ? Math.min(resume, full) : full;
        st.lastGameTime = -1L;
        if (resume > 0) {
            LOGGER.info("[scg2_maid_compat] {} 接着上次的换弹进度（剩 {} 刻 / 总 {} 刻）",
                    maid.getName().getString(), resume, full);
        }
        maid.setSwingingArms(false);
        NbtHelper.getOrCreateTag(gun).putInt("scg2tlm:reloading", st.timer);
        setReloadAnimFlag(maid, true);
    }

    /** 只改同步数据里的「换弹中」标记，不碰 firing / melee（空闲补弹路径用）。 */
    static void setReloadAnimFlag(EntityMaid maid, boolean reloading) {
        CompoundTag tag = ((EntityMaidTaskDataInvoker) maid).invokeGetSyncTaskData();
        if (tag == null) tag = new CompoundTag();
        if (reloading) {
            tag.putBoolean("scg2tlm:reloading", true);
        } else {
            tag.remove("scg2tlm:reloading");
        }
        ((EntityMaidTaskDataInvoker) maid).invokeSetSyncTaskData(tag);
    }

    /**
     * 「空闲补弹」的判据（用户要求）：<b>没有敌人</b>、弹匣不满、背包里有对应弹药 → 补满。
     *
     * <p>换弹本来就是由射击行为驱动的，而射击行为要求有 {@code ATTACK_TARGET} ——
     * 所以打完之后弹匣半空也不会自己压满，下一波遇敌要先现换一次。
     * 这个方法交给 {@code MaidSC2GunIdleReloadTask} 用。</p>
     *
     * <p>已经在换弹时也返回 true：目标中途死了的话，得有人把这次换弹跑完。</p>
     */
    static boolean shouldIdleReload(EntityMaid maid, ItemStack gun) {
        if (!SC2GunCompat.isSC2Gun(gun)) return false;
        if (SC2GunCompat.isBusyUsingItem(maid)) return false;
        ReloadState st = SCG2TLM$RELOAD.get(maid);
        if (st != null && st.active()) return true;              // 继续把进行中的换弹跑完
        int capacity = SC2GunCompat.computeMaxAmmo(gun);         // 无日志版本（每刻都会调）
        if (capacity <= 0) return false;
        if (top.ribs.scguns.common.Gun.getAmmoCount(gun) >= capacity) return false;  // 满了不用补
        return SC2GunCompat.hasAmmoInInventory(maid, gun);       // 背包里得有对应弹药
    }

    /**
     * 「被打断的换弹进度」——下次用<b>同一把枪</b>换弹时接着走，而不是从头再来。
     *
     * <h3>为什么需要</h3>
     * <p>换弹倒计时只在射击行为运行时的 {@code tick()} 里递减，而行为结束时 {@code stop()}
     * 会把 {@code SCG2TLM$RELOAD} 的计时器清零。于是「换弹中挨打 → 进食 → 行为退出」
     * 会把已经走过的进度全部丢掉，吃完从头换弹 —— 这整段时间女仆毫无输出，直接影响生存。</p>
     *
     * <p>这里在中断点把剩余刻数存下来；{@link #startReload} 再次开始时取回。
     * 键是女仆，值是「那把枪 + 剩余刻数 + 存档时刻」：</p>
     * <ul>
     *   <li>换枪了 → {@code isSameItemSameTags} 不匹配 → 不续（各枪各自算）</li>
     *   <li>隔太久（{@value #SAVED_RELOAD_TTL} 刻 = 30 秒）才回来 → 视为过期，重新换</li>
     *   <li>取回即清 → 只会续一次，不会反复吃旧进度</li>
     * </ul>
     */
    private static final class SavedReload {
        final ItemStack gun;
        final int remaining;
        final long savedAt;

        SavedReload(ItemStack gun, int remaining, long savedAt) {
            this.gun = gun;
            this.remaining = remaining;
            this.savedAt = savedAt;
        }
    }

    private static final java.util.Map<EntityMaid, SavedReload> SCG2TLM$SAVED_RELOAD =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /** 存下的进度最多留 30 秒；隔太久就当没这回事。 */
    private static final long SAVED_RELOAD_TTL = 600L;

    /** 中断时把剩余刻数存起来（没有进行中的换弹就清掉旧存档）。 */
    private static void saveReloadProgress(EntityMaid maid, ItemStack gun, long gameTime) {
        ReloadState st = SCG2TLM$RELOAD.get(maid);
        int remaining = st != null ? st.timer : 0;
        if (remaining <= 0 || !SC2GunCompat.isSC2Gun(gun)) {
            SCG2TLM$SAVED_RELOAD.remove(maid);
            return;
        }
        SCG2TLM$SAVED_RELOAD.put(maid, new SavedReload(gun.copy(), remaining, gameTime));
    }

    /** 取回同一把枪的换弹进度（取走即清；换枪 / 过期 / 没有存档都返回 0）。 */
    private static int takeSavedReloadProgress(EntityMaid maid, ItemStack gun, long gameTime) {
        SavedReload saved = SCG2TLM$SAVED_RELOAD.remove(maid);
        if (saved == null) return 0;
        if (gameTime - saved.savedAt > SAVED_RELOAD_TTL) return 0;
        if (!ItemStack.isSameItemSameComponents(saved.gun, gun)) return 0;
        return saved.remaining;
    }

    /**
     * 每只女仆上次开火的游戏刻。
     *
     * <p>{@code attackCooldown} 是实例字段，两个行为实例会各自计时 → 同一 tick 各自开火
     * → 表现为「一次打出两发」。这里再加一道<b>按女仆</b>的最小开火间隔，
     * 与实例字段无关，两个实例共享，保证一 tick 最多开火一次。</p>
     */
    private static final java.util.Map<EntityMaid, Long> SCG2TLM$LAST_FIRE =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /** 跨实例的最小开火间隔（tick）。 */
    private static final long SCG2TLM$MIN_FIRE_GAP = 1L;
    /**
     * 每只女仆的「目标记忆」，<b>按女仆共享</b>，与行为实例解耦。
     *
     * <h3>为什么必须是静态的（已反编译原版 Brain 确认）</h3>
     * <p>{@code TaskSC2GunAttack} 把本行为<b>注册进两个活动</b>：{@code createBrainTasks} →
     * {@code Activity.WORK}，{@code createRideBrainTasks} → {@code InitEntities.RIDE_WORK}。</p>
     * <ul>
     *   <li>{@code Brain#setActiveActivity} 会 {@code activeActivities.clear()} 后只放
     *       「核心活动 + 这一个非核心活动」，所以两个实例<b>不会长期并存</b>；</li>
     *   <li>但 {@code Brain#tickEachRunningBehavior} 遍历的是
     *       {@code behaviorsByPriority} <b>全表</b>中所有 {@code RUNNING} 的行为，
     *       <b>不按活动过滤</b>（1.20.1 反编译：{@code for (Behavior b : this.getRunningBehaviors())
     *       b.tickOrStop(...)}，而 {@code getRunningBehaviors()} 收集的是所有活动下状态为
     *       {@code RUNNING} 的行为）。并且 {@code Brain#startEachNonRunningBehavior} 对同优先级
     *       的行为是「全都 tryStart」，没有 break。</li>
     * </ul>
     * <p>结论：<b>上马/下马（WORK ⇄ RIDE_WORK 切换）的那一瞬间，两个实例可以同时 RUNNING
     * 并各自 tick</b>。只要 seeTime / 目标记忆是实例字段，两个实例就会各算各的：
     * 一个已经判定「跟丢了、放弃目标」，另一个还在跟 —— 表现就是目标时锁时丢、
     * 放弃判定被另一个实例反复归零。</p>
     *
     * <h3>字段含义</h3>
     * <ul>
     *   <li>{@code seeTime}：连续可见为正、连续不可见为负（仅用于诊断与减速计数）；</li>
     *   <li>{@code lostTicks}：连续失去视野的刻数，重新看到目标立刻归零；</li>
     *   <li>{@code lastSeen}：最后一次<b>真的看到</b>目标时的位置；</li>
     *   <li>{@code searching} / {@code searchTicks}：跟丢后走向最后目击点的状态与已用刻数。</li>
     * </ul>
     */
    static final class TargetMemory {
        UUID targetId;
        Vec3 lastSeen;
        int seeTime;
        int lostTicks;
        int searchTicks;
        boolean searching;
    }

    private static final java.util.Map<EntityMaid, TargetMemory> SCG2TLM$TARGET =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    static TargetMemory targetMemoryOf(EntityMaid maid) {
        return SCG2TLM$TARGET.computeIfAbsent(maid, m -> new TargetMemory());
    }

    /**
     * 走位任务用：失去视野后是否应该走向最后目击点，以及那个点在哪。
     *
     * <p>搜索状态由本行为（开火任务）判定，但<b>执行移动的是走位任务</b> ——
     * 因为本行为的职责是「打不打」，而 {@code MaidSC2GunWalkToTarget} 才是负责走位的那个。</p>
     */
    static Optional<Vec3> searchTargetOf(EntityMaid maid) {
        TargetMemory mem = SCG2TLM$TARGET.get(maid);
        if (mem == null || !mem.searching || mem.lastSeen == null) {
            return Optional.empty();
        }
        return Optional.of(mem.lastSeen);
    }

    /**
     * 战斗期间临时扩大女仆「工作范围（restriction）」的租约表，<b>按女仆共享</b>。
     *
     * <h3>旧实现会永久吃掉工作范围</h3>
     * <p>旧实现把 {@code savedRestrictCenter / savedRestrictRadius / restrictionExpanded}
     * 放在<b>行为实例字段</b>里，并且只在 {@code stop()} 里还原。两个漏洞：</p>
     * <ol>
     *   <li><b>实例互相踩</b>：两个实例并存时（见 {@link TargetMemory}），实例 B 会把 A
     *       已经扩过的半径当成「原值」再扩一次（48 → 96）；随后 A 停下时又把 96 还原成 16，
     *       同一次交火里半径被来回改；</li>
     *   <li><b>stop() 根本没被调用</b>：行为在 RUNNING 状态下被丢弃时原版不会调用
     *       {@code stop()} —— 读档、换任务（{@code refreshBrain} → {@code makeBrain}
     *       会重新 {@code new} 一套行为）、区块卸载都会重建 brain。这些情况下扩圈就
     *       <b>永久留在女仆身上</b>，表现就是「女仆的工作范围莫名其妙变大，而且改不回去」。</li>
     * </ol>
     * <p>现在原值写进女仆自己的 {@code ForgeData}（随存档保存，brain 重建也不会丢），
     * 静态表只负责计数：<b>第一个实例</b>存档并扩圈，<b>最后一个实例</b>还原。
     * 另外每次开始战斗时如果发现「上次的扩圈还没还」，会先按存档里的原值还原再重新扩，
     * 于是泄漏最多只影响一轮交火，不会越滚越大。</p>
     */
    private static final java.util.Map<EntityMaid, Integer> SCG2TLM$RESTRICT_LEASES =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /** 扩圈前的原始限制中心（{@link BlockPos#asLong()}）。 */
    private static final String NBT_ORIG_RESTRICT_POS = "scg2tlm:orig_restrict_pos";
    /** 扩圈前的原始限制半径。 */
    private static final String NBT_ORIG_RESTRICT_RADIUS = "scg2tlm:orig_restrict_radius";
    /** 「当前限制是我扩出来的」标记，用于跨 brain 重建地记账。 */
    private static final String NBT_RESTRICT_EXPANDED = "scg2tlm:restrict_expanded";

    /**
     * 「待补搜」记录：搜索被新敌人打断时，把原目标的最后目击点和剩余搜索时间记下来，
     * 等眼前这波打完再回去搜（CQB 清房间不半途而废）。
     *
     * <h3>为什么必须有它</h3>
     * <p>原版 {@code StartAttacking} 是 {@code absent(ATTACK_TARGET)} 才选目标，所以我们
     * 一旦放弃目标，女仆就<b>再也没有那个敌人的任何记忆</b>了 —— 表现就是
     * 「搜索时冒出别的敌人 → 之前那个彻底忘了」。这张表就是把那份记忆补上。</p>
     *
     * <ul>
     *   <li>{@code point}：被打断时目标的最后目击点；</li>
     *   <li>{@code budget}：还能补搜多少刻（= 被打断时还没用完的搜索时间），
     *       只在真正走回去补搜时才消耗，打新敌人的时间不算；</li>
     *   <li>{@code expireAt}：记忆的保底寿命，避免一个几十秒前的目击点被无限期记着。</li>
     * </ul>
     * <p>只保留<b>一个</b>槽位（后打断的覆盖先前的），这是刻意的：CQB 里同时记住一串
     * 掩体后的敌人只会让走位变得难以预测。</p>
     */
    static final class PendingSearch {
        Vec3 point;
        int budget;
        long expireAt;
    }

    /** 待补搜记忆的保底寿命（1200 刻 = 60 秒）。 */
    private static final int PENDING_SEARCH_MAX_AGE_TICKS = 1200;

    private static final java.util.Map<EntityMaid, PendingSearch> SCG2TLM$PENDING =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /** 走位任务用：当前是否有待补搜记录（只判存在，失效判定在 {@link #pendingSearchPointOf}）。 */
    static boolean hasPendingSearch(EntityMaid maid) {
        return SCG2TLM$PENDING.containsKey(maid);
    }

    /**
     * 走位任务用：现在是否<b>真的该去补搜</b>，以及那个点在哪。
     *
     * <p>关键的一条：<b>当前必须没有攻击目标</b>。因为「补搜」的正确时机是「这波打完了」，
     * 而记忆会一直保留到打完为止 —— 如果这里不看 {@code ATTACK_TARGET}，
     * 女仆就会一边打新敌人一边朝旧目击点走（走位任务会优先执行补搜分支）。</p>
     */
    static Optional<Vec3> resumeSearchPointOf(EntityMaid maid) {
        if (maid.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) return Optional.empty();
        return pendingSearchPointOf(maid);
    }

    /**
     * 走位任务用：取待补搜的点。
     *
     * <p>任何一条失效检查不满足就直接把记忆丢掉并返回空：过期、超出搜索距离。</p>
     *
     * <p><b>刻意不查工作范围（restriction）</b>：补搜发生时女仆已经没有目标、开火任务已经
     * {@code stop()} 过，工作范围早就被租约还原成原值了（战斗期才扩到 48~96）。
     * 如果这里再按原值去卡「目击点在不在工作范围内」，那么所有 16~96 格之间的目击点
     * 都会在补搜刚开始时被误判为越界而丢掉 —— 记了等于没记。
     * 距离上限（{@code target_search_range}）已经足够约束这段路。</p>
     */
    static Optional<Vec3> pendingSearchPointOf(EntityMaid maid) {
        PendingSearch pending = SCG2TLM$PENDING.get(maid);
        if (pending == null || pending.point == null) return Optional.empty();
        if (maid.level().getGameTime() > pending.expireAt) {
            SCG2TLM$PENDING.remove(maid);
            return Optional.empty();
        }
        double max = SCG2TLMConfig.TARGET_SEARCH_RANGE.get();
        if (maid.distanceToSqr(pending.point) > max * max) {
            SCG2TLM$PENDING.remove(maid);
            return Optional.empty();
        }
        return Optional.of(pending.point);
    }

    /** 走位任务用：消耗一刻补搜预算，返回是否还有预算继续走。 */
    static boolean consumePendingSearchTick(EntityMaid maid, long gameTime) {
        PendingSearch pending = SCG2TLM$PENDING.get(maid);
        if (pending == null) return false;
        if (gameTime > pending.expireAt || --pending.budget <= 0) {
            SCG2TLM$PENDING.remove(maid);
            return false;
        }
        return true;
    }

    static void clearPendingSearch(EntityMaid maid) {
        SCG2TLM$PENDING.remove(maid);
    }

    private long lastChatBubbleGameTime = 0;
    private long lastFireChatGameTime = 0;
    private int firingTicks = 0;
    private boolean lastFiringSynced = false;
    private boolean lastReloadSynced = false;
    private int meleeTicks = 0;
    private boolean lastMeleeSynced = false;

    public MaidSC2GunShootTask() {
        super(ImmutableMap.of(
                MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
                MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT
        ), 60, Integer.MAX_VALUE);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        ItemStack gun = maid.getMainHandItem();
        if (!SC2GunCompat.isSC2Gun(gun)) return false;
        // 正在用物品（吃东西 / 用治疗品 / 喝药）时不开枪。举盾格挡不算，见 SC2GunCompat#isBusyUsingItem
        if (SC2GunCompat.isBusyUsingItem(maid)) return false;
        return maid.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        ItemStack gun = maid.getMainHandItem();
        if (!SC2GunCompat.isSC2Gun(gun)) return false;
        // 战斗中被打断去进食时（TLM 的 MaidHealSelfTask 会中途插进来）立刻停火，
        // 否则就是「一边吃东西一边开枪」。
        if (SC2GunCompat.isBusyUsingItem(maid)) return false;
        if (!maid.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) return false;
        LivingEntity target = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
        if (!target.isAlive() || target.isRemoved()) {
            maid.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            maid.setTarget(null);
            return false;
        }
        return true;
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        ItemStack gun = maid.getMainHandItem();
        // 只清「动画标记」，绝不碰 SCG2TLM$RELOAD 里的换弹状态 ——
        // 否则换弹中途被重启的行为实例会把进度抹掉（这正是「击杀后换弹不生效」的一半原因）。
        NbtHelper.getOrCreateTag(gun).remove("scg2tlm:reloading");
        firingTicks = 0;
        lastFiringSynced = false;
        lastReloadSynced = false;
        meleeTicks = 0;
        lastMeleeSynced = false;
        ((EntityMaidTaskDataInvoker) maid).invokeSetSyncTaskData(new CompoundTag());
        acquireRestrictionLease(maid);
        maid.setSwingingArms(true);
        NbtHelper.getOrCreateTag(maid.getMainHandItem()).putBoolean("scg2tlm:in_combat", true);
        if (gameTime - lastChatBubbleGameTime >= 600) {
            lastChatBubbleGameTime = gameTime;
            String[] keys = {
                    "chat_bubble.scg2_maid_compat.spot.0",
                    "chat_bubble.scg2_maid_compat.spot.1",
                    "chat_bubble.scg2_maid_compat.spot.2"
            };
            String key = keys[maid.getRandom().nextInt(keys.length)];
            maid.getChatBubbleManager().addChatBubble(TextChatBubbleData.create(600, Component.translatable(key), IChatBubbleData.TYPE_2, IChatBubbleData.DEFAULT_PRIORITY));
            SoundEvent sound = InitSounds.MAID_FIND_TARGET.get();
            if (sound != null) {
                maid.playSound(sound, 1.0f, 0.8f + maid.getRandom().nextFloat() * 0.4f);
            }
        }
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long gameTime) {
        // 工作范围是「最后一个实例离开时才还」的租约，不是本实例的私有字段
        releaseRestrictionLease(maid);
        ItemStack gun = maid.getMainHandItem();
        if (SC2GunCompat.isSC2Gun(gun) && SC2GunCompat.needsReload(gun) && !SC2GunCompat.hasAmmoInInventory(maid, gun)) {
            showNoAmmoChat(maid, gameTime);
        }
        reloadFailCooldown = 0;
        // 行为结束（例如目标死亡）就结束进行中的换弹：清掉共享状态与动画标记。
        // 关键是别再假设「守卫表由本实例释放」——共享状态就是唯一真相，没有守卫表了。
        //
        // 但「结束」不等于「白干」：如果换弹已经走了一部分，把剩余刻数<b>存起来</b>，
        // 下次用同一把枪换弹时接着走（用户要求：换弹被打断不该从头再来）。
        // 触发这条路径的典型场景：换弹中挨打 → TLM 的 MaidHealSelfTask 插进来进食
        // → 我们的射击行为因「正在用物品」而退出 → 换弹进度就此丢失 →
        // 吃完从头换弹（这整段时间毫无输出）。
        saveReloadProgress(maid, gun, gameTime);
        ReloadState st = SCG2TLM$RELOAD.get(maid);
        if (st != null) {
            st.timer = 0;
        }
        maid.setSwingingArms(false);
        maid.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        NbtHelper.getOrCreateTag(gun).remove("scg2tlm:reloading");
        NbtHelper.getOrCreateTag(gun).remove("scg2tlm:in_combat");
        NbtHelper.getOrCreateTag(gun).remove("scg2tlm:firing");
        firingTicks = 0;
        lastFiringSynced = false;
        lastReloadSynced = false;
        meleeTicks = 0;
        lastMeleeSynced = false;
        ((EntityMaidTaskDataInvoker) maid).invokeSetSyncTaskData(new CompoundTag());
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        ItemStack gun = maid.getMainHandItem();
        if (!SC2GunCompat.isSC2Gun(gun)) return;

        SC2GunCompat.tickEnergyRecharge(maid, gun);

        if (firingTicks > 0 && --firingTicks == 0) {
            syncAnimationFlags(maid);
        }
        if (meleeTicks > 0 && --meleeTicks == 0) {
            syncAnimationFlags(maid);
        }

        // ---- 换弹：状态取自按女仆共享的 SCG2TLM$RELOAD，任何实例都能接手把它跑完 ----
        // 推进逻辑抽到 tickReload 里，空闲补弹行为（MaidSC2GunIdleReloadTask）共用同一套，
        // 所以「目标中途死了 → 射击行为退出」也不会把换弹卡在半路。
        ReloadTick rt = tickReload(maid, gun, gameTime);
        if (rt != ReloadTick.NOT_RELOADING) {
            maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> {
                if (target.isAlive()) {
                    maid.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ());
                }
            });
            if (rt == ReloadTick.FINISHED) {
                attackCooldown = 5;
            } else if (rt == ReloadTick.FAILED) {
                reloadFailCooldown = 100;
                showNoAmmoChat(maid, gameTime);
            }
            return;
        }

        if (reloadFailCooldown > 0) {
            reloadFailCooldown--;
            if (reloadFailCooldown <= 0) {
                if (SC2GunCompat.hasAmmoInInventory(maid, gun)) {
                    reloadFailCooldown = 0;
                } else {
                    showNoAmmoChat(maid, gameTime);
                }
            }
            return;
        }

        boolean needsReload = SC2GunCompat.needsReload(gun);
        boolean hasAmmoInInv = SC2GunCompat.hasAmmoInInventory(maid, gun);

        if (tryBayonetMelee(maid, gun)) {
            return;
        }

        if (needsReload && !hasAmmoInInv) {
            if (!SC2GunCompat.hasBayonet(gun)) {
                showNoAmmoChat(maid, gameTime);
            }
            return;
        }

        if (needsReload) {
            if (hasAmmoInInv) {
                // 只清动画标记，然后这一 tick 不计时地重新开始换弹。
                // 注意：真正「换弹中」的判据是上面的 SCG2TLM$RELOAD，不是这个 NBT 标记，
                // 所以这里清标记不会让进行中的换弹作废。
                if (NbtHelper.getTag(gun) != null && NbtHelper.getTag(gun).contains("scg2tlm:reloading")) {
                    NbtHelper.getTag(gun).remove("scg2tlm:reloading");
                    syncAnimationFlags(maid);
                }
                startReload(maid, gun, gameTime);
            } else {
                maid.setSwingingArms(false);
            }
            return;
        }

        maid.setSwingingArms(true);
        maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> {
            if (!target.isAlive() || maid.distanceToSqr(target) > getMaxTargetRangeSqr(maid, gun)) {
                maid.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                maid.setTarget(null);
                return;
            }
            maid.setTarget(target);
            maid.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ());

            // ---- 视野与目标记忆（按女仆共享，见 TargetMemory 的说明）----
            TargetMemory mem = targetMemoryOf(maid);
            if (!target.getUUID().equals(mem.targetId)) {
                // 换了目标：整体归零，否则会拿着上一个目标的目击点乱走
                mem.targetId = target.getUUID();
                mem.lastSeen = null;
                mem.seeTime = 0;
                mem.lostTicks = 0;
                mem.searchTicks = 0;
                mem.searching = false;
            }

            boolean canSee = maid.canSee(target);
            if (canSee != (mem.seeTime > 0)) {
                mem.seeTime = 0;
            }
            if (canSee) {
                mem.seeTime++;
                // 只有「真的看见」才刷新目击点
                mem.lastSeen = target.position();
                mem.lostTicks = 0;
                mem.searchTicks = 0;
                mem.searching = false;
            } else {
                mem.seeTime--;
                mem.lostTicks++;

                // ---- 打得到谁就打谁：目标看不见了，而旁边还有看得见的敌人 → 换目标 ----
                // 原版 StartAttacking 是 absent(ATTACK_TARGET) 才选目标，我们握着目标时
                // 女仆永远不会自己换人；所以这里必须主动放弃，把「选目标」的活交还给它，
                // 下一 tick 就会锁上那个看得见的敌人。
                //
                // 但「换目标」不等于「忘掉」：CQB 清房间时把刚才那个躲起来的敌人忘掉是不能接受的，
                // 所以先把它记进 SCG2TLM$PENDING（最后目击点 + 剩余搜索时间），
                // 等这波打完女仆会自己走回去补搜。两个开关见 target_search_interruptible /
                // target_search_resume。
                if (SCG2TLMConfig.TARGET_SEARCH_INTERRUPTIBLE.get() && otherEnemyVisible(maid, target)) {
                    rememberPendingSearch(maid, mem, gameTime);
                    abandonTarget(maid);
                    return;
                }
            }

            // ---- 跟丢处理：能换就换，换不了才搜（A/B 混合策略）----
            // 走到这里时「周围没有别的看得见的敌人」（有的话上面已经放弃换人了），
            // 所以可以放心给一个宽松的搜索时长（默认 20 秒）。
            if (mem.lostTicks >= SCG2TLMConfig.TARGET_LOST_SIGHT_TICKS.get()) {
                if (canInvestigate(maid, mem)) {
                    if (mem.searching) {
                        // 搜索时间用尽还是没找到 → 放弃
                        if (++mem.searchTicks >= SCG2TLMConfig.TARGET_SEARCH_TICKS.get()) {
                            abandonTarget(maid);
                            return;
                        }
                    } else {
                        // 第一次越过阈值：交给走位任务走向最后目击点
                        mem.searching = true;
                        mem.searchTicks = 0;
                    }
                } else {
                    // 目击点在工作范围外 / 太远 / 没有有效的目击点 → 直接放弃
                    abandonTarget(maid);
                    return;
                }
            }

            if (attackCooldown > 0) {
                if (--attackCooldown > 0) {
                    return;
                }
            }

            boolean lineInfantry = SC2GunCompat.isLineInfantryGun(gun);
            if (lineInfantry) {
                long volleyPhase = gameTime % SCG2TLMConfig.VOLLEY_INTERVAL.get();
                if (volleyPhase >= 3) {
                    attackCooldown = 1;
                    return;
                }
            }

            // 开火只需要「现在看得见」：原来的 seeTime >= -60 是恒真条件
            // （seeTime 只在 canSee=false 时递减，为负时必然 canSee=false），已删掉。
            if (canSee) {
                // 跨实例去重：两个行为实例不得在同一 tick（或过近的两 tick）同时开火
                Long lastFire = SCG2TLM$LAST_FIRE.get(maid);
                if (lastFire != null && gameTime - lastFire < SCG2TLM$MIN_FIRE_GAP) {
                    return;
                }
                SCG2TLM$LAST_FIRE.put(maid, gameTime);

                maid.swing(InteractionHand.MAIN_HAND);
                attackCooldown = doRangedAttack(maid, target) + SCG2TLMConfig.FIRING_INTERVAL.get();
                firingTicks = 5;
                syncAnimationFlags(maid);
                if (lineInfantry) {
                    attackCooldown = 3;
                }
                if (attackCooldown <= 0) {
                    attackCooldown = 5;
                }
                if (gameTime - lastFireChatGameTime >= 600) {
                    lastFireChatGameTime = gameTime;
                    String[] keys = {
                            "chat_bubble.scg2_maid_compat.fire.0",
                            "chat_bubble.scg2_maid_compat.fire.1",
                            "chat_bubble.scg2_maid_compat.fire.2",
                            "chat_bubble.scg2_maid_compat.fire.3"
                    };
                    String key = keys[maid.getRandom().nextInt(keys.length)];
                    maid.getChatBubbleManager().addChatBubble(TextChatBubbleData.create(600, Component.translatable(key), IChatBubbleData.TYPE_2, IChatBubbleData.DEFAULT_PRIORITY));
                    SoundEvent sound = InitSounds.MAID_RANGE_ATTACK.get();
                    if (sound != null) {
                        maid.playSound(sound, 1.0f, 0.8f + maid.getRandom().nextFloat() * 0.4f);
                    }
                }
            }
        });
    }

    private void startReload(EntityMaid maid, ItemStack gun, long gameTime) {
        // 状态机本体抽到静态的 beginReload（空闲补弹行为共用）
        ReloadState st = SCG2TLM$RELOAD.get(maid);
        boolean alreadyReloading = st != null && st.active();
        beginReload(maid, gun, gameTime);
        if (alreadyReloading) {
            return;   // 跨实例去重：已经在换弹就不重复播音效/气泡
        }
        if (gameTime - lastChatBubbleGameTime >= 600) {
            lastChatBubbleGameTime = gameTime;
            String[] keys = {
                    "chat_bubble.scg2_maid_compat.reload.0",
                    "chat_bubble.scg2_maid_compat.reload.1",
                    "chat_bubble.scg2_maid_compat.reload.2"
            };
            String key = keys[maid.getRandom().nextInt(keys.length)];
            maid.getChatBubbleManager().addChatBubble(TextChatBubbleData.create(600, Component.translatable(key), IChatBubbleData.TYPE_2, IChatBubbleData.DEFAULT_PRIORITY));
            SoundEvent sound = InitSounds.MAID_AI_CHAT.get();
            if (sound != null) {
                maid.playSound(sound, 1.0f, 0.8f + maid.getRandom().nextFloat() * 0.4f);
            }
        }
    }

    private void syncAnimationFlags(EntityMaid maid) {
        boolean firing = firingTicks > 0;
        // 「换弹中」的真相在共享状态里（按女仆），不是本实例字段
        ReloadState st = SCG2TLM$RELOAD.get(maid);
        boolean reloading = st != null && st.active();
        boolean melee = meleeTicks > 0;
        if (firing == lastFiringSynced && reloading == lastReloadSynced && melee == lastMeleeSynced) {
            return;
        }
        lastFiringSynced = firing;
        lastReloadSynced = reloading;
        lastMeleeSynced = melee;
        CompoundTag tag = ((EntityMaidTaskDataInvoker) maid).invokeGetSyncTaskData();
        if (tag == null) {
            tag = new CompoundTag();
        }
        if (firing) {
            tag.putBoolean("scg2tlm:firing", true);
        } else {
            tag.remove("scg2tlm:firing");
        }
        if (reloading) {
            tag.putBoolean("scg2tlm:reloading", true);
        } else {
            tag.remove("scg2tlm:reloading");
        }
        if (melee) {
            tag.putBoolean("scg2tlm:melee", true);
        } else {
            tag.remove("scg2tlm:melee");
        }
        ((EntityMaidTaskDataInvoker) maid).invokeSetSyncTaskData(tag);
    }

    private void showNoAmmoChat(EntityMaid maid, long gameTime) {
        if (gameTime - lastChatBubbleGameTime < 600) return;
        lastChatBubbleGameTime = gameTime;
        String[] keys = {
                "chat_bubble.scg2_maid_compat.no_ammo.0",
                "chat_bubble.scg2_maid_compat.no_ammo.1",
                "chat_bubble.scg2_maid_compat.no_ammo.2",
                "chat_bubble.scg2_maid_compat.no_ammo.3"
        };
        String key = keys[maid.getRandom().nextInt(keys.length)];
        maid.getChatBubbleManager().addChatBubble(TextChatBubbleData.create(600, Component.translatable(key), IChatBubbleData.TYPE_2, IChatBubbleData.DEFAULT_PRIORITY));
        SoundEvent sound = InitSounds.MAID_AI_CHAT.get();
        if (sound != null) {
            maid.playSound(sound, 1.0f, 0.8f + maid.getRandom().nextFloat() * 0.4f);
        }
    }

    private int doRangedAttack(EntityMaid maid, LivingEntity target) {
        try {
            return SC2GunCompat.performGunAttack(maid, target, maid.getMainHandItem());
        } catch (Exception e) {
            return 100;
        }
    }

    private double getMaxTargetRangeSqr(EntityMaid maid, ItemStack gun) {
        if (SC2GunCompat.needsReload(gun) && !SC2GunCompat.hasAmmoInInventory(maid, gun)) {
            return 8.0 * 8.0;
        }
        // 按武器类别来：min(全局 gun_range, 该类索敌半径)。
        // 原来这里用的是写死 64 的 getGunRange()，所以霰弹枪/喷火器也会在 64 格外开火。
        double range = SC2GunCompat.getMaxFireRange(gun);
        return range * range;
    }

    /**
     * 把「被打断的搜索」记进 {@link #SCG2TLM$PENDING}。
     *
     * <p>剩余预算 = 配置的搜索时长 − 本目标已经搜掉的刻数；已经搜完（或配置为 0 = 不搜索）
     * 就不记 —— 那样「回头补搜」本来就是空动作。</p>
     */
    private static void rememberPendingSearch(EntityMaid maid, TargetMemory mem, long gameTime) {
        if (!SCG2TLMConfig.TARGET_SEARCH_RESUME.get()) return;
        int configured = SCG2TLMConfig.TARGET_SEARCH_TICKS.get();
        if (configured <= 0 || mem.lastSeen == null) return;

        PendingSearch pending = new PendingSearch();
        pending.point = mem.lastSeen;
        pending.budget = Math.max(1, configured - mem.searchTicks);
        pending.expireAt = gameTime + PENDING_SEARCH_MAX_AGE_TICKS;
        SCG2TLM$PENDING.put(maid, pending);
    }

    /**
     * 除当前目标之外，是否还有一个「女仆自己能攻击、并且看得见」的敌人。
     *
     * <p>判定直接复用 {@link TaskSC2GunAttack#findValidTarget}，所以跟女仆平时自动选目标的
     * 规则（复仇目标 → 正在打女仆的最近目标 → TLM 传感器）完全一致，不会出现
     * 「这里认为有别人、真放手了却选不到」的错判。</p>
     */
    private static boolean otherEnemyVisible(EntityMaid maid, LivingEntity current) {
        Optional<LivingEntity> other = TaskSC2GunAttack.findValidTarget(maid);
        if (other.isEmpty()) return false;
        LivingEntity candidate = other.get();
        return candidate.isAlive() && !candidate.getUUID().equals(current.getUUID()) && maid.canSee(candidate);
    }

    /**
     * 跟丢之后是否值得走一趟最后目击点。
     *
     * <p>三个条件都要满足：</p>
     * <ol>
     *   <li>搜索时长配置非 0（0 = 失去视野后直接放弃）；</li>
     *   <li>最后目击点在女仆当前的限制范围内 —— 战斗期间限制已经被扩到 48~96，
     *       所以这一条实际是「别为了一个影子跑出工作范围」；</li>
     *   <li>目击点离女仆不超过 {@code target_search_range}，避免为了一个影子长途奔袭。</li>
     * </ol>
     * <p>调用点已经保证「周围没有别的看得见的敌人」——有别人的话上面就换目标了，
     * 所以这里的搜索时长可以给得很宽松（默认 20 秒）。</p>
     */
    private static boolean canInvestigate(EntityMaid maid, TargetMemory mem) {
        if (SCG2TLMConfig.TARGET_SEARCH_TICKS.get() <= 0) return false;
        if (mem.lastSeen == null) return false;
        BlockPos seenPos = BlockPos.containing(mem.lastSeen.x, mem.lastSeen.y, mem.lastSeen.z);
        if (!maid.isWithinRestriction(seenPos)) return false;
        double max = SCG2TLMConfig.TARGET_SEARCH_RANGE.get();
        return maid.distanceToSqr(mem.lastSeen) <= max * max;
    }

    /**
     * 放弃目标：清 ATTACK_TARGET，顺带清掉目标记忆与行走/注视目标。
     *
     * <p>清 {@code WALK_TARGET} 是必要的：否则走位任务留下的「走向最后目击点」会继续生效，
     * 女仆会在没有目标的情况下继续往那边走。</p>
     */
    private static void abandonTarget(EntityMaid maid) {
        maid.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        maid.setTarget(null);
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        TargetMemory mem = SCG2TLM$TARGET.get(maid);
        if (mem != null) {
            mem.targetId = null;
            mem.lastSeen = null;
            mem.seeTime = 0;
            mem.lostTicks = 0;
            mem.searchTicks = 0;
            mem.searching = false;
        }
    }

    /** 战斗期间把工作范围扩到 3 倍（下限 48、上限 96）。 */
    private static int expandedRestrictRadius(float original) {
        return Math.min(Math.max(48, (int) original * 3), 96);
    }

    /**
     * 申请一份「工作范围租约」，第一个实例负责存档原值并扩圈。
     *
     * <p>如果女仆身上还留着上一轮没还的扩圈标记（读档 / 行为被丢弃），先按存档里的原值
     * 还原再重新扩 —— 这样泄漏最多影响一轮交火，不会越滚越大。</p>
     */
    private static void acquireRestrictionLease(EntityMaid maid) {
        CompoundTag pd = maid.getPersistentData();
        int leases = SCG2TLM$RESTRICT_LEASES.merge(maid, 1, Integer::sum);
        if (leases > 1) return;                 // 已经有实例在管这件事
        if (pd.getBoolean(NBT_RESTRICT_EXPANDED)) {
            restoreRestriction(maid);           // 先还上次的，再重新借
        }
        if (!maid.hasRestriction()) return;

        BlockPos center = maid.getRestrictCenter();
        float radius = maid.getRestrictRadius();
        pd.putLong(NBT_ORIG_RESTRICT_POS, center.asLong());
        pd.putFloat(NBT_ORIG_RESTRICT_RADIUS, radius);
        pd.putBoolean(NBT_RESTRICT_EXPANDED, true);
        maid.restrictTo(center, expandedRestrictRadius(radius));
    }

    /** 归还租约：最后一个实例离开时才真正还原工作范围。 */
    private static void releaseRestrictionLease(EntityMaid maid) {
        Integer current = SCG2TLM$RESTRICT_LEASES.get(maid);
        int left = (current == null ? 0 : current) - 1;
        if (left > 0) {
            SCG2TLM$RESTRICT_LEASES.put(maid, left);
            return;
        }
        SCG2TLM$RESTRICT_LEASES.remove(maid);
        restoreRestriction(maid);
    }

    /** 按女仆 ForgeData 里存档的原值还原工作范围（可跨 brain 重建、读档）。 */
    private static void restoreRestriction(EntityMaid maid) {
        CompoundTag pd = maid.getPersistentData();
        if (!pd.getBoolean(NBT_RESTRICT_EXPANDED)) return;
        long pos = pd.getLong(NBT_ORIG_RESTRICT_POS);
        float radius = pd.getFloat(NBT_ORIG_RESTRICT_RADIUS);
        pd.remove(NBT_RESTRICT_EXPANDED);
        pd.remove(NBT_ORIG_RESTRICT_POS);
        pd.remove(NBT_ORIG_RESTRICT_RADIUS);
        if (radius > 0.0f) {
            maid.restrictTo(BlockPos.of(pos), (int) radius);
        }
    }

    private boolean tryBayonetMelee(EntityMaid maid, ItemStack gun) {
        if (!SCG2TLMConfig.BAYONET_MELEE.get()) return false;
        if (!SC2GunCompat.hasBayonet(gun)) return false;
        Optional<LivingEntity> targetOpt = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (targetOpt.isEmpty()) return false;
        LivingEntity target = targetOpt.get();
        if (!target.isAlive()) return false;
        if (maid.distanceTo(target) > SC2GunCompat.getMeleeReach(gun)) return false;

        if (attackCooldown > 0) {
            attackCooldown--;
        } else {
            maid.swing(InteractionHand.MAIN_HAND);
            attackCooldown = SC2GunCompat.performMeleeAttack(maid, target, gun);
            meleeTicks = 15;
            syncAnimationFlags(maid);
            if (attackCooldown <= 0) {
                attackCooldown = 5;
            }
        }
        maid.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ());
        return true;
    }
}
