package com.scg2tlm.elmomod.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.scg2tlm.elmomod.SCG2TLMConfig;
import com.scg2tlm.elmomod.SCGExtraCompatHelper;
import com.scg2tlm.elmomod.compat.SC2HealCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.ribs.scguns.entity.projectile.ProjectileEntity;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Mixin(ProjectileEntity.class)
public class ProjectileEntityMixin {

    /**
     * 治疗弹识别：{@code impactEffect == minecraft:instant_health}。
     *
     * <p>SC2 的医疗枪（十字军）配置里 {@code projectile.impactEffect} 就是
     * {@code minecraft:instant_health}，且 {@code damage=0.0}。用它作为判据，
     * 让治疗弹豁免下面的友军拦截。</p>
     *
     * <p><b>异常时返回 false</b>：宁可保守地拦住（保持原有的友伤保护），
     * 也不能因为判据出错而误伤友军。</p>
     */
    private static boolean scg2tlm$isHealingProjectile(ProjectileEntity self) {
        try {
            var projectile = self.getProjectile();
            if (projectile == null) return false;
            ResourceLocation effect = projectile.getImpactEffect();
            return effect != null && SC2HealCompat.INSTANT_HEALTH.equals(effect);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 候选实体是不是「发射者自己」。
     *
     * <p>弹丸的出膛点常常落在射击者自己的碰撞箱内，而 SC2 是靠
     * {@code canHitEntity}（{@code PROJECTILE_TARGETS} 谓词）来排除射手的。
     * 但我们的治疗弹<b>绕过了友军过滤</b>，如果那个谓词本身不排除射手，
     * 就会出现「女仆的治疗弹打在自己身上」——弹药白费、主人反而治不到。</p>
     *
     * <p>因此这里显式排除射手。用<b>身份比较</b>（{@code ==}）而不是 {@code equals}，
     * 语义最严格：只有恰好是发射者本人才排除。</p>
     *
     * <p>注意：{@code getOwner()} 与 {@code getShooter()} 都试一遍，
     * 不同弹丸类二者可能只填一个。</p>
     */
    private static boolean scg2tlm$isShooter(ProjectileEntity self, Entity candidate) {
        if (candidate == null) return false;
        try {
            return self.getOwner() == candidate || self.getShooter() == candidate;
        } catch (Throwable t) {
            return false;
        }
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true, remap = false)
    private void scg2tlm$onHitEntity(Entity target, Vec3 hitPos, Vec3 startPos, Vec3 endPos, boolean headshot, CallbackInfo ci) {
        ProjectileEntity self = (ProjectileEntity) (Object) this;
        Entity shooter = self.getOwner();

        // 治疗弹豁免：它的作用就是给友军回血，必须能命中友军
        boolean healing = scg2tlm$isHealingProjectile(self);

        // 但绝不作用于发射者自己（否则女仆给自己回血、白白浪费弹药与一次施法）
        if (healing && scg2tlm$isShooter(self, target)) {
            ci.cancel();
            return;
        }

        if (target instanceof Player && shooter instanceof EntityMaid) {
            if (!healing) {
                ci.cancel();
            }
            return;
        }

        if (!(target instanceof EntityMaid victim)) return;
        if (healing) return;   // 治疗弹对友方女仆同样放行

        if (shooter instanceof EntityMaid maid) {
            Optional<? extends LivingEntity> a = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
            Optional<? extends LivingEntity> b = victim.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
            if (a.isPresent() && b.isPresent() && a.get() == b.get()) {
                ci.cancel();
            }
        } else if (shooter instanceof Player player) {
            if (player.getUUID().equals(victim.getOwnerUUID())) {
                ci.cancel();
            }
        }
    }

    @Redirect(method = {"findEntityOnPath", "findEntitiesOnPath"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"),
            remap = false)
    private List<Entity> scg2tlm$filterFriendlyMaidEntities(Level level, Entity projectileEntity, AABB aabb,
                                                            Predicate<? super Entity> original,
                                                            Vec3 startVec, Vec3 endVec) {
        ProjectileEntity self = (ProjectileEntity) (Object) this;
        // 治疗弹不做友军过滤：否则弹丸会穿过需要治疗的主人/友方女仆。
        // 但必须排除发射者自己 —— 弹丸出膛点常落在女仆自己的碰撞箱内，
        // 否则治疗弹会先打中自己（弹药白费、主人反而治不到）。
        //
        // 签名必须是「被调用方实例 + 实参 + 目标方法自身的参数」：
        //   (Level, Entity, AABB, Predicate, Vec3, Vec3)
        // 1.20.1 那份源码写的是 (Level, Entity, AABB, Predicate, ProjectileEntity) —— 尾参数
        // 位置放错了（Mixin 会在 validateParams 里报 "Found unexpected argument type
        // ...ProjectileEntity at index 0"，然后直接崩启动）。所以这条重定向从来没生效过：
        // 成品 1.0.8 的 jar 里这个类只有 onHitEntity 一个方法，是后来才加的。
        if (scg2tlm$isHealingProjectile(self)) {
            return level.getEntities(projectileEntity, aabb,
                    ((Predicate<Entity>) original).and(e -> !scg2tlm$isShooter(self, e)));
        }
        return level.getEntities(projectileEntity, aabb,
                ((Predicate<Entity>) original).and(e -> !isFriendlyMaid(self, e)));
    }

    /**
     * 友军闸门的<b>公共漏斗</b>：{@code ProjectileEntity.getHitResult(Entity, Vec3, Vec3)}
     * 是 SC2 所有子弹找目标的必经之路 —— 基类的 {@code findEntityOnPath} / {@code findEntitiesOnPath}
     * 都调它，连 {@code LightningProjectileEntity} 自己覆盖的那两个版本也照样调它，
     * 而且<b>没有任何子弹类覆盖 {@code getHitResult}</b>。
     *
     * <p>为什么非要放这里：15 个子弹类自己覆盖了 {@code onHitEntity} 而且<b>不调 {@code super}</b>
     * （高级弹 {@code AdvancedRoundProjectileEntity}、火箭、等离子、FireRound、OsborneSlug、
     * Beowulf、BrassBolt、GibbsRound、KrahgRound、MicroJet、Ramrod、SculkCell、Shotball、
     * BearPackShell、LightningProjectile）—— 挂在基类 {@code onHitEntity} 上的拦截对它们全部失效，
     * 这就是「高级弹仍然打女仆/友军」的原因。</p>
     *
     * <p>返回 {@code null} = 这一发不命中该实体：子弹直接穿过去，连带跳过伤害、护盾破防、
     * 命中效果（impactEffect）与元素爆裂。</p>
     */
    @Inject(method = "getHitResult", at = @At("HEAD"), cancellable = true, remap = false)
    private void scg2tlm$skipFriendlyTarget(Entity candidate, Vec3 startVec, Vec3 endVec,
                                            CallbackInfoReturnable<ProjectileEntity.EntityResult> cir) {
        ProjectileEntity self = (ProjectileEntity) (Object) this;

        // 治疗弹必须能命中友军（否则治不到主人/友方女仆），只排除发射者自己
        if (scg2tlm$isHealingProjectile(self)) {
            if (scg2tlm$isShooter(self, candidate)) {
                cir.setReturnValue(null);
            }
            return;
        }

        if (isFriendlyFireTarget(self, candidate)) {
            cir.setReturnValue(null);
        }
    }

    /**
     * 「这一发不该命中它」的唯一判据（命中判定层）。
     *
     * <p>把原来分散在三处的规则合成一份，并补上漏判的同主人女仆：</p>
     * <ul>
     *   <li>{@link #isFriendlyMaid}：同主人的女仆、或正在打同一个目标的女仆
     *       （原来只写在基类 {@code findEntityOnPath} 的 {@code getEntities} 重定向里）；</li>
     *   <li>女仆不打玩家（原来只在基类 {@code onHitEntity} 拦截里）；</li>
     *   <li>玩家不打自己名下的女仆（原来只在 {@code MaidProjectileHandler} 的 LivingIncomingDamageEvent 里）；</li>
     *   <li>scgextra 同阵营不打（原来只在 {@code ExampleMod.onFactionFriendlyFire} 的 LivingIncomingDamageEvent 里）——
     *       这一条仍受 {@code prevent_faction_friendly_fire} 与「装了 scgextra」两个前提约束。</li>
     * </ul>
     * <p>事件层那两处<b>保留</b>做兜底（无主弹体、或别的 mod 直接调 hurt 的情况）。</p>
     */
    private static boolean isFriendlyFireTarget(ProjectileEntity projectile, Entity candidate) {
        if (candidate == null) return false;
        Entity owner = projectile.getOwner();
        if (!(owner instanceof LivingEntity shooter)) return false;

        // 发射者自己：基类本来就排除，覆盖了 onHitEntity 的子弹不一定
        if (candidate == shooter || candidate == projectile.getShooter()) return true;
        if (!(candidate instanceof LivingEntity victim)) return false;

        if (shooter instanceof EntityMaid) {
            // 女仆不打玩家；也不打自己人（同主人 / 同 ATTACK_TARGET）
            return victim instanceof Player || isFriendlyMaid(projectile, victim);
        }
        if (shooter instanceof Player player) {
            return victim instanceof EntityMaid maid
                    && maid.getOwnerUUID() != null && maid.getOwnerUUID().equals(player.getUUID());
        }

        if (!SCG2TLMConfig.PREVENT_FACTION_FRIENDLY_FIRE.get()) return false;
        if (!net.neoforged.fml.ModList.get().isLoaded("scgextra")) return false;
        return SCGExtraCompatHelper.isFriendlies(shooter, victim);
    }

    private static boolean isFriendlyMaid(ProjectileEntity projectile, Entity candidate) {
        if (!(candidate instanceof EntityMaid victim)) return false;
        Entity shooter = projectile.getOwner();
        if (!(shooter instanceof EntityMaid shooterMaid)) {
            if (shooter instanceof Player player) {
                return victim.getOwnerUUID() != null && victim.getOwnerUUID().equals(player.getUUID());
            }
            return false;
        }
        if (victim.getOwnerUUID() != null && victim.getOwnerUUID().equals(shooterMaid.getOwnerUUID())) {
            return true;
        }
        Optional<? extends LivingEntity> a = shooterMaid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        Optional<? extends LivingEntity> b = victim.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        return a.isPresent() && b.isPresent() && a.get() == b.get();
    }
}
