package to.itsme.itsmyconfig;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import to.itsme.itsmyconfig.api.ItsMyConfigAPI;
import to.itsme.itsmyconfig.command.CommandManager;
import to.itsme.itsmyconfig.hook.PAPIHook;
import to.itsme.itsmyconfig.message.AudienceResolver;
import to.itsme.itsmyconfig.migration.MigrationManager;
import to.itsme.itsmyconfig.placeholder.PlaceholderManager;
import to.itsme.itsmyconfig.placeholder.type.*;
import to.itsme.itsmyconfig.processor.ConsoleFilter;
import to.itsme.itsmyconfig.processor.PacketListener;
import to.itsme.itsmyconfig.processor.ProcessorManager;
import to.itsme.itsmyconfig.util.IMCSerializer;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Versions;

import java.io.File;
import java.util.*;

/**
 * ItsMyConfig class represents the main configuration class for the plugin.
 * It extends the JavaPlugin class and provides methods to manage the plugin configuration.
 * It also holds a PlaceholderManager instance.
 */
public final class ItsMyConfig extends JavaPlugin {

    private static ItsMyConfig instance;
    private static ItsMyConfigAPI api;

    private final PlaceholderManager placeholderManager = new PlaceholderManager(this);
    ProcessorManager processorManager;
    private FileConfiguration config;
    private File placeholdersFolder;
    private String symbolPrefix;
    private boolean debug;
    private ConsoleFilter consoleFilter;

    /**
     * Gets the instance of ItsMyConfig.
     *
     * @return The instance of ItsMyConfig.
     */
    public static ItsMyConfig getInstance() {
        return instance;
    }

    /**
     * Gets the instance of the default ItsMyConfig api.
     *
     * @return The instance of ItsMyConfigAPI.
     */
    public static ItsMyConfigAPI getAPI() {
        return api;
    }

    @Override
    public void onLoad() {
        instance = this;
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        this.getLogger().info("Loading ItsMyConfig...");
        if (Versions.isBelow(1, 16, 5)) {
            this.getLogger().info("Unsupported version. Please consider updating to 1.16.5+");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        final long start = System.currentTimeMillis();
        if (!this.init()) {
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.getLogger().info("ItsMyConfig loaded in " + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * Initializes the plugin, registering services, loading configuration,
     * and setting up packet listeners and metrics.
     *
     * @return {@code true} if initialization succeeded, {@code false} otherwise.
     */
    private boolean init() {
        api = new DefaultIMCAPI(this);
        this.getServer().getServicesManager().register(
                ItsMyConfigAPI.class,
                api,
                this,
                ServicePriority.Normal
        );
        AudienceResolver.load(this);
        List.of("imc", "itsmyconfig").forEach(alias -> new PAPIHook(this, alias).register());
        new CommandManager(this);

        this.placeholdersFolder = new File(this.getDataFolder(), "placeholders");
        this.loadConfig();

        new Metrics(this, 21713);

        this.processorManager = new ProcessorManager(this);

        final PacketListener listener = this.processorManager.getListener();
        if (listener == null) {
            this.getLogger().warning("No suitable packet listener found. Disabling plugin...");
            return false;
        }

        this.getLogger().info("Using packet listener: " + listener.name());
        this.processorManager.load();

        if (getConfig().getBoolean("translate-console")) {
            // Register Console Filter
            this.consoleFilter = new ConsoleFilter();
            final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            final var config = ctx.getConfiguration();
            config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME).addFilter(this.consoleFilter);
            ctx.updateLoggers();
        }

        return true;
    }

    @Override
    public void onDisable() {
        if (this.consoleFilter != null) {
            final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            final var config = ctx.getConfiguration();
            config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME).removeFilter(this.consoleFilter);
            ctx.updateLoggers();
        }
        AudienceResolver.close();
        this.processorManager.close();
    }

    /**
     * The loadConfig method is responsible for loading the configuration file and initializing various settings and data.
     * It performs the following steps:
     * <p>
     * 0. Cache time before loading placeholders.
     * 1-2. Track previously registered placeholders and progress bars.
     * 3-4. Clear all registered placeholders and progress bars.
     * 5. Save the default configuration file if it does not exist
     * 6. Reload the configuration from the file.
     * 7. Loads the symbol prefix from the configuration.
     * 8-9. Maps to keep track of registered placeholders and progress bars to avoid duplicates.
     * 10-11. Load and register placeholders and progress bars from the main configuration file.
     * 12. Load and register placeholders and progress bars from additional custom .yml files.
     * 13 - 14. Print all info about duplicated placeholders and bars.
     * 15 - 16. Print all info about deleted placeholders and bars.
     * 17. Clear maps from the cache to save memory.
     * 18. Send the placeholders loaded message.
     */
    public void loadConfig() {
        // 0: Cache time before loading placeholders
        final long time = System.currentTimeMillis();

        // 1 - 2: cache old placeholder and bar names
        final Set<String> previousPlaceholders = new HashSet<>(placeholderManager.getPlaceholderKeys());

        // 3 - 4: unregister all placeholders and bars
        this.placeholderManager.unregisterAll();

        // 5 - 7: load config.yml
        this.saveDefaultConfig();
        this.reloadConfig();
        this.config = this.getConfig();
        this.reloadConfigParams();

        this.getLogger().info("Using packet serializer: " + IMCSerializer.currentSerializerType().name());
        /* Disable warning temporarily till MM_COPY is stable
        if (IMCSerializer.currentSerializerType() != SerializerType.MM_COPY) {
            this.getLogger().warning("Your server is running with an outdated version of the Adventure library. This may be caused by an old server jar or a plugin that includes Adventure without properly relocating it. This can lead to compatibility issues with serialization.");
        }*/

        // 8 - 9:  Maps to keep track of registered placeholders and progress bars
        final Map<String, List<String>> placeholderPaths = new HashMap<>();

        // 10 - 11: Load and register placeholders and progress bars from the main configuration file
        // 12: Load and register placeholders and progress bars from additional custom .yml files
        if (placeholdersFolder.mkdirs()) {
            this.saveResource("placeholders/default.yml", false);
            this.saveResource("placeholders/example.yml", false);
        }

        new MigrationManager(this).migrate();
        this.placeholderManager.loadFolder(placeholdersFolder, placeholderPaths);

        // 13 - 14: Print all info about duplicated placeholders and bars
        final String listSeparator = "\n   - ";
        final Comparator<String> comparator = Comparator.comparingInt(String::length);
        for (final Map.Entry<String, List<String>> entry : placeholderPaths.entrySet()) {
            final String name = entry.getKey();
            final List<String> paths = entry.getValue();

            paths.sort(comparator);
            if (paths.size() > 1) {
                this.getLogger().warning(
                        "Placeholder \"" + name + "\" is duplicated in the following files:" + listSeparator + String.join(listSeparator, paths)
                );
            }
        }

        // 15 - 16: Print all info about deleted placeholders and bars
        previousPlaceholders.removeAll(placeholderManager.getPlaceholderKeys());
        for (final String identifier : previousPlaceholders) {
            this.getLogger().info(String.format("Unregistering placeholder %s as it no longer exists in the configuration.", identifier));
        }

        // 17: delete all cache from memory
        placeholderPaths.clear();
        previousPlaceholders.clear();

        // 18: Send the placeholders loaded message
        this.getLogger().info(
                String.format(
                        "Loaded all %d Placeholders in %dms",
                        placeholderManager.getPlaceholderKeys().size(),
                        System.currentTimeMillis() - time
                )
        );
    }

    /**
     * (Re-)Loads the config params from the configuration.
     */
    private void reloadConfigParams() {
        this.debug = this.config.getBoolean("debug");
        this.symbolPrefix = this.config.getString("symbol-prefix");
        Strings.setSymbolPrefix(this.symbolPrefix);
        MathPlaceholder.UPDATE_FORMATTINGS();
    }

    /**
     * Retrieves the symbol prefix.
     *
     * @return The symbol prefix used in messages or text.
     */
    public String getSymbolPrefix() {
        return this.symbolPrefix;
    }

    /**
     * Retrieves whether debug is enabled or not.
     *
     * @return The debug boolean used for debug checks.
     */
    public boolean isDebug() {
        return this.debug;
    }

    /**
     * Sets the debug mode for the plugin.
     *
     * @param debug The boolean value to set debug mode.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Toggles the debug mode for the plugin.
     * If debug is currently enabled, it will be disabled, and vice versa.
     *
     * @return The new state of debug mode after toggling.
     */
    public boolean toggleDebug() {
        this.setDebug(!this.isDebug());
        return this.debug;
    }

    /**
     * Retrieves the PlaceholderManager instance.
     *
     * @return The PlaceholderManager instance.
     */
    public PlaceholderManager getPlaceholderManager() {
        return this.placeholderManager;
    }

    /**
     * Retrieves the folder used to store custom placeholder .yml files.
     *
     * @return The placeholders folder (e.g. /plugins/ItsMyConfig/placeholders).
     */
    public File getPlaceholdersFolder() {
        return placeholdersFolder;
    }
}
