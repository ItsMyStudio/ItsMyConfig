package to.itsme.itsmyconfig.tag.impl;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.message.AudienceResolver;
import to.itsme.itsmyconfig.tag.api.ArgumentsTag;
import to.itsme.itsmyconfig.util.Utilities;
import to.itsme.itsmyconfig.util.Versions;

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
    @SuppressWarnings("deprecation")
    public String process(
            final Player player,
            final String[] arguments
    ) {
        final String bar = arguments[0];
        final Component component = Utilities.translate(bar, player);

        if (Versions.isOver(1, 19, 0)) {
            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    Utilities.BUNGEE_SERIALIZER.serialize(component)
            );
        } else {
            AudienceResolver.resolve(player).sendActionBar(component);
        }

        return "";
    }

}
