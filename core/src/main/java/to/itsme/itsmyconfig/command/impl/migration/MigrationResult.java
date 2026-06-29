package to.itsme.itsmyconfig.command.impl.migration;

import java.io.File;

public record MigrationResult(
        File file,
        int stringsChanged,
        boolean skipped,
        String skipReason
) {

    public static MigrationResult changed(final File file, final int count) {
        return new MigrationResult(file, count, false, null);
    }

    public static MigrationResult unchanged(final File file) {
        return new MigrationResult(file, 0, false, null);
    }

    public static MigrationResult skipped(final File file, final String reason) {
        return new MigrationResult(file, 0, true, reason);
    }

}
