package com.scg2tlm.elmomod.compat;



import top.ribs.scguns.util.NbtHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import com.scg2tlm.elmomod.SCG2TLMConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import net.neoforged.neoforge.common.ItemAbilities;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import top.ribs.scguns.util.GunModifierHelper;
import top.ribs.scguns.util.GunEnchantmentHelper;
import top.ribs.scguns.util.ScEnchants;
import top.ribs.scguns.util.Caps;
import top.ribs.scguns.util.MobType;
import top.ribs.scguns.init.ModEnchantments;
import top.ribs.scguns.entity.projectile.ProjectileEntity;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.Animation;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.AnimationBuilder;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.ILoopType;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.controller.AnimationController;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import top.ribs.scguns.common.Gun;
import top.ribs.scguns.common.Gun.WeaponType;
import top.ribs.scguns.common.GripType;
import top.ribs.scguns.init.ModTags;
import top.ribs.scguns.item.BayonetItem;
import top.ribs.scguns.item.attachment.IAttachment;
import top.ribs.scguns.entity.ai.AIGunEvent;
import top.ribs.scguns.entity.throwable.ThrowableGrenadeEntity;
import top.ribs.scguns.item.BeaconGrenadeItem;
import top.ribs.scguns.item.GasGrenadeItem;
import top.ribs.scguns.item.GrenadeItem;
import top.ribs.scguns.item.AmmoBoxItem;
import top.ribs.scguns.item.ammo_boxes.CreativeAmmoBoxItem;
import top.ribs.scguns.item.GunItem;
import top.ribs.scguns.item.animated.AnimatedGunItem;

public final class SC2GunCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Class<?> ENERGY_GUN_CLASS = initEnergyGunClass();

    private static Class<?> initEnergyGunClass() {
        try {
            return Class.forName("top.ribs.scguns.common.item.gun.RechargeableEnergyGunItem");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static final Set<String> SC2_MINIGUN_GUNS = Set.of(
            "thunderhead", "gattaler", "weevil", "hullbreaker",
            "cr4k_mining_laser", "flayed_god", "spitfire", "cyclone"
    );
    private static final Set<String> CC_MINIGUN_GUNS = Set.of(
            "electrothermal_autocannon"
    );
    private static final Set<String> SC2_RPG_GUNS = Set.of(
            "kiln_gun", "scratches", "dozier_rl", "terra_incognita"
    );
    private static final Set<String> CC_RPG_GUNS = Set.of(
            "lustre"
    );
    private static final Set<String> OREG_MINIGUN_GUNS = Set.of(
            "electrum_spoolgun", "mauvite_beam_rifle"
    );
    private static final Set<String> OREG_RPG_GUNS = Set.of(
            "electrum_bomber", "mauvite_launcher"
    );

    public static final String SCGUNS_NS = "scguns";
    public static final String SCCAVES_NS = "scguns_cnc";
    public static final String OREGUNIZED_NS = "scguns_oregunized";

    /** SC2 射弹类所在的包（兜底判断用；信号弹/钩爪/毒气云这几个不在 ProjectileEntity 链上）。 */
    private static final String SCGUNS_PROJECTILE_PACKAGE = "top.ribs.scguns.entity.projectile";

    /**
     * 这个物品是不是「SC2 体系的枪」。
     *
     * <h2>判据：类型，而不是命名空间</h2>
     * <p>SC2 自己就是这么判的 —— {@code NetworkGunManager.prepare} 里
     * {@code BuiltInRegistries.ITEM.getValues().stream().filter(item -> item instanceof GunItem)}，
     * 它遍历<b>全部</b>注册物品做这个判断。实测证据：</p>
     * <ul>
     *   <li>{@code AnimatedGunItem} / {@code ScorchedWeapon} 都 {@code extends GunItem}；</li>
     *   <li>SC2 本体 {@code GunItem} 的 16 个子类<b>全是枪</b>
     *       （AirGun / Energy / Underwater / Silenced / DiamondSteel / DualWield / Sculk …），
     *       没有一个刺刀、弹药之类；</li>
     *   <li>附属的枪定义都在 {@code data/<附属modId>/guns/}，即物品位于附属自己的命名空间。</li>
     * </ul>
     *
     * <h2>为什么不再维护命名空间白名单</h2>
     * <p>曾经的实现是「{@code scguns} / {@code scguns_cnc} / {@code scguns_oregunized}
     * + 一张手写集合」。但 SC2 的装载约定是<b>枪定义放在自己 modId 的命名空间下</b>，
     * 于是「每来一个新附属就多一个新命名空间」是结构性的，白名单必然一直欠债
     * （实测就先后为 {@code scguns_cnc}、{@code scgunsww1} 各补过一次）。</p>
     * <p>改成类型判据后<b>零维护</b>：任何走 SC2 体系的附属（含以后新出的）自动生效。</p>
     *
     * <h2>顺带修掉的一个假阳性</h2>
     * <p>旧的白名单只看命名空间，不看它是不是枪：{@code scguns:syringe}、{@code scguns:gun_frame}
     * 这类同命名空间的非枪物品也会被判为 true。凡是<b>只</b>调用本方法、没有额外
     * {@code instanceof GunItem} 的地方都会被带偏（例：女仆受击反击的判定）。类型判据没有这个问题。</p>
     *
     * <h2>性能</h2>
     * <p>不劣于旧实现，实际略优：{@code instanceof} 是 JVM 类型检查，
     * 而旧实现要先做一次 {@code BuiltInRegistries.ITEM.getKey} 注册表反查。</p>
     */
    public static boolean isSC2Gun(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof GunItem;
    }

    /**
     * 「排队枪毙时代的古董枪」名单 —— 只影响 {@link #isLineInfantryGun} 那个配置开关。
     *
     * <p>这里的 {@code path} 是相对物品 id 的<b>路径</b>，与命名空间无关，所以
     * {@code scguns:musket} 之类的原版 SC2 古董枪照旧命中。但 {@code scgunsww1} 那些
     * 一战的枪（{@code mosin_nagant}、{@code luger} …）路径不同名，<b>不会</b>落进来。</p>
     *
     * <p>这是有意保留的：{@code LINE_INFANTRY} 是<b>玩法设计选择</b>（哪些枪该站成横队），
     * 不是「这物品是不是枪」这类可推导的事实，因此不适合做成自动判据。</p>
     */
    public static boolean isAntiqueGun(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) return false;
        String path = key.getPath();
        return "musket".equals(path) || "flintlock_pistol".equals(path)
                || "repeating_musket".equals(path) || "laser_musket".equals(path)
                || "vulcanic_repeater".equals(path) || "turnpike".equals(path)
                || "forlorn_hope".equals(path);
    }

    public static boolean isLineInfantryGun(ItemStack stack) {
        return SCG2TLMConfig.LINE_INFANTRY.get() && isAntiqueGun(stack);
    }

    public static Vec3 getFormationPosition(EntityMaid maid, LivingEntity target, ItemStack gun) {
        double range = getIdealRange(gun);
        Entity owner = maid.getOwner();
        Vec3 anchor = owner != null ? owner.position() : maid.position();
        Vec3 enemyPos = target.position();
        Vec3 n = enemyPos.subtract(anchor);
        double hd = Math.sqrt(n.x * n.x + n.z * n.z);
        Vec3 normal;
        if (hd < 0.001) {
            normal = new Vec3(0, 0, 1);
        } else {
            normal = new Vec3(n.x / hd, 0, n.z / hd);
        }
        Vec3 lineDir = new Vec3(-normal.z, 0, normal.x);
        double forward = Math.min(range, Math.max(4.0, hd * 0.6));
        Vec3 center = enemyPos.subtract(normal.scale(forward));
        double spacing = SCG2TLMConfig.FORMATION_SPACING.get();
        int size = SCG2TLMConfig.FORMATION_SIZE.get();
        Vec3 rel = maid.position().subtract(center);
        double proj = rel.x * lineDir.x + rel.z * lineDir.z;
        double slotF = Math.round(proj / spacing);
        int half = (size - 1) / 2;
        slotF = Math.max(-half, Math.min(half, slotF));
        return center.add(lineDir.scale(slotF * spacing));
    }

    public static boolean isCCGun(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && SCCAVES_NS.equals(key.getNamespace());
    }

    public static boolean isOregunizedGun(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && OREGUNIZED_NS.equals(key.getNamespace());
    }

    public static ResourceLocation getGunId(ItemStack stack) {
        if (isSC2Gun(stack)) {
            return BuiltInRegistries.ITEM.getKey(stack.getItem());
        }
        return null;
    }

    public static boolean isEnergyGun(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (ENERGY_GUN_CLASS == null) return false;
        return ENERGY_GUN_CLASS.isInstance(stack.getItem());
    }

    /**
     * 插件能量枪（{@code RechargeableEnergyGunItem}）的「FE → 弹匣」回充参数读取器。
     *
     * <p>这三个 getter 是插件自己的方法，<b>不参与 SRG 重映射</b>，所以按名字反射即可；
     * 拿不到就退化成「不做 FE 回充」，绝不让女仆的枪在这里抛异常。</p>
     */
    private static final Method ENERGY_REQUIRED_GETTER = initEnergyGetter("getEnergyRequired");
    private static final Method REFILL_COOLDOWN_GETTER = initEnergyGetter("getRefillCooldown");
    private static final Method USES_OVERHEAT_GETTER = initEnergyGetter("getUsesOverheat");
    private static final Method RELOAD_RECHARGE_MULT_GETTER = initEnergyGetter("getReloadRechargeTimeMult");

    /** 过热上限。插件把 {@code Math.min(heat + 1, 50.0f)} 硬编码在 {@code ServerPlayHandlerMixin} 里，没有配置项。 */
    private static final float MAX_HEAT = 50.0f;

    private static Method initEnergyGetter(String name) {
        if (ENERGY_GUN_CLASS == null) return null;
        try {
            return ENERGY_GUN_CLASS.getMethod(name);
        } catch (NoSuchMethodException | SecurityException e) {
            LOGGER.warn("SC2GunCompat: RechargeableEnergyGunItem 没有 {}()，FE 回充退化为不生效", name);
            return null;
        }
    }

    /**
     * 女仆手里 FE 能量枪的「用 FE 换弹」—— <b>照抄插件自己的 {@code RechargeableEnergyGunItem.inventoryTick}</b>。
     *
     * <h2>为什么必须由我们代跑</h2>
     * <p>插件的扣电/回充逻辑写在 {@code Item.inventoryTick}（SRG {@code m_6883_}）里，而
     * {@code inventoryTick} <b>只有玩家背包会调</b>（{@code Inventory.tick}）。女仆不是玩家，
     * 这段代码对女仆一次都不会执行 —— 插件眼里女仆手上的枪永远是「没被 tick 过」的状态。</p>
     * <p>这里按 {@code EntityMaid} 手动跑同一套判定，语义与玩家完全一致：</p>
     * <pre>
     * stored &gt;= energyRequired &amp;&amp; AmmoCount &lt; capacity &amp;&amp; !IsShooting
     *     → RechargeCounter -= 1；减到 &lt;= 0 时 extractEnergy(energyRequired) + AmmoCount += 1 + RechargeCounter = refillCooldown
     * 否则
     *     → RechargeCounter = refillCooldown
     * </pre>
     *
     * <h2>和旧实现的区别（用户报告：女仆用 FE 枪无限弹药）</h2>
     * <p>旧实现是 {@code energy.receiveEnergy(rate, false)} —— <b>无中生有</b>给枪充电（默认 10 FE/刻），
     * 于是枪上的能量条永远是满的；现在只从枪<b>自己存的那点电</b>里扣，
     * 电从哪来由玩家负责（充电器充电 / 换核心），我们不再造电。</p>
     * <p>{@code energyRequired} / {@code refillCooldown} 读的是插件配置的实时值
     * （默认：lustre 10000 FE/40 刻，electrothermal_autocannon 750/2，scatterer 1000/10），
     * 玩家在插件配置里改数值，女仆这边跟着变。</p>
     */
    public static void tickEnergyRecharge(EntityMaid maid, ItemStack stack) {
        if (!isEnergyGun(stack)) return;
        if (ENERGY_REQUIRED_GETTER == null || REFILL_COOLDOWN_GETTER == null) return;

        Item item = stack.getItem();
        int energyRequired = readIntGetter(ENERGY_REQUIRED_GETTER, item);
        if (energyRequired <= 0) return;
        int refillCooldown = Math.max(0, readIntGetter(REFILL_COOLDOWN_GETTER, item));

        CompoundTag tag = NbtHelper.getOrCreateTag(stack);

        // 散热也写在插件的 inventoryTick 里，同样只有玩家会跑。
        // 不补这段，电热自动炮那类「持续射击会过热」的枪在女仆手里热量只涨不退，射速被永久压住。
        if (maid.tickCount % 10 == 0 && Boolean.TRUE.equals(readGetter(USES_OVERHEAT_GETTER, item))) {
            float heat = tag.getFloat("HeatLevel");
            tag.putFloat("HeatLevel", Math.max(0, (int) heat - 1));
        }

        int capacity = computeMaxAmmo(stack);
        int ammoCount = tag.getInt("AmmoCount");
        int rechargeCounter = tag.getInt("RechargeCounter");

        // 1.21 / NeoForge：能力不再包一层 LazyOptional，直接返回可空的值；
        // 能量能力也从 ForgeCapabilities.ENERGY 挪到了 Capabilities.EnergyStorage.ITEM。
        IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        int stored = Caps.map(energy, IEnergyStorage::getEnergyStored, 0);

        if (stored >= energyRequired && ammoCount < capacity && !tag.getBoolean("IsShooting")) {
            tag.putInt("RechargeCounter", rechargeCounter - 1);
            if (rechargeCounter <= 0) {
                Caps.ifPresent(energy, e -> e.extractEnergy(energyRequired, false));
                tag.putInt("AmmoCount", Mth.clamp(ammoCount + 1, 0, capacity));
                tag.putInt("RechargeCounter", refillCooldown);
            }
        } else {
            tag.putInt("RechargeCounter", refillCooldown);
        }
    }

    /**
     * FE 能量枪「开火之后」要做的事 —— <b>照抄插件 {@code ServerPlayHandlerMixin.scguns_cnc$handleShoot}</b>。
     *
     * <h2>为什么女仆这边必须自己补（用户报告：过热机制对女仆不生效）</h2>
     * <p>插件的热量<b>只涨不在这里</b>涨 —— 全 jar 里写 {@code HeatLevel} 的只有四处：
     * {@code RechargeableEnergyGunItem.inventoryTick}（降温）、{@code GunEnchantmentHelperMixin}（读热量改射速）、
     * {@code EnergyBoltProjectileEntity}（读热量做命中效果），以及 <b>{@code ServerPlayHandlerMixin.scguns_cnc$handleShoot}（涨热）</b>。
     * 最后那个包的是 {@code ServerPlayHandler.handleShoot(C2SMessageShoot, ServerPlayer, …)} ——
     * <b>玩家客户端封包驱动的开火路径</b>，女仆永远不经过它。SC2 本体也完全不碰 {@code HeatLevel}（扫过常量池），
     * 所以女仆手上的电热自动炮热量一直停在原值：射速按「当前热度」恒定不变，既不升温也不被惩罚。</p>
     * <p>插件原逻辑（每开一枪）：</p>
     * <pre>
     * tag.putInt("RechargeCounter", (int) Math.floor(getRefillCooldown() * getReloadRechargeTimeMult()));  // 重置 FE 回充计时
     * if (getUsesOverheat()) tag.putFloat("HeatLevel", Math.min(heat + 1.0f, 50.0f));                      // 涨热，上限 50
     * </pre>
     * <p>降温那半段已在 {@link #tickEnergyRecharge} 里补上（每 10 刻 {@code HeatLevel-1}）；
     * 射速惩罚不用我们做：女仆的 {@link #getFireInterval} 走的正是
     * {@code GunEnchantmentHelper.getRate}，而插件用 {@code GunEnchantmentHelperMixin} 把
     * {@code rate × (1 + HeatLevel/15)} 挂在了那个函数上 —— 热量一涨，射速自动跟着降
     * （满热 50 ⇒ 慢到 4.33 倍）。命中效果同理：{@code EnergyBoltProjectileEntity} 读的是弹丸身上的
     * 武器栈，而 SC2 的 {@code AIGunEvent} 会 {@code setWeapon(stack)}。</p>
     */
    public static void onEnergyGunFired(ItemStack stack) {
        if (!isEnergyGun(stack)) return;
        Item item = stack.getItem();
        CompoundTag tag = NbtHelper.getOrCreateTag(stack);

        // 1) 每枪重置 FE 回充计时（玩家那边也是这么做的，所以连射时回充会被一直推后）
        Object mult = readGetter(RELOAD_RECHARGE_MULT_GETTER, item);
        double rechargeMult = mult instanceof Number n ? n.doubleValue() : 1.0;
        tag.putInt("RechargeCounter",
                (int) Math.floor(readIntGetter(REFILL_COOLDOWN_GETTER, item) * rechargeMult));

        // 2) 涨热（只有 useOverheat 的枪，例如电热自动炮）
        if (Boolean.TRUE.equals(readGetter(USES_OVERHEAT_GETTER, item))) {
            float heat = tag.getFloat("HeatLevel");
            tag.putFloat("HeatLevel", Math.min(heat + 1.0f, MAX_HEAT));
        }
    }

    private static int readIntGetter(Method getter, Object target) {
        Object v = readGetter(getter, target);
        return v instanceof Number n ? n.intValue() : 0;
    }

    private static Object readGetter(Method getter, Object target) {
        if (getter == null) return null;
        try {
            return getter.invoke(target);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("SC2GunCompat: 读取 {}() 失败：{}", getter.getName(), e.toString());
            return null;
        }
    }

    public static int performGunAttack(LivingEntity maid, LivingEntity target, ItemStack gun) {
        if (!(maid instanceof Mob mob)) return 10;
        if (maid.level().isClientSide) return 10;
        if (!(gun.getItem() instanceof GunItem gunItem)) {
            return 10;
        }

        Gun modifiedGun = gunItem.getModifiedGun(gun);
        Gun.General general = getGeneral(gun, gunItem);
        // 射速必须走「附魔 → 配件」这条链（附魔里有 Trigger Finger / Heavy Shot / Puncturing
        // 三个都会改射速）。原来只调 GunModifierHelper.getModifiedRate(...) —— 那个函数只读配件，
        // 于是女仆的枪「不吃射速附魔」。
        int rate = getFireInterval(gun);

        // FE 能量枪原来在这里被豁免（0 发也照开火）。已取消：没弹就不开火 ——
        // 否则 AmmoCount 掉到 0 也照样突突，「弹匣会打完」就是句空话。
        if (!Gun.hasAmmo(gun)) {
            return rate;
        }

        rotateToFace(maid, target);

        mob.swing(InteractionHand.MAIN_HAND);
        if (modifiedGun != null && modifiedGun.getProjectile() != null && modifiedGun.getProjectile().getItem() != null) {
            AIGunEvent.performGunAttack(mob, target, gun, modifiedGun, SCG2TLMConfig.GUN_ACCURACY.get().floatValue());
        } else {
            AIGunEvent.performGunAttack(mob, target, gun, gunItem.getGun(), SCG2TLMConfig.GUN_ACCURACY.get().floatValue());
        }

        if (target instanceof Mob mobTarget) {
            mobTarget.setLastHurtByMob(maid);
            mobTarget.setTarget(mob);
        }

        playFireSound(maid, gun, gunItem);

        // 这段原来被 if (!isEnergyGun(gun)) 包着 —— 能量枪永不扣弹，是「无限弹药」的另一半原因。
        // 现在能量枪和普通枪一样每发扣 1（幽灵弹的免扣判定照旧生效）。
        int ammoCount = Gun.getAmmoCount(gun);
        if (ammoCount > 0) {
            // 幽灵弹（scguns:reclaimed）：按 SC2 的公式判定这次要不要真的扣弹
            if (shouldConsumeAmmo(maid, gun)) {
                NbtHelper.getOrCreateTag(gun).putInt("AmmoCount", ammoCount - 1);
            }
            Gun.Projectile projectile = getProjectile(gun, gunItem);
            if (projectile != null && projectile.ejectsCasing() && !projectile.ejectDuringReload()
                    && maid.getRandom().nextFloat() < 0.8f) {
                spawnCasing(maid, gun, gunItem, 1);
            }
        }

        // FE 能量枪的开火后处理：涨热 + 重置 FE 回充计时。
        // 插件的涨热只写在玩家封包路径（ServerPlayHandler.handleShoot）里，女仆不经过 ——
        // 不代跑的话电热自动炮的女仆热量恒定，射速惩罚永远停在当前那档（用户报的现象）。
        onEnergyGunFired(gun);

        int bursts = general.getBurstAmount();
        if (bursts > 1) {
            int shotInBurst = NbtHelper.getOrCreateTag(gun).getInt("scg2tlm:burst_shot");
            shotInBurst++;
            if (shotInBurst >= bursts) {
                NbtHelper.getOrCreateTag(gun).putInt("scg2tlm:burst_shot", 0);
                return Math.max(general.getBurstCooldown(), rate * 2);
            }
            NbtHelper.getOrCreateTag(gun).putInt("scg2tlm:burst_shot", shotInBurst);
        }

        return rate;
    }

    /**
     * 女仆的开火间隔（刻）。
     *
     * <p>走 SC2 玩家路径同一套顺序 —— {@code ShootTracker.putCooldown} 里就是这么写的：</p>
     * <pre>
     * int rate = GunEnchantmentHelper.getRate(weapon, modifiedGun);   // 附魔（Trigger Finger / Heavy Shot / Puncturing）
     * rate = GunModifierHelper.getModifiedRate(weapon, rate);         // 配件
     * </pre>
     * <p>而 {@code GunEnchantmentHelper.getRate} 内部本身就已经调过 {@code getModifiedRate}，
     * 所以调它一个就够。</p>
     *
     * <p><b>不要</b>改用 {@code GunModifierHelper.getModifiedRate(gun, general.getRate())}：
     * 那个函数只读<b>配件</b>（它内部是 {@code Gun.getAttachment(type, weapon)} → {@code IAttachment}），
     * 完全看不到附魔 —— 这就是之前「女仆射速不吃附魔 / 全自动武器变慢」的原因。</p>
     */
    public static int getFireInterval(ItemStack gun) {
        if (gun.isEmpty() || !(gun.getItem() instanceof GunItem gunItem)) return 5;
        Gun modifiedGun = gunItem.getModifiedGun(gun);
        if (modifiedGun == null) modifiedGun = gunItem.getGun();
        if (modifiedGun == null || modifiedGun.getGeneral() == null) return 5;
        try {
            return Math.max(1, GunEnchantmentHelper.getRate(gun, modifiedGun));
        } catch (Throwable t) {
            // 附属 mod 的枪数据不全时兜底：退回只算配件的老行为
            return Math.max(1, GunModifierHelper.getModifiedRate(gun, modifiedGun.getGeneral().getRate()));
        }
    }

    public static int getAttackCooldown(ItemStack gun) {
        return getFireInterval(gun);
    }

    private static Gun.General getGeneral(ItemStack gun, GunItem gunItem) {
        Gun modifiedGun = gunItem.getModifiedGun(gun);
        if (modifiedGun != null) {
            Gun.General general = modifiedGun.getGeneral();
            if (general != null) return general;
        }
        return gunItem.getGun().getGeneral();
    }

    private static Gun.Reloads getReloads(ItemStack gun, GunItem gunItem) {
        Gun modifiedGun = gunItem.getModifiedGun(gun);
        if (modifiedGun != null) {
            Gun.Reloads reloads = modifiedGun.getReloads();
            if (reloads != null) return reloads;
        }
        return gunItem.getGun().getReloads();
    }

    private static Gun.Sounds getSounds(ItemStack gun, GunItem gunItem) {
        Gun modifiedGun = gunItem.getModifiedGun(gun);
        if (modifiedGun != null) {
            Gun.Sounds sounds = modifiedGun.getSounds();
            if (sounds != null) return sounds;
        }
        return gunItem.getGun().getSounds();
    }

    private static Gun.Projectile getProjectile(ItemStack gun, GunItem gunItem) {
        Gun modifiedGun = gunItem.getModifiedGun(gun);
        if (modifiedGun != null) {
            Gun.Projectile projectile = modifiedGun.getProjectile();
            if (projectile != null) return projectile;
        }
        return gunItem.getGun().getProjectile();
    }

    private static void playFireSound(LivingEntity entity, ItemStack gun, GunItem gunItem) {
        boolean silenced = GunModifierHelper.isSilencedFire(gun);
        Gun.Sounds sounds = getSounds(gun, gunItem);
        ResourceLocation soundId = silenced ? sounds.getSilencedFire() : sounds.getFire();
        if (soundId == null && silenced) {
            soundId = sounds.getFire();
        }
        if (soundId == null) {
            warnMissingSoundOnce(gun, "开火音效 id 为空（枪的 sounds 里没有 fire/silencedFire）");
            return;
        }
        float volume = GunModifierHelper.getFireSoundVolume(gun) * 4.0f;
        playResolvedSound(entity, soundId, volume);
    }

    /**
     * 按 id 播放枪械音效 —— <b>注册表查不到就退回 {@code SoundEvent.createVariableRangeEvent}</b>。
     *
     * <h3>为什么要兜底</h3>
     * <p>原来这里（以及 {@link #playGunSound}）都是
     * {@code BuiltInRegistries.SOUND_EVENT.get(id)}，查不到就<b>静默什么都不播</b>。
     * 而 SC2 本体播放怪物枪声用的是
     * {@code SoundEvent.createVariableRangeEvent(fireSound)}（见 {@code GunAttackGoal.shoot()}）——
     * 那个写法只要求音效名能在 {@code sounds.json} 里解析，不依赖注册表里先有事件对象。</p>
     *
     * <p>实测：附属 mod 的枪械引用的音效 id 在 json 里都是能对上的
     * （WW1 复用 SC2 的 {@code scguns:item.bruiser.fire} 之类、CNC 自带 {@code sounds.json}），
     * 但注册表查表会拿到 null → 女仆打附属枪完全没声音。这里两种写法都试，
     * 且只对「都失败」的情况打一次告警（把 id 打进日志，方便定位到底是哪个音效缺文件）。</p>
     */
    private static void playResolvedSound(LivingEntity entity, ResourceLocation soundId, float volume) {
        SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(soundId);
        if (soundEvent == null) {
            // 与 SC2 本体一致：按名字构造事件，交给 SoundManager 去 sounds.json 里解析
            soundEvent = SoundEvent.createVariableRangeEvent(soundId);
            warnMissingSoundOnce(null, "注册表里没有音效事件，改用 createVariableRangeEvent: " + soundId);
        }
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                soundEvent, SoundSource.NEUTRAL, volume, 0.9f + entity.getRandom().nextFloat() * 0.2f);
    }

    /** 同一个 id 只告警一次，避免刷屏。 */
    private static final Set<String> WARNED_SOUND_IDS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static void warnMissingSoundOnce(@Nullable ItemStack gun, String message) {
        String key = (gun == null ? "" : String.valueOf(BuiltInRegistries.ITEM.getKey(gun.getItem()))) + '|' + message;
        if (WARNED_SOUND_IDS.add(key)) {
            LOGGER.warn("[scg2_maid_compat] 枪械音效问题：{}（枪={}）", message,
                    gun == null ? "-" : String.valueOf(BuiltInRegistries.ITEM.getKey(gun.getItem())));
        }
    }

    public static void playGunSound(LivingEntity entity, ItemStack gun, ResourceLocation soundId, float volume) {
        if (soundId == null) return;
        playResolvedSound(entity, soundId, volume);
    }

    public static void playPreReloadSound(LivingEntity entity, ItemStack gun) {
        if (!(gun.getItem() instanceof GunItem gunItem)) return;
        playGunSound(entity, gun, getSounds(gun, gunItem).getPreReload(), 1.0f);
    }

    /**
     * 播<b>换弹起手</b>音 —— 默认用配置项 {@code reload_sound}（默认 {@code scguns:item.gauss.reload}）。
     *
     * <h2>为什么不直接用枪数据里的 {@code sounds.reload}</h2>
     * <p>SC2 的 {@code ReloadTracker.playReloadSound(Player)} 第一句就是
     * {@code if (this.stack.getItem() instanceof AnimatedGunItem) return;} ——
     * 动画枪的换弹音由物品动画的音效关键帧（{@code sound_effects}）负责，
     * 处理器注册在 SC2 的 {@code AnimatedGunItem} 上（全 jar 只有它引用 {@code SoundKeyframe}）。
     * 于是数据里的 {@code reload} 字段只对「玩家 + 非动画枪」生效：全 jar 里除了
     * {@code Gun$Sounds} 自己的访问器，<b>只有 {@code ReloadTracker} 引用 {@code getReload}</b>
     * —— 连 SC2 自己的枪手怪都不放它。</p>
     *
     * <p>而那批数据本身几乎全是占位音：SC2 本体 141 把枪里 132 把写 {@code gauss.reload}，
     * cnc 的 27 把枪里 24 把写 {@code gauss.reload}，剩下的 {@code lustre}/{@code scatterer}/
     * {@code electrothermal_autocannon} 写的是 {@code flamethrower.reload}
     * —— 我们原来对所有枪都放它，于是女仆成了唯一会听到这些占位音的对象
     * （用户报告：「女仆的换弹音效可能是喷火器的音效」）。</p>
     *
     * <p>所以改成配置驱动：默认统一播 {@code scguns:item.gauss.reload}（用户指定）；
     * <b>留空</b>则回退到 SC2 的原规则（枪数据字段，动画枪不放）。</p>
     */
    public static void playReloadSound(LivingEntity entity, ItemStack gun) {
        if (!(gun.getItem() instanceof GunItem gunItem)) return;
        String configured = SCG2TLMConfig.RELOAD_SOUND.get();
        if (configured != null && !configured.isBlank()) {
            ResourceLocation id = ResourceLocation.tryParse(configured.trim());
            if (id != null) {
                playGunSound(entity, gun, id, 1.0f);
                return;
            }
        }
        // 回退：SC2 原规则 —— 动画枪不放数据里的 reload。
        if (gunItem instanceof AnimatedGunItem) return;
        playGunSound(entity, gun, getSounds(gun, gunItem).getReload(), 1.0f);
    }

    public static void playCockSound(LivingEntity entity, ItemStack gun) {
        if (!(gun.getItem() instanceof GunItem gunItem)) return;
        playGunSound(entity, gun, getSounds(gun, gunItem).getCock(), 1.0f);
    }

    /**
     * 播放枪械<b>自己配置的</b>开火音效（含消音分支）。
     *
     * <p>给医疗兵（十字军）那条非攻击路径复用：{@code SC2HealCompat.fireHealingShot} 直接调
     * {@code AIGunEvent.performGunAttack} 生成弹丸，绕过了 {@link #performGunAttack}，
     * 于是这一枪<b>完全没有音效</b>。</p>
     */
    public static void playGunFireSound(LivingEntity entity, ItemStack gun) {
        if (!(gun.getItem() instanceof GunItem gunItem)) return;
        playFireSound(entity, gun, gunItem);
    }

    /**
     * 播放「换弹完成」音效 —— 也就是<b>上膛</b>音（配置项 {@code reload_end_sound}，空字符串 = 静音）。
     *
     * <p>不用枪械的 {@code cock} 字段：SC2 那把 {@code item/pistol/cock.ogg} 实际是换弹录音，
     * 放在换弹结束的位置听起来就是又换了一次弹。默认用拉栓音 {@code scguns:item.bolt.bolt}（用户指定）。</p>
     */
    public static void playReloadEndSound(LivingEntity entity, ItemStack gun) {
        String configured = SCG2TLMConfig.RELOAD_END_SOUND.get();
        if (configured == null || configured.isBlank()) return;
        ResourceLocation id = ResourceLocation.tryParse(configured.trim());
        if (id == null) return;
        playGunSound(entity, gun, id, 1.0f);
    }

    public static boolean isGrenade(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        if (item instanceof GrenadeItem || item instanceof GasGrenadeItem || item instanceof BeaconGrenadeItem) {
            return true;
        }
        return false;
    }

    public static void performGrenadeThrow(EntityMaid maid, LivingEntity target, ItemStack stack) {
        if (maid.level().isClientSide) return;
        rotateToFace(maid, target);

        Item item = stack.getItem();
        ThrowableGrenadeEntity projectile = null;

        if (item instanceof GrenadeItem grenadeItem) {
            projectile = grenadeItem.create(maid.level(), maid, 30);
        } else if (item instanceof GasGrenadeItem gasItem) {
            projectile = gasItem.create(maid.level(), maid, 30, 4.0f);
        } else if (item instanceof BeaconGrenadeItem beaconItem) {
            projectile = beaconItem.create(maid.level(), maid, 30);
        }

        if (projectile != null) {
            projectile.setPos(maid.getX(), maid.getEyeY(), maid.getZ());
            double dx = target.getX() - maid.getX();
            double dy = target.getEyeY() - maid.getEyeY();
            double dz = target.getZ() - maid.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            projectile.shoot(dx, dy + horizontalDist * 0.15, dz, 1.2f, 0.5f);
            maid.level().addFreshEntity(projectile);
            stack.shrink(1);
        }
    }

    private static void rotateToFace(LivingEntity entity, LivingEntity target) {
        double dx = target.getX() - entity.getX();
        double dz = target.getZ() - entity.getZ();
        double dy = target.getEyeY() - entity.getEyeY();
        double dist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, dist)));

        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.yRotO = yaw;
        entity.xRotO = pitch;

        if (entity instanceof Mob mob) {
            mob.yHeadRot = yaw;
            mob.yHeadRotO = yaw;
        }
    }

    public static boolean needsReload(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem)) {
            return false;
        }
        // FE 能量枪（插件的 RechargeableEnergyGunItem）原来在这里直接 return false —— 永不换弹。
        // 这个豁免已取消：弹匣照样会打完，打完就要换弹，消耗背包里的 pulse_core / energy_core
        // （插件 lore 写的就是「Energy Core can be swapped for immediate recharge」）。
        if (Gun.Reloads.hasInfiniteAmmo(stack)) {
            return false;
        }
        return !Gun.hasAmmo(stack);
    }

    public static int getReloadTime(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem gunItem)) return 20;
        Gun.Reloads reloads = getReloads(stack, gunItem);
        double baseTime = reloads.getReloadTimer() + reloads.getEmptyMagTimer();
        // Quick Hands（快速换弹）：每级 -25%，系数与 SC2 自己的
        // GunEnchantmentHelper.getMagReloadSpeed / getReloadInterval 完全一致。
        // 原来这里只乘了配件修正（getModifiedReloadSpeed 只读配件）→ 换弹附魔无效。
        int quickHands = GunEnchantmentHelper.getQuickHands(stack);
        if (quickHands > 0) {
            baseTime *= Math.max(0.0, 1.0 - 0.25 * quickHands);
        }
        return Math.max(1, (int) GunModifierHelper.getModifiedReloadSpeed(stack, baseTime));
    }

    /**
     * 这次开火要不要真的扣一发弹药 —— 幽灵弹（{@code scguns:reclaimed}）判定。
     *
     * <p>汉化包里 {@code enchantment.scguns.reclaimed} 就叫「幽灵弹」，效果是<b>有概率不消耗弹药</b>。
     * SC2 把这段写在 {@code common/network/ServerPlayHandler.consumeAmmo(ServerPlayer, ItemStack)} 里，
     * 也就是说<b>只有玩家开火才会走</b>；女仆的扣弹是我们自己在 {@link #performGunAttack} 里做的，
     * 原来是无条件 {@code AmmoCount - 1}，所以幽灵弹对女仆完全无效。</p>
     *
     * <p>公式照抄 SC2（含 {@code Mth.clamp(level, 1, 2)}）：</p>
     * <pre>
     * level == 0                      → 一定扣
     * level 1  → nextInt(3) != 0      → 2/3 扣、1/3 不扣
     * level 2+ → nextInt(2) != 0      → 1/2 扣、1/2 不扣（level 3 及以上按 2 夹住）
     * </pre>
     */
    private static boolean shouldConsumeAmmo(LivingEntity shooter, ItemStack gun) {
        int level = ScEnchants.level(gun, ModEnchantments.RECLAIMED);
        if (level <= 0) return true;
        return shooter.getRandom().nextInt(4 - Mth.clamp(level, 1, 2)) != 0;
    }

    public static int getMaxAmmo(ItemStack stack) {
        int capacity = computeMaxAmmo(stack);
        LOGGER.info("getMaxAmmo: capacity={}, stack={}", capacity, stack);
        return capacity;
    }

    /**
     * 弹匣容量（{@code getMaxAmmo} 的无日志版本）。
     *
     * <p>拆出来是因为 {@code getMaxAmmo} 里那句 {@code LOGGER.info} —— 它被召唤进
     * {@link #tickEnergyRecharge} 后<b>每刻都会打一行</b>（女仆拿着能量枪就刷屏），
     * 所以热路径走这个不发日志的版本。</p>
     */
    public static int computeMaxAmmo(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem gunItem)) return 0;
        return GunModifierHelper.getModifiedAmmoCapacity(stack, gunItem.getGun());
    }

    public static void setFullAmmo(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem)) {
            return;
        }
        int maxAmmo = getMaxAmmo(stack);
        NbtHelper.getOrCreateTag(stack).putInt("AmmoCount", maxAmmo);
    }

    public static boolean hasAmmoInInventory(EntityMaid maid, ItemStack gun) {
        if (SCG2TLMConfig.RELOAD_FREE_AMMO.get()) {
            return true;
        }
        // FE 能量枪原来在这里直接 return true（「永远有弹药」），已取消：
        // 现在它和普通枪走同一条判定 —— 背包里得有枪数据声明的弹药物品
        // （lustre → scguns:energy_core；scatterer / electrothermal_autocannon → scguns_cnc:pulse_core）。
        if (!(gun.getItem() instanceof GunItem gunItem)) {
            return false;
        }
        Item ammoType = getProjectileItem(gun, gunItem);
        if (ammoType == null) {
            return false;
        }
        // 能量枪不吃创造弹药盒（用户要求）：见 ignoreCreativeAmmoBox 的注释。
        boolean ignoreCreative = isEnergyGun(gun);
        IItemHandler inv = maid.getAvailableInv(true);
        if (!ignoreCreative && hasCreativeAmmoBox(inv) && isAmmoProvidedByCreativeBox(ammoType)) {
            return true;
        }
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack slot = inv.getStackInSlot(i);
            if (slot.isEmpty()) continue;
            if (slot.getItem() == ammoType) return true;
            if (slot.getItem() instanceof AmmoBoxItem) {
                if (ignoreCreative && isCreativeAmmoBox(slot)) continue;
                if (AmmoBoxItem.getContents(slot).anyMatch(s -> s.getItem() == ammoType)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 能量枪要不要无视创造弹药盒。
     *
     * <h2>为什么（用户报告 + 要求）</h2>
     * <p>用户报告「FE 弹药恢复貌似是一次回满的」，实测日志给的是
     * {@code reloadFromInventory(creative_box): maxAmmo=100, stack=1 electrothermal_autocannon} ——
     * 女仆背包里那个<b>创造弹药盒</b>把弹匣直接补满且分文不取（创造盒的语义就是「该弹药无限供应」），
     * 于是 FE 与核心这两条真实补给被整个绕开，FE 经济根本测不出来。</p>
     * <p>所以对 {@code RechargeableEnergyGunItem}（`lustre` / `scatterer` / `electrothermal_autocannon`）
     * 一律<b>无视创造弹药盒</b>：{@link #hasAmmoInInventory} 不认它、{@link #reloadFromInventory} 不从它取弹。
     * 注意创造盒是 {@link AmmoBoxItem} 的子类，所以普通弹药盒的循环里也要单独把它剔掉 —— 只判
     * {@code instanceof AmmoBoxItem} 会漏。</p>
     * <p>{@code reload_free_ammo} 配置项<b>不</b>受此影响：那是用户显式打开的「换弹不耗弹药」总开关，
     * 打开时对所有枪（含能量枪）都免费。</p>
     */
    private static boolean ignoreCreativeAmmoBox(ItemStack gun) {
        return isEnergyGun(gun);
    }

    /** 这个槽位是不是创造弹药盒（必须先于 {@code instanceof AmmoBoxItem} 判断）。 */
    private static boolean isCreativeAmmoBox(ItemStack slot) {
        return !slot.isEmpty() && slot.getItem() instanceof CreativeAmmoBoxItem;
    }

    private static boolean hasCreativeAmmoBox(IItemHandler inv) {
        for (int i = 0; i < inv.getSlots(); i++) {
            if (isCreativeAmmoBox(inv.getStackInSlot(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAmmoProvidedByCreativeBox(Item ammoType) {
        TagKey<Item> scgunsAmmo = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("scguns", "ammo"));
        return ammoType.builtInRegistryHolder().is(scgunsAmmo);
    }

    private static Item getProjectileItem(ItemStack gun, GunItem gunItem) {
        Gun modifiedGun = gunItem.getModifiedGun(gun);
        if (modifiedGun != null) {
            var projectile = modifiedGun.getProjectile();
            if (projectile != null) {
                Item item = projectile.getItem();
                if (item != null) return item;
            }
        }
        var baseProjectile = gunItem.getGun().getProjectile();
        if (baseProjectile != null) {
            Item baseItem = baseProjectile.getItem();
            if (baseItem != null) return baseItem;
        }
        return null;
    }

    public static boolean reloadFromInventory(EntityMaid maid, ItemStack gun) {
        // FE 能量枪原来在这里直接 return false（永不换弹），已取消 —— 它和普通枪一样从背包里
        // 取弹药物品，单个核心换满弹匣（isSingleReloadAmmo 已认识 scguns:energy_core 与 scguns_cnc:pulse_core）。
        if (!(gun.getItem() instanceof GunItem gunItem)) {
            return false;
        }
        if (SCG2TLMConfig.RELOAD_FREE_AMMO.get()) {
            int maxAmmo = getMaxAmmo(gun);
            CompoundTag tag = NbtHelper.getOrCreateTag(gun);
            tag.putInt("AmmoCount", maxAmmo);
            tag.putInt("PrevAmmoCount", maxAmmo);
            LOGGER.info("reloadFromInventory(free): maxAmmo={}, stack={}", maxAmmo, gun);
            return true;
        }
        Gun modifiedGun = gunItem.getModifiedGun(gun);
        int maxAmmo = getMaxAmmo(gun);
        Item ammoType = getProjectileItem(gun, gunItem);
        if (ammoType == null) {
            LOGGER.warn("reloadFromInventory: ammoType is null, cannot reload");
            return false;
        }

        ResourceLocation ammoId = BuiltInRegistries.ITEM.getKey(ammoType);
        boolean isSingleReload = isSingleReloadAmmo(ammoType);
        boolean isEnergyCore = ammoId != null && "scguns".equals(ammoId.getNamespace())
                && "energy_core".equals(ammoId.getPath());
        boolean isBlazeFuel = ammoId != null && "scguns".equals(ammoId.getNamespace())
                && "blaze_fuel".equals(ammoId.getPath());

        IItemHandler inv = maid.getAvailableInv(true);
        // 能量枪无视创造盒（见 ignoreCreativeAmmoBox）：否则弹匣被免费补满，FE/核心两条补给全被绕开。
        boolean ignoreCreative = ignoreCreativeAmmoBox(gun);
        if (!ignoreCreative && hasCreativeAmmoBox(inv) && isAmmoProvidedByCreativeBox(ammoType)) {
            int maxAmmo2 = getMaxAmmo(gun);
            CompoundTag tag = NbtHelper.getOrCreateTag(gun);
            tag.putInt("AmmoCount", maxAmmo2);
            tag.putInt("PrevAmmoCount", maxAmmo2);
            LOGGER.info("reloadFromInventory(creative_box): maxAmmo={}, stack={}", maxAmmo2, gun);
            return true;
        }
        int found = 0;

        if (isSingleReload) {
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack slot = inv.getStackInSlot(i);
                if (!slot.isEmpty() && slot.getItem() == ammoType) {
                    ItemStack taken = inv.extractItem(i, 1, false);
                    found = taken.getCount();
                    break;
                }
            }
            if (found == 0) {
                found = extractFromAmmoBoxes(maid, inv, ammoType, 1, ignoreCreative);
            }
            if (found > 0) {
                LOGGER.info("reloadFromInventory(single): maxAmmo={}, found={}", maxAmmo, found);
                NbtHelper.getOrCreateTag(gun).putInt("AmmoCount", maxAmmo);
                if (isBlazeFuel) {
                    spawnEmptyTank(maid);
                } else if (ammoId != null && SCCAVES_NS.equals(ammoId.getNamespace())) {
                    spawnDepletedNSItem(maid, ammoId.getNamespace(), ammoId.getPath(), "depleted_crystal");
                } else if (ammoId != null && OREGUNIZED_NS.equals(ammoId.getNamespace())) {
                    spawnDepletedNSItem(maid, ammoId.getNamespace(), ammoId.getPath(), "depleted_electrum_cell");
                } else {
                    spawnDepletedCore(maid);
                }
            }
            return found > 0;
        } else {
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack slot = inv.getStackInSlot(i);
                if (!slot.isEmpty() && slot.getItem() == ammoType) {
                    int toTake = Math.min(slot.getCount(), maxAmmo - found);
                    ItemStack taken = inv.extractItem(i, toTake, false);
                    found += taken.getCount();
                    if (found >= maxAmmo) break;
                }
            }
            if (found < maxAmmo) {
                found += extractFromAmmoBoxes(maid, inv, ammoType, maxAmmo - found, ignoreCreative);
            }
            if (found > 0) {
                int newCount = Math.min(found, maxAmmo);
                CompoundTag tag = NbtHelper.getOrCreateTag(gun);
                int prevAmmo = tag.getInt("AmmoCount");

                int casingCount = tag.contains("PrevAmmoCount") ? tag.getInt("PrevAmmoCount") : 0;
                tag.putInt("AmmoCount", newCount);
                tag.putInt("PrevAmmoCount", newCount);
                LOGGER.info("reloadFromInventory: maxAmmo={}, found={}, newCount={}, prevAmmo={}, ammoType={}", maxAmmo, found, newCount, prevAmmo, ammoType);
                Gun.Projectile proj = getProjectile(gun, gunItem);
                if (proj != null && proj.ejectDuringReload()) {
                    spawnCasing(maid, gun, gunItem, casingCount);
                }
            }
            return found > 0;
        }

    }

    private static int extractFromAmmoBoxes(EntityMaid maid, IItemHandler inv, Item ammoType, int maxAmount,
                                            boolean ignoreCreativeBox) {
        int extracted = 0;
        for (int i = 0; i < inv.getSlots() && extracted < maxAmount; i++) {
            ItemStack slot = inv.getStackInSlot(i);
            if (slot.isEmpty() || !(slot.getItem() instanceof AmmoBoxItem)) continue;
            // 创造盒是 AmmoBoxItem 的子类：能量枪要无视它，就得在「是不是弹药盒」这步之前先剔掉。
            if (ignoreCreativeBox && isCreativeAmmoBox(slot)) continue;

            if (AmmoBoxItem.getContents(slot).noneMatch(s -> s.getItem() == ammoType)) continue;

            ItemStack boxStack = inv.extractItem(i, Integer.MAX_VALUE, false);
            if (boxStack.isEmpty()) continue;

            int toExtract = Math.min(AmmoBoxItem.getTotalItemCount(boxStack), maxAmount - extracted);
            int taken = removeFromBoxTag(boxStack, ammoType, toExtract);
            extracted += taken;

            ItemStack remainder = inv.insertItem(i, boxStack, false);
            if (!remainder.isEmpty()) {
                ItemEntity entity = new ItemEntity(maid.level(),
                        maid.getX(), maid.getY() + 1.0, maid.getZ(), remainder);
                entity.setPickUpDelay(10);
                maid.level().addFreshEntity(entity);
            }
        }
        return extracted;
    }

    private static int removeFromBoxTag(ItemStack boxStack, Item ammoType, int maxRemove) {
        CompoundTag tag = NbtHelper.getOrCreateTag(boxStack);
        if (!tag.contains("Items", Tag.TAG_LIST)) return 0;

        ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        int remaining = maxRemove;

        for (int j = 0; j < items.size() && remaining > 0; j++) {
            CompoundTag itemTag = items.getCompound(j);
            // 1.21：ItemStack.of(CompoundTag) 没了（物品数据变成组件，读回来要注册表），
            // 改用插件本体的 NbtHelper.itemFromTag（内部走 ItemStack.CODEC + NbtOps）。
            ItemStack itemStack = NbtHelper.itemFromTag(itemTag);
            if (itemStack.getItem() == ammoType) {
                int take = Math.min(itemStack.getCount(), remaining);
                itemStack.shrink(take);
                remaining -= take;
                if (itemStack.isEmpty()) {
                    items.remove(j);
                    j--;
                } else {
                    // 写回也得用同一套编码：1.20.1 的键名是 "Count"，1.21 的 ItemStack.CODEC 是 "count"，
                    // 继续 putInt("Count") 会写进一个编解码器不认识的键，减掉的弹药等于没减（弹药盒变无限）。
                    items.set(j, NbtHelper.tagFromItem(itemStack));
                }
            }
        }

        int removed = maxRemove - remaining;
        if (items.isEmpty()) {
            tag.remove("Items");
        } else {
            tag.put("Items", items);
        }
        return removed;
    }

    private static int insertIntoAmmoBoxes(IItemHandler inv, ItemStack stack) {
        int remaining = stack.getCount();
        for (int i = 0; i < inv.getSlots() && remaining > 0; i++) {
            ItemStack slot = inv.getStackInSlot(i);
            if (slot.isEmpty() || !(slot.getItem() instanceof AmmoBoxItem)) continue;

            ItemStack toAdd = stack.copy();
            toAdd.setCount(remaining);
            int added = AmmoBoxItem.add(slot, toAdd);
            remaining -= added;
        }
        return remaining;
    }

    private static void spawnDepletedCore(EntityMaid maid) {
        Item depletedCore = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("scguns", "depleted_energy_core"));
        if (depletedCore == null) {
            return;
        }
        ItemStack stack = new ItemStack(depletedCore);
        IItemHandler inv = maid.getAvailableInv(false);
        ItemStack remaining = ItemHandlerHelper.insertItemStacked(inv, stack, false);
        if (!remaining.isEmpty()) {
            ItemEntity entity = new ItemEntity(maid.level(),
                    maid.getX(), maid.getY() + 1.0, maid.getZ(),
                    remaining);
            entity.setPickUpDelay(10);
            maid.level().addFreshEntity(entity);
        }
    }

    private static boolean isSingleReloadAmmo(Item item) {
        if (item == null) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return false;
        String ns = id.getNamespace();
        String path = id.getPath();
        if (SCGUNS_NS.equals(ns)) {
            return "energy_core".equals(path) || "blaze_fuel".equals(path);
        }
        if (SCCAVES_NS.equals(ns)) {
            return "pulse_core".equals(path);
        }
        if (OREGUNIZED_NS.equals(ns)) {
            return "electrum_cell".equals(path) || "mauvite_core".equals(path);
        }
        return false;
    }

    private static void spawnDepletedNSItem(EntityMaid maid, String namespace, String ammoPath, String defaultDepletedPath) {
        String depletedItemPath = "depleted_" + ammoPath;
        Item depleted = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath(namespace, depletedItemPath));
        if (depleted == null) {
            depleted = BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath(namespace, defaultDepletedPath));
        }
        if (depleted == null) return;
        ItemStack stack = new ItemStack(depleted);
        IItemHandler inv = maid.getAvailableInv(false);
        ItemStack remaining = ItemHandlerHelper.insertItemStacked(inv, stack, false);
        if (!remaining.isEmpty()) {
            ItemEntity entity = new ItemEntity(maid.level(),
                    maid.getX(), maid.getY() + 1.0, maid.getZ(), remaining);
            entity.setPickUpDelay(10);
            maid.level().addFreshEntity(entity);
        }
    }

    private static void spawnEmptyTank(EntityMaid maid) {
        Item emptyTank = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("scguns", "empty_tank"));
        if (emptyTank == null) {
            return;
        }
        ItemStack stack = new ItemStack(emptyTank);
        IItemHandler inv = maid.getAvailableInv(false);
        ItemStack remaining = ItemHandlerHelper.insertItemStacked(inv, stack, false);
        if (!remaining.isEmpty()) {
            ItemEntity entity = new ItemEntity(maid.level(),
                    maid.getX(), maid.getY() + 1.0, maid.getZ(),
                    remaining);
            entity.setPickUpDelay(10);
            maid.level().addFreshEntity(entity);
        }
    }

    private static void spawnCasing(LivingEntity entity, ItemStack gun, GunItem gunItem, int count) {
        if (SCG2TLMConfig.RELOAD_FREE_AMMO.get()) {
            return;
        }

        Item ammoType = getProjectileItem(gun, gunItem);
        if (ammoType == null || isSingleReloadAmmo(ammoType)) {
            return;
        }

        Gun.Projectile projectile = getProjectile(gun, gunItem);
        if (projectile == null) return;

        boolean ejects = projectile.ejectsCasing();
        if (!ejects) return;

        ResourceLocation casingId = projectile.getCasingType();
        if (casingId == null) {
            casingId = projectile.casingType;
            if (casingId == null) {
                return;
            }
        }

        Item casingItem = BuiltInRegistries.ITEM.get(casingId);
        if (casingItem == null) {
            return;
        }

        ItemStack casingStack = new ItemStack(casingItem, count);

        if (entity instanceof EntityMaid maid) {
            IItemHandlerModifiable inv = maid.getAvailableInv(false);

            int remaining = insertIntoAmmoBoxes(inv, casingStack.copy());
            if (remaining <= 0) return;

            IItemHandler noOffhand = new RangedWrapper(inv, 0, inv.getSlots() - 1);
            ItemStack leftover = ItemHandlerHelper.insertItemStacked(noOffhand, new ItemStack(casingItem, remaining), false);
            if (!leftover.isEmpty()) {
                ItemEntity entityItem = new ItemEntity(entity.level(),
                        entity.getX(), entity.getY() + 0.5, entity.getZ(),
                        leftover);
                entityItem.setPickUpDelay(10);
                entity.level().addFreshEntity(entityItem);
            }
        } else {
            ItemEntity entityItem = new ItemEntity(entity.level(),
                    entity.getX(), entity.getY() + 0.5, entity.getZ(),
                    casingStack);
            entityItem.setPickUpDelay(20);
            entity.level().addFreshEntity(entityItem);
        }
    }

    /**
     * 全局交战距离（格）—— 配置里的 {@code gun_range}。
     *
     * <p>原来这里写死返回 {@code 64.0f}，配置项 {@code gun_range} 只影响
     * {@link #getIdealRange}/{@link #getMinRange} 的走位比例，**开火距离上限**是死的 64 格 ——
     * 于是霰弹枪/喷火器也会在 64 格外开火。现在它读配置，并且作为
     * {@link #getMaxFireRange(ItemStack)} 的封顶值。</p>
     */
    public static float getGunRange() {
        return SCG2TLMConfig.GUN_RANGE.get().floatValue();
    }

    /**
     * 这把枪所属类别的「索敌距离预算」（格）。
     *
     * <p>分类判据与 {@link TaskSC2GunAttack} 原来那份完全一致（先看 {@link #getSpecialRangeClass}
     * 的几个特判枪，再看 SC2 的 {@code WeaponType}），只是抽到这里做成唯一一份，
     * 免得索敌和开火两处各维护一套：</p>
     * <ul>
     *   <li>狙击 / 等离子 / 激光 / {@code gauss_rifle} → {@code sniper_search_radius}</li>
     *   <li>步枪 / 马格南 / 重型 / 轻机枪 → {@code rifle_magnum_search_radius}</li>
     *   <li><b>霰弹枪</b>（含 {@code plasmabuss}/{@code libertas}）→ {@code shotgun_search_radius}</li>
     *   <li><b>喷火器 / 电击</b> → {@code flamethrower_search_radius}</li>
     *   <li>冲锋枪 → {@code smg_shotgun_pistol_search_radius}（键名是历史遗留）</li>
     *   <li><b>手枪</b> → {@code pistol_search_radius}</li>
     *   <li>其它 / 读不到 → {@code gun_range}</li>
     * </ul>
     */
    public static double getClassRangeBudget(ItemStack stack) {
        String special = getSpecialRangeClass(stack);
        if ("sniper".equals(special)) return SCG2TLMConfig.SNIPER_SEARCH_RADIUS.get();
        if ("shotgun".equals(special)) return SCG2TLMConfig.SHOTGUN_SEARCH_RADIUS.get();
        if ("rifle".equals(special)) return SCG2TLMConfig.RIFLE_MAGNUM_SEARCH_RADIUS.get();

        WeaponType type = getWeaponType(stack);
        if (type == null) return SCG2TLMConfig.GUN_RANGE.get();
        return switch (type) {
            case sniper, plasma, laser -> SCG2TLMConfig.SNIPER_SEARCH_RADIUS.get();
            case rifle, magnum, heavy, lmg -> SCG2TLMConfig.RIFLE_MAGNUM_SEARCH_RADIUS.get();
            case shotgun -> SCG2TLMConfig.SHOTGUN_SEARCH_RADIUS.get();
            case flamethrower, shock -> SCG2TLMConfig.FLAMETHROWER_SEARCH_RADIUS.get();
            case smg -> SCG2TLMConfig.SMG_SHOTGUN_PISTOL_SEARCH_RADIUS.get();
            case pistol -> SCG2TLMConfig.PISTOL_SEARCH_RADIUS.get();
            // WeaponType 一共 13 个常量，`special` 是那个「特殊武器」兜底类别：
            // 走全局交战距离（其中几个真正特殊的枪由上面的 getSpecialRangeClass 特判接管）。
            case special -> SCG2TLMConfig.GUN_RANGE.get();
        };
    }

    /**
     * 女仆拿着这把枪时，最远会在多少格开火 —— {@code min(全局 gun_range, 该类索敌半径)}。
     *
     * <p>取 min 是为了「只收紧、不放大」：近程武器（霰弹枪/喷火器/手枪/冲锋枪）不会再有
     * 64 格开火这种离谱行为，而狙击枪/步枪这类本来就跟 {@code gun_range} 相当或更短的类别
     * 行为不变（想整体拉远就把 {@code gun_range} 调大）。</p>
     */
    public static double getMaxFireRange(ItemStack stack) {
        return Math.max(2.0, Math.min(SCG2TLMConfig.GUN_RANGE.get(), getClassRangeBudget(stack)));
    }

    public static WeaponType getWeaponType(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem gunItem)) return null;
        return getGeneral(stack, gunItem).getWeaponType();
    }

    public static boolean isMinigunGun(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) return false;
        return SC2_MINIGUN_GUNS.contains(key.getPath())
                || CC_MINIGUN_GUNS.contains(key.getPath())
                || OREG_MINIGUN_GUNS.contains(key.getPath());
    }

    public static boolean isRpgGun(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) return false;
        return SC2_RPG_GUNS.contains(key.getPath())
                || CC_RPG_GUNS.contains(key.getPath())
                || OREG_RPG_GUNS.contains(key.getPath());
    }

    public static boolean setAnimOnce(AnimationController<?> controller, String name, ILoopType loopType) {
        Animation current = controller.getCurrentAnimation();
        if (current != null && name.equals(current.animationName)) {
            return true;
        }
        controller.setAnimation(new AnimationBuilder().addAnimation(name, loopType));
        return false;
    }

    /** 特定武器 id 的射程分类覆盖（用户手动指定，忽略其本体 WeaponType）。 */
    public static String getSpecialRangeClass(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null || !key.getNamespace().equals("scguns")) {
            return null;
        }
        return switch (key.getPath()) {
            case "sterilizer" -> "rifle";
            case "plasmabuss", "libertas" -> "shotgun";
            case "gauss_rifle" -> "sniper";
            default -> null;
        };
    }

    /** 近战类枪械（霰弹枪/喷火器/电击）：需要贴近目标输出，不受远程走位限制。 */
    public static boolean isMeleeClassGun(ItemStack stack) {
        String special = getSpecialRangeClass(stack);
        if ("shotgun".equals(special)) {
            return true;
        }
        WeaponType type = getWeaponType(stack);
        return type == WeaponType.shotgun || type == WeaponType.flamethrower || type == WeaponType.shock;
    }

    public static double getIdealRange(ItemStack stack) {
        double baseRange = SCG2TLMConfig.GUN_RANGE.get();
        String special = getSpecialRangeClass(stack);
        if (special != null) {
            if (special.equals("sniper")) return Math.min(baseRange * 1.25, SCG2TLMConfig.SNIPER_SEARCH_RADIUS.get() * 0.85);
            if (special.equals("rifle")) return baseRange * 0.5;
            if (special.equals("shotgun")) return baseRange * 0.125;
        }
        WeaponType type = getWeaponType(stack);
        if (type == null) return baseRange * 0.375;
        if (type == WeaponType.sniper) return Math.min(baseRange * 1.25, SCG2TLMConfig.SNIPER_SEARCH_RADIUS.get() * 0.85);
        if (type == WeaponType.rifle) return baseRange * 0.5;
        if (type == WeaponType.magnum) return baseRange * 0.4375;
        if (type == WeaponType.smg) return baseRange * 0.25;
        if (type == WeaponType.shotgun) return baseRange * 0.125;
        if (type == WeaponType.pistol) return baseRange * 0.1875;
        if (type == WeaponType.heavy) return baseRange * 0.5;
        if (type == WeaponType.lmg) return baseRange * 0.5625;
        if (type == WeaponType.flamethrower) return baseRange * 0.09375;
        if (type == WeaponType.shock) return baseRange * 0.15625;
        if (type == WeaponType.plasma || type == WeaponType.laser) return Math.min(baseRange * 1.25, SCG2TLMConfig.SNIPER_SEARCH_RADIUS.get() * 0.85);
        return baseRange * 0.375;
    }

    public static double getMinRange(ItemStack stack) {
        double baseRange = SCG2TLMConfig.GUN_RANGE.get();
        String special = getSpecialRangeClass(stack);
        if (special != null) {
            if (special.equals("sniper")) return baseRange * 0.4375;
            if (special.equals("rifle")) return baseRange * 0.125;
            if (special.equals("shotgun")) return baseRange * 0.03125;
        }
        WeaponType type = getWeaponType(stack);
        if (type == null) return baseRange * 0.078125;
        if (type == WeaponType.sniper) return baseRange * 0.4375;
        if (type == WeaponType.rifle) return baseRange * 0.125;
        if (type == WeaponType.magnum) return baseRange * 0.125;
        if (type == WeaponType.smg) return baseRange * 0.0625;
        if (type == WeaponType.shotgun) return baseRange * 0.03125;
        if (type == WeaponType.pistol) return baseRange * 0.046875;
        if (type == WeaponType.heavy) return baseRange * 0.25;
        if (type == WeaponType.lmg) return baseRange * 0.15625;
        if (type == WeaponType.flamethrower) return baseRange * 0.03125;
        if (type == WeaponType.shock) return baseRange * 0.046875;
        if (type == WeaponType.plasma || type == WeaponType.laser) return baseRange * 0.3125;
        return baseRange * 0.078125;
    }

    /**
     * 取该枪的握持类型，失败或无枪时返回 {@code null}。
     *
     * <p>与 {@code getModifiedGun} 路径解耦，供 {@link #getAnimationGunType} 等
     * 需要在「武器数据不全」时兜底的地方使用（实测 SC2 附属 mod 的部分枪缺字段）。</p>
     */
    public static GripType getGripTypeOf(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem gunItem)) return null;
        try {
            Gun modifiedGun = gunItem.getModifiedGun(stack);
            if (modifiedGun == null || modifiedGun.getGeneral() == null) return null;
            return modifiedGun.getGeneral().getGripType(stack);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 该枪用哪套持枪动画：{@code "pistol"} / {@code "rifle"} / {@code "rpg"}。
     *
     * <h3>判据：握持方式优先（GripType），武器类别兜底</h3>
     * <p>姿势本质上由<b>怎么握</b>决定，而不是由<b>算什么武器</b>决定，所以主判据用 SC2 的
     * {@code GripType}（12 种）：</p>
     * <table border="1">
     *   <caption>握持 → 动画</caption>
     *   <tr><th>GripType</th><th>动画</th></tr>
     *   <tr><td>{@code ONE_HANDED} {@code ONE_HANDED_2} {@code DUAL_WIELD}</td><td>{@code pistol}</td></tr>
     *   <tr><td>{@code TWO_HANDED} {@code TWO_HANDED_SHOTGUN} {@code TWO_HANDED_SMG}</td><td>{@code rifle}</td></tr>
     * </table>
     *
     * <p>这样能天然覆盖一些「按类别判会判错」的情形：</p>
     * <ul>
     *   <li>手枪装了枪托 → 握持变成 {@code TWO_HANDED} → 自动走步枪姿势
     *       （旧实现为此写了一段特判，现在由握持统一处理）；</li>
     *   <li>单手握持的截短霰弹枪 / 手枪型短管枪 → 手枪姿势；</li>
     *   <li>SC2 附属 mod 里<b>只写 gripType、不写 weaponType</b> 的枪
     *       （实测 {@code scgunsww1:luger}）→ 不再被误判成步枪。</li>
     * </ul>
     *
     * <h3>两类特例必须优先于握持判定</h3>
     * <ul>
     *   <li><b>机枪</b>（{@code isMinigunGun}）→ {@code rifle}：机枪的握持类型是
     *       {@code MINI_GUN*}，不属于上面两类；按握持会落到兜底，
     *       这里显式归为步枪姿势。</li>
     *   <li><b>火箭筒</b>（{@code isRpgGun}）→ {@code rpg}：握持为 {@code BAZOOKA}，
     *       同样不属于上面两类。</li>
     * </ul>
     *
     * <h3>已知限制（暂不处理）</h3>
     * <p><b>{@code DUAL_WIELD} 是「假双持」</b>——实测 {@code scguns:rat_king_and_queen}：</p>
     * <ul>
     *   <li>它的几何文件里是 {@code gun_body} 统一下的
     *       {@code main1}/{@code main2}、{@code barrel1}/{@code barrel2}、
     *       {@code magazine}/{@code magazine_2}，即<b>一个物品、两根枪管</b>，并不是两把枪；</li>
     *   <li>整个 SC2 里 {@code DUAL_WIELD} <b>只出现在 {@code GripType} 的枚举定义中</b>，
     *       没有任何逻辑类引用它 —— 也就是说 SC2 自己不为它做任何特殊处理
     *       （不换姿势、不换渲染），它更像一个分类标记。</li>
     * </ul>
     * <p>因此「女仆缺少双持动画」这个说法不成立。目前它按 {@code DUAL_WIELD} 归到
     * {@code pistol} 姿势，<b>视觉上是否合适尚未实测判定</b>，暂按现状保留；
     * 若实测发现应改判，把它从上面那一支挪到 {@code TWO_HANDED} 支即可。</p>
     *
     * <p>兜底顺序：握持读不到 → 用武器类别（手枪/马格南 → {@code pistol}，其余 → {@code rifle}）；
     * 类别也读不到 → {@code rifle}（最保守的通用姿势）。</p>
     */
    public static String getAnimationGunType(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem)) return "";

        // ---- 特例优先：这两类有专属动画，且握持类型不在下面的两支里 ----
        if (isMinigunGun(stack)) return "rifle";
        if (isRpgGun(stack)) return "rpg";

        // ---- 主判据：握持方式 ----
        GripType grip = getGripTypeOf(stack);
        if (grip == GripType.ONE_HANDED || grip == GripType.ONE_HANDED_2
                || grip == GripType.DUAL_WIELD) {
            return "pistol";
        }
        if (grip == GripType.TWO_HANDED || grip == GripType.TWO_HANDED_SHOTGUN
                || grip == GripType.TWO_HANDED_SMG) {
            return "rifle";
        }

        // ---- 兜底：握持读不到（或用的是 MINI_GUN/BAZOOKA 之类）时按武器类别 ----
        WeaponType type = getWeaponType(stack);
        if (type == WeaponType.pistol || type == WeaponType.magnum) return "pistol";
        return "rifle";
    }

    /**
     * 这把枪算不算「双手武器」——占两只手，因此不能同时拿盾。
     *
     * <p>判据与 {@link #getAnimationGunType} 的分支完全一致：只有
     * {@code ONE_HANDED} / {@code ONE_HANDED_2} / {@code DUAL_WIELD}（走 {@code pistol} 姿势的那三类）
     * 算单手，其余（{@code TWO_HANDED*}、{@code MINI_GUN*}、{@code BAZOOKA}…）都占两只手 ——
     * 也就是说「姿势用不用两只手臂」就是这里的判据。</p>
     *
     * <p>兜底是保守的：握持与类别都读不到时 {@code getAnimationGunType} 返回 {@code "rifle"}，
     * 于是判为双手、不给盾。宁可少给一面盾，也不要出现「左手举盾、右手还在端步枪」的姿势冲突。</p>
     */
    public static boolean isTwoHandedGun(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem)) return false;
        return !"pistol".equals(getAnimationGunType(stack));
    }

    /**
     * 女仆是不是正忙着「使用物品」—— 吃东西、用治疗品、喝药之类，此时不该开枪。
     *
     * <h3>为什么不直接用 {@code isUsingItem()}</h3>
     * <p><b>举盾格挡也算 {@code isUsingItem()}</b>：TLM 的 {@code MaidUseShieldTask.start()} 是
     * {@code maid.startUsingItem(OFF_HAND)}，被动挡子弹那条路也一样。盾在副手、枪在主手，
     * 两者井水不犯河水 —— 所以「举盾」要从这里排除掉，否则手枪 + 盾的女仆一举盾就停火。</p>
     *
     * <h3>为什么用「使用手 + 副手物品」判断</h3>
     * <p>不能用 {@code isBlocking()} / {@code getUseItem()}：使用中的物品<b>不同步</b>，
     * 客户端拿不到（这里也被动画代码复用，那是客户端渲染路径）。改用两样同步得到的数据 ——
     * 使用手（entity flag）＋ 副手槽位物品。</p>
     */
    public static boolean isBusyUsingItem(Mob mob) {
        if (!mob.isUsingItem()) return false;
        boolean blocking = mob.getUsedItemHand() == InteractionHand.OFF_HAND
                && mob.getOffhandItem().canPerformAction(ItemAbilities.SHIELD_BLOCK);
        return !blocking;
    }

    /**
     * 这次伤害是不是「射弹打中」—— 用来让女仆挨子弹也举盾。
     *
     * <h3>为什么以伤害类型为主判据</h3>
     * <p><b>SC2 的射弹类型很多，继承关系也不统一</b>（实装 0.5.5 里 35 个类，逐个查过）：</p>
     * <ul>
     *   <li>25 个枪弹/霰弹/火箭/手雷类 → 继承 {@code top.ribs.scguns.entity.projectile.ProjectileEntity}
     *       （注意它继承的是 {@code Entity}，<b>不是</b>原版 {@code Projectile}）；</li>
     *   <li>{@code EnemyProjectileEntity} / {@code turret.TurretProjectileEntity} → 继承原版 {@code AbstractArrow}，
     *       伤害走原版 {@code minecraft:arrow}（本身就在 {@code is_projectile} 标签里，TLM 自己就认）；</li>
     *   <li>{@code RaidFlareEntity}（{@code ThrowableProjectile}）、{@code TraumaHookEntity}（{@code FishingHook}）、
     *       {@code SulfurGasCloudEntity}（{@code Entity}）→ 这几个压根不造成伤害。</li>
     * </ul>
     * <p>而所有真正造成伤害的枪械射弹，都经 {@code ModDamageTypes.Sources} 走同一个伤害类型
     * <b>{@code scguns:bullet}</b>（子类没自己写伤害的，也由 {@code ProjectileEntity} 基类走这条路）。
     * 所以「伤害类型」才是稳定判据，按类名/继承去白名单迟早漏。</p>
     *
     * <h3>判定顺序</h3>
     * <ol>
     *   <li>伤害类型是 {@code scguns:bullet} → 是（覆盖 SC2 全部枪械射弹，含附属 mod 的枪）；</li>
     *   <li>已经是原版弹射物伤害（{@code IS_PROJECTILE}）→ 否，交给 TLM 自己那条被动举盾；</li>
     *   <li>兜底：来源实体是弹射物体系（原版 {@code Projectile}、SC2 的 {@code ProjectileEntity}，
     *       或 SC2 射弹包里的类）→ 也算 —— 这样以后新增的射弹类型、或别的枪械 mod 用自己的伤害类型时，
     *       不用再改代码。</li>
     * </ol>
     */
    public static boolean isBulletDamage(DamageSource source) {
        if (source == null) return false;

        // 1) SC2 枪械射弹的统一伤害类型
        var type = source.typeHolder();
        if (type != null) {
            var key = type.unwrapKey();
            if (key.isPresent()) {
                ResourceLocation id = key.get().location();
                if (SCGUNS_NS.equals(id.getNamespace()) && "bullet".equals(id.getPath())) {
                    return true;
                }
            }
        }

        // 2) 原版弹射物伤害交给 TLM 原本那条路，不重复处理
        if (source.is(DamageTypeTags.IS_PROJECTILE)) return false;

        // 3) 兜底：来源是个射弹实体（原版体系 / SC2 体系 / SC2 射弹包）
        Entity direct = source.getDirectEntity();
        if (direct == null) return false;
        return direct instanceof Projectile
                || direct instanceof ProjectileEntity
                || direct.getClass().getName().startsWith(SCGUNS_PROJECTILE_PACKAGE);
    }

    public static boolean hasBayonet(ItemStack stack) {        if (!(stack.getItem() instanceof GunItem gunItem)) return false;
        return gunItem.hasBayonet(stack) || stack.is(ModTags.Items.BUILT_IN_BAYONET);
    }

    public static float getMeleeReach(ItemStack stack) {
        float base = 2.5f;
        if (stack.getItem() instanceof GunItem gunItem) {
            Gun modified = gunItem.getModifiedGun(stack);
            if (modified != null && modified.getGeneral() != null) {
                base = modified.getGeneral().getMeleeReach();
            }
        }
        return base + 0.5f;
    }

    public static int getMeleeCooldownTicks(ItemStack stack) {
        return 20;
    }

    public static float getMeleeDamage(ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem gunItem)) return 1.0f;
        Gun modified = gunItem.getModifiedGun(stack);
        if (modified == null || modified.getGeneral() == null) return 1.0f;
        return modified.getGeneral().getMeleeDamage();
    }

    public static int performMeleeAttack(EntityMaid maid, LivingEntity target, ItemStack gun) {
        if (!(gun.getItem() instanceof GunItem gunItem)) return 10;
        float baseDamage = getMeleeDamage(gun);
        boolean hasBayonet = gunItem.hasBayonet(gun);
        if (hasBayonet) {
            baseDamage += GunModifierHelper.getAdditionalDamage(gun, true);
        }

        float damage = baseDamage;
        if (hasBayonet) {
            damage += getBayonetEnchantmentDamage(gun, target);
        }

        DamageSource damageSource = maid.damageSources().mobAttack(maid);
        target.hurt(damageSource, damage);
        if (hasBayonet) {
            applyBayonetEnchantmentEffects(maid, target, gun);
        }
        applyKnockback(maid, target, gun);

        // 横扫 AoE（参考 TLM EntityMaid.doSweepHurt：以目标为中心按扇形/范围伤害）
        AABB sweepRange = target.getBoundingBox().inflate(1.0D, 0.25D, 1.0D);
        List<LivingEntity> swept = maid.level().getEntitiesOfClass(LivingEntity.class, sweepRange,
                e -> e != maid && e != target && e.isAlive()
                        && !maid.isAlliedTo(e) && maid.hasLineOfSight(e) && maid.canAttackType(e.getType()));
        if (!swept.isEmpty()) {
            float yawRadians = maid.getYRot() * ((float) Math.PI / 180F);
            double knockX = (double) Mth.sin(yawRadians) * 0.5D;
            double knockZ = (double) (-Mth.cos(yawRadians)) * 0.5D;
            for (LivingEntity entity : swept) {
                float sweepDamage = baseDamage * 0.5F;
                if (hasBayonet) {
                    sweepDamage += getBayonetEnchantmentDamage(gun, entity) * 0.5F;
                    applyBayonetEnchantmentEffects(maid, entity, gun);
                }
                entity.push(knockX, 0.0D, knockZ);
                entity.hurt(damageSource, sweepDamage);
            }
        }

        maid.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.9f + maid.getRandom().nextFloat() * 0.2f);
        if (maid.level() instanceof ServerLevel serverLevel) {
            // 横扫粒子放在女仆「正前方」。
            //
            // 修过的 bug：旧实现自己算三角函数，写成
            //     px = x - sin(yaw)
            //     pz = z + cos(yaw)
            // 而 Minecraft 里 yaw 对应的前方向量是 (sin(yaw), 0, -cos(yaw))，
            // 所以那两个符号正好写反，粒子落在了女仆<b>正后方</b>。
            // （同一段代码上方的击退量用的是正确符号，两者当初就不一致。）
            //
            // 现在直接取 getLookAngle()（视线单位向量），既不必再手算三角函数、
            // 也不会出现符号写反的可能。
            Vec3 look = maid.getLookAngle();
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    maid.getX() + look.x, maid.getY() + maid.getBbHeight() * 0.5D, maid.getZ() + look.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        return getMeleeCooldownTicks(gun);
    }

    /** 刺刀附魔伤害（对齐 SCG MeleeAttackHandler.getEnchantmentDamageFromBayonet：锐利/亡灵杀手/节肢杀手 × 0.70）。 */
    private static float getBayonetEnchantmentDamage(ItemStack gun, LivingEntity target) {
        if (!(gun.getItem() instanceof GunItem gunItem)) return 0.0f;
        float enchantmentDamage = 0.0f;
        for (IAttachment.Type type : IAttachment.Type.values()) {
            ItemStack attachmentStack = gunItem.getAttachment(gun, type);
            if (!attachmentStack.isEmpty() && attachmentStack.getItem() instanceof BayonetItem) {
                // 1.21：DamageEnchantment 类没了（锐利/亡灵杀手/节肢杀手都成了数据包附魔），
                // 它的 getDamageBonus(等级, MobType) 由插件本体的 ScEnchants.getDamageBonus 复现，
                // 结果与「累加每个 DamageEnchantment 的加成」完全一致。
                enchantmentDamage += ScEnchants.getDamageBonus(attachmentStack, MobType.of(target)) * 0.70F;
            }
        }
        return enchantmentDamage;
    }

    /** 刺刀附魔效果（对齐 SCG applyEnchantmentEffects：火焰附加 / 击退）。 */
    private static void applyBayonetEnchantmentEffects(LivingEntity attacker, LivingEntity target, ItemStack gun) {
        if (!(gun.getItem() instanceof GunItem gunItem)) return;
        for (IAttachment.Type type : IAttachment.Type.values()) {
            ItemStack attachmentStack = gunItem.getAttachment(gun, type);
            if (!attachmentStack.isEmpty() && attachmentStack.getItem() instanceof BayonetItem) {
                // 1.21：EnchantmentHelper.getEnchantments(ItemStack) 没了，附魔改成按 Holder 读的组件，
                // ScEnchants.getEnchantments 就是插件本体为此写的 1.20.1 等价物。
                Map<Holder<Enchantment>, Integer> enchantments = ScEnchants.getEnchantments(attachmentStack);
                for (Map.Entry<Holder<Enchantment>, Integer> entry : enchantments.entrySet()) {
                    Holder<Enchantment> enchantment = entry.getKey();
                    int level = entry.getValue();
                    if (ScEnchants.is(enchantment, Enchantments.FIRE_ASPECT)) {
                        target.igniteForSeconds(level * 4);
                    } else if (ScEnchants.is(enchantment, Enchantments.KNOCKBACK)) {
                        Vec3 direction = target.position().subtract(attacker.position()).normalize();
                        target.knockback(level * 0.5F, -direction.x(), -direction.z());
                    }
                }
            }
        }
    }

    /** 枪身击退附魔（对齐 SCG applyKnockback：0.4 + 击退等级 × 0.5）。 */
    private static void applyKnockback(LivingEntity attacker, LivingEntity target, ItemStack gun) {
        int knockbackLevel = ScEnchants.level(gun, Enchantments.KNOCKBACK);
        Vec3 direction = target.position().subtract(attacker.position()).normalize();
        target.knockback(0.4F + knockbackLevel * 0.5F, -direction.x(), -direction.z());
    }
}