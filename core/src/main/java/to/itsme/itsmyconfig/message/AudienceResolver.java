package to.itsme.itsmyconfig.message;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.command.CommandSender;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.context.CommandSource;
import to.itsme.itsmyconfig.ItsMyConfig;

@SuppressWarnings("all")
public class AudienceResolver {

    private static final Resolver AUDIENCE_RESOLVER = new BukkitResolver();

    /**
     * Dummy method to load resolvers onEnable, and just logs it.
     */
    public static void load(final ItsMyConfig plugin) {
        plugin.getLogger().fine("Loaded " + AUDIENCE_RESOLVER.getClass().getSimpleName() + " successfully!");
    }

    public static Audience resolve(final CommandSender sender) {
        return AUDIENCE_RESOLVER.resolve(sender);
    }

    public static Audience resolve(final CommandSource source) {
        return AUDIENCE_RESOLVER.resolve((BukkitCommandSource) source);
    }

    public static Audience resolve(final BukkitCommandSource source) {
        return AUDIENCE_RESOLVER.resolve(source);
    }

    public static void send(final CommandSender sender, final ComponentLike component) {
        AUDIENCE_RESOLVER.resolve(sender).sendMessage(component);
    }

    public static void send(final BukkitCommandSource source, final ComponentLike component) {
        AUDIENCE_RESOLVER.resolve(source).sendMessage(component);
    }

    public static void close() {
        AUDIENCE_RESOLVER.close();
    }

    public sealed interface Resolver permits BukkitResolver, PaperResolver {

        Audience resolve(final CommandSender sender);

        default Audience resolve(final BukkitCommandSource source) {
            return resolve(source.origin());
        }

        default void close() {}

    }

    public static final class BukkitResolver implements Resolver {

        private final BukkitAudiences audiences;

        {
            audiences = BukkitAudiences.create(ItsMyConfig.getInstance());
        }

        @Override
        public Audience resolve(final CommandSender sender) {
            return audiences.sender(sender);
        }

        @Override
        public void close() {
            this.audiences.close();
        }

    }

    public static final class PaperResolver implements Resolver {

        @Override
        public Audience resolve(final CommandSender sender) {
            return (Audience) sender;
        }

    }

}
