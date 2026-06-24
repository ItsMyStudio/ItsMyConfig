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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DynamicPlaceHolder class is a PlaceholderExpansion that handles dynamic placeholders for the ItsMyConfig plugin.
 * It provides methods for handling various types of placeholders, such as fonts, progress bars, and custom placeholders.
 * This class extends the PlaceholderExpansion class.
 */
public final class PAPIHook extends PlaceholderExpansion {

    /**
     * This variable is an instance of the ItsMyConfig class.
     */
    private final ItsMyConfig plugin;
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
    private final String identifier;

    private static final Map<String, CachedResult> CACHE = new HashMap<>();

    private record CachedResult(String name, int length) {}

    public static void clearCache() {
        CACHE.clear();
    }

    /**
     * DynamicPlaceHolder is a class that represents a dynamic placeholder for a placeholder expansion.
     * It handles different types of placeholders and provides methods to handle font, progress, and custom placeholders.
     */
    public PAPIHook(final ItsMyConfig plugin, final String identifier) {
        this.plugin = plugin;
        this.identifier = identifier;
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
     *         they are joined by commas.
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
        params = PlaceholderAPI.setPlaceholders(player, params.replaceAll("\\$\\((.*?)\\)\\$", "%$1%"));
        params = PlaceholderAPI.setBracketPlaceholders(player, params);

        final String[] splitParams = params.split("_", -1);
        if (splitParams.length == 0) {
            return ILLEGAL_ARGUMENT_MSG;
        }

        final String firstParam = splitParams[0].toLowerCase();
        if ("parse".equals(firstParam)) {
            return handleParse(splitParams, player);
        }
        if (("font".equals(firstParam) || "f".equals(firstParam)) && splitParams.length >= 3) {
            return handleFont(splitParams);
        }
        return handlePlaceholder(splitParams, player);
    }

    /**
     * Handles font-related operations based on the given parameters.
     *
     * @param splitParams The array of parameters, where the font type is at index 1 and additional parameters are at subsequent indices.
     * @return The processed font or an error message if the font type is unknown or if an error occurs during font processing.
     */
    private String handleFont(final String[] splitParams) {
        String fontType = splitParams[1].toLowerCase();
        if ("latin".equals(fontType)) {
            try {
                int integer = Integer.parseInt(splitParams[2]);
                return Strings.integerToRoman(integer);
            } catch (NumberFormatException e) {
                return ILLEGAL_NUMBER_FORMAT_MSG;
            }
        } else if ("smallcaps".equals(fontType)) {
            final StringBuilder messageBuilder = new StringBuilder();
            for (int i = 2; i < splitParams.length; i++) {
                if (i > 2) messageBuilder.append("_");
                messageBuilder.append(splitParams[i]);
            }
            return MappedFont.SMALL_CAPS.apply(messageBuilder.toString().toLowerCase());
        }
        return "ERROR";
    }

    /**
     * Handles the imc_parse_ placeholder that processes text with tags and placeholders.
     * 
     * @param splitParams The array of parameters, where the content to parse starts at index 1.
     * @param player The player for whom the placeholder is being processed.
     * @return The parsed text with tags and placeholders processed.
     */
    private String handleParse(final String[] splitParams, final Player player) {
        if (splitParams.length < 2) {
            return ILLEGAL_ARGUMENT_MSG;
        }

        // Join all parameters after "parse" to reconstruct the content
        final StringBuilder contentBuilder = new StringBuilder();
        for (int i = 1; i < splitParams.length; i++) {
            if (i > 1) {
                contentBuilder.append("_");
            }
            contentBuilder.append(splitParams[i]);
        }
        
        String content = contentBuilder.toString();
        
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
                case "mini" -> IMCSerializer.toMiniMessage(component);
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
     * @param player      The player object.
     * @return The formatted string.
     */
    private String handlePlaceholder(final String[] params, final Player player) {
        final String joined = String.join("_", params);
        final CachedResult cached = CACHE.get(joined);

        int nameLength = 0;
        CompiledPlaceholder compiled = null;

        if (cached != null) {
            compiled = plugin.getPlaceholderManager().getCompiled(cached.name());
            if (compiled != null) {
                nameLength = cached.length();
            } else {
                CACHE.remove(joined);
            }
        }

        if (compiled == null) {
            for (int i = params.length; i > 0; i--) {
                final StringBuilder candidateBuilder = new StringBuilder(params[0]);
                for (int j = 1; j < i; j++) {
                    candidateBuilder.append("_").append(params[j]);
                }
                final String candidate = candidateBuilder.toString();
                compiled = plugin.getPlaceholderManager().getCompiled(candidate);
                if (compiled != null) {
                    nameLength = i;
                    CACHE.put(joined, new CachedResult(candidate, nameLength));
                    break;
                }
            }

            if (compiled == null) {
                return PLACEHOLDER_NOT_FOUND_MSG;
            }
        }

        final int remaining = params.length - nameLength;

        if (remaining == 0) {
            if (!compiled.accepts(0)) {
                return compiled.invalidArgumentsMessage(0);
            }
            return compiled.caller().call(player);
        }

        final StringBuilder builder = new StringBuilder(params[nameLength]);
        for (int i = nameLength + 1; i < params.length; i++) {
            builder.append("_");
            builder.append(params[i]);
        }

        final String candidate = builder.toString();
        final int colonIndex = candidate.indexOf(':');

        if (colonIndex == -1) {
            if (compiled.accepts(0)) {
                return compiled.caller().call(player);
            }
        }

        final String[] args = extractArguments(candidate);
        if (!compiled.accepts(args.length)) {
            return compiled.invalidArgumentsMessage(args.length);
        }

        return compiled.caller().call(player, args);
    }

    private static String[] extractArguments(final String raw) {
        final List<String> args = new ArrayList<>();

        int i = 0;
        while (i < raw.length()) {
            char delimiter = raw.charAt(i);

            int end;
            if (delimiter == '"' || delimiter == '\'' || delimiter == '`') {
                i++; // skip opening quote
                end = raw.indexOf(delimiter, i);
                if (end == -1) break;
                args.add(raw.substring(i, end));
                i = end + 1;
                // skip trailing ':' separator
                if (i < raw.length() && raw.charAt(i) == ':') i++;
            } else {
                end = i;
                while (end < raw.length()) {
                    char c = raw.charAt(end);
                    if (c == ':' || Character.isWhitespace(c)) break;
                    end++;
                }
                args.add(raw.substring(i, end));
                i = end + 1; // skip ':'
            }
        }

        return args.toArray(new String[0]);
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

}
