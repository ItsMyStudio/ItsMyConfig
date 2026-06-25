package to.itsme.itsmyconfig.font;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.tag.TagPattern;
import org.jetbrains.annotations.NotNull;
import to.itsme.itsmyconfig.util.Strings;

public abstract class FontImpl implements Font {

    private final @TagPattern String name;
    private final TextReplacementConfig config = this.createConfig();

    public FontImpl(final @TagPattern String name) {
        this.name = name;
    }

    public @TagPattern String getName() {
        return name;
    }

    @Override
    public @NotNull Component apply(final @NotNull Component component) {
        return component.replaceText(config);
    }

    /**
     * Constructs a {@link TextReplacementConfig} that matches each letter
     * individually via {@link Strings#LETTERS_PATTERN} and replaces it using
     * this font's {@link #apply(String)} transformation.
     *
     * <p>Non-letter characters pass through unchanged.</p>
     */
    private TextReplacementConfig createConfig() {
        final TextReplacementConfig.Builder config = TextReplacementConfig.builder();
        config.match(Strings.LETTERS_PATTERN).replacement((matchResult, builder) -> {
            final String text = this.apply(matchResult.group());
            return Component.text().content(text);
        });
        return config.build();
    }

}
