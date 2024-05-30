package to.itsme.itsmyconfig.tag.impl;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.tag.api.ArgumentsTag;
import to.itsme.itsmyconfig.tag.api.Cancellable;
import to.itsme.itsmyconfig.util.Scheduler;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RepeatTag extends ArgumentsTag implements Cancellable {

    private final Map<UUID, List<WrappedTask>> tasksMap = new ConcurrentHashMap<>();

    {
        Scheduler.runTimerAsync(() -> tasksMap.entrySet().removeIf(entry -> {
            final Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) return true;

            final List<WrappedTask> tasks = entry.getValue();
            if (tasks == null || tasks.isEmpty()) return true;

            for (final WrappedTask task : tasks) {
                if (!task.isCancelled()) {
                    return false;
                }
            }

            return true;
        }), 10, 10);
    }

    @Override
    public String name() {
        return "repeat";
    }

    @Override
    public int minArguments() {
        return 2;
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
        final String text = arguments[0];
        final int amount = Strings.intOrDefault(arguments[1], 1);

        int delayInTicks = 20;

        if (arguments.length > 2) {
            delayInTicks = Strings.intOrDefault(arguments[2], 20);
        }

        final AtomicInteger times = new AtomicInteger(amount);
        Scheduler.runTimerAsync(task -> {
            if (!player.isOnline() || times.getAndDecrement() <= 0) {
                task.cancel();
                return;
            }

            tasksMap.computeIfAbsent(player.getUniqueId(), id -> new ArrayList<>()).add(task);
            final Component translated = Utilities.translate(
                    text
                            .replaceAll("<v:repeat_total>", String.valueOf(amount))
                            .replaceAll("<v:repeat_left>", String.valueOf(times.get()))
                            .replaceAll("<v:repeat_count>", String.valueOf(amount - times.get()))
                    ,
                    player
            );

            if (!Component.empty().equals(translated)) {
                plugin.adventure().player(player).sendMessage(translated);
            }

        }, 0L, delayInTicks);

        return "";
    }

    @Override
    public void cancelFor(final Player player) {
        final List<WrappedTask> tasks = tasksMap.get(player.getUniqueId());
        if (tasks == null) {
            return;
        }

        for (final WrappedTask task : tasks) {
            if (!task.isCancelled()) task.cancel();
        }

        tasksMap.remove(player.getUniqueId());
    }

}
