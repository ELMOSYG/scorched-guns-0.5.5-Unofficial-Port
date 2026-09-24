package top.ribs.scguns.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Rarity;

/**
 * Scorched Guns music discs.
 *
 * <p>1.21 removed {@code RecordItem}: a disc is now a plain item carrying the
 * {@code JUKEBOX_PLAYABLE} component, which points at a {@link JukeboxSong}
 * datapack entry ({@code data/scguns/jukebox_song/<id>.json}) holding the sound,
 * the length and the comparator output.</p>
 */
public final class ModJukeboxSongs {
    public static final ResourceKey<JukeboxSong> MASS_PRODUCTION = key("mass_production");
    public static final ResourceKey<JukeboxSong> MASS_DESTRUCTION = key("mass_destruction");
    public static final ResourceKey<JukeboxSong> MASS_DESTRUCTION_EXTENDED = key("mass_destruction_extended");

    private ModJukeboxSongs() {
    }

    private static ResourceKey<JukeboxSong> key(String name) {
        return ResourceKey.create(Registries.JUKEBOX_SONG,
                ResourceLocation.fromNamespaceAndPath("scguns", name));
    }

    /** Properties for a music disc item: single stack, rare, playable in a jukebox. */
    public static Item.Properties disc(ResourceKey<JukeboxSong> song) {
        return new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(song);
    }
}
