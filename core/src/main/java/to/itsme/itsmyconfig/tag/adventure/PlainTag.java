package to.itsme.itsmyconfig.tag.adventure;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.Modifying;
import org.jspecify.annotations.NonNull;

/**
 * A {@link Modifying} tag that strips all formatting (colors, decorations, fonts)
 * from every component in the subtree, leaving only plain text.
 */
public final class PlainTag implements Modifying {

    public static final String NAME = "plain";

    @Override
    public @NonNull Component apply(final Component current, final int depth) {
        return current.style(style -> style
                .color(null)
                .decoration(TextDecoration.BOLD, TextDecoration.State.NOT_SET)
                .decoration(TextDecoration.ITALIC, TextDecoration.State.NOT_SET)
                .decoration(TextDecoration.UNDERLINED, TextDecoration.State.NOT_SET)
                .decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.NOT_SET)
                .decoration(TextDecoration.OBFUSCATED, TextDecoration.State.NOT_SET)
                .font(null)
                .clickEvent(null)
                .hoverEvent(null)
                .insertion(null)
        );
    }

}