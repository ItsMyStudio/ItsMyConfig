package to.itsme.itsmyconfig.placeholder;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.util.Strings;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The PlaceholderData class is an abstract class that represents the basic structure of a placeholder data object.
 * It provides methods for obtaining the placeholder type and generating the placeholder result.
 */
public abstract class Placeholder {

    /**
     * Represents the key of the placeholder.
     */
    protected final String key;
    /**
     * Represents a set of all argument numbers.
     */
    protected final Set<Integer> arguments = new HashSet<>();
    /**
     * Represents the config section of the placeholder.
     */
    private final ConfigurationSection section;
    /**
     * Represents the config section of the placeholder.
     */
    private final String filePath;
    /**
     * Represents the type of the placeholder.
     */
    private final PlaceholderType type;
    /**
     * Represents a list of dependancy arguments.
     */
    private final Set<PlaceholderDependancy> dependancies;

    /**
     * Represents a set of the compiled placeholders / variants.
     */
    protected Set<CompiledPlaceholder> compiledPlaceholders;

    /**
     * Represents a placeholder data object.
     */
    public Placeholder(
            final ConfigurationSection section,
            final String filePath,
            final PlaceholderType type,
            final PlaceholderDependancy... dependancies
    ) {
        this.type = type;
        this.section = section;
        this.filePath = filePath;
        this.key = section.getName();
        this.dependancies = Set.of(dependancies);
        this.compiledPlaceholders = Set.of(mainCompiledPlaceholder());
    }

    public int minArgs() {
        return 0;
    }

    public int maxArgs() {
        return -1;
    }

    public Set<CompiledPlaceholder> getCompiledPlaceholders() {
        return compiledPlaceholders;
    }

    protected CompiledPlaceholder mainCompiledPlaceholder() {
        return new CompiledPlaceholder(
                this.key,
                this,
                this::asString,
                this.minArgs(),
                this.maxArgs()
        );
    }

    protected CompiledPlaceholder compileVariant(
            final String variant,
            final PlaceholderCaller caller,
            final int minArguments,
            final int maxArguments
    ) {
        return new CompiledPlaceholder(
                this.key + "_" + variant,
                this,
                caller,
                minArguments,
                maxArguments
        );
    }

    /**
     * Converts the given Player and arguments to a formatted string.
     *
     * @param player The Player object.
     * @param args   The array of strings.
     * @return The formatted string.
     */
    public String asString(final OfflinePlayer player, final String[] args) {
        final String result;
        if (player != null && player.isOnline()) {
            result = PlaceholderAPI.setPlaceholders(player.getPlayer(), this.getResult(player.getPlayer(), args));
        } else {
            result = PlaceholderAPI.setPlaceholders(player, this.getResult(player, args));
        }

        return result;
    }

    protected String asVariantString(
            final OfflinePlayer player,
            String result
    ) {
        if (player != null && player.isOnline()) {
            result = PlaceholderAPI.setPlaceholders(player.getPlayer(), result);
        } else {
            result = PlaceholderAPI.setPlaceholders(player, result);
        }

        return result;
    }

    /**
     * This method is used to retrieve the result of a placeholder evaluation.
     *
     * @param args The arguments used for the placeholder evaluation.
     * @return The result of the placeholder evaluation as a string.
     */
    @SuppressWarnings("unused")
    public String getResult(final String[] args) {
        throw new RuntimeException("Placeholder " + this.type.name() + " does not accept empty requirements");
    }

    /**
     * This method is used to retrieve the result of a placeholder evaluation.
     *
     * @param args The arguments used for the placeholder evaluation.
     * @return The result of the placeholder evaluation as a string.
     */
    public String getResult(final Player player, final String[] args) {
        return this.getResult((OfflinePlayer) player, args);
    }

    /**
     * This method is used to retrieve the result of a placeholder evaluation.
     *
     * @param args The arguments used for the placeholder evaluation.
     * @return The result of the placeholder evaluation as a string.
     */
    public String getResult(final OfflinePlayer player, final String[] args) {
        throw new RuntimeException("Placeholder " + this.type.name() + " does not accept OfflinePlayer");
    }

    /**
     * Replaces arguments in a given message string.
     *
     * @param params  The array of parameters to use for replacement.
     * @param message The message string to replace arguments in.
     * @return The message string with replaced arguments.
     */
    public String replaceArguments(final String[] params, final String message) {
        return this.replaceArguments(params, message, 0);
    }

    /**
     * Replaces placeholders in a given message with the provided arguments.
     *
     * @param params  The array of parameters to replace the placeholders with.
     * @param message The message string containing the placeholders.
     * @return The updated message string with placeholders replaced by the corresponding parameters.
     */
    public String replaceArguments(
            final String[] params,
            final String message,
            final int skippedParams
    ) {
        if (this.arguments.isEmpty() || params.length == 0) {
            return message;
        }

        String output = message;
        for (final Integer argument : this.arguments) {
            final int index = argument + skippedParams;
            if (index >= params.length) continue;
            // Dollar signs are quoted before using replaceAll
            output = output.replaceAll(Pattern.quote("{" + argument + "}"), params[index].replace("$", "\\$"));
        }

        return output;
    }

    /**
     * Registers arguments for the PlaceholderData object.
     * This method adds the arguments obtained from the given string to the existing list of arguments.
     *
     * @param string The string containing the arguments to be registered.
     */
    protected void registerArguments(final String string) {
        this.arguments.addAll(Strings.getArguments(string));
    }

    /**
     * Retrieves a specific section from the YAML document.
     *
     * @return the {@link ConfigurationSection} object representing the specified section.
     */
    public ConfigurationSection getConfigurationSection() {
        return this.section;
    }

    /**
     * Retrieves the specific location of the YAML document.
     */
    public String getFilePath() {
        return this.filePath;
    }

    /**
     * Retrieves the type of the placeholder.
     *
     * @return The type of the placeholder.
     */
    public PlaceholderType getType() {
        return this.type;
    }

    public boolean hasDependency(final PlaceholderDependancy dependancy) {
        return this.dependancies.contains(dependancy);
    }

    /**
     * Reloads the placeholder data from its configuration section.
     *
     * @return true if the reload was successful, false otherwise.
     */
    public boolean reloadFromSection() {
        return false;
    }

}
