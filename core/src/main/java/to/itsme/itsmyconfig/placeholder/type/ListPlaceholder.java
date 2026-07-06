package to.itsme.itsmyconfig.placeholder.type;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import to.itsme.itsmyconfig.placeholder.CompiledPlaceholder;
import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.placeholder.PlaceholderDependancy;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;
import to.itsme.itsmyconfig.util.Strings;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ListPlaceholder extends Placeholder {

    private final List<String> list;

    public ListPlaceholder(
            final String filePath,
            final ConfigurationSection section
    ) {
        super(section, filePath, PlaceholderType.LIST, PlaceholderDependancy.NONE);
        this.list = section.getStringList("values");

        final Set<CompiledPlaceholder> placeholders = new LinkedHashSet<>();
        // no "main" placeholder
        for (int i = 1; i <= this.list.size(); i++) {
            final int index = i - 1;
            placeholders.add(this.compileVariant(
                    String.valueOf(i),
                    (player, args) -> this.asVariantString(player, list.get(index)),
                    0,
                    0
            ));
        }
        this.compiledPlaceholders = placeholders;
    }

    @Override
    public String getResult(
            final OfflinePlayer player,
            final String[] args
    ) {
        if (args.length == 0) {
            return "";
        }

        final int line = Strings.intOrDefault(args[0], 1) - 1;
        if (line >= list.size() || line < 0) {
            return "";
        }

        return list.get(line);
    }

}
