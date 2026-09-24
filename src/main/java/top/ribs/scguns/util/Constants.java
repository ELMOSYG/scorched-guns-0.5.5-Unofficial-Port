package top.ribs.scguns.util;

import net.minecraft.world.item.Rarity;

/**
 * Rarity tiers used by the Scorched Guns item registry.
 *
 * <p>0.5.5 registered eleven custom rarities through Forge's
 * {@code Rarity.create(name, colour)} enum extension. 1.21 has no such helper -
 * custom entries would need NeoForge's enum-extension descriptor
 * ({@code META-INF/enumextensions.json}) with a matching constructor, which also
 * changes how the value is referenced from code.</p>
 *
 * <p>The port keeps the names so every call site compiles and folds each custom
 * tier onto the nearest vanilla rarity, chosen from the colour the original used.
 * The only visible difference is the tooltip colour; restoring the exact colours
 * means adding the enum extension descriptor and switching these constants to the
 * generated values.</p>
 */
public class Constants {
    public static final Rarity OCEANIC = Rarity.RARE;           // was BLUE
    public static final Rarity UNIQUE = Rarity.RARE;            // was GREEN
    public static final Rarity PIGLISH = Rarity.UNCOMMON;       // was GOLD
    public static final Rarity SCORCHED = Rarity.RARE;          // was RED
    public static final Rarity DEEP_DARK = Rarity.EPIC;         // was DARK_AQUA
    public static final Rarity ENDISH = Rarity.EPIC;            // was DARK_PURPLE
    public static final Rarity BIZARRE = Rarity.UNCOMMON;       // was GRAY
    public static final Rarity TREATED_BRASS = Rarity.UNCOMMON; // was YELLOW
    public static final Rarity DIAMOND_STEEL = Rarity.RARE;     // was AQUA
    public static final Rarity WRECKER = Rarity.UNCOMMON;       // was DARK_GRAY
    public static final Rarity RUSTY = Rarity.COMMON;           // was a custom orange

    public Constants() {
        super();
    }
}
