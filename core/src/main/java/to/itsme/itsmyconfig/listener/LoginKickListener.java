package to.itsme.itsmyconfig.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.util.AdventureUtil;
import to.itsme.itsmyconfig.util.IMCSerializer;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Translates symbol-prefixed kick messages of players that are refused while logging in
 * (e.g. banned players), which never reach the play-phase disconnect packet processors.
 * <p>
 * Runs at {@link EventPriority#HIGHEST} so moderation plugins have already set their message.
 * On Paper the server's native Adventure component is read and written via reflection, since
 * {@code net.kyori} is relocated in the shaded jar. On Spigot the legacy string API is used.
 */
public final class LoginKickListener implements Listener {

    /**
     * Legacy serializer emitting hex colors in the {@code §x§r§r§g§g§b§b} format Spigot understands.
     */
    private static final LegacyComponentSerializer HEX_LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    /**
     * The server's own (non-relocated) Adventure {@code Component} class, or {@code null} if absent (Spigot).
     */
    private static final Class<?> NATIVE_COMPONENT_CLASS = findNativeComponentClass();

    private static final NativeAccessor PRE_LOGIN_ACCESSOR = NativeAccessor.find(AsyncPlayerPreLoginEvent.class);

    private final ItsMyConfig plugin;

    public LoginKickListener(final ItsMyConfig plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles players refused before login (bans, etc). Called asynchronously.
     *
     * @param event the pre-login event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPreLogin(final AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        this.process("PRE-LOGIN", event, event.getUniqueId(), PRE_LOGIN_ACCESSOR, event::getKickMessage, event::setKickMessage);
    }

    /**
     * Reads the kick message, translates it if it starts with the symbol-prefix, and writes it back.
     * Leaves the message untouched if it has no symbol-prefix, and falls back to
     * {@link #fallback(String)} if translation fails.
     *
     * @param phase        the name of the login phase, used for logging
     * @param event        the login event
     * @param uuid         the unique id of the player being refused
     * @param accessor     the native component accessor, or {@code null} to use the legacy string API
     * @param legacyGetter reads the kick message as a legacy string
     * @param legacySetter writes the kick message as a legacy string
     */
    private void process(
            final String phase,
            final Event event,
            final UUID uuid,
            final @Nullable NativeAccessor accessor,
            final Supplier<String> legacyGetter,
            final Consumer<String> legacySetter
    ) {
        Utilities.debug(() -> "################# %s KICK #################".formatted(phase));

        final String message;
        try {
            message = accessor == null ? legacyGetter.get() : accessor.read(event);
        } catch (final Exception e) {
            this.warn(phase, e);
            return;
        }

        if (message == null || message.isEmpty()) {
            Utilities.debug(() -> "Kick message is null or empty\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        Utilities.debug(() -> "Found kick message: " + message);

        final Optional<String> parsed = Strings.parsePrefixedMessage(message);
        if (parsed.isEmpty()) {
            Utilities.debug(() -> "Kick message doesn't start w/ the symbol-prefix: " + message + "\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        try {
            // getOfflinePlayer(UUID) never performs a blocking profile lookup, unlike getOfflinePlayer(String)
            final Component translated = Utilities.translate(parsed.get(), Bukkit.getOfflinePlayer(uuid));
            Utilities.debug(() -> "Final Result: " + IMCSerializer.toMiniMessage(translated) + "\n" + "Overriding...");
            if (accessor == null) {
                legacySetter.accept(toLegacy(translated));
            } else {
                accessor.write(event, translated);
            }
        } catch (final Exception e) {
            this.warn(phase, e);
            legacySetter.accept(fallback(legacyGetter.get()));
        }

        Utilities.debug(() -> Strings.DEBUG_HYPHEN);
    }

    /**
     * Logs a single-line warning for a failed kick message translation.
     * The full stack trace is only logged while debug is enabled.
     *
     * @param phase     the name of the login phase
     * @param exception the cause of the failure
     */
    private void warn(final String phase, final Exception exception) {
        final Throwable cause = exception instanceof InvocationTargetException && exception.getCause() != null
                ? exception.getCause() : exception;
        this.plugin.getLogger().warning(
                "Failed to translate %s kick message, sending it untranslated: %s".formatted(phase, cause)
        );
        Utilities.debug(() -> "Kick message translation failure", cause);
    }

    /**
     * Serializes a component into a legacy string, using the {@code §x§r§r§g§g§b§b} hex color format.
     *
     * @param component the component to serialize
     * @return the legacy string
     */
    static @NotNull String toLegacy(final @NotNull Component component) {
        return HEX_LEGACY_SERIALIZER.serialize(component);
    }

    /**
     * Strips the symbol-prefix from a legacy kick message without translating it,
     * used when translation fails. Messages without a symbol-prefix are returned as-is.
     *
     * @param legacyMessage the original legacy kick message
     * @return the message without its symbol-prefix, with {@code &} color codes applied
     */
    static String fallback(final String legacyMessage) {
        return Strings.parsePrefixedMessage(legacyMessage)
                .map(stripped -> ChatColor.translateAlternateColorCodes('&', stripped))
                .orElse(legacyMessage);
    }

    private static @Nullable Class<?> findNativeComponentClass() {
        try {
            // split up so the shadow plugin does not relocate this to the shaded Adventure
            return Class.forName("net{}kyori{}adventure{}text{}Component".replace("{}", "."));
        } catch (final ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Reflective access to Paper's {@code kickMessage()} / {@code kickMessage(Component)} methods.
     *
     * @param getter the kick message getter
     * @param setter the kick message setter
     */
    private record NativeAccessor(Method getter, Method setter) {

        /**
         * Looks up the native component accessors of an event class.
         *
         * @param eventClass the event class
         * @return the accessor, or {@code null} if the server does not provide the methods (Spigot)
         */
        static @Nullable NativeAccessor find(final Class<?> eventClass) {
            if (NATIVE_COMPONENT_CLASS == null) {
                return null;
            }
            try {
                return new NativeAccessor(
                        eventClass.getMethod("kickMessage"),
                        eventClass.getMethod("kickMessage", NATIVE_COMPONENT_CLASS)
                );
            } catch (final NoSuchMethodException e) {
                return null;
            }
        }

        String read(final Event event) throws ReflectiveOperationException {
            final Component component = AdventureUtil.toComponent(this.getter.invoke(event));
            return component == null ? null : IMCSerializer.toMiniMessage(component);
        }

        void write(final Event event, final Component component) throws ReflectiveOperationException {
            final Object nativeComponent = AdventureUtil.fromComponent(component);
            if (nativeComponent == null) {
                throw new IllegalStateException("Could not convert component to the server's Adventure component");
            }
            this.setter.invoke(event, nativeComponent);
        }
    }

}
