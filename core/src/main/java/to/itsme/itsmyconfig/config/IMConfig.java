package to.itsme.itsmyconfig.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.common.ScalarStyle;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import dev.dejvokep.boostedyaml.utils.format.Formatter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class IMConfig extends YamlDocument {

    /**
     * Formatter to determine the scalar style for configuration values.
     * Currently preserves the current key's style as-is.
     */
    private static final Formatter<ScalarStyle, String> CONFIG_FORMATTER = Formatter.identity();

    /**
     * Constructs a new instance of {@link IMConfig}.
     *
     * @param document The YAML configuration file to be loaded or created.
     * @param defaults An optional input stream containing default configuration data.
     *                 This data is used if the specified configuration file is not found.
     * @throws IOException Thrown if there are issues reading or creating the configuration file.
     */
    public IMConfig(
            @NotNull File document,
            @Nullable InputStream defaults
    ) throws IOException {
        this(document, defaults, true);
    }

    /**
     * Constructs a new instance of {@link IMConfig}.
     *
     * @param document The YAML configuration file to be loaded or created.
     * @param defaults An optional input stream containing default configuration data.
     *                 This data is used if the specified configuration file is not found.
     * @throws IOException Thrown if there are issues reading or creating the configuration file.
     */
    public IMConfig(
            @NotNull File document,
            @Nullable InputStream defaults,
            boolean autoUpdate
    ) throws IOException {
        super(document, defaults,
                LoaderSettings
                        .builder()
                        .setAutoUpdate(autoUpdate)
                        .build(),
                DumperSettings
                        .builder()
                        .setPreserveFlowStyle(true)
                        .setPreserveScalarStyle(true)
                        .setScalarFormatter(CONFIG_FORMATTER)
                        .build(),
                UpdaterSettings
                        .builder()
                        .setKeepAll(autoUpdate)
                        .build()
        );
    }

    /**
     * Saves the current configuration to the associated YAML file.
     *
     * @return {@code true} if the save operation is successful, otherwise {@code false}.
     */
    @Override
    public boolean save() {
        return this.save("Undefined Message");
    }

    /**
     * Saves the current configuration to the associated YAML file with an optional custom error message.
     *
     * @param message Custom error message to be included in case of an exception.
     * @return {@code true} if the save operation is successful, otherwise a {@code RuntimeException} is thrown with the specified error message.
     */
    public boolean save(final String message) {
        try {
            return super.save();
        } catch (IOException e) {
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Reloads the configuration from the associated YAML file.
     *
     * @return {@code true} if the reload operation is successful, otherwise a {@code RuntimeException} is thrown with a default error message.
     */
    @Override
    public boolean reload() {
        return this.reload("Failed while reloading file: " + Objects.requireNonNull(this.getFile()).getPath());
    }

    /**
     * Reloads the configuration from the associated YAML file with an optional custom error message.
     *
     * @param message Custom error message to be included in case of an exception.
     * @return {@code true} if the reload operation is successful, otherwise a {@code RuntimeException} is thrown with the specified error message.
     */
    public boolean reload(final String message) {
        try {
            return super.reload();
        } catch (IOException e) {
            throw new RuntimeException(message, e);
        }
    }

}
