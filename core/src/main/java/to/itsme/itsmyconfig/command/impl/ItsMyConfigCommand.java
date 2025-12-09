package to.itsme.itsmyconfig.command.impl;

import dev.velix.imperat.BukkitSource;
import dev.velix.imperat.annotations.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.command.util.PlayerSelector;
import to.itsme.itsmyconfig.message.AudienceResolver;
import to.itsme.itsmyconfig.placeholder.Placeholder;
import to.itsme.itsmyconfig.placeholder.PlaceholderType;
import to.itsme.itsmyconfig.message.Message;
import to.itsme.itsmyconfig.util.Utilities;

import java.io.IOException;

@Command("itsmyconfig")
@Permission("itsmyconfig.admin")
@SuppressWarnings("deprecation")
public final class ItsMyConfigCommand {

    private final ItsMyConfig plugin;

    public ItsMyConfigCommand(final ItsMyConfig plugin) {
        this.plugin = plugin;
    }

    @Usage
    @SubCommand("help")
    public void usage(final BukkitSource source) {
        final String message = """
                  <gold><plugin></gold> | Config has never been easier
                
                    <gray>• <white>/itsmyconfig reload
                    <gray>• <white>/itsmyconfig parse <gold><target> <message>
                    <gray>• <white>/itsmyconfig message <gold><target> <message>
                    <gray>• <white>/itsmyconfig config <gold><placeholder> <value>
                
                  <gray>• <white>Project: <aqua>ItsMe.to
                  <gray>• <white>Support: <click:open_url:'https://discord.gg/itsme-to'><green>discord.gg/itsme-to</click>
                  <gray>• <white>Developer: <yellow><author> <gray>(%s)
                
                """.formatted(plugin.getDescription().getVersion());

        AudienceResolver.send(source, Utilities.MM.deserialize(message, pluginInfo(), authorInfo()));
    }

    private TagResolver pluginInfo() {
        final PluginDescriptionFile description = plugin.getDescription();

        // Create a text block for the hover message
        final String hoverMessage = """
            <white>Name: <gold>%s
            <white>Version: <gold>%s
            <white>Support Server: <gold>https://discord.gg/itsme-to
            """.formatted(description.getName(), description.getVersion());

        // Return the TagResolver with the plugin information
        return TagResolver.resolver("plugin", (argumentQueue, context) -> {
            assert description.getWebsite() != null;
            return Tag.selfClosingInserting(
                    Component.text()
                            .content(description.getName())
                            .decorate(TextDecoration.BOLD)
                            .hoverEvent(Utilities.MM.deserialize(hoverMessage))
                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, description.getWebsite()))
            );
        });
    }

    private TagResolver authorInfo() {
        // Create a text block for the hover message
        final String hoverMessage = """
            <white>Discord: <aqua>@iiAhmedYT</aqua>
            <white>Github: <aqua>https://github.com/iiAhmedYT</aqua>
            """;

        // Return the TagResolver with the author information
        return TagResolver.resolver("author", (argumentQueue, context) -> Tag.selfClosingInserting(
                Component.text()
                        .content("iiAhmedYT")
                        .decorate(TextDecoration.UNDERLINED)
                        .hoverEvent(Utilities.MM.deserialize(hoverMessage))
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/iiAhmedYT"))
        ));
    }

    @SubCommand("reload")
    @Permission("itsmyconfig.reload")
    @Description("Reloads the plugin config")
    public void reload(final BukkitSource source) {
        plugin.loadConfig();
        Message.RELOAD.send(source);
    }

    @SubCommand("messagedirectly")
    @Permission("itsmyconfig.message")
    @Description("Sends messages to players")
    public void messageDirectly(
            final BukkitSource source,
            final @Named("target") PlayerSelector players,
            final @Named("message") @Greedy String message
    ) {
        for (final Player player : players) {
            player.sendMessage(this.plugin.getSymbolPrefix() + message);
        }

        if (!source.isConsole()) {
            Message.MESSAGE_SENT.send(source);
        }
    }

    @SubCommand("message")
    @Permission("itsmyconfig.message")
    @Description("Sends messages to players")
    public void message(
            final BukkitSource source,
            final @Named("target") PlayerSelector players,
            final @Named("message") @Greedy String message
    ) {
        for (final Player player : players) {
            final Component component = Utilities.translate(message, player);
            if (!Component.empty().equals(component)) {
                AudienceResolver.resolve(player).sendMessage(component);
            }
        }

        if (!source.isConsole()) {
            Message.MESSAGE_SENT.send(source);
        }
    }


    @SubCommand("parse")
    @Permission("itsmyconfig.parse")
    @Description("Parse messages as players")
    public void parse(
            final BukkitSource source,
            final @Named("target") PlayerSelector players,
            final @Named("message") @Greedy String message
    ) {
        for (final Player player : players) {
            final Component component = Utilities.translate(message, player);
            if (!Component.empty().equals(component)) {
                AudienceResolver.resolve(source.origin()).sendMessage(component);
            }
        }

        if (!source.isConsole()) {
            Message.MESSAGE_SENT.send(source);
        }
    }

    @SubCommand("debug")
    @Permission("itsmyconfig.debug")
    @Description("Toggles debug mode for messages (not permanent)")
    public void debug(final BukkitSource source) {
        final boolean debug = plugin.toggleDebug();
        final String message = debug ? "<green>Debug mode enabled!</green>" : "<red>Debug mode disabled!</red>";
        AudienceResolver.send(source, Utilities.MM.deserialize(message));
        if (debug) {
            plugin.getLogger().info("Debug mode is now enabled.");
        } else {
            plugin.getLogger().info("Debug mode is now disabled.");
        }
    }

    @SubCommand("config")
    @Permission("itsmyconfig.config")
    @Description("Sets config values for placeholder")
    public void config(
            final BukkitSource source,
            @SuggestionProvider("ModifiablePlaceholder") final Placeholder placeholder,
            @Named("value") @Greedy final String value
    ) {
        final ConfigurationSection section = placeholder.getConfigurationSection();
        final PlaceholderType type = PlaceholderType.find(section.getString("type"));
        if (type == PlaceholderType.ANIMATION || type == PlaceholderType.RANDOM) {
            AudienceResolver.send(source, Utilities.MM.deserialize("<red>Placeholder <yellow>" + placeholder + "</yellow>'s type is not supported via commands.</red>"));
            return;
        }

        section.set("value", value);
        final Configuration root = section.getRoot();
        if (root instanceof YamlConfiguration conf) {
            try {
                conf.save(placeholder.getFilePath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        AudienceResolver.send(source, Utilities.MM.deserialize("<green>Placeholder <yellow>" + section.getName() + "</yellow>'s value was updated successfully!</green>"));
        this.reload(source);
    }

    @Command("message")
    @Permission("itsmyconfig.message")
    public void msgCommand(
            final BukkitSource source,
            final @Named("target") PlayerSelector players,
            final @Named("message") @Greedy String message
    ) {
        this.message(source, players, message);
    }

    @Command("config")
    @Permission("itsmyconfig.config")
    public void configCommand(
            final BukkitSource source,
            @SuggestionProvider("ModifiablePlaceholder") final Placeholder placeholder,
            @Named("value") @Greedy final String value
    ) {
        this.config(source, placeholder, value);
    }

}
