package to.itsme.itsmyconfig;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import to.itsme.itsmyconfig.command.CommandManager;
import to.itsme.itsmyconfig.listener.impl.PacketChatListener;
import to.itsme.itsmyconfig.listener.impl.PacketItemListener;
import to.itsme.itsmyconfig.placeholder.DynamicPlaceHolder;
import to.itsme.itsmyconfig.placeholder.PlaceholderData;
import to.itsme.itsmyconfig.placeholder.PlaceholderManager;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;
import to.itsme.itsmyconfig.placeholder.type.AnimatedPlaceholderData;
import to.itsme.itsmyconfig.placeholder.type.ColorPlaceholderData;
import to.itsme.itsmyconfig.placeholder.type.RandomPlaceholderData;
import to.itsme.itsmyconfig.placeholder.type.StringPlaceholderData;
import to.itsme.itsmyconfig.progress.ProgressBar;
import to.itsme.itsmyconfig.progress.ProgressBarBucket;
import to.itsme.itsmyconfig.requirement.RequirementManager;

import java.io.File;
/**
 * ItsMyConfig class represents the main configuration class for the plugin.
 * It extends the JavaPlugin class and provides methods to manage the plugin configuration.
 * It also holds instances of PlaceholderManager, ProgressBarBucket, RequirementManager, and BukkitAudiences.
 */
public final class ItsMyConfig extends JavaPlugin {

    //private static final boolean ALLOW_ITEM_EDITS = true;

    private static ItsMyConfig instance;
    private final PlaceholderManager placeholderManager = new PlaceholderManager();
    private final ProgressBarBucket progressBarBucket = new ProgressBarBucket();
    private String symbolPrefix;
    private boolean allowItemEdits;
    private RequirementManager requirementManager;

    private BukkitAudiences adventure;

    public static ItsMyConfig getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        this.getLogger().info("Loading ItsMyConfig...");
        final long start = System.currentTimeMillis();
        instance = this;
        new DynamicPlaceHolder(this, progressBarBucket).register();
        new CommandManager(this);

        this.requirementManager = new RequirementManager();
        this.adventure = BukkitAudiences.create(this);

        loadConfig();

        new Metrics(this, 21713);

        final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketChatListener(this));

        if (allowItemEdits) {
            protocolManager.addPacketListener(new PacketItemListener(this));
        }

        this.getLogger().info("ItsMyConfig loaded in " + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * The loadConfig method is responsible for loading the configuration file and initializing various settings and data.
     * It performs the following steps:
     * <p>
     * 1. Clears all progress bars in the ProgressBarBucket.
     * 2. Saves the default configuration file if it does not exist.
     * 3. Reloads the configuration file.
     * 4. Loads the symbol prefix from the configuration.
     * 5. Load the allow item edits
     * 6. Loads the custom placeholders from the configuration and registers them.
     * 7. Loads the custom progress bars from the configuration and registers them.
     * 8. Load de custom yaml files
     */
    public void loadConfig() {
        progressBarBucket.clearAllProgressBars();
        this.saveDefaultConfig();
        this.reloadConfig();
        this.loadSymbolPrefix();
        this.loadAllowItemEdits();
        this.loadPlaceholders();
        this.loadProgressBars();
        this.loadCustomYmlFiles();
    }

    /**
     * Loads the symbol prefix from the configuration.
     */
    private void loadSymbolPrefix() {
        this.symbolPrefix = this.getConfig().getString("symbol-prefix");
    }

    /**
     * Loads the allow-item-edits setting from the configuration.
     */
    private void loadAllowItemEdits() {
        this.allowItemEdits = this.getConfig().getBoolean("allow-item-edits");
    }

    /**
     * Loads the placeholders from the configuration file and registers them with the placeholder manager.
     * This method iterates over the placeholders configuration section, retrieves the placeholder data,
     * registers any associated requirements, and finally registers the placeholder with the placeholder manager.
     */
    private void loadPlaceholders() {
        placeholderManager.unregisterAll();
        final ConfigurationSection placeholdersConfigSection =
                this.getConfig().getConfigurationSection("custom-placeholder");
        for (final String identifier : placeholdersConfigSection.getKeys(false)) {
            final long currentTime = System.currentTimeMillis();
            final PlaceholderData data = this.getPlaceholderData(placeholdersConfigSection.getConfigurationSection(identifier));
            registerPlaceholder(placeholdersConfigSection, identifier, data);
            this.getLogger().info(String.format("Registered placeholder %s in %dms", identifier, System.currentTimeMillis() - currentTime));
        }
    }

    /**
     * Retrieves the placeholder data based on the provided configuration section and identifier.
     *
     * @param placeholderSection The configuration section containing the placeholder data.
     * @return The placeholder data object.
     */
    private PlaceholderData getPlaceholderData(final ConfigurationSection placeholderSection) {
        final PlaceholderType type = PlaceholderType.find(placeholderSection.getString("type"));

        final String valueProperty = "value";
        final String valuesProperty = "values";

        switch (type) {
            case RANDOM:
                return new RandomPlaceholderData(placeholderSection.getStringList(valuesProperty));
            case ANIMATION:
                return new AnimatedPlaceholderData(
                        placeholderSection.getStringList(valuesProperty),
                        placeholderSection.getInt("interval", 20)
                );
            case COLOR:
                return new ColorPlaceholderData(placeholderSection);
            default:
            case STRING:
                return new StringPlaceholderData(placeholderSection.getString(valueProperty, ""));
        }
    }

    /**
     * Registers a placeholder with the provided identifier and data.
     *
     * @param placeholdersConfigSection The ConfigurationSection containing placeholder data.
     * @param identifier               The identifier of the placeholder.
     * @param data                     The PlaceholderData object representing the data of the placeholder.
     */
    private void registerPlaceholder(
            final ConfigurationSection placeholdersConfigSection,
            final String identifier,
            final PlaceholderData data
    ) {
        final ConfigurationSection requirementsConfigSection =
                placeholdersConfigSection.getConfigurationSection(identifier + ".requirements");
        if (requirementsConfigSection != null) {
            for (final String req : requirementsConfigSection.getKeys(false)) {
                data.registerRequirement(requirementsConfigSection.getConfigurationSection(req));
            }
        }
        this.placeholderManager.register(identifier, data);
    }

    /**
     * Loads progress bars from the configuration file.
     * Each progress bar is registered in the ProgressBarBucket.
     */
    private void loadProgressBars() {
        final ConfigurationSection progressBarConfigSection =
                this.getConfig().getConfigurationSection("custom-progress");
        for (final String identifier : progressBarConfigSection.getKeys(false)) {
            final ConfigurationSection configurationSection =
                    progressBarConfigSection
                            .getConfigurationSection(identifier);
            progressBarBucket.registerProgressBar(
                    new ProgressBar(
                            identifier,
                            configurationSection.getString("symbol"),
                            configurationSection.getString("completed-color"),
                            configurationSection.getString("progress-color"),
                            configurationSection.getString("remaining-color")
                    )
            );
        }
    }

    /**
     * Loads custom .yml files from the plugin's data folder recursively.
     * It checks for the existence of the data folder, then proceeds to load .yml files using the `loadYmlFiles` method.
     */
    private void loadCustomYmlFiles() {
        File dataFolder = getDataFolder();
        if (dataFolder.exists()) {
            loadYmlFiles(dataFolder);
        }
    }

    /**
     * Recursively loads .yml files from the specified folder.
     * It iterates through the files in the folder, loading each .yml file using the `loadCustomYml` method if it meets the criteria.
     *
     * @param folder The folder from which to load .yml files.
     */
    private void loadYmlFiles(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    loadYmlFiles(file);
                } else if (file.isFile() && file.getName().endsWith(".yml") && !file.getName().equals("config.yml")) {
                    loadCustomYml(file);
                }
            }
        }
    }

    /**
     * Loads custom data from a .yml file.
     * It reads the file using `YamlConfiguration` and extracts custom progress bars and placeholders if they exist.
     *
     * @param file The .yml file to load custom data from.
     */
    private void loadCustomYml(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.contains("custom-progress")) {
            loadProgressBarsFromYml(config.getConfigurationSection("custom-progress"), file.getName());
        }
        if (config.contains("custom-placeholder")) {
            loadPlaceholdersFromYml(config.getConfigurationSection("custom-placeholder"), file.getName());
        }
    }

    /**
     * Loads custom progress bars from a YAML configuration section.
     * It iterates over each progress bar defined in the section, constructs a `ProgressBar` object, and registers it with the `progressBarBucket`.
     *
     * @param section  The YAML configuration section containing progress bar data.
     * @param fileName The name of the file from which the data is loaded.
     */
    private void loadProgressBarsFromYml(ConfigurationSection section, String fileName) {
        for (String identifier : section.getKeys(false)) {
            long currentTime = System.currentTimeMillis(); // Capture current time
            ConfigurationSection progressBarSection = section.getConfigurationSection(identifier);
            progressBarBucket.registerProgressBar(
                    new ProgressBar(
                            identifier,
                            progressBarSection.getString("symbol"),
                            progressBarSection.getString("completed-color"),
                            progressBarSection.getString("progress-color"),
                            progressBarSection.getString("remaining-color")
                    )
            );
            getLogger().info(String.format("Registered progress bar %s from file %s in %dms", identifier, fileName, System.currentTimeMillis() - currentTime));
        }
    }

    /**
     * Loads custom placeholders from a YAML configuration section.
     * It iterates over each placeholder defined in the section, constructs a corresponding `PlaceholderData` object, and registers it with the `placeholderManager`.
     * Additionally, it registers any associated requirements for each placeholder.
     *
     * @param section  The YAML configuration section containing placeholder data.
     * @param fileName The name of the file from which the data is loaded.
     */
    private void loadPlaceholdersFromYml(ConfigurationSection section, String fileName) {
        if (section == null) {
            getLogger().warning(String.format("No custom placeholders found in file %s", fileName));
            return;
        }

        for (String identifier : section.getKeys(false)) {
            ConfigurationSection placeholderSection = section.getConfigurationSection(identifier);
            if (placeholderSection == null) {
                getLogger().warning(String.format("Invalid placeholder configuration for %s in file %s", identifier, fileName));
                continue;
            }

            // Use getPlaceholderData to retrieve PlaceholderData
            PlaceholderData placeholderData = getPlaceholderData(placeholderSection);

            // Load requirements if they exist
            if (placeholderSection.isConfigurationSection("requirements")) {
                ConfigurationSection requirementsSection = placeholderSection.getConfigurationSection("requirements");
                if (requirementsSection != null) {
                    for (String reqIdentifier : requirementsSection.getKeys(false)) {
                        ConfigurationSection reqSection = requirementsSection.getConfigurationSection(reqIdentifier);
                        if (reqSection != null) {
                            placeholderData.registerRequirement(reqSection);
                        } else {
                            getLogger().warning(String.format("Invalid requirement configuration for %s in placeholder %s from file %s", reqIdentifier, identifier, fileName));
                        }
                    }
                }
            }

            placeholderManager.register(identifier, placeholderData);
            long currentTime = System.currentTimeMillis(); // Capture current time
            getLogger().info(String.format("Registered placeholder %s from file %s in %dms", identifier, fileName, System.currentTimeMillis() - currentTime));
        }
    }

    /**
     * Retrieves the instance of the `BukkitAudiences` class used for sending chat messages and titles.
     *
     * @return The instance of the `BukkitAudiences` class.
     * @throws IllegalStateException if the plugin is disabled and the `Adventure` instance is accessed.
     */
    public BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

    /**
     * Retrieves the symbol prefix.
     *
     * @return The symbol prefix used in messages or text.
     */
    public String getSymbolPrefix() {
        return symbolPrefix;
    }

    /**
     * Retrieves the PlaceholderManager instance.
     *
     * @return The PlaceholderManager instance.
     */
    public PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }

    /**
     * Returns the RequirementManager object. The RequirementManager class is responsible for managing requirements
     * and validating them.
     *
     * @return the RequirementManager object
     */
    public RequirementManager getRequirementManager() {
        return requirementManager;
    }

    public boolean isAllowItemEdits() {
        return allowItemEdits;
    }

}
