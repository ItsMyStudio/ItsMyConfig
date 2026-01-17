package to.itsme.itsmyconfig.util;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.font.Font;
import to.itsme.itsmyconfig.font.FontTag;
import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.placeholder.type.ColorPlaceholder;
import to.itsme.itsmyconfig.tag.TagManager;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * The Utilities class provides various utility methods for performing common tasks.
 */
public final class Utilities {

    private static final ItsMyConfig plugin = ItsMyConfig.getInstance();

    public static final MiniMessage MM, EMPTY_MM;
    public static final GsonComponentSerializer GSON_SERIALIZER = BukkitComponentSerializer.gson();
    public static final BungeeComponentSerializer BUNGEE_SERIALIZER = BungeeComponentSerializer.get();

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
            final Class<?> textComponentImpClazz = Component.empty().getClass(); // Should be net.kyori.adventure.text.TextComponentImpl class
            final Field contentField = textComponentImpClazz.getDeclaredField("content");
            contentField.setAccessible(true);
            TEXT_COMPONENT_CONTENT = contentField;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Logs debug information to the console if the debug mode is enabled.
     *
     * @param supplier The supplier that provides the debug information to log.
     */
    public static void debug(final Supplier<String> supplier) {
        if (plugin.isDebug()) {
            final String[] splitText = supplier.get().split("\\n");
            for (final String text : splitText) {
                plugin.getLogger().info(text);
            }
        }
    }

    /**
     * Logs debug information along with an exception stack trace to the console if the debug mode is enabled.
     *
     * @param supplier The supplier that provides the debug information to log.
     * @param exception The Exception object representing the exception to log.
     */
    public static void debug(final Supplier<String> supplier, final Throwable exception) {
        if (plugin.isDebug()) {
            plugin.getLogger().log(Level.SEVERE, supplier.get(), exception);
        }
    }

    /**
     * Translates a String into a {@link Component}
     *
     * @param text The text to translate.
     * @return The translated {@link Component}.
     */
    public static @NotNull Component translate(
            final String text,
            final TagResolver... placeholders
    ) {
        final Component translated = EMPTY_MM.deserialize(
                Strings.quote(text),
                emptyItsMyConfigTag(),
                FONT_RESOLVER, StandardTags.defaults(),
                TagResolver.resolver(placeholders)
        );

        applyChatColors(translated);
        return cleanUp(translated);
    }

    /**
     * Translates a String into a {@link Component}
     *
     * @param text The text to translate.
     * @param player The player translated-for.
     * @return The translated {@link Component}.
     */
    public static @NotNull Component translate(
            final String text,
            final OfflinePlayer player,
            final TagResolver... placeholders
    ) {
        if (player.isOnline()) {
            return translate(text, player.getPlayer(), placeholders);
        }

        final Component translated = EMPTY_MM.deserialize(
                Strings.quote(text),
                itsMyConfigTag(player), papiTag(player),
                FONT_RESOLVER, StandardTags.defaults(),
                TagResolver.resolver(placeholders)
        );

        applyChatColors(translated);
        return cleanUp(translated);
    }

    /**
     * Translates a String into a {@link Component}
     *
     * @param text The text to translate.
     * @param player The player translated-for.
     * @return The translated {@link Component}.
     */
    public static @NotNull Component translate(
            final String text,
            final Player player,
            final TagResolver... placeholders
    ) {
        final Component translated = EMPTY_MM.deserialize(
                TagManager.process(
                        player, Strings.quote(text)
                ),
                itsMyConfigTag(player), papiTag(player),
                FONT_RESOLVER, StandardTags.defaults(),
                TagResolver.resolver(placeholders)
        );

        applyChatColors(translated);
        return cleanUp(translated);
    }

    /**
     * Provides a ItsMyConfig placeholders tag resolver.
     *
     * @return The ItsMyConfig placeholder tag resolver.
     */
    public static TagResolver emptyItsMyConfigTag() {
        return TagResolver.resolver("p", (argumentQueue, context) -> {
            if (!argumentQueue.hasNext()) {
                return Tag.preProcessParsed("Unknown Placeholder");
            }

            final String name = argumentQueue.popOr("").value();
            final Placeholder data = plugin.getPlaceholderManager().get(name);
            if (data == null) {
                return Tag.preProcessParsed("Unknown Placeholder");
            }

            if (data instanceof ColorPlaceholder colorPlaceholder) {
                return colorPlaceholder.getStyle();
            }

            final List<String> args = new LinkedList<>();
            while (argumentQueue.hasNext()) {
                args.add(argumentQueue.pop().value());
            }

            final String parsed = data.asString(args.toArray(new String[0]));
            return Tag.preProcessParsed((parsed == null ? "" : parsed).replace("§", "&"));
        });
    }

    /**
     * Provides a ItsMyConfig placeholders tag resolver.
     *
     * @param player The player for whom the resolver is being created.
     * @return The ItsMyConfig placeholder tag resolver.
     */
    public static TagResolver itsMyConfigTag(final OfflinePlayer player) {
        return TagResolver.resolver("p", (argumentQueue, context) -> {
            if (!argumentQueue.hasNext()) {
                return Tag.preProcessParsed("Unknown Placeholder");
            }

            final String name = argumentQueue.popOr("").value();
            final Placeholder data = plugin.getPlaceholderManager().get(name);
            if (data == null) {
                return Tag.preProcessParsed("Unknown Placeholder");
            }

            if (data instanceof ColorPlaceholder colorPlaceholder) {
                return colorPlaceholder.getStyle();
            }

            final List<String> args = new LinkedList<>();
            while (argumentQueue.hasNext()) {
                args.add(argumentQueue.pop().value());
            }

            final String parsed = data.asString(player, args.toArray(new String[0]));
            return Tag.preProcessParsed((parsed == null ? "" : parsed).replace("§", "&"));
        });
    }

    /**
     * Provides a PlaceholderAPI tag resolver.
     *
     * @param player The player for whom the resolver is being created.
     * @return The PlaceholderAPI tag resolver.
     */
    public static TagResolver papiTag(final OfflinePlayer player) {
        return TagResolver.resolver("papi", (argumentQueue, context) -> {
            final String papiPlaceholder = argumentQueue.popOr("papi tag requires an argument").value();
            final String parsedPlaceholder = PlaceholderAPI.setPlaceholders(player, '%' + papiPlaceholder + '%');
            return Tag.preProcessParsed(parsedPlaceholder.replace("§", "&"));
        });
    }

    /**
     * Cleans up a Component by removing all decorations that are explicitly set to false.
     * This is applied recursively to all children.
     *
     * @param component The Component to clean up.
     * @return The cleaned-up Component.
     */
    public static Component cleanUp(final Component component) {
        // Clean decorations at this level
        final Map<TextDecoration, TextDecoration.State> decorations = component.decorations();
        final Map<TextDecoration, TextDecoration.State> cleanedDecorations = decorations.entrySet().stream()
                .filter(entry -> entry.getValue() != TextDecoration.State.FALSE)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Recursively clean children
        final List<Component> cleanedChildren = component.children().stream()
                .map(Utilities::cleanUp)
                .collect(Collectors.toList());

        return component.decorations(cleanedDecorations).children(cleanedChildren);
    }

    /**
     * Applies chat colors to a root component and its children recursively.
     *
     * @param rootComponent The root component to apply chat colors to.
     */
    @SuppressWarnings("all")
    @Deprecated(forRemoval = true)
    public static void applyChatColors(final Component rootComponent) {
        if (rootComponent instanceof TextComponent textComponent) {
            final String translateAlternateColorCodes = ChatColor.translateAlternateColorCodes('&', textComponent.content());
            modifyTextComponent(textComponent, translateAlternateColorCodes);
            for (final Component component : rootComponent.children()) {
                applyChatColors(component);
            }
        }
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
