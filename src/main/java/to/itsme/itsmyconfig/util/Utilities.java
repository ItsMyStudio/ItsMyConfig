package to.itsme.itsmyconfig.util;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.font.Font;
import to.itsme.itsmyconfig.font.FontTag;
import to.itsme.itsmyconfig.placeholder.PlaceholderData;
import to.itsme.itsmyconfig.placeholder.type.ColorPlaceholderData;
import to.itsme.itsmyconfig.tag.TagManager;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * The Utilities class provides various utility methods for performing common tasks.
 */
public final class Utilities {

    private static final ItsMyConfig plugin = ItsMyConfig.getInstance();

    public static final MiniMessage MM, EMPTY_MM;

    private static final TagResolver FONT_RESOLVER;
    private static final Field TEXT_COMPONENT_CONTENT;

    static {
        final TagResolver.Builder builder = TagResolver.builder();
        for (final @Subst("") Font font : Font.values()) {
            builder.tag(font.getName(), new FontTag(font));
        }
        FONT_RESOLVER = builder.build();
        MM = MiniMessage.builder()
                .tags(
                        TagResolver.builder()
                                .resolvers(
                                        StandardTags.defaults(),
                                        FONT_RESOLVER
                                ).build()
                ).build();
        EMPTY_MM = MiniMessage.builder().tags(TagResolver.empty()).build();
        try {
            final Class<?> textComponentImpClazz = Class.forName("net.kyori.adventure.text.TextComponentImpl");
            final Field contentField = textComponentImpClazz.getDeclaredField("content");
            contentField.setAccessible(true);
            TEXT_COMPONENT_CONTENT = contentField;
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Logs debug information to the console if the debug mode is enabled.
     *
     * @param text The debug information to log.
     */
    public static void debug(final String text) {
        if (plugin.getConfig().getBoolean("debug")) {
            plugin.getLogger().info(text);
        }
    }

    /**
     * Logs debug information along with an exception stack trace to the console if the debug mode is enabled.
     *
     * @param text      The debug information to log.
     * @param exception The Exception object representing the exception to log.
     */
    public static void debug(final String text, final Exception exception) {
        if (plugin.getConfig().getBoolean("debug")) {
            plugin.getLogger().log(Level.SEVERE, text, exception);
        }
    }

    /**
     * Translates a String into a component
     *
     * @param text The text to translate.
     * @param player The player translated-for.
     * @return The translated component.
     */
    public static Component translate(
            final String text,
            final Player player,
            final TagResolver... placeholders
    ) {
        final Component translated = fixClickEvent(
                EMPTY_MM.deserialize(
                        TagManager.process(
                                player, Strings.quote(text)
                        ),
                        itsMyConfigTag(player), papiTag(player),
                        FONT_RESOLVER, StandardTags.defaults(), playerSubtags(player),
                        TagResolver.resolver(placeholders)
                )
        );

        applyChatColors(translated);
        return translated;
    }

    /**
     * Serialized then deserialized components with a click event have their value starting with "&f"
     * <br>
     * This fixes it.
     *
     * @return  the fixed component
     */
    private static Component fixClickEvent(final Component component) {
        final ClickEvent event = component.clickEvent();
        Component copied = component;

        // Serialized then deserialized components with a click event have their value starting with "&f".
        if (event != null && event.value().startsWith("&f")) {
            copied = component.clickEvent(ClickEvent.clickEvent(event.action(), event.value().substring(2)));
        }

        copied = copied.children(copied.children().stream().map(Utilities::fixClickEvent).collect(Collectors.toList()));
        return copied;
    }

    /**
     * Resolves all possible tags
     *
     * @param player The player for whom the resolver is being created.
     * @return All {@link TagResolver}s of th player combined.
     */
    public static TagResolver playerTag(final Player player) {
        return TagResolver.resolver(
                itsMyConfigTag(player), papiTag(player), playerSubtags(player)
        );
    }

    /**
     * Resolves all possible tags except main ones
     *
     * @param player The player for whom the resolver is being created.
     * @return All {@link TagResolver}s of th player combined.
     */
    public static TagResolver playerSubtags(final Player player) {
        return TagResolver.resolver(
                titleTag(player), subtitleTag(player),
                actionbarTag(player), soundTag(player)
        );
    }

    /**
     * Provides a ItsMyConfig placeholders tag resolver.
     *
     * @param player The player for whom the resolver is being created.
     * @return The ItsMyConfig placeholder tag resolver.
     */
    public static TagResolver itsMyConfigTag(final Player player) {
        return TagResolver.resolver("p", (argumentQueue, context) -> {
            if (!argumentQueue.hasNext()) {
                return Tag.preProcessParsed("Unknown Placeholder");
            }

            final String name = argumentQueue.popOr("").value();
            final PlaceholderData data = plugin.getPlaceholderManager().get(name);
            if (data == null) {
                return Tag.preProcessParsed("Unknown Placeholder");
            }

            if (data instanceof ColorPlaceholderData) {
                return Tag.styling(builder -> builder.merge(((ColorPlaceholderData) data).getStyle()));
            }

            final List<String> args = new LinkedList<>();
            while (argumentQueue.hasNext()) {
                args.add(argumentQueue.pop().value());
            }

            final String parsed = data.asString(player, args.toArray(new String[0]));
            return Tag.preProcessParsed(parsed == null ? "" : parsed);
        });
    }

    /**
     * Provides a PlaceholderAPI tag resolver.
     *
     * @param player The player for whom the resolver is being created.
     * @return The PlaceholderAPI tag resolver.
     */
    public static TagResolver papiTag(final Player player) {
        return TagResolver.resolver("papi", (argumentQueue, context) -> {
            final String papiPlaceholder = argumentQueue.popOr("papi tag requires an argument").value();
            final String parsedPlaceholder = PlaceholderAPI.setPlaceholders(player, '%' + papiPlaceholder + '%');
            return Tag.preProcessParsed(parsedPlaceholder.replace("§", "&"));
        });
    }

    /**
     * Provides a title tag resolver.
     *
     * @param player The player for whom the resolver is being created.
     * @return The title tag resolver.
     */
    public static TagResolver titleTag(final Player player) {
        return TagResolver.resolver("title", (argumentQueue, context) -> {
            final List<Tag.Argument> args = new ArrayList<>();
            while (argumentQueue.hasNext()) {
                args.add(argumentQueue.pop());
            }

            if (args.size() == 1) {
                final Title title = Title.title(MM.deserialize(args.get(0).value(), playerTag(player)), Component.empty());
                plugin.adventure().player(player).showTitle(title);
            } else if (args.size() == 2) {
                final Title title = Title.title(MM.deserialize(args.get(0).value()), MM.deserialize(args.get(1).value(), playerTag(player)));
                plugin.adventure().player(player).showTitle(title);
            } else if (args.size() == 4) {
                final Title.Times times = createTimes(
                        args.get(0).asInt(),
                        args.get(1).asInt(),
                        args.get(2).asInt()
                );
                final Title title = Title.title(
                        MM.deserialize(args.get(3).value(), playerTag(player)),
                        Component.empty(),
                        times
                );
                plugin.adventure().player(player).showTitle(title);
            } else if (args.size() == 5) {
                final Title.Times times = createTimes(
                        args.get(0).asInt(),
                        args.get(1).asInt(),
                        args.get(2).asInt()
                );
                final Title title = Title.title(
                        MM.deserialize(args.get(3).value(), playerTag(player)),
                        MM.deserialize(args.get(4).value(), playerTag(player)),
                        times
                );
                plugin.adventure().player(player).showTitle(title);
            } else {
                return Tag.preProcessParsed("Invalid title tag arguments");
            }

            return Tag.preProcessParsed("");
        });
    }

    /**
     * Provides a subtitle tag resolver.
     *
     * @param player The player for whom the resolver is being created.
     * @return The subtitle tag resolver.
     */
    public static TagResolver subtitleTag(final Player player) {
        return TagResolver.resolver("subtitle", (argumentQueue, context) -> {
            final List<Tag.Argument> args = new ArrayList<>();
            while (argumentQueue.hasNext()) {
                args.add(argumentQueue.pop());
            }

            if (args.size() == 1) {
                final Title title = Title.title(Component.empty(), MM.deserialize(args.get(0).value(), playerTag(player)));
                plugin.adventure().player(player).showTitle(title);
            } else if (args.size() == 4) {
                final Title.Times times = createTimes(
                        args.get(0).asInt(),
                        args.get(1).asInt(),
                        args.get(2).asInt()
                );
                final Title title = Title.title(
                        Component.empty(),
                        MM.deserialize(args.get(3).value(), playerTag(player)),
                        times
                );
                plugin.adventure().player(player).showTitle(title);
            } else {
                return Tag.preProcessParsed("Invalid subtitle tag arguments");
            }

            return Tag.preProcessParsed("");
        });
    }

    /**
     * Provides an action bar tag resolver.
     *
     * @param player The player for whom the resolver is being created.
     * @return The action bar tag resolver.
     */
    public static TagResolver actionbarTag(final Player player) {
        return TagResolver.resolver("actionbar", (argumentQueue, context) -> {
            final String bar = argumentQueue.popOr("Invalid actionbar value").value();
            plugin.adventure().player(player).sendActionBar(MM.deserialize(bar, playerTag(player)));
            return Tag.selfClosingInserting(Component.empty());
        });
    }

    /**
     * Provides a sound player tag resolver.
     *
     * @param player The player for whom the resolver is being created.
     * @return The sound tag resolver.
     */
    public static TagResolver soundTag(final Player player) {
        return TagResolver.resolver("sound", (argumentQueue, context) -> {
            final List<Tag.Argument> args = new ArrayList<>();
            while (argumentQueue.hasNext()) {
                args.add(argumentQueue.pop());
            }

            if (args.size() == 1) {
                final Sound sound = Sound.valueOf(args.get(0).value());
                player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
            } else if (args.size() == 3) {
                final Sound sound = Sound.valueOf(args.get(0).value());
                final float volume = (float) args.get(1).asDouble().orElse(1.0D);
                final float pitch = (float) args.get(2).asDouble().orElse(1.0D);
                player.playSound(player.getLocation(), sound, volume, pitch);
            } else {
                return Tag.preProcessParsed("Invalid sound tag arguments");
            }

            return Tag.preProcessParsed("");
        });
    }

    /**
     * Applies chat colors to a root component and its children recursively.
     *
     * @param rootComponent The root component to apply chat colors to.
     */
    public static void applyChatColors(final Component rootComponent) {
        if (rootComponent instanceof TextComponent) {
            final TextComponent textComponent = (TextComponent) rootComponent;
            final String translateAlternateColorCodes = ChatColor.translateAlternateColorCodes('&', textComponent.content());
            modifyTextComponent(textComponent, translateAlternateColorCodes);
            for (final Component component : rootComponent.children()) {
                applyChatColors(component);
            }
        }
    }

    /**
     * Converts a list of objects to a string, where each object is represented on a new line.
     *
     * @param list The list of objects to be converted to a string.
     * @return The string representation of the list.
     */
    public static String toString(final @NotNull List<?> list) {
        return String.join(System.lineSeparator(), list.stream().map(Object::toString).toArray(String[]::new));
    }

    /**
     * Helper method to create Title times with optional values.
     *
     * @param fadeIn  The fade in time.
     * @param stay    The stay time.
     * @param fadeOut The fade out time.
     * @return Title.Times object with specified times.
     */
    @SuppressWarnings("all")
    private static Title.Times createTimes(
            final @NotNull OptionalInt fadeIn,
            final @NotNull OptionalInt stay,
            final @NotNull OptionalInt fadeOut
    ) {
        return Title.Times.times(
                Ticks.duration(fadeIn.orElse(10)),
                Ticks.duration(stay.orElse(70)),
                Ticks.duration(fadeOut.orElse(20))
        );
    }

    /**
     * Modifies the content of a TextComponent instance.
     *
     * @param component The TextComponent instance to modify.
     * @param content       The new content for the TextComponent.
     */
    private static void modifyTextComponent(
            final @NotNull TextComponent component,
            final @Nullable String content
    ) {
        try {
            TEXT_COMPONENT_CONTENT.set(component, content);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
