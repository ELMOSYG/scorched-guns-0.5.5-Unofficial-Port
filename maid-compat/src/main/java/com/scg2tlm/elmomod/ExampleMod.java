package com.scg2tlm.elmomod;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.scg2tlm.elmomod.client.DefaultGeckoAnimationInjector;
import com.scg2tlm.elmomod.client.SCG2TLMClothConfigListener;
import com.scg2tlm.elmomod.compat.BetterCombatHandler;
import com.scg2tlm.elmomod.compat.MaidProjectileHandler;
import com.scg2tlm.elmomod.compat.MedkitInteractHandler;
import com.scg2tlm.elmomod.compat.SC2GunCompat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixins;
import java.util.Optional;
import java.util.UUID;

@Mod(ExampleMod.MODID)
public class ExampleMod {
    public static final String MODID = "scg2_maid_compat";

    /**
     * Touhou Little Maid, the mod this compat exists for. Every class that touches TLM types is
     * only ever reached from behind this check: the constructor below returns before registering
     * anything, and the mixin config is gated by {@code MaidMixinPlugin}, so with TLM absent no
     * TLM class is loaded and no mixin is applied.
     */
    public static final String TLM_MODID = "touhou_little_maid";

    /**
     * Cloth Config's mod id.
     *
     * <p>Optional in both directions: TLM does not depend on it either, it only uses it when present
     * (its {@code compat.cloth} package). Guarded before the config screen listener is registered, so
     * the compat keeps working with the options in the TOML file alone.</p>
     */
    public static final String CLOTH_CONFIG_MODID = "cloth_config";

    private static final Logger LOGGER = LogManager.getLogger(MODID);

    /**
     * 1.21.1 / NeoForge entry point. Forge's {@code FMLJavaModLoadingContext} and
     * {@code ModLoadingContext} no longer exist, so the bus and the container arrive as
     * constructor parameters (NeoForge injects them).
     */
    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, SCG2TLMConfig.SPEC);

        if (!ModList.get().isLoaded(TLM_MODID)) {
            LOGGER.info("Touhou Little Maid is not installed - the Scorched Guns maid compat is disabled.");
            return;
        }

        NeoForge.EVENT_BUS.register(this);

        // Written as explicit registrations instead of @EventBusSubscriber: annotation scanning
        // would load these classes (and therefore TLM) even when TLM is absent.
        NeoForge.EVENT_BUS.addListener(MaidProjectileHandler::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(MaidProjectileHandler::onLivingHurt);
        NeoForge.EVENT_BUS.addListener(MaidProjectileHandler::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(MedkitInteractHandler::onEntityInteract);

        if (FMLEnvironment.dist.isClient()) {
            registerClientOnlyHandlers();
        }

        // scgextra 是可选的：只有装了 scgextra 才注册其专属 mixin 配置，避免缺失类导致崩溃
        if (ModList.get().isLoaded("scgextra")) {
            Mixins.addConfiguration("scg2_maid_compat_scgextra.mixins.json");
        }
    }

    /**
     * Kept in its own method so the dedicated server never has to resolve the client-only handler
     * classes: it is only called when {@code FMLEnvironment.dist.isClient()}.
     *
     * <p>The Cloth Config screen is registered here too, but only when Cloth Config is actually
     * loaded: it was dropped during the port because Cloth Config was neither a dependency of this mod
     * nor of TLM, and the instance did not have it. It is present again now (HANDOFF section 47), and
     * the hook is TLM's own {@code AddClothConfigEvent}, so no Forge-only class is involved.</p>
     */
    private static void registerClientOnlyHandlers() {
        NeoForge.EVENT_BUS.addListener(BetterCombatHandler::onClientTick);
        NeoForge.EVENT_BUS.addListener(DefaultGeckoAnimationInjector::onDefaultGeckoAnimation);

        // Guarded like everything else TLM- or Cloth-facing: the listener's parameter type comes from
        // TLM and its entry builders from Cloth Config, so mentioning the class without both loaded
        // would throw NoClassDefFoundError.
        if (ModList.get().isLoaded(CLOTH_CONFIG_MODID)) {
            NeoForge.EVENT_BUS.addListener(SCG2TLMClothConfigListener::onAddClothConfig);
        } else {
            LOGGER.info("Cloth Config is not installed - the maid compat's options stay in "
                + "config/scg2_maid_compat-common.toml instead of a config screen.");
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof top.ribs.scguns.entity.projectile.ProjectileEntity)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof EntityMaid shooter)) {
            return;
        }

        if (event.getEntity() instanceof Player
                && shooter.getOwnerUUID() != null && shooter.getOwnerUUID().equals(event.getEntity().getUUID())) {
            event.setCanceled(true);
            return;
        }

        if (event.getEntity() instanceof EntityMaid victim) {
            Optional<? extends LivingEntity> a = shooter.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
            Optional<? extends LivingEntity> b = victim.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
            if (a.isPresent() && b.isPresent() && a.get() == b.get()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onMaidHurtTargetBack(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof EntityMaid maid)) {
            return;
        }
        if (maid.level().isClientSide) {
            return;
        }
        if (!SC2GunCompat.isSC2Gun(maid.getMainHandItem())) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (attacker == maid || !attacker.isAlive()) {
            return;
        }
        if (attacker instanceof EntityMaid) {
            return;
        }
        UUID owner = maid.getOwnerUUID();
        if (owner != null && attacker.getUUID().equals(owner)) {
            return;
        }
        maid.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, attacker);
    }

    // SCG 本体 bug 修复：Viventrum 没有攻击黑名单，会索敌主人名下的其他随从（宠物）。取消该索敌。
    @SubscribeEvent
    public void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof top.ribs.scguns.entity.monster.ViventrumEntity viventrum)) {
            return;
        }
        // NeoForge renamed these accessors: getNewTarget()/setNewTarget() became
        // getNewAboutToBeSetTarget()/setNewAboutToBeSetTarget().
        if (!(event.getNewAboutToBeSetTarget() instanceof TamableAnimal targetPet)) {
            return;
        }
        LivingEntity owner = viventrum.getOwner();
        if (owner != null && owner == targetPet.getOwner()) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    // SCG Extra 阵营友伤防护：同一阵营的怪互相攻击时取消伤害（覆盖所有 SCG 弹体类型 + 近战/爆炸）。
    // 放行玩家/女仆（"player" 阵营）发起的伤害，保留原有行为。
    @SubscribeEvent
    public void onFactionFriendlyFire(LivingIncomingDamageEvent event) {
        if (!SCG2TLMConfig.PREVENT_FACTION_FRIENDLY_FIRE.get()) {
            return;
        }
        // scgextra 未安装时直接跳过。这里刻意不放进 SCGExtraCompatHelper：
        // 那个类要保持「零依赖、纯反射」，才能在最坏情况下安全降级。
        if (!ModList.get().isLoaded("scgextra")) {
            return;
        }
        if (event.getEntity().level().isClientSide) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) {
            attacker = event.getSource().getDirectEntity();
        }
        if (attacker instanceof Projectile projectile) {
            attacker = projectile.getOwner();
        }
        if (!(attacker instanceof LivingEntity attackerLiving)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        EntityType<?> type = attackerLiving.getType();
        if (type == EntityType.PLAYER || type == InitEntities.MAID.get()) {
            return;
        }
        if (SCGExtraCompatHelper.isFriendlies(attackerLiving, victim)) {
            event.setCanceled(true);
        }
    }
}
