package to.itsme.itsmyconfig.placeholder;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.config.IMConfig;
import to.itsme.itsmyconfig.placeholder.type.*;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * The PlaceholderManager class is responsible for managing placeholders.
 * It provides methods to register, unregister, and retrieve placeholders.
 */
public final class PlaceholderManager {

    private final ItsMyConfig plugin;

    /**
     * Represents a synchronized map of placeholder keys and PlaceholderData objects.
     * Placeholders are used to represent dynamic values that can be replaced in messages or text.
     */
    private final Map<String, Placeholder> placeholders = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, CompiledPlaceholder> compiledPlaceholders = Collections.synchronizedMap(new LinkedHashMap<>());

    private final List<String> papiPlaceholderKeys = new ArrayList<>();

    public PlaceholderManager(final ItsMyConfig plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers a placeholder with the provided key and value.
     *
     * @param key   The key of the placeholder.
     * @param value The PlaceholderData object representing the value of the placeholder.
     */
    public void register(final String key, final Placeholder value) {
        this.placeholders.put(key, value);
        this.compileIncremental(value);
    }

    /**
     * Clears all registered placeholders.
     */
    public void unregisterAll() {
        this.placeholders.clear();
        this.papiPlaceholderKeys.clear();
        this.compiledPlaceholders.clear();
    }

    /**
     * Unregisters a placeholder with the specified key.
     *
     * @param key The key of the placeholder to unregister.
     */
    public void unregister(final String key) {
        final Placeholder removed = this.placeholders.remove(key);
        if (removed != null) {
            this.removeCompiled(removed);
        }
    }

    /**
     * Checks if the specified key is present in the PlaceholderManager.
     *
     * @param key The key to check.
     * @return true if the key is present, false otherwise.
     */
    public boolean has(final String key) {
        return this.placeholders.containsKey(key);
    }

    public boolean hasCompiled(final String key) {
        return this.compiledPlaceholders.containsKey(key);
    }

    /**
     * Retrieves the placeholder data object associated with the given key.
     *
     * @param key The key used to retrieve the placeholder data object.
     * @return The PlaceholderData object associated with the given key, or null if the key does not exist.
     */
    public Placeholder get(final String key) {
        return this.placeholders.get(key);
    }

    public CompiledPlaceholder getCompiled(final String key) {
        return this.compiledPlaceholders.get(key);
    }

    /**
     * Returns a {@link Map} of placeholders.
     *
     * @return a map containing placeholders as keys and their corresponding {@link Placeholder} objects as values
     */
    public Map<String, Placeholder> getPlaceholdersMap() {
        return Map.copyOf(placeholders);
    }

    /**
     * Retrieves the keys of all registered placeholders.
     *
     * @return a set containing the keys of all registered placeholders.
     */
    public Set<String> getPlaceholderKeys() {
        return placeholders.keySet();
    }

    public List<String> getPapiPlaceholderKeys() {
        return papiPlaceholderKeys;
    }

    private void compileIncremental(final Placeholder placeholder) {
        for (final CompiledPlaceholder compiled : placeholder.getCompiledPlaceholders()) {
            this.compiledPlaceholders.put(compiled.key(), compiled);
            this.papiPlaceholderKeys.add("%itsmyconfig_" + compiled.key() + "%");
            this.papiPlaceholderKeys.add("%imc_" + compiled.key() + "%");
        }
    }

    private void removeCompiled(final Placeholder placeholder) {
        for (final CompiledPlaceholder compiled : placeholder.getCompiledPlaceholders()) {
            this.compiledPlaceholders.remove(compiled.key());
            this.papiPlaceholderKeys.remove("%itsmyconfig_" + compiled.key() + "%");
            this.papiPlaceholderKeys.remove("%imc_" + compiled.key() + "%");
        }
    }

    /**
     * Recursively loads .yml files from the specified folder.
     * It iterates through the files in the folder, loading each .yml file using the `loadCustomYml` method if it meets the criteria.
     *
     * @param folder           The folder from which to load .yml files.
     * @param placeholderPaths A map of registered placeholders to avoid duplicates.
     */
    public void loadFolder(
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
     * @param file             The .yml file to load custom data from.
     * @param placeholderPaths A map of registered placeholders to avoid duplicates.
     */
    private void loadYAMLFile(
            final File file,
            final Map<String, List<String>> placeholderPaths
    ) {
        try {
            final IMConfig config = new IMConfig(file, null, false);
            if (config.isSection("custom-placeholder")) {
                loadPlaceholdersSection(config.getSection("custom-placeholder"), file, placeholderPaths);
            }
        } catch (final Exception e) {
            plugin.getLogger().log(Level.SEVERE, String.format("Error occurred while loading YAML file %s", file.getPath()), e);
        }
    }

    /**
     * Loads custom placeholders from a YAML configuration section.
     * It iterates over each placeholder defined in the section, constructs a corresponding `PlaceholderData` object, and registers it with the `placeholderManager`.
     *
     * @param section The YAML configuration section containing placeholder data.
     * @param paths   A map of registered placeholders to avoid duplicates.
     */
    private void loadPlaceholdersSection(
            final Section section,
            final File file,
            final Map<String, List<String>> paths
    ) {
        final String filePath = formatPath("ItsMyConfig\\" + file.getPath().replace("/", "\\").replace(plugin.getDataFolder().getPath() + "\\", ""));
        if (section == null) {
            plugin.getLogger().warning(String.format("No custom placeholders found in file %s", filePath));
            return;
        }

        for (final String identifier : section.getRoutesAsStrings(false)) {
            if (has(identifier)) {
                paths.get(identifier).add(filePath);
                continue;
            }

            final Section placeholderSection = section.getSection(identifier);
            if (placeholderSection == null) {
                plugin.getLogger().warning(String.format("Invalid placeholder configuration for %s in file %s", identifier, filePath));
                continue;
            }

            final Placeholder placeholder = getPlaceholder(file.getPath(), placeholderSection);

            register(identifier, placeholder);
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
    private Placeholder getPlaceholder(final String filePath, final Section section) {
        final PlaceholderType type = PlaceholderType.find(section.getString("type"));
        return switch (type) {
            case MATH -> new MathPlaceholder(filePath, section);
            case RANDOM -> new RandomPlaceholder(filePath, section);
            case LIST -> new ListPlaceholder(filePath, section);
            case ANIMATION -> new AnimatedPlaceholder(filePath, section);
            case COLOR -> new ColorPlaceholder(filePath, section);
            case COLORED_TEXT -> new ColoredTextPlaceholder(filePath, section);
            case PROGRESS_BAR -> new ProgressbarPlaceholder(filePath, section);
            case CONDITIONAL -> new ConditionalPlaceholder(filePath, section);
            case SWITCH -> new SwitchPlaceholder(filePath, section);
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

}
