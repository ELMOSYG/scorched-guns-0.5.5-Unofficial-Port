package top.ribs.scguns.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Scorched Guns enchantments.
 *
 * <p>1.21 turned {@code Enchantment} into a datapack registry, so these are no
 * longer registered from code: every entry lives in
 * {@code data/scguns/enchantment/<id>.json} and this class only exposes the keys
 * used to look levels up and to write data.</p>
 */
public final class ModEnchantments {
    public static final ResourceKey<Enchantment> QUICK_HANDS = key("quick_hands");
    public static final ResourceKey<Enchantment> TRIGGER_FINGER = key("trigger_finger");
    public static final ResourceKey<Enchantment> LIGHTWEIGHT = key("lightweight");
    public static final ResourceKey<Enchantment> COLLATERAL = key("collateral");
    public static final ResourceKey<Enchantment> RECLAIMED = key("reclaimed");
    public static final ResourceKey<Enchantment> ACCELERATOR = key("accelerator");
    public static final ResourceKey<Enchantment> PUNCTURING = key("puncturing");
    public static final ResourceKey<Enchantment> SHELL_CATCHER = key("shell_catcher");
    public static final ResourceKey<Enchantment> BANZAI = key("banzai");
    public static final ResourceKey<Enchantment> HEAVY_SHOT = key("heavy_shot");
    public static final ResourceKey<Enchantment> ELEMENTAL_POP = key("elemental_pop");
    public static final ResourceKey<Enchantment> WATER_PROOF = key("waterproof");
    public static final ResourceKey<Enchantment> HOT_BARREL = key("hot_barrel");
    public static final ResourceKey<Enchantment> GUN_RUST = key("gun_rust");
    public static final ResourceKey<Enchantment> CORRODED = key("corroded");

    private ModEnchantments() {
    }

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("scguns", name));
    }
}
