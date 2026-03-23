package to.itsme.itsmyconfig.requirement;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;
import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.requirement.type.NumberRequirement;
import to.itsme.itsmyconfig.requirement.type.RegexRequirement;
import to.itsme.itsmyconfig.requirement.type.StringRequirement;

import java.util.Collection;
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

    /**
     * Retrieves the deny message for a placeholder data object.
     *
     * @param placeholder    The PlaceholderData object.
     * @param player  The Player object.
     * @param params  The array of strings.
     * @return The deny message as a string, or null if there is no deny message.
     */
    public String getDenyMessage(
            final Placeholder placeholder,
            final @Nullable OfflinePlayer player,
            final String[] params
    ) {
        final String mode = placeholder.getRequirementMode();
        final Collection<RequirementData> requirements = placeholder.getRequirements();

        if (requirements.isEmpty()) return null;

        switch (mode.toLowerCase()) {
            case "or": {
                // OR mode: at least one requirement must pass. If ALL fail, return the last deny.
                String lastDeny = null;
                for (final RequirementData requirement : requirements) {
                    final String deny = processRequirementData(requirement, placeholder, player, params);
                    if (deny == null) {
                        return null; // at least one passed
                    }
                    lastDeny = deny;
                }
                return lastDeny; // all failed
            }
            case "xor": {
                // XOR mode: exactly one requirement must pass.
                int passCount = 0;
                String lastDeny = null;
                for (final RequirementData requirement : requirements) {
                    final String deny = processRequirementData(requirement, placeholder, player, params);
                    if (deny == null) {
                        passCount++;
                    } else {
                        lastDeny = deny;
                    }
                }
                if (passCount == 1) return null; // exactly one passed
                return lastDeny != null ? lastDeny : ""; // 0 or 2+ passed
            }
            case "and":
            default: {
                // AND mode (current behavior): all must pass. First failure returns deny.
                for (final RequirementData requirement : requirements) {
                    final String deny = processRequirementData(requirement, placeholder, player, params);
                    if (deny != null) {
                        return deny;
                    }
                }
                return null;
            }
        }
    }

    /**
     * Processes the requirement data and performs validation.
     *
     * @param requirementData The RequirementData object representing the requirement to be processed.
     * @param data The PlaceholderData object containing the data needed for processing.
     * @param player The Player object representing the player.
     * @param params The array of parameters to be used for substitution.
     * @return The deny message if the requirement is not met, or null if the requirement is met.
     */
    private String processRequirementData(
            final RequirementData requirementData,
            final Placeholder data,
            final @Nullable OfflinePlayer player,
            final String[] params
    ) {
        final Requirement<?> requirement = this.getRequirementByType(requirementData.identifier());

        if (requirement == null) {
            return null;
        }

        final String input = getParameters(player, data, requirementData.input(), params);
        final String output = getParameters(player, data, requirementData.output(), params);

        boolean passed = requirement.validate(requirementData.identifier(), input, output);
        if (requirementData.negate()) {
            passed = !passed;
        }
        if (passed) {
            return null;
        }

        return getParameters(player, data, requirementData.deny(), params);
    }

    /**
     * Retrieves the parameters for a placeholder evaluation by replacing arguments in a given message string.
     *
     * @param player     The OfflinePlayer object.
     * @param data       The PlaceholderData object.
     * @param parameter  The parameter to be replaced.
     * @param params     The array of parameters to use for replacement.
     * @return The message string with replaced arguments.
     */
    private String getParameters(
            final @Nullable OfflinePlayer player,
            final Placeholder data,
            final String parameter,
            final String[] params
    ) {
        final String replacedArgs = data.replaceArguments(params, parameter);
        if (player == null) {
            return replacedArgs;
        }

        return PlaceholderAPI.setPlaceholders(player, replacedArgs);
    }

}
