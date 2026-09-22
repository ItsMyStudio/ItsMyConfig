package to.itsme.itsmyconfig.tag.argument;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.message.AudienceResolver;
import to.itsme.itsmyconfig.tag.api.ArgumentsTag;
import to.itsme.itsmyconfig.tag.api.Cancellable;
import to.itsme.itsmyconfig.util.Scheduler;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class BossbarTag extends ArgumentsTag implements Cancellable {

    private final Map<UUID, List<BossBar>> barMap = new ConcurrentHashMap<>();

    {
        Scheduler.runTimerAsync(() -> barMap.entrySet().removeIf(entry -> {
            final Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) return true;

            final List<BossBar> bars = entry.getValue();
            if (bars == null) return true;

            return bars.isEmpty();
        }), 10, 10);
    }

    @Override
    public String name() {
        return "bossbar";
    }

    @Override
    public int minArguments() {
        return 4;
    }

    @Override
    public int maxArguments() {
        return 5;
    }

    @Override
    public String process(
            final Player player,
            final String[] arguments
    ) {
        final BossBar.Color color = BossBar.Color.NAMES.value(arguments[2].toLowerCase());
        final BossBar.Overlay overlay = BossBar.Overlay.NAMES.value(arguments[3].toLowerCase());

        long delay;
        if (arguments.length > 4) {
            delay = Integer.parseInt(arguments[4]);
        } else {
            delay = 20;
        }

        final String progressString = arguments[1];
        final float progress;
        final boolean updateProgress;
        switch (progressString) {
            case "progress":
                progress = 0.0f;
                updateProgress = true;
                break;
            case "reverse":
                progress = 1.0f;
                updateProgress = true;
                break;
            default:
                updateProgress = false;
                progress = Math.min(Strings.floatOrDefault(progressString, 1.0f) / 100, 1.0f);
                break;
        }

        final String text = arguments[0];
        final Component component = Utilities.translate(translate(text, progress, delay), player);

        final BossBar bar = BossBar.bossBar(
                component,
                progress,
                color == null ? BossBar.Color.PINK : color,
                overlay == null ? BossBar.Overlay.PROGRESS : overlay
        );


        barMap.computeIfAbsent(player.getUniqueId(), id -> new CopyOnWriteArrayList<>()).add(bar);
        AudienceResolver.resolve(player).showBossBar(bar);

        final AtomicInteger atomicTicks = new AtomicInteger();
        Scheduler.runTimerAsync((task) -> {
            final List<BossBar> barList = barMap.get(player.getUniqueId());
            if (barList == null || !barList.contains(bar)) {
                task.cancel();
                AudienceResolver.resolve(player).hideBossBar(bar);
                return;
            }

            final int ticks = atomicTicks.incrementAndGet();
            final long leftTicks = delay - ticks;
            bar.name(Utilities.translate(translate(text, progress, leftTicks), player));

            if (updateProgress) {
                final float currentProgress = (float) ticks / delay;
                bar.progress(Math.min(progressString.equals("reverse") ? 1.0F - currentProgress : currentProgress, 1.0f));
            }

            if (leftTicks <= 0) {
                AudienceResolver.resolve(player).hideBossBar(bar);
                barList.remove(bar);
                task.cancel();
            }
        }, 0, 1);

        return "";
    }

    private String translate(
            final String text,
            final float progress,
            final long ticksLeft
    ) {
        return text
                .replaceAll("<v:time_left>", String.valueOf(ticksLeft / 20))
                .replaceAll("<v:time_left_percent>", String.valueOf(progress * 100));
    }

    @Override
    public void cancelFor(final Player player) {
        final List<BossBar> bars = barMap.get(player.getUniqueId());
        if (bars == null) {
            return;
        }

        if (!bars.isEmpty()) {
            final Audience audience = AudienceResolver.resolve(player);
            for (final BossBar bar : bars) {
                audience.hideBossBar(bar);
            }
        }

        barMap.remove(player.getUniqueId());
    }

}
