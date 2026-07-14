package dev.velix.imperat;

import dev.velix.imperat.util.reflection.Reflections;
import org.bukkit.Bukkit;

/**
 * Drop-in replacement for Imperat's {@code Version} that understands Paper/Leaf 26.x
 * version strings such as {@code 26.2.build.24-alpha}.
 *
 * <p>Upstream Imperat 1.9.x parses {@code Bukkit.getBukkitVersion().split("-")[0]} and
 * fails with {@link NumberFormatException} on the non-numeric {@code build} segment.
 * Minor-only helpers ({@code isOrOver(13)}, etc.) also mis-detect calendar versions
 * where {@code MINOR} is small (e.g. 26.2 → MINOR=2).</p>
 *
 * <p>This class is compiled into the plugin and relocated with Imperat, replacing the
 * dependency copy in the shaded jar.</p>
 */
public final class Version {

    public static final String VERSION_EXACT;
    public static final boolean IS_FOLIA = Reflections.findClass("io.papermc.paper.threadedregions.RegionizedServer");
    public static final boolean IS_PAPER = Reflections.findClass(
            "com.destroystokyo.paper.PaperConfig",
            "io.papermc.paper.configuration.Configuration"
    );

    public static final int MAJOR, MINOR, PATCH;

    static {
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
            // leave defaults when Bukkit is unavailable (unit tests)
        }
        VERSION_EXACT = exact;
        MAJOR = major;
        MINOR = minor;
        PATCH = patch;
    }

    // initialize after MAJOR/MINOR/PATCH
    public static final String NMS = findVersion();
    public static final boolean IS_13_R2_PLUS = isOrOver(1, 13, 2);
    public static final boolean IS_20_R2_PLUS = isOrOver(1, 20, 2);
    public static final boolean IS_20_R4_PLUS = isOrOver(1, 20, 5);

    private Version() {
    }

    /**
     * {@code 1.21.4-R0.1-SNAPSHOT} → {@code 1.21.4}; {@code 26.2.build.24-alpha} → {@code 26.2}
     */
    static String resolveVersionExact(final String bukkitVersion) {
        if (bukkitVersion == null || bukkitVersion.isEmpty()) {
            return "0.0.0";
        }
        final String beforeHyphen = bukkitVersion.split("-", 2)[0];
        final String[] segments = beforeHyphen.split("\\.");
        final StringBuilder numeric = new StringBuilder();
        for (final String segment : segments) {
            if (!isNumeric(segment)) {
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

    /**
     * Calendar versions (26.x) and any major &gt; 1 are newer than the entire 1.x line.
     */
    private static boolean isPostLegacyMinecraftLine() {
        return MAJOR > 1;
    }

    public static boolean is(final int minor) {
        if (isPostLegacyMinecraftLine()) {
            return false;
        }
        return MINOR == minor;
    }

    public static boolean is(final int major, final int minor, final int patch) {
        return MAJOR == major && MINOR == minor && PATCH == patch;
    }

    public static boolean isOver(final int minor) {
        if (isPostLegacyMinecraftLine()) {
            return true;
        }
        return MINOR > minor;
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

    public static boolean isOrOver(final int minor) {
        if (isPostLegacyMinecraftLine()) {
            return true;
        }
        return MINOR >= minor;
    }

    public static boolean isOrOver(final int major, final int minor, final int patch) {
        return is(major, minor, patch) || isOver(major, minor, patch);
    }

    public static boolean isBelow(final int minor) {
        if (isPostLegacyMinecraftLine()) {
            return false;
        }
        return MINOR < minor;
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

    public static boolean isOrBelow(final int minor) {
        if (isPostLegacyMinecraftLine()) {
            return false;
        }
        return MINOR <= minor;
    }

    public static boolean isOrBelow(final int major, final int minor, final int patch) {
        return is(major, minor, patch) || isBelow(major, minor, patch);
    }

    private static String findVersion() {
        // Paper 26.x has MINOR=2; treat any post-1.x Paper as modern for NMS mapping
        if (IS_PAPER && (isPostLegacyMinecraftLine() || MINOR >= 20)) {
            return switch (VERSION_EXACT) {
                case "1.20", "1.20.1" -> "1_20_R1";
                case "1.20.2", "1.20.3" -> "1_20_R2";
                case "1.20.4" -> "1_20_R3";
                case "1.20.5", "1.20.6" -> "1_20_R4";
                case "1.21", "1.21.1" -> "1_21_R1";
                case "1.21.2", "1.21.3" -> "1_21_R2";
                case "1.21.4" -> "1_21_R3";
                case "1.21.5" -> "1_21_R4";
                case "1.21.6", "1.21.7", "1.21.8" -> "1_21_R5";
                default -> "UNKNOWN";
            };
        }
        try {
            return Bukkit.getServer().getClass().getPackage().getName().substring(24);
        } catch (final Throwable t) {
            return "UNKNOWN";
        }
    }

}
