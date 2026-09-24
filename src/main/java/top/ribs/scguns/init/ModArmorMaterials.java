package top.ribs.scguns.init;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Scorched Guns armor materials.
 *
 * <p>In 1.21.1 {@code ArmorMaterial} is still an ordinary <b>built-in</b> registry,
 * populated by {@code ArmorMaterials.bootstrap} and extended by mods through a
 * {@link DeferredRegister} over {@link BuiltInRegistries#ARMOR_MATERIAL}. It only
 * becomes a data-driven registry in <b>1.21.2</b>.</p>
 *
 * <p>The earlier port treated it as a data pack registry: the constants were built
 * with {@code DeferredHolder.create(Registries.ARMOR_MATERIAL, id)}, which nothing
 * ever binds, and the values lived in {@code data/scguns/armor_material/*.json},
 * which 1.21.1 never reads. Reading a material therefore threw
 * {@code NullPointerException: Trying to access unbound value:
 * ResourceKey[minecraft:armor_material / scguns:treated_brass]}.</p>
 *
 * <p>Durability is not part of the material in 1.21.1 either: the per-piece
 * multiplier has to be applied to the item's own properties, which
 * {@link #durability(Item.Properties, Holder, ArmorItem.Type)} does with the
 * factors the 0.5.5 enum carried.</p>
 */
public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> REGISTER =
            DeferredRegister.create(BuiltInRegistries.ARMOR_MATERIAL, "scguns");

    /**
     * Declared before the material constants on purpose: Java initialises static
     * fields in textual order, and {@link #register} writes into this map, so a
     * declaration further down would still be null while the constants initialise.
     */
    private static final Map<Holder<ArmorMaterial>, Integer> DURABILITY_FACTORS = new IdentityHashMap<>();

    /**
     * 0.5.5 indexed its defence arrays by {@code ArmorItem.Type.ordinal()}, i.e.
     * {@code HELMET, CHESTPLATE, LEGGINGS, BOOTS}. The order is spelled out in the
     * parameters here so it cannot be transposed again; {@code BODY} (horse armor)
     * takes the chestplate value, as vanilla does.
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SCRAP = register(
            "scrap", 6, 5, 6, 6, 4, 9, SoundEvents.ARMOR_EQUIP_LEATHER, 0.5F, 0.05F,
            () -> Ingredient.of(Items.COPPER_INGOT));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> REDCOAT = register(
            "redcoat", 12, 2, 4, 3, 2, 9, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
            () -> Ingredient.of(Items.LEATHER));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ADRIEN = register(
            "adrien", 22, 3, 6, 6, 4, 8, SoundEvents.ARMOR_EQUIP_IRON, 0.5F, 0.1F,
            () -> Ingredient.of(ModItems.TREATED_IRON_INGOT.get()));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ANTHRALITE = register(
            "anthralite", 32, 3, 5, 4, 3, 12, SoundEvents.ARMOR_EQUIP_GOLD, 1.0F, 0.05F,
            () -> Ingredient.of(ModItems.ANTHRALITE_INGOT.get()));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> DIAMOND_STEEL = register(
            "diamond_steel", 36, 3, 7, 5, 3, 16, SoundEvents.ARMOR_EQUIP_DIAMOND, 2.0F, 0.05F,
            () -> Ingredient.of(ModItems.DIAMOND_STEEL_INGOT.get()));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TREATED_BRASS = register(
            "treated_brass", 30, 4, 5, 5, 4, 10, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.2F,
            () -> Ingredient.of(ModItems.TREATED_BRASS_INGOT.get()));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ANCIENT_BRASS = register(
            "ancient_brass", 16, 3, 5, 4, 3, 10, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.15F,
            () -> Ingredient.of(ModItems.ANCIENT_BRASS.get()));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> EXO_SUIT = register(
            "exo_suit", 200, 1, 1, 1, 1, 6, SoundEvents.ARMOR_EQUIP_NETHERITE, 0.0F, 0.0F,
            () -> Ingredient.of(ModItems.TREATED_IRON_INGOT.get()));

    private ModArmorMaterials() {
    }

    /** 1.21.1 keeps armor durability on the item, not on the material. */
    public static Item.Properties durability(Item.Properties properties,
                                             Holder<ArmorMaterial> material,
                                             ArmorItem.Type type) {
        Integer factor = DURABILITY_FACTORS.get(material);
        return properties.durability(type.getDurability(factor == null ? 15 : factor));
    }

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> register(
            String name, int durabilityFactor,
            int helmet, int chestplate, int leggings, int boots,
            int enchantmentValue, Holder<SoundEvent> equipSound,
            float toughness, float knockbackResistance,
            Supplier<Ingredient> repairIngredient) {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, helmet);
        defense.put(ArmorItem.Type.CHESTPLATE, chestplate);
        defense.put(ArmorItem.Type.LEGGINGS, leggings);
        defense.put(ArmorItem.Type.BOOTS, boots);
        defense.put(ArmorItem.Type.BODY, chestplate);

        DeferredHolder<ArmorMaterial, ArmorMaterial> holder = REGISTER.register(name, () -> new ArmorMaterial(
                defense,
                enchantmentValue,
                equipSound,
                repairIngredient,
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("scguns", name))),
                toughness,
                knockbackResistance));
        DURABILITY_FACTORS.put(holder, durabilityFactor);
        return holder;
    }
}
