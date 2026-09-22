package to.itsme.itsmyconfig.message;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.command.CommandSender;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.context.CommandSource;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.util.Versions;

public class AudienceResolver {

<<<<<<< HEAD
    private static Resolver AUDIENCE_RESOLVER;
=======
    @SuppressWarnings("ConstantConditions")
    private static final Resolver AUDIENCE_RESOLVER = Audience.class.isAssignableFrom(CommandSender.class)
            ? new PaperResolver() : new BukkitResolver(); // since we relocate Adventure, Bukkit always returns
>>>>>>> 39afa1f44463bd1462b2497b6820e2ee86437eb5

    /**
     * Loads resolvers onEnable, and logs it.
     */
    public static void load(final ItsMyConfig plugin) {
<<<<<<< HEAD
        if (AUDIENCE_RESOLVER == null) {
            AUDIENCE_RESOLVER = createResolver();
        }
        plugin.getLogger().fine("Loaded " + AUDIENCE_RESOLVER.getClass().getSimpleName() + " successfully!");
    }

    private static Resolver getResolver() {
        if (AUDIENCE_RESOLVER == null) {
            AUDIENCE_RESOLVER = createResolver();
        }
        return AUDIENCE_RESOLVER;
    }

    private static Resolver createResolver() {
        if (Versions.IS_PAPER) {
            return new PaperResolver();
        }
        return new BukkitResolver();
=======
        plugin.getLogger().fine("Loaded " + AUDIENCE_RESOLVER.getClass().getSimpleName() + " successfully!");
>>>>>>> 39afa1f44463bd1462b2497b6820e2ee86437eb5
    }

    public static Audience resolve(final CommandSender sender) {
        return getResolver().resolve(sender);
    }

<<<<<<< HEAD
    public static Audience resolve(final Source source) {
        return getResolver().resolve((BukkitSource) source);
    }

    public static Audience resolve(final BukkitSource source) {
        return getResolver().resolve(source);
=======
    public static Audience resolve(final CommandSource source) {
        return AUDIENCE_RESOLVER.resolve((BukkitCommandSource) source);
    }

    public static Audience resolve(final BukkitCommandSource source) {
        return AUDIENCE_RESOLVER.resolve(source);
>>>>>>> 39afa1f44463bd1462b2497b6820e2ee86437eb5
    }

    public static void send(final CommandSender sender, final ComponentLike component) {
        getResolver().resolve(sender).sendMessage(component);
    }

<<<<<<< HEAD
    public static void send(final BukkitSource source, final ComponentLike component) {
        getResolver().resolve(source).sendMessage(component);
=======
    public static void send(final BukkitCommandSource source, final ComponentLike component) {
        AUDIENCE_RESOLVER.resolve(source).sendMessage(component);
>>>>>>> 39afa1f44463bd1462b2497b6820e2ee86437eb5
    }

    public static void close() {
        if (AUDIENCE_RESOLVER != null) {
            AUDIENCE_RESOLVER.close();
            AUDIENCE_RESOLVER = null;
        }
    }

    public sealed interface Resolver permits BukkitResolver, PaperResolver {

        Audience resolve(final CommandSender sender);

        default Audience resolve(final BukkitCommandSource source) {
            return resolve(source.origin());
        }

        default void close() {}

    }

    public static final class BukkitResolver implements Resolver {

        private BukkitAudiences audiences;

        public BukkitResolver() {
            final ItsMyConfig plugin = ItsMyConfig.getInstance();
            if (plugin != null && plugin.isEnabled()) {
                this.audiences = BukkitAudiences.create(plugin);
            }
        }

        @Override
        public Audience resolve(final CommandSender sender) {
            if (this.audiences == null) {
                final ItsMyConfig plugin = ItsMyConfig.getInstance();
                if (plugin != null && plugin.isEnabled()) {
                    this.audiences = BukkitAudiences.create(plugin);
                }
            }
            return this.audiences != null ? this.audiences.sender(sender) : (sender instanceof Audience audience ? audience : Audience.empty());
        }

        @Override
        public void close() {
            if (this.audiences != null) {
                this.audiences.close();
                this.audiences = null;
            }
        }

    }

    public static final class PaperResolver implements Resolver {

        @Override
        public Audience resolve(final CommandSender sender) {
            return sender;
        }

    }

}
