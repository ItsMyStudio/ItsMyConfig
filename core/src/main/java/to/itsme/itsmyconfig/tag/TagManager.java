package to.itsme.itsmyconfig.tag;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import to.itsme.itsmyconfig.tag.api.ArgumentsTag;
import to.itsme.itsmyconfig.tag.api.Cancellable;
import to.itsme.itsmyconfig.tag.api.Tag;
import to.itsme.itsmyconfig.tag.impl.*;
import to.itsme.itsmyconfig.tag.impl.title.SubtitleTag;
import to.itsme.itsmyconfig.tag.impl.title.TitleTag;
import to.itsme.itsmyconfig.tag.impl.toast.ToastTag;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import to.itsme.itsmyconfig.util.ChatResendDetector;

public final class TagManager {

    private static final Pattern ARG_TAG_PATTERN = Pattern.compile("<(\\w+)((?::(?:\"([^\"]*)\"|'([^']*)'|`([^`]*)`|([^:\\s>]+)))*?)>");

    private static final int INITIAL_CAPACITY;
    private static final Map<String, Tag> tags = new LinkedHashMap<>();

    static {
        final AtomicInteger defaultCapacity = new AtomicInteger();
        List.of(
                new RepeatTag(), new DelayTag(),
                new BossbarTag(), new ActiobarTag(),
                new TitleTag(), new SubtitleTag(),
                new ToastTag(), new SoundTag()
        ).forEach(tag -> {
            tags.put(tag.name(), tag);
            defaultCapacity.set(Math.max(tag.maxArguments(), defaultCapacity.get()));
        });

        INITIAL_CAPACITY = defaultCapacity.get();
    }

    public static String process(
            final Player player,
            @NotNull String text
    ) {
        // handle argument tags
        text = processArgumentTags(player, text);

        return text;
    }

    public static String processArgumentTags(
            final Player player,
            @NotNull String text
    ) {
        // Check if player is currently in burst mode (packet flood already detected by listener)
        final String playerIdentifier = player.getUniqueId().toString();
        final boolean isChatClearBurst = ChatResendDetector.isInBurst(playerIdentifier);
        
        Matcher matcher = ARG_TAG_PATTERN.matcher(text);
        while (matcher.find()) {
            final int start = matcher.start();
            final int end = matcher.end();

            // Skip escaped tags
            if (start > 0 && text.charAt(start - 1) == '\\') {
                continue;
            }

            final String tagName = matcher.group(1);
            final Tag tag = tags.get(tagName);
            if (!(tag instanceof ArgumentsTag argumentsTag)) {
                continue; // unknown tag — skip safely, do NOT replace
            }

            // During chat clear bursts, filter out sound and actionbar tags
            if (isChatClearBurst && (tagName.equals("sound") || tagName.equals("actionbar"))) {
                // For both actionbar and sound during burst: remove entirely
                // - Actionbar messages should be cancelled completely (if message becomes empty, packet is cancelled)
                // - Sound tags should be stripped so sound doesn't play
                text = text.substring(0, start) + text.substring(end);
                matcher = ARG_TAG_PATTERN.matcher(text);
                continue;
            }

            final String arguments = matcher.group(2);
            final String[] args = extractArguments(arguments);
            if (args.length == 1 && "cancel".equals(args[0])) {
                if (tag instanceof Cancellable cancellable) {
                    cancellable.cancelFor(player);
                    text = text.substring(0, start) + text.substring(end);
                    matcher = ARG_TAG_PATTERN.matcher(text);
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
            matcher = ARG_TAG_PATTERN.matcher(text);
        }

        return text;
    }

    public static String[] extractArguments(final String rawArgs) {
        final List<String> args = new ArrayList<>(INITIAL_CAPACITY);

        int i = 0;
        while (i < rawArgs.length()) {
            if (rawArgs.charAt(i) != ':') {
                i++;
                continue;
            }
            i++; // skip ':'
            if (i >= rawArgs.length()) break;

            char delimiter = rawArgs.charAt(i);
            int end;

            if (delimiter == '"' || delimiter == '\'' || delimiter == '`') {
                i++; // skip opening quote
                end = rawArgs.indexOf(delimiter, i);
                if (end == -1) break;
                args.add(rawArgs.substring(i, end));
                i = end + 1;
            } else {
                end = i;
                while (end < rawArgs.length()) {
                    char c = rawArgs.charAt(end);
                    if (c == ':' || c == '>' || Character.isWhitespace(c)) break;
                    end++;
                }
                args.add(rawArgs.substring(i, end));
                i = end;
            }
        }

        return args.toArray(new String[0]);
    }

}
