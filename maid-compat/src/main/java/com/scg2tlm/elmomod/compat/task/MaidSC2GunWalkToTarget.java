package com.scg2tlm.elmomod.compat.task;

import com.scg2tlm.elmomod.SCG2TLMConfig;
import com.scg2tlm.elmomod.compat.SC2GunCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class MaidSC2GunWalkToTarget extends Behavior<EntityMaid> {
    private final float speed;
    private boolean strafingClockwise;
    private int strafingTime = -1;

    public MaidSC2GunWalkToTarget(float speed) {
        // ATTACK_TARGET 从 VALUE_PRESENT 放宽成 REGISTERED：
        // 「补搜」（回到被新敌人打断的那个最后目击点）发生时女仆<b>没有目标</b>，
        // 但这段路同样得有人走。目标判定仍然在 checkExtraStartConditions 里做，
        // 所以放宽内存要求不会让它在别的场合乱跑。
        super(ImmutableMap.of(
                MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED,
                MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED
        ), 1200);
        this.speed = speed;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        if (maid.isMaidInSittingPose() || maid.isPassenger()) return false;
        if (!SC2GunCompat.isSC2Gun(maid.getMainHandItem())) return false;
        // 跟随模式拴绳（用户要求）：离主人超过半径就得先回去，哪怕目标在理想射程内。
        if (leashTriggered(maid)) return true;
        // 有「待补搜」的点、且当前没有攻击目标（= 这波打完了）→ 没有目标也要走，
        // 这是 CQB 回头清房间的那条腿
        if (!maid.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                && MaidSC2GunShootTask.hasPendingSearch(maid)) {
            return true;
        }
        return maid.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET);
    }

    /**
     * 跟随模式拴绳能不能用：<b>跟随模式</b>（{@code !isHomeModeEnable()}）+ 半径 > 0 + 主人同维度且活着。
     *
     * <p>不满足时（留守模式 / 半径配成 0 / 主人离线、换维度）就没有可锚定的对象，
     * 走位任务退回老规则「距敌 > 8 格不动」—— 否则女仆会追着敌人跑到天边
     * （狙击枪的理想射程是 96 格）。</p>
     */
    static boolean leashAnchorAvailable(EntityMaid maid) {
        if (SCG2TLMConfig.FOLLOW_LEASH_RADIUS.get() <= 0) return false;
        if (maid.isHomeModeEnable()) return false;
        LivingEntity owner = maid.getOwner();
        return owner != null && owner.isAlive() && owner.level() == maid.level();
    }

    /**
     * 跟随模式的拴绳要不要生效（离主人超过半径）。
     *
     * <p>「跟随模式」= TLM 的 {@code EntityMaid#isHomeModeEnable()} 为 false（留守/待命是 true）。
     * 半径为 0 表示关闭。用户选择 8 格：超过就把她拉回主人身边，
     * <b>射击不受影响</b> —— 开火是另一个行为（{@code MaidSC2GunShootTask}），它只要求有目标，
     * 不要求站着不动，所以「跟着玩家边走边打」成立。</p>
     */
    static boolean leashTriggered(EntityMaid maid) {
        if (!leashAnchorAvailable(maid)) return false;
        double radius = SCG2TLMConfig.FOLLOW_LEASH_RADIUS.get();
        return maid.distanceToSqr(maid.getOwner()) > radius * radius;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        if (maid.isMaidInSittingPose() || maid.isPassenger()) return false;
        if (!SC2GunCompat.isSC2Gun(maid.getMainHandItem())) return false;
        // 拴绳生效时不必再看目标距离：这一段路的目标是「回主人身边」
        if (leashTriggered(maid)) return true;
        // 补搜状态：只在「没有目标」时生效，有效性（预算/时效/距离）由 tick 统一管
        if (!maid.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                && MaidSC2GunShootTask.hasPendingSearch(maid)) {
            return !maid.isMaidInSittingPose() && !maid.isPassenger()
                    && SC2GunCompat.isSC2Gun(maid.getMainHandItem());
        }
        if (!checkExtraStartConditions(level, maid)) return false;
        Optional<LivingEntity> targetOpt = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (targetOpt.isEmpty()) return false;
        LivingEntity target = targetOpt.get();
        if (!target.isAlive() || target.isRemoved()) {
            maid.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            maid.setTarget(null);
            return false;
        }
        ItemStack gun = maid.getMainHandItem();
        if (SC2GunCompat.isLineInfantryGun(gun)) {
            return true;
        }
        // 跟丢后的「走向最后目击点」由开火任务判定，本任务负责执行 ——
        // 这里必须无条件接管：目标躲进掩体时距离通常正好落在理想射程内，
        // 走原来的判断会被判成「不用走位」而直接停下，搜索就永远走不出去。
        if (MaidSC2GunShootTask.searchTargetOf(maid).isPresent()) {
            return true;
        }
        boolean outOfAmmo = SC2GunCompat.needsReload(gun) && !SC2GunCompat.hasAmmoInInventory(maid, gun);
        boolean hasBayonet = SC2GunCompat.hasBayonet(gun);
        double dist = maid.distanceTo(target);
        if (outOfAmmo && hasBayonet) {
            // 刺刀模式：持续走位（追击/环绕），不受远程理想距离限制
            return true;
        }
        double idealRange = SC2GunCompat.getIdealRange(gun);
        double minRange = SC2GunCompat.getMinRange(gun);
        return dist > idealRange * 1.1 || dist < minRange * 0.9;
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long gameTime) {
        maid.getNavigation().stop();
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        if (maid.isMaidInSittingPose() || maid.isPassenger()) return;
        ItemStack gun = maid.getMainHandItem();
        if (!SC2GunCompat.isSC2Gun(gun)) return;

        // ---- 跟随模式拴绳：优先于「走到敌人理想射程」----
        // 只设 WALK_TARGET、不碰 LOOK_TARGET：射击行为自己会转过去瞄目标，
        // 所以「跟着玩家走 + 照常开火」两件事可以并行。
        if (leashTriggered(maid)) {
            LivingEntity owner = maid.getOwner();
            if (owner != null) {
                double radius = SCG2TLMConfig.FOLLOW_LEASH_RADIUS.get();
                int closeEnough = (int) Math.max(1.0, radius * 0.5);
                maid.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                        new WalkTarget(new EntityTracker(owner, false), speed, closeEnough));
            }
            return;
        }

        // ---- 补搜：回到被新敌人打断的那个最后目击点（只在「没有攻击目标」时执行）----
        Optional<Vec3> pending = MaidSC2GunShootTask.resumeSearchPointOf(maid);
        if (pending.isPresent()) {
            Vec3 point = pending.get();
            if (maid.distanceToSqr(point) <= 4.0) {
                // 走到了：补搜结束（看得见的话 StartAttacking 会立刻重新锁上它）
                MaidSC2GunShootTask.clearPendingSearch(maid);
                maid.getNavigation().stop();
                maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                return;
            }
            if (!MaidSC2GunShootTask.consumePendingSearchTick(maid, gameTime)) {
                maid.getNavigation().stop();
                maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                return;
            }
            BehaviorUtils.setWalkAndLookTargetMemories(maid,
                    BlockPos.containing(point.x, point.y, point.z), speed, 1);
            return;
        }

        maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> {
            maid.setTarget(target);

            if (SC2GunCompat.isLineInfantryGun(gun)) {
                Vec3 formationPos = SC2GunCompat.getFormationPosition(maid, target, gun);
                if (maid.distanceToSqr(formationPos) > 2.25) {
                    BehaviorUtils.setWalkAndLookTargetMemories(maid, BlockPos.containing(formationPos), speed, 0);
                } else {
                    maid.getNavigation().stop();
                    maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                }
                return;
            }

            double dist = maid.distanceTo(target);

            // 搜索模式：目标已经躲起来（连续失去视野超过配置时长），走向最后目击点。
            // 放在线性步兵判断之后，保持列兵「站定齐射」的设计不被破坏。
            java.util.Optional<Vec3> searchPos = MaidSC2GunShootTask.searchTargetOf(maid);
            if (searchPos.isPresent()) {
                Vec3 seen = searchPos.get();
                BehaviorUtils.setWalkAndLookTargetMemories(maid,
                        BlockPos.containing(seen.x, seen.y, seen.z), speed, 1);
                return;
            }

            boolean hasBayonet = SC2GunCompat.hasBayonet(gun);
            boolean outOfAmmo = SC2GunCompat.needsReload(gun) && !SC2GunCompat.hasAmmoInInventory(maid, gun);
            float meleeReach = hasBayonet ? SC2GunCompat.getMeleeReach(gun) : 0;

            // 刺刀模式（打空备弹 + 有刺刀）：追击到近战范围后环绕敌人走位
            if (outOfAmmo && hasBayonet) {
                if (dist > meleeReach * 1.15) {
                    strafingTime = -1;
                    BehaviorUtils.setWalkAndLookTargetMemories(maid, target, speed * 1.5f, 0);
                } else {
                    strafingTime++;
                    if (strafingTime >= 40) {
                        if (maid.getRandom().nextFloat() < 0.3f) {
                            strafingClockwise = !strafingClockwise;
                        }
                        strafingTime = 0;
                    }
                    maid.getNavigation().stop();
                    maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                    float forward;
                    if (dist < meleeReach * 0.7) {
                        forward = -0.3f;
                    } else if (dist > meleeReach) {
                        forward = 0.3f;
                    } else {
                        forward = 0.0f;
                    }
                    maid.getMoveControl().strafe(forward, strafingClockwise ? 0.3f : -0.3f);
                    maid.setYRot(Mth.rotateIfNecessary(maid.getYRot(), maid.yHeadRot, 0));
                    BehaviorUtils.lookAtEntity(maid, target);
                }
                return;
            }

            // 「距敌 > 8 格就不动」这条在<b>没有拴绳可锚定</b>时保留：留守模式（别为追人跑离岗位）、
            // 半径配成 0、主人离线/换维度。有拴绳时（跟随模式 + 主人在场）不冻结 ——
            // 她照常走位/拉近，由 leashTriggered 用「离主人 8 格」约束，
            // 于是表现是「跟着玩家、边走边打」，而<b>不会被敌人牵着跑</b>（超出 8 格就被拉回主人）。
            // 原来这条对两种情况都生效 —— 结果跟随模式下她超过 8 格就彻底站住
            // （下面 idealRange 那整段走位逻辑被短路），那才是用户最初报的现象。
            if (dist > 8.0 && !SC2GunCompat.isMeleeClassGun(gun) && !leashAnchorAvailable(maid)) {
                maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                maid.getNavigation().stop();
                return;
            }

            double idealRange = SC2GunCompat.getIdealRange(gun);
            double minRange = SC2GunCompat.getMinRange(gun);

            if (meleeReach > 0 && dist <= meleeReach + 1.0) {
                // Target is within bayonet range - hold ground
                maid.getNavigation().stop();
                maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            } else if (dist > idealRange) {
                BehaviorUtils.setWalkAndLookTargetMemories(maid, target, speed, (int)(idealRange * 0.7));
            } else if (dist < minRange) {
                if (hasBayonet && meleeReach > 0) {
                    // Has bayonet - close into melee range instead of backing away
                    BehaviorUtils.setWalkAndLookTargetMemories(maid, target, speed, 0);
                } else {
                    // Within strafing range the strafing task handles positioning, so do not set a walk target
                    maid.getNavigation().stop();
                    maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                }
            } else {
                maid.getNavigation().stop();
                maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            }
        });
    }
}
