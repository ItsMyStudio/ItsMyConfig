package to.itsme.itsmyconfig.requirement.type;

import to.itsme.itsmyconfig.requirement.Requirement;

import java.util.Objects;

public final class NumberRequirement extends Requirement<Double> {

    /**
     * The EQUAL_IDENTIFIER variable represents the syntax rule for the equality operator ("==").
     * It is a private static final String.
     */
    private static final String EQUAL_IDENTIFIER = "==";
    /**
     * The GREATER_IDENTIFIER variable represents the syntax rule ">" in the context of number requirements.
     * It is a private static final String initialized with the value ">".
     * <p>
     * The GREATER_IDENTIFIER is used in the NumberRequirement class to define a syntax rule for checking if an input number is greater than an output number.
     * It is added as a syntax rule to the syntaxRules map in the NumberRequirement class using the addSyntaxRule method.
     * <p>
     * Example usage:
     * NumberRequirement requirement = new NumberRequirement();
     * boolean isValid = requirement.validate(GREATER_IDENTIFIER, "10", "5");
     * // isValid will be true, as 10 is greater than 5
     *
     * @see NumberRequirement#addSyntaxRule
     */
    private static final String GREATER_IDENTIFIER = ">";
    /**
     * The LESSER_IDENTIFIER variable represents the syntax rule for the less than comparison operator ("<").
     * It is a private static final String and it is used in the NumberRequirement class to define the syntax rules for validating input and output values.
     */
    private static final String LESSER_IDENTIFIER = "<";
    /**
     *
     */
    private static final String GREATER_OR_EQUAL_IDENTIFIER = ">=";
    /**
     * The LESSER_OR_EQUAL_IDENTIFIER variable represents the syntax identifier for the "less than or equal" requirement.
     * It is a private static final String that contains the value "<=".
     * This identifier is used to check if a given input is less than or equal to a given output value.
     * It is part of the NumberRequirement class and is used in the syntaxRules map to define the requirement checkers.
     */
    private static final String LESSER_OR_EQUAL_IDENTIFIER = "<=";
    /**
     * The NOT_EQUAL_IDENTIFIER variable represents the syntax rule for the "not equal" comparison operator, which is "!=".
     * It is a private static final String variable.
     * <p>
     * The syntaxRules variable in the containing class NumberRequirement is a Map that stores the syntax rules for a Requirement object.
     * Each syntax rule consists of a String representing the syntax, and an array of RequirementChecker objects.
     * The NOT_EQUAL_IDENTIFIER is added to the syntaxRules map in the constructor of NumberRequirement.
     * <p>
     * The NOT_EQUAL_IDENTIFIER can be accessed by calling the identifiers() method of the NumberRequirement class, which returns an array of Strings representing the required identifiers
     * .
     * <p>
     * The NOT_EQUAL_IDENTIFIER is used in the validate() method of NumberRequirement to check if the given input and output satisfy the "not equal" requirement.
     * It is also used in the isValid() method to check if a given syntax, input and output combination satisfy the syntax rules.
     * <p>
     * The NOT_EQUAL_IDENTIFIER is a constant value and should not be modified.
     */
    private static final String NOT_EQUAL_IDENTIFIER = "!=";

    /**
     * The IDENTIFIERS variable is a private final array of Strings that contains the syntax identifiers for a NumberRequirement object.
     * The identifiers represent different types of number requirements, such as equal, greater than, lesser than, greater than or equal to,
     * lesser than or equal to, and not equal to.
     * <p>
     * The IDENTIFIERS array can be accessed by the NumberRequirement class and is used to validate if a given identifier matches any of the required identifiers.
     * <p>
     * The NumberRequirement class initializes the IDENTIFIERS array with the syntax identifiers in the constructor.
     * <p>
     * The identifiers() method returns the IDENTIFIERS array.
     * <p>
     * The validate() method is overridden from the Requirement class and takes an identifier, inputString, and outputString as parameters.
     * It transforms the inputString and outputString into Double values and checks if both values are not null.
     * If both values are not null, it checks if the identifier matches any of the required identifiers and checks if the input and output values satisfy
     * the specific requirement by calling the isValid() method.
     * If the requirement is satisfied, the method returns true. Otherwise, it returns false.
     * <p>
     * The transformString() method is a private helper method that takes a value as a String and tries to convert it into a Double value.
     * It first tries to convert the value using Double.parseDouble(). If it fails, it tries to convert the value to an Integer using Integer.parseInt().
     * If that fails, it tries to convert the value to a Float using Float.parseFloat().
     * If all conversion attempts fail, it returns 0.0.
     * <p>
     * The areBothValuesNotNull() method is a private helper method that takes two Double values, input and output, and checks if both values are not null.
     * If both values are not null, it returns true. Otherwise, it returns false.
     */
    private final String[] IDENTIFIERS = new String[]{
            EQUAL_IDENTIFIER,
            GREATER_IDENTIFIER,
            LESSER_IDENTIFIER,
            GREATER_OR_EQUAL_IDENTIFIER,
            LESSER_OR_EQUAL_IDENTIFIER,
            NOT_EQUAL_IDENTIFIER};

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
        final Double input = this.transformString(inputString);
        final Double output = this.transformString(outputString);
        return areBothValuesNotNull(input, output) && this.isValid(identifier, input, output);
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
     * Converts a string to a double value. If the string can be parsed as a double, it will be returned as is.
     * If the string cannot be parsed as a double, it will try to parse it as an integer. If that fails too, it will
     * try to parse it as a float. If all parsing attempts fail, 0.0 will be returned.
     *
     * @param value the string value to be converted
     * @return the converted double value or 0.0 if parsing fails
     */
    private Double transformString(final String value) {
        Double convertedValue = 0.0;
        try {
            convertedValue = Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
        }
        if (convertedValue.equals(0.0)) {
            try {
                convertedValue = (double) Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
            }
        }
        if (convertedValue.equals(0.0)) {
            try {
                convertedValue = (double) Float.parseFloat(value);
            } catch (NumberFormatException ignored) {
            }
        }
        return convertedValue;
    }

    /**
     * Checks if both input and output values are not null.
     *
     * @param input  the input value to check
     * @param output the output value to check
     * @return true if both input and output values are not null, false otherwise
     */
    private boolean areBothValuesNotNull(Double input, Double output) {
        return input != null && output != null;
    }
}
