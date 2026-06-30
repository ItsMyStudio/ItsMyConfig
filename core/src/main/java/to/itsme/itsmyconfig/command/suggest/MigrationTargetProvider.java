package to.itsme.itsmyconfig.command.suggest;

import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.providers.SuggestionProvider;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.command.impl.migration.FileHandlerRegistry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MigrationTargetProvider implements SuggestionProvider<BukkitCommandSource> {

    private final ItsMyConfig plugin = ItsMyConfig.getInstance();

    @Override
    public List<String> provide(final SuggestionContext<BukkitCommandSource> context, final Argument<BukkitCommandSource> argument) {
        final File pluginsDir = plugin.getDataFolder().getParentFile();
        final String input = context.getArgToComplete().value();

        final File base;
        final String prefix;

        final int lastSlash = input.lastIndexOf('/');
        if (lastSlash == -1) {
            base = pluginsDir;
            prefix = "";
        } else {
            base = new File(pluginsDir, input.substring(0, lastSlash));
            prefix = input.substring(0, lastSlash + 1);
        }

        final File[] children = base.listFiles();
        if (children == null) return List.of();

        final List<String> suggestions = new ArrayList<>();
        for (final File child : children) {
            final String name = child.getName();
            if (name.startsWith("itsmyconfig-backup-")) continue;

            if (child.isDirectory()) {
                suggestions.add(prefix + name + "/");
            } else if (FileHandlerRegistry.forFile(child) != null) {
                suggestions.add(prefix + name);
            }
        }

        return suggestions;
    }

}