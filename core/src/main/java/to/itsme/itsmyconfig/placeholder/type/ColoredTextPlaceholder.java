package to.itsme.itsmyconfig.placeholder.type;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.placeholder.PlaceholderDependancy;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;
import to.itsme.itsmyconfig.util.Utilities;

import java.util.Set;

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
                PlaceholderDependancy.NONE,
                PlaceholderDependancy.PLAYER,
                PlaceholderDependancy.OFFLINE_PLAYER
        );
        this.miniText = section.getString("value", "");
        this.registerArguments(this.miniText);

        this.compiledPlaceholders = Set.of(
                mainCompiledPlaceholder(),
                this.compileVariant("legacy", this::getLegacyResult, 0, -1),
                this.compileVariant("l", this::getLegacyResult, 0, -1),
                this.compileVariant("console", this::getConsoleResult, 0, -1),
                this.compileVariant("c", this::getConsoleResult, 0, -1),
                this.compileVariant("mini", this::getMiniResult, 0, -1),
                this.compileVariant("m", this::getMiniResult, 0, -1)
        );
    }

    @Override
    public String getResult(String[] args) {
        if (args.length == 0) {
            return this.miniText;
        }

        return this.replaceArguments(args, this.miniText);
    }

    @Override
    public String getResult(final Player player, final String[] args) {
        if (args.length == 0) {
            return this.miniText;
        }

        return this.replaceArguments(args, this.miniText);
    }

    @Override
    public String getResult(final OfflinePlayer player, final String[] args) {
        if (args.length == 0) {
            return this.miniText;
        }

        return this.replaceArguments(args, this.miniText);
    }

    private String getLegacyResult(final OfflinePlayer player, final String[] args) {
        return this.asVariantString(player, this.replaceArguments(
                args,
                AMPERSAND_SERIALIZER.serialize(
                        Utilities.translate(this.miniText, player)
                )
        ));
    }

    private String getConsoleResult(final OfflinePlayer player, final String[] args) {
        return this.asVariantString(player, this.replaceArguments(
                args,
                SECTION_SERIALIZER.serialize(
                        Utilities.translate(this.miniText, player)
                )
        ));
    }

    private String getMiniResult(final OfflinePlayer player, final String[] args) {
        return this.asVariantString(player, this.replaceArguments(args, this.miniText));
    }

}
