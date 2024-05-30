package to.itsme.itsmyconfig.placeholder.type;

import net.kyori.adventure.text.format.*;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import to.itsme.itsmyconfig.placeholder.PlaceholderData;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;
import to.itsme.itsmyconfig.util.Strings;

import java.util.*;

/**
 * Represents a color placeholder data that can be used in placeholders.
 */
public final class ColorPlaceholderData extends PlaceholderData {

    /**
     * Represents the style of a variable.
     */
    private Style style;
    /**
     * Represents a legacy color value.
     */
    private final ChatColor legacyColor;
    /**
     * Represents a boolean value indicating whether the variable is a legacy value.
     */
    private final boolean legacy, /**
     *
     */
    invalid;
    /**
     * Represents a private final String variable in the ColorPlaceholderData class.
     */
    private final String value, /**
     * Represents a value for a name.
     *
     * @see ColorPlaceholderData
     * @see PlaceholderData
     */
    nameValue, /**
     * Represents a color placeholder data object.
     */
    hexValue;
    /***/
    private String properties = "", /**
     * Represents a placeholder data object with a mini prefix property.
     */
    propertiesMiniPrefix = "", /**
     *
     */
    propertiesMiniSuffix = "";

    /**
     * Represents a map of decoration properties.
     */
    private final static Map<String, String> DECORATIONS_PROPERTIES = new HashMap<String, String>() {
        {
            put("bold", "&l");
            put("italic", "&o");
            put("obfuscated", "&k");
            put("underlined", "&n");
            put("strikethrough", "&m");
        }
    };

    /**
     * Represents a color placeholder data object.
     */
    public ColorPlaceholderData(final ConfigurationSection properties) {
        super(PlaceholderType.COLOR);
        this.value = properties.getString("value", "").toLowerCase();

        final NamedTextColor namedTextColor = NamedTextColor.NAMES.value(this.value);
        if (namedTextColor != null) {
            this.legacy = true;
            this.invalid = false;
            this.nameValue = this.value;
            this.hexValue = namedTextColor.asHexString();
        } else if (Strings.HEX_PATTERN.matcher(this.value).matches()) {
            this.legacy = false;
            this.invalid = false;
            final TextColor textColor = TextColor.fromHexString(this.value);
            if (textColor != null) {
                this.hexValue = this.value;
                this.nameValue = NamedTextColor.nearestTo(textColor).toString();
            } else {
                this.nameValue = this.value;
                this.hexValue = "#ff0000";
            }
        } else {
            this.legacy = false;
            this.invalid = true;
            this.nameValue = "white";
            this.hexValue = "#ff0000";
        }

        this.legacyColor = ChatColor.valueOf(this.nameValue.toUpperCase());
        initializeStyle(properties);
    }

    /**
     * Initializes the style for the ColorPlaceholderData object.
     * Uses a ConfigurationSection to determine the style properties.
     *
     * @param configurationSection The ConfigurationSection containing the style properties.
     */
    private void initializeStyle(ConfigurationSection configurationSection) {
        final Style.Builder builder = Style.style().color(TextColor.fromHexString(hexValue));

        final StringBuilder propertiesBuilder = new StringBuilder();
        final StringBuilder propertiesPrefixBuilder = new StringBuilder();
        final StringBuilder propertiesSuffixBuilder = new StringBuilder();

        for (final String decorationType : DECORATIONS_PROPERTIES.keySet()) {
            if (configurationSection.getBoolean(decorationType)) {
                propertiesBuilder.append(DECORATIONS_PROPERTIES.get(decorationType));
                propertiesPrefixBuilder.append("<").append(decorationType).append(">");
                propertiesSuffixBuilder.append("</").append(decorationType).append(">");
                builder.decorate(TextDecoration.valueOf(decorationType.toUpperCase(Locale.ENGLISH)));
            }
        }

        this.properties = propertiesBuilder.toString();
        this.propertiesMiniPrefix = propertiesPrefixBuilder.toString();
        this.propertiesMiniSuffix = propertiesSuffixBuilder.toString();
        this.style = builder.build();
    }

    /**
     * This method is used to retrieve the result of a placeholder evaluation.
     *
     * @param params The arguments used for the placeholder evaluation.
     * @return The result of the placeholder evaluation as a string.
     */
    @Override
    public String getResult(final String[] params) {
        if (this.invalid) {
            return "";
        }

        if (params.length == 0) {
            return this.value + this.properties;
        }

        final String firstArg = params[0].toLowerCase(Locale.ROOT);
        switch (firstArg) {
            case "closestname":
                return this.nameValue;
            case "legacy":
                return (legacy ? legacyColor.toString() + this.properties : '&' + this.hexValue + this.properties).replace("§", "&");
            case "console":
                if (legacy) {
                    return legacyColor + this.properties.replaceAll("&", "§");
                }
                final String hexColor = this.hexValue.substring(1);
                final StringBuilder minecraftFormat = new StringBuilder("§x");
                for (int i = 0; i < hexColor.length(); i++) {
                    minecraftFormat.append("§").append(hexColor.charAt(i));
                }
                return minecraftFormat + this.properties.replaceAll("&", "§");
            case "mini":
                final String prefix = "<" + this.value + ">" + propertiesMiniPrefix;
                if (params.length > 1) {
                    final String suffix = "</" + this.value + ">" + propertiesMiniSuffix;
                    final StringBuilder result = new StringBuilder(prefix);
                    for (int i = 2; i < params.length; i++) {
                        result.append(params[i]);
                        if (i != params.length - 1) {
                            result.append(" ");
                        }
                    }
                    return result.append(suffix).toString();
                }
                return prefix;
        }
        return this.value;
    }

    /**
     * Retrieves the style associated with this instance.
     *
     * @return The style of this instance.
     */
    public Style getStyle() {
        return this.style;
    }

}
