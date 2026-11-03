package to.itsme.itsmyconfig.processor;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
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
        final String chosenPlugin = chooseListener(plugin);
        if (chosenPlugin == null) {
            return null;
        }
        plugin.getLogger().info("Using packet listener: " + chosenPlugin);

        final ConfigurationSection configSection = plugin.getConfig().getConfigurationSection("listeners");
        return switch (chosenPlugin) {
            case "ProtocolLib" -> new PLibListener(
                    plugin,
                    configSection != null && configSection.getBoolean("ProtocolLib.cache-processors", false)
            );
            case "PacketEvents" -> new PEventsListener(plugin);
            default -> {
                plugin.getLogger().warning("Unknown plugin handler: " + chosenPlugin);
                yield null;
            }
        };
    }

    /**
     * Picks the packet library to use, from the {@code listeners} section of
     * config.yml and the libraries that are available.
     * <p>
     * It only reads the config and looks up classes and plugins, so it is safe
     * to call from {@code onLoad}. This lets the plugin skip loading its shaded
     * PacketEvents when ProtocolLib is chosen: PacketEvents' {@code load()}
     * injects netty handlers right away when the server is already bound
     * (Paper binds the port before plugins load).
     *
     * @return "PacketEvents", "ProtocolLib", or {@code null} if neither is available
     */
    public static String chooseListener(final JavaPlugin plugin) {
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
        return sorted.getFirst().getKey();
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
