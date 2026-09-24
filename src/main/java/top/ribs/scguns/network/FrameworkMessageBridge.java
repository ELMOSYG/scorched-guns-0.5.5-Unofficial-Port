package top.ribs.scguns.network;

import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Adapter between the 1.20.1 Framework message classes shipped with Scorched
 * Guns and the codec based Framework 0.13 API used on Minecraft 1.21.1.
 *
 * <p>Framework 0.13 dropped the {@code PlayMessage} base class together with the
 * old {@code Message} interface: a play message is now registered with a
 * {@link StreamCodec} plus a handler {@link BiConsumer}. Every Scorched Guns
 * message class still implements the equivalent 1.20.1 shape
 * ({@code X(FriendlyByteBuf-free default ctor)}, {@code decode(FriendlyByteBuf)},
 * {@code encode(X, FriendlyByteBuf)} and {@code handle(X, MessageContext)}), so
 * this bridge exposes that shape as the exact codec/handler pair the new API
 * wants. It keeps the 52 message implementations untouched, which is why the
 * port does not need to re-derive every wire format by hand.</p>
 *
 * <p>The lookup is deliberately tolerant of the two remaining deviations found
 * in the message package, neither of which the bridge can normalise by changing
 * the classes themselves:</p>
 * <ul>
 *     <li>a few messages already declare {@link RegistryFriendlyByteBuf}
 *     because their body uses a {@code STREAM_CODEC}; that is the buffer type
 *     Framework actually supplies, so such a method is picked up as is,</li>
 *     <li>{@code S2CMessageSyncGunData} predates the convention entirely and
 *     only offers {@code X(FriendlyByteBuf)} plus {@code toBytes(FriendlyByteBuf)};
 *     the original 1.20.1 handler never registered it, so it is adapted here and
 *     its (unreachable) handler fails loudly instead of silently dropping it.</li>
 * </ul>
 *
 * <p>Lookups happen once per message class; the resulting normalised
 * encode/decode/handle operations are cached.</p>
 */
public final class FrameworkMessageBridge {
    private FrameworkMessageBridge() {
    }

    /**
     * One message class resolved to plain operations. Encoding and decoding are
     * expressed in terms of {@link RegistryFriendlyByteBuf} because that is what
     * Framework 0.13 hands to a play message codec; a class whose method still
     * declares the 1.20.1 {@link FriendlyByteBuf} accepts that instance unchanged.
     */
    private record Accessor(Function<RegistryFriendlyByteBuf, Object> decode,
                            BiConsumer<Object, RegistryFriendlyByteBuf> encode,
                            BiConsumer<Object, MessageContext> handle) {
    }

    /**
     * Stand-in for a message that carries no {@code handle(X, MessageContext)}.
     * Such a class is dead code inherited from 1.20.1, where the original packet
     * handler never registered it. Registration still has to succeed, so the
     * placeholder reports the problem if the message is ever actually received.
     */
    private static final BiConsumer<Object, MessageContext> NO_HANDLER = (message, context) -> {
        throw new UnsupportedOperationException("Scorched Guns message " + message.getClass().getName()
                + " has no handle(X, MessageContext) method and was never registered by the 1.20.1 packet handler");
    };

    private static final Map<Class<?>, Accessor> ACCESSORS = new ConcurrentHashMap<>();

    private static Accessor accessor(Class<?> type) {
        return ACCESSORS.computeIfAbsent(type, key -> {
            try {
                Method decode = findMethod(key, "decode", key, RegistryFriendlyByteBuf.class);
                Method encode = findMethod(key, "encode", void.class, key, RegistryFriendlyByteBuf.class);
                Method handle = findMethod(key, "handle", void.class, key, MessageContext.class);
                if (decode != null && encode != null) {
                    Object prototype = newInstance(noArgsConstructor(key));
                    BiConsumer<Object, MessageContext> messageHandler = NO_HANDLER;
                    if (handle != null) {
                        messageHandler = (message, context) -> invoke(handle, message, message, context);
                    }
                    return new Accessor(
                            buffer -> invoke(decode, prototype, buffer),
                            (message, buffer) -> invoke(encode, message, message, buffer),
                            messageHandler);
                }

                // Legacy shape: X(FriendlyByteBuf) plus toBytes(FriendlyByteBuf).
                Constructor<?> legacyDecode = bufferConstructor(key);
                Method toBytes = findMethod(key, "toBytes", void.class, FriendlyByteBuf.class);
                if (legacyDecode != null && toBytes != null) {
                    return new Accessor(
                            buffer -> newInstance(legacyDecode, buffer),
                            (message, buffer) -> invoke(toBytes, message, buffer),
                            NO_HANDLER);
                }
                throw new NoSuchMethodException(key.getName() + " does not follow the expected encode/decode/handle shape");
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Scorched Guns message " + key.getName()
                        + " does not follow the expected encode/decode/handle shape", e);
            }
        });
    }

    private static Constructor<?> noArgsConstructor(Class<?> type) throws NoSuchMethodException {
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor;
    }

    /** Single argument constructor taking a packet buffer, used by the legacy message shape. */
    private static Constructor<?> bufferConstructor(Class<?> type) {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == 1
                    && constructor.getParameterTypes()[0].isAssignableFrom(RegistryFriendlyByteBuf.class)) {
                constructor.setAccessible(true);
                return constructor;
            }
        }
        return null;
    }

    /**
     * Resolves a public message method by name, arity and return type. Both the
     * 1.20.1 {@link FriendlyByteBuf} parameter and the 1.21.1
     * {@link RegistryFriendlyByteBuf} one are accepted; an exact parameter match
     * wins over a merely assignable one. Returns {@code null} when the class does
     * not declare such a method.
     */
    private static Method findMethod(Class<?> type, String name, Class<?> returnType, Class<?>... argumentTypes) {
        Method assignable = null;
        for (Method candidate : type.getMethods()) {
            if (!candidate.getName().equals(name)
                    || candidate.getParameterCount() != argumentTypes.length
                    || !returnType.isAssignableFrom(candidate.getReturnType())) {
                continue;
            }
            Class<?>[] parameters = candidate.getParameterTypes();
            boolean exact = true;
            boolean compatible = true;
            for (int i = 0; i < parameters.length; i++) {
                exact &= parameters[i] == argumentTypes[i];
                compatible &= parameters[i].isAssignableFrom(argumentTypes[i]);
            }
            if (exact) {
                return candidate;
            }
            if (compatible && assignable == null) {
                assignable = candidate;
            }
        }
        return assignable;
    }

    /** Re-throws the real cause instead of wrapping it in a reflection wrapper. */
    private static Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw unwrap(e);
        }
    }

    private static Object newInstance(Constructor<?> constructor, Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw unwrap(e);
        }
    }

    private static RuntimeException unwrap(ReflectiveOperationException e) {
        Throwable cause = e instanceof InvocationTargetException invocation ? invocation.getCause() : e;
        if (cause instanceof RuntimeException runtime) {
            return runtime;
        }
        if (cause instanceof Error error) {
            throw error;
        }
        return new RuntimeException(cause);
    }

    @SuppressWarnings("unchecked")
    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> codec(Class<T> type) {
        Accessor accessor = accessor(type);
        return StreamCodec.of(
                (buffer, message) -> accessor.encode().accept(message, buffer),
                buffer -> (T) accessor.decode().apply(buffer));
    }

    public static <T> BiConsumer<T, MessageContext> handler(Class<T> type) {
        Accessor accessor = accessor(type);
        return (message, context) -> accessor.handle().accept(message, context);
    }
}
