package to.itsme.itsmyconfig.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import to.itsme.itsmyconfig.util.Strings;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LoginKickListenerTest {

    @BeforeAll
    static void beforeAll() {
        Strings.setSymbolPrefix("$");
    }

    @Test
    void testPrefixDetection() {
        // Typical legacy ban messages, as set by moderation plugins
        assertEquals(
                Optional.of("§c<p:server-title>\n&7Reason: Cheating"),
                Strings.parsePrefixedMessage("§c$<p:server-title>\n§7Reason: Cheating")
        );
        // Paper exposes the message as a component, serialized to MiniMessage before parsing
        assertEquals(
                Optional.of("<red><p:server-title>"),
                Strings.parsePrefixedMessage("<red>$<p:server-title>")
        );

        // No prefix: the message must be left untouched
        assertEquals(Optional.empty(), Strings.parsePrefixedMessage("§cYou are banned!"));
        assertEquals(Optional.empty(), Strings.parsePrefixedMessage("You are not whitelisted on this server!"));
        assertEquals(Optional.empty(), Strings.parsePrefixedMessage("Price: $5"));
    }

    @Test
    void testFallback() {
        // Prefix is stripped, colors are kept
        assertEquals("§cBanned", LoginKickListener.fallback("§c$Banned"));
        assertEquals("§cBanned\n§7Reason: §fCheating", LoginKickListener.fallback("§c$Banned\n§7Reason: §fCheating"));
        assertEquals("§aWelcome", LoginKickListener.fallback("$&aWelcome"));
        assertEquals("Secret", LoginKickListener.fallback("{$}Secret"));

        // Unprefixed messages are returned as-is
        assertEquals("§cYou are banned!", LoginKickListener.fallback("§cYou are banned!"));
        assertEquals("&cRaw", LoginKickListener.fallback("&cRaw"));
        assertEquals("", LoginKickListener.fallback(""));
        assertNull(LoginKickListener.fallback(null));
    }

    @Test
    void testToLegacy() {
        assertEquals("§cBanned", LoginKickListener.toLegacy(Component.text("Banned", NamedTextColor.RED)));
        assertEquals(
                "§x§1§2§a§b§e§fBanned",
                LoginKickListener.toLegacy(Component.text("Banned", TextColor.color(0x12ABEF)))
        );
    }

}
