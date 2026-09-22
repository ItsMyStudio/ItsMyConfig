package to.itsme.itsmyconfig.util;

import org.bukkit.Bukkit;
import to.itsme.itsmyconfig.util.reflect.Reflections;

@SuppressWarnings("unused")
public final class Versions {

<<<<<<< HEAD
    public static final String VERSION_EXACT;
=======
    public static final String VERSION_EXACT = Bukkit.getBukkitVersion()
            .split("-")[0]
            .replaceAll("\\.build\\.\\d+", "");
>>>>>>> 39afa1f44463bd1462b2497b6820e2ee86437eb5
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
            maj = Strings.intOrDefault(versions[0], 1);
        }
        if (versions.length > 1) {
            min = Strings.intOrDefault(versions[1], 16);
        }
        if (versions.length > 2) {
            pat = Strings.intOrDefault(versions[2], 0);
        }
        MAJOR = maj;
        MINOR = min;
        PATCH = pat;
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
