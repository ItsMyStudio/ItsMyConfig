package to.itsme.itsmyconfig.migration.impl;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.route.Route;
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
    public boolean migrate(final ItsMyConfig plugin, final YamlDocument config, final File file) {
        if (!isMainConfig(plugin, file)) {
            return false;
        }

        if (!config.isSection("custom-placeholder")) {
            return false;
        }

        boolean modified = false;
        final Section configSection = config.getSection("custom-placeholder");
        assert configSection != null;
        for (final Object key : new ArrayList<>(configSection.getKeys())) {
            if (!configSection.isSection(Route.fromSingleKey(key))) {
                continue;
            }

            final Section section = configSection.getSection(Route.fromSingleKey(key));
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
                    section.remove("mode");
                    modified = true;
                }
            }

            if (section.isSection("requirements")) {
                final Section requirementsSection = section.getSection("requirements");
                assert requirementsSection != null;

                final List<Map<String, Object>> conditions = new ArrayList<>();
                for (final Object requirementKey : new ArrayList<>(requirementsSection.getKeys())) {
                    if (!requirementsSection.isSection(Route.fromSingleKey(requirementKey))) {
                        continue;
                    }

                    final Section requirementSection = requirementsSection.getSection(Route.fromSingleKey(requirementKey));
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
                    section.remove("value");
                    section.set("false", "");
                    section.set("conditions", conditions);
                    section.remove("requirements");
                } else {
                    // rename original to a new UUID-based name, conditional takes the original name
                    final String shortId = UUID.randomUUID().toString().substring(0, 8);
                    final String newKey = key + "-" + shortId;

                    // copy all values from original section to new key
                    final Section newSection = configSection.createSection(newKey);
                    // copy of the key set — set() moves nested sections out of the one we iterate
                    for (final Object sectionKey : new ArrayList<>(section.getKeys())) {
                        if (sectionKey.equals("requirements")) continue;
                        newSection.set(Route.fromSingleKey(sectionKey), section.get(Route.fromSingleKey(sectionKey)));
                    }

                    // clear ALL existing keys before writing conditional fields
                    for (final Object sectionKey : new ArrayList<>(section.getKeys())) {
                        section.remove(Route.fromSingleKey(sectionKey));
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
