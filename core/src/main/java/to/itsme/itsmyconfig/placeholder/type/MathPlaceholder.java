package to.itsme.itsmyconfig.placeholder.type;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import redempt.crunch.CompiledExpression;
import redempt.crunch.Crunch;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.placeholder.PlaceholderDependancy;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;
import to.itsme.itsmyconfig.util.Utilities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

public final class MathPlaceholder extends Placeholder {

    private static final NavigableMap<Long, String> GLOBAL_SUFFIXES = new TreeMap<>();
    private static final DecimalFormat FIXED_FORMAT = new DecimalFormat("#");
    private static final DecimalFormat COMMAS_FORMAT = new DecimalFormat("#,###");

    static {
        UPDATE_FORMATTINGS();
    }

    public static void UPDATE_FORMATTINGS() {
        final FileConfiguration config = ItsMyConfig.getInstance().getConfig();
        GLOBAL_SUFFIXES.put(1_000L, config.getString("formatting.thousands", "k"));
        GLOBAL_SUFFIXES.put(1_000_000L, config.getString("formatting.millions", "M"));
        GLOBAL_SUFFIXES.put(1_000_000_000L, config.getString("formatting.billions", "B"));
        GLOBAL_SUFFIXES.put(1_000_000_000_000L, config.getString("formatting.trillions", "T"));
        GLOBAL_SUFFIXES.put(1_000_000_000_000_000L, config.getString("formatting.quadrillions", "Q"));
    }

    private final CompiledExpression expression;
    private final int variablesRequired;

    private final int precision;
    private final RoundingMode mode;

    public MathPlaceholder(
            final String filePath,
            final ConfigurationSection section
    ) {
        super(section, filePath, PlaceholderType.MATH, PlaceholderDependancy.NONE);
        final String value = section.getString("value", "0");
        this.registerArguments(value);

        this.precision = section.getInt("precision");
        this.mode = RoundingMode.valueOf(section.getString("mode", "HALF_UP"));

        String copy = value;
        for (final int argument : this.arguments) {
            copy = copy.replace("{" + argument + "}", "$" + (argument + 1));
        }

        this.expression = Crunch.compileExpression(copy);
        this.variablesRequired = this.expression.getVariableCount();

        this.compiledPlaceholders = Set.of(
                mainCompiledPlaceholder(),
                this.compileVariant("commas", this::getCommasResult, this.variablesRequired, this.variablesRequired),
                this.compileVariant("fixed", this::getFixedResult, this.variablesRequired, this.variablesRequired),
                this.compileVariant("formatted", this::getFormattedResult, this.variablesRequired, this.variablesRequired)
        );
    }

    @Override
    public int minArgs() {
        return this.variablesRequired;
    }

    @Override
    public int maxArgs() {
        return this.variablesRequired;
    }

    @Override
    public String getResult(
            final OfflinePlayer player,
            final String[] args
    ) {
        final Double result = this.evaluate(args);
        if (result == null) {
            return this.invalidResult(args);
        }
        return new BigDecimal(result).setScale(this.precision, this.mode).stripTrailingZeros().toPlainString();
    }

    private String getCommasResult(final OfflinePlayer player, final String[] args) {
        final Double result = this.evaluate(args);
        return this.asVariantString(player, result == null ? this.invalidResult(args) : COMMAS_FORMAT.format(result));
    }

    private String getFixedResult(final OfflinePlayer player, final String[] args) {
        final Double result = this.evaluate(args);
        return this.asVariantString(player, result == null ? this.invalidResult(args) : FIXED_FORMAT.format(result));
    }

    private String getFormattedResult(final OfflinePlayer player, final String[] args) {
        final Double result = this.evaluate(args);
        return this.asVariantString(player, result == null ? this.invalidResult(args) : formatNumber(result.longValue()));
    }

    private Double evaluate(final String[] args) {
        if (args.length < variablesRequired) {
            return null;
        }

        final double[] vals = this.convertArray(args, variablesRequired);
        if (vals == null) {
            return null;
        }

        return expression.evaluate(vals);
    }

    private String invalidResult(final String[] args) {
        if (args.length < variablesRequired) {
            return String.format("Invalid variable count, provided: %d, required: %d", args.length, variablesRequired);
        }
        return "One of the arguments is an invalid number";
    }

    public double[] convertArray(final String[] args, final int limit) {
        final double[] doubleArgs = new double[limit];
        for (int i = 0; i < limit; i++) {
            final String arg = args[i];
            try {
                doubleArgs[i] = Double.parseDouble(arg);
            } catch (final NumberFormatException e) {
                Utilities.debug(() -> arg + " is not a number, returning null");
                return null;
            }
        }
        return doubleArgs;
    }

    public int variablesRequired() {
        return this.variablesRequired;
    }

    @SuppressWarnings("all")
    private String formatNumber(long balance) {
        if (balance == Long.MIN_VALUE) {
            return formatNumber(Long.MIN_VALUE + 1);
        }
        if (balance < 0) {
            return "-" + formatNumber(-balance);
        }

        if (balance < 1000) {
            return Long.toString(balance);
        }

        final Map.Entry<Long, String> e = GLOBAL_SUFFIXES.floorEntry(balance);
        final Long divideBy = e.getKey();
        final String suffix = e.getValue();

        final long truncated = balance / (divideBy / 10);
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }

}
