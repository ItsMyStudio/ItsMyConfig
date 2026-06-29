package to.itsme.itsmyconfig.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

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
    void testExtractArguments() {
        // --- Basic colon-separated parsing ---

        // Empty string
        assertArrayEquals(new String[]{}, ea(""));

        // Single argument, no colon
        assertArrayEquals(new String[]{"hello"}, ea("hello"));

        // Multiple colon-separated arguments
        assertArrayEquals(new String[]{"a", "b", "c"}, ea("a:b:c"));

        // Leading colon is skipped
        assertArrayEquals(new String[]{"a", "b"}, ea(":a:b"));

        // Leading colon only
        assertArrayEquals(new String[]{}, ea(":"));

        // --- Double-quoted values ---

        // Simple double-quoted arg
        assertArrayEquals(new String[]{"hello world"}, ea("\"hello world\""));

        // Double-quoted with leading colon
        assertArrayEquals(new String[]{"hello world"}, ea(":\"hello world\""));

        // Double-quoted containing colon
        assertArrayEquals(new String[]{"a:b"}, ea("\"a:b\""));

        // Double-quoted followed by unquoted arg
        assertArrayEquals(new String[]{"hello world", "foo"}, ea("\"hello world\":foo"));

        // Multiple double-quoted args
        assertArrayEquals(new String[]{"foo bar", "baz qux"}, ea("\"foo bar\":\"baz qux\""));

        // Empty double-quoted string
        assertArrayEquals(new String[]{""}, ea("\"\""));

        // --- Single-quoted values ---

        // Simple single-quoted arg
        assertArrayEquals(new String[]{"hello world"}, ea("'hello world'"));

        // Single-quoted containing colons
        assertArrayEquals(new String[]{"a:b:c"}, ea("'a:b:c'"));

        // Empty single-quoted string
        assertArrayEquals(new String[]{""}, ea("''"));

        // --- Backtick-quoted values ---

        // Simple backtick-quoted arg
        assertArrayEquals(new String[]{"hello world"}, ea("`hello world`"));

        // Backtick-quoted containing colon
        assertArrayEquals(new String[]{"a:b"}, ea("`a:b`"));

        // --- Mixed quoted and unquoted ---

        // Unquoted first, then double-quoted
        assertArrayEquals(new String[]{"plain", "quoted value"}, ea("plain:\"quoted value\""));

        // Double-quoted first, then unquoted
        assertArrayEquals(new String[]{"quoted value", "plain"}, ea("\"quoted value\":plain"));

        // All three quote types in one string
        assertArrayEquals(new String[]{"double", "single", "backtick"}, ea("\"double\":'single':`backtick`"));

        // --- Whitespace termination of unquoted args ---

        // Space terminates unquoted arg
        assertEquals("hello", ea("hello world")[0]);

        // Tab terminates unquoted arg
        assertEquals("hello", ea("hello\tworld")[0]);

        // --- Extra stop characters ---

        // Single extra stop terminates unquoted arg
        assertArrayEquals(new String[]{"hello"}, ea("hello|world", '|'));

        // Semicolon as stop character
        assertArrayEquals(new String[]{"hello"}, ea("hello;world", ';'));

        // Extra stop is ignored inside quoted args
        assertArrayEquals(new String[]{"hello|world"}, ea("\"hello|world\"", '|'));

        // Extra stop interacts correctly with colon separation
        assertArrayEquals(new String[]{"a", "b"}, ea("a:b|c", '|'));

        // No extra stops — normal colon separation
        assertArrayEquals(new String[]{"a", "b", "c"}, ea("a:b:c"));

        // --- Unterminated quoted strings ---

        // Unterminated double quote yields no arg
        assertArrayEquals(new String[]{}, ea("\"unterminated"));

        // Unterminated single quote yields no arg
        assertArrayEquals(new String[]{}, ea("'unterminated"));

        // Unterminated backtick yields no arg
        assertArrayEquals(new String[]{}, ea("`unterminated"));

        // Valid arg before unterminated quote is still returned
        assertArrayEquals(new String[]{"hello"}, ea("hello:\"unterminated"));

        // --- Trailing colon after quoted value ---

        // Colon between two quoted segments is consumed correctly
        assertArrayEquals(new String[]{"foo", "bar"}, ea("\"foo\":\"bar\""));

        // --- Return type contract ---

        // Result is never null
        assertNotNull(ea(""));
        assertNotNull(ea("a:b"));

        // Single arg without delimiter has length 1
        assertEquals(1, ea("only").length);
    }

    /** Delegates to the static method under test. Replace class name as needed. */
    private static String[] ea(String raw, char... extraStops) {
        return Strings.extractArguments(raw, extraStops);
    }

}
