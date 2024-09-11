package to.itsme.itsmyconfig.placeholder.type;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.placeholder.PlaceholderDependancy;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;
import to.itsme.itsmyconfig.util.Utilities;

import java.util.Locale;

public final class ColoredTextPlaceholder extends Placeholder {

    private final static LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer
            .builder()
            .character('§')
            .hexCharacter('#')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final static LegacyComponentSerializer AMPERSAND_SERIALIZER = LegacyComponentSerializer
            .builder()
            .character('&')
            .hexCharacter('#')
            .hexColors()
            .build();

    private final String miniText;

    public ColoredTextPlaceholder(
            final String filePath,
            final ConfigurationSection section
    ) {
        super(
                section,
                filePath,
                PlaceholderType.COLORED_TEXT,
                PlaceholderDependancy.PLAYER,
                PlaceholderDependancy.OFFLINE_PLAYER
        );
        this.miniText = section.getString("value", "");
        this.registerArguments(this.miniText);
    }

    @Override
    public String getResult(final Player player, final String[] args) {
        if (args.length == 0) {
            return this.miniText;
        }

        final String firstArg = args[0].toLowerCase(Locale.ROOT);
        return switch (firstArg) {
            case "l", "legacy" -> this.replaceArguments(
                    args,
                    AMPERSAND_SERIALIZER.serialize(
                            Utilities.translate(this.miniText, player)
                    ), 1
            );
            case "c", "console" -> this.replaceArguments(
                    args,
                    SECTION_SERIALIZER.serialize(
                            Utilities.translate(this.miniText, player)
                    ), 1
            );
            case "m", "mini" -> this.replaceArguments(args, this.miniText, 1);
            default -> this.replaceArguments(args, this.miniText);
        };
    }

    @Override
    public String getResult(final OfflinePlayer player, final String[] args) {
        if (args.length == 0) {
            return this.miniText;
        }

        final String firstArg = args[0].toLowerCase(Locale.ROOT);
        return switch (firstArg) {
            case "l", "legacy" -> this.replaceArguments(
                    args,
                    AMPERSAND_SERIALIZER.serialize(
                            Utilities.translate(this.miniText, player)
                    ), 1
            );
            case "c", "console" -> this.replaceArguments(
                    args,
                    SECTION_SERIALIZER.serialize(
                            Utilities.translate(this.miniText, player)
                    ), 1
            );
            case "m", "mini" -> this.replaceArguments(args, this.miniText, 1);
            default -> this.replaceArguments(args, this.miniText);
        };
    }

}
