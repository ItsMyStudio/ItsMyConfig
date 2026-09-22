package to.itsme.itsmyconfig.command.impl.migration.file;

import to.itsme.itsmyconfig.command.impl.migration.FileHandler;
import to.itsme.itsmyconfig.command.impl.migration.MigrationResult;
import to.itsme.itsmyconfig.command.impl.migration.MigrationSession;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class PropertiesFileHandler implements FileHandler {

    @Override
    public MigrationResult migrate(final File file, final MigrationSession session) {
        final List<String> lines;
        try {
            lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (final Exception e) {
            return MigrationResult.skipped(file, "failed to read: " + e.getMessage());
        }

        int changed = 0;
        final List<String> output = new ArrayList<>(lines.size());

        for (final String line : lines) {
            // preserve blank lines and comments (# or !)
            final String trimmed = line.stripLeading();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                output.add(line);
                continue;
            }

            final int eq = line.indexOf('=');
            if (eq == -1) {
                // no = sign, treat whole line as plain string
                final String migrated = session.migrator().migrate(line);
                if (!migrated.equals(line)) changed++;
                output.add(migrated);
                continue;
            }

            final String key = line.substring(0, eq);
            final String value = line.substring(eq + 1);
            final String migratedValue = session.migrator().migrate(value);

            if (!migratedValue.equals(value)) changed++;
            output.add(key + "=" + migratedValue);
        }

        if (changed == 0) {
            return MigrationResult.unchanged(file);
        }

        try {
            session.backup(file);
            Files.write(file.toPath(), output, StandardCharsets.UTF_8);
        } catch (final Exception e) {
            return MigrationResult.skipped(file, "failed to save: " + e.getMessage());
        }

        return MigrationResult.changed(file, changed);
    }

}
