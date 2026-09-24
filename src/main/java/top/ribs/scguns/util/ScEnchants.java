package top.ribs.scguns.util;


import top.ribs.scguns.util.ScEnchants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Enchantment helpers for the 1.21 enchantment rework.
 *
 * <p>1.21 made enchantments datapack entries addressed by
 * {@link ResourceKey}, so {@code Enchantments.SHARPNESS} is no longer an
 * {@code Enchantment} and the Forge helpers that took one
 * ({@code getDamageBonus}, {@code getKnockbackBonus}, {@code getFireAspect},
 * {@code getSweepingDamageRatio}, {@code hasBindingCurse}) are gone. The levels
 * are read straight off the {@code ENCHANTMENTS} component, which needs no
 * registry access, and the vanilla 1.20.1 formulas are reproduced here so the
 * gun damage code keeps behaving the same.</p>
 */
public final class ScEnchants {
    private ScEnchants() {
    }

    /** Level of one enchantment on a stack, or 0. */
    public static int level(ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            if (holder.unwrapKey().filter(key::equals).isPresent()) {
                return enchantments.getLevel(holder);
            }
        }
        return 0;
    }

    public static boolean has(ItemStack stack, ResourceKey<Enchantment> key) {
        return level(stack, key) > 0;
    }

    /** True when a holder is the given enchantment (replaces `ScEnchants.is(holder, Enchantments.X)`). */
    public static boolean is(Holder<Enchantment> holder, ResourceKey<Enchantment> key) {
        return holder != null && holder.unwrapKey().filter(key::equals).isPresent();
    }

    /** Map lookup by key for snapshots taken with {@link #getEnchantments}. */
    public static int get(Map<Holder<Enchantment>, Integer> map, ResourceKey<Enchantment> key) {
        for (Map.Entry<Holder<Enchantment>, Integer> entry : map.entrySet()) {
            if (is(entry.getKey(), key)) {
                return entry.getValue();
            }
        }
        return 0;
    }

    public static boolean contains(Map<Holder<Enchantment>, Integer> map, ResourceKey<Enchantment> key) {
        for (Holder<Enchantment> holder : map.keySet()) {
            if (is(holder, key)) {
                return true;
            }
        }
        return false;
    }

    /** Level of one enchantment across a living entity's equipment (vanilla semantics). */
    public static int level(LivingEntity entity, ResourceKey<Enchantment> key) {
        if (entity == null) {
            return 0;
        }
        int total = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            total += level(entity.getItemBySlot(slot), key);
        }
        return total;
    }

    public static int mainHandLevel(LivingEntity entity, ResourceKey<Enchantment> key) {
        return entity == null ? 0 : level(entity.getMainHandItem(), key);
    }

    /** Vanilla 1.20.1 {@code EnchantmentHelper.getDamageBonus}. */
    public static float getDamageBonus(ItemStack stack, MobType mobType) {
        float bonus = 0.0F;
        int sharpness = level(stack, Enchantments.SHARPNESS);
        if (sharpness > 0) {
            bonus += 1.0F + 0.5F * (sharpness - 1);
        }
        if (mobType == MobType.UNDEAD) {
            int smite = level(stack, Enchantments.SMITE);
            if (smite > 0) {
                bonus += 2.5F * smite;
            }
        } else if (mobType == MobType.ARTHROPOD) {
            int bane = level(stack, Enchantments.BANE_OF_ARTHROPODS);
            if (bane > 0) {
                bonus += 2.5F * bane;
            }
        }
        return bonus;
    }

    public static int getKnockbackBonus(LivingEntity entity) {
        return mainHandLevel(entity, Enchantments.KNOCKBACK);
    }

    public static int getFireAspect(LivingEntity entity) {
        return mainHandLevel(entity, Enchantments.FIRE_ASPECT);
    }

    public static float getSweepingDamageRatio(LivingEntity entity) {
        int sweeping = mainHandLevel(entity, Enchantments.SWEEPING_EDGE);
        return sweeping > 0 ? 1.0F - 1.0F / (sweeping + 1.0F) : 0.0F;
    }

    public static boolean hasBindingCurse(ItemStack stack) {
        return has(stack, Enchantments.BINDING_CURSE);
    }

    public static boolean hasVanishingCurse(ItemStack stack) {
        return has(stack, Enchantments.VANISHING_CURSE);
    }

    /** Snapshot of a stack's enchantments, keyed by holder. */
    public static Map<Holder<Enchantment>, Integer> getEnchantments(ItemStack stack) {
        Map<Holder<Enchantment>, Integer> map = new LinkedHashMap<>();
        if (stack == null || stack.isEmpty()) {
            return map;
        }
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            map.put(holder, enchantments.getLevel(holder));
        }
        return map;
    }

    public static void setEnchantments(ItemStack stack, Map<Holder<Enchantment>, Integer> enchantments) {
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.forEach(mutable::set);
        stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
    }

    /**
     * Resolves a datapack enchantment key to the {@link Holder} the 1.21 API
     * writes onto a stack.
     *
     * <p>1.21 enchantments live in a datapack registry, so the holder can only
     * come from a running server's registries; this returns {@code null} when no
     * server is up (a caller that needs it must then skip the write rather than
     * corrupt the stack with an unregistered enchantment).</p>
     */
    @Nullable
    public static Holder<Enchantment> holder(ResourceKey<Enchantment> key) {
        if (key == null) {
            return null;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        Registry<Enchantment> registry = server.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        return registry.getHolder(key).orElse(null);
    }

    /**
     * 1.21 replacement for the removed {@code ItemStack#enchant(Enchantment, int)}
     * overload that took a plain enchantment: applies (replacing any existing
     * level) the enchantment named by {@code key}. Silently does nothing when the
     * enchantment is not present in the current datapack.
     */
    public static boolean enchant(ItemStack stack, ResourceKey<Enchantment> key, int level) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Holder<Enchantment> holder = holder(key);
        if (holder == null) {
            return false;
        }
        stack.enchant(holder, level);
        return true;
    }
}
