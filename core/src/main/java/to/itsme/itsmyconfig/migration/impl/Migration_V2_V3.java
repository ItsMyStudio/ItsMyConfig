package to.itsme.itsmyconfig.migration.impl;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.route.Route;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.config.IMConfig;
import to.itsme.itsmyconfig.migration.Migration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public final class Migration_V2_V3 extends Migration {

    @Override
    public String getName() {
        return "V2_V3";
    }

    @Override
    public boolean migrate(final ItsMyConfig plugin, final YamlDocument config, final File file) {
        if (!isMainConfig(plugin, file)) {
            return false;
        }

        boolean changed = false;
        if (!config.isSection("listeners")) {
            final Section listeners = config.createSection("listeners");
            final Section packetEvents = listeners.createSection("PacketEvents");
            packetEvents.set("priority", 1);

            final Section protocolLib = listeners.createSection("ProtocolLib");
            protocolLib.set("priority", 2);
            protocolLib.set("cache-processors", false);

            if (!config.contains("config-version") || config.getInt("config-version") < 2) {
                config.set("config-version", 2);
            }

            changed = true;
        }

        final boolean hasCustomPlaceholder = config.isSection("custom-placeholder");
        final boolean hasCustomProgress = config.isSection("custom-progress");

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

                final IMConfig migratedConf = new IMConfig(migratedConfig, null, false);
                final Section newSection = migratedConf.createSection("custom-placeholder");
                if (hasCustomPlaceholder) {
                    final Section placeholderSection = config.getSection("custom-placeholder");
                    // copy of the key set — set() moves sections out of the one we iterate
                    for (final Object name : new ArrayList<>(placeholderSection.getKeys())) {
                        newSection.set(Route.fromSingleKey(name), placeholderSection.get(Route.fromSingleKey(name)));
                    }
                }

                if (hasCustomProgress) {
                    final Section progressSection = config.getSection("custom-progress");
                    for (final Object name : new ArrayList<>(progressSection.getKeys())) {
                        final Section section = progressSection.getSection(Route.fromSingleKey(name));
                        if (section == null) continue;
                        section.set("value", section.getString("symbol"));
                        section.set("type", "progress_bar");
                        section.remove("symbol");
                        newSection.set(Route.fromSingleKey(name), section);
                    }
                }

                migratedConf.save("Failed to save " + migratedConfig.getAbsolutePath());
                config.remove("custom-progress");
                config.remove("custom-placeholder");
                changed = true;
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }

        return changed;
    }

}
