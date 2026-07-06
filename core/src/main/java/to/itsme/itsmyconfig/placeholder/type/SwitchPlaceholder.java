package to.itsme.itsmyconfig.placeholder.type;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.placeholder.PlaceholderDependancy;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;
import to.itsme.itsmyconfig.util.Strings;

import java.util.*;

public final class SwitchPlaceholder extends Placeholder {

    private final Map<String, String> exactCases;
    private final long[] rangeStarts;
    private final long[] rangeEnds;
    private final String[] rangeValues;
    private final String defaultValue;

    public SwitchPlaceholder(
            final String filePath,
            final ConfigurationSection section
    ) {
        super(section, filePath, PlaceholderType.SWITCH, PlaceholderDependancy.NONE);
        this.defaultValue = section.getString("default", "");

        final ConfigurationSection cases = section.getConfigurationSection("cases");
        if (cases == null) {
            this.exactCases = Collections.emptyMap();
            this.rangeStarts = new long[0];
            this.rangeEnds = new long[0];
            this.rangeValues = Strings.EMPTY_STRING_ARRAY;
            return;
        }

        final Map<String, String> exact = new LinkedHashMap<>();
        final List<RangeEntry> ranges = new ArrayList<>();

        for (final String key : cases.getKeys(false)) {
            final Object raw = cases.get(key);
            final String value = raw == null ? "" : String.valueOf(raw);

            final Range r = parseRangeKey(key);
            if (r != null) {
                if (r.start > r.end) continue;
                ranges.add(new RangeEntry(r.start, r.end, value));
            } else {
                exact.put(key, value);
            }
        }

        this.exactCases = Collections.unmodifiableMap(exact);

        ranges.sort(Comparator.comparingLong((RangeEntry e) -> e.start).thenComparingLong(e -> e.end));
        final List<RangeEntry> filtered = new ArrayList<>(ranges.size());
        RangeEntry prev = null;
        for (final RangeEntry cur : ranges) {
            if (prev != null && cur.start <= prev.end) continue;
            filtered.add(cur);
            prev = cur;
        }

        final int n = filtered.size();
        this.rangeStarts = new long[n];
        this.rangeEnds = new long[n];
        this.rangeValues = new String[n];
        for (int i = 0; i < n; i++) {
            final RangeEntry e = filtered.get(i);
            this.rangeStarts[i] = e.start;
            this.rangeEnds[i] = e.end;
            this.rangeValues[i] = e.value;
        }
    }

    /**
     * Parses a numeric range.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code -10}  → (-∞, 10]</li>
     *   <li>{@code 10-}  → [10, +∞)</li>
     *   <li>{@code 5-10} → [5, 10]</li>
     * </ul>
     *
     * @return the parsed range, or {@code null} if the input is not a valid range
     */
    private static Range parseRangeKey(final String keyRaw) {
        if (keyRaw == null) return null;
        final String key = keyRaw.trim();
        if (!key.contains("-")) return null;

        if (key.startsWith("-") && key.length() > 1) {
            final Long end = tryParseLong(key.substring(1));
            if (end == null) return null;
            return new Range(Long.MIN_VALUE, end);
        }

        if (key.endsWith("-") && key.length() > 1) {
            final Long start = tryParseLong(key.substring(0, key.length() - 1));
            if (start == null) return null;
            return new Range(start, Long.MAX_VALUE);
        }

        final int dash = key.indexOf('-');
        if (dash <= 0 || dash >= key.length() - 1) return null;
        final Long start = tryParseLong(key.substring(0, dash));
        final Long end = tryParseLong(key.substring(dash + 1));
        if (start == null || end == null) return null;
        return new Range(start, end);
    }

    private static Long tryParseLong(final String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (final Exception ignored) {
            return null;
        }
    }

    /**
     * Substitutes {@code {0}, {1}, ...} in the matched value with args[1], args[2], ...
     * args[0] is the switch key, not a template variable.
     */
    private static String applyArgs(final String template, final String[] args) {
        String out = template;
        for (int i = 1; i < args.length; i++) {
            out = out.replace("{" + (i - 1) + "}", args[i]);
        }
        return out;
    }

    @Override
    public int minArgs() {
        return 1;
    }

    /**
     * Resolves a value using the following lookup order:
     *
     * <ol>
     *   <li>Exact key match in {@link #exactCases}</li>
     *   <li>Empty-string key (for blank input)</li>
     *   <li>Numeric key matched against a range using binary search</li>
     *   <li>{@link #defaultValue}</li>
     * </ol>
     */
    @Override
    public String getResult(final OfflinePlayer player, final String[] args) {
        if (args.length == 0) {
            return defaultValue;
        }

        final String key = args[0];
        String value = null;

        if (!key.isEmpty()) {
            value = exactCases.get(key);
        }

        if (value == null && key.isEmpty()) {
            value = exactCases.get("");
        }

        if (value == null) {
            final Long num = tryParseLong(key);
            if (num != null) {
                final int idx = findRangeIndex(num);
                if (idx >= 0) {
                    value = rangeValues[idx];
                }
            }
        }

        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        if (args.length == 1) {
            return value;
        }

        return applyArgs(value, args);
    }

    /**
     * Binary search: finds the range entry whose [start, end] contains x.
     * Ranges are non-overlapping and sorted by start then end.
     */
    private int findRangeIndex(final long x) {
        if (rangeStarts.length == 0) return -1;
        int lo = 0, hi = rangeStarts.length - 1;
        int best = -1;
        while (lo <= hi) {
            final int mid = (lo + hi) >>> 1;
            if (rangeStarts[mid] <= x) {
                best = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        if (best == -1) return -1;
        return (x <= rangeEnds[best]) ? best : -1;
    }

    private record Range(long start, long end) {}

    private record RangeEntry(long start, long end, String value) {}

}
