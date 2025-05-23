package to.itsme.itsmyconfig.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StringsTest {

    @BeforeAll
    static void beforeAll() {
        Strings.setSymbolPrefix("$");
    }

    @Test
    void testParsePrefixedMessage() {
        // Null and empty inputs
        assertEquals(Optional.empty(), Strings.parsePrefixedMessage(null));
        assertEquals(Optional.empty(), Strings.parsePrefixedMessage(""));

        // Simple prefix at start
        assertEquals(Optional.of("hello"), Strings.parsePrefixedMessage("$hello"));
        assertEquals(Optional.of(" hello"), Strings.parsePrefixedMessage("$ hello"));

        // Formatting codes before prefix
        assertEquals(Optional.of("&aGreen"), Strings.parsePrefixedMessage("&a$Green"));
        assertEquals(Optional.of("&a Green"), Strings.parsePrefixedMessage("&a$ Green"));
        assertEquals(Optional.of(" &aGreen"), Strings.parsePrefixedMessage("$ &aGreen"));
        assertEquals(Optional.empty(), Strings.parsePrefixedMessage("&aGreen")); // no prefix

        // MiniMessage tags
        assertEquals(Optional.empty(), Strings.parsePrefixedMessage("<bold>Bold"));
        assertEquals(Optional.of("<bold>Bold"), Strings.parsePrefixedMessage("$<bold>Bold"));
        assertEquals(Optional.of("<bold>Bold"), Strings.parsePrefixedMessage("<bold>$Bold")); // prefix inside tag

        // Whitespace handling
        assertEquals(Optional.of(" test"), Strings.parsePrefixedMessage("$ test"));
        assertEquals(Optional.of(" test"), Strings.parsePrefixedMessage(" $test")); // whitespace before $

        // Incognito prefix
        assertEquals(Optional.of("Secret"), Strings.parsePrefixedMessage("{$}Secret"));
        assertEquals(Optional.of("Secret Message"), Strings.parsePrefixedMessage("$Secret {$}Message"));

        // No prefix
        assertEquals(Optional.empty(), Strings.parsePrefixedMessage("No prefix here"));

        // Prefix after tag and/or whitespace
        assertEquals(Optional.of(" <bold>after"), Strings.parsePrefixedMessage(" <bold>$after"));
        assertEquals(Optional.of("  after"), Strings.parsePrefixedMessage("  $after"));
    }

    @Test
    void testEnglishify() {
        assertEquals("A la carte", Strings.englishify("À la carte"));
        assertEquals("Cafe", Strings.englishify("Café"));
        assertEquals("resume", Strings.englishify("résumé"));
    }

    @Test
    void testColorless() {
        assertEquals("Hello", Strings.colorless("&aHello"));
        assertEquals("Hello", Strings.colorless("§aHello"));
        assertEquals("Hello", Strings.colorless("H&kello"));
    }

    @Test
    void testIsNumber() {
        assertTrue(Strings.isNumber("123"));
        assertTrue(Strings.isNumber("123.45"));
        assertFalse(Strings.isNumber("abc"));
        assertFalse(Strings.isNumber(""));
    }

    @Test
    void testIntOrDefault() {
        assertEquals(42, Strings.intOrDefault("42", 0));
        assertEquals(0, Strings.intOrDefault("not a number", 0));
        assertEquals(0, Strings.intOrDefault("", 0));
    }

    @Test
    void testFloatOrDefault() {
        assertEquals(3.14f, Strings.floatOrDefault("3.14", 0f), 0.0001f);
        assertEquals(0f, Strings.floatOrDefault("abc", 0f), 0.0001f);
        assertEquals(0f, Strings.floatOrDefault("", 0f), 0.0001f);
    }

    @Test
    void testIntegerToRoman() {
        assertEquals("X", Strings.integerToRoman(10));
        assertEquals("IV", Strings.integerToRoman(4));
        assertEquals("MCMXCIV", Strings.integerToRoman(1994));
    }

    @Test
    void testGetArguments() {
        List<Integer> args = List.copyOf(Strings.getArguments("Value is {1} and {2}"));
        assertEquals(List.of(1, 2), args);
        assertTrue(Strings.getArguments("No args here").isEmpty());
        assertEquals(List.of(123), List.copyOf(Strings.getArguments("Test {123}")));
    }

    @Test
    void testQuoteEscapesTags() {
        // No <quote> tags, return as is
        assertEquals("plain text", Strings.quote("plain text"));

        // With <quote> tag, should escape tags inside
        String input = "<quote>This <bold>should</bold> be escaped</quote>";
        String output = Strings.quote(input);
        assertTrue(output.contains("\\<bold>should</bold>"));

        // With properties (ignorecolors), should skip color tags
        String input2 = "<quote:ignorecolors>This <red>should</red> be partly escaped</quote>";
        String output2 = Strings.quote(input2);
        assertTrue(output2.contains("<red>should</red>")); // color tag not escaped
    }

    @Test
    void testIsColor_and_IsDecoration() {
        assertTrue(Strings.isColor("red"));
        assertFalse(Strings.isColor("bold"));
        assertTrue(Strings.isDecoration("bold"));
        assertFalse(Strings.isDecoration("red"));
    }

    @Test
    void testTextlessExtractsNumbersAndDot() {
        assertEquals("123.4567", Strings.textless("123.45.67abc"));
        assertEquals("9.87", Strings.textless("abc9.8.7xyz"));
        assertEquals("", Strings.textless("no numbers"));
    }

    @Test
    void testMiniMessageSerialization() {
        Component component = Component.text("Welcome ")
            .append(
                Component.text("Adventurer <custom-tag>")
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.suggestCommand("/quest start"))
                    .hoverEvent(HoverEvent.showText(Component.text("Start your epic quest!")))
                    .insertion("Adventurer")
            )
            .append(Component.text("! "))
            .append(
                Component.text(" [Hint]")
                    .color(NamedTextColor.AQUA)
                    .decorate(TextDecoration.ITALIC)
                    .hoverEvent(HoverEvent.showText(Component.text("Try exploring the cave to the east.")))
            )
            .append(Component.text(" "))
            .append(
                Component.text("[Key]")
                    .color(NamedTextColor.GREEN)
                    .clickEvent(ClickEvent.runCommand("/use key"))
                    .hoverEvent(HoverEvent.showText(Component.text("Use your mysterious key")))
            )
            .append(Component.text(" (Mana: "))
            .append(
                Component.score("player123", "mana")
                    .color(NamedTextColor.AQUA)
            )
            .append(Component.text(") "))
            .append(
                Component.text("[Inventory]")
                    .color(NamedTextColor.GRAY)
                    .decorate(TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.openUrl("https://example.com/inventory"))
                    .hoverEvent(HoverEvent.showText(Component.text("View your inventory online")))
            );

        String mm = MMSerializer.serialize(component, null, false);

        System.out.println(mm);
        assertNotNull(mm);
        assertFalse(mm.contains("\\<custom-tag>"));
    }

    @Test
    public void testGsonToMiniMessageConversion() {
        String json = """
            {
              "text": "Test ",
              "extra": [
                {
                  "text": "Hello ",
                  "color": "gold",
                  "bold": true
                },
                {
                  "text": "world!",
                  "color": "aqua",
                  "italic": true
                }
              ]
            }
            """;

        GsonComponentSerializer gson = GsonComponentSerializer.gson();
        Component component = gson.deserialize(json);

        MiniMessage mini = MiniMessage.miniMessage();
        String miniMessage = mini.serialize(component);

        String expected = "Test <bold><gold>Hello </gold></bold><italic><aqua>world!</aqua></italic>";
        assertEquals(expected, miniMessage);
    }

}
