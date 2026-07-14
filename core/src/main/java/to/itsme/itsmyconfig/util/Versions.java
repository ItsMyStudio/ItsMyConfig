package to.itsme.itsmyconfig.util;

import org.bukkit.Bukkit;
import to.itsme.itsmyconfig.util.reflect.Reflections;

@SuppressWarnings("unused")
public final class Versions {

    /**
     * Numeric game/API version only (e.g. {@code 1.21.4} or {@code 26.2}).
     * Non-numeric segments such as {@code build} from Paper/Leaf 26.x are stripped.
     */
    public static final String VERSION_EXACT;
    public static final boolean IS_FOLIA = Reflections.findClass("io.papermc.paper.threadedregions.RegionizedServer");
    public static final boolean IS_PAPER = Reflections.findClass("com.destroystokyo.paper.PaperConfig", "io.papermc.paper.configuration.Configuration");

    public static final int MAJOR, MINOR, PATCH;

    static {
        // Guard for unit tests / early class-load without a live Bukkit server.
        String exact = "0.0.0";
        int major = 0;
        int minor = 0;
        int patch = 0;
        try {
            exact = resolveVersionExact(Bukkit.getBukkitVersion());
            final int[] parts = parseVersionParts(exact);
            major = parts[0];
            minor = parts[1];
            patch = parts[2];
        } catch (final Throwable ignored) {
            // leave defaults (0.0.0)
        }
        VERSION_EXACT = exact;
        MAJOR = major;
        MINOR = minor;
        PATCH = patch;
    }

    private Versions() {
    }

    /**
     * Normalizes a Bukkit version string to leading numeric segments only.
     * <ul>
     *   <li>{@code 1.21.4-R0.1-SNAPSHOT} → {@code 1.21.4}</li>
     *   <li>{@code 26.2.build.24-alpha} → {@code 26.2}</li>
     *   <li>{@code 26.1.2} → {@code 26.1.2}</li>
     * </ul>
     */
    static String resolveVersionExact(final String bukkitVersion) {
        if (bukkitVersion == null || bukkitVersion.isEmpty()) {
            return "0.0.0";
        }

        // Drop classifiers: -R0.1-SNAPSHOT, -alpha, etc.
        final String beforeHyphen = bukkitVersion.split("-", 2)[0];
        final String[] segments = beforeHyphen.split("\\.");
        final StringBuilder numeric = new StringBuilder();

        for (final String segment : segments) {
            if (!isNumeric(segment)) {
                // Stop at first non-numeric token (e.g. "build" in 26.2.build.24)
                break;
            }
            if (numeric.length() > 0) {
                numeric.append('.');
            }
            numeric.append(segment);
        }

        return numeric.length() == 0 ? "0.0.0" : numeric.toString();
    }

    static int[] parseVersionParts(final String versionExact) {
        final String[] versions = versionExact.split("\\.");
        final int major = versions.length > 0 ? parseIntOrZero(versions[0]) : 0;
        final int minor = versions.length > 1 ? parseIntOrZero(versions[1]) : 0;
        final int patch = versions.length > 2 ? parseIntOrZero(versions[2]) : 0;
        return new int[]{major, minor, patch};
    }

    private static boolean isNumeric(final String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static int parseIntOrZero(final String value) {
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    public static boolean is(final int major, final int minor, final int patch) {
        return MAJOR == major && MINOR == minor && PATCH == patch;
    }

    public static boolean isOver(final int major, final int minor, final int patch) {
        if (MAJOR > major) {
            return true;
        } else if (MAJOR == major) {
            if (MINOR > minor) {
                return true;
            } else if (MINOR == minor) {
                return PATCH > patch;
            }
        }
        return false;
    }

    public static boolean isOrOver(final int major, final int minor, final int patch) {
        return is(major, minor, patch) || isOver(major, minor, patch);
    }

    public static boolean isBelow(final int major, final int minor, final int patch) {
        if (MAJOR < major) {
            return true;
        } else if (MAJOR == major) {
            if (MINOR < minor) {
                return true;
            } else if (MINOR == minor) {
                return PATCH < patch;
            }
        }
        return false;
    }

    public static boolean isOrBelow(final int major, final int minor, final int patch) {
        return is(major, minor, patch) || isBelow(major, minor, patch);
    }

}
