package com.scg2tlm.elmomod;

import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Gate for every mixin of this compat.
 *
 * <p>The compat is only meaningful with Touhou Little Maid installed, and most of its mixins either
 * target TLM classes or reference TLM types from inside their own bodies (for example
 * {@code ProjectileEntityMixin} asks whether the shooter is an {@code EntityMaid}). Applying any of
 * them without TLM would therefore blow up with {@code NoClassDefFoundError}, and mixin's default
 * behaviour for a missing target is a hard failure too.</p>
 *
 * <p>So the whole configuration is switched off in one place: the probe below loads no TLM class
 * (only asks whether it is loadable), and {@link #shouldApplyMixin} then answers for every mixin in
 * the config. With TLM absent the compat loads, logs one line, and does nothing at all.</p>
 */
public class MaidMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogManager.getLogger("scg2_maid_compat-mixin");

    /** TLM's maid entity: present exactly when the mod is installed. */
    private static final String TLM_MAID = "com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid";

    private boolean tlmPresent;

    /**
     * Ask whether TLM is installed <b>without loading any of its classes</b>.
     *
     * <p>{@code Class.forName} is wrong here and was measured to be wrong: resolving
     * {@code EntityMaid} loads its superclass chain, which reaches {@code LivingEntity} while
     * mixins are still being prepared, and Mixin then fails the run with
     * "geckolib.mixins.json:common.LivingEntityMixin ... target LivingEntity was loaded too
     * early". Looking the class file up as a resource answers the same question and loads
     * nothing.</p>
     */
    private static boolean probeTlm() {
        try {
            ClassLoader loader = MaidMixinPlugin.class.getClassLoader();
            if (loader.getResource(TLM_MAID.replace('.', '/') + ".class") != null) {
                return true;
            }
        } catch (Throwable ignored) {
            // fall through to the mod list
        }

        try {
            var modList = net.neoforged.fml.loading.LoadingModList.get();
            return modList != null && modList.getModFileById(ExampleMod.TLM_MODID) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
        this.tlmPresent = probeTlm();

        if (this.tlmPresent) {
            LOGGER.info("Touhou Little Maid detected - enabling the Scorched Guns maid compatibility mixins.");
        } else {
            LOGGER.info("Touhou Little Maid not detected - skipping every maid compatibility mixin.");
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return this.tlmPresent;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
