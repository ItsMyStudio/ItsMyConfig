package to.itsme.itsmyconfig.migration;

import dev.dejvokep.boostedyaml.YamlDocument;
import to.itsme.itsmyconfig.ItsMyConfig;

import java.io.File;

public abstract class Migration {

    public abstract String getName();

    public abstract boolean migrate(ItsMyConfig plugin, YamlDocument config, File file);

    public boolean isMainConfig(final ItsMyConfig plugin, final File file) {
        return file.equals(new File(plugin.getDataFolder(), "config.yml"));
    }

}
