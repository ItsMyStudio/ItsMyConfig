package to.itsme.itsmyconfig.placeholder.type;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.placeholder.PlaceholderDependancy;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;

import java.util.*;

/**
 * YAML:
 * custom-placeholder:
 *   range-example:
 *     type: range
 *     default: ""
 *     values:
 *       "1-90":   "someCommand {0} %some_placeholder%"
 *       "91-120": "otherCommand {0} %another_placeholder%"
 * <p>
 * Supported keys (inclusive):
 *  - "A-B"  (A..B)
 *  - "-B"   (MIN..B)
 *  - "A-"   (A..MAX)
 * <p>
 * Usage:
 * %itsmyconfig_range-example_<number>::<arg1>::<arg2>...%
 * <p>
 * Notes:
 *  - args[0] = <number> (used for interval selection)
 *  - args[1] -> {0}, args[2] -> {1}, etc.
 * <p>
 * Example:
 * %itsmyconfig_range-example_95::PlayerName%
 * args[0]=95, args[1]=PlayerName -> returns: "otherCommand PlayerName %another_placeholder%"
 */

public final class RangePlaceholder extends Placeholder {

    private final ItsMyConfig plugin = ItsMyConfig.getInstance();
    private final String defaultValue;
    private final long[] starts;
    private final long[] ends;
    private final String[] values;

    public RangePlaceholder(
            final String filePath,
            final ConfigurationSection section
    ) {
        super(section, filePath, PlaceholderType.RANGE, PlaceholderDependancy.NONE);

        this.defaultValue = section.getString("default", "");

        final ConfigurationSection cfg = section.getConfigurationSection("values");
        if (cfg == null) {
            this.starts = new long[0];
            this.ends = new long[0];
            this.values = new String[0];
            return;
        }

        // Parse all entries
        final List<Entry> entries = new ArrayList<>();
        for (final String key : cfg.getKeys(false)) {
            final Object raw = cfg.get(key);
            final String value = raw == null ? "" : String.valueOf(raw);

            final Range r = parseRangeKey(key);
            if (r == null) {
                warn(section, "Invalid range key '" + key + "' (skipped). Expected A-B, -B, or A-");
                continue;
            }
            if (r.start > r.end) {
                warn(section, "Range key '" + key + "' has start > end (skipped).");
                continue;
            }

            entries.add(new Entry(r.start, r.end, value, key));
        }

        // Sort by start, then end
        entries.sort(Comparator
                .comparingLong((Entry e) -> e.start)
                .thenComparingLong(e -> e.end));

        // Detect overlaps: "first wins", later overlapping entries are skipped with warning
        final List<Entry> filtered = new ArrayList<>(entries.size());
        Entry prev = null;
        for (final Entry cur : entries) {
            if (prev != null && cur.start <= prev.end) {
                warn(section,
                        "Overlapping ranges: '" + prev.originalKey + "' [" + prev.start + "-" + prev.end + "] "
                                + "overlaps with '" + cur.originalKey + "' [" + cur.start + "-" + cur.end + "]. "
                                + "Keeping the first, skipping '" + cur.originalKey + "'.");
                continue;
            }
            filtered.add(cur);
            prev = cur;
        }

        // Store in arrays for fast lookup
        final int n = filtered.size();
        final long[] s = new long[n];
        final long[] e = new long[n];
        final String[] v = new String[n];

        for (int i = 0; i < n; i++) {
            final Entry it = filtered.get(i);
            s[i] = it.start;
            e[i] = it.end;
            v[i] = it.value;
        }

        this.starts = s;
        this.ends = e;
        this.values = v;
    }

    @Override
    public String getResult(
            final OfflinePlayer player,
            final String[] args
    ) {
        if (args.length == 0) {
            return defaultValue;
        }

        final long x;
        try {
            x = Long.parseLong(args[0]);
        } catch (final Exception ignored) {
            return defaultValue;
        }

        final int idx = findRangeIndex(x);
        if (idx < 0) {
            return defaultValue;
        }

        final String template = values[idx];
        if (template == null || template.isEmpty()) {
            return defaultValue;
        }

        if (args.length == 1) {
            return template;
        }

        return applyArgs(template, args);
    }

    /**
     * Binary search by start, then check containment.
     */
    private int findRangeIndex(final long x) {
        if (starts.length == 0) return -1;

        int lo = 0, hi = starts.length - 1;
        int best = -1;

        // find rightmost start <= x
        while (lo <= hi) {
            final int mid = (lo + hi) >>> 1;
            if (starts[mid] <= x) {
                best = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }

        if (best == -1) return -1;
        return (x <= ends[best]) ? best : -1;
    }

    private static Range parseRangeKey(final String keyRaw) {
        if (keyRaw == null) return null;
        final String key = keyRaw.trim();
        if (key.isEmpty()) return null;

        // "-B"
        if (key.startsWith("-") && key.length() > 1) {
            final long end = parseLongSafe(key.substring(1));
            if (end == Long.MIN_VALUE) return null;
            return new Range(Long.MIN_VALUE, end);
        }

        // "A-"
        if (key.endsWith("-") && key.length() > 1) {
            final long start = parseLongSafe(key.substring(0, key.length() - 1));
            if (start == Long.MIN_VALUE) return null;
            return new Range(start, Long.MAX_VALUE);
        }

        // "A-B"
        final int dash = key.indexOf('-');
        if (dash <= 0 || dash >= key.length() - 1) return null;

        final long start = parseLongSafe(key.substring(0, dash));
        final long end = parseLongSafe(key.substring(dash + 1));
        if (start == Long.MIN_VALUE || end == Long.MIN_VALUE) return null;

        return new Range(start, end);
    }

    private static long parseLongSafe(final String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (final Exception ignored) {
            return Long.MIN_VALUE;
        }
    }

    private static String applyArgs(final String template, final String[] args) {
        String out = template;
        // args[0] is the number; replacements start from args[1] -> {0}
        for (int i = 1; i < args.length; i++) {
            out = out.replace("{" + (i - 1) + "}", args[i]);
        }
        return out;
    }

    private void warn(final ConfigurationSection section, final String msg) {
        plugin.getLogger().warning("Range placeholder misconfig at '" + section.getCurrentPath() + "': " + msg);
    }

    private record Range(long start, long end) {}

    private record Entry(long start, long end, String value, String originalKey) {}
}
