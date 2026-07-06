package to.itsme.itsmyconfig.command.impl.migration;

import java.io.File;

public interface FileHandler {

    MigrationResult migrate(File file, MigrationSession session);

}
