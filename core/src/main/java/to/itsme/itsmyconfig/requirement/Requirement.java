package to.itsme.itsmyconfig.requirement;

import java.util.HashMap;
import java.util.Map;

/**
 * The Requirement class is an abstract class that represents a requirement. It provides utility methods to add syntax rules and check if an input and output satisfy the requirement
 * .
 *
 * @param <V> the type of the input and output
 */
public abstract class Requirement<V> {
    /**
     * The syntaxRules variable is a protected final Map that stores the syntax rules for a Requirement object.
     * The key of the map is a String representing the syntax, and the value is an array of RequirementChecker objects.
     * Each RequirementChecker is used to check if a given input and output satisfy a specific requirement.
     * <p>
     * The map is initialized with an empty HashMap.
     * <p>
     * The syntaxRules map can be accessed by subclasses of the Requirement class, allowing them to add and retrieve syntax rules.
     * <p>
     * The syntaxRules map can be modified by calling the addSyntaxRule() method, which takes a syntax String as the first parameter,
     * and an array of RequirementChecker objects as the second parameter. These syntax rules will be stored in the syntaxRules map
     * with the given syntax as the key.
     * <p>
     * The isValid() method can be used to check if a given syntax, input and output combination satisfy the syntax rules.
     * If the given syntax does not exist in the syntaxRules map, the method will return true.
     * Otherwise, it will iterate over the RequirementChecker objects associated with the given syntax.
     * If any of the RequirementCheckers return true when called with the given input and output, the method will return true.
     * Otherwise, it will return false.
     * <p>
     * The matchIdentifier() method checks if the given identifier matches any of the required identifiers specified by subclasses
     * of the Requirement class. It splits the input identifier by spaces and compares the first value with the required identifiers.
     * If a match is found, the method will return true.
     * Otherwise, it will return false.
     * <p>
     * The validate() method is an abstract method that needs to be implemented by subclasses of the Requirement class.
     * It takes an identifier, input and output as parameters and returns a boolean value indicating whether the identifier,
     * input and output satisfy the specific requirement.
     * <p>
     * The identifiers() method is an abstract method that needs to be implemented by subclasses of the Requirement class.
     * It returns an array of Strings representing the required identifiers.
     * <p>
     * The syntaxRules, patternMatchesIdentifier(), checkRequirement(), and check() declarations reflected
     * in the given code blocks were extracted from the containing class of the initial symbol, and are not directly related
     * to the syntaxRules variable.
     */
    protected final Map<String, RequirementChecker<V>[]> syntaxRules = new HashMap<>();

    /**
     * Adds a syntax rule to the {@code syntaxRules} map.
     *
     * @param syntax              the syntax rule to add
     * @param requirementCheckers the requirement checkers for the syntax rule
     */
    @SafeVarargs
    protected final void addSyntaxRule(final String syntax, final RequirementChecker<V>... requirementCheckers) {
        this.syntaxRules.put(syntax, requirementCheckers);
    }

    /**
     * Checks if the input and output satisfy the given requirement.
     *
     * @param syntax the syntax of the requirement
     * @param input  the input value to be checked
     * @param output the output value to be checked against
     * @return true if the requirement is satisfied, false otherwise
     */
    protected boolean isValid(final String syntax, final V input, final V output) {
        return checkRequirement(syntax, input, output);
    }

    /**
     * Checks if the given identifier matches the pattern.
     *
     * @param identifier the identifier to check
     * @return true if the identifier matches the pattern, false otherwise
     */
    public boolean matchIdentifier(final String identifier) {
        return patternMatchesIdentifier(identifier);
    }

    /**
     * Validates if the given input and output satisfy a specific requirement.
     *
     * @param identifier the unique identifier of the requirement
     * @param input      the input data
     * @param output     the output data
     * @return true if the input and output satisfy the requirement, false otherwise
     */
    public abstract boolean validate(final String identifier, final String input, final String output);

    /**
     * Returns an array of identifiers.
     *
     * @return An array of identifiers represented as strings.
     */
    public abstract String[] identifiers();

    /**
     * Checks if the first value of the given identifier matches any of the required identifiers.
     *
     * @param identifier The identifier to be checked.
     * @return true if the first value of the identifier matches any of the required identifiers, false otherwise.
     */
    private boolean patternMatchesIdentifier(final String identifier) {
        final String firstValueOfIdentifier = identifier.split(" ")[0];
        for (final String requiredIdentifier : this.identifiers()) {
            if (firstValueOfIdentifier.equals(requiredIdentifier)) return true;
        }
        return false;
    }

    /**
     * Checks if the given input and output satisfy a specific requirement according to the provided syntax.
     *
     * @param syntax the syntax of the requirement to check
     * @param input  the input value to check against the requirement
     * @param output the output value to check against the requirement
     * @return true if the input and output satisfy the requirement, false otherwise
     */
    private boolean checkRequirement(
            final String syntax,
            final V input,
            final V output
    ) {
        final RequirementChecker<V>[] checkers = this.syntaxRules.get(syntax);
        if (checkers == null) {
            return true;
        }
        for (final RequirementChecker<V> checker : checkers) {
            if (checker.check(input, output)) {
                return true;
            }
        }
        return false;
    }
}
