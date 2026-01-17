package to.itsme.itsmyconfig.placeholder;

import java.util.Locale;

/**
 * Represents the types of a placeholder.
 */
public enum PlaceholderType {

    /**
     * Represents the type of a placeholder.
     */
    STRING,
    /**
     * Represents the color type of a placeholder.
     */
    COLOR,
    MATH,
    /**
     * Represents a placeholder type for getting a value of a list using the (index + 1).
     */
    LIST,
    /**
     * Represents a placeholder type for getting a value from a map using a key.
     */
    MAP,
    RANGE,
    /**
     * Represents a placeholder type for getting random values out of a list.
     */
    RANDOM,
    /**
     * Represents the type of an animation placeholder.
     */
    ANIMATION,
    PROGRESS_BAR,
    COLORED_TEXT;

    /**
     * Finds the PlaceholderType for the given type.
     *
     * @param type The string value representing the type of placeholder.
     * @return The PlaceholderType for the given type, or STRING if the type is not found or an exception occurs.
     */
    public static PlaceholderType find(final String type) {
        try {
            return PlaceholderType.valueOf(type.toUpperCase(Locale.ENGLISH));
        } catch (final Exception exception) {
            return STRING;
        }
    }

}
