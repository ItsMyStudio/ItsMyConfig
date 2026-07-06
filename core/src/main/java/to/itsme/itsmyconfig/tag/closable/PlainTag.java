package to.itsme.itsmyconfig.tag.closable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.tag.api.ClosableTag;
import to.itsme.itsmyconfig.util.Utilities;

public final class PlainTag extends ClosableTag {

    @Override
    public String name() {
        return "plain";
    }

    @Override
    public String process(final Player player, final String inner, final String[] arguments) {
        final Component parsed = Utilities.MM.deserialize(inner);
        return PlainTextComponentSerializer.plainText().serialize(parsed);
    }

}