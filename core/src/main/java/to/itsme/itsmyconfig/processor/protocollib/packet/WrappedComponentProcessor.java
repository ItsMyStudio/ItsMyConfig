package to.itsme.itsmyconfig.processor.protocollib.packet;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.processor.protocollib.PLibProcessor;
import to.itsme.itsmyconfig.util.IMCSerializer;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

import java.util.Optional;

public class WrappedComponentProcessor implements PLibProcessor {

    public static final WrappedComponentProcessor INSTANCE = new WrappedComponentProcessor();

    @Override
    public String name() {
        return "WRAPPED_COMPONENT";
    }

    @Override
    public boolean canHandle(final PacketContainer container) {
        final WrappedChatComponent wrappedComponent = container.getChatComponents().readSafely(0);
        return wrappedComponent != null && !wrappedComponent.getJson().isEmpty();
    }

    @Override
    public void process(final PacketEvent event) {
        final PacketContainer container = event.getPacket();
        final Player player = event.getPlayer();

        Utilities.debug(() -> "################# CHAT PACKET #################\nProcessing packet WRAPPED_COMPONENT");

        final WrappedChatComponent wrappedComponent = container.getChatComponents().readSafely(0);
        if (wrappedComponent == null) {
            Utilities.debug(() -> "Packet is null or empty\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        final String found = wrappedComponent.getJson();
        if (found.isEmpty()) {
            Utilities.debug(() -> "Packet is null or empty\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        final String message;
        try {
            message = IMCSerializer.toMiniMessage(found);
        } catch (final Exception e) {
            Utilities.debug(() -> "An error happened while de/serializing " + found + ": ", e);
            Utilities.debug(() -> Strings.DEBUG_HYPHEN);
            return;
        }

        Utilities.debug(() -> "Found message: " + message);

        final Optional<String> parsed = Strings.parsePrefixedMessage(message);
        if (parsed.isEmpty()) {
            Utilities.debug(() -> "Message doesn't start w/ the symbol-prefix: " + message + "\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        final Component translated = Utilities.translate(parsed.get(), player);
        if (translated.equals(Component.empty())) {
            event.setCancelled(true);
            Utilities.debug(() -> "Component is empty, cancelling...\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        Utilities.debug(() -> "Final Product: " + IMCSerializer.toMiniMessage(translated) + "\n" + "Overriding...");
        container.getChatComponents().write(0, WrappedChatComponent.fromJson(
                Utilities.GSON_SERIALIZER.serialize(translated)
        ));
        Utilities.debug(() -> Strings.DEBUG_HYPHEN);
    }

}
