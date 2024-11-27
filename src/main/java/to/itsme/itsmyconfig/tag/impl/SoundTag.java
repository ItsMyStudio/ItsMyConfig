package to.itsme.itsmyconfig.tag.impl;

import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.tag.api.ArgumentsTag;
import to.itsme.itsmyconfig.util.Strings;

public class SoundTag extends ArgumentsTag {

    @Override
    public String name() {
        return "sound";
    }

    @Override
    public int minArguments() {
        return 1;
    }

    @Override
    public int maxArguments() {
        return 3;
    }

    @Override
    public String process(
            final Player player,
            final String[] arguments
    ) {
        if (arguments.length == 1) {
            player.playSound(player.getLocation(), arguments[0], 1.0F, 1.0F);
        } else if (arguments.length == 3) {
            final float volume = Strings.floatOrDefault(arguments[1], 1.0F);
            final float pitch = Strings.floatOrDefault(arguments[2], 1.0F);
            player.playSound(player.getLocation(), arguments[0], volume, pitch);
        }
        return "";
    }

}
