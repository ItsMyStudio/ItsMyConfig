package to.itsme.itsmyconfig.requirement.type;

import to.itsme.itsmyconfig.requirement.Requirement;

import java.util.Objects;

public final class NumberRequirement extends Requirement<Double> {

    private static final String EQUAL_IDENTIFIER = "==";
    private static final String GREATER_IDENTIFIER = ">";
    private static final String LESSER_IDENTIFIER = "<";
    private static final String GREATER_OR_EQUAL_IDENTIFIER = ">=";
    private static final String LESSER_OR_EQUAL_IDENTIFIER = "<=";
    private static final String NOT_EQUAL_IDENTIFIER = "!=";

    private final String[] IDENTIFIERS = new String[]{
            EQUAL_IDENTIFIER,
            GREATER_IDENTIFIER,
            LESSER_IDENTIFIER,
            GREATER_OR_EQUAL_IDENTIFIER,
            LESSER_OR_EQUAL_IDENTIFIER,
            NOT_EQUAL_IDENTIFIER
    };

    /**
     * Represents a requirement for number values. It supports comparison operators such as equals, greater than,
     * lesser than, greater than or equal to, lesser than or equal to, and not equal to.
     */
    public NumberRequirement() {
        this.addSyntaxRule(EQUAL_IDENTIFIER, Objects::equals);
        this.addSyntaxRule(GREATER_IDENTIFIER, (input, output) -> input > output);
        this.addSyntaxRule(LESSER_IDENTIFIER, (input, output) -> input < output);
        this.addSyntaxRule(GREATER_OR_EQUAL_IDENTIFIER, this.syntaxRules.get(GREATER_IDENTIFIER)[0], this.syntaxRules.get(EQUAL_IDENTIFIER)[0]);
        this.addSyntaxRule(LESSER_OR_EQUAL_IDENTIFIER, this.syntaxRules.get(LESSER_IDENTIFIER)[0], this.syntaxRules.get(EQUAL_IDENTIFIER)[0]);
        this.addSyntaxRule(NOT_EQUAL_IDENTIFIER, (input, output) -> !Objects.equals(input, output));
    }

    /**
     * Validates the input and output strings based on a given identifier.
     *
     * @param identifier   the identifier used to determine the type of validation
     * @param inputString  the input string to be validated
     * @param outputString the output string to be validated against
     * @return true if the validation is successful, false otherwise
     */
    @Override
    public boolean validate(
            final String identifier,
            final String inputString,
            final String outputString
    ) {
        final Double input = this.parseDouble(inputString);
        final Double output = this.parseDouble(outputString);
        return input != null && output != null && this.isValid(identifier, input, output);
    }

    /**
     * Returns an array of identifiers.
     *
     * @return An array of identifiers represented as strings.
     */
    @Override
    public String[] identifiers() {
        return IDENTIFIERS;
    }

    /**
     * Parses a string as a {@code Double}. Returns {@code null} (not 0.0) on failure
     * so callers can distinguish "invalid number" from "zero".
     *
     * @param value the string to parse
     * @return the parsed {@code Double}, or {@code null} if the string is not a valid double
     */
    private Double parseDouble(final String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

}
