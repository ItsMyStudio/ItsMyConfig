package to.itsme.itsmyconfig.placeholder.type;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.placeholder.PlaceholderDependancy;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * YAML:
 * custom-placeholder:
 *   switch-type-placeholder:
 *     type: switch
 *     input: "%some_placeholder%"
 *     # Optional:
 *     # default: ""
 *     # ignorecase: true
 *     values:
 *       "value1": "result1"
 *       "value2": "result2"
 *
 * Usage:
 * %itsmyconfig_switch-type-placeholder%
 * or with args:
 * %itsmyconfig_switch-type-placeholder_arg0::arg1%
 */
public final class SwitchPlaceholder extends Placeholder {

    private final String input;
    private final Map<String, String> values;
    private final boolean ignoreCase;
    private final String defaultValue;

    public SwitchPlaceholder(
            final String filePath,
            final ConfigurationSection section
    ) {
        super(section, filePath, PlaceholderType.SWITCH, PlaceholderDependancy.PLAYER);
        this.input = section.getString("input", "");
        this.ignoreCase = section.getBoolean("ignorecase", false);
        this.defaultValue = section.getString("default", "");
        this.registerArguments(this.input);

        final ConfigurationSection valuesSection = section.getConfigurationSection("values");
        if (valuesSection == null) {
            this.values = Collections.emptyMap();
            return;
        }

        final Map<String, String> tmp = new HashMap<>();
        for (final String key : valuesSection.getKeys(false)) {
            final Object raw = valuesSection.get(key);
            final String value;
            if (raw instanceof java.util.List<?> list) {
                value = list.stream().map(String::valueOf).collect(Collectors.joining("\n"));
            } else {
                value = raw == null ? "" : String.valueOf(raw);
            }
            tmp.put(normalizeKey(key), value);
        }
        this.values = Collections.unmodifiableMap(tmp);
    }

    @Override
    public String getResult(
            final OfflinePlayer player,
            final String[] args
    ) {
        // Resolve input: replace {0}, {1}... then resolve PAPI placeholders
        String resolvedInput = this.replaceArguments(args, this.input);
        if (player != null) {
            resolvedInput = PlaceholderAPI.setPlaceholders(player, resolvedInput);
        }

        final String key = normalizeKey(resolvedInput);
        final String value = values.get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        if (args.length == 0) {
            return value;
        }
        return applyArgs(value, args);
    }

    private String normalizeKey(final String key) {
        if (key == null) return "";
        return ignoreCase ? key.toLowerCase(Locale.ROOT) : key;
    }

    private static String applyArgs(final String template, final String[] args) {
        String out = template;
        for (int i = 0; i < args.length; i++) {
            out = out.replace("{" + i + "}", args[i]);
        }
        return out;
    }
}
