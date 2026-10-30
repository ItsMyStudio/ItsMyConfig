package to.itsme.itsmyconfig.processor.packetevents;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import to.itsme.itsmyconfig.processor.PacketProcessor;
import to.itsme.itsmyconfig.util.IMCSerializer;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

import java.nio.charset.StandardCharsets;
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
    /**
     * Cheap check on the raw packet bytes before decoding its text.
     * <p>
     * {@link #processComponent} only acts when the text contains the symbol prefix
     * (the incognito prefix contains it too) or is a "message not delivered" message.
     * Both show up as plain UTF-8 in the encoded packet, so when neither occurs the
     * component does not need to be decoded at all. Decoding the title is most of the
     * cost of the bossbar packets that plugins resend several times a second.
     *
     * @param event the intercepted packet
     * @return {@code false} if the packet can be left untouched without decoding it
     */
    public static boolean mayNeedProcessing(final PacketSendEvent event) {
        final String prefix = Strings.symbolPrefix;
        if (prefix == null || prefix.isEmpty()) {
            return true;
        }
        if (!prefix.equals(rawPrefix)) {
            rawPrefixBytes = escapeFree(prefix) ? prefix.getBytes(StandardCharsets.UTF_8) : null;
            rawPrefix = prefix;
        }
        final byte[] prefixBytes = rawPrefixBytes;
        if (prefixBytes == null) {
            return true;
        }
        final Object buf = event.getByteBuf();
        final int start = ByteBufHelper.readerIndex(buf);
        final int end = ByteBufHelper.writerIndex(buf);
        return contains(buf, start, end, prefixBytes) || contains(buf, start, end, FAIL_KEY);
    }

    private static final byte[] FAIL_KEY = "message_not_delivered".getBytes(StandardCharsets.UTF_8);
    private static volatile String rawPrefix;
    private static volatile byte[] rawPrefixBytes;

    // Characters a JSON encoder may escape, and characters whose encoded form is not plain UTF-8.
    private static boolean escapeFree(final String text) {
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (c < 0x20 || c == 0x7f || "\"\\<>&='".indexOf(c) >= 0 || Character.isSurrogate(c)) {
                return false;
            }
        }
        return true;
    }

    private static boolean contains(final Object buf, final int start, final int end, final byte[] needle) {
        final int first = needle[0] & 0xff;
        for (int i = start, last = end - needle.length; i <= last; i++) {
            if (ByteBufHelper.getUnsignedByte(buf, i) != first) {
                continue;
            }
            int j = 1;
            while (j < needle.length && ByteBufHelper.getUnsignedByte(buf, i + j) == (needle[j] & 0xff)) {
                j++;
            }
            if (j == needle.length) {
                return true;
            }
        }
        return false;
    }

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
