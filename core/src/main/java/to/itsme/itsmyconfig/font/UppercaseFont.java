package to.itsme.itsmyconfig.font;

import org.jetbrains.annotations.NotNull;

public class UppercaseFont extends FontImpl {

    public static final UppercaseFont INSTANCE = new UppercaseFont();

    public UppercaseFont() {
        super("uppercase");
    }

    @Override
    public @NotNull String apply(@NotNull String text) {
        return text.toUpperCase();
    }

}
