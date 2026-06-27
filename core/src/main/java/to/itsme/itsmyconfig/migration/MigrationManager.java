package to.itsme.itsmyconfig.migration;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.migration.impl.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class MigrationManager {

    private final ItsMyConfig plugin;
    private final List<Migration> migrations = new ArrayList<>();

    public MigrationManager(final ItsMyConfig plugin) {
        this.plugin = plugin;
        this.migrations.add(new Migration_V2_V3());
        this.migrations.add(new Migration_V4_V5());
    }

    public void migrate() {
        final List<File> ymlFiles = collectTargetFiles();

        for (final Migration migration : this.migrations) {
            final String name = migration.getName();
            boolean anyChanged = false;

            for (final File file : ymlFiles) {
                try {
                    final FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    if (migration.migrate(plugin, config, file)) {
                        config.save(file);
                        anyChanged = true;
                    }
                } catch (final IOException e) {
                    plugin.getLogger().warning("[Migration/" + name + "] failed to save " + file.getName());
                }
            }

            if (anyChanged) {
                plugin.getLogger().info("[Migration/" + name + "] completed — files updated");
            } else if (plugin.isDebug()) {
                plugin.getLogger().info("[Migration/" + name + "] completed — no changes");
            }
        }
    }

    private List<File> collectTargetFiles() {
        final List<File> result = new ArrayList<>();

        final File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (configFile.isFile()) {
            result.add(configFile);
        }

        final File placeholdersFolder = plugin.getPlaceholdersFolder();
        if (placeholdersFolder.isDirectory()) {
            final File[] files = placeholdersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (final File file : files) {
                    if (file.isFile()) {
                        result.add(file);
                    }
                }
            }
        }

        return result;
    }

}
