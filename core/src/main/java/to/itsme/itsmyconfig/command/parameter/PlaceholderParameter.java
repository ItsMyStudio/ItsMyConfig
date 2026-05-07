package to.itsme.itsmyconfig.command.parameter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.SimpleArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.command.handler.PlaceholderException;
import to.itsme.itsmyconfig.placeholder.Placeholder;

public class PlaceholderParameter extends SimpleArgumentType<BukkitCommandSource, Placeholder> {

    private final ItsMyConfig plugin;

    public PlaceholderParameter(final ItsMyConfig plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable Placeholder parse(
            @NotNull CommandContext<BukkitCommandSource> context,
            @NotNull Argument<BukkitCommandSource> argument,
            @NotNull String input
    ) throws CommandException {
        final Placeholder placeholder = plugin.getPlaceholderManager().get(input);
        if (placeholder != null) {
            return placeholder;
        }
        throw new PlaceholderException(input);
    }

    @Override
    public SuggestionProvider<BukkitCommandSource> getSuggestionProvider() {
        return SuggestionProvider.staticSuggestions(plugin.getPlaceholderManager().getPlaceholdersMap().keySet().toArray(new String[0]));
    }

}
