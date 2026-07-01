package to.itsme.itsmyconfig.util;

import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.placeholder.PlaceholderManager;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringMigrator {

    private record DelimiterStyle(Pattern prefix, char open, char close) {
        static final DelimiterStyle PERCENT =
                new DelimiterStyle(Pattern.compile("%(?:imc|itsmyconfig)_"), '%', '%');
        static final DelimiterStyle BRACE =
                new DelimiterStyle(Pattern.compile("\\{(?:imc|itsmyconfig)_"), '{', '}');
    }

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

        String result = input;
        result = migratePAPIDelimited(result, DelimiterStyle.BRACE);
        result = migratePAPIDelimited(result, DelimiterStyle.PERCENT);
        result = migrateTagP(result);
        return result;
    }

    private boolean isListType(final String key) {
        final Placeholder placeholder = this.manager.get(key);
        return placeholder != null && placeholder.getType() == PlaceholderType.LIST;
    }

    private boolean hasCompiledVariant(final String key, final String arg) {
        return this.manager.hasCompiled(key + "_" + arg);
    }

    /**
     * Scans {@code text} for the first underscore-delimited prefix satisfying {@code test},
     * trying progressively longer prefixes (up to each underscore) left-to-right.
     * Returns the underscore index that ends the matching prefix, or -1 if none match.
     */
    private static int findFirstUnderscorePrefix(final String text, final Predicate<String> test) {
        int search = 0;
        while (search < text.length()) {
            final int underscore = text.indexOf('_', search);
            if (underscore == -1) break;
            final String candidate = text.substring(0, underscore);
            if (test.test(candidate)) {
                return underscore;
            }
            search = underscore + 1;
        }
        return -1;
    }

    private String resolveRegisteredKey(final String inner) {
        if (this.manager.get(inner) != null) {
            return inner;
        }
        final int underscore = findFirstUnderscorePrefix(inner, candidate -> this.manager.get(candidate) != null);
        return underscore == -1 ? null : inner.substring(0, underscore);
    }

    /**
     * Scans {@code afterKey} for the first underscore that separates a compiled variant
     * name from its arguments. Returns the index of that underscore, or -1 if not found.
     * <p>Example: for key="eco-format" and afterKey="console_{arg}",
     * finds underscore at position 7 because "eco-format_console" is a compiled variant.
     */
    private int findCompiledVariantSep(final String afterKey, final String key) {
        return findFirstUnderscorePrefix(afterKey, candidate -> this.manager.hasCompiled(key + "_" + candidate));
    }

    /**
     * Migrates old-style PAPI-format placeholders using the given delimiter style
     * (e.g. {@code %imc_key_arg%} or {@code {imc_key_arg}}) to the new colon-separated format.
     */
    private String migratePAPIDelimited(final String input, final DelimiterStyle style) {
        final StringBuilder sb = new StringBuilder(input.length());
        int last = 0;
        final Matcher m = style.prefix().matcher(input);
        while (m.find()) {
            sb.append(input, last, m.start());
            final String prefix = m.group();
            final int contentStart = m.end();

            final int closingDelim = findClosingDelimiter(input, contentStart, style);
            if (closingDelim == -1) {
                sb.append(prefix);
                last = contentStart;
                continue;
            }

            final String inner = input.substring(contentStart, closingDelim);
            sb.append(prefix).append(migrateInner(inner)).append(style.close());
            last = closingDelim + 1;
        }
        sb.append(input.substring(last));
        return sb.toString();
    }

    /**
     * Migrates the content between delimiters (e.g. {@code key_arg} in {@code %key_arg%})
     * to its new-format equivalent, or returns it unchanged if no migration applies.
     */
    private String migrateInner(final String inner) {
        final Matcher fontMatch = FONT_LEGACY.matcher(inner);
        if (fontMatch.matches()) {
            final String type = fontMatch.group(1).toLowerCase();
            final String arg = fontMatch.group(2);
            return type + ':' + arg;
        }

        // If already in new-format (single colon separator) and the key part
        // before the colon is a compiled variant, emit unchanged.
        // Must NOT be old-style :: (double colon after the key).
        final int newStyleColon = inner.indexOf(':');
        if (newStyleColon != -1 && newStyleColon + 1 < inner.length() && inner.charAt(newStyleColon + 1) != ':') {
            final String compiledCandidate = inner.substring(0, newStyleColon);
            if (this.manager.hasCompiled(compiledCandidate)) {
                return inner;
            }
        }

        final String key = resolveRegisteredKey(inner);
        if (key == null) {
            return inner;
        }

        if (key.equals(inner) || inner.charAt(key.length()) != '_') {
            return inner;
        }

        final String afterKey = inner.substring(key.length() + 1);
        final int doubleColon = afterKey.indexOf("::");
        final String variant;
        final String firstArg;
        final String[] moreArgs;

        if (doubleColon != -1) {
            // Old-style :: separator — no compiled variant hunting needed
            variant = null;
            firstArg = afterKey.substring(0, doubleColon);
            moreArgs = afterKey.substring(doubleColon + 2).split("::", -1);
        } else {
            // Try to find a compiled variant prefix in afterKey
            // e.g. "console_{arg}" → variant="console", firstArg="{arg}"
            final int variantSep = findCompiledVariantSep(afterKey, key);
            if (variantSep != -1) {
                variant = afterKey.substring(0, variantSep);
                firstArg = variantSep + 1 < afterKey.length() ? afterKey.substring(variantSep + 1) : "";
            } else {
                variant = null;
                firstArg = afterKey;
            }
            moreArgs = new String[0];
        }

        final StringBuilder out = new StringBuilder();
        if (variant != null) {
            out.append(key).append('_').append(variant);
            if (!firstArg.isEmpty()) {
                out.append(':').append(quoteArg(firstArg));
            }
        } else if (hasCompiledVariant(key, firstArg)) {
            out.append(key).append('_').append(firstArg);
            for (final String arg : moreArgs) {
                out.append(':').append(quoteArg(arg));
            }
        } else {
            out.append(key).append(':').append(quoteArg(firstArg));
            for (final String arg : moreArgs) {
                out.append(':').append(quoteArg(arg));
            }
        }
        return out.toString();
    }

    private static int findClosingDelimiter(final String input, final int fromIndex, final DelimiterStyle style) {
        final int next = input.indexOf(style.close(), fromIndex);
        if (next == -1) return -1;
        // If this delimiter opens a new placeholder of the same kind, the current one is unclosed — skip it
        if (style.prefix().matcher(input).region(next, input.length()).lookingAt()) return -1;
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
