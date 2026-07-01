package to.itsme.itsmyconfig.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import to.itsme.itsmyconfig.placeholder.PlaceholderManager;
import to.itsme.itsmyconfig.placeholder.type.ListPlaceholder;
import to.itsme.itsmyconfig.placeholder.type.StringPlaceholder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StringMigratorTest {

    private static PlaceholderManager manager;

    @BeforeAll
    static void beforeAll() {
        manager = new PlaceholderManager(null);

        var config = new YamlConfiguration();
        var section = config.createSection("mylist");
        section.set("type", "list");
        section.set("values", List.of("a", "b", "c"));
        manager.register("mylist", new ListPlaceholder("test.yml", section));

        var config2 = new YamlConfiguration();
        var section2 = config2.createSection("with_underscore");
        section2.set("type", "list");
        section2.set("values", List.of("x", "y", "z"));
        manager.register("with_underscore", new ListPlaceholder("test.yml", section2));

        registerNonList("greet");
        registerNonList("msg");
        registerNonList("score");
        registerNonList("key");
        registerNonList("test");
        registerNonList("example");
        registerNonList("myplaceholder");
    }

    private static void registerNonList(final String name) {
        final var config = new YamlConfiguration();
        final var section = config.createSection(name);
        section.set("value", name);
        manager.register(name, new StringPlaceholder("test.yml", section));
    }

    private static StringMigrator migrator() {
        return new StringMigrator(manager);
    }

    void check(final String expected, final String input) {
        final String out = migrator().migrate(input);
        assertEquals(expected, out, "Failed to migrate: " + input);
        assertEquals(expected, migrator().migrate(out), "Failed to double-migrate: " + input);
    }

    @Test
    void nullReturnsNull() {
        assertNull(migrator().migrate(null));
    }

    @Test
    void emptyString() {
        check("", "");
    }

    @Test
    void noPlaceholdersUnchanged() {
        check("hello world", "hello world");
    }

    @Test
    void papiNoArgsUnchanged() {
        check("%imc_myplaceholder%", "%imc_myplaceholder%");
        check("%itsmyconfig_myplaceholder%", "%itsmyconfig_myplaceholder%");
    }

    @Test
    void papiOldStyleNonListFirstArgAfterUnderscore() {
        check("%imc_greet:hello:world%", "%imc_greet_hello::world%");
    }

    @Test
    void papiOldStyleNonListThreeArgs() {
        check("%imc_greet:hello:world:foo%", "%imc_greet_hello::world::foo%");
    }

    @Test
    void papiOldStyleArgsWithSpaces() {
        check("%imc_msg:'hello world':'foo bar'%", "%imc_msg_hello world::foo bar%");
    }

    @Test
    void papiOldStyleListTypeAlreadyCorrect() {
        check("%imc_mylist_3%", "%imc_mylist_3%");
    }

    @Test
    void papiOldStyleNumericFirstArgNonListDoesNotFold() {
        check("%imc_score:42%", "%imc_score_42%");
    }

    @Test
    void papiOldStyleItsmyconfig() {
        check("%itsmyconfig_greet:hello:world%", "%itsmyconfig_greet_hello::world%");
        check("%itsmyconfig_mylist_3%", "%itsmyconfig_mylist_3%");
    }

    @Test
    void papiKeyWithUnderscoreResolvesCorrectly() {
        check("%imc_with_underscore_3%", "%imc_with_underscore_3%");
        check("%imc_with_underscore:hello:world%", "%imc_with_underscore_hello::world%");
    }

    @Test
    void papiFirstArgEmptyIsNotList() {
        check("%imc_key:'':''%", "%imc_key_::%");
    }

    @Test
    void papiEmptyKeyNotMatched() {
        check("%%", "%%");
    }

    @Test
    void tagPNoArgsUnchanged() {
        check("<p:myplaceholder>", "<p:myplaceholder>");
    }

    @Test
    void tagPListTypeNumericFolds() {
        check("<p:mylist_3>", "<p:mylist:3>");
        check("<p:mylist_42>", "<p:mylist:42>");
    }

    @Test
    void tagPNonListNumericDoesNotFold() {
        check("<p:score:42>", "<p:score:42>");
    }

    @Test
    void tagPFirstArgNotNumericUnchanged() {
        check("<p:greet:hello>", "<p:greet:hello>");
    }

    @Test
    void tagPListTypeExtraArgsKept() {
        check("<p:mylist_3:extra:stuff>", "<p:mylist:3:extra:stuff>");
    }

    @Test
    void tagPQuotedFirstArg() {
        check("<p:mylist_3>", "<p:mylist:\"3\">");
        check("<p:mylist_3>", "<p:mylist:'3'>");
    }

    @Test
    void tagPListTypeWithNonNumericQuotedUnchanged() {
        check("<p:mylist:\"hello\">", "<p:mylist:\"hello\">");
    }

    @Test
    void mixedPlaceholders() {
        check(
                "prefix %imc_mylist_3% %imc_msg:'hello world':foo%",
                "prefix %imc_mylist_3% %imc_msg_hello world::foo%"
        );
    }

    @Test
    void tagPAndPapiTogether() {
        check(
                "<p:mylist_3> %imc_greet:hello%",
                "<p:mylist:3> %imc_greet_hello%"
        );
    }

    @Test
    void nonNumericFirstArgWithSpecialChars() {
        check("%imc_test:'hello:world':end%", "%imc_test_hello:world::end%");
    }

    @Test
    void papiPlaceholderFollowedByLiteralPercent() {
        check("%imc_greet:hello% gives 50% off", "%imc_greet_hello% gives 50% off");
    }

    @Test
    void papiFontSmallcapsMigrates() {
        check("%imc_smallcaps:hello_world%", "%imc_font_smallcaps_hello_world%");
    }

    @Test
    void papiFontShorthandSmallcapsMigrates() {
        check("%imc_smallcaps:test%", "%imc_f_smallcaps_test%");
    }

    @Test
    void papiFontLatinMigrates() {
        check("%imc_latin:42%", "%imc_font_latin_42%");
    }

    @Test
    void papiFontShorthandLatinMigrates() {
        check("%imc_latin:9%", "%imc_f_latin_9%");
    }

    @Test
    void papiFontItsmyconfigPrefix() {
        check("%itsmyconfig_smallcaps:hello_world%", "%itsmyconfig_font_smallcaps_hello_world%");
        check("%itsmyconfig_latin:42%", "%itsmyconfig_f_latin_42%");
    }

    @Test
    void papiFontAlreadyMigratedUnchanged() {
        check("%imc_smallcaps:hello_world%", "%imc_smallcaps:hello_world%");
        check("%imc_latin:42%", "%imc_latin:42%");
    }

    @Test
    void papiFontMixedWithOtherPlaceholders() {
        check(
                "%imc_smallcaps:hello% %imc_greet:hello%",
                "%imc_font_smallcaps_hello% %imc_greet_hello%"
        );
    }

}
