package to.itsme.itsmyconfig.command.impl.migration;

import to.itsme.itsmyconfig.command.impl.migration.file.PropertiesFileHandler;
import to.itsme.itsmyconfig.command.impl.migration.file.TextFileHandler;
import to.itsme.itsmyconfig.command.impl.migration.file.YamlFileHandler;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class FileHandlerRegistry {

    public static final Map<String, FileHandler> handlers = new HashMap<>();

    static  {
        final var yamlFileHandler = new YamlFileHandler();
        register("yml", yamlFileHandler);
        register("yaml", yamlFileHandler);
        register("txt", new TextFileHandler());
        register("properties", new PropertiesFileHandler());
    }

    public static void register(final String extension, final FileHandler handler) {
        handlers.put(extension.toLowerCase(), handler);
    }

    public static FileHandler forFile(final File file) {
        final String name = file.getName();
        final int dot = name.lastIndexOf('.');
        if (dot == -1) return null;
        return handlers.get(name.substring(dot + 1).toLowerCase());
    }

}
