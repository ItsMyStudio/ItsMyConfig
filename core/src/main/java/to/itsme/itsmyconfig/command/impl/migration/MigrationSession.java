package to.itsme.itsmyconfig.command.impl.migration;

import to.itsme.itsmyconfig.util.StringMigrator;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class MigrationSession {

    private final StringMigrator migrator;
    private final File pluginsDir;
    private final FileHandlerRegistry registry;
    private final BackupManager backupManager;

    public MigrationSession(final StringMigrator migrator, final File pluginsDir) {
        this.migrator = migrator;
        this.pluginsDir = pluginsDir;
        this.registry = new FileHandlerRegistry();
        this.backupManager = new BackupManager(String.valueOf(Instant.now().getEpochSecond()));
    }

    public StringMigrator migrator() {
        return migrator;
    }

    /**
     * Backs up the given file before modification.
     * Called by file handlers just before they write.
     */
    public void backup(final File file) throws IOException {
        backupManager.backup(file, pluginsDir);
    }

    /**
     * Resolves {@code target} against the plugins directory,
     * then walks and migrates all matching files.
     *
     * @param target      relative path, e.g. "Essentials/" or "Essentials/messages/en.yml"
     * @param recursively whether to descend into subdirectories
     * @return one result per processed file
     */
    public List<MigrationResult> run(final String target, final boolean recursively) {
        final File resolved = new File(pluginsDir, target);
        final List<MigrationResult> results = new ArrayList<>();

        if (!resolved.exists()) {
            return results;
        }

        if (resolved.isFile()) {
            processFile(resolved, results);
        } else {
            walkDirectory(resolved, recursively, results);
        }

        return results;
    }

    private void walkDirectory(final File dir, final boolean recursively, final List<MigrationResult> results) {
        final File[] children = dir.listFiles();
        if (children == null) return;

        for (final File child : children) {
            if (child.isDirectory()) {
                // never descend into our own backup folders
                if (child.getName().startsWith("itsmyconfig-backup-")) continue;
                if (recursively) walkDirectory(child, true, results);
            } else {
                processFile(child, results);
            }
        }
    }

    private void processFile(final File file, final List<MigrationResult> results) {
        final FileHandler handler = registry.forFile(file);
        if (handler == null) return; // unsupported extension, silently skip
        results.add(handler.migrate(file, this));
    }

}
