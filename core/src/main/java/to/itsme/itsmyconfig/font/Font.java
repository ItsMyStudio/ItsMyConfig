package to.itsme.itsmyconfig.font;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.TagPattern;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public interface Font {

    @TagPattern String getName();
    @NotNull String apply(final @NotNull String text);
    @NotNull Component apply(final @NotNull Component component);

    static Collection<Font> values() {
        return List.of(MappedFont.SMALL_CAPS);
    }

}
