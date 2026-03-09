package to.itsme.itsmyconfig.placeholder.type;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.placeholder.PlaceholderDependancy;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;

import java.util.List;

/**
 * The StringPlaceholderData class represents a placeholder data object for strings.
 * It extends the PlaceholderData class and provides methods for registering arguments,
 * obtaining the result of a placeholder evaluation, and replacing arguments in a given message string.
 */
public final class StringPlaceholder extends Placeholder {

    /**
     * The message string for the placeholder data.
     */
    private String message;

    /**
     * Represents a placeholder data object for strings.
     * It extends the PlaceholderData class and provides methods for registering arguments,
     * obtaining the result of a placeholder evaluation, and replacing arguments in a given message string.
     */
    public StringPlaceholder(
            final String filePath,
            final ConfigurationSection section
    ) {

        super(section, filePath, PlaceholderType.STRING, PlaceholderDependancy.NONE);

        final Object value = section.get("value");
        if (value instanceof List<?>) {
            this.message = String.join("\n", section.getStringList("value"));
        } else if (value instanceof String) {
            this.message = (String) value;
        } else {
            this.message = "";
        }

        this.registerArguments(this.message);
    }

    /**
     * Replaces arguments in a given message string.
     *
     * @param params     The array of parameters to use for replacement.
     * @return The message string with replaced arguments.
     */
    @Override
    public String getResult(final OfflinePlayer player, final String[] params) {
        return this.replaceArguments(params, this.message);
    }

    /**
     * Reloads the placeholder data from the configuration section.
     *
     * @return true if the reload was successful, false otherwise.
     */
    @Override
    public boolean reloadFromSection() {

        final Object value = this.getConfigurationSection().get("value");
        if (value instanceof List<?>) {
            this.message = String.join("\n", getConfigurationSection().getStringList("value"));
        } else if (value instanceof String) {
            this.message = (String) value;
        } else {
            this.message = "";
        }

        return true;
    }

}
