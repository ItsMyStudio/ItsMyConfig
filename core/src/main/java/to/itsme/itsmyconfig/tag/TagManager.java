package to.itsme.itsmyconfig.tag;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import to.itsme.itsmyconfig.tag.api.ArgumentsTag;
import to.itsme.itsmyconfig.tag.api.Cancellable;
import to.itsme.itsmyconfig.tag.api.ClosableTag;
import to.itsme.itsmyconfig.tag.api.Tag;
import to.itsme.itsmyconfig.tag.argument.*;
import to.itsme.itsmyconfig.tag.closable.*;
import to.itsme.itsmyconfig.tag.argument.title.SubtitleTag;
import to.itsme.itsmyconfig.tag.argument.title.TitleTag;
import to.itsme.itsmyconfig.tag.argument.toast.ToastTag;
import to.itsme.itsmyconfig.util.Strings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TagManager {

    // Matches opening tags: <tagname> or <tagname:arg1:arg2>
    private static final Pattern ARG_TAG_PATTERN = Pattern.compile("<(\\w+)((?::(?:\"([^\"]*)\"|'([^']*)'|`([^`]*)`|([^:\\s>]+)))*?)>");
    private static final Map<String, Tag> tags = new LinkedHashMap<>();

    static {
        List.of(
                // argument tags
                new RepeatTag(), new DelayTag(),
                new BossbarTag(), new ActiobarTag(),
                new TitleTag(), new SubtitleTag(),
                new ToastTag(), new SoundTag(),
                // closable tags
                new UppercaseTag(), new LowercaseTag(), new PlainTag()
        ).forEach(tag -> tags.put(tag.name(), tag));
    }

    public static String process(
            final Player player,
            @NotNull String text
    ) {
        text = processArgumentTags(player, text);
        return text;
    }

    public static String processArgumentTags(
            final Player player,
            @NotNull String text
    ) {
        int searchFrom = 0;
        Matcher matcher = ARG_TAG_PATTERN.matcher(text);
        while (matcher.find(searchFrom)) {
            final int start = matcher.start();
            final int end = matcher.end();

            // Skip escaped tags
            if (start > 0 && text.charAt(start - 1) == '\\') {
                searchFrom = end;
                continue;
            }

            final String tagName = matcher.group(1);
            final Tag tag = tags.get(tagName);

            // --- Closable tag ---
            if (tag instanceof ClosableTag closableTag) {
                final String closeToken = "</" + tagName + ">";
                final int closeStart = text.indexOf(closeToken, end);

                if (closeStart == -1) {
                    searchFrom = end;
                    continue; // no recreate — string unchanged
                }

                final int closeEnd = closeStart + closeToken.length();
                final String inner = text.substring(end, closeStart);

                final String arguments = matcher.group(2);
                final String[] args = arguments.isEmpty()
                        ? new String[0]
                        : Strings.extractArguments(arguments, '>');

                final String replaced;
                if (args.length < closableTag.minArguments()) {
                    replaced = "[Not enough arguments for Tag: " + tagName + "]";
                } else if (args.length > closableTag.maxArguments()) {
                    replaced = "[Too many arguments for Tag: " + tagName + "]";
                } else {
                    replaced = closableTag.process(player, inner, args);
                }

                text = text.substring(0, start) + replaced + text.substring(closeEnd);
                matcher = ARG_TAG_PATTERN.matcher(text); // recreate — string changed
                searchFrom = start;
                continue;
            }

            // --- Argument tag ---
            if (!(tag instanceof ArgumentsTag argumentsTag)) {
                searchFrom = start + 1; // re-scan from inside, not past the whole match
                continue; // unknown tag - skip safely, do NOT replace
            }

            final String arguments = matcher.group(2);
            final String[] args = Strings.extractArguments(arguments, '>');
            if (args.length == 1 && "cancel".equals(args[0])) {
                if (tag instanceof Cancellable cancellable) {
                    cancellable.cancelFor(player);
                    text = text.substring(0, start) + text.substring(end);
                    matcher = ARG_TAG_PATTERN.matcher(text); // recreate — string changed
                    searchFrom = start;
                    continue;
                }
            }

            final String replaced;
            if (args.length < argumentsTag.minArguments()) {
                replaced = "[Not enough arguments for Tag: " + tagName + "]";
            } else if (args.length > argumentsTag.maxArguments()) {
                replaced = "[Too many arguments for Tag: " + tagName + "]";
            } else {
                replaced = argumentsTag.process(player, args);
            }

            text = text.substring(0, start) + replaced + text.substring(end);
            matcher = ARG_TAG_PATTERN.matcher(text); // recreate — string changed
            searchFrom = start + replaced.length();
        }

        return text;
    }

}
