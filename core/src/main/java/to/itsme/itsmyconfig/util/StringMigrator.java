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
    private static final Pattern FONT_LEGACY =
            Pattern.compile("^(?:font|f)_(smallcaps|latin)_(.+)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

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

    private boolean hasCompiledVariant(final String key, final String arg) {
        return this.manager.hasCompiled(key + "_" + arg);
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

            final Matcher fontMatch = FONT_LEGACY.matcher(inner);
            if (fontMatch.matches()) {
                final String type = fontMatch.group(1).toLowerCase();
                final String arg = fontMatch.group(2);
                sb.append(prefix).append(type).append(':').append(arg).append('%');
                last = closingPercent + 1;
                continue;
            }

            // If already in new-format (single colon separator) and the key part
            // before the colon is a compiled variant, emit unchanged.
            // Must NOT be old-style :: (double colon after the key).
            final int newStyleColon = inner.indexOf(':');
            if (newStyleColon != -1 && newStyleColon + 1 < inner.length() && inner.charAt(newStyleColon + 1) != ':') {
                final String compiledCandidate = inner.substring(0, newStyleColon);
                if (this.manager.hasCompiled(compiledCandidate)) {
                    sb.append(prefix).append(inner).append('%');
                    last = closingPercent + 1;
                    continue;
                }
            }

            final String key = resolveRegisteredKey(inner);

            if (key == null) {
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
            } else if (hasCompiledVariant(key, firstArg)) {
                sb.append(prefix).append(key).append('_').append(firstArg);
                for (final String arg : moreArgs) {
                    sb.append(':').append(quoteArg(arg));
                }
                sb.append('%');
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
        final int next = input.indexOf('%', fromIndex);
        if (next == -1) return -1;
        // If this % opens a new PAPI placeholder, the current one is unclosed — skip it
        if (PAPI_PREFIX.matcher(input).region(next, input.length()).lookingAt()) return -1;
        return next;
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

            if (args.length > 0 && (isListType(key) && Strings.isNumber(args[0]) || hasCompiledVariant(key, args[0]))) {
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
