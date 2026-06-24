package to.itsme.itsmyconfig.command.suggest;

import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.providers.SuggestionProvider;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.placeholder.Placeholder;

import java.util.List;

public class ModifiablePlaceholderProvider implements SuggestionProvider<BukkitCommandSource> {

    private final ItsMyConfig plugin = ItsMyConfig.getInstance();

    @Override
    public List<String> provide(SuggestionContext<BukkitCommandSource> context, Argument<BukkitCommandSource> argument) {
        return plugin.getPlaceholderManager().getPlaceholdersMap().keySet().stream().filter(name -> {
            final Placeholder data = plugin.getPlaceholderManager().get(name);
            return data.getConfigurationSection().contains("value");
        }).toList();
    }

}
