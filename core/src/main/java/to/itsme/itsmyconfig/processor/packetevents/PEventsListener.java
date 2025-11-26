package to.itsme.itsmyconfig.processor.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import to.itsme.itsmyconfig.processor.PacketContent;
import to.itsme.itsmyconfig.processor.PacketListener;
import to.itsme.itsmyconfig.processor.PacketProcessor;
import to.itsme.itsmyconfig.util.IMCSerializer;
import to.itsme.itsmyconfig.util.Strings;
import to.itsme.itsmyconfig.util.Utilities;

import java.util.Map;
import java.util.Optional;

public class PEventsListener implements PacketListener, com.github.retrooper.packetevents.event.PacketListener {

    private PacketListenerCommon common;
    private static final String FAIL_MESSAGE_PREFIX = "<color:red><lang:multiplayer.message_not_delivered:";

    /* Cache packet processors for quick access */
    private final Map<PacketType.Play.Server, PacketProcessor<?>> packetTypeMap = Map.of(
            /*PacketType.Play.Server.CHAT_MESSAGE, PEventsProcessor.CHAT_MESSAGE,*/
            PacketType.Play.Server.SYSTEM_CHAT_MESSAGE, PEventsProcessor.SYSTEM_CHAT_MESSAGE,
            PacketType.Play.Server.DISCONNECT, PEventsProcessor.DISCONNECT
    );

    @Override
    public String name() {
        return "PacketEvents";
    }

    @Override
    public void load() {
        PacketEvents.getAPI().init();
        this.common = PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.NORMAL);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onPacketSend(final PacketSendEvent event) {
        final PacketTypeCommon type = event.getPacketType();

        if (!(type instanceof PacketType.Play.Server server)) {
            return;
        }

        final PacketProcessor<?> processor = packetTypeMap.get(server);
        if (processor == null) {
            return;
        }

        Utilities.debug(() -> "################# CHAT PACKET #################\nProcessing packet " + server.name());

        // Convert to wrapped packet only once
        Object wrappedPacket = switch (server) {
            case CHAT_MESSAGE -> new WrapperPlayServerChatMessage(event);
            case SYSTEM_CHAT_MESSAGE -> new WrapperPlayServerSystemChatMessage(event);
            case DISCONNECT -> new WrapperPlayServerDisconnect(event);
            default -> null;
        };

        if (wrappedPacket == null) {
            return;
        }

        // Unpack the wrapped packet
        final PacketContent<?> packet;

        try {
            packet = ((PacketProcessor<Object>) processor).unpack(wrappedPacket);
        } catch (final Exception ex) {
            Utilities.debug(() -> "Exception while unpacking the packet: ", ex);
            Utilities.debug(() -> Strings.DEBUG_HYPHEN);
            return;
        }

        if (packet == null || packet.isEmpty()) {
            Utilities.debug(() -> "Packet is null or empty\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        final String message = packet.message();
        Utilities.debug(() -> "Found message: " + message);

        if (message.startsWith(FAIL_MESSAGE_PREFIX)) {
            Utilities.debug(() -> "Message send failure message, cancelling...");
            event.setCancelled(true);
            return;
        }

        final Optional<String> parsed = Strings.parsePrefixedMessage(message);
        if (parsed.isEmpty()) {
            Utilities.debug(() -> "Message doesn't start w/ the symbol-prefix: " + message + "\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        final Player player = event.getPlayer();
        final Component translated = Utilities.translate(parsed.get(), player);
        if (translated.equals(Component.empty())) {
            event.setCancelled(true);
            Utilities.debug(() -> "Component is empty, cancelling...\n" + Strings.DEBUG_HYPHEN);
            return;
        }

        Utilities.debug(() -> "Final Product: " + IMCSerializer.toMiniMessage(translated) + "\n" + "Overriding...");
        event.markForReEncode(true);
        packet.save(translated);
        Utilities.debug(() -> Strings.DEBUG_HYPHEN);
    }

    @Override
    public void close() {
        PacketEvents.getAPI().getEventManager().unregisterListener(this.common);
        PacketEvents.getAPI().terminate();
    }
}
