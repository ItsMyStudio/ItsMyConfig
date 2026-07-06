package to.itsme.itsmyconfig.placeholder.type;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.placeholder.PlaceholderDependancy;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The RandomPlaceholderData class is a concrete implementation of the PlaceholderData class
 * that generates random placeholder data based on provided messages.
 */
public final class RandomPlaceholder extends Placeholder {

    /**
     * An instance of the Random class used for generating random numbers.
     */
    private final Random random = new Random();
    /**
     *
     */
    private final List<String> messages = new ArrayList<>();

    /**
     * Constructs a RandomPlaceholderData object with the given messages.
     */
    public RandomPlaceholder(
            final String filePath,
            final ConfigurationSection section
    ) {
        super(section, filePath, PlaceholderType.RANDOM, PlaceholderDependancy.NONE);
        final List<String> messages = section.getStringList("values");
        for (final String message : messages) {
            this.messages.add(message);
            this.registerArguments(message);
        }
    }

    /**
     * Generates a random entry from the messages list.
     *
     * @return The random entry as an AbstractMap.SimpleEntry object,
     * or null if the messages list is empty.
     */
    public String getRandomEntry() {
        if (messages.isEmpty()) {
            return null;
        }

        return messages.get(random.nextInt(messages.size()));
    }

    /**
     * Retrieves the result of placeholder evaluation.
     *
     * @param params The array of parameters used for the evaluation.
     * @return The result of the evaluation as a string.
     */
    @Override
    public String getResult(final OfflinePlayer player, final String[] params) {
        final String entry = this.getRandomEntry();
        return (entry == null) ? null : this.replaceArguments(params, entry);
    }

}
