package to.itsme.itsmyconfig.migration.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.migration.Migration;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public final class Migration_V2_V3 extends Migration {

    @Override
    public String getName() {
        return "V2_V3";
    }

    @Override
    public boolean migrate(final ItsMyConfig plugin, final FileConfiguration config, final File file) {
        if (!isMainConfig(plugin, file)) {
            return false;
        }

        boolean changed = false;
        if (!config.isConfigurationSection("listeners")) {
            final ConfigurationSection listeners = config.createSection("listeners");
            final ConfigurationSection packetEvents = listeners.createSection("PacketEvents");
            packetEvents.set("priority", 1);

            final ConfigurationSection protocolLib = listeners.createSection("ProtocolLib");
            protocolLib.set("priority", 2);
            protocolLib.set("cache-processors", false);

            if (!config.contains("config-version") || config.getInt("config-version") < 2) {
                config.set("config-version", 2);
            }

            changed = true;
        }

        final boolean hasCustomPlaceholder = config.isConfigurationSection("custom-placeholder");
        final boolean hasCustomProgress = config.isConfigurationSection("custom-progress");

        if (hasCustomPlaceholder || hasCustomProgress) {
            final File directory = plugin.getPlaceholdersFolder();
            File migratedConfig = new File(directory, "migrated-config.yml");
            if (migratedConfig.exists()) {
                migratedConfig = new File(directory, UUID.randomUUID() + ".yml");
            }
            try {
                final boolean created = migratedConfig.createNewFile();
                if (!created) {
                    return changed;
                }

                final YamlConfiguration migratedConf = YamlConfiguration.loadConfiguration(migratedConfig);
                final ConfigurationSection newSection = migratedConf.createSection("custom-placeholder");
                if (hasCustomPlaceholder) {
                    for (final String name : Objects.requireNonNull(config.getConfigurationSection("custom-placeholder")).getKeys(false)) {
                        newSection.set(name, config.get("custom-placeholder." + name));
                    }
                }

                if (hasCustomProgress) {
                    for (final String name : Objects.requireNonNull(config.getConfigurationSection("custom-progress")).getKeys(false)) {
                        final ConfigurationSection section = config.getConfigurationSection("custom-progress." + name);
                        if (section == null) continue;
                        section.set("value", section.getString("symbol"));
                        section.set("type", "progress_bar");
                        section.set("symbol", null);
                        newSection.set(name, section);
                    }
                }

                migratedConf.save(migratedConfig);
                config.set("custom-progress", null);
                config.set("custom-placeholder", null);
                changed = true;
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }

        return changed;
    }

}
