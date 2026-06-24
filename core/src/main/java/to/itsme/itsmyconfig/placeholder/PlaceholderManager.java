package to.itsme.itsmyconfig.placeholder;

import to.itsme.itsmyconfig.placeholder.type.MathPlaceholder;

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
        this.compiledPlaceholders.put(key, new CompiledPlaceholder(
                placeholder,
                placeholder::asString,
                this.minArguments(placeholder),
                this.maxArguments(placeholder)
        ));

        switch (placeholder.getType()) {
            case COLOR:
                this.compileVariant(key, placeholder, "legacy", placeholder::asLegacyString, 0, 0);
                this.compileVariant(key, placeholder, "l", placeholder::asLegacyString, 0, 0);
                this.compileVariant(key, placeholder, "console", placeholder::asConsoleString, 0, 0);
                this.compileVariant(key, placeholder, "c", placeholder::asConsoleString, 0, 0);
                this.compileVariant(key, placeholder, "mini", placeholder::asMiniString, 0, 1);
                this.compileVariant(key, placeholder, "m", placeholder::asMiniString, 0, 1);
                break;
            case COLORED_TEXT:
                this.compileVariant(key, placeholder, "legacy", placeholder::asLegacyString, 0, -1);
                this.compileVariant(key, placeholder, "l", placeholder::asLegacyString, 0, -1);
                this.compileVariant(key, placeholder, "console", placeholder::asConsoleString, 0, -1);
                this.compileVariant(key, placeholder, "c", placeholder::asConsoleString, 0, -1);
                this.compileVariant(key, placeholder, "mini", placeholder::asMiniString, 0, -1);
                this.compileVariant(key, placeholder, "m", placeholder::asMiniString, 0, -1);
                break;
            case MATH:
                final int required = this.minArguments(placeholder);
                this.compileVariant(key, placeholder, "commas", placeholder::asCommasString, required, required);
                this.compileVariant(key, placeholder, "fixed", placeholder::asFixedString, required, required);
                this.compileVariant(key, placeholder, "formatted", placeholder::asFormattedString, required, required);
                break;
            default:
                break;
        }
    }

    private int minArguments(final Placeholder placeholder) {
        return switch (placeholder.getType()) {
            case LIST, MAP, RANGE -> 1;
            case PROGRESS_BAR -> 2;
            case MATH -> placeholder instanceof MathPlaceholder mathPlaceholder ? mathPlaceholder.variablesRequired() : 0;
            default -> 0;
        };
    }

    private int maxArguments(final Placeholder placeholder) {
        return switch (placeholder.getType()) {
            case COLOR -> 0;
            case PROGRESS_BAR -> 2;
            case MATH -> placeholder instanceof MathPlaceholder mathPlaceholder ? mathPlaceholder.variablesRequired() : -1;
            default -> -1;
        };
    }

    private void compileVariant(
            final String key,
            final Placeholder placeholder,
            final String variant,
            final PlaceholderCaller caller,
            final int minArguments,
            final int maxArguments
    ) {
        final String compiledKey = key + "_" + variant;
        if (!this.compiledPlaceholders.containsKey(compiledKey)) {
            this.compiledPlaceholders.put(compiledKey, new CompiledPlaceholder(
                    placeholder,
                    caller,
                    minArguments,
                    maxArguments
            ));
        }
    }

}
