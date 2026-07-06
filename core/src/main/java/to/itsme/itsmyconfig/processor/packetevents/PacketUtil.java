package to.itsme.itsmyconfig.processor.packetevents;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import to.itsme.itsmyconfig.processor.PacketProcessor;
import to.itsme.itsmyconfig.util.IMCSerializer;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

import java.util.Optional;

/**
 * Utility methods shared across PacketEvents-based packet processors.
 * <p>
 * Provides the common pipeline for extracting, parsing, translating and
 * re-serializing chat-like components from intercepted packets.
 */
public final class PacketUtil {

    private PacketUtil() {}

    /**
     * Prints a debug header for the given processor.
     * <p>
     * Intended to be called as the first statement inside
     * {@code process()}.
     *
     * @param processor the processor emitting the debug output
     */
    public static void startDebug(final PacketProcessor processor) {
        Utilities.debug(() -> "################# %s PACKET #################".formatted(processor.name()));
    }

    /**
     * Processes an intercepted {@link Component} through the common
     * translation pipeline.
     * <p>
     * The pipeline performs, in order:
     * <ol>
     *   <li>MiniMessage serialisation of the input component</li>
     *   <li>Null / empty guard</li>
     *   <li>Undeliverable-message prefix guard (cancellation signal)</li>
     *   <li>Symbol-prefix scan ({@link Strings#parsePrefixedMessage})</li>
     *   <li>Placeholder / tag translation via {@link Utilities#translate}</li>
     *   <li>Empty-result guard (cancellation signal)</li>
     * </ol>
     * <p>
     * All intermediate steps produce debug output through {@link Utilities#debug}.
     *
     * @param player    the player who will receive the packet
     * @param component the component extracted from the raw packet
     * @return {@code null} if the packet should be left untouched,
     *         {@link Component#empty()} if the packet should be canceled,
     *         or the translated component that should replace the original
     */
    @Nullable
    public static Component processComponent(final Player player, final Component component) {
        final String message = IMCSerializer.toMiniMessage(component);

        if (message == null || message.isEmpty()) {
            Utilities.debug(() -> "Text is null or empty\n" + Strings.DEBUG_HYPHEN);
            return null;
        }

        if (message.startsWith(Strings.FAIL_MESSAGE_PREFIX)) {
            Utilities.debug(() -> "Message send failure message, cancelling...");
            return Component.empty();
        }

        Utilities.debug(() -> "Found text: " + message);

        final Optional<String> parsed = Strings.parsePrefixedMessage(message);
        if (parsed.isEmpty()) {
            Utilities.debug(() -> "Text doesn't start w/ the symbol-prefix: " + message + "\n" + Strings.DEBUG_HYPHEN);
            return null;
        }

        final Component translated = Utilities.translate(parsed.get(), player);
        if (translated.equals(Component.empty())) {
            Utilities.debug(() -> "Component is empty, cancelling...\n" + Strings.DEBUG_HYPHEN);
            return Component.empty();
        }

        Utilities.debug(() -> "Final Result: " + IMCSerializer.toMiniMessage(translated) + "\n" + "Overriding...");
        return translated;
    }

}
