package to.itsme.itsmyconfig;

import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import to.itsme.itsmyconfig.api.ItsMyConfigAPI;
import to.itsme.itsmyconfig.command.CommandManager;
import to.itsme.itsmyconfig.processor.PacketListener;
import to.itsme.itsmyconfig.processor.ProcessorManager;
import to.itsme.itsmyconfig.hook.PAPIHook;
import to.itsme.itsmyconfig.message.AudienceResolver;
import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.placeholder.PlaceholderManager;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;
import to.itsme.itsmyconfig.placeholder.type.*;
import to.itsme.itsmyconfig.placeholder.type.ProgressbarPlaceholder;
import to.itsme.itsmyconfig.requirement.RequirementManager;
import to.itsme.itsmyconfig.util.IMCSerializer;
import to.itsme.itsmyconfig.util.LibraryLoader;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Versions;
import to.itsme.itsmyconfig.processor.ConsoleFilter;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * ItsMyConfig class represents the main configuration class for the plugin.
 * It extends the JavaPlugin class and provides methods to manage the plugin configuration.
 * It also holds instances of PlaceholderManager, ProgressBarBucket, RequirementManager, and BukkitAudiences.
 */
public final class ItsMyConfig extends JavaPlugin {

    private static ItsMyConfig instance;
    private static ItsMyConfigAPI api;

    private final PlaceholderManager placeholderManager = new PlaceholderManager();
    private final RequirementManager requirementManager = new RequirementManager();
    private FileConfiguration config;
    private String symbolPrefix;
    private boolean debug;

    ProcessorManager processorManager;
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
        LibraryLoader.loadLibraries();
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
        api = new DefaultIMCAPI(this);
        this.getServer().getServicesManager().register(
                ItsMyConfigAPI.class,
                api,
                this,
                org.bukkit.plugin.ServicePriority.Normal
        );
        AudienceResolver.load(this);
        List.of("imc", "itsmyconfig").forEach(alias -> new PAPIHook(this, alias).register());
        new CommandManager(this);

        this.loadConfig();

        new Metrics(this, 21713);

        this.processorManager = new ProcessorManager(this);

        final PacketListener listener = this.processorManager.getListener();
        if (listener == null) {
            this.getLogger().warning("No suitable packet listener found. Disabling plugin...");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.getLogger().info("Using packet listener: " + listener.name());
        this.processorManager.load();

        if (getConfig().getBoolean("translate-console")) {
            // Register Console Filter
            this.consoleFilter = new ConsoleFilter();
            final org.apache.logging.log4j.core.LoggerContext ctx = (org.apache.logging.log4j.core.LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
            final org.apache.logging.log4j.core.config.Configuration config = ctx.getConfiguration();
            config.getLoggerConfig(org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME).addFilter(this.consoleFilter);
            ctx.updateLoggers();
        }

        //if (Versions.IS_PAPER && Versions.isOrOver(1, 17, 2) && Versions.isBelow(1, 21, 6)) {
            //this.getLogger().info("Registering Kick Listener");
            //this.getServer().getPluginManager().registerEvents(new PlayerListener(),this);
        //}

        this.getLogger().info("ItsMyConfig loaded in " + (System.currentTimeMillis() - start) + "ms");
    }

    @Override
    public void onDisable() {
        if (this.consoleFilter != null) {
            final org.apache.logging.log4j.core.LoggerContext ctx = (org.apache.logging.log4j.core.LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
            final org.apache.logging.log4j.core.config.Configuration config = ctx.getConfiguration();
            config.getLoggerConfig(org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME).removeFilter(this.consoleFilter);
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
        final File folder = new File(this.getDataFolder(), "placeholders");
        if (folder.mkdirs()) {
            this.saveResource("placeholders/default.yml", false);
            this.saveResource("placeholders/example.yml", false);
        }
        
        this.migrateConfig(folder);
        this.loadFolder(folder, placeholderPaths);

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
     * Recursively loads .yml files from the specified folder.
     * It iterates through the files in the folder, loading each .yml file using the `loadCustomYml` method if it meets the criteria.
     *
     * @param folder                 The folder from which to load .yml files.
     * @param placeholderPaths       A map of registered placeholders to avoid duplicates.
     */
    private void loadFolder(
            final File folder,
            final Map<String, List<String>> placeholderPaths
    ) {
        if (folder == null || !folder.isDirectory()) {
            return;
        }

        final File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (final File file : files) {
            if (file.isDirectory()) {
                this.loadFolder(file, placeholderPaths);
            } else if (file.isFile() && file.getName().endsWith(".yml")) {
                this.loadYAMLFile(file, placeholderPaths);
            }
        }
    }

    /**
     * Loads custom data from a .yml file.
     * It reads the file using `YamlConfiguration` and extracts custom progress bars and placeholders if they exist.
     *
     * @param file                   The .yml file to load custom data from.
     * @param placeholderPaths       A map of registered placeholders to avoid duplicates.
     */
    private void loadYAMLFile(
            final File file,
            final Map<String, List<String>> placeholderPaths
    ) {
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.isConfigurationSection("custom-placeholder")) {
            loadPlaceholdersSection(config.getConfigurationSection("custom-placeholder"), file, placeholderPaths);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void migrateConfig(final File directory) {
        if (!this.config.isConfigurationSection("listeners")) {
            ConfigurationSection listeners = this.config.createSection("listeners");
            ConfigurationSection packetEvents = listeners.createSection("PacketEvents");
            packetEvents.set("priority", 1);

            ConfigurationSection protocolLib = listeners.createSection("ProtocolLib");
            protocolLib.set("priority", 2);
            protocolLib.set("cache-processors", false);
            this.saveConfig();
        }

        if (!this.config.contains("config-version")) {
            this.config.set("config-version", 1);
            this.saveConfig();
        }

        final boolean needsMigration = 
            this.config.isConfigurationSection("custom-placeholder")
            || this.config.isConfigurationSection("custom-progress");
        if (needsMigration) {
            File migratedConfig = new File(directory, "migrated-config.yml");
            if (migratedConfig.exists()) {
                migratedConfig = new File(directory, UUID.randomUUID() + ".yml");
            }
            try {
                final boolean created = migratedConfig.createNewFile();
                if (!created) {
                    return;
                }

                final YamlConfiguration migratedConf = YamlConfiguration.loadConfiguration(migratedConfig);
                final ConfigurationSection newSection = migratedConf.createSection("custom-placeholder");
                if (this.config.isConfigurationSection("custom-placeholder")) {
                    for (final String name : Objects.requireNonNull(this.config.getConfigurationSection("custom-placeholder")).getKeys(false)) {
                        newSection.set(name, this.config.get("custom-placeholder." + name));
                    }
                }

                if (this.config.isConfigurationSection("custom-progress")) {
                    for (final String name : Objects.requireNonNull(this.config.getConfigurationSection("custom-progress")).getKeys(false)) {
                        final ConfigurationSection section = this.config.getConfigurationSection("custom-progress." + name);
                        section.set("value", section.getString("symbol"));
                        section.set("type", "progress_bar");
                        section.set("symbol", null);
                        newSection.set(name, section);
                    }
                }

                migratedConf.save(migratedConfig);
                this.config.set("custom-progress", null);
                this.config.set("custom-placeholder", null);
                this.saveConfig();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Loads custom placeholders from a YAML configuration section.
     * It iterates over each placeholder defined in the section, constructs a corresponding `PlaceholderData` object, and registers it with the `placeholderManager`.
     * Additionally, it registers any associated requirements for each placeholder.
     *
     * @param section                The YAML configuration section containing placeholder data.
     * @param paths                  A map of registered placeholders to avoid duplicates.
     */
    @SuppressWarnings("ConstantConditions")
    private void loadPlaceholdersSection(
            final ConfigurationSection section,
            final File file,
            final Map<String, List<String>> paths
    ) {
        final String filePath = formatPath("ItsMyConfig\\" + file.getPath().replace("/", "\\").replace(getDataFolder().getPath() + "\\", ""));
        if (section == null) {
            getLogger().warning(String.format("No custom placeholders found in file %s", filePath));
            return;
        }

        for (final String identifier : section.getKeys(false)) {
            if (placeholderManager.has(identifier)) {
                paths.get(identifier).add(filePath);
                continue;
            }

            final ConfigurationSection placeholderSection = section.getConfigurationSection(identifier);
            if (placeholderSection == null) {
                getLogger().warning(String.format("Invalid placeholder configuration for %s in file %s", identifier, filePath));
                continue;
            }

            // Use getPlaceholderData to retrieve PlaceholderData
            final Placeholder placeholder = this.getPlaceholder(file.getPath(), placeholderSection);

            // Load requirements if they exist
            if (placeholderSection.isConfigurationSection("requirements")) {
                final ConfigurationSection requirementsSection = placeholderSection.getConfigurationSection("requirements");
                for (final String reqIdentifier : requirementsSection.getKeys(false)) {
                    final ConfigurationSection reqSection = requirementsSection.getConfigurationSection(reqIdentifier);
                    if (reqSection != null) {
                        placeholder.registerRequirement(reqSection);
                    } else {
                        getLogger().warning(String.format("Invalid requirement configuration for %s in placeholder %s from file %s", reqIdentifier, identifier, filePath));
                    }
                }
            }

            placeholderManager.register(identifier, placeholder);
            paths.computeIfAbsent(identifier, v -> new ArrayList<>()).add(filePath);
        }
    }

    /**
     * Retrieves the placeholder data based on the provided configuration section and identifier.
     *
     * @param filePath The path of the file config is from
     * @param section  The configuration section containing the placeholder data.
     * @return The placeholder data object.
     */
    private Placeholder getPlaceholder(String filePath, final ConfigurationSection section) {
        final PlaceholderType type = PlaceholderType.find(section.getString("type"));
        return switch (type) {
            case MATH -> new MathPlaceholder(filePath, section);
            case RANDOM -> new RandomPlaceholder(filePath, section);
            case LIST -> new ListPlaceholder(filePath, section);
            case ANIMATION -> new AnimatedPlaceholder(filePath, section);
            case COLOR -> new ColorPlaceholder(filePath, section);
            case COLORED_TEXT -> new ColoredTextPlaceholder(filePath, section);
            case PROGRESS_BAR -> new ProgressbarPlaceholder(filePath, section);
            default -> new StringPlaceholder(filePath, section);
        };
    }

    /**
     * Formats a file path to start with "ItsMyConfig" and shortens it if it contains more than 5 directories.
     *
     * @param path The original file path.
     * @return The formatted file path.
     */
    private String formatPath(final String path) {
        final String separator = File.separator;
        final String normalizedPath = path.replace("/", separator).replace("\\", separator);
        final String[] parts = normalizedPath.split(separator.equals("\\") ? "\\\\" : separator);
        if (parts.length > 5) {
            final StringBuilder shortenedPath = new StringBuilder(parts[0]);
            shortenedPath.append(separator).append(parts[1]);
            for (int i = 2; i < parts.length - 2; i++) {
                shortenedPath.append(separator).append("..");
            }
            shortenedPath.append(separator).append(parts[parts.length - 2]).append(separator).append(parts[parts.length - 1]);
            return shortenedPath.toString();
        }
        return path;
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
     * Returns the RequirementManager object. The RequirementManager class is responsible for managing requirements
     * and validating them.
     *
     * @return the RequirementManager object
     */
    public RequirementManager getRequirementManager() {
        return this.requirementManager;
    }

}
