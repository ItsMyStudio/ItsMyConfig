package to.itsme.itsmyconfig.command;

import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.BukkitImperat;
import studio.mevera.imperat.exception.PermissionDeniedException;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.command.impl.ItsMyConfigCommand;
import to.itsme.itsmyconfig.command.parameter.PlaceholderParameter;
import to.itsme.itsmyconfig.command.parameter.SelectorParameter;
import to.itsme.itsmyconfig.command.util.PlayerSelector;
import to.itsme.itsmyconfig.message.Message;
import to.itsme.itsmyconfig.placeholder.Placeholder;


public final class CommandManager {

    private final ItsMyConfig plugin;
    private final BukkitImperat<BukkitCommandSource> handler;

    public CommandManager(final ItsMyConfig plugin) {
        this.plugin = plugin;
        this.handler = BukkitImperat.builder(plugin)
                .argType(PlayerSelector.class, new SelectorParameter())
                .argType(Placeholder.class, new PlaceholderParameter(plugin))
                .exceptionHandler(
                        PermissionDeniedException.class,
                        (exception, context) -> Message.NO_PERMISSION.send(context.source())
                )
                .build();
        this.registerCommands();
    }

    public void registerCommands() {
        this.handler.registerCommands(new ItsMyConfigCommand(this.plugin));
    }

}
