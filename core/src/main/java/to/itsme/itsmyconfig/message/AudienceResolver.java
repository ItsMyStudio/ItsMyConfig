package to.itsme.itsmyconfig.message;

import dev.velix.imperat.BukkitSource;
import dev.velix.imperat.context.Source;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.util.Utilities;
import to.itsme.itsmyconfig.util.Versions;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AudienceResolver {

    private static final Resolver AUDIENCE_RESOLVER = Versions.IS_PAPER ? new PaperResolver() : new LegacyResolver();

    /**
     * Dummy method to load resolvers onEnable, and just logs it.
     */
    @SuppressWarnings("all")
    public static void load(final ItsMyConfig plugin) {
        plugin.getLogger().fine("Loaded " + AUDIENCE_RESOLVER.getClass().getSimpleName() + " successfully!");
    }

    public static Audience resolve(final CommandSender sender) {
        return AUDIENCE_RESOLVER.resolve(sender);
    }

    public static Audience resolve(final Source source) {
        return AUDIENCE_RESOLVER.resolve((BukkitSource) source);
    }

    public static Audience resolve(final BukkitSource source) {
        return AUDIENCE_RESOLVER.resolve(source);
    }

    public static void send(final CommandSender sender, final ComponentLike component) {
        AUDIENCE_RESOLVER.resolve(sender).sendMessage(component);
    }

    public static void send(final BukkitSource source, final ComponentLike component) {
        AUDIENCE_RESOLVER.resolve(source).sendMessage(component);
    }

    public static void close() {
        AUDIENCE_RESOLVER.close();
    }

    public sealed interface Resolver permits LegacyResolver, PaperResolver {

        Audience resolve(final CommandSender sender);

        default Audience resolve(final BukkitSource source) {
            return resolve(source.origin());
        }

        default void close() {}

    }

    public static final class PaperResolver implements Resolver {

        private final LegacyResolver fallback = new LegacyResolver();

        @Override
        public Audience resolve(final CommandSender sender) {
            if (sender instanceof Audience audience) {
                return audience;
            }
            return fallback.resolve(sender);
        }

        @Override
        public void close() {
            fallback.close();
        }

    }

    public static final class LegacyResolver implements Resolver {

        private final Map<UUID, LegacyPlayerAudience> playerAudiences = new ConcurrentHashMap<>();
        private final LegacySenderAudience consoleAudience = new LegacySenderAudience(Bukkit.getConsoleSender());

        @Override
        public Audience resolve(final CommandSender sender) {
            if (sender instanceof Player player) {
                return playerAudiences.compute(player.getUniqueId(), (uuid, existing) -> {
                    if (existing == null) {
                        return new LegacyPlayerAudience(player);
                    }
                    existing.sender = player;
                    return existing;
                });
            }
            if (sender == Bukkit.getConsoleSender()) {
                return consoleAudience;
            }
            return new LegacySenderAudience(sender);
        }

        @Override
        public void close() {
            playerAudiences.values().forEach(LegacyPlayerAudience::close);
            playerAudiences.clear();
        }

    }

    private static class LegacySenderAudience implements Audience {

        protected CommandSender sender;

        private LegacySenderAudience(final CommandSender sender) {
            this.sender = sender;
        }

        @Override
        public void sendMessage(final Component component) {
            sender.sendMessage(Utilities.LEGACY_SERIALIZER.serialize(component));
        }

        @Override
        public void sendActionBar(final Component component) {
            sendMessage(component);
        }

        @Override
        public void showTitle(final Title title) {
            sendMessage(title.title());
            if (!Component.empty().equals(title.subtitle())) {
                sendMessage(title.subtitle());
            }
        }

    }

    private static final class LegacyPlayerAudience extends LegacySenderAudience {

        private final Map<BossBar, org.bukkit.boss.BossBar> activeBossBars = new ConcurrentHashMap<>();
        private final Map<BossBar, BossBar.Listener> listeners = new ConcurrentHashMap<>();

        private LegacyPlayerAudience(final Player sender) {
            super(sender);
        }

        @Override
        public void sendActionBar(final Component component) {
            this.player().spigot().sendMessage(ChatMessageType.ACTION_BAR, Utilities.toBungee(component));
        }

        @Override
        public void showTitle(final Title title) {
            final Title.Times times = title.times() == null ? Title.DEFAULT_TIMES : title.times();
            this.player().sendTitle(
                    Utilities.LEGACY_SERIALIZER.serialize(title.title()),
                    Utilities.LEGACY_SERIALIZER.serialize(title.subtitle()),
                    ticks(times.fadeIn()),
                    ticks(times.stay()),
                    ticks(times.fadeOut())
            );
        }

        @Override
        public void showBossBar(final BossBar bar) {
            final org.bukkit.boss.BossBar bukkitBar = activeBossBars.computeIfAbsent(bar, this::createBossBar);
            final Player player = player();
            if (!bukkitBar.getPlayers().contains(player)) {
                bukkitBar.addPlayer(player);
            }
            bukkitBar.setVisible(true);
        }

        @Override
        public void hideBossBar(final BossBar bar) {
            final org.bukkit.boss.BossBar bukkitBar = activeBossBars.remove(bar);
            if (bukkitBar == null) {
                return;
            }

            final BossBar.Listener listener = listeners.remove(bar);
            if (listener != null) {
                bar.removeListener(listener);
            }

            bukkitBar.removeAll();
            bukkitBar.setVisible(false);
        }

        private Player player() {
            return (Player) this.sender;
        }

        private org.bukkit.boss.BossBar createBossBar(final BossBar bar) {
            final org.bukkit.boss.BossBar bukkitBar = Bukkit.createBossBar(
                    Utilities.LEGACY_SERIALIZER.serialize(bar.name()),
                    mapColor(bar.color()),
                    mapStyle(bar.overlay())
            );

            bukkitBar.setProgress(bar.progress());
            applyFlags(bukkitBar, bar.flags());

            final BossBar.Listener listener = new BossBar.Listener() {
                @Override
                public void bossBarNameChanged(final BossBar changedBar, final Component oldName, final Component newName) {
                    bukkitBar.setTitle(Utilities.LEGACY_SERIALIZER.serialize(newName));
                }

                @Override
                public void bossBarProgressChanged(final BossBar changedBar, final float oldProgress, final float newProgress) {
                    bukkitBar.setProgress(newProgress);
                }

                @Override
                public void bossBarColorChanged(final BossBar changedBar, final BossBar.Color oldColor, final BossBar.Color newColor) {
                    bukkitBar.setColor(mapColor(newColor));
                }

                @Override
                public void bossBarOverlayChanged(final BossBar changedBar, final BossBar.Overlay oldOverlay, final BossBar.Overlay newOverlay) {
                    bukkitBar.setStyle(mapStyle(newOverlay));
                }

                @Override
                public void bossBarFlagsChanged(final BossBar changedBar, final Set<BossBar.Flag> added, final Set<BossBar.Flag> removed) {
                    applyFlags(bukkitBar, changedBar.flags());
                }
            };

            bar.addListener(listener);
            listeners.put(bar, listener);
            return bukkitBar;
        }

        private void close() {
            for (final BossBar bar : Set.copyOf(activeBossBars.keySet())) {
                hideBossBar(bar);
            }
        }

        private static int ticks(final Duration duration) {
            return Math.max(0, (int) (duration.toMillis() / 50L));
        }

        private static BarColor mapColor(final BossBar.Color color) {
            return BarColor.valueOf(color.name());
        }

        private static BarStyle mapStyle(final BossBar.Overlay overlay) {
            return switch (overlay) {
                case PROGRESS -> BarStyle.SOLID;
                case NOTCHED_6 -> BarStyle.SEGMENTED_6;
                case NOTCHED_10 -> BarStyle.SEGMENTED_10;
                case NOTCHED_12 -> BarStyle.SEGMENTED_12;
                case NOTCHED_20 -> BarStyle.SEGMENTED_20;
            };
        }

        private static void applyFlags(final org.bukkit.boss.BossBar bukkitBar, final Set<BossBar.Flag> flags) {
            for (final BarFlag flag : BarFlag.values()) {
                bukkitBar.removeFlag(flag);
            }
            for (final BossBar.Flag flag : flags) {
                bukkitBar.addFlag(mapFlag(flag));
            }
        }

        private static BarFlag mapFlag(final BossBar.Flag flag) {
            return switch (flag) {
                case DARKEN_SCREEN -> BarFlag.DARKEN_SKY;
                case PLAY_BOSS_MUSIC -> BarFlag.PLAY_BOSS_MUSIC;
                case CREATE_WORLD_FOG -> BarFlag.CREATE_FOG;
            };
        }

    }

}
