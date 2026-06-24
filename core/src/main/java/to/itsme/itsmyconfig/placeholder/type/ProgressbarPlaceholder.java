package to.itsme.itsmyconfig.placeholder.type;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.placeholder.PlaceholderDependancy;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;

/**
 * ProgressBar class represents a progress bar with customizable colors and pattern.
 */
public final class ProgressbarPlaceholder extends Placeholder {
    /**
     * Represents the pattern used for rendering a progress bar.
     */
    private final String pattern, /**
     * The completedColor variable represents the color used to display the completed part of the progress bar.
     * <p>
     * It is a private instance variable of the ProgressBar class.
     * <p>
     * Example usage:
     * ProgressBar progressBar = new ProgressBar("key", "pattern", "completedColor", "progressColor", "remainingColor");
     * String color = progressBar.completedColor;
     */
    completedColor, /**
     * The progressColor variable holds the color used to represent the progress in a ProgressBar object.
     * <p>
     * Possible values can be any valid color string supported by the application.
     * This color will be used to render the portion of the progress bar that represents the completed progress.
     * <p>
     * The progressColor is set during the initialization of a ProgressBar object through the constructor.
     * It cannot be changed once the object is created.
     * <p>
     * This variable is used internally by the ProgressBar class in the calculation and rendering of the progress bar.
     *
     * @see ProgressbarPlaceholder
     * @see ProgressbarPlaceholder#render(double, double)
     * @see ProgressbarPlaceholder#buildProgressBar(int)
     */
    progressColor, /**
     * Represents the color used to display the remaining part of the progress bar.
     */
    remainingColor;

    /**
     * Represents a progress bar with customizable colors and pattern.
     */
    public ProgressbarPlaceholder(
            final String filePath,
            final ConfigurationSection section
    ) {
        super(section, filePath, PlaceholderType.PROGRESS_BAR, PlaceholderDependancy.NONE);
        this.pattern = section.getString("value");
        this.completedColor =  section.getString("completed-color");
        this.progressColor = section.getString("progress-color");
        this.remainingColor = section.getString("remaining-color");
    }

    @Override
    public int minArgs() {
        return 2;
    }

    @Override
    public int maxArgs() {
        return 2;
    }

    /**
     * Renders a progress bar based on the given value and max.
     *
     * @param value The current value of the progress bar.
     * @param max The maximum value of the progress bar.
     * @return The rendered progress bar as a string.
     */
    public String render(
            final double value,
            final double max
    ) {
        return buildProgressBar(calculateCompleted(value, max));
    }

    /**
     * Calculates the number of completed elements based on a given value and maximum value.
     * The completed elements are calculated by dividing the value by the maximum value,
     * multiplying the result by the length of the pattern, and rounding it to the nearest integer.
     * The calculated value is then limited to the maximum length of the pattern.
     *
     * @param value the current value
     * @param max   the maximum value
     * @return the number of completed elements
     */
    private int calculateCompleted(final double value, final double max) {
        final double percent = value / max;
        int completed = (int) Math.round(percent * pattern.length());
        return Math.min(completed, pattern.length());
    }

    /**
     * Builds a progress bar based on the specified completion level.
     *
     * @param completed The level of completion, represented as an integer between 0 and the length of the pattern.
     * @return The progress bar as a string.
     */
    private String buildProgressBar(final int completed) {
        final StringBuilder stringBuilder = new StringBuilder();
        if (completed != 0) {
            stringBuilder.append(completedColor);
            stringBuilder.append(pattern, 0, completed);
        }
        if (completed != pattern.length()) {
            stringBuilder.append(progressColor);
            stringBuilder.append(pattern, completed, completed + 1);
            stringBuilder.append(remainingColor);
            stringBuilder.append(pattern, completed + 1, pattern.length());
        }
        return stringBuilder.toString();
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getResult(
            final OfflinePlayer player,
            final String[] args
    ) {
        if (args.length < 2) {
            return "Invalid args amount";
        }

        try {
            final double value = Double.parseDouble(args[0]);
            final double maxValue = Double.parseDouble(args[1]);
            return ChatColor.translateAlternateColorCodes('&', this.render(value, maxValue));
        } catch (final NumberFormatException ignored) {}
        return "";
    }

}
