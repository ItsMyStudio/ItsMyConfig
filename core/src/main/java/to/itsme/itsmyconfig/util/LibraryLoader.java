package to.itsme.itsmyconfig.util;

import com.alessiodp.libby.BukkitLibraryManager;
import com.alessiodp.libby.Library;
import com.alessiodp.libby.LibraryManager;
import com.alessiodp.libby.relocation.Relocation;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.util.reflect.Reflections;

import java.util.function.Supplier;
import java.util.logging.Level;

public enum LibraryLoader {

    /* ========================================================= //
    // Adventure Components & MiniMessage //
    ADVENTURE(
            "net.kyori",
            "adventure-text-minimessage",
            BuildParameters.ADVENTURE_VERSION,
            () -> true,
            new Relocation("net.kyori", BuildParameters.SHADE_PATH + "kyori")
    ),
    // ========================================================= //
    // Adventure Serializers //
    ADVENTURE_BUNGEE_SERIALIZER(
            "net.kyori",
            "adventure-text-serializer-bungeecord",
            BuildParameters.ADVENTURE_PLATFORM_VERSION,
            () -> true,
            new Relocation("net.kyori", BuildParameters.SHADE_PATH + "kyori")
    )
    // ========================================================= */
    // Adventure Bukkit Platfrom //
    @SuppressWarnings("ConstantConditions")
    ADVENTURE_PLATFORM(
            "net{}kyori",
            "adventure-platform-bukkit",
            BuildParameters.ADVENTURE_PLATFORM_VERSION,
            () -> !Reflections.findClass("net{}kyori{}adventure{}platform.bukkit.BukkitComponentSerializer")
    ),
    ADVENTURE_GSON_SERIALIZER(
            "net{}kyori",
            "adventure-text-serializer-gson",
            BuildParameters.ADVENTURE_VERSION,
            () -> !Reflections.findClass("net{}kyori{}adventure{}text.serializer.gson.GsonSerializer")
    );

    private static final LibraryManager MANAGER = new BukkitLibraryManager(ItsMyConfig.getInstance());

    static {
        MANAGER.addMavenCentral();
    }

    private final Library library;
    private final boolean shouldLoad;

    LibraryLoader(
            final String groupID,
            final String artifactID,
            final String version,
            final Supplier<Boolean> load,
            final Relocation... relocations
    ) {
        this(groupID, artifactID, version, null, load, relocations);
    }

    LibraryLoader(
            final String groupID,
            final String artifactID,
            final String version,
            final String repo,
            final Supplier<Boolean> load,
            final Relocation... relocations
    ) {
        final Library.Builder builder = Library.builder()
                .groupId(groupID)
                .artifactId(artifactID)
                .version(version)
                .resolveTransitiveDependencies(true);

        if (repo != null) {
            builder.repository(repo);
        }

        for (final Relocation relocation : relocations) {
            builder.relocate(relocation);
        }

        this.library = builder.build();
        this.shouldLoad = this.canLoad(load);
    }

    public static void loadLibraries() {
        for (final LibraryLoader value : values()) {
            if (value.shouldLoad) {
                ItsMyConfig.getInstance().getLogger().info("Loading library " + value.library.getArtifactId() + "...");
                MANAGER.loadLibrary(value.library);
            } else {
                ItsMyConfig.getInstance().getLogger().info("Library " + value.library.getArtifactId() + " is not needed.");
            }
        }
    }

    public boolean shouldLoad() {
        return shouldLoad;
    }

    private boolean canLoad(final Supplier<Boolean> load) {
        try {
            return load.get();
        } catch (final Throwable err) {
            ItsMyConfig.getInstance().getLogger().log(Level.WARNING, "Failed to check load library " + this.library.getArtifactId() + ", loading anyway...");
            return true;
        }
    }

}
