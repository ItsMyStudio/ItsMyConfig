package to.itsme.itsmyconfig.command.impl.migration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

public final class BackupManager {

    private final String timestamp;
    private final Set<String> backedUpFiles = new HashSet<>();

    public BackupManager(final String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Copies {@code file} into the backup folder of its plugin root,
     * preserving relative structure. No-ops if already backed up this session.
     *
     * @param file       the file to back up
     * @param pluginsDir the plugins/ directory (base for resolving plugin root)
     */
    public void backup(final File file, final File pluginsDir) throws IOException {
        final String absolutePath = file.getAbsolutePath();
        if (backedUpFiles.contains(absolutePath)) {
            return;
        }

        final File pluginRoot = resolvePluginRoot(file, pluginsDir);
        if (pluginRoot == null) {
            // file is not inside any known plugin folder — back up next to itself
            copyToBackup(file, pluginsDir, pluginsDir);
        } else {
            copyToBackup(file, pluginRoot, pluginsDir);
        }

        backedUpFiles.add(absolutePath);
    }

    private void copyToBackup(final File file, final File pluginRoot, final File pluginsDir) throws IOException {
        final File backupRoot = new File(pluginRoot, "itsmyconfig-backup-" + timestamp);

        // Preserve relative path within the plugin root
        final String relative = pluginRoot.toURI().relativize(file.toURI()).getPath();
        final File dest = new File(backupRoot, relative);

        dest.getParentFile().mkdirs();
        Files.copy(file.toPath(), dest.toPath());
    }

    /**
     * Returns the direct child of pluginsDir that contains this file,
     * or null if the file is directly inside pluginsDir.
     */
    private File resolvePluginRoot(final File file, final File pluginsDir) {
        File current = file.getParentFile();
        File child = file;
        while (current != null) {
            if (current.equals(pluginsDir)) {
                // child is the plugin root folder (e.g. plugins/Essentials/)
                return child.isDirectory() ? child : null;
            }
            child = current;
            current = current.getParentFile();
        }
        return null;
    }

}
