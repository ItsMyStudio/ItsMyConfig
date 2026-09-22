package to.itsme.itsmyconfig.command;

import dev.velix.imperat.BukkitImperat;
import dev.velix.imperat.exception.PermissionDeniedException;
import dev.velix.imperat.resolvers.SuggestionResolver;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.SeriaPrefix;
import to.itsme.itsmyconfig.command.impl.SeriaPrefixCommand;
import to.itsme.itsmyconfig.command.parameter.PlaceholderParameter;
import to.itsme.itsmyconfig.command.parameter.SelectorParameter;
import to.itsme.itsmyconfig.command.util.PlayerSelector;
import to.itsme.itsmyconfig.message.Message;
import to.itsme.itsmyconfig.placeholder.Placeholder;


public final class CommandManager {

    private final ItsMyConfig plugin;
    private final BukkitImperat handler;

    public CommandManager(final ItsMyConfig plugin) {
        this.plugin = plugin;
        this.handler = BukkitImperat.builder(plugin)
                .parameterType(PlayerSelector.class, new SelectorParameter())
                .parameterType(Placeholder.class, new PlaceholderParameter(plugin))
                .throwableResolver(
                        PermissionDeniedException.class,
                        (exception, imperat, context) -> Message.NO_PERMISSION.send(context.source())
                )
                .namedSuggestionResolver("ModifiablePlaceholder", SuggestionResolver.plain(
                        plugin.getPlaceholderManager().getPlaceholdersMap().keySet().stream().filter(name -> {
                            final Placeholder data = plugin.getPlaceholderManager().get(name);
                            return data.getConfigurationSection().contains("value");
                        }).toList()
                ))
                .build();
        this.registerCommands();
    }

    public void registerCommands() {
        this.handler.registerCommands(new SeriaPrefixCommand(this.plugin));
    }

}
