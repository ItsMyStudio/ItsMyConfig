package to.itsme.itsmyconfig.requirement.type;

import to.itsme.itsmyconfig.requirement.Requirement;

import java.util.regex.Pattern;

/**
 * The RegexRequirement class is a concrete implementation of the Requirement class.
 * It represents a requirement that checks if a regular expression matches a given string input.
 */
public final class RegexRequirement extends Requirement<String> {

    /**
     * The REGEX_MATCHES_IDENTIFIER variable is a String that represents the identifier for a requirement that checks if a regular expression matches a given string input.
     * It is used in the RegexRequirement class, which is a concrete implementation of the Requirement class.
     * The addSyntaxRule() method in the Requirement class uses this identifier to add syntax rules for the specific requirement.
     * The validate() method uses this identifier to check if a given identifier matches the required identifier of the requirement.
     */
    private static final String REGEX_MATCHES_IDENTIFIER = "regex matches";

    /**
     * The RegexRequirement class represents a requirement that checks if a regular expression
     * matches a given string input.
     */
    public RegexRequirement() {
        this.addSyntaxRule(REGEX_MATCHES_IDENTIFIER, (input, output) -> Pattern.compile(output).matcher(input).matches());
    }

    /**
     * Validates if the given input and output satisfy the regular expression matches requirement.
     *
     * @param identifier the unique identifier of the requirement
     * @param input      the input data to be checked
     * @param output     the output data to be checked against
     * @return true if the input and output satisfy the requirement, false otherwise
     */
    @Override
    public boolean validate(final String identifier, final String input, final String output) {
        return this.isValid(REGEX_MATCHES_IDENTIFIER, input, output);
    }

    /**
     * Checks if the given identifier matches the required identifier.
     *
     * @param identifier The identifier to be checked.
     * @return true if the identifier matches the required identifier, false otherwise.
     */
    @Override
    public boolean matchIdentifier(final String identifier) {
        return identifier.equals(REGEX_MATCHES_IDENTIFIER);
    }

    /**
     * Returns an array of identifiers.
     *
     * @return An array of identifiers represented as strings.
     */
    @Override
    public String[] identifiers() {
        return new String[]{REGEX_MATCHES_IDENTIFIER};
    }
}
