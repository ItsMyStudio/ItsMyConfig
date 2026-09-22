package to.itsme.itsmyconfig.command.impl.migration.file;

import to.itsme.itsmyconfig.command.impl.migration.FileHandler;
import to.itsme.itsmyconfig.command.impl.migration.MigrationResult;
import to.itsme.itsmyconfig.command.impl.migration.MigrationSession;
import to.itsme.itsmyconfig.config.IMConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class YamlFileHandler implements FileHandler {

    @Override
    public MigrationResult migrate(final File file, final MigrationSession session) {
        final IMConfig config;
        try {
            config = new IMConfig(file, null, false);
        } catch (final Exception e) {
            return MigrationResult.skipped(file, "malformed YAML: " + e.getMessage());
        }

        int changed = 0;

        for (final String key : config.getRoutesAsStrings(true)) {
            final Object value = config.get(key);

            if (value instanceof String s) {
                final String migrated = session.migrator().migrate(s);
                if (!migrated.equals(s)) {
                    config.set(key, migrated);
                    changed++;
                }
            } else if (value instanceof List<?> list) {
                final List<Object> migrated = new ArrayList<>(list.size());
                boolean listChanged = false;
                for (final Object element : list) {
                    if (element instanceof String s) {
                        final String migratedStr = session.migrator().migrate(s);
                        migrated.add(migratedStr);
                        if (!migratedStr.equals(s)) {
                            listChanged = true;
                            changed++;
                        }
                    } else {
                        migrated.add(element);
                    }
                }
                if (listChanged) {
                    config.set(key, migrated);
                }
            }
        }

        if (changed == 0) {
            return MigrationResult.unchanged(file);
        }

        try {
            session.backup(file);
            config.save("Failed to save migrated file " + file.getAbsolutePath());
        } catch (final Exception e) {
            return MigrationResult.skipped(file, "failed to save: " + e.getMessage());
        }

        return MigrationResult.changed(file, changed);
    }

}
