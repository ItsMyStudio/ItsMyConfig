package to.itsme.itsmyconfig.placeholder.type;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.placeholder.PlaceholderDependancy;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;
import to.itsme.itsmyconfig.requirement.Requirement;
import to.itsme.itsmyconfig.requirement.RequirementManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ConditionalPlaceholder extends Placeholder {

    private static final RequirementManager REQUIREMENTS = new RequirementManager();

    private final List<Condition> conditions = new ArrayList<>();
    private final String trueValue;
    private final String falseValue;

    public ConditionalPlaceholder(
            final String filePath,
            final ConfigurationSection section
    ) {
        super(section, filePath, PlaceholderType.CONDITIONAL, PlaceholderDependancy.NONE);

        this.trueValue = section.getString("true", "");
        this.falseValue = section.getString("false", "");
        this.registerArguments(this.trueValue);
        this.registerArguments(this.falseValue);

        List<Map<?, ?>> conditionMaps = section.getMapList("conditions");
        if (conditionMaps.isEmpty()) {
            conditionMaps = section.getMapList("condtions"); // common typo fallback
        }

        for (final Map<?, ?> conditionMap : conditionMaps) {
            final Condition condition = Condition.from(conditionMap);
            this.conditions.add(condition);
            this.registerArguments(condition.input());
            this.registerArguments(condition.output());
            if (condition.falseValue() != null) {
                this.registerArguments(condition.falseValue());
            }
        }
    }

    /**
     * Evaluates the condition list in declaration order (AND-like semantics).
     *
     * <p>Returns the first matching condition's {@code falseValue}
     * (or the global {@link #falseValue} if not set per-condition).
     * If <strong>all</strong> conditions match, returns {@link #trueValue}.</p>
     */
    @Override
    public String getResult(final OfflinePlayer player, final String[] args) {
        for (final Condition condition : this.conditions) {
            if (!condition.matches(player, args)) {
                final String output = condition.falseValue() == null ? this.falseValue : condition.falseValue();
                return this.parse(player, args, output);
            }
        }

        return this.parse(player, args, this.trueValue);
    }

    private String parse(final OfflinePlayer player, final String[] args, final String value) {
        final String replaced = this.replaceArguments(args, value == null ? "" : value);
        return player == null ? replaced : PlaceholderAPI.setPlaceholders(player, replaced);
    }

    private record Condition(String type, String input, String output, String falseValue) {

        private static Condition from(final Map<?, ?> map) {
            return new Condition(
                    value(map, "type"),
                    value(map, "input"),
                    value(map, "output"),
                    nullableValue(map, "false")
            );
        }

        private static String value(final Map<?, ?> map, final String key) {
            final String value = nullableValue(map, key);
            return value == null ? "" : value;
        }

        private static String nullableValue(final Map<?, ?> map, final String key) {
            Object value = map.get(key);
            if (value == null) {
                for (final Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() != null && entry.getKey().toString().equals(key)) {
                        value = entry.getValue();
                        break;
                    }
                }
            }
            return value == null ? null : String.valueOf(value);
        }

        private static String parse(final OfflinePlayer player, final String[] args, final String value) {
            String output = value == null ? "" : value;
            for (int i = 0; i < args.length; i++) {
                output = output.replace("{" + i + "}", args[i]);
            }
            return player == null ? output : PlaceholderAPI.setPlaceholders(player, output);
        }

        /**
         * Delegates to the {@link Requirement} matching this condition's type.
         *
         * <p>Supports {@code string}, {@code number}, and {@code regex} requirement types.</p>
         *
         * @return {@code false} if the requirement type is unknown or the validation fails
         */
        private boolean matches(final OfflinePlayer player, final String[] args) {
            final Requirement<?> requirement = REQUIREMENTS.getRequirementByType(this.type);
            if (requirement == null) {
                return false;
            }

            return requirement.validate(
                    this.type,
                    parse(player, args, this.input),
                    parse(player, args, this.output)
            );
        }
    }
}
