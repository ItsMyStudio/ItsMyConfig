package to.itsme.itsmyconfig.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import to.itsme.itsmyconfig.placeholder.PlaceholderManager;
import to.itsme.itsmyconfig.placeholder.type.ListPlaceholder;

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
    void papiOldStyleListTypeDropsExtraArgs() {
        check("%imc_mylist_3%", "%imc_mylist_3::extra::more%");
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
    void placeholderInsideAnother() {
        check("%imc_test:%example%:example2%", "%imc_test_%example%::example2%");
    }

    @Test
    void papiPlaceholderFollowedByLiteralPercent() {
        check("%imc_greet:hello% gives 50% off", "%imc_greet_hello% gives 50% off");
    }

}
