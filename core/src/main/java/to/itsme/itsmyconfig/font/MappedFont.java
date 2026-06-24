package to.itsme.itsmyconfig.font;

import net.kyori.adventure.text.minimessage.tag.TagPattern;
import org.jetbrains.annotations.NotNull;
import to.itsme.itsmyconfig.util.Strings;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MappedFont extends FontImpl {

    public static final MappedFont SMALL_CAPS = new MappedFont(
            "smallcaps",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz",
            "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘꞯʀsᴛᴜᴠᴡxʏᴢᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘꞯʀsᴛᴜᴠᴡxʏᴢ"
    );

    private final Map<Character, Character> characterMap;

    MappedFont(
            final @TagPattern String name,
            final String original,
            final String replacements
    ) {
        super(name);
        this.characterMap = createMapping(original, replacements);
    }

    private static Map<Character, Character> createMapping(final String original, final String replacements) {
        if (original.length() != replacements.length()) {
            throw new IllegalArgumentException("Original and replacement texts must be of the same length.");
        }

        final Map<Character, Character> mapping = new HashMap<>();
        for (int i = 0; i < original.length(); i++) {
            mapping.put(original.charAt(i), replacements.charAt(i));
        }
        return mapping;
    }

    @Override
    public @NotNull String apply(final @NotNull String text) {
        final byte[] bytes = Strings.englishify(text).getBytes(StandardCharsets.UTF_8);
        final StringBuilder builder = new StringBuilder();
        for (final byte messageByte : bytes) {
            final char originalChar = (char) messageByte;
            final char styledChar = characterMap.getOrDefault(originalChar, originalChar);
            builder.append(styledChar);
        }
        return builder.toString();
    }

}
