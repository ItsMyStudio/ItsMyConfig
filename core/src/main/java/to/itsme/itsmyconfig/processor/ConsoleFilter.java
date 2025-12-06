package to.itsme.itsmyconfig.processor;

import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import to.itsme.itsmyconfig.message.AudienceResolver;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

import java.util.Optional;

public class ConsoleFilter extends AbstractFilter {

    @Override
    public Result filter(LogEvent event) {
        return checkMessage(event.getMessage());
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return checkMessage(msg);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        if (msg instanceof String) {
            return checkMessage(new SimpleMessage((String) msg));
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        return checkMessage(new SimpleMessage(msg));
    }

    private Result checkMessage(Message message) {
        if (message == null) {
            return Result.NEUTRAL;
        }

        String content = message.getFormattedMessage();
        Optional<String> parsed = Strings.parsePrefixedMessage(content);

        if (parsed.isPresent()) {
            try {
                // Translate the parsed message (which has the prefix removed)
                Component translated = Utilities.translate(parsed.get());

                // Use AudienceResolver on both Paper and Spigot
                // Note: Gradients will be converted to single colors in console output
                // This is a limitation of how BukkitAudiences handles console rendering
                AudienceResolver.resolve(org.bukkit.Bukkit.getConsoleSender()).sendMessage(translated);

                // Deny the original message to prevent it from being logged in its raw form
                return Result.DENY;
            } catch (Exception ignored) {
            }
        }

        return Result.NEUTRAL;
    }
}
