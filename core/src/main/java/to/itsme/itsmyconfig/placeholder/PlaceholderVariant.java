package to.itsme.itsmyconfig.placeholder;

import java.util.Locale;
import java.util.Map;

public enum PlaceholderVariant {
    LEGACY,
    CONSOLE,
    MINI,
    RAW;

    private static final Map<String, PlaceholderVariant> LOOKUP = Map.of(
            "legacy", LEGACY, "l", LEGACY, "console", CONSOLE, "c", CONSOLE, "mini", MINI, "m", MINI
    );

    public static PlaceholderVariant find(final String input) {
        if (input == null || input.isEmpty()) return RAW;
        final PlaceholderVariant variant = LOOKUP.get(input.toLowerCase(Locale.ROOT));
        return variant != null ? variant : RAW;
    }

}
