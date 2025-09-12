package to.itsme.itsmyconfig.tag.impl.toast;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.util.Scheduler;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitToastSender implements ToastSender {

    private final Set<NamespacedKey> advancementKeys = ConcurrentHashMap.newKeySet();

    public BukkitToastSender() {
        Scheduler.runTimer(() -> {
            int size = advancementKeys.size();
            advancementKeys.removeIf(key -> {
                Bukkit.getUnsafe().removeAdvancement(key);
                return true;
            });
            if (size > 0) {
                Bukkit.reloadData();
            }
        }, 20 * 60 * 20, 20 * 60 * 20); // every 20 minutes
    }

    /**
     * Sends a custom toast message to the player.
     *
     * @param player     The player to receive the toast.
     * @param title      The title component of the toast.
     * @param icon       The material to show as the icon.
     * @param frameType  The frame type: "task", "goal", or "challenge".
     */
    public void sendToast(Player player, Component title, Material icon, String frameType) {
        String titleJson = GsonComponentSerializer.gson().serialize(title);
        String descJson = GsonComponentSerializer.gson().serialize(Component.empty());

        String sanitizedFrame = switch (frameType.toLowerCase()) {
            case "challenge", "goal" -> frameType.toLowerCase();
            default -> "task";
        };

        NamespacedKey key = new NamespacedKey("itsmyconfig", "toast_" + UUID.randomUUID());
        String advancementJson = String.format(
            """
            {
              "criteria": {
                "impossible": {
                  "trigger": "minecraft:impossible"
                }
              },
              "display": {
                "icon": {
                  "item": "%s",
                  "id": "%s"
                },
                "title": %s,
                "description": %s,
                "frame": "%s",
                "announce_to_chat": false,
                "show_toast": true,
                "hidden": true
              }
            }
            """,
            icon.getKey(),
            icon.getKey(),
            titleJson,
            descJson,
            sanitizedFrame
        );

        Advancement advancement = Bukkit.getUnsafe().loadAdvancement(key, advancementJson);
        if (advancement == null) return;

        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        progress.awardCriteria("impossible");

        Scheduler.runLater(
                () -> {
                    player.getAdvancementProgress(advancement).revokeCriteria("impossible");
                    advancementKeys.add(key);
                },
                20L // 1 second later
        );
    }

}
