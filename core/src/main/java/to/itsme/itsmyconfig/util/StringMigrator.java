package to.itsme.itsmyconfig.util;

import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.placeholder.PlaceholderManager;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringMigrator {

    private static final Pattern PAPI_PREFIX = Pattern.compile("%(?:imc|itsmyconfig)_");
    private static final Pattern TAG_P_PLACEHOLDER = Pattern.compile("<p:([^>]+?)>");

    private final PlaceholderManager manager;

    public StringMigrator(final PlaceholderManager manager) {
        this.manager = manager;
    }

    public String migrate(final String input) {
        if (input == null) {
            return null;
        }

        return migrateTagP(migratePAPI(input));
    }

    private boolean isListType(final String key) {
        final Placeholder placeholder = this.manager.get(key);
        return placeholder != null && placeholder.getType() == PlaceholderType.LIST;
    }

    private String resolveRegisteredKey(final String inner) {
        if (this.manager.get(inner) != null) {
            return inner;
        }
        int search = 0;
        while (search < inner.length()) {
            final int underscore = inner.indexOf('_', search);
            if (underscore == -1) break;
            final String candidate = inner.substring(0, underscore);
            if (this.manager.get(candidate) != null) {
                return candidate;
            }
            search = underscore + 1;
        }
        return null;
    }

    private String migratePAPI(final String input) {
        final StringBuilder sb = new StringBuilder(input.length());
        int last = 0;
        final Matcher m = PAPI_PREFIX.matcher(input);
        while (m.find()) {
            sb.append(input, last, m.start());
            final String prefix = m.group();
            final int contentStart = m.end();

            final int closingPercent = findClosingPercent(input, contentStart);
            if (closingPercent == -1) {
                sb.append(prefix);
                last = contentStart;
                continue;
            }

            final String inner = input.substring(contentStart, closingPercent);
            final String key = resolveRegisteredKey(inner);

            if (key == null) {
                final int colon = inner.indexOf(':');
                if (colon != -1 && this.manager.get(inner.substring(0, colon)) != null) {
                    sb.append(prefix).append(inner).append('%');
                    last = closingPercent + 1;
                    continue;
                }
                final int heuristicUnderscore = inner.indexOf('_');
                if (heuristicUnderscore != -1) {
                    final String heuristicKey = inner.substring(0, heuristicUnderscore);
                    final String rest = inner.substring(heuristicUnderscore + 1);
                    sb.append(prefix).append(heuristicKey);
                    for (final String arg : rest.split("::", -1)) {
                        sb.append(':').append(quoteArg(arg));
                    }
                    sb.append('%');
                    last = closingPercent + 1;
                    continue;
                }
                sb.append(prefix).append(inner).append('%');
                last = closingPercent + 1;
                continue;
            }

            if (key.equals(inner) || inner.charAt(key.length()) != '_') {
                sb.append(prefix).append(inner).append('%');
                last = closingPercent + 1;
                continue;
            }

            final String afterKey = inner.substring(key.length() + 1);
            final int doubleColon = afterKey.indexOf("::");
            final String firstArg;
            final String[] moreArgs;
            if (doubleColon != -1) {
                firstArg = afterKey.substring(0, doubleColon);
                moreArgs = afterKey.substring(doubleColon + 2).split("::", -1);
            } else {
                firstArg = afterKey;
                moreArgs = new String[0];
            }

            if (isListType(key) && Strings.isNumber(firstArg)) {
                sb.append(prefix).append(key).append('_').append(firstArg).append('%');
            } else {
                sb.append(prefix).append(key).append(':').append(quoteArg(firstArg));
                for (final String arg : moreArgs) {
                    sb.append(':').append(quoteArg(arg));
                }
                sb.append('%');
            }
            last = closingPercent + 1;
        }
        sb.append(input.substring(last));
        return sb.toString();
    }

    private static int findClosingPercent(final String input, final int fromIndex) {
        int i = fromIndex;
        final Matcher original = PAPI_PREFIX.matcher(input);
        while (true) {
            final int next = input.indexOf('%', i);
            if (next == -1) return -1;
            // if this % opens a new PAPI placeholder, current one is unclosed
            final Matcher m = original.region(next, input.length());
            if (m.lookingAt()) return -1;
            // if this looks like an embedded %other% placeholder, skip over it
            final int closing = input.indexOf('%', next + 1);
            if (closing != -1) {
                final String between = input.substring(next + 1, closing);
                if (!between.isEmpty() && !between.contains(" ") && !between.contains("%")) {
                    if (closing + 1 < input.length()) {
                        i = closing + 1;
                        continue;
                    }
                }
            }
            return next;
        }
    }

    private String migrateTagP(final String input) {
        final Matcher m = TAG_P_PLACEHOLDER.matcher(input);
        if (!m.find()) {
            return input;
        }

        final StringBuilder sb = new StringBuilder(input.length());
        int last = 0;
        m.reset();
        while (m.find()) {
            sb.append(input, last, m.start());
            final String inner = m.group(1);
            final int firstColon = inner.indexOf(':');
            if (firstColon == -1) {
                sb.append(m.group());
                last = m.end();
                continue;
            }

            final String key = inner.substring(0, firstColon);
            final String argsPart = inner.substring(firstColon + 1);
            final String[] args = splitTagArgs(argsPart);

            if (args.length > 0 && isListType(key) && Strings.isNumber(args[0])) {
                sb.append("<p:").append(key).append('_').append(args[0]);
                for (int j = 1; j < args.length; j++) {
                    sb.append(':').append(args[j]);
                }
                sb.append('>');
            } else {
                sb.append(m.group());
            }
            last = m.end();
        }
        sb.append(input.substring(last));
        return sb.toString();
    }

    private static String[] splitTagArgs(final String argsPart) {
        final List<String> args = new ArrayList<>();
        int i = 0;
        while (i < argsPart.length()) {
            final char c = argsPart.charAt(i);
            if (c == '"' || c == '\'' || c == '`') {
                final int end = argsPart.indexOf(c, i + 1);
                if (end == -1) break;
                args.add(argsPart.substring(i + 1, end));
                i = end + 1;
                if (i < argsPart.length() && argsPart.charAt(i) == ':') i++;
            } else {
                final int end = argsPart.indexOf(':', i);
                if (end == -1) {
                    args.add(argsPart.substring(i));
                    break;
                }
                args.add(argsPart.substring(i, end));
                i = end + 1;
            }
        }
        return args.toArray(new String[0]);
    }

    private static String quoteArg(final String arg) {
        if (arg.isEmpty() || arg.indexOf(':') != -1 || arg.indexOf(' ') != -1) {
            final boolean hasSingle = arg.indexOf('\'') != -1;
            final boolean hasDouble = arg.indexOf('"') != -1;
            if (hasSingle && hasDouble) {
                return "`" + arg + "`";
            }
            if (hasSingle) {
                return "\"" + arg + "\"";
            }
            return "'" + arg + "'";
        }
        return arg;
    }

}
