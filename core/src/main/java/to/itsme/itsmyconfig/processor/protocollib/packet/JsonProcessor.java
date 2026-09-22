package to.itsme.itsmyconfig.processor.protocollib.packet;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.processor.protocollib.PLibProcessor;
import to.itsme.itsmyconfig.util.IMCSerializer;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

import java.util.Optional;

public class JsonProcessor implements PLibProcessor {

    public static final JsonProcessor INSTANCE = new JsonProcessor();

    @Override
    public String name() {
        return "JSON";
    }

    @Override
    public boolean canHandle(final PacketContainer container) {
        return container.getStrings().readSafely(0) != null;
    }

    @Override
    public void process(final PacketEvent event) {
        final PacketContainer container = event.getPacket();
        final Player player = event.getPlayer();

        Utilities.debug(() -> "################# CHAT PACKET #################\nProcessing packet JSON");

        final String rawMessage = container.getStrings().readSafely(0);
        if (rawMessage == null) {
            Utilities.debug(() -> "Packet is null or empty\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        final String message = IMCSerializer.toMiniMessage(rawMessage);
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
        container.getStrings().write(0, Utilities.GSON_SERIALIZER.serialize(translated));
        Utilities.debug(() -> Strings.DEBUG_HYPHEN);
    }

}
