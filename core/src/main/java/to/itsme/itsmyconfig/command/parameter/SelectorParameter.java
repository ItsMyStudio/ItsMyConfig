package to.itsme.itsmyconfig.command.parameter;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.SimpleArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;
import to.itsme.itsmyconfig.command.handler.SelectorException;
import to.itsme.itsmyconfig.command.util.PlayerSelector;

import java.util.ArrayList;
import java.util.List;

public class SelectorParameter extends SimpleArgumentType<BukkitCommandSource, PlayerSelector> {

    @Override
    public PlayerSelector parse(
            @NotNull CommandContext<BukkitCommandSource> context,
            @NotNull Argument<BukkitCommandSource> argument,
            @NotNull String input
    ) throws CommandException {
        if ("all".equals(input)) {
            return PlayerSelector.all();
        }

        final Player player = Bukkit.getPlayer(input);
        if (player != null) {
            return PlayerSelector.of(player);
        }

        throw new SelectorException(input);
    }

    @Override
    public SuggestionProvider<BukkitCommandSource> getSuggestionProvider() {
        final List<String> names = new ArrayList<>(Bukkit.getOnlinePlayers().size());
        names.add("all");
        Bukkit.getOnlinePlayers().stream().map(Player::getName).forEach(names::add);
        return SuggestionProvider.staticSuggestions(names);
    }

}
