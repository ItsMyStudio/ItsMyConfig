package to.itsme.itsmyconfig.placeholder.type;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.placeholder.PlaceholderDependancy;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;
import to.itsme.itsmyconfig.util.Strings;

import java.util.List;

public final class ListPlaceholder extends Placeholder {

    private final List<String> list;
    private final String defaultValue;

    public ListPlaceholder(
            final String filePath,
            final ConfigurationSection section
    ) {
        super(section, filePath, PlaceholderType.LIST, PlaceholderDependancy.NONE);
        this.list = section.getStringList("values");
        this.defaultValue = section.getString("default", "");
    }

    @Override
    public String getResult(
            final OfflinePlayer player,
            final String[] args
    ) {
        if (args.length == 0) {
            return defaultValue;
        }

        final int line = Strings.intOrDefault(args[0], 1) - 1;
        if (line >= list.size() || line < 0) {
            return defaultValue;
        }

        final String value = list.get(line);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        if (args.length == 1) {
            return value;
        }

        return applyArgs(value, args);
    }

    private static String applyArgs(final String template, final String[] args) {
        String out = template;
        for (int i = 1; i < args.length; i++) {
            out = out.replace("{" + (i - 1) + "}", args[i]);
        }
        return out;
    }

}
