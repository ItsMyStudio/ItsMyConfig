package to.itsme.itsmyconfig.tag.closable;

import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.tag.api.ClosableTag;

public final class LowercaseTag extends ClosableTag {

    @Override
    public String name() {
        return "lowercase";
    }

    @Override
    public String process(final Player player, final String inner, final String[] arguments) {
        return inner.toLowerCase();
    }

}