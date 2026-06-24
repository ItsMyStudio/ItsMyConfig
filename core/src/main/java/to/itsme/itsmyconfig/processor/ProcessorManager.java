package to.itsme.itsmyconfig.processor;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.PluginManager;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.processor.packetevents.PEventsListener;
import to.itsme.itsmyconfig.processor.protocollib.PLibListener;
import to.itsme.itsmyconfig.util.reflect.Reflections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessorManager {

    private final ItsMyConfig plugin;
    private final PacketListener listener;

    public ProcessorManager(final ItsMyConfig plugin) {
        this.plugin = plugin;
        this.listener = decideHandler();
    }

    public PacketListener decideHandler() {
        final PluginManager manager = plugin.getServer().getPluginManager();
        final ConfigurationSection configSection = plugin.getConfig().getConfigurationSection("listeners");

        if (configSection == null) {
            plugin.getLogger().warning("No listener config section found.");
            return null;
        }

        final Map<String, Integer> availableListeners = new HashMap<>();

        if (Reflections.findClass("to{}itsme{}itsmyconfig{}shade{}packetevents.PacketEvents")) {
            int priority = configSection.getInt("PacketEvents.priority", Integer.MAX_VALUE);
            availableListeners.put("PacketEvents", priority);
        }

        if (manager.getPlugin("ProtocolLib") != null) {
            int priority = configSection.getInt("ProtocolLib.priority", Integer.MAX_VALUE);
            availableListeners.put("ProtocolLib", priority);
        }

        if (availableListeners.isEmpty()) {
            return null;
        }

        final List<Map.Entry<String, Integer>> sorted = new ArrayList<>(availableListeners.entrySet());
        sorted.sort(Map.Entry.comparingByValue());

        final String chosenPlugin = sorted.getFirst().getKey();
        plugin.getLogger().info("Using packet listener: " + chosenPlugin);

        return switch (chosenPlugin) {
            case "ProtocolLib" -> new PLibListener(
                    plugin,
                    configSection.getBoolean("ProtocolLib.cache-processors", false)
            );
            case "PacketEvents" -> new PEventsListener();
            default -> {
                plugin.getLogger().warning("Unknown plugin handler: " + chosenPlugin);
                yield null;
            }
        };
    }

    public void load() {
        listener.load();
    }

    public void close() {
        listener.close();
    }

    public PacketListener getListener() {
        return listener;
    }

}
