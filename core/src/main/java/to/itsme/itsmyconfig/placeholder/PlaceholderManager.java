package to.itsme.itsmyconfig.placeholder;

import java.util.*;

/**
 * The PlaceholderManager class is responsible for managing placeholders.
 * It provides methods to register, unregister, and retrieve placeholders.
 */
public final class PlaceholderManager {

    /**
     * Represents a synchronized map of placeholder keys and PlaceholderData objects.
     * Placeholders are used to represent dynamic values that can be replaced in messages or text.
     */
    private final Map<String, Placeholder> placeholders = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, CompiledPlaceholder> compiledPlaceholders = Collections.synchronizedMap(new LinkedHashMap<>());

    private final List<String> papiPlaceholderKeys = new ArrayList<>();

    /**
     * Registers a placeholder with the provided key and value.
     *
     * @param key   The key of the placeholder.
     * @param value The PlaceholderData object representing the value of the placeholder.
     */
    public void register(final String key, final Placeholder value) {
        this.placeholders.put(key, value);
        this.rebuildCompiledPlaceholders();
    }

    /**
     * Clears all registered placeholders.
     */
    public void unregisterAll() {
        this.placeholders.clear();
        this.compiledPlaceholders.clear();
    }

    /**
     * Unregisters a placeholder with the specified key.
     *
     * @param key The key of the placeholder to unregister.
     */
    public void unregister(final String key) {
        this.placeholders.remove(key);
        this.rebuildCompiledPlaceholders();
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

    private void rebuildCompiledPlaceholders() {
        this.papiPlaceholderKeys.clear();
        this.compiledPlaceholders.clear();
        for (final Map.Entry<String, Placeholder> entry : this.placeholders.entrySet()) {
            this.compile(entry.getValue());
        }

        this.compiledPlaceholders.keySet().forEach(
                s -> {
                    this.papiPlaceholderKeys.add("%itsmyconfig_" + s + "%");
                    this.papiPlaceholderKeys.add("%imc_" + s + "%");
                }
        );
    }

    private void compile(final Placeholder placeholder) {
        for (final CompiledPlaceholder compiledPlaceholder : placeholder.getCompiledPlaceholders()) {
            this.compiledPlaceholders.put(compiledPlaceholder.key(), compiledPlaceholder);
        }
    }

}
