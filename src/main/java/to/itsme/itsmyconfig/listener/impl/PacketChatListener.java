package to.itsme.itsmyconfig.listener.impl;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.AdventureComponentConverter;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.ItsMyConfig;
import to.itsme.itsmyconfig.component.AbstractComponent;
import to.itsme.itsmyconfig.listener.PacketListener;
import to.itsme.itsmyconfig.util.Utilities;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class PacketChatListener extends PacketListener {

    private final boolean internalAdventure;
    private final Method fromJson, fromComponent;
    private final BungeeComponentSerializer bungee = BungeeComponentSerializer.get();

    public PacketChatListener(
            final ItsMyConfig plugin
    ) {
        super(plugin, PacketType.Play.Server.CHAT, PacketType.Play.Server.DISGUISED_CHAT, PacketType.Play.Server.SYSTEM_CHAT);
        Method fromComponent, fromJson;
        try {
            fromJson = AdventureComponentConverter.class.getDeclaredMethod(
                    "fromJsonAsObject",
                    String.class
            );
            fromComponent = AdventureComponentConverter.class.getDeclaredMethod(
                    "fromComponent",
                    AdventureComponentConverter.getComponentClass()
            );
        } catch (final Throwable ignored) {
            fromComponent = null;
            fromJson = null;
        }

        this.fromJson = fromJson;
        this.fromComponent = fromComponent;
        this.internalAdventure = this.fromComponent != null;
    }

    @Override
    public void onPacketSending(final PacketEvent event) {
        Utilities.debug("################# CHAT PACKET #################");
        final PacketContainer container = event.getPacket();
        final PacketResponse response = this.processPacket(container);
        if (response == null || response.message.isEmpty()) {
            Utilities.debug("###############################################");
            return;
        }

        final String message = response.message;
        Utilities.debug("Checking: " + message);
        if (!this.startsWithSymbol(message)) {
            Utilities.debug("Message doesn't start w/ the symbol-prefix: " + message);
            Utilities.debug("###############################################");
            return;
        }

        final Player player = event.getPlayer();
        final Component parsed = Utilities.translate(this.processMessage(message), player);

        if (parsed.equals(Component.empty())) {
            event.setCancelled(true);
            return;
        }

        Utilities.debug("Overriding Message as " + response.type.name());
        switch (response.type) {
            case JSON:
                container.getStrings().write(0, gsonComponentSerializer.serialize(parsed));
                break;
            case WRAPPED_COMPONENT:
                container.getChatComponents().write(0, WrappedChatComponent.fromJson(
                        gsonComponentSerializer.serialize(parsed)
                ));
                break;
            case BUNGEE_COMPONENT:
                container.getModifier().withType(TextComponent.class).write(0, new TextComponent(
                        BungeeComponentSerializer.get().serialize(parsed)
                ));
                break;
            case SERVER_ADVENTURE:
                final StructureModifier<Object> modifier = container.getModifier().withType(AdventureComponentConverter.getComponentClass());
                final String jsonComponent = gsonComponentSerializer.serialize(parsed);
                try {
                    modifier.write(0, fromJson.invoke(null, jsonComponent));
                } catch (final IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
                break;
        }

        Utilities.debug("###############################################");
    }

    private PacketResponse processPacket(final PacketContainer container) {
        Utilities.debug("Proccessing a packet");
        final StructureModifier<TextComponent> textComponentModifier = container.getModifier().withType(TextComponent.class);
        if (textComponentModifier.size() == 1) {
            Utilities.debug("Using Bungeecord TextComponent..");
            return new PacketResponse(ResponseType.BUNGEE_COMPONENT, processBaseComponents(textComponentModifier.readSafely(0)));
        } else {
            Utilities.debug("Failed to use Bungeecord TextComponent, trying ProtocolLib's WrappedChatComponent");
        }

        final WrappedChatComponent wrappedComponent = container.getChatComponents().readSafely(0);
        if (wrappedComponent != null) {
            final String found = wrappedComponent.getJson();
            if (!found.isEmpty()) {
                Utilities.debug("Found String: " + found);
                try {
                    Utilities.debug("Trying as json");
                    return new PacketResponse(ResponseType.WRAPPED_COMPONENT, AbstractComponent.parse(found).toMiniMessage());
                } catch (final Exception e) {
                    Utilities.debug("An error happened while de/serializing " + found + ": ", e);
                }
            }
        }

        final String rawMessage = container.getStrings().readSafely(0);
        if (rawMessage != null) {
            Utilities.debug("Raw-Parsing message: " + rawMessage);
            return new PacketResponse(ResponseType.JSON, AbstractComponent.parse(rawMessage).toMiniMessage());
        }

        if (internalAdventure) {
            try {
                final StructureModifier<?> modifier = container.getModifier().withType(AdventureComponentConverter.getComponentClass());
                if (modifier.size() == 1) {
                    final WrappedChatComponent wrappedAComponent = (WrappedChatComponent) fromComponent.invoke(null, modifier.readSafely(0));
                    final String json = wrappedAComponent.getJson();
                    Utilities.debug("Performing Server-Side Adventure for " + json);
                    return new PacketResponse(ResponseType.SERVER_ADVENTURE, AbstractComponent.parse(json).toMiniMessage());
                } else {
                    Utilities.debug("Failed to use Server-Side Adventure, Trying Bungeecord TextComponent..");
                }
            } catch (Throwable ignored) {
                Utilities.debug("Failed to use Server-Side Adventure, Trying Bungeecord TextComponent..");
            }
        }

        Utilities.debug("Found nothing.. returning null.");
        return null;
    }

    private String processBaseComponents(final BaseComponent... components) {
        return AbstractComponent.parse(bungee.deserialize(components)).toMiniMessage();
    }

    private static final class PacketResponse {

        private final ResponseType type;
        private final String message;

        private PacketResponse(
                final ResponseType type,
                final String message
        ) {
            this.type = type;
            this.message = message;
        }

    }

    private enum ResponseType {
        JSON, WRAPPED_COMPONENT, BUNGEE_COMPONENT, SERVER_ADVENTURE
    }

}
