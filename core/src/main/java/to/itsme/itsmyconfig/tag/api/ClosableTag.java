package to.itsme.itsmyconfig.tag.api;

import org.bukkit.entity.Player;

public abstract class ClosableTag implements Tag {

    /**
     * Minimum number of arguments this tag accepts.
     * Defaults to 0 since arguments are optional for closable tags.
     */
    public int minArguments() {
        return 0;
    }

    /**
     * Maximum number of arguments this tag accepts.
     * Defaults to 0 — override if your tag uses arguments.
     */
    public int maxArguments() {
        return 0;
    }

    /**
     * Process the inner text between the open and close tags.
     *
     * @param player    the player context, may be null
     * @param inner     the raw text between {@code <name>} and {@code </name>}
     * @param arguments the parsed arguments from the opening tag (may be empty)
     * @return the replacement string
     */
    public abstract String process(final Player player, final String inner, final String[] arguments);

}