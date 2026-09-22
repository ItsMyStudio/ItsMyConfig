package to.itsme.itsmyconfig.tag.api;

import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.ItsMyConfig;

public abstract class ArgumentsTag implements Tag {

    protected final ItsMyConfig plugin = ItsMyConfig.getInstance();

    public abstract int minArguments();

    public abstract int maxArguments();

    public abstract String process(final Player player, final String[] arguments);

}
