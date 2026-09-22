package dev.velix.imperat;

import dev.velix.imperat.util.reflection.Reflections;
import org.bukkit.Bukkit;

public final class Version {

    public static final String VERSION_EXACT;
    public static final boolean IS_FOLIA = Reflections.findClass("io.papermc.paper.threadedregions.RegionizedServer");
    public static final boolean IS_PAPER = Reflections.findClass("com.destroystokyo.paper.PaperConfig", "io.papermc.paper.configuration.Configuration", "io.papermc.paper.plugin.configuration.PluginMeta");

    public static final int MAJOR, MINOR, PATCH;

    static {
        String versionStr = "";
        try {
            if (Bukkit.getServer() != null) {
                try {
                    final Object mcVer = Bukkit.getServer().getClass().getMethod("getMinecraftVersion").invoke(Bukkit.getServer());
                    if (mcVer instanceof String s && !s.isEmpty()) {
                        versionStr = s;
                    }
                } catch (final Throwable ignored) {
                }

                if (versionStr.isEmpty()) {
                    versionStr = Bukkit.getBukkitVersion().split("-")[0];
                }
            }
        } catch (final Throwable ignored) {
        }
        if (versionStr.isEmpty()) {
            versionStr = "1.20.4";
        }

        VERSION_EXACT = versionStr;

        final String[] versions = VERSION_EXACT.split("\\.");
        int maj = 1, min = 16, pat = 5;
        if (versions.length > 0) {
            maj = parseLeadingInt(versions[0], 1);
        }
        if (versions.length > 1) {
            min = parseLeadingInt(versions[1], 16);
        }
        if (versions.length > 2) {
            pat = parseLeadingInt(versions[2], 0);
        }
        MAJOR = maj;
        MINOR = min;
        PATCH = pat;
    }

    private static int parseLeadingInt(final String text, final int def) {
        if (text == null || text.isEmpty()) return def;
        final StringBuilder sb = new StringBuilder();
        for (final char c : text.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            } else if (sb.length() > 0) {
                break;
            }
        }
        if (sb.length() == 0) return def;
        try {
            return Integer.parseInt(sb.toString());
        } catch (final Throwable ignored) {
            return def;
        }
    }

    // initialize after IS_PAPER is initialized
    public static final String NMS = findVersion();
    public static final boolean IS_13_R2_PLUS = isOrOver(1, 13, 2);
    public static final boolean IS_20_R2_PLUS = isOrOver(1, 20, 2);
    public static final boolean IS_20_R4_PLUS = isOrOver(1, 20, 5);

    public static boolean is(final int minor) {
        return (MAJOR == 1 && MINOR == minor) || (MAJOR > 1);
    }

    public static boolean is(final int major, final int minor, final int patch) {
        return MAJOR == major && MINOR == minor && PATCH == patch;
    }

    public static boolean isOver(final int minor) {
        return MAJOR > 1 || MINOR > minor;
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
        return MAJOR > 1 || MINOR >= minor;
    }

    public static boolean isOrOver(final int major, final int minor, final int patch) {
        return is(major, minor, patch) || isOver(major, minor, patch);
    }

    public static boolean isBelow(final int minor) {
        return MAJOR == 1 && MINOR < minor;
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
        return MAJOR == 1 && MINOR <= minor;
    }

    public static boolean isOrBelow(final int major, final int minor, final int patch) {
        return is(major, minor, patch) || isBelow(major, minor, patch);
    }

    private static String findVersion() {
        try {
            if (IS_PAPER && (MAJOR > 1 || MINOR >= 20)) {
                return switch (VERSION_EXACT) {
                    case "1.20", "1.20.1" -> "1_20_R1";
                    case "1.20.2", "1.20.3" -> "1_20_R2";
                    case "1.20.4" -> "1_20_R3";
                    case "1.20.5", "1.20.6" -> "1_20_R4";
                    case "1.21" -> "1_21_R1";
                    default -> "UNKNOWN";
                };
            }
            final String pkgName = Bukkit.getServer().getClass().getPackage().getName();
            return pkgName.length() >= 24 ? pkgName.substring(24) : "UNKNOWN";
        } catch (final Throwable ignored) {
            return "UNKNOWN";
        }
    }

}
