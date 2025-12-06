package to.itsme.itsmyconfig.message;

import dev.velix.imperat.BukkitSource;
import dev.velix.imperat.context.Source;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

import java.util.List;

public enum Message {
    INVALID_USE("invalid-use"),
    RELOAD("reload"),
    NO_PERMISSION("no-permission"),
    MESSAGE_SENT("message-sent");

    private static final ItsMyConfig plugin = ItsMyConfig.getInstance();
    private final String path;

    Message(final String path) {
        this.path = path;
    }

    public void send(final Player player, final TagResolver... resolvers) {
        final Component component = Utilities.translate(this.toString(), player, resolvers);
        AudienceResolver.resolve(player).sendMessage(component);
    }

    public void send(final Source source, final TagResolver... replacers) {
        send((BukkitSource) source, replacers);
    }

    public void send(final BukkitSource source, final TagResolver... replacers) {
        if (source.isConsole()) {
            AudienceResolver.resolve(source).sendMessage(Utilities.translate(toString(), replacers));
        } else {
            send(source.asPlayer(), replacers);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public String toString() {
        final Object msg = plugin.getConfig().get("messages." + this.path);

        final String result;
        if (msg instanceof List<?>) {
            result = Strings.toString((List<String>) msg);
        } else if (msg instanceof String) {
            result = (String) msg;
        } else {
            result = "";
        }

        return result;
    }

}
