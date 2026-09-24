package top.ribs.scguns.util;


import top.ribs.scguns.util.NbtHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * 1.21 replacement for the removed {@code ItemStack} NBT accessors.
 *
 * <p>Scorched Guns stores its gun state ({@code AmmoCount}, {@code HeatLevel},
 * {@code RechargeCounter}, {@code IsShooting}, ...) as raw NBT. 1.21 moved item
 * data to components; {@code DataComponents.CUSTOM_DATA} is the component that
 * still carries a free-form {@link CompoundTag}, so it is the faithful
 * equivalent of the old Forge behaviour - including the same NBT key names,
 * which downstream mods rely on.</p>
 *
 * <h2>Why writes must not go through {@link #getTag(ItemStack)}</h2>
 *
 * <p>1.21 changed two things that used to make in-place mutation safe:</p>
 * <ul>
 *   <li>{@code ItemStack.copy()} copies its component map <b>shallowly</b>
 *       ({@code PatchedDataComponentMap.copy()} shares the patch map and its
 *       values), so a copy -- {@code AbstractContainerMenu}'s {@code remoteSlots}
 *       shadow, {@code ServerEntity}'s last-sent equipment, ... -- holds the very
 *       same {@link CustomData} instance, hence the very same
 *       {@link CompoundTag}.</li>
 *   <li>Those sync paths only send an update when
 *       {@code ItemStack.matches(old, now)} is false, and that compares
 *       components <b>by value</b>.</li>
 * </ul>
 *
 * <p>Mutating the shared tag in place is therefore invisible to both comparisons:
 * the server ends up with the change and never tells anyone, which players see as
 * "the attachment/ammo/heat only shows up after dropping the item and picking it
 * back up". 1.20.1 could not hit this, because {@code ItemStack.copy()}
 * deep-copied the tag.</p>
 *
 * <p>The rule is therefore:</p>
 * <ul>
 *   <li>{@link #getTag(ItemStack)} -- <b>read-only</b>, returns the live tag (or
 *       null). Cheap, safe to call in render/tick loops.</li>
 *   <li>{@link #getOrCreateTag(ItemStack)} / {@link #getTagForWrite(ItemStack)} --
 *       return a tag that belongs to this stack <b>alone</b> (the component is
 *       re-set with a copy, which detaches it from every earlier copy of the
 *       stack). Use these for anything that mutates.</li>
 * </ul>
 */
public final class NbtHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("scguns-nbt");

    /**
     * Registry access used to encode and decode item stacks.
     *
     * <p>{@code ItemStack.CODEC} writes enchantments as registry references, so a plain
     * {@code NbtOps} cannot encode an enchanted stack at all - it fails with "Can't access registry
     * minecraft:enchantment". That failure used to be swallowed into an empty tag, so an enchanted
     * attachment installed on a gun was written as {@code {}} and read back as nothing: destroyed on
     * both sides (HANDOFF section 58). The provider is set when a level loads or a server starts;
     * until then the codecs fall back to plain NbtOps, which is enough for unenchanted stacks.</p>
     */
    private static volatile HolderLookup.Provider registryAccess;

    private NbtHelper() {
    }

    /** Called by {@code RegistryAccessListener} when a level loads or a server starts. */
    public static void setRegistryAccess(@Nullable HolderLookup.Provider provider) {
        registryAccess = provider;
    }

    /** Whether a provider has been seen yet - for diagnostics only. */
    public static boolean hasRegistryAccess() {
        return registryAccess != null;
    }

    private static com.mojang.serialization.DynamicOps<Tag> ops() {
        return ops(registryAccess);
    }

    /**
     * Codecs bound to one specific registry access.
     *
     * <p>Call sites that know their own side (a player's {@code registryAccess()}) should pass it
     * explicitly instead of relying on the static one above: the tag only ever stores a registry
     * <b>reference</b>, and the reference is re-resolved by whoever decodes it, so encoding with the
     * caller's own registry is correct on both sides (HANDOFF section 59).</p>
     */
    private static com.mojang.serialization.DynamicOps<Tag> ops(@Nullable HolderLookup.Provider provider) {
        return provider == null ? NbtOps.INSTANCE : RegistryOps.create(NbtOps.INSTANCE, provider);
    }

    /** How to describe the registry access used, for the failure logs below. */
    private static String describe(@Nullable HolderLookup.Provider provider) {
        if (provider == null) {
            return "no registry access yet";
        }
        return provider == registryAccess ? "with registry access" : "with the caller's registry access";
    }

    /** Read-only view of the stack's custom-data tag, or null when it has none. */
    public static CompoundTag getTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.getUnsafe();
    }

    public static boolean hasTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && !data.isEmpty();
    }

    /**
     * A writable tag that belongs to this stack alone, created and attached when
     * absent.
     *
     * <p><b>{@code CustomData.of()} copies its argument</b> (it calls
     * {@code CompoundTag.copy()}), so the instance that ends up inside the
     * component is NOT the one passed in.  Returning the freshly built tag here
     * would hand the caller a detached object and silently discard the write --
     * which is exactly what happened to the gun ammo pre-fill: a mob's gun came
     * out with {@code custom_data: {}} instead of {@code {AmmoCount: n}}.  The
     * tag is therefore re-read from the component after storing it.</p>
     *
     * <p>Re-setting the component also detaches the patch from every copy of this
     * stack taken earlier, which is what makes a later in-place write visible to
     * {@code ItemStack.matches} -- see the class javadoc.  A copy of the tag is
     * therefore taken on each call: keep read paths on {@link #getTag(ItemStack)}.</p>
     */
    public static CompoundTag getOrCreateTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data != null ? data.getUnsafe() : new CompoundTag();
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        CustomData stored = stack.get(DataComponents.CUSTOM_DATA);
        return stored != null ? stored.getUnsafe() : tag;
    }

    /**
     * {@link #getTag(ItemStack)}, but the returned tag belongs to this stack alone
     * so an in-place write is picked up by the sync paths.  Returns null exactly
     * when {@link #getTag(ItemStack)} would, so the {@code tag != null} guards
     * around the 0.5.5 write sites keep their original meaning.
     */
    @Nullable
    public static CompoundTag getTagForWrite(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data.getUnsafe()));
        CustomData stored = stack.get(DataComponents.CUSTOM_DATA);
        return stored != null ? stored.getUnsafe() : null;
    }

    public static void setTag(ItemStack stack, CompoundTag tag) {
        // CustomData.of() copies, which is what we want here: the caller keeps
        // ownership of its own tag instead of aliasing the component's.
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    /**
     * 1.21 replacement for {@NbtHelper.itemFromTag(CompoundTag)}: item data now holds
     * components, which need a registry lookup, so the stack is decoded with
     * {@code NbtOps} instead. Unknown items decode to an empty stack, matching the
     * old behaviour of returning an empty stack for unparsable data.
     */
    public static ItemStack itemFromTag(@Nullable CompoundTag tag) {
        return itemFromTag(tag, registryAccess);
    }

    /** {@link #itemFromTag(CompoundTag)} with an explicit registry access - see {@link #ops(HolderLookup.Provider)}. */
    public static ItemStack itemFromTag(@Nullable CompoundTag tag, @Nullable HolderLookup.Provider provider) {
        if (tag == null || tag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var result = ItemStack.CODEC.parse(ops(provider), tag);
        if (result.error().isPresent()) {
            LOGGER.error("Could not decode an item stack ({}); treating it as empty: {}",
                describe(provider),
                result.error().get().message());
            return ItemStack.EMPTY;
        }
        return result.result().orElse(ItemStack.EMPTY);
    }

    /**
     * 1.21 replacement for {@code ItemStack.save(CompoundTag)} round trips.
     *
     * <p>Returns an <b>empty</b> tag when encoding fails, and logs it. Callers must treat an empty
     * result for a non-empty stack as a failure and keep whatever they had, rather than storing the
     * empty tag - that is how the enchanted attachments were being destroyed (HANDOFF section 58).</p>
     */
    public static CompoundTag tagFromItem(ItemStack stack) {
        return tagFromItem(stack, registryAccess);
    }

    /** {@link #tagFromItem(ItemStack)} with an explicit registry access - see {@link #ops(HolderLookup.Provider)}. */
    public static CompoundTag tagFromItem(ItemStack stack, @Nullable HolderLookup.Provider provider) {
        // An empty stack has no representation in ItemStack.CODEC (it rejects minecraft:air and counts
        // outside [1;99]), so this is normal input rather than a failure and must not be logged.
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        var encoded = ItemStack.CODEC.encodeStart(ops(provider), stack);
        if (encoded.result().isEmpty()) {
            LOGGER.error("Could not encode {} ({}); callers must not overwrite existing data with the"
                    + " empty tag this returns: {}",
                stack, describe(provider),
                encoded.error().map(Object::toString).orElse("unknown error"));
            return new CompoundTag();
        }
        Tag tag = encoded.result().get();
        return tag instanceof CompoundTag compound ? compound : new CompoundTag();
    }
}
