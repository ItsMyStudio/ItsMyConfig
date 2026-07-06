package to.itsme.itsmyconfig.migration.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.migration.Migration;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;

import java.io.File;
import java.util.*;

public final class Migration_V4_V5 extends Migration {

    @Override
    public String getName() {
        return "V4_V5";
    }

    @Override
    public boolean migrate(final ItsMyConfig plugin, final FileConfiguration config, final File file) {
        if (!isMainConfig(plugin, file)) {
            return false;
        }

        if (!config.isConfigurationSection("custom-placeholder")) {
            return false;
        }

        boolean modified = false;
        final ConfigurationSection configSection = config.getConfigurationSection("custom-placeholder");
        assert configSection != null;
        for (final String key : configSection.getKeys(false)) {
            if (!configSection.isConfigurationSection(key)) {
                continue;
            }

            final ConfigurationSection section = configSection.getConfigurationSection(key);
            assert section != null;

            String typeString = section.getString("type");
            if (typeString == null) {
                continue;
            }

            if (typeString.equalsIgnoreCase("MAP") || typeString.equalsIgnoreCase("RANGE")) {
                section.set("type", "SWITCH");
                typeString = "SWITCH";
                modified = true;
            }

            final PlaceholderType type = PlaceholderType.findOrElse(typeString, null);
            if (type == null) continue;

            if (PlaceholderType.MATH == type) {
                if (section.contains("mode")) {
                    section.set("rounding", section.getString("mode"));
                    section.set("mode", null);
                    modified = true;
                }
            }

            if (section.isConfigurationSection("requirements")) {
                final ConfigurationSection requirementsSection = section.getConfigurationSection("requirements");
                assert requirementsSection != null;

                final List<Map<String, Object>> conditions = new ArrayList<>();
                for (final String requirementKey : requirementsSection.getKeys(false)) {
                    if (!requirementsSection.isConfigurationSection(requirementKey)) {
                        continue;
                    }

                    final ConfigurationSection requirementSection = requirementsSection.getConfigurationSection(requirementKey);
                    assert requirementSection != null;

                    final Map<String, Object> condition = new LinkedHashMap<>();
                    condition.put("type", requirementSection.getString("type"));
                    condition.put("input", requirementSection.getString("input"));
                    condition.put("output", requirementSection.getString("output"));
                    condition.put("false", requirementSection.getString("deny", ""));
                    conditions.add(condition);
                }

                if (PlaceholderType.STRING == type) {
                    // in-place conversion
                    section.set("type", "conditional");
                    section.set("true", section.getString("value"));
                    section.set("value", null);
                    section.set("false", "");
                    section.set("conditions", conditions);
                    section.set("requirements", null);
                } else {
                    // rename original to a new UUID-based name, conditional takes the original name
                    final String shortId = UUID.randomUUID().toString().substring(0, 8);
                    final String newKey = key + "-" + shortId;

                    // copy all values from original section to new key
                    final ConfigurationSection newSection = configSection.createSection(newKey);
                    for (final String sectionKey : section.getKeys(false)) {
                        if (sectionKey.equals("requirements")) continue;
                        newSection.set(sectionKey, section.get(sectionKey));
                    }

                    // clear ALL existing keys before writing conditional fields
                    for (final String sectionKey : new ArrayList<>(section.getKeys(false))) {
                        section.set(sectionKey, null);
                    }

                    // overwrite original with conditional
                    section.set("type", "conditional");
                    section.set("true", "%itsmyconfig_" + newKey + "%");
                    section.set("false", "");
                    section.set("conditions", conditions);
                }

                modified = true;
            }

        }

        return modified;
    }

}
