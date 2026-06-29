package to.itsme.itsmyconfig.tag.argument.title;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import to.itsme.itsmyconfig.message.AudienceResolver;
import to.itsme.itsmyconfig.tag.api.ArgumentsTag;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

public class SubtitleTag extends ArgumentsTag {

    @Override
    public String name() {
        return "subtitle";
    }

    @Override
    public int minArguments() {
        return 1;
    }

    @Override
    public int maxArguments() {
        return 4;
    }

    @Override
    public String process(
            final Player player,
            final String[] arguments
    ) {

        final Title title;
        if (arguments.length == 1) {
            title = Title.title(Component.empty(), Utilities.translate(arguments[0], player));
        } else if (arguments.length == 4) {
            final Title.Times times = this.createTimes(
                    Strings.intOrDefault(arguments[0], 10),
                    Strings.intOrDefault(arguments[1], 70),
                    Strings.intOrDefault(arguments[2], 20)
            );

            title = Title.title(Component.empty(), Utilities.translate(arguments[3], player), times);
        } else {
            title = null;
        }

        if (title != null) AudienceResolver.resolve(player).showTitle(title);

        return "";
    }

    @SuppressWarnings("all")
    private Title.Times createTimes(
            final @NotNull int fadeIn,
            final @NotNull int stay,
            final @NotNull int fadeOut
    ) {
        return Title.Times.times(
                Ticks.duration(fadeIn),
                Ticks.duration(stay),
                Ticks.duration(fadeOut)
        );
    }

}
