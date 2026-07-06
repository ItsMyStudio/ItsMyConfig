package to.itsme.itsmyconfig.hook;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.font.MappedFont;
import to.itsme.itsmyconfig.placeholder.CompiledPlaceholder;
import to.itsme.itsmyconfig.tag.TagManager;
import to.itsme.itsmyconfig.util.IMCSerializer;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

import java.util.List;

/**
 * DynamicPlaceHolder class is a PlaceholderExpansion that handles dynamic placeholders for the ItsMyConfig plugin.
 * It provides methods for handling various types of placeholders, such as fonts, progress bars, and custom placeholders.
 * This class extends the PlaceholderExpansion class.
 */
public final class PAPIHook extends PlaceholderExpansion {

    /**
     * ILLEGAL_NUMBER_FORMAT_MSG represents the error message when an illegal number format is encountered.
     */
    public static final String ILLEGAL_NUMBER_FORMAT_MSG = "Illegal Number Format";
    /**
     * ILLEGAL_ARGUMENT_MSG represents a string constant that indicates an illegal argument has been provided.
     * This constant is used in various methods in the DynamicPlaceHolder class.
     */
    public static final String ILLEGAL_ARGUMENT_MSG = "Illegal Argument";
    /**
     * PLACEHOLDER_NOT_FOUND_MSG is a constant variable that represents the message displayed when a placeholder is not found.
     */
    public static final String PLACEHOLDER_NOT_FOUND_MSG = "Placeholder not found";

    /** Prefix for {@code %imc_parse_<content>_<variant>} */
    private static final String PARSE_PREFIX = "parse_";
    /** Prefix for {@code %imc_smallcaps:<text>} */
    private static final String SMALLCAPS_PREFIX = "smallcaps";
    /** Prefix for {@code %imc_latin:<number>} */
    private static final String LATIN_PREFIX = "latin";

    /**
     * This variable is an instance of the ItsMyConfig class.
     */
    private final ItsMyConfig plugin;
    private final String identifier;

    /**
     * DynamicPlaceHolder is a class that represents a dynamic placeholder for a placeholder expansion.
     * It handles different types of placeholders and provides methods to handle font, progress, and custom placeholders.
     */
    public PAPIHook(final ItsMyConfig plugin, final String identifier) {
        this.plugin = plugin;
        this.identifier = identifier;
    }

    private static String parseFormatVariant(final String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        return switch (input.toLowerCase()) {
            case "legacy", "l" -> "legacy";
            case "console", "c" -> "console";
            case "mini", "m", "raw", "r" -> "mini";
            default -> null;
        };
    }

    /**
     * Returns the identifier for this object.
     *
     * @return the identifier for this object
     */
    @Override
    public @NotNull String getIdentifier() {
        return this.identifier;
    }

    /**
     * Retrieves the author(s) of the plugin.
     *
     * @return The author(s) of the plugin as a string. If there are multiple authors,
     * they are joined by commas.
     */
    @Override
    @SuppressWarnings("deprecation")
    public @NotNull String getAuthor() {
        return String.join(", ", this.plugin.getDescription().getAuthors());
    }

    /**
     * Retrieves the version of the plugin.
     *
     * @return The version of the plugin.
     */
    @Override
    @SuppressWarnings("deprecation")
    public @NotNull String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    /**
     * This method is used to persist data.
     *
     * @return true if the data is successfully persisted, false otherwise.
     */
    @Override
    public boolean persist() {
        return true;
    }

    /**
     * This method handles placeholder requests for the DynamicPlaceHolder expansion.
     * It replaces placeholders in the given params string with actual values and returns the result.
     *
     * @param player The player for whom the placeholder is being requested.
     * @param params The placeholder parameters string.
     * @return The result of the placeholder request.
     */
    @Override
    public @Nullable String onPlaceholderRequest(final Player player, @NotNull String params) {
        params = PlaceholderAPI.setPlaceholders(player, params);
        params = PlaceholderAPI.setBracketPlaceholders(player, params);
        if (Strings.startsWithIgnoreCase(params, PARSE_PREFIX)) {
            return handleParse(params.substring(PARSE_PREFIX.length()), player);
        }
        if (isPrefixedBy(params, SMALLCAPS_PREFIX)) {
            return handleSmallCaps(params);
        }
        if (isPrefixedBy(params, LATIN_PREFIX)) {
            return handleLatin(params);
        }
        return handlePlaceholder(params, player);
    }

    @Override
    public @NotNull List<String> getPlaceholders() {
        return this.plugin.getPlaceholderManager().getPapiPlaceholderKeys();
    }

    /**
     * Handles {@code smallcaps:<text>}.
     */
    private String handleSmallCaps(final String params) {
        final int colonIndex = params.indexOf(':');
        final String text = colonIndex == -1 ? "" : params.substring(colonIndex + 1);
        if (text.isEmpty()) {
            return ILLEGAL_ARGUMENT_MSG;
        }
        return MappedFont.SMALL_CAPS.apply(text.toLowerCase());
    }

    /**
     * Handles {@code latin:<number>}.
     */
    private String handleLatin(final String params) {
        final int colonIndex = params.indexOf(':');
        if (colonIndex == -1) {
            return ILLEGAL_ARGUMENT_MSG;
        }
        try {
            return Strings.integerToRoman(Integer.parseInt(params.substring(colonIndex + 1)));
        } catch (NumberFormatException e) {
            return ILLEGAL_NUMBER_FORMAT_MSG;
        }
    }

    private static boolean isPrefixedBy(final String params, final String prefix) {
        return Strings.startsWithIgnoreCase(params, prefix)
                && (params.length() == prefix.length() || params.charAt(prefix.length()) == ':');
    }

    /**
     * Handles the {@code imc_parse_} placeholder that processes text with tags and placeholders.
     *
     * @param content The content to parse (everything after the {@code parse_} prefix).
     * @param player  The player for whom the placeholder is being processed.
     * @return The parsed text with tags and placeholders processed.
     */
    private String handleParse(String content, final Player player) {
        if (content.isEmpty()) return ILLEGAL_ARGUMENT_MSG;

        // Check if there's a format specification at the end (e.g., _legacy, _mini, _console)
        String variant = "legacy";
        final String[] formatParts = content.split("_");
        if (formatParts.length > 0) {
            final String found = parseFormatVariant(formatParts[formatParts.length - 1]);
            if (found != null) {
                variant = found;
                content = content.substring(0, content.lastIndexOf("_" + formatParts[formatParts.length - 1]));
            }
        }

        try {
            final String processedContent = TagManager.process(player, content);
            final var component = Utilities.translate(processedContent, player);

            return switch (variant) {
                case "legacy", "console" -> Utilities.LEGACY_SERIALIZER.serialize(component);
                default -> IMCSerializer.toMiniMessage(component);
            };

        } catch (Exception e) {
            return "Parse Error: " + e.getMessage();
        }
    }

    /**
     * Handles the placeholder based on the params and player.
     *
     * @param params The array of split parameters.
     * @param player The player object.
     * @return The formatted string.
     */
    private String handlePlaceholder(final String params, final Player player) {
        final int colonIndex = params.indexOf(':');
        final String candidate = colonIndex == -1 ? params : params.substring(0, colonIndex);
        final CompiledPlaceholder compiled = plugin.getPlaceholderManager().getCompiled(candidate);

        if (compiled == null) {
            return PLACEHOLDER_NOT_FOUND_MSG;
        }

        if (colonIndex == -1) {
            if (!compiled.accepts(0)) {
                return compiled.invalidArgumentsMessage(0);
            }
            return compiled.caller().call(player);
        }

        final String[] args = Strings.extractArguments(params.substring(colonIndex + 1));
        if (!compiled.accepts(args.length)) {
            return compiled.invalidArgumentsMessage(args.length);
        }

        return compiled.caller().call(player, args);
    }

}
