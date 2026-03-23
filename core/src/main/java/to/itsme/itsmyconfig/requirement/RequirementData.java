package to.itsme.itsmyconfig.requirement;

/**
 * The RequirementData class represents the data for a requirement.
 * Each RequirementData object contains an identifier, input, output, and deny message.
 *
 * @param identifier The identifier represents the unique identifier of a requirement.
 *                   <p>
 *                   This identifier is used to uniquely identify a requirement in the system.
 *                   <p>
 *                   Example Usage:
 *                   <pre>
 *                   RequirementData requirement = new RequirementData("R001", "Input", "Output", "Deny Message");
 *                   String identifier = requirement.getIdentifier();
 *                   </pre>
 * @param input      The input variable represents the input data for a requirement.
 *                   It is a private final String in the RequirementData class.
 *                   Each RequirementData object contains an identifier, input, output, and deny message.
 * @param output     The output variable represents the output of a RequirementData object.
 *                   It is a string value that contains the output data.
 *                   <p>
 *                   This variable is a private final field and is accessible only within the containing class.
 *                   It is initialized through the constructor of the RequirementData class.
 *                   <p>
 *                   Example usage:
 *                   RequirementData requirement = new RequirementData("REQ1", "input", "output", "deny");
 *                   System.out.println(requirement.getOutput()); // prints "output"
 * @param deny       The deny message for a requirement.
 */
public record RequirementData(String identifier, String input, String output, String deny, boolean negate) {}
