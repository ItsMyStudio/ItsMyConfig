package to.itsme.itsmyconfig.tag.impl;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.tag.api.ArgumentsTag;
import to.itsme.itsmyconfig.util.Scheduler;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

public class DelayTag extends ArgumentsTag {

    @Override
    public String name() {
        return "delay";
    }

    @Override
    public int minArguments() {
        return 2;
    }

    @Override
    public int maxArguments() {
        return 2;
    }

    @Override
    public String process(
            final Player player,
            final String[] arguments
    ) {
        final int delayInTicks = Strings.intOrDefault(arguments[0], 0);
        final String text = arguments[1];
        final Component component = Utilities.translate(text, player);
        if (!Component.empty().equals(component)) {
            Scheduler.runLaterAsync(() -> plugin.adventure().player(player).sendMessage(
                    component
            ), delayInTicks);
        }
        return "";
    }

}
