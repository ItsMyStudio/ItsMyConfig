package to.itsme.itsmyconfig.placeholder.type;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.placeholder.PlaceholderDependancy;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;
import to.itsme.itsmyconfig.util.Strings;

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

    private final String completedSymbol;
    private final String progressSymbol;
    private final String remainingSymbol;
    private final int length;

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
        this.completedSymbol = section.getString("completed-symbol", null);
        this.progressSymbol = section.getString("progress-symbol", null);
        this.remainingSymbol = section.getString("remaining-symbol", null);
        this.length = section.getInt("length", this.pattern != null ? this.pattern.length() : 10);
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
        return render(value, max, length, completedSymbol, progressSymbol, remainingSymbol, completedColor, progressColor, remainingColor);
    }

    /**
     * Renders a progress bar based on the given value, max, and overridable parameters.
     *
     * @param value           The current value of the progress bar.
     * @param max             The maximum value of the progress bar.
     * @param effectiveLength The total length of the bar.
     * @param effCompSym      The completed symbol (nullable).
     * @param effProgSym      The progress symbol (nullable).
     * @param effRemSym       The remaining symbol (nullable).
     * @param effCompColor    The completed color.
     * @param effProgColor    The progress color.
     * @param effRemColor     The remaining color.
     * @return The rendered progress bar as a string.
     */
    private String render(
            final double value,
            final double max,
            final int effectiveLength,
            final String effCompSym,
            final String effProgSym,
            final String effRemSym,
            final String effCompColor,
            final String effProgColor,
            final String effRemColor
    ) {
        return buildProgressBar(
                calculateCompleted(value, max, effectiveLength),
                effectiveLength, effCompSym, effProgSym, effRemSym,
                effCompColor, effProgColor, effRemColor
        );
    }

    /**
     * Calculates the number of completed elements based on a given value and maximum value.
     * The completed elements are calculated by dividing the value by the maximum value,
     * multiplying the result by the length of the pattern, and rounding it to the nearest integer.
     * The calculated value is then limited to the maximum length of the pattern.
     *
     * @param value the current value
     * @param max   the maximum value
     * @param total the total bar length
     * @return the number of completed elements
     */
    private int calculateCompleted(final double value, final double max, final int total) {
        final double percent = value / max;
        int completed = (int) Math.round(percent * total);
        return Math.min(completed, total);
    }

    /**
     * Builds a progress bar based on the specified completion level.
     *
     * @param completed    The level of completion, represented as an integer between 0 and total.
     * @param total        The total length of the bar.
     * @param effCompSym   The completed symbol (nullable, falls back to pattern-based mode).
     * @param effProgSym   The progress symbol (nullable).
     * @param effRemSym    The remaining symbol (nullable).
     * @param effCompColor The completed color.
     * @param effProgColor The progress color.
     * @param effRemColor  The remaining color.
     * @return The progress bar as a string.
     */
    private String buildProgressBar(
            final int completed,
            final int total,
            final String effCompSym,
            final String effProgSym,
            final String effRemSym,
            final String effCompColor,
            final String effProgColor,
            final String effRemColor
    ) {
        final StringBuilder stringBuilder = new StringBuilder();

        if (effCompSym != null || effProgSym != null || effRemSym != null) {
            // Symbol-based mode
            final String cChar = effCompSym != null ? effCompSym : "\u2588";
            final String pChar = effProgSym != null ? effProgSym : cChar;
            final String rChar = effRemSym != null ? effRemSym : "\u2591";

            if (completed > 0) {
                stringBuilder.append(effCompColor);
                stringBuilder.append(cChar.repeat(completed));
            }
            if (completed < total) {
                stringBuilder.append(effProgColor);
                stringBuilder.append(pChar);
                if (completed + 1 < total) {
                    stringBuilder.append(effRemColor);
                    stringBuilder.append(rChar.repeat(total - completed - 1));
                }
            }
        } else {
            // Pattern-based mode
            if (completed != 0) {
                stringBuilder.append(effCompColor);
                stringBuilder.append(pattern, 0, completed);
            }
            if (completed != pattern.length()) {
                stringBuilder.append(effProgColor);
                stringBuilder.append(pattern, completed, completed + 1);
                stringBuilder.append(effRemColor);
                stringBuilder.append(pattern, completed + 1, pattern.length());
            }
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

            // Optional overrides: length, completed-symbol, progress-symbol, remaining-symbol,
            //                     completed-color, progress-color, remaining-color
            final int effLength       = args.length > 2 && !args[2].isEmpty() ? Strings.intOrDefault(args[2], this.length) : this.length;
            final String effCompSym   = args.length > 3 && !args[3].isEmpty() ? args[3] : this.completedSymbol;
            final String effProgSym   = args.length > 4 && !args[4].isEmpty() ? args[4] : this.progressSymbol;
            final String effRemSym    = args.length > 5 && !args[5].isEmpty() ? args[5] : this.remainingSymbol;
            final String effCompColor = args.length > 6 && !args[6].isEmpty() ? args[6] : this.completedColor;
            final String effProgColor = args.length > 7 && !args[7].isEmpty() ? args[7] : this.progressColor;
            final String effRemColor  = args.length > 8 && !args[8].isEmpty() ? args[8] : this.remainingColor;

            return ChatColor.translateAlternateColorCodes('&', this.render(
                    value, maxValue, effLength, effCompSym, effProgSym, effRemSym,
                    effCompColor, effProgColor, effRemColor
            ));
        } catch (final NumberFormatException ignored) {}
        return "";
    }

}
