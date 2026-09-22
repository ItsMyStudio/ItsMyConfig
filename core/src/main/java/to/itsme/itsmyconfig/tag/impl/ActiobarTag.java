package to.itsme.itsmyconfig.tag.impl;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.message.AudienceResolver;
import to.itsme.itsmyconfig.tag.api.ArgumentsTag;
import to.itsme.itsmyconfig.util.Utilities;

public class ActiobarTag extends ArgumentsTag {

    @Override
    public String name() {
        return "actionbar";
    }

    @Override
    public int minArguments() {
        return 1;
    }

    @Override
    public int maxArguments() {
        return 1;
    }

    @Override
    public String process(
            final Player player,
            final String[] arguments
    ) {
        final String bar = arguments[0];
        final Component component = Utilities.translate(bar, player);

        AudienceResolver.resolve(player).sendActionBar(component);

        return "";
    }

}
