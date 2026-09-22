package to.itsme.itsmyconfig.requirement;

import to.itsme.itsmyconfig.requirement.type.NumberRequirement;
import to.itsme.itsmyconfig.requirement.type.RegexRequirement;
import to.itsme.itsmyconfig.requirement.type.StringRequirement;

import java.util.Set;

/**
 * This class is responsible for managing requirements and validating them.
 */
public final class RequirementManager {
    /**
     * The RequirementManager class is responsible for managing requirements and validating them.
     */
    private final Set<Requirement<?>> requirements = Set.of(
            new NumberRequirement(),
            new RegexRequirement(),
            new StringRequirement()
    );

    /**
     * Retrieves a Requirement object that matches the given type.
     *
     * @param type the type of the Requirement object to retrieve
     * @return the Requirement object that matches the given type, or null if no match is found
     */
    public Requirement<?> getRequirementByType(final String type) {
        for (final Requirement<?> requirement : this.requirements) {
            if (requirement.matchIdentifier(type)) {
                return requirement;
            }
        }
        return null;
    }

}
