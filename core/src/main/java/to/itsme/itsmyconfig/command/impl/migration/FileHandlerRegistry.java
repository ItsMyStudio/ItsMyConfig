package to.itsme.itsmyconfig.command.impl.migration;

import to.itsme.itsmyconfig.command.impl.migration.file.PropertiesFileHandler;
import to.itsme.itsmyconfig.command.impl.migration.file.TextFileHandler;
import to.itsme.itsmyconfig.command.impl.migration.file.YamlFileHandler;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class FileHandlerRegistry {

    private final Map<String, FileHandler> handlers = new HashMap<>();

    public FileHandlerRegistry() {
        final var yamlFileHandler = new YamlFileHandler();
        register("yml", yamlFileHandler);
        register("yaml", yamlFileHandler);
        register("txt", new TextFileHandler());
        register("properties", new PropertiesFileHandler());
    }

    public void register(final String extension, final FileHandler handler) {
        handlers.put(extension.toLowerCase(), handler);
    }

    public FileHandler forFile(final File file) {
        final String name = file.getName();
        final int dot = name.lastIndexOf('.');
        if (dot == -1) return null;
        return handlers.get(name.substring(dot + 1).toLowerCase());
    }

}
