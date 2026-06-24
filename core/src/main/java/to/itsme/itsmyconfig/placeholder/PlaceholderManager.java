package to.itsme.itsmyconfig.placeholder;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
        return placeholders;
    }

    /**
     * Retrieves the keys of all registered placeholders.
     *
     * @return a set containing the keys of all registered placeholders.
     */
    public Set<String> getPlaceholderKeys() {
        return placeholders.keySet();
    }

    private void rebuildCompiledPlaceholders() {
        this.compiledPlaceholders.clear();
        for (final Map.Entry<String, Placeholder> entry : this.placeholders.entrySet()) {
            this.compile(entry.getKey(), entry.getValue());
        }
    }

    private void compile(final String key, final Placeholder placeholder) {
        this.compiledPlaceholders.put(key, new CompiledPlaceholder(placeholder, placeholder::asString));

        switch (placeholder.getType()) {
            case COLOR, COLORED_TEXT:
                this.compileVariant(key, placeholder, "legacy", placeholder::asLegacyString);
                this.compileVariant(key, placeholder, "l", placeholder::asLegacyString);
                this.compileVariant(key, placeholder, "console", placeholder::asConsoleString);
                this.compileVariant(key, placeholder, "c", placeholder::asConsoleString);
                this.compileVariant(key, placeholder, "mini", placeholder::asMiniString);
                this.compileVariant(key, placeholder, "m", placeholder::asMiniString);
                this.compileVariant(key, placeholder, "raw", placeholder::asRawString);
                this.compileVariant(key, placeholder, "r", placeholder::asRawString);
                break;
            case MATH:
                this.compileVariant(key, placeholder, "commas", placeholder::asCommasString);
                this.compileVariant(key, placeholder, "fixed", placeholder::asFixedString);
                this.compileVariant(key, placeholder, "formatted", placeholder::asFormattedString);
                break;
            default:
                break;
        }
    }

    private void compileVariant(
            final String key,
            final Placeholder placeholder,
            final String variant,
            final PlaceholderCaller caller
    ) {
        final String compiledKey = key + "_" + variant;
        if (!this.compiledPlaceholders.containsKey(compiledKey)) {
            this.compiledPlaceholders.put(compiledKey, new CompiledPlaceholder(placeholder, caller));
        }
    }
}
