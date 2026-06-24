package to.itsme.itsmyconfig.requirement.type;

import to.itsme.itsmyconfig.requirement.Requirement;
import to.itsme.itsmyconfig.util.Strings;

import java.util.Arrays;

/**
 * The StringRequirement class is a final class that extends the Requirement class. It represents a requirement
 * for strings. It provides methods to add syntax rules for string comparison and validation of input and output.
 */
public final class StringRequirement extends Requirement<String> {

    /**
     * The StringRequirement class represents a requirement for strings. It provides methods to add syntax rules
     * for string comparison and validation of input and output.
     */
    public StringRequirement() {
        this.addSyntaxRule("equals", String::equals);
        this.addSyntaxRule("contains", String::contains);
    }

    /**
     * Validates the input and output strings based on the given identifier and syntax rules.
     *
     * @param identifier   The identifier for the syntax rule. It can include special modifiers like 'ignorecase' and 'ignorecolor'.
     * @param inputString  The input string to be validated.
     * @param outputString The output string to be validated.
     * @return true if the input and output strings pass the validation against the identifier and syntax rules, false otherwise.
     */
    @Override
    public boolean validate(
            String identifier,
            final String inputString,
            final String outputString
    ) {
        final String[] modifiedValues = handleSpecialIdentifiers(
                identifier, inputString, outputString
        );

        identifier = modifiedValues[0];
        final String input = modifiedValues[1];
        final String output = modifiedValues[2];

        final boolean reverse = identifier.startsWith("!");
        final String[] syntaxArguments = identifier.split(" ");

        return validateStrings(
                reverse,
                Arrays.copyOfRange(
                        identifier.split(" "),
                        1,
                        syntaxArguments.length
                ),
                input,
                output
        );
    }

    /**
     * Handle special identifiers and return the modified values.
     *
     * @param identifier   the special identifier to handle
     * @param inputString  the input string to modify
     * @param outputString the output string to modify
     * @return an array of strings containing the modified identifier, input, and output
     */
    // Handle special identifiers and return the modified values
    private String[] handleSpecialIdentifiers(
            String identifier,
            final String inputString,
            final String outputString
    ) {
        String input = inputString;
        String output = outputString;

        if (identifier.contains("ignorecase")) {
            identifier = identifier.replace("ignorecase", "");
            input = inputString.toLowerCase();
            output = outputString.toLowerCase();
        }

        if (identifier.contains("ignorecolor")) {
            identifier = identifier.replace("ignorecolor", "");
            input = Strings.colorless(inputString);
            output = Strings.colorless(outputString);
        }

        return new String[]{identifier, input, output};
    }

    /**
     * Validates input and output strings based on syntax arguments and the 'reverse' flag.
     * It checks if the input and output strings satisfy the given requirement.
     *
     * @param reverse         a boolean indicating whether to reverse the requirement
     * @param syntaxArguments an array of syntax arguments representing the requirements
     * @param input           a String representing the input value to be checked
     * @param output          a String representing the output value to be checked against
     * @return true if the input and output strings satisfy the requirements, false otherwise
     */
    // Validate input and output strings based on syntaxArguments and 'reverse' flag
    private boolean validateStrings(
            final boolean reverse,
            final String[] syntaxArguments,
            final String input,
            final String output
    ) {
        boolean result = true;

        for (String syntax : syntaxArguments) {
            result = reverse != this.isValid(syntax, input, output);
            if (!result) break;
        }

        return result;
    }

    /**
     * Returns an array of identifiers.
     *
     * @return An array of identifiers represented as strings.
     */
    @Override
    public String[] identifiers() {
        return new String[]{
                "string",
                "!string"
        };
    }

}
