package to.itsme.itsmyconfig.placeholder.type;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.placeholder.PlaceholderDependancy;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * YAML:
 * custom-placeholder:
 *   map-type-placeholder:
 *     type: map
 *     # Optional:
 *     # default: ""
 *     # ignorecase: true
 *     values:
 *       Key1: "some value"
 *       keY2:  "other value"
 *
 * Usage:
 * %itsmyconfig_map-type-placeholder_Key1%
 * or with args:
 * %itsmyconfig_map-type-placeholder_Key1::<playerName>%
 */
public final class MapPlaceholder extends Placeholder {

    private final Map<String, String> map;
    private final boolean ignoreCase;
    private final String defaultValue;

    public MapPlaceholder(
            final String filePath,
            final ConfigurationSection section
    ) {
        super(section, filePath, PlaceholderType.MAP, PlaceholderDependancy.NONE);

        this.ignoreCase = section.getBoolean("ignorecase", false);
        this.defaultValue = section.getString("default", "");

        final ConfigurationSection values = section.getConfigurationSection("values");
        if (values == null) {
            this.map = Collections.emptyMap();
            return;
        }

        // Pre-size for fewer rehashes (HashMap capacity ~ size/0.75 + 1)
        final int size = values.getKeys(false).size();
        final int capacity = (int) (size / 0.75f) + 1;

        final Map<String, String> tmp = new HashMap<>(Math.max(16, capacity));
        for (final String key : values.getKeys(false)) {
            final Object raw = values.get(key);
            final String value = raw == null ? "" : String.valueOf(raw);
            tmp.put(normalizeKey(key), value);
        }

        this.map = Collections.unmodifiableMap(tmp);
    }

    @Override
    public String getResult(
            final OfflinePlayer player,
            final String[] args
    ) {
        if (args.length == 0) {
            return defaultValue;
        }

        final String key = normalizeKey(args[0]);
        final String value = map.get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        // Optional: support {0},{1}... substitution from remaining args
        if (args.length == 1) {
            return value;
        }

        return applyArgs(value, args);
    }

    private String normalizeKey(final String key) {
        if (key == null) return "";
        return ignoreCase ? key.toLowerCase(java.util.Locale.ROOT) : key;
    }

    private static String applyArgs(final String template, final String[] args) {
        String out = template;
        // args[0] is the map-key; replacements start from args[1] -> {0}
        for (int i = 1; i < args.length; i++) {
            out = out.replace("{" + (i - 1) + "}", args[i]);
        }
        return out;
    }
}
