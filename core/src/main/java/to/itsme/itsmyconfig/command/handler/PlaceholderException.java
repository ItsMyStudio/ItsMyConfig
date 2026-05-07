package to.itsme.itsmyconfig.command.handler;

import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.SelfHandlingException;
import to.itsme.itsmyconfig.message.AudienceResolver;
import to.itsme.itsmyconfig.util.Utilities;

public class PlaceholderException extends SelfHandlingException {

    private final String name;

    public PlaceholderException(final String name) {
        this.name = name;
    }

    @Override
    public <S extends CommandSource> void handle(CommandContext<S> context) {
        AudienceResolver.resolve(context.source()).sendMessage(
                Utilities.MM.deserialize(
                        "<red>Placeholder <yellow>" + name + "</yellow> was not found.</red>"
                )
        );
    }

}
