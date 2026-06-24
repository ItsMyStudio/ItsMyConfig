package to.itsme.itsmyconfig.font;

import org.jetbrains.annotations.NotNull;

public class LowercaseFont extends FontImpl {

    public static final LowercaseFont INSTANCE = new LowercaseFont();

    public LowercaseFont() {
        super("lowercase");
    }

    @Override
    public @NotNull String apply(@NotNull String text) {
        return text.toLowerCase();
    }

}
